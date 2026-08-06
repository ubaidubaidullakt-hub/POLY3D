package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Light3D
import com.example.ui.PolyStudioViewModel
import com.example.ui.theme.*

data class BackgroundPreset(
    val name: String,
    val color: Color,
    val icon: String
)

@Composable
fun LightingMaterialPanel(
    viewModel: PolyStudioViewModel,
    light: Light3D
) {
    val sceneState by viewModel.sceneState.collectAsStateWithLifecycle()
    val isTurntableActive by viewModel.isTurntableActive.collectAsStateWithLifecycle()

    val backgroundPresets = listOf(
        BackgroundPreset("Dark Slate", Color(0xFF0F172A), "🌙"),
        BackgroundPreset("Studio White", Color(0xFFF8FAFC), "☀️"),
        BackgroundPreset("Neutral Gray", Color(0xFF262626), "🪙"),
        BackgroundPreset("OLED Black", Color(0xFF000000), "🖤"),
        BackgroundPreset("Blueprint Blue", Color(0xFF0C4A6E), "📘"),
        BackgroundPreset("Cyber Sunset", Color(0xFF31103F), "🌆"),
        BackgroundPreset("Emerald Synth", Color(0xFF064E3B), "🌲"),
        BackgroundPreset("Warm Cream", Color(0xFFFEF3C7), "📜")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("lighting_material_panel")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = VibrantPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Environment & Backdrop Studio", style = MaterialTheme.typography.titleMedium, color = VibrantTextPrimary)
        }
        Text("Customize 3D viewport background color, studio lighting, grid and showcase turntable", style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Viewport Background Colors
        Text("🎨 3D Viewport Background Color", style = MaterialTheme.typography.titleSmall, color = VibrantPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(backgroundPresets) { preset ->
                val isSelected = sceneState.backgroundColor == preset.color
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VibrantSurfaceVariant,
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) VibrantPrimary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setBackgroundColor(preset.color) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(preset.color)
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${preset.icon} ${preset.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) VibrantPrimary else VibrantTextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Turntable & Camera Showcase Mode
        Text("🎠 3D Showcase & Presentation", style = MaterialTheme.typography.titleSmall, color = VibrantPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.toggleTurntable() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTurntableActive) VibrantAccentPink else VibrantPrimary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isTurntableActive) "Pause Turntable" else "Spin Turntable", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = { viewModel.toggleGridVisibility() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (sceneState.gridSettings.showGrid) "Hide Grid" else "Show Grid", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Projection mode toggle (Perspective vs Orthographic)
        OutlinedButton(
            onClick = { viewModel.toggleOrthographicProjection() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (sceneState.camera.isOrthographic) "Switch to Perspective View" else "Switch to Orthographic (CAD) View",
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Camera Angle Presets
        Text("Quick Camera Angles:", style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Isometric", "Front", "Top", "Right").forEach { angle ->
                FilterChip(
                    selected = false,
                    onClick = { viewModel.setPresetCameraView(angle) },
                    label = { Text(angle, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = VibrantSurfaceVariant,
                        labelColor = VibrantTextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Studio Lighting Controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = VibrantAccentGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Studio Lighting", style = MaterialTheme.typography.titleSmall, color = VibrantPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Key Light Intensity: ${String.format("%.1f", light.intensity)}x", style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
                Spacer(modifier = Modifier.weight(1f))
            }
            Slider(
                value = light.intensity,
                onValueChange = { viewModel.updateLightIntensity(it, light.ambientIntensity) },
                valueRange = 0.2f..2.5f,
                colors = SliderDefaults.colors(thumbColor = VibrantAccentGreen, activeTrackColor = VibrantAccentGreen)
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ambient Skylight: ${String.format("%.1f", light.ambientIntensity)}x", style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
                Spacer(modifier = Modifier.weight(1f))
            }
            Slider(
                value = light.ambientIntensity,
                onValueChange = { viewModel.updateLightIntensity(light.intensity, it) },
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(thumbColor = VibrantPrimary, activeTrackColor = VibrantPrimary)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Sun Direction Angle:", style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dir X: ${String.format("%.2f", light.direction.x)}", style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
                Slider(
                    value = light.direction.x,
                    onValueChange = { viewModel.updateLightDirection(it, light.direction.y, light.direction.z) },
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(thumbColor = VibrantPrimary, activeTrackColor = VibrantPrimary)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Dir Y: ${String.format("%.2f", light.direction.y)}", style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
                Slider(
                    value = light.direction.y,
                    onValueChange = { viewModel.updateLightDirection(light.direction.x, it, light.direction.z) },
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(thumbColor = VibrantPrimary, activeTrackColor = VibrantPrimary)
                )
            }
        }
    }
}


