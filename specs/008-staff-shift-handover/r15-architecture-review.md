# Review cấu trúc 008 trước gộp bảng R-15

Đây là checkpoint T050a trước migration, giữ kết quả kiểm tại thời điểm đó. Tiến độ chuyển bảng/ngoại lệ nhóm dữ liệu mới hơn xem [review T050](r15-implementation-review.md); không lấy ghi chú “chưa có migration” dưới đây làm trạng thái hiện tại.

Ngày 2026-10-31; nhánh impl/008-q262, nền d2bd7de0. Chưa commit, push hoặc mở PR.

## Trạng thái và giới hạn

T050a hoàn tất: đã đỏ trước cài, 19 test tập trung xanh, clean verify chạy hết và không phát sinh lớp hoặc phương thức đỏ mới so với nền Q-262/develop. T050 gộp bảng vẫn mở: chưa có migration V008_94/V008_95, chưa đổi dữ liệu hoặc API công khai, chưa xóa 25 dòng PENDING_DATA_GROUP của E.

## 1. Chuyển gói

- Bảy nhóm assignment, credential, duty, handover, ratio, shift, staff chuyển từ staffing/<nhóm>/internal sang staffing/internal/<nhóm>.
- Controller vào staffing/web; request và response vào staffing/web/{request,response}.
- AssignmentCommandPort và ShiftCommandPort là interface nội bộ ở service, bỏ gói con service/port.
- Đợt chuyển đầu sửa namespace/import ở 164 file; 120 file đổi đường dẫn (117 main, 3 test). Sau đó chuyển M09Retention từ internal/service vào internal/persistence và thêm factory ở persistence.
- Các chữ ký và gói api không đổi. Các test kiến trúc M09 vẫn kiểm cả caller hợp lệ và caller trái phép; không nới luật cho xanh.

Lỗi script chọn nhầm src/test cho 117 file main đã được phát hiện trước biên dịch, sửa đúng đường dẫn theo manifest và kiểm SHA-256 sau chuyển; không ghi đè/mất nội dung. Manifest nằm ngoài mã nguồn: backend/target/008-r15-verification/architecture-moves.json ở worktree gốc.

## 2. Đưa SQL về persistence

| Lớp dịch vụ | Lớp nhận SQL | Điểm giữ nguyên |
| --- | --- | --- |
| AssignmentReminders | ShiftStore | Claim cờ chỉ một lần; danh sách ca cần nhắc và điều kiện theo giờ |
| CredentialInvalidityEffects | CredentialStore, AssignmentStore | Mốc vô hiệu; lọc phân công còn hiệu lực, thứ tự khóa và cập nhật |
| CredentialReminders | CredentialStore, AssignmentStore, ShiftStore, DepartmentDutyStore | Cảnh báo một lần, loại công việc/chứng chỉ bắt buộc, bộ phận thiếu phụ trách |
| CredentialService | CredentialStore | Kiểm phạm vi còn hiệu lực, thu hồi, reset cờ và đọc danh mục |
| PracticeScopeLookup | CredentialStore | Tra đúng id và trạng thái; trả cùng SPI của A |
| HandoverCorrection | HandoverStore, StaffStore | Người lập/người xác nhận/nhân viên nhận thông báo, giữ id bàn giao gốc |
| ShiftLifecycle | ShiftStore | Đóng lịch sử phụ trách tại thời điểm kết thúc ca |
| StaffEmploymentEffects | AssignmentStore | Chọn các giao Trưởng tầng đang hiệu lực để xử lý nghỉ việc |
| StaffService | StaffStore | Khóa bộ phận trước đổi; đóng/thêm khoảng nhân sự rồi tăng phiên |

Không truyền chuỗi SQL từ service vào một wrapper chung. Các truy vấn đặt ở phương thức Store theo nghiệp vụ hiện có. Giao dịch, quyền, kiểm điều kiện, audit và sự kiện vẫn do dịch vụ hiện có thực hiện.

