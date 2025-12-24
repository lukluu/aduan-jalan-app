package com.example.aduanjalan.ui.report

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
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
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.data.kml.KmlLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.Locale

// Konstanta untuk jarak toleransi dari jalan KML (dalam meter)
private const val ROAD_PROXIMITY_THRESHOLD_METERS = 15.0

@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    detectionViewModel: DetectionViewModel = hiltViewModel()
) {
    // --- Inisialisasi State & Context ---
    val context = LocalContext.current
    val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    val coroutineScope = rememberCoroutineScope()

    // --- Mengamati State dari ViewModel ---
    val isCheckingRoad by detectionViewModel.isCheckingRoad.collectAsState()
    val roadType by detectionViewModel.roadType.collectAsState()

    // --- State lokal untuk UI dan logika validasi ---
    var address by remember { mutableStateOf("") }
    var isGeocoding by remember { mutableStateOf(false) }
    var kmlRoads by remember { mutableStateOf<List<List<LatLng>>>(emptyList()) }
    var isPinOnForbiddenRoad by remember { mutableStateOf(false) }
    var isPinOutsideKendari by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-3.99, 122.51), 12f)
    }

    // --- UI Theming, Properties, dan Permission Launcher ---
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
    ) { granted -> if (granted) { moveToCurrentLocation(context, fusedLocationProviderClient, cameraPositionState, coroutineScope) } }

    // --- Memuat data KML ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            kmlRoads = parseKmlLineStrings(context.resources.openRawResource(R.raw.kmljalankendari))
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            moveToCurrentLocation(context, fusedLocationProviderClient, cameraPositionState, coroutineScope)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // --- Logika utama yang berjalan setiap kali peta berhenti bergerak ---
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target

            // 1. Dapatkan alamat terlebih dahulu
            isGeocoding = true
            val currentAddress = withContext(Dispatchers.IO) { getAddressSafe(context, target.latitude, target.longitude) }
            address = currentAddress
            isGeocoding = false

            // 2. Lakukan validasi KML
            isPinOnForbiddenRoad = isLocationNearAnyKmlPath(
                point = target,
                paths = kmlRoads,
                toleranceMeters = ROAD_PROXIMITY_THRESHOLD_METERS
            )
            Log.d("MapScreen", "KML Check: Is pin on forbidden road: $isPinOnForbiddenRoad")

            // 3. Lakukan validasi batas wilayah berdasarkan string alamat
            isPinOutsideKendari = !currentAddress.contains("Kendari", ignoreCase = true)
            Log.d("MapScreen", "Address Check: Address is '$currentAddress', Is pin outside Kendari: $isPinOutsideKendari")

            // 4. Panggil ViewModel untuk mengambil tipe jalan
            detectionViewModel.fetchRoadTypeAtLocation(target)

            // 5. Simpan lokasi dan alamat ke ViewModel
            detectionViewModel.setLocation(target, currentAddress)
        }
    }

    // --- Struktur UI dengan Scaffold ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Langkah 2/3: Tentukan Lokasi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Ikon Lokasi",
                            tint = if (isPinOnForbiddenRoad || isPinOutsideKendari) MaterialTheme.colorScheme.error else PrimaryColor,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text("Lokasi Dipilih", style = MaterialTheme.typography.labelMedium, color = textColorSecondary)
                            val addressText = when {
                                isGeocoding -> "Mencari alamat..."
                                address.isNotEmpty() -> address
                                else -> "Alamat tidak ditemukan"
                            }
                            Text(addressText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = textColorPrimary)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp)) {
                        Icon(Icons.Default.Signpost, contentDescription = "Tipe Jalan", modifier = Modifier.size(18.dp), tint = textColorSecondary)
                        Spacer(Modifier.width(8.dp))
                        val roadTypeText = when {
                            isCheckingRoad -> "Mengecek tipe jalan..."
                            // Jika roadType tidak null, gunakan nilainya. Jika null, gunakan string default.
                            // Dengan cara ini, roadTypeText dijamin selalu String, bukan String?
                            roadType != null -> roadType ?: "Tipe jalan tidak terdeteksi"
                            else -> "Tipe jalan tidak terdeteksi"
                        }
                        Text(
                            text = roadTypeText, // Sekarang aman, tidak akan error
                            style = MaterialTheme.typography.bodyMedium, // Font lebih besar
                            fontWeight = FontWeight.SemiBold, // Sedikit tebal
                            color = textColorPrimary.copy(alpha = 0.8f)
                        )

                    }

                    if (isPinOnForbiddenRoad) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Lokasi tidak dapat dipilih karena berada di atas jalan utama.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 40.dp)
                        )
                    }

                    if (isPinOutsideKendari) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Lokasi harus berada di dalam wilayah Kota Kendari.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 40.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navController.navigate("deskripsi_screen") },
                        enabled = !isPinOnForbiddenRoad && !isPinOutsideKendari
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
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties
            ) {
                MapEffect(Unit) { map ->
                    try {
                        val layer = KmlLayer(map, R.raw.kmljalankendari, context)
                        layer.addLayerToMap()
                    } catch (e: Exception) {
                        Log.e("MapScreen", "Failed to render KML layer: ${e.message}")
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.Center)) {
                Icon(
                    imageVector = if (isPinOnForbiddenRoad || isPinOutsideKendari) Icons.Default.Block else Icons.Default.Place,
                    contentDescription = "Pin Lokasi",
                    tint = if (isPinOnForbiddenRoad || isPinOutsideKendari) MaterialTheme.colorScheme.error else PrimaryColor,
                    modifier = Modifier.size(48.dp).offset(y = (-24).dp)
                )
            }

            FloatingActionButton(
                onClick = {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        moveToCurrentLocation(context, fusedLocationProviderClient, cameraPositionState, coroutineScope)
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                containerColor = fabContainerColor,
                contentColor = PrimaryColor,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Lokasi Saya")
            }
        }
    }
}

