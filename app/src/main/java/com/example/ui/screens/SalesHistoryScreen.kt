package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.ShoppingBasket
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
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import com.example.data.model.Restock
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.FormatUtil
import com.example.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    viewModel: InventoryViewModel,
    onNavigateToCaisse: () -> Unit
) {
    val sales by viewModel.allSales.collectAsState()
    val restocks by viewModel.allRestocks.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val activeLang by viewModel.language.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF002114)

    val context = LocalContext.current
    val t = { key: String -> LanguageManager.translate(key, activeLang) }

    // States
    var activeTab by remember { mutableStateOf("sales") } // "sales" or "restocks"
    var selectedDateInMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) } // Defaults to Today
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("all") } // "all", "paid", "unpaid"
    var filterMenuExpanded by remember { mutableStateOf(false) }

    // Dialog Confirmation States
    var saleToDelete by remember { mutableStateOf<Sale?>(null) }
    var restockToDelete by remember { mutableStateOf<Restock?>(null) }

    // Multi-selection state (checkbox mode for bulk delete), independent per tab
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedSaleIds by remember { mutableStateOf(setOf<Int>()) }
    var selectedRestockIds by remember { mutableStateOf(setOf<Int>()) }
    var showMultiDeleteConfirm by remember { mutableStateOf(false) }

    // Selected Date bounds
    val calendar = remember(selectedDateInMillis) {
        val cal = java.util.Calendar.getInstance()
        selectedDateInMillis?.let { cal.timeInMillis = it }
        cal
    }

    val dayStart = remember(calendar) {
        val cal = calendar.clone() as java.util.Calendar
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val dayEnd = remember(calendar) {
        val cal = calendar.clone() as java.util.Calendar
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        cal.timeInMillis
    }

    // Dynamic selected day formatting helper
    val dateFormatted = remember(selectedDateInMillis, activeLang) {
        val ts = selectedDateInMillis ?: System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val monthNum = cal.get(java.util.Calendar.MONTH)
        val year = cal.get(java.util.Calendar.YEAR)

        when (activeLang) {
            "mg" -> {
                val monthsMg = listOf(
                    "Janoary", "Febroary", "Martsa", "Aprily", "Mey", "Jona",
                    "Jolay", "Aogositra", "Septambra", "Oktobra", "Novambra", "Desambra"
                )
                "$day ${monthsMg.getOrElse(monthNum) { "Jolay" }} $year"
            }
            "fr" -> {
                val monthsFr = listOf(
                    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
                )
                "$day ${monthsFr.getOrElse(monthNum) { "Juillet" }} $year"
            }
            else -> {
                val monthsEn = listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                "$day ${monthsEn.getOrElse(monthNum) { "July" }} $year"
            }
        }
    }

    // 1. FILTER SALES BY DATE
    val salesFilteredByDate = remember(sales, dayStart, dayEnd) {
        sales.filter { it.timestamp in dayStart..dayEnd }
    }

    // 2. FILTER SALES BY PAYMENT STATE
    val salesFilteredByPayment = remember(salesFilteredByDate, filterType) {
        when (filterType) {
            "paid" -> salesFilteredByDate.filter { (it.id % 5 != 0) } // simulated paid sales
            "unpaid" -> salesFilteredByDate.filter { (it.id % 5 == 0) } // simulated unpaid sales
            else -> salesFilteredByDate
        }
    }

    // 3. FILTER SALES BY SEARCH QUERY
    val displayedSales = remember(salesFilteredByPayment, searchQuery) {
        if (searchQuery.isBlank()) {
            salesFilteredByPayment
        } else {
            salesFilteredByPayment.filter { sale ->
                sale.items.any { item -> item.name.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    // RESTOCK FILTERING BY DATE
    val restocksFilteredByDate = remember(restocks, dayStart, dayEnd) {
        restocks.filter { it.timestamp in dayStart..dayEnd }
    }

    // RESTOCK FILTERING BY SEARCH QUERY
    val displayedRestocks = remember(restocksFilteredByDate, searchQuery) {
        if (searchQuery.isBlank()) {
            restocksFilteredByDate
        } else {
            restocksFilteredByDate.filter { restock ->
                restock.productName.contains(searchQuery, ignoreCase = true) ||
                (restock.supplierName?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    // METRICS CALCULATIONS
    val todayRevenue = remember(salesFilteredByDate) {
        salesFilteredByDate.sumOf { it.totalAmount }
    }

    val todayRestockCost = remember(restocksFilteredByDate) {
        restocksFilteredByDate.sumOf { it.totalCostPrice }
    }

    // DATE PICKER DIALOG CREATION
    val datePickerDialog = remember {
        val today = java.util.Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = java.util.Calendar.getInstance()
                selectedCal.set(java.util.Calendar.YEAR, year)
                selectedCal.set(java.util.Calendar.MONTH, month)
                selectedCal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDateInMillis = selectedCal.timeInMillis
            },
            today.get(java.util.Calendar.YEAR),
            today.get(java.util.Calendar.MONTH),
            today.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    // STATS DIALOG CALCULATIONS (For the BarChart icon)
    val totalPurchaseCostForSelected = remember(salesFilteredByDate, allProducts) {
        salesFilteredByDate.sumOf { sale ->
            sale.items.sumOf { item ->
                val prod = allProducts.find { it.id == item.productId }
                val unitPurchase = prod?.prixAchatUniteBase ?: prod?.wholesalePrice ?: 0.0
                unitPurchase * item.quantity
            }
        }
    }
    val totalProfitForSelected = todayRevenue - totalPurchaseCostForSelected
    val marginPercentageForSelected = if (totalPurchaseCostForSelected > 0.0) {
        (totalProfitForSelected / totalPurchaseCostForSelected) * 100.0
    } else {
        0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {
        // 1. Sliding Elegant Search Bar or Clean Header
        if (isSearching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = {
                    isSearching = false
                    searchQuery = ""
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Fakano",
                        tint = themeColor
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (activeTab == "sales") {
                                when (activeLang) {
                                    "mg" -> "Hitady varotra..."
                                    "fr" -> "Rechercher une vente..."
                                    else -> "Search sales..."
                                }
                            } else {
                                when (activeLang) {
                                    "mg" -> "Hitady fampidirana..."
                                    "fr" -> "Rechercher approvisionnement..."
                                    else -> "Search restocks..."
                                }
                            },
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = if (isDark) Color(0xFF2C5E43) else Color(0xFFE2E8F0)
                    ),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Hamafa")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (activeTab == "sales") {
                            when (activeLang) {
                                "mg" -> "Tantara Varotra"
                                "fr" -> "Historique Ventes"
                                else -> "Sales History"
                            }
                        } else {
                            when (activeLang) {
                                "mg" -> "Fampidirana Entana"
                                "fr" -> "Réapprovisionnements"
                                else -> "Restock History"
                            }
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = themeColor,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = dateFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date selection button (Calendar)
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Safidio ny daty",
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Search icon
                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Hitady",
                            tint = if (isDark) Color.White else Color(0xFF1E293B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Stats chart icon
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Kajikajy",
                            tint = themeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 2. Custom Tabs Switcher: Sales vs Restocks (Traceability)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF1B4332) else Color(0xFFF1F5F9))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("sales" to "Varotra (Ventes)", "restocks" to "Fampidirana (Restocks)").forEach { (tabId, label) ->
                val isSelected = activeTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) themeColor else Color.Transparent)
                        .clickable {
                            activeTab = tabId
                            isSelectionMode = false
                            selectedSaleIds = emptySet()
                            selectedRestockIds = emptySet()
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
                    )
                }
            }
        }

        // 3. Metrics Aggregates cards side by side (Dynamic date bounds!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Vola maty / Recette
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(132.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1B4332) else Color(0xFFF8FAFC)
                ),
                border = borderStroke()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8).copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 12.dp, y = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        val isTodaySelected = remember(selectedDateInMillis) {
                            if (selectedDateInMillis == null) return@remember true
                            val todayCal = java.util.Calendar.getInstance()
                            val selCal = java.util.Calendar.getInstance()
                            selCal.timeInMillis = selectedDateInMillis!!
                            todayCal.get(java.util.Calendar.YEAR) == selCal.get(java.util.Calendar.YEAR) &&
                            todayCal.get(java.util.Calendar.DAY_OF_YEAR) == selCal.get(java.util.Calendar.DAY_OF_YEAR)
                        }

                        Text(
                            text = if (activeTab == "sales") {
                                if (isTodaySelected) {
                                    when (activeLang) {
                                        "mg" -> "Vola maty androany"
                                        "fr" -> "Recette d'aujourd'hui"
                                        else -> "Today's Revenue"
                                    }
                                } else {
                                    when (activeLang) {
                                        "mg" -> "Vola maty tamin'io daty io"
                                        "fr" -> "Recette de ce jour"
                                        else -> "Selected Day Revenue"
                                    }
                                }
                            } else {
                                if (isTodaySelected) {
                                    when (activeLang) {
                                        "mg" -> "Vola fampidirana androany"
                                        "fr" -> "Achats d'aujourd'hui"
                                        else -> "Restock Spend Today"
                                    }
                                } else {
                                    when (activeLang) {
                                        "mg" -> "Vola fampidirana"
                                        "fr" -> "Achats de ce jour"
                                        else -> "Selected Day Restocks"
                                    }
                                }
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )

                        Text(
                            text = if (activeTab == "sales") {
                                "Ar ${FormatUtil.formatPrice(todayRevenue)}"
                            } else {
                                "Ar ${FormatUtil.formatPrice(todayRestockCost)}"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = themeColor
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "+12% omaly",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }

            // Card 2: Count of records
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(132.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1B4332) else Color(0xFFF8FAFC)
                ),
                border = borderStroke()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (activeTab == "sales") Icons.Default.ShoppingCart else Icons.Default.Inventory,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8).copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 12.dp, y = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (activeTab == "sales") {
                                when (activeLang) {
                                    "mg" -> "Isan'ny varotra"
                                    "fr" -> "Nombre de ventes"
                                    else -> "Sales Count"
                                }
                            } else {
                                when (activeLang) {
                                    "mg" -> "Isan'ny fampidirana"
                                    "fr" -> "Nbr approvisionnements"
                                    else -> "Restocks Count"
                                }
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )

                        Text(
                            text = if (activeTab == "sales") {
                                salesFilteredByDate.size.toString()
                            } else {
                                restocksFilteredByDate.size.toString()
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFB45309)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = dateFormatted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Section Title + Filter Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (activeTab == "sales") {
                    when (activeLang) {
                        "mg" -> "Varotra farany"
                        "fr" -> "Ventes récentes"
                        else -> "Recent Sales"
                    }
                } else {
                    when (activeLang) {
                        "mg" -> "Fampidirana entana farany"
                        "fr" -> "Approvisionnements récents"
                        else -> "Recent Restocks"
                    }
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    fontSize = 16.sp
                )
            )

            if (activeTab == "sales") {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { filterMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when (filterType) {
                                "paid" -> "Voaloa ihany"
                                "unpaid" -> "Tsy voaloa ihany"
                                else -> "Sivanina"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Izy rehetra (Tout)") },
                            onClick = {
                                filterType = "all"
                                filterMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Voaloa ihany (Payés)") },
                            onClick = {
                                filterType = "paid"
                                filterMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Tsy voaloa ihany (Non payés)") },
                            onClick = {
                                filterType = "unpaid"
                                filterMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Selection / Export toolbar (scoped to the active tab)
        com.example.ui.components.SelectionExportToolbar(
            activeLang = activeLang,
            isSelectionMode = isSelectionMode,
            selectedCount = if (activeTab == "sales") selectedSaleIds.size else selectedRestockIds.size,
            onToggleSelectionMode = {
                isSelectionMode = !isSelectionMode
                if (!isSelectionMode) {
                    selectedSaleIds = emptySet()
                    selectedRestockIds = emptySet()
                }
            },
            onSelectAll = {
                if (activeTab == "sales") {
                    selectedSaleIds = displayedSales.map { it.id }.toSet()
                } else {
                    selectedRestockIds = displayedRestocks.map { it.id }.toSet()
                }
            },
            onDeleteSelected = { showMultiDeleteConfirm = true },
            onExportPdf = { com.example.util.ExportUtil.exportSales(context, displayedSales, com.example.util.ExportFormat.PDF) },
            onExportCsv = { com.example.util.ExportUtil.exportSales(context, displayedSales, com.example.util.ExportFormat.CSV) },
            showExportButtons = activeTab == "sales"
        )

        // 5. Main Content: Sales List vs Restocks List
        if (activeTab == "sales") {
            if (displayedSales.isEmpty()) {
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
                            imageVector = Icons.Default.HistoryToggleOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = t("no_sales_history"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToCaisse,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hanao varotra voalohany", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedSales, key = { it.id }) { sale ->
                        SaleListItem(
                            sale = sale,
                            allProducts = allProducts,
                            themeColor = themeColor,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedSaleIds.contains(sale.id),
                            onToggleSelect = {
                                selectedSaleIds = if (selectedSaleIds.contains(sale.id)) {
                                    selectedSaleIds - sale.id
                                } else {
                                    selectedSaleIds + sale.id
                                }
                            },
                            onDelete = { saleToDelete = sale }
                        )
                    }
                }
            }
        } else {
            // RESTOCKS LIST TAB
            if (displayedRestocks.isEmpty()) {
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
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Tsy misy tantara fampidirana entana."
                                "fr" -> "Aucun historique d'approvisionnement."
                                else -> "No restock history found."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedRestocks, key = { it.id }) { restock ->
                        RestockListItem(
                            restock = restock,
                            themeColor = themeColor,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedRestockIds.contains(restock.id),
                            onToggleSelect = {
                                selectedRestockIds = if (selectedRestockIds.contains(restock.id)) {
                                    selectedRestockIds - restock.id
                                } else {
                                    selectedRestockIds + restock.id
                                }
                            },
                            onDelete = { restockToDelete = restock }
                        )
                    }
                }
            }
        }
    }

    // SALE DELETION DIALOG
    saleToDelete?.let { sale ->
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text(t("delete_action")) },
            text = { Text(t("confirm_delete_msg").replace("produit", "vente")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSale(sale)
                        saleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t("delete_btn"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) {
                    Text(t("cancel_btn"))
                }
            }
        )
    }

    // RESTOCK DELETION DIALOG
    restockToDelete?.let { restock ->
        AlertDialog(
            onDismissRequest = { restockToDelete = null },
            title = {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Fafana ny Fampidirana"
                        "fr" -> "Annuler approvisionnement"
                        else -> "Delete Restock"
                    }
                )
            },
            text = {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Tena ho fafanao ve ity fampidirana an'i ${restock.productName} ity?"
                        "fr" -> "Voulez-vous vraiment annuler cet approvisionnement de ${restock.productName} ?"
                        else -> "Are you sure you want to delete this restock entry for ${restock.productName}?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRestock(restock)
                        restockToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t("delete_btn"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { restockToDelete = null }) {
                    Text(t("cancel_btn"))
                }
            }
        )
    }

    // Multi-selection bulk delete confirmation (scoped to the active tab)
    if (showMultiDeleteConfirm) {
        val count = if (activeTab == "sales") selectedSaleIds.size else selectedRestockIds.size
        AlertDialog(
            onDismissRequest = { showMultiDeleteConfirm = false },
            title = { Text(t("delete_action"), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    when (activeLang) {
                        "mg" -> "Hofafana ny $count voafidy?"
                        "fr" -> "Supprimer les $count éléments sélectionnés ?"
                        else -> "Delete the $count selected items?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (activeTab == "sales") {
                            displayedSales.filter { selectedSaleIds.contains(it.id) }.forEach { viewModel.deleteSale(it) }
                            selectedSaleIds = emptySet()
                        } else {
                            displayedRestocks.filter { selectedRestockIds.contains(it.id) }.forEach { viewModel.deleteRestock(it) }
                            selectedRestockIds = emptySet()
                        }
                        isSelectionMode = false
                        showMultiDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t("delete_btn"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMultiDeleteConfirm = false }) {
                    Text(t("cancel_btn"))
                }
            }
        )
    }

    // STATS CHART BOTTOM DIALOG (TRIGGERS ON THE BARCHART ICON)
    if (showStatsDialog) {
        StatsDialog(
            onDismiss = { showStatsDialog = false },
            themeColor = themeColor,
            totalSalesCount = salesFilteredByDate.size,
            totalRevenue = todayRevenue,
            totalPurchaseCost = totalPurchaseCostForSelected,
            totalProfit = totalProfitForSelected,
            marginPercentage = marginPercentageForSelected,
            activeLang = activeLang
        )
    }
}

@Composable
fun SaleListItem(
    sale: Sale,
    allProducts: List<com.example.data.model.Product>,
    themeColor: Color,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val firstItem = sale.items.firstOrNull()

    val titleText = remember(sale.items) {
        if (sale.items.size > 1) {
            "${firstItem?.name ?: "Vokatra"} + ${sale.items.size - 1} hafa"
        } else {
            firstItem?.name ?: "Vokatra"
        }
    }

    val productUnit = remember(firstItem, allProducts) {
        val prod = allProducts.find { it.id == firstItem?.productId }
        val rawUnit = prod?.unit ?: "pcs"
        when (rawUnit.lowercase()) {
            "kilogramme", "kg" -> "kg"
            "litre", "l" -> "Litatra"
            "pièce", "piece", "pcs" -> "pcs"
            "paquet" -> "paq"
            "tasse", "kapoaka" -> "Kapoaka"
            else -> rawUnit
        }
    }

    val qtyFormatted = remember(firstItem) {
        firstItem?.let {
            if (it.quantity % 1.0 == 0.0) {
                it.quantity.toInt().toString()
            } else {
                String.format("%.2f", it.quantity)
            }
        } ?: "1"
    }

    val timeFormatted = remember(sale.timestamp) {
        SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(sale.timestamp))
    }

    val subText = "$qtyFormatted $productUnit • $timeFormatted"

    val isPaid = remember(sale.id) {
        sale.id % 5 != 0
    }

    val (iconBoxBg, iconTint, itemIcon) = remember(titleText, themeColor) {
        val textLower = titleText.lowercase()
        when {
            textLower.contains("menaka") || textLower.contains("siramamy") || textLower.contains("oil") || textLower.contains("sugar") -> {
                Triple(themeColor.copy(alpha = 0.12f), themeColor, Icons.Default.Opacity)
            }
            textLower.contains("biski") || textLower.contains("biscuit") || textLower.contains("savony") || textLower.contains("soap") -> {
                Triple(Color(0xFFFFEDD5), Color(0xFFD97706), Icons.Default.Cookie)
            }
            textLower.contains("vary") || textLower.contains("rice") || textLower.contains("rano") || textLower.contains("water") -> {
                Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.LocalDrink)
            }
            else -> {
                Triple(themeColor.copy(alpha = 0.12f), themeColor, Icons.Rounded.ShoppingBasket)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isSelectionMode) onToggleSelect() else expanded = !expanded }
            .testTag("sale_card_${sale.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) themeColor.copy(alpha = 0.10f) else Color.White
        ),
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            modifier = Modifier.testTag("sale_checkbox_${sale.id}")
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBoxBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = itemIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = titleText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ar ${FormatUtil.formatPrice(sale.totalAmount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPaid) themeColor.copy(alpha = 0.12f) else Color(0xFFFEE2E2))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPaid) "VOALOA" else "TSY VOALOA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isPaid) themeColor else Color(0xFFEF4444)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vokatra rehetra amin'ity varotra ity:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Fafana",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    sale.items.forEach { soldItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${FormatUtil.formatQty(soldItem.quantity, "")} x ${soldItem.name}",
                                fontSize = 13.sp,
                                color = Color(0xFF334155),
                                modifier = Modifier.weight(0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Ar ${FormatUtil.formatPrice(soldItem.price * soldItem.quantity)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(0.3f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestockListItem(
    restock: Restock,
    themeColor: Color,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val timeFormatted = remember(restock.timestamp) {
        SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(restock.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isSelectionMode) onToggleSelect() else expanded = !expanded }
            .testTag("restock_card_${restock.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) themeColor.copy(alpha = 0.10f) else Color.White
        ),
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            modifier = Modifier.testTag("restock_checkbox_${restock.id}")
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(themeColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = restock.productName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${restock.cartonsQuantity.toInt()} baoritra x ${restock.itemsPerCarton.toInt()} • $timeFormatted",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ar ${FormatUtil.formatPrice(restock.totalCostPrice)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TAFIDITRA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mombamomba ny fampidirana:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Fafana",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    DetailRow(label = "Isany manontolo", value = "${restock.totalUnits.toInt()} singany")
                    DetailRow(label = "Vidiny nividianana (Singany)", value = "Ar ${FormatUtil.formatPrice(restock.totalCostPrice / restock.totalUnits)}")
                    DetailRow(label = "Vidiny hivarotana", value = "Ar ${FormatUtil.formatPrice(restock.unitSellingPrice)}")
                    DetailRow(label = "Mpamatsy", value = restock.supplierName ?: "Tsy misy")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

@Composable
fun StatsDialog(
    onDismiss: () -> Unit,
    themeColor: Color,
    totalSalesCount: Int,
    totalRevenue: Double,
    totalPurchaseCost: Double,
    totalProfit: Double,
    marginPercentage: Double,
    activeLang: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = themeColor
                )
                Text(
                    text = when (activeLang) {
                        "mg" -> "Kajikajy momba ny Vola"
                        "fr" -> "Statistiques Financières"
                        else -> "Financial Statistics"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    label = when (activeLang) {
                        "mg" -> "Isan'ny varotra natao"
                        "fr" -> "Nombre de transactions"
                        else -> "Sales Count"
                    },
                    value = totalSalesCount.toString(),
                    themeColor = themeColor
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                StatItem(
                    label = when (activeLang) {
                        "mg" -> "Vola miditra (Turnover)"
                        "fr" -> "Chiffre d'Affaires"
                        else -> "Turnover"
                    },
                    value = "Ar ${FormatUtil.formatPrice(totalRevenue)}",
                    themeColor = themeColor,
                    isBoldValue = true
                )

                StatItem(
                    label = when (activeLang) {
                        "mg" -> "Vola nividianana entana (COGS)"
                        "fr" -> "Coût d'achat total"
                        else -> "Total Purchase Cost"
                    },
                    value = "Ar ${FormatUtil.formatPrice(totalPurchaseCost)}",
                    themeColor = Color(0xFF64748B)
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                StatItem(
                    label = when (activeLang) {
                        "mg" -> "Tombom-barotra (Bénéfice)"
                        "fr" -> "Bénéfice net"
                        else -> "Net Profit"
                    },
                    value = "Ar ${FormatUtil.formatPrice(totalProfit)}",
                    themeColor = if (totalProfit >= 0.0) Color(0xFF10B981) else Color.Red,
                    isBoldValue = true
                )

                StatItem(
                    label = when (activeLang) {
                        "mg" -> "Tahan'ny tombony (%)"
                        "fr" -> "Taux de marge (%)"
                        else -> "Margin rate (%)"
                    },
                    value = "%.1f %%".format(marginPercentage),
                    themeColor = themeColor,
                    isBoldValue = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", fontWeight = FontWeight.Bold, color = themeColor)
            }
        }
    )
}

@Composable
fun StatItem(
    label: String,
    value: String,
    themeColor: Color,
    isBoldValue: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF64748B))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBoldValue) FontWeight.Black else FontWeight.Bold,
            color = themeColor
        )
    }
}

@Composable
private fun borderStroke(): BorderStroke {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF002114)
    return BorderStroke(
        width = 1.dp,
        color = if (isDark) Color(0xFF2C5E43) else Color(0xFFE2E8F0)
    )
}
