package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.RenderMode
import com.example.ui.PolyStudioViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopStudioBar(
    viewModel: PolyStudioViewModel,
    sceneRenderMode: RenderMode,
    onOpenImportExportDialog: () -> Unit,
    onPickLocalFile: (() -> Unit)? = null
) {
    var showCameraMenu by remember { mutableStateOf(false) }
    var showRenderMenu by remember { mutableStateOf(false) }
    var showBgMenu by remember { mutableStateOf(false) }
    val isCameraLocked by viewModel.isCameraLocked.collectAsStateWithLifecycle()
    val isObjectLocked by viewModel.isObjectLocked.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    Surface(
        color = VibrantBg,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().testTag("top_studio_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = VibrantPrimary,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "PolyStudio Logo",
                            tint = VibrantOnPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PolyStudio 3D",
                        style = MaterialTheme.typography.titleMedium,
                        color = VibrantTextPrimary
                    )
                    Text(
                        text = "3D Design & Animation",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantTextSecondary
                    )
                }
            }

            // Central Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Undo Button
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = canUndo,
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo Action",
                        tint = if (canUndo) VibrantPrimary else VibrantTextSecondary.copy(alpha = 0.35f)
                    )
                }

                // Redo Button
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = canRedo,
                    modifier = Modifier.testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo Action",
                        tint = if (canRedo) VibrantPrimary else VibrantTextSecondary.copy(alpha = 0.35f)
                    )
                }

                // Pick Local File Button (.glb, .fbx, .obj)
                if (onPickLocalFile != null) {
                    IconButton(
                        onClick = onPickLocalFile,
                        modifier = Modifier.testTag("open_local_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Open Local .glb / .fbx / .obj File",
                            tint = VibrantPrimary
                        )
                    }
                }

                // File Import/Export Dialog Button
                IconButton(
                    onClick = onOpenImportExportDialog,
                    modifier = Modifier.testTag("import_export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Import / Export Models",
                        tint = VibrantTextPrimary
                    )
                }

                // Viewport Lock Toggle Button
                IconButton(
                    onClick = { viewModel.toggleCameraLock() },
                    modifier = Modifier.testTag("viewport_lock_button")
                ) {
                    Icon(
                        imageVector = if (isCameraLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isCameraLocked) "Camera Locked" else "Camera Unlocked",
                        tint = if (isCameraLocked) VibrantAccentPink else VibrantTextSecondary
                    )
                }

                // Camera Preset Views Menu
                Box {
                    IconButton(
                        onClick = { showCameraMenu = true },
                        modifier = Modifier.testTag("camera_view_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Camera View Presets",
                            tint = VibrantTextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showCameraMenu,
                        onDismissRequest = { showCameraMenu = false },
                        modifier = Modifier.background(VibrantSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Isometric 3D", color = VibrantTextPrimary) },
                            onClick = { viewModel.setPresetCameraView("Isometric"); showCameraMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Front View", color = VibrantTextPrimary) },
                            onClick = { viewModel.setPresetCameraView("Front"); showCameraMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Top View", color = VibrantTextPrimary) },
                            onClick = { viewModel.setPresetCameraView("Top"); showCameraMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Right View", color = VibrantTextPrimary) },
                            onClick = { viewModel.setPresetCameraView("Right"); showCameraMenu = false }
                        )
                    }
                }

                // Viewport Background Color Menu
                Box {
                    IconButton(
                        onClick = { showBgMenu = true },
                        modifier = Modifier.testTag("bg_color_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatColorFill,
                            contentDescription = "Viewport Background Color",
                            tint = VibrantAccentPink
                        )
                    }
                    DropdownMenu(
                        expanded = showBgMenu,
                        onDismissRequest = { showBgMenu = false },
                        modifier = Modifier.background(VibrantSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("🌙 Dark Slate", color = VibrantTextPrimary) },
                            onClick = { viewModel.setBackgroundColor(Color(0xFF0F172A)); showBgMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("☀️ Studio White", color = VibrantTextPrimary) },
                            onClick = { viewModel.setBackgroundColor(Color(0xFFF8FAFC)); showBgMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("🖤 OLED Black", color = VibrantTextPrimary) },
                            onClick = { viewModel.setBackgroundColor(Color(0xFF000000)); showBgMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("🪙 Neutral Gray", color = VibrantTextPrimary) },
                            onClick = { viewModel.setBackgroundColor(Color(0xFF262626)); showBgMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("📘 Blueprint Blue", color = VibrantTextPrimary) },
                            onClick = { viewModel.setBackgroundColor(Color(0xFF0C4A6E)); showBgMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("🌆 Cyber Sunset", color = VibrantTextPrimary) },
                            onClick = { viewModel.setBackgroundColor(Color(0xFF31103F)); showBgMenu = false }
                        )
                    }
                }

                // Render Mode Switcher Menu
                Box {
                    IconButton(
                        onClick = { showRenderMenu = true },
                        modifier = Modifier.testTag("render_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Render Mode",
                            tint = VibrantPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showRenderMenu,
                        onDismissRequest = { showRenderMenu = false },
                        modifier = Modifier.background(VibrantSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("PBR Shaded Lit", color = VibrantTextPrimary) },
                            onClick = { viewModel.setRenderMode(RenderMode.PBR_LIT); showRenderMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Wireframe Grid", color = VibrantTextPrimary) },
                            onClick = { viewModel.setRenderMode(RenderMode.WIREFRAME); showRenderMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Skeleton Bones", color = VibrantTextPrimary) },
                            onClick = { viewModel.setRenderMode(RenderMode.SKELETON_BONES); showRenderMenu = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Quick Save Project Button
                Button(
                    onClick = { viewModel.saveCurrentProjectToDb() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantPrimary,
                        contentColor = VibrantOnPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("save_project_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EXPORT", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

