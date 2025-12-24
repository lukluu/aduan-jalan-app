package com.example.aduanjalan.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduanjalan.data.DataStoreManager
import com.example.aduanjalan.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

// Sealed Interface untuk merepresentasikan state UI yang jelas
sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val message: String) : AuthState
    data class Error(val message: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    // Gunakan satu state untuk mengelola semua kondisi (loading, success, error)
    var authState by mutableStateOf<AuthState>(AuthState.Idle)
        private set

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authState = AuthState.Loading
            try {
                val res = repository.login(email, password)
                dataStoreManager.saveToken(res.token)
                dataStoreManager.saveUserName(res.user.name)
                authState = AuthState.Success("Berhasil login")
            } catch (e: Exception) {
                authState = AuthState.Error(e.message ?: "Email atau password salah")
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            authState = AuthState.Loading
            try {
                val responseMessage = repository.register(name, email, password)
                // Pesan sukses diambil dari response jika ada, jika tidak, gunakan default
                authState = AuthState.Success(responseMessage.message ?: "Pendaftaran berhasil!")
            } catch (e: Exception) {
                authState = AuthState.Error(e.message ?: "Gagal mendaftar, coba lagi")
            }
        }
    }

    // Fungsi untuk mereset state setelah pesan ditampilkan di UI
    fun resetAuthState() {
        authState = AuthState.Idle
    }

    suspend fun getToken(): String? {
        return dataStoreManager.getToken().firstOrNull()
    }
}