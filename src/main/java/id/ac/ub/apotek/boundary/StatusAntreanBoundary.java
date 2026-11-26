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

    private ResponseEntity<?> jawaban(int status, Object body) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }
    private void galat(Model model, HttpServletResponse response, int status, String pesan) {
        response.setStatus(status); model.addAttribute("pesanGalat", pesan);
    }
}
