-- 007 T021 phần lịch đo (R-15: lich_do → monitoring_setting kind MEASUREMENT_SCHEDULE; lich_do_chi_so, tần suất,
-- mức quan trọng, ghi chú → JSON components). indicator_id giữ chỉ số đầu của lịch để dùng chỉ mục; khóa duy nhất
-- Hiệu lực, Chờ hiệu lực chỉ áp cho ngưỡng (một người có thể có nhiều lịch đo cùng lúc).
ALTER TABLE monitoring_setting DROP CHECK chk_monitoring_kind;
ALTER TABLE monitoring_setting
    ADD CONSTRAINT chk_monitoring_kind CHECK (kind IN ('THRESHOLD','MEASUREMENT_SCHEDULE'));
ALTER TABLE monitoring_setting
    MODIFY status ENUM('PENDING','EFFECTIVE','EXPIRED','CANCELLED','STOPPED') NOT NULL;
ALTER TABLE monitoring_setting
    ADD COLUMN replaces_id BIGINT NULL AFTER components,
    ADD COLUMN stopped_at DATETIME(6) NULL AFTER replaces_id;
ALTER TABLE monitoring_setting
    MODIFY effective_key VARCHAR(100) GENERATED ALWAYS AS
        (IF(kind='THRESHOLD' AND status='EFFECTIVE',scope_key,NULL)) STORED,
    MODIFY pending_key VARCHAR(100) GENERATED ALWAYS AS
        (IF(kind='THRESHOLD' AND status='PENDING',scope_key,NULL)) STORED;
ALTER TABLE monitoring_setting
    ADD CONSTRAINT chk_monitoring_schedule_personal CHECK (kind <> 'MEASUREMENT_SCHEDULE' OR scope = 'PERSONAL'),
    ADD CONSTRAINT chk_monitoring_threshold_status CHECK (kind <> 'THRESHOLD' OR status <> 'STOPPED'),
    ADD CONSTRAINT chk_monitoring_schedule_status CHECK (kind <> 'MEASUREMENT_SCHEDULE' OR status <> 'CANCELLED'),
    ADD INDEX idx_monitoring_resident_kind (resident_id, kind, status);

DROP TRIGGER trg_monitoring_setting_update_guard;
DELIMITER $$
CREATE TRIGGER trg_monitoring_setting_update_guard BEFORE UPDATE ON monitoring_setting FOR EACH ROW
BEGIN
    IF OLD.status IN ('EXPIRED','CANCELLED','STOPPED')
       OR NOT (NEW.id <=> OLD.id) OR NOT (NEW.kind <=> OLD.kind)
       OR NOT (NEW.scope <=> OLD.scope) OR NOT (NEW.resident_id <=> OLD.resident_id)
       OR NOT (NEW.indicator_id <=> OLD.indicator_id)
       OR NOT (NEW.effective_from <=> OLD.effective_from)
       OR NOT (NEW.components <=> OLD.components) OR NOT (NEW.reason <=> OLD.reason)
       OR NOT (NEW.replaces_id <=> OLD.replaces_id)
       OR NOT (NEW.created_by <=> OLD.created_by) OR NOT (NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='monitoring_setting version is immutable';
    END IF;
END$$
DELIMITER ;
