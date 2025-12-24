package com.example.aduanjalan.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduanjalan.data.DataStoreManager
import com.example.aduanjalan.data.remote.response.AllReportResponse
import com.example.aduanjalan.data.repository.AllReportsRepository // Import repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val reportsRepository: AllReportsRepository // Suntikkan repository aduan
) : ViewModel() {

    private val _userName = MutableStateFlow("Pengguna")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // --- STATE BARU UNTUK CAROUSEL ---
    private val _latestReports = MutableStateFlow<List<AllReportResponse>>(emptyList())
    val latestReports: StateFlow<List<AllReportResponse>> = _latestReports.asStateFlow()

    private val _isLoadingReports = MutableStateFlow(false)
    val isLoadingReports: StateFlow<Boolean> = _isLoadingReports.asStateFlow()

    private val _reportError = MutableStateFlow<String?>(null)
    val reportError: StateFlow<String?> = _reportError.asStateFlow()
    // --- AKHIR STATE BARU ---

    init {
        getUserNameFromToken()
        loadLatestReports() // Panggil fungsi untuk memuat data carousel
    }

    private fun getUserNameFromToken() {
        viewModelScope.launch {
            dataStoreManager.getUserName().collect { name ->
                _userName.value = name
            }
        }
    }

    // --- FUNGSI BARU UNTUK MENGAMBIL 3 ADUAN TERBARU ---
    private fun loadLatestReports() {
        viewModelScope.launch {
            _isLoadingReports.value = true
            try {
                val token = dataStoreManager.getToken().first()
                if (token.isNotBlank()) {
                    val allReports = reportsRepository.getAllReports(token) ?: emptyList()
                    // Urutkan berdasarkan tanggal terbaru dan ambil 3 teratas
                    _latestReports.value = allReports
                        .sortedByDescending { it.created_at }
                        .take(3)
                    _reportError.value = null
                } else {
                    _reportError.value = "Token tidak valid."
                }
            } catch (e: Exception) {
                _reportError.value = "Gagal memuat aduan terbaru."
            } finally {
                _isLoadingReports.value = false
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