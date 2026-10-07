package com.example.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.background.GeofenceManager
import com.example.background.RoutineAlarmManager
import com.example.data.model.ActionType
import com.example.data.model.Routine
import com.example.data.model.RoutineActionItem
import com.example.data.model.RoutineTrigger
import com.example.data.model.TriggerType
import com.example.shizuku.ShizukuServiceHelper
import com.example.util.BluetoothHelper
import com.example.util.LocationHelper
import com.example.util.SavedBluetoothDevice
import com.example.util.SolarCalculator
import com.example.util.TimeParser
import com.example.util.UserLocation
import java.util.Calendar
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * RoutineEditor is a Compose component providing a form for:
 * 1. Naming a routine (title, description, icon, accent color)
 * 2. Selecting trigger types including scheduled triggers (specific times, daily/weekly intervals)
 *    with AlarmManager precision integration.
 * 3. Defining corresponding shell commands as actions (AOD, Auto-Rotate, Dark Mode, custom commands)
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditor(
    initialRoutine: Routine? = null,
    onSave: (Routine) -> Unit,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 1. Routine Name and Metadata State
    var routineName by remember(initialRoutine) {
        mutableStateOf(initialRoutine?.name ?: initialRoutine?.title ?: "")
    }
    var description by remember(initialRoutine) {
        mutableStateOf(initialRoutine?.description ?: "")
    }
    var selectedColor by remember(initialRoutine) {
        mutableStateOf(initialRoutine?.colorHex ?: "#3871E0")
    }
    var selectedIcon by remember(initialRoutine) {
        mutableStateOf(initialRoutine?.iconName ?: "schedule")
    }

    // 2. Predefined Trigger Selection State
    var selectedTriggerType by remember(initialRoutine) {
        mutableStateOf(initialRoutine?.triggerType ?: TriggerType.TIME)
    }

    // Scheduled Trigger State (AlarmManager Integration)
    var scheduleMode by remember(initialRoutine) { mutableStateOf("DAILY") } // "SPECIFIC_TIME", "DAILY", "WEEKLY", "INTERVAL"
    var startTime by remember(initialRoutine) { mutableStateOf("08:00") }
    var endTime by remember(initialRoutine) { mutableStateOf("22:00") }
    var selectedDays by remember(initialRoutine) { mutableStateOf("Mon, Tue, Wed, Thu, Fri, Sat, Sun") }
    var intervalMinutes by remember(initialRoutine) { mutableIntStateOf(60) }
    var alarmTestStatus by remember { mutableStateOf<String?>(null) }

    // Other Trigger states
    var wifiSsid by remember(initialRoutine) { mutableStateOf("Home-5G") }
    var wifiConnected by remember(initialRoutine) { mutableStateOf(true) }
    var btDevice by remember(initialRoutine) { mutableStateOf("Any Paired Device") }
    var btConnected by remember(initialRoutine) { mutableStateOf(true) }
    var hasBtPermission by remember { mutableStateOf(BluetoothHelper.hasBluetoothPermission(context)) }
    var savedBtDevices by remember {
        mutableStateOf(if (hasBtPermission) BluetoothHelper.getSavedBluetoothDevices(context) else emptyList<SavedBluetoothDevice>())
    }
    var showCustomBtInput by remember { mutableStateOf(false) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBtPermission = isGranted
        if (isGranted) {
            savedBtDevices = BluetoothHelper.getSavedBluetoothDevices(context)
            if (btDevice == "Any Paired Device" && savedBtDevices.isNotEmpty()) {
                btDevice = savedBtDevices.first().name
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(selectedTriggerType, hasBtPermission) {
        if (selectedTriggerType == TriggerType.BLUETOOTH && hasBtPermission) {
            savedBtDevices = BluetoothHelper.getSavedBluetoothDevices(context)
        }
    }

    // Solar Trigger State (Sunrise / Sunset)
    var solarOffsetMinutes by remember(initialRoutine) { mutableIntStateOf(0) }
    var solarDays by remember(initialRoutine) { mutableStateOf("Daily") }
    var hasLocationPermission by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }
    var userLocation by remember { mutableStateOf(LocationHelper.getLastKnownLocation(context)) }
    var isRefreshingLocation by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val granted = fineGranted || coarseGranted
        hasLocationPermission = granted
        if (granted) {
            isRefreshingLocation = true
            LocationHelper.requestFreshLocation(context) { loc ->
                userLocation = loc
                isRefreshingLocation = false
            }
        }
    }

    // Geolocation Trigger States
    var geoLabel by remember(initialRoutine) { mutableStateOf("Home") }
    var geoTransition by remember(initialRoutine) { mutableStateOf("ENTER") } // "ENTER" or "EXIT"
    var geoRadiusMeters by remember(initialRoutine) { mutableIntStateOf(150) }
    var geoLatitude by remember(initialRoutine) { mutableStateOf(userLocation.latitude.toString()) }
    var geoLongitude by remember(initialRoutine) { mutableStateOf(userLocation.longitude.toString()) }

    androidx.compose.runtime.LaunchedEffect(selectedTriggerType, hasLocationPermission) {
        if ((selectedTriggerType == TriggerType.SUNRISE || selectedTriggerType == TriggerType.SUNSET || selectedTriggerType == TriggerType.LOCATION) && hasLocationPermission) {
            val loc = LocationHelper.getLastKnownLocation(context)
            userLocation = loc
            if (selectedTriggerType == TriggerType.LOCATION && (geoLatitude.isBlank() || geoLatitude == "0.0")) {
                geoLatitude = loc.latitude.toString()
                geoLongitude = loc.longitude.toString()
            }
        }
    }

    var batteryLevel by remember(initialRoutine) { mutableFloatStateOf(20f) }
    var batteryCharging by remember(initialRoutine) { mutableStateOf(false) }
    var appName by remember(initialRoutine) { mutableStateOf("YouTube") }

    // Initialize trigger values from initialRoutine if present
    remember(initialRoutine) {
        initialRoutine?.triggerConfigJson?.let { jsonStr ->
            try {
                val json = JSONObject(jsonStr)
                when (initialRoutine.triggerType) {
                    TriggerType.TIME -> {
                        scheduleMode = json.optString("scheduleMode", "DAILY")
                        startTime = json.optString("startTime", "08:00")
                        endTime = json.optString("endTime", "22:00")
                        selectedDays = json.optString("days", "Mon, Tue, Wed, Thu, Fri, Sat, Sun")
                        intervalMinutes = json.optInt("intervalMinutes", 60)
                    }
                    TriggerType.SUNRISE, TriggerType.SUNSET -> {
                        solarOffsetMinutes = json.optInt("offsetMinutes", 0)
                        solarDays = json.optString("days", "Daily")
                        if (json.has("latitude") && json.has("longitude")) {
                            val lat = json.optDouble("latitude")
                            val lng = json.optDouble("longitude")
                            val lbl = json.optString("locationLabel", null)
                            if (lat != 0.0 && lng != 0.0) {
                                userLocation = UserLocation(latitude = lat, longitude = lng, label = lbl)
                            }
                        }
                    }
                    TriggerType.LOCATION -> {
                        geoLabel = json.optString("label", "Home")
                        geoTransition = json.optString("transition", "ENTER")
                        geoRadiusMeters = json.optInt("radiusMeters", 150)
                        if (json.has("latitude") && json.has("longitude")) {
                            val lat = json.optDouble("latitude")
                            val lng = json.optDouble("longitude")
                            if (lat != 0.0 && lng != 0.0) {
                                geoLatitude = lat.toString()
                                geoLongitude = lng.toString()
                            }
                        }
                    }
                    TriggerType.WIFI -> {
                        wifiSsid = json.optString("ssid", "Home-5G")
                        wifiConnected = json.optBoolean("connected", true)
                    }
                    TriggerType.BLUETOOTH -> {
                        btDevice = json.optString("device", "Pixel Buds Pro")
                        btConnected = json.optBoolean("connected", true)
                    }
                    TriggerType.BATTERY -> {
                        batteryLevel = json.optInt("level", 20).toFloat()
                        batteryCharging = json.optBoolean("charging", false)
                    }
                    TriggerType.APP -> {
                        appName = json.optString("appName", "YouTube")
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors on init
            }
        }
    }

    // 3. Action Items State (Mapped to corresponding shell commands)
    var aodEnabled by remember(initialRoutine) { mutableStateOf(true) }
    var aodActive by remember(initialRoutine) { mutableStateOf(false) }

    var autoRotateEnabled by remember(initialRoutine) { mutableStateOf(false) }
    var autoRotateActive by remember(initialRoutine) { mutableStateOf(false) }

    var darkModeEnabled by remember(initialRoutine) { mutableStateOf(true) }
    var darkModeActive by remember(initialRoutine) { mutableStateOf(false) }

    var wifiState by remember(initialRoutine) { mutableStateOf(true) }
    var wifiActive by remember(initialRoutine) { mutableStateOf(false) }

    var hotspotState by remember(initialRoutine) { mutableStateOf(true) }
    var hotspotActive by remember(initialRoutine) { mutableStateOf(false) }

    var bluetoothState by remember(initialRoutine) { mutableStateOf(true) }
    var bluetoothActive by remember(initialRoutine) { mutableStateOf(false) }

    var batterySaverState by remember(initialRoutine) { mutableStateOf(true) }
    var batterySaverActive by remember(initialRoutine) { mutableStateOf(false) }

    var timeoutMs by remember(initialRoutine) { mutableIntStateOf(30000) }
    var timeoutActive by remember(initialRoutine) { mutableStateOf(false) }

    var soundProfile by remember(initialRoutine) { mutableStateOf("NORMAL") } // "NORMAL" (Ring), "VIBRATE", "SILENT"
    var soundActive by remember(initialRoutine) { mutableStateOf(false) }
    var soundVolumePercent by remember(initialRoutine) { mutableIntStateOf(70) }
    var soundVolumeEnabled by remember(initialRoutine) { mutableStateOf(true) }

    // Custom user-defined shell command actions
    val customShellCommands = remember { mutableStateListOf<String>() }
    var newCustomCommand by remember { mutableStateOf("") }
    var testExecutionStatus by remember { mutableStateOf<String?>(null) }

    // Populate actions from existing routine
    remember(initialRoutine) {
        val existingActions = initialRoutine?.actions?.ifEmpty { initialRoutine.parseActions() } ?: emptyList()
        for (action in existingActions) {
            when (action.type) {
                ActionType.AOD -> {
                    aodActive = true
                    aodEnabled = action.enabledState
                }
                ActionType.AUTO_ROTATE -> {
                    autoRotateActive = true
                    autoRotateEnabled = action.enabledState
                }
                ActionType.DARK_MODE -> {
                    darkModeActive = true
                    darkModeEnabled = action.enabledState
                }
                ActionType.WIFI -> {
                    wifiActive = true
                    wifiState = action.enabledState
                }
                ActionType.HOTSPOT -> {
                    hotspotActive = true
                    hotspotState = action.enabledState
                }
                ActionType.BLUETOOTH -> {
                    bluetoothActive = true
                    bluetoothState = action.enabledState
                }
                ActionType.BATTERY_SAVER -> {
                    batterySaverActive = true
                    batterySaverState = action.enabledState
                }
                ActionType.SCREEN_TIMEOUT -> {
                    timeoutActive = true
                    timeoutMs = action.intValue
                }
                ActionType.SOUND_PROFILE -> {
                    soundActive = true
                    val prof = action.stringValue.uppercase()
                    soundProfile = if (prof == "SILENT" || prof == "VIBRATE") prof else "NORMAL"
                    if (action.intValue in 0..100) {
                        soundVolumePercent = action.intValue
                        soundVolumeEnabled = true
                    } else if (action.intValue == -1) {
                        soundVolumeEnabled = false
                    }
                }
            }
        }
    }

    // Helper function to build trigger config JSON
    fun buildTriggerConfig(): String {
        val json = JSONObject()
        when (selectedTriggerType) {
            TriggerType.TIME -> {
                val normalizedStart = TimeParser.parseToHourMinute(startTime)?.let {
                    TimeParser.format24Hour(it.first, it.second)
                } ?: startTime
                val normalizedEnd = TimeParser.parseToHourMinute(endTime)?.let {
                    TimeParser.format24Hour(it.first, it.second)
                } ?: endTime
                json.put("scheduleMode", scheduleMode)
                json.put("startTime", normalizedStart)
                json.put("endTime", normalizedEnd)
                json.put("days", selectedDays)
                json.put("intervalMinutes", intervalMinutes)
                json.put("useExactAlarm", true)
            }
            TriggerType.SUNRISE, TriggerType.SUNSET -> {
                json.put("solarEvent", selectedTriggerType.name)
                json.put("offsetMinutes", solarOffsetMinutes)
                json.put("days", solarDays)
                json.put("latitude", userLocation.latitude)
                json.put("longitude", userLocation.longitude)
                json.put("locationLabel", userLocation.label ?: LocationHelper.formatCoordinates(userLocation.latitude, userLocation.longitude))
                json.put("useExactAlarm", true)
            }
            TriggerType.LOCATION -> {
                val lat = geoLatitude.toDoubleOrNull() ?: userLocation.latitude
                val lng = geoLongitude.toDoubleOrNull() ?: userLocation.longitude
                json.put("label", geoLabel.ifBlank { "Selected Location" })
                json.put("transition", geoTransition)
                json.put("radiusMeters", geoRadiusMeters)
                json.put("latitude", lat)
                json.put("longitude", lng)
                json.put("preciseMode", true)
            }
            TriggerType.WIFI -> {
                json.put("ssid", wifiSsid)
                json.put("connected", wifiConnected)
            }
            TriggerType.BLUETOOTH -> {
                json.put("device", btDevice)
                json.put("connected", btConnected)
            }
            TriggerType.BATTERY -> {
                json.put("level", batteryLevel.toInt())
                json.put("charging", batteryCharging)
            }
            TriggerType.APP -> {
                json.put("appName", appName)
                json.put("packageName", "com.google.android.${appName.lowercase()}")
            }
        }
        return json.toString()
    }

    // Helper function to build list of RoutineActionItem
    fun buildActions(): List<RoutineActionItem> {
        val list = mutableListOf<RoutineActionItem>()
        if (aodActive) list.add(RoutineActionItem(type = ActionType.AOD, enabledState = aodEnabled))
        if (autoRotateActive) list.add(RoutineActionItem(type = ActionType.AUTO_ROTATE, enabledState = autoRotateEnabled))
        if (darkModeActive) list.add(RoutineActionItem(type = ActionType.DARK_MODE, enabledState = darkModeEnabled))
        if (wifiActive) list.add(RoutineActionItem(type = ActionType.WIFI, enabledState = wifiState))
        if (hotspotActive) list.add(RoutineActionItem(type = ActionType.HOTSPOT, enabledState = hotspotState))
        if (bluetoothActive) list.add(RoutineActionItem(type = ActionType.BLUETOOTH, enabledState = bluetoothState))
        if (batterySaverActive) list.add(RoutineActionItem(type = ActionType.BATTERY_SAVER, enabledState = batterySaverState))
        if (timeoutActive) list.add(RoutineActionItem(type = ActionType.SCREEN_TIMEOUT, intValue = timeoutMs))
        if (soundActive) {
            val vol = if (soundProfile == "SILENT") 0 else if (soundVolumeEnabled) soundVolumePercent else -1
            list.add(RoutineActionItem(type = ActionType.SOUND_PROFILE, stringValue = soundProfile, intValue = vol))
        }
        return list
    }

    // Helper function to compute shell commands corresponding to chosen actions
    fun getCorrespondingShellCommands(): List<String> {
        val commands = mutableListOf<String>()
        if (aodActive) {
            commands.add("settings put secure doze_always_on ${if (aodEnabled) 1 else 0}")
        }
        if (autoRotateActive) {
            commands.add("settings put system accelerometer_rotation ${if (autoRotateEnabled) 1 else 0}")
        }
        if (darkModeActive) {
            commands.add("cmd uimode night ${if (darkModeEnabled) "yes" else "no"}")
        }
        if (wifiActive) {
            commands.add("svc wifi ${if (wifiState) "enable" else "disable"}")
        }
        if (hotspotActive) {
            commands.add(if (hotspotState) "cmd wifi start-softap || svc wifi startSoftAp" else "cmd wifi stop-softap || svc wifi stopSoftAp")
        }
        if (bluetoothActive) {
            commands.add("cmd bluetooth_manager ${if (bluetoothState) "enable" else "disable"} || svc bluetooth ${if (bluetoothState) "enable" else "disable"}")
        }
        if (batterySaverActive) {
            commands.add("cmd power set-mode ${if (batterySaverState) 1 else 0}")
        }
        if (timeoutActive) {
            commands.add("settings put system screen_off_timeout $timeoutMs")
        }
        if (soundActive) {
            val ringerCode = when (soundProfile.uppercase()) {
                "SILENT" -> 0
                "VIBRATE" -> 1
                else -> 2
            }
            if (soundProfile == "SILENT") {
                commands.add("cmd audio set-ringer-mode 0 && settings put global zen_mode 2")
            } else if (soundProfile == "VIBRATE") {
                commands.add("cmd audio set-ringer-mode 1 && settings put global zen_mode 0")
            } else {
                if (soundVolumeEnabled) {
                    commands.add("cmd audio set-ringer-mode 2 && media volume --stream 3 --set $soundVolumePercent")
                } else {
                    commands.add("cmd audio set-ringer-mode 2")
                }
            }
        }
        commands.addAll(customShellCommands)
        return commands
    }

    val correspondingCommands = getCorrespondingShellCommands()

    // Real-time calculation of next trigger time via AlarmManager helper
    val currentTriggerJson = buildTriggerConfig()
    val nextScheduledTriggerMillis = remember(selectedTriggerType, scheduleMode, startTime, selectedDays, intervalMinutes, solarOffsetMinutes, solarDays, userLocation) {
        if (selectedTriggerType == TriggerType.TIME ||
            selectedTriggerType == TriggerType.SUNRISE ||
            selectedTriggerType == TriggerType.SUNSET) {
            RoutineAlarmManager.calculateNextTriggerMillis(context, selectedTriggerType, currentTriggerJson)
        } else null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ==========================================
        // SECTION 1: Routine Name & Identity
        // ==========================================
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Routine Identity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    label = { Text("Routine Name *") },
                    placeholder = { Text("e.g. Bedtime Power Saver, Work Focus, Morning Wakeup") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Turn off AOD and activate silent mode on weeknights") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_description_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Color accent picker
                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val colorOptions = listOf(
                    "#3871E0" to "Pixel Blue",
                    "#1E8E3E" to "Emerald",
                    "#E8710A" to "Amber",
                    "#D93025" to "Coral",
                    "#9334E6" to "Purple"
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEach { (hex, _) ->
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 2: Scheduled Trigger Selection (AlarmManager)
        // ==========================================
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "2. Select Trigger (If This Happens)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Choose condition type from the predefined trigger list:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TriggerType.values().forEach { type ->
                        val isSelected = selectedTriggerType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTriggerType = type },
                            label = { Text(type.displayName) },
                            leadingIcon = {
                                val icon = when (type) {
                                    TriggerType.TIME -> Icons.Default.Alarm
                                    TriggerType.SUNRISE -> Icons.Default.LightMode
                                    TriggerType.SUNSET -> Icons.Default.Nightlight
                                    TriggerType.LOCATION -> Icons.Default.LocationOn
                                    TriggerType.WIFI -> Icons.Default.Wifi
                                    TriggerType.BLUETOOTH -> Icons.Default.Bluetooth
                                    TriggerType.BATTERY -> Icons.Default.BatteryChargingFull
                                    TriggerType.APP -> Icons.Default.PlayArrow
                                }
                                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.testTag("trigger_type_chip_${type.name}")
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Detailed configuration for the selected trigger
                when (selectedTriggerType) {
                    TriggerType.TIME -> {
                        // ----------------------------------------------------
                        // Scheduled Triggers: Precision AlarmManager Settings
                        // ----------------------------------------------------
                        Text(
                            text = "Scheduled Trigger Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Mode Selector Chips
                        val scheduleModes = listOf(
                            "DAILY" to "Daily",
                            "WEEKLY" to "Weekly Days",
                            "SPECIFIC_TIME" to "Specific Time",
                            "INTERVAL" to "Interval Repeat"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            scheduleModes.forEach { (modeKey, modeTitle) ->
                                val isModeSelected = scheduleMode == modeKey
                                FilterChip(
                                    selected = isModeSelected,
                                    onClick = { scheduleMode = modeKey },
                                    label = { Text(modeTitle) },
                                    leadingIcon = {
                                        val icon = when (modeKey) {
                                            "DAILY" -> Icons.Default.Schedule
                                            "WEEKLY" -> Icons.Default.CalendarMonth
                                            "SPECIFIC_TIME" -> Icons.Default.Alarm
                                            "INTERVAL" -> Icons.Default.Repeat
                                            else -> Icons.Default.Schedule
                                        }
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.testTag("schedule_mode_$modeKey")
                                )
                            }
                        }

                        // Time Input & Presets for non-interval modes
                        if (scheduleMode != "INTERVAL") {
                            Text(
                                text = "Trigger Time (Exact)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            val parsedTime = TimeParser.parseToHourMinute(startTime)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { startTime = it },
                                    label = { Text("Time (e.g. 9:00 PM, 21:00)") },
                                    placeholder = { Text("21:00 or 9:00 PM") },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val (initHour, initMin) = parsedTime ?: Pair(21, 0)
                                                TimePickerDialog(
                                                    context,
                                                    { _, hourOfDay, minute ->
                                                        startTime = TimeParser.format24Hour(hourOfDay, minute)
                                                    },
                                                    initHour,
                                                    initMin,
                                                    false
                                                ).show()
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.AccessTime,
                                                contentDescription = "Pick Clock",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("time_start_input"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                FilledTonalButton(
                                    onClick = {
                                        val (initHour, initMin) = parsedTime ?: Pair(21, 0)
                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                startTime = TimeParser.format24Hour(hourOfDay, minute)
                                            },
                                            initHour,
                                            initMin,
                                            false
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("pick_time_clock_btn")
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick")
                                }
                            }

                            // Dynamic Confirmation Badge
                            if (parsedTime != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Will trigger at: ${TimeParser.format12Hour(parsedTime.first, parsedTime.second)} (24h: ${TimeParser.format24Hour(parsedTime.first, parsedTime.second)})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "⚠ Please enter a valid time (e.g. \"9:00 PM\", \"21:00\", or \"9pm\") or tap Pick.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            // Quick Time Presets
                            Text(
                                text = "Quick Time Presets:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    "07:00" to "🌅 7:00 AM",
                                    "08:30" to "💼 8:30 AM",
                                    "12:00" to "☀️ 12:00 PM",
                                    "18:00" to "🌆 6:00 PM",
                                    "21:00" to "🌙 9:00 PM",
                                    "22:30" to "🛏️ 10:30 PM"
                                ).forEach { (timeVal, timeLabel) ->
                                    val isSelected = parsedTime != null &&
                                            TimeParser.format24Hour(parsedTime.first, parsedTime.second) == timeVal
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .clickable { startTime = timeVal }
                                            .padding(2.dp)
                                    ) {
                                        Text(
                                            text = timeLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Days of week selector for DAILY / WEEKLY
                        if (scheduleMode == "WEEKLY" || scheduleMode == "DAILY") {
                            Text(
                                text = "Active Days of Week",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            val currentDaysList = remember(selectedDays) {
                                if (selectedDays.equals("Daily", ignoreCase = true) ||
                                    selectedDays.equals("Every Day", ignoreCase = true)
                                ) {
                                    daysOfWeek
                                } else {
                                    selectedDays.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                daysOfWeek.forEach { day ->
                                    val isDaySelected = currentDaysList.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDaySelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable {
                                                val updated = currentDaysList.toMutableList()
                                                if (isDaySelected) {
                                                    if (updated.size > 1) updated.remove(day)
                                                } else {
                                                    updated.add(day)
                                                }
                                                selectedDays = if (updated.size == 7) "Daily" else updated.joinToString(", ")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.first().toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDaySelected)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Day Preset Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        selectedDays = "Mon, Tue, Wed, Thu, Fri, Sat, Sun"
                                    }
                                ) {
                                    Text("Every Day", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        selectedDays = "Mon, Tue, Wed, Thu, Fri"
                                    }
                                ) {
                                    Text("Weekdays (Mon-Fri)", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        selectedDays = "Sat, Sun"
                                    }
                                ) {
                                    Text("Weekends (Sat-Sun)", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }

                        // Interval Repetition Selector
                        if (scheduleMode == "INTERVAL") {
                            Text(
                                text = "Repeating Interval (Every ${if (intervalMinutes >= 60) "${intervalMinutes / 60} hour(s)" else "$intervalMinutes minutes"})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(15, 30, 60, 120, 240, 360, 720).forEach { mins ->
                                    val isSelected = intervalMinutes == mins
                                    val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { intervalMinutes = mins },
                                        label = { Text("Every $label") }
                                    )
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // AlarmManager Precision Status & Next Trigger Badge
                        // ----------------------------------------------------
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AlarmManager Precision Trigger (RTC_WAKEUP)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = "Guarantees exact wake-up even while your device is in deep Doze mode, firing the routine actions precisely on schedule.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (nextScheduledTriggerMillis != null) {
                                    val formattedNext = RoutineAlarmManager.formatNextTriggerHumanReadable(nextScheduledTriggerMillis)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "⏰ Next Scheduled Execution:\n$formattedNext",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Quick Test Precision Alarm
                                OutlinedButton(
                                    onClick = {
                                        alarmTestStatus = "Armed! Test exact alarm will fire in 5s..."
                                        RoutineAlarmManager.scheduleTestAlarm(context, initialRoutine?.id ?: 9999L, delaySeconds = 5)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("test_alarm_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Exact Alarm (Fires in 5s)")
                                }

                                alarmTestStatus?.let { status ->
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    TriggerType.SUNRISE, TriggerType.SUNSET -> {
                        val isSunrise = selectedTriggerType == TriggerType.SUNRISE
                        val eventName = if (isSunrise) "Sunrise" else "Sunset"
                        val eventColor = if (isSunrise) Color(0xFFF57C00) else Color(0xFF5E35B1)
                        val eventIcon = if (isSunrise) Icons.Default.LightMode else Icons.Default.Nightlight

                        // Header Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = eventColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, eventColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(eventColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = eventIcon,
                                        contentDescription = null,
                                        tint = eventColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "$eventName Routine Trigger",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Fires based on real-time astronomical calculation for your location",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 1. Real Location Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "User Location",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (hasLocationPermission) {
                                                isRefreshingLocation = true
                                                LocationHelper.requestFreshLocation(context) { loc ->
                                                    userLocation = loc
                                                    isRefreshingLocation = false
                                                }
                                            } else {
                                                locationPermissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("refresh_location_btn")
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Refresh Location",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                val displayLabel = userLocation.label ?: LocationHelper.formatCoordinates(userLocation.latitude, userLocation.longitude)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = displayLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Coordinates: ${LocationHelper.formatCoordinates(userLocation.latitude, userLocation.longitude)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (hasLocationPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                text = if (hasLocationPermission) "GPS Ready" else "Using Default",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = if (hasLocationPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }

                                if (!hasLocationPermission) {
                                    Button(
                                        onClick = {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("grant_location_permission_btn"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Grant Location Access for Exact Solar Times")
                                    }
                                }
                            }
                        }

                        // 2. Today's Solar Calculations Preview
                        val todaySolar = remember(userLocation) {
                            SolarCalculator.calculateSolarTimes(Calendar.getInstance(), userLocation.latitude, userLocation.longitude)
                        }

                        Text(
                            text = "Today's Solar Schedule at Your Location",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Sunrise card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSunrise) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, if (isSunrise) Color(0xFFF57C00) else Color.Transparent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LightMode, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sunrise", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val sunriseTimeStr = todaySolar.sunriseMillis?.let {
                                        val cal = Calendar.getInstance().apply { timeInMillis = it }
                                        TimeParser.format12Hour(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                                    } ?: "N/A"
                                    Text(
                                        text = sunriseTimeStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSunrise) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Sunset card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!isSunrise) Color(0xFFEDE7F6) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, if (!isSunrise) Color(0xFF5E35B1) else Color.Transparent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Nightlight, contentDescription = null, tint = Color(0xFF5E35B1), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sunset", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val sunsetTimeStr = todaySolar.sunsetMillis?.let {
                                        val cal = Calendar.getInstance().apply { timeInMillis = it }
                                        TimeParser.format12Hour(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                                    } ?: "N/A"
                                    Text(
                                        text = sunsetTimeStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (!isSunrise) Color(0xFF512DA8) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // 3. Offset selector ("When to trigger")
                        Text(
                            text = "Trigger Timing Offset",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        val offsetOptions = listOf(
                            -60 to "1h before",
                            -30 to "30m before",
                            -15 to "15m before",
                            0 to "At $eventName",
                            15 to "15m after",
                            30 to "30m after",
                            60 to "1h after"
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            offsetOptions.forEach { (offset, label) ->
                                val isSelected = solarOffsetMinutes == offset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { solarOffsetMinutes = offset },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("solar_offset_${offset}")
                                )
                            }
                        }

                        // 4. Days of the week selection
                        Text(
                            text = "Repeat Days",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        val dayOptions = listOf("Daily", "Weekdays", "Weekends")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dayOptions.forEach { opt ->
                                val isSelected = solarDays.equals(opt, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { solarDays = opt },
                                    label = { Text(opt) },
                                    modifier = Modifier.weight(1f).testTag("solar_days_${opt.lowercase()}")
                                )
                            }
                        }

                        // 5. Next trigger preview badge
                        val solarNextMillis = remember(selectedTriggerType, solarOffsetMinutes, solarDays, userLocation) {
                            val cfg = buildTriggerConfig()
                            RoutineAlarmManager.calculateNextTriggerMillis(context, selectedTriggerType, cfg)
                        }

                        if (solarNextMillis != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = eventColor.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = eventIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = eventColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Next trigger: ${RoutineAlarmManager.formatNextTriggerHumanReadable(solarNextMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    TriggerType.LOCATION -> {
                        val geoThemeColor = Color(0xFF00897B) // Precision Emerald / Teal for Geolocation

                        // Header Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = geoThemeColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, geoThemeColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(geoThemeColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = geoThemeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Precision Geolocation Trigger",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Trigger actions when entering or leaving a precise geographic boundary using GPS & network location.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 1. Permission status card if not granted
                        if (!hasLocationPermission) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Location Permission Required",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Precise Geofencing requires location access to monitor boundaries in the background.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("grant_geo_permission_btn")
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Grant Precise Location Access")
                                    }
                                }
                            }
                        }

                        // 2. Location Label & Presets
                        Text(
                            text = "Target Area Name / Label",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = geoLabel,
                            onValueChange = { geoLabel = it },
                            label = { Text("Location Name") },
                            placeholder = { Text("e.g. Home, Office, Gym") },
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, tint = geoThemeColor)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("geo_label_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Quick place presets
                        val placePresets = listOf("Home", "Office", "Gym", "School", "Store")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            placePresets.forEach { preset ->
                                val isSelected = geoLabel.equals(preset, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { geoLabel = preset },
                                    label = { Text(preset, fontSize = 12.sp) },
                                    modifier = Modifier.testTag("preset_$preset")
                                )
                            }
                        }

                        // 3. Current Location & Coordinate Tools
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = null,
                                            tint = geoThemeColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Device GPS Location",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            isRefreshingLocation = true
                                            LocationHelper.requestFreshLocation(context) { freshLoc ->
                                                userLocation = freshLoc
                                                geoLatitude = freshLoc.latitude.toString()
                                                geoLongitude = freshLoc.longitude.toString()
                                                isRefreshingLocation = false
                                            }
                                        },
                                        enabled = !isRefreshingLocation,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("refresh_geo_location_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isRefreshingLocation) "Updating..." else "Use Current GPS", fontSize = 12.sp)
                                    }
                                }

                                Text(
                                    text = "Current: ${LocationHelper.formatCoordinates(userLocation.latitude, userLocation.longitude)} (${userLocation.provider})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Manual coordinate coordinates
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = geoLatitude,
                                        onValueChange = { geoLatitude = it },
                                        label = { Text("Latitude") },
                                        placeholder = { Text("37.7749") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("geo_lat_input"),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = geoLongitude,
                                        onValueChange = { geoLongitude = it },
                                        label = { Text("Longitude") },
                                        placeholder = { Text("-122.4194") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("geo_lng_input"),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        // 4. Geofence Transition Type (Enter vs Exit)
                        Text(
                            text = "Trigger Event (Condition)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = geoTransition == "ENTER",
                                onClick = { geoTransition = "ENTER" },
                                label = { Text("When Arriving (Enter)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("geo_transition_enter")
                            )
                            FilterChip(
                                selected = geoTransition == "EXIT",
                                onClick = { geoTransition = "EXIT" },
                                label = { Text("When Leaving (Exit)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("geo_transition_exit")
                            )
                        }

                        // 5. Geofence Precision Radius
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Geofence Radius (Precision)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = geoThemeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${geoRadiusMeters} meters",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = geoThemeColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Slider(
                            value = geoRadiusMeters.toFloat(),
                            onValueChange = { geoRadiusMeters = it.toInt() },
                            valueRange = 50f..1000f,
                            steps = 18,
                            modifier = Modifier.fillMaxWidth().testTag("geo_radius_slider")
                        )

                        val radiusPresets = listOf(50 to "50m (Precise)", 100 to "100m", 250 to "250m", 500 to "500m", 1000 to "1km")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            radiusPresets.forEach { (radiusVal, labelText) ->
                                val isSelected = geoRadiusMeters == radiusVal
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { geoRadiusMeters = radiusVal },
                                    label = { Text(labelText, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("geo_radius_${radiusVal}")
                                )
                            }
                        }

                        // 6. Real-time Distance & Geofence Status Preview
                        val parsedLat = geoLatitude.toDoubleOrNull()
                        val parsedLng = geoLongitude.toDoubleOrNull()
                        if (parsedLat != null && parsedLng != null) {
                            val currentDist = GeofenceManager.computeDistanceMeters(
                                userLocation.latitude,
                                userLocation.longitude,
                                parsedLat,
                                parsedLng
                            )
                            val isCurrentlyInside = currentDist <= geoRadiusMeters

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrentlyInside) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrentlyInside) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentlyInside) Icons.Default.CheckCircle else Icons.Default.Place,
                                        contentDescription = null,
                                        tint = if (isCurrentlyInside) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isCurrentlyInside) "Currently INSIDE Geofence zone" else "Currently OUTSIDE Geofence zone",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentlyInside) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Distance: ~${currentDist.toInt()}m from center point (Radius: ${geoRadiusMeters}m)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 7. Technology Notice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = geoThemeColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hardware GPS Proximity Alert + WorkManager background fail-safe monitoring enabled.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TriggerType.WIFI -> {
                        Text(
                            text = "Wi-Fi Trigger Settings",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("Wi-Fi Network SSID") },
                            placeholder = { Text("e.g. Home_Network_5G") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (wifiConnected) "Trigger when connected" else "Trigger when disconnected")
                            Switch(
                                checked = wifiConnected,
                                onCheckedChange = { wifiConnected = it }
                            )
                        }
                    }

                    TriggerType.BLUETOOTH -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Saved Bluetooth Devices",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = {
                                    if (hasBtPermission) {
                                        savedBtDevices = BluetoothHelper.getSavedBluetoothDevices(context)
                                    } else {
                                        btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                    }
                                },
                                modifier = Modifier.size(32.dp).testTag("refresh_bt_devices_btn")
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh Devices",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Permission request banner if BLUETOOTH_CONNECT not granted on Android 12+
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !hasBtPermission) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Permission Required for Saved Devices",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = "Allow access to show your actual paired Bluetooth devices (car, headphones, smartwatch).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                                        modifier = Modifier.fillMaxWidth().testTag("grant_bt_permission_btn")
                                    ) {
                                        Text("Allow Access to Saved Devices")
                                    }
                                }
                            }
                        }

                        // Option 1: Any Paired Device
                        val isAnySelected = btDevice == "Any Paired Device" || btDevice.isBlank() || btDevice.equals("Any Device", ignoreCase = true)
                        Surface(
                            onClick = { btDevice = "Any Paired Device" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAnySelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isAnySelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier.fillMaxWidth().testTag("bt_device_any")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isAnySelected,
                                    onClick = { btDevice = "Any Paired Device" }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Any Paired Device", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Triggers when any paired device connects or disconnects",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Option 2: Actual Saved/Paired Bluetooth Devices
                        if (savedBtDevices.isNotEmpty()) {
                            Text(
                                text = "Paired Accessories (${savedBtDevices.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            savedBtDevices.forEach { dev ->
                                val isSelected = btDevice.equals(dev.name, ignoreCase = true) || btDevice.equals(dev.address, ignoreCase = true)
                                Surface(
                                    onClick = { btDevice = dev.name },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth().testTag("bt_saved_device_${dev.address}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { btDevice = dev.name }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dev.name,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "MAC: ${dev.address}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (hasBtPermission) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "No paired Bluetooth accessories found on this device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Option 3: Custom or Unpaired Device Name input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Or specify custom device name",
                                style = MaterialTheme.typography.labelMedium
                            )
                            TextButton(onClick = { showCustomBtInput = !showCustomBtInput }) {
                                Text(if (showCustomBtInput) "Hide" else "Enter Name")
                            }
                        }

                        if (showCustomBtInput) {
                            OutlinedTextField(
                                value = btDevice,
                                onValueChange = { btDevice = it },
                                label = { Text("Bluetooth Device Name") },
                                placeholder = { Text("e.g. Car Audio, Pixel Buds Pro") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("bt_custom_name_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Trigger Condition (Connect vs Disconnect)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (btConnected) "Trigger when connected" else "Trigger when disconnected",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (btConnected) "Runs routine when this device connects" else "Runs routine when disconnected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = btConnected,
                                onCheckedChange = { btConnected = it },
                                modifier = Modifier.testTag("bt_trigger_connection_switch")
                            )
                        }
                    }

                    TriggerType.BATTERY -> {
                        Text(
                            text = "Battery Threshold: ${batteryLevel.toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = batteryLevel,
                            onValueChange = { batteryLevel = it },
                            valueRange = 5f..100f,
                            steps = 18,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Trigger on Charger Connected")
                            Switch(
                                checked = batteryCharging,
                                onCheckedChange = { batteryCharging = it }
                            )
                        }
                    }

                    TriggerType.APP -> {
                        Text(
                            text = "App Launch Settings",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = appName,
                            onValueChange = { appName = it },
                            label = { Text("Application Name") },
                            placeholder = { Text("e.g. YouTube, Maps, Kindle") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // SECTION 3: Action Definitions & Shell Commands
        // ==========================================
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "3. Actions & Shell Commands (Then Do That)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Configure actions that map directly to privileged shell commands via Shizuku:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Action Item 1: Always-On Display (AOD)
                ActionSettingCard(
                    title = "Always-On Display (AOD)",
                    shellCommand = "settings put secure doze_always_on ${if (aodEnabled) 1 else 0}",
                    icon = Icons.Default.LightMode,
                    isActive = aodActive,
                    onActiveChange = { aodActive = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (aodEnabled) "Set AOD: Enabled" else "Set AOD: Disabled")
                        Switch(checked = aodEnabled, onCheckedChange = { aodEnabled = it })
                    }
                }

                // Action Item 2: Auto-Rotate
                ActionSettingCard(
                    title = "Screen Auto-Rotation",
                    shellCommand = "settings put system accelerometer_rotation ${if (autoRotateEnabled) 1 else 0}",
                    icon = Icons.Default.ScreenRotation,
                    isActive = autoRotateActive,
                    onActiveChange = { autoRotateActive = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (autoRotateEnabled) "Auto-Rotate: On" else "Auto-Rotate: Off (Portrait)")
                        Switch(checked = autoRotateEnabled, onCheckedChange = { autoRotateEnabled = it })
                    }
                }

                // Action Item 3: Dark Mode
                ActionSettingCard(
                    title = "Dark Theme",
                    shellCommand = "cmd uimode night ${if (darkModeEnabled) "yes" else "no"}",
                    icon = Icons.Default.Nightlight,
                    isActive = darkModeActive,
                    onActiveChange = { darkModeActive = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (darkModeEnabled) "Dark Mode: Always On" else "Light Mode")
                        Switch(checked = darkModeEnabled, onCheckedChange = { darkModeEnabled = it })
                    }
                }

                // Action Item 4: Battery Saver
                ActionSettingCard(
                    title = "Battery Saver Mode",
                    shellCommand = "cmd power set-mode ${if (batterySaverState) 1 else 0}",
                    icon = Icons.Default.BatteryChargingFull,
                    isActive = batterySaverActive,
                    onActiveChange = { batterySaverActive = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (batterySaverState) "Battery Saver: Turn On" else "Battery Saver: Turn Off")
                        Switch(checked = batterySaverState, onCheckedChange = { batterySaverState = it })
                    }
                }

                // Action Item 5: Wi-Fi Power
                ActionSettingCard(
                    title = "Wi-Fi Power",
                    shellCommand = "svc wifi ${if (wifiState) "enable" else "disable"}",
                    icon = Icons.Default.Wifi,
                    isActive = wifiActive,
                    onActiveChange = { wifiActive = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (wifiState) "Turn Wi-Fi On" else "Turn Wi-Fi Off")
                        Switch(checked = wifiState, onCheckedChange = { wifiState = it })
                    }
                }

                // Action Item 5b: Wi-Fi Hotspot
                val hotspotShellCmd = if (hotspotState) "cmd wifi start-softap || svc wifi startSoftAp" else "cmd wifi stop-softap || svc wifi stopSoftAp"
                ActionSettingCard(
                    title = "Wi-Fi Hotspot",
                    shellCommand = hotspotShellCmd,
                    icon = Icons.Default.WifiTethering,
                    isActive = hotspotActive,
                    onActiveChange = { hotspotActive = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (hotspotState) "Turn Hotspot On" else "Turn Hotspot Off")
                        Switch(checked = hotspotState, onCheckedChange = { hotspotState = it })
                    }
                }

                // Action Item 6: Screen Timeout
                ActionSettingCard(
                    title = "Screen Off Timeout",
                    shellCommand = "settings put system screen_off_timeout $timeoutMs",
                    icon = Icons.Default.HourglassTop,
                    isActive = timeoutActive,
                    onActiveChange = { timeoutActive = it }
                ) {
                    val seconds = timeoutMs / 1000
                    Column {
                        Text("Timeout: ${seconds}s")
                        Slider(
                            value = timeoutMs.toFloat(),
                            onValueChange = { timeoutMs = it.toInt() },
                            valueRange = 15000f..300000f,
                            steps = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Action Item 7: Bluetooth Power
                ActionSettingCard(
                    title = "Bluetooth Power",
                    shellCommand = "cmd bluetooth_manager ${if (bluetoothState) "enable" else "disable"} || svc bluetooth ${if (bluetoothState) "enable" else "disable"}",
                    icon = Icons.Default.Bluetooth,
                    isActive = bluetoothActive,
                    onActiveChange = { bluetoothActive = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (bluetoothState) "Turn Bluetooth On" else "Turn Bluetooth Off")
                        Switch(checked = bluetoothState, onCheckedChange = { bluetoothState = it })
                    }
                }

                // Action Item 8: Sound & Volume Profile (Ring, Vibrate, Silent, Volume Slider)
                val soundShellCmd = when (soundProfile.uppercase()) {
                    "SILENT" -> "cmd audio set-ringer-mode 0 && settings put global zen_mode 2"
                    "VIBRATE" -> "cmd audio set-ringer-mode 1 && settings put global zen_mode 0"
                    else -> if (soundVolumeEnabled) "cmd audio set-ringer-mode 2 && media volume --stream 3 --set $soundVolumePercent%" else "cmd audio set-ringer-mode 2"
                }

                val soundIcon = when (soundProfile.uppercase()) {
                    "SILENT" -> Icons.Default.VolumeOff
                    "VIBRATE" -> Icons.Default.Vibration
                    else -> if (soundVolumeEnabled && soundVolumePercent == 0) Icons.Default.VolumeMute else Icons.Default.VolumeUp
                }

                ActionSettingCard(
                    title = "Sound & Volume Profile",
                    shellCommand = soundShellCmd,
                    icon = soundIcon,
                    isActive = soundActive,
                    onActiveChange = { soundActive = it }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Ringer & Sound Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Mode Selection: Ring, Vibrate, Silent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Ring / Normal
                            FilterChip(
                                selected = soundProfile == "NORMAL" || soundProfile == "RING",
                                onClick = { soundProfile = "NORMAL" },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Ring") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sound_mode_ring_chip")
                            )

                            // 2. Vibrate
                            FilterChip(
                                selected = soundProfile == "VIBRATE",
                                onClick = { soundProfile = "VIBRATE" },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Vibrate") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sound_mode_vibrate_chip")
                            )

                            // 3. Silent
                            FilterChip(
                                selected = soundProfile == "SILENT",
                                onClick = { soundProfile = "SILENT" },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VolumeOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Silent") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sound_mode_silent_chip")
                            )
                        }

                        // Mode Description Banner
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when (soundProfile.uppercase()) {
                                    "SILENT" -> "Mutes all sounds, vibrations, and notifications (engages Do Not Disturb)."
                                    "VIBRATE" -> "Silences ringtones while keeping haptic vibrations active for calls and alerts."
                                    else -> "Enables normal ringtone and audio playback at specified volume level."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Volume Control (For Ring / Normal mode, and optional for other modes)
                        if (soundProfile != "SILENT") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Automate Volume Level",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Switch(
                                    checked = soundVolumeEnabled,
                                    onCheckedChange = { soundVolumeEnabled = it },
                                    modifier = Modifier.testTag("sound_volume_switch")
                                )
                            }

                            if (soundVolumeEnabled) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Media & Ring Volume",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$soundVolumePercent%",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Slider(
                                        value = soundVolumePercent.toFloat(),
                                        onValueChange = { soundVolumePercent = it.toInt() },
                                        valueRange = 0f..100f,
                                        steps = 19, // 5% increments
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("sound_volume_slider")
                                    )

                                    // Quick Presets Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(0 to "Mute", 25 to "25%", 50 to "50%", 75 to "75%", 100 to "100%").forEach { (vol, label) ->
                                            FilterChip(
                                                selected = soundVolumePercent == vol,
                                                onClick = { soundVolumePercent = vol },
                                                label = { Text(label, fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom Shell Commands Input
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Custom Shell Command Action",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCustomCommand,
                        onValueChange = { newCustomCommand = it },
                        placeholder = { Text("e.g. settings put global zen_mode 1") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_command_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    IconButton(
                        onClick = {
                            if (newCustomCommand.isNotBlank()) {
                                customShellCommands.add(newCustomCommand.trim())
                                newCustomCommand = ""
                            }
                        },
                        modifier = Modifier.testTag("add_custom_command_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Command")
                    }
                }

                if (customShellCommands.isNotEmpty()) {
                    Text(
                        text = "Added Custom Shell Commands:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    customShellCommands.forEachIndexed { index, cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E24))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cmd,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF81C995),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { customShellCommands.removeAt(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.LightGray)
                            }
                        }
                    }
                }

                // Command Inspector Box
                if (correspondingCommands.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generated Shell Script Execution Plan:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF16161D))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            correspondingCommands.forEach { cmd ->
                                Text(
                                    text = "$ $cmd",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64B5F6)
                                )
                            }
                        }
                    }
                }

                // Test Shell Commands Live via Shizuku
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            testExecutionStatus = "Executing commands via Shizuku..."
                            var anyFailure = false
                            val results = mutableListOf<String>()
                            for (cmd in correspondingCommands) {
                                val res = ShizukuServiceHelper.executeCommand(cmd)
                                if (!res.isSuccess) anyFailure = true
                                results.add("$cmd -> ${if (res.isSuccess) "OK" else "FAIL"}")
                            }
                            testExecutionStatus = if (anyFailure) {
                                "Test finished with warnings: " + results.joinToString(", ")
                            } else {
                                "Test successful! All ${correspondingCommands.size} commands executed."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_commands_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dry-Run Shell Commands Now")
                }

                testExecutionStatus?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.contains("successful")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ==========================================
        // SECTION 4: Save & Cancel Buttons
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("cancel_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    val finalName = routineName.trim().ifBlank { "Untitled Routine" }
                    val actions = buildActions()
                    val trigger = RoutineTrigger(
                        type = selectedTriggerType,
                        configJson = buildTriggerConfig()
                    )

                    val routine = Routine(
                        id = initialRoutine?.id ?: 0L,
                        name = finalName,
                        isEnabled = initialRoutine?.isEnabled ?: true,
                        triggers = listOf(trigger),
                        actions = actions,
                        description = description.trim(),
                        iconName = selectedIcon,
                        colorHex = selectedColor,
                        lastExecutedTimestamp = initialRoutine?.lastExecutedTimestamp ?: 0L,
                        lastExecutionStatus = initialRoutine?.lastExecutionStatus ?: "IDLE",
                        createdAt = initialRoutine?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(routine)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("save_routine_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Routine")
            }
        }
    }
}

/**
 * Reusable card for defining an action with its corresponding privileged shell command.
 */
@Composable
private fun ActionSettingCard(
    title: String,
    shellCommand: String,
    icon: ImageVector,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = onActiveChange
                )
            }

            // Display corresponding shell command tag
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Command: $shellCommand",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )

            if (isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}
