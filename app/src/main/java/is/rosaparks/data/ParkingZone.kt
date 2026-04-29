package `is`.rosaparks.data

import androidx.compose.ui.graphics.Color
import `is`.rosaparks.ui.theme.ZoneBlue
import `is`.rosaparks.ui.theme.ZoneGreen
import `is`.rosaparks.ui.theme.ZoneRed
import `is`.rosaparks.ui.theme.ZoneYellow

data class ParkingZone(
    override val id: String,
    val name: String,
    val color: Color,
    val pricePerHour: Int,
    val description: String,
    val maxHours: Int? = null,
    val pricePerHourAfterInitial: Int? = null,
    val initialHours: Int? = null,
    val weekdayHours: String? = null,
    val weekendHours: String? = null,
) : ParkingLocation {
    override val displayName: String get() = name
}

val reykjavikZones =
    listOf(
        ParkingZone(
            id = "P1",
            name = "P1 — Rautt svæði",
            color = ZoneRed,
            pricePerHour = 660,
            description = "Miðborg innri (Bankastræti, Laugavegur)",
            maxHours = 3,
            weekdayHours = "09:00–21:00",
            weekendHours = "10:00–21:00",
        ),
        ParkingZone(
            id = "P2",
            name = "P2 — Blátt svæði",
            color = ZoneBlue,
            pricePerHour = 240,
            description = "Miðborg ytri (Hlemmur, Hljómskálagarður)",
            weekdayHours = "09:00–21:00",
            weekendHours = "10:00–21:00",
        ),
        ParkingZone(
            id = "P3",
            name = "P3 — Grænt svæði",
            color = ZoneGreen,
            pricePerHour = 240,
            description = "Vesturbær, Þingholt",
            initialHours = 2,
            pricePerHourAfterInitial = 70,
            weekdayHours = "09:00–18:00",
        ),
        ParkingZone(
            id = "P4",
            name = "P4 — Gult svæði",
            color = ZoneYellow,
            pricePerHour = 240,
            description = "Laugardalur, Háskóli",
            weekdayHours = "08:00–16:00",
        ),
    )
