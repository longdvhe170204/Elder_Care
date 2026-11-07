# T030: phần trực tuyến đã cài và phần cần nền A

Ngày05/11, nhánh `impl/008-integration`. E chỉ sửa màn008 và ghép dòng điều hướng của008; chưa sửa `mobile/lib/common`, HTTP002 hoặc API của A.

## Đã có

`mobile/lib/staff/handover/handover_screen.dart` đọc ca/bàn giao/nguồn tham khảo, nhận định, mục và ý kiến theo dữ liệu đã lọc ở backend. Menu cần `SHIFT_HANDOVER/VIEW`. Các lệnh lập, hoàn tất, xác nhận, gửi ý kiến, phản hồi và gửi lại dùng đúng hợp đồng hiện hành và `allowedCommands`; không qua hàng đợi ngoại tuyến. Chưa nhận xác nhận thì giữ nội dung/khóa lệnh; mất quyền thì xóa nội dung. Không tự xác nhận hoặc chuyển trách nhiệm ở client.

## Hai điểm chưa đủ để đóng T030

| Điểm | Bằng chứng hiện tại | Phần cần A/chủ dự án review |
| --- | --- | --- |
| Đọc ETag cho `If-Match` | `backend/.../staffing/web/HandoverController.response` trả ETag; `HandoverBody` và schema Handover không có version. `mobile/lib/common/api/api_client.dart` chỉ trả JSON, bỏ header. Web đã đọc ETag qua client riêng | A/000 T010b: bổ sung cách đọc body và header ở client chung, giữ chữ ký/cách dùng get/post/requestJson cũ. E dùng ETag nguyên bản cho thêm/sửa/xóa mục và nhận định; không suy version từ id/trạng thái hoặc thêm version vào HTTP khi chưa duyệt |
| Lưu và xóa ngoại tuyến theo phạm vi | 008 FR060 và 002 FR032/033 yêu cầu đúng phạm vi hiện tại; có CFG-M15-07 và mốc kết thúc sớm. HTTP `/me/permissions` chỉ có chức năng/thao tác/loại phạm vi; `/me/shifts` không công bố hạn quyền lưu hoặc tập nội dung còn được xem | A/002 cung cấp hợp đồng nguồn phạm vi/hạn dùng phù hợp cho client; cần chủ dự án duyệt nếu thêm trường/endpoint. Không cố định hai giờ, lấy endsAt làm hạn, lấy allowedCommands làm quyền lưu, hoặc dùng đồng hồ thiết bị thay quyết định máy chủ |

Chưa chọn thêm endpoint hoặc trường. Khi có hợp đồng được duyệt, E cài kho của feature bằng nền đã có, kiểm xóa theo hạn/đổi phiên/thu hồi và không ghi ngoại tuyến. Không thay đổi schema hoặc SecureStore/OfflineQueue của A trong phiên này.

T030 vẫn `[ ]`: còn chỉnh nội dung dùng If-Match, cache và kiểm xóa khi hết phạm vi. Màn trực tuyến hiện có và test của nó không thay cho các phần này hoặc T044b. Việc cần review đã ghi team-plan mục8; không tự gửi tin nhắn, commit, push hoặc PR.
