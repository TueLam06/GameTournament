package gtms.model;

import java.util.Arrays;
import java.util.stream.Collectors;

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
            // Admin rút lại duyệt khi còn mở đăng ký, ví dụ để loại trực tiếp đủ 2, 4, 8... đội
            case APPROVED: return next == LOCKED || next == REJECTED || next == CANCELLED;
            case LOCKED:   return next == CANCELLED; // chỉ xảy ra khi cả giải bị hủy
            default:       return false;
        }
    }

    /** Các trạng thái được chuyển tới từ trạng thái này, để ghi vào thông báo lỗi. */
    public String nextOptions() {
        String labels = Arrays.stream(values()).filter(s -> s != this && canTransitionTo(s))
                .map(s -> "\"" + s.getLabel() + "\"").collect(Collectors.joining(", "));
        return labels.isEmpty() ? "Đây là trạng thái cuối, không chuyển tiếp được nữa."
                : "Từ trạng thái này chỉ chuyển được sang: " + labels + ".";
    }
}
