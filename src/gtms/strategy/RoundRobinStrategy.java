package gtms.strategy;

import gtms.model.Match;
import gtms.model.Team;
import gtms.model.Tournament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Vòng tròn một lượt, mỗi cặp gặp nhau đúng một lần: n chẵn thì n-1 vòng, n lẻ thì n vòng (mỗi đội nghỉ một vòng). */
public class RoundRobinStrategy implements ScheduleStrategy {
    private static final long serialVersionUID = 1L;

    @Override
    public List<Match> generate(Tournament tournament, List<Team> teams) {
        if (teams.size() < 2) {
            throw new IllegalArgumentException("Cần ít nhất 2 đội để xếp lịch vòng tròn");
        }
        List<Team> slots = new ArrayList<>(teams);
        if (slots.size() % 2 == 1) {
            slots.add(null); // đội gặp chỗ trống này được nghỉ vòng đó
        }
        int n = slots.size();
        List<Match> matches = new ArrayList<>();
        int counter = 1;
        for (int round = 1; round <= n - 1; round++) {
            int slotInRound = 0;
            for (int i = 0; i < n / 2; i++) {
                Team home = slots.get(i);
                Team away = slots.get(n - 1 - i);
                if (home != null && away != null) {
                    String id = String.format("%s-M%02d", tournament.getTournamentId(), counter++);
                    matches.add(new Match(tournament, id, round,
                            tournament.scheduledTimeFor(round, slotInRound++), home, away));
                }
            }
            // đội ở vị trí 0 đứng yên, các đội còn lại xoay một bước để ra cặp đấu vòng sau
            Collections.rotate(slots.subList(1, n), 1);
        }
        return matches;
    }
}
