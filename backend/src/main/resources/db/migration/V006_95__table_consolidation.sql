-- 006 T069: gộp bảng theo research R-15 của 000 và data-model 006 mục 0.
-- Phần nền chuyển danh mục thuốc, trạng thái thuốc theo người cao tuổi và checkpoint job sang bảng dùng chung.

-- F-03, DBR-25: giữ thời điểm UTC của TIMESTAMP cũ khi chuyển sang DATETIME(6).
SET @m07_previous_time_zone = @@SESSION.time_zone;
SET time_zone = '+00:00';

ALTER TABLE medication_absence_interval
    MODIFY COLUMN from_time DATETIME(6) NOT NULL,
    MODIFY COLUMN expected_to_time DATETIME(6) NULL,
    MODIFY COLUMN actual_to_time DATETIME(6) NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE medication_handover
    MODIFY COLUMN handover_time DATETIME(6) NOT NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE medication_handover_dose
    MODIFY COLUMN reverted_at DATETIME(6) NULL;
ALTER TABLE medication_return
    MODIFY COLUMN return_time DATETIME(6) NOT NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE no_medication_order
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE prn_administration
    MODIFY COLUMN administered_at DATETIME(6) NOT NULL,
    MODIFY COLUMN offline_device_time DATETIME(6) NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE family_medication_batch
    MODIFY COLUMN received_at DATETIME(6) NOT NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
ALTER TABLE supplied_medication_intake
    MODIFY COLUMN received_at DATETIME(6) NOT NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Hoạt chất của đơn là ảnh chụp tại lúc lập đơn, không còn bảng con riêng.
ALTER TABLE prescription
    ADD COLUMN active_ingredient_ids JSON NULL AFTER medication_id;

UPDATE prescription p
SET p.active_ingredient_ids = COALESCE(
        (SELECT JSON_ARRAYAGG(pai.allergen_id)
         FROM prescription_active_ingredient pai
         WHERE pai.prescription_id = p.id),
        JSON_ARRAY());

ALTER TABLE prescription
    MODIFY COLUMN active_ingredient_ids JSON NOT NULL;

-- Dòng đối chiếu và liên kết đơn của lô là thành phần của bản ghi cha, không còn vòng đời độc lập.
-- Gỡ trigger cũ trước khi chép cả phiếu đã xác nhận/hủy; tạo lại bảo vệ theo dòng sau khi chép xong.
DROP TRIGGER trg_reconciliation_terminal_lock;
DROP TRIGGER trg_reconciliation_no_delete;

ALTER TABLE medication_reconciliation
    ADD COLUMN `lines` JSON NULL AFTER cancel_reason;

