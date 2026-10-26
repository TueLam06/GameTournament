package gtms.model;

import gtms.strategy.RoundRobinStrategy;
import gtms.strategy.ScheduleStrategy;
import gtms.strategy.SingleEliminationStrategy;

/** Các lớp sinh lịch không lưu dữ liệu gì, nên mỗi thể thức chỉ cần một đối tượng dùng chung. */
public enum TournamentFormat {
    ROUND_ROBIN("Vòng tròn", new RoundRobinStrategy(), true),
    SINGLE_ELIMINATION("Loại trực tiếp", new SingleEliminationStrategy(), false);

    private final String label;
    private final ScheduleStrategy strategy;
    private final boolean drawAllowed;

    TournamentFormat(String label, ScheduleStrategy strategy, boolean drawAllowed) {
        this.label = label;
        this.strategy = strategy;
        this.drawAllowed = drawAllowed;
    }

    public String getLabel() { return label; }
    public ScheduleStrategy getStrategy() { return strategy; }
    public boolean isDrawAllowed() { return drawAllowed; }
}
