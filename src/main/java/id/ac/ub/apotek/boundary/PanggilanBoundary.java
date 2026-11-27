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

@Controller
public class PanggilanBoundary {
    private final PanggilanService service;
    public PanggilanBoundary(PanggilanService service) { this.service=service; }
    @GetMapping("/loket/panggilan")
    public String halaman(@RequestParam(required=false) String idJadwal,Model model,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        try { model.addAttribute("snapshot",service.snapshot(idJadwal)); }
        catch(IllegalArgumentException ex) { error(response,model,400,ex.getMessage()); }
        catch(NoSuchElementException ex) { error(response,model,404,ex.getMessage()); }
        catch(DataAccessException ex) { error(response,model,503,"Antrean belum dapat dimuat. Silakan coba lagi."); }
        return "panggilan/index";
    }
    @GetMapping("/loket/panggilan/data") @ResponseBody
    public ResponseEntity<?> data(@RequestParam(required=false) String idJadwal) {
        try { return json(200,service.snapshot(idJadwal)); }
        catch(IllegalArgumentException ex) { return json(400,Map.of("pesan",ex.getMessage())); }
        catch(NoSuchElementException ex) { return json(404,Map.of("pesan",ex.getMessage())); }
        catch(DataAccessException ex) { return json(503,Map.of("pesan","Antrean belum dapat dimuat.")); }
    }
    @PostMapping("/loket/panggilan/{idJadwal}/{aksi}")
    public ModelAndView proses(@PathVariable String idJadwal,@PathVariable String aksi,
            @RequestParam(required=false) String idPanggilan,@RequestParam(required=false) String konfirmasi,
            Model model,HttpServletResponse response,RedirectAttributes flash) {
        response.setHeader("Cache-Control","no-store");
        try {
            String message=switch(aksi) {
                case "panggil" -> { service.panggil(idJadwal); yield "Nomor antrean dipanggil. Tunggu konfirmasi kehadiran pasien."; }
                case "hadir" -> { service.hadir(idJadwal,idPanggilan); yield "Kehadiran dikonfirmasi. Pasien Sedang Dilayani."; }
                case "lewati" -> { service.lewati(idJadwal,idPanggilan,"ya".equals(konfirmasi)); yield "Pelewatan tersimpan. Panggilan berikutnya diperbarui jika tersedia."; }
                default -> throw new IllegalArgumentException("Tindakan panggilan tidak valid.");
            };
            flash.addFlashAttribute("pesanBerhasil",message);
            var redirect=new RedirectView("/loket/panggilan?idJadwal="+UriUtils.encodeQueryParam(idJadwal,StandardCharsets.UTF_8),true);
            redirect.setStatusCode(HttpStatus.SEE_OTHER); return new ModelAndView(redirect);
        } catch(IllegalArgumentException ex) { error(response,model,400,ex.getMessage()); }
        catch(NoSuchElementException ex) { error(response,model,404,ex.getMessage()); }
        catch(PanggilanException ex) { error(response,model,409,ex.getMessage()); }
        catch(DataAccessException ex) { error(response,model,503,"Perubahan gagal disimpan. Muat ulang antrean dan coba lagi."); }
        // Operasi tulis sudah berakhir/rollback; baca ulang keadaan server untuk pemulihan form lama.
        try { model.addAttribute("snapshot",service.snapshot(idJadwal)); }
        catch(IllegalArgumentException|NoSuchElementException|DataAccessException ignored) { /* Pesan awal dipertahankan. */ }
        return new ModelAndView("panggilan/index",model.asMap());
    }
    private ResponseEntity<?> json(int status,Object value) { return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(value); }
    private void error(HttpServletResponse response,Model model,int status,String message) { response.setStatus(status); model.addAttribute("pesanGalat",message); }
}
