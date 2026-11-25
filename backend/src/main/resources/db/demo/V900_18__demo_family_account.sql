-- Dữ liệu demo (chỉ profile demo; chủ dự án duyệt 2026-11-25, kiểm demo): tài khoản người thân demo.nguoithan để kiểm
-- cổng người thân trên web, app (mã VietQR nộp tiền của 017, Lịch chăm sóc hằng ngày của 011). Gắn hồ sơ người thân 9101
-- Lê Văn Cường của V900_17: người đại diện kiêm liên hệ chính của người cao tuổi 9101, đủ quyền xem sức khỏe, chi phí.
-- Ghi như AccountService.createFamily (AccountStore.insertFamily, history, addRole): vai trò duy nhất Người thân (AC-10),
-- trạng thái Hoạt động; mật khẩu chung "Demo2026!" như V002_90 (quickstart 002 mục 1, chỉ dùng cho demo), không buộc đổi
-- mật khẩu như các tài khoản demo khác. Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @demo_at = UTC_TIMESTAMP(6);
SET @created_at = @demo_at - INTERVAL 1 DAY;
SET @office = (SELECT id FROM account WHERE username = 'demo.hanhchinh');

INSERT INTO account (username, type, family_member_id, status, password_hash, must_change_password,
                     inactivity_reference_at, created_at, created_by)
VALUES ('demo.nguoithan', 'FAMILY_MEMBER', 9101, 'ACTIVE',
        '$argon2id$v=19$m=16384,t=2,p=1$17mqxm8hOdIjyC60bGYBQw$ja8n7hsdD4zLhrpMS2eSF1MPLbM1/KY00izjSCQkh9c',
        FALSE, @created_at, @created_at, @office);

SET @family = (SELECT id FROM account WHERE username = 'demo.nguoithan');

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            details, unique_key)
VALUES ('M15.ACCOUNT', @family, 'STATUS', 'M15', 'POINT', 'ACTIVE', @created_at, @office, FALSE,
        JSON_OBJECT('lockCause', NULL, 'auditLogId', NULL),
        CONCAT('M15.ASH:', @family, ':', DATE_FORMAT(@created_at, '%Y-%m-%dT%H:%i:%s.%f')));

INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, actor_id, reason)
VALUES ('ROLE_ASSIGNMENT', 'ACCOUNT', @family, 'AC-10', @created_at, @office, 'Tạo tài khoản');
