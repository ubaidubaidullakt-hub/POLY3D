package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Mesh3D
import com.example.model.Vector3
import com.example.ui.PolyStudioViewModel
import com.example.ui.theme.*

@Composable
fun TransformPanel(
    viewModel: PolyStudioViewModel,
    selectedMesh: Mesh3D?
) {
    val isObjectLocked by viewModel.isObjectLocked.collectAsStateWithLifecycle()
    if (selectedMesh == null) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No 3D object selected. Tap an object in the viewport or select from Outliner.", color = VibrantTextSecondary)
        }
        return
    }

    val bbox = selectedMesh.calculateBoundingBox()
    var isUniformScale by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("transform_panel")
    ) {
        // Object Lock Toggle Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isObjectLocked) VibrantPrimaryContainer else VibrantSurfaceVariant
            ),
            onClick = { viewModel.toggleObjectLock() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isObjectLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isObjectLocked) VibrantAccentPink else VibrantPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isObjectLocked) "🔒 Mesh Transform Locked" else "🔓 Mesh Movable & Scalable",
                        style = MaterialTheme.typography.titleSmall,
                        color = VibrantTextPrimary
                    )
                    Text(
                        text = if (isObjectLocked) "Mesh position fixed; protects from accidental movement" else "Tap to lock object transform coordinates",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantTextSecondary
                    )
                }
                Switch(
                    checked = isObjectLocked,
                    onCheckedChange = { viewModel.setObjectLock(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VibrantAccentPink,
                        checkedTrackColor = VibrantPrimary
                    )
                )
            }
        }

        Text(
            text = "Transform & Resizing: ${selectedMesh.name}",
            style = MaterialTheme.typography.titleMedium,
            color = VibrantTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Position Sliders (X, Y, Z)
        Text("Position (X, Y, Z)", style = MaterialTheme.typography.labelSmall, color = VibrantPrimary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AxisSlider(
                label = "X: ${String.format("%.2f", selectedMesh.position.x)}",
                value = selectedMesh.position.x,
                range = -10f..10f,
                color = VibrantAccentRed,
                modifier = Modifier.weight(1f)
            ) { newX -> viewModel.updateSelectedMeshPosition(selectedMesh.position.copy(x = newX)) }

            AxisSlider(
                label = "Y: ${String.format("%.2f", selectedMesh.position.y)}",
                value = selectedMesh.position.y,
                range = -10f..10f,
                color = VibrantAccentGreen,
                modifier = Modifier.weight(1f)
            ) { newY -> viewModel.updateSelectedMeshPosition(selectedMesh.position.copy(y = newY)) }

            AxisSlider(
                label = "Z: ${String.format("%.2f", selectedMesh.position.z)}",
                value = selectedMesh.position.z,
                range = -10f..10f,
                color = VibrantPrimary,
                modifier = Modifier.weight(1f)
            ) { newZ -> viewModel.updateSelectedMeshPosition(selectedMesh.position.copy(z = newZ)) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rotation Sliders (X, Y, Z Degrees)
        Text("Rotation (Degrees)", style = MaterialTheme.typography.labelSmall, color = VibrantAccentPink)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AxisSlider(
                label = "Rx: ${selectedMesh.rotation.x.toInt()}°",
                value = selectedMesh.rotation.x,
                range = -180f..180f,
                color = VibrantAccentRed,
                modifier = Modifier.weight(1f)
            ) { newRx -> viewModel.updateSelectedMeshRotation(selectedMesh.rotation.copy(x = newRx)) }

            AxisSlider(
                label = "Ry: ${selectedMesh.rotation.y.toInt()}°",
                value = selectedMesh.rotation.y,
                range = -180f..180f,
                color = VibrantAccentGreen,
                modifier = Modifier.weight(1f)
            ) { newRy -> viewModel.updateSelectedMeshRotation(selectedMesh.rotation.copy(y = newRy)) }

            AxisSlider(
                label = "Rz: ${selectedMesh.rotation.z.toInt()}°",
                value = selectedMesh.rotation.z,
                range = -180f..180f,
                color = VibrantPrimary,
                modifier = Modifier.weight(1f)
            ) { newRz -> viewModel.updateSelectedMeshRotation(selectedMesh.rotation.copy(z = newRz)) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scale Sliders & Resizing Dimensions
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scale Multiplier", style = MaterialTheme.typography.labelSmall, color = VibrantPrimary)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { isUniformScale = !isUniformScale }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (isUniformScale) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock Uniform Scale",
                    tint = if (isUniformScale) VibrantPrimary else VibrantTextSecondary
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AxisSlider(
                label = "Sx: ${String.format("%.2f", selectedMesh.scale.x)}",
                value = selectedMesh.scale.x,
                range = 0.1f..5f,
                color = VibrantAccentRed,
                modifier = Modifier.weight(1f)
            ) { newSx ->
                if (isUniformScale) viewModel.updateSelectedMeshScale(Vector3(newSx, newSx, newSx))
                else viewModel.updateSelectedMeshScale(selectedMesh.scale.copy(x = newSx))
            }

            AxisSlider(
                label = "Sy: ${String.format("%.2f", selectedMesh.scale.y)}",
                value = selectedMesh.scale.y,
                range = 0.1f..5f,
                color = VibrantAccentGreen,
                modifier = Modifier.weight(1f)
            ) { newSy ->
                if (isUniformScale) viewModel.updateSelectedMeshScale(Vector3(newSy, newSy, newSy))
                else viewModel.updateSelectedMeshScale(selectedMesh.scale.copy(y = newSy))
            }

            AxisSlider(
                label = "Sz: ${String.format("%.2f", selectedMesh.scale.z)}",
                value = selectedMesh.scale.z,
                range = 0.1f..5f,
                color = VibrantPrimary,
                modifier = Modifier.weight(1f)
            ) { newSz ->
                if (isUniformScale) viewModel.updateSelectedMeshScale(Vector3(newSz, newSz, newSz))
                else viewModel.updateSelectedMeshScale(selectedMesh.scale.copy(z = newSz))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Precision Bounding Box Dimensions (Width, Height, Depth)
        Card(
            colors = CardDefaults.cardColors(containerColor = VibrantSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Real Dimensions:", style = MaterialTheme.typography.bodySmall, color = VibrantTextSecondary)
                Text(
                    "W: ${String.format("%.2f", bbox.width)}m  H: ${String.format("%.2f", bbox.height)}m  D: ${String.format("%.2f", bbox.depth)}m",
                    style = MaterialTheme.typography.labelMedium,
                    color = VibrantPrimary
                )
            }
        }
    }
}

@Composable
private fun AxisSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
    }
}

