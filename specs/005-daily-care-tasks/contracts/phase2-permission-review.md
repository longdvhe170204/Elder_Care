# Điểm cần chủ dự án quyết — quyền danh mục Phase 2

Ngày 2026-10-27. Căn cứ: plan C-16 dùng `LOAI_CONG_VIEC/CAU_HINH`; spec FR-038 và FR-028a cho Quản lý viện cấu hình. Nhưng `m15.account.api.dto.Function` và Phụ lục 27 hiện không có dòng danh mục này; mục 24.1 không có Q đang mở. Không tự đặt PermissionKey, không sửa nghiệp vụ hoặc Function của A.

Đã hỏi trong phiên, **chưa nhận câu trả lời** cho điểm quyền (khác với bản dịch HTTP và hai operation result-values đã được duyệt).

Phương án đề nghị: như tiền lệ M13 không có dòng thông báo trong Phụ lục 27, kiểm `CurrentUser.roles()` có `NURSING_HOME_MANAGER` trong lệnh, ghi nhật ký/transaction/Reason bình thường. Thêm đúng chín phương thức dự kiến vào `RequiresPermissionArchTest.SELF_CHECKED`, ghi căn cứ FR-038/FR-028a và giao A review:

- `m04.internal.catalog.service.CareCatalogService.createTaskType`, `updateTaskType`, `deactivateTaskType`.
- `m04.internal.catalog.service.CareCatalogService.createResultValue`, `updateResultValue`, `deactivateResultValue`.
- `m04.internal.catalog.service.NonPerformanceReasonService.create`, `update`, `deactivate`.

Phương án còn lại: chờ A công bố dòng quyền/enum chính thức theo nghiệp vụ đã được chủ dự án chốt, rồi dùng `@RequiresPermission` đúng khóa đó. Không mượn `TASK_CHECKLIST` hoặc `SHIFT_TASK_PLANNING` vì đây là chức năng khác.

Trong lúc chờ: chỉ làm API/sự kiện, schema, khóa/job, provider, kiến trúc, bản dịch và test hợp đồng. Đã có migration lý do cố định; **chưa có service/controller danh mục**, chưa sửa SELF_CHECKED, không để endpoint ghi chạy thiếu quyền. T007/T007a tiếp tục `[ ]`.
