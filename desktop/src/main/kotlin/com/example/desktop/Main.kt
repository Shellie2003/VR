package com.example.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Point d'entrée de la version de bureau.
 *
 * Cette fenêtre ne contient PAS encore la caisse : elle est le témoin de la chaîne de build. Tant
 * qu'elle s'ouvre, trois choses sont prouvées d'un coup — le code Compose compile hors Android, le
 * paquet natif se fabrique, et il se lance sur la machine cible. C'est ce socle qui permettra
 * d'amener les écrans un par un en sachant, à chaque étape, si c'est le code déplacé ou la chaîne
 * de build qui est en cause.
 *
 * Les couleurs sont reprises de la charte Android (vert sombre `0xFF012D1D` en clair,
 * `0xFFC1ECD4` en sombre) écrites ici en dur à dessein : le thème du projet vit dans `:app`, donc
 * dans un module Android inaccessible depuis ici. C'est exactement la dette que le découpage en
 * modules partagés viendra lever — et la laisser visible vaut mieux que de recopier tout le thème.
 */
fun main() = application {
    val version = DesktopBuildInfo.versionCourante()

    Window(
        onCloseRequest = ::exitApplication,
        title = DesktopBuildInfo.titreFenetre(version),
        state = rememberWindowState(width = 900.dp, height = 640.dp)
    ) {
        EcranSocle(version = version)
    }
}

@Composable
private fun EcranSocle(version: String?) {
    val schema = lightColorScheme(
        primary = Color(0xFF012D1D),
        secondary = Color(0xFF855300),
        background = Color(0xFFFBF9F8),
        surface = Color(0xFFFBF9F8),
        surfaceVariant = Color(0xFFEFEDED)
    )

    MaterialTheme(colorScheme = schema) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = DesktopBuildInfo.NOM_APPLICATION,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Version de bureau — socle de construction",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(32.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        LigneInfo("Interface", "Compose Multiplatform (JVM)")
                        LigneInfo("Version du paquet", version ?: "lancée depuis les sources")
                        LigneInfo("Java", System.getProperty("java.version") ?: "inconnue")
                        LigneInfo(
                            "Système",
                            "${System.getProperty("os.name")} ${System.getProperty("os.arch")}"
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Prochaine étape : extraire le code commun (modèles, calculs, " +
                                "écrans) en modules indépendants, puis les brancher ici.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LigneInfo(libelle: String, valeur: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = libelle,
            modifier = Modifier.width(180.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valeur,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
