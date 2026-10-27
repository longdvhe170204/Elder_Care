# Elder_Care app (`mobile/`)

Một app Flutter chung cho nhân viên, người thân và người cao tuổi; chức năng theo loại tài khoản (Q-346, Q-365). "App nhân viên", "app người thân", "app người cao tuổi" trong spec là ba phần của app này.

Đọc trước khi viết mã: [AGENTS.md](../AGENTS.md), [DESIGN.md](../DESIGN.md) (mục 15 riêng cho Flutter; 15.6 phần người cao tuổi), [docs/team-plan.md](../docs/team-plan.md).

**Giai đoạn**: chủ dự án mở **phần người cao tuổi** ngày 2026-10-24 (002 T097: đăng nhập QR, mật khẩu dự phòng, đổi mật khẩu lần đầu, mở khóa sinh trắc học, màn "Hôm nay" khung) và mở thêm **phần nhân viên, người thân** ngày 2026-10-27: task mobile của mọi feature làm được. Phần nền dùng chung ở `lib/common/` do A làm: điều hướng theo loại tài khoản (000 T006, T007), lint (T008b), script sinh client (T010b), hàng đợi ngoại tuyến drift (T100), đăng nhập nhân viên, người thân (002 T046, T047), kết quả đồng bộ và chờ xem lại (002 T060); màn cần các phần này thì làm sau, trước đó làm phần trực tuyến và test widget với bản giả. Thư viện đã duyệt: `http`, `mobile_scanner`, `local_auth`, `flutter_secure_storage`, `firebase_messaging`, `geolocator`, `url_launcher`, `drift` (hàng đợi ngoại tuyến của phần nhân viên, research R-01, R-13 của 000).

**Máy có dấu cách trong đường dẫn Flutter** (ví dụ `C:\Users\ADMIN VIP\flutter`): hook native assets của `objective_c` (kéo theo bởi `local_auth`) không đặt nháy đường dẫn nên `flutter test` báo `'C:\Users\ADMIN' is not recognized`. Gọi Flutter qua tên ngắn 8.3, ví dụ `C:\Users\ADMINV~1\flutter\bin\flutter.bat test`, hoặc cài Flutter ở thư mục không có dấu cách.

## Lệnh

```bash
flutter pub get
flutter test                                   # một file: flutter test test/<đường dẫn>_test.dart
flutter test --plain-name "<tên test>"
flutter analyze
dart format <file>
```

Thêm package: nhờ A (người giữ `pubspec.yaml`).

## 1. Cấu trúc `lib/`

| Thư mục | Nhiệm vụ | Ai sửa |
| --- | --- | --- |
| `main.dart` | Mở màn đăng nhập rồi chuyển sang `staff/`, `family/` hoặc `resident/` theo loại tài khoản (000 T006) | A |
| `common/` | Dùng chung: theme (token của DESIGN.md frontmatter, mục 15.2), widget công thức (mục 7.2, 15: `task_card`, `status_pill`, `resident_avatar`…), client API, phiên đăng nhập, hàng đợi ngoại tuyến (drift) và phong bì đồng bộ (000 `OfflineEnvelope`), định dạng ngày giờ | A duyệt |
| `staff/<module>/` | Phần nhân viên; ghi được khi mất mạng (Q-01) | Chủ module (docs/team-plan.md) |
| `family/<module>/` | Phần người thân | Chủ module |
| `resident/<module>/` | Phần người cao tuổi (DESIGN.md 15.6: chữ lớn, vùng bấm lớn, nút SOS) | Chủ module |

Tasks ghi tên logic tiếng Việt: `chung` → `common`, `nhan-vien` → `staff`, `nguoi-than` → `family`, `nguoi-cao-tuoi` → `resident`; thư mục con dịch theo glossary, ví dụ `nhan-vien/dang-nhap/` → `staff/login/`, `nguoi-cao-tuoi/dang-nhap-qr/` → `resident/qr_login/`.

## 2. Thư mục con theo feature

Dùng cùng tên với web (frontend/README.md mục 2), đổi `-` thành `_`: `care_tasks`, `medication`, `alerts`, `handover`, `family_portal`… Mỗi feature chỉ ghi vào thư mục của mình trong từng nhánh.

