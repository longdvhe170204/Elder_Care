-- 000 T026: nhật ký hệ thống, nhóm 3, chỉ ghi thêm (data-model mục 5, research R-06; FR-042 → FR-051a).
-- Phân vùng theo năm của occurred_at (UTC) nên khóa chính gồm occurred_at và bảng không có khóa ngoại.
-- Vận hành thêm phân vùng năm mới bằng REORGANIZE PARTITION p_max trước khi sang năm (000 T102).
CREATE TABLE audit_log (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    occurred_at       DATETIME(6)   NOT NULL,
    device_time       DATETIME(6)   NULL,
    actor_type        ENUM ('USER', 'SCHEDULER', 'BOOTSTRAP') NOT NULL,
    actor_id          BIGINT        NULL,
    role              VARCHAR(10)   NULL,
    action            VARCHAR(40)   NOT NULL,
    entity_type       VARCHAR(40)   NOT NULL,
    entity_id         VARCHAR(64)   NOT NULL,
    resident_id       BIGINT        NULL,
    data_before       JSON          NULL,
    data_after        JSON          NULL,
    reason            VARCHAR(2000) NULL,
    basis             JSON          NULL,
    details           JSON          NULL,
    exempt_from_purge BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id, occurred_at),
    KEY idx_audit_log_entity (entity_type, entity_id, occurred_at),
    KEY idx_audit_log_actor (actor_id, occurred_at),
    KEY idx_audit_log_action (action, occurred_at),
    KEY idx_audit_log_resident (resident_id),
    CONSTRAINT chk_audit_log_user_actor CHECK (actor_type <> 'USER' OR actor_id IS NOT NULL)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    PARTITION BY RANGE COLUMNS (occurred_at) (
        PARTITION p_2026 VALUES LESS THAN ('2027-01-01'),
        PARTITION p_2027 VALUES LESS THAN ('2028-01-01'),
        PARTITION p_2028 VALUES LESS THAN ('2029-01-01'),
        PARTITION p_2029 VALUES LESS THAN ('2030-01-01'),
        PARTITION p_2030 VALUES LESS THAN ('2031-01-01'),
        PARTITION p_max VALUES LESS THAN (MAXVALUE)
        );

-- Mẫu trigger nhóm 3 (data-model mục 1.4): nhật ký không có bản nháp nên chặn mọi UPDATE.
CREATE TRIGGER trg_audit_log_no_update
    BEFORE UPDATE
    ON audit_log
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log is append-only (FR-048)';

-- Chỉ job loại bỏ dữ liệu (eldercare_retention) được xóa; bản tóm tắt đợt loại bỏ không bao giờ bị xóa (FR-051).
-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
CREATE TRIGGER trg_audit_log_no_delete
    BEFORE DELETE
    ON audit_log
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' OR OLD.exempt_from_purge THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT ON audit_log TO 'eldercare_app'@'%';
GRANT DELETE ON audit_log TO 'eldercare_retention'@'%';
