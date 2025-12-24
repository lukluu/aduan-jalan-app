package com.example.aduanjalan.ui.report

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.aduanjalan.domain.model.Detection
import com.example.aduanjalan.ui.detection.DetectionViewModel
import com.example.aduanjalan.ui.theme.PrimaryColor
import com.example.aduanjalan.ui.utils.loadCorrectlyOrientedBitmap
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun UploadImageScreen(
    navController: NavHostController,
    viewModel: DetectionViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()
    val detectionList by viewModel.detections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)
    val currentBitmap by viewModel.latestProcessedBitmap.collectAsState()

    var hasProcessedCameraResult by rememberSaveable { mutableStateOf(false) }

    // --- LANGKAH 1: DEFINISIKAN PALET WARNA DINAMIS ---
    val isDarkTheme = isSystemInDarkTheme()
    val scaffoldBackgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFFAFAFA)
    val bottomSurfaceColor = if (isDarkTheme) Color(0xFF1F1F1F) else MaterialTheme.colorScheme.surface
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant


    LaunchedEffect(navController.currentBackStackEntry) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        val imageUri = savedStateHandle?.get<Uri>("captured_image_uri")
        if (imageUri != null) {
            val bitmap = try { loadCorrectlyOrientedBitmap(context, imageUri) } catch (e: Exception) { null }
            bitmap?.let {
                viewModel.setCurrentBitmapOnly(it)
                savedStateHandle.get<List<Detection>>("captured_detections")?.let { detections -> viewModel.setDetections(detections) }
            }
            savedStateHandle.remove<Uri>("captured_image_uri")
            savedStateHandle.remove<List<Detection>>("captured_detections")
            hasProcessedCameraResult = true
        } else {
            if (!hasProcessedCameraResult) {
                viewModel.prepareForNewReport()
            }
        }
    }

    val pickGalleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = try { loadCorrectlyOrientedBitmap(context, it) } catch (_: Exception) { null }
            bitmap?.let { viewModel.setBitmapAndDetect(it); hasProcessedCameraResult = true }
        }
    }

    Scaffold(
        // --- LANGKAH 2: TERAPKAN WARNA LATAR BELAKANG ---
        containerColor = scaffoldBackgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor), // TopAppBar tetap PrimaryColor
                title = { Text("Langkah 1/3: Unggah Gambar", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.resetAll(); navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            if (currentBitmap != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    // Terapkan warna surface dinamis
                    color = bottomSurfaceColor
                ) {
                    Column(
                        modifier = Modifier
                            .padding(WindowInsets.navigationBars.asPaddingValues())
                            .padding(16.dp)
                    ) {
                        // Terapkan warna teks dinamis
                        Text("Hasil Analisis Citra", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColorPrimary)
                        Spacer(Modifier.height(12.dp))

                        if (isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Menganalisis gambar...", color = textColorSecondary)
                            }
                        } else if (detectionList.isEmpty()) {
                            Text("Tidak terdeteksi kerusakan jalan.", color = textColorSecondary)
                        } else {
                            val grouped = remember(detectionList) { detectionList.groupBy { it.label }.mapValues { it.value.size } }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Ditemukan kerusakan:", fontWeight = FontWeight.SemiBold, color = textColorPrimary)
                                grouped.forEach { (label, count) ->
                                    Text("• $count ${label.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodyMedium, color = textColorPrimary)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (detectionList.isNotEmpty() && !isLoading) {
                            Button(onClick = { navController.navigate("map_picker") }, modifier = Modifier.fillMaxWidth()) {
                                Text("Sesuaikan Lokasi")
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { coroutineScope.launch { currentBitmap?.let { viewModel.detect(it) } } },
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // DashedUploadButton dari M3 sudah cukup adaptif
                DashedUploadButton(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    onClick = { showDialog = true },
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (currentBitmap != null) {
                    currentBitmap?.asImageBitmap()?.let { bitmap ->
                        ImageWithBoundingBoxes(
                            imageBitmap = bitmap,
                            detections = detectionList,
                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pratinjau gambar akan ditampilkan di sini setelah Anda mengunggahnya.",
                            // Terapkan warna teks dinamis
                            color = textColorSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }

        // AlertDialog dari M3 sudah theme-aware
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Pilih Sumber Gambar") },
                confirmButton = {
                    TextButton(onClick = { showDialog = false; navController.navigate("camera_realtime") }) { Text("Kamera") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false; pickGalleryLauncher.launch("image/*") }) { Text("Galeri") }
                }
            )
        }
    }
}


@Composable
fun DashedUploadButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    // --- LANGKAH 3: BUAT DASHED BUTTON MENJADI THEME-AWARE ---
    val isDarkTheme = isSystemInDarkTheme()
    val primaryColor = PrimaryColor // Warna utama tetap sama
    val backgroundColor = if(isDarkTheme) primaryColor.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = !isLoading) { onClick() }
            .drawBehind {
                drawRoundRect(
                    color = primaryColor,
                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp).let {
                if (isLoading) it.alpha(0.5f) else it
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.FileUpload,
                contentDescription = "Unggah",
                tint = primaryColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Unggah Gambar Kerusakan",
                color = primaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ImageWithBoundingBoxes tidak perlu diubah karena warna deteksi (merah, biru, putih)
// adalah warna fungsional yang harus tetap konsisten.
@SuppressLint("UnusedBoxWithConstraintsScope", "DefaultLocale")
@Composable
fun ImageWithBoundingBoxes(imageBitmap: ImageBitmap, detections: List<Detection>, modifier: Modifier = Modifier) {
    val originalWidth = imageBitmap.width.toFloat()
    val originalHeight = imageBitmap.height.toFloat()
    val aspectRatio = originalWidth / originalHeight
    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val scaleX = canvasWidth / originalWidth
            val scaleY = canvasHeight / originalHeight
            drawImage(image = imageBitmap, dstSize = IntSize(canvasWidth.toInt(), canvasHeight.toInt()))
            detections.forEach { item ->
                val left = item.bbox_x * scaleX
                val top = item.bbox_y * scaleY
                val width = item.bbox_width * scaleX
                val height = item.bbox_height * scaleY
                drawRect(color = Color.Red, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(width, height), style = Stroke(width = 4f))
                drawRect(color = Color(0xBF1C7ED6), topLeft = Offset(left, (top - 40f).coerceAtLeast(0f)), size = androidx.compose.ui.geometry.Size(width, 40f))
                drawContext.canvas.nativeCanvas.drawText(
                    "${item.label} (${String.format("%.1f", item.confidence)}%)",
                    left + 10f,
                    (top - 12f).coerceAtLeast(30f),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 32f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}