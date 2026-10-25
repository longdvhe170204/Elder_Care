-- 003 T007: giường, lịch sử chỉ thêm và CHECK DBR-26/BR-M03-10.
CREATE TABLE bed (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, room_id BIGINT NOT NULL, code VARCHAR(20) NOT NULL,
 state ENUM('EMPTY','OCCUPIED','HELD','AWAITING_READY','UNDER_MAINTENANCE','SUSPENDED','RETIRED') NOT NULL DEFAULT 'EMPTY',
 hold_reason ENUM('ABSENT_RESIDENT','WAITING_RECORD') NULL,
 hold_resident_id BIGINT NULL, hold_waiting_record_id BIGINT NULL, hold_until DATETIME(6) NULL,
 hold_expiry_notified_at DATETIME(6) NULL, awaiting_ready_since DATETIME(6) NULL, ready_deadline DATETIME(6) NULL,
 last_closed_allocation_id BIGINT NULL, ready_reminded_at DATETIME(6) NULL, suspension_reason VARCHAR(2000) NULL,
 version INT NOT NULL DEFAULT 0,
 UNIQUE KEY uq_bed_room_code(room_id,code),
 CONSTRAINT fk_bed_room FOREIGN KEY(room_id) REFERENCES room(id) ON DELETE RESTRICT,
 CONSTRAINT chk_bed_hold_state CHECK((state='HELD')=(hold_reason IS NOT NULL)),
 CONSTRAINT chk_bed_absent_hold CHECK(hold_reason<>'ABSENT_RESIDENT' OR hold_resident_id IS NOT NULL),
 CONSTRAINT chk_bed_waiting_hold CHECK(hold_reason<>'WAITING_RECORD' OR (hold_waiting_record_id IS NOT NULL AND hold_until IS NOT NULL)),
 CONSTRAINT chk_bed_ready_state CHECK((state='AWAITING_READY')=(ready_deadline IS NOT NULL)),
 CONSTRAINT chk_bed_suspended_state CHECK((state='SUSPENDED')=(suspension_reason IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- Khóa ngoại last_closed_allocation_id thêm ở V003_03 khi bảng bed_allocation được tạo (T023).
CREATE TABLE bed_state_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, bed_id BIGINT NOT NULL,
 from_state ENUM('EMPTY','OCCUPIED','HELD','AWAITING_READY','UNDER_MAINTENANCE','SUSPENDED','RETIRED') NULL, to_state ENUM('EMPTY','OCCUPIED','HELD','AWAITING_READY','UNDER_MAINTENANCE','SUSPENDED','RETIRED') NOT NULL,
 event VARCHAR(30) NOT NULL, occurred_at DATETIME(6) NOT NULL, planned_at DATETIME(6) NULL,
 executed_late BOOLEAN NOT NULL DEFAULT FALSE, actor_id BIGINT NULL, reason VARCHAR(2000) NULL,
 basis_type VARCHAR(40) NOT NULL, basis_id BIGINT NOT NULL,
 disinfected BOOLEAN NULL, ppe_used BOOLEAN NULL, disinfection_required BOOLEAN NULL,
 note VARCHAR(2000) NULL, floor_area_id BIGINT NOT NULL,
 KEY idx_bed_history_time(bed_id,occurred_at,id), KEY idx_bed_history_basis(basis_type,basis_id),
 KEY idx_bed_history_retention(occurred_at),
 CONSTRAINT fk_bed_history_bed FOREIGN KEY(bed_id) REFERENCES bed(id) ON DELETE RESTRICT,
 CONSTRAINT chk_bed_history_disinfection CHECK(
   event<>'CONFIRM_READY' OR (disinfection_required IS NOT NULL AND
     (disinfection_required=FALSE OR (COALESCE(disinfected,FALSE) AND COALESCE(ppe_used,FALSE)))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TRIGGER trg_bed_state_history_no_update BEFORE UPDATE ON bed_state_history FOR EACH ROW
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='bed_state_history is append-only (DBR-23)';
DELIMITER $$
CREATE TRIGGER trg_bed_state_history_no_delete BEFORE DELETE ON bed_state_history FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='bed_state_history is removed only by the retention job';
 END IF;
END$$
DELIMITER ;
GRANT INSERT,UPDATE ON bed TO 'eldercare_app'@'%';
GRANT INSERT ON bed_state_history TO 'eldercare_app'@'%';
GRANT DELETE ON bed_state_history TO 'eldercare_retention'@'%';
