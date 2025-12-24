package com.example.aduanjalan.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.aduanjalan.R
import com.example.aduanjalan.ui.auth.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavHostController
) {
    val viewModel: AuthViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        delay(1000)
        val token = viewModel.getToken()
        if (token.isNullOrEmpty()) {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // --- LANGKAH 1: DEFINISIKAN WARNA BERDASARKAN TEMA ---
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color.White
    val textColor = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            // --- LANGKAH 2: TERAPKAN WARNA LATAR BELAKANG DINAMIS ---
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo App",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aduan Jalan",
                fontSize = 28.sp,
                // --- LANGKAH 3: TERAPKAN WARNA TEKS DINAMIS ---
                color = textColor,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}