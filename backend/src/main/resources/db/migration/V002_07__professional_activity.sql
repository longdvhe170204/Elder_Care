-- 002 T063: danh mục nghiệp vụ chuyên môn và cảnh báo giấy phép (data-model mục 4; research A-09; FR-036 → FR-039).
-- professional_activity là danh mục nhóm 1 (tên, trạng thái, version như CatalogBase của 000), sửa bằng UPDATE kèm nhật
-- ký PROFESSIONAL_ACTIVITY_*; mục mặc định không ngừng hiệu lực được (FR-036). Thao tác (function_code, PROFESSIONAL)
-- phải thỏa mọi mục đang hiệu lực của chức năng đó (FR-037). Loại giấy phép là mã loại trong danh mục của 008 (FR-005),
-- NULL là chưa cấu hình: thao tác chuyên môn bị từ chối tới khi Quản lý viện cấu hình (BR-M06-05).
CREATE TABLE professional_activity
(
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code                    VARCHAR(60)  NOT NULL,
    name                    VARCHAR(200) NOT NULL,
    function_code           VARCHAR(60)  NOT NULL COMMENT 'Chức năng Phụ lục 27 (Function), thao tác PROFESSIONAL',
    required_license_type   VARCHAR(60)  NULL COMMENT 'Mã loại giấy phép/đào tạo của 008; NULL: chưa cấu hình',
    requires_facility_scope BOOLEAN      NOT NULL,
    is_default              BOOLEAN      NOT NULL DEFAULT FALSE,
    status                  ENUM ('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    active_name             VARCHAR(200) GENERATED ALWAYS AS (IF(status = 'ACTIVE', name, NULL)) STORED,
    version                 INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_professional_activity_code UNIQUE (code),
    CONSTRAINT uq_professional_activity_active_name UNIQUE (active_name),
    CONSTRAINT chk_professional_activity_default CHECK (NOT is_default OR status = 'ACTIVE'),
    KEY idx_professional_activity_function (function_code, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Mục phạm vi hành nghề yêu cầu, thuộc danh mục phạm vi hành nghề của 008 (FR-037 (b), BR-M15-19).
CREATE TABLE professional_activity_practice_scope
(
    activity_id       BIGINT NOT NULL,
    practice_scope_id BIGINT NOT NULL,
    PRIMARY KEY (activity_id, practice_scope_id),
    CONSTRAINT fk_professional_activity_practice_scope FOREIGN KEY (activity_id) REFERENCES professional_activity (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Bảng kỹ thuật chống cảnh báo lặp cho cùng một ngày hết hạn (FR-039; plan Complexity Tracking).
CREATE TABLE license_expiry_warning
(
    kind        ENUM ('FACILITY', 'PRACTITIONER') NOT NULL,
    license_id  BIGINT      NOT NULL,
    expiry_date DATE        NOT NULL,
    warned_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (kind, license_id, expiry_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO professional_activity (code, name, function_code, required_license_type, requires_facility_scope, is_default)
VALUES ('INTERNAL_DIAGNOSIS_PRESCRIBING', 'Chẩn đoán và kê đơn nội bộ', 'PRESCRIPTION', NULL, TRUE, TRUE),
       ('MEDICATION_ADMINISTRATION', 'Phát thuốc và xác nhận liều', 'MEDICATION_ADMINISTRATION', NULL, FALSE, TRUE);

GRANT INSERT, UPDATE ON professional_activity TO 'eldercare_app'@'%';
GRANT INSERT, DELETE ON professional_activity_practice_scope TO 'eldercare_app'@'%';
GRANT INSERT ON license_expiry_warning TO 'eldercare_app'@'%';
