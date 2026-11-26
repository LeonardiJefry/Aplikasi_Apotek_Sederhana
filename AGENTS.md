# Instruksi agent Klinik Leonardo

Repo ini untuk web Klinik Leonardo, studi kasus Klinik Pratama Sehat Bersama, RSI Kelompok 1. Alur utama menghubungkan pasien, petugas loket, admin, dan dokter dari booking sampai pemeriksaan selesai. Gunakan ID UC/PC/UI persis seperti di Modul 2 dan 3.

## Mulai setiap tugas

1. Baca [ringkasan proyek](docs/PROJECT.md), [kontrak integrasi](docs/KONTRAK.md), dan issue GitHub yang diberikan. Pilih satu kartu di bawah sesuai anggota dan UC. Jika penugasan belum disebut, identifikasi pemilik dari issue atau minta ID tugas.
2. Cek `git status --short` dan `git branch --show-current`. Kerjakan satu issue di branch kartu. Pertahankan perubahan lokal yang sudah ada. Jika kontrak upstream belum tersedia, tulis dependensi di issue, siapkan UI dengan data uji lokal, dan lanjutkan bagian yang bisa diverifikasi.
3. Sebelum mengubah kode, sebutkan UC, branch, file yang akan disentuh, dan dependensi. Gunakan desain layar Modul 2 §2.8. Cocokkan dengan Hi-Fi RSI yang dipakai tim. Gunakan istilah Klinik Leonardo dan label status dari kontrak.

| Anggota | Use case | Kartu |
| --- | --- | --- |
| MF · M. Furqanurrahman | UC-01 Jadwal, UC-02 Booking | [MF](docs/agents/MF.md) |
| RA · Rakha Ahmad Fabiano | UC-03 Pantau antrean, UC-04 Batal | [RA](docs/agents/RA.md) |
| NA · Muhammad Naufal Alfarrel | UC-05 Check-in, UC-06 Lewati | [NA](docs/agents/NA.md) |
| JL · M. Jefry Leonardi | UC-07 Kuota vaksinasi, UC-08 Verifikasi pasien | [JL](docs/agents/JL.md) |
| AH · Affan Herlino Tazakha Wijaya | UC-09 Dashboard dokter, UC-10 Selesai | [AH](docs/agents/AH.md) |

## Aturan repo

- Untuk repo baru gunakan Java 21, Spring Boot, Spring MVC, Thymeleaf, Spring Security, Spring Data JPA, MySQL 8, dan Maven Wrapper. Struktur BCE per fitur. Boundary mengurus tampilan/input, controller memetakan permintaan, service menjalankan aturan dan transaksi, entity/repository menangani data. Ikuti build yang sudah ada pada repo berjalan. Usulkan perubahan stack melalui issue bersama.
- Satu model untuk setiap entitas bersama. Gunakan layanan/DTO lintas fitur di [kontrak](docs/KONTRAK.md). Ajukan perubahan skema, login, layout, status, atau kontrak melalui satu PR bersama review pemiliknya. JL mengoordinasikan migrasi.
- Ambil identitas dari sesi login, cek peran dan kepemilikan di server, aktifkan CSRF untuk form, serta pakai data sintetis dan konfigurasi lokal. Jangan masukkan identitas pasien asli atau kredensial ke repo, log, atau tangkapan layar PR.
- Jalankan `./mvnw test` atau `.\mvnw.cmd test` di Windows setelah fondasi tersedia. Coba alur sukses dan penolakan di browser dengan akun demo. Catat perintah dan hasil aktual.
- Laporkan perubahan, hasil cek, dependensi, dan langkah berikutnya di issue/PR. Serahkan hasil di branch fitur untuk direview. Merge ke branch utama dilakukan oleh tim.
