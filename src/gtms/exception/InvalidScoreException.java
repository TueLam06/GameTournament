package gtms.exception;

/** Tỉ số để trống, âm, hoặc hòa ở thể thức loại trực tiếp. */
public class InvalidScoreException extends TournamentException {
    private static final long serialVersionUID = 1L;

    public InvalidScoreException(String message) {
        super(message);
    }
}
