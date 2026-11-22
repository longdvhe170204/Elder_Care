-- 001 FR-036b (Q-423, chủ dự án chốt 2026-11-20; plan-bf08 Bước 6): quyết định của Hành chính khi gia đình không đồng ý đổi
-- mức chăm sóc theo lần đánh giá lại: giữ mức cũ (kèm thỏa thuận lựa chọn có rủi ro đang hiệu lực và ngày xem lại) hoặc rời
-- viện. Mỗi lần đánh giá tối đa một quyết định; chỉ thêm, trừ cột review_notified_at (đánh dấu đã nhắc ngày xem lại).
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

CREATE TABLE care_level_change_decision
(
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id        BIGINT        NOT NULL,
    assessment_id      BIGINT        NOT NULL,
    decision           ENUM ('KEEP_CURRENT_LEVEL', 'LEAVE_FACILITY') NOT NULL,
    risk_agreement_id  BIGINT        NULL,
    review_date        DATE          NULL,
    note               VARCHAR(1000) NULL,
    recorded_by        BIGINT        NOT NULL,
    recorded_at        DATETIME(6)   NOT NULL,
    review_notified_at DATETIME(6)   NULL,
    CONSTRAINT uq_care_level_change_decision_assessment UNIQUE (assessment_id),
    CONSTRAINT fk_care_level_change_decision_resident FOREIGN KEY (resident_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT chk_care_level_change_decision_keep CHECK (decision <> 'KEEP_CURRENT_LEVEL'
        OR (risk_agreement_id IS NOT NULL AND review_date IS NOT NULL)),
    KEY ix_care_level_change_decision_review (review_date, review_notified_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=RECORD';

DELIMITER $$

-- Chỉ được đặt review_notified_at một lần; các cột khác không đổi (constitution XII)
CREATE TRIGGER trg_care_level_change_decision_no_update
    BEFORE UPDATE
    ON care_level_change_decision
    FOR EACH ROW
BEGIN
    IF NOT (NEW.resident_id <=> OLD.resident_id
        AND NEW.assessment_id <=> OLD.assessment_id
        AND NEW.decision <=> OLD.decision
        AND NEW.risk_agreement_id <=> OLD.risk_agreement_id
        AND NEW.review_date <=> OLD.review_date
        AND NEW.note <=> OLD.note
        AND NEW.recorded_by <=> OLD.recorded_by
        AND NEW.recorded_at <=> OLD.recorded_at)
        OR (OLD.review_notified_at IS NOT NULL AND NOT (NEW.review_notified_at <=> OLD.review_notified_at)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'care level change decisions are append-only (constitution XII)';
    END IF;
END$$

CREATE TRIGGER trg_care_level_change_decision_no_delete
    BEFORE DELETE
    ON care_level_change_decision
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'care level change decisions are removed only by the retention job (DBR-23)';
    END IF;
END$$

DELIMITER ;

GRANT INSERT, UPDATE ON care_level_change_decision TO 'eldercare_app'@'%';
GRANT DELETE ON care_level_change_decision TO 'eldercare_retention'@'%';
