# Klinik Leonardo — Aplikasi Apotek Sederhana

Web Klinik Pratama Sehat Bersama, RSI Kelompok 1. Java 21, Spring Boot, Thymeleaf, Spring Security dan Spring Data JPA dengan pola BCE. Jefry (JL) mengerjakan seluruh tahap secara berurutan.

## Jalankan dari Git Bash

Pasang JDK 21, pastikan `java -version` serta `JAVA_HOME` sesuai, lalu dari folder repo:

```bash
./mvnw --batch-mode --no-transfer-progress verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Buka [http://localhost:8080/login](http://localhost:8080/login). Profil default juga `demo`, sehingga `./mvnw spring-boot:run` memakai H2 tanpa server database, Docker, atau file `.env`. Unduhan Maven/dependensi diperlukan pada build pertama.

| Akun sintetis | Peran |
| --- | --- |
| `pasien`, `pasien2` | Pasien |
| `loket` | Petugas loket |
| `admin` | Admin |
| `dokter`, `dokter2` | Dokter |

Password demo lokal: `demo-klinik-2026`. Ini fixture publik sintetis. Untuk menggantinya pada proses demo, `export DEMO_LOCAL_PASSWORD='password_demo_minimal_12_karakter'` sebelum menjalankan aplikasi. Database H2 hanya berada di memori dan kembali ke fixture awal setelah JVM berhenti. Server demo mendengarkan di `127.0.0.1`.

## Profil dan status

| Profil | Database dan konfigurasi |
| --- | --- |
| `demo` (default) | H2 runtime, Flyway V1-V5, seed sintetis; tidak membaca `.env` |
| `test` | H2 terpisah untuk tes; tidak membaca `.env` |
| `mysql` (opsional historis) | Membaca `.env` lama; tidak dibutuhkan dalam alur harian/CI |
| Supabase | Belum diimplementasikan; DB-01 setelah QA-01 lulus dan masuk main |

SETUP-00 sampai UC-07 terintegrasi melalui PR #6 sampai #13. Login sebagai pasien, buka `/pasien/jadwal`, pilih tanggal serta poli/dokter, lalu Pilih Jadwal untuk booking. **Antrean Saya** menampilkan status dan pembatalan. **Check-in hari ini** mengubah tiket Terdaftar menjadi Hadir/Menunggu selama jendela waktu yang valid, tanpa mengubah kuota. Loket memiliki **Peringatan Check-in** dan **Panggilan Antrean** UC-06: timer server 120 detik, konfirmasi hadir/lewati, riwayat, panggilan berikutnya dan pengembalian kuota pada pelewatan ketiga. UC-07 menambah **Kelola kuota vaksinasi** untuk admin pada `/admin/vaksinasi`: pilih tanggal/jenis, ubah kuota minimal sebesar peserta terdaftar, tutup saat Penuh dan buka kembali saat tersedia. Halaman publik `/vaksinasi` menampilkan angka agregat/status terbaru tanpa login. UC-08 menambah **Cari dan verifikasi pasien** pada /loket/pasien: pencarian NIK/noRM/nama, pilihan pasien, riwayat dan audit petugas/waktu server. UI-09 sampai UI-10 menunggu tahap berikutnya. Lihat [PROGRESS.md](PROGRESS.md) untuk status review/verifikasi aktual, [SETUP.md](SETUP.md) untuk langkah lengkap, [roadmap](docs/PROJECT.md), [kontrak](docs/KONTRAK.md), serta [arsitektur BCE](docs/ARSITEKTUR.md).

## Pemeriksaan

```bash
./mvnw --batch-mode --no-transfer-progress verify
python scripts/smoke_demo.py target/apotek-0.0.1-SNAPSHOT.jar
```

`verify` menjalankan tes migrasi, constraint, login, peran, CSRF, kepemilikan, UC-01 sampai UC-08, submit bersamaan, batas waktu dan rollback, lalu membuat JAR. Tes check-in dan panggilan juga memakai HTTP Tomcat sungguhan dengan Clock terkendali. Skrip Python standar memulai JAR dari folder sementara tanpa `.env`, memeriksa enam akun sintetis dan UC-01 sampai UC-08, kemudian mengulangi startup di JVM baru. Skrip JAR mengikuti waktu server sebenarnya: di luar jendela/jam layanan, penolakan dan tidak berubahnya data yang diverifikasi. CI menjalankan kedua perintah dengan JDK 21 tanpa service database. Skrip HTTP melengkapi pemeriksaan visual browser.

## Cara kerja

Satu tahap per branch sesuai [roadmap](docs/PROJECT.md). Pertahankan perubahan lokal; Jefry melakukan commit, push, PR dan merge. UC-08 dikerjakan setelah UC-07 masuk main melalui PR #13 dan Java CI lulus. UC-09 menunggu UC-08 terverifikasi dan terintegrasi. Status working tree belum berarti perubahan telah terintegrasi. Ikuti [CONTRIBUTING.md](CONTRIBUTING.md).

Migrasi MySQL historis V1/V2 tetap utuh. Pada H2, placeholder opsi engine/kolasi dikosongkan dan Hibernate memakai `ddl-auto=validate`. Migrasi PostgreSQL khusus disiapkan pada DB-01 di lokasi terpisah.
