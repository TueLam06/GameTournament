package gtms.exception;

/** Chuyển trạng thái không được phép (xem canTransitionTo() trong các enum trạng thái). */
public class InvalidStatusTransitionException extends TournamentException {
    private static final long serialVersionUID = 1L;

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
