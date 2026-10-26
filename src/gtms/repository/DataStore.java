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

    public void save(Path file) throws IOException {
        idCounters = IdGenerator.snapshot();
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(this);
        }
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
