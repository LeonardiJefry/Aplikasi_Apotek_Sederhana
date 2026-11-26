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
