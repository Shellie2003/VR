package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.AnomalyDetector
import com.example.util.FormatUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Écran « Sécurité & Surveillance » — la vue du gérant sur ce qui se passe réellement dans son
 * épicerie quand il n'est pas derrière le comptoir.
 *
 * Deux onglets :
 *  - **Alertes** : les anomalies détectées (manquants de caisse, suppressions, retours anormaux,
 *    ventes sous le prix d'achat...), classées par gravité, avec le poste et l'employé concernés ;
 *  - **Journal** : la trace brute et horodatée de chaque geste sensible, y compris les tentatives
 *    refusées.
 *
 * L'écran ne dénonce personne et ne bloque rien : il rend visible ce qui, autrement, ne se voit
 * qu'au moment où l'argent manque.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuriteScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val anomalies by viewModel.anomalies.collectAsState()
    val logs by viewModel.auditLogs.collectAsState()
    val fenetreJours by viewModel.fenetreAnalyseJours.collectAsState()

    var ongletAlertes by remember { mutableStateOf(true) }

    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE) }

    val titre = when (activeLang) {
        "mg" -> "Fiarovana sy Fanaraha-maso"
        "fr" -> "Sécurité & Surveillance"
        else -> "Security & Monitoring"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(titre, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("securite_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        )

        // Fenêtre d'analyse : une épicerie regarde rarement plus loin qu'un mois en arrière, mais
        // le gérant doit pouvoir resserrer sur la semaine écoulée après un incident.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(7, 30, 90).forEach { jours ->
                FilterChip(
                    selected = fenetreJours == jours,
                    onClick = { viewModel.fenetreAnalyseJours.value = jours },
                    label = {
                        Text(
                            when (activeLang) {
                                "mg" -> "$jours andro"
                                else -> "$jours j"
                            },
                            fontWeight = if (fenetreJours == jours) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        TabRow(selectedTabIndex = if (ongletAlertes) 0 else 1) {
            Tab(
                selected = ongletAlertes,
                onClick = { ongletAlertes = true },
                text = {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Fampitandremana (${anomalies.size})"
                            "fr" -> "Alertes (${anomalies.size})"
                            else -> "Alerts (${anomalies.size})"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.testTag("securite_tab_alertes")
            )
            Tab(
                selected = !ongletAlertes,
                onClick = { ongletAlertes = false },
                text = {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Tantara"
                            "fr" -> "Journal"
                            else -> "Journal"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.testTag("securite_tab_journal")
            )
        }

        if (ongletAlertes) {
            if (anomalies.isEmpty()) {
                EtatVideSecurite(
                    icone = Icons.Default.VerifiedUser,
                    couleur = Color(0xFF2E7D32),
                    message = when (activeLang) {
                        "mg" -> "Tsy misy zavatra mampiahiahy hita tato anatin'ny $fenetreJours andro."
                        "fr" -> "Aucune anomalie détectée sur les $fenetreJours derniers jours."
                        else -> "No anomaly detected over the last $fenetreJours days."
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(anomalies, key = { "${it.code}_${it.responsable}_${it.timestamp}" }) { anomalie ->
                        CarteAnomalie(anomalie = anomalie, dateFormat = dateFormat, activeLang = activeLang)
                    }
                }
            }
        } else {
            if (logs.isEmpty()) {
                EtatVideSecurite(
                    icone = Icons.Default.HistoryToggleOff,
                    couleur = MaterialTheme.colorScheme.outline,
                    message = when (activeLang) {
                        "mg" -> "Mbola tsy misy zavatra voarakitra."
                        "fr" -> "Aucune trace enregistrée pour l'instant."
                        else -> "No trace recorded yet."
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        LigneJournal(log = log, dateFormat = dateFormat)
                    }
                }
            }
        }
    }
}

@Composable
private fun EtatVideSecurite(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    couleur: Color,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(icone, contentDescription = null, tint = couleur, modifier = Modifier.size(56.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun CarteAnomalie(
    anomalie: AnomalyDetector.Anomalie,
    dateFormat: SimpleDateFormat,
    activeLang: String
) {
    val couleur = couleurSeverite(anomalie.severite)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("anomalie_${anomalie.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = couleur.copy(alpha = 0.07f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, couleur.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = iconeSeverite(anomalie.severite),
                        contentDescription = null,
                        tint = couleur,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = anomalie.titre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(couleur.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = libelleSeverite(anomalie.severite, activeLang),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = couleur
                    )
                }
            }

            Text(
                text = anomalie.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(Date(anomalie.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (anomalie.montant > 0.0) {
                    Text(
                        text = "${FormatUtil.formatPrice(anomalie.montant)} Ar",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = couleur
                    )
                }
            }
        }
    }
}

@Composable
private fun LigneJournal(log: AuditLog, dateFormat: SimpleDateFormat) {
    val couleur = if (log.bloque) Color(0xFFD32F2F) else couleurSeverite(log.severite)
    val auteur = listOf(log.utilisateur, log.deviceName).filter { it.isNotBlank() }.joinToString(" • ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(couleur)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (log.bloque) "⛔ ${log.type}" else log.type,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = couleur
                )
                if (log.cible.isNotBlank()) {
                    Text(text = log.cible, style = MaterialTheme.typography.bodyMedium)
                }
                if (log.details.isNotBlank()) {
                    Text(
                        text = log.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = buildString {
                        append(dateFormat.format(Date(log.timestamp)))
                        if (auteur.isNotBlank()) append("  —  $auteur")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (log.montant > 0.0) {
                Text(
                    text = "${FormatUtil.formatPrice(log.montant)} Ar",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun couleurSeverite(severite: String): Color = when (severite) {
    AuditLog.SEVERITE_ALERTE -> Color(0xFFD32F2F)
    AuditLog.SEVERITE_ATTENTION -> Color(0xFFEF6C00)
    else -> Color(0xFF0277BD)
}

private fun iconeSeverite(severite: String) = when (severite) {
    AuditLog.SEVERITE_ALERTE -> Icons.Default.GppMaybe
    AuditLog.SEVERITE_ATTENTION -> Icons.Default.WarningAmber
    else -> Icons.Default.Info
}

private fun libelleSeverite(severite: String, lang: String): String = when (severite) {
    AuditLog.SEVERITE_ALERTE -> when (lang) {
        "mg" -> "MAIKA"
        "en" -> "CRITICAL"
        else -> "ALERTE"
    }
    AuditLog.SEVERITE_ATTENTION -> when (lang) {
        "mg" -> "TANDREMO"
        "en" -> "WARNING"
        else -> "ATTENTION"
    }
    else -> "INFO"
}
