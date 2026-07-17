package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.BarcodeUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BarcodeListScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeLang by viewModel.language.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    
    // Get all products from database flow
    val allProducts by viewModel.allProducts.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = With barcode, 1 = Without barcode
    
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF002114)
    val cardBg = if (isDark) Color(0xFF1B4332) else Color(0xFFF8FAFC)
    val cardBorderColor = if (isDark) Color(0xFF2C5E43) else Color(0xFFE2E8F0)
    val mainTextColor = if (isDark) Color.White else Color(0xFF1E293B)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Language translations
    val screenTitle = when (activeLang) {
        "mg" -> "Fikirakirana Kaody Bar"
        "fr" -> "Gestion des codes-barres"
        else -> "Barcode Management"
    }

    val searchHint = when (activeLang) {
        "mg" -> "Hitady entana..."
        "fr" -> "Rechercher un produit..."
        else -> "Search product..."
    }

    val tabWithBarcode = when (activeLang) {
        "mg" -> "Misy Kaody"
        "fr" -> "Avec code-barres"
        else -> "With Barcode"
    }

    val tabWithoutBarcode = when (activeLang) {
        "mg" -> "Tsy misy kaody"
        "fr" -> "Sans code-barres"
        else -> "Without Barcode"
    }

    val emptyWithText = when (activeLang) {
        "mg" -> "Tsy misy entana manana kaody bar mifanaraka amin'ny fikarohana."
        "fr" -> "Aucun produit avec code-barres ne correspond à la recherche."
        else -> "No products with barcodes match your search."
    }

    val emptyWithoutText = when (activeLang) {
        "mg" -> "Voarindra daholo ny kaody bar-n'ny entana rehetra! faly tsara."
        "fr" -> "Tous les produits ont déjà un code-barres configuré !"
        else -> "All products have barcode configured successfully!"
    }

    val printAllBtn = when (activeLang) {
        "mg" -> "Hanonta Ny Rehetra (PDF)"
        "fr" -> "Tout imprimer (Feuille PDF)"
        else -> "Print All (PDF Sheet)"
    }

    val printAllSubtitle = when (activeLang) {
        "mg" -> "Hiteraka antontan-taratasy ho an'ny entana rehetra"
        "fr" -> "Générer un seul document pour tous les articles"
        else -> "Consolidate and print barcodes for all items"
    }

    val generateCodeBtn = when (activeLang) {
        "mg" -> "Hiteraka kaody"
        "fr" -> "Générer"
        else -> "Generate"
    }

    // Filter products
    val productsWithBarcode = allProducts.filter { !it.barcode.isNullOrBlank() && com.example.util.BarcodeUtil.isGeneratedBarcode(it.barcode) }
    val productsWithoutBarcode = allProducts.filter { it.barcode.isNullOrBlank() }

    val filteredList = if (selectedTab == 0) {
        productsWithBarcode.filter {
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.barcode.contains(searchQuery, ignoreCase = true)
        }.sortedWith(compareBy({ it.category.lowercase() }, { it.name.lowercase() }))
    } else {
        productsWithoutBarcode.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }.sortedWith(compareBy({ it.category.lowercase() }, { it.name.lowercase() }))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1B4332) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Hiverina",
                            tint = mainTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = mainTextColor,
                            fontSize = 20.sp
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Consolidated Document Printing Bar at top
            if (productsWithBarcode.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            val pdfFile = BarcodeUtil.generateConsolidatedBarcodePdf(context, productsWithBarcode)
                            if (pdfFile != null) {
                                BarcodeUtil.printBarcode(context, pdfFile)
                            } else {
                                val errMsg = when (activeLang) {
                                    "mg" -> "Tsy nandeha ny famoronana PDF"
                                    "fr" -> "Erreur lors de la génération du PDF"
                                    else -> "Failed to generate PDF document"
                                }
                                android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                        .testTag("print_consolidated_pdf_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, themeColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(themeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = printAllBtn,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                                Text(
                                    text = "${printAllSubtitle} (${productsWithBarcode.size} ${if (activeLang == "mg") "entana" else "articles"})",
                                    fontSize = 11.sp,
                                    color = secondaryTextColor
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Tabs for filtering (With vs Without barcodes)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = themeColor
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tabWithBarcode,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Badge(
                                containerColor = if (selectedTab == 0) themeColor else secondaryTextColor.copy(alpha = 0.2f),
                                contentColor = if (selectedTab == 0) Color.White else mainTextColor
                            ) {
                                Text(text = productsWithBarcode.size.toString(), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tabWithoutBarcode,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Badge(
                                containerColor = if (selectedTab == 1) themeColor else secondaryTextColor.copy(alpha = 0.2f),
                                contentColor = if (selectedTab == 1) Color.White else mainTextColor
                            ) {
                                Text(text = productsWithoutBarcode.size.toString(), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(searchHint, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = secondaryTextColor)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = cardBorderColor,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = mainTextColor,
                    unfocusedTextColor = mainTextColor
                )
            )

            // Barcode Grid List / Cards
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.SearchOff else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (selectedTab == 0) secondaryTextColor else themeColor,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (selectedTab == 0) emptyWithText else emptyWithoutText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = secondaryTextColor,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                val groupedList = filteredList.groupBy { it.category }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedList.forEach { (category, categoryProducts) ->
                        item(key = "header_${category}") {
                            Surface(
                                color = themeColor.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(themeColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = category.ifBlank { if (activeLang == "mg") "Tsy voasokajy" else "Non classifié" }.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColor,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                        items(categoryProducts, key = { it.id }) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("barcode_item_${product.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Product details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = mainTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${product.category} • ${product.unit}",
                                            fontSize = 11.sp,
                                            color = secondaryTextColor
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(themeColor.copy(alpha = 0.1f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = String.format("%,.0f Ar", product.price),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = themeColor
                                            )
                                        }
                                    }
                                    
                                    if (!product.barcode.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isDark) Color(0xFF002114) else Color(0xFFE2E8F0).copy(alpha = 0.5f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode,
                                                contentDescription = null,
                                                tint = themeColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = product.barcode,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = mainTextColor
                                            )
                                        }
                                    }
                                }

                                // Quick Actions
                                if (product.barcode.isNullOrBlank()) {
                                    // Generate Button
                                    Button(
                                        onClick = {
                                            val generatedBarcode = com.example.util.BarcodeUtil.generateStandardBarcode()
                                            
                                            val updatedProduct = product.copy(barcode = generatedBarcode)
                                            viewModel.saveProduct(updatedProduct)
                                            
                                            val successMsg = when (activeLang) {
                                                "mg" -> "Voaforona ny kaody bar ho an'i ${product.name}!"
                                                "fr" -> "Code-barres généré pour ${product.name} !"
                                                else -> "Barcode generated for ${product.name}!"
                                            }
                                            android.widget.Toast.makeText(context, successMsg, android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = themeColor,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = generateCodeBtn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // Individual layout actions
                                    IconButton(
                                        onClick = {
                                            val pdfFile = BarcodeUtil.generateBarcodePdf(context, product.name, product.barcode)
                                            if (pdfFile != null) {
                                                BarcodeUtil.printBarcode(context, pdfFile)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF1B4332) else Color(0xFFF1F5F9))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "Print",
                                            tint = themeColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
