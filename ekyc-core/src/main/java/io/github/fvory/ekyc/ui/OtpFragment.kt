package io.github.fvory.ekyc.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import io.github.fvory.ekyc.databinding.FragmentOtpBinding
import kotlinx.coroutines.launch

internal class OtpFragment : Fragment() {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkycViewModel by activityViewModels()

    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSendOtp.setOnClickListener {
            sendOtp()
        }

        binding.btnVerifyOtp.setOnClickListener {
            verifyOtp()
        }
    }

    private fun sendOtp() {
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        if (phone.length < 9) {
            binding.layoutPhone.error = "Nomor handphone tidak valid"
            return
        }
        binding.layoutPhone.error = null

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSendOtp.isEnabled = false

            viewModel.repository.validatePhone(phone)
                .onSuccess { res ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, res.message ?: "OTP berhasil dikirim", Toast.LENGTH_SHORT).show()
                    viewModel.phone = phone

                    val cooldownSeconds = ((res.data?.cooldown ?: 2.0) * 60).toLong()
                    startCountdown(cooldownSeconds)
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendOtp.isEnabled = true
                    Toast.makeText(context, e.message ?: "Gagal mengirim OTP", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun startCountdown(seconds: Long) {
        countDownTimer?.cancel()
        binding.tvTimer.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = millisUntilFinished / 1000
                binding.tvTimer.text = "Kirim ulang dalam $remaining detik"
            }

            override fun onFinish() {
                binding.tvTimer.visibility = View.GONE
                binding.btnSendOtp.isEnabled = true
            }
        }.start()
    }

    private fun verifyOtp() {
        val phone = viewModel.phone.ifEmpty { binding.etPhone.text?.toString()?.trim().orEmpty() }
        val otp = binding.etOtp.text?.toString()?.trim().orEmpty()

        if (phone.isEmpty()) {
            binding.layoutPhone.error = "Kirim OTP terlebih dahulu"
            return
        }

        if (otp.length != 6) {
            binding.layoutOtp.error = "Masukkan 6 digit kode OTP"
            return
        }
        binding.layoutOtp.error = null

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnVerifyOtp.isEnabled = false

            viewModel.repository.verifyOtp(phone, otp, viewModel.verifyToken)
                .onSuccess { res ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerifyOtp.isEnabled = true

                    viewModel.otp = otp
                    viewModel.verifyToken = res.data?.token

                    (activity as? EzsignEkycActivity)?.navigateToLivenessInstruction()
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerifyOtp.isEnabled = true
                    Toast.makeText(context, e.message ?: "Kode OTP tidak valid", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
