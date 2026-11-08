-- 017 T004 (data-model 017 mục 0, 2, 3; research E-03): sổ số dư và giao dịch của người cao tuổi.
-- R-15: giao_dich là bảng giữ. so_du không gộp được vào resident_module_state: khóa (resident_id, module) chỉ có một
-- dòng M11 mỗi người và dòng đó là trạng thái chi phí của 010 (BillingResidentState); sổ cần cột tiền có kiểu,
-- mã nộp tiền UNIQUE và khóa dòng riêng (DBR-28) nên giữ bảng resident_balance (plan 017 "Ghi chú triển khai").
-- Các bảng còn lại gộp vào bảng chung, không tạo ở đây: lịch sử tình trạng → status_history, ngân hàng → catalog_item,
-- phiếu báo phí, bảng quyết toán → record_event, lần xuất, lần tải → nhật ký, mốc sự kiện, lần chạy → reminder_marker.
-- Tiền DECIMAL(15,0) đồng, có dấu (000 data-model mục 1, audit F-07). Thời điểm do caller truyền từ BusinessTime.
CREATE TABLE resident_balance (
 resident_id BIGINT NOT NULL PRIMARY KEY,
 opened_at DATETIME(6) NOT NULL,
 opening_contract_id BIGINT NOT NULL,
 payment_code VARCHAR(16) COLLATE utf8mb4_bin NOT NULL,
 balance DECIMAL(15,0) NOT NULL DEFAULT 0,
 deposit_balance DECIMAL(15,0) NOT NULL DEFAULT 0,
 status ENUM('NORMAL','RUNNING_LOW','IN_DEBT') NOT NULL DEFAULT 'NORMAL',
 days_covered INT NULL,
 cycle_started_on DATE NULL,
 last_notice_on DATE NULL,
 in_debt_since DATE NULL,
 reminders_stopped BOOLEAN NOT NULL DEFAULT FALSE,
 reminders_stopped_by BIGINT NULL,
 reminders_stopped_reason VARCHAR(1000) NULL,
 deposit_refund_due_since DATETIME(6) NULL,
 deposit_refund_reminded_at DATETIME(6) NULL,
 final_status_reminded_at DATETIME(6) NULL,
 version INT NOT NULL DEFAULT 0,
 CONSTRAINT uq_resident_balance_payment_code UNIQUE(payment_code),
 CONSTRAINT chk_resident_balance_stop CHECK(
   (reminders_stopped=FALSE AND reminders_stopped_by IS NULL AND reminders_stopped_reason IS NULL)
   OR (reminders_stopped=TRUE AND reminders_stopped_by IS NOT NULL AND reminders_stopped_reason IS NOT NULL)),
 INDEX ix_resident_balance_status(status)
) COMMENT = 'DataGroup=BUSINESS';

CREATE TABLE balance_transaction (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 resident_id BIGINT NOT NULL,
 ledger ENUM('BALANCE','DEPOSIT') NOT NULL,
 type ENUM('CASH_PAYMENT','BANK_TRANSFER_PAYMENT','STATEMENT_PAYMENT','REFUND','DEPOSIT_OFFSET','ADJUSTMENT',
           'DEPOSIT_RECEIPT','DEPOSIT_REFUND') NOT NULL,
 amount DECIMAL(15,0) NOT NULL,
 occurred_at DATETIME(6) NOT NULL,
 method ENUM('CASH','BANK_TRANSFER','INTERNAL','OTHER') NOT NULL,
 bank_transaction_code VARCHAR(100) NULL,
 cost_statement_id BIGINT NULL,
 original_transaction_id BIGINT NULL,
 linked_transaction_id BIGINT NULL,
 payer_name VARCHAR(200) NULL,
 recipient JSON NULL,
 note VARCHAR(1000) NULL,
 reason VARCHAR(1000) NULL,
 status ENUM('PENDING_APPROVAL','CONFIRMED','REJECTED','CANCELLED') NOT NULL,
 approval_request_id BIGINT NULL,
 approved_by BIGINT NULL,
 confirmed_at DATETIME(6) NULL,
 adjusted BOOLEAN NOT NULL DEFAULT FALSE,
 duplicate_code_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
 locked_at DATETIME(6) NULL,
 version INT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 created_by BIGINT NULL,
 CONSTRAINT fk_balance_transaction_ledger FOREIGN KEY(resident_id) REFERENCES resident_balance(resident_id),
 CONSTRAINT fk_balance_transaction_original FOREIGN KEY(original_transaction_id) REFERENCES balance_transaction(id),
 CONSTRAINT fk_balance_transaction_linked FOREIGN KEY(linked_transaction_id) REFERENCES balance_transaction(id),
 -- DBR-30: một giao dịch thanh toán mỗi bảng chi phí.
 CONSTRAINT uq_balance_transaction_statement UNIQUE(cost_statement_id),
 -- FR-010: cặp sổ, loại theo bảng 15.9.
 CONSTRAINT chk_balance_transaction_ledger_type CHECK(
   (ledger='BALANCE' AND type IN ('CASH_PAYMENT','BANK_TRANSFER_PAYMENT','STATEMENT_PAYMENT','REFUND',
                                  'DEPOSIT_OFFSET','ADJUSTMENT'))
   OR (ledger='DEPOSIT' AND type IN ('DEPOSIT_RECEIPT','DEPOSIT_REFUND','DEPOSIT_OFFSET','ADJUSTMENT'))),
 -- FR-005, FR-007, FR-010, FR-015: dấu số tiền theo loại; cấn trừ tăng số dư, giảm sổ cọc.
 CONSTRAINT chk_balance_transaction_sign CHECK(
   (type IN ('CASH_PAYMENT','BANK_TRANSFER_PAYMENT','DEPOSIT_RECEIPT') AND amount>0)
   OR (type IN ('REFUND','DEPOSIT_REFUND') AND amount<0)
   OR (type='DEPOSIT_OFFSET' AND ((ledger='BALANCE' AND amount>0) OR (ledger='DEPOSIT' AND amount<0)))
   OR type IN ('STATEMENT_PAYMENT','ADJUSTMENT')),
 -- Phương thức theo loại; chuyển khoản bắt buộc mã giao dịch ngân hàng (FR-007).
 CONSTRAINT chk_balance_transaction_method CHECK(
   (type='CASH_PAYMENT' AND method='CASH')
   OR (type='BANK_TRANSFER_PAYMENT' AND method='BANK_TRANSFER')
   OR (type='DEPOSIT_RECEIPT' AND method IN ('CASH','BANK_TRANSFER'))
   OR (type IN ('STATEMENT_PAYMENT','DEPOSIT_OFFSET') AND method='INTERNAL')
   OR type IN ('REFUND','DEPOSIT_REFUND','ADJUSTMENT')),
 CONSTRAINT chk_balance_transaction_bank_code CHECK(method<>'BANK_TRANSFER' OR bank_transaction_code IS NOT NULL),
 CONSTRAINT chk_balance_transaction_statement CHECK((type='STATEMENT_PAYMENT') = (cost_statement_id IS NOT NULL)),
 CONSTRAINT chk_balance_transaction_reason CHECK(type<>'ADJUSTMENT' OR reason IS NOT NULL),
 -- Đã xác nhận, Từ chối, Đã hủy là nhóm 3: khóa dòng (000 data-model mục 1.4).
 CONSTRAINT chk_balance_transaction_lock CHECK(
   (status='PENDING_APPROVAL' AND locked_at IS NULL AND confirmed_at IS NULL)
   OR (status='CONFIRMED' AND locked_at IS NOT NULL AND confirmed_at IS NOT NULL)
   OR (status IN ('REJECTED','CANCELLED') AND locked_at IS NOT NULL AND confirmed_at IS NULL)),
 INDEX ix_balance_transaction_ledger(resident_id,ledger,status,confirmed_at),
 INDEX ix_balance_transaction_bank_code(bank_transaction_code),
 INDEX ix_balance_transaction_status_type(status,type)
) COMMENT = 'DataGroup=MULTIPLE';

