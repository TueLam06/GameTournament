package gtms.app;

import gtms.exception.TournamentException;
import gtms.model.Admin;
import gtms.model.TeamManager;
import gtms.model.Tournament;
import gtms.model.TournamentFormat;
import gtms.repository.DataStore;
import gtms.service.TournamentService;
import gtms.util.AppClock;

import java.time.LocalDateTime;

/** Dữ liệu mẫu: 1 Admin, 6 đội Valorant (mỗi đội 5 tuyển thủ), 2 giải đấu. */
public final class SampleData {
    public static final String GAME = "Valorant";
    public static final String DEFAULT_PASSWORD = "Ptit@2026";

    private static final String[][] TEAMS = {
            {"PTIT Phoenix", "Hà Nội"}, {"Saigon Dragons", "TP.HCM"}, {"Hanoi Wolves", "Hà Nội"},
            {"Danang Sharks", "Đà Nẵng"}, {"Hue Royals", "Huế"}, {"Cantho Tigers", "Cần Thơ"}};
    private static final String[] POSITIONS = {"Duelist", "Initiator", "Controller", "Sentinel", "Flex"};

    private SampleData() { }

    public static DataStore create() throws TournamentException {
        DataStore store = new DataStore();
        TournamentService service = new TournamentService(store);
        Admin admin = service.addAdmin("Ban tổ chức PTIT", "admin@ptit.edu.vn", DEFAULT_PASSWORD);

        for (int i = 0; i < TEAMS.length; i++) {
            TeamManager manager = service.registerAccount("Đội trưởng " + (i + 1),
                    "captain" + (i + 1) + "@gmail.com", DEFAULT_PASSWORD);
            service.createTeam(manager, TEAMS[i][0], "logo_" + (i + 1) + ".png", TEAMS[i][1], GAME);
            for (int p = 0; p < POSITIONS.length; p++) {
                String nickname = TEAMS[i][0].substring(0, 3).toUpperCase() + "_" + POSITIONS[p];
                service.addNewPlayer(manager, nickname, "Tuyển thủ " + (i + 1) + "." + (p + 1), POSITIONS[p],
                        "VAL#" + (1000 + i * 10 + p));
            }
        }

        LocalDateTime now = AppClock.now();
        Tournament cup = service.createTournament(admin, "PTIT Valorant Cup 2026", GAME, TournamentFormat.ROUND_ROBIN,
                4, 5, now.minusDays(3), now.plusDays(7), "10.000.000 VNĐ");
        admin.openRegistration(cup);
        service.createTournament(admin, "PTIT Knockout Series", GAME, TournamentFormat.SINGLE_ELIMINATION,
                8, 5, now.plusDays(1), now.plusDays(14), "Cúp + 5.000.000 VNĐ");
        return store;
    }
}
