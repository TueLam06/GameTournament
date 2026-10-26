package gtms.strategy;

import gtms.model.Match;
import gtms.model.Ranking;

import java.io.Serializable;
import java.util.List;

public interface RankingCalculator extends Serializable {
    void recalculate(Ranking ranking, List<Match> matches);
}
