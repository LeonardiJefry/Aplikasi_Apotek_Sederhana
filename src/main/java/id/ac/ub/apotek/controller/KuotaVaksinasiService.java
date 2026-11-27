package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.entity.JadwalVaksinasi;
import id.ac.ub.apotek.repository.JadwalVaksinasiRepository;
import id.ac.ub.apotek.security.CurrentUserService;
import java.time.*;
import java.time.format.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PC-07: kunci jadwal sebelum membaca peserta dan memperbarui kuota/status. */
@Service
@Transactional(readOnly=true)
public class KuotaVaksinasiService {
    private final JadwalVaksinasiRepository jadwal;
    private final CurrentUserService current;
    public KuotaVaksinasiService(JadwalVaksinasiRepository jadwal, CurrentUserService current) {
        this.jadwal=jadwal; this.current=current;
    }
    public record Layanan(String idJadwalVaksinasi,String tanggal,String jenisVaksin,int kuota,
            int jumlahTerdaftar,int sisaKuota,boolean pendaftaranDibuka,String status,String versi) {}
    public List<String> jenisVaksin() { return jadwal.jenisVaksin(); }
    // Snapshot publik hanya angka agregat dan identitas layanan, tidak memuat data pasien.
    public List<Layanan> cari(String tanggal,String jenis) {
        return jadwal.findByTanggalAndJenisVaksinOrderByIdJadwalVaksinasi(tanggal(tanggal),jenis(jenis))
                .stream().map(this::dto).toList();
    }
    public Layanan pilih(String tanggal,String jenis,String id) {
        admin(); var rows=cari(tanggal,jenis);
        if(rows.isEmpty()) throw new NoSuchElementException("Jadwal vaksinasi tidak ditemukan.");
        if(id==null || id.isBlank()) {
            if(rows.size()!=1) throw new IllegalStateException("Ada beberapa jadwal. Pilih ID layanan yang akan diubah.");
            return rows.getFirst();
        }
        validasiId(id);
        return rows.stream().filter(j->j.idJadwalVaksinasi().equals(id)).findFirst()
                .orElseThrow(()->new NoSuchElementException("Jadwal tidak sesuai tanggal dan jenis vaksin."));
    }
    @Transactional
    public Layanan simpan(String id,String tanggal,String jenis,String kuotaBaru,String versi) {
        admin(); validasiId(id); var date=tanggal(tanggal); var vaccine=jenis(jenis); int quota=kuota(kuotaBaru);
        var selected=jadwal.findForUpdate(id).orElseThrow(()->new NoSuchElementException("Jadwal vaksinasi tidak ditemukan."));
        if(!selected.getTanggal().equals(date) || !selected.getJenisVaksin().equals(vaccine))
            throw new IllegalStateException("Pilihan layanan berubah. Muat ulang sebelum menyimpan.");
        var before=dto(selected);
        if(versi==null || !before.versi().equals(versi))
            throw new IllegalStateException("Data layanan telah berubah. Muat ulang dan periksa kuota terbaru.");
        // Jumlah peserta dibaca dari baris terkunci, tidak menerima jumlah/sisa/status dari form.
        if(quota<selected.getJumlahTerdaftar())
            throw new IllegalStateException("Kuota lebih kecil dari jumlah peserta terdaftar ("+selected.getJumlahTerdaftar()+").");
        selected.setKuota(quota);
        selected.setPendaftaranDibuka(quota>selected.getJumlahTerdaftar());
        jadwal.flush(); // Kegagalan UPDATE membatalkan kuota dan status sekaligus.
        return dto(selected);
    }
    private Layanan dto(JadwalVaksinasi j) {
        int left=j.getKuota()-j.getJumlahTerdaftar(); boolean open=left>0 && j.isPendaftaranDibuka();
        String version=j.getKuota()+":"+j.getJumlahTerdaftar()+":"+j.isPendaftaranDibuka();
        return new Layanan(j.getIdJadwalVaksinasi(),j.getTanggal().toString(),j.getJenisVaksin(),j.getKuota(),
                j.getJumlahTerdaftar(),left,open,left==0?"Penuh":open?"Tersedia":"Ditutup",version);
    }
    private LocalDate tanggal(String value) {
        if(value==null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException("Tanggal layanan wajib berformat YYYY-MM-DD.");
        try { return LocalDate.parse(value,DateTimeFormatter.ISO_LOCAL_DATE); }
        catch(DateTimeParseException ex) { throw new IllegalArgumentException("Tanggal layanan tidak valid."); }
    }
    private String jenis(String value) {
        if(value==null || value.isBlank() || value.length()>50) throw new IllegalArgumentException("Jenis vaksin wajib diisi, maksimal 50 karakter.");
        return value.trim();
    }
    private int kuota(String value) {
        if(value==null || !value.matches("[0-9]{1,10}")) throw new IllegalArgumentException("Kuota harus bilangan bulat minimal 0.");
        try { return Integer.parseInt(value); }
        catch(NumberFormatException ex) { throw new IllegalArgumentException("Kuota maksimal 2147483647."); }
    }
    private void validasiId(String id) {
        if(id==null || !id.matches("[A-Za-z0-9_-]{1,10}")) throw new IllegalArgumentException("ID layanan tidak valid.");
    }
    private void admin() {
        if(!"ADMIN".equals(current.requireUser().getPeran())) throw new AccessDeniedException("Akses admin diperlukan.");
    }
}
