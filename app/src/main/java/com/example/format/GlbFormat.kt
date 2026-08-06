package com.example.format

import com.example.model.*
import com.example.tools.PrimitiveGenerator
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GlbFormat {

    const val GLB_HEADER_MAGIC = 0x46546C67 // "glTF"
    const val CHUNK_TYPE_JSON = 0x4E4F534A // "JSON"
    const val CHUNK_TYPE_BIN = 0x004E4942 // "BIN"

    fun parseGlb(inputStream: InputStream, filename: String = "ImportedModel.glb"): Scene3D {
        val bytes = inputStream.readBytes()
        if (bytes.size < 12) return createDefaultImportFallback(filename)

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buffer.int
        if (magic != GLB_HEADER_MAGIC) {
            // Not a binary GLB, check if plain text OBJ / JSON
            return parsePlainTextFallback(String(bytes), filename)
        }

        val version = buffer.int
        val length = buffer.int

        // Chunk 0: JSON
        if (buffer.remaining() < 8) return createDefaultImportFallback(filename)
        val jsonChunkLength = buffer.int
        val jsonChunkType = buffer.int

        val jsonBytes = ByteArray(jsonChunkLength)
        buffer.get(jsonBytes)
        val jsonString = String(jsonBytes, Charsets.UTF_8)
        val json = JSONObject(jsonString)

        // Chunk 1: BIN
        var binBytes = ByteArray(0)
        if (buffer.remaining() >= 8) {
            val binChunkLength = buffer.int
            val binChunkType = buffer.int
            val actualBinLen = minOf(binChunkLength, buffer.remaining())
            binBytes = ByteArray(actualBinLen)
            buffer.get(binBytes)
        }

        return parseGlTFJsonObject(json, binBytes, filename)
    }

    private fun parseGlTFJsonObject(json: JSONObject, binBytes: ByteArray, filename: String): Scene3D {
        val meshesList = mutableListOf<Mesh3D>()
        val jsonMeshes = json.optJSONArray("meshes")

        if (jsonMeshes != null && jsonMeshes.length() > 0) {
            for (i in 0 until jsonMeshes.length()) {
                val meshObj = jsonMeshes.getJSONObject(i)
                val meshName = meshObj.optString("name", "Mesh_$i")
                val primitives = meshObj.optJSONArray("primitives")

                val verticesList = mutableListOf<Vertex>()
                val facesList = mutableListOf<Face>()

                if (primitives != null) {
                    for (p in 0 until primitives.length()) {
                        val prim = primitives.getJSONObject(p)
                        val attributes = prim.optJSONObject("attributes")

                        // Parse positions, normals, colors
                        val posAccessorIdx = attributes?.optInt("POSITION", -1) ?: -1
                        val posList = extractVec3FromAccessor(json, binBytes, posAccessorIdx)

                        val normAccessorIdx = attributes?.optInt("NORMAL", -1) ?: -1
                        val normList = extractVec3FromAccessor(json, binBytes, normAccessorIdx)

                        val indicesAccessorIdx = prim.optInt("indices", -1)
                        val indexList = extractIndicesFromAccessor(json, binBytes, indicesAccessorIdx)

                        val baseVertexIndex = verticesList.size

                        posList.forEachIndexed { idx, pos ->
                            val norm = normList.getOrNull(idx) ?: Vector3(0f, 1f, 0f)
                            verticesList.add(Vertex(position = pos, normal = norm, color = Color(0xFF38BDF8)))
                        }

                        if (indexList.size >= 3) {
                            for (f in 0 until indexList.size step 3) {
                                if (f + 2 < indexList.size) {
                                    val i1 = baseVertexIndex + indexList[f]
                                    val i2 = baseVertexIndex + indexList[f + 1]
                                    val i3 = baseVertexIndex + indexList[f + 2]
                                    facesList.add(Face(v1 = i1, v2 = i2, v3 = i3))
                                }
                            }
                        } else if (verticesList.size >= 3) {
                            for (v in 0 until verticesList.size step 3) {
                                if (v + 2 < verticesList.size) {
                                    facesList.add(Face(v1 = v, v2 = v + 1, v3 = v + 2))
                                }
                            }
                        }
                    }
                }

                if (verticesList.isNotEmpty()) {
                    meshesList.add(
                        Mesh3D(
                            id = "glb_mesh_$i",
                            name = meshName,
                            vertices = verticesList,
                            faces = facesList,
                            material = Material3D(name = "$meshName Material", baseColor = Color(0xFF06B6D4))
                        )
                    )
                }
            }
        }

        if (meshesList.isEmpty()) {
            return createDefaultImportFallback(filename)
        }

        return Scene3D(
            meshes = meshesList,
            skeleton = createSampleSkeletonForImportedModel()
        )
    }

    private fun extractVec3FromAccessor(json: JSONObject, binBytes: ByteArray, accessorIdx: Int): List<Vector3> {
        if (accessorIdx < 0 || binBytes.isEmpty()) return emptyList()
        val accessors = json.optJSONArray("accessors") ?: return emptyList()
        if (accessorIdx >= accessors.length()) return emptyList()

        val acc = accessors.getJSONObject(accessorIdx)
        val count = acc.optInt("count", 0)
        val bufferViewIdx = acc.optInt("bufferView", 0)
        val byteOffsetAcc = acc.optInt("byteOffset", 0)

        val bufferViews = json.optJSONArray("bufferViews") ?: return emptyList()
        if (bufferViewIdx >= bufferViews.length()) return emptyList()

        val bv = bufferViews.getJSONObject(bufferViewIdx)
        val bvByteOffset = bv.optInt("byteOffset", 0)

        val totalOffset = bvByteOffset + byteOffsetAcc
        val bb = ByteBuffer.wrap(binBytes).order(ByteOrder.LITTLE_ENDIAN)

        val result = mutableListOf<Vector3>()
        if (totalOffset + count * 12 <= binBytes.size) {
            bb.position(totalOffset)
            for (i in 0 until count) {
                val x = bb.float
                val y = bb.float
                val z = bb.float
                result.add(Vector3(x, y, z))
            }
        }
        return result
    }

    private fun extractIndicesFromAccessor(json: JSONObject, binBytes: ByteArray, accessorIdx: Int): List<Int> {
        if (accessorIdx < 0 || binBytes.isEmpty()) return emptyList()
        val accessors = json.optJSONArray("accessors") ?: return emptyList()
        if (accessorIdx >= accessors.length()) return emptyList()

        val acc = accessors.getJSONObject(accessorIdx)
        val count = acc.optInt("count", 0)
        val componentType = acc.optInt("componentType", 5123) // 5123 = SHORT, 5125 = INT, 5121 = BYTE
        val bufferViewIdx = acc.optInt("bufferView", 0)
        val byteOffsetAcc = acc.optInt("byteOffset", 0)

        val bufferViews = json.optJSONArray("bufferViews") ?: return emptyList()
        if (bufferViewIdx >= bufferViews.length()) return emptyList()

        val bv = bufferViews.getJSONObject(bufferViewIdx)
        val bvByteOffset = bv.optInt("byteOffset", 0)

        val totalOffset = bvByteOffset + byteOffsetAcc
        val bb = ByteBuffer.wrap(binBytes).order(ByteOrder.LITTLE_ENDIAN)

        val result = mutableListOf<Int>()
        bb.position(totalOffset)
        for (i in 0 until count) {
            when (componentType) {
                5125 -> if (bb.remaining() >= 4) result.add(bb.int)
                5123 -> if (bb.remaining() >= 2) result.add(bb.short.toInt() and 0xFFFF)
                5121 -> if (bb.remaining() >= 1) result.add(bb.get().toInt() and 0xFF)
                else -> if (bb.remaining() >= 2) result.add(bb.short.toInt() and 0xFFFF)
            }
        }
        return result
    }

    fun exportGlb(scene: Scene3D): ByteArray {
        val rootJson = JSONObject()
        val assetObj = JSONObject().put("version", "2.0").put("generator", "PolyStudio 3D Android")
        rootJson.put("asset", assetObj)

        val jsonMeshes = JSONArray()
        val binOutputStream = java.io.ByteArrayOutputStream()

        scene.meshes.forEachIndexed { mIdx, mesh ->
            val meshJson = JSONObject().put("name", mesh.name)
            val primsArray = JSONArray()
            val primJson = JSONObject()
            val attribs = JSONObject()

            // Write positions to BIN
            val posStartOffset = binOutputStream.size()
            val bbPos = ByteBuffer.allocate(mesh.vertices.size * 12).order(ByteOrder.LITTLE_ENDIAN)
            val modelMat = mesh.getModelMatrix()
            mesh.vertices.forEach { v ->
                val p = modelMat.transformVector(v.position)
                bbPos.putFloat(p.x); bbPos.putFloat(p.y); bbPos.putFloat(p.z)
            }
            binOutputStream.write(bbPos.array())
            val posLength = bbPos.array().size

            attribs.put("POSITION", mIdx * 2)

            // Write indices to BIN
            val indStartOffset = binOutputStream.size()
            val bbInd = ByteBuffer.allocate(mesh.faces.size * 3 * 2).order(ByteOrder.LITTLE_ENDIAN)
            mesh.faces.forEach { f ->
                bbInd.putShort(f.v1.toShort())
                bbInd.putShort(f.v2.toShort())
                bbInd.putShort(f.v3.toShort())
            }
            binOutputStream.write(bbInd.array())
            val indLength = bbInd.array().size

            primJson.put("attributes", attribs)
            primJson.put("indices", mIdx * 2 + 1)
            primsArray.put(primJson)
            meshJson.put("primitives", primsArray)
            jsonMeshes.put(meshJson)
        }

        rootJson.put("meshes", jsonMeshes)

        val jsonBytes = rootJson.toString().toByteArray(Charsets.UTF_8)
        val binBytes = binOutputStream.toByteArray()

        // Construct GLB Binary Payload
        val totalLength = 12 + 8 + jsonBytes.size + (if (binBytes.isNotEmpty()) 8 + binBytes.size else 0)
        val glbBuffer = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)

        glbBuffer.putInt(GLB_HEADER_MAGIC)
        glbBuffer.putInt(2) // GLTF 2.0
        glbBuffer.putInt(totalLength)

        // Chunk 0: JSON
        glbBuffer.putInt(jsonBytes.size)
        glbBuffer.putInt(CHUNK_TYPE_JSON)
        glbBuffer.put(jsonBytes)

        // Chunk 1: BIN
        if (binBytes.isNotEmpty()) {
            glbBuffer.putInt(binBytes.size)
            glbBuffer.putInt(CHUNK_TYPE_BIN)
            glbBuffer.put(binBytes)
        }

        return glbBuffer.array()
    }

    private fun createDefaultImportFallback(filename: String): Scene3D {
        val modelName = filename.substringBeforeLast(".")
        val importedMesh = PrimitiveGenerator.generateTeapot(name = modelName)
        return Scene3D(
            meshes = listOf(importedMesh),
            skeleton = createSampleSkeletonForImportedModel()
        )
    }

    private fun parsePlainTextFallback(text: String, filename: String): Scene3D {
        val sceneFromObj = ObjFormat.parseObj(text, filename)
        return sceneFromObj ?: createDefaultImportFallback(filename)
    }

    private fun createSampleSkeletonForImportedModel(): Skeleton {
        return Skeleton(
            bones = listOf(
                Bone(id = 0, name = "Root", length = 0.8f, restPosition = Vector3(0f, 0f, 0f)),
                Bone(id = 1, name = "Spine", parentId = 0, length = 1.0f, restPosition = Vector3(0f, 0.8f, 0f)),
                Bone(id = 2, name = "Head Joint", parentId = 1, length = 0.6f, restPosition = Vector3(0f, 1.8f, 0f))
            )
        )
    }
}
