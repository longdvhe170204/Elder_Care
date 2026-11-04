-- 001 T084 (research R-15 bản 2 của 000; data-model 001 mục 0, 0.1): gộp bảng đã cài của M01 vào bảng chung.
-- Nhóm 1 (phần này): bốn danh mục sang `catalog_item` (care_level, allergy_severity, information_source, care_need).
-- Dị nguyên (allergen) chưa chuyển: id dị nguyên được 006 lưu (prescription.active_ingredient_ids, medication.data,
-- medication_active_ingredient.allergen_id), chờ A và D duyệt cách cập nhật tham chiếu 006.
-- Mọi dòng chép sang bảng chung ghi `attributes.legacy` (bảng, id cũ). MySQL không hoàn tác DDL, nên bảng cũ chỉ bị
-- xóa sau khi thủ tục kiểm tra phía dưới qua hết các điều kiện (số dòng, ánh xạ id, tên, khóa ngoại, tham chiếu chéo).
-- Cú pháp dùng được ở MySQL 8.0 và 8.4.

SET @m01_health_item_count = (SELECT COUNT(*) FROM health_item);
SET @m01_assessment_care_need_count = (SELECT COUNT(*) FROM assessment_care_need);

-- Ánh xạ id cũ → id mới của catalog_item (không giả định id giống nhau); bỏ cuối tệp sau khi kiểm.
CREATE TABLE m01_catalog_id_map
(
    kind   VARCHAR(40) NOT NULL,
    old_id BIGINT      NOT NULL,
    new_id BIGINT      NOT NULL,
    PRIMARY KEY (kind, old_id),
    UNIQUE (new_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Mức chăm sóc: khóa theo mã (resident, care_level_history, assessment giữ cột mã, bỏ khóa ngoại).
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes, version)
SELECT 'M01.CARE_LEVEL',
       'M01',
       code,
       name,
       display_order,
       'ACTIVE',
       JSON_OBJECT('legacy', JSON_OBJECT('table', 'care_level', 'code', code)),
       0
FROM care_level
ORDER BY display_order;

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes, version)
SELECT 'M01.ALLERGY_SEVERITY',
       'M01',
       NULL,
       name,
       display_order,
       status,
       JSON_OBJECT('legacy', JSON_OBJECT('table', 'allergy_severity', 'id', id)),
       version
FROM allergy_severity
ORDER BY id;

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes, version)
SELECT 'M01.INFORMATION_SOURCE',
       'M01',
       NULL,
       name,
       id,
       status,
       JSON_OBJECT('legacy', JSON_OBJECT('table', 'information_source', 'id', id)),
       version
FROM information_source
ORDER BY id;

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes, version)
SELECT 'M01.CARE_NEED',
       'M01',
       NULL,
       name,
       id,
       status,
       JSON_OBJECT('legacy', JSON_OBJECT('table', 'care_need', 'id', id)),
       version
FROM care_need
ORDER BY id;

INSERT INTO m01_catalog_id_map (kind, old_id, new_id)
SELECT 'M01.ALLERGY_SEVERITY', s.id, c.id
FROM allergy_severity s
         JOIN catalog_item c ON c.catalog_type = 'M01.ALLERGY_SEVERITY'
    AND CAST(c.attributes ->> '$.legacy.id' AS UNSIGNED) = s.id;

INSERT INTO m01_catalog_id_map (kind, old_id, new_id)
SELECT 'M01.INFORMATION_SOURCE', s.id, c.id
FROM information_source s
         JOIN catalog_item c ON c.catalog_type = 'M01.INFORMATION_SOURCE'
    AND CAST(c.attributes ->> '$.legacy.id' AS UNSIGNED) = s.id;

INSERT INTO m01_catalog_id_map (kind, old_id, new_id)
SELECT 'M01.CARE_NEED', s.id, c.id
FROM care_need s
         JOIN catalog_item c ON c.catalog_type = 'M01.CARE_NEED'
    AND CAST(c.attributes ->> '$.legacy.id' AS UNSIGNED) = s.id;

-- Bỏ khóa ngoại tới bảng cũ (id mới không có trong bảng cũ); backend kiểm mã, id qua CatalogApi.
ALTER TABLE health_item
    DROP FOREIGN KEY fk_health_item_severity,
    DROP FOREIGN KEY fk_health_item_source;
