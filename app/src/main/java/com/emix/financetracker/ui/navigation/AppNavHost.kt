package com.emix.financetracker.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.emix.financetracker.ui.screens.inflation.InflationScreen
import com.emix.financetracker.ui.screens.main.MainScreen
import com.emix.financetracker.ui.screens.onboarding.OnboardingScreen
import com.emix.financetracker.ui.screens.receipt.ReceiptScreen
import com.emix.financetracker.ui.screens.receipt.ScannerScreen
import com.emix.financetracker.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")
    object Receipt : Screen("receipt/{source}/{qrData}") {
        fun createRoute(source: String, qrData: String? = null) = "receipt/$source/${qrData ?: ""}"
    }
    object Inflation : Screen("inflation/{canonicalName}/{unit}") {
        fun createRoute(canonicalName: String, unit: String) = "inflation/$canonicalName/$unit"
    }
    object Scanner : Screen("scanner")
    object Settings : Screen("settings")
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "onboarding"
) {
    val viewModel: AppNavViewModel = hiltViewModel()
    var hasActiveBudget by remember { mutableStateOf<Boolean?>(null) }
    var isInitialized by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onBudgetCreated = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Main.route) {
            MainScreen(
                onScanQr = { navController.navigate(Screen.Scanner.route) },
                onAddManual = { navController.navigate(Screen.Receipt.createRoute("manual")) },
                onProductClick = { cn, unit -> navController.navigate(Screen.Inflation.createRoute(cn, unit)) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            route = Screen.Receipt.route,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType },
                navArgument("qrData") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "manual"
            val qrData = backStackEntry.arguments?.getString("qrData")
            ReceiptScreen(
                source = source,
                qrData = qrData,
                onConfirmed = { navController.popBackStack(Screen.Main.route, false) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Scanner.route) {
            ScannerScreen(
                onQrScanned = { qrData ->
                    navController.navigate(Screen.Receipt.createRoute("scan", qrData)) {
                        popUpTo(Screen.Scanner.route) { inclusive = true }
                    }
                },
                onError = { error ->
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Inflation.route,
            arguments = listOf(
                navArgument("canonicalName") { type = NavType.StringType },
                navArgument("unit") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawCn = backStackEntry.arguments?.getString("canonicalName") ?: ""
            val rawUnit = backStackEntry.arguments?.getString("unit") ?: ""
            val canonicalName = try { java.net.URLDecoder.decode(rawCn, "UTF-8") } catch (e: Exception) { rawCn }
            val unit = try { java.net.URLDecoder.decode(rawUnit, "UTF-8") } catch (e: Exception) { rawUnit }
            InflationScreen(canonicalName = canonicalName, unit = unit, onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }

    // Initial navigation check
    LaunchedEffect(Unit) {
        if (!isInitialized) {
            hasActiveBudget = viewModel.hasActiveBudget()
            delay(100)
            val hasBudget = hasActiveBudget ?: false
            if (hasBudget) {
                navController.navigate(Screen.Main.route) {
                    popUpTo(startDestination) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(startDestination) { inclusive = true }
                }
            }
            isInitialized = true
        }
    }
}