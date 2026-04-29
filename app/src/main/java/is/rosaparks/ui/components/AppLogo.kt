package `is`.rosaparks.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import `is`.rosaparks.R

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_emblem),
        contentDescription = "Rósa Parks",
        modifier = modifier.size(36.dp),
    )
}
