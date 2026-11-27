-- DB-01 PostgreSQL/Supabase: salinan V1 MySQL/H2 dengan tipe PostgreSQL. Migrasi MySQL historis tidak diubah.
-- Identifier tanpa tanda kutip disimpan huruf kecil oleh PostgreSQL; Hibernate/JDBC memakai nama yang sama tanpa kutip.
-- Modul 3 bagian 3.1: delapan tabel PDM. Nama fisik dipertahankan persis.
CREATE TABLE Pasien (
 idPasien VARCHAR(10) NOT NULL, nik CHAR(16) NOT NULL,
 noRekamMedis VARCHAR(20) NOT NULL, nama VARCHAR(100) NOT NULL, tanggalLahir DATE NOT NULL,
 CONSTRAINT pk_pasien PRIMARY KEY (idPasien),
 CONSTRAINT uq_pasien_nik UNIQUE (nik), CONSTRAINT uq_pasien_no_rm UNIQUE (noRekamMedis),
 CONSTRAINT chk_pasien_nik CHECK (nik ~ '^[0-9]{16}$')
);
CREATE TABLE Poli (
 idPoli VARCHAR(10) NOT NULL, namaPoli VARCHAR(50) NOT NULL,
 CONSTRAINT pk_poli PRIMARY KEY (idPoli), CONSTRAINT uq_poli_nama UNIQUE (namaPoli)
);
CREATE TABLE Dokter (
 idDokter VARCHAR(10) NOT NULL, nama VARCHAR(100) NOT NULL,
 spesialisasi VARCHAR(100) NOT NULL, idPoli VARCHAR(10) NOT NULL,
 CONSTRAINT pk_dokter PRIMARY KEY (idDokter),
 CONSTRAINT fk_dokter_poli FOREIGN KEY (idPoli) REFERENCES Poli (idPoli)
);
CREATE INDEX ix_dokter_poli ON Dokter (idPoli);
CREATE TABLE JadwalPraktik (
 idJadwal VARCHAR(10) NOT NULL, idDokter VARCHAR(10) NOT NULL, idPoli VARCHAR(10) NOT NULL,
 tanggal DATE NOT NULL, sesi VARCHAR(30) NOT NULL, jamMulai TIME NOT NULL, jamSelesai TIME NOT NULL,
 kuotaTotal INT NOT NULL, sisaKuota INT NOT NULL,
 CONSTRAINT pk_jadwal_praktik PRIMARY KEY (idJadwal),
 CONSTRAINT fk_jadwal_dokter FOREIGN KEY (idDokter) REFERENCES Dokter (idDokter),
 CONSTRAINT fk_jadwal_poli FOREIGN KEY (idPoli) REFERENCES Poli (idPoli),
 CONSTRAINT chk_jadwal_jam CHECK (jamSelesai > jamMulai),
 CONSTRAINT chk_jadwal_kuota_total CHECK (kuotaTotal >= 0),
 CONSTRAINT chk_jadwal_sisa_kuota CHECK (sisaKuota BETWEEN 0 AND kuotaTotal)
);
CREATE INDEX ix_jadwal_tanggal_poli ON JadwalPraktik (tanggal, idPoli);
CREATE INDEX ix_jadwal_dokter_tanggal ON JadwalPraktik (idDokter, tanggal);
CREATE TABLE Antrean (
 idAntrean VARCHAR(12) NOT NULL, idPasien VARCHAR(10) NOT NULL, idJadwal VARCHAR(10) NOT NULL,
 nomorAntrean INT NOT NULL, tglBooking TIMESTAMP(0) NOT NULL, statusAntrean VARCHAR(30) NOT NULL,
 waktuCheckIn TIMESTAMP(0) NULL, waktuSelesai TIMESTAMP(0) NULL,
 CONSTRAINT pk_antrean PRIMARY KEY (idAntrean),
 CONSTRAINT fk_antrean_pasien FOREIGN KEY (idPasien) REFERENCES Pasien (idPasien),
 CONSTRAINT fk_antrean_jadwal FOREIGN KEY (idJadwal) REFERENCES JadwalPraktik (idJadwal),
 CONSTRAINT uq_antrean_nomor_per_jadwal UNIQUE (idJadwal, nomorAntrean),
 CONSTRAINT chk_antrean_nomor CHECK (nomorAntrean > 0),
 CONSTRAINT chk_antrean_status CHECK (statusAntrean IN
 ('Terdaftar','Menunggu','Hadir/Menunggu','Sedang Dilayani','Dilewati','Tidak Hadir','Selesai','Dibatalkan'))
);
CREATE INDEX ix_antrean_pasien_status ON Antrean (idPasien, statusAntrean);
CREATE INDEX ix_antrean_jadwal_status_nomor ON Antrean (idJadwal, statusAntrean, nomorAntrean);
CREATE TABLE JadwalVaksinasi (
 idJadwalVaksinasi VARCHAR(10) NOT NULL, tanggal DATE NOT NULL, jenisVaksin VARCHAR(50) NOT NULL,
 kuota INT NOT NULL, jumlahTerdaftar INT NOT NULL, pendaftaranDibuka BOOLEAN NOT NULL,
 CONSTRAINT pk_jadwal_vaksinasi PRIMARY KEY (idJadwalVaksinasi),
 CONSTRAINT chk_vaksinasi_kuota CHECK (kuota >= 0),
 CONSTRAINT chk_vaksinasi_terdaftar CHECK (jumlahTerdaftar BETWEEN 0 AND kuota)
);
CREATE INDEX ix_vaksinasi_tanggal_jenis ON JadwalVaksinasi (tanggal, jenisVaksin);
CREATE TABLE Notifikasi (
 idNotifikasi VARCHAR(10) NOT NULL, idAntrean VARCHAR(12) NOT NULL,
 statusKirim VARCHAR(20) NOT NULL, waktuKirim TIMESTAMP(0) NULL,
 CONSTRAINT pk_notifikasi PRIMARY KEY (idNotifikasi),
 CONSTRAINT fk_notifikasi_antrean FOREIGN KEY (idAntrean) REFERENCES Antrean (idAntrean),
 CONSTRAINT chk_notifikasi_status CHECK (statusKirim IN ('Terkirim','Gagal','Tertunda'))
);
CREATE INDEX ix_notifikasi_antrean ON Notifikasi (idAntrean);
CREATE TABLE RiwayatPelewatan (
 idRiwayatPelewatan VARCHAR(12) NOT NULL, idAntrean VARCHAR(12) NOT NULL,
 waktuPelewatan TIMESTAMP(0) NOT NULL, urutanPelewatan SMALLINT NOT NULL, hasilPelewatan VARCHAR(20) NOT NULL,
 CONSTRAINT pk_riwayat_pelewatan PRIMARY KEY (idRiwayatPelewatan),
 CONSTRAINT fk_riwayat_pelewatan_antrean FOREIGN KEY (idAntrean) REFERENCES Antrean (idAntrean),
 CONSTRAINT uq_riwayat_urutan UNIQUE (idAntrean, urutanPelewatan),
 CONSTRAINT chk_riwayat_urutan CHECK (urutanPelewatan >= 1),
 CONSTRAINT chk_riwayat_hasil CHECK (hasilPelewatan IN ('Dilewati','Tidak Hadir'))
);
CREATE INDEX ix_riwayat_antrean ON RiwayatPelewatan (idAntrean);
