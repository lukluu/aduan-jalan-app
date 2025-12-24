package com.example.aduanjalan.data.remote.response


data class MyReportWrapper(
    val success: Boolean,
    val message: String,
    val data: List<MyReportResponse>
)

data class MyReportResponse(
    val id: Int,
    val image_url: String?,
    val location_address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val detected_labels: String?,
    val status: String?,
    val priority_score: Double?,
    val rank: Int?,
    val road_type: String?,
    val total_reports: Int?,
    val vote_count: Int?,
    val created_at: String?,
    val answers: List<AnswerResponse>?
)

data class AnswerResponse(
    val criteria_name: String?,
    val option_label: String?,
    val option_value: Int?
)
