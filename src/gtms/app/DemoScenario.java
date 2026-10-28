package gtms.app;

import gtms.exception.TournamentException;
import gtms.model.Admin;
import gtms.model.Match;
import gtms.model.Player;
import gtms.model.Registration;
import gtms.model.Team;
import gtms.model.TeamManager;
import gtms.model.Tournament;
import gtms.model.TournamentFormat;
import gtms.model.User;
import gtms.repository.DataStore;
import gtms.service.TournamentService;
import gtms.util.AppClock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Kịch bản tự động đi qua các use case chính, gồm cả các luồng ngoại lệ. */
public final class DemoScenario {

    @FunctionalInterface
    private interface Step {
        void run() throws Exception;
    }

    private DemoScenario() { }

    public static void run() throws Exception {
        AppClock.setFixed(LocalDateTime.of(2026, 11, 1, 10, 0));
        DataStore store = SampleData.create();
        TournamentService service = new TournamentService(store);
        Admin admin = (Admin) service.login("admin@ptit.edu.vn", SampleData.DEFAULT_PASSWORD);
        List<TeamManager> managers = store.users().findAllOfType(TeamManager.class);
        List<Team> teams = store.teams().findAll();

        Printer.title("1. Tài khoản và phân quyền (UC-01, UC-02, UC-16)");
        expectError("Đăng ký mật khẩu yếu", () -> service.registerAccount("An", "an@gmail.com", "123"));
        expectError("Đăng ký trùng email", () -> service.registerAccount("An", "captain1@gmail.com", "Abcd1234"));
        expectError("Đăng nhập sai mật khẩu", () -> service.login("captain1@gmail.com", "sai-mat-khau1"));
        TeamManager newbie = service.registerAccount("Nguyễn Văn Mới", "newbie@gmail.com", "Newbie2026");
        System.out.println("Tạo tài khoản: " + newbie);
        admin.lockUser(newbie);
        expectError("Đăng nhập tài khoản đã bị khóa", () -> service.login("newbie@gmail.com", "Newbie2026"));
        admin.unlockUser(newbie);
        User again = service.login("newbie@gmail.com", "Newbie2026");
        System.out.println("Mở khóa và đăng nhập lại thành công: " + again.getName() + " / " + again.getRole());

        Printer.title("2. Đội và tuyển thủ (UC-05)");
        Team small = service.createTeam(newbie, "Newbie Squad", "", "Hà Nội", SampleData.GAME);
        for (int i = 1; i <= 3; i++) {
            service.addNewPlayer(newbie, "NEW_" + i, "Tân binh " + i, "Flex", "VAL#900" + i);
        }
        Printer.team(small);
        Player star = teams.get(0).getPlayers().get(0);
        expectError("Thêm tuyển thủ đang thuộc đội khác", () -> newbie.addPlayer(star));
        expectError("Tạo đội trùng tên", () -> service.createTeam(managers.get(0), "PTIT Phoenix", "", "HN", "Valorant"));
        expectError("Thiếu nickname tuyển thủ", () -> service.addNewPlayer(newbie, " ", "Tên", "Flex", "VAL#1"));

        Tournament cup = service.searchTournaments("valorant cup", null, null).get(0);
        Printer.title("3. Đăng ký giải " + cup.getName() + " (UC-06, UC-07, UC-08, UC-10)");
        List<Registration> regs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            regs.add(managers.get(i).registerTournament(cup));
        }
        Printer.registrations(cup.getRegistrations());
        expectError("Đội chỉ có 3 tuyển thủ đăng ký", () -> newbie.registerTournament(cup));
        expectError("Đăng ký trùng giải", () -> managers.get(0).registerTournament(cup));
        expectError("Đăng ký khi giải đã đủ 4 đội", () -> managers.get(4).registerTournament(cup));
        admin.approveRegistration(regs.get(0));
        admin.approveRegistration(regs.get(2));
        admin.approveRegistration(regs.get(3));
        managers.get(1).cancelRegistration(regs.get(1));
        System.out.println("Đội 2 hủy đơn -> giải trống 1 slot: " + regs.get(1));
        Registration late = managers.get(5).registerTournament(cup);
        admin.rejectRegistration(late, "Đội chưa nộp hồ sơ tuyển thủ");
        expectError("Hủy đơn đã bị từ chối", () -> managers.get(5).cancelRegistration(late));
        Registration fifth = managers.get(4).registerTournament(cup);
        admin.approveRegistration(fifth);
        System.out.println("Đơn bị từ chối trả lại slot, đội 5 đăng ký được: " + fifth);
        Printer.registrations(cup.getRegistrations());

        Printer.title("4. Đóng đăng ký và sinh lịch vòng tròn (UC-11)");
        expectError("Sinh lịch khi chưa đóng đăng ký", () -> admin.generateSchedule(cup));
        admin.closeRegistration(cup);
        expectError("Đăng ký sau khi đã đóng cổng", () -> managers.get(1).registerTournament(cup));
        admin.generateSchedule(cup);
        Printer.schedule(cup);
        expectError("Sinh lịch lần hai không xác nhận", () -> admin.generateSchedule(cup));

        Printer.title("5. Nhập kết quả và cập nhật BXH (UC-12, UC-13)");
        List<Match> rr = cup.getMatches();
        expectError("Nhập tỉ số âm", () -> admin.updateMatchResult(rr.get(0), -1, 2));
        expectError("Để trống tỉ số", () -> admin.updateMatchResult(rr.get(0), null, 2));
        int[][] scores = {{13, 7}, {13, 11}, {9, 13}, {13, 13}, {13, 5}, {6, 13}};
        for (int i = 0; i < rr.size(); i++) {
            admin.updateMatchResult(rr.get(i), scores[i][0], scores[i][1]);
        }
        Printer.standings(cup);
        System.out.println("Sửa kết quả trận " + rr.get(3).getMatchId() + " từ 13-13 thành 13-10 -> tính lại toàn bộ BXH");
        admin.updateMatchResult(rr.get(3), 13, 10);
        Printer.tournamentDetail(cup);

