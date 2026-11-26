-- Dữ liệu demo BF-07 (chỉ profile demo; 2026-11-25): chỉ định Phụ trách bộ phận mặc định cho 4 bộ phận
-- (Q-292, chú thích 37 Phụ lục 27) để bước Ghi đã mua trên Yêu cầu mua sắm (BF-07 bước 11) chạy được ngay:
--   - Y tế: demo.dieuduong (9005, Hoàng Thị Dung)
--   - Chăm sóc: demo.truongtang (9003, Trần Quốc Tuấn)
--   - Bếp: demo.bep (9007, Đỗ Thị Bích)
--   - Hành chính – Kế toán: demo.hanhchinh (9008, Bùi Thị Hạnh; DepartmentDutyService yêu cầu vai trò OFFICE_STAFF)

SET @demo_at = UTC_TIMESTAMP(6);
SET @manager = (SELECT id FROM account WHERE username = 'demo.quanly');

CREATE TEMPORARY TABLE demo_dept_lead (name VARCHAR(200) PRIMARY KEY, staff_id BIGINT NOT NULL);
INSERT INTO demo_dept_lead (name, staff_id) VALUES
    ('Y tế', 9005),
    ('Chăm sóc', 9003),
    ('Bếp', 9007),
    ('Hành chính – Kế toán', 9008);

UPDATE catalog_item c
    JOIN demo_dept_lead l ON c.catalog_type = 'M09.DEPARTMENT' AND c.name = l.name AND c.status = 'ACTIVE'
SET c.attributes = JSON_SET(c.attributes, '$.leadStaffId', l.staff_id),
    c.version = c.version + 1;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_id, starts_at, command, actor_id,
                            by_system, reason, details)
SELECT 'M09.DEPARTMENT',
       COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'), 'null') AS UNSIGNED), c.id),
       'LEAD_STAFF',
       'M09',
       'PERIOD',
       l.staff_id,
       @demo_at,
       'LEAD_CHANGED',
       @manager,
       FALSE,
       'Chỉ định phụ trách bộ phận mặc định cho dữ liệu demo',
       JSON_OBJECT('legacy', NULL)
FROM catalog_item c
         JOIN demo_dept_lead l ON c.catalog_type = 'M09.DEPARTMENT' AND c.name = l.name AND c.status = 'ACTIVE';

DROP TEMPORARY TABLE demo_dept_lead;
