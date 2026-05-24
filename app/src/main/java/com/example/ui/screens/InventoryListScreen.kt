package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    viewModel: InventoryViewModel,
    onEditProduct: (Product) -> Unit
) {
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Determine category list to display as Chips
    val categoriesList = listOf("All") + allProducts.map { it.category }.distinct().sorted()

    // Delete Confirmation dialog state
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search input
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Fikarohana amin'ny anarana...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventory_search_box"),
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

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category filtering Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(categoriesList) { category ->
                val isSelected = category.lowercase() == selectedCategory.lowercase()
                val label = if (category == "All") "rehetra" else category

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedCategory.value = category },
                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("category_chip_$category"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Product Catalog Count header
        Text(
            text = "${filteredProducts.size} entana mifanaraka",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tsy misy vokatra hita.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Main Product LazyColumn
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp), // avoids nav block
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProducts) { product ->
                    InventoryItemRow(
                        product = product,
                        onEdit = { onEditProduct(product) },
                        onDelete = { productToDelete = product },
                        onIncrementStock = { viewModel.adjustStock(product, product.stock + 1) },
                        onDecrementStock = {
                            if (product.stock > 0) {
                                viewModel.adjustStock(product, product.stock - 1)
                            }
                        }
                    )
                }
            }
        }
    }

    // Accidental deletion check Dialog
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hamafa inona ihany?") },
            text = { Text("Tena fafanao tokoa ve ny entana '${product.name}' amin'ny lisitrao?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eny, fafao")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Hanafo")
                }
            }
        )
    }
}

@Composable
fun InventoryItemRow(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onIncrementStock: () -> Unit,
    onDecrementStock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_row_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Name, price & options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ar ${formatPrice(product.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Hanova", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Hamafa", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer / Quick stock controllers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Inline stock status
                val isAvailable = product.stock > 0
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAvailable) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.error
                            )
                    )
                    Text(
                        text = if (isAvailable) "Misy ${product.stock} ao amin'ny tahiry" else "Lany tahiry avokoa!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                }

                // Plus / minus controllers
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Decrease stock
                    IconButton(
                        onClick = onDecrementStock,
                        enabled = product.stock > 0,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Hampihena tahiry",
                            modifier = Modifier.size(14.dp),
                            tint = if (product.stock > 0) MaterialTheme.colorScheme.onSurface else Color.LightGray
                        )
                    }

                    // Stock figure display
                    Text(
                        text = product.stock.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Increase stock
                    IconButton(
                        onClick = onIncrementStock,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Hampitombo tahiry",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
