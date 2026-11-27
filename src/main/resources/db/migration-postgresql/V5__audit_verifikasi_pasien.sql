-- UC-08: audit terpisah dari identitas pasien dan status antrean; V1-V4 tetap.
CREATE TABLE VerifikasiPasien (
 idVerifikasi VARCHAR(36) NOT NULL, idPasien VARCHAR(10) NOT NULL,
 idPetugas VARCHAR(10) NOT NULL, waktuVerifikasi TIMESTAMP(6) NOT NULL,
 statusVerifikasi VARCHAR(20) NOT NULL, sidikIdentitas CHAR(64) NOT NULL,
 CONSTRAINT pk_verifikasi_pasien PRIMARY KEY (idVerifikasi),
 CONSTRAINT fk_verifikasi_pasien FOREIGN KEY (idPasien) REFERENCES Pasien (idPasien),
 CONSTRAINT fk_verifikasi_petugas FOREIGN KEY (idPetugas) REFERENCES Pengguna (idPengguna),
 CONSTRAINT chk_verifikasi_status CHECK (statusVerifikasi = 'Terverifikasi')
);
CREATE INDEX ix_verifikasi_pasien_waktu ON VerifikasiPasien (idPasien, waktuVerifikasi);
