package com.example.aduanjalan.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduanjalan.data.DataStoreManager
import com.example.aduanjalan.data.remote.response.AllReportResponse
import com.example.aduanjalan.data.repository.AllReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val reportsRepository: AllReportsRepository
) : ViewModel() {

    private val _userName = MutableStateFlow("Pengguna")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _latestReports = MutableStateFlow<List<AllReportResponse>>(emptyList())
    val latestReports: StateFlow<List<AllReportResponse>> = _latestReports.asStateFlow()

    // State loading untuk Skeleton (Load Awal)
    private val _isLoadingReports = MutableStateFlow(false)
    val isLoadingReports: StateFlow<Boolean> = _isLoadingReports.asStateFlow()

    // State loading untuk Refresh (Spinner di atas)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _reportError = MutableStateFlow<String?>(null)
    val reportError: StateFlow<String?> = _reportError.asStateFlow()

    private var isDataLoaded = false

    init {
        getUserNameFromToken()
        loadLatestReports()
    }

    private fun getUserNameFromToken() {
        viewModelScope.launch {
            dataStoreManager.getUserName().collect { name ->
                _userName.value = name
            }
        }
    }

    // Fungsi Public untuk dipanggil dari UI saat ditarik
    fun refreshHome() {
        getUserNameFromToken() // Refresh nama user juga
        loadLatestReports(forceUpdate = true)
    }

    private fun loadLatestReports(forceUpdate: Boolean = false) {
        // 1. Jika sedang loading (baik awal atau refresh), stop.
        if (_isLoadingReports.value || _isRefreshing.value) return

        // 2. Logic "One Time Fetch":
        // Jika TIDAK dipaksa (forceUpdate = false) DAN data sudah pernah diload, stop.
        if (!forceUpdate && isDataLoaded && _latestReports.value.isNotEmpty()) return

        viewModelScope.launch {
            // Tentukan state loading mana yang aktif
            if (forceUpdate) {
                _isRefreshing.value = true // Spinner Refresh
            } else {
                _isLoadingReports.value = true // Skeleton Awal
            }

            try {
                val token = dataStoreManager.getToken().first()
                if (token.isNotBlank()) {
                    val allReports = reportsRepository.getAllReports(token) ?: emptyList()

                    _latestReports.value = allReports
                        .sortedByDescending { it.created_at }
                        .take(3)

                    isDataLoaded = true
                }
            } catch (e: Exception) {
                _reportError.value = "Gagal memuat data"
            } finally {
                // Matikan kedua indikator loading
                if (forceUpdate) {
                    _isRefreshing.value = false
                } else {
                    delay(500) // Delay sedikit untuk skeleton biar smooth
                    _isLoadingReports.value = false
                }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            dataStoreManager.clear()
            onLoggedOut()
        }
    }
}