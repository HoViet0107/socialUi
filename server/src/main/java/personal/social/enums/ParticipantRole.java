package personal.social.enums;

public enum ParticipantRole {
    ADMIN,
    VICE_ADMIN,
    NOT_PARTICIPANT,
    PARTICIPANT;

    @Override
    public String toString() {
        return name();
    }
}
