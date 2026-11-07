# Bàn giao C/005: mốc nhận công việc tồn đã được chốt

Người dùng xác nhận ngày 2026-11-05: việc dự kiến **13:50**, Bình thuộc ca sau đã xếp **14:00–22:00**, nhận lúc **14:10** thì kiểm điều kiện Bình tại **14:10**, theo thời gian máy chủ xử lý lệnh. Không còn chờ chủ dự án quyết định mốc này.

Nguồn đã cập nhật: [nghiệp vụ mục 13.5 và 24.2](../../docs/business/nghiep-vu.md), [008 FR-044a và tiêu chí chấp nhận](spec.md). Quyết định áp dụng cho lệnh nhận việc tồn từ ca trước; không đổi quy tắc giao/phân lại công việc thông thường.

## Phần còn phải cài

| Chủ phần | Việc | Trạng thái thực tế |
| --- | --- | --- |
| C/005 | T038, `CareChecklistService.claimant`, `canClaim` và lệnh `claim`: kiểm ca sau/phân công/phạm vi/vai trò/chứng chỉ tại giờ nhận | Mã trong nhánh đang đọc chưa theo quyết định mới; E không sửa source/spec/test của C |
| E/008 | T043c: nguồn thật `care.api.spi.ShiftStaffPort` cho dữ liệu ca/phân công/chứng chỉ/bàn giao | Đã cài CareStaffLookup và T043c kiểm xanh05/11; không còn chờ vòng đời hai cờ, còn bàn giao/review consumer |
| C và E | Chạy lại nhận việc tồn, xác nhận bàn giao và bảo vệ Q-93 bằng nguồn thật | Chưa nghiệm thu; không dùng test fake hoặc test quyền xác nhận của E thay cho luồng này |

## Đúng chỗ C cần đối chiếu

[CareChecklistService.java](../../backend/src/main/java/com/eldercare/backend/care/internal/checklist/service/CareChecklistService.java) hiện có:

- `claimant`: lấy ca bằng `before.shiftId()`, kiểm người nhận qua `members(shift.id())` và `onShift(actor, before.plannedAt())`.
- `canClaim`: gọi cùng `claimant`, để quyết định có cho nhận việc hay không.
- `claim`: gọi `claimant`, nhận việc và ghi lịch sử/nhật ký trong transaction.

Vì vậy, chỉ thay `plannedAt` bằng giờ hiện tại vẫn có thể bị chặn bởi danh sách thành viên ca cũ. Với việc tồn, C cần đối chiếu đúng ca sau trong phạm vi công việc theo giờ nhận; không dùng giờ dự kiến cũ để đòi người nhận có mặt ở ca trước. Giữ kiểm quyền, khóa, ghi lịch sử và rollback hiện có; giao diện và lệnh dùng cùng điều kiện.

[ShiftStaffPort.java](../../backend/src/main/java/com/eldercare/backend/care/api/spi/ShiftStaffPort.java) đã khai báo các hàm `shiftContaining`, `members`, `onShift`, `qualified`, `assignments` và các truy vấn liên quan trong hợp đồng 005 mục 2.1. Quyết định này không thêm hoặc đổi chữ ký API/DTO/sự kiện. Nguồn thật của E đã được cài qua CareStaffLookup và kiểm xanh T043c ngày05/11; consumer claim và nghiệm thu cùng nguồn thật vẫn thuộc C/T043b, không coi interface hay test fake là luồng đã tích hợp.

## Kết quả cần kiểm, chưa đánh đạt

1. Bình đủ điều kiện lúc14:10 nhận được việc tồn dự kiến13:50 dù lúc13:50 chưa vào ca.
2. Người không thuộc đúng ca sau/phạm vi hoặc không đủ vai trò/chứng chỉ tại giờ nhận bị từ chối; từng đủ điều kiện ở giờ dự kiến cũ không thay thế kiểm hiện tại.
3. Giờ dự kiến13:50, trạng thái và lịch sử cũ được giữ; ghi thêm người nhận và thời điểm nhận, không tự tạo việc mới hoặc chuyển thành hoàn thành.
4. Việc đã nhận trong lúc chờ xác nhận bàn giao không tự đóng khi xác nhận (Q-93); không làm nhận lại được việc đã đóng.
5. Điều kiện hiển thị thao tác nhận và lệnh nhận thống nhất; quy tắc giao/phân lại công việc thông thường giữ nguyên.

Các bộ kiểm hiện có để C đối chiếu là `FR_030_CareChecklistIT`, `FR_047a_PendingHandoverIT`, `FR_047_TaskHandoverIT`; chưa khẳng định chúng đã bao phủ hoặc đã xanh theo quyết định này. C cần sửa spec/research/kiểm thử thuộc 005 theo nguồn nghiệp vụ mới và nghiệm thu với adapter thật của E.

Phiên này chỉ ghi quyết định và bàn giao, không sửa mã005 hoặc mở PR/gửi tin cho thành viên khác. Dòng chờ trong team-plan giữ lại phần cài/kiểm thử, không còn ghi chờ quyết định mốc nhận.

## Điểm đối chiếu consumer sau khi có nguồn008 thật

- Test HandoverIT/AssignmentIT của008 có import cấu hình fake005 để cô lập nguồn công việc. Các test mới chọn rõ bean careStaffLookup thay vì để FakeShiftStaff @Primary trả mặc định. Không sửa fake chung hoặc test005 của C. C cần chọn nguồn008 thật trong lượt tích hợp của mình, không coi test đang dùng fake là đã nối nguồn.
- CareChecklistService hiện chỉ gọi handoverTasks khi user.roles có NURSING_STAFF. C cần đối chiếu người có trách nhiệm là Trưởng tầng thuộc đúng ca sau với quyền checklist hiện hành (FR049): adapter008 tính quyền cho đúng staffId, nhưng điều kiện gọi consumer phải phù hợp quyết định13.5. Không tự mở quyền cho mọi nhân viên trong ca.
- Việc008 hoàn tất adapter không sửa thay claimant/canClaim hoặc năm callback care còn chặn theo bảng phối hợp. T043b và nghiệm thu nguồn thật vẫn độc lập.
