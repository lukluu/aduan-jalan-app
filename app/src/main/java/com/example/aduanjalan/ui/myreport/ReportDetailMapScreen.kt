package com.example.aduanjalan.ui.myreport

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.aduanjalan.ui.component.SuccessPopup
import com.example.aduanjalan.ui.theme.PrimaryColor
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailMapScreen(
    navController: NavController,
    reportId: Int?,
    viewModel: MyReportsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val reports by viewModel.reports.collectAsState()
    val deleteStatus by viewModel.deleteStatus.collectAsState()

    // --- LANGKAH 1: DEFINISIKAN PALET WARNA DINAMIS ---
    val isDarkTheme = isSystemInDarkTheme()
    val bottomSheetColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White
    val textFieldContainerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White
    val imagePlaceholderBgColor = if (isDarkTheme) Color.DarkGray else Color.LightGray
    val dividerColor = if (isDarkTheme) Color.Gray.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.5f)
    val loadingTextColor = if (isDarkTheme) Color.LightGray else Color.Gray

    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Memuat aduan kamu...", color = loadingTextColor)
        }
        return
    }

    val initialReport = reports.find { it.id == reportId } ?: reports.first()
    val initialLatLng = LatLng(initialReport.latitude ?: 0.0, initialReport.longitude ?: 0.0)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 15.5f)
    }

    var selectedReport by remember { mutableStateOf(initialReport) }
    var showSheet by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessPopup by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredReports = remember(searchQuery, reports) {
        if (searchQuery.isBlank()) reports else reports.filter {
            it.location_address?.contains(searchQuery, true) == true ||
                    it.status?.contains(searchQuery, true) == true
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val markerBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

    // Gaya peta dinamis Anda sudah benar
    val mapProperties = remember(isDarkTheme) {
        val styleJson = if (isDarkTheme) R.raw.map_style_dark else R.raw.map_style_light
        MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, styleJson),
            isMyLocationEnabled = false
        )
    }

    // LaunchedEffect tidak berubah
    LaunchedEffect(filteredReports) {
        val loader = ImageLoader(context)
        filteredReports.forEach { report ->
            if (report.image_url != null && !markerBitmaps.containsKey(report.id)) {
                val request = ImageRequest.Builder(context).data(report.image_url).allowHardware(false).size(120, 120).build()
                val result = withContext(Dispatchers.IO) { runCatching { (loader.execute(request) as? SuccessResult)?.drawable?.toBitmap(120, 120) }.getOrNull() }
                val finalBitmap = result?.let { addStatusBorder(it, report.status, isDarkTheme) } // Pass isDarkTheme ke border
                finalBitmap?.let { markerBitmaps[report.id] = it }
            }
        }
    }

    LaunchedEffect(deleteStatus) {
        if (deleteStatus != null) {
            val (success, _) = deleteStatus!!
            if (success) {
                showSheet = false
                showSuccessPopup = true
            }
            viewModel.clearDeleteStatus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
            properties = mapProperties
        ) {
            filteredReports.forEach { report ->
                key(report.id) {
                    val latLng = LatLng(report.latitude ?: 0.0, report.longitude ?: 0.0)
                    val bitmap = markerBitmaps[report.id]
                    val labelText = "Priority #${report.rank ?: 0}"

                    // Buat marker dengan label yang theme-aware
                    val markerWithLabel = bitmap?.let { createMarkerWithLabel(it, labelText, isDarkTheme) }

                    Marker(
                        state = MarkerState(position = latLng),
                        title = report.location_address ?: "Lokasi Aduan",
                        icon = markerWithLabel?.let { BitmapDescriptorFactory.fromBitmap(it) },
                        onClick = {
                            selectedReport = report; showSheet = true; scope.launch { cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(latLng, 17f), durationMs = 600) }; true
                        }
                    )
                }
            }
        }

        // Search & Back
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = 60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(48.dp).background(PrimaryColor, RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))

            // --- LANGKAH 2: BUAT TEXTFIELD MENJADI THEME-AWARE ---
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari alamat atau status aduan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                singleLine = true,
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.weight(1f).height(50.dp),
                colors = TextFieldDefaults.colors(
                    // Terapkan warna dinamis
                    focusedContainerColor = textFieldContainerColor,
                    unfocusedContainerColor = textFieldContainerColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // --- LANGKAH 3: BUAT BOTTOM SHEET MENJADI THEME-AWARE ---
        if (showSheet) {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = bottomSheetColor, // Terapkan warna dinamis
                shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
            ) {
                selectedReport.let { report ->
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight * 0.65f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(report.image_url ?: ""),
                            contentDescription = "Foto Aduan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(imagePlaceholderBgColor) // Terapkan warna dinamis
                        )

                        // Sisa konten di dalam Column
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Black
                            Text(text = report.location_address ?: "-", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = PrimaryColor)
                            Text(text = report.road_type ?: "-",color = textColorSecondary, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal))
                            Text(text = report.detected_labels ?: "Jenis kerusakan tidak diketahui",color = textColorSecondary, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }

                        Divider(color = dividerColor) // Terapkan warna dinamis

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailItemThemeAware(
                                label = "Status",
                                value = report.status ?: "-",
                                icon = when (report.status?.lowercase()) {
                                    "pending" -> Icons.Default.HourglassEmpty
                                    "pengecekan" -> Icons.Default.Visibility
                                    "pengerjaan" -> Icons.Default.Settings
                                    "selesai" -> Icons.Default.CheckCircle
                                    "ditolak" -> Icons.Default.Cancel
                                    else -> Icons.Default.HelpOutline
                                },
                                iconTint = when (report.status?.lowercase()) {
                                    "pending" -> Color(0xFFFFC107)
                                    "pengecekan" -> Color(0xFF03A9F4)
                                    "pengerjaan" -> Color(0xFFFF9800)
                                    "selesai" -> Color(0xFF4CAF50)
                                    "ditolak" -> Color(0xFFF44336)
                                    else -> Color.Gray
                                }
                            )
                            DetailItemThemeAware(
                                label = "Skor Prioritas",
                                value = report.priority_score?.toString() ?: "-",
                                icon = Icons.Default.Star,
                                iconTint = PrimaryColor
                            )
                            DetailItemThemeAware(
                                label = "Peringkat",
                                value = "#${report.rank ?: 0}",
                                icon = Icons.Default.BarChart,
                                iconTint = PrimaryColor
                            )
                            DetailItemThemeAware(
                                label = "Jumlah Vote",
                                value = report.vote_count?.toString() ?: "0",
                                icon = Icons.Default.ThumbUp
                            )
                            DetailItemThemeAware(
                                label = "Tanggal Aduan",
                                value = report.created_at ?: "-",
                                icon = Icons.Default.Event
                            )
                        }

                        Divider(color = dividerColor) // Terapkan warna dinamis

                        if (!report.answers.isNullOrEmpty()) {
                            Text(
                                text = "Detail Jawaban Penilaian:",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryColor
                            )
                            report.answers.forEach { ans ->
                                DetailItemAnswer(
                                    label = ans.criteria_name ?: "-",
                                    value = ans.option_label ?: "-"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { showSheet = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("Tutup Detail", fontWeight = FontWeight.SemiBold) }
                            OutlinedButton(onClick = { showConfirmDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red); Spacer(modifier = Modifier.width(6.dp)); Text("Hapus Aduan") }
                        }
                    }
                }
            }
        }

        // AlertDialog dari M3 sudah theme-aware
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Hapus Aduan") },
                text = { Text("Apakah kamu yakin ingin menghapus aduan ini?") },
                confirmButton = { TextButton(onClick = { showConfirmDialog = false; viewModel.deleteReport(selectedReport.id ?: return@TextButton) }) { Text("Ya, Hapus", color = Color.Red) } },
                dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Batal") } }
            )
        }

        if (showSuccessPopup) {
            SuccessPopup(
                message = "Aduan berhasil dihapus",
                onDismiss = { showSuccessPopup = false; navController.previousBackStackEntry?.savedStateHandle?.set("refreshMyReports", true); navController.popBackStack() }
            )
        }
    }
}

