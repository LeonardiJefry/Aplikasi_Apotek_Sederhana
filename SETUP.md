# Setup kolaborasi Klinik Leonardo

Gunakan panduan ini bersama [AGENTS.md](AGENTS.md) dan [README](README.md) saat berkontribusi ke repo [Aplikasi_Apotek_Sederhana](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana).

## 1. Siapkan akses dan pembagian tugas

Setiap anggota memerlukan Git, akun GitHub, dan AI agent yang bisa membaca folder proyek. Gunakan Git Credential Manager atau metode login GitHub yang sudah dipakai masing-masing.

Pemilik repo mengundang empat anggota lewat [pengaturan Collaborators](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/settings/access). Terima undangan sebelum mulai bekerja.

Gunakan [Issue #2](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/issues/2) untuk mencatat pembagian tugas dan [Issue #3](https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana/issues/3) untuk koordinasi skema awal. Catat pekerjaan fondasi aplikasi, login, MySQL, dan data demo dengan ID [SETUP-00]. Buat satu issue per use case sesuai [daftar pekerjaan](docs/PROJECT.md#pembagian-dan-urutan-kerja), tetapkan pemilik, dan isi tautan kartu serta dependensinya. Nomor issue GitHub berbeda dari ID use case.

## 2. Gabung dari komputer masing-masing

Jalankan perintah ini saat pertama kali clone. Buka folder `Aplikasi_Apotek_Sederhana` sebagai workspace di IDE atau agent.

```bash
git clone https://github.com/LeonardiJefry/Aplikasi_Apotek_Sederhana.git
cd Aplikasi_Apotek_Sederhana
git status --short
```

Sebelum mulai satu issue baru, pastikan perubahan lokal sudah disimpan, lalu pakai contoh berikut. Ganti branch di baris terakhir sesuai kartu tugas. Untuk melanjutkan branch yang sudah dibuat, gunakan `git switch feat/uc-01-jadwal`.

```bash
git switch main
git pull --ff-only origin main
git switch -c feat/uc-01-jadwal
```

### Konfigurasi AI agent

| Alat | Cara pakai |
| --- | --- |
| Codex | Mulai sesi baru dari root repo. Codex membaca `AGENTS.md` di konteks proyek. Tempel prompt di bawah agar Codex tahu kartu dan issue yang harus dikerjakan. |
| Cursor, Copilot, Claude Code, atau agent lain | Buka root repo lalu gunakan prompt yang sama. Bila chat mendukung lampiran atau referensi file, masukkan `AGENTS.md`, kartu sendiri, dan `docs/KONTRAK.md`. |
| Agent yang hanya menerima teks | Tempel isi file panduan bersama, kartu sendiri, kontrak, dan deskripsi issue. Untuk mengedit langsung, agent perlu akses ke folder clone. |

**Prompt awal yang bisa disalin.** Ganti inisial, UC, dan nomor issue.

> Saya MF, mengerjakan UC-01 pada issue #N di repo Aplikasi_Apotek_Sederhana. Baca AGENTS.md, docs/PROJECT.md, docs/KONTRAK.md, dan docs/agents/MF.md. Cek branch dan perubahan lokal. Ringkas batas tugas, dependensi, dan kondisi selesai, lalu implementasikan hanya UC-01. Jalankan verifikasi yang relevan. Laporkan file yang berubah, hasil cek, dan blocker yang masih ada.

Untuk sesi pertama, cek apakah agent menyebutkan fitur, pemilik, branch, dan aturan penerimaan yang sesuai dengan kartunya. Buka sesi baru setelah instruksi repo diperbarui.

## 3. Jalankan aplikasi di komputer masing-masing

Instal Git, JDK 21, dan MySQL 8.4 atau Docker Desktop sesuai [README](README.md#mulai-di-windows-powershell). Dari root repo di PowerShell, cek versi Java dan buat konfigurasi lokal.

```powershell
java -version
if (-not (Test-Path -LiteralPath '.env')) {
    Copy-Item -LiteralPath '.env.example' -Destination '.env'
}
```

Edit `.env` sesuai database lokal. Gunakan nama database `apotek` dan isi `DB_HOST`, `DB_PORT`, `DB_USERNAME`, dan `DB_PASSWORD` berdasarkan konfigurasi masing-masing.

- **Docker Desktop**. Jalankan `docker compose up -d --wait` dari root repo.
- **MySQL lokal**. Buat database `apotek` dan pastikan akun di `.env` punya akses ke database itu.

Jalankan aplikasi di PowerShell.

```powershell
.\mvnw.cmd spring-boot:run
```

Buka [localhost:8080](http://localhost:8080) untuk melihat halaman awal. Untuk verifikasi build sebelum membuka PR, jalankan `.\mvnw.cmd --batch-mode verify`. Login dan data demo untuk fitur klinik mengikuti hasil pekerjaan SETUP-00.

Untuk macOS/Linux, gunakan langkah pada [README](README.md) dan Maven Wrapper `./mvnw`.

## 4. Laporkan progres dari issue sampai PR

Gunakan satu status per issue pada GitHub Projects atau label issue. Mulai dari **Todo**, lalu **In Progress**, **Review**, dan **Done** setelah PR masuk dan alur terkait dicoba. Pakai **Blocked** jika perlu, dengan nomor issue dependensi dan langkah yang masih bisa dikerjakan. Isi deskripsi issue/PR dengan UC, batas perubahan, bukti, dan cara menjalankan cek.

```text
UC / issue:
Status dan branch:
Perubahan selesai:
Verifikasi dan hasil:
Dependensi atau blocker:
Langkah berikutnya:
```

Setelah verifikasi, stage file yang relevan, commit, dan jalankan `git push -u origin HEAD`. Buka PR ke branch utama, cantumkan `Closes #N` dengan nomor issue sendiri, dan minta review anggota yang memakai kontraknya. Sinkronkan branch sesuai kebiasaan tim bila branch utama bergerak, lalu ulangi cek. Gunakan [skenario demo](docs/PROJECT.md#cek-demo) untuk penutupan bersama.

Rujukan resmi untuk [instruksi proyek Codex](https://learn.chatgpt.com/docs/agent-configuration/agents-md), [undangan kolaborator](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/inviting-collaborators-to-a-personal-repository), dan [GitHub flow](https://docs.github.com/en/get-started/using-github/github-flow).
