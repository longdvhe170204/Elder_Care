-- T042, FR-001a, DBR-36: lịch sử chuyển bộ phận chỉ thêm và đóng khoảng một lần.
DELIMITER $$
CREATE TRIGGER trg_staff_department_close_once BEFORE UPDATE ON staff_department FOR EACH ROW
BEGIN
 IF OLD.ends_at IS NOT NULL OR NEW.ends_at IS NULL OR NEW.ends_at<OLD.starts_at
 OR NOT(OLD.id<=>NEW.id) OR NOT(OLD.staff_id<=>NEW.staff_id)
 OR NOT(OLD.department_id<=>NEW.department_id) OR NOT(OLD.starts_at<=>NEW.starts_at)
 OR NOT(OLD.actor_id<=>NEW.actor_id) OR NOT(OLD.reason<=>NEW.reason) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='staff department history closes once (FR-001a, DBR-36)';
 END IF;
END$$
CREATE TRIGGER trg_staff_department_no_delete BEFORE DELETE ON staff_department FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='staff department history removed only by retention (FR-001a)';
 END IF;
END$$
DELIMITER ;

REVOKE UPDATE ON staff_department FROM 'eldercare_app'@'%';
GRANT UPDATE(ends_at) ON staff_department TO 'eldercare_app'@'%';
