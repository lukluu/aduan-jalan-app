//
//package com.example.aduanjalan.ui.utils
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.util.Log
//import com.example.aduanjalan.domain.model.Detection
//import org.tensorflow.lite.Interpreter
//import org.tensorflow.lite.support.common.FileUtil
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import kotlin.math.*
//
//object TFLiteHelper {
//    private const val TAG = "TFLiteHelper"
//    private const val MODEL_INPUT_SIZE = 320
//    private const val CONF_THRESHOLD = 0.50f
//    private const val IOU_THRESHOLD = 0.50f
//
//    private var interpreter: Interpreter? = null
//    private lateinit var labels: List<String>
//
//    // --------------------------------------------------
//    // Inisialisasi model
//    // --------------------------------------------------
//    fun init(context: Context) {
//        if (interpreter == null) {
//            val modelBuffer = FileUtil.loadMappedFile(context, "best_int8.tflite")
//            val options = Interpreter.Options().apply {
//                setNumThreads(4)
//            }
//            interpreter = Interpreter(modelBuffer, options)
//            labels = FileUtil.loadLabels(context, "labelsku.txt")
//            Log.d(TAG, "✅ Model YOLOv8 loaded successfully with ${labels.size} labels")
//        }
//    }
//
//    // --------------------------------------------------
//    // Jalankan deteksi pada gambar Bitmap
//    // --------------------------------------------------
//    fun detectBitmap(context: Context, bitmap: Bitmap): List<Detection> {
//        if (interpreter == null) init(context)
//
//        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
//        val inputBuffer = bitmapToBuffer(scaledBitmap)
//
//        val outputTensor = interpreter!!.getOutputTensor(0)
//        val outputShape = outputTensor.shape() // [1, 11, 8400]
//        Log.d(TAG, "✅ Output shape: ${outputShape.joinToString()}")
//
//        // Buat buffer sesuai output model
//        val outputBuffer = Array(outputShape[1]) { FloatArray(outputShape[2]) }
//        interpreter!!.run(inputBuffer, arrayOf(outputBuffer))
//
//        // Transpose output ke [8400, 11]
//        val transposed = Array(outputShape[2]) { FloatArray(outputShape[1]) }
//        for (i in 0 until outputShape[1]) {
//            for (j in 0 until outputShape[2]) {
//                transposed[j][i] = outputBuffer[i][j]
//            }
//        }
//
//        return processYOLOOutput(transposed, bitmap.width, bitmap.height)
//    }
//
//
//    // --------------------------------------------------
//    // Konversi bitmap ke ByteBuffer (input model)
//    // --------------------------------------------------
//    private fun bitmapToBuffer(bitmap: Bitmap): ByteBuffer {
//        val input = ByteBuffer.allocateDirect(1 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3 * 4)
//        input.order(ByteOrder.nativeOrder())
//
//        val pixels = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
//        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
//
//        var idx = 0
//        for (y in 0 until MODEL_INPUT_SIZE) {
//            for (x in 0 until MODEL_INPUT_SIZE) {
//                val v = pixels[idx++]
//                input.putFloat(((v shr 16) and 0xFF) / 255.0f) // R
//                input.putFloat(((v shr 8) and 0xFF) / 255.0f)  // G
//                input.putFloat((v and 0xFF) / 255.0f)          // B
////                input.putFloat(((v shr 16 and 0xFF).toFloat() / 255f))
////                input.putFloat(((v shr 8 and 0xFF).toFloat() / 255f))
////                input.putFloat(((v and 0xFF).toFloat() / 255f))
//            }
//        }
//        return input
//    }
//
//    // --------------------------------------------------
//    // Parsing output YOLOv8 ke list deteksi
//    // --------------------------------------------------
//    private fun processYOLOOutput(
//        outputs: Array<FloatArray>,
//        origW: Int,
//        origH: Int
//    ): List<Detection> {
//        val detections = mutableListOf<Detection>()
//        val numBoxes = outputs.size       // 8400
//        val numValues = outputs[0].size   // 84
//
//        for (i in 0 until numBoxes) {
//            val cx = outputs[i][0]
//            val cy = outputs[i][1]
//            val w = outputs[i][2]
//            val h = outputs[i][3]
//
//            var maxClassScore = -Float.MAX_VALUE
//            var classIndex = -1
//
//            for (c in 4 until numValues) {
//                val score = outputs[i][c]
//                if (score > maxClassScore) {
//                    maxClassScore = score
//                    classIndex = c - 4
//                }
//            }
//
//            val confidence = maxClassScore
//            if (confidence > CONF_THRESHOLD && classIndex in labels.indices) {
//                // konversi ke koordinat gambar asli
//                val xMin = (cx - w / 2f) * origW
//                val yMin = (cy - h / 2f) * origH
//                val width = w * origW
//                val height = h * origH
//
//                val safeX = xMin.coerceIn(0f, origW - 1f)
//                val safeY = yMin.coerceIn(0f, origH - 1f)
//                val safeW = min(width, origW - safeX)
//                val safeH = min(height, origH - safeY)
//
//
//                detections.add(
//                    Detection(
//                        label = labels[classIndex],
//                        confidence = confidence * 100f,
//                        bbox_x = safeX.toInt(),
//                        bbox_y = safeY.toInt(),
//                        bbox_width = safeW.toInt(),
//                        bbox_height = safeH.toInt()
//                    )
//                )
//                Log.d(
//                    TAG,
//                    "Box: ${labels[classIndex]} (${safeX.toInt()}, ${safeY.toInt()}, ${safeW.toInt()}, ${safeH.toInt()}) conf=${"%.2f".format(confidence)}"
//                )
//            }
//        }
//
//        Log.d(TAG, "✅ Raw detections: ${detections.size}")
//        return nms(detections, IOU_THRESHOLD)
//    }
//
//    // --------------------------------------------------
//    // Non-Maximum Suppression (menghapus overlap)
//    // --------------------------------------------------
//    private fun nms(detections: List<Detection>, iouThreshold: Float): List<Detection> {
//        val sorted = detections.sortedByDescending { it.confidence }
//        val result = mutableListOf<Detection>()
//        val active = BooleanArray(sorted.size) { true }
//
//        for (i in sorted.indices) {
//            if (!active[i]) continue
//            val detA = sorted[i]
//            result.add(detA)
//
//            for (j in i + 1 until sorted.size) {
//                if (!active[j]) continue
//                val detB = sorted[j]
//                val iou = calculateIoU(detA, detB)
//                if (iou > iouThreshold) active[j] = false
//            }
//        }
//        return result
//    }
//
//    // --------------------------------------------------
//    // Hitung Intersection over Union (IoU)
//    // --------------------------------------------------
//    private fun calculateIoU(a: Detection, b: Detection): Float {
//        val x1 = max(a.bbox_x.toFloat(), b.bbox_x.toFloat())
//        val y1 = max(a.bbox_y.toFloat(), b.bbox_y.toFloat())
//        val x2 = min(a.bbox_x + a.bbox_width.toFloat(), b.bbox_x + b.bbox_width.toFloat())
//        val y2 = min(a.bbox_y + a.bbox_height.toFloat(), b.bbox_y + b.bbox_height.toFloat())
//
//        val interArea = max(0f, x2 - x1) * max(0f, y2 - y1)
//        val unionArea = (a.bbox_width * a.bbox_height) + (b.bbox_width * b.bbox_height) - interArea
//        return if (unionArea <= 0f) 0f else interArea / unionArea
//    }
//}
package com.example.aduanjalan.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.aduanjalan.domain.model.Detection
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

