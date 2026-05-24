package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.model.Product
import com.example.data.repository.InventoryRepository
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.InventoryViewModelFactory

enum class ScreenTab {
    Fandraisana,
    Kalkilatera,
    Manampy,
    Lisitra,
    Varotra
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout()
            }
        }
    }
}

@Composable
fun MainAppLayout() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { InventoryRepository(database.productDao(), database.saleDao()) }
    val viewModel: InventoryViewModel = viewModel(factory = InventoryViewModelFactory(repository))

    var currentTab by remember { mutableStateOf(ScreenTab.Fandraisana) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    // Navigation callbacks
    val navigateToHome = {
        currentTab = ScreenTab.Fandraisana
        productToEdit = null
        viewModel.searchQuery.value = ""
    }

    val navigateToList = {
        currentTab = ScreenTab.Lisitra
        productToEdit = null
        viewModel.searchQuery.value = ""
        viewModel.selectedCategory.value = "All"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBarSection()
        },
        bottomBar = {
            BottomNavBarSection(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    currentTab = tab
                    productToEdit = null // reset edit mode when tab is switched manually
                }
            )
        },
        floatingActionButton = {
            // Show FAB in Home and List tabs to quickly open product add screen
            if (currentTab == ScreenTab.Fandraisana || currentTab == ScreenTab.Lisitra) {
                FloatingActionButton(
                    onClick = {
                        productToEdit = null
                        currentTab = ScreenTab.Manampy
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = Color(0xFF2A1700),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(bottom = 72.dp) // Offset above the bottom tab bar
                        .testTag("add_product_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Manampy",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.Fandraisana -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToList = navigateToList,
                    onNavigateToCalculator = { product ->
                        viewModel.addToCart(product, 1)
                        currentTab = ScreenTab.Kalkilatera
                    },
                    onEditProduct = { product ->
                        productToEdit = product
                        currentTab = ScreenTab.Manampy
                    }
                )
                ScreenTab.Kalkilatera -> CalculatorScreen(
                    viewModel = viewModel,
                    onNavigateToHome = navigateToHome
                )
                ScreenTab.Manampy -> AddProductScreen(
                    editingProduct = productToEdit,
                    onSaveProduct = { product ->
                        viewModel.saveProduct(product)
                        productToEdit = null
                        // after saving, switch back to list or home
                        navigateToList()
                    },
                    onCancel = {
                        productToEdit = null
                        navigateToHome()
                    }
                )
                ScreenTab.Lisitra -> InventoryListScreen(
                    viewModel = viewModel,
                    onEditProduct = { product ->
                        productToEdit = product
                        currentTab = ScreenTab.Manampy
                    }
                )
                ScreenTab.Varotra -> SalesHistoryScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun TopAppBarSection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBasket,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Varotra",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Offline Indicator Tag
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline indicator",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BottomNavBarSection(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    NavigationTabItem(ScreenTab.Fandraisana, "Fandraisana", Icons.Default.Home),
                    NavigationTabItem(ScreenTab.Kalkilatera, "Kalkila", Icons.Default.Calculate),
                    NavigationTabItem(ScreenTab.Manampy, "Manampy", Icons.Default.AddCircle),
                    NavigationTabItem(ScreenTab.Lisitra, "Lisitra", Icons.AutoMirrored.Filled.List),
                    NavigationTabItem(ScreenTab.Varotra, "Varotra", Icons.Default.History)
                )

                tabs.forEach { item ->
                    val isSelected = currentTab == item.tab
                    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                    Column(
                          modifier = Modifier
                              .weight(1f)
                              .clickable { onTabSelected(item.tab) }
                              .padding(vertical = 8.dp)
                              .testTag("nav_tab_${item.tab.name.lowercase()}"),
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) VarotraPrimaryFixed.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                  )
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = tint,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

data class NavigationTabItem(
    val tab: ScreenTab,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
