# Setup Klinik Leonardo

PIC seluruh tahap: Jefry (JL). Urutan: SETUP-00 → UC-01 sampai UC-10 → QA-01 → DB-01. [PROGRESS.md](PROGRESS.md) memuat status aktual dan cek yang masih tertunda.

## 1. Jalankan demo lokal dari Git Bash

Gunakan JDK 21 dan Git Bash. Pastikan `java -version` menampilkan Java 21 dan `JAVA_HOME` mengarah ke JDK tersebut. Dari clone baru:

```bash
git clone https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana.git
cd Aplikasi_Apotek_Sederhana
./mvnw --batch-mode --no-transfer-progress verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Untuk repo yang sudah ada, mulai langsung dari foldernya. Buka [halaman login](http://localhost:8080/login). `./mvnw spring-boot:run` juga memakai profil default `demo`. Build pertama perlu internet untuk mengunduh Maven dan dependensi. Tidak perlu membuat database, menjalankan MySQL/Docker, menyalin `.env.example`, atau menyediakan akun Supabase.

`.env` pribadi tetap disimpan dan diabaikan Git. Profil `demo` dan `test` tidak mengimpor file itu; konfigurasi database/kredensial MySQL lama tidak memengaruhi alur default. Profil `mysql` yang dipilih secara eksplisit tetap membacanya. Variabel proses Spring seperti `SPRING_PROFILES_ACTIVE` atau `SPRING_DATASOURCE_URL` dapat mengubah konfigurasi; hindari variabel database lama dalam terminal demo.

| Username sintetis | Peran | ID identitas | Halaman |
| --- | --- | --- | --- |
| `pasien` | PASIEN | PAS-001 | `/pasien` |
| `pasien2` | PASIEN | PAS-002 | `/pasien` |
| `loket` | PETUGAS_LOKET | USR-003 | `/loket` |
| `admin` | ADMIN | USR-004 | `/admin` |
| `dokter` | DOKTER | DOK-001 | `/dokter` |
| `dokter2` | DOKTER | DOK-002 | `/dokter` |

Password seluruh akun demo lokal: `demo-klinik-2026`, yaitu fixture publik sintetis. Untuk menggantinya, sebelum startup:

```bash
export DEMO_LOCAL_PASSWORD='password_sintetis_minimal_12_karakter'
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Password minimal 12 karakter dan maksimal 72 byte UTF-8. Database menyimpan hash BCrypt. Jangan memakai password pribadi. Keluar menggunakan form POST dengan CSRF. Demo mendengarkan hanya di `127.0.0.1`; H2 console tidak diaktifkan.

## 2. Database demo dan profil

| Profil | Penggunaan |
| --- | --- |
| `demo` (default) | `jdbc:h2:mem:klinik-demo`, MODE=MySQL, seed otomatis sintetis |
| `test` | `jdbc:h2:mem:klinik-test`, password fixture tes tersendiri |
| `mysql` | Opsional historis, konfigurasi DB dari `.env` |
| `dev` lama | Tidak dipakai; pilih demo atau mysql secara eksplisit |

H2 ada pada classpath runtime, termasuk saat Maven membuat executable JAR. Flyway menjalankan migrasi V1/V2 yang sudah ada; hanya placeholder `tableOptions` dikosongkan untuk H2. Hibernate memakai `ddl-auto=validate`. Jangan mengedit migrasi yang pernah dipasang atau mengubahnya menjadi `update/create`.

Fixture mencakup 2 poli, 2 dokter, 2 pasien, 4 jadwal, 9 antrean, 2 jadwal vaksinasi, 1 notifikasi, 4 riwayat pelewatan, dan 6 akun sintetis. Seluruh delapan status kontrak tersedia. Tanggal mengikuti `Asia/Jakarta` saat database pertama kali dibuat. Seed berjalan atomik satu kali per database dengan kunci `DemoDataset`, sehingga pemanggilan ulang pada JVM yang sama mempertahankan perubahan data.

Data demo H2 hilang saat JVM berhenti. Ctrl+C lalu jalankan ulang aplikasi untuk kembali ke fixture awal dengan tanggal startup baru. Data MySQL opsional tetap persisten; aturan restart H2 tidak berlaku untuk MySQL.

## 3. Verifikasi build, HTTP, dan browser

```bash
./mvnw --batch-mode --no-transfer-progress verify
python scripts/smoke_demo.py target/apotek-0.0.1-SNAPSHOT.jar
```

Di Linux dapat memakai `python3`. Skrip hanya memakai pustaka standar Python. `verify` mengompilasi, menjalankan tes, dan membuat executable JAR. Tes memeriksa migrasi berulang, seed berulang, relasi/constraint, autentikasi, empat peran, akun nonaktif, CSRF, kepemilikan pasien/dokter, serta profil demo dan default tanpa `.env`.

Tes regresi `ServletSecurityTests` juga menjalankan Tomcat pada port lokal acak untuk memeriksa forward POST saat CSRF ditolak. Port loopback harus diizinkan. Ini melengkapi MockMvc yang tidak menjalankan forward servlet secara otomatis. Setelah source Java diubah, build ulang JAR sebelum menjalankan skrip smoke; skrip selalu memakai JAR yang ditunjuk, bukan source terbaru.

