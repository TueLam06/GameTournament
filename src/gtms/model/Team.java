package gtms.model;

import gtms.exception.TournamentException;
import gtms.util.Validator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Team implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String teamId;
    private String teamName;
    private String logo;
    private String region;
    private final String game;
    private TeamStatus status = TeamStatus.ACTIVE;
    private final List<Player> players = new ArrayList<>();

    public Team(String teamId, String teamName, String logo, String region, String game) {
        this.teamId = Validator.requireText(teamId, "mã đội");
        this.teamName = Validator.requireText(teamName, "tên đội");
        this.logo = logo == null ? "" : logo.trim();
        this.region = Validator.requireText(region, "khu vực");
        this.game = Validator.requireText(game, "tựa game");
    }

    public void addPlayer(Player player) throws TournamentException {
        ensureActive();
        if (player == null) {
            throw new IllegalArgumentException("Thiếu thông tin tuyển thủ");
        }
        if (players.contains(player)) {
            throw new TournamentException(player.getNickname() + " đã có trong đội " + teamName);
        }
        if (player.hasTeam()) {
            throw new TournamentException(player.getNickname() + " đang thuộc đội khác ("
                    + player.getCurrentTeamId() + "), phải rời đội cũ trước");
        }
        players.add(player);
        player.joinTeam(teamId);
    }

    public Player removePlayer(String playerId) throws TournamentException {
        Player player = findPlayer(playerId).orElseThrow(() ->
                new TournamentException("Không có tuyển thủ " + playerId + " trong đội " + teamName));
        players.remove(player);
        player.leaveTeam();
        return player;
    }

    public Optional<Player> findPlayer(String playerId) {
        return players.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst();
    }

    /** Chỉ để hỏi trước; Tournament.addRegistration() kiểm tra lại và báo lỗi chi tiết. */
    public boolean isEligible(Tournament tournament) {
        return status == TeamStatus.ACTIVE
                && game.equalsIgnoreCase(tournament.getGame())
                && players.size() >= tournament.getTeamSize();
    }

    /** Tuyển thủ chỉ rời đội, không bị xóa. Nơi gọi phải kiểm tra đội không còn dự giải. */
    public void disband() throws TournamentException {
        ensureActive();
        players.forEach(Player::leaveTeam);
        players.clear();
        status = TeamStatus.DISBANDED;
    }

    public void updateInfo(String teamName, String logo, String region) throws TournamentException {
        ensureActive();
        this.teamName = Validator.requireText(teamName, "tên đội");
        this.logo = logo == null ? "" : logo.trim();
        this.region = Validator.requireText(region, "khu vực");
    }

    private void ensureActive() throws TournamentException {
        if (status != TeamStatus.ACTIVE) {
            throw new TournamentException("Đội " + teamName + " đã giải thể");
        }
    }

    @Override public String getId() { return teamId; }
    public String getTeamId() { return teamId; }
    public String getTeamName() { return teamName; }
    public String getLogo() { return logo; }
    public String getRegion() { return region; }
    public String getGame() { return game; }
    public TeamStatus getStatus() { return status; }
    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }

    @Override
    public String toString() {
        return String.format("%-6s %-18s %-8s %-18s %d tuyển thủ, %s",
                teamId, teamName, region, game, players.size(), status);
    }
}
