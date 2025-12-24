package com.example.aduanjalan.data.remote.response

import com.example.aduanjalan.domain.model.User

data class LoginResponse(
    val token: String,
    val user: User
)
