-- Gộp bảng về 79 (research R-15 của 000, chủ dự án yêu cầu 2026-11-13): mã đặt lại mật khẩu (002 T105) thành dòng
-- simple_record loại M15.PASSWORD_RESET_CODE, cha là tài khoản, created_at là lúc xin mã; trạng thái ACTIVE, USED,
-- REVOKED; băm (hex), hạn và số lần sai ở content. Cú pháp dùng được ở MySQL 8.0 và 8.4.
INSERT INTO simple_record (kind, module, entity_type, entity_id, status, content, created_at, created_by)
SELECT 'M15.PASSWORD_RESET_CODE',
       'M15',
       'M15.ACCOUNT',
       account_id,
       CASE WHEN used_at IS NOT NULL THEN 'USED' WHEN revoked_at IS NOT NULL THEN 'REVOKED' ELSE 'ACTIVE' END,
       JSON_OBJECT('codeHash', LOWER(HEX(code_hash)),
                   'expiresAt', DATE_FORMAT(expires_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'failedAttempts', failed_attempts,
                   'legacy', JSON_OBJECT('table', 'password_reset_code', 'id', id)),
       requested_at,
       account_id
FROM password_reset_code
ORDER BY id;

REVOKE INSERT, UPDATE ON password_reset_code FROM 'eldercare_app'@'%';
DROP TABLE password_reset_code;
