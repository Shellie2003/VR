# Directives Système et Règles de Cohérence du Projet

Ce fichier contient des instructions persistantes destinées aux assistants IA de Google AI Studio pour maintenir la cohérence de l'application, éviter les régressions et respecter le travail existant.

---

### 1. 🛡️ Préservation de l'Architecture & Code Existant
- **Pas de réécriture inutile :** Ne jamais recréer ou dupliquer des classes de données, des DAOs, ou des fonctions utilitaires existantes. Toujours réutiliser le `InventoryViewModel` et `InventoryRepository`.
- **Analyse avant modification :** Il est **obligatoire** de lire l'intégralité d'un fichier avec `view_file` avant d'y appliquer des modifications. Les modifications doivent être ciblées et chirurgicales (utiliser `edit_file` ou `multi_edit_file` au lieu de réécrire des fichiers entiers).
- **Compilation régulière :** Valider chaque étape de modification en exécutant `compile_applet`. Ne jamais laisser l'application dans un état non compilable.

### 2. 🎨 Cohérence Visuelle & Thématique (Material Design 3)
- **Thème et Couleurs :** Utiliser exclusivement le système de couleurs dynamiques et les variables de thème fournies par le `viewModel.themeColor` ou `MaterialTheme.colorScheme`. Ne jamais utiliser de codes couleurs hexadécimaux bruts dans les Composables.
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

1. **Écran de Caisse & Calculatrice (`CalculatorScreen`)**
   - Calculatrice interactive pour saisir des montants manuels ou ajouter des produits du catalogue directement au panier.
   - Gestion des types de produits (vendu par unité standard, liquide en Litres, ou poids en Kilogrammes avec boutons pré-réglés `1/4 kg`, `1/2 kg`, `+1`).
   - Saisie rapide par code-barres via caméra hautement optimisée à très faible latence (mise au point automatique centrale intelligente, tap-to-focus manuel, optimisation du processeur d'image matériel via le mode scène Code-barres, et sélecteur de zoom rapide tactile 1x/2x/3x pour lire les codes minuscules ou lointains) ou scanner physique externe.
   - Validation de la vente avec enregistrement automatique dans la base de données Room, mise à jour instantanée des stocks, et avertissement si stock insuffisant.

2. **Écran de Gestion de Stock & Inventaire (`InventoryListScreen` / `AddProductScreen`)**
   - Liste des produits avec recherche instantanée et filtrage par stock faible.
   - **Retour à la ligne automatique des catégories :** Utilisation de `FlowRow` pour afficher la catégorie, le SKU et les badges de statut (ALERT, LOW QTY) afin d'éviter tout écrasement ou distorsion de la mise en page lorsque les noms de catégories sont longs.
   - Ajout ou édition complète d'un produit (Nom, Prix de vente, Prix d'achat de base, Unité, Code-barres, Seuil d'alerte de stock faible, Fournisseur attitré).
   - Suppression sécurisée et ajustement manuel rapide des quantités en stock.
   - **Génération & Impression de Code-barres Vectoriels (Nouveau) :** Génération automatique et aléatoire de numéros de code-barres uniques (Code 39) en un clic. Création automatique en arrière-plan d'une étiquette PDF au format vectoriel haute résolution incluant le nom du produit et le code-barres, sauvegardée directement dans le dossier public `Downloads/EpicerieBarcodes`. Lancement instantané du flux d'impression Android standard (`PrintManager`) pour une impression rapide sur des étiquettes adhésives.
   - **Intégration Open Food Facts API (Nouveau) :** Module de recherche mondiale de produits intégré permettant de scanner un code-barres ou de saisir un mot-clé pour pré-remplir instantanément la fiche produit. Récupère automatiquement le nom, la marque, la description, la catégorie (mappée intelligemment vers nos sections Alimentation, Boissons, Légumes, Épicerie, Droguerie, Hafa) ainsi que l'image miniature officielle du produit via Coil. L'utilisateur n'a plus qu'à saisir son prix de vente final.

3. **Écran d'Historique des Ventes (`SalesHistoryScreen`)**
   - Journal chronologique détaillé de toutes les transactions effectuées.
   - Visualisation claire des gains, du chiffre d'affaires et de la marge bénéficiaire globale sur des périodes choisies.
   - Possibilité d'annuler/supprimer une vente (réajustant automatiquement le stock associé).

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
   - **Calculateur d'achat :** Permet d'entrer un produit, le nombre de cartons/lots achetés chez un fournisseur, le nombre de pièces par carton, et le coût total d'achat en gros.
   - **Analyse de marge automatique :** Calcule instantanément le coût unitaire réel, le bénéfice total généré, le bénéfice par carton, le bénéfice par unité, et le taux de marge d'après le prix de vente unitaire saisi (que l'utilisateur peut ajuster en direct sur cet écran).
   - **Validation du réapprovisionnement :** Met à jour directement le stock du produit en y ajoutant les nouvelles unités reçues, enregistre le nouveau prix d'achat/gros et le nouveau prix de vente, et lie le fournisseur choisi au produit.
   - **Alerte de stock faible (Tahiry ho lany) :** Un deuxième onglet qui liste automatiquement tous les produits dont le niveau de stock est inférieur au seuil configuré, avec un bouton d'accès rapide "Approvisionner" pour les charger directement dans le calculateur.
   - **Ajout rapide de fournisseurs :** Formulaire en boîte de dialogue pour créer instantanément de nouveaux fournisseurs sur place si non listés.

7. **Écran de Gestion de Codes-Barres (`BarcodeListScreen`)**
   - Accessible depuis la page des Paramètres.
   - **Filtres par onglets tactiles :** Sépare d'un clic les produits avec code-barres et sans code-barres pour un suivi instantané de l'état de l'inventaire.
   - **Recherche temps réel & Groupement par Catégorie :** Filtrage instantané des produits par nom ou numéro de code-barres. Les produits sont triés et groupés dynamiquement par catégorie (par exemple : Alimentation, Boisson, etc.) avec des bannières de titre de section colorées et élégantes.
   - **Génération unifiée en arrière-plan :** Pour les produits sans code-barres, un clic sur le bouton "Générer" attribue dynamiquement un code unique de 13 chiffres intégrant une clé de contrôle (checksum) conforme aux normes internationales EAN-13, et met à jour instantanément la base de données locale.
   - **Impression consolidée intelligente (`varotra_code_barre.pdf`) :** Génère un document PDF unique regroupant l'ensemble des codes-barres générés par l'application sous forme de planche d'étiquettes vectorielles haute résolution disposées en grille de 2 colonnes (5 lignes par page) pour un scan optimal. Les produits y sont regroupés et ordonnés par catégorie.
   - **Formes de codes-barres optimisées (EAN-13 Standard de Qualité) :** Les barres de garde (début, milieu, fin) s'étendent élégamment vers le bas à travers la zone de texte, et les chiffres sont espacés et présentés de manière conforme aux standards industriels sous la planche pour un scan instantané à la caméra ou avec un lecteur optique.
   - **Saut de page automatique sans coupe :** Intègre une logique géométrique stricte de saut de page (dès 10 étiquettes atteintes par planche), garantissant qu'aucune étiquette ne soit coupée horizontalement entre deux pages.
   - **Impression unitaire :** Possibilité de relancer individuellement l'impression d'un produit précis depuis sa fiche avec le même rendu EAN-13 haute fidélité.
