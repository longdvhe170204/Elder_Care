-- Dữ liệu demo (chỉ profile demo; yêu cầu 2026-11-24 khi test tay BF-10): người cao tuổi A (9001), B (9002) của V900_02 đi
-- được luồng Kết thúc lưu trú. V900_02 ghi hợp đồng bằng SQL nên thiếu hai thứ mà lệnh của ứng dụng tạo ra:
-- (1) nội dung hợp đồng không có residencyFeeBillingPeriod (lệnh Lập hợp đồng của 004 luôn ghi), 010 không chia được phí
--     lưu trú theo ngày, lệnh Lập khoản theo sự kiện báo 500 nên không chốt được kỳ cuối (BF-09, BF-10 bước 3);
-- (2) không có sổ số dư (017 mở khi hợp đồng sang Chờ ký), Kế toán không quyết toán được (BF-10 bước 6a).
-- Dòng nội dung của V900_02 đã khóa (trigger trg_contract_version_row_lock, constitution XII) và migration đã áp dụng giữ
-- nguyên checksum, nên thêm phiên bản nội dung số 2 cùng ngày hiệu lực, chép nội dung cũ kèm kỳ tính phí theo cột của hợp
-- đồng (MONTH) và "không cần cọc" (V900_02 không thu cọc); 004 đọc phiên bản có sequence_no lớn nhất trong ngày. Sổ số dư mở
-- với số dư, cọc bằng 0 và mã nộp tiền cố định, đúng ký tự kiểm tra của PaymentCode (017). Chạy lại không đổi gì. Cú pháp
-- dùng được ở MySQL 8.0 và 8.4.

INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content, residence_type, care_level,
                              created_at, locked_at)
SELECT 'CONTENT', v.contract_id, 2, v.effective_from,
       JSON_SET(v.content, '$.residencyFeeBillingPeriod', c.residency_fee_billing_period,
                '$.noDepositRequired', TRUE),
       v.residence_type, v.care_level, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM contract_version v
         JOIN contract c ON c.id = v.contract_id
WHERE v.kind = 'CONTENT'
  AND v.contract_id IN (9001, 9002)
  AND v.sequence_no = 1
  AND NOT EXISTS (SELECT 1 FROM contract_version x
                  WHERE x.kind = 'CONTENT' AND x.contract_id = v.contract_id AND x.sequence_no = 2);

INSERT INTO resident_balance (resident_id, opened_at, opening_contract_id, payment_code, deposit_status,
                              deposit_status_contract_id)
SELECT c.resident_id, c.created_at, c.id, p.code, 'NOT_REQUIRED', c.id
FROM contract c
         JOIN (SELECT 9001 AS contract_id, 'NCTDEM9012' AS code
               UNION ALL SELECT 9002, 'NCTDEM9023') p ON p.contract_id = c.id
WHERE NOT EXISTS (SELECT 1 FROM resident_balance b WHERE b.resident_id = c.resident_id);
