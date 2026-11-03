-- Nới SERIALIZED_EVENT của Event Publication Registry (V000_05) từ VARCHAR(4000) sang TEXT: sự kiện có lý do 2000 ký tự
-- (khuôn Reason chung) sau khi escape JSON vượt 4000 ký tự, ví dụ CarePlanReturned của 005 (plan 005, ca biên
-- FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage). Cột không có chỉ mục; truy vấn của spring-modulith-events-jdbc
-- chỉ đọc, ghi nguyên giá trị.
ALTER TABLE EVENT_PUBLICATION MODIFY COLUMN SERIALIZED_EVENT TEXT NOT NULL;
