-- 002 T105: mã đặt lại mật khẩu gửi qua tin nhắn (FR-052a, BR-M15-25, Q-399; CFG-M15-12 → 14). Bảng kỹ thuật, không
-- phải bản ghi nghiệp vụ: không dùng device_credential vì mã 6 chữ số lặp giữa các tài khoản (khóa duy nhất secret_hash).
-- Chỉ lưu SHA-256 của id tài khoản ghép mã. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).
CREATE TABLE password_reset_code
(
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT      NOT NULL,
    code_hash       BINARY(32)  NOT NULL COMMENT 'SHA-256 của id tài khoản và mã, không lưu mã',
    requested_at    DATETIME(6) NOT NULL,
    expires_at      DATETIME(6) NOT NULL COMMENT 'requested_at + CFG-M15-12 đọc lúc xin mã',
    failed_attempts INT         NOT NULL DEFAULT 0,
    used_at         DATETIME(6) NULL,
    revoked_at      DATETIME(6) NULL COMMENT 'Có mã mới, hoặc sai đủ CFG-M15-13 lần',
    CONSTRAINT fk_password_reset_code_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT chk_password_reset_code_expiry CHECK (expires_at > requested_at),
    CONSTRAINT chk_password_reset_code_attempts CHECK (failed_attempts >= 0),
    KEY idx_password_reset_code_account (account_id, requested_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Tài khoản ứng dụng chỉ thêm, sửa (SELECT toàn schema ở V000_00); không xóa dòng.
GRANT INSERT, UPDATE ON password_reset_code TO 'eldercare_app'@'%';
