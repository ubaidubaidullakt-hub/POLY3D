package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.PolyStudioViewModel
import com.example.ui.theme.*

@Composable
fun PrimitivesPanel(
    viewModel: PolyStudioViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("primitives_panel")
    ) {
        Text(
            text = "Create Geometric 3D Primitives",
            style = MaterialTheme.typography.titleMedium,
            color = VibrantTextPrimary
        )
        Text(
            text = "Select a parametric shape to insert into your workspace",
            style = MaterialTheme.typography.labelSmall,
            color = VibrantTextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimitiveCard("Cube", Icons.Default.CropSquare, VibrantPrimary, Modifier.weight(1f)) { viewModel.addPrimitive("Cube") }
            PrimitiveCard("Sphere", Icons.Default.RadioButtonUnchecked, VibrantAccentPink, Modifier.weight(1f)) { viewModel.addPrimitive("Sphere") }
            PrimitiveCard("Cylinder", Icons.Default.ViewAgenda, VibrantAccentGreen, Modifier.weight(1f)) { viewModel.addPrimitive("Cylinder") }
            PrimitiveCard("Torus", Icons.Default.PanoramaFishEye, VibrantPrimary, Modifier.weight(1f)) { viewModel.addPrimitive("Torus") }
            PrimitiveCard("Teapot", Icons.Default.Coffee, VibrantPrimary, Modifier.weight(1f)) { viewModel.addPrimitive("Teapot") }
            PrimitiveCard("Molecular", Icons.Default.Grain, VibrantAccentRed, Modifier.weight(1f)) { viewModel.addPrimitive("Molecular Cluster") }
        }
    }
}

@Composable
private fun PrimitiveCard(
    name: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = VibrantSurfaceVariant),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = name, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = name, style = MaterialTheme.typography.labelSmall, color = VibrantTextPrimary)
        }
    }
}

