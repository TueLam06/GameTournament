package gtms.app;

import gtms.exception.TournamentException;
import gtms.model.Admin;
import gtms.model.Match;
import gtms.model.MatchStatus;
import gtms.model.Player;
import gtms.model.Registration;
import gtms.model.Team;
import gtms.model.TeamManager;
import gtms.model.Tournament;
import gtms.model.TournamentFormat;
import gtms.model.TournamentStatus;
import gtms.model.User;
import gtms.repository.DataStore;
import gtms.service.TournamentService;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Chương trình console. Chạy "demo" để xem kịch bản tự động, "test" để chạy kiểm thử. */
public class ConsoleApp {
    private static final Path DATA_FILE = Paths.get("data", "gtms.dat");

    /** Giống Runnable nhưng được phép ném ngoại lệ checked. */
    @FunctionalInterface
    private interface Action {
        void run() throws TournamentException, IOException;
    }

    private final TournamentService service;
    private final ConsoleInput in;

    public ConsoleApp(TournamentService service, ConsoleInput in) {
        this.service = service;
        this.in = in;
    }

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        String mode = args.length > 0 ? args[0] : "";
        if (mode.equals("demo")) {
            DemoScenario.run();
            return;
        }
        if (mode.equals("test")) {
            System.exit(gtms.test.CoreTests.runAll() ? 0 : 1);
        }
        DataStore store = loadOrCreate();
        new ConsoleApp(new TournamentService(store), new ConsoleInput(new Scanner(System.in, StandardCharsets.UTF_8)))
                .guestLoop();
        store.save(DATA_FILE);
        System.out.println("Đã lưu dữ liệu vào " + DATA_FILE.toAbsolutePath());
    }

    private static DataStore loadOrCreate() throws TournamentException {
        if (Files.exists(DATA_FILE)) {
            try {
                DataStore store = DataStore.load(DATA_FILE);
                System.out.println("Đã nạp dữ liệu từ " + DATA_FILE);
                return store;
            } catch (IOException e) {
                System.out.println("Không đọc được file dữ liệu (" + e.getMessage() + ").");
                try {
                    System.out.println("Đã giữ lại file cũ ở " + DataStore.backupBroken(DATA_FILE) + ", dùng dữ liệu mẫu.");
                } catch (IOException moveError) {
                    // không đổi tên được thì dừng, tránh ghi đè lên file cũ khi thoát
                    throw new IllegalStateException("Không sao lưu được file dữ liệu hỏng: " + moveError.getMessage());
                }
            }
        }
        System.out.println("Khởi tạo dữ liệu mẫu. Mật khẩu mặc định: " + SampleData.DEFAULT_PASSWORD);
        return SampleData.create();
    }

    /** Mọi thao tác menu chạy qua đây để nhập sai chỉ báo lỗi, không làm dừng chương trình. */
    private void execute(Action action) {
        try {
            action.run();
        } catch (TournamentException e) {
            System.out.println("  ! Lỗi nghiệp vụ [" + e.getClass().getSimpleName() + "]: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("  ! Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("  ! Lỗi đọc/ghi file: " + e.getMessage());
        } catch (RuntimeException e) {
            // lỗi lập trình chưa lường trước: báo ra rồi quay lại menu, để khi thoát dữ liệu vẫn được lưu
            System.out.println("  ! Lỗi không mong đợi: " + e);
        }
    }

    private void guestLoop() {
        while (true) {
            System.out.println();
            System.out.println("===== GAME TOURNAMENT MANAGEMENT SYSTEM =====");
            System.out.println(" 1. Đăng nhập   2. Đăng ký tài khoản   3. Xem/tìm giải đấu   4. Xem chi tiết giải   0. Thoát");
            switch (in.integer("Chọn: ")) {
                case 1: execute(this::login); break;
                case 2: execute(() -> {
                    TeamManager m = service.registerAccount(in.text("Họ tên: "), in.text("Email: "),
                            in.text("Mật khẩu (>= 8 ký tự, có chữ và số): "));
                    System.out.println("Tạo tài khoản thành công: " + m.getUserId());
                }); break;
                case 3: execute(this::searchTournaments); break;
                case 4: execute(() -> Printer.tournamentDetail(pickTournament())); break;
                case 0: return;
                default: System.out.println("  ! Lựa chọn không hợp lệ");
            }
        }
    }

    private void login() throws TournamentException {
        User user = service.login(in.text("Email: "), in.text("Mật khẩu: "));
        System.out.println("Xin chào " + user.getName() + " (" + user.getRole() + ")");
        if (user instanceof Admin) {
            adminLoop((Admin) user);
        } else if (user instanceof TeamManager) {
            managerLoop((TeamManager) user);
        }
        user.logout();
    }

    private void searchTournaments() {
        String keyword = in.text("Từ khóa tên giải (Enter = tất cả): ");
        String statusText = in.text("Trạng thái (Enter = tất cả; DRAFT, REGISTRATION_OPEN, ONGOING, FINISHED...): ");
        TournamentStatus status = statusText.isEmpty() ? null : TournamentStatus.valueOf(statusText.toUpperCase());
        String game = in.text("Tựa game (Enter = tất cả): ");
        Printer.tournaments(service.searchTournaments(keyword, status, game));
    }

    private Tournament pickTournament() {
        Printer.tournaments(service.getStore().tournaments().findAll());
        String id = in.text("Mã giải: ");
        return service.getStore().tournaments().findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giải " + id));
    }

    private Match pickMatch(Tournament t) {
        Printer.schedule(t);
        String id = in.text("Mã trận: ");
        return t.findMatch(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trận " + id));
    }

    /** Trận vòng sau của nhánh loại trực tiếp có thể chưa có đội; phải chặn trước khi hỏi tỉ số. */
    private Match pickPlayableMatch(Tournament t) {
        Match m = pickMatch(t);
        if (m.getTeamA() == null || m.getTeamB() == null) {
            throw new IllegalStateException("Trận " + m.getMatchId() + " chưa xác định đủ hai đội");
        }
        return m;
    }

    private Team requireTeam(TeamManager me) throws TournamentException {
        if (!me.hasActiveTeam()) {
            throw new TournamentException("Bạn chưa có đội, hãy tạo đội trước");
        }
        return me.getManagedTeam();
    }

    private void managerLoop(TeamManager me) {
        while (true) {
            me.showMenu();
            switch (in.integer("Chọn: ")) {
                case 1: execute(this::searchTournaments); break;
                case 2: execute(() -> Printer.tournamentDetail(pickTournament())); break;
                case 3: execute(() -> {
                    Team t = service.createTeam(me, in.text("Tên đội: "), in.text("Logo: "), in.text("Khu vực: "),
                            in.text("Tựa game: "));
                    System.out.println("Đã tạo đội " + t.getTeamId());
                }); break;
                case 4: execute(() -> Printer.team(requireTeam(me))); break;
                case 5: execute(() -> {
                    Printer.team(requireTeam(me));
                    service.updateTeam(me, in.text("Tên đội mới: "), in.text("Logo mới: "), in.text("Khu vực mới: "));
                    System.out.println("Đã cập nhật: " + me.getManagedTeam());
                }); break;
                case 6: execute(() -> {
                    String name = requireTeam(me).getTeamName();
                    if (in.confirm("Giải thể đội " + name + "? Mọi tuyển thủ sẽ rời đội")) {
                        service.disbandTeam(me);
                        System.out.println("Đã giải thể đội " + name);
                    }
                }); break;
                case 7: execute(() -> System.out.println("Đã thêm " + service.addNewPlayer(me, in.text("Nickname: "),
                        in.text("Họ tên: "), in.text("Vị trí: "), in.text("ID trong game: ")).getPlayerId())); break;
                case 8: execute(() -> {
                    Printer.team(requireTeam(me));
                    me.updatePlayer(in.text("Mã tuyển thủ: ").toUpperCase(), in.text("Nickname mới: "),
                            in.text("Vị trí mới: "));
                    System.out.println("Đã cập nhật tuyển thủ");
                }); break;
                case 9: execute(() -> System.out.println("Đã xóa "
                        + service.removePlayer(me, in.text("Mã tuyển thủ: ").toUpperCase()).getNickname())); break;
                case 10: execute(() -> registerTournament(me)); break;
                case 11: execute(() -> {
                    Team team = requireTeam(me);
                    Printer.registrations(service.getRegistrationsOf(team));
                    Tournament t = pickTournament();
                    Registration r = t.findActiveRegistration(team)
                            .orElseThrow(() -> new TournamentException("Đội không có đơn còn hiệu lực ở giải này"));
                    me.cancelRegistration(r);
                    System.out.println("Đã hủy đơn " + r.getRegistrationId());
                }); break;
                case 12: execute(() -> {
                    Team team = me.getManagedTeam();
                    if (team == null) throw new TournamentException("Bạn chưa có đội");
                    System.out.println("Thành tích: " + service.getTeamSummary(team));
                    Printer.registrations(service.getRegistrationsOf(team));
                    service.getTeamHistory(team).forEach(m -> System.out.println("   " + m));
                }); break;
                case 0: return;
                default: System.out.println("  ! Lựa chọn không hợp lệ");
            }
        }
    }

    /** Bỏ trống ô chọn tuyển thủ thì lấy teamSize người đầu danh sách đội. */
    private void registerTournament(TeamManager me) throws TournamentException {
        Team team = requireTeam(me);
        Tournament t = pickTournament();
        System.out.printf("Điều kiện: game %s, %d tuyển thủ/đội, hạn đăng ký đến %s, đã dùng %d/%d slot%n",
                t.getGame(), t.getTeamSize(), t.getRegistrationEnd().format(ConsoleInput.DATE_TIME),
                t.countOccupiedSlots(), t.getMaxTeams());
        System.out.println("Đội của bạn " + (t.canRegister(team) ? "đủ điều kiện đăng ký" : "chưa đủ điều kiện"));
        Printer.team(team);
        String ids = in.text("Mã tuyển thủ thi đấu, cách nhau bởi dấu phẩy (Enter = " + t.getTeamSize()
                + " người đầu danh sách): ");
        Registration r;
        if (ids.isBlank()) {
            r = me.registerTournament(t);
        } else {
            List<Player> roster = new ArrayList<>();
            for (String id : ids.split(",")) {
                roster.add(team.findPlayer(id.trim().toUpperCase())
                        .orElseThrow(() -> new IllegalArgumentException("Không có tuyển thủ " + id.trim() + " trong đội")));
            }
            r = me.registerTournament(t, roster);
        }
        System.out.println("Đã gửi đơn: " + r);
    }

    private void adminLoop(Admin me) {
        while (true) {
            me.showMenu();
            switch (in.integer("Chọn: ")) {
                case 1: execute(this::searchTournaments); break;
                case 2: execute(() -> Printer.tournamentDetail(pickTournament())); break;
                case 3: execute(() -> {
                    Tournament t = service.createTournament(me, in.text("Tên giải: "), in.text("Tựa game: "),
                            readFormat(), in.integer("Số đội tối đa: "), in.integer("Số tuyển thủ mỗi đội: "),
                            in.dateTime("Mở đăng ký"), in.dateTime("Đóng đăng ký"), in.text("Giải thưởng: "));
                    System.out.println("Đã tạo giải " + t.getTournamentId() + " ở trạng thái Nháp");
                }); break;
                case 4: execute(() -> {
                    Tournament t = pickTournament();
                    me.updateTournament(t, in.text("Tên giải mới: "), in.text("Giải thưởng mới: "));
                    if (t.getStatus() == TournamentStatus.DRAFT && in.confirm("Sửa thể thức, quy mô, thời gian đăng ký?")) {
                        me.updateTournamentSettings(t, readFormat(), in.integer("Số đội tối đa: "),
                                in.integer("Số tuyển thủ mỗi đội: "), in.dateTime("Mở đăng ký"), in.dateTime("Đóng đăng ký"));
                    }
                    System.out.println("Đã cập nhật: " + t);
                }); break;
                case 5: execute(() -> {
                    Tournament t = pickTournament();
                    if (in.confirm("Xóa hẳn giải " + t.getName() + "?")) {
                        service.deleteTournament(me, t);
                        System.out.println("Đã xóa giải " + t.getTournamentId());
                    }
                }); break;
                case 6: execute(() -> {
                    Tournament t = pickTournament();
                    if (in.confirm("Hủy giải " + t.getName() + "?")) me.cancelTournament(t);
                }); break;
                case 7: execute(() -> me.openRegistration(pickTournament())); break;
                case 8: execute(() -> {
                    Tournament t = pickTournament();
                    Printer.registrations(t.getRegistrations());
                    Registration r = t.findRegistration(in.text("Mã đơn: ").toUpperCase())
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn"));
                    if (in.confirm("Duyệt đơn này?")) {
                        me.approveRegistration(r);
                    } else {
                        me.rejectRegistration(r, in.text("Lý do từ chối: "));
                    }
                    System.out.println("Cập nhật: " + r);
                }); break;
                case 9: execute(() -> me.closeRegistration(pickTournament())); break;
                case 10: execute(() -> {
                    Tournament t = pickTournament();
                    if (t.getMatches().isEmpty()) {
                        me.generateSchedule(t);
                    } else if (in.confirm("Giải đã có lịch. Xóa lịch cũ và sinh lại?")) {
                        me.regenerateSchedule(t);
                    }
                    Printer.schedule(t);
                }); break;
                case 11: execute(() -> {
                    Match m = pickMatch(pickTournament());
                    me.startMatch(m);
                    System.out.println("Đã bắt đầu: " + m);
                }); break;
                case 12: execute(() -> {
                    Match m = pickPlayableMatch(pickTournament());
                    if (m.getStatus() == MatchStatus.FINISHED
                            && !in.confirm("Trận đã có kết quả " + m.getScoreA() + "-" + m.getScoreB()
                            + ". Sửa lại và tính lại bảng xếp hạng?")) {
                        return;
                    }
                    me.updateMatchResult(m, in.optionalInteger("Tỉ số " + m.getTeamA().getTeamName() + ": "),
                            in.optionalInteger("Tỉ số " + m.getTeamB().getTeamName() + ": "));
                    System.out.println("Đã ghi: " + m);
                    Printer.standings(m.getTournament());
                }); break;
                case 13: execute(() -> {
                    Match m = pickPlayableMatch(pickTournament());
                    boolean teamALoses = in.confirm(m.getTeamA().getTeamName() + " bỏ cuộc? (n = " + m.getTeamB().getTeamName() + ")");
                    me.recordWalkover(m, teamALoses ? m.getTeamA() : m.getTeamB());
                    System.out.println("Đã ghi: " + m);
                }); break;
                case 14: execute(() -> {
                    service.getStore().users().findAll().forEach(System.out::println);
                    User u = service.getStore().users().findById(in.text("Mã tài khoản: "))
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
                    if (in.confirm("Khóa tài khoản (n = mở khóa)?")) me.lockUser(u); else me.unlockUser(u);
                    System.out.println("Cập nhật: " + u);
                }); break;
                case 15: execute(() -> {
                    Tournament t = pickTournament();
                    Path file = service.exportRankingCsv(t, Paths.get("data", "ranking_" + t.getTournamentId() + ".csv"));
                    System.out.println("Đã xuất " + file.toAbsolutePath());
                }); break;
                case 0: return;
                default: System.out.println("  ! Lựa chọn không hợp lệ");
            }
        }
    }

    private TournamentFormat readFormat() {
        return TournamentFormat.valueOf(in.text("Thể thức (ROUND_ROBIN / SINGLE_ELIMINATION): ").toUpperCase());
    }
}
