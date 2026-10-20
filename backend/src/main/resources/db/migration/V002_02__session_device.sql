-- 002 T005: phiên đăng nhập và thiết bị (data-model mục 2; research A-02; FR-013, FR-013a, FR-014a, FR-041c).
-- Hai bảng kỹ thuật, được UPDATE; thay đổi nghiệp vụ tương ứng (đăng nhập, khóa) đã có nhật ký (plan Complexity Tracking).
-- Kênh RESIDENT_APP và các lý do kết thúc phiên của người cao tuổi thêm ở V002_08 (T090).
CREATE TABLE login_session
(
    token_hash     BINARY(32)  NOT NULL PRIMARY KEY COMMENT 'SHA-256 của token, không lưu token',
    account_id     BIGINT      NOT NULL,
    device_id      CHAR(36)    NOT NULL COMMENT 'Giá trị X-Device-Id',
    channel        ENUM ('WEB', 'STAFF_APP', 'FAMILY_APP') NOT NULL,
    started_at     DATETIME(6) NOT NULL,
    last_active_at DATETIME(6) NOT NULL,
    ended_at       DATETIME(6) NULL,
    end_reason     ENUM ('LOGOUT', 'ACCOUNT_LEFT_ACTIVE', 'DEVICE_SIGNED_IN_ELSEWHERE', 'TIMED_OUT') NULL,
    -- Mỗi thiết bị tối đa một phiên đang mở (BR-M15-15).
    open_device_id CHAR(36) GENERATED ALWAYS AS (IF(ended_at IS NULL, device_id, NULL)) STORED,
    CONSTRAINT uq_login_session_open_device UNIQUE (open_device_id),
    CONSTRAINT fk_login_session_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT chk_login_session_end CHECK ((ended_at IS NULL) = (end_reason IS NULL)),
    KEY idx_login_session_device (device_id, ended_at),
    KEY idx_login_session_account (account_id, ended_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- device: lần kết nối gần nhất, đối chiếu với lastConnectedAt của phong bì đồng bộ (FR-041c).
CREATE TABLE device
(
    device_id         CHAR(36)    NOT NULL PRIMARY KEY,
    last_connected_at DATETIME(6) NOT NULL,
    last_account_id   BIGINT      NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

GRANT INSERT, UPDATE ON login_session TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON device TO 'eldercare_app'@'%';
