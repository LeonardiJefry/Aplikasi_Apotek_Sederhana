# Instruksi agent Klinik Leonardo

Repo ini mengimplementasikan Klinik Pratama Sehat Bersama, RSI Kelompok 1. Gunakan identitas Klinik Leonardo dan ID UC/PC/UI persis dari Modul 2 dan 3.

PIC tunggal untuk SETUP-00, UC-01 sampai UC-10, QA-01, dan DB-01 adalah Jefry (JL). Ikuti urutan di roadmap dan baca kartu berdasarkan UC.

## Mulai pekerjaan

1. Baca [PROGRESS.md](PROGRESS.md), [roadmap](docs/PROJECT.md), [kontrak](docs/KONTRAK.md), dan kartu tugas. Cek `git status --short --branch`. Lanjutkan tahap aktif atau ambil tahap pertama yang dependensinya siap. Gunakan satu branch per tahap dan pertahankan perubahan lokal.
2. Sebutkan tugas, branch, cakupan, dan dependensi awal, lalu langsung kerjakan bagian yang bisa diverifikasi. Gunakan UI dari Modul 2 §2.7 dan frame Hi-Fi RSI yang sesuai.
3. Gunakan Java 21, Spring Boot, Thymeleaf, Spring Security, Spring Data JPA, Maven Wrapper, dan struktur BCE sesuai repo. Boundary menangani HTTP dan input, service menerapkan aturan bisnis serta transaksi, repository bertanggung jawab atas akses data, dan template menampilkan DTO. Gunakan satu model untuk setiap entitas bersama.
4. Selama SETUP-00, fitur, dan QA-01, gunakan profil `demo` dan `test` berbasis H2 dengan data sintetis. Kerjakan frontend dan backend melalui alur aplikasi normal. Kebutuhan data dicatat di model, tes, dan kontrak. Mulai integrasi PostgreSQL Supabase pada DB-01 setelah QA-01 lulus dan hasilnya masuk main.
5. Ambil identitas dari sesi, validasi peran dan kepemilikan di server, aktifkan CSRF pada form, dan pertahankan akun demo hanya pada profil yang mengizinkannya. Simpan rahasia di lingkungan lokal/server dan gunakan data pasien sintetis.
6. Dari Bash, jalankan `./mvnw --batch-mode --no-transfer-progress verify` atau pengujian yang relevan. Cek browser untuk alur sukses dan penolakan yang terkait. Catat hasil aktual, cek tertunda, dependensi, dan langkah berikutnya di `PROGRESS.md`.

Lihat [kartu JL dan indeks fitur](docs/agents/JL.md) untuk kriteria selesai setiap tahap.

## Perubahan dan Git

Pertahankan aturan bisnis pada kontrak. Catat usulan perubahan struktur data, status, login, atau layout bersama dan periksa dampaknya pada UC yang terkait. Jangan mengubah migrasi yang sudah pernah terpasang. Bila repo memuat migrasi MySQL lama, pertahankan riwayatnya dan gunakan lokasi PostgreSQL terpisah pada DB-01.

Selesai dengan daftar file, verifikasi, dan usulan pesan commit. Serahkan commit, push, PR, dan merge kepada Jefry. Sebelum push, tinjau author, committer, dan isi dengan `git log -1 --format=fuller`. Ikuti [CONTRIBUTING.md](CONTRIBUTING.md) pada repo.