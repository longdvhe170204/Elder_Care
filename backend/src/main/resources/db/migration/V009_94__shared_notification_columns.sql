-- 009 T043 (research R-15 mục 4, 5): cột sinh có tiền tố module trên bảng chung record_event cho truy vấn của M13 (A
-- review). Lần gửi dồn vào khung gộp tin (FR-016b) và kết quả lần gửi (gửi lại lỗi tạm FR-018, danh sách lỗi gửi).
-- Thời điểm dự kiến của lần gửi là occurred_at của dòng nên không cần cột sinh. Cú pháp có ở MySQL 8.0, 8.4.
ALTER TABLE record_event
    ADD COLUMN m13_batch_window_id BIGINT GENERATED ALWAYS AS (
        IF(kind = 'M13.DELIVERY_ATTEMPT', JSON_VALUE(content, '$.batchWindowId' RETURNING SIGNED), NULL)) STORED,
    ADD COLUMN m13_result          VARCHAR(30) GENERATED ALWAYS AS (
        IF(kind = 'M13.DELIVERY_RESULT', JSON_VALUE(content, '$.result'), NULL)) STORED,
    ADD INDEX ix_record_event_m13_batch_window (m13_batch_window_id),
    ADD INDEX ix_record_event_m13_result (m13_result);
