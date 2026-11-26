package id.ac.ub.apotek.boundary;

import id.ac.ub.apotek.controller.*;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class CheckInBoundary {
    private final CheckInService service;
    private final LoketCheckInService loket;
    public CheckInBoundary(CheckInService service,LoketCheckInService loket) { this.service=service; this.loket=loket; }

    @GetMapping({"/pasien/check-in","/pasien/antrean/{idAntrean}/check-in","/pasien/antrean/{idAntrean}/check-in/hasil"})
    public String halaman(@PathVariable(required=false) String idAntrean,HttpServletRequest request,HttpServletResponse response,Model model) {
        response.setHeader("Cache-Control","no-store");
        try {
            var panel=service.panel(idAntrean); model.addAttribute("panel",panel);
            if (request.getRequestURI().endsWith("/hasil")) {
                if (panel.tiket()==null || panel.tiket().waktuCheckIn()==null) throw new CheckInException("Kehadiran belum tercatat.");
                model.addAttribute("berhasil",true);
            } else if (panel.tiket()!=null && !panel.tiket().bolehCheckIn())
                throw new CheckInException("Check-in belum dapat dilakukan. Periksa status tiket, tanggal kunjungan dan jendela check-in.");
        } catch (IllegalArgumentException ex) { error(response,model,400,ex.getMessage()); }
        catch (NoSuchElementException ex) { error(response,model,404,ex.getMessage()); }
        catch (CheckInException ex) { error(response,model,409,ex.getMessage()); }
        catch (DataAccessException ex) { error(response,model,503,"Check-in belum dapat dimuat. Silakan coba lagi."); }
        return "checkin/index";
    }
    @PostMapping("/pasien/antrean/{idAntrean}/check-in")
    public ModelAndView checkIn(@PathVariable String idAntrean,HttpServletResponse response,Model model) {
        response.setHeader("Cache-Control","no-store");
        try {
            var id=service.checkIn(idAntrean); var redirect=new RedirectView("/pasien/antrean/"+id+"/check-in/hasil",true);
            redirect.setStatusCode(HttpStatus.SEE_OTHER); return new ModelAndView(redirect);
        } catch (IllegalArgumentException ex) { error(response,model,400,ex.getMessage()); }
        catch (NoSuchElementException ex) { error(response,model,404,ex.getMessage()); }
        catch (CheckInException ex) { error(response,model,409,ex.getMessage()); }
        catch (DataAccessException ex) { error(response,model,503,"Check-in gagal disimpan. Silakan muat ulang antrean dan coba lagi."); }
        return new ModelAndView("checkin/index",model.asMap());
    }
    @GetMapping("/pasien/antrean/{idAntrean}/check-in/data")
    @ResponseBody public ResponseEntity<?> data(@PathVariable String idAntrean) {
        try { return json(200,service.panel(idAntrean)); }
        catch (IllegalArgumentException ex) { return json(400,Map.of("pesan",ex.getMessage())); }
        catch (NoSuchElementException ex) { return json(404,Map.of("pesan",ex.getMessage())); }
        catch (DataAccessException ex) { return json(503,Map.of("pesan","Check-in belum dapat dimuat.")); }
    }
    @GetMapping("/loket/check-in")
    public String peringatan(Model model,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        try { model.addAttribute("snapshot",loket.snapshot()); }
        catch (DataAccessException ex) { error(response,model,503,"Peringatan belum dapat dimuat. Silakan coba lagi."); }
        return "checkin/loket";
    }
    @GetMapping("/loket/check-in/data")
    @ResponseBody public ResponseEntity<?> peringatanData() {
        try { return json(200,loket.snapshot()); }
        catch (DataAccessException ex) { return json(503,Map.of("pesan","Peringatan belum dapat dimuat.")); }
    }
    private ResponseEntity<?> json(int status,Object body) { return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body); }
    private void error(HttpServletResponse response,Model model,int status,String pesan) { response.setStatus(status); model.addAttribute("pesanGalat",pesan); }
}
