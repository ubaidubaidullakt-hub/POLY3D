package com.example.tools

import com.example.model.*

object SkeletalRigger {

    fun autoSkinMeshToSkeleton(mesh: Mesh3D, skeleton: Skeleton): Mesh3D {
        if (skeleton.bones.isEmpty()) return mesh

        val updatedVertices = mesh.vertices.map { vertex ->
            val worldPos = mesh.getModelMatrix().transformVector(vertex.position)

            var bestBoneIdx = 0
            var minDistSq = Float.MAX_VALUE

            skeleton.bones.forEachIndexed { idx, bone ->
                val boneWorldMat = skeleton.getBoneWorldMatrix(bone.id)
                val bonePos = boneWorldMat.transformVector(Vector3.Zero)
                val dSq = worldPos.distance(bonePos)
                if (dSq < minDistSq) {
                    minDistSq = dSq
                    bestBoneIdx = idx
                }
            }

            vertex.copy(
                boneIndices = intArrayOf(bestBoneIdx, 0, 0, 0),
                boneWeights = floatArrayOf(1.0f, 0f, 0f, 0f)
            )
        }

        return mesh.copy(vertices = updatedVertices)
    }

    fun updateBoneRotation(
        skeleton: Skeleton,
        boneId: Int,
        newRotation: Vector3
    ): Skeleton {
        val updatedBones = skeleton.bones.map { bone ->
            if (bone.id == boneId) {
                bone.copy(rotation = newRotation)
            } else {
                bone
            }
        }
        return skeleton.copy(bones = updatedBones)
    }

    fun evaluateAnimationPose(
        skeleton: Skeleton,
        anim: AnimationTrack,
        timeSeconds: Float
    ): Skeleton {
        if (anim.keyframes.isEmpty()) return skeleton

        val time = timeSeconds % anim.durationSeconds
        val updatedBones = skeleton.bones.map { bone ->
            val boneFrames = anim.keyframes.filter { it.boneId == bone.id }.sortedBy { it.timeSeconds }
            if (boneFrames.isNotEmpty()) {
                val activeFrame = boneFrames.lastOrNull { it.timeSeconds <= time } ?: boneFrames.first()
                bone.copy(
                    position = activeFrame.position,
                    rotation = activeFrame.rotation
                )
            } else {
                bone
            }
        }

        return skeleton.copy(bones = updatedBones)
    }
}
