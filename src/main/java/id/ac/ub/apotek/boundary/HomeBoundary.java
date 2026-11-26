package id.ac.ub.apotek.boundary;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import id.ac.ub.apotek.security.CurrentUserService;
import org.springframework.ui.Model;

/** Boundary untuk halaman awal; aturan bisnis ditempatkan pada package controller. */
@Controller
public class HomeBoundary {
    private final CurrentUserService current;
    public HomeBoundary(CurrentUserService current) { this.current = current; }

    @GetMapping("/")
    public String index() {
        return "redirect:" + switch (current.requireUser().getPeran()) {
            case "PASIEN" -> "/pasien";
            case "PETUGAS_LOKET" -> "/loket";
            case "ADMIN" -> "/admin";
            case "DOKTER" -> "/dokter";
            default -> throw new org.springframework.security.access.AccessDeniedException("Peran tidak valid");
        };
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping({"/pasien", "/loket", "/admin", "/dokter"})
    public String home(Model model) {
        var user = current.requireUser();
        model.addAttribute("username", user.getUsername());
        model.addAttribute("peran", user.getPeran());
        model.addAttribute("roleLabel", switch (user.getPeran()) {
            case "PASIEN" -> "Pasien";
            case "PETUGAS_LOKET" -> "Petugas Loket";
            case "ADMIN" -> "Admin";
            default -> "Dokter";
        });
        return "index";
    }

    // AccessDeniedHandler forwards the original method, including POST on CSRF failure.
    @RequestMapping("/akses-ditolak")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String denied() { return "error/403"; }
}
