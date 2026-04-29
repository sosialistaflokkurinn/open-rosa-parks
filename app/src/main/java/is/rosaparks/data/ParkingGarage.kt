package `is`.rosaparks.data

data class ParkingGarage(
    override val id: String,
    val name: String,
    val url: String,
) : ParkingLocation {
    override val displayName: String get() = name
}
