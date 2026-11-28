package id.ac.ub.apotek.boundary;

import id.ac.ub.apotek.controller.*;
import jakarta.servlet.http.*;
import java.util.NoSuchElementException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class PembatalanBoundary {
    private final PembatalanService service;
    private final StatusAntreanService status;
    public PembatalanBoundary(PembatalanService service, StatusAntreanService status) { this.service = service; this.status = status; }

    @GetMapping({"/pasien/antrean/{idAntrean}/pembatalan", "/pasien/antrean/{idAntrean}/pembatalan/hasil"})
    public String halaman(@PathVariable String idAntrean, HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        try {
            var tiket = status.snapshot(idAntrean).dipilih();
            model.addAttribute("tiket", tiket);
            if (request.getRequestURI().endsWith("/hasil")) {
                if (!tiket.statusAntrean().equals("Dibatalkan")) throw new PembatalanException("Antrean ini belum dibatalkan.");
                model.addAttribute("berhasil", true);
            } else if (!tiket.bolehBukaBatal()) throw new PembatalanException("Antrean tidak dapat dibatalkan dari status " + tiket.statusAntrean() + ".");
        } catch (IllegalArgumentException ex) { galat(response, model, 400, ex.getMessage()); }
        catch (NoSuchElementException ex) { galat(response, model, 404, ex.getMessage()); }
        catch (PembatalanException ex) { galat(response, model, 409, ex.getMessage()); }
        catch (DataAccessException ex) { galat(response, model, 503, "Pembatalan belum dapat dimuat. Silakan coba lagi."); }
        return "antreanpasien/pembatalan";
    }

    @PostMapping("/pasien/antrean/{idAntrean}/pembatalan")
    public ModelAndView batalkan(@PathVariable String idAntrean, @RequestParam(required = false) String konfirmasi,
                                 HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        try {
            var id = service.batalkan(idAntrean, "ya".equals(konfirmasi));
            var redirect = new RedirectView("/pasien/antrean/" + id + "/pembatalan/hasil", true);
            redirect.setStatusCode(HttpStatus.SEE_OTHER);
            return new ModelAndView(redirect);
        } catch (IllegalArgumentException ex) { galat(response, model, 400, ex.getMessage()); }
        catch (NoSuchElementException ex) { galat(response, model, 404, ex.getMessage()); }
        catch (PembatalanException ex) { galat(response, model, 409, ex.getMessage()); }
        catch (DataAccessException ex) { galat(response, model, 503, "Pembatalan gagal disimpan. Silakan muat ulang status antrean dan coba lagi."); }
        return new ModelAndView("antreanpasien/pembatalan", model.asMap());
    }
    private void galat(HttpServletResponse response, Model model, int status, String pesan) {
        response.setStatus(status); model.addAttribute("pesanGalat", pesan);
    }
}
