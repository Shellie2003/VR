package com.example.data.local

import androidx.room.*
import com.example.data.model.Retour
import kotlinx.coroutines.flow.Flow

@Dao
interface RetourDao {
    @Query("SELECT * FROM retours ORDER BY timestamp DESC")
    fun getAllRetours(): Flow<List<Retour>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRetour(retour: Retour): Long

    @Delete
    suspend fun deleteRetour(retour: Retour)
}
