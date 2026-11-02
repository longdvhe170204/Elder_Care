-- T005a, R-15: ba bảng riêng M11; lịch sử, trạng thái NCT, marker chờ bảng/API chung của A.
-- Thời điểm do caller truyền từ BusinessTime/Clock; tên bảng/cột theo glossary 010.
CREATE TABLE cost_statement (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 kind ENUM('PERIOD','STATEMENT') NOT NULL,
 period_month CHAR(7) NULL,
 period_starts_on DATE NULL,
 period_ends_on DATE NULL,
 period_id BIGINT NULL,
 resident_id BIGINT NULL,
 statement_type ENUM('REGULAR','SUPPLEMENTARY') NULL,
 final_period BOOLEAN NOT NULL DEFAULT FALSE,
 through_date DATE NULL,
 status ENUM('OPEN','AWAITING_FINALIZATION','FINALIZED') NOT NULL DEFAULT 'OPEN',
 finalization_due_on DATE NULL,
 total DECIMAL(15,0) NULL,
 totals_by_type JSON NULL,
 submitted_by BIGINT NULL,
 submitted_at DATETIME(6) NULL,
 change_warning JSON NULL,
 finalized_by BIGINT NULL,
 finalized_at DATETIME(6) NULL,
 first_export_id BIGINT NULL,
 locked_at DATETIME(6) NULL,
 unique_key VARCHAR(200) COLLATE utf8mb4_bin NOT NULL,
 version INT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 created_by BIGINT NULL,
 CONSTRAINT uq_cost_statement_key UNIQUE(unique_key),
 CONSTRAINT fk_cost_statement_period FOREIGN KEY(period_id) REFERENCES cost_statement(id),
 CONSTRAINT chk_cost_statement_shape CHECK(
   (kind='PERIOD' AND period_month IS NOT NULL AND period_starts_on IS NOT NULL
    AND period_ends_on IS NOT NULL AND period_id IS NULL AND resident_id IS NULL
    AND statement_type IS NULL AND final_period=FALSE AND through_date IS NULL
    AND total IS NULL AND totals_by_type IS NULL AND first_export_id IS NULL
    AND DAYOFMONTH(period_starts_on)=1 AND period_ends_on=LAST_DAY(period_starts_on)
    AND DATE_FORMAT(period_starts_on,'%Y-%m')=period_month)
   OR
   (kind='STATEMENT' AND period_month IS NULL AND period_starts_on IS NULL
    AND period_ends_on IS NULL AND period_id IS NOT NULL AND resident_id IS NOT NULL
    AND statement_type IS NOT NULL)),
 CONSTRAINT chk_cost_statement_key CHECK(unique_key=CASE WHEN kind='PERIOD'
   THEN CONCAT('PERIOD:',period_month)
   ELSE CONCAT('STATEMENT:',period_id,':',resident_id,':',statement_type) END),
 CONSTRAINT chk_cost_statement_lock CHECK(
   (status='FINALIZED' AND locked_at IS NOT NULL AND finalized_at IS NOT NULL)
   OR (status<>'FINALIZED' AND locked_at IS NULL AND finalized_at IS NULL)),
 CONSTRAINT chk_cost_statement_final_totals CHECK(kind<>'STATEMENT' OR status<>'FINALIZED'
   OR (total IS NOT NULL AND totals_by_type IS NOT NULL)),
 CONSTRAINT chk_cost_statement_totals CHECK(totals_by_type IS NULL
   OR JSON_SCHEMA_VALID('{"type":"array","items":{"type":"object","required":["type","total"],"additionalProperties":false,"properties":{"type":{"type":"string","enum":["RESIDENCY_FEE","DAY_CARE_SESSION_FEE","SERVICE","CONSUMABLE","ACTIVITY","MEDICATION","FAMILY_OVERNIGHT_STAY","FAMILY_MEAL","PURCHASE_ASSISTANCE","OTHER"]},"total":{"type":"integer"}}}}',totals_by_type)),
 INDEX ix_cost_statement_period_status(period_id,status),
 INDEX ix_cost_statement_resident_period(resident_id,period_id),
 INDEX ix_cost_statement_due(kind,status,finalization_due_on)
);

