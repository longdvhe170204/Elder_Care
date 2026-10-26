-- T026: chỉ lệnh người dùng lập bàn giao và các mục; không nạp bản ghi nghiệp vụ.
CREATE TABLE shift_handover (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 shift_id BIGINT NOT NULL UNIQUE,
 next_shift_id BIGINT NULL,
 handover_staff_id BIGINT NOT NULL,
 created_by BIGINT NULL,
 finalized_by BIGINT NULL,
 confirmed_by BIGINT NULL,
 duty_basis VARCHAR(60) NULL,
 status ENUM('DRAFT','PREPARED','HAS_COMMENTS','CONFIRMED') NOT NULL DEFAULT 'DRAFT',
 created_at DATETIME(6) NOT NULL,
 finalized_at DATETIME(6) NULL,
 confirmed_at DATETIME(6) NULL,
 source_keys_at_finalization JSON NULL,
 version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(shift_id) REFERENCES work_shift(id),
 FOREIGN KEY(next_shift_id) REFERENCES work_shift(id),
 FOREIGN KEY(handover_staff_id) REFERENCES staff_profile(id),
 INDEX ix_handover_next(next_shift_id,status),
 CHECK ((status='DRAFT' AND finalized_at IS NULL) OR (status<>'DRAFT' AND finalized_at IS NOT NULL)),
 CHECK ((status='CONFIRMED' AND confirmed_at IS NOT NULL) OR (status<>'CONFIRMED' AND confirmed_at IS NULL))
);
CREATE TABLE handover_item (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 handover_id BIGINT NOT NULL,
 item_group ENUM('TASK','DOSE','VITAL_SIGN','ALERT','INCIDENT','RESIDENT_CHANGE','BED_AWAITING_READINESS','STORED_BELONGING','FOLLOW_UP_ISSUE') NOT NULL,
 source_type VARCHAR(100) NULL,
 source_id VARCHAR(150) NULL,
 resident_id BIGINT NULL,
 room_id BIGINT NULL,
 description TEXT NOT NULL,
 note TEXT NULL,
 note_author VARCHAR(255) NULL,
 resolved BOOLEAN NOT NULL DEFAULT FALSE,
 section ENUM('FINALIZED','AFTER_PREPARATION','RESOLVED_DURING_SHIFT') NOT NULL DEFAULT 'FINALIZED',
 status_at_finalization VARCHAR(100) NULL,
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(handover_id) REFERENCES shift_handover(id),
 CHECK (resident_id IS NOT NULL OR room_id IS NOT NULL),
 INDEX ix_handover_item_parent(handover_id),
 INDEX ix_handover_item_resident(resident_id)
);
CREATE TABLE handover_assessment (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 handover_id BIGINT NOT NULL,
 assessment_kind ENUM('GENERAL','BY_RESIDENT') NOT NULL,
 resident_id BIGINT NULL,
 resident_key BIGINT GENERATED ALWAYS AS (COALESCE(resident_id,0)) STORED,
 content TEXT NOT NULL,
 FOREIGN KEY(handover_id) REFERENCES shift_handover(id),
 UNIQUE KEY uq_handover_assessment(handover_id,assessment_kind,resident_key),
 CHECK ((assessment_kind='GENERAL' AND resident_id IS NULL) OR (assessment_kind='BY_RESIDENT' AND resident_id>0))
);
CREATE TABLE handover_comment (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 handover_id BIGINT NOT NULL,
 content TEXT NOT NULL,
 sender VARCHAR(255) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(handover_id) REFERENCES shift_handover(id)
);
CREATE TABLE handover_reply (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 comment_id BIGINT NOT NULL,
 content TEXT NOT NULL,
 sender VARCHAR(255) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(comment_id) REFERENCES handover_comment(id)
);

