package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.FormatUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val allSales by viewModel.allSales.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF002114)
    val mainTextColor = if (isDark) Color.White else Color(0xFF1E293B)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // 7 days, 30 days, or "this month"
    var selectedRange by remember { mutableStateOf(7) }

    val rangeStart = remember(selectedRange) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        when (selectedRange) {
            7 -> cal.add(Calendar.DAY_OF_YEAR, -6)
            30 -> cal.add(Calendar.DAY_OF_YEAR, -29)
            else -> cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.timeInMillis
    }

    val dayCount = remember(selectedRange, rangeStart) {
        when (selectedRange) {
            7 -> 7
            30 -> 30
            else -> {
                val today = Calendar.getInstance()
                today.get(Calendar.DAY_OF_MONTH)
            }
        }
    }

    val filteredSales = remember(allSales, rangeStart) {
        allSales.filter { it.timestamp >= rangeStart }
    }

    val totalRevenue = remember(filteredSales) { filteredSales.sumOf { it.totalAmount } }
    val totalCost = remember(filteredSales, allProducts) {
        filteredSales.sumOf { sale ->
            sale.items.sumOf { item ->
                val prod = allProducts.find { it.id == item.productId }
                val unitCost = prod?.prixAchatUniteBase ?: prod?.wholesalePrice ?: 0.0
                unitCost * item.quantity
            }
        }
    }
    val totalProfit = totalRevenue - totalCost
    val marginPct = if (totalCost > 0.0) (totalProfit / totalCost) * 100.0 else 0.0

    // Daily revenue buckets for the bar chart
    val dailyRevenue = remember(filteredSales, rangeStart, dayCount) {
        val values = mutableListOf<Double>()
        val cal = Calendar.getInstance().apply { timeInMillis = rangeStart }
        for (i in 0 until dayCount) {
            val dayStartCal = cal.clone() as Calendar
            dayStartCal.add(Calendar.DAY_OF_YEAR, i)
            val dayStartMs = dayStartCal.timeInMillis
            val dayEndMs = dayStartMs + 24 * 60 * 60 * 1000 - 1
            val rev = filteredSales.filter { it.timestamp in dayStartMs..dayEndMs }.sumOf { it.totalAmount }
            values.add(rev)
        }
        values
    }

    val rangeEndLabel = remember(dayCount, rangeStart) {
        val dayFmt = SimpleDateFormat("dd/MM", Locale.FRANCE)
        val endCal = Calendar.getInstance().apply { timeInMillis = rangeStart }
        endCal.add(Calendar.DAY_OF_YEAR, dayCount - 1)
        "${dayFmt.format(Date(rangeStart))} → ${dayFmt.format(endCal.time)}"
    }

    // Top products by revenue within the range
    data class TopProduct(val name: String, val quantity: Double, val revenue: Double, val unit: String)
    val topProducts = remember(filteredSales) {
        val map = LinkedHashMap<String, TopProduct>()
        filteredSales.forEach { sale ->
            sale.items.forEach { item ->
                val existing = map[item.name]
                if (existing != null) {
                    map[item.name] = existing.copy(
                        quantity = existing.quantity + item.quantity,
                        revenue = existing.revenue + item.price * item.quantity
                    )
                } else {
                    map[item.name] = TopProduct(item.name, item.quantity, item.price * item.quantity, "")
                }
            }
        }
        map.values.sortedByDescending { it.revenue }.take(8)
    }

    val screenTitle = when (activeLang) {
        "mg" -> "Kajy sy Tatitra"
        "fr" -> "Rapports & Tableau de bord"
        else -> "Reports & Dashboard"
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
                            fontSize = 19.sp
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Range selector chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    7 to when (activeLang) { "mg" -> "7 andro"; "fr" -> "7 jours"; else -> "7 days" },
                    30 to when (activeLang) { "mg" -> "30 andro"; "fr" -> "30 jours"; else -> "30 days" },
                    -1 to when (activeLang) { "mg" -> "Ity volana ity"; "fr" -> "Ce mois-ci"; else -> "This month" }
                ).forEach { (rangeValue, label) ->
                    FilterChip(
                        selected = selectedRange == rangeValue,
                        onClick = { selectedRange = rangeValue },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("dashboard_range_$rangeValue")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary cards
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = when (activeLang) { "mg" -> "Vola miditra"; "fr" -> "Chiffre d'affaires"; else -> "Revenue" },
                    value = "${FormatUtil.formatPrice(totalRevenue)} Ar",
                    color = themeColor
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = when (activeLang) { "mg" -> "Tombom-barotra"; "fr" -> "Bénéfice net"; else -> "Net profit" },
                    value = "${FormatUtil.formatPrice(totalProfit)} Ar",
                    color = if (totalProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SummaryStatCard(
                modifier = Modifier.fillMaxWidth(),
                label = when (activeLang) { "mg" -> "Taham-bidiny"; "fr" -> "Taux de marge"; else -> "Margin rate" },
                value = "${FormatUtil.formatPrice(marginPct)} %",
                color = when {
                    marginPct < 0 -> Color(0xFFC62828)
                    marginPct < 15 -> Color(0xFFF57C00)
                    else -> Color(0xFF2E7D32)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Revenue bar chart
            Card(
                modifier = Modifier.fillMaxWidth().testTag("dashboard_revenue_chart_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Vola miditra isan'andro"
                            "fr" -> "Chiffre d'affaires par jour"
                            else -> "Daily revenue"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainTextColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RevenueBarChart(values = dailyRevenue, barColor = themeColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = rangeEndLabel,
                        fontSize = 11.sp,
                        color = secondaryTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Top products
            Text(
                text = when (activeLang) {
                    "mg" -> "Entana be mpividy indrindra"
                    "fr" -> "Produits les plus vendus"
                    else -> "Top selling products"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = mainTextColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (topProducts.isEmpty()) {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Mbola tsy misy varotra tamin'io fe-potoana io."
                        "fr" -> "Aucune vente sur cette période."
                        else -> "No sales in this period."
                    },
                    fontSize = 12.sp,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val maxRevenue = topProducts.maxOf { it.revenue }.coerceAtLeast(1.0)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topProducts.forEachIndexed { index, product ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("dashboard_top_product_$index"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${product.name}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = mainTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${FormatUtil.formatPrice(product.revenue)} Ar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = themeColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = (product.revenue / maxRevenue).toFloat(),
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = themeColor,
                                    trackColor = themeColor.copy(alpha = 0.12f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${FormatUtil.formatQty(product.quantity, "").trim()} " + when (activeLang) {
                                        "mg" -> "voavidy"
                                        "fr" -> "vendu(s)"
                                        else -> "sold"
                                    },
                                    fontSize = 10.sp,
                                    color = secondaryTextColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RevenueBarChart(values: List<Double>, barColor: Color) {
    val maxVal = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val barCount = values.size
        if (barCount == 0) return@Canvas
        val spacing = 2.dp.toPx()
        val barWidth = ((size.width - spacing * (barCount - 1)) / barCount).coerceAtLeast(1f)
        values.forEachIndexed { index, value ->
            val barHeightRatio = (value / maxVal).toFloat().coerceIn(0f, 1f)
            val barHeight = (barHeightRatio * size.height).coerceAtLeast(if (value > 0) 2f else 0f)
            val left = index * (barWidth + spacing)
            drawRect(
                color = if (value > 0) barColor else barColor.copy(alpha = 0.15f),
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight.coerceAtLeast(1f))
            )
        }
    }
}
