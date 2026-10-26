package gtms.strategy;

import gtms.model.Match;
import gtms.model.Team;
import gtms.model.Tournament;

import java.io.Serializable;
import java.util.List;

/** teams: các đội đã được khóa danh sách, theo thứ tự đăng ký. */
public interface ScheduleStrategy extends Serializable {
    List<Match> generate(Tournament tournament, List<Team> teams);
}
