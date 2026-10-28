package gtms.model;

import java.util.Arrays;
import java.util.stream.Collectors;

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

    /** Các trạng thái được chuyển tới từ trạng thái này, để ghi vào thông báo lỗi. */
    public String nextOptions() {
        String labels = Arrays.stream(values()).filter(s -> s != this && canTransitionTo(s))
                .map(s -> "\"" + s.getLabel() + "\"").collect(Collectors.joining(", "));
        return labels.isEmpty() ? "Đây là trạng thái cuối, không chuyển tiếp được nữa."
                : "Từ trạng thái này chỉ chuyển được sang: " + labels + ".";
    }
}
