package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.model.Product
import com.example.data.repository.InventoryRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.InventoryViewModelFactory
import com.example.util.FormatUtil
import com.example.util.LanguageManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay

enum class ScreenTab {
    Fandraisana, // Accueil (Dashboard)
    Caisse,      // POS / Checkout
    Manampy,     // Add product
    Tahiry,      // Stock listing / Fast edits
    Dettes,      // Client Debts
    Historique,  // Sales history
    Parametres   // Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainLifecycleContainer()
        }
    }
}

@Composable
fun MainLifecycleContainer() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    // Initialize repository with all 3 DAOs
    val repository = remember { 
        InventoryRepository(database.productDao(), database.saleDao(), database.debtDao()) 
    }
    
    val viewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(repository, context.applicationContext)
    )

    val themeColor by viewModel.themeColor.collectAsState()

    // States for screens
    var isSplashVisible by remember { mutableStateOf(true) }
    val isActivated by viewModel.isActivated.collectAsState()
    val activeLang by viewModel.language.collectAsState()

    // Notification permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // On start configurations
    LaunchedEffect(Unit) {
        // Init channels
        NotificationHelper.createNotificationChannel(context.applicationContext)
        
        // Request on Tiramisu+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2.5 seconds Splash screen simulation
        delay(2500)
        isSplashVisible = false
    }

    val t = { key: String -> LanguageManager.translate(key, activeLang) }

    MyApplicationTheme(primaryColorOverride = themeColor) {
        when {
            isSplashVisible -> {
                SplashScreen(t)
            }
            else -> {
                MainAppLayout(viewModel = viewModel, t = t)
            }
        }
    }
}

