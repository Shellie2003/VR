package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class InventoryRepository(
    private val database: AppDatabase,
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val debtDao: DebtDao,
    val produitDao: ProduitDao,
    val uniteProduitDao: UniteProduitDao,
    val reglePrixDao: ReglePrixDao,
    val fournisseurDao: FournisseurDao,
    val mouvementStockDao: MouvementStockDao,
    val lotProduitDao: LotProduitDao,
    val venteDao: VenteDao,
    val lignesVenteDao: LigneVenteDao,
    val restockDao: RestockDao,
    val mouvementCaisseDao: MouvementCaisseDao,
    val caisseSessionDao: CaisseSessionDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts().flowOn(kotlinx.coroutines.Dispatchers.IO)
    val allSales: Flow<List<Sale>> = saleDao.getAllSales().flowOn(kotlinx.coroutines.Dispatchers.IO)
    val allDebts: Flow<List<Debt>> = debtDao.getAllDebts().flowOn(kotlinx.coroutines.Dispatchers.IO)
    val allRestocks: Flow<List<Restock>> = restockDao.getAllRestocks().flowOn(kotlinx.coroutines.Dispatchers.IO)
    val allCategories: Flow<List<String>> = productDao.getAllCategories().flowOn(kotlinx.coroutines.Dispatchers.IO)
    val allMouvementsCaisse: Flow<List<MouvementCaisse>> = mouvementCaisseDao.getAllMouvements().flowOn(kotlinx.coroutines.Dispatchers.IO)
    val allVentes: Flow<List<Vente>> = venteDao.getAllVentes().flowOn(kotlinx.coroutines.Dispatchers.IO)
    val allCaisseSessions: Flow<List<CaisseSession>> = caisseSessionDao.getAllSessions().flowOn(kotlinx.coroutines.Dispatchers.IO)
    val openCaisseSession: Flow<CaisseSession?> = caisseSessionDao.getOpenSession().flowOn(kotlinx.coroutines.Dispatchers.IO)

    suspend fun insertRestock(restock: Restock) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        restockDao.insertRestock(restock)
    }

    suspend fun deleteRestock(restock: Restock) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        restockDao.deleteRestock(restock)
    }

    suspend fun insertMouvementCaisse(mouvement: MouvementCaisse) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        mouvementCaisseDao.insertMouvement(mouvement)
    }

    suspend fun deleteMouvementCaisse(mouvement: MouvementCaisse) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        mouvementCaisseDao.deleteMouvement(mouvement)
    }

    suspend fun insertCaisseSession(session: CaisseSession): Long = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        caisseSessionDao.insertSession(session)
    }

    suspend fun updateCaisseSession(session: CaisseSession) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        caisseSessionDao.updateSession(session)
    }

    suspend fun getCashSalesTotal(start: Long, end: Long): Double = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        venteDao.getCashSalesTotal(start, end)
    }

    fun getLimitedProducts(limit: Int): Flow<List<Product>> = productDao.getLimitedProducts(limit).flowOn(kotlinx.coroutines.Dispatchers.IO)
    fun searchProducts(query: String, category: String, showLowStockOnly: Boolean): Flow<List<Product>> = 
        productDao.searchProducts(query, category, showLowStockOnly).flowOn(kotlinx.coroutines.Dispatchers.IO)
    fun searchTemplateProducts(query: String, category: String): Flow<List<Product>> = 
        productDao.searchTemplateProducts(query, category).flowOn(kotlinx.coroutines.Dispatchers.IO)
    fun getAllTemplateCategories(): Flow<List<String>> = productDao.getAllTemplateCategories().flowOn(kotlinx.coroutines.Dispatchers.IO)
    fun hasProducts(): Flow<Boolean> = productDao.hasProducts().flowOn(kotlinx.coroutines.Dispatchers.IO)
    fun hasTemplates(): Flow<Boolean> = productDao.hasTemplates().flowOn(kotlinx.coroutines.Dispatchers.IO)
    fun getProductsWithBarcodes(): Flow<List<Product>> = productDao.getProductsWithBarcodes().flowOn(kotlinx.coroutines.Dispatchers.IO)
    suspend fun getProductByBarcode(barcode: String): Product? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        productDao.getProductByBarcode(barcode)
    }
    suspend fun getProductByName(name: String): Product? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        productDao.getProductByName(name)
    }

    suspend fun insertProduct(product: Product) {
        val generatedId = productDao.insertProduct(product)
        if (product.isTemplate) {
            return
        }
        val productWithId = product.copy(id = generatedId.toInt())
        // Sync to the new relational Produits table
        val existingProduit = produitDao.getProduitById(generatedId)
        val newProduit = Produit(
            id = generatedId,
            nom = productWithId.name,
            nomCourt = productWithId.nomCourt ?: productWithId.name,
            categorie = productWithId.category,
            sousCategorie = productWithId.sousCategorie,
            marque = productWithId.marque,
            description = productWithId.description,
            uniteBase = productWithId.unit,
            quantiteStock = productWithId.stock,
            seuilAlerte = productWithId.lowStockThreshold,
            stockMax = productWithId.stockMax,
            emplacement = productWithId.emplacement,
            prixAchatUniteBase = productWithId.prixAchatUniteBase,
            fournisseurId = productWithId.fournisseurId,
            gerePeremption = productWithId.gerePeremption,
            imageUrl = productWithId.imageUrl,
            codeBarrePrincipal = productWithId.barcode,
            taxable = productWithId.taxable,
            tauxTaxe = productWithId.tauxTaxe,
            actif = true,
            dateAjout = existingProduit?.dateAjout ?: System.currentTimeMillis(),
            dateDerniereMaj = System.currentTimeMillis()
        )
        if (existingProduit != null) {
            produitDao.updateProduit(newProduit)
        } else {
            produitDao.insertProduit(newProduit)
        }

        // Upsert default UniteProduit
        val existingUnite = (if (productWithId.barcode.isNotEmpty()) {
            uniteProduitDao.getUniteByBarcode(productWithId.barcode)
        } else null) ?: uniteProduitDao.getBaseUniteForProduit(generatedId)

        val baseUnite = UniteProduit(
            id = existingUnite?.id ?: 0L,
            produitId = generatedId,
            nomUnite = productWithId.unit,
            facteurVersBase = 1.0,
            prixVente = productWithId.price,
            prixAchat = productWithId.prixAchatUniteBase,
            codeBarre = productWithId.barcode.ifEmpty { null },
            estUniteBase = true,
            estUniteVenteDefaut = true,
            ordre = 0,
            actif = true
        )
        if (existingUnite != null) {
            uniteProduitDao.updateUnite(baseUnite)
        } else {
            uniteProduitDao.insertUnite(baseUnite)
        }

        // Record a MouvementStock
        val qtyBefore = existingProduit?.quantiteStock ?: 0.0
        val mvt = MouvementStock(
            produitId = generatedId,
            type = if (existingProduit == null) "ENTREE" else "CORRECTION",
            quantite = productWithId.stock,
            quantiteAvant = qtyBefore,
            quantiteApres = productWithId.stock,
            note = "Enregistré depuis le modèle Product"
        )
        mouvementStockDao.insertMouvement(mvt)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
        // Sync
        val existingProduit = produitDao.getProduitById(product.id.toLong())
        val newProduit = Produit(
            id = product.id.toLong(),
            nom = product.name,
            nomCourt = product.nomCourt ?: product.name,
            categorie = product.category,
            sousCategorie = product.sousCategorie,
            marque = product.marque,
            description = product.description,
            uniteBase = product.unit,
            quantiteStock = product.stock,
            seuilAlerte = product.lowStockThreshold,
            stockMax = product.stockMax,
            emplacement = product.emplacement,
            prixAchatUniteBase = product.prixAchatUniteBase,
            fournisseurId = product.fournisseurId,
            gerePeremption = product.gerePeremption,
            imageUrl = product.imageUrl,
            codeBarrePrincipal = product.barcode,
            taxable = product.taxable,
            tauxTaxe = product.tauxTaxe,
            actif = true,
            dateAjout = existingProduit?.dateAjout ?: System.currentTimeMillis(),
            dateDerniereMaj = System.currentTimeMillis()
        )
        if (existingProduit != null) {
            produitDao.updateProduit(newProduit)
        } else {
            produitDao.insertProduit(newProduit)
        }

        // Upsert default UniteProduit
        val existingUnite = (if (product.barcode.isNotEmpty()) {
            uniteProduitDao.getUniteByBarcode(product.barcode)
        } else null) ?: uniteProduitDao.getBaseUniteForProduit(product.id.toLong())

        val baseUnite = UniteProduit(
            id = existingUnite?.id ?: 0L,
            produitId = product.id.toLong(),
            nomUnite = product.unit,
            facteurVersBase = 1.0,
            prixVente = product.price,
            prixAchat = product.prixAchatUniteBase,
            codeBarre = product.barcode.ifEmpty { null },
            estUniteBase = true,
            estUniteVenteDefaut = true,
            ordre = 0,
            actif = true
        )
        if (existingUnite != null) {
            uniteProduitDao.updateUnite(baseUnite)
        } else {
            uniteProduitDao.insertUnite(baseUnite)
        }

        // Record movement if quantity changed
        val qtyBefore = existingProduit?.quantiteStock ?: 0.0
        if (qtyBefore != product.stock) {
            val mvt = MouvementStock(
                produitId = product.id.toLong(),
                type = "CORRECTION",
                quantite = Math.abs(product.stock - qtyBefore),
                quantiteAvant = qtyBefore,
                quantiteApres = product.stock,
                note = "Ajustement de stock depuis le modèle Product"
            )
            mouvementStockDao.insertMouvement(mvt)
        }
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
        val existing = produitDao.getProduitById(product.id.toLong())
        if (existing != null) {
            produitDao.deleteProduit(existing)
        }
    }

    suspend fun deleteProductById(id: Int) {
        productDao.deleteProductById(id)
        val existing = produitDao.getProduitById(id.toLong())
        if (existing != null) {
            produitDao.deleteProduit(existing)
        }
    }

    suspend fun getProductById(id: Int): Product? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun checkoutSale(sale: Sale, decrementStock: Boolean = true, modePaiement: String = "ESPECES") = database.withTransaction {
        saleDao.insertSale(sale)

        // Convert to Vente (relational table)
        val newVente = Vente(
            dateVente = sale.timestamp,
            montantTotal = sale.totalAmount,
            modePaiement = modePaiement
        )
        val venteId = venteDao.insertVente(newVente)

        for (item in sale.items) {
            val originalId = item.productId.toLong()
            var existingProduit = produitDao.getProduitById(originalId)

            if (existingProduit == null) {
                // Fallback to barcode
                val matchingLegacyProduct = productDao.getProductById(item.productId)
                if (matchingLegacyProduct != null && matchingLegacyProduct.barcode.isNotEmpty()) {
                    existingProduit = produitDao.getProduitByBarcode(matchingLegacyProduct.barcode)
                }
            }

            if (existingProduit == null) {
                // Fallback to name
                existingProduit = produitDao.getProduitByName(item.name)
            }

            if (existingProduit != null) {
                val targetId = existingProduit.id
                // Get or create base UniteProduit
                var uniteId = 0L
                val units = if (existingProduit.codeBarrePrincipal?.isNotEmpty() == true) {
                    uniteProduitDao.getUniteByBarcode(existingProduit.codeBarrePrincipal)
                } else null

                val baseUnit = if (units != null) units else uniteProduitDao.getBaseUniteForProduit(targetId)

                if (baseUnit != null) {
                    uniteId = baseUnit.id
                } else {
                    val baseUnite = UniteProduit(
                        produitId = targetId,
                        nomUnite = existingProduit.uniteBase,
                        facteurVersBase = 1.0,
                        prixVente = item.price,
                        prixAchat = existingProduit.prixAchatUniteBase,
                        codeBarre = existingProduit.codeBarrePrincipal?.ifEmpty { null },
                        estUniteBase = true,
                        estUniteVenteDefaut = true,
                        ordre = 0,
                        actif = true
                    )
                    uniteId = uniteProduitDao.insertUnite(baseUnite)
                }

                // Save LigneVente
                val ligne = LigneVente(
                    venteId = venteId,
                    produitId = targetId,
                    uniteId = uniteId,
                    quantite = item.quantity,
                    prixUnitaireApplique = item.price,
                    montantLigne = item.quantity * item.price
                )
                lignesVenteDao.insertLigneVente(ligne)

                if (decrementStock) {
                    // Record MouvementStock
                    val mvt = MouvementStock(
                        produitId = targetId,
                        type = "SORTIE_VENTE",
                        quantite = item.quantity,
                        quantiteAvant = existingProduit.quantiteStock,
                        quantiteApres = (existingProduit.quantiteStock - item.quantity).coerceAtLeast(0.0),
                        referenceId = venteId,
                        note = "Vente #${venteId}"
                    )
                    mouvementStockDao.insertMouvement(mvt)

                    // Update quantity stock of Produit
                    val updatedProduit = existingProduit.copy(
                        quantiteStock = (existingProduit.quantiteStock - item.quantity).coerceAtLeast(0.0),
                        dateDerniereMaj = System.currentTimeMillis()
                    )
                    produitDao.updateProduit(updatedProduit)
                }
            }

            if (decrementStock) {
                // Update legacy Product model stock to stay synchronized
                val matchingProduct = productDao.getProductById(item.productId)
                if (matchingProduct != null) {
                    val currentStock = matchingProduct.stock
                    val updatedStock = (currentStock - item.quantity).coerceAtLeast(0.0)
                    val updatedProduct = matchingProduct.copy(stock = updatedStock)
                    productDao.updateProduct(updatedProduct)
                }
            }
        }
    }

    suspend fun deleteSale(sale: Sale) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        saleDao.deleteSale(sale)
    }

    suspend fun insertDebt(debt: Debt) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        debtDao.insertDebt(debt)
    }
    suspend fun updateDebt(debt: Debt) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        debtDao.updateDebt(debt)
    }
    suspend fun deleteDebt(debt: Debt) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        debtDao.deleteDebt(debt)
    }
}
