package gtms.app;

import gtms.model.Match;
import gtms.model.RankingEntry;
import gtms.model.Registration;
import gtms.model.Team;
import gtms.model.Tournament;

import java.util.List;
import java.util.Map;

/** Các hàm hiển thị dạng bảng trên console. */
public final class Printer {
    private Printer() { }

    public static void title(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }

    public static void tournaments(List<Tournament> tournaments) {
        if (tournaments.isEmpty()) {
            System.out.println("(không có giải đấu phù hợp)");
            return;
        }
        System.out.printf("%-8s %-28s %-18s %-15s %-9s %s%n", "Mã", "Tên giải", "Game", "Thể thức", "Slot", "Trạng thái");
        tournaments.forEach(System.out::println);
    }

    public static void tournamentDetail(Tournament t) {
        title("Giải " + t.getName() + " (" + t.getTournamentId() + ")");
        System.out.printf("Game: %s | Thể thức: %s | Quy mô: %d người/đội | Tối đa: %d đội%n",
                t.getGame(), t.getFormat().getLabel(), t.getTeamSize(), t.getMaxTeams());
        System.out.printf("Đăng ký: %s -> %s | Giải thưởng: %s | Trạng thái: %s%n",
                t.getRegistrationStart().format(ConsoleInput.DATE_TIME),
                t.getRegistrationEnd().format(ConsoleInput.DATE_TIME), t.getPrize(), t.getStatus().getLabel());
        registrations(t.getRegistrations());
        schedule(t);
        standings(t);
        t.getChampion().ifPresent(c -> System.out.println(">>> VÔ ĐỊCH: " + c.getTeamName()));
    }

    public static void registrations(List<Registration> registrations) {
        System.out.println("-- Đơn đăng ký (" + registrations.size() + ")");
        registrations.forEach(r -> System.out.println("   " + r));
    }

    public static void schedule(Tournament t) {
        Map<Integer, List<Match>> byRound = t.getMatchesByRound();
        if (byRound.isEmpty()) {
            System.out.println("-- Chưa có lịch thi đấu");
            return;
        }
        System.out.println("-- Lịch thi đấu (" + t.getMatches().size() + " trận)");
        byRound.forEach((round, matches) -> matches.forEach(m -> System.out.println("   " + m)));
    }

    public static void standings(Tournament t) {
        List<RankingEntry> standings = t.getRanking().getStandings();
        if (standings.isEmpty()) {
            return;
        }
        System.out.println("-- Bảng xếp hạng (thắng 3, hòa 1, thua 0)");
        System.out.printf("   %-3s %-18s %3s %3s %3s %3s %5s %5s%n", "#", "Đội", "Tr", "T", "H", "B", "HS", "Điểm");
        for (int i = 0; i < standings.size(); i++) {
            System.out.printf("   %-3d %s%n", i + 1, standings.get(i));
        }
    }

    public static void team(Team team) {
        System.out.println(team);
        System.out.printf("   %-6s %-15s %-22s %-11s %s%n", "Mã", "Nickname", "Họ tên", "Vị trí", "Game ID");
        team.getPlayers().forEach(p -> System.out.println("   " + p));
    }
}
