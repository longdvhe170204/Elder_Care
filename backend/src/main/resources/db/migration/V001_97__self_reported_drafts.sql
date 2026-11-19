-- 001 T109 (Q-412, BR-M01-18): bản Nháp tự khai do Hành chính lập khi người cao tuổi còn Đang tiếp nhận.
-- Phiếu nguyện vọng: Nháp nằm ngay trong wish (data-model 0.1), kiểm chứng đổi dòng DRAFT -> ACTIVE nên giữ id và tệp bản ký.
-- Đánh giá đầu vào: phần "Gia đình khai" ở cột JSON riêng, tách khỏi nhận xét của Bác sĩ (FR-025a).
-- Chỉ thêm cột, giá trị; CHECK nào có thể sai với cột để trống của Nháp thì dựng lại với điều kiện bỏ qua Nháp, Đã hủy.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4.

ALTER TABLE wish
    MODIFY COLUMN status ENUM ('ACTIVE','SUPERSEDED','EXPIRED','DRAFT','DISCARDED') NOT NULL,
    ADD COLUMN verified_by    BIGINT        NULL COMMENT 'Bác sĩ, Điều dưỡng kiểm chứng bản Nháp (Q-412); rỗng với phiên bản ghi thẳng',
    ADD COLUMN verified_at    DATETIME(6)   NULL,
    ADD COLUMN discarded_by   BIGINT        NULL,
    ADD COLUMN discarded_at   DATETIME(6)   NULL,
    ADD COLUMN discard_reason VARCHAR(2000) NULL,
    ADD COLUMN draft_opinions JSON          NULL COMMENT 'Ý kiến khác của Nháp; khi kiểm chứng chép sang record_event M01.WISH_OPINION',
    ADD COLUMN draft_key      VARCHAR(40) GENERATED ALWAYS AS (IF(status = 'DRAFT', CONCAT(kind, ':', resident_id), NULL)) STORED
        COMMENT 'UNIQUE: tối đa một Nháp mỗi loại mỗi người (BR-M01-18)',
    ADD UNIQUE KEY uq_wish_draft (draft_key),
    ADD CONSTRAINT ck_wish_verified CHECK ((verified_by IS NULL) = (verified_at IS NULL)),
    ADD CONSTRAINT ck_wish_discard CHECK ((status = 'DISCARDED') = (discarded_by IS NOT NULL AND discarded_at IS NOT NULL
        AND discard_reason IS NOT NULL AND CHAR_LENGTH(TRIM(discard_reason)) > 0)),
    ADD CONSTRAINT ck_wish_draft_opinions CHECK (draft_opinions IS NULL OR status = 'DRAFT');

ALTER TABLE wish
    DROP CHECK ck_wish_kind_columns,
    DROP CHECK ck_wish_signer_member,
    DROP CHECK ck_wish_selection,
    DROP CHECK ck_wish_court;

ALTER TABLE wish
    ADD CONSTRAINT ck_wish_kind_columns CHECK (
        (kind = 'END_OF_LIFE'
            AND status IN ('ACTIVE', 'SUPERSEDED', 'DRAFT', 'DISCARDED')
            AND respondent_type IN ('RESIDENT', 'PATIENT_REPRESENTATIVE', 'LEGAL_REPRESENTATIVE')
            AND (status IN ('DRAFT', 'DISCARDED')
                OR (choice IS NOT NULL AND first_contact IS NOT NULL AND assessing_doctor_id IS NOT NULL
                    AND can_self_determine IS NOT NULL AND assessed_at IS NOT NULL AND support_method IS NOT NULL
                    AND review_date IS NOT NULL AND signer_type IS NOT NULL AND signed_at IS NOT NULL
                    AND signed_copy_file_id IS NOT NULL AND source IS NOT NULL))
            AND hospital1_name IS NULL AND hospital1_address IS NULL AND hospital2_name IS NULL
            AND hospital2_address IS NULL AND insurance_registration_place IS NULL AND insurance_card_number IS NULL
            AND insurance_card_expiry IS NULL AND ask_family_first IS NULL AND escort IS NULL
            AND notify_family_member_ids IS NULL)
            OR (kind = 'TREATMENT'
            AND status IN ('ACTIVE', 'EXPIRED', 'DRAFT', 'DISCARDED')
            AND respondent_type IN ('RESIDENT', 'FAMILY_MEMBER', 'OBSERVING_STAFF')
            AND (status IN ('DRAFT', 'DISCARDED')
                OR (hospital1_name IS NOT NULL AND hospital1_address IS NOT NULL AND ask_family_first IS NOT NULL
                    AND escort IS NOT NULL))
            AND choice IS NULL AND first_contact IS NULL AND relationship IS NULL AND assessing_doctor_id IS NULL
            AND can_self_determine IS NULL AND assessed_at IS NULL AND support_method IS NULL AND review_date IS NULL
            AND signer_type IS NULL AND signer_family_member_id IS NULL AND representative_selection IS NULL
            AND court_decision_number IS NULL AND court_decision_date IS NULL AND court_decision_court IS NULL
            AND court_decision_file_id IS NULL AND signed_at IS NULL AND source IS NULL)),
    ADD CONSTRAINT ck_wish_signer_member CHECK (
        kind <> 'END_OF_LIFE' OR status IN ('DRAFT', 'DISCARDED')
            OR (signer_type = 'RESIDENT') = (signer_family_member_id IS NULL)),
    ADD CONSTRAINT ck_wish_selection CHECK (
        kind <> 'END_OF_LIFE' OR status IN ('DRAFT', 'DISCARDED')
            OR (signer_type = 'PATIENT_REPRESENTATIVE') = (representative_selection IS NOT NULL)),
    ADD CONSTRAINT ck_wish_court CHECK (
        kind <> 'END_OF_LIFE' OR status IN ('DRAFT', 'DISCARDED')
            OR (signer_type = 'LEGAL_REPRESENTATIVE'
            AND court_decision_number IS NOT NULL AND court_decision_date IS NOT NULL
            AND court_decision_court IS NOT NULL AND court_decision_file_id IS NOT NULL)
            OR (signer_type <> 'LEGAL_REPRESENTATIVE'
            AND court_decision_number IS NULL AND court_decision_date IS NULL
            AND court_decision_court IS NULL AND court_decision_file_id IS NULL));

