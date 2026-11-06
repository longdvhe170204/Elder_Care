-- 008 T050, R-15: chép đủ dữ liệu trước khi bỏ 17 bảng; không đổi id logic/API/nhật ký cũ.
-- ID vật lý mới được Store ánh xạ từ (loại, legacy.table/id), người dùng chốt ngày 2026-10-31.
-- Khóa ngoại vật lý đổi trong migration này; phạm vi chuyên môn M15 vẫn dùng id logic của M09.
DROP TRIGGER trg_credential_protected_fields;
DROP TRIGGER trg_staff_department_close_once;
DROP TRIGGER trg_work_shift_update;
DROP TRIGGER trg_staff_assignment_update;
DROP TRIGGER trg_staff_assignment_delete;
DROP TRIGGER trg_handover_update;
DROP TRIGGER trg_handover_delete;

-- Dãy dùng chung vượt toàn bộ miền id cũ, để bản ghi mới không trùng id logic của dòng đã chép.
SET @m09_floor = (SELECT GREATEST(COALESCE((SELECT MAX(id) FROM catalog_item),0),
 COALESCE((SELECT MAX(id) FROM department),0),COALESCE((SELECT MAX(id) FROM credential_type),0),
 COALESCE((SELECT MAX(id) FROM practice_scope),0),COALESCE((SELECT MAX(id) FROM shift_template),0))+1);
SET @m09_ddl=CONCAT('ALTER TABLE catalog_item AUTO_INCREMENT=',@m09_floor);
PREPARE m09_stmt FROM @m09_ddl;
EXECUTE m09_stmt;
DEALLOCATE PREPARE m09_stmt;

INSERT INTO catalog_item(catalog_type,module,name,status,attributes,version)
SELECT 'M09.DEPARTMENT','M09',name,status,
 JSON_OBJECT('leadStaffId',lead_staff_id,'createdAt',DATE_FORMAT(created_at,'%Y-%m-%dT%H:%i:%s.%fZ'),
 'legacy',JSON_OBJECT('table','department','id',id)),version FROM department ORDER BY id;
-- Giữ mã dài cũ trong legacyCode: code kỹ thuật của bảng chung chỉ dài 40, nguồn cũ cho phép 60/100.
INSERT INTO catalog_item(catalog_type,module,code,name,status,attributes,version)
SELECT 'M09.CREDENTIAL_TYPE','M09',IF(CHAR_LENGTH(code)<=40,code,CONCAT('M09-',LEFT(SHA2(code,256),36))),
 name,status,JSON_OBJECT('credentialGroup',credential_group,'hasExpiry',IF(has_expiry,CAST('true' AS JSON),CAST('false' AS JSON)),
 'requiredForRoles',required_for_roles,'hasPracticeScopes',IF(has_practice_scopes,CAST('true' AS JSON),CAST('false' AS JSON)),
 'legacyCode',code,'createdAt',DATE_FORMAT(created_at,'%Y-%m-%dT%H:%i:%s.%fZ'),
 'legacy',JSON_OBJECT('table','credential_type','id',id)),version FROM credential_type ORDER BY id;
INSERT INTO catalog_item(catalog_type,module,code,name,status,attributes,version)
SELECT 'M09.PRACTICE_SCOPE','M09',IF(CHAR_LENGTH(code)<=40,code,CONCAT('M09-',LEFT(SHA2(code,256),36))),
 name,status,JSON_OBJECT('legacyCode',code,'createdAt',DATE_FORMAT(created_at,'%Y-%m-%dT%H:%i:%s.%fZ'),
 'legacy',JSON_OBJECT('table','practice_scope','id',id)),version FROM practice_scope ORDER BY id;
