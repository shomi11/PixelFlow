package com.vpmapps.pixelflow.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vpmapps.pixelflow.background.RoutineAlarmManager
import com.vpmapps.pixelflow.background.DeviceFactsStore
import com.vpmapps.pixelflow.background.GeofenceManager
import com.vpmapps.pixelflow.background.RoutineActivationStore
import com.vpmapps.pixelflow.background.TriggerEvaluator
import com.vpmapps.pixelflow.background.TriggerMonitorService
import com.vpmapps.pixelflow.service.RoutineStateSnapshotManager
import com.vpmapps.pixelflow.widget.WidgetUpdateHelper
import com.vpmapps.pixelflow.background.WorkManagerHelper
import com.vpmapps.pixelflow.data.local.AppDatabase
import com.vpmapps.pixelflow.data.model.Routine
import com.vpmapps.pixelflow.data.model.RoutineEntity
import com.vpmapps.pixelflow.data.model.RoutineExecutionLog
import com.vpmapps.pixelflow.data.repository.RoutineRepository
import com.vpmapps.pixelflow.notification.RoutineNotificationManager
import com.vpmapps.pixelflow.service.RoutineExecutionEngine
import com.vpmapps.pixelflow.shizuku.NetworkStatusMonitor
import com.vpmapps.pixelflow.shizuku.ShellResult
import com.vpmapps.pixelflow.shizuku.ShizukuManager
import com.vpmapps.pixelflow.shizuku.ShizukuPermissionManager
import com.vpmapps.pixelflow.shizuku.ShizukuState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * RoutineViewModel manages the state of routines, exposes reactive Flow
 * streams for Room database queries, and provides methods to toggle routine
 * enabled status and execute actions.
 */
class RoutineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoutineRepository
    private val executionEngine = RoutineExecutionEngine(application)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = RoutineRepository(db.routineDao(), application)
        try {
            ShizukuManager.init()
            ShizukuManager.checkStatus()
            NetworkStatusMonitor.initialize(application)
        } catch (e: Throwable) {
            android.util.Log.e("RoutineViewModel", "Error initializing Shizuku in ViewModel", e)
        }
        WorkManagerHelper.setupPeriodicMonitoring(application)
        try {
            RoutineAlarmManager.rescheduleAllRoutines(application)
        } catch (e: Throwable) {
            android.util.Log.e("RoutineViewModel", "Error rescheduling routines", e)
        }

        viewModelScope.launch {
            try {
                repository.populateDefaultsIfEmpty(application)
                RoutineAlarmManager.rescheduleAllRoutines(application)
            } catch (e: Throwable) {
                android.util.Log.e("RoutineViewModel", "Error populating default routines", e)
            }
        }

        viewModelScope.launch {
            ShizukuPermissionManager.permissionEvents.collect { event ->
                if (event.isGranted) {
                    _userMessage.emit("Shizuku permission granted! Elevated controls unlocked.")
                } else {
                    _userMessage.emit("Shizuku permission was denied.")
                }
            }
        }
    }

    // --- Exposed Room Database Flow Objects ---

    /**
     * Direct Flow observing all routines from the Room database.
     */
    val allRoutinesFlow: Flow<List<Routine>> = repository.allRoutines

    /**
     * StateFlow exposing the current list of routines to UI composables.
     */
    val routines: StateFlow<List<Routine>> = repository.allRoutines
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Set of currently executing routine IDs.
     */
    val runningRoutineIds: StateFlow<Set<Long>> = com.vpmapps.pixelflow.service.RoutineExecutionTracker.runningRoutineIds

    /**
     * Alias for routine list StateFlow.
     */
    val routineList: StateFlow<List<Routine>> = routines

    /**
     * Direct Flow observing execution history logs.
     */
    val allLogsFlow: Flow<List<RoutineExecutionLog>> = repository.allLogs

    /**
     * StateFlow exposing execution logs to UI composables.
     */
    val logs: StateFlow<List<RoutineExecutionLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Returns a Flow of execution history logs specific to a given routine ID.
     */
    fun getLogsForRoutine(routineId: Long): Flow<List<RoutineExecutionLog>> {
        return repository.getLogsForRoutine(routineId)
    }

    // --- Shizuku State & Communications ---

    val shizukuState: StateFlow<ShizukuState> = ShizukuManager.state
    val shizukuVersion: StateFlow<Int> = ShizukuManager.version
    val isShizukuPermissionGranted: StateFlow<Boolean> = ShizukuManager.isPermissionGranted
    val isShizukuReconnecting: StateFlow<Boolean> = ShizukuPermissionManager.isReconnecting
    val isWifiConnected: StateFlow<Boolean> = NetworkStatusMonitor.isWifiConnected
    val networkDescription: StateFlow<String> = NetworkStatusMonitor.networkDescription

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _consoleOutput = MutableStateFlow<ShellResult?>(null)
    val consoleOutput: StateFlow<ShellResult?> = _consoleOutput.asStateFlow()

    private val _areNotificationsEnabled = MutableStateFlow(
        RoutineNotificationManager.areNotificationsEnabled(application)
    )
    val areNotificationsEnabled: StateFlow<Boolean> = _areNotificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        RoutineNotificationManager.setNotificationsEnabled(getApplication(), enabled)
        _areNotificationsEnabled.value = enabled
        viewModelScope.launch {
            _userMessage.emit("Routine execution notifications ${if (enabled) "enabled" else "disabled"}")
        }
    }

    fun sendTestNotification() {
        RoutineNotificationManager.sendTestNotification(getApplication())
        viewModelScope.launch {
            _userMessage.emit("Sent test execution notification to system tray")
        }
    }

    // --- Routine State Toggling Methods ---

    /**
     * Sets up everything an enabled routine needs to notice its conditions, then checks them right
     * away so a routine whose conditions are already met starts immediately.
     */
    private fun armRoutine(routine: Routine) {
        val app = getApplication<Application>()
        RoutineAlarmManager.scheduleExactRoutineAlarm(app, routine)
        GeofenceManager.registerGeofence(app, routine)
        RoutineAlarmManager.scheduleRoutineCustomNotifications(app, routine)
        TriggerMonitorService.refresh(app)
        TriggerEvaluator.evaluateAsync(app, routine.id)
    }

    /**
     * Removes a routine's alarms and geofence. When [restoreReason] is set and the routine is
     * currently on, its previous settings are restored.
     */
    private suspend fun disarmRoutine(routine: Routine?, routineId: Long, restoreReason: String?) {
        val app = getApplication<Application>()
        RoutineAlarmManager.cancelRoutineAlarm(app, routineId)
        RoutineAlarmManager.cancelCustomNotificationAlarm(app, routineId)
        GeofenceManager.removeGeofence(app, routineId)
        DeviceFactsStore.clearGeofence(app, routineId)
        RoutineActivationStore.clear(app, routineId)
        if (routine != null && restoreReason != null && routine.restoreOnEnd &&
            RoutineStateSnapshotManager.hasSnapshot(app, routineId)
        ) {
            executionEngine.restoreRoutineSettings(routine, restoreReason)
        }
        TriggerMonitorService.refresh(app)
    }

    /**
     * Toggles the enabled status of the given routine.
     */
    fun toggleRoutineEnabled(routine: Routine) {
        viewModelScope.launch {
            val newState = !routine.isEnabled
            repository.toggleRoutine(routine.id, newState)
            if (newState) {
                armRoutine(routine.copy(isEnabled = true))
            } else {
                disarmRoutine(routine, routine.id, restoreReason = "Routine Disabled")
            }
            WidgetUpdateHelper.updateAllWidgets(getApplication())
            _userMessage.emit("Routine \"${routine.title}\" ${if (newState) "enabled" else "disabled"}")
        }
    }

    /**
     * Toggles the enabled status of a routine by its ID.
     */
    fun toggleRoutineEnabled(routineId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleRoutine(routineId, isEnabled)
            val routine = repository.getRoutineById(routineId)
            if (isEnabled) {
                routine?.let { armRoutine(it.copy(isEnabled = true)) }
            } else {
                disarmRoutine(routine, routineId, restoreReason = "Routine Disabled")
            }
            _userMessage.emit("Routine ${if (isEnabled) "enabled" else "disabled"}")
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    /**
     * Explicit setter for routine enabled state.
     */
    fun setRoutineEnabled(routineId: Long, isEnabled: Boolean) {
        toggleRoutineEnabled(routineId, isEnabled)
    }

    /**
     * Alias for toggleRoutineEnabled(routine: Routine).
     */
    fun toggleRoutine(routine: Routine) {
        toggleRoutineEnabled(routine)
    }

    // --- Routine CRUD & Operations ---

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            disarmRoutine(routine, routine.id, restoreReason = "Routine Deleted")
            repository.deleteRoutine(routine)
            WidgetUpdateHelper.updateAllWidgets(getApplication())
            _userMessage.emit("Routine \"${routine.title}\" deleted")
        }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            disarmRoutine(repository.getRoutineById(routineId), routineId, restoreReason = "Routine Deleted")
            repository.deleteRoutineById(routineId)
            WidgetUpdateHelper.updateAllWidgets(getApplication())
            _userMessage.emit("Routine deleted")
        }
    }

    fun saveRoutine(routine: Routine, onComplete: () -> Unit) {
        viewModelScope.launch {
            val savedRoutine = if (routine.id == 0L) {
                val insertedId = repository.insertRoutine(routine)
                _userMessage.emit("Created \"${routine.title}\"")
                routine.copy(id = insertedId)
            } else {
                repository.updateRoutine(routine)
                _userMessage.emit("Updated \"${routine.title}\"")
                routine
            }
            if (savedRoutine.isEnabled) {
                // Clear alarms/geofence of conditions that were removed, then arm the current ones.
                // The routine's on/off state is kept, so an edit that still matches doesn't re-run it.
                val app = getApplication<Application>()
                RoutineAlarmManager.cancelRoutineAlarm(app, savedRoutine.id)
                GeofenceManager.removeGeofence(app, savedRoutine.id)
                armRoutine(savedRoutine)
            } else {
                RoutineAlarmManager.cancelCustomNotificationAlarm(getApplication(), savedRoutine.id)
            }
            WidgetUpdateHelper.updateAllWidgets(getApplication())
            onComplete()
        }
    }

    fun executeRoutineNow(routine: Routine) {
        viewModelScope.launch {
            _userMessage.emit("Running \"${routine.title}\"...")
            val success = executionEngine.executeRoutine(routine, "Manual Quick Run")
            if (success) {
                _userMessage.emit("Routine \"${routine.title}\" executed successfully!")
            } else {
                _userMessage.emit("Routine \"${routine.title}\" executed with warnings (check logs)")
            }
        }
    }

    fun restoreRoutineNow(routine: Routine) {
        viewModelScope.launch {
            _userMessage.emit("Restoring settings for \"${routine.title}\"...")
            val success = executionEngine.restoreRoutineSettings(routine, "Manually Ended by User")
            if (success) {
                _userMessage.emit("Restored settings to how they were before \"${routine.title}\"!")
            } else {
                _userMessage.emit("No saved settings snapshot found for \"${routine.title}\"")
            }
            com.vpmapps.pixelflow.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun executeConsoleCommand(cmd: String) {
        viewModelScope.launch {
            val result = ShizukuManager.executeCommand(cmd)
            _consoleOutput.value = result
        }
    }

    /**
     * Grants usage access (for "App opened" routines) through Shizuku and reports whether it worked.
     */
    fun grantUsageAccessViaShizuku(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val granted = com.vpmapps.pixelflow.util.SpecialAccessHelper.grantUsageAccessViaShizuku(getApplication())
            _userMessage.emit(if (granted) "Usage access granted" else "Couldn't grant usage access automatically")
            if (granted) TriggerMonitorService.refresh(getApplication())
            onResult(granted)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _userMessage.emit("Execution logs cleared")
        }
    }

    fun checkShizukuStatus() {
        viewModelScope.launch {
            ShizukuManager.checkStatus()
        }
    }

    fun requestShizukuPermission() {
        viewModelScope.launch {
            ShizukuManager.checkStatus()
            val dispatched = ShizukuManager.helper.requestPermission()
            if (dispatched) {
                _userMessage.emit("Permission dialog dispatched. Please tap Allow.")
            } else if (ShizukuManager.isAuthorized()) {
                _userMessage.emit("Shizuku is already authorized!")
            } else {
                _userMessage.emit("Shizuku binder not active. Start Shizuku via Wireless Debugging first.")
            }
        }
    }

    fun refreshShizukuStatus() {
        viewModelScope.launch {
            val isAlive = ShizukuPermissionManager.reconnectShizuku(force = true)
            if (isAlive) {
                if (ShizukuManager.isAuthorized()) {
                    _userMessage.emit("Shizuku re-bound & authorized (API v${ShizukuManager.version.value})")
                } else {
                    _userMessage.emit("Shizuku binder re-bound! Requesting permission...")
                    ShizukuManager.requestPermission()
                }
            } else {
                _userMessage.emit("Shizuku binder offline. Start Shizuku via Wireless Debugging.")
            }
        }
    }

    fun reconnectShizuku() {
        viewModelScope.launch {
            _userMessage.emit("Reconnecting to Shizuku service...")
            val isAlive = ShizukuPermissionManager.reconnectShizuku(force = true)
            if (isAlive) {
                _userMessage.emit("Shizuku reconnected successfully!")
            } else {
                _userMessage.emit("Shizuku binder unreachable. Ensure Wireless Debugging is running.")
            }
        }
    }

    fun grantDndAccessViaShizuku() {
        viewModelScope.launch {
            _userMessage.emit("Granting Do Not Disturb policy via Shizuku...")
            val pkg = getApplication<Application>().packageName
            val result = ShizukuManager.grantNotificationPolicyAccess(pkg)
            if (result.isSuccess) {
                _userMessage.emit("Do Not Disturb access granted via Shizuku!")
            } else {
                _userMessage.emit("Shizuku offline or unauthorized: ${result.stderr.ifEmpty { "Please allow Shizuku permission first" }}")
            }
        }
    }
}