        Printer.title("6. Giải loại trực tiếp 4 đội: nextMatch, walkover");
        LocalDateTime now = AppClock.now();
        expectError("Tạo giải loại trực tiếp tối đa 6 đội", () -> service.createTournament(admin, "Lỗi",
                SampleData.GAME, TournamentFormat.SINGLE_ELIMINATION, 6, 5, now.minusDays(1), now.plusDays(2), ""));
        Tournament ko = service.createTournament(admin, "PTIT Knockout Mini", SampleData.GAME,
                TournamentFormat.SINGLE_ELIMINATION, 4, 5, now.minusDays(1), now.plusDays(2), "Cúp lưu niệm");
        admin.openRegistration(ko);
        for (int i = 0; i < 4; i++) {
            admin.approveRegistration(managers.get(i).registerTournament(ko));
        }
        admin.closeRegistration(ko);
        admin.generateSchedule(ko);
        Printer.schedule(ko);
        List<Match> round1 = ko.getMatchesByRound().get(1);
        Match finalMatch = ko.getMatchesByRound().get(2).get(0);
        expectError("Tỉ số hòa ở thể thức loại trực tiếp", () -> admin.updateMatchResult(round1.get(0), 1, 1));
        expectError("Nhập kết quả chung kết khi chưa đủ đội", () -> admin.updateMatchResult(finalMatch, 2, 0));
        admin.updateMatchResult(round1.get(0), 2, 1);
        expectError("Sửa kết quả trận loại trực tiếp", () -> admin.updateMatchResult(round1.get(0), 0, 2));
        admin.recordWalkover(round1.get(1), round1.get(1).getTeamA());
        admin.updateMatchResult(finalMatch, 1, 2);
        Printer.tournamentDetail(ko);

        Printer.title("7. Lưu/nạp file, xuất CSV, tìm kiếm (NFR-07, FR-03, FR-15)");
        Path dataFile = Paths.get("data", "demo.dat");
        store.save(dataFile);
        DataStore loaded = DataStore.load(dataFile);
        Tournament reloaded = loaded.tournaments().findById(cup.getTournamentId()).orElseThrow();
        System.out.println("Đã lưu " + Files.size(dataFile) + " byte và nạp lại: " + loaded.users().size()
                + " tài khoản, " + loaded.teams().size() + " đội, " + loaded.tournaments().size() + " giải");
        System.out.println("BXH sau khi nạp lại, đội đầu bảng: "
                + reloaded.getRanking().getStandings().get(0).getTeam().getTeamName());
        Path csv = service.exportRankingCsv(cup, Paths.get("data", "ranking_" + cup.getTournamentId() + ".csv"));
        System.out.println("Đã xuất BXH ra " + csv);
        System.out.println("Lịch sử thi đấu của " + teams.get(0).getTeamName() + ": "
                + service.getTeamSummary(teams.get(0)));
        service.getTeamHistory(teams.get(0)).forEach(m -> System.out.println("   " + m));

        Printer.title("8. Sửa, xóa giải và sửa đội (FR-05, FR-06, FR-09)");
        expectError("Xóa giải đã kết thúc", () -> service.deleteTournament(admin, cup));
        Tournament draft = service.searchTournaments("Knockout Series", null, null).get(0);
        admin.updateTournamentSettings(draft, TournamentFormat.ROUND_ROBIN, 6, 5, now, now.plusDays(5));
        System.out.println("Sửa cấu hình giải Nháp: " + draft);
        service.deleteTournament(admin, draft);
        System.out.println("Đã xóa giải Nháp " + draft.getTournamentId() + ", còn " + store.tournaments().size() + " giải");
        expectError("Đổi tên đội trùng đội khác", () -> service.updateTeam(newbie, "Hanoi Wolves", "", "Hà Nội"));
        // lấy mã từ đối tượng: mã sinh ra phụ thuộc dữ liệu đã tạo trước đó (ví dụ khi chạy demo trong GUI)
        String firstNewbieId = small.getPlayers().get(0).getPlayerId();
        newbie.updatePlayer(firstNewbieId, "NEW_ACE", "Duelist");
        service.disbandTeam(newbie);
        System.out.println("Sửa tuyển thủ " + firstNewbieId + " thành NEW_ACE rồi giải thể Newbie Squad: " + small);
        System.out.println("Thống kê giải theo trạng thái: " + service.countByStatus());
        expectError("Tạo giải có hạn đóng trước ngày mở", () -> service.createTournament(admin, "Lỗi", "Valorant",
                TournamentFormat.ROUND_ROBIN, 4, 5, now, now.minusDays(1), ""));
        AppClock.useSystemClock();
    }

    /** Chạy một bước dự kiến lỗi và in ra ngoại lệ bắt được. */
    private static void expectError(String description, Step step) throws Exception {
        try {
            step.run();
            System.out.println("[KHÔNG LỖI?] " + description);
        } catch (TournamentException e) {
            printCaught(description, e.getKind(), e);
        } catch (IllegalArgumentException e) {
            printCaught(description, "Dữ liệu không hợp lệ", e);
        }
    }

    /** Giữ tên lớp ngoại lệ trong ngoặc để khi thuyết trình chỉ ra được cây ngoại lệ. */
    private static void printCaught(String description, String kind, Exception e) {
        System.out.printf("[OK] %-42s -> %s (%s): %s%n", description, kind, e.getClass().getSimpleName(),
                e.getMessage());
    }
}
