-- 018 T002: khai báo tạm trú, lưu trú (data-model mục 2 → 5; FR-001 → FR-008, DBR-33). Tên theo glossary 24.5:
-- khai_bao_nct = registration_resident, khai_bao_tam_tru = residence_registration, khai_bao_lan_nop =
-- registration_submission, nghia_vu_khai_bao = registration_obligation, nhac_khai_bao = registration_reminder. Người cao
-- tuổi, hợp đồng, phụ lục, tệp là tham chiếu logic. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

-- Đích khóa theo người (mọi lệnh khai báo khóa dòng này trước) và cờ thường trú cùng xã/phường của hồ sơ (FR-001, Q-215):
-- khai báo sau của cùng hồ sơ dùng loại Thông báo lưu trú.
CREATE TABLE registration_resident
(
    resident_id              BIGINT        NOT NULL PRIMARY KEY,
    local_permanent_resident BOOLEAN       NOT NULL DEFAULT FALSE,
    basis_note               VARCHAR(1000) NULL,
    confirmed_by_id          BIGINT        NULL,
    confirmed_at             DATETIME(6)   NULL,
    version                  INT           NOT NULL DEFAULT 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nghĩa vụ khai báo (kỹ thuật, đệm số liệu tính; research Z-03): mỗi mốc phát sinh một dòng, hạn chốt lúc ghi mốc (mốc +
-- CFG-M02-12). Dựng lại được từ dữ liệu nguồn; không phải bản ghi nghiệp vụ, không có lệnh (Q-306). registration_id là
-- tham chiếu logic tới khai báo đáp ứng (khóa ngoại theo chiều ngược lại ở residence_registration.obligation_id).
CREATE TABLE registration_obligation
(
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id         BIGINT        NOT NULL,
    trigger_type        ENUM ('ADMISSION_COMPLETED', 'RESIDENTIAL_EFFECTIVE', 'RENEWAL_ADDENDUM', 'EXPIRED') NOT NULL,
    trigger_at          DATETIME(6)   NOT NULL,
    due_at              DATETIME(6)   NOT NULL,
    source_id           BIGINT        NOT NULL COMMENT 'Hợp đồng, phụ lục hoặc khai báo Hết hạn sinh ra mốc',
    registration_id     BIGINT        NULL,
    submitted           BOOLEAN       NOT NULL DEFAULT FALSE,
    manager_notified_at DATETIME(6)   NULL COMMENT 'Đã báo Quản lý viện quá hạn (một lần, FR-002)',
    cancelled_at        DATETIME(6)   NULL,
    CONSTRAINT uq_registration_obligation_trigger UNIQUE (resident_id, trigger_type, source_id),
    INDEX ix_registration_obligation_due (due_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Khai báo (nhóm 2; nội dung nộp, kết quả, xóa là nhóm 3: cột đặt một lần, sửa sai qua đính chính M02.RESIDENCE_REGISTRATION).
-- DBR-33: mỗi người tối đa một khai báo đang xử lý (Cần khai báo, Đã nộp) và một khai báo Đã xác nhận, bằng hai cột sinh
-- UNIQUE (NULL không trùng nhau). Khai báo mới Đã xác nhận: khai báo cũ chuyển Được thay thế trước, cùng transaction.
CREATE TABLE residence_registration
(
    id                              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id                     BIGINT        NOT NULL,
    type                            ENUM ('TEMPORARY_RESIDENCE_REGISTRATION', 'STAY_NOTIFICATION') NOT NULL,
    renewal                         BOOLEAN       NOT NULL DEFAULT FALSE,
    obligation_id                   BIGINT        NULL,
    due_at                          DATETIME(6)   NOT NULL,
    local_permanent_resident        BOOLEAN       NOT NULL DEFAULT FALSE,
    basis_note                      VARCHAR(1000) NULL,
    status                          ENUM ('TO_SUBMIT', 'SUBMITTED', 'CONFIRMED', 'EXPIRED', 'DEREGISTERED', 'SUPERSEDED',
        'CANCELLED')                              NOT NULL DEFAULT 'TO_SUBMIT',
    current_submission_id           BIGINT        NULL COMMENT 'Lần nộp hiện hành (tham chiếu logic, tránh khóa ngoại vòng)',
    expiry_date                     DATE          NULL,
    evidence_file_id                BIGINT        NULL,
    deregistered_at                 DATETIME(6)   NULL,
    deregistered_by_id              BIGINT        NULL,
    deregistration_evidence_file_id BIGINT        NULL,
    replaced_registration_id        BIGINT        NULL COMMENT 'Khai báo cũ bị khai báo này thay (FR-005)',
    in_progress_key                 BIGINT GENERATED ALWAYS AS (IF(status IN ('TO_SUBMIT', 'SUBMITTED'), resident_id, NULL)) STORED,
    confirmed_key                   BIGINT GENERATED ALWAYS AS (IF(status = 'CONFIRMED', resident_id, NULL)) STORED,
    CONSTRAINT uq_residence_registration_in_progress UNIQUE (in_progress_key),
    CONSTRAINT uq_residence_registration_confirmed UNIQUE (confirmed_key),
    CONSTRAINT fk_residence_registration_obligation FOREIGN KEY (obligation_id)
        REFERENCES registration_obligation (id) ON DELETE RESTRICT,
    CONSTRAINT fk_residence_registration_replaced FOREIGN KEY (replaced_registration_id)
        REFERENCES residence_registration (id) ON DELETE RESTRICT,
    -- Đăng ký tạm trú được chấp nhận phải có ngày hết hạn (FR-003); mọi trạng thái sau chấp nhận giữ ngày đó.
    CONSTRAINT ck_residence_registration_expiry CHECK (type <> 'TEMPORARY_RESIDENCE_REGISTRATION'
        OR status IN ('TO_SUBMIT', 'SUBMITTED', 'CANCELLED') OR expiry_date IS NOT NULL),
    CONSTRAINT ck_residence_registration_deregistered CHECK (status <> 'DEREGISTERED'
        OR (deregistered_at IS NOT NULL AND deregistered_by_id IS NOT NULL
            AND deregistration_evidence_file_id IS NOT NULL)),
    INDEX ix_residence_registration_resident (resident_id),
    INDEX ix_residence_registration_status_expiry (status, expiry_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Lần nộp (nhóm 3, FR-003): chỉ thêm; kết quả đặt một lần từ PENDING sang ACCEPTED hoặc REJECTED (từ chối có lý do).
CREATE TABLE registration_submission
(
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    registration_id     BIGINT        NOT NULL,
    submitted_date      DATE          NOT NULL,
    channel             ENUM ('PUBLIC_SERVICE_PORTAL', 'IN_PERSON') NOT NULL,
    receiving_authority VARCHAR(255)  NOT NULL,
    dossier_code        VARCHAR(100)  NULL,
    result              ENUM ('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    rejection_reason    VARCHAR(1000) NULL,
    recorded_by         BIGINT        NOT NULL,
    recorded_at         DATETIME(6)   NOT NULL,
    CONSTRAINT fk_registration_submission_registration FOREIGN KEY (registration_id)
        REFERENCES residence_registration (id) ON DELETE RESTRICT,
    CONSTRAINT ck_registration_submission_reason CHECK ((result = 'REJECTED') = (rejection_reason IS NOT NULL))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Chống nhắc trùng (kỹ thuật; research Z-06): mỗi (loại nhắc, người, ngày) một lần, kể cả khi job chạy lại, chạy song song.
CREATE TABLE registration_reminder
(
    type        ENUM ('CREATE', 'OVERDUE', 'RENEWAL', 'DEREGISTER') NOT NULL,
    resident_id BIGINT NOT NULL,
    `date`      DATE   NOT NULL,
    PRIMARY KEY (type, resident_id, `date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$

-- Chuyển trạng thái theo data-model mục 3; cột đặt một lần (ngày hết hạn, khai báo xóa) không đổi sau khi đã đặt.
CREATE TRIGGER trg_residence_registration_transition
    BEFORE UPDATE
    ON residence_registration
    FOR EACH ROW
BEGIN
    IF NOT (OLD.resident_id <=> NEW.resident_id AND OLD.renewal <=> NEW.renewal
        AND OLD.obligation_id <=> NEW.obligation_id
        AND OLD.replaced_registration_id <=> NEW.replaced_registration_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'residence_registration identity columns cannot change';
    END IF;
    IF OLD.expiry_date IS NOT NULL AND NOT (OLD.expiry_date <=> NEW.expiry_date) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'residence_registration expiry_date is set once, use a correction';
    END IF;
    IF OLD.deregistered_at IS NOT NULL AND NOT (OLD.deregistered_at <=> NEW.deregistered_at
        AND OLD.deregistered_by_id <=> NEW.deregistered_by_id
        AND OLD.deregistration_evidence_file_id <=> NEW.deregistration_evidence_file_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'residence_registration deregistration is set once';
    END IF;
    IF OLD.status IN ('DEREGISTERED', 'SUPERSEDED', 'CANCELLED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a deregistered, superseded or cancelled registration cannot change';
    END IF;
    IF NOT ((OLD.status = NEW.status)
        OR (OLD.status = 'TO_SUBMIT' AND NEW.status IN ('SUBMITTED', 'CANCELLED'))
        OR (OLD.status = 'SUBMITTED' AND NEW.status IN ('CONFIRMED', 'TO_SUBMIT', 'CANCELLED'))
        OR (OLD.status = 'CONFIRMED' AND NEW.status IN ('EXPIRED', 'DEREGISTERED', 'SUPERSEDED'))
        OR (OLD.status = 'EXPIRED' AND NEW.status = 'DEREGISTERED')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'residence_registration status transition not allowed (FR-005)';
    END IF;
    IF OLD.status <> 'TO_SUBMIT' AND NOT (OLD.type <=> NEW.type AND OLD.local_permanent_resident <=> NEW.local_permanent_resident
        AND OLD.basis_note <=> NEW.basis_note) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'registration type changes only while TO_SUBMIT (FR-001)';
    END IF;
END$$

CREATE TRIGGER trg_residence_registration_no_delete
    BEFORE DELETE
    ON residence_registration
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'residence_registration rows are removed only by the retention job';
    END IF;
END$$

CREATE TRIGGER trg_registration_submission_set_once
    BEFORE UPDATE
    ON registration_submission
    FOR EACH ROW
BEGIN
    IF NOT (OLD.registration_id <=> NEW.registration_id AND OLD.submitted_date <=> NEW.submitted_date
        AND OLD.channel <=> NEW.channel AND OLD.receiving_authority <=> NEW.receiving_authority
        AND OLD.dossier_code <=> NEW.dossier_code AND OLD.recorded_by <=> NEW.recorded_by
        AND OLD.recorded_at <=> NEW.recorded_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'registration_submission rows are append-only, use a correction (FR-003)';
    END IF;
    IF OLD.result <> 'PENDING' OR NEW.result = 'PENDING' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'registration_submission result is set once (FR-003)';
    END IF;
END$$

CREATE TRIGGER trg_registration_submission_no_delete
    BEFORE DELETE
    ON registration_submission
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'registration_submission rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON registration_resident TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON registration_obligation TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON residence_registration TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON registration_submission TO 'eldercare_app'@'%';
GRANT INSERT ON registration_reminder TO 'eldercare_app'@'%';
GRANT DELETE ON registration_resident TO 'eldercare_retention'@'%';
GRANT DELETE ON registration_obligation TO 'eldercare_retention'@'%';
GRANT DELETE ON residence_registration TO 'eldercare_retention'@'%';
GRANT DELETE ON registration_submission TO 'eldercare_retention'@'%';
GRANT DELETE ON registration_reminder TO 'eldercare_retention'@'%';
