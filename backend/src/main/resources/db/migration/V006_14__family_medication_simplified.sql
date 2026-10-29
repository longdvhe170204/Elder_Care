-- Q-393 (2026-10-29): thuốc gia đình gửi không theo dõi số lượng còn. Bỏ biến động số lượng, mốc nhắc tự động, số
-- lượng còn và cột lô ở liều, PRN; lô lưu số lượng lúc tiếp nhận và số đếm thực tế khi Hoàn trả, Hủy (data-model 006
-- mục thuoc_gia_dinh_gui; FR-044 → FR-046, BR-M07-11, 19). Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

REVOKE INSERT ON family_medication_movement FROM 'eldercare_app'@'%';
DROP TABLE family_medication_movement;
DROP TABLE family_medication_reminder;

ALTER TABLE family_medication_batch
    DROP CHECK chk_batch_remaining_non_negative,
    DROP COLUMN remaining_quantity,
    ADD COLUMN final_quantity DECIMAL(10,2) NULL COMMENT 'Số đếm thực tế khi Hoàn trả, Hủy (Q-393)',
    ADD COLUMN closed_at DATETIME(6) NULL,
    ADD COLUMN closed_by_id BIGINT NULL,
    ADD COLUMN return_recipient_name VARCHAR(255) NULL,
    ADD COLUMN return_relationship VARCHAR(100) NULL,
    ADD COLUMN witness VARCHAR(255) NULL;

ALTER TABLE dose_result DROP COLUMN family_medication_lot_id;
ALTER TABLE prn_administration DROP COLUMN family_medication_batch_id;

DELIMITER $$
CREATE TRIGGER trg_family_medication_batch_guard BEFORE UPDATE ON family_medication_batch FOR EACH ROW
BEGIN
 IF OLD.status IN ('DEPLETED','RETURNED','DISPOSED') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='family medication batch is closed (FR-042)';
 END IF;
 IF NOT(OLD.initial_quantity<=>NEW.initial_quantity) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='initial quantity of family medication batch is fixed (FR-045, Q-393)';
 END IF;
END$$
DELIMITER ;
