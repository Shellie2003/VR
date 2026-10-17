package com.example.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.ui.partage.accueil.AccueilEcran
import com.example.ui.partage.coquille.CoquilleBureau
import com.example.ui.partage.coquille.SectionAVenir
import com.example.ui.partage.coquille.SectionBureau
import com.example.ui.partage.theme.VarotraTheme
import com.example.util.ShopMode
import com.example.util.enDecimal

/**
 * Point d'entrée de la version de bureau.
 *
 * La fenêtre n'est plus le simple témoin de la chaîne de build : elle sert un vrai écran — le
 * catalogue — rendu par `:core:ui` et calculé par le code partagé avec Android (prix, mise en
 * forme, filtrage). Le thème vient lui aussi de la charte commune : les quatre couleurs qui étaient
 * recopiées ici à la main ont disparu, c'est la dette que le découpage devait lever.
 *
 * Ce qui manque encore est nommé à l'écran plutôt que caché : sans base de données partagée (Room
 * KMP, délibérément remis à plus tard parce qu'il touche au fichier de base de chaque boutique en
 * service), la caisse, les dettes et les rapports n'ont rien à lire. Le catalogue affiche donc un
 * jeu de démonstration, et le dit.
 */
fun main() = application {
    val version = DesktopBuildInfo.versionCourante()

    Window(
        onCloseRequest = ::exitApplication,
        title = DesktopBuildInfo.titreFenetre(version),
        state = rememberWindowState(width = 1200.dp, height = 760.dp)
    ) {
        FenetreVarotra(version = version)
    }
}

@Composable
private fun FenetreVarotra(version: String?) {
    // Tout l'état de la fenêtre est ici, en un seul endroit : la version de bureau n'a pas encore
    // de ViewModel parce qu'elle n'a pas encore de source de données à observer. Le jour où elle en
    // aura une, les écrans n'auront pas à changer — ils reçoivent déjà leur état en paramètres.
    var sombre by remember { mutableStateOf(false) }
    var langue by remember { mutableStateOf("fr") }
    var modeBoutique by remember { mutableStateOf(ShopMode.DETAIL) }
    var section by remember { mutableStateOf(SectionBureau.CATALOGUE) }
    var recherche by remember { mutableStateOf("") }

    // Panier LOCAL à la fenêtre, et volontairement non enregistré : il n'existe que pour que le
    // badge de quantité et son bouton « - » de la carte produit soient réellement fonctionnels
    // plutôt que décoratifs. Sans base de données partagée, une vente ne peut aller nulle part —
    // afficher un panier qui prétendrait encaisser serait pire que de ne pas en avoir.
    var panier by remember { mutableStateOf<Map<Int, Double>>(emptyMap()) }

    VarotraTheme(sombre = sombre) {
        CoquilleBureau(
            nomApplication = DesktopBuildInfo.NOM_APPLICATION,
            sousTitre = when (langue) {
                "mg" -> "Varotra sy Tahiry — ordinatera"
                "fr" -> "Caisse & stock — bureau"
                else -> "POS & inventory — desktop"
            },
            sectionActive = section,
            onSectionChange = { section = it },
            langue = langue,
            piedMenu = {
                ReglagesMenu(
                    langue = langue,
                    onLangueChange = { langue = it },
                    sombre = sombre,
                    onSombreChange = { sombre = it },
                    modeBoutique = modeBoutique,
                    onModeChange = { modeBoutique = it }
                )
            }
        ) { sectionActive ->
            when (sectionActive) {
                SectionBureau.CATALOGUE -> Column(modifier = Modifier.fillMaxSize()) {
                    BandeauDemonstration(langue = langue)
                    AccueilEcran(
                        produits = DonneesDemo.catalogue,
                        // Sur Android ce nom vient des préférences (écran Épicerie). Le bureau n'a
                        // pas encore de préférences partagées, donc le nom de l'application tient
                        // la place — jamais un faux nom de boutique, qui ferait croire à une donnée.
                        nomEpicerie = DesktopBuildInfo.NOM_APPLICATION,
                        recherche = recherche,
                        onRechercheChange = { recherche = it },
                        quantitesPanier = panier,
                        onAjouter = { produit ->
                            val actuel = panier[produit.id] ?: 0.0
                            panier = panier + (produit.id to actuel + 1.0)
                        },
                        onRetirer = { produit ->
                            val actuel = panier[produit.id] ?: 0.0
                            // En dessous de 1, la ligne disparaît du panier : c'est ce que fait le
                            // bouton « - » d'Android, et non un passage à zéro qui laisserait un
                            // badge « 0 » affiché sur la carte.
                            panier = if (actuel <= 1.0) panier - produit.id
                            else panier + (produit.id to actuel - 1.0)
                        },
                        modeBoutique = modeBoutique,
                        langue = langue,
                        // `themeColor` et `colorScheme.primary` sont la même couleur à l'exécution
                        // côté Android (MainActivity passe l'une comme surcharge de l'autre) : la
                        // carte reçoit donc ici exactement la teinte qu'elle a sur le téléphone.
                        couleurTheme = MaterialTheme.colorScheme.primary
                    )
                }

                SectionBureau.A_PROPOS -> SectionAPropos(version = version, langue = langue)

                SectionBureau.CAISSE,
                SectionBureau.DETTES,
                SectionBureau.RAPPORTS ->
                    SectionAVenir(titre = sectionActive.libelle(langue), langue = langue)
            }
        }
    }
}

