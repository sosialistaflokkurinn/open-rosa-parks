package `is`.rosaparks.data.api

import kotlinx.serialization.Serializable

@Serializable
data class ZonesResponse(
    val zones: List<ApiZone>,
    val source: String? = null,
)

@Serializable
data class ApiZone(
    val id: String,
    val name: String,
    val description: String,
    val coordinates: List<List<List<Double>>>,
    val pricing: ApiPricing,
    val fee: String? = null,
)

@Serializable
data class ApiPricing(
    val pricePerHour: Int,
    val maxHours: Int? = null,
    val initialHours: Int? = null,
    val pricePerHourAfterInitial: Int? = null,
    val weekdayHours: String? = null,
    val weekendHours: String? = null,
)
