package com.example.data.local

import androidx.room.*
import com.example.data.model.Restock
import kotlinx.coroutines.flow.Flow

@Dao
interface RestockDao {
    @Query("SELECT * FROM restocks ORDER BY timestamp DESC")
    fun getAllRestocks(): Flow<List<Restock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestock(restock: Restock)

    @Delete
    suspend fun deleteRestock(restock: Restock)

    @Query("DELETE FROM restocks WHERE id = :id")
    suspend fun deleteRestockById(id: Int)
}
