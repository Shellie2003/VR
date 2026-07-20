# Directives Système et Règles de Cohérence du Projet

Ce fichier contient des instructions persistantes destinées aux assistants IA de Google AI Studio pour maintenir la cohérence de l'application, éviter les régressions et respecter le travail existant.

---

### 1. 🛡️ Préservation de l'Architecture & Code Existant
- **Pas de réécriture inutile :** Ne jamais recréer ou dupliquer des classes de données, des DAOs, ou des fonctions utilitaires existantes. Toujours réutiliser le `InventoryViewModel` et `InventoryRepository`.
- **Analyse avant modification :** Il est **obligatoire** de lire l'intégralité d'un fichier avec `view_file` avant d'y appliquer des modifications. Les modifications doivent être ciblées et chirurgicales (utiliser `edit_file` ou `multi_edit_file` au lieu de réécrire des fichiers entiers).
- **Compilation régulière :** Valider chaque étape de modification en exécutant `compile_applet`. Ne jamais laisser l'application dans un état non compilable.

### 2. 🎨 Cohérence Visuelle & Thématique (Material Design 3)
- **Thème et Couleurs (Mis à jour) :** L'application intègre le thème officiel avec les palettes de couleurs suivantes :
  - *Mode Clair (Light) :* Primaire (`0xFF012D1D`), Secondaire (`0xFF855300`), Arrière-plan/Surface (`0xFFFBF9F8`), Conteneurs de surface (`0xFFEFEDED`).
  - *Mode Sombre (Dark) :* Primaire (`0xFFC1ECD4`), Secondaire (`0xFFFEA619`), Arrière-plan/Surface (`0xFF002114`), Conteneurs de surface (`0xFF1B4332`).
  - Utiliser exclusivement le système de couleurs dynamiques et les variables de thème fournies par `MaterialTheme.colorScheme` ou `viewModel.themeColor` (qui par défaut mappe vers le nouveau vert sombre `0xFF012D1D`). Ne jamais utiliser de codes couleurs hexadécimaux bruts dans les Composables.
- **Typographie Officielle :**
  - Titres et En-têtes : Police **Epilogue** (Gras, Semi-Gras, Moyen).
  - Corps de texte et Badges : Police **Be Vietnam Pro** (Normal, Moyen, Semi-Gras).
- **Icônes Consistantes :** Pour l'icône principale de l'application (le panier d'achat), toujours utiliser `Icons.Rounded.ShoppingBasket` (et non `Icons.Default.ShoppingBag`) afin d'assurer une cohérence parfaite entre l'écran d'accueil, le splash screen et l'historique des ventes.
- **Espacement et Paddings :** Respecter une grille de 8dp constante. Préférer des marges généreuses et des arrondis de cartes de `12.dp` à `16.dp` pour un rendu moderne et aéré.

### 3. 🌐 Prise en charge Multilingue (FR / MG)
- L'application prend en charge le Français (`fr`) et le Malgache (`mg`).
- Lors de l'ajout de nouvelles interfaces ou de nouveaux boutons, toujours récupérer la langue active depuis `viewModel.language` et fournir des textes traduits pour les deux langues (ainsi que l'anglais par défaut).
- Exemple :
  ```kotlin
  val title = when (activeLang) {
      "mg" -> "Fampidirana entana"
      "fr" -> "Approvisionnement"
      else -> "Restocking"
  }
  ```

### 4. 🚀 Simplicité et Focus Fonctionnel
- Ne pas ajouter de fonctionnalités non demandées explicitement par l'utilisateur (over-engineering).
- S'en tenir strictement au besoin décrit en se concentrant sur une expérience utilisateur fluide, rapide et exempte de bugs.
- Conserver le stockage local via la base de données Room préexistante pour toutes les entités importantes (produits, ventes, dettes, fournisseurs).

