package br.gov.interpretaai.server.delivery;

public class DeliveryException extends RuntimeException {
    private final int status;
    private final String code;

    public DeliveryException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
