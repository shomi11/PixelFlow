package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RoutineEntity
import com.example.data.model.RoutineExecutionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE isEnabled = 1")
    suspend fun getActiveRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: Long)

    @Query("SELECT COUNT(*) FROM routines")
    suspend fun getTotalRoutineCount(): Int

    @Query("UPDATE routines SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateRoutineEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE routines SET lastExecutedTimestamp = :timestamp, lastExecutionStatus = :status WHERE id = :id")
    suspend fun updateExecutionStatus(id: Long, timestamp: Long, status: String)

    // Execution logs
    @Query("SELECT * FROM routine_execution_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<RoutineExecutionLog>>

    @Query("SELECT * FROM routine_execution_logs WHERE routineId = :routineId ORDER BY timestamp DESC")
    fun getLogsForRoutine(routineId: Long): Flow<List<RoutineExecutionLog>>

    @Query("SELECT * FROM routine_execution_logs WHERE isSuccess = 0 ORDER BY timestamp DESC")
    fun getFailedLogs(): Flow<List<RoutineExecutionLog>>

    @Insert
    suspend fun insertLog(log: RoutineExecutionLog): Long

    @Query("DELETE FROM routine_execution_logs WHERE routineId = :routineId")
    suspend fun deleteLogsForRoutine(routineId: Long)

    @Query("DELETE FROM routine_execution_logs")
    suspend fun clearLogs()
}
