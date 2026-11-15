# Đề xuất bổ sung TripDetail — chờ chủ dự án duyệt

Căn cứ: FR-036, FR-038, FR-039; dòng C (014) ở team-plan mục 8 chốt ngày 07/11/2026. Đây là đề xuất trường và nguồn, chưa sửa schema TripDetail hoặc cài API cho các phần dưới đây.

Mỗi phần gắn với một người tham gia, theo khoảng giờ rời/về hiện hành của chuyến. Không suy thuốc đã giao từ `isPrepared`, không suy sức khỏe an toàn từ kết quả đánh giá Đạt. Cần phân biệt nguồn không có dữ liệu, nguồn chưa kết nối/lỗi và người dùng không có quyền.

| Phần đề xuất | Trường cần hiển thị | API nguồn đã có và khoảng còn thiếu |
| --- | --- | --- |
| `medicationDoses` | Mã liều, mã đơn, thuốc (mã/tên/hàm lượng theo DTO của 006), giờ dùng, trạng thái hiện hành; danh sách liều trong khoảng đi | 006 có `MedicationScheduleApi.byResident(residentId, from, to)` trả `DoseDisplayView`. `TripMedicationApi.isPrepared` chỉ trả điều kiện chặn, không trả danh sách. Care không import medication vì chiều phụ thuộc ngược; cần D duyệt/cài adapter cho cổng đọc do care công bố. |
| `carriedMedication` | Liều đã giao mang theo, thời điểm giao, lượng giao, đơn vị; quyết định không mang thuốc và lý do nếu có | Chưa có API public trả giao thuốc theo người/chuyến. `DoseHandoverApi.inShift` phục vụ bàn giao ca, không thay thế nguồn chuyến; `pendingHandoverConfirmation` không chứng minh đã giao thuốc. D/006 cần chốt DTO và nguồn đọc thực tế trước khi khai chữ ký. |
| `health` | Dấu nghi nhiễm; cảnh báo và sự cố đang mở: mã, loại, mức độ, trạng thái, chi tiết khi được phép | 007 có `IncidentApi.monitoringFlag`, `AlertApi.byResident`, `IncidentApi.byResident`; care chỉ có `ActivityHealthGatePort` trả boolean. Không gọi incident/health trực tiếp vì vòng module. D/007 cần adapter về cổng đọc do care công bố và lọc quyền sức khỏe tại nguồn. |
| `careNotes` | Lưu ý chăm sóc của lần đánh giá Đã xác nhận gần nhất | Nguồn 001 có `AssessmentApi.currentCareNote(Collection<Long>)` và `latestConfirmed`. Dữ liệu này thuộc 001, không dựng lại từ kết quả đánh giá khả năng đi chuyến. Cần giữ quyền xem nhóm sức khỏe. |
| `primaryContact` | Mã người liên hệ, họ tên, số điện thoại | 007 đã khai `EmergencyFamilyPort.primaryContact` do 012 cài, trả `EmergencyFamilyContactView`. Đây là SPI 007, care chưa có đường đọc hợp lệ. Nguồn 012 cần được D phối hợp chuyển tiếp qua cổng care; chỉ Trưởng tầng và trưởng đoàn được xem số điện thoại theo FR-039. |
| `emergencyCard` | Nhận dạng (tên, ảnh, ngày sinh, phòng/giường); phần được phép: nguyện vọng cuối đời, dấu chăm sóc nguy kịch, dị ứng, thuốc hiện hành, bệnh mạn tính, lưu ý chăm sóc, liên hệ chính; thời điểm dữ liệu và mục nguồn chưa lấy được | 007 hiện có `EmergencyCardService.card(incidentId, CurrentUser)` ở internal và snapshot `EmergencyCard` gắn sự cố, chưa có API public chỉ đọc theo người/chuyến. Không gọi internal hoặc tạo sự cố để lấy thẻ. D/007 cần đề xuất nguồn đọc thẻ theo người, áp đúng quyền 007; thiếu quyền chỉ hiện phần nhận dạng được cho phép. |

Chưa đề xuất thêm trường về nguy cơ ngã/đi lạc vì Q-281 đã bỏ cờ nguy cơ. Không dùng thông tin demo thay nguồn còn thiếu.

Sau khi chủ dự án duyệt phạm vi trường ở bảng này, C và D cần chốt chữ ký care.api.spi/DTO, fake chung và điều kiện quyền; khi đó mới cập nhật api-014, sinh client, viết test đỏ và cài. T017/T018 giữ [ ] cho phần này.
