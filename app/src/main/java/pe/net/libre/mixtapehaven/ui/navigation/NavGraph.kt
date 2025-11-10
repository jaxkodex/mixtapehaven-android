package pe.net.libre.mixtapehaven.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import pe.net.libre.mixtapehaven.ui.home.HomeScreen
import pe.net.libre.mixtapehaven.ui.onboarding.OnboardingScreen
import pe.net.libre.mixtapehaven.ui.onboarding.OnboardingViewModel
import pe.net.libre.mixtapehaven.ui.troubleshoot.TroubleshootScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Troubleshoot : Screen("troubleshoot")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToTroubleshoot = {
                    navController.navigate(Screen.Troubleshoot.route)
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen()
        }

        composable(Screen.Troubleshoot.route) {
            TroubleshootScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
