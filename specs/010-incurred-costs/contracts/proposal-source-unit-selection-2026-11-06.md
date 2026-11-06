# Đề xuất chọn đúng đơn vị nguồn — T011/T012, 06/11/2026

Trạng thái: **chủ dự án đã duyệt ngày 06/11/2026 trong cuộc trò chuyện** toàn bộ bổ sung sourceUnits/billingUnit như tài liệu. Đã áp dụng vào YAML và BillingSourceView của E; chưa có endpoint/lệnh lập khoản hoàn chỉnh, chưa đổi API nguồn.

FR-005 yêu cầu chọn từng bản ghi nguồn; DBR-15 giữ một khoản hiệu lực trên bộ loại nguồn/id/đơn vị tính phí. Chỉ mục có khóa `(source_type, source_id, billing_unit)`. Một lượt ở lại có nhiều đêm; một lần giao thuốc có thể có nhiều thuốc. `CreateCostsFromEvents.sourceIds` hiện chỉ gửi id, nên chưa thể diễn đạt việc chỉ chọn một đơn vị trong cùng nguồn. Không tự chọn đơn vị đầu tiên, đổi id gốc hoặc mặc định lập tất cả.

## Thay đổi HTTP đề nghị

Giữ đường dẫn, phương thức và các trường hiện có. Thêm trường tùy chọn `sourceUnits` trong `CreateCostsFromEvents`, gồm các phần tử `{sourceId, billingUnit}`; hai trường của mỗi phần tử bắt buộc. Đây là khóa chọn nguồn, không phải ô nhập lượng, giá hoặc số tiền. Ví dụ:

```json
{
  "residentId": 42,
  "sourceType": "OVERNIGHT_STAY",
  "sourceUnits": [
    {"sourceId": "stay-17", "billingUnit": "2026-11-04"}
  ]
}
```

Trong kết quả `BillingSource`, thêm `billingUnit` để trả khóa kỹ thuật tương ứng; giữ `unit` cho đơn vị sử dụng/hiển thị lấy từ nguồn theo FR-005 (ví dụ viên/gói/đêm). Không suy đơn vị vật lý từ id thuốc. Ví dụ một dòng được chọn có `sourceId=stay-17`, `billingUnit=2026-11-04`, `unit=đêm`, `quantity=1`.

Các kiểu chọn `sourceUnits`, `sourceIds`, `dateRange` loại trừ nhau. `sourceIds` cũ vẫn dùng khi mỗi id xác định duy nhất một đơn vị trong người/loại nguồn đã chọn. Nếu một id khớp nhiều đơn vị, trả dòng bỏ qua `CONDITION_NOT_MET`, lý do cần chọn đơn vị cụ thể; không tự lập khoản nào cho id mơ hồ đó. Giữ cách trả kết quả từng nguồn của `CostBatchResult`. Phí lưu trú tiếp tục dùng khoảng ngày như hợp đồng hiện có.

Nếu được duyệt: E sửa hợp đồng 010, DTO công khai liên quan, glossary mục 16, test hợp đồng và client sinh; ghi “Đổi api” ở team-plan mục 8 để A/các consumer review. Chưa triển khai trước quyết định.

## Nguồn dữ liệu còn thiếu, bàn giao chủ nguồn

- C/005: sự kiện có lượng và serviceId nhưng API công khai hiện chưa trả đơn vị sử dụng cùng căn cứ phân biệt vật phẩm tiêu hao/dịch vụ. Cần C cung cấp hợp đồng đọc metadata tối thiểu hoặc bổ sung sự kiện theo quy trình duyệt; không dùng API sức khỏe gia đình để thay nguồn tài chính.
- D/006: sự kiện có medicationId và lượng, `MedicationSummary` chưa trả usageUnit. Cần D cung cấp đơn vị dùng của bản ghi nguồn và đối chiếu DECIMAL(12,3) trong mã với DECIMAL(10,2) trong tài liệu. Khi một lần giao có nhiều thuốc, hợp đồng điều chỉnh cũng cần xác định đúng đơn vị; phần này thuộc T021, không coi T010 đã nghiệm thu producer thật.
- E không sửa API/fake hoặc truy vấn bảng riêng C/D. Đây là yêu cầu bàn giao hợp đồng, chưa phải tên interface/DTO mới được duyệt. Phần truy vấn chỉ mục E vẫn làm độc lập; phần lập khoản/HTTP đầy đủ chưa thể đánh xong khi thiếu các căn cứ trên.
