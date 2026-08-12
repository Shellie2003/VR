package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.BuildConfig
import com.example.data.model.Vendeur
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.BackupHelper
import com.example.util.ExportUtil
import com.example.util.FirebaseBackupManager
import com.example.util.InfoMiseAJour
import com.example.util.LanguageManager
import com.example.util.UpdateManager
import kotlinx.coroutines.launch

/**
 * Message précis pour chaque issue d'une restauration cloud (voir
 * [InventoryViewModel.restaurerDepuisCloud]) — un gérant hors-ligne ne doit jamais lire « aucune
 * sauvegarde trouvée » (il croirait le code cassé), et une vraie faute de frappe ne doit jamais
 * lire « pas de réseau » (il chercherait du Wi-Fi au lieu de corriger le code).
 */
private fun messageResultatRestauration(
    resultat: InventoryViewModel.ResultatRestaurationCloud,
    activeLang: String
): String = when (resultat) {
    InventoryViewModel.ResultatRestaurationCloud.SUCCES -> when (activeLang) {
        "mg" -> "Tafita! Tafaverina ny angona. Karohina ny sary."
        "fr" -> "Restauration réussie ! Recherche des photos en cours."
        else -> "Restore successful! Fetching photos."
    }
    InventoryViewModel.ResultatRestaurationCloud.CODE_INVALIDE -> when (activeLang) {
        "mg" -> "Diso ny kaody. Hamarino tsara ny litera sy isa."
        "fr" -> "Code invalide. Vérifiez lettres et chiffres."
        else -> "Invalid code. Check letters and digits."
    }
    InventoryViewModel.ResultatRestaurationCloud.HORS_LIGNE -> when (activeLang) {
        "mg" -> "Tsy misy aterineto. Andramo indray rehefa misy fifandraisana."
        "fr" -> "Pas de connexion internet. Réessayez une fois connecté."
        else -> "No internet connection. Try again once online."
    }
    InventoryViewModel.ResultatRestaurationCloud.AUCUNE_SAUVEGARDE -> when (activeLang) {
        "mg" -> "Tsy nisy backup hita tamin'io kaody io."
        "fr" -> "Aucune sauvegarde trouvée pour ce code. Vérifiez qu'il a bien été copié depuis l'autre téléphone."
        else -> "No backup found for this code. Check it was copied correctly from the other phone."
    }
}

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
    onNavigateToDashboard: () -> Unit,
    onNavigateToPeremption: () -> Unit,
    onNavigateToEpicerie: () -> Unit,
    onNavigateToSecurite: () -> Unit,
    onNavigateToEtagere: () -> Unit
) {
    val context = LocalContext.current
    // Tablet/large-screen layout: cap the settings column's width and center it instead of
    // stretching every card edge-to-edge on a wide screen.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val activeLang by viewModel.language.collectAsState()
    val groceryNameVal by viewModel.groceryName.collectAsState()
    val currentThemeKey by viewModel.colorTheme.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val shopModeVal by viewModel.shopMode.collectAsState()
    val themeModeVal by viewModel.themeMode.collectAsState()
    val firebaseDatabaseUrlVal by viewModel.firebaseDatabaseUrl.collectAsState()
    val firebaseBackupToken = viewModel.firebaseBackupToken
    val allVendeursVal by viewModel.allVendeurs.collectAsState()


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

    val pharmacieLabel = when (activeLang) {
        "mg" -> "Farmasia (Pharmacie)"
        "fr" -> "Pharmacie"
        else -> "Pharmacy"
    }

    val barLabel = when (activeLang) {
        "mg" -> "Bara (Bar)"
        "fr" -> "Bar / Boissons"
        else -> "Bar / Drinks"
    }

    // Un seul endroit décrit les modes et leur libellé : ajouter un métier plus tard ne demandera
    // pas de retoucher la mise en page du sélecteur.
    val modesDisponibles = listOf(
        com.example.util.ShopMode.DETAIL to retailLabel,
        com.example.util.ShopMode.GROSSISTE to wholesaleLabel,
        com.example.util.ShopMode.PHARMACIE to pharmacieLabel,
        com.example.util.ShopMode.BAR to barLabel
    )

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

    /**
     * Fabrique l'archive (données + photos), la dépose dans Téléchargements quand c'est possible,
     * puis ouvre le partage. Le message final distingue les deux cas : une copie durable a bien
     * été écrite, ou seul le partage a eu lieu. Annoncer la première quand c'est la seconde
     * reviendrait à promettre au gérant une sauvegarde qui ne survivrait pas à sa désinstallation.
     */
    fun partagerSauvegarde() {
        coroutineScope.launch {
            val (archive, deposee) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val fichier = BackupHelper.creerArchivePartageable(context)
                val ok = fichier != null && BackupHelper.deposerDansTelechargements(context, fichier)
                fichier to ok
            }
            snackbarMessage = when {
                archive == null -> when (activeLang) {
                    "mg" -> "Hadisoana: Tsy misy backup hita ao amin'ny finday."
                    "fr" -> "Échec : Aucun fichier de sauvegarde trouvé."
                    else -> "Failed: No backup file found on this device."
                }
                deposee -> when (activeLang) {
                    "mg" -> "Voatahiry ao amin'ny ${BackupHelper.cheminPublicLisible()} koa ny backup (misy ny sary)."
                    "fr" -> "Sauvegarde (photos incluses) également copiée dans ${BackupHelper.cheminPublicLisible()}."
                    else -> "Backup (photos included) also saved to ${BackupHelper.cheminPublicLisible()}."
                }
                else -> when (activeLang) {
                    "mg" -> "Nozaraina ny backup (misy ny sary). Tsy voatahiry ao amin'ny ${BackupHelper.cheminPublicLisible()}: tehirizo ny rakitra."
                    "fr" -> "Sauvegarde (photos incluses) partagée. Copie dans ${BackupHelper.cheminPublicLisible()} impossible : enregistrez le fichier vous-même."
                    else -> "Backup (photos included) shared. Could not copy to ${BackupHelper.cheminPublicLisible()}: save the file yourself."
                }
            }
            showSnackbar = true
            if (archive != null) ExportUtil.shareFile(context, archive, BackupHelper.MIME_ARCHIVE)
        }
    }

    // Android 9 et antérieur uniquement : le dépôt dans Téléchargements réclame une autorisation.
    // Quel que soit le verdict on partage — seule la copie durable en dépend.
    val demandeStockageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> partagerSauvegarde() }

    // Import a backup file picked from anywhere (WhatsApp download, Bluetooth, SD card, USB...)
    // — no Firebase project or email required, just a file someone can send you.
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                // Accepte les deux formats : l'archive .zip de cette version (données + photos,
                // extraites au passage dans le dossier photos) et le .json nu des versions
                // précédentes — une sauvegarde d'il y a six mois doit rester restaurable.
                val jsonText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.util.ArchiveUtil.lireSauvegarde(context, uri)
                }
                if (jsonText.isNullOrBlank()) {
                    snackbarMessage = when (activeLang) {
                        "mg" -> "Hadisoana: tsy voavaky ilay rakitra nosafidianao."
                        "fr" -> "Échec : impossible de lire le fichier sélectionné."
                        else -> "Failed: could not read the selected file."
                    }
                } else {
                    viewModel.syncFullDatabaseSync(jsonText)
                    snackbarMessage = when (activeLang) {
                        "mg" -> "Tafita! Nalaina avy amin'ilay rakitra ny tahiry."
                        "fr" -> "Importation réussie ! Les données du fichier ont été restaurées."
                        else -> "Import successful! Data from the file has been restored."
                    }
                }
                showSnackbar = true
            }
        }
    }

    // Firebase cloud backup local UI state
    var firebaseDatabaseUrlInput by remember(firebaseDatabaseUrlVal) { mutableStateOf(firebaseDatabaseUrlVal) }
    var isCloudBackupLoading by remember { mutableStateOf(false) }
    var isCloudRestoreLoading by remember { mutableStateOf(false) }
    // Récupération sur un nouveau téléphone : saisie du code d'une ancienne installation.
    var showRecoveryCodeDialog by remember { mutableStateOf(false) }
    var recoveryCodeInput by remember { mutableStateOf("") }
    var recoveryCodeError by remember { mutableStateOf(false) }

    // Tokens Material3 directement plutôt qu'un isDark manuel comparant background à un hex précis
    // (voir AGENTS.md §57) : ce dernier casse silencieusement dès que la palette change de valeurs,
    // alors que colorScheme.* suit toujours le thème réellement appliqué. Noms de variables conservés
    // tels quels (cardBg/cardBorderColor/mainTextColor/secondaryTextColor sont passés en paramètre à
    // VendeurRolesCard et réutilisés à des dizaines d'endroits dans ce fichier) pour ne pas avoir à
    // toucher chaque site d'appel individuellement.
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorderColor = MaterialTheme.colorScheme.outlineVariant
    val mainTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = if (isTablet) Alignment.TopCenter else Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .then(if (isTablet) Modifier.widthIn(max = 640.dp) else Modifier.fillMaxWidth())
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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

            // 2. CARTE VERS LA FICHE ÉPICERIE
            // Le nom de l'épicerie ne vit plus seul ici : il fait partie d'une fiche d'identité
            // (nom, logo, adresse, téléphone, NIF/STAT, pied de reçu) qui alimente tous les PDF.
            // Un lien évite de dupliquer la saisie à deux endroits et allège cet écran.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNavigateToEpicerie() }
                    .testTag("settings_epicerie_button"),
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
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = storeNameLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                text = groceryNameVal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = mainTextColor
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Logo, adiresy, telefaonina, NIF/STAT — miseho amin'ny PDF"
                                    "fr" -> "Logo, adresse, téléphone, NIF/STAT — repris sur les PDF"
                                    else -> "Logo, address, phone, tax IDs — printed on every PDF"
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
                                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
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
                                containerColor = if (isLightSel) themeColor else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isLightSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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
                                containerColor = if (isDarkSel) themeColor else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isDarkSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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
                                containerColor = if (isSystemSel) themeColor else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSystemSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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
                                    containerColor = if (isSelected) themeColor else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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

                    // Grille 2 x 2 plutôt qu'une seule rangée : à quatre métiers, des boutons
                    // côte à côte deviendraient trop étroits pour rester lisibles sur un téléphone.
                    modesDisponibles.chunked(2).forEach { rangee ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rangee.forEach { (mode, libelle) ->
                                val estActif = shopModeVal == mode
                                Button(
                                    onClick = { viewModel.updateShopMode(mode) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("shop_mode_${mode.cle}"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (estActif) themeColor else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (estActif) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (estActif) Color.Transparent else cardBorderColor
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = libelle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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

            // CARTE VERS SÉCURITÉ & SURVEILLANCE (alertes anti-triche + journal d'audit)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNavigateToSecurite() }
                    .testTag("settings_securite_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
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
                                .background(MaterialTheme.colorScheme.error),
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
                                    "mg" -> "Fiarovana sy Fanaraha-maso"
                                    "fr" -> "Sécurité & Surveillance"
                                    else -> "Security & Monitoring"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Fampitandremana sy tantaran'ny asa nataon'ny mpiasa"
                                    "fr" -> "Alertes et journal des gestes sensibles des employés"
                                    else -> "Alerts and audit journal of sensitive actions"
                                },
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // CARTE VERS LE PLAN D'ÉTAGÈRES (rayons interactifs, cliquer une case pour voir les
            // produits qui y sont rangés)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNavigateToEtagere() }
                    .testTag("settings_etagere_button"),
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
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Fandrindrana ny efitrano fivarotana"
                                    "fr" -> "Plan d'étagères"
                                    else -> "Shelf plan"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Rayons sy efitra, tsindrio hijery ny entana ao anatiny"
                                    "fr" -> "Rayons et cases, tapez pour voir les produits rangés"
                                    else -> "Shelves and slots, tap to see what's stored there"
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

            // C.4: CARD REDIRECT TO EXPIRY ALERTS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNavigateToPeremption() }
                    .testTag("settings_peremption_button"),
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
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Fetr'andro Peremptiona"
                                    "fr" -> "Alertes de péremption"
                                    else -> "Expiry alerts"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Fitantanana ny lots sy ny fetr'androny"
                                    "fr" -> "Gérer les lots et leurs dates de péremption"
                                    else -> "Manage batches and their expiry dates"
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

                    Text(
                        text = when (activeLang) {
                            "mg" -> "Tsy manana Firebase na mailaka? Alefaso amin'ny WhatsApp, Bluetooth, na carte SD ny rakitra hitahirizana, ary alao amin'ny finday hafa."
                            "fr" -> "Pas de Firebase ni d'email ? Envoie le fichier de sauvegarde par WhatsApp, Bluetooth ou carte SD, et importe-le sur un autre téléphone."
                            else -> "No Firebase or email? Send the backup file via WhatsApp, Bluetooth or SD card, and import it on another phone."
                        },
                        fontSize = 11.sp,
                        color = secondaryTextColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Share backup file button (WhatsApp, Bluetooth, SD card, USB...)
                        OutlinedButton(
                            onClick = {
                                // Sur Android 9 et antérieur, le dépôt dans Téléchargements exige
                                // une autorisation : on la demande ici, au moment où le geste a du
                                // sens pour le gérant. Le partage lui-même n'en a jamais besoin, et
                                // a donc lieu quel que soit le verdict — c'est le message final qui
                                // dit la vérité sur ce qui a réellement été enregistré.
                                if (BackupHelper.autorisationStockageRequise(context)) {
                                    demandeStockageLauncher.launch(
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                } else {
                                    partagerSauvegarde()
                                }
                            },
                            modifier = Modifier.weight(1f).height(40.dp).testTag("share_backup_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Hizara"
                                    "fr" -> "Partager"
                                    else -> "Share"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Import backup file button
                        OutlinedButton(
                            onClick = { importBackupLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f).height(40.dp).testTag("import_backup_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Importer"
                                    "fr" -> "Importer"
                                    else -> "Import"
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

                    // La sauvegarde cloud est active dès l'installation : sans URL saisie, elle part
                    // vers le serveur Varotra. Le message est donc affiché en permanence, en
                    // précisant simplement où atterrissent les données.
                    Text(
                        text = if (firebaseDatabaseUrlVal.isBlank()) {
                            when (activeLang) {
                                "mg" -> "✓ Mandeha ho azy ny backup any amin'ny serveur Varotra rehefa misy internet."
                                "fr" -> "✓ Sauvegarde automatique sur le serveur Varotra dès que le téléphone a internet."
                                else -> "✓ Automatic backup to the Varotra server whenever the phone has internet."
                            }
                        } else {
                            when (activeLang) {
                                "mg" -> "✓ Mandeha ho azy ny backup any amin'ny base-nao manokana."
                                "fr" -> "✓ Sauvegarde automatique sur votre propre base Firebase."
                                else -> "✓ Automatic backup to your own Firebase database."
                            }
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )

                    val lastBackup by viewModel.lastCloudBackupTime.collectAsState()
                    if (lastBackup > 0L) {
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Backup farany: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date(lastBackup))}"
                                "fr" -> "Dernière sauvegarde : ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date(lastBackup))}"
                                else -> "Last backup: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date(lastBackup))}"
                            },
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }

                    HorizontalDivider(color = cardBorderColor.copy(alpha = 0.5f))

                    // CODE DE RÉCUPÉRATION — sans lui, une sauvegarde cloud ne sert à rien : c'est
                    // la seule adresse qui permet de la retrouver depuis un autre téléphone.
                    val recoveryCodeVal by viewModel.recoveryCode.collectAsState()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Kaody famerenana (soraty an-taratasy!)"
                                "fr" -> "Code de récupération (notez-le sur un papier !)"
                                else -> "Recovery code (write it down!)"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainTextColor
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = recoveryCodeVal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = themeColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeColor.copy(alpha = 0.10f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("recovery_code_text")
                            )
                            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                            IconButton(onClick = {
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(recoveryCodeVal))
                                snackbarMessage = when (activeLang) {
                                    "mg" -> "Voadika ny kaody."
                                    "fr" -> "Code copié."
                                    else -> "Code copied."
                                }
                                showSnackbar = true
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = themeColor)
                            }
                        }
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Raha very na simba ny finday, ity kaody ity ihany no ahafahana mamerina ny angona. Alefaso amin'ny mpivarotra koa izy."
                                "fr" -> "Si le téléphone est perdu ou cassé, ce code est le seul moyen de retrouver vos données. Communiquez-le aussi à votre fournisseur."
                                else -> "If the phone is lost or broken, this code is the only way to get your data back. Also send it to your vendor."
                            },
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                        OutlinedButton(
                            onClick = {
                                recoveryCodeInput = ""
                                recoveryCodeError = false
                                showRecoveryCodeDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().testTag("enter_recovery_code_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Nanolo finday? Ampidiro ny kaody taloha"
                                    "fr" -> "Nouveau téléphone ? Saisir mon ancien code"
                                    else -> "New phone? Enter my previous code"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = cardBorderColor.copy(alpha = 0.5f))

                    OutlinedTextField(
                        value = firebaseDatabaseUrlInput,
                        onValueChange = { firebaseDatabaseUrlInput = it },
                        label = {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "URL Firebase manokana (tsy voatery)"
                                    "fr" -> "URL Firebase personnelle (facultatif)"
                                    else -> "Own Firebase URL (optional)"
                                }
                            )
                        },
                        placeholder = { Text("https://mon-projet-default-rtdb.firebaseio.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("firebase_database_url_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text(
                        text = when (activeLang) {
                            "mg" -> "Avelao foana ity toerana ity: mandeha ho azy ny backup any amin'ny serveur Varotra. Raha tianao ny hitahiry ny angona any amin'ny Firebase anao manokana, ampidiro eto ny URL-n'ny Realtime Database-nao."
                            "fr" -> "Laissez ce champ vide : la sauvegarde se fait automatiquement sur le serveur Varotra, vous n'avez rien à créer. Ne le remplissez que si vous voulez héberger vos données sur votre propre projet Firebase (Realtime Database)."
                            else -> "Leave this field empty: backups go to the Varotra server automatically, nothing to set up. Fill it in only if you want to host your data on your own Firebase project (Realtime Database)."
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
                                // Champ vide = sauvegarde sur le serveur Varotra (comportement par
                                // défaut) : on ne bloque donc plus l'utilisateur, on résout l'URL.
                                run {
                                    viewModel.updateFirebaseDatabaseUrl(firebaseDatabaseUrlInput.trim())
                                    val urlEffective = FirebaseBackupManager.resolveDatabaseUrl(firebaseDatabaseUrlInput)
                                    isCloudBackupLoading = true
                                    coroutineScope.launch {
                                        val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            viewModel.getFullDatabaseJsonSync()
                                        }
                                        val result = FirebaseBackupManager.uploadBackup(urlEffective, firebaseBackupToken, json)
                                        isCloudBackupLoading = false
                                        snackbarMessage = if (result.isSuccess) {
                                            when (activeLang) {
                                                "mg" -> "Tafita! Voatahiry any amin'ny rahona ny tahiry-nao."
                                                "fr" -> "Sauvegarde Cloud réussie ! Vos données sont en ligne."
                                                else -> "Cloud backup successful! Your data is now online."
                                            }
                                        } else {
                                            when (activeLang) {
                                                "mg" -> "Hadisoana: tsy voatahiry any amin'ny rahona (jereo ny URL sy ny internet)."
                                                "fr" -> "Échec de la sauvegarde Cloud (vérifiez l'URL et la connexion internet)."
                                                else -> "Cloud backup failed (check the database URL and internet connection)."
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
                                run {
                                    viewModel.updateFirebaseDatabaseUrl(firebaseDatabaseUrlInput.trim())
                                    isCloudRestoreLoading = true
                                    coroutineScope.launch {
                                        val resultat = viewModel.restaurerDepuisCloud()
                                        isCloudRestoreLoading = false
                                        snackbarMessage = messageResultatRestauration(resultat, activeLang)
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

            if (showRecoveryCodeDialog) {
                AlertDialog(
                    onDismissRequest = { showRecoveryCodeDialog = false },
                    title = {
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Mamerina angona avy amin'ny finday taloha"
                                "fr" -> "Récupérer les données d'un ancien téléphone"
                                else -> "Recover data from a previous phone"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Ampidiro ny kaody famerenana an'ilay finday taloha, avy eo tsindrio \"Restaurer\"."
                                    "fr" -> "Saisissez le code de récupération de l'ancien téléphone, puis appuyez sur « Restaurer »."
                                    else -> "Enter the previous phone's recovery code, then tap \"Restore\"."
                                },
                                fontSize = 13.sp
                            )
                            OutlinedTextField(
                                value = recoveryCodeInput,
                                onValueChange = {
                                    recoveryCodeInput = it
                                    recoveryCodeError = false
                                },
                                placeholder = { Text("VRT-A3F9-K2M7-QP4X") },
                                isError = recoveryCodeError,
                                singleLine = true,
                                supportingText = {
                                    if (recoveryCodeError) {
                                        Text(
                                            text = when (activeLang) {
                                                "mg" -> "Diso ny kaody. Hamarino tsara ny litera sy isa."
                                                "fr" -> "Code invalide. Vérifiez lettres et chiffres."
                                                else -> "Invalid code. Check letters and digits."
                                            },
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("recovery_code_input")
                            )
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Tsy voatahiry ao amin'ny backup ny sarin'ny entana. Ho tadiavina ho azy amin'ny alalan'ny code-barres izy ireo aorian'ny famerenana."
                                    "fr" -> "Les photos ne sont pas sauvegardées. Celles des produits à code-barres seront retrouvées automatiquement après la restauration."
                                    else -> "Photos are not backed up. Those of barcoded products will be fetched automatically after restoring."
                                },
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            recoveryCodeError = false
                            isCloudRestoreLoading = true
                            viewModel.updateFirebaseDatabaseUrl(firebaseDatabaseUrlInput.trim())
                            coroutineScope.launch {
                                val resultat = viewModel.restaurerDepuisCloud(recoveryCodeInput)
                                isCloudRestoreLoading = false
                                if (resultat == InventoryViewModel.ResultatRestaurationCloud.CODE_INVALIDE) {
                                    // Rien n'est modifié sur une saisie invalide (voir
                                    // appliquerCodeRecuperation) : le dialogue reste ouvert avec
                                    // l'erreur inline, exactement comme avant ce refactor.
                                    recoveryCodeError = true
                                } else {
                                    showRecoveryCodeDialog = false
                                    snackbarMessage = messageResultatRestauration(resultat, activeLang)
                                    showSnackbar = true
                                }
                            }
                        }) {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Haverina"
                                    "fr" -> "Restaurer"
                                    else -> "Restore"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRecoveryCodeDialog = false }) {
                            Text(LanguageManager.translate("cancel_btn", activeLang))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // B.3/E.2: opt-in employee accounts (PIN vendeur/gérant)
            VendeurRolesCard(
                viewModel = viewModel,
                vendeurs = allVendeursVal,
                activeLang = activeLang,
                mainTextColor = mainTextColor,
                secondaryTextColor = secondaryTextColor,
                cardBg = cardBg,
                cardBorderColor = cardBorderColor,
                themeColor = themeColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            UpdateCard(activeLang = activeLang, themeColor = themeColor)

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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
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
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// B.3/E.2: opt-in employee accounts (PIN vendeur/gérant). An empty list means the feature is
// inactive — no login prompt or restriction appears anywhere else in the app.
@Composable
private fun VendeurRolesCard(
    viewModel: InventoryViewModel,
    vendeurs: List<Vendeur>,
    activeLang: String,
    mainTextColor: Color,
    secondaryTextColor: Color,
    cardBg: Color,
    cardBorderColor: Color,
    themeColor: Color
) {
    var editingVendeur by remember { mutableStateOf<Vendeur?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var vendeurToDelete by remember { mutableStateOf<Vendeur?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("vendeur_roles_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(themeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Kaonty & Andraikitra"
                            "fr" -> "Comptes & Rôles"
                            else -> "Accounts & Roles"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainTextColor
                    )
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Safidy: ampiasao raha misy mpiasa maromaro mampiasa ity finday ity"
                            "fr" -> "Optionnel : utile si plusieurs employés utilisent cet appareil"
                            else -> "Optional: useful if several employees share this device"
                        },
                        fontSize = 11.sp,
                        color = secondaryTextColor
                    )
                }
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_vendeur_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = themeColor)
                }
            }

            if (vendeurs.isNotEmpty()) {
                HorizontalDivider(color = cardBorderColor.copy(alpha = 0.5f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vendeurs.forEach { vendeur ->
                        Row(
                            modifier = Modifier.fillMaxWidth().testTag("vendeur_row_${vendeur.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = vendeur.nom,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = mainTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (vendeur.role == Vendeur.ROLE_GERANT) themeColor.copy(alpha = 0.15f) else secondaryTextColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (vendeur.role == Vendeur.ROLE_GERANT) {
                                                when (activeLang) { "mg" -> "Gerànta"; "fr" -> "Gérant"; else -> "Manager" }
                                            } else {
                                                when (activeLang) { "mg" -> "Mpivarotra"; "fr" -> "Vendeur"; else -> "Cashier" }
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (vendeur.role == Vendeur.ROLE_GERANT) themeColor else secondaryTextColor
                                        )
                                    }
                                    if (!vendeur.actif) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = when (activeLang) { "mg" -> "Tsy mavitrika"; "fr" -> "Inactif"; else -> "Inactive" },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                            Row {
                                IconButton(onClick = { editingVendeur = vendeur }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { vendeurToDelete = vendeur }) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        VendeurEditDialog(
            viewModel = viewModel,
            vendeur = null,
            activeLang = activeLang,
            themeColor = themeColor,
            onDismiss = { showAddDialog = false }
        )
    }
    editingVendeur?.let { v ->
        VendeurEditDialog(
            viewModel = viewModel,
            vendeur = v,
            activeLang = activeLang,
            themeColor = themeColor,
            onDismiss = { editingVendeur = null }
        )
    }
    vendeurToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { vendeurToDelete = null },
            title = {
                Text(
                    text = when (activeLang) { "mg" -> "Hamafa"; "fr" -> "Supprimer"; else -> "Delete" },
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("${v.nom} ?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteVendeur(v); vendeurToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(when (activeLang) { "mg" -> "Hamafa"; "fr" -> "Supprimer"; else -> "Delete" }, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vendeurToDelete = null }) {
                    Text(when (activeLang) { "mg" -> "Hanafoana"; "fr" -> "Annuler"; else -> "Cancel" })
                }
            }
        )
    }
}

@Composable
private fun VendeurEditDialog(
    viewModel: InventoryViewModel,
    vendeur: Vendeur?,
    activeLang: String,
    themeColor: Color,
    onDismiss: () -> Unit
) {
    var nom by remember { mutableStateOf(vendeur?.nom ?: "") }
    var role by remember { mutableStateOf(vendeur?.role ?: Vendeur.ROLE_VENDEUR) }
    var actif by remember { mutableStateOf(vendeur?.actif ?: true) }
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }

    val isEditing = vendeur != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) {
                    when (activeLang) { "mg" -> "Hanova kaonty"; "fr" -> "Modifier le compte"; else -> "Edit account" }
                } else {
                    when (activeLang) { "mg" -> "Kaonty vaovao"; "fr" -> "Nouveau compte"; else -> "New account" }
                },
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it; nameError = it.isBlank() },
                    label = { Text(when (activeLang) { "mg" -> "Anarana"; "fr" -> "Nom"; else -> "Name" }) },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("vendeur_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = role == Vendeur.ROLE_VENDEUR,
                        onClick = { role = Vendeur.ROLE_VENDEUR },
                        label = { Text(when (activeLang) { "mg" -> "Mpivarotra"; "fr" -> "Vendeur"; else -> "Cashier" }) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = role == Vendeur.ROLE_GERANT,
                        onClick = { role = Vendeur.ROLE_GERANT },
                        label = { Text(when (activeLang) { "mg" -> "Gerànta"; "fr" -> "Gérant"; else -> "Manager" }) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) { pin = it.filter { c -> c.isDigit() }; pinError = false } },
                    label = {
                        Text(
                            if (isEditing) {
                                when (activeLang) { "mg" -> "PIN vaovao (safidy)"; "fr" -> "Nouveau PIN (optionnel)"; else -> "New PIN (optional)" }
                            } else {
                                when (activeLang) { "mg" -> "PIN (isa 4-6)"; "fr" -> "PIN (4 à 6 chiffres)"; else -> "PIN (4-6 digits)" }
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = pinError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("vendeur_pin_input")
                )

                OutlinedTextField(
                    value = pinConfirm,
                    onValueChange = { if (it.length <= 6) pinConfirm = it.filter { c -> c.isDigit() } },
                    label = { Text(when (activeLang) { "mg" -> "Hamarino ny PIN"; "fr" -> "Confirmer le PIN"; else -> "Confirm PIN" }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = pinError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("vendeur_pin_confirm_input")
                )

                if (isEditing) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = actif, onCheckedChange = { actif = it })
                        Text(
                            text = when (activeLang) { "mg" -> "Mavitrika"; "fr" -> "Compte actif"; else -> "Active account" },
                            fontSize = 13.sp
                        )
                    }
                }

                if (pinError) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Tsy mitovy ny PIN na tsy ampy 4 isa"
                            "fr" -> "Les PIN ne correspondent pas ou sont trop courts (4 min.)"
                            else -> "PINs don't match or are too short (min. 4)"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nameClean = nom.trim()
                    nameError = nameClean.isEmpty()

                    val pinProvided = pin.isNotEmpty() || pinConfirm.isNotEmpty()
                    val needsPin = !isEditing || pinProvided
                    pinError = needsPin && (pin.length < 4 || pin != pinConfirm)

                    if (!nameError && !pinError) {
                        val pinHash = if (pinProvided) Vendeur.hashPin(pin) else (vendeur?.pinHash ?: "")
                        val base = vendeur ?: Vendeur(nom = nameClean, pinHash = pinHash, role = role)
                        val toSave = base.copy(
                            nom = nameClean,
                            pinHash = pinHash,
                            role = role,
                            actif = actif
                        )
                        viewModel.saveVendeur(toSave)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("vendeur_save_button")
            ) {
                Text(when (activeLang) { "mg" -> "Hitahiry"; "fr" -> "Enregistrer"; else -> "Save" }, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(when (activeLang) { "mg" -> "Hanafoana"; "fr" -> "Annuler"; else -> "Cancel" })
            }
        }
    )
}

/**
 * Mise à jour interne de l'app (voir [UpdateManager]) : vérifie le manifeste publié sur R2,
 * télécharge l'APK avec une barre de progression, puis lance l'installateur système. Toujours à
 * l'initiative de l'utilisateur — aucune vérification ni téléchargement automatique au démarrage,
 * conformément au choix du gérant que les mises à jour restent facultatives.
 *
 * Utilise les tokens Material3 (`MaterialTheme.colorScheme.*`) plutôt que les couleurs codées en
 * dur encore présentes ailleurs dans cet écran (voir AGENTS.md §57) : ce composant est neuf, pas
 * de dette à reproduire.
 */
@Composable
private fun UpdateCard(activeLang: String, themeColor: Color) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<InfoMiseAJour?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadedFile by remember { mutableStateOf<java.io.File?>(null) }
    var bannerMessage by remember { mutableStateOf("") }
    var bannerIsError by remember { mutableStateOf(false) }

    val installSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Retour depuis l'écran système "Autoriser cette source" : retenter l'installation si
        // l'utilisateur vient de l'accorder, sinon ne rien faire (il a le fichier prêt, le bouton
        // Installer reste disponible pour réessayer).
        val fichier = downloadedFile
        if (fichier != null && UpdateManager.autorisationInstallationAccordee(context)) {
            UpdateManager.lancerInstallation(context, fichier)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("update_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = themeColor)
                Text(
                    text = when (activeLang) {
                        "mg" -> "Fanavaozana ny app"
                        "fr" -> "Mise à jour de l'application"
                        else -> "App update"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = when (activeLang) {
                    "mg" -> "Version ankehitriny: ${BuildConfig.VERSION_NAME}"
                    "fr" -> "Version actuelle : ${BuildConfig.VERSION_NAME}"
                    else -> "Current version: ${BuildConfig.VERSION_NAME}"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (bannerMessage.isNotEmpty()) {
                val bg = if (bannerIsError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                val fg = if (bannerIsError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .testTag("update_message"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (bannerIsError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(text = bannerMessage, fontSize = 12.sp, color = fg)
                }
            }

            val infoDisponible = updateInfo
            if (infoDisponible != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Version vaovao: ${infoDisponible.versionName}"
                            "fr" -> "Nouvelle version disponible : ${infoDisponible.versionName}"
                            else -> "New version available: ${infoDisponible.versionName}"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (infoDisponible.notesVersion.isNotBlank()) {
                        Text(text = infoDisponible.notesVersion, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = downloadProgress,
                            modifier = Modifier.fillMaxWidth().testTag("update_progress"),
                            color = themeColor
                        )
                    } else if (downloadedFile != null) {
                        Button(
                            onClick = {
                                val fichier = downloadedFile ?: return@Button
                                if (UpdateManager.autorisationInstallationAccordee(context)) {
                                    UpdateManager.lancerInstallation(context, fichier)
                                } else {
                                    installSettingsLauncher.launch(UpdateManager.intentParametresAutorisationInstallation(context))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("update_install_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Ampidino"
                                    "fr" -> "Installer"
                                    else -> "Install"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isDownloading = true
                                downloadProgress = 0f
                                coroutineScope.launch {
                                    val resultat = UpdateManager.telechargerApk(context, infoDisponible) { progres ->
                                        downloadProgress = progres
                                    }
                                    isDownloading = false
                                    resultat.fold(
                                        onSuccess = { fichier -> downloadedFile = fichier },
                                        onFailure = {
                                            bannerIsError = true
                                            bannerMessage = when (activeLang) {
                                                "mg" -> "Tsy voaray ny fanavaozana. Andramo indray."
                                                "fr" -> "Échec du téléchargement de la mise à jour. Réessayez."
                                                else -> "Failed to download the update. Try again."
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("update_download_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Alaina"
                                    "fr" -> "Télécharger"
                                    else -> "Download"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        isChecking = true
                        bannerMessage = ""
                        coroutineScope.launch {
                            val resultat = UpdateManager.verifierMiseAJourDisponible(BuildConfig.VERSION_CODE)
                            isChecking = false
                            resultat.fold(
                                onSuccess = { info ->
                                    if (info != null) {
                                        updateInfo = info
                                    } else {
                                        bannerIsError = false
                                        bannerMessage = when (activeLang) {
                                            "mg" -> "Efa ny version farany no eo aminao."
                                            "fr" -> "Vous avez déjà la dernière version."
                                            else -> "You already have the latest version."
                                        }
                                    }
                                },
                                onFailure = {
                                    bannerIsError = true
                                    bannerMessage = when (activeLang) {
                                        "mg" -> "Tsy afaka nanamarina raha misy fanavaozana (jereo ny internet)."
                                        "fr" -> "Impossible de vérifier les mises à jour (vérifiez votre connexion internet)."
                                        else -> "Could not check for updates (check your internet connection)."
                                    }
                                }
                            )
                        }
                    },
                    enabled = !isChecking,
                    modifier = Modifier.fillMaxWidth().testTag("update_check_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = themeColor, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Hijery raha misy fanavaozana"
                                "fr" -> "Vérifier les mises à jour"
                                else -> "Check for updates"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
