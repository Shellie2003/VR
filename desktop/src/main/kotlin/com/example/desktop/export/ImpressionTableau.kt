package com.example.desktop.export

import com.example.util.MiseEnPageTableau
import com.example.util.Tableau
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.PrinterJob
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ce qui s'est passé quand le gérant a demandé une impression. */
sealed interface ResultatImpression {
    /** Le travail est parti à l'imprimante (ou au générateur de PDF du système). */
    object Lancee : ResultatImpression

    /** Le gérant a fermé la boîte de dialogue d'impression : rien à annoncer, rien à corriger. */
    object Annulee : ResultatImpression

    /** Il n'y avait rien à imprimer. */
    object ListeVide : ResultatImpression

    /** Aucune imprimante déclarée sur la machine. */
    object AucuneImprimante : ResultatImpression

    /** L'impression a échoué, avec son motif technique. */
    data class Echec(val motif: String) : ResultatImpression
}

/**
 * L'impression d'un tableau sur l'imprimante du PC.
 *
 * ## Pourquoi imprimer, alors que l'export Excel existe déjà
 *
 * L'export CSV **suppose qu'Excel ou LibreOffice soit installé** — ce que la section 105 avait
 * noté comme limite sans la mesurer. Sur le PC nu d'une épicerie, un `.csv` s'ouvre dans le Bloc-
 * notes : colonnes collées, accents cassés, impression illisible. Or les deux usages les plus
 * concrets d'un PC en boutique sont justement du papier : la **liste de stock** qu'on emporte dans
 * les rayons pour compter, et la **liste des trosa** qu'on garde sous la main pour réclamer.
 *
 * ## Et cela donne le PDF gratuitement
 *
 * Windows installe d'office une imprimante virtuelle « Microsoft Print to PDF ». Passer par la
 * boîte de dialogue d'impression du système offre donc, sans une ligne de plus et sans
 * bibliothèque PDF, exactement ce que la section 105 déclarait hors de portée : un fichier qui
 * s'ouvre dans n'importe quel navigateur, chez le comptable comme chez le gérant.
 *
 * ## Le dessin n'est pas partageable, l'arithmétique l'est
 *
 * Android trace ses PDF sur un `Canvas`, un PC sur un `Graphics2D`, et mesurer un texte demande
 * une police concrète : ce code-ci ne peut pas être commun. Mais le calcul des largeurs de
 * colonnes et du découpage en pages ne dépend d'aucune des deux plateformes — il vit dans
 * `MiseEnPageTableau` (`:core:logic`) et il est testé, parce que c'est lui qui, en cas d'erreur,
 * sort une colonne de la feuille sans que rien ne le signale.
 */
object ImpressionTableau {

    private const val MARGE = 36f
    private const val HAUTEUR_LIGNE = 14f
    private const val HAUTEUR_ENTETE = 46f

    private val formatDateHeure = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    /**
     * Ouvre la boîte de dialogue d'impression du système pour [tableau].
     *
     * **La boîte de dialogue est volontairement affichée**, et non court-circuitée : c'est elle qui
     * laisse choisir l'imprimante — « Microsoft Print to PDF » comprise —, l'orientation et le
     * nombre de copies. L'imposer nous obligerait à redévelopper tout cela, et mal.
     *
     * Tout est enveloppé, y compris contre `LinkageError` : `java.desktop` est bien déclaré dans
     * les modules de `jpackage`, mais la section 103 a montré qu'un module manquant ne se voit que
     * sur le poste du gérant — et un bouton d'impression ne doit jamais pouvoir fermer le logiciel.
     */
    fun imprimer(tableau: Tableau): ResultatImpression {
        if (tableau.lignes.isEmpty()) return ResultatImpression.ListeVide
        return try {
            if (PrinterJob.lookupPrintServices().isEmpty()) {
                return ResultatImpression.AucuneImprimante
            }
            val travail = PrinterJob.getPrinterJob()
            travail.jobName = tableau.titre
            travail.setPrintable(PrintableTableau(tableau))
            if (!travail.printDialog()) return ResultatImpression.Annulee
            travail.print()
            ResultatImpression.Lancee
        } catch (e: LinkageError) {
            ResultatImpression.Echec(ERREUR_COMPOSANT_MANQUANT)
        } catch (e: Exception) {
            ResultatImpression.Echec(e.message ?: e.javaClass.simpleName)
        }
    }

