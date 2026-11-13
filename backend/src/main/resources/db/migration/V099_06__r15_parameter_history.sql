-- Gộp bảng về 79 (research R-15 của 000, chủ dự án yêu cầu 2026-11-13): lịch sử đổi giá trị tham số thành dòng
-- status_history loại M15.PARAMETER (nhóm 3, chỉ thêm như bảng cũ, DBR-24): attribute là mã tham số (entity_id = 0 vì
-- tham số khóa bằng mã), starts_at là mốc hiệu lực, giá trị ở details.value, command CHANGE hoặc RESTORE_DEFAULT;
-- unique_key 'M15.PARAMETER:<mã>:<mốc hiệu lực>' giữ ràng buộc (mã, mốc hiệu lực). Migration thêm tham số viết sau file
-- này ghi lịch sử vào status_history. Cú pháp dùng được ở MySQL 8.0 và 8.4.
INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, starts_at, command, actor_id, by_system,
                            reason, details, unique_key)
SELECT 'M15.PARAMETER',
       0,
       code,
       'M15',
       'POINT',
       effective_from,
       IF(is_default_restore, 'RESTORE_DEFAULT', 'CHANGE'),
       actor_id,
       actor_id IS NULL,
       reason,
       JSON_OBJECT('value', value),
       CONCAT('M15.PARAMETER:', code, ':', DATE_FORMAT(effective_from, '%Y-%m-%dT%H:%i:%s.%f'))
FROM parameter_history
ORDER BY id;

REVOKE INSERT ON parameter_history FROM 'eldercare_app'@'%';
REVOKE DELETE ON parameter_history FROM 'eldercare_retention'@'%';
DROP TABLE parameter_history;
