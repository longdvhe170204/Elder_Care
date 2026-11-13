-- 000 T048: dữ liệu mẫu cho profile demo và test (spec 000 US1 "Independent Test", CHK029; quickstart mục 1).
-- Mỗi nhóm dữ liệu một loại mẫu, dựng theo mẫu migration ở backend/docs/data-group-conventions.md (000 T045).
-- Entity, service của dữ liệu mẫu chỉ có trong mã test (com.cambae.backend.m15.sample).

-- Nhóm 1 – Danh mục: tên Hiệu lực không trùng nhờ cột sinh active_name có UNIQUE (FR-003, BR-M15-06).
CREATE TABLE sample_catalog
(
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    status      ENUM ('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    active_name VARCHAR(100) GENERATED ALWAYS AS (IF(status = 'ACTIVE', name, NULL)) STORED,
    version     INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_sample_catalog_active_name UNIQUE (active_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhóm 2 – Nghiệp vụ có trạng thái: chỉ đổi qua lệnh; tham chiếu danh mục bằng khóa ngoại RESTRICT (FR-002, FR-005).
CREATE TABLE sample_case
(
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    catalog_id BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    version    INT         NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    created_by BIGINT      NOT NULL,
    CONSTRAINT fk_sample_case_catalog FOREIGN KEY (catalog_id) REFERENCES sample_catalog (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Phiên bản có hiệu lực theo ngày lịch, gồm cả ngày cuối; không chồng khoảng kiểm ở backend (FR-004, FR-008).
CREATE TABLE sample_case_version
(
    id             BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    case_id        BIGINT      NOT NULL,
    effective_from DATE        NOT NULL,
    effective_to   DATE        NULL,
    note           VARCHAR(200) NULL,
    CONSTRAINT fk_sample_case_version_case FOREIGN KEY (case_id) REFERENCES sample_case (id) ON DELETE RESTRICT,
    CONSTRAINT chk_sample_case_version_range CHECK (effective_to IS NULL OR effective_to >= effective_from)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhóm 3 – Ghi nhận đã xác nhận, loại gắn tầng/khu vực (M15.SAMPLE_RECORD; FR-026 b).
CREATE TABLE sample_floor_record
(
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    catalog_id    BIGINT       NULL,
    occurred_at   DATETIME(6)  NOT NULL,
    device_time   DATETIME(6)  NULL,
    actor_id      BIGINT       NOT NULL,
    floor_area_id BIGINT       NOT NULL,
    note          VARCHAR(200) NULL,
    CONSTRAINT fk_sample_floor_record_catalog FOREIGN KEY (catalog_id) REFERENCES sample_catalog (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhóm 3, loại không gắn tầng/khu vực (ví dụ bản ghi do Hành chính ghi; FR-026 c).
CREATE TABLE sample_office_record
(
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    occurred_at DATETIME(6)  NOT NULL,
    device_time DATETIME(6)  NULL,
    actor_id    BIGINT       NOT NULL,
    note        VARCHAR(200) NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Hai trigger mẫu nhóm 3 cho mỗi bảng (data-model mục 1.4).
CREATE TRIGGER trg_sample_floor_record_no_update
    BEFORE UPDATE
    ON sample_floor_record
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sample_floor_record is append-only (FR-009)';

DELIMITER $$
CREATE TRIGGER trg_sample_floor_record_no_delete
    BEFORE DELETE
    ON sample_floor_record
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sample_floor_record rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

CREATE TRIGGER trg_sample_office_record_no_update
    BEFORE UPDATE
    ON sample_office_record
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sample_office_record is append-only (FR-009)';

DELIMITER $$
CREATE TRIGGER trg_sample_office_record_no_delete
    BEFORE DELETE
    ON sample_office_record
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sample_office_record rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

-- Danh mục: tạo, sửa, ngừng hiệu lực, xóa khi chưa được tham chiếu; nhóm 2: thêm và đổi qua lệnh; nhóm 3: chỉ thêm.
GRANT INSERT, UPDATE, DELETE ON sample_catalog TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON sample_case TO 'eldercare_app'@'%';
GRANT INSERT ON sample_case_version TO 'eldercare_app'@'%';
GRANT INSERT ON sample_floor_record TO 'eldercare_app'@'%';
GRANT INSERT ON sample_office_record TO 'eldercare_app'@'%';
GRANT DELETE ON sample_floor_record TO 'eldercare_retention'@'%';
GRANT DELETE ON sample_office_record TO 'eldercare_retention'@'%';

INSERT INTO sample_catalog (name) VALUES ('Mục mẫu 1'), ('Mục mẫu 2');
