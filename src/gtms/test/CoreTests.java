package gtms.test;

import gtms.app.DemoScenario;
import gtms.app.SampleData;
import gtms.exception.InvalidScoreException;
import gtms.exception.InvalidStatusTransitionException;
import gtms.exception.RegistrationClosedException;
import gtms.exception.TeamNotEligibleException;
import gtms.exception.TournamentException;
import gtms.model.Admin;
import gtms.model.Match;
import gtms.model.Player;
import gtms.model.RankingEntry;
import gtms.model.Registration;
import gtms.model.RegistrationStatus;
import gtms.model.Team;
import gtms.model.TeamManager;
import gtms.model.Tournament;
import gtms.model.TournamentFormat;
import gtms.model.TournamentStatus;
import gtms.model.User;
import gtms.repository.DataStore;
import gtms.repository.Repository;
import gtms.service.TournamentService;
import gtms.util.AppClock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Chạy bằng "ConsoleApp test". Không dùng JUnit: test nào sai thì ném AssertionError. */
public final class CoreTests {

    @FunctionalInterface
    private interface TestCase {
        void run() throws Exception;
    }

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static int sequence = 0;

    private CoreTests() { }

    public static boolean runAll() {
        AppClock.setFixed(NOW);
        Map<String, TestCase> tests = new LinkedHashMap<>();
        tests.put("Mật khẩu băm, đủ 6 ký tự; email không trống", CoreTests::passwordIsHashed);
        tests.put("Round Robin 4 đội: 6 trận, 3 vòng, mỗi cặp gặp 1 lần", () -> roundRobinPairs(4, 6, 3));
        tests.put("Round Robin 5 đội (lẻ, có bye): 10 trận, 5 vòng", () -> roundRobinPairs(5, 10, 5));
        tests.put("Single Elimination 8 đội: 7 trận, 3 vòng; 6 đội không đóng được", CoreTests::singleEliminationBracket);
        tests.put("BXH: thắng 3 / hòa 1 / thua 0, sắp xếp đúng", CoreTests::rankingPoints);
        tests.put("Sửa kết quả không cộng điểm hai lần", CoreTests::editResultRecalculates);
        tests.put("Hết slot thì không nhận đơn, hủy đơn thì trống slot", CoreTests::fullTournamentRejects);
        tests.put("Ngoại lệ tỉ số: âm, trống, hòa/sửa ở loại trực tiếp", CoreTests::invalidScores);
        tests.put("Ngoại lệ đăng ký: thiếu người, sai game, đóng cổng khi thiếu đội", CoreTests::registrationErrors);
        tests.put("Chuyển trạng thái giải không hợp lệ bị chặn", CoreTests::invalidTransitions);
        tests.put("Giải tự kết thúc khi trận cuối có kết quả", CoreTests::tournamentFinishes);
        tests.put("Sửa cấu hình và xóa giải chỉ khi còn Nháp", CoreTests::editAndDeleteOnlyInDraft);
        tests.put("Thành tích tổng hợp của đội qua nhiều trận", CoreTests::teamSummary);
        tests.put("Repository generic: tìm theo mã, lọc theo kiểu con", CoreTests::genericRepository);
        tests.put("Lưu và nạp lại file dữ liệu giữ nguyên tham chiếu", CoreTests::saveAndLoad);
        tests.put("NFR-01: sinh lịch 32 đội + BXH dưới 2 giây", CoreTests::performance32Teams);
        tests.put("Không xóa được tuyển thủ khi đội còn đơn hiệu lực", CoreTests::playerCannotLeaveWhileRegistered);
        tests.put("Hủy giải thì đơn hủy theo, đội giải thể được", CoreTests::cancelledTournamentReleasesTeams);
        tests.put("Sửa cấu hình bị lỗi thì giữ nguyên cấu hình cũ", CoreTests::failedSettingsChangeKeepsOldValues);
        tests.put("File dữ liệu hỏng được giữ lại, không bị ghi đè", CoreTests::brokenFileIsBackedUp);
        tests.put("Demo chạy được khi đã có dữ liệu mẫu trước đó", CoreTests::demoRunsAfterSampleData);

        int passed = 0;
        for (Map.Entry<String, TestCase> test : tests.entrySet()) {
            try {
                test.getValue().run();
                passed++;
                System.out.println("[PASS] " + test.getKey());
            } catch (Throwable e) {
                System.out.println("[FAIL] " + test.getKey() + " -> " + e);
            }
        }
        System.out.printf("Kết quả: %d/%d test đạt%n", passed, tests.size());
        AppClock.useSystemClock();
        return passed == tests.size();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static <E extends Exception> void expect(Class<E> type, TestCase action) throws Exception {
        try {
            action.run();
        } catch (Exception e) {
            if (type.isInstance(e)) {
                return;
            }
            throw new AssertionError("Mong đợi " + type.getSimpleName() + " nhưng nhận " + e);
        }
        throw new AssertionError("Mong đợi " + type.getSimpleName() + " nhưng không có ngoại lệ");
    }

    private static TeamManager managerWithTeam(int players, String game) throws TournamentException {
        int id = ++sequence;
        TeamManager m = new TeamManager("TM" + id, "Manager " + id, "m" + id + "@test.vn", "Passw0rd");
        m.createTeam("Team " + id, "", "HN", game);
        for (int i = 0; i < players; i++) {
            m.addPlayer(new Player("P" + id + "_" + i, "nick" + id + "_" + i, "Player " + i, "Flex", "G" + i));
        }
        return m;
    }

    private static Admin admin() {
        return new Admin("AD" + (++sequence), "Admin", "admin" + sequence + "@test.vn", "Passw0rd");
    }

    private static Tournament openTournament(Admin admin, TournamentFormat format, int maxTeams)
            throws TournamentException {
        Tournament t = admin.createTournament("Giải test " + (++sequence), "Valorant", format, maxTeams, 2,
                NOW.minusDays(1), NOW.plusDays(1), "");
        admin.openRegistration(t);
        return t;
    }

    /** Giải đã sinh lịch với n đội, mọi đơn được duyệt. */
    private static Tournament ongoing(TournamentFormat format, int teams) throws TournamentException {
        Admin admin = admin();
        Tournament t = openTournament(admin, format, teams);
        for (int i = 0; i < teams; i++) {
            admin.approveRegistration(managerWithTeam(2, "Valorant").registerTournament(t));
        }
        admin.closeRegistration(t);
        admin.generateSchedule(t);
        return t;
    }

    private static void passwordIsHashed() throws Exception {
        User u = new TeamManager("TMX", "X", "x@test.vn", "Secret123");
        check(u.verifyPassword("Secret123"), "mật khẩu đúng phải được chấp nhận");
        check(!u.verifyPassword("secret123"), "mật khẩu sai phải bị từ chối");
        new TeamManager("TMY", "Y", "y", "123456");   // 6 ký tự và email bất kỳ không trống là hợp lệ
        expect(IllegalArgumentException.class, () -> new TeamManager("TMZ", "Z", "z", "12345"));
        expect(IllegalArgumentException.class, () -> new TeamManager("TMW", "W", " ", "123456"));
    }

    private static void roundRobinPairs(int teams, int expectedMatches, int expectedRounds) throws Exception {
        Tournament t = ongoing(TournamentFormat.ROUND_ROBIN, teams);
        check(t.getMatches().size() == expectedMatches, "số trận = " + t.getMatches().size());
        check(t.getMatchesByRound().size() == expectedRounds, "số vòng = " + t.getMatchesByRound().size());
        Set<String> pairs = new HashSet<>();
        for (Match m : t.getMatches()) {
            String a = m.getTeamA().getTeamId();
            String b = m.getTeamB().getTeamId();
            check(pairs.add(a.compareTo(b) < 0 ? a + b : b + a), "cặp đấu lặp lại: " + a + "-" + b);
        }
        for (List<Match> round : t.getMatchesByRound().values()) {
            Set<Team> inRound = new HashSet<>();
            round.forEach(m -> { inRound.add(m.getTeamA()); inRound.add(m.getTeamB()); });
            check(inRound.size() == round.size() * 2, "một đội đá hai trận trong cùng vòng");
        }
    }

    private static void singleEliminationBracket() throws Exception {
        Tournament t = ongoing(TournamentFormat.SINGLE_ELIMINATION, 8);
        Map<Integer, List<Match>> rounds = t.getMatchesByRound();
        check(t.getMatches().size() == 7, "8 đội loại trực tiếp phải có 7 trận, có " + t.getMatches().size());
        check(rounds.get(1).size() == 4 && rounds.get(2).size() == 2 && rounds.get(3).size() == 1, "sai số trận mỗi vòng");
        check(rounds.get(2).stream().allMatch(m -> m.getTeamA() == null && m.getTeamB() == null),
                "vòng 2 chưa có đội trước khi vòng 1 có kết quả");
        rounds.get(1).forEach(m -> check(m.getNextMatch() != null, "trận vòng 1 phải trỏ tới vòng 2"));
        Match first = rounds.get(1).get(0);
        first.setResult(2, 0);
        check(first.getNextMatch().getTeamA() == first.getTeamA(), "đội thắng phải được đưa vào trận vòng sau");

        expect(IllegalArgumentException.class, () -> openTournament(admin(), TournamentFormat.SINGLE_ELIMINATION, 6));
        Admin admin = admin();
        Tournament six = openTournament(admin, TournamentFormat.SINGLE_ELIMINATION, 8);
        for (int i = 0; i < 6; i++) {
            admin.approveRegistration(managerWithTeam(2, "Valorant").registerTournament(six));
        }
        expect(TournamentException.class, () -> admin.closeRegistration(six));
        check(six.getStatus() == TournamentStatus.REGISTRATION_OPEN, "6 đội thì giải phải vẫn mở đăng ký");
        for (int i = 0; i < 2; i++) {
            admin.approveRegistration(managerWithTeam(2, "Valorant").registerTournament(six));
        }
        admin.closeRegistration(six);
        check(admin.generateSchedule(six).size() == 7, "duyệt thêm cho đủ 8 đội thì sinh được lịch");
    }

    private static void rankingPoints() throws Exception {
        Tournament t = ongoing(TournamentFormat.ROUND_ROBIN, 3);
        List<Match> ms = t.getMatches();
        ms.get(0).setResult(2, 0);
        ms.get(1).setResult(1, 1);
        ms.get(2).setResult(0, 3);
        int total = t.getRanking().getStandings().stream().mapToInt(RankingEntry::getPoints).sum();
        check(total == 3 + 3 + 1 + 1, "tổng điểm phải là 8, nhận " + total);
        List<RankingEntry> s = t.getRanking().getStandings();
        for (int i = 1; i < s.size(); i++) {
            check(s.get(i - 1).getPoints() >= s.get(i).getPoints(), "BXH chưa sắp giảm dần theo điểm");
        }
    }

    private static void editResultRecalculates() throws Exception {
        Tournament t = ongoing(TournamentFormat.ROUND_ROBIN, 2);
        Match m = t.getMatches().get(0);
        m.setResult(1, 0);
        // giải 2 đội đã tự kết thúc sau trận duy nhất, Admin vẫn được sửa kết quả
        m.setResult(0, 2);
        RankingEntry winner = t.getRanking().findEntry(m.getTeamB()).orElseThrow();
        RankingEntry loser = t.getRanking().findEntry(m.getTeamA()).orElseThrow();
        check(winner.getPoints() == 3 && winner.getPlayed() == 1, "đội thắng phải có 3 điểm sau 1 trận");
        check(loser.getPoints() == 0 && loser.getWins() == 0, "đội thua không được giữ điểm cũ");
        check(winner.getScoreDifference() == 2, "hiệu số phải là +2");
    }

    private static void fullTournamentRejects() throws Exception {
        Admin admin = admin();
        Tournament t = openTournament(admin, TournamentFormat.ROUND_ROBIN, 2);
        List<TeamManager> ms = new ArrayList<>();
        List<Registration> rs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ms.add(managerWithTeam(2, "Valorant"));
            rs.add(ms.get(i).registerTournament(t));
        }
        TeamManager late = managerWithTeam(2, "Valorant");
        expect(RegistrationClosedException.class, () -> late.registerTournament(t));
        check(t.getRegistrations().size() == 2, "đơn bị từ chối vì hết slot không được lưu lại");
        ms.get(0).cancelRegistration(rs.get(0));
        Registration r = late.registerTournament(t);
        admin.approveRegistration(r);
        check(r.getStatus() == RegistrationStatus.APPROVED && t.countOccupiedSlots() == 2,
                "hủy đơn phải trả lại slot cho đội khác");
    }

