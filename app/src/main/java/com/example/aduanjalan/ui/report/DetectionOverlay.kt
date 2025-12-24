package com.example.aduanjalan.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.aduanjalan.domain.model.Detection
import kotlin.math.min

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    frameWidth: Int,
    frameHeight: Int,
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false // jika nanti pakai kamera depan
) {
    if (frameWidth <= 0 || frameHeight <= 0) return

    Canvas(modifier = modifier) {
        val viewW = size.width
        val viewH = size.height
        val fw = frameWidth.toFloat()
        val fh = frameHeight.toFloat()

        // Karena PreviewView FIT_CENTER: skala seragam + letterbox
        val scale = min(viewW / fw, viewH / fh)
        val contentW = fw * scale
        val contentH = fh * scale
        val offsetX = (viewW - contentW) / 2f
        val offsetY = (viewH - contentH) / 2f

        detections.forEach { det ->
            // Jika kamera depan, koordinat perlu di-mirror horizontal
            val x = if (isFrontCamera) (frameWidth - det.bbox_x - det.bbox_width).toFloat() else det.bbox_x.toFloat()
            val y = det.bbox_y.toFloat()
            val w = det.bbox_width.toFloat()
            val h = det.bbox_height.toFloat()

            val left = offsetX + x * scale
            val top = offsetY + y * scale
            val boxW = w * scale
            val boxH = h * scale

            // Kotak bbox
            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                style = Stroke(width = 3f)
            )

            // Bar judul di atas bbox
            val titleH = 35f
            val titleTop = (top - titleH).coerceAtLeast(0f)
            drawRect(
                color = Color(0xAA1C7ED6),
                topLeft = Offset(left, titleTop),
                size = Size(boxW, titleH)
            )

            // Label + confidence
            drawContext.canvas.nativeCanvas.drawText(
                "${det.label} (${String.format("%.1f", det.confidence)}%)",
                left + 8f,
                (titleTop + titleH - 8f), // baseline sedikit di dalam bar
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 28f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
            )
        }
    }
}
