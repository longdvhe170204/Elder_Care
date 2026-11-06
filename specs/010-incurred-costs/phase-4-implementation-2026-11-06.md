# Phase 4 — đã triển khai mã, chưa kiểm chứng

Nhánh impl/010-foundation-next, nền 2cb7e892. Người dùng cho làm Phase 4 độc lập, giữ Phase 3 mở; cấm chạy verify khi chưa được cho phép. Cuối phase báo/dừng để người dùng quyết định kiểm tra và merge; chưa chuyển Phase 5. Không tự commit/push/merge/đổi nhánh/mở PR.

## T014: phần triển khai

- Dùng lại CostPricingService.event cho sự kiện; tách catalogue để dùng đúng ngoại lệ buổi bán trú phát sinh. Giá hợp đồng còn thiếu không tự lấy giá danh mục thay thế. Thuốc được phân giải qua facilityMedicationServiceOf, lượng nguồn giữ nguyên. Thiếu ánh xạ dịch vụ là SOURCE_UNAVAILABLE, khác thiếu phiên bản đơn giá.
- Bổ sung phân giải dịch vụ phí lưu trú theo loại phòng và dịch vụ chăm sóc theo mức trong nội dung hợp đồng tại ngày nguồn. Dịch vụ chăm sóc hiện tại phải khớp dịch vụ đã đăng ký, không tự gán dịch vụ mới vào hợp đồng lịch sử.
- CalendarCostPricing: phí theo tháng/ngày, ngày cuối tháng giữ chênh lệch làm tròn; ngày vắng chưa giải phóng giường vẫn 100%, ngày đã giải phóng bị từ chối. Lưu giá gốc tháng và căn cứ riêng với đơn giá ngày để tái hiện phép tính.
- Buổi bán trú có lịch: dùng hệ số nguồn đã xác định, không suy mặc định khi thiếu; lịch tháng lấy từng phiên bản hợp đồng tại ngày, loại ngày khu nghỉ, không tính buổi phát sinh vào mẫu số, buổi có lịch cuối tháng nhận chênh lệch rồi mới nhân hệ số. Snapshot lịch dùng lại trong cùng lô/transaction, không cache toàn cục. Không dùng API lịch bị cắt theo mốc dừng để làm sai mẫu số.
- Buổi phát sinh: giá buổi trong hợp đồng khi có; hợp đồng tháng hoặc chưa ghi giá buổi thì dùng phiên bản dịch vụ buổi riêng, đánh dấu tham chiếu trong transaction. Giữ đúng serviceId thực sự dùng giá, không gắn id dịch vụ tháng cho phiên bản giá buổi khác. Hệ số 100% theo Q-140.
- CostPricingSnapshots/CostPricingSnapshot dựng calculation, hai dấu định giá và cột unit_price_basis/source_values đã có. Giữ BigDecimal lượng 0,125, NULL tiền khi thiếu giá, 0 khi thuộc gói; thay dấu giá không xóa dấu lập trước/chưa vào ở. Giữ các fact nguồn có giá trị JSON null.
- CostPriceEvents nhận PriceVersionCreated/AddendumApplied sau commit, CostPriceRecalculator dùng transaction của listener: khóa người/bảng/khoản; giá mới chỉ chọn EVENT_BASED thiếu giá trong khoảng hiệu lực, không đụng khoản đã có giá; phụ lục xét hợp đồng đúng người/ngày, xét cả tháng cho khoản bán trú vì mẫu số có thể đổi.
- CostRepricingStore kiểm lại khoản chưa khóa sau scan, không sửa FINALIZED/CANCELLED hoặc bảng đã khóa. Dùng cổng @m11_recalculating và khôi phục giá trị cũ trong finally; cập nhật giá/căn cứ/snapshot, về DRAFT, xóa căn cứ kiểm/duyệt và mở lại bảng Chờ chốt nếu cần. Không tạo bảng/khoản/điều chỉnh mới.
- Tính lại giá dựa snapshot lượng/trạng thái nguồn đã được E chấp nhận; không coi việc API danh sách hiện tại không trả lịch sử là nguồn bị hủy. Sửa/hủy/đổi thời gian nguồn và biến động sau chốt thuộc T021; nguồn đã chuyển loại không bị ép qua bộ tính giá cũ. Giá/căn cứ thực sự không đổi được bỏ qua, so BigDecimal theo giá trị để tránh thay đổi giả do scale.
- Cùng transaction ghi marker theo người/khoản/căn cứ, lịch sử trước/sau (kể cả căn cứ và giá trị nguồn), nhật ký có căn cứ và sự kiện CostItemChanged. Sự kiện phát lại không ghi lịch sử/thông báo lặp; lỗi nguồn/hạ tầng không bị biến thành giá 0, lỗi ghi làm rollback tác động và marker.

