package com.example.aduanjalan.ui.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.aduanjalan.data.remote.response.AllReportResponse
import com.example.aduanjalan.ui.component.BottomBar
import com.example.aduanjalan.ui.navigation.Screen
import com.example.aduanjalan.ui.theme.PrimaryColor
import com.example.aduanjalan.ui.utils.capitalizeWords
import com.example.aduanjalan.ui.utils.shimmerEffect
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val latestReports by viewModel.latestReports.collectAsState()
    val isLoadingReports by viewModel.isLoadingReports.collectAsState()
    // 1. Ambil state refreshing
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val surfaceMenuColor = if (isDarkTheme) Color(0xFF1F1F1F) else Color(0xFFF7F7F7)
    val headerTextColor = Color.White

    BackHandler {
        (context as? Activity)?.finish()
    }

    Scaffold(
        containerColor = PrimaryColor,
        bottomBar = { BottomBar(navController = navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(PrimaryColor)
        ) {
            // Header (Tetap Statis)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person, contentDescription = "User Icon", tint = headerTextColor,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(headerTextColor.copy(alpha = 0.2f)).padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Hi, ${userName.capitalizeWords()}", color = headerTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Selamat Datang 👋", color = headerTextColor.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.logout { navController.navigate("login") { popUpTo(0) { inclusive = true } } } },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color.White))
                    ) {
                        Text("Keluar", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout Icon", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Konten Menu dengan Carousel & SwipeRefresh
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = surfaceMenuColor,
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            ) {
                // 2. Bungkus Konten Surface dengan SwipeRefresh
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing),
                    onRefresh = { viewModel.refreshHome() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 3. Tambahkan verticalScroll agar konten bisa ditarik
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()) // PENTING agar swipe terdeteksi
                            .padding(vertical = 24.dp)
                    ) {
                        Text(
                            text = "Aduan Masuk Terbaru",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // --- BAGIAN CAROUSEL DENGAN SKELETON ---
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingReports) {
                                // Tampilkan Skeleton saat load awal (bukan refresh)
                                CarouselSkeletonLoader()
                            } else if (latestReports.isNotEmpty()) {
                                LatestReportsCarousel(
                                    reports = latestReports,
                                    navController = navController
                                )
                            } else {
                                Text("Belum ada aduan terbaru.", color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(Modifier.padding(horizontal = 24.dp)) {
                            PrimaryMenuButton(
                                text = "Buat Aduan Baru",
                                subText = "Laporkan kerusakan jalan di sini",
                                icon = Icons.Default.CameraAlt,
                                onClick = { navController.navigate(Screen.UploadImage.route) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SecondaryMenuButton(
                                text = "Riwayat Aduan Saya",
                                icon = Icons.Default.Description,
                                onClick = { navController.navigate(Screen.MyReports.route) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SecondaryMenuButton(
                                text = "Peta Persebaran Aduan",
                                icon = Icons.Default.Map,
                                onClick = { navController.navigate(Screen.AllReports.route) }
                            )
                            // Spacer tambahan agar konten tidak terpotong di paling bawah
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun CarouselSkeletonLoader() {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kartu Dummy yang Shimmering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp) // Samakan tinggi dengan HorizontalPager di LatestReportsCarousel
                .clip(RoundedCornerShape(16.dp))
                .shimmerEffect() // Panggil modifier shimmer
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Indikator Dummy (Titik-titik)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { // Buat 3 titik dummy
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .size(8.dp)
                        .shimmerEffect() // Titik juga shimmer
                )
            }
        }
    }
}
@OptIn(ExperimentalPagerApi::class)
@Composable
fun LatestReportsCarousel(
    reports: List<AllReportResponse>,
    navController: NavHostController
) {
    val pagerState = rememberPagerState(initialPage = Int.MAX_VALUE / 2)

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            count = Int.MAX_VALUE,
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            itemSpacing = 12.dp,
            modifier = Modifier.height(120.dp)
        ) { page ->
            val actualIndex = page % reports.size
            val report = reports[actualIndex]

            CarouselCard(
                report = report,
                onClick = {
                    navController.navigate("all_reports/${report.id}")
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(reports.size) { iteration ->
                val color = if (pagerState.currentPage % reports.size == iteration) {
                    PrimaryColor
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
fun CarouselCard(report: AllReportResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(model = report.image_path),
                contentDescription = "Gambar Aduan",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = report.location_address ?: "Lokasi tidak diketahui",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Status: ${report.status ?: "N/A"}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// --- PERUBAHAN DIMULAI DI SINI ---

@Composable
fun PrimaryMenuButton(text: String, subText: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp), // Dibuat lebih tinggi
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SecondaryMenuButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val surfaceColor = if (isDarkTheme) Color(0xFF2A2A2A) else Color.White
    val borderColor = if (isDarkTheme) Color.Gray.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = PrimaryColor
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}