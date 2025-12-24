package com.example.aduanjalan.ui.allreport

import android.annotation.SuppressLint
import android.graphics.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.aduanjalan.R
import com.example.aduanjalan.data.remote.response.AllReportResponse
import com.example.aduanjalan.ui.component.BottomBar
import com.example.aduanjalan.ui.component.TopBar
import com.example.aduanjalan.ui.theme.PrimaryColor
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllReportsScreen(
    navController: NavController,
    viewModel: AllReportsViewModel = hiltViewModel(),
    selectedReportId: Int? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val reports by viewModel.filteredReports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var currentLatLng by remember { mutableStateOf<LatLng?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedReport by remember { mutableStateOf<AllReportResponse?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-4.0562, 122.5489), 13f)
    }

    val markerBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

    // --- LANGKAH 1: DEFINISIKAN PALET WARNA DINAMIS ---
    val isDarkTheme = isSystemInDarkTheme()
    val bottomSheetColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White
    val textFieldContainerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White
    val imagePlaceholderBgColor = if (isDarkTheme) Color.DarkGray else Color.LightGray
    val dividerColor = if (isDarkTheme) Color.Gray.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.5f)
    val bottomBarWrapperColor = if (isDarkTheme) Color(0xFF1F1F1F) else Color.White

    // LaunchedEffect tidak berubah, kecuali pemanggilan helper
    LaunchedEffect(Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { loc ->
            loc?.let { currentLatLng = LatLng(it.latitude, it.longitude) }
        }
        viewModel.loadReports()
    }

    LaunchedEffect(currentLatLng) {
        currentLatLng?.let { cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(it, 14f), durationMs = 800) }
    }

    LaunchedEffect(reports) {
        val loader = ImageLoader(context)
        reports.forEach { report ->
            if (report.image_path != null && !markerBitmaps.containsKey(report.id)) {
                val request = ImageRequest.Builder(context).data(report.image_path).allowHardware(false).size(160, 160).build()
                val result = withContext(Dispatchers.IO) {
                    runCatching { (loader.execute(request) as? SuccessResult)?.drawable?.toBitmap(160, 160) }.getOrNull()
                }
                val finalBitmap = result?.let { addStatusBorder(it, report.status) }
                finalBitmap?.let { markerBitmaps[report.id] = it }
            }
        }
    }

    LaunchedEffect(selectedReportId, reports) {
        if (selectedReportId != null && reports.isNotEmpty()) {
            val reportToShow = reports.find { it.id == selectedReportId }
            if (reportToShow != null) {
                selectedReport = reportToShow
                showSheet = true
                reportToShow.latitude?.let { lat ->
                    reportToShow.longitude?.let { lng ->
                        val reportPosition = LatLng(lat, lng)
                        scope.launch { cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(reportPosition, 16f), durationMs = 600) }
                    }
                }
            }
        }
    }

    val mapProperties = remember(isDarkTheme) {
        val styleJson = if (isDarkTheme) R.raw.map_style_dark else R.raw.map_style_light
        MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, styleJson),
            isMyLocationEnabled = false
        )
    }

    Scaffold(
        topBar = { TopBar() },
        bottomBar = {
            // --- LANGKAH 2: BUAT PEMBUNGKUS BOTTOM BAR THEME-AWARE ---
            Box(Modifier.background(bottomBarWrapperColor)) {
                BottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                properties = mapProperties
            ) {
                reports.forEach { report ->
                    val lat = report.latitude ?: return@forEach
                    val lng = report.longitude ?: return@forEach
                    val pos = LatLng(lat, lng)
                    val bitmap = markerBitmaps[report.id]
                    val labelText = "Priority #${report.rank ?: 0} - Status ${report.status?.ifEmpty { "Aduan" } ?: "Aduan"}"
                    // Pass isDarkTheme agar label marker beradaptasi
                    val markerWithLabel = bitmap?.let { createMarkerWithLabel(it, labelText, isDarkTheme) }

                    Marker(
                        state = MarkerState(position = pos),
                        title = report.status ?: "Aduan",
                        snippet = report.location_address ?: "",
                        icon = markerWithLabel?.let { BitmapDescriptorFactory.fromBitmap(it) },
                        onClick = {
                            selectedReport = report; showSheet = true; scope.launch { cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(pos, 16f), durationMs = 600) }; true
                        }
                    )
                }
            }

            // --- LANGKAH 3: BUAT SEARCH BAR THEME-AWARE ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = 45.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; viewModel.updateSearch(it) },
                    placeholder = { Text("Cari lokasi...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") }, // Tint akan otomatis
                    singleLine = true,
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).shadow(6.dp, RoundedCornerShape(30.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = textFieldContainerColor,
                        unfocusedContainerColor = textFieldContainerColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                        // Warna cursor dan teks akan otomatis diatur oleh Material 3
                    )
                )
            }

            // FAB tidak perlu diubah karena warnanya tegas (PrimaryColor)
            FloatingActionButton(onClick = { navController.navigate("all_reports_list") }, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 90.dp, end = 16.dp), containerColor = PrimaryColor) { Icon(imageVector = Icons.Default.List, contentDescription = "Daftar Aduan", tint = Color.White) }
            FloatingActionButton(onClick = { currentLatLng?.let { latLng -> scope.launch { cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(latLng, 16f), durationMs = 600) } } }, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp, end = 16.dp), containerColor = PrimaryColor) { Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Lokasi Saya", tint = Color.White) }
            if (isLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(48.dp)) }
            error?.let { Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center)) }

            // --- LANGKAH 4: BUAT BOTTOM SHEET DETAIL THEME-AWARE ---
            if (showSheet && selectedReport != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp

                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                    containerColor = bottomSheetColor, // Terapkan warna dinamis
                    shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
                ) {
                    selectedReport?.let { report ->
                        var currentReport by remember { mutableStateOf(report) }
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight * 0.7f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(currentReport.image_path ?: ""),
                                contentDescription = "Foto Aduan",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(imagePlaceholderBgColor) // Terapkan warna dinamis
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically // <-- Tambahkan ini untuk menyejajarkan ikon dan teks
                            ) {
                                if (currentReport.has_voted != true) {
                                    // --- Tombol "Dukung" yang bisa diklik ---
                                    Button(
                                        onClick = {
                                            viewModel.voteReport(currentReport.id)
                                            currentReport = currentReport.copy(
                                                has_voted = true,
                                                vote_count = (currentReport.vote_count ?: 0) + 1
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp) // <-- Atur padding
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ThumbUp,
                                            contentDescription = "Dukung",
                                            modifier = Modifier.size(18.dp) // <-- Atur ukuran ikon
                                        )
                                        Spacer(modifier = Modifier.width(8.dp)) // <-- Beri jarak antara ikon dan teks
                                        Text("Dukung", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    // --- Teks "Kamu Sudah Dukung" (status non-aktif) ---
                                    val votedTextColor = if (isDarkTheme) Color.LightGray else Color.Gray
                                    val votedBackgroundColor = if (isDarkTheme) Color(0x22FFFFFF) else Color(0x22000000)

                                    // Gunakan Row lagi untuk menampung ikon dan teks
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(votedBackgroundColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp) // <-- Atur padding
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ThumbUp,
                                            contentDescription = "Sudah Didukung",
                                            tint = votedTextColor, // <-- Beri warna yang sama dengan teks
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Kamu Sudah Dukung",
                                            color = votedTextColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Black
                                Text(text = currentReport.location_address ?: "-", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = PrimaryColor)
                                Text(text = currentReport.road_type ?: "-",color = textColorSecondary, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal))
                                Text(text = currentReport.detected_labels ?: "Jenis kerusakan tidak diketahui",color = textColorSecondary, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            }
                            Divider(color = dividerColor) // Terapkan warna dinamis
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Panggil versi theme-aware dari DetailItem
                                DetailItemThemeAware(
                                    label = "Status",
                                    value = currentReport.status ?: "-",
                                    icon = when (currentReport.status?.lowercase()) {
                                        "pending" -> Icons.Default.HourglassEmpty
                                        "pengecekan" -> Icons.Default.Visibility
                                        "pengerjaan" -> Icons.Default.Settings
                                        "selesai" -> Icons.Default.CheckCircle
                                        "ditolak" -> Icons.Default.Cancel
                                        else -> Icons.Default.HelpOutline
                                    },
                                    iconTint = when (currentReport.status?.lowercase()) {
                                        "pending" -> Color(0xFFFFC107)
                                        "pengecekan" -> Color(0xFF03A9F4)
                                        "pengerjaan" -> Color(0xFFFF9800)
                                        "selesai" -> Color(0xFF4CAF50)
                                        "ditolak" -> Color(0xFFF44336)
                                        else -> Color.Gray
                                    }
                                )
                                DetailItemThemeAware(
                                    label = "Aduan Oleh",
                                    value = currentReport.user?.name ?: "-",
                                    icon = Icons.Default.Person
                                )
                                DetailItemThemeAware(
                                    label = "Skor Prioritas",
                                    value = currentReport.priority_score?.toString() ?: "-",
                                    icon = Icons.Default.Star,
                                    iconTint = PrimaryColor
                                )
                                DetailItemThemeAware(
                                    label = "Peringkat",
                                    value = "#${currentReport.rank ?: 0}",
                                    icon = Icons.Default.BarChart,
                                    iconTint = PrimaryColor
                                )
                                DetailItemThemeAware(
                                    label = "Jumlah Dukungan",
                                    value = currentReport.vote_count?.toString() ?: "0",
                                    icon = Icons.Default.ThumbUp
                                )
                                DetailItemThemeAware(
                                    label = "Tanggal Aduan",
                                    value = currentReport.created_at ?: "-",
                                    icon = Icons.Default.Event
                                )
                            }
                            if (!currentReport.answers.isNullOrEmpty()) {
                                Divider(color = dividerColor) // Terapkan warna dinamis
                                Text(text = "Detail Jawaban Penilaian:", fontWeight = FontWeight.Bold, color = PrimaryColor)
                                val answers = currentReport.answers ?: emptyList()
                                answers.forEach { ans ->
                                    // Panggil versi theme-aware dari DetailItemAnswer
                                    DetailItemAnswerThemeAware(label = ans.criteria_name ?: "-", value = ans.option_label ?: "-")
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = { showSheet = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("Tutup", fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}

// --- LANGKAH 5: PERBARUI SEMUA FUNGSI HELPER AGAR THEME-AWARE ---

fun android.graphics.drawable.Drawable.toBitmap(width: Int, height: Int): Bitmap {
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return bitmap
}

fun addStatusBorder(src: Bitmap, status: String?): Bitmap {
    val borderWidth = 10f
    val size = 160
    val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val borderColor = when (status?.lowercase()) {
        "pending" -> Color(0xFFFFC107).toArgb(); "pengecekan" -> Color(0xFF03A9F4).toArgb(); "pengerjaan" -> Color(0xFFFF9800).toArgb(); "selesai" -> Color(0xFF4CAF50).toArgb(); "ditolak" -> Color(0xFFF44336).toArgb(); else -> Color.DarkGray.toArgb()
    }
    paint.style = Paint.Style.FILL; paint.color = borderColor
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    val rect = RectF(borderWidth, borderWidth, size - borderWidth, size - borderWidth)
    val imageShader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = imageShader }
    canvas.drawOval(rect, imagePaint)
    return result
}

// Perbarui createMarkerWithLabel agar menerima isDarkTheme
fun createMarkerWithLabel(image: Bitmap, label: String, isDarkTheme: Boolean): Bitmap {
    val textColor = if (isDarkTheme) Color.White.toArgb() else Color.Black.toArgb()
    val textBackgroundColor = if (isDarkTheme) Color.Black.copy(alpha = 0.7f).toArgb() else Color.White.copy(alpha = 0.8f).toArgb()
    val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textColor; textSize = 28f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    val textWidth = paintText.measureText(label)
    val width = maxOf(image.width.toFloat(), textWidth + 24f).toInt()
    val height = image.height + 40
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textBackgroundColor }
    val rect = RectF(0f, 0f, width.toFloat(), 38f)
    canvas.drawRoundRect(rect, 20f, 20f, paintBg)
    canvas.drawText(label, width / 2f, 28f, paintText)
    canvas.drawBitmap(image, ((width - image.width) / 2f), 38f, null)
    return result
}

// Buat versi theme-aware dari komponen detail agar tidak konflik dengan impor
@Composable
private fun DetailItemThemeAware(
    label: String,
    value: String,
    icon: ImageVector? = null, // <-- Parameter baru untuk ikon
    iconTint: Color = Color.Unspecified // <-- Parameter baru untuk warna ikon
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically // <-- Pusatkan item secara vertikal
    ) {
        // Tampilkan ikon jika ada
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (iconTint != Color.Unspecified) iconTint else textColorSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Teks Label
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            color = textColorSecondary
        )

        Spacer(modifier = Modifier.weight(1f)) // <-- Dorong value ke ujung kanan

        // Teks Value
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            color = textColorPrimary
        )
    }
}

@Composable
private fun DetailItemAnswerThemeAware(label: String, value: String) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        if (value.isBlank()) {
            Text(text = label, color = textColorSecondary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        } else {
            Text(text = label, color = textColorSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = value, color = textColorPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}