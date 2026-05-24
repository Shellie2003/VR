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
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.InventoryViewModel

@Composable
fun CalculatorScreen(
    viewModel: InventoryViewModel,
    onNavigateToHome: () -> Unit
) {
    val cart by viewModel.cart.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val totalAmount by viewModel.cartTotal.collectAsState(0.0)
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastReceiptAmount by remember { mutableStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // POS Product Selector Board (Quick tap to add)
        Text(
            text = "Fidio ny entana hamidy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Small horizontal filter search
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Fikarohana entana...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                .testTag("pos_search"),
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

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Products horizontal flow
        val availableProducts = filteredProducts.filter { it.stock > 0 }
        if (availableProducts.isEmpty()) {
            Text(
                text = "Tsy misy entana mifanaraka amin'io fikarohana io na lany stock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableProducts) { product ->
                    Card(
                        onClick = { viewModel.addToCart(product, 1) },
                        modifier = Modifier
                            .width(130.dp)
                            .height(68.dp)
                            .testTag("pos_product_pill_${product.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${product.stock} sisa",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Ar ${formatPrice(product.price)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))

        // Cart items section
        Text(
            text = "Entana fividianana",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Empty Calculator",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "Goba! Tsy misy entana voafantina.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onNavigateToHome,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Hijery ny entana")
                    }
                }
            }
        } else {
            // Cart List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cart) { cartItem ->
                    CartItemRow(
                        cartItem = cartItem,
                        onUpdateQuantity = { qty -> viewModel.updateCartQuantity(cartItem.product, qty) },
                        onRemove = { viewModel.removeFromCart(cartItem.product) }
                    )
                }
            }

            // Pay Section
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hataon'ny vidiny:",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ar ${formatPrice(totalAmount)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clear All
                        OutlinedButton(
                            onClick = { viewModel.clearCart() },
                            modifier = Modifier.weight(0.3f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Fafao")
                        }

                        // Checkout
                        Button(
                            onClick = {
                                lastReceiptAmount = totalAmount
                                viewModel.checkoutCart()
                                showSuccessDialog = true
                            },
                            modifier = Modifier
                                .weight(0.7f)
                                .height(48.dp)
                                .testTag("checkout_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingCartCheckout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mandoa vola", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Checkout Confirmation Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Tafiditra ny varotra!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Voarandrana avokoa ny momba ny fividianana mitentina Ar ${formatPrice(lastReceiptAmount)} ary nohavaozina ny tahiry (stock).",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Ekena")
                    }
                }
            }
        )
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val product = cartItem.product

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_row_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ar ${formatPrice(product.price)} / entana",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ar ${formatPrice(cartItem.totalPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Decrement Button
                IconButton(
                    onClick = {
                        if (cartItem.quantity > 1) {
                            onUpdateQuantity(cartItem.quantity - 1)
                        } else {
                            onRemove()
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Latsaka",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quantity Text
                Text(
                    text = cartItem.quantity.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Increment Button
                IconButton(
                    onClick = {
                        if (cartItem.quantity < product.stock) {
                            onUpdateQuantity(cartItem.quantity + 1)
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    enabled = cartItem.quantity < product.stock
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ampiana",
                        modifier = Modifier.size(16.dp),
                        tint = if (cartItem.quantity < product.stock) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete Button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hamafa",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
