# Proyek Klinik Leonardo

Klinik Leonardo adalah web Klinik Pratama Sehat Bersama. Pasien melihat jadwal dan kuota, booking, menerima nomor, memantau antrean, membatalkan jika memenuhi batas, lalu check-in. Loket memverifikasi pasien dan menangani nomor yang terlewat. Dokter memantau jadwalnya dan menyelesaikan pemeriksaan. Admin mengatur kuota vaksinasi, yang memakai jadwal dan kapasitas tersendiri.

## Pembagian dan urutan kerja

| Tahap | Judul issue | Pemilik | Rujukan | Dependensi |
| --- | --- | --- | --- | --- |
| 0 | [SETUP-00] Fondasi aplikasi, login, MySQL dan data demo | JL + MF | Modul 3 §3.1 | Repo dan panduan ada |
| 1 | [UC-01] Jadwal praktik dan sisa kuota | [MF](agents/MF.md) | PC-01, UI-01 | SETUP-00 |
| 1 | [UC-02] Booking antrean online | [MF](agents/MF.md) | PC-02, UI-02 | UC-01 |
| 2 | [UC-03] Pantau status dan nomor antrean pasien | [RA](agents/RA.md) | PC-03, UI-03 | UC-02 |
| 2 | [UC-04] Batalkan pendaftaran antrean | [RA](agents/RA.md) | PC-04, UI-04 | UC-02, UI dari UC-03 |
| 2 | [UC-05] Check-in kedatangan pasien | [NA](agents/NA.md) | PC-05, UI-05 | UC-02, tautan dari UC-03 |
| 3 | [UC-06] Panggil, lewati, dan panggil ulang antrean | [NA](agents/NA.md) | PC-06, UI-06 | UC-02, UC-05, integrasi tindakan loket dan dokter |
| 1 | [UC-07] Kelola kuota harian vaksinasi | [JL](agents/JL.md) | PC-07, UI-07 | SETUP-00 |
| 1 | [UC-08] Cari dan verifikasi data pasien | [JL](agents/JL.md) | PC-08, UI-08 | SETUP-00, data demo. Gunakan UC-02 untuk cek riwayat terintegrasi. |
| 2 | [UC-09] Dashboard antrean dokter | [AH](agents/AH.md) | PC-09, UI-09 | UC-02. Aksi panggil/lewati tersambung setelah kontrak UC-06 siap. |
| 3 | [UC-10] Selesaikan antrean dan beri notifikasi loket | [AH](agents/AH.md) | PC-10, UI-10 | UC-09, UC-06 untuk cek dari status Sedang Dilayani |

SETUP-00 menghasilkan build yang bisa dijalankan dari clone baru, konfigurasi profil `dev`, migrasi awal sesuai PDM, role dan akun demo, layout umum, serta data sintetis. JL menyiapkan build, akses, dan migrasi. MF memasok skema dan fixture poli/dokter/jadwal/antrean. Setelah fondasi masuk, MF dan JL bisa mengerjakan fiturnya secara paralel. RA, NA, dan AH bisa memulai UI dan verifikasi dengan fixture yang sama. PR fitur menyatakan dependensi yang diperlukan untuk skenario lintas modul.

## Cakupan brief awal

Brief awal mencantumkan daftar fitur produk. Modul 2 dan 3 merinci 10 use case pada tabel kerja. Item brief yang masih memerlukan rancangan dicatat di bawah agar tim dapat menentukan kelanjutannya setelah demo utama.

| No. brief | Kebutuhan | Status rancangan dan pemilik |
| --- | --- | --- |
| 1–2 | Simpan dan ubah data personal | JL memiliki master pasien untuk UC-08. Registrasi pasien dan edit profil mandiri memerlukan issue lanjutan. |
| 3–4 | Booking dan nomor antrean digital | MF, RA, NA, dan AH melalui UC-02 sampai UC-06 serta UC-09/10. |
| 5 | Rekam medis elektronik | AH, usulan lanjutan. Riwayat antrean/kunjungan UC-08 mencakup data operasional. RME perlu model dan hak akses sendiri. |
| 6 | Konsultasi dokter online | AH, usulan lanjutan. Perlu rancangan alur serta kanal. |
| 7 | Jadwal dokter real-time | MF, UC-01. |
| 8 | Rincian tagihan dan integrasi perbankan | MF, usulan lanjutan. Perlu rancangan tagihan dan lingkungan uji penyedia pembayaran. |
| 9 | Tiket aduan dan pelacakan status | NA, usulan lanjutan. Perlu rancangan alur petugas dan status aduan. |
| 10 | Artikel kesehatan | RA, usulan lanjutan. Perlu rancangan publikasi dan pemilik konten. |

## Cek demo

1. Pasien A membuka jadwal, booking slot, dan mendapat nomor. Perebutan satu slot terakhir dari dua sesi menghasilkan satu sukses. Halaman jadwal memperbarui kuota.
2. Pasien A melihat nomor dan status. Coba pembatalan dengan waktu lebih dari satu jam dan pada batas satu jam. Reset fixture untuk jalur check-in. Pasien B hanya bisa mengakses antreannya sendiri.
3. Pasien A check-in dalam rentang waktu yang benar. Loket memanggil dan memulai layanan. Dengan fixture pasien lain, coba dua pelewatan, pemanggilan ulang, dan transisi ke Tidak Hadir saat percobaan ketiga. Cek kuota dan riwayat.
4. Dokter membuka dashboard, menyelesaikan pasien yang sedang dilayani, dan melihat jumlah menunggu diperbarui. Loket melihat notifikasi atau status Tertunda saat kanal demo belum tersedia.
5. Admin menguji kuota vaksin di bawah peserta terdaftar dan kuota sama dengan peserta terdaftar. Loket mencari pasien, memeriksa riwayat, dan menyimpan verifikasi. Coba akses dengan peran yang keliru.

## Acuan dan keputusan tim

- [Brief Kelompok 1](https://chatgpt.com/library?file_id=libfile_f260d6ff3c408191b8e62652465042a9&account_id=personal) memuat nama proyek, studi kasus, dan cakupan awal.
- [Modul 2](https://chatgpt.com/library?file_id=libfile_f5ed14cf7d7481918d3c53cde7a4b176&account_id=personal) memuat UC/PC/UI. Gunakan sketsa UI-01 sampai UI-10 dan [Hi-Fi RSI](https://www.figma.com/design/SdPjXAnUkru2hynXut8cQU/RSI?node-id=1-6&t=TeLYEHyElE58FfpF-1). Bagikan akses ke lima anggota dan catat frame yang dipakai per issue.
- [Modul 3](https://chatgpt.com/library?file_id=libfile_6439d173688c8191b1da128bb704e00d&account_id=personal) §3.1–3.2 memuat DDL MySQL dan contoh PHP/PDO. Bagian §3.3–3.4 masih memuat contoh “Jalan Skuy” dan matriks yang belum lengkap. Implementasi web memakai sketsa klinik dan pemetaan di atas.
- Tim menggunakan logika modul PHP sebagai rujukan perilaku untuk rencana Spring Boot. UC-08 mengikuti spesifikasi Modul 2 untuk pencarian nama dan audit verifikasi yang belum tercakup di PC-08 versi PHP. Lihat [kartu JL](agents/JL.md).
- Tautan laporan ChatGPT mengikuti akses masing-masing akun. Unggah salinan ke lokasi bersama tim yang sudah diizinkan dan perbarui tautan ini agar semua anggota bisa membuka sumber yang sama.
