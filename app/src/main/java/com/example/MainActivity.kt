package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ShoppingBasket
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
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

enum class ScreenTab {
    Fandraisana, // Accueil (Dashboard)
    Caisse,      // POS / Checkout
    Manampy,     // Add product
    Tahiry,      // Stock listing / Fast edits
    Dettes,      // Client Debts
    Historique,  // Sales history
    Parametres,  // Settings
    Commission,  // Restocking & supply commission calculator
    BarcodeList, // Barcode management and sheet printing
    Synchronisation // Multi-terminal synchronization screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable StrictMode in debug builds to catch accidental I/O on the main thread
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

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

    // Initialize DB off the UI thread to avoid blocking composition / causing StrictMode violations
    var database by remember { mutableStateOf<AppDatabase?>(null) }
    var repository by remember { mutableStateOf<InventoryRepository?>(null) }

    LaunchedEffect(Unit) {
        // perform database build on IO dispatcher
        val db = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context.applicationContext)
        }
        database = db
        repository = InventoryRepository(
            db,
            db.productDao(),
            db.saleDao(),
            db.debtDao(),
            db.produitDao(),
            db.uniteProduitDao(),
            db.reglePrixDao(),
            db.fournisseurDao(),
            db.mouvementStockDao(),
            db.lotProduitDao(),
            db.venteDao(),
            db.lignesVenteDao(),
            db.restockDao()
        )
    }

    // Show a lightweight loading UI while DB/repository are initializing
    if (repository == null) {
        MyApplicationTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Initialisation...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        return
    }

    // repository is non-null here
    val repo = repository!!
    val viewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(repo, context.applicationContext)
    )

    LaunchedEffect(viewModel) {
        com.example.sync.SyncManager.syncBridge = object : com.example.sync.SyncBridge {
            override fun handleReserveStock(productId: String, quantity: Double): Boolean {
                return viewModel.reserveStockSync(productId, quantity)
            }
            override fun handleCommitSale(saleJson: String): Boolean {
                return viewModel.commitSaleSync(saleJson)
            }
            override fun handleUpdateStockGlobal(productId: String, newQuantity: Double) {
                viewModel.updateStockSync(productId, newQuantity)
            }
            override fun getAllProductsJson(): String {
                return viewModel.getAllProductsJsonSync()
            }
            override fun handleSyncStock(stockJson: String) {
                viewModel.syncAllProductsSync(stockJson)
            }
            override fun logMessage(text: String) {
                viewModel.addSyncLog(text)
            }
            override fun getFullDatabaseJson(): String {
                return viewModel.getFullDatabaseJsonSync()
            }
            override fun handleFullDatabaseSync(syncJson: String) {
                viewModel.syncFullDatabaseSync(syncJson)
            }
        }
    }

    val themeColor by viewModel.themeColor.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

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

        // 1.0 second Splash screen simulation for ultra fast startup
        delay(1000)
        isSplashVisible = false
    }

    val t = { key: String -> LanguageManager.translate(key, activeLang) }

    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    MyApplicationTheme(darkTheme = isDark, primaryColorOverride = themeColor) {
        when {
            isSplashVisible -> {
                SplashScreen(t)
            }
            !isActivated -> {
                ActivationScreen(viewModel = viewModel, t = t)
            }
            else -> {
                MainAppLayout(viewModel = viewModel, t = t)
            }
        }
    }
}

// ... rest of file remains unchanged (SplashScreen, ActivationScreen, MainAppLayout, TopAppBarSection, BottomNavBarSection, etc.)

