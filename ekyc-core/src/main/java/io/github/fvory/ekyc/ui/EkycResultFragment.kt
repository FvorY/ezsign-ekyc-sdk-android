package io.github.fvory.ekyc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import io.github.fvory.ekyc.databinding.FragmentEkycResultBinding
import kotlinx.coroutines.launch

internal class EkycResultFragment : Fragment() {

    private var _binding: FragmentEkycResultBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkycViewModel by activityViewModels()
    private var regisStatus = DEFAULT_REGIS_STATUS

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEkycResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnFinish.setOnClickListener {
            (activity as? EzsignEkycActivity)?.finishSuccess("Verifikasi eKYC selesai", regisStatus)
        }

        finalizeEkyc()
    }

    private fun finalizeEkyc() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.resultProgress.visibility = View.VISIBLE
            binding.btnFinish.isEnabled = false

            val submitLivenessRes = viewModel.repository.submitLiveness(
                viewModel.bizToken,
                viewModel.livenessId
            )

            if (submitLivenessRes.isFailure) {
                binding.resultProgress.visibility = View.GONE
                binding.tvResultTitle.text = "Verification Failed"
                binding.tvResultDesc.text = submitLivenessRes.exceptionOrNull()?.message ?: "Face liveness verification failed."
                binding.btnFinish.isEnabled = true
                binding.btnFinish.text = "Close"
                binding.btnFinish.setOnClickListener {
                    (activity as? EzsignEkycActivity)?.finishWithError(
                        "LIVENESS_SUBMIT_FAILED",
                        binding.tvResultDesc.text.toString()
                    )
                }
                return@launch
            }

            val updateRes = viewModel.repository.updateMobile(
                nama = viewModel.nama,
                nik = viewModel.nik,
                birthDate = viewModel.birthDate,
                phone = viewModel.phone,
                otp = viewModel.otp,
                bizToken = viewModel.bizToken,
                verifyToken = viewModel.verifyToken
            )

            if (updateRes.isFailure) {
                binding.resultProgress.visibility = View.GONE
                binding.tvResultTitle.text = "Verification Failed"
                binding.tvResultDesc.text = updateRes.exceptionOrNull()?.message ?: "Failed to update your identity data."
                binding.btnFinish.isEnabled = true
                binding.btnFinish.text = "Close"
                binding.btnFinish.setOnClickListener {
                    (activity as? EzsignEkycActivity)?.finishWithError(
                        "UPDATE_PROFILE_FAILED",
                        binding.tvResultDesc.text.toString()
                    )
                }
                return@launch
            }

            // Same as the Flutter app: the refreshed session tells whether RA review starts
            // or the identity has to be submitted again.
            regisStatus = viewModel.repository.refreshAuth().getOrNull()?.data?.regisStatus
                ?.takeIf { it.isNotBlank() } ?: DEFAULT_REGIS_STATUS

            binding.resultProgress.visibility = View.GONE
            binding.tvResultTitle.text = "Verification Complete"
            binding.tvResultDesc.text = if (regisStatus in REKEY_STATUSES) {
                "Your data was submitted, but the server asks you to verify your identity again."
            } else {
                "Your personal identity has been verified and is waiting for RA approval."
            }
            binding.btnFinish.isEnabled = true
            binding.btnFinish.text = "Finish"
        }
    }

    private companion object {
        const val DEFAULT_REGIS_STATUS = "wait_ra"
        val REKEY_STATUSES = setOf("in_kyc", "reject_ra", "reject_ra_rekey_exp")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