    private static void invalidScores() throws Exception {
        Tournament rr = ongoing(TournamentFormat.ROUND_ROBIN, 2);
        Match m = rr.getMatches().get(0);
        expect(InvalidScoreException.class, () -> m.setResult(-1, 0));
        expect(InvalidScoreException.class, () -> m.setResult(null, 0));
        check(!m.getStatus().isPlayed(), "trận phải giữ nguyên trạng thái khi tỉ số sai");
        Tournament se = ongoing(TournamentFormat.SINGLE_ELIMINATION, 4);
        Match first = se.getMatches().get(0);
        expect(InvalidScoreException.class, () -> first.setResult(1, 1));
        first.setResult(2, 1);
        expect(InvalidStatusTransitionException.class, () -> first.setResult(0, 2));
        check(first.getScoreA() == 2 && first.getNextMatch().getTeamA() == first.getTeamA(),
                "kết quả loại trực tiếp đã ghi phải giữ nguyên");
    }

    private static void registrationErrors() throws Exception {
        Admin admin = admin();
        Tournament t = openTournament(admin, TournamentFormat.ROUND_ROBIN, 4);
        Tournament draft = admin.createTournament("Nháp", "Valorant", TournamentFormat.ROUND_ROBIN, 4, 2,
                NOW.minusDays(1), NOW.plusDays(1), "");
        Team enough = managerWithTeam(2, "Valorant").getManagedTeam();
        check(t.canRegister(enough), "đội đủ người phải đăng ký được giải đang mở");
        check(draft.registrationProblem(enough).orElse("").contains("Nháp"),
                "giải Nháp phải báo lý do chưa mở đăng ký, không phải đội chưa đủ điều kiện");
        expect(TeamNotEligibleException.class, () -> managerWithTeam(1, "Valorant").registerTournament(t));
        expect(TeamNotEligibleException.class, () -> managerWithTeam(2, "LoL").registerTournament(t));
        TeamManager ok = managerWithTeam(2, "Valorant");
        ok.registerTournament(t);
        expect(TeamNotEligibleException.class, () -> ok.registerTournament(t));
        expect(TournamentException.class, () -> admin.closeRegistration(t));   // chưa có đội nào được duyệt
        for (int i = 0; i < 2; i++) {
            admin.approveRegistration(managerWithTeam(2, "Valorant").registerTournament(t));
        }
        admin.closeRegistration(t);
        expect(RegistrationClosedException.class, () -> managerWithTeam(2, "Valorant").registerTournament(t));
        check(t.getRegistrations().get(0).getStatus() == RegistrationStatus.REJECTED,
                "đơn chưa duyệt phải bị từ chối khi đóng cổng");
    }

