-- Gộp bảng về 79 (research R-15 của 000, chủ dự án yêu cầu 2026-11-13): lần chốt suất (011, nhóm 3) thành dòng
-- record_event loại M08.PORTION_CLOSING của bữa ngày M08.DAILY_MEAL, đã xác nhận ngay (trigger của record_event chặn sửa,
-- xóa như bảng cũ); actor_id là người chốt, occurred_at là thời điểm chốt, unique_key theo bữa ngày giữ "mỗi bữa chốt một
-- lần" (FR-033); phần còn lại ở content. Id lần chốt đổi (id cũ ở content.legacy); nhật ký cũ trỏ M08.PORTION_CLOSING
-- theo id cũ (chưa có dữ liệu vận hành). Cú pháp dùng được ở MySQL 8.0 và 8.4.
INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system, occurred_at,
                          confirmed_at, unique_key)
SELECT 'M08.DAILY_MEAL',
       daily_meal_id,
       'M08',
       'M08.PORTION_CLOSING',
       JSON_OBJECT('mealId', meal_id,
                   'mealDate', DATE_FORMAT(meal_date, '%Y-%m-%d'),
                   'mealTime', DATE_FORMAT(meal_time, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'lateThresholdMinutes', late_threshold_minutes,
                   'closedAfterMealTime', IF(closed_after_meal_time, CAST('true' AS JSON), CAST('false' AS JSON)),
                   'menuId', menu_id,
                   'totalPortions', total_portions,
                   'persons', counted_residents,
                   'portions', portions_by_diet,
                   'flags', flags,
                   'missingData', missing_data,
                   'legacy', JSON_OBJECT('table', 'portion_closing', 'id', id)),
       closed_by,
       FALSE,
       closed_at,
       closed_at,
       CONCAT('M08.PORTION_CLOSING:', daily_meal_id)
FROM portion_closing
ORDER BY id;

REVOKE SELECT, INSERT ON portion_closing FROM 'eldercare_app'@'%';
REVOKE DELETE ON portion_closing FROM 'eldercare_retention'@'%';
DROP TABLE portion_closing;