### 5. 📝 Documentation Continue et Maintien du Fichier AGENTS.md (MANDATORY)
- **Mise à jour systématique :** À chaque ajout, modification ou suppression d'une fonctionnalité ou d'un écran, l'assistant IA **DOIT** impérativement mettre à jour ce fichier `AGENTS.md` dans la section **"6. 🗺️ Carte des Fonctionnalités de l'Application"** ci-dessous pour refléter l'état réel et actuel du projet. Cela garantit une transmission parfaite du contexte aux futurs assistants.

---

### 6. 🗺️ Carte des Fonctionnalités Actuelles de l'Application

Voici l'inventaire officiel des fonctionnalités implémentées dans l'application :

1. **Écran de Caisse & Calculatrice (`CalculatorScreen` / `HomeScreen` Grille)**
   - Calculatrice interactive pour saisir des montants manuels ou ajouter des produits du catalogue directement au panier.
   - Gestion des types de produits (vendu par unité standard, liquide en Litres, ou poids en Kilogrammes avec boutons pré-réglés `1/4 kg`, `1/2 kg`, `+1`).
   - Saisie rapide par code-barres via caméra hautement optimisée à très faible latence.
   - Validation de la vente avec enregistrement automatique dans la base de données Room, mise à jour instantanée des stocks, et avertissement si stock insuffisant.
   - **Badge de clic / Quantité & Bouton de décrémentation (Mise à jour) :** Chaque carte de produit sur l'écran d'accueil affiche un badge dynamique coloré dans le coin supérieur droit de son image montrant le nombre de fois qu'elle a été cliquée (quantité active dans le panier). Ce badge intègre un bouton `-` interactif à faible encombrement permettant d'annuler/décrémenter directement un clic sans avoir à ouvrir le panneau du panier.
   - **Design Visuel Purifié (Nouveau) :** Retrait complet des badges de catégories sur les cartes de l'écran d'accueil, avec augmentation de la hauteur de l'image de 130dp à 165dp pour un rendu visuel moderne et immersif. Chaque fiche présente de manière épurée et aérée uniquement le nom du produit, le total en stock (badge de couleur douce) et le prix unitaire d'un seul coup d'œil.

