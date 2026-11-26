# Kontrak integrasi

Gunakan nama yang sudah ada jika repo sudah punya kontrak setara. Untuk repo baru, nama di bawah adalah usulan awal untuk dibekukan dalam SETUP-00. Ubah kontrak lewat PR dan beri tahu anggota yang bergantung padanya.

## Keputusan roadmap mandiri — 26 November 2026

Jefry (JL) adalah PIC implementasi seluruh tahap. Label MF/RA/NA/AH pada tabel di bawah dipertahankan sebagai asal spesifikasi dan batas integrasi UC; tidak lagi menetapkan penugasan anggota lain. Urutan aktif adalah SETUP-00, UC-01 sampai UC-10, QA-01, kemudian DB-01. Lihat [roadmap](PROJECT.md) dan [progres aktual](../PROGRESS.md).

- Alur default memakai profil `demo` H2 in-memory. Profil `test` memakai database H2 lain. Keduanya tidak membaca `.env` pribadi dan tidak membutuhkan server database.
- H2 tersedia pada classpath runtime. Flyway memakai migrasi V1/V2 yang sama dengan placeholder `tableOptions` kosong pada H2. Hibernate tetap `validate`; migrasi yang ada tidak diubah.
- Seed hanya tersedia pada profil `demo`, `test`, dan profil opsional historis `mysql`, serta harus diaktifkan melalui `klinik.demo.enabled`. Pada demo/test, seed aktif dengan password sintetis; pada mysql, seed opt-in dari `.env`. Pengamanan sesi, peran, kepemilikan dan CSRF tetap sama.
- Data demo bertahan selama JVM hidup; restart proses H2 membuat dataset baru dengan tanggal saat startup. Aturan seed sekali per database tetap berlaku selama proses yang sama. MySQL tetap persisten dan menggunakan penanda seed yang sama.
- Halaman galat umum `/akses-ditolak` boleh dirender tanpa login, menerima metode permintaan asli saat forward (termasuk POST), dan selalu mengembalikan HTTP 403. Halaman ini tidak memuat data pasien. Penolakan CSRF tidak mengautentikasi pengguna atau mengakhiri sesi logout yang ditolak. Prefix fitur tetap dibatasi peran.
- DB-01 setelah QA-01 lulus dan masuk main menyiapkan driver/profil Supabase serta lokasi Flyway PostgreSQL terpisah. Kebutuhan: tipe tanggal/waktu, REGEXP NIK, identifier camelCase, PK/FK/UNIQUE/CHECK, kunci transaksi dan konsistensi kuota. Tidak ada perubahan status atau aturan bisnis pada SETUP-00 H2 ini.

Keputusan profil dan PIC di atas menggantikan ketentuan MySQL/dev serta pembagian tim lama di bagian historis berikut. Semua kontrak perilaku UC tetap berlaku.

## Fondasi SETUP-00 untuk review Issue #3

