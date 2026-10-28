package gtms.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Được ghi kết quả ngay từ SCHEDULED, không bắt buộc qua IN_PROGRESS. */
public enum MatchStatus {
    SCHEDULED("Chưa diễn ra"),
    IN_PROGRESS("Đang diễn ra"),
    FINISHED("Hoàn thành"),
    WALKOVER("Xử thua"),
    CANCELLED("Đã hủy");

    private final String label;

    MatchStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

    public boolean canTransitionTo(MatchStatus next) {
        switch (this) {
            case SCHEDULED:   return next == IN_PROGRESS || next == FINISHED
                                     || next == WALKOVER || next == CANCELLED;
            case IN_PROGRESS: return next == FINISHED || next == WALKOVER;
            case FINISHED:    return next == FINISHED; // sửa kết quả đã ghi
            default:          return false;
        }
    }

    /** Các trạng thái được chuyển tới từ trạng thái này, để ghi vào thông báo lỗi. */
    public String nextOptions() {
        String labels = Arrays.stream(values()).filter(s -> s != this && canTransitionTo(s))
                .map(s -> "\"" + s.getLabel() + "\"").collect(Collectors.joining(", "));
        return labels.isEmpty() ? "Đây là trạng thái cuối, không chuyển tiếp được nữa."
                : "Từ trạng thái này chỉ chuyển được sang: " + labels + ".";
    }

    /** Trận có tỉ số để tính bảng xếp hạng (kể cả xử thua). */
    public boolean isPlayed() {
        return this == FINISHED || this == WALKOVER;
    }
}
