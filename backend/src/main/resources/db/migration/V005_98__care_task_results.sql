-- T035/R-15: một task chứa tối đa một kết quả gốc; id kết quả chính là id task.
ALTER TABLE care_task
 ADD COLUMN result_type VARCHAR(40) NULL,
 ADD COLUMN result_numeric_value DECIMAL(10,2) NULL,
 ADD COLUMN result_code_value VARCHAR(100) NULL,
 ADD COLUMN result_values JSON NULL,
 ADD COLUMN result_performed_at DATETIME(6) NULL,
 ADD COLUMN result_recorded_at DATETIME(6) NULL,
 ADD COLUMN result_device_at DATETIME(6) NULL,
 ADD COLUMN result_recorded_by BIGINT NULL,
 ADD COLUMN result_late_recording BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN result_permission_lost BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN result_permission_basis ENUM('ASSIGNED','FLOOR_MANAGER_ROLE','SHIFT_LEAD_DUTY') NULL,
 ADD COLUMN vital_sign_record_id BIGINT NULL,
 ADD COLUMN meal_id BIGINT NULL,
 ADD COLUMN reason_type_id BIGINT NULL,
 ADD COLUMN closed_by_id BIGINT NULL,
 ADD COLUMN closed_by_type ENUM('STAFF','SYSTEM') NULL,
 ADD COLUMN awaiting_remeasurement BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN pending_result JSON NULL,
 ADD CONSTRAINT ck_care_result_complete CHECK (
   (result_performed_at IS NULL AND result_type IS NULL AND result_recorded_at IS NULL AND result_recorded_by IS NULL AND result_permission_basis IS NULL AND result_values IS NULL AND result_numeric_value IS NULL AND result_code_value IS NULL AND result_device_at IS NULL AND vital_sign_record_id IS NULL AND meal_id IS NULL AND NOT result_late_recording AND NOT result_permission_lost)
   OR
   (result_performed_at IS NOT NULL AND result_type IS NOT NULL AND result_recorded_at IS NOT NULL AND result_recorded_by IS NOT NULL AND result_permission_basis IS NOT NULL AND result_values IS NOT NULL AND status IN('COMPLETED','COMPLETED_LATE'))
 ),
 ADD CONSTRAINT ck_care_result_time CHECK(result_performed_at IS NULL OR result_performed_at<=COALESCE(result_device_at,result_recorded_at)),
 ADD CONSTRAINT ck_care_result_not_performed CHECK(status<>'NOT_PERFORMED' OR closed_by_type IS NULL OR closed_by_type<>'STAFF' OR reason_type_id IS NOT NULL),
 ADD CONSTRAINT ck_care_result_pending CHECK((NOT awaiting_remeasurement AND pending_result IS NULL) OR (awaiting_remeasurement AND pending_result IS NOT NULL AND status IN('DUE','OVERDUE') AND result_performed_at IS NULL)),
 ADD INDEX ix_care_task_result(resident_id,result_type,result_performed_at);

-- Trigger final_update của V005_97 đã cấm mọi UPDATE của task đóng, gồm kết quả.
-- DELETE chỉ dành tài khoản retention; ứng dụng không thể xóa bản gốc để ghi lại.
DELIMITER $$
CREATE TRIGGER trg_care_task_result_delete BEFORE DELETE ON care_task FOR EACH ROW
BEGIN
 IF OLD.result_performed_at IS NOT NULL AND SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CARE_RESULT_IMMUTABLE';
 END IF;
END$$
DELIMITER ;
