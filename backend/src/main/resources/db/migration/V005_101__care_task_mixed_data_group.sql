-- T064/R-15: task nhóm 2 và kết quả nhóm 3 dùng chung một dòng.
-- Cột sinh bao phủ cả dữ liệu đã có, không cần sửa bản ghi đã chốt.
ALTER TABLE care_task
 ADD COLUMN locked_at DATETIME(6) GENERATED ALWAYS AS (
  CASE WHEN status IN ('COMPLETED','COMPLETED_LATE','NOT_PERFORMED','CANCELLED')
   THEN COALESCE(closed_at,result_recorded_at,created_at) ELSE NULL END
 ) STORED,
 COMMENT = 'DataGroup=MULTIPLE';

DROP TRIGGER trg_care_task_final_update;
DELIMITER $$
CREATE TRIGGER trg_care_task_row_lock BEFORE UPDATE ON care_task FOR EACH ROW
BEGIN
 IF OLD.locked_at IS NOT NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TASK_CLOSED';
 END IF;
END$$
CREATE TRIGGER trg_care_task_no_delete BEFORE DELETE ON care_task FOR EACH ROW
BEGIN
 IF OLD.locked_at IS NOT NULL AND SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TASK_CLOSED';
 END IF;
END$$
DELIMITER ;
