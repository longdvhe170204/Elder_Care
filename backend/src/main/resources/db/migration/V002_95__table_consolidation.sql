-- 002 T098: gộp bảng theo research R-15 (bản 2) của 000, data-model 002 mục 0, 0.1. Không đổi nghiệp vụ, API, quyền.
-- Hai bảng gộp riêng của 002 (permission_assignment, device_credential); vai trò, quy tắc xem trường, nghiệp vụ chuyên môn
-- vào catalog_item; lịch sử trạng thái tài khoản vào status_history; bản ghi chờ xem lại, lỗi dữ liệu quyền vào
-- simple_record; cảnh báo giấy phép vào reminder_marker. Dòng đổi id ghi legacy = {table, id} trong JSON của dòng; dòng
-- gán vai trò, thiết bị gắn giữ id. Bảng còn lại khai nhóm dữ liệu bằng COMMENT (audit F-04). Cú pháp có ở MySQL 8.0, 8.4.

-- Quyền tài khoản: gán vai trò, thu hẹp, quyền gán thêm (nhóm 1 có lịch sử: đóng dòng cũ, thêm dòng mới, không xóa).
CREATE TABLE permission_assignment
(
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind             ENUM ('ROLE_ASSIGNMENT', 'RESTRICTION', 'ADDITIONAL_GRANT') NOT NULL,
    applies_to       ENUM ('ROLE', 'ACCOUNT') NOT NULL,
    account_id       BIGINT        NULL,
    role             VARCHAR(5)    NULL COMMENT 'Vai trò được gán, hoặc vai trò bị thu hẹp (common.Role.code)',
    function_code    VARCHAR(40)   NULL,
    operation        ENUM ('VIEW', 'CREATE', 'UPDATE', 'CONFIRM', 'APPROVE', 'FINALIZE', 'PROFESSIONAL') NULL,
    restriction_kind ENUM ('REMOVE_OPERATION', 'LOWER_SCOPE') NULL,
    metric_group     VARCHAR(40)   NULL,
    channel          ENUM ('DASHBOARD', 'REPORT') NULL,
    effective_from   DATETIME(6)   NOT NULL,
    effective_to     DATETIME(6)   NULL,
    actor_id         BIGINT        NULL COMMENT 'NULL chỉ với gán vai trò khởi tạo (BR-M15-22)',
    reason           VARCHAR(2000) NOT NULL,
    -- Mỗi (account_id, role) tối đa một dòng gán vai trò còn hiệu lực.
    active_role_key  VARCHAR(30) GENERATED ALWAYS AS (
        IF(kind = 'ROLE_ASSIGNMENT' AND effective_to IS NULL, CONCAT(account_id, ':', role), NULL)) STORED,
    CONSTRAINT uq_permission_assignment_active_role UNIQUE (active_role_key),
    CONSTRAINT fk_permission_assignment_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT chk_permission_assignment_kind CHECK (
        (kind = 'ROLE_ASSIGNMENT' AND applies_to = 'ACCOUNT' AND account_id IS NOT NULL AND role IS NOT NULL
            AND function_code IS NULL AND operation IS NULL AND restriction_kind IS NULL)
            OR (kind = 'RESTRICTION' AND function_code IS NOT NULL AND operation IS NOT NULL
            AND restriction_kind IS NOT NULL AND actor_id IS NOT NULL
            AND ((applies_to = 'ROLE' AND role IS NOT NULL AND account_id IS NULL)
                OR (applies_to = 'ACCOUNT' AND account_id IS NOT NULL AND role IS NULL)))
            OR (kind = 'ADDITIONAL_GRANT' AND applies_to = 'ACCOUNT' AND account_id IS NOT NULL AND role IS NULL
            AND function_code = 'CARE_PLAN' AND operation = 'APPROVE' AND restriction_kind IS NULL
            AND actor_id IS NOT NULL)),
    CONSTRAINT chk_permission_assignment_dashboard CHECK (
        function_code = 'DASHBOARD_REPORT' OR (metric_group IS NULL AND channel IS NULL)),
    CONSTRAINT chk_permission_assignment_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT chk_permission_assignment_period CHECK (effective_to IS NULL OR effective_to >= effective_from),
    KEY idx_permission_assignment_account (kind, account_id, effective_to),
    KEY idx_permission_assignment_role (kind, role, effective_to)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'DataGroup=CATALOG';

INSERT INTO permission_assignment (id, kind, applies_to, account_id, role, effective_from, effective_to, actor_id,
                                   reason)
SELECT id, 'ROLE_ASSIGNMENT', 'ACCOUNT', account_id, role, effective_from, effective_to, assigned_by, reason
FROM role_assignment;

INSERT INTO permission_assignment (kind, applies_to, account_id, role, function_code, operation, restriction_kind,
                                   metric_group, channel, effective_from, effective_to, actor_id, reason)
SELECT 'RESTRICTION', applies_to, account_id, role, function_code, operation, kind, metric_group, channel,
       effective_from, effective_to, actor_id, reason
FROM permission_restriction
ORDER BY id;

INSERT INTO permission_assignment (kind, applies_to, account_id, function_code, operation, effective_from, effective_to,
                                   actor_id, reason)
SELECT 'ADDITIONAL_GRANT', 'ACCOUNT', account_id, function_code, operation, effective_from, effective_to, actor_id, reason
FROM additional_grant
ORDER BY id;

-- Mã QR đăng nhập và thiết bị gắn của tài khoản người cao tuổi (nhóm 2): cùng tài khoản, cùng một mã băm tra khi đăng
-- nhập hoặc nhận token thiết bị (FR-069 → FR-071, BR-M15-24). Chỉ thêm và đặt mốc một lần, không xóa.
CREATE TABLE device_credential
(
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind              ENUM ('LOGIN_QR_CODE', 'BOUND_DEVICE') NOT NULL,
    account_id        BIGINT        NOT NULL,
    secret_hash       BINARY(32)    NOT NULL COMMENT 'SHA-256 của mã QR hoặc token thiết bị, không lưu giá trị',
    device_id         CHAR(36)      NULL COMMENT 'Mã QR: máy đã dùng; thiết bị gắn: X-Device-Id lúc gắn',
    created_by        BIGINT        NULL COMMENT 'Người tạo mã QR',
    created_at        DATETIME(6)   NOT NULL,
    expires_at        DATETIME(6)   NULL,
    used_at           DATETIME(6)   NULL,
    revoked_at        DATETIME(6)   NULL COMMENT 'Đặt khi có mã mới của cùng tài khoản',
    login_qr_code_id  BIGINT        NULL COMMENT 'Thiết bị gắn bằng mã nào; NULL khi gắn bằng mật khẩu dự phòng',
    ended_at          DATETIME(6)   NULL,
    ended_by          BIGINT        NULL COMMENT 'NULL: Bộ lập lịch hoặc gắn máy mới',
    end_reason        ENUM ('NEW_DEVICE_BOUND', 'MANUAL', 'ACCOUNT_LOCKED') NULL,
    end_note          VARCHAR(2000) NULL COMMENT 'Lý do khi gỡ thủ công',
    legacy_qr_id      BIGINT        NULL,
    -- Tối đa một thiết bị đang gắn mỗi tài khoản, một tài khoản đang gắn mỗi thiết bị (FR-014a).
    active_account_id BIGINT GENERATED ALWAYS AS (
        IF(kind = 'BOUND_DEVICE' AND ended_at IS NULL, account_id, NULL)) STORED,
    active_device_id  CHAR(36) GENERATED ALWAYS AS (
        IF(kind = 'BOUND_DEVICE' AND ended_at IS NULL, device_id, NULL)) STORED,
    CONSTRAINT uq_device_credential_secret UNIQUE (secret_hash),
    CONSTRAINT uq_device_credential_active_account UNIQUE (active_account_id),
    CONSTRAINT uq_device_credential_active_device UNIQUE (active_device_id),
    CONSTRAINT fk_device_credential_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT fk_device_credential_qr FOREIGN KEY (login_qr_code_id) REFERENCES device_credential (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_device_credential_kind CHECK (
        (kind = 'LOGIN_QR_CODE' AND created_by IS NOT NULL AND expires_at IS NOT NULL AND expires_at > created_at
            AND (used_at IS NULL) = (device_id IS NULL) AND login_qr_code_id IS NULL AND ended_at IS NULL
            AND end_reason IS NULL)
            OR (kind = 'BOUND_DEVICE' AND device_id IS NOT NULL AND expires_at IS NULL AND used_at IS NULL
            AND revoked_at IS NULL AND (ended_at IS NULL) = (end_reason IS NULL))),
    -- Bảng cũ viết end_reason <> 'MANUAL' OR CHAR_LENGTH(TRIM(note)) > 0 nên note NULL lọt qua (CHECK nhận NULL)
    CONSTRAINT chk_device_credential_note CHECK (
        end_reason IS NULL OR end_reason <> 'MANUAL' OR (end_note IS NOT NULL AND CHAR_LENGTH(TRIM(end_note)) > 0)),
    KEY idx_device_credential_account (account_id, kind, used_at, revoked_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'DataGroup=BUSINESS';

INSERT INTO device_credential (id, kind, account_id, secret_hash, device_id, created_at, ended_at, ended_by,
                               end_reason, end_note)
SELECT id, 'BOUND_DEVICE', account_id, device_token_hash, device_id, bound_at, unbound_at, unbound_by, unbind_reason,
       unbind_note
FROM bound_device;

INSERT INTO device_credential (kind, account_id, secret_hash, device_id, created_by, created_at, expires_at, used_at,
                               revoked_at, legacy_qr_id)
SELECT 'LOGIN_QR_CODE', account_id, code_hash, used_device_id, created_by, created_at, expires_at, used_at, revoked_at,
       id
FROM login_qr_code
ORDER BY id;

UPDATE device_credential b
    JOIN bound_device old ON old.id = b.id
    JOIN device_credential q ON q.legacy_qr_id = old.login_qr_code_id
SET b.login_qr_code_id = q.id
WHERE b.kind = 'BOUND_DEVICE';

ALTER TABLE device_credential
    DROP COLUMN legacy_qr_id;

CREATE TRIGGER trg_device_credential_no_delete
    BEFORE DELETE
    ON device_credential
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'device_credential rows are never deleted';

-- Danh mục: vai trò (permissionVersion chỉ tăng, A-05), quy tắc xem trường và nghiệp vụ chuyên môn (nạp sẵn).
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
SELECT 'M15.ROLE', 'M15', code, name, 0, 'ACTIVE',
       JSON_OBJECT('accountType', account_type, 'executionScope', execution_scope, 'permissionVersion',
                   permission_version)
FROM role
ORDER BY code;

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
SELECT 'M15.FIELD_VISIBILITY', 'M15', CONCAT(role, ':', info_group), CONCAT(role, ':', info_group), 0, 'ACTIVE',
       JSON_OBJECT('visibility', visibility)
FROM field_visibility_rule
ORDER BY role, info_group;

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes, version)
SELECT 'M15.PROFESSIONAL_ACTIVITY', 'M15', p.code, p.name, 0, p.status,
       JSON_OBJECT('functionCode', p.function_code,
                   'requiredLicenseType', p.required_license_type,
                   'requiresFacilityScope', IF(p.requires_facility_scope, CAST('true' AS JSON), CAST('false' AS JSON)),
                   'isDefault', IF(p.is_default, CAST('true' AS JSON), CAST('false' AS JSON)),
                   'practiceScopeIds', COALESCE((SELECT JSON_ARRAYAGG(s.practice_scope_id)
                                                 FROM professional_activity_practice_scope s
                                                 WHERE s.activity_id = p.id), JSON_ARRAY()),
                   'legacy', JSON_OBJECT('table', 'professional_activity', 'id', p.id)),
       p.version
FROM professional_activity p
ORDER BY p.id;

-- Trạng thái tài khoản tại thời điểm (FR-026a): mỗi lần chuyển một dòng POINT.
INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            details, unique_key)
SELECT 'M15.ACCOUNT', account_id, 'STATUS', 'M15', 'POINT', status, effective_from, actor_id, actor_id IS NULL,
       JSON_OBJECT('lockCause', lock_cause, 'auditLogId', audit_log_id,
                   'legacy', JSON_OBJECT('table', 'account_status_history', 'id', id)),
       CONCAT('M15.ASH:', account_id, ':', DATE_FORMAT(effective_from, '%Y-%m-%dT%H:%i:%s.%f'))
FROM account_status_history
ORDER BY id;

-- Bản ghi ngoại tuyến chờ xem lại: quyết định đầu tiên thắng (khóa dòng, version), dòng đã quyết định khóa.
INSERT INTO simple_record (kind, module, status, content, unique_key, locked_at, created_at, created_by, version)
SELECT 'M15.PENDING_REVIEW_RECORD', 'M15', status,
       JSON_OBJECT('recordType', record_type,
                   'content', content,
                   'idempotencyKey', idempotency_key,
                   'recorderId', recorder_id,
                   'deviceId', device_id,
                   'deviceTime', DATE_FORMAT(device_time, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'syncedAt', DATE_FORMAT(synced_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'lastConnectedAt', DATE_FORMAT(last_connected_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'pendingReason', pending_reason,
                   'floorAreaId', floor_area_id,
                   'decidedBy', decided_by,
                   'decidedAt', DATE_FORMAT(decided_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'rejectionReason', rejection_reason,
                   'resultReference', result_reference,
                   'legacy', JSON_OBJECT('table', 'pending_review_record', 'id', id)),
       CONCAT('M15.PRR:', recorder_id, ':', idempotency_key),
       decided_at, synced_at, recorder_id, version
FROM pending_review_record
ORDER BY id;

-- Lỗi dữ liệu nguồn của lần kiểm quyền: một lỗi đang mở mỗi (nguồn, bản ghi, loại lỗi). Không rõ người kiểm quyền của
-- dòng cũ nên created_by = 0.
INSERT INTO simple_record (kind, module, status, content, unique_key, created_at, created_by)
SELECT 'M15.PERMISSION_DATA_ERROR', 'M15', IF(fixed_at IS NULL, 'OPEN', 'FIXED'),
       JSON_OBJECT('source', source,
                   'recordId', record_id,
                   'errorKind', error_kind,
                   'firstAt', DATE_FORMAT(first_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'lastAt', DATE_FORMAT(last_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'occurrences', occurrences,
                   'fixedAt', DATE_FORMAT(fixed_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'legacy', JSON_OBJECT('table', 'permission_data_error', 'id', id)),
       IF(fixed_at IS NULL, CONCAT('M15.PDE:', source, '|', IFNULL(record_id, ''), '|', error_kind), NULL),
       first_at, 0
FROM permission_data_error
ORDER BY id;

INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at)
SELECT 'M15.LICENSE_EXPIRY_WARNING', kind, license_id, DATE_FORMAT(expiry_date, '%Y-%m-%d'), warned_at
FROM license_expiry_warning;

-- Bỏ bảng cũ (gỡ quyền trước: quyền theo bảng không tự mất khi DROP TABLE).
REVOKE INSERT ON account_status_history FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON role_assignment FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON permission_restriction FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON additional_grant FROM 'eldercare_app'@'%';
REVOKE UPDATE (permission_version) ON role FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON permission_data_error FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON pending_review_record FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON professional_activity FROM 'eldercare_app'@'%';
REVOKE INSERT, DELETE ON professional_activity_practice_scope FROM 'eldercare_app'@'%';
REVOKE INSERT ON license_expiry_warning FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON login_qr_code FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON bound_device FROM 'eldercare_app'@'%';

ALTER TABLE permission_matrix
    DROP FOREIGN KEY fk_permission_matrix_role;

DROP TABLE bound_device;
DROP TABLE login_qr_code;
DROP TABLE role_assignment;
DROP TABLE permission_restriction;
DROP TABLE additional_grant;
DROP TABLE field_visibility_rule;
DROP TABLE professional_activity_practice_scope;
DROP TABLE professional_activity;
DROP TABLE license_expiry_warning;
DROP TABLE account_status_history;
DROP TABLE pending_review_record;
DROP TABLE permission_data_error;
DROP TABLE role;

GRANT INSERT, UPDATE ON permission_assignment TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON device_credential TO 'eldercare_app'@'%';

ALTER TABLE account COMMENT = 'DataGroup=BUSINESS';
ALTER TABLE permission_matrix COMMENT = 'DataGroup=CATALOG';
ALTER TABLE login_session COMMENT = 'DataGroup=TECHNICAL';
ALTER TABLE device COMMENT = 'DataGroup=TECHNICAL';
