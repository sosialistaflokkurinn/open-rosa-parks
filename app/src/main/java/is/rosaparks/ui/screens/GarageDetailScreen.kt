package `is`.rosaparks.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `is`.rosaparks.data.ParkingGarage
import `is`.rosaparks.ui.components.AppLogo
import `is`.rosaparks.ui.components.PlateInput
import `is`.rosaparks.viewmodel.ParkingViewModel

private val PLATE_REGEX = Regex("^([A-Z]{2}[0-9]{3}|[A-Z]{3}[0-9]{2})$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageDetailScreen(
    viewModel: ParkingViewModel,
    garage: ParkingGarage,
    onStartParking: () -> Unit,
    onBack: () -> Unit,
) {
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
                        Text("Myndavélagreiðsla")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Til baka",
                        )
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
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Garage header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFF1A1A1A),
                    ) {}
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = garage.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Bílastæðasjóður Reykjavíkur",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ANPR explainer — set the user's expectations up front
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hvernig þetta virkar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text =
                            "1. Myndavél við innkeyrsluhlið les bílnúmerið og opnar hliðið.\n" +
                                "2. Þú getur greitt í appinu hvenær sem er á meðan þú ert inni í húsinu.\n" +
                                "3. Við útkeyrslu les myndavélin plötuna aftur og staðfestir greiðsluna.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ef útkeyrsluhliðið opnast ekki",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text =
                            "Líklegast missti myndavélin af plötunni (algengt í snjó og vondu veðri). " +
                                "Hringdu í 411-3403 og lestu plötuna upp fyrir starfsmann — þeir leiðrétta " +
                                "skráninguna í kerfinu. Ef það virkar ekki þarftu að greiða aftur í " +
                                "greiðsluvélinni í húsinu og senda endurgreiðslubeiðni á bilahus@reykjavik.is.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Plate input
            Text(
                text = "Bílnúmer",
                style = MaterialTheme.typography.titleMedium,
            )
            PlateInput(
                plate = plate,
                onPlateChange = { viewModel.updatePlate(it) },
            )
            Text(
                text =
                    "Gakktu úr skugga um að platan sé rétt. Hún er auðkennið sem bílastæðahúsið " +
                        "notar til að tengja greiðsluna við bílinn þinn.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Start button
            Button(
                onClick = onStartParking,
                enabled = plate.matches(PLATE_REGEX),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Text(
                    text = "Hefja myndavélagreiðslu",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Text(
                text = "Þjónustugjald: 0 kr.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
