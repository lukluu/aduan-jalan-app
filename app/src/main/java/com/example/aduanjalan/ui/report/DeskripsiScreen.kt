package com.example.aduanjalan.ui.report

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.aduanjalan.data.DataStoreManager
import com.example.aduanjalan.ui.component.SuccessPopup
import com.example.aduanjalan.ui.detection.DetectionViewModel
import com.example.aduanjalan.ui.theme.PrimaryColor
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeskripsiScreen(
    navController: NavHostController,
    detectionViewModel: DetectionViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }

    val detections by detectionViewModel.detections.collectAsState()
    val location = detectionViewModel.location.collectAsState()
    val address = detectionViewModel.address.collectAsState()
    // --- PERUBAHAN BARU: Ambil roadType dari ViewModel ---
    val roadType = detectionViewModel.roadType.collectAsState()
    val bitmap: Bitmap? = detectionViewModel.currentBitmap
    val criterias by detectionViewModel.criterias.collectAsState()

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    val selectedOptions = remember { mutableStateMapOf<Int, Int?>() }
    val showError = remember { mutableStateMapOf<Int, Boolean>() }

    var showSuccessPopup by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    val isFormComplete by remember {
        derivedStateOf {
            criterias.isNotEmpty() && criterias.size == selectedOptions.count { it.value != null }
        }
    }

    // --- Definisi Palet Warna Dinamis ---
    val isDarkTheme = isSystemInDarkTheme()
    val scaffoldBackgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFFAFAFA)
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF1F1F1F) else MaterialTheme.colorScheme.surface
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val bottomBarColor = if (isDarkTheme) Color(0xFF1F1F1F) else Color.White


    LaunchedEffect(Unit) {
        isRefreshing = true
        detectionViewModel.fetchCriterias()
        isRefreshing = false
    }

    Scaffold(
        containerColor = scaffoldBackgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Langkah 3/3 : Deskripsi Aduan", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
            )
        },
        bottomBar = {
            Surface(
                color = bottomBarColor,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            criterias.forEach { criteria ->
                                showError[criteria.id] = selectedOptions[criteria.id] == null
                            }
                            if (!isFormComplete) return@Button
                            scope.launch {
                                // PERUBAHAN PENTING: Panggil fungsi submit dengan data roadType
                                // (Walaupun roadType belum termasuk di argument submitReport, ini adalah tempat logikanya)
                                Log.d("DeskripsiScreen", "Road Type to be submitted: ${roadType.value}")

                                try {
                                    isSubmitting = true
                                    val token = dataStoreManager.getToken().first()
                                    val lat = location.value?.latitude ?: 0.0
                                    val lon = location.value?.longitude ?: 0.0
                                    val roadTypee = roadType.value ?: "-"
                                    val addr = address.value.ifEmpty { "Lokasi tidak diketahui" }
                                    if (bitmap == null) { isSubmitting = false; return@launch }
                                    val scaledBitmap = resizeBitmap(bitmap, 1024, 1024)
                                    val tempFile = File(context.cacheDir, "upload_image.jpg")
                                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, tempFile.outputStream())

                                    // Anda mungkin perlu menyesuaikan fungsi submitReport di ViewModel
                                    // jika server Anda memerlukan field 'roadType'
                                    detectionViewModel.submitReport(
                                        token = token, imageFile = tempFile, address = addr, lat = lat, lon = lon, road = roadTypee,
                                        detections = detections, answers = selectedOptions.mapValues { it.value!! }
                                    )
                                    successMessage = "Aduan berhasil dikirim"
                                    showSuccessPopup = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = isFormComplete && !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Aduan")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        detectionViewModel.fetchCriterias()
                        isRefreshing = false
                    }
                },
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Lokasi Aduan",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(12.dp))

                                // --- TAMPILAN ALAMAT ---
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    Text("📍", modifier = Modifier.padding(end = 8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Alamat", style = MaterialTheme.typography.bodySmall, color = textColorSecondary)
                                        Text(address.value.ifEmpty { "-" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textColorPrimary)
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // --- PERUBAHAN BARU: TAMPILAN JENIS JALAN ---
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    Text("🛣️", modifier = Modifier.padding(end = 8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Jenis Jalan", style = MaterialTheme.typography.bodySmall, color = textColorSecondary)
                                        Text(roadType.value ?: "-", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textColorPrimary)
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // --- TAMPILAN KOORDINAT ---
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                                        Text("🌐", modifier = Modifier.padding(end = 8.dp))
                                        Column {
                                            Text("Latitude", style = MaterialTheme.typography.bodySmall, color = textColorSecondary)
                                            Text(location.value?.latitude?.toString() ?: "-", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textColorPrimary)
                                        }
                                    }
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                                        Text("🌐", modifier = Modifier.padding(end = 8.dp))
                                        Column {
                                            Text("Longitude", style = MaterialTheme.typography.bodySmall, color = textColorSecondary)
                                            Text(location.value?.longitude?.toString() ?: "-", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textColorPrimary)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ... (Kode untuk Pertanyaan Deskripsi dan List Kriteria tetap sama)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📋 Pertanyaan Deskripsi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Mohon jawab beberapa pertanyaan berikut untuk melengkapi laporan Anda",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColorSecondary
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    items(criterias) { criteria ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                criteria.question?.let {
                                    Text(it, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(12.dp))
                                criteria.options.forEach { option ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedOptions[criteria.id] == option.id) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            } else {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                            }
                                        ),
                                        onClick = { selectedOptions[criteria.id] = option.id; showError[criteria.id] = false }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth()) {
                                            RadioButton(
                                                selected = selectedOptions[criteria.id] == option.id,
                                                onClick = { selectedOptions[criteria.id] = option.id; showError[criteria.id] = false }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(option.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.7f), color = textColorPrimary)
                                        }
                                    }
                                }
                                if (showError[criteria.id] == true) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("⚠️", modifier = Modifier.padding(end = 8.dp))
                                            Text("Mohon pilih salah satu opsi", color = Color.Red, fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Mengirim Aduan...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (showSuccessPopup) {
        SuccessPopup(
            message = successMessage,
            onDismiss = {
                showSuccessPopup = false
                detectionViewModel.resetAll()
                navController.navigate("my_reports") { popUpTo("home") { inclusive = false } }
            }
        )
    }
}

fun resizeBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val ratio = minOf(maxWidth.toFloat() / source.width, maxHeight.toFloat() / source.height)
    return if (ratio < 1f) {
        val width = (source.width * ratio).toInt()
        val height = (source.height * ratio).toInt()
        Bitmap.createScaledBitmap(source, width, height, true)
    } else source
}