    private static void invalidTransitions() throws Exception {
        Admin admin = admin();
        Tournament t = admin.createTournament("T", "Valorant", TournamentFormat.ROUND_ROBIN, 4, 2,
                NOW.minusDays(1), NOW.plusDays(1), "");
        expect(InvalidStatusTransitionException.class, () -> t.changeStatus(TournamentStatus.ONGOING));
        expect(InvalidStatusTransitionException.class, () -> t.changeStatus(TournamentStatus.FINISHED));
        expect(InvalidStatusTransitionException.class, () -> admin.generateSchedule(t));
        expect(IllegalArgumentException.class, () -> admin.createTournament("Sai ngày", "Valorant",
                TournamentFormat.ROUND_ROBIN, 4, 2, NOW, NOW.minusHours(1), ""));
    }

    private static void tournamentFinishes() throws Exception {
        Tournament t = ongoing(TournamentFormat.SINGLE_ELIMINATION, 4);
        for (List<Match> round : t.getMatchesByRound().values()) {
            for (Match m : round) {
                m.setResult(3, 1);
            }
        }
        check(t.getStatus() == TournamentStatus.FINISHED, "giải phải tự chuyển sang FINISHED");
        check(t.getChampion().isPresent(), "phải xác định được nhà vô địch");
    }

