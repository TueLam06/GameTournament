package gtms.exception;

/** Lớp cha của mọi lỗi vi phạm luật giải; giao diện chỉ cần bắt kiểu này. */
public class TournamentException extends Exception {
    private static final long serialVersionUID = 1L;

    public TournamentException(String message) {
        super(message);
    }

    /** Tên loại lỗi hiển thị cho người dùng; lớp con ghi đè để phân loại rõ hơn. */
    public String getKind() {
        return "Lỗi nghiệp vụ";
    }
}
