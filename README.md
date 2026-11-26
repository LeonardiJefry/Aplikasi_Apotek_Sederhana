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
| `demo` (default) | H2 runtime, Flyway V1/V2, seed sintetis; tidak membaca `.env` |
| `test` | H2 terpisah untuk tes; tidak membaca `.env` |
| `mysql` (opsional historis) | Membaca `.env` lama; tidak dibutuhkan dalam alur harian/CI |
| Supabase | Belum diimplementasikan; DB-01 setelah QA-01 lulus dan masuk main |

SETUP-00, UC-01 dan UC-02 terintegrasi melalui PR #6, #7 dan #8. Login sebagai pasien, buka `/pasien/jadwal`, pilih tanggal serta poli/dokter, lalu Pilih Jadwal untuk ringkasan dan konfirmasi booking. Untuk percobaan di luar jam praktik, pilih besok dan Poli Umum. UC-03 menyediakan **Antrean Saya** pada `/pasien/antrean`: pilih tiket sendiri, lihat status, nomor sedang dilayani dan jumlah antrean aktif di depan, dengan refresh otomatis. Pembatalan dan check-in mandiri menunggu UC-04/05; UI-04 sampai UI-10 belum diimplementasikan. Lihat [PROGRESS.md](PROGRESS.md) untuk status review/verifikasi aktual, [SETUP.md](SETUP.md) untuk langkah lengkap, [roadmap](docs/PROJECT.md), [kontrak](docs/KONTRAK.md), serta [arsitektur BCE](docs/ARSITEKTUR.md).

## Pemeriksaan

```bash
./mvnw --batch-mode --no-transfer-progress verify
python scripts/smoke_demo.py target/apotek-0.0.1-SNAPSHOT.jar
```

`verify` menjalankan tes migrasi, constraint, login, peran, CSRF, kepemilikan, jadwal, booking, perebutan slot terakhir, submit ganda, rollback, status/posisi antrean serta profil demo/default, lalu membuat JAR. Skrip Python standar memulai JAR dari folder sementara tanpa `.env`, memeriksa enam akun sintetis, akses lintas peran, password salah, CSRF, logout, jadwal, booking dan status antrean, kemudian mengulangi startup di JVM baru. CI menjalankan kedua perintah dengan JDK 21 tanpa service database. Skrip HTTP ini melengkapi pemeriksaan visual browser.

## Cara kerja

Satu tahap per branch sesuai [roadmap](docs/PROJECT.md). Pertahankan perubahan lokal; Jefry melakukan commit, push, PR dan merge. UC-03 dikerjakan setelah UC-02 masuk main melalui PR #8. UC-04 menunggu UC-03 direview dan diintegrasikan. Status working tree belum berarti perubahan telah terintegrasi. Ikuti [CONTRIBUTING.md](CONTRIBUTING.md).

Migrasi MySQL historis V1/V2 tetap utuh. Pada H2, placeholder opsi engine/kolasi dikosongkan dan Hibernate memakai `ddl-auto=validate`. Migrasi PostgreSQL khusus disiapkan pada DB-01 di lokasi terpisah.
