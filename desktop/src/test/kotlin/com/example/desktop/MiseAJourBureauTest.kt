package com.example.desktop

import com.example.desktop.maj.MiseAJourBureau
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le remplacement d'une application par elle-même sous Windows ne peut être exécuté ni ici (pas de
 * Windows) ni en CI (Linux, sans interface). Ces cas couvrent donc **tout ce qui peut l'être par un
 * calcul** — et ce n'est pas un pis-aller : une erreur dans l'une de ces règles ne provoquerait
 * aucune exception. Elle laisserait une boutique sur une version ancienne pour toujours, ou pire,
 * détruirait une installation en déplaçant le mauvais dossier.
 */
class MiseAJourBureauTest {

    // --- Version installée -----------------------------------------------------------------

    @Test
    fun `le numero de version comparable est le troisieme segment`() {
        assertEquals(150, MiseAJourBureau.versionCodeDepuisNom("1.0.150"))
        assertEquals(7, MiseAJourBureau.versionCodeDepuisNom("1.0.7"))
    }

    @Test
    fun `un quatrieme segment ne fait pas passer une version recente pour ancienne`() {
        // Le piège exact que vise le choix du troisième segment plutôt que du dernier : avec le
        // dernier, `1.0.150.2` vaudrait 2, donc une version plus récente paraîtrait plus ancienne —
        // et le logiciel refuserait pour toujours une mise à jour qui existe.
        assertEquals(150, MiseAJourBureau.versionCodeDepuisNom("1.0.150.2"))
    }

    @Test
    fun `une version absente ou illisible ne donne aucun numero`() {
        // L'absence est le cas NORMAL d'un lancement depuis l'IDE. Renvoyer 0 ferait croire à une
        // installation infiniment en retard, donc proposerait une mise à jour par-dessus un arbre
        // de développement.
        assertNull(MiseAJourBureau.versionCodeDepuisNom(null))
        assertNull(MiseAJourBureau.versionCodeDepuisNom(""))
        assertNull(MiseAJourBureau.versionCodeDepuisNom("   "))
        assertNull(MiseAJourBureau.versionCodeDepuisNom("1.0"))
        assertNull(MiseAJourBureau.versionCodeDepuisNom("1.0.x"))
        assertNull(MiseAJourBureau.versionCodeDepuisNom("instantané"))
    }

    @Test
    fun `un troisieme segment nul est refuse`() {
        // `1.0.0` ne peut pas être une version réelle : le numéro vient du nombre de commits, qui
        // vaut au moins 1. Le voir signifie que le calcul a échoué (clone superficiel en CI,
        // leçon de release.yml), et le prendre pour argent comptant proposerait une mise à jour à
        // chaque démarrage.
        assertNull(MiseAJourBureau.versionCodeDepuisNom("1.0.0"))
    }

    // --- Manifeste distant -----------------------------------------------------------------

    @Test
    fun `une version plus recente est proposee avec tous ses champs`() {
        val info = MiseAJourBureau.parserManifeste(
            """{"versionCode": 151, "versionName": "1.0.151",
               "archiveUrl": "https://exemple/varotra-bureau.zip",
               "sha256": "abc123", "notes": "Corrige la caisse"}""",
            versionCodeActuel = 150
        )
        assertNotNull(info)
        assertEquals(151, info!!.versionCode)
        assertEquals("1.0.151", info.versionName)
        assertEquals("https://exemple/varotra-bureau.zip", info.archiveUrl)
        assertEquals("abc123", info.sha256)
        assertEquals("Corrige la caisse", info.notes)
    }

    @Test
    fun `une version identique ou plus ancienne ne propose rien`() {
        val identique = """{"versionCode": 150, "archiveUrl": "https://exemple/a.zip"}"""
        val plusAncienne = """{"versionCode": 149, "archiveUrl": "https://exemple/a.zip"}"""
        assertNull(MiseAJourBureau.parserManifeste(identique, 150))
        assertNull(MiseAJourBureau.parserManifeste(plusAncienne, 150))
    }

