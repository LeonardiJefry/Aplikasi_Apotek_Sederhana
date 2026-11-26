# MF · M. Furqanurrahman

Pemilik jadwal praktik dan booking pasien. Rujukan Modul 2 UC-01/02, PC-01/02 dan UI-01/02, serta Modul 3 MF–SC01 (hlm. 9–14).

| Issue | Branch | Modul yang dikerjakan |
| --- | --- | --- |
| UC-01 | `feat/uc-01-jadwal` | `jadwal` · filter jadwal dan tampilan kuota |
| UC-02 | `feat/uc-02-booking` | `booking` · ringkasan, konfirmasi, dan nomor antrean |

## Kerjakan

1. Bantu JL pada SETUP-00 dengan PDM dan fixture Poli, Dokter, JadwalPraktik, dan Antrean. Dokumentasikan field DTO jadwal dan tiket untuk RA, NA, JL, dan AH.
2. UC-01 menampilkan dokter, poli, sesi, jam, sisa kuota, dan label Tersedia/Penuh. Validasi tanggal dan minimal satu filter poli/dokter. Perbarui kuota otomatis dan tampilkan pesan saat daftar kosong atau gagal dimuat.
3. UC-02 menampilkan ringkasan untuk pasien yang sedang login. Saat konfirmasi, kunci jadwal, cek lagi kuota dan validitas jadwal, buat nomor unik per sesi berstatus Terdaftar, kurangi kuota, lalu tampilkan hasil serta tautan ke UC-03. Identitas pasien berasal dari sesi.

## Selesai jika

- Filter menampilkan jadwal yang sesuai. Booking pada jadwal Penuh dinonaktifkan di UI dan ditolak server.
- Dua permintaan yang bersaing untuk satu slot terakhir menghasilkan satu tiket. Pada kegagalan simpan, kuota dan tiket tetap konsisten.
- Nomor tiket yang dihasilkan bisa dimuat oleh RA dan NA. Uji dari UI dan catat contoh DTO pada PR.

**Reviewer integrasi:** RA untuk tiket, NA untuk check-in, dan JL untuk migrasi.
