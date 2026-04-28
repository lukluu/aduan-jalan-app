package com.example.aduanjalan.ui.report

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.aduanjalan.ui.detection.DetectionViewModel
import com.example.aduanjalan.ui.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun CameraScreen(
    navController: NavHostController,
    viewModel: DetectionViewModel = hiltViewModel()
) {
    LockToFullSensor()
    val context = LocalContext.current
    val lensFacing = remember { CameraSelector.LENS_FACING_BACK }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    if (!hasCameraPermission) {
        LaunchedEffect(Unit) { launcher.launch(android.Manifest.permission.CAMERA) }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Meminta izin kamera...") }
        return
    }

    // UI Kamera + overlay
//    Box(modifier = Modifier.fillMaxSize()) {
//        CameraPreview(
//            lensFacing = lensFacing,
//            // --- PERBAIKAN DI SINI ---
//            // Menggunakan nama fungsi yang sudah diperbarui di ViewModel
//            onFrame = { bitmap -> viewModel.setBitmapAndDetect(bitmap) },
//            onFrameSize = { w, h -> viewModel.setFrameSize(w, h) }
//        )
//
//        val detections by viewModel.detections.collectAsState()
//        val fs = viewModel.frameSize.collectAsState().value
//        val fw = fs?.first ?: 0
//        val fh = fs?.second ?: 0
//
//        DetectionOverlay(
//            detections = detections,
//            frameWidth = fw,
//            frameHeight = fh,
//            modifier = Modifier.fillMaxSize(),
//            isFrontCamera = (lensFacing == CameraSelector.LENS_FACING_FRONT)
//        )
//
//        // FAB Capture — ambil hasil realtime terakhir (bitmap + detections)
//        val coroutineScope = rememberCoroutineScope()
//        FloatingActionButton(
//            onClick = {
//                coroutineScope.launch {
//                    // Ambil frozen frame & detections dari viewmodel (realtime last)
//                    val frozen = viewModel.latestProcessedBitmap.value ?: viewModel.currentBitmap
//                    val latestDet = viewModel.latestDetections.value
//
//                    if (frozen == null) return@launch
//
//                    // Optional: lakukan kompresi/simpan di background thread
//                    val toSave = withContext(Dispatchers.Default) {
//                        try {
//                            frozen.copy(Bitmap.Config.ARGB_8888, true)
//                        } catch (e: Exception) {
//                            frozen
//                        }
//                    }
//
//                    // Simpan ke cache
//                    val fileName = "captured_${System.currentTimeMillis()}.jpg"
//                    val uri = BitmapUtils.saveBitmapToCache(context, toSave, fileName)
//
//                    // Kirim ke UploadImageScreen melalui previousBackStackEntry
//                    navController.previousBackStackEntry?.savedStateHandle?.set("captured_image_uri", uri)
//                    navController.previousBackStackEntry?.savedStateHandle?.set("captured_detections", latestDet)
//
//                    // Stop realtime & clear overlay agar tidak 'bleed' saat kembali
//                    viewModel.stopRealtimeDetection()
//
//                    navController.popBackStack()
//                }
//            },
//            containerColor = Color.White,
//            shape = androidx.compose.foundation.shape.CircleShape,
//            modifier = Modifier
//                .align(if (isPortrait) Alignment.BottomCenter else Alignment.CenterEnd)
//                .padding(100.dp)
//        ) {
//            Icon(
//                imageVector = Icons.Default.CameraAlt,
//                contentDescription = "Capture",
//                modifier = Modifier.graphicsLayer { rotationZ = 0f }
//            )
//        }
//    }
    Box(modifier = Modifier.fillMaxSize()) {

        CameraPreview(
            lensFacing = lensFacing,
            onFrame = { bitmap -> viewModel.setBitmapAndDetect(bitmap) },
            onFrameSize = { w, h -> viewModel.setFrameSize(w, h) }
        )

        val detections by viewModel.detections.collectAsState()
        val fs = viewModel.frameSize.collectAsState().value
        val fw = fs?.first ?: 0
        val fh = fs?.second ?: 0

        DetectionOverlay(
            detections = detections,
            frameWidth = fw,
            frameHeight = fh,
            modifier = Modifier.fillMaxSize(),
            isFrontCamera = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        )

        // =============================
        // 🔥 SLIDER CONTROL (BARU)
        // =============================

        val conf by viewModel.confidenceThreshold.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {

            val alignment = if (isPortrait) Alignment.CenterEnd else Alignment.CenterStart

            Column(
                modifier = Modifier
                    .align(alignment)
                    .padding(
                        start = if (!isPortrait) 32.dp else 12.dp, // ⬅️ hanya geser saat landscape
                        end = 12.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 🔥 TEXT TEPAT DI ATAS SLIDER
                Text(
                    text = "${"%.2f".format(conf)}",
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerticalSlider(
                    value = conf,
                    onChange = { viewModel.setConfidenceThreshold(it) }
                )
            }
        }

        // =============================
        // 📸 FAB CAPTURE (TETAP)
        // =============================

        val coroutineScope = rememberCoroutineScope()

        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    val frozen = viewModel.latestProcessedBitmap.value ?: viewModel.currentBitmap
                    val latestDet = viewModel.latestDetections.value

                    if (frozen == null) return@launch

                    val toSave = withContext(Dispatchers.Default) {
                        try {
                            frozen.copy(Bitmap.Config.ARGB_8888, true)
                        } catch (e: Exception) {
                            frozen
                        }
                    }

                    val fileName = "captured_${System.currentTimeMillis()}.jpg"
                    val uri = BitmapUtils.saveBitmapToCache(context, toSave, fileName)

                    navController.previousBackStackEntry?.savedStateHandle?.set("captured_image_uri", uri)
                    navController.previousBackStackEntry?.savedStateHandle?.set("captured_detections", latestDet)

                    viewModel.stopRealtimeDetection()
                    navController.popBackStack()
                }
            },
            containerColor = Color.White,
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier
                .align(if (isPortrait) Alignment.BottomCenter else Alignment.CenterEnd)
                .padding(100.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capture",
                modifier = Modifier.graphicsLayer { rotationZ = 0f }
            )
        }
    }
}

@Composable
fun LockToFullSensor() {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

        onDispose {
            activity?.requestedOrientation =
                originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun VerticalSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .width(60.dp)
            .fillMaxHeight(0.3f)
    ) {

        val heightPx = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        val y = change.position.y

                        val newValue = 1f - (y / heightPx)
                        onChange(newValue.coerceIn(0.1f, 1.0f))
                    }
                }
        ) {

            // TRACK
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.4f))
            )

            // THUMB (posisi real, bukan kira-kira)
            val thumbY = (1f - value) * heightPx

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, thumbY.toInt() - 20) }
                    .size(24.dp)
                    .background(Color.White, shape = CircleShape)
            )
        }
    }
}