package com.vpmapps.pixelflow.shizuku

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

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
        lastUnavailableAt = 0L
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
     * Ensures Shizuku binder connectivity and authorization, waiting asynchronously
     * if the app was recently un-frozen or started in the background.
     */
    suspend fun ensureAuthorized(timeoutMs: Long = 3000L): Boolean = withContext(Dispatchers.IO) {
        if (checkPermission() && Shizuku.pingBinder()) {
            _isBinderAlive.value = true
            _state.value = ShizukuState.AUTHORIZED
            return@withContext true
        }

        initialize()
        try {
            ShizukuPermissionManager.reconnectShizuku(force = true)
        } catch (e: Throwable) {
            Log.w(TAG, "reconnectShizuku error: ${e.message}")
        }

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            checkBindingAndPermission()
            if (Shizuku.pingBinder() && checkPermission()) {
                Log.i(TAG, "Shizuku binder and permission confirmed ready after ${System.currentTimeMillis() - startTime}ms")
                _isBinderAlive.value = true
                _state.value = ShizukuState.AUTHORIZED
                return@withContext true
            }
            kotlinx.coroutines.delay(100)
        }
        val finalStatus = Shizuku.pingBinder() && checkPermission()
        Log.i(TAG, "Shizuku ensureAuthorized completed with status: $finalStatus")
        return@withContext finalStatus
    }

    /**
     * Robust execution method that runs shell operations using Shizuku.newProcess,
     * with root shell fallback for rooted environments.
     *
     * @param command The privileged shell command to execute (e.g. "settings put secure doze_always_on 1")
     * @return ShellResult containing exitCode, stdout, stderr, executedCommand, and isSuccess.
     */
    suspend fun executeCommand(command: String): ShellResult = withContext(Dispatchers.IO) {
        // 1. Fast check; wait for the binder only if Shizuku wasn't just found to be unavailable,
        // so a routine with several actions doesn't stall for seconds on every command.
        var isAuthorized = checkPermission() && Shizuku.pingBinder()
        if (!isAuthorized && System.currentTimeMillis() - lastUnavailableAt > UNAVAILABLE_BACKOFF_MS) {
            isAuthorized = ensureAuthorized(3000L)
        }
        if (!isAuthorized) {
            lastUnavailableAt = System.currentTimeMillis()
        }

        if (isAuthorized) {
            try {
                Log.d(TAG, "Executing privileged command via Shizuku.newProcess: $command")
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply { isAccessible = true }
                val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
                return@withContext collectProcess(process, command)
            } catch (e: Throwable) {
                Log.e(TAG, "Exception while executing Shizuku command: $command", e)
            }
        }

        // 2. Secondary fallback: check if root 'su' shell is available on device
        val rootResult = executeViaRoot(command)
        if (rootResult != null) {
            return@withContext rootResult
        }

        // 3. Neither Shizuku nor Root was available to execute privileged shell command
        Log.w(TAG, "Privileged execution unavailable for command: $command (Shizuku inactive/unauthorized, root unavailable)")
        ShellResult(
            exitCode = 1,
            stdout = "",
            stderr = "Privileged execution failed: Shizuku service is not running or authorized, and root is unavailable.",
            executedCommand = command,
            isSuccess = false
        )
    }

    private suspend fun executeViaRoot(command: String): ShellResult? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val result = collectProcess(process, "su -c $command")
            if (result.exitCode == 0 || result.stdout.isNotBlank()) {
                Log.i(TAG, "Executed command successfully via root 'su': $command")
                result
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Time after a failed connection during which commands fail fast instead of waiting again. */
    private const val UNAVAILABLE_BACKOFF_MS = 15_000L

    /** Upper bound for one shell command; system "cmd" calls normally finish in well under a second. */
    private const val COMMAND_TIMEOUT_SECONDS = 20L

    @Volatile
    private var lastUnavailableAt = 0L

    /**
     * Reads stdout and stderr in parallel (reading one after the other can deadlock once the other
     * pipe's buffer fills) and kills the process if it runs longer than [COMMAND_TIMEOUT_SECONDS].
     */
    private suspend fun collectProcess(process: Process, command: String): ShellResult = coroutineScope {
        val stdoutJob = async(Dispatchers.IO) {
            try { process.inputStream.bufferedReader().use { it.readText() }.trim() } catch (e: Exception) { "" }
        }
        val stderrJob = async(Dispatchers.IO) {
            try { process.errorStream.bufferedReader().use { it.readText() }.trim() } catch (e: Exception) { "" }
        }
        // Blocking waitFor() in its own job: Shizuku's remote process doesn't reliably support the
        // timed waitFor(long, TimeUnit) overload. destroy() unblocks it on timeout.
        val exitJob = async(Dispatchers.IO) { process.waitFor() }
        val exitValue = withTimeoutOrNull(TimeUnit.SECONDS.toMillis(COMMAND_TIMEOUT_SECONDS)) { exitJob.await() }
        val finished = exitValue != null
        if (!finished) {
            Log.w(TAG, "Command timed out after ${COMMAND_TIMEOUT_SECONDS}s: $command")
            try { process.destroy() } catch (e: Exception) { /* already gone */ }
            exitJob.cancel()
        }
        val stdout = stdoutJob.await()
        val stderr = stderrJob.await()
        val exitCode = exitValue ?: 124
        ShellResult(
            exitCode = exitCode,
            stdout = stdout,
            stderr = if (finished) stderr else "Timed out after ${COMMAND_TIMEOUT_SECONDS}s. $stderr".trim(),
            executedCommand = command,
            isSuccess = exitCode == 0
        )
    }
}