## Quyết định người dùng và Đổi api

- Người dùng giao chọn cách khớp spec/nghiệp vụ. Theo Q-297, giữ dịch vụ phí lưu trú và chăm sóc riêng, không tự cộng giá hoặc tạo thêm bản ghi nguồn. Ví dụ dịch vụ phí lưu trú giá tháng 6 triệu/tháng 30 ngày cho ra 200.000/ngày; 3 triệu của dịch vụ chăm sóc không bị tự cộng thành 300.000. Phase 3 vẫn phải dùng nguồn hợp lệ cho từng dịch vụ, không tự sinh khoản chăm sóc.
- Người dùng cũng giao chọn phương án cho đề xuất thông báo: áp dụng mở rộng CostItemChanged với notificationRequests/HasNotificationRequests. Giữ constructor hai tham số; JSON outbox cũ thiếu trường được đọc thành danh sách rỗng. Không thêm endpoint/field HTTP hoặc sửa API của A/B/C/D.
- Thông báo Nhẹ cho Hành chính qua M13, không tên thuốc/danh sách id dài/chênh lệch tính sẵn. Có bộ dựng thông báo thiếu giá ban đầu để T011 ghép vào lệnh, và thông báo khi bổ sung được giá hoặc khoản/bảng phải kiểm lại. Chưa gửi thông báo từ helper giá đơn thuần.
- CostRecalculation mở rộng payload lịch sử nội bộ với basisBefore/basisAfter/sourceBefore/sourceAfter; giữ constructor cũ và đọc payload cũ được. Không sửa bảng hoặc lịch sử đã lưu.

## T013 và cổng kiểm đang mở

- Đã viết/bổ sung unit và IT cho giá tháng/ngày, phụ lục đổi giá theo ngày, phần dư cuối tháng/buổi, hệ số 0/thiếu, ngày khu nghỉ, buổi phát sinh, lượng thuốc, ánh xạ dịch vụ, snapshot, outbox cũ, chọn khoản/chống lặp, trạng thái chốt/hủy, mở lại bảng và rollback khi nhật ký lỗi.
- Chỉ chạy spotless:apply trên 19 file Java của Phase 4: thành công, 18 file định dạng lại/1 file đã sạch. Lệnh này không biên dịch, không chạy test và không chứng minh hành vi đúng.
- Không chạy compile/test/IT/verify hoặc bộ kiểm frontend. T013/T014 giữ [ ]; chưa có bằng chứng đỏ→xanh cho mã Phase 4. Không lấy kết quả 261 ca của lượt trước hoặc chữ BUILD SUCCESS của formatter làm kết quả kiểm thử.
- Chưa nghiệm thu/merge phase. Trước merge cần người dùng cho phép các kiểm cần thiết, xử lý kết quả và review Đổi api với A/consumer. Phase 3 còn phải nối bộ giá/snapshot/thông báo vào lệnh lập khoản; không đánh Phase 3 xong bằng phần triển khai này. T021/T033 vẫn giữ phần nguồn thay đổi/sau chốt/tích hợp thật của chúng.
