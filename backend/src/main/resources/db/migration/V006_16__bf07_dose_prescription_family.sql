-- 006, sửa BF-07 (2026-11-22, Q-426 → Q-436). Chỉ thêm cột, thêm hai bảng; không sửa migration cũ. Cú pháp MySQL 8.0 và 8.4.
--   Phần 1, 2: Hoãn, Nhận liều, chỉ số trước khi dùng, mức quan trọng về thời gian (medication_dose, prescription, dose_result).
--   Phần 3: Tiếp tục tạm đơn Quan trọng khi trở về từ bệnh viện (prescription).
--   Phần 4: mốc leo thang đối chiếu quá 8 giờ (medication_reconciliation).
--   Phần 5: chỉ định PRN; hiệu quả PRN (prn_effectiveness, bảng chỉ thêm vì dose_result chỉ INSERT).
--   Phần 7: kiểm tra tiếp nhận, loại hủy thuốc gia đình gửi (mốc gửi duyệt yêu cầu mua ở V019_08).
--   Phần 8: Điều dưỡng tầng khác phụ trách tạm (temporary_floor_nurse_assignment).

-- 1. Liều: dấu Hoãn có chủ đích (liều giữ trạng thái Đến giờ hoặc Trễ), người nhận liều
ALTER TABLE medication_dose
    ADD COLUMN postponed_at DATETIME(6) NULL COMMENT 'Hoãn có chủ đích gần nhất (BR-M07-26)',
    ADD COLUMN postponed_by_id BIGINT NULL,
    ADD COLUMN postponed_reason VARCHAR(2000) NULL,
    ADD COLUMN claimed_by_id BIGINT NULL COMMENT 'Điều dưỡng đang nhận liều (BR-M07-26)',
    ADD COLUMN claim_expires_at DATETIME(6) NULL COMMENT 'Nhận liều hết hạn sau CFG-M07-08';

-- Chỉ số đo trước khi dùng gắn với kết quả liều
ALTER TABLE dose_result
    ADD COLUMN pre_dose_measurements JSON NULL COMMENT 'Mảng {code, value} các chỉ số đo trước khi dùng (BR-M07-26)';

-- 2, 3, 5. Đơn: mức quan trọng, chỉ số cần đo, chỉ định PRN, Tiếp tục tạm. Các cột này không nằm trong danh sách khóa của
-- trg_prescription_lock nên sửa được như ghi chú hướng dẫn (11.1).
ALTER TABLE prescription
    ADD COLUMN time_criticality ENUM ('NORMAL', 'CRITICAL') NOT NULL DEFAULT 'NORMAL' COMMENT 'Mức quan trọng về thời gian (BR-M07-27)',
    ADD COLUMN pre_dose_measurements JSON NULL COMMENT 'Mã chỉ số phải đo trước khi dùng (BR-M07-26)',
    ADD COLUMN indication VARCHAR(500) NULL COMMENT 'Chỉ định dùng thuốc khi cần (PRN)',
    ADD COLUMN temp_resumed_at DATETIME(6) NULL COMMENT 'Tiếp tục tạm theo giấy ra viện (BR-M07-28)',
    ADD COLUMN temp_resumed_by_id BIGINT NULL,
    ADD COLUMN temp_resume_basis VARCHAR(2000) NULL,
    ADD COLUMN temp_resume_file_id BIGINT NULL;

-- 4. Phiếu đối chiếu quá hạn thứ hai
ALTER TABLE medication_reconciliation
    ADD COLUMN escalated_at DATETIME(6) NULL COMMENT 'Mốc đã báo thêm Trưởng tầng, Quản lý viện (CFG-M07-10, BR-M07-29)';

-- 7. Lô gia đình gửi: kiểm tra tiếp nhận, loại hủy (người chứng kiến dùng cột witness có sẵn, thời điểm là closed_at)
ALTER TABLE family_medication_batch
    ADD COLUMN label_ok BOOLEAN NULL COMMENT 'Nhãn thuốc đạt; NULL ở lô cũ (BR-M07-30)',
    ADD COLUMN expiry_ok BOOLEAN NULL,
    ADD COLUMN packaging_ok BOOLEAN NULL,
    ADD COLUMN name_ok BOOLEAN NULL COMMENT 'Đúng tên người cao tuổi',
    ADD COLUMN dispose_kind ENUM ('FAMILY_REQUEST', 'EXPIRED') NULL COMMENT 'Đã hủy theo yêu cầu gia đình hoặc do hết hạn dùng';

-- (Cột procurement_request.submitted_at nằm ở V019_08, vì bảng này tạo ở V019_02, sau V006.)

-- 5. Hiệu quả lần dùng PRN: chỉ thêm dòng (dose_result chỉ INSERT)
CREATE TABLE prn_effectiveness
(
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dose_result_id   BIGINT        NOT NULL COMMENT 'Lần dùng PRN (dose_result.kind = PRN)',
    effectiveness    VARCHAR(2000) NOT NULL,
    recorded_by_id   BIGINT        NOT NULL,
    recorded_at      DATETIME(6)   NOT NULL,
    CONSTRAINT uq_prn_effectiveness_result UNIQUE (dose_result_id),
    CONSTRAINT fk_prn_effectiveness_result FOREIGN KEY (dose_result_id) REFERENCES dose_result (id) ON DELETE RESTRICT,
    CONSTRAINT chk_prn_effectiveness_text CHECK (CHAR_LENGTH(TRIM(effectiveness)) > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=RECORD';

DELIMITER $$
CREATE TRIGGER trg_prn_effectiveness_no_update
    BEFORE UPDATE
    ON prn_effectiveness
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'prn_effectiveness is append-only (BR-M07-04)';
END$$
CREATE TRIGGER trg_prn_effectiveness_no_delete
    BEFORE DELETE
    ON prn_effectiveness
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'prn_effectiveness is append-only (BR-M07-04)';
END$$
DELIMITER ;

-- 8. Điều dưỡng tầng khác phụ trách tạm khi tầng không có Điều dưỡng (BR-M07-31)
CREATE TABLE temporary_floor_nurse_assignment
(
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    floor_area_id   BIGINT      NOT NULL COMMENT 'Tầng/khu vực được phụ trách',
    shift_id        BIGINT      NOT NULL COMMENT 'Ca của tầng được phụ trách',
    nurse_account_id BIGINT     NOT NULL COMMENT 'Điều dưỡng được chỉ định (đang có ca ở tầng khác)',
    assigned_by_id  BIGINT      NOT NULL COMMENT 'Quản lý viện chỉ định',
    assigned_at     DATETIME(6) NOT NULL,
    expires_at      DATETIME(6) NOT NULL COMMENT 'Hết ca của tầng hoặc hết ca của chính Điều dưỡng, mốc nào sớm hơn',
    revoked_at      DATETIME(6) NULL,
    CONSTRAINT chk_temp_floor_nurse_period CHECK (expires_at > assigned_at),
    KEY idx_temp_floor_nurse_floor (floor_area_id, expires_at),
    KEY idx_temp_floor_nurse_nurse (nurse_account_id, expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=BUSINESS';

GRANT INSERT ON prn_effectiveness TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON temporary_floor_nurse_assignment TO 'eldercare_app'@'%';
