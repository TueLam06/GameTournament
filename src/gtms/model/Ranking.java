package gtms.model;

import gtms.strategy.RankingCalculator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Cách tính điểm nằm ở RankingCalculator; lớp này chỉ giữ danh sách và thứ tự. */
public class Ranking implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Tournament tournament;
    private final List<RankingEntry> entries = new ArrayList<>();
    private RankingCalculator calculator;

    Ranking(Tournament tournament, RankingCalculator calculator) {
        this.tournament = tournament;
        this.calculator = calculator;
    }

    /** Tạo một entry 0 điểm cho mỗi đội khi giải bắt đầu. */
    void initialize(Collection<Team> teams) {
        entries.clear();
        teams.forEach(team -> entries.add(new RankingEntry(team)));
    }

    /** Tính lại từ đầu dựa trên tất cả trận đã có kết quả (không cộng dồn). */
    public void recalculate(List<Match> matches) {
        calculator.recalculate(this, matches);
    }

    public List<RankingEntry> getStandings() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public Optional<RankingEntry> findEntry(Team team) {
        return entries.stream().filter(e -> e.getTeam() == team).findFirst();
    }

    public void sortEntries(Comparator<RankingEntry> order) {
        entries.sort(order);
    }

    public List<RankingEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void setCalculator(RankingCalculator calculator) {
        this.calculator = calculator;
        recalculate(tournament.getMatches());
    }

    public Tournament getTournament() { return tournament; }
    public RankingCalculator getCalculator() { return calculator; }
}
