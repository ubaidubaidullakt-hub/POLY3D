package com.example.tools

import com.example.model.*
import androidx.compose.ui.graphics.Color
import java.util.ArrayDeque
import kotlin.math.*

enum class BrushType(val displayName: String, val icon: String, val description: String) {
    PENCIL("Pencil", "✏️", "Thin, sharp precision line drawing"),
    PEN("Pen", "✒️", "Smooth opaque outline stroke"),
    BRUSH("Brush", "🖌️", "Standard 3D surface paint brush"),
    WATERCOLOUR("Watercolour", "🎨", "Translucent soft wet-edge blending"),
    WATERMARK("Watermark", "🏷️", "Stamps metallic emblem pattern onto mesh"),
    FILL_BUCKET("Fill Bucket", "🪣", "Flood fills region enclosed by outlines")
}

object Painter3D {

    data class PaintBrush(
        val type: BrushType = BrushType.BRUSH,
        val color: Color = Color(0xFF38BDF8),
        val radius: Float = 0.5f,
        val hardness: Float = 0.8f,
        val metallic: Float = 0.2f,
        val roughness: Float = 0.4f,
        val opacity: Float = 1.0f,
        val watermarkPattern: String = "Star Emblem" // "Star Emblem", "Geometric Grid", "Logo Stamp"
    )

    fun paintAtHitPoint(
        mesh: Mesh3D,
        hit: RaycastHit,
        brush: PaintBrush
    ): Mesh3D {
        return when (brush.type) {
            BrushType.PENCIL -> paintPencil(mesh, hit, brush)
            BrushType.PEN -> paintPen(mesh, hit, brush)
            BrushType.BRUSH -> paintStandardBrush(mesh, hit, brush)
            BrushType.WATERCOLOUR -> paintWatercolour(mesh, hit, brush)
            BrushType.WATERMARK -> paintWatermark(mesh, hit, brush)
            BrushType.FILL_BUCKET -> floodFillMeshRegion(mesh, hit, brush)
        }
    }

    private fun paintPencil(mesh: Mesh3D, hit: RaycastHit, brush: PaintBrush): Mesh3D {
        val hitPoint = hit.hitPoint
        val modelMat = mesh.getModelMatrix()
        val fineRadius = maxOf(0.08f, brush.radius * 0.35f)

        val updatedVertices = mesh.vertices.map { vertex ->
            val worldPos = modelMat.transformVector(vertex.position)
            val dist = worldPos.distance(hitPoint)

            if (dist <= fineRadius) {
                vertex.copy(color = brush.color)
            } else {
                vertex
            }
        }
        return mesh.copy(vertices = updatedVertices)
    }

    private fun paintPen(mesh: Mesh3D, hit: RaycastHit, brush: PaintBrush): Mesh3D {
        val hitPoint = hit.hitPoint
        val modelMat = mesh.getModelMatrix()
        val penRadius = maxOf(0.12f, brush.radius * 0.55f)

        val updatedVertices = mesh.vertices.map { vertex ->
            val worldPos = modelMat.transformVector(vertex.position)
            val dist = worldPos.distance(hitPoint)

            if (dist <= penRadius) {
                val factor = (1f - (dist / penRadius).pow(3f)).coerceIn(0f, 1f)
                val blended = blendColors(vertex.color, brush.color, factor * brush.opacity)
                vertex.copy(color = blended)
            } else {
                vertex
            }
        }
        return mesh.copy(vertices = updatedVertices)
    }

    private fun paintStandardBrush(mesh: Mesh3D, hit: RaycastHit, brush: PaintBrush): Mesh3D {
        val hitPoint = hit.hitPoint
        val modelMat = mesh.getModelMatrix()

        val updatedVertices = mesh.vertices.map { vertex ->
            val worldPos = modelMat.transformVector(vertex.position)
            val dist = worldPos.distance(hitPoint)

            if (dist <= brush.radius) {
                val normalizedDist = dist / brush.radius
                val falloff = if (normalizedDist < brush.hardness) {
                    1f
                } else {
                    1f - ((normalizedDist - brush.hardness) / maxOf(0.01f, 1f - brush.hardness)).coerceIn(0f, 1f)
                }
                val blendedColor = blendColors(vertex.color, brush.color, falloff * brush.opacity)
                vertex.copy(color = blendedColor)
            } else {
                vertex
            }
        }
        return mesh.copy(vertices = updatedVertices)
    }