CREATE TABLE cost_item (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 resident_id BIGINT NOT NULL,
 statement_id BIGINT NOT NULL,
 period_id BIGINT NOT NULL,
 cost_type ENUM('RESIDENCY_FEE','DAY_CARE_SESSION_FEE','SERVICE','CONSUMABLE','ACTIVITY','MEDICATION','FAMILY_OVERNIGHT_STAY','FAMILY_MEAL','PURCHASE_ASSISTANCE','OTHER') NOT NULL,
 origin ENUM('EVENT_BASED','MANUAL','PURCHASE_ASSISTANCE','ADJUSTMENT') NOT NULL,
 source_type ENUM('RESIDENCY_DAY','DAY_CARE_SESSION','CARE_TASK_RESULT','DOSE','PRN_ADMINISTRATION','DISPENSE_RECORD','FAMILY_MEAL','OVERNIGHT_STAY','ACTIVITY_ATTENDANCE') NULL,
 source_id VARCHAR(128) COLLATE utf8mb4_bin NULL,
 billing_unit VARCHAR(32) COLLATE utf8mb4_bin NULL,
 purchase_assistance_id BIGINT NULL,
 service_id BIGINT NULL,
 item_code VARCHAR(100) NULL,
 description VARCHAR(2000) NOT NULL,
 incurred_on DATE NOT NULL,
 occurred_at DATETIME(6) NULL,
 quantity DECIMAL(10,2) NOT NULL,
 unit VARCHAR(40) NOT NULL,
 unit_price DECIMAL(15,0) NULL,
 factor_percent DECIMAL(5,2) NULL,
 amount DECIMAL(15,0) NULL,
 unit_price_basis JSON NULL,
 source_values JSON NULL,
 flags SET('INCLUDED_IN_PACKAGE','MISSING_UNIT_PRICE','NOT_YET_MOVED_IN','CREATED_IN_ADVANCE','AFTER_FINALIZED_PERIOD','EXCEEDS_CONSENT','URGENT_EXCESS','AFFECTS_TERMINATION') NOT NULL DEFAULT '',
 reason VARCHAR(2000) NULL,
 original_item_id BIGINT NULL,
 original_period_id BIGINT NULL,
 adjustment_type ENUM('POST_FINALIZATION_CORRECTION','SOURCE_CHANGE','WAIVER','LATE_ADDITION') NULL,
 source_change_id BIGINT NULL,
 status ENUM('DRAFT','CHECKED','APPROVED','FINALIZED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
 checked_by BIGINT NULL,
 checked_at DATETIME(6) NULL,
 approved_by BIGINT NULL,
 approved_at DATETIME(6) NULL,
 excess_approval_reason VARCHAR(2000) NULL,
 cancellation_reason VARCHAR(2000) NULL,
 cancelled_by BIGINT NULL,
 unique_key VARCHAR(200) COLLATE utf8mb4_bin NULL,
 locked_at DATETIME(6) NULL,
 version INT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 created_by BIGINT NOT NULL,
 CONSTRAINT uq_cost_item_key UNIQUE(unique_key),
 CONSTRAINT fk_cost_item_statement FOREIGN KEY(statement_id) REFERENCES cost_statement(id),
 CONSTRAINT fk_cost_item_period FOREIGN KEY(period_id) REFERENCES cost_statement(id),
 CONSTRAINT fk_cost_item_original FOREIGN KEY(original_item_id) REFERENCES cost_item(id),
 CONSTRAINT fk_cost_item_original_period FOREIGN KEY(original_period_id) REFERENCES cost_statement(id),
 CONSTRAINT chk_cost_item_origin CHECK(
   (origin='EVENT_BASED' AND source_type IS NOT NULL AND source_id IS NOT NULL AND billing_unit IS NOT NULL AND purchase_assistance_id IS NULL
      AND original_item_id IS NULL AND original_period_id IS NULL)
   OR (origin='MANUAL' AND source_type IS NULL AND source_id IS NULL AND billing_unit IS NULL AND purchase_assistance_id IS NULL
      AND original_item_id IS NULL AND original_period_id IS NULL AND reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason))>0)
   OR (origin='PURCHASE_ASSISTANCE' AND source_type IS NULL AND source_id IS NULL AND billing_unit IS NULL AND purchase_assistance_id IS NOT NULL
      AND original_item_id IS NULL AND original_period_id IS NULL)
   OR (origin='ADJUSTMENT' AND purchase_assistance_id IS NULL AND original_period_id IS NOT NULL
      AND reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason))>0 AND ((original_item_id IS NOT NULL AND source_type IS NULL AND source_id IS NULL AND billing_unit IS NULL)
        OR (original_item_id IS NULL AND source_type IS NOT NULL AND source_id IS NOT NULL AND billing_unit IS NOT NULL)))),
 CONSTRAINT chk_cost_item_key CHECK(
   (origin='EVENT_BASED' AND status<>'CANCELLED' AND unique_key IS NOT NULL
      AND unique_key=CONCAT(source_type,':',source_id,':',billing_unit))
   OR ((origin<>'EVENT_BASED' OR status='CANCELLED') AND unique_key IS NULL)),
 CONSTRAINT chk_cost_item_unpriced CHECK(
   (FIND_IN_SET('MISSING_UNIT_PRICE',flags)>0 AND amount IS NULL)
   OR (FIND_IN_SET('MISSING_UNIT_PRICE',flags)=0 AND amount IS NOT NULL)),
 CONSTRAINT chk_cost_item_lock CHECK(
   (status IN ('FINALIZED','CANCELLED') AND locked_at IS NOT NULL)
   OR (status NOT IN ('FINALIZED','CANCELLED') AND locked_at IS NULL)),
 INDEX ix_cost_item_statement_status(statement_id,status),
 INDEX ix_cost_item_resident_date(resident_id,incurred_on),
 INDEX ix_cost_item_source(source_type,source_id),
 INDEX ix_cost_item_original(original_item_id),
 INDEX ix_cost_item_service_status(service_id,status)
);

