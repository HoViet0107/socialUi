package personal.social.enums;

public enum ObjectType {
    POST, COMMENT, REPLY, FOLLOW;

    @Override
    public String toString() {
        return name();
    }
}
