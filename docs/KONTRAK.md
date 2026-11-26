# Kontrak integrasi

Gunakan nama yang sudah ada jika repo sudah punya kontrak setara. Untuk repo baru, nama di bawah adalah usulan awal untuk dibekukan dalam SETUP-00. Ubah kontrak lewat PR dan beri tahu anggota yang bergantung padanya.

## Data dan hak akses

| Data | Penanggung jawab | Pemakai |
| --- | --- | --- |
| `Pasien`, login, peran, ID pasien/dokter dalam sesi | JL | Semua |
| `Poli`, `Dokter`, `JadwalPraktik`, `Antrean` | MF merawat model dan repository | MF, RA, NA, JL, AH |
| `RiwayatPelewatan` dan metadata panggilan bila diperlukan | NA | NA, AH |
| `JadwalVaksinasi` dan data audit verifikasi | JL | JL |
| `Notifikasi` antrean selesai | AH | AH, NA (loket) |

- Gunakan MySQL 8 dan nama fisik konsisten dengan migrasi bersama. PDM Modul 3 menetapkan PK string, kuota integer, FK, serta nomor antrean positif dan unik per `idJadwal`. Bedakan `idAntrean` (identitas tiket) dari `nomorAntrean` (nomor tampilan per sesi).
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