    private static void editAndDeleteOnlyInDraft() throws Exception {
        DataStore store = new DataStore();
        TournamentService service = new TournamentService(store);
        Admin admin = admin();
        Tournament draft = service.createTournament(admin, "Nháp", "Valorant", TournamentFormat.ROUND_ROBIN, 4, 2,
                NOW.minusDays(1), NOW.plusDays(1), "");
        admin.updateTournamentSettings(draft, TournamentFormat.SINGLE_ELIMINATION, 8, 3, NOW, NOW.plusDays(2));
        check(draft.getMaxTeams() == 8 && draft.getFormat() == TournamentFormat.SINGLE_ELIMINATION, "chưa sửa được cấu hình");
        admin.openRegistration(draft);
        expect(InvalidStatusTransitionException.class, () -> admin.updateTournamentSettings(draft,
                TournamentFormat.ROUND_ROBIN, 4, 2, NOW, NOW.plusDays(1)));
        expect(InvalidStatusTransitionException.class, () -> service.deleteTournament(admin, draft));
        Tournament other = service.createTournament(admin, "Xóa được", "Valorant", TournamentFormat.ROUND_ROBIN, 4, 2,
                NOW, NOW.plusDays(1), "");
        service.deleteTournament(admin, other);
        check(store.tournaments().size() == 1, "giải Nháp phải bị xóa khỏi kho");
    }

