# Proyek Klinik Leonardo

Klinik Leonardo adalah web Klinik Pratama Sehat Bersama. Pasien melihat jadwal dan kuota, booking, menerima nomor, memantau antrean, membatalkan jika memenuhi batas, lalu check-in. Loket memverifikasi pasien dan menangani nomor yang terlewat. Dokter memantau jadwalnya dan menyelesaikan pemeriksaan. Admin mengatur kuota vaksinasi, yang memakai jadwal dan kapasitas tersendiri.

## Roadmap pengerjaan mandiri

PIC seluruh tahap adalah **Jefry (JL)**. Kerjakan berurutan satu tahap per branch. Kartu MF/RA/NA/AH tetap menjadi rujukan spesifikasi UC, bukan pembagian PIC. Status aktual berada di [PROGRESS.md](../PROGRESS.md).

| Tahap | Branch | Rujukan | Prasyarat masuk tahap |
| --- | --- | --- | --- |
| SETUP-00 Fondasi, login, layout, H2, seed dan CI | `chore/setup-00-fondasi` | [JL](agents/JL.md), Modul 3 §3.1 | Repo dan panduan |
| UC-01 Jadwal praktik dan sisa kuota | `feat/uc-01-jadwal` | [MF](agents/MF.md), PC-01, UI-01 | SETUP-00 terverifikasi dan masuk main |
| UC-02 Booking antrean online | `feat/uc-02-booking` | [MF](agents/MF.md), PC-02, UI-02 | UC-01 masuk main |
| UC-03 Pantau status dan nomor antrean pasien | `feat/uc-03-status-antrean` | [RA](agents/RA.md), PC-03, UI-03 | UC-02 masuk main |
| UC-04 Batalkan pendaftaran antrean | `feat/uc-04-batal-antrean` | [RA](agents/RA.md), PC-04, UI-04 | UC-03 masuk main |
| UC-05 Check-in kedatangan pasien | `feat/uc-05-check-in` | [NA](agents/NA.md), PC-05, UI-05 | UC-04 masuk main; tiket UC-02/03 |
| UC-06 Panggil, lewati, dan panggil ulang | `feat/uc-06-panggil-lewati` | [NA](agents/NA.md), PC-06, UI-06 | UC-05 masuk main |
| UC-07 Kelola kuota harian vaksinasi | `feat/uc-07-kuota-vaksinasi` | [JL](agents/JL.md), PC-07, UI-07 | UC-06 masuk main; fixture vaksinasi |
| UC-08 Cari dan verifikasi pasien | `feat/uc-08-verifikasi-pasien` | [JL](agents/JL.md), PC-08, UI-08 | UC-07 masuk main; riwayat UC-02 |
| UC-09 Dashboard antrean dokter | `feat/uc-09-dashboard-dokter` | [AH](agents/AH.md), PC-09, UI-09 | UC-08 masuk main; layanan UC-06 |
| UC-10 Selesai pemeriksaan dan notifikasi loket | `feat/uc-10-selesai-antrean` | [AH](agents/AH.md), PC-10, UI-10 | UC-09 masuk main |
| QA-01 Regresi lintas peran pada demo H2 | `test/qa-01-integrasi-demo` | Cek demo di bawah, kontrak | Seluruh UC-01 sampai UC-10 masuk main |
| DB-01 PostgreSQL Supabase | `chore/db-01-supabase` | Kontrak, PDM dan kebutuhan skema seluruh UC | QA-01 lulus dan masuk main |

SETUP-00 menghasilkan aplikasi yang dapat dijalankan dari clone baru dengan profil default `demo`: H2 di memori, migrasi Flyway V1/V2 yang sudah ada, akun empat peran, layout umum dan data sintetis. Profil `test` memakai H2 terpisah. MySQL tersimpan sebagai profil opsional historis; tidak diperlukan dalam alur harian atau CI. Data H2 dimulai ulang saat JVM berhenti. Commit, push, PR dan merge dilakukan Jefry. Status `review` berarti siap ditinjau dengan cek tertunda yang dicatat; `done` memerlukan verifikasi dan integrasi ke main, bukan sekadar working tree di branch main.

DB-01 memakai JDBC backend dan Flyway PostgreSQL dengan lokasi migrasi terpisah. Jangan mengubah migrasi MySQL historis atau menambahkan koneksi Supabase sebelum QA-01 selesai. Catat kebutuhan PostgreSQL selama pengerjaan fitur di PROGRESS.md.

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
- [Modul 2](https://chatgpt.com/library?file_id=libfile_7839acd877c88191ba0c3e8f535266f2&account_id=personal) memuat UC/PC/UI. Gunakan sketsa bagian 2.7 UI-01 sampai UI-10 dan [Hi-Fi RSI](https://www.figma.com/design/SdPjXAnUkru2hynXut8cQU/RSI?node-id=1-6&t=TeLYEHyElE58FfpF-1). Jefry mencatat frame yang dipakai per tahap.
- [Modul 3](https://chatgpt.com/library?file_id=libfile_eb81c58028d4819192d118600f137221&account_id=personal) §3.1-3.2 memuat DDL MySQL dan contoh PHP/PDO. SETUP-00 memakai delapan tabel pada bagian 3.1 halaman 2–8 sebagai baseline fisik. Implementasi web memakai Spring Boot, sketsa klinik, dan pemetaan di atas.
- Tim menggunakan logika modul PHP sebagai rujukan perilaku untuk rencana Spring Boot. UC-08 mengikuti spesifikasi Modul 2 untuk pencarian nama dan audit verifikasi yang belum tercakup di PC-08 versi PHP. Lihat [kartu JL](agents/JL.md).
- Tautan laporan ChatGPT mengikuti akses masing-masing akun. Unggah salinan ke lokasi bersama tim yang sudah diizinkan dan perbarui tautan ini agar semua anggota bisa membuka sumber yang sama.