## 3. Bên trong một thư mục

```
lib/staff/care_tasks/
  shift_checklist_screen.dart   màn (một Widget gắn vào điều hướng)
  task_card.dart                widget chỉ feature này dùng (widget dùng chung để ở common/)
  care_task_labels.dart         nhãn enum → tiếng Việt, hàm thuần
test/staff/care_tasks/
  shift_checklist_screen_test.dart
```

- File snake_case; lớp PascalCase theo glossary (`hop_duyet.dart` → `approval_inbox.dart`).
- Hậu tố: `_screen.dart` (cả màn), `_sheet.dart` (khung trượt), `_form.dart`, `_card.dart`, `_labels.dart`.
- Test đặt ở `test/` cùng đường dẫn với `lib/`, tên có mã quy tắc ở đầu như web.

## 4. Luật bắt buộc

1. Không import chéo giữa thư mục feature hay giữa `staff/`, `family/`, `resident/`; dùng chung thì đưa lên `common/` (A duyệt).
2. Gọi API chỉ qua client trong `common/`; lỗi theo Problem Details như web (`errorCode`, 412 có `currentValue`).
3. Màu, chữ, bo góc chỉ qua theme trong `common/`; không viết `Color(0x…)`, cỡ chữ thô (DESIGN.md 15).
4. Ngày giờ hiển thị dd/MM/yyyy HH:mm, giờ Việt Nam; gửi ISO-8601 có `+07:00`.
5. Lệnh ghi khi mất mạng (phần nhân viên) đi qua hàng đợi ngoại tuyến và `/sync` của 002; lệnh bắt buộc trực tuyến thì báo cần mạng.
6. Chữ trên màn tiếng Việt; tên trong mã tiếng Anh theo glossary.

## 5. Đã chốt khi mở mobile (2026-10-24, A)

- Trạng thái: `ChangeNotifier` của Flutter (`common/auth/session_controller.dart`), không thêm gói.
- Điều hướng: `MaterialApp.home` đổi màn theo trạng thái phiên; màn con dùng `Navigator` của Flutter. Đổi sang `go_router` khi phần nhân viên có nhiều màn.
- Client API: viết tay trong `common/api/api_client.dart` (Problem Details thành `ApiProblem`, mất mạng thành `ApiUnreachable`); script sinh client (000 T010b) làm khi phần nhân viên cần nhiều endpoint.
- Kho token: `flutter_secure_storage` qua `common/auth/secure_store.dart` (test dùng `MemoryStore`).
- Theme: `common/theme/app_theme.dart` (token DESIGN.md, `Tones` cho tông trạng thái, `AppTheme.resident` chữ x1,25 vùng bấm 56). Font Be Vietnam Pro 400, 700 đóng gói ở `assets/fonts/`; còn thiếu 500, 600.
- Chưa chốt: gói biểu tượng lucide cho Flutter, cách lưu chế độ sáng tối (DESIGN.md 15.7).

## 6. Mở phần nhân viên, người thân (2026-10-27, chủ dự án)

- Chủ module làm màn của feature mình trong `staff/<feature>/`, `family/<feature>/` theo tasks; thư mục chưa có thì tạo cùng PR đầu tiên.
- Thứ tự nền của A trong `common/` và `main.dart`: 000 T006, T007 (điều hướng theo loại tài khoản, khung ba phần), T008b (lint), 002 T046, T047 (đăng nhập nhân viên trên thiết bị dùng chung, mở khóa sớm; đăng nhập người thân), 000 T100 (hàng đợi ngoại tuyến drift, phong bì `Idempotency-Key`, `thoiDiemThietBi`), 002 T060 (kết quả đồng bộ, chờ xem lại), 000 T010b (script sinh client khi số endpoint lớn).
- Tới khi có hàng đợi ngoại tuyến: lệnh ghi của phần nhân viên gọi API trực tuyến qua `common/api/api_client.dart` và báo cần mạng khi `ApiUnreachable`; không tự viết hàng đợi riêng trong thư mục feature.
- Màn chỉ dùng dữ liệu mẫu khi không có backend phải ghi rõ trong test; dữ liệu minh họa theo AGENTS.md mục 6.
