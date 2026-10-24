-- T025/T043: tra cứu giường đang giữ sau khóa người, không đọc snapshot cũ hoặc khóa giường sai thứ tự.
ALTER TABLE resident_position ADD COLUMN waiting_hold_bed_ids JSON NOT NULL DEFAULT (JSON_ARRAY());
INSERT INTO resident_position(resident_id,waiting_hold_bed_ids)
 SELECT hold_resident_id,JSON_ARRAYAGG(id) FROM bed
 WHERE state='HELD' AND hold_reason='WAITING_RECORD' GROUP BY hold_resident_id
 ON DUPLICATE KEY UPDATE waiting_hold_bed_ids=VALUES(waiting_hold_bed_ids);
