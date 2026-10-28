package gtms.model;

import gtms.exception.InvalidStatusTransitionException;
import gtms.exception.RegistrationClosedException;
import gtms.util.AppClock;
import gtms.util.Validator;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Đơn đăng ký cho cả đội, không chọn riêng danh sách thi đấu. */
public class Registration implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String registrationId;
    private final Team team;
    private final Tournament tournament;
    private final LocalDateTime registrationDate;
    private RegistrationStatus status = RegistrationStatus.PENDING;
    private String rejectionReason;

    public Registration(String registrationId, Team team, Tournament tournament) {
        if (team == null || tournament == null) {
            throw new IllegalArgumentException("Đơn đăng ký phải có đội và giải");
        }
        this.registrationId = Validator.requireText(registrationId, "registrationId");
        this.team = team;
        this.tournament = tournament;
        this.registrationDate = AppClock.now();
    }

    public void approve() throws InvalidStatusTransitionException {
        transitionTo(RegistrationStatus.APPROVED);
    }

    public void reject(String reason) throws InvalidStatusTransitionException {
        String checkedReason = Validator.requireText(reason, "lý do từ chối");
        transitionTo(RegistrationStatus.REJECTED);
        rejectionReason = checkedReason;
    }

    /** Chỉ hủy được khi giải còn mở đăng ký và đơn chưa bị khóa. */
    public void cancel() throws RegistrationClosedException, InvalidStatusTransitionException {
        if (status == RegistrationStatus.LOCKED || !tournament.isRegistrationWindowOpen()) {
            throw new RegistrationClosedException("Không thể hủy đơn " + registrationId
                    + ": cổng đăng ký đã đóng hoặc danh sách đã khóa");
        }
        transitionTo(RegistrationStatus.CANCELLED);
    }

    /** Tournament gọi khi giải bị hủy; không cần kiểm tra hạn đăng ký như cancel(). */
    void cancelByTournament() throws InvalidStatusTransitionException {
        transitionTo(RegistrationStatus.CANCELLED);
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

    /** Đơn còn hiệu lực thì chiếm một trong maxTeams slot của giải. */
    public boolean isActive() {
        return status == RegistrationStatus.PENDING || status == RegistrationStatus.APPROVED
                || status == RegistrationStatus.LOCKED;
    }

    @Override public String getId() { return registrationId; }
    public String getRegistrationId() { return registrationId; }
    public Team getTeam() { return team; }
    public Tournament getTournament() { return tournament; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public RegistrationStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }

    @Override
    public String toString() {
        String extra = rejectionReason != null ? " - " + rejectionReason : "";
        return String.format("%-7s %-18s -> %-8s %s%s", registrationId, team.getTeamName(),
                tournament.getTournamentId(), status.getLabel(), extra);
    }
}