-- Tên mẫu cũ dài tối đa 255, bảng chung 200: lưu nguyên tên trong originalName; khóa tên kỹ thuật có băm.
INSERT INTO catalog_item(catalog_type,module,name,status,attributes,version)
SELECT 'M09.SHIFT_TEMPLATE','M09',
 IF(CHAR_LENGTH(name)<=200,name,CONCAT(LEFT(name,158),' #',LEFT(SHA2(name,256),40))),status,
 JSON_OBJECT('startTime',TIME_FORMAT(start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(end_time,'%H:%i:%s'),
 'scopeType',scope_type,'requiresHandover',IF(requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),
 'originalName',name,'createdAt',DATE_FORMAT(created_at,'%Y-%m-%dT%H:%i:%s.%fZ'),
 'legacy',JSON_OBJECT('table','shift_template','id',id)),version FROM shift_template ORDER BY id;

SET @m09_floor = (SELECT GREATEST(COALESCE((SELECT MAX(id) FROM simple_record),0),
 COALESCE((SELECT MAX(id) FROM staff_import_batch),0))+1);
SET @m09_ddl=CONCAT('ALTER TABLE simple_record AUTO_INCREMENT=',@m09_floor);
PREPARE m09_stmt FROM @m09_ddl;
EXECUTE m09_stmt;
DEALLOCATE PREPARE m09_stmt;
INSERT INTO simple_record(kind,module,status,content,unique_key,locked_at,created_at,created_by)
SELECT 'M09.STAFF_IMPORT_BATCH','M09','RECORDED',
 JSON_OBJECT('fileName',file_name,'rowCount',row_count,'legacy',JSON_OBJECT('table','staff_import_batch','id',id)),
 CONCAT('M09.STAFF_IMPORT_BATCH:legacy:',id),created_at,created_at,created_by FROM staff_import_batch ORDER BY id;

-- Bỏ chỉ các FK tới bảng legacy; các FK giữa tám bảng giữ nguyên.
DELIMITER $$
CREATE PROCEDURE m09_drop_legacy_fks()
BEGIN
 DECLARE done BOOLEAN DEFAULT FALSE;
 DECLARE tbl VARCHAR(64);
 DECLARE fk VARCHAR(64);
 DECLARE fks CURSOR FOR SELECT DISTINCT TABLE_NAME,CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
  WHERE CONSTRAINT_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME IN
  ('department','credential_type','practice_scope','shift_template','staff_import_batch',
   'department_lead_history','shift_lead_history','shift_publication','floor_manager_assignment',
   'credential_practice_scope','handover_item','handover_assessment','handover_comment','handover_reply',
   'staffing_ratio_calculation','floor_scope_lock','m09_job_run');
 DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=TRUE;
 OPEN fks;
 fk_loop: LOOP
  FETCH fks INTO tbl,fk;
  IF done THEN LEAVE fk_loop; END IF;
  SET @m09_ddl=CONCAT('ALTER TABLE ',tbl,' DROP FOREIGN KEY ',fk);
  PREPARE m09_stmt FROM @m09_ddl;
  EXECUTE m09_stmt;
  DEALLOCATE PREPARE m09_stmt;
 END LOOP;
 CLOSE fks;
END$$
DELIMITER ;
CALL m09_drop_legacy_fks();
DROP PROCEDURE m09_drop_legacy_fks;

UPDATE staff_department d JOIN catalog_item c ON c.catalog_type='M09.DEPARTMENT' AND c.m09_legacy_id=d.department_id
 SET d.department_id=c.id;
UPDATE staff_profile s JOIN simple_record r ON r.kind='M09.STAFF_IMPORT_BATCH' AND r.m09_legacy_id=s.import_batch_id
 SET s.import_batch_id=r.id;
UPDATE credential d JOIN catalog_item c ON c.catalog_type='M09.CREDENTIAL_TYPE' AND c.m09_legacy_id=d.credential_type_id
 SET d.credential_type_id=c.id;
UPDATE work_shift s JOIN catalog_item c ON c.catalog_type='M09.SHIFT_TEMPLATE' AND c.m09_legacy_id=s.template_id
 SET s.template_id=c.id;
ALTER TABLE staff_department ADD CONSTRAINT fk_staff_department_catalog FOREIGN KEY(department_id) REFERENCES catalog_item(id);
ALTER TABLE staff_profile ADD CONSTRAINT fk_staff_import_record FOREIGN KEY(import_batch_id) REFERENCES simple_record(id);
ALTER TABLE credential ADD CONSTRAINT fk_credential_catalog FOREIGN KEY(credential_type_id) REFERENCES catalog_item(id);
ALTER TABLE work_shift ADD CONSTRAINT fk_shift_template_catalog FOREIGN KEY(template_id) REFERENCES catalog_item(id);

ALTER TABLE credential ADD COLUMN practice_scope_ids JSON NOT NULL DEFAULT (JSON_ARRAY());
UPDATE credential c SET practice_scope_ids=COALESCE(
 (SELECT JSON_ARRAYAGG(p.id) FROM credential_practice_scope s
  JOIN catalog_item p ON p.catalog_type='M09.PRACTICE_SCOPE' AND p.m09_legacy_id=s.practice_scope_id
  WHERE s.credential_id=c.id),JSON_ARRAY());

SET @m09_floor = (SELECT GREATEST(COALESCE((SELECT MAX(id) FROM status_history),0),
 COALESCE((SELECT MAX(id) FROM department_lead_history),0),COALESCE((SELECT MAX(id) FROM shift_lead_history),0))+1);
SET @m09_ddl=CONCAT('ALTER TABLE status_history AUTO_INCREMENT=',@m09_floor);
PREPARE m09_stmt FROM @m09_ddl;
EXECUTE m09_stmt;
DEALLOCATE PREPARE m09_stmt;
INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,ends_at,command,actor_id,by_system,reason,details,unique_key)
SELECT 'M09.DEPARTMENT',department_id,'LEAD_STAFF','M09','PERIOD',staff_id,starts_at,ends_at,'LEAD_CHANGED',actor_id,FALSE,reason,
 JSON_OBJECT('legacy',JSON_OBJECT('table','department_lead_history','id',id)),
 CONCAT('M09.DEPARTMENT_LEAD_HISTORY:legacy:',id) FROM department_lead_history ORDER BY id;
INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,ends_at,command,actor_id,by_system,reason,details,unique_key)
SELECT 'M09.SHIFT',shift_id,'LEAD_STAFF','M09','PERIOD',staff_id,starts_at,ends_at,basis,actor_id,FALSE,reason,
 JSON_OBJECT('legacy',JSON_OBJECT('table','shift_lead_history','id',id)),
 CONCAT('M09.SHIFT_LEAD_HISTORY:legacy:',id) FROM shift_lead_history ORDER BY id;

SET @m09_floor = (SELECT GREATEST(COALESCE((SELECT MAX(id) FROM record_event),0),
 COALESCE((SELECT MAX(id) FROM shift_publication),0),COALESCE((SELECT MAX(id) FROM handover_comment),0),
 COALESCE((SELECT MAX(id) FROM handover_reply),0),COALESCE((SELECT MAX(id) FROM staffing_ratio_calculation),0))+1);
SET @m09_ddl=CONCAT('ALTER TABLE record_event AUTO_INCREMENT=',@m09_floor);
PREPARE m09_stmt FROM @m09_ddl;
EXECUTE m09_stmt;
DEALLOCATE PREPARE m09_stmt;
INSERT INTO record_event(entity_type,entity_id,module,kind,content,by_system,occurred_at,confirmed_at,unique_key)
SELECT 'M09.SCHEDULE',schedule_id,'M09','M09.SHIFT_PUBLICATION',
 JSON_OBJECT('content',content,'legacy',JSON_OBJECT('table','shift_publication','id',id)),FALSE,published_at,published_at,
 CONCAT('M09.SHIFT_PUBLICATION:legacy:',id) FROM shift_publication ORDER BY id;
INSERT INTO record_event(entity_type,entity_id,module,kind,content,by_system,occurred_at,confirmed_at,unique_key)
SELECT 'M09.HANDOVER',handover_id,'M09','M09.HANDOVER_COMMENT',
 JSON_OBJECT('content',content,'sender',sender,'legacy',JSON_OBJECT('table','handover_comment','id',id)),
 FALSE,created_at,created_at,CONCAT('M09.HANDOVER_COMMENT:legacy:',id) FROM handover_comment ORDER BY id;
INSERT INTO record_event(entity_type,entity_id,module,kind,content,reply_to_id,by_system,occurred_at,confirmed_at,unique_key)
SELECT 'M09.HANDOVER',c.handover_id,'M09','M09.HANDOVER_REPLY',
 JSON_OBJECT('content',r.content,'sender',r.sender,'legacy',JSON_OBJECT('table','handover_reply','id',r.id)),
 e.id,FALSE,r.created_at,r.created_at,CONCAT('M09.HANDOVER_REPLY:legacy:',r.id)
 FROM handover_reply r JOIN handover_comment c ON c.id=r.comment_id
 JOIN record_event e ON e.kind='M09.HANDOVER_COMMENT' AND e.m09_legacy_id=c.id ORDER BY r.id;
