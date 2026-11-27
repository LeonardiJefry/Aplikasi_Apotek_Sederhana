package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.entity.Notifikasi;
import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.CurrentUserService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * UC-10 notifikasi loket (PC-10 Notifikasi.kirim/catat). Dijalankan sesudah status Selesai tersimpan dan dalam
 * transaksi sendiri, sehingga kegagalan notifikasi tidak membatalkan Selesai. Demo belum memiliki kanal kirim:
 * notifikasi yang tersimpan berstatus Tertunda dan ditampilkan pada panel loket (Modul 3 AH-SC05).
 */
@Service
public class NotifikasiLoketService {
    private static final DateTimeFormatter JAM=DateTimeFormatter.ofPattern("HH:mm");
    private static final char[] KODE="ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final NotifikasiRepository notifikasi;
    private final AntreanRepository antrean;
    private final JadwalPraktikRepository jadwal;
    private final CurrentUserService current;
    private final TransactionTemplate baru;
    private final SecureRandom random=new SecureRandom();
    /** Simulasi kegagalan kanal untuk demo/tes kriteria AH: Selesai tetap tersimpan saat notifikasi gagal. */
    private volatile boolean simulasiGagal;

    public NotifikasiLoketService(NotifikasiRepository notifikasi, AntreanRepository antrean, JadwalPraktikRepository jadwal,
            CurrentUserService current, PlatformTransactionManager tx, @Value("${klinik.notifikasi.simulasi-gagal:false}") boolean simulasiGagal) {
        this.notifikasi=notifikasi; this.antrean=antrean; this.jadwal=jadwal; this.current=current; this.simulasiGagal=simulasiGagal;
        this.baru=new TransactionTemplate(tx);
        baru.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        baru.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public void setSimulasiGagal(boolean simulasiGagal) { this.simulasiGagal=simulasiGagal; }

    public record Item(String idAntrean, String nomorTampilan, String nama, String waktuSelesai, String statusKirim) {}

    /**
     * Membuat atau mengirim ulang notifikasi untuk tiket Selesai milik jadwal dokter yang login.
     * Idempoten: tiket yang sudah punya notifikasi Tertunda/Terkirim tidak dibuatkan baris baru.
     * @return Tertunda, Terkirim atau Gagal
     */
    public String kirim(String idAntrean) {
        var idDokter=current.requireDokterId();
        if(idAntrean==null || idAntrean.isBlank() || idAntrean.length()>12) throw new IllegalArgumentException("ID antrean tidak valid.");
        var tiket=antrean.findByIdAntreanAndJadwal_Dokter_IdDokter(idAntrean,idDokter).orElseThrow(()->new NoSuchElementException("Antrean tidak ditemukan pada jadwal Anda."));
        if(!"Selesai".equals(tiket.getStatusAntrean()))
            throw new PemeriksaanException("Notifikasi loket hanya untuk pasien yang sudah Selesai.",tiket.getStatusAntrean(),tiket.getNomorAntrean());
        try {
            return baru.execute(status->simpan(idAntrean));
        } catch(RuntimeException gagal) {
            // PC-10: kegagalan dicatat sebagai Gagal; bila pencatatan juga gagal, status tetap dilaporkan Gagal.
            try { baru.executeWithoutResult(status->catatGagal(idAntrean)); } catch(RuntimeException ignored) { /* dilaporkan ke dokter */ }
            return "Gagal";
        }
    }

    /** Panel loket: notifikasi pemeriksaan selesai pada jadwal yang dipilih, terbaru dahulu. Hanya membaca. */
    @Transactional(readOnly=true)
    public List<Item> daftarUntukLoket(String idJadwal) {
        var actor=current.requireUser();
        if(!"PETUGAS_LOKET".equals(actor.getPeran())) throw new AccessDeniedException("Panel notifikasi hanya untuk petugas loket.");
        if(idJadwal==null || !jadwal.existsById(idJadwal)) return List.of();
        return notifikasi.selesaiPadaJadwal(idJadwal).stream().map(r->new Item((String)r[0],DashboardDokterService.nomor((Integer)r[1]),(String)r[2],
                r[3]==null?null:((LocalDateTime)r[3]).format(JAM),r[4]==null?"Belum tercatat":(String)r[4])).toList();
    }

    /** Status notifikasi satu tiket untuk dashboard dokter; null bila belum tercatat. */
    @Transactional(readOnly=true)
    public String status(String idAntrean) {
        return notifikasi.findFirstByAntrean_IdAntreanOrderByIdNotifikasi(idAntrean).map(Notifikasi::getStatusKirim).orElse(null);
    }

    private String simpan(String idAntrean) {
        var tiket=antrean.findForUpdate(idAntrean).orElseThrow(()->new NoSuchElementException("Antrean tidak ditemukan."));
        var ada=notifikasi.findFirstByAntrean_IdAntreanOrderByIdNotifikasi(idAntrean).orElse(null);
        if(ada!=null && !"Gagal".equals(ada.getStatusKirim())) return ada.getStatusKirim();
        if(simulasiGagal) throw new IllegalStateException("Kanal notifikasi loket tidak tersedia.");
        if(ada!=null) { ada.setStatusKirim("Tertunda"); ada.setWaktuKirim(null); }
        else notifikasi.save(new Notifikasi(idBaru(),tiket,"Tertunda",null));
        notifikasi.flush();
        return "Tertunda";
    }
    private void catatGagal(String idAntrean) {
        var tiket=antrean.findForUpdate(idAntrean).orElseThrow();
        if(notifikasi.findFirstByAntrean_IdAntreanOrderByIdNotifikasi(idAntrean).isEmpty())
            notifikasi.saveAndFlush(new Notifikasi(idBaru(),tiket,"Gagal",null));
    }
    private String idBaru() {
        String id;
        do {
            var kode=new StringBuilder("NTF-");
            for(int i=0;i<6;i++) kode.append(KODE[random.nextInt(KODE.length)]);
            id=kode.toString();
        } while(notifikasi.existsById(id));
        return id;
    }
}
