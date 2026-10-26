package gtms.app;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Đọc dữ liệu từ bàn phím; nhập sai định dạng thì báo lỗi và hỏi lại thay vì làm dừng chương trình. */
public class ConsoleInput {
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String text(String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "0";
    }

    public int integer(String prompt) {
        while (true) {
            String raw = text(prompt);
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("  ! \"" + raw + "\" không phải số nguyên, vui lòng nhập lại.");
            }
        }
    }

    /** Cho phép để trống: trả về null để tầng nghiệp vụ báo InvalidScoreException. */
    public Integer optionalInteger(String prompt) {
        while (true) {
            String raw = text(prompt);
            if (raw.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("  ! \"" + raw + "\" không phải số nguyên, vui lòng nhập lại.");
            }
        }
    }

    public LocalDateTime dateTime(String prompt) {
        while (true) {
            String raw = text(prompt + " (dd/MM/yyyy HH:mm): ");
            try {
                return LocalDateTime.parse(raw, DATE_TIME);
            } catch (DateTimeParseException e) {
                System.out.println("  ! Sai định dạng ngày giờ, ví dụ đúng: 01/11/2026 19:00");
            }
        }
    }

    public boolean confirm(String prompt) {
        return text(prompt + " (y/n): ").equalsIgnoreCase("y");
    }
}
