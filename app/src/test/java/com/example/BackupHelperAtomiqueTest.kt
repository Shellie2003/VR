package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.BackupHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * La sauvegarde de sécurité locale est la dernière défense avant le cloud : c'est elle que le
 * démarrage rejoue pour remonter les données après une migration destructive ou une réinstallation
 * (sections 41 et 45). Elle était écrite par `writeText`, qui TRONQUE le fichier avant d'écrire —
 * et `CrashReporter` (section 47) l'appelle depuis le gestionnaire d'exception fatale, donc
 * exactement quand le processus est en train de mourir. Un fichier coupé dépasse les 20 octets de
 * `hasBackup`, donc l'application croyait avoir un filet inexploitable.
 *
 * Ces tests ne peuvent pas tuer un processus en plein milieu d'une écriture. Ils verrouillent ce
 * qui est observable, et qui est précisément ce dont dépend l'atomicité : l'écriture passe par un
 * fichier temporaire renommé, et ce temporaire ne survit jamais à l'opération.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupHelperAtomiqueTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun cible() = File(context.filesDir, "database_safety_backup.json")
    private fun temporaire() = File(context.filesDir, "database_safety_backup.json.tmp")

    private fun json(n: Int) = """{"products":[],"sales":[],"marqueur":$n}"""

    @Test
    fun `la sauvegarde ecrite est relue a l'identique`() {
        BackupHelper.saveBackup(context, json(1))

        assertEquals(json(1), BackupHelper.readBackup(context))
        assertTrue(BackupHelper.hasBackup(context))
    }

    @Test
    fun `aucun fichier temporaire ne survit a une sauvegarde reussie`() {
        // Sans cette garantie, un appareil où le renommage échoue accumulerait une seconde copie
        // complète de la sauvegarde à chaque écriture — et elles sont fréquentes (section 40).
        BackupHelper.saveBackup(context, json(1))

        assertFalse("Le fichier temporaire doit avoir été renommé, pas laissé en place", temporaire().exists())
        assertTrue(cible().isFile)
    }

    @Test
    fun `un temporaire orphelin d'une ecriture interrompue n'est jamais relu comme la sauvegarde`() {
        // C'est l'état exact que laisse un processus tué en pleine écriture : un temporaire tronqué
        // à côté de la sauvegarde précédente, qui reste la seule à faire foi.
        BackupHelper.saveBackup(context, json(1))
        temporaire().writeText("""{"products":[{"name":"coup""")

        assertEquals(json(1), BackupHelper.readBackup(context))

        // Et l'écriture suivante écrase ce résidu au lieu d'échouer dessus.
        BackupHelper.saveBackup(context, json(2))
        assertEquals(json(2), BackupHelper.readBackup(context))
        assertFalse(temporaire().exists())
    }

    @Test
    fun `une sauvegarde plus courte ne laisse aucun reste de la precedente`() {
        // Le renommage remplace le fichier entier. Une écriture en place aurait laissé la queue de
        // l'ancien JSON collée à la fin du nouveau, donc un fichier syntaxiquement invalide.
        BackupHelper.saveBackup(context, """{"products":[],"sales":[],"remplissage":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}""")
        BackupHelper.saveBackup(context, json(3))

        assertEquals(json(3), BackupHelper.readBackup(context))
    }

    @Test
    fun `sans aucune sauvegarde la lecture ne pretend rien`() {
        cible().delete()
        temporaire().delete()

        assertEquals(null, BackupHelper.readBackup(context))
        assertFalse(BackupHelper.hasBackup(context))
    }
}
