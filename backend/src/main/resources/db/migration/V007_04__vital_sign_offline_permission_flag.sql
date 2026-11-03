-- 002 A-11: giữ cờ đồng bộ sau khi mất quyền trên bản ghi M06, kể cả lần đo lại.
-- Bảng đã khai DataGroup=MULTIPLE ở V007_01; cờ chỉ được ghi lúc INSERT.
ALTER TABLE vital_sign_record
    ADD COLUMN synced_after_permission_loss BOOLEAN NOT NULL DEFAULT FALSE;

DROP TRIGGER trg_vital_sign_record_row_lock;
DELIMITER $$
CREATE TRIGGER trg_vital_sign_record_row_lock BEFORE UPDATE ON vital_sign_record FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL
       OR OLD.confirmation_status <> 'AWAITING_REMEASUREMENT'
       OR NEW.confirmation_status NOT IN ('CONFIRMED','REMEASURED')
       OR NEW.confirmed_at IS NULL OR NEW.locked_at IS NULL
       OR NOT (NEW.locked_at <=> NEW.confirmed_at)
       OR NOT (NEW.id <=> OLD.id)
       OR NOT (NEW.resident_id <=> OLD.resident_id)
       OR NOT (NEW.indicator_id <=> OLD.indicator_id)
       OR NOT (NEW.measured_at <=> OLD.measured_at)
       OR NOT (NEW.measured_by <=> OLD.measured_by)
       OR NOT (NEW.task_id <=> OLD.task_id)
       OR NOT (NEW.device_measured_at <=> OLD.device_measured_at)
       OR NOT (NEW.offline <=> OLD.offline)
       OR NOT (NEW.synced_after_permission_loss <=> OLD.synced_after_permission_loss)
       OR NOT (NEW.threshold_version_id <=> OLD.threshold_version_id)
       OR NOT (NEW.level <=> OLD.level)
       OR NOT (NEW.remeasurement_deadline <=> OLD.remeasurement_deadline)
       OR NOT (NEW.remeasurement_for <=> OLD.remeasurement_for)
       OR NOT (NEW.components <=> OLD.components)
       OR NOT (NEW.note <=> OLD.note)
       OR NOT (NEW.created_at <=> OLD.created_at)
       OR NOT (NEW.created_by <=> OLD.created_by)
       -- Chỉ được thêm NOT_REMEASURED (bit 2), không bỏ nhãn cũ.
       OR ((NEW.labels+0) & (OLD.labels+0)) <> (OLD.labels+0)
       OR (((NEW.labels+0) ^ (OLD.labels+0)) & ~2) <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='vital_sign_record is immutable except one confirmation';
    END IF;
END$$
DELIMITER ;
