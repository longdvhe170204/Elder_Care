-- 000 T019: khóa chống lặp lệnh (Idempotency-Key), bảng kỹ thuật, không thuộc ba nhóm dữ liệu (data-model mục 6).
-- Người gửi lưu theo tên đăng nhập (plan 000, "Ghi chú triển khai"). Giữ 7 ngày (thông số kỹ thuật).
CREATE TABLE idempotency_key (
    key_value    VARCHAR(100) NOT NULL,
    username     VARCHAR(100) NOT NULL,
    request_hash CHAR(64)     NOT NULL,
    result_json  JSON         NULL,
    expires_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (key_value, username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

GRANT INSERT, UPDATE, DELETE ON idempotency_key TO 'eldercare_app'@'%';
