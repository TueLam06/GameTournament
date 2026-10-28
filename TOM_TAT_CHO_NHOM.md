# Tóm tắt mã nguồn Milestone 4 – cho cả nhóm

Mã Java của đề tài **Game Tournament Management System**, viết theo đúng sơ đồ lớp ở Milestone 3.
Chi tiết đầy đủ xem file báo cáo `Group 5 - Milestone 4.docx`.

## 1. Chương trình làm gì

| Vai trò | Làm được gì |
|---|---|
| **Khách** (chưa đăng nhập) | Đăng ký tài khoản; xem, tìm giải theo tên, trạng thái, game; xem chi tiết giải gồm lịch thi đấu và bảng xếp hạng |
| **Đội trưởng** | Tạo, sửa, giải thể đội; thêm, sửa, xóa tuyển thủ; đăng ký cả đội vào giải; hủy đăng ký; xem lịch sử và thành tích của đội |
| **Ban tổ chức (Admin)** | Tạo, sửa, xóa, hủy giải; mở và đóng đăng ký; duyệt hoặc từ chối đơn; sinh lịch thi đấu; nhập và sửa kết quả; xử thua; khóa, mở khóa tài khoản; xuất bảng xếp hạng ra CSV |

Những việc chương trình tự làm:

- Kiểm tra điều kiện đăng ký: đủ người, đúng game, còn hạn, còn slot.
- Sinh lịch theo thể thức vòng tròn (tự xử lý khi số đội lẻ) hoặc loại trực tiếp (số đội là 2, 4, 8, 16...).
- Mỗi khi có kết quả, tính lại bảng xếp hạng và đưa đội thắng lên vòng sau. Trận cuối xong thì tự kết thúc giải và xác định đội vô địch.
- Thao tác sai thì báo lỗi rõ ràng mà chương trình không dừng.
- Lưu toàn bộ dữ liệu vào file khi thoát và đọc lại ở lần mở sau.

Chương trình giúp gì cho đồ án:

- **M4:** chính là sản phẩm phải nộp: các lớp Java, có dùng collections và xử lý ngoại lệ.
- **M5:** là phần mã để tái cấu trúc theo SOLID và áp dụng State Pattern.
- **M6:** chế độ `demo` dùng khi thuyết trình, chế độ `test` dùng cho phần kiểm thử. Giao diện Swing (`gui/`) được nộp kèm để demo trực tiếp khi báo cáo.

Lưu ý: package `gtms.app` chỉ là phần giao diện console (menu, dữ liệu mẫu, kịch bản demo). Các luật của giải đấu nằm trong `gtms.model` và `gtms.service`.

## 2. Công nghệ đang dùng

| Hạng mục | Dùng gì | Ghi chú |
|---|---|---|
| Ngôn ngữ | Java 17 trở lên (đã chạy với JDK 21) | Không cần IDE riêng |
| Build | `javac` / `java` gõ lệnh trực tiếp | Không dùng Maven, Gradle |
| Thư viện | Chỉ thư viện chuẩn của Java | Không cài thêm gì |
| Giao diện | Console (`Scanner`) và Swing (`gui/`) | Hai giao diện dùng chung lớp nghiệp vụ và file dữ liệu |
| Lưu dữ liệu | Java Serialization (`ObjectOutputStream`) ghi ra `data/gtms.dat` | Không dùng CSDL |
| Xuất file | `BufferedWriter` ghi bảng xếp hạng ra CSV | Mở được bằng Excel |
| Ngày giờ | `java.time` (`LocalDateTime`, `Clock`) | `AppClock` cho phép cố định giờ khi test |
| Mật khẩu | `MessageDigest` SHA-256 + salt ngẫu nhiên (`SecureRandom`) | Không lưu mật khẩu gốc |
| Kiểm thử | Tự viết trong `gtms.test.CoreTests` | Không dùng JUnit (dự kiến chuyển ở M6) |

Các kiến thức của môn được dùng trong mã:

- **Đóng gói:** mọi thuộc tính `private`, getter trả danh sách chỉ đọc, kiểm tra dữ liệu ngay trong constructor.
- **Kế thừa, đa hình:** `User` (abstract) → `TeamManager`, `Admin`, cùng ghi đè `showMenu()`, `getRole()`.
- **Interface:** `ScheduleStrategy` (sinh lịch), `RankingCalculator` (tính điểm). Đây chính là Strategy Pattern.
- **Enum:** 6 enum trạng thái; 3 enum có hàm `canTransitionTo()` để chặn chuyển trạng thái sai.
- **Collections:** `ArrayList`, `LinkedHashMap`, `TreeMap`, `HashSet`, `HashMap`, `Comparator`, `Optional`, Stream API.
- **Generics:** `Repository<T extends Identifiable>` là kho dùng chung cho tài khoản, đội, tuyển thủ, giải đấu.
- **Ngoại lệ:** `TournamentException` (checked) và 4 lớp con; mọi lỗi được bắt ở một chỗ là `ConsoleApp.execute()`.

