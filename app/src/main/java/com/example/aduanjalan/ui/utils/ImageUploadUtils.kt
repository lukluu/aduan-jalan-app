package com.example.aduanjalan.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.InputStream

/**
 * Menyalin isi dari [uri] ke file sementara yang dapat dibaca sebagai path lokal.
 */
fun getRealPathFromUri(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("image_", ".jpg", context.cacheDir)
        tempFile.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
        tempFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
fun loadImageBitmapFromUri(context: Context, uri: Uri): ImageBitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun loadCorrectlyOrientedBitmap(context: Context, uri: Uri): Bitmap? {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream?.close()

    val exifStream = context.contentResolver.openInputStream(uri)
    val exif = ExifInterface(exifStream!!)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )

    val rotationMatrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotationMatrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotationMatrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotationMatrix.postRotate(270f)
    }

    exifStream.close()
    return bitmap?.let {
        Bitmap.createBitmap(it, 0, 0, it.width, it.height, rotationMatrix, true)
    }
}
/**
 * Launcher untuk mengambil gambar dari galeri menggunakan Jetpack Compose.
 */
@Composable
fun pickImageLauncher(onImagePicked: (Uri?) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImagePicked(uri)
    }

    return {
        launcher.launch("image/*")
    }
}
