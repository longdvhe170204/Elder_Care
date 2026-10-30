-- 000 T108 phần 2 (research R-15, data-model 000 mục 8): kết quả áp dụng (application_result, 1–1) thành cột của
-- approval_request; yêu cầu đã áp dụng khóa theo dòng (locked_at, constitution XII 3.2.0), nên tác động không ghi được
-- hai lần (FR-037, SC-003).
ALTER TABLE approval_request
    ADD COLUMN result_type        VARCHAR(40) NULL,
    ADD COLUMN result_id          VARCHAR(64) NULL,
    ADD COLUMN result_recorded_at DATETIME(6) NULL,
    ADD COLUMN locked_at          DATETIME(6) NULL;

UPDATE approval_request r JOIN application_result a ON a.approval_request_id = r.id
SET r.result_type        = a.result_type,
    r.result_id          = a.result_id,
    r.result_recorded_at = a.recorded_at,
    r.locked_at          = a.recorded_at;

DROP TABLE application_result;

DELIMITER $$
CREATE TRIGGER trg_approval_request_row_lock
    BEFORE UPDATE
    ON approval_request
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'approval_request row is locked once applied (constitution XII)';
    END IF;
END$$

CREATE TRIGGER trg_approval_request_no_delete
    BEFORE DELETE
    ON approval_request
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL AND SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'applied approval_request rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;