INSERT INTO record_event(entity_type,entity_id,module,kind,content,by_system,occurred_at,confirmed_at,unique_key)
SELECT 'M09.SHIFT',shift_id,'M09','M09.STAFFING_RATIO_CALCULATION',
 JSON_OBJECT('calculatedAt',DATE_FORMAT(calculated_at,'%Y-%m-%dT%H:%i:%s.%fZ'),'event',event,
 'totalWeight',total_weight,'staffCount',staff_count,'ratio',ratio,'threshold',threshold,
 'meetsThreshold',IF(meets_threshold,CAST('true' AS JSON),CAST('false' AS JSON)),
 'legacy',JSON_OBJECT('table','staffing_ratio_calculation','id',id)),FALSE,calculated_at,calculated_at,
 CONCAT('M09.STAFFING_RATIO_CALCULATION:legacy:',id) FROM staffing_ratio_calculation ORDER BY id;

ALTER TABLE staff_assignment
 ADD COLUMN kind ENUM('CARE','FLOOR_MANAGER') NOT NULL DEFAULT 'CARE',
 ADD COLUMN floor_area_id BIGINT NULL,
 ADD COLUMN assigned_by BIGINT NULL,
 ADD COLUMN reason VARCHAR(2000) NULL,
 ADD COLUMN legacy JSON NULL,
 ADD COLUMN legacy_floor_manager_id BIGINT GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(legacy->'$.id') AS UNSIGNED)) STORED,
 ADD UNIQUE KEY uq_staff_assignment_legacy_floor(legacy_floor_manager_id),
 MODIFY COLUMN shift_id BIGINT NULL DEFAULT NULL,
 MODIFY COLUMN target_type ENUM('FLOOR','AREA','ROOM','RESIDENT') NULL DEFAULT NULL,
 MODIFY COLUMN target_id BIGINT NULL DEFAULT NULL,
 MODIFY COLUMN role VARCHAR(40) NULL DEFAULT NULL,
 MODIFY COLUMN assignment_kind ENUM('PRIMARY','SUPPORT') NULL DEFAULT NULL,
 MODIFY COLUMN ends_at DATETIME(6) NULL DEFAULT NULL,
 DROP CHECK staff_assignment_chk_1,
 ADD CONSTRAINT chk_assignment_kind CHECK (
  (kind='CARE' AND shift_id IS NOT NULL AND target_type IS NOT NULL AND target_id IS NOT NULL
   AND role IS NOT NULL AND assignment_kind IS NOT NULL AND ends_at IS NOT NULL AND ends_at>starts_at
   AND floor_area_id IS NULL AND legacy IS NULL)
  OR (kind='FLOOR_MANAGER' AND status IN ('ACTIVE','ENDED') AND floor_area_id IS NOT NULL AND shift_id IS NULL
   AND target_type IS NULL AND target_id IS NULL AND task_type_id IS NULL AND role IS NULL AND assignment_kind IS NULL
   AND (legacy_floor_manager_id IS NOT NULL OR assigned_by IS NOT NULL)
   AND (ends_at IS NULL OR ends_at>starts_at OR (status='ENDED' AND ends_at=starts_at)))),
 ADD KEY ix_assignment_floor(kind,floor_area_id,starts_at,ends_at);
SET @m09_floor = (SELECT GREATEST(COALESCE((SELECT MAX(id) FROM staff_assignment),0),
 COALESCE((SELECT MAX(id) FROM floor_manager_assignment),0))+1);
SET @m09_ddl=CONCAT('ALTER TABLE staff_assignment AUTO_INCREMENT=',@m09_floor);
PREPARE m09_stmt FROM @m09_ddl;
EXECUTE m09_stmt;
DEALLOCATE PREPARE m09_stmt;
INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,ends_at,status,assigned_by,reason,version,created_at,legacy)
SELECT 'FLOOR_MANAGER',staff_id,floor_area_id,starts_at,ends_at,status,assigned_by,reason,version,created_at,
 JSON_OBJECT('table','floor_manager_assignment','id',id) FROM floor_manager_assignment ORDER BY id;

ALTER TABLE shift_handover
 ADD COLUMN items JSON NOT NULL DEFAULT (JSON_ARRAY()),
 ADD COLUMN next_item_id BIGINT NOT NULL DEFAULT 1,
 ADD COLUMN assessments JSON NOT NULL DEFAULT (JSON_ARRAY()),
 ADD COLUMN locked_at DATETIME(6) NULL,
 ADD CONSTRAINT chk_handover_json_arrays CHECK (JSON_TYPE(items)='ARRAY' AND JSON_TYPE(assessments)='ARRAY');
