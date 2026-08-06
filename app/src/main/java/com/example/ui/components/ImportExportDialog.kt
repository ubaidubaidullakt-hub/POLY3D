package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.PolyStudioViewModel
import com.example.ui.theme.*

@Composable
fun ImportExportDialog(
    viewModel: PolyStudioViewModel,
    onDismiss: () -> Unit,
    onPickLocalFile: ((appendToScene: Boolean) -> Unit)? = null
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Presets, 1 = Export

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VibrantBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("3D Model Import & Export", color = VibrantTextPrimary, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = VibrantTextPrimary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = VibrantSurface,
                    contentColor = VibrantPrimary
                ) {
                    Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                        Text("Sample Presets & Import", color = if (activeTab == 0) VibrantPrimary else VibrantTextSecondary, modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                        Text("Export Formats", color = if (activeTab == 1) VibrantPrimary else VibrantTextSecondary, modifier = Modifier.padding(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == 0) {
                    if (onPickLocalFile != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VibrantSurfaceVariant),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Import Model From Local Storage", style = MaterialTheme.typography.titleSmall, color = VibrantPrimary)
                                Text("Select .glb, .gltf, .fbx, or .obj files stored on your device", style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            onPickLocalFile(false)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimary, contentColor = VibrantOnPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open File", style = MaterialTheme.typography.labelSmall)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onPickLocalFile(true)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VibrantPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add to Scene", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    Text("Or Load Built-In Sample Presets:", style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    val presets = listOf(
                        "Sci-Fi Mech" to "Animated Robot Skeleton & Joint Hierarchy",
                        "Hemoglobin Molecular Protein" to "Complex Molecular Cluster for Polygon Compression",
                        "Low-Poly Sports Car" to "Multi-mesh Automotive Model",
                        "Geometric Prism" to "Glass & Metallic Sculptures"
                    )

                    presets.forEach { (title, desc) ->
                        Card(
                            onClick = {
                                viewModel.loadSamplePreset(title)
                                onDismiss()
                            },
                            colors = CardDefaults.cardColors(containerColor = VibrantSurfaceVariant),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(title, style = MaterialTheme.typography.titleSmall, color = VibrantPrimary)
                                Text(desc, style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)
                            }
                        }
                    }
                } else {
                    Text("Export Current 3D Scene To Format:", style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val glb = viewModel.exportGlbBytes()
                            viewModel.saveCurrentProjectToDb()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimary, contentColor = VibrantOnPrimary),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Binary GLB (.glb)")
                    }

                    Button(
                        onClick = {
                            val fbx = viewModel.exportFbxString()
                            viewModel.saveCurrentProjectToDb()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantActive, contentColor = VibrantHighlight),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export FBX Document (.fbx)")
                    }

                    Button(
                        onClick = {
                            val obj = viewModel.exportObjString()
                            viewModel.saveCurrentProjectToDb()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimaryContainer, contentColor = VibrantOnPrimaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Wavefront OBJ (.obj)")
                    }
                }
            }
        },
        confirmButton = {}
    )
}

