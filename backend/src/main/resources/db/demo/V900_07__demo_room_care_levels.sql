-- Dữ liệu demo (chỉ profile demo; 2026-11-20): bốn phòng demo của V900_01 không ghi allowed_care_levels nên nhận mặc định
-- JSON_ARRAY() rỗng; mọi giường trượt điều kiện FR-018(a) "Mức chăm sóc không được phòng cho phép" của 003, Hành chính
-- không phân bổ được giường cho người mới (BF-01 bước 8). 003 FR-004: danh sách mức chăm sóc được phép của phòng khởi tạo
-- từ loại phòng. Sửa bằng migration mới, không sửa V900_01 (đã chạy trên máy phát triển và bản demo Docker, sửa thẳng thì
-- Flyway báo lệch checksum). Chỉ đổi phòng demo còn rỗng, nên chạy lại không đổi gì. Cú pháp dùng được ở MySQL 8.0 và 8.4.
UPDATE room r
JOIN room_type t ON t.id = r.room_type_id
SET r.allowed_care_levels = t.allowed_care_levels
WHERE r.id IN (9001, 9002, 9003, 9004)
  AND JSON_LENGTH(r.allowed_care_levels) = 0;
