package com.example.desktop.donnees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.RecoveryCode

/**
 * Le bandeau en tête de chaque écran : d'où viennent les données affichées.
 *
 * Il remplace l'ancien bandeau de démonstration, qui ne faisait que constater le problème. Celui-ci
 * porte l'action qui le résout, et à l'endroit exact où le gérant lit que ses données ne sont pas
 * là — plutôt que dans un écran de réglages qu'il faudrait penser à ouvrir.
 *
 * Deux états, et le second est volontairement discret : une fois la boutique reliée, le bandeau ne
 * doit plus attirer l'œil, seulement permettre d'actualiser.
 */
@Composable
fun BandeauSourceDonnees(
    langue: String,
    codeRelie: String?,
    enChargement: Boolean,
    onConnecter: () -> Unit,
    onActualiser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relie = codeRelie != null
    Card(
        modifier = modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (relie) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (relie) texteRelie(langue, codeRelie!!) else texteDemonstration(langue),
                style = MaterialTheme.typography.bodyMedium,
                color = if (relie) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            if (enChargement) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                TextButton(onClick = if (relie) onActualiser else onConnecter) {
                    Text(if (relie) libelleActualiser(langue) else libelleConnecter(langue))
                }
            }
        }
    }
}

/**
 * La saisie du code de récupération.
 *
 * Le code est **déjà en possession du gérant** : il est affiché dans les Paramètres de son
 * téléphone depuis la section 40, et il est aussi déposé chez le fournisseur. Rien de nouveau à
 * créer, à configurer ni à retenir — c'est tout l'intérêt de passer par lui plutôt que par un
 * identifiant de plus.
 *
 * L'aperçu formaté sous le champ (`VRT-A3F9-K2M7-QP4X`) se met à jour pendant la frappe : il dit
 * au gérant que sa saisie est reconnue **avant** qu'il valide, au lieu de lui opposer un refus
 * après un aller-retour réseau.
 */
