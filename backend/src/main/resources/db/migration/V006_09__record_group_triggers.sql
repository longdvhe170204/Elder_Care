-- 006: Group 3 triggers for the absence, handover, PRN, family medication and supplied intake tables.
-- V006_05 -> V006_08 created these tables without the Group 3 triggers required by data-model sections 6, 8, 9, 9a.
-- Reference: data-model section 11; 000 data-model section 1; DBR-23, FR-030, FR-048a, FR-054.

DELIMITER $$

-- 1. medication_handover - Group 3, only status moves forward (HANDED_OVER -> WAITING_RETURN -> RETURNED; FR-048a)
CREATE TRIGGER trg_medication_handover_no_update
BEFORE UPDATE ON medication_handover
FOR EACH ROW
BEGIN
    IF NOT (OLD.resident_id <=> NEW.resident_id
            AND OLD.absence_interval_id <=> NEW.absence_interval_id
            AND OLD.trip_id <=> NEW.trip_id
            AND OLD.recipient_type <=> NEW.recipient_type
            AND OLD.recipient_id <=> NEW.recipient_id
            AND OLD.recipient_name <=> NEW.recipient_name
            AND OLD.relationship <=> NEW.relationship
            AND OLD.dispensed_by_id <=> NEW.dispensed_by_id
            AND OLD.handover_time <=> NEW.handover_time
            AND OLD.created_at <=> NEW.created_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Medication handover is append-only except its status (Group 3: FR-048a)';
    END IF;
    IF NOT (OLD.status <=> NEW.status
            OR (OLD.status = 'HANDED_OVER' AND NEW.status IN ('WAITING_RETURN', 'RETURNED'))
            OR (OLD.status = 'WAITING_RETURN' AND NEW.status = 'RETURNED')) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid medication handover status transition (FR-048a)';
    END IF;
END$$

CREATE TRIGGER trg_medication_handover_no_delete
BEFORE DELETE ON medication_handover
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Medication handover cannot be deleted (Group 3)';
END$$

-- 2. medication_handover_item - Group 3 (child of medication_handover)
CREATE TRIGGER trg_medication_handover_item_no_update
BEFORE UPDATE ON medication_handover_item
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Medication handover item is append-only (Group 3)';
END$$

CREATE TRIGGER trg_medication_handover_item_no_delete
BEFORE DELETE ON medication_handover_item
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Medication handover item cannot be deleted (Group 3)';
END$$

-- 3. medication_handover_dose - Group 3, reverted_at is set once when the dose returns early (Q-60)
CREATE TRIGGER trg_medication_handover_dose_no_update
BEFORE UPDATE ON medication_handover_dose
FOR EACH ROW
BEGIN
    IF NOT (OLD.handover_id <=> NEW.handover_id
            AND OLD.dose_id <=> NEW.dose_id
            AND (OLD.reverted_at <=> NEW.reverted_at OR OLD.reverted_at IS NULL)) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Medication handover dose is append-only except setting reverted_at once (Group 3: Q-60)';
    END IF;
END$$

CREATE TRIGGER trg_medication_handover_dose_no_delete
BEFORE DELETE ON medication_handover_dose
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Medication handover dose cannot be deleted (Group 3)';
END$$

-- 4. medication_return - Group 3
CREATE TRIGGER trg_medication_return_no_update
BEFORE UPDATE ON medication_return
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Medication return is append-only (Group 3)';
END$$

CREATE TRIGGER trg_medication_return_no_delete
BEFORE DELETE ON medication_return
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Medication return cannot be deleted (Group 3)';
END$$

-- 5. medication_return_item - Group 3 (child of medication_return)
CREATE TRIGGER trg_medication_return_item_no_update
BEFORE UPDATE ON medication_return_item
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Medication return item is append-only (Group 3)';
END$$

CREATE TRIGGER trg_medication_return_item_no_delete
BEFORE DELETE ON medication_return_item
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Medication return item cannot be deleted (Group 3)';
END$$

-- 6. no_medication_order - Group 3 (FR-048b)
CREATE TRIGGER trg_no_medication_order_no_update
BEFORE UPDATE ON no_medication_order
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No-medication order is append-only (Group 3: FR-048b)';
END$$

CREATE TRIGGER trg_no_medication_order_no_delete
BEFORE DELETE ON no_medication_order
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No-medication order cannot be deleted (Group 3)';
END$$

-- 7. prn_administration - Group 3 (BR-M07-04)
CREATE TRIGGER trg_prn_administration_no_update
BEFORE UPDATE ON prn_administration
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'PRN administration is append-only (Group 3: BR-M07-04)';
END$$

CREATE TRIGGER trg_prn_administration_no_delete
BEFORE DELETE ON prn_administration
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'PRN administration cannot be deleted (Group 3)';
END$$

-- 8. family_medication_movement - Group 3
CREATE TRIGGER trg_family_medication_movement_no_update
BEFORE UPDATE ON family_medication_movement
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Family medication movement is append-only (Group 3)';
END$$

CREATE TRIGGER trg_family_medication_movement_no_delete
BEFORE DELETE ON family_medication_movement
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Family medication movement cannot be deleted (Group 3)';
END$$

-- 9. supplied_medication_intake - Group 3 (FR-054, BR-M07-24)
CREATE TRIGGER trg_supplied_medication_intake_no_update
BEFORE UPDATE ON supplied_medication_intake
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Supplied medication intake is append-only (Group 3: FR-054)';
END$$

CREATE TRIGGER trg_supplied_medication_intake_no_delete
BEFORE DELETE ON supplied_medication_intake
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Supplied medication intake cannot be deleted (Group 3)';
END$$

DELIMITER ;
