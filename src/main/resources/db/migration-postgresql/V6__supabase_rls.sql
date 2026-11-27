-- DB-01: tabel klinik berada di skema non-publik dan Row Level Security aktif tanpa policy.
-- Supabase Data API (anon/authenticated) tidak dapat membaca data pasien; aplikasi Spring memakai
-- koneksi JDBC pemilik tabel yang tidak terpengaruh RLS. Tidak mengubah kolom, status atau data.
ALTER TABLE Pasien ENABLE ROW LEVEL SECURITY;
ALTER TABLE Poli ENABLE ROW LEVEL SECURITY;
ALTER TABLE Dokter ENABLE ROW LEVEL SECURITY;
ALTER TABLE JadwalPraktik ENABLE ROW LEVEL SECURITY;
ALTER TABLE Antrean ENABLE ROW LEVEL SECURITY;
ALTER TABLE JadwalVaksinasi ENABLE ROW LEVEL SECURITY;
ALTER TABLE Notifikasi ENABLE ROW LEVEL SECURITY;
ALTER TABLE RiwayatPelewatan ENABLE ROW LEVEL SECURITY;
ALTER TABLE Pengguna ENABLE ROW LEVEL SECURITY;
ALTER TABLE DemoDataset ENABLE ROW LEVEL SECURITY;
ALTER TABLE PanggilanAntrean ENABLE ROW LEVEL SECURITY;
ALTER TABLE VerifikasiPasien ENABLE ROW LEVEL SECURITY;
