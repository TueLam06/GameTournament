package gtms.model;

import gtms.util.Validator;

import java.io.Serializable;

/** Chỉ lưu mã đội (không lưu Team) để Team.addPlayer() biết tuyển thủ đã thuộc đội khác chưa. */
public class Player implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String playerId;
    private String nickname;
    private final String fullName;
    private String position;
    private final String gameId;
    private String currentTeamId; // null khi chưa thuộc đội nào

    public Player(String playerId, String nickname, String fullName, String position, String gameId) {
        this.playerId = Validator.requireText(playerId, "mã tuyển thủ");
        this.nickname = Validator.requireText(nickname, "nickname");
        this.fullName = Validator.requireText(fullName, "họ tên");
        this.position = Validator.requireText(position, "vị trí");
        this.gameId = Validator.requireText(gameId, "Game ID");
    }

    public void updateInfo(String nickname, String position) {
        this.nickname = Validator.requireText(nickname, "nickname");
        this.position = Validator.requireText(position, "vị trí");
    }

    // Phạm vi package: code ngoài gtms.model không gọi được; trong package chỉ Team dùng để gán/gỡ đội
    void joinTeam(String teamId) { this.currentTeamId = teamId; }
    void leaveTeam() { this.currentTeamId = null; }

    @Override public String getId() { return playerId; }
    public String getPlayerId() { return playerId; }
    public String getNickname() { return nickname; }
    public String getFullName() { return fullName; }
    public String getPosition() { return position; }
    public String getGameId() { return gameId; }
    public String getCurrentTeamId() { return currentTeamId; }
    public boolean hasTeam() { return currentTeamId != null; }

    @Override
    public String toString() {
        return String.format("%-6s %-15s %-22s %-11s %s", playerId, nickname, fullName, position, gameId);
    }
}
