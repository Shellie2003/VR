package com.example.data.model

/**
 * Un lot de péremption tel qu'une **sauvegarde** le porte, et non tel que Room le stocke.
 *
 * ## Pourquoi un modèle de plus, et pas [LotProduit]
 *
 * [LotProduit] référence le produit par `produitId`, un identifiant **local** à un appareil. La
 * sauvegarde ne peut donc pas le transporter : depuis la section 29, elle identifie chaque lot par
 * le code-barres ou le nom de son produit, précisément parce qu'un id local désignerait un autre
 * article sur un autre poste. Un lecteur de sauvegarde n'a donc pas de `produitId` à mettre, et
 * en inventer un serait pire que de ne pas en avoir.
 *
 * Ce modèle porte ce que la sauvegarde contient réellement. Le poste qui le lit résout le produit
 * s'il le peut ; s'il ne le peut pas (article supprimé depuis), le lot reste affichable sous le nom
 * que la sauvegarde a conservé, ce qui est exactement ce dont le gérant a besoin pour aller
 * chercher la marchandise en rayon.
 */
data class LotLisible(
    val produitNom: String,
    val produitBarcode: String = "",
    val numeroLot: String? = null,
    val quantite: Double,
    val datePeremption: Long,
    val dateReception: Long = 0L,
    /**
     * L'unité du produit, reprise du catalogue à la lecture quand le produit a pu être retrouvé.
     * Vide sinon : mieux vaut une quantité sans unité qu'une unité inventée.
     */
    val unite: String = ""
)
