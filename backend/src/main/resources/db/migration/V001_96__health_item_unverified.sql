-- 001 T101 (Q-400, BR-M01-17): mục sức khỏe do Hành chính ghi theo lời gia đình mang dấu "chưa kiểm chứng" tới khi Bác sĩ hoặc
-- Điều dưỡng kiểm chứng. Cột thêm mới, không đổi cột cũ; dòng cũ mặc định đã kiểm chứng. Cú pháp dùng được ở MySQL 8.0 và 8.4.
ALTER TABLE health_item
    ADD COLUMN unverified  BOOLEAN     NOT NULL DEFAULT FALSE COMMENT 'Tự khai của gia đình, chưa được Bác sĩ hoặc Điều dưỡng kiểm chứng (Q-400)',
    ADD COLUMN verified_by BIGINT      NULL COMMENT 'Tài khoản kiểm chứng',
    ADD COLUMN verified_at DATETIME(6) NULL,
    ADD CONSTRAINT ck_health_item_verified CHECK (
        (unverified = TRUE AND verified_by IS NULL AND verified_at IS NULL)
            OR (unverified = FALSE AND (verified_by IS NULL) = (verified_at IS NULL)));

DROP TRIGGER trg_health_item_immutable;

DELIMITER $$
CREATE TRIGGER trg_health_item_immutable
    BEFORE UPDATE
    ON health_item
    FOR EACH ROW
BEGIN
    IF NOT (OLD.resident_id <=> NEW.resident_id AND OLD.type <=> NEW.type AND OLD.allergen_id <=> NEW.allergen_id
        AND OLD.free_text <=> NEW.free_text AND OLD.severity_id <=> NEW.severity_id
        AND OLD.source_id <=> NEW.source_id AND OLD.recorded_by <=> NEW.recorded_by
        AND OLD.recorded_at <=> NEW.recorded_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'health_item content is immutable (DBR-04)';
    END IF;
    IF OLD.status = 'EXCLUDED' AND NOT (NEW.status <=> 'EXCLUDED' AND OLD.excluded_at <=> NEW.excluded_at
        AND OLD.excluded_by <=> NEW.excluded_by AND OLD.exclusion_reason <=> NEW.exclusion_reason) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'an excluded health_item cannot be reinstated (DBR-04)';
    END IF;
    IF OLD.unverified = FALSE AND NOT (NEW.unverified = FALSE AND OLD.verified_by <=> NEW.verified_by
        AND OLD.verified_at <=> NEW.verified_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a verified health_item cannot become unverified (Q-400)';
    END IF;
END$$
DELIMITER ;
