package com.example.data.local

import androidx.room.*
import com.example.data.model.CaisseSession
import kotlinx.coroutines.flow.Flow

@Dao
interface CaisseSessionDao {
    @Query("SELECT * FROM caisse_sessions ORDER BY dateOuverture DESC")
    fun getAllSessions(): Flow<List<CaisseSession>>

    @Query("SELECT * FROM caisse_sessions WHERE dateFermeture IS NULL LIMIT 1")
    fun getOpenSession(): Flow<CaisseSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CaisseSession): Long

    @Update
    suspend fun updateSession(session: CaisseSession)
}
