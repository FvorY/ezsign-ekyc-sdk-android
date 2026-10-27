package io.github.fvory.ekyc.api

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal class EkycRepository(
    baseUrl: String,
    private val authToken: String
) {
    private val api: EkycApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(EkycApiService::class.java)
    }

    private val bearerToken: String
        get() = if (authToken.startsWith("Bearer ")) authToken else "Bearer $authToken"

    suspend fun getEkycStatus(): Result<ApiResponse<EkycStatusData>> = runCatching {
        val res = api.getEkycStatus(bearerToken)
        if (!res.isSuccessful) throw Exception(res.errorBody()?.string() ?: "Failed to get eKYC status")
        res.body() ?: throw Exception("Empty response body")
    }

    suspend fun checkDemografi(nama: String, nik: String, birthDate: String): Result<ApiResponse<Any>> = runCatching {
        val req = DemografiCheckRequest(nama = nama, nik = nik, birthDate = birthDate)
        val resCheck = api.checkData(bearerToken, req)
        if (!resCheck.isSuccessful) throw Exception(resCheck.errorBody()?.string() ?: "Failed data validation")

        val resNik = api.verifyNik(bearerToken, VerifyNikRequest(nik))
        if (!resNik.isSuccessful) throw Exception(resNik.errorBody()?.string() ?: "NIK verification failed")

        val resDemo = api.checkDemografi(bearerToken, req)
        if (!resDemo.isSuccessful) throw Exception(resDemo.errorBody()?.string() ?: "Demographic verification failed")
        resDemo.body() ?: throw Exception("Empty response body")
    }

    suspend fun validatePhone(phone: String): Result<ApiResponse<ValidatePhoneData>> = runCatching {
        val req = ValidatePhoneRequest(phone = phone)
        val res = api.validatePhone(bearerToken, req)
        if (!res.isSuccessful) throw Exception(res.errorBody()?.string() ?: "Failed to request OTP")
        res.body() ?: throw Exception("Empty response body")
    }

    suspend fun verifyOtp(phone: String, otp: String, verifyToken: String?): Result<ApiResponse<VerifyOtpData>> = runCatching {
        val req = VerifyOtpRequest(phoneNumber = phone, otp = otp, token = verifyToken)
        val res = api.verifyOtp(bearerToken, req)
        if (!res.isSuccessful) throw Exception(res.errorBody()?.string() ?: "Invalid OTP")
        res.body() ?: throw Exception("Empty response body")
    }

    suspend fun getBizToken(): Result<ApiResponse<BizTokenData>> = runCatching {
        val res = api.getBizToken(bearerToken)
        if (!res.isSuccessful) throw Exception(res.errorBody()?.string() ?: "Failed to get biz token")
        res.body() ?: throw Exception("Empty response body")
    }

    suspend fun submitLiveness(bizToken: String, livenessId: String): Result<ApiResponse<Any>> = runCatching {
        val textMedia = "text/plain".toMediaTypeOrNull()
        val bizTokenBody = bizToken.toRequestBody(textMedia)
        val verifyIdBody = livenessId.toRequestBody(textMedia)
        val dataTypeBody = "0".toRequestBody(textMedia)
        val encryptionTypeBody = "0".toRequestBody(textMedia)

        val res = api.submitLiveness(bearerToken, bizTokenBody, verifyIdBody, dataTypeBody, encryptionTypeBody)
        if (!res.isSuccessful) throw Exception(res.errorBody()?.string() ?: "Failed to verify liveness result")
        res.body() ?: throw Exception("Empty response body")
    }

    suspend fun updateMobile(
        nama: String,
        nik: String,
        birthDate: String,
        phone: String,
        otp: String,
        bizToken: String
    ): Result<ApiResponse<Any>> = runCatching {
        val req = UpdateMobileRequest(
            nama = nama,
            nik = nik,
            birthDate = birthDate,
            phone = phone,
            otp = otp,
            bizToken = bizToken
        )
        val res = api.updateMobile(bearerToken, req)
        if (!res.isSuccessful) throw Exception(res.errorBody()?.string() ?: "Failed to update profile")
        res.body() ?: throw Exception("Empty response body")
    }

    suspend fun refreshAuth(): Result<ApiResponse<AuthRefreshData>> = runCatching {
        val res = api.refreshAuth(bearerToken)
        if (!res.isSuccessful) throw Exception(res.errorBody()?.string() ?: "Failed to refresh auth")
        res.body() ?: throw Exception("Empty response body")
    }
}
