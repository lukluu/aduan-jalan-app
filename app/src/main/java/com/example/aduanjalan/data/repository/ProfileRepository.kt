package com.example.aduanjalan.data.repository


import android.content.Context
import android.net.Uri
import com.example.aduanjalan.data.DataStoreManager
import com.example.aduanjalan.data.remote.api.LaravelApiService
import com.example.aduanjalan.data.remote.response.ProfileResponse
import com.example.aduanjalan.ui.utils.uriToFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val apiService: LaravelApiService,
    private val dataStoreManager: DataStoreManager,
    @ApplicationContext private val context: Context
) {

    suspend fun getProfile(): Result<ProfileResponse> {
        return try {
            val token = "Bearer ${dataStoreManager.getToken().first()}"
            val response = apiService.getProfile(token)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        name: String,
        email: String,
        telephone: String?,
        address: String?,
        gender: String?,
        imageUri: Uri?,
        password: String?,
    ): Result<ProfileResponse> {
        return try {
            val token = "Bearer ${dataStoreManager.getToken().first()}"

            // Konversi String ke RequestBody
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
            val telBody = telephone?.toRequestBody("text/plain".toMediaTypeOrNull())
            val addressBody = address?.toRequestBody("text/plain".toMediaTypeOrNull())
            val genderBody = gender?.toRequestBody("text/plain".toMediaTypeOrNull())
            val passBody = password?.toRequestBody("text/plain".toMediaTypeOrNull())
            val passConfirmBody = password?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Konversi Uri gambar ke MultipartBody.Part
            var imagePart: MultipartBody.Part? = null
            imageUri?.let {
                val imageFile = uriToFile(it, context)
                val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData(
                    "photo", imageFile.name, requestImageFile
                )
            }

            val response = apiService.updateProfile(
                token,
                nameBody,
                emailBody,
                telBody,
                addressBody,
                genderBody,
                imagePart,
                passBody,
                passConfirmBody
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}