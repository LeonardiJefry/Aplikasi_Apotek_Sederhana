# Aplikasi Apotek Sederhana

Proyek kelompok RSI menggunakan Java, Spring Boot, Thymeleaf, dan MySQL dengan pola Boundary–Controller–Entity (BCE).

## Status

Fondasi proyek dan kolaborasi sudah disiapkan. Halaman awal dapat dijalankan; fitur bisnis, entitas, dan tabel akan mengikuti rancangan Modul 2 yang disepakati tim.

## Teknologi

| Bagian | Pilihan |
| --- | --- |
| Java | JDK 21 |
| Backend | Spring Boot 4.1.2, Spring MVC |
| Tampilan | Thymeleaf, HTML, CSS, JavaScript |
| Database | MySQL 8.4, Spring Data JPA |
| Build | Maven Wrapper |
| Pengujian | JUnit, H2 untuk test lokal, MySQL di CI |

Versi Spring Boot mengacu pada [dokumentasi resmi](https://docs.spring.io/spring-boot/). Jika praktikum menetapkan versi lain, sepakati perubahan melalui pull request.

## Mulai di Windows (PowerShell)

1. Pasang Git dan JDK 21. Pastikan `java -version` menampilkan versi 21 dan `JAVA_HOME` mengarah ke folder JDK.
2. Ambil proyek:

~~~powershell
git clone https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana.git
cd Aplikasi_Apotek_Sederhana
Copy-Item .env.example .env
~~~

3. Edit `.env` dan isi konfigurasi database lokal. Aplikasi membaca file ini otomatis dari folder proyek.

**Pilihan A — Docker Desktop:**

~~~powershell
docker compose up -d --wait
~~~

**Pilihan B — MySQL yang sudah terpasang:**

Buat database `apotek` pada MySQL, lalu sesuaikan host, port, username, dan password di `.env`. Tidak perlu menjalankan Docker jika sudah memakai MySQL lokal. Setiap anggota memakai database lokal masing-masing.

4. Jalankan:

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

Buka [http://localhost:8080](http://localhost:8080). Unduhan Maven dan dependency memerlukan internet pada build pertama.

Untuk macOS/Linux, salin konfigurasi memakai `cp .env.example .env`, lalu jalankan `./mvnw spring-boot:run`.

## Pemeriksaan sebelum mengirim kode

~~~powershell
.\mvnw.cmd --batch-mode verify
~~~

Test lokal memakai H2 di memori, sehingga tidak memerlukan MySQL. GitHub Actions juga menjalankan test terhadap MySQL 8.4.

## Cara kerja tim

1. Buat atau ambil satu [Issue](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/issues), lalu tentukan satu PIC.
2. Buat branch dari `main`: `feat/nama-fitur`, `fix/nama-bug`, atau `docs/nama-dokumen`.
3. Kerjakan dan uji perubahan.
4. Push branch, lalu buka pull request ke `main` dan cantumkan `Closes #nomor-issue`.
5. Minta satu teman melakukan review. Setelah CI hijau dan review selesai, gabungkan dengan **Squash and merge**.

Lihat [panduan kontribusi](CONTRIBUTING.md), [arsitektur BCE](docs/ARSITEKTUR.md), dan [pengaturan tim](docs/SETUP_TIM.md).

## Peta folder

| Lokasi | Isi |
| --- | --- |
| `src/main/java/id/ac/ub/apotek/boundary` | Penerima HTTP, validasi input, penghubung ke tampilan |
| `src/main/java/id/ac/ub/apotek/controller` | Alur use case dan aturan bisnis |
| `src/main/java/id/ac/ub/apotek/entity` | Entitas JPA sesuai model data |
| `src/main/java/id/ac/ub/apotek/repository` | Akses database dengan Spring Data JPA |
| `src/main/resources/templates` | Halaman Thymeleaf |
| `src/main/resources/static` | CSS dan JavaScript |
| `src/test` | Test aplikasi |
| `docs` | Kesepakatan teknis dan panduan tim |

## Catatan database

Konfigurasi awal memakai `ddl-auto=validate`: aplikasi tidak mengubah tabel otomatis. Sebelum menambahkan entitas, sertakan skrip perubahan skema yang disepakati tim. Gunakan data dummy untuk pengembangan dan simpan password hanya pada `.env` lokal.

## Referensi alat

Maven Wrapper diambil dari [panduan resmi Spring](https://github.com/spring-guides/gs-serving-web-content/tree/main/complete) dan mempertahankan pemberitahuan lisensinya.
