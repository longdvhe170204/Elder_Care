# Rà phản hồi HTTP 010 cần chốt — 2026-11-04

Đối chiếu nguyên trạng `contracts/api-010.yaml` trên develop `62fdb4f2`. Bản dịch đã được duyệt ngày 29/10; lượt này không sửa hợp đồng hoặc tự thêm DTO. Theo plan mục “Chuẩn bị triển khai 2026-10-29”, phải chốt cấu trúc trước endpoint tương ứng.

Có 17 phản hồi thành công chỉ ghi description, không khai content/schema. Đây không tự động là lỗi cú pháp OpenAPI: với lệnh, có thể chủ đích trả body rỗng. Cần chủ dự án xác nhận body rỗng hay bổ sung schema trước khi viết controller/client. Riêng hai GET phải có cấu trúc dữ liệu để làm màn tra cứu; không tự suy tên trường REST từ tên cột SQL.

| Thao tác | Mã hiện có | Dòng trong hợp đồng |
| --- | --- | --- |
| POST /costs/items/manual | 201 | 61 |
| POST /costs/items/{id}/adjust | 201 | 71 |
| POST /costs/items/{id}/uncheck | 200 | 94 |
| POST /costs/items/{id}/return | 200 | 112 |
| POST /costs/items/{id}/cancel | 200 | 120 |
| GET /costs/post-finalization-changes | 200 | 127 |
| POST /costs/statements/{id}/return | 200 | 159 |
| POST /costs/statements/{id}/finalize | 200 | 167 |
| GET /costs/periods | 200 | 173 |
| POST /purchase-assistance-requests | 201 | 189 |
| POST /purchase-assistance-requests/{id}/send | 200 | 197 |
| POST /purchase-assistance-requests/{id}/agree | 200 | 204 |
| POST /purchase-assistance-requests/{id}/reject | 200 | 212 |
| POST /purchase-assistance-requests/{id}/request-additional-consent | 200 | 220 |
| POST /purchase-assistance-requests/{id}/withdraw-additional-consent | 200 | 228 |
| POST /purchase-assistance-requests/{id}/cancel | 200 | 236 |
| POST /purchase-assistance-requests/{id}/record-purchased | 200 | 245 |

Nếu chọn trả bản ghi sau lệnh, cần duyệt liên kết đến schema đã có (CostItem/CostStatement/PurchaseAssistanceRequest) hoặc cấu trúc khác; hiện chưa có quyết định này. Không đổi mã 200/201 thành 204 để tự giải quyết. Hai GET cần duyệt schema danh sách kỳ và biến động sau chốt. Các endpoint đã có schema vẫn theo hợp đồng hiện tại; không xem danh sách này là lý do dừng toàn bộ phần nền.
