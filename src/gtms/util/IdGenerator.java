package gtms.util;

import java.util.HashMap;
import java.util.Map;

/** Sinh mã định danh dạng tiền tố + số thứ tự, ví dụ TM001, TOUR002, REG015. */
public final class IdGenerator {
    private static final Map<String, Integer> counters = new HashMap<>();

    private IdGenerator() { }

    public static synchronized String next(String prefix) {
        int value = counters.merge(prefix, 1, Integer::sum);
        return String.format("%s%03d", prefix, value);
    }

    /** Bản sao bộ đếm để lưu xuống file cùng dữ liệu. */
    public static synchronized Map<String, Integer> snapshot() {
        return new HashMap<>(counters);
    }

    /** Khôi phục bộ đếm sau khi nạp dữ liệu, tránh sinh trùng mã. */
    public static synchronized void restore(Map<String, Integer> saved) {
        counters.clear();
        counters.putAll(saved);
    }
}
