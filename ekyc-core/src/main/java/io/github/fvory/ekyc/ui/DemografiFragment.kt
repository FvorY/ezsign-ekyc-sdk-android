package io.github.fvory.ekyc.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import io.github.fvory.ekyc.databinding.FragmentDemografiBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal class DemografiFragment : Fragment() {

    private var _binding: FragmentDemografiBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkycViewModel by activityViewModels()

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDemografiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etBirthDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnContinue.setOnClickListener {
            validateAndSubmit()
        }

        checkStatus()
    }

    private fun checkStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnContinue.isEnabled = false

            viewModel.repository.getEkycStatus()
                .onSuccess { res ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnContinue.isEnabled = true
                    if (res.data?.canEkyc == false) {
                        val message = res.message ?: "Akun tidak dapat melakukan eKYC"
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        (activity as? EzsignEkycActivity)?.finishWithError("EKYC_NOT_ALLOWED", message)
                    }
                }
                .onFailure {
                    binding.progressBar.visibility = View.GONE
                    binding.btnContinue.isEnabled = true
                }
        }
    }

    private fun showDatePicker() {
        val dpd = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.etBirthDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR) - 20,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show()
    }

    private fun validateAndSubmit() {
        val nik = binding.etNik.text?.toString()?.trim().orEmpty()
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val birthDate = binding.etBirthDate.text?.toString()?.trim().orEmpty()

        if (nik.length != 16) {
            binding.layoutNik.error = "NIK harus terdiri dari 16 digit"
            return
        } else {
            binding.layoutNik.error = null
        }

        if (name.isEmpty()) {
            binding.layoutName.error = "Nama lengkap wajib diisi"
            return
        } else {
            binding.layoutName.error = null
        }

        if (birthDate.isEmpty()) {
            binding.layoutBirthDate.error = "Tanggal lahir wajib dipilih"
            return
        } else {
            binding.layoutBirthDate.error = null
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Review Your Information")
            .setMessage("Identity Number: $nik\nFullname: $name\nDate of Birth: $birthDate\n\nPlease confirm that the information you entered is correct.")
            .setPositiveButton("Confirm") { _, _ ->
                submitDemografi(nik, name, birthDate)
            }
            .setNegativeButton("Review", null)
            .show()
    }

    private fun submitDemografi(nik: String, name: String, birthDate: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnContinue.isEnabled = false

            viewModel.repository.checkDemografi(name, nik, birthDate)
                .onSuccess {
                    binding.progressBar.visibility = View.GONE
                    binding.btnContinue.isEnabled = true

                    viewModel.nik = nik
                    viewModel.nama = name
                    viewModel.birthDate = birthDate

                    (activity as? EzsignEkycActivity)?.navigateToOtp()
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnContinue.isEnabled = true
                    Toast.makeText(context, e.message ?: "Verifikasi demografi gagal", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
