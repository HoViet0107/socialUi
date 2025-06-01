package personal.social.enums;

public enum ParticipantStatus {
    ACTIVE,     // đang là thành viên nhóm
    LEFT,       // tự rời nhóm
    REMOVED,    // bị xóa bởi admin
    INVITED;     // được mời nhưng chưa tham gia

    @Override
    public String toString() {
        return name();
    }
}
