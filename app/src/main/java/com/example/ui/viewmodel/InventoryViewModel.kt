package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import com.example.data.model.Debt
import com.example.data.model.Fournisseur
import com.example.data.model.MouvementCaisse
import com.example.data.repository.InventoryRepository
import com.example.util.AppPreferences
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CartItem(
    val id: String, // "product_<id>" or "misc_<uuid>"
    val name: String,
    val price: Double, // active price
    val quantity: Double,
    val unit: String,
    val productId: Int? = null, // null for misc items
    val maxStock: Double = 99999.0,
    val regularPrice: Double = price,
    val wholesalePrice: Double? = null
) {
    val totalPrice: Double get() = price * quantity
}

class InventoryViewModel(
    private val repository: InventoryRepository,
    private val context: Context
) : ViewModel() {

    private val coroutineExceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, exception ->
        android.util.Log.e("InventoryViewModel", "Hadisoana Coroutine: ${exception.localizedMessage ?: exception.message}", exception)
    }

    val appPreferences = AppPreferences(context)

    // Observable Preference States
    val language = MutableStateFlow(appPreferences.language)
    val isActivated = MutableStateFlow(appPreferences.isActivated)
    val installationId = appPreferences.installationId
    val firebaseBackupToken = appPreferences.firebaseBackupToken

    val groceryName = MutableStateFlow(appPreferences.groceryName)
    val colorTheme = MutableStateFlow(appPreferences.colorTheme)
    val themeMode = MutableStateFlow(appPreferences.themeMode)
    val shopMode = MutableStateFlow(appPreferences.shopMode)
    val excludedProductIds = MutableStateFlow(appPreferences.excludedProductIds)
    val firebaseDatabaseUrl = MutableStateFlow(appPreferences.firebaseDatabaseUrl)

    fun updateFirebaseDatabaseUrl(url: String) {
        appPreferences.firebaseDatabaseUrl = url
        firebaseDatabaseUrl.value = url
    }

    fun toggleProductSyncExclusion(productId: Int) {
        val currentSet = appPreferences.excludedProductIds.toMutableSet()
        val idStr = productId.toString()
        if (currentSet.contains(idStr)) {
            currentSet.remove(idStr)
        } else {
            currentSet.add(idStr)
        }
        appPreferences.excludedProductIds = currentSet
        excludedProductIds.value = currentSet
        triggerLocalSafetyBackup()
    }

    val themeColor: StateFlow<androidx.compose.ui.graphics.Color> = colorTheme.map {
        when (it) {
            "sunset" -> androidx.compose.ui.graphics.Color(0xFFE65100)
            "indigo" -> androidx.compose.ui.graphics.Color(0xFF1E3A8A)
            "rose" -> androidx.compose.ui.graphics.Color(0xFF881337)
            else -> androidx.compose.ui.graphics.Color(0xFF012D1D) // default primary
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), androidx.compose.ui.graphics.Color(0xFF012D1D))

    fun updateGroceryName(name: String) {
        appPreferences.groceryName = name
        groceryName.value = name
    }

    fun updateColorTheme(theme: String) {
        appPreferences.colorTheme = theme
        colorTheme.value = theme
    }

    fun updateThemeMode(mode: String) {
        appPreferences.themeMode = mode
        themeMode.value = mode
    }

    fun updateShopMode(mode: String) {
        appPreferences.shopMode = mode
        shopMode.value = mode
        // Recalculate cart item prices based on the new mode
        val current = _cart.value.map { item ->
            if (item.productId != null) {
                val activePrice = if (mode == "wholesale" && item.wholesalePrice != null && item.wholesalePrice > 0.0) {
                    item.wholesalePrice
                } else {
                    item.regularPrice
                }
                item.copy(price = activePrice)
            } else {
                item
            }
        }
        _cart.value = current
    }

    // Real-time trial remaining updates
    private val _trialTimeRemaining = MutableStateFlow(appPreferences.trialTimeRemainingMs)
    val trialTimeRemaining: StateFlow<Long> = _trialTimeRemaining.asStateFlow()

    // Filtering states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val showLowStockOnly = MutableStateFlow(false)

    // Debts state filtering
    val debtSearchQuery = MutableStateFlow("")
    val debtFilter = MutableStateFlow("Toutes") // Toutes, Non payées, Payées

    // Database Flows
    val allProducts: StateFlow<List<Product>> = repository.getLimitedProducts(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<Debt>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRestocks: StateFlow<List<com.example.data.model.Restock>> = repository.allRestocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cash register (caisse) manual movements: entrées (cash in) et sorties (cash out)
    val allMouvementsCaisse: StateFlow<List<MouvementCaisse>> = repository.allMouvementsCaisse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Theoretical cash balance: total sales (always cash) + manual cash-ins - manual cash-outs
    val soldeCaisse: StateFlow<Double> = combine(allSales, allMouvementsCaisse) { sales, mouvements ->
        val totalVentes = sales.sumOf { it.totalAmount }
        val totalEntrees = mouvements.filter { it.type == "ENTREE" }.sumOf { it.montant }
        val totalSorties = mouvements.filter { it.type == "SORTIE" }.sumOf { it.montant }
        totalVentes + totalEntrees - totalSorties
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun saveMouvementCaisse(mouvement: MouvementCaisse) {
        viewModelScope.launch(coroutineExceptionHandler) {
            repository.insertMouvementCaisse(mouvement)
            com.example.sync.SyncManager.triggerDatabaseSync()
            triggerLocalSafetyBackup()
        }
    }

    fun deleteMouvementCaisse(mouvement: MouvementCaisse) {
        viewModelScope.launch(coroutineExceptionHandler) {
            repository.deleteMouvementCaisse(mouvement)
            com.example.sync.SyncManager.triggerDatabaseSync()
            triggerLocalSafetyBackup()
        }
    }

    fun saveRestock(restock: com.example.data.model.Restock) {
        viewModelScope.launch(coroutineExceptionHandler) {
            repository.insertRestock(restock)
            triggerLocalSafetyBackup()
        }
    }

    fun deleteRestock(restock: com.example.data.model.Restock) {
        viewModelScope.launch(coroutineExceptionHandler) {
            repository.deleteRestock(restock)
            triggerLocalSafetyBackup()
        }
    }

    val categories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastClickedProductId = MutableStateFlow<Int?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val coOccurrenceMap: StateFlow<Map<Int, Map<Int, Int>>> = allSales
        .map { sales ->
            val map = mutableMapOf<Int, MutableMap<Int, Int>>()
            for (sale in sales) {
                val itemIds = sale.items.map { it.productId }.distinct()
                if (itemIds.size < 2) continue
                for (i in itemIds.indices) {
                    val p1 = itemIds[i]
                    if (p1 == 0) continue
                    for (j in i + 1 until itemIds.size) {
                        val p2 = itemIds[j]
                        if (p2 == 0) continue
                        map.getOrPut(p1) { mutableMapOf() }[p2] = (map[p1]?.get(p2) ?: 0) + 1
                        map.getOrPut(p2) { mutableMapOf() }[p1] = (map[p2]?.get(p1) ?: 0) + 1
                    }
                }
            }
            map
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun getSemanticScore(p1: Product, p2: Product): Double {
        val name1 = p1.name.lowercase()
        val name2 = p2.name.lowercase()
        var score = 0.0

        if (p1.category == p2.category) {
            score += 1.0
        }

        if (name1.contains("sucre")) {
            if (name2.contains("café") || name2.contains("cafe") || name2.contains("taf")) score += 20.0
            if (name2.contains("thé") || name2.contains("the")) score += 15.0
            if (name2.contains("lait") || name2.contains("francelait")) score += 15.0
            if (name2.contains("farine")) score += 10.0
            if (name2.contains("biscuit") || name2.contains("biski")) score += 8.0
        }
        if (name2.contains("sucre")) {
            if (name1.contains("café") || name1.contains("cafe") || name1.contains("taf")) score += 20.0
            if (name1.contains("thé") || name1.contains("the")) score += 15.0
            if (name1.contains("lait") || name1.contains("francelait")) score += 15.0
            if (name1.contains("farine")) score += 10.0
            if (name1.contains("biscuit") || name1.contains("biski")) score += 8.0
        }

        if (name1.contains("huile") || name1.contains("oil") || name1.contains("menaka")) {
            if (name2.contains("vary") || name2.contains("riz")) score += 25.0
            if (name2.contains("pâte") || name2.contains("pate") || name2.contains("mac") || name2.contains("spaghetti")) score += 20.0
            if (name2.contains("sardine")) score += 18.0
            if (name2.contains("sel")) score += 15.0
            if (name2.contains("farine")) score += 12.0
        }
        if (name2.contains("huile") || name2.contains("oil") || name2.contains("menaka")) {
            if (name1.contains("vary") || name1.contains("riz")) score += 25.0
            if (name1.contains("pâte") || name1.contains("pate") || name1.contains("mac") || name1.contains("spaghetti")) score += 20.0
            if (name1.contains("sardine")) score += 18.0
            if (name1.contains("sel")) score += 15.0
            if (name1.contains("farine")) score += 12.0
        }

        if (name1.contains("café") || name1.contains("cafe") || name1.contains("taf") || name1.contains("thé") || name1.contains("the")) {
            if (name2.contains("sucre")) score += 20.0
            if (name2.contains("lait") || name2.contains("francelait")) score += 15.0
        }
        if (name2.contains("café") || name2.contains("cafe") || name2.contains("taf") || name2.contains("thé") || name2.contains("the")) {
            if (name1.contains("sucre")) score += 20.0
            if (name1.contains("lait") || name1.contains("francelait")) score += 15.0
        }

        if (name1.contains("farine")) {
            if (name2.contains("levure") || name2.contains("gloripan")) score += 25.0
            if (name2.contains("sucre")) score += 15.0
            if (name2.contains("marga") || name2.contains("jadida") || name2.contains("orkide") || name2.contains("beurre")) score += 20.0
        }
        if (name2.contains("farine")) {
            if (name1.contains("levure") || name1.contains("gloripan")) score += 25.0
            if (name1.contains("sucre")) score += 15.0
            if (name1.contains("marga") || name1.contains("jadida") || name1.contains("orkide") || name1.contains("beurre")) score += 20.0
        }

        if (name1.contains("savon") || name1.contains("savony")) {
            if (name2.contains("dentifrice") || name2.contains("signal") || name2.contains("colgate")) score += 20.0
            if (name2.contains("brosse")) score += 15.0
            if (name2.contains("shampoing")) score += 15.0
            if (name2.contains("det ") || name2.contains("ariel") || name2.contains("oxi") || name2.contains("lessive")) score += 10.0
        }
        if (name2.contains("savon") || name2.contains("savony")) {
            if (name1.contains("dentifrice") || name1.contains("signal") || name1.contains("colgate")) score += 20.0
            if (name1.contains("brosse")) score += 15.0
            if (name1.contains("shampoing")) score += 15.0
            if (name1.contains("det ") || name1.contains("ariel") || name1.contains("oxi") || name1.contains("lessive")) score += 10.0
        }

        if (name1.contains("couche") || name1.contains("cou ")) {
            if (name2.contains("lingette")) score += 25.0
            if (name2.contains("goodcare") || name2.contains("bebem") || name2.contains("lait")) score += 15.0
        }
        if (name2.contains("couche") || name2.contains("cou ")) {
            if (name1.contains("lingette")) score += 25.0
            if (name1.contains("goodcare") || name1.contains("bebem") || name1.contains("lait")) score += 15.0
        }

        if (name1.contains("ampoule") || name1.contains("amp ")) {
            if (name2.contains("pile") || name2.contains("toshiba") || name2.contains("energy")) score += 20.0
            if (name2.contains("bougie") || name2.contains("mateza")) score += 15.0
            if (name2.contains("allumette") || name2.contains("briquet") || name2.contains("mimosa")) score += 15.0
        }
        if (name2.contains("ampoule") || name2.contains("amp ")) {
            if (name1.contains("pile") || name1.contains("toshiba") || name1.contains("energy")) score += 20.0
            if (name1.contains("bougie") || name1.contains("mateza")) score += 15.0
            if (name1.contains("allumette") || name1.contains("briquet") || name1.contains("mimosa")) score += 15.0
        }

        if (name1.contains("bougie") || name1.contains("mateza")) {
            if (name2.contains("allumette") || name2.contains("briquet") || name2.contains("mimosa") || name2.contains("roller")) score += 25.0
        }
        if (name2.contains("bougie") || name2.contains("mateza")) {
            if (name1.contains("allumette") || name1.contains("briquet") || name1.contains("mimosa") || name1.contains("roller")) score += 25.0
        }

        return score
    }

    // Combined Flow for searched & filtered products using database-level search and dynamic predictive association sorting
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredProducts: StateFlow<List<Product>> = combine(
        searchQuery,
        selectedCategory,
        showLowStockOnly
    ) { query, cat, lowStockOnly ->
        Triple(query, cat, lowStockOnly)
    }.flatMapLatest { (query, cat, lowStockOnly) ->
        repository.searchProducts(query, cat, lowStockOnly)
    }.combine(combine(lastClickedProductId, coOccurrenceMap) { lastId, coMap ->
        lastId to coMap
    }) { products, (lastId, coMap) ->
        if (lastId == null) {
            products
        } else {
            val clickedProduct = products.find { it.id == lastId } ?: allProducts.value.find { it.id == lastId }
            val clickedCoOccurrences = coMap[lastId] ?: emptyMap()
            
            products.sortedWith(compareByDescending { product ->
                if (product.id == lastId) {
                    999999.0
                } else {
                    val coOccurCount = clickedCoOccurrences[product.id] ?: 0
                    val coScore = coOccurCount * 1000.0
                    val semScore = if (clickedProduct != null) getSemanticScore(clickedProduct, product) else 0.0
                    coScore + semScore
                }
            })
        }
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined Flow for searched & filtered debts
    val filteredDebts: StateFlow<List<Debt>> = combine(
        allDebts,
        debtSearchQuery,
        debtFilter
    ) { debts, query, filter ->
        debts.filter { debt ->
            val matchesQuery = debt.debtorName.contains(query, ignoreCase = true) || 
                               debt.note.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "Payées" -> debt.isPaid
                "Non payées" -> !debt.isPaid
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total active debts amount
    val totalOutstandingDebts: Flow<Double> = allDebts.map { debts ->
        debts.filter { !it.isPaid }.sumOf { it.balance }
    }

    // Seeding products on empty state with Dispatchers.IO to prevent main-thread block
    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO + coroutineExceptionHandler) {
            repository.hasProducts().take(1).collect { hasProducts ->
                val hasBackupFile = com.example.util.BackupHelper.hasBackup(context)
                if (hasBackupFile) {
                    val restored = restoreLocalSafetyBackup()
                    if (restored) {
                        addSyncLog("Famerenana ny tahiry avy amin'ny backup fiarovana nahomby!")
                    }
                } else if (!hasProducts) {
                    seedSampleProducts()
                }
            }
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO + coroutineExceptionHandler) {
            repository.hasTemplates().take(1).collect { hasTemplates ->
                if (!hasTemplates || !appPreferences.hasSeededNewCategories) {
                    seedNewCategoriesAndProducts()
                    appPreferences.hasSeededNewCategories = true
                }
            }
        }
    }

    fun changeLanguage(lang: String) {
        appPreferences.language = lang
        language.value = lang
    }

    fun submitActivationCode(code: String): Boolean {
        val cleanCode = code.trim()
        val instId = installationId.toLongOrNull() ?: 0L
        val expected = ((instId * 3) + 123456) % 1000000
        val expectedStr = String.format("%06d", expected)
        
        if (cleanCode == expectedStr) {
            appPreferences.isActivated = true
            isActivated.value = true
            return true
        }
        return false
    }

    private suspend fun seedSampleProducts() {
        val sample1 = Product(
            name = "Vary Gasy",
            price = 3200.0,
            category = "Alimentation",
            stock = 50.0,
            lowStockThreshold = 20.0,
            unit = "Kilogramme",
            imageUrl = "",
            sku = "SKU-VARY-01",
            stock_quantity = 50
        )
        val sample2 = Product(
            name = "Karoty vao",
            price = 1500.0,
            category = "Légumes",
            stock = 20.0,
            lowStockThreshold = 10.0,
            unit = "Kilogramme",
            imageUrl = "",
            sku = "SKU-KAROTY-02",
            stock_quantity = 20
        )
        val sample3 = Product(
            name = "Menaka Gasy",
            price = 8800.0,
            category = "Alimentation",
            stock = 20.0,
            lowStockThreshold = 5.0,
            unit = "Litre",
            imageUrl = "",
            sku = "SKU-MENAKA-03",
            stock_quantity = 20
        )
        val sample4 = Product(
            name = "Biski / Biscuit",
            price = 600.0,
            category = "Autre",
            stock = 120.0,
            lowStockThreshold = 5.0,
            unit = "Pièce",
            imageUrl = "",
            sku = "SKU-BISKI-04",
            stock_quantity = 120
        )
        repository.insertProduct(sample1)
        repository.insertProduct(sample2)
        repository.insertProduct(sample3)
        repository.insertProduct(sample4)
    }

    private suspend fun seedNewCategoriesAndProducts() {
        val newProducts = listOf(
            // 1. Épicerie - Farine & Boulangerie
            Product(name = "Farine Special (EGY) 1Kg*10", price = 0.0, category = "Épicerie - Farine & Boulangerie", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Farine Special (EGYPT) 50 Kg", price = 0.0, category = "Épicerie - Farine & Boulangerie", stock = 0.0, unit = "Kilogramme", prixAchatUniteBase = 0.0),
            Product(name = "Farine VOILA EGYPT 50kg", price = 0.0, category = "Épicerie - Farine & Boulangerie", stock = 0.0, unit = "Kilogramme", prixAchatUniteBase = 0.0),
            Product(name = "Farine Voila Egypt", price = 0.0, category = "Épicerie - Farine & Boulangerie", stock = 0.0, unit = "Kilogramme", prixAchatUniteBase = 0.0),
            Product(name = "Levure Gloripan 500", price = 0.0, category = "Épicerie - Farine & Boulangerie", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 2. Épicerie - Pâtes alimentaires
            Product(name = "Mac Bella Vita Elbow 5KG", price = 0.0, category = "Épicerie - Pâtes alimentaires", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Mac Bella Vita Fus 5KG", price = 0.0, category = "Épicerie - Pâtes alimentaires", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Mac Bella Vita Twist 5KG", price = 0.0, category = "Épicerie - Pâtes alimentaires", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Mac Francia Elbow 500gm", price = 0.0, category = "Épicerie - Pâtes alimentaires", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Mac Francia Fusilli 500gm", price = 0.0, category = "Épicerie - Pâtes alimentaires", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 3. Épicerie - Huiles de cuisine
            Product(name = "Lubec Gear Oil SAE 90 1 LT *12", price = 0.0, category = "Épicerie - Huiles de cuisine", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Lubec Gear Oil SAE 90 5 LT *6", price = 0.0, category = "Épicerie - Huiles de cuisine", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Oil COCOTOP 5ltr *4", price = 0.0, category = "Épicerie - Huiles de cuisine", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Oil COCOTOP 5ltr*4", price = 0.0, category = "Épicerie - Huiles de cuisine", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Oil HINA 20L", price = 0.0, category = "Épicerie - Huiles de cuisine", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 4. Épicerie - Lait & Produits laitiers
            Product(name = "Crème Sucrée DANICA 1L", price = 0.0, category = "Épicerie - Lait & Produits laitiers", stock = 0.0, unit = "Litre", prixAchatUniteBase = 0.0),
            Product(name = "Francelait 400g Age 1", price = 0.0, category = "Épicerie - Lait & Produits laitiers", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Francelait 400g Age 2", price = 0.0, category = "Épicerie - Lait & Produits laitiers", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Francelait 400g Age 3", price = 0.0, category = "Épicerie - Lait & Produits laitiers", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Francelait 900g Age 1", price = 0.0, category = "Épicerie - Lait & Produits laitiers", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 5. Épicerie - Margarine & Matières grasses
            Product(name = "Marga Orkide 200gm * 24", price = 0.0, category = "Épicerie - Margarine & Matières grasses", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Margarine JADIDA*16", price = 0.0, category = "Épicerie - Margarine & Matières grasses", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 6. Épicerie - Conserves
            Product(name = "Mais Doux D OR 340gm*24", price = 0.0, category = "Épicerie - Conserves", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Mais Doux Soleil D'OR 340gm*24", price = 0.0, category = "Épicerie - Conserves", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Sardine Anny *50", price = 0.0, category = "Épicerie - Conserves", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Sardine Delmoneco", price = 0.0, category = "Épicerie - Conserves", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Sardine Omega MAVO *50", price = 0.0, category = "Épicerie - Conserves", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 7. Épicerie - Condiments, Épices & Assaisonnements
            Product(name = "Cube Calnort 10gm*60", price = 0.0, category = "Épicerie - Condiments, Épices & Assaisonnements", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Ketchup Heven plastique 340*12", price = 0.0, category = "Épicerie - Condiments, Épices & Assaisonnements", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Mayonese Lesieur 235 pm", price = 0.0, category = "Épicerie - Condiments, Épices & Assaisonnements", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Mayonese Lesieur 475 Gm", price = 0.0, category = "Épicerie - Condiments, Épices & Assaisonnements", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Mayonnaise Lesieur 710g*12", price = 0.0, category = "Épicerie - Condiments, Épices & Assaisonnements", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 8. Épicerie - Confiseries & Snacks
            Product(name = "Ani Cake Marble 160gm 4*6", price = 0.0, category = "Épicerie - Confiseries & Snacks", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Bingo Goma 120*12", price = 0.0, category = "Épicerie - Confiseries & Snacks", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Bonbon Lollypop MINI 50*20", price = 0.0, category = "Épicerie - Confiseries & Snacks", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Extra Baume *12", price = 0.0, category = "Épicerie - Confiseries & Snacks", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "I Believe 110gm *24", price = 0.0, category = "Épicerie - Confiseries & Snacks", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 9. Boissons - Café & Thé
            Product(name = "Cafe Salone 30gm*20", price = 0.0, category = "Boissons - Café & Thé", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cafe TAF 90 Gm *100", price = 0.0, category = "Boissons - Café & Thé", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cafe Taf 250 Gm *40", price = 0.0, category = "Boissons - Café & Thé", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cafe ZOTO 30g *20", price = 0.0, category = "Boissons - Café & Thé", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Café Taf TL 30*20", price = 0.0, category = "Boissons - Café & Thé", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 10. Boissons - Jus
            Product(name = "Jus Bashayer 1Ltr*12", price = 0.0, category = "Boissons - Jus", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Jus Dynamic*30", price = 0.0, category = "Boissons - Jus", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Jus Le Fruit 1L", price = 0.0, category = "Boissons - Jus", stock = 0.0, unit = "Litre", prixAchatUniteBase = 0.0),
            Product(name = "Jus Tampico 1Ltr", price = 0.0, category = "Boissons - Jus", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Jus Tampico 300ml", price = 0.0, category = "Boissons - Jus", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 11. Hygiène & Beauté
            Product(name = "Cali Jojoba *10", price = 0.0, category = "Hygiène & Beauté", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cali Kinana *10", price = 0.0, category = "Hygiène & Beauté", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cali Ravintsara Boite *10", price = 0.0, category = "Hygiène & Beauté", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cali Ravintsara*10", price = 0.0, category = "Hygiène & Beauté", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Color Cheveux 3in1 BELLE 30ML", price = 0.0, category = "Hygiène & Beauté", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 12. Bébé - Couches & Lingettes
            Product(name = "Cou Goodcare Twin Maxi 4 5*32", price = 0.0, category = "Bébé - Couches & Lingettes", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cou Goodcare Twin Midi 3*36", price = 0.0, category = "Bébé - Couches & Lingettes", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cou Goodcare Twin Mini 2 5*40", price = 0.0, category = "Bébé - Couches & Lingettes", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cou bebem PANTS Jun  26", price = 0.0, category = "Bébé - Couches & Lingettes", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Couche Adult Comfort Lx10", price = 0.0, category = "Bébé - Couches & Lingettes", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 13. Entretien ménager & Nettoyage
            Product(name = "Bleu D'azure", price = 0.0, category = "Entretien ménager & Nettoyage", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Det Ariel GM 30 *120", price = 0.0, category = "Entretien ménager & Nettoyage", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Det Extra Pro 30g*150", price = 0.0, category = "Entretien ménager & Nettoyage", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Det Extra Propre 15g *300", price = 0.0, category = "Entretien ménager & Nettoyage", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Det OXI 30g", price = 0.0, category = "Entretien ménager & Nettoyage", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 14. Entretien chaussures (Cirage)
            Product(name = "Pate Presto", price = 0.0, category = "Entretien chaussures (Cirage)", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pate Salone Mats {CR} x 40", price = 0.0, category = "Entretien chaussures (Cirage)", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pate Salone Mats {L} x 40", price = 0.0, category = "Entretien chaussures (Cirage)", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pate Salone Mats {P} x 40", price = 0.0, category = "Entretien chaussures (Cirage)", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pate Salone Mats {V} x 40", price = 0.0, category = "Entretien chaussures (Cirage)", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 15. Désodorisants & Parfums d'ambiance
            Product(name = "Boule Ext Auto GM", price = 0.0, category = "Désodorisants & Parfums d'ambiance", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Boule Ext Auto PM", price = 0.0, category = "Désodorisants & Parfums d'ambiance", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 16. Papeterie & Fournitures scolaires
            Product(name = "Cah Classmate 100P GF", price = 0.0, category = "Papeterie & Fournitures scolaires", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cah Classmate 100P PF", price = 0.0, category = "Papeterie & Fournitures scolaires", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cah Classmate 200P GF", price = 0.0, category = "Papeterie & Fournitures scolaires", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cah Classmate 200P PF", price = 0.0, category = "Papeterie & Fournitures scolaires", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Cah Madabook 200P PF", price = 0.0, category = "Papeterie & Fournitures scolaires", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 17. Éclairage (Ampoules)
            Product(name = "Amp LED B22 12WT", price = 0.0, category = "Éclairage (Ampoules)", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Amp LED B22 15WT", price = 0.0, category = "Éclairage (Ampoules)", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Amp LED B22 18WT", price = 0.0, category = "Éclairage (Ampoules)", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Amp LED E27 12WT", price = 0.0, category = "Éclairage (Ampoules)", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Amp LED E27 15WT", price = 0.0, category = "Éclairage (Ampoules)", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 18. Bougies
            Product(name = "Bougie Mateza GM", price = 0.0, category = "Bougies", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Bougie Mateza PM*30", price = 0.0, category = "Bougies", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Bougie Voila PM", price = 0.0, category = "Bougies", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Bougie ZAPP GM 50g*40", price = 0.0, category = "Bougies", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 19. Allumettes & Briquets
            Product(name = "Allumette Mimosa *10", price = 0.0, category = "Allumettes & Briquets", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Allumette Star Queen*10", price = 0.0, category = "Allumettes & Briquets", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Briq Voila Roller *20", price = 0.0, category = "Allumettes & Briquets", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Briq Voila TIC-TAK*20", price = 0.0, category = "Allumettes & Briquets", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Briquet SPECIAL *20", price = 0.0, category = "Allumettes & Briquets", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 20. Insecticides & Anti-moustiques
            Product(name = "ETKINTOX Insect 300ml*24", price = 0.0, category = "Insecticides & Anti-moustiques", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "ETKINTOX Insect 500ml*24", price = 0.0, category = "Insecticides & Anti-moustiques", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Kingtox Ody Moka * 60", price = 0.0, category = "Insecticides & Anti-moustiques", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Menabolo SALAMA 7ml*200", price = 0.0, category = "Insecticides & Anti-moustiques", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Moskila Ody Moka *60", price = 0.0, category = "Insecticides & Anti-moustiques", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 21. Piles électriques
            Product(name = "Pile Energy R 20 *24", price = 0.0, category = "Piles électriques", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pile Energy R 6 * 25", price = 0.0, category = "Piles électriques", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pile Toshiba R20- 10*20", price = 0.0, category = "Piles électriques", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pile VOILA R 20 * 24 Boit", price = 0.0, category = "Piles électriques", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 22. Quincaillerie
            Product(name = "Pointe 100X20 - 20 Kg", price = 0.0, category = "Quincaillerie", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pointe 120X20 - 20 Kg", price = 0.0, category = "Quincaillerie", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pointe 40X14 - 20 Kg", price = 0.0, category = "Quincaillerie", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pointe 50X16 - 20 Kg", price = 0.0, category = "Quincaillerie", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Pointe 50X18 - 20 Kg", price = 0.0, category = "Quincaillerie", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 23. Lubrifiants & Fluides moteur
            Product(name = "Graise BOSS JN MP-3* 15Kg", price = 0.0, category = "Lubrifiants & Fluides moteur", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Graise SPECIAL MP-3* 1Kg*24", price = 0.0, category = "Lubrifiants & Fluides moteur", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Graisse SCOPE N°3 15KG", price = 0.0, category = "Lubrifiants & Fluides moteur", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Huile BOSS 4T SAE 20W50 1Lt*12", price = 0.0, category = "Lubrifiants & Fluides moteur", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),
            Product(name = "Huile BOSS ATF Type A 1LTR*12", price = 0.0, category = "Lubrifiants & Fluides moteur", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0),

            // 24. Autres / Divers
            Product(name = "BONBON LOLLYPOP 48*16", price = 0.0, category = "Autres / Divers", stock = 0.0, unit = "Pièce", prixAchatUniteBase = 0.0)
        )
        newProducts.forEach { product ->
            repository.insertProduct(product.copy(isTemplate = true))
        }
    }

    // Cart Management
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // Calculated fields from Cart
    val cartTotal: Flow<Double> = _cart.map { items ->
        items.sumOf { it.totalPrice }
    }

    fun addToCart(product: Product, quantity: Double) {
        val current = _cart.value.toMutableList()
        val itemId = "product_${product.id}"
        val existingIndex = current.indexOfFirst { it.id == itemId }
        
        val activePrice = if (shopMode.value == "wholesale" && product.wholesalePrice != null && product.wholesalePrice > 0.0) {
            product.wholesalePrice
        } else {
            product.price
        }
        
        if (existingIndex != -1) {
            val updatedQty = current[existingIndex].quantity + quantity
            val cappedQty = updatedQty.coerceAtMost(product.stock)
            if (cappedQty > 0) {
                current[existingIndex] = current[existingIndex].copy(
                    quantity = cappedQty,
                    price = activePrice
                )
            }
        } else {
            val cappedQty = quantity.coerceAtMost(product.stock)
            if (cappedQty > 0) {
                current.add(
                    CartItem(
                        id = itemId,
                        name = product.name,
                        price = activePrice,
                        quantity = cappedQty,
                        unit = product.unit,
                        productId = product.id,
                        maxStock = product.stock,
                        regularPrice = product.price,
                        wholesalePrice = product.wholesalePrice
                    )
                )
            }
        }
        _cart.value = current
    }

    fun addMiscToCart(name: String, price: Double, quantity: Double) {
        val current = _cart.value.toMutableList()
        val uniqueId = "misc_${UUID.randomUUID()}"
        current.add(
            CartItem(
                id = uniqueId,
                name = name,
                price = price,
                quantity = quantity,
                unit = "Pièce",
                productId = null
            )
        )
        _cart.value = current
    }

    fun updateCartQuantity(itemId: String, quantity: Double) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = current[index]
            val capped = quantity.coerceIn(0.01, item.maxStock)
            current[index] = item.copy(quantity = capped)
            _cart.value = current
        }
    }

    fun changeCartQuantityByDelta(itemId: String, delta: Double) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = current[index]
            val updatedQty = item.quantity + delta
            if (updatedQty <= 0.0) {
                current.removeAt(index)
            } else {
                val capped = updatedQty.coerceAtMost(item.maxStock)
                current[index] = item.copy(quantity = capped)
            }
            _cart.value = current
        }
    }

    fun removeFromCart(itemId: String) {
        _cart.value = _cart.value.filter { it.id != itemId }
    }

    fun undoLastCartItem() {
        val current = _cart.value
        if (current.isNotEmpty()) {
            _cart.value = current.dropLast(1)
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // Checkout (creates sale record & decrements inventory stocks)
    fun checkoutCart(): Boolean {
        val cartSnapshot = _cart.value
        if (cartSnapshot.isEmpty()) return false

        // Check if we are a client terminal
        if (com.example.sync.SyncManager.isConnected.value && !com.example.sync.SyncManager.isServer.value) {
            // We are a Client! We must request validation from the Server.
            val soldItems = cartSnapshot.map {
                com.example.sync.ProductDeduction(
                    productId = it.productId?.toString() ?: "",
                    quantity = it.quantity
                )
            }
            
            // Generate Sale JSON
            val soldItemsForSale = cartSnapshot.map {
                SoldItem(
                    productId = it.productId ?: 0,
                    name = it.name,
                    quantity = it.quantity,
                    price = it.price
                )
            }
            val total = cartSnapshot.sumOf { it.totalPrice }
            val newSale = Sale(
                timestamp = System.currentTimeMillis(),
                totalAmount = total,
                items = soldItemsForSale
            )
            
            // Convert Sale to JSON using standard JSONObject
            val saleJsonObj = org.json.JSONObject()
            saleJsonObj.put("timestamp", newSale.timestamp)
            saleJsonObj.put("totalAmount", newSale.totalAmount)
            val itemsArr = org.json.JSONArray()
            newSale.items.forEach {
                val itemObj = org.json.JSONObject()
                itemObj.put("productId", it.productId)
                itemObj.put("name", it.name)
                itemObj.put("quantity", it.quantity)
                itemObj.put("price", it.price)
                itemsArr.put(itemObj)
            }
            saleJsonObj.put("items", itemsArr)
            val saleJson = saleJsonObj.toString()

            // Run blocking connection to server
            val success = kotlinx.coroutines.runBlocking {
                com.example.sync.SyncManager.requestSaleOnServer(saleJson, soldItems)
            }

            if (success) {
                clearCart()
                return true
            } else {
                return false
            }
        }

        viewModelScope.launch {
            try {
                val soldItems = cartSnapshot.map {
                    SoldItem(
                        productId = it.productId ?: 0,
                        name = it.name,
                        quantity = it.quantity,
                        price = it.price
                    )
                }
                val total = cartSnapshot.sumOf { it.totalPrice }
                val newSale = Sale(
                    timestamp = System.currentTimeMillis(),
                    totalAmount = total,
                    items = soldItems
                )

                // 1. Perform atomic transaction
                repository.checkoutSale(newSale)

                // 2. Alert if any product fell into low-stock state
                for (cartItem in cartSnapshot) {
                    if (cartItem.productId != null) {
                        val matchingProduct = repository.getProductById(cartItem.productId)
                        if (matchingProduct != null && matchingProduct.isLowStock) {
                            try {
                                NotificationHelper.showLowStockNotification(context, matchingProduct)
                            } catch (nt: Throwable) {
                                nt.printStackTrace()
                            }
                        }
                    }
                }

                // 3. Clear cart
                clearCart()

                // Trigger network database sync automatically
                com.example.sync.SyncManager.triggerDatabaseSync()
                triggerLocalSafetyBackup()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return true
    }

    // Robust searching methods
    fun searchProductsInDb(query: String): Flow<List<Product>> {
        return repository.searchProducts(query, "All", false)
    }

    fun searchTemplateProductsInDb(query: String, category: String = "All"): Flow<List<Product>> {
        return repository.searchTemplateProducts(query, category)
    }

    fun getAllTemplateCategories(): Flow<List<String>> {
        return repository.getAllTemplateCategories()
    }

    fun getProductsWithBarcodes(): Flow<List<Product>> {
        return repository.getProductsWithBarcodes()
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return repository.getProductByBarcode(barcode)
    }

    // Actions for Products
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            if (product.id == 0) {
                repository.insertProduct(product)
                if (product.isLowStock) {
                    NotificationHelper.showLowStockNotification(context, product)
                }
            } else {
                repository.updateProduct(product)
                if (product.isLowStock) {
                    NotificationHelper.showLowStockNotification(context, product)
                }
            }
            com.example.sync.SyncManager.triggerDatabaseSync()
            triggerLocalSafetyBackup()
        }
    }

    fun deleteProduct(product: Product) {
        if (com.example.sync.SyncManager.isConnected.value && !com.example.sync.SyncManager.isServer.value) {
            addSyncLog("Tsy mahazo mamafa entana ny Client (La suppression est réservée au Serveur)")
            return
        }
        viewModelScope.launch {
            repository.deleteProduct(product)
            removeFromCart("product_${product.id}")
            com.example.sync.SyncManager.triggerDatabaseSync()
            triggerLocalSafetyBackup()
        }
    }

    fun adjustStock(product: Product, newStock: Double) {
        viewModelScope.launch {
            val updatedProduct = product.copy(stock = newStock)
            repository.updateProduct(updatedProduct)
            if (updatedProduct.isLowStock) {
                NotificationHelper.showLowStockNotification(context, updatedProduct)
            }
            com.example.sync.SyncManager.triggerDatabaseSync()
            triggerLocalSafetyBackup()
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSale(sale)
            com.example.sync.SyncManager.triggerDatabaseSync()
            triggerLocalSafetyBackup()
        }
    }

    // Debt Management CRUD
    fun saveDebt(debt: Debt) {
        viewModelScope.launch {
            repository.insertDebt(debt)
            com.example.sync.SyncManager.triggerDatabaseSync()
            triggerLocalSafetyBackup()
        }
    }

    fun updateDebtRepayment(debtId: Int, repayAmount: Double) {
        viewModelScope.launch {
            val debts = allDebts.value
            val debt = debts.find { it.id == debtId }
            if (debt != null) {
                val newBalance = (debt.balance - repayAmount).coerceAtLeast(0.0)
                val isPaidNow = newBalance <= 0.0
                val updatedDebt = debt.copy(
                    balance = newBalance,
                    isPaid = isPaidNow
                )
                repository.updateDebt(updatedDebt)
                com.example.sync.SyncManager.triggerDatabaseSync()
                triggerLocalSafetyBackup()
            }
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
            com.example.sync.SyncManager.triggerDatabaseSync()
            triggerLocalSafetyBackup()
        }
    }

    // Fournisseur Management
    val allFournisseurs: StateFlow<List<Fournisseur>> = repository.fournisseurDao.getAllFournisseurs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveFournisseur(fournisseur: Fournisseur) {
        viewModelScope.launch {
            repository.fournisseurDao.insertFournisseur(fournisseur)
        }
    }

    // MULTI-TERMINAL SYNCHRONIZATION BRIDGE METHODS
    val syncLogs = MutableStateFlow<List<String>>(emptyList())

    fun addSyncLog(text: String) {
        val current = syncLogs.value.takeLast(100).toMutableList()
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        current.add("[$timeStr] $text")
        syncLogs.value = current
    }

    fun reserveStockSync(productId: String, quantity: Double): Boolean {
        val id = productId.toIntOrNull() ?: return false
        val product = kotlinx.coroutines.runBlocking { repository.getProductById(id) } ?: return false
        return product.stock >= quantity
    }

    fun commitSaleSync(saleJson: String): Boolean {
        return try {
            val obj = org.json.JSONObject(saleJson)
            val timestamp = obj.getLong("timestamp")
            val totalAmount = obj.getDouble("totalAmount")
            val itemsArr = obj.getJSONArray("items")
            val list = mutableListOf<SoldItem>()
            for (i in 0 until itemsArr.length()) {
                val itemObj = itemsArr.getJSONObject(i)
                list.add(
                    SoldItem(
                        productId = itemObj.getInt("productId"),
                        name = itemObj.getString("name"),
                        quantity = itemObj.getDouble("quantity"),
                        price = itemObj.getDouble("price")
                    )
                )
            }
            val sale = Sale(
                timestamp = timestamp,
                totalAmount = totalAmount,
                items = list
            )
            kotlinx.coroutines.runBlocking {
                repository.checkoutSale(sale)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun updateStockSync(productId: String, newQuantity: Double) {
        val id = productId.toIntOrNull() ?: return
        viewModelScope.launch {
            val product = repository.getProductById(id)
            if (product != null) {
                val updatedProduct = product.copy(stock = newQuantity)
                repository.updateProduct(updatedProduct)
            }
        }
    }

    fun getAllProductsJsonSync(): String {
        val products = allProducts.value
        val arr = org.json.JSONArray()
        val excluded = excludedProductIds.value
        products.forEach {
            if (!excluded.contains(it.id.toString())) {
                val obj = org.json.JSONObject()
                obj.put("id", it.id)
                obj.put("name", it.name)
                obj.put("price", it.price)
                obj.put("category", it.category)
                obj.put("stock", it.stock)
                obj.put("imageUrl", it.imageUrl ?: "")
                obj.put("unit", it.unit)
                obj.put("sku", it.sku ?: "")
                obj.put("lowStockThreshold", it.lowStockThreshold)
                obj.put("prixAchatUniteBase", it.prixAchatUniteBase ?: 0.0)
                obj.put("barcode", it.barcode ?: "")
                obj.put("stock_quantity", it.stock_quantity)
                obj.put("nomCourt", it.nomCourt ?: "")
                obj.put("sousCategorie", it.sousCategorie ?: "")
                obj.put("marque", it.marque ?: "")
                obj.put("description", it.description ?: "")
                obj.put("stockMax", it.stockMax ?: 0.0)
                obj.put("emplacement", it.emplacement ?: "")
                obj.put("fournisseurId", it.fournisseurId ?: -1L)
                obj.put("gerePeremption", it.gerePeremption)
                obj.put("taxable", it.taxable)
                obj.put("tauxTaxe", it.tauxTaxe)
                arr.put(obj)
            }
        }
        return arr.toString()
    }

    fun syncAllProductsSync(stockJson: String) {
        viewModelScope.launch {
            try {
                val arr = org.json.JSONArray(stockJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optInt("id", 0)
                    val name = obj.getString("name")
                    val price = obj.getDouble("price")
                    val category = obj.getString("category")
                    val stock = obj.getDouble("stock")
                    val imageUrl = obj.optString("imageUrl", "")
                    val unit = obj.getString("unit")
                    val sku = obj.optString("sku", "")
                    val threshold = obj.optDouble("lowStockThreshold", 0.0)
                    val basePrice = obj.optDouble("prixAchatUniteBase", 0.0)
                    val barcode = obj.optString("barcode", "")
                    val stockQuantity = obj.optInt("stock_quantity", 0)
                    val nomCourt = if (obj.has("nomCourt") && !obj.isNull("nomCourt")) obj.getString("nomCourt") else null
                    val sousCategorie = if (obj.has("sousCategorie") && !obj.isNull("sousCategorie")) obj.getString("sousCategorie") else null
                    val marque = if (obj.has("marque") && !obj.isNull("marque")) obj.getString("marque") else null
                    val description = if (obj.has("description") && !obj.isNull("description")) obj.getString("description") else null
                    val stockMax = if (obj.has("stockMax") && !obj.isNull("stockMax")) obj.getDouble("stockMax") else null
                    val emplacement = if (obj.has("emplacement") && !obj.isNull("emplacement")) obj.getString("emplacement") else null
                    val fournisseurId = if (obj.has("fournisseurId") && obj.getLong("fournisseurId") != -1L) obj.getLong("fournisseurId") else null
                    val gerePeremption = obj.optBoolean("gerePeremption", false)
                    val taxable = obj.optBoolean("taxable", false)
                    val tauxTaxe = obj.optDouble("tauxTaxe", 0.0)

                    val existing = if (barcode.isNotEmpty()) {
                        repository.getProductByBarcode(barcode)
                    } else if (sku.isNotEmpty()) {
                        allProducts.value.find { it.sku.equals(sku, ignoreCase = true) }
                    } else {
                        null
                    } ?: repository.getProductByName(name)

                    if (existing != null) {
                        val updated = existing.copy(
                            name = name,
                            price = price,
                            category = category,
                            stock = stock,
                            imageUrl = imageUrl,
                            unit = unit,
                            sku = sku,
                            lowStockThreshold = threshold,
                            prixAchatUniteBase = basePrice,
                            barcode = barcode,
                            stock_quantity = stockQuantity,
                            nomCourt = nomCourt,
                            sousCategorie = sousCategorie,
                            marque = marque,
                            description = description,
                            stockMax = stockMax,
                            emplacement = emplacement,
                            fournisseurId = fournisseurId,
                            gerePeremption = gerePeremption,
                            taxable = taxable,
                            tauxTaxe = tauxTaxe
                        )
                        repository.insertProduct(updated)
                    } else {
                        val newProd = Product(
                            id = 0,
                            name = name,
                            price = price,
                            category = category,
                            stock = stock,
                            imageUrl = imageUrl,
                            unit = unit,
                            sku = sku,
                            lowStockThreshold = threshold,
                            prixAchatUniteBase = basePrice,
                            barcode = barcode,
                            stock_quantity = stockQuantity,
                            nomCourt = nomCourt,
                            sousCategorie = sousCategorie,
                            marque = marque,
                            description = description,
                            stockMax = stockMax,
                            emplacement = emplacement,
                            fournisseurId = fournisseurId,
                            gerePeremption = gerePeremption,
                            taxable = taxable,
                            tauxTaxe = tauxTaxe
                        )
                        repository.insertProduct(newProd)
                    }
                }
                addSyncLog("Mise à jour complète du catalogue (${arr.length()} produits)")
            } catch (e: Exception) {
                e.printStackTrace()
                addSyncLog("Erreur de synchronisation du catalogue: ${e.message}")
            }
        }
    }

    fun getFullDatabaseJsonSync(): String {
        return kotlinx.coroutines.runBlocking {
            try {
                val products = repository.allProducts.first()
                val sales = repository.allSales.first()
                val debts = repository.allDebts.first()
                val restocks = repository.allRestocks.first()
                val mouvementsCaisse = repository.allMouvementsCaisse.first()
                val excluded = excludedProductIds.value

                val productsArr = org.json.JSONArray()
                products.forEach { prod ->
                    if (!excluded.contains(prod.id.toString())) {
                        val obj = org.json.JSONObject()
                        obj.put("id", prod.id)
                        obj.put("name", prod.name)
                        obj.put("price", prod.price)
                        obj.put("category", prod.category)
                        obj.put("stock", prod.stock)
                        obj.put("imageUrl", prod.imageUrl ?: "")
                        obj.put("unit", prod.unit)
                        obj.put("barcode", prod.barcode)
                        obj.put("wholesalePrice", prod.wholesalePrice ?: 0.0)
                        obj.put("sku", prod.sku)
                        obj.put("lowStockThreshold", prod.lowStockThreshold)
                        obj.put("prixAchatUniteBase", prod.prixAchatUniteBase)
                        obj.put("isTemplate", prod.isTemplate)
                        obj.put("stock_quantity", prod.stock_quantity)
                        obj.put("nomCourt", prod.nomCourt ?: "")
                        obj.put("sousCategorie", prod.sousCategorie ?: "")
                        obj.put("marque", prod.marque ?: "")
                        obj.put("description", prod.description ?: "")
                        obj.put("stockMax", prod.stockMax ?: 0.0)
                        obj.put("emplacement", prod.emplacement ?: "")
                        obj.put("fournisseurId", prod.fournisseurId ?: -1L)
                        obj.put("gerePeremption", prod.gerePeremption)
                        obj.put("taxable", prod.taxable)
                        obj.put("tauxTaxe", prod.tauxTaxe)
                        productsArr.put(obj)
                    }
                }

                val salesArr = org.json.JSONArray()
                sales.forEach { sale ->
                    val saleObj = org.json.JSONObject()
                    saleObj.put("timestamp", sale.timestamp)
                    saleObj.put("totalAmount", sale.totalAmount)
                    
                    val itemsArr = org.json.JSONArray()
                    sale.items.forEach { item ->
                        val itemObj = org.json.JSONObject()
                        itemObj.put("productId", item.productId)
                        itemObj.put("name", item.name)
                        itemObj.put("quantity", item.quantity)
                        itemObj.put("price", item.price)
                        itemsArr.put(itemObj)
                    }
                    saleObj.put("items", itemsArr)
                    salesArr.put(saleObj)
                }

                val debtsArr = org.json.JSONArray()
                debts.forEach { debt ->
                    val debtObj = org.json.JSONObject()
                    debtObj.put("debtorName", debt.debtorName)
                    debtObj.put("amount", debt.amount)
                    debtObj.put("balance", debt.balance)
                    debtObj.put("date", debt.date)
                    debtObj.put("note", debt.note)
                    debtObj.put("isPaid", debt.isPaid)
                    debtsArr.put(debtObj)
                }

                val restocksArr = org.json.JSONArray()
                restocks.forEach { restock ->
                    val restockObj = org.json.JSONObject()
                    restockObj.put("id", restock.id)
                    restockObj.put("productId", restock.productId)
                    restockObj.put("productName", restock.productName)
                    restockObj.put("cartonsQuantity", restock.cartonsQuantity)
                    restockObj.put("itemsPerCarton", restock.itemsPerCarton)
                    restockObj.put("totalUnits", restock.totalUnits)
                    restockObj.put("totalCostPrice", restock.totalCostPrice)
                    restockObj.put("unitSellingPrice", restock.unitSellingPrice)
                    restockObj.put("supplierId", restock.supplierId ?: -1L)
                    restockObj.put("supplierName", restock.supplierName ?: "")
                    restockObj.put("timestamp", restock.timestamp)
                    restocksArr.put(restockObj)
                }

                val mouvementsCaisseArr = org.json.JSONArray()
                mouvementsCaisse.forEach { mouvement ->
                    val mvtObj = org.json.JSONObject()
                    mvtObj.put("type", mouvement.type)
                    mvtObj.put("montant", mouvement.montant)
                    mvtObj.put("motif", mouvement.motif)
                    mvtObj.put("note", mouvement.note)
                    mvtObj.put("date", mouvement.date)
                    mouvementsCaisseArr.put(mvtObj)
                }

                com.example.sync.SyncSerializer.serializeFullSync(
                    productsArr.toString(),
                    salesArr.toString(),
                    debtsArr.toString(),
                    restocksArr.toString(),
                    mouvementsCaisseArr.toString()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                "{}"
            }
        }
    }

    fun syncFullDatabaseSync(syncJson: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val resultsMap = com.example.sync.SyncSerializer.deserializeFullSync(syncJson)
                val productsStr = resultsMap["products"] ?: "[]"
                val salesStr = resultsMap["sales"] ?: "[]"
                val debtsStr = resultsMap["debts"] ?: "[]"
                val restocksStr = resultsMap["restocks"] ?: "[]"
                val mouvementsCaisseStr = resultsMap["mouvementsCaisse"] ?: "[]"

                // 1. Parse & Merge Products
                val productsList = mutableListOf<Product>()
                val prodArr = org.json.JSONArray(productsStr)
                for (i in 0 until prodArr.length()) {
                    val obj = prodArr.getJSONObject(i)
                    productsList.add(
                        Product(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            price = obj.getDouble("price"),
                            category = obj.getString("category"),
                            stock = obj.getDouble("stock"),
                            imageUrl = obj.optString("imageUrl", ""),
                            unit = obj.optString("unit", "Pièce"),
                            barcode = obj.optString("barcode", ""),
                            wholesalePrice = if (obj.has("wholesalePrice") && !obj.isNull("wholesalePrice")) obj.getDouble("wholesalePrice") else null,
                            sku = obj.optString("sku", ""),
                            lowStockThreshold = obj.optDouble("lowStockThreshold", 5.0),
                            prixAchatUniteBase = obj.optDouble("prixAchatUniteBase", 0.0),
                            isTemplate = obj.optBoolean("isTemplate", false),
                            stock_quantity = obj.optInt("stock_quantity", 0),
                            nomCourt = if (obj.has("nomCourt") && !obj.isNull("nomCourt")) obj.getString("nomCourt") else null,
                            sousCategorie = if (obj.has("sousCategorie") && !obj.isNull("sousCategorie")) obj.getString("sousCategorie") else null,
                            marque = if (obj.has("marque") && !obj.isNull("marque")) obj.getString("marque") else null,
                            description = if (obj.has("description") && !obj.isNull("description")) obj.getString("description") else null,
                            stockMax = if (obj.has("stockMax") && !obj.isNull("stockMax")) obj.getDouble("stockMax") else null,
                            emplacement = if (obj.has("emplacement") && !obj.isNull("emplacement")) obj.getString("emplacement") else null,
                            fournisseurId = if (obj.has("fournisseurId") && obj.getLong("fournisseurId") != -1L) obj.getLong("fournisseurId") else null,
                            gerePeremption = obj.optBoolean("gerePeremption", false),
                            taxable = obj.optBoolean("taxable", false),
                            tauxTaxe = obj.optDouble("tauxTaxe", 0.0)
                        )
                    )
                }

                productsList.forEach { prod ->
                    val existing = if (prod.barcode.isNotEmpty()) {
                        repository.getProductByBarcode(prod.barcode)
                    } else if (prod.sku.isNotEmpty()) {
                        allProducts.value.find { it.sku.equals(prod.sku, ignoreCase = true) }
                    } else {
                        null
                    } ?: repository.getProductByName(prod.name)

                    if (existing == null) {
                        repository.insertProduct(prod.copy(id = 0))
                    } else {
                        val updated = existing.copy(
                            name = prod.name,
                            price = prod.price,
                            category = prod.category,
                            stock = prod.stock,
                            imageUrl = prod.imageUrl,
                            unit = prod.unit,
                            sku = prod.sku,
                            lowStockThreshold = prod.lowStockThreshold,
                            prixAchatUniteBase = prod.prixAchatUniteBase,
                            barcode = prod.barcode,
                            isTemplate = prod.isTemplate,
                            stock_quantity = prod.stock_quantity,
                            nomCourt = prod.nomCourt,
                            sousCategorie = prod.sousCategorie,
                            marque = prod.marque,
                            description = prod.description,
                            stockMax = prod.stockMax,
                            emplacement = prod.emplacement,
                            fournisseurId = prod.fournisseurId,
                            gerePeremption = prod.gerePeremption,
                            taxable = prod.taxable,
                            tauxTaxe = prod.tauxTaxe
                        )
                        repository.insertProduct(updated)
                    }
                }

                // 2. Parse & Merge Sales
                val salesList = mutableListOf<Sale>()
                val saleArr = org.json.JSONArray(salesStr)
                for (i in 0 until saleArr.length()) {
                    val obj = saleArr.getJSONObject(i)
                    val itemsArr = obj.getJSONArray("items")
                    val itemsList = mutableListOf<SoldItem>()
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        itemsList.add(
                            SoldItem(
                                productId = itemObj.getInt("productId"),
                                name = itemObj.getString("name"),
                                quantity = itemObj.getDouble("quantity"),
                                price = itemObj.getDouble("price")
                            )
                        )
                    }
                    salesList.add(
                        Sale(
                            timestamp = obj.getLong("timestamp"),
                            totalAmount = obj.getDouble("totalAmount"),
                            items = itemsList
                        )
                    )
                }

                var newSalesCount = 0
                for (incomingSale in salesList) {
                    val saleExists = allSales.value.any { 
                        it.timestamp == incomingSale.timestamp && Math.abs(it.totalAmount - incomingSale.totalAmount) < 0.01 
                    }
                    if (!saleExists) {
                        val mappedItems = mutableListOf<SoldItem>()
                        for (item in incomingSale.items) {
                            val localProd = repository.getProductByName(item.name)
                            if (localProd != null) {
                                mappedItems.add(item.copy(productId = localProd.id))
                            } else {
                                mappedItems.add(item)
                            }
                        }
                        repository.checkoutSale(incomingSale.copy(id = 0, items = mappedItems), decrementStock = false)
                        newSalesCount++
                    }
                }
                if (newSalesCount > 0) {
                    addSyncLog("Mise à jour ventes: $newSalesCount ventes enregistrées")
                }

                // 3. Parse & Merge Debts
                val debtsList = mutableListOf<Debt>()
                val debtArr = org.json.JSONArray(debtsStr)
                for (i in 0 until debtArr.length()) {
                    val obj = debtArr.getJSONObject(i)
                    debtsList.add(
                        Debt(
                            debtorName = obj.getString("debtorName"),
                            amount = obj.getDouble("amount"),
                            balance = obj.getDouble("balance"),
                            date = obj.getLong("date"),
                            note = obj.optString("note", ""),
                            isPaid = obj.optBoolean("isPaid", false)
                        )
                    )
                }

                var newDebtsCount = 0
                var updatedDebtsCount = 0
                debtsList.forEach { incomingDebt ->
                    val existingDebt = allDebts.value.find {
                        it.debtorName.equals(incomingDebt.debtorName, ignoreCase = true) && 
                        Math.abs(it.date - incomingDebt.date) < 60000
                    }
                    if (existingDebt == null) {
                        repository.insertDebt(incomingDebt.copy(id = 0))
                        newDebtsCount++
                    } else {
                        if (incomingDebt.balance < existingDebt.balance || incomingDebt.isPaid != existingDebt.isPaid) {
                            val updatedDebt = existingDebt.copy(
                                balance = incomingDebt.balance,
                                isPaid = incomingDebt.isPaid,
                                note = incomingDebt.note
                            )
                            repository.updateDebt(updatedDebt)
                            updatedDebtsCount++
                        }
                    }
                }
                if (newDebtsCount > 0 || updatedDebtsCount > 0) {
                    addSyncLog("Mise à jour dettes: $newDebtsCount nouvelles, $updatedDebtsCount modifiées")
                }

                // 4. Parse & Merge Restocks
                val restocksList = mutableListOf<com.example.data.model.Restock>()
                val restockArr = org.json.JSONArray(restocksStr)
                for (i in 0 until restockArr.length()) {
                    val obj = restockArr.getJSONObject(i)
                    restocksList.add(
                        com.example.data.model.Restock(
                            productId = obj.getInt("productId"),
                            productName = obj.getString("productName"),
                            cartonsQuantity = obj.getDouble("cartonsQuantity"),
                            itemsPerCarton = obj.getDouble("itemsPerCarton"),
                            totalUnits = obj.getDouble("totalUnits"),
                            totalCostPrice = obj.getDouble("totalCostPrice"),
                            unitSellingPrice = obj.getDouble("unitSellingPrice"),
                            supplierId = if (obj.has("supplierId") && obj.getLong("supplierId") != -1L) obj.getLong("supplierId") else null,
                            supplierName = if (obj.has("supplierName")) obj.getString("supplierName") else null,
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }

                var newRestocksCount = 0
                restocksList.forEach { incomingRestock ->
                    val restockExists = allRestocks.value.any {
                        it.productId == incomingRestock.productId && 
                        Math.abs(it.timestamp - incomingRestock.timestamp) < 60000 && 
                        Math.abs(it.totalUnits - incomingRestock.totalUnits) < 0.01
                    }
                    if (!restockExists) {
                        repository.insertRestock(incomingRestock.copy(id = 0))
                        newRestocksCount++
                    }
                }
                if (newRestocksCount > 0) {
                    addSyncLog("Mise à jour approvisionnements: $newRestocksCount enregistrés")
                }

                // 5. Parse & Merge Mouvements de caisse
                val mouvementsCaisseList = mutableListOf<MouvementCaisse>()
                val mvtCaisseArr = org.json.JSONArray(mouvementsCaisseStr)
                for (i in 0 until mvtCaisseArr.length()) {
                    val obj = mvtCaisseArr.getJSONObject(i)
                    mouvementsCaisseList.add(
                        MouvementCaisse(
                            type = obj.getString("type"),
                            montant = obj.getDouble("montant"),
                            motif = obj.getString("motif"),
                            note = obj.optString("note", ""),
                            date = obj.getLong("date")
                        )
                    )
                }

                var newMouvementsCaisseCount = 0
                mouvementsCaisseList.forEach { incomingMouvement ->
                    val mouvementExists = allMouvementsCaisse.value.any {
                        it.type == incomingMouvement.type &&
                        Math.abs(it.date - incomingMouvement.date) < 60000 &&
                        Math.abs(it.montant - incomingMouvement.montant) < 0.01
                    }
                    if (!mouvementExists) {
                        repository.insertMouvementCaisse(incomingMouvement.copy(id = 0))
                        newMouvementsCaisseCount++
                    }
                }
                if (newMouvementsCaisseCount > 0) {
                    addSyncLog("Mise à jour mouvements de caisse: $newMouvementsCaisseCount enregistrés")
                }

                addSyncLog("Nahomby ny fampitoviana ny tahiry rehetra!")
                triggerLocalSafetyBackup()
            } catch (e: Exception) {
                e.printStackTrace()
                addSyncLog("Hadisoana fampitoviana: ${e.message}")
            }
        }
    }

    fun triggerLocalSafetyBackup() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val dbJson = getFullDatabaseJsonSync()
                com.example.util.BackupHelper.saveBackup(context, dbJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreLocalSafetyBackup(): Boolean {
        return try {
            val backupJson = com.example.util.BackupHelper.readBackup(context)
            if (backupJson != null) {
                syncFullDatabaseSync(backupJson)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("InventoryViewModel", "InventoryViewModel onCleared - viewModelScope automatically cancelled")
    }
}

class InventoryViewModelFactory(
    private val repository: InventoryRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
