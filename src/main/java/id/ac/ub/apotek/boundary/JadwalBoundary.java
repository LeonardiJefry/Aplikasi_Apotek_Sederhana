package id.ac.ub.apotek.boundary;

import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.CurrentUserService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/pasien/jadwal")
public class JadwalBoundary {
    private final JadwalService service;
    private final CurrentUserService current;
    private final Clock clock;

    public JadwalBoundary(JadwalService service, CurrentUserService current, Clock clock) {
        this.service = service; this.current = current; this.clock = clock;
    }

    @GetMapping
    public String jadwal(@RequestParam(required = false) String tanggal,
            @RequestParam(required = false) String idPoli, @RequestParam(required = false) String idDokter,
            Model model, HttpServletResponse response) {
        current.requirePasienId();
        response.setHeader("Cache-Control", "no-store");
        boolean dicari = tanggal != null || idPoli != null || idDokter != null;
        model.addAttribute("tanggal", tanggal == null ? LocalDate.now(clock).toString() : tanggal);
        model.addAttribute("idPoli", idPoli == null ? "" : idPoli);
        model.addAttribute("idDokter", idDokter == null ? "" : idDokter);
        model.addAttribute("dicari", dicari);
        model.addAttribute("username", current.requireUser().getUsername());
        model.addAttribute("hariIni", LocalDate.now(clock).toString());
        model.addAttribute("jadwal", List.of());
        model.addAttribute("tersedia", 0L);
        model.addAttribute("penuh", 0L);
        model.addAttribute("pilihan", new JadwalService.PilihanFilter(List.of(), List.of()));
        try {
            model.addAttribute("pilihan", service.pilihanFilter());
            if (dicari) {
                var rows = service.ambilJadwal(tanggal, idPoli, idDokter);
                model.addAttribute("jadwal", rows);
                model.addAttribute("tersedia", rows.stream().filter(JadwalDto::tersedia).count());
                model.addAttribute("penuh", rows.stream().filter(j -> !j.tersedia()).count());
                model.addAttribute("diperbarui", waktu());
            }
        } catch (IllegalArgumentException ex) {
            response.setStatus(400);
            model.addAttribute("pesanGalat", ex.getMessage());
        } catch (DataAccessException ex) {
            response.setStatus(503);
            model.addAttribute("pesanGalat", "Jadwal belum dapat dimuat. Silakan coba lagi.");
        }
        return "jadwal/index";
    }

    public record Snapshot(List<JadwalDto> jadwal, String diperbaruiPada) {}

    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<?> data(@RequestParam(required = false) String tanggal,
            @RequestParam(required = false) String idPoli, @RequestParam(required = false) String idDokter) {
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(new Snapshot(service.ambilJadwal(tanggal, idPoli, idDokter), waktu()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message", ex.getMessage()));
        } catch (DataAccessException ex) {
            return ResponseEntity.status(503).cacheControl(CacheControl.noStore())
                    .body(Map.of("message", "Jadwal belum dapat dimuat. Silakan coba lagi."));
        }
    }

    @GetMapping("/pilih")
    public String pilih(@RequestParam(required = false) String idJadwal, Model model, HttpServletResponse response) {
        current.requirePasienId();
        response.setHeader("Cache-Control", "no-store");
        try {
            model.addAttribute("jadwal", service.pilihJadwal(idJadwal));
        } catch (NoSuchElementException ex) {
            response.setStatus(404); model.addAttribute("pesanGalat", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            response.setStatus(400); model.addAttribute("pesanGalat", ex.getMessage());
        } catch (IllegalStateException ex) {
            response.setStatus(409); model.addAttribute("pesanGalat", ex.getMessage());
        } catch (DataAccessException ex) {
            response.setStatus(503); model.addAttribute("pesanGalat", "Jadwal belum dapat dimuat. Silakan coba lagi.");
        }
        return "jadwal/pilihan";
    }

    private String waktu() { return LocalTime.now(clock).format(DateTimeFormatter.ofPattern("HH:mm:ss")); }
}
