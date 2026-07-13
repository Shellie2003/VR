package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.data.model.Debt
import com.example.ui.components.BarcodeScannerView
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.FormatUtil
import com.example.util.LanguageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    viewModel: InventoryViewModel,
    onNavigateToHome: () -> Unit,
    initialSubTab: String = "checkout"
) {
    val context = LocalContext.current
    val cart by viewModel.cart.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val totalAmount by viewModel.cartTotal.collectAsState(0.0)
    val activeLang by viewModel.language.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // UI Translation helper
    val t = { key: String -> LanguageManager.translate(key, activeLang) }

    // Screen SubTab state: "checkout" or "quick_misc"
    var subTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }

    // State for Cash Received (Espèces reçues)
    var amountReceivedStr by remember { mutableStateOf("") }
    val amountReceived = amountReceivedStr.toDoubleOrNull() ?: 0.0

    // Search query for product picker inside checkout
    var pickerSearchQuery by remember { mutableStateOf("") }

    // Multiplier states for original Calculator (Calcul Rapide / quick_misc)
    var miscUnitPriceStr by remember { mutableStateOf("") }
    var miscQtyStr by remember { mutableStateOf("1") }

    val miscPrice = miscUnitPriceStr.toDoubleOrNull() ?: 0.0
    val miscQty = miscQtyStr.toDoubleOrNull() ?: 1.0
    val miscProductTotal = miscPrice * miscQty

    // Change / Missing calculations
    val changeToReturn = amountReceived - totalAmount
    val amountMissing = totalAmount - amountReceived

    var showClearConfirm by remember { mutableStateOf(false) }
    var showAddedMiscDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showRealCameraScanner by remember { mutableStateOf(false) }
    var showTrosaDialog by remember { mutableStateOf(false) }
    var trosaDebtorName by remember { mutableStateOf("") }
    val isTrosaMode = amountReceivedStr.trim().isEmpty()

    // New state variables for QuickMiscPage sub-tabs and multiplier inputs
    var quickMiscSubTab by remember { mutableStateOf(0) }
    var calcPriceStr by remember { mutableStateOf("0") }
    var calcQtyStr by remember { mutableStateOf("0") }

    val resetAllFields = {
        amountReceivedStr = ""
        pickerSearchQuery = ""
        trosaDebtorName = ""
        miscUnitPriceStr = ""
        miscQtyStr = "1"
        calcPriceStr = "0"
        calcQtyStr = "0"
    }

    val calcPrice = calcPriceStr.toDoubleOrNull() ?: 0.0
    val calcQty = calcQtyStr.toDoubleOrNull() ?: 0.0
    val calcTotal = calcPrice * calcQty

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet && subTab == "checkout") {
        // ==========================================
            // TABLET SPLIT-SCREEN POS REGISTER
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT PANE: Product Picker Section (Catalog)
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("select_product"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = pickerSearchQuery,
                            onValueChange = { pickerSearchQuery = it },
                            placeholder = { Text(t("search_placeholder"), fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                IconButton(onClick = { showRealCameraScanner = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Scan Barcode",
                                        tint = themeColor
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredProducts by remember(pickerSearchQuery) {
                        viewModel.searchProductsInDb(pickerSearchQuery)
                    }.collectAsState(initial = emptyList())

                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t("no_products_found"),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(filteredProducts) { product ->
                                CashRegisterProductCard(
                                    product = product,
                                    onAddToCart = { prod, qty -> viewModel.addToCart(prod, qty) },
                                    themeColor = themeColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(175.dp)
                                )
                            }
                        }
                    }
                }

                // RIGHT PANE: Cart & Billing Details Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Misc Title Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val quickMiscTitle = when (activeLang) {
                            "mg" -> "Vola Mivantana (Quick Misc)"
                            "fr" -> "Divers rapide"
                            else -> "Quick Miscellaneous"
                        }
                        Text(
                            text = quickMiscTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Scrollable Quick Misc Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        val volaPresets = listOf(200, 300, 500, 1000, 2000, 5000)
                        items(volaPresets) { vola ->
                            SuggestionChip(
                                onClick = {
                                    viewModel.addMiscToCart(t("misc_item"), vola.toDouble(), 1.0)
                                    val addedMsg = when (activeLang) {
                                        "mg" -> "Nampiana Entana Hafa: +${vola} Ar"
                                        "fr" -> "Divers ajouté : +Ar ${vola}"
                                        else -> "Added misc: +Ar ${vola}"
                                    }
                                    Toast.makeText(context, addedMsg, Toast.LENGTH_SHORT).show()
                                },
                                label = {
                                    Text(
                                        text = "+$vola Ar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = themeColor.copy(alpha = 0.12f),
                                    labelColor = themeColor
                                ),
                                border = null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    // Cart item list
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (cart.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = t("cart_empty"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(cart) { item ->
                                    val maxStock = item.maxStock
                                    val step = if (item.unit.lowercase().contains("litre") || 
                                                   item.unit.lowercase().contains("kilo") || 
                                                   item.unit == "L" || 
                                                   item.unit == "kg") 0.25 else 1.0

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${FormatUtil.formatPrice(item.price)} Ar / ${item.unit}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    fontSize = 10.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    val qtyStr = if (item.quantity % 1.0 == 0.0) {
                                                        item.quantity.toInt().toString()
                                                    } else {
                                                        "%.2f".format(item.quantity)
                                                    }
                                                    Text(
                                                        text = "x$qtyStr ${item.unit}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.changeCartQuantityByDelta(item.id, -step)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.RemoveCircleOutline,
                                                    contentDescription = "Remove",
                                                    tint = themeColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            IconButton(
                                                onClick = {
                                                    if (item.quantity < maxStock) {
                                                        viewModel.changeCartQuantityByDelta(item.id, step)
                                                    }
                                                },
                                                enabled = item.quantity < maxStock,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddCircleOutline,
                                                    contentDescription = "Add",
                                                    tint = if (item.quantity < maxStock) themeColor else MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = "${FormatUtil.formatPrice(item.price * item.quantity)} Ar",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = themeColor,
                                                fontSize = 13.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }

                    // Billing dashboard at the bottom of Right pane
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = t("cash_given"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val notes = listOf(1000, 2000, 5000, 10000, 20000)
                                    items(notes) { note ->
                                        OutlinedButton(
                                            onClick = {
                                                val cur = amountReceivedStr.toDoubleOrNull() ?: 0.0
                                                amountReceivedStr = (cur + note).toInt().toString()
                                            },
                                            modifier = Modifier.height(24.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            val formattedLabel = if (note >= 1000) "${note / 1000}k" else "$note"
                                            Text(
                                                text = "$formattedLabel Ar",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { amountReceivedStr = "" },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Clear",
                                        tint = Color.Red,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = amountReceivedStr,
                                    onValueChange = { amountReceivedStr = it },
                                    placeholder = { Text("${t("cash_given")} (Ar)", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                )

                                if (amountReceived > 0.0) {
                                    val isSufficient = changeToReturn >= 0.0
                                    val boxColor = if (isSufficient) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    val boxBgColor = boxColor.copy(alpha = 0.1f)

                                    Box(
                                        modifier = Modifier
                                            .weight(1.7f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(boxBgColor)
                                            .border(1.dp, boxColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSufficient) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = boxColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            val statusText = if (isSufficient) {
                                                when (activeLang) {
                                                    "mg" -> "Famerenana: Ar ${FormatUtil.formatPrice(changeToReturn)}"
                                                    "fr" -> "Rendu: Ar ${FormatUtil.formatPrice(changeToReturn)}"
                                                    else -> "Change: Ar ${FormatUtil.formatPrice(changeToReturn)}"
                                                }
                                            } else {
                                                when (activeLang) {
                                                    "mg" -> "Tsy ampy Ar ${FormatUtil.formatPrice(-changeToReturn)}"
                                                    "fr" -> "Manque Ar ${FormatUtil.formatPrice(-changeToReturn)}"
                                                    else -> "Short Ar ${FormatUtil.formatPrice(-changeToReturn)}"
                                                }
                                            }
                                            Text(
                                                text = statusText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = boxColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.1f)) {
                                    val labelText = when (activeLang) {
                                        "mg" -> "Totalin'ny fandoavana:"
                                        "fr" -> "Total à payer :"
                                        else -> "Total to pay:"
                                    }
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text(
                                            text = "${FormatUtil.formatPrice(totalAmount)} Ar",
                                            color = themeColor,
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (totalAmount > 0.0) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    amountReceivedStr = totalAmount.toInt().toString()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowDownward,
                                                    contentDescription = "Set total as cash received",
                                                    tint = themeColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (cart.isNotEmpty()) {
                                    IconButton(
                                        onClick = { showClearConfirm = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "Clear Cart",
                                            tint = Color.Red,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                ElevatedButton(
                                    onClick = {
                                        if (isTrosaMode) {
                                            trosaDebtorName = ""
                                            showTrosaDialog = true
                                        } else if (amountReceived > 0.0 && amountReceived < totalAmount) {
                                            val errStr = when (activeLang) {
                                                "mg" -> "Tsy ampy ny vola nomen'ny mpanjifa! Tsy ampy Ar ${FormatUtil.formatPrice(amountMissing)}"
                                                "fr" -> "Montant insuffisant ! Il manque Ar ${FormatUtil.formatPrice(amountMissing)}"
                                                else -> "Insufficient cash received! Short Ar ${FormatUtil.formatPrice(amountMissing)}"
                                            }
                                            Toast.makeText(context, errStr, Toast.LENGTH_LONG).show()
                                        } else {
                                            val success = viewModel.checkoutCart()
                                            if (success) {
                                                resetAllFields()
                                                Toast.makeText(context, t("checkout_success"), Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, t("checkout_stock_error"), Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    enabled = cart.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = if (isTrosaMode) Color.Red else themeColor,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .weight(1.8f)
                                        .height(44.dp)
                                        .testTag("calculator_checkout_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircleOutline,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val submitLabel = if (isTrosaMode) "Trosa" else when (activeLang) {
                                            "mg" -> "Hamarina"
                                            "fr" -> "Valider"
                                            else -> "Checkout"
                                        }
                                        Text(
                                            text = submitLabel,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (subTab == "checkout") {
            // ==========================================
            // PRIMARY POS CASH REGISTER / CHECKOUT TAB
            // ==========================================

            // 1. Quick Misc Header and Action Chips row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickMiscTitle = when (activeLang) {
                        "mg" -> "Vola Mivantana (Quick Misc)"
                        "fr" -> "Divers rapide"
                        else -> "Quick Miscellaneous"
                    }
                    Text(
                        text = quickMiscTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Quick Misc Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    val volaPresets = listOf(200, 300, 500, 1000, 2000, 5000)
                    items(volaPresets) { vola ->
                        SuggestionChip(
                            onClick = {
                                viewModel.addMiscToCart(t("misc_item"), vola.toDouble(), 1.0)
                                val addedMsg = when (activeLang) {
                                    "mg" -> "Nampiana Entana Hafa: +${vola} Ar"
                                    "fr" -> "Divers ajouté : +Ar ${vola}"
                                    else -> "Added misc: +Ar ${vola}"
                                }
                                Toast.makeText(context, addedMsg, Toast.LENGTH_SHORT).show()
                            },
                            label = {
                                Text(
                                    text = "+$vola Ar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

            // 2. Main Cart Item Section (Flexible Middle Part)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (cart.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = t("cart_empty"),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(cart) { item ->
                            val maxStock = item.maxStock
                            val step = if (item.unit.lowercase().contains("litre") || 
                                           item.unit.lowercase().contains("kilo") || 
                                           item.unit == "L" || 
                                           item.unit == "kg") 0.25 else 1.0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${FormatUtil.formatPrice(item.price)} Ar / ${item.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            val qtyStr = if (item.quantity % 1.0 == 0.0) {
                                                item.quantity.toInt().toString()
                                            } else {
                                                "%.2f".format(item.quantity)
                                            }
                                            Text(
                                                text = "x$qtyStr ${item.unit}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }

                                // Right controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = {
                                            viewModel.changeCartQuantityByDelta(item.id, -step)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveCircleOutline,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            if (item.quantity < maxStock) {
                                                viewModel.changeCartQuantityByDelta(item.id, step)
                                            }
                                        },
                                        enabled = item.quantity < maxStock,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = "Add",
                                            tint = if (item.quantity < maxStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = "${FormatUtil.formatPrice(item.price * item.quantity)} Ar",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.5.dp, modifier = Modifier.padding(vertical = 4.dp))

            // 3. Product Picker Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t("select_product"),
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = pickerSearchQuery,
                        onValueChange = { pickerSearchQuery = it },
                        placeholder = { Text(t("search_placeholder"), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { showRealCameraScanner = true }) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Scan Barcode",
                                    tint = themeColor
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable Horizontal Product List
                val filteredProducts by remember(pickerSearchQuery) {
                    viewModel.searchProductsInDb(pickerSearchQuery)
                }.collectAsState(initial = emptyList())

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                ) {
                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t("no_products_found"),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 4.dp, top = 4.dp)
                        ) {
                            items(filteredProducts) { product ->
                                CashRegisterProductCard(
                                    product = product,
                                    onAddToCart = { prod, qty -> viewModel.addToCart(prod, qty) },
                                    themeColor = themeColor,
                                    modifier = Modifier
                                        .width(155.dp)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Billing card / billing details section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Line 1: Banknote addition row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("cash_given"),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val notes = listOf(1000, 2000, 5000, 10000, 20000)
                            items(notes) { note ->
                                OutlinedButton(
                                    onClick = {
                                        val cur = amountReceivedStr.toDoubleOrNull() ?: 0.0
                                        amountReceivedStr = (cur + note).toInt().toString()
                                    },
                                    modifier = Modifier.height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    val formattedLabel = if (note >= 1000) "${note / 1000}k" else "$note"
                                    Text(
                                        text = "$formattedLabel Ar",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { amountReceivedStr = "" },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Clear",
                                tint = Color.Red,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Line 2: Cash Input text field and Change display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountReceivedStr,
                            onValueChange = { amountReceivedStr = it },
                            placeholder = { Text("${t("cash_given")} (Ar)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )

                        if (amountReceived > 0.0) {
                            val isSufficient = changeToReturn >= 0.0
                            val boxColor = if (isSufficient) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            val boxBgColor = boxColor.copy(alpha = 0.1f)

                            Box(
                                modifier = Modifier
                                    .weight(1.7f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(boxBgColor)
                                    .border(1.dp, boxColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSufficient) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = boxColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    val statusText = if (isSufficient) {
                                        when (activeLang) {
                                            "mg" -> "Famerenana: Ar ${FormatUtil.formatPrice(changeToReturn)}"
                                            "fr" -> "Rendu: Ar ${FormatUtil.formatPrice(changeToReturn)}"
                                            else -> "Change: Ar ${FormatUtil.formatPrice(changeToReturn)}"
                                        }
                                    } else {
                                        when (activeLang) {
                                            "mg" -> "Tsy ampy Ar ${FormatUtil.formatPrice(-changeToReturn)}"
                                            "fr" -> "Manque Ar ${FormatUtil.formatPrice(-changeToReturn)}"
                                            else -> "Short Ar ${FormatUtil.formatPrice(-changeToReturn)}"
                                        }
                                    }
                                    Text(
                                        text = statusText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = boxColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Line 3: Billing grand total and main checkout buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val labelText = when (activeLang) {
                                "mg" -> "Totalin'ny fandoavana:"
                                "fr" -> "Total à payer :"
                                else -> "Total to pay:"
                            }
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(
                                    text = "${FormatUtil.formatPrice(totalAmount)} Ar",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (totalAmount > 0.0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            amountReceivedStr = totalAmount.toInt().toString()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowDownward,
                                            contentDescription = "Set total as cash received",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (cart.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearConfirm = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Cart",
                                    tint = Color.Red,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        ElevatedButton(
                            onClick = {
                                if (isTrosaMode) {
                                    trosaDebtorName = ""
                                    showTrosaDialog = true
                                } else if (amountReceived > 0.0 && amountReceived < totalAmount) {
                                    val errStr = when (activeLang) {
                                        "mg" -> "Tsy ampy ny vola nomen'ny mpanjifa! Tsy ampy Ar ${FormatUtil.formatPrice(amountMissing)}"
                                        "fr" -> "Montant insuffisant ! Il manque Ar ${FormatUtil.formatPrice(amountMissing)}"
                                        else -> "Insufficient cash received! Short Ar ${FormatUtil.formatPrice(amountMissing)}"
                                    }
                                    Toast.makeText(context, errStr, Toast.LENGTH_LONG).show()
                                } else {
                                    val success = viewModel.checkoutCart()
                                    if (success) {
                                        resetAllFields()
                                        Toast.makeText(context, t("checkout_success"), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, t("checkout_stock_error"), Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = cart.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (isTrosaMode) Color.Red else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (isTrosaMode) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier
                                .weight(2f)
                                .height(40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val submitLabel = if (isTrosaMode) "Trosa" else when (activeLang) {
                                    "mg" -> "Hamarina"
                                    "fr" -> "Valider"
                                    else -> "Checkout"
                                }
                                Text(
                                    text = submitLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ========================================================
            // COMPREHENSIVE POS QUICK MISCELLANEOUS TAB (QuickMiscPage)
            // ========================================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                // 1. App Bar / Title Block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { subTab = "checkout" },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        val titleText = when (activeLang) {
                            "mg" -> "Kajy haingana"
                            "fr" -> "Divers rapide"
                            else -> "Quick Misc"
                        }
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val miscCartItems = cart.filter { it.id.startsWith("misc_") }
                        if (miscCartItems.isNotEmpty()) {
                            IconButton(
                                onClick = { showAddedMiscDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = Color.Red,
                                            contentColor = Color.White
                                        ) {
                                            Text(text = miscCartItems.size.toString(), fontSize = 9.sp)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Show added items",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (cart.isNotEmpty()) {
                            val clearLabel = when (activeLang) {
                                "mg" -> "Hamafa"
                                "fr" -> "Vider"
                                else -> "Clear"
                            }
                            TextButton(
                                onClick = { showClearConfirm = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = clearLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // 2. High-Contrast Premium Billing Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // Grand Total & Change display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val totalLabel = when (activeLang) {
                                    "mg" -> "Totaly"
                                    "fr" -> "Total à payer"
                                    else -> "Total"
                                }
                                Text(
                                    text = totalLabel,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${FormatUtil.formatPrice(totalAmount)} Ar",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    if (cart.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                viewModel.undoLastCartItem()
                                                val undoMsg = when (activeLang) {
                                                    "mg" -> "Nofafana ny entana farany"
                                                    "fr" -> "Dernier élément annulé"
                                                    else -> "Last item undone"
                                                }
                                                Toast.makeText(context, undoMsg, Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Undo,
                                                contentDescription = "Undo",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (amountReceived > 0.0) {
                                val isSufficient = changeToReturn >= 0.0
                                val boxColor = if (isSufficient) Color(0xFF81C784) else Color(0xFFE57373)
                                val boxBgColor = Color.White.copy(alpha = 0.15f)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(boxBgColor)
                                        .border(1.5.dp, boxColor, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        val statusText = if (isSufficient) {
                                            when (activeLang) {
                                                "mg" -> "FAMERENANA SISA"
                                                "fr" -> "À RENDRE"
                                                else -> "CHANGE"
                                            }
                                        } else {
                                            when (activeLang) {
                                                "mg" -> "TSY AMPY"
                                                "fr" -> "INSUFFISANT"
                                                else -> "SHORT"
                                            }
                                        }
                                        Text(
                                            text = statusText,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = boxColor
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val displayChange = if (changeToReturn >= 0) changeToReturn else -changeToReturn
                                        Text(
                                            text = "${FormatUtil.formatPrice(displayChange)} Ar",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Cash input field with Backspace icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = amountReceivedStr,
                                onValueChange = { amountReceivedStr = it },
                                placeholder = {
                                    Text(
                                        text = "${t("cash_given")} (Ar)",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 13.sp
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { amountReceivedStr = "" },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Clear",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Scrollable Banknotespresets
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val notes = listOf(1000, 2000, 5000, 10000, 20000)
                            items(notes) { note ->
                                Button(
                                    onClick = {
                                        val cur = amountReceivedStr.toDoubleOrNull() ?: 0.0
                                        amountReceivedStr = (cur + note).toInt().toString()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = null,
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    val formattedLabel = if (note >= 1000) "${note / 1000}k" else "$note"
                                    Text(
                                        text = "$formattedLabel Ar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Sub-Tab Row selectors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickAmountLabel = when (activeLang) {
                        "mg" -> "Vola haingana"
                        "fr" -> "Montant rapide"
                        else -> "Quick Amount"
                    }
                    val multiplierLabel = when (activeLang) {
                        "mg" -> "Kajy fampitomboana"
                        "fr" -> "Calculateur Multiplicateur"
                        else -> "Multiplier Calculator"
                    }

                    // Tab 0 button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { quickMiscSubTab = 0 }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = quickAmountLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (quickMiscSubTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(3.dp)
                                .background(if (quickMiscSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                        )
                    }

                    // Tab 1 button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { quickMiscSubTab = 1 }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = multiplierLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (quickMiscSubTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(3.dp)
                                .background(if (quickMiscSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                        )
                    }
                }

                // 4. Tab Contents Viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (quickMiscSubTab == 0) {
                        // Predefined grid view
                        val pricePresets = listOf(
                            50.0, 100.0, 200.0, 300.0, 400.0, 500.0,
                            600.0, 700.0, 800.0, 900.0, 1000.0, 1500.0,
                            2000.0, 3000.0, 4000.0, 5000.0, 10000.0, 15000.0, 20000.0
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(pricePresets.size) { index ->
                                val price = pricePresets[index]
                                val isEvenIndex = index % 2 == 0
                                val bgColor = if (isEvenIndex) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                }
                                val fgColor = if (isEvenIndex) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }

                                Card(
                                    onClick = {
                                        viewModel.addMiscToCart(t("misc_item"), price, 1.0)
                                        val badgeMsg = when (activeLang) {
                                            "mg" -> "Tafiditra an-karona: +${price.toInt()} Ar"
                                            "fr" -> "Ajouté au panier : +Ar ${price.toInt()}"
                                            else -> "Added to cart: +Ar ${price.toInt()}"
                                        }
                                        Toast.makeText(context, badgeMsg, Toast.LENGTH_SHORT).show()
                                    },
                                    colors = CardDefaults.cardColors(containerColor = bgColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${FormatUtil.formatPrice(price)} Ar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = fgColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Multiplier Calculator layout
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        // Reset bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    calcPriceStr = "0"
                                                    calcQtyStr = "0"
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Reset",
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Left/Right inputs split row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Unit Price Column
                                            Column(modifier = Modifier.weight(1f)) {
                                                val priceLabel = when (activeLang) {
                                                    "mg" -> "Vidiny (Price)"
                                                    "fr" -> "Prix unitaire"
                                                    else -> "Unit Price"
                                                }
                                                Text(
                                                    text = priceLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = calcPriceStr,
                                                        onValueChange = { calcPriceStr = it },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(52.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                                        ),
                                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    IconButton(
                                                        onClick = { calcPriceStr = "0" },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Backspace,
                                                            contentDescription = "Clear",
                                                            tint = Color.Red,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Price increment shortcuts
                                                val priceShortcuts = listOf(10, 50, 100, 200, 500, 1000, 2000, 5000, 10000)
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    priceShortcuts.forEach { v ->
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                                .clickable {
                                                                    calcPriceStr = if ((calcPrice + v) % 1.0 == 0.0) {
                                                                        (calcPrice + v).toInt().toString()
                                                                    } else {
                                                                        "%.1f".format(calcPrice + v)
                                                                    }
                                                                }
                                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = "+$v",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Quantity Column
                                            Column(modifier = Modifier.weight(1f)) {
                                                val qtyLabel = when (activeLang) {
                                                    "mg" -> "Isany (Quantity)"
                                                    "fr" -> "Quantité"
                                                    else -> "Quantity"
                                                }
                                                Text(
                                                    text = qtyLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = calcQtyStr,
                                                        onValueChange = { calcQtyStr = it },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(52.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                                        ),
                                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    IconButton(
                                                        onClick = { calcQtyStr = "0" },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Backspace,
                                                            contentDescription = "Clear",
                                                            tint = Color.Red,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Quantity increment shortcuts
                                                val qtyShortcuts = listOf(1, 2, 3, 4, 5, 10, 20, 50)
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    qtyShortcuts.forEach { v ->
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f))
                                                                .border(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                                .clickable {
                                                                    calcQtyStr = if ((calcQty + v) % 1.0 == 0.0) {
                                                                        (calcQty + v).toInt().toString()
                                                                    } else {
                                                                        "%.1f".format(calcQty + v)
                                                                    }
                                                                }
                                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = "+$v",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Total preview card
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    val calcTotalLabel = when (activeLang) {
                                                        "mg" -> "Totalin'ny kajy :"
                                                        "fr" -> "Total Calcul :"
                                                        else -> "Total Calculation:"
                                                    }
                                                    Text(
                                                        text = calcTotalLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${FormatUtil.formatPrice(calcPrice)} Ar x ${if (calcQty % 1.0 == 0.0) calcQty.toInt().toString() else "%.1f".format(calcQty)}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }

                                                Text(
                                                    text = "${FormatUtil.formatPrice(calcTotal)} Ar",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Submit button (Amber yellow like)
                                        Button(
                                            onClick = {
                                                val itemName = when (activeLang) {
                                                    "mg" -> "Kajy (${FormatUtil.formatPrice(calcPrice)} x ${if (calcQty % 1.0 == 0.0) calcQty.toInt().toString() else "%.1f".format(calcQty)})"
                                                    "fr" -> "Calcul (${FormatUtil.formatPrice(calcPrice)} x ${if (calcQty % 1.0 == 0.0) calcQty.toInt().toString() else "%.1f".format(calcQty)})"
                                                    else -> "Calculation (${FormatUtil.formatPrice(calcPrice)} x ${if (calcQty % 1.0 == 0.0) calcQty.toInt().toString() else "%.1f".format(calcQty)})"
                                                }
                                                viewModel.addMiscToCart(itemName, calcPrice, calcQty)

                                                val addedMsg = when (activeLang) {
                                                    "mg" -> "Nampiana kajy: +Ar ${FormatUtil.formatPrice(calcTotal)}"
                                                    "fr" -> "Calcul ajouté : +Ar ${FormatUtil.formatPrice(calcTotal)}"
                                                    else -> "Calculation added: +Ar ${FormatUtil.formatPrice(calcTotal)}"
                                                }
                                                Toast.makeText(context, addedMsg, Toast.LENGTH_SHORT).show()

                                                calcPriceStr = "0"
                                                calcQtyStr = "0"
                                            },
                                            enabled = calcPrice > 0.0 && calcQty > 0.0,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFFB300),
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddShoppingCart,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val addLabel = when (activeLang) {
                                                "mg" -> "Hampiditra ao an-karona"
                                                "fr" -> "Ajouter au panier"
                                                else -> "Add to Cart"
                                            }
                                            Text(
                                                text = addLabel,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 6. Global Bottom Action bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val backLabel = when (activeLang) {
                        "fr" -> "Retour"
                        else -> "Back"
                    }
                    val checkoutLabel = if (isTrosaMode) "Trosa" else when (activeLang) {
                        "mg" -> "Hamarina"
                        "fr" -> "Valider"
                        else -> "Checkout"
                    }

                    OutlinedButton(
                        onClick = { subTab = "checkout" },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = backLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (isTrosaMode) {
                                trosaDebtorName = ""
                                showTrosaDialog = true
                            } else if (amountReceived > 0.0 && amountReceived < totalAmount) {
                                val errStr = when (activeLang) {
                                    "mg" -> "Tsy ampy ny vola nomen'ny mpanjifa! Tsy ampy Ar ${FormatUtil.formatPrice(amountMissing)}"
                                    "fr" -> "Montant insuffisant ! Il manque Ar ${FormatUtil.formatPrice(amountMissing)}"
                                    else -> "Insufficient cash received! Short Ar ${FormatUtil.formatPrice(amountMissing)}"
                                }
                                Toast.makeText(context, errStr, Toast.LENGTH_LONG).show()
                            } else {
                                val success = viewModel.checkoutCart()
                                if (success) {
                                    resetAllFields()
                                    subTab = "checkout"
                                    Toast.makeText(context, t("checkout_success"), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, t("checkout_stock_error"), Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = cart.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isTrosaMode) Color.Red else Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(2f)
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = checkoutLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

    // Clear cart confirmation Alert Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text(
                    text = t("confirm_delete_msg").replace("supprimer définitivement ce produit", "vider le panier")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCart()
                        showClearConfirm = false
                    }
                ) {
                    Text(
                        text = t("vider_panier").replace("Vider ny ", ""),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(t("cancel_btn"))
                }
            }
        )
    }

    if (showBarcodeScanner) {
        val scannerTitle = when (activeLang) {
            "mg" -> "Scanner Code-barres mavitrika"
            "fr" -> "Scanner de code-barres"
            else -> "Simulated Barcode Scanner"
        }
        val scannerHint = when (activeLang) {
            "mg" -> "Asio ny kaody eo anelanelan'ny tsipika maitso"
            "fr" -> "Placez le code-barres dans le cadre vert"
            else -> "Place the barcode inside the green frame"
        }
        val scanSimulationTitle = when (activeLang) {
            "mg" -> "Vokatra misy kaody bar (Hanahaka scan) :"
            "fr" -> "Sélectionner un produit pour simuler :"
            else -> "Select a product to simulate scan:"
        }
        val manualEntryLabel = when (activeLang) {
            "mg" -> "Na soraty eto :"
            "fr" -> "Ou saisir manuellement le code-barres :"
            else -> "Or type barcode manually:"
        }
        val scanBtnLabel = when (activeLang) {
            "mg" -> "Scan"
            "fr" -> "Scanner"
            else -> "Scan"
        }
        val scanCloseLabel = when (activeLang) {
            "mg" -> "Hanakatona"
            "fr" -> "Fermer"
            else -> "Close"
        }

        var manualBarcode by remember { mutableStateOf("") }
        var scanNotification by remember { mutableStateOf<String?>(null) }

        var laserYPercent by remember { mutableStateOf(0.1f) }
        var movingDown by remember { mutableStateOf(true) }
        LaunchedEffect(movingDown) {
            while (true) {
                kotlinx.coroutines.delay(40)
                if (movingDown) {
                    laserYPercent += 0.05f
                    if (laserYPercent >= 0.9f) {
                        movingDown = false
                    }
                } else {
                    laserYPercent -= 0.05f
                    if (laserYPercent <= 0.1f) {
                        movingDown = true
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showBarcodeScanner = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = themeColor
                    )
                    Text(
                        text = scannerTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = scannerHint,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    val realScanLabel = when (activeLang) {
                        "mg" -> "📷 Hanomboka scan mivantana (Fakantsary)"
                        "fr" -> "📷 Scanner en direct avec Caméra"
                        else -> "📷 Launch Live Scanner (Camera)"
                    }

                    Button(
                        onClick = {
                            showRealCameraScanner = true
                            showBarcodeScanner = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(realScanLabel, fontWeight = FontWeight.Bold)
                    }

                    // Viewfinder Screen Simulation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(2.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                showRealCameraScanner = true
                                showBarcodeScanner = false
                            }
                    ) {
                        // Corner borders guides
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cornerSize = 24.dp.toPx()
                            val strokeW = 4.dp.toPx()

                            // Top Left
                            drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cornerSize, strokeW))
                            drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(strokeW, cornerSize))

                            // Top Right
                            drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(w - cornerSize, 0f), size = androidx.compose.ui.geometry.Size(cornerSize, strokeW))
                            drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(w - strokeW, 0f), size = androidx.compose.ui.geometry.Size(strokeW, cornerSize))

                            // Bottom Left
                            drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(0f, h - strokeW), size = androidx.compose.ui.geometry.Size(cornerSize, strokeW))
                            drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(0f, h - cornerSize), size = androidx.compose.ui.geometry.Size(strokeW, cornerSize))

                            // Bottom Right
                            drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(w - cornerSize, h - strokeW), size = androidx.compose.ui.geometry.Size(cornerSize, strokeW))
                            drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(w - strokeW, h - cornerSize), size = androidx.compose.ui.geometry.Size(strokeW, cornerSize))
                        }

                        // Animated Glowing Laser line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = (180 * laserYPercent).dp)
                                .background(Color(0xFF22C55E))
                        )

                        // Notification feedback
                        if (scanNotification != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xCC1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4ADE80),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = scanNotification ?: "",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = themeColor.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .size(64.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    // Simulation List
                    val productsWithBarcodes by viewModel.getProductsWithBarcodes().collectAsState(initial = emptyList())
                    Text(
                        text = scanSimulationTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF334155)
                    )

                    if (productsWithBarcodes.isEmpty()) {
                        Text(
                            text = "❌ " + when(activeLang) {
                                "mg" -> "Tsy misy vokatra misy kaody bar voasoratra aloha."
                                "fr" -> "Aucun produit avec code-barres enregistré."
                                else -> "No barcode products in system."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(productsWithBarcodes) { product ->
                                Card(
                                    onClick = {
                                        viewModel.addToCart(product, 1.0)
                                        scanNotification = "${product.name} (Ar ${product.price})\nBarcode: ${product.barcode}"
                                    },
                                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${product.name} [${product.barcode}]",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = manualEntryLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualBarcode,
                            onValueChange = { manualBarcode = it },
                            placeholder = { Text("Ex: 6111222333444", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val match = viewModel.getProductByBarcode(manualBarcode.trim())
                                    if (match != null) {
                                        viewModel.addToCart(match, 1.0)
                                        scanNotification = "${match.name} (Ar ${match.price})\nBarcode: ${match.barcode}"
                                    } else {
                                        scanNotification = when(activeLang) {
                                            "mg" -> "Tsy hita : ${manualBarcode.trim()}"
                                            "fr" -> "Non trouvé : ${manualBarcode.trim()}"
                                            else -> "Not found: ${manualBarcode.trim()}"
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(scanBtnLabel, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showBarcodeScanner = false
                    }
                ) {
                    Text(scanCloseLabel)
                }
            }
        )

        if (scanNotification != null) {
            LaunchedEffect(scanNotification) {
                kotlinx.coroutines.delay(1800)
                scanNotification = null
                manualBarcode = ""
            }
        }
    }

    if (showRealCameraScanner) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showRealCameraScanner = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                BarcodeScannerView(
                    onBarcodeScanned = { scannedCode ->
                        // Managed directly in continuous mode
                    },
                    onClose = {
                        showRealCameraScanner = false
                    },
                    language = activeLang,
                    themeColor = themeColor,
                    continuousMode = true,
                    viewModel = viewModel
                )
            }
        }
    }

    if (showAddedMiscDialog) {
        val dialogTitle = when (activeLang) {
            "mg" -> "Entana mivantana vao nampidirina"
            "fr" -> "Articles divers ajoutés"
            else -> "Added Miscellaneous Items"
        }
        val closeLabel = when (activeLang) {
            "mg" -> "Hanakatona"
            "fr" -> "Fermer"
            else -> "Close"
        }
        AlertDialog(
            onDismissRequest = { showAddedMiscDialog = false },
            title = {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val miscCartItems = cart.filter { it.id.startsWith("misc_") }
                if (miscCartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Tsy misy entana mivantana nampidirina."
                                "fr" -> "Aucun article divers ajouté."
                                else -> "No miscellaneous items added."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(miscCartItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val formattedQty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.1f".format(item.quantity)
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Qty: $formattedQty",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${FormatUtil.formatPrice(item.price * item.quantity)} Ar",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.changeCartQuantityByDelta(item.id, -1.0)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveCircle,
                                            contentDescription = "Remove",
                                            tint = Color.Red,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAddedMiscDialog = false }
                ) {
                    Text(
                        text = closeLabel,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    if (showTrosaDialog) {
        val trosaTitle = when (activeLang) {
            "mg" -> "Trosa vao vaovao"
            "fr" -> "Nouvelle dette (Trosa)"
            else -> "New Debt (Trosa)"
        }
        val trosaText = when (activeLang) {
            "mg" -> "Ampidiro ny anaran'ny mpanjifa mitrosa:"
            "fr" -> "Entrez le nom du bénéficiaire :"
            else -> "Enter debtor name:"
        }
        val trosaLabel = when (activeLang) {
            "mg" -> "Anaran'ny mpanjifa"
            "fr" -> "Nom du bénéficiaire"
            else -> "Debtor name"
        }
        val confirmLabel = when (activeLang) {
            "mg" -> "Hamarina"
            "fr" -> "Valider"
            else -> "Confirm"
        }
        val cancelLabel = when (activeLang) {
            "mg" -> "Hanafoana"
            "fr" -> "Annuler"
            else -> "Cancel"
        }

        AlertDialog(
            onDismissRequest = {
                trosaDebtorName = ""
                showTrosaDialog = false
            },
            title = {
                Text(
                    text = trosaTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = trosaText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = trosaDebtorName,
                        onValueChange = { trosaDebtorName = it },
                        label = { Text(trosaLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = trosaDebtorName.trim()
                        if (trimmedName.isNotEmpty()) {
                            val itemsNote = cart.joinToString(", ") { "${it.name} (${if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else it.quantity} x ${FormatUtil.formatPrice(it.price)})" }
                            val newDebt = Debt(
                                debtorName = trimmedName,
                                amount = totalAmount,
                                balance = totalAmount,
                                date = System.currentTimeMillis(),
                                note = itemsNote,
                                isPaid = false
                            )
                            viewModel.saveDebt(newDebt)
                            val success = viewModel.checkoutCart()
                            if (success) {
                                resetAllFields()
                                showTrosaDialog = false
                                Toast.makeText(context, t("checkout_success"), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, t("checkout_stock_error"), Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = trosaDebtorName.trim().isNotEmpty()
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        trosaDebtorName = ""
                        showTrosaDialog = false
                    }
                ) {
                    Text(cancelLabel)
                }
            }
        )
    }
}

@Composable
fun PresetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colorScheme.primary
        ),
        elevation = null,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.height(22.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun CashRegisterProductCard(
    product: Product,
    onAddToCart: (Product, Double) -> Unit,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    val isLiquid = product.unit.lowercase().contains("litre") || 
                   product.unit == "L"
    val isWeight = product.unit.lowercase().contains("kilo") || 
                   product.unit.lowercase().contains("kg")

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            Color(0xFFE0E0E0)
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Row with Image on left, Stock & Price on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image Box
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(16.dp))
                        .background(Color(0xFFF9F9F9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFFCCCCCC),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Stock and Price Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val stockQtyStr = if (product.stock % 1.0 == 0.0) {
                        product.stock.toInt().toString()
                    } else {
                        "%.1f".format(product.stock)
                    }
                    Text(
                        text = "T : $stockQtyStr",
                        color = if (isOutOfStock) MaterialTheme.colorScheme.error else Color(0xFF757575),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${FormatUtil.formatPrice(product.price)} Ar",
                        color = Color(0xFF333333),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Product Name
            Text(
                text = product.name,
                color = Color(0xFF333333),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Preset Buttons row (pill style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isLiquid) {
                    // Liquid: 1/4 L, 1/2 L, +1
                    PillPresetButton(
                        text = "1/4 L",
                        onClick = { onAddToCart(product, 0.25) },
                        modifier = Modifier.weight(1f)
                    )
                    PillPresetButton(
                        text = "1/2 L",
                        onClick = { onAddToCart(product, 0.5) },
                        modifier = Modifier.weight(1f)
                    )
                    PillPresetButton(
                        text = "+1",
                        onClick = { onAddToCart(product, 1.0) },
                        modifier = Modifier.weight(1f)
                    )
                } else if (isWeight) {
                    // Weight: 1/4 kg, 1/2 kg, +1
                    PillPresetButton(
                        text = "1/4 kg",
                        onClick = { onAddToCart(product, 0.25) },
                        modifier = Modifier.weight(1f)
                    )
                    PillPresetButton(
                        text = "1/2 kg",
                        onClick = { onAddToCart(product, 0.5) },
                        modifier = Modifier.weight(1f)
                    )
                    PillPresetButton(
                        text = "+1",
                        onClick = { onAddToCart(product, 1.0) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Standard: +1, +5, +10 (matching the image exactly)
                    PillPresetButton(
                        text = "+1",
                        onClick = { onAddToCart(product, 1.0) },
                        modifier = Modifier.weight(1f)
                    )
                    PillPresetButton(
                        text = "+5",
                        onClick = { onAddToCart(product, 5.0) },
                        modifier = Modifier.weight(1f)
                    )
                    PillPresetButton(
                        text = "+10",
                        onClick = { onAddToCart(product, 10.0) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PillPresetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF8CC1F0)) // Lovely soft blue matching image
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
