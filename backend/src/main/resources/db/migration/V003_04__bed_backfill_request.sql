-- T034/FR-027: chỉ mục kỹ thuật của yêu cầu nhập bù, không thay nội dung sau khi lập.
CREATE TABLE bed_backfill_request (
 approval_request_id BIGINT NOT NULL PRIMARY KEY,
 resident_id BIGINT NOT NULL,
 current_allocation_id BIGINT NOT NULL,
 new_bed_id BIGINT NOT NULL,
 transfer_at DATETIME(6) NOT NULL,
 KEY idx_backfill_resident_time(resident_id,transfer_at),
 KEY idx_backfill_bed_time(new_bed_id,transfer_at),
 CONSTRAINT fk_backfill_request FOREIGN KEY(approval_request_id) REFERENCES approval_request(id) ON DELETE RESTRICT,
 CONSTRAINT fk_backfill_allocation FOREIGN KEY(current_allocation_id) REFERENCES bed_allocation(id) ON DELETE RESTRICT,
 CONSTRAINT fk_backfill_new_bed FOREIGN KEY(new_bed_id) REFERENCES bed(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TRIGGER trg_bed_backfill_request_no_update BEFORE UPDATE ON bed_backfill_request FOR EACH ROW
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Backfill request index is immutable (FR-027)';

DELIMITER $$
CREATE TRIGGER trg_bed_backfill_request_no_delete BEFORE DELETE ON bed_backfill_request FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Backfill request index is removed only by retention';
 END IF;
END$$
DELIMITER ;

GRANT INSERT ON bed_backfill_request TO 'eldercare_app'@'%';
GRANT DELETE ON bed_backfill_request TO 'eldercare_retention'@'%';
