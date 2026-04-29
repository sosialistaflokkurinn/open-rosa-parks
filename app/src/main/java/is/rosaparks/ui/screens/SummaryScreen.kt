package `is`.rosaparks.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import `is`.rosaparks.data.ParkingGarage
import `is`.rosaparks.data.ParkingZone
import `is`.rosaparks.ui.components.AppLogo
import `is`.rosaparks.ui.components.SurchargeTable
import `is`.rosaparks.viewmodel.ParkingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: ParkingViewModel,
    onNewParking: () -> Unit,
) {
    BackHandler { onNewParking() }

    val session by viewModel.completedSession.collectAsState()
    val completedSession = session ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppLogo()
                        Text("Yfirlit")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Summary header
            Text(
                text = "Lokið!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            // Location + plate — show display name regardless of type
            Text(
                text = "${completedSession.location.displayName}\n${completedSession.plate}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            // Duration
            val hours = completedSession.durationMinutes / 60
            val mins = completedSession.durationMinutes % 60
            Text(
                text = "Tími: %d:%02d".format(hours, mins),
                style = MaterialTheme.typography.headlineMedium,
            )

            // Cost block differs for zone vs garage
            when (completedSession.location) {
                is ParkingZone -> {
                    Text(
                        text = "${completedSession.totalCostKr} kr",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "borgargjald",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SurchargeTable(session = completedSession)
                }
                is ParkingGarage -> {
                    Text(
                        text = "Verð birt af húsinu",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text =
                            "Bílastæðahúsið reiknar verðið við útkeyrslu, byggt á tegund " +
                                "stæðis og þeim tíma sem þú varst inni.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // New parking button
            Button(
                onClick = onNewParking,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Text(
                    text = "Nýtt stæði",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}
