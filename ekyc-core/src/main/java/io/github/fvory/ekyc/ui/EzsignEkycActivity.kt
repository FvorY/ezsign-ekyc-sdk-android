package io.github.fvory.ekyc.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import io.github.fvory.ekyc.EzsignEkyc
import io.github.fvory.ekyc.R
import io.github.fvory.ekyc.databinding.ActivityEzsignEkycBinding
import io.github.fvory.ekyc.model.EkycConfig
import io.github.fvory.ekyc.model.EkycResult
import kotlinx.coroutines.launch

internal class EzsignEkycActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEzsignEkycBinding
    private val viewModel: EkycViewModel by viewModels()
    private lateinit var config: EkycConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEzsignEkycBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = intent.getSerializableExtra(EXTRA_CONFIG) as? EkycConfig ?: run {
            finishWithError("INVALID_CONFIG", "EkycConfig is missing")
            return
        }

        viewModel.init(config)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Same as the iOS SDK: back returns to the previous step, leaving the first step cancels
        // the flow, and the result screen cannot be left while it finalizes.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    currentFragment() is EkycResultFragment -> Unit
                    supportFragmentManager.backStackEntryCount > 0 -> supportFragmentManager.popBackStack()
                    else -> finishCancelled()
                }
            }
        })
        supportFragmentManager.addOnBackStackChangedListener { updateToolbar() }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, DemografiFragment())
            }
            supportFragmentManager.executePendingTransactions()
        }
        updateToolbar()
    }

    private fun currentFragment(): Fragment? =
        supportFragmentManager.findFragmentById(R.id.fragmentContainer)

    private fun updateToolbar() {
        val fragment = currentFragment()
        binding.toolbar.title = when (fragment) {
            is OtpFragment -> "Verification Phone"
            is LivenessInstructionFragment -> "Active Liveness Check"
            is EkycResultFragment -> "Verification"
            else -> config.title
        }
        when (fragment) {
            is EkycResultFragment -> binding.toolbar.navigationIcon = null
            is DemografiFragment -> binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_clear_material)
            else -> binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        }
    }

    fun navigateToOtp() {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, OtpFragment())
            addToBackStack(null)
        }
    }

    fun navigateToLivenessInstruction() {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, LivenessInstructionFragment())
            addToBackStack(null)
        }
    }

    fun navigateToResult() {
        // The liveness engine reports success while its own activity is still on top, so this
        // activity is stopped; wait until it is resumed before committing.
        lifecycleScope.launch {
            withResumed {
                supportFragmentManager.commit {
                    replace(R.id.fragmentContainer, EkycResultFragment())
                }
                supportFragmentManager.executePendingTransactions()
                updateToolbar()
            }
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
