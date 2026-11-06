# Đề xuất T022b — Phân công trên app, 2026-10-28

Trạng thái 2026-10-29 sau review: A/chủ dự án đã duyệt bố cục, hai file nền, cả hai hợp đồng HTTP và cho đổi staffId khi phân công/ca còn Nháp. Năm thao tác đã nhập api-008.yaml; T054/T055 đã cài và kiểm xanh, đã đánh [x]. Mã T022b đã có, 75 test mobile/analyze sạch sau sửa trạng thái xem trước và kết quả tìm kiếm trả chậm; T022b vẫn [ ] vì chờ nguồn thật B/C, tên/lựa chọn đầy đủ và nghiệm thu thiết bị. Không còn chờ duyệt các quyết định đã chốt. Xem assignment-verification-2026-10-29.md.

## 1. Bố cục đã được A duyệt theo xác nhận của E

Màn cho nhân viên xem phân công trong ca mình có tên; Trưởng tầng/Quản lý viện thao tác trong phạm vi backend trả. Dùng theme trời xanh hiện có, thẻ, Be Vietnam Pro, lucide; một cột trên điện thoại, cuộn được ở chữ 200%, sáng/tối. Không thêm màu, font hoặc package.

    ┌──────────────────────────────────────────┐
    │ Phân công                    Đăng xuất   │
    ├──────────────────────────────────────────┤
    │ Tháng trước    [Tháng đang xem]    Sau    │
    │ [Chọn lịch trong phạm vi được xem]        │
    │ [Chọn ca: ngày, giờ, tên mẫu ca]          │
    │                              [Làm mới]   │
    ├──────────────────────────────────────────┤
    │ Phân công trong ca                       │
    │ [Họ tên từ backend]         [Trạng thái]  │
    │ [Đối tượng được phép xem]                │
    │ [Vai trò] · [Chính/Hỗ trợ]                │
    │ Từ [ngày giờ VN] tới [ngày giờ VN]        │
    │ [Chi tiết và thao tác được phép]         │
    ├──────────────────────────────────────────┤
    │ [Lập phân công]                          │
    │ [Sao chép phân công] [Giao Trưởng tầng]   │
    └──────────────────────────────────────────┘

Khung chỉ minh họa bố cục, không phải dữ liệu thật. Nút phụ chỉ xuất hiện khi đủ quyền/điều kiện từ backend; không đặt nhiều nút chính cạnh nhau. Lập phân công mở màn con bằng Navigator; chi tiết, lý do, kết quả sao chép dùng bottom sheet cuộn được.

- Người chỉ xem ca cá nhân: GET /me/shifts với khoảng tháng [ngày 01, ngày 01 tháng sau), rồi GET /shifts/{id}/assignments; không có lựa chọn lịch của người khác.
- Người quản lý lịch: như web hiện có, GET /shift-schedules?month=yyyy-MM khi có SHIFT_SCHEDULE CREATE/UPDATE/APPROVE; backend lọc phạm vi. Trước thao tác đọc lại GET /shifts/{id} lấy allowedCommands.
- Hiện Nháp/Tương lai/Hiệu lực/Đã kết thúc/Đã hủy đúng backend; không tự xác định trạng thái/người phụ trách bằng đồng hồ app.
- Tên nhân viên, tầng/khu/phòng/người cao tuổi và loại công việc lấy từ API đọc của chủ nguồn. Ô chọn hiện tên, không bắt người dùng gõ mã nội bộ; không bịa tên khi nguồn thiếu/bị lọc, không hiện liên hệ hoặc dữ liệu sức khỏe.
- Thiếu danh mục/quyền: nói rõ phần chưa tải được, cho Thử lại; không coi nguồn thiếu là danh sách nghiệp vụ rỗng.
- Tab Phân công thêm cuối thanh dưới khi GET /me/permissions có SHIFT_SCHEDULE/VIEW. Khi gộp hai nhánh, giữ ba mục nền và Ca của tôi của T016b; tối đa năm mục. T016b chưa có trong origin/develop; không tự đánh task đó xong trên nhánh này.

## 2. Luồng thao tác đã có hợp đồng