- Stack dan package yang sudah berjalan dipertahankan: `id.ac.ub.apotek`, dengan satu entitas/repository bersama per tabel. Nama tabel dan kolom fisik persis Modul 3 bagian 3.1 (termasuk camelCase). JPA menggunakan `PhysicalNamingStrategyStandardImpl`; jangan membuat model duplikat di package fitur baru.
- Migrasi tunggal memakai Flyway: V1 berisi 8 tabel PDM beserta seluruh PK/FK/UNIQUE/CHECK/index; V2 menambah `Pengguna` dan `DemoDataset`. DDL Hibernate hanya `validate`. Migrasi berikutnya dimulai V3, tidak mengedit V1/V2 setelah dipakai tim. Skema audit UC-08 dan metadata panggilan UC-06 masih menjadi pekerjaan issue fiturnya.
- `Pengguna` memetakan satu peran `PASIEN`, `PETUGAS_LOKET`, `ADMIN`, atau `DOKTER`. Pasien wajib memiliki FK pasien, dokter wajib memiliki FK dokter, loket/admin memakai ID pengguna tanpa FK pasien/dokter. Username unik, identitas pasien/dokter per akun unik, hash BCrypt, dan status aktif disimpan di server.
- `CurrentUserService.requireUser()` mengembalikan `KlinikPrincipal` berisi `idPengguna`, `idPasien`, `idDokter`, dan `peran`. Gunakan `requirePasienId()` / `requireDokterId()` untuk ID pelaku, bukan parameter form. Principal tidak menyimpan entity JPA. `Clock` bersama memakai `Asia/Jakarta` dan dapat diganti pada tes.
- Prefix server `/pasien/**`, `/loket/**`, `/admin/**`, `/dokter/**` masing-masing dibatasi ke satu peran. Rute lain ditolak kecuali rute dasar/login/aset yang dinyatakan di `SecurityConfig`. Fitur baru yang memakai URL lain harus menambah matcher eksplisit beserta tes otorisasi. Pembatasan URL wajib dilengkapi pembatasan objek pada layanan fitur.
- `AntreanAccessService.requirePasienTicket(idAntrean)` dan `requireDokterTicket(idAntrean)` menyediakan pemeriksaan kepemilikan dari sesi dan DTO identitas tiket, tanpa membuka endpoint fitur. Repository menyediakan query tiket pasien/jadwal dokter yang sudah dibatasi. Operasi tulis fitur tetap harus memvalidasi ulang kepemilikan dan status dalam transaksi dengan urutan kunci kontrak.
- Fragment `fragments/layout :: head(title)`, `brand`, `header`, dan `footer` dapat dipakai bersama. Form Thymeleaf wajib memakai `th:action` agar token CSRF disisipkan. Header menyediakan logout POST.
- Seed satu kali per database dan atomik dengan kunci baris `DemoDataset`. Bukan migrasi data produksi. Tanggal fixture adalah tanggal pemasangan pertama. Pada H2 memori, data dipertahankan selama JVM hidup dan dibuat ulang setelah proses berhenti. Pada profil mysql opsional, `.env` mengaktifkan seed dan data dipertahankan saat restart.

Perubahan bersama ini disiapkan untuk ditinjau Jefry sebagai PIC tunggal. Referensi MF/NA tetap menunjukkan kaitan PDM/fixture dan akses loket; belum menyatakan persetujuan review atau merge.

## Data dan hak akses

| Data | Penanggung jawab | Pemakai |
| --- | --- | --- |
| `Pasien`, login, peran, ID pasien/dokter dalam sesi | JL | Semua |
| `Poli`, `Dokter`, `JadwalPraktik`, `Antrean` | MF merawat model dan repository | MF, RA, NA, JL, AH |
| `RiwayatPelewatan` dan metadata panggilan bila diperlukan | NA | NA, AH |
| `JadwalVaksinasi` dan data audit verifikasi | JL | JL |
| `Notifikasi` antrean selesai | AH | AH, NA (loket) |

- Gunakan H2 demo/test hingga QA-01 dan nama fisik konsisten dengan migrasi bersama. DDL MySQL 8 historis tetap dipertahankan; PostgreSQL diterapkan pada DB-01. PDM Modul 3 menetapkan PK string, kuota integer, FK, serta nomor antrean positif dan unik per `idJadwal`. Bedakan `idAntrean` (identitas tiket) dari `nomorAntrean` (nomor tampilan per sesi).
- Peta baseline `PASIEN` → `idPasien`, `DOKTER` → `idDokter`, `PETUGAS_LOKET` dan `ADMIN` → identitas pengguna. Ambil identitas pelaku dari sesi server. Pasien hanya membaca atau mengubah tiket sendiri. Dokter hanya membaca atau mengubah tiket pada jadwalnya. Petugas loket memiliki akses pada tindakan loket dan admin pada tindakan kuota vaksinasi.
- Waktu sesi mengikuti `Asia/Jakarta`. Gunakan sumber waktu server yang dapat dikendalikan saat pengujian.

## Antarmuka antarmodul

### UC-01 yang disediakan

PIC implementasi Jefry. Acuan: Modul 2 PC-01 hlm. 86–87, UI-01 hlm. 97; Hi-Fi RSI `115:881` (UI-01 desktop). Tidak ada perubahan entitas, status antrean, atau migrasi.

