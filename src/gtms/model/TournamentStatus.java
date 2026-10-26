package gtms.model;

public enum TournamentStatus {
    DRAFT("Nháp"),
    REGISTRATION_OPEN("Đang mở đăng ký"),
    REGISTRATION_CLOSED("Đã đóng đăng ký"),
    ONGOING("Đang diễn ra"),
    FINISHED("Đã kết thúc"),
    CANCELLED("Đã hủy");

    private final String label;

    TournamentStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

    public boolean canTransitionTo(TournamentStatus next) {
        switch (this) {
            case DRAFT:               return next == REGISTRATION_OPEN || next == CANCELLED;
            case REGISTRATION_OPEN:   return next == REGISTRATION_CLOSED || next == CANCELLED;
            case REGISTRATION_CLOSED: return next == ONGOING || next == CANCELLED;
            case ONGOING:             return next == FINISHED;
            default:                  return false; // FINISHED, CANCELLED không chuyển tiếp được
        }
    }
}
