-- 007 T108 (FR-081; 016 FR-042, FR-042a; "Đổi api" incident.api duyệt 2026-11-08): mức lúc tiếp nhận của sự cố,
-- ghi cùng received_at trong lệnh Tiếp nhận (như alert.level_at_received), để 016 chia thời gian tiếp nhận theo mức
-- lúc tiếp nhận; mức hiện hành vẫn ở cột level.
ALTER TABLE incident
    ADD COLUMN level_at_received ENUM ('LOW','MEDIUM','CRITICAL') NULL AFTER received_by;

-- Sự cố đã tiếp nhận trước bản này: mức sau lần đổi mức cuối cùng trước lúc tiếp nhận; không có thì mức trước lần đổi
-- đầu tiên sau lúc tiếp nhận; không đổi mức lần nào thì mức hiện hành. Bỏ sự cố Đã hủy (trigger chặn sửa, báo cáo
-- không đọc).
UPDATE incident i
SET i.level_at_received = COALESCE(
        (SELECT JSON_UNQUOTE(JSON_EXTRACT(e.content, '$.to'))
         FROM record_event e
         WHERE e.kind = 'M05.INCIDENT_LEVEL_CHANGE' AND e.entity_type = 'M05.INCIDENT' AND e.entity_id = i.id
           AND e.occurred_at <= i.received_at
         ORDER BY e.occurred_at DESC, e.id DESC
         LIMIT 1),
        (SELECT JSON_UNQUOTE(JSON_EXTRACT(e.content, '$.from'))
         FROM record_event e
         WHERE e.kind = 'M05.INCIDENT_LEVEL_CHANGE' AND e.entity_type = 'M05.INCIDENT' AND e.entity_id = i.id
           AND e.occurred_at > i.received_at
         ORDER BY e.occurred_at, e.id
         LIMIT 1),
        i.level)
WHERE i.received_at IS NOT NULL
  AND i.status <> 'CANCELLED';

GRANT UPDATE (level_at_received) ON incident TO 'eldercare_app'@'%';
