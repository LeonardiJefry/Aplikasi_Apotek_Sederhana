-- Tambahan SETUP-00 untuk autentikasi; delapan tabel PDM tetap utuh.
CREATE TABLE Pengguna (
 idPengguna VARCHAR(10) NOT NULL, username VARCHAR(50) NOT NULL,
 passwordHash VARCHAR(100) NOT NULL, peran VARCHAR(20) NOT NULL,
 idPasien VARCHAR(10) NULL, idDokter VARCHAR(10) NULL, aktif BOOLEAN NOT NULL,
 CONSTRAINT pk_pengguna PRIMARY KEY (idPengguna), CONSTRAINT uq_pengguna_username UNIQUE (username),
 CONSTRAINT uq_pengguna_pasien UNIQUE (idPasien), CONSTRAINT uq_pengguna_dokter UNIQUE (idDokter),
 CONSTRAINT fk_pengguna_pasien FOREIGN KEY (idPasien) REFERENCES Pasien (idPasien),
 CONSTRAINT fk_pengguna_dokter FOREIGN KEY (idDokter) REFERENCES Dokter (idDokter),
 CONSTRAINT chk_pengguna_peran CHECK (peran IN ('PASIEN','PETUGAS_LOKET','ADMIN','DOKTER')),
 CONSTRAINT chk_pengguna_identitas CHECK (
 (peran = 'PASIEN' AND idPasien IS NOT NULL AND idDokter IS NULL) OR
 (peran = 'DOKTER' AND idDokter IS NOT NULL AND idPasien IS NULL) OR
 (peran IN ('PETUGAS_LOKET','ADMIN') AND idPasien IS NULL AND idDokter IS NULL))
);
CREATE TABLE DemoDataset (
 id VARCHAR(30) NOT NULL PRIMARY KEY, seeded BOOLEAN NOT NULL
);
INSERT INTO DemoDataset (id, seeded) VALUES ('demo-v1', false);
