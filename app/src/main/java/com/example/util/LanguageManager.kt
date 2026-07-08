package com.example.util

object LanguageManager {
    
    // Supported languages
    val LANGUAGES = listOf(
        "mg" to "Malgache",
        "fr" to "Français",
        "en" to "English"
    )

    private val translations = mapOf(
        "app_title" to mapOf(
            "mg" to "Varotra sy Tahiry",
            "fr" to "Caisse & Gestion Stock",
            "en" to "POS & Inventory"
        ),
        "activation_required" to mapOf(
            "mg" to "Mila fampahavitrihana ny fampiharana",
            "fr" to "Activation requise pour l'application",
            "en" to "App activation required"
        ),
        "installation_id" to mapOf(
            "mg" to "Installation ID (Alefaso any amin'ny mpamorona):",
            "fr" to "Installation ID (Envoyez au développeur) :",
            "en" to "Installation ID (Send to developer):"
        ),
        "activation_code_label" to mapOf(
            "mg" to "Hampiditra code fampahavitrihana (6 tondro)",
            "fr" to "Saisir le code d'activation (6 chiffres)",
            "en" to "Enter activation code (6 digits)"
        ),
        "activate_btn" to mapOf(
            "mg" to "Hampandeha ny fampiharana",
            "fr" to "Activer l'application",
            "en" to "Activate application"
        ),
        "activation_success" to mapOf(
            "mg" to "Tafita! Vita fampahavitrihana maharitra ny fampiharana.",
            "fr" to "Succès ! L'application est activée en permanence.",
            "en" to "Success! The application is permanently activated."
        ),
        "activation_error" to mapOf(
            "mg" to "Diso ny code fampahavitrihana nampidirinao!",
            "fr" to "Code d'activation invalide !",
            "en" to "Invalid activation code!"
        ),
        "trial_mode" to mapOf(
            "mg" to "Fomba Andrana (24 Ora)",
            "fr" to "Mode d'essai gratuit (24h)",
            "en" to "Free Trial Mode (24h)"
        ),
        "trial_active" to mapOf(
            "mg" to "Mbola manan-kery ny andrana.",
            "fr" to "Essai toujours actif.",
            "en" to "Trial active."
        ),
        "trial_expired" to mapOf(
            "mg" to "Lasa ny 24 ora! Tapitra ny fotoana andrana.",
            "fr" to "Temps d'essai écoulé !",
            "en" to "Trial time expired!"
        ),
        "trial_expired_desc" to mapOf(
            "mg" to "Mba tohizo ny fampiasana, ampidiro ny code fampahavitrihana.",
            "fr" to "Pour continuer à utiliser l'app, veuillez entrer le code d'activation.",
            "en" to "To continue using the app, please enter the activation code."
        ),
        "hours_remaining" to mapOf(
            "mg" to "Sisa fotoana handramana:",
            "fr" to "Temps d'essai restant :",
            "en" to "Trial time remaining:"
        ),
        "tab_home" to mapOf(
            "mg" to "Fandraisana",
            "fr" to "Accueil",
            "en" to "Home"
        ),
        "tab_pos" to mapOf(
            "mg" to "Kajy",
            "fr" to "Caisse",
            "en" to "POS"
        ),
        "tab_add" to mapOf(
            "mg" to "Manampy",
            "fr" to "Ajouter",
            "en" to "Add"
        ),
        "tab_inventory" to mapOf(
            "mg" to "Lisitra",
            "fr" to "Stock",
            "en" to "Stock"
        ),
        "tab_debts" to mapOf(
            "mg" to "Trosa",
            "fr" to "Dettes",
            "en" to "Debts"
        ),
        "tab_history" to mapOf(
            "mg" to "Tantara",
            "fr" to "Historique",
            "en" to "History"
        ),
        "search_hint" to mapOf(
            "mg" to "Hikaroka entana...",
            "fr" to "Rechercher un produit...",
            "en" to "Search products..."
        ),
        "total_products" to mapOf(
            "mg" to "Isan'ny entana",
            "fr" to "Total Produits",
            "en" to "Total Products"
        ),
        "total_categories" to mapOf(
            "mg" to "Isan'ny sokajy",
            "fr" to "Total Catégories",
            "en" to "Total Categories"
        ),
        "latest_added" to mapOf(
            "mg" to "Entana 5 farany nampidirina",
            "fr" to "5 derniers produits",
            "en" to "5 latest products"
        ),
        "no_products" to mapOf(
            "mg" to "Tsy misy entana mbola voatahiry",
            "fr" to "Aucun produit disponible",
            "en" to "No products available"
        ),
        "add_first_product" to mapOf(
            "mg" to "Hampiditra ny entana voalohany",
            "fr" to "Ajouter votre premier produit",
            "en" to "Add your first product"
        ),
        "calcul_rapide" to mapOf(
            "mg" to "Calcul Rapide",
            "fr" to "Calcul Rapide",
            "en" to "Quick Misc"
        ),
        "vola_voaray" to mapOf(
            "mg" to "Vola voaray",
            "fr" to "Montant reçu",
            "en" to "Amount received"
        ),
        "vola_averina" to mapOf(
            "mg" to "Vola averina",
            "fr" to "Monnaie à rendre",
            "en" to "Change to return"
        ),
        "vola_tsy_ampy" to mapOf(
            "mg" to "Vola mbola tsy ampy",
            "fr" to "Montant manquant",
            "en" to "Amount missing"
        ),
        "vider_panier" to mapOf(
            "mg" to "Vider ny panier",
            "fr" to "Vider le panier",
            "en" to "Clear cart"
        ),
        "valider_vente" to mapOf(
            "mg" to "Valider ny varotra",
            "fr" to "Valider la vente",
            "en" to "Validate sale"
        ),
        "validation_success_msg" to mapOf(
            "mg" to "Voaray ny fividianana ary nohavaozina ny tahiry.",
            "fr" to "Vente validée avec succès et stock mis à jour.",
            "en" to "Sale successfully validated and stock updated."
        ),
        "validation_insufficient" to mapOf(
            "mg" to "Tsy ampy ny vola voaray handoavana ny totaliny!",
            "fr" to "Le montant reçu est insuffisant pour valider !",
            "en" to "The amount received is insufficient to validate!"
        ),
        "add_product_title" to mapOf(
            "mg" to "Hampiditra Entana Vaovao",
            "fr" to "Ajouter un Nouveau Produit",
            "en" to "Add New Product"
        ),
        "edit_product_title" to mapOf(
            "mg" to "Hanova ny momba ny entana",
            "fr" to "Modifier le Produit",
            "en" to "Edit Product"
        ),
        "product_name" to mapOf(
            "mg" to "Anaran'ny entana",
            "fr" to "Nom du produit",
            "en" to "Product name"
        ),
        "category_label" to mapOf(
            "mg" to "Sokajy",
            "fr" to "Catégorie",
            "en" to "Category"
        ),
        "unit_label" to mapOf(
            "mg" to "Inona ny refy (Unité)?",
            "fr" to "Unité de mesure",
            "en" to "Unit of measure"
        ),
        "unit_piece" to mapOf(
            "mg" to "Pièce / Tapa-pako",
            "fr" to "Pièce",
            "en" to "Piece"
        ),
        "unit_litre" to mapOf(
            "mg" to "Litre (L)",
            "fr" to "Litre",
            "en" to "Liter"
        ),
        "unit_kg" to mapOf(
            "mg" to "Kilogramme (kg)",
            "fr" to "Kilogramme",
            "en" to "Kilogram"
        ),
        "unit_paquet" to mapOf(
            "mg" to "Paquet / Fonosana",
            "fr" to "Paquet",
            "en" to "Packet"
        ),
        "unit_kapoaka" to mapOf(
            "mg" to "Tasse / Kapoaka",
            "fr" to "Kapoaka / Tasse",
            "en" to "Cup / Kapoaka"
        ),
        "unit_price" to mapOf(
            "mg" to "Vidiny iray (Ar)",
            "fr" to "Prix unitaire (Ar)",
            "en" to "Unit price (Ar)"
        ),
        "initial_stock" to mapOf(
            "mg" to "Tahiry voalohany (Stock)",
            "fr" to "Stock initial",
            "en" to "Initial stock"
        ),
        "save_btn" to mapOf(
            "mg" to "Hitehirizana",
            "fr" to "Enregistrer le produit",
            "en" to "Save Product"
        ),
        "img_url_label" to mapOf(
            "mg" to "URL-n'ny sary (Safidy fotsiny)",
            "fr" to "URL de l'image (Optionnel)",
            "en" to "Image URL (Optional)"
        ),
        "take_photo_btn" to mapOf(
            "mg" to "Handray sary (Caméra)",
            "fr" to "Prendre une photo (Caméra)",
            "en" to "Take Photo (Camera)"
        ),
        "select_gallery_btn" to mapOf(
            "mg" to "Hifidy sary amin'ny Galerie",
            "fr" to "Choisir de la galerie",
            "en" to "Choose from Gallery"
        ),
        "custom_category_label" to mapOf(
            "mg" to "Hanoratra sokajy vaovao",
            "fr" to "Saisir une nouvelle catégorie",
            "en" to "Enter new category"
        ),
        "adjust_stock_title" to mapOf(
            "mg" to "Hampitombo na hampihena tahiry haingana",
            "fr" to "Ajustement rapide du stock",
            "en" to "Quick Stock Adjustment"
        ),
        "edit_action" to mapOf(
            "mg" to "Hanova mombamomba",
            "fr" to "Modifier",
            "en" to "Edit Details"
        ),
        "delete_action" to mapOf(
            "mg" to "Hamafa tanteraka",
            "fr" to "Supprimer",
            "en" to "Delete"
        ),
        "confirm_delete_msg" to mapOf(
            "mg" to "Azonao antoka ve fa hofafanao tanteraka ity entana ity?",
            "fr" to "Voulez-vous vraiment supprimer définitivement ce produit ?",
            "en" to "Are you sure you want to permanently delete this product?"
        ),
        "cancel_btn" to mapOf(
            "mg" to "Hanafoana",
            "fr" to "Annuler",
            "en" to "Cancel"
        ),
        "delete_btn" to mapOf(
            "mg" to "Hamafa",
            "fr" to "Supprimer",
            "en" to "Delete"
        ),
        "total_debts" to mapOf(
            "mg" to "Totalin'ny trosa rehetra ankehitriny",
            "fr" to "Total des dettes en cours",
            "en" to "Total Outstanding Debts"
        ),
        "new_debt" to mapOf(
            "mg" to "Trosa Vaovao",
            "fr" to "Nouvelle Dette",
            "en" to "New Debt"
        ),
        "debtor_name" to mapOf(
            "mg" to "Anaran'ny mpitrosa",
            "fr" to "Nom du débiteur",
            "en" to "Debtor name"
        ),
        "debt_amount" to mapOf(
            "mg" to "Vola trosaina (Ar)",
            "fr" to "Montant de la dette (Ar)",
            "en" to "Debt amount (Ar)"
        ),
        "debt_note" to mapOf(
            "mg" to "Fanamarihana (Safidy)",
            "fr" to "Note (Optionnel)",
            "en" to "Note (Optional)"
        ),
        "debt_date" to mapOf(
            "mg" to "Daty",
            "fr" to "Date",
            "en" to "Date"
        ),
        "save_debt_btn" to mapOf(
            "mg" to "Hitehirizana ny trosa",
            "fr" to "Enregistrer la dette",
            "en" to "Save Debt"
        ),
        "search_debt_hint" to mapOf(
            "mg" to "Hikaroka mpitrosa na note...",
            "fr" to "Rechercher par nom ou note...",
            "en" to "Search by debtor or note..."
        ),
        "filter_unpaid" to mapOf(
            "mg" to "Mbola tsy voaloha",
            "fr" to "Non payées",
            "en" to "Unpaid"
        ),
        "filter_paid" to mapOf(
            "mg" to "Efa voaloha",
            "fr" to "Payées",
            "en" to "Paid"
        ),
        "filter_all" to mapOf(
            "mg" to "Rehetra",
            "fr" to "Toutes",
            "en" to "All"
        ),
        "debt_repay_title" to mapOf(
            "mg" to "Handoa ampahany amin'ny trosa",
            "fr" to "Remboursement partiel",
            "en" to "Partial Repayment"
        ),
        "repay_amount_label" to mapOf(
            "mg" to "Sora-bola hoaloa (Ar)",
            "fr" to "Montant remboursé (Ar)",
            "en" to "Repayment Amount (Ar)"
        ),
        "remaining_debt" to mapOf(
            "mg" to "Sisa trosany:",
            "fr" to "Solde restant :",
            "en" to "Remaining balance:"
        ),
        "repay_btn" to mapOf(
            "mg" to "Hanamarina ny fandoavana",
            "fr" to "Confirmer le paiement",
            "en" to "Confirm Repayment"
        ),
        "repaid_badge" to mapOf(
            "mg" to "Tafaloha",
            "fr" to "Payée",
            "en" to "Paid"
        ),
        "unpaid_badge" to mapOf(
            "mg" to "Trosa",
            "fr" to "En cours",
            "en" to "Unpaid"
        ),
        "no_sales_history" to mapOf(
            "mg" to "Tsy misy tantara momba ny varotra mbola voasoratra.",
            "fr" to "Aucune vente enregistrée dans l'historique.",
            "en" to "No sales recorded in history."
        ),
        "make_first_sale_btn" to mapOf(
            "mg" to "Hanao ny varotra voalohany",
            "fr" to "Faire la première vente",
            "en" to "Make your first sale"
        ),
        "calculator_multiplicator" to mapOf(
            "mg" to "Kalkilatora fampitomboana",
            "fr" to "Calculateur Multiplicateur",
            "en" to "Multiplier Calculator"
        ),
        "misc_item" to mapOf(
            "mg" to "Entana isan-karazany",
            "fr" to "Article divers",
            "en" to "Miscellaneous Item"
        ),
        "receipt_details_title" to mapOf(
            "mg" to "Détails ny recibu / fividianana",
            "fr" to "Détails du reçu de caisse",
            "en" to "Receipt Details"
        ),
        "settings_lang" to mapOf(
            "mg" to "Safidy ny fiteny (Langue)",
            "fr" to "Choisir la langue",
            "en" to "Select Language"
        ),
        "quick_misc_btn" to mapOf(
            "mg" to "Kajy haingana",
            "fr" to "Divers rapide",
            "en" to "Quick Misc"
        ),
        "cart_empty" to mapOf(
            "mg" to "Foana ny panier fividianana",
            "fr" to "Le panier est vide",
            "en" to "Cart is empty"
        ),
        "select_product" to mapOf(
            "mg" to "Hifidy entana",
            "fr" to "Choisir un produit",
            "en" to "Select product"
        ),
        "search_placeholder" to mapOf(
            "mg" to "Hikaroka entana...",
            "fr" to "Rechercher...",
            "en" to "Search..."
        ),
        "no_products_found" to mapOf(
            "mg" to "Tsy misy entana hita",
            "fr" to "Aucun produit trouvé",
            "en" to "No products found"
        ),
        "cash_given" to mapOf(
            "mg" to "Vola voaray",
            "fr" to "Espèces reçues",
            "en" to "Cash given"
        ),
        "checkout_success" to mapOf(
            "mg" to "Voaray ny fividianana ary nohavaozina ny tahiry.",
            "fr" to "Vente validée avec succès et stock mis à jour.",
            "en" to "Sale successfully validated and stock updated."
        ),
        "checkout_stock_error" to mapOf(
            "mg" to "Misy olana ny tahiry entana!",
            "fr" to "Erreur de stock lors de la validation !",
            "en" to "Stock error during validation!"
        )
    )

    fun translate(key: String, lang: String): String {
        val entry = translations[key] ?: return key
        return entry[lang] ?: entry["mg"] ?: key
    }
}