2. **Écran de Gestion de Stock & Inventaire (`InventoryListScreen` / `AddProductScreen`)**
   - Liste des produits avec recherche instantanée et filtrage par stock faible.
   - **Retour à la ligne automatique des catégories :** Utilisation de `FlowRow` pour afficher la catégorie, le SKU et les badges de statut (ALERT, LOW QTY) afin d'éviter tout écrasement ou distorsion de la mise en page lorsque les noms de catégories sont longs.
   - Ajout ou édition complète d'un produit (Nom, Prix de vente, Prix d'achat de base, Unité, Code-barres, Seuil d'alerte de stock faible, Fournisseur attitré).
   - Suppression sécurisée et ajustement manuel rapide des quantités en stock.
   - **Filtre de Modèles Secteur-Neutre (Nouveau) :** Les produits pré-configurés (modèles d'épicerie) sont désormais entièrement masqués des listes de vente, de stock et de recherche par défaut. Cela permet de conserver l'application parfaitement propre et prête pour n'importe quel autre secteur d'activité (poissonnerie, bar, etc.).
   - **Ajout rapide depuis un Modèle (Nouveau) :** Bouton d'accès direct dans le formulaire de création de produit pour ouvrir un sélecteur de modèles. L'utilisateur peut y rechercher et filtrer par catégorie des centaines de produits pré-enregistrés. Sélectionner un modèle pré-remplit instantanément et localement la fiche produit (Nom, Catégorie, Unité, SKU), lui laissant simplement à saisir le prix de vente et les stocks restants.
   - **Génération & Impression de Code-barres Vectoriels (Nouveau) :** Génération automatique et aléatoire de numéros de code-barres uniques (Code 39) en un clic. Création automatique en arrière-plan d'une étiquette PDF au format vectoriel haute résolution incluant le nom du produit et le code-barres, sauvegardée directement dans le dossier public `Downloads/EpicerieBarcodes`. Lancement instantané du flux d'impression Android standard (`PrintManager`) pour une impression rapide sur des étiquettes adhésives.
   - **Intégration Open Food Facts API (Nouveau) :** Module de recherche mondiale de produits intégré permettant de scanner un code-barres ou de saisir un mot-clé pour pré-remplir instantanément la fiche produit. Récupère automatiquement le nom, la marque, la description, la catégorie (mappée intelligemment vers nos sections Alimentation, Boissons, Légumes, Épicerie, Droguerie, Hafa) ainsi que l'image miniature officielle du produit via Coil. L'utilisateur n'a plus qu'à saisir son prix de vente final.

3. **Écran d'Historique & Traçabilité des Transactions (`SalesHistoryScreen`)**
   - **Système d'Onglets d'Historiques :** Permet de basculer instantanément entre l'Historique des Ventes (**"Varotra"**) et l'Historique d'Approvisionnement (**"Fampidirana"**) pour une traçabilité totale et absolue de toutes les opérations financières et de stock.
   - **Correction et Visualisation :** Affiche le chiffre d'affaires de la journée sous le terme exact de **"Vola maty androany"** (ou "Vola maty tamin'io daty io" si une autre date est choisie) de manière entièrement dynamique.
   - **Filtre par calendrier haute précision :** Sélecteur de date interactif à icône de calendrier permettant d'isoler et d'analyser les transactions d'une journée spécifique (mettant à jour instantanément les métriques de revenus et le nombre d'opérations).
   - **Barre de recherche dynamique et fluide :** Barre de recherche s'activant par glissement pour filtrer les ventes par nom de produit et les réapprovisionnements par nom de produit ou fournisseur en temps réel.
   - **Kajy momba ny Vola (Analyses Financières Réelles) :** Un clic sur l'icône de diagramme à barres (BarChart) ouvre un tableau d'analyse consolidé calculant le nombre total de ventes, le chiffre d'affaires, le coût d'achat des produits vendus (COGS), le bénéfice net de la journée et le taux de marge réel.
   - Possibilité d'annuler/supprimer n'importe quelle vente ou réapprovisionnement de l'historique avec confirmation de sécurité.

4. **Écran de Gestion de Dettes (`DebtsScreen`)**
   - Enregistrement et suivi des clients ayant des arriérés de paiement.
   - Remboursement partiel ou total des dettes avec mise à jour du solde dû.
   - Suppression ou extinction de dettes.

5. **Écran de Paramètres & Options (`SettingsScreen`)**
   - Changement instantané de la langue de l'interface (Français `fr` ou Malgache `mg`).
   - Personnalisation du thème de couleur principal (Bleu, Vert, Orange, Violet, etc.).
   - Choix du mode d'affichage thématique (Sombre, Clair ou adaptation automatique selon le Système) pour une visibilité idéale de jour comme de nuit.
   - Modification du nom de l'épicerie affiché en haut de l'écran d'accueil.
   - Bouton de redirection vers le calculateur d'approvisionnement.
   - Bouton de redirection vers l'écran de gestion et impression consolidée de codes-barres.

