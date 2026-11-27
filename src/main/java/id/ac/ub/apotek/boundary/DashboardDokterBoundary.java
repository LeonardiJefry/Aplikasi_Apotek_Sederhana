package id.ac.ub.apotek.boundary;

import id.ac.ub.apotek.controller.*;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriUtils;

/** UC-09: halaman dan snapshot dashboard dokter; tindakan diteruskan ke layanan panggilan UC-06. */
@Controller
public class DashboardDokterBoundary {
    private final DashboardDokterService service;
    private final PanggilanService panggilan;
    public DashboardDokterBoundary(DashboardDokterService service, PanggilanService panggilan) {
        this.service=service; this.panggilan=panggilan;
    }

    @GetMapping("/dokter/antrean")
    public String halaman(@RequestParam(required=false) String idJadwal, Model model, HttpServletResponse response) {
        noStore(response);
        try { model.addAttribute("snapshot",service.snapshot(idJadwal)); }
        catch(IllegalArgumentException ex) { error(response,model,400,ex.getMessage()); }
        catch(NoSuchElementException ex) { error(response,model,404,ex.getMessage()); }
        catch(DataAccessException ex) { error(response,model,503,"Dashboard belum dapat dimuat. Silakan coba lagi."); }
        return "dashboarddokter/index";
    }

    @GetMapping("/dokter/antrean/data") @ResponseBody
    public ResponseEntity<?> data(@RequestParam(required=false) String idJadwal) {
        try { return json(200,service.snapshot(idJadwal)); }
        catch(IllegalArgumentException ex) { return json(400,Map.of("pesan",ex.getMessage())); }
        catch(NoSuchElementException ex) { return json(404,Map.of("pesan",ex.getMessage())); }
        catch(DataAccessException ex) { return json(503,Map.of("pesan","Dashboard belum dapat dimuat.")); }
    }

    @PostMapping("/dokter/antrean/{idJadwal}/{aksi}")
    public ModelAndView proses(@PathVariable String idJadwal, @PathVariable String aksi,
            @RequestParam(required=false) String idPanggilan, @RequestParam(required=false) String konfirmasi,
            Model model, HttpServletResponse response, RedirectAttributes flash) {
        noStore(response);
        try {
            // Pastikan jadwal milik dokter dan hari ini sebelum tindakan; PanggilanService memeriksa ulang di transaksi.
            service.snapshot(idJadwal);
            String pesan=switch(aksi) {
                case "panggil" -> { panggilan.panggil(idJadwal); yield "Nomor antrean dipanggil. Tunggu pasien masuk ruang periksa."; }
                case "hadir" -> { panggilan.hadir(idJadwal,idPanggilan); yield "Kehadiran dikonfirmasi. Pasien Sedang Dilayani."; }
                case "lewati" -> { panggilan.lewati(idJadwal,idPanggilan,"ya".equals(konfirmasi)); yield "Pelewatan tersimpan. Panggilan berikutnya diperbarui jika tersedia."; }
                default -> throw new IllegalArgumentException("Tindakan antrean tidak valid.");
            };
            flash.addFlashAttribute("pesanBerhasil",pesan);
            var redirect=new RedirectView("/dokter/antrean?idJadwal="+UriUtils.encodeQueryParam(idJadwal,StandardCharsets.UTF_8),true);
            redirect.setStatusCode(HttpStatus.SEE_OTHER);
            return new ModelAndView(redirect);
        } catch(IllegalArgumentException ex) { error(response,model,400,ex.getMessage()); }
        catch(NoSuchElementException ex) { error(response,model,404,ex.getMessage()); }
        catch(PanggilanException ex) { error(response,model,409,ex.getMessage()); }
        catch(DataAccessException ex) { error(response,model,503,"Perubahan gagal disimpan. Muat ulang dashboard dan coba lagi."); }
        // Transaksi tulis sudah selesai/rollback; tampilkan keadaan server terbaru bila jadwal sah.
        try { model.addAttribute("snapshot",service.snapshot(idJadwal)); }
        catch(IllegalArgumentException|NoSuchElementException|DataAccessException ignored) { /* Pesan awal dipertahankan. */ }
        return new ModelAndView("dashboarddokter/index",model.asMap());
    }

    private void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        response.setHeader("Referrer-Policy","no-referrer");
    }
    private ResponseEntity<?> json(int status,Object value) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).header("Referrer-Policy","no-referrer").body(value);
    }
    private void error(HttpServletResponse response,Model model,int status,String message) {
        response.setStatus(status); model.addAttribute("pesanGalat",message);
    }
}
