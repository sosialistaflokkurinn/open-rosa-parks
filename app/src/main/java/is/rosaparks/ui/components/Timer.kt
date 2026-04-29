package `is`.rosaparks.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun TimerDisplay(
    elapsedSeconds: Long,
    modifier: Modifier = Modifier,
) {
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60

    val desc = "$hours klst $minutes mínútur $seconds sekúndur"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics { contentDescription = desc },
    ) {
        Text(
            text = "%d:%02d:%02d".format(hours, minutes, seconds),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "klst : mín : sek",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
