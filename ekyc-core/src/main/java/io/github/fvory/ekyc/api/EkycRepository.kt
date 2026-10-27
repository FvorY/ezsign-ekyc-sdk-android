package io.github.fvory.ekyc.api

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal class EkycRepository(
    baseUrl: String,
    private val authToken: String
) {
    private val api: EkycApiService

    init {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "ezSign-Android")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val cleanUrl = baseUrl.trim().trimEnd('/')
        val baseWithApi = if (cleanUrl.endsWith("/api")) cleanUrl else "$cleanUrl/api"
        val normalizedBaseUrl = "$baseWithApi/"

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(EkycApiService::class.java)
    }

    private val bearerToken: String
        get() {
            var clean = authToken.trim()
            if (clean.startsWith("\"") && clean.endsWith("\"")) {
                clean = clean.substring(1, clean.length - 1).trim()
            }
            if (clean.startsWith("Bearer ", ignoreCase = true)) {
                clean = clean.substring(7).trim()
            }
            clean = clean.replace("\n", "").replace("\r", "").replace(" ", "").trim()
            return "Bearer $clean"
        }

    private fun extractErrorMessage(errorBodyStr: String?, defaultMessage: String): String {
        if (errorBodyStr.isNullOrBlank()) return defaultMessage
        return try {
            val json = org.json.JSONObject(errorBodyStr)
            val msg = when {
                json.has("message") && !json.isNull("message") -> json.getString("message")
                json.has("error") && !json.isNull("error") -> json.getString("error")
                else -> defaultMessage
            }
            if (msg.contains("JWT", ignoreCase = true) || msg.contains("Unauthorized", ignoreCase = true)) {
                "Sesi token tidak valid atau kadaluarsa ($msg). Pastikan token masih aktif dan Base URL sesuai environment akun Anda."
            } else {
                msg
            }
        } catch (_: Exception) {
            errorBodyStr
        }
    }

    suspend fun getEkycStatus(): Result<ApiResponse<EkycStatusData>> = apiResult {
        val res = api.getEkycStatus(bearerToken)
        if (!res.isSuccessful) {
            throw Exception(extractErrorMessage(res.errorBody()?.string(), "Gagal memeriksa status eKYC"))
        }
        val body = res.body() ?: throw Exception("Response body kosong")
        if (body.code != 200 && body.code != 0) {
            throw Exception(body.message ?: "Gagal memeriksa status eKYC")
        }
        body
    }

    suspend fun checkDemografi(nama: String, nik: String, birthDate: String): Result<ApiResponse<Any>> = apiResult {
        val req = DemografiCheckRequest(nama = nama, nik = nik, birthDate = birthDate)
        val resCheck = api.checkData(bearerToken, req)
        if (!resCheck.isSuccessful) {
            throw Exception(extractErrorMessage(resCheck.errorBody()?.string(), "Validasi data gagal"))
        }
        val checkBody = resCheck.body()
        if (checkBody != null && checkBody.code != 200 && checkBody.code != 0) {
            throw Exception(checkBody.message ?: "Validasi data gagal")
        }

        val resNik = api.verifyNik(bearerToken, VerifyNikRequest(nik))
        if (!resNik.isSuccessful) {
            throw Exception(extractErrorMessage(resNik.errorBody()?.string(), "Verifikasi NIK gagal"))
        }
        val nikBody = resNik.body()
        if (nikBody != null && nikBody.code != 200 && nikBody.code != 0) {
            throw Exception(nikBody.message ?: "Verifikasi NIK gagal")
        }

        val resDemo = api.checkDemografi(bearerToken, req)
        if (!resDemo.isSuccessful) {
            throw Exception(extractErrorMessage(resDemo.errorBody()?.string(), "Verifikasi demografi gagal"))
        }
        val demoBody = resDemo.body() ?: throw Exception("Response body kosong")
        if (demoBody.code != 200 && demoBody.code != 0) {
            throw Exception(demoBody.message ?: "Verifikasi demografi gagal")
        }
        demoBody
    }

    suspend fun validatePhone(phone: String): Result<ApiResponse<ValidatePhoneData>> = apiResult {
        val req = ValidatePhoneRequest(phone = phone)
        val res = api.validatePhone(bearerToken, req)
        if (!res.isSuccessful) {
            throw Exception(extractErrorMessage(res.errorBody()?.string(), "Gagal meminta OTP"))
        }
        val body = res.body() ?: throw Exception("Response body kosong")
        if (body.code != 200 && body.code != 0) {
            throw Exception(body.message ?: "Gagal meminta OTP")
        }
        body
    }

    suspend fun verifyOtp(phone: String, otp: String, verifyToken: String?): Result<ApiResponse<VerifyOtpData>> = apiResult {
        val req = VerifyOtpRequest(phoneNumber = phone, otp = otp, token = verifyToken)
        val res = api.verifyOtp(bearerToken, req)
        if (!res.isSuccessful) {
            throw Exception(extractErrorMessage(res.errorBody()?.string(), "Kode OTP tidak valid"))
        }
        val body = res.body() ?: throw Exception("Response body kosong")
        if (body.code != 200 && body.code != 0) {
            throw Exception(body.message ?: "Kode OTP tidak valid")
        }
        body
    }

    suspend fun getBizToken(): Result<ApiResponse<BizTokenData>> = apiResult {
        val res = api.getBizToken(bearerToken)
        if (!res.isSuccessful) {
            throw Exception(extractErrorMessage(res.errorBody()?.string(), "Gagal mendapatkan biz token"))
        }
        val body = res.body() ?: throw Exception("Response body kosong")
        if (body.code != 200 && body.code != 0) {
            throw Exception(body.message ?: "Gagal mendapatkan biz token")
        }
        body
    }

    suspend fun submitLiveness(bizToken: String, livenessId: String): Result<ApiResponse<Any>> = apiResult {
        val textMedia = "text/plain".toMediaTypeOrNull()
        val bizTokenBody = bizToken.toRequestBody(textMedia)
        val verifyIdBody = livenessId.toRequestBody(textMedia)
        val dataTypeBody = "0".toRequestBody(textMedia)
        val encryptionTypeBody = "0".toRequestBody(textMedia)

        val res = api.submitLiveness(bearerToken, bizTokenBody, verifyIdBody, dataTypeBody, encryptionTypeBody)
        if (!res.isSuccessful) {
            throw Exception(extractErrorMessage(res.errorBody()?.string(), "Verifikasi wajah liveness gagal"))
        }
        val body = res.body() ?: throw Exception("Response body kosong")
        if (body.code != 200 && body.code != 0) {
            throw Exception(body.message ?: "Verifikasi wajah liveness gagal")
        }
        body
    }

    suspend fun updateMobile(
        nama: String,
        nik: String,
        birthDate: String,
        phone: String,
        otp: String,
        bizToken: String,
        verifyToken: String? = null
    ): Result<ApiResponse<Any>> = apiResult {
        val req = UpdateMobileRequest(
            nama = nama,
            nik = nik,
            birthDate = birthDate,
            phone = phone,
            otp = otp,
            bizToken = bizToken,
            token = verifyToken
        )
        val res = api.updateMobile(bearerToken, req)
        if (!res.isSuccessful) {
            throw Exception(extractErrorMessage(res.errorBody()?.string(), "Gagal memperbarui profil"))
        }
        val body = res.body() ?: throw Exception("Response body kosong")
        if (body.code != 200 && body.code != 0) {
            throw Exception(body.message ?: "Gagal memperbarui profil")
        }
        body
    }

    suspend fun refreshAuth(): Result<ApiResponse<AuthRefreshData>> = apiResult {
        val res = api.refreshAuth(bearerToken)
        if (!res.isSuccessful) {
            throw Exception(extractErrorMessage(res.errorBody()?.string(), "Gagal memperbarui sesi"))
        }
        val body = res.body() ?: throw Exception("Response body kosong")
        if (body.code != 200 && body.code != 0) {
            throw Exception(body.message ?: "Gagal memperbarui sesi")
        }
        body
    }
}
