package com.example.model

import kotlin.math.*

data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector3(x / scalar, y / scalar, z / scalar) else Vector3()
    operator fun unaryMinus() = Vector3(-x, -y, -z)

    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3) = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun lengthSquared() = x * x + y * y + z * z
    fun length() = sqrt(lengthSquared())

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 0.00001f) this / len else Vector3()
    }

    fun distance(other: Vector3) = (this - other).length()

    fun lerp(other: Vector3, t: Float) = Vector3(
        x + (other.x - x) * t,
        y + (other.y - y) * t,
        z + (other.z - z) * t
    )

    companion object {
        val Zero = Vector3(0f, 0f, 0f)
        val One = Vector3(1f, 1f, 1f)
        val Up = Vector3(0f, 1f, 0f)
        val Right = Vector3(1f, 0f, 0f)
        val Forward = Vector3(0f, 0f, 1f)
    }
}

data class Vector2(
    val x: Float = 0f,
    val y: Float = 0f
) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2(x * scalar, y * scalar)
    fun length() = sqrt(x * x + y * y)
}

data class Vector4(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 1f
)

class Matrix4(val m: FloatArray = FloatArray(16)) {

    init {
        if (m.all { it == 0f }) {
            // default identity
            m[0] = 1f; m[5] = 1f; m[10] = 1f; m[15] = 1f
        }
    }

    operator fun times(other: Matrix4): Matrix4 {
        val res = FloatArray(16)
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += m[row + k * 4] * other.m[k + col * 4]
                }
                res[row + col * 4] = sum
            }
        }
        return Matrix4(res)
    }

    fun transformVector(v: Vector3): Vector3 {
        val rx = m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12]
        val ry = m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13]
        val rz = m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14]
        val rw = m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15]
        return if (rw != 0f && rw != 1f) Vector3(rx / rw, ry / rw, rz / rw) else Vector3(rx, ry, rz)
    }

    fun transformDirection(v: Vector3): Vector3 {
        val rx = m[0] * v.x + m[4] * v.y + m[8] * v.z
        val ry = m[1] * v.x + m[5] * v.y + m[9] * v.z
        val rz = m[2] * v.x + m[6] * v.y + m[10] * v.z
        return Vector3(rx, ry, rz).normalized()
    }

    companion object {
        fun identity() = Matrix4(floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        ))

        fun translation(v: Vector3) = Matrix4(floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            v.x, v.y, v.z, 1f
        ))

        fun scale(v: Vector3) = Matrix4(floatArrayOf(
            v.x, 0f, 0f, 0f,
            0f, v.y, 0f, 0f,
            0f, 0f, v.z, 0f,
            0f, 0f, 0f, 1f
        ))

        fun rotationX(degrees: Float): Matrix4 {
            val rad = Math.toRadians(degrees.toDouble()).toFloat()
            val cos = cos(rad)
            val sin = sin(rad)
            return Matrix4(floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, cos, sin, 0f,
                0f, -sin, cos, 0f,
                0f, 0f, 0f, 1f
            ))
        }

        fun rotationY(degrees: Float): Matrix4 {
            val rad = Math.toRadians(degrees.toDouble()).toFloat()
            val cos = cos(rad)
            val sin = sin(rad)
            return Matrix4(floatArrayOf(
                cos, 0f, -sin, 0f,
                0f, 1f, 0f, 0f,
                sin, 0f, cos, 0f,
                0f, 0f, 0f, 1f
            ))
        }

        fun rotationZ(degrees: Float): Matrix4 {
            val rad = Math.toRadians(degrees.toDouble()).toFloat()
            val cos = cos(rad)
            val sin = sin(rad)
            return Matrix4(floatArrayOf(
                cos, sin, 0f, 0f,
                -sin, cos, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            ))
        }

        fun eulerRotation(rotDegrees: Vector3): Matrix4 {
            return rotationY(rotDegrees.y) * rotationX(rotDegrees.x) * rotationZ(rotDegrees.z)
        }

        fun perspective(fovYDeg: Float, aspect: Float, zNear: Float, zFar: Float): Matrix4 {
            val f = (1.0 / tan(Math.toRadians(fovYDeg.toDouble() / 2.0))).toFloat()
            val rangeInv = 1.0f / (zNear - zFar)
            return Matrix4(floatArrayOf(
                f / aspect, 0f, 0f, 0f,
                0f, f, 0f, 0f,
                0f, 0f, (zNear + zFar) * rangeInv, -1f,
                0f, 0f, 2f * zNear * zFar * rangeInv, 0f
            ))
        }

        fun orthographic(left: Float, right: Float, bottom: Float, top: Float, zNear: Float, zFar: Float): Matrix4 {
            val rMinusL = right - left
            val tMinusB = top - bottom
            val fMinusN = zFar - zNear
            return Matrix4(floatArrayOf(
                2f / rMinusL, 0f, 0f, 0f,
                0f, 2f / tMinusB, 0f, 0f,
                0f, 0f, -2f / fMinusN, 0f,
                -(right + left) / rMinusL, -(top + bottom) / tMinusB, -(zFar + zNear) / fMinusN, 1f
            ))
        }

        fun lookAt(eye: Vector3, target: Vector3, up: Vector3): Matrix4 {
            val zAxis = (eye - target).normalized()
            val xAxis = up.cross(zAxis).normalized()
            val yAxis = zAxis.cross(xAxis)

            return Matrix4(floatArrayOf(
                xAxis.x, yAxis.x, zAxis.x, 0f,
                xAxis.y, yAxis.y, zAxis.y, 0f,
                xAxis.z, yAxis.z, zAxis.z, 0f,
                -xAxis.dot(eye), -yAxis.dot(eye), -zAxis.dot(eye), 1f
            ))
        }
    }
}

