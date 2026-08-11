package `is`.rosaparks.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `is`.rosaparks.ui.components.AppLogo

/**
 * Two-mode screen explaining the state of the integration with
 * Bílastæðasjóður. When [firstLaunch] is true the user must tick the
 * acknowledgement before they can continue (no back affordance, system
 * back is consumed). When false it acts as a regular sub-screen with a
 * "Loka" close action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackendStatusScreen(
    firstLaunch: Boolean,
    onAcknowledge: () -> Unit,
    onClose: () -> Unit,
) {
    if (firstLaunch) {
        BackHandler { /* no-op — must acknowledge */ }
    }

    var checked by remember { mutableStateOf(!firstLaunch) }

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
                            if (firstLaunch) "Tenging í bið" else "Staða bakendatengingar",
                        )
                    }
                },
                navigationIcon = {
                    if (!firstLaunch) {
                        IconButton(onClick = onClose) {
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
            Text(
                text = "Tenging í bið: engir milliliðir leyfðir",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Hvers vegna þetta app er enn einungis sýnishorn",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text =
                    "Rósa Parks er sönnun þess að Reykjavíkurborg getur rekið sitt eigið " +
                        "bílastæðakerfi án þess að EasyPark eða Parka taki sinn hlut. Við höfum " +
                        "sótt um beina tengingu við Bílastæðasjóð. Þar til borgin veitir aðgang " +
                        "teljast allar greiðslur í þessu appi einungis sýnishorn.",
                style = MaterialTheme.typography.bodyLarge,
            )

            // Critical warning — primary-red panel so it can't be missed.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MIKILVÆGT",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text =
                            "Notkun þessa apps kemur EKKI í veg fyrir bílastæðasektir fyrr en " +
                                "tengingu hefur verið komið á. Borgin fær enga tilkynningu um " +
                                "að þú hafir lagt. Notaðu þetta aðeins sem sýnishorn.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hvar við stöndum gagnvart Bílastæðasjóði",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )

            Timeline()

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hver borgar og hve mikið",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )

            Text(
                text =
                    "Þessar tölur fundust hvergi í opnum gögnum borgarinnar. Við þekkjum " +
                        "þær einungis af því að við spurðum beint.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "1.080 EUR (~155 þ. kr), götukantur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text =
                            "Einskiptis tengingargjald sem rennur til ScanGo (Visma iAsset), " +
                                "ekki til borgarinnar. ScanGo er þjónustuaðili Bílastæðasjóðs " +
                                "og rekur forritið fyrir götukantinn.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Bílahús: verð á döfinni",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text =
                            "Bílahúsin nota annað forrit og annan þjónustuaðila en " +
                                "götukantur. Verð fyrir þá tengingu liggur ekki fyrir og " +
                                "finnst ekki í opinberum gögnum.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Text(
                text =
                    "Reykjavíkurborg greiðir nú þegar Visma iAsset (ScanGo) og Idegu tugi " +
                        "milljóna króna fyrir rekstur þessara forrita. Það er merkilegt að " +
                        "nýir aðilar þurfi að greiða þessum sömu fyrirtækjum aukalega fyrir " +
                        "að tengjast þjónustu sem borgin kaupir nú þegar fyrir sína borgara.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Til hvers flækjustigið?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )

            Text(
                text =
                    "Tvö aðskilin forrit. Tveir þjónustuaðilar. Fyrir eitt " +
                        "borgarbílastæðakerfi.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text =
                    "Fyrir hvern er þetta flækjustig, borgarbúa? Þeir borga fyrir bæði " +
                        "kerfin og rugla milli mismunandi appa. Þetta er ekki tæknileg " +
                        "nauðsyn heldur útvistunarkreddur sem framlengja kostnað og dreifa " +
                        "ábyrgð til einkafyrirtækja sem hagnast á flækjunni.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    colors =
                        CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.tertiary,
                            uncheckedColor = MaterialTheme.colorScheme.tertiary,
                            checkmarkColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                )
                Text(
                    modifier = Modifier.padding(top = 14.dp, start = 4.dp),
                    text =
                        "Ég skil að þetta er sýnishorn. Ég þarf áfram að greiða í gegnum " +
                            "opinberar leiðir til að forðast sekt.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = if (firstLaunch) onAcknowledge else onClose,
                enabled = checked,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
            ) {
                Text(
                    text = if (firstLaunch) "Halda áfram" else "Loka",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

private data class TimelineStep(
    val title: String,
    val date: String,
    val description: String,
    val state: StepState,
)

private enum class StepState { Done, InProgress, Pending }

private val STEPS =
    listOf(
        TimelineStep(
            title = "Fyrirspurn send",
            date = "1. apríl 2026",
            description =
                "Upphafleg fyrirspurn send á Bílastæðasjóð um tengingu. " +
                    "Engin viðbrögð í tæpa tvær vikur, ítrekun send 10. apríl.",
            state = StepState.Done,
        ),
        TimelineStep(
            title = "Fyrsta svar með skilmálum",
            date = "17. apríl 2026",
            description =
                "Bílastæðasjóður svarar með skilmálaskjali (frá 2019) og staðfestir " +
                    "tengingargjald 1.080 EUR, 16 dögum eftir upphaflega fyrirspurn.",
            state = StepState.Done,
        ),
        TimelineStep(
            title = "Umsókn send",
            date = "24. apríl 2026",
            description = "Formleg tengingarbeiðni undirrituð og send af stjórn flokksins.",
            state = StepState.Done,
        ),
        TimelineStep(
            title = "Móttaka staðfest",
            date = "27. apríl 2026",
            description = "Bílastæðasjóður staðfestir móttöku og hefur yfirferð.",
            state = StepState.Done,
        ),
        TimelineStep(
            title = "Borgin staðfestir umfang",
            date = "5. maí 2026",
            description =
                "Bílastæðasjóður staðfestir: sami samningur með bæði bílahúsin og " +
                    "götukantur, en sitt hvort forritið og þjónustuaðilinn.",
            state = StepState.Done,
        ),
        TimelineStep(
            title = "Verðlagning og samningsdrög",
            date = "Yfirstandandi",
            description =
                "1.080 EUR tengingargjald fyrir götukantinn staðfest (rennur til ScanGo). " +
                    "Verðlagning fyrir bílahús-tengingu á döfinni.",
            state = StepState.InProgress,
        ),
        TimelineStep(
            title = "Samstarfssamningur",
            date = "Síðar",
            description = "Undirritaður af báðum aðilum þegar verðlagning liggur fyrir.",
            state = StepState.Pending,
        ),
        TimelineStep(
            title = "Gagnavistunarúttekt",
            date = "Síðar",
            description = "Þjónustu- og nýsköpunarsvið borgarinnar staðfestir að bakendinn uppfylli kröfur.",
            state = StepState.Pending,
        ),
        TimelineStep(
            title = "Live tenging",
            date = "Síðar",
            description =
                "Þegar borgin opnar hliðið renna greiðslur þínar beint til hennar: " +
                    "engin þjónustugjöld, engir milliliðir.",
            state = StepState.Pending,
        ),
    )

@Composable
private fun Timeline() {
    Column(modifier = Modifier.fillMaxWidth()) {
        STEPS.forEachIndexed { index, step ->
            TimelineRow(step = step, isLast = index == STEPS.lastIndex)
        }
    }
}

@Composable
private fun TimelineRow(
    step: TimelineStep,
    isLast: Boolean,
) {
    val accent =
        when (step.state) {
            StepState.Done -> MaterialTheme.colorScheme.tertiary
            StepState.InProgress -> MaterialTheme.colorScheme.primary
            StepState.Pending -> MaterialTheme.colorScheme.outline
        }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp),
        ) {
            Surface(
                modifier = Modifier.size(20.dp),
                shape = CircleShape,
                color = accent,
            ) { Box(modifier = Modifier.fillMaxSize()) }
            if (!isLast) {
                Box(
                    modifier =
                        Modifier
                            .width(3.dp)
                            .height(60.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = accent.copy(alpha = 0.4f),
                    ) {}
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp, bottom = if (isLast) 0.dp else 16.dp),
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                text = step.date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
