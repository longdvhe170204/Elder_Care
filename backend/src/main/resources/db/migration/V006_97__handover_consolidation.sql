-- 006 T069 (R-15 của 000, data-model 006 mục 0.1): gộp medication_handover_item, medication_handover_dose,
-- medication_return, medication_return_item, no_medication_order, supplied_medication_intake vào medication_handover.
-- Lần giao (kind HANDOVER) giữ id; dòng thuốc giao, liều mang theo vào `lines` {items, doses}. Nhận lại, lệnh không mang
-- thuốc, tiếp nhận cung ứng cấp id mới sau id lớn nhất của cả bốn bảng gốc, ghi legacy = {table, id}; nhật ký, thông báo
-- 009 cũ trỏ id cũ không nối sang id mới (A chốt 2026-11-05, như V001_95). Ba loại này nhóm 3, khóa khi thêm.

-- Giữ thời điểm UTC của DATETIME(6) khi ghi vào JSON (như V006_95).
SET @m07_previous_time_zone = @@SESSION.time_zone;
SET time_zone = '+00:00';

-- Gỡ khóa dòng để chép `lines` vào cả lần giao đã nhận lại; tạo lại ở cuối.
DROP TRIGGER trg_medication_handover_row_lock;

ALTER TABLE medication_handover
    DROP CHECK chk_handover_returned_lock;

ALTER TABLE medication_handover
    ADD COLUMN kind ENUM ('HANDOVER', 'RETURN', 'NO_MEDICATION', 'SUPPLIED_INTAKE') NOT NULL DEFAULT 'HANDOVER' AFTER id,
    ADD COLUMN handover_id BIGINT NULL COMMENT 'Lần nhận lại trỏ lần giao' AFTER trip_id,
    MODIFY COLUMN recipient_type ENUM ('STAFF', 'TRIP_LEADER', 'RELATIVE') NULL,
    MODIFY COLUMN dispensed_by_id BIGINT NULL,
    MODIFY COLUMN handover_time DATETIME(6) NULL,
    MODIFY COLUMN status ENUM ('HANDED_OVER', 'WAITING_RETURN', 'RETURNED') NULL DEFAULT NULL,
    ADD COLUMN recorded_by_id BIGINT NULL COMMENT 'Người ghi nhận lại' AFTER status,
    ADD COLUMN return_time DATETIME(6) NULL AFTER recorded_by_id,
    ADD COLUMN reason VARCHAR(2000) NULL COMMENT 'Lý do nhận lại, không mang thuốc, tiếp nhận không đạt' AFTER return_time,
    ADD COLUMN created_by_id BIGINT NULL COMMENT 'Người lập lệnh không mang thuốc' AFTER reason,
    ADD COLUMN procurement_line_id BIGINT NULL AFTER created_by_id,
    ADD COLUMN procurement_request_id BIGINT NULL AFTER procurement_line_id,
    ADD COLUMN prescription_id BIGINT NULL AFTER procurement_request_id,
    ADD COLUMN medication_id BIGINT NULL AFTER prescription_id,
    ADD COLUMN medication_name VARCHAR(255) NULL AFTER medication_id,
    ADD COLUMN concentration VARCHAR(100) NULL AFTER medication_name,
    ADD COLUMN dosage_form VARCHAR(100) NULL AFTER concentration,
    ADD COLUMN quantity_received DECIMAL(10, 2) NULL AFTER dosage_form,
    ADD COLUMN unit VARCHAR(50) NULL AFTER quantity_received,
    ADD COLUMN expiry_date DATE NULL AFTER unit,
    ADD COLUMN item_condition VARCHAR(500) NULL AFTER expiry_date,
    ADD COLUMN intake_result ENUM ('PASSED', 'NOT_PASSED') NULL AFTER item_condition,
    ADD COLUMN discrepancy VARCHAR(2000) NULL AFTER intake_result,
    ADD COLUMN deliverer_name VARCHAR(255) NULL AFTER discrepancy,
    ADD COLUMN receiver_id BIGINT NULL AFTER deliverer_name,
    ADD COLUMN received_at DATETIME(6) NULL AFTER receiver_id,
    ADD COLUMN `lines` JSON NULL COMMENT 'HANDOVER {items, doses}; RETURN {items}' AFTER received_at,
    ADD COLUMN legacy JSON NULL COMMENT 'Bảng và id cũ của dòng chép sang (R-15)' AFTER locked_at;

