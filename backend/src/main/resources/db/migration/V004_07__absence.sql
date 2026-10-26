-- 004 Phase 8 (US6): lượt vắng, ngày vắng, quyết định giữ giường, báo vắng và ngày nghỉ của bán trú (data-model mục 8; FR-048 →
-- FR-061, BR-M02-06, 07, 17, 18). Tên theo glossary 10.6: luot_vang = absence, lich_su_du_kien_tro_lai =
-- expected_return_history, ngay_vang = absence_day, quyet_dinh_giu_giuong = bed_hold_decision, bao_vang_ban_tru =
-- day_care_absence_notice, ngay_nghi_ban_tru = day_care_closure_day.

-- Lượt vắng (nhóm 2), bất biến khi đã đóng (FR-061): sai sót về thời điểm đi đường đính chính M02.ABSENCE. Thời điểm lưu theo UTC.
-- Bệnh viện không bắt buộc dự kiến trở lại (FR-048); mọi loại khác bắt buộc. open_resident_id có giá trị khi lượt đang mở nên
-- UNIQUE chặn hai lượt mở của một người.
CREATE TABLE absence
(
    id                    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id           BIGINT       NOT NULL,
    absence_type          ENUM ('HOME_VISIT', 'FAMILY_OUTING', 'SAME_DAY_MEDICAL_VISIT', 'OTHER', 'HOSPITAL') NOT NULL,
    reason                VARCHAR(2000) NULL,
    standard_reason       ENUM ('HOME_VISIT_END_OF_LIFE_WISH') NULL,
    departed_at           DATETIME(6)  NOT NULL,
    expected_return_at    DATETIME(6)  NULL,
    returned_at           DATETIME(6)  NULL,
    picked_up_by_id       BIGINT       NULL,
    pickup_record_id      BIGINT       NULL,
    transfer_unit         VARCHAR(255) NULL,
    handover_staff_id     BIGINT       NOT NULL,
    takes_medication      BOOLEAN      NOT NULL DEFAULT FALSE,
    initial_bed_hold      BOOLEAN      NOT NULL,
    threshold_notified_at DATETIME(6)  NULL,
    review_date           DATE         NULL,
    previous_absence_id   BIGINT       NULL,
    status                ENUM ('OPEN', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    open_resident_id      BIGINT GENERATED ALWAYS AS (IF(status = 'OPEN', resident_id, NULL)) STORED,
    created_by            BIGINT       NOT NULL,
    created_at            DATETIME(6)  NOT NULL,
    closed_by             BIGINT       NULL,
    CONSTRAINT uq_absence_open_resident UNIQUE (open_resident_id),
    CONSTRAINT chk_absence_expected_return CHECK (absence_type = 'HOSPITAL' OR expected_return_at IS NOT NULL),
    CONSTRAINT chk_absence_closed CHECK (status = 'OPEN' OR returned_at IS NOT NULL),
    CONSTRAINT chk_absence_returned_after_departure CHECK (returned_at IS NULL OR returned_at >= departed_at),
    CONSTRAINT fk_absence_previous FOREIGN KEY (previous_absence_id) REFERENCES absence (id) ON DELETE RESTRICT,
    KEY ix_absence_resident (resident_id, departed_at),
    KEY ix_absence_pickup_record (pickup_record_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Các mốc dự kiến trở lại đã đổi (nhóm 3, FR-049); lần đầu (lúc mở lượt) không ghi vì chưa có giá trị cũ.
CREATE TABLE expected_return_history
(
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    absence_id BIGINT        NOT NULL,
    old_value  DATETIME(6)   NULL,
    new_value  DATETIME(6)   NOT NULL,
    reason     VARCHAR(2000) NOT NULL,
    actor_id   BIGINT        NOT NULL,
    at         DATETIME(6)   NOT NULL,
    CONSTRAINT fk_expected_return_history_absence FOREIGN KEY (absence_id) REFERENCES absence (id) ON DELETE RESTRICT,
    KEY ix_expected_return_history_absence (absence_id, at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Quyết định giữ giường (nhóm 3, FR-055, FR-056, FR-061). HOLD_CONTINUE bắt buộc ngày xem xét lại; COEFFICIENT_ONLY dành cho
-- lượt đã đóng, chỉ đặt hệ số cho các ngày trong [applies_from, applies_to].
CREATE TABLE bed_hold_decision
(
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    absence_id   BIGINT        NOT NULL,
    threshold_date DATE        NOT NULL,
    choice       ENUM ('HOLD_CONTINUE', 'RELEASE', 'COEFFICIENT_ONLY') NOT NULL,
    coefficient  DECIMAL(5, 2) NOT NULL,
    review_date  DATE          NULL,
    applies_from DATE          NOT NULL,
    applies_to   DATE          NULL,
    reason       VARCHAR(2000) NOT NULL,
    decided_by   BIGINT        NOT NULL,
    decided_at   DATETIME(6)   NOT NULL,
    CONSTRAINT chk_bed_hold_decision_review CHECK (choice <> 'HOLD_CONTINUE' OR review_date IS NOT NULL),
    CONSTRAINT chk_bed_hold_decision_coefficient CHECK (coefficient BETWEEN 0 AND 100),
    CONSTRAINT fk_bed_hold_decision_absence FOREIGN KEY (absence_id) REFERENCES absence (id) ON DELETE RESTRICT,
    KEY ix_bed_hold_decision_absence (absence_id, applies_from)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Kết quả tính theo ngày (không thuộc ba nhóm của 1.5; Complexity Tracking của plan): job NGAY_VANG ghi mỗi ngày một dòng với
-- bảng hiện hành lúc tính; chỉ ghi lại khi có quyết định giữ giường hoặc đính chính (FR-052, FR-058). hold_policy là giá trị giữ giường của dòng chính sách đã áp (ngưỡng giữ giường là ngày đầu tiên khác YES, FR-055); hold_bed là giường có
-- đang được giữ vào ngày đó hay không; dong_chinh_sach giữ dòng đã áp (JSON) để giải trình.
CREATE TABLE absence_day
(
    absence_id           BIGINT        NOT NULL,
    `date`               DATE          NOT NULL,
    sequence_no          INT           NOT NULL,
    coefficient          DECIMAL(5, 2) NOT NULL,
    hold_bed             BOOLEAN       NOT NULL,
    hold_policy          ENUM ('YES', 'NO', 'MANAGER_DECIDES') NOT NULL DEFAULT 'YES',
    policy_line_source   ENUM ('CONTRACT', 'FACILITY', 'DEFAULT') NOT NULL,
    policy_line          JSON          NULL,
    pending_decision_flag BOOLEAN      NOT NULL DEFAULT FALSE,
    no_policy_line_flag  BOOLEAN       NOT NULL DEFAULT FALSE,
    decision_id          BIGINT        NULL,
    calculated_at        DATETIME(6)   NOT NULL,
    parameter_at         DATETIME(6)   NOT NULL,
    PRIMARY KEY (absence_id, `date`),
    CONSTRAINT chk_absence_day_sequence CHECK (sequence_no >= 1),
    CONSTRAINT chk_absence_day_coefficient CHECK (coefficient BETWEEN 0 AND 100),
    CONSTRAINT fk_absence_day_absence FOREIGN KEY (absence_id) REFERENCES absence (id) ON DELETE RESTRICT,
    CONSTRAINT fk_absence_day_decision FOREIGN KEY (decision_id) REFERENCES bed_hold_decision (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Báo vắng của bán trú (nhóm 3, FR-060). session_id rỗng là cả ngày; buổi thuộc 005 nên chưa có khóa ngoại.
CREATE TABLE day_care_absence_notice
(
    id                   BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id          BIGINT      NOT NULL,
    `date`               DATE        NOT NULL,
    session_id           BIGINT      NULL,
    family_notified_at   DATETIME(6) NOT NULL,
    within_notice_period BOOLEAN     NOT NULL,
    recorded_by          BIGINT      NOT NULL,
    recorded_at          DATETIME(6) NOT NULL,
    KEY ix_day_care_absence_notice_resident (resident_id, `date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Ngày khu bán trú nghỉ (nhóm 2, FR-060a): mỗi ngày tối đa một khai báo Hiệu lực (active_date UNIQUE).
CREATE TABLE day_care_closure_day
(
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `date`           DATE          NOT NULL,
    reason           VARCHAR(2000) NOT NULL,
    status           ENUM ('ACTIVE', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    active_date      DATE GENERATED ALWAYS AS (IF(status = 'ACTIVE', `date`, NULL)) STORED,
    late_declaration BOOLEAN       NOT NULL DEFAULT FALSE,
    declared_by      BIGINT        NOT NULL,
    declared_at      DATETIME(6)   NOT NULL,
    cancelled_by     BIGINT        NULL,
    cancel_reason    VARCHAR(2000) NULL,
    cancelled_at     DATETIME(6)   NULL,
    CONSTRAINT uq_day_care_closure_day_active UNIQUE (active_date),
    KEY ix_day_care_closure_day_date (`date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$

-- FR-061: lượt vắng đã đóng không bị sửa. Lượt đang mở được cập nhật (mốc dự kiến, ngưỡng, ngày xem xét lại, lúc đóng).
CREATE TRIGGER trg_absence_immutable_when_closed
    BEFORE UPDATE
    ON absence
    FOR EACH ROW
BEGIN
    IF OLD.status = 'CLOSED' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a closed absence cannot be changed, use a correction (FR-061)';
    END IF;
END$$

CREATE TRIGGER trg_absence_no_delete
    BEFORE DELETE
    ON absence
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'absence rows are removed only by the retention job';
    END IF;
END$$

CREATE TRIGGER trg_expected_return_history_immutable
    BEFORE UPDATE
    ON expected_return_history
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'expected_return_history rows are append-only (FR-049)';
END$$

CREATE TRIGGER trg_expected_return_history_no_delete
    BEFORE DELETE
    ON expected_return_history
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'expected_return_history rows are removed only by the retention job';
    END IF;
END$$

CREATE TRIGGER trg_bed_hold_decision_immutable
    BEFORE UPDATE
    ON bed_hold_decision
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'bed_hold_decision rows are append-only (FR-055)';
END$$

CREATE TRIGGER trg_bed_hold_decision_no_delete
    BEFORE DELETE
    ON bed_hold_decision
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'bed_hold_decision rows are removed only by the retention job';
    END IF;
END$$

CREATE TRIGGER trg_day_care_absence_notice_immutable
    BEFORE UPDATE
    ON day_care_absence_notice
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'day_care_absence_notice rows are append-only (FR-060)';
END$$

CREATE TRIGGER trg_day_care_absence_notice_no_delete
    BEFORE DELETE
    ON day_care_absence_notice
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'day_care_absence_notice rows are removed only by the retention job';
    END IF;
END$$

CREATE TRIGGER trg_day_care_closure_day_no_delete
    BEFORE DELETE
    ON day_care_closure_day
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'day_care_closure_day rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON absence TO 'eldercare_app'@'%';
GRANT INSERT ON expected_return_history TO 'eldercare_app'@'%';
GRANT INSERT ON bed_hold_decision TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON absence_day TO 'eldercare_app'@'%';
GRANT INSERT ON day_care_absence_notice TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON day_care_closure_day TO 'eldercare_app'@'%';
GRANT DELETE ON absence TO 'eldercare_retention'@'%';
GRANT DELETE ON expected_return_history TO 'eldercare_retention'@'%';
GRANT DELETE ON bed_hold_decision TO 'eldercare_retention'@'%';
GRANT DELETE ON absence_day TO 'eldercare_retention'@'%';
GRANT DELETE ON day_care_absence_notice TO 'eldercare_retention'@'%';
GRANT DELETE ON day_care_closure_day TO 'eldercare_retention'@'%';
