package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.StudioToolMode
import com.example.ui.theme.*

@Composable
fun ToolbarModeSelector(
    activeMode: StudioToolMode,
    onSelectMode: (StudioToolMode) -> Unit
) {
    Surface(
        color = VibrantSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().testTag("toolbar_mode_selector")
    ) {
        ScrollableTabRow(
            selectedTabIndex = activeMode.ordinal,
            containerColor = VibrantSurface,
            contentColor = VibrantPrimary,
            edgePadding = 8.dp
        ) {
            ToolTabItem(
                label = "Transform",
                icon = Icons.Default.OpenWith,
                isSelected = activeMode == StudioToolMode.TRANSFORM,
                onClick = { onSelectMode(StudioToolMode.TRANSFORM) }
            )
            ToolTabItem(
                label = "Primitives",
                icon = Icons.Default.Category,
                isSelected = activeMode == StudioToolMode.PRIMITIVES,
                onClick = { onSelectMode(StudioToolMode.PRIMITIVES) }
            )
            ToolTabItem(
                label = "3D Paint",
                icon = Icons.Default.Brush,
                isSelected = activeMode == StudioToolMode.PAINT,
                onClick = { onSelectMode(StudioToolMode.PAINT) }
            )
            ToolTabItem(
                label = "Skeletal Rig",
                icon = Icons.Default.AccessibilityNew,
                isSelected = activeMode == StudioToolMode.SKELETON_RIG,
                onClick = { onSelectMode(StudioToolMode.SKELETON_RIG) }
            )
            ToolTabItem(
                label = "Molecular Compress",
                icon = Icons.Default.Grain,
                isSelected = activeMode == StudioToolMode.MOLECULAR_COMPRESS,
                onClick = { onSelectMode(StudioToolMode.MOLECULAR_COMPRESS) }
            )
            ToolTabItem(
                label = "Light & Material",
                icon = Icons.Default.Lightbulb,
                isSelected = activeMode == StudioToolMode.LIGHT_MATERIAL,
                onClick = { onSelectMode(StudioToolMode.LIGHT_MATERIAL) }
            )
            ToolTabItem(
                label = "Outliner",
                icon = Icons.Default.AccountTree,
                isSelected = activeMode == StudioToolMode.OUTLINER,
                onClick = { onSelectMode(StudioToolMode.OUTLINER) }
            )
        }
    }
}

@Composable
private fun ToolTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Tab(
        selected = isSelected,
        onClick = onClick,
        selectedContentColor = VibrantPrimary,
        unselectedContentColor = VibrantTextSecondary,
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

