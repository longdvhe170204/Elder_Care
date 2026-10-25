-- Tệp đính kèm dùng chung (001 T003; research H-13, NFR-08). Nội dung nằm trong bucket S3, khóa đối tượng là uuid; bảng
-- này chỉ giữ siêu dữ liệu. Bảng kỹ thuật: tệp vừa tải lên chưa có chủ sở hữu, lệnh nghiệp vụ gắn một lần; tệp đã gắn
-- không đổi được. Số phiên bản V001_06_1 (không phải V001_01 như tasks ghi) vì V001_02 → V001_06 đã áp dụng ở các cơ
-- sở dữ liệu đang chạy; thứ tự không ảnh hưởng vì cột photo_file_id của resident chưa có khóa ngoại tới bảng này.
CREATE TABLE file
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    uuid          CHAR(36)     NOT NULL COMMENT 'Khóa đối tượng trong bucket S3',
    original_name VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    sha256        CHAR(64)     NOT NULL,
    uploaded_by   BIGINT       NOT NULL COMMENT 'Tài khoản tải lên',
    uploaded_at   DATETIME(6)  NOT NULL,
    owner_type    VARCHAR(60)  NULL COMMENT 'Loại bản ghi chủ sở hữu, ví dụ M01.CONSENT; NULL tới khi gắn',
    owner_id      BIGINT       NULL,
    attached_at   DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_file_uuid (uuid),
    KEY ix_file_owner (owner_type, owner_id),
    KEY ix_file_unattached (attached_at, uploaded_at),
    CONSTRAINT ck_file_owner_all_or_none CHECK (
        (owner_type IS NULL AND owner_id IS NULL AND attached_at IS NULL)
            OR (owner_type IS NOT NULL AND owner_id IS NOT NULL AND attached_at IS NOT NULL))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$
CREATE TRIGGER trg_file_attached_immutable
    BEFORE UPDATE
    ON file
    FOR EACH ROW
BEGIN
    IF OLD.attached_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'file rows are immutable once attached (research H-13)';
    END IF;
END$$

-- Tệp chưa gắn là dữ liệu tạm, job dọn xóa được; tệp đã gắn theo hạn lưu giữ của bản ghi chủ sở hữu (000 R-11).
CREATE TRIGGER trg_file_no_delete_attached
    BEFORE DELETE
    ON file
    FOR EACH ROW
BEGIN
    IF OLD.attached_at IS NOT NULL AND SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'attached file rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, DELETE ON file TO 'eldercare_app'@'%';
GRANT UPDATE (owner_type, owner_id, attached_at) ON file TO 'eldercare_app'@'%';
GRANT DELETE ON file TO 'eldercare_retention'@'%';
