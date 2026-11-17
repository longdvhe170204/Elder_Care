-- Sửa dữ liệu demo của V900_02 (chỉ profile demo; 2026-11-17): mẫu ca 9001 ghi createdAt bằng DATETIME của MySQL
-- ('yyyy-MM-dd HH:mm:ss.ffffff'), trong khi backend đọc trường này thành Instant (ISO-8601 UTC), nên job phút của 005 đọc
-- mẫu ca lúc khởi động bị lỗi và backend dừng. Không sửa V900_02 vì đã áp trên các máy chạy demo (Flyway kiểm checksum).
-- Đổi sang dạng của các migration chính (V008_95: DATE_FORMAT(..., '%Y-%m-%dT%H:%i:%s.%fZ')); chạy lại không đổi gì.

UPDATE catalog_item
SET attributes = JSON_SET(attributes, '$.createdAt',
                          DATE_FORMAT(CAST(attributes ->> '$.createdAt' AS DATETIME(6)), '%Y-%m-%dT%H:%i:%s.%fZ'))
WHERE id = 9001
  AND catalog_type = 'M09.SHIFT_TEMPLATE'
  AND attributes ->> '$.createdAt' NOT LIKE '%T%';

-- Người phụ trách ca: backend đọc cột work_shift.lead_staff_id (ShiftStore), V900_02 chỉ ghi dòng lịch sử LEAD_STAFF.
-- Ca đã Đóng, Đã hủy không sửa được (trigger FR-022) nên bỏ qua.
UPDATE work_shift
SET lead_staff_id = 9005
WHERE template_id = 9001
  AND scope_type = 'FLOOR'
  AND scope_id = 9001
  AND lead_staff_id IS NULL
  AND status NOT IN ('CLOSED', 'CANCELLED');
