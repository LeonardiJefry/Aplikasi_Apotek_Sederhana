package id.ac.ub.apotek.controller;

/** UC-10: status antrean tidak memenuhi syarat untuk diselesaikan (PC-10 STATUS_TIDAK_VALID). */
public class PemeriksaanException extends RuntimeException {
    private final String statusAntrean;
    private final int nomorAntrean;
    public PemeriksaanException(String pesan, String statusAntrean, int nomorAntrean) {
        super(pesan); this.statusAntrean=statusAntrean; this.nomorAntrean=nomorAntrean;
    }
    public String getStatusAntrean() { return statusAntrean; }
    public int getNomorAntrean() { return nomorAntrean; }
}
