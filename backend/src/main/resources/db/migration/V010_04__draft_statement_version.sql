-- FR-004/033: khoản mới thay đổi tập khoản và làm mất hiệu lực ETag của bảng.
-- Giữ khóa cha của trg_cost_item_insert: khoản không thể lọt vào bảng đang/đã chốt.
DELIMITER $$
CREATE TRIGGER trg_cost_item_statement_version AFTER INSERT ON cost_item FOR EACH ROW
BEGIN
 UPDATE cost_statement SET version=version+1,
   status=CASE WHEN NEW.status='DRAFT' THEN 'OPEN' ELSE status END
 WHERE id=NEW.statement_id;
END$$
DELIMITER ;
