-- 017 T025 (B): hoàn tiền, hoàn cọc, điều chỉnh bằng chuyển khoản không có mã giao dịch ngân hàng (api-017
-- NewBalanceTransaction không có trường này); FR-007 chỉ bắt buộc mã cho tiền nộp chuyển khoản. Thu hẹp CHECK của V017_01.
ALTER TABLE balance_transaction DROP CHECK chk_balance_transaction_bank_code;
ALTER TABLE balance_transaction ADD CONSTRAINT chk_balance_transaction_bank_code CHECK(
 type NOT IN ('BANK_TRANSFER_PAYMENT','DEPOSIT_RECEIPT') OR method<>'BANK_TRANSFER' OR bank_transaction_code IS NOT NULL);
