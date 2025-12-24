package com.example.aduanjalan.ui.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduanjalan.data.remote.api.OverpassApiService
import com.example.aduanjalan.data.remote.request.DetectionRequest
import com.example.aduanjalan.data.repository.CreateReportRepository
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
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class DetectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CreateReportRepository,
    private val overpassApiService: OverpassApiService
) : ViewModel() {
    private val TAG = "DetectionViewModel"

    companion object {
        // --- PERUBAHAN: Radius pencarian diperluas untuk akurasi yang lebih baik ---
        private const val SEARCH_RADIUS_METERS = 15.0

        private val roadTypeTranslationMap = mapOf(
            "motorway" to "Jalan Tol",
            "trunk" to "Jalan Lintas Nasional",
            "primary" to "Jalan Arteri Primer",
            "secondary" to "Jalan Arteri Sekunder",
            "tertiary" to "Jalan Kolektor",
            "unclassified" to "Jalan Lokal",
            "residential" to "Jalan Perumahan / Permukiman",
            "living_street" to "Jalan Lingkungan Tenang",
            "service" to "Jalan Servis / Akses",
            "track" to "Jalan Tanah / Kebun",
            "footway" to "Jalur Pejalan Kaki",
            "cycleway" to "Jalur Sepeda",
            "path" to "Jalur Setapak"
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
            // --- PERUBAHAN: Delay dikurangi agar pencarian terasa lebih cepat ---
            delay(100)

            _isCheckingRoad.value = true
            _roadType.value = null

            try {
                val query = buildOverpassQuery(latLng.latitude, latLng.longitude)
                val response = withContext(Dispatchers.IO) {
                    overpassApiService.getRoadData(query)
                }

                if (response.isSuccessful && response.body() != null) {
                    val foundRoadTypeEnglish = response.body()!!.elements
                        .firstNotNullOfOrNull { it.tags?.highway }

                    _roadType.value = translateRoadType(foundRoadTypeEnglish)

                    Log.d(TAG, "Overpass check success. English type: $foundRoadTypeEnglish, Translated: ${_roadType.value}")
                } else {
                    Log.e(TAG, "Overpass API Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to call Overpass API", e)
            } finally {
                _isCheckingRoad.value = false
            }
        }
    }

    private fun buildOverpassQuery(lat: Double, lon: Double): String {
        return "[out:json][timeout:25];" +
                "way(around:$SEARCH_RADIUS_METERS,$lat,$lon)[highway];" +
                "out tags;"
    }

    private fun translateRoadType(englishType: String?): String? {
        if (englishType == null) return null
        return roadTypeTranslationMap[englishType] ?: englishType.replaceFirstChar { it.uppercase() }
    }

    // =================================================================================
    // --- BAGIAN LAMA: Logika Deteksi Gambar, Lokasi, dan Laporan (Tidak berubah) ---
    // =================================================================================

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
        try {
            _latestProcessedBitmap.value = bitmap
        } catch (_: Exception) { /* ignore */ }

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
                Log.d(TAG, "Detections: ${results.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Detection exception", e)
            } finally {
                _isLoading.value = false
                processing.set(false)
            }
        }
    }

    fun clearDetection() {
        _detections.value = emptyList()
        _latestDetections.value = emptyList()
        _latestProcessedBitmap.value = null
        currentBitmap = null
        processing.set(false)
    }

    /**
     * Diperbarui untuk membersihkan state tipe jalan juga.
     */
    fun resetAll() {
        Log.d(TAG, "Resetting all report creation states.")
        clearDetection()
        _location.value = null
        _address.value = ""
        _roadType.value = null
        _criterias.value = emptyList()
    }

    fun stopRealtimeDetection() {
        clearDetection()
    }

    fun prepareForNewReport() {
        if (currentBitmap != null || latestProcessedBitmap.value != null) {
            Log.d("ViewModelLifecycle", "Stale data detected. Resetting for new report.")
            resetAll()
        }
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
            try {
                _criterias.value = repository.getCriterias()
            } catch (e: Exception) {
                Log.e("DetectionViewModel", "Failed to fetch criterias", e)
            }
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
                        label = it.label,
                        confidence = it.confidence.toDouble(),
                        bbox_x = it.bbox_x.toDouble(),
                        bbox_y = it.bbox_y.toDouble(),
                        bbox_width = it.bbox_width.toDouble(),
                        bbox_height = it.bbox_height.toDouble()
                    )
                }

                val answersList = answers.map { mapOf(it.key.toString() to it.value) }

                val response = repository.submitReport(
                    token = token,
                    imageFile = imageFile,
                    address = address,
                    lat = lat,
                    lon = lon,
                    roadType = road,
                    detections = detectionRequests,
                    answers = answersList
                )

                Log.d("SubmitReport", "Success: $response")
                true
            } catch (e: Exception) {
                Log.e("SubmitReport", "Error: ${e.message}", e)
                false
            }
        }
    }
}