- `GET /pasien/jadwal`: halaman pasien. Tanggal wajib pada pencarian dan minimal satu `idPoli` atau `idDokter`; dua filter dipakai bersama (AND), dokter harus sesuai poli. Kunjungan awal mengisi tanggal server Asia/Jakarta dan meminta pengguna memilih filter. Dokter pada dropdown mengikuti poli.
- `GET /pasien/jadwal/data?tanggal=YYYY-MM-DD&idPoli=...&idDokter=...`: snapshot `{jadwal: [...], diperbaruiPada: "HH:mm:ss"}`. Setiap item DTO memuat `idJadwal`, `idDokter`, `dokter`, `spesialisasi`, `idPoli`, `poli`, `tanggal`, `sesi`, `jamMulai`, `jamSelesai`, `kuotaTotal`, `sisaKuota`, `status`. Jam memakai HH:mm, status kuota Tersedia/Penuh. Tidak ada data identitas pasien. Respons memakai Cache-Control no-store.
- Hasil kosong adalah 200 dengan daftar kosong/pesan Jadwal tidak tersedia. Input tidak valid 400; gangguan akses DB 503 dengan pesan umum. Semua rute tetap di prefix pasien dan layanan memvalidasi identitas pasien dari sesi.
- Polling nominal 5 detik, satu permintaan aktif, timeout 8 detik, dan tombol perbarui manual. Data lama ditandai bila refresh gagal; pilihan jadwal dinonaktifkan sampai snapshot segar tersedia. Redirect login/akses berubah diperlakukan sebagai sesi kedaluwarsa, bukan snapshot sukses.
- `GET /pasien/jadwal/pilih?idJadwal=...` membaca ulang jadwal; Penuh ditolak 409, ID hilang/tidak ditemukan 404. Pada UC-02, pilihan valid menghasilkan redirect 302 ke `/pasien/booking?idJadwal=...`. GET tidak membuat tiket, mengurangi kuota, atau menerima ID pasien dari form. Ringkasan UC-02 juga memeriksa waktu berakhir sesi.

`JadwalService`, `JadwalDto` dan query bersama dapat digunakan tahap berikutnya; aturan tulis serta penguncian UC-02 tetap mengikuti kontrak transaksi di bawah. Pemeriksaan kuota pada pilihan UC-01 bukan reservasi; UC-02 wajib memvalidasi ulang dalam transaksi.

### UC-02 yang disediakan

PIC Jefry. Acuan: Modul 2 UC-02 hlm. 5, PC-02 hlm. 87-88, UI-02 hlm. 99; Hi-Fi RSI `115:1129`. Tidak menambah entitas/migrasi atau mengubah status kontrak.

- `GET /pasien/booking?idJadwal=...` menampilkan ringkasan jadwal dan pasien dari sesi. `GET /pasien/booking/konfirmasi?idJadwal=...` menyiapkan form dengan draft terikat sesi/pasien/jadwal. Tanpa ID, kembali ke jadwal. Keduanya tidak menulis DB atau mencadangkan slot.
- `POST /pasien/booking/konfirmasi` menerima `bookingToken` dan CSRF. ID pasien dari CurrentUserService, ID jadwal dari draft server; parameter tambahan bukan sumber keduanya. Draft berlaku 15 menit, maksimal 16 per sesi. PK tiket 12 karakter acak menjadi kunci retry, tanpa kolom/migrasi baru.
- BookingService mengunci JadwalPraktik lalu Antrean, mengecek tiket token yang sama, lalu memvalidasi kuota positif dan waktu server sebelum akhir sesi. Nomor baru adalah MAX seluruh nomor per jadwal + 1, termasuk tiket berstatus akhir; tidak menggunakan ulang nomor yang dibatalkan. INSERT Terdaftar dan pengurangan kuota atomik. Respons sukses 303 menuju tiket. Retry token sama mengembalikan tiket sama tanpa pengurangan lagi, termasuk saat kuota kini nol/sesi berakhir. Ini tidak menetapkan UNIQUE pasien/jadwal yang tidak ada di PDM.
- `POST /pasien/booking/batal` membuang draft yang belum tersimpan dan 303 ke jadwal tanpa efek DB. Jika form lama sudah menghasilkan tiket, arahkan ke tiket itu; tidak mengubah status/kuota. Pembatalan tiket tersimpan mengikuti UC-04.
- `GET /pasien/booking/tiket/{idAntrean}` memuat tiket sendiri melalui query kepemilikan. Tiket pasien lain/tidak dikenal 404, peran lain 403; respons pribadi no-store. Tautan `/pasien/antrean/{idAntrean}` dilayani oleh UC-03 untuk status, posisi dan polling; alias detail statis dari UC-02 telah diganti.
- Kuota penuh/sesi berakhir: 409, maksimal lima alternatif poli sama dengan kuota positif dan waktu akhir belum lewat, diurutkan tanggal/jam. ID/draft tidak valid atau kedaluwarsa: 400; jadwal hilang: 404; gangguan DB: 503 dengan pesan umum dan jalur kembali/coba lagi. Simpan gagal me-rollback tiket dan kuota.

