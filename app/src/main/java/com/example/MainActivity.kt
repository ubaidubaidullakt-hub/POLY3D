package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PolyStudioViewModel
import com.example.ui.StudioToolMode
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme

import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                PolyStudio3DApp()
            }
        }
    }
}

@Composable
fun PolyStudio3DApp(
    polyViewModel: PolyStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val sceneState by polyViewModel.sceneState.collectAsStateWithLifecycle()
    val selectedMeshId by polyViewModel.selectedMeshId.collectAsStateWithLifecycle()
    val selectedBoneId by polyViewModel.selectedBoneId.collectAsStateWithLifecycle()
    val toolMode by polyViewModel.toolMode.collectAsStateWithLifecycle()

    val compressionOptions by polyViewModel.compressionOptions.collectAsStateWithLifecycle()
    val compressionReport by polyViewModel.compressionReport.collectAsStateWithLifecycle()
    val compressedPreviewMesh by polyViewModel.compressedPreviewMesh.collectAsStateWithLifecycle()
    val isComparisonActive by polyViewModel.comparisonViewActive.collectAsStateWithLifecycle()

    val brushState by polyViewModel.brushState.collectAsStateWithLifecycle()
    val toastMessage by polyViewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var appendImportMode by remember { mutableStateOf(false) }

    // File picker launcher for importing local GLB, FBX, OBJ files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val filename = queryFileName(context, uri)
                if (inputStream != null) {
                    polyViewModel.import3DFile(inputStream, filename, appendToScene = appendImportMode)
                } else {
                    polyViewModel.showToast("Unable to open file stream")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                polyViewModel.showToast("Failed to open file: ${e.localizedMessage}")
            }
        }
    }

    val openLocalFilePicker = { appendToScene: Boolean ->
        appendImportMode = appendToScene
        filePickerLauncher.launch(arrayOf("*/*"))
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            polyViewModel.clearToast()
        }
    }

    val selectedMesh = sceneState.meshes.find { it.id == selectedMeshId } ?: sceneState.meshes.firstOrNull()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VibrantBg,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                TopStudioBar(
                    viewModel = polyViewModel,
                    sceneRenderMode = sceneState.renderMode,
                    onOpenImportExportDialog = { showImportExportDialog = true },
                    onPickLocalFile = { openLocalFilePicker(false) }
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(VibrantSurface)
            ) {
                // Active Tool Panel Surface
                Surface(
                    color = VibrantSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (toolMode) {
                        StudioToolMode.TRANSFORM -> TransformPanel(
                            viewModel = polyViewModel,
                            selectedMesh = selectedMesh
                        )
                        StudioToolMode.PRIMITIVES -> PrimitivesPanel(
                            viewModel = polyViewModel
                        )
                        StudioToolMode.PAINT -> PaintPanel(
                            viewModel = polyViewModel,
                            brushState = brushState
                        )
                        StudioToolMode.SKELETON_RIG -> SkeletonPanel(
                            viewModel = polyViewModel,
                            skeleton = sceneState.skeleton,
                            selectedBoneId = selectedBoneId,
                            isPlayingAnimation = sceneState.activeAnimation?.isPlaying == true
                        )
                        StudioToolMode.MOLECULAR_COMPRESS -> MolecularCompressPanel(
                            viewModel = polyViewModel,
                            options = compressionOptions,
                            report = compressionReport,
                            isComparisonActive = isComparisonActive,
                            onOpenExportDialog = { showImportExportDialog = true }
                        )
                        StudioToolMode.LIGHT_MATERIAL -> LightingMaterialPanel(
                            viewModel = polyViewModel,
                            light = sceneState.light
                        )
                        StudioToolMode.OUTLINER -> OutlinerPanel(
                            viewModel = polyViewModel,
                            meshes = sceneState.meshes,
                            selectedMeshId = selectedMeshId
                        )
                    }
                }

                // Toolbar Mode Selector Tabs
                ToolbarModeSelector(
                    activeMode = toolMode,
                    onSelectMode = { polyViewModel.setToolMode(it) }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Viewport3D(
                viewModel = polyViewModel,
                scene = sceneState,
                selectedMeshId = selectedMeshId,
                toolMode = toolMode,
                compressedMesh = compressedPreviewMesh,
                compressionReport = compressionReport,
                isComparisonActive = isComparisonActive,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showImportExportDialog) {
            ImportExportDialog(
                viewModel = polyViewModel,
                onDismiss = { showImportExportDialog = false },
                onPickLocalFile = openLocalFilePicker
            )
        }
    }
}

private fun queryFileName(context: android.content.Context, uri: android.net.Uri): String {
    var name = "ImportedModel.glb"
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    val displayName = it.getString(index)
                    if (!displayName.isNullOrBlank()) {
                        name = displayName
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    if (name == "ImportedModel.glb") {
        uri.path?.let { p ->
            val last = p.substringAfterLast("/")
            if (last.contains(".")) {
                name = last
            }
        }
    }
    return name
}
