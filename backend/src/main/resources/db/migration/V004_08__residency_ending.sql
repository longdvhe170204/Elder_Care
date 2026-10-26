-- 004 Phase 9 (US7): hồ sơ kết thúc lưu trú, bàn giao người cao tuổi, ngoại lệ điều kiện kết thúc (data-model mục 9; FR-062 →
-- FR-068, BR-M02-19). Tên theo glossary 10.6: ho_so_ket_thuc = residency_ending, ban_giao_nct = resident_handover,
-- ngoai_le_dieu_kien = ending_condition_exception. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

-- Hồ sơ kết thúc lưu trú (nhóm 2). Một người chỉ có một hồ sơ Đang chuẩn bị: open_resident_id có giá trị khi hồ sơ đang chuẩn bị
-- nên UNIQUE chặn hai hồ sơ mở (FR-062). Bác sĩ chỉ lập Xuất viện, Chuyển cơ sở vì lý do y tế: backend kiểm theo vai trò người
-- lập. Hồ sơ đã Hoàn tất hoặc Đã hủy không sửa nữa (FR-067).
CREATE TABLE residency_ending
(
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id       BIGINT        NOT NULL,
    end_case          ENUM ('CONTRACT_EXPIRED', 'DISCHARGE', 'TRANSFER_TO_ANOTHER_FACILITY', 'CONTRACT_TERMINATION') NOT NULL,
    medical_reason    BOOLEAN       NOT NULL DEFAULT FALSE,
    destination       VARCHAR(255)  NULL,
    expected_end_date DATE          NOT NULL,
    actual_ended_at   DATETIME(6)   NULL COMMENT 'Thời điểm hiệu lực của lệnh Kết thúc lưu trú (FR-065)',
    requested_by      VARCHAR(255)  NOT NULL,
    reason            VARCHAR(2000) NOT NULL,
    overdue_since     DATE          NULL COMMENT 'Dấu "quá ngày dự kiến" từ ngày liền sau ngày kết thúc dự kiến (FR-066a)',
    final_period_ref  BIGINT        NULL COMMENT 'Bảng kỳ cuối nháp do KyCuoiPort của 010 trả (FR-066)',
    status            ENUM ('PREPARING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PREPARING',
    cancel_reason     VARCHAR(2000) NULL,
    open_resident_id  BIGINT GENERATED ALWAYS AS (IF(status = 'PREPARING', resident_id, NULL)) STORED,
    version           INT           NOT NULL DEFAULT 0,
    created_by        BIGINT        NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    closed_by         BIGINT        NULL,
    closed_at         DATETIME(6)   NULL,
    CONSTRAINT uq_residency_ending_open_resident UNIQUE (open_resident_id),
    CONSTRAINT chk_residency_ending_destination CHECK (end_case <> 'TRANSFER_TO_ANOTHER_FACILITY' OR destination IS NOT NULL),
    CONSTRAINT chk_residency_ending_completed CHECK (status <> 'COMPLETED' OR actual_ended_at IS NOT NULL),
    CONSTRAINT chk_residency_ending_cancelled CHECK (status <> 'CANCELLED' OR cancel_reason IS NOT NULL),
    KEY ix_residency_ending_resident (resident_id, created_at),
    KEY ix_residency_ending_open (status, expected_end_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Bàn giao người cao tuổi (nhóm 3, điều kiện (e), FR-063): chỉ thêm; sai sót đi đường đính chính M02.RESIDENT_HANDOVER. Điều kiện
-- (e) Đạt khi có ít nhất một dòng chưa bị Hủy ghi nhận.
CREATE TABLE resident_handover
(
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ending_id         BIGINT       NOT NULL,
    recipient_type    ENUM ('FAMILY_MEMBER', 'RECEIVING_FACILITY') NOT NULL,
    family_member_id  BIGINT       NULL,
    facility_name     VARCHAR(255) NULL,
    handover_at       DATETIME(6)  NOT NULL,
    handover_staff_id BIGINT       NOT NULL,
    recorded_by       BIGINT       NOT NULL,
    recorded_at       DATETIME(6)  NOT NULL,
    CONSTRAINT chk_resident_handover_recipient CHECK (
        (recipient_type = 'FAMILY_MEMBER' AND family_member_id IS NOT NULL AND facility_name IS NULL)
            OR (recipient_type = 'RECEIVING_FACILITY' AND facility_name IS NOT NULL AND family_member_id IS NULL)),
    CONSTRAINT fk_resident_handover_ending FOREIGN KEY (ending_id) REFERENCES residency_ending (id) ON DELETE RESTRICT,
    KEY ix_resident_handover_ending (ending_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Ngoại lệ điều kiện kết thúc đã duyệt (nhóm 3, FR-064). Không có END_E_HANDOVER và END_H_SETTLEMENT_CONFIRMATION: hai điều
-- kiện đó không có ngoại lệ. ending_id rỗng được khi căn cứ là danh sách việc sau qua đời, chỉ cho END_H_SETTLEMENT (FR-071a,
-- Q-387). Mỗi yêu cầu phê duyệt cho đúng một dòng: dòng do apply của loại M02.ENDING_EXCEPTION thêm.
CREATE TABLE ending_condition_exception
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ending_id           BIGINT      NULL,
    resident_id         BIGINT      NOT NULL,
    condition_code      ENUM ('END_A_COSTS', 'END_B_BELONGINGS', 'END_C_STORED_MEDICATION', 'END_D_ALERTS_INCIDENTS',
        'END_H_SETTLEMENT') NOT NULL,
    approval_request_id BIGINT      NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    CONSTRAINT uq_ending_condition_exception_request UNIQUE (approval_request_id),
    CONSTRAINT chk_ending_condition_exception_basis CHECK (ending_id IS NOT NULL OR condition_code = 'END_H_SETTLEMENT'),
    CONSTRAINT fk_ending_condition_exception_ending FOREIGN KEY (ending_id) REFERENCES residency_ending (id) ON DELETE RESTRICT,
    KEY ix_ending_condition_exception_ending (ending_id),
    KEY ix_ending_condition_exception_resident (resident_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$

-- FR-067: hồ sơ đã Hoàn tất hoặc Đã hủy không bị sửa. Hồ sơ đang chuẩn bị được cập nhật (ngày dự kiến, dấu quá ngày, kỳ cuối,
-- chuyển trạng thái).
CREATE TRIGGER trg_residency_ending_immutable_when_closed
    BEFORE UPDATE
    ON residency_ending
    FOR EACH ROW
BEGIN
    IF OLD.status <> 'PREPARING' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a completed or cancelled residency ending cannot be changed (FR-067)';
    END IF;
END$$

CREATE TRIGGER trg_residency_ending_no_delete
    BEFORE DELETE
    ON residency_ending
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'residency_ending rows are removed only by the retention job';
    END IF;
END$$

CREATE TRIGGER trg_resident_handover_immutable
    BEFORE UPDATE
    ON resident_handover
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'resident_handover rows are append-only, use a correction (DBR-23)';
END$$

CREATE TRIGGER trg_resident_handover_no_delete
    BEFORE DELETE
    ON resident_handover
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'resident_handover rows are removed only by the retention job';
    END IF;
END$$

CREATE TRIGGER trg_ending_condition_exception_immutable
    BEFORE UPDATE
    ON ending_condition_exception
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ending_condition_exception rows are append-only (FR-064)';
END$$

CREATE TRIGGER trg_ending_condition_exception_no_delete
    BEFORE DELETE
    ON ending_condition_exception
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ending_condition_exception rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON residency_ending TO 'eldercare_app'@'%';
GRANT INSERT ON resident_handover TO 'eldercare_app'@'%';
GRANT INSERT ON ending_condition_exception TO 'eldercare_app'@'%';
GRANT DELETE ON residency_ending TO 'eldercare_retention'@'%';
GRANT DELETE ON resident_handover TO 'eldercare_retention'@'%';
GRANT DELETE ON ending_condition_exception TO 'eldercare_retention'@'%';
