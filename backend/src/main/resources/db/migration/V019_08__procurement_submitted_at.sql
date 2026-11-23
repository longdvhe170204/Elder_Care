-- 019, sửa BF-07 (2026-11-22, Q-434, BR-M11-22): mốc gửi duyệt để báo Quản lý viện khi yêu cầu mua thuốc mức Gấp chờ duyệt
-- quá CFG-M11-12 (24 giờ). Cú pháp MySQL 8.0 và 8.4.
ALTER TABLE procurement_request
    ADD COLUMN submitted_at DATETIME(6) NULL COMMENT 'Mốc gửi duyệt gần nhất (CFG-M11-12)';
