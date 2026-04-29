package `is`.rosaparks.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `is`.rosaparks.data.ParkingGarage
import `is`.rosaparks.data.ParkingSession
import `is`.rosaparks.data.ParkingZone
import `is`.rosaparks.ui.components.AppLogo
import `is`.rosaparks.ui.components.TimerDisplay
import `is`.rosaparks.viewmodel.ParkingViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveParkingScreen(
    viewModel: ParkingViewModel,
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
    val session by viewModel.activeSession.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val cost by viewModel.runningCost.collectAsState()

    BackHandler { onCancel() }

    val currentSession = session ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppLogo()
                        Text("Stæði virkt")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hætta við",
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Location info — zone vs garage renders differently
            when (val loc = currentSession.location) {
                is ParkingZone -> ZoneInfoCard(zone = loc)
                is ParkingGarage -> GarageInfoCard(garage = loc)
            }

            // Night-hours banner — garage sessions near midnight
            if (currentSession.location is ParkingGarage && isNearGarageClosing()) {
                NightHoursBanner()
            }

            // Plate
            Text(
                text = currentSession.plate,
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timer
            TimerDisplay(elapsedSeconds = elapsedSeconds)

            // Running cost — zone sessions only; garage sessions show a placeholder
            when (currentSession.location) {
                is ParkingZone -> {
                    Text(
                        text = "$cost kr",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "borgargjald",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is ParkingGarage -> {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "verð birt við lok lotu",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Savings strip — zone sessions only when there is actual cost
            if (currentSession.location is ParkingZone && cost > 0) {
                val savings = ParkingSession.easyParkSurchargeForCost(cost)
                Text(
                    text = "Þú sparar: $savings kr miðað við EasyPark",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Recovery card — garage sessions only, always visible so the user
            // never has to hunt for the phone number when the gate won't open.
            if (currentSession.location is ParkingGarage) {
                GarageRecoveryCard(session = currentSession)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stop button
            Button(
                onClick = onStop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(
                    text = "Stöðva",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}

/** 411-3403 stops answering at 24:00 and garages close 00:00–07:00. */
private fun isNearGarageClosing(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour >= 23 || hour < 7
}

@Composable
private fun NightHoursBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Næturtími",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    "Bílastæðahúsin loka 00:00–07:00 og sími 411-3403 er ekki svarað " +
                        "á þeim tíma. Ökumenn bera sjálfir ábyrgð á kostnaði ef bíll " +
                        "læsist inni yfir nótt — EasyPark gefur út PIN fyrir göngudyr " +
                        "ef þess þarf.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun GarageRecoveryCard(session: ParkingSession) {
    val context = LocalContext.current
    val garageName = session.location.displayName
    val plate = session.plate

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hliðið opnast ekki?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text =
                    "Myndavélin gæti hafa misskráð plötuna (algengt í snjó eða " +
                        "slæmu veðri). Lausn: hringdu í 411-3403 og lestu upp " +
                        "plötu $plate. Starfsmaður leiðréttir skráninguna. Ef það " +
                        "virkar ekki, greiddu í vélinni í húsinu og sendu " +
                        "endurgreiðslubeiðni á bilahus@reykjavik.is.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:4113403"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Hringja 411-3403")
                }
                OutlinedButton(
                    onClick = {
                        val subject = "Endurgreiðsla — $garageName ($plate)"
                        val body =
                            "Hæ,\n\nÉg greiddi fyrir stæði í $garageName í gegnum Rósa Parks " +
                                "en útkeyrsluhliðið opnaðist ekki.\n\nPlata: $plate\nHús: $garageName\n\n" +
                                "Vinsamlegast endurgreiðið.\n\nKv."
                        val uri =
                            Uri.parse(
                                "mailto:bilahus@reykjavik.is" +
                                    "?subject=" + Uri.encode(subject) +
                                    "&body=" + Uri.encode(body),
                            )
                        context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Senda tölvupóst")
                }
            }
        }
    }
}

@Composable
private fun ZoneInfoCard(zone: ParkingZone) {
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
                modifier =
                    Modifier
                        .size(32.dp)
                        .semantics {
                            contentDescription = "${zone.id} litur"
                        },
                shape = CircleShape,
                color = zone.color,
            ) {}
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = zone.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        buildString {
                            append("${zone.pricePerHour} kr/klst")
                            if (zone.maxHours != null) {
                                append(" (${zone.maxHours} klst hámark)")
                            }
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GarageInfoCard(garage: ParkingGarage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color(0xFF1A1A1A),
                ) {}
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = garage.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Myndavélagreiðsla",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text =
                    "Myndavél við innkeyrsluhlið les bílnúmerið. Ef hún mislas " +
                        "— til dæmis í snjó eða slæmu veðri — opnast útkeyrsluhliðið ekki, " +
                        "því plata sem appið greiddi fyrir og plata sem kerfið skráði " +
                        "stemma ekki. Lausn: hringja í 411-3403 og lesa plötuna upp.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
