package com.example.aduanjalan.ui.utils

fun String.capitalizeWords(): String {
    return this.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
}