UPDATE shift_handover h SET
 next_item_id=COALESCE((SELECT MAX(id)+1 FROM handover_item i WHERE i.handover_id=h.id),1),
 items=COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',i.id,'itemGroup',i.item_group,'sourceType',i.source_type,
 'sourceId',i.source_id,'residentId',i.resident_id,'roomId',i.room_id,'description',i.description,
 'note',i.note,'noteAuthor',i.note_author,'resolved',IF(i.resolved,CAST('true' AS JSON),CAST('false' AS JSON)),
 'section',i.section,'statusAtFinalization',i.status_at_finalization,
 'createdAt',DATE_FORMAT(i.created_at,'%Y-%m-%dT%H:%i:%s.%fZ'))) FROM handover_item i WHERE i.handover_id=h.id),JSON_ARRAY()),
 assessments=COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('assessmentKind',a.assessment_kind,
 'residentId',a.resident_id,'residentKey',a.resident_key,'content',a.content)) FROM handover_assessment a WHERE a.handover_id=h.id),JSON_ARRAY()),
 locked_at=IF(status='CONFIRMED',confirmed_at,NULL);

INSERT IGNORE INTO technical_lock(lock_key,version,updated_at)
 SELECT CONCAT('M09.TANG:',floor_area_id),0,'1970-01-01 00:00:00' FROM floor_scope_lock;
INSERT INTO reminder_marker(kind,entity_type,entity_id,marker,marked_at,details)
 SELECT CONCAT('M09.JOB.',job),'M09.JOB',0,DATE_FORMAT(scheduled_at,'%Y-%m-%dT%H:%i:%sZ'),COALESCE(finished_at,started_at),
 JSON_OBJECT('scheduledAt',DATE_FORMAT(scheduled_at,'%Y-%m-%dT%H:%i:%s.%fZ'),
 'actualAt',DATE_FORMAT(started_at,'%Y-%m-%dT%H:%i:%s.%fZ'),'result',result,
 'legacy',JSON_OBJECT('table','m09_job_run','job',job,'scheduledAt',CAST(scheduled_at AS CHAR)))
 FROM m09_job_run WHERE result='SUCCEEDED';
-- Giữ dấu lỗi/chưa xong như metadata legacy riêng; không dùng nó để chặn lần thử lại.
INSERT INTO reminder_marker(kind,entity_type,entity_id,marker,marked_at,details)
 SELECT 'M09.JOB.LEGACY','M09.JOB',0,SHA2(CONCAT(job,':',scheduled_at),256),COALESCE(finished_at,started_at),
 JSON_OBJECT('job',job,'scheduledAt',CAST(scheduled_at AS CHAR),'startedAt',CAST(started_at AS CHAR),
 'finishedAt',CAST(finished_at AS CHAR),'result',result,
 'legacy',JSON_OBJECT('table','m09_job_run','job',job,'scheduledAt',CAST(scheduled_at AS CHAR)))
 FROM m09_job_run WHERE result<>'SUCCEEDED' OR result IS NULL;

ALTER TABLE staff_profile ADD COLUMN locked_at DATETIME(6) NULL, COMMENT='DataGroup=MULTIPLE';
ALTER TABLE staff_department COMMENT='DataGroup=RECORD';
ALTER TABLE credential COMMENT='DataGroup=CATALOG';
ALTER TABLE shift_schedule COMMENT='DataGroup=BUSINESS';
ALTER TABLE work_shift COMMENT='DataGroup=BUSINESS';
ALTER TABLE shift_staff COMMENT='DataGroup=BUSINESS';
ALTER TABLE staff_assignment COMMENT='DataGroup=BUSINESS';
ALTER TABLE shift_handover COMMENT='DataGroup=MULTIPLE';

DELIMITER $$
CREATE TRIGGER trg_staff_profile_row_lock BEFORE UPDATE ON staff_profile FOR EACH ROW
BEGIN
 IF OLD.locked_at IS NOT NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='staff original is locked'; END IF;
