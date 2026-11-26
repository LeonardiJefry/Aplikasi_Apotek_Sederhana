# Arsitektur BCE

Aplikasi berada dalam satu proyek Spring Boot. Halaman ditampilkan melalui Thymeleaf; CSS dan JavaScript disimpan bersama aplikasinya.

## Pembagian tanggung jawab

| Lapisan | Lokasi | Tanggung jawab |
| --- | --- | --- |
| Boundary | `boundary`, `templates`, `static` | HTTP, form, validasi input, dan tampilan |
| Controller BCE | `controller` | Urutan use case, aturan bisnis, dan transaksi |
| Entity | `entity` | Data dan relasi JPA sesuai model Modul 2 |
| Repository | `repository` | Operasi penyimpanan dan query database |

`@Controller` adalah anotasi Spring MVC untuk boundary HTTP. Istilah **controller BCE** merujuk pada pengendali use case, yang dapat memakai `@Service`. Pembagian ini perlu dipetakan bersama terhadap nama kelas pada desain tim agar tidak ada dua pemahaman berbeda.

## Contoh alur implementasi

Permintaan halaman diterima boundary, boundary memanggil controller BCE, controller memakai repository dan entity, lalu boundary mengembalikan template. Thymeleaf tidak memanggil repository secara langsung.

`HomeBoundary` mengarahkan pengguna ke beranda berdasarkan identitas sesi melalui `CurrentUserService`; aturan akses ditangani Spring Security. Layanan bisnis fitur tetap berada di lapisan controller BCE/service.

## Kesepakatan sebelum fitur pertama

1. Lampirkan rancangan Modul 2 atau tautannya pada Issue terkait.
2. Cocokkan nama entitas, relasi, status, dan aturan perubahan Antrean/Tiket dengan model data.
3. Tetapkan satu PIC untuk perubahan skema bersama.
4. Sertakan skrip perubahan skema sebelum menambah `@Entity`; konfigurasi `ddl-auto=validate` tidak membuat tabel otomatis.
5. Setelah endpoint dashboard dan kebutuhan pembaruan ditentukan, implementasikan polling JavaScript sesuai interval yang disepakati. Polling dan WebSocket belum ditambahkan pada fondasi ini.

Fondasi sudah memiliki sembilan entitas JPA bersama untuk tabel PDM dan Pengguna, serta repository dan layanan akses tiket. Flyway mengelola V1/V2; jangan menduplikasi model. H2 demo/test dipakai hingga QA-01, kemudian PostgreSQL Supabase pada DB-01.
