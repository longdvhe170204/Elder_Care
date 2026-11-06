> Ghép lại05/11: biên bản duyệt lịch sử,không phải kết quả kiểm hiện tại. Đã đưa đúng5operation/6schema sang nhánh008-integration và chạy lại19unit/arch+64IT xanh. Nguồn B attendanceOn và danh mục/kế hoạch C đã có ở checkout hiện tại;ghi chú thiếu nguồn29/10 bên dưới là lịch sử. Callback005/nghiệm thu mobile còn theo T022b/T043b/T044b. Chi tiết validation/completion-status.

# Hợp đồng bổ sung T054/T055 — đã được A/chủ dự án duyệt, 2026-10-29

E chuyển xác nhận trong phiên: A duyệt cả hợp đồng sửa Nháp và hợp đồng đọc kết quả; chủ dự án xác nhận **FR-025 cho đổi nhân viên khi cả phân công và ca còn Nháp**. Bố cục T022b và phạm vi ApiClient/StaffRoot cũng đã duyệt. Không còn chờ các quyết định này.

## 1. Sửa Nháp — phạm vi đã duyệt

- GET /assignments/{id}: dữ liệu hiện hành, shiftId/version và ETag; giữ mảng Assignment cũ.
- POST /assignments/{id}/draft-update-preview: UPDATE, If-Match, loại chính id đang sửa khỏi kiểm trùng; chỉ đọc.
- POST /assignments/{id}/update-draft: UPDATE, If-Match, Idempotency-Key; thay toàn bộ NewAssignment trên cùng id, giữ nguồn/lịch sử tạo, tăng version; khóa và kiểm lại dưới transaction. Không cần CREATE.
- Đổi staffId chỉ khi phân công, ca và lịch đều Nháp. Sau công bố giữ luồng FR-027. Không có DELETE rồi POST thay sửa; không sửa spec hoặc nguồn nghiệp vụ.
- Nháp không tạo phạm vi/tác động công việc/liều hoặc nhật ký từng lần sửa (FR-025/055). Cảnh báo trước lưu; sửa form phải xem trước lại. Lỗi 412 trả currentValue hiện hành sau kiểm quyền.
- Tên updateDraftAssignment được bổ sung vào allowedCommands khi Nháp có UPDATE; các tên/lệnh cũ giữ nguyên.

Hợp đồng hoạt động: [api-008.yaml](contracts/api-008.yaml). Bản duyệt riêng: [proposal-assignment-draft-update.yaml](contracts/proposal-assignment-draft-update.yaml). id/status được khai báo lại cùng kiểu trong nhánh allOf để bộ đọc OpenAPI không bỏ hai trường bắt buộc đã duyệt; ý nghĩa JSON không đổi.

## 2. Đọc kết quả — phạm vi đã duyệt

- GET /shifts/{id}/assignment-resolution: residentId, at, role bắt buộc, taskTypeId tùy chọn. at thuộc [bắt đầu, kết thúc). Backend chọn người chính theo vai trò/công việc và Điều dưỡng phụ trách riêng; chỉ phân công có hiệu lực trong đúng ca, đúng vị trí/thành viên tại mốc.
- GET /shifts/{id}/unassigned-residents: evaluatedAt bằng giờ bắt đầu ca, hai cờ thiếu Điều dưỡng phụ trách/nhân viên chăm sóc chính không giới hạn. Xét lịch sử vị trí, trạng thái cuối/vắng và lịch đến bán trú. Không tạo snapshot hoặc nhắc khi GET.
- Kiểm quyền/phạm vi qua PermissionApi/ScopeApi. Có tên trong ca không tự cho xem toàn tầng; danh sách thiếu người dành cho Trưởng tầng/Người phụ trách ca và Quản lý viện trong phạm vi. Ca Nháp/Đã hủy trả STATE_CHANGED.
- Thiếu/mâu thuẫn nguồn trả 422 ASSIGNMENT_SOURCE_UNAVAILABLE; null/danh sách rỗng chỉ là kết quả đủ nguồn. Giữ mảng Assignment cũ, không đổi Java API/SPI của B/C/D/A.

Bản duyệt riêng: [proposal-assignment-resolution.yaml](contracts/proposal-assignment-resolution.yaml); năm thao tác đã nhập api-008.yaml.

## 3. Kiểm chứng và giới hạn nguồn thật

Test được viết trước: mười test sửa Nháp lỗi ở endpoint chưa có; mười hai test đọc lỗi 404 trước cài. Bổ sung kiểm phiên bản, chống lặp, hai người sửa/công bố đồng thời, UPDATE không CREATE, rollback sau SQL, lịch sử chuyển phòng, vắng, bán trú, nguồn thiếu/mâu thuẫn, quyền và ca hủy. ApiContractIT của A được giữ nguyên để đối chiếu hợp đồng.

develop 685dea6 chưa có bản thật ContractReadApi.attendanceOn của B (004), TaskTypeApi/CarePlanApi/ShiftTaskApi và danh mục loại công việc của C (005). Test dùng fake chung đúng chủ, ngoài test không có fake. T055 đọc danh sách đầy đủ sẽ báo thiếu nguồn tới khi B cung cấp lịch đến; T022b chưa nghiệm thu luồng thật bằng kiểm widget. Các việc này ghi team-plan mục 8.

Chi tiết lớp, lệnh kiểm và kết quả cuối ở plan.md, tasks.md và dòng mới nhất docs/progress.md. Không commit/push/PR/merge trong phiên này.