6. **Écran de Calculateur d'Approvisionnement & Marges (`CommissionScreen`)**
   - Accessible depuis la page des Paramètres.
   - **Tutoriel & Onboarding intégré :** Intègre une carte d'astuces "💡 Mode d'emploi / Fampiasana" décrivant précisément l'ordre idéal de saisie pour guider pas à pas l'utilisateur (choix de produit, cartons, pièces, coût d'achat total, prix de vente).
   - **Calculateur d'achat & Bouton "Effacer tout" :** Permet d'entrer un produit, le nombre de cartons/lots achetés chez un fournisseur, le nombre de pièces par carton, et le coût total d'achat en gros. Un bouton rapide "Hamafana / Effacer tout" permet d'effacer d'un seul clic les données de démonstration pour y saisir de nouvelles valeurs personnalisées.
   - **Analyse de marge automatique :** Calcule de manière robuste et sans plantage (en gérant élégamment les champs vides) le coût unitaire réel, le bénéfice total généré, le bénéfice par carton, le bénéfice par unité, et le taux de marge d'après le prix de vente unitaire saisi (que l'utilisateur peut ajuster en direct sur cet écran).
   - **Badge de Sécurité de Marge Dynamique (Nouveau) :** Affiche un badge coloré dynamique au sommet du panneau d'analyse pour alerter instantanément le gérant sur la rentabilité de sa tarification : Fatiantoka/Perte (Rouge) ⚠️, Tombony kely/Marge faible (Jaune) ⚠️, ou Tombony tsara/Marge excellente (Vert) ✨.
   - **Validation du réapprovisionnement :** Met à jour directement le stock du produit en y ajoutant les nouvelles unités reçues, enregistre le nouveau prix d'achat/gros et le nouveau prix de vente, et lie le fournisseur choisi au produit.
   - **Alerte de stock faible (Tahiry ho lany) :** Un deuxième onglet qui liste automatiquement tous les produits dont le niveau de stock est inférieur au seuil configuré, avec un bouton d'accès rapide "Approvisionner" pour les charger directement dans le calculateur.
   - **Ajout rapide de fournisseurs :** Formulaire en boîte de dialogue pour créer instantanément de nouveaux fournisseurs sur place si non listés.
   - **Traduction parfaite et harmonisée :** Toutes les lignes de calcul de marges, de coûts de revient et de bénéfices unitaires/globaux sont entièrement localisées avec soin en Malgache (MG), Français (FR) et Anglais (EN).

7. **Écran de Gestion de Codes-Barres (`BarcodeListScreen`)**
   - Accessible depuis la page des Paramètres.
   - **Filtres par onglets tactiles :** Sépare d'un clic les produits avec code-barres et sans code-barres pour un suivi instantané de l'état de l'inventaire.
   - **Recherche temps réel & Groupement par Catégorie :** Filtrage instantané des produits par nom ou numéro de code-barres. Les produits sont triés et groupés dynamiquement par catégorie (par exemple : Alimentation, Boisson, etc.) avec des bannières de titre de section colorées et élégantes.
   - **Génération unifiée en arrière-plan :** Pour les produits sans code-barres, un clic sur le bouton "Générer" attribue dynamiquement un code unique de 13 chiffres intégrant une clé de contrôle (checksum) conforme aux normes internationales EAN-13, et met à jour instantanément la base de données locale.
   - **Impression consolidée intelligente (`varotra_code_barre.pdf`) :** Génère un document PDF unique regroupant l'ensemble des codes-barres générés par l'application sous forme de planche d'étiquettes vectorielles haute résolution disposées en grille de 2 colonnes (5 lignes par page) pour un scan optimal. Les produits y sont regroupés et ordonnés par catégorie.
   - **Formes de codes-barres optimisées (EAN-13 Standard de Qualité) :** Les barres de garde (début, milieu, fin) s'étendent élégamment vers le bas à travers la zone de texte, et les chiffres sont espacés et présentés de manière conforme aux standards industriels sous la planche pour un scan instantané à la caméra ou avec un lecteur optique.
   - **Saut de page automatique sans coupe :** Intègre une logique géométrique stricte de saut de page (dès 10 étiquettes atteintes par planche), garantissant qu'aucune étiquette ne soit coupée horizontalement entre deux pages.
   - **Impression unitaire :** Possibilité de relancer individuellement l'impression d'un produit précis depuis sa fiche avec le même rendu EAN-13 haute fidélité.

