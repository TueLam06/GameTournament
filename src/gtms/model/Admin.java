package gtms.model;

import gtms.exception.InvalidStatusTransitionException;
import gtms.exception.TournamentException;
import gtms.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;

/** Mỗi hàm kiểm tra tài khoản còn hoạt động rồi gọi sang giải, đơn hoặc trận được truyền vào. */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(String userId, String name, String email, String rawPassword) {
        super(userId, name, email, rawPassword);
    }

    public Tournament createTournament(String name, String game, TournamentFormat format, int maxTeams,
                                       int teamSize, LocalDateTime registrationStart,
                                       LocalDateTime registrationEnd, String prize) throws TournamentException {
        ensureActive();
        return new Tournament(IdGenerator.next("TOUR"), name, game, format, maxTeams, teamSize,
                registrationStart, registrationEnd, prize);
    }

    public void updateTournament(Tournament tournament, String name, String prize) throws TournamentException {
        ensureActive();
        tournament.updateInfo(name, prize);
    }

    /** Thể thức, quy mô và thời gian đăng ký chỉ sửa được khi giải còn Nháp. */
    public void updateTournamentSettings(Tournament tournament, TournamentFormat format, int maxTeams, int teamSize,
                                         LocalDateTime start, LocalDateTime end) throws TournamentException {
        ensureActive();
        tournament.updateSettings(format, maxTeams, teamSize, start, end);
    }

    /** Chỉ xóa được giải còn Nháp và chưa có đơn; giải đã mở thì phải hủy. */
    public void checkCanDelete(Tournament tournament) throws TournamentException {
        ensureActive();
        if (tournament.getStatus() != TournamentStatus.DRAFT || !tournament.getRegistrations().isEmpty()) {
            throw new InvalidStatusTransitionException("Chỉ xóa được giải còn Nháp và chưa có đơn đăng ký (giải "
                    + tournament.getName() + " đang ở trạng thái: " + tournament.getStatus().getLabel() + ")");
        }
    }

    public void openRegistration(Tournament tournament) throws TournamentException {
        ensureActive();
        tournament.openRegistration();
    }

    public void closeRegistration(Tournament tournament) throws TournamentException {
        ensureActive();
        tournament.closeRegistration();
    }

    public void cancelTournament(Tournament tournament) throws TournamentException {
        ensureActive();
        tournament.changeStatus(TournamentStatus.CANCELLED);
    }

    public void approveRegistration(Registration registration) throws TournamentException {
        ensureActive();
        registration.approve();
    }

    public void rejectRegistration(Registration registration, String reason) throws TournamentException {
        ensureActive();
        registration.reject(reason);
    }

    public List<Match> generateSchedule(Tournament tournament) throws TournamentException {
        ensureActive();
        return tournament.generateSchedule();
    }

    /** Xóa lịch cũ và sinh lại; chỉ được khi chưa trận nào có kết quả. */
    public List<Match> regenerateSchedule(Tournament tournament) throws TournamentException {
        ensureActive();
        return tournament.regenerateSchedule();
    }

    public void startMatch(Match match) throws TournamentException {
        ensureActive();
        match.start();
    }

    public void updateMatchResult(Match match, Integer scoreA, Integer scoreB) throws TournamentException {
        ensureActive();
        match.setResult(scoreA, scoreB);
    }

    public void recordWalkover(Match match, Team loser) throws TournamentException {
        ensureActive();
        match.setWalkover(loser);
    }

    public void lockUser(User user) throws TournamentException {
        ensureActive();
        if (user == this) {
            throw new TournamentException("Admin không thể tự khóa tài khoản của mình");
        }
        user.setAccountStatus(AccountStatus.LOCKED);
    }

    public void unlockUser(User user) throws TournamentException {
        ensureActive();
        user.setAccountStatus(AccountStatus.ACTIVE);
    }

    @Override
    public String getRole() { return "Admin"; }

    @Override
    public void showMenu() {
        System.out.println("===== MENU BAN TỔ CHỨC: " + getName() + " =====");
        System.out.println(" 1. Xem/tìm giải đấu            2. Xem chi tiết giải (lịch, BXH)");
        System.out.println(" 3. Tạo giải đấu                4. Sửa giải đấu");
        System.out.println(" 5. Xóa giải (khi còn Nháp)     6. Hủy giải");
        System.out.println(" 7. Mở đăng ký                  8. Duyệt / từ chối đơn");
        System.out.println(" 9. Đóng đăng ký               10. Sinh lịch thi đấu");
        System.out.println("11. Bắt đầu trận               12. Nhập / sửa kết quả trận");
        System.out.println("13. Xử thua (walkover)         14. Khóa / mở khóa tài khoản");
        System.out.println("15. Xuất BXH ra file CSV        0. Đăng xuất");
    }
}
