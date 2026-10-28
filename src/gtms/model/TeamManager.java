package gtms.model;

import gtms.exception.TournamentException;
import gtms.util.IdGenerator;

import java.util.List;

/** Mỗi đội trưởng chỉ quản lý một đội còn hoạt động. */
public class TeamManager extends User {
    private static final long serialVersionUID = 1L;

    private Team managedTeam;

    public TeamManager(String userId, String name, String email, String rawPassword) {
        super(userId, name, email, rawPassword);
    }

    public Team createTeam(String teamName, String logo, String region, String game) throws TournamentException {
        ensureActive();
        if (hasActiveTeam()) {
            throw new TournamentException(getName() + " đã quản lý đội " + managedTeam.getTeamName());
        }
        managedTeam = new Team(IdGenerator.next("TEAM"), teamName, logo, region, game);
        return managedTeam;
    }

    public void updateTeam(String teamName, String logo, String region) throws TournamentException {
        ensureActive();
        requireTeam().updateInfo(teamName, logo, region);
    }

    public void addPlayer(Player player) throws TournamentException {
        ensureActive();
        requireTeam().addPlayer(player);
    }

    public void updatePlayer(String playerId, String nickname, String position) throws TournamentException {
        ensureActive();
        Team team = requireTeam();
        team.findPlayer(playerId)
                .orElseThrow(() -> new TournamentException("Không có tuyển thủ " + playerId + " trong đội " + team.getTeamName()))
                .updateInfo(nickname, position);
    }

    public Player removePlayer(String playerId) throws TournamentException {
        ensureActive();
        return requireTeam().removePlayer(playerId);
    }

    /**
     * Đăng ký cả đội vào giải. Ném RegistrationClosedException nếu giải không mở hoặc đã đủ đội,
     * TeamNotEligibleException nếu đội không đủ điều kiện.
     */
    public Registration registerTournament(Tournament tournament) throws TournamentException {
        ensureActive();
        Team team = requireTeam();
        Registration registration = new Registration(IdGenerator.next("REG"), team, tournament);
        tournament.addRegistration(registration);
        return registration;
    }

    public void cancelRegistration(Registration registration)
            throws TournamentException {
        ensureActive();
        if (registration.getTeam() != managedTeam) {
            throw new TournamentException("Chỉ được hủy đơn của đội mình quản lý");
        }
        registration.cancel();
    }

    public void disbandTeam(List<Tournament> tournaments) throws TournamentException {
        ensureActive();
        Team team = requireTeam();
        boolean stillPlaying = tournaments.stream()
                .anyMatch(t -> t.findActiveRegistration(team).isPresent()
                        && t.getStatus() != TournamentStatus.FINISHED
                        && t.getStatus() != TournamentStatus.CANCELLED);
        if (stillPlaying) {
            throw new TournamentException("Đội đang tham gia giải, không thể giải thể");
        }
        team.disband();
        managedTeam = null;
    }

    private Team requireTeam() throws TournamentException {
        if (!hasActiveTeam()) {
            throw new TournamentException(getName() + " chưa có đội, hãy tạo đội trước");
        }
        return managedTeam;
    }

    public boolean hasActiveTeam() {
        return managedTeam != null && managedTeam.getStatus() == TeamStatus.ACTIVE;
    }

    public Team getManagedTeam() { return managedTeam; }

    @Override
    public String getRole() { return "TeamManager"; }

    @Override
    public void showMenu() {
        System.out.println("===== MENU ĐỘI TRƯỞNG: " + getName() + " =====");
        System.out.println(" 1. Xem/tìm giải đấu            2. Xem chi tiết giải (lịch, BXH)");
        System.out.println(" 3. Tạo đội                     4. Xem đội của tôi");
        System.out.println(" 5. Sửa thông tin đội           6. Giải thể đội");
        System.out.println(" 7. Thêm tuyển thủ              8. Sửa tuyển thủ");
        System.out.println(" 9. Xóa tuyển thủ              10. Đăng ký giải đấu");
        System.out.println("11. Hủy đăng ký                12. Lịch sử và thành tích đội");
        System.out.println(" 0. Đăng xuất");
    }
}
