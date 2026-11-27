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

H2 ada pada classpath runtime, termasuk saat Maven membuat executable JAR. Flyway menjalankan V1/V2 historis, V3 metadata panggilan UC-06, dan V4 presisi mikrodetik pada timestamp panggilan; placeholder `tableOptions` dikosongkan untuk H2. Hibernate memakai `ddl-auto=validate`. Jangan mengedit migrasi yang pernah dipasang atau mengubahnya menjadi `update/create`.

Fixture mencakup 2 poli, 2 dokter, 2 pasien, 4 jadwal, 9 antrean, 2 jadwal vaksinasi, 1 notifikasi, 4 riwayat pelewatan, dan 6 akun sintetis. Seluruh delapan status kontrak tersedia. Tanggal mengikuti `Asia/Jakarta` saat database pertama kali dibuat. Seed berjalan atomik satu kali per database dengan kunci `DemoDataset`, sehingga pemanggilan ulang pada JVM yang sama mempertahankan perubahan data.

Data demo H2 hilang saat JVM berhenti. Ctrl+C lalu jalankan ulang aplikasi untuk kembali ke fixture awal dengan tanggal startup baru. Data MySQL opsional tetap persisten; aturan restart H2 tidak berlaku untuk MySQL.

## 3. Verifikasi build, HTTP, dan browser

```bash
./mvnw --batch-mode --no-transfer-progress verify
python scripts/smoke_demo.py target/apotek-0.0.1-SNAPSHOT.jar
```

Di Linux dapat memakai `python3`. Skrip hanya memakai pustaka standar Python. `verify` mengompilasi, menjalankan tes, dan membuat executable JAR. Tes memeriksa migrasi berulang, seed berulang, relasi/constraint, autentikasi, empat peran, akun nonaktif, CSRF, kepemilikan pasien/dokter, serta profil demo dan default tanpa `.env`.

Tes regresi `ServletSecurityTests` juga menjalankan Tomcat pada port lokal acak untuk memeriksa forward POST saat CSRF ditolak. Port loopback harus diizinkan. Ini melengkapi MockMvc yang tidak menjalankan forward servlet secara otomatis. Setelah source Java diubah, build ulang JAR sebelum menjalankan skrip smoke; skrip selalu memakai JAR yang ditunjuk, bukan source terbaru.

Skrip HTTP memulai JAR pada `127.0.0.1:18080` dari direktori sementara kosong, memeriksa login enam akun sintetis, password salah, penolakan akses lintas peran, CSRF, logout, jadwal serta booking. Booking memakai jadwal besok, pasien dari sesi, penolakan spoofing, retry tanpa kuota ganda, batal draft dan privasi tiket. Proses dihentikan, kemudian startup dan pemeriksaan diulang pada JVM baru. Port 18080 harus kosong. Log berada di `target/demo-smoke-1.log` dan `target/demo-smoke-2.log`. CI menggunakan JDK 21, H2, dan kedua perintah ini tanpa service database.

Untuk pemeriksaan visual, jalankan aplikasi normal di port 8080 dan coba:

1. Tanpa login, URL `/pasien` mengarah ke login. Password salah menampilkan pesan gagal.
2. Masuk sebagai pasien, loket, admin dan dokter; beranda menampilkan peran dan username yang tepat.
3. Dari setiap akun, buka URL tiga peran lain; hasil harus 403.
4. Logout mengembalikan ke login; halaman peran kembali meminta login.
5. Hentikan proses dan jalankan lagi; akun demo tersedia kembali tanpa duplikasi atau error migrasi.

Catat hasil build, tes, startup HTTP, dan browser secara terpisah. Lulus MockMvc tidak sama dengan lulus browser. Bila DNS Maven terhalang, periksa `mvn -version` dan cache dependency lokal lalu coba `mvn --offline --batch-mode --no-transfer-progress verify`. Jangan mengganti versi dependency atau mengklaim verify berhasil bila ada artifact yang belum tersimpan. Cache lengkap dan repositori asal artifact tetap diperlukan. Sandbox yang menolak Bash atau loopback perlu dibedakan dari kegagalan aturan aplikasi.

