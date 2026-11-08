-- T036: đính chính được phép Hủy ghi nhận (Đã đóng -> Đã hủy) và đổi loại trên sự cố Đã đóng.
-- Mọi cột xử lý khác của sự cố đã kết thúc vẫn bị chặn.
DROP TRIGGER trg_incident_row_lock;

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
    IF OLD.status IN ('CLOSED','CANCELLED') THEN
        IF NOT (OLD.status = 'CLOSED'
                AND NEW.status IN ('CLOSED','CANCELLED')
                AND NEW.level <=> OLD.level
                AND NEW.assignee_id <=> OLD.assignee_id
                AND NEW.resolution <=> OLD.resolution
                AND NEW.infection_result <=> OLD.infection_result
                AND NEW.closed_by <=> OLD.closed_by
                AND NEW.closed_at <=> OLD.closed_at
                AND NEW.received_at <=> OLD.received_at
                AND NEW.was_emergency <=> OLD.was_emergency) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='incident handling is closed';
        END IF;
    END IF;
END$$
DELIMITER ;
