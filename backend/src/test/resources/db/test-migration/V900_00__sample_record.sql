-- Bảng chỉ có ở profile test, cho AppendOnlyRepositoryIT. Không cấp DELETE cho eldercare_app (R-05).
CREATE TABLE sample_record (
    id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    note VARCHAR(200) NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

GRANT INSERT ON sample_record TO 'eldercare_app'@'%';
