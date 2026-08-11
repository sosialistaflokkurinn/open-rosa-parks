package `is`.rosaparks.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `is`.rosaparks.BuildConfig
import `is`.rosaparks.data.api.BlikkDebtor
import `is`.rosaparks.data.api.createBlikkTestPayment
import `is`.rosaparks.data.api.fetchBlikkPaymentStatus
import `is`.rosaparks.data.api.validateBlikkAccount
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The three ways a payment can be started, in descending order of how much the
 * payer is spared. [DIRECT_DEBTOR] is the one the top-up design assumes and
 * the one Blikk has not enabled: both Straumbogi sales channels answered
 * `403 sales channel does not allow direct debtor payments` on 2026-08-09, and
 * nothing in the merchant portal turns it on. It stays selectable so the
 * refusal is visible on the phone rather than a claim in a document.
 */
private enum class BlikkProbeMode(
    val label: String,
    val explanation: String,
) {
    ANONYMOUS(
        "Nafnlaus",
        "Ekkert auðkenni fer héðan. Þú segir til nafns og auðkennir þig hjá Blikk.",
    ),
    IDENTIFIED(
        "Forskráð kennitala",
        "Kennitalan fylgir með, svo Blikk veit strax hver borgar. Aðeins sterk auðkenning eftir.",
    ),
    DIRECT_DEBTOR(
        "Direct Debtor",
        "Kennitala og reikningur, engin Blikk-síða. Rásin leyfir þetta ekki enn, þú átt að fá 403.",
    ),
}

