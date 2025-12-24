package com.example.aduanjalan.data.remote.request

data class ReportRequest(
    val imagePath: String,
    val lat: Double,
    val lon: Double,
    val description: String,
    val criterias: Map<String, Any>
)