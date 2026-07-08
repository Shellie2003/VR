package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
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
    val units = listOf("Pièce", "Litre", "Kilogramme", "Paquet", "Tasse/Kapoaka")
    var selectedUnit by remember(editingProduct) {
        mutableStateOf(editingProduct?.unit ?: "Pièce")
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
                    onValueChange = { customCategory = it },
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
                                            "Tasse/Kapoaka" -> t("unit_kapoaka")
                                            else -> u
                                        }
                                    )
                                },
                                onClick = {
                                    selectedUnit = u
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

            // Initial Stock Input (Double for decimal/deciliter/kilograms bulk)
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

                        if (!nameError && !priceError && !stockError && !thresholdError) {
                            val saved = Product(
                                id = editingProduct?.id ?: 0,
                                name = finalName,
                                price = finalPrice,
                                category = finalCategory,
                                stock = finalStock,
                                lowStockThreshold = finalThreshold,
                                unit = selectedUnit,
                                imageUrl = imageUrl.trim()
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
}
