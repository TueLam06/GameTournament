package gtms.repository;

import gtms.model.Player;
import gtms.model.Team;
import gtms.model.Tournament;
import gtms.model.User;
import gtms.util.IdGenerator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Ghi/đọc cả 4 kho bằng Java Serialization. Sau khi đọc lại, các đối tượng vẫn trỏ đúng vào nhau.
 * Sửa cấu trúc lớp trong model thì file cũ có thể không đọc được nữa.
 */
public class DataStore implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Repository<User> users = new Repository<>();
    private final Repository<Team> teams = new Repository<>();
    private final Repository<Player> players = new Repository<>();
    private final Repository<Tournament> tournaments = new Repository<>();
    private Map<String, Integer> idCounters = new HashMap<>();

    /** Ghi ra file tạm rồi mới thay file thật, để lỗi giữa chừng không làm hỏng file đang có. */
    public void save(Path file) throws IOException {
        idCounters = IdGenerator.snapshot();
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp = parent.resolve(file.getFileName() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temp))) {
            out.writeObject(this);
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Đổi tên file không đọc được thành *.hong-<thời điểm> để lần lưu sau không ghi đè lên nó. */
    public static Path backupBroken(Path file) throws IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = file.resolveSibling(file.getFileName() + ".hong-" + stamp);
        return Files.move(file, backup);
    }

    public static DataStore load(Path file) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            DataStore store = (DataStore) in.readObject();
            IdGenerator.restore(store.idCounters);
            return store;
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("File dữ liệu không đúng định dạng: " + file, e);
        }
    }

    public Repository<User> users() { return users; }
    public Repository<Team> teams() { return teams; }
    public Repository<Player> players() { return players; }
    public Repository<Tournament> tournaments() { return tournaments; }
}
