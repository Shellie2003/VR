package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProduitDao {
    @Query("SELECT * FROM produits ORDER BY nom ASC")
    fun getAllProduits(): Flow<List<Produit>>

    @Query("SELECT * FROM produits WHERE id = :id")
    suspend fun getProduitById(id: Long): Produit?

    @Query("SELECT * FROM produits WHERE nom = :name LIMIT 1")
    suspend fun getProduitByName(name: String): Produit?

    @Query("SELECT * FROM produits WHERE codeBarrePrincipal = :barcode LIMIT 1")
    suspend fun getProduitByBarcode(barcode: String): Produit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduit(produit: Produit): Long

    @Update
    suspend fun updateProduit(produit: Produit)

    @Delete
    suspend fun deleteProduit(produit: Produit)
}

@Dao
interface UniteProduitDao {
    @Query("SELECT * FROM unites_produit WHERE produitId = :produitId ORDER BY ordre ASC")
    fun getUnitesForProduit(produitId: Long): Flow<List<UniteProduit>>

    @Query("SELECT * FROM unites_produit WHERE produitId = :produitId AND estUniteBase = 1 LIMIT 1")
    suspend fun getBaseUniteForProduit(produitId: Long): UniteProduit?

    @Query("SELECT * FROM unites_produit WHERE codeBarre = :barcode LIMIT 1")
    suspend fun getUniteByBarcode(barcode: String): UniteProduit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnite(unite: UniteProduit): Long

    @Update
    suspend fun updateUnite(unite: UniteProduit)

    @Delete
    suspend fun deleteUnite(unite: UniteProduit)
}

@Dao
interface ReglePrixDao {
    @Query("SELECT * FROM regles_prix WHERE produitId = :produitId AND actif = 1")
    fun getReglesForProduit(produitId: Long): Flow<List<ReglePrix>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegle(regle: ReglePrix): Long

    @Update
    suspend fun updateRegle(regle: ReglePrix)

    @Delete
    suspend fun deleteRegle(regle: ReglePrix)
}

@Dao
interface FournisseurDao {
    @Query("SELECT * FROM fournisseurs ORDER BY nom ASC")
    fun getAllFournisseurs(): Flow<List<Fournisseur>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFournisseur(fournisseur: Fournisseur): Long

    @Update
    suspend fun updateFournisseur(fournisseur: Fournisseur)

    @Delete
    suspend fun deleteFournisseur(fournisseur: Fournisseur)
}

@Dao
interface MouvementStockDao {
    @Query("SELECT * FROM mouvements_stock WHERE produitId = :produitId ORDER BY dateMouvement DESC")
    fun getMouvementsForProduit(produitId: Long): Flow<List<MouvementStock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouvement(mouvement: MouvementStock): Long
}

@Dao
interface LotProduitDao {
    @Query("SELECT * FROM lots_produit WHERE produitId = :produitId ORDER BY datePeremption ASC")
    fun getLotsForProduit(produitId: Long): Flow<List<LotProduit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLot(lot: LotProduit): Long

    @Update
    suspend fun updateLot(lot: LotProduit)

    @Delete
    suspend fun deleteLot(lot: LotProduit)
}

@Dao
interface VenteDao {
    @Query("SELECT * FROM ventes ORDER BY dateVente DESC")
    fun getAllVentes(): Flow<List<Vente>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVente(vente: Vente): Long

    @Delete
    suspend fun deleteVente(vente: Vente)
}

@Dao
interface LigneVenteDao {
    @Query("SELECT * FROM lignes_vente WHERE venteId = :venteId")
    fun getLignesForVente(venteId: Long): Flow<List<LigneVente>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLigneVente(ligne: LigneVente): Long
}
