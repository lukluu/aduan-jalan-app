package com.example.aduanjalan.data.repository


import com.example.aduanjalan.data.remote.api.LaravelApiService
import com.example.aduanjalan.data.remote.request.CreateReportRequest
import com.example.aduanjalan.data.remote.request.DetectionRequest
import com.example.aduanjalan.data.remote.response.BaseResponse
import com.example.aduanjalan.domain.model.Criteria
import com.example.aduanjalan.domain.model.CriteriaOption
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import javax.inject.Inject

class CreateReportRepository @Inject constructor(
    private val api: LaravelApiService
) {
    suspend fun getCriterias(): List<Criteria> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCriterias()
            response.map { cr ->
                Criteria(
                    id = cr.id,
                    name = cr.name,
                    question = cr.question,
                    weight = cr.weight,
                    options = cr.options.map { opt ->
                        CriteriaOption(
                            id = opt.id,
                            label = opt.label,
                            value = opt.value
                        )
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    suspend fun submitReport(
        token: String,
        imageFile: File,
        address: String,
        roadType: String,
        lat: Double,
        lon: Double,
        detections: List<DetectionRequest>,
        answers: List<Map<String, Int>>
    ): BaseResponse = withContext(Dispatchers.IO) {

        val gson = Gson()

        // ✅ Siapkan bagian multipart
        val imageRequest = RequestBody.create("image/*".toMediaTypeOrNull(), imageFile)
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequest)

        val addressPart = RequestBody.create("text/plain".toMediaTypeOrNull(), address)
        val latPart = RequestBody.create("text/plain".toMediaTypeOrNull(), lat.toString())
        val lonPart = RequestBody.create("text/plain".toMediaTypeOrNull(), lon.toString())

        // konversi list deteksi & jawaban jadi JSON string
        val detectionsJson = gson.toJson(detections)
        val answersJson = gson.toJson(answers)
        val detectionsPart = RequestBody.create("application/json".toMediaTypeOrNull(), detectionsJson)
        val answersPart = RequestBody.create("application/json".toMediaTypeOrNull(), answersJson)
        val roadTypePart = RequestBody.create("text/plain".toMediaTypeOrNull(), roadType)

        api.createReport(
            token = "Bearer $token",
            image = imagePart,
            locationAddress = addressPart,
            latitude = latPart,
            roadType = roadTypePart,
            longitude = lonPart,
            detections = detectionsPart,
            answers = answersPart
        )
    }

}
