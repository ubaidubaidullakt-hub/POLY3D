package com.example.model

data class Bone(
    val id: Int,
    val name: String,
    val parentId: Int = -1,
    val length: Float = 1.0f,
    val restPosition: Vector3 = Vector3.Zero,
    val restRotation: Vector3 = Vector3.Zero, // Degrees X, Y, Z
    val position: Vector3 = restPosition,
    val rotation: Vector3 = restRotation
) {
    fun getLocalTransformMatrix(): Matrix4 {
        val tMat = Matrix4.translation(position)
        val rMat = Matrix4.eulerRotation(rotation)
        return tMat * rMat
    }
}

data class Skeleton(
    val bones: List<Bone> = emptyList()
) {
    fun getBoneWorldMatrix(boneId: Int): Matrix4 {
        val bone = bones.find { it.id == boneId } ?: return Matrix4.identity()
        val localMat = bone.getLocalTransformMatrix()
        return if (bone.parentId >= 0 && bone.parentId != boneId) {
            getBoneWorldMatrix(bone.parentId) * localMat
        } else {
            localMat
        }
    }
}

data class BoneKeyframe(
    val boneId: Int,
    val timeSeconds: Float,
    val position: Vector3,
    val rotation: Vector3
)

data class AnimationTrack(
    val id: String,
    val name: String,
    val durationSeconds: Float = 2.0f,
    val keyframes: List<BoneKeyframe> = emptyList(),
    val isPlaying: Boolean = false,
    val currentTime: Float = 0f,
    val speed: Float = 1.0f,
    val isLooping: Boolean = true
)
