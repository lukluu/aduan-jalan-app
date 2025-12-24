package com.example.aduanjalan.data.remote.api

import com.example.aduanjalan.data.remote.request.CreateReportRequest
import com.example.aduanjalan.data.remote.request.LoginRequest
import com.example.aduanjalan.data.remote.request.RegisterRequest
import com.example.aduanjalan.data.remote.response.AllReportResponse
import com.example.aduanjalan.data.remote.response.AllReportWrapper
import com.example.aduanjalan.data.remote.response.BaseResponse
import com.example.aduanjalan.data.remote.response.CriteriaResponse
import com.example.aduanjalan.data.remote.response.LoginResponse
import com.example.aduanjalan.data.remote.response.MyReportWrapper
import com.example.aduanjalan.data.remote.response.ProfileResponse
import com.example.aduanjalan.data.remote.response.RegisterResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface LaravelApiService {

    // 🔑 Auth
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("logout")
    suspend fun logout(@Header("Authorization") token: String): BaseResponse

    // 📋 Criteria
    @GET("criterias")
    suspend fun getCriterias(): List<CriteriaResponse>

    // 🧾 Create Report
    @Multipart
    @POST("reports")
    suspend fun createReport(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("location_address") locationAddress: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("road_type") roadType: RequestBody,
        @Part("detections") detections: RequestBody,
        @Part("answers") answers: RequestBody
    ): BaseResponse

    // 📄 My Reports
    @GET("reports/my")
    suspend fun getMyReports(
        @Header("Authorization") token: String
    ): MyReportWrapper

    // ❌ Delete Report
    @DELETE("reports/{id}")
    suspend fun deleteReport(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<BaseResponse>


    @GET("reports")
    suspend fun getAllReports(
        @Header("Authorization") token: String
    ): Response<AllReportWrapper>


    @POST("reports/{id}/vote")
    suspend fun voteReport(
        @Header("Authorization") token: String,
        @Path("id") reportId: Int
    ): Response<BaseResponse>

    @GET("profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): ProfileResponse

    @Multipart
    @POST("profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("telephone_number") telephoneNumber: RequestBody?,
        @Part("address") address: RequestBody?,
        @Part("gender") gender: RequestBody?,
        @Part photo: MultipartBody.Part?,
        @Part("password") password: RequestBody?,
        @Part("password_confirmation") passwordConfirmation: RequestBody?
    ): ProfileResponse


}
