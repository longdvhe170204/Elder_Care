# Review API M09 cho 009 và 016 — 2026-10-28, E

Phạm vi: hai dòng chờ E review ở team-plan mục 8, trên `origin/develop` `685dea6`. Nhánh tài liệu `docs/008-api-review`, worktree `.worktrees/008-api-review`. Đã đọc thay đổi `35ed5ad` (ShiftApi cho 009) và `3062c62` (thống kê cho 016), hợp đồng 008 mục 1.3–1.4, plan/spec 009 và 016, mã cài, DTO, bản giả và các nơi dùng liên quan. Không sửa mã triển khai hoặc test trong kho.

## 1. Kết luận

- `StaffStatisticsApi`: chấp nhận phần thay đổi của 008 cho 016 trong phạm vi đã review; chưa thấy lỗi cần chặn ở hàm mới và trường mới.
- `ShiftApi`: hai hàm đọc khớp hợp đồng; còn một lỗi độ ổn định của dữ liệu test cần E sửa ở T053. Chưa coi gói kiểm thử này đã ổn định khi pool đổi kết nối.
- Đã hoàn thành việc review T052. Theo quy định mục 8, xóa hai dòng chờ review và ghi lỗi cụ thể thành dòng mới cho E; không ghi toàn phase hoặc toàn hệ thống đã nghiệm thu.

## 2. Phát hiện cần sửa

### P2 — ShiftQueriesIT lấy ID từ kết nối khác

- Vị trí mới trong thay đổi `35ed5ad`: `backend/src/test/java/com/cambae/backend/m09/ShiftQueriesIT.java:183` và `:188`, trong `FR_023_onDutyDoctorsArePresentDoctorsMarkedOnDutyFor009`. Sau INSERT lịch/ca, test gọi SELECT LAST_INSERT_ID bằng một lần lấy kết nối khác. Helper `fixture` được cả hai test API mới sử dụng còn cùng mẫu này ở dòng 78, 82, 89; ba chỗ helper đã tồn tại trước thay đổi A.
- Điều kiện: pool trả kết nối B cho SELECT sau khi INSERT chạy trên A. LAST_INSERT_ID là giá trị của phiên B nên không bảo đảm là ID của bản ghi vừa thêm. Test có thể lỗi khóa ngoại trước khi kiểm được `onDutyDoctors` hoặc `staffOnShift`.
- Đã tái hiện với lớp `ShiftQueriesIT` thật đã biên dịch, hai kết nối MySQL thật và DataSource đổi kết nối. Phiên B được đặt LAST_INSERT_ID bằng 0, gọi chính helper `fixture` với một nhân viên và tầng chưa dùng: nhận `DataIntegrityViolationException`, mã MySQL 1452, khóa ngoại `work_shift.template_id`. Mọi INSERT thử nằm trong transaction và được rollback; không thay đổi tài khoản, schema hoặc quyền.
- Cách sửa đề nghị: chuyển phép tái hiện thành test thường trú; dùng `AssignmentFixtures.insert` có sẵn để lấy generated key trên cùng kết nối ở cả năm chỗ, giữ nguyên assertion nghiệp vụ. Không cần đổi API hoặc production. Đây là T053, tách khỏi T051 đã sửa `M09GovernanceIT`.
- Bằng chứng cục bộ, bị Git bỏ qua: `backend/target/008-api-review-verification/ReviewProbe.java` và `fixture-reproduction.txt` trong worktree gốc. Phép tái hiện này là bằng chứng review, chưa thay thế test hồi quy thường trú của T053.

## 3. Những điểm đã đối chiếu

