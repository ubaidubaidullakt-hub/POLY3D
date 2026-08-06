package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

enum class RenderMode {
    SHADED,
    WIREFRAME,
    PBR_LIT,
    SKELETON_BONES,
    MOLECULAR_HEATMAP
}

enum class GizmoMode {
    TRANSLATE,
    ROTATE,
    SCALE,
    NONE
}

data class Light3D(
    val direction: Vector3 = Vector3(-0.5f, -1.0f, -0.7f).normalized(),
    val color: Color = Color.White,
    val intensity: Float = 1.0f,
    val ambientIntensity: Float = 0.35f
)

data class Camera3D(
    val orbitYaw: Float = 45f,
    val orbitPitch: Float = 25f,
    val orbitDistance: Float = 6f,
    val panOffset: Vector3 = Vector3.Zero,
    val fovY: Float = 60f,
    val isOrthographic: Boolean = false
) {
    fun getEyePosition(): Vector3 {
        val radYaw = Math.toRadians(orbitYaw.toDouble()).toFloat()
        val radPitch = Math.toRadians(orbitPitch.toDouble()).toFloat()

        val x = orbitDistance * cos(radPitch) * sin(radYaw)
        val y = orbitDistance * sin(radPitch)
        val z = orbitDistance * cos(radPitch) * cos(radYaw)

        return Vector3(x, y, z) + panOffset
    }

    fun getTargetPosition(): Vector3 = panOffset

    fun getViewMatrix(): Matrix4 {
        return Matrix4.lookAt(getEyePosition(), getTargetPosition(), Vector3.Up)
    }

    fun getProjectionMatrix(aspectRatio: Float): Matrix4 {
        return if (isOrthographic) {
            val h = orbitDistance * 0.5f
            val w = h * aspectRatio
            Matrix4.orthographic(-w, w, -h, h, 0.1f, 100f)
        } else {
            Matrix4.perspective(fovY, aspectRatio, 0.1f, 100f)
        }
    }
}

data class GridSettings(
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val gridLines: Int = 20,
    val gridSpacing: Float = 0.5f
)

data class Scene3D(
    val meshes: List<Mesh3D> = emptyList(),
    val skeleton: Skeleton = Skeleton(),
    val activeAnimation: AnimationTrack? = null,
    val light: Light3D = Light3D(),
    val camera: Camera3D = Camera3D(),
    val renderMode: RenderMode = RenderMode.PBR_LIT,
    val gizmoMode: GizmoMode = GizmoMode.TRANSLATE,
    val gridSettings: GridSettings = GridSettings(),
    val backgroundColor: Color = Color(0xFF0F172A) // Dark slate
)
