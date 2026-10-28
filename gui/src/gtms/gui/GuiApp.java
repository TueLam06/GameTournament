package gtms.gui;

import gtms.app.DemoScenario;
import gtms.app.SampleData;
import gtms.exception.TournamentException;
import gtms.model.Admin;
import gtms.model.Match;
import gtms.model.MatchStatus;
import gtms.model.Player;
import gtms.model.RankingEntry;
import gtms.model.Registration;
import gtms.model.Team;
import gtms.model.TeamManager;
import gtms.model.Tournament;
import gtms.model.TournamentFormat;
import gtms.model.User;
import gtms.repository.DataStore;
import gtms.service.TournamentService;
import gtms.test.CoreTests;
import gtms.util.AppClock;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Giao diện Swing để demo khi báo cáo, dùng chung dữ liệu data/gtms.dat với bản console.
 * Chỉ gọi các lớp model/service, không chứa luật nghiệp vụ riêng.
 */
public class GuiApp extends JFrame {
    private static final Path DATA_FILE = Paths.get("data", "gtms.dat");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter SHORT_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private DataStore store;
    private TournamentService service;
    private User currentUser;

    private final DefaultTableModel tournamentModel = readOnlyModel("Mã", "Tên giải", "Thể thức", "Slot", "Trạng thái");
    private final DefaultTableModel registrationModel = readOnlyModel("Mã đơn", "Đội", "Trạng thái", "Lý do từ chối");
    private final DefaultTableModel matchModel = readOnlyModel("Mã trận", "Vòng", "Giờ", "Đội A", "Tỉ số", "Đội B", "Trạng thái");
    private final DefaultTableModel rankingModel = readOnlyModel("#", "Đội", "Trận", "Thắng", "Hòa", "Thua", "Hiệu số", "Điểm");
    private final DefaultTableModel playerModel = readOnlyModel("Mã", "Nickname", "Họ tên", "Vị trí", "Game ID");
    private final JTable tournamentTable = table(tournamentModel);
    private final JTable registrationTable = table(registrationModel);
    private final JTable matchTable = table(matchModel);
    private final JTable playerTable = table(playerModel);
    private final JLabel userLabel = new JLabel();
    private final JLabel infoLabel = new JLabel();
    private final JLabel teamLabel = new JLabel();
    private final JButton loginButton = new JButton();
    private final JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