DROP TRIGGER trg_wish_only_supersede;

DELIMITER $$
CREATE TRIGGER trg_wish_only_supersede
    BEFORE UPDATE
    ON wish
    FOR EACH ROW
BEGIN
    IF NOT (NEW.kind <=> OLD.kind
        AND NEW.legacy_id <=> OLD.legacy_id
        AND NEW.resident_id <=> OLD.resident_id
        AND NEW.recorded_by <=> OLD.recorded_by
        AND NEW.recorded_at <=> OLD.recorded_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wish owner and writer are immutable (FR-023b, FR-023k)';
    END IF;
    IF OLD.status = 'DRAFT' THEN
        -- Nháp tự khai (Q-412): sửa nội dung, kiểm chứng thành Hiệu lực hoặc Hủy nháp
        IF NEW.status NOT IN ('DRAFT', 'ACTIVE', 'DISCARDED') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a draft wish can only be edited, verified or discarded (BR-M01-18)';
        END IF;
        IF NEW.status = 'ACTIVE' AND (NEW.verified_by IS NULL OR NEW.draft_opinions IS NOT NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a verified wish needs its verifier (BR-M01-18)';
        END IF;
    ELSE
        IF NOT (NEW.note <=> OLD.note
            AND NEW.respondent_type <=> OLD.respondent_type
            AND NEW.respondent_family_member_id <=> OLD.respondent_family_member_id
            AND NEW.signed_copy_file_id <=> OLD.signed_copy_file_id
            AND NEW.choice <=> OLD.choice
            AND NEW.first_contact <=> OLD.first_contact
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
            AND NEW.source <=> OLD.source
            AND NEW.hospital1_name <=> OLD.hospital1_name
            AND NEW.hospital1_address <=> OLD.hospital1_address
            AND NEW.hospital2_name <=> OLD.hospital2_name
            AND NEW.hospital2_address <=> OLD.hospital2_address
            AND NEW.insurance_registration_place <=> OLD.insurance_registration_place
            AND NEW.insurance_card_number <=> OLD.insurance_card_number
            AND NEW.insurance_card_expiry <=> OLD.insurance_card_expiry
            AND NEW.ask_family_first <=> OLD.ask_family_first
            AND NEW.escort <=> OLD.escort
            AND NEW.notify_family_member_ids <=> OLD.notify_family_member_ids
            AND NEW.verified_by <=> OLD.verified_by
            AND NEW.verified_at <=> OLD.verified_at
            AND NEW.discarded_by <=> OLD.discarded_by
            AND NEW.discarded_at <=> OLD.discarded_at
            AND NEW.discard_reason <=> OLD.discard_reason
            AND NEW.draft_opinions <=> OLD.draft_opinions) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wish content is immutable (FR-023b, FR-023k)';
        END IF;
        IF OLD.status = 'ACTIVE' AND NEW.status <> IF(OLD.kind = 'END_OF_LIFE', 'SUPERSEDED', 'EXPIRED') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'an active wish can only be superseded or expire (DBR-34, DBR-39)';
        END IF;
        IF OLD.status <> 'ACTIVE' THEN
            -- Bản đã hết hiệu lực nhận tham chiếu tới bản mới sau khi bản mới được thêm (đúng một lần, từ NULL);
            -- bản Đã hủy không đổi nữa
            IF NOT (NEW.status <=> OLD.status
                AND (NEW.superseded_by_id <=> OLD.superseded_by_id
                    OR (OLD.superseded_by_id IS NULL AND OLD.status <> 'DISCARDED'))) THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a superseded, expired or discarded wish cannot change (DBR-34, DBR-39)';
            END IF;
        END IF;
    END IF;
END$$
DELIMITER ;

-- Nháp đổi nội dung nên tài khoản ứng dụng được UPDATE cả bảng; trigger trên giữ bất biến cho mọi trạng thái khác Nháp.
GRANT UPDATE ON wish TO 'eldercare_app'@'%';

ALTER TABLE assessment
    ADD COLUMN family_declared JSON NULL COMMENT 'Phần "Gia đình khai" do Hành chính ghi (Q-412, FR-025a); chỉ đánh giá đầu vào',
    ADD CONSTRAINT ck_assessment_family_declared CHECK (family_declared IS NULL OR type = 'INITIAL');
