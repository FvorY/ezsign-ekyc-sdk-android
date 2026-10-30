# ezsign-ekyc-sdk-android

SDK Android native (Kotlin) untuk alur verifikasi identitas elektronik (eKYC) ezSign. Satu pemanggilan menjalankan seluruh alur: validasi data kependudukan, verifikasi OTP nomor handphone, active liveness wajah, sampai finalisasi data ke server ezSign.

SDK iOS dengan API, alur, teks, dan kode error yang sama ada di [ezsign-ekyc-sdk-ios](https://github.com/FvorY/ezsign-ekyc-sdk-ios).

## Daftar isi

- [Demo](#demo)
- [Fitur](#fitur)
- [Persyaratan](#persyaratan)
- [Komponen liveness engine](#komponen-liveness-engine)
- [Prasyarat lisensi liveness (wajib)](#prasyarat-lisensi-liveness-wajib)
- [Instalasi](#instalasi)
- [Mulai cepat](#mulai-cepat)
- [Referensi API](#referensi-api)
- [Alur verifikasi](#alur-verifikasi)
- [Arsitektur liveness](#arsitektur-liveness)
- [Endpoint server](#endpoint-server)
- [Penanganan hasil dan kode error](#penanganan-hasil-dan-kode-error)
- [Kode error liveness engine](#kode-error-liveness-engine)
- [Izin, manifest, dan ProGuard](#izin-manifest-dan-proguard)
- [Data dan privasi](#data-dan-privasi)
- [Keselarasan Android dan iOS](#keselarasan-android-dan-ios)
- [Aplikasi sample](#aplikasi-sample)
- [Troubleshooting](#troubleshooting)
- [Struktur proyek](#struktur-proyek)
- [Lisensi](#lisensi)

## Demo

APK dan IPA demo tersedia di halaman [Releases](https://github.com/FvorY/ezsign-ekyc-sdk-ios/releases) repositori iOS, versi terbaru [demo-ekyc-1.4](https://github.com/FvorY/ezsign-ekyc-sdk-ios/releases/tag/demo-ekyc-1.4).

1. Pasang `ezSign-eKYC-Android.apk`. Izinkan *Install unknown apps* bila diminta.
2. Login ke akun ezSign pada environment yang akan diuji, lalu salin token sesi dari `data.token` di respons login.
3. Buka aplikasi demo, pilih environment (DEV / STG / PROD) atau isi Base URL, tempel token, lalu ketuk **Mulai Verifikasi eKYC**.

> APK demo memakai package `com.sign.livenessdemo`, yang sudah terdaftar di lisensi liveness ezSign, dan bisa terpasang berdampingan dengan aplikasi ezSign resmi. Lihat [Prasyarat lisensi liveness](#prasyarat-lisensi-liveness-wajib).

## Fitur

- **Pemeriksaan status eKYC**: memastikan akun boleh melakukan eKYC (`canEkyc`) sebelum alur dimulai.
- **Validasi demografi**: NIK 16 digit, nama lengkap, dan tanggal lahir divalidasi ke server.
- **Verifikasi OTP**: nomor handphone dinormalkan ke format `+62…`, OTP dikirim lewat SMS dengan cooldown kirim ulang, lalu diverifikasi.
- **Active liveness**: layar instruksi persiapan, lalu deteksi wajah native.
- **Finalisasi otomatis**: mengirim hasil liveness, memperbarui biodata akun, me-refresh sesi, dan mengembalikan `regisStatus` dari server.
- **UI siap pakai**: tampilan mengikuti aplikasi ezSign (font Inter, warna, ilustrasi). SDK memakai tema terang sendiri sehingga tema aplikasi host tidak ikut berubah.
- **Hasil bertipe**: `EkycResult.Success`, `EkycResult.Failed`, atau `EkycResult.Cancelled`.

## Persyaratan

| Item | Nilai |
|---|---|
| Android minimum | API 24 (Android 7.0) |
| compileSdk / targetSdk SDK | 34 |
| Java / Kotlin JVM target | 17 |
| ABI | `arm64-v8a`, `armeabi-v7a` (library native liveness engine) |
| Orientasi | Portrait |
| Perangkat | Perangkat fisik dengan kamera depan (liveness tidak berjalan di emulator) |
| Jaringan | Akses HTTPS ke `baseUrl` ezSign dan ke layanan liveness (region Indonesia) |
| Akun | Token sesi akun ezSign yang valid di environment yang dipakai |

## Komponen liveness engine

| Komponen | Nilai |
|---|---|
| Library | `faceidv5/faceidv5.aar` (modul `:faceidv5`), liveness engine versi **5.7.21A** |
| Konversi | Referensi Android Support Library di AAR sudah dipetakan ke AndroidX; aplikasi host tidak perlu Jetifier |
| Model deteksi | `ekyc-core/src/main/res/raw/faceidmodel.bin` (dibaca otomatis oleh engine dari `res/raw`) |
| Resource layar deteksi | `liveness_*.png`, `liveness_*.m4a` (`raw`, `raw-en`), `livenessv5_sdk_strings.xml`, `livenessv5_sdk_colors.xml` (`values`, `values-en`, `values-id`). Engine mencarinya berdasarkan nama, jadi jangan diganti nama atau dihapus. |
| Bahasa layar deteksi | Inggris (`setLanguage("en")`) |
| Region layanan | Indonesia (sudah dikonfigurasi di SDK) |
| Mode integrasi | Normal (mode 0): lisensi dan konfigurasi liveness diambil otomatis oleh engine |

Liveness engine mendukung Android 5.0–14. SDK ini memakai batas yang lebih ketat, yaitu API 24.

> **16 KB page size.** `libfaceidlivenessv5.so` di versi 5.7.21A masih memakai alignment 4 KB. Google Play mewajibkan dukungan 16 KB page size untuk aplikasi yang menargetkan Android 15 (API 35) ke atas. Dukungan ini tersedia mulai liveness engine versi 5.8.5. Untuk aplikasi yang menargetkan API 35+, minta paket engine 5.8.5 ke tim ezSign lalu ganti `faceidv5.aar` dan resource-nya.

## Prasyarat lisensi liveness (wajib)

Lisensi liveness terikat pada **package name aplikasi** yang menjalankan SDK. Saat liveness dimulai, engine mengirim `bundle_id` (package name) dan signature APK ke server lisensi, lalu memasang lisensi dari respons server. Kalau package name belum terdaftar di lisensi liveness ezSign, liveness gagal sebelum kamera terbuka dengan kode `1003` (`AUTHENTICATION_FAIL{illegal_param}` atau `AUTHENTICATION_FAIL{bundleid_error}`).

Sebelum integrasi:

1. Tentukan `applicationId` final aplikasi Anda untuk debug dan release, termasuk suffix seperti `.debug` bila ada.
2. Minta tim ezSign mendaftarkan setiap `applicationId` tersebut ke lisensi liveness.
3. Untuk iOS, daftarkan juga Bundle ID aplikasinya.

Langkah demografi dan OTP tidak bergantung pada lisensi ini. Karena itu, masalah lisensi baru terlihat di layar **Active Liveness Check**.

## Instalasi

Liveness engine ikut di repo ini sebagai file `faceidv5/faceidv5.aar`, dibungkus dalam modul Gradle `:faceidv5`. `ekyc-core` bergantung pada modul tersebut, bukan langsung pada file AAR, karena library Android tidak boleh bergantung langsung pada file `.aar` lokal (build release akan gagal). SDK dipasang sebagai modul source, sehingga liveness engine ikut terpasang tanpa langkah manual.

1. Salin folder `ekyc-core` **dan** `faceidv5` ke root proyek Anda, atau tambahkan repo ini sebagai git submodule.
2. Daftarkan kedua modul di `settings.gradle.kts`:

   ```kotlin
   include(":faceidv5")
   include(":ekyc-core")
   // Jika letaknya di luar root proyek:
   // project(":faceidv5").projectDir = file("../ezsign-ekyc-sdk-android/faceidv5")
   // project(":ekyc-core").projectDir = file("../ezsign-ekyc-sdk-android/ekyc-core")
   ```

3. Tambahkan dependensi di `app/build.gradle.kts`:

   ```kotlin
   dependencies {
       implementation(project(":ekyc-core"))
   }
   ```

4. Pastikan version catalog proyek Anda punya alias yang dipakai `ekyc-core/build.gradle.kts` (lihat [`gradle/libs.versions.toml`](gradle/libs.versions.toml)). Cara lainnya, ganti alias tersebut dengan koordinat dependensi langsung.

5. Deklarasikan plugin Android Library di `build.gradle.kts` root proyek Anda. Proyek baru dari Android Studio biasanya hanya mendeklarasikan plugin application dan Kotlin, sehingga build gagal dengan `Error resolving plugin [id: 'com.android.library', …] … already on the classpath with an unknown version`:

   ```kotlin
   plugins {
       alias(libs.plugins.android.application) apply false
       alias(libs.plugins.android.library) apply false // tambahkan baris ini
       alias(libs.plugins.kotlin.android) apply false
   }
   ```

   Pastikan versi `agp` dan `kotlin` di catalog sama dengan yang dipakai aplikasi Anda.

Dengan cara ini, AAR liveness engine, model, resource layar deteksi, dan aturan R8 ikut terpasang otomatis, baik untuk build debug maupun release (termasuk dengan minify dan shrink resource).

## Mulai cepat

### Activity Result API (disarankan)

```kotlin
import io.github.fvory.ekyc.EzsignEkyc
import io.github.fvory.ekyc.model.EkycConfig
import io.github.fvory.ekyc.model.EkycResult

class MainActivity : AppCompatActivity() {

    private val ekycLauncher = registerForActivityResult(EzsignEkyc.Contract()) { result ->
        when (result) {
            is EkycResult.Success -> when (result.regisStatus) {
                "in_kyc", "reject_ra", "reject_ra_rekey_exp" -> restartEkyc()
                else -> openDashboard() // "wait_ra": menunggu persetujuan RA
            }
            is EkycResult.Failed -> showError("[${result.errorCode}] ${result.message}")
            EkycResult.Cancelled -> Unit // Pengguna menutup alur sebelum selesai
        }
    }

    private fun startVerification(sessionToken: String) {
        ekycLauncher.launch(
            EkycConfig(
                authToken = sessionToken,
                baseUrl = "https://api.ezsign.id/api"
            )
        )
    }
}
```

Panggil `registerForActivityResult` sebelum Activity/Fragment mencapai state `STARTED`, misalnya sebagai property seperti contoh di atas.

### `startActivityForResult` (API lama)

```kotlin
private const val REQUEST_CODE_EKYC = 1001

EzsignEkyc.start(this, EkycConfig(authToken = sessionToken), REQUEST_CODE_EKYC)

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE_EKYC) {
        when (val result = EzsignEkyc.parseResult(resultCode, data)) {
            is EkycResult.Success -> { /* ... */ }
            is EkycResult.Failed -> { /* ... */ }
            EkycResult.Cancelled -> { /* ... */ }
        }
    }
}
```

## Referensi API

### `EzsignEkyc`

| Anggota | Keterangan |
|---|---|
| `Contract()` | `ActivityResultContract<EkycConfig, EkycResult>` untuk Activity Result API. |
| `start(activity, config, requestCode = 1001)` | Membuka alur eKYC dengan `startActivityForResult`. |
| `parseResult(resultCode, data)` | Mengubah `Intent` hasil menjadi `EkycResult`. Mengembalikan `Cancelled` bila tidak ada data. |
| `EXTRA_RESULT` | Key extra `Intent` yang berisi `EkycResult` (Serializable). |

### `EkycConfig`

| Properti | Tipe | Default | Keterangan |
|---|---|---|---|
| `authToken` | `String` | wajib | Token sesi login ezSign (`data.token` dari respons login). Awalan `Bearer ` dan tanda kutip di sekelilingnya dibersihkan otomatis. |
| `baseUrl` | `String` | `https://gtm.myesign.id/api` | Base URL API ezSign. Garis miring di akhir dibuang dan `/api` ditambahkan bila belum ada. |
| `title` | `String` | `Your Identity` | Judul toolbar di layar pertama (Data Identitas). Layar berikutnya memakai judul tetap. |
| `primaryColorHex` | `String` | `#15C1E7` | Disediakan untuk kustomisasi warna. Saat ini belum diterapkan; UI memakai palet ezSign. |

Base URL per environment:

| Environment | Base URL |
|---|---|
| DEV | `https://gtm.myesign.id/api` |
| STG | `https://devapi.mesign.id/api` |
| PROD | `https://api.ezsign.id/api` |

Token harus diterbitkan oleh host yang sama dengan `baseUrl`. Token DEV tidak berlaku di STG/PROD, begitu juga sebaliknya.

### `EkycResult`

| Tipe | Properti | Kapan |
|---|---|---|
| `Success` | `message: String`, `regisStatus: String` | Semua langkah berhasil dan pengguna menekan **Finish**. Lihat tabel `regisStatus` di bawah. |
| `Failed` | `errorCode: String`, `message: String` | Alur berhenti karena error. Lihat [kode error](#penanganan-hasil-dan-kode-error). |
| `Cancelled` | — | Pengguna menutup alur (tombol close atau back di layar Data Identitas). |

Nilai `regisStatus` diambil dari respons `auth/user/refresh` setelah finalisasi, sama seperti aplikasi ezSign. Jika refresh gagal, nilainya `wait_ra`.

| `regisStatus` | Arti | Tindakan aplikasi host |
|---|---|---|
| `wait_ra` | Data menunggu persetujuan Registration Authority (RA). | Arahkan pengguna ke dashboard. |
| `in_kyc`, `reject_ra` | Server meminta data identitas diverifikasi ulang (rekey). | Jalankan alur eKYC lagi. |
| `reject_ra_rekey_exp` | Verifikasi ulang diperlukan karena masa berlaku data sudah habis. | Jalankan alur eKYC lagi. |
| lainnya | Status registrasi lain dari server. | Tangani sesuai aturan bisnis ezSign. |

## Alur verifikasi

```
Aplikasi host
  └─ EzsignEkyc.Contract().launch(config)
       1. Data Identitas  ── ekyc-status → checkdata → verifyNik → checkdemografi
       2. Verifikasi OTP  ── validatephone (kirim OTP) → verifyotp
       3. Active Liveness ── get-biz-token → liveness engine (kamera)
       4. Hasil           ── liveness-face-id → update-mobile → auth/user/refresh
  ◀─ EkycResult
```

1. **Data Identitas** (judul `config.title`): saat layar dibuka, SDK memeriksa `ekyc-status`. Jika `canEkyc` bernilai `false`, alur berakhir dengan `Failed("EKYC_NOT_ALLOWED")`. Pengguna mengisi NIK (16 digit), nama sesuai KTP, dan tanggal lahir, lalu mengonfirmasi data di dialog **Review Your Information**. Alurnya sama dengan aplikasi ezSign: tidak ada foto KTP maupun selfie terpisah.
2. **Verification Phone**: nomor handphone dinormalkan ke `+62…` (minimal 9 digit setelah `+62`). Setelah OTP terkirim lewat SMS, pengguna memasukkan 6 digit kode. Token dari respons `verifyotp` disimpan untuk dipakai saat finalisasi.
3. **Active Liveness Check**: menampilkan instruksi persiapan. Setelah izin kamera diberikan, SDK meminta `biz_token` dan `liveness_id` ke server ezSign, lalu membuka layar deteksi liveness.
4. **Verification**: SDK mengirim hasil liveness, memperbarui biodata akun beserta nomor handphone, lalu me-refresh sesi. `regisStatus` dari respons refresh dikembalikan ke aplikasi host.

Navigasi:

- Layar pertama memakai tombol close (X). Tombol close atau back sistem di layar ini mengakhiri alur dengan `Cancelled`.
- Di layar OTP dan liveness, tombol back kembali ke layar sebelumnya.
- Layar hasil tidak bisa ditinggalkan dengan back selama finalisasi berjalan. Alur berakhir lewat tombol **Finish** (sukses) atau **Close** (gagal).

## Arsitektur liveness

Autentikasi liveness terdiri dari lima langkah. Kredensial layanan liveness (`api_key`, `api_secret`) hanya ada di server ezSign, tidak pernah di aplikasi.

| Langkah | Dikerjakan oleh | Implementasi |
|---|---|---|
| 1. Membuat `sign` dari `api_key`/`api_secret` | Server ezSign | — |
| 2. `get_biz_token` | Server ezSign | SDK memanggil `register/user/get-biz-token`, respons berisi `biz_token` dan `liveness_id`. |
| 3. Preprocessing (lisensi dan konfigurasi) | Liveness engine di perangkat | Hasil di callback `onPreDetectFinish`. |
| 4. Deteksi wajah dan callback hasil | Liveness engine di perangkat | Hasil di callback `onDetectFinish`; kode selain `1000` ditangani sesuai [Kode error liveness engine](#kode-error-liveness-engine). |
| 5. Verify (perbandingan dan anti-spoofing) | Server ezSign | SDK memanggil `register/user/liveness-face-id` dengan `biz_token`, `verify_id = liveness_id`, `data_type = 0` (data SDK 5.x lewat `biz_token`), dan `encryption_type = 0`. |

Respons Verify (`result_code` seperti `1000 SUCCESS`, `2000 PASS_LIVING_NOT_THE_SAME`, `4100 FAIL_LIVING_FACE_ATTACK`/`VIDEO_LACK_FRAMES`, serta info risiko perangkat dan wajah) dievaluasi oleh server ezSign dan tidak diteruskan ke aplikasi. Aplikasi hanya menerima `code` dan `message` ezSign. Jika Verify tidak lolos, alur berakhir dengan `Failed("LIVENESS_SUBMIT_FAILED")` berisi pesan dari server.

## Endpoint server

Semua request memakai `POST` dengan header `Authorization: Bearer <authToken>`. Path di bawah relatif terhadap `baseUrl`.

| Langkah | Endpoint | Body |
|---|---|---|
| Status eKYC | `register/user/ekyc-status` | — |
| Cek data | `register/checkdata` | `nama`, `nik`, `birthDate` (`dd-MM-yyyy`) |
| Verifikasi NIK | `register/verifyNik` | `nik` |
| Cek demografi | `register/checkdemografi` | `nama`, `nik`, `birthDate` |
| Kirim OTP | `otp/validatephone` | `phone` (`+62…`), `otpType: 1`, `menu: "register_phone_number_validation"` |
| Verifikasi OTP | `register/user/verifyotp` | `phoneNumber` (`+62…`), `otp`, `token` |
| Biz token liveness | `register/user/get-biz-token` | — (respons berisi `biz_token`, `liveness_id`) |
| Submit liveness | `register/user/liveness-face-id` | multipart: `biz_token`, `verify_id`, `data_type: 0`, `encryption_type: 0` |
| Update profil | `register/update-mobile` | `nama`, `nik`, `birthDate`, `phone`, `otp`, `bizToken`, `token` |
| Refresh sesi | `auth/user/refresh` | — (respons berisi `regisStatus`) |

Respons dianggap berhasil jika HTTP 2xx dan field `code` bernilai `200` atau `0`. Jika tidak, `message` dari server ditampilkan ke pengguna.

## Penanganan hasil dan kode error

| `errorCode` | Penyebab | Saran penanganan |
|---|---|---|
| `INVALID_CONFIG` | `EkycConfig` tidak sampai ke Activity SDK. | Jalankan SDK lewat `EzsignEkyc.Contract()` atau `EzsignEkyc.start()`. |
| `EKYC_NOT_ALLOWED` | `ekyc-status` mengembalikan `canEkyc = false`, misalnya akun sudah eKYC, kuota habis, atau akun WNA. | Tampilkan `message` dan arahkan pengguna sesuai status akunnya. |
| `LIVENESS_SUBMIT_FAILED` | `liveness-face-id` ditolak, misalnya wajah tidak cocok, liveness tidak lolos, atau error server. | Beri pengguna kesempatan mengulang eKYC. |
| `UPDATE_PROFILE_FAILED` | `update-mobile` ditolak. | Tampilkan `message`, lalu ulangi atau hubungi dukungan. |

Error di langkah demografi, OTP, pembuatan biz token, dan deteksi liveness ditampilkan di layar masing-masing. Pengguna bisa mencoba lagi tanpa keluar dari alur, sehingga error ini tidak dikembalikan sebagai `Failed`.

## Kode error liveness engine

Kode berikut berasal dari callback `onPreDetectFinish` dan `onDetectFinish`. SDK menampilkannya dalam dialog **Liveness Check Failed** dengan pengelompokan pesan yang sama seperti aplikasi ezSign, diikuti kodenya, misalnya `(1016)`.

| Kode | Pesan engine | Arti | Pesan yang ditampilkan SDK |
|---|---|---|---|
| 1000 | `LIVENESS_FINISH` | Liveness selesai | — (lanjut ke finalisasi) |
| 1001 | `BIZ_TOKEN_DENIED` | `biz_token` tidak valid | Coba lagi* |
| 1002 | `ILLEGAL_PARAMETER:{…}` | Parameter tidak valid | Coba lagi* |
| 1003 | `AUTHENTICATION_FAIL:{…}` | Autentikasi lisensi gagal | Package name yang ditolak dan cara mendaftarkannya |
| 1004 | `MOBILE_PHONE_NOT_SUPPORT` | Perangkat tidak didukung | Perangkat tidak didukung** |
| 1005 | Nama exception | Exception internal engine | Coba lagi* |
| 1006 | `REQUEST_FREQUENTLY` | Pemanggilan bersamaan di perangkat yang sama | Coba lagi* |
| 1007 | `NETWORK_TIME_OUT` | Request jaringan timeout | Coba lagi* |
| 1008 | `INTERNAL_ERROR` | Error konfigurasi jaringan | Coba lagi* |
| 1009 | `INVALID_BUNDLE_ID` | Verifikasi informasi gagal | Perangkat tidak didukung** |
| 1010 | `NETWORK_ERROR` | Tidak ada koneksi internet | Coba lagi* |
| 1011 | `USER_CANCEL` | Pengguna membatalkan | `Liveness has been cancelled` (judul **Liveness Cancelled**) |
| 1012 | `NO_CAMERA_PERMISSION` | Tidak ada izin kamera | Minta izin kamera |
| 1013 | `DEVICE_NOT_SUPPORT` | Kamera tidak bisa dibuka | Perangkat tidak didukung** |
| 1014 | `FACE_INIT_FAIL` | Pengenalan wajah gagal dimulai | Coba lagi* |
| 1016 | `LIVENESS_FAILURE` | Liveness gagal | Coba lagi* |
| 1017 | `GO_TO_BACKGROUND` | Aplikasi masuk background | Coba lagi* |
| 1018 | `LIVENESS_TIME_OUT` | Pengguna tidak bergerak terlalu lama | Coba lagi* |
| 1019 | `DATA_UPLOAD_FAIL` | Upload data gagal | Coba lagi* |
| 1022–1026 | `…SCRN…`, `VIDEO_SAVE_FAIL`, `NO_AUDIO_RECORD_PERMISSION` | Perekaman layar/audio | Pesan engine apa adanya (fitur ini tidak dipakai SDK) |
| lainnya | — | Kode baru dari engine | Pesan engine apa adanya |

\* `We couldn't complete the liveness check. Please try again in a few moments.`
\** `Your device is currently not supported. Please try using a different device.`

Setelah dialog ditutup, pengguna tetap di layar **Active Liveness Check** dan bisa menekan **Start Liveness** lagi.

## Izin, manifest, dan ProGuard

Manifest SDK digabung otomatis ke aplikasi host dan sudah mendeklarasikan:

- Izin `CAMERA`, `INTERNET`, dan `ACCESS_NETWORK_STATE`.
- `EzsignEkycActivity` dan activity layar deteksi liveness (portrait, tidak di-export).

Izin kamera diminta saat runtime ketika pengguna menekan **Start Liveness**, sebelum layar deteksi dibuka.

SDK tidak memakai fitur perekaman layar liveness, sehingga service perekaman layar dan izin `FOREGROUND_SERVICE_MEDIA_PROJECTION` tidak diperlukan.

Aturan ProGuard/R8 untuk SDK dan liveness engine ikut terpasang otomatis lewat `ekyc-core/consumer-rules.pro`.

## Data dan privasi

- NIK, nama, tanggal lahir, nomor handphone, dan OTP hanya dikirim ke `baseUrl` ezSign lewat HTTPS dan tidak disimpan di perangkat setelah alur selesai.
- Video dan foto wajah diambil dan dikirim oleh liveness engine langsung ke layanan liveness. SDK ezSign tidak menyimpan file liveness lokal (callback `onLivenessFileCallback` dan `onLivenessLocalFileCallBack` diabaikan).
- SDK tidak menulis log berisi token, data identitas, maupun body HTTP.

## Keselarasan Android dan iOS

Kedua SDK memakai alur, endpoint, teks layar, aturan validasi, kode `errorCode`, nilai `regisStatus`, dan pemetaan kode error liveness engine yang sama. Perbedaannya hanya pada hal yang memang khas platform:

| Aspek | Android | iOS |
|---|---|---|
| Entry point | `EzsignEkyc.Contract()` / `EzsignEkyc.start()` | `EzsignEkyc.start(from:config:completion:)` |
| Hasil | `EkycResult.Success` / `Failed` / `Cancelled` | `.success` / `.failure` / `.cancelled` |
| Liveness engine | `faceidv5.aar` 5.7.21A | Framework liveness statis (build Desember 2024) |
| Identitas lisensi | `applicationId` | Bundle ID |
| Izin kamera | Runtime permission | `NSCameraUsageDescription` + permintaan izin sistem |
| Deteksi posisi tegak perangkat | Bawaan engine | Dinonaktifkan (`phoneVertical = Disable`), sama seperti aplikasi ezSign |
| Kode `INVALID_CONFIG` | Ada (Activity tanpa config) | Tidak ada (config wajib di API) |
| Kode `INCOMPLETE` | Tidak ada | Ada (layar hasil ditutup sebelum finalisasi selesai) |

## Aplikasi sample

Modul `sample` adalah aplikasi simulasi fintech yang memanggil SDK.

```bash
./gradlew :sample:assembleDebug
```

Sample memakai `applicationId` `com.sign.livenessdemo`, yang sudah terdaftar di lisensi liveness ezSign. Lisensi liveness terikat pada package name, jadi untuk menguji dengan package lain, gunakan parameter `ekycApplicationId`:

```bash
./gradlew :sample:assembleDebug -PekycApplicationId=<package terdaftar>
```

Unit test SDK:

```bash
./gradlew :ekyc-core:testDebugUnitTest
```

## Troubleshooting

**Dialog "Liveness Check Failed" berisi "The liveness license rejected …"**
SDK menampilkan dialog seperti ini:

```
The liveness license rejected com.example.myapp. Register this package name with
the ezSign liveness license. (AUTHENTICATION_FAIL{illegal_param}) (1003)
```

Package name yang tertulis di dialog belum terdaftar di lisensi liveness ezSign. Kode di dalam kurung bisa berupa `AUTHENTICATION_FAIL{illegal_param}` atau `bundleid_error`. Daftarkan package name tersebut, atau build sample dengan `-PekycApplicationId=<package terdaftar>`.

**Dialog berisi kode `(1014)` (`FACE_INIT_FAIL`)**
Kamera deteksi gagal dibuka. Penyebab yang paling umum adalah `faceidv5.aar` versi asli dari vendor yang masih memakai Android Support Library: logcat menampilkan `NoClassDefFoundError: Failed resolution of: Landroid/support/v4/app/ActivityCompat;`. Pakai `faceidv5.aar` dari repo ini, yang sudah dikonversi ke AndroidX, atau aktifkan `android.enableJetifier=true` bila Anda harus memakai AAR asli. Pastikan juga kamera tidak sedang dipakai aplikasi lain.

**Layar deteksi langsung tertutup dengan kode `(1016)` (`LIVENESS_FAILURE`)**
Jika layar deteksi tertutup kurang dari sedetik setelah terbuka, resource layar deteksi tidak ikut terpasang; logcat menampilkan `Invalid resource ID 0x00000000`. Resource ini (`liveness_*.png`, `liveness_*.m4a`, `livenessv5_sdk_*.xml`) tidak termasuk dalam `faceidv5.aar` dan sudah disertakan di `ekyc-core/src/main/res`. Pastikan modul `ekyc-core` dipasang utuh tanpa menghapus folder `res`. Jika layar sempat berjalan, kode ini berarti pengguna tidak lolos deteksi; minta pengguna mengikuti instruksi di layar persiapan lalu mencoba lagi.

**Upload ke Google Play ditolak karena 16 KB page size**
Library native liveness engine 5.7.21A belum mendukung 16 KB page size. Lihat catatan di [Komponen liveness engine](#komponen-liveness-engine).

**`Unauthorized` / `JWT validation failed`**
Token sudah kedaluwarsa atau diterbitkan oleh environment lain. Login ulang di host yang sama dengan `baseUrl`, lalu pakai `data.token` dari respons login. Jangan memakai token dari respons OTP sebagai `authToken`.

**Kamera tidak terbuka atau liveness tidak muncul di emulator**
Liveness memerlukan kamera fisik dan library native ARM (`arm64-v8a`, `armeabi-v7a`). Uji di perangkat fisik.

**Build release gagal: `Direct local .aar file dependencies are not supported when building an AAR`**
Modul library di proyek Anda bergantung langsung pada file `.aar` lokal, misalnya `implementation(files("libs/faceidv5.aar"))` di dalam modul library. Pakai modul `:faceidv5` dari repo ini seperti dijelaskan di [Instalasi](#instalasi).

**`NoClassDefFoundError` untuk class liveness engine**
Liveness engine tidak ikut terpasang. Pastikan `include(":faceidv5")` ada di `settings.gradle.kts` dan folder `faceidv5` ikut disalin ke proyek.

**Pesan "Akun tidak dapat melakukan eKYC"**
Server menyatakan akun tidak eligible (`canEkyc = false`). Aplikasi host menerima `Failed("EKYC_NOT_ALLOWED")`.

## Struktur proyek

```
faceidv5/                      Modul pembungkus liveness engine
  faceidv5.aar                 Liveness engine 5.7.21A (dikonversi ke AndroidX)
ekyc-core/                     Library SDK
  src/main/java/io/github/fvory/ekyc/
    EzsignEkyc.kt              Entry point publik
    model/EkycModels.kt        EkycConfig, EkycResult
    api/                       Retrofit service, repository, model API
    ui/                        Activity, fragment per langkah, ViewModel
    util/PhoneNumber.kt        Normalisasi nomor handphone +62
  src/main/res/raw/faceidmodel.bin                Model deteksi wajah
  src/main/res/**/liveness_*, livenessv5_sdk_*    Gambar, audio panduan, teks, dan warna layar deteksi
  src/test/                    Unit test
sample/                        Aplikasi simulasi
```

## Lisensi

Kode SDK didistribusikan di bawah lisensi MIT; lihat [`LICENSE`](LICENSE). Liveness engine beserta resource-nya (`faceidv5.aar`, `faceidmodel.bin`, `liveness_*`, `livenessv5_sdk_*`) adalah komponen pihak ketiga yang tunduk pada perjanjian lisensi tersendiri.
