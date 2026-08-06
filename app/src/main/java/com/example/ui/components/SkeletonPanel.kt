package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.model.Skeleton
import com.example.model.Vector3
import com.example.ui.PolyStudioViewModel
import com.example.ui.theme.*

@Composable
fun SkeletonPanel(
    viewModel: PolyStudioViewModel,
    skeleton: Skeleton,
    selectedBoneId: Int?,
    isPlayingAnimation: Boolean
) {
    val selectedBone = skeleton.bones.find { it.id == selectedBoneId } ?: skeleton.bones.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("skeleton_panel")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Skeletal Rigging & Animation", style = MaterialTheme.typography.titleMedium, color = VibrantTextPrimary)
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.addBoneToSkeleton() },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimary, contentColor = VibrantOnPrimary),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bone", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Bone", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { viewModel.autoSkinMeshToSkeleton() },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantActive, contentColor = VibrantHighlight),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Auto-Skin", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bone Joints List
        Text("Bone Joints Tree:", style = MaterialTheme.typography.labelSmall, color = VibrantPrimary)
        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(skeleton.bones) { bone ->
                FilterChip(
                    selected = bone.id == selectedBoneId,
                    onClick = { viewModel.selectBone(bone.id) },
                    label = { Text(bone.name, color = if (bone.id == selectedBoneId) VibrantOnPrimary else VibrantTextPrimary) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibrantPrimary,
                        containerColor = VibrantSurfaceVariant
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Accessibility, contentDescription = null, tint = if (bone.id == selectedBoneId) VibrantOnPrimary else VibrantPrimary, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedBone != null) {
            Text("Rotate Joint: ${selectedBone.name}", style = MaterialTheme.typography.labelSmall, color = VibrantPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rx: ${selectedBone.rotation.x.toInt()}°", style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
                    Slider(
                        value = selectedBone.rotation.x,
                        onValueChange = { viewModel.updateSelectedBoneRotation(selectedBone.rotation.copy(x = it)) },
                        colors = SliderDefaults.colors(thumbColor = VibrantPrimary, activeTrackColor = VibrantPrimary),
                        valueRange = -180f..180f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ry: ${selectedBone.rotation.y.toInt()}°", style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
                    Slider(
                        value = selectedBone.rotation.y,
                        onValueChange = { viewModel.updateSelectedBoneRotation(selectedBone.rotation.copy(y = it)) },
                        colors = SliderDefaults.colors(thumbColor = VibrantPrimary, activeTrackColor = VibrantPrimary),
                        valueRange = -180f..180f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rz: ${selectedBone.rotation.z.toInt()}°", style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
                    Slider(
                        value = selectedBone.rotation.z,
                        onValueChange = { viewModel.updateSelectedBoneRotation(selectedBone.rotation.copy(z = it)) },
                        colors = SliderDefaults.colors(thumbColor = VibrantPrimary, activeTrackColor = VibrantPrimary),
                        valueRange = -180f..180f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Animation Timeline Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = VibrantSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.toggleAnimationPlay() }) {
                    Icon(
                        imageVector = if (isPlayingAnimation) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause Animation",
                        tint = VibrantPrimary
                    )
                }
                Text("Animation Timeline (Looping)", style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
                Text("30 FPS", style = MaterialTheme.typography.labelSmall, color = VibrantTextSecondary)
            }
        }
    }
}

