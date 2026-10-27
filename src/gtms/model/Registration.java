package gtms.model;

import gtms.exception.InvalidStatusTransitionException;
import gtms.exception.RegistrationClosedException;
import gtms.util.AppClock;
import gtms.util.Validator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Registration implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String registrationId;
    private final Team team;
    private final Tournament tournament;
    private final LocalDateTime registrationDate;
    private RegistrationStatus status = RegistrationStatus.PENDING;
    private String rejectionReason;
    private final List<Player> registeredPlayers;
    private Integer waitingPosition; // khác null: đơn đang nằm trong danh sách chờ

    public Registration(String registrationId, Team team, Tournament tournament, List<Player> roster) {
        if (team == null || tournament == null || roster == null) {
            throw new IllegalArgumentException("Đơn đăng ký phải có đội, giải và danh sách thi đấu");
        }
        this.registrationId = Validator.requireText(registrationId, "registrationId");
        this.team = team;
        this.tournament = tournament;
        this.registeredPlayers = new ArrayList<>(roster);
        this.registrationDate = AppClock.now();
    }

    /** Đơn còn trong danh sách chờ thì chưa có slot nên chưa duyệt được. */
    public void approve() throws InvalidStatusTransitionException {
        if (waitingPosition != null) {
            throw new InvalidStatusTransitionException("Đơn " + registrationId
                    + " đang ở vị trí chờ số " + waitingPosition + ", giải chưa còn slot");
        }
        transitionTo(RegistrationStatus.APPROVED);
    }

    public void reject(String reason) throws InvalidStatusTransitionException {
        String checkedReason = Validator.requireText(reason, "lý do từ chối");
        boolean heldSlot = holdsSlot();
        transitionTo(RegistrationStatus.REJECTED);
        rejectionReason = checkedReason;
        waitingPosition = null;
        tournament.onRegistrationReleased(heldSlot);
    }

    /** Chỉ hủy được khi giải còn mở đăng ký và đơn chưa bị khóa. */
    public void cancel() throws RegistrationClosedException, InvalidStatusTransitionException {
        if (status == RegistrationStatus.LOCKED || !tournament.isRegistrationWindowOpen()) {
            throw new RegistrationClosedException("Không thể hủy đơn " + registrationId
                    + ": cổng đăng ký đã đóng hoặc danh sách đã khóa");
        }
        boolean heldSlot = holdsSlot();
        transitionTo(RegistrationStatus.CANCELLED);
        waitingPosition = null;
        tournament.onRegistrationReleased(heldSlot);
    }

    /** Tournament gọi khi giải bị hủy; không cần kiểm tra hạn đăng ký như cancel(). */
    void cancelByTournament() throws InvalidStatusTransitionException {
        transitionTo(RegistrationStatus.CANCELLED);
        waitingPosition = null;
    }

    public void lock() throws InvalidStatusTransitionException {
        transitionTo(RegistrationStatus.LOCKED);
    }

    private void transitionTo(RegistrationStatus next) throws InvalidStatusTransitionException {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException("Đơn " + registrationId + " không thể chuyển từ "
                    + status.getLabel() + " sang " + next.getLabel());
        }
        status = next;
    }

    /** Đơn đang chiếm một trong maxTeams slot (đơn trong danh sách chờ thì không). */
    public boolean holdsSlot() {
        return status == RegistrationStatus.APPROVED || status == RegistrationStatus.LOCKED
                || (status == RegistrationStatus.PENDING && waitingPosition == null);
    }

    public boolean isActive() {
        return status == RegistrationStatus.PENDING || status == RegistrationStatus.APPROVED
                || status == RegistrationStatus.LOCKED;
    }

    void setWaitingPosition(Integer position) {
        this.waitingPosition = position;
    }

    @Override public String getId() { return registrationId; }
    public String getRegistrationId() { return registrationId; }
    public Team getTeam() { return team; }
    public Tournament getTournament() { return tournament; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public RegistrationStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Integer getWaitingPosition() { return waitingPosition; }
    public List<Player> getRegisteredPlayers() { return Collections.unmodifiableList(registeredPlayers); }

    @Override
    public String toString() {
        String extra = waitingPosition != null ? " (chờ số " + waitingPosition + ")"
                : rejectionReason != null ? " - " + rejectionReason : "";
        return String.format("%-7s %-18s -> %-8s %s%s", registrationId, team.getTeamName(),
                tournament.getTournamentId(), status.getLabel(), extra);
    }
}
