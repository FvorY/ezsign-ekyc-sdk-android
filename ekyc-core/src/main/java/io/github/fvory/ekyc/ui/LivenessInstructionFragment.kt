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
                    activity.runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        AlertDialog.Builder(requireContext())
                            .setTitle("Liveness tidak dapat dimulai")
                            .setMessage(describeStartError(errorCode, errorMessage))
                            .setPositiveButton("OK", null)
                            .show()
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

            override fun onLivenessFileCallback(livenessFilePath: String?) {
                // Optional liveness video file callback
            }

            override fun onLivenessLocalFileCallBack(info: MegliveLocalFileInfo?) {
                // Optional local video file callback
            }
        })
    }

    /**
     * Megvii validates its license against the host app's package name. AUTHENTICATION_FAIL /
     * bundleid_error means that package name is not registered for the ezSign Megvii account.
     */
    private fun describeStartError(errorCode: Int, errorMessage: String?): String {
        val raw = errorMessage ?: "Deteksi gagal dimulai ($errorCode)"
        val upper = raw.uppercase()
        if (upper.contains("AUTHENTICATION_FAIL") || upper.contains("BUNDLEID")) {
            val packageName = requireContext().packageName
            return "Lisensi liveness menolak aplikasi $packageName. " +
                "Package name ini harus didaftarkan pada akun Megvii ezSign. ($raw)"
        }
        return raw
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
