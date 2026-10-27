package io.github.fvory.ekyc.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.fvory.ekyc.api.EkycRepository
import io.github.fvory.ekyc.model.EkycConfig

internal class EkycViewModel : ViewModel() {

    lateinit var config: EkycConfig
    lateinit var repository: EkycRepository

    var nik: String = ""
    var nama: String = ""
    var birthDate: String = ""

    var phone: String = ""
    var otp: String = ""
    var verifyToken: String? = null

    var bizToken: String = ""
    var livenessId: String = ""

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun init(config: EkycConfig) {
        this.config = config
        this.repository = EkycRepository(config.baseUrl, config.authToken)
    }

    fun setLoading(isLoading: Boolean) {
        _loading.postValue(isLoading)
    }

    fun setError(msg: String?) {
        _errorMessage.postValue(msg)
    }
}
