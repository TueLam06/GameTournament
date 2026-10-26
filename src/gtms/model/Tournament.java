package gtms.model;

import gtms.exception.InvalidStatusTransitionException;
import gtms.exception.RegistrationClosedException;
import gtms.exception.TeamNotEligibleException;
import gtms.exception.TournamentException;
import gtms.strategy.DefaultRankingCalculator;
import gtms.strategy.ScheduleStrategy;
import gtms.util.AppClock;
import gtms.util.Validator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Cách sinh lịch nằm ở ScheduleStrategy, nên thêm thể thức mới không phải sửa lớp này. */
public class Tournament implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String tournamentId;
    private String name;
    private final String game;
    private TournamentFormat format;
    private int maxTeams;
    private int teamSize;
    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private String prize;
    private TournamentStatus status = TournamentStatus.DRAFT;
    private final List<Registration> registrations = new ArrayList<>();
    private final List<Match> matches = new ArrayList<>();
    private final Ranking ranking;
    private ScheduleStrategy scheduleStrategy;

    public Tournament(String tournamentId, String name, String game, TournamentFormat format,
                      int maxTeams, int teamSize, LocalDateTime registrationStart,
                      LocalDateTime registrationEnd, String prize) {
        this.tournamentId = Validator.requireText(tournamentId, "tournamentId");
        this.name = Validator.requireText(name, "name");
        this.game = Validator.requireText(game, "game");
        this.prize = prize == null ? "" : prize.trim();
        applySettings(format, maxTeams, teamSize, registrationStart, registrationEnd);
        this.ranking = new Ranking(this, new DefaultRankingCalculator());
    }

    private void applySettings(TournamentFormat format, int maxTeams, int teamSize,
                               LocalDateTime start, LocalDateTime end) {
        if (format == null) {
            throw new IllegalArgumentException("Thiếu thông tin bắt buộc: format");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Thiếu thời gian mở/đóng đăng ký");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Thời điểm đóng đăng ký phải sau thời điểm mở đăng ký");
        }
        this.format = format;
        this.scheduleStrategy = format.getStrategy();
        this.maxTeams = Validator.requireMin(maxTeams, 2, "maxTeams");
        this.teamSize = Validator.requireMin(teamSize, 1, "teamSize");
        this.registrationStart = start;
        this.registrationEnd = end;
    }

    /** Chỉ sửa được khi giải còn Nháp, vì sau đó đã có đội đăng ký theo cấu hình cũ. */
    public void updateSettings(TournamentFormat format, int maxTeams, int teamSize,
                               LocalDateTime start, LocalDateTime end) throws InvalidStatusTransitionException {
        if (status != TournamentStatus.DRAFT) {
            throw new InvalidStatusTransitionException("Chỉ sửa thể thức/quy mô khi giải ở trạng thái Nháp");
        }
        applySettings(format, maxTeams, teamSize, start, end);
    }

    public void updateInfo(String name, String prize) {
        this.name = Validator.requireText(name, "name");
        this.prize = prize == null ? "" : prize.trim();
    }

    public boolean isRegistrationWindowOpen() {
        LocalDateTime now = AppClock.now();
        return status == TournamentStatus.REGISTRATION_OPEN
                && !now.isBefore(registrationStart) && !now.isAfter(registrationEnd);
    }

    /** Chỉ để hỏi trước, không ném ngoại lệ; addRegistration() mới là nơi báo lỗi. */
    public boolean canRegister(Team team) {
        return isRegistrationWindowOpen() && team.isEligible(this) && findActiveRegistration(team).isEmpty();
    }

    /** Hết slot thì đơn vẫn được nhận nhưng vào danh sách chờ. */
    public void addRegistration(Registration registration)
            throws RegistrationClosedException, TeamNotEligibleException {
        if (registration.getTournament() != this) {
            throw new IllegalArgumentException("Đơn đăng ký không thuộc giải " + name);
        }
        if (!isRegistrationWindowOpen()) {
            throw new RegistrationClosedException("Giải " + name + " không mở đăng ký ("
                    + status.getLabel() + ", hạn " + registrationStart + " - " + registrationEnd + ")");
        }
        checkEligibility(registration.getTeam(), registration.getRegisteredPlayers());
        if (countOccupiedSlots() >= maxTeams) {
            registration.setWaitingPosition(getWaitingList().size() + 1);
        }
        registrations.add(registration);
    }

    private void checkEligibility(Team team, List<Player> roster) throws TeamNotEligibleException {
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new TeamNotEligibleException("Đội " + team.getTeamName() + " đã giải thể");
        }
        if (!team.getGame().equalsIgnoreCase(game)) {
            throw new TeamNotEligibleException("Đội " + team.getTeamName() + " thi đấu " + team.getGame()
                    + ", giải yêu cầu " + game);
        }
        if (findActiveRegistration(team).isPresent()) {
            throw new TeamNotEligibleException("Đội " + team.getTeamName() + " đã đăng ký giải này");
        }
        if (team.getPlayers().size() < teamSize) {
            throw new TeamNotEligibleException("Đội " + team.getTeamName() + " còn thiếu "
                    + (teamSize - team.getPlayers().size()) + " tuyển thủ (yêu cầu " + teamSize + ")");
        }
        if (roster.size() != teamSize || new HashSet<>(roster).size() != roster.size()) {
            throw new TeamNotEligibleException("Danh sách thi đấu phải gồm đúng " + teamSize
                    + " tuyển thủ khác nhau (nhận " + roster.size() + ")");
        }
        for (Player p : roster) {
            if (!team.getPlayers().contains(p)) {
                throw new TeamNotEligibleException(p.getNickname() + " không thuộc đội " + team.getTeamName());
            }
        }
    }

    /** Gọi khi một đơn bị từ chối hoặc hủy: trả slot cho đơn chờ đầu tiên rồi đánh số lại hàng chờ. */
    void onRegistrationReleased(boolean heldSlot) {
        List<Registration> waiting = getWaitingList();
        if (heldSlot && status == TournamentStatus.REGISTRATION_OPEN && !waiting.isEmpty()) {
            waiting.get(0).setWaitingPosition(null);
            waiting = waiting.subList(1, waiting.size());
        }
        for (int i = 0; i < waiting.size(); i++) {
            waiting.get(i).setWaitingPosition(i + 1);
        }
    }

    public int countOccupiedSlots() {
        return (int) registrations.stream().filter(Registration::holdsSlot).count();
    }

    public List<Registration> getWaitingList() {
        return registrations.stream()
                .filter(r -> r.getStatus() == RegistrationStatus.PENDING && r.getWaitingPosition() != null)
                .sorted(Comparator.comparing(Registration::getWaitingPosition))
                .collect(Collectors.toList());
    }

    public Optional<Registration> findActiveRegistration(Team team) {
        return registrations.stream().filter(r -> r.getTeam() == team && r.isActive()).findFirst();
    }

    public Optional<Registration> findRegistration(String registrationId) {
        return registrations.stream().filter(r -> r.getRegistrationId().equals(registrationId)).findFirst();
    }

    /** Các đội có đơn đã khóa, theo thứ tự đăng ký. */
    public List<Team> getConfirmedTeams() {
        return registrations.stream()
                .filter(r -> r.getStatus() == RegistrationStatus.LOCKED)
                .map(Registration::getTeam)
                .collect(Collectors.toList());
    }

    public void changeStatus(TournamentStatus next) throws InvalidStatusTransitionException {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException("Giải " + name + " không thể chuyển từ "
                    + status.getLabel() + " sang " + next.getLabel());
        }
        if (next == TournamentStatus.ONGOING && matches.isEmpty()) {
            throw new InvalidStatusTransitionException("Phải sinh lịch thi đấu trước khi bắt đầu giải");
        }
        if (next == TournamentStatus.FINISHED && !allMatchesClosed()) {
            throw new InvalidStatusTransitionException("Giải còn trận chưa có kết quả");
        }
        status = next;
    }

    public void openRegistration() throws InvalidStatusTransitionException {
        changeStatus(TournamentStatus.REGISTRATION_OPEN);
    }

    /** Khóa các đơn đã duyệt, từ chối các đơn chưa được duyệt. */
    public void closeRegistration() throws TournamentException {
        changeStatus(TournamentStatus.REGISTRATION_CLOSED);
        for (Registration r : new ArrayList<>(registrations)) {
            if (r.getStatus() == RegistrationStatus.APPROVED) {
                r.lock();
            } else if (r.getStatus() == RegistrationStatus.PENDING) {
                r.reject("Giải đã đóng đăng ký khi đơn chưa được duyệt");
            }
        }
    }

    /** Sinh lịch, tạo bảng xếp hạng 0 điểm và chuyển giải sang ONGOING. */
    public List<Match> generateSchedule() throws TournamentException {
        if (!matches.isEmpty()) {
            throw new TournamentException("Giải đã có lịch, Admin cần xác nhận để xóa lịch cũ và sinh lại");
        }
        if (status != TournamentStatus.REGISTRATION_CLOSED) {
            throw new InvalidStatusTransitionException("Chỉ sinh lịch khi giải đã đóng đăng ký (hiện tại: "
                    + status.getLabel() + ")");
        }
        buildSchedule();
        changeStatus(TournamentStatus.ONGOING);
        return getMatches();
    }

    /** Chỉ được khi chưa trận nào có kết quả. */
    public List<Match> regenerateSchedule() throws TournamentException {
        if (status != TournamentStatus.ONGOING) {
            throw new InvalidStatusTransitionException("Chỉ sinh lại lịch khi giải đang diễn ra");
        }
        if (matches.stream().anyMatch(m -> m.getStatus().isPlayed())) {
            throw new TournamentException("Đã có trận được ghi kết quả, không thể sinh lại lịch");
        }
        matches.clear();
        buildSchedule();
        return getMatches();
    }

    private void buildSchedule() throws TournamentException {
        List<Team> teams = getConfirmedTeams();
        if (teams.size() < 2) {
            throw new TournamentException("Cần ít nhất 2 đội đã được duyệt để sinh lịch (hiện có "
                    + teams.size() + ")");
        }
        matches.addAll(scheduleStrategy.generate(this, teams));
        ranking.initialize(teams);
    }

    /** Vòng 1 vào ngày sau khi đóng đăng ký; mỗi vòng một ngày, từ 18:00, các trận cách nhau 1 giờ. */
    public LocalDateTime scheduledTimeFor(int round, int slotInRound) {
        return registrationEnd.toLocalDate().plusDays(round).atTime(18, 0).plusHours(slotInRound);
    }

    /** Match gọi sau mỗi lần ghi hoặc sửa kết quả. */
    void onMatchUpdated() {
        ranking.recalculate(matches);
        if (status == TournamentStatus.ONGOING && allMatchesClosed()) {
            status = TournamentStatus.FINISHED;
        }
    }

    private boolean allMatchesClosed() {
        return !matches.isEmpty() && matches.stream()
                .allMatch(m -> m.getStatus().isPlayed() || m.getStatus() == MatchStatus.CANCELLED);
    }

    public Map<Integer, List<Match>> getMatchesByRound() {
        return matches.stream().collect(Collectors.groupingBy(Match::getRound, TreeMap::new, Collectors.toList()));
    }

    public Optional<Match> findMatch(String matchId) {
        return matches.stream().filter(m -> m.getMatchId().equalsIgnoreCase(matchId)).findFirst();
    }

    public List<Match> getMatchesOf(Team team) {
        return matches.stream().filter(m -> m.involves(team)).collect(Collectors.toList());
    }

    /** Loại trực tiếp: đội thắng chung kết. Vòng tròn: đội đứng đầu bảng. */
    public Optional<Team> getChampion() {
        if (status != TournamentStatus.FINISHED) {
            return Optional.empty();
        }
        if (format == TournamentFormat.SINGLE_ELIMINATION) {
            return matches.stream().max(Comparator.comparingInt(Match::getRound)).flatMap(Match::getWinner);
        }
        return ranking.getStandings().stream().findFirst().map(RankingEntry::getTeam);
    }

    @Override public String getId() { return tournamentId; }
    public String getTournamentId() { return tournamentId; }
    public String getName() { return name; }
    public String getGame() { return game; }
    public TournamentFormat getFormat() { return format; }
    public int getMaxTeams() { return maxTeams; }
    public int getTeamSize() { return teamSize; }
    public LocalDateTime getRegistrationStart() { return registrationStart; }
    public LocalDateTime getRegistrationEnd() { return registrationEnd; }
    public String getPrize() { return prize; }
    public TournamentStatus getStatus() { return status; }
    public Ranking getRanking() { return ranking; }
    public ScheduleStrategy getScheduleStrategy() { return scheduleStrategy; }
    public List<Registration> getRegistrations() { return Collections.unmodifiableList(registrations); }
    public List<Match> getMatches() { return Collections.unmodifiableList(matches); }

    /** Dùng khi muốn thử thể thức khác (ví dụ Swiss) cho một giải. */
    public void setScheduleStrategy(ScheduleStrategy strategy) throws InvalidStatusTransitionException {
        if (!matches.isEmpty()) {
            throw new InvalidStatusTransitionException("Không đổi chiến lược khi giải đã có lịch");
        }
        this.scheduleStrategy = strategy;
    }

    @Override
    public String toString() {
        return String.format("%-8s %-28s %-18s %-15s %2d/%-2d đội  %s",
                tournamentId, name, game, format.getLabel(), countOccupiedSlots(), maxTeams, status.getLabel());
    }
}
