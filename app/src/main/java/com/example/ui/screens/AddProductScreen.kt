package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
import com.example.ui.components.BarcodeScannerView
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Fournisseur
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.LanguageManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: InventoryViewModel,
    editingProduct: Product?,
    onSaveProduct: (Product) -> Unit,
    onCancel: () -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val t = { key: String -> LanguageManager.translate(key, activeLang) }

    // Tablet/large-screen layout: cap the form's width and center it instead of letting every
    // field stretch edge-to-edge on a wide screen.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Suppliers (Fournisseurs) state
    val suppliers by viewModel.allFournisseurs.collectAsState()
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var newSupplierContact by remember { mutableStateOf("") }
    var newSupplierAddress by remember { mutableStateOf("") }

    // Core identification state
    var name by remember(editingProduct) { mutableStateOf(editingProduct?.name ?: "") }
    var nomCourt by remember(editingProduct) { mutableStateOf(editingProduct?.nomCourt ?: "") }
    var sousCategorie by remember(editingProduct) { mutableStateOf(editingProduct?.sousCategorie ?: "") }
    var marque by remember(editingProduct) { mutableStateOf(editingProduct?.marque ?: "") }
    var description by remember(editingProduct) { mutableStateOf(editingProduct?.description ?: "") }

    // Stock & Pricing state
    var priceStr by remember(editingProduct) { mutableStateOf(editingProduct?.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var stockStr by remember(editingProduct) { mutableStateOf(editingProduct?.stock?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var lowStockThresholdStr by remember(editingProduct) { mutableStateOf(editingProduct?.lowStockThreshold?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "5") }
    var stockMaxStr by remember(editingProduct) { mutableStateOf(editingProduct?.stockMax?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var emplacement by remember(editingProduct) { mutableStateOf(editingProduct?.emplacement ?: "") }
    var prixAchatUniteBaseStr by remember(editingProduct) { mutableStateOf(editingProduct?.prixAchatUniteBase?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var wholesalePriceStr by remember(editingProduct) { mutableStateOf(editingProduct?.wholesalePrice?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }

    // Suppliers association state
    var selectedFournisseurId by remember(editingProduct) { mutableStateOf(editingProduct?.fournisseurId) }
    var showSupplierDropdown by remember { mutableStateOf(false) }

    // Traceability & Fiscal state
    var barcode by remember(editingProduct) { mutableStateOf(editingProduct?.barcode ?: "") }
    var sku by remember(editingProduct) { mutableStateOf(editingProduct?.sku ?: "") }
    var gerePeremption by remember(editingProduct) { mutableStateOf(editingProduct?.gerePeremption ?: false) }
    var taxable by remember(editingProduct) { mutableStateOf(editingProduct?.taxable ?: false) }
    var tauxTaxeStr by remember(editingProduct) { mutableStateOf(editingProduct?.tauxTaxe?.toString() ?: "20.0") }
    var imageUrl by remember(editingProduct) { mutableStateOf(editingProduct?.imageUrl ?: "") }

    // UI State flags
    var showLiveScanner by remember { mutableStateOf(false) }
    var wholesalePriceError by remember { mutableStateOf(false) }
    var stockQuantityStr by remember(editingProduct) { mutableStateOf(editingProduct?.stock_quantity?.toString() ?: "0") }
    var stockQuantityError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Open Food Facts states and helpers
    val coroutineScope = rememberCoroutineScope()
    var showOffSearch by remember { mutableStateOf(false) }
    var showTemplateSearch by remember { mutableStateOf(false) }
    var offSearchQuery by remember { mutableStateOf("") }
    var offSearchResults by remember { mutableStateOf<List<com.example.util.OpenFoodFactsApi.OffProduct>>(emptyList()) }
    var offSearchLoading by remember { mutableStateOf(false) }
    var offSearchError by remember { mutableStateOf<String?>(null) }
    var offOnlyMadagascar by remember { mutableStateOf(true) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val localPath = saveBitmapToLocalFile(context, bitmap)
            if (localPath != null) {
                imageUrl = "file://$localPath"
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val localPath = saveUriToLocalFile(context, uri)
            if (localPath != null) {
                imageUrl = "file://$localPath"
            }
        }
    }

    // Dropdown Categories
    val standardCategories = listOf(
        "Épicerie - Farine & Boulangerie",
        "Épicerie - Pâtes alimentaires",
        "Épicerie - Huiles de cuisine",
        "Épicerie - Lait & Produits laitiers",
        "Épicerie - Margarine & Matières grasses",
        "Épicerie - Conserves",
        "Épicerie - Condiments, Épices & Assaisonnements",
        "Épicerie - Confiseries & Snacks",
        "Boissons - Café & Thé",
        "Boissons - Jus",
        "Hygiène & Beauté",
        "Bébé - Couches & Lingettes",
        "Entretien ménager & Nettoyage",
        "Entretien chaussures (Cirage)",
        "Désodorisants & Parfums d'ambiance",
        "Papeterie & Fournitures scolaires",
        "Éclairage (Ampoules)",
        "Bougies",
        "Allumettes & Briquets",
        "Insecticides & Anti-moustiques",
        "Piles électriques",
        "Quincaillerie",
        "Lubrifiants & Fluides moteur",
        "Autres / Divers",
        "Hafa"
    )
    var selectedCategory by remember(editingProduct) {
        val cat = editingProduct?.category ?: "Épicerie - Farine & Boulangerie"
        mutableStateOf(if (standardCategories.contains(cat)) cat else "Hafa")
    }
    var customCategory by remember(editingProduct) {
        val cat = editingProduct?.category ?: ""
        mutableStateOf(if (!standardCategories.contains(cat)) cat else "")
    }

    // Units
    val units = listOf("Pièce", "Litre", "Kilogramme", "Paquet", "Carton", "Sac", "Boîte", "Bouteille", "Tasse/Kapoaka")
    var selectedUnit by remember(editingProduct) {
        mutableStateOf(editingProduct?.unit ?: "Pièce")
    }

    var hasManuallyChangedUnit by remember { mutableStateOf(false) }
    var hasManuallyChangedCategory by remember { mutableStateOf(false) }

    fun mapOffCategory(offCategoryStr: String?, productName: String?): String {
        val textToSearch = "${offCategoryStr ?: ""} ${productName ?: ""}".lowercase()
        return when {
            textToSearch.contains("farine") || textToSearch.contains("boulangerie") || textToSearch.contains("levure") || textToSearch.contains("bread") || textToSearch.contains("pain") || textToSearch.contains("flour") || textToSearch.contains("koba") || textToSearch.contains("lafarina") -> "Épicerie - Farine & Boulangerie"
            textToSearch.contains("pâte") || textToSearch.contains("pasta") || textToSearch.contains("macaroni") || textToSearch.contains("spaghetti") || textToSearch.contains("noodle") || textToSearch.contains("ramen") || textToSearch.contains("elbow") || textToSearch.contains("fusilli") || textToSearch.contains("twist") -> "Épicerie - Pâtes alimentaires"
            textToSearch.contains("gear oil") || textToSearch.contains("sae") || textToSearch.contains("boss 4t") || textToSearch.contains("atf") || textToSearch.contains("graise") || textToSearch.contains("graisse") || textToSearch.contains("lubrifiant") || textToSearch.contains("moteur") || textToSearch.contains("engine") || textToSearch.contains("lubec") -> "Lubrifiants & Fluides moteur"
            textToSearch.contains("huile") || textToSearch.contains("oil") || textToSearch.contains("menaka") || textToSearch.contains("cocotop") || textToSearch.contains("hina") -> "Épicerie - Huiles de cuisine"
            textToSearch.contains("lait") || textToSearch.contains("dairy") || textToSearch.contains("yogurt") || textToSearch.contains("danica") || textToSearch.contains("francelait") || textToSearch.contains("ronono") || textToSearch.contains("cheese") || textToSearch.contains("fromage") || textToSearch.contains("cream") || textToSearch.contains("crème") -> "Épicerie - Lait & Produits laitiers"
            textToSearch.contains("margarine") || textToSearch.contains("jadida") || textToSearch.contains("orkide") || textToSearch.contains("butter") || textToSearch.contains("beurre") || textToSearch.contains("matière grasse") || textToSearch.contains("marga") -> "Épicerie - Margarine & Matières grasses"
            textToSearch.contains("conserve") || textToSearch.contains("sardine") || textToSearch.contains("canned") || textToSearch.contains("mais doux") || textToSearch.contains("d'or") || textToSearch.contains("anny") || textToSearch.contains("delmoneco") || textToSearch.contains("mavo") || textToSearch.contains("omega") || textToSearch.contains("soleil d'or") -> "Épicerie - Conserves"
            textToSearch.contains("cube") || textToSearch.contains("calnort") || textToSearch.contains("ketchup") || textToSearch.contains("mayonese") || textToSearch.contains("mayonnaise") || textToSearch.contains("lesieur") || textToSearch.contains("sauce") || textToSearch.contains("épice") || textToSearch.contains("condiment") || textToSearch.contains("sira") || textToSearch.contains("sel") || textToSearch.contains("assaisonnement") -> "Épicerie - Condiments, Épices & Assaisonnements"
            textToSearch.contains("snack") || textToSearch.contains("biscuit") || textToSearch.contains("biski") || textToSearch.contains("cookie") || textToSearch.contains("candy") || textToSearch.contains("bonbon") || textToSearch.contains("lollypop") || textToSearch.contains("cake") || textToSearch.contains("bingo") || textToSearch.contains("chocolat") || textToSearch.contains("sweet") || textToSearch.contains("confi") || textToSearch.contains("goma") || textToSearch.contains("baume") || textToSearch.contains("extra baume") -> "Épicerie - Confiseries & Snacks"
            textToSearch.contains("café") || textToSearch.contains("cafe") || textToSearch.contains("thé") || textToSearch.contains("tea") || textToSearch.contains("taf") || textToSearch.contains("zoto") || textToSearch.contains("salone") -> "Boissons - Café & Thé"
            textToSearch.contains("jus") || textToSearch.contains("juice") || textToSearch.contains("drink") || textToSearch.contains("soda") || textToSearch.contains("cola") || textToSearch.contains("bashayer") || textToSearch.contains("dynamic") || textToSearch.contains("le fruit") || textToSearch.contains("tampico") || textToSearch.contains("eau") || textToSearch.contains("rano") || textToSearch.contains("sprite") || textToSearch.contains("fanta") -> "Boissons - Jus"
            textToSearch.contains("hygiène") || textToSearch.contains("beauté") || textToSearch.contains("savon") || textToSearch.contains("soap") || textToSearch.contains("cali") || textToSearch.contains("cosmetic") || textToSearch.contains("dentifrice") || textToSearch.contains("shamp") || textToSearch.contains("cheveux") || textToSearch.contains("belle") || textToSearch.contains("jojoba") || textToSearch.contains("kinana") || textToSearch.contains("ravintsara") -> "Hygiène & Beauté"
            textToSearch.contains("couche") || textToSearch.contains("bebem") || textToSearch.contains("goodcare") || textToSearch.contains("baby") || textToSearch.contains("bebe") || textToSearch.contains("lingette") || textToSearch.contains("comfort") || textToSearch.contains("pants") -> "Bébé - Couches & Lingettes"
            textToSearch.contains("nettoyage") || textToSearch.contains("entretien") || textToSearch.contains("ariel") || textToSearch.contains("oxi") || textToSearch.contains("detergent") || textToSearch.contains("lessive") || textToSearch.contains("savony") || textToSearch.contains("bleu d'azure") -> "Entretien ménager & Nettoyage"
            textToSearch.contains("cirage") || textToSearch.contains("presto") || textToSearch.contains("salone mats") || textToSearch.contains("chaussure") || textToSearch.contains("pate presto") || textToSearch.contains("pate salone") -> "Entretien chaussures (Cirage)"
            textToSearch.contains("désodorisant") || textToSearch.contains("parfum d'ambiance") || textToSearch.contains("boule ext auto") || textToSearch.contains("air") -> "Désodorisants & Parfums d'ambiance"
            textToSearch.contains("cahier") || textToSearch.contains("stylo") || textToSearch.contains("classmate") || textToSearch.contains("madabook") || textToSearch.contains("fournitures") || textToSearch.contains("papeterie") || textToSearch.contains("school") || textToSearch.contains("book") || textToSearch.contains("cah") -> "Papeterie & Fournitures scolaires"
            textToSearch.contains("ampoule") || textToSearch.contains("led") || textToSearch.contains("eclairage") || textToSearch.contains("light") || textToSearch.contains("b22") || textToSearch.contains("e27") || textToSearch.contains("lightbulb") -> "Éclairage (Ampoules)"
            textToSearch.contains("bougie") || textToSearch.contains("mateza") || textToSearch.contains("voila pm") || textToSearch.contains("zapp") -> "Bougies"
            textToSearch.contains("allumette") || textToSearch.contains("briquet") || textToSearch.contains("mimosa") || textToSearch.contains("briq") || textToSearch.contains("whatshot") || textToSearch.contains("fire") || textToSearch.contains("star queen") || textToSearch.contains("roller") || textToSearch.contains("tic-tak") -> "Allumettes & Briquets"
            textToSearch.contains("insecticide") || textToSearch.contains("anti-moustique") || textToSearch.contains("etkintox") || textToSearch.contains("kingtox") || textToSearch.contains("moskila") || textToSearch.contains("menabolo") || textToSearch.contains("salama") -> "Insecticides & Anti-moustiques"
            textToSearch.contains("pile") || textToSearch.contains("battery") || textToSearch.contains("toshiba") || textToSearch.contains("energy r") || textToSearch.contains("voila r") -> "Piles électriques"
            textToSearch.contains("pointe") || textToSearch.contains("vis") || textToSearch.contains("clou") || textToSearch.contains("quincaillerie") || textToSearch.contains("hardware") -> "Quincaillerie"
            textToSearch.contains("hafa") || textToSearch.contains("autres / divers") || textToSearch.contains("divers") || textToSearch.contains("lollypop 48") -> "Autres / Divers"
            else -> "Hafa"
        }
    }

    fun performBarcodeLookup(code: String) {
        if (code.isBlank()) return
        coroutineScope.launch {
            offSearchLoading = true
            try {
                val response = com.example.util.OpenFoodFactsApi.service.getProductByBarcode(code)
                val p = response.product
                if (p != null) {
                    name = p.productName ?: ""
                    barcode = p.code ?: code
                    imageUrl = p.imageUrl ?: ""
                    marque = p.brands ?: ""
                    description = p.genericName ?: p.categories ?: ""
                    selectedCategory = mapOffCategory(p.categories, p.productName)
                    
                    android.widget.Toast.makeText(
                        context,
                        if (activeLang == "mg") "Entana hita tamin'ny OFF! Ampidiro ny vidiny." else "Produit trouvé sur OFF! Saisissez le prix.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        context,
                        if (activeLang == "mg") "Tsy hita tamin'ny OFF" else "Produit non trouvé sur OFF",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    context,
                    if (activeLang == "mg") "Nisy olana teo amin'ny fifandraisana" else "Erreur de connexion OFF",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } finally {
                offSearchLoading = false
            }
        }
    }

    // Smart prediction effect
    LaunchedEffect(name) {
        if (name.isBlank() || editingProduct != null) return@LaunchedEffect
        val lower = name.lowercase()
        
        if (!hasManuallyChangedCategory) {
            val predictedCat = when {
                lower.contains("farine") || lower.contains("boulangerie") || lower.contains("levure") || lower.contains("bread") || lower.contains("pain") || lower.contains("flour") || lower.contains("koba") || lower.contains("lafarina") -> "Épicerie - Farine & Boulangerie"
                lower.contains("pâte") || lower.contains("pasta") || lower.contains("macaroni") || lower.contains("spaghetti") || lower.contains("noodle") || lower.contains("ramen") || lower.contains("elbow") || lower.contains("fusilli") || lower.contains("twist") -> "Épicerie - Pâtes alimentaires"
                lower.contains("gear oil") || lower.contains("sae") || lower.contains("boss 4t") || lower.contains("atf") || lower.contains("graise") || lower.contains("graisse") || lower.contains("lubrifiant") || lower.contains("moteur") || lower.contains("engine") || lower.contains("lubec") -> "Lubrifiants & Fluides moteur"
                lower.contains("huile") || lower.contains("oil") || lower.contains("menaka") || lower.contains("cocotop") || lower.contains("hina") -> "Épicerie - Huiles de cuisine"
                lower.contains("lait") || lower.contains("dairy") || lower.contains("yogurt") || lower.contains("danica") || lower.contains("francelait") || lower.contains("ronono") || lower.contains("cheese") || lower.contains("fromage") || lower.contains("cream") || lower.contains("crème") -> "Épicerie - Lait & Produits laitiers"
                lower.contains("margarine") || lower.contains("jadida") || lower.contains("orkide") || lower.contains("butter") || lower.contains("beurre") || lower.contains("matière grasse") || lower.contains("marga") -> "Épicerie - Margarine & Matières grasses"
                lower.contains("conserve") || lower.contains("sardine") || lower.contains("canned") || lower.contains("mais doux") || lower.contains("d'or") || lower.contains("anny") || lower.contains("delmoneco") || lower.contains("mavo") || lower.contains("omega") || lower.contains("soleil d'or") -> "Épicerie - Conserves"
                lower.contains("cube") || lower.contains("calnort") || lower.contains("ketchup") || lower.contains("mayonese") || lower.contains("mayonnaise") || lower.contains("lesieur") || lower.contains("sauce") || lower.contains("épice") || lower.contains("condiment") || lower.contains("sira") || lower.contains("sel") || lower.contains("assaisonnement") -> "Épicerie - Condiments, Épices & Assaisonnements"
                lower.contains("snack") || lower.contains("biscuit") || lower.contains("biski") || lower.contains("cookie") || lower.contains("candy") || lower.contains("bonbon") || lower.contains("lollypop") || lower.contains("cake") || lower.contains("bingo") || lower.contains("chocolat") || lower.contains("sweet") || lower.contains("confi") || lower.contains("goma") || lower.contains("baume") || lower.contains("extra baume") -> "Épicerie - Confiseries & Snacks"
                lower.contains("café") || lower.contains("cafe") || lower.contains("thé") || lower.contains("tea") || lower.contains("taf") || lower.contains("zoto") || lower.contains("salone") -> "Boissons - Café & Thé"
                lower.contains("jus") || lower.contains("juice") || lower.contains("drink") || lower.contains("soda") || lower.contains("cola") || lower.contains("bashayer") || lower.contains("dynamic") || lower.contains("le fruit") || lower.contains("tampico") || lower.contains("eau") || lower.contains("rano") || lower.contains("sprite") || lower.contains("fanta") -> "Boissons - Jus"
                lower.contains("hygiène") || lower.contains("beauté") || lower.contains("savon") || lower.contains("soap") || lower.contains("cali") || lower.contains("cosmetic") || lower.contains("dentifrice") || lower.contains("shamp") || lower.contains("cheveux") || lower.contains("belle") || lower.contains("jojoba") || lower.contains("kinana") || lower.contains("ravintsara") -> "Hygiène & Beauté"
                lower.contains("couche") || lower.contains("bebem") || lower.contains("goodcare") || lower.contains("baby") || lower.contains("bebe") || lower.contains("lingette") || lower.contains("comfort") || lower.contains("pants") -> "Bébé - Couches & Lingettes"
                lower.contains("nettoyage") || lower.contains("entretien") || lower.contains("ariel") || lower.contains("oxi") || lower.contains("detergent") || lower.contains("lessive") || lower.contains("savony") || lower.contains("bleu d'azure") -> "Entretien ménager & Nettoyage"
                lower.contains("cirage") || lower.contains("presto") || lower.contains("salone mats") || lower.contains("chaussure") || lower.contains("pate presto") || lower.contains("pate salone") -> "Entretien chaussures (Cirage)"
                lower.contains("désodorisant") || lower.contains("parfum d'ambiance") || lower.contains("boule ext auto") || lower.contains("air") -> "Désodorisants & Parfums d'ambiance"
                lower.contains("cahier") || lower.contains("stylo") || lower.contains("classmate") || lower.contains("madabook") || lower.contains("fournitures") || lower.contains("papeterie") || lower.contains("school") || lower.contains("book") || lower.contains("cah") -> "Papeterie & Fournitures scolaires"
                lower.contains("ampoule") || lower.contains("led") || lower.contains("eclairage") || lower.contains("light") || lower.contains("b22") || lower.contains("e27") || lower.contains("lightbulb") -> "Éclairage (Ampoules)"
                lower.contains("bougie") || lower.contains("mateza") || lower.contains("voila pm") || lower.contains("zapp") -> "Bougies"
                lower.contains("allumette") || lower.contains("briquet") || lower.contains("mimosa") || lower.contains("briq") || lower.contains("whatshot") || lower.contains("fire") || lower.contains("star queen") || lower.contains("roller") || lower.contains("tic-tak") -> "Allumettes & Briquets"
                lower.contains("insecticide") || lower.contains("anti-moustique") || lower.contains("etkintox") || lower.contains("kingtox") || lower.contains("moskila") || lower.contains("menabolo") || lower.contains("salama") -> "Insecticides & Anti-moustiques"
                lower.contains("pile") || lower.contains("battery") || lower.contains("toshiba") || lower.contains("energy r") || lower.contains("voila r") -> "Piles électriques"
                lower.contains("pointe") || lower.contains("vis") || lower.contains("clou") || lower.contains("quincaillerie") || lower.contains("hardware") -> "Quincaillerie"
                lower.contains("hafa") || lower.contains("autres / divers") || lower.contains("divers") || lower.contains("lollypop 48") -> "Autres / Divers"
                else -> null
            }
            if (predictedCat != null) {
                selectedCategory = predictedCat
            }
        }
        
        if (!hasManuallyChangedUnit) {
            val predictedUnit = when {
                lower.contains("vary") || lower.contains("bary") || lower.contains("rice") || lower.contains("sira") || lower.contains("sel") || lower.contains("sukra") || lower.contains("sucre") || lower.contains("lafarina") || lower.contains("farine") || lower.contains("karoty") || lower.contains("voatabia") || lower.contains("pataty") || lower.contains("legume") || lower.contains("tongolo") || lower.contains("oignon") -> "Kilogramme"
                lower.contains("menaka") || lower.contains("oil") || lower.contains("huile") || lower.contains("rano") || lower.contains("eau") || lower.contains("ronono") || lower.contains("lait") -> "Litre"
                lower.contains("carton") || lower.contains("baoritra") || lower.contains("boite de") -> "Carton"
                lower.contains("sac") || lower.contains("gony") -> "Sac"
                lower.contains("paquet") || lower.contains("fonosana") || lower.contains("biski") || lower.contains("biscuit") -> "Paquet"
                lower.contains("boite") || lower.contains("boatina") || lower.contains("can") -> "Boîte"
                lower.contains("bouteille") || lower.contains("tavoahangy") || lower.contains("cola") || lower.contains("fanta") || lower.contains("biera") || lower.contains("beer") || lower.contains("sprite") -> "Bouteille"
                lower.contains("kapoaka") || lower.contains("tasse") -> "Tasse/Kapoaka"
                else -> "Pièce"
            }
            selectedUnit = predictedUnit
        }
    }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showUnitDropdown by remember { mutableStateOf(false) }

    // Form validation states
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var stockError by remember { mutableStateOf(false) }
    var thresholdError by remember { mutableStateOf(false) }
    var prixAchatError by remember { mutableStateOf(false) }
    var stockMaxError by remember { mutableStateOf(false) }
    var tauxTaxeError by remember { mutableStateOf(false) }

    val isEditing = editingProduct != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = if (isTablet) Alignment.TopCenter else Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isTablet) Modifier.widthIn(max = 640.dp) else Modifier)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen title
            Text(
                text = if (isEditing) t("edit_product_title") else t("add_product_title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Open Food Facts Search Card
            if (!isEditing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOffSearch = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Mikaroka amin'ny Open Food Facts"
                                    "fr" -> "Rechercher sur Open Food Facts"
                                    else -> "Search Open Food Facts"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Fenoy ho azy ny mombamomba ny vokatra"
                                    "fr" -> "Remplir automatiquement la fiche produit"
                                    else -> "Auto-fill product details instantly"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Choose from Templates Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTemplateSearch = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                                        contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Mampiasa modely vokatra efa misy"
                                    "fr" -> "Utiliser un modèle de produit"
                                    else -> "Use a product template"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Mifidiana amin'ny sokajy sy vokatra an-jatony efa vonona"
                                    "fr" -> "Choisir parmi des centaines de produits pré-configurés"
                                    else -> "Choose from hundreds of pre-configured products"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Section 1: Core Identification
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                                                        Icon(androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        Text(
                            text = when(activeLang) {
                                "mg" -> "Fampahafantarana ny entana"
                                "fr" -> "Fiche produit centrale"
                                else -> "Core Identification"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Name input
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = it.isBlank()
                        },
                        label = { Text(t("product_name")) },
                        isError = nameError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Short Name
                    OutlinedTextField(
                        value = nomCourt,
                        onValueChange = { nomCourt = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Anarana fohy (ho an'ny tapakila)"
                                "fr" -> "Nom court (pour ticket)"
                                else -> "Short name (for receipts)"
                            }
                        ) },
                        supportingText = { Text("Ex: Vary gasy") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category select drop down
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = showCategoryDropdown,
                            onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(t("category_label")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .testTag("product_category_select"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                standardCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            selectedCategory = cat
                                            hasManuallyChangedCategory = true
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Custom category input if "Hafa"
                    if (selectedCategory == "Hafa") {
                        OutlinedTextField(
                            value = customCategory,
                            onValueChange = { 
                                customCategory = it 
                                hasManuallyChangedCategory = true
                            },
                            label = { Text(t("custom_category_label")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_custom_category_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Sub Category
                    OutlinedTextField(
                        value = sousCategorie,
                        onValueChange = { sousCategorie = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Sokajy manaraka (Sous-catégorie)"
                                "fr" -> "Sous-catégorie"
                                else -> "Sub-category"
                            }
                        ) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Brand (Marque)
                    OutlinedTextField(
                        value = marque,
                        onValueChange = { marque = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Marika (Marque)"
                                "fr" -> "Marque"
                                else -> "Brand"
                            }
                        ) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }
            }

            // Section 2: Pricing & Stock (Tahiry & Vidiny)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                                                        Icon(androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        Text(
                            text = when(activeLang) {
                                "mg" -> "Vidiny, Refy & Tahiry"
                                "fr" -> "Tarification, Unités & Stock"
                                else -> "Pricing, Units & Stock"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Unit of Measure selection drop down
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = showUnitDropdown,
                            onExpandedChange = { showUnitDropdown = !showUnitDropdown }
                        ) {
                            OutlinedTextField(
                                value = when (selectedUnit) {
                                    "Pièce" -> t("unit_piece")
                                    "Litre" -> t("unit_litre")
                                    "Kilogramme" -> t("unit_kg")
                                    "Paquet" -> t("unit_paquet")
                                    "Carton" -> t("unit_carton")
                                    "Sac" -> t("unit_sac")
                                    "Boîte" -> t("unit_boite")
                                    "Bouteille" -> t("unit_bouteille")
                                    "Tasse/Kapoaka" -> t("unit_kapoaka")
                                    else -> selectedUnit
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(t("unit_label")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUnitDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .testTag("product_unit_select"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showUnitDropdown,
                                onDismissRequest = { showUnitDropdown = false }
                            ) {
                                units.forEach { u ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (u) {
                                                    "Pièce" -> t("unit_piece")
                                                    "Litre" -> t("unit_litre")
                                                    "Kilogramme" -> t("unit_kg")
                                                    "Paquet" -> t("unit_paquet")
                                                    "Carton" -> t("unit_carton")
                                                    "Sac" -> t("unit_sac")
                                                    "Boîte" -> t("unit_boite")
                                                    "Bouteille" -> t("unit_bouteille")
                                                    "Tasse/Kapoaka" -> t("unit_kapoaka")
                                                    else -> u
                                                }
                                            )
                                        },
                                        onClick = {
                                            selectedUnit = u
                                            hasManuallyChangedUnit = true
                                            showUnitDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Price Purchase input (Prix d'achat unité base)
                    OutlinedTextField(
                        value = prixAchatUniteBaseStr,
                        onValueChange = {
                            prixAchatUniteBaseStr = it
                            prixAchatError = it.isNotEmpty() && (it.toDoubleOrNull() == null || it.toDouble() < 0.0)
                        },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Vidiny nividianana (Prix d'achat)"
                                "fr" -> "Prix d'achat de référence"
                                else -> "Reference Purchase Price"
                            }
                        ) },
                        prefix = { Text("Ar ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = prixAchatError,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Price Selling input
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = {
                            priceStr = it
                            priceError = it.toDoubleOrNull() == null || it.toDouble() <= 0
                        },
                        label = { Text(t("unit_price")) },
                        prefix = { Text("Ar ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = priceError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_price_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Wholesale Price Input
                    OutlinedTextField(
                        value = wholesalePriceStr,
                        onValueChange = {
                            wholesalePriceStr = it
                            wholesalePriceError = it.isNotEmpty() && (it.toDoubleOrNull() == null || it.toDouble() < 0)
                        },
                        label = { Text(
                            when (activeLang) {
                                "mg" -> "Vidiny Ambongadiny (Wholesale Price)"
                                "fr" -> "Prix de gros"
                                else -> "Wholesale Price"
                            }
                        ) },
                        prefix = { Text("Ar ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = wholesalePriceError,
                        supportingText = { Text("Ampiasaina amin'ny fomba 'Grossiste'") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_wholesale_price_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Initial Stock Input
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = {
                            stockStr = it
                            stockError = it.toDoubleOrNull() == null || it.toDouble() < 0
                            // Auto sync integer field
                            it.toDoubleOrNull()?.let { d ->
                                stockQuantityStr = d.toInt().toString()
                            }
                        },
                        label = { Text(t("initial_stock")) },
                        supportingText = { Text("Ex: 1.5, 20.0, 100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stockError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_stock_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Stock Quantity field (Integer)
                    OutlinedTextField(
                        value = stockQuantityStr,
                        onValueChange = {
                            stockQuantityStr = it
                            stockQuantityError = it.toIntOrNull() == null || it.toInt() < 0
                        },
                        label = { Text(
                            when (activeLang) {
                                "mg" -> "Isan'ny Tahiry (Stock Quantity - Int)"
                                "fr" -> "Quantité de stock (Entier)"
                                else -> "Stock Quantity (Integer)"
                            }
                        ) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stockQuantityError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_stock_quantity_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Stock Max
                    OutlinedTextField(
                        value = stockMaxStr,
                        onValueChange = {
                            stockMaxStr = it
                            stockMaxError = it.isNotEmpty() && (it.toDoubleOrNull() == null || it.toDouble() < 0.0)
                        },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Tahiry farany ambony (Stock Maximum)"
                                "fr" -> "Stock Maximum"
                                else -> "Maximum Stock Limit"
                            }
                        ) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stockMaxError,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Emplacement (shelf location)
                    OutlinedTextField(
                        value = emplacement,
                        onValueChange = { emplacement = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Toerana ao amin'ny fivarotana (Emplacement)"
                                "fr" -> "Emplacement en rayon"
                                else -> "Shelf Location"
                            }
                        ) },
                        placeholder = { Text("Rayon A-1") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Low Stock Threshold Input (Double)
                    OutlinedTextField(
                        value = lowStockThresholdStr,
                        onValueChange = {
                            lowStockThresholdStr = it
                            thresholdError = it.toDoubleOrNull() == null || it.toDouble() < 0
                        },
                        label = { Text("Low Stock Alert Seuil (Alerte)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = thresholdError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_threshold_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Section 3: Traceability & Fiscality
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                                                        Icon(androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        Text(
                            text = when(activeLang) {
                                "mg" -> "Fitsipika & Fitsirihana"
                                "fr" -> "Traçabilité & Fiscalité"
                                else -> "Traceability & Fiscal"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Barcode Input
                    val barcodeLabel = when (activeLang) {
                        "mg" -> "Kaody Bar (Barcode)"
                        "fr" -> "Code-barres"
                        else -> "Barcode"
                    }
                    val barcodeSupportText = when (activeLang) {
                        "mg" -> "Tsindrio ny fakantsary handinihana kaody, na ny bokotra rafraîchir hamoronana kaody"
                        "fr" -> "Appuyez sur l'appareil photo pour scanner, ou sur l'icône rafraîchir pour générer un code"
                        else -> "Tap camera to scan, or refresh icon to generate a random code"
                    }

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text(barcodeLabel) },
                        trailingIcon = {
                            Row(
                                modifier = Modifier.padding(end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (barcode.isNotBlank()) {
                                    IconButton(onClick = { performBarcodeLookup(barcode) }) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search Barcode on Open Food Facts",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = { showLiveScanner = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Scan Barcode",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    barcode = com.example.util.BarcodeUtil.generateStandardBarcode()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Generate Barcode",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        },
                        supportingText = { 
                            Column {
                                Text(barcodeSupportText)
                                if (barcode.isNotBlank()) {
                                    Text(
                                        text = if (activeLang == "mg") "Tsindrio ny fitaratra handinihana ity kaody ity tamin'ny OFF" else "Cliquez sur la loupe pour rechercher ce code sur OFF",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_barcode_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // SKU Input
                    val skuLabel = when (activeLang) {
                        "mg" -> "Kaody SKU"
                        "fr" -> "Code SKU"
                        else -> "SKU Code"
                    }
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text(skuLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Expiry tracking toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Araho maso ny fahasimbana"
                                    "fr" -> "Gérer la péremption (FIFO)"
                                    else -> "Manage expiration dates (FIFO)"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Suivi dates de péremption"
                                    "fr" -> "Active le suivi par lot et date"
                                    else -> "Enables batch & expiry tracking"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = gerePeremption,
                            onCheckedChange = { gerePeremption = it }
                        )
                    }

                    // Taxable Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Misy hetra (Produit taxable)"
                                    "fr" -> "Produit taxable"
                                    else -> "Taxable item"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Hampiharana tahan'ny hetra"
                                    "fr" -> "Permet d'appliquer une taxe"
                                    else -> "Enables applying product taxes"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = taxable,
                            onCheckedChange = { taxable = it }
                        )
                    }

                    if (taxable) {
                        OutlinedTextField(
                            value = tauxTaxeStr,
                            onValueChange = {
                                tauxTaxeStr = it
                                tauxTaxeError = it.toDoubleOrNull() == null || it.toDouble() < 0.0
                            },
                            label = { Text(
                                when(activeLang) {
                                    "mg" -> "Tahan'ny hetra (%)"
                                    "fr" -> "Taux de taxe (%)"
                                    else -> "Tax Rate (%)"
                                }
                            ) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = tauxTaxeError,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Image URL option
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text(t("img_url_label")) },
                        placeholder = { Text("https://example.com/photo.png") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_image_url_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Camera / Gallery selections
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Fakan-tsary"
                                    "fr" -> "Caméra"
                                    else -> "Camera"
                                },
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Sary ato"
                                    "fr" -> "Galerie"
                                    else -> "Gallery"
                                },
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Section 4: Fournisseur Principal
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                                        Icon(androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Mpanome Entana (Fournisseur)"
                                    "fr" -> "Fournisseur principal"
                                    else -> "Main Supplier"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = { showAddSupplierDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                                                        Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when(activeLang) {
                                    "mg" -> "Ampina"
                                    "fr" -> "Ajouter"
                                    else -> "Add"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Supplier dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = showSupplierDropdown,
                            onExpandedChange = { showSupplierDropdown = !showSupplierDropdown }
                        ) {
                            val selectedSupplierName = suppliers.find { it.id == selectedFournisseurId }?.nom ?: when(activeLang) {
                                "mg" -> "Mbola tsy misy (Aucun)"
                                "fr" -> "Aucun fournisseur sélectionné"
                                else -> "No supplier selected"
                            }
                            OutlinedTextField(
                                value = selectedSupplierName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(
                                    when(activeLang) {
                                        "mg" -> "Mpanome entana"
                                        "fr" -> "Fournisseur"
                                        else -> "Supplier"
                                    }
                                ) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSupplierDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showSupplierDropdown,
                                onDismissRequest = { showSupplierDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(
                                        when(activeLang) {
                                            "mg" -> "Tsy misy (Aucun)"
                                            "fr" -> "Aucun"
                                            else -> "None"
                                        }
                                    ) },
                                    onClick = {
                                        selectedFournisseurId = null
                                        showSupplierDropdown = false
                                    }
                                )
                                suppliers.forEach { supplier ->
                                    DropdownMenuItem(
                                        text = { Text("${supplier.nom} (${supplier.contact ?: ""})") },
                                        onClick = {
                                            selectedFournisseurId = supplier.id
                                            showSupplierDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action CTAs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cancel_product_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(t("cancel_btn"))
                }

                Button(
                    onClick = {
                        val finalName = name.trim()
                        val finalPrice = priceStr.toDoubleOrNull() ?: 0.0
                        val finalStock = stockStr.toDoubleOrNull() ?: 0.0
                        val finalThreshold = lowStockThresholdStr.toDoubleOrNull() ?: 5.0
                        val finalCategory = if (selectedCategory == "Hafa") customCategory.trim().ifEmpty { "Hafa" } else selectedCategory

                        nameError = finalName.isEmpty()
                        priceError = finalPrice <= 0.0
                        stockError = stockStr.toDoubleOrNull() == null || finalStock < 0.0
                        thresholdError = lowStockThresholdStr.toDoubleOrNull() == null || finalThreshold < 0.0

                        val finalStockQuantity = stockQuantityStr.toIntOrNull() ?: 0
                        stockQuantityError = stockQuantityStr.toIntOrNull() == null || finalStockQuantity < 0

                        val finalWholesalePrice = wholesalePriceStr.toDoubleOrNull()
                        val finalPrixAchatUniteBase = prixAchatUniteBaseStr.toDoubleOrNull() ?: 0.0
                        val finalStockMax = stockMaxStr.toDoubleOrNull()
                        val finalTauxTaxe = tauxTaxeStr.toDoubleOrNull() ?: 0.0

                        prixAchatError = prixAchatUniteBaseStr.isNotEmpty() && (prixAchatUniteBaseStr.toDoubleOrNull() == null || prixAchatUniteBaseStr.toDouble() < 0.0)
                        stockMaxError = stockMaxStr.isNotEmpty() && (stockMaxStr.toDoubleOrNull() == null || stockMaxStr.toDouble() < 0.0)
                        tauxTaxeError = taxable && (tauxTaxeStr.toDoubleOrNull() == null || tauxTaxeStr.toDouble() < 0.0)

                        if (!nameError && !priceError && !stockError && !thresholdError && !stockQuantityError && !prixAchatError && !stockMaxError && !tauxTaxeError) {
                            val saved = Product(
                                id = editingProduct?.id ?: 0,
                                name = finalName,
                                price = finalPrice,
                                category = finalCategory,
                                stock = finalStock,
                                lowStockThreshold = finalThreshold,
                                unit = selectedUnit,
                                imageUrl = imageUrl.trim(),
                                barcode = barcode.trim(),
                                wholesalePrice = finalWholesalePrice,
                                sku = sku.trim(),
                                stock_quantity = finalStockQuantity,
                                nomCourt = nomCourt.trim().ifEmpty { null },
                                sousCategorie = sousCategorie.trim().ifEmpty { null },
                                marque = marque.trim().ifEmpty { null },
                                description = description.trim().ifEmpty { null },
                                stockMax = finalStockMax,
                                emplacement = emplacement.trim().ifEmpty { null },
                                fournisseurId = selectedFournisseurId,
                                gerePeremption = gerePeremption,
                                taxable = taxable,
                                tauxTaxe = finalTauxTaxe,
                                prixAchatUniteBase = finalPrixAchatUniteBase
                            )
                            onSaveProduct(saved)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_product_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(t("save_btn").substring(0, t("save_btn").length.coerceAtMost(16)), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // New Supplier popup Dialog
    if (showAddSupplierDialog) {
        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = {
                Text(
                    text = when(activeLang) {
                        "mg" -> "Mpanome Entana Vaovao"
                        "fr" -> "Nouveau Fournisseur"
                        else -> "New Supplier"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSupplierName,
                        onValueChange = { newSupplierName = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Anarana"
                                "fr" -> "Nom"
                                else -> "Name"
                            }
                        ) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSupplierContact,
                        onValueChange = { newSupplierContact = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Fifandraisana (Phone)"
                                "fr" -> "Contact (Tél)"
                                else -> "Contact (Phone)"
                            }
                        ) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSupplierAddress,
                        onValueChange = { newSupplierAddress = it },
                        label = { Text(
                            when(activeLang) {
                                "mg" -> "Adiresy"
                                "fr" -> "Adresse"
                                else -> "Address"
                            }
                        ) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSupplierName.isNotBlank()) {
                            val f = Fournisseur(
                                nom = newSupplierName.trim(),
                                contact = newSupplierContact.trim().ifEmpty { null },
                                adresse = newSupplierAddress.trim().ifEmpty { null },
                                actif = true
                            )
                            viewModel.saveFournisseur(f)
                            newSupplierName = ""
                            newSupplierContact = ""
                            newSupplierAddress = ""
                            showAddSupplierDialog = false
                        }
                    }
                ) {
                    Text(
                        when(activeLang) {
                            "mg" -> "Hitehirizana"
                            "fr" -> "Enregistrer"
                            else -> "Save"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) {
                    Text(t("cancel_btn"))
                }
            }
        )
    }

    if (showLiveScanner) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showLiveScanner = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                BarcodeScannerView(
                    onBarcodeScanned = { scannedCode ->
                        android.util.Log.d("AddProductScreen", "onBarcodeScanned triggered: scannedCode='$scannedCode'")
                        barcode = scannedCode
                        showLiveScanner = false
                        performBarcodeLookup(scannedCode)
                    },
                    onClose = {
                        showLiveScanner = false
                    },
                    language = activeLang,
                    themeColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showOffSearch) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showOffSearch = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Open Food Facts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showOffSearch = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search input
                    OutlinedTextField(
                        value = offSearchQuery,
                        onValueChange = { offSearchQuery = it },
                        placeholder = { 
                            Text(
                                if (activeLang == "mg") "Tadiavo eto ny entana..." 
                                else "Saisissez un nom de produit ou mot-clé..."
                            )
                        },
                        trailingIcon = {
                            if (offSearchQuery.isNotBlank()) {
                                IconButton(onClick = { offSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Madagascar filter toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { offOnlyMadagascar = !offOnlyMadagascar }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.material3.Switch(
                            checked = offOnlyMadagascar,
                            onCheckedChange = { offOnlyMadagascar = it }
                        )
                        Column {
                            Text(
                                text = if (activeLang == "mg") "Madagasikara ihany" else "Madagascar uniquement",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (activeLang == "mg") "Sivana ho an'ny vokatra misy eto an-toerana" else "Filtrer les produits disponibles localement",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { 
                            if (offSearchQuery.isNotBlank()) {
                                offSearchLoading = true
                                offSearchError = null
                                coroutineScope.launch {
                                    try {
                                        val processedQuery = com.example.util.OpenFoodFactsApi.formatQueryForOff(offSearchQuery)
                                        val response = if (offOnlyMadagascar) {
                                            com.example.util.OpenFoodFactsApi.service.searchProducts(
                                                terms = processedQuery,
                                                tagtype0 = "countries",
                                                tagContains0 = "contains",
                                                tag0 = "madagascar"
                                            )
                                        } else {
                                            com.example.util.OpenFoodFactsApi.service.searchProducts(terms = processedQuery)
                                        }
                                        offSearchResults = response.products ?: emptyList()
                                        if (offSearchResults.isEmpty()) {
                                            offSearchError = if (activeLang == "mg") "Tsy nahitana vokatra" else "Aucun produit trouvé"
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        offSearchError = if (activeLang == "mg") "Nisy olana teo amin'ny fifandraisana" else "Erreur de connexion"
                                    } finally {
                                        offSearchLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (activeLang == "mg") "Mikaroka" else "Rechercher")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (offSearchLoading) {
                            CircularProgressIndicator()
                        } else if (offSearchError != null) {
                            Text(
                                text = offSearchError ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (offSearchResults.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = if (activeLang == "mg") "Tsy misy vokatra voasafidy" else "Entrez un mot-clé pour lancer la recherche",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(offSearchResults.size) { index ->
                                    val p = offSearchResults[index]
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                name = p.productName ?: ""
                                                barcode = p.code ?: ""
                                                imageUrl = p.imageUrl ?: ""
                                                marque = p.brands ?: ""
                                                description = p.genericName ?: p.categories ?: ""
                                                selectedCategory = mapOffCategory(p.categories, p.productName)
                                                showOffSearch = false
                                                
                                                android.widget.Toast.makeText(
                                                    context,
                                                    if (activeLang == "mg") "Entana tafiditra! Vidiny sisa no ampidirina." 
                                                    else "Détails du produit importés ! Saisissez le prix.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Thumbnail Image
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                if (!p.imageUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(p.imageUrl).crossfade(true).size(100).build(),
                                                        placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color(0xFFE2E8F0)),
                                                        error = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color(0xFFFFCDD2)),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Image,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.align(Alignment.Center)
                                                    )
                                                }
                                            }

                                            // Product Details
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = p.productName ?: "Sans nom",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                if (!p.brands.isNullOrBlank()) {
                                                    Text(
                                                        text = p.brands,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                if (!p.code.isNullOrBlank()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.Label,
                                                        contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = p.code,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTemplateSearch) {
        val templateCategoriesState = viewModel.getAllTemplateCategories().collectAsState(initial = emptyList())
        val templateCategories = listOf("All") + templateCategoriesState.value

        var queryText by remember { mutableStateOf("") }
        var selectedCat by remember { mutableStateOf("All") }
        val templatesListState = viewModel.searchTemplateProductsInDb(queryText, selectedCat).collectAsState(initial = emptyList())
        val templatesList = templatesListState.value

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showTemplateSearch = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                                        contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = if (activeLang == "mg") "Modely vokatra efa misy" else "Modèles de produits",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(onClick = { showTemplateSearch = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search input
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        placeholder = { Text(if (activeLang == "mg") "Hikaroka modely..." else "Rechercher un modèle...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (queryText.isNotEmpty()) {
                                IconButton(onClick = { queryText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Categories LazyRow
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        items(templateCategories) { cat ->
                            val isSelected = selectedCat == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCat = cat },
                                label = { Text(cat, style = MaterialTheme.typography.bodySmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }

                    // Templates List
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (templatesList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (activeLang == "mg") "Tsy misy modely hita" else "Aucun modèle trouvé",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(templatesList, key = { it.id }) { item ->
                                    Card(
                                        onClick = {
                                            // Pre-fill the form with template details!
                                            name = item.name
                                            selectedCategory = item.category
                                            selectedUnit = item.unit
                                            sku = item.sku
                                            priceStr = "" // Let user enter sale price
                                            prixAchatUniteBaseStr = "" // Let user enter purchase price
                                            stockStr = "" // Let user enter initial stock
                                            
                                            showTemplateSearch = false
                                            
                                            android.widget.Toast.makeText(
                                                context,
                                                if (activeLang == "mg") "Modely voafantina! Ampidiro ny vidiny sy ny tahiry." 
                                                else "Modèle sélectionné ! Remplissez le prix et le stock.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = item.category,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                                if (item.unit.isNotEmpty()) {
                                                    Text(
                                                        text = "Unité: ${item.unit}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                contentDescription = "Select"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Product photos are kept small on purpose: this also lets them be embedded as base64 inside
// the JSON backups (local safety backup + Firebase) so they survive a data wipe or new device,
// without needing any paid image storage (see ImageBackupUtil).
private const val MAX_PRODUCT_IMAGE_DIMENSION = 1024

private fun downscaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int = MAX_PRODUCT_IMAGE_DIMENSION): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap
    val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
    val newWidth = (width * ratio).toInt().coerceAtLeast(1)
    val newHeight = (height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

fun saveBitmapToLocalFile(context: android.content.Context, bitmap: Bitmap): String? {
    return try {
        val resized = downscaleBitmapIfNeeded(bitmap)
        val fileName = "product_img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, 82, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveUriToLocalFile(context: android.content.Context, uri: Uri): String? {
    return try {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            android.graphics.BitmapFactory.decodeStream(input)
        } ?: return null
        val resized = downscaleBitmapIfNeeded(bitmap)
        val fileName = "product_img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, 82, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

