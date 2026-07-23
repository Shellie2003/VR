package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact toolbar reused across list screens (Stock, Historique, Dettes, Mouvements de caisse).
 * Normally shows PDF/Excel export buttons and a toggle to enter checkbox multi-selection mode.
 * While in selection mode it switches to show the selection count, "select all" and bulk delete.
 */
@Composable
fun SelectionExportToolbar(
    activeLang: String,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onToggleSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    showExportButtons: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Text(
                text = when (activeLang) {
                    "mg" -> "$selectedCount voafidy"
                    "fr" -> "$selectedCount sélectionné(s)"
                    else -> "$selectedCount selected"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSelectAll, modifier = Modifier.testTag("selection_select_all")) {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Rehetra"
                        "fr" -> "Tout"
                        else -> "All"
                    },
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = onDeleteSelected,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("selection_delete_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete selected",
                    tint = if (selectedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                )
            }
            IconButton(onClick = onToggleSelectionMode, modifier = Modifier.testTag("selection_cancel_button")) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel selection")
            }
        } else {
            IconButton(onClick = onToggleSelectionMode, modifier = Modifier.testTag("selection_mode_toggle")) {
                Icon(
                    imageVector = Icons.Default.CheckBox,
                    contentDescription = when (activeLang) {
                        "mg" -> "Fifidianana"
                        "fr" -> "Sélectionner"
                        else -> "Select"
                    },
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (showExportButtons) {
                TextButton(onClick = onExportCsv, modifier = Modifier.testTag("export_csv_button")) {
                    Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Excel", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onExportPdf, modifier = Modifier.testTag("export_pdf_button")) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "PDF", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
