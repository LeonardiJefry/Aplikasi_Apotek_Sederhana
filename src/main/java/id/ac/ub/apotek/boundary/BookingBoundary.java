package id.ac.ub.apotek.boundary;

import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.CurrentUserService;
import jakarta.servlet.http.*;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class BookingBoundary {
    private final BookingService service;
    private final BookingDraftStore drafts;
    private final CurrentUserService current;
    public BookingBoundary(BookingService service, BookingDraftStore drafts, CurrentUserService current) {
        this.service = service; this.drafts = drafts; this.current = current;
    }

    @GetMapping({"/pasien/booking", "/pasien/booking/konfirmasi"})
    public String ringkasan(@RequestParam(required = false) String idJadwal, HttpServletRequest request,
                            HttpServletResponse response, Model model) {
        var idPasien = current.requirePasienId();
        response.setHeader("Cache-Control", "no-store");
        if (idJadwal == null || idJadwal.isBlank()) return "redirect:/pasien/jadwal";
        boolean konfirmasi = request.getRequestURI().endsWith("/konfirmasi");
        model.addAttribute("konfirmasi", konfirmasi);
        try {
            if (konfirmasi && idJadwal.length() <= 10) {
                var draft = drafts.siapkan(request.getSession(), idPasien, idJadwal.strip());
                var existing = service.hasilDraft(draft.idJadwal(), draft.token());
                if (existing.isPresent()) return "redirect:/pasien/booking/tiket/" + existing.get().idAntrean();
                model.addAttribute("bookingToken", draft.token());
            }
            model.addAttribute("ringkasan", service.buatRingkasan(idJadwal));
        } catch (BookingException ex) {
            galat(model, response, ex, idJadwal);
        } catch (DataAccessException ex) {
            response.setStatus(503); model.addAttribute("pesanGalat", "Booking gagal, silakan coba lagi.");
        }
        return "booking/ringkasan";
    }

    @PostMapping("/pasien/booking/konfirmasi")
    public ModelAndView konfirmasi(@RequestParam(required = false) String bookingToken,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        var idPasien = current.requirePasienId();
        response.setHeader("Cache-Control", "no-store");
        BookingDraftStore.Draft draft = null;
        try {
            draft = drafts.ambil(request.getSession(), bookingToken, idPasien);
            var ticket = service.prosesBooking(draft.idJadwal(), draft.token());
            return redirect("/pasien/booking/tiket/" + ticket.idAntrean());
        } catch (IllegalArgumentException ex) {
            response.setStatus(400); model.addAttribute("pesanGalat", ex.getMessage());
        } catch (BookingException ex) {
            galat(model, response, ex, draft == null ? null : draft.idJadwal());
        } catch (DataAccessException ex) {
            response.setStatus(503); model.addAttribute("pesanGalat", "Booking gagal, silakan coba lagi.");
        }
        if (draft != null) model.addAttribute("cobaLagi", draft.idJadwal());
        return new ModelAndView("booking/ringkasan", model.asMap());
    }

    @PostMapping("/pasien/booking/batal")
    public ModelAndView batal(@RequestParam(required = false) String bookingToken,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        var idPasien = current.requirePasienId();
        response.setHeader("Cache-Control", "no-store");
        try {
            var draft = drafts.ambil(request.getSession(), bookingToken, idPasien);
            var existing = service.hasilDraft(draft.idJadwal(), draft.token());
            if (existing.isPresent()) return redirect("/pasien/booking/tiket/" + existing.get().idAntrean());
            drafts.hapus(request.getSession(), draft);
            return redirect("/pasien/jadwal");
        } catch (IllegalArgumentException ex) {
            response.setStatus(400); model.addAttribute("pesanGalat", ex.getMessage());
        }
        catch (DataAccessException ex) {
            response.setStatus(503); model.addAttribute("pesanGalat", "Booking belum dapat dimuat. Silakan coba lagi.");
        }
        return new ModelAndView("booking/ringkasan", model.asMap());
    }

    @GetMapping("/pasien/booking/tiket/{idAntrean}")
    public String tiket(@PathVariable String idAntrean, Model model, HttpServletResponse response) {
        current.requirePasienId();
        response.setHeader("Cache-Control", "no-store");
        try { model.addAttribute("tiket", service.tiket(idAntrean)); }
        catch (BookingException ex) { response.setStatus(404); model.addAttribute("pesanGalat", ex.getMessage()); }
        catch (DataAccessException ex) { response.setStatus(503); model.addAttribute("pesanGalat", "Tiket belum dapat dimuat. Silakan coba lagi."); }
        return "booking/tiket";
    }

    private void galat(Model model, HttpServletResponse response, BookingException ex, String idJadwal) {
        response.setStatus(switch (ex.reason()) { case INVALID -> 400; case NOT_FOUND -> 404; default -> 409; });
        model.addAttribute("pesanGalat", ex.getMessage());
        model.addAttribute("alternatif", List.of());
        if (ex.reason() != BookingException.Reason.INVALID && idJadwal != null) {
            try { model.addAttribute("alternatif", service.alternatif(idJadwal)); }
            catch (DataAccessException ignored) { /* Tautan kembali ke jadwal tetap tersedia. */ }
        }
    }

    private ModelAndView redirect(String url) {
        var view = new RedirectView(url, true);
        view.setStatusCode(HttpStatus.SEE_OTHER);
        return new ModelAndView(view);
    }
}
