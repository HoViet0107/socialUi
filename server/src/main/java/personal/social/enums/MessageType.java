package personal.social.enums;

public enum MessageType {
    NEW_MESSAGE,
    MESSAGE_READ,
    USER_TYPING,
    USER_ONLINE,
    USER_OFFLINE,
    NEW_CONVERSATION;

    public String toString() {
        return name();
    }
}
