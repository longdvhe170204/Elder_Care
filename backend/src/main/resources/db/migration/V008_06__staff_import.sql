-- T047: chỉ lưu dấu lần import, không lưu file Excel.
CREATE TABLE staff_import_batch (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 file_name VARCHAR(255) NOT NULL,
 row_count INT NOT NULL,
 created_by BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT chk_staff_import_rows CHECK(row_count>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
UPDATE staff_profile SET normalized_phone=CASE WHEN REGEXP_REPLACE(phone_number,'[[:space:].-]','') REGEXP '^[0-9]{1,15}$' THEN REGEXP_REPLACE(phone_number,'[[:space:].-]','') ELSE NULL END;
ALTER TABLE staff_profile ADD CONSTRAINT fk_staff_import_batch FOREIGN KEY(import_batch_id) REFERENCES staff_import_batch(id) ON DELETE RESTRICT;
DELIMITER $$
CREATE TRIGGER trg_staff_import_batch_no_update BEFORE UPDATE ON staff_import_batch FOR EACH ROW
BEGIN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='staff import batch is append-only (FR-001c)';
END$$
CREATE TRIGGER trg_staff_import_batch_no_delete BEFORE DELETE ON staff_import_batch FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='staff import batch removed only by retention (FR-001c)';
 END IF;
END$$
DELIMITER ;
GRANT INSERT ON staff_import_batch TO 'eldercare_app'@'%';
GRANT DELETE ON staff_import_batch TO 'eldercare_retention'@'%';
