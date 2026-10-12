package com.vpmapps.pixelflow.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpmapps.pixelflow.background.GeofenceManager
import com.vpmapps.pixelflow.background.RoutineAlarmManager
import com.vpmapps.pixelflow.data.model.ActionType
import com.vpmapps.pixelflow.data.model.CustomNotificationConfig
import com.vpmapps.pixelflow.data.model.Routine
import com.vpmapps.pixelflow.data.model.RoutineActionItem
import com.vpmapps.pixelflow.data.model.RoutineTrigger
import com.vpmapps.pixelflow.data.model.TriggerType
import com.vpmapps.pixelflow.shizuku.ShizukuServiceHelper
import com.vpmapps.pixelflow.util.AppInfoHelper
import com.vpmapps.pixelflow.util.BluetoothHelper
import com.vpmapps.pixelflow.util.InstalledAppInfo
import com.vpmapps.pixelflow.util.LocationHelper
import com.vpmapps.pixelflow.util.SavedBluetoothDevice
import com.vpmapps.pixelflow.ui.util.rememberAppHapticFeedback
import com.vpmapps.pixelflow.util.SolarCalculator
import com.vpmapps.pixelflow.util.TimeParser
import com.vpmapps.pixelflow.util.UserLocation
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val haptics = rememberAppHapticFeedback()
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

    // When Routine Ends restoration setting
    var restoreOnEnd by remember(initialRoutine) {
        mutableStateOf(initialRoutine?.restoreOnEnd ?: true)
    }

    // 2. Predefined Trigger Selection State
    var selectedTriggerType by remember(initialRoutine) {
        mutableStateOf(initialRoutine?.triggerType ?: TriggerType.TIME)
    }

    // Scheduled Trigger State (AlarmManager Integration)
    var scheduleMode by remember(initialRoutine) { mutableStateOf("DAILY") } // "SPECIFIC_TIME", "DAILY", "WEEKLY", "INTERVAL"
    var startTime by remember(initialRoutine) { mutableStateOf("08:00") }
    var endTime by remember(initialRoutine) { mutableStateOf("22:00") }
    var hasEndTime by remember(initialRoutine) { mutableStateOf(false) }
    var durationHours by remember(initialRoutine) { mutableStateOf("4") }
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
    var appPackage by remember(initialRoutine) { mutableStateOf("com.google.android.youtube") }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }
    var showCustomAppInput by remember { mutableStateOf(false) }
    var appFilterMode by remember { mutableStateOf("ALL") }
    var showAllAppsExpanded by remember { mutableStateOf(false) }

    // Fetch installed apps off main thread when APP trigger is active
    androidx.compose.runtime.LaunchedEffect(selectedTriggerType) {
        if (selectedTriggerType == TriggerType.APP && installedApps.isEmpty()) {
            isLoadingApps = true
            withContext(Dispatchers.IO) {
                val list = AppInfoHelper.getInstalledApps(context)
                withContext(Dispatchers.Main) {
                    installedApps = list
                    isLoadingApps = false
                }
            }
        }
    }

    val refreshInstalledApps = {
        coroutineScope.launch {
            isLoadingApps = true
            withContext(Dispatchers.IO) {
                val list = AppInfoHelper.getInstalledApps(context)
                withContext(Dispatchers.Main) {
                    installedApps = list
                    isLoadingApps = false
                }
            }
        }
    }

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
                        hasEndTime = json.optBoolean("hasEndTime", json.has("endTime") && json.optString("endTime", "").isNotBlank() && json.optString("endTime") != json.optString("startTime"))
                        durationHours = json.optString("durationHours", "4")
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
                            val lbl = json.optString("locationLabel", null)
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
                        appPackage = json.optString("packageName", "com.google.android.youtube")
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

    // Custom Notification action state
    var customNotificationActive by remember(initialRoutine) { mutableStateOf(false) }
    var customNotificationTitle by remember(initialRoutine) { mutableStateOf("Routine Alert") }
    var customNotificationMessage by remember(initialRoutine) { mutableStateOf("") }
    var customNotificationIcon by remember(initialRoutine) { mutableStateOf("bell") }
    var customNotificationScheduleType by remember(initialRoutine) { mutableStateOf("IMMEDIATE") } // "IMMEDIATE", "ONCE", "DAILY"
    var customNotificationTime by remember(initialRoutine) { mutableStateOf("09:00") }

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
                ActionType.CUSTOM_NOTIFICATION -> {
                    customNotificationActive = true
                    val cfg = CustomNotificationConfig.fromJsonString(action.stringValue)
                    customNotificationTitle = cfg.title
                    customNotificationMessage = cfg.message
                    customNotificationIcon = cfg.iconName
                    customNotificationScheduleType = cfg.scheduleType
                    customNotificationTime = cfg.time
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
                val normalizedEnd = if (hasEndTime) {
                    TimeParser.parseToHourMinute(endTime)?.let {
                        TimeParser.format24Hour(it.first, it.second)
                    } ?: endTime
                } else ""
                json.put("scheduleMode", scheduleMode)
                json.put("startTime", normalizedStart)
                json.put("hasEndTime", hasEndTime)
                json.put("endTime", normalizedEnd)
                json.put("durationHours", durationHours)
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
                json.put("packageName", if (appPackage.isNotBlank()) appPackage else "com.google.android.${appName.lowercase().replace(" ", "")}")
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
        if (customNotificationActive) {
            val cfg = CustomNotificationConfig(
                title = customNotificationTitle.ifBlank { "Routine Alert" },
                message = customNotificationMessage,
                iconName = customNotificationIcon,
                scheduleType = customNotificationScheduleType,
                time = customNotificationTime
            )
            list.add(
                RoutineActionItem(
                    type = ActionType.CUSTOM_NOTIFICATION,
                    enabledState = true,
                    intValue = 0,
                    stringValue = cfg.toJsonString()
                )
            )
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
                commands.add("settings put global mode_ringer 0 && settings put global zen_mode 2")
            } else if (soundProfile == "VIBRATE") {
                commands.add("settings put global zen_mode 0 && settings put global mode_ringer 1")
            } else {
                if (soundVolumeEnabled) {
                    commands.add("settings put global mode_ringer 2 && media volume --stream 3 --set $soundVolumePercent")
                } else {
                    commands.add("settings put global mode_ringer 2")
                }
            }
        }
        if (customNotificationActive) {
            val timing = when (customNotificationScheduleType) {
                "DAILY" -> "daily at $customNotificationTime"
                "ONCE" -> "once at $customNotificationTime"
                else -> "immediate on routine trigger"
            }
            commands.add("echo 'Notify: \"${customNotificationTitle.ifBlank { "Routine Alert" }}\" ($timing)'")
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

    var showInfoDialog by remember { mutableStateOf(false) }
    var showQuickEditDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val handleSaveRoutine = {
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
            restoreOnEnd = restoreOnEnd,
            createdAt = initialRoutine?.createdAt ?: System.currentTimeMillis()
        )
        onSave(routine)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
        // ==========================================
        // SECTION 1: Routine Name & Identity
        // ==========================================
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 3.dp,
                pressedElevation = 6.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = "Routine Identity",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedTextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    label = { Text("Routine Name *") },
                    placeholder = { Text("e.g. Bedtime Power Saver, Work Focus, Morning Wakeup") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        focusedLabelColor = Color(0xFF2563EB),
                        focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_name_input"),
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Turn off AOD and activate silent mode on weeknights") },
                    singleLine = false,
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        focusedLabelColor = Color(0xFF2563EB),
                        focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_description_input"),
                    shape = RoundedCornerShape(16.dp)
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
                                .clickable {
                                    haptics.selectionTick()
                                    selectedColor = hex
                                }
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
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 3.dp,
                pressedElevation = 6.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "2",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = "Select Trigger",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Choose condition type from the predefined trigger list:",
                    style = MaterialTheme.typography.bodyMedium,
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
                            onClick = {
                                haptics.selectionTick()
                                selectedTriggerType = type
                            },
                            label = { Text(type.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.16f),
                                selectedLabelColor = Color(0xFF1D4ED8),
                                selectedLeadingIconColor = Color(0xFF2563EB),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                selectedBorderColor = Color(0xFF2563EB),
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            ),
                            shape = RoundedCornerShape(14.dp),
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
                                    onClick = {
                                        haptics.selectionTick()
                                        scheduleMode = modeKey
                                    },
                                    label = { Text(modeTitle, fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium) },
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
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.16f),
                                        selectedLabelColor = Color(0xFF1D4ED8),
                                        selectedLeadingIconColor = Color(0xFF2563EB),
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isModeSelected,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                        selectedBorderColor = Color(0xFF2563EB),
                                        borderWidth = 1.dp,
                                        selectedBorderWidth = 1.5.dp
                                    ),
                                    shape = RoundedCornerShape(14.dp),
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
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF2563EB),
                                        focusedLabelColor = Color(0xFF2563EB),
                                        focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    ),
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
                                    shape = RoundedCornerShape(12.dp)
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
                                            .clickable {
                                                haptics.selectionTick()
                                                startTime = timeVal
                                            }
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

                            // End Time / Duration Options
                            Spacer(modifier = Modifier.height(10.dp))
                            ElevatedCard(
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.elevatedCardElevation(
                                    defaultElevation = if (hasEndTime) 2.dp else 0.dp,
                                    pressedElevation = 4.dp
                                ),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (hasEndTime)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
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
                                                imageVector = Icons.Default.HourglassTop,
                                                contentDescription = null,
                                                tint = if (hasEndTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Set End Time / Duration",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Routine runs for a set duration, then restores settings",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        ExpressiveSwitch(
                                            checked = hasEndTime,
                                            onCheckedChange = { checked ->
                                                hasEndTime = checked
                                                if (checked) {
                                                    // Default end time is 4 hours after start time
                                                    val (h, m) = parsedTime ?: Pair(21, 0)
                                                    val endH = (h + 4) % 24
                                                    endTime = TimeParser.format24Hour(endH, m)
                                                }
                                            },
                                            modifier = Modifier.testTag("has_end_time_switch")
                                        )
                                    }

                                    if (hasEndTime) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val parsedEndTime = TimeParser.parseToHourMinute(endTime)

                                        // Quick Duration Presets (e.g. 1h, 2h, 4h, 8h)
                                        Text(
                                            text = "Quick Duration (from start time):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            listOf(
                                                1 to "1 hour",
                                                2 to "2 hours",
                                                4 to "4 hours",
                                                6 to "6 hours",
                                                8 to "8 hours"
                                            ).forEach { (hrs, lbl) ->
                                                val (sh, sm) = parsedTime ?: Pair(21, 0)
                                                val targetEndH = (sh + hrs) % 24
                                                val expectedEndStr = TimeParser.format24Hour(targetEndH, sm)
                                                val isSelected = parsedEndTime != null &&
                                                        TimeParser.format24Hour(parsedEndTime.first, parsedEndTime.second) == expectedEndStr

                                                ExpressiveFilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        durationHours = hrs.toString()
                                                        endTime = expectedEndStr
                                                    },
                                                    label = { Text(lbl, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Or Specific End Clock Time:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = endTime,
                                                onValueChange = { endTime = it },
                                                label = { Text("End Time (e.g. 01:00 or 1:00 AM)") },
                                                placeholder = { Text("01:00") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF2563EB),
                                                    focusedLabelColor = Color(0xFF2563EB),
                                                    focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                                ),
                                                trailingIcon = {
                                                    IconButton(
                                                        onClick = {
                                                            val (initHour, initMin) = parsedEndTime ?: Pair(1, 0)
                                                            TimePickerDialog(
                                                                context,
                                                                { _, hourOfDay, minute ->
                                                                    endTime = TimeParser.format24Hour(hourOfDay, minute)
                                                                },
                                                                initHour,
                                                                initMin,
                                                                false
                                                            ).show()
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Default.AccessTime,
                                                            contentDescription = "Pick End Clock",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("time_end_input"),
                                                shape = RoundedCornerShape(10.dp)
                                            )

                                            FilledTonalButton(
                                                onClick = {
                                                    val (initHour, initMin) = parsedEndTime ?: Pair(1, 0)
                                                    TimePickerDialog(
                                                        context,
                                                        { _, hourOfDay, minute ->
                                                            endTime = TimeParser.format24Hour(hourOfDay, minute)
                                                        },
                                                        initHour,
                                                        initMin,
                                                        false
                                                    ).show()
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("pick_end_time_clock_btn")
                                            ) {
                                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Pick")
                                            }
                                        }

                                        if (parsedEndTime != null && parsedTime != null) {
                                            val startMinutesTotal = parsedTime.first * 60 + parsedTime.second
                                            val endMinutesTotal = parsedEndTime.first * 60 + parsedEndTime.second
                                            val diffMinutes = if (endMinutesTotal >= startMinutesTotal) {
                                                endMinutesTotal - startMinutesTotal
                                            } else {
                                                (24 * 60 - startMinutesTotal) + endMinutesTotal
                                            }
                                            val durationHrs = diffMinutes / 60
                                            val durationMins = diffMinutes % 60
                                            val durationLabel = buildString {
                                                if (durationHrs > 0) append("$durationHrs hr ")
                                                if (durationMins > 0 || durationHrs == 0) append("$durationMins min")
                                            }.trim()

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                                                        text = "Active from ${TimeParser.format12Hour(parsedTime.first, parsedTime.second)} to ${TimeParser.format12Hour(parsedEndTime.first, parsedEndTime.second)} ($durationLabel duration)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }
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
                                                haptics.toggleAction(!isDaySelected)
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
                                        haptics.buttonClick()
                                        selectedDays = "Mon, Tue, Wed, Thu, Fri, Sat, Sun"
                                    }
                                ) {
                                    Text("Every Day", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        haptics.buttonClick()
                                        selectedDays = "Mon, Tue, Wed, Thu, Fri"
                                    }
                                ) {
                                    Text("Weekdays (Mon-Fri)", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        haptics.buttonClick()
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
                                    ExpressiveFilterChip(
                                        selected = isSelected,
                                        onClick = { intervalMinutes = mins },
                                        label = { Text("Every $label", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) }
                                    )
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // AlarmManager Precision Status & Next Trigger Badge (Status-Critical Alert Card with glowing border)
                        // ----------------------------------------------------
                        ElevatedCard(
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.elevatedCardElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 2.dp
                            ),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF2563EB),
                                                Color(0xFF60A5FA),
                                                Color(0xFF38BDF8),
                                                Color(0xFF818CF8),
                                                Color(0xFF2563EB)
                                            )
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF2563EB).copy(alpha = 0.12f),
                                            Color(0xFF3B82F6).copy(alpha = 0.04f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Status-Critical Badge Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF2563EB),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Alarm,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "AlarmManager RTC_WAKEUP",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Precision Hardware Standby Trigger",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Glowing Pill Alert Badge
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF2563EB).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2563EB))
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "EXACT WAKEUP",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Guarantees exact hardware-level wake-up even while your device is in deep Doze mode, firing routine actions precisely on schedule without battery manager throttling.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (nextScheduledTriggerMillis != null) {
                                    val formattedNext = RoutineAlarmManager.formatNextTriggerHumanReadable(nextScheduledTriggerMillis)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.25f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Next Scheduled Alarm Execution",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1D4ED8)
                                                )
                                                Text(
                                                    text = formattedNext,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                // Styled Internal "Test Action" Button
                                Button(
                                    onClick = {
                                        alarmTestStatus = "Armed! Test exact alarm will fire in 5s..."
                                        RoutineAlarmManager.scheduleTestAlarm(context, initialRoutine?.id ?: 9999L, delaySeconds = 5)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2563EB),
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 2.dp,
                                        pressedElevation = 6.dp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("test_alarm_btn")
                                ) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Test Exact Alarm (Fires in 5s)",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                alarmTestStatus?.let { status ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF00897B).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Color(0xFF00897B).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF00897B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = status,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF00897B)
                                            )
                                        }
                                    }
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
                                ExpressiveFilterChip(
                                    selected = isSelected,
                                    onClick = { solarOffsetMinutes = offset },
                                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
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
                                ExpressiveFilterChip(
                                    selected = isSelected,
                                    onClick = { solarDays = opt },
                                    label = { Text(opt, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = geoThemeColor,
                                focusedLabelColor = geoThemeColor,
                                focusedContainerColor = geoThemeColor.copy(alpha = 0.035f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("geo_label_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Quick place presets
                        val placePresets = listOf("Home", "Office", "Gym", "School", "Store")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            placePresets.forEach { preset ->
                                val isSelected = geoLabel.equals(preset, ignoreCase = true)
                                ExpressiveFilterChip(
                                    selected = isSelected,
                                    onClick = { geoLabel = preset },
                                    label = { Text(preset, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                    accentColor = geoThemeColor,
                                    shape = RoundedCornerShape(10.dp),
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
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = geoThemeColor,
                                            focusedLabelColor = geoThemeColor,
                                            focusedContainerColor = geoThemeColor.copy(alpha = 0.035f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier.weight(1f).testTag("geo_lat_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = geoLongitude,
                                        onValueChange = { geoLongitude = it },
                                        label = { Text("Longitude") },
                                        placeholder = { Text("-122.4194") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = geoThemeColor,
                                            focusedLabelColor = geoThemeColor,
                                            focusedContainerColor = geoThemeColor.copy(alpha = 0.035f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier.weight(1f).testTag("geo_lng_input"),
                                        shape = RoundedCornerShape(12.dp)
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
                            ExpressiveFilterChip(
                                selected = geoTransition == "ENTER",
                                onClick = { geoTransition = "ENTER" },
                                label = { Text("When Arriving (Enter)", fontWeight = if (geoTransition == "ENTER") FontWeight.Bold else FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                accentColor = geoThemeColor,
                                modifier = Modifier.weight(1f).testTag("geo_transition_enter")
                            )
                            ExpressiveFilterChip(
                                selected = geoTransition == "EXIT",
                                onClick = { geoTransition = "EXIT" },
                                label = { Text("When Leaving (Exit)", fontWeight = if (geoTransition == "EXIT") FontWeight.Bold else FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                accentColor = geoThemeColor,
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
                                ExpressiveFilterChip(
                                    selected = isSelected,
                                    onClick = { geoRadiusMeters = radiusVal },
                                    label = { Text(labelText, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                    accentColor = geoThemeColor,
                                    shape = RoundedCornerShape(10.dp),
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                focusedLabelColor = Color(0xFF2563EB),
                                focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (wifiConnected) "Trigger when connected" else "Trigger when disconnected")
                            ExpressiveSwitch(
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    focusedLabelColor = Color(0xFF2563EB),
                                    focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("bt_custom_name_input"),
                                shape = RoundedCornerShape(12.dp)
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
                            ExpressiveSwitch(
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
                            ExpressiveSwitch(
                                checked = batteryCharging,
                                onCheckedChange = { batteryCharging = it }
                            )
                        }
                    }

                    TriggerType.APP -> {
                        // Header with app count and refresh button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Target Application",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (installedApps.isNotEmpty()) {
                                        "${installedApps.size} apps available on device"
                                    } else {
                                        "Select app to trigger routine upon launch"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptics.buttonClick()
                                    refreshInstalledApps()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("refresh_installed_apps_btn")
                            ) {
                                if (isLoadingApps) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh Applications",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Selected App Banner (similar to Bluetooth selected card)
                        val selectedAppInfo = remember(installedApps, appName, appPackage) {
                            installedApps.firstOrNull {
                                (appPackage.isNotBlank() && it.packageName.equals(appPackage, ignoreCase = true)) ||
                                it.appName.equals(appName, ignoreCase = true)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("selected_app_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InstalledAppIcon(
                                    context = context,
                                    packageName = selectedAppInfo?.packageName ?: appPackage.ifBlank { "com.google.android.${appName.lowercase()}" },
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Selected Target App",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (selectedAppInfo?.isSystemApp == true) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = "System",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = appName.ifBlank { "YouTube" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = selectedAppInfo?.packageName ?: appPackage.ifBlank { "Triggers when application is brought to foreground" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Search Bar with instant filtering and clear action
                        OutlinedTextField(
                            value = appSearchQuery,
                            onValueChange = { appSearchQuery = it },
                            placeholder = { Text("Search installed apps or packages...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (appSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        haptics.buttonClick()
                                        appSearchQuery = ""
                                    }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                focusedLabelColor = Color(0xFF2563EB),
                                focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_search_text_field"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Category Filter Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val userAppCount = remember(installedApps) { installedApps.count { !it.isSystemApp } }
                            val sysAppCount = remember(installedApps) { installedApps.count { it.isSystemApp } }

                            listOf(
                                "ALL" to "All (${installedApps.size})",
                                "USER" to "User Apps ($userAppCount)",
                                "SYSTEM" to "System Apps ($sysAppCount)"
                            ).forEach { (mode, label) ->
                                val isSelected = appFilterMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptics.selectionTick()
                                        appFilterMode = mode
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("app_filter_chip_$mode")
                                )
                            }
                        }

                        // Loading State
                        if (isLoadingApps && installedApps.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Scanning installed applications...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Filtered Apps List
                        val filteredApps = remember(installedApps, appSearchQuery, appFilterMode) {
                            installedApps.filter { app ->
                                val matchesQuery = appSearchQuery.isBlank() ||
                                    app.appName.contains(appSearchQuery, ignoreCase = true) ||
                                    app.packageName.contains(appSearchQuery, ignoreCase = true)
                                val matchesMode = when (appFilterMode) {
                                    "USER" -> !app.isSystemApp
                                    "SYSTEM" -> app.isSystemApp
                                    else -> true
                                }
                                matchesQuery && matchesMode
                            }
                        }

                        if (filteredApps.isNotEmpty()) {
                            Text(
                                text = if (appSearchQuery.isNotBlank()) "Search Results (${filteredApps.size}):" else "Installed Apps (${filteredApps.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val appsToShow = if (appSearchQuery.isNotBlank() || showAllAppsExpanded) {
                                filteredApps
                            } else {
                                filteredApps.take(8)
                            }

                            appsToShow.forEach { app ->
                                val isSelected = (appPackage.isNotBlank() && app.packageName.equals(appPackage, ignoreCase = true)) ||
                                    (appPackage.isBlank() && app.appName.equals(appName, ignoreCase = true))
                                Surface(
                                    onClick = {
                                        haptics.selectionTick()
                                        appName = app.appName
                                        appPackage = app.packageName
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("installed_app_${app.packageName}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                haptics.selectionTick()
                                                appName = app.appName
                                                appPackage = app.packageName
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        InstalledAppIcon(
                                            context = context,
                                            packageName = app.packageName,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = app.appName,
                                                    fontWeight = FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (app.isSystemApp) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                                    ) {
                                                        Text(
                                                            text = "System",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = app.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Show more / Show less toggle if list exceeds 8 items and not searching
                            if (appSearchQuery.isBlank() && filteredApps.size > 8) {
                                TextButton(
                                    onClick = {
                                        haptics.buttonClick()
                                        showAllAppsExpanded = !showAllAppsExpanded
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("toggle_all_apps_btn")
                                ) {
                                    Icon(
                                        if (showAllAppsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (showAllAppsExpanded) "Show Fewer Apps" else "Show All Apps (${filteredApps.size})"
                                    )
                                }
                            }
                        } else if (!isLoadingApps) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = if (appSearchQuery.isNotBlank()) "No applications matching \"$appSearchQuery\"" else "No applications found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (appSearchQuery.isNotBlank()) {
                                        TextButton(onClick = {
                                            haptics.buttonClick()
                                            appSearchQuery = ""
                                        }) {
                                            Text("Clear Search")
                                        }
                                    }
                                }
                            }
                        }

                        // Option 3: Custom or Unlisted Package Name input (like Bluetooth custom input)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Or specify custom app or package name",
                                style = MaterialTheme.typography.labelMedium
                            )
                            TextButton(onClick = {
                                haptics.buttonClick()
                                showCustomAppInput = !showCustomAppInput
                            }) {
                                Text(if (showCustomAppInput) "Hide" else "Enter Name")
                            }
                        }

                        if (showCustomAppInput) {
                            OutlinedTextField(
                                value = appName,
                                onValueChange = {
                                    appName = it
                                    appPackage = ""
                                },
                                label = { Text("Application Name") },
                                placeholder = { Text("e.g. YouTube, Maps, Kindle, com.vpmapps.pixelflow.app") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    focusedLabelColor = Color(0xFF2563EB),
                                    focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_app_name_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 3: Action Definitions & Shell Commands
        // ==========================================
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 3.dp,
                pressedElevation = 6.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "3",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = "Actions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Choose device services and settings to automate when this routine activates:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Action Item 1: Always-On Display (AOD)
                ActionSettingCard(
                    title = "Always-On Display (AOD)",
                    shellCommand = "settings put secure doze_always_on ${if (aodEnabled) 1 else 0}",
                    icon = Icons.Default.LightMode,
                    isActive = aodActive,
                    onActiveChange = { 
                        aodActive = it
                        if (it) aodEnabled = true
                    }
                )

                // Action Item 2: Auto-Rotate
                ActionSettingCard(
                    title = "Screen Auto-Rotation",
                    shellCommand = "settings put system accelerometer_rotation ${if (autoRotateEnabled) 1 else 0}",
                    icon = Icons.Default.ScreenRotation,
                    isActive = autoRotateActive,
                    onActiveChange = { 
                        autoRotateActive = it
                        if (it) autoRotateEnabled = true
                    }
                )

                // Action Item 3: Dark Mode
                ActionSettingCard(
                    title = "Dark Theme",
                    shellCommand = "cmd uimode night ${if (darkModeEnabled) "yes" else "no"}",
                    icon = Icons.Default.Nightlight,
                    isActive = darkModeActive,
                    onActiveChange = { 
                        darkModeActive = it
                        if (it) darkModeEnabled = true
                    }
                )

                // Action Item 4: Battery Saver
                ActionSettingCard(
                    title = "Battery Saver Mode",
                    shellCommand = "cmd power set-mode ${if (batterySaverState) 1 else 0}",
                    icon = Icons.Default.BatteryChargingFull,
                    isActive = batterySaverActive,
                    onActiveChange = { 
                        batterySaverActive = it
                        if (it) batterySaverState = true
                    }
                )

                // Action Item 5: Wi-Fi Power
                ActionSettingCard(
                    title = "Wi-Fi Power",
                    shellCommand = "svc wifi ${if (wifiState) "enable" else "disable"}",
                    icon = Icons.Default.Wifi,
                    isActive = wifiActive,
                    onActiveChange = { 
                        wifiActive = it
                        if (it) wifiState = true
                    }
                )

                // Action Item 5b: Wi-Fi Hotspot
                val hotspotShellCmd = if (hotspotState) "cmd wifi start-softap || svc wifi startSoftAp" else "cmd wifi stop-softap || svc wifi stopSoftAp"
                ActionSettingCard(
                    title = "Wi-Fi Hotspot",
                    shellCommand = hotspotShellCmd,
                    icon = Icons.Default.WifiTethering,
                    isActive = hotspotActive,
                    onActiveChange = { 
                        hotspotActive = it
                        if (it) hotspotState = true
                    }
                )

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
                        Text("Timeout: ${seconds}s", style = MaterialTheme.typography.bodySmall)
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
                    onActiveChange = { 
                        bluetoothActive = it
                        if (it) bluetoothState = true
                    }
                )

                // Action Item 8: Sound & Volume Profile (Ring, Vibrate, Silent, Volume Slider)
                val soundShellCmd = when (soundProfile.uppercase()) {
                    "SILENT" -> "settings put global mode_ringer 0 && settings put global zen_mode 2"
                    "VIBRATE" -> "settings put global zen_mode 0 && settings put global mode_ringer 1"
                    else -> if (soundVolumeEnabled) "settings put global mode_ringer 2 && media volume --stream 3 --set $soundVolumePercent%" else "settings put global mode_ringer 2"
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
                            val isRingSelected = soundProfile == "NORMAL" || soundProfile == "RING"
                            FilterChip(
                                selected = isRingSelected,
                                onClick = { soundProfile = "NORMAL" },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Ring", fontWeight = if (isRingSelected) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.16f),
                                    selectedLabelColor = Color(0xFF1D4ED8),
                                    selectedLeadingIconColor = Color(0xFF2563EB),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isRingSelected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    selectedBorderColor = Color(0xFF2563EB),
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.5.dp
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sound_mode_ring_chip")
                            )

                            // 2. Vibrate
                            val isVibrateSelected = soundProfile == "VIBRATE"
                            FilterChip(
                                selected = isVibrateSelected,
                                onClick = { soundProfile = "VIBRATE" },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Vibrate", fontWeight = if (isVibrateSelected) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.16f),
                                    selectedLabelColor = Color(0xFF1D4ED8),
                                    selectedLeadingIconColor = Color(0xFF2563EB),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isVibrateSelected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    selectedBorderColor = Color(0xFF2563EB),
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.5.dp
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sound_mode_vibrate_chip")
                            )

                            // 3. Silent
                            val isSilentSelected = soundProfile == "SILENT"
                            FilterChip(
                                selected = isSilentSelected,
                                onClick = { soundProfile = "SILENT" },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VolumeOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Silent", fontWeight = if (isSilentSelected) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.16f),
                                    selectedLabelColor = Color(0xFF1D4ED8),
                                    selectedLeadingIconColor = Color(0xFF2563EB),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSilentSelected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    selectedBorderColor = Color(0xFF2563EB),
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.5.dp
                                ),
                                shape = RoundedCornerShape(14.dp),
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

                        val notificationMgr = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager }
                        var isDndGranted by remember { mutableStateOf(notificationMgr?.isNotificationPolicyAccessGranted == true) }
                        val isShizukuReady = remember { com.vpmapps.pixelflow.shizuku.ShizukuManager.isAuthorized() }
                        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                        androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                    isDndGranted = notificationMgr?.isNotificationPolicyAccessGranted == true
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                        }

                        if (soundProfile == "VIBRATE" || soundProfile == "SILENT") {
                            if (isDndGranted || isShizukuReady) {
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
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isDndGranted) "✓ Do Not Disturb access active (native system control ready)" else "✓ Shizuku privileged access active (automated control ready)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFEF4444),
                                                Color(0xFFF87171),
                                                Color(0xFFFCA5A5),
                                                Color(0xFFEF4444)
                                            )
                                        )
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFEF4444),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Do Not Disturb Permission Required",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Text(
                                                    text = "Android prevents automated Vibrate/Silent switching without DND access.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    val fallback = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(fallback)
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFDC2626),
                                                contentColor = Color.White
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(
                                                defaultElevation = 2.dp,
                                                pressedElevation = 4.dp
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("grant_dnd_editor_btn")
                                        ) {
                                            Text("Grant Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
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
                                ExpressiveSwitch(
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
                                            val isVolSelected = soundVolumePercent == vol
                                            FilterChip(
                                                selected = isVolSelected,
                                                onClick = { soundVolumePercent = vol },
                                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isVolSelected) FontWeight.Bold else FontWeight.Medium) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.16f),
                                                    selectedLabelColor = Color(0xFF1D4ED8),
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = isVolSelected,
                                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                    selectedBorderColor = Color(0xFF2563EB),
                                                    borderWidth = 1.dp,
                                                    selectedBorderWidth = 1.5.dp
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Item 9: Custom Scheduled Notification
                val notifTimingDesc = when (customNotificationScheduleType) {
                    "DAILY" -> "Every day at $customNotificationTime"
                    "ONCE" -> "Once at $customNotificationTime"
                    else -> "Immediate on routine activation"
                }
                val notifShellCmd = "notify --title \"${customNotificationTitle.ifBlank { "Routine Alert" }}\" --icon $customNotificationIcon --schedule $customNotificationScheduleType"

                val notifIconVector = when (customNotificationIcon) {
                    "star" -> Icons.Default.Star
                    "heart" -> Icons.Default.Favorite
                    "bolt" -> Icons.Default.Bolt
                    "check" -> Icons.Default.CheckCircle
                    "coffee" -> Icons.Default.Coffee
                    "fitness" -> Icons.Default.FitnessCenter
                    "calendar" -> Icons.Default.CalendarToday
                    "alert" -> Icons.Default.Warning
                    "info" -> Icons.Default.Info
                    else -> Icons.Default.Notifications
                }

                ActionSettingCard(
                    title = "Custom Notification",
                    shellCommand = notifShellCmd,
                    icon = notifIconVector,
                    isActive = customNotificationActive,
                    onActiveChange = {
                        haptics.toggleAction(it)
                        customNotificationActive = it
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title Input
                        OutlinedTextField(
                            value = customNotificationTitle,
                            onValueChange = { customNotificationTitle = it },
                            label = { Text("Notification Title") },
                            placeholder = { Text("e.g. Focus time, Hydrate, Stand up, Bedtime") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = notifIconVector,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                focusedLabelColor = Color(0xFF2563EB),
                                focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_notif_title_input")
                        )

                        // Message / Text Input
                        OutlinedTextField(
                            value = customNotificationMessage,
                            onValueChange = { customNotificationMessage = it },
                            label = { Text("Message / Description (Optional)") },
                            placeholder = { Text("e.g. Remember to stretch and stay hydrated") },
                            minLines = 2,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                focusedLabelColor = Color(0xFF2563EB),
                                focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_notif_message_input")
                        )

                        // Icon Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Choose Notification Icon",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val iconOptions = listOf(
                                "bell" to ("Bell" to Icons.Default.Notifications),
                                "star" to ("Star" to Icons.Default.Star),
                                "heart" to ("Heart" to Icons.Default.Favorite),
                                "bolt" to ("Energy" to Icons.Default.Bolt),
                                "check" to ("Task" to Icons.Default.CheckCircle),
                                "coffee" to ("Coffee" to Icons.Default.Coffee),
                                "fitness" to ("Fitness" to Icons.Default.FitnessCenter),
                                "calendar" to ("Calendar" to Icons.Default.CalendarToday),
                                "alert" to ("Alert" to Icons.Default.Warning),
                                "info" to ("Info" to Icons.Default.Info)
                            )

                            // Row 1 of icons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                iconOptions.take(5).forEach { (iconKey, pair) ->
                                    val (label, iconVec) = pair
                                    val isSelected = customNotificationIcon == iconKey
                                    Surface(
                                        onClick = {
                                            haptics.selectionTick()
                                            customNotificationIcon = iconKey
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("notif_icon_$iconKey")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = iconVec,
                                                contentDescription = label,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Row 2 of icons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                iconOptions.drop(5).take(5).forEach { (iconKey, pair) ->
                                    val (label, iconVec) = pair
                                    val isSelected = customNotificationIcon == iconKey
                                    Surface(
                                        onClick = {
                                            haptics.selectionTick()
                                            customNotificationIcon = iconKey
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("notif_icon_$iconKey")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = iconVec,
                                                contentDescription = label,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Schedule Type Selection: Immediate, Once, Daily
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Delivery Timing",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "IMMEDIATE" to "On Run",
                                    "ONCE" to "Just Once",
                                    "DAILY" to "Every Day"
                                ).forEach { (typeKey, label) ->
                                    val isSelected = customNotificationScheduleType == typeKey
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            haptics.selectionTick()
                                            customNotificationScheduleType = typeKey
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = when (typeKey) {
                                                    "DAILY" -> Icons.Default.Repeat
                                                    "ONCE" -> Icons.Default.Schedule
                                                    else -> Icons.Default.PlayArrow
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.16f),
                                            selectedLabelColor = Color(0xFF1D4ED8),
                                            selectedLeadingIconColor = Color(0xFF2563EB),
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            selectedBorderColor = Color(0xFF2563EB),
                                            borderWidth = 1.dp,
                                            selectedBorderWidth = 1.5.dp
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("notif_sched_${typeKey.lowercase()}")
                                    )
                                }
                            }
                        }

                        // Time Selection (shown when ONCE or DAILY)
                        if (customNotificationScheduleType == "ONCE" || customNotificationScheduleType == "DAILY") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
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
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (customNotificationScheduleType == "DAILY") "Every Day At Time" else "Trigger Once At Time",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = customNotificationTime,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Quick Presets
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("07:00", "09:00", "12:00", "18:00", "21:00").forEach { presetTime ->
                                            val isTimeSelected = customNotificationTime == presetTime
                                            FilterChip(
                                                selected = isTimeSelected,
                                                onClick = {
                                                    haptics.selectionTick()
                                                    customNotificationTime = presetTime
                                                },
                                                label = {
                                                    Text(
                                                        text = presetTime,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isTimeSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // Stepper controls for Hour and Minute
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Adjust Hours & Minutes:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            val parts = customNotificationTime.split(":")
                                            val curHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                                            val curMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

                                            OutlinedButton(
                                                onClick = {
                                                    haptics.buttonClick()
                                                    val newHour = (curHour - 1 + 24) % 24
                                                    customNotificationTime = String.format("%02d:%02d", newHour, curMin)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("-1h", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    haptics.buttonClick()
                                                    val newHour = (curHour + 1) % 24
                                                    customNotificationTime = String.format("%02d:%02d", newHour, curMin)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("+1h", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    haptics.buttonClick()
                                                    val newMin = (curMin + 15) % 60
                                                    customNotificationTime = String.format("%02d:%02d", curHour, newMin)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("+15m", fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    // Schedule Summary Text
                                    Text(
                                        text = if (customNotificationScheduleType == "DAILY")
                                            "✓ Notification will trigger every day at $customNotificationTime"
                                        else
                                            "✓ Notification will trigger once at $customNotificationTime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Preview of Notification Banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = notifIconVector,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "PixelFlow",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = if (customNotificationScheduleType == "IMMEDIATE") "Now" else customNotificationTime,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = customNotificationTitle.ifBlank { "Routine Alert" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (customNotificationMessage.isNotBlank()) {
                                        Text(
                                            text = customNotificationMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom Shell Commands Input & Advanced Section
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                var showAdvancedShell by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedShell = !showAdvancedShell }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Add Custom Shell Command",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Execute privileged adb / system commands directly",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FilledIconButton(
                                onClick = { showAdvancedShell = !showAdvancedShell },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (showAdvancedShell) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand shell options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Input row: Always visible, clear prompt text, visually distinct primary '+' button
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Enter privileged shell or adb command to execute upon routine activation:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Commands execute directly with Shizuku root/adb privileges.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = newCustomCommand,
                                onValueChange = { newCustomCommand = it },
                                label = { Text("Shell Command Prompt") },
                                placeholder = { Text("e.g. settings put global zen_mode 1") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                supportingText = {
                                    Text("Supports system settings, svc, cmd, and package commands")
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    focusedLabelColor = Color(0xFF2563EB),
                                    focusedContainerColor = Color(0xFF2563EB).copy(alpha = 0.035f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_command_input"),
                                shape = RoundedCornerShape(16.dp)
                            )

                            // Visually distinct prominent primary '+' button (56dp square for seamless touch target)
                            Button(
                                onClick = {
                                    if (newCustomCommand.isNotBlank()) {
                                        customShellCommands.add(newCustomCommand.trim())
                                        newCustomCommand = ""
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 4.dp
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("add_custom_command_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Command",
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        if (customShellCommands.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Configured Custom Shell Commands (${customShellCommands.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            customShellCommands.forEachIndexed { index, cmd ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E1E24))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Advanced Expandable: Script Inspection & Live Testing
                        if (showAdvancedShell) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Command Inspector Box
                            if (correspondingCommands.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Generated Shell Script Execution Plan:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF16161D))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        correspondingCommands.forEach { cmd ->
                                            Text(
                                                text = "$ $cmd",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF64B5F6)
                                            )
                                        }
                                    }
                                }
                            }

                            // Test Shell Commands Live via Shizuku
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
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
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dry-Run Shell Commands Now")
                            }

                            testExecutionStatus?.let { status ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (status.contains("successful")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 3.5: Auto-Restore (When Routine Ends)
        // ==========================================
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (restoreOnEnd) 4.dp else 2.dp,
                pressedElevation = 6.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header with Prominent Auto-Restore Pill Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (restoreOnEnd)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (restoreOnEnd)
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Auto-Restore",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Highly visible status pill
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (restoreOnEnd)
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (restoreOnEnd)
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (restoreOnEnd)
                                                        Color(0xFF00897B)
                                                    else
                                                        MaterialTheme.colorScheme.outline
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (restoreOnEnd) "ENABLED" else "OFF",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (restoreOnEnd)
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Revert system settings when routine ends",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    ExpressiveSwitch(
                        checked = restoreOnEnd,
                        onCheckedChange = { restoreOnEnd = it },
                        modifier = Modifier.testTag("restore_on_end_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High-visibility informative banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (restoreOnEnd)
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (restoreOnEnd)
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                        else
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (restoreOnEnd) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (restoreOnEnd) Color(0xFF00897B) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (restoreOnEnd) {
                                "Auto-Restore is Active: Original system states are recorded automatically and restored once routine conditions finish."
                            } else {
                                "Auto-Restore is Off: Settings will remain as modified even after routine finishes until manually adjusted."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (restoreOnEnd) FontWeight.Medium else FontWeight.Normal,
                            color = if (restoreOnEnd)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // FIXED, COHESIVE BOTTOM ACTION BAR
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fixed_bottom_action_bar"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Main action: prominent Save Routine button
                    Button(
                        onClick = handleSaveRoutine,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_routine_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Routine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Auxiliary features to the side: Edit, Share, Info as smaller, unobtrusive icons to prevent visual noise
                    IconButton(
                        onClick = { showQuickEditDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("routine_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val actions = buildActions()
                            val triggerSummary = when (selectedTriggerType) {
                                TriggerType.TIME -> "Time trigger ($startTime, $scheduleMode, $selectedDays)"
                                TriggerType.SUNRISE -> "Sunrise trigger (Solar offset: ${solarOffsetMinutes}m, $solarDays)"
                                TriggerType.SUNSET -> "Sunset trigger (Solar offset: ${solarOffsetMinutes}m, $solarDays)"
                                TriggerType.LOCATION -> "Location trigger (${geoLabel.ifBlank { "Location" }}, radius: ${geoRadiusMeters}m, $geoTransition)"
                                TriggerType.WIFI -> "Wi-Fi trigger ($wifiSsid)"
                                TriggerType.BLUETOOTH -> "Bluetooth trigger ($btDevice)"
                                TriggerType.BATTERY -> "Battery trigger (${batteryLevel.toInt()}%, ${if (batteryCharging) "Charging" else "Discharging"})"
                                TriggerType.APP -> "App launch trigger ($appName${if (appPackage.isNotBlank()) " [$appPackage]" else ""})"
                            }
                            val shareContent = buildString {
                                appendLine("⚡ Routine: ${routineName.trim().ifBlank { "Untitled Routine" }}")
                                if (description.isNotBlank()) appendLine("Description: $description")
                                appendLine("Trigger: $triggerSummary")
                                appendLine("Configured Actions (${actions.size}):")
                                actions.forEach {
                                    appendLine(" • ${it.type.name}")
                                }
                                appendLine("\nExported via PixelFlow")
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Pixel Routine: $routineName")
                                putExtra(Intent.EXTRA_TEXT, shareContent)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Routine"))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("routine_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Routine",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("routine_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Routine Information",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Info Dialog
    if (showInfoDialog) {
        val configuredActions = buildActions()
        val triggerSummary = when (selectedTriggerType) {
            TriggerType.TIME -> "Time: $startTime ($scheduleMode, Days: $selectedDays)"
            TriggerType.SUNRISE -> "Sunrise (Solar offset: ${solarOffsetMinutes}m, Days: $solarDays)"
            TriggerType.SUNSET -> "Sunset (Solar offset: ${solarOffsetMinutes}m, Days: $solarDays)"
            TriggerType.LOCATION -> "Location: ${geoLabel.ifBlank { "Custom Location" }} (${geoRadiusMeters}m radius, $geoTransition)"
            TriggerType.WIFI -> "Wi-Fi: SSID ${wifiSsid.ifBlank { "Any" }} (${if (wifiConnected) "Connected" else "Disconnected"})"
            TriggerType.BLUETOOTH -> "Bluetooth: ${btDevice.ifBlank { "Any device" }} (${if (btConnected) "Connected" else "Disconnected"})"
            TriggerType.APP -> "App launch: ${appName.ifBlank { "Custom app" }}${if (appPackage.isNotBlank()) " ($appPackage)" else ""}"
            TriggerType.BATTERY -> "Battery: ${batteryLevel.toInt()}% (${if (batteryCharging) "Charging" else "Discharging"})"
        }

        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Routine Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = try { Color(android.graphics.Color.parseColor(selectedColor)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                            modifier = Modifier.size(16.dp)
                        ) {}
                        Text(
                            text = routineName.trim().ifBlank { "Untitled Routine" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (description.isNotBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TRIGGER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = triggerSummary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (selectedTriggerType == TriggerType.TIME || selectedTriggerType == TriggerType.SUNRISE || selectedTriggerType == TriggerType.SUNSET) {
                            Text(
                                text = "✓ Precision AlarmManager RTC_WAKEUP active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ACTIONS (${configuredActions.size} CONFIGURED)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (configuredActions.isEmpty()) {
                            Text(
                                text = "No actions enabled yet. Toggle desired settings in editor.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            configuredActions.take(5).forEach { action ->
                                Text(
                                    text = "• ${action.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (configuredActions.size > 5) {
                                Text(
                                    text = "+ ${configuredActions.size - 5} more actions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Quick Edit Dialog
    if (showQuickEditDialog) {
        var tempName by remember { mutableStateOf(routineName) }
        var tempDesc by remember { mutableStateOf(description) }

        AlertDialog(
            onDismissRequest = { showQuickEditDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Quick Edit Routine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Routine Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempDesc,
                        onValueChange = { tempDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            showQuickEditDialog = false
                            coroutineScope.launch {
                                scrollState.animateScrollTo(0)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Scroll to top of editor")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        routineName = tempName
                        description = tempDesc
                        showQuickEditDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showQuickEditDialog = false
                        onCancel()
                    }
                ) {
                    Text("Discard / Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

/**
 * Reusable card for defining an action with its corresponding privileged shell command.
 * De-cluttered: Prominent recognizable service title, modern icon, high-contrast switch,
 * with underlying command placed below in small low-contrast monospace font with expandable view.
 */
@Composable
private fun ActionSettingCard(
    title: String,
    shellCommand: String,
    icon: ImageVector,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    var showCommandDetails by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isActive) 2.dp else 0.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Header Row: Service Icon + Recognizable Name + Modern Switch Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isActive)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Underneath command subtitle: very small, low-contrast monospace font
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showCommandDetails = !showCommandDetails }
                                .padding(top = 2.dp)
                        ) {
                            Text(
                                text = if (showCommandDetails) shellCommand else shellCommand.take(34) + if (shellCommand.length > 34) "…" else "",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                maxLines = if (showCommandDetails) 3 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = if (showCommandDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle command details",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                ExpressiveSwitch(
                    checked = isActive,
                    onCheckedChange = onActiveChange
                )
            }

            if (isActive && content != null) {
                Spacer(modifier = Modifier.height(10.dp))
                content()
            }
        }
    }
}

/**
 * Loads and displays an installed application's launcher icon with graceful fallback.
 */
@Composable
fun InstalledAppIcon(
    context: Context,
    packageName: String,
    modifier: Modifier = Modifier.size(36.dp)
) {
    var iconBitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }

    androidx.compose.runtime.LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            val bmp = AppInfoHelper.getAppIconBitmap(context, packageName)
            withContext(Dispatchers.Main) {
                iconBitmap = bmp
            }
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(8.dp))
        )
    } else {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

