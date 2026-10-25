-- T023: phân bổ, khóa theo người và dấu nhắc; DBR-09/FR-017/FR-019/FR-026.
CREATE TABLE bed_allocation (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 resident_id BIGINT NOT NULL, bed_id BIGINT NOT NULL,
 status ENUM('FUTURE','ACTIVE','CLOSED','CANCELLED') NOT NULL,
 start_at DATETIME(6) NOT NULL, planned_start_at DATETIME(6) NULL,
 end_at DATETIME(6) NULL, expected_end DATETIME(6) NULL,
 effective_end DATETIME(6) GENERATED ALWAYS AS (COALESCE(end_at,expected_end,CAST('9999-12-31' AS DATETIME))) STORED,
 start_reason VARCHAR(2000) NULL, end_reason VARCHAR(2000) NULL, cancellation_reason VARCHAR(2000) NULL,
 transfer_reason_id BIGINT NULL, opened_by_id BIGINT NULL, closed_by_id BIGINT NULL,
 basis_type VARCHAR(40) NULL, basis_id BIGINT NULL, previous_allocation_id BIGINT NULL,
 checked_care_level ENUM('BASIC','FREQUENT','SPECIAL') NOT NULL,
 care_level_source ENUM('CURRENT','CONTRACT','REGISTRATION') NOT NULL,
 prescribing_doctor_id BIGINT NULL, backfilled BOOLEAN NOT NULL DEFAULT FALSE,
 actual_recorded_at DATETIME(6) NULL, planned_at DATETIME(6) NULL, executed_late BOOLEAN NOT NULL DEFAULT FALSE,
 waiting_for_bed_ready BOOLEAN NOT NULL DEFAULT FALSE, room_unsuitable BOOLEAN NOT NULL DEFAULT FALSE,
 awaiting_request BOOLEAN NOT NULL DEFAULT FALSE, awaiting_same_price BOOLEAN NOT NULL DEFAULT FALSE,
 awaiting_doctor_confirmation BOOLEAN NOT NULL DEFAULT FALSE,
 confirming_doctor_id BIGINT NULL, doctor_confirmed_at DATETIME(6) NULL, doctor_confirmation_reason VARCHAR(2000) NULL,
 notified_planned_start_at DATETIME(6) NULL,
 active_bed_id BIGINT GENERATED ALWAYS AS (IF(status='ACTIVE',bed_id,NULL)) STORED,
 active_resident_id BIGINT GENERATED ALWAYS AS (IF(status='ACTIVE',resident_id,NULL)) STORED,
 future_resident_id BIGINT GENERATED ALWAYS AS (IF(status='FUTURE',resident_id,NULL)) STORED,
 UNIQUE KEY uq_allocation_active_bed(active_bed_id),
 UNIQUE KEY uq_allocation_active_resident(active_resident_id),
 UNIQUE KEY uq_allocation_future_resident(future_resident_id),
 KEY idx_allocation_bed_start(bed_id,start_at), KEY idx_allocation_resident_start(resident_id,start_at),
 KEY idx_allocation_status_start(status,start_at),
 CONSTRAINT fk_allocation_bed FOREIGN KEY(bed_id) REFERENCES bed(id) ON DELETE RESTRICT,
 CONSTRAINT fk_allocation_transfer_reason FOREIGN KEY(transfer_reason_id) REFERENCES bed_transfer_reason(id) ON DELETE RESTRICT,
 CONSTRAINT fk_allocation_previous FOREIGN KEY(previous_allocation_id) REFERENCES bed_allocation(id) ON DELETE RESTRICT,
 CONSTRAINT chk_allocation_closed_end CHECK((status='CLOSED')=(end_at IS NOT NULL)),
 CONSTRAINT chk_allocation_end CHECK(end_at IS NULL OR end_at>start_at),
 CONSTRAINT chk_allocation_expected_end CHECK(expected_end IS NULL OR expected_end>start_at),
 CONSTRAINT chk_allocation_doctor CHECK(prescribing_doctor_id IS NULL OR transfer_reason_id IS NOT NULL),
 CONSTRAINT chk_allocation_cancelled_reason CHECK((status='CANCELLED')=(cancellation_reason IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE bed ADD CONSTRAINT fk_bed_last_allocation FOREIGN KEY(last_closed_allocation_id) REFERENCES bed_allocation(id) ON DELETE RESTRICT;

CREATE TABLE resident_position (
 resident_id BIGINT NOT NULL PRIMARY KEY, without_bed_since DATETIME(6) NULL, version INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE allocation_reminder (
 target_type VARCHAR(40) NOT NULL, target_id BIGINT NOT NULL,
 flag_type ENUM('WITHOUT_BED','AWAITING_REQUEST','AWAITING_SAME_PRICE','ROOM_UNSUITABLE') NOT NULL,
 started_at DATETIME(6) NOT NULL, last_reminded_at DATETIME(6) NULL, reminder_count INT NOT NULL DEFAULT 0,
 PRIMARY KEY(target_type,target_id,flag_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DELIMITER $$
CREATE TRIGGER trg_bed_allocation_no_overlap_insert BEFORE INSERT ON bed_allocation FOR EACH ROW
BEGIN
 IF NEW.status<>'CANCELLED' AND EXISTS (
   SELECT 1 FROM bed_allocation a WHERE a.bed_id=NEW.bed_id AND a.status<>'CANCELLED'
    AND a.start_at<COALESCE(NEW.end_at,NEW.expected_end,'9999-12-31')
    AND NEW.start_at<a.effective_end
 ) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ALLOCATION_TIME_OVERLAP'; END IF;
END$$
CREATE TRIGGER trg_bed_allocation_guard_update BEFORE UPDATE ON bed_allocation FOR EACH ROW
BEGIN
 IF OLD.status IN ('CLOSED','CANCELLED') THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Closed or cancelled allocation is immutable (FR-026)';
 END IF;
 IF NEW.status<>'CANCELLED' AND EXISTS (
   SELECT 1 FROM bed_allocation a WHERE a.id<>OLD.id AND a.bed_id=NEW.bed_id AND a.status<>'CANCELLED'
    AND a.start_at<COALESCE(NEW.end_at,NEW.expected_end,'9999-12-31')
    AND NEW.start_at<a.effective_end
 ) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ALLOCATION_TIME_OVERLAP'; END IF;
END$$
CREATE TRIGGER trg_bed_allocation_no_delete BEFORE DELETE ON bed_allocation FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Allocation is removed only by retention';
 END IF;
END$$
DELIMITER ;

-- Phân biệt JSON null với trường không có để đính chính được mốc kết thúc thành NULL.
CREATE VIEW v_current_allocation AS
SELECT a.id,a.resident_id,
 CASE WHEN JSON_CONTAINS_PATH(c.corrected_content,'one','$.bedId')
   THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(c.corrected_content,'$.bedId')) AS UNSIGNED) ELSE a.bed_id END AS bed_id,
 a.status,
 CASE WHEN JSON_CONTAINS_PATH(c.corrected_content,'one','$.startAt')
   THEN CAST(REPLACE(REPLACE(JSON_UNQUOTE(JSON_EXTRACT(c.corrected_content,'$.startAt')),'T',' '),'Z','') AS DATETIME(6)) ELSE a.start_at END AS start_at,
 a.planned_start_at,
 CASE WHEN JSON_CONTAINS_PATH(c.corrected_content,'one','$.endAt')
   THEN CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(c.corrected_content,'$.endAt')),'null'),'T',' '),'Z','') AS DATETIME(6)) ELSE a.end_at END AS end_at,
 a.expected_end,a.start_reason,a.end_reason,a.cancellation_reason,a.transfer_reason_id,
 a.opened_by_id,a.closed_by_id,a.basis_type,a.basis_id,a.previous_allocation_id,
 a.checked_care_level,a.care_level_source,a.prescribing_doctor_id,a.backfilled,a.actual_recorded_at,
 a.planned_at,a.executed_late,a.waiting_for_bed_ready,a.room_unsuitable,a.awaiting_request,a.awaiting_same_price,
 a.awaiting_doctor_confirmation,a.confirming_doctor_id,a.doctor_confirmed_at,a.doctor_confirmation_reason,
 a.notified_planned_start_at,
 COALESCE(CASE WHEN JSON_CONTAINS_PATH(c.corrected_content,'one','$.endAt')
   THEN CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(c.corrected_content,'$.endAt')),'null'),'T',' '),'Z','') AS DATETIME(6)) ELSE a.end_at END,
   a.expected_end,CAST('9999-12-31' AS DATETIME)) AS effective_end,
 IF(a.status='ACTIVE',CASE WHEN JSON_CONTAINS_PATH(c.corrected_content,'one','$.bedId')
   THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(c.corrected_content,'$.bedId')) AS UNSIGNED) ELSE a.bed_id END,NULL) AS active_bed_id,
 a.active_resident_id,a.future_resident_id,
 r.id AS room_id,r.floor_id,f.area_id,(c.correction_id IS NOT NULL) AS corrected
FROM bed_allocation a
LEFT JOIN v_latest_correction c ON c.record_type='M03.BED_ALLOCATION' AND c.original_record_id=a.id
JOIN bed b ON b.id=CASE WHEN JSON_CONTAINS_PATH(c.corrected_content,'one','$.bedId')
 THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(c.corrected_content,'$.bedId')) AS UNSIGNED) ELSE a.bed_id END
JOIN room r ON r.id=b.room_id JOIN floor f ON f.id=r.floor_id
WHERE NOT EXISTS(SELECT 1 FROM v_voided_record v WHERE v.record_type='M03.BED_ALLOCATION' AND v.original_record_id=a.id);

GRANT INSERT,UPDATE ON bed_allocation TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE ON resident_position TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON allocation_reminder TO 'eldercare_app'@'%';
GRANT DELETE ON bed_allocation TO 'eldercare_retention'@'%';
GRANT DELETE ON allocation_reminder TO 'eldercare_retention'@'%';
GRANT DELETE ON resident_position TO 'eldercare_retention'@'%';
