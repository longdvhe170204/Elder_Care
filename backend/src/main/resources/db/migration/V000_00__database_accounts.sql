-- 000 T024: hai tài khoản của ứng dụng (research R-05). Flyway chạy bằng tài khoản quản trị (DB_MIGRATION_*).
-- Mật khẩu là placeholder lấy từ biến môi trường, không ghi vào kho mã; tài khoản đã có thì giữ nguyên (lớp cơ sở
-- test tạo sẵn hai tài khoản trước khi Flyway chạy).
-- Mức schema chỉ cấp SELECT. INSERT, UPDATE, DELETE cấp theo từng bảng trong migration của bảng đó: eldercare_app
-- không có DELETE trên bảng nhóm 3, chỉ SELECT, INSERT trên audit_log, correction, parameter_history;
-- eldercare_retention có DELETE trên bảng mà job loại bỏ dữ liệu xử lý.
CREATE USER IF NOT EXISTS 'eldercare_app'@'%' IDENTIFIED BY '${appPassword}';
CREATE USER IF NOT EXISTS 'eldercare_retention'@'%' IDENTIFIED BY '${retentionPassword}';

GRANT SELECT ON `${flyway:database}`.* TO 'eldercare_app'@'%', 'eldercare_retention'@'%';
