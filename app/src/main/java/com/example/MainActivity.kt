package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuPermissionManager
import com.example.ui.screens.CreateRoutineScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RoutineViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RoutineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            ShizukuManager.init()
            ShizukuPermissionManager.attachApplication(application)
            ShizukuPermissionManager.reconnectShizuku()
            com.example.notification.RoutineNotificationManager.init(this)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error initializing Shizuku on launch", e)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PixelRoutinesApp(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            ShizukuPermissionManager.onAppResumed()
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error checking Shizuku on resume", e)
        }
    }
}

@Composable
fun PixelRoutinesApp(viewModel: RoutineViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("pixel_routines_prefs", Context.MODE_PRIVATE)
    }
    val isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)
    val startDestination = if (isOnboardingCompleted) "dashboard" else "onboarding"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                viewModel = viewModel,
                onFinishOnboarding = {
                    prefs.edit().putBoolean("onboarding_completed", true).apply()
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onCreateRoutineClick = {
                    navController.navigate("create_routine/-1")
                },
                onEditRoutineClick = { routineId ->
                    navController.navigate("create_routine/$routineId")
                },
                onOpenOnboarding = {
                    navController.navigate("onboarding")
                }
            )
        }

        composable(
            route = "create_routine/{routineId}",
            arguments = listOf(
                navArgument("routineId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: -1L
            CreateRoutineScreen(
                routineId = if (routineId > 0) routineId else null,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
