-- Nội dung ghi nhận bất biến và trạng thái xử lý cùng dòng (T034, R-15).
CREATE TABLE incident
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NULL,
    incident_type_id BIGINT NOT NULL,
    current_type_id BIGINT NOT NULL,
    type_code VARCHAR(64) NOT NULL,
    context ENUM ('CARE','EATING','MEDICATION','MOBILITY','ACTIVITY','TRANSPORT','TREATMENT',
                  'OFFSITE_ACTIVITY','WRONG_MEAL_SERVED','BELONGINGS') NULL,
    occurred_at DATETIME(6) NULL,
    discovered_at DATETIME(6) NOT NULL,
    floor_area_id BIGINT NULL,
    location TEXT NULL,
    ongoing_activity TEXT NULL,
    description TEXT NOT NULL,
    initial_response TEXT NULL,
    initial_level ENUM ('LOW','MEDIUM','CRITICAL') NOT NULL,
    lower_level_reason TEXT NULL,
    source_alert_id BIGINT NULL,
    belonging_id BIGINT NULL,
    belonging_handover_id BIGINT NULL,
    out_of_scope BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    confirmed_at DATETIME(6) NOT NULL,
    status ENUM ('NEW','IN_PROGRESS','MONITORING','CLOSED','CANCELLED') NOT NULL DEFAULT 'NEW',
    level ENUM ('LOW','MEDIUM','CRITICAL') NOT NULL,
    was_emergency BOOLEAN NOT NULL DEFAULT FALSE,
    escalation_level TINYINT NOT NULL DEFAULT 0,
    acknowledgement_deadline DATETIME(6) NULL,
    max_escalation_at DATETIME(6) NULL,
    max_reminder_count INT NOT NULL DEFAULT 0,
    assignee_id BIGINT NULL,
    handover_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    received_at DATETIME(6) NULL,
    received_by BIGINT NULL,
    resolution TEXT NULL,
    infection_result ENUM ('CONFIRMED_INFECTION','RULED_OUT','UNDETERMINED') NULL,
    closed_by BIGINT NULL,
    closed_at DATETIME(6) NULL,
    no_on_duty_doctor BOOLEAN NOT NULL DEFAULT FALSE,
    escalation_stopped BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    open_emergency TINYINT GENERATED ALWAYS AS
        (IF(level = 'CRITICAL' AND status IN ('NEW','IN_PROGRESS','MONITORING'), 1, NULL)) STORED,
    CONSTRAINT chk_incident_resident CHECK (resident_id IS NOT NULL OR type_code = 'FACILITY'),
    CONSTRAINT chk_incident_facility_location CHECK (type_code <> 'FACILITY' OR floor_area_id IS NOT NULL),
    KEY ix_incident_resident_status (resident_id, status),
    KEY ix_incident_floor_status (floor_area_id, status),
    KEY ix_incident_open_emergency (resident_id, open_emergency),
    KEY ix_incident_deadline (status, acknowledgement_deadline)
) COMMENT='DataGroup=MULTIPLE';

DELIMITER $$
CREATE TRIGGER trg_incident_row_lock BEFORE UPDATE ON incident FOR EACH ROW
BEGIN
    IF NOT (NEW.id <=> OLD.id)
       OR NOT (NEW.resident_id <=> OLD.resident_id)
       OR NOT (NEW.incident_type_id <=> OLD.incident_type_id)
       OR NOT (NEW.context <=> OLD.context)
       OR NOT (NEW.occurred_at <=> OLD.occurred_at)
       OR NOT (NEW.discovered_at <=> OLD.discovered_at)
       OR NOT (NEW.floor_area_id <=> OLD.floor_area_id)
       OR NOT (NEW.location <=> OLD.location)
       OR NOT (NEW.ongoing_activity <=> OLD.ongoing_activity)
       OR NOT (NEW.description <=> OLD.description)
       OR NOT (NEW.initial_response <=> OLD.initial_response)
       OR NOT (NEW.initial_level <=> OLD.initial_level)
       OR NOT (NEW.lower_level_reason <=> OLD.lower_level_reason)
       OR NOT (NEW.source_alert_id <=> OLD.source_alert_id)
       OR NOT (NEW.belonging_id <=> OLD.belonging_id)
       OR NOT (NEW.belonging_handover_id <=> OLD.belonging_handover_id)
       OR NOT (NEW.out_of_scope <=> OLD.out_of_scope)
       OR NOT (NEW.recorded_by <=> OLD.recorded_by)
       OR NOT (NEW.created_at <=> OLD.created_at)
       OR NOT (NEW.confirmed_at <=> OLD.confirmed_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='incident content is immutable';
    END IF;
    -- Đính chính được nối ở T036. Mọi đường khác giữ đóng/hủy là trạng thái cuối.
    IF OLD.status IN ('CLOSED','CANCELLED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='incident handling is closed';
    END IF;
END$$
CREATE TRIGGER trg_incident_no_delete BEFORE DELETE ON incident FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(),'@',1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='incident is removed only by retention';
    END IF;
END$$
DELIMITER ;

GRANT INSERT ON incident TO 'eldercare_app'@'%';
GRANT UPDATE (current_type_id,type_code,status,level,was_emergency,escalation_level,
              acknowledgement_deadline,max_escalation_at,max_reminder_count,assignee_id,
              handover_acknowledged,received_at,received_by,resolution,infection_result,
              closed_by,closed_at,no_on_duty_doctor,escalation_stopped,version) ON incident TO 'eldercare_app'@'%';
GRANT DELETE ON incident TO 'eldercare_retention'@'%';
