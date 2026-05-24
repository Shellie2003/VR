package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.ui.viewmodel.InventoryViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: InventoryViewModel,
    onNavigateToList: () -> Unit,
    onNavigateToCalculator: (Product) -> Unit,
    onEditProduct: (Product) -> Unit
) {
    val products by viewModel.filteredProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val sales by viewModel.allSales.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Calculate details
    val totalProducts = allProducts.sumOf { it.stock }
    val categoriesCount = allProducts.map { it.category.lowercase().trim() }.distinct().size

    // Last 4 added products
    val recentlyAdded = allProducts.take(4)

    // Dialog for product detail quick action
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Tadiavo ny vokatra...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon",
                    tint = MaterialTheme.colorScheme.outline
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("search_input"),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Bento Grid Stats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Products Card (Geometric Balance)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(128.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(16.dp)
                            .testTag("stats_products_card")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(com.example.ui.theme.VarotraPrimaryFixedDim.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory,
                                        contentDescription = "Products",
                                        tint = com.example.ui.theme.VarotraPrimaryFixed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "ENTANA",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = com.example.ui.theme.VarotraPrimaryFixed,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = totalProducts.toString(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    // Categories Card (Geometric Balance)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(128.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(16.dp)
                            .testTag("stats_categories_card")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = "Categories",
                                        tint = Color(0xFF2A1700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "KARAZANA",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF684000),
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = categoriesCount.toString(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color(0xFF2A1700),
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            // Headings and Recent items list
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vokatra farany nampidirina",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Izy rehetra",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onNavigateToList() }
                            .padding(4.dp)
                    )
                }
            }

            if (recentlyAdded.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Empty",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tsy misy vokatra voatahiry",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentlyAdded) { product ->
                    ProductRowCard(
                        product = product,
                        onClick = { selectedProductForDetail = product }
                    )
                }
            }
        }
    }

    // Detail Quick Action Dialog
    selectedProductForDetail?.let { product ->
        ProductDetailDialog(
            product = product,
            onDismiss = { selectedProductForDetail = null },
            onAddToCart = { qty ->
                viewModel.addToCart(product, qty)
                selectedProductForDetail = null
            },
            onDelete = {
                viewModel.deleteProduct(product)
                selectedProductForDetail = null
            },
            onEdit = {
                onEditProduct(product)
                selectedProductForDetail = null
            }
        )
    }
}

@Composable
fun ProductRowCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image
            ProductImage(product = product, modifier = Modifier.size(56.dp))

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                
                // Unified metadata
                val inStockText = if (product.stock > 0) "Misy tahiry" else "Lany tahiry"
                Text(
                    text = "${product.category} • $inStockText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price unit
            Text(
                text = "Ar ${formatPrice(product.price)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ProductImage(
    product: Product,
    modifier: Modifier = Modifier
) {
    if (product.imageUrl.isNotEmpty()) {
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.name,
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
    } else {
        // Aesthetic category based fallback
        val icon = when (product.category.lowercase().trim()) {
            "sakafo" -> Icons.Default.Restaurant
            "legioma", "voankazo" -> Icons.Default.Park
            "zava-pisotro" -> Icons.Default.LocalHospital // fallback for drinks
            else -> Icons.Default.ShoppingBasket
        }
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailDialog(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCart: (Int) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var quantity by remember { mutableStateOf(1) }
    val maxStock = product.stock

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Info Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProductImage(product = product, modifier = Modifier.size(100.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Karazana: ${product.category}", style = MaterialTheme.typography.bodyMedium)
                        Text("Tahiry (Stock): ${product.stock} sisa", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Ar ${formatPrice(product.price)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (maxStock > 0) {
                    Text("Hividy firy?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Latsaka")
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(
                            onClick = { if (quantity < maxStock) quantity++ },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Ampiana")
                        }
                    }
                } else {
                    Text(
                        text = "Tsy misy tahiry azo amidy intsony!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit and Delete small buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Hanova", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hamafa", tint = MaterialTheme.colorScheme.error)
                    }
                }

                // Add to Cart
                Button(
                    onClick = { onAddToCart(quantity) },
                    enabled = maxStock > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hividy")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hakatona", color = MaterialTheme.colorScheme.outline)
            }
        }
    )
}

fun formatPrice(amount: Double): String {
    return try {
        val formatter = NumberFormat.getNumberInstance(Locale.FRANCE) // spaces for thousands separator
        formatter.format(amount)
    } catch (e: Exception) {
        String.format(Locale.getDefault(), "%.0f", amount)
    }
}
