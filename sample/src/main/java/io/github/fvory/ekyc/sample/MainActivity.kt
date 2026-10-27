package io.github.fvory.ekyc.sample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.fvory.ekyc.EzsignEkyc
import io.github.fvory.ekyc.model.EkycConfig
import io.github.fvory.ekyc.model.EkycResult
import io.github.fvory.ekyc.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isVerified = false

    private val ekycLauncher = registerForActivityResult(EzsignEkyc.Contract()) { result ->
        when (result) {
            is EkycResult.Success -> {
                isVerified = true
                binding.tvUserStatusBadge.text = "eKYC Selesai"
                binding.tvUserStatusBadge.setBackgroundResource(R.drawable.badge_status_success)
                binding.tvUserStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.sample_success_text))

                binding.btnStartEkyc.text = "eKYC Selesai (Menunggu RA)"
                binding.btnStartEkyc.isEnabled = false

                binding.tvCallbackResult.text = "Callback SDK: Success\nStatus: ${result.regisStatus}\nPesan: ${result.message}"
                Toast.makeText(this, "eKYC Berhasil Diselesaikan!", Toast.LENGTH_SHORT).show()
            }
            is EkycResult.Failed -> {
                binding.tvCallbackResult.text = "Callback SDK: Failed\nKode: ${result.errorCode}\nPesan: ${result.message}"
                Toast.makeText(this, "eKYC Gagal: ${result.message}", Toast.LENGTH_LONG).show()
            }
            is EkycResult.Cancelled -> {
                binding.tvCallbackResult.text = "Callback SDK: Cancelled oleh pengguna"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartEkyc.setOnClickListener {
            launchEkyc()
        }

        binding.cardSignDoc.setOnClickListener { handleServiceClick("Tanda Tangani Dokumen") }
        binding.cardUploadDoc.setOnClickListener { handleServiceClick("Kirim Dokumen") }
        binding.cardSeal.setOnClickListener { handleServiceClick("Segel Elektronik") }
        binding.cardCert.setOnClickListener { handleServiceClick("Sertifikat Saya") }
    }

    private fun handleServiceClick(serviceName: String) {
        if (!isVerified) {
            AlertDialog.Builder(this)
                .setTitle("Fitur Terkunci")
                .setMessage("Layanan '$serviceName' memerlukan akun terverifikasi eKYC. Silakan verifikasi identitas Anda terlebih dahulu.")
                .setPositiveButton("Verifikasi Sekarang") { _, _ ->
                    launchEkyc()
                }
                .setNegativeButton("Nanti", null)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle(serviceName)
                .setMessage("Akun Anda telah menyelesaikan eKYC. Layanan siap digunakan setelah disetujui Registration Authority.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun launchEkyc() {
        val token = binding.etAuthToken.text?.toString()?.trim().orEmpty()
        val baseUrl = binding.etBaseUrl.text?.toString()?.trim().orEmpty().ifEmpty { "https://gtm.myesign.id/api" }

        if (token.isEmpty()) {
            binding.layoutAuthToken.error = "Masukkan Bearer Auth Token terlebih dahulu"
            Toast.makeText(this, "Silakan isi Bearer Auth Token akun Anda untuk pengujian", Toast.LENGTH_SHORT).show()
            return
        }
        binding.layoutAuthToken.error = null

        val config = EkycConfig(
            authToken = token,
            baseUrl = baseUrl,
            title = "Verifikasi Identitas"
        )
        ekycLauncher.launch(config)
    }
}
