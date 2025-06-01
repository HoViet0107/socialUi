package personal.social.enums;

public enum ReactionType {
    LIKE, UNLIKE, DISLIKE, UNDISLIKE, REPORT, UNREPORT, SHARE, UNSHARE;
    @Override
    public String toString() {
        return name();
    }
}
