package gtms.util;

/** Kiểm tra dữ liệu nhập. Sai thì ném IllegalArgumentException kèm tên trường bị sai. */
public final class Validator {

    private Validator() { }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Thiếu thông tin bắt buộc: " + fieldName);
        }
        return value.trim();
    }

    public static int requireMin(int value, int min, String fieldName) {
        if (value < min) {
            throw new IllegalArgumentException(fieldName + " phải từ " + min + " trở lên (đang nhập " + value + ")");
        }
        return value;
    }
}
