package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.FirebaseBackupManager
import com.example.util.LanguageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: InventoryViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToCommission: () -> Unit,
    onNavigateToBarcodes: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToCaisseMouvements: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val groceryNameVal by viewModel.groceryName.collectAsState()
    val currentThemeKey by viewModel.colorTheme.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val shopModeVal by viewModel.shopMode.collectAsState()
    val themeModeVal by viewModel.themeMode.collectAsState()
    val firebaseBucketVal by viewModel.firebaseStorageBucket.collectAsState()
    val installationId = viewModel.installationId

    // Local state for grocery name editing
    var nameInput by remember(groceryNameVal) { mutableStateOf(groceryNameVal) }

    // Translate helper
    val t = { key: String -> LanguageManager.translate(key, activeLang) }

    // Translations for settings page
    val settingsTitle = when (activeLang) {
        "mg" -> "Fikirakirana"
        "fr" -> "Paramètres"
        else -> "Settings"
    }

    val storeNameLabel = when (activeLang) {
        "mg" -> "Anaran'ny Tsena"
        "fr" -> "Nom de l'épicerie"
        else -> "Grocery Store Name"
    }

    val shopModeLabel = when (activeLang) {
        "mg" -> "Fomba fivarotana (Shop Mode)"
        "fr" -> "Mode de vente"
        else -> "Shop Scale / Pricing Mode"
    }

    val retailLabel = when (activeLang) {
        "mg" -> "Mpaninjara (Retail/Détail)"
        "fr" -> "Détail / Épicerie"
        else -> "Retail (Grocery/Small)"
    }

    val wholesaleLabel = when (activeLang) {
        "mg" -> "Ambongadiny (Grossiste/Bulk)"
        "fr" -> "Gros / Grossiste"
        else -> "Wholesale (Bulk/Gros)"
    }

    val themeLabel = when (activeLang) {
        "mg" -> "Loko Fototra (Thème)"
        "fr" -> "Thème (Couleurs)"
        else -> "Color Theme"
    }

    val displayModeLabel = when (activeLang) {
        "mg" -> "Fomba Fampisehoana"
        "fr" -> "Mode d'affichage"
        else -> "Display Mode"
    }

    val lightLabel = when (activeLang) {
        "mg" -> "Mazava (Light)"
        "fr" -> "Clair"
        else -> "Light"
    }

    val darkLabel = when (activeLang) {
        "mg" -> "Maizina (Dark)"
        "fr" -> "Sombre"
        else -> "Dark"
    }

    val systemLabel = when (activeLang) {
        "mg" -> "Araka ny finday"
        "fr" -> "Système"
        else -> "System"
    }

    val historyBtnTitle = when (activeLang) {
        "mg" -> "Tantaran'ny Varotra (Historique)"
        "fr" -> "Historique des ventes"
        else -> "Sales History"
    }

    val saveBtnText = when (activeLang) {
        "mg" -> "Hitehirizana"
        "fr" -> "Enregistrer"
        else -> "Save"
    }

    val languageLabel = when (activeLang) {
        "mg" -> "Safidy Fiteny"
        "fr" -> "Langue de l'application"
        else -> "Application Language"
    }

    val savedMessage = when (activeLang) {
        "mg" -> "Tafita! Voatahiry ny anaran'ny tsena."
        "fr" -> "Enregistré ! Nom mis à jour."
        else -> "Saved! Name updated successfully."
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // Firebase cloud backup local UI state
    var firebaseBucketInput by remember(firebaseBucketVal) { mutableStateOf(firebaseBucketVal) }
    var isCloudBackupLoading by remember { mutableStateOf(false) }
    var isCloudRestoreLoading by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF002114)
    val cardBg = if (isDark) Color(0xFF1B4332) else Color(0xFFF8FAFC)
    val cardBorderColor = if (isDark) Color(0xFF2C5E43) else Color(0xFFE2E8F0)
    val mainTextColor = if (isDark) Color.White else Color(0xFF1E293B)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Custom Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onNavigateToHome,
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
                    text = settingsTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = mainTextColor,
                        fontSize = 22.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. CARD FOR STORE NAME EDITING
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = storeNameLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainTextColor
                        )
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grocery_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0xFF1B4332) else Color.White,
                            unfocusedContainerColor = if (isDark) Color(0xFF1B4332) else Color.White,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = if (isDark) Color(0xFF2C5E43) else Color(0xFFCBD5E1),
                            focusedTextColor = mainTextColor,
                            unfocusedTextColor = mainTextColor
                        )
                    )

                    Button(
                        onClick = {
                            if (nameInput.trim().isNotEmpty()) {
                                viewModel.updateGroceryName(nameInput.trim())
                                showSnackbar = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = saveBtnText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. CARD FOR THEME SELECTION
            val themes = listOf(
                Triple("emerald", Color(0xFF13503C), "Emerôda"),
                Triple("sunset", Color(0xFFE65100), "Sariaka"),
                Triple("indigo", Color(0xFF1E3A8A), "Manga"),
                Triple("rose", Color(0xFF881337), "Mena")
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = themeLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainTextColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        themes.forEach { (key, color, label) ->
                            val isSelected = currentThemeKey == key
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clickable { viewModel.updateColorTheme(key) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) (if (isDark) Color.White else Color.Black) else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) mainTextColor else secondaryTextColor
                                )
                            }
                        }
                    }
                }
            }

            // CARD FOR THEME MODE SELECTION (LIGHT / DARK / SYSTEM)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness4,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = displayModeLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainTextColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Light Option
                        val isLightSel = themeModeVal == "light"
                        Button(
                            onClick = { viewModel.updateThemeMode("light") },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLightSel) themeColor else (if (isDark) Color(0xFF1B4332) else Color.White),
                                contentColor = if (isLightSel) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isLightSel) Color.Transparent else cardBorderColor
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = lightLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Dark Option
                        val isDarkSel = themeModeVal == "dark"
                        Button(
                            onClick = { viewModel.updateThemeMode("dark") },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkSel) themeColor else (if (isDark) Color(0xFF1B4332) else Color.White),
                                contentColor = if (isDarkSel) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isDarkSel) Color.Transparent else cardBorderColor
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = darkLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // System Option
                        val isSystemSel = themeModeVal == "system"
                        Button(
                            onClick = { viewModel.updateThemeMode("system") },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSystemSel) themeColor else (if (isDark) Color(0xFF1B4332) else Color.White),
                                contentColor = if (isSystemSel) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSystemSel) Color.Transparent else cardBorderColor
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = systemLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. CARD FOR LANGUAGE SELECTION (TOWARDS INTEGRATED SETTINGS EXPERIENCE)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = languageLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainTextColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageManager.LANGUAGES.forEach { (code, name) ->
                            val isSelected = activeLang == code
                            Button(
                                onClick = { viewModel.changeLanguage(code) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) themeColor else (if (isDark) Color(0xFF1B4332) else Color.White),
                                    contentColor = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else cardBorderColor
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = when(code) {
                                        "mg" -> "Malagasy"
                                        "fr" -> "Français"
                                        else -> "English"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4.5. CARD FOR SHOP MODE SELECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = shopModeLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainTextColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Retail Option
                        val isRetail = shopModeVal == "retail"
                        Button(
                            onClick = { viewModel.updateShopMode("retail") },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRetail) themeColor else (if (isDark) Color(0xFF1B4332) else Color.White),
                                contentColor = if (isRetail) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isRetail) Color.Transparent else cardBorderColor
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = retailLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Wholesale Option
                        val isWholesale = shopModeVal == "wholesale"
                        Button(
                            onClick = { viewModel.updateShopMode("wholesale") },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isWholesale) themeColor else (if (isDark) Color(0xFF1B4332) else Color.White),
                                contentColor = if (isWholesale) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isWholesale) Color.Transparent else cardBorderColor
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = wholesaleLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // CARD REDIRECT TO COMMISSION / APPROVISIONNEMENT
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNavigateToCommission() }
                    .testTag("settings_commission_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
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
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Fampidirana entana & Tombony"
                                    "fr" -> "Approvisionnement & Marge"
                                    else -> "Procurement & Margins"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Kajio ny tombom-barotra ary ampitomboy ny tahiry"
                                    "fr" -> "Calculer les bénéfices et réapprovisionner le stock"
                                    else -> "Calculate profit and restock products"
                                },
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

            // 5. PROMINENT CARD REDIRECT TO HISTORY
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNavigateToHistory() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = historyBtnTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                text = "Hijery ny tantaran'ny varotra rehetra",
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

            // CARD REDIRECT TO BARCODES MANAGEMENT
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNavigateToBarcodes() }
                    .testTag("settings_barcodes_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
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
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Fikirakirana Kaody Bar"
                                    "fr" -> "Gestion des codes-barres"
                                    else -> "Barcode Management"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Hijery, hiteraka ary hanonta taratasy kaody bar iray"
                                    "fr" -> "Visualiser, générer et imprimer une feuille de codes-barres"
                                    else -> "View, generate and print a sheet of barcodes"
                                },
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

            // CARD REDIRECT TO MULTI-TERMINAL SYNC
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNavigateToSync() }
                    .testTag("settings_sync_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
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
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Mampitohy finday maro (Sync)"
                                    "fr" -> "Synchronisation multi-terminal"
                                    else -> "Multi-Terminal Sync"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Hampitahana ny tahiry sy ny varotra amin'ny finday haha amin'ny alalan'ny Wi-Fi"
                                    "fr" -> "Partager le stock et les ventes en temps réel via le réseau local Wi-Fi"
                                    else -> "Share stock and sales in real-time over local Wi-Fi"
                                },
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

            // CARD REDIRECT TO CASH REGISTER MOVEMENTS (Entrée/Sortie de caisse)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNavigateToCaisseMouvements() }
                    .testTag("settings_caisse_mouvements_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
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
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Vola an-Kesty (Miditra/Mivoaka)"
                                    "fr" -> "Mouvements de caisse"
                                    else -> "Cash Movements"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Hisoratra ny fidiran-bola sy ny fivoahan-bola an-kesty"
                                    "fr" -> "Enregistrer les entrées et sorties d'espèces de la caisse"
                                    else -> "Record manual cash-in and cash-out movements"
                                },
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

            // CARD REDIRECT TO REPORTS & DASHBOARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNavigateToDashboard() }
                    .testTag("settings_dashboard_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
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
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Kajy sy Tatitra"
                                    "fr" -> "Rapports & Tableau de bord"
                                    else -> "Reports & Dashboard"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Vola miditra, tombom-barotra ary entana be mpividy"
                                    "fr" -> "Chiffre d'affaires, marges et produits les plus vendus"
                                    else -> "Revenue, margins and top selling products"
                                },
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

            // SECTION SAFETY BACKUP & SECURITY
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("settings_backup_security_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Fiarovana ny Tahiry"
                                    "fr" -> "Sauvegarde & Sécurité"
                                    else -> "Backup & Security"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = mainTextColor
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Misoroka ny fahaverezan'ny angon-drakitra noho ny fanavaozana"
                                    "fr" -> "Prévient la perte de données lors des mises à jour"
                                    else -> "Prevents data loss during future application updates"
                                },
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                    }

                    HorizontalDivider(color = cardBorderColor.copy(alpha = 0.5f))

                    Text(
                        text = when (activeLang) {
                            "mg" -> "Tehirizo ny tahiry rehetra amin'ny toerana azo antoka na havereno raha misy fahasimbana."
                            "fr" -> "Sauvegardez vos données localement ou restaurez-les en cas d'anomalie."
                            else -> "Save your data locally or restore it in case of any data corruption."
                        },
                        fontSize = 12.sp,
                        color = secondaryTextColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Backup Button
                        Button(
                            onClick = {
                                viewModel.triggerLocalSafetyBackup()
                                snackbarMessage = when (activeLang) {
                                    "mg" -> "Tafita! Voatahiry ao amin'ny finday ny backup-nao."
                                    "fr" -> "Sauvegarde réussie ! Vos données sont en sécurité."
                                    else -> "Backup successful! Your data is fully secured."
                                }
                                showSnackbar = true
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Backup"
                                    "fr" -> "Sauvegarder"
                                    else -> "Backup"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Restore Button
                        OutlinedButton(
                            onClick = {
                                val success = viewModel.restoreLocalSafetyBackup()
                                if (success) {
                                    snackbarMessage = when (activeLang) {
                                        "mg" -> "Tafita! Tafaverina soa aman-tsara ny tahiry rehetra."
                                        "fr" -> "Restauration réussie ! Toutes vos données sont rétablies."
                                        else -> "Restore successful! All your data has been restored."
                                    }
                                } else {
                                    snackbarMessage = when (activeLang) {
                                        "mg" -> "Hadisoana: Tsy misy backup hita ao amin'ny finday."
                                        "fr" -> "Échec : Aucun fichier de sauvegarde trouvé."
                                        else -> "Failed: No backup file found on this device."
                                    }
                                }
                                showSnackbar = true
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = themeColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Haverina"
                                    "fr" -> "Restaurer"
                                    else -> "Restore"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // SECTION FIREBASE CLOUD BACKUP
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("settings_firebase_backup_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Sauvegarde Cloud (Firebase)"
                                    "fr" -> "Sauvegarde Cloud (Firebase)"
                                    else -> "Cloud Backup (Firebase)"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = mainTextColor
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Tehirizo any amin'ny rahona (internet) ny tahiry"
                                    "fr" -> "Sauvegardez vos données à distance, même en cas de perte du téléphone"
                                    else -> "Back up your data remotely, even if the phone is lost"
                                },
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                    }

                    HorizontalDivider(color = cardBorderColor.copy(alpha = 0.5f))

                    OutlinedTextField(
                        value = firebaseBucketInput,
                        onValueChange = { firebaseBucketInput = it },
                        label = {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Firebase Storage Bucket"
                                    "fr" -> "Bucket Firebase Storage"
                                    else -> "Firebase Storage Bucket"
                                }
                            )
                        },
                        placeholder = { Text("mon-projet.appspot.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("firebase_bucket_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text(
                        text = when (activeLang) {
                            "mg" -> "Alaivo tao amin'ny Firebase Console > Storage ny anaran'ny bucket, ary avereno ho \"allow read, write: if true\" ny rules ao amin'ny lalana backups/. Raha tsy misy bucket voatondro dia tsy afaka mampiasa ity fikirakirana ity."
                            "fr" -> "Créez un projet Firebase (gratuit), activez Storage, copiez le nom du bucket ici et autorisez la lecture/écriture sur le chemin backups/ dans les règles de sécurité. Sans bucket configuré, ces boutons resteront inactifs."
                            else -> "Create a free Firebase project, enable Storage, paste the bucket name here and allow read/write on the backups/ path in the security rules. Without a configured bucket, these buttons will show a clear error."
                        },
                        fontSize = 11.sp,
                        color = secondaryTextColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cloud Backup Button
                        Button(
                            onClick = {
                                if (firebaseBucketInput.isBlank()) {
                                    snackbarMessage = when (activeLang) {
                                        "mg" -> "Ampidiro aloha ny anaran'ny bucket Firebase."
                                        "fr" -> "Veuillez d'abord renseigner le bucket Firebase."
                                        else -> "Please enter the Firebase bucket first."
                                    }
                                    showSnackbar = true
                                } else {
                                    viewModel.updateFirebaseStorageBucket(firebaseBucketInput.trim())
                                    isCloudBackupLoading = true
                                    coroutineScope.launch {
                                        val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            viewModel.getFullDatabaseJsonSync()
                                        }
                                        val result = FirebaseBackupManager.uploadBackup(firebaseBucketInput.trim(), installationId, json)
                                        isCloudBackupLoading = false
                                        snackbarMessage = if (result.isSuccess) {
                                            when (activeLang) {
                                                "mg" -> "Tafita! Voatahiry any amin'ny rahona ny tahiry-nao."
                                                "fr" -> "Sauvegarde Cloud réussie ! Vos données sont en ligne."
                                                else -> "Cloud backup successful! Your data is now online."
                                            }
                                        } else {
                                            when (activeLang) {
                                                "mg" -> "Hadisoana: tsy voatahiry any amin'ny rahona (jereo ny bucket sy ny internet)."
                                                "fr" -> "Échec de la sauvegarde Cloud (vérifiez le bucket et la connexion internet)."
                                                else -> "Cloud backup failed (check the bucket name and internet connection)."
                                            }
                                        }
                                        showSnackbar = true
                                    }
                                }
                            },
                            enabled = !isCloudBackupLoading && !isCloudRestoreLoading,
                            modifier = Modifier.weight(1f).height(40.dp).testTag("firebase_backup_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor,
                                contentColor = Color.White
                            )
                        ) {
                            if (isCloudBackupLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (activeLang) {
                                        "mg" -> "Cloud"
                                        "fr" -> "Sauvegarder"
                                        else -> "Backup"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Cloud Restore Button
                        OutlinedButton(
                            onClick = {
                                if (firebaseBucketInput.isBlank()) {
                                    snackbarMessage = when (activeLang) {
                                        "mg" -> "Ampidiro aloha ny anaran'ny bucket Firebase."
                                        "fr" -> "Veuillez d'abord renseigner le bucket Firebase."
                                        else -> "Please enter the Firebase bucket first."
                                    }
                                    showSnackbar = true
                                } else {
                                    viewModel.updateFirebaseStorageBucket(firebaseBucketInput.trim())
                                    isCloudRestoreLoading = true
                                    coroutineScope.launch {
                                        val result = FirebaseBackupManager.downloadBackup(firebaseBucketInput.trim(), installationId)
                                        isCloudRestoreLoading = false
                                        result.onSuccess { json -> viewModel.syncFullDatabaseSync(json) }
                                        snackbarMessage = if (result.isSuccess) {
                                            when (activeLang) {
                                                "mg" -> "Tafita! Tafaverina ny tahiry avy any amin'ny rahona."
                                                "fr" -> "Restauration Cloud réussie ! Données récupérées."
                                                else -> "Cloud restore successful! Data recovered."
                                            }
                                        } else {
                                            when (activeLang) {
                                                "mg" -> "Hadisoana: tsy misy backup hita any amin'ny rahona."
                                                "fr" -> "Échec : aucune sauvegarde trouvée sur le Cloud."
                                                else -> "Failed: no backup found on the Cloud."
                                            }
                                        }
                                        showSnackbar = true
                                    }
                                }
                            },
                            enabled = !isCloudBackupLoading && !isCloudRestoreLoading,
                            modifier = Modifier.weight(1f).height(40.dp).testTag("firebase_restore_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor)
                        ) {
                            if (isCloudRestoreLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = themeColor, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (activeLang) {
                                        "mg" -> "Haverina"
                                        "fr" -> "Restaurer"
                                        else -> "Restore"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Animated snackbar notification at the bottom
        if (showSnackbar) {
            LaunchedEffect(showSnackbar) {
                kotlinx.coroutines.delay(2000)
                showSnackbar = false
            }
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80)
                    )
                    Text(
                        text = snackbarMessage.ifEmpty { savedMessage },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
