package com.example.aduanjalan.ui.report

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.aduanjalan.R
import com.example.aduanjalan.data.common.UiState
import com.example.aduanjalan.data.remote.response.WilayahItem
import com.example.aduanjalan.ui.detection.DetectionViewModel
import com.example.aduanjalan.ui.theme.PrimaryColor
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale

// --- HELPER FUNCTION: Parse JSON Array dari Backend ---
// Mengubah string "[[122.5, -4.0], ...]" menjadi List<LatLng>
fun parseGeoJsonPolyline(jsonString: String?): List<LatLng> {
    if (jsonString.isNullOrEmpty()) return emptyList()
    val points = mutableListOf<LatLng>()
    try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val point = jsonArray.getJSONArray(i)
            // OSRM Format: [Longitude, Latitude]
            val lon = point.getDouble(0)
            val lat = point.getDouble(1)
            points.add(LatLng(lat, lon))
        }
    } catch (e: Exception) {
        Log.e("PolylineParse", "Error parsing GeoJSON: ${e.message}")
        return emptyList()
    }
    return points
}

data class AddressDetail(
    val fullAddress: String,
    val street: String?,
    val district: String?,
    val city: String?,
    val province: String?
)

@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    detectionViewModel: DetectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    val coroutineScope = rememberCoroutineScope()

    // State dari ViewModel
    val isCheckingRoad by detectionViewModel.isCheckingRoad.collectAsState()
    val roadType by detectionViewModel.roadType.collectAsState()
    val wilayahState by detectionViewModel.wilayahState.collectAsState()

    // State Lokal
    var displayedAddress by remember { mutableStateOf("") }
    var isGeocoding by remember { mutableStateOf(false) }

    // State Validasi Lokasi
    var isPinOutsideKendari by remember { mutableStateOf(false) }
    var isValidRoad by remember { mutableStateOf(false) }
    var detectedDbJalan by remember { mutableStateOf<WilayahItem?>(null) }
    var matchedByPolyline by remember { mutableStateOf(false) }
    var isStrictValidationActive by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-3.99, 122.51), 12f)
    }

    // Theming
    val isDarkTheme = isSystemInDarkTheme()
    val bottomSurfaceColor = if (isDarkTheme) Color(0xFF1F1F1F) else Color.White
    val fabContainerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray

    val mapProperties = remember(isDarkTheme) {
        val styleJson = if (isDarkTheme) R.raw.map_style_dark else R.raw.map_style_light
        MapProperties(mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, styleJson))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) moveToCurrentLocation(context, fusedLocationProviderClient, cameraPositionState, coroutineScope)
    }

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            moveToCurrentLocation(context, fusedLocationProviderClient, cameraPositionState, coroutineScope)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // --- LOGIKA UTAMA: Cek Lokasi Pin vs Data Polyline ---
    LaunchedEffect(cameraPositionState.isMoving, wilayahState) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target

            // Reset status validasi
            isValidRoad = false
            detectedDbJalan = null
            matchedByPolyline = false
            isStrictValidationActive = false

            // 1. Cek Data Polyline dari Database
            if (wilayahState is UiState.Success) {
                val listJalan = (wilayahState as UiState.Success).data
                if (listJalan.isNotEmpty()) {
                    isStrictValidationActive = true // Aktifkan mode validasi ketat

                    for (jalan in listJalan) {
                        // GUNAKAN HELPER JSON PARSER DI SINI
                        val path = parseGeoJsonPolyline(jalan.polylineString)

                        if (path.isNotEmpty()) {
                            // Cek apakah pin menempel di garis dengan toleransi 25 meter
                            if (PolyUtil.isLocationOnPath(target, path, true, 25.0)) {
                                isValidRoad = true
                                detectedDbJalan = jalan
                                matchedByPolyline = true
                                break
                            }
                        }
                    }
                }
            }

            // 2. Ambil alamat dari Google (Geocoding)
            isGeocoding = true
            val googleAddressDetail = withContext(Dispatchers.IO) {
                getAddressDetail(context, target.latitude, target.longitude)
            }
            isGeocoding = false

            // 3. Tentukan Nama Jalan & Status
            if (matchedByPolyline && detectedDbJalan != null) {
                val dbName = detectedDbJalan!!.namaJalan
                val district = googleAddressDetail.district ?: detectedDbJalan!!.kecamatan
                val city = googleAddressDetail.city ?: detectedDbJalan!!.kota
                displayedAddress = "$dbName, $district, $city"
                isPinOutsideKendari = false
            } else {
                displayedAddress = googleAddressDetail.fullAddress
                // Contoh validasi wilayah manual (bisa disesuaikan)
                isPinOutsideKendari = !googleAddressDetail.fullAddress.contains("Kendari", ignoreCase = true)
            }

            // Jika API mati/kosong, anggap valid (Mode Bebas)
            if (!isStrictValidationActive) {
                isValidRoad = true
            }

            detectionViewModel.fetchRoadTypeAtLocation(target)
            detectionViewModel.setLocation(target, displayedAddress)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Lokasi Aduan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        detectionViewModel.fetchWilayahData()
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            moveToCurrentLocation(context, fusedLocationProviderClient, cameraPositionState, coroutineScope)
                        }
                        Toast.makeText(context, "Memuat ulang data...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(color = bottomSurfaceColor, tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Indikator Status & Alamat
                    Row(verticalAlignment = Alignment.Top) {
                        val iconTint = if (isPinOutsideKendari) MaterialTheme.colorScheme.error
                        else if (matchedByPolyline) Color(0xFF4CAF50) // Hijau
                        else if (isStrictValidationActive && !isValidRoad) Color(0xFFFFA000) // Kuning/Orange
                        else PrimaryColor

                        Icon(Icons.Filled.LocationOn, "Ikon Lokasi", tint = iconTint, modifier = Modifier.padding(end = 12.dp))
                        Column {
                            Text("Lokasi Dipilih", style = MaterialTheme.typography.labelMedium, color = textColorSecondary)

                            val addressText = if (isGeocoding) "Mencari alamat..." else if (displayedAddress.isNotEmpty()) displayedAddress else "Alamat tidak ditemukan"
                            Text(addressText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = textColorPrimary)

                            if (matchedByPolyline) {
                                Text("✓ Terverifikasi di Jalur: ${detectedDbJalan?.namaJalan}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            } else if (isStrictValidationActive && !isValidRoad && !isPinOutsideKendari) {
                                Text("⚠ Di luar jalur wilayah kewenangan", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFA000))
                            } else if (!isStrictValidationActive) {
                                Text("⚠ Mode Bebas (Data Wilayah Tidak Tersedia)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Info Tipe Jalan
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp)) {
                        Icon(Icons.Default.Signpost, contentDescription = "Tipe Jalan", modifier = Modifier.size(18.dp), tint = textColorSecondary)
                        Spacer(Modifier.width(8.dp))
                        val roadTypeText = when {
                            isCheckingRoad -> "Mengecek tipe jalan..."
                            roadType != null -> roadType ?: "Tipe jalan tidak terdeteksi"
                            else -> "Tipe jalan tidak terdeteksi"
                        }
                        Text(roadTypeText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = textColorPrimary.copy(alpha = 0.8f))
                    }

                    if (isPinOutsideKendari) {
                        Spacer(Modifier.height(8.dp))
                        Text("Lokasi harus berada di dalam wilayah Kota Kendari.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 40.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navController.navigate("deskripsi_screen") },
                        enabled = !isPinOutsideKendari && (!isStrictValidationActive || isValidRoad)
                    ) {
                        Text("Deskripsi")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState, properties = mapProperties) {
                // Render Garis Biru
                if (wilayahState is UiState.Success) {
                    val listJalan = (wilayahState as UiState.Success).data
                    listJalan.forEach { jalan ->
                        // GUNAKAN HELPER JSON PARSER UTK MENGGAMBAR
                        val points = parseGeoJsonPolyline(jalan.polylineString)
                        if (points.isNotEmpty()) {
                            Polyline(
                                points = points,
                                color = Color(0xFF2196F3),
                                width = 15f,
                                zIndex = 1f,
                                onClick = { Toast.makeText(context, "Jalan: ${jalan.namaJalan}", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
            }

            // Pin Tengah
            Box(modifier = Modifier.align(Alignment.Center)) {
                val pinTint = if (isPinOutsideKendari) MaterialTheme.colorScheme.error
                else if (matchedByPolyline) Color(0xFF4CAF50)
                else if (isStrictValidationActive && !isValidRoad) Color(0xFFFFA000)
                else PrimaryColor
                Icon(if (isPinOutsideKendari) Icons.Default.Block else Icons.Default.Place, "Pin", tint = pinTint, modifier = Modifier.size(48.dp).offset(y = (-24).dp))
            }

            // Tombol Lokasi Saya
            FloatingActionButton(
                onClick = {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        moveToCurrentLocation(context, fusedLocationProviderClient, cameraPositionState, coroutineScope)
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                containerColor = fabContainerColor, contentColor = PrimaryColor,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            ) { Icon(Icons.Default.MyLocation, "Lokasi Saya") }
        }
    }
}

// --- FUNGSI HELPER LAINNYA ---
suspend fun getAddressDetail(context: Context, lat: Double, lng: Double): AddressDetail {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val list: MutableList<Address>? = withContext(Dispatchers.IO) {
            geocoder.getFromLocation(lat, lng, 1)
        }
        if (!list.isNullOrEmpty()) {
            val address = list[0]
            val fullAddress = address.getAddressLine(0)
            val plusCodeRegex = "^[A-Z0-9]{4}\\+[A-Z0-9]{2,},?\\s?".toRegex()
            val cleanFullAddress = fullAddress.replace(plusCodeRegex, "").trim()
            AddressDetail(
                fullAddress = cleanFullAddress, street = address.thoroughfare, district = address.subLocality,
                city = address.locality ?: address.subAdminArea, province = address.adminArea
            )
        } else { AddressDetail("Wilayah tidak terdeteksi", null, null, null, null) }
    } catch (e: Exception) { AddressDetail("Gagal mendapatkan alamat", null, null, null, null) }
}

private fun moveToCurrentLocation(context: Context, fusedLocationProviderClient: FusedLocationProviderClient, cameraPositionState: CameraPositionState, coroutineScope: CoroutineScope) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
    fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
        location?.let {
            coroutineScope.launch {
                cameraPositionState.animate(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 17f), durationMs = 1000)
            }
        }
    }
}