    private static void teamSummary() throws Exception {
        DataStore store = new DataStore();
        Tournament t = ongoing(TournamentFormat.ROUND_ROBIN, 3);
        store.tournaments().add(t);
        List<Match> ms = t.getMatches();
        Team team = ms.get(0).getTeamA();
        for (Match m : ms) {
            if (m.involves(team)) {
                m.setResult(m.getTeamA() == team ? 2 : 0, m.getTeamA() == team ? 0 : 2);
            } else {
                m.setResult(1, 1);
            }
        }
        TournamentService.TeamSummary s = new TournamentService(store).getTeamSummary(team);
        check(s.getTournaments() == 1 && s.getPlayed() == 2 && s.getWins() == 2, "sai thành tích: " + s);
        check(s.getTitles() == 1, "đội thắng cả 2 trận phải vô địch: " + s);
    }

    private static void playerCannotLeaveWhileRegistered() throws Exception {
        TournamentService service = new TournamentService(new DataStore());
        Admin admin = service.addAdmin("Admin", "roster-admin@test.vn", "Passw0rd");
        TeamManager manager = service.registerAccount("Manager", "roster-tm@test.vn", "Passw0rd");
        service.createTeam(manager, "Roster Team", "", "HN", "Valorant");
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            players.add(service.addNewPlayer(manager, "r" + i, "Player " + i, "Flex", "G" + i));
        }
        Tournament t = service.createTournament(admin, "Roster Cup", "Valorant", TournamentFormat.ROUND_ROBIN, 4, 2,
                NOW.minusDays(1), NOW.plusDays(1), "");
        admin.openRegistration(t);
        Registration r = manager.registerTournament(t);
        admin.approveRegistration(r);
        expect(TournamentException.class, () -> service.removePlayer(manager, players.get(2).getPlayerId()));
        manager.cancelRegistration(r);
        service.removePlayer(manager, players.get(2).getPlayerId());
        check(manager.getManagedTeam().getPlayers().size() == 2, "hủy đơn rồi thì phải xóa được tuyển thủ");
    }

    private static void cancelledTournamentReleasesTeams() throws Exception {
        TournamentService service = new TournamentService(new DataStore());
        Admin admin = service.addAdmin("Admin", "cancel-admin@test.vn", "Passw0rd");
        TeamManager manager = service.registerAccount("Manager", "cancel-tm@test.vn", "Passw0rd");
        service.createTeam(manager, "Cancel Team", "", "HN", "Valorant");
        for (int i = 0; i < 2; i++) {
            service.addNewPlayer(manager, "c" + i, "Player " + i, "Flex", "G" + i);
        }
        Tournament t = service.createTournament(admin, "Cancel Cup", "Valorant", TournamentFormat.ROUND_ROBIN, 4, 2,
                NOW.minusDays(1), NOW.plusDays(1), "");
        admin.openRegistration(t);
        Registration r = manager.registerTournament(t);
        admin.approveRegistration(r);
        admin.cancelTournament(t);
        check(r.getStatus() == RegistrationStatus.CANCELLED, "đơn phải bị hủy cùng giải");
        service.disbandTeam(manager);
        check(!manager.hasActiveTeam(), "đội phải giải thể được sau khi giải bị hủy");
    }

    private static void failedSettingsChangeKeepsOldValues() throws Exception {
        Admin admin = admin();
        Tournament t = admin.createTournament("Cấu hình", "Valorant", TournamentFormat.SINGLE_ELIMINATION, 8, 5,
                NOW.minusDays(1), NOW.plusDays(1), "");
        expect(IllegalArgumentException.class, () -> admin.updateTournamentSettings(t, TournamentFormat.ROUND_ROBIN,
                16, 0, NOW, NOW.plusDays(2)));
        check(t.getFormat() == TournamentFormat.SINGLE_ELIMINATION && t.getMaxTeams() == 8 && t.getTeamSize() == 5,
                "cấu hình bị đổi một phần: " + t.getFormat() + ", " + t.getMaxTeams() + ", " + t.getTeamSize());
    }

    private static void brokenFileIsBackedUp() throws Exception {
        Path dir = Files.createTempDirectory("gtms");
        Path file = dir.resolve("gtms.dat");
        Files.writeString(file, "không phải dữ liệu");
        expect(java.io.IOException.class, () -> DataStore.load(file));
        Path backup = DataStore.backupBroken(file);
        check(!Files.exists(file) && Files.readString(backup).equals("không phải dữ liệu"), "file hỏng phải được giữ lại");
        new DataStore().save(file);
        check(DataStore.load(file) != null && Files.exists(backup), "lưu mới không được ghi đè lên bản sao lưu");
        Files.delete(file);
        Files.delete(backup);
        Files.delete(dir);
    }

    private static void demoRunsAfterSampleData() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            SampleData.create();          // giống GUI: đã có dữ liệu mẫu nên bộ sinh mã không còn bắt đầu từ 1
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            DemoScenario.run();
        } finally {
            System.setOut(original);
            AppClock.setFixed(NOW);       // demo trả đồng hồ về giờ thật, các test sau cần giờ cố định
        }
        String text = output.toString(StandardCharsets.UTF_8);
        check(!text.contains("KHÔNG LỖI?"), "có bước demo lẽ ra phải báo lỗi nhưng không báo");
        check(text.contains("NEW_ACE"), "demo phải chạy đến bước cuối");
    }

    private static void genericRepository() {
        Repository<User> users = new Repository<>();
        users.add(new TeamManager("TM900", "A", "a@test.vn", "Passw0rd"));
        users.add(new Admin("AD900", "B", "b@test.vn", "Passw0rd"));
        check(users.findById("tm900").isPresent(), "tìm theo mã không phân biệt hoa thường");
        check(users.findAllOfType(Admin.class).size() == 1, "lọc theo kiểu con Admin");
        try {
            users.add(new Admin("AD900", "C", "c@test.vn", "Passw0rd"));
            throw new AssertionError("phải chặn trùng mã");
        } catch (IllegalArgumentException expected) {
            // trùng mã phải bị chặn
        }
    }

    private static void saveAndLoad() throws Exception {
        DataStore store = new DataStore();
        Tournament t = ongoing(TournamentFormat.ROUND_ROBIN, 3);
        t.getMatches().get(0).setResult(2, 1);
        store.tournaments().add(t);
        Path file = Files.createTempFile("gtms", ".dat");
        store.save(file);
        Tournament copy = DataStore.load(file).tournaments().findById(t.getTournamentId()).orElseThrow();
        Files.deleteIfExists(file);
        check(copy.getMatches().size() == 3, "phải nạp lại đủ 3 trận");
        Match m = copy.getMatches().get(0);
        check(copy.getRanking().findEntry(m.getTeamA()).isPresent(), "Ranking và Match phải trỏ cùng một Team");
        check(copy.getRanking().getStandings().get(0).getPoints() == 3, "điểm phải được giữ sau khi nạp");
    }

    private static void performance32Teams() throws Exception {
        long start = System.nanoTime();
        Tournament rr = ongoing(TournamentFormat.ROUND_ROBIN, 32);
        for (Match m : rr.getMatches()) {
            m.setResult(1, 0);
        }
        Tournament se = ongoing(TournamentFormat.SINGLE_ELIMINATION, 32);
        long ms = (System.nanoTime() - start) / 1_000_000;
        check(rr.getMatches().size() == 496, "32 đội vòng tròn phải có 496 trận");
        check(se.getMatches().size() == 31, "32 đội loại trực tiếp phải có 31 trận");
        check(ms < 2000, "mất " + ms + " ms");
        System.out.println("       (sinh 2 lịch 32 đội + ghi 496 kết quả: " + ms + " ms)");
    }
}