DTO `Ringkasan` berisi `pasien: {nama, noRekamMedis}` dan `jadwal: JadwalDto`. DTO `Tiket` berisi `idAntrean`, `nomorAntrean`, `statusAntrean`, `waktuBooking`, `pasien` dan `jadwal`. Contoh sintetis hasil booking JDW-003 pada database demo baru: `idAntrean=7X4HD9P2Q6KC`, `nomorAntrean=2`, `statusAntrean=Terdaftar`, `pasien.nama=Pasien Demo Dua`, `pasien.noRekamMedis=DEMO-RM-002`, `jadwal.sisaKuota=8`. Token/waktu aktual dibangkitkan server. Nama/noRM hanya untuk pemilik tiket; tidak ada NIK/password/hash di DTO. Gunakan ID tiket dan repository bersama untuk UC-03/05, dengan pemeriksaan kepemilikan/status baru pada tiap tindakan.

Penguncian dan rollback UC-02 telah diverifikasi pada H2 READ_COMMITTED melalui dua thread dan trigger kegagalan UPDATE, bukan hanya mock repository. PostgreSQL tetap memerlukan regresi transaksi pada DB-01.

### UC-03 yang disediakan

PIC Jefry. Acuan: UC-03 hlm. 5-6, PC-03 hlm. 88-89, UI-03 hlm. 101 dan Hi-Fi RSI `111:777`. Mengikuti nama status dan hitungan aktif pada kontrak ini ketika teks UC lama menyebut Sedang Diperiksa/Terlewat/Batal atau selisih nomor. Tidak menambah skema, seed, status, login atau transaksi tulis.

