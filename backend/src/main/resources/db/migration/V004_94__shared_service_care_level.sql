-- 004 T007, T110 (R-15 mục 5; data-model 0.1 dòng dich_vu): mỗi mức chăm sóc có tối đa một dịch vụ chăm sóc Hiệu lực (Q-297).
-- Thuộc tính của loại M02.SERVICE nằm trong catalog_item.attributes nên ràng buộc giữ bằng cột sinh có UNIQUE.
ALTER TABLE catalog_item
    ADD COLUMN m02_active_care_level VARCHAR(20) GENERATED ALWAYS AS (
        IF(catalog_type = 'M02.SERVICE' AND status = 'ACTIVE' AND attributes ->> '$.type' = 'CARE',
           attributes ->> '$.careLevelCode', NULL)) STORED,
    ADD CONSTRAINT uq_catalog_item_m02_care_level UNIQUE (m02_active_care_level);
