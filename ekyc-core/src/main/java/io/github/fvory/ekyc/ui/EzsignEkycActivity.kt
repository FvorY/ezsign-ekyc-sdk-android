package io.github.fvory.ekyc.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import io.github.fvory.ekyc.EzsignEkyc
import io.github.fvory.ekyc.R
import io.github.fvory.ekyc.databinding.ActivityEzsignEkycBinding
import io.github.fvory.ekyc.model.EkycConfig
import io.github.fvory.ekyc.model.EkycResult

internal class EzsignEkycActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEzsignEkycBinding
    private val viewModel: EkycViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEzsignEkycBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val config = intent.getSerializableExtra(EXTRA_CONFIG) as? EkycConfig
        if (config == null) {
            finishWithError("INVALID_CONFIG", "EkycConfig is missing")
            return
        }

        viewModel.init(config)
        binding.toolbar.title = config.title
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            finishCancelled()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, DemografiFragment())
            }
        }
    }

    fun navigateToOtp() {
        binding.toolbar.title = "Verification Phone"
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, OtpFragment())
            addToBackStack(null)
        }
    }

    fun navigateToLivenessInstruction() {
        binding.toolbar.title = "Active Liveness Check"
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, LivenessInstructionFragment())
            addToBackStack(null)
        }
    }

    fun navigateToResult() {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, EkycResultFragment())
        }
    }

    fun finishSuccess(message: String, regisStatus: String = "wait_ra") {
        val result = EkycResult.Success(message, regisStatus)
        val data = Intent().apply {
            putExtra(EzsignEkyc.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    fun finishWithError(errorCode: String, message: String) {
        val result = EkycResult.Failed(errorCode, message)
        val data = Intent().apply {
            putExtra(EzsignEkyc.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_CANCELED, data)
        finish()
    }

    fun finishCancelled() {
        val data = Intent().apply {
            putExtra(EzsignEkyc.EXTRA_RESULT, EkycResult.Cancelled)
        }
        setResult(Activity.RESULT_CANCELED, data)
        finish()
    }

    companion object {
        const val EXTRA_CONFIG = "io.github.fvory.ekyc.EXTRA_CONFIG"
    }
}
