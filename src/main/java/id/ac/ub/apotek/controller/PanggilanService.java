package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.entity.*;
import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Satu layanan panggil/lewati/hadir untuk loket dan dashboard dokter UC-09/10. */
@Service
@Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
public class PanggilanService {
    private final JadwalPraktikRepository jadwal;
    private final AntreanRepository antrean;
    private final RiwayatPelewatanRepository riwayat;
    private final PanggilanAntreanRepository panggilan;
    private final CurrentUserService current;
    private final Clock clock;
    public PanggilanService(JadwalPraktikRepository jadwal, AntreanRepository antrean,
            RiwayatPelewatanRepository riwayat, PanggilanAntreanRepository panggilan,
            CurrentUserService current, Clock clock) {
        this.jadwal=jadwal; this.antrean=antrean; this.riwayat=riwayat;
        this.panggilan=panggilan; this.current=current; this.clock=clock;
    }
    public record Kartu(String idAntrean,int nomorAntrean,String nama,String noRekamMedis,
                        String statusAntrean,String waktuCheckIn,int jumlahPelewatan,String terakhirDilewati) {
        public String nomorTampilan() { return String.format(Locale.ROOT,"%03d",nomorAntrean); }
    }
    public record Snapshot(List<JadwalDto> pilihan,JadwalDto jadwal,Kartu dipanggil,Kartu berikutnya,
            List<Kartu> riwayat,String sekarang,boolean jamLayanan,boolean sedangDilayani,
            boolean bolehPanggil,boolean bolehLewati,String idPanggilan,String waktuPanggil,
            String batasTunggu,long sisaDetik,String versi) {}

