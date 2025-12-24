package com.example.aduanjalan

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box // Import Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState // Import collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.aduanjalan.ui.component.NoInternetOverlay // Pastikan Import ini ada
import com.example.aduanjalan.ui.navigation.AppNavHost
import com.example.aduanjalan.ui.theme.AduanJalanTheme
import com.example.aduanjalan.ui.utils.NetworkMonitor // Pastikan Import ini ada
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject // Import Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 1. Inject NetworkMonitor di sini
    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen()

        setContent {
            val navController = rememberNavController()

            AduanJalanTheme {
                // 2. Pantau status internet secara real-time
                // default true agar saat start tidak langsung merah sekejap
                val isOnline by networkMonitor.isConnected.collectAsState(initial = true)

                // 3. Gunakan Box untuk menumpuk UI
                Box(modifier = Modifier.fillMaxSize()) {

                    // Layer Bawah: Aplikasi Utama Anda
                    AppNavHost(
                        navController = navController,
                        startDestination = "splash"
                    )

                    // Layer Atas: Overlay No Internet
                    // Ini akan otomatis muncul menutupi AppNavHost jika isOnline = false
                    NoInternetOverlay(isOnline = isOnline)
                }
            }
        }
    }
}