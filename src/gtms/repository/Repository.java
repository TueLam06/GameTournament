package gtms.repository;

import gtms.model.Identifiable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Kho dùng chung cho mọi đối tượng có mã. LinkedHashMap để tìm theo mã nhanh mà vẫn giữ thứ tự thêm vào. */
public class Repository<T extends Identifiable> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, T> items = new LinkedHashMap<>();

    public void add(T item) {
        if (items.containsKey(item.getId())) {
            throw new IllegalArgumentException("Mã " + item.getId() + " đã tồn tại");
        }
        items.put(item.getId(), item);
    }

    /** Mã trong hệ thống đều viết hoa nên người dùng có thể nhập "tour001" hay "TOUR001". */
    public Optional<T> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(items.get(id.trim().toUpperCase()));
    }

    public List<T> findAll() {
        return new ArrayList<>(items.values());
    }

    public List<T> findBy(Predicate<? super T> condition) {
        return items.values().stream().filter(condition).collect(Collectors.toList());
    }

    public Optional<T> findFirst(Predicate<? super T> condition) {
        return items.values().stream().filter(condition).findFirst();
    }

    /** Ví dụ: findAllOfType(Admin.class) lấy riêng các Admin trong kho User. */
    public <S extends T> List<S> findAllOfType(Class<S> type) {
        return items.values().stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }

    public boolean remove(String id) {
        return items.remove(id) != null;
    }

    public int size() {
        return items.size();
    }
}