| Việc trên màn | Yêu cầu gửi/nguồn quyền |
| --- | --- |
| Xem | GET /shifts/{id}/assignments; backend kiểm phạm vi (FR-033, FR-054) |
| Xem trước khi lập | POST /shifts/{id}/assignments/preview, body NewAssignment; không ghi |
| Lập sau khi kiểm | POST /shifts/{id}/assignments; cần createAssignment; hiện đủ warnings trước Lưu |
| Xóa Nháp | DELETE /assignments/{id}; cần deleteAssignment và dòng DRAFT; hộp xác nhận; không tự thêm body/lý do/If-Match |
| Hủy Tương lai | POST /assignments/{id}/cancel với reason; cần cancelAssignment và dòng SCHEDULED |
| Kết thúc Hiệu lực | POST /assignments/{id}/end với reason; cần endAssignment và dòng ACTIVE |
| Sao chép | POST /shifts/{id}/copy-assignments với sourceShiftId; cần copyAssignments; hiện từng notCopied và reason |
| Xem/giao Trưởng tầng | GET/POST /floor-manager-assignments; tạo chỉ Quản lý viện có quyền tương ứng |
| Rút ngắn giao | POST /floor-manager-assignments/{id}/end với to, reason; chỉ Quản lý viện có quyền tương ứng |

- NewAssignment giữ đúng staffId, targetType, targetId, taskTypeId tùy chọn, role, assignmentKind, assignedNurse, startsAt/endsAt tùy chọn. Không thêm version hoặc confirmWarnings vào HTTP. Mặc định giờ ca; ngày giờ gửi ISO-8601 có +07:00.
- Bộ lọc giúp chọn nhân viên trong ca; backend kiểm có mặt, phạm vi, chứng chỉ, chồng phân công. Không cài lại BR-M09-01/FR-028 trong Dart.
- Sửa bất kỳ trường form nào thì bỏ kết quả xem trước cũ. Hiện cảnh báo FR-031 và ô xác nhận đã đọc nếu có; chưa kiểm/đã đổi form thì chưa cho Lưu. Lệnh tạo vẫn được backend kiểm lại.
- POST có Idempotency-Key theo hợp đồng: cùng lệnh/nội dung giữ khóa khi thử lại vì mất mạng; nội dung hoặc lệnh mới thì khóa mới. Không tự thêm khóa cho preview/DELETE.
- Lỗi ApiProblem/detail hiện rõ; tải lại khi trạng thái đã đổi, giữ lý do đang nhập để xem lại. Không tự thử lại lệnh ghi.
- Giao/rút ngắn Trưởng tầng kiểm AC-01 từ GET /me và quyền tương ứng, không suy Quản lý viện chỉ từ CREATE vốn Trưởng tầng cũng có. Backend vẫn kiểm cuối.
- Không đưa lệnh quản lý phân công vào hàng đợi ghi nhận ngoại tuyến 002. Mất mạng báo chưa thực hiện và cho người dùng thử lại; không áp quy tắc lưu xem bàn giao của T030 sang màn này.

## 3. Phạm vi hai file nền đã được A cho phép

### 3.1. mobile/lib/common/api/api_client.dart

Đề nghị thêm đúng hàm DELETE:

    Future<void> delete(String path) async {
      await _send('DELETE', path, null);
    }

Đi qua _send hiện có để giữ token, X-Device-Id, Accept, Problem Details, mất mạng và lastConnectedAt. DELETE của 008 thành công trả 204, không đọc body thành object; không thêm HTTP client/thư viện.

T022b cần getList đã được A duyệt và kiểm ở nhánh T016b, nhưng origin/develop 685dea6 chưa có. Khi cài dùng lại đúng phần client và test tương thích tại worktree ../008-my-shifts-mobile; nếu develop chưa nhận thì mang riêng phần đã duyệt sang nhánh này, ghi nguồn rõ. Không sao chép màn Ca của tôi hoặc đánh T016b [x] khi không có mã đó ở nhánh.

Giữ chữ ký get/post/getList, cách trả Map/null của get/post và lỗi chung. Test DELETE đặt trong mobile/test/staff/shifts/; không sửa test A.

### 3.2. mobile/lib/staff/staff_root.dart

- Import shifts/assignments_screen.dart; thêm cuối mục Phân công, LucideIcons.clipboardList, AssignmentsScreen(api: widget.session.api) khi có SHIFT_SCHEDULE/VIEW.
- Dùng cùng một lượt đọc /me/permissions cho các mục 008 sau khi gộp; không gọi mỗi lần build. Chưa xác định quyền thì ẩn mục.
- Khi gộp T016b, giữ Ca của tôi và Phân công sau ba mục cũ, không ghi đè tab. Kiểm chiều cao để nhãn không cắt ở chữ 200%; kiểm bốn mục ở develop hiện tại và năm mục khi hai phần cùng có.
- Không sửa app.dart, main.dart, SessionController hoặc pubspec.yaml. Màn đọc vai trò qua GET /me, không mở rộng UserInfo của A.

## 4. Hai chỗ thiếu hợp đồng — đã được duyệt và bổ sung 2026-10-29