    private fun paintWatercolour(mesh: Mesh3D, hit: RaycastHit, brush: PaintBrush): Mesh3D {
        val hitPoint = hit.hitPoint
        val modelMat = mesh.getModelMatrix()
        val radius = brush.radius * 1.3f

        val updatedVertices = mesh.vertices.map { vertex ->
            val worldPos = modelMat.transformVector(vertex.position)
            val dist = worldPos.distance(hitPoint)

            if (dist <= radius) {
                val normalizedDist = dist / radius
                val softFalloff = exp(-3f * normalizedDist * normalizedDist) * 0.45f * brush.opacity
                val blendedColor = blendColors(vertex.color, brush.color, softFalloff)
                vertex.copy(color = blendedColor)
            } else {
                vertex
            }
        }
        return mesh.copy(vertices = updatedVertices)
    }

    private fun paintWatermark(mesh: Mesh3D, hit: RaycastHit, brush: PaintBrush): Mesh3D {
        val hitPoint = hit.hitPoint
        val modelMat = mesh.getModelMatrix()
        val radius = maxOf(0.4f, brush.radius * 1.5f)

        val updatedVertices = mesh.vertices.map { vertex ->
            val worldPos = modelMat.transformVector(vertex.position)
            val dist = worldPos.distance(hitPoint)

            if (dist <= radius) {
                val dx = worldPos.x - hitPoint.x
                val dy = worldPos.y - hitPoint.y
                val angle = atan2(dy, dx)
                val normalizedDist = dist / radius

                val inPattern = when (brush.watermarkPattern) {
                    "Star Emblem" -> {
                        val starR = 0.5f + 0.35f * sin(angle * 5f)
                        normalizedDist <= starR
                    }
                    "Geometric Grid" -> {
                        val gridX = abs((dx * 12f) % 1f)
                        val gridY = abs((dy * 12f) % 1f)
                        gridX < 0.25f || gridY < 0.25f || normalizedDist < 0.2f
                    }
                    "Logo Stamp" -> {
                        val ring = (normalizedDist * 8f).toInt() % 2 == 0
                        ring || normalizedDist < 0.18f
                    }
                    else -> normalizedDist <= 0.8f
                }

                if (inPattern) {
                    val stampColor = blendColors(vertex.color, brush.color, 0.9f * brush.opacity)
                    vertex.copy(color = stampColor)
                } else {
                    vertex
                }
            } else {
                vertex
            }
        }
        return mesh.copy(vertices = updatedVertices)
    }

