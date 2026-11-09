-- 006 T069 (R-15 của 000, data-model 006 mục 0.1): chuyển năm bảng nhỏ của M07 sang bảng dùng chung của 000.
-- record_event: prescription_issue (M07.PRESCRIPTION_ISSUE), prescription_basis (M07.PRESCRIPTION_BASIS), đối tượng
-- M07.PRESCRIPTION. simple_record: dose_review (M07.DOSE_REVIEW), medication_absence_interval (M07.ABSENCE_INTERVAL),
-- reconciliation_correction_request (M07.RECONCILIATION_CORRECTION_REQUEST). Mọi dòng cấp id mới của bảng chung, ghi
-- content.legacy = {table, id}; năm loại không là đích của nhật ký, đính chính, tệp hay thông báo (plan 006 mục "T069:
-- id mới sau gộp"). Bảng cũ không ghi người tạo cho dose_review, khoảng vắng: created_by = 0 (không rõ).

SET @m07_previous_time_zone = @@SESSION.time_zone;
SET time_zone = '+00:00';

INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system, occurred_at,
                          confirmed_at, unique_key)
SELECT 'M07.PRESCRIPTION',
       i.prescription_id,
       'M07',
       'M07.PRESCRIPTION_ISSUE',
       JSON_OBJECT('prescriptionId', i.prescription_id,
                   'reconciliationLineId', i.reconciliation_line_id,
                   'lineNumber', i.line_number,
                   'issueType', i.issue_type,
                   'issueCode', i.issue_code,
                   'duplicatePrescriptionId', i.duplicate_prescription_id,
                   'allergyItemId', i.allergy_item_id,
                   'severity', i.severity,
                   'activeIngredient', i.active_ingredient,
                   'manualReviewRequired', i.manual_review_required IS TRUE,
                   'confirmationReason', i.confirmation_reason,
                   'confirmedById', i.confirmed_by_id,
                   'occurredAt', DATE_FORMAT(i.occurred_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'legacy', JSON_OBJECT('table', 'prescription_issue', 'id', i.id)),
       i.confirmed_by_id,
       FALSE,
       i.occurred_at,
       i.occurred_at,
       NULL
FROM prescription_issue i
ORDER BY i.id;

INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system, occurred_at,
                          confirmed_at, unique_key)
SELECT 'M07.PRESCRIPTION',
       b.prescription_id,
       'M07',
       'M07.PRESCRIPTION_BASIS',
       JSON_OBJECT('prescriptionId', b.prescription_id,
                   'command', b.command,
                   'facilityName', b.facility_name,
                   'prescriberName', b.prescriber_name,
                   'prescribedDate', DATE_FORMAT(b.prescribed_date, '%Y-%m-%d'),
                   'basisType', b.basis_type,
                   'callerName', b.caller_name,
                   'receiverName', b.receiver_name,
                   'calledAt', DATE_FORMAT(b.called_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'documentFileId', b.document_file_id,
                   'recorderId', b.recorder_id,
                   'recordedAt', DATE_FORMAT(b.recorded_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'legacy', JSON_OBJECT('table', 'prescription_basis', 'id', b.id)),
       b.recorder_id,
       FALSE,
       b.recorded_at,
       b.recorded_at,
       NULL
FROM prescription_basis b
ORDER BY b.id;

INSERT INTO simple_record (kind, module, resident_id, entity_type, entity_id, status, content, unique_key, created_at,
                           created_by, version)
SELECT 'M07.DOSE_REVIEW',
       'M07',
       NULL,
       CASE
           WHEN r.dose_result_id IS NOT NULL THEN 'M07.DOSE_RESULT'
           WHEN r.prn_usage_id IS NOT NULL THEN 'M07.PRN_ADMINISTRATION'
       END,
       COALESCE(r.dose_result_id, r.prn_usage_id),
       r.status,
       JSON_OBJECT('doseResultId', r.dose_result_id,
                   'prnUsageId', r.prn_usage_id,
                   'floorAreaId', r.floor_area_id,
                   'reviewerCandidate', CAST(r.reviewer_candidate AS CHAR),
                   'reviewerId', r.reviewer_id,
                   'reviewedAt', DATE_FORMAT(r.reviewed_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'note', r.note,
                   'legacy', JSON_OBJECT('table', 'dose_review', 'id', r.id)),
       IF(r.dose_result_id IS NULL, NULL, CONCAT('M07.DR:', r.dose_result_id)),
       r.created_at,
       0,
       r.version
FROM dose_review r
ORDER BY r.id;

INSERT INTO simple_record (kind, module, resident_id, entity_type, entity_id, status, content, unique_key, created_at,
                           created_by)
SELECT 'M07.ABSENCE_INTERVAL',
       'M07',
       a.resident_id,
       'M01.RESIDENT',
       a.resident_id,
       IF(a.actual_to_time IS NULL, 'OPEN', 'CLOSED'),
       JSON_OBJECT('absenceType', a.absence_type,
                   'fromTime', DATE_FORMAT(a.from_time, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'expectedToTime', DATE_FORMAT(a.expected_to_time, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'actualToTime', DATE_FORMAT(a.actual_to_time, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'absenceSessionId', a.absence_session_id,
                   'tripId', a.trip_id,
                   'noMedicationOrderId', a.no_medication_order_id,
                   'legacy', JSON_OBJECT('table', 'medication_absence_interval', 'id', a.id)),
       IF(a.actual_to_time IS NULL, CONCAT('M07.AI:', a.resident_id), NULL),
       a.created_at,
       0
FROM medication_absence_interval a
ORDER BY a.id;

INSERT INTO simple_record (kind, module, resident_id, entity_type, entity_id, status, content, created_at, created_by)
SELECT 'M07.RECONCILIATION_CORRECTION_REQUEST',
       'M07',
       NULL,
       'M07.RECONCILIATION',
       c.reconciliation_id,
       c.status,
       JSON_OBJECT('reconciliationId', c.reconciliation_id,
                   'correctedContentJson', CAST(c.corrected_content_json AS CHAR),
                   'reason', c.reason,
                   'createdById', c.created_by_id,
                   'confirmedById', c.confirmed_by_id,
                   'confirmedAt', DATE_FORMAT(c.confirmed_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'correctionId', c.correction_id,
                   'returnReason', c.return_reason,
                   'legacy', JSON_OBJECT('table', 'reconciliation_correction_request', 'id', c.id)),
       c.created_at,
       c.created_by_id
FROM reconciliation_correction_request c
ORDER BY c.id;

-- Lần giao trỏ khoảng vắng theo id mới; gỡ khóa dòng để đổi cả lần giao đã nhận lại, tạo lại như V006_97.
DROP TRIGGER trg_medication_handover_row_lock;

UPDATE medication_handover h
    JOIN simple_record s ON s.kind = 'M07.ABSENCE_INTERVAL'
        AND JSON_UNQUOTE(s.content->'$.legacy.table') = 'medication_absence_interval'
        AND CAST(s.content->'$.legacy.id' AS UNSIGNED) = h.absence_interval_id
SET h.absence_interval_id = s.id
WHERE h.absence_interval_id IS NOT NULL;

ALTER TABLE medication_handover
    ADD CONSTRAINT fk_handover_absence_interval FOREIGN KEY (absence_interval_id) REFERENCES simple_record (id)
        ON DELETE RESTRICT;

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

DROP TABLE prescription_issue;
DROP TABLE prescription_basis;
DROP TABLE dose_review;
DROP TABLE medication_absence_interval;
DROP TABLE reconciliation_correction_request;

SET time_zone = @m07_previous_time_zone;
