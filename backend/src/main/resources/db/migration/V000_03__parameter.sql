-- 000 T029: tham số cấu hình (data-model mục 2, research R-07; FR-011 → FR-022, DBR-24).
-- parameter: nhóm 1, mỗi mã đúng một dòng nên đúng một giá trị hiện hành; mã cố định, không thêm, không xóa qua ứng dụng.
-- parameter_history: nhóm 3, chỉ thêm; giá trị tại thời điểm t là dòng có effective_from lớn nhất mà <= t (FR-019).
CREATE TABLE parameter (
    code          VARCHAR(16)  NOT NULL PRIMARY KEY,
    description   VARCHAR(500) NOT NULL,
    kind          ENUM ('DURATION', 'INTEGER', 'PERCENTAGE', 'MONEY', 'TIME_OF_DAY', 'TABLE') NOT NULL,
    unit          VARCHAR(16)  NULL,
    constraints   JSON         NULL,
    default_value JSON         NOT NULL,
    current_value JSON         NOT NULL,
    used_in       VARCHAR(500) NULL,
    version       INT          NOT NULL DEFAULT 0,
    changed_at    DATETIME(6)  NULL,
    changed_by    BIGINT       NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE parameter_history (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code               VARCHAR(16)   NOT NULL,
    value              JSON          NOT NULL,
    effective_from     DATETIME(6)   NOT NULL,
    actor_id           BIGINT        NULL COMMENT 'NULL: Bộ lập lịch ở lần nạp ban đầu',
    reason             VARCHAR(1000) NULL,
    is_default_restore BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_parameter_history UNIQUE (code, effective_from),
    CONSTRAINT fk_parameter_history_code FOREIGN KEY (code) REFERENCES parameter (code) ON DELETE RESTRICT,
    CONSTRAINT chk_parameter_history_reason CHECK (actor_id IS NULL OR CHAR_LENGTH(TRIM(reason)) > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Mẫu trigger nhóm 3 (data-model mục 1.4); dùng USER() vì trong trigger CURRENT_USER() là tài khoản tạo trigger.
CREATE TRIGGER trg_parameter_history_no_update
    BEFORE UPDATE
    ON parameter_history
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'parameter_history is append-only (DBR-24)';

DELIMITER $$
CREATE TRIGGER trg_parameter_history_no_delete
    BEFORE DELETE
    ON parameter_history
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'parameter_history rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

-- Ứng dụng chỉ đổi giá trị (UPDATE), không thêm hay xóa mã (FR-011); lịch sử chỉ thêm.
GRANT UPDATE ON parameter TO 'eldercare_app'@'%';
GRANT INSERT ON parameter_history TO 'eldercare_app'@'%';
GRANT DELETE ON parameter_history TO 'eldercare_retention'@'%';
