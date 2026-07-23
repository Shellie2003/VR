package com.example.data.local

import androidx.room.*
import com.example.data.model.MouvementCaisse
import kotlinx.coroutines.flow.Flow

@Dao
interface MouvementCaisseDao {
    @Query("SELECT * FROM mouvements_caisse ORDER BY date DESC")
    fun getAllMouvements(): Flow<List<MouvementCaisse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouvement(mouvement: MouvementCaisse): Long

    @Delete
    suspend fun deleteMouvement(mouvement: MouvementCaisse)
}
