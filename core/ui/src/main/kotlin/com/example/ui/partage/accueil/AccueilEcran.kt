package com.example.ui.partage.accueil

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.util.CatalogueFiltre
import com.example.util.ShopMode

/**
 * L'écran d'accueil, **porté depuis `HomeScreen` d'Android** : le même en-tête, la même grille de
 * cartes, les mêmes espacements.
 *
 * **La seule adaptation au format PC est le nombre de colonnes, et elle est calculée pour NE PAS
 * changer le design.** Android fixe 2 colonnes sur téléphone et 3 sur tablette
 * (`GridCells.Fixed(if (isTablet) 3 else 2)`). Reprendre `Fixed(3)` sur une fenêtre de 1400 px
 * donnerait des cartes de plus de 400 px de large pour une zone d'image qui reste haute de 165dp :
 * la carte se déformerait, et c'est précisément ce qu'on nous demande d'éviter. `Adaptive(240.dp)`
 * conserve au contraire la **largeur de carte de la tablette** — 3 colonnes dans ~800dp moins les
 * 48dp de marge et les 2 écarts de 16dp donnent 240dp par carte — et laisse le nombre de colonnes
 * suivre la fenêtre. Une tablette affiche donc exactement ce qu'elle affichait, et un grand écran
 * montre plus de cartes **de la même taille** plutôt que les mêmes cartes étirées.
 *
 * Les marges reprennent les valeurs tablette d'Android : 24dp horizontalement, 16dp d'écart dans
 * les deux directions, 24dp sous la grille (et non les 100dp du téléphone, qui ne servent qu'à
 * dégager la barre de navigation basse — absente ici).
 *
 * État entièrement remonté, comme tous les composables de ce module : la grille ne connaît ni base
 * de données ni ViewModel, elle reçoit la liste, les quantités du panier et les rappels.
 */
@Composable
fun AccueilEcran(
    produits: List<Product>,
    nomEpicerie: String,
    recherche: String,
    onRechercheChange: (String) -> Unit,
    quantitesPanier: Map<Int, Double>,
    onAjouter: (Product) -> Unit,
    onRetirer: (Product) -> Unit,
    modeBoutique: ShopMode,
    langue: String,
    couleurTheme: Color,
    modifier: Modifier = Modifier,
    /** Voir `EnteteAccueil` : donne le focus au champ de recherche pour un lecteur de code-barres. */
    focusRecherche: FocusRequester? = null
) {
    // Mémorisé sur ses entrées : le filtrage traverse tout le catalogue, le refaire à chaque
    // recomposition sans rapport serait gratuit.
    val visibles = remember(produits, recherche) {
        CatalogueFiltre.filtrer(produits = produits, recherche = recherche)
    }

    // Le champ de recherche prend le focus à l'ouverture de l'écran. Ce n'est pas un confort :
    // sur un PC, un lecteur de code-barres USB TAPE le code puis valide, exactement comme un
    // clavier. Sans champ déjà actif, le vendeur scanne et rien ne se passe — alors que sur
    // téléphone le scan passe par la caméra et ne demande aucun focus.
    // `runCatching` : `requestFocus()` lève si le champ n'est pas encore attaché.
    LaunchedEffect(Unit) { runCatching { focusRecherche?.requestFocus() } }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        EnteteAccueil(
            nomEpicerie = nomEpicerie,
            couleurTheme = couleurTheme,
            recherche = recherche,
            onRechercheChange = onRechercheChange,
            titreSection = when (langue) {
                // Le malgache est EXACTEMENT la chaîne d'Android ; les deux autres langues sont
                // ajoutées parce qu'AGENTS.md section 3 l'exige, là où l'écran Android laisse cette
                // ligne en malgache quelle que soit la langue choisie.
                "mg" -> "Entana farany nampidirina"
                "fr" -> "Produits récemment ajoutés"
                else -> "Recently added products"
            },
            libelleVoirTout = when (langue) {
                "mg" -> "Izy rehetra"
                "fr" -> "Voir tout"
                else -> "See all"
            },
            // Sur Android ce lien ouvre l'écran Stock, qui n'est pas encore porté. En attendant, il
            // fait la chose la plus proche de son libellé : effacer la recherche pour tout montrer.
            onVoirTout = { onRechercheChange("") },
            focusRequester = focusRecherche
        )

        if (visibles.isEmpty()) {
            Text(
                text = when (langue) {
                    "mg" -> "Tsy misy entana mifanaraka amin'ny fikarohana."
                    "fr" -> "Aucun produit ne correspond à cette recherche."
                    else -> "No product matches this search."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            )
        } else {
            // L'ASCENSEUR VISIBLE, second réflexe de bureau absent du mobile.
            //
            // Sur un téléphone, on sait qu'une liste défile parce qu'on la touche : l'indicateur
            // n'apparaît que pendant le geste, et c'est la bonne convention là-bas. Sur un PC, rien
            // ne dit qu'il reste du contenu plus bas avant d'avoir tenté la molette — et un gérant
            // qui ne voit pas d'ascenseur conclut que son catalogue s'arrête là. C'est la même
            // famille de défaut que le `LIMIT 100` de la section 42 : des produits invisibles sans
            // le moindre message.
            //
            // L'ascenseur est à DROITE de la grille, pas par-dessus : il ne doit pas recouvrir la
            // carte de la dernière colonne, que la souris doit pouvoir atteindre.
            val etatGrille = rememberLazyGridState()
            Row(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = etatGrille,
                columns = GridCells.Adaptive(minSize = 240.dp),
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Clé stable sur l'identifiant, comme sur Android : sans elle Compose recycle les
                // emplacements au lieu de suivre les éléments, et toute mutation recompose des
                // cartes qui n'ont pas changé.
                items(visibles, key = { it.id }) { produit ->
                    CarteProduitGrille(
                        produit = produit,
                        quantitePanier = quantitesPanier[produit.id],
                        modeBoutique = modeBoutique,
                        langue = langue,
                        couleurTheme = couleurTheme,
                        onClick = { onAjouter(produit) },
                        onDecrement = { onRetirer(produit) }
                    )
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(etatGrille),
                modifier = Modifier.fillMaxHeight().padding(start = 8.dp)
            )
            }
        }
    }
}