@Composable
fun SplashScreen(t: (String) -> String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "VAROTRA",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "Caisse & Stock Hors-ligne",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun ActivationScreen(viewModel: InventoryViewModel, t: (String) -> String) {
    var codeInput by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = t("activation_required"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Installation ID card info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = t("installation_id"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = viewModel.installationId,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 3.sp
                    )
                }

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it
                        codeError = false
                    },
                    label = { Text(t("activation_code_label")) },
                    isError = codeError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        if (codeError) {
                            Text(t("activation_error"), color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Button(
                    onClick = {
                        val success = viewModel.submitActivationCode(codeInput)
                        if (!success) {
                            codeError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("activate_app_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(t("activate_btn"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(
    viewModel: InventoryViewModel,
    t: (String) -> String
) {
    var currentTab by remember { mutableStateOf(ScreenTab.Fandraisana) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    val isActivated by viewModel.isActivated.collectAsState()
    var calculatorInitialSubTab by remember { mutableStateOf("checkout") }

    // Navigation redirects
    val navigateToHome = {
        currentTab = ScreenTab.Fandraisana
        productToEdit = null
        viewModel.searchQuery.value = ""
    }

    val navigateToList = {
        currentTab = ScreenTab.Tahiry
        productToEdit = null
        viewModel.searchQuery.value = ""
        viewModel.selectedCategory.value = "All"
    }

    val navigateToCaisse = {
        currentTab = ScreenTab.Caisse
        productToEdit = null
        viewModel.searchQuery.value = ""
        calculatorInitialSubTab = "checkout"
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {
            TabletDrawerSection(
                viewModel = viewModel,
                currentTab = currentTab,
                onTabSelected = { tab ->
                    if (tab == ScreenTab.Manampy) {
                        currentTab = ScreenTab.Manampy
                        productToEdit = null
                    } else {
                        if (tab == ScreenTab.Caisse) {
                            calculatorInitialSubTab = "checkout"
                        }
                        currentTab = tab
                        productToEdit = null
                    }
                },
                t = t
            )

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (currentTab != ScreenTab.Fandraisana && currentTab != ScreenTab.Historique && currentTab != ScreenTab.Parametres) {
                    TopAppBarSection(
                        viewModel = viewModel,
                        onNavigateToSettings = { currentTab = ScreenTab.Parametres }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentTab) {
                        ScreenTab.Fandraisana -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToAddProduct = { currentTab = ScreenTab.Manampy },
                            onNavigateToList = navigateToList,
                            onNavigateToSettings = { currentTab = ScreenTab.Parametres }
                        )
                        ScreenTab.Caisse -> CalculatorScreen(
                            viewModel = viewModel,
                            onNavigateToHome = navigateToHome,
                            initialSubTab = calculatorInitialSubTab
                        )
                        ScreenTab.Manampy -> AddProductScreen(
                            viewModel = viewModel,
                            editingProduct = productToEdit,
                            onSaveProduct = { product ->
                                viewModel.saveProduct(product)
                                productToEdit = null
                                navigateToList()
                            },
                            onCancel = {
                                productToEdit = null
                                navigateToHome()
                            }
                        )
                        ScreenTab.Tahiry -> InventoryListScreen(
                            viewModel = viewModel,
                            onEditProduct = { product ->
                                productToEdit = product
                                currentTab = ScreenTab.Manampy
                            },
                            onNavigateToAddProduct = { currentTab = ScreenTab.Manampy }
                        )
                        ScreenTab.Dettes -> DebtsScreen(
                            viewModel = viewModel
                        )
                        ScreenTab.Historique -> SalesHistoryScreen(
                            viewModel = viewModel,
                            onNavigateToCaisse = navigateToCaisse
                        )
                        ScreenTab.Parametres -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateToHistory = { currentTab = ScreenTab.Historique },
                            onNavigateToHome = navigateToHome
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (currentTab != ScreenTab.Fandraisana && currentTab != ScreenTab.Historique && currentTab != ScreenTab.Parametres) {
                    TopAppBarSection(
                        viewModel = viewModel,
                        onNavigateToSettings = { currentTab = ScreenTab.Parametres }
                    )
                }
            },
            bottomBar = {
                BottomNavBarSection(
                    viewModel = viewModel,
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        if (tab == ScreenTab.Manampy) {
                            calculatorInitialSubTab = "quick_misc"
                            currentTab = ScreenTab.Caisse
                        } else {
                            if (tab == ScreenTab.Caisse) {
                                calculatorInitialSubTab = "checkout"
                            }
                            currentTab = tab
                            productToEdit = null
                        }
                    }
                )
            },
            floatingActionButton = {}
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    ScreenTab.Fandraisana -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAddProduct = { currentTab = ScreenTab.Manampy },
                        onNavigateToList = navigateToList,
                        onNavigateToSettings = { currentTab = ScreenTab.Parametres }
                    )
                    ScreenTab.Caisse -> CalculatorScreen(
                        viewModel = viewModel,
                        onNavigateToHome = navigateToHome,
                        initialSubTab = calculatorInitialSubTab
                    )
                    ScreenTab.Manampy -> AddProductScreen(
                        viewModel = viewModel,
                        editingProduct = productToEdit,
                        onSaveProduct = { product ->
                            viewModel.saveProduct(product)
                            productToEdit = null
                            navigateToList()
                        },
                        onCancel = {
                            productToEdit = null
                            navigateToHome()
                        }
                    )
                    ScreenTab.Tahiry -> InventoryListScreen(
                        viewModel = viewModel,
                        onEditProduct = { product ->
                            productToEdit = product
                            currentTab = ScreenTab.Manampy
                        },
                        onNavigateToAddProduct = { currentTab = ScreenTab.Manampy }
                    )
                    ScreenTab.Dettes -> DebtsScreen(
                        viewModel = viewModel
                    )
                    ScreenTab.Historique -> SalesHistoryScreen(
                        viewModel = viewModel,
                        onNavigateToCaisse = navigateToCaisse
                    )
                    ScreenTab.Parametres -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToHistory = { currentTab = ScreenTab.Historique },
                        onNavigateToHome = navigateToHome
                    )
                }
            }
        }
    }
}

@Composable
fun TopAppBarSection(
    viewModel: InventoryViewModel,
    onNavigateToSettings: () -> Unit
) {
    val themeColor by viewModel.themeColor.collectAsState()
    val groceryNameVal by viewModel.groceryName.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Branding header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = groceryNameVal,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = themeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
            }

            // Settings icon button
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.12f))
                    .testTag("app_bar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Paramètres",
                    tint = themeColor
                )
            }
        }
    }
}