UPDATE medication_reconciliation r
SET r.`lines` = COALESCE(
        (SELECT JSON_ARRAYAGG(JSON_OBJECT(
                'id', i.id,
                'reconciliationId', i.reconciliation_id,
                'itemOrder', i.item_order,
                'source', i.source,
                'prescriptionId', i.prescription_id,
                'admissionMedicationItemId', i.admission_medication_item_id,
                'medicationDescription', i.medication_description,
                'decision', i.decision,
                'reason', i.reason,
                'newPrescriptionJson', CAST(i.new_prescription_json AS CHAR CHARACTER SET utf8mb4),
                'createdById', i.created_by_id,
                'createdAt', DATE_FORMAT(i.created_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                'discontinuedOutsideTicket', IF(i.discontinued_outside_ticket, TRUE, FALSE),
                'createdPrescriptionId', i.created_prescription_id))
         FROM reconciliation_item i
         WHERE i.reconciliation_id = r.id),
        JSON_ARRAY());

ALTER TABLE medication_reconciliation
    MODIFY COLUMN `lines` JSON NOT NULL;

DROP TRIGGER trg_family_medication_batch_guard;

ALTER TABLE family_medication_batch
    ADD COLUMN prescription_bindings JSON NULL AFTER file_ids;

UPDATE family_medication_batch b
SET b.prescription_bindings = COALESCE(
        (SELECT JSON_ARRAYAGG(JSON_OBJECT(
                'id', p.id,
                'batchId', p.batch_id,
                'prescriptionId', p.prescription_id,
                'boundFrom', DATE_FORMAT(p.bound_from, '%Y-%m-%dT%H:%i:%s.%fZ'),
                'boundTo', DATE_FORMAT(p.bound_to, '%Y-%m-%dT%H:%i:%s.%fZ')))
         FROM family_medication_prescription p
         WHERE p.batch_id = b.id),
        JSON_ARRAY());

ALTER TABLE family_medication_batch
    MODIFY COLUMN prescription_bindings JSON NOT NULL,
    ADD INDEX idx_family_medication_prescription_bindings
        ((CAST(prescription_bindings->'$[*].prescriptionId' AS UNSIGNED ARRAY)));

DELIMITER $$
CREATE TRIGGER trg_family_medication_batch_guard BEFORE UPDATE ON family_medication_batch FOR EACH ROW
BEGIN
    IF OLD.status IN ('DEPLETED','RETURNED','DISPOSED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='family medication batch is closed (FR-042)';
    END IF;
    IF NOT(OLD.initial_quantity<=>NEW.initial_quantity) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='initial quantity of family medication batch is fixed (FR-045, Q-393)';
    END IF;
END$$
DELIMITER ;

ALTER TABLE medication_reconciliation
    ADD COLUMN locked_at DATETIME(6) NULL AFTER confirmed_at;
UPDATE medication_reconciliation
SET locked_at = confirmed_at
WHERE status = 'CONFIRMED';

ALTER TABLE medication_reconciliation
    ADD CONSTRAINT chk_reconciliation_confirmed_lock
        CHECK ((status = 'CONFIRMED') = (locked_at IS NOT NULL));

ALTER TABLE medication_handover
    ADD COLUMN locked_at DATETIME(6) NULL AFTER created_at;
UPDATE medication_handover
SET locked_at = handover_time
WHERE status = 'RETURNED';

ALTER TABLE medication_handover
    ADD CONSTRAINT chk_handover_returned_lock
        CHECK ((status = 'RETURNED') = (locked_at IS NOT NULL));

-- catalog_item có khóa tên đang hiệu lực theo loại. Tên lưu ở catalog_item gồm đủ bộ nhận dạng cũ;
-- tên hiển thị nguyên bản nằm trong attributes.name để giữ API 006 không đổi.
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes, version)
SELECT 'M07.MEDICATION',
       'M07',
       CONCAT('M07-', SUBSTRING(SHA2(LOWER(CONCAT_WS('|', m.name, m.concentration, m.dosage_form)), 256), 1, 35)),
       CONCAT(m.name, ' | ', m.concentration, ' | ', m.dosage_form),
       0,
       m.status,
       JSON_OBJECT(
               'name', m.name,
               'concentration', m.concentration,
               'dosageForm', m.dosage_form,
               'defaultRoute', m.default_route,
               'usageUnit', m.usage_unit,
               'beforeWindowMinutes', m.before_window_minutes,
               'afterWindowMinutes', m.after_window_minutes,
               'specialControl', IF(m.special_control, CAST('true' AS JSON), CAST('false' AS JSON)),
               'activeIngredientIds', COALESCE(
                       (SELECT JSON_ARRAYAGG(mai.allergen_id)
                        FROM medication_active_ingredient mai
                        WHERE mai.medication_id = m.id),
                       JSON_ARRAY()),
               'legacy', JSON_OBJECT('table', 'medication', 'id', m.id)),
       m.version
FROM medication m
ORDER BY m.id;

-- Bỏ khóa ngoại cũ trước khi thay id thuốc bằng id catalog_item mới.
DROP TRIGGER trg_prescription_lock;

ALTER TABLE prescription
    DROP FOREIGN KEY fk_prescription_medication;
ALTER TABLE medication_dose
    DROP FOREIGN KEY fk_medication_dose_medication;

UPDATE prescription p
    JOIN catalog_item c
      ON c.catalog_type = 'M07.MEDICATION'
     AND CAST(JSON_UNQUOTE(c.attributes->'$.legacy.id') AS UNSIGNED) = p.medication_id
SET p.medication_id = c.id;

UPDATE medication_dose d
    JOIN catalog_item c
      ON c.catalog_type = 'M07.MEDICATION'
     AND CAST(JSON_UNQUOTE(c.attributes->'$.legacy.id') AS UNSIGNED) = d.medication_id
SET d.medication_id = c.id;

UPDATE family_medication_batch b
    JOIN catalog_item c
      ON c.catalog_type = 'M07.MEDICATION'
     AND CAST(JSON_UNQUOTE(c.attributes->'$.legacy.id') AS UNSIGNED) = b.medication_id
SET b.medication_id = c.id
WHERE b.medication_id IS NOT NULL;

UPDATE supplied_medication_intake i
    JOIN catalog_item c
      ON c.catalog_type = 'M07.MEDICATION'
     AND CAST(JSON_UNQUOTE(c.attributes->'$.legacy.id') AS UNSIGNED) = i.medication_id
SET i.medication_id = c.id;

ALTER TABLE prescription
    ADD CONSTRAINT fk_prescription_catalog_medication
        FOREIGN KEY (medication_id) REFERENCES catalog_item (id) ON DELETE RESTRICT;
ALTER TABLE medication_dose
    ADD CONSTRAINT fk_medication_dose_catalog_medication
        FOREIGN KEY (medication_id) REFERENCES catalog_item (id) ON DELETE RESTRICT;

DROP TRIGGER trg_medication_handover_no_update;
DROP TRIGGER trg_medication_handover_no_delete;

DELIMITER $$
CREATE TRIGGER trg_medication_reconciliation_row_lock
    BEFORE UPDATE ON medication_reconciliation
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL OR OLD.status = 'CANCELLED' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Medication reconciliation row is locked';
    END IF;
END$$

CREATE TRIGGER trg_medication_reconciliation_no_delete
    BEFORE DELETE ON medication_reconciliation
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Medication reconciliation is removed only by retention';
    END IF;
END$$

CREATE TRIGGER trg_medication_handover_row_lock
    BEFORE UPDATE ON medication_handover
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Medication handover row is locked';
    END IF;
    IF NOT (OLD.resident_id <=> NEW.resident_id)
       OR NOT (OLD.absence_interval_id <=> NEW.absence_interval_id)
       OR NOT (OLD.trip_id <=> NEW.trip_id)
       OR NOT (OLD.recipient_type <=> NEW.recipient_type)
       OR NOT (OLD.recipient_id <=> NEW.recipient_id)
       OR NOT (OLD.recipient_name <=> NEW.recipient_name)
       OR NOT (OLD.relationship <=> NEW.relationship)
       OR NOT (OLD.dispensed_by_id <=> NEW.dispensed_by_id)
       OR NOT (OLD.handover_time <=> NEW.handover_time)
       OR NOT (OLD.created_at <=> NEW.created_at) THEN
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

CREATE TRIGGER trg_medication_handover_no_delete
    BEFORE DELETE ON medication_handover
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Medication handover is removed only by retention';
    END IF;
END$$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_prescription_lock
    BEFORE UPDATE
    ON prescription
    FOR EACH ROW
BEGIN
    IF OLD.status IN ('DISCONTINUED', 'EXPIRED') AND NEW.status <> OLD.status THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Final status of prescription cannot be modified (FR-013, DBR-14)';
    END IF;

    IF NOT (NEW.medication_id <=> OLD.medication_id)
       OR NOT (NEW.active_ingredient_ids <=> OLD.active_ingredient_ids)
       OR NOT (NEW.dosage <=> OLD.dosage)
       OR NOT (NEW.dosage_unit <=> OLD.dosage_unit)
       OR NOT (NEW.route <=> OLD.route)
       OR NOT (NEW.prescription_type <=> OLD.prescription_type)
       OR NOT (NEW.frequency_type <=> OLD.frequency_type)
       OR NOT (NEW.timed_slots <=> OLD.timed_slots)
       OR NOT (NEW.every_n_hours <=> OLD.every_n_hours)
       OR NOT (NEW.first_slot <=> OLD.first_slot)
       OR NOT (NEW.days_of_week <=> OLD.days_of_week)
       OR NOT (NEW.min_interval_minutes <=> OLD.min_interval_minutes)
       OR NOT (NEW.max_doses_per_24h <=> OLD.max_doses_per_24h)
       OR NOT (NEW.medication_source <=> OLD.medication_source)
       OR NOT (NEW.prescriber_type <=> OLD.prescriber_type)
       OR NOT (NEW.facility_name <=> OLD.facility_name)
       OR NOT (NEW.prescriber_name <=> OLD.prescriber_name)
       OR NOT (NEW.prescriber_id <=> OLD.prescriber_id)
       OR NOT (NEW.prescribed_at <=> OLD.prescribed_at)
       OR NOT (NEW.prescription_file_id <=> OLD.prescription_file_id)
       OR NOT (NEW.replaces_id <=> OLD.replaces_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Fixed fields of saved prescription cannot be modified (FR-014, DBR-14)';
    END IF;

    IF NOT (NEW.started_at <=> OLD.started_at) AND NOT (OLD.pending_admission = 1 AND NEW.pending_admission = 0) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Prescription start time cannot be modified unless admission is completed (FR-034a)';
    END IF;
END$$
DELIMITER ;

-- Trạng thái theo người cao tuổi của M07 dùng resident_module_state.
INSERT INTO resident_module_state (resident_id, module, data, version, updated_at)
SELECT resident_id,
       'M07',
       JSON_OBJECT(
               'reconciliationDueAt', DATE_FORMAT(reconciliation_due_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
               'reconciliationReason', reconciliation_reason,
               'reconciliationWarnedAt', DATE_FORMAT(reconciliation_warned_at, '%Y-%m-%dT%H:%i:%s.%fZ')),
       version,
       UTC_TIMESTAMP(6)
FROM resident_medication;

ALTER TABLE resident_module_state
    ADD COLUMN m07_reconciliation_due_at DATETIME(6)
        GENERATED ALWAYS AS (
            IF(module = 'M07',
               CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(data->'$.reconciliationDueAt'), 'null'), 'T', ' '), 'Z', '') AS DATETIME(6)),
               NULL)
        ) STORED,
    ADD INDEX idx_resident_module_state_m07_due (m07_reconciliation_due_at);

