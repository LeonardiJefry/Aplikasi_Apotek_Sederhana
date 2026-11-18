# Pengaturan tim di GitHub

## Yang sudah tersedia di kode

- Panduan clone, menjalankan aplikasi, dan pengujian.
- Struktur BCE dan halaman awal Thymeleaf.
- Template tugas, laporan bug, dan pull request.
- Workflow **Java CI** dengan job **Build dan Test**.
- Pengabaian konfigurasi lokal dan penyamaan format berkas.

## Undang anggota

Buka [Settings → Collaborators](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/settings/access), pilih **Add people**, dan masukkan username GitHub masing-masing anggota. Pastikan akun yang dipilih benar, lalu undang. Anggota harus menerima undangan sebelum bisa push branch.

Daftar anggota belum ditetapkan karena username teman-teman belum diberikan. Jangan berbagi akun atau password GitHub.

## Lindungi main

Bagian ini adalah panduan konfigurasi; keberadaan file ini tidak mengaktifkan proteksi branch.

Setelah collaborator bergabung dan CI pertama muncul, pemilik dapat membuka [Settings → Branches](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/settings/branches) dan menambahkan branch protection rule untuk `main`:

- **Require a pull request before merging**.
- **Require approvals**: 1 persetujuan.
- **Dismiss stale pull request approvals when new commits are pushed**.
- **Require conversation resolution before merging**.
- **Require status checks to pass before merging**: pilih check **Build dan Test** yang benar-benar muncul setelah workflow pertama berjalan.
- Biarkan **Allow force pushes** dan **Allow deletions** tidak aktif.

Jangan mewajibkan check yang namanya belum muncul. Pastikan ada anggota lain yang bisa mereview sebelum mewajibkan persetujuan; penulis PR tidak dapat menyetujui PR sendiri.

Pada [Settings → General](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/settings), gunakan **Squash merging** dan aktifkan **Automatically delete head branches** bila disepakati tim.

## Pembagian tugas

Buat Issue sebelum implementasi. Tentukan satu PIC dan satu reviewer per tugas; catat progres melalui Issue dan pull request. Tugas awal tersedia pada tab Issues. Nama anggota dan pembagian fitur tetap mengikuti kesepakatan kelompok.
