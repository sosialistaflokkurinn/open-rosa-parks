package `is`.rosaparks.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import `is`.rosaparks.BuildConfig
import `is`.rosaparks.ui.components.AppLogo
import `is`.rosaparks.viewmodel.ParkingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: ParkingViewModel,
    onBack: () -> Unit,
    onBackendStatus: () -> Unit,
    onBlikkTest: () -> Unit,
) {
    val openMapFirst by viewModel.openMapFirst.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppLogo()
                        Text("Um Rósa Parks")
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
            var whyExpanded by remember { mutableStateOf(false) }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { whyExpanded = !whyExpanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Af hverju þetta app?",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (whyExpanded) "Loka" else "Opna",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(if (whyExpanded) 180f else 0f),
                )
            }
            AnimatedVisibility(visible = whyExpanded) {
                Text(
                    text =
                        "EasyPark innheimtir 15% þjónustugjald (að lágmarki 75 kr) og Parka " +
                            "rukkar 89 kr fyrir hverja færslu, til viðbótar við þau gjöld sem " +
                            "borgin sjálf ákveður. Bílastæðasjóður skilar 1,85 milljarða króna " +
                            "afgangi á ári, en Reykjavíkurborg hefur samt aldrei látið hanna " +
                            "sitt eigið app.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gjöf til borgarbúa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text =
                            "Rósa Parks er opinn hugbúnaður og sönnun þess að borgin " +
                                "getur gert þetta sjálf. Ekkert þjónustugjald, ekkert álag. " +
                                "Aðeins lögboðin bílastæðagjöld.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val context = LocalContext.current
                    Text(
                        text = "Skoða kóðann →",
                        modifier =
                            Modifier.clickable {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://github.com/samtak-svf/open-rosa-parks".toUri(),
                                    ),
                                )
                            },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text =
                    "Þetta app er mótmæli gegn leiguauðherjum sem hagnast á " +
                        "grunnþjónustu borgarinnar. Appið var hannað á nokkrum " +
                        "klukkustundum til að sanna aðalatriðið: Þetta er auðvelt. " +
                        "Borgin á bara að gera þetta.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            Text(
                text = "Stillingar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Opna appið á kortaskjá",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Sýnir staðsetningu og gjaldsvæði strax við opnun.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = openMapFirst,
                    onCheckedChange = { viewModel.setOpenMapFirst(it) },
                )
            }

            Text(
                text = "Tekur gildi þegar þú ferð til baka.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gagnsæi: hvers vegna við erum ekki live enn",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onBackendStatus() }
                        .padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                textDecoration = TextDecoration.Underline,
            )

            // Debug-only entry to the Blikk rail probe. R8 constant-folds
            // BuildConfig.DEBUG to false in release, so neither the branch nor
            // the screen it reaches survives into a shipped build.
            if (BuildConfig.DEBUG) {
                Text(
                    text = "Blikk prófun (debug)",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onBlikkTest() }
                            .padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    textDecoration = TextDecoration.Underline,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Samtakamáttur svf.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Opinn hugbúnaður, MIT leyfi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val versionContext = LocalContext.current
            Text(
                text = "Útgáfa ${BuildConfig.VERSION_NAME}",
                modifier =
                    if (BuildConfig.DEBUG) {
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                runCatching {
                                    FirebaseCrashlytics.getInstance().recordException(
                                        IllegalStateException("crashlytics-test-non-fatal"),
                                    )
                                }
                                Toast
                                    .makeText(
                                        versionContext,
                                        "Crashlytics test event sent",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            },
                        )
                    } else {
                        Modifier
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