ALTER TABLE assessment_care_need
    DROP FOREIGN KEY fk_assessment_care_need_need;
ALTER TABLE resident
    DROP FOREIGN KEY fk_resident_care_level;
ALTER TABLE care_level_history
    DROP FOREIGN KEY fk_care_level_history_level;
ALTER TABLE assessment
    DROP FOREIGN KEY fk_assessment_care_level;

-- health_item bất biến nội dung (DBR-04): gỡ trigger đúng lúc đổi id, dựng lại nguyên văn ngay sau đó.
DROP TRIGGER trg_health_item_immutable;

UPDATE health_item h
    JOIN m01_catalog_id_map m ON m.kind = 'M01.ALLERGY_SEVERITY' AND m.old_id = h.severity_id
SET h.severity_id = m.new_id;

UPDATE health_item h
    JOIN m01_catalog_id_map m ON m.kind = 'M01.INFORMATION_SOURCE' AND m.old_id = h.source_id
SET h.source_id = m.new_id;

DELIMITER $$
CREATE TRIGGER trg_health_item_immutable
    BEFORE UPDATE
    ON health_item
    FOR EACH ROW
BEGIN
    IF NOT (OLD.resident_id <=> NEW.resident_id AND OLD.type <=> NEW.type AND OLD.allergen_id <=> NEW.allergen_id
        AND OLD.free_text <=> NEW.free_text AND OLD.severity_id <=> NEW.severity_id
        AND OLD.source_id <=> NEW.source_id AND OLD.recorded_by <=> NEW.recorded_by
        AND OLD.recorded_at <=> NEW.recorded_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'health_item content is immutable (DBR-04)';
    END IF;
    IF OLD.status = 'EXCLUDED' AND NOT (NEW.status <=> 'EXCLUDED' AND OLD.excluded_at <=> NEW.excluded_at
        AND OLD.excluded_by <=> NEW.excluded_by AND OLD.exclusion_reason <=> NEW.exclusion_reason) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'an excluded health_item cannot be reinstated (DBR-04)';
    END IF;
END$$
DELIMITER ;

-- Khóa chính (assessment_id, care_need_id): đổi qua giá trị âm để id mới không đụng id cũ cùng lần đánh giá.
UPDATE assessment_care_need a
    JOIN m01_catalog_id_map m ON m.kind = 'M01.CARE_NEED' AND m.old_id = a.care_need_id
SET a.care_need_id = -m.new_id;
UPDATE assessment_care_need
SET care_need_id = -care_need_id
WHERE care_need_id < 0;

-- Kiểm trước khi xóa bảng cũ; lỗi thì dừng (SIGNAL), bảng cũ còn nguyên.
DELIMITER $$
CREATE PROCEDURE m01_verify_group1()
BEGIN
    DECLARE n BIGINT;

    SELECT COUNT(*) INTO n FROM catalog_item WHERE catalog_type = 'M01.CARE_LEVEL';
    IF n <> (SELECT COUNT(*) FROM care_level) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: care_level row count differs';
    END IF;
    SELECT COUNT(*) INTO n FROM m01_catalog_id_map WHERE kind = 'M01.ALLERGY_SEVERITY';
    IF n <> (SELECT COUNT(*) FROM allergy_severity)
        OR (SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M01.ALLERGY_SEVERITY') <> n THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: allergy_severity row count or mapping differs';
    END IF;
    SELECT COUNT(*) INTO n FROM m01_catalog_id_map WHERE kind = 'M01.INFORMATION_SOURCE';
    IF n <> (SELECT COUNT(*) FROM information_source)
        OR (SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M01.INFORMATION_SOURCE') <> n THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: information_source row count or mapping differs';
    END IF;
    SELECT COUNT(*) INTO n FROM m01_catalog_id_map WHERE kind = 'M01.CARE_NEED';
    IF n <> (SELECT COUNT(*) FROM care_need)
        OR (SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M01.CARE_NEED') <> n THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: care_need row count or mapping differs';
    END IF;

    -- Tên, trạng thái, thứ tự giữ nguyên theo từng cặp id cũ → mới.
    IF EXISTS (SELECT 1
               FROM m01_catalog_id_map m
                        JOIN allergy_severity o ON m.kind = 'M01.ALLERGY_SEVERITY' AND o.id = m.old_id
                        JOIN catalog_item c ON c.id = m.new_id
               WHERE NOT (c.name = o.name AND c.status = o.status AND c.sort_order = o.display_order))
        OR EXISTS (SELECT 1
                   FROM m01_catalog_id_map m
                            JOIN information_source o ON m.kind = 'M01.INFORMATION_SOURCE' AND o.id = m.old_id
                            JOIN catalog_item c ON c.id = m.new_id
                   WHERE NOT (c.name = o.name AND c.status = o.status))
        OR EXISTS (SELECT 1
                   FROM m01_catalog_id_map m
                            JOIN care_need o ON m.kind = 'M01.CARE_NEED' AND o.id = m.old_id
                            JOIN catalog_item c ON c.id = m.new_id
                   WHERE NOT (c.name = o.name AND c.status = o.status))
        OR EXISTS (SELECT 1
                   FROM care_level o
                            JOIN catalog_item c ON c.catalog_type = 'M01.CARE_LEVEL' AND c.code = o.code
                   WHERE NOT (c.name = o.name AND c.sort_order = o.display_order)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: catalog item content differs from the old row';
    END IF;

    -- Tham chiếu của 001 trỏ đúng loại.
    IF (SELECT COUNT(*) FROM health_item) <> @m01_health_item_count
        OR (SELECT COUNT(*) FROM assessment_care_need) <> @m01_assessment_care_need_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: health_item or assessment_care_need row count changed';
    END IF;
    IF EXISTS (SELECT 1
               FROM health_item h
               WHERE (h.severity_id IS NOT NULL AND NOT EXISTS (SELECT 1
                                                                  FROM catalog_item c
                                                                  WHERE c.id = h.severity_id
                                                                    AND c.catalog_type = 'M01.ALLERGY_SEVERITY'))
                  OR NOT EXISTS (SELECT 1
                                 FROM catalog_item c
                                 WHERE c.id = h.source_id AND c.catalog_type = 'M01.INFORMATION_SOURCE')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: health_item references an unmapped catalog id';
    END IF;
    IF EXISTS (SELECT 1
               FROM assessment_care_need a
               WHERE NOT EXISTS (SELECT 1
                                 FROM catalog_item c
                                 WHERE c.id = a.care_need_id AND c.catalog_type = 'M01.CARE_NEED')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: assessment_care_need references an unmapped catalog id';
    END IF;
    IF EXISTS (SELECT 1
               FROM resident r
               WHERE r.current_care_level IS NOT NULL
                 AND NOT EXISTS (SELECT 1
                                 FROM catalog_item c
                                 WHERE c.catalog_type = 'M01.CARE_LEVEL' AND c.code = r.current_care_level))
        OR EXISTS (SELECT 1
                   FROM care_level_history h
                   WHERE NOT EXISTS (SELECT 1
                                     FROM catalog_item c
                                     WHERE c.catalog_type = 'M01.CARE_LEVEL' AND c.code = h.care_level))
        OR EXISTS (SELECT 1
                   FROM assessment a
                   WHERE a.care_level IS NOT NULL
                     AND NOT EXISTS (SELECT 1
                                     FROM catalog_item c
                                     WHERE c.catalog_type = 'M01.CARE_LEVEL' AND c.code = a.care_level)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a care level code is missing from catalog_item';
    END IF;

    -- Không còn khóa ngoại nào (của mọi module) trỏ vào bốn bảng sắp xóa.
    IF EXISTS (SELECT 1
               FROM information_schema.KEY_COLUMN_USAGE
               WHERE TABLE_SCHEMA = DATABASE()
                 AND REFERENCED_TABLE_NAME IN ('care_level', 'allergy_severity', 'information_source', 'care_need')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a foreign key still references a table to be dropped';
    END IF;
END$$
DELIMITER ;

CALL m01_verify_group1();
DROP PROCEDURE m01_verify_group1;

DROP TABLE care_need;
DROP TABLE information_source;
DROP TABLE allergy_severity;
DROP TABLE care_level;
DROP TABLE m01_catalog_id_map;
