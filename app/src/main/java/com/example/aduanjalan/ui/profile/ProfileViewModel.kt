package com.example.aduanjalan.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduanjalan.data.DataStoreManager
import com.example.aduanjalan.data.remote.response.ProfileResponse
import com.example.aduanjalan.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: ProfileResponse? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getProfile()
                .onSuccess { response ->
                    _uiState.update { it.copy(isLoading = false, user = response) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun updateProfile(
        name: String,
        email: String,
        telephone: String,
        address: String,
        gender: String,
        imageUri: Uri?,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            repository.updateProfile(
                name, email, telephone, address, gender, imageUri,
                if (password.isBlank()) null else password
            )
                .onSuccess { response ->
                    // Update nama di DataStore agar header HomeScreen juga update
                    response.user?.name?.let { newName ->
                        dataStoreManager.saveUserName(newName)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = response.message,
                            user = ProfileResponse(
                                id = response.user?.id,
                                name = response.user?.name,
                                email = response.user?.email,
                                telephoneNumber = response.user?.telephoneNumber,
                                address = response.user?.address,
                                photo = response.user?.photo,
                                gender = response.user?.gender
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}