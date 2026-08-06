package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.TouchApp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Material3D
import com.example.tools.BrushType
import com.example.tools.Painter3D
import com.example.ui.PolyStudioViewModel
import com.example.ui.theme.*

@Composable
fun PaintPanel(
    viewModel: PolyStudioViewModel,
    brushState: Painter3D.PaintBrush
) {
    val isCameraLocked by viewModel.isCameraLocked.collectAsStateWithLifecycle()
    val autoSelectOnTouch by viewModel.autoSelectMeshOnTouch.collectAsStateWithLifecycle()

    val colorSwatches = listOf(
        Color(0xFF0F172A), VibrantPrimary, VibrantAccentPink, VibrantAccentGreen,
        VibrantAccentRed, VibrantHighlight, Color(0xFFA855F7), Color(0xFFFFD700), Color(0xFFFFFFFF)
    )

    val materialPresets = listOf(
        Material3D.StandardGold,
        Material3D.ChromeSilver,
        Material3D.NeonGlow,
        Material3D.MattePlastic,
        Material3D.CarbonFiber,
        Material3D.EmeraldGlass
    )

    val watermarkPatterns = listOf("Star Emblem", "Geometric Grid", "Logo Stamp")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("paint_panel")
    ) {
        // Painting Camera Lock Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isCameraLocked) VibrantPrimaryContainer else VibrantSurfaceVariant
            ),
            onClick = { viewModel.toggleCameraLock() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCameraLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isCameraLocked) VibrantAccentPink else VibrantPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCameraLocked) "🔒 Camera Locked for Painting" else "🔓 Camera Unlocked",
                        style = MaterialTheme.typography.titleSmall,
                        color = VibrantTextPrimary
                    )
                    Text(
                        text = if (isCameraLocked) "Touch drag paints continuous strokes without orbiting camera" else "Lock camera angle to paint smoothly without moving viewport",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantTextSecondary
                    )
                }
                Switch(
                    checked = isCameraLocked,
                    onCheckedChange = { viewModel.setCameraLock(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VibrantAccentPink,
                        checkedTrackColor = VibrantPrimary
                    )
                )
            }
        }

        // Direct Touch Object Selection Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (autoSelectOnTouch) VibrantSurfaceVariant else VibrantSurfaceVariant.copy(alpha = 0.6f)
            ),
            onClick = { viewModel.toggleAutoSelectMeshOnTouch() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = if (autoSelectOnTouch) VibrantPrimary else VibrantTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "👆 Touch to Select Object",
                        style = MaterialTheme.typography.titleSmall,
                        color = VibrantTextPrimary
                    )
                    Text(
                        text = if (autoSelectOnTouch) "Enabled: Tapping any model instantly selects it for editing/painting" else "Disabled: Selection stays locked on current object",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantTextSecondary
                    )
                }
                Switch(
                    checked = autoSelectOnTouch,
                    onCheckedChange = { viewModel.setAutoSelectMeshOnTouch(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VibrantPrimary,
                        checkedTrackColor = VibrantAccentGreen
                    )
                )
            }
        }

        Text("Paint Tool Selection", style = MaterialTheme.typography.titleMedium, color = VibrantTextPrimary)
        Spacer(modifier = Modifier.height(6.dp))

        // Brush Type Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BrushType.values()) { bType ->
                val isSelected = brushState.type == bType
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.updateBrushType(bType) },
                    label = {
                        Text("${bType.icon} ${bType.displayName}")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibrantPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = VibrantSurfaceVariant,
                        labelColor = VibrantTextPrimary
                    )
                )
            }
        }

        // Contextual Tool Instructions / Sub-options
        if (brushState.type == BrushType.FILL_BUCKET) {
            Card(
                colors = CardDefaults.cardColors(containerColor = VibrantPrimaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🪣", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fill Bucket Mode: Draw an outline with Pen/Pencil first, then tap inside the drawn area to flood fill color fast!",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantTextPrimary
                    )
                }
            }
        } else if (brushState.type == BrushType.WATERMARK) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Watermark Stamp Pattern:", style = MaterialTheme.typography.labelSmall, color = VibrantPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    watermarkPatterns.forEach { pattern ->
                        val isSel = brushState.watermarkPattern == pattern
                        SuggestionChip(
                            onClick = { viewModel.updateWatermarkPattern(pattern) },
                            label = { Text(pattern, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSel) VibrantAccentPink else VibrantSurfaceVariant,
                                labelColor = if (isSel) Color.White else VibrantTextPrimary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sliders for Radius and Opacity
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Radius: ${String.format("%.2f", brushState.radius)}m", style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = brushState.radius,
                onValueChange = { viewModel.updateBrushRadius(it) },
                valueRange = 0.05f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = VibrantPrimary,
                    activeTrackColor = VibrantPrimary
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Opacity: ${(brushState.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = brushState.opacity,
                onValueChange = { viewModel.updateBrushOpacity(it) },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = VibrantAccentPink,
                    activeTrackColor = VibrantAccentPink
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Color Swatches
        Text("Brush Color Palette", style = MaterialTheme.typography.labelSmall, color = VibrantPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colorSwatches.forEach { color ->
                val isSelected = brushState.color == color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                        )
                        .clickable { viewModel.updateBrushColor(color) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Material Presets
        Text("Apply Full Material Preset", style = MaterialTheme.typography.labelSmall, color = VibrantPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(materialPresets) { mat ->
                Card(
                    onClick = { viewModel.applyMaterialPresetToSelected(mat) },
                    colors = CardDefaults.cardColors(containerColor = VibrantSurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(mat.baseColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = mat.presetName, style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
                    }
                }
            }
        }
    }
}


