package gtms.service;

import gtms.exception.TournamentException;
import gtms.model.Admin;
import gtms.model.Match;
import gtms.model.Player;
import gtms.model.RankingEntry;
import gtms.model.Registration;
import gtms.model.Team;
import gtms.model.TeamManager;
import gtms.model.Tournament;
import gtms.model.TournamentFormat;
import gtms.model.TournamentStatus;
import gtms.model.User;
import gtms.repository.DataStore;
import gtms.util.IdGenerator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Các thao tác cần duyệt toàn bộ dữ liệu: kiểm tra email, tên đội không trùng, tìm kiếm, lịch sử. */
public class TournamentService {
    private final DataStore store;

    public TournamentService(DataStore store) {
        this.store = store;
    }

    public TeamManager registerAccount(String name, String email, String password) throws TournamentException {
        if (findUserByEmail(email) != null) {
            throw new TournamentException("Email " + email + " đã được sử dụng");
        }
        TeamManager manager = new TeamManager(IdGenerator.next("TM"), name, email, password);
        store.users().add(manager);
        return manager;
    }

    public Admin addAdmin(String name, String email, String password) throws TournamentException {
        if (findUserByEmail(email) != null) {
            throw new TournamentException("Email " + email + " đã được sử dụng");
        }
        Admin admin = new Admin(IdGenerator.next("AD"), name, email, password);
        store.users().add(admin);
        return admin;
    }

    public User login(String email, String password) throws TournamentException {
        User user = findUserByEmail(email);
        if (user == null) {
            throw new TournamentException("Sai email hoặc mật khẩu");
        }
        user.login(password);
        return user;
    }

    private User findUserByEmail(String email) {
        String key = email == null ? "" : email.trim().toLowerCase();
        return store.users().findFirst(u -> u.getEmail().equals(key)).orElse(null);
    }

    public Team createTeam(TeamManager manager, String name, String logo, String region, String game)
            throws TournamentException {
        boolean duplicated = store.teams().findFirst(t -> t.getTeamName().equalsIgnoreCase(name.trim())).isPresent();
        if (duplicated) {
            throw new TournamentException("Tên đội " + name + " đã tồn tại");
        }
        Team team = manager.createTeam(name, logo, region, game);
        store.teams().add(team);
        return team;
    }

    public void updateTeam(TeamManager manager, String name, String logo, String region) throws TournamentException {
        Team own = manager.getManagedTeam();
        boolean duplicated = store.teams()
                .findFirst(t -> t != own && t.getTeamName().equalsIgnoreCase(name.trim())).isPresent();
        if (duplicated) {
            throw new TournamentException("Tên đội " + name + " đã tồn tại");
        }
        manager.updateTeam(name, logo, region);
    }

    public void disbandTeam(TeamManager manager) throws TournamentException {
        manager.disbandTeam(store.tournaments().findAll());
    }

    public Player addNewPlayer(TeamManager manager, String nickname, String fullName, String position,
                               String gameId) throws TournamentException {
        Player player = new Player(IdGenerator.next("P"), nickname, fullName, position, gameId);
        manager.addPlayer(player);
        store.players().add(player);
        return player;
    }

    /**
     * Không cho xóa tuyển thủ khi đội đang thi đấu, hoặc khi người đó có tên trong một đơn đăng ký còn hiệu lực:
     * nếu không, tuyển thủ có thể sang đội khác và có tên trong hai đội ở cùng một giải.
     */
    public Player removePlayer(TeamManager manager, String playerId) throws TournamentException {
        Team team = manager.getManagedTeam();
        for (Tournament t : store.tournaments().findAll()) {
            if (t.getStatus() == TournamentStatus.ONGOING && t.getConfirmedTeams().contains(team)) {
                throw new TournamentException("Đội đang thi đấu, phải hoàn tất giải trước khi xóa tuyển thủ");
            }
            if (t.getStatus() == TournamentStatus.FINISHED) {
                continue;
            }
            Optional<Registration> inRoster = t.findActiveRegistration(team).filter(r ->
                    r.getRegisteredPlayers().stream().anyMatch(p -> p.getPlayerId().equals(playerId)));
            if (inRoster.isPresent()) {
                throw new TournamentException("Tuyển thủ " + playerId + " có tên trong đơn "
                        + inRoster.get().getRegistrationId() + " của giải " + t.getName()
                        + "; hãy hủy đơn hoặc chờ giải kết thúc");
            }
        }
        return manager.removePlayer(playerId);
    }

