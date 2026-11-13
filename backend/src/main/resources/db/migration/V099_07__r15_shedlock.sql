-- Gộp bảng về 79 (research R-15 của 000, chủ dự án yêu cầu 2026-11-13): khóa job của ShedLock dùng chung bảng
-- technical_lock (lock_key là tên job) với ba cột lock_until, locked_at, locked_by; SchedulingConfig khai tên bảng, tên
-- cột cho JdbcTemplateLockProvider. ShedLock chỉ ghi bốn cột của nó nên updated_at có mặc định. Dòng khóa lệnh của module
-- (lock_until NULL) không khớp điều kiện lock_until <= now của ShedLock. Cú pháp dùng được ở MySQL 8.0 và 8.4.
ALTER TABLE technical_lock
    MODIFY updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN lock_until TIMESTAMP(3) NULL COMMENT 'ShedLock: job giữ khóa tới lúc này',
    ADD COLUMN locked_at TIMESTAMP(3) NULL,
    ADD COLUMN locked_by VARCHAR(255) NULL;

INSERT INTO technical_lock (lock_key, version, updated_at, lock_until, locked_at, locked_by)
SELECT name, 0, UTC_TIMESTAMP(6), lock_until, locked_at, locked_by
FROM shedlock
ON DUPLICATE KEY UPDATE lock_until = VALUES(lock_until),
                        locked_at  = VALUES(locked_at),
                        locked_by  = VALUES(locked_by);

REVOKE INSERT, UPDATE ON shedlock FROM 'eldercare_app'@'%';
DROP TABLE shedlock;