    @Test
    fun `un manifeste sans adresse d'archive ne propose rien`() {
        // Annoncer une mise à jour dont le fichier est introuvable est pire que de ne rien
        // annoncer : le gérant clique, échoue, et recommence.
        assertNull(
            MiseAJourBureau.parserManifeste("""{"versionCode": 200, "archiveUrl": ""}""", 150)
        )
        assertNull(MiseAJourBureau.parserManifeste("""{"versionCode": 200}""", 150))
    }

    @Test
    fun `un manifeste illisible ne leve pas d'exception`() {
        // Un objet R2 tronqué, une page d'erreur HTML servie à la place du JSON : rien de tout cela
        // ne doit faire tomber le logiciel au moment où le gérant clique sur « Vérifier ».
        assertNull(MiseAJourBureau.parserManifeste("pas du json", 150))
        assertNull(MiseAJourBureau.parserManifeste("", 150))
        assertNull(MiseAJourBureau.parserManifeste("<html>404</html>", 150))
    }

    // --- Dossier d'installation ------------------------------------------------------------

    @Test
    fun `la racine est deux niveaux au-dessus du jar`() {
        val racine = MiseAJourBureau.racineInstallation(
            File("/opt/Varotra/app/varotra-bureau.jar")
        )
        assertEquals(File("/opt/Varotra"), racine)
    }

    @Test
    fun `hors d'un paquet aucune racine n'est proposee`() {
        // LE cas qui compte. Lancé par Gradle, notre code vit dans `build/classes/kotlin/main` :
        // remonter de deux niveaux désignerait `build/classes`, un dossier qu'on s'apprêterait à
        // remplacer par une application entière. Le contrôle du nom `app` est la seule chose qui
        // l'empêche.
        assertNull(
            MiseAJourBureau.racineInstallation(File("/projet/desktop/build/classes/kotlin/main"))
        )
        assertNull(MiseAJourBureau.racineInstallation(File("/quelque/part/autre/x.jar")))
        assertNull(MiseAJourBureau.racineInstallation(null))
        // Un jar posé à la racine du système : son parent s'appelle "", pas "app".
        assertNull(MiseAJourBureau.racineInstallation(File("/x.jar")))
    }

    @Test
    fun `le dossier app est reconnu quelle que soit sa casse`() {
        assertEquals(
            File("/opt/Varotra"),
            MiseAJourBureau.racineInstallation(File("/opt/Varotra/App/varotra.jar"))
        )
    }

    // --- Détection du système --------------------------------------------------------------

    @Test
    fun `seul Windows est reconnu comme automatisable`() {
        assertTrue(MiseAJourBureau.estWindows("Windows 11"))
        assertTrue(MiseAJourBureau.estWindows("windows 10"))
        assertTrue(!MiseAJourBureau.estWindows("Linux"))
        assertTrue(!MiseAJourBureau.estWindows("Mac OS X"))
        assertTrue(!MiseAJourBureau.estWindows(null))
    }

    // --- Script de remplacement ------------------------------------------------------------
    //
    // C'est le test le plus important du fichier : ce script manipule le dossier d'installation
    // réel du gérant, et aucune exécution ne peut le vérifier ici.

    private fun script(
        racine: String = "C:\\Program Files\\Varotra",
        nouveau: String = "C:\\Program Files\\Varotra-nouvelle-version"
    ) = MiseAJourBureau.scriptRemplacement(
        pid = 4242,
        racine = File(racine),
        nouveau = File(nouveau),
        nomLanceur = "Varotra.exe"
    )

    @Test
    fun `le script attend la fermeture du logiciel avant de toucher au dossier`() {
        val texte = script()
        val attente = texte.indexOf("tasklist /FI \"PID eq 4242\"")
        val premierMove = texte.indexOf("move \"%CIBLE%\"")
        assertTrue("L'attente du processus doit être présente", attente >= 0)
        assertTrue("Le déplacement doit être présent", premierMove >= 0)
        assertTrue(
            "Déplacer le dossier avant la fermeture du logiciel échouerait sous Windows",
            attente < premierMove
        )
    }

