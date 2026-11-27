package id.ac.ub.apotek.boundary;

import id.ac.ub.apotek.controller.VerifikasiPasienService;
import id.ac.ub.apotek.security.CurrentUserService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller @RequestMapping("/loket/pasien")
public class VerifikasiPasienBoundary {
    private final VerifikasiPasienService service;
    private final CurrentUserService current;
    public VerifikasiPasienBoundary(VerifikasiPasienService service,CurrentUserService current) { this.service=service; this.current=current; }
    @GetMapping
    public String cari(@RequestParam(required=false) String jenis,@RequestParam(required=false) String kataKunci,
            @RequestParam(defaultValue="0") int halaman,Model model,HttpServletResponse response) {
        dasar(model,response); model.addAttribute("jenis",jenis==null?"NO_RM":jenis); model.addAttribute("kataKunci",kataKunci==null?"":kataKunci);
        model.addAttribute("hasil",null);
        try { if(jenis!=null || kataKunci!=null) model.addAttribute("hasil",service.cari(jenis,kataKunci,halaman)); }
        catch(IllegalArgumentException ex) { galat(model,response,400,ex.getMessage()); }
        catch(DataAccessException ex) { galat(model,response,503,"Pencarian belum dapat dilakukan. Silakan coba lagi."); }
        return "pasien/cari";
    }
    @GetMapping("/{idPasien}")
    public String detail(@PathVariable String idPasien,@RequestParam(defaultValue="0") int halamanPendaftaran,
            @RequestParam(defaultValue="0") int halamanKunjungan,Model model,HttpServletResponse response) {
        return halamanDetail(idPasien,halamanPendaftaran,halamanKunjungan,model,response);
    }
    @PostMapping("/{idPasien}/verifikasi")
    public ModelAndView verifikasi(@PathVariable String idPasien,@RequestParam(required=false) String idVerifikasi,
            @RequestParam(required=false) String sidikIdentitas,@RequestParam(required=false) String konfirmasi,
            Model model,HttpServletResponse response,RedirectAttributes flash) {
        dasar(model,response);
        try {
            var result=service.verifikasi(idPasien,idVerifikasi,sidikIdentitas,"ya".equals(konfirmasi));
            flash.addFlashAttribute("pesanSukses","Identitas Terverifikasi oleh "+result.petugas()+" pada "+result.waktu()+" WIB.");
            var redirect=new RedirectView("/loket/pasien/"+idPasien,true); redirect.setStatusCode(HttpStatus.SEE_OTHER);
            return new ModelAndView(redirect);
        } catch(IllegalArgumentException ex) { galat(model,response,400,ex.getMessage()); }
        catch(NoSuchElementException ex) { galat(model,response,404,ex.getMessage()); }
        catch(IllegalStateException ex) { galat(model,response,409,ex.getMessage()); }
        catch(DataAccessException ex) { galat(model,response,503,"Verifikasi gagal disimpan. Riwayat sebelumnya tetap berlaku. Silakan coba lagi."); }
        return new ModelAndView(halamanDetail(idPasien,0,0,model,response),model.asMap());
    }
    private String halamanDetail(String id,int pageA,int pageB,Model model,HttpServletResponse response) {
        dasar(model,response); model.addAttribute("detail",null); model.addAttribute("idVerifikasi",UUID.randomUUID().toString());
        try { model.addAttribute("detail",service.detail(id,pageA,pageB)); }
        catch(IllegalArgumentException ex) { jikaBelum(model,response,400,ex.getMessage()); }
        catch(NoSuchElementException ex) { jikaBelum(model,response,404,ex.getMessage()); }
        catch(DataAccessException ex) { jikaBelum(model,response,503,"Data pasien belum dapat dimuat. Silakan coba lagi."); }
        return "pasien/detail";
    }
    private void dasar(Model model,HttpServletResponse response) {
        model.addAttribute("username",current.requireUser().getUsername());
        response.setHeader("Cache-Control","no-store"); response.setHeader("Referrer-Policy","no-referrer");
    }
    private void galat(Model model,HttpServletResponse response,int code,String text) { response.setStatus(code); model.addAttribute("pesanGalat",text); }
    private void jikaBelum(Model model,HttpServletResponse response,int code,String text) { if(!model.containsAttribute("pesanGalat")) galat(model,response,code,text); }
}
