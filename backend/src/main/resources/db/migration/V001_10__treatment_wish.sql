-- Phiếu nguyện vọng khi điều trị (001 US7; FR-023j → FR-023m, BR-M01-16, DBR-39; data-model mục 6b; research H-18).
-- Nhóm 2, có phiên bản theo đúng mẫu của phiếu nguyện vọng cuối đời (V001_09): nội dung không sửa, không xóa; chỉ đổi
-- theo chiều ACTIVE → EXPIRED. Không có dòng nhắc định kỳ (FR-023l). Số thẻ bảo hiểm y tế chờ đối chiếu pháp lý 24.4
-- (Q-384). Chỉ dùng cú pháp có ở MySQL 8.0 và 8.4 (research R-01 của 000).
CREATE TABLE treatment_wish
(
    id                           BIGINT       NOT NULL AUTO_INCREMENT,
    resident_id                  BIGINT       NOT NULL,
    hospital1_name               VARCHAR(255) NOT NULL,
    hospital1_address            VARCHAR(500) NOT NULL,
    hospital2_name               VARCHAR(255) NULL,
    hospital2_address            VARCHAR(500) NULL,
    insurance_registration_place VARCHAR(255) NULL,
    insurance_card_number        VARCHAR(20)  NULL,
    insurance_card_expiry        DATE         NULL,
    ask_family_first             BOOLEAN      NOT NULL COMMENT 'Hỏi gia đình trước khi chuyển viện không khẩn cấp',
    escort                       ENUM ('FAMILY','FACILITY_STAFF','CASE_BY_CASE') NOT NULL,
    note                         TEXT         NULL COMMENT 'Ý kiến gia đình khác không ghi ở đây mà ở treatment_wish_opinion (BR-M01-14)',
    respondent_type              ENUM ('RESIDENT','FAMILY_MEMBER','OBSERVING_STAFF') NOT NULL,
    respondent_family_member_id  BIGINT       NULL COMMENT 'Hồ sơ người thân do 012 giữ nên không có khóa ngoại',
    signed_copy_file_id          BIGINT       NULL COMMENT 'Không bắt buộc',
    status                       ENUM ('ACTIVE','EXPIRED') NOT NULL,
    superseded_by_id             BIGINT       NULL,
    active_resident_id           BIGINT GENERATED ALWAYS AS (IF(status = 'ACTIVE', resident_id, NULL)) STORED COMMENT 'UNIQUE: tối đa một phiên bản Hiệu lực (DBR-39)',
    recorded_by                  BIGINT       NOT NULL,
    recorded_at                  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_treatment_wish_active (active_resident_id),
    KEY ix_treatment_wish_resident (resident_id, id),
    CONSTRAINT fk_treatment_wish_resident FOREIGN KEY (resident_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT fk_treatment_wish_signed_copy FOREIGN KEY (signed_copy_file_id) REFERENCES file (id) ON DELETE RESTRICT,
    CONSTRAINT fk_treatment_wish_superseded_by FOREIGN KEY (superseded_by_id) REFERENCES treatment_wish (id) ON DELETE RESTRICT,
    CONSTRAINT ck_treatment_wish_hospital1 CHECK (
        CHAR_LENGTH(TRIM(hospital1_name)) > 0 AND CHAR_LENGTH(TRIM(hospital1_address)) > 0),
    -- FR-023j: bệnh viện thứ hai đủ cả tên và địa chỉ hoặc không có cả hai
    CONSTRAINT ck_treatment_wish_hospital2 CHECK (
        (hospital2_name IS NULL AND hospital2_address IS NULL)
            OR (CHAR_LENGTH(TRIM(hospital2_name)) > 0 AND CHAR_LENGTH(TRIM(hospital2_address)) > 0)),
    CONSTRAINT ck_treatment_wish_respondent CHECK (
        (respondent_type = 'FAMILY_MEMBER') = (respondent_family_member_id IS NOT NULL)),
    CONSTRAINT ck_treatment_wish_status CHECK (status = 'EXPIRED' OR superseded_by_id IS NULL)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Người thân cần báo: 0..n dòng, mỗi người có quan hệ Hiệu lực lúc ghi (kiểm ở dịch vụ, hồ sơ người thân do 012 giữ).
CREATE TABLE treatment_wish_notify
(
    treatment_wish_id BIGINT NOT NULL,
    family_member_id  BIGINT NOT NULL,
    PRIMARY KEY (treatment_wish_id, family_member_id),
    KEY ix_treatment_wish_notify_member (family_member_id),
    CONSTRAINT fk_treatment_wish_notify_wish FOREIGN KEY (treatment_wish_id) REFERENCES treatment_wish (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Ý kiến gia đình khác ý kiến người cao tuổi: một dòng một ý kiến có nguồn, chỉ thêm (BR-M01-14, FR-023g).
CREATE TABLE treatment_wish_opinion
(
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    treatment_wish_id       BIGINT      NOT NULL,
    source_type             ENUM ('RESIDENT','FAMILY_MEMBER','OBSERVING_STAFF') NOT NULL,
    source_family_member_id BIGINT      NULL COMMENT 'Hồ sơ người thân do 012 giữ nên không có khóa ngoại',
    content                 TEXT        NOT NULL,
    recorded_by             BIGINT      NOT NULL,
    recorded_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_treatment_wish_opinion_wish (treatment_wish_id, id),
    CONSTRAINT fk_treatment_wish_opinion_wish FOREIGN KEY (treatment_wish_id) REFERENCES treatment_wish (id) ON DELETE RESTRICT,
    CONSTRAINT ck_treatment_wish_opinion_source CHECK (
        (source_type = 'FAMILY_MEMBER') = (source_family_member_id IS NOT NULL)),
    CONSTRAINT ck_treatment_wish_opinion_content CHECK (CHAR_LENGTH(TRIM(content)) > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
CREATE TRIGGER trg_treatment_wish_only_expire
    BEFORE UPDATE
    ON treatment_wish
    FOR EACH ROW
BEGIN
    IF NOT (NEW.resident_id <=> OLD.resident_id
        AND NEW.hospital1_name <=> OLD.hospital1_name
        AND NEW.hospital1_address <=> OLD.hospital1_address
        AND NEW.hospital2_name <=> OLD.hospital2_name
        AND NEW.hospital2_address <=> OLD.hospital2_address
        AND NEW.insurance_registration_place <=> OLD.insurance_registration_place
        AND NEW.insurance_card_number <=> OLD.insurance_card_number
        AND NEW.insurance_card_expiry <=> OLD.insurance_card_expiry
        AND NEW.ask_family_first <=> OLD.ask_family_first
        AND NEW.escort <=> OLD.escort
        AND NEW.note <=> OLD.note
        AND NEW.respondent_type <=> OLD.respondent_type
        AND NEW.respondent_family_member_id <=> OLD.respondent_family_member_id
        AND NEW.signed_copy_file_id <=> OLD.signed_copy_file_id
        AND NEW.recorded_by <=> OLD.recorded_by
        AND NEW.recorded_at <=> OLD.recorded_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'treatment wish content is immutable (FR-023k)';
    END IF;
    IF OLD.status = 'ACTIVE' AND NEW.status <> 'EXPIRED' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'an active treatment wish can only expire (DBR-39)';
    END IF;
    IF OLD.status = 'EXPIRED' THEN
        -- Bản hết hiệu lực nhận tham chiếu tới bản mới sau khi bản mới được thêm (đúng một lần, từ NULL)
        IF NOT (NEW.status <=> OLD.status
            AND (NEW.superseded_by_id <=> OLD.superseded_by_id OR OLD.superseded_by_id IS NULL)) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'an expired treatment wish cannot change (DBR-39)';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_treatment_wish_no_delete
    BEFORE DELETE
    ON treatment_wish
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'treatment_wish rows are removed only by the retention job (FR-051)';
    END IF;
END$$

CREATE TRIGGER trg_treatment_wish_notify_no_delete
    BEFORE DELETE
    ON treatment_wish_notify
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'treatment_wish_notify rows are removed only by the retention job (FR-051)';
    END IF;
END$$

CREATE TRIGGER trg_treatment_wish_opinion_no_delete
    BEFORE DELETE
    ON treatment_wish_opinion
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'treatment_wish_opinion rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

CREATE TRIGGER trg_treatment_wish_notify_no_update
    BEFORE UPDATE
    ON treatment_wish_notify
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'treatment_wish_notify is append-only (FR-023k)';

CREATE TRIGGER trg_treatment_wish_opinion_no_update
    BEFORE UPDATE
    ON treatment_wish_opinion
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'treatment_wish_opinion is append-only (BR-M01-14)';

GRANT INSERT ON treatment_wish TO 'eldercare_app'@'%';
GRANT UPDATE (status, superseded_by_id) ON treatment_wish TO 'eldercare_app'@'%';
GRANT INSERT ON treatment_wish_notify TO 'eldercare_app'@'%';
GRANT INSERT ON treatment_wish_opinion TO 'eldercare_app'@'%';
GRANT DELETE ON treatment_wish TO 'eldercare_retention'@'%';
GRANT DELETE ON treatment_wish_notify TO 'eldercare_retention'@'%';
GRANT DELETE ON treatment_wish_opinion TO 'eldercare_retention'@'%';
