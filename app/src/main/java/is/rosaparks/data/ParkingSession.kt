package `is`.rosaparks.data

import kotlin.math.round

data class ParkingSession(
    val location: ParkingLocation,
    val plate: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val serverSessionId: String? = null,
) {
    val isActive: Boolean get() = endTimeMillis == null

    val durationMinutes: Long
        get() {
            val end = endTimeMillis ?: System.currentTimeMillis()
            return ((end - startTimeMillis) / 60_000).coerceAtLeast(0)
        }

    val totalCostKr: Int
        get() = costForMinutes(durationMinutes)

    /** EasyPark: 15% þjónustugjald, lágmark 75 kr. Zone sessions only. */
    val easyParkSurcharge: Int
        get() = if (location is ParkingZone) easyParkSurchargeForCost(totalCostKr) else 0

    /** Parka: 89 kr fast gjald per færslu. Zone sessions only. */
    val parkaSurcharge: Int
        get() = if (location is ParkingZone) 89 else 0

    companion object {
        fun costForMinutes(
            minutes: Long,
            pricePerHour: Int,
        ): Int = ((minutes.toDouble() / 60.0) * pricePerHour).let { round(it).toInt() }

        fun easyParkSurchargeForCost(cost: Int): Int = round(cost * 0.15).toInt().coerceAtLeast(75)
    }

    /**
     * Garage pricing is set by Bílastæðasjóður and is not yet exposed to the app;
     * final charges come from ScanGo at exit. Until we have that integration, we
     * surface a 0 running cost for garage sessions and let the UI show the actual
     * amount as "Verð birt við lok" — we don't guess.
     */
    fun costForMinutes(minutes: Long): Int =
        when (val loc = location) {
            is ParkingZone -> costForZone(loc, minutes)
            is ParkingGarage -> 0
        }

    private fun costForZone(
        zone: ParkingZone,
        minutes: Long,
    ): Int {
        val initial = zone.initialHours
        val afterRate = zone.pricePerHourAfterInitial
        if (initial != null && afterRate != null && minutes > initial * 60) {
            val initialCost = zone.pricePerHour * initial
            val remainingMinutes = minutes - initial * 60
            return initialCost + round((remainingMinutes.toDouble() / 60.0) * afterRate).toInt()
        }
        val maxMin = zone.maxHours?.let { it * 60L }
        val capped = if (maxMin != null) minutes.coerceAtMost(maxMin) else minutes
        return costForMinutes(capped, zone.pricePerHour)
    }
}
