-- 001 T005: hồ sơ người cao tuổi, lịch sử trạng thái, mức chăm sóc (data-model mục 2, 3, 9; research H-04). Cú pháp dùng
-- được ở MySQL 8.0 và 8.4 (research R-01 của 000). Tham chiếu tới tệp (photo_file_id), tài khoản, sự cố, yêu cầu phê
-- duyệt là định danh logic, không có khóa ngoại chéo module (constitution X); khóa ngoại tới bảng `file` thêm bằng
-- migration riêng khi T003 vào develop.

-- Ba mức chăm sóc: danh mục cố định, nạp từ migration, không có API tạo, sửa, xóa (Q-291).
CREATE TABLE care_level
(
    code          VARCHAR(20)  NOT NULL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    display_order INT          NOT NULL,
    CONSTRAINT uq_care_level_order UNIQUE (display_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO care_level (code, name, display_order)
VALUES ('BASIC', 'Chăm sóc cơ bản', 1),
       ('REGULAR', 'Chăm sóc thường xuyên', 2),
       ('SPECIAL', 'Chăm sóc đặc biệt', 3);

-- Hồ sơ người cao tuổi, nhóm 2. profile_code dạng NCT-<năm>-<id 6 chữ số> đặt ngay sau INSERT trong cùng transaction
-- (cột sinh không dùng được với AUTO_INCREMENT), nên NULL cho tới lúc đó. Trạng thái cuối: Kết thúc lưu trú, Qua đời,
-- Hủy tiếp nhận.
CREATE TABLE resident
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profile_code        VARCHAR(20)  NULL,
    previous_profile_id BIGINT       NULL COMMENT 'Hồ sơ trước cùng CCCD đã ở Kết thúc lưu trú hoặc Hủy tiếp nhận (FR-004)',
    full_name           VARCHAR(200) NOT NULL,
    -- Không phân biệt hoa thường, bỏ khoảng trắng thừa, có phân biệt dấu (H-04, FR-005)
    full_name_match     VARCHAR(200) COLLATE utf8mb4_0900_as_ci
        GENERATED ALWAYS AS (TRIM(REGEXP_REPLACE(full_name, '[[:space:]]+', ' '))) STORED,
    date_of_birth       DATE         NOT NULL,
    gender              ENUM ('MALE', 'FEMALE') NOT NULL,
    id_document_type    ENUM ('CITIZEN_ID', 'OTHER') NULL,
    id_document_number  VARCHAR(30)  NULL,
    photo_file_id       BIGINT       NULL COMMENT 'Tệp ảnh (common.file, H-13)',
    permanent_address   VARCHAR(500) NULL,
    contact_address     VARCHAR(500) NULL,
    contact             JSON         NULL COMMENT 'Số điện thoại, thư điện tử',
    special_notes       TEXT         NULL,
    status              ENUM ('ADMISSION_IN_PROGRESS', 'IN_RESIDENCE', 'TEMPORARILY_ABSENT', 'OFF_SITE_ACTIVITY',
        'HOSPITALIZED', 'RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED') NOT NULL,
    status_since        DATETIME(6)  NOT NULL COMMENT 'Thời điểm vào trạng thái hiện tại (FR-046)',
    current_care_level  VARCHAR(20)  NULL COMMENT 'NULL tới khi đánh giá đầu vào được xác nhận (FR-033)',
    version             INT          NOT NULL DEFAULT 0 COMMENT 'Khóa lạc quan, chỉ cho Cập nhật thông tin cá nhân (FR-006)',
    created_at          DATETIME(6)  NOT NULL,
    created_by          BIGINT       NOT NULL,
    -- CCCD duy nhất trong hồ sơ chưa ở trạng thái cuối (DBR-01)
    open_citizen_id     VARCHAR(30) GENERATED ALWAYS AS (
        IF(id_document_type = 'CITIZEN_ID' AND status NOT IN ('RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED'),
           id_document_number, NULL)) STORED,
    CONSTRAINT uq_resident_profile_code UNIQUE (profile_code),
    CONSTRAINT uq_resident_open_citizen_id UNIQUE (open_citizen_id),
    CONSTRAINT fk_resident_previous FOREIGN KEY (previous_profile_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT fk_resident_care_level FOREIGN KEY (current_care_level) REFERENCES care_level (code) ON DELETE RESTRICT,
    CONSTRAINT chk_resident_document CHECK ((id_document_type IS NULL) = (id_document_number IS NULL)),
    KEY idx_resident_match (full_name_match, date_of_birth),
    KEY idx_resident_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Lịch sử trạng thái, nhóm 3 chỉ thêm (BR-M01-04, FR-046). Lý do bắt buộc trừ lệnh do hệ thống (điểm danh của 014) và
-- Tạo hồ sơ. basis_type/basis_id: sự cố, chuyến đi, chỉ định, bằng chứng.
CREATE TABLE resident_status_history
(
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id      BIGINT        NOT NULL,
    from_status      ENUM ('ADMISSION_IN_PROGRESS', 'IN_RESIDENCE', 'TEMPORARILY_ABSENT', 'OFF_SITE_ACTIVITY',
        'HOSPITALIZED', 'RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED') NULL COMMENT 'NULL với CREATE_PROFILE',
    to_status        ENUM ('ADMISSION_IN_PROGRESS', 'IN_RESIDENCE', 'TEMPORARILY_ABSENT', 'OFF_SITE_ACTIVITY',
        'HOSPITALIZED', 'RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED') NOT NULL,
    command          VARCHAR(30)   NOT NULL,
    occurred_at      DATETIME(6)   NOT NULL COMMENT 'Đồng hồ máy chủ',
    performed_by     BIGINT        NOT NULL COMMENT 'Tài khoản; với lệnh từ 014 là người điểm danh',
    by_system        BOOLEAN       NOT NULL DEFAULT FALSE,
    reason           VARCHAR(2000) NULL,
    basis_type       VARCHAR(30)   NULL,
    basis_id         BIGINT        NULL,
    condition_waiver JSON          NULL COMMENT 'Yêu cầu ngoại lệ đã duyệt và các điều kiện được miễn (US4 #10)',
    CONSTRAINT fk_resident_status_history_resident FOREIGN KEY (resident_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT chk_resident_status_history_reason CHECK (reason IS NOT NULL OR by_system OR command = 'CREATE_PROFILE'),
    CONSTRAINT chk_resident_status_history_basis CHECK ((basis_type IS NULL) = (basis_id IS NULL)),
    KEY idx_resident_status_history_resident (resident_id, occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Mức chăm sóc theo thời gian, nhóm 3 chỉ thêm (FR-052, FR-053). effective_from: thời điểm xác nhận đánh giá, hoặc 00:00
-- của ngày hiệu lực thực tế của yêu cầu thay đổi lưu trú (BR-M15-07). basis_id: lần đánh giá, hoặc yêu cầu phê duyệt.
CREATE TABLE care_level_history
(
    id             BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id    BIGINT      NOT NULL,
    care_level     VARCHAR(20) NOT NULL,
    effective_from DATETIME(6) NOT NULL,
    basis_type     ENUM ('ASSESSMENT', 'RESIDENCY_CHANGE_REQUEST') NOT NULL,
    basis_id       BIGINT      NOT NULL,
    recorded_at    DATETIME(6) NOT NULL,
    recorded_by    BIGINT      NOT NULL,
    CONSTRAINT uq_care_level_history UNIQUE (resident_id, effective_from),
    CONSTRAINT fk_care_level_history_resident FOREIGN KEY (resident_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT fk_care_level_history_level FOREIGN KEY (care_level) REFERENCES care_level (code) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Chỉ job loại bỏ dữ liệu hết hạn (tài khoản eldercare_retention) xóa được, theo FR-051 và LuuTruProvider của M01.
-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
CREATE TRIGGER trg_resident_no_delete
    BEFORE DELETE
    ON resident
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'resident rows are removed only by the retention job (FR-051)';
    END IF;
END$$

CREATE TRIGGER trg_resident_status_history_no_delete
    BEFORE DELETE
    ON resident_status_history
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'resident_status_history rows are removed only by the retention job (FR-051)';
    END IF;
END$$

CREATE TRIGGER trg_care_level_history_no_delete
    BEFORE DELETE
    ON care_level_history
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'care_level_history rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

CREATE TRIGGER trg_resident_status_history_no_update
    BEFORE UPDATE
    ON resident_status_history
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'resident_status_history is append-only (FR-009)';

CREATE TRIGGER trg_care_level_history_no_update
    BEFORE UPDATE
    ON care_level_history
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'care_level_history is append-only (FR-009)';

GRANT INSERT, UPDATE ON resident TO 'eldercare_app'@'%';
GRANT INSERT ON resident_status_history TO 'eldercare_app'@'%';
GRANT INSERT ON care_level_history TO 'eldercare_app'@'%';
GRANT DELETE ON resident TO 'eldercare_retention'@'%';
GRANT DELETE ON resident_status_history TO 'eldercare_retention'@'%';
GRANT DELETE ON care_level_history TO 'eldercare_retention'@'%';
