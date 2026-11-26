# JL · M. Jefry Leonardi

Pemilik kuota vaksinasi dan pencarian serta verifikasi pasien. Rujukan Modul 2 UC-07/08, PC-07/08 dan UI-07/08, serta Modul 3 JL–SC04 (hlm. 25–29).

| Issue | Branch | Modul yang dikerjakan |
| --- | --- | --- |
| SETUP-00 | `chore/setup-00-fondasi` | Build, login/peran, migrasi awal, layout, dan seed demo bersama MF |
| UC-07 | `feat/uc-07-kuota-vaksinasi` | `vaksinasi` · ubah kuota pada jadwal vaksin |
| UC-08 | `feat/uc-08-verifikasi-pasien` | `pasien` · pencarian, detail, riwayat, dan audit verifikasi |

## Kerjakan

1. SETUP-00 menyediakan Maven Wrapper, pengaturan DB lokal, empat peran, akun demo, dan tata letak. Masukkan delapan tabel PDM dari Modul 3 dalam migrasi awal, tambah tabel akses yang dibutuhkan, dan koordinasikan seed dengan MF. Catat cara menjalankannya. Tes ke MySQL lokal dari clone bersih.
2. UC-07 hanya untuk admin. Pilih jadwal dari tanggal dan jenis vaksin. Tolak jadwal tidak ditemukan atau hasil ambigu hingga ada ID yang jelas. Kuota integer minimal sebesar jumlah peserta terdaftar. Sisa = kuota − terdaftar. Sisa nol berarti Penuh dan pendaftaran ditutup. Gunakan jadwal demo yang tersedia untuk verifikasi.
3. UC-08 hanya untuk petugas loket. Cari berdasarkan NIK (16 digit), nomor rekam medis, atau nama sesuai Modul 2. Jika ada beberapa hasil, petugas memilih pasien. Tampilkan identitas, riwayat pendaftaran, dan riwayat kunjungan operasional dari antrean selesai. Saat verifikasi, simpan ID pasien, identitas petugas dari sesi, serta waktu verifikasi. Tambahkan migrasi audit yang disepakati setelah membandingkan PDM dan PC-08.

## Selesai jika

- Repo dapat dijalankan oleh semua anggota dengan DB sendiri dan data sintetis. Masing-masing peran hanya melihat menu dan tindakan yang diizinkan.
- Kuota di bawah jumlah terdaftar ditolak. Kuota sama dengan jumlah terdaftar menghasilkan Penuh. Kuota di atas jumlah terdaftar menghasilkan Tersedia.
- Pencarian NIK, nomor rekam medis, dan nama bekerja. Data kosong dan input tidak valid memiliki pesan. Audit verifikasi tetap ada setelah reload dan bisa dikaitkan dengan petugas.

**Reviewer integrasi:** MF untuk PDM dan seed, NA untuk akses petugas loket.
