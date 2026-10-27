-- 004 Phase 12 (T069): hai ngày để dựng dữ liệu ngày lưu trú cho 010 (FR-052, FR-074, FR-075a). admitted_on là ngày Hoàn
-- tất tiếp nhận (các ngày từ ngày bắt đầu hợp đồng tới trước ngày này mang dấu "chưa vào ở"); terminated_on là ngày hợp
-- đồng chuyển Chấm dứt (Hủy tiếp nhận, Kết thúc lưu trú sớm, Qua đời), mốc dừng tính phí. Dòng cũ để NULL. Cú pháp dùng
-- được ở MySQL 8.0 và 8.4 (research R-01 của 000).
ALTER TABLE admission_application
    ADD COLUMN admitted_on DATE NULL;

ALTER TABLE contract
    ADD COLUMN terminated_on DATE NULL;
