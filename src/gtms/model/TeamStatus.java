package gtms.model;

/** Trạng thái đội. Việc đội đang dự giải nào được suy ra từ Registration. */
public enum TeamStatus {
    ACTIVE("Đang hoạt động"),
    DISBANDED("Đã giải thể");

    private final String label;

    TeamStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