Jika Git Bash gagal sebelum Maven dengan `NtCreateDirectoryObject ... 0xC0000022`, masalahnya berada pada startup MSYS. Wrapper Windows `mvnw.cmd --offline --batch-mode --no-transfer-progress verify` dapat menjadi fallback dari terminal Windows bila distribusi Maven dan dependensi sudah tersimpan. Gunakan cache Maven yang memang lengkap; folder cache lain yang kosong tidak dapat menggantikannya. Settings cache khusus di `target/` hanya alat diagnosis lokal dan tidak diperlukan clone baru/CI.

Jika Tomcat melaporkan `Unable to establish loopback connection` dengan `Permission denied: connect`, koneksi lokal pada lingkungan eksekusi ditolak. Mode offline Maven tidak menyelesaikan pembatasan itu. Jalankan kembali verify lengkap dan smoke JAR dari terminal lokal yang mengizinkan koneksi loopback. Hasil sebagian tes tanpa servlet nyata, atau keberhasilan migrasi H2 sebelum kegagalan Tomcat, belum membuktikan startup, login HTTP maupun tampilan browser berhasil. Hasil aktual UC-05 terbaru dicatat terpisah dari riwayat pada PROGRESS.md.

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

Login/layout tetap memakai token Design System Hi-Fi RSI yang sudah diterapkan. UC-01 sampai UC-06 tersedia seperti bagian 7-12; UI-07 sampai UI-10 menunggu tahap fitur. Font Poppins memakai fallback bila belum tersedia. Model tetap di `id.ac.ub.apotek.entity` dan repository di `id.ac.ub.apotek.repository`; jangan menduplikasi model.

## 7. Mencoba UC-01 — jadwal praktik dan sisa kuota

Jalankan demo seperti bagian 1, login `pasien` dengan password demo, lalu klik **Lihat Jadwal Praktik** atau buka `/pasien/jadwal`. Gunakan tanggal default dan Poli Umum: fixture menampilkan dr. Demo Satu, sesi Pagi, 08:00–14:00, sisa 6/10. Poli Gigi menampilkan dokter gigi. Memilih poli lain mereset dokter yang tidak cocok. Tanpa poli/dokter, form dan server menolak pencarian.

Hasil diperbarui otomatis setiap 5 detik dan bisa diperbarui manual. Tanggal tanpa jadwal menampilkan Jadwal tidak tersedia. Pemilihan jadwal membaca ulang kuota; jika sudah penuh, server menolak. Pilih Jadwal membuka ringkasan UC-02 tanpa membuat antrean atau mengurangi kuota. Penulisan baru dilakukan saat Konfirmasi Booking.

Tes JadwalTests mencakup tanggal/filter tidak valid, kombinasi dokter-poli, hasil kosong, kuota berubah menjadi penuh, pilihan read-only, hak akses dan privasi DTO. Skrip smoke JAR juga menguji UC-01 pada kedua startup. Untuk validasi visual, cek dropdown, tabel, refresh, penolakan akses, serta indikator data lama ketika server dihentikan. State Penuh diuji otomatis dengan perubahan fixture transaksional yang di-rollback, tanpa mengubah dataset demo tetap.

Acuan tampilan: Hi-Fi RSI frame `115:881`, memakai ulang layout/tokens fondasi dan kontrol HTML native. Poppins tetap fallback bila tidak tersedia. Ikon SVG Figma tidak berhasil diunduh dari sesi ini (timeout); tidak ada URL sementara atau gambar rusak yang disisipkan. Kesesuaian ikon, font dan layout bersama belum identik dengan seluruh frame Figma. Field tanggal native mengikuti locale browser, sedangkan parameter dan tanggal informasi memakai YYYY-MM-DD.

## 8. Mencoba UC-02 - booking antrean online

