-- UC-06: satu panggilan terakhir per sesi. Timer dan token tetap setelah reload/restart.
-- V1/V2 historis tidak diubah. PostgreSQL memakai lokasi migrasi tersendiri pada DB-01.
CREATE TABLE PanggilanAntrean (
 idJadwal VARCHAR(10) NOT NULL, idAntrean VARCHAR(12) NOT NULL,
 idPanggilan VARCHAR(36) NOT NULL, waktuPanggil DATETIME NOT NULL,
 hasilPanggilan VARCHAR(20) NOT NULL, waktuTutup DATETIME NULL,
 idPengguna VARCHAR(10) NOT NULL,
 CONSTRAINT pk_panggilan_antrean PRIMARY KEY (idJadwal),
 CONSTRAINT fk_panggilan_jadwal FOREIGN KEY (idJadwal) REFERENCES JadwalPraktik (idJadwal),
 CONSTRAINT fk_panggilan_antrean FOREIGN KEY (idAntrean) REFERENCES Antrean (idAntrean),
 CONSTRAINT fk_panggilan_pengguna FOREIGN KEY (idPengguna) REFERENCES Pengguna (idPengguna),
 CONSTRAINT uq_panggilan_token UNIQUE (idPanggilan),
 CONSTRAINT chk_panggilan_hasil CHECK (hasilPanggilan IN ('Menunggu','Hadir','Dilewati')),
 CONSTRAINT chk_panggilan_tutup CHECK (
   (hasilPanggilan = 'Menunggu' AND waktuTutup IS NULL) OR
   (hasilPanggilan IN ('Hadir','Dilewati') AND waktuTutup IS NOT NULL AND waktuTutup >= waktuPanggil))
) ${tableOptions};
