package id.ac.ub.apotek.controller;

public class BookingException extends RuntimeException {
    public enum Reason { INVALID, NOT_FOUND, FULL, CLOSED }
    private final Reason reason;
    public BookingException(Reason reason, String message) { super(message); this.reason = reason; }
    public Reason reason() { return reason; }
}
