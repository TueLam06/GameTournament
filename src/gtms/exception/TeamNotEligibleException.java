package gtms.exception;

/** Đội không đủ điều kiện dự giải: thiếu người, sai game, đội đã giải thể, đăng ký trùng. */
public class TeamNotEligibleException extends TournamentException {
    private static final long serialVersionUID = 1L;

    public TeamNotEligibleException(String message) {
        super(message);
    }

    @Override
    public String getKind() {
        return "Đội chưa đủ điều kiện";
    }
}