/**
 * Debug-only probe for the Blikk Ecom rail (docs/greidslugatt/DECISION.md v2).
 *
 * This is not the top-up feature and must not grow into one here: whether
 * money lands in the operator's account or straight in the fund's is still
 * open (DECISION.md §3.1), and building a balance before that is answered
 * would be building on sand. All this screen does is create a real payment of
 * a few krónur, hand the payer to their bank, and watch the status until it
 * settles — enough to prove the rail works from a phone, and nothing more.
 *
 * The status transcript is the actual deliverable: it shows the documented
 * lifecycle (DRAFT → PENDING → SCA_REQUIRED → SCA_COMPLETE → terminal)
 * happening against production, and it shows the Direct Debtor refusal as the
 * rail itself words it.
 *
 * Payer details are prefilled from local.properties via BuildConfig, so no
 * kennitala or account number is committed, and a release build carries empty
 * strings whatever is configured locally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlikkTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clock = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    var mode by remember { mutableStateOf(BlikkProbeMode.IDENTIFIED) }
    var kennitala by remember { mutableStateOf(BuildConfig.BLIKK_TEST_KENNITALA) }
    var bban by remember { mutableStateOf(BuildConfig.BLIKK_TEST_BBAN) }
    var amountKr by remember { mutableStateOf(1) }
    var paymentId by remember { mutableStateOf<String?>(null) }
    var scaUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var terminal by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val transcript = remember { mutableStateListOf<String>() }

    fun log(line: String) {
        transcript.add("${clock.format(Date())}  $line")
    }

    // Poll while the payment is alive. Blikk's own In-App guidance is to poll
    // for scaRedirectUrl or a terminal status, so this is the documented
    // mechanism rather than a workaround for the missing webhook — the worker
    // sends no callbackUrl on purpose.
    LaunchedEffect(paymentId) {
        val id = paymentId ?: return@LaunchedEffect
        while (!terminal) {
            delay(2_000)
            val result = runCatching { fetchBlikkPaymentStatus(id) }
            result
                .onSuccess { payment ->
                    if (payment.status != status) {
                        status = payment.status
                        val reason = payment.message.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                        log("→ ${payment.status}$reason")
                    }
                    // A back-channel-SCA bank returns no URL on create but may
                    // surface one later; pick it up if it appears.
                    if (scaUrl.isBlank() && payment.scaRedirectUrl.isNotBlank()) {
                        scaUrl = payment.scaRedirectUrl
                        log("greiðslusíða birtist við pollun")
                    }
                    if (payment.terminal) {
                        terminal = true
                        log(if (payment.paid) "GREITT" else "LOKASTAÐA án greiðslu")
                    }
                }.onFailure { error ->
                    log("pollun mistókst: ${error.message}")
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blikk prófun") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Til baka")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Raunveruleg greiðsla",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text =
                    "Þetta er production rásin, ekki stage. Upphæðin fer raunverulega af " +
                        "reikningnum þínum og inn á reikning sölurásarinnar. Ekkert er geymt " +
                        "hjá okkur: engin inneign, engin greiðsluskrá, ekkert logg. Hvaða " +
                        "auðkenni fara héðan ræðst af leiðinni sem þú velur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Bakendi: ${BuildConfig.API_BASE_URL}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(1, 2, 3).forEach { value ->
                    FilterChip(
                        selected = amountKr == value,
                        onClick = { amountKr = value },
                        enabled = paymentId == null,
                        label = { Text("$value kr.") },
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BlikkProbeMode.entries.forEach { value ->
                    FilterChip(
                        selected = mode == value,
                        onClick = { mode = value },
                        enabled = paymentId == null,
                        label = { Text(value.label) },
                    )
                }
            }
            Text(
                text = mode.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (mode != BlikkProbeMode.ANONYMOUS) {
                OutlinedTextField(
                    value = kennitala,
                    onValueChange = { kennitala = it },
                    label = { Text("Kennitala") },
                    enabled = paymentId == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (mode == BlikkProbeMode.DIRECT_DEBTOR) {
                OutlinedTextField(
                    value = bban,
                    onValueChange = { bban = it },
                    label = { Text("Reikningur (12 tölustafir)") },
                    enabled = paymentId == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = {
                        log("staðfesti reikning, engir peningar hreyfast")
                        scope.launch {
                            runCatching { validateBlikkAccount(kennitala, bban) }
                                .onSuccess { check ->
                                    val kind = if (check.corporate) "fyrirtækjareikningur" else "einstaklingsreikningur"
                                    val why = check.message.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                                    log(if (check.valid) "reikningur gildur, $kind" else "reikningur ógildur$why")
                                }.onFailure { error -> log("staðfesting mistókst: ${error.message}") }
                        }
                    },
                    enabled = paymentId == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Staðfesta reikning (0 kr.)")
                }
            }

            Button(
                onClick = {
                    busy = true
                    transcript.clear()
                    terminal = false
                    status = ""
                    scaUrl = ""
                    log("stofna greiðslu, $amountKr kr., ${mode.label.lowercase()}")
                    val debtor =
                        when (mode) {
                            BlikkProbeMode.ANONYMOUS -> null
                            BlikkProbeMode.IDENTIFIED -> BlikkDebtor(kennitala = kennitala)
                            BlikkProbeMode.DIRECT_DEBTOR ->
                                BlikkDebtor(
                                    kennitala = kennitala,
                                    bban = bban,
                                    name = BuildConfig.BLIKK_TEST_NAME,
                                )
                        }
                    scope.launch {
                        runCatching { createBlikkTestPayment(amountKr, debtor) }
                            .onSuccess { payment ->
                                status = payment.status
                                scaUrl = payment.scaRedirectUrl
                                log("leið: ${payment.mode}")
                                log("→ ${payment.status}  id=${payment.id}")
                                if (payment.scaRedirectUrl.isBlank()) {
                                    log("engin greiðslusíða, bíð eftir ýtingu úr bankaappinu")
                                }
                                paymentId = payment.id
                            }.onFailure { error ->
                                log("stofnun mistókst: ${error.message}")
                            }
                        busy = false
                    }
                },
                enabled = !busy && (paymentId == null || terminal),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (paymentId == null) "Stofna greiðslu" else "Ný greiðsla")
            }

            if (scaUrl.isNotBlank() && !terminal) {
                Button(
                    onClick = {
                        log("opna greiðslusíðu")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(scaUrl)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Opna greiðslu í vafra")
                }
            }

            if (paymentId != null && !terminal) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Fylgist með stöðu: $status",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (transcript.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        transcript.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }

            if (terminal) {
                OutlinedButton(
                    onClick = {
                        paymentId = null
                        terminal = false
                        status = ""
                        scaUrl = ""
                        transcript.clear()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Hreinsa")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
