package com.example.aduanjalan.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.aduanjalan.data.remote.response.MyReportResponse
import com.example.aduanjalan.ui.allreport.AllReportsListScreen
import com.example.aduanjalan.ui.allreport.AllReportsScreen
import com.example.aduanjalan.ui.auth.LoginScreen
import com.example.aduanjalan.ui.auth.RegisterScreen
import com.example.aduanjalan.ui.detection.DetectionViewModel
import com.example.aduanjalan.ui.home.HomeScreen
import com.example.aduanjalan.ui.myreport.MyReportScreen
import com.example.aduanjalan.ui.myreport.ReportDetailMapScreen
import com.example.aduanjalan.ui.profile.ProfileScreen
import com.example.aduanjalan.ui.report.CameraScreen
import com.example.aduanjalan.ui.report.DeskripsiScreen
import com.example.aduanjalan.ui.report.MapScreen
import com.example.aduanjalan.ui.report.UploadImageScreen
import com.example.aduanjalan.ui.splash.SplashScreen

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String
) {
    // Ambil DetectionViewModel agar konsisten antar screen
    val detectionViewModel: DetectionViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash
        composable("splash") {
            SplashScreen(navController)
        }

        // Auth
        composable("login") {
            LoginScreen(navController)
        }
        composable("register") {
            RegisterScreen(navController)
        }

        // Home
        composable("home") {
            HomeScreen(navController)
        }

        // Upload + Deteksi
        composable("upload_image") {
            UploadImageScreen(
                navController = navController,
                viewModel = detectionViewModel
            )
        }

        composable("camera_realtime") {
            CameraScreen(navController)
        }

        // Map Picker
        composable("map_picker") {
            MapScreen(
                navController = navController,
                detectionViewModel = detectionViewModel
            )
        }

        // Deskripsi
        composable("deskripsi_screen") {
            DeskripsiScreen(
                navController = navController,
                detectionViewModel = detectionViewModel
            )
        }

        // My Reports
        composable(Screen.MyReports.route) {
            MyReportScreen(navController = navController)
        }

        // All Reports
        composable("all_reports") {
            AllReportsScreen(navController = navController)
        }
        composable("all_reports_list") {
            AllReportsListScreen(navController = navController)
        }
        composable(
            route = "all_reports/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.IntType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getInt("reportId")
            AllReportsScreen(navController = navController, selectedReportId = reportId)
        }


        composable(
            route = "reportDetailMap/{reportId}",
            arguments = listOf(navArgument("reportId") { defaultValue = "-1" })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId")?.toIntOrNull()
            ReportDetailMapScreen(navController = navController, reportId = reportId)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
    }
}