-- Checkpoint job dùng reminder_marker; kết quả lần chạy nằm trong details, lần chạy kỹ thuật đã ghi jobs.log.
INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at, details)
SELECT CONCAT('M07.JOB.', job_key),
       'M07.JOB',
       0,
       checkpoint,
       COALESCE(completed_at, UTC_TIMESTAMP(6)),
       JSON_OBJECT('result', result)
FROM m07_job_run;

-- Gỡ quyền trước khi bỏ bảng cũ.
REVOKE INSERT ON prescription_active_ingredient FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE, DELETE ON reconciliation_item FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON family_medication_prescription FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE, DELETE ON medication_active_ingredient FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE, DELETE ON medication FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON resident_medication FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON m07_job_run FROM 'eldercare_app'@'%';

DROP TABLE prescription_active_ingredient;
DROP TABLE reconciliation_item;
DROP TABLE family_medication_prescription;
DROP TABLE medication_active_ingredient;
DROP TABLE medication;
DROP TABLE resident_medication;
DROP TABLE m07_job_run;

-- Sáu bảng đích cuối cùng của feature 006. Các phần gộp còn lại của T069 bổ sung tiếp trong migration này.
ALTER TABLE prescription COMMENT = 'DataGroup=BUSINESS';
ALTER TABLE medication_dose COMMENT = 'DataGroup=BUSINESS';
ALTER TABLE dose_result COMMENT = 'DataGroup=RECORD';
ALTER TABLE medication_reconciliation COMMENT = 'DataGroup=MULTIPLE';
ALTER TABLE family_medication_batch COMMENT = 'DataGroup=BUSINESS';
ALTER TABLE medication_handover COMMENT = 'DataGroup=MULTIPLE';

SET time_zone = @m07_previous_time_zone;