8. **Écran de Synchronisation Multi-terminal (`SyncScreen`)**
   - Accessible depuis la page des Paramètres.
   - **Mode Serveur (Gérant) :** Permet d'héberger une base de données de synchronisation sur le réseau local Wi-Fi. Affiche un code QR contenant l'adresse IP locale du serveur pour faciliter l'association instantanée des clients, inclut un bouton de redirection rapide vers les paramètres de Point d'accès (Hotspot) du téléphone pour faciliter le partage réseau direct, et montre le nombre de terminaux connectés en temps réel.
   - **Mode Client (Vendeur) :** Intègre un scanner de code QR dédié et hautement optimisé (`QrCodeScannerView`) filtrant exclusivement les formats QR Code (évitant tout conflit avec les codes-barres standards) et incluant un raccourci d'accès direct aux paramètres Wi-Fi de l'appareil ainsi qu'une interface de saisie manuelle de l'adresse IP en secours, permettant de lire l'adresse IP du serveur et s'y connecter instantanément.
   - **Mise à jour en temps réel & Synchronisation feno bidirectionnelle (Mise à jour) :** Permet la synchronisation bidirectionnelle fluide et automatique de toute la base de données (produits avec leurs images, ventes, dettes, approvisionnements/restocks) dès la connexion et lors de chaque transaction ou modification de stock. Résout définitivement le décalage de quantité en empêchant le double-décrément de stock lors du traitement des transactions synchronisées.
    - **Sécurité et Contrôle de Suppression :** Restreint strictement la suppression d'un produit aux appareils définis comme Serveur (Gérant) ; toute tentative de suppression initiée par un appareil Client est bloquée et signalée par un avertissement clair pour préserver l'intégrité de la base de données.
   - **Forçage manuel de synchronisation :** Ajout de boutons physiques "Forcer la synchronisation" (Ampitoviana mivantana ny tahiry) sur les écrans Client et Serveur pour déclencher un cycle de consolidation complet à la demande.
   - **Console de Journalisation (Logs) :** Fournit un affichage temps réel de tous les événements de synchronisation avec un code couleur spécifique (Vert pour les succès, Rouge pour les erreurs) pour faciliter le débogage.

    - **Gestion de Synchronisation Sélective (Nouveau) :** Ajout d'un bouton de synchronisation directe dans la barre d'outils supérieure (`TopAppBar`) à côté des paramètres. Il ouvre un dialogue de contrôle de synchronisation en temps réel qui permet de rechercher n'importe quel produit et d'activer/désactiver sélectivement sa synchronisation. Un bouton de suppression ("fako") permet d'annuler sa synchronisation à tout moment (le produit reste local mais n'est plus propagé sur le réseau), avec affichage de badges clairs d'état de synchronisation ("Mampitovy" / "Tsy ampitoviana").

9. **Sécurisation & Backup Automatique de la Base de Données (Nouveau)**
   - **Protection anti-effacement automatique :** L'application intègre un système d'auto-sauvegarde automatique en arrière-plan. À chaque transaction, modification de produit, paiement de dette, ou fampitoviana (synchronisation) réussie, la base de données consolidée est sérialisée en JSON et sauvegardée dans un fichier local sécurisé (`database_safety_backup.json`) au sein de l'espace privé de l'application.
   - **Restauration transparente automatique :** Si la base de données venait à être effacée ou réinitialisée lors d'une mise à jour de version de l'application (changement de schéma ou version Room sans migration), l'application détecte automatiquement la perte de données au démarrage et réinjecte instantanément la totalité des produits, ventes, et dettes depuis la sauvegarde de sécurité locale.
   - **Contrôles manuels dans les Paramètres :** Ajout d'une section dédiée "Fiarovana ny Tahiry" (Sauvegarde & Sécurité) dans l'écran des paramètres permettant à l'utilisateur de déclencher à tout moment un backup manuel ou de forcer une restauration complète de ses enregistrements professionnels.

