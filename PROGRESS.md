# Progres Klinik Leonardo

Diperbarui: 26 November 2026 (WIB). PIC seluruh tahap: Jefry (JL).

## Tahap aktif

**SETUP-00: review - 46 tes lulus; menunggu commit dan integrasi ke main oleh Jefry.**

Branch tujuan: `chore/setup-00-fondasi`. Branch aktual sesi: `main`; perubahan masih di working tree dan belum di-commit atau diintegrasikan. `.git` hanya dapat dibaca dalam sesi ini. Tidak ada commit, push, PR, atau merge oleh agent. Perubahan lokal awal termasuk `mvnw.cmd` dipertahankan. `.env` pribadi tidak diubah; SHA-256 awal/akhir sama dan file tetap diabaikan Git.

Instruksi AGENTS.md revisi sudah diterapkan dari versi mandiri yang dibaca dan dikonfirmasi ulang oleh Jefry. Roadmap/kartu/kontrak direkonsiliasi dengan panduan revisi versi 2 dan kode aktual. ZIP revisi `Klinik_Leonardo_Agent_Setup (1).zip` telah diidentifikasi, tetapi transfer bytes gagal karena DNS; isi PROGRESS.md asli di arsip belum dapat dibandingkan. Catatan ini dibuat berdasarkan hasil aktual workspace, bukan mengasumsikan tahap di arsip sudah selesai.

## Verifikasi lokal terbaru dan keputusan tahap berikutnya

Laporan Surefire terbaru telah diperiksa: ApotekApplicationTests 8, DefaultDemoProfileTests 1, DemoProfileTests 9, SchemaConstraintsTests 27, ServletSecurityTests 1; total **46 tes, 0 gagal, 0 error, 0 skipped**. Tes servlet dengan Tomcat sungguhan kini lulus di terminal lokal.

Log `target/demo-smoke-2.log` mencatat Tomcat berhasil startup kedua pada port 18080, 26 November 2026 pukul 20.29 WIB. Jefry mengonfirmasi aplikasi sudah bisa berjalan. Ketercapaian startup kedua berarti pemeriksaan run pertama telah dilewati skrip. Hasil akhir skrip dinyatakan berhasil berdasarkan konfirmasi Jefry; stdout PASS tidak tersimpan dalam log aplikasi. Pemeriksaan visual seluruh halaman browser belum dicatat terpisah.

Git masih di `main` pada commit `4cbba12`, dengan seluruh perubahan fondasi berada di working tree. Ini belum merupakan integrasi hasil SETUP-00 ke main. Jefry menegaskan: **tunggu commit dan integrasi SETUP-00 sebelum mulai UC-01**. Karena itu agent tidak menambahkan implementasi UC-01, melakukan commit, push, PR, atau merge. Catatan error sandbox dan cache di bagian selanjutnya adalah riwayat percobaan terdahulu, bukan hasil terbaru dari terminal lokal.

Langkah berikutnya: Jefry meninjau perubahan, menyimpannya pada branch fondasi, lalu melakukan commit dan integrasi. Setelah konfirmasi atau bukti commit tersedia, cek ulang Git, tandai SETUP-00 done, dan mulai UC-01 pada branch tahapnya.

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
| SETUP-00 | review | 46 tes lulus; startup kedua tercatat dan aplikasi dikonfirmasi berjalan. Menunggu commit/integrasi oleh Jefry; cek visual browser dan runtime JDK 21 belum dicatat |
| UC-01 | todo | Tunggu SETUP-00 terverifikasi dan masuk main; `feat/uc-01-jadwal` |
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

Belum ada implementasi UI-01 sampai UI-10. Label pemilik MF/RA/NA/AH pada kartu lama adalah asal spesifikasi; Jefry mengerjakan semua tahap. Detail branch/UC/PC/UI ada di [roadmap](docs/PROJECT.md).

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