CREATE TABLE cost_source_index (
 source_type ENUM('CARE_TASK_RESULT','DOSE','PRN_ADMINISTRATION','DISPENSE_RECORD','FAMILY_MEAL','OVERNIGHT_STAY','ACTIVITY_ATTENDANCE') NOT NULL,
 source_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 billing_unit VARCHAR(32) COLLATE utf8mb4_bin NOT NULL,
 resident_id BIGINT NOT NULL,
 incurred_on DATE NOT NULL,
 occurred_at DATETIME(6) NOT NULL,
 service_id BIGINT NULL,
 medication_id BIGINT NULL,
 current_quantity DECIMAL(10,2) NOT NULL,
 cancelled BOOLEAN NOT NULL DEFAULT FALSE,
 updated_at DATETIME(6) NOT NULL,
 version INT NOT NULL DEFAULT 0,
 PRIMARY KEY(source_type,source_id,billing_unit),
 INDEX ix_cost_source_resident_date(resident_id,source_type,incurred_on)
);

DELIMITER $$
CREATE TRIGGER trg_cost_statement_insert BEFORE INSERT ON cost_statement FOR EACH ROW
BEGIN
 IF NEW.kind='STATEMENT' AND NOT EXISTS(
   SELECT 1 FROM cost_statement WHERE id=NEW.period_id AND kind='PERIOD') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_INVALID_PERIOD';
 END IF;
END$$

CREATE TRIGGER trg_cost_statement_row_lock BEFORE UPDATE ON cost_statement FOR EACH ROW
BEGIN
 IF OLD.locked_at IS NOT NULL THEN
  IF NOT (OLD.id <=> NEW.id)
     OR NOT (OLD.kind <=> NEW.kind)
     OR NOT (OLD.period_month <=> NEW.period_month)
     OR NOT (OLD.period_starts_on <=> NEW.period_starts_on)
     OR NOT (OLD.period_ends_on <=> NEW.period_ends_on)
     OR NOT (OLD.period_id <=> NEW.period_id)
     OR NOT (OLD.resident_id <=> NEW.resident_id)
     OR NOT (OLD.statement_type <=> NEW.statement_type)
     OR NOT (OLD.final_period <=> NEW.final_period)
     OR NOT (OLD.through_date <=> NEW.through_date)
     OR NOT (OLD.status <=> NEW.status)
     OR NOT (OLD.total <=> NEW.total)
     OR NOT (OLD.totals_by_type <=> NEW.totals_by_type)
     OR NOT (OLD.submitted_by <=> NEW.submitted_by)
     OR NOT (OLD.submitted_at <=> NEW.submitted_at)
     OR NOT (OLD.change_warning <=> NEW.change_warning)
     OR NOT (OLD.finalized_by <=> NEW.finalized_by)
     OR NOT (OLD.finalized_at <=> NEW.finalized_at)
     OR NOT (OLD.locked_at <=> NEW.locked_at)
     OR NOT (OLD.unique_key <=> NEW.unique_key)
     OR NOT (OLD.version <=> NEW.version)
     OR NOT (OLD.created_at <=> NEW.created_at)
     OR NOT (OLD.created_by <=> NEW.created_by)
     OR NOT (OLD.finalization_due_on <=> NEW.finalization_due_on) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_STATEMENT_LOCKED';
  END IF;
  IF NOT (OLD.first_export_id <=> NEW.first_export_id)
     AND NOT (OLD.first_export_id IS NULL AND NEW.first_export_id IS NOT NULL) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_STATEMENT_LOCKED';
  END IF;
 END IF;
 IF NOT (OLD.id <=> NEW.id) OR OLD.kind<>NEW.kind THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_IDENTITY_IMMUTABLE';
 END IF;
 IF NEW.kind='STATEMENT' AND NOT EXISTS(
   SELECT 1 FROM cost_statement WHERE id=NEW.period_id AND kind='PERIOD') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_INVALID_PERIOD';
 END IF;
