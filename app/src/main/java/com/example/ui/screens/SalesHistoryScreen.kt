package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Sale
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SalesHistoryScreen(
    viewModel: InventoryViewModel
) {
    val sales by viewModel.allSales.collectAsState()

    // Aggregates
    val totalRevenue = sales.sumOf { it.totalAmount }
    val totalTransactions = sales.size

    // Delete confirmation dialog state
    var saleToDelete by remember { mutableStateOf<Sale?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stats Card - Revenue & transactions summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Vola maty rehetra (Total Sales)",
                        style = MaterialTheme.typography.labelMedium,
                        color = com.example.ui.theme.VarotraPrimaryFixed,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ar ${formatPrice(totalRevenue)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .background(com.example.ui.theme.VarotraPrimaryFixed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = totalTransactions.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Varotra",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.example.ui.theme.VarotraPrimaryFixed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Text(
            text = "Diarin'ny Varotra (Sales Log)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (sales.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "Tsy mbola nisy varotra natao.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Log listing
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp), // avoid nav collision
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sales) { sale ->
                    SaleCard(
                        sale = sale,
                        onDelete = { saleToDelete = sale }
                    )
                }
            }
        }
    }

    // Accidental sale deletion popup
    saleToDelete?.let { sale ->
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text("Tena fafana ity varotra ity?") },
            text = { Text("Mety hiteraka tsy fahampiana amin'ny kajy mifanaraka amin'ny fidiram-bola izany. Fafana tokoa ve?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSale(sale)
                        saleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eny, fafao")
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) {
                    Text("Hanafo")
                }
            }
        )
    }
}

@Composable
fun SaleCard(
    sale: Sale,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Convert time
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val dateString = remember(sale.timestamp) { formatter.format(Date(sale.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sale_card_${sale.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Brief Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${sale.items.sumOf { it.quantity }} entana",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ar ${formatPrice(sale.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Fafao ity varotra ity",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Display details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { isExpanded = !isExpanded }
                            .padding(4.dp)
                    )
                }
            }

            // Expanded Item list details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Text(
                        text = "Details momba ny fizorany:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    sale.items.forEach { soldItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = soldItem.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${soldItem.quantity} x Ar ${formatPrice(soldItem.price)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Ar ${formatPrice(soldItem.price * soldItem.quantity)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
