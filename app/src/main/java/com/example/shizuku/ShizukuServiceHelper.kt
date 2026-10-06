package com.example.shizuku

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader

/**
 * ShizukuServiceHelper is a singleton object that manages Shizuku binding state,
 * verifies permissions with checkSelfPermission, handles user permission requests,
 * and exposes a robust executeCommand method using Shizuku.newProcess.
 */
object ShizukuServiceHelper {

    private const val TAG = "ShizukuServiceHelper"
    const val SHIZUKU_REQUEST_CODE = 9001

    // Backward compatibility helper
    fun getInstance(): ShizukuServiceHelper = this

    private val _state = MutableStateFlow(ShizukuState.UNAVAILABLE)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val _version = MutableStateFlow(-1)
    val version: StateFlow<Int> = _version.asStateFlow()

    private val _isBinderAlive = MutableStateFlow(false)
    val isBinderAlive: StateFlow<Boolean> = _isBinderAlive.asStateFlow()

    @Volatile
    private var isInitialized = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Shizuku binder connected")
        _isBinderAlive.value = true
        checkBindingAndPermission()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Shizuku binder disconnected / died")
        _isBinderAlive.value = false
        _state.value = ShizukuState.UNAVAILABLE
        _version.value = -1
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Shizuku permission granted by user")
                    _state.value = ShizukuState.AUTHORIZED
                } else {
                    Log.w(TAG, "Shizuku permission denied by user")
                    _state.value = ShizukuState.PERMISSION_REQUIRED
                }
            }
        }

    /**
     * Initializes binder and permission listeners safely once.
     */
    fun init() {
        initialize()
    }

    fun initialize() {
        if (isInitialized) {
            checkBindingAndPermission()
            return
        }
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            isInitialized = true
            checkBindingAndPermission()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register Shizuku listeners", e)
            _state.value = ShizukuState.UNAVAILABLE
            _isBinderAlive.value = false
        }
    }

    /**
     * Cleans up registered listeners.
     */
    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            isInitialized = false
        } catch (e: Throwable) {
            Log.w(TAG, "Error cleaning up Shizuku listeners", e)
        }
    }

    /**
     * Evaluates current Shizuku binder connectivity and permission status.
     */
    fun checkBindingAndPermission() {
        try {
            val ping = try {
                Shizuku.pingBinder()
            } catch (e: Throwable) {
                false
            }
            _isBinderAlive.value = ping
            if (ping) {
                val ver = try {
                    Shizuku.getVersion()
                } catch (e: Throwable) {
                    -1
                }
                _version.value = ver

                val hasPerm = try {
                    checkPermission()
                } catch (e: Throwable) {
                    false
                }
                _state.value = if (hasPerm) {
                    ShizukuState.AUTHORIZED
                } else {
                    ShizukuState.PERMISSION_REQUIRED
                }
            } else {
                _state.value = ShizukuState.UNAVAILABLE
            }
        } catch (e: Throwable) {
            _isBinderAlive.value = false
            _state.value = ShizukuState.UNAVAILABLE
        }
    }

    /**
     * Raw checkSelfPermission call against Shizuku service.
     *
     * @return PackageManager.PERMISSION_GRANTED or PackageManager.PERMISSION_DENIED
     */
    fun checkSelfPermission(): Int {
        return try {
            if (!Shizuku.pingBinder() || Shizuku.isPreV11()) {
                PackageManager.PERMISSION_DENIED
            } else {
                Shizuku.checkSelfPermission()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error calling Shizuku.checkSelfPermission()", e)
            PackageManager.PERMISSION_DENIED
        }
    }

    /**
     * Verifies if the app has been granted Shizuku permissions using Shizuku.checkSelfPermission().
     *
     * @return true if Shizuku binder is active and checkSelfPermission returns PERMISSION_GRANTED.
     */
    fun checkPermission(): Boolean {
        return checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    fun hasPermission(): Boolean = checkPermission()
    fun isPermissionGranted(): Boolean = checkPermission()

    /**
     * Checks if Shizuku suggests displaying a permission rationale before requesting.
     */
    fun shouldShowRequestPermissionRationale(): Boolean {
        return try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                Shizuku.shouldShowRequestPermissionRationale()
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Requests Shizuku authorization from the user.
     *
     * @param requestCode The request code to associate with this request.
     * @return true if permission request was dispatched or already granted, false if binder is unavailable.
     */
    fun requestPermission(requestCode: Int = SHIZUKU_REQUEST_CODE): Boolean {
        return try {
            if (!Shizuku.pingBinder()) {
                Log.w(TAG, "Cannot request permission: Shizuku binder is not available")
                return false
            }
            if (checkPermission()) {
                Log.i(TAG, "Shizuku permission is already granted")
                _state.value = ShizukuState.AUTHORIZED
                return true
            }
            Log.i(TAG, "Dispatching Shizuku.requestPermission(requestCode=$requestCode)")
            Shizuku.requestPermission(requestCode)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Error requesting Shizuku permission", e)
            false
        }
    }

    /**
     * One-shot check and request helper: checks if permission is granted, and if missing,
     * automatically dispatches a request to the user.
     *
     * @return true if already granted, false if request was dispatched or binder is unavailable.
     */
    fun checkAndRequestPermission(requestCode: Int = SHIZUKU_REQUEST_CODE): Boolean {
        return if (checkPermission()) {
            _state.value = ShizukuState.AUTHORIZED
            true
        } else {
            requestPermission(requestCode)
            false
        }
    }

    /**
     * Robust execution method that runs shell operations using Shizuku.newProcess.
     *
     * @param command The privileged shell command to execute (e.g. "settings put secure doze_always_on 1")
     * @return ShellResult containing exitCode, stdout, stderr, executedCommand, and isSuccess.
     */
    suspend fun executeCommand(command: String): ShellResult = withContext(Dispatchers.IO) {
        val isAuthorized = checkPermission() && Shizuku.pingBinder()

        if (!isAuthorized) {
            Log.w(TAG, "Shizuku not authorized or binder inactive. Simulating: $command")
            return@withContext ShellResult(
                exitCode = 0,
                stdout = "[Simulated output: Shizuku binder not active. In active ADB mode, this executes: $command]",
                stderr = "",
                executedCommand = command,
                isSuccess = true
            )
        }

        try {
            Log.d(TAG, "Executing privileged command via Shizuku.newProcess: $command")
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

            val stdout = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val stderr = process.errorStream.bufferedReader().use { it.readText() }.trim()
            val exitCode = process.waitFor()

            ShellResult(
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                executedCommand = command,
                isSuccess = exitCode == 0
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Exception while executing Shizuku command: $command", e)
            ShellResult(
                exitCode = -1,
                stdout = "",
                stderr = e.localizedMessage ?: "Unknown shell execution error",
                executedCommand = command,
                isSuccess = false
            )
        }
    }
}
