package id.ac.ub.apotek.boundary;

import id.ac.ub.apotek.controller.StatusAntreanService;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StatusAntreanBoundary {
    private final StatusAntreanService service;
    public StatusAntreanBoundary(StatusAntreanService service) { this.service = service; }

    @GetMapping({"/pasien/antrean", "/pasien/antrean/{idAntrean}"})
    public String halaman(@PathVariable(required = false) String idAntrean,
                          @RequestParam(required = false, name = "tiket") String pilihan,
                          Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        try { model.addAttribute("snapshot", service.snapshot(idAntrean == null ? pilihan : idAntrean)); }
        catch (IllegalArgumentException ex) { galat(model, response, 400, ex.getMessage()); }
        catch (NoSuchElementException ex) { galat(model, response, 404, ex.getMessage()); }
        catch (DataAccessException ex) { galat(model, response, 503, "Status antrean belum dapat dimuat. Silakan coba lagi."); }
        return "antreanpasien/status";
    }

    @GetMapping({"/pasien/antrean/data", "/pasien/antrean/{idAntrean}/data"})
    @ResponseBody
    public ResponseEntity<?> data(@PathVariable(required = false) String idAntrean) {
        try { return jawaban(200, service.snapshot(idAntrean)); }
        catch (IllegalArgumentException ex) { return jawaban(400, Map.of("pesan", ex.getMessage())); }
        catch (NoSuchElementException ex) { return jawaban(404, Map.of("pesan", ex.getMessage())); }
        catch (DataAccessException ex) { return jawaban(503, Map.of("pesan", "Status antrean belum dapat dimuat. Silakan coba lagi.")); }
    }

    /** Sambungan baca-saja; transaksi pembatalan/check-in disediakan pada UC-04/05. */
    @GetMapping({"/pasien/antrean/{idAntrean}/pembatalan", "/pasien/antrean/{idAntrean}/check-in"})
    public String informasi(@PathVariable String idAntrean, HttpServletRequest request,
                            HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        boolean batal = request.getRequestURI().endsWith("/pembatalan");
        model.addAttribute("judul", batal ? "Pembatalan antrean" : "Check-in kedatangan");
        try {
            var tiket = service.snapshot(idAntrean).dipilih();
            model.addAttribute("tiket", tiket);
            if (batal ? tiket.bolehBatal() : tiket.bolehCheckIn()) {
                model.addAttribute("informasi", "Layanan mandiri ini belum tersedia. Silakan hubungi petugas loket.");
            } else galat(model, response, 409, batal
                    ? "Tiket tidak memenuhi syarat pembatalan. Pembatalan memerlukan waktu lebih dari 60 menit sebelum praktik dan status yang diizinkan."
                    : "Check-in hanya untuk tiket Terdaftar pada hari kunjungan, mulai 30 menit sebelum praktik sampai jam selesai.");
        } catch (IllegalArgumentException ex) { galat(model, response, 400, ex.getMessage()); }
        catch (NoSuchElementException ex) { galat(model, response, 404, ex.getMessage()); }
        catch (DataAccessException ex) { galat(model, response, 503, "Tiket belum dapat dimuat. Silakan coba lagi."); }
        return "antreanpasien/informasi";
    }

    private ResponseEntity<?> jawaban(int status, Object body) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }
    private void galat(Model model, HttpServletResponse response, int status, String pesan) {
        response.setStatus(status); model.addAttribute("pesanGalat", pesan);
    }
}
