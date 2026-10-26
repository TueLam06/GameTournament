package gtms.strategy;

import gtms.model.Match;
import gtms.model.Ranking;
import gtms.model.RankingEntry;

import java.util.Comparator;
import java.util.List;

/**
 * Thắng 3, hòa 1, thua 0. Bằng điểm thì xét hiệu số, rồi số trận thắng, rồi tên đội.
 * Mỗi lần gọi đều tính lại từ 0 nên sửa kết quả không bị cộng điểm hai lần.
 */
public class DefaultRankingCalculator implements RankingCalculator {
    private static final long serialVersionUID = 1L;

    public static final int WIN_POINTS = 3;
    public static final int DRAW_POINTS = 1;
    public static final int LOSS_POINTS = 0;

    public static final Comparator<RankingEntry> STANDING_ORDER =
            Comparator.comparingInt(RankingEntry::getPoints).reversed()
                    .thenComparing(Comparator.comparingInt(RankingEntry::getScoreDifference).reversed())
                    .thenComparing(Comparator.comparingInt(RankingEntry::getWins).reversed())
                    .thenComparing(e -> e.getTeam().getTeamName());

    @Override
    public void recalculate(Ranking ranking, List<Match> matches) {
        ranking.getEntries().forEach(RankingEntry::reset);
        for (Match match : matches) {
            if (match.getStatus().isPlayed()) {
                ranking.findEntry(match.getTeamA()).ifPresent(e -> e.updateFromMatch(match));
                ranking.findEntry(match.getTeamB()).ifPresent(e -> e.updateFromMatch(match));
            }
        }
        ranking.getEntries().forEach(e -> e.applyPoints(WIN_POINTS, DRAW_POINTS, LOSS_POINTS));
        ranking.sortEntries(STANDING_ORDER);
    }
}
