package com.example.aduanjalan.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object BitmapUtils {

    /**
     * Simpan Bitmap ke folder cache aplikasi dan kembalikan Uri-nya
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String = "captured_image.jpg"): Uri {
        // Buat file di cache
        val cacheDir = File(context.cacheDir, "images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
        }

        return Uri.fromFile(file)
    }
}
