-- 002 T090: tài khoản người cao tuổi, mã QR đăng nhập, thiết bị gắn (data-model mục 2, 3; research A-15, A-16;
-- FR-067 → FR-075; Q-365, Q-370). Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

-- account: loại RESIDENT liên kết đúng một hồ sơ người cao tuổi (001), tối đa một tài khoản mỗi người (FR-067).
-- Nguyên nhân khóa mới: kết thúc lưu trú (sự kiện của 001), người cao tuổi tự yêu cầu, có lý do, theo đề nghị của
-- người thân qua yêu cầu phê duyệt (FR-072, A-16).
ALTER TABLE account
    MODIFY type ENUM ('STAFF', 'FAMILY_MEMBER', 'RESIDENT') NOT NULL,
    ADD COLUMN resident_id BIGINT NULL COMMENT 'Tham chiếu logic hồ sơ người cao tuổi (001)' AFTER family_member_id,
    MODIFY lock_cause ENUM ('MANUAL', 'RESIGNATION', 'EXPIRED_AFTER_RESIDENCY_END', 'RELATIONSHIP_ENDED',
        'RESIDENT_RESIDENCY_ENDED', 'RESIDENT_REQUESTED', 'WITH_REASON', 'FAMILY_REQUEST') NULL,
    ADD CONSTRAINT uq_account_resident UNIQUE (resident_id),
    DROP CHECK chk_account_link;

ALTER TABLE account
    ADD CONSTRAINT chk_account_link CHECK (
        (type = 'STAFF' AND staff_id IS NOT NULL AND family_member_id IS NULL AND resident_id IS NULL)
            OR (type = 'FAMILY_MEMBER' AND family_member_id IS NOT NULL AND staff_id IS NULL AND resident_id IS NULL)
            OR (type = 'RESIDENT' AND resident_id IS NOT NULL AND staff_id IS NULL AND family_member_id IS NULL));

ALTER TABLE account_status_history
    MODIFY lock_cause ENUM ('MANUAL', 'RESIGNATION', 'EXPIRED_AFTER_RESIDENCY_END', 'RELATIONSHIP_ENDED',
        'RESIDENT_RESIDENCY_ENDED', 'RESIDENT_REQUESTED', 'WITH_REASON', 'FAMILY_REQUEST') NULL;

-- Kênh người cao tuổi (không tự hết theo thời gian, CFG-M15-10) và lý do kết thúc phiên: gỡ thiết bị, đổi mật khẩu.
ALTER TABLE login_session
    MODIFY channel ENUM ('WEB', 'STAFF_APP', 'FAMILY_APP', 'RESIDENT_APP') NOT NULL,
    MODIFY end_reason ENUM ('LOGOUT', 'ACCOUNT_LEFT_ACTIVE', 'DEVICE_SIGNED_IN_ELSEWHERE', 'TIMED_OUT', 'DEVICE_UNBOUND',
        'PASSWORD_CHANGED') NULL;

