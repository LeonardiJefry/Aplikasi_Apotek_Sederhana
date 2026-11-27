package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.entity.*;
import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class VerifikasiPasienService {
    private final PasienRepository pasien;
    private final AntreanRepository antrean;
    private final VerifikasiPasienRepository audit;
    private final PenggunaRepository pengguna;
    private final CurrentUserService current;
    private final Clock clock;
    public VerifikasiPasienService(PasienRepository pasien,AntreanRepository antrean,VerifikasiPasienRepository audit,
            PenggunaRepository pengguna,CurrentUserService current,Clock clock) {
        this.pasien=pasien; this.antrean=antrean; this.audit=audit; this.pengguna=pengguna; this.current=current; this.clock=clock;
    }
    public record Hasil(String idPasien,String nama,String noRekamMedis,String nikTersamar) {}
    public record Identitas(String idPasien,String nama,String noRekamMedis,String nik,String nikTersamar,String tanggalLahir) {}
    public record Riwayat(String idAntrean,String tanggalLayanan,String layanan,String dokter,int nomorAntrean,String status,String waktuBooking,String waktuSelesai) {}
    public record Audit(String idVerifikasi,String idPetugas,String petugas,String waktu,String status) {}
    public record Detail(Identitas pasien,Page<Riwayat> pendaftaran,Page<Riwayat> kunjungan,List<Audit> verifikasi,
            String sidikIdentitas,boolean terverifikasi) {}
    public Page<Hasil> cari(String jenis,String kata,int halaman) {
        loket(); var page=page(halaman); var keyword=kata==null?"":kata.trim();
        if(keyword.isEmpty()) throw new IllegalArgumentException("Kata kunci wajib diisi.");
        return switch(jenis==null?"":jenis) {
            case "NIK" -> {
                if(!keyword.matches("[0-9]{16}")) throw new IllegalArgumentException("NIK harus terdiri dari 16 digit angka.");
                yield satu(pasien.findByNik(keyword),halaman);
            }
            case "NO_RM" -> {
                if(keyword.length()>20 || keyword.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Nomor rekam medis maksimal 20 karakter.");
                yield satu(pasien.findByNoRekamMedis(keyword),halaman);
            }
            case "NAMA" -> {
                if(keyword.length()<2 || keyword.length()>100 || keyword.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Nama harus 2 sampai 100 karakter.");
                yield pasien.findByNamaContainingIgnoreCaseOrderByNamaAscIdPasienAsc(keyword,page).map(this::hasil);
            }
            default -> throw new IllegalArgumentException("Jenis pencarian tidak valid.");
        };
    }
    public Detail detail(String id,int halamanPendaftaran,int halamanKunjungan) {
        loket(); validasiId(id); var p=pasien.findById(id).orElseThrow(this::tidakAda);
        var versions=audit.terbaru(id,PageRequest.of(0,10)); var sidik=sidik(p);
        return new Detail(new Identitas(id,p.getNama(),p.getNoRekamMedis(),p.getNik(),mask(p.getNik()),p.getTanggalLahir().toString()),
                antrean.riwayatPendaftaran(id,page(halamanPendaftaran)).map(this::riwayat),
                antrean.riwayatKunjungan(id,page(halamanKunjungan)).map(this::riwayat),versions.stream().map(this::auditDto).toList(),
                sidik,!versions.isEmpty() && versions.getFirst().getSidikIdentitas().equals(sidik));
    }
    @Transactional
    public Audit verifikasi(String id,String token,String sidik,boolean dikonfirmasi) {
        var actor=loket(); validasiId(id);
        if(!dikonfirmasi) throw new IllegalArgumentException("Konfirmasikan kecocokan identitas pasien terlebih dahulu.");
        if(token==null || !token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") || sidik==null || !sidik.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Formulir verifikasi tidak valid. Buka ulang pasien yang dipilih.");
        var p=pasien.findForUpdate(id).orElseThrow(this::tidakAda);
        var existing=audit.findById(token);
        if(existing.isPresent()) {
            var v=existing.get();
            if(!v.getPasien().getIdPasien().equals(id) || !v.getPetugas().getIdPengguna().equals(actor.getIdPengguna()) || !v.getSidikIdentitas().equals(sidik))
                throw new IllegalStateException("Formulir sudah dipakai untuk verifikasi lain.");
            return auditDto(v); // Replay tidak membuat audit atau menimpa waktu semula.
        }
        if(!sidik(p).equals(sidik)) throw new IllegalStateException("Identitas pasien berubah. Muat ulang dan cocokkan kembali sebelum verifikasi.");
        var petugas=pengguna.findById(actor.getIdPengguna()).orElseThrow(()->new AccessDeniedException("Petugas tidak tersedia."));
        if(!petugas.isAktif() || !"PETUGAS_LOKET".equals(petugas.getPeran())) throw new AccessDeniedException("Petugas tidak berwenang.");
        return auditDto(audit.saveAndFlush(new VerifikasiPasien(token,p,petugas,LocalDateTime.now(clock),sidik)));
    }
    private Page<Hasil> satu(Optional<Pasien> value,int halaman) {
        var results=halaman==0?value.stream().map(this::hasil).toList():List.<Hasil>of();
        return new PageImpl<>(results,page(halaman),value.isPresent()?1:0);
    }
    private Hasil hasil(Pasien p) { return new Hasil(p.getIdPasien(),p.getNama(),p.getNoRekamMedis(),mask(p.getNik())); }
    private Riwayat riwayat(Antrean a) { var j=a.getJadwal(); return new Riwayat(a.getIdAntrean(),j.getTanggal().toString(),j.getPoli().getNamaPoli(),j.getDokter().getNama(),a.getNomorAntrean(),a.getStatusAntrean(),a.getTglBooking().toString(),a.getWaktuSelesai()==null?null:a.getWaktuSelesai().toString()); }
    private Audit auditDto(VerifikasiPasien v) { return new Audit(v.getIdVerifikasi(),v.getPetugas().getIdPengguna(),v.getPetugas().getUsername(),v.getWaktuVerifikasi().toString(),v.getStatusVerifikasi()); }
    private String sidik(Pasien p) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(String.join(Character.toString(0),p.getIdPasien(),p.getNik(),p.getNoRekamMedis(),p.getNama(),p.getTanggalLahir().toString()).getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException ex) { throw new IllegalStateException("Algoritma sidik identitas tidak tersedia.",ex); }
    }
    private String mask(String nik) { return nik.substring(0,4)+" •••• •••• "+nik.substring(12); }
    private PageRequest page(int n) { if(n<0 || n>100000) throw new IllegalArgumentException("Halaman tidak valid."); return PageRequest.of(n,20); }
    private void validasiId(String id) { if(id==null || !id.matches("[A-Za-z0-9_-]{1,10}")) throw new IllegalArgumentException("ID pasien tidak valid."); }
    private NoSuchElementException tidakAda() { return new NoSuchElementException("Data pasien tidak ditemukan."); }
    private KlinikPrincipal loket() { var actor=current.requireUser(); if(!"PETUGAS_LOKET".equals(actor.getPeran())) throw new AccessDeniedException("Akses petugas loket diperlukan."); return actor; }
}
