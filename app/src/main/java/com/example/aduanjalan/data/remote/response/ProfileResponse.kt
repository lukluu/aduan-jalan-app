package com.example.aduanjalan.data.remote.response


import com.google.gson.annotations.SerializedName

// Bisa digunakan untuk response GET dan POST profile
data class ProfileResponse(
    @SerializedName("message") // Untuk response update
    val message: String? = null,

    @SerializedName("user") // Untuk response update
    val user: UserData? = null,

    // Langsung mapping jika response-nya adalah objek user (untuk GET profile)
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("telephone_number")
    val telephoneNumber: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("photo")
    val photo: String? = null,

    @SerializedName("gender")
    val gender: String? = null
)

// Sub-model untuk struktur bersarang di response update
data class UserData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("telephone_number")
    val telephoneNumber: String?,

    @SerializedName("address")
    val address: String?,

    @SerializedName("photo")
    val photo: String?,

    @SerializedName("gender")
    val gender: String?
)