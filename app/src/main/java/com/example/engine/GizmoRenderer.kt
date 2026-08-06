package com.example.engine

import com.example.model.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

enum class GizmoAxis {
    NONE, X, Y, Z, CENTER
}

object GizmoRenderer {

    fun renderGizmo(
        drawScope: DrawScope,
        selectedMesh: Mesh3D,
        scene: Scene3D,
        canvasWidth: Float,
        canvasHeight: Float,
        activeAxis: GizmoAxis = GizmoAxis.NONE
    ) {
        val mode = scene.gizmoMode
        if (mode == GizmoMode.NONE) return

        val viewProjMatrix = scene.camera.getProjectionMatrix(canvasWidth / canvasHeight) * scene.camera.getViewMatrix()
        val origin = selectedMesh.position

        val axisLength = 1.2f

        val xEnd = origin + Vector3(axisLength, 0f, 0f)
        val yEnd = origin + Vector3(0f, axisLength, 0f)
        val zEnd = origin + Vector3(0f, 0f, axisLength)

        val xColor = if (activeAxis == GizmoAxis.X) Color(0xFFFF5252) else Color(0xFFEF4444)
        val yColor = if (activeAxis == GizmoAxis.Y) Color(0xFF4ADE80) else Color(0xFF22C55E)
        val zColor = if (activeAxis == GizmoAxis.Z) Color(0xFF60A5FA) else Color(0xFF3B82F6)

        val sOrigin = project(origin, viewProjMatrix, canvasWidth, canvasHeight) ?: return
        val sX = project(xEnd, viewProjMatrix, canvasWidth, canvasHeight)
        val sY = project(yEnd, viewProjMatrix, canvasWidth, canvasHeight)
        val sZ = project(zEnd, viewProjMatrix, canvasWidth, canvasHeight)

        when (mode) {
            GizmoMode.TRANSLATE -> {
                // X Axis Red Arrow
                if (sX != null) {
                    drawScope.drawLine(xColor, sOrigin, sX, strokeWidth = 4.0f)
                    drawScope.drawCircle(xColor, radius = 9f, center = sX)
                }
                // Y Axis Green Arrow
                if (sY != null) {
                    drawScope.drawLine(yColor, sOrigin, sY, strokeWidth = 4.0f)
                    drawScope.drawCircle(yColor, radius = 9f, center = sY)
                }
                // Z Axis Blue Arrow
                if (sZ != null) {
                    drawScope.drawLine(zColor, sOrigin, sZ, strokeWidth = 4.0f)
                    drawScope.drawCircle(zColor, radius = 9f, center = sZ)
                }
                // Center origin handle
                drawScope.drawCircle(Color.White, radius = 8f, center = sOrigin)
            }
            GizmoMode.ROTATE -> {
                // Draw rotation rings
                drawScope.drawCircle(xColor, radius = 45f, center = sOrigin, style = Stroke(3.5f))
                drawScope.drawCircle(yColor, radius = 60f, center = sOrigin, style = Stroke(3.5f))
                drawScope.drawCircle(zColor, radius = 75f, center = sOrigin, style = Stroke(3.5f))
            }
            GizmoMode.SCALE -> {
                // Draw scale handles (square boxes at end)
                if (sX != null) {
                    drawScope.drawLine(xColor, sOrigin, sX, strokeWidth = 4f)
                    drawScope.drawRect(xColor, topLeft = Offset(sX.x - 7f, sX.y - 7f), size = androidx.compose.ui.geometry.Size(14f, 14f))
                }
                if (sY != null) {
                    drawScope.drawLine(yColor, sOrigin, sY, strokeWidth = 4f)
                    drawScope.drawRect(yColor, topLeft = Offset(sY.x - 7f, sY.y - 7f), size = androidx.compose.ui.geometry.Size(14f, 14f))
                }
                if (sZ != null) {
                    drawScope.drawLine(zColor, sOrigin, sZ, strokeWidth = 4f)
                    drawScope.drawRect(zColor, topLeft = Offset(sZ.x - 7f, sZ.y - 7f), size = androidx.compose.ui.geometry.Size(14f, 14f))
                }
            }
            GizmoMode.NONE -> {}
        }
    }

    fun detectGizmoAxisHit(
        touchOffset: Offset,
        selectedMesh: Mesh3D,
        scene: Scene3D,
        canvasWidth: Float,
        canvasHeight: Float
    ): GizmoAxis {
        val viewProjMatrix = scene.camera.getProjectionMatrix(canvasWidth / canvasHeight) * scene.camera.getViewMatrix()
        val origin = selectedMesh.position

        val axisLength = 1.2f
        val sOrigin = project(origin, viewProjMatrix, canvasWidth, canvasHeight) ?: return GizmoAxis.NONE
        val sX = project(origin + Vector3(axisLength, 0f, 0f), viewProjMatrix, canvasWidth, canvasHeight)
        val sY = project(origin + Vector3(0f, axisLength, 0f), viewProjMatrix, canvasWidth, canvasHeight)
        val sZ = project(origin + Vector3(0f, 0f, axisLength), viewProjMatrix, canvasWidth, canvasHeight)

        val threshold = 28f

        if ((touchOffset - sOrigin).getDistance() <= threshold) return GizmoAxis.CENTER
        if (sX != null && (touchOffset - sX).getDistance() <= threshold) return GizmoAxis.X
        if (sY != null && (touchOffset - sY).getDistance() <= threshold) return GizmoAxis.Y
        if (sZ != null && (touchOffset - sZ).getDistance() <= threshold) return GizmoAxis.Z

        return GizmoAxis.NONE
    }

    private fun project(p: Vector3, viewProjMat: Matrix4, w: Float, h: Float): Offset? {
        val clip = viewProjMat.transformVector(p)
        if (clip.z < 0f) return null
        val sx = (clip.x + 1f) * 0.5f * w
        val sy = (1f - clip.y) * 0.5f * h
        return Offset(sx, sy)
    }
}
