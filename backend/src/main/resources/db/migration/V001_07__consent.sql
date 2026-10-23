-- Bản đồng ý chia sẻ dữ liệu (001 US5; FR-010 → FR-016, BR-M01-08, DBR-03; data-model mục 4, 8). Nhóm 2: nội dung không
-- sửa, không xóa; chỉ đổi theo chiều ACTIVE → WITHDRAWN (rút lại hoặc được thay thế). Chỉ dùng cú pháp có ở MySQL 8.0 và
-- 8.4 (research R-01 của 000).
CREATE TABLE consent
(
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    resident_id                BIGINT       NOT NULL,
    consenter_type             ENUM ('RESIDENT','LEGAL_REPRESENTATIVE') NOT NULL,
    consenter_family_member_id BIGINT       NULL COMMENT 'Người đại diện theo pháp luật ký thay (FR-011); hồ sơ người thân do 012 giữ nên không có khóa ngoại',
    court_decision_number      VARCHAR(100) NULL,
    court_decision_date        DATE         NULL,
    court_decision_court       VARCHAR(200) NULL,
    court_decision_file_id     BIGINT       NULL,
    photo_consent              BOOLEAN      NOT NULL,
    notification_consent       BOOLEAN      NOT NULL COMMENT 'Thông báo không khẩn về người cao tuổi (Q-353)',
    consented_at               DATETIME(6)  NOT NULL,
    evidence_file_id           BIGINT       NOT NULL COMMENT 'Bản ký đã scan (FR-010)',
    status                     ENUM ('ACTIVE','WITHDRAWN') NOT NULL,
    active_resident_id         BIGINT GENERATED ALWAYS AS (IF(status = 'ACTIVE', resident_id, NULL)) STORED COMMENT 'UNIQUE: tối đa một bản Hiệu lực (FR-013)',
    withdrawn_at               DATETIME(6)  NULL,
    withdrawn_by               BIGINT       NULL,
    withdrawal_reason          VARCHAR(2000) NULL COMMENT 'NULL khi được thay thế',
    withdrawal_evidence_file_id BIGINT      NULL COMMENT 'NULL khi được thay thế',
    superseded_by_id           BIGINT       NULL,
    created_at                 DATETIME(6)  NOT NULL,
    created_by                 BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_consent_active (active_resident_id),
    KEY ix_consent_resident (resident_id, id),
    CONSTRAINT fk_consent_resident FOREIGN KEY (resident_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT fk_consent_evidence FOREIGN KEY (evidence_file_id) REFERENCES file (id) ON DELETE RESTRICT,
    CONSTRAINT fk_consent_court_file FOREIGN KEY (court_decision_file_id) REFERENCES file (id) ON DELETE RESTRICT,
    CONSTRAINT fk_consent_withdrawal_file FOREIGN KEY (withdrawal_evidence_file_id) REFERENCES file (id) ON DELETE RESTRICT,
    CONSTRAINT fk_consent_superseded_by FOREIGN KEY (superseded_by_id) REFERENCES consent (id) ON DELETE RESTRICT,
    -- Q-348: người ký thay có đủ bốn thông tin quyết định của Tòa án; người cao tuổi tự ký thì không có cột nào
    CONSTRAINT ck_consent_signer CHECK (
        (consenter_type = 'RESIDENT'
            AND consenter_family_member_id IS NULL
            AND court_decision_number IS NULL AND court_decision_date IS NULL
            AND court_decision_court IS NULL AND court_decision_file_id IS NULL)
            OR (consenter_type = 'LEGAL_REPRESENTATIVE'
            AND consenter_family_member_id IS NOT NULL
            AND court_decision_number IS NOT NULL AND court_decision_date IS NOT NULL
            AND court_decision_court IS NOT NULL AND court_decision_file_id IS NOT NULL)),
    -- FR-014: rút lại bằng lệnh có cả lý do và bằng chứng; được thay thế thì không có cả hai
    CONSTRAINT ck_consent_withdrawal CHECK (
        (status = 'ACTIVE'
            AND withdrawn_at IS NULL AND withdrawn_by IS NULL AND withdrawal_reason IS NULL
            AND withdrawal_evidence_file_id IS NULL AND superseded_by_id IS NULL)
            OR (status = 'WITHDRAWN' AND withdrawn_at IS NOT NULL AND withdrawn_by IS NOT NULL
            AND ((withdrawal_reason IS NULL) = (withdrawal_evidence_file_id IS NULL))
            AND (superseded_by_id IS NULL OR withdrawal_reason IS NULL)))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Phạm vi người thân được xem sức khỏe; ghi cùng lệnh với bản đồng ý, đổi phạm vi là ghi bản mới (US5 #6).
CREATE TABLE consent_family_member
(
    consent_id       BIGINT NOT NULL,
    family_member_id BIGINT NOT NULL COMMENT 'Hồ sơ người thân do 012 giữ nên không có khóa ngoại',
    PRIMARY KEY (consent_id, family_member_id),
    KEY ix_consent_family_member (family_member_id),
    CONSTRAINT fk_consent_family_member_consent FOREIGN KEY (consent_id) REFERENCES consent (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Bảng kỹ thuật của các job nhắc theo chu kỳ (data-model mục 8): thêm dòng khi điều kiện nhắc bắt đầu; job cập nhật
-- lần nhắc cuối. Không khóa ngoại tới resident (như return_overdue_alert): dòng không chứa dữ liệu cá nhân, không chặn
-- job loại bỏ hồ sơ hết hạn.
CREATE TABLE periodic_reminder
(
    type             ENUM ('MISSING_CONSENT','MISSING_END_OF_LIFE_WISH','CREATE_CARE_LEVEL_CHANGE_REQUEST',
        'REVIEW_SELF_DETERMINATION') NOT NULL,
    resident_id      BIGINT      NOT NULL,
    milestone_id     BIGINT      NOT NULL DEFAULT 0 COMMENT 'Lần đánh giá lại hoặc phiếu gây ra việc nhắc; 0 với loại không có mốc',
    started_at       DATETIME(6) NOT NULL,
    last_reminded_at DATETIME(6) NULL,
    reminder_count   INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (type, resident_id, milestone_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
CREATE TRIGGER trg_consent_only_withdrawal
    BEFORE UPDATE
    ON consent
    FOR EACH ROW
BEGIN
    IF NOT (NEW.resident_id <=> OLD.resident_id
        AND NEW.consenter_type <=> OLD.consenter_type
        AND NEW.consenter_family_member_id <=> OLD.consenter_family_member_id
        AND NEW.court_decision_number <=> OLD.court_decision_number
        AND NEW.court_decision_date <=> OLD.court_decision_date
        AND NEW.court_decision_court <=> OLD.court_decision_court
        AND NEW.court_decision_file_id <=> OLD.court_decision_file_id
        AND NEW.photo_consent <=> OLD.photo_consent
        AND NEW.notification_consent <=> OLD.notification_consent
        AND NEW.consented_at <=> OLD.consented_at
        AND NEW.evidence_file_id <=> OLD.evidence_file_id
        AND NEW.created_at <=> OLD.created_at
        AND NEW.created_by <=> OLD.created_by) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'consent content is immutable (FR-016)';
    END IF;
    IF OLD.status = 'WITHDRAWN' THEN
        -- Bản được thay thế nhận tham chiếu tới bản mới sau khi bản mới được thêm (đúng một lần, từ NULL)
        IF NOT (NEW.status <=> OLD.status
            AND NEW.withdrawn_at <=> OLD.withdrawn_at
            AND NEW.withdrawn_by <=> OLD.withdrawn_by
            AND NEW.withdrawal_reason <=> OLD.withdrawal_reason
            AND NEW.withdrawal_evidence_file_id <=> OLD.withdrawal_evidence_file_id
            AND (NEW.superseded_by_id <=> OLD.superseded_by_id
                OR (OLD.superseded_by_id IS NULL AND OLD.withdrawal_reason IS NULL))) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'consent is already withdrawn and cannot change (FR-014)';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_consent_no_delete
    BEFORE DELETE
    ON consent
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'consent rows are removed only by the retention job (FR-051)';
    END IF;
END$$

CREATE TRIGGER trg_consent_family_member_no_delete
    BEFORE DELETE
    ON consent_family_member
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'consent_family_member rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

CREATE TRIGGER trg_consent_family_member_no_update
    BEFORE UPDATE
    ON consent_family_member
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'consent_family_member is append-only (FR-016)';

GRANT INSERT ON consent TO 'eldercare_app'@'%';
GRANT UPDATE (status, withdrawn_at, withdrawn_by, withdrawal_reason, withdrawal_evidence_file_id, superseded_by_id)
    ON consent TO 'eldercare_app'@'%';
GRANT INSERT ON consent_family_member TO 'eldercare_app'@'%';
GRANT INSERT ON periodic_reminder TO 'eldercare_app'@'%';
GRANT UPDATE (last_reminded_at, reminder_count) ON periodic_reminder TO 'eldercare_app'@'%';
GRANT DELETE ON consent TO 'eldercare_retention'@'%';
GRANT DELETE ON consent_family_member TO 'eldercare_retention'@'%';
GRANT DELETE ON periodic_reminder TO 'eldercare_retention'@'%';