1. Jalankan demo, login `pasien2`, buka jadwal dan pilih **besok + Poli Umum**. Jadwal JDW-003 dimulai dengan sisa 9/10; tanggal menyesuaikan startup. Jadwal hari ini yang telah melewati jam selesai tidak dapat dibooking.
2. Klik **Pilih Jadwal**. Periksa poli, dokter, tanggal, sesi, Pasien Demo Dua dan DEMO-RM-002. Ringkasan tidak memesan slot.
3. Klik **Lanjut ke Konfirmasi Data**, kemudian **Batal**. Cari jadwal yang sama; kuota tetap 9/10.
4. Ulangi lalu klik **Konfirmasi Booking**. Tiket berstatus Terdaftar dan nomor 002 muncul pada database demo baru. Kuota menjadi 8/10. Membuka ulang halaman tiket tidak membuat booking lagi. Pengiriman ulang form/token yang sama juga mengembalikan tiket tersimpan.
5. Klik **Lihat Status Pendaftaran** untuk status antrean UC-03, nomor yang sedang dilayani dan jumlah antrean di depan yang diperbarui otomatis. Pembatalan tiket tersimpan dan check-in mengikuti UC-04/05.
6. Salin URL tiket, logout, lalu login `pasien`. URL tersebut menampilkan Tiket tidak ditemukan (404). Login admin/loket/dokter lalu buka URL booking: akses ditolak (403).

Kuota penuh atau sesi berakhir menghasilkan pesan penolakan dan sesi alternatif terbuka pada poli yang sama jika tersedia. Untuk mencari poli lain, gunakan Pilih jadwal lain. Draft konfirmasi berlaku 15 menit; setelah kedaluwarsa pilih jadwal kembali. Identitas pasien dan pilihan konfirmasi berasal dari sesi server. Form POST memakai CSRF, tanpa field ID pasien.

BookingTests memeriksa ringkasan, konfirmasi, replay, manipulasi input, draft lintas sesi/kedaluwarsa, batal sebelum dan sesudah simpan, kuota menjadi penuh setelah ringkasan, alternatif serta akses peran/kepemilikan. BookingTransactionsTests memakai transaksi nyata H2 untuk perebutan slot terakhir, submit sama bersamaan, nomor tidak digunakan ulang, serta kegagalan UPDATE kuota yang me-rollback tiket dan dapat dicoba lagi. Seed permanen dan migrasi tidak diubah.

Acuan UI-02: Modul 2 bagian 2.7 dan Hi-Fi RSI frame `115:1129`. Desktop diperiksa dengan layout dan fallback font fondasi; kesesuaian penuh ikon/font serta viewport mobile belum diverifikasi. Hasil aktual, versi runtime dan keterbatasan Git Bash tercatat di PROGRESS.md.

## 9. Mencoba UC-03 - status antrean pasien

Login `pasien2`, klik **Antrean Saya** pada beranda, atau buka `/pasien/antrean`. Pilih tiket dari dropdown **Tiket kunjungan Anda** lalu klik **Lihat Status**. Daftar hanya berisi tiket milik sesi pasien; tiket yang baru dibooking juga bisa dibuka melalui tautan di halaman hasil booking.

- Pada dataset demo baru, pilih tiket ANT-DEMO0004: nomor sendiri 004, status Menunggu, nomor sedang dilayani 001, dan 3 orang di depan pada JDW-001. Hitungan berasal dari entri aktif bernomor lebih kecil, termasuk yang sedang dilayani dan Dilewati; nomor berstatus Selesai/Dibatalkan/Tidak Hadir tidak dihitung. Ini bukan pengurangan dua nomor.
- Tiket ANT-DEMO0005 pada jadwal dokter lain menampilkan belum ada nomor sedang dilayani dan 0 orang di depan. ANT-DEMO0008/0009 memperlihatkan status akhir, tanpa posisi menunggu. Belum memiliki tiket menampilkan ajakan melihat jadwal.
- Halaman membaca ulang snapshot setiap 5 detik; tombol **Perbarui sekarang** tersedia jika JavaScript aktif. Tanpa JavaScript, muat ulang halaman. Saat koneksi gagal, data terakhir ditandai belum diperbarui dan tautan tindakan dinonaktifkan sampai snapshot segar diperoleh.
- Klik Batalkan Antrean jika syarat waktu/status terpenuhi untuk membuka UC-04; GET tidak menulis data dan konfirmasi akhir memakai POST. Check-in membuka UC-05 untuk tiket Terdaftar, belum check-in dan berada pada jendela waktu yang benar. Status akhir dan tiket sedang dilayani tidak mengaktifkan keduanya.
- Akses tiket pasien lain menghasilkan 404 baik HTML maupun JSON, sedangkan akun admin/loket/dokter mendapat 403. Snapshot tidak menampilkan nama, noRM, NIK, ID pasien atau ID tiket orang lain; daftar antrean sesi hanya membawa nomor dan status.

