-- Phần nguồn đọc T013 của bảng gộp thiết lập theo dõi (R-15); lịch đo bổ sung ở T021.
CREATE TABLE monitoring_setting (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind VARCHAR(32) NOT NULL,
    scope ENUM('FACILITY','PERSONAL') NOT NULL,
    resident_id BIGINT NULL,
    indicator_id BIGINT NOT NULL,
    effective_from DATETIME(6) NOT NULL,
    effective_until DATETIME(6) NULL,
    status ENUM('PENDING','EFFECTIVE','EXPIRED','CANCELLED') NOT NULL,
    components JSON NOT NULL,
    reason TEXT NULL,
    ended_reason TEXT NULL,
    ended_by BIGINT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    scope_key VARCHAR(100) GENERATED ALWAYS AS
        (CONCAT(kind,':',scope,':',IFNULL(resident_id,0),':',indicator_id)) STORED,
    effective_key VARCHAR(100) GENERATED ALWAYS AS
        (IF(status='EFFECTIVE',scope_key,NULL)) STORED,
    pending_key VARCHAR(100) GENERATED ALWAYS AS
        (IF(status='PENDING',scope_key,NULL)) STORED,
    UNIQUE KEY uk_monitoring_effective(effective_key),
    UNIQUE KEY uk_monitoring_pending(pending_key),
    KEY idx_monitoring_threshold_at(indicator_id,resident_id,effective_from),
    CONSTRAINT chk_monitoring_kind CHECK(kind='THRESHOLD'),
    CONSTRAINT chk_monitoring_scope CHECK(
        (scope='FACILITY' AND resident_id IS NULL) OR (scope='PERSONAL' AND resident_id IS NOT NULL)),
    CONSTRAINT chk_monitoring_interval CHECK(effective_until IS NULL OR effective_until > effective_from),
    CONSTRAINT chk_monitoring_components CHECK(JSON_TYPE(components)='OBJECT' AND JSON_LENGTH(components)>0)
) COMMENT='DataGroup=STATEFUL';

DELIMITER $$
CREATE TRIGGER trg_monitoring_setting_update_guard BEFORE UPDATE ON monitoring_setting FOR EACH ROW
BEGIN
    IF OLD.status IN ('EXPIRED','CANCELLED')
       OR NOT (NEW.id <=> OLD.id) OR NOT (NEW.kind <=> OLD.kind)
       OR NOT (NEW.scope <=> OLD.scope) OR NOT (NEW.resident_id <=> OLD.resident_id)
       OR NOT (NEW.indicator_id <=> OLD.indicator_id)
       OR NOT (NEW.effective_from <=> OLD.effective_from)
       OR NOT (NEW.components <=> OLD.components) OR NOT (NEW.reason <=> OLD.reason)
       OR NOT (NEW.created_by <=> OLD.created_by) OR NOT (NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='monitoring_setting threshold version is immutable';
    END IF;
END$$
CREATE TRIGGER trg_monitoring_setting_no_delete BEFORE DELETE ON monitoring_setting FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='monitoring_setting rows are removed only by retention job';
    END IF;
END$$
DELIMITER ;
GRANT INSERT, UPDATE ON monitoring_setting TO 'eldercare_app'@'%';
GRANT DELETE ON monitoring_setting TO 'eldercare_retention'@'%';