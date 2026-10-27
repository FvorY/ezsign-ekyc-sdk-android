package io.github.fvory.ekyc.api

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

data class EkycStatusData(
    @SerializedName("canEkyc") val canEkyc: Boolean,
    @SerializedName("citizenshipCode") val citizenshipCode: String?
)

data class DemografiCheckRequest(
    @SerializedName("nama") val nama: String,
    @SerializedName("nik") val nik: String,
    @SerializedName("birthDate") val birthDate: String // dd-MM-yyyy
)

data class VerifyNikRequest(
    @SerializedName("nik") val nik: String
)

data class ValidatePhoneRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("otpType") val otpType: Int = 1,
    @SerializedName("menu") val menu: String = "register_update"
)

data class ValidatePhoneData(
    @SerializedName("cooldown") val cooldown: Double?
)

data class VerifyOtpRequest(
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("token") val token: String? = null
)

data class VerifyOtpData(
    @SerializedName("token") val token: String?
)

data class BizTokenData(
    @SerializedName("biz_token") val bizToken: String,
    @SerializedName("liveness_id") val livenessId: String
)

data class UpdateMobileRequest(
    @SerializedName("nama") val nama: String,
    @SerializedName("nik") val nik: String,
    @SerializedName("birthDate") val birthDate: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("bizToken") val bizToken: String
)

data class AuthRefreshData(
    @SerializedName("regisStatus") val regisStatus: String?,
    @SerializedName("token") val token: String?
)
