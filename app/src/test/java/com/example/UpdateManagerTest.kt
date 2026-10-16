package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.util.AppPreferences
import com.example.util.UpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * `org.json.JSONObject` n'a pas d'implémentation réelle dans le android.jar utilisé par les tests
 * JVM purs (il faut Robolectric pour ça, comme pour Migration22To23Test) — d'où le RunWith même si
 * ces fonctions n'utilisent par ailleurs aucune API Android.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpdateManagerTest {

    private val manifesteValide = """
        {"versionCode": 42, "versionName": "1.0.42", "apkUrl": "https://pub-test.r2.dev/latest/varotra.apk", "sha256": "abc123", "notesVersion": "Corrections diverses"}
    """.trimIndent()

    @Test
    fun `version distante plus recente -- mise a jour proposee`() {
        val info = UpdateManager.parserManifesteMiseAJour(manifesteValide, versionCodeActuel = 10)
        assertEquals(42, info?.versionCode)
        assertEquals("1.0.42", info?.versionName)
        assertEquals("https://pub-test.r2.dev/latest/varotra.apk", info?.apkUrl)
        assertEquals("abc123", info?.sha256)
        assertEquals("Corrections diverses", info?.notesVersion)
    }

    @Test
    fun `version distante egale -- deja a jour, rien a proposer`() {
        assertNull(UpdateManager.parserManifesteMiseAJour(manifesteValide, versionCodeActuel = 42))
    }

    @Test
    fun `version distante plus ancienne -- rien a proposer`() {
        assertNull(UpdateManager.parserManifesteMiseAJour(manifesteValide, versionCodeActuel = 100))
    }

    @Test
    fun `apkUrl manquant -- manifeste inutilisable, rien a proposer`() {
        val manifesteSansUrl = """{"versionCode": 99, "versionName": "1.0.99"}"""
        assertNull(UpdateManager.parserManifesteMiseAJour(manifesteSansUrl, versionCodeActuel = 1))
    }

    @Test
    fun `json corrompu -- rien a proposer plutot qu'un crash`() {
        assertNull(UpdateManager.parserManifesteMiseAJour("pas du json", versionCodeActuel = 1))
        assertNull(UpdateManager.parserManifesteMiseAJour("", versionCodeActuel = 1))
    }

    @Test
    fun `sha256 calcule correspond a l'empreinte connue d'un contenu fixe`() {
        val fichier = File.createTempFile("update_manager_test", ".txt")
        try {
            fichier.writeText("varotra")
            // sha256("varotra") calculé indépendamment (openssl / python hashlib) : valeur fixe,
            // sert de non-régression sur l'algorithme et l'encodage hexadécimal utilisés.
            assertEquals(
                "9a3dbd2d63f2ac87db3d440850969472baaa7f02bbd199817bca3b9c77b4153b",
                UpdateManager.calculerSha256(fichier)
            )
        } finally {
            fichier.delete()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Pastille « nouvelle version » : la règle qui décide si le gérant apprend un jour qu'une
    // version existe. Une erreur ici ne planterait rien et ne se verrait nulle part — elle
    // laisserait simplement une épicerie sur une version ancienne indéfiniment.
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `une version serveur plus recente allume la pastille`() {
        assertTrue(UpdateManager.badgeMiseAJourVisible(versionConnue = 118, versionInstallee = 116))
    }

    @Test
    fun `la pastille s'eteint d'elle-meme une fois la mise a jour installee`() {
        // Propriété décisive : aucun code de nettoyage n'est nécessaire. Après installation, le
        // versionCode installé rattrape puis dépasse la valeur mémorisée, et la pastille disparaît.
        assertFalse(UpdateManager.badgeMiseAJourVisible(versionConnue = 118, versionInstallee = 118))
        assertFalse(UpdateManager.badgeMiseAJourVisible(versionConnue = 118, versionInstallee = 120))
    }

    @Test
    fun `aucune verification aboutie -- aucune pastille`() {
        // 0 est la valeur par défaut de la préférence : tant qu'aucune vérification n'a réussi,
        // rien ne doit être annoncé. Afficher une pastille « au cas où » apprendrait au gérant à
        // l'ignorer, et donc à ignorer la vraie.
        assertFalse(UpdateManager.badgeMiseAJourVisible(versionConnue = 0, versionInstallee = 116))
    }

    @Test
    fun `une valeur memorisee plus ancienne que l'installee n'allume rien`() {
        // Cas d'un retour à une version antérieure, ou d'une préférence restaurée depuis une
        // sauvegarde ancienne : la comparaison stricte suffit à l'absorber.
        assertFalse(UpdateManager.badgeMiseAJourVisible(versionConnue = 100, versionInstallee = 116))
    }

    /**
     * Le vrai point faible d'une pastille calculée au démarrage : survivre au redémarrage suivant.
     *
     * La vérification n'a lieu qu'une fois par jour civil et seulement en ligne. Si l'état n'était
     * gardé qu'en mémoire, le gérant verrait l'avertissement une fois, puis plus rien — relance de
     * l'application, vérification du jour déjà faite, pastille éteinte. Et un épicier relance son
     * application des dizaines de fois par jour, souvent hors ligne.
     *
     * Ce test reproduit exactement ce redémarrage : il écrit la version trouvée, puis construit une
     * NOUVELLE instance d'AppPreferences sur le même contexte, comme le ferait un nouveau processus.
     */
    @Test
    fun `la version trouvee survit au redemarrage de l'application`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        AppPreferences(context).derniereVersionDisponible = 118

        val apresRedemarrage = AppPreferences(context)
        assertEquals(118, apresRedemarrage.derniereVersionDisponible)
        assertTrue(
            UpdateManager.badgeMiseAJourVisible(
                versionConnue = apresRedemarrage.derniereVersionDisponible,
                versionInstallee = 116
            )
        )
    }
}
