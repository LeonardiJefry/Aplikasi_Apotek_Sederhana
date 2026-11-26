# Progres Klinik Leonardo

Diperbarui: 27 November 2026 (WIB). PIC seluruh tahap: Jefry (JL).

## Tahap aktif

**UC-05: review - implementasi lokal dipertahankan; verifikasi ulang HTTP/startup dan browser terhalang izin loopback pada sesi terbaru. SETUP-00 sampai UC-04: done melalui PR #6 sampai #10.**

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