### 4.1. FR-025 — sửa phân công Nháp đã lưu (liên quan Q-82)

- Spec dòng 387 cho sửa/xóa phân công trên lịch Nháp. Q-82 ở spec dòng 23 chốt Nháp chưa tạo phạm vi/gán công việc, không chốt giao thức sửa.
- Hợp đồng trước bổ sung chỉ có tạo, xem trước tạo và DELETE; đây là lý do bổ sung ba thao tác T054 đã duyệt.
- AssignmentController nay có GET chi tiết, preview sửa và update-draft; không dùng lệnh xóa thay cập nhật.
- Luồng đã được duyệt cập nhật nguyên tử trên cùng id; DELETE rồi POST có thể làm mất bản cũ nếu lệnh tạo bị từ chối.
- Đã duyệt và nhập ba thao tác sửa Nháp vào api-008.yaml; staffId được đổi khi phân công/ca đều Nháp. AssignmentService/AssignmentStore cập nhật nguyên tử giữ id; xem assignment-contract-review.md mục 1.

### 4.2. FR-028/FR-029 — kết quả người phụ trách/người còn thiếu người phụ trách

- Spec dòng 403/405: backend xác định nhân viên chính theo ưu tiên đối tượng/loại công việc; Trưởng tầng và người phụ trách ca xem danh sách người cao tuổi chưa có Điều dưỡng phụ trách hoặc nhân viên chăm sóc chính.
- Summary GET /shifts/{id}/assignments nhắc danh sách này nhưng schema là mảng Assignment. AssignmentResponses.Assignment chỉ có dữ liệu phân công, không có kết quả theo người cao tuổi hoặc danh sách còn thiếu.
- GET danh sách cũ vẫn trả các dòng; hai GET mới đọc kết quả, không dùng thông báo thay API đọc.
- Không suy người chính hoặc danh sách thiếu từ bảng phân công trong Dart: sẽ chép quy tắc vị trí theo thời điểm/ưu tiên đối tượng/nguồn 001/003 sang client, trái constitution XI.
- Đã duyệt và nhập hai GET đọc kết quả vào api-008.yaml; AssignmentReadService tính ở backend theo mốc. Không đổi mảng cũ hoặc tính ưu tiên trong Dart.

Hai bản riêng đã được duyệt, lưu làm biên bản. Phần chưa có nguồn thật của B/C tiếp tục báo thiếu nguồn; không tạo fake production hoặc đánh T022b hoàn tất.

## 5. File của E và kiểm chứng sau khi được duyệt

- Màn/state/form/nhãn trong mobile/lib/staff/shifts/, cùng web features/shifts: assignments_screen.dart, assignments_controller.dart, assignment_form.dart, assignment_labels.dart, floor_manager_sheet.dart; chỉ tạo file cần khi cài.
- Test trong mobile/test/staff/shifts/, tên bắt đầu FR_024/FR_025/FR_027/FR_031/FR_032/FR_033/FR_054. Không viết fake nguồn thứ hai hoặc fake production.
- Test trước, thấy đỏ rồi cài: DELETE 204/header/Problem/mạng và client cũ; menu/phạm vi/allowedCommands; form đổi phải preview lại; cảnh báo trước lưu; Idempotency-Key; DELETE Nháp khác POST hủy; reason; từng lý do không sao chép; tên backend; ca qua đêm/+07:00; kết quả ca/tháng cũ không ghi đè; tải/rỗng/lỗi/Thử lại.
- Kiểm widget điện thoại/tablet, sáng/tối/chữ 200%, nhãn thanh dưới không cắt. Chạy flutter test/analyze/dart format, kiểm ảnh và thiết bị khi đủ môi trường. Không dùng ảnh/mock làm bằng chứng thiết bị/backend thật.
- Chỉ đánh [x] phần đã kiểm xanh và đủ phạm vi được chốt. Cập nhật plan/tasks/glossary mục 14, xóa dòng team-plan xong, thêm A review mã nền và phần còn chờ; progress chỉ thêm, giữ CRLF. Không commit/push/PR/merge khi chưa đồng ý.

## 6. Căn cứ duyệt

- DESIGN.md dòng 513: "Đề xuất bố cục (phác thảo hoặc mockup) → chủ dự án duyệt → mới sửa mã."
- mobile/README.md dòng 29: common/ do A duyệt; dòng 74: thêm màn feature vào staff_root phải A duyệt.
- AGENTS.md mục 2.1, 2.5: nghiệp vụ và HTTP đã được duyệt theo xác nhận của E ngày 2026-10-29; không tự đổi spec/docs/business hoặc API Java của chủ nguồn.
