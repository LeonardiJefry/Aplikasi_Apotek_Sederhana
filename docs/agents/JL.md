# JL · M. Jefry Leonardi

PIC tunggal SETUP-00, UC-01 sampai UC-10, QA-01 dan DB-01 adalah Jefry (JL). Baca [roadmap](../PROJECT.md) untuk branch dan urutan. Spesifikasi UC-01/02: [MF](MF.md); UC-03/04: [RA](RA.md); UC-05/06: [NA](NA.md); UC-09/10: [AH](AH.md). Nama kartu historis dipertahankan sebagai rujukan Modul 2/3.

Rujukan UC-07/08: Modul 2 PC-07/08 dan UI-07/08, serta Modul 3 JL-SC04 (hlm. 25-29).

| Issue | Branch | Modul yang dikerjakan |
| --- | --- | --- |
| SETUP-00 | `chore/setup-00-fondasi` | Build, login/peran, migrasi awal, layout, H2 demo/test, seed sintetis dan CI |
| UC-07 | `feat/uc-07-kuota-vaksinasi` | `vaksinasi` · ubah kuota pada jadwal vaksin |
| UC-08 | `feat/uc-08-verifikasi-pasien` | `pasien` · pencarian, detail, riwayat, dan audit verifikasi |

## Kerjakan

1. SETUP-00 menyediakan Maven Wrapper, H2 runtime tanpa server database, empat peran, akun sintetis, dan tata letak. Gunakan migrasi serta model yang sudah ada, profil demo default dan test terisolasi. Cek verify, startup, login/peran, CSRF, fixture dan startup kedua. Catat hasil aktual dan keterbatasan lingkungan pada PROGRESS.md. MySQL opsional; Supabase ditunda ke DB-01 setelah QA-01 lulus dan masuk main.
2. UC-07 hanya untuk admin. Pilih jadwal dari tanggal dan jenis vaksin. Tolak jadwal tidak ditemukan atau hasil ambigu hingga ada ID yang jelas. Kuota integer minimal sebesar jumlah peserta terdaftar. Sisa = kuota − terdaftar. Sisa nol berarti Penuh dan pendaftaran ditutup. Gunakan jadwal demo yang tersedia untuk verifikasi.
3. UC-08 hanya untuk petugas loket. Cari berdasarkan NIK (16 digit), nomor rekam medis, atau nama sesuai Modul 2. Jika ada beberapa hasil, petugas memilih pasien. Tampilkan identitas, riwayat pendaftaran, dan riwayat kunjungan operasional dari antrean selesai. Saat verifikasi, simpan ID pasien, identitas petugas dari sesi, serta waktu verifikasi. Tambahkan migrasi audit yang disepakati setelah membandingkan PDM dan PC-08.

## Selesai jika

- Repo dapat dijalankan dari clone baru dengan H2 memori dan data sintetis tanpa .env/server DB. Masing-masing peran hanya melihat menu dan tindakan yang diizinkan. Profil test terpisah dan CI memakai H2. Startup kedua membuat fixture baru pada JVM baru.
- Kuota di bawah jumlah terdaftar ditolak. Kuota sama dengan jumlah terdaftar menghasilkan Penuh. Kuota di atas jumlah terdaftar menghasilkan Tersedia.
- Pencarian NIK, nomor rekam medis, dan nama bekerja. Data kosong dan input tidak valid memiliki pesan. Audit verifikasi tetap ada setelah reload dan bisa dikaitkan dengan petugas.

**Review dan integrasi:** Jefry meninjau hasil, menjalankan cek tertunda, lalu melakukan commit, push, PR dan merge. UC-01 dimulai setelah SETUP-00 terverifikasi dan masuk main; DB-01 setelah QA-01 lulus dan masuk main.
