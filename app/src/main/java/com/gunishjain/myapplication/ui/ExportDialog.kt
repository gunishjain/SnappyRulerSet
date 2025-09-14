package com.gunishjain.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gunishjain.myapplication.export.BitmapExporter

/**
 * Dialog for selecting export format and quality
 */
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (BitmapExporter.ExportFormat, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf(BitmapExporter.ExportFormat.PNG) }
    var quality by remember { mutableStateOf(90) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Export Drawing")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Format selection
                Text(
                    text = "Select Format:",
                    style = MaterialTheme.typography.titleMedium
                )
                
                BitmapExporter.ExportFormat.values().forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedFormat == format),
                                onClick = { selectedFormat = format },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedFormat == format),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = format.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Quality slider for JPEG
                if (selectedFormat == BitmapExporter.ExportFormat.JPEG) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quality: $quality%",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = quality.toFloat(),
                        onValueChange = { quality = it.toInt() },
                        valueRange = 10f..100f,
                        steps = 8
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onExport(selectedFormat, quality)
                }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}
