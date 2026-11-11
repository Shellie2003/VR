package com.example.desktop

import com.example.desktop.export.ExportFichier
import com.example.desktop.export.ResultatExport
import com.example.util.Tableau
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Seules les décisions **sans écriture** sont couvertes ici, et c'est délibéré : un test qui
 * écrirait réellement dans `Documents/Varotra` déposerait des fichiers dans le dossier personnel
 * de la machine de CI — et, exécuté sur le poste d'un développeur, dans le vrai dossier d'export
 * du gérant. Un test ne doit pas laisser de trace là où l'utilisateur rangera ses données.
 *
 * Le refus d'une liste vide est justement un chemin qui retourne **avant** de toucher au disque,
 * donc vérifiable sans effet de bord — et c'est le garde-fou qui compte, puisque c'est lui qui
 * empêche d'annoncer un fichier qui ne contiendrait que ses en-têtes.
 */
class ExportFichierTest {

    private fun tableau(lignes: List<List<String>>) = Tableau(
        nomFichier = "test_export",
        titre = "Test",
        entetes = listOf("A", "B"),
        lignes = lignes,
        poidsColonnes = listOf(1f, 1f)
    )

    @Test
    fun `une liste vide est refusee sans ecrire aucun fichier`() {
        val avant = ExportFichier.dossier().let { if (it.isDirectory) it.list()?.size ?: 0 else -1 }
        assertEquals(ResultatExport.ListeVide, ExportFichier.ecrireCsv(tableau(emptyList())))
        val apres = ExportFichier.dossier().let { if (it.isDirectory) it.list()?.size ?: 0 else -1 }
        // Ni fichier déposé, ni dossier créé au passage : le refus intervient avant tout accès
        // au disque, y compris le `mkdirs()`.
        assertEquals(avant, apres)
    }

    @Test
    fun `le dossier d export est un sous-dossier nomme Varotra`() {
        // C'est le repère que le gérant retient une fois pour toutes, et qu'annonce le dialogue
        // après chaque export. Le renommer rendrait introuvables les fichiers déjà exportés.
        assertEquals("Varotra", ExportFichier.dossier().name)
    }

    @Test
    fun `nommer le dossier ne le cree pas`() {
        // `dossier()` est appelée pour construire un chemin et pour l'afficher — y compris sur le
        // chemin d'échec. Si elle créait le dossier, un export refusé laisserait derrière lui un
        // dossier vide dans Documents, que le gérant prendrait pour ses données perdues.
        val cible = ExportFichier.dossier()
        val existaitAvant = cible.isDirectory
        ExportFichier.dossier()
        if (!existaitAvant) assertFalse(cible.isDirectory)
        assertTrue(cible.path.isNotEmpty())
    }
}