@Composable
fun BottomNavBarSection(
    viewModel: InventoryViewModel,
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val t = { key: String -> LanguageManager.translate(key, activeLang) }

    val navTabs = listOf(
        NavigationTab(ScreenTab.Fandraisana, t("tab_home"), Icons.Default.Home),
        NavigationTab(ScreenTab.Caisse, t("tab_pos"), Icons.Default.Inventory),
        NavigationTab(ScreenTab.Manampy, t("quick_misc_btn"), Icons.Default.Calculate),
        NavigationTab(ScreenTab.Tahiry, t("tab_inventory"), Icons.AutoMirrored.Filled.List),
        NavigationTab(ScreenTab.Dettes, t("tab_debts"), Icons.Default.People)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 1.dp,
        color = Color.White
    ) {
        Column {
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navTabs.forEach { tabItem ->
                    if (tabItem.tab == ScreenTab.Manampy) {
                        // Custom central yellow/orange FAB matching screenshot 100%
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFFB300))
                                    .clickable { onTabSelected(ScreenTab.Manampy) }
                                    .testTag("nav_tab_manampy"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = tabItem.label,
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    } else {
                        val isSelected = currentTab == tabItem.tab
                        val iconColor = if (isSelected) Color(0xFF13503C) else Color(0xFF94A3B8)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTabSelected(tabItem.tab) }
                                .padding(vertical = 4.dp)
                                .testTag("nav_tab_${tabItem.tab.name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tabItem.icon,
                                contentDescription = tabItem.label,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tabItem.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = iconColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Small orange dot centered under active tab's text
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF57C00))
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NavigationTab(
    val tab: ScreenTab,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun TabletDrawerSection(
    viewModel: InventoryViewModel,
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    t: (String) -> String
) {
    val themeColor by viewModel.themeColor.collectAsState()
    val activeLang by viewModel.language.collectAsState()
    val groceryNameVal by viewModel.groceryName.collectAsState()
    var expandedLanguageMenu by remember { mutableStateOf(false) }

    val drawerTabs = listOf(
        NavigationTab(ScreenTab.Fandraisana, t("tab_home"), Icons.Default.Home),
        NavigationTab(ScreenTab.Caisse, t("tab_pos"), Icons.Default.Inventory),
        NavigationTab(ScreenTab.Tahiry, t("tab_inventory"), Icons.AutoMirrored.Filled.List),
        NavigationTab(ScreenTab.Dettes, t("tab_debts"), Icons.Default.People),
        NavigationTab(ScreenTab.Manampy, t("tab_add"), Icons.Default.Add),
        NavigationTab(ScreenTab.Parametres, t("tab_settings"), Icons.Default.Settings)
    )

    Surface(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Branding Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(themeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = groceryNameVal,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = themeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Epicerie Pro",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp)

                // Navigation Items List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    drawerTabs.forEach { tabItem ->
                        val isSelected = currentTab == tabItem.tab
                        val bgSelectedColor = themeColor.copy(alpha = 0.12f)
                        val textAndIconColor = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) bgSelectedColor else Color.Transparent)
                                .clickable { onTabSelected(tabItem.tab) }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = tabItem.icon,
                                contentDescription = tabItem.label,
                                tint = textAndIconColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = tabItem.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textAndIconColor,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(themeColor)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Section: Languages & Offline Status
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(thickness = 0.5.dp)

                // Language Select Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedLanguageMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Malagasy (MG)"
                                "fr" -> "Français (FR)"
                                "en" -> "English (EN)"
                                else -> activeLang.uppercase()
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = expandedLanguageMenu,
                        onDismissRequest = { expandedLanguageMenu = false },
                        modifier = Modifier.width(220.dp)
                    ) {
                        LanguageManager.LANGUAGES.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    viewModel.changeLanguage(code)
                                    expandedLanguageMenu = false
                                }
                            )
                        }
                    }
                }

                // Off-line Status Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32))
                        )
                        Text(
                            text = "100% OFF-LINE MODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}
