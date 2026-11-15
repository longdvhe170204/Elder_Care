-- 017 (lưu giữ, FR-051; chủ dự án chốt 2026-11-12): cặp cấn trừ cọc ↔ số dư trỏ nhau hai chiều qua
-- linked_transaction_id, nên khóa ngoại cũ chặn xóa cả hai khi hết hạn lưu giữ. Gỡ liên kết khi xóa: xóa một giao dịch
-- của cặp thì liên kết ở giao dịch kia về NULL. Thao tác của khóa ngoại không chạy trigger, nên trigger khóa dòng giữ
-- nguyên với mọi lệnh khác; giao dịch chỉ bị xóa bởi tài khoản eldercare_retention (trg_balance_transaction_no_delete).
ALTER TABLE balance_transaction DROP FOREIGN KEY fk_balance_transaction_linked;
ALTER TABLE balance_transaction
    ADD CONSTRAINT fk_balance_transaction_linked FOREIGN KEY (linked_transaction_id) REFERENCES balance_transaction (id)
        ON DELETE SET NULL;
