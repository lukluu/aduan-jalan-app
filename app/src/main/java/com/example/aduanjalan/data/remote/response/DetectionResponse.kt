package com.example.aduanjalan.data.remote.response


data class DetectionResponse(
    val image_url: String,
    val detections: List<DetectionItem>
)

data class DetectionItem(
    val label: String,
    val confidence: Double,
    val bbox_x: Int,
    val bbox_y: Int,
    val bbox_width: Int,
    val bbox_height: Int
)
