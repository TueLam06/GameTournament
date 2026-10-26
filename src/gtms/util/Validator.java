package gtms.util;

import java.util.regex.Pattern;

/** Kiểm tra dữ liệu nhập. Sai thì ném IllegalArgumentException kèm tên trường bị sai. */
public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Validator() { }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Thiếu thông tin bắt buộc: " + fieldName);
        }
        return value.trim();
    }

    public static String requireEmail(String email) {
        String value = requireText(email, "email");
        if (!EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("Email không đúng định dạng: " + value);
        }
        return value.toLowerCase();
    }

    public static int requireMin(int value, int min, String fieldName) {
        if (value < min) {
            throw new IllegalArgumentException(fieldName + " phải >= " + min + " (nhận " + value + ")");
        }
        return value;
    }
}
