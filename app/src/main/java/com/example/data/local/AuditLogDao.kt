package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    // Le journal peut grossir vite dans une épicerie active : on n'en expose que les 500 dernières
    // entrées à l'écran, ce qui couvre largement plusieurs semaines de gestes sensibles.
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 500")
    fun getRecentLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getLogsSince(since: Long): List<AuditLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog)

    @Query("DELETE FROM audit_logs WHERE timestamp < :before")
    suspend fun deleteLogsBefore(before: Long)
}
