# ezsign-ekyc-sdk-android

SDK Android native (Kotlin) untuk alur verifikasi identitas elektronik (eKYC) ezSign. Satu pemanggilan menjalankan seluruh alur: validasi data kependudukan, verifikasi OTP nomor handphone, active liveness wajah dengan Megvii FaceID LV5, sampai finalisasi data ke server ezSign.

SDK iOS dengan API dan alur yang sama ada di [ezsign-ekyc-sdk-ios](https://github.com/FvorY/ezsign-ekyc-sdk-ios).

## Daftar isi

- [Demo](#demo)
- [Fitur](#fitur)
- [Persyaratan](#persyaratan)
- [Prasyarat lisensi liveness (wajib)](#prasyarat-lisensi-liveness-wajib)
- [Instalasi](#instalasi)
- [Mulai cepat](#mulai-cepat)
- [Referensi API](#referensi-api)
- [Alur verifikasi](#alur-verifikasi)
- [Endpoint server](#endpoint-server)
- [Penanganan hasil dan kode error](#penanganan-hasil-dan-kode-error)
- [Izin, manifest, dan ProGuard](#izin-manifest-dan-proguard)
- [Aplikasi sample](#aplikasi-sample)
- [Troubleshooting](#troubleshooting)
- [Struktur proyek](#struktur-proyek)
- [Lisensi](#lisensi)

## Demo

APK dan IPA demo tersedia di halaman [Releases](https://github.com/FvorY/ezsign-ekyc-sdk-ios/releases) repositori iOS, versi terbaru [demo-ekyc-1.2](https://github.com/FvorY/ezsign-ekyc-sdk-ios/releases/tag/demo-ekyc-1.2).

1. Pasang `ezSign-eKYC-Android.apk`. Izinkan *Install unknown apps* bila diminta.
2. Login ke akun ezSign pada environment yang akan diuji, lalu salin token sesi dari `data.token` di respons login.
3. Buka aplikasi demo, pilih environment (DEV / STG / PROD) atau isi Base URL, tempel token, lalu ketuk **Mulai Verifikasi eKYC**.

> Langkah liveness pada APK demo baru bisa berjalan setelah package name demo terdaftar di lisensi Megvii. Lihat [Prasyarat lisensi liveness](#prasyarat-lisensi-liveness-wajib).

## Fitur

- **Pemeriksaan status eKYC**: memastikan akun boleh melakukan eKYC (`canEkyc`) sebelum alur dimulai.
- **Validasi demografi**: NIK 16 digit, nama lengkap, dan tanggal lahir divalidasi ke server.
- **Verifikasi OTP**: mengirim OTP lewat SMS dengan cooldown kirim ulang, lalu memverifikasi kodenya.
- **Active liveness**: layar instruksi persiapan, lalu deteksi wajah native Megvii FaceID LV5.
- **Finalisasi otomatis**: mengirim hasil liveness, memperbarui biodata akun, dan me-refresh sesi.
- **UI siap pakai**: tampilan mengikuti aplikasi ezSign (font Inter, warna, ilustrasi). SDK memakai tema terang sendiri sehingga tema aplikasi host tidak ikut berubah.
- **Hasil bertipe**: `EkycResult.Success`, `EkycResult.Failed`, atau `EkycResult.Cancelled`.

## Persyaratan

| Item | Nilai |
|---|---|
| Android minimum | API 24 (Android 7.0) |
| compileSdk / targetSdk | 34 |
| Java / Kotlin JVM target | 17 |
| Orientasi | Portrait |
| Perangkat | Perangkat fisik dengan kamera depan (liveness tidak berjalan di emulator) |
| Akun | Token sesi akun ezSign yang valid di environment yang dipakai |

## Prasyarat lisensi liveness (wajib)

Lisensi Megvii FaceID terikat pada **package name aplikasi** yang menjalankan SDK. Saat liveness dimulai, SDK Megvii mengirim `bundle_id` (package name) dan signature APK ke server Megvii, lalu memasang lisensi dari respons server. Kalau package name belum terdaftar di akun Megvii ezSign, liveness gagal sebelum kamera terbuka, dengan pesan seperti:

```
AUTHENTICATION_FAIL{illegal_param}
```

Sebelum integrasi:

1. Tentukan `applicationId` final aplikasi Anda untuk debug dan release, termasuk suffix seperti `.debug` bila ada.
2. Minta tim ezSign mendaftarkan setiap `applicationId` tersebut ke lisensi Megvii FaceID.
3. Untuk iOS, daftarkan juga Bundle ID aplikasinya.

Langkah demografi dan OTP tidak bergantung pada lisensi ini. Karena itu, masalah lisensi baru terlihat di layar **Active Liveness Check**.

## Instalasi

Library Megvii ikut di repo ini sebagai file lokal `ekyc-core/libs/faceidv5.aar`. Gradle tidak menyertakan file AAR lokal ke artefak library yang dipublikasikan, jadi aplikasi host perlu mendapatkan AAR tersebut lewat salah satu cara berikut.

### Opsi A: modul source (disarankan)

1. Salin folder `ekyc-core` ke root proyek Anda, atau tambahkan repo ini sebagai git submodule.
2. Daftarkan modulnya di `settings.gradle.kts`:

   ```kotlin
   include(":ekyc-core")
   // Jika letaknya di luar root proyek:
   // project(":ekyc-core").projectDir = file("../ezsign-ekyc-sdk-android/ekyc-core")
   ```

3. Tambahkan dependensi di `app/build.gradle.kts`:

   ```kotlin
   dependencies {
       implementation(project(":ekyc-core"))
   }
   ```

4. Pastikan version catalog proyek Anda punya alias yang dipakai `ekyc-core/build.gradle.kts` (lihat [`gradle/libs.versions.toml`](gradle/libs.versions.toml)). Cara lainnya, ganti alias tersebut dengan koordinat dependensi langsung.

### Opsi B: JitPack

1. Tambahkan repositori JitPack di `settings.gradle.kts`:

   ```kotlin
   dependencyResolutionManagement {
       repositories {
           google()
           mavenCentral()
           maven { url = uri("https://jitpack.io") }
       }
   }
   ```

2. Salin `ekyc-core/libs/faceidv5.aar` ke `app/libs/`, lalu tambahkan SDK dan library Megvii di `app/build.gradle.kts`:

   ```kotlin
   dependencies {
       implementation("com.github.FvorY:ezsign-ekyc-sdk-android:<tag>")
       implementation(files("libs/faceidv5.aar"))
   }
   ```

   Ganti `<tag>` dengan tag rilis yang tersedia di repo ini.

## Mulai cepat

### Activity Result API (disarankan)

```kotlin
import io.github.fvory.ekyc.EzsignEkyc
import io.github.fvory.ekyc.model.EkycConfig
import io.github.fvory.ekyc.model.EkycResult

class MainActivity : AppCompatActivity() {

    private val ekycLauncher = registerForActivityResult(EzsignEkyc.Contract()) { result ->
        when (result) {
            is EkycResult.Success -> {
                // result.regisStatus bernilai "wait_ra": data menunggu persetujuan RA
                showMessage(result.message)
            }
            is EkycResult.Failed -> {
                showError("[${result.errorCode}] ${result.message}")
            }
            EkycResult.Cancelled -> {
                // Pengguna menutup alur sebelum selesai
            }
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
| `title` | `String` | `Verifikasi Identitas (eKYC)` | Judul toolbar di layar pertama (Data Identitas). Layar berikutnya memakai judul tetap. |
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
| `Success` | `message: String`, `regisStatus: String` | Semua langkah berhasil dan pengguna menekan **Selesai**. Saat ini `regisStatus` bernilai `wait_ra`. |
| `Failed` | `errorCode: String`, `message: String` | Alur berhenti karena error. Lihat [kode error](#penanganan-hasil-dan-kode-error). |
| `Cancelled` | — | Pengguna menutup alur atau menekan back sebelum finalisasi. |

## Alur verifikasi

```
Aplikasi host
  └─ EzsignEkyc.Contract().launch(config)
       1. Data Identitas  ── ekyc-status → checkdata → verifyNik → checkdemografi
       2. Verifikasi OTP  ── validatephone (kirim OTP) → verifyotp
       3. Active Liveness ── get-biz-token → Megvii FaceID (kamera)
       4. Hasil           ── liveness-face-id → update-mobile → auth/user/refresh
  ◀─ EkycResult
```

1. **Data Identitas**: saat layar dibuka, SDK memeriksa `ekyc-status`. Jika `canEkyc` bernilai `false`, alur berakhir dengan `Failed("EKYC_NOT_ALLOWED")`. Pengguna lalu mengisi NIK (16 digit), nama sesuai KTP, dan tanggal lahir.
2. **Verifikasi OTP**: pengguna memasukkan nomor handphone, menerima OTP lewat SMS, lalu memasukkan kodenya. Token dari respons `verifyotp` disimpan untuk dipakai saat finalisasi.
3. **Active Liveness Check**: menampilkan instruksi persiapan. Setelah izin kamera diberikan, SDK meminta `biz_token` dan `liveness_id` ke server ezSign, lalu membuka deteksi Megvii.
4. **Hasil**: SDK mengirim hasil liveness, memperbarui biodata akun beserta nomor handphone, lalu me-refresh sesi. Status akhir `wait_ra` berarti data menunggu persetujuan Registration Authority (RA).

## Endpoint server

Semua request memakai `POST` dengan header `Authorization: Bearer <authToken>`. Path di bawah relatif terhadap `baseUrl`.

| Langkah | Endpoint | Body |
|---|---|---|
| Status eKYC | `register/user/ekyc-status` | — |
| Cek data | `register/checkdata` | `nama`, `nik`, `birthDate` (`dd-MM-yyyy`) |
| Verifikasi NIK | `register/verifyNik` | `nik` |
| Cek demografi | `register/checkdemografi` | `nama`, `nik`, `birthDate` |
| Kirim OTP | `otp/validatephone` | `phone`, `otpType: 1`, `menu: "register_phone_number_validation"` |
| Verifikasi OTP | `register/user/verifyotp` | `phoneNumber`, `otp`, `token` |
| Biz token liveness | `register/user/get-biz-token` | — (respons berisi `biz_token`, `liveness_id`) |
| Submit liveness | `register/user/liveness-face-id` | multipart: `biz_token`, `verify_id`, `data_type`, `encryption_type` |
| Update profil | `register/update-mobile` | `nama`, `nik`, `birthDate`, `phone`, `otp`, `bizToken`, `token` |
| Refresh sesi | `auth/user/refresh` | — |

Respons dianggap berhasil jika HTTP 2xx dan field `code` bernilai `200` atau `0`. Jika tidak, `message` dari server ditampilkan ke pengguna.

Deteksi wajah memakai host Megvii `https://api-idn.megvii.com`.

## Penanganan hasil dan kode error

| `errorCode` | Penyebab | Saran penanganan |
|---|---|---|
| `INVALID_CONFIG` | `EkycConfig` tidak sampai ke Activity SDK. | Jalankan SDK lewat `EzsignEkyc.Contract()` atau `EzsignEkyc.start()`. |
| `EKYC_NOT_ALLOWED` | `ekyc-status` mengembalikan `canEkyc = false`, misalnya akun sudah eKYC, kuota habis, atau akun WNA. | Tampilkan `message` dan arahkan pengguna sesuai status akunnya. |
| `LIVENESS_SUBMIT_FAILED` | `liveness-face-id` ditolak, misalnya wajah tidak cocok, liveness tidak lolos, atau error server. | Beri pengguna kesempatan mengulang eKYC. |
| `UPDATE_PROFILE_FAILED` | `update-mobile` ditolak. | Tampilkan `message`, lalu ulangi atau hubungi dukungan. |

Error di langkah demografi, OTP, dan pembuatan biz token ditampilkan di layar masing-masing. Pengguna bisa mencoba lagi tanpa keluar dari alur, sehingga error ini tidak dikembalikan sebagai `Failed`.

Kode hasil Megvii yang ditangani di layar liveness:

| Kode | Arti |
|---|---|
| `1000` | Berhasil, lanjut ke finalisasi. |
| `1011` | Pengguna membatalkan deteksi wajah. Pengguna tetap di layar instruksi dan bisa mencoba lagi. |
| lainnya | Ditampilkan sebagai pesan error. `AUTHENTICATION_FAIL{...}` berarti package name belum terdaftar di lisensi Megvii. |

## Izin, manifest, dan ProGuard

Manifest SDK digabung otomatis ke aplikasi host dan sudah mendeklarasikan:

- Izin `CAMERA`, `INTERNET`, dan `ACCESS_NETWORK_STATE`.
- `EzsignEkycActivity` dan activity Megvii FaceID (portrait, tidak di-export).

Izin kamera diminta saat runtime ketika pengguna menekan **Start Liveness**.

Aturan ProGuard/R8 ikut terpasang lewat `consumer-rules.pro`:

```proguard
-keep class com.megvii.** { *; }
-keep class com.meglive.** { *; }
-keep class io.github.fvory.ekyc.** { *; }
```

Jika library Megvii ditambahkan manual (Opsi B), pastikan aturan `com.megvii` di atas juga ada di konfigurasi R8 aplikasi.

## Aplikasi sample

Modul `sample` adalah aplikasi simulasi fintech yang memanggil SDK.

```bash
./gradlew :sample:assembleDebug
```

Lisensi Megvii terikat pada package name. Agar langkah liveness bisa diuji, build dengan `applicationId` yang sudah terdaftar:

```bash
./gradlew :sample:assembleDebug -PekycApplicationId=com.sign.ezsign
```

Tanpa parameter tersebut, sample memakai `io.github.fvory.ekyc.sample`. APK yang dibuild dengan `com.sign.ezsign` tidak bisa dipasang berdampingan dengan aplikasi ezSign resmi karena sertifikat penandatangannya berbeda. Hapus dulu aplikasi ezSign resmi dari perangkat uji.

Unit test SDK:

```bash
./gradlew :ekyc-core:testDebugUnitTest
```

## Troubleshooting

**Dialog "Liveness tidak dapat dimulai" saat menekan Start Liveness**
SDK menampilkan dialog seperti ini:

```
Lisensi liveness menolak aplikasi io.github.fvory.ekyc.sample. Package name ini harus
didaftarkan pada akun Megvii ezSign. (AUTHENTICATION_FAIL{illegal_param})
```

Package name yang tertulis di dialog belum terdaftar di lisensi Megvii ezSign. Kode di dalam kurung bisa berupa `AUTHENTICATION_FAIL{illegal_param}` atau `bundleid_error`. Daftarkan package name tersebut, atau build sample dengan `-PekycApplicationId=<package terdaftar>`.

**`Unauthorized` / `JWT validation failed`**
Token sudah kedaluwarsa atau diterbitkan oleh environment lain. Login ulang di host yang sama dengan `baseUrl`, lalu pakai `data.token` dari respons login. Jangan memakai token dari respons OTP sebagai `authToken`.

**Kamera tidak terbuka atau liveness tidak muncul di emulator**
Liveness memerlukan kamera fisik dan library native ARM (`arm64-v8a`, `armeabi-v7a`). Uji di perangkat fisik.

**`NoClassDefFoundError: com/megvii/...`**
Library Megvii tidak ikut terpasang. Pada Opsi B, tambahkan `implementation(files("libs/faceidv5.aar"))` di modul aplikasi.

**Pesan "Akun tidak dapat melakukan eKYC"**
Server menyatakan akun tidak eligible (`canEkyc = false`). Aplikasi host menerima `Failed("EKYC_NOT_ALLOWED")`.

## Struktur proyek

```
ekyc-core/                     Library SDK
  libs/faceidv5.aar            Megvii FaceID LV5
  src/main/java/io/github/fvory/ekyc/
    EzsignEkyc.kt              Entry point publik
    model/EkycModels.kt        EkycConfig, EkycResult
    api/                       Retrofit service, repository, model API
    ui/                        Activity, fragment per langkah, ViewModel
  src/main/res/raw/faceidmodel.bin   Model deteksi wajah Megvii
sample/                        Aplikasi simulasi
```

## Lisensi

Kode SDK didistribusikan di bawah lisensi MIT; lihat [`LICENSE`](LICENSE). Library Megvii FaceID (`faceidv5.aar`, `faceidmodel.bin`) tunduk pada perjanjian lisensi Megvii yang terpisah.
