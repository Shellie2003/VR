package com.example.desktop.maj

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

/** Ce que la carte montre à un instant donné. */
private sealed interface EtatMaj {
    object Repos : EtatMaj
    object Verification : EtatMaj
    object AJour : EtatMaj
    data class Disponible(val info: InfoMajBureau) : EtatMaj
    data class Telechargement(val progression: Float) : EtatMaj
    data class Prete(val info: InfoMajBureau, val nouveau: File) : EtatMaj
    data class Echec(val detail: String) : EtatMaj
}

/**
 * Mise à jour du logiciel depuis le logiciel lui-même, sans désinstaller ni réinstaller.
 *
 * ## Pourquoi cette carte existe
 *
 * La version de bureau ne passe par aucun magasin d'applications. Sans elle, chaque correctif
 * imposerait au gérant de retrouver un lien, télécharger ~90 Mo, désinstaller puis réinstaller —
 * une manipulation que personne ne fait, donc une boutique qui reste indéfiniment sur sa première
 * version, correctifs de données compris. C'est mot pour mot le défaut corrigé côté Android en
 * section 77, et la section 77 avait justement noté que le bureau l'attendrait.
 *
 * ## Facultative, et déclenchée par le gérant
 *
 * Aucune vérification automatique au démarrage ici, contrairement à Android. La raison n'est pas la
 * paresse : sur Android, la vérification silencieuse sert à **allumer une pastille**, et la mise à
 * jour reste un geste manuel. Ici le remplacement **ferme et relance le logiciel** — rien ne doit
 * pouvoir le déclencher pendant qu'une vente est en cours. Le gérant ouvre « À propos », il demande.
 *
 * ## État local plutôt que remonté à la fenêtre
 *
 * Contrairement aux composables de `:core:ui`, dont tout l'état arrive en paramètres pour qu'ils
 * restent partageables avec Android, celui-ci est **spécifique à cette plateforme** : il parle de
 * dossier d'installation, de processus et de script Windows. Le garder local évite d'imposer à la
 * fenêtre un état qui ne concerne qu'elle-même.
 */
@Composable
fun CarteMajBureau(
    langue: String,
    versionInstallee: Int?,
    racineInstallation: File?,
    modifier: Modifier = Modifier,
    service: ServiceMajBureau = remember { ServiceMajBureau() }
) {
    val portee = rememberCoroutineScope()
    var etat by remember { mutableStateOf<EtatMaj>(EtatMaj.Repos) }

    // L'obstacle est calculé À L'AFFICHAGE et non au clic : annoncer « nouvelle version
    // disponible » puis refuser de l'installer fait perdre deux fois le temps du gérant. Mieux vaut
    // lui dire tout de suite que cette installation-ci ne peut pas se mettre à jour seule.
    val obstacle = remember(racineInstallation, versionInstallee) {
        if (versionInstallee == null) ObstacleMaj.HORS_PAQUET else service.obstacle(racineInstallation)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = titre(langue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = sousTitre(langue),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            if (obstacle != null) {
                Message(texte = messageObstacle(obstacle, langue), erreur = false)
                return@Column
            }

            when (val courant = etat) {
                is EtatMaj.Repos, is EtatMaj.AJour, is EtatMaj.Echec -> {
                    Button(
                        onClick = {
                            portee.launch {
                                etat = EtatMaj.Verification
                                // try/catch/finally et non un simple fold : c'est la leçon des
                                // sections 55 et 77. Si la coroutine est interrompue entre la
                                // remise à zéro du bouton et l'écriture du message, le gérant voit
                                // un bouton redevenu normal SANS aucun résultat — et ne sait pas
                                // s'il doit réessayer. Ici un état est écrit dans tous les cas.
                                etat = try {
                                    service.verifier(versionInstallee!!).fold(
                                        onSuccess = { info ->
                                            if (info == null) EtatMaj.AJour
                                            else EtatMaj.Disponible(info)
                                        },
                                        onFailure = { EtatMaj.Echec(messageEchec(it, langue)) }
                                    )
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    EtatMaj.Echec(messageEchec(e, langue))
                                }
                            }
                        }
                    ) { Text(libelleVerifier(langue)) }

                    if (courant is EtatMaj.AJour) {
                        Spacer(Modifier.height(12.dp))
                        Message(texte = messageAJour(langue), erreur = false)
                    }
                    if (courant is EtatMaj.Echec) {
                        Spacer(Modifier.height(12.dp))
                        Message(texte = courant.detail, erreur = true)
                    }
                }

                is EtatMaj.Verification -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Text(
                        text = libelleVerificationEnCours(langue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is EtatMaj.Disponible -> {
                    VersionProposee(courant.info, langue)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            portee.launch {
                                // Le compteur appartient à CE téléchargement : déclaré dans la
                                // coroutine, il ne survit pas à son annulation et n'a pas à être
                                // remis à zéro à la main.
                                var dernierPourcent = -1
                                etat = EtatMaj.Telechargement(0f)
                                etat = try {
                                    service.telechargerEtPreparer(
                                        info = courant.info,
                                        racine = racineInstallation!!,
                                        // Un état par POURCENT et non par bloc lu : le
                                        // téléchargement fait ~90 Mo par blocs de 64 Ko, soit
                                        // près de mille cinq cents recompositions pour une barre
                                        // qui n'avance que de cent crans. Le gérant ne verrait
                                        // aucune différence, le logiciel passerait son temps à se
                                        // redessiner pendant qu'il télécharge.
                                        onProgression = { p ->
                                            val borne = p.coerceIn(0f, 1f)
                                            val pourcent = (borne * 100).toInt()
                                            if (pourcent != dernierPourcent) {
                                                dernierPourcent = pourcent
                                                etat = EtatMaj.Telechargement(borne)
                                            }
                                        }
                                    ).fold(
                                        onSuccess = { EtatMaj.Prete(courant.info, it) },
                                        onFailure = { erreur ->
                                            EtatMaj.Echec(messageEchec(erreur, langue))
                                        }
                                    )
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    EtatMaj.Echec(messageEchec(e, langue))
                                }
                            }
                        }
                    ) { Text(libelleTelecharger(langue)) }
                }

                is EtatMaj.Telechargement -> {
                    Text(
                        text = libelleTelechargementEnCours(langue, courant.progression),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { courant.progression },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                }

                is EtatMaj.Prete -> {
                    VersionProposee(courant.info, langue)
                    Spacer(Modifier.height(12.dp))
                    Message(texte = messagePrete(langue), erreur = false)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            // Le script attend la disparition de NOTRE processus avant de toucher
                            // au dossier : il faut donc réellement sortir, et tout de suite. Si le
                            // lancement du script échoue, on ne sort surtout pas — le gérant garde
                            // son logiciel ouvert et lit pourquoi.
                            service.lancerRemplacement(racineInstallation!!, courant.nouveau).fold(
                                onSuccess = { exitProcess(0) },
                                onFailure = { etat = EtatMaj.Echec(messageRemplacement(langue)) }
                            )
                        }
                    ) { Text(libelleInstaller(langue)) }
                }
            }
        }
    }
}

