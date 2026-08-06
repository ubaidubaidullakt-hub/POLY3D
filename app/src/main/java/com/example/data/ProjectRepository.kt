package com.example.data

import com.example.model.*
import com.example.tools.PrimitiveGenerator
import kotlinx.coroutines.flow.Flow
import androidx.compose.ui.graphics.Color

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun saveProject(project: ProjectEntity): Long {
        return projectDao.insertProject(project)
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteProjectById(id)
    }

    fun loadSamplePresetScene(presetName: String): Scene3D {
        return when (presetName) {
            "Sci-Fi Mech" -> createSciFiMechScene()
            "Hemoglobin Molecular Protein" -> createHemoglobinMoleculeScene()
            "Low-Poly Sports Car" -> createSportsCarScene()
            else -> createGeometricPrismScene()
        }
    }

    private fun createSciFiMechScene(): Scene3D {
        val torso = PrimitiveGenerator.generateCube(size = 1.2f, color = Color(0xFF1E293B), name = "Mech Torso")
            .copy(position = Vector3(0f, 1.2f, 0f))

        val head = PrimitiveGenerator.generateSphere(radius = 0.4f, rings = 12, segments = 12, color = Color(0xFF00F0FF), name = "Mech Core Head")
            .copy(position = Vector3(0f, 2.1f, 0f), material = Material3D.NeonGlow)

        val leftArm = PrimitiveGenerator.generateCylinder(radius = 0.2f, height = 1.2f, segments = 12, color = Color(0xFF64748B), name = "Arm L")
            .copy(position = Vector3(-1.1f, 1.2f, 0f), rotation = Vector3(0f, 0f, 15f))

        val rightArm = PrimitiveGenerator.generateCylinder(radius = 0.2f, height = 1.2f, segments = 12, color = Color(0xFF64748B), name = "Arm R")
            .copy(position = Vector3(1.1f, 1.2f, 0f), rotation = Vector3(0f, 0f, -15f))

        val skeleton = Skeleton(
            bones = listOf(
                Bone(id = 0, name = "Root", length = 0.8f, restPosition = Vector3(0f, 0f, 0f)),
                Bone(id = 1, name = "Chest Joint", parentId = 0, length = 1.2f, restPosition = Vector3(0f, 1.2f, 0f)),
                Bone(id = 2, name = "Head Joint", parentId = 1, length = 0.6f, restPosition = Vector3(0f, 2.1f, 0f)),
                Bone(id = 3, name = "Shoulder L", parentId = 1, length = 0.8f, restPosition = Vector3(-1.1f, 1.2f, 0f)),
                Bone(id = 4, name = "Shoulder R", parentId = 1, length = 0.8f, restPosition = Vector3(1.1f, 1.2f, 0f))
            )
        )

        val keyframes = listOf(
            BoneKeyframe(boneId = 2, timeSeconds = 0.0f, position = Vector3(0f, 2.1f, 0f), rotation = Vector3(0f, -20f, 0f)),
            BoneKeyframe(boneId = 2, timeSeconds = 1.0f, position = Vector3(0f, 2.1f, 0f), rotation = Vector3(0f, 20f, 0f)),
            BoneKeyframe(boneId = 2, timeSeconds = 2.0f, position = Vector3(0f, 2.1f, 0f), rotation = Vector3(0f, -20f, 0f))
        )

        val animation = AnimationTrack(
            id = "mech_idle",
            name = "Idle Breather",
            durationSeconds = 2.0f,
            keyframes = keyframes,
            isPlaying = true
        )

        return Scene3D(
            meshes = listOf(torso, head, leftArm, rightArm),
            skeleton = skeleton,
            activeAnimation = animation
        )
    }

    private fun createHemoglobinMoleculeScene(): Scene3D {
        val molecule = PrimitiveGenerator.generateMolecularStructure(atomCount = 28, name = "Hemoglobin Cluster")
        return Scene3D(
            meshes = listOf(molecule),
            renderMode = RenderMode.PBR_LIT
        )
    }

    private fun createSportsCarScene(): Scene3D {
        val body = PrimitiveGenerator.generateCube(size = 1.5f, color = Color(0xFFEF4444), name = "Car Chassis")
            .copy(position = Vector3(0f, 0.4f, 0f), scale = Vector3(1.2f, 0.4f, 2.2f), material = Material3D.StandardGold)

        val cabin = PrimitiveGenerator.generateCube(size = 1.0f, color = Color(0xFF1E293B), name = "Cabin Glass")
            .copy(position = Vector3(0f, 0.8f, -0.2f), scale = Vector3(0.9f, 0.4f, 1.0f), material = Material3D.CarbonFiber)

        val w1 = PrimitiveGenerator.generateCylinder(radius = 0.35f, height = 0.2f, color = Color(0xFF0F172A), name = "Wheel FL")
            .copy(position = Vector3(-0.9f, 0.35f, 0.9f), rotation = Vector3(0f, 0f, 90f))
        val w2 = PrimitiveGenerator.generateCylinder(radius = 0.35f, height = 0.2f, color = Color(0xFF0F172A), name = "Wheel FR")
            .copy(position = Vector3(0.9f, 0.35f, 0.9f), rotation = Vector3(0f, 0f, 90f))
        val w3 = PrimitiveGenerator.generateCylinder(radius = 0.35f, height = 0.2f, color = Color(0xFF0F172A), name = "Wheel RL")
            .copy(position = Vector3(-0.9f, 0.35f, -0.9f), rotation = Vector3(0f, 0f, 90f))
        val w4 = PrimitiveGenerator.generateCylinder(radius = 0.35f, height = 0.2f, color = Color(0xFF0F172A), name = "Wheel RR")
            .copy(position = Vector3(0.9f, 0.35f, -0.9f), rotation = Vector3(0f, 0f, 90f))

        return Scene3D(
            meshes = listOf(body, cabin, w1, w2, w3, w4)
        )
    }

    private fun createGeometricPrismScene(): Scene3D {
        val torus = PrimitiveGenerator.generateTorus(mainRadius = 1.1f, tubeRadius = 0.25f, name = "Outer Prism Ring")
            .copy(position = Vector3(0f, 1.0f, 0f), material = Material3D.EmeraldGlass)
        val sphere = PrimitiveGenerator.generateSphere(radius = 0.6f, name = "Inner Core")
            .copy(position = Vector3(0f, 1.0f, 0f), material = Material3D.ChromeSilver)

        return Scene3D(
            meshes = listOf(torus, sphere)
        )
    }
}
