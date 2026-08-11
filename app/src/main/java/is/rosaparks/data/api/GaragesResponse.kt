package `is`.rosaparks.data.api

import `is`.rosaparks.data.ParkingGarage
import kotlinx.serialization.Serializable

@Serializable
data class GaragesResponse(
    val garages: List<ApiGarage>,
    val source: String? = null,
)

@Serializable
data class ApiGarage(
    val id: String,
    val name: String,
    val url: String,
    val coordinates: List<List<List<Double>>>,
)

data class GarageMappingResult(
    val garages: List<ParkingGarage>,
    val polygons: Map<String, List<List<Pair<Double, Double>>>>,
)

fun GaragesResponse.toLocalGarages(): GarageMappingResult {
    val polygons = mutableMapOf<String, List<List<Pair<Double, Double>>>>()
    val garages =
        garages.map { api ->
            polygons[api.id] =
                api.coordinates.map { ring ->
                    ring.mapNotNull { coord ->
                        val lng = coord.getOrNull(0) ?: return@mapNotNull null
                        val lat = coord.getOrNull(1) ?: return@mapNotNull null
                        lng to lat
                    }
                }
            ParkingGarage(id = api.id, name = api.name, url = api.url)
        }
    return GarageMappingResult(garages, polygons)
}
