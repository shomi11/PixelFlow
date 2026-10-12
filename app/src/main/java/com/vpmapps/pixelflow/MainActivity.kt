package com.vpmapps.pixelflow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vpmapps.pixelflow.shizuku.ShizukuManager
import com.vpmapps.pixelflow.shizuku.ShizukuPermissionManager
import com.vpmapps.pixelflow.ui.screens.CreateRoutineScreen
import com.vpmapps.pixelflow.ui.screens.DashboardScreen
import com.vpmapps.pixelflow.ui.screens.OnboardingScreen
import com.vpmapps.pixelflow.ui.theme.MyApplicationTheme
import com.vpmapps.pixelflow.ui.viewmodel.RoutineViewModel

/**
 * Screen requested by a widget or alarm tap.
 */
sealed interface LaunchDestination {
    data object CreateRoutine : LaunchDestination
    data class EditRoutine(val routineId: Long) : LaunchDestination

    companion object {
        const val EXTRA_ACTION = "EXTRA_ACTION"
        const val EXTRA_ROUTINE_ID = "EXTRA_ROUTINE_ID"
        const val ACTION_CREATE_ROUTINE = "CREATE_ROUTINE"

        fun from(intent: Intent?): LaunchDestination? {
            if (intent == null) return null
            if (intent.getStringExtra(EXTRA_ACTION) == ACTION_CREATE_ROUTINE) return CreateRoutine
            val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
            return if (routineId > 0) EditRoutine(routineId) else null
        }
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: RoutineViewModel by viewModels()
    private val launchDestination = mutableStateOf<LaunchDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh launch; after a configuration change the screen is already showing
        if (savedInstanceState == null) {
            launchDestination.value = LaunchDestination.from(intent)
        }
        try {
            ShizukuManager.init()
            ShizukuPermissionManager.attachApplication(application)
            ShizukuPermissionManager.reconnectShizuku()
            com.vpmapps.pixelflow.notification.RoutineNotificationManager.init(this)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error initializing Shizuku on launch", e)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PixelRoutinesApp(
                        viewModel = viewModel,
                        launchDestination = launchDestination.value,
                        onLaunchDestinationHandled = { launchDestination.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        LaunchDestination.from(intent)?.let { launchDestination.value = it }
    }

    override fun onResume() {
        super.onResume()
        try {
            ShizukuPermissionManager.onAppResumed()
            // The app is in the foreground here, so Android always allows starting the trigger service
            com.vpmapps.pixelflow.background.TriggerMonitorService.refresh(this)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error checking Shizuku on resume", e)
        }
    }
}

@Composable
fun PixelRoutinesApp(
    viewModel: RoutineViewModel,
    launchDestination: LaunchDestination? = null,
    onLaunchDestinationHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("pixel_routines_prefs", Context.MODE_PRIVATE)
    }
    val isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)
    val startDestination = if (isOnboardingCompleted) "dashboard" else "onboarding"

    LaunchedEffect(launchDestination) {
        val destination = launchDestination ?: return@LaunchedEffect
        // Widget shortcuts only apply once setup is done; otherwise onboarding comes first
        if (prefs.getBoolean("onboarding_completed", false)) {
            val route = when (destination) {
                LaunchDestination.CreateRoutine -> "create_routine/-1"
                is LaunchDestination.EditRoutine -> "create_routine/${destination.routineId}"
            }
            navController.navigate(route) {
                popUpTo("dashboard")
                launchSingleTop = true
            }
        }
        onLaunchDestinationHandled()
    }

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
