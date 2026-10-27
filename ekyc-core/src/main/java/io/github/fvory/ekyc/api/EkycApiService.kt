package io.github.fvory.ekyc.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface EkycApiService {

    @POST("register/user/ekyc-status")
    suspend fun getEkycStatus(
        @Header("Authorization") token: String
    ): Response<ApiResponse<EkycStatusData>>

    @POST("register/checkdata")
    suspend fun checkData(
        @Header("Authorization") token: String,
        @Body request: DemografiCheckRequest
    ): Response<ApiResponse<Any>>

    @POST("register/verifyNik")
    suspend fun verifyNik(
        @Header("Authorization") token: String,
        @Body request: VerifyNikRequest
    ): Response<ApiResponse<Any>>

    @POST("register/checkdemografi")
    suspend fun checkDemografi(
        @Header("Authorization") token: String,
        @Body request: DemografiCheckRequest
    ): Response<ApiResponse<Any>>

    @POST("otp/validatephone")
    suspend fun validatePhone(
        @Header("Authorization") token: String,
        @Body request: ValidatePhoneRequest
    ): Response<ApiResponse<ValidatePhoneData>>

    @POST("register/user/verifyotp")
    suspend fun verifyOtp(
        @Header("Authorization") token: String,
        @Body request: VerifyOtpRequest
    ): Response<ApiResponse<VerifyOtpData>>

    @POST("register/user/get-biz-token")
    suspend fun getBizToken(
        @Header("Authorization") token: String
    ): Response<ApiResponse<BizTokenData>>

    @Multipart
    @POST("register/user/liveness-face-id")
    suspend fun submitLiveness(
        @Header("Authorization") token: String,
        @Part("biz_token") bizToken: RequestBody,
        @Part("verify_id") verifyId: RequestBody,
        @Part("data_type") dataType: RequestBody,
        @Part("encryption_type") encryptionType: RequestBody
    ): Response<ApiResponse<Any>>

    @POST("register/update-mobile")
    suspend fun updateMobile(
        @Header("Authorization") token: String,
        @Body request: UpdateMobileRequest
    ): Response<ApiResponse<Any>>

    @POST("auth/user/refresh")
    suspend fun refreshAuth(
        @Header("Authorization") token: String
    ): Response<ApiResponse<AuthRefreshData>>
}
