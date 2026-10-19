# Elder_Care app (`mobile/`)

Một app Flutter chung cho nhân viên, người thân và người cao tuổi; chức năng theo loại tài khoản (Q-346, Q-365). "App nhân viên", "app người thân", "app người cao tuổi" trong spec là ba phần của app này.

Đọc trước khi viết mã: [AGENTS.md](../AGENTS.md), [DESIGN.md](../DESIGN.md) (mục 15 riêng cho Flutter; 15.6 phần người cao tuổi), [docs/team-plan.md](../docs/team-plan.md).

**Giai đoạn**: mobile chưa viết mã chức năng, chỉ làm màn mẫu thiết kế với dữ liệu giả (CLAUDE.md, "Giai đoạn hiện tại"). Task `mobile/` trong tasks.md để chưa `[x]` tới khi chủ dự án mở phần này. Các mục ghi "chờ chốt" sẽ được A chốt khi mở mobile.

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

## 5. Chờ chốt khi mở mobile

- Cách quản lý trạng thái (ví dụ `ChangeNotifier`/`provider`, Riverpod hay bloc).
- Điều hướng (Navigator 2 / `go_router`).
- Sinh client API từ hợp đồng hay viết tay.
- Kho an toàn cho token (`flutter_secure_storage`, 002 T001).
