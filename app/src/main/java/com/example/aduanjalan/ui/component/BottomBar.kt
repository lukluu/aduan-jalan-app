package com.example.aduanjalan.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.aduanjalan.ui.navigation.Screen
import com.example.aduanjalan.ui.theme.PrimaryColor
import com.example.aduanjalan.ui.theme.TextSecondary

@Composable
fun BottomBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Beranda", Icons.Default.Home, Screen.Home),
        BottomNavItem("Map Aduan", Icons.Default.Map, Screen.AllReports),
        BottomNavItem("Buat Aduan", Icons.Default.AddCircle, Screen.UploadImage),
        BottomNavItem("Aduan Saya", Icons.Default.Report, Screen.MyReports),
        BottomNavItem("Profil", Icons.Default.Person, Screen.Profile)
    )

    // --- PERUBAHAN 1: DEFINISIKAN WARNA BERDASARKAN TEMA ---
    val isDarkTheme = isSystemInDarkTheme()

    // Warna untuk background Bottom Bar
    val surfaceColor = if (isDarkTheme) Color(0xFF1F1F1F) else Color.White
    // Warna untuk garis tipis di atas
    val borderColor = if (isDarkTheme) Color.Gray.copy(alpha = 0.3f) else Color.LightGray
    // Warna untuk item yang tidak dipilih
    val unselectedContentColor = if (isDarkTheme) Color.LightGray else TextSecondary


    Surface(
        // Terapkan warna background yang sudah ditentukan
        color = surfaceColor,
        modifier = Modifier
            // Terapkan warna border yang sudah ditentukan
            .border(0.5.dp, borderColor)
    ) {
        NavigationBar(
            modifier = Modifier.padding(horizontal = 16.dp),
            // Background NavigationBar dibuat transparan agar mengikuti warna Surface
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            items.forEach { item ->
                val selected = currentRoute?.startsWith(item.screen.route) == true

                // --- PERUBAHAN 2: GUNAKAN WARNA KONTEN YANG SUDAH DITENTUKAN ---
                val contentColor by animateColorAsState(
                    // Warna PrimaryColor untuk item terpilih, dan unselectedContentColor untuk lainnya
                    targetValue = if (selected) PrimaryColor else unselectedContentColor,
                    label = "contentColor"
                )

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(26.dp),
                            // Terapkan warna konten yang dianimasikan
                            tint = contentColor
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            // Terapkan warna konten yang dianimasikan
                            color = contentColor,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    alwaysShowLabel = true,
                    // --- PERUBAHAN 3: SEDERHANAKAN PENGATURAN WARNA ITEM ---
                    colors = NavigationBarItemDefaults.colors(
                        // Warna indikator (latar belakang item terpilih) dibuat sedikit transparan
                        indicatorColor = PrimaryColor.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val screen: Screen
)