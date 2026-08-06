package com.example.engine

import com.example.model.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

object SoftwareRenderer {

    data class ProjectedVertex(
        val screenPos: Offset,
        val depthZ: Float,
        val worldPos: Vector3,
        val worldNormal: Vector3,
        val color: Color
    )

    data class RenderPolygon(
        val p1: ProjectedVertex,
        val p2: ProjectedVertex,
        val p3: ProjectedVertex,
        val avgDepth: Float,
        val faceNormal: Vector3,
        val material: Material3D,
        val isSelected: Boolean
    )

    fun renderScene(
        drawScope: DrawScope,
        scene: Scene3D,
        canvasWidth: Float,
        canvasHeight: Float,
        selectedMeshId: String?
    ) {
        val camera = scene.camera
        val aspectRatio = if (canvasHeight > 0f) canvasWidth / canvasHeight else 1.0f
        val viewMatrix = camera.getViewMatrix()
        val projMatrix = camera.getProjectionMatrix(aspectRatio)
        val viewProjMatrix = projMatrix * viewMatrix

        // Render Grid & Ground Plane
        if (scene.gridSettings.showGrid) {
            renderGrid(drawScope, scene.gridSettings, viewProjMatrix, canvasWidth, canvasHeight)
        }

        val renderedPolygons = mutableListOf<RenderPolygon>()

        // Process Meshes
        scene.meshes.filter { it.isVisible }.forEach { mesh ->
            val isMeshSelected = mesh.id == selectedMeshId
            val modelMatrix = mesh.getModelMatrix()
            val mvpMatrix = viewProjMatrix * modelMatrix

            // Calculate animated vertex positions if skeletal animation is active
            val animatedVertices = calculateAnimatedVertices(mesh, scene.skeleton)

            // Project vertices to screen
            val projectedVertices = animatedVertices.map { vertex ->
                val worldPos = modelMatrix.transformVector(vertex.position)
                val worldNormal = modelMatrix.transformDirection(vertex.normal)

                // MVP transform
                val clipPos = mvpMatrix.transformVector(vertex.position)

                // Screen coordinates mapping (-1..1 to 0..Width/Height)
                val screenX = (clipPos.x + 1.0f) * 0.5f * canvasWidth
                val screenY = (1.0f - clipPos.y) * 0.5f * canvasHeight

                ProjectedVertex(
                    screenPos = Offset(screenX, screenY),
                    depthZ = clipPos.z,
                    worldPos = worldPos,
                    worldNormal = worldNormal,
                    color = vertex.color
                )
            }

            // Assemble faces
            mesh.faces.forEach { face ->
                if (face.v1 in projectedVertices.indices &&
                    face.v2 in projectedVertices.indices &&
                    face.v3 in projectedVertices.indices
                ) {
                    val pv1 = projectedVertices[f1(face, projectedVertices)]
                    val pv2 = projectedVertices[f2(face, projectedVertices)]
                    val pv3 = projectedVertices[f3(face, projectedVertices)]

                    // Backface culling
                    val e1 = pv2.screenPos - pv1.screenPos
                    val e2 = pv3.screenPos - pv1.screenPos
                    val crossZ = e1.x * e2.y - e1.y * e2.x

                    // Allow wireframe or two-sided rendering if enabled
                    if (crossZ < 0f || mesh.material.isWireframe || scene.renderMode == RenderMode.WIREFRAME) {
                        val avgDepth = (pv1.depthZ + pv2.depthZ + pv3.depthZ) / 3.0f
                        val faceNorm = (pv2.worldPos - pv1.worldPos).cross(pv3.worldPos - pv1.worldPos).normalized()

                        renderedPolygons.add(
                            RenderPolygon(
                                p1 = pv1,
                                p2 = pv2,
                                p3 = pv3,
                                avgDepth = avgDepth,
                                faceNormal = faceNorm,
                                material = mesh.material,
                                isSelected = isMeshSelected
                            )
                        )
                    }
                }
            }
        }

        // Painter's Algorithm Depth Sort (Far to Near)
        renderedPolygons.sortByDescending { it.avgDepth }

        // Draw Polygons
        renderedPolygons.forEach { poly ->
            drawPolygon(drawScope, poly, scene, selectedMeshId)
        }

        // Render Skeleton Bones overlay if mode enabled
        if (scene.renderMode == RenderMode.SKELETON_BONES || scene.skeleton.bones.isNotEmpty()) {
            renderSkeletonOverlay(drawScope, scene.skeleton, viewProjMatrix, canvasWidth, canvasHeight)
        }
    }

