package br.gov.interpretaai.server.media;

public class MediaUploadException extends RuntimeException {
    private final int status;
    private final String code;

    public MediaUploadException(int status, String code, String safeMessage) {
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
