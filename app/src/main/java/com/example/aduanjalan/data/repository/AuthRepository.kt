package com.example.aduanjalan.data.repository

import com.example.aduanjalan.data.remote.api.LaravelApiService
import com.example.aduanjalan.data.remote.request.LoginRequest
import com.example.aduanjalan.data.remote.request.RegisterRequest
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: LaravelApiService
) {
    suspend fun login(email: String, password: String) = api.login(LoginRequest(email, password))
    suspend fun register(name: String, email: String, password: String) =
        api.register(RegisterRequest(name, email, password))
    suspend fun logout(token: String) = api.logout("Bearer $token")
}
