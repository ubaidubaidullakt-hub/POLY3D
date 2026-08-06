package com.example.engine

import com.example.model.*
import androidx.compose.ui.geometry.Offset
import kotlin.math.tan

object Raycaster {

    fun screenPointToRay(
        touchPoint: Offset,
        camera: Camera3D,
        canvasWidth: Float,
        canvasHeight: Float
    ): Ray3D {
        val aspect = canvasWidth / canvasHeight
        val ndcX = (2.0f * touchPoint.x / canvasWidth) - 1.0f
        val ndcY = 1.0f - (2.0f * touchPoint.y / canvasHeight)

        val eye = camera.getEyePosition()
        val target = camera.getTargetPosition()
        val forward = (target - eye).normalized()
        val right = Vector3.Up.cross(forward).normalized()
        val up = forward.cross(right).normalized()

        val fovRad = Math.toRadians(camera.fovY.toDouble() * 0.5).toFloat()
        val halfH = tan(fovRad)
        val halfW = halfH * aspect

        val rayDir = (forward + right * (ndcX * halfW) + up * (ndcY * halfH)).normalized()
        return Ray3D(origin = eye, direction = rayDir)
    }

    fun raycastScene(
        ray: Ray3D,
        meshes: List<Mesh3D>
    ): RaycastHit? {
        var closestHit: RaycastHit? = null
        var minDistance = Float.MAX_VALUE

        meshes.filter { it.isVisible }.forEach { mesh ->
            val bbox = mesh.calculateBoundingBox()
            // Bounding box intersection check
            if (rayIntersectsBBox(ray, bbox)) {
                val modelMat = mesh.getModelMatrix()
                mesh.faces.forEachIndexed { faceIdx, face ->
                    val p1 = modelMat.transformVector(mesh.vertices[face.v1.coerceIn(mesh.vertices.indices)].position)
                    val p2 = modelMat.transformVector(mesh.vertices[face.v2.coerceIn(mesh.vertices.indices)].position)
                    val p3 = modelMat.transformVector(mesh.vertices[face.v3.coerceIn(mesh.vertices.indices)].position)

                    val dist = rayTriangleIntersection(ray, p1, p2, p3)
                    if (dist != null && dist < minDistance) {
                        minDistance = dist
                        val hitPoint = ray.origin + (ray.direction * dist)
                        closestHit = RaycastHit(
                            hitPoint = hitPoint,
                            distance = dist,
                            faceIndex = faceIdx,
                            meshId = mesh.id
                        )
                    }
                }
            }
        }

        return closestHit
    }

    private fun rayIntersectsBBox(ray: Ray3D, box: BoundingBox): Boolean {
        val invDirX = if (ray.direction.x != 0f) 1f / ray.direction.x else Float.MAX_VALUE
        val invDirY = if (ray.direction.y != 0f) 1f / ray.direction.y else Float.MAX_VALUE
        val invDirZ = if (ray.direction.z != 0f) 1f / ray.direction.z else Float.MAX_VALUE

        val t1 = (box.min.x - ray.origin.x) * invDirX
        val t2 = (box.max.x - ray.origin.x) * invDirX
        val t3 = (box.min.y - ray.origin.y) * invDirY
        val t4 = (box.max.y - ray.origin.y) * invDirY
        val t5 = (box.min.z - ray.origin.z) * invDirZ
        val t6 = (box.max.z - ray.origin.z) * invDirZ

        val tmin = maxOf(maxOf(minOf(t1, t2), minOf(t3, t4)), minOf(t5, t6))
        val tmax = minOf(minOf(maxOf(t1, t2), maxOf(t3, t4)), maxOf(t5, t6))

        return tmax >= maxOf(0f, tmin)
    }

    private fun rayTriangleIntersection(ray: Ray3D, p1: Vector3, p2: Vector3, p3: Vector3): Float? {
        val e1 = p2 - p1
        val e2 = p3 - p1
        val h = ray.direction.cross(e2)
        val a = e1.dot(h)

        if (a > -0.00001f && a < 0.00001f) return null // Ray parallel to triangle

        val f = 1.0f / a
        val s = ray.origin - p1
        val u = f * s.dot(h)

        if (u < 0.0f || u > 1.0f) return null

        val q = s.cross(e1)
        val v = f * ray.direction.dot(q)

        if (v < 0.0f || u + v > 1.0f) return null

        val t = f * e2.dot(q)
        return if (t > 0.00001f) t else null
    }
}
