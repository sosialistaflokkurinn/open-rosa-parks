package `is`.rosaparks.data.api

import androidx.compose.ui.graphics.Color
import `is`.rosaparks.data.ParkingZone
import `is`.rosaparks.ui.theme.ZoneBlue
import `is`.rosaparks.ui.theme.ZoneGreen
import `is`.rosaparks.ui.theme.ZoneRed
import `is`.rosaparks.ui.theme.ZoneYellow

private val ZONE_COLORS: Map<String, Color> =
    mapOf("P1" to ZoneRed, "P2" to ZoneBlue, "P3" to ZoneGreen, "P4" to ZoneYellow)

data class ZoneMappingResult(
    val zones: List<ParkingZone>,
    val polygons: Map<String, List<List<Pair<Double, Double>>>>,
)

fun ZonesResponse.toLocalZones(): ZoneMappingResult {
    val polygons = mutableMapOf<String, List<List<Pair<Double, Double>>>>()
    val zones =
        zones.mapNotNull { apiZone ->
            apiZone.toLocalZone(polygons)
        }
    return ZoneMappingResult(zones, polygons)
}

private fun ApiZone.toLocalZone(polygons: MutableMap<String, List<List<Pair<Double, Double>>>>): ParkingZone? {
    val color = ZONE_COLORS[id] ?: return null

    if (coordinates.isNotEmpty()) {
        polygons[id] =
            coordinates.map { ring ->
                ring.mapNotNull { coord ->
                    val lng = coord.getOrNull(0) ?: return@mapNotNull null
                    val lat = coord.getOrNull(1) ?: return@mapNotNull null
                    lng to lat
                }
            }
    }

    return ParkingZone(
        id = id,
        name = name,
        color = color,
        pricePerHour = pricing.pricePerHour,
        description = description,
        maxHours = pricing.maxHours,
        pricePerHourAfterInitial = pricing.pricePerHourAfterInitial,
        initialHours = pricing.initialHours,
        weekdayHours = pricing.weekdayHours,
        weekendHours = pricing.weekendHours,
    )
}
