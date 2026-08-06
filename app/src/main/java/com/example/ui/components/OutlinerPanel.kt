package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.Mesh3D
import com.example.ui.PolyStudioViewModel
import com.example.ui.theme.*

@Composable
fun OutlinerPanel(
    viewModel: PolyStudioViewModel,
    meshes: List<Mesh3D>,
    selectedMeshId: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .padding(16.dp)
            .testTag("outliner_panel")
    ) {
        Text("Scene Outliner & Node Tree", style = MaterialTheme.typography.titleMedium, color = VibrantTextPrimary)
        Text("Manage meshes, duplicate nodes, or clear selection", style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(meshes) { mesh ->
                val isSelected = mesh.id == selectedMeshId
                Card(
                    onClick = { viewModel.selectMesh(mesh.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) VibrantActive else VibrantSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = if (isSelected) VibrantHighlight else VibrantPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mesh.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) VibrantHighlight else VibrantTextPrimary
                            )
                            Text(
                                text = "${mesh.faces.size} Polys  •  ${mesh.vertices.size} Verts",
                                style = MaterialTheme.typography.labelSmall,
                                color = VibrantTextSecondary
                            )
                        }

                        // Duplicate Button
                        IconButton(onClick = { viewModel.duplicateMesh(mesh.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Duplicate",
                                tint = if (isSelected) VibrantHighlight else VibrantTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Delete Button
                        IconButton(onClick = { viewModel.deleteMesh(mesh.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = VibrantAccentRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

