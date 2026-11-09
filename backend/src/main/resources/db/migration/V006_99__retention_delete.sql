-- 006 F-06 (data-model 006 mục 13, FR-050, FR-051, FR-051a): job loại bỏ dữ liệu hết hạn xóa được dữ liệu thuốc.
-- Trước đó eldercare_retention không có quyền xóa trên bảng 006 và ba trigger chặn xóa với mọi tài khoản. Cùng cách với
-- trg_medication_reconciliation_no_delete, trg_medication_handover_no_delete (V006_95): chỉ eldercare_retention xóa được;
-- tài khoản ứng dụng vẫn bị chặn. medication_reconciliation, medication_handover đã có trigger như vậy, chỉ thiếu quyền.

DROP TRIGGER trg_prescription_no_delete;
DROP TRIGGER trg_medication_dose_no_delete;
DROP TRIGGER trg_dose_result_no_delete;

DELIMITER $$

CREATE TRIGGER trg_prescription_no_delete
    BEFORE DELETE ON prescription
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'prescription cannot be deleted (FR-014)';
    END IF;
END$$

CREATE TRIGGER trg_medication_dose_no_delete
    BEFORE DELETE ON medication_dose
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Medication doses cannot be deleted';
    END IF;
END$$

CREATE TRIGGER trg_dose_result_no_delete
    BEFORE DELETE ON dose_result
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dose result cannot be deleted (Group 3)';
    END IF;
END$$

DELIMITER ;

GRANT DELETE ON prescription TO 'eldercare_retention'@'%';
GRANT DELETE ON medication_dose TO 'eldercare_retention'@'%';
GRANT DELETE ON dose_result TO 'eldercare_retention'@'%';
GRANT DELETE ON medication_reconciliation TO 'eldercare_retention'@'%';
GRANT DELETE ON medication_handover TO 'eldercare_retention'@'%';
GRANT DELETE ON family_medication_batch TO 'eldercare_retention'@'%';
