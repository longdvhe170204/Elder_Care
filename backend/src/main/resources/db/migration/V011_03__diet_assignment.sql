-- 011 T013 (US1; data-model mục 4, 0; FR-010 → FR-017, BR-M08-03; research M-03, M-05): bản gán chế độ ăn, nhóm 2.
-- Theo R-15: bảng con ban_gan_han_che không tạo, danh sách dị nguyên ghi vào cột JSON personal_restrictions (ghi một lần
-- cùng bản gán); khóa theo người cao tuổi dinh_duong_nct ở resident_module_state loại M08.RESIDENT_NUTRITION.
-- Người cao tuổi, chế độ ăn (catalog_item), dị nguyên là tham chiếu logic, không khóa ngoại.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (R-01).
CREATE TABLE diet_assignment
(
    id                       BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id              BIGINT        NOT NULL,
    diet_id                  BIGINT        NOT NULL COMMENT 'catalog_item loại M08.DIET',
    texture                  ENUM ('REGULAR', 'SOFT', 'PUREED') NOT NULL,
    personal_restrictions    JSON          NOT NULL COMMENT 'Danh sách id dị nguyên (ban_gan_han_che đã gộp, R-15)',
    personal_instruction     VARCHAR(2000) NULL COMMENT 'Ghi chú nhu cầu dinh dưỡng (FR-010)',
    basis                    ENUM ('ASSESSMENT', 'REVIEW', 'OTHER') NOT NULL,
    basis_id                 BIGINT        NULL,
    review_notice_id         BIGINT        NULL COMMENT 'Thông báo xem lại (FR-059), Phase 10',
    requested_effective_from DATETIME(6)   NOT NULL COMMENT 'Thời điểm hiệu lực mong muốn (FR-011)',
    actual_effective_at      DATETIME(6)   NULL COMMENT 'Thời điểm hiệu lực thực tế (FR-017)',
    status                   ENUM ('PROPOSED', 'PENDING_EFFECTIVE', 'ACTIVE', 'DISCONTINUED', 'CANCELLED') NOT NULL,
    professional_reason      VARCHAR(2000) NULL COMMENT 'Bắt buộc khi thuộc FR-013 (a)–(d)',
    confirmed_warnings       JSON          NULL COMMENT 'Cảnh báo FR-016 đã xác nhận lúc áp dụng',
    applied_by               BIGINT        NULL,
    applied_at               DATETIME(6)   NULL,
    discontinued_at          DATETIME(6)   NULL,
    cancel_reason            VARCHAR(2000) NULL,
    cancelled_by             BIGINT        NULL COMMENT 'NULL khi hệ thống hủy theo trạng thái cuối (FR-015)',
    cancelled_at             DATETIME(6)   NULL,
    version                  INT           NOT NULL DEFAULT 0,
    created_by               BIGINT        NOT NULL,
    created_at               DATETIME(6)   NOT NULL,
    pending_key              BIGINT GENERATED ALWAYS AS (
        IF(status IN ('PROPOSED', 'PENDING_EFFECTIVE'), resident_id, NULL)) STORED
        COMMENT 'UNIQUE: tối đa một bản Đề xuất hoặc Chờ hiệu lực mỗi người (FR-012)',
    active_key               BIGINT GENERATED ALWAYS AS (IF(status = 'ACTIVE', resident_id, NULL)) STORED
        COMMENT 'UNIQUE: tối đa một bản Hiệu lực mỗi người (FR-011)',
    CONSTRAINT uq_diet_assignment_pending UNIQUE (pending_key),
    CONSTRAINT uq_diet_assignment_active UNIQUE (active_key),
    CONSTRAINT ck_diet_assignment_active CHECK (status NOT IN ('ACTIVE', 'DISCONTINUED') OR actual_effective_at IS NOT NULL),
    CONSTRAINT ck_diet_assignment_cancelled CHECK (status <> 'CANCELLED' OR cancel_reason IS NOT NULL),
    KEY ix_diet_assignment_resident (resident_id, id),
    KEY ix_diet_assignment_due (status, requested_effective_from),
    KEY ix_diet_assignment_diet (diet_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=BUSINESS';

-- Nội dung không đổi sau khi lập (FR-010, data-model mục 4); chỉ trạng thái và cột thời điểm. Lý do chuyên môn, cảnh
-- báo đã xác nhận đặt một lần lúc áp dụng. Ngừng, Đã hủy là trạng thái cuối.
DELIMITER $$
CREATE TRIGGER trg_diet_assignment_lock_content
    BEFORE UPDATE
    ON diet_assignment
    FOR EACH ROW
BEGIN
    IF NOT (NEW.resident_id <=> OLD.resident_id
        AND NEW.diet_id <=> OLD.diet_id
        AND NEW.texture <=> OLD.texture
        AND NEW.personal_restrictions <=> OLD.personal_restrictions
        AND NEW.personal_instruction <=> OLD.personal_instruction
        AND NEW.basis <=> OLD.basis
        AND NEW.basis_id <=> OLD.basis_id
        AND NEW.review_notice_id <=> OLD.review_notice_id
        AND NEW.requested_effective_from <=> OLD.requested_effective_from
        AND NEW.created_by <=> OLD.created_by
        AND NEW.created_at <=> OLD.created_at
        AND (OLD.professional_reason IS NULL OR NEW.professional_reason <=> OLD.professional_reason)
        AND (OLD.confirmed_warnings IS NULL OR NEW.confirmed_warnings <=> OLD.confirmed_warnings)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'diet_assignment content is immutable after it is proposed (FR-010)';
    END IF;
    IF OLD.status IN ('DISCONTINUED', 'CANCELLED') AND NEW.status <> OLD.status THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'diet_assignment is in a final status (FR-015)';
    END IF;
END$$
DELIMITER ;

-- Ứng dụng không xóa; job loại bỏ dữ liệu xóa theo hồ sơ (RetentionProvider, T007).
GRANT INSERT, UPDATE ON diet_assignment TO 'eldercare_app'@'%';
GRANT DELETE ON diet_assignment TO 'eldercare_retention'@'%';
