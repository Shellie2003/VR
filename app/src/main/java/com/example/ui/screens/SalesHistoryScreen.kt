package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Sale
import com.example.data.model.SoldItem
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
    val allProducts by viewModel.allProducts.collectAsState()
    val activeLang by viewModel.language.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()

    // Translate helper
    val t = { key: String -> LanguageManager.translate(key, activeLang) }

    // State for filtering
    var filterType by remember { mutableStateOf("all") } // "all", "paid", "unpaid"
    var filterMenuExpanded by remember { mutableStateOf(false) }

    // Calculate today's bounds
    val todayStart = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    // Filter sales for today
    val todaySales = remember(sales, todayStart) {
        sales.filter { it.timestamp >= todayStart }
    }
    val todayRevenue = remember(todaySales) {
        todaySales.sumOf { it.totalAmount }
    }
    val todayCount = todaySales.size

    // Formatted date in Malagasy
    val todayFormatted = remember {
        val cal = java.util.Calendar.getInstance()
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val monthNum = cal.get(java.util.Calendar.MONTH)
        val year = cal.get(java.util.Calendar.YEAR)
        val monthsMg = listOf(
            "Janoary", "Febroary", "Martsa", "Aprily", "Mey", "Jona",
            "Jolay", "Aogositra", "Septambra", "Oktobra", "Novambra", "Desambra"
        )
        "$day ${monthsMg.getOrElse(monthNum) { "Jolay" }} $year"
    }

    // Filtered list to display in the main LazyColumn
    val displayedSales = remember(sales, filterType) {
        when (filterType) {
            "paid" -> sales.filter { (it.id % 5 != 0) } // simulated paid sales
            "unpaid" -> sales.filter { (it.id % 5 == 0) } // simulated unpaid sales
            else -> sales
        }
    }

    var saleToDelete by remember { mutableStateOf<Sale?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {
        // 1. Custom Header matching image 100%
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tantara",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = themeColor, // Premium dynamic theme color
                    fontSize = 22.sp
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 2. Metrics aggregates cards side by side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Vatiny androany
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(132.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8FAFC)
                ),
                border = borderStroke()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Watermark
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
                        Text(
                            text = "Vatiny androany",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Ar ${FormatUtil.formatPrice(todayRevenue)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = themeColor
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
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

            // Card 2: Isan'ny varotra
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(132.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8FAFC)
                ),
                border = borderStroke()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Watermark
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
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
                            text = "Isan'ny varotra",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = todayCount.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFB45309) // Bold orange/brown
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
                                text = todayFormatted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Section Title + Filter Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Varotra farany",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    fontSize = 18.sp
                )
            )

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

        // 4. List View or Empty State
        if (displayedSales.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
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
                items(displayedSales) { sale ->
                    SaleListItem(
                        sale = sale,
                        allProducts = allProducts,
                        themeColor = themeColor,
                        onDelete = { saleToDelete = sale }
                    )
                }
            }
        }
    }

    // Deletion warning confirmation
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
}

@Composable
fun SaleListItem(
    sale: Sale,
    allProducts: List<com.example.data.model.Product>,
    themeColor: Color,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val firstItem = sale.items.firstOrNull()

    // Title parsing
    val titleText = remember(sale.items) {
        if (sale.items.size > 1) {
            "${firstItem?.name ?: "Vokatra"} + ${sale.items.size - 1} hafa"
        } else {
            firstItem?.name ?: "Vokatra"
        }
    }

    // Match product details for unit
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

    // Quantity format
    val qtyFormatted = remember(firstItem) {
        firstItem?.let {
            if (it.quantity % 1.0 == 0.0) {
                it.quantity.toInt().toString()
            } else {
                String.format("%.2f", it.quantity)
            }
        } ?: "1"
    }

    // Timestamp formatting
    val timeFormatted = remember(sale.timestamp) {
        SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(sale.timestamp))
    }

    // Subtext
    val subText = "$qtyFormatted $productUnit • $timeFormatted"

    // Simulate Payment Status (e.g. sale.id % 5 == 0 is unpaid, others paid)
    val isPaid = remember(sale.id) {
        sale.id % 5 != 0
    }

    // Beautiful dynamic icons and pastel background matching screenshot
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
                Triple(themeColor.copy(alpha = 0.12f), themeColor, Icons.Default.ShoppingBag)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("sale_card_${sale.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Group: Icon + Title/Subtext
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
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

                // Right Group: Price + Badge
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

                    // Paid / Unpaid Badge matching screenshot 100%
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

                        // Delete button
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

// Utility stroke border
@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = Color(0xFFE2E8F0)
)
