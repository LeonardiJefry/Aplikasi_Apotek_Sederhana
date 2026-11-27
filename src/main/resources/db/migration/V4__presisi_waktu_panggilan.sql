-- V3 sudah dipakai fixture review; perbaikan presisi diterapkan sebagai migrasi maju.
-- DATETIME tanpa presisi membulatkan panggilan pecahan detik ke detik berikutnya.
ALTER TABLE PanggilanAntrean MODIFY COLUMN waktuPanggil DATETIME(6) NOT NULL;
ALTER TABLE PanggilanAntrean MODIFY COLUMN waktuTutup DATETIME(6) NULL;
