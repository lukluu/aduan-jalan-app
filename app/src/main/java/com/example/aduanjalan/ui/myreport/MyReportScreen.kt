package com.example.aduanjalan.ui.myreport

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.aduanjalan.data.remote.response.MyReportResponse
import com.example.aduanjalan.ui.component.BottomBar
import com.example.aduanjalan.ui.component.SuccessPopup
import com.example.aduanjalan.ui.theme.PrimaryColor
import com.example.aduanjalan.ui.utils.shimmerEffect
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyReportScreen(
    navController: NavController,
    viewModel: MyReportsViewModel = hiltViewModel()
) {
    val reports by viewModel.reports.collectAsState()
    val isRefreshing by viewModel.refreshing.collectAsState()
    val isLoading by viewModel.loading.collectAsState() // Ambil state loading
    val deleteStatus by viewModel.deleteStatus.collectAsState()

    // ... (Kode state lain seperti showSuccessPopup, searchQuery, pagerState TETAP SAMA) ...
    // ... (Kode LaunchedEffect deleteStatus & navBackStackEntry TETAP SAMA) ...
    // ... (Variable tabs, filter logic TETAP SAMA) ...

    var showSuccessPopup by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val tabs = listOf("Semua Aduan", "Selesai Dikerjakan")

    val allReportsFiltered = remember(searchQuery, reports) {
        reports.filter {
            searchQuery.isBlank() ||
                    (it.detected_labels?.contains(searchQuery, true) == true) ||
                    (it.location_address?.contains(searchQuery, true) == true)
        }
    }
    val completedReportsFiltered = remember(searchQuery, reports) {
        reports.filter {
            (it.status?.contains("selesai", true) == true) &&
                    (searchQuery.isBlank() ||
                            (it.detected_labels?.contains(searchQuery, true) == true) ||
                            (it.location_address?.contains(searchQuery, true) == true))
        }
    }

    LaunchedEffect(deleteStatus) {
        deleteStatus?.let { (success, message) ->
            if (success) {
                successMessage = message
                showSuccessPopup = true
            }
            viewModel.clearDeleteStatus()
        }
    }

    val navBackStackEntry = navController.currentBackStackEntry
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("refreshMyReports")
            ?.observeForever { refresh ->
                if (refresh == true) {
                    viewModel.refreshReports()
                    navBackStackEntry.savedStateHandle["refreshMyReports"] = false
                }
            }
    }

    // Definisi Warna
    val isDarkTheme = isSystemInDarkTheme()
    val scaffoldBackgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFFAFAFA)
    val tabRowColor = if (isDarkTheme) Color(0xFF1F1F1F) else Color.White
    val unselectedTabColor = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray

    Scaffold(
        containerColor = scaffoldBackgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Aduan Saya", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
            )
        },
        bottomBar = { BottomBar(navController = navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari lokasi atau jenis kerusakan") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Tab Row
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = tabRowColor,
                    contentColor = PrimaryColor,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                val count = if (index == 0) allReportsFiltered.size else completedReportsFiltered.size
                                Text(
                                    text = "$title ($count)",
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            unselectedContentColor = unselectedTabColor
                        )
                    }
                }

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val reportsToShow = when (page) {
                        0 -> allReportsFiltered
                        else -> completedReportsFiltered
                    }

                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshing),
                        onRefresh = { viewModel.refreshReports() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // UPDATE: Kirim parameter isLoading ke ReportList
                        ReportList(
                            reports = reportsToShow,
                            isLoading = isLoading, // <-- Parameter Baru
                            onDelete = { reportId -> viewModel.deleteReport(reportId) },
                            onClick = { reportId -> navController.navigate("reportDetailMap/$reportId") }
                        )
                    }
                }
            }

            if (showSuccessPopup) {
                SuccessPopup(
                    message = successMessage,
                    onDismiss = { showSuccessPopup = false }
                )
            }
        }
    }
}

