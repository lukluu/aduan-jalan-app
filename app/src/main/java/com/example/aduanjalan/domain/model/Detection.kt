package com.example.aduanjalan.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Detection(
    val label: String,
    val confidence: Float,
    val bbox_x: Int,
    val bbox_y: Int,
    val bbox_width: Int,
    val bbox_height: Int
) : Parcelable