    /**
     * Flood Fill Bucket:
     * Starts from the hit face and propagates to adjacent faces sharing edges.
     * Traversal stops at faces/vertices whose color matches an outline color
     * or differs substantially from the starting region color.
     */
    fun floodFillMeshRegion(mesh: Mesh3D, hit: RaycastHit, brush: PaintBrush): Mesh3D {
        if (mesh.faces.isEmpty() || mesh.vertices.isEmpty()) return mesh
        val startFaceIdx = hit.faceIndex.coerceIn(mesh.faces.indices)
        val startFace = mesh.faces[startFaceIdx]

        // Build adjacency graph for faces sharing edges
        val edgeToFaces = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()
        mesh.faces.forEachIndexed { fIdx, face ->
            val edges = listOf(
                makeEdge(face.v1, face.v2),
                makeEdge(face.v2, face.v3),
                makeEdge(face.v3, face.v1)
            )
            edges.forEach { edge ->
                edgeToFaces.getOrPut(edge) { mutableListOf() }.add(fIdx)
            }
        }

        val faceNeighbors = Array(mesh.faces.size) { mutableSetOf<Int>() }
        edgeToFaces.values.forEach { faceList ->
            if (faceList.size > 1) {
                for (i in 0 until faceList.size) {
                    for (j in i + 1 until faceList.size) {
                        val f1 = faceList[i]
                        val f2 = faceList[j]
                        faceNeighbors[f1].add(f2)
                        faceNeighbors[f2].add(f1)
                    }
                }
            }
        }

        val startV1 = mesh.vertices[startFace.v1.coerceIn(mesh.vertices.indices)].color
        val startV2 = mesh.vertices[startFace.v2.coerceIn(mesh.vertices.indices)].color
        val startV3 = mesh.vertices[startFace.v3.coerceIn(mesh.vertices.indices)].color
        val targetRegionColor = averageColor(startV1, startV2, startV3)

        val fillColor = brush.color
        if (colorDistance(targetRegionColor, fillColor) < 0.05f) {
            return mesh
        }

        val visitedFaces = BooleanArray(mesh.faces.size) { false }
        val filledVertices = BooleanArray(mesh.vertices.size) { false }
        val queue = ArrayDeque<Int>()

        queue.add(startFaceIdx)
        visitedFaces[startFaceIdx] = true

        val maxFilledFaces = 12000
        var filledCount = 0

        while (queue.isNotEmpty() && filledCount < maxFilledFaces) {
            val currFaceIdx = queue.poll() ?: break
            filledCount++
            val currFace = mesh.faces[currFaceIdx]

            filledVertices[currFace.v1.coerceIn(mesh.vertices.indices)] = true
            filledVertices[currFace.v2.coerceIn(mesh.vertices.indices)] = true
            filledVertices[currFace.v3.coerceIn(mesh.vertices.indices)] = true

            for (neighborIdx in faceNeighbors[currFaceIdx]) {
                if (!visitedFaces[neighborIdx]) {
                    val nFace = mesh.faces[neighborIdx]
                    val nv1 = mesh.vertices[nFace.v1.coerceIn(mesh.vertices.indices)].color
                    val nv2 = mesh.vertices[nFace.v2.coerceIn(mesh.vertices.indices)].color
                    val nv3 = mesh.vertices[nFace.v3.coerceIn(mesh.vertices.indices)].color
                    val neighborColor = averageColor(nv1, nv2, nv3)

                    val distFromTarget = colorDistance(neighborColor, targetRegionColor)
                    val isOutlineBoundary = distFromTarget > 0.45f || (colorDistance(neighborColor, fillColor) < 0.08f)

                    if (!isOutlineBoundary) {
                        visitedFaces[neighborIdx] = true
                        queue.add(neighborIdx)
                    }
                }
            }
        }

        val updatedVertices = mesh.vertices.mapIndexed { idx, vertex ->
            if (filledVertices[idx]) {
                vertex.copy(color = fillColor)
            } else {
                vertex
            }
        }

        return mesh.copy(vertices = updatedVertices)
    }

    private fun makeEdge(v1: Int, v2: Int): Pair<Int, Int> {
        return if (v1 < v2) Pair(v1, v2) else Pair(v2, v1)
    }

    private fun colorDistance(c1: Color, c2: Color): Float {
        val dr = c1.red - c2.red
        val dg = c1.green - c2.green
        val db = c1.blue - c2.blue
        return sqrt(dr * dr + dg * dg + db * db)
    }

    private fun averageColor(c1: Color, c2: Color, c3: Color): Color {
        return Color(
            red = (c1.red + c2.red + c3.red) / 3f,
            green = (c1.green + c2.green + c3.green) / 3f,
            blue = (c1.blue + c2.blue + c3.blue) / 3f,
            alpha = 1.0f
        )
    }

    fun applyMaterialToMesh(mesh: Mesh3D, material: Material3D): Mesh3D {
        val updatedVertices = mesh.vertices.map { v ->
            v.copy(color = material.baseColor)
        }
        return mesh.copy(material = material, vertices = updatedVertices)
    }

    private fun blendColors(c1: Color, c2: Color, t: Float): Color {
        val factor = t.coerceIn(0f, 1f)
        return Color(
            red = c1.red + (c2.red - c1.red) * factor,
            green = c1.green + (c2.green - c1.green) * factor,
            blue = c1.blue + (c2.blue - c1.blue) * factor,
            alpha = c1.alpha
        )
    }
}

