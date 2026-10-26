-- 008 T004: hồ sơ nhân viên, bộ phận, giấy phép/đào tạo; tên theo glossary mục 14.
-- Tham chiếu liên module giữ id logic, không đọc hoặc ghi bảng module khác.
CREATE TABLE staff_profile (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 full_name VARCHAR(200) NOT NULL,
 position VARCHAR(200) NOT NULL,
 specialty VARCHAR(255) NULL,
 start_date DATE NULL,
 phone_number VARCHAR(50) NULL,
 normalized_phone VARCHAR(15) NULL,
 email VARCHAR(255) NULL,
 address VARCHAR(500) NULL,
 account_id BIGINT NULL UNIQUE,
 source ENUM('MANUAL','IMPORT') NOT NULL DEFAULT 'MANUAL',
 import_batch_id BIGINT NULL,
 status ENUM('WORKING','RESIGNED') NOT NULL DEFAULT 'WORKING',
 employment_ends_at DATETIME(6) NULL,
 termination_reason TEXT NULL,
 termination_applied_at DATETIME(6) NULL,
 version INT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 created_by BIGINT NULL,
 KEY idx_staff_normalized_phone(normalized_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE department (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(200) NOT NULL,
 lead_staff_id BIGINT NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 active_name VARCHAR(200) GENERATED ALWAYS AS (IF(status='ACTIVE',name,NULL)) STORED UNIQUE,
 version INT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_department_lead FOREIGN KEY(lead_staff_id) REFERENCES staff_profile(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE staff_department (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 staff_id BIGINT NOT NULL,
 department_id BIGINT NOT NULL,
 starts_at DATETIME(6) NOT NULL,
 ends_at DATETIME(6) NULL,
 actor_id BIGINT NOT NULL,
 reason TEXT NOT NULL,
 open_staff_id BIGINT GENERATED ALWAYS AS (IF(ends_at IS NULL,staff_id,NULL)) STORED UNIQUE,
 CONSTRAINT fk_staff_department_staff FOREIGN KEY(staff_id) REFERENCES staff_profile(id) ON DELETE RESTRICT,
 CONSTRAINT fk_staff_department_department FOREIGN KEY(department_id) REFERENCES department(id) ON DELETE RESTRICT,
 CONSTRAINT chk_staff_department_range CHECK(ends_at IS NULL OR ends_at>=starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE department_lead_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 department_id BIGINT NOT NULL,
 staff_id BIGINT NOT NULL,
 starts_at DATETIME(6) NOT NULL,
 ends_at DATETIME(6) NULL,
 actor_id BIGINT NOT NULL,
 reason TEXT NOT NULL,
 open_department_id BIGINT GENERATED ALWAYS AS (IF(ends_at IS NULL,department_id,NULL)) STORED UNIQUE,
 CONSTRAINT fk_department_history_staff FOREIGN KEY(staff_id) REFERENCES staff_profile(id) ON DELETE RESTRICT,
 CONSTRAINT fk_department_history_department FOREIGN KEY(department_id) REFERENCES department(id) ON DELETE RESTRICT,
 CONSTRAINT chk_department_lead_range CHECK(ends_at IS NULL OR ends_at>=starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE credential_type (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(60) NOT NULL UNIQUE,
 name VARCHAR(200) NOT NULL,
 credential_group ENUM('PROFESSIONAL_LICENSE','TRAINING') NOT NULL,
 has_expiry BOOLEAN NOT NULL,
 required_for_roles JSON NOT NULL,
 has_practice_scopes BOOLEAN NOT NULL DEFAULT FALSE,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 active_name VARCHAR(200) GENERATED ALWAYS AS (IF(status='ACTIVE',name,NULL)) STORED UNIQUE,
 version INT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- id kỹ thuật để dùng PracticeScopeCatalogApi của 002; code là mã logic của danh mục.
CREATE TABLE practice_scope (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(100) NOT NULL UNIQUE,
 name VARCHAR(200) NOT NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 active_name VARCHAR(200) GENERATED ALWAYS AS (IF(status='ACTIVE',name,NULL)) STORED UNIQUE,
 version INT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE credential (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 staff_id BIGINT NOT NULL,
 credential_type_id BIGINT NOT NULL,
 credential_number VARCHAR(100) NULL,
 issuing_authority VARCHAR(255) NULL,
 issue_date DATE NOT NULL,
 expiry_date DATE NULL,
 replaces_id BIGINT NULL,
 revoked_from DATE NULL,
 revocation_reason TEXT NULL,
 scan_file_id BIGINT NULL,
 expiry_warning_at DATETIME(6) NULL,
 invalidity_applied_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 created_by BIGINT NULL,
 CONSTRAINT fk_credential_staff FOREIGN KEY(staff_id) REFERENCES staff_profile(id) ON DELETE RESTRICT,
 CONSTRAINT fk_credential_type FOREIGN KEY(credential_type_id) REFERENCES credential_type(id) ON DELETE RESTRICT,
 CONSTRAINT fk_credential_replaced FOREIGN KEY(replaces_id) REFERENCES credential(id) ON DELETE RESTRICT,
 CONSTRAINT chk_credential_date CHECK(expiry_date IS NULL OR expiry_date>=issue_date),
 KEY idx_credential_staff_type(staff_id,credential_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE credential_practice_scope (
 credential_id BIGINT NOT NULL,
 practice_scope_id BIGINT NOT NULL,
 PRIMARY KEY(credential_id,practice_scope_id),
 CONSTRAINT fk_credential_scope_credential FOREIGN KEY(credential_id) REFERENCES credential(id) ON DELETE RESTRICT,
 CONSTRAINT fk_credential_scope_catalog FOREIGN KEY(practice_scope_id) REFERENCES practice_scope(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE m09_job_run (
 job VARCHAR(60) NOT NULL,
 scheduled_at DATETIME(6) NOT NULL,
 started_at DATETIME(6) NOT NULL,
 finished_at DATETIME(6) NULL,
 result ENUM('SUCCEEDED','FAILED') NULL,
 PRIMARY KEY(job,scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO department(name) VALUES ('Y tế'),('Chăm sóc'),('Bếp'),('Hành chính – Kế toán');
INSERT INTO credential_type(code,name,credential_group,has_expiry,required_for_roles,has_practice_scopes) VALUES
 ('DOCTOR_LICENSE','Giấy phép hành nghề bác sĩ','PROFESSIONAL_LICENSE',TRUE,JSON_ARRAY('DOCTOR'),TRUE),
 ('NURSING_LICENSE','Giấy phép hành nghề điều dưỡng','PROFESSIONAL_LICENSE',TRUE,JSON_ARRAY('NURSING_STAFF'),TRUE),
 ('FIRST_AID_TRAINING','Đào tạo sơ cứu','TRAINING',TRUE,JSON_ARRAY(),FALSE);

DELIMITER $$
CREATE TRIGGER trg_staff_profile_status BEFORE UPDATE ON staff_profile FOR EACH ROW
BEGIN
 IF NOT(OLD.status<=>NEW.status) AND COALESCE(@staff_status_command,0)<>1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='staff status changes only through commands (FR-003)';
 END IF;
END$$
CREATE TRIGGER trg_staff_profile_no_delete BEFORE DELETE ON staff_profile FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='staff rows removed only by retention (FR-001)';
 END IF;
END$$
CREATE TRIGGER trg_department_lead_history_close_only BEFORE UPDATE ON department_lead_history FOR EACH ROW
BEGIN
 IF OLD.ends_at IS NOT NULL OR NEW.ends_at IS NULL
 OR NOT(OLD.id<=>NEW.id) OR NOT(OLD.department_id<=>NEW.department_id)
 OR NOT(OLD.staff_id<=>NEW.staff_id) OR NOT(OLD.starts_at<=>NEW.starts_at)
 OR NOT(OLD.actor_id<=>NEW.actor_id) OR NOT(OLD.reason<=>NEW.reason) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='department lead history closes once (FR-001a)';
 END IF;
END$$
CREATE TRIGGER trg_department_lead_history_no_delete BEFORE DELETE ON department_lead_history FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='department lead history is append-only (FR-001a)';
 END IF;
END$$
CREATE TRIGGER trg_credential_protected_fields BEFORE UPDATE ON credential FOR EACH ROW
BEGIN
 IF COALESCE(@correct_credential,0)<>1 AND (
  NOT(OLD.id<=>NEW.id) OR NOT(OLD.staff_id<=>NEW.staff_id) OR NOT(OLD.credential_type_id<=>NEW.credential_type_id)
  OR NOT(OLD.credential_number<=>NEW.credential_number) OR NOT(OLD.issuing_authority<=>NEW.issuing_authority)
  OR NOT(OLD.issue_date<=>NEW.issue_date) OR NOT(OLD.expiry_date<=>NEW.expiry_date)
  OR NOT(OLD.replaces_id<=>NEW.replaces_id) OR NOT(OLD.scan_file_id<=>NEW.scan_file_id)
  OR NOT(OLD.created_at<=>NEW.created_at) OR NOT(OLD.created_by<=>NEW.created_by)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='credential descriptive fields require correction (FR-006)';
 END IF;
END$$
CREATE TRIGGER trg_credential_no_delete BEFORE DELETE ON credential FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='credential rows removed only by retention (FR-006)';
 END IF;
END$$
DELIMITER ;

GRANT INSERT,UPDATE ON staff_profile TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON department TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE ON staff_department TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE(ends_at) ON department_lead_history TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON credential_type TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON practice_scope TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE ON credential TO 'eldercare_app'@'%';
GRANT INSERT,DELETE ON credential_practice_scope TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE ON m09_job_run TO 'eldercare_app'@'%';
GRANT DELETE ON staff_profile TO 'eldercare_retention'@'%';
GRANT DELETE ON department TO 'eldercare_retention'@'%';
GRANT DELETE ON staff_department TO 'eldercare_retention'@'%';
GRANT DELETE ON department_lead_history TO 'eldercare_retention'@'%';
GRANT DELETE ON credential_type TO 'eldercare_retention'@'%';
GRANT DELETE ON practice_scope TO 'eldercare_retention'@'%';
GRANT DELETE ON credential TO 'eldercare_retention'@'%';
GRANT DELETE ON credential_practice_scope TO 'eldercare_retention'@'%';
