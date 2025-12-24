package com.example.aduanjalan.ui.allreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduanjalan.data.DataStoreManager
import com.example.aduanjalan.data.remote.response.AllReportResponse
import com.example.aduanjalan.data.repository.AllReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllReportsViewModel @Inject constructor(
    private val repo: AllReportsRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _reports = MutableStateFlow<List<AllReportResponse>>(emptyList())
    val reports: StateFlow<List<AllReportResponse>> = _reports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _voteMessage = MutableSharedFlow<String>()
    val voteMessage: SharedFlow<String> = _voteMessage.asSharedFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = dataStore.getToken().first()
                if (token.isNotBlank()) {
                    val data = repo.getAllReports(token) ?: emptyList()
                    _reports.value = data
                    _error.value = null
                } else {
                    _error.value = "Token tidak ditemukan. Silakan login."
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Gagal memuat laporan"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshReports() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                val token = dataStore.getToken().first()
                if (token.isNotBlank()) {
                    val data = repo.getAllReports(token) ?: emptyList()
                    _reports.value = data
                    _error.value = null
                } else {
                    _error.value = "Token tidak ditemukan. Silakan login."
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Gagal memuat laporan"
            } finally {
                _refreshing.value = false
            }
        }
    }

    // ✅ FUNGSI YANG DIPERBAIKI
    fun voteReport(reportId: Int) {
        viewModelScope.launch {
            try {
                val token = dataStore.getToken().first()
                if (token.isBlank()) {
                    _voteMessage.emit("Token tidak ditemukan. Silakan login.")
                    return@launch
                }

                // Langkah 1: Panggil API untuk vote
                val (success, message) = repo.voteReport(token, reportId)

                if (success) {
                    // Kirim pesan sukses ke UI
                    _voteMessage.emit(message)

                    // ✅ LANGKAH 2 (KRUSIAL): Ambil kembali semua data laporan
                    // Ini akan mendapatkan skor dan peringkat yang sudah diperbarui
                    // Kita tidak menampilkan loading indicator agar terlihat seamless
                    val updatedData = repo.getAllReports(token) ?: _reports.value
                    _reports.value = updatedData
                } else {
                    // Jika vote gagal, kirim pesan error dari server
                    _voteMessage.emit(message)
                }
            } catch (e: Exception) {
                _voteMessage.emit(e.localizedMessage ?: "Gagal melakukan vote")
            }
        }
    }

    val filteredReports = combine(_reports, _searchQuery) { reports, query ->
        if (query.isBlank()) reports
        else reports.filter {
            it.location_address?.contains(query, ignoreCase = true) == true ||
                    it.status?.contains(query, ignoreCase = true) == true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}