    /** Même sentinelle que la mise à jour interne : une installation dont un module manque. */
    const val ERREUR_COMPOSANT_MANQUANT = "COMPOSANT_MANQUANT"

    /**
     * Le rendu d'une page. Le système demande les pages **une par une** et ne connaît pas d'avance
     * la fin : rendre `NO_SUCH_PAGE` est la seule façon de la lui dire.
     */
    private class PrintableTableau(private val tableau: Tableau) : Printable {

        override fun print(graphics: Graphics, format: PageFormat, index: Int): Int {
            val g = graphics as Graphics2D
            val largeurUtile = (format.imageableWidth - 2 * MARGE).toFloat()
            val hauteurUtile = (format.imageableHeight - 2 * MARGE).toFloat()
            val parPage = MiseEnPageTableau.lignesParPage(hauteurUtile, HAUTEUR_ENTETE, HAUTEUR_LIGNE)
            val intervalle = MiseEnPageTableau.lignesDeLaPage(index, tableau.lignes.size, parPage)
                ?: return Printable.NO_SUCH_PAGE

            val largeurs = MiseEnPageTableau.largeursColonnes(largeurUtile, tableau.poidsColonnes)
            val gauche = (format.imageableX + MARGE).toFloat()
            val positions = MiseEnPageTableau.positionsColonnes(gauche, largeurs)
            var y = (format.imageableY + MARGE).toFloat()

            // Titre, et la date d'impression juste dessous : une liste de stock imprimée sans date
            // ne vaut rien une semaine plus tard, on ne sait plus de quel comptage elle parle.
            g.font = Font(Font.SANS_SERIF, Font.BOLD, 13)
            g.drawString(tableau.titre, gauche, y + 12f)
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 8)
            val pages = MiseEnPageTableau.nombreDePages(tableau.lignes.size, parPage)
            g.drawString(
                formatDateHeure.format(Date()) + "   —   " + (index + 1) + " / " + pages,
                gauche,
                y + 26f
            )
            y += 38f

            // En-têtes, puis un filet : sans lui, la première ligne de données se confond avec eux.
            g.font = Font(Font.SANS_SERIF, Font.BOLD, 8)
            tableau.entetes.forEachIndexed { colonne, entete ->
                dessinerCellule(g, entete, positions[colonne], y, largeurs[colonne])
            }
            y += 4f
            g.drawLine(
                gauche.toInt(),
                y.toInt(),
                (gauche + largeurUtile).toInt(),
                y.toInt()
            )

            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 8)
            for (ligne in intervalle) {
                y += HAUTEUR_LIGNE
                tableau.lignes[ligne].forEachIndexed { colonne, valeur ->
                    // Une ligne plus courte que les en-têtes ne doit pas lever : `TableauExportTest`
                    // vérifie l'invariant, mais une impression n'est pas l'endroit pour y échouer.
                    if (colonne < positions.size) {
                        dessinerCellule(g, valeur, positions[colonne], y, largeurs[colonne])
                    }
                }
            }
            return Printable.PAGE_EXISTS
        }

        /**
         * Dessine une cellule, **tronquée à la largeur de sa colonne**.
         *
         * Sans troncature, un nom de produit long passerait par-dessus la colonne suivante et
         * rendrait les deux illisibles — ce qui est pire que l'information manquante, parce qu'on
         * ne sait plus laquelle des deux on lit. Même approche que `ExportUtil.drawCellText` sur
         * Android.
         */
        private fun dessinerCellule(g: Graphics2D, texte: String, x: Float, y: Float, largeur: Float) {
            val metriques = g.fontMetrics
            val disponible = largeur - 4f
            if (disponible <= 0f) return
            var sortie = texte
            if (metriques.stringWidth(sortie) > disponible) {
                while (sortie.isNotEmpty() && metriques.stringWidth("$sortie…") > disponible) {
                    sortie = sortie.dropLast(1)
                }
                if (sortie.isNotEmpty()) sortie = "$sortie…"
            }
            g.drawString(sortie, x, y)
        }
    }
}
