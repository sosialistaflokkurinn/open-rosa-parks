package `is`.rosaparks.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `is`.rosaparks.data.ParkingGarage
import `is`.rosaparks.data.ParkingZone
import `is`.rosaparks.ui.components.AppLogo
import `is`.rosaparks.viewmodel.ParkingViewModel
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.location.LocationPuck
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberNullLocationProvider
import org.maplibre.compose.location.rememberUserLocationState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

private fun buildZoneGeoJsonString(
    zone: ParkingZone,
    polygons: Map<String, List<List<Pair<Double, Double>>>>,
): String {
    val empty = """{"type":"FeatureCollection","features":[]}"""
    val rings = polygons[zone.id] ?: return empty
    val features =
        rings.joinToString(",") { ring ->
            val coords = ring.joinToString(",") { (lng, lat) -> "[$lng,$lat]" }
            """{"type":"Feature","geometry":{"type":"Polygon",""" +
                """"coordinates":[[$coords]]},"properties":{}}"""
        }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

private fun buildRingsGeoJsonString(rings: List<List<Pair<Double, Double>>>): String {
    val features =
        rings.joinToString(",") { ring ->
            val coords = ring.joinToString(",") { (lng, lat) -> "[$lng,$lat]" }
            """{"type":"Feature","geometry":{"type":"Polygon",""" +
                """"coordinates":[[$coords]]},"properties":{}}"""
        }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

private fun hasLocationPermission(context: android.content.Context): Boolean =
    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneMapScreen(
    viewModel: ParkingViewModel,
    onZoneSelected: (ParkingZone) -> Unit,
    onGarageSelected: (ParkingGarage) -> Unit,
    onBack: (() -> Unit)?,
) {
    val zones by viewModel.zones.collectAsState()
    val selectedZone by viewModel.selectedZone.collectAsState()
    val polygons by viewModel.zonePolygons.collectAsState()
    val garages by viewModel.garages.collectAsState()
    val garagePolygons by viewModel.garagePolygons.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var hasAutoRequestedPermission by remember { mutableStateOf(false) }
    var hasAutoCenteredOnUser by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            hasPermission = results.values.any { it }
        }

    LaunchedEffect(Unit) {
        if (!hasPermission && !hasAutoRequestedPermission) {
            hasAutoRequestedPermission = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    val cameraState =
        rememberCameraState(
            firstPosition =
                CameraPosition(
                    target = Position(-21.9426, 64.1466),
                    zoom = 13.5,
                ),
        )

    @SuppressLint("MissingPermission")
    val locationProvider =
        key(hasPermission) {
            if (hasPermission) {
                rememberDefaultLocationProvider()
            } else {
                rememberNullLocationProvider()
            }
        }
    val locationState = rememberUserLocationState(locationProvider)

    LaunchedEffect(locationState.location, hasPermission) {
        val loc = locationState.location
        if (hasPermission && loc != null && !hasAutoCenteredOnUser) {
            hasAutoCenteredOnUser = true
            cameraState.animateTo(
                CameraPosition(target = loc.position, zoom = 15.0),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppLogo()
                        Text("Bílastæðakort")
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Til baka",
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!hasPermission) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    } else {
                        val loc = locationState.location
                        if (loc != null) {
                            coroutineScope.launch {
                                cameraState.animateTo(
                                    CameraPosition(
                                        target = loc.position,
                                        zoom = 15.0,
                                    ),
                                )
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Mín staðsetning",
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = cameraState,
                baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
            ) {
                // Render zones from outermost (P4) to innermost (P1)
                // so that P1 is on top and receives clicks first
                val zonesReversed = remember(zones) { zones.reversed() }
                for (zone in zonesReversed) {
                    val geoJson = remember(zone.id, polygons) { buildZoneGeoJsonString(zone, polygons) }
                    val isSelected = zone == selectedZone
                    val opacity = if (isSelected) 0.55f else 0.30f
                    val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(geoJson))

                    FillLayer(
                        id = "zone-fill-${zone.id}",
                        source = source,
                        color = const(zone.color),
                        opacity = const(opacity),
                        onClick = {
                            onZoneSelected(zone)
                            ClickResult.Consume
                        },
                    )
                }

                // Garages rendered on top of zones so they're always visible
                // as distinct dark building outlines. Tapping a garage enters the
                // Automatic Camera Parking flow (GarageDetailScreen).
                for (garage in garages) {
                    val rings = garagePolygons[garage.id] ?: continue
                    val geoJson = remember(garage.id, rings) { buildRingsGeoJsonString(rings) }
                    val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(geoJson))
                    FillLayer(
                        id = "garage-fill-${garage.id}",
                        source = source,
                        color = const(Color(0xFF1A1A1A)),
                        opacity = const(0.92f),
                        onClick = {
                            onGarageSelected(garage)
                            ClickResult.Consume
                        },
                    )
                }

                // Gate the puck on both permission AND a resolved first fix.
                // With just `hasPermission`, LocationPuck composes against a
                // null `locationState.location` and the underlying MapLibre
                // GeoJsonSource serializer throws NoSuchElementException
                // because the puck's coordinate is not yet present.
                // Crash first observed on internal-testing release 1.0 (2),
                // Samsung Galaxy A36 5G, Android 16 Beta.
                if (hasPermission && locationState.location != null) {
                    LocationPuck(
                        idPrefix = "user-location",
                        locationState = locationState,
                        cameraState = cameraState,
                    )
                }
            }
        }
    }
}