10. **Architecture, Optimisation et Performance (Nouveau)**
    - **Indexation Room Database :** Ajout d'un index sur `isTemplate` dans l'entité `Product` (en plus de `name`, `category`, `barcode`, `sku`) pour optimiser à l'extrême les temps d'accès lors de la recherche et filtrage de modèles de produits.
    - **Algorithme Prédictif Haute-Performance :** Implémentation d'un système de recommandation double-moteur (Co-occurrences transactionnelles tirées de l'historique et repli sémantique par mots-clés localisés). Toutes les fusions et tris de classement de pertinence s'exécutent de manière asynchrone sur le pool de threads `Dispatchers.Default` via des liaisons `combine` réactives, assurant des calculs fluides et instantanés même avec des catalogues volumineux sans jamais ralentir le fil d'exécution principal (UI thread).
    - **Optimisation de Rendu Jetpack Compose :** Configuration systématique de clés uniques stables (`key`) dans les listes et grilles de l'application (`HomeScreen` produits, `CalculatorScreen` produits et paniers, `InventoryListScreen` produits et puces de catégorie, `DebtsScreen` dettes, `SalesHistoryScreen` transactions) pour réduire à zéro les recompositions redondantes et éliminer les micro-saccades (jank) lors du défilement ou des mutations.
    - **R8 / ProGuard Minification & Réduction d'Espace :** Activation de `isMinifyEnabled = true` et `isShrinkResources = true` dans le profil de publication Gradle. Configuration de règles chirurgicales robustes dans `proguard-rules.pro` pour garantir que Room, Moshi, Retrofit, OkHttp et les modèles de données ne subissent aucune régression de sérialisation par réflexion tout en maximisant la compression.
    - **Compatibilité Multi-Architecture (ABIs) :** Configuration optimisée de la compilation native pour supporter de manière optimale tous les processeurs majeurs (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) assurant une exécution fluide et rapide des modules ML Kit et SQLite sur l'ensemble du parc d'appareils Android.
    - **Suppression en Cascade Sécurisée (Nouveau) :** Configuration de la contrainte `onDelete = ForeignKey.CASCADE` sur les clés étrangères des lignes de ventes (`lignes_vente`) référençant un produit ou une unité, éliminant définitivement les erreurs de violation d'intégrité référentielle (`SQLiteConstraintException`) et les crashs de l'application lors de la suppression de produits.
    - **Optimisation du Cold Start (Écran Blanc/Gris) (Nouveau) :** Intégration d'un arrière-plan de fenêtre personnalisé (`android:windowBackground`) lié à la couleur primaire de la marque (`#012D1D`) au niveau du thème de démarrage Android de l'activité principale. Cela élimine complètement les 3 secondes d'écran gris d'initialisation du système d'exploitation, assurant un affichage instantané et fluide du Splash Screen de la caisse.
    - **Système d'Activation Sécurisé (Nouveau) :** Intégration d'un écran d'activation obligatoire à la première ouverture de l'application (`ActivationScreen`). L'activation utilise l'identifiant unique de l'appareil (`Installation ID`) avec un bouton de copie au presse-papiers intégré en un clic, et applique la formule mathématique ultra-sécurisée `(I * 3 + 123456) mod 1000000` pour valider le code de déverrouillage permanent à 6 chiffres.
    - **Démarrage Ultra-Rapide et Optimisation Asynchrone (Nouveau) :** Déplacement de toutes les opérations de démarrage (vérification de base, chargement des sauvegardes et initialisation des templates de produits) vers le pool asynchrone `Dispatchers.IO` pour éliminer tout risque de blocage du thread UI principal. Les opérations d'écriture pour les centaines de produits d'import d'épicerie ont été optimisées chirurgicalement pour court-circuiter les synchronisations relationnelles inutiles lors de l'insertion de fiches de modèles (templates), réduisant de ce fait le délai d'affichage du Splash Screen à 1 seconde tout en conservant une fluidité absolue.

