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

    val appPreferences = AppPreferences(context)

    // Observable Preference States
    val language = MutableStateFlow(appPreferences.language)
    val isActivated = MutableStateFlow(appPreferences.isActivated)
    val installationId = appPreferences.installationId

    val groceryName = MutableStateFlow(appPreferences.groceryName)
    val colorTheme = MutableStateFlow(appPreferences.colorTheme)
    val themeMode = MutableStateFlow(appPreferences.themeMode)
    val shopMode = MutableStateFlow(appPreferences.shopMode)

    val themeColor: StateFlow<androidx.compose.ui.graphics.Color> = colorTheme.map {
        when (it) {
            "sunset" -> androidx.compose.ui.graphics.Color(0xFFE65100)
            "indigo" -> androidx.compose.ui.graphics.Color(0xFF1E3A8A)
            "rose" -> androidx.compose.ui.graphics.Color(0xFF881337)
            else -> androidx.compose.ui.graphics.Color(0xFF13503C) // "emerald" / default
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), androidx.compose.ui.graphics.Color(0xFF13503C))

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

    val categories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined Flow for searched & filtered products using database-level search
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredProducts: StateFlow<List<Product>> = combine(
        searchQuery,
        selectedCategory,
        showLowStockOnly
    ) { query, cat, lowStockOnly ->
        Triple(query, cat, lowStockOnly)
    }.flatMapLatest { (query, cat, lowStockOnly) ->
        repository.searchProducts(query, cat, lowStockOnly)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Seeding products on empty state
    init {
        viewModelScope.launch {
            repository.hasProducts().take(1).collect { hasProducts ->
                if (!hasProducts) {
                    seedSampleProducts()
                }
            }
        }
        viewModelScope.launch {
            if (!appPreferences.hasSeededNewCategories) {
                seedNewCategoriesAndProducts()
                appPreferences.hasSeededNewCategories = true
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
            repository.insertProduct(product)
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
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            removeFromCart("product_${product.id}")
        }
    }

    fun adjustStock(product: Product, newStock: Double) {
        viewModelScope.launch {
            val updatedProduct = product.copy(stock = newStock)
            repository.updateProduct(updatedProduct)
            if (updatedProduct.isLowStock) {
                NotificationHelper.showLowStockNotification(context, updatedProduct)
            }
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSale(sale)
        }
    }

    // Debt Management CRUD
    fun saveDebt(debt: Debt) {
        viewModelScope.launch {
            repository.insertDebt(debt)
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
            }
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
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
