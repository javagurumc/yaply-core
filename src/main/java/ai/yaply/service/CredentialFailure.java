package ai.yaply.service;

public class CredentialFailure extends RuntimeException {
    public enum Reason { SETUP, MISSING, UNREADABLE }
    private final Reason reason;
    public CredentialFailure(Reason reason) {
        super(switch (reason) {
            case SETUP -> "API key storage is unavailable. Contact the application administrator.";
            case MISSING -> "The tutor has not configured an API key.";
            case UNREADABLE -> "The saved API key cannot be read. Contact the administrator or replace it.";
        });
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
