-- 002 T006: vai trò, ma trận quyền, gán vai trò, thu hẹp, quyền gán thêm, quy tắc xem trường (data-model mục 3;
-- research A-04, A-05, A-08; FR-016 → FR-027).
-- role, permission_matrix, field_visibility_rule: nhóm 1 cố định, nạp bằng migration, không có API sửa (chỉ SELECT).
-- role_assignment, permission_restriction, additional_grant: nhóm 1 có lịch sử; thay đổi là đóng dòng cũ
-- (effective_to) và thêm dòng mới, không xóa (research A-05). Vai trò AC-16 thêm ở V002_08 (T090).
CREATE TABLE role
(
    code               VARCHAR(5)   NOT NULL PRIMARY KEY COMMENT 'Mã actor, ví dụ AC-01 (common.Role.code)',
    name               VARCHAR(100) NOT NULL,
    account_type       ENUM ('STAFF', 'FAMILY_MEMBER') NOT NULL,
    execution_scope    ENUM ('ASSIGNED', 'FACILITY', 'RELATIONSHIP') NOT NULL COMMENT 'Phạm vi của T, D (FR-034)',
    permission_version INT          NOT NULL DEFAULT 0 COMMENT 'Tăng khi thu hẹp theo vai trò (A-05)'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO role (code, name, account_type, execution_scope)
VALUES ('AC-01', 'Quản lý viện', 'STAFF', 'FACILITY'),
       ('AC-02', 'Trưởng tầng', 'STAFF', 'ASSIGNED'),
       ('AC-03', 'Bác sĩ', 'STAFF', 'FACILITY'),
       ('AC-04', 'Điều dưỡng', 'STAFF', 'ASSIGNED'),
       ('AC-05', 'Nhân viên chăm sóc', 'STAFF', 'ASSIGNED'),
       ('AC-07', 'Nhân viên bếp', 'STAFF', 'FACILITY'),
       ('AC-09', 'Nhân viên hành chính', 'STAFF', 'FACILITY'),
       ('AC-10', 'Người thân', 'FAMILY_MEMBER', 'RELATIONSHIP'),
       ('AC-14', 'Kế toán', 'STAFF', 'FACILITY');

-- Quyền tối đa: mỗi ô Phụ lục 27 thành 0..n dòng, cộng điều chỉnh của 19.3 (source = SECTION_19_3); nạp ở V002_04.
CREATE TABLE permission_matrix
(
    role           VARCHAR(5)  NOT NULL,
    function_code  VARCHAR(40) NOT NULL COMMENT 'Tên giá trị m15.account.api.Function',
    operation      ENUM ('VIEW', 'CREATE', 'UPDATE', 'CONFIRM', 'APPROVE', 'FINALIZE', 'PROFESSIONAL') NOT NULL,
    scope          ENUM ('FACILITY', 'ASSIGNED', 'RELATIONSHIP') NOT NULL,
    footnote_codes JSON        NOT NULL COMMENT 'Mã chú thích của ô, ví dụ ["10", "43"]',
    source         ENUM ('APPENDIX_27', 'SECTION_19_3') NOT NULL,
    PRIMARY KEY (role, function_code, operation),
    CONSTRAINT fk_permission_matrix_role FOREIGN KEY (role) REFERENCES role (code) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE role_assignment
(
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id     BIGINT        NOT NULL,
    role           VARCHAR(5)    NOT NULL,
    effective_from DATETIME(6)   NOT NULL,
    effective_to   DATETIME(6)   NULL,
    assigned_by    BIGINT        NULL COMMENT 'NULL: khởi tạo hệ thống (BR-M15-22)',
    reason         VARCHAR(2000) NOT NULL,
    -- Mỗi (account_id, role) tối đa một dòng còn hiệu lực.
    active_role    VARCHAR(5) GENERATED ALWAYS AS (IF(effective_to IS NULL, role, NULL)) STORED,
    CONSTRAINT uq_role_assignment_active UNIQUE (account_id, active_role),
    CONSTRAINT fk_role_assignment_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_assignment_role FOREIGN KEY (role) REFERENCES role (code) ON DELETE RESTRICT,
    CONSTRAINT chk_role_assignment_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT chk_role_assignment_period CHECK (effective_to IS NULL OR effective_to >= effective_from),
    KEY idx_role_assignment_role (role, effective_to)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Thu hẹp theo vai trò hoặc theo tài khoản: bỏ một loại thao tác hoặc hạ phạm vi (FR-025). metric_group, channel chỉ
-- với DASHBOARD_REPORT (016 FR-010a).
CREATE TABLE permission_restriction
(
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    applies_to     ENUM ('ROLE', 'ACCOUNT') NOT NULL,
    role           VARCHAR(5)    NULL,
    account_id     BIGINT        NULL,
    function_code  VARCHAR(40)   NOT NULL,
    operation      ENUM ('VIEW', 'CREATE', 'UPDATE', 'CONFIRM', 'APPROVE', 'FINALIZE', 'PROFESSIONAL') NOT NULL,
    kind           ENUM ('REMOVE_OPERATION', 'LOWER_SCOPE') NOT NULL,
    metric_group   VARCHAR(40)   NULL,
    channel        ENUM ('DASHBOARD', 'REPORT') NULL,
    effective_from DATETIME(6)   NOT NULL,
    effective_to   DATETIME(6)   NULL,
    actor_id       BIGINT        NOT NULL,
    reason         VARCHAR(2000) NOT NULL,
    CONSTRAINT fk_permission_restriction_role FOREIGN KEY (role) REFERENCES role (code) ON DELETE RESTRICT,
    CONSTRAINT fk_permission_restriction_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT chk_permission_restriction_target CHECK (
        (applies_to = 'ROLE' AND role IS NOT NULL AND account_id IS NULL)
            OR (applies_to = 'ACCOUNT' AND account_id IS NOT NULL AND role IS NULL)),
    CONSTRAINT chk_permission_restriction_dashboard CHECK (
        function_code = 'DASHBOARD_REPORT' OR (metric_group IS NULL AND channel IS NULL)),
    CONSTRAINT chk_permission_restriction_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT chk_permission_restriction_period CHECK (effective_to IS NULL OR effective_to >= effective_from)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Ngoại lệ duy nhất được thêm vượt ma trận: Duyệt kế hoạch chăm sóc cho Điều dưỡng (FR-024, Q-07).
CREATE TABLE additional_grant
(
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id     BIGINT        NOT NULL,
    function_code  VARCHAR(40)   NOT NULL,
    operation      ENUM ('VIEW', 'CREATE', 'UPDATE', 'CONFIRM', 'APPROVE', 'FINALIZE', 'PROFESSIONAL') NOT NULL,
    effective_from DATETIME(6)   NOT NULL,
    effective_to   DATETIME(6)   NULL,
    actor_id       BIGINT        NOT NULL,
    reason         VARCHAR(2000) NOT NULL,
    CONSTRAINT fk_additional_grant_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT chk_additional_grant_key CHECK (function_code = 'CARE_PLAN' AND operation = 'APPROVE'),
    CONSTRAINT chk_additional_grant_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT chk_additional_grant_period CHECK (effective_to IS NULL OR effective_to >= effective_from)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Vai trò × nhóm thông tin (FR-022; 19.3, chú thích ⁵, ¹⁴, ¹⁶, ³⁰); nạp ở V002_04.
CREATE TABLE field_visibility_rule
(
    role       VARCHAR(5) NOT NULL,
    info_group ENUM ('IDENTITY', 'CONTACT', 'CARE_LEVEL', 'HEALTH', 'MEDICATION', 'WISHES', 'COSTS', 'FINANCE',
        'INTERNAL_PUBLIC') NOT NULL,
    visibility ENUM ('FULL', 'PRESENCE_ONLY', 'NONE') NOT NULL,
    PRIMARY KEY (role, info_group),
    CONSTRAINT fk_field_visibility_rule_role FOREIGN KEY (role) REFERENCES role (code) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

GRANT INSERT, UPDATE ON role_assignment TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON permission_restriction TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON additional_grant TO 'eldercare_app'@'%';
-- Tăng permission_version khi thu hẹp theo vai trò.
GRANT UPDATE (permission_version) ON role TO 'eldercare_app'@'%';
