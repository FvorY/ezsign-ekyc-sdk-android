# ezsign-ekyc-sdk-android

Native Android SDK (Kotlin) untuk alur verifikasi identitas elektronik (eKYC) ezSign, mencakup verifikasi demografi kependudukan, validasi OTP SMS, serta integrasi Megvii FaceID Active Liveness Detection.

## Fitur

* **Pemeriksaan Status eKYC:** Validasi eligibility akun dan kuota verifikasi.
* **Verifikasi Data Demografi:** Validasi NIK 16-digit, nama lengkap, dan tanggal lahir secara berurutan (`checkdata`, `verifyNik`, `checkdemografi`).
* **Verifikasi OTP Telepon:** Pengiriman kode OTP via SMS dengan timer cooldown dan verifikasi kode.
* **Active Liveness Detection:** Instruksi persiapan wajah dan deteksi keaktifan wajah native menggunakan Megvii FaceID LV5 SDK.
* **Penyelesaian Otomatis:** Submit hasil liveness dan update biodata akun ke server ezSign.

## Instalasi

### Gradle (JitPack)

Tambahkan repository JitPack pada `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Tambahkan dependensi pada `build.gradle.kts` modul aplikasi Anda:

```kotlin
dependencies {
    implementation("com.github.FvorY:ezsign-ekyc-sdk-android:1.0.0")
}
```

## Penggunaan

### Menggunakan Activity Result API (Disarankan)

```kotlin
class MainActivity : AppCompatActivity() {

    private val ekycLauncher = registerForActivityResult(EzsignEkyc.Contract()) { result ->
        when (result) {
            is EkycResult.Success -> {
                Log.d("eKYC", "Status: ${result.regisStatus}, Message: ${result.message}")
            }
            is EkycResult.Failed -> {
                Log.e("eKYC", "Error [${result.errorCode}]: ${result.message}")
            }
            is EkycResult.Cancelled -> {
                Log.d("eKYC", "Dibatalkan oleh pengguna")
            }
        }
    }

    private fun startVerification(authToken: String) {
        val config = EkycConfig(
            authToken = authToken,
            baseUrl = "https://api.ezsign.id",
            title = "Verifikasi Identitas"
        )
        ekycLauncher.launch(config)
    }
}
```

### Menggunakan `startActivityForResult`

```kotlin
val config = EkycConfig(
    authToken = "YOUR_BEARER_TOKEN",
    baseUrl = "https://api.ezsign.id"
)
EzsignEkyc.start(this, config, REQUEST_CODE_EKYC)

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE_EKYC) {
        val result = EzsignEkyc.parseResult(resultCode, data)
        // Handle result
    }
}
```

## Persyaratan Minimum

* Android SDK minimum: API 24 (Android 7.0)
* Target SDK: API 34+
* Izin Kamera (`android.permission.CAMERA`)
* Izin Internet (`android.permission.INTERNET`)

## Lisensi

Didistribusikan di bawah lisensi MIT. Lihat `LICENSE` untuk informasi lebih lanjut.