UPDATE medication_handover h
SET h.`lines` = JSON_OBJECT(
        'items', COALESCE(
                (SELECT JSON_ARRAYAGG(JSON_OBJECT(
                        'id', i.id,
                        'handoverId', i.handover_id,
                        'prescriptionId', i.prescription_id,
                        'quantity', i.quantity,
                        'familyMedicationBatchId', i.family_medication_batch_id,
                        'medicationSource', i.medication_source))
                 FROM medication_handover_item i
                 WHERE i.handover_id = h.id),
                JSON_ARRAY()),
        'doses', COALESCE(
                (SELECT JSON_ARRAYAGG(JSON_OBJECT(
                        'id', d.id,
                        'handoverId', d.handover_id,
                        'doseId', d.dose_id,
                        'revertedAt', DATE_FORMAT(d.reverted_at, '%Y-%m-%dT%H:%i:%s.%fZ')))
                 FROM medication_handover_dose d
                 WHERE d.handover_id = h.id),
                JSON_ARRAY()));

SET @m07_handover_id_base = GREATEST(
        COALESCE((SELECT MAX(id) FROM medication_handover), 0),
        COALESCE((SELECT MAX(id) FROM medication_return), 0),
        COALESCE((SELECT MAX(id) FROM no_medication_order), 0),
        COALESCE((SELECT MAX(id) FROM supplied_medication_intake), 0));

INSERT INTO medication_handover (id, kind, resident_id, trip_id, handover_id, recorded_by_id, return_time, reason,
                                 created_at, locked_at, `lines`, legacy)
SELECT x.new_id,
       'RETURN',
       h.resident_id,
       h.trip_id,
       x.handover_id,
       x.recorded_by_id,
       x.return_time,
       x.reason,
       x.created_at,
       x.created_at,
       JSON_OBJECT('items', COALESCE(
               (SELECT JSON_ARRAYAGG(JSON_OBJECT(
                       'id', ri.id,
                       'returnId', x.new_id,
                       'prescriptionId', ri.prescription_id,
                       'quantity', ri.quantity))
                FROM medication_return_item ri
                WHERE ri.return_id = x.id),
               JSON_ARRAY())),
       JSON_OBJECT('table', 'medication_return', 'id', x.id)
FROM (SELECT r.*, @m07_handover_id_base + ROW_NUMBER() OVER (ORDER BY r.id) AS new_id FROM medication_return r) x
         JOIN medication_handover h ON h.id = x.handover_id;

SET @m07_handover_id_base = GREATEST(@m07_handover_id_base, COALESCE((SELECT MAX(id) FROM medication_handover), 0));

INSERT INTO medication_handover (id, kind, resident_id, trip_id, reason, created_by_id, created_at, locked_at, legacy)
SELECT @m07_handover_id_base + ROW_NUMBER() OVER (ORDER BY o.id),
       'NO_MEDICATION',
       o.resident_id,
       o.trip_id,
       o.reason,
       o.created_by_id,
       o.created_at,
       o.created_at,
       JSON_OBJECT('table', 'no_medication_order', 'id', o.id)
FROM no_medication_order o;

UPDATE medication_absence_interval a
    JOIN medication_handover m ON m.kind = 'NO_MEDICATION'
        AND JSON_UNQUOTE(m.legacy->'$.table') = 'no_medication_order'
        AND CAST(m.legacy->'$.id' AS UNSIGNED) = a.no_medication_order_id
SET a.no_medication_order_id = m.id
WHERE a.no_medication_order_id IS NOT NULL;

SET @m07_handover_id_base = GREATEST(@m07_handover_id_base, COALESCE((SELECT MAX(id) FROM medication_handover), 0));

INSERT INTO medication_handover (id, kind, resident_id, procurement_line_id, procurement_request_id, prescription_id,
                                 medication_id, medication_name, concentration, dosage_form, quantity_received, unit,
                                 expiry_date, item_condition, intake_result, discrepancy, reason, deliverer_name,
                                 receiver_id, received_at, created_at, locked_at, legacy)
SELECT @m07_handover_id_base + ROW_NUMBER() OVER (ORDER BY s.id),
       'SUPPLIED_INTAKE',
       s.resident_id,
       s.procurement_line_id,
       s.procurement_request_id,
       s.prescription_id,
       s.medication_id,
       s.medication_name,
       s.concentration,
       s.dosage_form,
       s.quantity_received,
       s.unit,
       s.expiry_date,
       s.item_condition,
       s.intake_result,
       s.discrepancy,
       s.reason,
       s.deliverer_name,
       s.receiver_id,
       s.received_at,
       s.created_at,
       s.created_at,
       JSON_OBJECT('table', 'supplied_medication_intake', 'id', s.id)
FROM supplied_medication_intake s;

DROP TABLE medication_handover_item;
DROP TABLE medication_handover_dose;
DROP TABLE medication_return_item;
DROP TABLE medication_return;
DROP TABLE no_medication_order;
DROP TABLE supplied_medication_intake;

