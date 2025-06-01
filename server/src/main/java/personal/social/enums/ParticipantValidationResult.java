package personal.social.enums;

public enum ParticipantValidationResult {
    VALID,
    NOT_IN_CONVERSATION,
    NOT_ADMIN,
    NOT_GROUP_CHAT,
    TARGET_NOT_FOUND;
    @Override
    public String toString() {
        return name();
    }
}