// Konversi drawable tidak berubah
fun android.graphics.drawable.Drawable.toBitmap(width: Int, height: Int): Bitmap {
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return bitmap
}

// Tambahkan border warna tidak berubah
fun addStatusBorder(src: Bitmap, status: String?, isDarkTheme: Boolean): Bitmap {
    // ... (Fungsi ini tidak diubah, tapi kita bisa membuatnya lebih theme-aware jika perlu)
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


// --- LANGKAH 4: BUAT DETAILITEM MENJADI THEME-AWARE ---
@Composable
fun DetailItemThemeAware(
    label: String,
    value: String,
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (iconTint != Color.Unspecified) iconTint else textColorSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            color = textColorSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            color = textColorPrimary
        )
    }
}

@Composable
fun DetailItemAnswer(label: String, value: String) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        // Jika value kosong, anggap ini sub-judul
        if (value.isBlank()) {
            Text(text = label, color = textColorSecondary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        } else {
            Text(text = label, color = textColorSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = value, color = textColorPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}


fun createMarkerWithLabel(image: Bitmap, label: String, isDarkTheme: Boolean): Bitmap {
    // --- LANGKAH 5: BUAT MARKER MENJADI THEME-AWARE ---
    val textColor = if (isDarkTheme) Color.White.toArgb() else Color.Black.toArgb()
    val textBackgroundColor = if (isDarkTheme) Color.Black.copy(alpha = 0.7f).toArgb() else Color.White.copy(alpha = 0.8f).toArgb()

    val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor; textSize = 28f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    val textWidth = paintText.measureText(label)

    val width = maxOf(image.width.toFloat(), textWidth + 24f).toInt()
    val height = image.height + 40

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textBackgroundColor }
    val rect = RectF(0f, 0f, width.toFloat(), 38f)
    canvas.drawRoundRect(rect, 20f, 20f, paintBg) // Latar belakang dengan sudut tumpul

    canvas.drawText(label, width / 2f, 28f, paintText)
    canvas.drawBitmap(image, ((width - image.width) / 2f), 38f, null)

    return result
}