- `GET /pasien/antrean` menampilkan tiket pasien dari sesi. Pilihan melalui `?tiket={idAntrean}` atau `/pasien/antrean/{idAntrean}`; ID yang tidak dimiliki dan tidak ada sama-sama 404. ID kosong/tidak valid atau lebih dari 12 karakter 400. Urutan pilihan: tiket aktif dahulu, tanggal menaik lalu nomor menurun; status akhir tetap bisa dipilih. Tanpa tiket: 200 dengan keadaan kosong.
- `GET /pasien/antrean/data` membaca pilihan awal; `/pasien/antrean/{idAntrean}/data` membaca tiket tertentu. Semua respons pribadi/galat no-store. Peran lain 403, anonim diarahkan login, gangguan DB 503 dengan pesan umum. ID pasien pada parameter tidak digunakan.
- `Snapshot` berisi `tiket` (pilihan tiket sendiri), `dipilih` (Detail atau null), dan `diperbaruiPada` HH:mm:ss Asia/Jakarta. Pilihan memuat idAntrean, nomorAntrean, statusAntrean, tanggal, poli, sesi. Detail memuat idAntrean, nomorAntrean, statusAntrean, JadwalDto, nomorSedangDilayani (daftar angka), jumlahDiDepan, berikutnya, statusAkhir, bolehBatal, bolehCheckIn dan keterangan.
- `jumlahDiDepan` menghitung tiket dengan nomor lebih kecil pada idJadwal yang sama dan status Terdaftar/Menunggu/Hadir/Menunggu/Dilewati/Sedang Dilayani. Selesai/Dibatalkan/Tidak Hadir dikecualikan. Untuk tiket sendiri berstatus akhir atau Sedang Dilayani, posisi menunggu null; UI menampilkan tanda pisah dan keterangan status. Dilewati masih aktif, bukan status akhir.
- `nomorSedangDilayani` berisi seluruh nomor berstatus Sedang Dilayani pada sesi yang sama; kosong bila belum ada. Daftar `berikutnya` maksimal lima nomor/status aktif yang belum Sedang Dilayani, urut nomor. Tidak memuat identitas/ID tiket pasien lain. Daftar adalah urutan nomor, bukan implementasi keputusan panggilan UC-06. Estimasi menit tidak dibuat tanpa aturan durasi layanan.
- Pembacaan multi-query memakai transaksi read-only REPEATABLE_READ untuk snapshot yang konsisten. Tidak mengunci/menulis kuota. DB-01 wajib menguji kembali snapshot/isolation ini pada PostgreSQL.
- Polling 5 detik, timeout 8 detik, satu request aktif, refresh manual, pulih dari data lama dan pemeriksaan redirect/content-type/sesi kedaluwarsa. Kegagalan mempertahankan data terakhir dengan penanda dan menonaktifkan tautan tindakan sampai snapshot berhasil. Tanpa JavaScript gunakan reload.
- `bolehBatal`: status Terdaftar/Menunggu/Hadir/Menunggu/Dilewati dan waktu tersisa sebelum jam mulai lebih dari 60 menit. `bolehCheckIn`: Terdaftar, hari jadwal sama, waktu dari 30 menit sebelum mulai sampai jam selesai inklusif. Ini hanya kelayakan tampilan; operasi UC-04/05 wajib memeriksa ulang di server pada transaksi tulis.
- GET `/pasien/antrean/{idAntrean}/pembatalan` disediakan UC-04 dengan konfirmasi dan POST. `/check-in` kini disediakan UC-05 dengan POST pencatatan kehadiran. Keduanya memeriksa kepemilikan dan kelayakan; membuka GET tidak mengubah status/kuota.

Contoh dataset sintetis awal: ANT-DEMO0004 milik pasien2 pada JDW-001 menghasilkan nomorAntrean=4, statusAntrean=Menunggu, nomorSedangDilayani=[1], jumlahDiDepan=3. Setelah tiket 001 selesai dan 002 Sedang Dilayani, snapshot berikutnya menghasilkan nomorSedangDilayani=[2], jumlahDiDepan=2. Tiket Dibatalkan memiliki jumlahDiDepan=null, berikutnya=[], statusAkhir=true. Tidak ada nama/noRM/NIK/password pada snapshot status.

### UC-04 yang disediakan

PIC Jefry. Acuan UC-04 hlm. 6-7, PC-04 hlm. 89, UI-04 hlm. 103 dan Hi-Fi RSI `111:1005`. Status yang diizinkan mengikuti kontrak bersama, termasuk Dilewati. Tidak menambah skema/seed/migrasi/status atau mengubah login.

