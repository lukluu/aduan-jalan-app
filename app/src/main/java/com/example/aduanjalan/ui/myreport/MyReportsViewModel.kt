package com.example.aduanjalan.ui.myreport

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduanjalan.data.DataStoreManager
import com.example.aduanjalan.data.remote.response.MyReportResponse
import com.example.aduanjalan.data.repository.MyReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyReportsViewModel @Inject constructor(
    private val repository: MyReportsRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _reports = MutableStateFlow<List<MyReportResponse>>(emptyList())
    val reports: StateFlow<List<MyReportResponse>> = _reports

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _deleteStatus = MutableStateFlow<Pair<Boolean, String>?>(null)
    val deleteStatus: StateFlow<Pair<Boolean, String>?> = _deleteStatus

    init {
        fetchMyReports()
    }

    fun fetchMyReports() {
        viewModelScope.launch {
            dataStore.getToken().collect { token ->
                if (token.isNotEmpty()) {
                    try {
                        _loading.value = true
                        _refreshing.value = true
                        val response = repository.getMyReports(token)
                        if (response.success) {
                            _reports.value = response.data
                            _error.value = null
                        } else {
                            _error.value = response.message
                        }
                    } catch (e: Exception) {
                        _error.value = e.message ?: "Terjadi kesalahan saat mengambil data"
                    } finally {
                        _loading.value = false
                        _refreshing.value = false
                    }
                } else {
                    _error.value = "Token tidak ditemukan. Silakan login ulang."
                    _loading.value = false
                    _refreshing.value = false
                }
            }
        }
    }

    fun refreshReports() {
        fetchMyReports()
    }

    // 🗑️ Fungsi untuk menghapus laporan
    fun deleteReport(reportId: Int) {
        viewModelScope.launch {
            dataStore.getToken().collect { token ->
                if (token.isNotEmpty()) {
                    try {
                        val success = repository.deleteReport(token, reportId)
                        if (success) {
                            _reports.value = _reports.value.filterNot { it.id == reportId }
                            _deleteStatus.value = Pair(true, "Laporan berhasil dihapus.")
                            Log.d("DeleteReport", "Success: Report $reportId deleted.")
                        } else {
                            _deleteStatus.value = Pair(false, "Gagal menghapus laporan.")
                        }
                    } catch (e: Exception) {
                        _deleteStatus.value = Pair(false, e.message ?: "Terjadi kesalahan.")
                    }
                } else {
                    _deleteStatus.value = Pair(false, "Token tidak ditemukan.")
                }
            }
        }
    }

    fun clearDeleteStatus() {
        _deleteStatus.value = null
    }
}