/**
 * Dit franchement que les produits affichés ne sont pas ceux d'une boutique.
 *
 * Sans ce bandeau, un gérant à qui l'on montre la version de bureau croirait que ses données sont
 * déjà là, et conclurait à une perte de données en ne reconnaissant aucun article.
 */
@Composable
private fun BandeauDemonstration(langue: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = when (langue) {
                "mg" -> "Entana fanandramana ity : mbola tsy mifandray amin'ny tahiry " +
                    "an-tsokosoko ny ordinatera."
                "fr" -> "Produits de démonstration : la version de bureau n'est pas encore " +
                    "reliée à la base de données de la boutique."
                else -> "Demonstration products: the desktop version is not connected to the " +
                    "shop database yet."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun ReglagesMenu(
    langue: String,
    onLangueChange: (String) -> Unit,
    sombre: Boolean,
    onSombreChange: (Boolean) -> Unit,
    modeBoutique: ShopMode,
    onModeChange: (ShopMode) -> Unit
) {
    // Les puces sont posées sur l'aplat primaire du menu : leurs couleurs par défaut, calculées
    // pour une surface, y seraient illisibles — d'où le couple explicite onPrimary/primary, le même
    // raisonnement que pour la carte de facturation de la caisse Android.
    val couleurs = FilterChipDefaults.filterChipColors(
        labelColor = MaterialTheme.colorScheme.onPrimary,
        selectedContainerColor = MaterialTheme.colorScheme.onPrimary,
        selectedLabelColor = MaterialTheme.colorScheme.primary
    )

    Column {
        Text(
            text = when (langue) {
                "mg" -> "Fandrindrana"
                "fr" -> "Réglages"
                else -> "Settings"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("mg", "fr", "en").forEach { code ->
                FilterChip(
                    selected = langue == code,
                    onClick = { onLangueChange(code) },
                    label = { Text(code.uppercase()) },
                    colors = couleurs
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        FilterChip(
            selected = sombre,
            onClick = { onSombreChange(!sombre) },
            label = {
                Text(
                    when (langue) {
                        "mg" -> "Maizina"
                        "fr" -> "Mode sombre"
                        else -> "Dark mode"
                    }
                )
            },
            colors = couleurs
        )

        Spacer(Modifier.height(8.dp))

        // Le mode de boutique change le prix affiché par `PriceUtil` — exactement comme sur le
        // téléphone. Le mettre à portée du catalogue permet de le constater d'un clic.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ShopMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == modeBoutique,
                    onClick = { onModeChange(mode) },
                    label = { Text(mode.libelle(langue)) },
                    colors = couleurs
                )
            }
        }
    }
}

@Composable
private fun SectionAPropos(version: String?, langue: String) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text(
            text = SectionBureau.A_PROPOS.libelle(langue),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(20.dp))

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                LigneInfo("Interface", "Compose Multiplatform (JVM)")
                LigneInfo("Version du paquet", version ?: "lancée depuis les sources")
                LigneInfo("Java", System.getProperty("java.version") ?: "inconnue")
                LigneInfo(
                    "Système",
                    "${System.getProperty("os.name")} ${System.getProperty("os.arch")}"
                )
                LigneInfo(
                    "Modules partagés",
                    ":core:model · :core:logic · :core:ui"
                )
                Spacer(Modifier.height(20.dp))
                DemoMontantPartage()
            }
        }
    }
}

@Composable
private fun LigneInfo(libelle: String, valeur: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = libelle,
            modifier = Modifier.width(200.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valeur,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Démonstration vivante du code partagé, et non une simple mention dans une liste.
 *
 * `NombreUtil` est la règle qui lit les montants saisis à la caisse — virgule ou point, espaces de
 * milliers, devise en suffixe. C'est le calcul dont une erreur ne se verrait PAS à l'écran mais
 * produirait une monnaie rendue fausse. Le voir fonctionner ici prouve qu'une seule source de
 * vérité sert les deux plateformes : si cette règle change côté Android, elle change ici, sans
 * recopie.
 *
 * Un champ interactif plutôt qu'une valeur figée : une constante affichée prouverait seulement que
 * le module se compile, pas que la règle s'exécute.
 */
@Composable
private fun DemoMontantPartage() {
    var saisie by remember { mutableStateOf("12 500,50 Ar") }
    val valeur = saisie.enDecimal()

    Column {
        Text(
            text = "Lecture d'un montant (règle partagée avec la caisse Android)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = saisie,
            onValueChange = { saisie = it },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Text(
            // Une saisie illisible renvoie `null` et non zéro — c'est la garantie centrale de
            // `NombreUtil` : une faute de frappe ne doit jamais devenir un montant nul silencieux.
            text = valeur?.let { "Valeur lue : $it" } ?: "Saisie refusée (ce n'est pas un nombre)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (valeur == null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
