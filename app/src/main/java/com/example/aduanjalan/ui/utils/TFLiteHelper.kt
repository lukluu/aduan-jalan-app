//package com.example.aduanjalan.ui.utils
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.util.Log
//import com.example.aduanjalan.domain.model.Detection
//import com.example.aduanjalan.ml.SsdMobilenetV11Metadata1
//import org.tensorflow.lite.support.common.FileUtil
//import org.tensorflow.lite.support.image.ImageProcessor
//import org.tensorflow.lite.support.image.TensorImage
//import org.tensorflow.lite.support.image.ops.ResizeOp
//
//object TFLiteHelper {
//    private const val TAG = "TFLiteHelper"
//    private const val MODEL_INPUT_SIZE = 300
//    private const val THRESHOLD = 0.5f
//
//    private val imageProcessor = ImageProcessor.Builder()
//        .add(ResizeOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
//        .build()
//
//    fun detectBitmap(
//        context: Context,
//        bitmap: Bitmap,
//        confThreshold: Float = THRESHOLD
//    ): List<Detection> {
//        Log.d(TAG, "detectBitmap: ${bitmap.width}x${bitmap.height}")
//
//        val model = try {
//            SsdMobilenetV11Metadata1.newInstance(context)
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to load model: ${e.message}", e)
//            return emptyList()
//        }
//
//        val labels = FileUtil.loadLabels(context, "labels.txt")
//
//        try {
//            var image = TensorImage.fromBitmap(bitmap)
//            image = imageProcessor.process(image)
//
//            val outputs = model.process(image)
//            val locations = outputs.locationsAsTensorBuffer.floatArray
//            val classes = outputs.classesAsTensorBuffer.floatArray
//            val scores = outputs.scoresAsTensorBuffer.floatArray
//            val numDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray[0].toInt()
//
//            val results = mutableListOf<Detection>()
//            for (i in 0 until numDetections) {
//                val score = scores[i]
//                if (score < confThreshold) continue
//
//                val labelIdx = classes[i].toInt()
//                val label = if (labelIdx in labels.indices) labels[labelIdx] else "Class $labelIdx"
//
//                val ymin = locations[i * 4]
//                val xmin = locations[i * 4 + 1]
//                val ymax = locations[i * 4 + 2]
//                val xmax = locations[i * 4 + 3]
//
//                results.add(
//                    Detection(
//                        label = label,
//                        confidence = score * 100f,
//                        bbox_x = (xmin * bitmap.width).toInt(),
//                        bbox_y = (ymin * bitmap.height).toInt(),
//                        bbox_width = ((xmax - xmin) * bitmap.width).toInt(),
//                        bbox_height = ((ymax - ymin) * bitmap.height).toInt()
//                    )
//                )
//            }
//
//            Log.d(TAG, "Detections found=${results.size}")
//            return results
//        } catch (e: Exception) {
//            Log.e(TAG, "Detection failed: ${e.message}", e)
//            return emptyList()
//        } finally {
//            model.close()
//        }
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
    private const val MODEL_INPUT_SIZE = 640
    private const val CONF_THRESHOLD = 0.25f   // turunkan agar lebih sensitif
    private const val IOU_THRESHOLD = 0.45f

    private var interpreter: Interpreter? = null
    private lateinit var labels: List<String>

    // --------------------------------------------------
    // Inisialisasi model
    // --------------------------------------------------
    fun init(context: Context) {
        if (interpreter == null) {
            val modelBuffer = FileUtil.loadMappedFile(context, "best_float16.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            labels = FileUtil.loadLabels(context, "labelsku.txt")
            Log.d(TAG, "✅ Model YOLOv8 loaded successfully with ${labels.size} labels")
        }
    }

    // --------------------------------------------------
    // Jalankan deteksi pada gambar Bitmap
    // --------------------------------------------------
    fun detectBitmap(context: Context, bitmap: Bitmap): List<Detection> {
        if (interpreter == null) init(context)

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
        val inputBuffer = bitmapToBuffer(scaledBitmap)

        val outputTensor = interpreter!!.getOutputTensor(0)
        val outputShape = outputTensor.shape() // [1, 11, 8400]
        Log.d(TAG, "✅ Output shape: ${outputShape.joinToString()}")

        // Buat buffer sesuai output model
        val outputBuffer = Array(outputShape[1]) { FloatArray(outputShape[2]) }
        interpreter!!.run(inputBuffer, arrayOf(outputBuffer))

        // Transpose output ke [8400, 11]
        val transposed = Array(outputShape[2]) { FloatArray(outputShape[1]) }
        for (i in 0 until outputShape[1]) {
            for (j in 0 until outputShape[2]) {
                transposed[j][i] = outputBuffer[i][j]
            }
        }

        return processYOLOOutput(transposed, bitmap.width, bitmap.height)
    }


    // --------------------------------------------------
    // Konversi bitmap ke ByteBuffer (input model)
    // --------------------------------------------------
    private fun bitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val input = ByteBuffer.allocateDirect(1 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3 * 4)
        input.order(ByteOrder.nativeOrder())

        val pixels = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var idx = 0
        for (y in 0 until MODEL_INPUT_SIZE) {
            for (x in 0 until MODEL_INPUT_SIZE) {
                val v = pixels[idx++]
                input.putFloat(((v shr 16) and 0xFF) / 255.0f) // R
                input.putFloat(((v shr 8) and 0xFF) / 255.0f)  // G
                input.putFloat((v and 0xFF) / 255.0f)          // B
//                input.putFloat(((v shr 16 and 0xFF).toFloat() / 255f))
//                input.putFloat(((v shr 8 and 0xFF).toFloat() / 255f))
//                input.putFloat(((v and 0xFF).toFloat() / 255f))
            }
        }
        return input
    }

    // --------------------------------------------------
    // Parsing output YOLOv8 ke list deteksi
    // --------------------------------------------------
    private fun processYOLOOutput(
        outputs: Array<FloatArray>,
        origW: Int,
        origH: Int
    ): List<Detection> {
        val detections = mutableListOf<Detection>()
        val numBoxes = outputs.size       // 8400
        val numValues = outputs[0].size   // 84

        for (i in 0 until numBoxes) {
            val cx = outputs[i][0]
            val cy = outputs[i][1]
            val w = outputs[i][2]
            val h = outputs[i][3]

            var maxClassScore = -Float.MAX_VALUE
            var classIndex = -1

            for (c in 4 until numValues) {
                val score = outputs[i][c]
                if (score > maxClassScore) {
                    maxClassScore = score
                    classIndex = c - 4
                }
            }

            val confidence = maxClassScore
            if (confidence > CONF_THRESHOLD && classIndex in labels.indices) {
                // konversi ke koordinat gambar asli
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
                Log.d(
                    TAG,
                    "Box: ${labels[classIndex]} (${safeX.toInt()}, ${safeY.toInt()}, ${safeW.toInt()}, ${safeH.toInt()}) conf=${"%.2f".format(confidence)}"
                )
            }
        }

        Log.d(TAG, "✅ Raw detections: ${detections.size}")
        return nms(detections, IOU_THRESHOLD)
    }

    // --------------------------------------------------
    // Non-Maximum Suppression (menghapus overlap)
    // --------------------------------------------------
    private fun nms(detections: List<Detection>, iouThreshold: Float): List<Detection> {
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
                if (iou > iouThreshold) active[j] = false
            }
        }
        return result
    }

    // --------------------------------------------------
    // Hitung Intersection over Union (IoU)
    // --------------------------------------------------
    private fun calculateIoU(a: Detection, b: Detection): Float {
        val x1 = max(a.bbox_x.toFloat(), b.bbox_x.toFloat())
        val y1 = max(a.bbox_y.toFloat(), b.bbox_y.toFloat())
        val x2 = min(a.bbox_x + a.bbox_width.toFloat(), b.bbox_x + b.bbox_width.toFloat())
        val y2 = min(a.bbox_y + a.bbox_height.toFloat(), b.bbox_y + b.bbox_height.toFloat())

        val interArea = max(0f, x2 - x1) * max(0f, y2 - y1)
        val unionArea = (a.bbox_width * a.bbox_height) + (b.bbox_width * b.bbox_height) - interArea
        return if (unionArea <= 0f) 0f else interArea / unionArea
    }
}
