package com.example.aduanjalan.domain.model

data class CriteriaOption(
    val id: Int,
    val label: String,
    val value: Int
)

data class Criteria(
    val id: Int,
    val name: String,
    val question: String?,
    val weight: Double,
    val options: List<CriteriaOption>
)