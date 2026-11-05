# Quyết định lượng tính phí 005 → 010 — 2026-11-05

Người dùng đã chốt trong cuộc trò chuyện; nội dung đã được ghi tại mục 4 báo cáo `completion-status-2026-11-05.md` của 008. Không còn chờ người dùng gửi quyết định. Tài liệu này bàn giao để C cài producer và E cài consumer, chưa xác nhận mã nguồn hai phía đã hoàn tất.

## Nội dung đã chốt

1. Vật phẩm: `quantity` là số lượng thực dùng, đúng đơn vị tính phí. Dịch vụ tính theo lần/buổi: `1` khi thực sự hoàn thành và có tính phí.
2. Không tự đổi ml, phút hoặc giá trị đo thành đơn vị dịch vụ; không mặc định `1` khi đơn vị không khớp. Quy đổi khác cần quyết định riêng. Không thực hiện thì không phát sự kiện dịch vụ hoàn thành.
3. `AMEND_CONTENT`: `newQuantity` là **tổng mới**, không phải chênh lệch; vẫn phát sự kiện đính chính tính phí nếu lượng giữ nguyên.
4. `VOID`: `newQuantity=null`, không dùng `0` làm tín hiệu hủy; giữ bản gốc và lịch sử.
5. 010 cập nhật nguồn kỹ thuật; Hành chính vẫn lập khoản bằng lệnh. Khoản chưa chốt tính lại/hủy theo quy tắc nguồn; khoản đã chốt giữ nguyên, báo Hành chính lập điều chỉnh. Không tự tạo khoản điều chỉnh hoặc tính sẵn tiền chênh lệch.

## Phần triển khai còn lại

| Chủ phần | Việc cần làm | Task |
| --- | --- | --- |
| C/005 | Cài `BillableCareResultRecorded.quantity` theo loại kết quả và kiểm đơn vị thực; bỏ gate chỉ sau khi producer có kiểm thử hợp lệ | T033/T036, Phase 6 |
| C/005 | Cài `BillableCareResultCorrected.newQuantity` cho AMEND_CONTENT/VOID; giữ căn cứ và khóa sự kiện hiện có | T050/T051, Phase 9 |
| E/010 | Cài listener/chỉ mục nguồn; kiểm chống trùng và cập nhật nguồn, rồi tính lại/thông báo đúng trạng thái khoản | T010/T021 |

Hai sự kiện hiện chưa mang trường đơn vị. Consumer không được tự suy đơn vị từ tên dịch vụ hoặc lượng đo. Khi nối nguồn thật phải đối chiếu dữ liệu công khai hiện có; nếu cần đổi hợp đồng của C/B thì đưa chủ phần review trước. Quyết định lượng ở trên không tự cấp quyền thêm trường API.
