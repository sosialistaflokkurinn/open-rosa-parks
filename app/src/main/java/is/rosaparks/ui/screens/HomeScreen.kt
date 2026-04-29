package `is`.rosaparks.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `is`.rosaparks.ui.components.AppLogo
import `is`.rosaparks.ui.components.PlateInput
import `is`.rosaparks.ui.components.ZoneCard
import `is`.rosaparks.viewmodel.ParkingViewModel

private val PLATE_REGEX = Regex("^([A-Z]{2}[0-9]{3}|[A-Z]{3}[0-9]{2})$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ParkingViewModel,
    onStartParking: () -> Unit,
    onAbout: () -> Unit,
    onMap: () -> Unit,
) {
    val zones by viewModel.zones.collectAsState()
    val selectedZone by viewModel.selectedZone.collectAsState()
    val plate by viewModel.plate.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppLogo()
                        Text(
                            text = "Rósa Parks",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onMap) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Kort",
                        )
                    }
                    IconButton(onClick = onAbout) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Um appið",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Veldu svæði",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(zones) { zone ->
                    ZoneCard(
                        zone = zone,
                        isSelected = zone == selectedZone,
                        onClick = { viewModel.selectZone(zone) },
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PlateInput(
                        plate = plate,
                        onPlateChange = { viewModel.updatePlate(it) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onStartParking,
                        enabled = selectedZone != null && plate.matches(PLATE_REGEX),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = selectedZone?.color ?: MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Text(
                            text = "Leggja",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Bottom strip
            Text(
                text = "Þjónustugjald: 0 kr. Alltaf.",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
