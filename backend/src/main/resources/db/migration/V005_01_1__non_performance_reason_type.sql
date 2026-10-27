-- T007a/FR-028a: tên phiên bản chỉ gồm số để Flyway nhận được (tên logic 01a trong tasks).
CREATE TABLE care_non_performance_reason_type (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(200) NOT NULL,
 fixed BOOLEAN NOT NULL DEFAULT FALSE,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 version INT NOT NULL DEFAULT 0,
 active_name VARCHAR(200) GENERATED ALWAYS AS (IF(status='ACTIVE',name,NULL)) STORED,
 UNIQUE KEY uq_care_reason_active_name(active_name),
 CONSTRAINT chk_care_fixed_reason_active CHECK (NOT fixed OR status='ACTIVE')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO care_non_performance_reason_type(name,fixed) VALUES ('Người cao tuổi từ chối',TRUE);
DELIMITER $$
CREATE TRIGGER trg_care_reason_fixed_update BEFORE UPDATE ON care_non_performance_reason_type
FOR EACH ROW
BEGIN
 IF OLD.fixed AND (NOT (NEW.name <=> OLD.name) OR NOT (NEW.fixed <=> OLD.fixed) OR NEW.status <> 'ACTIVE') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='REASON_TYPE_FIXED';
 END IF;
END$$
CREATE TRIGGER trg_care_reason_fixed_delete BEFORE DELETE ON care_non_performance_reason_type
FOR EACH ROW
BEGIN
 IF OLD.fixed THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='REASON_TYPE_FIXED';
 END IF;
END$$
DELIMITER ;
GRANT INSERT,UPDATE,DELETE ON care_non_performance_reason_type TO 'eldercare_app'@'%';
