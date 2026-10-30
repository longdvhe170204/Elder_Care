-- 000 T108: bảy bảng dùng chung sau gộp (research R-15, data-model 000 mục 8, glossary 6.6; constitution XII 3.2.0).
-- Module đọc, ghi qua administration.shared.api (catalog_item, status_history, record_event, simple_record,
-- resident_module_state) và common.data (reminder_marker, technical_lock). Cột module là tiền tố của mã loại.
-- Module thêm cột sinh, chỉ mục ở file riêng V<NNN>_94__shared_<mô tả>.sql (R-15 mục 5).

-- Nhóm 1: mọi danh mục. Mã không trùng trong cùng loại và cùng cha; tên không trùng giữa các mục đang Hiệu lực
-- (BR-M15-06). Bảng khác trỏ tới id với ON DELETE RESTRICT (FR-002).
CREATE TABLE catalog_item
(
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    catalog_type VARCHAR(40)   NOT NULL,
    module       VARCHAR(8)    NOT NULL,
    code         VARCHAR(40)   NULL,
    code_key     VARCHAR(110) GENERATED ALWAYS AS
        (IF(code IS NULL, NULL, CONCAT(catalog_type, ':', IFNULL(parent_id, 0), ':', code))) STORED,
    name         VARCHAR(200)  NOT NULL,
    parent_id    BIGINT        NULL,
    sort_order   INT           NOT NULL DEFAULT 0,
    status       ENUM ('ACTIVE', 'INACTIVE') NOT NULL,
    active_name  VARCHAR(270) GENERATED ALWAYS AS
        (IF(status = 'ACTIVE', CONCAT(catalog_type, ':', IFNULL(parent_id, 0), ':', name), NULL)) STORED,
    attributes   JSON          NULL,
    confirmed_by BIGINT        NULL,
    confirmed_at DATETIME(6)   NULL,
    version      INT           NOT NULL DEFAULT 0,
    CONSTRAINT uq_catalog_item_code_key UNIQUE (code_key),
    CONSTRAINT uq_catalog_item_active_name UNIQUE (active_name),
    CONSTRAINT fk_catalog_item_parent FOREIGN KEY (parent_id) REFERENCES catalog_item (id) ON DELETE RESTRICT,
    CONSTRAINT chk_catalog_item_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    INDEX ix_catalog_item_type (catalog_type, parent_id, sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhóm 3, chỉ thêm: lịch sử trạng thái, lịch sử phụ trách. Khoảng (PERIOD) đặt ends_at một lần; mỗi thuộc tính của một
-- đối tượng có tối đa một khoảng mở (open_key). unique_key thay UNIQUE riêng của bảng lịch sử cũ.
CREATE TABLE status_history
(
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    entity_type   VARCHAR(40)   NOT NULL,
    entity_id     BIGINT        NOT NULL,
    attribute     VARCHAR(40)   NOT NULL,
    module        VARCHAR(8)    NOT NULL,
    kind          ENUM ('POINT', 'PERIOD') NOT NULL,
    value_before  VARCHAR(100)  NULL,
    value         VARCHAR(100)  NULL,
    value_id      BIGINT        NULL,
    starts_at     DATETIME(6)   NOT NULL,
    ends_at       DATETIME(6)   NULL,
    command       VARCHAR(40)   NULL,
    actor_id      BIGINT        NULL,
    by_system     BOOLEAN       NOT NULL,
    reason        VARCHAR(2000) NULL,
    basis_type    VARCHAR(40)   NULL,
    basis_id      BIGINT        NULL,
    details       JSON          NULL,
    floor_area_id BIGINT        NULL,
    unique_key    VARCHAR(200)  NULL,
    open_key      VARCHAR(110) GENERATED ALWAYS AS
        (IF(kind = 'PERIOD' AND ends_at IS NULL, CONCAT(entity_type, ':', entity_id, ':', attribute), NULL)) STORED,
    CONSTRAINT uq_status_history_unique_key UNIQUE (unique_key),
    CONSTRAINT uq_status_history_open_key UNIQUE (open_key),
    CONSTRAINT chk_status_history_ends CHECK (ends_at IS NULL OR (kind = 'PERIOD' AND ends_at >= starts_at)),
    INDEX ix_status_history_entity (entity_type, entity_id, attribute, starts_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhóm 3: bản ghi con ghi thêm dần. Dòng nháp (confirmed_at NULL) sửa được tới khi xác nhận (mục 1.4); đích của
-- correction theo record_type = kind.
CREATE TABLE record_event
(
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    entity_type    VARCHAR(40)  NOT NULL,
    entity_id      BIGINT       NOT NULL,
    module         VARCHAR(8)   NOT NULL,
    kind           VARCHAR(60)  NOT NULL,
    content        JSON         NULL,
    reply_to_id    BIGINT       NULL,
    actor_id       BIGINT       NULL,
    by_system      BOOLEAN      NOT NULL,
    occurred_at    DATETIME(6)  NOT NULL,
    device_at      DATETIME(6)  NULL,
    confirmed_at   DATETIME(6)  NULL,
    floor_area_id  BIGINT       NULL,
    unique_key     VARCHAR(200) NULL,
    CONSTRAINT uq_record_event_unique_key UNIQUE (unique_key),
    -- Trả lời nằm trong cùng chuỗi bản ghi; job loại bỏ xóa cả chuỗi cùng đợt (R-11)
    CONSTRAINT fk_record_event_reply FOREIGN KEY (reply_to_id) REFERENCES record_event (id) ON DELETE CASCADE,
    INDEX ix_record_event_entity (entity_type, entity_id, kind, occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhóm 2, khóa theo dòng: đối tượng nhỏ có máy trạng thái ở domain của module. locked_at đặt khi xác nhận, chốt,
-- đóng (hoặc khi thêm với dòng nhóm 3); dòng đã khóa không đổi được cột nào.
CREATE TABLE simple_record
(
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind           VARCHAR(60)  NOT NULL,
    module         VARCHAR(8)   NOT NULL,
    resident_id    BIGINT       NULL,
    entity_type    VARCHAR(40)  NULL,
    entity_id      BIGINT       NULL,
    status         VARCHAR(30)  NOT NULL,
    content        JSON         NULL,
    effective_from DATE         NULL,
    effective_to   DATE         NULL,
    unique_key     VARCHAR(200) NULL,
    locked_at      DATETIME(6)  NULL,
    created_at     DATETIME(6)  NOT NULL,
    created_by     BIGINT       NOT NULL,
    version        INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_simple_record_unique_key UNIQUE (unique_key),
    CONSTRAINT chk_simple_record_effective CHECK (effective_to IS NULL OR effective_from IS NULL
        OR effective_to >= effective_from),
    INDEX ix_simple_record_resident (kind, resident_id, status),
    INDEX ix_simple_record_entity (kind, entity_type, entity_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhóm 2 kỹ thuật: trạng thái theo người cao tuổi của từng module, thay các bảng *_nct.
CREATE TABLE resident_module_state
(
    resident_id BIGINT      NOT NULL,
    module      VARCHAR(8)  NOT NULL,
    data        JSON        NOT NULL,
    version     INT         NOT NULL DEFAULT 0,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (resident_id, module)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Kỹ thuật, chỉ thêm: dấu đã nhắc, đã cảnh báo, đã xử lý.
CREATE TABLE reminder_marker
(
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind        VARCHAR(60) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id   BIGINT      NOT NULL,
    marker      VARCHAR(80) NOT NULL,
    marked_at   DATETIME(6) NOT NULL,
    details     JSON        NULL,
    CONSTRAINT uq_reminder_marker UNIQUE (kind, entity_type, entity_id, marker)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Kỹ thuật: dòng chỉ để khóa (INSERT IGNORE rồi SELECT … FOR UPDATE).
CREATE TABLE technical_lock
(
    lock_key   VARCHAR(200) NOT NULL PRIMARY KEY,
    version    INT          NOT NULL DEFAULT 0,
    updated_at DATETIME(6)  NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

DELIMITER $$

-- Chỉ đặt ends_at của khoảng một lần; mọi cột khác giữ nguyên (DBR-23).
CREATE TRIGGER trg_status_history_no_update
    BEFORE UPDATE
    ON status_history
    FOR EACH ROW
BEGIN
    IF OLD.ends_at IS NOT NULL OR NEW.ends_at IS NULL
        OR NOT (OLD.entity_type <=> NEW.entity_type) OR NOT (OLD.entity_id <=> NEW.entity_id)
        OR NOT (OLD.attribute <=> NEW.attribute) OR NOT (OLD.module <=> NEW.module) OR NOT (OLD.kind <=> NEW.kind)
        OR NOT (OLD.value_before <=> NEW.value_before) OR NOT (OLD.value <=> NEW.value)
        OR NOT (OLD.value_id <=> NEW.value_id) OR NOT (OLD.starts_at <=> NEW.starts_at)
        OR NOT (OLD.command <=> NEW.command) OR NOT (OLD.actor_id <=> NEW.actor_id)
        OR NOT (OLD.by_system <=> NEW.by_system) OR NOT (OLD.reason <=> NEW.reason)
        OR NOT (OLD.basis_type <=> NEW.basis_type) OR NOT (OLD.basis_id <=> NEW.basis_id)
        OR NOT (OLD.details <=> NEW.details) OR NOT (OLD.floor_area_id <=> NEW.floor_area_id)
        OR NOT (OLD.unique_key <=> NEW.unique_key) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'status_history is append-only; ends_at is set once (DBR-23)';
    END IF;
END$$

CREATE TRIGGER trg_status_history_no_delete
    BEFORE DELETE
    ON status_history
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'status_history rows are removed only by the retention job (FR-051)';
    END IF;
END$$

-- Dòng đã xác nhận không đổi; dòng nháp chỉ đổi nội dung và thời điểm xác nhận (mục 1.4).
CREATE TRIGGER trg_record_event_no_update
    BEFORE UPDATE
    ON record_event
    FOR EACH ROW
BEGIN
    IF OLD.confirmed_at IS NOT NULL
        OR NOT (OLD.entity_type <=> NEW.entity_type) OR NOT (OLD.entity_id <=> NEW.entity_id)
        OR NOT (OLD.module <=> NEW.module) OR NOT (OLD.kind <=> NEW.kind)
        OR NOT (OLD.reply_to_id <=> NEW.reply_to_id) OR NOT (OLD.actor_id <=> NEW.actor_id)
        OR NOT (OLD.by_system <=> NEW.by_system) OR NOT (OLD.occurred_at <=> NEW.occurred_at)
        OR NOT (OLD.device_at <=> NEW.device_at) OR NOT (OLD.floor_area_id <=> NEW.floor_area_id)
        OR NOT (OLD.unique_key <=> NEW.unique_key) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'record_event is append-only once confirmed (DBR-23)';
    END IF;
END$$

CREATE TRIGGER trg_record_event_no_delete
    BEFORE DELETE
    ON record_event
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'record_event rows are removed only by the retention job (FR-051)';
    END IF;
END$$

-- Khóa theo dòng (constitution XII 3.2.0): dòng đã khóa không đổi cột nào.
CREATE TRIGGER trg_simple_record_row_lock
    BEFORE UPDATE
    ON simple_record
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'simple_record row is locked (constitution XII)';
    END IF;
END$$

CREATE TRIGGER trg_simple_record_no_delete
    BEFORE DELETE
    ON simple_record
    FOR EACH ROW
BEGIN
    IF OLD.locked_at IS NOT NULL AND SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'locked simple_record rows are removed only by the retention job';
    END IF;
END$$

DELIMITER ;

-- SELECT đã cấp ở mức schema (V000_00). Bảng nhóm 3 và bảng kỹ thuật: tài khoản ứng dụng không xóa (FR-051).
GRANT INSERT, UPDATE, DELETE ON catalog_item TO 'eldercare_app'@'%';
GRANT DELETE ON catalog_item TO 'eldercare_retention'@'%';
GRANT INSERT, UPDATE (ends_at) ON status_history TO 'eldercare_app'@'%';
GRANT DELETE ON status_history TO 'eldercare_retention'@'%';
GRANT INSERT, UPDATE ON record_event TO 'eldercare_app'@'%';
GRANT DELETE ON record_event TO 'eldercare_retention'@'%';
GRANT INSERT, UPDATE ON simple_record TO 'eldercare_app'@'%';
GRANT DELETE ON simple_record TO 'eldercare_retention'@'%';
GRANT INSERT, UPDATE ON resident_module_state TO 'eldercare_app'@'%';
GRANT DELETE ON resident_module_state TO 'eldercare_retention'@'%';
GRANT INSERT ON reminder_marker TO 'eldercare_app'@'%';
GRANT DELETE ON reminder_marker TO 'eldercare_retention'@'%';
GRANT INSERT, UPDATE ON technical_lock TO 'eldercare_app'@'%';
