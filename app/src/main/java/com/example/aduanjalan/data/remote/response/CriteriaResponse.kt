package com.example.aduanjalan.data.remote.response

data class CriteriaOptionResponse(
    val id: Int,
    val label: String,
    val value: Int
)

data class CriteriaResponse(
    val id: Int,
    val name: String,
    val question: String?,
    val weight: Double,
    val options: List<CriteriaOptionResponse>
)