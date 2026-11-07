-- 019 T017 (data-model mục 0, 0.1, 4; FR-011 → FR-016, DBR-32; glossary 25.7). Cú pháp dùng được ở MySQL 8.0 và 8.4.
-- Theo R-15: loại tài sản ở catalog_item (M03.ASSET_TYPE), lịch sử vị trí và bộ phận ở status_history (M03.ASSET, thuộc
-- tính LOCATION, DEPARTMENT), lần bảo trì và hư hỏng ở record_event (M03.ASSET_MAINTENANCE, M03.ASSET_DAMAGE).
-- Tiền DECIMAL(15,0) theo quy ước 000 (audit F-07).

-- Hai loại hệ thống (chủ dự án duyệt 2026-11-06): api-019 không có nhóm loại, nên Giường (Q-238, Q-242) và Xe (biển số,
-- hạn đăng kiểm, bảo hiểm; lịch xe) nhận bằng mã; không ngừng hiệu lực được, đổi tên được.
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes, version)
VALUES ('M03.ASSET_TYPE', 'M03', 'BED', 'Giường', 1, 'ACTIVE', JSON_OBJECT('system', TRUE), 0),
       ('M03.ASSET_TYPE', 'M03', 'VEHICLE', 'Xe', 2, 'ACTIVE', JSON_OBJECT('system', TRUE), 0);

-- Nhóm 1 (thông tin mô tả) và nhóm 2 (trạng thái, vị trí, bộ phận chỉ đổi qua lệnh). floor_area_id là tầng của vị trí
-- hiện hành (phòng, tầng), để kiểm phạm vi Trưởng tầng (Q-313); khu vực, bãi xe, kho tài sản không thuộc tầng nào.
CREATE TABLE asset
(
    id                         BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code                       VARCHAR(40)    NOT NULL,
    type_id                    BIGINT         NOT NULL,
    name                       VARCHAR(200)   NOT NULL,
    department_id              BIGINT         NOT NULL,
    source                     ENUM ('PURCHASED', 'DONATED', 'TRANSFERRED_IN', 'BED') NOT NULL,
    source_description         VARCHAR(2000)  NULL,
    procurement_request_id     BIGINT         NULL,
    acquisition_cost           DECIMAL(15, 0) NULL,
    in_service_on              DATE           NULL,
    maintenance_interval_days  INT            NULL,
    maintenance_due_on         DATE           NULL,
    license_plate              VARCHAR(20)    NULL,
    seat_count                 SMALLINT       NULL,
    vehicle_inspection_due_on  DATE           NULL,
    insurance_due_on           DATE           NULL,
    bed_id                     BIGINT         NULL,
    status                     ENUM ('READY', 'IN_USE', 'DAMAGED', 'UNDER_MAINTENANCE', 'DISPOSED') NOT NULL DEFAULT 'READY',
    location_type              ENUM ('AREA', 'FLOOR', 'ROOM', 'PARKING_LOT', 'ASSET_STORAGE') NOT NULL,
    location_id                BIGINT         NULL,
    floor_area_id              BIGINT         NULL,
    maintenance_overdue        BOOLEAN        NOT NULL DEFAULT FALSE,
    vehicle_inspection_overdue BOOLEAN        NOT NULL DEFAULT FALSE,
    insurance_overdue          BOOLEAN        NOT NULL DEFAULT FALSE,
    awaiting_resident_transfer BOOLEAN        NOT NULL DEFAULT FALSE,
    created_by_staff_id        BIGINT         NULL,
    created_at                 DATETIME(6)    NOT NULL,
    version                    INT            NOT NULL DEFAULT 0,
    CONSTRAINT uq_asset_code UNIQUE (code),
    CONSTRAINT uq_asset_bed UNIQUE (bed_id),
    CONSTRAINT fk_asset_type FOREIGN KEY (type_id) REFERENCES catalog_item (id) ON DELETE RESTRICT,
    -- FR-008c, Q-294: mua mới gắn yêu cầu mua sắm và có nguyên giá; tặng, chuyển ghi nguồn
    CONSTRAINT chk_asset_purchased CHECK (source <> 'PURCHASED'
        OR (procurement_request_id IS NOT NULL AND acquisition_cost IS NOT NULL)),
    CONSTRAINT chk_asset_cost_with_request CHECK (procurement_request_id IS NULL OR acquisition_cost IS NOT NULL),
    CONSTRAINT chk_asset_source_described CHECK (source NOT IN ('DONATED', 'TRANSFERRED_IN')
        OR CHAR_LENGTH(TRIM(source_description)) > 0),
    CONSTRAINT chk_asset_bed CHECK ((source = 'BED') = (bed_id IS NOT NULL)),
    CONSTRAINT chk_asset_location CHECK (location_type IN ('PARKING_LOT', 'ASSET_STORAGE') OR location_id IS NOT NULL),
    CONSTRAINT chk_asset_amounts CHECK (acquisition_cost IS NULL OR acquisition_cost >= 0),
    KEY ix_asset_department (department_id, status),
    KEY ix_asset_floor (floor_area_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=BUSINESS';

DELIMITER $$

-- FR-013: Đã thanh lý là trạng thái cuối; nguồn và yêu cầu mua sắm đã gắn không đổi (DBR-36).
CREATE TRIGGER trg_asset_final
    BEFORE UPDATE
    ON asset
    FOR EACH ROW
BEGIN
    IF OLD.status = 'DISPOSED' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'disposed asset cannot change (FR-013)';
    END IF;
    IF NOT (NEW.source <=> OLD.source AND NEW.procurement_request_id <=> OLD.procurement_request_id
        AND NEW.bed_id <=> OLD.bed_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'asset source, procurement request and bed are set once (DBR-32, DBR-36)';
    END IF;
END$$

DELIMITER ;

GRANT INSERT, UPDATE ON asset TO 'eldercare_app'@'%';
GRANT DELETE ON asset TO 'eldercare_retention'@'%';