| Phần | Kết quả đọc mã và hợp đồng |
| --- | --- |
| `openHandovers(scope)` | Store chọn PREPARED/HAS_COMMENTS, thêm DRAFT khi ca AWAITING_HANDOVER; loại ca DRAFT/CANCELLED; không cắt theo ngày. Service lọc ScopeFilter trước khi dựng view, giữ giờ kết thúc ca gốc. Test có ca cũ 40 ngày, bản nháp chưa chờ bàn giao, bản đã xác nhận và tầng ngoài phạm vi |
| Trường tỷ lệ mới | `staffCount`, `totalWeight` lấy từ lần tính được lưu; mẫu ca và giờ ca lấy từ ca cha. Store vẫn chọn lần tính cuối trước khi ca kết thúc, giữ dấu từng không đạt; không thay công thức, ngưỡng hoặc cách lọc của hàm cũ |
| `shiftEndsAt` | Được điền ở cả hàm bàn giao theo khoảng cũ và hàm đang mở mới; consumer 016 dùng để tính thời gian chờ |
| `credentialGroup` | Lấy nhóm từ loại chứng chỉ; bộ lọc ngày/hiệu lực của hàm cũ giữ nguyên. Consumer 016 phân cửa sổ giấy phép/đào tạo theo hai CFG đã có |
| `staffOnShift` | Dùng nguồn ca FLOOR/AREA đã công bố, xét khoảng giờ nửa mở, không phụ thuộc job đã chuyển PUBLISHED sang IN_PROGRESS. `presentAt` loại vắng, rời ca, rút do nghỉ việc, nghỉ được duyệt và thời điểm trước khi bổ sung; loại ID trùng. Vai trò/chứng chỉ của nhóm nhận do 009 lọc theo hợp đồng |
| `onDutyDoctors` | Ủy cho `doctors` hiện có: chỉ ca FACILITY tại thời điểm hỏi, có dấu on_duty_doctor, còn có mặt, có vai trò DOCTOR, loại trùng. Dấu trực trên ca tầng không được tính |
| Bản giả và nơi dùng | FakeShifts có reset/đặt Bác sĩ trực và hai hàm mới; FakeStaffStatistics có openHandovers. Bản giả thống kê nhận dữ liệu đặt sẵn, không mô phỏng toàn bộ quyền/trạng thái ca. Các nơi tạo ba DTO đã cập nhật và được biên dịch trong lượt verify |

## 4. Bằng chứng kiểm và giới hạn

Tận dụng lượt `clean verify` đã hoàn tất trong phiên này trên cùng HEAD `685dea6` ở nhánh `impl/008-governance-fixture`: phần Java khác HEAD chỉ là M09GovernanceIT, không thuộc mã đang review; đã so SHA-256 của hai service và hai lớp test chính với nguồn dùng trong lượt đó. Không chạy lại toàn kho khi nhánh review chỉ sửa tài liệu.

| Lớp liên quan đã xanh | Số test |
| --- | --- |
| StaffStatisticsReportFieldsIT | 3 |
| StaffStatisticsIT | 7 |
| ShiftQueriesIT | 7 |
| MetricShiftStaffingRatioTest | 3 |
| MetricShiftHandoversTest | 1 |
| MetricLicenseStaffTest | 2 |
| BR_M13_04_ShiftGroupsIT | 5 |
| FR_020_StaffingDashboardIT | 1 |

Tổng 29 test liên quan xanh, không bỏ qua. Lượt toàn kho có 637 test đơn vị/kiến trúc xanh; 1427 IT còn 7 thất bại + 9 lỗi nền ở M01/M03; riêng M09 345 test xanh, Spotless 2006 file qua. Kết quả xanh thông thường không phủ điều kiện đổi kết nối đã làm phép tái hiện lỗi fixture ở mục 2.

Chưa đo hiệu năng, chưa chạy trên thiết bị hoặc nghiệm thu toàn luồng thông báo 009 với mọi nguồn thật. Các test consumer dùng bản giả không được ghi thành bằng chứng toàn luồng nguồn thật; T043b/T044b/T049b và các review khác giữ nguyên. Không có phát hiện cần đổi chữ ký API, migration hoặc nghiệp vụ trong hai phần được review.

## 5. Khắc phục T053 — 2026-10-28

Phát hiện P2 ở mục 2 đã được xử lý trên nhánh `impl/008-shift-query-fixture` (worktree `.worktrees/008-shift-query-fixture`), chưa gộp vào develop. Test thường trú kiểm đổi kết nối cho cả ca tầng lẫn ca Bác sĩ trực đã đỏ riêng ở hai helper rồi xanh; năm chỗ dùng AssignmentFixtures.insert, giữ nguyên assertion cũ và chọn tháng riêng cho test mới. ShiftQueriesIT 8/8 xanh cả lượt riêng lẫn toàn kho; M09 345 test xanh, Spotless 2006 file qua. Clean verify chạy đủ 14:16, 637 đơn vị/kiến trúc xanh, 1427 IT còn đúng 7 thất bại + 9 lỗi nền M01/M03. Đã xóa dòng chờ sửa T053 trong team-plan của worktree review; các phần giới hạn nghiệm thu tại mục 4 vẫn giữ nguyên.