## 3. Thư mục và package

```
GameTournament/src/gtms/
├── model/       Lớp nghiệp vụ: User, TeamManager, Admin, Team, Player, Tournament,
│                Registration, Match, Ranking, RankingEntry + 6 enum
├── strategy/    Sinh lịch vòng tròn, loại trực tiếp; tính điểm 3-1-0
├── exception/   TournamentException và 4 lớp con
├── repository/  Repository<T>, DataStore (lưu/đọc file)
├── service/     TournamentService: kiểm tra trùng email, trùng tên đội, tìm kiếm, lịch sử
├── util/        AppClock, IdGenerator, PasswordUtil, Validator
├── app/         ConsoleApp (menu), DemoScenario, SampleData
└── test/        CoreTests (21 test)

GameTournament/gui/  Giao diện Swing (GuiApp) để demo khi báo cáo, nộp kèm mã nguồn.
```

## 4. Chạy thử

```bash
cd GameTournament
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out gtms.app.ConsoleApp          # dùng bằng menu
java -cp out gtms.app.ConsoleApp demo     # demo tự động
java -cp out gtms.app.ConsoleApp test     # chạy 21 test
```

Muốn bấm chuột cho nhanh: bấm đúp `gui/build.bat` (Windows) hoặc chạy `bash gui/build.sh`. Script build ra `gui/build/GameTournamentGUI.jar` rồi mở giao diện. Hướng dẫn chi tiết ở `gui/README.md`.

Tài khoản mẫu, mật khẩu chung `Ptit@2026`:
- Admin: `admin@ptit.edu.vn`
- Đội trưởng: `captain1@gmail.com` đến `captain6@gmail.com`

## 5. Quy tắc đã chốt trong mã

- Mật khẩu ít nhất 6 ký tự; email chỉ cần không để trống.
- Thắng 3, hòa 1, thua 0. Bằng điểm thì xét hiệu số, rồi số trận thắng. Đội bỏ cuộc thua 0-1.
- Loại trực tiếp: số đội phải là 2, 4, 8, 16...; vòng 1 ghép theo thứ tự đăng ký (1-2, 3-4...). Kết quả đã ghi thì không sửa được (vòng tròn thì sửa được).
- Đăng ký cả đội, không chọn riêng đội hình. Mỗi đơn còn hiệu lực chiếm một slot; hết slot thì không nhận đơn. Đơn bị hủy hoặc từ chối thì trả lại slot.
- Đóng đăng ký chỉ được khi có ít nhất 2 đội đã duyệt (loại trực tiếp: đúng 2, 4, 8, 16... đội), để giải luôn sinh được lịch. Đã đóng thì không mở lại được.
- Chỉ sửa cấu hình hoặc xóa giải khi giải còn Nháp.
- Đội còn đơn đăng ký hiệu lực ở giải chưa kết thúc thì không xóa tuyển thủ được.
- Hủy giải thì mọi đơn còn hiệu lực của giải đó cũng bị hủy.
- File dữ liệu hỏng được đổi tên thành `gtms.dat.hong-<thời điểm>` thay vì bị ghi đè.

Nếu nhóm muốn đổi quy tắc nào thì báo để sửa cả mã lẫn báo cáo.

## 6. Khi sửa mã

- Chạy `test` trước khi gửi code cho nhóm; phải đạt 21/21.
- Lỗi vi phạm luật giải: ném `TournamentException` hoặc lớp con. Lỗi dữ liệu nhập (để trống, số nhỏ hơn mức tối thiểu): ném `IllegalArgumentException`.
- Sửa cấu trúc một lớp trong `model` thì xóa `data/gtms.dat`, vì file cũ có thể không đọc được nữa.

## 7. Việc còn lại

- **M5:** áp dụng State Pattern cho giải và trận; tách menu ra khỏi lớp nghiệp vụ; gộp chỗ kiểm tra điều kiện đăng ký đang bị lặp; bổ sung `TournamentService`, `Repository`, `DataStore` vào sơ đồ lớp.
- **Chưa làm:** gửi thông báo (UC-06), thông số trận đấu (FR-12).
- **M6:** chuyển test sang JUnit, chuẩn bị demo bằng giao diện Swing và thuyết trình.
