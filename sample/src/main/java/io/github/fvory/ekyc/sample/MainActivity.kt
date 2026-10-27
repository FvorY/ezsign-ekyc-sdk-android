package io.github.fvory.ekyc.sample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.fvory.ekyc.EzsignEkyc
import io.github.fvory.ekyc.model.EkycConfig
import io.github.fvory.ekyc.model.EkycResult
import io.github.fvory.ekyc.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val ekycLauncher = registerForActivityResult(EzsignEkyc.Contract()) { result ->
        when (result) {
            is EkycResult.Success -> {
                binding.tvStatus.text = "Status: Berhasil (${result.regisStatus})\n${result.message}"
                Toast.makeText(this, "eKYC Selesai", Toast.LENGTH_SHORT).show()
            }
            is EkycResult.Failed -> {
                binding.tvStatus.text = "Status: Gagal [${result.errorCode}]\n${result.message}"
                Toast.makeText(this, "eKYC Gagal: ${result.message}", Toast.LENGTH_LONG).show()
            }
            is EkycResult.Cancelled -> {
                binding.tvStatus.text = "Status: Dibatalkan oleh pengguna"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartEkyc.setOnClickListener {
            val token = binding.etAuthToken.text?.toString()?.trim().orEmpty()
            if (token.isEmpty()) {
                binding.layoutAuthToken.error = "Auth token tidak boleh kosong"
                return@setOnClickListener
            }
            binding.layoutAuthToken.error = null

            val config = EkycConfig(
                authToken = token,
                baseUrl = "https://api.ezsign.id",
                title = "Verifikasi Identitas"
            )
            ekycLauncher.launch(config)
        }
    }
}
