-- 004 audit (2026-10-27; FR-001, FR-074, FR-075): Hoàn tất tiếp nhận hợp lệ không cần lượt đăng ký mở, nên ngày vào ở được
-- ghi theo người (resident_residency) thay vì theo lượt đăng ký. admission_application.admitted_on (V004_10) chỉ còn là
-- giá trị dự phòng khi đọc. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).
ALTER TABLE resident_residency
    ADD COLUMN admitted_on DATE NULL COMMENT 'Ngày Hoàn tất tiếp nhận; đặt một lần';
