-- Dịch loại cảnh báo nút hỗ trợ NV_YEU_CAU_HO_TRO thành STAFF_ASSIST_REQUEST (007 T062, bản dịch contracts/api-translation.md mục 7.1).
-- Trigger chặn sửa dòng đã đóng được tạo lại sau khi đổi dòng cũ; không sửa V007_03 đã chạy.
DROP TRIGGER IF EXISTS trg_alert_update_guard;
ALTER TABLE alert DROP CHECK chk_alert_resident;
ALTER TABLE alert MODIFY COLUMN open_assist_lock BIGINT GENERATED ALWAYS AS
    (IF(type = 'STAFF_ASSIST_REQUEST' AND status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS'), requester_id, NULL)) STORED;
UPDATE alert
SET type = 'STAFF_ASSIST_REQUEST', type_lock = REPLACE(type_lock, 'NV_YEU_CAU_HO_TRO', 'STAFF_ASSIST_REQUEST')
WHERE type = 'NV_YEU_CAU_HO_TRO';
ALTER TABLE alert ADD CONSTRAINT chk_alert_resident CHECK (resident_id IS NOT NULL OR type = 'STAFF_ASSIST_REQUEST');

DELIMITER $$
CREATE TRIGGER trg_alert_update_guard BEFORE UPDATE ON alert FOR EACH ROW
BEGIN
    -- Chặn đổi khi đã đóng hoặc chuyển sự cố, trừ khi chỉ cập nhật version
    IF OLD.status IN ('CLOSED', 'CONVERTED_TO_INCIDENT') THEN
        IF NOT (NEW.resident_id <=> OLD.resident_id
            AND NEW.floor_area_id <=> OLD.floor_area_id
            AND NEW.type <=> OLD.type
            AND NEW.type_lock <=> OLD.type_lock
            AND NEW.level <=> OLD.level
            AND NEW.level_at_received <=> OLD.level_at_received
            AND NEW.occurrence_count <=> OLD.occurrence_count
            AND NEW.first_at <=> OLD.first_at
            AND NEW.latest_at <=> OLD.latest_at
            AND NEW.status <=> OLD.status
            AND NEW.escalation_level <=> OLD.escalation_level
            AND NEW.acknowledgement_deadline <=> OLD.acknowledgement_deadline
            AND NEW.max_escalation_at <=> OLD.max_escalation_at
            AND NEW.max_reminder_count <=> OLD.max_reminder_count
            AND NEW.assignee_id <=> OLD.assignee_id
            AND NEW.handover_acknowledged <=> OLD.handover_acknowledged
            AND NEW.receiver_id <=> OLD.receiver_id
            AND NEW.received_at <=> OLD.received_at
            AND NEW.resolution <=> OLD.resolution
            AND NEW.close_reason <=> OLD.close_reason
            AND NEW.closed_by_id <=> OLD.closed_by_id
            AND NEW.closed_by_system <=> OLD.closed_by_system
            AND NEW.closed_at <=> OLD.closed_at
            AND NEW.incident_id <=> OLD.incident_id
            AND NEW.critical_flag_id <=> OLD.critical_flag_id
            AND NEW.no_on_duty_doctor <=> OLD.no_on_duty_doctor
            AND NEW.escalation_stopped <=> OLD.escalation_stopped
            AND NEW.created_by_id <=> OLD.created_by_id
            AND NEW.description <=> OLD.description
            AND NEW.requester_id <=> OLD.requester_id
            AND NEW.arrived_at <=> OLD.arrived_at
            AND NEW.arrived_by_id <=> OLD.arrived_by_id
            AND NEW.skip_arrival_time <=> OLD.skip_arrival_time) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'closed or converted alert is immutable';
        END IF;
    END IF;

    -- Chặn đổi type_lock và first_at
    IF NOT (NEW.type_lock <=> OLD.type_lock) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert type_lock is immutable';
    END IF;
    IF NOT (NEW.first_at <=> OLD.first_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert first_at is immutable';
    END IF;

    -- Chặn đổi resident_id (trừ khi gán nct_id lần đầu cho STAFF_ASSIST_REQUEST)
    IF NOT (NEW.resident_id <=> OLD.resident_id) THEN
        IF NOT (OLD.resident_id IS NULL AND NEW.resident_id IS NOT NULL AND OLD.type = 'STAFF_ASSIST_REQUEST') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert resident_id is immutable once set';
        END IF;
    END IF;

    -- Chặn đổi arrived_at khi đã có
    IF OLD.arrived_at IS NOT NULL AND NOT (NEW.arrived_at <=> OLD.arrived_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert arrived_at is immutable once set';
    END IF;
END$$
DELIMITER ;
