-- T012/R-15: hai bảng riêng của kế hoạch; người tham gia/ý kiến ở record_event của 000.
-- DATETIME của kế hoạch là ngày/giờ nghiệp vụ Asia/Ho_Chi_Minh (005 DBR-25).
CREATE TABLE care_plan (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 resident_id BIGINT NOT NULL,
 revision INT NOT NULL,
 status ENUM('DRAFT','PENDING_APPROVAL','PENDING_EFFECT','EFFECTIVE','EXPIRED','CANCELLED') NOT NULL,
 effective_from DATE NULL,
 effective_to DATE NULL,
 previous_plan_id BIGINT NULL,
 change_reason VARCHAR(2000) NULL,
 created_by BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 approved_by BIGINT NULL,
 approved_at DATETIME(6) NULL,
 self_approved BOOLEAN NOT NULL DEFAULT FALSE,
 submitted_at DATETIME(6) NULL,
 reminded_at DATETIME(6) NULL,
 escalated_at DATETIME(6) NULL,
 end_reason ENUM('NEW_REVISION','FINAL_STATUS','RETURNED','CANCELLED','WITHDRAWN') NULL,
 version INT NOT NULL DEFAULT 0,
 open_resident BIGINT GENERATED ALWAYS AS(IF(status IN('DRAFT','PENDING_APPROVAL','PENDING_EFFECT'),resident_id,NULL)) STORED,
 effective_resident BIGINT GENERATED ALWAYS AS(IF(status='EFFECTIVE',resident_id,NULL)) STORED,
 UNIQUE KEY uq_care_plan_revision(resident_id,revision),
 UNIQUE KEY uq_care_plan_open(open_resident),
 UNIQUE KEY uq_care_plan_effective(effective_resident),
 CONSTRAINT fk_care_plan_previous FOREIGN KEY(previous_plan_id) REFERENCES care_plan(id) ON DELETE RESTRICT,
 CONSTRAINT chk_care_plan_revision CHECK(revision>0),
 CONSTRAINT chk_care_plan_approval CHECK(approved_at IS NULL OR (effective_from IS NOT NULL AND effective_from>DATE(approved_at))),
 CONSTRAINT chk_care_plan_period CHECK(effective_to IS NULL OR (effective_from IS NOT NULL AND effective_to>=effective_from)),
 INDEX ix_care_plan_due(status,effective_from),
 INDEX ix_care_plan_resident_date(resident_id,effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DataGroup=BUSINESS';

CREATE TABLE care_plan_item (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 care_plan_id BIGINT NOT NULL,
 kind ENUM('TASK','RISK_AGREEMENT') NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 source_item_id BIGINT NULL,
 task_type_id BIGINT NULL,
 frequency_type ENUM('TIMES_PER_DAY','EVERY_N_HOURS','DAYS_OF_WEEK','ONCE_PER_SHIFT') NULL,
 interval_hours SMALLINT NULL,
 starts_at TIME NULL,
 data JSON NOT NULL,
 choice VARCHAR(2000) NULL,
 discussed_risks VARCHAR(2000) NULL,
 resident_participates BOOLEAN NULL,
 assessing_doctor_id BIGINT NULL,
 can_self_determine BOOLEAN NULL,
 support_method VARCHAR(2000) NULL,
 assessment_at DATETIME(6) NULL,
 assessment_review_date DATE NULL,
 decision_maker_type ENUM('RESIDENT','PATIENT_REPRESENTATIVE','DOCTOR') NULL,
 decision_maker_family_id BIGINT NULL,
 decision_maker_doctor_id BIGINT NULL,
 doctor_decision_reason VARCHAR(1000) NULL,
 review_date DATE NULL,
 recorded_by BIGINT NULL,
 recorded_at DATETIME(6) NULL,
 CONSTRAINT fk_care_plan_item_plan FOREIGN KEY(care_plan_id) REFERENCES care_plan(id) ON DELETE RESTRICT,
 CONSTRAINT fk_care_plan_item_source FOREIGN KEY(source_item_id) REFERENCES care_plan_item(id) ON DELETE RESTRICT,
 CONSTRAINT fk_care_plan_item_catalog FOREIGN KEY(task_type_id) REFERENCES catalog_item(id) ON DELETE RESTRICT,
 CONSTRAINT chk_care_plan_item_kind CHECK(
  (kind='TASK' AND task_type_id IS NOT NULL AND frequency_type IS NOT NULL AND choice IS NULL)
  OR (kind='RISK_AGREEMENT' AND task_type_id IS NULL AND frequency_type IS NULL AND interval_hours IS NULL AND starts_at IS NULL
   AND choice IS NOT NULL AND discussed_risks IS NOT NULL AND resident_participates IS NOT NULL
   AND assessing_doctor_id IS NOT NULL AND can_self_determine IS NOT NULL
   AND support_method IS NOT NULL AND CHAR_LENGTH(TRIM(support_method))>0
   AND assessment_at IS NOT NULL AND assessment_review_date IS NOT NULL
   AND decision_maker_type IS NOT NULL AND review_date IS NOT NULL
   AND (resident_participates OR NOT can_self_determine)
   AND (can_self_determine=(decision_maker_type='RESIDENT'))
   AND ((decision_maker_type='RESIDENT' AND decision_maker_family_id IS NULL AND decision_maker_doctor_id IS NULL)
    OR (decision_maker_type='PATIENT_REPRESENTATIVE' AND decision_maker_family_id IS NOT NULL AND decision_maker_doctor_id IS NULL)
    OR (decision_maker_type='DOCTOR' AND decision_maker_family_id IS NULL AND decision_maker_doctor_id IS NOT NULL
     AND doctor_decision_reason IS NOT NULL AND CHAR_LENGTH(TRIM(doctor_decision_reason))>0)))
 ),
 CONSTRAINT chk_care_plan_item_interval CHECK(frequency_type IS NULL OR frequency_type<>'EVERY_N_HOURS'
  OR (interval_hours IS NOT NULL AND interval_hours BETWEEN 1 AND 23 AND starts_at IS NOT NULL)),
 INDEX ix_care_plan_item_revision(care_plan_id,kind,active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DataGroup=BUSINESS';

DELIMITER $$
CREATE TRIGGER trg_care_plan_item_parent_insert BEFORE INSERT ON care_plan_item FOR EACH ROW
BEGIN
 DECLARE parent_status VARCHAR(30);
 SELECT status INTO parent_status FROM care_plan WHERE id=NEW.care_plan_id FOR SHARE;
 IF parent_status IS NULL OR parent_status<>'DRAFT' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='REVISION_LOCKED'; END IF;
END$$
CREATE TRIGGER trg_care_plan_item_parent_update BEFORE UPDATE ON care_plan_item FOR EACH ROW
BEGIN
 DECLARE parent_status VARCHAR(30);
 SELECT status INTO parent_status FROM care_plan WHERE id=OLD.care_plan_id FOR SHARE;
 IF parent_status IS NULL OR parent_status<>'DRAFT' OR NEW.care_plan_id<>OLD.care_plan_id OR NEW.kind<>OLD.kind
 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='REVISION_LOCKED'; END IF;
END$$
CREATE TRIGGER trg_care_plan_item_parent_delete BEFORE DELETE ON care_plan_item FOR EACH ROW
BEGIN
 DECLARE parent_status VARCHAR(30);
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SELECT status INTO parent_status FROM care_plan WHERE id=OLD.care_plan_id FOR SHARE;
  IF parent_status IS NULL OR parent_status<>'DRAFT' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='REVISION_LOCKED'; END IF;
 END IF;
END$$
-- Guard chỉ cho hai loại M04; không đổi hành vi của loại record_event thuộc module khác.
CREATE TRIGGER trg_care_risk_event_parent_insert BEFORE INSERT ON record_event FOR EACH ROW
BEGIN
 DECLARE parent_status VARCHAR(30);
 IF NEW.kind IN('M04.RISK_PARTICIPANT','M04.RISK_OPINION') THEN
  SELECT p.status INTO parent_status FROM care_plan p JOIN care_plan_item i ON i.care_plan_id=p.id
   WHERE i.id=NEW.entity_id AND i.kind='RISK_AGREEMENT' AND i.active AND NEW.entity_type='M04.RISK_AGREEMENT' FOR SHARE;
  IF parent_status IS NULL OR parent_status<>'DRAFT' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='REVISION_LOCKED'; END IF;
 END IF;
END$$
CREATE TRIGGER trg_care_risk_event_parent_update BEFORE UPDATE ON record_event FOR EACH ROW
BEGIN
 DECLARE parent_status VARCHAR(30);
 IF OLD.kind IN('M04.RISK_PARTICIPANT','M04.RISK_OPINION') THEN
  SELECT p.status INTO parent_status FROM care_plan p JOIN care_plan_item i ON i.care_plan_id=p.id
   WHERE i.id=OLD.entity_id AND i.kind='RISK_AGREEMENT' AND i.active AND OLD.entity_type='M04.RISK_AGREEMENT' FOR SHARE;
  IF parent_status IS NULL OR parent_status<>'DRAFT' OR NEW.kind<>OLD.kind OR NEW.entity_type<>OLD.entity_type OR NEW.entity_id<>OLD.entity_id
  THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='REVISION_LOCKED'; END IF;
 END IF;
END$$
CREATE TRIGGER trg_care_risk_event_parent_delete BEFORE DELETE ON record_event FOR EACH ROW
BEGIN
 DECLARE parent_status VARCHAR(30);
 IF OLD.kind IN('M04.RISK_PARTICIPANT','M04.RISK_OPINION') AND SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SELECT p.status INTO parent_status FROM care_plan p JOIN care_plan_item i ON i.care_plan_id=p.id
   WHERE i.id=OLD.entity_id AND i.kind='RISK_AGREEMENT' AND OLD.entity_type='M04.RISK_AGREEMENT' FOR SHARE;
  IF parent_status IS NULL OR parent_status<>'DRAFT' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='REVISION_LOCKED'; END IF;
 END IF;
END$$
DELIMITER ;
GRANT INSERT,UPDATE,DELETE ON care_plan TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON care_plan_item TO 'eldercare_app'@'%';
GRANT DELETE ON care_plan TO 'eldercare_retention'@'%';
GRANT DELETE ON care_plan_item TO 'eldercare_retention'@'%';
