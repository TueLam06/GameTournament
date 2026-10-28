# Game Tournament Management System – Nhóm 5 (Milestone 4)

Yêu cầu: JDK 17 trở lên (đã chạy thử với JDK 21). Không dùng thư viện ngoài.

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out gtms.app.ConsoleApp          # chương trình console tương tác
java -cp out gtms.app.ConsoleApp demo     # kịch bản demo tự động
java -cp out gtms.app.ConsoleApp test     # bộ kiểm thử lõi (21 test)
```

Giao diện Swing dùng để demo khi báo cáo: bấm đúp `gui/build.bat` (xem [gui/README.md](gui/README.md)). Giao diện chỉ gọi các lớp trong `src/`, không chứa luật nghiệp vụ riêng.

Tóm tắt cho thành viên nhóm: xem [TOM_TAT_CHO_NHOM.md](TOM_TAT_CHO_NHOM.md).

Tài khoản mẫu (mật khẩu `Ptit@2026`): `admin@ptit.edu.vn` (Admin), `captain1@gmail.com` … `captain6@gmail.com` (Team Manager).
Dữ liệu được lưu ở `data/gtms.dat` khi thoát chương trình; xóa file này để quay về dữ liệu mẫu.
