package com.example.model

import androidx.compose.ui.graphics.Color

data class Vertex(
    val position: Vector3,
    val normal: Vector3 = Vector3(0f, 1f, 0f),
    val color: Color = Color.White,
    val uv: Vector2 = Vector2(0f, 0f),
    val boneIndices: IntArray = intArrayOf(0, 0, 0, 0),
    val boneWeights: FloatArray = floatArrayOf(1f, 0f, 0f, 0f)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vertex) return false
        return position == other.position && normal == other.normal && color == other.color
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + normal.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }
}

data class Face(
    val v1: Int,
    val v2: Int,
    val v3: Int,
    val normal: Vector3 = Vector3(0f, 1f, 0f),
    val color: Color? = null,
    val materialId: String = "default"
)

data class Material3D(
    val id: String = "default",
    val name: String = "Default Material",
    val baseColor: Color = Color(0xFF3B82F6),
    val metallic: Float = 0.2f,
    val roughness: Float = 0.4f,
    val emission: Color = Color.Transparent,
    val isWireframe: Boolean = false,
    val presetName: String = "Custom"
) {
    companion object {
        val StandardGold = Material3D(
            id = "gold", name = "Standard Gold", baseColor = Color(0xFFFFD700),
            metallic = 0.9f, roughness = 0.15f, presetName = "Gold"
        )
        val ChromeSilver = Material3D(
            id = "chrome", name = "Chrome Silver", baseColor = Color(0xFFE2E8F0),
            metallic = 0.95f, roughness = 0.05f, presetName = "Chrome"
        )
        val NeonGlow = Material3D(
            id = "neon", name = "Cyber Neon", baseColor = Color(0xFF00F0FF),
            metallic = 0.1f, roughness = 0.2f, emission = Color(0xFF00F0FF), presetName = "Neon"
        )
        val MattePlastic = Material3D(
            id = "matte", name = "Matte Plastic", baseColor = Color(0xFF64748B),
            metallic = 0.0f, roughness = 0.8f, presetName = "Matte"
        )
        val CarbonFiber = Material3D(
            id = "carbon", name = "Carbon Fiber", baseColor = Color(0xFF1E293B),
            metallic = 0.5f, roughness = 0.3f, presetName = "Carbon"
        )
        val EmeraldGlass = Material3D(
            id = "emerald", name = "Emerald Crystal", baseColor = Color(0xFF10B981),
            metallic = 0.3f, roughness = 0.1f, presetName = "Emerald"
        )
    }
}

data class Mesh3D(
    val id: String,
    val name: String,
    val vertices: List<Vertex>,
    val faces: List<Face>,
    val material: Material3D = Material3D(),
    val position: Vector3 = Vector3.Zero,
    val rotation: Vector3 = Vector3.Zero, // Degrees X, Y, Z
    val scale: Vector3 = Vector3.One,
    val isSelected: Boolean = false,
    val isVisible: Boolean = true,
    val molecularPartId: Int = 0
) {
    fun calculateBoundingBox(): BoundingBox {
        if (vertices.isEmpty()) return BoundingBox()
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        val modelMatrix = getModelMatrix()
        vertices.forEach { v ->
            val worldPos = modelMatrix.transformVector(v.position)
            minX = minOf(minX, worldPos.x); minY = minOf(minY, worldPos.y); minZ = minOf(minZ, worldPos.z)
            maxX = maxOf(maxX, worldPos.x); maxY = maxOf(maxY, worldPos.y); maxZ = maxOf(maxZ, worldPos.z)
        }
        return BoundingBox(Vector3(minX, minY, minZ), Vector3(maxX, maxY, maxZ))
    }

    fun getModelMatrix(): Matrix4 {
        val tMat = Matrix4.translation(position)
        val rMat = Matrix4.eulerRotation(rotation)
        val sMat = Matrix4.scale(scale)
        return tMat * rMat * sMat
    }
}