object TFLiteHelper {

    private const val TAG = "TFLiteHelper"
    private const val MODEL_INPUT_SIZE = 320

    var confThreshold = 0.2f
    var iouThreshold = 0.5f

    private var interpreter: Interpreter? = null
    private lateinit var labels: List<String>

    // --------------------------------------------------
    // Inisialisasi model
    // --------------------------------------------------
    fun init(context: Context) {
        if (interpreter == null) {
            val modelBuffer = FileUtil.loadMappedFile(context, "best_int8.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            labels = FileUtil.loadLabels(context, "labelsku.txt")
            Log.d(TAG, "✅ Model YOLO loaded with ${labels.size} labels")
        }
    }

    // --------------------------------------------------
    // Jalankan deteksi
    // --------------------------------------------------
    fun detectBitmap(context: Context, bitmap: Bitmap): List<Detection> {
        if (interpreter == null) init(context)

        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            MODEL_INPUT_SIZE,
            MODEL_INPUT_SIZE,
            true
        )

        val inputBuffer = bitmapToBuffer(scaledBitmap)

        val outputTensor = interpreter!!.getOutputTensor(0)
        val outputShape = outputTensor.shape() // [1, 11, 8400]

        val outputBuffer = Array(outputShape[1]) { FloatArray(outputShape[2]) }
        interpreter!!.run(inputBuffer, arrayOf(outputBuffer))

        // transpose
        val transposed = Array(outputShape[2]) { FloatArray(outputShape[1]) }
        for (i in 0 until outputShape[1]) {
            for (j in 0 until outputShape[2]) {
                transposed[j][i] = outputBuffer[i][j]
            }
        }

        return processYOLOOutput(transposed, bitmap.width, bitmap.height)
    }

