package com.example.tools

import com.example.model.*
import androidx.compose.ui.graphics.Color
import kotlin.math.*

object PrimitiveGenerator {

    fun generateCube(
        size: Float = 1.0f,
        color: Color = Color(0xFF38BDF8),
        name: String = "Cube"
    ): Mesh3D {
        val s = size / 2.0f
        val positions = listOf(
            Vector3(-s, -s,  s), Vector3( s, -s,  s), Vector3( s,  s,  s), Vector3(-s,  s,  s), // Front
            Vector3( s, -s, -s), Vector3(-s, -s, -s), Vector3(-s,  s, -s), Vector3( s,  s, -s), // Back
            Vector3(-s,  s,  s), Vector3( s,  s,  s), Vector3( s,  s, -s), Vector3(-s,  s, -s), // Top
            Vector3(-s, -s, -s), Vector3( s, -s, -s), Vector3( s, -s,  s), Vector3(-s, -s,  s), // Bottom
            Vector3( s, -s,  s), Vector3( s, -s, -s), Vector3( s,  s, -s), Vector3( s,  s,  s), // Right
            Vector3(-s, -s, -s), Vector3(-s, -s,  s), Vector3(-s,  s,  s), Vector3(-s,  s, -s)  // Left
        )

        val normals = listOf(
            Vector3( 0f,  0f,  1f), Vector3( 0f,  0f, -1f),
            Vector3( 0f,  1f,  0f), Vector3( 0f, -1f,  0f),
            Vector3( 1f,  0f,  0f), Vector3(-1f,  0f,  0f)
        )

        val vertices = mutableListOf<Vertex>()
        for (i in 0 until 6) {
            val norm = normals[i]
            for (j in 0 until 4) {
                vertices.add(Vertex(position = positions[i * 4 + j], normal = norm, color = color))
            }
        }

        val faces = mutableListOf<Face>()
        for (i in 0 until 6) {
            val base = i * 4
            faces.add(Face(base, base + 1, base + 2))
            faces.add(Face(base, base + 2, base + 3))
        }

        return Mesh3D(
            id = "cube_${System.currentTimeMillis()}",
            name = name,
            vertices = vertices,
            faces = faces,
            material = Material3D(baseColor = color, name = "$name Material")
        )
    }

    fun generateSphere(
        radius: Float = 1.0f,
        rings: Int = 16,
        segments: Int = 16,
        color: Color = Color(0xFFEC4899),
        name: String = "Sphere"
    ): Mesh3D {
        val vertices = mutableListOf<Vertex>()
        val faces = mutableListOf<Face>()

        for (r in 0..rings) {
            val v = r.toFloat() / rings
            val phi = v * PI.toFloat()

            for (s in 0..segments) {
                val u = s.toFloat() / segments
                val theta = u * 2f * PI.toFloat()

                val x = cos(theta) * sin(phi)
                val y = cos(phi)
                val z = sin(theta) * sin(phi)

                val pos = Vector3(x, y, z) * radius
                val norm = Vector3(x, y, z).normalized()

                vertices.add(Vertex(position = pos, normal = norm, color = color))
            }
        }

        for (r in 0 until rings) {
            for (s in 0 until segments) {
                val first = r * (segments + 1) + s
                val second = first + segments + 1

                faces.add(Face(first, second, first + 1))
                faces.add(Face(second, second + 1, first + 1))
            }
        }

        return Mesh3D(
            id = "sphere_${System.currentTimeMillis()}",
            name = name,
            vertices = vertices,
            faces = faces,
            material = Material3D(baseColor = color, name = "$name Material")
        )
    }

    fun generateCylinder(
        radius: Float = 0.8f,
        height: Float = 1.6f,
        segments: Int = 16,
        color: Color = Color(0xFF10B981),
        name: String = "Cylinder"
    ): Mesh3D {
        val vertices = mutableListOf<Vertex>()
        val faces = mutableListOf<Face>()
        val halfH = height / 2.0f

        // Side vertices
        for (i in 0..segments) {
            val theta = (i.toFloat() / segments) * 2f * PI.toFloat()
            val x = cos(theta) * radius
            val z = sin(theta) * radius
            val norm = Vector3(cos(theta), 0f, sin(theta)).normalized()

            vertices.add(Vertex(position = Vector3(x,  halfH, z), normal = norm, color = color))
            vertices.add(Vertex(position = Vector3(x, -halfH, z), normal = norm, color = color))
        }

        for (i in 0 until segments) {
            val base = i * 2
            faces.add(Face(base, base + 1, base + 2))
            faces.add(Face(base + 1, base + 3, base + 2))
        }

        return Mesh3D(
            id = "cylinder_${System.currentTimeMillis()}",
            name = name,
            vertices = vertices,
            faces = faces,
            material = Material3D(baseColor = color, name = "$name Material")
        )
    }

