package com.example.aduanjalan.domain.model

data class OverpassResponse(
    val elements: List<Element>
)

data class Element(
    val type: String,
    val id: Long,
    val tags: Tags?
)

data class Tags(
    val highway: String?
)