END$$

CREATE TRIGGER trg_cost_statement_no_delete BEFORE DELETE ON cost_statement FOR EACH ROW
BEGIN
 IF OLD.locked_at IS NOT NULL AND SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_STATEMENT_LOCKED';
 END IF;
END$$

CREATE TRIGGER trg_cost_item_insert BEFORE INSERT ON cost_item FOR EACH ROW
BEGIN
 DECLARE parent_kind VARCHAR(20) DEFAULT NULL;
 DECLARE parent_period BIGINT DEFAULT NULL;
 DECLARE parent_resident BIGINT DEFAULT NULL;
 DECLARE parent_lock DATETIME(6) DEFAULT NULL;
 SELECT kind,period_id,resident_id,locked_at INTO parent_kind,parent_period,parent_resident,parent_lock
   FROM cost_statement WHERE id=NEW.statement_id FOR SHARE;
 IF parent_lock IS NOT NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_PARENT_LOCKED';
 END IF;
 IF parent_kind IS NULL OR parent_kind<>'STATEMENT'
    OR NOT (parent_period <=> NEW.period_id) OR NOT (parent_resident <=> NEW.resident_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_INVALID_STATEMENT';
 END IF;
END$$

CREATE TRIGGER trg_cost_item_row_lock BEFORE UPDATE ON cost_item FOR EACH ROW
BEGIN
 DECLARE parent_kind VARCHAR(20) DEFAULT NULL;
 DECLARE parent_period BIGINT DEFAULT NULL;
 DECLARE parent_resident BIGINT DEFAULT NULL;
 DECLARE parent_lock DATETIME(6) DEFAULT NULL;
 DECLARE old_parent_lock DATETIME(6) DEFAULT NULL;
 IF OLD.locked_at IS NOT NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_ITEM_LOCKED';
 END IF;
 SELECT locked_at INTO old_parent_lock FROM cost_statement WHERE id=OLD.statement_id FOR SHARE;
 IF old_parent_lock IS NOT NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_PARENT_LOCKED';
 END IF;
 SELECT kind,period_id,resident_id,locked_at INTO parent_kind,parent_period,parent_resident,parent_lock
   FROM cost_statement WHERE id=NEW.statement_id FOR SHARE;
 IF parent_lock IS NOT NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_PARENT_LOCKED';
 END IF;
 IF parent_kind IS NULL OR parent_kind<>'STATEMENT'
    OR NOT (parent_period <=> NEW.period_id) OR NOT (parent_resident <=> NEW.resident_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_INVALID_STATEMENT';
 END IF;
 IF COALESCE(@m11_recalculating,0)<>1 AND
   (NOT (OLD.quantity <=> NEW.quantity) OR NOT (OLD.unit_price <=> NEW.unit_price)
    OR NOT (OLD.factor_percent <=> NEW.factor_percent) OR NOT (OLD.amount <=> NEW.amount)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_RECALCULATION_REQUIRED';
 END IF;
END$$

CREATE TRIGGER trg_cost_item_no_delete BEFORE DELETE ON cost_item FOR EACH ROW
BEGIN
 DECLARE parent_lock DATETIME(6) DEFAULT NULL;
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SELECT locked_at INTO parent_lock FROM cost_statement WHERE id=OLD.statement_id FOR SHARE;
  IF OLD.locked_at IS NOT NULL OR parent_lock IS NOT NULL THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_PARENT_LOCKED';
  END IF;
 END IF;
END$$
DELIMITER ;

GRANT SELECT,INSERT,UPDATE ON cost_statement TO 'eldercare_app'@'%';
GRANT SELECT,INSERT,UPDATE ON cost_item TO 'eldercare_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE ON cost_source_index TO 'eldercare_app'@'%';
GRANT SELECT,DELETE ON cost_statement TO 'eldercare_retention'@'%';
GRANT SELECT,DELETE ON cost_item TO 'eldercare_retention'@'%';
GRANT SELECT,DELETE ON cost_source_index TO 'eldercare_retention'@'%';
