package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Fournisseur
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.FormatUtil
import com.example.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommissionScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val suppliers by viewModel.allFournisseurs.collectAsState()

    // 2 main tabs: 1. Calculateur, 2. Alerte Stock (Low Stock)
    var selectedTabState by remember { mutableStateOf(0) }
    val tabs = when (activeLang) {
        "mg" -> listOf("Kajy famatsiana", "Tahiry ho lany")
        "fr" -> listOf("Calculateur d'achat", "Stock Faible")
        else -> listOf("Procurement Calculator", "Low Stock")
    }

    // Calculator states
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantityStr by remember { mutableStateOf("12") }       // default e.g. 12 cartons
    var itemsPerCartonStr by remember { mutableStateOf("24") } // default e.g. 24 soaps per carton
    var totalCostPriceStr by remember { mutableStateOf("120000") } // default total wholesale cost
    var unitSellingPriceStr by remember { mutableStateOf("600") }  // new retail price per unit
    var selectedFournisseurId by remember { mutableStateOf<Long?>(null) }

    // Dropdown triggers
    var showProductDropdown by remember { mutableStateOf(false) }
    var showSupplierDropdown by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }

    // New Supplier Quick Dialog
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var newSupplierContact by remember { mutableStateOf("") }
    var newSupplierAddress by remember { mutableStateOf("") }

    // Success notification
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // Filter products for dropdown search
    val filteredProductList = remember(allProducts, productSearchQuery) {
        if (productSearchQuery.isBlank()) {
            allProducts
        } else {
            allProducts.filter { it.name.contains(productSearchQuery, ignoreCase = true) }
        }
    }

    // Auto-prefill selling price when product changes
    LaunchedEffect(selectedProduct) {
        selectedProduct?.let {
            unitSellingPriceStr = if (it.price % 1.0 == 0.0) it.price.toInt().toString() else it.price.toString()
            productSearchQuery = it.name
            // prefill wholesale price if exists
            it.wholesalePrice?.let { wp ->
                // guess items per carton if wholesale exists
                val wpVal = if (wp % 1.0 == 0.0) wp.toInt() else wp.toDouble()
                // let's keep things customizable
            }
        }
    }

    // Main Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Hiverina",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = when (activeLang) {
                        "mg" -> "Kajy Tombom-barotra & Famatsiana"
                        "fr" -> "Approvisionnement & Bénéfices"
                        else -> "Procurement & Margins"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B),
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Tab Bar
            TabRow(
                selectedTabIndex = selectedTabState,
                containerColor = Color.White,
                contentColor = themeColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                        color = themeColor
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabState == index,
                        onClick = { selectedTabState = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTabState == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // 3. Tab Contents
            if (selectedTabState == 0) {
                // TAB 1: RESTOCK CALCULATOR
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section Title: Product Selection
                    Text(
                        text = when (activeLang) {
                            "mg" -> "1. Safidio ny vokatra sy ny mpamatsy"
                            "fr" -> "1. Choisir le produit & fournisseur"
                            else -> "1. Select Product & Supplier"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    // Searchable Dropdown for Product
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = productSearchQuery,
                            onValueChange = {
                                productSearchQuery = it
                                showProductDropdown = true
                                if (selectedProduct?.name != it) {
                                    selectedProduct = null
                                }
                            },
                            label = {
                                Text(
                                    when (activeLang) {
                                        "mg" -> "Tadiavo ny vokatra (Vary, Savony...)"
                                        "fr" -> "Rechercher un produit..."
                                        else -> "Search Product..."
                                    }
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showProductDropdown = !showProductDropdown }) {
                                    Icon(
                                        imageVector = if (showProductDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("commission_product_search"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        DropdownMenu(
                            expanded = showProductDropdown && filteredProductList.isNotEmpty(),
                            onDismissRequest = { showProductDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 240.dp)
                        ) {
                            filteredProductList.forEach { product ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(product.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${FormatUtil.formatPrice(product.price)} Ar",
                                                color = themeColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = product
                                        showProductDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Supplier Row with Dropdown and Quick Add button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            val activeSupplier = suppliers.find { it.id == selectedFournisseurId }
                            OutlinedTextField(
                                value = activeSupplier?.nom ?: when (activeLang) {
                                    "mg" -> "Mpamatsy tsy fantatra"
                                    "fr" -> "Fournisseur non spécifié"
                                    else -> "No Supplier Selected"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = {
                                    Text(
                                        when (activeLang) {
                                            "mg" -> "Mpamatsy (Fournisseur)"
                                            "fr" -> "Fournisseur"
                                            else -> "Supplier"
                                        }
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showSupplierDropdown = !showSupplierDropdown }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            DropdownMenu(
                                expanded = showSupplierDropdown,
                                onDismissRequest = { showSupplierDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (activeLang == "mg") "Tsy misy" else "Aucun / Sans fournisseur") },
                                    onClick = {
                                        selectedFournisseurId = null
                                        showSupplierDropdown = false
                                    }
                                )
                                suppliers.forEach { supplier ->
                                    DropdownMenuItem(
                                        text = { Text(supplier.nom, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedFournisseurId = supplier.id
                                            showSupplierDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quick Add Supplier Button
                        IconButton(
                            onClick = { showAddSupplierDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(themeColor.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Quick Add Supplier",
                                tint = themeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Section Title: Procurement parameters
                    Text(
                        text = when (activeLang) {
                            "mg" -> "2. Ampidiro ny vidiny sy ny habetsahany"
                            "fr" -> "2. Entrer les quantités & prix d'achat"
                            else -> "2. Enter Quantity & Cost prices"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    // Inputs Row 1: Quantity of Cartons & Items per Carton
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = {
                                Text(
                                    when (activeLang) {
                                        "mg" -> "Isan'ny baoritra / lot"
                                        "fr" -> "Nbr Cartons / Lots"
                                        else -> "Quantity (Cartons)"
                                    }
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = itemsPerCartonStr,
                            onValueChange = { itemsPerCartonStr = it },
                            label = {
                                Text(
                                    when (activeLang) {
                                        "mg" -> "Singany isaky ny baoritra"
                                        "fr" -> "Pièces par Carton"
                                        else -> "Items per Carton"
                                    }
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Inputs Row 2: Total Cost price & Unit Selling Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = totalCostPriceStr,
                            onValueChange = { totalCostPriceStr = it },
                            label = {
                                Text(
                                    when (activeLang) {
                                        "mg" -> "Vidiny nividianana azy manontolo"
                                        "fr" -> "Coût d'achat total"
                                        else -> "Total Cost Price"
                                    }
                                )
                            },
                            suffix = { Text("Ar", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = unitSellingPriceStr,
                            onValueChange = { unitSellingPriceStr = it },
                            label = {
                                Text(
                                    when (activeLang) {
                                        "mg" -> "Vidiny hivorotana azy (Singany)"
                                        "fr" -> "Prix de vente unit."
                                        else -> "Unit Selling Price"
                                    }
                                )
                            },
                            suffix = { Text("Ar", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981) // Highlight green since they can adjust this
                            )
                        )
                    }

                    // Profit Calculations Section (Auto-Calculated)
                    val qVal = quantityStr.toDoubleOrNull() ?: 1.0
                    val itemsVal = itemsPerCartonStr.toDoubleOrNull() ?: 1.0
                    val totalCostVal = totalCostPriceStr.toDoubleOrNull() ?: 0.0
                    val sellPriceVal = unitSellingPriceStr.toDoubleOrNull() ?: 0.0

                    val totalUnits = qVal * itemsVal
                    val costPerCarton = if (qVal > 0) totalCostVal / qVal else 0.0
                    val costPerUnit = if (totalUnits > 0) totalCostVal / totalUnits else 0.0
                    val totalRevenue = totalUnits * sellPriceVal
                    val totalProfit = totalRevenue - totalCostVal
                    val profitPerCarton = if (qVal > 0) totalProfit / qVal else 0.0
                    val profitPerUnit = sellPriceVal - costPerUnit
                    val marginPct = if (totalCostVal > 0) (totalProfit / totalCostVal) * 100 else 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = themeColor
                                )
                                Text(
                                    text = when (activeLang) {
                                        "mg" -> "Kajy Tombony & Elanelana (Analyse de Marge)"
                                        "fr" -> "Analyse de Marge & Bénéfices"
                                        else -> "Margin & Profit Analysis"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            }

                            HorizontalDivider(color = themeColor.copy(alpha = 0.1f))

                            // Outputs grid
                            CalculationRow(
                                label = if (activeLang == "mg") "Isany manontolo ho tafiditra" else "Total unités approvisionnées",
                                value = "${totalUnits.toInt()} ${selectedProduct?.unit ?: "Pces"}",
                                isBold = true
                            )

                            CalculationRow(
                                label = if (activeLang == "mg") "Vidiny nividianana ny 1 Baoritra" else "Coût de revient par Carton",
                                value = "${FormatUtil.formatPrice(costPerCarton)} Ar"
                            )

                            CalculationRow(
                                label = if (activeLang == "mg") "Vidiny nividianana ny 1 Singany" else "Coût de revient par Singany (unité)",
                                value = "${FormatUtil.formatPrice(costPerUnit)} Ar"
                            )

                            HorizontalDivider(color = themeColor.copy(alpha = 0.1f))

                            CalculationRow(
                                label = if (activeLang == "mg") "Vola maty manontolo (Chiffre d'Affaire)" else "Revenu de vente total",
                                value = "${FormatUtil.formatPrice(totalRevenue)} Ar"
                            )

                            CalculationRow(
                                label = if (activeLang == "mg") "TOMBOM-BAROTRA MANONTOLO (Bénéfice)" else "BÉNÉFICE TOTAL OBTENU",
                                value = "${FormatUtil.formatPrice(totalProfit)} Ar",
                                color = if (totalProfit >= 0) Color(0xFF10B981) else Color.Red,
                                isBold = true
                            )

                            CalculationRow(
                                label = if (activeLang == "mg") "Tombony isaky ny Baoritra (Bénéfice/Carton)" else "Bénéfice par Carton",
                                value = "${FormatUtil.formatPrice(profitPerCarton)} Ar",
                                color = if (profitPerCarton >= 0) Color(0xFF059669) else Color.Red,
                                isBold = true
                            )

                            CalculationRow(
                                label = if (activeLang == "mg") "Tombony isaky ny Singany (Bénéfice/Unité)" else "Bénéfice par Unité",
                                value = "${FormatUtil.formatPrice(profitPerUnit)} Ar",
                                color = if (profitPerUnit >= 0) Color(0xFF059669) else Color.Red
                            )

                            CalculationRow(
                                label = if (activeLang == "mg") "Taux de Marge obtenu" else "Taux de Marge (%)",
                                value = "%.1f %%".format(marginPct),
                                color = themeColor,
                                isBold = true
                            )
                        }
                    }

                    // Save Restock action button
                    Button(
                        onClick = {
                            val prod = selectedProduct
                            if (prod == null) {
                                return@Button
                            }
                            // Calculate new stock and update
                            val newStock = prod.stock + totalUnits
                            // Update price, stock, supplier, wholesalePrice on product
                            val updatedProduct = prod.copy(
                                stock = newStock,
                                price = sellPriceVal,
                                wholesalePrice = costPerUnit,
                                prixAchatUniteBase = costPerUnit,
                                fournisseurId = selectedFournisseurId
                            )
                            viewModel.saveProduct(updatedProduct)

                            successMessage = when (activeLang) {
                                "mg" -> "Tafita! Nampiana +${totalUnits.toInt()} ny stock an'i ${prod.name}."
                                "fr" -> "Réapprovisionnement réussi ! +${totalUnits.toInt()} unités de ${prod.name}."
                                else -> "Procurement completed successfully! +${totalUnits.toInt()} of ${prod.name} added."
                            }
                            showSuccessSnackbar = true

                            // reset forms
                            selectedProduct = null
                            productSearchQuery = ""
                            quantityStr = "12"
                            itemsPerCartonStr = "24"
                            totalCostPriceStr = "120000"
                            unitSellingPriceStr = ""
                        },
                        enabled = selectedProduct != null && totalUnits > 0 && totalCostVal > 0 && sellPriceVal > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("validate_commission_restock_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Hamafiso ny fampidirana entana"
                                "fr" -> "Valider le réapprovisionnement"
                                else -> "Confirm & Complete Restock"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                // TAB 2: ALERTE STOCK / LOW STOCK RESTOCK SUGGESTIONS
                val lowStockProducts = remember(allProducts) {
                    allProducts.filter { it.stock < it.lowStockThreshold }
                }

                if (lowStockProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Milamina tsara ny tahiry! Tsy misy lany."
                                    "fr" -> "Le stock est parfait ! Rien à réapprovisionner."
                                    else -> "Excellent stock level! No products low on stock."
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Ireto vokatra ireto dia efa ho lany (Mila famatsiana):"
                                "fr" -> "Ces produits nécessitent un réapprovisionnement :"
                                else -> "These products need restocking soon:"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )

                        lowStockProducts.forEach { product ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFEE2E2))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                val currentStockStr = if (product.stock % 1.0 == 0.0) product.stock.toInt().toString() else "%.1f".format(product.stock)
                                                Text(
                                                    text = "${currentStockStr} ${product.unit}",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "(Seuil: ${product.lowStockThreshold.toInt()})",
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "PV: ${FormatUtil.formatPrice(product.price)} Ar | PA: ${FormatUtil.formatPrice(product.wholesalePrice ?: product.prixAchatUniteBase)} Ar",
                                            fontSize = 12.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            selectedProduct = product
                                            selectedTabState = 0 // switch to calculator
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = themeColor,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (activeLang == "mg") "Hamatsy" else "Approvisionner",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Animated snackbar notification at the bottom
        if (showSuccessSnackbar) {
            LaunchedEffect(showSuccessSnackbar) {
                kotlinx.coroutines.delay(2500)
                showSuccessSnackbar = false
            }
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981)
                    )
                    Text(
                        text = successMessage,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Add Supplier Quick Dialog
        if (showAddSupplierDialog) {
            AlertDialog(
                onDismissRequest = { showAddSupplierDialog = false },
                title = {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Mpamatsy Vaovao"
                            "fr" -> "Nouveau Fournisseur"
                            else -> "New Supplier"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newSupplierName,
                            onValueChange = { newSupplierName = it },
                            label = { Text(if (activeLang == "mg") "Anaran'ny mpamatsy" else "Nom du fournisseur") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newSupplierContact,
                            onValueChange = { newSupplierContact = it },
                            label = { Text(if (activeLang == "mg") "Contact" else "Télephone / Contact") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newSupplierAddress,
                            onValueChange = { newSupplierAddress = it },
                            label = { Text(if (activeLang == "mg") "Adrany" else "Adresse") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newSupplierName.trim().isNotEmpty()) {
                                val supplier = Fournisseur(
                                    nom = newSupplierName.trim(),
                                    contact = if (newSupplierContact.trim().isEmpty()) null else newSupplierContact.trim(),
                                    adresse = if (newSupplierAddress.trim().isEmpty()) null else newSupplierAddress.trim()
                                )
                                viewModel.saveFournisseur(supplier)
                                showAddSupplierDialog = false
                                newSupplierName = ""
                                newSupplierContact = ""
                                newSupplierAddress = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Text(if (activeLang == "mg") "Tehirizina" else "Enregistrer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSupplierDialog = false }) {
                        Text(if (activeLang == "mg") "Hanafoana" else "Annuler")
                    }
                }
            )
        }
    }
}

@Composable
fun CalculationRow(
    label: String,
    value: String,
    color: Color = Color(0xFF1E293B),
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.3f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = color,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
