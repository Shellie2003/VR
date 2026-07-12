package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.BorderStroke
import com.example.ui.components.BarcodeScannerView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: InventoryViewModel,
    editingProduct: Product?,
    onSaveProduct: (Product) -> Unit,
    onCancel: () -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val t = { key: String -> LanguageManager.translate(key, activeLang) }

    // State holders
    var name by remember(editingProduct) { mutableStateOf(editingProduct?.name ?: "") }
    var priceStr by remember(editingProduct) { mutableStateOf(editingProduct?.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var stockStr by remember(editingProduct) { mutableStateOf(editingProduct?.stock?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var imageUrl by remember(editingProduct) { mutableStateOf(editingProduct?.imageUrl ?: "") }
    var lowStockThresholdStr by remember(editingProduct) { mutableStateOf(editingProduct?.lowStockThreshold?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "5") }
    var barcode by remember(editingProduct) { mutableStateOf(editingProduct?.barcode ?: "") }
    var wholesalePriceStr by remember(editingProduct) { mutableStateOf(editingProduct?.wholesalePrice?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var wholesalePriceError by remember { mutableStateOf(false) }
    var showLiveScanner by remember { mutableStateOf(false) }
    var sku by remember(editingProduct) { mutableStateOf(editingProduct?.sku ?: "") }
    var stockQuantityStr by remember(editingProduct) { mutableStateOf(editingProduct?.stock_quantity?.toString() ?: "0") }
    var stockQuantityError by remember { mutableStateOf(false) }

    // Dropdown Categories
    val standardCategories = listOf("Alimentation", "Légumes", "Boissons", "Épicerie", "Droguerie", "Hafa")
    var selectedCategory by remember(editingProduct) {
        val cat = editingProduct?.category ?: "Alimentation"
        mutableStateOf(if (standardCategories.contains(cat)) cat else "Hafa")
    }
    var customCategory by remember(editingProduct) {
        val cat = editingProduct?.category ?: ""
        mutableStateOf(if (!standardCategories.contains(cat)) cat else "")
    }

    // Units
    val units = listOf("Pièce", "Litre", "Kilogramme", "Paquet", "Carton", "Sac", "Boîte", "Bouteille", "Tasse/Kapoaka")
    var selectedUnit by remember(editingProduct) {
        mutableStateOf(editingProduct?.unit ?: "Pièce")
    }

    var hasManuallyChangedUnit by remember { mutableStateOf(false) }
    var hasManuallyChangedCategory by remember { mutableStateOf(false) }

    // Smart prediction effect
    LaunchedEffect(name) {
        if (name.isBlank() || editingProduct != null) return@LaunchedEffect
        val lower = name.lowercase()
        
        if (!hasManuallyChangedCategory) {
            val predictedCat = when {
                lower.contains("vary") || lower.contains("bary") || lower.contains("rice") || lower.contains("menaka") || lower.contains("oil") || lower.contains("huile") || lower.contains("sira") || lower.contains("sel") || lower.contains("saka") || lower.contains("sukra") || lower.contains("sucre") || lower.contains("biski") || lower.contains("biscuit") || lower.contains("koba") || lower.contains("farine") || lower.contains("lafarina") -> "Alimentation"
                lower.contains("karoty") || lower.contains("pataty") || lower.contains("voatabia") || lower.contains("tongolo") || lower.contains("oignon") || lower.contains("legume") || lower.contains("tsaramaso") -> "Légumes"
                lower.contains("rano") || lower.contains("eau") || lower.contains("jus") || lower.contains("cola") || lower.contains("beer") || lower.contains("biera") || lower.contains("fanta") || lower.contains("boisson") || lower.contains("sprite") -> "Boissons"
                lower.contains("savony") || lower.contains("soap") || lower.contains("omipitika") || lower.contains("detergent") || lower.contains("shampoo") || lower.contains("odifitra") -> "Droguerie"
                lower.contains("chocolat") || lower.contains("bonbon") || lower.contains("chewing") || lower.contains("kafe") || lower.contains("cafe") || lower.contains("the") -> "Épicerie"
                else -> null
            }
            if (predictedCat != null) {
                selectedCategory = predictedCat
            }
        }
        
        if (!hasManuallyChangedUnit) {
            val predictedUnit = when {
                lower.contains("vary") || lower.contains("bary") || lower.contains("rice") || lower.contains("sira") || lower.contains("sel") || lower.contains("sukra") || lower.contains("sucre") || lower.contains("lafarina") || lower.contains("farine") || lower.contains("karoty") || lower.contains("voatabia") || lower.contains("pataty") || lower.contains("legume") || lower.contains("tongolo") || lower.contains("oignon") -> "Kilogramme"
                lower.contains("menaka") || lower.contains("oil") || lower.contains("huile") || lower.contains("rano") || lower.contains("eau") || lower.contains("ronono") || lower.contains("lait") -> "Litre"
                lower.contains("carton") || lower.contains("baoritra") || lower.contains("boite de") -> "Carton"
                lower.contains("sac") || lower.contains("gony") -> "Sac"
                lower.contains("paquet") || lower.contains("fonosana") || lower.contains("biski") || lower.contains("biscuit") -> "Paquet"
                lower.contains("boite") || lower.contains("boatina") || lower.contains("can") -> "Boîte"
                lower.contains("bouteille") || lower.contains("tavoahangy") || lower.contains("cola") || lower.contains("fanta") || lower.contains("biera") || lower.contains("beer") || lower.contains("sprite") -> "Bouteille"
                lower.contains("kapoaka") || lower.contains("tasse") -> "Tasse/Kapoaka"
                else -> "Pièce"
            }
            selectedUnit = predictedUnit
        }
    }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showUnitDropdown by remember { mutableStateOf(false) }

    // Form validation states
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var stockError by remember { mutableStateOf(false) }
    var thresholdError by remember { mutableStateOf(false) }

    val isEditing = editingProduct != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen title
            Text(
                text = if (isEditing) t("edit_product_title") else t("add_product_title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = it.isBlank()
                },
                label = { Text(t("product_name")) },
                isError = nameError,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Category select drop down
            Box(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(t("category_label")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .testTag("product_category_select"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        standardCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    hasManuallyChangedCategory = true
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Custom category input if "Hafa"
            if (selectedCategory == "Hafa") {
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { 
                        customCategory = it 
                        hasManuallyChangedCategory = true
                    },
                    label = { Text(t("custom_category_label")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_custom_category_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Unit of Measure selection drop down
            Box(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = showUnitDropdown,
                    onExpandedChange = { showUnitDropdown = !showUnitDropdown }
                ) {
                    OutlinedTextField(
                        value = when (selectedUnit) {
                            "Pièce" -> t("unit_piece")
                            "Litre" -> t("unit_litre")
                            "Kilogramme" -> t("unit_kg")
                            "Paquet" -> t("unit_paquet")
                            "Carton" -> t("unit_carton")
                            "Sac" -> t("unit_sac")
                            "Boîte" -> t("unit_boite")
                            "Bouteille" -> t("unit_bouteille")
                            "Tasse/Kapoaka" -> t("unit_kapoaka")
                            else -> selectedUnit
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(t("unit_label")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUnitDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .testTag("product_unit_select"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showUnitDropdown,
                        onDismissRequest = { showUnitDropdown = false }
                    ) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (u) {
                                            "Pièce" -> t("unit_piece")
                                            "Litre" -> t("unit_litre")
                                            "Kilogramme" -> t("unit_kg")
                                            "Paquet" -> t("unit_paquet")
                                            "Carton" -> t("unit_carton")
                                            "Sac" -> t("unit_sac")
                                            "Boîte" -> t("unit_boite")
                                            "Bouteille" -> t("unit_bouteille")
                                            "Tasse/Kapoaka" -> t("unit_kapoaka")
                                            else -> u
                                        }
                                    )
                                },
                                onClick = {
                                    selectedUnit = u
                                    hasManuallyChangedUnit = true
                                    showUnitDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Price unit input
            OutlinedTextField(
                value = priceStr,
                onValueChange = {
                    priceStr = it
                    priceError = it.toDoubleOrNull() == null || it.toDouble() <= 0
                },
                label = { Text(t("unit_price")) },
                prefix = { Text("Ar ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = priceError,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_price_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Wholesale Price Input
            val wholesaleLabel = when (activeLang) {
                "mg" -> "Vidiny Ambongadiny (Wholesale Price)"
                "fr" -> "Prix de gros"
                else -> "Wholesale Price"
            }
            OutlinedTextField(
                value = wholesalePriceStr,
                onValueChange = {
                    wholesalePriceStr = it
                    wholesalePriceError = it.isNotEmpty() && (it.toDoubleOrNull() == null || it.toDouble() < 0)
                },
                label = { Text(wholesaleLabel) },
                prefix = { Text("Ar ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = wholesalePriceError,
                supportingText = { Text("Ampiasaina amin'ny fomba 'Grossiste'") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_wholesale_price_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Barcode Input
            val barcodeLabel = when (activeLang) {
                "mg" -> "Kaody Bar (Barcode)"
                "fr" -> "Code-barres"
                else -> "Barcode"
            }
            
            val barcodeSupportText = when (activeLang) {
                "mg" -> "Tsindrio ny fakantsary handinihana kaody, na ny bokotra rafraîchir hamoronana kaody"
                "fr" -> "Appuyez sur l'appareil photo pour scanner, ou sur l'icône rafraîchir pour générer un code"
                else -> "Tap camera to scan, or refresh icon to generate a random code"
            }

            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text(barcodeLabel) },
                trailingIcon = {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            showLiveScanner = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scan Barcode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            // Generate a random high-quality barcode
                            val randomPrefix = listOf("611", "301", "325", "400").random()
                            val randomDigits = (1000000000..9999999999).random().toString()
                            barcode = randomPrefix + randomDigits.take(10)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Generate Barcode",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                supportingText = { Text(barcodeSupportText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_barcode_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // SKU Input
            val skuLabel = when (activeLang) {
                "mg" -> "Kaody SKU (Stock Keeping Unit)"
                "fr" -> "Code SKU (Stock Keeping Unit)"
                else -> "SKU (Stock Keeping Unit)"
            }
            val skuSupportText = when (activeLang) {
                "mg" -> "ID manokana ho an'ny fitantanana tahiry"
                "fr" -> "Identifiant unique pour la gestion des stocks"
                else -> "Unique identifier for inventory tracking"
            }
            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text(skuLabel) },
                supportingText = { Text(skuSupportText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_sku_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Initial Stock Input & Unit Selector card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = when(activeLang) {
                            "mg" -> "Tahiry & Karazana Refy"
                            "fr" -> "Stock & Type de mesure"
                            else -> "Stock & Measurement Unit"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = {
                            stockStr = it
                            stockError = it.toDoubleOrNull() == null || it.toDouble() < 0
                        },
                        label = { Text(t("initial_stock")) },
                        supportingText = { Text("Ex: 1.5, 20.0, 100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stockError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_stock_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Stock Quantity field
                    val stockQtyLabel = when (activeLang) {
                        "mg" -> "Isan'ny Tahiry (Stock Quantity - Int)"
                        "fr" -> "Quantité de stock (Entier)"
                        else -> "Stock Quantity (Integer)"
                    }
                    val stockQtySupportText = when (activeLang) {
                        "mg" -> "Isan'ny entana eo am-pelatanana (Tsy misy faingo)"
                        "fr" -> "Nombre de produits disponibles (Entier)"
                        else -> "Number of items on hand (Integer)"
                    }
                    OutlinedTextField(
                        value = stockQuantityStr,
                        onValueChange = {
                            stockQuantityStr = it
                            stockQuantityError = it.toIntOrNull() == null || it.toInt() < 0
                        },
                        label = { Text(stockQtyLabel) },
                        supportingText = { Text(stockQtySupportText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stockQuantityError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_stock_quantity_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick unit selection chips
                    Text(
                        text = when(activeLang) {
                            "mg" -> "Hifidy ny refy mivantana amin'ny tahiry :"
                            "fr" -> "Sélectionner l'unité pour ce stock :"
                            else -> "Select unit for this stock:"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )

                    // Group 1 of units (first 5)
                    val row1Units = units.take(5)
                    // Group 2 of units (remaining 4)
                    val row2Units = units.drop(5)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row1Units.forEach { u ->
                            val isSelected = selectedUnit == u
                            val uLabel = when (u) {
                                "Pièce" -> t("unit_piece")
                                "Litre" -> t("unit_litre")
                                "Kilogramme" -> t("unit_kg")
                                "Paquet" -> t("unit_paquet")
                                "Carton" -> t("unit_carton")
                                "Sac" -> t("unit_sac")
                                "Boîte" -> t("unit_boite")
                                "Bouteille" -> t("unit_bouteille")
                                "Tasse/Kapoaka" -> t("unit_kapoaka")
                                else -> u
                            }

                            val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            val chipTextCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            val chipBorder = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedUnit = u
                                        hasManuallyChangedUnit = true
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = chipTextCol,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    Text(
                                        text = uLabel,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = chipTextCol
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Placeholders to balance weight if needed, or expand to fill
                        row2Units.forEach { u ->
                            val isSelected = selectedUnit == u
                            val uLabel = when (u) {
                                "Pièce" -> t("unit_piece")
                                "Litre" -> t("unit_litre")
                                "Kilogramme" -> t("unit_kg")
                                "Paquet" -> t("unit_paquet")
                                "Carton" -> t("unit_carton")
                                "Sac" -> t("unit_sac")
                                "Boîte" -> t("unit_boite")
                                "Bouteille" -> t("unit_bouteille")
                                "Tasse/Kapoaka" -> t("unit_kapoaka")
                                else -> u
                            }

                            val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            val chipTextCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            val chipBorder = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedUnit = u
                                        hasManuallyChangedUnit = true
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = chipTextCol,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    Text(
                                        text = uLabel,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = chipTextCol
                                    )
                                }
                            }
                        }
                        
                        // Add an empty spacer to balance the weights perfectly (row 2 has 4 elements, row 1 has 5)
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Show hint based on selected unit
                    val hintMsg = when (selectedUnit) {
                        "Carton", "Sac" -> when(activeLang) {
                            "mg" -> "💡 Tsara ampiasaina amin'ny fivarotana ambongadiny (Grossiste)"
                            "fr" -> "💡 Idéal pour la vente en gros (Wholesale)"
                            else -> "💡 Ideal for wholesale / bulk sales"
                        }
                        "Litre", "Kilogramme" -> when(activeLang) {
                            "mg" -> "💡 Afaka mampiasa fehezana desimaly (ohatra: 1.5 kg, 0.5 litra)"
                            "fr" -> "💡 Supporte les stocks décimaux (ex: 1.5 kg, 0.5 litre)"
                            else -> "💡 Supports decimal stocks (e.g. 1.5 kg, 0.5 liter)"
                        }
                        else -> null
                    }
                    if (hintMsg != null) {
                        Text(
                            text = hintMsg,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Low Stock Threshold Input (Double)
            OutlinedTextField(
                value = lowStockThresholdStr,
                onValueChange = {
                    lowStockThresholdStr = it
                    thresholdError = it.toDoubleOrNull() == null || it.toDouble() < 0
                },
                label = { Text("Low Stock Alert Seuil (Alerte)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = thresholdError,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_threshold_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Image URL option
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text(t("img_url_label")) },
                placeholder = { Text("https://example.com/photo.png") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_image_url_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Simulated Camera and Gallery selections
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Sary / Photo :",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Simulate quick camera snapshot by inserting a beautiful placeholder image URL
                                imageUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=500"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Caméra", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                // Simulate image gallery selection
                                imageUrl = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=500"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galerie", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action CTAs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cancel_product_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(t("cancel_btn"))
                }

                Button(
                    onClick = {
                        val finalName = name.trim()
                        val finalPrice = priceStr.toDoubleOrNull() ?: 0.0
                        val finalStock = stockStr.toDoubleOrNull() ?: 0.0
                        val finalThreshold = lowStockThresholdStr.toDoubleOrNull() ?: 5.0
                        val finalCategory = if (selectedCategory == "Hafa") customCategory.trim().ifEmpty { "Hafa" } else selectedCategory

                        nameError = finalName.isEmpty()
                        priceError = finalPrice <= 0.0
                        stockError = stockStr.toDoubleOrNull() == null || finalStock < 0.0
                        thresholdError = lowStockThresholdStr.toDoubleOrNull() == null || finalThreshold < 0.0

                        val finalStockQuantity = stockQuantityStr.toIntOrNull() ?: 0
                        stockQuantityError = stockQuantityStr.toIntOrNull() == null || finalStockQuantity < 0

                        val finalWholesalePrice = wholesalePriceStr.toDoubleOrNull()

                        if (!nameError && !priceError && !stockError && !thresholdError && !stockQuantityError) {
                            val saved = Product(
                                id = editingProduct?.id ?: 0,
                                name = finalName,
                                price = finalPrice,
                                category = finalCategory,
                                stock = finalStock,
                                lowStockThreshold = finalThreshold,
                                unit = selectedUnit,
                                imageUrl = imageUrl.trim(),
                                barcode = barcode.trim(),
                                wholesalePrice = finalWholesalePrice,
                                sku = sku.trim(),
                                stock_quantity = finalStockQuantity
                            )
                            onSaveProduct(saved)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_product_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(t("save_btn").substring(0, t("save_btn").length.coerceAtMost(16)), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showLiveScanner) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showLiveScanner = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                BarcodeScannerView(
                    onBarcodeScanned = { scannedCode ->
                        android.util.Log.d("AddProductScreen", "onBarcodeScanned triggered: scannedCode='$scannedCode'")
                        barcode = scannedCode
                        showLiveScanner = false
                    },
                    onClose = {
                        showLiveScanner = false
                    },
                    language = activeLang,
                    themeColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
