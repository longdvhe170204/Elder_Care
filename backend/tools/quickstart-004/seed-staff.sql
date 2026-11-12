-- Hồ sơ nhân viên 9001 → 9009 mà tài khoản demo/V002_90 trỏ tới (staff_id), chưa có dữ liệu demo của 008 tạo.
-- Chỉ chạy trên schema demo riêng của nghiệm thu (elder_care_qs2), bằng tài khoản quản trị; chạy lại không sao.
INSERT IGNORE INTO staff_profile (id, full_name, position, start_date, account_id, source, status, version, created_at, created_by)
VALUES (9001, 'Quản lý viện Demo', 'Quản lý viện', '2025-01-01', 1, 'MANUAL', 'WORKING', 0, UTC_TIMESTAMP(6), 1),
       (9002, 'Quản lý viện Demo 2', 'Quản lý viện', '2025-01-01', 2, 'MANUAL', 'WORKING', 0, UTC_TIMESTAMP(6), 1),
       (9003, 'Trưởng tầng Demo', 'Trưởng tầng', '2025-01-01', 3, 'MANUAL', 'WORKING', 0, UTC_TIMESTAMP(6), 1),
       (9004, 'Bác sĩ Demo', 'Bác sĩ', '2025-01-01', 4, 'MANUAL', 'WORKING', 0, UTC_TIMESTAMP(6), 1),
       (9005, 'Điều dưỡng Demo', 'Điều dưỡng', '2025-01-01', 5, 'MANUAL', 'WORKING', 0, UTC_TIMESTAMP(6), 1),
       (9006, 'Chăm sóc Demo', 'Nhân viên chăm sóc', '2025-01-01', 6, 'MANUAL', 'WORKING', 0, UTC_TIMESTAMP(6), 1),
       (9007, 'Bếp Demo', 'Nhân viên bếp', '2025-01-01', 7, 'MANUAL', 'WORKING', 0, UTC_TIMESTAMP(6), 1),
       (9008, 'Hành chính Demo', 'Hành chính', '2025-01-01', 8, 'MANUAL', 'WORKING', 0, UTC_TIMESTAMP(6), 1),
       (9009, 'Kế toán Demo', 'Kế toán', '2025-01-01', 9, 'MANUAL', 'WORKING', 0, UTC_TIMESTAMP(6), 1);
INSERT INTO staff_department (staff_id, department_id, starts_at, actor_id, reason)
SELECT s.id, c.id, '2025-01-01', 1, 'Dữ liệu mẫu nghiệm thu quickstart'
FROM staff_profile s
         JOIN (SELECT 9001 sid, 4 dep UNION ALL SELECT 9002, 4 UNION ALL SELECT 9003, 2 UNION ALL SELECT 9004, 1
               UNION ALL SELECT 9005, 1 UNION ALL SELECT 9006, 2 UNION ALL SELECT 9007, 3 UNION ALL SELECT 9008, 4
               UNION ALL SELECT 9009, 4) d ON d.sid = s.id
         JOIN catalog_item c ON c.catalog_type = 'M09.DEPARTMENT' AND c.m09_legacy_id = d.dep
WHERE NOT EXISTS (SELECT 1 FROM staff_department x WHERE x.staff_id = s.id AND x.ends_at IS NULL);
