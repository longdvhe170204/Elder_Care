# Đề xuất thông báo tính lại giá — Phase 4/T014

Trạng thái: người dùng đã giao chọn/cài phương án phù hợp spec trong cuộc trò chuyện 06/11; E áp dụng đề xuất, thêm hỗ trợ JSON outbox cũ thiếu notificationRequests bằng danh sách rỗng. Chờ A/consumer review và kiểm thử trước merge.

T014/FR-004/FR-012/FR-020 cần báo Hành chính khi giá được bổ sung hoặc khoản đã kiểm/duyệt bị đưa về Nháp. M13 chỉ tiếp nhận sự kiện triển khai HasNotificationRequests; CostItemChanged hiện chỉ có residentId/periodId. Không gọi implementation nội bộ M13, không sửa A/common, không dùng sự kiện mua hộ cho tác động tính lại giá.

Đề xuất mở rộng sự kiện hiện có, giữ hai trường và constructor cũ:

```java
public record CostItemChanged(long residentId, long periodId,
        List<NotificationRequest> notificationRequests) implements HasNotificationRequests {
    public CostItemChanged(long residentId, long periodId) {
        this(residentId, periodId, List.of());
    }
    public CostItemChanged {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
```

Thông báo Nhẹ, InformationType.COSTS, chỉ nhóm FacilityRole(OFFICE_STAFF); không có tên thuốc, danh sách id dài hoặc số tiền chênh lệch tính sẵn. SourceRecord trỏ một khoản đã đổi, hoặc bảng nếu thông báo bảng; eventKey chống lặp theo khoản và căn cứ tính lại. Không tự tạo khoản điều chỉnh. Không thông báo khi phép tính/căn cứ không đổi hoặc thiếu giá vẫn chưa được giải quyết.

Nếu được duyệt: E sửa record, ghi “Đổi api” ở module-interfaces/glossary/team-plan để A (009/016), consumer 017 review; test đọc lại outbox và constructor cũ. Chưa đổi chữ ký API, DTO hoặc mã A/B/C/D trong đề xuất này.