    public GuiApp() {
        super("Game Tournament - giao diện chạy thử");
        loadData();
        buildLayout();
        refreshAll();
        if (tournamentTable.getRowCount() > 0) {
            tournamentTable.setRowSelectionInterval(0, 0);
        }
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAndExit();
            }
        });
        setSize(1250, 720);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // không đặt được giao diện hệ điều hành thì dùng giao diện mặc định của Swing
        }
        SwingUtilities.invokeLater(() -> new GuiApp().setVisible(true));
    }

    private void loadData() {
        try {
            store = Files.exists(DATA_FILE) ? DataStore.load(DATA_FILE) : sampleData();
        } catch (IOException e) {
            String message = "Không đọc được " + DATA_FILE + " (" + e.getMessage() + ").";
            try {
                message += "\nFile cũ được giữ lại ở " + DataStore.backupBroken(DATA_FILE) + ".\nChương trình dùng dữ liệu mẫu.";
            } catch (IOException moveError) {
                // không đổi tên được thì thoát, tránh ghi đè lên file cũ khi đóng cửa sổ
                JOptionPane.showMessageDialog(null, message + "\nKhông sao lưu được file, chương trình sẽ thoát.",
                        "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            JOptionPane.showMessageDialog(null, message, "Lỗi dữ liệu", JOptionPane.WARNING_MESSAGE);
            store = sampleData();
        }
        service = new TournamentService(store);
    }

    private static DataStore sampleData() {
        try {
            return SampleData.create();
        } catch (TournamentException e) {
            throw new IllegalStateException("Dữ liệu mẫu bị lỗi: " + e.getMessage(), e);
        }
    }

    private void buildLayout() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(userLabel);
        toolbar.addSeparator();
        loginButton.addActionListener(e -> {
            if (currentUser == null) {
                run(this::login);
            } else {
                currentUser.logout();
                currentUser = null;
                refreshAll();
            }
        });
        toolbar.add(loginButton);
        toolbar.add(button("Đăng ký tài khoản", this::registerAccount));
        toolbar.addSeparator();
        toolbar.add(button("Chạy test", () -> showOutput("Kết quả kiểm thử", CoreTests::runAll)));
        toolbar.add(button("Chạy demo", () -> showOutput("Kịch bản demo", DemoScenario::run)));
        toolbar.addSeparator();
        toolbar.add(button("Lưu dữ liệu", () -> {
            store.save(DATA_FILE);
            info("Đã lưu vào " + DATA_FILE.toAbsolutePath());
        }));
        toolbar.add(button("Về dữ liệu mẫu", this::resetSampleData));

        int[] widths = {70, 210, 100, 50, 120};
        for (int i = 0; i < widths.length; i++) {
            tournamentTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        tournamentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshDetail();
            }
        });

        JPanel teamPanel = new JPanel(new BorderLayout(0, 6));
        teamPanel.add(teamLabel, BorderLayout.NORTH);
        teamPanel.add(new JScrollPane(playerTable), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Đơn đăng ký", new JScrollPane(registrationTable));
        tabs.addTab("Lịch thi đấu", new JScrollPane(matchTable));
        tabs.addTab("Bảng xếp hạng", new JScrollPane(table(rankingModel)));
        tabs.addTab("Đội của tôi", teamPanel);

        JPanel right = new JPanel(new BorderLayout(0, 6));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        right.add(infoLabel, BorderLayout.NORTH);
        right.add(tabs, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tournamentTable), right);
        split.setDividerLocation(470);

        actionPanel.setBorder(BorderFactory.createTitledBorder("Thao tác"));
        add(toolbar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }

    // Nút thao tác thay đổi theo vai trò đang đăng nhập
    private void rebuildActions() {
        actionPanel.removeAll();
        if (currentUser instanceof Admin) {
            Admin admin = (Admin) currentUser;
            actionPanel.add(button("Tạo giải", () -> createTournament(admin)));
            actionPanel.add(button("Mở đăng ký", () -> admin.openRegistration(requireTournament())));
            actionPanel.add(button("Duyệt đơn", () -> admin.approveRegistration(requireRegistration())));
            actionPanel.add(button("Từ chối đơn", () -> rejectRegistration(admin)));
            actionPanel.add(button("Đóng đăng ký", () -> admin.closeRegistration(requireTournament())));
            actionPanel.add(button("Sinh lịch", () -> generateSchedule(admin)));
            actionPanel.add(button("Nhập kết quả", () -> enterResult(admin)));
            actionPanel.add(button("Xử thua", () -> walkover(admin)));
            actionPanel.add(button("Hủy giải", () -> {
                Tournament t = requireTournament();
                if (confirm("Hủy giải " + t.getName() + "?")) admin.cancelTournament(t);
            }));
            actionPanel.add(button("Xóa giải", () -> {
                Tournament t = requireTournament();
                if (confirm("Xóa hẳn giải " + t.getName() + "?")) service.deleteTournament(admin, t);
            }));
            actionPanel.add(button("Xuất CSV", () -> {
                Tournament t = requireTournament();
                info("Đã xuất " + service.exportRankingCsv(t,
                        Paths.get("data", "ranking_" + t.getTournamentId() + ".csv")).toAbsolutePath());
            }));
            actionPanel.add(button("Khóa / mở tài khoản", () -> lockOrUnlock(admin)));
        } else if (currentUser instanceof TeamManager) {
            TeamManager manager = (TeamManager) currentUser;
            actionPanel.add(button("Tạo đội", () -> createTeam(manager)));
            actionPanel.add(button("Thêm tuyển thủ", () -> addPlayer(manager)));
            actionPanel.add(button("Xóa tuyển thủ", () -> removePlayer(manager)));
            actionPanel.add(button("Đăng ký giải đang chọn", () -> registerTournament(manager)));
            actionPanel.add(button("Hủy đăng ký", () -> cancelRegistration(manager)));
            actionPanel.add(button("Thành tích đội", () -> showTeamSummary(manager)));
        } else {
            actionPanel.add(new JLabel("Đăng nhập để thao tác. Mật khẩu mẫu: " + SampleData.DEFAULT_PASSWORD));
        }
        actionPanel.revalidate();
        actionPanel.repaint();
    }

    private void refreshAll() {
        String selectedId = selectedTournament().map(Tournament::getTournamentId).orElse(null);
        tournamentModel.setRowCount(0);
        for (Tournament t : store.tournaments().findAll()) {
            tournamentModel.addRow(new Object[]{t.getTournamentId(), t.getName(), t.getFormat().getLabel(),
                    t.countOccupiedSlots() + "/" + t.getMaxTeams(), t.getStatus().getLabel()});
        }
        for (int row = 0; row < tournamentModel.getRowCount(); row++) {
            if (tournamentModel.getValueAt(row, 0).equals(selectedId)) {
                tournamentTable.setRowSelectionInterval(row, row);
            }
        }
        refreshDetail();
        refreshTeam();
        userLabel.setText(currentUser == null ? " Khách  "
                : " " + currentUser.getName() + " (" + currentUser.getRole() + ")  ");
        loginButton.setText(currentUser == null ? "Đăng nhập" : "Đăng xuất");
        rebuildActions();
    }

    private void refreshDetail() {
        registrationModel.setRowCount(0);
        matchModel.setRowCount(0);
        rankingModel.setRowCount(0);
        Optional<Tournament> selected = selectedTournament();
        if (selected.isEmpty()) {
            infoLabel.setText("Chọn một giải ở bảng bên trái.");
            return;
        }
        Tournament t = selected.get();
        String champion = t.getChampion().map(c -> " | <b>Vô địch: " + c.getTeamName() + "</b>").orElse("");
        infoLabel.setText("<html><b>" + t.getName() + "</b> (" + t.getTournamentId() + ") | " + t.getGame()
                + " | " + t.getFormat().getLabel() + " | " + t.getTeamSize() + " người/đội | Đăng ký: "
                + t.getRegistrationStart().format(TIME) + " - " + t.getRegistrationEnd().format(TIME)
                + "<br>Giải thưởng: " + t.getPrize() + " | Trạng thái: " + t.getStatus().getLabel() + champion
                + "</html>");
        for (Registration r : t.getRegistrations()) {
            registrationModel.addRow(new Object[]{r.getRegistrationId(), r.getTeam().getTeamName(),
                    r.getStatus().getLabel(), r.getRejectionReason() == null ? "" : r.getRejectionReason()});
        }
        for (Match m : t.getMatches()) {
            matchModel.addRow(new Object[]{m.getMatchId(), m.getRound(),
                    m.getScheduledTime() == null ? "" : m.getScheduledTime().format(SHORT_TIME),
                    teamName(m.getTeamA()), m.getStatus().isPlayed() ? m.getScoreA() + " - " + m.getScoreB() : "vs",
                    teamName(m.getTeamB()), m.getStatus().getLabel()});
        }
        List<RankingEntry> standings = t.getRanking().getStandings();
        for (int i = 0; i < standings.size(); i++) {
            RankingEntry e = standings.get(i);
            rankingModel.addRow(new Object[]{i + 1, e.getTeam().getTeamName(), e.getPlayed(), e.getWins(),
                    e.getDraws(), e.getLosses(), String.format("%+d", e.getScoreDifference()), e.getPoints()});
        }
    }

    private void refreshTeam() {
        playerModel.setRowCount(0);
        if (!(currentUser instanceof TeamManager)) {
            teamLabel.setText("Đăng nhập bằng tài khoản đội trưởng để xem đội.");
            return;
        }
        TeamManager manager = (TeamManager) currentUser;
        if (!manager.hasActiveTeam()) {
            teamLabel.setText("Bạn chưa có đội. Bấm \"Tạo đội\" ở dưới.");
            return;
        }
        Team team = manager.getManagedTeam();
        teamLabel.setText("<html><b>" + team.getTeamName() + "</b> (" + team.getTeamId() + ") | " + team.getRegion()
                + " | " + team.getGame() + " | " + team.getPlayers().size() + " tuyển thủ</html>");
        for (Player p : team.getPlayers()) {
            playerModel.addRow(new Object[]{p.getPlayerId(), p.getNickname(), p.getFullName(), p.getPosition(),
                    p.getGameId()});
        }
    }

    private void login() throws TournamentException {
        JComboBox<String> email = new JComboBox<>(store.users().findAll().stream().map(User::getEmail)
                .toArray(String[]::new));
        email.setEditable(true);
        JPasswordField password = new JPasswordField(SampleData.DEFAULT_PASSWORD);
        if (form("Đăng nhập", "Email", email, "Mật khẩu", password)) {
            currentUser = service.login(String.valueOf(email.getSelectedItem()), new String(password.getPassword()));
        }
    }

    private void registerAccount() throws TournamentException {
        JTextField name = new JTextField();
        JTextField email = new JTextField();
        JPasswordField password = new JPasswordField();
        if (form("Đăng ký tài khoản đội trưởng", "Họ tên", name, "Email", email,
                "Mật khẩu (ít nhất 6 ký tự)", password)) {
            TeamManager m = service.registerAccount(name.getText(), email.getText(), new String(password.getPassword()));
            info("Đã tạo tài khoản " + m.getUserId() + ". Hãy đăng nhập.");
        }
    }

    private void resetSampleData() {
        if (confirm("Bỏ toàn bộ dữ liệu hiện tại và quay về dữ liệu mẫu?")) {
            store = sampleData();
            service = new TournamentService(store);
            currentUser = null;
        }
    }

    private void createTournament(Admin admin) throws TournamentException {
        LocalDateTime now = AppClock.now();
        JTextField name = new JTextField("Giải thử nghiệm");
        JTextField game = new JTextField(SampleData.GAME);
        JComboBox<TournamentFormat> format = new JComboBox<>(TournamentFormat.values());
        JTextField maxTeams = new JTextField("4");
        JTextField teamSize = new JTextField("5");
        JTextField start = new JTextField(now.format(TIME));
        JTextField end = new JTextField(now.plusDays(7).format(TIME));
        JTextField prize = new JTextField("1.000.000 VNĐ");
        if (form("Tạo giải đấu", "Tên giải", name, "Tựa game", game, "Thể thức", format, "Số đội tối đa", maxTeams,
                "Số tuyển thủ mỗi đội", teamSize, "Mở đăng ký (dd/MM/yyyy HH:mm)", start,
                "Đóng đăng ký (dd/MM/yyyy HH:mm)", end, "Giải thưởng", prize)) {
            Tournament t = service.createTournament(admin, name.getText(), game.getText(),
                    (TournamentFormat) format.getSelectedItem(), Integer.parseInt(maxTeams.getText().trim()),
                    Integer.parseInt(teamSize.getText().trim()), LocalDateTime.parse(start.getText().trim(), TIME),
                    LocalDateTime.parse(end.getText().trim(), TIME), prize.getText());
            info("Đã tạo giải " + t.getTournamentId() + " ở trạng thái Nháp. Bấm \"Mở đăng ký\" để các đội đăng ký.");
        }
    }

    private void rejectRegistration(Admin admin) throws TournamentException {
        Registration r = requireRegistration();
        String reason = JOptionPane.showInputDialog(this, "Lý do từ chối đơn " + r.getRegistrationId() + ":");
        if (reason != null) {
            admin.rejectRegistration(r, reason);
        }
    }

    private void generateSchedule(Admin admin) throws TournamentException {
        Tournament t = requireTournament();
        if (t.getMatches().isEmpty()) {
            admin.generateSchedule(t);
        } else if (confirm("Giải đã có lịch. Xóa lịch cũ và sinh lại?")) {
            admin.regenerateSchedule(t);
        }
    }

    private void enterResult(Admin admin) throws TournamentException {
        Match m = requireMatch();
        if (m.getTeamA() == null || m.getTeamB() == null) {
            throw new IllegalStateException("Trận " + m.getMatchId() + " chưa đủ hai đội");
        }
        if (m.getStatus() == MatchStatus.FINISHED && !confirm("Trận đã có kết quả " + m.getScoreA() + " - "
                + m.getScoreB() + ". Sửa lại và tính lại bảng xếp hạng?")) {
            return;
        }
        JTextField scoreA = new JTextField();
        JTextField scoreB = new JTextField();
        if (form("Nhập kết quả " + m.getMatchId(), m.getTeamA().getTeamName(), scoreA,
                m.getTeamB().getTeamName(), scoreB)) {
            admin.updateMatchResult(m, optionalInt(scoreA.getText()), optionalInt(scoreB.getText()));
        }
    }

    private void walkover(Admin admin) throws TournamentException {
        Match m = requireMatch();
        if (m.getTeamA() == null || m.getTeamB() == null) {
            throw new IllegalStateException("Trận " + m.getMatchId() + " chưa đủ hai đội");
        }
        Object[] options = {m.getTeamA().getTeamName(), m.getTeamB().getTeamName()};
        int choice = JOptionPane.showOptionDialog(this, "Đội nào bỏ cuộc?", "Xử thua " + m.getMatchId(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice >= 0) {
            admin.recordWalkover(m, choice == 0 ? m.getTeamA() : m.getTeamB());
        }
    }

    private void lockOrUnlock(Admin admin) throws TournamentException {
        JComboBox<User> users = new JComboBox<>(store.users().findAll().toArray(new User[0]));
        Object[] options = {"Khóa", "Mở khóa"};
        int choice = JOptionPane.showOptionDialog(this, users, "Khóa / mở khóa tài khoản",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        User user = (User) users.getSelectedItem();
        if (choice == 0) {
            admin.lockUser(user);
        } else if (choice == 1) {
            admin.unlockUser(user);
        }
        if (choice >= 0) {
            info(user.getEmail() + ": " + user.getAccountStatus());
        }
    }

    private void createTeam(TeamManager manager) throws TournamentException {
        JTextField name = new JTextField();
        JTextField logo = new JTextField();
        JTextField region = new JTextField("Hà Nội");
        JTextField game = new JTextField(SampleData.GAME);
        if (form("Tạo đội", "Tên đội", name, "Logo", logo, "Khu vực", region, "Tựa game", game)) {
            service.createTeam(manager, name.getText(), logo.getText(), region.getText(), game.getText());
        }
    }

    private void addPlayer(TeamManager manager) throws TournamentException {
        JTextField nickname = new JTextField();
        JTextField fullName = new JTextField();
        JTextField position = new JTextField("Flex");
        JTextField gameId = new JTextField();
        if (form("Thêm tuyển thủ", "Nickname", nickname, "Họ tên", fullName, "Vị trí", position,
                "ID trong game", gameId)) {
            service.addNewPlayer(manager, nickname.getText(), fullName.getText(), position.getText(), gameId.getText());
        }
    }

    private void removePlayer(TeamManager manager) throws TournamentException {
        int row = playerTable.getSelectedRow();
        if (row < 0) {
            throw new IllegalStateException("Chọn một tuyển thủ ở tab \"Đội của tôi\"");
        }
        String playerId = (String) playerModel.getValueAt(row, 0);
        if (confirm("Xóa tuyển thủ " + playerModel.getValueAt(row, 1) + " khỏi đội?")) {
            service.removePlayer(manager, playerId);
        }
    }

    private void registerTournament(TeamManager manager) throws TournamentException {
        Tournament t = requireTournament();
        if (!manager.hasActiveTeam()) {
            throw new TournamentException("Bạn chưa có đội, hãy tạo đội trước");
        }
        Team team = manager.getManagedTeam();
        Optional<String> problem = t.registrationProblem(team);
        if (problem.isPresent()) {
            throw new TournamentException(problem.get());
        }
        if (confirm("<html>Giải yêu cầu " + t.getTeamSize() + " tuyển thủ, đã dùng "
                + t.countOccupiedSlots() + "/" + t.getMaxTeams() + " slot. Đội " + team.getTeamName() + " có "
                + team.getPlayers().size() + " tuyển thủ, đủ điều kiện.<br>Đăng ký cả đội vào giải "
                + t.getName() + "?</html>")) {
            Registration r = manager.registerTournament(t);
            info("Đã gửi đơn " + r.getRegistrationId());
        }
    }

    private void cancelRegistration(TeamManager manager) throws TournamentException {
        Tournament t = requireTournament();
        Registration r = t.findActiveRegistration(manager.getManagedTeam())
                .orElseThrow(() -> new TournamentException("Đội không có đơn còn hiệu lực ở giải này"));
        if (confirm("Hủy đơn " + r.getRegistrationId() + "?")) {
            manager.cancelRegistration(r);
        }
    }

    private void showTeamSummary(TeamManager manager) throws TournamentException {
        if (manager.getManagedTeam() == null) {
            throw new TournamentException("Bạn chưa có đội");
        }
        Team team = manager.getManagedTeam();
        StringBuilder text = new StringBuilder(team.getTeamName() + ": " + service.getTeamSummary(team) + "\n\n");
        service.getTeamHistory(team).forEach(m -> text.append(m).append('\n'));
        showText("Thành tích " + team.getTeamName(), text.toString());
    }

    /** Mọi nút bấm chạy qua đây: lỗi hiện thành hộp thoại, sau đó vẽ lại các bảng. */
    private void run(Action action) {
        try {
            action.run();
        } catch (TournamentException e) {
            error("Lỗi nghiệp vụ [" + e.getClass().getSimpleName() + "]\n" + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException | DateTimeParseException e) {
            error("Dữ liệu không hợp lệ\n" + e.getMessage());
        } catch (Exception e) {
            error("Lỗi: " + e);
        }
        refreshAll();
    }

    /** Chạy test hoặc demo và gom những gì chúng in ra console vào một cửa sổ. */
    private void showOutput(String title, Action action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } catch (Exception e) {
            buffer.writeBytes(("\nLỗi: " + e).getBytes(StandardCharsets.UTF_8));
        } finally {
            System.setOut(original);
            AppClock.useSystemClock();
        }
        showText(title, buffer.toString(StandardCharsets.UTF_8));
    }

    private void showText(String title, String text) {
        JTextArea area = new JTextArea(text, 32, 115);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), title, JOptionPane.PLAIN_MESSAGE);
    }

    private void saveAndExit() {
        try {
            store.save(DATA_FILE);
        } catch (IOException e) {
            if (!confirm("Không lưu được dữ liệu (" + e.getMessage() + "). Vẫn thoát?")) {
                return;
            }
        }
        dispose();
        System.exit(0);
    }

    private Optional<Tournament> selectedTournament() {
        int row = tournamentTable.getSelectedRow();
        return row < 0 ? Optional.empty() : store.tournaments().findById((String) tournamentModel.getValueAt(row, 0));
    }

    private Tournament requireTournament() {
        return selectedTournament().orElseThrow(() -> new IllegalStateException("Chọn một giải ở bảng bên trái"));
    }

    private Registration requireRegistration() {
        int row = registrationTable.getSelectedRow();
        if (row < 0) {
            throw new IllegalStateException("Chọn một đơn ở tab \"Đơn đăng ký\"");
        }
        return requireTournament().findRegistration((String) registrationModel.getValueAt(row, 0)).orElseThrow();
    }

    private Match requireMatch() {
        int row = matchTable.getSelectedRow();
        if (row < 0) {
            throw new IllegalStateException("Chọn một trận ở tab \"Lịch thi đấu\"");
        }
        return requireTournament().findMatch((String) matchModel.getValueAt(row, 0)).orElseThrow();
    }

    /** Ô để trống trả về null để Match báo "Tỉ số không được để trống". */
    private static Integer optionalInt(String text) {
        return text.isBlank() ? null : Integer.valueOf(text.trim());
    }

    private static String teamName(Team team) {
        return team == null ? "(chờ)" : team.getTeamName();
    }

    /** Tham số xen kẽ: nhãn, ô nhập, nhãn, ô nhập... */
    private boolean form(String title, Object... labelsAndFields) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 6));
        for (int i = 0; i < labelsAndFields.length; i += 2) {
            panel.add(new JLabel((String) labelsAndFields[i]));
            panel.add((JComponent) labelsAndFields[i + 1]);
        }
        return JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private JButton button(String text, Action action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> run(action));
        return b;
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Xác nhận", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        return table;
    }
}
