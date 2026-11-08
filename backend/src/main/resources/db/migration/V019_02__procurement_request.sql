-- 019 T006 (data-model mục 0, 0.1, 3; FR-008 → FR-008d, BR-M11-17 → 19, DBR-36; glossary 25.7). Cú pháp dùng được ở
-- MySQL 8.0 và 8.4 (research R-01 của 000). Theo R-15: dòng (yeu_cau_mua_sam_dong) và nguồn gắn (mua_sam_gan_nguon) là cột
-- JSON của yêu cầu; domain kiểm dòng, một nguồn gắn đúng một yêu cầu (DBR-36) kiểm dưới khóa kỹ thuật. Mốc nhắc
-- CFG-M11-08, 09 ghi ở reminder_marker (nhac_019 bỏ). Tiền DECIMAL(15,0) theo quy ước 000 (audit F-07).
-- Nhóm 2 → 3: nội dung khóa khi rời Nháp; locked_at đặt khi vào trạng thái cuối (Đã đối chiếu, Từ chối, Đã hủy).
CREATE TABLE procurement_request
(
    id                            BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    department_id                 BIGINT         NOT NULL,
    created_by_staff_id           BIGINT         NOT NULL,
    type                          ENUM ('INGREDIENT', 'ASSET', 'SERVICE', 'MEDICATION', 'OTHER') NOT NULL,
    priority                      ENUM ('NORMAL', 'URGENT') NULL COMMENT 'Bắt buộc khi MEDICATION (FR-008d)',
    needed_by                     DATETIME(6)    NULL COMMENT 'Bắt buộc khi MEDICATION (FR-008d)',
    related_file_ids              JSON           NULL,
    `lines`                       JSON           NOT NULL COMMENT 'Dòng: name, quantity, estimatedUnitPrice, amount; MEDICATION thêm residentId, prescriptionId, activeIngredient, strength, dosageForm, unit',
    estimated_total               DECIMAL(15, 0) NOT NULL,
    reason                        VARCHAR(2000)  NOT NULL,
    needed_on                     DATE           NULL,
    reference                     JSON           NULL,
    original_request_id           BIGINT         NULL COMMENT 'Yêu cầu bổ sung phần vượt (FR-008a)',
    approved_amount               DECIMAL(15, 0) NULL,
    reduction_reason              VARCHAR(2000)  NULL,
    decided_by_staff_id           BIGINT         NULL,
    approved_at                   DATETIME(6)    NULL COMMENT 'Mốc nhắc CFG-M11-08',
    actual_amount                 DECIMAL(15, 0) NULL,
    purchased_on                  DATE           NULL,
    supplier                      VARCHAR(500)   NULL,
    receipt_file_ids              JSON           NULL,
    purchase_recorded_by_staff_id BIGINT         NULL,
    purchase_recorded_at          DATETIME(6)    NULL COMMENT 'Mốc nhắc CFG-M11-09',
    reconciliation_result         ENUM ('MATCHED', 'DISCREPANCY') NULL,
    discrepancy_amount            DECIMAL(15, 0) NULL,
    discrepancy_resolution        ENUM ('ACCEPT_DISCREPANCY', 'REQUEST_REFUND') NULL,
    reconciled_by_staff_id        BIGINT         NULL,
    linked_sources                JSON           NOT NULL COMMENT 'Nguồn gắn (DBR-36): sourceType ASSET|MAINTENANCE, sourceId, amount, linkedAt',
    status                        ENUM ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'PENDING_RECONCILIATION', 'DISCREPANCY',
        'RECONCILED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    status_reason                 VARCHAR(2000)  NULL COMMENT 'Lý do của lệnh chuyển gần nhất: từ chối, hủy, trả lại, ghi lệch, xử lý lệch',
    created_at                    DATETIME(6)    NOT NULL,
    locked_at                     DATETIME(6)    NULL,
    version                       INT            NOT NULL DEFAULT 0,
    CONSTRAINT chk_procurement_request_medication CHECK (type <> 'MEDICATION' OR (priority IS NOT NULL AND needed_by IS NOT NULL)),
    CONSTRAINT chk_procurement_request_amounts CHECK (estimated_total >= 0
        AND (approved_amount IS NULL OR approved_amount >= 0)
        AND (actual_amount IS NULL OR actual_amount >= 0)),
    CONSTRAINT chk_procurement_request_final_locked CHECK (status NOT IN ('RECONCILED', 'REJECTED', 'CANCELLED')
        OR locked_at IS NOT NULL),
    KEY ix_procurement_request_department (department_id, created_at),
    KEY ix_procurement_request_status (status, approved_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=MULTIPLE';

DELIMITER $$

-- FR-008: yêu cầu đã gửi không sửa nội dung; muốn đổi thì Rút lại về Nháp.
CREATE TRIGGER trg_procurement_request_lock_content
    BEFORE UPDATE
    ON procurement_request
    FOR EACH ROW
BEGIN
    IF OLD.status <> 'DRAFT'
        AND NOT (NEW.department_id <=> OLD.department_id
            AND NEW.created_by_staff_id <=> OLD.created_by_staff_id
            AND NEW.type <=> OLD.type
            AND NEW.priority <=> OLD.priority
            AND NEW.needed_by <=> OLD.needed_by
            AND NEW.related_file_ids <=> OLD.related_file_ids
            AND NEW.`lines` <=> OLD.`lines`
            AND NEW.estimated_total <=> OLD.estimated_total
            AND NEW.reason <=> OLD.reason
            AND NEW.needed_on <=> OLD.needed_on
            AND NEW.reference <=> OLD.reference
            AND NEW.original_request_id <=> OLD.original_request_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'procurement request content is locked outside DRAFT (FR-008)';
    END IF;
END$$

-- Constitution XII, R-15: dòng ở trạng thái cuối không đổi.
CREATE TRIGGER trg_procurement_request_row_lock
    BEFORE UPDATE
    ON procurement_request
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'procurement request row is locked (FR-008)';
    END IF;
END$$

CREATE TRIGGER trg_procurement_request_no_delete
    BEFORE DELETE
    ON procurement_request
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'procurement request rows are removed only by the retention job';
    END IF;
END$$

DELIMITER ;

GRANT INSERT, UPDATE ON procurement_request TO 'eldercare_app'@'%';
GRANT DELETE ON procurement_request TO 'eldercare_retention'@'%';
