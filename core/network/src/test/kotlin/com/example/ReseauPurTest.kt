package com.example

import com.example.util.FirebaseBackupManager
import com.example.util.OpenFoodFactsApi
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Les deux seules fonctions **pures** des clients REST — et elles n'avaient jusqu'ici aucun test,
 * alors que chacune décide quelque chose de conséquent.
 *
 * `resolveDatabaseUrl` choisit **où partent les sauvegardes d'une boutique**. Une erreur ici
 * n'afficherait rien d'anormal : les sauvegardes partiraient simplement au mauvais endroit, et on
 * ne le découvrirait qu'au moment d'en avoir besoin — c'est-à-dire trop tard.
 *
 * `formatQueryForOff` décide ce qui est réellement envoyé au catalogue mondial. Une régression se
 * traduirait par des recherches qui ne trouvent plus rien, sans message d'erreur.
 *
 * Le reste de ces deux objets est du transport HTTP, non rejoué en CI — précédent établi dans ce
 * projet pour `LicenceManager` et `UpdateManager` : seule la logique pure est testée.
 */
class ReseauPurTest {

    // --- Où partent les sauvegardes -------------------------------------------------------------

    @Test
    fun `sans url personnelle, la sauvegarde part vers la base du fournisseur`() {
        // C'est le cas de l'immense majorité des boutiques : le gérant ne crée aucun projet
        // Firebase, et la sauvegarde cloud doit fonctionner dès l'installation.
        assertEquals(
            FirebaseBackupManager.CENTRAL_BACKUP_DATABASE_URL,
            FirebaseBackupManager.resolveDatabaseUrl("")
        )
    }

    @Test
    fun `une url faite d'espaces est traitee comme absente, pas comme une adresse`() {
        // Un champ « effacé » laisse souvent un espace. L'envoyer tel quel comme adresse ferait
        // échouer chaque sauvegarde en silence.
        assertEquals(
            FirebaseBackupManager.CENTRAL_BACKUP_DATABASE_URL,
            FirebaseBackupManager.resolveDatabaseUrl("   ")
        )
    }

    @Test
    fun `l'url personnelle du gerant a la priorite`() {
        // Le gérant qui veut héberger ses données lui-même doit être respecté : sa base l'emporte.
        assertEquals(
            "https://chez-moi.firebaseio.com/",
            FirebaseBackupManager.resolveDatabaseUrl("https://chez-moi.firebaseio.com/")
        )
    }

    @Test
    fun `les espaces autour d'une url saisie sont retires`() {
        assertEquals(
            "https://chez-moi.firebaseio.com/",
            FirebaseBackupManager.resolveDatabaseUrl("  https://chez-moi.firebaseio.com/  ")
        )
    }

    // --- Ce qui est réellement envoyé au catalogue mondial ---------------------------------------

    @Test
    fun `un nom colle est separe en mots pour que la recherche aboutisse`() {
        // Le gérant tape ce qu'il lit sur l'emballage, sans espace. Le catalogue, lui, indexe des
        // mots séparés.
        assertEquals("Coca Cola", OpenFoodFactsApi.formatQueryForOff("CocaCola"))
    }

    @Test
    fun `la marque collee est reconnue quelle que soit la casse`() {
        assertEquals("coca cola", OpenFoodFactsApi.formatQueryForOff("cocacola"))
        assertEquals("coca cola", OpenFoodFactsApi.formatQueryForOff("COCACOLA"))
    }

    @Test
    fun `une saisie normale n'est pas abimee`() {
        // Le cas le plus courant de loin : il ne doit surtout rien arriver au texte saisi.
        assertEquals("riz", OpenFoodFactsApi.formatQueryForOff("riz"))
        assertEquals("huile de tournesol", OpenFoodFactsApi.formatQueryForOff("huile de tournesol"))
    }

    @Test
    fun `les espaces autour de la recherche sont retires`() {
        assertEquals("sucre", OpenFoodFactsApi.formatQueryForOff("  sucre  "))
    }
}