M09Retention giữ nguyên hành vi lưu giữ, chuyển nguyên lớp sang persistence. Factory nhận JDBC/RetentionDatabase/BusinessTime/AccountApi/ProfessionalActivityApi và tạo 17 provider theo Kind; cấu hình chỉ khai bean qua factory.

## 3. Test trước và kiểm sau

| Lượt | Kết quả | Chứng cứ |
| --- | --- | --- |
| Đỏ trước cài | LayerArchTest: 10 test; 3 thất bại; 0 lỗi thực thi, 0 bỏ qua | architecture-red.log |
| Sau chuyển gói và SQL | LayerArchTest 10, M09ArchRuleTest 4, M09ArchTest 2, StaffAccountLookupTest 3: tổng 19 xanh | architecture-green.log |
| Toàn backend | 674 unit/arch xanh; 1486 IT, 7 thất bại/9 lỗi tại 9 lớp baseline; 258 IT staffing xanh, 0 bỏ qua; 13:55 | architecture-clean-verify.log |

Log ở backend/target/008-r15-verification của worktree gốc, không đưa bí mật vào tài liệu.

LayerArchTest đã bỏ danh sách PENDING_MODULE_ROOT_PACKAGES chỉ gồm E, xóa một ngoại lệ service.port của E và mười ngoại lệ phụ thuộc JDBC của E. Giữ nguyên ngoại lệ của chủ module khác. PENDING_API_ROOT_TYPES/PENDING_NESTED_RECORD_OWNERS không có dòng E cần xóa.

## 4. Phần tiếp theo và phụ thuộc

- T050 dùng bản thật API bảng chung của A (000 T108 đã có); không còn chờ nền này.
- Phải kiểm chép đủ dữ liệu cũ, khóa ngoại, legacy, id/logical record type, COMMENT DataGroup, trigger theo dòng và quyền MySQL trước khi xóa bảng cũ.
- Chờ nguồn AllocationHandoverApi của C để nghiệm thu toàn luồng cảnh báo phân bổ; không chặn bước cấu trúc hoặc migration hiện tại.
- Giữ các worktree/nhánh 008 mobile và 010 đang dở; chưa tích hợp các thay đổi đó vào nhánh hiện tại.

## 5. Kết luận review và các lớp đỏ sẵn

- Toàn bộ 258 IT staffing xanh, gồm DutyDirectoryIT 7 test Q-262 và StaffImportTransportIT. Không có thay đổi tập phương thức thất bại so với lượt clean verify Q-262 trước refactor.
- Spotless check sạch; LayerArchTest trực tiếp kiểm staffing sau khi xóa ngoại lệ E. Toàn backend vẫn BUILD FAILURE do baseline bên dưới; không báo toàn backend xanh.
- T050a [x], xóa dòng cấu trúc E ở team-plan mục 8. T050 vẫn [ ]; 25 dòng PENDING_DATA_GROUP giữ nguyên cho tới khi migration và test bảo vệ xanh.
- Không có thay đổi file api của staffing, không sửa migration đã merge hoặc test của module khác. 155 file ở bảy worktree cũ giữ nguyên SHA-256. File sửa giữ CRLF, UTF-8 không BOM.
- Danh sách từng file: [r15-architecture-files.md](r15-architecture-files.md). Điểm cần chủ dự án chọn trước phần đổi ID: [r15-migration-identity-review.md, mục 1](r15-migration-identity-review.md).

| Lớp baseline | Thất bại | Lỗi thực thi |
| --- | ---: | ---: |
| BR_11_TemporaryAbsenceOverdueIT | 0 | 1 |
| BR_05_ReassessmentNoticeIT | 0 | 2 |
| CompleteAdmissionConsentIT | 0 | 2 |
| CompleteAdmissionWishWarningsIT | 0 | 3 |
| FR_023h_KhongDauNangLucIT | 1 | 0 |
| FR_034a_CompleteAdmissionIT | 4 | 0 |
| ResidentContractIT | 1 | 0 |
| StatusHistoryIT | 0 | 1 |
| FR_035_LocationHistoryHttpIT | 1 | 0 |
