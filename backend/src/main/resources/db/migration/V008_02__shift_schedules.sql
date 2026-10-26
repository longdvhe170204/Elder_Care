-- T012: bảng của M09; tham chiếu sang module khác chỉ giữ id logic.
CREATE TABLE shift_template (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(255) NOT NULL,
 start_time TIME NOT NULL,
 end_time TIME NOT NULL,
 scope_type ENUM('FLOOR','AREA','FACILITY') NOT NULL,
 requires_handover BOOLEAN NOT NULL DEFAULT TRUE,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE shift_schedule (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 month CHAR(7) NOT NULL,
 scope_type ENUM('FLOOR','AREA','FACILITY') NOT NULL,
 scope_id BIGINT NULL,
 scope_key BIGINT GENERATED ALWAYS AS (COALESCE(scope_id,0)) STORED,
 status ENUM('DRAFT','PUBLISHED') NOT NULL DEFAULT 'DRAFT',
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 created_by BIGINT NULL,
 published_at DATETIME(6) NULL,
 published_by BIGINT NULL,
 self_approved BOOLEAN NOT NULL DEFAULT FALSE,
 publication_reason VARCHAR(2000) NULL,
 version BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uq_shift_schedule_month(month,scope_type,scope_key),
 CHECK ((scope_type='FACILITY' AND scope_id IS NULL) OR (scope_type<>'FACILITY' AND scope_id>0))
);
CREATE TABLE work_shift (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 schedule_id BIGINT NOT NULL,
 template_id BIGINT NOT NULL,
 starts_at DATETIME(6) NOT NULL,
 ends_at DATETIME(6) NOT NULL,
 scope_type ENUM('FLOOR','AREA','FACILITY') NOT NULL,
 scope_id BIGINT NULL,
 requires_handover BOOLEAN NOT NULL,
 status ENUM('DRAFT','PUBLISHED','IN_PROGRESS','AWAITING_HANDOVER','CLOSED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
 lead_staff_id BIGINT NULL,
 missing_lead BOOLEAN NOT NULL DEFAULT FALSE,
 below_threshold BOOLEAN NOT NULL DEFAULT FALSE,
 manager_notified_below_threshold_at DATETIME(6) NULL,
 prepare_handover_reminded_at DATETIME(6) NULL,
 shift_end_reminded_at DATETIME(6) NULL,
 unconfirmed_handover_reminded_at DATETIME(6) NULL,
 unassigned_residents_notified_at DATETIME(6) NULL,
 cancellation_reason VARCHAR(2000) NULL,
 version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(schedule_id) REFERENCES shift_schedule(id),
 FOREIGN KEY(template_id) REFERENCES shift_template(id),
 FOREIGN KEY(lead_staff_id) REFERENCES staff_profile(id),
 CHECK (TIMESTAMPDIFF(MICROSECOND,starts_at,ends_at)>0 AND TIMESTAMPDIFF(MICROSECOND,starts_at,ends_at)<=86400000000),
 INDEX ix_work_shift_scope(scope_type,scope_id,starts_at),
 INDEX ix_work_shift_start(status,starts_at),
 INDEX ix_work_shift_end(status,ends_at)
);
CREATE TABLE shift_staff (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 shift_id BIGINT NOT NULL,
 staff_id BIGINT NOT NULL,
 on_duty_doctor BOOLEAN NOT NULL DEFAULT FALSE,
 absent_at DATETIME(6) NULL,
 absence_reason VARCHAR(2000) NULL,
 absence_recorded_by BIGINT NULL,
 withdrawn_on_resignation_at DATETIME(6) NULL,
 added_at DATETIME(6) NULL,
 addition_reason VARCHAR(2000) NULL,
 approved_leave_request_id BIGINT NULL,
 entry_basis ENUM('SCHEDULED','ADDED','SWAPPED','COVERED','INVITED') NOT NULL DEFAULT 'SCHEDULED',
 departure_basis VARCHAR(40) NULL,
 left_at DATETIME(6) NULL,
 FOREIGN KEY(shift_id) REFERENCES work_shift(id),
 FOREIGN KEY(staff_id) REFERENCES staff_profile(id),
 UNIQUE KEY uq_shift_staff(shift_id,staff_id),
 INDEX ix_shift_staff_member(staff_id)
);
CREATE TABLE shift_lead_history (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 shift_id BIGINT NOT NULL,
 staff_id BIGINT NOT NULL,
 starts_at DATETIME(6) NOT NULL,
 ends_at DATETIME(6) NULL,
 basis ENUM('SCHEDULED','CHANGED','ABSENCE','SHIFT_SWAP_015') NOT NULL,
 actor_id BIGINT NULL,
 reason VARCHAR(2000) NULL,
 open_shift_id BIGINT GENERATED ALWAYS AS (IF(ends_at IS NULL,shift_id,NULL)) STORED,
 UNIQUE KEY uq_shift_lead_open(open_shift_id),
 FOREIGN KEY(shift_id) REFERENCES work_shift(id),
 FOREIGN KEY(staff_id) REFERENCES staff_profile(id)
);
CREATE TABLE shift_publication (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 schedule_id BIGINT NOT NULL,
 published_at DATETIME(6) NOT NULL,
 content JSON NOT NULL,
 FOREIGN KEY(schedule_id) REFERENCES shift_schedule(id)
);

DELIMITER $$
CREATE TRIGGER trg_work_shift_update BEFORE UPDATE ON work_shift FOR EACH ROW
BEGIN
 IF OLD.status IN ('CLOSED','CANCELLED') AND (
 NOT(OLD.id<=>NEW.id) OR NOT(OLD.schedule_id<=>NEW.schedule_id) OR NOT(OLD.template_id<=>NEW.template_id)
 OR NOT(OLD.starts_at<=>NEW.starts_at) OR NOT(OLD.ends_at<=>NEW.ends_at)
 OR NOT(OLD.scope_type<=>NEW.scope_type) OR NOT(OLD.scope_id<=>NEW.scope_id)
 OR NOT(OLD.requires_handover<=>NEW.requires_handover) OR NOT(OLD.status<=>NEW.status)
 OR NOT(OLD.lead_staff_id<=>NEW.lead_staff_id) OR NOT(OLD.missing_lead<=>NEW.missing_lead)
 OR NOT(OLD.below_threshold<=>NEW.below_threshold) OR NOT(OLD.cancellation_reason<=>NEW.cancellation_reason)
 OR NOT(OLD.version<=>NEW.version)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='closed or cancelled shift is immutable (FR-022)';
 END IF;
 IF OLD.status<>'DRAFT' AND (NOT(OLD.starts_at<=>NEW.starts_at) OR NOT(OLD.ends_at<=>NEW.ends_at)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='published shift times are immutable (FR-017)';
 END IF;
END$$
CREATE TRIGGER trg_work_shift_delete BEFORE DELETE ON work_shift FOR EACH ROW
BEGIN
 IF OLD.status<>'DRAFT' AND SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='only draft shifts can be deleted (FR-012)';
 END IF;
END$$
CREATE TRIGGER trg_shift_staff_delete BEFORE DELETE ON shift_staff FOR EACH ROW
BEGIN
 IF (SELECT status FROM work_shift WHERE id=OLD.shift_id)<>'DRAFT' AND SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='published shift membership is retained (FR-037a)';
 END IF;
END$$
CREATE TRIGGER trg_shift_lead_close_once BEFORE UPDATE ON shift_lead_history FOR EACH ROW
BEGIN
 IF OLD.ends_at IS NOT NULL OR NEW.ends_at IS NULL OR NEW.ends_at<OLD.starts_at
 OR NOT(OLD.id<=>NEW.id) OR NOT(OLD.shift_id<=>NEW.shift_id) OR NOT(OLD.staff_id<=>NEW.staff_id)
 OR NOT(OLD.starts_at<=>NEW.starts_at) OR NOT(OLD.basis<=>NEW.basis)
 OR NOT(OLD.actor_id<=>NEW.actor_id) OR NOT(OLD.reason<=>NEW.reason) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='shift lead history closes once (FR-015)';
 END IF;
END$$
CREATE TRIGGER trg_shift_lead_delete BEFORE DELETE ON shift_lead_history FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='shift lead history is append-only (FR-015)';
 END IF;
END$$
CREATE TRIGGER trg_shift_publication_update BEFORE UPDATE ON shift_publication FOR EACH ROW
BEGIN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='publication snapshot is append-only (FR-055)';
END$$
CREATE TRIGGER trg_shift_publication_delete BEFORE DELETE ON shift_publication FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='publication snapshot is append-only (FR-055)';
 END IF;
END$$
DELIMITER ;

GRANT INSERT,UPDATE,DELETE ON shift_template TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE ON shift_schedule TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON work_shift TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON shift_staff TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE(ends_at) ON shift_lead_history TO 'eldercare_app'@'%';
GRANT INSERT ON shift_publication TO 'eldercare_app'@'%';
GRANT DELETE ON shift_template TO 'eldercare_retention'@'%';
GRANT DELETE ON shift_schedule TO 'eldercare_retention'@'%';
GRANT DELETE ON work_shift TO 'eldercare_retention'@'%';
GRANT DELETE ON shift_staff TO 'eldercare_retention'@'%';
GRANT DELETE ON shift_lead_history TO 'eldercare_retention'@'%';
GRANT DELETE ON shift_publication TO 'eldercare_retention'@'%';
