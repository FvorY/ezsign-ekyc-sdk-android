package io.github.fvory.ekyc.model

import java.io.Serializable

data class EkycConfig(
    val authToken: String,
    val baseUrl: String = "https://gtm.myesign.id/api",
    val title: String = "Your Identity",
    val primaryColorHex: String = "#15C1E7"
) : Serializable

sealed class EkycResult : Serializable {
    data class Success(
        val message: String,
        val regisStatus: String
    ) : EkycResult()

    data class Failed(
        val errorCode: String,
        val message: String
    ) : EkycResult()

    object Cancelled : EkycResult()
}
