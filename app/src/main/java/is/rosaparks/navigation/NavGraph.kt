package `is`.rosaparks.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import `is`.rosaparks.ui.screens.AboutScreen
import `is`.rosaparks.ui.screens.ActiveParkingScreen
import `is`.rosaparks.ui.screens.GarageDetailScreen
import `is`.rosaparks.ui.screens.HomeScreen
import `is`.rosaparks.ui.screens.SummaryScreen
import `is`.rosaparks.ui.screens.ZoneMapScreen
import `is`.rosaparks.viewmodel.ParkingViewModel

object Routes {
    const val HOME = "home"
    const val ACTIVE_PARKING = "active_parking"
    const val SUMMARY = "summary"
    const val ABOUT = "about"
    const val ZONE_MAP = "zone_map"
    const val GARAGE_DETAIL = "garage_detail/{garageId}"

    fun garageDetail(garageId: String) = "garage_detail/$garageId"
}

@Composable
fun RosaParksNavGraph(viewModel: ParkingViewModel = viewModel()) {
    val preferencesLoaded by viewModel.preferencesLoaded.collectAsState()
    if (!preferencesLoaded) return

    val navController = rememberNavController()
    val startDestination =
        remember {
            if (viewModel.openMapFirst.value) Routes.ZONE_MAP else Routes.HOME
        }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onStartParking = {
                    viewModel.startParking()
                    navController.navigate(Routes.ACTIVE_PARKING) {
                        launchSingleTop = true
                    }
                },
                onAbout = {
                    navController.navigate(Routes.ABOUT) {
                        launchSingleTop = true
                    }
                },
                onMap = {
                    navController.navigate(Routes.ZONE_MAP) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.ACTIVE_PARKING) {
            ActiveParkingScreen(
                viewModel = viewModel,
                onStop = {
                    viewModel.stopParking()
                    navController.navigate(Routes.SUMMARY) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                },
                onCancel = {
                    viewModel.cancelParking()
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.SUMMARY) {
            SummaryScreen(
                viewModel = viewModel,
                onNewParking = {
                    viewModel.reset()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.ABOUT) {
            val initialOpenMapFirst = remember { viewModel.openMapFirst.value }
            AboutScreen(
                viewModel = viewModel,
                onBack = {
                    val nowOpenMapFirst = viewModel.openMapFirst.value
                    if (nowOpenMapFirst != initialOpenMapFirst) {
                        val destination = if (nowOpenMapFirst) Routes.ZONE_MAP else Routes.HOME
                        navController.navigate(destination) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable(Routes.ZONE_MAP) {
            ZoneMapScreen(
                viewModel = viewModel,
                onZoneSelected = { zone ->
                    viewModel.selectZone(zone)
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ZONE_MAP) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGarageSelected = { garage ->
                    viewModel.selectGarage(garage)
                    navController.navigate(Routes.garageDetail(garage.id)) {
                        launchSingleTop = true
                    }
                },
                onBack =
                    if (startDestination == Routes.ZONE_MAP) {
                        null
                    } else {
                        { navController.popBackStack() }
                    },
            )
        }
        composable(
            Routes.GARAGE_DETAIL,
            arguments = listOf(navArgument("garageId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val garageId = backStackEntry.arguments?.getString("garageId") ?: return@composable
            val garage = viewModel.garageById(garageId) ?: return@composable
            GarageDetailScreen(
                viewModel = viewModel,
                garage = garage,
                onStartParking = {
                    viewModel.startParking()
                    navController.navigate(Routes.ACTIVE_PARKING) {
                        popUpTo(Routes.GARAGE_DETAIL) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
