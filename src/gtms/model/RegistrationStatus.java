package gtms.model;

public enum RegistrationStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Bị từ chối"),
    LOCKED("Đã khóa"),
    CANCELLED("Đã hủy");

    private final String label;

    RegistrationStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

    public boolean canTransitionTo(RegistrationStatus next) {
        switch (this) {
            case PENDING:  return next == APPROVED || next == REJECTED || next == CANCELLED;
            case APPROVED: return next == LOCKED || next == CANCELLED;
            default:       return false;
        }
    }
}
