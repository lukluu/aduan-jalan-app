package com.example.aduanjalan.data.remote.response

data class AllReportResponse(
    val id: Int,
    val image_path: String?,
    val location_address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val priority_score: Float?,
    val status: String?,
    val road_type: String?,
    val rank: Int?,
    val created_at: String?,
    val detected_labels: String?,
    val vote_count: Int?,
    val has_voted: Boolean?,
    val user: UserSimple?,
    val answers: List<AnswerResponses>?
)
data class UserSimple(
    val id: Int?,
    val name: String?
)

data class AllReportWrapper(
    val success: Boolean,
    val message: String,
    val data: List<AllReportResponse>?
)
data class AnswerResponses(
    val criteria_name: String?,
    val option_label: String?,
    val option_value: Int?
)
