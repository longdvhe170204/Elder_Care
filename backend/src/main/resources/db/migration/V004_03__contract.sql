-- 004 T032, T044, T110: hợp đồng và phiên bản hợp đồng (data-model mục 0, 5, 7; FR-019 → FR-029, FR-036 → FR-047, DBR-02, DBR-06,
-- DBR-07; research T-04). Dịch vụ, lịch đến của Nháp, phiên bản nội dung và phụ lục gộp vào contract_version theo cột kind (R-15).
-- Bỏ chính sách vắng theo hợp đồng (Q-395). Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

-- Hợp đồng, nhóm 2: nội dung chỉ sửa được khi Nháp và không chờ duyệt điều khoản (trigger bên dưới). contract_number
-- (HD-<năm>-<id 6 chữ số>) đặt ngay sau INSERT trong cùng transaction. current_end_date là bản sao ngày kết thúc của phiên
-- bản nội dung mới nhất (FR-022); chỉ lệnh Ghi nhận đã ký và handler áp dụng phụ lục ghi cột này.
CREATE TABLE contract
(
    id                           BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    contract_number              VARCHAR(20)    NULL,
    resident_id                  BIGINT         NOT NULL,
    residence_type               ENUM ('LONG_TERM_RESIDENTIAL', 'SHORT_TERM_RESIDENTIAL', 'DAY_CARE') NOT NULL,
    start_date                   DATE           NOT NULL,
    end_date                     DATE           NULL COMMENT 'Bắt buộc với nội trú ngắn ngày (FR-019)',
    current_end_date             DATE           NULL COMMENT 'Ngày kết thúc hiện hành sau phụ lục gia hạn (FR-022)',
    care_level                   ENUM ('BASIC', 'REGULAR', 'SPECIAL') NOT NULL,
    room_type_id                 BIGINT         NULL,
    bed_id                       BIGINT         NULL,
    signing_representative_id    BIGINT         NULL COMMENT 'Bắt buộc khi Ghi nhận đã ký (FR-021, DBR-02)',
    signed_date                  DATE           NULL,
    scan_file_id                 BIGINT         NULL,
    residency_fee_billing_period ENUM ('MONTH', 'DAY', 'SESSION') NOT NULL,
    deposit_amount               DECIMAL(15, 0) NULL,
    no_deposit_required          BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'Cùng deposit_amount NULL là "không yêu cầu" (FR-030)',
    bed_hold_policy              VARCHAR(2000)  NULL,
    termination_condition        VARCHAR(2000)  NULL,
    content_hash                 CHAR(64)       NULL COMMENT 'SHA-256 nội dung nháp lúc gửi duyệt điều khoản (FR-025a)',
    awaiting_terms_approval      BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'Khóa nội dung khi yêu cầu duyệt điều khoản Chờ duyệt',
    terms_approval_request_id    BIGINT         NULL COMMENT 'Yêu cầu duyệt điều khoản đang chờ hoặc đã được duyệt cho content_hash',
    status                       ENUM ('DRAFT', 'PENDING_SIGNATURE', 'ACTIVE', 'ENDED', 'TERMINATED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    termination_reason           VARCHAR(2000)  NULL,
    terminated_on                DATE           NULL COMMENT 'Ngày chuyển Chấm dứt, mốc dừng tính phí (FR-070, FR-075a)',
    overdue_since                DATE           NULL COMMENT 'Dấu "quá hạn hợp đồng" từ ngày liền sau ngày kết thúc (FR-029)',
    manager_notified_overdue_at  DATETIME(6)    NULL COMMENT 'Đã báo Quản lý viện vượt CFG-M02-10, một lần mỗi hợp đồng',
    version                      INT            NOT NULL DEFAULT 0,
    created_by                   BIGINT         NOT NULL,
    created_at                   DATETIME(6)    NOT NULL,
    CONSTRAINT uq_contract_number UNIQUE (contract_number),
    CONSTRAINT chk_contract_short_term_end CHECK (residence_type <> 'SHORT_TERM_RESIDENTIAL' OR end_date IS NOT NULL),
    CONSTRAINT chk_contract_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_contract_deposit CHECK (deposit_amount IS NULL OR deposit_amount >= 0),
    CONSTRAINT chk_contract_deposit_choice CHECK (NOT no_deposit_required OR deposit_amount IS NULL),
    CONSTRAINT chk_contract_terminated_reason CHECK (status <> 'TERMINATED' OR termination_reason IS NOT NULL),
    CONSTRAINT chk_contract_signed CHECK (status NOT IN ('ACTIVE', 'ENDED', 'TERMINATED')
        OR (signed_date IS NOT NULL AND signing_representative_id IS NOT NULL AND scan_file_id IS NOT NULL)),
    KEY ix_contract_resident (resident_id, start_date),
    KEY ix_contract_status_end (status, current_end_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=BUSINESS';

-- Phiên bản hợp đồng. DRAFT: đúng một dòng mỗi hợp đồng, giữ dịch vụ và lịch đến dạng JSON, sửa được khi hợp đồng Nháp;
-- recorded_prices (serviceId -> đơn giá) đặt một lần lúc Ghi nhận đã ký (FR-036). CONTENT: phiên bản nội dung, nhóm 3, khóa
-- ngay khi thêm (FR-024, DBR-07). ADDENDUM: phụ lục do một yêu cầu M02.RESIDENCY_CHANGE đã duyệt tạo; khóa khi rời Chờ hiệu lực.
-- content_version_id không có khóa ngoại: phiên bản nội dung đã trỏ phụ lục, hai chiều RESTRICT thì job loại bỏ không xóa được.
CREATE TABLE contract_version
(
    id                  BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind                ENUM ('DRAFT', 'CONTENT', 'ADDENDUM') NOT NULL,
    contract_id         BIGINT         NOT NULL,
    created_at          DATETIME(6)    NOT NULL,
    locked_at           DATETIME(6)    NULL,
    -- DRAFT
    services            JSON           NULL,
    arrival_schedule    JSON           NULL,
    recorded_prices     JSON           NULL,
    -- CONTENT
    sequence_no         SMALLINT       NULL,
    effective_from      DATE           NULL,
    addendum_id         BIGINT         NULL,
    content             JSON           NULL,
    residence_type      ENUM ('LONG_TERM_RESIDENTIAL', 'SHORT_TERM_RESIDENTIAL', 'DAY_CARE') NULL,
    care_level          ENUM ('BASIC', 'REGULAR', 'SPECIAL') NULL,
    end_date            DATE           NULL,
    room_type_id        BIGINT         NULL,
    deposit_amount      DECIMAL(15, 0) NULL,
    -- ADDENDUM
    approval_request_id BIGINT         NULL,
    change_types        SET ('RESIDENCE_TYPE', 'CARE_LEVEL', 'SERVICE', 'DURATION', 'BED', 'ARRIVAL_SCHEDULE') NULL,
    content_before      JSON           NULL,
    content_after       JSON           NULL,
    effective_date      DATE           NULL,
    addendum_status     ENUM ('PENDING_EFFECTIVE', 'APPLIED', 'NOT_APPLIED') NULL,
    content_version_id  BIGINT         NULL,
    draft_contract_id   BIGINT GENERATED ALWAYS AS (IF(kind = 'DRAFT', contract_id, NULL)) STORED,
    CONSTRAINT uq_contract_version_draft UNIQUE (draft_contract_id),
    CONSTRAINT uq_contract_version_sequence UNIQUE (contract_id, sequence_no),
    CONSTRAINT uq_contract_version_request UNIQUE (approval_request_id),
    CONSTRAINT fk_contract_version_contract FOREIGN KEY (contract_id) REFERENCES contract (id) ON DELETE RESTRICT,
    CONSTRAINT fk_contract_version_addendum FOREIGN KEY (addendum_id) REFERENCES contract_version (id) ON DELETE RESTRICT,
    CONSTRAINT chk_contract_version_draft CHECK (kind <> 'DRAFT' OR (services IS NOT NULL AND arrival_schedule IS NOT NULL)),
    CONSTRAINT chk_contract_version_content CHECK (kind <> 'CONTENT' OR (sequence_no IS NOT NULL AND effective_from IS NOT NULL
        AND content IS NOT NULL AND residence_type IS NOT NULL AND care_level IS NOT NULL)),
    CONSTRAINT chk_contract_version_addendum_row CHECK (kind <> 'ADDENDUM' OR (approval_request_id IS NOT NULL
        AND change_types IS NOT NULL AND content_before IS NOT NULL AND content_after IS NOT NULL
        AND effective_date IS NOT NULL AND addendum_status IS NOT NULL)),
    CONSTRAINT chk_contract_version_applied CHECK (addendum_status IS NULL OR addendum_status <> 'APPLIED'
        OR content_version_id IS NOT NULL),
    KEY ix_contract_version_lookup (contract_id, kind, effective_from, sequence_no),
    KEY ix_contract_version_addendum_date (contract_id, kind, effective_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=MULTIPLE';

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$

-- Nội dung hợp đồng chỉ sửa được khi Nháp và không chờ duyệt điều khoản (FR-020, FR-023, FR-025a, DBR-06). Số hợp đồng
-- đặt một lần rồi bất biến. Các cột trạng thái, ký, dấu quá hạn, khóa duyệt, phiên bản vẫn ghi được.
CREATE TRIGGER trg_contract_lock_content
    BEFORE UPDATE
    ON contract
    FOR EACH ROW
BEGIN
    IF (OLD.status <> 'DRAFT' OR OLD.awaiting_terms_approval)
        AND NOT (NEW.resident_id <=> OLD.resident_id
            AND NEW.residence_type <=> OLD.residence_type
            AND NEW.start_date <=> OLD.start_date
            AND NEW.end_date <=> OLD.end_date
            AND NEW.care_level <=> OLD.care_level
            AND NEW.room_type_id <=> OLD.room_type_id
            AND NEW.bed_id <=> OLD.bed_id
            AND NEW.residency_fee_billing_period <=> OLD.residency_fee_billing_period
            AND NEW.deposit_amount <=> OLD.deposit_amount
            AND NEW.no_deposit_required <=> OLD.no_deposit_required
            AND NEW.bed_hold_policy <=> OLD.bed_hold_policy
            AND NEW.termination_condition <=> OLD.termination_condition) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract content is locked outside DRAFT or while awaiting terms approval (DBR-06)';
    END IF;
    IF OLD.contract_number IS NOT NULL AND NOT (NEW.contract_number <=> OLD.contract_number) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract number is immutable once set (FR-019)';
    END IF;
END$$

-- Lưới an toàn của DBR-06: không có hai hợp đồng Hiệu lực chồng khoảng [ngày bắt đầu, ngày kết thúc hiện hành], gồm cả hai
-- đầu, ngày kết thúc rỗng là vô hạn. Cơ chế chính là khóa theo người ở dịch vụ.
CREATE TRIGGER trg_contract_no_overlap
    BEFORE UPDATE
    ON contract
    FOR EACH ROW
BEGIN
    IF NEW.status = 'ACTIVE'
        AND (OLD.status <> 'ACTIVE' OR NOT (NEW.current_end_date <=> OLD.current_end_date)) THEN
        IF EXISTS (SELECT 1
                   FROM contract c
                   WHERE c.resident_id = NEW.resident_id
                     AND c.id <> NEW.id
                     AND c.status = 'ACTIVE'
                     AND c.start_date <= COALESCE(NEW.current_end_date, NEW.end_date, '9999-12-31')
                     AND NEW.start_date <= COALESCE(c.current_end_date, c.end_date, '9999-12-31')) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a resident cannot have two overlapping ACTIVE contracts (DBR-06)';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_contract_no_delete
    BEFORE DELETE
    ON contract
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract rows are removed only by the retention job (FR-051)';
    END IF;
END$$

-- DBR-07: ngày hiệu lực của phụ lục không sớm hơn ngày bắt đầu hợp đồng gốc (FR-040). Backend kiểm trước; đây là lưới an toàn.
CREATE TRIGGER trg_contract_version_addendum_date
    BEFORE INSERT
    ON contract_version
    FOR EACH ROW
BEGIN
    IF NEW.kind = 'ADDENDUM'
        AND NEW.effective_date < (SELECT c.start_date FROM contract c WHERE c.id = NEW.contract_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'addendum effective date cannot precede the contract start date (DBR-07)';
    END IF;
END$$

-- Dòng đã khóa không đổi (constitution XII). DRAFT: dịch vụ và lịch đến khóa theo trạng thái hợp đồng cha (FR-020, FR-023,
-- FR-025a, DBR-06); đơn giá đã ghi đặt một lần (FR-036). ADDENDUM: nội dung phụ lục không đổi (FR-047).
CREATE TRIGGER trg_contract_version_row_lock
    BEFORE UPDATE
    ON contract_version
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract_version row is locked (constitution XII)';
    END IF;
    IF OLD.kind = 'DRAFT' THEN
        IF NOT (CAST(NEW.services AS CHAR) <=> CAST(OLD.services AS CHAR)
            AND CAST(NEW.arrival_schedule AS CHAR) <=> CAST(OLD.arrival_schedule AS CHAR))
            AND EXISTS (SELECT 1 FROM contract c WHERE c.id = OLD.contract_id
                                                   AND (c.status <> 'DRAFT' OR c.awaiting_terms_approval)) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract services and schedule are locked outside DRAFT or while awaiting terms approval (DBR-06)';
        END IF;
        IF OLD.recorded_prices IS NOT NULL
            AND NOT (CAST(NEW.recorded_prices AS CHAR) <=> CAST(OLD.recorded_prices AS CHAR)) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'recorded unit prices are set once (FR-036)';
        END IF;
    END IF;
    IF OLD.kind = 'ADDENDUM' AND NOT (NEW.contract_id <=> OLD.contract_id
        AND NEW.approval_request_id <=> OLD.approval_request_id
        AND NEW.change_types <=> OLD.change_types
        AND NEW.effective_date <=> OLD.effective_date
        AND NEW.created_at <=> OLD.created_at
        AND CAST(NEW.content_before AS CHAR) <=> CAST(OLD.content_before AS CHAR)
        AND CAST(NEW.content_after AS CHAR) <=> CAST(OLD.content_after AS CHAR)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract addendum content is immutable (FR-047)';
    END IF;
END$$

CREATE TRIGGER trg_contract_version_no_delete
    BEFORE DELETE
    ON contract_version
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract_version rows are removed only by the retention job (DBR-23)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON contract TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON contract_version TO 'eldercare_app'@'%';
GRANT DELETE ON contract TO 'eldercare_retention'@'%';
GRANT DELETE ON contract_version TO 'eldercare_retention'@'%';
