package com.example

import com.example.util.VarotraPalette
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Un test qui recopie des constantes n'a de valeur que pendant une extraction — et c'est
 * exactement le moment. Ces couleurs sont celles que les boutiques ont sous les yeux ; elles
 * viennent d'être déplacées hors de `:app` pour que la version de bureau lise la même source, et
 * le seul risque réel de ce déplacement est la faute de frappe sur un chiffre hexadécimal, qu'une
 * relecture ne voit pas et qu'aucun test d'interface ne rattraperait.
 *
 * Seules les valeurs identitaires sont épinglées : le vert de la marque et les fonds, c'est-à-dire
 * ce dont un écart se remarquerait immédiatement. Épingler les 44 valeurs ne ferait que recopier
 * le fichier.
 */
class VarotraPaletteTest {

    @Test
    fun `le vert de la marque est inchange dans les deux modes`() {
        assertEquals(0xFF012D1DL, VarotraPalette.LIGHT_PRIMARY)
        assertEquals(0xFFC1ECD4L, VarotraPalette.DARK_PRIMARY)
    }

    @Test
    fun `les fonds sont inchanges dans les deux modes`() {
        assertEquals(0xFFFBF9F8L, VarotraPalette.LIGHT_BACKGROUND)
        assertEquals(0xFF002114L, VarotraPalette.DARK_BACKGROUND)
    }

    @Test
    fun `chaque couleur est entierement opaque`() {
        // Une valeur saisie sans son octet alpha (0x012D1D au lieu de 0xFF012D1D) produirait un
        // élément totalement transparent : non pas une couleur fausse, mais un élément invisible.
        val toutes = listOf(
            VarotraPalette.LIGHT_PRIMARY, VarotraPalette.LIGHT_ON_PRIMARY,
            VarotraPalette.LIGHT_PRIMARY_CONTAINER, VarotraPalette.LIGHT_ON_PRIMARY_CONTAINER,
            VarotraPalette.LIGHT_SECONDARY, VarotraPalette.LIGHT_ON_SECONDARY,
            VarotraPalette.LIGHT_SECONDARY_CONTAINER, VarotraPalette.LIGHT_ON_SECONDARY_CONTAINER,
            VarotraPalette.LIGHT_BACKGROUND, VarotraPalette.LIGHT_ON_BACKGROUND,
            VarotraPalette.LIGHT_SURFACE, VarotraPalette.LIGHT_ON_SURFACE,
            VarotraPalette.LIGHT_SURFACE_VARIANT, VarotraPalette.LIGHT_ON_SURFACE_VARIANT,
            VarotraPalette.LIGHT_OUTLINE, VarotraPalette.LIGHT_OUTLINE_VARIANT,
            VarotraPalette.LIGHT_ERROR, VarotraPalette.LIGHT_ON_ERROR,
            VarotraPalette.LIGHT_ERROR_CONTAINER, VarotraPalette.LIGHT_ON_ERROR_CONTAINER,
            VarotraPalette.DARK_PRIMARY, VarotraPalette.DARK_ON_PRIMARY,
            VarotraPalette.DARK_PRIMARY_CONTAINER, VarotraPalette.DARK_ON_PRIMARY_CONTAINER,
            VarotraPalette.DARK_SECONDARY, VarotraPalette.DARK_ON_SECONDARY,
            VarotraPalette.DARK_SECONDARY_CONTAINER, VarotraPalette.DARK_ON_SECONDARY_CONTAINER,
            VarotraPalette.DARK_BACKGROUND, VarotraPalette.DARK_ON_BACKGROUND,
            VarotraPalette.DARK_SURFACE, VarotraPalette.DARK_ON_SURFACE,
            VarotraPalette.DARK_SURFACE_VARIANT, VarotraPalette.DARK_ON_SURFACE_VARIANT,
            VarotraPalette.DARK_OUTLINE, VarotraPalette.DARK_OUTLINE_VARIANT,
            VarotraPalette.DARK_ERROR, VarotraPalette.DARK_ON_ERROR,
            VarotraPalette.DARK_ERROR_CONTAINER, VarotraPalette.DARK_ON_ERROR_CONTAINER
        )
        toutes.forEach { couleur ->
            assertEquals(
                "Couleur sans octet alpha : ${java.lang.Long.toHexString(couleur)}",
                0xFFL,
                (couleur shr 24) and 0xFFL
            )
        }
    }
}