ALTER TABLE medication_handover
    ADD COLUMN no_medication_key VARCHAR(50)
        GENERATED ALWAYS AS (IF(kind = 'NO_MEDICATION', CONCAT(resident_id, ':', trip_id), NULL)) VIRTUAL
        COMMENT 'Một lệnh không mang thuốc mỗi (người, chuyến), thay uq_resident_trip_no_med',
    ADD COLUMN return_key BIGINT
        GENERATED ALWAYS AS (IF(kind = 'RETURN', handover_id, NULL)) VIRTUAL
        COMMENT 'Một lần nhận lại mỗi lần giao',
    ADD CONSTRAINT uq_handover_no_medication_key UNIQUE (no_medication_key),
    ADD CONSTRAINT uq_handover_return_key UNIQUE (return_key),
    ADD CONSTRAINT fk_handover_return_handover FOREIGN KEY (handover_id) REFERENCES medication_handover (id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT chk_handover_returned_lock CHECK (kind <> 'HANDOVER' OR ((status = 'RETURNED') = (locked_at IS NOT NULL))),
    ADD CONSTRAINT chk_handover_record_lock CHECK (kind = 'HANDOVER' OR locked_at IS NOT NULL),
    ADD CONSTRAINT chk_handover_kind_handover CHECK (kind <> 'HANDOVER' OR (recipient_type IS NOT NULL
        AND dispensed_by_id IS NOT NULL AND handover_time IS NOT NULL AND status IS NOT NULL)),
    ADD CONSTRAINT chk_handover_kind_return CHECK (kind <> 'RETURN' OR (handover_id IS NOT NULL
        AND recorded_by_id IS NOT NULL AND return_time IS NOT NULL)),
    ADD CONSTRAINT chk_handover_kind_no_medication CHECK (kind <> 'NO_MEDICATION' OR (trip_id IS NOT NULL
        AND reason IS NOT NULL AND created_by_id IS NOT NULL)),
    ADD CONSTRAINT chk_handover_kind_intake CHECK (kind <> 'SUPPLIED_INTAKE' OR (procurement_line_id IS NOT NULL
        AND procurement_request_id IS NOT NULL AND prescription_id IS NOT NULL AND medication_id IS NOT NULL
        AND medication_name IS NOT NULL AND concentration IS NOT NULL AND dosage_form IS NOT NULL
        AND quantity_received > 0 AND unit IS NOT NULL AND expiry_date IS NOT NULL AND item_condition IS NOT NULL
        AND intake_result IS NOT NULL AND deliverer_name IS NOT NULL AND receiver_id IS NOT NULL
        AND received_at IS NOT NULL)),
    ADD CONSTRAINT chk_handover_intake_not_passed CHECK (kind <> 'SUPPLIED_INTAKE' OR intake_result = 'PASSED'
        OR (discrepancy IS NOT NULL AND reason IS NOT NULL)),
    ADD INDEX idx_handover_kind_resident_created (kind, resident_id, created_at),
    ADD INDEX idx_handover_trip (trip_id),
    ADD INDEX idx_handover_procurement_request (procurement_request_id),
    ADD INDEX idx_handover_procurement_line (procurement_line_id),
    ADD INDEX idx_handover_dose_ids ((CAST(`lines`->'$.doses[*].doseId' AS UNSIGNED ARRAY)));

DELIMITER $$
CREATE TRIGGER trg_medication_handover_row_lock
    BEFORE UPDATE ON medication_handover
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Medication handover row is locked';
    END IF;
    IF NOT (OLD.kind <=> NEW.kind)
       OR NOT (OLD.resident_id <=> NEW.resident_id)
       OR NOT (OLD.absence_interval_id <=> NEW.absence_interval_id)
       OR NOT (OLD.trip_id <=> NEW.trip_id)
       OR NOT (OLD.recipient_type <=> NEW.recipient_type)
       OR NOT (OLD.recipient_id <=> NEW.recipient_id)
       OR NOT (OLD.recipient_name <=> NEW.recipient_name)
       OR NOT (OLD.relationship <=> NEW.relationship)
       OR NOT (OLD.dispensed_by_id <=> NEW.dispensed_by_id)
       OR NOT (OLD.handover_time <=> NEW.handover_time)
       OR NOT (OLD.created_at <=> NEW.created_at)
       OR NOT (OLD.legacy <=> NEW.legacy) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Medication handover fixed fields cannot be modified';
    END IF;
    IF NOT (OLD.status <=> NEW.status
       OR (OLD.status = 'HANDED_OVER' AND NEW.status IN ('WAITING_RETURN', 'RETURNED'))
       OR (OLD.status = 'WAITING_RETURN' AND NEW.status = 'RETURNED')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid medication handover status transition (FR-048a)';
    END IF;
    IF (NEW.status = 'RETURNED') <> (NEW.locked_at IS NOT NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Returned medication handover must be locked';
    END IF;
END$$
DELIMITER ;

SET time_zone = @m07_previous_time_zone;
