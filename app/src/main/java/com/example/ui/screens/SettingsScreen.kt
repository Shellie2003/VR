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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: InventoryViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToCommission: () -> Unit,
    onNavigateToBarcodes: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val groceryNameVal by viewModel.groceryName.collectAsState()
    val currentThemeKey by viewModel.colorTheme.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val shopModeVal by viewModel.shopMode.collectAsState()
    val themeModeVal by viewModel.themeMode.collectAsState()

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
    var showSnackbar by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF1B1C1C)
    val cardBg = if (isDark) Color(0xFF222323) else Color(0xFFF8FAFC)
    val cardBorderColor = if (isDark) Color(0xFF343535) else Color(0xFFE2E8F0)
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
                        .background(if (isDark) Color(0xFF2D2E2E) else Color(0xFFF1F5F9))
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
                            focusedContainerColor = if (isDark) Color(0xFF1B1C1C) else Color.White,
                            unfocusedContainerColor = if (isDark) Color(0xFF1B1C1C) else Color.White,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = if (isDark) Color(0xFF4E5050) else Color(0xFFCBD5E1),
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
                                containerColor = if (isLightSel) themeColor else (if (isDark) Color(0xFF1B1C1C) else Color.White),
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
                                containerColor = if (isDarkSel) themeColor else (if (isDark) Color(0xFF1B1C1C) else Color.White),
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
                                containerColor = if (isSystemSel) themeColor else (if (isDark) Color(0xFF1B1C1C) else Color.White),
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
                                    containerColor = if (isSelected) themeColor else (if (isDark) Color(0xFF1B1C1C) else Color.White),
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
                                containerColor = if (isRetail) themeColor else (if (isDark) Color(0xFF1B1C1C) else Color.White),
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
                                containerColor = if (isWholesale) themeColor else (if (isDark) Color(0xFF1B1C1C) else Color.White),
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
                        imageVector = Icons.Default.KeyboardArrowRight,
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
                        imageVector = Icons.Default.KeyboardArrowRight,
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
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(24.dp)
                    )
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
                        text = savedMessage,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
