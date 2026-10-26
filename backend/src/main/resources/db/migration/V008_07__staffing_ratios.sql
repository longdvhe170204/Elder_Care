-- FR-037: kết quả tính chỉ thêm; giữ cả khi actor xóa ca Nháp nên shift_id là tham chiếu logic.
CREATE TABLE staffing_ratio_calculation (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 shift_id BIGINT NOT NULL,
 calculated_at DATETIME(6) NOT NULL,
 event ENUM('DRAFT_SAVED','PUBLISHED','STAFF_ABSENT','STAFF_ADDED','EMPLOYMENT_ENDED','SHIFT_SWAP_015','CARE_LEVEL_CHANGED','ALLOCATION_CHANGED','RESIDENT_ABSENCE','FINAL_STATUS') NOT NULL,
 total_weight DECIMAL(8,2) NOT NULL,
 staff_count INT NOT NULL,
 ratio DECIMAL(8,2) NULL,
 threshold DECIMAL(8,2) NOT NULL,
 meets_threshold BOOLEAN NOT NULL,
 CHECK(total_weight>=0 AND staff_count>=0 AND threshold>0),
 CHECK((staff_count=0 AND ratio IS NULL) OR (staff_count>0 AND ratio>=0)),
 INDEX ix_staffing_ratio_shift(shift_id,calculated_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DELIMITER $$
CREATE TRIGGER trg_staffing_ratio_no_update BEFORE UPDATE ON staffing_ratio_calculation FOR EACH ROW
BEGIN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='staffing ratio calculation is append-only (FR-037)';
END$$
CREATE TRIGGER trg_staffing_ratio_no_delete BEFORE DELETE ON staffing_ratio_calculation FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='staffing ratio calculation removed only by retention (FR-037)';
 END IF;
END$$
DELIMITER ;
GRANT INSERT ON staffing_ratio_calculation TO 'eldercare_app'@'%';
GRANT DELETE ON staffing_ratio_calculation TO 'eldercare_retention'@'%';