    // --------------------------------------------------
    // Bitmap → ByteBuffer
    // --------------------------------------------------
    private fun bitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val input = ByteBuffer.allocateDirect(
            1 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3 * 4
        )
        input.order(ByteOrder.nativeOrder())

        val pixels = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var idx = 0
        for (y in 0 until MODEL_INPUT_SIZE) {
            for (x in 0 until MODEL_INPUT_SIZE) {
                val v = pixels[idx++]
                input.putFloat(((v shr 16) and 0xFF) / 255f)
                input.putFloat(((v shr 8) and 0xFF) / 255f)
                input.putFloat((v and 0xFF) / 255f)
            }
        }
        return input
    }

    // --------------------------------------------------
    // Parsing output YOLO
    // --------------------------------------------------
    private fun processYOLOOutput(
        outputs: Array<FloatArray>,
        origW: Int,
        origH: Int
    ): List<Detection> {

        val detections = mutableListOf<Detection>()

        val numBoxes = outputs.size
        val numValues = outputs[0].size

        for (i in 0 until numBoxes) {

            val cx = outputs[i][0]
            val cy = outputs[i][1]
            val w = outputs[i][2]
            val h = outputs[i][3]

            var maxScore = -Float.MAX_VALUE
            var classIndex = -1

            for (c in 4 until numValues) {
                val score = outputs[i][c]
                if (score > maxScore) {
                    maxScore = score
                    classIndex = c - 4
                }
            }

            val confidence = maxScore

            // ✅ PAKAI DYNAMIC CONFIDENCE
            if (confidence > confThreshold && classIndex in labels.indices) {

                val xMin = (cx - w / 2f) * origW
                val yMin = (cy - h / 2f) * origH
                val width = w * origW
                val height = h * origH

                val safeX = xMin.coerceIn(0f, origW - 1f)
                val safeY = yMin.coerceIn(0f, origH - 1f)
                val safeW = min(width, origW - safeX)
                val safeH = min(height, origH - safeY)

                detections.add(
                    Detection(
                        label = labels[classIndex],
                        confidence = confidence * 100f,
                        bbox_x = safeX.toInt(),
                        bbox_y = safeY.toInt(),
                        bbox_width = safeW.toInt(),
                        bbox_height = safeH.toInt()
                    )
                )
            }
        }

        Log.d(TAG, "Raw detections: ${detections.size}")

        // ✅ PAKAI DYNAMIC IOU
        return nms(detections, iouThreshold)
    }

    // --------------------------------------------------
    // NMS
    // --------------------------------------------------
    private fun nms(
        detections: List<Detection>,
        iouThreshold: Float
    ): List<Detection> {

        val sorted = detections.sortedByDescending { it.confidence }
        val result = mutableListOf<Detection>()
        val active = BooleanArray(sorted.size) { true }

        for (i in sorted.indices) {
            if (!active[i]) continue

            val detA = sorted[i]
            result.add(detA)

            for (j in i + 1 until sorted.size) {
                if (!active[j]) continue

                val detB = sorted[j]
                val iou = calculateIoU(detA, detB)

                if (iou > iouThreshold) {
                    active[j] = false
                }
            }
        }

        return result
    }

    // --------------------------------------------------
    // IoU
    // --------------------------------------------------
    private fun calculateIoU(a: Detection, b: Detection): Float {

        val x1 = max(a.bbox_x.toFloat(), b.bbox_x.toFloat())
        val y1 = max(a.bbox_y.toFloat(), b.bbox_y.toFloat())
        val x2 = min(
            a.bbox_x + a.bbox_width.toFloat(),
            b.bbox_x + b.bbox_width.toFloat()
        )
        val y2 = min(
            a.bbox_y + a.bbox_height.toFloat(),
            b.bbox_y + b.bbox_height.toFloat()
        )

        val interArea = max(0f, x2 - x1) * max(0f, y2 - y1)

        val unionArea =
            (a.bbox_width * a.bbox_height) +
                    (b.bbox_width * b.bbox_height) -
                    interArea

        return if (unionArea <= 0f) 0f else interArea / unionArea
    }
}