@Composable
private fun VersionProposee(info: InfoMajBureau, langue: String) {
    Column {
        Text(
            text = when (langue) {
                "mg" -> "Misy version vaovao : ${info.versionName}"
                "fr" -> "Nouvelle version disponible : ${info.versionName}"
                else -> "New version available: ${info.versionName}"
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (info.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = info.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Message(texte: String, erreur: Boolean) {
    // Une carte pleine couleur et non un petit texte gris : la section 55 avait constaté qu'un
    // résultat fondu dans le décor est un résultat qu'un utilisateur pressé ne remarque pas, au
    // point de croire qu'il ne s'est rien passé du tout.
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (erreur) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = texte,
            style = MaterialTheme.typography.bodyMedium,
            color = if (erreur) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

// --- Libellés. Trois langues, comme l'exige AGENTS.md section 3. -------------------------------

private fun titre(langue: String) = when (langue) {
    "mg" -> "Fanavaozana ny rindrambaiko"
    "fr" -> "Mise à jour du logiciel"
    else -> "Software update"
}

private fun sousTitre(langue: String) = when (langue) {
    "mg" -> "Tsy mila esorina sy apetraka indray ny rindrambaiko."
    "fr" -> "Sans désinstaller ni réinstaller : le logiciel se remplace et se relance seul."
    else -> "No uninstall, no reinstall: the software replaces itself and restarts."
}

private fun libelleVerifier(langue: String) = when (langue) {
    "mg" -> "Hijery fanavaozana"
    "fr" -> "Vérifier les mises à jour"
    else -> "Check for updates"
}

private fun libelleVerificationEnCours(langue: String) = when (langue) {
    "mg" -> "Mijery…"
    "fr" -> "Vérification…"
    else -> "Checking…"
}

private fun libelleTelecharger(langue: String) = when (langue) {
    "mg" -> "Hampidina"
    "fr" -> "Télécharger"
    else -> "Download"
}

private fun libelleTelechargementEnCours(langue: String, progression: Float): String {
    val pourcent = (progression * 100).toInt()
    return when (langue) {
        "mg" -> "Ampidinina… $pourcent %"
        "fr" -> "Téléchargement… $pourcent %"
        else -> "Downloading… $pourcent %"
    }
}

private fun libelleInstaller(langue: String) = when (langue) {
    "mg" -> "Apetraka ary avereno alefa"
    "fr" -> "Installer et redémarrer"
    else -> "Install and restart"
}

private fun messageAJour(langue: String) = when (langue) {
    "mg" -> "Efa ny version farany no ampiasainao."
    "fr" -> "Vous utilisez déjà la dernière version."
    else -> "You already have the latest version."
}

private fun messagePrete(langue: String) = when (langue) {
    "mg" -> "Vonona. Hikatona ny rindrambaiko ary hiverina ho azy amin'ny version vaovao."
    "fr" -> "Prêt. Le logiciel va se fermer puis se rouvrir tout seul sur la nouvelle version."
    else -> "Ready. The software will close and reopen itself on the new version."
}

/**
 * Le message à montrer pour un échec, selon sa cause réelle.
 *
 * Un seul aiguillage pour les trois sites d'échec de cette carte, plutôt qu'un `if` recopié :
 * c'est précisément la duplication qui avait laissé le cas de l'empreinte traité d'un côté et pas
 * de l'autre. Et surtout, les trois causes ne demandent pas la même chose au gérant — réessayer
 * plus tard, réessayer tout de suite, ou prévenir le fournisseur. Les fondre en « erreur réseau »
 * ferait attendre une connexion qui marchait (section 56).
 */
private fun messageEchec(erreur: Throwable, langue: String): String = when (erreur.message) {
    ServiceMajBureau.ERREUR_COMPOSANT_MANQUANT -> messageComposant(langue)
    "EMPREINTE_INVALIDE" -> messageEmpreinte(langue)
    else -> messageReseau(langue)
}

private fun messageComposant(langue: String) = when (langue) {
    // Rien à réessayer : c'est le paquet installé qui est incomplet, pas la connexion. Le dire est
    // la seule façon que l'information remonte au fournisseur au lieu d'être prise pour une
    // coupure de réseau, que le gérant attendrait en vain.
    "mg" -> "Tsy feno ity rindrambaiko napetraka ity : tsy afaka manavao ho azy. " +
        "Ampandreneso ny mpamatsy."
    "fr" -> "Cette installation est incomplète : la mise à jour automatique n'est pas disponible. " +
        "Signalez-le au fournisseur."
    else -> "This installation is incomplete: automatic updating is unavailable. " +
        "Please report it to your supplier."
}

private fun messageReseau(langue: String) = when (langue) {
    "mg" -> "Tsy afaka nijery : jereo ny fifandraisana amin'ny aterineto."
    "fr" -> "Vérification impossible : contrôlez la connexion internet, puis réessayez."
    else -> "Could not check: check the internet connection, then try again."
}

private fun messageEmpreinte(langue: String) = when (langue) {
    // Le fichier a été refusé plutôt qu'installé : c'est la bonne nouvelle, et il faut le dire,
    // sinon un gérant conclut à une panne là où le logiciel vient de le protéger.
    "mg" -> "Simba ny rakitra nampidinina ka nolavina. Andramo indray."
    "fr" -> "Le fichier téléchargé était incomplet : il a été refusé, rien n'a été installé. " +
        "Réessayez."
    else -> "The downloaded file was incomplete: it was rejected and nothing was installed. " +
        "Please try again."
}

private fun messageRemplacement(langue: String) = when (langue) {
    "mg" -> "Tsy afaka nanolo ny rindrambaiko. Tsy niova izy ka mbola mandeha tsara."
    "fr" -> "Le remplacement n'a pas pu démarrer. Rien n'a été modifié : le logiciel " +
        "fonctionne toujours."
    else -> "The replacement could not start. Nothing was changed: the software still works."
}

private fun messageObstacle(obstacle: ObstacleMaj, langue: String) = when (obstacle) {
    ObstacleMaj.HORS_PAQUET -> when (langue) {
        "mg" -> "Nalefa avy amin'ny loharano ity : tsy misy paika hosoloina."
        "fr" -> "Lancé depuis les sources : il n'y a pas de paquet installé à remplacer."
        else -> "Running from sources: there is no installed package to replace."
    }
    ObstacleMaj.SYSTEME_NON_PRIS_EN_CHARGE -> when (langue) {
        "mg" -> "Windows ihany no ahafahana manavao ho azy aloha."
        "fr" -> "La mise à jour automatique n'est disponible que sous Windows pour le moment."
        else -> "Automatic update is only available on Windows for now."
    }
    ObstacleMaj.DOSSIER_PROTEGE -> when (langue) {
        "mg" -> "Voaaro ny lahatahiry misy ny rindrambaiko : ilaina ny mpandrindra."
        "fr" -> "Le dossier d'installation est protégé par le système. Cette copie a été posée " +
            "par l'installateur : relancez-le en administrateur pour mettre à jour, ou utilisez " +
            "la version portable, qui se met à jour seule."
        else -> "The installation folder is protected by the system. This copy was placed by " +
            "the installer: run it again as administrator to update, or use the portable " +
            "version, which updates itself."
    }
}
