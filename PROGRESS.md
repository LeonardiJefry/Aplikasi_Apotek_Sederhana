# Progres Klinik Leonardo

Diperbarui: 26 November 2026 (WIB). PIC seluruh tahap: Jefry (JL).

## Tahap aktif

**UC-01: review — implementasi jadwal praktik dan sisa kuota siap ditinjau. SETUP-00: done melalui PR #6.**

Branch tujuan UC-01: `feat/uc-01-jadwal`. Branch aktual sesi: `chore/setup-00-fondasi` pada `a90c2e2`, working tree awal bersih. PR [#6](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/pull/6) sudah merged ke main (`2fe5510`). Perbandingan GitHub antara a90c2e2 dan merge commit menghasilkan daftar berkas kosong: isi baseline lokal sama dengan hasil integrasi. Main lokal belum diperbarui. Pembuatan branch UC-01 ditolak karena `.git` hanya baca; perubahan fitur tetap di working tree dan perlu dipindahkan ke branch tujuan oleh Jefry sebelum commit. Tidak ada commit/push/PR/merge oleh agent; .env pribadi tidak diedit.

## UC-01 — implementasi dan verifikasi terbaru

Acuan dibaca: Modul 2 revisi PC-01 halaman 86–87, UI-01 halaman 97; metadata, design context dan screenshot Hi-Fi RSI frame `115:881`. Implementasi memakai layout/tokens fondasi, form HTML native, dan data sintetis yang sama.

- JadwalBoundary, JadwalService dan JadwalDto menyediakan filter tanggal + minimal satu poli/dokter, validasi hubungan dokter-poli, daftar dokter sesuai poli, snapshot tanpa identitas pasien, hasil kosong, serta pesan kesalahan.
- Jadwal dibaca melalui query repository bersama dengan fetch dokter/poli. Tidak ada skema/entity/migrasi baru atau perubahan data seed.
- `/pasien/jadwal/data` memakai no-store dan dibaca ulang; polling 5 detik dengan timeout, refresh manual, kondisi data lama dan sesi kedaluwarsa. Link pilihan dinonaktifkan saat refresh gagal.
- Pilih Jadwal membuka titik sambung baca-saja; server membaca ulang kuota dan menolak Penuh (409) atau ID tidak ditemukan (404). Belum membuat tiket atau mengurangi kuota. Booking nyata tetap UC-02.
- Tampilan tanggal ringkasan dipastikan YYYY-MM-DD; waktu HH:mm. Field date native dapat menampilkan format locale browser, tetapi nilai kiriman memakai YYYY-MM-DD.

| Pemeriksaan UC-01 | Hasil aktual |
| --- | --- |
| Maven lokal 3.9.16 offline verify | BUILD SUCCESS, 59 tes lulus (13 JadwalTests + 46 fondasi), 0 gagal/error/skipped |
| Maven Wrapper Windows offline verify setelah koreksi format tanggal | BUILD SUCCESS, 59 tes lulus. JDK 24.0.2, compile release 21, Mockito agent dari cache. Log `target/uc01-wrapper-final.log` |
| Git Bash verify | Terhalang sebelum Maven: MSYS `NtCreateDirectoryObject ... 0xC0000022`; `target/uc01-bash-verify.log` |
| Smoke executable JAR, dua JVM baru | Lulus: enam akun per startup, akses lintas peran, CSRF, logout, snapshot UC-01, validasi, privasi dan pilihan read-only. Log `target/uc01-smoke.log`; pengulangan final termasuk tanggal ada di `target/uc01-smoke-final.log` |
| Browser localhost dengan akun pasien | Lulus: form, tabel Poli Umum 6/10, pilihan dokter mengikuti poli, filter kosong ditolak, hasil tanpa jadwal, ringkasan pilihan, auto-refresh teramati tiap 5 detik, akses /admin ditolak |
| Browser saat koneksi server sengaja dihentikan | Lulus: pesan data terakhir belum diperbarui muncul, link Pilih Jadwal kehilangan href dan aria-disabled=true |
| Sintaks JavaScript/Python dan git diff --check | Lulus |

Tes otomatis juga mengubah sisa kuota menjadi nol di transaksi uji, memeriksa JSON/status Penuh, tombol disabled, penolakan pemilihan lewat URL dan tidak adanya efek samping tiket/kuota. Seluruh perubahan fixture di-rollback. Browser memakai proses H2 sementara; proses tersebut sudah dihentikan.

Keterbatasan review visual: screenshot desktop telah diperiksa, tetapi layout bersama dan font fallback fondasi dipertahankan. Unduhan SVG Figma timeout; tidak ada aset rusak/URL sementara di runtime. Ikon, font dan posisi layout bersama belum identik seluruhnya dengan frame Figma. Tampilan mobile menggunakan CSS responsif tetapi belum diperiksa di viewport perangkat. Tidak mengklaim hasil pixel-identik atau CI GitHub yang belum dijalankan untuk UC-01.

Daftar berkas UC-01: boundary/JadwalBoundary.java; controller/JadwalDto.java dan JadwalService.java; repository/JadwalPraktikRepository.java dan DokterRepository.java; templates/jadwal/index.html dan pilihan.html; templates/index.html; static/js/jadwal.js; static/css/app.css; JadwalTests.java; scripts/smoke_demo.py; README.md; SETUP.md; docs/KONTRAK.md; docs/SETUP_00_VERIFIKASI.md; PROGRESS.md.

Langkah berikutnya: pindahkan perubahan ke feat/uc-01-jadwal berbasis origin/main, review hasil, lalu commit/push/PR/merge oleh Jefry. Setelah UC-01 masuk main, mulai UC-02. Usulan commit: `feat(uc-01): add practice schedules and live remaining quotas`.

Instruksi AGENTS.md revisi sudah diterapkan dari versi mandiri yang dibaca dan dikonfirmasi ulang oleh Jefry. Roadmap/kartu/kontrak direkonsiliasi dengan panduan revisi versi 2 dan kode aktual. ZIP revisi `Klinik_Leonardo_Agent_Setup (1).zip` telah diidentifikasi, tetapi transfer bytes gagal karena DNS; isi PROGRESS.md asli di arsip belum dapat dibandingkan. Catatan ini dibuat berdasarkan hasil aktual workspace, bukan mengasumsikan tahap di arsip sudah selesai.

## Riwayat verifikasi SETUP-00 sebelum integrasi

Laporan Surefire terbaru telah diperiksa: ApotekApplicationTests 8, DefaultDemoProfileTests 1, DemoProfileTests 9, SchemaConstraintsTests 27, ServletSecurityTests 1; total **46 tes, 0 gagal, 0 error, 0 skipped**. Tes servlet dengan Tomcat sungguhan kini lulus di terminal lokal.

Log `target/demo-smoke-2.log` mencatat Tomcat berhasil startup kedua pada port 18080, 26 November 2026 pukul 20.29 WIB. Jefry mengonfirmasi aplikasi sudah bisa berjalan. Ketercapaian startup kedua berarti pemeriksaan run pertama telah dilewati skrip. Hasil akhir skrip dinyatakan berhasil berdasarkan konfirmasi Jefry; stdout PASS tidak tersimpan dalam log aplikasi. Pemeriksaan visual seluruh halaman browser belum dicatat terpisah.

Pada saat verifikasi fondasi, Git masih di main pada 4cbba12 dan Jefry meminta menunggu integrasi. Gerbang ini sekarang terpenuhi melalui PR #6; UC-01 dikerjakan setelah konfirmasi merged dari GitHub. Catatan error sandbox dan cache di bagian selanjutnya adalah riwayat percobaan fondasi.

Hasil SETUP-00 telah di-commit a90c2e2 dan diintegrasikan oleh Jefry melalui merge 2fe5510. Tidak ada tindakan Git tulis dari agent.

## Riwayat koreksi setelah uji lokal Jefry - penolakan CSRF

Sebelum koreksi ini, laporan Surefire dari terminal lokal menunjukkan 41 tes lulus dan executable JAR sudah terbentuk. Log `demo-smoke-1.log` menunjukkan Tomcat berhasil startup di port 18080, dan skrip mencapai pemeriksaan HTTP. Skrip kemudian gagal pada POST login tanpa CSRF yang mengharapkan 403. Respons aktualnya tidak dicetak oleh versi skrip saat itu, sehingga kode status kegagalan tidak diasumsikan dari screenshot saja.

Audit menemukan halaman `/akses-ditolak` masih memerlukan login dan hanya menerima GET. Error handler meneruskan metode asli, termasuk POST. Konfigurasi ini dapat mengalihkan penolakan anonim ke login atau gagal merender forward POST. Koreksi: halaman galat umum diizinkan tanpa login, menerima metode asli, dan secara eksplisit mengembalikan 403; prefix fitur, CSRF dan kepemilikan tetap dibatasi. Halaman galat tidak berisi identitas pasien.

Berkas koreksi: `SecurityConfig.java`, `HomeBoundary.java`, `ApotekApplicationTests.java`, `ServletSecurityTests.java`, `scripts/smoke_demo.py`, `SETUP.md`, `docs/KONTRAK.md`, `docs/SETUP_00_VERIFIKASI.md` dan catatan ini. Skrip sekarang mencetak status HTTP/Location saat ekspektasi gagal, memeriksa bahwa login tanpa CSRF tidak mengautentikasi, dan logout tanpa CSRF tidak mengakhiri sesi.

| Pemeriksaan koreksi | Hasil aktual |
| --- | --- |
| Maven 3.9.16 offline verify, Mockito agent dari cache | 46 tes: **45 lulus, 0 failure assertion, 1 error**. ServletSecurityTests tidak dapat memulai Tomcat karena `Unable to establish loopback connection` / `Permission denied: connect`. Verify belum lulus. Log `target/csrf-maven-verify-agent.log` |
| Regresi MockMvc test/demo | Dua regresi baru lulus pada kedua profil: halaman galat anonim dan forward POST merender 403 tanpa redirect |
| Package offline `-DskipTests package` setelah tes | **BUILD SUCCESS**, executable JAR telah dibangun ulang dengan koreksi. Ini bukan hasil verify yang lulus. Log `target/csrf-package.log` |
| Sintaks skrip Python, whitespace, .env | Lulus; .env sebelum/sesudah koreksi sama dan tidak diedit |
| Startup/HTTP setelah koreksi | Belum terverifikasi di terminal lokal; tes servlet dalam sandbox terhalang loopback. Startup sukses di terminal Jefry di atas adalah sebelum koreksi |

Maven verify tanpa agent sempat berhenti pada self-attach Mockito dan dihentikan; percobaan dengan agent lokal yang sama menjalankan seluruh tes. Git Bash masih terhalang MSYS `0xC0000022`. Versi dependency tidak diganti. Riwayat DNS/cache pada bagian awal di bawah mendahului tersedianya Maven/JAR dari terminal lokal.

Langkah lokal berikutnya di CMD: `mvnw.cmd --batch-mode --no-transfer-progress verify`, lalu `python scripts\smoke_demo.py target\apotek-0.0.1-SNAPSHOT.jar`. JAR di target sudah memuat perbaikan; skrip smoke juga dapat langsung dicoba ulang. Status tetap review, bukan done; UC-01 tetap menunggu verifikasi dan integrasi ke main oleh Jefry.

## Roadmap dan status aktual

| Tahap | Status | Prasyarat / langkah berikutnya |
| --- | --- | --- |
| SETUP-00 | done | 46 tes lulus; PR #6 merged ke main |
| UC-01 | review | 59 tes total lulus, smoke dan browser diperiksa; pindahkan working tree ke branch fitur, review dan integrasi oleh Jefry |
| UC-02 | todo | Setelah UC-01 masuk main |
| UC-03 | todo | Setelah UC-02 masuk main |
| UC-04 | todo | Setelah UC-03 masuk main |
| UC-05 | todo | Setelah UC-04 masuk main |
| UC-06 | todo | Setelah UC-05 masuk main |
| UC-07 | todo | Setelah UC-06 masuk main |
| UC-08 | todo | Setelah UC-07 masuk main |
| UC-09 | todo | Setelah UC-08 masuk main |
| UC-10 | todo | Setelah UC-09 masuk main |
| QA-01 | todo | Seluruh UC terintegrasi; regresi lintas peran di H2 |
| DB-01 | todo | QA-01 lulus dan masuk main; PostgreSQL Supabase |

UI-01 sudah diimplementasikan untuk review; UI-02 sampai UI-10 belum diimplementasikan. Label pemilik MF/RA/NA/AH pada kartu lama adalah asal spesifikasi; Jefry mengerjakan semua tahap. Detail branch/UC/PC/UI ada di [roadmap](docs/PROJECT.md).

## Perubahan SETUP-00 H2

- `demo` menjadi profil default. H2 dipindahkan dari scope test ke runtime dalam pom.xml. Tidak membutuhkan server MySQL atau Supabase.
- Profil demo memakai H2 memori, seed sintetis, password fixture publik `demo-klinik-2026`, server loopback dan H2 console nonaktif. Profil `test` memakai H2 dan password sintetis terpisah.
- Demo/test tidak membaca `.env`. Konfigurasi MySQL lama dipertahankan dalam `application-mysql.properties`, dengan impor `.env` dan seed opt-in. Profil lama dev tidak dipakai lagi.
- Migrasi Flyway V1/V2, sembilan entitas/repository, sesi login, empat peran, layout Thymeleaf, CSRF dan pemeriksaan kepemilikan digunakan kembali. Tidak mengubah SQL migrasi historis atau aturan bisnis UC.
- Seed dibatasi ke profil demo/test/mysql dan kondisi enabled. Pemasangan berulang pada database yang sama tidak menduplikasi fixture. Data H2 baru setelah JVM berhenti.
- CI memakai JDK 21 dan H2, tanpa service MySQL. Skrip `scripts/smoke_demo.py` disiapkan untuk menguji executable JAR, login enam akun, penolakan akses, CSRF, logout dan dua startup dari direktori sementara kosong. CI belum dijalankan dari sesi ini.
- README, SETUP, AGENTS, roadmap, kontrak dan kartu fitur diselaraskan dengan pengerjaan mandiri serta gerbang QA-01/DB-01.

## Riwayat verifikasi awal — build dan tes

| Pemeriksaan | Hasil |
| --- | --- |
| Git Bash: `./mvnw --batch-mode --no-transfer-progress verify` | Terhalang sebelum Wrapper berjalan: Bash gagal membuat objek MSYS, `NtCreateDirectoryObject ... 0xC0000022` |
| Maven Wrapper Windows `verify` sebagai fallback | Exit 1: DNS `repo.maven.apache.org` tidak dapat diresolusikan saat mengunduh Maven 3.9.16; `target/h2-wrapper-verify.log` |
| Maven lokal 3.9.11 offline `test-compile` | **Lulus, BUILD SUCCESS**. 30 source utama dan 4 source tes; `--release 21`, runtime JDK 24.0.2; `target/h2-test-compile.log` |
| Maven lokal offline `verify` | Exit 1: `junit-platform-launcher:6.0.3` tidak tersedia di cache. Bukan verify yang lulus; `target/h2-maven-verify.log` |
| Tes melalui engine JUnit 6.0.3 proyek | **41 lulus, 0 gagal**, exit 0. Classpath dari cache, kompilasi terbaru, Mockito sebagai Java agent, runner sementara di target; `target/h2-engine-tests-final.log` |
| Package offline `-DskipTests package` | Exit 1: cache plugin jar tidak lengkap (`org/codehaus/plexus/archiver/FileSet`). Executable JAR belum berhasil dibuat; `target/h2-package.log` |
| Sintaks skrip HTTP: `python -m py_compile scripts/smoke_demo.py` | Lulus. Ini bukan hasil eksekusi HTTP |
| `git diff --check` | Lulus |

41 hasil tes terdiri dari 27 kasus constraint, 6 metode alur keamanan/fixture pada test, 7 pada demo (termasuk 6 alur yang sama), dan 1 cek profil default. Cakupan: migrasi ulang, seed ulang, jumlah fixture dan delapan status, login empat peran, password salah, akun nonaktif, CSRF login/logout, matriks akses, kepemilikan tiket/jadwal, profil demo tanpa impor .env dan profil default H2. Profil default membuktikan 6 pengguna dan 9 antrean tanpa memilih profil secara eksplisit.

Tes engine tambahan tidak menggantikan Maven verify atau hasil runtime JDK 21. Versi Maven/dependency proyek tidak diganti untuk menutupi keterbatasan cache. Semua log di target bersifat sementara dan diabaikan Git.

## Riwayat verifikasi awal — startup dan browser

| Pemeriksaan | Hasil |
| --- | --- |
| Startup demo dari kelas utama dengan classpath runtime tersimpan dan H2 | Terhubung ke `jdbc:h2:mem:klinik-demo`; V1/V2 diterapkan ke skema kosong; Hibernate memvalidasi JPA. Lalu exit 1 saat connector Tomcat membuka loopback: `Unable to establish loopback connection`, `SocketException: Permission denied: connect`. Log `target/h2-demo-startup.log` |
| Seed/login pada profil demo dan default di konteks tes | Lulus; dibuktikan tes di atas, bukan startup HTTP |
| Browser: login, halaman peran, penolakan lintas peran | **Belum terverifikasi**, karena server HTTP tidak dapat berjalan dalam sandbox. MockMvc bukan browser |
| Skrip smoke executable JAR dan startup kedua | **Belum dieksekusi**: JAR belum terbentuk dan loopback dibatasi. CI sudah disiapkan, belum dipicu |

Tidak ada server yang ditinggalkan berjalan. Tidak ada koneksi atau perubahan Supabase. MySQL opsional tidak diperlukan untuk penerimaan fondasi H2.

## Serah terima dan gerbang berikutnya

Jalankan dari Git Bash di terminal lokal dengan JDK 21 dan jaringan unduhan Maven:

```bash
./mvnw --batch-mode --no-transfer-progress verify
python scripts/smoke_demo.py target/apotek-0.0.1-SNAPSHOT.jar
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Buka http://localhost:8080/login dan lakukan cek visual di SETUP.md. Catat hasil startup kedua. Pindahkan perubahan ke branch SETUP-00 sebelum commit bila masih di main. Jefry meninjau diff, author/committer (`git log -1 --format=fuller`), lalu melakukan commit, push, PR dan merge. Tandai done hanya setelah cek selesai dan hasil masuk main; baru lanjut UC-01. Review saat ini bukan persetujuan merge atau klaim CI hijau.

Usulan pesan commit: `chore(setup-00): run clinic demo on H2 without a database server`.

## Kebutuhan DB-01 yang ditunda

- Driver/profil Supabase dan JDBC backend, rahasia melalui environment; belum ditambahkan pada SETUP-00.
- Lokasi migrasi Flyway PostgreSQL terpisah; pertahankan V1/V2 MySQL historis.
- Terjemahkan tipe DATETIME/TINYINT, ekspresi REGEXP NIK, identifier camelCase, waktu Asia/Jakarta, indeks dan seluruh PK/FK/UNIQUE/CHECK.
- Regresi login, peran, kepemilikan, kunci transaksi, perebutan slot terakhir, kuota, pembatalan, check-in dan penyelesaian pada PostgreSQL uji setelah seluruh fitur dan QA-01 selesai.
