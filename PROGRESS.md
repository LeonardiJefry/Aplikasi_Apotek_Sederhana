# Progres Klinik Leonardo

Diperbarui: 27 November 2026 (WIB). PIC seluruh tahap: Jefry (JL).

## Tahap aktif

**UC-07: review - implementasi kuota admin dan ketersediaan publik tersedia pada feat/uc-07-kuota-vaksinasi. Verify lokal lulus 154 tes dan pembuatan JAR; browser desktop/mobile serta batas peran lulus. Smoke dua startup JAR H2 dan UC-01 sampai UC-07 lulus. Belum commit/push/CI/merge UC-07. SETUP-00 sampai UC-06: done melalui PR #6 sampai #12.**

## UC-07 - implementasi dan verifikasi kuota vaksinasi

Prasyarat diperiksa langsung: [PR #12](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/pull/12) **merged=true**, merge **afd2e6c830eea040d782ff74766cee7e4ba37b41**, head terakhir **8fc255ed**. Java CI [run 45919184878](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/actions/runs/45919184878) terkonfirmasi completed/success. Perbandingan isi 8fc255ed dengan afd2e6c8 menghasilkan `files=[]`; baseline lokal sama dengan hasil merge. UC-06 kini done. Main lokal belum di-fetch; tidak reset/clean atau mengubah berkas pribadi.

Branch aktual **feat/uc-07-kuota-vaksinasi**, dibuat Jefry setelah pembuatan ref oleh agent ditolak karena .git hanya baca. HEAD awal **8fc255e**, tracked bersih; file tambahan lokal dipertahankan. Seluruh perubahan fitur masih lokal; tidak ada staging/commit/push/PR/merge oleh agent.

Acuan dibaca langsung: Modul 2 UC-07 halaman 8, PC-07 halaman 79, UI-07 halaman 93-94; design context dan screenshot Hi-Fi desktop **38:238** dan mobile **38:317**. Memakai entitas/tabel JadwalVaksinasi, seed 80/56 dan 10/10, login/peran serta layout bersama. Tidak mengubah migrasi V1-V4, seed, .env, pom.xml atau konfigurasi database. Lima aset SVG Figma berhasil diunduh ke static/images/vaksinasi; URL sementara tidak dipakai saat runtime.

- Admin memilih tanggal ISO dan jenis vaksin, memuat layanan, lalu mengubah kuota. Hasil tanggal/jenis ganda wajib memilih ID eksplisit; tidak otomatis memakai baris pertama. ID/date/jenis dicocokkan di server. Kuota bilangan bulat 0..2147483647 dan minimal jumlahTerdaftar terbaru.
- Simpan mengunci baris JadwalVaksinasi, membaca jumlah peserta dari DB lalu memperbarui kuota dan pendaftaranDibuka secara atomik. Sisa nol = Penuh/false; sisa positif = Tersedia/true. Jumlah peserta tetap. Versi form mendeteksi perubahan kuota/jumlah/flag; dua admin bersamaan menghasilkan satu sukses dan satu konflik. Kegagalan tulis me-rollback kuota/flag, retry bekerja.
- GET /admin/vaksinasi dan POST /admin/vaksinasi/kuota dibatasi admin, identitas sesi dan CSRF. Sukses 303; invalid 400; tidak ada/tidak cocok 404 pada pemilihan; konflik kuota/versi 409; DB 503 dengan pesan umum. GET/no-store tidak menulis. Batalkan memuat ulang data tersimpan. Preview JS ditandai belum disimpan; perubahan filter menonaktifkan Simpan sampai layanan dimuat ulang.
- GET /vaksinasi dan /vaksinasi/data tersedia tanpa login, hanya angka agregat dan identitas/status layanan. Publik memperbarui ringkasan setiap 5 detik dengan timeout 8 detik dan penanda data lama. Tidak ada data pasien, NIK/noRM atau akun. Belum ada transaksi pendaftaran peserta vaksinasi pada model/roadmap; UC-07 menyediakan flag dan ketersediaannya, tanpa endpoint pendaftaran semu. Penulis peserta selanjutnya harus mengunci jadwal serta memvalidasi flag/sisa sebelum menambah jumlahTerdaftar, sesuai kontrak.

| Pemeriksaan UC-07 | Hasil aktual |
| --- | --- |
| Tes baru terarah | **BUILD SUCCESS: 14 tes**, 11 aturan/HTTP MockMvc + 3 transaksi, 0 failure/error/skipped; target/uc07-targeted-tests.log. Cakupan: kuota minimum/lebih besar/nol/overflow, input tanggal/jenis, ID ambigu, replay/form lama, CSRF/peran, snapshot publik, dua admin bersamaan, perubahan peserta, rollback dan retry |
| Git Bash verify agent | Proses keluar **-1073741502** sebelum ada keluaran Maven; target/uc07-bash-verify.log kosong. Tidak menyatakan Git Bash lokal berhasil dari percobaan ini |
| Wrapper Windows offline verify | **BUILD SUCCESS: 154 tes, 0 failure, 0 error, 0 skipped**, termasuk tiga tes HTTP yang sebelumnya terhalang loopback. JAR standar target/apotek-0.0.1-SNAPSHOT.jar berhasil dibuat; **target/uc07-verify.log**. Cache Maven 3.9.16/JDK 24.0.2, compile release 21; runtime JDK 21/CI UC-07 masih perlu hasil sesudah push |
| Paket review dan startup | Paket terpisah target/uc07-package/apotek-uc07-review.jar **BUILD SUCCESS** (packaging ini melewati tes; POM sementara di target). JAR berhasil startup normal profil demo di port **18083**, H2 V1-V4, Hibernate validate dan Tomcat; **target/uc07-package.log**, **target/uc07-startup.log**. Bukan Clock/fixture tambahan |
| Smoke executable JAR standar | **Lulus dua startup/JVM**, python scripts/smoke_demo.py target/apotek-0.0.1-SNAPSHOT.jar; **target/uc07-smoke.log**. Enam akun/empat peran, CSRF/logout serta UC-01 sampai UC-07 lulus pada kedua startup. UC-07 memverifikasi 80/56/24, tolak 55, simpan 56 menjadi Penuh/false, tolak versi lama, kembalikan 80 menjadi Tersedia/true, halaman/snapshot publik tanpa login serta larangan akses peran lain. Tidak mengimpor .env atau memakai server MySQL; proses smoke dihentikan skrip setelah tiap run |
| JavaScript/Python/aset | node --check vaksinasi.js, py_compile smoke_demo.py dan git diff --check lulus. Lima SVG nonkosong: info 20x20, syringe 24x24, shield 20x20, clock 16x16 dan chevron 20x20 |

| Pemeriksaan browser pada executable JAR demo port 18083 | Hasil aktual |
| --- | --- |
| Login dan pilihan layanan | Admin login melalui form normal; link Kelola kuota vaksinasi membuka halaman. Influenza Demo hari ini menunjukkan VAK-001, kuota **80**, terdaftar **56**, sisa **24** dan Tersedia |
| Pembatalan dan kuota minimum | Isi 70 lalu Batalkan kembali ke 80 tanpa simpan. Isi 55 ditahan validasi HTML minimum 56; penolakan POST server didukung tes/smoke tersendiri |
| Penuh dan publik | Simpan 56 sukses: **56/56, sisa 0, Penuh, Pendaftaran ditutup**. Tab publik yang sudah terbuka ikut berubah otomatis tanpa reload manual |
| Form lama | Tab kedua dengan versi awal 80 mengirim 90 sesudah tab pertama menyimpan 56: ditolak sebagai data telah berubah; ringkasan DB tetap 56/56 dan sisa 0 |
| Pembukaan kembali/pemulihan | Simpan kembali 80 menghasilkan **80/56, sisa 24, Tersedia**. Setelah logout, halaman publik dapat dimuat kembali tanpa login dan menunjukkan angka yang sama |
| Mobile dan aset | Viewport CSS aktual **390x844**, scrollWidth **371**, tanpa overflow horizontal; tombol aksi fixed dengan batas bawah **844**. Pada lebar desktop, form/ringkasan berdampingan. Empat gambar img lengkap dan ukuran alami/render sesuai 20/24/20/16; chevron SVG lokal tampil pada select |
| Tanpa JavaScript | Script sementara dinonaktifkan dan halaman dimuat ulang: instruksi noscript tampil. Submit memakai tombol HTML asli berhasil menyimpan kuota 80 dan menampilkan pesan sukses. JavaScript dan override viewport dikembalikan |
| Peran | Login pasien/loket/dokter masing-masing berhasil; membuka URL /admin/vaksinasi menghasilkan **Akses dibatasi**. Seluruh sesi uji logout |
| Refresh gagal dan pulih | Blok sementara hanya request /vaksinasi/data pada tab publik menghasilkan **Pembaruan gagal. Data terakhir belum diperbarui**. Setelah blok dilepas dan reload, indikator kembali terbaru/otomatis 5 detik. Tidak ada error JS pada log publik yang diperiksa sebelum simulasi blok; blok request dikembalikan kosong |

Inspeksi JAR standar setelah verify memastikan H2 runtime, KuotaVaksinasiBoundary, V4 dan kelima SVG tersedia; BrowserHarness/MutableClock/TimeFixture tidak masuk paket. Sesi proses uji port 18083 yang dimulai agent dihentikan dengan Ctrl+C setelah pemeriksaan; tidak menghentikan proses demo lain milik Jefry.

Keterbatasan visual yang dipertahankan: header/sidebar/brand dan font fallback adalah fondasi proyek; tidak mengubah layout bersama atau menambahkan menu laporan/akun yang belum tersedia hanya untuk menyamai Figma. Form, ringkasan, warna, SVG, ukuran input 48px dan responsivitas mengikuti UI-07; kesamaan penuh shell/font dengan Figma tidak diklaim.

Berkas fitur: controller/KuotaVaksinasiService.java; boundary/KuotaVaksinasiBoundary.java; repository/JadwalVaksinasiRepository.java; security/SecurityConfig.java; templates/vaksinasi/index.html dan fragments/vaksinasi.html; templates/index.html; static/js/vaksinasi.js, static/css/app.css, static/images/vaksinasi/{info,syringe,shield,clock,chevron}.svg; KuotaVaksinasiTests.java dan KuotaVaksinasiTransactionsTests.java; scripts/smoke_demo.py; README.md; SETUP.md; docs/KONTRAK.md; PROGRESS.md.

`.env` hash tetap **B497017DB1ABDC46A88BB9BA4B333D0254A6783FE2F68FD030995DC6A3EF7B42** dan file tambahan lokal **9864E33D974948FAC45A552BA123C5F9AB7353EB7D7169D665D1BDE5D2C50769**. Helper/POM/log/paket uji berada di target dan diabaikan Git. Usulan commit **feat(uc-07): manage vaccination quotas and public availability**. Stage hanya PROGRESS.md README.md SETUP.md docs/KONTRAK.md scripts/smoke_demo.py src; commit/push/PR/merge diserahkan kepada Jefry. UC-08 menunggu UC-07 terverifikasi dan masuk main; Supabase tetap DB-01 setelah QA-01 lulus dan masuk main.

## UC-06 - commit, push dan CI PR #12

Pemeriksaan Git lokal menunjukkan branch `feat/uc-06-panggil-lewati` pada **b3979c38037ff2ced6ea559b6cb79000652ba065**, author/committer Jefry, pesan `feat(uc-06): add persistent queue calls and atomic skipping`; branch melacak origin dengan HEAD sama. Working tree tracked bersih sebelum pembaruan dokumentasi ini; file tambahan lokal tetap dipertahankan. Tidak ada commit/push/PR/merge oleh agent.

[PR #12](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/pull/12) terkonfirmasi **open, merged=false**, base main `eb358efe`, head b3979c3. Java CI [run 45918873213](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/actions/runs/45918873213), job **140244529568 Build dan Test**, selesai **success**. Log job diperiksa langsung; CI memakai calon hasil merge PR `eae2cdaf` dengan head tersebut, bukan bukti PR telah merged.

| Verifikasi CI | Hasil aktual |
| --- | --- |
| Bash, JDK 21 dan Maven verify | Temurin 21; `./mvnw --batch-mode --no-transfer-progress verify` **BUILD SUCCESS**, **140 tes, 0 failure, 0 error, 0 skipped**. Tiga tes HTTP yang terhalang loopback lokal kini lulus di runner CI |
| Startup executable JAR | `python3 scripts/smoke_demo.py target/apotek-0.0.1-SNAPSHOT.jar` lulus untuk **dua JVM/startup** dari direktori kerja baru, H2 dan akun sintetis; tanpa server MySQL atau .env pribadi |
| Login dan batas akses | Enam akun sintetis/empat peran lulus login, penolakan lintas peran, CSRF dan logout pada kedua startup |
| Integrasi UC-01 sampai UC-06 | Smoke jadwal, booking, status, pembatalan, check-in dan panggilan lulus dua kali. Smoke UC-06 memeriksa halaman loket, snapshot read-only, larangan jadwal besok, CSRF dan isolasi peran. Jalur panggil/tunggu/lewati/replay juga lulus melalui PanggilanServletTests; alur browser ber-Clock sintetis dicatat di bawah |

Hasil CI melengkapi keterbatasan verify/startup agent pada catatan historis di bawah, tanpa mengubah hasil lokal tersebut. **UC-06 belum done karena PR belum merged.** Langkah berikut: Jefry meninjau dan merge PR #12, lalu mulai branch UC-07 dari main terbaru. Pembaruan sesi ini hanya PROGRESS.md; usulan commit dokumentasi `docs(uc-06): record successful JDK 21 CI`. Commit/push/merge tetap dilakukan Jefry; Supabase tetap DB-01 sesudah QA-01 lulus dan masuk main.

## UC-06 - verifikasi browser dan perbaikan presisi timer

Branch aktual sudah **`feat/uc-06-panggil-lewati`**, dibuat Jefry; seluruh perubahan lokal dipertahankan, belum ada commit/push/PR/merge oleh agent. Fixture uji yang dijalankan Jefry pada port **18082** berhasil dibuka melalui browser. Clock sintetis mulai 08:00 WIB dan terus berjalan; semua tindakan memakai formulir aplikasi normal dengan akun sintetis.

| Cek browser pada runtime V1-V3 | Hasil aktual |
| --- | --- |
| Mulai panggilan dan reload | JDW-003 memanggil 001, mencatat waktu panggil; tombol Lewati nonaktif sebelum batas tunggu. Reload mempertahankan waktu/token panggilan, sisa detik berkurang dan tidak kembali ke awal. Setelah batas terlewati, pembaruan server mengaktifkan Lewati |
| Modal dan pembatalan | JDW-002 menampilkan peringatan pelewatan ketiga. Batal dan Escape menutup modal tanpa mengubah dua riwayat/status Dilewati |
| Pelewatan ketiga dan pengulangan | Ya, lewati menghasilkan Tidak Hadir, riwayat **3 kali**, tanpa kandidat berikutnya. Tab formulir lama dengan JavaScript nonaktif dikirim sesudahnya: ditolak sebagai panggilan sudah diproses. Halaman jadwal pasien menunjukkan kuota JDW-002 **5/5**, bertambah tepat satu dari fixture 4/5 |
| Pelewatan pertama dan panggilan berikutnya | JDW-003 nomor 001 menjadi Dilewati, riwayat **1 kali**, lalu **002** otomatis dipanggil dengan timer baru. Nomor 001 tetap menjadi kandidat panggil ulang |
| Konfirmasi hadir | Pada panggilan 002, Pasien hadir, mulai layanan menghasilkan **Sedang Dilayani**, panggilan aktif ditutup dan panggilan baru diblokir sampai pemeriksaan selesai. Tidak ada riwayat pelewatan tambahan |
| Integrasi status/kuota | Jadwal pasien menunjukkan JDW-003 tetap **8/10**; status pasien pemilik tiket 001 menunjukkan Dilewati dan nomor yang Sedang Dilayani **002**, tanpa identitas pasien lain |
| Mobile | Viewport CSS **390 x 844**, scrollWidth **371**, tombol Lewati position fixed dengan batas bawah **828**. Tidak ada overflow horizontal; riwayat berubah menjadi daftar dan tindakan konfirmasi berhasil |
| Refresh dan data lama | Blok sementara hanya request JSON JDW-003 menghasilkan pesan data terakhir belum diperbarui. Setelah blok dilepas, refresh kembali sukses. Tidak ditemukan TypeError/ReferenceError/SyntaxError pada log browser yang diperiksa |
| Peran dan tanpa JavaScript | Pasien/admin/dokter ditolak membuka halaman loket. Form tanpa JS menampilkan konfirmasi langsung dan server menolak token lama. Semua sesi uji logout; JavaScript, blok request dan viewport dikembalikan |

Temuan nyata saat membuka panggilan pertama: timer sempat **121 detik**, karena DATETIME V3 membulatkan pecahan detik ke detik berikutnya. Perbaikan memakai migrasi maju **V4__presisi_waktu_panggilan.sql** ke DATETIME(6), tanpa mengubah V1-V3 yang sudah dipakai. Entity menormalkan waktu ke mikrodetik; snapshot membaca Clock setelah data serta membulatkan sisa durasi ke atas tanpa menghilangkan pecahan terakhir.

Tes baru menyimpan waktu **08:00:00.750123400**, membersihkan persistence context dan membaca ulang DB: timestamp tetap **08:00:00.750123**, sisa awal **120**, satu mikrodetik sebelum batas masih ditolak, tepat batas diterima. Konfirmasi hadir segera pada waktu pecahan detik juga berhasil dan waktuTutup tersimpan presisi. **19 tes terarah lulus** (14 PanggilanTests + 5 transaksi), tanpa failure/error/skipped; `target/uc06-precision-tests.log`.

Setelah Jefry me-restart launcher dan marker `UC06_UI_FIXTURE_READY` muncul kembali, cek browser runtime V4 selesai: panggilan JDW-003 menyimpan waktu **2026-11-27T08:01:02.244843 WIB** dan menampilkan sisa awal **120 detik**. Konfirmasi hadir segera berhasil menjadi **Sedang Dilayani**, menutup panggilan tanpa riwayat pelewatan dan memblokir panggilan baru selama layanan berlangsung. Pada JDW-002, konfirmasi pelewatan ketiga kembali menghasilkan **Tidak Hadir / 3 kali**. Sesi loket logout; pengaturan JavaScript, blok request dan viewport dikembalikan.

| Pemeriksaan akhir setelah V4 | Hasil aktual |
| --- | --- |
| Tes presisi dan transaksi terarah | **BUILD SUCCESS: 19 tes**, 0 failure/error/skipped; `target/uc06-precision-tests.log` |
| Verify lengkap Wrapper Windows offline | **140 tes: 137 lulus, 0 failure, 3 error, 0 skipped; BUILD FAILURE.** CheckInServletTests, ServletSecurityTests dan PanggilanServletTests gagal memulai Tomcat karena `Unable to establish loopback connection` / `Permission denied: connect`. Memakai cache Maven 3.9.16 dan JDK 24.0.2 dengan compile release 21; `target/uc06-final-verify.log`. Ini belum kelulusan verify lengkap/JDK 21 |
| Paket review setelah V4 | **BUILD SUCCESS**, `target/uc06-package/apotek-uc06-review.jar`; `target/uc06-final-package.log`. Packaging memakai POM sementara dan direktori output terpisah, **tes dilewati** pada perintah ini; bukan hasil verify. pom.xml repo tetap utuh |
| Startup agent dan startup fixture lokal | Percobaan startup JAR oleh agent sebelumnya berhasil memuat H2, migrasi V1-V3 dan validasi Hibernate, lalu **gagal HTTP** akibat loopback (`target/uc06-startup.log`). Terpisah dari itu, fixture terbaru yang dijalankan Jefry pada profil demo port 18082 **berhasil startup HTTP** dan dipakai untuk cek browser V4 di atas. Belum ada hasil smoke dua startup executable JAR terbaru |
| Git Bash dan CI | Percobaan Git Bash agent sebelumnya gagal sebelum Maven karena MSYS `NtCreateDirectoryObject / 0xC0000022`; fallback offline Windows menghasilkan hasil tes di atas. Belum ada commit/push UC-06, sehingga belum ada hasil CI UC-06 pada JDK 21 |

Pemeriksaan panggil ulang setelah status Selesai tetap lewat tes layanan dengan simulasi hasil UC-10, karena aksi Selesai/dashboard UC-09/10 belum diimplementasikan. Tidak membuat jalan pintas status hanya untuk pemeriksaan browser. Hasil akhir di atas melengkapi catatan historis sebelum V4 di bawah; tidak mengubah hasil lama menjadi kelulusan verify baru.

Sesi lanjutan berhasil mengakses kembali workspace dan mencocokkan log akhir dengan branch `feat/uc-06-panggil-lewati`, HEAD `d0d0601`. Inspeksi arsip JAR terbaru memastikan H2 runtime, PanggilanBoundary serta migrasi V3/V4 tersedia; BrowserHarness/MutableClock/TimeFixture tidak masuk paket. `git diff --check` lulus. Hash `.env` tetap **B497017DB1ABDC46A88BB9BA4B333D0254A6783FE2F68FD030995DC6A3EF7B42**; file tambahan lokal tetap **9864E33D974948FAC45A552BA123C5F9AB7353EB7D7169D665D1BDE5D2C50769**. Tidak ada staging/commit/push/merge oleh agent. Proses fixture dimulai Jefry; hentikan terminal port 18082 dengan Ctrl+C setelah selesai memakai demo.

Tambahan berkas sesi ini: V4__presisi_waktu_panggilan.sql; penyesuaian entity/PanggilanAntrean.java, controller/PanggilanService.java, PanggilanTests.java; README.md, SETUP.md, docs/KONTRAK.md dan PROGRESS.md. `.env` serta file tambahan lokal tetap dipertahankan. UC-07 tetap menunggu UC-06 terverifikasi dan masuk main; Supabase tetap DB-01.

## Riwayat implementasi UC-06 sebelum pemeriksaan browser

Prasyarat terverifikasi melalui GitHub: PR [#11](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/pull/11) sudah merged ke main (`eb358efe`). Java CI [run 45911450886](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/actions/runs/45911450886), job Build dan Test, selesai success pada JDK 21: **120 tes, 0 failure/error/skipped**, BUILD SUCCESS, smoke dua startup JAR dan enam akun/peran/CSRF/logout serta UC-01 sampai UC-05 lulus. Perbandingan d0d0601 dengan merge eb358efe menghasilkan daftar file kosong. Keterbatasan verify agent UC-05 sebelumnya kini dilengkapi bukti CI, sehingga UC-05 sudah done.

Branch tujuan **`feat/uc-06-panggil-lewati`**. Branch aktual tetap `feat/uc-05-check-in` pada `d0d0601`; percobaan membuat branch UC-06 gagal membuat ref lock karena `.git` hanya baca. Working tree awal tracked bersih; file tambahan lokal dipertahankan. Implementasi menggunakan isi baseline UC-05 yang sama dengan hasil merge, tanpa fetch/reset/clean. Jefry dapat membawa seluruh perubahan lokal ke branch baru dengan `git switch -c feat/uc-06-panggil-lewati` sebelum commit. Tidak ada commit/push/PR/merge oleh agent.

Acuan dibaca langsung: Modul 2 revisi UC-06 PDF halaman 7, PC-06 halaman 77-79 dan UI-06 halaman 90-92; Figma design context/screenshot desktop `89:780`, mobile `89:798`, modal `93:749`. Layout/identitas/sesi/model bersama dipakai ulang. Salah satu teks Figma menyebut Tidak Hadir setelah dua pelewatan; implementasi mengikuti kontrak dan PC-06: **ketiga**, dengan kesempatan ulang setelah pelewatan 1 dan 2.

- V3 menambah entitas/repository `PanggilanAntrean`: satu metadata panggilan terakhir per jadwal, token UUID unik, waktuPanggil, hasil dan waktuTutup, serta ID aktor dari sesi. Reload tidak mereset timer. V1/V2, seed, delapan status tiket dan konfigurasi DB tetap utuh.
- PanggilanService dipakai loket dan disediakan untuk integrasi dokter UC-09/10. Dokter dibatasi ke jadwal miliknya pada layanan; endpoint UI saat ini hanya prefix loket. Kandidat regular mengikuti nomor, lalu kesempatan ulang Dilewati. Setelah pasien berikutnya selesai, Dilewati didahulukan. Tidak memanggil ketika masih ada panggilan aktif atau Sedang Dilayani; tidak membuat aksi Selesai sebelum UC-10.
- GET halaman/JSON read-only. POST panggil/hadir/lewati memakai CSRF; hadir/lewati wajib token panggilan aktif, lewati wajib konfirmasi. Jam layanan hari yang sama dan waktu server sesudah kunci diperiksa ulang. Lewati ditolak pada 119 detik, diizinkan tepat 120 detik. Form lama tidak bisa memproses panggilan baru, termasuk tiket sama yang dipanggil ulang.
- Kunci JadwalPraktik lalu Antrean menjaga konsistensi dengan UC-02/04/05. Pelewatan 1/2 menjadi Dilewati; ketiga Tidak Hadir dan satu kuota dikembalikan. Riwayat, status, kuota, penutupan panggilan dan panggilan kandidat berikutnya atomik. Dua skip bersamaan, hadir melawan skip, dan panggil ganda diuji. Trigger kegagalan kuota/panggilan berikutnya membuktikan rollback seluruh operasi lalu retry aman.
- UI menyediakan pilihan jadwal, nomor aktif, nama/noRM untuk loket, timer server, konfirmasi hadir, modal Batal/Escape/Ya lewati, kandidat berikutnya dan riwayat. Riwayat memakai label Terakhir dilewati untuk timestamp yang memang tersedia. Polling 5 detik, timeout 8 detik, indikator data lama dan tombol nonaktif saat refresh gagal; mobile dirancang sticky/list; tanpa JS konfirmasi tampil langsung dan reload memperbarui kelayakan. Kesesuaian browser belum dinyatakan lulus.
- GET/JSON/galat no-store; 400 input tidak valid, 404 jadwal tidak ada/bukan milik dokter pada layanan, 403 peran/CSRF, 409 status/waktu/token dan 503 galat DB tanpa rincian koneksi. Tidak menambah notifikasi UC-10 atau koneksi PostgreSQL.

| Pemeriksaan UC-06 | Hasil aktual |
| --- | --- |
| Kompilasi dan tes baru terarah | BUILD SUCCESS, **17 tes**: 12 PanggilanTests dan 5 PanggilanTransactionsTests, tanpa failure/error/skipped; `target/uc06-tests.log`. Kegagalan awal satu tes akibat konteks autentikasi tes setelah MockMvc telah diperbaiki pada setup tes, bukan melemahkan layanan |
| Git Bash verify | Gagal sebelum Maven: MSYS NtCreateDirectoryObject / 0xC0000022; `target/uc06-bash-verify.log` |
| Verify lengkap Wrapper Windows offline | **138 tes, 0 failure, 3 error, 0 skipped**, BUILD FAILURE. CheckInServletTests, ServletSecurityTests dan PanggilanServletTests gagal memulai Tomcat karena loopback `Permission denied: connect`; 135 tes lain lulus. JDK 24.0.2 dengan compile release 21 dan Mockito agent cache; `target/uc06-verify.log` |
| Verify tanpa tiga kelas servlet | **135 tes, 0 failure/error/skipped**. Tahap repackage kemudian gagal rename JAR standar, sehingga **bukan BUILD SUCCESS verify**; `target/uc06-nonservlet-verify.log`. Proses demo Jefry masih berjalan; paket review dibuat di lokasi lain |
| Paket review terpisah | **BUILD SUCCESS**, `target/uc06-package/apotek-uc06-review.jar`, menggunakan POM sementara pada target dengan sumber/dependensi yang sama dan direktori output lain. Tes dilewati pada packaging ini, hasil 135 tes di atas terpisah. pom.xml repo tidak diubah. H2 runtime, V3 dan boundary UC-06 ada pada JAR; helper browser/Clock tes tidak masuk JAR. `target/uc06-package-review.log` |
| Startup JAR review | H2 berhasil dimuat, **V1-V3** diterapkan dan Hibernate validate berhasil. Tomcat gagal membuka selector loopback (`Permission denied: connect`), sehingga startup HTTP **gagal**. `target/uc06-startup.log`; bukan kegagalan migrasi/model |
| HTTP, smoke JAR dan browser UC-06 | Belum lulus dari sesi ini. PanggilanServletTests sudah disediakan untuk HTTP nyata; skrip smoke ditambah cek halaman, snapshot, penolakan jadwal besok, CSRF dan peran UC-06. Skrip smoke runtime terbaru dan browser perlu server UC-06 pada lingkungan yang mengizinkan loopback |
| JavaScript/Python dan whitespace | node --check, py_compile dan git diff --check lulus. `.env` tetap hash B497017D...EF7B42; file tambahan lokal tetap 9864E33D...C50769 |

Keterbatasan UI: unduhan aset SVG Figma gagal DNS, termasuk ikon alert; tidak menambahkan URL sementara, gambar kosong, atau ikon pengganti yang diklaim sama. Header/sidebar/font fallback fondasi tetap digunakan. Kesesuaian penuh ikon/font/layout Figma belum lulus. Cek browser desktop/mobile, countdown/reload, Batal/Escape, submit sah/ulang, peran, panggilan berikutnya, serta status/kuota ketiga masih perlu dilakukan pada runtime terbaru; tidak memakai server 8080/fixture UC-05 lama sebagai bukti UI UC-06.

Helper sementara `target/Uc06BrowserHarness.java` sudah dikompilasi dan launcher `target/uc06-browser-demo.sh` disiapkan. Jalankan dari Git Bash lokal dengan `bash target/uc06-browser-demo.sh`, atau dari CMD `"C:\Program Files\Git\bin\bash.exe" target/uc06-browser-demo.sh`. Marker `UC06_UI_FIXTURE_READY` berarti runtime uji port **18082** siap: Clock sintetis mulai 08:00 WIB dan terus berjalan, JDW-003 memiliki dua kandidat untuk timer/pelewatan/panggilan berikutnya; JDW-002 memiliki dua riwayat dan panggilan >120 detik untuk uji pelewatan ketiga. Ini H2/JVM terpisah, tidak mengubah jam komputer, seed atau JAR normal, dan tidak ada endpoint pengubah waktu/data. Helper/launcher/POM paket sementara diabaikan Git.

Berkas UC-06: boundary/PanggilanBoundary.java; controller/PanggilanService.java dan PanggilanException.java; entity/PanggilanAntrean.java; repository/PanggilanAntreanRepository.java, AntreanRepository.java dan RiwayatPelewatanRepository.java; V3__panggilan_antrean.sql; templates/panggilan/index.html, index.html dan checkin/loket.html; static/js/panggilan.js dan static/css/app.css; PanggilanTests, PanggilanTransactionsTests, PanggilanServletTests dan ApotekApplicationTests; scripts/smoke_demo.py; README.md; SETUP.md; docs/KONTRAK.md; PROGRESS.md.

Usulan commit: `feat(uc-06): add persistent queue calls and atomic skipping`. Stage berkas fitur secara sengaja dengan `git add PROGRESS.md README.md SETUP.md docs/KONTRAK.md scripts/smoke_demo.py src`, tinjau diff lalu commit/push/PR/merge oleh Jefry. Selesaikan cek tertunda dan CI sebelum menutup UC-06; UC-07 menunggu integrasinya ke main. DB-01 tetap setelah QA-01 lulus dan masuk main.

## Penyelesaian cek browser UC-05 - fixture terpisah pukul 08:00 WIB

Jefry menjalankan launcher fixture dari terminal lokal hingga `UC05_UI_FIXTURE_READY`. Pemeriksaan melalui browser memakai JVM/H2 uji di port **18081**, Clock tetap 27 November 2026 pukul 08:00 WIB dan jadwal sintetis JDW-003 hari ini. Server demo normal port 8080 terpisah; jam komputer dan kode aplikasi tidak diubah. Branch aktual `feat/uc-05-check-in`, HEAD `a5aa8e4`; tidak ada push/PR/merge oleh agent.

| Pemeriksaan fixture melalui browser | Hasil aktual |
| --- | --- |
| Peringatan sebelum check-in | Loket melihat kartu **001 belum check-in** pada Poli Umum/dr. Demo Satu/Pagi hari ini. Kartu tidak memuat identitas pasien |
| Tutup, refresh dan pulihkan | Tutup peringatan menyembunyikan kartu; setelah refresh kartu tetap tersembunyi dan tombol pemulihan tampil. Tampilkan peringatan yang disembunyikan mengembalikan kartu. Pasien masih melihat tiket Terdaftar sesudah tindakan loket ini |
| Kelayakan pasien | Akun pasien membuka Check-in hari ini dan melihat ANT-DEMO0006, nomor **001**, Terdaftar, jendela **07:30-14:00**, waktu server **08:00:00** dan tombol aktif |
| Check-in sukses | Klik Check-in sekarang melalui formulir normal menghasilkan halaman sukses: **Hadir/Menunggu**, nomor tetap **001**, waktu kehadiran **2026-11-27 08:00:00 WIB** tersimpan. Tombol check-in kemudian nonaktif; tahap kehadiran berubah menjadi tercatat |
| Peringatan setelah check-in | Refresh loket menghilangkan kartu 001, menampilkan Tidak ada peringatan check-in saat ini dan menyembunyikan tombol pemulihan |
| Reload hasil | Muat ulang halaman hasil mempertahankan status Hadir/Menunggu, nomor 001 dan waktu kehadiran 08:00:00; tidak menulis kehadiran baru |
| Formulir lama dan validasi ulang server | Sebelum check-in, siapkan tab kedua dengan formulir sah serta JavaScript sementara nonaktif agar tidak diperbarui polling. Sesudah sukses di tab pertama, kirim formulir lama melalui tombol asli: server menampilkan **Check-in ditolak. Tiket harus berstatus Terdaftar dan belum pernah check-in.** Membaca ulang hasil tetap Hadir/Menunggu, 001, 08:00:00. Ini uji form lama nyata, bukan sekadar tombol hasil yang sudah disabled |
| Kuota lintas UC | Halaman jadwal setelah sukses dan penolakan pengulangan menunjukkan JDW-003 tetap **9/10**, sesuai fixture awal; JDW-001 tetap 6/10. Check-in tidak menambah atau mengurangi kuota |
| Pembersihan browser | JavaScript dan override viewport dikembalikan, pasien dan loket logout, tab formulir lama ditutup. Tidak menyimpan screenshot sebagai berkas deliverable |

Pemeriksaan visual sukses dan interaksi kartu yang tertunda pada sesi sebelumnya kini selesai. Batas waktu inklusif, rollback/persaingan transaksi dan penolakan lintas peran tetap didukung hasil tes Java yang tercatat terpisah; fixture visual ini tidak mengubah hasil verify lengkap terbaru yang masih gagal pada dua tes servlet karena pembatasan loopback proses agent. Tidak menjalankan ulang Maven karena source aplikasi tidak berubah. Runtime JDK 21 dan CI UC-05 tetap perlu hasil aktual tersendiri. UC-06 baru dibuka setelah UC-05 terverifikasi dan masuk main; PostgreSQL Supabase tetap DB-01 sesudah QA-01 lulus dan masuk main.

Proses fixture dimulai Jefry. Agent tidak dapat membaca metadata proses melalui CIM untuk mengidentifikasi dan menghentikannya secara aman; hentikan terminal fixture **18081** dengan Ctrl+C setelah pemeriksaan. Jangan menghentikan demo normal 8080 jika masih dipakai. Data fixture ini akan hilang setelah JVM dihentikan.

Berkas tracked yang berubah hanya **PROGRESS.md**; `.env` dan file tambahan lokal tetap memiliki hash yang sama. `git diff --check` lulus. Tidak ada staging/commit/push/PR/merge oleh agent. Usulan commit dokumentasi: `docs(uc-05): record completed browser verification`.

## Riwayat verifikasi browser UC-05 - server normal sebelum fixture

Jefry menjalankan executable JAR demo melalui Git Bash lokal; aplikasi berhasil diakses di `http://localhost:8080`. Ini membuktikan startup HTTP melalui terminal Jefry berhasil, terpisah dari percobaan startup/verify agent yang gagal karena izin loopback. Branch tetap `feat/uc-05-check-in`, commit `a5aa8e4`, belum di-push sesuai keterangan Jefry. Tidak ada perubahan kode aplikasi atau pengulangan tes Java pada sesi browser ini.

| Pemeriksaan browser aktual | Hasil |
| --- | --- |
| Login, password salah dan logout | Login pasien, pasien2, loket, admin dan dokter berhasil. Password sintetis salah ditolak. Setelah logout, membuka `/pasien/check-in` kembali ke `/login` |
| Batas empat peran | Pasien, loket, admin dan dokter masing-masing ditolak saat membuka tiga area peran lain, termasuk URL check-in pasien/loket. Halaman akses dibatasi tampil tanpa data pasien |
| Kepemilikan tiket | Pasien2 membuka tiket check-in milik pasien pertama: Tiket tidak ditemukan, tanpa detail tiket |
| Dataset dan alur normal | Fixture sintetis tampil. Buat satu booking melalui jadwal/konfirmasi normal: tiket `WF856C4ZYC77`, nomor 005, JDW-001 hari ini, Terdaftar; kuota 6 menjadi 5 |
| Sebelum jendela check-in | Waktu server sekitar 06:10-06:16 WIB; jadwal 08:00-14:00 membuka check-in 07:30-14:00. Halaman menolak, tombol nonaktif, nomor/status tetap 005/Terdaftar; waktu kehadiran belum tercatat |
| Halaman hasil sebelum check-in | `/check-in/hasil` menampilkan Kehadiran belum tercatat; HTTP **409** teramati pada respons navigasi browser |
| Desktop dan mobile | Kartu jadwal/nomor/status/jendela tampil. Viewport CSS terukur **390 x 844**, scrollWidth 371 <= innerWidth 390, tanpa overflow horizontal. Tombol bawah memakai position fixed, batas bawah 828 < 844, tetap nonaktif sebelum waktunya |
| Polling, refresh dan data lama | Waktu/status diperbarui dan refresh manual berhasil. Blok sementara hanya request JSON tiket ini menghasilkan pesan data terakhir belum diperbarui, tombol tetap nonaktif. Setelah blok dilepas, refresh kembali berhasil. Tidak ditemukan TypeError/ReferenceError/SyntaxError pada log browser yang diperiksa |
| Tanpa JavaScript | Setelah script sementara dinonaktifkan dan reload, instruksi muat ulang muncul, tombol refresh JS tersembunyi, tombol check-in tetap nonaktif sesuai hasil server. JavaScript lalu diaktifkan kembali dan halaman dimuat ulang |
| Loket | Halaman dan refresh peringatan berhasil, keadaan kosong tampil sesuai waktu sebelum jendela dibuka. Kartu berisi peringatan belum tersedia; tutup/pulihkan kartu dan hilangnya kartu setelah check-in belum diuji visual |
| Pemulihan data dan browser | Tiket pengujian dibatalkan melalui konfirmasi UC-04 normal: Dibatalkan, kuota kembali **6/10**. Riwayat tiket sintetis tetap ada sampai JVM di-restart. Pengaturan viewport, blok request, dan JavaScript dikembalikan; sesi uji logout. Server milik Jefry tetap berjalan |

Belum menyatakan check-in sukses/replay atau interaksi kartu peringatan lulus browser: waktu nyata belum mencapai jendela. Hasil aturan/transaksi dan HTTP dengan Clock terkendali dari sesi sebelumnya tetap tercatat terpisah di bawah. Untuk menuntaskan visual tanpa mengubah jam komputer atau aplikasi normal, helper fixture sementara sudah dikompilasi di `target/uc05-browser-classes` dan disediakan launcher **`bash target/uc05-browser-demo.sh`**. Fixture menjalankan H2/JVM terpisah di port 18081 dengan Clock 08:00 WIB serta jadwal sintetis JDW-003 hari ini; bukan opsi pada JAR normal, tidak mengubah data proses 8080, dan tidak masuk commit. Launcher perlu dijalankan dari Git Bash lokal Jefry karena pembatasan loopback pada proses agent belum berubah. Lanjutkan sukses check-in, reload/replay, serta tutup/pulihkan/hilangnya peringatan pada fixture tersebut, kemudian hentikan proses fixture. UC-06 tetap menunggu UC-05 terverifikasi dan masuk main; Supabase tetap DB-01 sesudah QA-01.

Perubahan tracked sesi ini hanya PROGRESS.md. `.env` dan file tambahan lokal memiliki hash yang sama dengan catatan sebelumnya; tidak ada staging/commit/push/PR/merge oleh agent. Usulan commit dokumentasi setelah pengecekan: `docs(uc-05): record live browser verification`.

## Status Git terbaru - setelah commit oleh Jefry

Branch aktual sudah `feat/uc-05-check-in`, HEAD `a5aa8e4` (`feat(uc-05): add patient check-in and counter alerts`). Author dan committer Jefry; commit dibuat Jefry pada 27 November 2026. Saat pemeriksaan awal sesi ini, seluruh berkas tracked bersih; file tambahan lokal tetap tidak dilacak dan dipertahankan. Jefry menyatakan UC-05 **belum di-push**. Tidak ada push, PR atau merge oleh agent, dan belum ada bukti UC-05 masuk main. Catatan branch UC-04 di bagian sebelumnya merupakan riwayat sebelum commit ini.

Langkah aktif tetap menyelesaikan verifikasi UC-05. Untuk membuka pemeriksaan browser yang masih tertunda, jalankan JAR yang sudah dibuat dari terminal Git Bash lokal dengan `java -jar target/apotek-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo`, lalu biarkan proses berjalan. Perintah ini memakai H2 runtime tanpa MySQL atau Supabase. Kegagalan loopback pada proses yang dijalankan agent sudah dicatat; jangan mengulang tes yang sama tanpa perubahan lingkungan. Sesudah server lokal dapat dibuka, lanjutkan cek login/peran serta UC-05 desktop/mobile. UC-06 menunggu UC-05 terverifikasi dan masuk main sesuai roadmap.

## Verifikasi ulang UC-05 - 27 November 2026, sesi lanjutan

Sesuai arahan Jefry, lanjut dari UC-05; tidak mengulang implementasi UC-01 sampai UC-04 atau membuka UC-06. Acuan lokal AGENTS.md, roadmap, kontrak, kartu JL/NA, dan CONTRIBUTING.md sudah dibaca. ZIP revisi `Klinik_Leonardo_Agent_Setup.zip` teridentifikasi sebagai versi 2, tetapi pengambilan byte terbaru gagal DNS; kecocokan isi ZIP tidak dapat diverifikasi ulang. Instruksi lokal sudah menetapkan demo/test H2, PIC tunggal JL, dan DB-01 setelah QA-01 lulus serta masuk main.

Branch aktual tetap `feat/uc-04-batal-antrean` pada `830c6a0`. Percobaan `git switch -c feat/uc-05-check-in` gagal membuat ref lock karena `.git` hanya baca. Seluruh perubahan lokal UC-05 dan file tambahan pribadi dipertahankan; tidak ada staging, commit, push, PR atau merge. Konfirmasi merge/CI pada catatan di bawah berasal dari sesi sebelumnya, bukan pemeriksaan GitHub baru.

Audit konfigurasi: H2 **sudah** berscope runtime dalam pom.xml; profil default demo, profil mysql opsional, migrasi V1/V2, seed enam akun/empat peran, dan CI verify + smoke tanpa service database sudah tersedia. Tidak perlu menambahkan konfigurasi duplikat. Profil demo/test tidak mengimpor `.env`. Tidak mengubah migrasi, seed, login, layout bersama atau konfigurasi Supabase pada sesi ini; kode UC-05 yang ada ditinjau dan dipertahankan.

| Pemeriksaan sesi terbaru | Hasil aktual |
| --- | --- |
| Git Bash `./mvnw --batch-mode --no-transfer-progress verify` | Gagal sebelum Maven: MSYS `NtCreateDirectoryObject ... 0xC0000022`. Log `target/uc05-resume-bash-verify.log` |
| Maven/cache lokal | Maven 3.9.16 tersedia di cache Wrapper; tidak ada `mvn` pada PATH. JDK tersedia 24.0.2, compile release 21. Cache default `.m2/repository` dan settings lokal `target/offline-settings.xml` dapat menjalankan Maven offline. Cache alternatif `target/maven-cache` tidak lengkap; tidak dipakai pada hasil tes berikut |
| Verify lengkap melalui Wrapper Windows offline | **BUILD FAILURE: 120 tes, 0 failure, 2 error, 0 skipped.** 118 tes lulus. Dua error: CheckInServletTests dan ServletSecurityTests gagal memulai Tomcat karena `Unable to establish loopback connection` / `Permission denied: connect`. Log `target/uc05-resume-verify.log` |
| Aturan, transaksi, autentikasi dan fixture tanpa servlet nyata | CheckInTests 14 dan CheckInTransactionsTests 3 lulus, termasuk batas waktu, kepemilikan/peran, CSRF, aksi berulang, persaingan transaksi dan rollback. Tes profil default/demo, enam akun sintetis, login empat peran, penolakan lintas peran dan seed/migrasi berulang juga lulus melalui H2/MockMvc; ini bukan hasil browser |
| Verify terbatas dan pembuatan JAR | **BUILD SUCCESS: 118 tes, 0 failure/error/skipped**, dengan `-Dtest=!CheckInServletTests,!ServletSecurityTests`. Dua kelas servlet secara eksplisit dikecualikan; hasil ini bukan kelulusan verify lengkap. Executable JAR dibuat ulang. Log `target/uc05-resume-nonservlet-verify.log` |
| Startup JAR profil demo | **Gagal membuka HTTP.** H2 runtime berhasil dimuat, Flyway V1/V2 diterapkan dan Hibernate validate berjalan; Tomcat kemudian gagal membuka selector loopback dengan izin koneksi ditolak. Log `target/uc05-resume-startup.log`. Tidak menyatakan login/seed melalui JAR berhasil pada sesi ini |
| Browser desktop/mobile dan JavaScript | Koneksi Edge tersedia kembali. Upaya membuka aplikasi menghasilkan connection refused karena server tidak hidup. Fixture UI sementara juga gagal startup pada batas loopback yang sama. Tidak ada hasil visual, polling, sticky button atau interaksi kartu yang dinyatakan lulus |
| Berkas pribadi dan whitespace | Hash `.env` tetap sama dengan catatan sebelum pekerjaan (`B497017D...EF7B42`); hash file tambahan lokal tetap `9864E33D...C50769`. `git diff --check` lulus |

Hasil sukses 120 tes dan HTTP pada bagian riwayat di bawah tetap merupakan hasil sesi sebelumnya. Hasil terbaru di atas tidak menggantikannya dengan klaim sukses baru. Langkah berikut: jalankan verify lengkap dan smoke JAR dari terminal lokal yang mengizinkan loopback, lalu cek UC-05 desktop/mobile (sukses, penolakan, polling dan tutup/pulihkan peringatan). Runtime Java 21/CI UC-05 tetap belum diverifikasi di sesi ini. Setelah review selesai, Jefry memindahkan working tree ke branch UC-05 dan melakukan integrasi; UC-06 baru dimulai sesudah itu. DB-01 tetap menunggu QA-01 lulus dan masuk main.

Perubahan sesi lanjutan: PROGRESS.md dan penjelasan fallback pada SETUP.md. Daftar implementasi UC-05 serta usulan commit tetap pada bagian berikut. Tidak ada proses server UI yang dibiarkan berjalan setelah startup gagal.

## Riwayat implementasi UC-05 sebelum verifikasi ulang

Branch tujuan: `feat/uc-05-check-in`. Branch aktual sesi: `feat/uc-04-batal-antrean` pada `830c6a0`. PR [#10](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/pull/10) terkonfirmasi merged ke main (`a90723d`) dan Java CI UC-04 sukses. Perbandingan isi kedua commit melalui GitHub menghasilkan daftar berkas kosong. Perubahan tracked awal bersih; satu file tambahan lokal tak dilacak pada root dengan nama diawali `tration and restore quota atomically` dipertahankan dan hash akhirnya tetap sama. File tersebut tidak termasuk perintah staging yang disarankan. Main lokal belum diperbarui; pembuatan branch UC-05 ditolak karena `.git` hanya baca di sesi agent. Tidak ada commit/push/PR/merge oleh agent; `.env` tidak diubah.

### UC-05 - implementasi dan verifikasi sesi sebelumnya

Acuan dibaca: Modul 2 revisi UC-05 hlm. 7-8, PC-05 hlm. 89-91, UI-05 hlm. 105; design context/screenshot Figma desktop `89:579`, mobile `89:597` dan peringatan loket `93:737`. Memakai ulang entitas, sesi, layout dan seed sintetis.

- CheckInPolicy dipakai bersama oleh kelayakan UC-03 dan penulisan UC-05: Terdaftar, waktuCheckIn null, hari sama, >= 30 menit sebelum mulai dan <= jam selesai. Kedua batas inklusif; jendela yang secara aritmetika mulai pada hari sebelumnya tetap menolak check-in beda hari.
- CheckInService membaca ID jadwal skalar milik pasien sesi, mengunci JadwalPraktik lalu Antrean, memeriksa ulang pemilik/jadwal/status/waktu tersimpan, kemudian membaca Clock server. Satu transaksi mengubah Hadir/Menunggu dan menyimpan waktuCheckIn. Ulang ditolak tanpa menimpa waktu pertama. Kuota, nomor, tanggal booking dan waktu selesai tetap.
- Halaman `/pasien/check-in` memilih tiket Terdaftar hari ini atau menampilkan keadaan kosong. Detail/POST `/pasien/antrean/{id}/check-in`, hasil `/hasil`, JSON `/data`. GET tidak menulis data; POST memakai CSRF dan identitas sesi, mengabaikan ID pasien/jadwal/waktu dari klien. Sukses 303, input tidak valid 400, tiket bukan milik pasien/tidak ada 404, peran lain 403, syarat gagal 409, gangguan DB 503 tanpa rincian koneksi.
- Halaman menampilkan nomor/status/jadwal, jendela check-in, tahap kehadiran dan waktu server/kehadiran. Tindakan mobile dirancang tetap di bawah layar. Polling 5 detik, timeout 8 detik, refresh manual dan penanda data lama; submit dinonaktifkan saat data belum segar. Tanpa JavaScript gunakan reload.
- LoketCheckInService menyediakan `/loket/check-in` dan `/data` hanya untuk PETUGAS_LOKET, dengan pemeriksaan peran di layanan. Definisi operasional mendekati giliran karena modul tidak memberi angka: dua nomor aktif terdepan yang belum Sedang Dilayani pada jadwal hari ini selama jendela terbuka; peringatan hanya untuk Terdaftar yang waktuCheckIn masih null. Hadir/Menunggu/Menunggu/Dilewati tetap menempati posisi. Peringatan membawa nomor/jadwal tanpa identitas/ID tiket pasien lain.
- Peringatan dibaca ulang dan hilang setelah kehadiran tercatat. Tutup peringatan hanya menyembunyikan kartu lokal, tanpa mutasi server. Tidak membuat panggilan, pelewatan, notifikasi UC-10, skema/migrasi baru, atau perubahan seed/login. UC-06 masih menunggu tahap ini diverifikasi dan masuk main.

| Pemeriksaan UC-05 | Hasil aktual |
| --- | --- |
| Maven Wrapper Windows 3.9.16 offline verify | **BUILD SUCCESS, 120 tes, 0 gagal/error/skipped**: 102 regresi + 14 CheckInTests + 3 CheckInTransactionsTests + 1 CheckInServletTests. JDK 24.0.2, compile release 21, Mockito Java agent cache. `target/uc05-verify.log` |
| Git Bash verify | Terhalang sebelum Maven oleh MSYS `NtCreateDirectoryObject ... 0xC0000022`. Fallback Wrapper Windows di atas lulus; `target/uc05-bash-verify.log` |
| Aturan dan transaksi H2 | Lulus: kedua batas inklusif, di luar rentang/beda hari, status/timestamp tidak valid, validasi ulang sesudah preview, pemilik/peran/CSRF, spoofing input, kuota/nomor/tanggal booking tidak berubah, dua thread menghasilkan satu check-in, rollback status dan timestamp lalu retry berhasil |
| HTTP servlet dengan Clock tes | Lulus: Tomcat pada port acak, login/CSRF, check-in sukses, hasil Hadir/Menunggu dengan waktu 08:00, replay ditolak dan waktu pertama tetap. Database H2 khusus terpisah |
| Startup executable JAR dengan waktu nyata | Dua startup baru lulus: enam akun/peran/CSRF/logout, UC-01 sampai UC-05. Saat dijalankan di luar jendela, check-in hari ini/tiket besok ditolak dan status/waktu tidak berubah. `target/uc05-smoke.log`; pengulangan JAR terakhir dicatat di `target/uc05-smoke-final.log` |
| HTTP fixture runtime terkontrol | Lulus: peringatan JDW-003/001 ada sebelum check-in, lalu hilang; status Hadir/Menunggu dan waktu 2026-11-27 08:00:00 tersimpan, replay ditolak, kuota tetap 9. `target/uc05-controlled-http.log` |
| Package setelah penyempurnaan HTML/CSS/JavaScript | BUILD SUCCESS; `target/uc05-package-final.log`. Package tidak menjalankan ulang tes Java; 120 hasil verify di atas terpisah |
| Browser desktop/mobile dan interaksi JavaScript | **Belum terverifikasi.** Kontrol browser gagal sebelum tab dibuat karena native pipe tidak tersedia. Dicoba ulang setelah Jefry menyambungkan ulang, masih gagal. Tidak ada klaim screenshot, tombol sticky, polling atau interaksi kartu sudah lulus browser |
| Sintaks JavaScript/Python, whitespace, berkas pribadi | node --check, py_compile dan git diff --check lulus; hash `.env` dan file tambahan lokal sebelum/sesudah sama |

Clock terkendali hanya digunakan konfigurasi tes dan helper Java sementara di target. Helper browser/runtime HTTP memulai kelas aplikasi dengan Clock 08:00 dan fixture jadwal sintetis; bukan opsi pada JAR demo normal dan bukan bukti visual browser. Tidak menambah endpoint untuk mengubah status/waktu atau melewati aturan. Tes core di-rollback, fixture transaksi/servlet dibersihkan; proses helper telah dihentikan. Tidak ada perubahan MySQL/Supabase.

Keterbatasan review: pemeriksaan visual desktop/mobile dan interaksi JavaScript masih perlu dilakukan setelah kontrol browser pulih. Lima unduhan SVG Figma timeout; tidak memasang aset kosong atau URL sementara. Struktur kartu memakai token/layout dan font fallback fondasi; kesesuaian penuh ikon/font/layout bersama belum tercapai. Runtime JDK 21 dan CI UC-05 belum dijalankan dari sesi ini. Status review bukan klaim seluruh pemeriksaan selesai; UC-06 tetap menunggu verifikasi dan integrasi UC-05.

Daftar berkas UC-05: boundary/CheckInBoundary.java dan StatusAntreanBoundary.java; controller/CheckInService.java, CheckInPolicy.java, CheckInException.java, LoketCheckInService.java dan StatusAntreanService.java; repository/AntreanRepository.java; templates/checkin/index.html dan loket.html; templates/index.html dan antreanpasien/status.html; static/js/check-in.js; static/css/app.css; CheckInTests.java, CheckInTransactionsTests.java dan CheckInServletTests.java; scripts/smoke_demo.py; README.md; SETUP.md; docs/KONTRAK.md; PROGRESS.md.

Sebelum commit, Jefry dapat membawa working tree ke branch tujuan dengan `git fetch origin` lalu `git switch --no-track -c feat/uc-05-check-in origin/main`. Untuk mengecualikan file tambahan lokal, stage hanya `PROGRESS.md README.md SETUP.md docs/KONTRAK.md scripts/smoke_demo.py src`, lalu tinjau `git diff --cached`. Usulan commit: `feat(uc-05): add patient check-in and counter alerts`. Commit, pemeriksaan `git log -1 --format=fuller`, push, PR dan merge tetap oleh Jefry. Selesaikan cek visual yang tertunda sebelum menutup tahap dan lanjut UC-06.

## Riwayat UC-04 sebelum integrasi

Branch tujuan: `feat/uc-04-batal-antrean`. Branch aktual sesi: `feat/uc-03-status-antrean` pada `1aea612`; working tree awal bersih. PR [#9](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/pull/9) terkonfirmasi merged ke main (`a448c7f`), dan Java CI UC-03 sukses. Perbandingan 1aea612 dengan merge a448c7f melalui GitHub menghasilkan daftar berkas kosong; baseline lokal sesuai hasil integrasi. Main lokal belum diperbarui. Pembuatan branch UC-04 ditolak karena `.git` hanya baca di sesi agent. Perubahan tetap dipertahankan di working tree untuk dipindahkan Jefry sebelum commit. Tidak ada commit/push/PR/merge oleh agent; `.env` tetap utuh.

### Implementasi dan verifikasi UC-04

Acuan dibaca: Modul 2 revisi UC-04 hlm. 6-7, PC-04 hlm. 89, UI-04 hlm. 103, serta design context/screenshot Hi-Fi RSI frame `111:1005`. Kelayakan mengikuti kontrak bersama untuk empat status aktif, termasuk Dilewati, ketika teks prasyarat UC lama berbeda.

- PembatalanBoundary mengganti sambungan informasi UC-03 pada GET `/pasien/antrean/{id}/pembatalan` dengan detail dan modal konfirmasi. Kembali/Escape menutup modal tanpa menulis data. Tanpa JavaScript/dukungan dialog, formulir konfirmasi tersedia langsung. Tombol Ya, Batalkan mengirim POST dengan CSRF dan persetujuan eksplisit.
- PembatalanPolicy dipakai bersama oleh tampilan UC-03 dan penyimpanan UC-04: Terdaftar/Menunggu/Hadir/Menunggu/Dilewati, dengan waktu lebih dari 60 menit sebelum jam mulai. Tepat 60 menit/kurang, Sedang Dilayani dan seluruh status akhir ditolak. Server mengambil waktu setelah kunci jadwal/tiket diperoleh.
- PembatalanService membaca ID jadwal skalar dari tiket milik sesi, lalu mengunci JadwalPraktik dan Antrean dalam urutan yang sama dengan booking. Setelah terkunci, pemilik/jadwal/status/waktu/kapasitas diperiksa ulang. Perubahan Dibatalkan dan pengembalian satu kuota atomik; kegagalan UPDATE kuota me-rollback perubahan status.
- Submit ulang ditolak 409 tanpa pengembalian kedua. Dua pembatalan berbeda pada sesi sama tidak kehilangan penambahan kuota. Booking yang bersaing dengan pembatalan menjaga kuota dan jumlah tiket aktif tetap konsisten. Jika kuota sudah mencapai kapasitas, seluruh operasi ditolak tanpa mengubah status.
- Sukses 303 ke halaman hasil yang membaca status DB; hasil sebelum tiket Dibatalkan ditolak 409. Notifikasi keberhasilan adalah pesan layar. Nomor tiket, tanggal booking, waktu check-in/selesai, entitas, seed dan migrasi tetap utuh. Tidak menambah notifikasi loket UC-10.
- Tiket bukan milik pasien/tidak ada sama-sama 404, ID/konfirmasi tidak valid 400, peran lain 403, syarat gagal 409, gangguan DB 503 tanpa rincian koneksi. Parameter ID pasien/jadwal tambahan tidak digunakan. Seluruh respons pribadi no-store. Link status UC-03 dan kuota UC-01 memperlihatkan hasil terbaru; check-in mandiri tetap menunggu UC-05.

| Pemeriksaan UC-04 | Hasil aktual |
| --- | --- |
| Maven Wrapper Windows 3.9.16 offline verify | **BUILD SUCCESS, 102 tes, 0 gagal/error/skipped**: 86 regresi + 12 PembatalanTests + 4 PembatalanTransactionsTests. JDK 24.0.2, compile release 21, Mockito Java agent cache. `target/uc04-verify.log` |
| Git Bash verify | Terhalang sebelum Maven oleh MSYS `NtCreateDirectoryObject ... 0xC0000022`. Fallback Wrapper Windows di atas lulus; `target/uc04-bash-verify.log` |
| Aturan dan keamanan | Lulus: 61/60 menit, ringkasan lama melewati batas, seluruh status diizinkan/ditolak, pemilik/peran, CSRF dan konfirmasi, input identitas palsu, kuota penuh, hasil tidak boleh mengklaim sukses sebelum dibatalkan, galat DB 503 |
| Transaksi H2 nyata | Lulus: pembatalan ganda bersamaan hanya satu berhasil; dua tiket berbeda mengembalikan dua kuota; trigger UPDATE gagal me-rollback status dan retry aman; booking bersamaan pembatalan menjaga invariant |
| Startup executable JAR dan HTTP | **Lulus dua startup JVM baru** dari folder sementara: enam akun, peran/CSRF/logout, UC-01 sampai UC-04 termasuk replay ditolak dan snapshot status/kuota terbaru. `target/uc04-smoke.log`, log aplikasi `target/demo-smoke-1.log` dan `target/demo-smoke-2.log` |
| Browser desktop localhost | **Lulus**: pilih tiket besok, detail/modal, Kembali dan Escape tanpa perubahan (kuota tetap 9), Ya Batalkan menghasilkan Dibatalkan dan 10/10, reload hasil tetap 10, link status dan kuota benar, tiket Sedang Dilayani/already cancelled tidak menyediakan konfirmasi, pasien lain tidak dapat membuka tiket dan admin ditolak |
| Sintaks JavaScript/Python, whitespace, `.env` | Lulus: node --check, py_compile dan git diff --check. Hash SHA-256 `.env` sebelum/sesudah sama |

Tes aturan/MockMvc di-rollback; tes transaksi memakai jadwal/tiket khusus yang dibersihkan. Browser dan smoke memakai H2 proses sementara; seluruh proses sudah dihentikan. Tidak ada perubahan data pribadi, MySQL atau Supabase.

Keterbatasan review: runtime JDK 21 dan CI UC-04 belum dijalankan dari sesi ini. Viewport mobile dan browser dengan JavaScript dinonaktifkan belum diperiksa; fallback konfirmasi diverifikasi lewat HTML/form server. Struktur kartu dan dialog native mengikuti UI-04 dengan layout/font fallback fondasi. Lima unduhan SVG Figma timeout; tidak memasang aset kosong/URL sementara. Ikon, font dan layout bersama belum identik penuh dengan Figma.

Daftar berkas UC-04: boundary/PembatalanBoundary.java dan StatusAntreanBoundary.java; controller/PembatalanService.java, PembatalanPolicy.java, PembatalanException.java dan StatusAntreanService.java; repository/AntreanRepository.java; templates/antreanpasien/pembatalan.html dan status.html; static/js/pembatalan.js; static/css/app.css; PembatalanTests.java, PembatalanTransactionsTests.java dan StatusAntreanTests.java; scripts/smoke_demo.py; README.md; SETUP.md; docs/KONTRAK.md; PROGRESS.md.

Langkah Jefry sebelum commit: `git fetch origin`, kemudian `git switch --no-track -c feat/uc-04-batal-antrean origin/main` untuk membawa working tree ke branch tujuan. Tinjau diff, commit, periksa `git log -1 --format=fuller`, push dan PR ke main. Usulan commit: `feat(uc-04): cancel patient registration and restore quota atomically`. UC-05 menunggu UC-04 terverifikasi dan masuk main.

## Riwayat UC-03 sebelum integrasi

Branch tujuan: `feat/uc-03-status-antrean`. Branch aktual sesi: `feat/uc-02-booking` pada `10d52c6`; working tree awal bersih. PR [#8](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/pull/8) sudah merged ke main (`05adc6d`); Java CI pada commit UC-02 juga terkonfirmasi sukses. Perbandingan isi 10d52c6 dengan merge 05adc6d melalui GitHub menghasilkan daftar berkas kosong. Main lokal belum diperbarui. Pembuatan branch UC-03 ditolak karena `.git` hanya baca di sesi agent; perubahan dipertahankan untuk dipindahkan Jefry sebelum commit. Tidak ada commit/push/PR/merge oleh agent.

### Implementasi dan verifikasi UC-03

Acuan dibaca: Modul 2 revisi UC-03 hlm. 5-6, PC-03 hlm. 88-89, UI-03 hlm. 101 dan design context/screenshot Hi-Fi RSI frame `111:777`. Kontrak proyek mengatasi perbedaan istilah status dan rumus selisih nomor pada UC lama: hitungan yang diterapkan adalah jumlah tiket aktif bernomor lebih kecil dalam idJadwal yang sama.

- StatusAntreanBoundary/Service/Dto menyediakan halaman `/pasien/antrean`, pilihan tiket sendiri, detail `/pasien/antrean/{id}`, serta JSON `/data` pada root/detail. Tiket baru UC-02 tersambung langsung dari hasil booking. Beranda, sidebar jadwal dan booking menyediakan akses Antrean Saya.
- Snapshot memuat nomor sendiri, status, jadwal dokter/poli/tanggal/sesi, seluruh nomor Sedang Dilayani pada sesi yang sama, jumlah aktif di depan dan maksimal lima nomor/status antrean aktif. Tidak membawa nama/noRM/NIK/ID pasien atau ID tiket orang lain. Tidak membuat perkiraan menit tanpa aturan durasi layanan.
- Status aktif: Terdaftar, Menunggu, Hadir/Menunggu, Dilewati dan Sedang Dilayani. Status akhir: Selesai, Dibatalkan dan Tidak Hadir. Posisi menunggu pada tiket akhir atau yang sedang dilayani adalah null; keterangan menjelaskan statusnya. Dilewati tetap aktif untuk arahan panggilan ulang.
- Pembacaan memakai transaksi read-only REPEATABLE_READ agar beberapa query berada pada satu snapshot. Tidak menulis tiket/kuota dan tidak mengubah skema, seed, migrasi, login atau nilai status.
- Polling 5 detik, timeout 8 detik, refresh manual, no-store, penanda data lama dan sesi kedaluwarsa. Tautan tindakan dinonaktifkan saat refresh gagal dan dipulihkan setelah data segar. Tanpa JavaScript gunakan reload.
- Kelayakan tombol mengikuti batas batal lebih dari 60 menit serta check-in hari yang sama dari 30 menit sebelum mulai sampai selesai inklusif. Sambungan GET `/pembatalan` dan `/check-in` masih informasi baca-saja yang mengarahkan ke loket; server memeriksa kepemilikan/kelayakan. Operasi tulis mandiri tetap UC-04/05 setelah gerbang masing-masing.
- Tiket bukan milik pasien/tidak ada sama-sama 404, ID tidak valid 400, peran lain 403, gangguan DB 503 dengan pesan umum. Tanpa tiket menampilkan keadaan kosong; seluruh tampilan memakai DTO.

| Pemeriksaan UC-03 | Hasil aktual |
| --- | --- |
| Maven Wrapper Windows 3.9.16 offline verify | **BUILD SUCCESS, 86 tes, 0 gagal/error/skipped**: 72 regresi + 14 StatusAntreanTests. JDK 24.0.2, compile release 21, Mockito Java agent dari cache. Log `target/uc03-verify.log` |
| Git Bash verify | Terhalang sebelum Maven oleh MSYS `NtCreateDirectoryObject ... 0xC0000022`. Fallback Wrapper Windows di atas lulus; `target/uc03-bash-verify.log` |
| Tes aturan dan keamanan | Lulus: nomor berlubang, pemisahan sesi/dokter/tanggal, perubahan status, seluruh status akhir/Dilewati, tiket kosong, daftar maksimal lima nomor, beberapa nomor dilayani, kepemilikan dan peran, batas waktu tombol serta galat DB 503 |
| Startup executable JAR dan HTTP | **Lulus dua startup baru** pada JAR final: login enam akun, peran/CSRF/logout, UC-01/02 dan snapshot/status akhir/privasi UC-03. `target/uc03-smoke-final.log`, aplikasi `target/demo-smoke-1.log` dan `target/demo-smoke-2.log` |
| Browser desktop | **Lulus**: Antrean Saya dari beranda, pilihan tiket, ringkasan 004/Menunggu/001/3 orang; perubahan tanpa reload ke nomor dilayani 002 dan 2 orang; status tiket berubah Dibatalkan, posisi dihilangkan; pilihan dropdown tetap setelah refresh; pasien lain ditolak dan admin mendapat halaman akses dibatasi |
| Browser saat server dihentikan | **Lulus**: data terakhir dan nomor tetap terlihat, pesan pembaruan gagal muncul, tautan pembatalan yang semula aktif kehilangan href dan aria-disabled=true |
| JAR setelah koreksi JavaScript | Package offline **BUILD SUCCESS** (`target/uc03-package-final.log`); koreksi pilihan dropdown diperiksa ulang di browser dan smoke JAR final di atas. Package ini tidak menjalankan ulang tes Java; hasil verify 86 tes tetap tercatat terpisah |
| Sintaks JavaScript/Python, whitespace, `.env` | Lulus: node --check, py_compile dan git diff --check; hash SHA-256 `.env` sebelum/sesudah sama |

Tes MockMvc/aturan memakai transaksi yang di-rollback. Satu pemeriksaan privasi awal salah mencocokkan substring nik di kata klinik; assertion telah diperbaiki menjadi nama field JSON persis dan verify di atas lulus. Pemeriksaan perubahan otomatis memakai helper Java sementara di target (bukan bagian JAR/commit), yang mengubah fixture H2 proses browser dalam transaksi. Tidak menambah endpoint pengubah status pada aplikasi. Browser juga memeriksa sambungan pembatalan sebagai informasi baca-saja. Seluruh proses pengujian sudah dihentikan; tidak ada data DB permanen atau koneksi Supabase yang diubah.

Keterbatasan visual: struktur kartu ringkasan tiga kolom, detail dan daftar nomor mengikuti UI-03 dengan layout serta font fallback fondasi. Tujuh unduhan SVG Figma timeout pada sesi ini; tidak memasang aset kosong atau URL sementara. Ikon/font serta posisi layout bersama belum identik penuh dengan Figma. Runtime JDK 21/CI UC-03 dan viewport mobile belum diverifikasi dari sesi ini. Pembatalan/check-in mandiri belum diimplementasikan pada tahap ini.

Daftar berkas UC-03: boundary/StatusAntreanBoundary.java dan BookingBoundary.java; controller/StatusAntreanService.java dan StatusAntreanDto.java; repository/AntreanRepository.java; templates/antreanpasien/status.html dan informasi.html; templates/booking/tiket.html dan ringkasan.html; templates/index.html dan jadwal/index.html; static/js/status-antrean.js; static/css/app.css; StatusAntreanTests.java; scripts/smoke_demo.py; README.md; SETUP.md; docs/KONTRAK.md; docs/SETUP_00_VERIFIKASI.md; PROGRESS.md.

Langkah Jefry sebelum commit: `git fetch origin`, lalu `git switch --no-track -c feat/uc-03-status-antrean origin/main` untuk membawa working tree ke branch tujuan. Tinjau diff, commit, periksa `git log -1 --format=fuller`, push dan PR ke main. Usulan commit: `feat(uc-03): add live patient queue status`. UC-04 menunggu UC-03 terverifikasi dan masuk main.

## Riwayat UC-02 sebelum integrasi

Branch tujuan: `feat/uc-02-booking`. Branch aktual sesi: `feat/uc-01-jadwal` pada `9dc6f9c`; working tree awal bersih. PR [#7](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/pull/7) terkonfirmasi merged ke main pada `6e9123b`. Perbandingan kedua commit melalui GitHub menghasilkan daftar berkas kosong, sehingga isi baseline lokal sesuai hasil integrasi. Main lokal belum diperbarui. Pembuatan branch UC-02 ditolak karena `.git` hanya baca di sesi agent. Seluruh perubahan dipertahankan di working tree untuk dipindahkan Jefry ke branch tujuan sebelum commit. Agent tidak melakukan commit/push/PR/merge; `.env` pribadi tidak diedit.

### Implementasi dan verifikasi UC-02

Acuan dibaca: Modul 2 revisi UC-02 hlm. 5, PC-02 hlm. 87-88, UI-02 hlm. 99; design context dan screenshot Hi-Fi RSI frame `115:1129`. UI memakai ulang layout/tokens fondasi, ringkasan jadwal, kartu pasien dan kartu ringkasan booking.

- Alur UC-01 Pilih Jadwal mengarah ke ringkasan booking, lalu konfirmasi data. GET tidak menulis tiket atau kuota. Nama/no. rekam medis berasal dari pasien yang login; parameter ID pasien/jadwal tambahan pada POST tidak menjadi sumber identitas atau pilihan.
- POST konfirmasi memakai CSRF dan draft terikat sesi, pasien, serta jadwal. Draft berlaku 15 menit, maksimal 16 per sesi. Token acak 12 karakter dipakai sebagai PK tiket/idempotensi, sesuai skema yang ada. Submit ulang token yang sama mengembalikan tiket yang sama, termasuk ketika kuota kini nol. Ini bukan larangan semua booking pasien yang sama pada sesi praktik yang sama.
- BookingService mengunci JadwalPraktik lalu Antrean, memvalidasi waktu/kuota, mengambil nomor terbesar seluruh status + 1, menyimpan Terdaftar dan mengurangi satu kuota dalam satu transaksi. Kegagalan UPDATE kuota me-rollback INSERT tiket. Skema, migrasi historis dan seed tetap digunakan tanpa perubahan.
- Kuota penuh atau sesi telah berakhir ditolak 409 dengan alternatif maksimal lima sesi terbuka pada poli yang sama. ID tidak valid 400, tidak ditemukan 404, gangguan penyimpanan 503 dengan pesan umum. Waktu memakai Clock Asia/Jakarta.
- Batal sebelum konfirmasi menghapus draft tanpa membuat/membatalkan tiket. Batal dari form lama setelah booking tersimpan kembali ke tiket; pembatalan tiket tersimpan tetap pekerjaan UC-04.
- Tiket milik pasien tersedia di `/pasien/booking/tiket/{idAntrean}`. Tautan `/pasien/antrean/{idAntrean}` sementara menampilkan detail/status tersimpan sebagai sambungan UC-03. Posisi antrean, nomor sedang dilayani dan polling status belum dikerjakan. Tiket pasien lain menghasilkan 404; peran lain 403. Halaman pribadi no-store.

| Pemeriksaan UC-02 | Hasil aktual |
| --- | --- |
| Maven Wrapper Windows 3.9.16 offline verify | **BUILD SUCCESS, 72 tes, 0 gagal/error/skipped**: 59 regresi + 9 BookingTests + 4 BookingTransactionsTests. JDK 24.0.2, compile release 21, Mockito agent dari cache. Log `target/uc02-verify.log` |
| Git Bash verify | Terhalang sebelum Maven oleh MSYS `NtCreateDirectoryObject ... 0xC0000022`. Fallback Wrapper Windows di atas lulus; log `target/uc02-bash-verify.log` |
| Transaksi dan konkurensi H2 | Dua pasien memperebutkan satu slot: tepat satu sukses. Dua submit token sama bersamaan: satu tiket. Trigger kegagalan UPDATE: tiket dan kuota rollback, retry berhasil. Nomor tiket batal tidak digunakan ulang |
| Startup executable JAR dan HTTP | **Lulus dua startup JVM baru** dari folder sementara: enam akun, lintas peran, CSRF/logout, UC-01, booking pasien2, penolakan spoofing, replay tanpa kuota ganda, batal draft dan privasi tiket. Log `target/uc02-smoke.log`, aplikasi `target/demo-smoke-1.log` dan `target/demo-smoke-2.log` |
| Browser desktop localhost | **Lulus**: pilih besok/Poli Umum, ringkasan pasien2, konfirmasi, batal draft (kuota tetap 9), booking nomor 002/Terdaftar (9 menjadi 8), reload tiket, tautan status, tiket tidak terbuka bagi pasien lain, admin ditolak, sesi berakhir menampilkan alternatif |
| Sintaks JavaScript/Python, whitespace, `.env` | Lulus: node --check, py_compile dan git diff --check. Hash SHA-256 `.env` sebelum/sesudah sama |

Tes transaksional memakai fixture terpisah yang dibersihkan setelah tes; tes MockMvc di-rollback. Smoke/browser memakai H2 proses sementara; proses pengujian sudah dihentikan. Tidak ada koneksi MySQL/Supabase atau perubahan kredensial.

Keterbatasan: runtime JDK 21 dan CI GitHub UC-02 belum dijalankan dari sesi ini. Browser desktop diperiksa; viewport mobile belum diperiksa. Tampilan mengikuti struktur frame UI-02 dengan layout/font fallback fondasi; ikon, font dan posisi layout bersama belum identik seluruhnya dengan Figma. Tidak ada URL aset sementara atau gambar rusak.

Daftar berkas UC-02: boundary/BookingBoundary.java, BookingDraftStore.java dan JadwalBoundary.java; controller/BookingService.java dan BookingException.java; repository/AntreanRepository.java dan JadwalPraktikRepository.java; templates/booking/ringkasan.html dan tiket.html; templates/jadwal/pilihan.html; static/js/booking.js; static/css/app.css; BookingTests.java, BookingTransactionsTests.java dan JadwalTests.java; scripts/smoke_demo.py; README.md; SETUP.md; docs/KONTRAK.md; PROGRESS.md.

Langkah Jefry sebelum commit (mempertahankan working tree):

```bash
git fetch origin
git switch --no-track -c feat/uc-02-booking origin/main
git add .
git diff --cached
```

Tinjau hasil lalu commit, periksa `git log -1 --format=fuller`, push dan PR ke main. Usulan commit: `feat(uc-02): add atomic online queue booking`. UC-03 menunggu UC-02 terverifikasi dan masuk main.

## Riwayat UC-01 sebelum integrasi

Branch tujuan UC-01: `feat/uc-01-jadwal`. Branch aktual sesi: `chore/setup-00-fondasi` pada `a90c2e2`, working tree awal bersih. PR [#6](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/pull/6) sudah merged ke main (`2fe5510`). Perbandingan GitHub antara a90c2e2 dan merge commit menghasilkan daftar berkas kosong: isi baseline lokal sama dengan hasil integrasi. Main lokal belum diperbarui. Pembuatan branch UC-01 ditolak karena `.git` hanya baca; perubahan fitur tetap di working tree dan perlu dipindahkan ke branch tujuan oleh Jefry sebelum commit. Tidak ada commit/push/PR/merge oleh agent; .env pribadi tidak diedit.

### Implementasi dan verifikasi UC-01

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
| UC-01 | done | 59 tes lulus, smoke/browser diperiksa; PR #7 merged ke main |
| UC-02 | done | 72 tes lulus; dua startup JAR/HTTP dan browser lulus; PR #8 merged ke main, CI sukses |
| UC-03 | done | 86 tes lulus, dua startup JAR/HTTP dan browser lulus; PR #9 merged, CI sukses |
| UC-04 | done | 102 tes lulus, dua startup JAR/HTTP dan browser lulus; PR #10 merged, CI sukses |
| UC-05 | review | 120 tes dan startup/HTTP lulus; cek visual desktop/mobile tertunda karena koneksi browser |
| UC-06 | todo | Setelah UC-05 masuk main |
| UC-07 | todo | Setelah UC-06 masuk main |
| UC-08 | todo | Setelah UC-07 masuk main |
| UC-09 | todo | Setelah UC-08 masuk main |
| UC-10 | todo | Setelah UC-09 masuk main |
| QA-01 | todo | Seluruh UC terintegrasi; regresi lintas peran di H2 |
| DB-01 | todo | QA-01 lulus dan masuk main; PostgreSQL Supabase |

UI-01 sampai UI-04 terintegrasi; UI-05 diimplementasikan untuk review dengan cek visual tertunda; UI-06 sampai UI-10 belum diimplementasikan. Label pemilik MF/RA/NA/AH pada kartu lama adalah asal spesifikasi; Jefry mengerjakan semua tahap. Detail branch/UC/PC/UI ada di [roadmap](docs/PROJECT.md).

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
