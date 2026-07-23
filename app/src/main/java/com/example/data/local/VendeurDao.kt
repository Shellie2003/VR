package com.example.data.local

import androidx.room.*
import com.example.data.model.Vendeur
import kotlinx.coroutines.flow.Flow

@Dao
interface VendeurDao {
    @Query("SELECT * FROM vendeurs ORDER BY nom ASC")
    fun getAllVendeurs(): Flow<List<Vendeur>>

    @Query("SELECT * FROM vendeurs WHERE id = :id LIMIT 1")
    suspend fun getVendeurById(id: Long): Vendeur?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendeur(vendeur: Vendeur): Long

    @Update
    suspend fun updateVendeur(vendeur: Vendeur)

    @Delete
    suspend fun deleteVendeur(vendeur: Vendeur)
}