- `GET /pasien/antrean/{idAntrean}/pembatalan` menampilkan detail sendiri, syarat dan modal konfirmasi. Kembali/Escape menutup modal tanpa perubahan data. Tanpa JavaScript, konfirmasi tampil langsung. GET tidak mencadangkan atau mengubah kuota.
- `POST` pada URL yang sama menerima `konfirmasi=ya` dan CSRF. Identitas pasien dari sesi; parameter tambahan idPasien/idJadwal tidak digunakan. Konfirmasi hilang/tidak sesuai atau ID tidak valid 400, tiket tidak dikenal/bukan milik pasien 404, peran lain 403, status/waktu/kuota tidak memenuhi 409, gangguan DB 503. Pesan galat tidak membawa rincian koneksi DB.
- PembatalanService memakai transaksi tulis dengan urutan kunci JadwalPraktik lalu Antrean. Query awal hanya mengambil ID jadwal skalar dari tiket milik sesi agar entity tiket tidak dibaca ke persistence context sebelum kunci jadwal. Setelah kedua kunci diperoleh, kepemilikan, keterkaitan jadwal, status, waktu dan batas kapasitas diperiksa ulang.
- PembatalanPolicy dipakai bersama oleh kelayakan tampilan UC-03 dan penyimpanan UC-04. Status boleh: Terdaftar, Menunggu, Hadir/Menunggu, Dilewati. Waktu server Asia/Jakarta harus lebih dari 60 menit sebelum mulai. Sedang Dilayani/Selesai/Dibatalkan/Tidak Hadir dan tepat 60 menit atau kurang ditolak. Waktu dibaca setelah menunggu kunci.
- Perubahan menjadi Dibatalkan dan penambahan sisaKuota satu disimpan atomik. Jika kuota sudah mencapai kuotaTotal, tolak tanpa mengubah status. Kegagalan UPDATE kuota me-rollback status. Aksi ulang Dibatalkan menghasilkan 409 tanpa pengembalian lagi. Dua pembatalan berbeda diserialkan pada jadwal yang sama agar tidak kehilangan penambahan kuota.
- Sukses menghasilkan 303 ke `/pasien/antrean/{idAntrean}/pembatalan/hasil`. Halaman hasil hanya mengklaim berhasil jika status DB Dibatalkan; URL hasil sebelum pembatalan menghasilkan 409. Hasil dapat dimuat ulang tanpa penulisan. Pesan keberhasilan adalah notifikasi layar, tidak menambah Notifikasi loket yang merupakan UC-10.
- Seluruh respons pribadi no-store. Tautan kembali ke UC-03 dan kuota UC-01 memakai jadwal tiket yang sama; polling keduanya membaca DB terbaru. Tidak mengubah nomor, tanggal booking, waktu check-in atau waktu selesai tiket. Integrasi PostgreSQL DB-01 wajib mengulang uji rollback/persaingan ini.

Contoh demo baru: pasien/ANT-DEMO0006 pada JDW-003 besok, Terdaftar dengan sisaKuota=9. Setelah pembatalan: Dibatalkan, sisaKuota=10. Submit ulang tetap 10. Bila sebelumnya ada booking lain yang mengonsumsi satu kuota, hasil pengembalian menjadi 9, bukan mengatur ulang ke kuotaTotal.

### UC-05 yang disediakan

PIC Jefry. Acuan UC-05 hlm. 7-8, PC-05 hlm. 89-91, UI-05 hlm. 105; Hi-Fi desktop `89:579`, mobile `89:597`, peringatan `93:737`. Menggunakan Antrean/JadwalPraktik dan waktuCheckIn yang sudah ada. Tidak menambah entitas, migrasi, nilai status, seed atau pengaturan login.

