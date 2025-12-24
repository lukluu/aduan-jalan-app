package com.example.aduanjalan.data.repository

import com.example.aduanjalan.data.remote.api.LaravelApiService
import com.example.aduanjalan.data.remote.response.MyReportWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyReportsRepository @Inject constructor(
    private val api: LaravelApiService
) {

    suspend fun getMyReports(token: String): MyReportWrapper {
        return api.getMyReports("Bearer $token")
    }

    suspend fun deleteReport(token: String, reportId: Int): Boolean {
        val response = api.deleteReport("Bearer $token", reportId)
        return response.isSuccessful
    }
}
