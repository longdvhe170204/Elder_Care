# T050: đã chọn SQL chỉ đọc M09, rút yêu cầu bổ sung API A

**Quyết định thay thế ngày 2026-11-05:** người dùng đồng ý xử lý trong phần E theo quy ước R-15 ở team-plan mục 8: SQL chỉ đọc dòng có loại mang tiền tố module mình trong persistence, nội dung vẫn qua API có sẵn. Không còn yêu cầu A bổ sung scanBefore để giải quyết T050. API/interface, bản thật và fake của A giữ nguyên. Các mục bên dưới lưu phương án đề xuất ngày 31/10, không còn là blocker hiện hành.

M09Retention hiện chọn ID từ record_event bằng module='M09', kind M09.STAFFING_RATIO_CALCULATION hoặc M09.SHIFT_PUBLICATION, đúng entity_type, occurred_at < cutoff; dùng NOT EXISTS để loại ca còn hoạt động trước ORDER BY occurred_at,id và LIMIT batch. Không duyệt từ danh sách cha đang tồn tại nên không bỏ sót bản ghi mồ côi. Nội dung/ID logic legacy đọc bằng RecordEventApi.require hiện có. Lệnh xóa vẫn dùng kết nối RetentionDatabase và đúng loại/ID vật lý như trước.

Đã bổ sung 9 ca kiểm thử trong StaffingRatioSchemaIT: hai trường hợp ID cũ/mới, sáu trạng thái ca với batch=1 và loại sự kiện khác, một bản công bố không còn lịch cha; giữ test mồ côi cũ và biên cutoff. Sau khi người dùng cho phép kiểm thử, **117 unit/arch + 333 IT của staffing và consumer/hợp đồng liên quan xanh, 0 failure/error/skip**, gồm migration, schema/trigger và ApiContractIT. T050 đã [x]; không chạy lifecycle verify toàn kho và không công bố toàn spec xong.

## Phương án API cũ — không còn yêu cầu triển khai

Ngày 2026-10-31. Người dùng chọn “Chờ A bổ sung API”. E không sửa API, bản cài hoặc bản giả của A; chỉ tiếp tục phần độc lập và kiểm lại sau khi API có trên develop.

## 1. API đang chờ A bổ sung

Bổ sung vào administration.shared.api.RecordEventApi:

    <T> List<RecordEvent<T>> scanBefore(
        SharedKind<T> kind, Instant cutoff, long afterId, int limit);

Chỉ đọc; giữ nguyên các hàm/chữ ký/DTO hiện có. Trả đúng loại kind, occurredAt < cutoff, id > afterId, theo id tăng dần, tối đa limit dòng. cutoff bắt buộc, afterId >= 0, limit > 0; trang đầu afterId=0, trang sau dùng id cuối trang trước. Caller tự kiểm quyền nghiệp vụ; API không thêm endpoint HTTP.

Phạm vi A cần xem xét/cung cấp: RecordEventApi, lớp cài thật/Store của shared, FakeRecordEventApi hiện có, bộ hợp đồng test chạy cả thật và fake, module-interfaces 000 mục 1.8 và mục glossary của API dùng chung; chỉ mục hỗ trợ trong V008_94__shared_staffing_legacy_ids.sql do A review. Không tạo bản giả mới hoặc đổi hàm cũ.

## 2. Vì sao API hiện có chưa đủ

- RecordEventApi chỉ có list(kind, parent), require(id), findByUniqueKey. Không có cách liệt kê những sự kiện không còn cha.
- StaffingRatioStore/FR-037 cố ý giữ lịch sử tỷ lệ khi actor xóa ca Nháp; V008_07 không đặt FK từ staffing_ratio_calculation tới work_shift. Sau gộp, dòng này nằm ở record_event với entity_type=M09.SHIFT và id ca đã xóa.
- M09Retention hiện có provider M09.STAFFING_RATIO_CALCULATION: phải tìm cả các dòng mồ côi đã hết hạn. Duyệt id ca đang còn rồi gọi list(kind,parent) bỏ sót các dòng đó.
- SharedRowRetention chỉ xóa kèm khi administration.retention xóa bản ghi cha. Ca Nháp đã bị người dùng xóa từ trước không còn để xuất hiện trong chuỗi xóa; helper này không thay thế được bước tìm ứng viên mồ côi.
- Không thể đoán khoảng ID để gọi require hoặc giữ thêm một bảng chỉ mục riêng trái R-15. Nhận định cũ cấm mọi SQL trực tiếp đã được thay thế: quy ước R-15 hiện cho phép SQL chỉ đọc các dòng M09 trong persistence, vì vậy chọn phương án đó ngày 05/11/2026.

## 3. Kiểm thử cần có

1. Hợp đồng chạy trên cả bean thật và FakeRecordEventApi chung.
2. Có hai loại sự kiện và parent khác nhau, gồm parent đã xóa; không lọt loại khác.
3. Hai trang có id thưa; không trùng/bỏ dòng, giới hạn đúng số lượng.
4. Mốc occurredAt đúng cutoff bị loại; thứ tự kết quả theo id dù occurredAt không cùng thứ tự.
5. Chặn cutoff null, afterId âm và limit <= 0.
6. M09Retention tìm/xóa được tỷ lệ mồ côi đủ hạn, giữ tỷ lệ còn mới và ca còn hoạt động.

## 4. Phần E vẫn làm độc lập

Bàn giao JSON, phân công Trưởng tầng, danh mục/import, lịch sử và báo cáo theo các ca đang có, job/marker/log, chuyển fixture và test nhóm dữ liệu. Chỉ phần lưu giữ/quét sự kiện nói trên cần API bổ sung; T050 chưa được đánh hoàn tất trước khi kiểm được phần này.
