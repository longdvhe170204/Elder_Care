-- T019: bảng M09; khóa kỹ thuật không phải bản ghi nghiệp vụ do hệ thống lập.
ALTER TABLE work_shift ADD COLUMN publication_assignments_notified_at DATETIME(6) NULL;
CREATE TABLE floor_scope_lock (
 floor_area_id BIGINT PRIMARY KEY
);
CREATE TABLE floor_manager_assignment (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 staff_id BIGINT NOT NULL,
 floor_area_id BIGINT NOT NULL,
 starts_at DATETIME(6) NOT NULL,
 ends_at DATETIME(6) NULL,
 status ENUM('ACTIVE','ENDED') NOT NULL DEFAULT 'ACTIVE',
 assigned_by BIGINT NULL,
 reason VARCHAR(2000) NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(staff_id) REFERENCES staff_profile(id),
 CHECK (ends_at IS NULL OR ends_at>starts_at),
 INDEX ix_floor_manager_scope(floor_area_id,starts_at,ends_at),
 INDEX ix_floor_manager_staff(staff_id,starts_at)
);
CREATE TABLE staff_assignment (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 shift_id BIGINT NOT NULL,
 staff_id BIGINT NOT NULL,
 target_type ENUM('FLOOR','AREA','ROOM','RESIDENT') NOT NULL,
 target_id BIGINT NOT NULL,
 task_type_id BIGINT NULL,
 role VARCHAR(40) NOT NULL,
 assignment_kind ENUM('PRIMARY','SUPPORT') NOT NULL,
 assigned_nurse BOOLEAN NOT NULL DEFAULT FALSE,
 starts_at DATETIME(6) NOT NULL,
 ends_at DATETIME(6) NOT NULL,
 actual_end_at DATETIME(6) NULL,
 status ENUM('DRAFT','SCHEDULED','ACTIVE','ENDED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
 end_reason VARCHAR(2000) NULL,
 source ENUM('MANUAL','COPIED','SHIFT_SWAP_015') NOT NULL DEFAULT 'MANUAL',
 version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 created_by BIGINT NULL,
 FOREIGN KEY(shift_id) REFERENCES work_shift(id),
 FOREIGN KEY(staff_id) REFERENCES staff_profile(id),
 CHECK (ends_at>starts_at),
 CHECK (actual_end_at IS NULL OR (actual_end_at>=starts_at AND actual_end_at<=ends_at)),
 INDEX ix_staff_assignment_shift(shift_id,status),
 INDEX ix_staff_assignment_staff(staff_id,starts_at),
 INDEX ix_staff_assignment_target(target_type,target_id,starts_at)
);

DELIMITER $$
CREATE TRIGGER trg_floor_manager_update BEFORE UPDATE ON floor_manager_assignment FOR EACH ROW
BEGIN
 IF OLD.status='ENDED' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ended floor manager assignment is immutable (FR-024)';
 END IF;
END$$
CREATE TRIGGER trg_floor_manager_delete BEFORE DELETE ON floor_manager_assignment FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='floor manager assignment cannot be deleted (FR-024)';
 END IF;
END$$
CREATE TRIGGER trg_staff_assignment_update BEFORE UPDATE ON staff_assignment FOR EACH ROW
BEGIN
 IF OLD.status IN ('ENDED','CANCELLED') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='terminal staff assignment is immutable (FR-027)';
 END IF;
END$$
CREATE TRIGGER trg_staff_assignment_delete BEFORE DELETE ON staff_assignment FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' AND (OLD.status<>'DRAFT' OR
  (SELECT status FROM work_shift WHERE id=OLD.shift_id)<>'DRAFT') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='only draft assignment in draft shift can be deleted (FR-025)';
 END IF;
END$$
DELIMITER ;

GRANT INSERT,UPDATE ON floor_scope_lock TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE ON floor_manager_assignment TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON staff_assignment TO 'eldercare_app'@'%';
GRANT DELETE ON floor_manager_assignment TO 'eldercare_retention'@'%';
GRANT DELETE ON staff_assignment TO 'eldercare_retention'@'%';
