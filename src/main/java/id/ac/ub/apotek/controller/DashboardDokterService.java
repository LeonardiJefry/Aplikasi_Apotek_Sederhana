package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.entity.Antrean;
import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.CurrentUserService;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * UC-09 / PC-09: dashboard antrean ruang periksa untuk dokter yang login pada hari ini.
 * Identitas dokter selalu dari sesi; tindakan panggil/hadir/lewati memakai PanggilanService UC-06.
 */
@Service
@Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
public class DashboardDokterService {
    private static final DateTimeFormatter JAM=DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TANGGAL=DateTimeFormatter.ofPattern("EEEE, d MMM yyyy",Locale.forLanguageTag("id-ID"));
    private final JadwalPraktikRepository jadwal;
    private final AntreanRepository antrean;
    private final DokterRepository dokter;
    private final PanggilanAntreanRepository metadataPanggilan;
    private final NotifikasiRepository notifikasi;
    private final PanggilanService panggilan;
    private final CurrentUserService current;
    private final Clock clock;
    public DashboardDokterService(JadwalPraktikRepository jadwal, AntreanRepository antrean, DokterRepository dokter,
            PanggilanAntreanRepository metadataPanggilan, NotifikasiRepository notifikasi, PanggilanService panggilan,
            CurrentUserService current, Clock clock) {
        this.jadwal=jadwal; this.antrean=antrean; this.dokter=dokter; this.metadataPanggilan=metadataPanggilan; this.notifikasi=notifikasi;
        this.panggilan=panggilan; this.current=current; this.clock=clock;
    }

    /** Baris antrean untuk dokter pemilik jadwal; tanpa NIK, tanggal lahir atau data akun. */
    public record Baris(String idAntrean, int nomorAntrean, String nama, String noRekamMedis,
                        String statusAntrean, String waktuCheckIn, String waktuSelesai) {
        public String nomorTampilan() { return DashboardDokterService.nomor(nomorAntrean); }
    }
    /** Hitungan PC-09: sisa antrean = persis status Menunggu; status lain dihitung terpisah. */
    public record Sesi(JadwalDto jadwal, List<Baris> antrean, int menunggu, int hadirMenunggu,
                       int sedangDilayani, int selesai, int dilewati, int terdaftar,
                       boolean berlangsung, boolean dipilih) {}
    /** Pasien Sedang Dilayani pada sesi terpilih beserta waktu panggil dan lama layanan dari metadata UC-06. */
    public record Pelayanan(Baris pasien, String dipanggil, Long durasiMenit) {}
    /** UC-10: pasien terakhir Selesai pada sesi terpilih beserta status notifikasi loket (null = belum tercatat). */
    public record Terakhir(Baris pasien, Long durasiMenit, String statusNotifikasi) {}
    public record Snapshot(String dokter, String spesialisasi, String inisial, LocalDate tanggal, String tanggalTampilan,
                           List<Sesi> sesi, int totalMenunggu, int totalHadirMenunggu, Sesi aktif, Pelayanan pelayanan,
                           Terakhir terakhir, PanggilanService.Snapshot panggilan, String sekarang, String versi) {}

    public Snapshot snapshot(String idJadwal) {
        var idDokter=current.requireDokterId();
        if(idJadwal!=null && (idJadwal.isBlank() || idJadwal.length()>10)) throw new IllegalArgumentException("ID jadwal tidak valid.");
        var profil=dokter.findById(idDokter).orElseThrow(()->new NoSuchElementException("Data dokter tidak ditemukan."));
        var now=LocalDateTime.now(clock); var today=now.toLocalDate();
        var jadwalHariIni=jadwal.cariJadwal(today,null,idDokter);
        // Hanya jadwal milik dokter pada hari ini yang dapat dipilih; jadwal dokter lain/hari lain sama-sama 404.
        if(idJadwal!=null && jadwalHariIni.stream().noneMatch(j->j.getIdJadwal().equals(idJadwal)))
            throw new NoSuchElementException("Jadwal tidak ditemukan pada dashboard Anda hari ini.");
        String dipilih=idJadwal!=null?idJadwal:jadwalHariIni.stream()
                .filter(j->!now.toLocalTime().isAfter(j.getJamSelesai())).findFirst()
                .or(()->jadwalHariIni.stream().findFirst()).map(j->j.getIdJadwal()).orElse(null);

        var sesi=new ArrayList<Sesi>(); var versi=new StringBuilder(); Sesi aktif=null; Pelayanan pelayanan=null; Terakhir terakhir=null;
        for(var j:jadwalHariIni) {
            List<Antrean> daftar=antrean.daftarSesiLoket(j.getIdJadwal());
            var baris=daftar.stream().map(this::baris).toList();
            var berlangsung=j.getTanggal().equals(today) && !now.toLocalTime().isBefore(j.getJamMulai()) && !now.toLocalTime().isAfter(j.getJamSelesai());
            var item=new Sesi(JadwalDto.from(j),baris,hitung(daftar,"Menunggu"),hitung(daftar,"Hadir/Menunggu"),
                    hitung(daftar,"Sedang Dilayani"),hitung(daftar,"Selesai"),hitung(daftar,"Dilewati"),
                    j.getKuotaTotal()-j.getSisaKuota(),berlangsung,j.getIdJadwal().equals(dipilih));
            sesi.add(item);
            if(item.dipilih()) { aktif=item; pelayanan=pelayanan(j.getIdJadwal(),daftar,now); terakhir=terakhir(j.getIdJadwal(),daftar); }
            versi.append(j.getIdJadwal()).append(j.getSisaKuota())
                 .append(daftar.stream().map(a->a.getIdAntrean()+a.getStatusAntrean()+a.getWaktuCheckIn()+a.getWaktuSelesai()).toList());
        }
        var call=dipilih==null?null:panggilan.snapshot(dipilih);
        if(call!=null) versi.append(call.versi());
        if(terakhir!=null) versi.append(terakhir.statusNotifikasi());
        return new Snapshot(profil.getNama(),profil.getSpesialisasi(),inisial(profil.getNama()),today,today.format(TANGGAL),
                List.copyOf(sesi),sesi.stream().mapToInt(Sesi::menunggu).sum(),sesi.stream().mapToInt(Sesi::hadirMenunggu).sum(),
                aktif,pelayanan,terakhir,call,LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                Integer.toHexString(versi.toString().hashCode()));
    }

