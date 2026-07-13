package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
import com.example.ui.components.BarcodeScannerView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Fournisseur
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.LanguageManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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

    // Suppliers (Fournisseurs) state
    val suppliers by viewModel.allFournisseurs.collectAsState()
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var newSupplierContact by remember { mutableStateOf("") }
    var newSupplierAddress by remember { mutableStateOf("") }

    // Core identification state
    var name by remember(editingProduct) { mutableStateOf(editingProduct?.name ?: "") }
    var nomCourt by remember(editingProduct) { mutableStateOf(editingProduct?.nomCourt ?: "") }
    var sousCategorie by remember(editingProduct) { mutableStateOf(editingProduct?.sousCategorie ?: "") }
    var marque by remember(editingProduct) { mutableStateOf(editingProduct?.marque ?: "") }
    var description by remember(editingProduct) { mutableStateOf(editingProduct?.description ?: "") }

    // Stock & Pricing state
    var priceStr by remember(editingProduct) { mutableStateOf(editingProduct?.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var stockStr by remember(editingProduct) { mutableStateOf(editingProduct?.stock?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var lowStockThresholdStr by remember(editingProduct) { mutableStateOf(editingProduct?.lowStockThreshold?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "5") }
    var stockMaxStr by remember(editingProduct) { mutableStateOf(editingProduct?.stockMax?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var emplacement by remember(editingProduct) { mutableStateOf(editingProduct?.emplacement ?: "") }
    var prixAchatUniteBaseStr by remember(editingProduct) { mutableStateOf(editingProduct?.prixAchatUniteBase?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var wholesalePriceStr by remember(editingProduct) { mutableStateOf(editingProduct?.wholesalePrice?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }

    // Suppliers association state
    var selectedFournisseurId by remember(editingProduct) { mutableStateOf(editingProduct?.fournisseurId) }
    var showSupplierDropdown by remember { mutableStateOf(false) }

    // Traceability & Fiscal state
    var barcode by remember(editingProduct) { mutableStateOf(editingProduct?.barcode ?: "") }
    var sku by remember(editingProduct) { mutableStateOf(editingProduct?.sku ?: "") }
    var gerePeremption by remember(editingProduct) { mutableStateOf(editingProduct?.gerePeremption ?: false) }
    var taxable by remember(editingProduct) { mutableStateOf(editingProduct?.taxable ?: false) }
    var tauxTaxeStr by remember(editingProduct) { mutableStateOf(editingProduct?.tauxTaxe?.toString() ?: "20.0") }
    var imageUrl by remember(editingProduct) { mutableStateOf(editingProduct?.imageUrl ?: "") }

    // UI State flags
    var showLiveScanner by remember { mutableStateOf(false) }
    var wholesalePriceError by remember { mutableStateOf(false) }
    var stockQuantityStr by remember(editingProduct) { mutableStateOf(editingProduct?.stock_quantity?.toString() ?: "0") }
    var stockQuantityError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val localPath = saveBitmapToLocalFile(context, bitmap)
            if (localPath != null) {
                imageUrl = "file://$localPath"
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val localPath = saveUriToLocalFile(context, uri)
            if (localPath != null) {
                imageUrl = "file://$localPath"
            }
        }
    }

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
    var prixAchatError by remember { mutableStateOf(false) }
    var stockMaxError by remember { mutableStateOf(false) }
    var tauxTaxeError by remember { mutableStateOf(false) }

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

            // Section 1: Core Identification
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = when(activeLang) {
                                "mg" -> "Fampahafantarana ny entana"
                                "fr" -> "Fiche produit centrale"
                                else -> "Core Identification"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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

                    // Short Name
                    OutlinedTextField(
                        value = nomCourt,
                        onValueChange = { nomCourt = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Anarana fohy (ho an'ny tapakila)"
                                "fr" -> "Nom court (pour ticket)"
                                else -> "Short name (for receipts)"
                            }
                        ) },
                        supportingText = { Text("Ex: Vary gasy") },
                        modifier = Modifier.fillMaxWidth(),
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

                    // Sub Category
                    OutlinedTextField(
                        value = sousCategorie,
                        onValueChange = { sousCategorie = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Sokajy manaraka (Sous-catégorie)"
                                "fr" -> "Sous-catégorie"
                                else -> "Sub-category"
                            }
                        ) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Brand (Marque)
                    OutlinedTextField(
                        value = marque,
                        onValueChange = { marque = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Marika (Marque)"
                                "fr" -> "Marque"
                                else -> "Brand"
                            }
                        ) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }
            }

            // Section 2: Pricing & Stock (Tahiry & Vidiny)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = when(activeLang) {
                                "mg" -> "Vidiny, Refy & Tahiry"
                                "fr" -> "Tarification, Unités & Stock"
                                else -> "Pricing, Units & Stock"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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

                    // Price Purchase input (Prix d'achat unité base)
                    OutlinedTextField(
                        value = prixAchatUniteBaseStr,
                        onValueChange = {
                            prixAchatUniteBaseStr = it
                            prixAchatError = it.isNotEmpty() && (it.toDoubleOrNull() == null || it.toDouble() < 0.0)
                        },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Vidiny nividianana (Prix d'achat)"
                                "fr" -> "Prix d'achat de référence"
                                else -> "Reference Purchase Price"
                            }
                        ) },
                        prefix = { Text("Ar ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = prixAchatError,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Price Selling input
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
                    OutlinedTextField(
                        value = wholesalePriceStr,
                        onValueChange = {
                            wholesalePriceStr = it
                            wholesalePriceError = it.isNotEmpty() && (it.toDoubleOrNull() == null || it.toDouble() < 0)
                        },
                        label = { Text(
                            when (activeLang) {
                                "mg" -> "Vidiny Ambongadiny (Wholesale Price)"
                                "fr" -> "Prix de gros"
                                else -> "Wholesale Price"
                            }
                        ) },
                        prefix = { Text("Ar ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = wholesalePriceError,
                        supportingText = { Text("Ampiasaina amin'ny fomba 'Grossiste'") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_wholesale_price_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Initial Stock Input
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = {
                            stockStr = it
                            stockError = it.toDoubleOrNull() == null || it.toDouble() < 0
                            // Auto sync integer field
                            it.toDoubleOrNull()?.let { d ->
                                stockQuantityStr = d.toInt().toString()
                            }
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

                    // Stock Quantity field (Integer)
                    OutlinedTextField(
                        value = stockQuantityStr,
                        onValueChange = {
                            stockQuantityStr = it
                            stockQuantityError = it.toIntOrNull() == null || it.toInt() < 0
                        },
                        label = { Text(
                            when (activeLang) {
                                "mg" -> "Isan'ny Tahiry (Stock Quantity - Int)"
                                "fr" -> "Quantité de stock (Entier)"
                                else -> "Stock Quantity (Integer)"
                            }
                        ) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stockQuantityError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_stock_quantity_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Stock Max
                    OutlinedTextField(
                        value = stockMaxStr,
                        onValueChange = {
                            stockMaxStr = it
                            stockMaxError = it.isNotEmpty() && (it.toDoubleOrNull() == null || it.toDouble() < 0.0)
                        },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Tahiry farany ambony (Stock Maximum)"
                                "fr" -> "Stock Maximum"
                                else -> "Maximum Stock Limit"
                            }
                        ) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stockMaxError,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Emplacement (shelf location)
                    OutlinedTextField(
                        value = emplacement,
                        onValueChange = { emplacement = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Toerana ao amin'ny fivarotana (Emplacement)"
                                "fr" -> "Emplacement en rayon"
                                else -> "Shelf Location"
                            }
                        ) },
                        placeholder = { Text("Rayon A-1") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

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
                }
            }

            // Section 3: Traceability & Fiscality
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = when(activeLang) {
                                "mg" -> "Fitsipika & Fitsirihana"
                                "fr" -> "Traçabilité & Fiscalité"
                                else -> "Traceability & Fiscal"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                                IconButton(onClick = { showLiveScanner = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Scan Barcode",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
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
                        "mg" -> "Kaody SKU"
                        "fr" -> "Code SKU"
                        else -> "SKU Code"
                    }
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text(skuLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Expiry tracking toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Araho maso ny fahasimbana"
                                    "fr" -> "Gérer la péremption (FIFO)"
                                    else -> "Manage expiration dates (FIFO)"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Suivi dates de péremption"
                                    "fr" -> "Active le suivi par lot et date"
                                    else -> "Enables batch & expiry tracking"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = gerePeremption,
                            onCheckedChange = { gerePeremption = it }
                        )
                    }

                    // Taxable Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Misy hetra (Produit taxable)"
                                    "fr" -> "Produit taxable"
                                    else -> "Taxable item"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Hampiharana tahan'ny hetra"
                                    "fr" -> "Permet d'appliquer une taxe"
                                    else -> "Enables applying product taxes"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = taxable,
                            onCheckedChange = { taxable = it }
                        )
                    }

                    if (taxable) {
                        OutlinedTextField(
                            value = tauxTaxeStr,
                            onValueChange = {
                                tauxTaxeStr = it
                                tauxTaxeError = it.toDoubleOrNull() == null || it.toDouble() < 0.0
                            },
                            label = { Text(
                                when(activeLang) {
                                    "mg" -> "Tahan'ny hetra (%)"
                                    "fr" -> "Taux de taxe (%)"
                                    else -> "Tax Rate (%)"
                                }
                            ) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = tauxTaxeError,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

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

                    // Camera / Gallery selections
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Fakan-tsary"
                                    "fr" -> "Caméra"
                                    else -> "Camera"
                                },
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Sary ato"
                                    "fr" -> "Galerie"
                                    else -> "Gallery"
                                },
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Section 4: Fournisseur Principal
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Mpanome Entana (Fournisseur)"
                                    "fr" -> "Fournisseur principal"
                                    else -> "Main Supplier"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = { showAddSupplierDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Ampina"
                                    "fr" -> "Ajouter"
                                    else -> "Add"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Supplier dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = showSupplierDropdown,
                            onExpandedChange = { showSupplierDropdown = !showSupplierDropdown }
                        ) {
                            val selectedSupplierName = suppliers.find { it.id == selectedFournisseurId }?.nom ?: when(activeLang) {
                                "mg" -> "Mbola tsy misy (Aucun)"
                                "fr" -> "Aucun fournisseur sélectionné"
                                else -> "No supplier selected"
                            }
                            OutlinedTextField(
                                value = selectedSupplierName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(
                                    when(activeLang) {
                                        "mg" -> "Mpanome entana"
                                        "fr" -> "Fournisseur"
                                        else -> "Supplier"
                                    }
                                ) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSupplierDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showSupplierDropdown,
                                onDismissRequest = { showSupplierDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(
                                        when(activeLang) {
                                            "mg" -> "Tsy misy (Aucun)"
                                            "fr" -> "Aucun"
                                            else -> "None"
                                        }
                                    ) },
                                    onClick = {
                                        selectedFournisseurId = null
                                        showSupplierDropdown = false
                                    }
                                )
                                suppliers.forEach { supplier ->
                                    DropdownMenuItem(
                                        text = { Text("${supplier.nom} (${supplier.contact ?: ""})") },
                                        onClick = {
                                            selectedFournisseurId = supplier.id
                                            showSupplierDropdown = false
                                        }
                                    )
                                }
                            }
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
                        val finalPrixAchatUniteBase = prixAchatUniteBaseStr.toDoubleOrNull() ?: 0.0
                        val finalStockMax = stockMaxStr.toDoubleOrNull()
                        val finalTauxTaxe = tauxTaxeStr.toDoubleOrNull() ?: 0.0

                        prixAchatError = prixAchatUniteBaseStr.isNotEmpty() && (prixAchatUniteBaseStr.toDoubleOrNull() == null || prixAchatUniteBaseStr.toDouble() < 0.0)
                        stockMaxError = stockMaxStr.isNotEmpty() && (stockMaxStr.toDoubleOrNull() == null || stockMaxStr.toDouble() < 0.0)
                        tauxTaxeError = taxable && (tauxTaxeStr.toDoubleOrNull() == null || tauxTaxeStr.toDouble() < 0.0)

                        if (!nameError && !priceError && !stockError && !thresholdError && !stockQuantityError && !prixAchatError && !stockMaxError && !tauxTaxeError) {
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
                                stock_quantity = finalStockQuantity,
                                nomCourt = nomCourt.trim().ifEmpty { null },
                                sousCategorie = sousCategorie.trim().ifEmpty { null },
                                marque = marque.trim().ifEmpty { null },
                                description = description.trim().ifEmpty { null },
                                stockMax = finalStockMax,
                                emplacement = emplacement.trim().ifEmpty { null },
                                fournisseurId = selectedFournisseurId,
                                gerePeremption = gerePeremption,
                                taxable = taxable,
                                tauxTaxe = finalTauxTaxe,
                                prixAchatUniteBase = finalPrixAchatUniteBase
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

    // New Supplier popup Dialog
    if (showAddSupplierDialog) {
        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = {
                Text(
                    text = when(activeLang) {
                        "mg" -> "Mpanome Entana Vaovao"
                        "fr" -> "Nouveau Fournisseur"
                        else -> "New Supplier"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSupplierName,
                        onValueChange = { newSupplierName = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Anarana"
                                "fr" -> "Nom"
                                else -> "Name"
                            }
                        ) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSupplierContact,
                        onValueChange = { newSupplierContact = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Fifandraisana (Phone)"
                                "fr" -> "Contact (Tél)"
                                else -> "Contact (Phone)"
                            }
                        ) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSupplierAddress,
                        onValueChange = { newSupplierAddress = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Adiresy"
                                "fr" -> "Adresse"
                                else -> "Address"
                            }
                        ) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSupplierName.isNotBlank()) {
                            val f = Fournisseur(
                                nom = newSupplierName.trim(),
                                contact = newSupplierContact.trim().ifEmpty { null },
                                adresse = newSupplierAddress.trim().ifEmpty { null },
                                actif = true
                            )
                            viewModel.saveFournisseur(f)
                            newSupplierName = ""
                            newSupplierContact = ""
                            newSupplierAddress = ""
                            showAddSupplierDialog = false
                        }
                    }
                ) {
                    Text(
                        when(activeLang) {
                            "mg" -> "Hitehirizana"
                            "fr" -> "Enregistrer"
                            else -> "Save"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) {
                    Text(t("cancel_btn"))
                }
            }
        )
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

fun saveBitmapToLocalFile(context: android.content.Context, bitmap: Bitmap): String? {
    return try {
        val fileName = "product_img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveUriToLocalFile(context: android.content.Context, uri: Uri): String? {
    return try {
        val fileName = "product_img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

