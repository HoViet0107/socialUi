package personal.social.enums;

public enum MediaType {
    IMAGE, VIDEO, AUDIO, FILE, GIF;

    @Override
    public String toString() {
        return name();
    }
}
