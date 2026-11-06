# Đề xuất màn Ca của tôi — T016b, 2026-10-28

> **Ghép lại 2026-11-05:** đây là biên bản duyệt lịch sử từ worktree nguồn, không phải đề xuất mới. Mã đã đưa vào impl/008-integration, tương thích ApiClient hiện tại (không sửa common API), dùng PageBody/danh sách dòng theo DESIGN 6.8.1 và menu Thêm của StaffRoot. Toàn app 179 test xanh, analyze sạch; T016b đã [x]. Phạm vi sửa nền đã duyệt ở biên bản dưới, A review bản ghép. Không coi kết quả widget là nghiệm thu thiết bị/người dùng thật ở T044b.

Trạng thái: sau bước chờ duyệt, E xác nhận "chủ A duyệt rồi" trong phiên ngày 2026-10-28, cho tiếp tục bố cục và hai file nền bên dưới. Đã cài trên nhánh `impl/008-my-shifts-mobile` từ origin/develop 685dea6; T016b `[x]` sau 77/77 test toàn app xanh, analyze sạch, format qua. Chưa kiểm thiết bị/backend thật; xem validation.md mục T016b. A còn review mã nền, không còn chờ duyệt đề xuất.

## Bố cục đề nghị

Theo DESIGN.md mục 15: nền trời, nội dung trên thẻ, Be Vietnam Pro theo theme, biểu tượng lucide, vùng bấm ít nhất 48 logical px. Danh sách một cột, căn trái; không thêm màu, font, package hoặc quy tắc nghiệp vụ.

```text
┌─────────────────────────────────────┐
│ Ca của tôi                Đăng xuất │
├─────────────────────────────────────┤
│  Tháng trước   Tháng 10/2026   Sau   │
│  Chỉ ca của bạn trong tháng         │
│                         [Làm mới]   │
├─────────────────────────────────────┤
│  28/10/2026           [Đã công bố]  │
│  Từ 28/10/2026 07:00               │
│  Đến 28/10/2026 15:00              │
│  Nhân viên trong ca: [số từ API]    │
│  [Chi tiết ca]                     │
├─────────────────────────────────────┤
│  Các ca tiếp theo, xếp theo giờ     │
├─────────────────────────────────────┤
│ Đồng bộ | Chờ xem lại | Mở khóa sớm │
│                   Ca của tôi       │
└─────────────────────────────────────┘
```

Phác thảo minh họa bố cục, không phải dữ liệu thật. Thanh dưới giữ ba mục hiện có và thêm Ca của tôi làm mục thứ tư khi GET /me/permissions có SHIFT_SCHEDULE + VIEW; chưa tải được quyền thì ẩn mục này, cùng nguyên tắc web.

- Tháng mặc định là tháng hiện tại theo giờ Việt Nam; hai nút xem tháng trước/sau, khoảng gọi API là từ ngày 01 tới ngày 01 tháng sau, đầu gồm/cuối không gồm theo glossary 14.8.
- Chạm một ca mở bottom sheet: giờ bắt đầu/kết thúc, trạng thái, Người phụ trách ca và danh sách người cùng ca. Chỉ hiện tên/cờ Bác sĩ trực/vắng/nghỉ được duyệt từ dữ liệu backend; không hiển thị liên hệ, lý do riêng tư hoặc tự suy nhiệm vụ.
- Tên nhân viên lấy từ GET /staff đã được backend lọc; thiếu thông tin thì ghi rõ chưa có thông tin được phép xem, không bịa tên. Nháp/đã hủy và phạm vi do backend quyết định, giao diện không mở lịch người khác.
- Có trạng thái đang tải, không có ca, lỗi tải/Thử lại, không có quyền và mất mạng. Ngày giờ dùng tiện ích chung; cỡ chữ 200% và giao diện tối vẫn đọc được. Không lưu lịch xuống máy hoặc thêm hàng đợi ngoại tuyến cho màn chỉ đọc này.
- T016b làm phần lịch cá nhân, người phụ trách và người cùng ca của US2; màn phân công T022b, bàn giao T030 và ghi vắng T041b vẫn là các task riêng.

## Dữ liệu và file trong phần E

