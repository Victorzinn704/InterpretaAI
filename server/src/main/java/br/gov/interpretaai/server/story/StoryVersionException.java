package br.gov.interpretaai.server.story;

public class StoryVersionException extends RuntimeException {
    private final int status;
    private final String code;

    public StoryVersionException(int status, String code, String message) {
        super(message);
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
