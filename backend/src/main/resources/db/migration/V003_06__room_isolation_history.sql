-- T054: lịch sử cách ly là ghi nhận chỉ thêm, không lưu bản sao vùng khoanh của007.
CREATE TABLE room_isolation_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 room_id BIGINT NOT NULL, action ENUM('ISOLATE','LIFT') NOT NULL,
 occurred_at DATETIME(6) NOT NULL, actor_id BIGINT NOT NULL,
 reason VARCHAR(2000) NOT NULL, incident_id BIGINT NULL,
 CONSTRAINT fk_isolation_room FOREIGN KEY(room_id) REFERENCES room(id) ON DELETE RESTRICT,
 CONSTRAINT chk_isolation_reason CHECK(CHAR_LENGTH(TRIM(reason))>0),
 KEY idx_isolation_room_time(room_id,occurred_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DELIMITER $$
CREATE TRIGGER trg_room_isolation_no_update BEFORE UPDATE ON room_isolation_history FOR EACH ROW
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room_isolation_history is append-only';
$$
CREATE TRIGGER trg_room_isolation_no_delete BEFORE DELETE ON room_isolation_history FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='room_isolation_history is removed only by retention';
 END IF;
END$$
DELIMITER ;
GRANT INSERT ON room_isolation_history TO 'eldercare_app'@'%';
GRANT DELETE ON room_isolation_history TO 'eldercare_retention'@'%';