- `GET /pasien/check-in` memilih tiket Terdaftar hari ini dari sesi pasien; tanpa kandidat 200 dengan keadaan kosong. Tiket tertentu melalui `GET /pasien/antrean/{idAntrean}/check-in`; data dibaca dari DB, halaman bukan milik pasien/tidak ditemukan 404, input tidak valid 400, kelayakan gagal 409. Root dan halaman detail memakai DTO Panel/Tiket.
- `POST /pasien/antrean/{idAntrean}/check-in` hanya menerima identitas tiket pada path serta CSRF. ID pasien dari sesi dan ID jadwal dari tiket server; parameter idPasien/idJadwal/waktuCheckIn klien diabaikan. Status wajib Terdaftar dan waktuCheckIn masih null. Peran lain 403, ulang/status/waktu tidak valid 409, gangguan simpan 503 tanpa rincian koneksi DB.
- CheckInPolicy digunakan bersama tampilan UC-03 dan penulisan UC-05. Hari sekarang harus sama dengan tanggal jadwal, waktu >= tanggal/jam mulai minus 30 menit dan <= jam selesai. Kedua batas inklusif; jendela yang secara aritmetika mulai pada hari sebelumnya tetap tidak mengizinkan check-in beda hari.
- CheckInService membaca ID jadwal skalar dari tiket milik sesi, mengunci JadwalPraktik lalu Antrean, memeriksa ulang pemilik/jadwal/status/waktuCheckIn, kemudian mengambil waktu server setelah kunci. Satu transaksi mengubah status ke Hadir/Menunggu dan menyimpan waktuCheckIn. Kegagalan simpan me-rollback kedua field; aksi ulang ditolak tanpa mengganti waktu awal. Kuota, nomor, tanggal booking dan waktu selesai tidak diubah.
- Sukses 303 menuju `/pasien/antrean/{idAntrean}/check-in/hasil`. Halaman hasil baru menampilkan keberhasilan jika waktuCheckIn sudah tercatat. Reload tidak menulis data. JSON `/pasien/antrean/{idAntrean}/check-in/data` memuat `{tiket, sekarang}` dengan waktu/status/jadwal sendiri, batasBuka/batasTutup, jendelaDibuka dan bolehCheckIn; membaca data tidak mengharuskan jendela sedang dibuka.
- `GET /loket/check-in` dan `/loket/check-in/data` hanya untuk PETUGAS_LOKET, termasuk pemeriksaan peran pada layanan. Snapshot memuat daftar `{idJadwal, nomorAntrean, poli, dokter, sesi, tanggal}` dan diperbaruiPada; tanpa nama/noRM/NIK/ID pasien atau ID tiket pasien lain. Baca multi-query memakai REPEATABLE_READ, tanpa operasi tulis.
- Definisi operasional karena ambang tidak diberi angka pada Modul 2: untuk setiap jadwal hari ini yang jendela check-in-nya terbuka, ambil dua nomor aktif terdepan yang belum Sedang Dilayani (Terdaftar/Menunggu/Hadir/Menunggu/Dilewati). Peringatkan hanya kandidat Terdaftar dengan waktuCheckIn null. Tiket final tidak mengisi posisi. Peringatan otomatis hilang ketika kondisi tersebut tidak berlaku lagi.
- Semua respons pribadi/galat no-store. Polling 5 detik, timeout 8 detik, satu request aktif dan refresh manual; kegagalan ditandai, submit dinonaktifkan sampai snapshot segar. Kartu peringatan dapat disembunyikan hanya pada halaman lokal dan ditampilkan kembali tanpa mengubah server. UC-06 kelak memakai layanan/transaksi panggilan terpisah; UC-05 tidak membuat panggilan, pelewatan atau notifikasi UC-10.
- DB-01 wajib mengulang uji penguncian, waktu, rollback dan pembacaan peringatan di PostgreSQL. Clock terkendali hanya ada di konfigurasi tes/helper UI sementara, bukan opsi baru untuk mengubah waktu check-in aplikasi normal.

| Pemilik | Permukaan integrasi yang disediakan | Konsumen |
| --- | --- | --- |
| MF | Baca `JadwalPraktik` menurut tanggal dan filter poli/dokter. Hasil memuat `idJadwal`, dokter, poli, tanggal, sesi, jam, kuota, dan status ketersediaan. Booking menerima `idJadwal` dan mengembalikan `idAntrean`, `nomorAntrean`, dan `sisaKuota`. | RA, NA, JL, AH |
| RA | Snapshot tiket pasien sesuai sesi login memuat `idAntrean`, `idJadwal`, `nomorAntrean`, `statusAntrean`, info sesi, nomor yang sedang dilayani, dan jumlah di depan. Halaman status menyediakan akses ke pembatalan dan check-in. | MF, NA |
| NA | Layanan panggil, mulai layanan, lewati, dan panggil ulang menerima konteks pengguna dan jadwal, lalu memvalidasi tiket/panggilan aktif. Hasil memuat status, nomor berikutnya jika ada, dan jumlah pelewatan saat relevan. Layanan check-in menerima tiket milik pasien dan mengembalikan status, nomor, serta waktu check-in. | RA, AH |
| JL | Layanan identitas pasien dan dokter memakai identitas dari sesi. Pencarian/detail pasien dibatasi untuk loket. Riwayat operasional membaca Antrean dan JadwalPraktik. | Semua |
| AH | Snapshot dashboard per dokter dan hari kerja, dikelompokkan per sesi. Tindakan selesai menghasilkan status, waktu selesai, snapshot terbaru, dan status notifikasi. Loket bisa membaca notifikasi internal yang dihasilkan. | NA |

