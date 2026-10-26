package gtms.model;

import java.io.Serializable;

/** Thành tích của một đội trong một giải. */
public class RankingEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Team team;
    private int played;
    private int points;
    private int wins;
    private int draws;
    private int losses;
    private int scoreDifference;

    RankingEntry(Team team) {
        this.team = team;
    }

    public void reset() {
        played = points = wins = draws = losses = scoreDifference = 0;
    }

    /** Bỏ qua trận chưa có kết quả hoặc không có đội này. */
    public void updateFromMatch(Match match) {
        if (!match.getStatus().isPlayed() || !match.involves(team)) {
            return;
        }
        Team opponent = match.getTeamA() == team ? match.getTeamB() : match.getTeamA();
        int own = match.scoreOf(team);
        int against = match.scoreOf(opponent);
        played++;
        scoreDifference += own - against;
        if (own > against) {
            wins++;
        } else if (own < against) {
            losses++;
        } else {
            draws++;
        }
    }

    public void applyPoints(int winPoints, int drawPoints, int lossPoints) {
        points = wins * winPoints + draws * drawPoints + losses * lossPoints;
    }

    public Team getTeam() { return team; }
    public int getPlayed() { return played; }
    public int getPoints() { return points; }
    public int getWins() { return wins; }
    public int getDraws() { return draws; }
    public int getLosses() { return losses; }
    public int getScoreDifference() { return scoreDifference; }

    @Override
    public String toString() {
        return String.format("%-18s %3d %3d %3d %3d %+5d %5d", team.getTeamName(), played, wins, draws, losses,
                scoreDifference, points);
    }
}
