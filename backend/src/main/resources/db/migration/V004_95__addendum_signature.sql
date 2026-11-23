-- 004 Phase 14, Bước 3 (FR-043d; Q-419, chủ dự án chốt 2026-11-20): phụ lục của yêu cầu không phải thay đổi nhỏ ở Chờ ký tới
-- khi Hành chính ghi nhận bản đã ký; yêu cầu bị hủy thì phụ lục Đã hủy (FR-043f, Q-420); số phụ lục tăng dần theo hợp đồng.
-- Đánh số 95 (không phải 07 như plan-bf08.md): sau V004_94 đã chạy, số nhỏ hơn phải bật out-of-order (team-plan mục 8).
-- Phụ lục có sẵn: signature_required = FALSE (giữ hành vi cũ), số phụ lục theo (created_at, id) của từng hợp đồng.
-- Tham số CFG-M02-17, 18 nạp ở V099_09 (sau khi parameter_history gộp vào status_history ở V099_06; nạp ở đây thì
-- schema đã qua V099_06 báo lỗi bảng không có). Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

ALTER TABLE contract_version
    MODIFY addendum_status ENUM ('PENDING_SIGNATURE', 'PENDING_EFFECTIVE', 'APPLIED', 'NOT_APPLIED', 'CANCELLED') NULL,
    ADD COLUMN addendum_no                 SMALLINT      NULL AFTER content_version_id,
    ADD COLUMN signature_required          BOOLEAN       NOT NULL DEFAULT FALSE AFTER addendum_no,
    ADD COLUMN signed_date                 DATE          NULL AFTER signature_required,
    ADD COLUMN signed_file_id              BIGINT        NULL AFTER signed_date,
    ADD COLUMN signed_by_family_member_id  BIGINT        NULL AFTER signed_file_id,
    ADD COLUMN facility_signatory_staff_id BIGINT        NULL AFTER signed_by_family_member_id,
    ADD COLUMN signature_recorded_by       BIGINT        NULL AFTER facility_signatory_staff_id,
    ADD COLUMN signature_recorded_at       DATETIME(6)   NULL AFTER signature_recorded_by,
    ADD COLUMN signature_note              VARCHAR(1000) NULL AFTER signature_recorded_at;

-- Trigger cũ chặn UPDATE dòng đã khóa: bỏ trước khi đánh số phụ lục có sẵn, tạo lại ở dưới
DROP TRIGGER IF EXISTS trg_contract_version_row_lock;

UPDATE contract_version cv
    JOIN (SELECT id, ROW_NUMBER() OVER (PARTITION BY contract_id ORDER BY created_at, id) AS rn
          FROM contract_version
          WHERE kind = 'ADDENDUM') numbered ON numbered.id = cv.id
SET cv.addendum_no = numbered.rn;

ALTER TABLE contract_version
    ADD CONSTRAINT uq_contract_version_addendum_no UNIQUE (contract_id, addendum_no),
    ADD CONSTRAINT chk_contract_version_addendum_no CHECK (kind <> 'ADDENDUM' OR addendum_no IS NOT NULL),
    ADD CONSTRAINT chk_contract_version_signed CHECK (signature_required = FALSE
        OR addendum_status IN ('PENDING_SIGNATURE', 'NOT_APPLIED', 'CANCELLED')
        OR signed_date IS NOT NULL);

-- Dòng đã khóa không đổi (constitution XII). DRAFT: chép nguyên V004_03. ADDENDUM: nội dung, số phụ lục, dấu cần ký không
-- đổi (FR-047, FR-043d); ngày hiệu lực chỉ dời muộn hơn khi còn Chờ ký, Chờ hiệu lực (ký muộn, Q-419); các cột ký đặt một
-- lần khi phụ lục còn Chờ ký.
DELIMITER $$

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
        AND NEW.created_at <=> OLD.created_at
        AND NEW.addendum_no <=> OLD.addendum_no
        AND NEW.signature_required <=> OLD.signature_required
        AND CAST(NEW.content_before AS CHAR) <=> CAST(OLD.content_before AS CHAR)
        AND CAST(NEW.content_after AS CHAR) <=> CAST(OLD.content_after AS CHAR)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract addendum content is immutable (FR-047)';
    END IF;
    IF OLD.kind = 'ADDENDUM' AND NOT (NEW.effective_date <=> OLD.effective_date)
        AND NOT (OLD.addendum_status IN ('PENDING_SIGNATURE', 'PENDING_EFFECTIVE')
            AND NEW.effective_date > OLD.effective_date) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'addendum effective date only moves later before it is applied (Q-419)';
    END IF;
    IF OLD.kind = 'ADDENDUM' AND NOT (NEW.signed_date <=> OLD.signed_date
        AND NEW.signed_file_id <=> OLD.signed_file_id
        AND NEW.signed_by_family_member_id <=> OLD.signed_by_family_member_id
        AND NEW.facility_signatory_staff_id <=> OLD.facility_signatory_staff_id
        AND NEW.signature_recorded_by <=> OLD.signature_recorded_by
        AND NEW.signature_recorded_at <=> OLD.signature_recorded_at
        AND NEW.signature_note <=> OLD.signature_note)
        AND (OLD.signed_date IS NOT NULL OR OLD.addendum_status <> 'PENDING_SIGNATURE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'addendum signature is recorded once, while pending signature (Q-419)';
    END IF;
END$$

DELIMITER ;
