package com.example.aduanjalan.data.repository

import com.example.aduanjalan.data.remote.api.LaravelApiService
import com.example.aduanjalan.data.remote.response.AllReportResponse
import com.example.aduanjalan.data.remote.response.BaseResponse
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AllReportsRepository @Inject constructor(
    private val api: LaravelApiService
) {
    suspend fun getAllReports(token: String): List<AllReportResponse>? {
        val response = api.getAllReports("Bearer $token")
        return if (response.isSuccessful) response.body()?.data else null
    }

    suspend fun voteReport(token: String, reportId: Int): Pair<Boolean, String> {
        try {
            val response = api.voteReport("Bearer $token", reportId)
            return if (response.isSuccessful) {
                Pair(true, response.body()?.message ?: "Vote berhasil.")
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, BaseResponse::class.java).message ?: "Gagal vote."
                } catch (e: Exception) {
                    // Tangkap juga error dari catch block laravel
                    try {
                        val errorJson = Gson().fromJson(errorBody, Map::class.java)
                        errorJson["error"]?.toString() ?: "Gagal vote (Kode: ${response.code()})"
                    } catch (e: Exception) {
                        "Gagal vote (Kode: ${response.code()})"
                    }
                }
                Pair(false, errorMessage)
            }
        } catch (e: Exception) {
            return Pair(false, e.message ?: "Terjadi kesalahan jaringan.")
        }
    }
}
