package personal.social.enums;

public enum MessageStatus {
    SENDING, SENT, DELIVERED, SEEN, FAILED;

    public String toString() {
        return name();
    }
}
