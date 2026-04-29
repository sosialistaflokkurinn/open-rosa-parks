package `is`.rosaparks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import `is`.rosaparks.navigation.RosaParksNavGraph
import `is`.rosaparks.ui.theme.RosaParksTheme
import `is`.rosaparks.viewmodel.ParkingViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ParkingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !viewModel.preferencesLoaded.value }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RosaParksTheme {
                RosaParksNavGraph(viewModel = viewModel)
            }
        }
    }
}