END$$
CREATE TRIGGER trg_staff_department_no_update BEFORE UPDATE ON staff_department FOR EACH ROW
BEGIN
 IF OLD.ends_at IS NOT NULL OR NEW.ends_at IS NULL OR NEW.ends_at<OLD.starts_at
 OR NOT(OLD.id<=>NEW.id) OR NOT(OLD.staff_id<=>NEW.staff_id)
 OR NOT(OLD.department_id<=>NEW.department_id) OR NOT(OLD.starts_at<=>NEW.starts_at)
 OR NOT(OLD.actor_id<=>NEW.actor_id) OR NOT(OLD.reason<=>NEW.reason) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='staff department history closes once (FR-001a, DBR-36)';
 END IF;
END$$
CREATE TRIGGER trg_credential_protected_fields BEFORE UPDATE ON credential FOR EACH ROW
BEGIN
 IF COALESCE(@correct_credential,0)<>1 AND (
  NOT(OLD.id<=>NEW.id) OR NOT(OLD.staff_id<=>NEW.staff_id) OR NOT(OLD.credential_type_id<=>NEW.credential_type_id)
  OR NOT(OLD.credential_number<=>NEW.credential_number) OR NOT(OLD.issuing_authority<=>NEW.issuing_authority)
  OR NOT(OLD.issue_date<=>NEW.issue_date) OR NOT(OLD.expiry_date<=>NEW.expiry_date)
  OR NOT(OLD.practice_scope_ids<=>NEW.practice_scope_ids) OR NOT(OLD.replaces_id<=>NEW.replaces_id) OR NOT(OLD.scan_file_id<=>NEW.scan_file_id)
  OR NOT(OLD.created_at<=>NEW.created_at) OR NOT(OLD.created_by<=>NEW.created_by)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='credential descriptive fields require correction (FR-006)';
 END IF;
END$$
CREATE TRIGGER trg_work_shift_update BEFORE UPDATE ON work_shift FOR EACH ROW
BEGIN
 IF OLD.status IN ('CLOSED','CANCELLED') AND (
 NOT(OLD.id<=>NEW.id) OR NOT(OLD.schedule_id<=>NEW.schedule_id) OR NOT(OLD.template_id<=>NEW.template_id)
 OR NOT(OLD.starts_at<=>NEW.starts_at) OR NOT(OLD.ends_at<=>NEW.ends_at)
 OR NOT(OLD.scope_type<=>NEW.scope_type) OR NOT(OLD.scope_id<=>NEW.scope_id)
 OR NOT(OLD.requires_handover<=>NEW.requires_handover) OR NOT(OLD.status<=>NEW.status)
 OR NOT(OLD.lead_staff_id<=>NEW.lead_staff_id) OR NOT(OLD.missing_lead<=>NEW.missing_lead)
 OR NOT(OLD.below_threshold<=>NEW.below_threshold) OR NOT(OLD.cancellation_reason<=>NEW.cancellation_reason)
 OR NOT(OLD.version<=>NEW.version)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='closed or cancelled shift is immutable (FR-022)';
 END IF;
 IF OLD.status<>'DRAFT' AND (NOT(OLD.starts_at<=>NEW.starts_at) OR NOT(OLD.ends_at<=>NEW.ends_at)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='published shift times are immutable (FR-017)';
 END IF;
END$$
CREATE TRIGGER trg_staff_assignment_update BEFORE UPDATE ON staff_assignment FOR EACH ROW
BEGIN
 IF OLD.status IN ('ENDED','CANCELLED') OR NOT(OLD.kind<=>NEW.kind) OR NOT(OLD.legacy<=>NEW.legacy) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='terminal assignment and its logical identity are immutable (FR-024/027)';
 END IF;
END$$
CREATE TRIGGER trg_staff_assignment_insert BEFORE INSERT ON staff_assignment FOR EACH ROW
BEGIN
 IF NEW.legacy IS NOT NULL AND SUBSTRING_INDEX(USER(),'@',1)='eldercare_app' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='legacy identity is written only by migration';
 END IF;
END$$
CREATE TRIGGER trg_staff_assignment_delete BEFORE DELETE ON staff_assignment FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' AND
  (OLD.kind='FLOOR_MANAGER' OR OLD.status<>'DRAFT' OR (SELECT status FROM work_shift WHERE id=OLD.shift_id)<>'DRAFT') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='only draft care assignments can be deleted (FR-025)';
 END IF;
END$$
CREATE TRIGGER trg_shift_handover_row_lock BEFORE UPDATE ON shift_handover FOR EACH ROW
BEGIN
 DECLARE i INT DEFAULT 0;
 DECLARE j INT DEFAULT 0;
 DECLARE old_item JSON;
 DECLARE new_item JSON;
 DECLARE found BOOLEAN;
 IF OLD.locked_at IS NOT NULL OR OLD.status='CONFIRMED' OR NOT(OLD.shift_id<=>NEW.shift_id)
 OR (OLD.finalized_at IS NOT NULL AND
  (NOT(OLD.finalized_at<=>NEW.finalized_at) OR NOT(OLD.finalized_by<=>NEW.finalized_by)
   OR NOT(OLD.source_keys_at_finalization<=>NEW.source_keys_at_finalization)
   OR NOT(OLD.assessments<=>NEW.assessments))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover original is locked (FR-041/044)';
 END IF;
 -- Phần đã lập giữ nguyên; chỉ thêm mục AFTER_PREPARATION. Khi nháp, nguồn chỉ số/biến động không được bỏ/sửa.
 WHILE i<JSON_LENGTH(OLD.items) DO
  SET old_item=JSON_EXTRACT(OLD.items,CONCAT('$[',i,']'));
  SET j=0;
  SET found=FALSE;
  WHILE j<JSON_LENGTH(NEW.items) DO
   SET new_item=JSON_EXTRACT(NEW.items,CONCAT('$[',j,']'));
   IF JSON_EXTRACT(old_item,'$.id')=JSON_EXTRACT(new_item,'$.id') THEN
    SET found=TRUE;
    IF (OLD.finalized_at IS NOT NULL OR JSON_UNQUOTE(JSON_EXTRACT(old_item,'$.section'))='AFTER_PREPARATION') AND NOT(old_item<=>new_item) THEN
     SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='finalized handover item is immutable (FR-041)';
    END IF;
    IF JSON_UNQUOTE(JSON_EXTRACT(old_item,'$.itemGroup')) IN ('VITAL_SIGN','RESIDENT_CHANGE') AND
     (NOT(JSON_EXTRACT(old_item,'$.itemGroup')<=>JSON_EXTRACT(new_item,'$.itemGroup')) OR NOT(JSON_EXTRACT(old_item,'$.sourceType')<=>JSON_EXTRACT(new_item,'$.sourceType'))
     OR NOT(JSON_EXTRACT(old_item,'$.sourceId')<=>JSON_EXTRACT(new_item,'$.sourceId')) OR NOT(JSON_EXTRACT(old_item,'$.residentId')<=>JSON_EXTRACT(new_item,'$.residentId'))
     OR NOT(JSON_EXTRACT(old_item,'$.roomId')<=>JSON_EXTRACT(new_item,'$.roomId'))) THEN
     SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='critical handover source is immutable (FR-040)';
    END IF;
   END IF;
   SET j=j+1;
  END WHILE;
  IF NOT found AND (OLD.finalized_at IS NOT NULL OR JSON_UNQUOTE(JSON_EXTRACT(old_item,'$.itemGroup')) IN ('VITAL_SIGN','RESIDENT_CHANGE')) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover item cannot be discarded (FR-040/041)';
  END IF;
  SET i=i+1;
 END WHILE;
 SET i=0;
 WHILE i<JSON_LENGTH(NEW.items) DO
  SET new_item=JSON_EXTRACT(NEW.items,CONCAT('$[',i,']'));
  IF OLD.finalized_at IS NULL AND JSON_UNQUOTE(JSON_EXTRACT(new_item,'$.section'))='AFTER_PREPARATION' THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='after-preparation item requires prepared parent (FR-041)';
  END IF;
  IF COALESCE(JSON_TYPE(JSON_EXTRACT(new_item,'$.residentId')),'NULL')='NULL' AND COALESCE(JSON_TYPE(JSON_EXTRACT(new_item,'$.roomId')),'NULL')='NULL' THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover item requires resident or room';
  END IF;
  SET j=i+1;
  WHILE j<JSON_LENGTH(NEW.items) DO
   IF JSON_EXTRACT(new_item,'$.id')=JSON_EXTRACT(NEW.items,CONCAT('$[',j,'].id')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover item id is unique in parent';
   END IF;
   SET j=j+1;
  END WHILE;
  IF OLD.finalized_at IS NOT NULL AND NOT JSON_CONTAINS(OLD.items,JSON_OBJECT('id',JSON_EXTRACT(new_item,'$.id'))) AND
   JSON_UNQUOTE(JSON_EXTRACT(new_item,'$.section'))<>'AFTER_PREPARATION' THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='new item must be after preparation (FR-044)';
  END IF;
  SET i=i+1;
 END WHILE;
 IF NEW.status='CONFIRMED' THEN SET NEW.locked_at=NEW.confirmed_at; END IF;
END$$
CREATE TRIGGER trg_shift_handover_insert BEFORE INSERT ON shift_handover FOR EACH ROW
BEGIN
 IF NEW.status='CONFIRMED' THEN SET NEW.locked_at=NEW.confirmed_at; END IF;
END$$
CREATE TRIGGER trg_shift_handover_no_delete BEFORE DELETE ON shift_handover FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='handover removed only by retention (FR-047)';
 END IF;
END$$
DELIMITER ;

-- Thu hồi quyền riêng trên bảng cũ rồi mới bỏ bảng.
REVOKE INSERT,UPDATE,DELETE ON department FROM 'eldercare_app'@'%';
REVOKE INSERT,UPDATE(ends_at) ON department_lead_history FROM 'eldercare_app'@'%';
REVOKE INSERT,UPDATE,DELETE ON credential_type FROM 'eldercare_app'@'%';
REVOKE INSERT,UPDATE,DELETE ON practice_scope FROM 'eldercare_app'@'%';
REVOKE INSERT,DELETE ON credential_practice_scope FROM 'eldercare_app'@'%';
REVOKE DELETE ON department FROM 'eldercare_retention'@'%';
REVOKE DELETE ON department_lead_history FROM 'eldercare_retention'@'%';
REVOKE DELETE ON credential_type FROM 'eldercare_retention'@'%';
REVOKE DELETE ON practice_scope FROM 'eldercare_retention'@'%';
REVOKE DELETE ON credential_practice_scope FROM 'eldercare_retention'@'%';
REVOKE INSERT,UPDATE,DELETE ON shift_template FROM 'eldercare_app'@'%';
REVOKE INSERT,UPDATE(ends_at) ON shift_lead_history FROM 'eldercare_app'@'%';
REVOKE INSERT ON shift_publication FROM 'eldercare_app'@'%';
REVOKE DELETE ON shift_template FROM 'eldercare_retention'@'%';
REVOKE DELETE ON shift_lead_history FROM 'eldercare_retention'@'%';
REVOKE DELETE ON shift_publication FROM 'eldercare_retention'@'%';
REVOKE INSERT,UPDATE ON floor_scope_lock FROM 'eldercare_app'@'%';
REVOKE INSERT,UPDATE ON floor_manager_assignment FROM 'eldercare_app'@'%';
REVOKE DELETE ON floor_manager_assignment FROM 'eldercare_retention'@'%';
REVOKE INSERT,UPDATE,DELETE ON handover_item FROM 'eldercare_app'@'%';
REVOKE INSERT,UPDATE,DELETE ON handover_assessment FROM 'eldercare_app'@'%';
REVOKE INSERT ON handover_comment FROM 'eldercare_app'@'%';
REVOKE INSERT ON handover_reply FROM 'eldercare_app'@'%';
REVOKE DELETE ON handover_item FROM 'eldercare_retention'@'%';
REVOKE DELETE ON handover_assessment FROM 'eldercare_retention'@'%';
REVOKE DELETE ON handover_comment FROM 'eldercare_retention'@'%';
REVOKE DELETE ON handover_reply FROM 'eldercare_retention'@'%';
REVOKE INSERT ON staff_import_batch FROM 'eldercare_app'@'%';
REVOKE DELETE ON staff_import_batch FROM 'eldercare_retention'@'%';
REVOKE INSERT ON staffing_ratio_calculation FROM 'eldercare_app'@'%';
REVOKE DELETE ON staffing_ratio_calculation FROM 'eldercare_retention'@'%';
DROP TABLE handover_reply;
DROP TABLE handover_comment;
DROP TABLE handover_assessment;
DROP TABLE handover_item;
DROP TABLE credential_practice_scope;
DROP TABLE shift_publication;
DROP TABLE shift_lead_history;
DROP TABLE department_lead_history;
DROP TABLE floor_manager_assignment;
DROP TABLE staffing_ratio_calculation;
DROP TABLE staff_import_batch;
DROP TABLE floor_scope_lock;
DROP TABLE m09_job_run;
DROP TABLE department;
DROP TABLE credential_type;
DROP TABLE practice_scope;
DROP TABLE shift_template;
