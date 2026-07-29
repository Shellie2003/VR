package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Etagere
import com.example.data.model.NiveauEtagere
import com.example.data.model.ProduitNiveau
import kotlinx.coroutines.flow.Flow

@Dao
interface EtagereDao {
    @Query("SELECT * FROM etageres ORDER BY position ASC")
    fun getAllEtageres(): Flow<List<Etagere>>

    @Query("SELECT * FROM niveaux_etagere ORDER BY position ASC")
    fun getAllNiveaux(): Flow<List<NiveauEtagere>>

    @Query("SELECT * FROM produits_niveau")
    fun getAllProduitsNiveau(): Flow<List<ProduitNiveau>>

    @Insert
    suspend fun insertEtagere(etagere: Etagere): Long

    @Update
    suspend fun updateEtagere(etagere: Etagere)

    @Query("DELETE FROM etageres WHERE id = :etagereId")
    suspend fun deleteEtagereById(etagereId: Long)

    @Insert
    suspend fun insertNiveau(niveau: NiveauEtagere): Long

    @Update
    suspend fun updateNiveau(niveau: NiveauEtagere)

    @Query("DELETE FROM niveaux_etagere WHERE id = :niveauId")
    suspend fun deleteNiveauById(niveauId: Long)

    @Query("DELETE FROM niveaux_etagere WHERE etagereId = :etagereId")
    suspend fun deleteNiveauxByEtagere(etagereId: Long)

    // IGNORE : un double-tap sur "ajouter ce produit" est un doublon harmless grâce à l'index
    // unique (niveauId, produitId), pas une vraie tentative d'insertion concurrente.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProduitNiveau(lien: ProduitNiveau): Long

    @Query("DELETE FROM produits_niveau WHERE id = :id")
    suspend fun deleteProduitNiveauById(id: Long)

    @Query("DELETE FROM produits_niveau WHERE niveauId = :niveauId")
    suspend fun deleteProduitsNiveauByNiveau(niveauId: Long)

    @Query("DELETE FROM produits_niveau WHERE niveauId IN (SELECT id FROM niveaux_etagere WHERE etagereId = :etagereId)")
    suspend fun deleteProduitsNiveauByEtagere(etagereId: Long)

    @Query("SELECT * FROM etageres WHERE nom = :nom AND position = :position LIMIT 1")
    suspend fun findEtagereByNaturalKey(nom: String, position: Int): Etagere?

    @Query("SELECT * FROM niveaux_etagere WHERE etagereId = :etagereId AND position = :position LIMIT 1")
    suspend fun findNiveauByNaturalKey(etagereId: Long, position: Int): NiveauEtagere?

    @Query("SELECT * FROM produits_niveau WHERE niveauId = :niveauId AND produitId = :produitId LIMIT 1")
    suspend fun findProduitNiveau(niveauId: Long, produitId: Int): ProduitNiveau?
}
