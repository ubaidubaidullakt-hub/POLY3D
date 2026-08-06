package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.GizmoRenderer
import com.example.engine.Raycaster
import com.example.engine.SoftwareRenderer
import com.example.model.*
import com.example.ui.PolyStudioViewModel
import com.example.ui.StudioToolMode

import com.example.ui.theme.*

@Composable
fun Viewport3D(
    viewModel: PolyStudioViewModel,
    scene: Scene3D,
    selectedMeshId: String?,
    toolMode: StudioToolMode,
    compressedMesh: Mesh3D?,
    compressionReport: CompressionReport?,
    isComparisonActive: Boolean,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val selectedMesh = scene.meshes.find { it.id == selectedMeshId } ?: scene.meshes.firstOrNull()

    val isCameraLocked by viewModel.isCameraLocked.collectAsStateWithLifecycle()
    val isObjectLocked by viewModel.isObjectLocked.collectAsStateWithLifecycle()

    val totalPolyCount = scene.meshes.sumOf { it.faces.size }
    val totalVertexCount = scene.meshes.sumOf { it.vertices.size }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scene.backgroundColor)
            .testTag("viewport_3d")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isCameraLocked) {
                    if (!isCameraLocked) {
                        detectTransformGestures { centroid, pan, zoom, rotation ->
                            if (zoom != 1.0f) {
                                viewModel.updateCameraZoom(1f / zoom)
                            }
                        }
                    }
                }
                .pointerInput(selectedMeshId, toolMode, isCameraLocked, isObjectLocked) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val touchOffset = change.position
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()

                                // 1. Gizmo interaction if in transform mode & object not locked
                                if (selectedMesh != null && !isObjectLocked && scene.gizmoMode != GizmoMode.NONE && toolMode == StudioToolMode.TRANSFORM) {
                                    val hitAxis = GizmoRenderer.detectGizmoAxisHit(touchOffset, selectedMesh, scene, w, h)
                                    if (hitAxis != com.example.engine.GizmoAxis.NONE) {
                                        change.consume()
                                        when (hitAxis) {
                                            com.example.engine.GizmoAxis.X -> viewModel.updateSelectedMeshPosition(selectedMesh.position + Vector3(0.2f, 0f, 0f))
                                            com.example.engine.GizmoAxis.Y -> viewModel.updateSelectedMeshPosition(selectedMesh.position + Vector3(0f, 0.2f, 0f))
                                            com.example.engine.GizmoAxis.Z -> viewModel.updateSelectedMeshPosition(selectedMesh.position + Vector3(0f, 0f, 0.2f))
                                            com.example.engine.GizmoAxis.CENTER -> viewModel.updateSelectedMeshPosition(Vector3.Zero)
                                            else -> {}
                                        }
                                        continue
                                    }
                                }

                                // 2. Raycast Scene for hit
                                val ray = Raycaster.screenPointToRay(touchOffset, scene.camera, w, h)
                                val hit = Raycaster.raycastScene(ray, scene.meshes)

                                if (hit != null) {
                                    if (toolMode == StudioToolMode.PAINT) {
                                        change.consume()
                                        viewModel.paintMeshAtHit(hit)
                                    } else if (!change.previousPressed && !isObjectLocked) {
                                        viewModel.selectMesh(hit.meshId)
                                    }
                                } else if (!isCameraLocked && change.previousPressed) {
                                    val dragX = change.position.x - change.previousPosition.x
                                    val dragY = change.position.y - change.previousPosition.y
                                    if (dragX != 0f || dragY != 0f) {
                                        viewModel.updateCameraOrbit(-dragX * 0.4f, dragY * 0.4f)
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            canvasSize = size

            if (isComparisonActive && compressedMesh != null) {
                val halfWidth = size.width / 2f
                val leftScene = scene.copy(meshes = listOfNotNull(selectedMesh))
                SoftwareRenderer.renderScene(this, leftScene, halfWidth, size.height, selectedMeshId)

                val rightScene = scene.copy(meshes = listOf(compressedMesh))
                SoftwareRenderer.renderScene(this, rightScene, size.width, size.height, compressedMesh.id)

                drawLine(VibrantPrimary, Offset(halfWidth, 0f), Offset(halfWidth, size.height), strokeWidth = 3f)
            } else {
                SoftwareRenderer.renderScene(this, scene, size.width, size.height, selectedMeshId)

                if (selectedMesh != null && scene.gizmoMode != GizmoMode.NONE && toolMode == StudioToolMode.TRANSFORM && !isObjectLocked) {
                    GizmoRenderer.renderGizmo(this, selectedMesh, scene, size.width, size.height)
                }
            }
        }

        // Lock Status Overlay Bar (Top Center)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = isCameraLocked,
                onClick = { viewModel.toggleCameraLock() },
                label = { Text(if (isCameraLocked) "🔒 Camera Viewport Locked" else "🔓 Camera Free", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VibrantAccentPink,
                    selectedLabelColor = Color.White,
                    containerColor = VibrantSurface.copy(alpha = 0.85f),
                    labelColor = VibrantTextPrimary
                )
            )

            if (isObjectLocked) {
                FilterChip(
                    selected = true,
                    onClick = { viewModel.toggleObjectLock() },
                    label = { Text("🔒 Mesh Locked", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibrantPrimaryContainer,
                        selectedLabelColor = VibrantPrimary
                    )
                )
            }
        }

        // Overlay: 3D Scene Statistics Card (Top Left)
        Card(
            colors = CardDefaults.cardColors(containerColor = VibrantSurface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = selectedMesh?.name ?: "Full 3D Scene",
                    style = MaterialTheme.typography.titleSmall,
                    color = VibrantTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Polygons: $totalPolyCount  |  Vertices: $totalVertexCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibrantPrimary
                )

                if (selectedMesh != null) {
                    val bbox = selectedMesh.calculateBoundingBox()
                    Text(
                        text = "Dimensions: ${String.format("%.2f", bbox.width)}m × ${String.format("%.2f", bbox.height)}m × ${String.format("%.2f", bbox.depth)}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantTextSecondary
                    )
                }

                if (compressionReport != null && toolMode == StudioToolMode.MOLECULAR_COMPRESS) {
                    Text(
                        text = "Molecular Parts: ${compressionReport.molecularPartsCreated}  (-${compressionReport.reductionPercentage.toInt()}% Poly)",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantAccentGreen
                    )
                }
            }
        }

        // Overlay: Quick Gizmo Mode Buttons (Top Right)
        if (toolMode == StudioToolMode.TRANSFORM) {
            Surface(
                color = VibrantSurface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    IconButton(
                        onClick = { viewModel.setGizmoMode(GizmoMode.TRANSLATE) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (scene.gizmoMode == GizmoMode.TRANSLATE) VibrantPrimary else Color.Transparent
                        )
                    ) {
                        Text("Move", color = if (scene.gizmoMode == GizmoMode.TRANSLATE) VibrantOnPrimary else VibrantTextPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(
                        onClick = { viewModel.setGizmoMode(GizmoMode.ROTATE) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (scene.gizmoMode == GizmoMode.ROTATE) VibrantPrimary else Color.Transparent
                        )
                    ) {
                        Text("Rot", color = if (scene.gizmoMode == GizmoMode.ROTATE) VibrantOnPrimary else VibrantTextPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(
                        onClick = { viewModel.setGizmoMode(GizmoMode.SCALE) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (scene.gizmoMode == GizmoMode.SCALE) VibrantPrimary else Color.Transparent
                        )
                    ) {
                        Text("Scale", color = if (scene.gizmoMode == GizmoMode.SCALE) VibrantOnPrimary else VibrantTextPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
