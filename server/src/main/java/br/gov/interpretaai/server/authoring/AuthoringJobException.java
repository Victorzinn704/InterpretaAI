package br.gov.interpretaai.server.authoring;

public class AuthoringJobException extends RuntimeException {
    private final int status;
    private final String code;

    public AuthoringJobException(int status, String code, String safeMessage) {
        super(safeMessage);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}
