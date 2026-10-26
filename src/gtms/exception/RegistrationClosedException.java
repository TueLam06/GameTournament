package gtms.exception;

/** Đăng ký hoặc hủy đơn khi giải không mở đăng ký, đã quá hạn hoặc đơn đã khóa. */
public class RegistrationClosedException extends TournamentException {
    private static final long serialVersionUID = 1L;

    public RegistrationClosedException(String message) {
        super(message);
    }
}
