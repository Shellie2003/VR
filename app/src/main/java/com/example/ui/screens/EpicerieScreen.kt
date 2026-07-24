package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.LanguageManager
import com.example.util.ShopLogoUtil

/**
 * Écran « Mon épicerie » : tout ce qui décrit le commerce lui-même au même endroit — nom, logo,
 * coordonnées, identifiants fiscaux, pied de page des reçus, et le nom de CET appareil.
 *
 * Ces informations étaient auparavant éparpillées (le nom dans Paramètres, rien pour le reste) et
 * alourdissaient un écran d'accueil qui doit rester une caisse. Elles sont regroupées ici parce
 * qu'elles partagent un seul usage : identifier l'épicerie sur les documents qu'elle imprime.
 * Tout ce qui est saisi ici apparaît automatiquement en en-tête de chaque PDF (reçus de caisse,
 * rapports, exports).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpicerieScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeLang by viewModel.language.collectAsState()

    val groceryNameVal by viewModel.groceryName.collectAsState()
    val addressVal by viewModel.shopAddress.collectAsState()
    val phoneVal by viewModel.shopPhone.collectAsState()
    val nifVal by viewModel.shopNif.collectAsState()
    val statVal by viewModel.shopStat.collectAsState()
    val footerVal by viewModel.shopReceiptFooter.collectAsState()
    val logoPath by viewModel.shopLogoPath.collectAsState()
    val deviceNameVal by viewModel.deviceName.collectAsState()

    var nameInput by remember(groceryNameVal) { mutableStateOf(groceryNameVal) }
    var addressInput by remember(addressVal) { mutableStateOf(addressVal) }
    var phoneInput by remember(phoneVal) { mutableStateOf(phoneVal) }
    var nifInput by remember(nifVal) { mutableStateOf(nifVal) }
    var statInput by remember(statVal) { mutableStateOf(statVal) }
    var footerInput by remember(footerVal) { mutableStateOf(footerVal) }
    var deviceNameInput by remember(deviceNameVal) { mutableStateOf(deviceNameVal) }

    var message by remember { mutableStateOf<String?>(null) }

    // Le logo est relu depuis le disque à chaque changement de chemin : c'est un fichier unique et
    // léger (512 px max), la relecture est instantanée et évite de garder un bitmap périmé.
    val logoBitmap = remember(logoPath) { ShopLogoUtil.chargerLogo(logoPath) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val ok = viewModel.importerLogo(uri)
            message = if (ok) {
                when (activeLang) {
                    "mg" -> "Voaray ny logo."
                    "fr" -> "Logo importé."
                    else -> "Logo imported."
                }
            } else {
                when (activeLang) {
                    "mg" -> "Tsy voavaky ilay sary."
                    "fr" -> "Image illisible."
                    else -> "Unreadable image."
                }
            }
        }
    }

    val titre = LanguageManager.translate("shop_info_title", activeLang)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(titre, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("epicerie_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Logo ---
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Logo (miseho amin'ny rakitra PDF rehetra)"
                            "fr" -> "Logo (apparaît sur tous les PDF)"
                            else -> "Logo (shown on every PDF)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(8.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { logoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("import_logo_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Hampiditra"
                                    "fr" -> "Importer"
                                    else -> "Import"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (logoBitmap != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.supprimerLogo()
                                    message = when (activeLang) {
                                        "mg" -> "Voafafa ny logo."
                                        "fr" -> "Logo supprimé."
                                        else -> "Logo removed."
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(LanguageManager.translate("delete_btn", activeLang))
                            }
                        }
                    }
                }
            }

            // --- Identité ---
            SectionEpicerie(
                titre = when (activeLang) {
                    "mg" -> "Mombamomba"
                    "fr" -> "Identité"
                    else -> "Identity"
                }
            ) {
                ChampEpicerie(
                    valeur = nameInput,
                    onChange = { nameInput = it },
                    label = when (activeLang) {
                        "mg" -> "Anaran'ny fivarotana"
                        "fr" -> "Nom de l'épicerie"
                        else -> "Shop name"
                    },
                    icone = Icons.Default.Store,
                    testTag = "shop_name_input"
                )
                ChampEpicerie(
                    valeur = addressInput,
                    onChange = { addressInput = it },
                    label = when (activeLang) {
                        "mg" -> "Adiresy"
                        "fr" -> "Adresse"
                        else -> "Address"
                    },
                    icone = Icons.Default.LocationOn,
                    testTag = "shop_address_input"
                )
                ChampEpicerie(
                    valeur = phoneInput,
                    onChange = { phoneInput = it },
                    label = when (activeLang) {
                        "mg" -> "Laharana finday"
                        "fr" -> "Téléphone"
                        else -> "Phone"
                    },
                    icone = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone,
                    testTag = "shop_phone_input"
                )
            }

            // --- Identifiants fiscaux ---
            SectionEpicerie(
                titre = when (activeLang) {
                    "mg" -> "Laharana ara-panjakana"
                    "fr" -> "Identifiants fiscaux"
                    else -> "Tax identifiers"
                }
            ) {
                ChampEpicerie(
                    valeur = nifInput,
                    onChange = { nifInput = it },
                    label = "NIF",
                    icone = Icons.Default.Badge,
                    testTag = "shop_nif_input"
                )
                ChampEpicerie(
                    valeur = statInput,
                    onChange = { statInput = it },
                    label = "STAT",
                    icone = Icons.Default.Numbers,
                    testTag = "shop_stat_input"
                )
            }

            // --- Reçus ---
            SectionEpicerie(
                titre = when (activeLang) {
                    "mg" -> "Rosia"
                    "fr" -> "Reçus"
                    else -> "Receipts"
                }
            ) {
                ChampEpicerie(
                    valeur = footerInput,
                    onChange = { footerInput = it },
                    label = when (activeLang) {
                        "mg" -> "Teny farany amin'ny rosia"
                        "fr" -> "Message en bas des reçus"
                        else -> "Receipt footer message"
                    },
                    icone = Icons.Default.Receipt,
                    testTag = "shop_footer_input"
                )
            }

            Button(
                onClick = {
                    viewModel.updateShopInfo(
                        nom = nameInput,
                        adresse = addressInput,
                        telephone = phoneInput,
                        nif = nifInput,
                        stat = statInput,
                        piedDePage = footerInput
                    )
                    message = when (activeLang) {
                        "mg" -> "Voatahiry."
                        "fr" -> "Enregistré."
                        else -> "Saved."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_shop_info_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (activeLang) {
                        "mg" -> "Hitahiry"
                        "fr" -> "Enregistrer"
                        else -> "Save"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            // --- Appareil : volontairement séparé, ce n'est PAS une information sur l'épicerie
            // mais sur le poste qui l'utilise, et c'est ce qui signe chaque transaction. ---
            SectionEpicerie(
                titre = when (activeLang) {
                    "mg" -> "Ity finday ity"
                    "fr" -> "Cet appareil"
                    else -> "This device"
                }
            ) {
                ChampEpicerie(
                    valeur = deviceNameInput,
                    onChange = { deviceNameInput = it },
                    label = LanguageManager.translate("device_name_label", activeLang),
                    icone = Icons.Default.PhoneAndroid,
                    testTag = "device_name_input"
                )
                Text(
                    text = LanguageManager.translate("device_name_hint", activeLang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                OutlinedButton(
                    onClick = {
                        viewModel.updateDeviceName(deviceNameInput)
                        message = when (activeLang) {
                            "mg" -> "Voaova ny anaran'ity finday ity."
                            "fr" -> "Nom de l'appareil mis à jour."
                            else -> "Device name updated."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_device_name_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Hanova ny anarana"
                            "fr" -> "Renommer l'appareil"
                            else -> "Rename device"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    message?.let { texte ->
        LaunchedEffect(texte) {
            android.widget.Toast.makeText(context, texte, android.widget.Toast.LENGTH_SHORT).show()
            message = null
        }
    }
}

@Composable
private fun SectionEpicerie(titre: String, contenu: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = titre.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            contenu()
        }
    }
}

@Composable
private fun ChampEpicerie(
    valeur: String,
    onChange: (String) -> Unit,
    label: String,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icone, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp)
    )
}
