package gtms.model;

public enum AccountStatus {
    ACTIVE("Đang hoạt động"),
    LOCKED("Đã khóa");

    private final String label;

    AccountStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
