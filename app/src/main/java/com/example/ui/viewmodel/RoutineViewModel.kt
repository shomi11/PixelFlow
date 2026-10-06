package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.background.RoutineAlarmManager
import com.example.background.WorkManagerHelper
import com.example.data.local.AppDatabase
import com.example.data.model.Routine
import com.example.data.model.RoutineEntity
import com.example.data.model.RoutineExecutionLog
import com.example.data.model.TriggerType
import com.example.data.repository.RoutineRepository
import com.example.notification.RoutineNotificationManager
import com.example.service.RoutineExecutionEngine
import com.example.shizuku.NetworkStatusMonitor
import com.example.shizuku.ShellResult
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuPermissionManager
import com.example.shizuku.ShizukuState
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
     * Toggles the enabled status of the given routine.
     */
    fun toggleRoutineEnabled(routine: Routine) {
        viewModelScope.launch {
            val newState = !routine.isEnabled
            repository.toggleRoutine(routine.id, newState)
            if (newState) {
                if (routine.triggerType == TriggerType.TIME ||
                    routine.triggerType == TriggerType.SUNRISE ||
                    routine.triggerType == TriggerType.SUNSET) {
                    RoutineAlarmManager.scheduleExactRoutineAlarm(getApplication(), routine.copy(isEnabled = true))
                } else if (routine.triggerType == TriggerType.LOCATION) {
                    com.example.background.GeofenceManager.registerGeofence(getApplication(), routine.copy(isEnabled = true))
                }
            } else {
                RoutineAlarmManager.cancelRoutineAlarm(getApplication(), routine.id)
                com.example.background.GeofenceManager.removeGeofence(getApplication(), routine.id)
            }
            _userMessage.emit("Routine \"${routine.title}\" ${if (newState) "enabled" else "disabled"}")
        }
    }

    /**
     * Toggles the enabled status of a routine by its ID.
     */
    fun toggleRoutineEnabled(routineId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleRoutine(routineId, isEnabled)
            if (isEnabled) {
                repository.getRoutineById(routineId)?.let {
                    if (it.triggerType == TriggerType.TIME ||
                        it.triggerType == TriggerType.SUNRISE ||
                        it.triggerType == TriggerType.SUNSET) {
                        RoutineAlarmManager.scheduleExactRoutineAlarm(getApplication(), it.copy(isEnabled = true))
                    } else if (it.triggerType == TriggerType.LOCATION) {
                        com.example.background.GeofenceManager.registerGeofence(getApplication(), it.copy(isEnabled = true))
                    }
                }
            } else {
                RoutineAlarmManager.cancelRoutineAlarm(getApplication(), routineId)
                com.example.background.GeofenceManager.removeGeofence(getApplication(), routineId)
            }
            _userMessage.emit("Routine ${if (isEnabled) "enabled" else "disabled"}")
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
            RoutineAlarmManager.cancelRoutineAlarm(getApplication(), routine.id)
            com.example.background.GeofenceManager.removeGeofence(getApplication(), routine.id)
            repository.deleteRoutine(routine)
            _userMessage.emit("Routine \"${routine.title}\" deleted")
        }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            RoutineAlarmManager.cancelRoutineAlarm(getApplication(), routineId)
            com.example.background.GeofenceManager.removeGeofence(getApplication(), routineId)
            repository.deleteRoutineById(routineId)
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
                if (savedRoutine.triggerType == TriggerType.TIME ||
                    savedRoutine.triggerType == TriggerType.SUNRISE ||
                    savedRoutine.triggerType == TriggerType.SUNSET
                ) {
                    RoutineAlarmManager.scheduleExactRoutineAlarm(getApplication(), savedRoutine)
                } else if (savedRoutine.triggerType == TriggerType.LOCATION) {
                    com.example.background.GeofenceManager.registerGeofence(getApplication(), savedRoutine)
                }
            }
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

    fun executeConsoleCommand(cmd: String) {
        viewModelScope.launch {
            val result = ShizukuManager.executeCommand(cmd)
            _consoleOutput.value = result
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _userMessage.emit("Execution logs cleared")
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
}
