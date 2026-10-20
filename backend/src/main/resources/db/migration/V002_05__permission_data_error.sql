-- 002 T024: lỗi dữ liệu nguồn của lần kiểm quyền (data-model mục 5 loi_du_lieu_quyen; research A-10; FR-041b, CHK059).
-- Bảng kỹ thuật, được UPDATE (bộ đếm, khắc phục); mỗi lần từ chối đã có nhật ký PERMISSION_DENIED_SOURCE_DATA.
-- Mỗi (source, record_id, error_kind) tối đa một lỗi đang mở; khắc phục rồi gặp lại thì mở dòng mới.
CREATE TABLE permission_data_error
(
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source      VARCHAR(60) NOT NULL COMMENT 'Module, API nguồn, ví dụ M03.BED_ALLOCATION',
    record_id   BIGINT      NULL COMMENT 'Bản ghi nguồn lỗi nếu biết',
    error_kind  VARCHAR(60) NOT NULL,
    first_at    DATETIME(6) NOT NULL,
    last_at     DATETIME(6) NOT NULL,
    occurrences INT         NOT NULL DEFAULT 1,
    fixed_at    DATETIME(6) NULL COMMENT 'Lần kiểm tra kế tiếp đọc được dữ liệu hợp lệ',
    open_key    VARCHAR(150) GENERATED ALWAYS AS (
        IF(fixed_at IS NULL, CONCAT(source, '|', IFNULL(record_id, ''), '|', error_kind), NULL)) STORED,
    CONSTRAINT uq_permission_data_error_open UNIQUE (open_key),
    CONSTRAINT chk_permission_data_error_occurrences CHECK (occurrences >= 1),
    KEY idx_permission_data_error_record (record_id, fixed_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

GRANT INSERT, UPDATE ON permission_data_error TO 'eldercare_app'@'%';
