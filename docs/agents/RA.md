# RA · Rakha Ahmad Fabiano

Pemilik status antrean pasien dan pembatalan. Rujukan Modul 2 UC-03/04, PC-03/04 dan UI-03/04, serta Modul 3 RA–SC02 (hlm. 14–17).

| Issue | Branch | Modul yang dikerjakan |
| --- | --- | --- |
| UC-03 | `feat/uc-03-status-antrean` | `antreanpasien` · nomor, status, dan refresh |
| UC-04 | `feat/uc-04-batal-antrean` | `antreanpasien` · konfirmasi dan transaksi pembatalan |

## Kerjakan

1. UC-03 memakai tiket hasil MF. Tampilkan info dokter, poli, sesi, nomor pasien, nomor yang sedang dilayani, jumlah di depan, dan status terbaru. Tampilkan keadaan tanpa tiket dan status akhir. Saat refresh gagal, tampilkan bahwa data terakhir belum diperbarui.
2. Sambungkan tombol Check-in ke UC-05 milik NA dan tombol Batalkan ke UC-04. Tampilkan tombol sesuai kondisi tiket. Pastikan server memeriksa kepemilikan sesi untuk semua tindakan.
3. UC-04 memerlukan konfirmasi. Batalkan hanya dari status yang diizinkan dan saat waktu tersisa lebih dari 60 menit. Simpan status Dibatalkan dan kembalikan satu kuota pada transaksi yang sama. Tampilkan hasil yang jelas.

## Selesai jika

- Data pasien berubah otomatis saat loket atau dokter mengubah status. Tiket pasien lain tidak dapat dilihat atau dibatalkan.
- Pembatalan pada 61 menit berhasil dan menambah kuota satu kali. Pembatalan pada 60 menit atau kurang, aksi ulang, dan tiket yang sedang dilayani ditolak.
- Setelah pembatalan, UC-03 menampilkan status Dibatalkan dan UC-01 menampilkan kuota terbaru.

**Reviewer integrasi:** MF untuk kuota dan NA untuk check-in.
