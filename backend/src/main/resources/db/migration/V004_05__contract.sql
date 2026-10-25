-- 004 T032: hợp đồng lưu trú, bảng con và phiên bản nội dung (data-model mục 5; FR-019 → FR-029, FR-036, DBR-02, DBR-06;
-- research T-04). Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000). Người cao tuổi, loại phòng, giường, người
-- đại diện, tệp, yêu cầu phê duyệt là định danh logic của module khác (constitution X) nên không có khóa ngoại.

-- Hợp đồng, nhóm 2: nội dung chỉ sửa được khi Nháp và không chờ duyệt điều khoản (trigger bên dưới). contract_number
-- (HD-<năm>-<id 6 chữ số>) đặt ngay sau INSERT trong cùng transaction. current_end_date là bản sao ngày kết thúc của phiên
-- bản nội dung mới nhất (FR-022); chỉ lệnh Ghi nhận đã ký và handler áp dụng phụ lục ghi cột này.
CREATE TABLE contract
(
    id                          BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    contract_number             VARCHAR(20)    NULL,
    resident_id                 BIGINT         NOT NULL,
    residence_type              ENUM ('LONG_TERM_RESIDENTIAL', 'SHORT_TERM_RESIDENTIAL', 'DAY_CARE') NOT NULL,
    start_date                  DATE           NOT NULL,
    end_date                    DATE           NULL COMMENT 'Bắt buộc với nội trú ngắn ngày (FR-019)',
    current_end_date            DATE           NULL COMMENT 'Ngày kết thúc hiện hành sau phụ lục gia hạn (FR-022)',
    care_level                  ENUM ('BASIC', 'REGULAR', 'SPECIAL') NOT NULL,
    room_type_id                BIGINT         NULL,
    bed_id                      BIGINT         NULL,
    signing_representative_id   BIGINT         NULL COMMENT 'Bắt buộc khi Ghi nhận đã ký (FR-021, DBR-02)',
    signed_date                 DATE           NULL,
    scan_file_id                BIGINT         NULL,
    residency_fee_billing_period ENUM ('MONTH', 'DAY', 'SESSION') NOT NULL,
    deposit_amount              DECIMAL(15, 0) NULL,
    no_deposit_required         BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'Cùng deposit_amount NULL là "không yêu cầu" (FR-030)',
    bed_hold_policy             VARCHAR(2000)  NULL,
    termination_condition       VARCHAR(2000)  NULL,
    content_hash                CHAR(64)       NULL COMMENT 'SHA-256 nội dung nháp lúc gửi duyệt điều khoản (FR-025a)',
    awaiting_terms_approval     BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'Khóa nội dung khi yêu cầu duyệt điều khoản Chờ duyệt',
    terms_approval_request_id   BIGINT         NULL COMMENT 'Yêu cầu duyệt điều khoản đang chờ hoặc đã được duyệt cho content_hash',
    status                      ENUM ('DRAFT', 'PENDING_SIGNATURE', 'ACTIVE', 'ENDED', 'TERMINATED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    termination_reason          VARCHAR(2000)  NULL,
    overdue_since               DATE           NULL COMMENT 'Dấu "quá hạn hợp đồng" từ ngày liền sau ngày kết thúc (FR-029)',
    manager_notified_overdue_at DATETIME(6)    NULL COMMENT 'Đã báo Quản lý viện vượt CFG-M02-10, một lần mỗi hợp đồng',
    version                     INT            NOT NULL DEFAULT 0,
    created_by                  BIGINT         NOT NULL,
    created_at                  DATETIME(6)    NOT NULL,
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
  COLLATE = utf8mb4_0900_ai_ci;

-- Dịch vụ đăng ký trên hợp đồng (FR-019, FR-036). custom_unit_price là giá khác phiên bản đơn giá chuẩn (điều khoản khác
-- chuẩn); recorded_unit_price là đơn giá ghi lúc Ghi nhận đã ký, đặt một lần.
CREATE TABLE contract_service
(
    id                  BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    contract_id         BIGINT         NOT NULL,
    service_id          BIGINT         NOT NULL,
    in_package          BOOLEAN        NOT NULL DEFAULT FALSE,
    quantity            DECIMAL(10, 2) NOT NULL DEFAULT 1,
    custom_unit_price   DECIMAL(15, 0) NULL,
    recorded_unit_price DECIMAL(15, 0) NULL,
    CONSTRAINT uq_contract_service UNIQUE (contract_id, service_id),
    CONSTRAINT chk_contract_service_quantity CHECK (quantity > 0),
    CONSTRAINT chk_contract_service_price CHECK (custom_unit_price IS NULL OR custom_unit_price >= 0),
    CONSTRAINT fk_contract_service_contract FOREIGN KEY (contract_id) REFERENCES contract (id) ON DELETE RESTRICT,
    CONSTRAINT fk_contract_service_service FOREIGN KEY (service_id) REFERENCES service (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dòng ghi đè chính sách phí khi vắng, cùng cấu trúc CFG-M02-05 (FR-050, FR-051). Không chồng khoảng ngày trong cùng loại
-- lưu trú và loại vắng: dịch vụ kiểm.
CREATE TABLE contract_absence_policy
(
    id             BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    contract_id    BIGINT      NOT NULL,
    residence_type ENUM ('LONG_TERM_RESIDENTIAL', 'SHORT_TERM_RESIDENTIAL', 'DAY_CARE') NOT NULL,
    absence_type   VARCHAR(40) NOT NULL,
    from_day       INT         NULL,
    to_day         INT         NULL COMMENT 'NULL là không giới hạn',
    coefficient    VARCHAR(30) NOT NULL COMMENT 'Số phần trăm, PER_CONTRACT hoặc MANAGER_DECIDES',
    hold_bed       ENUM ('YES', 'NO', 'PER_CONTRACT', 'MANAGER_DECIDES') NULL,
    CONSTRAINT chk_contract_absence_days CHECK (from_day IS NULL OR to_day IS NULL OR to_day >= from_day),
    CONSTRAINT fk_contract_absence_policy_contract FOREIGN KEY (contract_id) REFERENCES contract (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Lịch đến bán trú (FR-019, Q-286): thứ trong tuần hoặc ngày cụ thể, giờ về sau giờ đến trong cùng một ngày.
CREATE TABLE contract_arrival_schedule
(
    id             BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    contract_id    BIGINT   NOT NULL,
    day_of_week    TINYINT  NULL,
    `date`         DATE     NULL,
    arrival_time   TIME     NOT NULL,
    departure_time TIME     NOT NULL,
    CONSTRAINT chk_contract_arrival_day CHECK ((day_of_week IS NULL) <> (`date` IS NULL)),
    CONSTRAINT chk_contract_arrival_week CHECK (day_of_week IS NULL OR day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_contract_arrival_times CHECK (departure_time > arrival_time),
    CONSTRAINT fk_contract_arrival_schedule_contract FOREIGN KEY (contract_id) REFERENCES contract (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Phiên bản nội dung, nhóm 3 (chỉ thêm): 0 lúc ký, tăng theo phụ lục (FR-024, DBR-07). content giữ toàn bộ nội dung, gồm
-- đơn giá đã ghi; các cột còn lại là bản trích để đọc nhanh. addendum_id chưa có khóa ngoại tới bảng phụ lục (V004_06).
CREATE TABLE contract_content_version
(
    id             BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    contract_id    BIGINT         NOT NULL,
    sequence_no    SMALLINT       NOT NULL,
    effective_from DATE           NOT NULL,
    addendum_id    BIGINT         NULL,
    content        JSON           NOT NULL,
    residence_type ENUM ('LONG_TERM_RESIDENTIAL', 'SHORT_TERM_RESIDENTIAL', 'DAY_CARE') NOT NULL,
    care_level     ENUM ('BASIC', 'REGULAR', 'SPECIAL') NOT NULL,
    end_date       DATE           NULL,
    room_type_id   BIGINT         NULL,
    deposit_amount DECIMAL(15, 0) NULL,
    CONSTRAINT uq_contract_content_version_sequence UNIQUE (contract_id, sequence_no),
    CONSTRAINT uq_contract_content_version_from UNIQUE (contract_id, effective_from),
    CONSTRAINT fk_contract_content_version_contract FOREIGN KEY (contract_id) REFERENCES contract (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

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
-- đầu, ngày kết thúc rỗng là vô hạn. Cơ chế chính là khóa resident_residency ở dịch vụ.
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

-- Bảng con: khóa theo trạng thái của hợp đồng cha (FR-020, FR-023, FR-025a).
CREATE TRIGGER trg_contract_service_lock_insert
    BEFORE INSERT
    ON contract_service
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM contract c WHERE c.id = NEW.contract_id AND (c.status <> 'DRAFT' OR c.awaiting_terms_approval)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract services are locked outside DRAFT or while awaiting terms approval (DBR-06)';
    END IF;
END$$

-- Ngoại lệ duy nhất: đơn giá ghi lúc ký (FR-036) đặt một lần dù hợp đồng đã khóa.
CREATE TRIGGER trg_contract_service_lock_update
    BEFORE UPDATE
    ON contract_service
    FOR EACH ROW
BEGIN
    IF NOT (NEW.contract_id <=> OLD.contract_id
        AND NEW.service_id <=> OLD.service_id
        AND NEW.in_package <=> OLD.in_package
        AND NEW.quantity <=> OLD.quantity
        AND NEW.custom_unit_price <=> OLD.custom_unit_price)
        AND EXISTS (SELECT 1 FROM contract c WHERE c.id = OLD.contract_id AND (c.status <> 'DRAFT' OR c.awaiting_terms_approval)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract services are locked outside DRAFT or while awaiting terms approval (DBR-06)';
    END IF;
    IF OLD.recorded_unit_price IS NOT NULL AND NOT (NEW.recorded_unit_price <=> OLD.recorded_unit_price) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'recorded unit price is set once (FR-036)';
    END IF;
END$$

CREATE TRIGGER trg_contract_service_lock_delete
    BEFORE DELETE
    ON contract_service
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention'
        AND EXISTS (SELECT 1 FROM contract c WHERE c.id = OLD.contract_id AND (c.status <> 'DRAFT' OR c.awaiting_terms_approval)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract services are locked outside DRAFT or while awaiting terms approval (DBR-06)';
    END IF;
END$$

CREATE TRIGGER trg_contract_absence_policy_lock_insert
    BEFORE INSERT
    ON contract_absence_policy
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM contract c WHERE c.id = NEW.contract_id AND (c.status <> 'DRAFT' OR c.awaiting_terms_approval)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract absence policy is locked outside DRAFT or while awaiting terms approval (DBR-06)';
    END IF;
END$$

CREATE TRIGGER trg_contract_absence_policy_lock_update
    BEFORE UPDATE
    ON contract_absence_policy
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM contract c WHERE c.id IN (OLD.contract_id, NEW.contract_id) AND (c.status <> 'DRAFT' OR c.awaiting_terms_approval)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract absence policy is locked outside DRAFT or while awaiting terms approval (DBR-06)';
    END IF;
END$$

CREATE TRIGGER trg_contract_absence_policy_lock_delete
    BEFORE DELETE
    ON contract_absence_policy
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention'
        AND EXISTS (SELECT 1 FROM contract c WHERE c.id = OLD.contract_id AND (c.status <> 'DRAFT' OR c.awaiting_terms_approval)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract absence policy is locked outside DRAFT or while awaiting terms approval (DBR-06)';
    END IF;
END$$

CREATE TRIGGER trg_contract_arrival_schedule_lock_insert
    BEFORE INSERT
    ON contract_arrival_schedule
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM contract c WHERE c.id = NEW.contract_id AND (c.status <> 'DRAFT' OR c.awaiting_terms_approval)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract arrival schedule is locked outside DRAFT or while awaiting terms approval (DBR-06)';
    END IF;
END$$

CREATE TRIGGER trg_contract_arrival_schedule_lock_update
    BEFORE UPDATE
    ON contract_arrival_schedule
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM contract c WHERE c.id IN (OLD.contract_id, NEW.contract_id) AND (c.status <> 'DRAFT' OR c.awaiting_terms_approval)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract arrival schedule is locked outside DRAFT or while awaiting terms approval (DBR-06)';
    END IF;
END$$

CREATE TRIGGER trg_contract_arrival_schedule_lock_delete
    BEFORE DELETE
    ON contract_arrival_schedule
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention'
        AND EXISTS (SELECT 1 FROM contract c WHERE c.id = OLD.contract_id AND (c.status <> 'DRAFT' OR c.awaiting_terms_approval)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract arrival schedule is locked outside DRAFT or while awaiting terms approval (DBR-06)';
    END IF;
END$$

CREATE TRIGGER trg_contract_content_version_no_delete
    BEFORE DELETE
    ON contract_content_version
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract_content_version rows are removed only by the retention job (DBR-23)';
    END IF;
END$$
DELIMITER ;

CREATE TRIGGER trg_contract_content_version_no_update
    BEFORE UPDATE
    ON contract_content_version
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract_content_version is append-only (DBR-23)';

GRANT INSERT, UPDATE ON contract TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON contract_service TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON contract_absence_policy TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON contract_arrival_schedule TO 'eldercare_app'@'%';
GRANT INSERT ON contract_content_version TO 'eldercare_app'@'%';
GRANT DELETE ON contract TO 'eldercare_retention'@'%';
GRANT DELETE ON contract_service TO 'eldercare_retention'@'%';
GRANT DELETE ON contract_absence_policy TO 'eldercare_retention'@'%';
GRANT DELETE ON contract_arrival_schedule TO 'eldercare_retention'@'%';
GRANT DELETE ON contract_content_version TO 'eldercare_retention'@'%';
