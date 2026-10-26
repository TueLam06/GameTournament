package gtms.strategy;

import gtms.model.Match;
import gtms.model.Team;
import gtms.model.Tournament;

import java.util.ArrayList;
import java.util.List;

/**
 * Loại trực tiếp. Thứ tự đăng ký được dùng làm hạt giống. Kích thước nhánh là lũy thừa của 2 nhỏ nhất
 * không nhỏ hơn số đội; các vị trí thiếu là bye và rơi vào các hạt giống cao nhất.
 * Đội được bye không có Match vòng 1 mà được xếp thẳng vào trận vòng 2.
 */
public class SingleEliminationStrategy implements ScheduleStrategy {
    private static final long serialVersionUID = 1L;

    @Override
    public List<Match> generate(Tournament tournament, List<Team> teams) {
        if (teams.size() < 2) {
            throw new IllegalArgumentException("Cần ít nhất 2 đội để xếp nhánh loại trực tiếp");
        }
        int bracketSize = Integer.highestOneBit(teams.size() - 1) << 1; // 2, 4, 8, 16, 32...
        int totalRounds = Integer.numberOfTrailingZeros(bracketSize);
        String tid = tournament.getTournamentId();

        // Tạo khung các vòng từ 2 trở đi, chưa có đội
        List<List<Match>> rounds = new ArrayList<>();
        rounds.add(new ArrayList<>()); // vòng 1 tạo sau
        int counter = 1;
        for (int round = 2; round <= totalRounds; round++) {
            List<Match> current = new ArrayList<>();
            int matchCount = bracketSize >> round;
            for (int i = 0; i < matchCount; i++) {
                current.add(new Match(tournament, String.format("%s-R%dM%d", tid, round, i + 1), round,
                        tournament.scheduledTimeFor(round, i), null, null));
            }
            rounds.add(current);
        }
        // Liên kết nextMatch giữa các vòng: trận i của vòng r đi tiếp vào trận i/2 của vòng r+1
        for (int r = 1; r < rounds.size() - 1; r++) {
            List<Match> current = rounds.get(r);
            for (int i = 0; i < current.size(); i++) {
                current.get(i).setNextMatch(rounds.get(r + 1).get(i / 2));
            }
        }

        // Vòng 1: ghép theo thứ tự hạt giống chuẩn để hai hạt giống đầu chỉ gặp nhau ở chung kết
        List<Integer> seedOrder = seedOrder(bracketSize);
        List<Match> firstRound = rounds.get(0);
        for (int pair = 0; pair < bracketSize / 2; pair++) {
            Team a = seedAt(teams, seedOrder.get(2 * pair));
            Team b = seedAt(teams, seedOrder.get(2 * pair + 1));
            Match next = totalRounds > 1 ? rounds.get(1).get(pair / 2) : null;
            if (a != null && b != null) {
                Match match = new Match(tournament, String.format("%s-R1M%d", tid, counter++), 1,
                        tournament.scheduledTimeFor(1, firstRound.size()), a, b);
                match.setNextMatch(next);
                firstRound.add(match);
            } else if (next != null) {
                next.assignTeam(a != null ? a : b); // đội được bye vào thẳng vòng 2
            }
        }

        List<Match> all = new ArrayList<>();
        rounds.forEach(all::addAll);
        return all;
    }

    /** Thứ tự hạt giống chuẩn, ví dụ 8 đội: 1,8,4,5,2,7,3,6. */
    static List<Integer> seedOrder(int size) {
        List<Integer> order = new ArrayList<>(List.of(1, 2));
        while (order.size() < size) {
            int doubled = order.size() * 2;
            List<Integer> next = new ArrayList<>();
            for (int seed : order) {
                next.add(seed);
                next.add(doubled + 1 - seed);
            }
            order = next;
        }
        return order;
    }

    private static Team seedAt(List<Team> teams, int seed) {
        return seed <= teams.size() ? teams.get(seed - 1) : null;
    }
}
