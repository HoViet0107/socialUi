package personal.social.enums;

public enum Status {
    ACTIVE, INACTIVE, DELETE;
    @Override
    public String toString() {
        return name();
    }
}