@Composable
private fun ReportList(
    reports: List<MyReportResponse>,
    isLoading: Boolean, // <-- Terima parameter isLoading
    onDelete: (Int) -> Unit,
    onClick: (Int) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val emptyTextColor = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray

    // LOGIKA SKELETON LOADING
    if (isLoading && reports.isEmpty()) {
        // Tampilkan Skeleton jika sedang loading DAN data belum ada
        ReportListSkeleton()
    } else if (reports.isEmpty()) {
        // Tampilkan Pesan Kosong jika TIDAK loading tapi data kosong
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada data aduan ditemukan", color = emptyTextColor)
                    }
                }
            }
        }
    } else {
        // Tampilkan Data Asli
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(reports, key = { it.id ?: -1 }) { report ->
                ReportItem(
                    report = report,
                    onDelete = { onDelete(report.id ?: -1) },
                    onClick = { onClick(report.id ?: -1) }
                )
            }
        }
    }
}

@Composable
fun ReportItem(
    report: MyReportResponse,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // --- LANGKAH 3: BUAT ReportItem MENJADI THEME-AWARE ---
    val isDarkTheme = isSystemInDarkTheme()
    val cardColor = if (isDarkTheme) Color(0xFF1F1F1F) else Color.White
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray
    val priorityTextColor = if (isDarkTheme) Color.White else Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = rememberAsyncImagePainter(report.image_url),
                    contentDescription = "Foto Aduan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.location_address ?: "Lokasi tidak diketahui",
                        color = PrimaryColor, // Biarkan PrimaryColor untuk judul
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = report.detected_labels ?: "-",
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
                                    "pending" -> Color(0xFFFFC107).copy(alpha = 0.2f)
                                    "pengecekan" -> Color(0xFF03A9F4).copy(alpha = 0.2f)
                                    "pengerjaan" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    "selesai" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    "ditolak" -> Color(0xFFF44336).copy(alpha = 0.2f)
                                    else -> Color.Gray.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val (icon, color) = when (report.status?.lowercase()) {
                            "pending" -> Icons.Default.HourglassEmpty to Color(0xFFFFC107)
                            "pengecekan" -> Icons.Default.Visibility to Color(0xFF03A9F4)
                            "pengerjaan" -> Icons.Default.Settings to Color(0xFFFF9800)
                            "selesai" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                            "ditolak" -> Icons.Default.Cancel to Color(0xFFF44336)
                            else -> Icons.Default.HelpOutline to Color.Gray
                        }
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = report.status?.replaceFirstChar { it.uppercase() } ?: "Pending",
                            color = color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(modifier = Modifier.align(Alignment.Top)) {
                    IconButton(onClick = { expanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", tint = textColorSecondary)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        // DropdownMenu juga perlu warna latar belakang dinamis
                        modifier = Modifier.background(cardColor)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus") },
                            text = { Text(text = "Hapus Aduan", fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                expanded = false
                                showConfirmDialog = true
                            }
                        )
                    }
                }
            }

            Text(
                text = "Priority #${report.rank ?: "N/A"}",
                color = priorityTextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }

    if (showConfirmDialog) {
        // AlertDialog dari Material 3 sudah mendukung tema gelap secara default
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onDelete()
                }) {
                    Text("Hapus", color = Color.Red)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Batal") } },
            title = { Text("Hapus Aduan?") },
            text = { Text("Apakah kamu yakin ingin menghapus Aduan ini?") }
        )
    }
}
@Composable
fun ReportListSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(5) { // Tampilkan 5 dummy item
            ReportItemSkeleton()
        }
    }
}

@Composable
fun ReportItemSkeleton() {
    val isDarkTheme = isSystemInDarkTheme()
    val cardColor = if (isDarkTheme) Color(0xFF1F1F1F) else Color.White

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Dummy Gambar
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Dummy Judul (Lokasi)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dummy Label
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dummy Tanggal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dummy Badge Status
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}