// =================================================================================
// --- FUNGSI-FUNGSI HELPER (Tidak Berubah & Wajib Ada) ---
// =================================================================================

private fun moveToCurrentLocation(
    context: android.content.Context,
    fusedLocationProviderClient: FusedLocationProviderClient,
    cameraPositionState: CameraPositionState,
    coroutineScope: CoroutineScope
) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
    fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            location?.let {
                coroutineScope.launch {
                    cameraPositionState.animate(
                        com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 17f),
                        durationMs = 1000
                    )
                }
            }
        }
}

suspend fun getAddressSafe(context: android.content.Context, lat: Double, lng: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val list = withContext(Dispatchers.IO) { geocoder.getFromLocation(lat, lng, 1) }
        val fullAddress = list?.firstOrNull()?.getAddressLine(0) ?: "Wilayah tidak terdeteksi"
        val plusCodeRegex = "^[A-Z0-9]{4}\\+[A-Z0-9]{2,},?\\s?".toRegex()
        fullAddress.replace(plusCodeRegex, "").trim()
    } catch (e: Exception) {
        e.printStackTrace()
        "Gagal mendapatkan alamat"
    }
}

fun isLocationNearAnyKmlPath(point: LatLng, paths: List<List<LatLng>>, toleranceMeters: Double): Boolean {
    if (paths.isEmpty()) return false
    for (path in paths) {
        if (PolyUtil.isLocationOnPath(point, path, true, toleranceMeters)) {
            return true
        }
    }
    return false
}

fun parseKmlLineStrings(inputStream: InputStream): List<List<LatLng>> {
    val paths = mutableListOf<List<LatLng>>()
    try {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var isInsideCoordinates = false
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("coordinates", ignoreCase = true)) {
                        isInsideCoordinates = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (isInsideCoordinates) {
                        val coordinatesText = parser.text.trim()
                        if (coordinatesText.isNotEmpty()) {
                            val path = coordinatesText.split(" ")
                                .mapNotNull { coordString ->
                                    val parts = coordString.split(",")
                                    if (parts.size >= 2) {
                                        try {
                                            LatLng(parts[1].toDouble(), parts[0].toDouble())
                                        } catch (e: NumberFormatException) { null }
                                    } else { null }
                                }
                            paths.add(path)
                        }
                        isInsideCoordinates = false
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("KmlParser", "Error parsing KML file: ${e.message}")
    } finally {
        inputStream.close()
    }
    return paths
}