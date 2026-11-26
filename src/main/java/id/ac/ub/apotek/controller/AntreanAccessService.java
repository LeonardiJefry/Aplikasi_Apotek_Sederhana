package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.repository.AntreanRepository;
import id.ac.ub.apotek.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fondasi pembatasan kepemilikan untuk layanan fitur; belum mengimplementasikan UI-03/09. */
@Service
@Transactional(readOnly = true)
public class AntreanAccessService {
    private final AntreanRepository antrean;
    private final CurrentUserService current;
    public AntreanAccessService(AntreanRepository antrean, CurrentUserService current) {
        this.antrean = antrean;
        this.current = current;
    }
    public TicketIdentity requirePasienTicket(String idAntrean) {
        var tiket = antrean.findByIdAntreanAndPasien_IdPasien(idAntrean, current.requirePasienId())
                .orElseThrow(() -> new AccessDeniedException("Tiket tidak dapat diakses"));
        return new TicketIdentity(tiket.getIdAntrean(), tiket.getJadwal().getIdJadwal(), tiket.getNomorAntrean());
    }
    public TicketIdentity requireDokterTicket(String idAntrean) {
        var tiket = antrean.findByIdAntreanAndJadwal_Dokter_IdDokter(idAntrean, current.requireDokterId())
                .orElseThrow(() -> new AccessDeniedException("Tiket tidak dapat diakses"));
        return new TicketIdentity(tiket.getIdAntrean(), tiket.getJadwal().getIdJadwal(), tiket.getNomorAntrean());
    }
    public record TicketIdentity(String idAntrean, String idJadwal, int nomorAntrean) {}
}
