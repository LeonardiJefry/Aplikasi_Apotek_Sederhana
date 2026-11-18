package id.ac.ub.apotek.boundary;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Boundary untuk halaman awal; aturan bisnis ditempatkan pada package controller. */
@Controller
public class HomeBoundary {
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