    private fun f1(f: Face, v: List<ProjectedVertex>) = f.v1.coerceIn(v.indices)
    private fun f2(f: Face, v: List<ProjectedVertex>) = f.v2.coerceIn(v.indices)
    private fun f3(f: Face, v: List<ProjectedVertex>) = f.v3.coerceIn(v.indices)

    private fun drawPolygon(
        drawScope: DrawScope,
        poly: RenderPolygon,
        scene: Scene3D,
        selectedMeshId: String?
    ) {
        val path = Path().apply {
            moveTo(poly.p1.screenPos.x, poly.p1.screenPos.y)
            lineTo(poly.p2.screenPos.x, poly.p2.screenPos.y)
            lineTo(poly.p3.screenPos.x, poly.p3.screenPos.y)
            close()
        }

        if (scene.renderMode == RenderMode.WIREFRAME || poly.material.isWireframe) {
            val wireColor = if (poly.isSelected) Color(0xFFF59E0B) else Color(0xFF38BDF8)
            drawScope.drawPath(path, wireColor, style = Stroke(width = 1.5f))
            return
        }

        // Shading model calculation (Phong / PBR Lit)
        val light = scene.light
        val lightDir = -light.direction
        val N = poly.faceNormal
        val L = lightDir
        val NdotL = maxOf(0f, N.dot(L))

        // Specular highlight
        val viewDir = (scene.camera.getEyePosition() - poly.p1.worldPos).normalized()
        val H = (L + viewDir).normalized()
        val NdotH = maxOf(0f, N.dot(H))
        val specPower = 1f + (1f - poly.material.roughness) * 64f
        val specIntensity = NdotH.pow(specPower) * poly.material.metallic

        // Base color blending
        val baseColor = poly.p1.color.takeIf { it != Color.White } ?: poly.material.baseColor
        val ambient = light.ambientIntensity
        val diffuse = NdotL * light.intensity
        val totalLight = (ambient + diffuse).coerceIn(0f, 1.2f)

        val r = (baseColor.red * totalLight + specIntensity).coerceIn(0f, 1f)
        val g = (baseColor.green * totalLight + specIntensity).coerceIn(0f, 1f)
        val b = (baseColor.blue * totalLight + specIntensity).coerceIn(0f, 1f)

        val finalColor = Color(r, g, b, baseColor.alpha)

        // Fill Triangle
        drawScope.drawPath(path, finalColor)

        // Draw selection outline or mesh edges
        val edgeColor = if (poly.isSelected) Color(0xFFF59E0B) else Color(0x22000000)
        val edgeWidth = if (poly.isSelected) 2.0f else 0.8f
        drawScope.drawPath(path, edgeColor, style = Stroke(width = edgeWidth))
    }

