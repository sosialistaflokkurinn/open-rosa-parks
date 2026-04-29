package `is`.rosaparks.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign

@Composable
fun PlateInput(
    plate: String,
    onPlateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = plate,
        onValueChange = onPlateChange,
        label = { Text("Bílnúmer") },
        placeholder = { Text("t.d. AB123") },
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
            ),
        textStyle =
            MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center,
            ),
        modifier = modifier.fillMaxWidth(),
    )
}
