package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import com.example.background.RoutineAlarmManager
import com.example.data.model.RoutineEntity
import com.example.data.model.TriggerType
import com.example.shizuku.ShizukuState
import com.example.ui.components.ExecutionLogsDialog
import com.example.ui.components.ShizukuConsoleDialog
import com.example.ui.components.ShizukuDiagnosticsDialog
import com.example.ui.components.ShizukuSettingsSheet
import com.example.ui.components.ShizukuStatusChip
import com.example.ui.components.ShizukuStatusIndicator
import com.example.ui.viewmodel.RoutineViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: RoutineViewModel,
    onCreateRoutineClick: () -> Unit,
    onEditRoutineClick: (Long) -> Unit,
    onOpenOnboarding: () -> Unit = {}
) {
    val routines by viewModel.routines.collectAsState()
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

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // 1. Shizuku Status Chip: Clicking opens the dedicated Shizuku Settings Sheet
                    ShizukuStatusChip(
                        state = shizukuState,
                        onClick = { showShizukuSheet = true },
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // 2. Execution History / Logs button
                    IconButton(
                        onClick = { showLogsDialog = true },
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
                            onClick = { showMoreMenu = true },
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
                onClick = onCreateRoutineClick,
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
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = bannerColor.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, bannerColor.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showShizukuSheet = true }
                            .testTag("shizuku_attention_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(bannerColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPermReq) Icons.Default.Key else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = bannerColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPermReq) "Shizuku Permission Required" else "Shizuku Service Offline",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = bannerColor
                                )
                                Text(
                                    text = if (isPermReq) "Tap to grant permission for system automation" else "Tap to configure wireless pairing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { showShizukuSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Configure")
                            }
                        }
                    }
                }
            }

            // Filter Chips & Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !filterActiveOnly,
                            onClick = { filterActiveOnly = false },
                            label = { Text("All (${routines.size})") },
                            modifier = Modifier.testTag("filter_all_chip")
                        )
                        FilterChip(
                            selected = filterActiveOnly,
                            onClick = { filterActiveOnly = true },
                            label = { Text("Active (${routines.count { it.isEnabled }})") },
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
                    RoutineCard(
                        routine = routine,
                        onToggle = { viewModel.toggleRoutine(routine) },
                        onRunNow = { viewModel.executeRoutineNow(routine) },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutineCard(
    routine: RoutineEntity,
    onToggle: () -> Unit,
    onRunNow: () -> Unit,
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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("routine_card_${routine.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

                Switch(
                    checked = routine.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.35f)
                    ),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Card Action Buttons: Run Now, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRunNow,
                    modifier = Modifier.testTag("run_routine_btn_${routine.id}"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run Now", style = MaterialTheme.typography.labelMedium)
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_routine_btn_${routine.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_routine_btn_${routine.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
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
                        Switch(
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

