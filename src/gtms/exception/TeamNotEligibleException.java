package gtms.exception;

/** Đội không đủ điều kiện dự giải: thiếu người, sai game, danh sách thi đấu sai, đăng ký trùng. */
public class TeamNotEligibleException extends TournamentException {
    private static final long serialVersionUID = 1L;

    public TeamNotEligibleException(String message) {
        super(message);
    }
}
