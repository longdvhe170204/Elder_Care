-- Phiếu nguyện vọng cuối đời (001 US7; FR-023a → FR-023i, DBR-34; data-model mục 6, 6a; research H-09). Nhóm 2, có phiên
-- bản: nội dung không sửa, không xóa; chỉ đổi theo chiều ACTIVE → SUPERSEDED. Chỉ dùng cú pháp có ở MySQL 8.0 và 8.4
-- (research R-01 của 000).
CREATE TABLE end_of_life_wish
(
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    resident_id                 BIGINT       NOT NULL,
    choice                      ENUM ('TRANSFER_TO_HOSPITAL','RETURN_HOME','STAY_PALLIATIVE') NOT NULL,
    first_contact               VARCHAR(500) NOT NULL COMMENT 'Người cần liên hệ trước khi thực hiện',
    note                        TEXT         NULL COMMENT 'Ý kiến khác không ghi ở đây mà ở end_of_life_wish_opinion (FR-023g)',
    respondent_type             ENUM ('RESIDENT','PATIENT_REPRESENTATIVE','LEGAL_REPRESENTATIVE') NOT NULL,
    respondent_family_member_id BIGINT       NULL COMMENT 'Hồ sơ người thân do 012 giữ nên không có khóa ngoại',
    relationship                VARCHAR(100) NULL,
    assessing_doctor_id         BIGINT       NOT NULL COMMENT 'Mã tài khoản Bác sĩ (AC-03) lúc ghi',
    can_self_determine          BOOLEAN      NOT NULL COMMENT 'Nhận định cho chính phiếu này, không phải dấu của người cao tuổi (FR-023h)',
    assessed_at                 DATETIME(6)  NOT NULL,
    support_method              TEXT         NOT NULL COMMENT 'Cách đã hỗ trợ người cao tuổi hiểu thông tin',
    review_date                 DATE         NOT NULL COMMENT 'Ngày xem lại nhận định (FR-023i)',
    signer_type                 ENUM ('RESIDENT','PATIENT_REPRESENTATIVE','LEGAL_REPRESENTATIVE') NOT NULL,
    signer_family_member_id     BIGINT       NULL,
    representative_selection    ENUM ('CHOSEN_BY_RESIDENT','CHOSEN_BY_FAMILY') NULL,
    court_decision_number       VARCHAR(100) NULL,
    court_decision_date         DATE         NULL,
    court_decision_court        VARCHAR(200) NULL,
    court_decision_file_id      BIGINT       NULL,
    signed_at                   DATETIME(6)  NOT NULL,
    signed_copy_file_id         BIGINT       NOT NULL COMMENT 'Bản ký đã scan (FR-023a)',
    source                      ENUM ('ADMISSION','FAMILY_REQUEST','CRITICAL_RECONFIRMATION') NOT NULL,
    status                      ENUM ('ACTIVE','SUPERSEDED') NOT NULL,
    superseded_by_id            BIGINT       NULL,
    active_resident_id          BIGINT GENERATED ALWAYS AS (IF(status = 'ACTIVE', resident_id, NULL)) STORED COMMENT 'UNIQUE: tối đa một phiên bản Hiệu lực (DBR-34)',
    recorded_by                 BIGINT       NOT NULL,
    recorded_at                 DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_end_of_life_wish_active (active_resident_id),
    KEY ix_end_of_life_wish_resident (resident_id, id),
    KEY ix_end_of_life_wish_review (status, review_date),
    CONSTRAINT fk_end_of_life_wish_resident FOREIGN KEY (resident_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT fk_end_of_life_wish_signed_copy FOREIGN KEY (signed_copy_file_id) REFERENCES file (id) ON DELETE RESTRICT,
    CONSTRAINT fk_end_of_life_wish_court_file FOREIGN KEY (court_decision_file_id) REFERENCES file (id) ON DELETE RESTRICT,
    CONSTRAINT fk_end_of_life_wish_superseded_by FOREIGN KEY (superseded_by_id) REFERENCES end_of_life_wish (id) ON DELETE RESTRICT,
    -- FR-023c: người cao tuổi ký khi và chỉ khi Bác sĩ nhận định tự quyết được
    CONSTRAINT ck_end_of_life_wish_signer CHECK (can_self_determine = (signer_type = 'RESIDENT')),
    CONSTRAINT ck_end_of_life_wish_signer_member CHECK ((signer_type = 'RESIDENT') = (signer_family_member_id IS NULL)),
    CONSTRAINT ck_end_of_life_wish_respondent CHECK (
        (respondent_type = 'RESIDENT' AND respondent_family_member_id IS NULL AND relationship IS NULL)
            OR (respondent_type <> 'RESIDENT' AND respondent_family_member_id IS NOT NULL AND relationship IS NOT NULL)),
    CONSTRAINT ck_end_of_life_wish_selection CHECK (
        (signer_type = 'PATIENT_REPRESENTATIVE') = (representative_selection IS NOT NULL)),
    -- FR-011: người đại diện theo pháp luật ký thì có đủ bốn thông tin quyết định của Tòa án; loại khác thì không có cột nào
    CONSTRAINT ck_end_of_life_wish_court CHECK (
        (signer_type = 'LEGAL_REPRESENTATIVE'
            AND court_decision_number IS NOT NULL AND court_decision_date IS NOT NULL
            AND court_decision_court IS NOT NULL AND court_decision_file_id IS NOT NULL)
            OR (signer_type <> 'LEGAL_REPRESENTATIVE'
            AND court_decision_number IS NULL AND court_decision_date IS NULL
            AND court_decision_court IS NULL AND court_decision_file_id IS NULL)),
    CONSTRAINT ck_end_of_life_wish_assessment CHECK (
        CHAR_LENGTH(TRIM(support_method)) > 0 AND review_date >= DATE(assessed_at)),
    CONSTRAINT ck_end_of_life_wish_status CHECK (status = 'SUPERSEDED' OR superseded_by_id IS NULL)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Ý kiến khác với lựa chọn, thường của gia đình: một dòng một ý kiến có nguồn, ghi cùng phiên bản, chỉ thêm
-- (FR-023c, FR-023g; mẫu cột nguồn ý kiến ở data-model mục 6a).
CREATE TABLE end_of_life_wish_opinion
(
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    wish_id                 BIGINT      NOT NULL,
    source_type             ENUM ('RESIDENT','FAMILY_MEMBER','OBSERVING_STAFF') NOT NULL,
    source_family_member_id BIGINT      NULL COMMENT 'Hồ sơ người thân do 012 giữ nên không có khóa ngoại',
    content                 TEXT        NOT NULL,
    recorded_by             BIGINT      NOT NULL,
    recorded_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_end_of_life_wish_opinion_wish (wish_id, id),
    CONSTRAINT fk_end_of_life_wish_opinion_wish FOREIGN KEY (wish_id) REFERENCES end_of_life_wish (id) ON DELETE RESTRICT,
    CONSTRAINT ck_end_of_life_wish_opinion_source CHECK (
        (source_type = 'FAMILY_MEMBER') = (source_family_member_id IS NOT NULL)),
    CONSTRAINT ck_end_of_life_wish_opinion_content CHECK (CHAR_LENGTH(TRIM(content)) > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
CREATE TRIGGER trg_end_of_life_wish_only_supersede
    BEFORE UPDATE
    ON end_of_life_wish
    FOR EACH ROW
BEGIN
    IF NOT (NEW.resident_id <=> OLD.resident_id
        AND NEW.choice <=> OLD.choice
        AND NEW.first_contact <=> OLD.first_contact
        AND NEW.note <=> OLD.note
        AND NEW.respondent_type <=> OLD.respondent_type
        AND NEW.respondent_family_member_id <=> OLD.respondent_family_member_id
        AND NEW.relationship <=> OLD.relationship
        AND NEW.assessing_doctor_id <=> OLD.assessing_doctor_id
        AND NEW.can_self_determine <=> OLD.can_self_determine
        AND NEW.assessed_at <=> OLD.assessed_at
        AND NEW.support_method <=> OLD.support_method
        AND NEW.review_date <=> OLD.review_date
        AND NEW.signer_type <=> OLD.signer_type
        AND NEW.signer_family_member_id <=> OLD.signer_family_member_id
        AND NEW.representative_selection <=> OLD.representative_selection
        AND NEW.court_decision_number <=> OLD.court_decision_number
        AND NEW.court_decision_date <=> OLD.court_decision_date
        AND NEW.court_decision_court <=> OLD.court_decision_court
        AND NEW.court_decision_file_id <=> OLD.court_decision_file_id
        AND NEW.signed_at <=> OLD.signed_at
        AND NEW.signed_copy_file_id <=> OLD.signed_copy_file_id
        AND NEW.source <=> OLD.source
        AND NEW.recorded_by <=> OLD.recorded_by
        AND NEW.recorded_at <=> OLD.recorded_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'end of life wish content is immutable (FR-023b)';
    END IF;
    IF OLD.status = 'ACTIVE' AND NEW.status <> 'SUPERSEDED' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'an active wish can only become superseded (DBR-34)';
    END IF;
    IF OLD.status = 'SUPERSEDED' THEN
        -- Bản được thay thế nhận tham chiếu tới bản mới sau khi bản mới được thêm (đúng một lần, từ NULL)
        IF NOT (NEW.status <=> OLD.status
            AND (NEW.superseded_by_id <=> OLD.superseded_by_id OR OLD.superseded_by_id IS NULL)) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a superseded wish cannot change (DBR-34)';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_end_of_life_wish_no_delete
    BEFORE DELETE
    ON end_of_life_wish
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'end_of_life_wish rows are removed only by the retention job (FR-051)';
    END IF;
END$$

CREATE TRIGGER trg_end_of_life_wish_opinion_no_delete
    BEFORE DELETE
    ON end_of_life_wish_opinion
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'end_of_life_wish_opinion rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

CREATE TRIGGER trg_end_of_life_wish_opinion_no_update
    BEFORE UPDATE
    ON end_of_life_wish_opinion
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'end_of_life_wish_opinion is append-only (FR-023g)';

GRANT INSERT ON end_of_life_wish TO 'eldercare_app'@'%';
GRANT UPDATE (status, superseded_by_id) ON end_of_life_wish TO 'eldercare_app'@'%';
GRANT INSERT ON end_of_life_wish_opinion TO 'eldercare_app'@'%';
GRANT DELETE ON end_of_life_wish TO 'eldercare_retention'@'%';
GRANT DELETE ON end_of_life_wish_opinion TO 'eldercare_retention'@'%';
