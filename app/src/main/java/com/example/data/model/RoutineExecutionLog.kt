package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity storing execution history logs for each routine,
 * including timestamps, success status, and error messages / exit codes
 * for privileged shell commands executed via Shizuku.
 */
@Entity(
    tableName = "routine_execution_logs",
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["timestamp"]),
        Index(value = ["isSuccess"])
    ]
)
data class RoutineExecutionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "routineId")
    val routineId: Long,

    @ColumnInfo(name = "routineTitle")
    val routineTitle: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "isSuccess")
    val isSuccess: Boolean,

    @ColumnInfo(name = "status")
    val status: String = if (isSuccess) "SUCCESS" else "FAILED", // SUCCESS, FAILED, SIMULATED, WARNING

    @ColumnInfo(name = "executedCommands")
    val executedCommands: String = "",

    @ColumnInfo(name = "stdout")
    val stdout: String = "",

    @ColumnInfo(name = "errorMessage")
    val errorMessage: String? = null,

    @ColumnInfo(name = "exitCode")
    val exitCode: Int = 0,

    @ColumnInfo(name = "triggerSummary")
    val triggerSummary: String = "",

    @ColumnInfo(name = "actionsSummary")
    val actionsSummary: String = "",

    @ColumnInfo(name = "shizukuUsed")
    val shizukuUsed: Boolean = true,

    @ColumnInfo(name = "outputDetails")
    val outputDetails: String = ""
)
