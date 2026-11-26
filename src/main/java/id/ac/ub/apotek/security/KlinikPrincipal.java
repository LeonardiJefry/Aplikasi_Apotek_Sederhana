package id.ac.ub.apotek.security;

import id.ac.ub.apotek.entity.Pengguna;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

/** Hanya ID dan hash login yang disalin; tidak menyimpan entitas/pasien di sesi. */
public final class KlinikPrincipal extends User {
    private final String idPengguna;
    private final String idPasien;
    private final String idDokter;
    private final String peran;

    public KlinikPrincipal(Pengguna pengguna) {
        super(pengguna.getUsername(), pengguna.getPasswordHash(), pengguna.isAktif(), true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_" + pengguna.getPeran()));
        idPengguna = pengguna.getIdPengguna();
        idPasien = pengguna.getPasien() == null ? null : pengguna.getPasien().getIdPasien();
        idDokter = pengguna.getDokter() == null ? null : pengguna.getDokter().getIdDokter();
        peran = pengguna.getPeran();
    }

    public String getIdPengguna() { return idPengguna; }
    public String getIdPasien() { return idPasien; }
    public String getIdDokter() { return idDokter; }
    public String getPeran() { return peran; }
}
