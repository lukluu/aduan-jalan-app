package com.example.aduanjalan.ui.allreport

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.aduanjalan.data.remote.response.AllReportResponse
import com.example.aduanjalan.ui.theme.PrimaryColor
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun AllReportsListScreen(
    navController: NavController,
    viewModel: AllReportsViewModel = hiltViewModel()
) {
    val reports by viewModel.filteredReports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isRefreshing by viewModel.refreshing.collectAsState()

    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val tabs = listOf("Prioritas Utama", "Aduan Terbaru")

    val sortedReports = remember(reports, pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> reports.sortedBy { it.rank ?: Int.MAX_VALUE }
            1 -> reports.sortedByDescending { it.created_at ?: "" }
            else -> reports
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    // --- LANGKAH 1: DEFINISIKAN PALET WARNA DINAMIS ---
    val isDarkTheme = isSystemInDarkTheme()
    val scaffoldBackgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFFAFAFA)
    val tabRowColor = if (isDarkTheme) Color(0xFF1F1F1F) else Color.White
    val unselectedTabColor = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray
    val emptyTextColor = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray

    Scaffold(
        // Terapkan warna latar belakang dinamis
        containerColor = scaffoldBackgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Daftar Semua Aduan", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                // Terapkan warna latar belakang TabRow
                containerColor = tabRowColor,
                contentColor = PrimaryColor // Warna untuk tab aktif
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(text = title) },
                        // Atur warna untuk tab yang tidak aktif
                        unselectedContentColor = unselectedTabColor
                    )
                }
            }

            HorizontalPager(
                count = tabs.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        !error.isNullOrEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(error ?: "Terjadi kesalahan", color = MaterialTheme.colorScheme.error)
                        }
                        sortedReports.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada data ditemukan", color = emptyTextColor)
                        }
                        else -> {
                            SwipeRefresh(
                                state = rememberSwipeRefreshState(isRefreshing),
                                onRefresh = { viewModel.refreshReports() }
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    items(sortedReports) { report ->
                                        AllReportListItem(
                                            report = report,
                                            onVote = { viewModel.voteReport(report.id) },
                                            onClick = { navController.navigate("all_reports/${report.id}") }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AllReportListItem(
    report: AllReportResponse,
    onVote: () -> Unit,
    onClick: () -> Unit
) {
    // --- LANGKAH 2: BUAT ITEM LIST MENJADI THEME-AWARE ---
    val isDarkTheme = isSystemInDarkTheme()
    val cardColor = if (isDarkTheme) Color(0xFF1F1F1F) else Color.White
    val borderColor = if (isDarkTheme) Color.Gray.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.4f)
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray
    val priorityTextColor = if (isDarkTheme) Color.White else Color.Black
    val voteIconColor = if (report.has_voted == true) PrimaryColor else textColorSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = rememberAsyncImagePainter(report.image_path),
                contentDescription = "Foto Aduan",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.location_address ?: "Lokasi tidak diketahui",
                    color = PrimaryColor, // Biarkan warna utama untuk lokasi
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Oleh: ${report.user?.name ?: "Anonim"}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColorPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Skor: ${report.priority_score ?: 0.0}",
                    fontSize = 14.sp,
                    color = textColorSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = report.created_at ?: "Beberapa waktu lalu",
                    fontSize = 14.sp,
                    color = textColorSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = when (report.status?.lowercase()) {
                                "pending" -> Color(0xFFFFC107).copy(alpha = 0.2f); "pengecekan" -> Color(0xFF03A9F4).copy(alpha = 0.2f); "pengerjaan" -> Color(0xFFFF9800).copy(alpha = 0.2f); "selesai" -> Color(0xFF4CAF50).copy(alpha = 0.2f); "ditolak" -> Color(0xFFF44336).copy(alpha = 0.2f); else -> Color.Gray.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val (icon, color) = when (report.status?.lowercase()) {
                        "pending" -> Icons.Default.HourglassEmpty to Color(0xFFFFC107); "pengecekan" -> Icons.Default.Visibility to Color(0xFF03A9F4); "pengerjaan" -> Icons.Default.Settings to Color(0xFFFF9800); "selesai" -> Icons.Default.CheckCircle to Color(0xFF4CAF50); "ditolak" -> Icons.Default.Cancel to Color(0xFFF44336); else -> Icons.Default.HelpOutline to Color.Gray
                    }
                    Icon(imageVector = icon, contentDescription = "Status Icon", tint = color, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = report.status?.replaceFirstChar { it.uppercase() } ?: "Status", color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = report.has_voted != true) { onVote() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (report.has_voted == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Dukung",
                    tint = voteIconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = (report.vote_count ?: 0).toString(),
                    color = voteIconColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        Text(
            text = "Priority #${report.rank ?: 0}",
            color = priorityTextColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}