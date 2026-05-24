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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    editingProduct: Product?,
    onSaveProduct: (Product) -> Unit,
    onCancel: () -> Unit
) {
    // State holders
    var name by remember(editingProduct) { mutableStateOf(editingProduct?.name ?: "") }
    var priceStr by remember(editingProduct) { mutableStateOf(editingProduct?.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var stockStr by remember(editingProduct) { mutableStateOf(editingProduct?.stock?.toString() ?: "") }
    var imageUrl by remember(editingProduct) { mutableStateOf(editingProduct?.imageUrl ?: "") }

    // Dropdown Categories
    val standardCategories = listOf("Sakafo", "Legioma", "Voankazo", "Zava-pisotro", "Fitaovana", "Hafa")
    var selectedCategory by remember(editingProduct) {
        mutableStateOf(editingProduct?.category ?: "Sakafo")
    }
    var customCategory by remember(editingProduct) {
        val initialCustom = if (editingProduct != null && !standardCategories.contains(editingProduct.category)) {
            editingProduct.category
        } else ""
        mutableStateOf(initialCustom)
    }

    var showDropdown by remember { mutableStateOf(false) }

    // Form errors
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var stockError by remember { mutableStateOf(false) }

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
                .padding(bottom = 96.dp), // Avoid bottom navigation overlap
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = if (isEditing) "Hanova ny Entana" else "Manampy Entana Vaovao",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = it.isBlank()
                },
                label = { Text("Anaran'ny entana") },
                isError = nameError,
                supportingText = { if (nameError) Text("Tsy maintsy fenoina ny anarana", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_name_input"),
                shape = RoundedCornerShape(16.dp)
            )

            // Category Selection Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = showDropdown,
                    onExpandedChange = { showDropdown = !showDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Karazany (Sokajy)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .testTag("product_category_select"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        standardCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Custom category input if selectedCategory is "Hafa (Other)"
            if (selectedCategory == "Hafa") {
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    label = { Text("Famaritana ny sokajy hafa") },
                    placeholder = { Text("Ex: Fitafiana, Boky, ...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_custom_category_input"),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Price Input
            OutlinedTextField(
                value = priceStr,
                onValueChange = {
                    priceStr = it
                    priceError = it.toDoubleOrNull() == null || it.toDouble() <= 0
                },
                label = { Text("Vidiny amin'ny Ariary (Ar)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = priceError,
                supportingText = { if (priceError) Text("Tsy maintsy isa mbola lehibe noho ny aotra", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_price_input"),
                shape = RoundedCornerShape(16.dp)
            )

            // Stock Input
            OutlinedTextField(
                value = stockStr,
                onValueChange = {
                    stockStr = it
                    stockError = it.toIntOrNull() == null || it.toInt() < 0
                },
                label = { Text("Isan'ny entana ao amin'ny tahiry (Stock)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = stockError,
                supportingText = { if (stockError) Text("Tsy maintsy isa tsy latsaky ny aotra", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_stock_input"),
                shape = RoundedCornerShape(16.dp)
            )

            // Image URL (optional)
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("Sarin'ny entana (rohy / URL) - azo latsaka") },
                placeholder = { Text("https://example.com/sary.jpg") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_image_url_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cancel
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_product_button"),
                     shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Hanafo")
                }

                // Save
                Button(
                    onClick = {
                        val finalName = name.trim()
                        val finalPrice = priceStr.toDoubleOrNull() ?: 0.0
                        val finalStock = stockStr.toIntOrNull() ?: 0
                        val finalCategory = if (selectedCategory == "Hafa") customCategory.trim().ifEmpty { "Hafa" } else selectedCategory

                        // Validations
                        nameError = finalName.isEmpty()
                        priceError = finalPrice <= 0
                        stockError = stockStr.toIntOrNull() == null || finalStock < 0

                        if (!nameError && !priceError && !stockError) {
                            val product = Product(
                                id = editingProduct?.id ?: 0,
                                name = finalName,
                                price = finalPrice,
                                category = finalCategory,
                                stock = finalStock,
                                imageUrl = imageUrl.trim()
                            )
                            onSaveProduct(product)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_product_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isEditing) "Hanova" else "Tehirizina")
                }
            }
        }
    }
}
