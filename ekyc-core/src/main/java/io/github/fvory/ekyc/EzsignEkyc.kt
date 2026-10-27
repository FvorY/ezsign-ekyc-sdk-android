package io.github.fvory.ekyc

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import io.github.fvory.ekyc.model.EkycConfig
import io.github.fvory.ekyc.model.EkycResult
import io.github.fvory.ekyc.ui.EzsignEkycActivity

object EzsignEkyc {

    const val EXTRA_RESULT = "io.github.fvory.ekyc.EXTRA_RESULT"

    /**
     * Starts the eKYC verification flow from an Activity.
     *
     * @param activity Hosting activity
     * @param config Configuration containing authToken and baseUrl
     * @param requestCode Request code for onActivityResult callback
     */
    fun start(activity: Activity, config: EkycConfig, requestCode: Int = 1001) {
        val intent = Intent(activity, EzsignEkycActivity::class.java).apply {
            putExtra(EzsignEkycActivity.EXTRA_CONFIG, config)
        }
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Parses the result from onActivityResult.
     */
    fun parseResult(resultCode: Int, data: Intent?): EkycResult {
        return (data?.getSerializableExtra(EXTRA_RESULT) as? EkycResult) ?: EkycResult.Cancelled
    }

    /**
     * ActivityResultContract for modern Activity Result API:
     *
     * val launcher = registerForActivityResult(EzsignEkyc.Contract()) { result ->
     *     when (result) {
     *         is EkycResult.Success -> ...
     *         is EkycResult.Failed -> ...
     *         is EkycResult.Cancelled -> ...
     *     }
     * }
     * launcher.launch(config)
     */
    class Contract : ActivityResultContract<EkycConfig, EkycResult>() {
        override fun createIntent(context: Context, input: EkycConfig): Intent {
            return Intent(context, EzsignEkycActivity::class.java).apply {
                putExtra(EzsignEkycActivity.EXTRA_CONFIG, input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): EkycResult {
            return EzsignEkyc.parseResult(resultCode, intent)
        }
    }
}
