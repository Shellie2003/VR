package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

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
    val lignesVenteDao: LigneVenteDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
    val allDebts: Flow<List<Debt>> = debtDao.getAllDebts()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()

    fun getLimitedProducts(limit: Int): Flow<List<Product>> = productDao.getLimitedProducts(limit)
    fun searchProducts(query: String, category: String, showLowStockOnly: Boolean): Flow<List<Product>> = 
        productDao.searchProducts(query, category, showLowStockOnly)
    fun hasProducts(): Flow<Boolean> = productDao.hasProducts()
    fun getProductsWithBarcodes(): Flow<List<Product>> = productDao.getProductsWithBarcodes()
    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getProductByBarcode(barcode)

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
        // Sync to the new relational Produits table
        val existingProduit = if (product.id != 0) produitDao.getProduitById(product.id.toLong()) else null
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
        val pId = produitDao.insertProduit(newProduit)
        val targetProduitId = if (product.id != 0) product.id.toLong() else pId

        // Upsert default UniteProduit
        val existingUnite = if (product.id != 0) uniteProduitDao.getUniteByBarcode(product.barcode) else null
        val baseUnite = UniteProduit(
            id = existingUnite?.id ?: 0L,
            produitId = targetProduitId,
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
        uniteProduitDao.insertUnite(baseUnite)

        // Record a MouvementStock
        val qtyBefore = existingProduit?.quantiteStock ?: 0.0
        val mvt = MouvementStock(
            produitId = targetProduitId,
            type = if (existingProduit == null) "ENTREE" else "CORRECTION",
            quantite = product.stock,
            quantiteAvant = qtyBefore,
            quantiteApres = product.stock,
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
        produitDao.insertProduit(newProduit)

        // Upsert default UniteProduit
        val existingUnite = if (product.barcode.isNotEmpty()) uniteProduitDao.getUniteByBarcode(product.barcode) else null
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
        uniteProduitDao.insertUnite(baseUnite)

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

    suspend fun getProductById(id: Int): Product? = productDao.getProductById(id)

    suspend fun insertSale(sale: Sale) {
        saleDao.insertSale(sale)

        // Convert to Vente (relational table)
        val newVente = Vente(
            dateVente = sale.timestamp,
            montantTotal = sale.totalAmount,
            modePaiement = "ESPECES"
        )
        val venteId = venteDao.insertVente(newVente)

        for (item in sale.items) {
            val produitId = item.productId.toLong()
            val existingProduit = produitDao.getProduitById(produitId)

            if (existingProduit != null) {
                // Get or create base UniteProduit
                var uniteId = 0L
                val units = if (existingProduit.codeBarrePrincipal?.isNotEmpty() == true) {
                    uniteProduitDao.getUniteByBarcode(existingProduit.codeBarrePrincipal)
                } else null

                if (units != null) {
                    uniteId = units.id
                } else {
                    val baseUnite = UniteProduit(
                        produitId = produitId,
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
                    produitId = produitId,
                    uniteId = uniteId,
                    quantite = item.quantity,
                    prixUnitaireApplique = item.price,
                    montantLigne = item.quantity * item.price
                )
                lignesVenteDao.insertLigneVente(ligne)

                // Record MouvementStock
                val mvt = MouvementStock(
                    produitId = produitId,
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
                produitDao.insertProduit(updatedProduit)
            }
        }
    }

    suspend fun checkoutSale(sale: Sale) = database.withTransaction {
        saleDao.insertSale(sale)

        // Convert to Vente (relational table)
        val newVente = Vente(
            dateVente = sale.timestamp,
            montantTotal = sale.totalAmount,
            modePaiement = "ESPECES"
        )
        val venteId = venteDao.insertVente(newVente)

        for (item in sale.items) {
            val produitId = item.productId.toLong()
            val existingProduit = produitDao.getProduitById(produitId)

            if (existingProduit != null) {
                // Get or create base UniteProduit
                var uniteId = 0L
                val units = if (existingProduit.codeBarrePrincipal?.isNotEmpty() == true) {
                    uniteProduitDao.getUniteByBarcode(existingProduit.codeBarrePrincipal)
                } else null

                if (units != null) {
                    uniteId = units.id
                } else {
                    val baseUnite = UniteProduit(
                        produitId = produitId,
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
                    produitId = produitId,
                    uniteId = uniteId,
                    quantite = item.quantity,
                    prixUnitaireApplique = item.price,
                    montantLigne = item.quantity * item.price
                )
                lignesVenteDao.insertLigneVente(ligne)

                // Record MouvementStock
                val mvt = MouvementStock(
                    produitId = produitId,
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
                produitDao.insertProduit(updatedProduit)
            }

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

    suspend fun deleteSale(sale: Sale) {
        saleDao.deleteSale(sale)
    }

    suspend fun insertDebt(debt: Debt) = debtDao.insertDebt(debt)
    suspend fun updateDebt(debt: Debt) = debtDao.updateDebt(debt)
    suspend fun deleteDebt(debt: Debt) = debtDao.deleteDebt(debt)
}