Daftar antrean berikutnya dibatasi lima nomor aktif, diurutkan menurut nomor. Nomor Dilewati bisa tetap muncul; keputusan panggilan ulang mengikuti petugas/UC-06. Jika data memuat beberapa tiket Sedang Dilayani, seluruh nomornya ditampilkan. Belum ada asumsi menit per pasien, sehingga estimasi waktu ditampilkan sebagai tanda pisah dan diberi keterangan belum tersedia.

StatusAntreanTests memeriksa seluruh status, hitungan dengan nomor berlubang, pemisahan sesi/dokter/tanggal, perubahan status, daftar kosong, batas lima nomor, privasi dan kepemilikan, kesalahan 503, akses peran, batas pembatalan 61/60 menit serta batas check-in inklusif. Acuan: Modul 2 UC-03 hlm. 5-6, PC-03 hlm. 88-89, UI-03 hlm. 101; Hi-Fi RSI `111:777`. Hasil browser/startup dan keterbatasan visual aktual dicatat di PROGRESS.md.

## 10. Mencoba UC-04 - pembatalan pendaftaran antrean

1. Mulai proses demo baru, login `pasien`, buka Antrean Saya dan pilih ANT-DEMO0006 untuk jadwal besok. Tiket ini Terdaftar, sisa kuota JDW-003 adalah 9/10 pada fixture awal.
2. Klik **Batalkan Antrean** untuk membuka detail dan informasi syarat. Klik tombol merah **Batalkan Antrean** untuk memunculkan modal **Batalkan Antrean?**. Pilih **Kembali** atau tekan Escape: modal tertutup dan pendaftaran/kuota tetap sama. Membuka halaman atau modal tidak membatalkan tiket.
3. Buka modal lagi lalu klik **Ya, Batalkan**. Hasil menampilkan pesan berhasil, status Dibatalkan dan kuota 10/10. Klik **Kembali ke Status Antrean** dan **Lihat Kuota Jadwal** untuk memeriksa pembaruan UC-03/01.
4. Membuka ulang hasil pembatalan tidak mengubah data. Pengiriman ulang POST/form lama ditolak 409 dan tidak menambah kuota lagi. Tiket Selesai/Tidak Hadir/Sedang Dilayani juga tidak bisa dibatalkan.
5. Buka URL pembatalan yang sama sebagai `pasien2`: hasil 404. Akun loket/admin/dokter mendapat 403. CSRF dan persetujuan `konfirmasi=ya` wajib pada POST; identitas pasien dari sesi, jadwal dari tiket server.

Status yang boleh dibatalkan adalah Terdaftar, Menunggu, Hadir/Menunggu dan Dilewati. Waktu tersisa harus **lebih dari 60 menit** terhadap jam mulai, diperiksa lagi setelah kunci jadwal/tiket diperoleh. Tes Clock terkendali memeriksa 61 menit berhasil, tepat 60 menit dan kurang ditolak, termasuk ringkasan lama yang melewati batas saat dikirim. Tidak mengganti jam komputer untuk tes manual.

Pembatalan mengunci JadwalPraktik lalu Antrean seperti booking, mengubah status dan mengembalikan satu kuota pada transaksi yang sama. Tes H2 dua thread memeriksa pembatalan ganda, dua pembatalan berbeda dan booking yang bersaing dengan pembatalan. Trigger kegagalan UPDATE kuota membuktikan rollback status serta retry yang aman. Kuota yang sudah mencapai kapasitas menyebabkan penolakan seluruh operasi, bukan pengembalian tambahan.

