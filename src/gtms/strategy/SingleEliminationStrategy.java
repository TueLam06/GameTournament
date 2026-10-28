package gtms.strategy;

import gtms.model.Match;
import gtms.model.Team;
import gtms.model.Tournament;

import java.util.ArrayList;
import java.util.List;

/**
 * Loại trực tiếp. Số đội phải là 2, 4, 8, 16... để không có đội nào được miễn đấu.
 * Vòng 1 ghép theo thứ tự đăng ký (1-2, 3-4, ...). Trận i của vòng r dẫn tới trận i/2 của vòng r+1;
 * các trận từ vòng 2 tạo sẵn nhưng chưa có đội, đội thắng được điền vào khi có kết quả.
 */
public class SingleEliminationStrategy implements ScheduleStrategy {
    private static final long serialVersionUID = 1L;

    @Override
    public List<Match> generate(Tournament tournament, List<Team> teams) {
        int n = teams.size();
        if (n < 2 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Loại trực tiếp cần số đội là 2, 4, 8, 16... (hiện có " + n + " đội)");
        }
        String tid = tournament.getTournamentId();
        List<Match> all = new ArrayList<>();

        List<Match> previous = new ArrayList<>();
        for (int i = 0; i < n / 2; i++) {
            Match match = new Match(tournament, String.format("%s-R1M%d", tid, i + 1), 1,
                    tournament.scheduledTimeFor(1, i), teams.get(2 * i), teams.get(2 * i + 1));
            previous.add(match);
        }
        all.addAll(previous);

        int round = 2;
        while (previous.size() > 1) {
            List<Match> current = new ArrayList<>();
            for (int i = 0; i < previous.size() / 2; i++) {
                current.add(new Match(tournament, String.format("%s-R%dM%d", tid, round, i + 1), round,
                        tournament.scheduledTimeFor(round, i), null, null));
            }
            for (int i = 0; i < previous.size(); i++) {
                previous.get(i).setNextMatch(current.get(i / 2));
            }
            all.addAll(current);
            previous = current;
            round++;
        }
        return all;
    }
}
