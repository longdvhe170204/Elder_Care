-- T020/R-15: công việc là nhóm 2; lịch sử dùng status_history, không tạo bảng lịch sử mới.
CREATE TABLE care_task (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 resident_id BIGINT NULL,
 location_type ENUM('ROOM','BED','FLOOR','AREA') NULL,
 location_id BIGINT NULL,
 task_type_id BIGINT NOT NULL,
 basis_type ENUM('CARE_PLAN_ITEM','MEASUREMENT','MEAL','ACTIVITY_SESSION','POST_FALL_MONITORING','CONTACT_MONITORING','WATER_INTAKE_ALERT','SHIFT_START_CHECK','SUPERVISION','AD_HOC','AD_HOC_RECORDING','REWORK') NOT NULL,
 basis_id BIGINT NULL,
 plan_id BIGINT NULL,
 original_task_id BIGINT NULL,
 shift_id BIGINT NOT NULL,
 floor_area_id BIGINT NOT NULL,
 planned_at DATETIME(6) NOT NULL,
 window_from DATETIME(6) NULL,
 window_to DATETIME(6) NULL,
 importance ENUM('NORMAL','IMPORTANT','MANDATORY') NOT NULL,
 performer_role VARCHAR(50) NOT NULL,
 performer_id BIGINT NULL,
 status ENUM('NOT_YET_DUE','DUE','OVERDUE','COMPLETED','COMPLETED_LATE','NOT_PERFORMED','CANCELLED') NOT NULL,
 data JSON NOT NULL,
 due_at DATETIME(6) NULL,
 overdue_at DATETIME(6) NULL,
 closed_at DATETIME(6) NULL,
 closure_reason VARCHAR(2000) NULL,
 closure_rule VARCHAR(100) NULL,
 created_by BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 version INT NOT NULL DEFAULT 0,
 duplicate_key VARCHAR(200) GENERATED ALWAYS AS (IF(status<>'CANCELLED' AND basis_type NOT IN('AD_HOC','AD_HOC_RECORDING','REWORK','SHIFT_START_CHECK','SUPERVISION'),CONCAT(basis_type,':',basis_id,':',resident_id,':',planned_at),NULL)) STORED,
 shift_check_key VARCHAR(160) GENERATED ALWAYS AS (IF(status<>'CANCELLED' AND basis_type IN('SHIFT_START_CHECK','SUPERVISION'),CONCAT(basis_type,':',shift_id,':',performer_id),NULL)) STORED,
 CONSTRAINT uq_care_task_basis UNIQUE(duplicate_key),
 CONSTRAINT uq_care_task_shift_check UNIQUE(shift_check_key),
 CONSTRAINT ck_care_task_target CHECK(resident_id IS NOT NULL OR location_id IS NOT NULL),
 CONSTRAINT ck_care_task_window CHECK((window_from IS NULL AND window_to IS NULL AND basis_type='AD_HOC_RECORDING') OR (window_from IS NOT NULL AND window_to IS NOT NULL AND window_from<=planned_at AND planned_at<=window_to)),
 INDEX ix_care_task_due(status,window_from),
 INDEX ix_care_task_overdue(status,window_to),
 INDEX ix_care_task_shift(shift_id,performer_id),
 INDEX ix_care_task_floor(floor_area_id,shift_id,performer_id),
 INDEX ix_care_task_resident(resident_id,planned_at),
 INDEX ix_care_task_source(basis_type,basis_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DataGroup=BUSINESS';
DELIMITER $$
CREATE TRIGGER trg_care_task_final_update BEFORE UPDATE ON care_task FOR EACH ROW
BEGIN
 IF OLD.status IN('COMPLETED','COMPLETED_LATE','NOT_PERFORMED','CANCELLED') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TASK_CLOSED';
 END IF;
END$$
DELIMITER ;
GRANT INSERT,UPDATE ON care_task TO 'eldercare_app'@'%';
GRANT DELETE ON care_task TO 'eldercare_retention'@'%';