    private fun renderGrid(
        drawScope: DrawScope,
        grid: GridSettings,
        viewProjMatrix: Matrix4,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        val lines = grid.gridLines
        val spacing = grid.gridSpacing
        val half = lines * spacing / 2f

        for (i in 0..lines) {
            val coord = -half + i * spacing

            // X-parallel line
            drawWorldLine(
                drawScope,
                Vector3(-half, 0f, coord),
                Vector3(half, 0f, coord),
                if (coord == 0f) Color(0xFFEF4444) else Color(0x3364748B),
                viewProjMatrix, canvasWidth, canvasHeight,
                strokeWidth = if (coord == 0f) 2.5f else 1.0f
            )

            // Z-parallel line
            drawWorldLine(
                drawScope,
                Vector3(coord, 0f, -half),
                Vector3(coord, 0f, half),
                if (coord == 0f) Color(0xFF3B82F6) else Color(0x3364748B),
                viewProjMatrix, canvasWidth, canvasHeight,
                strokeWidth = if (coord == 0f) 2.5f else 1.0f
            )
        }

        // Y-axis line (Up)
        if (grid.showAxes) {
            drawWorldLine(
                drawScope,
                Vector3(0f, 0f, 0f),
                Vector3(0f, 1.5f, 0f),
                Color(0xFF10B981), // Green Y axis
                viewProjMatrix, canvasWidth, canvasHeight,
                strokeWidth = 3.0f
            )
        }
    }

    private fun renderSkeletonOverlay(
        drawScope: DrawScope,
        skeleton: Skeleton,
        viewProjMatrix: Matrix4,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        skeleton.bones.forEach { bone ->
            val boneWorldMat = skeleton.getBoneWorldMatrix(bone.id)
            val startPos = boneWorldMat.transformVector(Vector3.Zero)
            val endPos = boneWorldMat.transformVector(Vector3(0f, bone.length, 0f))

            // Draw bone connector line
            drawWorldLine(
                drawScope, startPos, endPos,
                Color(0xFFEC4899), viewProjMatrix, canvasWidth, canvasHeight, strokeWidth = 3.5f
            )

            // Draw bone joint sphere
            val screenStart = projectPoint(startPos, viewProjMatrix, canvasWidth, canvasHeight)
            if (screenStart != null) {
                drawScope.drawCircle(Color(0xFFF43F5E), radius = 8f, center = screenStart)
                drawScope.drawCircle(Color.White, radius = 5f, center = screenStart)
            }
        }
    }

    fun drawWorldLine(
        drawScope: DrawScope,
        p1: Vector3, p2: Vector3,
        color: Color,
        viewProjMatrix: Matrix4,
        canvasWidth: Float, canvasHeight: Float,
        strokeWidth: Float = 1.0f
    ) {
        val sp1 = projectPoint(p1, viewProjMatrix, canvasWidth, canvasHeight)
        val sp2 = projectPoint(p2, viewProjMatrix, canvasWidth, canvasHeight)
        if (sp1 != null && sp2 != null) {
            drawScope.drawLine(color, sp1, sp2, strokeWidth = strokeWidth)
        }
    }

    private fun projectPoint(
        worldPos: Vector3,
        viewProjMatrix: Matrix4,
        canvasWidth: Float,
        canvasHeight: Float
    ): Offset? {
        val clip = viewProjMatrix.transformVector(worldPos)
        if (clip.z < 0f) return null
        val screenX = (clip.x + 1.0f) * 0.5f * canvasWidth
        val screenY = (1.0f - clip.y) * 0.5f * canvasHeight
        return Offset(screenX, screenY)
    }

    private fun calculateAnimatedVertices(mesh: Mesh3D, skeleton: Skeleton): List<Vertex> {
        if (skeleton.bones.isEmpty()) return mesh.vertices

        return mesh.vertices.map { v ->
            val boneIdx = v.boneIndices.getOrElse(0) { 0 }
            val boneWeight = v.boneWeights.getOrElse(0) { 1.0f }

            if (boneIdx in skeleton.bones.indices && boneWeight > 0f) {
                val boneMat = skeleton.getBoneWorldMatrix(boneIdx)
                val defPos = boneMat.transformVector(v.position)
                v.copy(position = v.position.lerp(defPos, boneWeight))
            } else {
                v
            }
        }
    }
}
