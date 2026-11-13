-- 002 T079a: tài khoản mẫu cho profile demo (cũng nạp ở test như mọi migration trong demo/, 000 T048; quickstart 002
-- mục 1): một tài khoản cho mỗi vai trò nhân viên AC-01 → 05, 07, 09, 14 và một Quản lý viện thứ hai. Mật khẩu chung
-- "Demo2026!" (quickstart 002 mục 1, chỉ dùng cho demo) băm Argon2id như PasswordConfig; không buộc đổi mật khẩu để dùng
-- ngay. staff_id 9001 → 9009 là id hồ sơ nhân viên mẫu, dữ liệu demo của 008 tạo hồ sơ cùng id. Tầng, ca, phân công,
-- giấy phép, người thân R, M là dữ liệu demo của module chủ (003, 008, 012), thêm khi các module đó có.

INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password, inactivity_reference_at,
                     created_at, created_by)
SELECT s.username, 'STAFF', s.staff_id, 'ACTIVE',
       '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c',
       FALSE, UTC_TIMESTAMP(6) - INTERVAL 1 DAY, UTC_TIMESTAMP(6) - INTERVAL 1 DAY, NULL
FROM (SELECT 'demo.quanly' AS username, 9001 AS staff_id
      UNION ALL SELECT 'demo.quanly2', 9002
      UNION ALL SELECT 'demo.truongtang', 9003
      UNION ALL SELECT 'demo.bacsi', 9004
      UNION ALL SELECT 'demo.dieuduong', 9005
      UNION ALL SELECT 'demo.chamsoc', 9006
      UNION ALL SELECT 'demo.bep', 9007
      UNION ALL SELECT 'demo.hanhchinh', 9008
      UNION ALL SELECT 'demo.ketoan', 9009) s;

INSERT INTO account_status_history (account_id, status, effective_from, actor_id)
SELECT id, 'ACTIVE', created_at, NULL
FROM account
WHERE username LIKE 'demo.%';

INSERT INTO role_assignment (account_id, role, effective_from, assigned_by, reason)
SELECT a.id, r.role, a.created_at, NULL, 'Dữ liệu mẫu demo (002 T079a)'
FROM account a
         JOIN (SELECT 'demo.quanly' AS username, 'AC-01' AS role
               UNION ALL SELECT 'demo.quanly2', 'AC-01'
               UNION ALL SELECT 'demo.truongtang', 'AC-02'
               UNION ALL SELECT 'demo.bacsi', 'AC-03'
               UNION ALL SELECT 'demo.dieuduong', 'AC-04'
               UNION ALL SELECT 'demo.chamsoc', 'AC-05'
               UNION ALL SELECT 'demo.bep', 'AC-07'
               UNION ALL SELECT 'demo.hanhchinh', 'AC-09'
               UNION ALL SELECT 'demo.ketoan', 'AC-14') r ON r.username = a.username;