@Composable
fun DialogueConnexionBoutique(
    langue: String,
    saisie: String,
    onSaisieChange: (String) -> Unit,
    message: String?,
    enChargement: Boolean,
    onValider: () -> Unit,
    onFermer: () -> Unit
) {
    val apercu = RecoveryCode.normalize(saisie)?.let { RecoveryCode.format(it) }
    AlertDialog(
        onDismissRequest = { if (!enChargement) onFermer() },
        title = { Text(titreDialogue(langue), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = explicationDialogue(langue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = saisie,
                    onValueChange = onSaisieChange,
                    label = { Text(libelleChamp(langue)) },
                    singleLine = true,
                    enabled = !enChargement,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                )
                if (apercu != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = apercu,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (message != null) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onValider, enabled = !enChargement && saisie.isNotBlank()) {
                Text(if (enChargement) libelleEnCours(langue) else libelleValider(langue))
            }
        },
        dismissButton = {
            TextButton(onClick = onFermer, enabled = !enChargement) { Text(libelleAnnuler(langue)) }
        }
    )
}

/** Le message à afficher pour chaque issue, dans la langue active. */
fun messageChargement(resultat: ResultatChargement, langue: String): String? = when (resultat) {
    is ResultatChargement.Succes -> null
    ResultatChargement.CodeInvalide -> when (langue) {
        "mg" -> "Tsy mety ny kaody. Jereo tsara ny kaody ao amin'ny Paramètres-n'ny finday."
        "fr" -> "Ce code n'est pas reconnu. Recopiez-le depuis Paramètres sur le téléphone de " +
            "la boutique."
        else -> "Unrecognised code. Copy it from Settings on the shop's phone."
    }
    // Les trois issues suivantes sont séparées exprès (leçon de la section 56) : un seul message
    // ferait corriger un code qui était correct, ou attendre un réseau qui marchait.
    ResultatChargement.AucuneSauvegarde -> when (langue) {
        "mg" -> "Mbola tsy misy tahiry amin'ity kaody ity. Sokafy ny finday ary andramo " +
            "« Alefa » ao amin'ny Paramètres."
        "fr" -> "Aucune sauvegarde sous ce code. Le code est bon, mais le téléphone n'a encore " +
            "rien envoyé : ouvrez-le et touchez « Envoyer » dans Paramètres."
        else -> "No backup under this code. The code is valid, but the phone has not sent " +
            "anything yet: open it and tap “Send” in Settings."
    }
    ResultatChargement.EchecReseau -> when (langue) {
        "mg" -> "Tsy tafita ny fifandraisana. Jereo ny aterineto ary andramo indray."
        "fr" -> "Connexion impossible. Vérifiez internet sur ce PC, puis réessayez."
        else -> "Could not connect. Check this PC's internet, then try again."
    }
    ResultatChargement.SauvegardeIllisible -> when (langue) {
        "mg" -> "Simba ny tahiry nalaina. Andramo « Alefa » indray avy amin'ny finday."
        "fr" -> "La sauvegarde a été trouvée mais n'a pas pu être lue. Relancez « Envoyer » " +
            "depuis le téléphone, puis réessayez."
        else -> "The backup was found but could not be read. Send it again from the phone, " +
            "then retry."
    }
}

private fun texteDemonstration(langue: String) = when (langue) {
    "mg" -> "Entana fanandramana ity : ampifandraiso amin'ny fivarotanao mba hahitana ny tena angona."
    "fr" -> "Données de démonstration. Reliez ce PC à votre boutique pour voir vos vraies données."
    else -> "Demonstration data. Connect this PC to your shop to see your real data."
}

private fun texteRelie(langue: String, code: String) = when (langue) {
    "mg" -> "Angona marina avy amin'ny fivarotana (${RecoveryCode.format(code)})."
    "fr" -> "Données réelles de la boutique (${RecoveryCode.format(code)}) — lecture seule."
    else -> "Live shop data (${RecoveryCode.format(code)}) — read only."
}

private fun libelleConnecter(langue: String) = when (langue) {
    "mg" -> "Ampifandraiso"
    "fr" -> "Relier ma boutique"
    else -> "Connect my shop"
}

private fun libelleActualiser(langue: String) = when (langue) {
    "mg" -> "Havaozina"
    "fr" -> "Actualiser"
    else -> "Refresh"
}

private fun titreDialogue(langue: String) = when (langue) {
    "mg" -> "Ampifandraiso amin'ny fivarotana"
    "fr" -> "Relier ce PC à votre boutique"
    else -> "Connect this PC to your shop"
}

private fun explicationDialogue(langue: String) = when (langue) {
    "mg" -> "Ampidiro ny kaody famerenana hita ao amin'ny Paramètres-n'ny finday. " +
        "Ny ordinatera dia mamaky fotsiny : tsy manova na inona na inona amin'ny finday."
    "fr" -> "Saisissez le code de récupération affiché dans Paramètres sur le téléphone de la " +
        "boutique. Le PC se contente de LIRE : il ne modifie rien sur le téléphone."
    else -> "Enter the recovery code shown in Settings on the shop's phone. The PC only READS: " +
        "it changes nothing on the phone."
}

private fun libelleChamp(langue: String) = when (langue) {
    "mg" -> "Kaody famerenana"
    "fr" -> "Code de récupération"
    else -> "Recovery code"
}

private fun libelleValider(langue: String) = when (langue) {
    "mg" -> "Ampifandraiso"
    "fr" -> "Relier"
    else -> "Connect"
}

private fun libelleEnCours(langue: String) = when (langue) {
    "mg" -> "Mampidina…"
    "fr" -> "Chargement…"
    else -> "Loading…"
}

private fun libelleAnnuler(langue: String) = when (langue) {
    "mg" -> "Hajanona"
    "fr" -> "Annuler"
    else -> "Cancel"
}
