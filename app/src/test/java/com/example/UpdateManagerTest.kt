package com.example

import com.example.util.UpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
