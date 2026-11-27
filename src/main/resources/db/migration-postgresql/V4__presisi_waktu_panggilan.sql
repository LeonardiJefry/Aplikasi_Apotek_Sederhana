-- V3 sudah dipakai fixture review; perbaikan presisi diterapkan sebagai migrasi maju.
-- TIMESTAMP(0) tanpa presisi membulatkan panggilan pecahan detik ke detik berikutnya.
ALTER TABLE PanggilanAntrean ALTER COLUMN waktuPanggil TYPE TIMESTAMP(6);
ALTER TABLE PanggilanAntrean ALTER COLUMN waktuTutup TYPE TIMESTAMP(6);
