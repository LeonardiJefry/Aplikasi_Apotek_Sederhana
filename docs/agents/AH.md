# AH · Affan Herlino Tazakha Wijaya

Pemilik dashboard dokter, penyelesaian pemeriksaan, dan notifikasi loket. Rujukan Modul 2 UC-09/10, PC-09/10 dan UI-09/10, serta Modul 3 AH–SC05 (hlm. 29–34).

| Issue | Branch | Modul yang dikerjakan |
| --- | --- | --- |
| UC-09 | `feat/uc-09-dashboard-dokter` | `dashboarddokter` · daftar antrean per sesi dan tindakan panggil |
| UC-10 | `feat/uc-10-selesai-antrean` | `dashboarddokter`, `notifikasi` · status selesai dan pemberitahuan loket |

## Kerjakan

1. UC-09 menampilkan antrean untuk dokter yang sedang login pada hari itu, dikelompokkan menurut sesi. Tampilkan nomor, nama pasien yang ditangani dokter, status, jumlah menunggu, dan keadaan kosong. Perbarui otomatis. Hubungkan aksi panggil dan lewati ke layanan NA.
2. UC-10 hanya mengubah tiket milik jadwal dokter dan berstatus Sedang Dilayani. Simpan Selesai dan waktu selesai secara atomik. Perbarui daftar dan tampilkan pasien berikutnya dengan kontrak NA.
3. Buat notifikasi untuk loket setelah status Selesai tersimpan. Untuk demo gunakan status Tertunda sebagaimana Modul 3 dan tampilkan pemberitahuan yang tersimpan di panel loket. Jika pengiriman belum ada atau gagal, sampaikan statusnya. Percobaan ulang harus menghindari notifikasi ganda.

## Selesai jika

- Dashboard dokter hanya menampilkan jadwalnya sendiri dan berubah saat pasien booking/check-in. Tangani keadaan tanpa jadwal atau antrean.
- Tombol Selesai hanya berhasil untuk status Sedang Dilayani yang sah. Aksi dari dokter lain, status lain, dan pengulangan ditolak tanpa mengubah timestamp.
- Loket dapat melihat pemberitahuan yang berhasil tersimpan. Simulasi kegagalan pengiriman membuktikan bahwa Selesai tetap tersimpan dan dashboard dapat dimuat ulang.

**Reviewer integrasi:** NA untuk aksi pemanggilan dan panel loket, RA untuk tampilan status pasien.