    public Snapshot snapshot(String id) {
        var actor=aktor(); var now=LocalDateTime.now(clock);
        var pilihan=jadwal.cariJadwal(now.toLocalDate(),null,
                "DOKTER".equals(actor.getPeran())?actor.getIdDokter():null).stream().map(JadwalDto::from).toList();
        if(id==null && !pilihan.isEmpty()) id=pilihan.getFirst().idJadwal();
        if(id==null) return new Snapshot(pilihan,null,null,null,List.of(),now.toString(),false,false,false,false,null,null,null,0,"kosong");
        validasiId(id); var sesi=jadwal.findOneByIdJadwal(id).orElseThrow(this::tidakAda); bolehAkses(actor,sesi);
        var daftar=antrean.daftarSesiLoket(id); var history=riwayat.riwayatSesi(id);
        var counts=new HashMap<String,Integer>(); var times=new HashMap<String,String>();
        for(var r:history) {
            var key=r.getAntrean().getIdAntrean(); counts.merge(key,(int)r.getUrutanPelewatan(),Math::max);
            times.putIfAbsent(key,r.getWaktuPelewatan().toString());
        }
        var call=panggilan.findById(id).orElse(null);
        var active=call!=null && call.menunggu() && admissible(call.getAntrean())?call:null;
        now=LocalDateTime.now(clock); // Snapshot waktu setelah data dibaca, tidak mendahului panggilan yang baru tersimpan.
        boolean serving=daftar.stream().anyMatch(a->"Sedang Dilayani".equals(a.getStatusAntrean()));
        boolean open=jamLayanan(sesi,now);
        var next=kandidat(daftar,call,active==null?null:active.getAntrean().getIdAntrean());
        var rows=daftar.stream().filter(a->counts.containsKey(a.getIdAntrean()))
                .map(a->kartu(a,counts.get(a.getIdAntrean()),times.get(a.getIdAntrean()))).toList();
        var version=(call==null?"-":call.getIdPanggilan()+call.getHasilPanggilan())+":"+
                daftar.stream().map(a->a.getIdAntrean()+a.getStatusAntrean()+a.getWaktuCheckIn()).toList()+":"+counts+":"+sesi.getSisaKuota();
        var bound=active==null?null:active.getWaktuPanggil().plusSeconds(120);
        var until=bound==null?Duration.ZERO:Duration.between(now,bound);
        long remaining=until.isNegative()?0:until.getSeconds()+(until.getNano()>0?1:0);
        return new Snapshot(pilihan,JadwalDto.from(sesi),active==null?null:kartu(active.getAntrean(),counts.getOrDefault(active.getAntrean().getIdAntrean(),0),times.get(active.getAntrean().getIdAntrean())),
                next==null?null:kartu(next,counts.getOrDefault(next.getIdAntrean(),0),times.get(next.getIdAntrean())),rows,now.toString(),open,serving,
                open && active==null && !serving && next!=null,open && active!=null && !serving && !now.isBefore(bound),
                active==null?null:active.getIdPanggilan(),active==null?null:active.getWaktuPanggil().toString(),bound==null?null:bound.toString(),remaining,version);
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void panggil(String id) {
        var actor=aktor(); validasiId(id); var sesi=jadwal.findForUpdate(id).orElseThrow(this::tidakAda); bolehAkses(actor,sesi);
        var now=LocalDateTime.now(clock); requireJam(sesi,now);
        var call=panggilan.findById(id).orElse(null);
        if(call!=null && call.menunggu() && admissible(call.getAntrean())) throw new PanggilanException("Masih ada nomor yang sedang dipanggil.");
        var daftar=antrean.daftarSesiLoket(id); requireTidakDilayani(daftar);
        var next=kandidat(daftar,call,null);
        if(next==null) throw new PanggilanException("Tidak ada antrean berikutnya.");
        mulai(sesi,call,next,now,actor.getIdPengguna());
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void hadir(String id,String token) { proses(id,token,false,true); }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void lewati(String id,String token,boolean dikonfirmasi) { proses(id,token,true,dikonfirmasi); }

    private void proses(String id,String token,boolean skip,boolean confirmed) {
        var actor=aktor(); validasiId(id);
        if(!confirmed) throw new IllegalArgumentException("Konfirmasi pelewatan diperlukan.");
        if(token==null || !token.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("Token panggilan tidak valid.");
        // Semua mutasi mengambil kunci JadwalPraktik sebelum Antrean, seperti booking/check-in/batal.
        var sesi=jadwal.findForUpdate(id).orElseThrow(this::tidakAda); bolehAkses(actor,sesi);
        var call=panggilan.findById(id).orElseThrow(()->new PanggilanException("Nomor belum dipanggil."));
        if(!call.menunggu() || !call.getIdPanggilan().equals(token)) throw new PanggilanException("Panggilan telah berubah atau sudah diproses. Muat ulang antrean.");
        var tiket=antrean.findForUpdate(call.getAntrean().getIdAntrean()).orElseThrow(this::tidakAda);
        if(!id.equals(tiket.getJadwal().getIdJadwal()) || !admissible(tiket)) throw new PanggilanException("Status atau jadwal tiket sudah berubah.");
        var daftar=antrean.daftarSesiLoket(id); requireTidakDilayani(daftar);
        var now=LocalDateTime.now(clock); requireJam(sesi,now);
        if(now.isBefore(call.getWaktuPanggil())) throw new PanggilanException("Waktu server mendahului waktu panggilan. Muat ulang setelah waktu tersinkron.");
        if(skip) {
            if(now.isBefore(call.getWaktuPanggil().plusSeconds(120))) throw new PanggilanException("Tunggu 120 detik sejak nomor dipanggil sebelum melewati.");
            int count=riwayat.jumlahPelewatan(tiket.getIdAntrean())+1;
            if(count>3) throw new PanggilanException("Batas pelewatan tiket sudah tercapai.");
            String status=count==3?"Tidak Hadir":"Dilewati";
            if(count==3 && sesi.getSisaKuota()>=sesi.getKuotaTotal()) throw new PanggilanException("Data kuota tidak konsisten. Pelewatan dibatalkan.");
            tiket.setStatusAntrean(status);
            riwayat.saveAndFlush(new RiwayatPelewatan(UUID.randomUUID().toString().replace("-","").substring(0,12),tiket,now,(byte)count,status));
            call.tutup("Dilewati",now); antrean.flush();
            if(count==3) { sesi.setSisaKuota(sesi.getSisaKuota()+1); jadwal.flush(); }
            var next=kandidat(daftar,call,tiket.getIdAntrean());
            if(next!=null) mulai(sesi,call,next,now,actor.getIdPengguna());
            else panggilan.flush();
        } else {
            tiket.setStatusAntrean("Sedang Dilayani"); call.tutup("Hadir",now);
            antrean.flush(); panggilan.flush();
        }
    }

    private void mulai(JadwalPraktik sesi,PanggilanAntrean call,Antrean candidate,LocalDateTime now,String actor) {
        var next=antrean.findForUpdate(candidate.getIdAntrean()).orElseThrow(this::tidakAda);
        if(!sesi.getIdJadwal().equals(next.getJadwal().getIdJadwal()) || !admissible(next)) throw new PanggilanException("Antrean berikutnya berubah. Muat ulang.");
        if(call==null) call=new PanggilanAntrean(sesi.getIdJadwal());
        call.mulai(next,UUID.randomUUID().toString(),now,actor); panggilan.saveAndFlush(call);
    }
    private Antrean kandidat(List<Antrean> daftar,PanggilanAntrean call,String exclude) {
        // Setelah pasien berikutnya selesai, beri kesempatan pada tiket Dilewati lebih dahulu.
        boolean requeue=call!=null && "Hadir".equals(call.getHasilPanggilan()) && "Selesai".equals(call.getAntrean().getStatusAntrean());
        var candidates=daftar.stream().filter(this::admissible).filter(a->!a.getIdAntrean().equals(exclude));
        return candidates.min(Comparator.<Antrean>comparingInt(a->"Dilewati".equals(a.getStatusAntrean())?(requeue?0:1):(requeue?1:0))
                .thenComparingInt(Antrean::getNomorAntrean)).orElse(null);
    }
    private boolean admissible(Antrean a) { return Set.of("Terdaftar","Menunggu","Hadir/Menunggu","Dilewati").contains(a.getStatusAntrean()); }
    private KlinikPrincipal aktor() {
        var actor=current.requireUser();
        if(!Set.of("PETUGAS_LOKET","DOKTER").contains(actor.getPeran())) throw new AccessDeniedException("Akses panggilan tidak diizinkan.");
        return actor;
    }
    private void bolehAkses(KlinikPrincipal actor,JadwalPraktik sesi) {
        if("DOKTER".equals(actor.getPeran()) && !sesi.getDokter().getIdDokter().equals(actor.getIdDokter())) throw tidakAda();
    }
    private boolean jamLayanan(JadwalPraktik j,LocalDateTime now) {
        return j.getTanggal().equals(now.toLocalDate()) && !now.toLocalTime().isBefore(j.getJamMulai()) && !now.toLocalTime().isAfter(j.getJamSelesai());
    }
    private void requireJam(JadwalPraktik sesi,LocalDateTime now) { if(!jamLayanan(sesi,now)) throw new PanggilanException("Panggilan hanya selama jam pelayanan pada hari jadwal."); }
    private void requireTidakDilayani(List<Antrean> daftar) { if(daftar.stream().anyMatch(a->"Sedang Dilayani".equals(a.getStatusAntrean()))) throw new PanggilanException("Masih ada pasien Sedang Dilayani pada sesi ini."); }
    private Kartu kartu(Antrean a,int count,String time) { return new Kartu(a.getIdAntrean(),a.getNomorAntrean(),a.getPasien().getNama(),a.getPasien().getNoRekamMedis(),a.getStatusAntrean(),a.getWaktuCheckIn()==null?null:a.getWaktuCheckIn().toString(),count,time); }
    private void validasiId(String id) { if(id==null || id.isBlank() || id.length()>10) throw new IllegalArgumentException("ID jadwal tidak valid."); }
    private NoSuchElementException tidakAda() { return new NoSuchElementException("Jadwal atau tiket tidak ditemukan."); }
}
