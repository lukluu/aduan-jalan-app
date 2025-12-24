package com.example.aduanjalan.data.remote.response

import com.example.aduanjalan.domain.model.User

data class RegisterResponse(
    val message: String,
    val user: User
)
