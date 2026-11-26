# NA · Muhammad Naufal Alfarrel

Pemilik check-in pasien dan proses pemanggilan loket. Rujukan Modul 2 UC-05/06, PC-05/06 dan UI-05/06, serta Modul 3 NA–SC03 (hlm. 18–25).

| Issue | Branch | Modul yang dikerjakan |
| --- | --- | --- |
| UC-05 | `feat/uc-05-check-in` | `checkin` · validasi tiket, waktu, dan kehadiran |
| UC-06 | `feat/uc-06-panggil-lewati` | `panggilan` · panggil, tunggu, lewati, panggil ulang |

## Kerjakan

1. UC-05 membuka tiket milik pasien pada hari jadwal dan berstatus Terdaftar. Dalam jendela 30 menit sebelum mulai hingga jam selesai, ubah ke Hadir/Menunggu dan simpan waktu check-in. Tampilkan konfirmasi dan nomor. Beri peringatan pada loket jika giliran pasien dekat dan pasien belum check-in.
2. UC-06 memanggil nomor sesuai urutan di sesi aktif. Buka pilihan Lewati setelah 120 detik berdasarkan waktu server dan tampilkan konfirmasi. Jika pasien hadir, mulai layanan. Jika dilewati, catat urutan dan hasil, kemudian lanjutkan panggilan. Panggil ulang pasien yang terlewat sesuai alur Modul 2 saat jam layanan masih berlangsung.
3. Percobaan pelewatan 1 dan 2 menghasilkan Dilewati. Percobaan 3 menghasilkan Tidak Hadir dan mengembalikan satu kuota dalam transaksi yang sama. Hubungkan aksi dari dashboard AH ke layanan yang sama. Bahas metadata panggilan dengan JL dan AH sebelum membuat migrasi.

## Selesai jika

- Check-in berhasil pada kedua batas waktu. Check-in di luar rentang, beda hari, beda pemilik, dan aksi berulang ditolak tanpa mengubah waktu yang tersimpan.
- Jam tunggu tetap konsisten setelah reload. Aksi ganda pada nomor yang sama tidak menggandakan riwayat atau penambahan kuota. Panggil ulang yang berakhir hadir dapat beralih ke Sedang Dilayani.
- UI loket dan dashboard dokter memakai aturan panggilan yang sama. Uji transisi dari booking MF hingga aksi selesai AH.

**Reviewer integrasi:** RA untuk UI pasien, AH untuk dashboard, dan JL untuk migrasi.
