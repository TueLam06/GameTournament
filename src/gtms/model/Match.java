package gtms.model;

import gtms.exception.InvalidScoreException;
import gtms.exception.InvalidStatusTransitionException;
import gtms.util.Validator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Không lưu đội thắng mà tính từ tỉ số, để hai dữ liệu không bao giờ lệch nhau.
 * nextMatch chỉ dùng ở thể thức loại trực tiếp: đội thắng được đưa vào trận đó,
 * vì vậy ở thể thức này kết quả đã ghi thì không sửa được nữa.
 */
public class Match implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /** Đội bỏ cuộc thua 0-1. */
    public static final int WALKOVER_WIN_SCORE = 1;
    public static final int WALKOVER_LOSE_SCORE = 0;

    private final Tournament tournament;
    private final String matchId;
    private final int round;
    private LocalDateTime scheduledTime;
    private Team teamA;
    private Team teamB;
    private Integer scoreA;
    private Integer scoreB;
    private MatchStatus status = MatchStatus.SCHEDULED;
    private Match nextMatch;

    public Match(Tournament tournament, String matchId, int round, LocalDateTime scheduledTime,
                 Team teamA, Team teamB) {
        if (tournament == null) {
            throw new IllegalArgumentException("Trận đấu phải thuộc một giải");
        }
        if (teamA != null && teamA == teamB) {
            throw new IllegalArgumentException("Hai vị trí của trận phải là hai đội khác nhau");
        }
        this.tournament = tournament;
        this.matchId = Validator.requireText(matchId, "matchId");
        this.round = Validator.requireMin(round, 1, "round");
        this.scheduledTime = scheduledTime;
        this.teamA = teamA;
        this.teamB = teamB;
    }

    public void start() throws InvalidStatusTransitionException {
        ensureReadyToPlay();
        transitionTo(MatchStatus.IN_PROGRESS);
    }

    /** Ghi tỉ số (vòng tròn thì sửa lại được). Kiểm tra hết rồi mới ghi, nên tỉ số sai thì trận giữ nguyên. */
    public void setResult(Integer scoreA, Integer scoreB)
            throws InvalidScoreException, InvalidStatusTransitionException {
        ensureReadyToPlay();
        if (scoreA == null || scoreB == null) {
            throw new InvalidScoreException("Tỉ số không được để trống");
        }
        if (scoreA < 0 || scoreB < 0) {
            throw new InvalidScoreException("Tỉ số phải là số nguyên không âm (nhận " + scoreA + "-" + scoreB + ")");
        }
        if (!tournament.getFormat().isDrawAllowed() && scoreA.equals(scoreB)) {
            throw new InvalidScoreException("Thể thức " + tournament.getFormat().getLabel() + " không cho phép hòa");
        }
        if (status.isPlayed() && tournament.getFormat() == TournamentFormat.SINGLE_ELIMINATION) {
            throw new InvalidStatusTransitionException("Trận " + matchId
                    + " đã có kết quả, thể thức loại trực tiếp không cho sửa");
        }
        transitionTo(MatchStatus.FINISHED);
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        moveWinnerForward();
        tournament.onMatchUpdated();
    }

    public void setWalkover(Team loser) throws InvalidStatusTransitionException {
        ensureReadyToPlay();
        if (loser != teamA && loser != teamB) {
            throw new IllegalArgumentException("Đội bị xử thua phải là một trong hai đội của trận");
        }
        transitionTo(MatchStatus.WALKOVER);
        scoreA = loser == teamA ? WALKOVER_LOSE_SCORE : WALKOVER_WIN_SCORE;
        scoreB = loser == teamB ? WALKOVER_LOSE_SCORE : WALKOVER_WIN_SCORE;
        moveWinnerForward();
        tournament.onMatchUpdated();
    }

    public void cancel() throws InvalidStatusTransitionException {
        transitionTo(MatchStatus.CANCELLED);
        tournament.onMatchUpdated();
    }

    /** Rỗng khi trận chưa đấu hoặc hòa. */
    public Optional<Team> getWinner() {
        if (!status.isPlayed() || scoreA.equals(scoreB)) {
            return Optional.empty();
        }
        return Optional.of(scoreA > scoreB ? teamA : teamB);
    }

    private void moveWinnerForward() {
        if (nextMatch != null) {
            getWinner().ifPresent(nextMatch::assignTeam);
        }
    }

    /** Điền đội vào chỗ trống đầu tiên; dùng khi đội thắng đi tiếp vào vòng sau. */
    public void assignTeam(Team team) {
        if (teamA == null) {
            teamA = team;
        } else if (teamB == null) {
            teamB = team;
        } else {
            throw new IllegalStateException("Trận " + matchId + " đã đủ hai đội");
        }
    }

    public void setNextMatch(Match nextMatch) {
        if (nextMatch == this || (nextMatch != null && nextMatch.tournament != tournament)) {
            throw new IllegalArgumentException("nextMatch phải là một trận khác của cùng giải");
        }
        this.nextMatch = nextMatch;
    }

    private void ensureReadyToPlay() throws InvalidStatusTransitionException {
        if (teamA == null || teamB == null) {
            throw new InvalidStatusTransitionException("Trận " + matchId + " chưa xác định đủ hai đội");
        }
        TournamentStatus ts = tournament.getStatus();
        if (ts != TournamentStatus.ONGOING && ts != TournamentStatus.FINISHED) {
            throw new InvalidStatusTransitionException("Giải " + tournament.getName()
                    + " không ở trạng thái thi đấu (" + ts.getLabel() + ")");
        }
    }

    private void transitionTo(MatchStatus next) throws InvalidStatusTransitionException {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException("Trận " + matchId + " không thể chuyển từ "
                    + status.getLabel() + " sang " + next.getLabel());
        }
        status = next;
    }

    public boolean involves(Team team) {
        return team != null && (team == teamA || team == teamB);
    }

    public int scoreOf(Team team) {
        return team == teamA ? scoreA : scoreB;
    }

    @Override public String getId() { return matchId; }
    public Tournament getTournament() { return tournament; }
    public String getMatchId() { return matchId; }
    public int getRound() { return round; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public Team getTeamA() { return teamA; }
    public Team getTeamB() { return teamB; }
    public Integer getScoreA() { return scoreA; }
    public Integer getScoreB() { return scoreB; }
    public MatchStatus getStatus() { return status; }
    public Match getNextMatch() { return nextMatch; }

    @Override
    public String toString() {
        String a = teamA == null ? "(chờ)" : teamA.getTeamName();
        String b = teamB == null ? "(chờ)" : teamB.getTeamName();
        String score = status.isPlayed() ? scoreA + " - " + scoreB : "  vs  ";
        String time = scheduledTime == null ? "" : scheduledTime.format(TIME_FORMAT);
        return String.format("%-10s V%-2d %-11s %18s %7s %-18s %s", matchId, round, time, a, score, b,
                status.getLabel());
    }
}
