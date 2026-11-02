-- 004 T051 (Q-395, 2026-10-29): CFG-M02-05 đổi từ bảng hệ số sang số ngày báo Quản lý viện và hai tỷ lệ bán trú. Cách nạp như
-- V000_04: giá trị hiện hành và một dòng parameter_history (Bộ lập lịch, actor_id NULL). Tên trường theo cách đọc của DayCareSessions
-- và AbsenceManagerNotice. Mặc định: báo ở ngày 30; bán trú vắng có báo 0%, không báo 50% (spec 004 FR-055, FR-060).
UPDATE parameter
SET description   = 'Phí khi vắng: số ngày báo Quản lý viện, tỷ lệ tính phí buổi bán trú vắng có báo, không báo',
    default_value = '{"managerNoticeDays":30,"dayCareNoticedPercent":0,"dayCareUnnoticedPercent":50}',
    current_value = '{"managerNoticeDays":30,"dayCareNoticedPercent":0,"dayCareUnnoticedPercent":50}',
    used_in       = 'BR-M02-06, BR-M02-07, Q-395',
    version       = version + 1
WHERE code = 'CFG-M02-05';

INSERT INTO parameter_history (code, value, effective_from, actor_id, reason, is_default_restore)
SELECT code, current_value, UTC_TIMESTAMP(6), NULL, NULL, FALSE
FROM parameter
WHERE code = 'CFG-M02-05';
