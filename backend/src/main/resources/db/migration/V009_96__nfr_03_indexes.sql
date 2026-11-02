-- 009 T044 (NFR-03, NFR-04; research R-15 mục 4): đo lại trên dữ liệu một năm (1,1 triệu thông báo, 5,8 triệu dòng
-- record_event) sau gộp bảng T043, thêm cột sinh, chỉ mục cho truy vấn quá 1 giây. Cú pháp có ở MySQL 8.0, 8.4.

-- Gửi lại lỗi tạm (FR-018, job 20 giây): chỉ mục m13_result đọc mọi kết quả FAILED (cả lỗi vĩnh viễn) rồi lọc lý do
-- trong JSON. Cột chỉ có giá trị ở kết quả lỗi tạm.
ALTER TABLE record_event
    ADD COLUMN m13_temporary_failure BOOLEAN GENERATED ALWAYS AS (
        IF(kind = 'M13.DELIVERY_RESULT' AND JSON_VALUE(content, '$.result') = 'FAILED'
               AND JSON_VALUE(content, '$.reason') LIKE 'TEMPORARY%', TRUE, NULL)) STORED,
    ADD INDEX ix_record_event_m13_temporary_failure (m13_temporary_failure);

-- Hộp thông báo (Khẩn cấp chưa xác nhận lên đầu, rồi mới gửi trước): sắp theo cột để đọc chỉ mục ngược và dừng ở
-- LIMIT thay vì sắp mọi thông báo của người nhận; lọc chưa đọc có chỉ mục riêng (người đã đọc hết thì không phải quét
-- mọi thông báo). Lượt nhắc gọi (job 20 giây): Khẩn cấp chưa xác nhận theo sent_at. Danh sách liên hệ trực tiếp (job):
-- thông báo người thân theo created_at.
ALTER TABLE notification
    ADD COLUMN urgent_unacknowledged TINYINT GENERATED ALWAYS AS (
        IF(level = 'URGENT' AND acknowledged_at IS NULL, 1, 0)) STORED,
    ADD INDEX idx_notification_inbox_order (recipient_key, hidden, urgent_unacknowledged, sent_at),
    ADD INDEX idx_notification_inbox_unread (recipient_key, hidden, viewed_at, urgent_unacknowledged, sent_at),
    ADD INDEX idx_notification_urgent_due (level, acknowledged_at, sent_at),
    ADD INDEX idx_notification_recipient_created (recipient_type, created_at);
