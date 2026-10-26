package gtms.model;

/** Đối tượng có mã định danh duy nhất, dùng làm khóa trong Repository. */
public interface Identifiable {
    String getId();
}
