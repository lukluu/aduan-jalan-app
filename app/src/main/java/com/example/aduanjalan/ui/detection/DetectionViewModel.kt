package com.example.aduanjalan.ui.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduanjalan.data.common.UiState
import com.example.aduanjalan.data.remote.api.OverpassApiService
import com.example.aduanjalan.data.remote.request.DetectionRequest
import com.example.aduanjalan.data.remote.response.WilayahItem
import com.example.aduanjalan.data.repository.CreateReportRepository
import com.example.aduanjalan.data.repository.WilayahRepository
import com.example.aduanjalan.domain.model.Criteria
import com.example.aduanjalan.domain.model.Detection
import com.example.aduanjalan.ui.utils.TFLiteHelper
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class DetectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CreateReportRepository,
    private val overpassApiService: OverpassApiService,
    private val wilayahRepository: WilayahRepository
) : ViewModel() {
    private val TAG = "DetectionViewModel"

    // =================================================================================
    // --- BAGIAN BARU: Data Wilayah & Validasi JSON Polyline ---
    // =================================================================================

    private val _wilayahState = MutableStateFlow<UiState<List<WilayahItem>>>(UiState.Loading)
    val wilayahState: StateFlow<UiState<List<WilayahItem>>> = _wilayahState

    init {
        fetchWilayahData()
        // 🔥 sync awal ke TFLite
    }

    // ================= TAMBAHAN BARU =================



    // 🔥 STATE UNTUK SLIDER
    private val _confidenceThreshold = MutableStateFlow(0.2f)
    val confidenceThreshold: StateFlow<Float> = _confidenceThreshold

    private val _iouThreshold = MutableStateFlow(0.5f)
    val iouThreshold: StateFlow<Float> = _iouThreshold

    // 🔥 SETTER (DIHUBUNGKAN KE TFLiteHelper)
    fun setConfidenceThreshold(value: Float) {
        _confidenceThreshold.value = value
        TFLiteHelper.confThreshold = value
    }

    fun setIouThreshold(value: Float) {
        _iouThreshold.value = value
        TFLiteHelper.iouThreshold = value
    }
    fun fetchWilayahData() {
        viewModelScope.launch {
            wilayahRepository.getWilayahs().collect { state ->
                if (state is UiState.Success) {
                    // Filter data agar aplikasi tidak crash saat render map
                    val validData = filterValidRoads(state.data)
                    _wilayahState.value = UiState.Success(validData)
                } else {
                    _wilayahState.value = state
                }
            }
        }
    }

    /**
     * Validasi Polyline: Memastikan string adalah JSON Array valid (Format: [[lon,lat],...])
     * Bukan format encoded string Google.
     */
    private fun filterValidRoads(rawList: List<WilayahItem>): List<WilayahItem> {
        val safeList = mutableListOf<WilayahItem>()

        for (item in rawList) {
            // 1. Cek String Kosong
            if (item.polylineString.isNullOrEmpty() || item.polylineString.startsWith("ERROR")) {
                continue
            }

            // 2. Cek Validitas JSON Array
            val isPolylineSafe = try {
                val jsonArray = JSONArray(item.polylineString)
                jsonArray.length() > 0 // Valid jika array tidak kosong
            } catch (e: Exception) {
                Log.e(TAG, "Skip jalan '${item.namaJalan}': Format JSON salah. Error: ${e.message}")
                false
            }

            if (!isPolylineSafe) continue

            // 3. Validasi Koordinat Titik Tengah/Awal (Safety check)
            if (!isValidCoordinate(item.latitudeAwal) || !isValidCoordinate(item.longitudeAwal)) {
                continue
            }

            safeList.add(item)
        }
        return safeList
    }

    private fun isValidCoordinate(coord: String?): Boolean {
        if (coord.isNullOrEmpty()) return false
        return try {
            val doubleVal = coord.replace(",", ".").toDouble()
            doubleVal > -200 && doubleVal < 200
        } catch (e: NumberFormatException) {
            false
        }
    }

    // =================================================================================
    // --- BAGIAN LAMA (Overpass, TFLite, Report Submission) ---
    // =================================================================================

    companion object {
        private const val SEARCH_RADIUS_METERS = 15.0
        private val roadTypeTranslationMap = mapOf(
            "motorway" to "Jalan Tol", "trunk" to "Jalan Lintas Nasional",
            "primary" to "Jalan Arteri Primer", "secondary" to "Jalan Arteri Sekunder",
            "tertiary" to "Jalan Kolektor", "unclassified" to "Jalan Lokal",
            "residential" to "Jalan Perumahan", "living_street" to "Jalan Lingkungan",
            "service" to "Jalan Akses", "track" to "Jalan Tanah",
            "footway" to "Jalur Pejalan Kaki", "cycleway" to "Jalur Sepeda", "path" to "Jalur Setapak"
        )
    }

    private val _roadType = MutableStateFlow<String?>(null)
    val roadType: StateFlow<String?> = _roadType
    private val _isCheckingRoad = MutableStateFlow(false)
    val isCheckingRoad: StateFlow<Boolean> = _isCheckingRoad
    private var roadCheckJob: Job? = null

    fun fetchRoadTypeAtLocation(latLng: LatLng) {
        roadCheckJob?.cancel()
        roadCheckJob = viewModelScope.launch {
            delay(100)
            _isCheckingRoad.value = true
            _roadType.value = null
            try {
                val query = buildOverpassQuery(latLng.latitude, latLng.longitude)
                val response = withContext(Dispatchers.IO) { overpassApiService.getRoadData(query) }
                if (response.isSuccessful && response.body() != null) {
                    val foundRoadTypeEnglish = response.body()!!.elements.firstNotNullOfOrNull { it.tags?.highway }
                    _roadType.value = translateRoadType(foundRoadTypeEnglish)
                }
            } catch (e: Exception) { Log.e(TAG, "Failed Overpass", e) }
            finally { _isCheckingRoad.value = false }
        }
    }

    private fun buildOverpassQuery(lat: Double, lon: Double): String {
        return "[out:json][timeout:25];way(around:$SEARCH_RADIUS_METERS,$lat,$lon)[highway];out tags;"
    }

    private fun translateRoadType(englishType: String?): String? {
        if (englishType == null) return null
        return roadTypeTranslationMap[englishType] ?: englishType.replaceFirstChar { it.uppercase() }
    }

    // --- TFLite Detection Logic ---
    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections: StateFlow<List<Detection>> = _detections
    private val _latestDetections = MutableStateFlow<List<Detection>>(emptyList())
    val latestDetections: StateFlow<List<Detection>> = _latestDetections
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _frameSize = MutableStateFlow<Pair<Int, Int>?>(null)
    val frameSize: StateFlow<Pair<Int, Int>?> = _frameSize
    fun setFrameSize(w: Int, h: Int) { _frameSize.value = w to h }

    var currentBitmap: Bitmap? = null
        private set
    private val _latestProcessedBitmap = MutableStateFlow<Bitmap?>(null)
    val latestProcessedBitmap: StateFlow<Bitmap?> = _latestProcessedBitmap

    fun setDetections(detections: List<Detection>) {
        _detections.value = detections
        _latestDetections.value = detections
    }

    private val processing = AtomicBoolean(false)

    fun setCurrentBitmapOnly(bitmap: Bitmap) {
        currentBitmap = bitmap
        _latestProcessedBitmap.value = bitmap
    }

    fun setBitmapAndDetect(bitmap: Bitmap) {
        try { _latestProcessedBitmap.value = bitmap } catch (_: Exception) { }
        currentBitmap = bitmap
        detect(bitmap)
    }

    fun detect(bitmap: Bitmap) {
        if (!processing.compareAndSet(false, true)) return
        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            try {
                val results = TFLiteHelper.detectBitmap(context, bitmap)
                _detections.value = results
                _latestDetections.value = results
            } catch (e: Exception) { Log.e(TAG, "Detection exception", e) }
            finally { _isLoading.value = false; processing.set(false) }
        }
    }

    // --- Reset & Submission Logic ---
    fun clearDetection() {
        _detections.value = emptyList()
        _latestDetections.value = emptyList()
        _latestProcessedBitmap.value = null
        currentBitmap = null
        processing.set(false)
    }

    fun resetAll() {
        clearDetection()
        _location.value = null
        _address.value = ""
        _roadType.value = null
        _criterias.value = emptyList()
    }

    fun stopRealtimeDetection() { clearDetection() }

    fun prepareForNewReport() {
        if (currentBitmap != null || latestProcessedBitmap.value != null) { resetAll() }
    }

    private val _location = MutableStateFlow<LatLng?>(null)
    val location: StateFlow<LatLng?> = _location
    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address
    fun setLocation(latLng: LatLng?, addr: String) {
        _location.value = latLng
        _address.value = addr
    }

    private val _criterias = MutableStateFlow<List<Criteria>>(emptyList())
    val criterias: StateFlow<List<Criteria>> = _criterias
    fun fetchCriterias() {
        viewModelScope.launch {
            try { _criterias.value = repository.getCriterias() }
            catch (e: Exception) { Log.e("DetectionViewModel", "Failed criterias", e) }
        }
    }

    suspend fun submitReport(
        token: String,
        imageFile: File,
        address: String,
        road: String,
        lat: Double,
        lon: Double,
        detections: List<Detection>,
        answers: Map<Int, Int>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val detectionRequests = detections.map {
                    DetectionRequest(
                        it.label,
                        it.confidence.toDouble(),
                        it.bbox_x.toDouble(),
                        it.bbox_y.toDouble(),
                        it.bbox_width.toDouble(),
                        it.bbox_height.toDouble()
                    )
                }
                val answersList = answers.map { mapOf(it.key.toString() to it.value) }

                // --- PERBAIKAN DI SINI (URUTAN PARAMETER DITUKAR) ---
                // Sebelumnya: (..., lat, lon, road, ...) -> Salah urutan tipe data
                // Sekarang:   (..., road, lat, lon, ...) -> Sesuai tipe data (String, Double, Double)

                val response = repository.submitReport(
                    token,
                    imageFile,
                    address,
                    road, // String (Road Type) dipindah ke sini
                    lat,  // Double (Latitude) dipindah ke sini
                    lon,  // Double (Longitude) dipindah ke sini
                    detectionRequests,
                    answersList
                )

                true
            } catch (e: Exception) {
                Log.e(TAG, "Submit report error", e)
                false
            }
        }
    }
}