    fun generateTorus(
        mainRadius: Float = 1.0f,
        tubeRadius: Float = 0.3f,
        radialSegments: Int = 16,
        tubularSegments: Int = 16,
        color: Color = Color(0xFFF59E0B),
        name: String = "Torus"
    ): Mesh3D {
        val vertices = mutableListOf<Vertex>()
        val faces = mutableListOf<Face>()

        for (j in 0..radialSegments) {
            for (i in 0..tubularSegments) {
                val u = i.toFloat() / tubularSegments * 2f * PI.toFloat()
                val v = j.toFloat() / radialSegments * 2f * PI.toFloat()

                val x = (mainRadius + tubeRadius * cos(v)) * cos(u)
                val y = tubeRadius * sin(v)
                val z = (mainRadius + tubeRadius * cos(v)) * sin(u)

                val center = Vector3(mainRadius * cos(u), 0f, mainRadius * sin(u))
                val pos = Vector3(x, y, z)
                val norm = (pos - center).normalized()

                vertices.add(Vertex(position = pos, normal = norm, color = color))
            }
        }

        for (j in 0 until radialSegments) {
            for (i in 0 until tubularSegments) {
                val a = (tubularSegments + 1) * j + i
                val b = (tubularSegments + 1) * (j + 1) + i
                val c = (tubularSegments + 1) * (j + 1) + i + 1
                val d = (tubularSegments + 1) * j + i + 1

                faces.add(Face(a, b, d))
                faces.add(Face(b, c, d))
            }
        }

        return Mesh3D(
            id = "torus_${System.currentTimeMillis()}",
            name = name,
            vertices = vertices,
            faces = faces,
            material = Material3D(baseColor = color, name = "$name Material")
        )
    }

    fun generateTeapot(
        scale: Float = 1.2f,
        color: Color = Color(0xFFA855F7),
        name: String = "Utah Teapot"
    ): Mesh3D {
        val sphere = generateSphere(radius = scale, rings = 12, segments = 12, color = color, name = name)
        // Deform sphere vertices to form teapot body + handle + spout
        val deformedVertices = sphere.vertices.map { v ->
            val p = v.position
            val newY = p.y * 0.7f
            val isHandleSide = p.x < -0.3f
            val isSpoutSide = p.x > 0.3f
            val newX = when {
                isHandleSide -> p.x * 1.3f
                isSpoutSide -> p.x * 1.4f + (p.y * 0.4f)
                else -> p.x
            }
            v.copy(position = Vector3(newX, newY, p.z))
        }
        return sphere.copy(vertices = deformedVertices)
    }

    fun generateMolecularStructure(
        atomCount: Int = 18,
        name: String = "Hemoglobin Cluster"
    ): Mesh3D {
        val vertices = mutableListOf<Vertex>()
        val faces = mutableListOf<Face>()
        var vertexOffset = 0

        val atomColors = listOf(
            Color(0xFFEF4444), // Oxygen Red
            Color(0xFF3B82F6), // Nitrogen Blue
            Color(0xFF64748B), // Carbon Grey
            Color(0xFFF59E0B)  // Sulfur Gold
        )

        for (i in 0 until atomCount) {
            val angle = (i.toFloat() / atomCount) * 2f * PI.toFloat()
            val radius = 1.2f + sin(i * 1.5f) * 0.4f
            val cx = cos(angle) * radius
            val cy = sin(i * 0.8f) * 0.6f
            val cz = sin(angle) * radius

            val atomColor = atomColors[i % atomColors.size]
            val atomSphere = generateSphere(radius = 0.25f, rings = 8, segments = 8, color = atomColor)

            atomSphere.vertices.forEach { v ->
                vertices.add(v.copy(position = v.position + Vector3(cx, cy, cz)))
            }
            atomSphere.faces.forEach { f ->
                faces.add(f.copy(v1 = f.v1 + vertexOffset, v2 = f.v2 + vertexOffset, v3 = f.v3 + vertexOffset))
            }
            vertexOffset += atomSphere.vertices.size
        }

        return Mesh3D(
            id = "molecule_${System.currentTimeMillis()}",
            name = name,
            vertices = vertices,
            faces = faces,
            material = Material3D(baseColor = Color(0xFFE2E8F0), name = "Molecular Material")
        )
    }
}