data class Quaternion(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 1f
) {
    fun toMatrix(): Matrix4 {
        val xx = x * x; val yy = y * y; val zz = z * z
        val xy = x * y; val xz = x * z; val yz = y * z
        val wx = w * x; val wy = w * y; val wz = w * z

        return Matrix4(floatArrayOf(
            1f - 2f * (yy + zz), 2f * (xy + wz), 2f * (xz - wy), 0f,
            2f * (xy - wz), 1f - 2f * (xx + zz), 2f * (yz + wx), 0f,
            2f * (xz + wy), 2f * (yz - wx), 1f - 2f * (xx + yy), 0f,
            0f, 0f, 0f, 1f
        ))
    }

    companion object {
        val Identity = Quaternion(0f, 0f, 0f, 1f)

        fun fromEulerDegrees(euler: Vector3): Quaternion {
            val rx = Math.toRadians(euler.x.toDouble() * 0.5).toFloat()
            val ry = Math.toRadians(euler.y.toDouble() * 0.5).toFloat()
            val rz = Math.toRadians(euler.z.toDouble() * 0.5).toFloat()

            val c1 = cos(ry); val s1 = sin(ry)
            val c2 = cos(rx); val s2 = sin(rx)
            val c3 = cos(rz); val s3 = sin(rz)

            return Quaternion(
                x = s1 * s2 * c3 + c1 * c2 * s3,
                y = s1 * c2 * c3 + c1 * s2 * s3,
                z = c1 * s2 * c3 - s1 * c2 * s3,
                w = c1 * c2 * c3 - s1 * s2 * s3
            )
        }
    }
}

data class BoundingBox(
    val min: Vector3 = Vector3(-1f, -1f, -1f),
    val max: Vector3 = Vector3(1f, 1f, 1f)
) {
    val size: Vector3 get() = max - min
    val center: Vector3 get() = (min + max) * 0.5f
    val width: Float get() = max.x - min.x
    val height: Float get() = max.y - min.y
    val depth: Float get() = max.z - min.z
}

data class Ray3D(
    val origin: Vector3,
    val direction: Vector3
)

data class RaycastHit(
    val hitPoint: Vector3,
    val distance: Float,
    val faceIndex: Int,
    val meshId: String
)
