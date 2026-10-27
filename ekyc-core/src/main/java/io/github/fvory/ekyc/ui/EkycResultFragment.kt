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
            (activity as? EzsignEkycActivity)?.finishSuccess("Verifikasi eKYC selesai, menunggu approval RA")
        }

        finalizeEkyc()
    }

    private fun finalizeEkyc() {
        lifecycleScope.launch {
            binding.resultProgress.visibility = View.VISIBLE
            binding.btnFinish.isEnabled = false

            val submitLivenessRes = viewModel.repository.submitLiveness(
                viewModel.bizToken,
                viewModel.livenessId
            )

            if (submitLivenessRes.isFailure) {
                binding.resultProgress.visibility = View.GONE
                binding.tvResultTitle.text = "Verifikasi Gagal"
                binding.tvResultDesc.text = submitLivenessRes.exceptionOrNull()?.message ?: "Gagal memverifikasi liveness wajah."
                binding.btnFinish.isEnabled = true
                binding.btnFinish.text = "Tutup"
                binding.btnFinish.setOnClickListener {
                    activity?.finish()
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
                binding.tvResultTitle.text = "Pembaruan Gagal"
                binding.tvResultDesc.text = updateRes.exceptionOrNull()?.message ?: "Gagal memperbarui data identitas."
                binding.btnFinish.isEnabled = true
                binding.btnFinish.text = "Tutup"
                binding.btnFinish.setOnClickListener {
                    activity?.finish()
                }
                return@launch
            }

            viewModel.repository.refreshAuth()

            binding.resultProgress.visibility = View.GONE
            binding.tvResultTitle.text = "Verifikasi Selesai"
            binding.tvResultDesc.text = "Data eKYC Anda telah berhasil diverifikasi dan sedang diproses oleh Registration Authority (RA)."
            binding.btnFinish.isEnabled = true
            binding.btnFinish.text = "Selesai"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
