-- 002 T004: tài khoản và lịch sử trạng thái (data-model mục 2; FR-001 → FR-015).
-- account: trạng thái chỉ đổi qua lệnh (khóa lạc quan version); username, type, staff_id, family_member_id không đổi sau
-- khi tạo (kiểm ở backend). Không có trạng thái kết thúc và không xóa (FR-005): không cấp DELETE, trigger chặn xóa.
-- Loại RESIDENT, cột resident_id và các nguyên nhân khóa của người cao tuổi thêm ở V002_08 (T090).
CREATE TABLE account
(
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username                VARCHAR(64)  NOT NULL,
    type                    ENUM ('STAFF', 'FAMILY_MEMBER') NOT NULL,
    staff_id                BIGINT       NULL COMMENT 'Tham chiếu logic hồ sơ nhân viên (008)',
    family_member_id        BIGINT       NULL COMMENT 'Tham chiếu logic hồ sơ người thân (012)',
    status                  ENUM ('ACTIVE', 'TEMPORARILY_LOCKED', 'INACTIVE', 'LOCKED') NOT NULL,
    lock_cause              ENUM ('MANUAL', 'RESIGNATION', 'EXPIRED_AFTER_RESIDENCY_END', 'RELATIONSHIP_ENDED') NULL,
    locked_until            DATETIME(6)  NULL,
    failed_login_count      INT          NOT NULL DEFAULT 0,
    password_hash           VARCHAR(255) NOT NULL COMMENT 'Argon2id (research A-03)',
    must_change_password    BOOLEAN      NOT NULL,
    last_login_at           DATETIME(6)  NULL,
    inactivity_reference_at DATETIME(6)  NOT NULL COMMENT 'Mốc tính Không hoạt động (FR-010)',
    version                 INT          NOT NULL DEFAULT 0,
    permission_version      INT          NOT NULL DEFAULT 0,
    created_at              DATETIME(6)  NOT NULL,
    created_by              BIGINT       NULL COMMENT 'NULL: khởi tạo hệ thống (BR-M15-22)',
    CONSTRAINT uq_account_username UNIQUE (username),
    CONSTRAINT uq_account_staff UNIQUE (staff_id),
    CONSTRAINT uq_account_family_member UNIQUE (family_member_id),
    CONSTRAINT chk_account_link CHECK (
        (type = 'STAFF' AND staff_id IS NOT NULL AND family_member_id IS NULL)
            OR (type = 'FAMILY_MEMBER' AND family_member_id IS NOT NULL AND staff_id IS NULL)),
    CONSTRAINT chk_account_lock_cause CHECK ((status = 'LOCKED') = (lock_cause IS NOT NULL)),
    CONSTRAINT chk_account_locked_until CHECK ((status = 'TEMPORARILY_LOCKED') = (locked_until IS NOT NULL)),
    CONSTRAINT chk_account_failed_login_count CHECK (failed_login_count >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TRIGGER trg_account_no_delete
    BEFORE DELETE
    ON account
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'account rows are never deleted (FR-005)';

-- account_status_history: nhóm 3, chỉ thêm; trạng thái tại t là dòng có effective_from lớn nhất mà <= t (FR-041a).
CREATE TABLE account_status_history
(
    id             BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id     BIGINT      NOT NULL,
    status         ENUM ('ACTIVE', 'TEMPORARILY_LOCKED', 'INACTIVE', 'LOCKED') NOT NULL,
    lock_cause     ENUM ('MANUAL', 'RESIGNATION', 'EXPIRED_AFTER_RESIDENCY_END', 'RELATIONSHIP_ENDED') NULL,
    effective_from DATETIME(6) NOT NULL,
    actor_id       BIGINT      NULL COMMENT 'NULL: Bộ lập lịch hoặc khởi tạo hệ thống',
    audit_log_id   BIGINT      NULL COMMENT 'audit_log phân vùng nên không có khóa ngoại',
    CONSTRAINT uq_account_status_history UNIQUE (account_id, effective_from),
    CONSTRAINT fk_account_status_history_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Mẫu trigger nhóm 3 (000 data-model mục 1.4).
CREATE TRIGGER trg_account_status_history_no_update
    BEFORE UPDATE
    ON account_status_history
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'account_status_history is append-only (FR-015)';

DELIMITER $$
CREATE TRIGGER trg_account_status_history_no_delete
    BEFORE DELETE
    ON account_status_history
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'account_status_history rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON account TO 'eldercare_app'@'%';
GRANT INSERT ON account_status_history TO 'eldercare_app'@'%';