- `mobile/lib/staff/shifts/my_shifts_screen.dart`: màn tháng và khung chi tiết ca.
- `mobile/lib/staff/shifts/my_shifts_controller.dart`: trạng thái tải, đổi tháng, xử lý lỗi; chỉ gọi ApiClient chung.
- `mobile/lib/staff/shifts/shift_labels.dart`: nhãn trạng thái đã có trong hợp đồng, cùng chữ với web.
- Test tại `mobile/test/staff/shifts/`: tháng và query đúng hợp đồng, ca qua đêm, không có dữ liệu, lỗi mạng/quyền, tên chỉ từ backend, đổi tháng không bị kết quả cũ ghi đè; kiểm chữ 200%, tối và điều hướng app.

API đã có: GET /me/shifts?from=yyyy-MM-dd&to=yyyy-MM-dd, GET /staff, GET /me/permissions. Không đề nghị thêm endpoint hoặc trường HTTP. Backend /me/shifts trả List<ShiftResponses.Shift>; client Dart hiện tại bỏ JSON array và trả null.

## Hai file nền A cần được cho phép sửa

### 1. mobile/lib/common/api/api_client.dart

Thêm hàm đọc danh sách; giữ chữ ký get/post và giữ nguyên xử lý token, thiết bị, Problem Details, thời điểm kết nối.

```dart
Future<List<Map<String, dynamic>>> getList(String path) async {
  final decoded = await _send('GET', path, null);
  if (decoded is! List<dynamic>) {
    throw const FormatException('Phản hồi danh sách không hợp lệ');
  }
  final rows = <Map<String, dynamic>>[];
  for (final Object? row in decoded) {
    if (row is! Map<String, dynamic>) {
      throw const FormatException('Phần tử danh sách không hợp lệ');
    }
    rows.add(row);
  }
  return rows;
}
```

Để `_send` dùng được cho cả object và array: chỉ đổi kiểu trả về riêng tư thành `Future<Object?>`, trả JSON đã giải mã. get/post tiếp tục bọc kết quả và chỉ trả Map hoặc null như hiện tại. Test kiểm cả array, object/post cũ, lỗi mạng/Problem và header phiên.

### 2. mobile/lib/staff/staff_root.dart

- Import màn từ `shifts/my_shifts_screen.dart`.
- Khi tạo StaffRoot, đọc quyền hiện hành bằng ApiClient.getList('/me/permissions'); chỉ thêm tab khi có function SHIFT_SCHEDULE, operation VIEW. Chưa có quyền hoặc lỗi tải thì giữ ba tab đang có, không suy từ vai trò.
- Thêm cuối danh sách tab để giữ thứ tự và trạng thái chọn của các tab cũ:

```dart
if (_canViewMyShifts)
  ('Ca của tôi', LucideIcons.calendarDays,
    () => MyShiftsScreen(api: widget.session.api)),
```

Không sửa app.dart, main.dart, pubspec.yaml hoặc màn của thành viên khác. Ghi A review hai thay đổi nền ở team-plan mục 8 cùng kết quả test.

Kiểm chữ 200% phát hiện nhãn bốn tab bị cắt: đã thêm test vị trí nhãn, thấy lỗi rồi tăng chiều cao NavigationBar theo textScaler khi có mục Ca của tôi. Thứ tự và mục chọn mặc định của các tab cũ giữ nguyên.

## Căn cứ cần duyệt trước mã màn hình

- DESIGN.md mục 13.2: "Đề xuất bố cục (phác thảo hoặc mockup) → chủ dự án duyệt → mới sửa mã."
- mobile/README.md mục 5: feature thêm màn vào thanh điều hướng nhân viên, "A duyệt"; common/ cũng do A giữ theo mục 1.

Đã thực hiện sau xác nhận duyệt: test trước lỗi rồi cài, toàn app 77/77 test xanh, analyze sạch, format qua; kiểm CRLF. Ảnh render widget sáng/tối/200% nằm trong mobile/build/my-shifts-verification của worktree này, chỉ dùng dữ liệu test. Nghiệm thu thiết bị/backend thật còn ở T044b; không commit/push/PR/merge khi chưa được đồng ý.
