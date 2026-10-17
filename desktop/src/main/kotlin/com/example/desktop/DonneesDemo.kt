package com.example.desktop

import com.example.data.model.Product

/**
 * Un petit catalogue de démonstration, et il est **volontairement dans `:desktop`** et non dans le
 * module d'interface.
 *
 * La version de bureau n'a pas encore de base de données : partager le dépôt et les DAO exigerait
 * Room KMP, c'est-à-dire toucher au fichier de base de chaque boutique en service, ce qui est
 * délibérément remis à plus tard. Il faut donc bien des données pour que l'écran montre quelque
 * chose. Les ranger ici garde `:core:ui` propre — un module d'interface qui embarque de fausses
 * données finit toujours par en livrer en production.
 *
 * Le contenu est choisi pour exercer les règles partagées, pas pour faire joli : un article en
 * stock faible (badge d'alerte), un article avec prix de gros (mode grossiste), deux médicaments
 * avec DCI et dosage (recherche par principe actif, mode pharmacie), un modèle pré-enregistré
 * (`isTemplate`) qui doit rester invisible, et des noms de casses variées pour que le tri se voie.
 */
object DonneesDemo {

    val catalogue: List<Product> = listOf(
        Product(
            id = 1,
            name = "Riz Makalioka 1 kg",
            price = 3500.0,
            category = "Alimentation",
            stock = 42.0,
            unit = "Kilogramme",
            barcode = "6001234500011",
            wholesalePrice = 3100.0
        ),
        Product(
            id = 2,
            name = "huile de tournesol 1 L",
            price = 9800.0,
            category = "Alimentation",
            stock = 3.0,
            lowStockThreshold = 6.0,
            unit = "Litre",
            barcode = "6001234500028"
        ),
        Product(
            id = 3,
            name = "Eau minérale 1,5 L",
            price = 2000.0,
            category = "Boisson",
            stock = 120.0,
            unit = "Pièce",
            barcode = "6001234500035",
            wholesalePrice = 1700.0
        ),
        Product(
            id = 4,
            name = "Savon de Marseille",
            price = 1500.0,
            category = "Droguerie",
            stock = 18.0,
            unit = "Pièce",
            barcode = "6001234500042"
        ),
        Product(
            id = 5,
            name = "Doliprane 500 mg",
            price = 4500.0,
            category = "Pharmacie",
            stock = 25.0,
            unit = "Plaquette",
            barcode = "3400930000011",
            dci = "paracétamol",
            dosage = "500 mg",
            formeGalenique = "Comprimé"
        ),
        Product(
            id = 6,
            name = "Efferalgan 1 g",
            price = 7200.0,
            category = "Pharmacie",
            stock = 4.0,
            lowStockThreshold = 10.0,
            unit = "Plaquette",
            barcode = "3400930000028",
            dci = "paracétamol",
            dosage = "1 g",
            formeGalenique = "Comprimé effervescent",
            surOrdonnance = false
        ),
        Product(
            id = 7,
            name = "Zébu séché 500 g",
            price = 18000.0,
            category = "Alimentation",
            stock = 7.0,
            unit = "Paquet",
            barcode = "6001234500059"
        ),
        Product(
            id = 8,
            name = "ananas frais",
            price = 2500.0,
            category = "Légumes",
            stock = 15.0,
            unit = "Pièce"
        ),
        // Ce modèle pré-enregistré ne doit JAMAIS apparaître dans la liste : c'est exactement ce
        // que `CatalogueFiltre` écarte, et le voir à l'écran signalerait une régression.
        Product(
            id = 9,
            name = "MODÈLE — Boisson gazeuse 33 cl",
            price = 0.0,
            category = "Boisson",
            stock = 0.0,
            isTemplate = true
        )
    )
}
