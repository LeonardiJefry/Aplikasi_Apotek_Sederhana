# SETUP-00 — hasil implementasi dan verifikasi

Tanggal: 26 November 2026 (WIB). PIC: Jefry (JL). Konteks skema awal: [Issue #3](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/issues/3).

**Status: done.** Fondasi a90c2e2 sudah diintegrasikan ke main melalui [PR #6](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/pull/6), merge commit 2fe5510; status merged dikonfirmasi lewat GitHub. [PROGRESS.md](../PROGRESS.md) adalah sumber status aktif, kini UC-01. Verifikasi lokal fondasi: **46 tes lulus, 0 gagal/error/skipped**, termasuk regresi servlet HTTP, serta startup kedua berhasil. Runtime lokal JDK 24 dengan target kompilasi 21; CI memakai JDK 21.

Permintaan menunggu integrasi SETUP-00 sebelum UC-01 sudah dipenuhi. Jefry melakukan commit/push/merge; agent baru melanjutkan UC-01 setelah memeriksa PR #6. Hasil sandbox pada bagian selanjutnya adalah riwayat, bukan status akhir fondasi. Roadmap tetap H2 sampai QA-01, lalu PostgreSQL Supabase pada DB-01.

## Koreksi forward POST saat CSRF ditolak

Uji lokal Jefry menghasilkan 41 tes Surefire lulus dan JAR executable. Log membuktikan Tomcat aktif pada port 18080. Skrip smoke gagal pada ekspektasi 403 dari POST login tanpa CSRF; versi skrip tersebut belum mencetak respons aktual.

Halaman galat sebelumnya meminta login dan hanya menerima GET, padahal error handler meneruskan metode POST asli. `SecurityConfig` kini mengizinkan halaman galat umum tanpa login, dan `HomeBoundary` menerima metode forward dengan status eksplisit 403. Halaman tidak memuat data pasien; CSRF dan prefix fitur tetap dilindungi. Dua regresi MockMvc memeriksa halaman anonim dan forward POST; `ServletSecurityTests` menambah pemeriksaan HTTP melalui Tomcat sungguhan untuk memastikan login/logout yang ditolak tidak mengubah autentikasi.

Hasil terbaru setelah koreksi: Maven 3.9.16 offline dengan Mockito agent menjalankan 46 tes, **45 lulus dan 1 error startup servlet** karena izin loopback sandbox (`Permission denied: connect`), bukan assertion HTTP yang sudah dijalankan. Verify belum lulus. Package terpisah `-DskipTests package` **BUILD SUCCESS** dan JAR target sudah memuat koreksi. Sintaks Python dan `git diff --check` lulus; .env tetap utuh. Log: `target/csrf-maven-verify-agent.log`, `target/csrf-package.log`.

Ulangi dari CMD lokal: `mvnw.cmd --batch-mode --no-transfer-progress verify` lalu `python scripts\smoke_demo.py target\apotek-0.0.1-SNAPSHOT.jar`. Skrip sekarang mencetak status dan Location bila gagal. JAR juga sudah siap untuk smoke langsung. Hasil HTTP terbaru dan startup kedua tetap harus dicatat sebelum penutupan SETUP-00.

## Riwayat hasil awal sebelum build lokal Jefry

| Bagian | Hasil |
| --- | --- |
| Kompilasi Maven lokal offline | Lulus: 30 source utama, 4 kelas tes, target Java 21. Runtime tersedia JDK 24.0.2 |
| Engine JUnit proyek | **41 lulus, 0 gagal**, exit 0; migrasi, constraint, seed, login/peran, CSRF, kepemilikan, demo dan profil default |
| Maven Wrapper verify dari Git Bash | Bash gagal dimulai karena izin sandbox (MSYS `0xC0000022`) |
| Maven Wrapper fallback Windows | DNS unduhan Maven tidak tersedia; exit 1 |
| Maven lokal verify | Terhalang cache `junit-platform-launcher:6.0.3`; exit 1 |
| Package offline tanpa tes | Terhalang dependency plugin jar `org/codehaus/plexus/archiver/FileSet`; exit 1 |
| Startup web demo | H2 tersambung, V1/V2 berhasil, JPA tervalidasi; Tomcat gagal membuka loopback karena `Permission denied: connect`, exit 1 |
| Browser dan startup kedua | Belum terverifikasi karena HTTP tidak dapat berjalan di sandbox |
| Skrip smoke HTTP | Sintaks Python lulus; eksekusi JAR/HTTP belum dilakukan |
| CI H2 dan smoke JAR | Dikonfigurasi; belum dipicu dari sesi ini |
| Whitespace dan .env | `git diff --check` lulus; .env tidak berubah dan tetap diabaikan Git |

Runner engine sementara memakai dependency JUnit 6.0.3 yang sama, hasil kompilasi terbaru dan Mockito Java agent. Hasil ini bukan Maven verify. Log rinci di `target/h2-test-compile.log`, `target/h2-engine-tests-final.log`, `target/h2-wrapper-verify.log`, `target/h2-maven-verify.log`, `target/h2-package.log`, dan `target/h2-demo-startup.log`; semuanya sementara dan diabaikan Git.

## Implementasi yang dipertahankan

Sembilan entitas JPA dan repository bersama memakai nama tabel PDM Modul 3: Pasien, Poli, Dokter, JadwalPraktik, Antrean, JadwalVaksinasi, Notifikasi, RiwayatPelewatan, dan Pengguna. V2 juga memuat penanda internal DemoDataset. Flyway V1/V2 tidak diedit. Hibernate hanya validate. Seed sintetis atomik dan sekali per database.

SecurityConfig membatasi prefix berdasarkan empat peran, default-deny rute lain, CSRF dan logout POST. Principal berasal dari sesi tanpa entity JPA; layanan akses tiket memvalidasi kepemilikan pasien/dokter. Fixture berisi dua pasien dan dua dokter, memungkinkan pemeriksaan kepemilikan. Login/layout dasar tetap mengikuti komponen Hi-Fi RSI yang sudah diterapkan. Layar UI-01 sampai UI-10 belum dibuat.

## Berkas sesi H2 yang diubah atau ditambahkan

- `AGENTS.md`, `PROGRESS.md`, `README.md`, `SETUP.md`, `.env.example`, `.gitignore`
- `.github/workflows/ci.yml`, `pom.xml`
- `docs/PROJECT.md`, `docs/KONTRAK.md`, `docs/ARSITEKTUR.md`, `docs/SETUP_00_VERIFIKASI.md`
- `docs/agents/JL.md`, `docs/agents/MF.md`, `docs/agents/RA.md`, `docs/agents/NA.md`, `docs/agents/AH.md`
- `src/main/resources/application.properties`
- `src/main/resources/application-demo.properties`, `application-mysql.properties`, `application-dev.properties`
- `src/main/java/id/ac/ub/apotek/demo/DemoDataInitializer.java`, `DemoDataService.java`
- `src/test/resources/application-test.properties`
- `src/test/java/id/ac/ub/apotek/ApotekApplicationTests.java`, `SchemaConstraintsTests.java`, `DemoProfileTests.java`, `DefaultDemoProfileTests.java`
- `scripts/smoke_demo.py`

Perubahan fondasi sebelumnya (entity/repository/security, layanan akses tiket, migrasi, layout/template/CSS) tetap ada di working tree. Perubahan awal `mvnw.cmd` juga dipertahankan; diff-nya hanya akhir baris (`git diff --ignore-space-at-eol --exit-code -- mvnw.cmd` sukses). Tidak ada perubahan .env pribadi atau koneksi Supabase.

## Menyelesaikan verifikasi

Ikuti [SETUP.md](../SETUP.md) dari Git Bash dengan JDK 21 dan jaringan Maven:

```bash
./mvnw --batch-mode --no-transfer-progress verify
python scripts/smoke_demo.py target/apotek-0.0.1-SNAPSHOT.jar
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Buka [login](http://localhost:8080/login): akun pasien, pasien2, loket, admin, dokter dan dokter2 memakai password sintetis `demo-klinik-2026` kecuali diubah lewat DEMO_LOCAL_PASSWORD. Periksa tampilan, login salah, akses lintas peran, logout serta startup kedua. Demo tidak membaca .env. MySQL historis tersedia hanya bila profil mysql dipilih secara eksplisit.

Branch aktual masih main dengan perubahan belum di-commit; .git dibatasi hanya baca. Jefry memindahkan hasil ke `chore/setup-00-fondasi`, menyelesaikan cek, lalu melakukan commit/push/PR/merge. Jangan mengartikan nama branch main sebagai bukti integrasi. UC-01 menunggu fondasi terverifikasi dan masuk main.

Usulan pesan commit: `chore(setup-00): run clinic demo on H2 without a database server`.
