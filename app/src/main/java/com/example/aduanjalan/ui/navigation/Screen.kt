package com.example.aduanjalan.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AllReports : Screen("all_reports")
    object UploadImage : Screen("upload_image")
    object MyReports : Screen("my_reports")
    object Profile : Screen("profile")
}
