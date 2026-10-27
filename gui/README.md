# Giao diện chạy thử (Swing)

Chỉ dùng để chạy thử trong nhóm. Thư mục `gui/` không nằm trong mã lõi: khi nộp bài, xóa cả thư mục này là xong.

## Build và chạy

Windows: bấm đúp `gui/build.bat`, hoặc gõ:

```bat
gui\build.bat
```

Git Bash, macOS, Linux:

```bash
bash gui/build.sh
```

Script biên dịch mã lõi (`src/`) cùng giao diện (`gui/src/`) vào `gui/out/`, đóng gói thành `gui/build/GameTournamentGUI.jar` rồi mở chương trình. Lần sau có thể chạy thẳng file jar, nhưng phải đứng ở thư mục `GameTournament` để dùng đúng dữ liệu:

```bash
java -jar gui/build/GameTournamentGUI.jar
```

## Dùng thế nào

- Bảng bên trái là danh sách giải; chọn một giải để xem đơn đăng ký, lịch thi đấu, bảng xếp hạng ở bên phải.
- Bấm **Đăng nhập**, chọn email trong danh sách (mật khẩu mẫu đã điền sẵn `Ptit@2026`). Hàng nút **Thao tác** ở dưới đổi theo vai trò:
  - Admin: tạo, mở, đóng, hủy, xóa giải; duyệt, từ chối đơn (chọn đơn trong tab Đơn đăng ký); sinh lịch; nhập kết quả, xử thua (chọn trận trong tab Lịch thi đấu); xuất CSV; khóa tài khoản.
  - Đội trưởng: tạo đội, thêm, xóa tuyển thủ; đăng ký giải đang chọn và chọn đội hình; hủy đăng ký; xem thành tích.
- **Chạy test**, **Chạy demo**: chạy bộ 21 test hoặc kịch bản demo và hiện kết quả trong cửa sổ.
- **Về dữ liệu mẫu**: xóa dữ liệu đang có, quay lại dữ liệu ban đầu.
- Dữ liệu tự lưu vào `data/gtms.dat` khi đóng cửa sổ, dùng chung với bản console.

Giao diện này chưa có: sửa giải, sửa đội, sửa tuyển thủ, giải thể đội, tìm và lọc giải. Các chức năng đó dùng bản console.
