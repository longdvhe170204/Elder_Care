-- 011 T009 (R-15 mục 5; data-model 011 mục 0.1 dòng che_do_an): đúng một chế độ ăn mặc định Hiệu lực (FR-001, FR-014).
-- Thuộc tính của loại M08.DIET nằm trong catalog_item.attributes nên ràng buộc giữ bằng cột sinh có UNIQUE
-- (thay khoa_mac_dinh của bảng che_do_an đã gộp).
ALTER TABLE catalog_item
    ADD COLUMN m08_default_diet TINYINT GENERATED ALWAYS AS (
        IF(catalog_type = 'M08.DIET' AND status = 'ACTIVE' AND attributes ->> '$.isDefault' = 'true', 1, NULL)) STORED,
    ADD CONSTRAINT uq_catalog_item_m08_default_diet UNIQUE (m08_default_diet);