    public Tournament createTournament(Admin admin, String name, String game, TournamentFormat format,
                                       int maxTeams, int teamSize, LocalDateTime start, LocalDateTime end,
                                       String prize) throws TournamentException {
        Tournament tournament = admin.createTournament(name, game, format, maxTeams, teamSize, start, end, prize);
        store.tournaments().add(tournament);
        return tournament;
    }

    public void deleteTournament(Admin admin, Tournament tournament) throws TournamentException {
        admin.checkCanDelete(tournament);
        store.tournaments().remove(tournament.getTournamentId());
    }

    /** Tham số null hoặc rỗng thì bỏ qua điều kiện đó. */
    public List<Tournament> searchTournaments(String keyword, TournamentStatus status, String game) {
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        return store.tournaments().findBy(t ->
                t.getName().toLowerCase().contains(key)
                        && (status == null || t.getStatus() == status)
                        && (game == null || game.isBlank() || t.getGame().equalsIgnoreCase(game.trim())));
    }

    public Map<TournamentStatus, Long> countByStatus() {
        return store.tournaments().findAll().stream()
                .collect(Collectors.groupingBy(Tournament::getStatus, Collectors.counting()));
    }

    /** Các trận đã có kết quả của đội ở mọi giải, sắp theo thời gian. */
    public List<Match> getTeamHistory(Team team) {
        return store.tournaments().findAll().stream()
                .flatMap(t -> t.getMatchesOf(team).stream())
                .filter(m -> m.getStatus().isPlayed())
                .sorted(Comparator.comparing(Match::getScheduledTime))
                .collect(Collectors.toList());
    }

    public TeamSummary getTeamSummary(Team team) {
        TeamSummary summary = new TeamSummary();
        summary.tournaments = (int) store.tournaments().findAll().stream()
                .filter(t -> t.getConfirmedTeams().contains(team)).count();
        for (Match m : getTeamHistory(team)) {
            Team opponent = m.getTeamA() == team ? m.getTeamB() : m.getTeamA();
            int own = m.scoreOf(team);
            int against = m.scoreOf(opponent);
            summary.played++;
            if (own > against) summary.wins++;
            else if (own < against) summary.losses++;
            else summary.draws++;
        }
        summary.titles = (int) store.tournaments().findAll().stream()
                .filter(t -> t.getChampion().filter(c -> c == team).isPresent()).count();
        return summary;
    }

    public static class TeamSummary {
        private int tournaments;
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int titles;

        public int getTournaments() { return tournaments; }
        public int getPlayed() { return played; }
        public int getWins() { return wins; }
        public int getDraws() { return draws; }
        public int getLosses() { return losses; }
        public int getTitles() { return titles; }

        @Override
        public String toString() {
            return String.format("%d giải, %d trận: %d thắng, %d hòa, %d thua, %d chức vô địch",
                    tournaments, played, wins, draws, losses, titles);
        }
    }

    public List<Registration> getRegistrationsOf(Team team) {
        List<Registration> result = new ArrayList<>();
        for (Tournament t : store.tournaments().findAll()) {
            t.getRegistrations().stream().filter(r -> r.getTeam() == team).forEach(result::add);
        }
        return result;
    }

    /** Ghi thêm BOM ở đầu file để Excel đọc đúng tiếng Việt. */
    public Path exportRankingCsv(Tournament tournament, Path file) throws IOException {
        Function<RankingEntry, String> toRow = e -> String.join(",", e.getTeam().getTeamName(),
                String.valueOf(e.getPlayed()), String.valueOf(e.getWins()), String.valueOf(e.getDraws()),
                String.valueOf(e.getLosses()), String.valueOf(e.getScoreDifference()),
                String.valueOf(e.getPoints()));
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write('﻿');
            writer.write("Hạng,Đội,Trận,Thắng,Hòa,Thua,Hiệu số,Điểm");
            writer.newLine();
            int rank = 1;
            for (RankingEntry entry : tournament.getRanking().getStandings()) {
                writer.write(rank++ + "," + toRow.apply(entry));
                writer.newLine();
            }
        }
        return file;
    }

    public DataStore getStore() {
        return store;
    }
}
