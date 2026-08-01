package `is`.rosaparks.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import `is`.rosaparks.R

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.mipmap.ic_launcher_round),
        contentDescription = "Rósa Parks",
        modifier =
            modifier
                .size(36.dp)
                .clip(CircleShape),
    )
}
