-- 012 T019: bản ghi đón (ban_ghi_don, nhóm 3; data-model mục 4, 0.1; FR-021, FR-026, FR-027; BR-M10-03; research F-07).
-- Theo R-15: sự kiện Đã dùng, Không dùng (ban_ghi_don_su_kien) là record_event loại M10.PICKUP_RECORD_EVENT gắn
-- M10.PICKUP_RECORD, khóa một lần dùng (khoa_da_dung) là unique_key; lần chặn đón (lan_chan_don) là record_event loại
-- M10.PICKUP_BLOCK gắn hồ sơ; yêu cầu ngoại lệ đón (yeu_cau_ngoai_le_don) là simple_record loại M10.PICKUP_EXCEPTION.
-- Người cao tuổi, người thân là tham chiếu logic (người đón theo ngoại lệ không có hồ sơ người thân). Cú pháp dùng được ở
-- MySQL 8.0 và 8.4 (R-01).

-- Nhóm 3: ứng dụng chỉ INSERT (không UPDATE, DELETE); đính chính theo 000; job loại bỏ xóa theo hồ sơ.
CREATE TABLE pickup_record
(
    id                     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id            BIGINT       NOT NULL,
    type                   ENUM ('TEMPORARY_ABSENCE', 'FAMILY_OUTING', 'DAY_CARE_DEPARTURE', 'SELF_RETURN',
        'INSTITUTE_RETURN', 'RESIDENCY_END') NOT NULL,
    family_member_id       BIGINT       NULL COMMENT 'Người đón có hồ sơ người thân; trống với Tự về, ngoại lệ',
    basis                  ENUM ('PICKUP_LIST', 'REPRESENTATIVE_EXCEPTION', 'MANAGER_EXCEPTION', 'SELF_RETURN') NOT NULL,
    exception_id           BIGINT       NULL,
    picker_name            VARCHAR(200) NULL,
    id_document_type       VARCHAR(40)  NULL COMMENT 'Giấy tờ đã đối chiếu, khớp số đã đăng ký (FR-021 bước 3)',
    id_document_number     VARCHAR(40)  NULL,
    at                     DATETIME(6)  NOT NULL,
    handover_staff_id      BIGINT       NOT NULL COMMENT 'Nhân viên bàn giao; nhân viên tiễn (Tự về); người đưa về',
    recorded_by            BIGINT       NOT NULL,
    expires_at             DATETIME(6)  NULL COMMENT 'at + CFG-M10-09 lúc ghi; trống với Viện đưa về (FR-027)',
    created_at             DATETIME(6)  NOT NULL,
    CONSTRAINT ck_pickup_record_self_return CHECK ((type = 'SELF_RETURN') = (basis = 'SELF_RETURN')),
    CONSTRAINT ck_pickup_record_picker CHECK (basis = 'SELF_RETURN'
        OR (picker_name IS NOT NULL AND id_document_number IS NOT NULL)),
    CONSTRAINT ck_pickup_record_exception CHECK ((exception_id IS NOT NULL)
        = (basis IN ('REPRESENTATIVE_EXCEPTION', 'MANAGER_EXCEPTION'))),
    CONSTRAINT ck_pickup_record_expiry CHECK ((type = 'INSTITUTE_RETURN') = (expires_at IS NULL)),
    CONSTRAINT uq_pickup_record_exception UNIQUE (exception_id),
    KEY ix_pickup_record_resident (resident_id, at),
    KEY ix_pickup_record_expiry (expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=RECORD';

-- Trigger nhóm 3 (000 data-model mục 1.4): không sửa; chỉ job loại bỏ dữ liệu (eldercare_retention) được xóa. USER() là
-- tài khoản đang kết nối (trong trigger CURRENT_USER() là tài khoản tạo trigger). Thông báo bằng ASCII.
CREATE TRIGGER trg_pickup_record_no_update
    BEFORE UPDATE
    ON pickup_record
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'pickup_record is append-only (012 FR-026)';

DELIMITER $$
CREATE TRIGGER trg_pickup_record_no_delete
    BEFORE DELETE
    ON pickup_record
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'pickup_record rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT ON pickup_record TO 'eldercare_app'@'%';
GRANT DELETE ON pickup_record TO 'eldercare_retention'@'%';
