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
