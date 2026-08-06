package com.example.model

import androidx.compose.ui.graphics.Color

data class MolecularCluster(
    val partId: Int,
    var centroid: Vector3,
    val vertexIndices: MutableList<Int> = mutableListOf(),
    val faceIndices: MutableList<Int> = mutableListOf(),
    val color: Color = Color.Unspecified
)

data class CompressionOptions(
    val targetMolecularParts: Int = 200, // User-defined target polygon / part count
    val decimationStrength: Float = 0.5f,
    val preserveBoundaries: Boolean = true,
    val showHeatmapPreview: Boolean = false
)

data class CompressionReport(
    val originalPolyCount: Int,
    val compressedPolyCount: Int,
    val originalVertexCount: Int,
    val compressedVertexCount: Int,
    val reductionPercentage: Float,
    val originalSizeKb: Int,
    val compressedSizeKb: Int,
    val molecularPartsCreated: Int,
    val fidelityScore: Float
)

object PolygonReductionEngine {

    private data class VoxelKey(val x: Int, val y: Int, val z: Int)

    fun compressByMolecularParts(
        mesh: Mesh3D,
        options: CompressionOptions
    ): Pair<Mesh3D, CompressionReport> {
        val originalVertices = mesh.vertices
        val originalFaces = mesh.faces
        if (originalVertices.isEmpty() || originalFaces.isEmpty()) {
            return Pair(mesh, CompressionReport(0, 0, 0, 0, 0f, 0, 0, 0, 100f))
        }

        val origPoly = originalFaces.size
        val targetCount = options.targetMolecularParts.coerceAtLeast(10)

        // If requested target count is equal to or greater than current faces, retain original mesh
        if (targetCount >= origPoly) {
            val origSize = maxOf(1, (originalVertices.size * 32 + originalFaces.size * 12) / 1024)
            val report = CompressionReport(
                originalPolyCount = origPoly,
                compressedPolyCount = origPoly,
                originalVertexCount = originalVertices.size,
                compressedVertexCount = originalVertices.size,
                reductionPercentage = 0f,
                originalSizeKb = origSize,
                compressedSizeKb = origSize,
                molecularPartsCreated = originalVertices.size,
                fidelityScore = 100f
            )
            return Pair(mesh, report)
        }

        // Calculate 3D bounding box to cover full model volume
        val bbox = mesh.calculateBoundingBox()
        val extX = maxOf(0.001f, bbox.max.x - bbox.min.x)
        val extY = maxOf(0.001f, bbox.max.y - bbox.min.y)
        val extZ = maxOf(0.001f, bbox.max.z - bbox.min.z)

        // Calculate 3D grid resolution Nx, Ny, Nz proportional to model aspect ratio
        val maxExt = maxOf(extX, maxOf(extY, extZ))
        val aspectX = extX / maxExt
        val aspectY = extY / maxExt
        val aspectZ = extZ / maxExt

        // Estimate grid resolution factor base to achieve roughly targetCount polygons
        val baseRes = Math.sqrt(targetCount.toDouble() * 1.6).toFloat().coerceIn(3f, 150f)
        val Nx = maxOf(2, (baseRes * aspectX).toInt())
        val Ny = maxOf(2, (baseRes * aspectY).toInt())
        val Nz = maxOf(2, (baseRes * aspectZ).toInt())

        val cellDx = extX / Nx
        val cellDy = extY / Ny
        val cellDz = extZ / Nz

        // Step 1: Partition original vertices into 3D spatial voxel cells
        val voxelMap = mutableMapOf<VoxelKey, MutableList<Int>>()
        val vertexVoxelKeys = Array(originalVertices.size) { i ->
            val pos = originalVertices[i].position
            val gx = ((pos.x - bbox.min.x) / cellDx).toInt().coerceIn(0, Nx - 1)
            val gy = ((pos.y - bbox.min.y) / cellDy).toInt().coerceIn(0, Ny - 1)
            val gz = ((pos.z - bbox.min.z) / cellDz).toInt().coerceIn(0, Nz - 1)
            val key = VoxelKey(gx, gy, gz)
            voxelMap.getOrPut(key) { mutableListOf() }.add(i)
            key
        }

        // Step 2: Compute representative vertex for each occupied 3D spatial voxel cell
        val newVertices = mutableListOf<Vertex>()
        val voxelToNewIndexMap = mutableMapOf<VoxelKey, Int>()

        var cellCounter = 0
        for ((key, vIndices) in voxelMap) {
            if (vIndices.isEmpty()) continue

            var posSum = Vector3.Zero
            var normSum = Vector3.Zero
            for (vIdx in vIndices) {
                posSum += originalVertices[vIdx].position
                normSum += originalVertices[vIdx].normal
            }
            val avgPos = posSum / vIndices.size.toFloat()
            val avgNorm = if (normSum.lengthSquared() > 0.0001f) normSum.normalized() else Vector3.Up

            val hue = (cellCounter * 137.5f) % 360f // Golden ratio angle color distribution
            val cellColor = if (options.showHeatmapPreview) Color.hsv(hue, 0.8f, 0.95f) else mesh.material.baseColor
            cellCounter++

            newVertices.add(
                Vertex(
                    position = avgPos,
                    normal = avgNorm,
                    color = cellColor
                )
            )
            voxelToNewIndexMap[key] = newVertices.size - 1
        }

        // Step 3: Re-index Faces to newly collapsed 3D voxel cell vertices
        val newFaces = mutableListOf<Face>()
        val visitedSignatures = mutableSetOf<Triple<Int, Int, Int>>()

        for (face in originalFaces) {
            val k1 = vertexVoxelKeys[face.v1.coerceIn(originalVertices.indices)]
            val k2 = vertexVoxelKeys[face.v2.coerceIn(originalVertices.indices)]
            val k3 = vertexVoxelKeys[face.v3.coerceIn(originalVertices.indices)]

            val nv1 = voxelToNewIndexMap[k1] ?: continue
            val nv2 = voxelToNewIndexMap[k2] ?: continue
            val nv3 = voxelToNewIndexMap[k3] ?: continue

            // Form simplified larger triangle if all 3 vertices belong to different 3D voxel cells
            if (nv1 != nv2 && nv2 != nv3 && nv1 != nv3) {
                val sorted = listOf(nv1, nv2, nv3).sorted()
                val signature = Triple(sorted[0], sorted[1], sorted[2])

                if (!visitedSignatures.contains(signature)) {
                    visitedSignatures.add(signature)
                    val p1 = newVertices[nv1].position
                    val p2 = newVertices[nv2].position
                    val p3 = newVertices[nv3].position

                    var norm = (p2 - p1).cross(p3 - p1)
                    norm = if (norm.lengthSquared() > 0.00001f) norm.normalized() else Vector3.Up

                    newFaces.add(
                        Face(
                            v1 = nv1,
                            v2 = nv2,
                            v3 = nv3,
                            normal = norm
                        )
                    )
                }
            }
        }

        // Fallback: If grid was coarse, construct convex hull triangle fan from new vertices
        if (newFaces.isEmpty() && newVertices.size >= 3) {
            for (i in 0 until newVertices.size - 2) {
                newFaces.add(Face(0, i + 1, i + 2, Vector3.Up))
            }
        }

        val compPoly = maxOf(1, newFaces.size)
        val reduction = ((origPoly - compPoly).toFloat() / maxOf(1, origPoly) * 100f).coerceIn(0f, 99.9f)

        val origSizeKb = maxOf(1, (originalVertices.size * 32 + originalFaces.size * 12) / 1024)
        val compSizeKb = maxOf(1, (newVertices.size * 32 + newFaces.size * 12) / 1024)

        val fidelity = (100f - (reduction * 0.35f)).coerceIn(25f, 99.9f)

        val compressedMesh = mesh.copy(
            id = "${mesh.id}_decimated",
            name = "${mesh.name} (${compPoly} Polys)",
            vertices = newVertices,
            faces = newFaces,
            molecularPartId = targetCount
        )

        val report = CompressionReport(
            originalPolyCount = origPoly,
            compressedPolyCount = compPoly,
            originalVertexCount = originalVertices.size,
            compressedVertexCount = newVertices.size,
            reductionPercentage = reduction,
            originalSizeKb = origSizeKb,
            compressedSizeKb = compSizeKb,
            molecularPartsCreated = newVertices.size,
            fidelityScore = fidelity
        )

        return Pair(compressedMesh, report)
    }
}

