package com.example.shizuku

/**
 * Representation of Shizuku service status.
 */
enum class ShizukuState {
    AUTHORIZED,
    PERMISSION_REQUIRED,
    UNAVAILABLE
}

/**
 * Execution outcome of an elevated shell command executed via Shizuku.
 */
data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executedCommand: String,
    val isSuccess: Boolean
)
