package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.CurrentUserService;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class LoketCheckInService {
    /** Keputusan operasional UC-05: dua nomor aktif terdepan yang belum sedang dilayani. */
    private static final int DEKAT = 2;
    private final AntreanRepository antrean;
    private final JadwalPraktikRepository jadwal;
    private final CurrentUserService current;
    private final Clock clock;
    public LoketCheckInService(AntreanRepository antrean,JadwalPraktikRepository jadwal,CurrentUserService current,Clock clock) {
        this.antrean=antrean; this.jadwal=jadwal; this.current=current; this.clock=clock;
    }
    public record Peringatan(String idJadwal,int nomorAntrean,String poli,String dokter,String sesi,String tanggal) {
        public String nomorTampilan() { return String.format(Locale.ROOT,"%03d",nomorAntrean); }
    }
    public record Snapshot(List<Peringatan> peringatan,String diperbaruiPada) {}
    public Snapshot snapshot() {
        if (!current.requireUser().getPeran().equals("PETUGAS_LOKET")) throw new AccessDeniedException("Akses petugas loket diperlukan");
        var now=LocalDateTime.now(clock); var warnings=new ArrayList<Peringatan>();
        for (var j:jadwal.findByTanggal(now.toLocalDate())) {
            if (!CheckInPolicy.waktuDibuka(now,j.getTanggal(),j.getJamMulai(),j.getJamSelesai())) continue;
            for (var a:antrean.terdepanBelumDilayani(j.getIdJadwal(),PageRequest.of(0,DEKAT)))
                if (a.getStatusAntrean().equals("Terdaftar") && a.getWaktuCheckIn()==null)
                    warnings.add(new Peringatan(j.getIdJadwal(),a.getNomorAntrean(),j.getPoli().getNamaPoli(),j.getDokter().getNama(),j.getSesi(),j.getTanggal().toString()));
        }
        warnings.sort(Comparator.comparing(Peringatan::idJadwal).thenComparingInt(Peringatan::nomorAntrean));
        return new Snapshot(List.copyOf(warnings),now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }
}
