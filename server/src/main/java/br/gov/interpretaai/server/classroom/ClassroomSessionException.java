package br.gov.interpretaai.server.classroom;

public final class ClassroomSessionException extends RuntimeException {
    private final int status;
    private final String code;
    private final String safeMessage;

    public ClassroomSessionException(int status, String code, String safeMessage) {
        super(code);
        this.status = status;
        this.code = code;
        this.safeMessage = safeMessage;
    }

    public int status() { return status; }
    public String code() { return code; }
    public String safeMessage() { return safeMessage; }
}
