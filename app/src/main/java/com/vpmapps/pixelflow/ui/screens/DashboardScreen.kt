package com.vpmapps.pixelflow.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vpmapps.pixelflow.background.RoutineAlarmManager
import com.vpmapps.pixelflow.ui.components.ExpressiveFilterChip
import com.vpmapps.pixelflow.ui.components.ExpressiveSwitch
import com.vpmapps.pixelflow.data.model.RoutineEntity
import com.vpmapps.pixelflow.data.model.TriggerType
import com.vpmapps.pixelflow.shizuku.ShizukuState
import com.vpmapps.pixelflow.ui.components.ExecutionLogsDialog
import com.vpmapps.pixelflow.ui.components.ShizukuConsoleDialog
import com.vpmapps.pixelflow.ui.components.ShizukuDiagnosticsDialog
import com.vpmapps.pixelflow.ui.components.ShizukuSettingsSheet
import com.vpmapps.pixelflow.ui.components.ShizukuStatusChip
import com.vpmapps.pixelflow.ui.components.ShizukuStatusIndicator
import com.vpmapps.pixelflow.ui.util.rememberAppHapticFeedback
import com.vpmapps.pixelflow.ui.viewmodel.RoutineViewModel
import com.vpmapps.pixelflow.util.SpecialAccessHelper
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: RoutineViewModel,
    onCreateRoutineClick: () -> Unit,
    onEditRoutineClick: (Long) -> Unit,
    onOpenOnboarding: () -> Unit = {}
) {
    val haptics = rememberAppHapticFeedback()
    val routines by viewModel.routines.collectAsState()
    val runningRoutineIds by viewModel.runningRoutineIds.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val shizukuState by viewModel.shizukuState.collectAsState()
    val shizukuVersion by viewModel.shizukuVersion.collectAsState()
    val consoleOutput by viewModel.consoleOutput.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val networkDescription by viewModel.networkDescription.collectAsState()
    val isShizukuReconnecting by viewModel.isShizukuReconnecting.collectAsState()
    val areNotificationsEnabled by viewModel.areNotificationsEnabled.collectAsState()

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.setNotificationsEnabled(true)
        }
    }

    val notificationManager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager }
    var hasDndAccess by remember {
        mutableStateOf(notificationManager?.isNotificationPolicyAccessGranted == true)
    }

    // Special access that specific trigger types depend on; re-checked whenever the screen resumes
    var canScheduleExactAlarms by remember { mutableStateOf(SpecialAccessHelper.canScheduleExactAlarms(context)) }
    var hasUsageAccess by remember { mutableStateOf(SpecialAccessHelper.hasUsageAccess(context)) }
    var hasBackgroundLocation by remember { mutableStateOf(SpecialAccessHelper.hasBackgroundLocation(context)) }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { hasBackgroundLocation = SpecialAccessHelper.hasBackgroundLocation(context) }
    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { hasBackgroundLocation = SpecialAccessHelper.hasBackgroundLocation(context) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted == true
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted == true
                canScheduleExactAlarms = SpecialAccessHelper.canScheduleExactAlarms(context)
                hasUsageAccess = SpecialAccessHelper.hasUsageAccess(context)
                hasBackgroundLocation = SpecialAccessHelper.hasBackgroundLocation(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
                viewModel.checkShizukuStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showConsoleDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showShizukuSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var filterActiveOnly by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val displayedRoutines = if (filterActiveOnly) {
        routines.filter { it.isEnabled }
    } else {
        routines
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pixel Routines",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    // 1. Shizuku Status Chip: Clicking opens the dedicated Shizuku Settings Sheet
                    ShizukuStatusChip(
                        state = shizukuState,
                        onClick = {
                            haptics.buttonClick()
                            showShizukuSheet = true
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // 2. Execution History / Logs button
                    IconButton(
                        onClick = {
                            haptics.buttonClick()
                            showLogsDialog = true
                        },
                        modifier = Modifier.testTag("open_logs_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Execution Logs"
                        )
                    }

                    // 3. Overflow Menu for secondary options (Alerts, Diagnostics, Guide)
                    Box {
                        IconButton(
                            onClick = {
                                haptics.buttonClick()
                                showMoreMenu = true
                            },
                            modifier = Modifier.testTag("open_more_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Shizuku Controls") },
                                onClick = {
                                    haptics.buttonClick()
                                    showMoreMenu = false
                                    showShizukuSheet = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Routine Alerts") },
                                onClick = {
                                    haptics.buttonClick()
                                    showMoreMenu = false
                                    showNotificationDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        if (areNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Terminal Console") },
                                onClick = {
                                    haptics.buttonClick()
                                    showMoreMenu = false
                                    showConsoleDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Terminal, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Diagnostics") },
                                onClick = {
                                    haptics.buttonClick()
                                    showMoreMenu = false
                                    showDiagnosticsDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.BugReport, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Setup Guide") },
                                onClick = {
                                    haptics.buttonClick()
                                    showMoreMenu = false
                                    onOpenOnboarding()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.buttonClick()
                    onCreateRoutineClick()
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Routine") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_routine_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Compact attention banner only shown when Shizuku requires attention
            if (shizukuState != ShizukuState.AUTHORIZED) {
                item {
                    val isPermReq = shizukuState == ShizukuState.PERMISSION_REQUIRED
                    val bannerColor = if (isPermReq) Color(0xFFE65100) else Color(0xFFC62828)
                    ElevatedCard(
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showShizukuSheet = true }
                            .testTag("shizuku_attention_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = bannerColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPermReq) Icons.Default.Key else Icons.Default.Security,
                                        contentDescription = null,
                                        tint = bannerColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(bannerColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPermReq) "Shizuku Permission Required" else "Shizuku Service Offline",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = bannerColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPermReq) "Tap to grant permission for system automation" else "Tap to configure wireless pairing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { showShizukuSheet = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Configure")
                            }
                        }
                    }
                }
            }

            // Exact alarms: without "Alarms & reminders" timed routines fire late (off by default on Android 14+)
            val enabledTypes = routines.filter { it.isEnabled }.map { it.triggerType }.toSet()
            val needsExactAlarms = enabledTypes.any { it == TriggerType.TIME || it == TriggerType.SUNRISE || it == TriggerType.SUNSET }
            if (needsExactAlarms && !canScheduleExactAlarms) {
                item {
                    RoutineAccessBanner(
                        icon = Icons.Default.Alarm,
                        title = "Allow alarms & reminders",
                        message = "Timed routines can run minutes late until you allow exact alarms.",
                        buttonLabel = "Allow",
                        onClick = { SpecialAccessHelper.openExactAlarmSettings(context) },
                        testTag = "exact_alarm_banner"
                    )
                }
            }

            // Usage access: needed to notice when an app is opened
            if (TriggerType.APP in enabledTypes && !hasUsageAccess) {
                item {
                    RoutineAccessBanner(
                        icon = Icons.Default.Smartphone,
                        title = "Allow usage access",
                        message = "\"App opened\" routines need usage access to see which app is on screen.",
                        buttonLabel = if (shizukuState == ShizukuState.AUTHORIZED) "Auto-grant" else "Settings",
                        onClick = {
                            if (shizukuState == ShizukuState.AUTHORIZED) {
                                viewModel.grantUsageAccessViaShizuku { granted ->
                                    hasUsageAccess = granted
                                    if (!granted) SpecialAccessHelper.openUsageAccessSettings(context)
                                }
                            } else {
                                SpecialAccessHelper.openUsageAccessSettings(context)
                            }
                        },
                        testTag = "usage_access_banner"
                    )
                }
            }

            // Background location: geofences don't fire while the app is closed without it
            if (TriggerType.LOCATION in enabledTypes && !hasBackgroundLocation) {
                item {
                    RoutineAccessBanner(
                        icon = Icons.Default.LocationOn,
                        title = "Allow location all the time",
                        message = "Location routines only work in the background with \"Allow all the time\".",
                        buttonLabel = "Allow",
                        onClick = {
                            if (!com.vpmapps.pixelflow.util.LocationHelper.hasLocationPermission(context)) {
                                foregroundLocationLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        },
                        testTag = "background_location_banner"
                    )
                }
            }

            // DND / Sound Mode Access Banner if not granted and sound profiles exist
            if (!hasDndAccess && routines.any { r -> r.actions.any { a -> a.type.name == "SOUND_PROFILE" } }) {
                item {
                    val dndColor = Color(0xFF673AB7) // Material Deep Purple
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = dndColor.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, dndColor.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback to app details
                                    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(fallback)
                                }
                            }
                            .testTag("dnd_access_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(dndColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = dndColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ringer & Vibrate Permission",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = dndColor
                                )
                                Text(
                                    text = "Tap to grant Do Not Disturb access so routines can switch to Vibrate/Silent mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (shizukuState == ShizukuState.AUTHORIZED) {
                                    FilledTonalButton(
                                        onClick = { viewModel.grantDndAccessViaShizuku() },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("dnd_shizuku_grant_btn")
                                    ) {
                                        Text("Auto-Grant")
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(fallback)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("dnd_settings_btn")
                                ) {
                                    Text("Settings")
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpressiveFilterChip(
                            selected = !filterActiveOnly,
                            onClick = { filterActiveOnly = false },
                            label = { Text("All (${routines.size})", fontWeight = if (!filterActiveOnly) FontWeight.Bold else FontWeight.Medium) },
                            modifier = Modifier.testTag("filter_all_chip")
                        )
                        ExpressiveFilterChip(
                            selected = filterActiveOnly,
                            onClick = { filterActiveOnly = true },
                            label = { Text("Active (${routines.count { it.isEnabled }})", fontWeight = if (filterActiveOnly) FontWeight.Bold else FontWeight.Medium) },
                            modifier = Modifier.testTag("filter_active_chip")
                        )
                    }

                    Text(
                        text = "Automations",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Empty state
            if (displayedRoutines.isEmpty()) {
                item {
                    EmptyRoutinesCard(onCreateRoutineClick = onCreateRoutineClick)
                }
            } else {
                items(displayedRoutines, key = { it.id }) { routine ->
                    val isExecuting = runningRoutineIds.contains(routine.id) || routine.lastExecutionStatus == "RUNNING"
                    RoutineCard(
                        routine = routine,
                        isRunning = isExecuting,
                        onToggle = { viewModel.toggleRoutine(routine) },
                        onRunNow = { viewModel.executeRoutineNow(routine) },
                        onRestoreNow = { viewModel.restoreRoutineNow(routine) },
                        onEdit = { onEditRoutineClick(routine.id) },
                        onDelete = { viewModel.deleteRoutine(routine) }
                    )
                }
            }
        }
    }

    if (showConsoleDialog) {
        ShizukuConsoleDialog(
            shizukuState = shizukuState,
            shizukuVersion = shizukuVersion,
            lastResult = consoleOutput,
            onRequestPermission = { viewModel.requestShizukuPermission() },
            onExecuteCommand = { cmd -> viewModel.executeConsoleCommand(cmd) },
            onDismiss = { showConsoleDialog = false }
        )
    }

    if (showLogsDialog) {
        ExecutionLogsDialog(
            logs = logs,
            onClearLogs = { viewModel.clearLogs() },
            onDismiss = { showLogsDialog = false }
        )
    }

    if (showGuideDialog) {
        ShizukuGuideDialog(onDismiss = { showGuideDialog = false })
    }

    if (showDiagnosticsDialog) {
        ShizukuDiagnosticsDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            onRequestPermission = { viewModel.requestShizukuPermission() }
        )
    }

    if (showNotificationDialog) {
        NotificationSettingsDialog(
            isEnabled = areNotificationsEnabled,
            hasPermission = hasNotificationPermission,
            onToggleEnabled = { enabled ->
                if (enabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.setNotificationsEnabled(enabled)
            },
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onSendTest = { viewModel.sendTestNotification() },
            onDismiss = { showNotificationDialog = false }
        )
    }

    if (showShizukuSheet) {
        ShizukuSettingsSheet(
            state = shizukuState,
            version = shizukuVersion,
            isWifiConnected = isWifiConnected,
            networkDescription = networkDescription,
            isReconnecting = isShizukuReconnecting,
            onRequestPermission = { viewModel.requestShizukuPermission() },
            onRefresh = { viewModel.refreshShizukuStatus() },
            onReconnect = { viewModel.reconnectShizuku() },
            onOpenSetupGuide = onOpenOnboarding,
            onOpenConsole = { showConsoleDialog = true },
            onOpenDiagnostics = { showDiagnosticsDialog = true },
            onDismiss = { showShizukuSheet = false }
        )
    }
}

@Composable
private fun RoutineAccessBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    buttonLabel: String,
    onClick: () -> Unit,
    testTag: String
) {
    val accent = MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutineCard(
    routine: RoutineEntity,
    isRunning: Boolean = false,
    onToggle: () -> Unit,
    onRunNow: () -> Unit,
    onRestoreNow: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = try {
        Color(android.graphics.Color.parseColor(routine.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val triggerIcon = getTriggerIcon(routine.triggerType)
    val actions = remember(routine.actionsJson) { routine.parseActions() }

    val isExecutingNow = isRunning || routine.lastExecutionStatus == "RUNNING"
    val isInActiveSchedule = routine.isCurrentlyInActiveWindow()
    val hasActiveSnapshot = remember(isExecutingNow, routine.id) {
        com.vpmapps.pixelflow.service.RoutineStateSnapshotManager.hasSnapshot(context, routine.id)
    }
    val isCurrentlyRunning = isExecutingNow || isInActiveSchedule || hasActiveSnapshot
    var showCardInfoDialog by remember { mutableStateOf(false) }
    val haptics = rememberAppHapticFeedback()

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.buttonClick()
                onEdit()
            }
            .testTag("routine_card_${routine.id}"),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isCurrentlyRunning) 4.dp else 2.dp,
            pressedElevation = 6.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isCurrentlyRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Currently Running prominent status label
            if (isCurrentlyRunning) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00897B).copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("currently_running_badge_${routine.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00897B))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isExecutingNow) "CURRENTLY RUNNING • Executing actions" else "CURRENTLY RUNNING • Active schedule",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00897B)
                        )
                    }
                }
            }

            // Header: Icon accent + Title + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = triggerIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (routine.description.isNotBlank()) {
                        Text(
                            text = routine.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                ExpressiveSwitch(
                    checked = routine.isEnabled,
                    onCheckedChange = { onToggle() },
                    activeColor = accentColor,
                    modifier = Modifier.testTag("routine_switch_${routine.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // IF condition badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IF:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = routine.getTriggerSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isScheduledRoutine = routine.triggerType == TriggerType.TIME ||
                    routine.triggerType == TriggerType.SUNRISE ||
                    routine.triggerType == TriggerType.SUNSET

            if (isScheduledRoutine && routine.isEnabled) {
                val nextMillis = remember(routine.triggerConfigJson, routine.isEnabled) {
                    RoutineAlarmManager.calculateNextTriggerMillis(context, routine)
                }
                if (nextMillis != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Next alarm: ${RoutineAlarmManager.formatNextTriggerHumanReadable(nextMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // THEN Actions list
            Text(
                text = "THEN:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                actions.forEach { action ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = action.toSummary(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (routine.restoreOnEnd) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onTertiary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AUTO-RESTORE ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Reverts modified device settings when routine finishes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Card Action Buttons: Run Now, End & Restore, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            haptics.confirmAction()
                            onRunNow()
                        },
                        modifier = Modifier.testTag("run_routine_btn_${routine.id}"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (hasActiveSnapshot) "Run Again" else "Run Now", style = MaterialTheme.typography.labelMedium)
                    }

                    if (hasActiveSnapshot) {
                        Button(
                            onClick = {
                                haptics.buttonClick()
                                onRestoreNow()
                            },
                            modifier = Modifier.testTag("restore_routine_btn_${routine.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("End & Restore", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Auxiliary feature 1: Edit
                    IconButton(
                        onClick = {
                            haptics.buttonClick()
                            onEdit()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_routine_btn_${routine.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Routine",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Auxiliary feature 2: Share
                    IconButton(
                        onClick = {
                            haptics.buttonClick()
                            val acts = actions.joinToString(" • ") { it.toSummary() }
                            val shareText = buildString {
                                appendLine("⚡ Routine: ${routine.title}")
                                if (routine.description.isNotBlank()) appendLine("Description: ${routine.description}")
                                appendLine("Trigger: ${routine.getTriggerSummary()}")
                                if (acts.isNotBlank()) appendLine("Actions: $acts")
                                appendLine("\nCreated with Pixel Routines")
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Pixel Routine: ${routine.title}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Routine"))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("share_routine_btn_${routine.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Routine",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Auxiliary feature 3: Info
                    IconButton(
                        onClick = {
                            haptics.buttonClick()
                            showCardInfoDialog = true
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("info_routine_btn_${routine.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Routine Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = {
                            haptics.rejectOrDelete()
                            onDelete()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_routine_btn_${routine.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCardInfoDialog) {
        AlertDialog(
            onDismissRequest = { showCardInfoDialog = false },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(routine.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (routine.description.isNotBlank()) {
                        Text(routine.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = "Trigger: ${routine.getTriggerSummary()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Status: ${if (routine.isEnabled) "Active / Scheduled" else "Disabled"}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (actions.isNotEmpty()) {
                        Text(
                            "Configured Actions (${actions.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        actions.forEach { act ->
                            Text("• ${act.toSummary()}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCardInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun EmptyRoutinesCard(onCreateRoutineClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Routines Configured",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create automated workflows like Bedtime AOD Off, Auto-Rotate on app launch, or Battery Saver triggers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateRoutineClick,
                modifier = Modifier.testTag("empty_add_routine_btn")
            ) {
                Text("Create First Routine")
            }
        }
    }
}

@Composable
fun ShizukuGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Setting up Shizuku on Pixel", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pixel Routines leverages Shizuku to toggle system settings without root:\n" +
                            "• Always-On Display (AOD)\n" +
                            "• Wi-Fi & Bluetooth state\n" +
                            "• Battery Saver mode\n" +
                            "• Screen off timeout",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quick Wireless Debugging Setup:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "1. Install Shizuku from Play Store or GitHub.\n" +
                            "2. Enable Developer Options & Wireless Debugging in Settings.\n" +
                            "3. Open Shizuku -> Start via Wireless Debugging.\n" +
                            "4. Return to Pixel Routines and tap 'Grant Permission'.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alternatively via PC ADB:\n" +
                            "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}

fun getTriggerIcon(type: TriggerType): ImageVector {
    return when (type) {
        TriggerType.TIME -> Icons.Default.Schedule
        TriggerType.SUNRISE -> Icons.Default.LightMode
        TriggerType.SUNSET -> Icons.Default.Nightlight
        TriggerType.LOCATION -> Icons.Default.LocationOn
        TriggerType.WIFI -> Icons.Default.Wifi
        TriggerType.BLUETOOTH -> Icons.Default.Bluetooth
        TriggerType.BATTERY -> Icons.Default.BatteryChargingFull
        TriggerType.APP -> Icons.Default.Smartphone
    }
}

@Composable
fun NotificationSettingsDialog(
    isEnabled: Boolean,
    hasPermission: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onSendTest: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Routine Execution Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Receive Android system notifications whenever an automated routine triggers and its actions execute.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Master Toggle Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Execution Alerts",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isEnabled) "Active in system tray" else "Alerts turned off",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        ExpressiveSwitch(
                            checked = isEnabled,
                            onCheckedChange = onToggleEnabled,
                            modifier = Modifier.testTag("notification_toggle_switch")
                        )
                    }
                }

                // Android 13+ Permission Card
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Permission Required (Android 13+)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notification permission is needed to show alerts when routines run in the background.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRequestPermission,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }

                // Info Details Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "What notifications show:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• Routine name and trigger cause (Schedule, Wi-Fi, Battery, etc.)\n" +
                                    "• Status of applied actions (AOD, Volume, Vibrate, Silent, Battery Saver)\n" +
                                    "• Exact execution timestamp and success/failure details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Test Notification Button
                OutlinedButton(
                    onClick = onSendTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_test_notification_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Test Notification")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

