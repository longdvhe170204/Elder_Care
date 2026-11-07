-- 013 T003: đồ gửi và bản ghi bàn giao (data-model mục 0, 3, 4; FR-003, FR-004, FR-011, FR-012, DBR-22, BR-M12-01,
-- BR-M15-03; research K-03, K-04). Theo R-15 chỉ hai bảng này là bảng riêng của M12 (data-model mục 0 "Giữ"):
-- loại đồ, mức tình trạng, vị trí lưu giữ ở catalog_item (M12.BELONGING_TYPE, M12.CONDITION_LEVEL, M12.STORAGE_LOCATION);
-- khóa theo người cao tuổi (do_gui_nct) ở resident_module_state loại M12.BELONGING_RESIDENT; ảnh của bản ghi ở cột JSON;
-- lần trả, lần chặn trả thêm bằng cột kind khi làm US3 (T017, migration sau, tạo lại trigger cùng lúc).
-- Người cao tuổi, nhân viên, sự cố, tệp, yêu cầu phê duyệt là tham chiếu logic, không khóa ngoại sang module khác.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (R-01).

-- Đồ gửi, nhóm 2: trạng thái, người giữ, vị trí, mức tình trạng là cột suy ra từ bản ghi bàn giao hiện hành, cập nhật
-- cùng transaction với lệnh và đính chính (research K-04, SC-008). Đúng một trong quantity, cash_amount theo loại đồ.
CREATE TABLE belonging
(
    id                      BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id             BIGINT        NOT NULL,
    type_id                 BIGINT        NOT NULL,
    description             VARCHAR(1000) NOT NULL,
    quantity                DECIMAL(12, 2) NULL,
    cash_amount             DECIMAL(15, 0) NULL,
    unit                    VARCHAR(40)   NULL,
    original_belonging_id   BIGINT        NULL COMMENT 'Đồ gốc khi tách (FR-012)',
    intake_handover_id      BIGINT        NULL COMMENT 'Bản ghi Tiếp nhận gốc (DBR-22); đặt trong cùng lệnh tạo đồ',
    status                  ENUM ('HELD', 'IN_USE', 'RETURNED', 'LOST', 'DAMAGED', 'DISPOSED') NOT NULL,
    holder_type             ENUM ('STAFF', 'RESIDENT') NULL COMMENT 'Trống khi Thất lạc hoặc trạng thái cuối',
    holder_id               BIGINT        NULL,
    storage_location_id     BIGINT        NULL,
    condition_level_id      BIGINT        NOT NULL,
    current_handover_id     BIGINT        NULL,
    current_handover_at     DATETIME(6)   NULL,
    excluded                BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'Hủy ghi nhận Tiếp nhận không có bản ghi sau (FR-011a)',
    disposal_clock_start_at DATETIME(6)   NULL COMMENT 'Tìm thấy sau khi mục Hoàn thành hoặc hồ sơ Đã đóng (FR-026a)',
    last_reminded_at        DATETIME(6)   NULL COMMENT 'Nhắc CFG-M12-02 (FR-027)',
    version                 INT           NOT NULL DEFAULT 0,
    created_by              BIGINT        NOT NULL,
    created_at              DATETIME(6)   NOT NULL,
    CONSTRAINT fk_belonging_type FOREIGN KEY (type_id) REFERENCES catalog_item (id) ON DELETE RESTRICT,
    CONSTRAINT fk_belonging_condition_level FOREIGN KEY (condition_level_id) REFERENCES catalog_item (id) ON DELETE RESTRICT,
    CONSTRAINT fk_belonging_storage_location FOREIGN KEY (storage_location_id) REFERENCES catalog_item (id) ON DELETE RESTRICT,
    CONSTRAINT fk_belonging_original FOREIGN KEY (original_belonging_id) REFERENCES belonging (id) ON DELETE RESTRICT,
    CONSTRAINT ck_belonging_amount CHECK ((quantity IS NULL) <> (cash_amount IS NULL)),
    CONSTRAINT ck_belonging_amount_positive CHECK (quantity IS NULL OR quantity >= 0),
    CONSTRAINT ck_belonging_cash_positive CHECK (cash_amount IS NULL OR cash_amount >= 0),
    CONSTRAINT ck_belonging_holder CHECK ((holder_type IS NULL) = (holder_id IS NULL)),
    KEY ix_belonging_resident (resident_id, status),
    KEY ix_belonging_holder (holder_type, holder_id, status),
    KEY ix_belonging_location (storage_location_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=BUSINESS';

-- Bản ghi bàn giao, nhóm 3 (BR-M12-01, BR-M15-03): chỉ thêm; incident_id đặt một lần từ NULL (FR-021, FR-024);
-- sai sót xử lý bằng đính chính của 000 (M12.BELONGING_HANDOVER, research K-05). Ảnh là mảng mã tệp (FR-010).
CREATE TABLE belonging_handover
(
    id                        BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    belonging_id              BIGINT        NOT NULL,
    resident_id               BIGINT        NOT NULL,
    kind                      ENUM ('INTAKE', 'GIVE_FOR_USE', 'TAKE_BACK', 'TRANSFER_CUSTODY', 'RETURN', 'REPORT_LOST',
        'RECORD_DAMAGE', 'FOUND', 'DISPOSAL') NOT NULL,
    target_status             ENUM ('HELD', 'IN_USE', 'RETURNED', 'LOST', 'DAMAGED', 'DISPOSED') NOT NULL,
    giver_type                ENUM ('FAMILY_MEMBER', 'OTHER_PERSON', 'RESIDENT', 'STAFF') NULL COMMENT 'Trống với Tìm thấy',
    giver_id                  BIGINT        NULL,
    giver_name                VARCHAR(200)  NULL,
    giver_id_document         VARCHAR(100)  NULL,
    recipient_type            ENUM ('FAMILY_MEMBER', 'OTHER_PERSON', 'RESIDENT', 'STAFF') NULL COMMENT 'Trống với Báo thất lạc',
    recipient_id              BIGINT        NULL,
    recipient_name            VARCHAR(200)  NULL,
    occurred_at               DATETIME(6)   NOT NULL,
    quantity                  DECIMAL(12, 2) NULL,
    cash_amount               DECIMAL(15, 0) NULL,
    condition_level_id        BIGINT        NULL,
    condition_description     VARCHAR(2000) NULL,
    storage_location_id       BIGINT        NULL,
    reason                    VARCHAR(2000) NULL,
    holder_absent             BOOLEAN       NOT NULL DEFAULT FALSE,
    valuable                  BOOLEAN       NOT NULL COMMENT 'CFG-M12-01 lúc lệnh (FR-009)',
    photo_file_ids            JSON          NOT NULL,
    recipient_confirmation_id BIGINT        NULL,
    incident_id               BIGINT        NULL,
    approval_request_id       BIGINT        NULL,
    receiving_unit            VARCHAR(500)  NULL,
    inventory_check_id        BIGINT        NULL,
    note                      VARCHAR(2000) NULL,
    recorded_by               BIGINT        NOT NULL,
    recorded_at               DATETIME(6)   NOT NULL,
    CONSTRAINT fk_belonging_handover_belonging FOREIGN KEY (belonging_id) REFERENCES belonging (id) ON DELETE RESTRICT,
    CONSTRAINT fk_belonging_handover_condition_level FOREIGN KEY (condition_level_id) REFERENCES catalog_item (id) ON DELETE RESTRICT,
    CONSTRAINT fk_belonging_handover_storage_location FOREIGN KEY (storage_location_id) REFERENCES catalog_item (id) ON DELETE RESTRICT,
    CONSTRAINT ck_belonging_handover_amount CHECK ((quantity IS NULL) <> (cash_amount IS NULL)),
    CONSTRAINT ck_belonging_handover_photos CHECK (JSON_TYPE(photo_file_ids) = 'ARRAY'),
    KEY ix_belonging_handover_belonging (belonging_id, id),
    KEY ix_belonging_handover_resident (resident_id),
    KEY ix_belonging_handover_incident (incident_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=RECORD';

DELIMITER $$

CREATE TRIGGER trg_belonging_handover_no_update
    BEFORE UPDATE
    ON belonging_handover
    FOR EACH ROW
BEGIN
    IF OLD.incident_id IS NOT NULL OR NEW.incident_id IS NULL
        OR NOT (OLD.belonging_id <=> NEW.belonging_id) OR NOT (OLD.resident_id <=> NEW.resident_id)
        OR NOT (OLD.kind <=> NEW.kind) OR NOT (OLD.target_status <=> NEW.target_status)
        OR NOT (OLD.giver_type <=> NEW.giver_type) OR NOT (OLD.giver_id <=> NEW.giver_id)
        OR NOT (OLD.giver_name <=> NEW.giver_name) OR NOT (OLD.giver_id_document <=> NEW.giver_id_document)
        OR NOT (OLD.recipient_type <=> NEW.recipient_type) OR NOT (OLD.recipient_id <=> NEW.recipient_id)
        OR NOT (OLD.recipient_name <=> NEW.recipient_name) OR NOT (OLD.occurred_at <=> NEW.occurred_at)
        OR NOT (OLD.quantity <=> NEW.quantity) OR NOT (OLD.cash_amount <=> NEW.cash_amount)
        OR NOT (OLD.condition_level_id <=> NEW.condition_level_id)
        OR NOT (OLD.condition_description <=> NEW.condition_description)
        OR NOT (OLD.storage_location_id <=> NEW.storage_location_id) OR NOT (OLD.reason <=> NEW.reason)
        OR NOT (OLD.holder_absent <=> NEW.holder_absent) OR NOT (OLD.valuable <=> NEW.valuable)
        OR NOT (OLD.photo_file_ids <=> NEW.photo_file_ids)
        OR NOT (OLD.recipient_confirmation_id <=> NEW.recipient_confirmation_id)
        OR NOT (OLD.approval_request_id <=> NEW.approval_request_id)
        OR NOT (OLD.receiving_unit <=> NEW.receiving_unit)
        OR NOT (OLD.inventory_check_id <=> NEW.inventory_check_id) OR NOT (OLD.note <=> NEW.note)
        OR NOT (OLD.recorded_by <=> NEW.recorded_by) OR NOT (OLD.recorded_at <=> NEW.recorded_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'belonging_handover is append-only; only incident_id may be set once (BR-M12-01)';
    END IF;
END$$

CREATE TRIGGER trg_belonging_handover_no_delete
    BEFORE DELETE
    ON belonging_handover
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'belonging_handover rows are removed only by the retention job (BR-M15-03, FR-051)';
    END IF;
END$$

DELIMITER ;

-- Ứng dụng không xóa; UPDATE trên bản ghi bàn giao chỉ để đặt incident_id một lần (trigger chặn phần còn lại).
GRANT INSERT, UPDATE ON belonging TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON belonging_handover TO 'eldercare_app'@'%';
GRANT DELETE ON belonging TO 'eldercare_retention'@'%';
GRANT DELETE ON belonging_handover TO 'eldercare_retention'@'%';

-- Loại đồ tối thiểu của FR-001 (16.1, trừ thuốc gia đình gửi). Tên khớp CFG-M12-01 ("Điện thoại", "Trang sức",
-- "Tiền mặt") vì dấu đồ có giá trị đọc theo tên loại lúc lệnh (FR-009). Tiền mặt có số tiền; tiền mặt, trang sức mặc
-- định cần đồng ý khi giao sử dụng (FR-015a, Q-155).
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES ('M12.BELONGING_TYPE', 'M12', 'PHONE', 'Điện thoại', 1, 'ACTIVE',
        JSON_OBJECT('hasCashAmount', FALSE, 'requiresConsentForUse', FALSE)),
       ('M12.BELONGING_TYPE', 'M12', 'GLASSES', 'Kính', 2, 'ACTIVE',
        JSON_OBJECT('hasCashAmount', FALSE, 'requiresConsentForUse', FALSE)),
       ('M12.BELONGING_TYPE', 'M12', 'CLOTHING', 'Quần áo', 3, 'ACTIVE',
        JSON_OBJECT('hasCashAmount', FALSE, 'requiresConsentForUse', FALSE)),
       ('M12.BELONGING_TYPE', 'M12', 'WHEELCHAIR', 'Xe lăn', 4, 'ACTIVE',
        JSON_OBJECT('hasCashAmount', FALSE, 'requiresConsentForUse', FALSE)),
       ('M12.BELONGING_TYPE', 'M12', 'DOCUMENTS', 'Giấy tờ', 5, 'ACTIVE',
        JSON_OBJECT('hasCashAmount', FALSE, 'requiresConsentForUse', FALSE)),
       ('M12.BELONGING_TYPE', 'M12', 'PERSONAL_ITEMS', 'Đồ dùng cá nhân', 6, 'ACTIVE',
        JSON_OBJECT('hasCashAmount', FALSE, 'requiresConsentForUse', FALSE)),
       ('M12.BELONGING_TYPE', 'M12', 'JEWELRY', 'Trang sức', 7, 'ACTIVE',
        JSON_OBJECT('hasCashAmount', FALSE, 'requiresConsentForUse', TRUE)),
       ('M12.BELONGING_TYPE', 'M12', 'CASH', 'Tiền mặt', 8, 'ACTIVE',
        JSON_OBJECT('hasCashAmount', TRUE, 'requiresConsentForUse', TRUE)),
       ('M12.BELONGING_TYPE', 'M12', 'OTHER_ASSETS', 'Tài sản khác', 9, 'ACTIVE',
        JSON_OBJECT('hasCashAmount', FALSE, 'requiresConsentForUse', FALSE));

-- Bốn mức tình trạng mặc định từ tốt tới xấu (FR-001a, Q-156). Mức cuối "Không dùng được" có mã UNUSABLE: code_key
-- UNIQUE của catalog_item giữ "đúng một mức cuối" (data-model 0.1); mức thêm sau không có mã.
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES ('M12.CONDITION_LEVEL', 'M12', 'GOOD', 'Tốt', 1, 'ACTIVE', JSON_OBJECT('unusable', FALSE)),
       ('M12.CONDITION_LEVEL', 'M12', 'SIGNS_OF_USE', 'Có dấu hiệu sử dụng', 2, 'ACTIVE', JSON_OBJECT('unusable', FALSE)),
       ('M12.CONDITION_LEVEL', 'M12', 'PARTLY_DAMAGED', 'Hư hỏng một phần', 3, 'ACTIVE', JSON_OBJECT('unusable', FALSE)),
       ('M12.CONDITION_LEVEL', 'M12', 'UNUSABLE', 'Không dùng được', 4, 'ACTIVE', JSON_OBJECT('unusable', TRUE));