DELIMITER $$
CREATE TRIGGER trg_handover_update BEFORE UPDATE ON shift_handover FOR EACH ROW
BEGIN
 IF OLD.status='CONFIRMED' OR NOT(OLD.shift_id<=>NEW.shift_id) OR
  (OLD.finalized_at IS NOT NULL AND (NOT(OLD.finalized_at<=>NEW.finalized_at) OR NOT(OLD.finalized_by<=>NEW.finalized_by) OR NOT(OLD.source_keys_at_finalization<=>NEW.source_keys_at_finalization))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover original is locked (FR-041/044)';
 END IF;
END$$
CREATE TRIGGER trg_handover_delete BEFORE DELETE ON shift_handover FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover cannot be deleted (FR-047)';
 END IF;
END$$
CREATE TRIGGER trg_handover_item_insert BEFORE INSERT ON handover_item FOR EACH ROW
BEGIN
 DECLARE parent_status VARCHAR(30);
 DECLARE finalized DATETIME(6);
 SELECT status,finalized_at INTO parent_status,finalized FROM shift_handover WHERE id=NEW.handover_id FOR UPDATE;
 IF parent_status='CONFIRMED' OR (finalized IS NOT NULL AND NEW.section<>'AFTER_PREPARATION') OR
  (finalized IS NULL AND NEW.section='AFTER_PREPARATION') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover section is locked (FR-041/044)';
 END IF;
END$$
CREATE TRIGGER trg_handover_item_update BEFORE UPDATE ON handover_item FOR EACH ROW
BEGIN
 DECLARE finalized DATETIME(6);
 SELECT finalized_at INTO finalized FROM shift_handover WHERE id=OLD.handover_id FOR UPDATE;
 IF finalized IS NOT NULL OR OLD.section='AFTER_PREPARATION' OR NEW.handover_id<>OLD.handover_id OR
  (OLD.item_group IN ('VITAL_SIGN','RESIDENT_CHANGE') AND (NEW.item_group<>OLD.item_group OR
   NOT (NEW.source_type<=>OLD.source_type) OR NOT (NEW.source_id<=>OLD.source_id) OR
   NOT (NEW.resident_id<=>OLD.resident_id) OR NOT (NEW.room_id<=>OLD.room_id))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='finalized handover item is immutable (FR-041)';
 END IF;
END$$
CREATE TRIGGER trg_handover_item_delete BEFORE DELETE ON handover_item FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' AND
  (OLD.item_group IN ('VITAL_SIGN','RESIDENT_CHANGE') OR OLD.section='AFTER_PREPARATION' OR
   (SELECT finalized_at FROM shift_handover WHERE id=OLD.handover_id) IS NOT NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover item cannot be discarded (FR-040/041)';
 END IF;
END$$
CREATE TRIGGER trg_handover_assessment_insert BEFORE INSERT ON handover_assessment FOR EACH ROW
BEGIN
 IF (SELECT finalized_at FROM shift_handover WHERE id=NEW.handover_id FOR UPDATE) IS NOT NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover assessment is locked (FR-041)';
 END IF;
END$$
CREATE TRIGGER trg_handover_assessment_update BEFORE UPDATE ON handover_assessment FOR EACH ROW
BEGIN
 IF OLD.handover_id<>NEW.handover_id OR (SELECT finalized_at FROM shift_handover WHERE id=OLD.handover_id FOR UPDATE) IS NOT NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover assessment is locked (FR-041)';
 END IF;
END$$
CREATE TRIGGER trg_handover_assessment_delete BEFORE DELETE ON handover_assessment FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' AND (SELECT finalized_at FROM shift_handover WHERE id=OLD.handover_id) IS NOT NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover assessment is locked (FR-041)';
 END IF;
END$$
CREATE TRIGGER trg_handover_comment_insert BEFORE INSERT ON handover_comment FOR EACH ROW
BEGIN
 IF (SELECT status FROM shift_handover WHERE id=NEW.handover_id FOR UPDATE)='CONFIRMED' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='confirmed handover is locked (FR-044)';
 END IF;
END$$
CREATE TRIGGER trg_handover_comment_update BEFORE UPDATE ON handover_comment FOR EACH ROW
BEGIN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover comment is append only (FR-042)';
END$$
CREATE TRIGGER trg_handover_comment_delete BEFORE DELETE ON handover_comment FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover comment is append only (FR-042)';
 END IF;
END$$
CREATE TRIGGER trg_handover_reply_insert BEFORE INSERT ON handover_reply FOR EACH ROW
BEGIN
 IF (SELECT h.status FROM shift_handover h JOIN handover_comment c ON c.handover_id=h.id WHERE c.id=NEW.comment_id FOR UPDATE)='CONFIRMED' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='confirmed handover is locked (FR-044)';
 END IF;
END$$
CREATE TRIGGER trg_handover_reply_update BEFORE UPDATE ON handover_reply FOR EACH ROW
BEGIN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover reply is append only (FR-042)';
END$$
CREATE TRIGGER trg_handover_reply_delete BEFORE DELETE ON handover_reply FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover reply is append only (FR-042)';
 END IF;
END$$
DELIMITER ;

GRANT INSERT,UPDATE,DELETE ON shift_handover TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON handover_item TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON handover_assessment TO 'eldercare_app'@'%';
GRANT INSERT ON handover_comment TO 'eldercare_app'@'%';
GRANT INSERT ON handover_reply TO 'eldercare_app'@'%';
GRANT DELETE ON shift_handover TO 'eldercare_retention'@'%';
GRANT DELETE ON handover_item TO 'eldercare_retention'@'%';
GRANT DELETE ON handover_assessment TO 'eldercare_retention'@'%';
GRANT DELETE ON handover_comment TO 'eldercare_retention'@'%';
GRANT DELETE ON handover_reply TO 'eldercare_retention'@'%';
