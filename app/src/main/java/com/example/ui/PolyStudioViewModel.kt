package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.*
import com.example.format.*
import com.example.model.*
import com.example.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

enum class StudioToolMode {
    TRANSFORM,
    PRIMITIVES,
    PAINT,
    SKELETON_RIG,
    MOLECULAR_COMPRESS,
    LIGHT_MATERIAL,
    OUTLINER
}

class PolyStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ProjectRepository(db.projectDao())
    }

    private val _sceneState = MutableStateFlow(repository.loadSamplePresetScene("Sci-Fi Mech"))
    val sceneState: StateFlow<Scene3D> = _sceneState.asStateFlow()

    private val _selectedMeshId = MutableStateFlow<String?>(_sceneState.value.meshes.firstOrNull()?.id)
    val selectedMeshId: StateFlow<String?> = _selectedMeshId.asStateFlow()

    private val _selectedBoneId = MutableStateFlow<Int?>(0)
    val selectedBoneId: StateFlow<Int?> = _selectedBoneId.asStateFlow()

    private val _toolMode = MutableStateFlow(StudioToolMode.TRANSFORM)
    val toolMode: StateFlow<StudioToolMode> = _toolMode.asStateFlow()

    private val _compressionOptions = MutableStateFlow(CompressionOptions())
    val compressionOptions: StateFlow<CompressionOptions> = _compressionOptions.asStateFlow()

    private val _compressionReport = MutableStateFlow<CompressionReport?>(null)
    val compressionReport: StateFlow<CompressionReport?> = _compressionReport.asStateFlow()

    private val _compressedPreviewMesh = MutableStateFlow<Mesh3D?>(null)
    val compressedPreviewMesh: StateFlow<Mesh3D?> = _compressedPreviewMesh.asStateFlow()

    private val _comparisonViewActive = MutableStateFlow(false)
    val comparisonViewActive: StateFlow<Boolean> = _comparisonViewActive.asStateFlow()

    private val _brushState = MutableStateFlow(Painter3D.PaintBrush())
    val brushState: StateFlow<Painter3D.PaintBrush> = _brushState.asStateFlow()

    private val _autoSelectMeshOnTouch = MutableStateFlow(true)
    val autoSelectMeshOnTouch: StateFlow<Boolean> = _autoSelectMeshOnTouch.asStateFlow()

    private val _isCameraLocked = MutableStateFlow(false)
    val isCameraLocked: StateFlow<Boolean> = _isCameraLocked.asStateFlow()

    private val _isObjectLocked = MutableStateFlow(false)
    val isObjectLocked: StateFlow<Boolean> = _isObjectLocked.asStateFlow()

    private var turntableJob: kotlinx.coroutines.Job? = null
    private val _isTurntableActive = MutableStateFlow(false)
    val isTurntableActive: StateFlow<Boolean> = _isTurntableActive.asStateFlow()

    // Undo & Redo History
    private val undoStack = java.util.ArrayDeque<Scene3D>()
    private val redoStack = java.util.ArrayDeque<Scene3D>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val savedProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Animation playback ticker
    init {
        viewModelScope.launch {
            while (true) {
                delay(33) // ~30 fps
                val scene = _sceneState.value
                val anim = scene.activeAnimation
                if (anim != null && anim.isPlaying) {
                    val nextTime = anim.currentTime + (0.033f * anim.speed)
                    val updatedAnim = anim.copy(currentTime = nextTime)
                    val updatedSkeleton = SkeletalRigger.evaluateAnimationPose(scene.skeleton, updatedAnim, nextTime)
                    _sceneState.value = scene.copy(
                        skeleton = updatedSkeleton,
                        activeAnimation = updatedAnim
                    )
                }
            }
        }
    }

    fun setToolMode(mode: StudioToolMode) {
        _toolMode.value = mode
        if (mode == StudioToolMode.MOLECULAR_COMPRESS) {
            runMolecularCompression(_compressionOptions.value.targetMolecularParts)
        }
    }

    fun selectMesh(id: String?) {
        _selectedMeshId.value = id
        _sceneState.update { current ->
            current.copy(
                meshes = current.meshes.map { m -> m.copy(isSelected = m.id == id) }
            )
        }
    }

    fun selectBone(boneId: Int?) {
        _selectedBoneId.value = boneId
    }

    fun updateCameraOrbit(deltaYaw: Float, deltaPitch: Float) {
        _sceneState.update { current ->
            val cam = current.camera
            val newYaw = (cam.orbitYaw + deltaYaw) % 360f
            val newPitch = (cam.orbitPitch + deltaPitch).coerceIn(-89f, 89f)
            current.copy(camera = cam.copy(orbitYaw = newYaw, orbitPitch = newPitch))
        }
    }

    fun updateCameraZoom(zoomFactor: Float) {
        _sceneState.update { current ->
            val cam = current.camera
            val newDist = (cam.orbitDistance * zoomFactor).coerceIn(1.5f, 25f)
            current.copy(camera = cam.copy(orbitDistance = newDist))
        }
    }

    fun updateCameraPan(panX: Float, panY: Float) {
        _sceneState.update { current ->
            val cam = current.camera
            val offset = Vector3(panX * 0.01f, panY * 0.01f, 0f)
            current.copy(camera = cam.copy(panOffset = cam.panOffset + offset))
        }
    }

    fun setPresetCameraView(viewName: String) {
        _sceneState.update { current ->
            val cam = when (viewName) {
                "Front" -> current.camera.copy(orbitYaw = 0f, orbitPitch = 0f)
                "Top" -> current.camera.copy(orbitYaw = 0f, orbitPitch = 89f)
                "Right" -> current.camera.copy(orbitYaw = 90f, orbitPitch = 0f)
                "Isometric" -> current.camera.copy(orbitYaw = 45f, orbitPitch = 30f)
                else -> current.camera
            }
            current.copy(camera = cam)
        }
    }

    fun toggleOrthographicProjection() {
        _sceneState.update { current ->
            val isOrtho = !current.camera.isOrthographic
            showToast(if (isOrtho) "📐 Orthographic Projection Enabled" else "🎥 Perspective Projection Enabled")
            current.copy(camera = current.camera.copy(isOrthographic = isOrtho))
        }
    }

    fun toggleGridVisibility() {
        _sceneState.update { current ->
            val show = !current.gridSettings.showGrid
            showToast(if (show) "🌐 Floor Grid Visible" else "🙈 Floor Grid Hidden")
            current.copy(gridSettings = current.gridSettings.copy(showGrid = show))
        }
    }

    fun setBackgroundColor(color: Color) {
        saveUndoSnapshot()
        _sceneState.update { it.copy(backgroundColor = color) }
        showToast("🎨 Viewport Background Color Updated")
    }

    fun toggleTurntable() {
        _isTurntableActive.value = !_isTurntableActive.value
        if (_isTurntableActive.value) {
            turntableJob?.cancel()
            turntableJob = viewModelScope.launch {
                while (isActive && _isTurntableActive.value) {
                    delay(30)
                    updateCameraOrbit(1.2f, 0f)
                }
            }
            showToast("🎠 3D Turntable Spin Started")
        } else {
            turntableJob?.cancel()
            showToast("⏹️ 3D Turntable Paused")
        }
    }

    fun updateLightDirection(x: Float, y: Float, z: Float) {
        _sceneState.update { current ->
            val newDir = Vector3(x, y, z).normalized()
            current.copy(light = current.light.copy(direction = newDir))
        }
    }

    fun updateLightIntensity(intensity: Float, ambient: Float) {
        _sceneState.update { current ->
            current.copy(light = current.light.copy(intensity = intensity, ambientIntensity = ambient))
        }
    }

    fun setGizmoMode(mode: GizmoMode) {
        _sceneState.update { it.copy(gizmoMode = mode) }
    }

    fun setRenderMode(mode: RenderMode) {
        _sceneState.update { it.copy(renderMode = mode) }
    }

    private fun saveUndoSnapshot() {
        val current = _sceneState.value
        if (undoStack.peekLast() != current) {
            if (undoStack.size >= 30) {
                undoStack.removeFirst()
            }
            undoStack.addLast(current)
            redoStack.clear()
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = redoStack.isNotEmpty()
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _sceneState.value
        redoStack.addLast(current)
        val prev = undoStack.removeLast()
        _sceneState.value = prev
        _selectedMeshId.value = prev.meshes.find { it.isSelected }?.id ?: prev.meshes.firstOrNull()?.id
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        showToast("↩️ Undone")
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _sceneState.value
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        _sceneState.value = next
        _selectedMeshId.value = next.meshes.find { it.isSelected }?.id ?: next.meshes.firstOrNull()?.id
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        showToast("↪️ Redone")
    }

    fun updateSelectedMeshPosition(pos: Vector3) {
        val selectedId = _selectedMeshId.value ?: return
        saveUndoSnapshot()
        _sceneState.update { current ->
            current.copy(
                meshes = current.meshes.map { m -> if (m.id == selectedId) m.copy(position = pos) else m }
            )
        }
    }

    fun updateSelectedMeshRotation(rot: Vector3) {
        val selectedId = _selectedMeshId.value ?: return
        saveUndoSnapshot()
        _sceneState.update { current ->
            current.copy(
                meshes = current.meshes.map { m -> if (m.id == selectedId) m.copy(rotation = rot) else m }
            )
        }
    }

    fun updateSelectedMeshScale(scale: Vector3) {
        val selectedId = _selectedMeshId.value ?: return
        saveUndoSnapshot()
        _sceneState.update { current ->
            current.copy(
                meshes = current.meshes.map { m -> if (m.id == selectedId) m.copy(scale = scale) else m }
            )
        }
    }

    fun addPrimitive(type: String) {
        saveUndoSnapshot()
        val newMesh = when (type) {
            "Cube" -> PrimitiveGenerator.generateCube()
            "Sphere" -> PrimitiveGenerator.generateSphere()
            "Cylinder" -> PrimitiveGenerator.generateCylinder()
            "Torus" -> PrimitiveGenerator.generateTorus()
            "Teapot" -> PrimitiveGenerator.generateTeapot()
            "Molecular Cluster" -> PrimitiveGenerator.generateMolecularStructure()
            else -> PrimitiveGenerator.generateCube()
        }

        _sceneState.update { current ->
            current.copy(
                meshes = current.meshes + newMesh
            )
        }
        selectMesh(newMesh.id)
        showToast("Added new $type to 3D scene")
    }

    fun toggleAutoSelectMeshOnTouch() {
        _autoSelectMeshOnTouch.value = !_autoSelectMeshOnTouch.value
        showToast(if (_autoSelectMeshOnTouch.value) "👆 Touch to Select Mesh ENABLED" else "👆 Touch to Select Mesh DISABLED")
    }

    fun setAutoSelectMeshOnTouch(enabled: Boolean) {
        _autoSelectMeshOnTouch.value = enabled
    }

    fun paintMeshAtHit(hit: RaycastHit) {
        val mesh = _sceneState.value.meshes.find { it.id == hit.meshId } ?: return
        if (_autoSelectMeshOnTouch.value && _selectedMeshId.value != hit.meshId) {
            selectMesh(hit.meshId)
        }
        saveUndoSnapshot()
        val paintedMesh = Painter3D.paintAtHitPoint(mesh, hit, _brushState.value)
        _sceneState.update { current ->
            current.copy(
                meshes = current.meshes.map { if (it.id == mesh.id) paintedMesh else it }
            )
        }
    }

    fun applyMaterialPresetToSelected(material: Material3D) {
        val selectedId = _selectedMeshId.value ?: return
        saveUndoSnapshot()
        _sceneState.update { current ->
            current.copy(
                meshes = current.meshes.map { m ->
                    if (m.id == selectedId) Painter3D.applyMaterialToMesh(m, material) else m
                }
            )
        }
        showToast("Applied ${material.name} material")
    }

    fun updateBrushType(type: BrushType) {
        _brushState.update { it.copy(type = type) }
        showToast("${type.icon} Selected ${type.displayName}")
    }

    fun updateBrushColor(color: Color) {
        _brushState.update { it.copy(color = color) }
    }

    fun updateBrushRadius(radius: Float) {
        _brushState.update { it.copy(radius = radius) }
    }

    fun updateBrushOpacity(opacity: Float) {
        _brushState.update { it.copy(opacity = opacity) }
    }

    fun updateWatermarkPattern(pattern: String) {
        _brushState.update { it.copy(watermarkPattern = pattern) }
        showToast("Watermark Pattern: $pattern")
    }

    // Molecular Part Model Compression
    fun runMolecularCompression(targetParts: Int) {
        val selectedId = _selectedMeshId.value
        val meshToCompress = _sceneState.value.meshes.find { it.id == selectedId } ?: _sceneState.value.meshes.firstOrNull()
        if (meshToCompress == null) return

        val opts = _compressionOptions.value.copy(targetMolecularParts = targetParts)
        _compressionOptions.value = opts

        viewModelScope.launch(Dispatchers.Default) {
            val (compressedMesh, report) = PolygonReductionEngine.compressByMolecularParts(meshToCompress, opts)
            _compressedPreviewMesh.value = compressedMesh
            _compressionReport.value = report
        }
    }

    fun applyMolecularCompressionToScene() {
        val compressed = _compressedPreviewMesh.value ?: return
        val report = _compressionReport.value ?: return

        saveUndoSnapshot()
        _sceneState.update { current ->
            val updatedMeshes = current.meshes.map { m ->
                if (m.isSelected || m.id == _selectedMeshId.value) compressed else m
            }
            current.copy(meshes = updatedMeshes)
        }
        selectMesh(compressed.id)
        showToast("Molecular Compression Applied! Reduced polygons by ${report.reductionPercentage.toInt()}%")
    }

    fun toggleComparisonView(active: Boolean) {
        _comparisonViewActive.value = active
    }

    // Skeletal Rigging
    fun updateSelectedBoneRotation(rot: Vector3) {
        val boneId = _selectedBoneId.value ?: return
        saveUndoSnapshot()
        _sceneState.update { current ->
            val updatedSk = SkeletalRigger.updateBoneRotation(current.skeleton, boneId, rot)
            current.copy(skeleton = updatedSk)
        }
    }

    fun addBoneToSkeleton() {
        saveUndoSnapshot()
        _sceneState.update { current ->
            val sk = current.skeleton
            val newId = sk.bones.size
            val parentId = _selectedBoneId.value ?: -1
            val newBone = Bone(
                id = newId,
                name = "Bone_$newId",
                parentId = parentId,
                length = 0.8f,
                restPosition = Vector3(0f, 0.8f, 0f)
            )
            current.copy(skeleton = sk.copy(bones = sk.bones + newBone))
        }
        selectBone(_sceneState.value.skeleton.bones.lastOrNull()?.id)
        showToast("Added new bone joint")
    }

    fun autoSkinMeshToSkeleton() {
        val selectedId = _selectedMeshId.value ?: return
        saveUndoSnapshot()
        _sceneState.update { current ->
            current.copy(
                meshes = current.meshes.map { m ->
                    if (m.id == selectedId) SkeletalRigger.autoSkinMeshToSkeleton(m, current.skeleton) else m
                }
            )
        }
        showToast("Mesh vertices skinned to skeleton bones!")
    }

    fun toggleAnimationPlay() {
        _sceneState.update { current ->
            val anim = current.activeAnimation ?: AnimationTrack(id = "anim", name = "Track 1")
            current.copy(activeAnimation = anim.copy(isPlaying = !anim.isPlaying))
        }
    }

    fun toggleCameraLock() {
        _isCameraLocked.value = !_isCameraLocked.value
        showToast(if (_isCameraLocked.value) "🔒 Camera Viewport Locked" else "🔓 Camera Viewport Unlocked")
    }

    fun setCameraLock(locked: Boolean) {
        _isCameraLocked.value = locked
    }

    fun toggleObjectLock() {
        _isObjectLocked.value = !_isObjectLocked.value
        showToast(if (_isObjectLocked.value) "🔒 Mesh Transform Locked" else "🔓 Mesh Transform Unlocked")
    }

    fun setObjectLock(locked: Boolean) {
        _isObjectLocked.value = locked
    }

    // Import / Export
    fun import3DFile(stream: InputStream, filename: String, appendToScene: Boolean = false) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val bytes = stream.readBytes()
                val extension = filename.substringAfterLast(".").lowercase()

                val importedScene = when {
                    extension == "glb" || extension == "gltf" -> GlbFormat.parseGlb(bytes.inputStream(), filename)
                    extension == "fbx" -> FbxFormat.parseFbx(bytes.inputStream(), filename)
                    extension == "obj" -> ObjFormat.parseObj(String(bytes, Charsets.UTF_8), filename) ?: GlbFormat.parseGlb(bytes.inputStream(), filename)
                    else -> GlbFormat.parseGlb(bytes.inputStream(), filename)
                }

                val normalizedMeshes = importedScene.meshes.mapIndexed { idx, m ->
                    val centeredMesh = normalizeMeshScaleAndCenter(m)
                    if (appendToScene) {
                        centeredMesh.copy(
                            id = "imported_${System.currentTimeMillis()}_$idx",
                            name = "${centeredMesh.name}_$idx"
                        )
                    } else {
                        centeredMesh
                    }
                }

                if (appendToScene) {
                    _sceneState.update { current ->
                        current.copy(meshes = current.meshes + normalizedMeshes)
                    }
                    _selectedMeshId.value = normalizedMeshes.firstOrNull()?.id
                    showToast("Added ${normalizedMeshes.size} mesh(es) from $filename into scene!")
                } else {
                    _sceneState.value = importedScene.copy(meshes = normalizedMeshes)
                    _selectedMeshId.value = normalizedMeshes.firstOrNull()?.id
                    showToast("Opened $filename! Ready for editing, painting & rigging.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Import error: ${e.localizedMessage ?: "Invalid 3D format"}")
            }
        }
    }

    private fun normalizeMeshScaleAndCenter(mesh: Mesh3D): Mesh3D {
        if (mesh.vertices.isEmpty()) return mesh
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        mesh.vertices.forEach { v ->
            minX = minOf(minX, v.position.x)
            minY = minOf(minY, v.position.y)
            minZ = minOf(minZ, v.position.z)
            maxX = maxOf(maxX, v.position.x)
            maxY = maxOf(maxY, v.position.y)
            maxZ = maxOf(maxZ, v.position.z)
        }

        val width = maxX - minX
        val height = maxY - minY
        val depth = maxZ - minZ
        val maxDim = maxOf(width, maxOf(height, depth))

        val targetSize = 2.0f
        val scaleFactor = if (maxDim > 15f || maxDim < 0.1f) targetSize / maxOf(maxDim, 0.001f) else 1.0f

        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val centerZ = (minZ + maxZ) / 2f

        val normalizedVertices = mesh.vertices.map { v ->
            v.copy(
                position = Vector3(
                    (v.position.x - centerX) * scaleFactor,
                    (v.position.y - centerY) * scaleFactor,
                    (v.position.z - centerZ) * scaleFactor
                )
            )
        }

        return mesh.copy(vertices = normalizedVertices)
    }

    fun exportGlbBytes(): ByteArray {
        return GlbFormat.exportGlb(_sceneState.value)
    }

    fun exportFbxString(): String {
        return FbxFormat.exportFbx(_sceneState.value)
    }

    fun exportObjString(): String {
        return ObjFormat.exportObj(_sceneState.value)
    }

    fun loadSamplePreset(presetName: String) {
        saveUndoSnapshot()
        val scene = repository.loadSamplePresetScene(presetName)
        _sceneState.value = scene
        _selectedMeshId.value = scene.meshes.firstOrNull()?.id
        showToast("Loaded $presetName model")
    }

    fun saveCurrentProjectToDb() {
        viewModelScope.launch {
            val scene = _sceneState.value
            val totalPolys = scene.meshes.sumOf { it.faces.size }
            val totalVerts = scene.meshes.sumOf { it.vertices.size }
            val mainName = scene.meshes.firstOrNull()?.name ?: "PolyStudio Project"

            val projectEntity = ProjectEntity(
                name = mainName,
                format = "GLB/FBX",
                polyCount = totalPolys,
                vertexCount = totalVerts,
                molecularParts = _compressionOptions.value.targetMolecularParts
            )
            repository.saveProject(projectEntity)
            showToast("Project '$mainName' saved locally!")
        }
    }

    fun deleteMesh(id: String) {
        saveUndoSnapshot()
        _sceneState.update { current ->
            current.copy(meshes = current.meshes.filterNot { it.id == id })
        }
        if (_selectedMeshId.value == id) {
            _selectedMeshId.value = _sceneState.value.meshes.firstOrNull()?.id
        }
        showToast("Mesh deleted")
    }

    fun duplicateMesh(id: String) {
        val mesh = _sceneState.value.meshes.find { it.id == id } ?: return
        saveUndoSnapshot()
        val copyMesh = mesh.copy(
            id = "${mesh.id}_copy_${System.currentTimeMillis()}",
            name = "${mesh.name} Copy",
            position = mesh.position + Vector3(0.5f, 0f, 0.5f)
        )
        _sceneState.update { it.copy(meshes = it.meshes + copyMesh) }
        selectMesh(copyMesh.id)
        showToast("Duplicated ${mesh.name}")
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