    @Test
    fun `le script relance le logiciel avant de supprimer l'ancienne version`() {
        val texte = script()
        val relance = texte.indexOf("start \"\" \"%CIBLE%\\%LANCEUR%\"")
        val suppression = texte.indexOf("rmdir /s /q \"%ANCIEN%\"\r\nexit /b 0")
        assertTrue(relance >= 0)
        assertTrue(suppression >= 0)
        assertTrue(
            "Supprimer l'ancienne version avant d'avoir relancé la nouvelle laisserait le " +
                "gérant sans aucun logiciel si le lancement échoue",
            relance < suppression
        )
    }

    @Test
    fun `chaque echec relance un logiciel en etat de marche`() {
        val texte = script()
        // Trois sorties d'échec, et aucune ne doit laisser le gérant sans logiciel : le dossier
        // intact est relancé, ou l'ancien est remis en place puis relancé.
        assertTrue("Sortie quand rien n'a bougé", texte.contains(":echec_intact"))
        assertTrue("Sortie quand la nouvelle version n'a pas pu être posée", texte.contains(":restaurer"))
        val restaurer = texte.indexOf(":restaurer")
        val remiseEnPlace = texte.indexOf("move \"%ANCIEN%\" \"%CIBLE%\"")
        assertTrue(
            "La branche de restauration doit remettre l'ancienne version en place",
            restaurer in 0 until remiseEnPlace
        )
        // Abandon pur : le logiciel ne s'est pas fermé, donc on ne touche à RIEN.
        val abandon = texte.indexOf(":abandon")
        assertTrue(abandon >= 0)
        assertTrue(
            "Après l'abandon le script ne doit plus rien déplacer",
            !texte.substring(abandon).contains("move ")
        )
    }

    @Test
    fun `les chemins sont proteges par des guillemets`() {
        // Une installation sous « C:\Program Files\Varotra » est le cas le plus courant, et un
        // chemin non protégé y couperait la commande au premier espace — donc déplacerait
        // « C:\Program » quelque part.
        val texte = script()
        assertTrue(texte.contains("set \"CIBLE=C:\\Program Files\\Varotra\""))
        assertTrue(texte.contains("set \"NOUVEAU=C:\\Program Files\\Varotra-nouvelle-version\""))
        assertTrue(texte.contains("set \"ANCIEN=C:\\Program Files\\Varotra.ancien\""))
        // Aucune occurrence du chemin brut hors d'une affectation protégée.
        val lignesNues = texte.lines().filter {
            it.contains("C:\\Program Files") && !it.trimStart().startsWith("set \"")
        }
        assertTrue("Chemin non protégé : $lignesNues", lignesNues.isEmpty())
    }

    @Test
    fun `l'attente utilise ping et non timeout`() {
        // `timeout` exige une console interactive et échoue (« input redirection is not
        // supported ») dans un processus lancé sans fenêtre : la boucle d'attente tournerait alors
        // à pleine vitesse et abandonnerait en quelques millisecondes.
        val texte = script()
        assertTrue(texte.contains("ping -n 2 127.0.0.1 >nul"))
        assertTrue("timeout ne fonctionne pas sans console", !texte.contains("timeout /t"))
    }

    @Test
    fun `le dossier de l'ancienne version derive du dossier cible`() {
        // S'ils divergeaient, le script supprimerait un dossier qui n'est pas celui qu'il a mis de
        // côté — autrement dit un dossier appartenant à quelqu'un d'autre.
        val texte = script(racine = "D:\\Caisse\\Varotra")
        assertTrue(
            texte.contains("set \"ANCIEN=D:\\Caisse\\Varotra${MiseAJourBureau.SUFFIXE_ANCIEN}\"")
        )
    }
}
