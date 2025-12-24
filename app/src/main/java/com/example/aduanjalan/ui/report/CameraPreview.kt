package com.example.aduanjalan.ui.report

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

@Composable
fun CameraPreview(
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    onFrame: (Bitmap) -> Unit,
    onFrameSize: ((width: Int, height: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize())

    LaunchedEffect(lensFacing) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0

        val preview = Preview.Builder()
            .setTargetRotation(rotation)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            // optional: setTargetResolution(Size(1280,720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        val executor = ContextCompat.getMainExecutor(context)


        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            try {
                val bitmap = imageProxy.toBitmap() ?: return@setAnalyzer
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val rotated = bitmap.rotate(rotationDegrees)

                onFrameSize?.invoke(rotated.width, rotated.height)
                onFrame(rotated)

            } catch (e: Exception) {
                Log.e("CameraAnalyzer", "Frame error: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// helper: ImageProxy -> Bitmap (NV21 -> JPEG -> Bitmap)
fun ImageProxy.toBitmap(): Bitmap? {
    val planeY = planes.getOrNull(0)?.buffer ?: return null
    val planeU = planes.getOrNull(1)?.buffer ?: return null
    val planeV = planes.getOrNull(2)?.buffer ?: return null

    val ySize = planeY.remaining()
    val uSize = planeU.remaining()
    val vSize = planeV.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)

    planeY.get(nv21, 0, ySize)
    // NV21 = Y + VU
    planeV.get(nv21, ySize, vSize)
    planeU.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 90, out)
    val imageBytes = out.toByteArray()
    return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val m = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}