Tanpa JavaScript atau dukungan dialog native, formulir konfirmasi tampil langsung dan tetap memerlukan tombol Ya, Batalkan. Keberhasilan baru ditampilkan setelah transaksi selesai; halaman hasil membaca status dari DB. Kegagalan simpan menampilkan pesan 503 dan jalur kembali untuk mencoba lagi. Seed dan migrasi tetap utuh. Acuan UC-04 hlm. 6-7, PC-04 hlm. 89, UI-04 hlm. 103; Hi-Fi RSI `111:1005`.

## 11. Mencoba UC-05 - check-in kedatangan dan peringatan loket

Masuk sebagai pasien, gunakan **Check-in hari ini** (`/pasien/check-in`) atau tombol **Check-in** pada tiket di Antrean Saya. Pilih tiket Terdaftar untuk hari yang sama. Halaman menampilkan nomor, dokter/poli/jadwal, tahap kehadiran, jendela check-in dan waktu server Asia/Jakarta. Membuka halaman tidak mengubah data.

1. Untuk fixture `pasien2`, tiket ANT-DEMO0005 berjadwal hari ini pada Poli Gigi 13:00-17:00: check-in dibuka **12:30 sampai 17:00**, inklusif. Di luar waktu itu halaman menolak dan tombol nonaktif; jangan mengubah jam komputer untuk mencobanya.
2. Alternatif saat 07:30-14:00: booking jadwal Poli Umum hari ini, lalu buka tiket baru dan klik **Check-in sekarang**. Status menjadi Hadir/Menunggu dan waktu check-in server tersimpan. Kuota dan nomor tidak berubah akibat check-in.
3. Muat ulang hasil: waktu pertama tetap. Kirim ulang form lama: 409, tanpa mengganti waktu kehadiran. Beda hari, tiket pasien lain, status selain Terdaftar atau waktu kehadiran yang sudah ada ditolak. Tombol Kembali hanya berpindah halaman.
4. Masuk sebagai `loket`, buka **Peringatan Check-in** (`/loket/check-in`). Peringatan menampilkan nomor dan jadwal, tanpa identitas pasien. Setelah pasien check-in, peringatan hilang pada pembaruan berikutnya. Halaman ini tidak memanggil atau melewati nomor; tindakan tersebut adalah UC-06.

Definisi operasional mendekati giliran pada UC-05: tiket Terdaftar yang belum check-in di antara **dua nomor aktif terdepan yang belum Sedang Dilayani**, menurut nomor pada jadwal yang sama. Peringatan hanya berlaku pada hari jadwal selama jendela check-in. Tiket Hadir/Menunggu/Menunggu/Dilewati tetap menghuni posisi antrean; status akhir dikeluarkan. Ketentuan angka dua ini dicatat karena Modul 2 tidak menetapkan angka ambang.

Halaman pasien/loket membaca ulang data setiap 5 detik, dapat diperbarui manual, dan menandai data lama saat gagal. Tombol check-in dinonaktifkan sampai data segar kembali. Tutup peringatan hanya menyembunyikan kartu pada halaman lokal; tombol Tampilkan peringatan yang disembunyikan mengembalikannya. Tidak ada perubahan status di server karena menyembunyikan peringatan. Tanpa JavaScript gunakan reload. Pada viewport kecil tombol check-in tetap berada di bagian bawah layar.

CheckInTests dan CheckInTransactionsTests menguji kedua batas inklusif, di luar rentang dan beda hari, kepemilikan/peran, CSRF, input identitas/waktu palsu, waktu tersimpan yang tidak boleh ditimpa, kuota/nomor/tanggal booking tetap, submit serentak, rollback dan ambang peringatan. CheckInServletTests menguji jalur sukses melalui HTTP Tomcat sungguhan dengan Clock fixture terpisah. Skrip smoke JAR memakai waktu server nyata; di luar jam yang valid, penolakan adalah hasil yang benar. Clock/fixture tes tidak dipasang sebagai opsi untuk melewati aturan pada aplikasi demo normal.

