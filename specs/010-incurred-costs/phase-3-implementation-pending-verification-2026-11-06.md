# Phase 3 — phần triển khai sau checkpoint chung, chờ kiểm tra

Nền d9a62fc5, nhánh impl/010-foundation-next. Người dùng yêu cầu gom triển khai trước, verify sau; cuối mỗi phase phải báo và dừng chờ người dùng quyết định merge rồi mới sang phase kế. Không tự commit/push/merge/đổi nhánh hoặc mở PR.

## Đã cài, chưa kiểm lại

- CreateCostsFromEventsRequest: residentId/loại nguồn bắt buộc, đúng enum hợp đồng; chọn đúng một kiểu sourceIds/sourceUnits/dateRange, danh sách không rỗng/trùng, phần tử bắt buộc và khoảng ngày có thứ tự hợp lệ. Getter kiểm tra có JsonIgnore, không thêm trường vào JSON đã duyệt. Không có ô lượng/giá/hệ số/số tiền trong thân lệnh.
- ResidencyBillingSources.between: gọi ResidencyDayApi mỗi lượt, giữ nguyên căn cứ hợp đồng/phiên bản, lượt vắng, giường giải phóng, chưa vào ở và cờ ngoài khoảng của B. Khóa ngày là residentId:date/DAY theo data-model; nối khoản hiệu lực qua cost_item, không ghi hoặc phụ thuộc cost_source_index. Nguồn đã có khoản vẫn được trả.
- BillingSourceIndexStore.effectiveItems: truy vấn tham số hóa, giới hạn người/loại/id nguồn, chỉ EVENT_BASED chưa hủy; danh sách rỗng trả map rỗng. Không đọc bảng riêng B/C/D hoặc ghi dữ liệu khi tra cứu.
- Web ở đúng thư mục charges đã đăng ký trong frontend/README: BillingSourceResults và sourceSelection. Chọn đơn vị bằng billingUnit, không suy từ unit hiển thị; khóa lựa chọn có cả người/loại để tránh mang lựa chọn sang người khác. RESIDENCY_DAY tiếp tục gửi khoảng ngày được người dùng chọn, không đổi sang sourceUnits.
- Bảng giữ nguồn đã có khoản, có thao tác mở khoản qua callback, hiển thị từng lý do bỏ qua; không có lọc “chưa lập khoản”, không nhập lượng/giá/tiền. Nhận canCreate từ màn gọi, không suy quyền từ vai trò. Xác nhận trước lệnh chỉ có hiệu lực với các dữ liệu đã xem; đổi lựa chọn/lượng/căn cứ hiển thị thì xác nhận cũ không còn hợp lệ. Chỉ nhận mô tả đã lọc quyền từ backend, không gọi API thuốc lấy thêm tên.
- Đây là component và xử lý nội bộ, chưa có màn trực tuyến/controller/lệnh lập khoản hoàn chỉnh. Không sửa bộ sinh client A, client sinh tự động hoặc đăng ký màn giả như đã dùng API thật.

## Kiểm thử và giới hạn bằng chứng

- Trước cài đã chạy riêng 25 ca: 16 failure ở kiểm thân lệnh, 5 error ở hàm tra cứu chưa cài, 4 control xanh. Đã thay các hàm khung bằng xử lý thật; chưa chạy lại để kết luận xanh theo yêu cầu hoãn kiểm của người dùng.
- Thêm 3 IT ở CostSchemaIT cho truy vấn khoản của ngày tính khi đọc: không cần chỉ mục nguồn, đúng phạm vi, khoản hủy/được thay thế và danh sách rỗng. Thêm test web cho chọn đúng đơn vị, giữ phạm vi, khoảng ngày lưu trú, xác nhận, quyền, lượng thuốc và lý do bỏ qua. Các test bổ sung chưa chạy.
- Không dùng 164 unit/arch + 97 IT của lượt trước làm kết quả cho thay đổi mới hoặc nền d9a62fc5 đã ghép thêm mã khác. T009/T011/T012 giữ [ ]; chưa nghiệm thu Phase 3, chưa sẵn sàng merge phase bằng checkpoint này.
- Không chạy thêm Maven verify, bộ test tổng, npm test/lint/build hoặc migrate dev trong lượt triển khai này. Còn định dạng và bộ kiểm cần chạy trước nghiệm thu/merge.

## Phần còn chặn Phase 3

- C/005 và D/006: metadata đơn vị/loại nguồn và producer thật theo source-metadata-handoff-2026-11-06.md. B/004: metadata dịch vụ/cách chọn dịch vụ nguồn; chưa tự coi thiếu serviceId là thiếu phiên bản giá hoặc giá 0.
- Buổi bán trú: DayCareSessionView.sessionId của B là số thứ tự trong ngày, không phải id toàn cục. Không lập khóa chỉ từ số đó hoặc đoán ánh xạ khi lịch bị đổi/thêm buổi; cần B/C đối chiếu định danh để nối đúng nguồn và tính lại. Phần này chưa cài bằng cách tự đặt id hay hệ số cho buổi chưa xác định.
- A/000: TRANSLATED của bộ sinh client hiện có 004 nhưng chưa có 010. Chưa sửa script chung hoặc gọi fetch/URL tự viết để né chờ.
- Còn lệnh lập khoản/bảng, kiểm trùng đồng thời, thời gian tính phí, định giá/nhật ký/thông báo và nối controller/màn API. T011/T012 chưa xong chỉ vì component/query đã có.

## Lõi E tiếp tục độc lập

- Phụ thuộc metadata B/C/D/client A chỉ chặn adapter và tích hợp, không chặn toàn bộ phần E của Phase 3. Tiếp tục lõi T009/T011 độc lập, không chờ nguồn mới rồi mới viết luật/khóa/bảng.
- ResidencyDayEligibility cài FR-007/011/Q-395: ngày trước nhập viện vẫn theo mốc hợp đồng; từ ngày giải phóng giường không lập phí; hôm nay/ngày sau chưa kết thúc bị chặn trừ phạm vi lập trước của bảng kỳ cuối; giữ ngày qua đời trọn ngày theo BillingPeriod. Mốc ngày/zone caller truyền từ nguồn và BusinessTime, không tự lấy giờ hoặc đoán nguồn còn thiếu.
- DraftStatementTransition và CostDraftStatements/DraftCostStatementStore cài bước chuẩn bị bảng nhận Nháp: MANDATORY transaction của caller, khóa M11 theo người trước kỳ/bảng; khóa dòng bảng nếu đã có, tránh khóa khoảng rỗng; tạo bảng thường khi caller đã chấp nhận khoản đầu, dùng lại Đang mở, mở lại Chờ chốt và tăng version. Kỳ hoặc bảng đã chốt trả requiresAdjustment, không mở lại hoặc tự sinh khoản điều chỉnh. Không tự tạo kỳ khi kỳ chưa có.
- Đây là bước nội bộ của lệnh, chưa phải endpoint/lệnh hoàn chỉnh: caller phải kiểm nguồn rồi gọi cùng transaction ghi khoản và nhật ký; không gọi riêng để lập bảng trống hoặc từ job. Phần ghi khoản, orchestration hàng loạt, định giá và thông báo/nhật ký của lệnh còn phải nối tiếp.
- Đã thêm test luật thuần, điều phối thứ tự khóa/bảng và IT tạo/dùng lại/mở lại/giữ nguyên bảng chốt. Chưa chạy các test này hoặc verify theo yêu cầu gom triển khai trước; không đánh T009/T011 hoặc Phase 3 xong. Tiếp tục phần E, vẫn chờ người dùng quyết định merge tại cuối phase trước khi chuyển phase.
