package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val totalPrice: Double get() = product.price * quantity
}

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    // Filtering states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    // Database Flows
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined Flow for searched & filtered products
    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        searchQuery,
        selectedCategory
    ) { products, query, cat ->
        products.filter { product ->
            val matchesQuery = product.name.contains(query, ignoreCase = true) || 
                               product.category.contains(query, ignoreCase = true)
            val matchesCategory = cat == "All" || product.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Seeding products on empty state
    init {
        viewModelScope.launch {
            // First delay slightly to wait for database to emit or check
            allProducts.take(1).collect { products ->
                if (products.isEmpty()) {
                    seedSampleProducts()
                }
            }
        }
    }

    private suspend fun seedSampleProducts() {
        val sample1 = Product(
            name = "Vary Gasy",
            price = 3200.0,
            category = "Sakafo",
            stock = 45,
            imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDQZBED1CINHzPYLL5kQlY2SY_8-PHOTnCMqb4uRWGGxuQcD9b8kPugF7c994nloj92WROcFJJ2X4lrYgTHvEGNvg5K9YbJZAthP9lwD5mZ5KhIJDrAFJlgNsz9LMBjw9vVZlEioWrVvwzn_Fg6SF_43QsA57Ot3lyRSJ9DnlTAw-vaWXK-Be2M9dk99fvKPlJBxTDoEFoSo_3oHwEf_sDo1833yFib1ma9PdhdPJ1cWAEaeoGOBwirvCJSShOMM1IcTh8F0_fTMwA"
        )
        val sample2 = Product(
            name = "Karoty vao",
            price = 1500.0,
            category = "Legioma",
            stock = 30,
            imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDzeEEs9icBQHVeo_eEfsfKS8V_Evvj6-Nxt57sdsa-eCmXevhJMY5dgZYhBqapBXwKIxBiKXyjmZQc0APzZWa2eYmH6pfkCGcD1cGPlg2Ib6vUX5CLXQnNWqe-8S-1EnGzsCzi2lL5slhIxKRhVZO2BK4NLPcJHemeyse8y1Of2pEE2AH1EP-8GtvVIQN3-BhFiLvpP2hw5893Gb6oS_EcXhYveMeUuCFyntvvKMPY-KMQou8SycWTw5dRL7OS4aOrGNtFX5OA16Q"
        )
        val sample3 = Product(
            name = "Menaka 1L",
            price = 8500.0,
            category = "Sakafo",
            stock = 15,
            imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDRDmqUF_GZvzg4Abxl71OxsDpQyirgqOHzGALIBBO2ag_p1GPcfQgDrpTrRMp6ARBlIGJAEE_ZBtXLF6BgL3ybWg_9_RwQjhjFfXJWaMKWaLx_ckzxd9o9WUSfygXmp7S0fnIvM5lRaXrUEQcZnOsqTOwkEYLTKYAUjRwNYK9AWPqZs7CHN6La_CmWznPN1GDO7sEahItbidIPEwSeXaFUvywjLW7bWwlplsIRdJi0id46QU8NJoU4evtqfUzETyGY2dRjXlplRn8"
        )
        val sample4 = Product(
            name = "Voankazo Voasary",
            price = 2000.0,
            category = "Voankazo",
            stock = 50,
            imageUrl = ""
        )
        val sample5 = Product(
            name = "Rano Mavitrika 1.5L",
            price = 1800.0,
            category = "Zava-pisotro",
            stock = 25,
            imageUrl = ""
        )
        repository.insertProduct(sample1)
        repository.insertProduct(sample2)
        repository.insertProduct(sample3)
        repository.insertProduct(sample4)
        repository.insertProduct(sample5)
    }

    // Cart Management
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // Calculated fields from Cart
    val cartTotal: Flow<Double> = _cart.map { items ->
        items.sumOf { it.totalPrice }
    }

    fun addToCart(product: Product, quantity: Int) {
        val current = _cart.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            val updatedQty = current[existingIndex].quantity + quantity
            val cappedQty = updatedQty.coerceAtMost(product.stock)
            if (cappedQty > 0) {
                current[existingIndex] = CartItem(product, cappedQty)
            }
        } else {
            val cappedQty = quantity.coerceAtMost(product.stock)
            if (cappedQty > 0) {
                current.add(CartItem(product, cappedQty))
            }
        }
        _cart.value = current
    }

    fun updateCartQuantity(product: Product, quantity: Int) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val capped = quantity.coerceIn(1, product.stock)
            current[index] = CartItem(product, capped)
            _cart.value = current
        }
    }

    fun removeFromCart(product: Product) {
        _cart.value = _cart.value.filter { it.product.id != product.id }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // Checkout (creates sale record & decrements inventory stocks)
    fun checkoutCart() {
        val cartSnapshot = _cart.value
        if (cartSnapshot.isEmpty()) return

        viewModelScope.launch {
            val soldItems = cartSnapshot.map {
                SoldItem(
                    productId = it.product.id,
                    name = it.product.name,
                    quantity = it.quantity,
                    price = it.product.price
                )
            }
            val total = cartSnapshot.sumOf { it.totalPrice }
            val newSale = Sale(
                timestamp = System.currentTimeMillis(),
                totalAmount = total,
                items = soldItems
            )

            // 1. Insert sale record
            repository.insertSale(newSale)

            // 2. Decrement product stock helper
            for (cartItem in cartSnapshot) {
                val currentStock = cartItem.product.stock
                val updatedStock = (currentStock - cartItem.quantity).coerceAtLeast(0)
                repository.updateProduct(cartItem.product.copy(stock = updatedStock))
            }

            // 3. Clear cart
            clearCart()
        }
    }

    // Actions for Products
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            if (product.id == 0) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            removeFromCart(product)
        }
    }

    fun adjustStock(product: Product, newStock: Int) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(stock = newStock))
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSale(sale)
        }
    }
}

class InventoryViewModelFactory(private val repository: InventoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
