# Panduan kontribusi

## Sebelum mulai

Ikuti langkah instalasi di [README](README.md). Setiap anggota clone repositori yang sama setelah menerima undangan collaborator.

Di folder proyek, atur identitas commit menggunakan identitas GitHub milik sendiri:

~~~powershell
git config user.name "Nama kamu"
git config user.email "Email commit GitHub kamu"
~~~

Gunakan alamat noreply dari GitHub bila ingin menyembunyikan email pribadi.

## Satu tugas, satu branch, satu pull request

Ambil satu Issue, tetapkan PIC, dan tuliskan kriteria selesai sebelum mengubah kode.

~~~powershell
git switch main
git pull --ff-only origin main
git switch -c feat/nama-fitur
~~~

Contoh nama branch: `feat/kelola-obat`, `fix/validasi-form`, `docs/model-data`. Contoh tersebut hanya pola nama, bukan penetapan cakupan fitur.

Setelah mengubah kode:

~~~powershell
.\mvnw.cmd --batch-mode verify
git status
git add path/file-yang-diubah
git diff --cached
git commit -m "feat: tambahkan nama fitur"
git push -u origin feat/nama-fitur
~~~

Ganti path dan pesan sesuai perubahanmu. Pilih berkas secara sengaja, lalu periksa diff sebelum commit.

Buka pull request dengan base `main`. Isi template, cantumkan `Closes #nomor-issue`, jelaskan cara menguji, dan minta review satu anggota lain. Gunakan draft PR jika belum selesai.

## Review dan penggabungan

- Reviewer memeriksa perilaku, aturan bisnis, perubahan database, dan kesesuaian dengan Modul 2.
- Penulis memperbaiki masukan dan memastikan check **Build dan Test** hijau.
- Setelah minimal satu persetujuan teman, gunakan **Squash and merge**.
- Ambil versi `main` terbaru sebelum memulai tugas berikutnya.

Ketentuan review ini adalah kesepakatan tim. Penegakannya oleh GitHub memerlukan pengaturan branch oleh pemilik; lihat [pengaturan tim](docs/SETUP_TIM.md).

## Jika branch tertinggal atau konflik

Pastikan perubahan lokal sudah di-commit sebelum memperbarui branch:

~~~powershell
git fetch origin
git merge origin/main
~~~

Jika konflik muncul, bahas dengan penulis kode terkait, perbaiki penanda konflik, jalankan test kembali, lalu commit dan push hasilnya. Jika perlu membatalkan merge yang sedang berkonflik, gunakan `git merge --abort`. Hindari force push pada branch bersama.

## Aturan struktur dan database

- Ikuti [arsitektur BCE](docs/ARSITEKTUR.md).
- Sepakati perubahan `pom.xml`, konfigurasi umum, dan model data dengan PIC terkait.
- Sertakan perubahan skema dan langkah penerapan saat menambah entitas.
- Commit source, template, konfigurasi contoh, dan test; `target/`, `.env`, dan konfigurasi IDE lokal sudah diabaikan.
- Gunakan data dummy dalam contoh, screenshot, dan pengujian.
