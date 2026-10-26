package gtms.util;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Đồng hồ dùng chung; có thể cố định thời điểm để chạy demo và kiểm thử. */
public final class AppClock {
    private static Clock clock = Clock.systemDefaultZone();

    private AppClock() { }

    public static LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public static void setFixed(LocalDateTime time) {
        ZoneId zone = ZoneId.systemDefault();
        clock = Clock.fixed(time.atZone(zone).toInstant(), zone);
    }

    public static void useSystemClock() {
        clock = Clock.systemDefaultZone();
    }
}