    /** Format nomor sesuai rancangan Hi-Fi dan Modul 3 (A-%03d). */
    public static String nomor(int nomorAntrean) { return String.format(Locale.ROOT,"A-%03d",nomorAntrean); }

    private Pelayanan pelayanan(String idJadwal,List<Antrean> daftar,LocalDateTime now) {
        var dilayani=daftar.stream().filter(a->"Sedang Dilayani".equals(a.getStatusAntrean())).findFirst().orElse(null);
        if(dilayani==null) return null;
        var call=metadataPanggilan.findById(idJadwal).orElse(null);
        // Metadata UC-06 hanya menyimpan panggilan terakhir; dipakai bila panggilan itu ditutup Hadir untuk pasien ini.
        if(call==null || !"Hadir".equals(call.getHasilPanggilan()) || call.getWaktuTutup()==null
                || !call.getAntrean().getIdAntrean().equals(dilayani.getIdAntrean())) return new Pelayanan(baris(dilayani),null,null);
        long menit=Math.max(0,Duration.between(call.getWaktuTutup(),now).toMinutes());
        return new Pelayanan(baris(dilayani),call.getWaktuPanggil().format(JAM),menit);
    }
    private Terakhir terakhir(String idJadwal,List<Antrean> daftar) {
        var selesai=daftar.stream().filter(a->"Selesai".equals(a.getStatusAntrean()) && a.getWaktuSelesai()!=null)
                .max(Comparator.comparing(Antrean::getWaktuSelesai).thenComparingInt(Antrean::getNomorAntrean)).orElse(null);
        if(selesai==null) return null;
        var call=metadataPanggilan.findById(idJadwal).orElse(null);
        Long durasi=call!=null && "Hadir".equals(call.getHasilPanggilan()) && call.getWaktuTutup()!=null
                && call.getAntrean().getIdAntrean().equals(selesai.getIdAntrean())
                ? Math.max(0,Duration.between(call.getWaktuTutup(),selesai.getWaktuSelesai()).toMinutes()) : null;
        var status=notifikasi.findFirstByAntrean_IdAntreanOrderByIdNotifikasi(selesai.getIdAntrean()).map(n->n.getStatusKirim()).orElse(null);
        return new Terakhir(baris(selesai),durasi,status);
    }
    private int hitung(List<Antrean> daftar,String status) { return (int)daftar.stream().filter(a->status.equals(a.getStatusAntrean())).count(); }
    private Baris baris(Antrean a) {
        return new Baris(a.getIdAntrean(),a.getNomorAntrean(),a.getPasien().getNama(),a.getPasien().getNoRekamMedis(),a.getStatusAntrean(),
                a.getWaktuCheckIn()==null?null:a.getWaktuCheckIn().format(JAM),a.getWaktuSelesai()==null?null:a.getWaktuSelesai().format(JAM));
    }
    private String inisial(String nama) {
        return Arrays.stream(nama.replaceFirst("(?i)^drg?\\.\\s*","").split("\\s+")).filter(s->!s.isBlank()).limit(2)
                .map(s->s.substring(0,1)).reduce("",String::concat).toUpperCase(Locale.ROOT);
    }
}
