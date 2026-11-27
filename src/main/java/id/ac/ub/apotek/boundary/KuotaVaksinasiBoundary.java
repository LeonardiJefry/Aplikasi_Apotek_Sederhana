package id.ac.ub.apotek.boundary;

import id.ac.ub.apotek.controller.KuotaVaksinasiService;
import id.ac.ub.apotek.security.CurrentUserService;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class KuotaVaksinasiBoundary {
    private final KuotaVaksinasiService service;
    private final CurrentUserService current;
    private final Clock clock;
    public KuotaVaksinasiBoundary(KuotaVaksinasiService service,CurrentUserService current,Clock clock) {
        this.service=service; this.current=current; this.clock=clock;
    }
    @GetMapping("/admin/vaksinasi")
    public String admin(@RequestParam(required=false) String tanggal,@RequestParam(required=false) String jenisVaksin,
            @RequestParam(required=false) String idJadwalVaksinasi,Model model,HttpServletResponse response) {
        return halaman(tanggal,jenisVaksin,idJadwalVaksinasi,null,true,model,response);
    }
    @PostMapping("/admin/vaksinasi/kuota")
    public ModelAndView simpan(@RequestParam(required=false) String tanggal,@RequestParam(required=false) String jenisVaksin,
            @RequestParam(required=false) String idJadwalVaksinasi,@RequestParam(required=false) String kuotaBaru,
            @RequestParam(required=false) String versi,Model model,HttpServletResponse response,RedirectAttributes flash) {
        response.setHeader("Cache-Control","no-store");
        try {
            var result=service.simpan(idJadwalVaksinasi,tanggal,jenisVaksin,kuotaBaru,versi);
            flash.addFlashAttribute("pesanSukses","Kuota berhasil diperbarui. Sisa kuota "+result.sisaKuota()+", status "+result.status()+".");
            var redirect=new RedirectView("/admin/vaksinasi?tanggal="+encode(result.tanggal())+"&jenisVaksin="+encode(result.jenisVaksin())+"&idJadwalVaksinasi="+encode(result.idJadwalVaksinasi()),true);
            redirect.setStatusCode(HttpStatus.SEE_OTHER); return new ModelAndView(redirect);
        } catch(IllegalArgumentException ex) { galat(model,response,400,ex.getMessage()); }
        catch(NoSuchElementException ex) { galat(model,response,404,ex.getMessage()); }
        catch(IllegalStateException ex) { galat(model,response,409,ex.getMessage()); }
        catch(DataAccessException ex) { galat(model,response,503,"Kuota gagal disimpan. Data sebelumnya tetap berlaku. Silakan coba lagi."); }
        return new ModelAndView(halaman(tanggal,jenisVaksin,idJadwalVaksinasi,kuotaBaru,true,model,response),model.asMap());
    }
    @GetMapping("/vaksinasi")
    public String publik(@RequestParam(required=false) String tanggal,@RequestParam(required=false) String jenisVaksin,
            Model model,HttpServletResponse response) {
        return halaman(tanggal,jenisVaksin,null,null,false,model,response);
    }
    @GetMapping("/vaksinasi/data") @ResponseBody
    public ResponseEntity<?> data(@RequestParam(required=false) String tanggal,@RequestParam(required=false) String jenisVaksin) {
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("layanan",service.cari(tanggal,jenisVaksin))); }
        catch(IllegalArgumentException ex) { return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message",ex.getMessage())); }
        catch(DataAccessException ex) { return ResponseEntity.status(503).cacheControl(CacheControl.noStore()).body(Map.of("message","Ketersediaan belum dapat dimuat. Silakan coba lagi.")); }
    }
    private String halaman(String tanggal,String jenis,String id,String input,boolean admin,Model model,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        if(admin) model.addAttribute("username",current.requireUser().getUsername());
        model.addAttribute("admin",admin); model.addAttribute("tanggal",tanggal==null?LocalDate.now(clock).toString():tanggal);
        model.addAttribute("jenisVaksin",jenis==null?"":jenis); model.addAttribute("pilihanJenis",List.of());
        model.addAttribute("layanan",List.of()); model.addAttribute("dipilih",null); model.addAttribute("kuotaBaru",input);
        try {
            model.addAttribute("pilihanJenis",service.jenisVaksin());
            if(tanggal!=null || jenis!=null || id!=null) {
                var rows=service.cari(tanggal,jenis); model.addAttribute("layanan",rows);
                if(admin) {
                    // Hasil ambigu wajib dipilih secara eksplisit, tidak pernah memakai baris pertama.
                    if(rows.size()>1 && (id==null || id.isBlank())) model.addAttribute("pesanPilihan","Ada beberapa jadwal. Pilih ID layanan yang akan diubah.");
                    else { var selected=service.pilih(tanggal,jenis,id); model.addAttribute("dipilih",selected);
                        if(input==null) model.addAttribute("kuotaBaru",Integer.toString(selected.kuota())); }
                }
            }
        } catch(IllegalArgumentException ex) { galatJikaBelum(model,response,400,ex.getMessage()); }
        catch(NoSuchElementException ex) { galatJikaBelum(model,response,404,ex.getMessage()); }
        catch(IllegalStateException ex) { galatJikaBelum(model,response,409,ex.getMessage()); }
        catch(DataAccessException ex) { galatJikaBelum(model,response,503,"Layanan vaksinasi belum dapat dimuat. Silakan coba lagi."); }
        return "vaksinasi/index";
    }
    private void galat(Model model,HttpServletResponse response,int status,String text) { response.setStatus(status); model.addAttribute("pesanGalat",text); }
    private void galatJikaBelum(Model model,HttpServletResponse response,int status,String text) { if(!model.containsAttribute("pesanGalat")) galat(model,response,status,text); }
    private String encode(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8); }
}