DELIMITER $$
-- DBR-17, FR-011: giao dịch đã khóa chỉ được đặt dấu "đã điều chỉnh" một lần (khi Đã xác nhận).
CREATE TRIGGER trg_balance_transaction_row_lock BEFORE UPDATE ON balance_transaction FOR EACH ROW
BEGIN
 IF OLD.locked_at IS NOT NULL THEN
  IF NOT (OLD.id <=> NEW.id)
     OR NOT (OLD.resident_id <=> NEW.resident_id)
     OR NOT (OLD.ledger <=> NEW.ledger)
     OR NOT (OLD.type <=> NEW.type)
     OR NOT (OLD.amount <=> NEW.amount)
     OR NOT (OLD.occurred_at <=> NEW.occurred_at)
     OR NOT (OLD.method <=> NEW.method)
     OR NOT (OLD.bank_transaction_code <=> NEW.bank_transaction_code)
     OR NOT (OLD.cost_statement_id <=> NEW.cost_statement_id)
     OR NOT (OLD.original_transaction_id <=> NEW.original_transaction_id)
     OR NOT (OLD.linked_transaction_id <=> NEW.linked_transaction_id)
     OR NOT (OLD.payer_name <=> NEW.payer_name)
     OR NOT (OLD.recipient <=> NEW.recipient)
     OR NOT (OLD.note <=> NEW.note)
     OR NOT (OLD.reason <=> NEW.reason)
     OR NOT (OLD.status <=> NEW.status)
     OR NOT (OLD.approval_request_id <=> NEW.approval_request_id)
     OR NOT (OLD.approved_by <=> NEW.approved_by)
     OR NOT (OLD.confirmed_at <=> NEW.confirmed_at)
     OR NOT (OLD.duplicate_code_confirmed <=> NEW.duplicate_code_confirmed)
     OR NOT (OLD.locked_at <=> NEW.locked_at)
     OR NOT (OLD.created_at <=> NEW.created_at)
     OR NOT (OLD.created_by <=> NEW.created_by) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_BALANCE_TRANSACTION_LOCKED';
  END IF;
  IF NOT (OLD.adjusted <=> NEW.adjusted)
     AND NOT (OLD.status='CONFIRMED' AND OLD.adjusted=FALSE AND NEW.adjusted=TRUE) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_BALANCE_TRANSACTION_LOCKED';
  END IF;
 END IF;
END$$

CREATE TRIGGER trg_balance_transaction_no_delete BEFORE DELETE ON balance_transaction FOR EACH ROW
BEGIN
 IF SUBSTRING_INDEX(USER(),'@',1)<>'eldercare_retention' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='M11_BALANCE_TRANSACTION_LOCKED';
 END IF;
END$$
DELIMITER ;

GRANT SELECT,INSERT,UPDATE ON resident_balance TO 'eldercare_app'@'%';
GRANT SELECT,INSERT,UPDATE ON balance_transaction TO 'eldercare_app'@'%';
GRANT SELECT,DELETE ON resident_balance TO 'eldercare_retention'@'%';
GRANT SELECT,DELETE ON balance_transaction TO 'eldercare_retention'@'%';
