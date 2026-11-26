# Setup kolaborasi Klinik Leonardo

Paket ini berisi panduan repo dan pembagian untuk lima anggota. Tempatkan `AGENTS.md`, `SETUP.md`, dan folder `docs/` di root repo. Jika nama file sudah dipakai, gabungkan panduan ke file yang ada dan pertahankan path yang dirujuk.

## 1. Siapkan repo sekali

Setiap anggota memerlukan Git, akun GitHub, dan AI agent yang bisa membaca folder proyek. Gunakan Git Credential Manager atau metode login GitHub yang sudah dipakai masing-masing.

**Sudah ada repo.** Buka root repo, jalankan `git remote -v`, buat branch `docs/setup-agent` dari branch utama yang sudah diperbarui, lalu salin isi paket. Commit `git add AGENTS.md SETUP.md docs` dan `git commit -m "docs: tambah panduan agent Klinik Leonardo"`. Push dengan `git push -u origin docs/setup-agent`, lalu buat PR ke branch utama.

**Repo baru.** Buat repo privat `klinik-leonardo` di GitHub tanpa inisialisasi. Buka PowerShell atau terminal di folder hasil ekstrak paket, lalu jalankan perintah ini. Ganti `USERNAME` dengan pemilik repo.

```bash
git init -b main
git add AGENTS.md SETUP.md docs
git commit -m "docs: siapkan kolaborasi Klinik Leonardo"
git remote add origin https://github.com/USERNAME/klinik-leonardo.git
git push -u origin main
```

Pemilik repo mengundang empat anggota dari **Settings → Collaborators → Add people** untuk repo pribadi. Setiap anggota menerima undangan sebelum clone. Buat satu issue untuk fondasi dengan judul `[SETUP-00] Fondasi aplikasi, login, MySQL dan data demo`. Buat 10 issue fitur dengan judul dari [daftar pekerjaan](docs/PROJECT.md#pembagian-dan-urutan-kerja), tetapkan pemilik, dan isi tautan kartu serta dependensinya. Nomor issue GitHub berbeda dari ID use case.

## 2. Gabung dari komputer masing-masing

Jalankan perintah ini sekali. Buka folder `klinik-leonardo` sebagai workspace di IDE atau agent.

```bash
git clone https://github.com/USERNAME/klinik-leonardo.git
cd klinik-leonardo
git status --short
```

Sebelum mulai satu issue baru, pastikan perubahan lokal sudah disimpan, lalu pakai contoh berikut. Ganti `main` jika branch utama bernama lain. Ganti branch di baris terakhir sesuai kartu tugas. Untuk melanjutkan branch yang sudah dibuat, gunakan `git switch feat/uc-01-jadwal`.

```bash
git switch main
git pull --ff-only
git switch -c feat/uc-01-jadwal
```

### Konfigurasi AI agent

| Alat | Cara pakai |
| --- | --- |
| Codex | Mulai sesi baru dari root repo. Codex membaca `AGENTS.md` di konteks proyek. Tempel prompt di bawah agar Codex tahu kartu dan issue yang harus dikerjakan. |
| Cursor, Copilot, Claude Code, atau agent lain | Buka root repo lalu gunakan prompt yang sama. Bila chat mendukung lampiran atau referensi file, masukkan `AGENTS.md`, kartu sendiri, dan `docs/KONTRAK.md`. |
| Agent yang hanya menerima teks | Tempel isi file panduan bersama, kartu sendiri, kontrak, dan deskripsi issue. Untuk mengedit langsung, agent perlu akses ke folder clone. |

**Prompt awal yang bisa disalin.** Ganti inisial, UC, dan nomor issue.

> Saya MF, mengerjakan UC-01 pada issue #2 di repo Klinik Leonardo. Baca AGENTS.md, docs/PROJECT.md, docs/KONTRAK.md, dan docs/agents/MF.md. Cek branch dan perubahan lokal. Ringkas batas tugas, dependensi, dan kondisi selesai, lalu implementasikan hanya UC-01. Jalankan verifikasi yang relevan. Laporkan file yang berubah, hasil cek, dan blocker yang masih ada.

Untuk sesi pertama, cek apakah agent menyebutkan fitur, pemilik, branch, dan aturan penerimaan yang sesuai dengan kartunya. Buka sesi baru setelah instruksi repo diperbarui.

## 3. Jalankan web setelah SETUP-00 masuk

Paket ini berisi Markdown. Perintah build berlaku setelah `pom.xml`, Maven Wrapper, konfigurasi, dan migrasi tersedia di branch utama. Instal JDK sesuai `pom.xml` (usulan repo baru Java 21) dan MySQL 8. Masing-masing anggota memakai database lokal. Buat schema `klinik_leonardo` dan akun lokal yang memiliki akses ke schema itu. SETUP-00 mendokumentasikan seed, akun demo, dan cara pembuatan schema.

Contoh dari root proyek di PowerShell. Isi `KATA_SANDI_LOKAL` di terminal pribadi.

```powershell
java -version
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/klinik_leonardo'
$env:SPRING_DATASOURCE_USERNAME = 'klinik_dev'
$env:SPRING_DATASOURCE_PASSWORD = 'KATA_SANDI_LOKAL'
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Untuk macOS/Linux, gunakan `export NAMA_VARIABEL='nilai'` untuk empat variabel yang sama. Jalankan `./mvnw test` dan `./mvnw spring-boot:run`. Buka `http://localhost:8080` dan masuk dengan akun demo yang ditulis pada PR fondasi.

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
