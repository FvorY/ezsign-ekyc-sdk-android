package io.github.fvory.ekyc.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.megvii.lv5.sdk.bean.MegliveLocalFileInfo
import com.megvii.lv5.sdk.manager.MegLiveDetectConfig
import com.megvii.lv5.sdk.manager.MegLiveDetectListener
import com.megvii.lv5.sdk.manager.MegLiveManager
import io.github.fvory.ekyc.databinding.FragmentLivenessInstructionBinding
import kotlinx.coroutines.launch

internal class LivenessInstructionFragment : Fragment() {

    private var _binding: FragmentLivenessInstructionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkycViewModel by activityViewModels()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            prepareAndStartLiveness()
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk liveness check", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLivenessInstructionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartLiveness.setOnClickListener {
            checkCameraPermission()
        }
    }

    private fun checkCameraPermission() {
        val permission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            prepareAndStartLiveness()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun prepareAndStartLiveness() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnStartLiveness.isEnabled = false

            viewModel.repository.getBizToken()
                .onSuccess { res ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnStartLiveness.isEnabled = true

                    val data = res.data
                    if (data == null) {
                        Toast.makeText(context, "Gagal mendapatkan token liveness", Toast.LENGTH_SHORT).show()
                        return@onSuccess
                    }

                    viewModel.bizToken = data.bizToken
                    viewModel.livenessId = data.livenessId

                    launchMegviiSDK(data.bizToken)
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnStartLiveness.isEnabled = true
                    Toast.makeText(context, e.message ?: "Gagal inisialisasi liveness", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun launchMegviiSDK(bizToken: String) {
        val activity = requireActivity()
        val config = MegLiveDetectConfig().apply {
            setBizToken(bizToken)
            setHost("https://api-idn.megvii.com")
            setLanguage("en")
        }

        MegLiveManager.getInstance().startDetect(activity, config, object : MegLiveDetectListener {
            override fun onPreDetectFinish(errorCode: Int, errorMessage: String?) {
                if (errorCode != 1000) {
                    activity.runOnUiThread { showMegviiError(errorCode, errorMessage) }
                }
            }

            override fun onDetectFinish(errorCode: Int, errorMessage: String?, token: String?) {
                activity.runOnUiThread {
                    if (errorCode == 1000) {
                        (activity as? EzsignEkycActivity)?.navigateToResult()
                    } else {
                        showMegviiError(errorCode, errorMessage)
                    }
                }
            }

            override fun onLivenessFileCallback(livenessFilePath: String?) {
                // Optional liveness video file callback
            }

            override fun onLivenessLocalFileCallBack(info: MegliveLocalFileInfo?) {
                // Optional local video file callback
            }
        })
    }

    private fun showMegviiError(errorCode: Int, errorMessage: String?) {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle(if (errorCode == MEGVII_USER_CANCEL) "Liveness Cancelled" else "Liveness Check Failed")
            .setMessage(describeMegviiError(errorCode, errorMessage))
            .setPositiveButton("OK", null)
            .show()
    }

    // Same grouping as the ezSign Flutter app. Megvii validates its license against the host
    // app's package name, so AUTHENTICATION_FAIL / bundleid_error names the rejected package.
    private fun describeMegviiError(errorCode: Int, errorMessage: String?): String {
        val raw = errorMessage.orEmpty()
        val upper = raw.uppercase()
        val message = when {
            upper.contains("AUTHENTICATION_FAIL") || upper.contains("BUNDLEID") ->
                "The liveness license rejected ${requireContext().packageName}. " +
                    "Register this package name with the ezSign Megvii account. ($raw)"
            errorCode == MEGVII_USER_CANCEL -> return "Liveness has been cancelled"
            errorCode == MEGVII_NO_CAMERA_PERMISSION ->
                "Camera permission is required. Please allow camera access and try again."
            errorCode in MEGVII_DEVICE_NOT_SUPPORTED ->
                "Your device is currently not supported. Please try using a different device."
            errorCode in MEGVII_RETRYABLE ->
                "We couldn't complete the liveness check. Please try again in a few moments."
            raw.isNotBlank() -> raw
            else -> "We couldn't complete the liveness check."
        }
        return "$message ($errorCode)"
    }

    private companion object {
        const val MEGVII_USER_CANCEL = 1011
        const val MEGVII_NO_CAMERA_PERMISSION = 1012
        val MEGVII_DEVICE_NOT_SUPPORTED = setOf(1004, 1009, 1013)
        val MEGVII_RETRYABLE = setOf(1001, 1002, 1003, 1005, 1006, 1007, 1008, 1010, 1014, 1016, 1017, 1018, 1019)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