Gunakan DTO untuk data tampilan. Format tanggal `YYYY-MM-DD` dan jam `HH:mm` pada UI. Pastikan respons status dan jumlah dibaca ulang dari DB. Refresh jadwal, status pasien, dan dashboard dokter otomatis. Untuk demo, mulai dengan polling 5 detik dan tampilkan keadaan gagal/kedaluwarsa bila refresh gagal. Pada halaman antrean umum, tampilkan nomor dan informasi sesi yang diperlukan, tanpa identitas pasien.

## Status, transaksi, dan kasus batas

| Peristiwa | Perubahan |
| --- | --- |
| Booking (MF) | Buat `Terdaftar`; kurangi `sisaKuota` tepat sekali. Kunci jadwal sebelum menghitung kuota dan nomor berikutnya. |
| Batal (RA) | `Terdaftar`, `Menunggu`, `Hadir/Menunggu`, atau `Dilewati` → `Dibatalkan` jika waktu tersisa **lebih dari 60 menit**. Kembalikan satu kuota. |
| Check-in (NA) | `Terdaftar` → `Hadir/Menunggu` pada hari jadwal dari **30 menit sebelum mulai sampai jam selesai**, termasuk kedua batas. Simpan `waktuCheckIn`. |
| Hadir saat dipanggil (NA) | Panggilan aktif yang kehadirannya dikonfirmasi → `Sedang Dilayani`. Validasi dokter/loket, sesi, dan tiket yang dipanggil. |
| Lewati (NA) | Simpan setiap kejadian. Percobaan 1–2 menghasilkan `Dilewati` dan kesempatan dipanggil ulang. Percobaan 3 menghasilkan `Tidak Hadir` dan mengembalikan satu kuota. Nomor tiket tetap. |
| Pemeriksaan selesai (AH) | `Sedang Dilayani` → `Selesai`, simpan `waktuSelesai`. Sesudah status berhasil disimpan, coba buat atau kirim notifikasi loket. Laporkan `Tertunda` atau `Gagal` sesuai hasil. |

- Pertahankan semua nilai status sesuai DDL termasuk `Menunggu`. `Selesai`, `Dibatalkan`, dan `Tidak Hadir` adalah status akhir. Cek status lagi saat menulis dan tolak aksi ulang yang akan mengulang efek samping.
- Untuk operasi gabungan, ambil kunci `JadwalPraktik` lalu `Antrean` dalam urutan yang sama. Simpan perubahan kuota, status, dan riwayat terkait pada transaksi yang sama. Pertahankan `0 ≤ sisaKuota ≤ kuotaTotal`.
- Gunakan satu logika pemanggilan dan pelewatan milik NA untuk loket dan dashboard dokter. Bandingkan waktu panggilan dengan batas 120 detik di server dan cegah submit ganda pada panggilan yang sama. Jika data panggilan perlu disimpan, tambahkan migrasi yang disepakati JL, NA, dan AH.
- Pisahkan arti hitungan. UC-03 memakai jumlah antrean aktif dengan nomor lebih kecil dalam sesi yang sama (PC-03). UC-09/10 menampilkan jumlah entri berstatus `Menunggu` atau `Hadir/Menunggu` (PC-09/10). Gunakan layanan pemanggilan NA untuk memilih pasien berikutnya.
- Untuk repo baru gunakan `src/main/java/id/ac/ub/klinikleonardo/<fitur>`, `src/main/resources/templates/<fitur>`, dan `src/main/resources/static/<fitur>`. Simpan migrasi di `src/main/resources/db/migration`. Pilih satu mekanisme migrasi dalam SETUP-00, dengan Flyway sebagai usulan.