Acuan: Modul 2 UC-05 hlm. 7-8, PC-05 hlm. 89-91, UI-05 hlm. 105; Figma desktop `89:579`, mobile `89:597` dan peringatan loket `93:737`. Hasil aktual browser, startup dan keterbatasan visual ada di PROGRESS.md.

## 12. Mencoba UC-06 - panggilan dan pelewatan antrean

Build ulang aplikasi sebelum mencoba UC-06. Login sebagai `loket`, klik **Panggilan Antrean** atau buka `/loket/panggilan`, lalu pilih jadwal hari ini. GET hanya membaca; **Panggil antrean berikutnya** mengirim POST dengan CSRF. Demo JDW-001 sudah memiliki pasien Sedang Dilayani sehingga memblokir panggilan baru pada sesi itu; JDW-002 dapat dicoba selama jam praktik, sedangkan JDW-003 adalah jadwal besok pada seed normal.

1. Mulai panggilan pada jadwal yang sedang buka dan tidak mempunyai pasien Sedang Dilayani. Nomor terkecil yang memenuhi syarat tampil, bersama nama/noRM sintetis, waktu server dan batas tunggu 120 detik. Reload tidak memulai ulang timer.
2. Sebelum 120 detik, Lewati nonaktif. Setelah snapshot server mengizinkan, buka **Lewati nomor antrean**. **Batal** atau Escape menutup modal tanpa perubahan. **Ya, lewati** menyimpan status/riwayat dan otomatis memanggil kandidat berikutnya jika tersedia.
3. Saat pasien hadir, pilih **Pasien hadir, mulai layanan**: status menjadi Sedang Dilayani tanpa riwayat pelewatan atau perubahan kuota. Tindakan selesai pemeriksaan baru disediakan UC-10; UC-06 tidak menyediakan jalan pintas mengubahnya ke Selesai.
4. Pelewatan pertama/kedua menghasilkan Dilewati. Setelah pasien berikutnya selesai dilayani, panggilan berikutnya mendahulukan pasien Dilewati untuk kesempatan ulang. Pelewatan ketiga menghasilkan Tidak Hadir dan mengembalikan satu kuota. Jika tidak ada kandidat lain setelah pelewatan, petugas dapat memulai panggilan berikutnya sendiri selama jam pelayanan.
5. Pengiriman form/token lama ditolak, termasuk ketika nomor yang sama sudah dipanggil ulang dengan token baru. Seluruh aturan diperiksa di server. Tes waktu dan transaksi menguji 119/120 detik, dua aksi bersamaan dan rollback kuota/riwayat/panggilan berikutnya.

Halaman membaca ulang data setiap 5 detik, memiliki refresh manual dan penanda data lama. Saat gagal memperbarui, tindakan dinonaktifkan. Countdown tampilan tidak menggantikan waktu server. Di mobile, tombol Lewati berada di bawah layar dan riwayat berbentuk daftar. Tanpa JavaScript, konfirmasi tampil langsung dan pengguna memuat ulang untuk memperoleh kelayakan terbaru. Pengguna pasien/admin/dokter tidak dapat membuka prefix loket; layanan bersama dokter nanti tetap membatasi jadwal dokter sesi.

V3 menambah metadata panggilan; V4 mempertahankan pecahan detik agar timer awal tidak membulat menjadi 121 detik. V1-V3 dan seed dipertahankan. Pada H2, restart JVM menghapus panggilan beserta data demo. Pada MySQL opsional, metadata timer tersimpan dan tidak diinisialisasi ulang oleh seed. Tidak ada driver/profil Supabase baru. Acuan UC-06/PC-06/UI-06 dan keputusan integrasi lengkap ada di docs/KONTRAK.md. Aset SVG Figma belum berhasil diunduh karena DNS; layout bersama dan font fallback dipertahankan, sehingga kesesuaian penuh ikon/font tidak dinyatakan lulus.