-- Mã QR đăng nhập dùng một lần (FR-069): chỉ lưu SHA-256; hết hạn theo CFG-M15-11 đọc lúc tạo; mã mới vô hiệu mã cũ.
CREATE TABLE login_qr_code
(
    id             BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id     BIGINT      NOT NULL,
    code_hash      BINARY(32)  NOT NULL COMMENT 'SHA-256 của mã, không lưu mã',
    created_by     BIGINT      NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    expires_at     DATETIME(6) NOT NULL,
    used_at        DATETIME(6) NULL,
    used_device_id CHAR(36)    NULL,
    revoked_at     DATETIME(6) NULL COMMENT 'Đặt khi có mã mới của cùng tài khoản',
    CONSTRAINT uq_login_qr_code_hash UNIQUE (code_hash),
    CONSTRAINT fk_login_qr_code_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT chk_login_qr_code_used CHECK ((used_at IS NULL) = (used_device_id IS NULL)),
    CONSTRAINT chk_login_qr_code_expiry CHECK (expires_at > created_at),
    KEY idx_login_qr_code_account (account_id, used_at, revoked_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Thiết bị gắn với tài khoản người cao tuổi (FR-070, FR-071, BR-M15-24): token thiết bị cho nút SOS, chỉ lưu SHA-256.
-- Tối đa một thiết bị đang gắn mỗi tài khoản, một tài khoản đang gắn mỗi thiết bị (FR-014a). Chỉ thêm và gỡ một lần.
CREATE TABLE bound_device
(
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id        BIGINT        NOT NULL,
    device_id         CHAR(36)      NOT NULL COMMENT 'Giá trị X-Device-Id lúc gắn',
    device_token_hash BINARY(32)    NOT NULL COMMENT 'SHA-256 của token thiết bị',
    bound_at          DATETIME(6)   NOT NULL,
    login_qr_code_id  BIGINT        NULL COMMENT 'NULL khi gắn bằng mật khẩu dự phòng',
    unbound_at        DATETIME(6)   NULL,
    unbound_by        BIGINT        NULL COMMENT 'NULL: Bộ lập lịch hoặc gắn máy mới',
    unbind_reason     ENUM ('NEW_DEVICE_BOUND', 'MANUAL', 'ACCOUNT_LOCKED') NULL,
    unbind_note       VARCHAR(2000) NULL COMMENT 'Lý do khi gỡ thủ công',
    active_account_id BIGINT GENERATED ALWAYS AS (IF(unbound_at IS NULL, account_id, NULL)) STORED,
    active_device_id  CHAR(36) GENERATED ALWAYS AS (IF(unbound_at IS NULL, device_id, NULL)) STORED,
    CONSTRAINT uq_bound_device_token UNIQUE (device_token_hash),
    CONSTRAINT uq_bound_device_active_account UNIQUE (active_account_id),
    CONSTRAINT uq_bound_device_active_device UNIQUE (active_device_id),
    CONSTRAINT fk_bound_device_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bound_device_qr FOREIGN KEY (login_qr_code_id) REFERENCES login_qr_code (id) ON DELETE RESTRICT,
    CONSTRAINT chk_bound_device_unbound CHECK ((unbound_at IS NULL) = (unbind_reason IS NULL)),
    CONSTRAINT chk_bound_device_note CHECK (unbind_reason <> 'MANUAL' OR CHAR_LENGTH(TRIM(unbind_note)) > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TRIGGER trg_login_qr_code_no_delete
    BEFORE DELETE
    ON login_qr_code
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'login_qr_code rows are never deleted';

CREATE TRIGGER trg_bound_device_no_delete
    BEFORE DELETE
    ON bound_device
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'bound_device rows are never deleted';

GRANT INSERT, UPDATE ON login_qr_code TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON bound_device TO 'eldercare_app'@'%';

-- Vai trò AC-16 (FR-016, Q-365), phạm vi chính mình (FR-068); cột NCT của Phụ lục 27 theo luật quy đổi T007:
-- X → VIEW, T → VIEW, CREATE, UPDATE; chú thích ⁴⁵, ⁴⁶, ⁴⁷. Đối chiếu bằng PermissionMatrixMatchesSourceIT.
ALTER TABLE role
    MODIFY account_type ENUM ('STAFF', 'FAMILY_MEMBER', 'RESIDENT') NOT NULL,
    MODIFY execution_scope ENUM ('ASSIGNED', 'FACILITY', 'RELATIONSHIP', 'SELF') NOT NULL COMMENT 'Phạm vi của T, D (FR-034)';

ALTER TABLE permission_matrix
    MODIFY scope ENUM ('FACILITY', 'ASSIGNED', 'RELATIONSHIP', 'SELF') NOT NULL;

INSERT INTO role (code, name, account_type, execution_scope)
VALUES ('AC-16', 'Người cao tuổi', 'RESIDENT', 'SELF');

INSERT INTO permission_matrix (role, function_code, operation, scope, footnote_codes, source)
VALUES ('AC-16', 'ACTIVITY_OUTING', 'VIEW', 'SELF', '["45"]', 'APPENDIX_27'),
       ('AC-16', 'SOS_BUTTON', 'VIEW', 'SELF', '["47"]', 'APPENDIX_27'),
       ('AC-16', 'SOS_BUTTON', 'CREATE', 'SELF', '["47"]', 'APPENDIX_27'),
       ('AC-16', 'SOS_BUTTON', 'UPDATE', 'SELF', '["47"]', 'APPENDIX_27'),
       ('AC-16', 'DISH_MENU', 'VIEW', 'SELF', '["45"]', 'APPENDIX_27'),
       ('AC-16', 'VISIT_REGISTRATION', 'VIEW', 'SELF', '["45"]', 'APPENDIX_27'),
       ('AC-16', 'FEEDBACK', 'VIEW', 'SELF', '["46"]', 'APPENDIX_27'),
       ('AC-16', 'FEEDBACK', 'CREATE', 'SELF', '["46"]', 'APPENDIX_27'),
       ('AC-16', 'FEEDBACK', 'UPDATE', 'SELF', '["46"]', 'APPENDIX_27'),
       ('AC-16', 'RESIDENT_CHANNEL', 'VIEW', 'SELF', '["45"]', 'APPENDIX_27'),
       ('AC-16', 'BELONGINGS', 'VIEW', 'SELF', '["45"]', 'APPENDIX_27');