Skrip HTTP memulai JAR pada `127.0.0.1:18080` dari direktori sementara kosong, memeriksa login enam akun sintetis, password salah, penolakan akses lintas peran, CSRF dan logout. Proses dihentikan, kemudian startup dan pemeriksaan diulang pada JVM baru. Port 18080 harus kosong. Log berada di `target/demo-smoke-1.log` dan `target/demo-smoke-2.log`. CI menggunakan JDK 21, H2, dan kedua perintah ini tanpa service database.

Untuk pemeriksaan visual, jalankan aplikasi normal di port 8080 dan coba:

1. Tanpa login, URL `/pasien` mengarah ke login. Password salah menampilkan pesan gagal.
2. Masuk sebagai pasien, loket, admin dan dokter; beranda menampilkan peran dan username yang tepat.
3. Dari setiap akun, buka URL tiga peran lain; hasil harus 403.
4. Logout mengembalikan ke login; halaman peran kembali meminta login.
5. Hentikan proses dan jalankan lagi; akun demo tersedia kembali tanpa duplikasi atau error migrasi.

Catat hasil build, tes, startup HTTP, dan browser secara terpisah. Lulus MockMvc tidak sama dengan lulus browser. Bila DNS Maven terhalang, periksa `mvn -version` dan cache dependency lokal lalu coba `mvn --offline --batch-mode --no-transfer-progress verify`. Jangan mengganti versi dependency atau mengklaim verify berhasil bila ada artifact yang belum tersimpan. Cache lengkap dan repositori asal artifact tetap diperlukan. Sandbox yang menolak Bash atau loopback perlu dibedakan dari kegagalan aturan aplikasi.

## 4. Branch dan serah terima

Sebelum mengedit, baca AGENTS.md, PROGRESS.md, roadmap, kontrak dan kartu tahap. Cek:

```bash
git status --short --branch
```

Branch fondasi: `chore/setup-00-fondasi`. Bila hasil lokal masih berada di main dan branch belum ada, Jefry dapat memindahkannya tanpa membuang working tree:

```bash
git switch -c chore/setup-00-fondasi
```

Jika branch sudah ada, gunakan `git switch chore/setup-00-fondasi`. Jangan pakai force/reset/clean untuk menghilangkan benturan. Agent menyerahkan perubahan dan usulan pesan commit; commit, push, PR dan merge dilakukan Jefry. Periksa identitas dengan `git log -1 --format=fuller` sebelum push. Ketentuan kontribusi lain ada di CONTRIBUTING.md.

SETUP-00 berstatus review ketika implementasi siap ditinjau dengan hasil dan cek tertunda tercatat. Status done memerlukan verifikasi dan integrasi ke main. UC-01 baru dimulai setelah SETUP-00 terverifikasi dan masuk main. Working tree di branch main belum membuktikan integrasi.

## 5. MySQL opsional historis

Bagian ini mempertahankan konfigurasi yang sudah ada. MySQL tidak diperlukan pada SETUP-00, pengembangan UC, QA-01 atau CI. Target jarak jauh proyek adalah PostgreSQL Supabase pada DB-01.

Jika masih ingin menjalankan konfigurasi MySQL lokal, gunakan MySQL 8.0.16+ (8.4 sesuai compose.yaml); MariaDB XAMPP bukan target kolasi yang sama. Pertahankan `.env` yang sudah ada. Hanya bila belum ada, salin `.env.example` lalu isi DB_HOST, DB_PORT, DB_NAME, DB_USERNAME dan DB_PASSWORD secara lokal. Jangan commit kredensial.

Buat database kosong melalui administrator lokal, lalu beri akun aplikasi hak CREATE, ALTER, INDEX, REFERENCES, SELECT, INSERT, UPDATE dan DELETE hanya pada database itu:

```sql
CREATE DATABASE apotek CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

Untuk Compose opsional, isi pula DB_ROOT_PASSWORD di `.env`, kemudian `docker compose up -d --wait`. Jalankan profil secara eksplisit:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

Seed MySQL hanya aktif jika `DEMO_ENABLED=true` dan `DEMO_PASSWORD` valid di `.env`. Password/tanggal/data tidak ditimpa setelah seed pertama. `allowPublicKeyRetrieval=true` dan `sslMode=DISABLED` pada contoh hanya untuk koneksi localhost. Tidak ada reset database atau baseline Flyway otomatis. File `.env` lama tidak diubah oleh pekerjaan H2 ini.

## 6. DB-01 dan batas SETUP-00

DB-01 baru dibuka setelah QA-01 lulus dan masuk main. Siapkan driver/profil PostgreSQL Supabase serta lokasi migrasi PostgreSQL terpisah. Audit tipe SQL, ekspresi NIK, casing identifier, timezone, constraint dan transaksi dari seluruh UC. Belum ada koneksi, migrasi PostgreSQL, atau kredensial Supabase yang dipasang pada SETUP-00.

Login/layout tetap memakai token Design System Hi-Fi RSI yang sudah diterapkan; tidak ada perubahan layar UC. Font Poppins memakai fallback bila belum tersedia. UI-01 sampai UI-10 menunggu tahap fitur. Model tetap di `id.ac.ub.apotek.entity` dan repository di `id.ac.ub.apotek.repository`; jangan menduplikasi model.
