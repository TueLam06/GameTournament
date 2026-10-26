package gtms.model;

import gtms.exception.TournamentException;
import gtms.util.PasswordUtil;
import gtms.util.Validator;

import java.io.Serializable;

/** Phần chung của TeamManager và Admin. Không lưu mật khẩu gốc, chỉ lưu salt và kết quả băm. */
public abstract class User implements Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String name;
    private final String email;
    private final String salt;
    private final String passwordHash;
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    private transient boolean loggedIn;

    protected User(String userId, String name, String email, String rawPassword) {
        this.userId = Validator.requireText(userId, "userId");
        this.name = Validator.requireText(name, "name");
        this.email = Validator.requireEmail(email);
        if (!PasswordUtil.isStrong(rawPassword)) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số");
        }
        this.salt = PasswordUtil.newSalt();
        this.passwordHash = PasswordUtil.hash(rawPassword, salt);
    }

    public boolean verifyPassword(String password) {
        return password != null && passwordHash.equals(PasswordUtil.hash(password, salt));
    }

    public void login(String password) throws TournamentException {
        if (!verifyPassword(password)) {
            throw new TournamentException("Sai email hoặc mật khẩu");
        }
        if (accountStatus == AccountStatus.LOCKED) {
            throw new TournamentException("Tài khoản " + email + " đang bị khóa");
        }
        loggedIn = true;
    }

    public void logout() {
        loggedIn = false;
    }

    public abstract void showMenu();

    public abstract String getRole();

    /** Gọi ở đầu mọi thao tác của lớp con để chặn tài khoản đã bị khóa. */
    protected void ensureActive() throws TournamentException {
        if (accountStatus != AccountStatus.ACTIVE) {
            throw new TournamentException("Tài khoản " + email + " đang bị khóa, không thể thao tác");
        }
    }

    // Không để public: chỉ Admin (cùng package) được khóa/mở khóa
    void setAccountStatus(AccountStatus status) {
        this.accountStatus = status;
    }

    @Override public String getId() { return userId; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public boolean isLoggedIn() { return loggedIn; }

    @Override
    public String toString() {
        return String.format("%-6s %-12s %-22s %-25s %s", userId, getRole(), name, email, accountStatus);
    }
}
