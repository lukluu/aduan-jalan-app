package com.example.aduanjalan.data.remote.request


data class DetectionRequest(
    val label: String,
    val confidence: Double,
    val bbox_x: Double,
    val bbox_y: Double,
    val bbox_width: Double,
    val bbox_height: Double
)

data class CreateReportRequest(
    val image_url: String,
    val location_address: String,
    val latitude: Double,
    val longitude: Double,
    val detections: List<DetectionRequest>,
    val answers: List<Map<String, Int>>
)
