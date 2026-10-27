package io.github.fvory.ekyc.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
        lifecycleScope.launch {
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
                    activity.runOnUiThread {
                        Toast.makeText(context, errorMessage ?: "Deteksi gagal dimulai ($errorCode)", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onDetectFinish(errorCode: Int, errorMessage: String?, token: String?) {
                activity.runOnUiThread {
                    if (errorCode == 1000) {
                        (activity as? EzsignEkycActivity)?.navigateToResult()
                    } else if (errorCode == 1011) {
                        Toast.makeText(context, "Liveness dibatalkan oleh pengguna", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, errorMessage ?: "Liveness gagal ($errorCode)", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onLivenessLocalFileCallBack(info: MegliveLocalFileInfo?) {
                // Optional local video file callback
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
