package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CompressionOptions
import com.example.model.CompressionReport
import com.example.ui.PolyStudioViewModel
import com.example.ui.theme.*

@Composable
fun MolecularCompressPanel(
    viewModel: PolyStudioViewModel,
    options: CompressionOptions,
    report: CompressionReport?,
    isComparisonActive: Boolean,
    onOpenExportDialog: () -> Unit
) {
    var customInputText by remember(options.targetMolecularParts) {
        mutableStateOf(options.targetMolecularParts.toString())
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    val quickPresets = listOf(
        Pair("Ultra Low", 50),
        Pair("Low Poly", 150),
        Pair("Medium", 400),
        Pair("Detailed", 800),
        Pair("High Detail", 1500),
        Pair("Ultra High", 3000)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("molecular_compress_panel")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Compress, contentDescription = null, tint = VibrantPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Shape-Preserving Low-Poly Decimation", style = MaterialTheme.typography.titleMedium, color = VibrantTextPrimary)
        }
        Text(
            text = "Reduces triangle count into larger polygonal facets while keeping 100% of the model's exact 3D volume and outer shape",
            style = MaterialTheme.typography.labelSmall,
            color = VibrantTextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Target Molecular / Polygon Count Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Target Polygon Facets: ${options.targetMolecularParts}",
                style = MaterialTheme.typography.titleSmall,
                color = VibrantPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text("10 to 3,000 Facets", style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)
        }

        Slider(
            value = options.targetMolecularParts.toFloat().coerceIn(10f, 3000f),
            onValueChange = {
                viewModel.runMolecularCompression(it.toInt())
            },
            valueRange = 10f..3000f,
            colors = SliderDefaults.colors(
                thumbColor = VibrantPrimary,
                activeTrackColor = VibrantPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Polygon Count Input Field
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = customInputText,
                onValueChange = { customInputText = it },
                label = { Text("Custom Target Count", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val parsed = customInputText.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            viewModel.runMolecularCompression(parsed)
                        }
                        keyboardController?.hide()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibrantPrimary,
                    unfocusedBorderColor = VibrantSurfaceVariant,
                    focusedLabelColor = VibrantPrimary
                ),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    val parsed = customInputText.toIntOrNull()
                    if (parsed != null && parsed > 0) {
                        viewModel.runMolecularCompression(parsed)
                    }
                    keyboardController?.hide()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimary),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apply")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Preset Chips
        Text("Quick Preset Target Facets:", style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickPresets) { (label, count) ->
                val isSelected = options.targetMolecularParts == count
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.runMolecularCompression(count) },
                    label = { Text("$label ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibrantPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = VibrantSurfaceVariant,
                        labelColor = VibrantTextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Real-Time Polygon Reduction Report Card
        if (report != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = VibrantSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("Original Polys", "${report.originalPolyCount}", VibrantTextSecondary)
                        MetricItem("New Big Polys", "${report.compressedPolyCount}", VibrantPrimary)
                        MetricItem("Reduction", "-${report.reductionPercentage.toInt()}%", VibrantAccentGreen)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("Orig Size", "${report.originalSizeKb} KB", VibrantTextSecondary)
                        MetricItem("Comp Size", "${report.compressedSizeKb} KB", VibrantPrimary)
                        MetricItem("Shape Fidelity", "${String.format("%.1f", report.fidelityScore)}%", VibrantAccentPink)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Split-Screen Side-by-Side Comparison Toggle
            OutlinedButton(
                onClick = { viewModel.toggleComparisonView(!isComparisonActive) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isComparisonActive) VibrantActive else Color.Transparent,
                    contentColor = if (isComparisonActive) VibrantHighlight else VibrantTextPrimary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isComparisonActive) "Exit Split" else "Split View", style = MaterialTheme.typography.labelSmall)
            }

            // Apply to Scene
            Button(
                onClick = { viewModel.applyMolecularCompressionToScene() },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantActive, contentColor = VibrantHighlight),
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply to Scene", style = MaterialTheme.typography.labelSmall)
            }

            // Export Compressed GLB/FBX
            Button(
                onClick = onOpenExportDialog,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimary, contentColor = VibrantOnPrimary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}


