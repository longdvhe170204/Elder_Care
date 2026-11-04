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

-- Nhóm 3 (một phần): người thân trong phạm vi bản đồng ý vào cột JSON consent.family_member_ids (thay
-- consent_family_member, ghi cùng INSERT). Không sửa được: eldercare_app không có quyền UPDATE cột này, trigger
-- trg_consent_only_withdrawal chặn cả tài khoản quản trị (như trg_consent_family_member_no_update cũ).
SET @m01_consent_member_count = (SELECT COUNT(*) FROM consent_family_member);

ALTER TABLE consent
    ADD COLUMN family_member_ids JSON NULL COMMENT 'Người thân được xem sức khỏe (thay consent_family_member)';

UPDATE consent c
SET c.family_member_ids = COALESCE((SELECT JSON_ARRAYAGG(m.family_member_id)
                                    FROM consent_family_member m
                                    WHERE m.consent_id = c.id), JSON_ARRAY());

ALTER TABLE consent
    MODIFY COLUMN family_member_ids JSON NOT NULL COMMENT 'Người thân được xem sức khỏe (thay consent_family_member)',
    ADD INDEX ix_consent_family_member_ids ((CAST(family_member_ids AS UNSIGNED ARRAY)));

DROP TRIGGER trg_consent_only_withdrawal;

DELIMITER $$
CREATE TRIGGER trg_consent_only_withdrawal
    BEFORE UPDATE
    ON consent
    FOR EACH ROW
BEGIN
    IF NOT (NEW.resident_id <=> OLD.resident_id
        AND NEW.consenter_type <=> OLD.consenter_type
        AND NEW.consenter_family_member_id <=> OLD.consenter_family_member_id
        AND NEW.court_decision_number <=> OLD.court_decision_number
        AND NEW.court_decision_date <=> OLD.court_decision_date
        AND NEW.court_decision_court <=> OLD.court_decision_court
        AND NEW.court_decision_file_id <=> OLD.court_decision_file_id
        AND NEW.photo_consent <=> OLD.photo_consent
        AND NEW.notification_consent <=> OLD.notification_consent
        AND NEW.consented_at <=> OLD.consented_at
        AND NEW.evidence_file_id <=> OLD.evidence_file_id
        AND NEW.family_member_ids <=> OLD.family_member_ids
        AND NEW.created_at <=> OLD.created_at
        AND NEW.created_by <=> OLD.created_by) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'consent content is immutable (FR-016)';
    END IF;
    IF OLD.status = 'WITHDRAWN' THEN
        -- Bản được thay thế nhận tham chiếu tới bản mới sau khi bản mới được thêm (đúng một lần, từ NULL)
        IF NOT (NEW.status <=> OLD.status
            AND NEW.withdrawn_at <=> OLD.withdrawn_at
            AND NEW.withdrawn_by <=> OLD.withdrawn_by
            AND NEW.withdrawal_reason <=> OLD.withdrawal_reason
            AND NEW.withdrawal_evidence_file_id <=> OLD.withdrawal_evidence_file_id
            AND (NEW.superseded_by_id <=> OLD.superseded_by_id
                OR (OLD.superseded_by_id IS NULL AND OLD.withdrawal_reason IS NULL))) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'consent is already withdrawn and cannot change (FR-014)';
        END IF;
    END IF;
END$$

CREATE PROCEDURE m01_verify_consent_members()
BEGIN
    IF (SELECT COALESCE(SUM(JSON_LENGTH(family_member_ids)), 0) FROM consent) <> @m01_consent_member_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: consent family member count differs';
    END IF;
    IF EXISTS (SELECT 1
               FROM consent_family_member m
                        JOIN consent c ON c.id = m.consent_id
               WHERE NOT (m.family_member_id MEMBER OF (c.family_member_ids))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a consent family member was not copied';
    END IF;
END$$
DELIMITER ;

CALL m01_verify_consent_members();
DROP PROCEDURE m01_verify_consent_members;

DROP TABLE consent_family_member;

-- Nhóm 3 (một phần): nhu cầu chăm sóc của lần đánh giá vào cột JSON assessment.care_need_ids (thay
-- assessment_care_need). trg_assessment_no_update đã chặn sửa khi Đã xác nhận, Đã hủy nên chặn cả đổi nhu cầu; gỡ
-- trigger đúng lúc chép (lần đánh giá đã xác nhận cũng nhận cột mới) rồi dựng lại nguyên văn.
SET @m01_care_need_link_count = (SELECT COUNT(*) FROM assessment_care_need);

ALTER TABLE assessment
    ADD COLUMN care_need_ids JSON NULL COMMENT 'Nhu cầu chăm sóc được chọn (thay assessment_care_need)';

DROP TRIGGER trg_assessment_no_update;

UPDATE assessment a
SET a.care_need_ids = COALESCE((SELECT JSON_ARRAYAGG(n.care_need_id)
                                FROM assessment_care_need n
                                WHERE n.assessment_id = a.id), JSON_ARRAY());

ALTER TABLE assessment
    MODIFY COLUMN care_need_ids JSON NOT NULL DEFAULT (JSON_ARRAY())
        COMMENT 'Nhu cầu chăm sóc được chọn (thay assessment_care_need)';

DELIMITER $$
CREATE TRIGGER trg_assessment_no_update
    BEFORE UPDATE
    ON assessment
    FOR EACH ROW
BEGIN
    IF OLD.status IN ('CONFIRMED', 'DISCARDED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a confirmed or discarded assessment is not edited, only corrected (FR-032)';
    END IF;
END$$

CREATE PROCEDURE m01_verify_assessment_care_needs()
BEGIN
    IF (SELECT COALESCE(SUM(JSON_LENGTH(care_need_ids)), 0) FROM assessment) <> @m01_care_need_link_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: assessment care need count differs';
    END IF;
    IF EXISTS (SELECT 1
               FROM assessment_care_need n
                        JOIN assessment a ON a.id = n.assessment_id
               WHERE NOT (n.care_need_id MEMBER OF (a.care_need_ids))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: an assessment care need was not copied';
    END IF;
END$$
DELIMITER ;

CALL m01_verify_assessment_care_needs();
DROP PROCEDURE m01_verify_assessment_care_needs;

DROP TABLE assessment_care_need;

-- Nhóm 3 (một phần): nhắc định kỳ (periodic_reminder) và cảnh báo Tạm vắng quá hạn (return_overdue_alert) sang
-- reminder_marker (data-model 001 mục 0.1). Nhắc định kỳ: kind = M01.PERIODIC_REMINDER.<type>, đối tượng
-- M01.RESIDENT; marker START:<milestoneId> lúc bắt đầu (marked_at = started_at), <milestoneId>:<n> mỗi lần nhắc. Số lần,
-- lần nhắc cuối tính từ các dòng. Bảng cũ chỉ giữ lần nhắc cuối nên các lần nhắc 1 → n đều nhận last_reminded_at.
SET @m01_reminder_count = (SELECT COUNT(*) FROM periodic_reminder);
SET @m01_reminded_total = (SELECT COALESCE(SUM(reminder_count), 0) FROM periodic_reminder);
SET @m01_alert_count = (SELECT COUNT(*) FROM return_overdue_alert);

INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at, details)
SELECT CONCAT('M01.PERIODIC_REMINDER.', type),
       'M01.RESIDENT',
       resident_id,
       CONCAT('START:', milestone_id),
       started_at,
       JSON_OBJECT('legacy', JSON_OBJECT('table', 'periodic_reminder'))
FROM periodic_reminder;

SET SESSION cte_max_recursion_depth = 100000;

INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at, details)
WITH RECURSIVE n (i) AS (SELECT 1
                         UNION ALL
                         SELECT i + 1
                         FROM n
                         WHERE i < (SELECT COALESCE(MAX(reminder_count), 0) FROM periodic_reminder))
SELECT CONCAT('M01.PERIODIC_REMINDER.', r.type),
       'M01.RESIDENT',
       r.resident_id,
       CONCAT(r.milestone_id, ':', n.i),
       COALESCE(r.last_reminded_at, r.started_at),
       JSON_OBJECT('legacy', JSON_OBJECT('table', 'periodic_reminder'))
FROM periodic_reminder r
         JOIN n ON n.i <= r.reminder_count;

INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at, details)
SELECT 'M01.RETURN_OVERDUE_ALERT',
       'M02.ABSENCE',
       absence_id,
       DATE_FORMAT(expected_return_at, '%Y-%m-%dT%H:%i:%s.%f'),
       alerted_at,
       JSON_OBJECT('residentId', resident_id, 'legacy', JSON_OBJECT('table', 'return_overdue_alert'))
FROM return_overdue_alert;

DELIMITER $$
CREATE PROCEDURE m01_verify_reminders()
BEGIN
    IF (SELECT COUNT(*)
        FROM reminder_marker
        WHERE kind LIKE 'M01.PERIODIC_REMINDER.%' AND marker LIKE 'START:%'
          AND details ->> '$.legacy.table' = 'periodic_reminder') <> @m01_reminder_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: periodic reminder start count differs';
    END IF;
    IF (SELECT COUNT(*)
        FROM reminder_marker
        WHERE kind LIKE 'M01.PERIODIC_REMINDER.%' AND marker NOT LIKE 'START:%'
          AND details ->> '$.legacy.table' = 'periodic_reminder') <> @m01_reminded_total THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: periodic reminder count differs';
    END IF;
    IF EXISTS (SELECT 1
               FROM periodic_reminder r
               WHERE NOT EXISTS (SELECT 1
                                 FROM reminder_marker m
                                 WHERE m.kind = CONCAT('M01.PERIODIC_REMINDER.', r.type)
                                   AND m.entity_type = 'M01.RESIDENT'
                                   AND m.entity_id = r.resident_id
                                   AND m.marker = CONCAT('START:', r.milestone_id)
                                   AND m.marked_at = r.started_at)
                  OR (SELECT COUNT(*)
                      FROM reminder_marker m
                      WHERE m.kind = CONCAT('M01.PERIODIC_REMINDER.', r.type)
                        AND m.entity_type = 'M01.RESIDENT'
                        AND m.entity_id = r.resident_id
                        AND m.marker LIKE CONCAT(r.milestone_id, ':%')) <> r.reminder_count
                  OR (r.reminder_count > 0
                   AND (SELECT MAX(m.marked_at)
                        FROM reminder_marker m
                        WHERE m.kind = CONCAT('M01.PERIODIC_REMINDER.', r.type)
                          AND m.entity_type = 'M01.RESIDENT'
                          AND m.entity_id = r.resident_id
                          AND m.marker LIKE CONCAT(r.milestone_id, ':%')) <> COALESCE(r.last_reminded_at, r.started_at))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a periodic reminder was not copied';
    END IF;
    IF (SELECT COUNT(*) FROM reminder_marker WHERE kind = 'M01.RETURN_OVERDUE_ALERT') <> @m01_alert_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: return overdue alert count differs';
    END IF;
    IF EXISTS (SELECT 1
               FROM return_overdue_alert a
               WHERE NOT EXISTS (SELECT 1
                                 FROM reminder_marker m
                                 WHERE m.kind = 'M01.RETURN_OVERDUE_ALERT'
                                   AND m.entity_type = 'M02.ABSENCE'
                                   AND m.entity_id = a.absence_id
                                   AND m.marker = DATE_FORMAT(a.expected_return_at, '%Y-%m-%dT%H:%i:%s.%f')
                                   AND m.marked_at = a.alerted_at)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a return overdue alert was not copied';
    END IF;
END$$
DELIMITER ;

CALL m01_verify_reminders();
DROP PROCEDURE m01_verify_reminders;

DROP TABLE periodic_reminder;
DROP TABLE return_overdue_alert;

-- Nhóm 3 (một phần): hai bảng nguyện vọng vào wish (data-model 001 mục 0.1). Phiếu cuối đời giữ id (nhật ký, tệp và nhắc
-- xem lại trỏ theo id); phiếu điều trị nhận id mới, id cũ ghi ở legacy_id, tệp M01.TREATMENT_WISH đổi owner_id theo id
-- mới. Người cần báo của phiếu điều trị vào notify_family_member_ids; ý kiến khác vào record_event loại
-- M01.WISH_OPINION (đối tượng cha là M01.END_OF_LIFE_WISH hoặc M01.TREATMENT_WISH, đúng loại bản ghi của chuỗi lưu giữ),
-- ghi đã xác nhận (chỉ thêm).
SET @m01_eol_count = (SELECT COUNT(*) FROM end_of_life_wish);
SET @m01_treatment_count = (SELECT COUNT(*) FROM treatment_wish);
SET @m01_notify_count = (SELECT COUNT(*) FROM treatment_wish_notify);
SET @m01_eol_opinion_count = (SELECT COUNT(*) FROM end_of_life_wish_opinion);
SET @m01_treatment_opinion_count = (SELECT COUNT(*) FROM treatment_wish_opinion);
SET @m01_treatment_file_count = (SELECT COUNT(*) FROM file WHERE owner_type = 'M01.TREATMENT_WISH');

CREATE TABLE wish
(
    id                           BIGINT       NOT NULL AUTO_INCREMENT,
    kind                         ENUM ('END_OF_LIFE','TREATMENT') NOT NULL,
    legacy_id                    BIGINT       NULL COMMENT 'Id cũ trong treatment_wish (chỉ dòng chuyển); phiếu cuối đời giữ id',
    resident_id                  BIGINT       NOT NULL,
    note                         TEXT         NULL COMMENT 'Ý kiến khác ghi ở record_event M01.WISH_OPINION (FR-023g, BR-M01-14)',
    respondent_type              ENUM ('RESIDENT','PATIENT_REPRESENTATIVE','LEGAL_REPRESENTATIVE','FAMILY_MEMBER','OBSERVING_STAFF') NOT NULL,
    respondent_family_member_id  BIGINT       NULL COMMENT 'Hồ sơ người thân do 012 giữ nên không có khóa ngoại',
    signed_copy_file_id          BIGINT       NULL COMMENT 'Bắt buộc với END_OF_LIFE (FR-023a), không bắt buộc với TREATMENT',
    status                       ENUM ('ACTIVE','SUPERSEDED','EXPIRED') NOT NULL,
    superseded_by_id             BIGINT       NULL,
    recorded_by                  BIGINT       NOT NULL,
    recorded_at                  DATETIME(6)  NOT NULL,
    -- Riêng END_OF_LIFE (NULL với TREATMENT)
    choice                       ENUM ('TRANSFER_TO_HOSPITAL','RETURN_HOME','STAY_PALLIATIVE') NULL,
    first_contact                VARCHAR(500) NULL COMMENT 'Người cần liên hệ trước khi thực hiện',
    relationship                 VARCHAR(100) NULL,
    assessing_doctor_id          BIGINT       NULL COMMENT 'Mã tài khoản Bác sĩ (AC-03) lúc ghi',
    can_self_determine           BOOLEAN      NULL COMMENT 'Nhận định cho chính phiếu này, không phải dấu của người cao tuổi (FR-023h)',
    assessed_at                  DATETIME(6)  NULL,
    support_method               TEXT         NULL COMMENT 'Cách đã hỗ trợ người cao tuổi hiểu thông tin',
    review_date                  DATE         NULL COMMENT 'Ngày xem lại nhận định (FR-023i)',
    signer_type                  ENUM ('RESIDENT','PATIENT_REPRESENTATIVE','LEGAL_REPRESENTATIVE') NULL,
    signer_family_member_id      BIGINT       NULL,
    representative_selection     ENUM ('CHOSEN_BY_RESIDENT','CHOSEN_BY_FAMILY') NULL,
    court_decision_number        VARCHAR(100) NULL,
    court_decision_date          DATE         NULL,
    court_decision_court         VARCHAR(200) NULL,
    court_decision_file_id       BIGINT       NULL,
    signed_at                    DATETIME(6)  NULL,
    source                       ENUM ('ADMISSION','FAMILY_REQUEST','CRITICAL_RECONFIRMATION') NULL,
    -- Riêng TREATMENT (NULL với END_OF_LIFE)
    hospital1_name               VARCHAR(255) NULL,
    hospital1_address            VARCHAR(500) NULL,
    hospital2_name               VARCHAR(255) NULL,
    hospital2_address            VARCHAR(500) NULL,
    insurance_registration_place VARCHAR(255) NULL,
    insurance_card_number        VARCHAR(20)  NULL,
    insurance_card_expiry        DATE         NULL,
    ask_family_first             BOOLEAN      NULL COMMENT 'Hỏi gia đình trước khi chuyển viện không khẩn cấp',
    escort                       ENUM ('FAMILY','FACILITY_STAFF','CASE_BY_CASE') NULL,
    notify_family_member_ids     JSON         NULL COMMENT 'Người thân cần báo (thay treatment_wish_notify), ghi cùng INSERT',
    active_key                   VARCHAR(40) GENERATED ALWAYS AS (IF(status = 'ACTIVE', CONCAT(kind, ':', resident_id), NULL)) STORED COMMENT 'UNIQUE: tối đa một phiên bản Hiệu lực mỗi loại mỗi người (DBR-34, DBR-39)',
    PRIMARY KEY (id),
    UNIQUE KEY uq_wish_active (active_key),
    KEY ix_wish_resident (resident_id, kind, id),
    KEY ix_wish_review (kind, status, review_date),
    KEY ix_wish_notify ((CAST(notify_family_member_ids AS UNSIGNED ARRAY))),
    CONSTRAINT fk_wish_resident FOREIGN KEY (resident_id) REFERENCES resident (id) ON DELETE RESTRICT,
    CONSTRAINT fk_wish_signed_copy FOREIGN KEY (signed_copy_file_id) REFERENCES file (id) ON DELETE RESTRICT,
    CONSTRAINT fk_wish_court_file FOREIGN KEY (court_decision_file_id) REFERENCES file (id) ON DELETE RESTRICT,
    CONSTRAINT fk_wish_superseded_by FOREIGN KEY (superseded_by_id) REFERENCES wish (id) ON DELETE RESTRICT,
    -- Cột bắt buộc và cột của loại kia là NULL
    CONSTRAINT ck_wish_kind_columns CHECK (
        (kind = 'END_OF_LIFE'
            AND status IN ('ACTIVE', 'SUPERSEDED')
            AND respondent_type IN ('RESIDENT', 'PATIENT_REPRESENTATIVE', 'LEGAL_REPRESENTATIVE')
            AND choice IS NOT NULL AND first_contact IS NOT NULL AND assessing_doctor_id IS NOT NULL
            AND can_self_determine IS NOT NULL AND assessed_at IS NOT NULL AND support_method IS NOT NULL
            AND review_date IS NOT NULL AND signer_type IS NOT NULL AND signed_at IS NOT NULL
            AND signed_copy_file_id IS NOT NULL AND source IS NOT NULL
            AND hospital1_name IS NULL AND hospital1_address IS NULL AND hospital2_name IS NULL
            AND hospital2_address IS NULL AND insurance_registration_place IS NULL AND insurance_card_number IS NULL
            AND insurance_card_expiry IS NULL AND ask_family_first IS NULL AND escort IS NULL
            AND notify_family_member_ids IS NULL)
            OR (kind = 'TREATMENT'
            AND status IN ('ACTIVE', 'EXPIRED')
            AND respondent_type IN ('RESIDENT', 'FAMILY_MEMBER', 'OBSERVING_STAFF')
            AND hospital1_name IS NOT NULL AND hospital1_address IS NOT NULL AND ask_family_first IS NOT NULL
            AND escort IS NOT NULL
            AND choice IS NULL AND first_contact IS NULL AND relationship IS NULL AND assessing_doctor_id IS NULL
            AND can_self_determine IS NULL AND assessed_at IS NULL AND support_method IS NULL AND review_date IS NULL
            AND signer_type IS NULL AND signer_family_member_id IS NULL AND representative_selection IS NULL
            AND court_decision_number IS NULL AND court_decision_date IS NULL AND court_decision_court IS NULL
            AND court_decision_file_id IS NULL AND signed_at IS NULL AND source IS NULL)),
    -- FR-023c: người cao tuổi ký khi và chỉ khi Bác sĩ nhận định tự quyết được
    CONSTRAINT ck_wish_signer CHECK (kind <> 'END_OF_LIFE' OR can_self_determine = (signer_type = 'RESIDENT')),
    CONSTRAINT ck_wish_signer_member CHECK (
        kind <> 'END_OF_LIFE' OR (signer_type = 'RESIDENT') = (signer_family_member_id IS NULL)),
    CONSTRAINT ck_wish_eol_respondent CHECK (
        kind <> 'END_OF_LIFE'
            OR (respondent_type = 'RESIDENT' AND respondent_family_member_id IS NULL AND relationship IS NULL)
            OR (respondent_type <> 'RESIDENT' AND respondent_family_member_id IS NOT NULL AND relationship IS NOT NULL)),
    CONSTRAINT ck_wish_selection CHECK (
        kind <> 'END_OF_LIFE' OR (signer_type = 'PATIENT_REPRESENTATIVE') = (representative_selection IS NOT NULL)),
    -- FR-011: người đại diện theo pháp luật ký thì có đủ bốn thông tin quyết định của Tòa án; loại khác thì không có cột nào
    CONSTRAINT ck_wish_court CHECK (
        kind <> 'END_OF_LIFE'
            OR (signer_type = 'LEGAL_REPRESENTATIVE'
            AND court_decision_number IS NOT NULL AND court_decision_date IS NOT NULL
            AND court_decision_court IS NOT NULL AND court_decision_file_id IS NOT NULL)
            OR (signer_type <> 'LEGAL_REPRESENTATIVE'
            AND court_decision_number IS NULL AND court_decision_date IS NULL
            AND court_decision_court IS NULL AND court_decision_file_id IS NULL)),
    CONSTRAINT ck_wish_assessment CHECK (
        kind <> 'END_OF_LIFE'
            OR (CHAR_LENGTH(TRIM(support_method)) > 0 AND review_date >= DATE(assessed_at))),
    CONSTRAINT ck_wish_hospital1 CHECK (
        kind <> 'TREATMENT'
            OR (CHAR_LENGTH(TRIM(hospital1_name)) > 0 AND CHAR_LENGTH(TRIM(hospital1_address)) > 0)),
    -- FR-023j: bệnh viện thứ hai đủ cả tên và địa chỉ hoặc không có cả hai
    CONSTRAINT ck_wish_hospital2 CHECK (
        kind <> 'TREATMENT'
            OR (hospital2_name IS NULL AND hospital2_address IS NULL)
            OR (CHAR_LENGTH(TRIM(hospital2_name)) > 0 AND CHAR_LENGTH(TRIM(hospital2_address)) > 0)),
    CONSTRAINT ck_wish_treatment_respondent CHECK (
        kind <> 'TREATMENT' OR (respondent_type = 'FAMILY_MEMBER') = (respondent_family_member_id IS NOT NULL)),
    CONSTRAINT ck_wish_status CHECK (status IN ('SUPERSEDED', 'EXPIRED') OR superseded_by_id IS NULL)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'DataGroup=BUSINESS';

INSERT INTO wish (id, kind, resident_id, note, respondent_type, respondent_family_member_id, signed_copy_file_id, status,
                  recorded_by, recorded_at, choice, first_contact, relationship, assessing_doctor_id, can_self_determine,
                  assessed_at, support_method, review_date, signer_type, signer_family_member_id,
                  representative_selection, court_decision_number, court_decision_date, court_decision_court,
                  court_decision_file_id, signed_at, source)
SELECT id, 'END_OF_LIFE', resident_id, note, respondent_type, respondent_family_member_id, signed_copy_file_id, status,
       recorded_by, recorded_at, choice, first_contact, relationship, assessing_doctor_id, can_self_determine,
       assessed_at, support_method, review_date, signer_type, signer_family_member_id, representative_selection,
       court_decision_number, court_decision_date, court_decision_court, court_decision_file_id, signed_at, source
FROM end_of_life_wish
ORDER BY id;

UPDATE wish w
    JOIN end_of_life_wish o ON o.id = w.id
SET w.superseded_by_id = o.superseded_by_id
WHERE o.superseded_by_id IS NOT NULL;

INSERT INTO wish (kind, legacy_id, resident_id, note, respondent_type, respondent_family_member_id, signed_copy_file_id,
                  status, recorded_by, recorded_at, hospital1_name, hospital1_address, hospital2_name, hospital2_address,
                  insurance_registration_place, insurance_card_number, insurance_card_expiry, ask_family_first, escort)
SELECT 'TREATMENT', id, resident_id, note, respondent_type, respondent_family_member_id, signed_copy_file_id,
       status, recorded_by, recorded_at, hospital1_name, hospital1_address, hospital2_name, hospital2_address,
       insurance_registration_place, insurance_card_number, insurance_card_expiry, ask_family_first, escort
FROM treatment_wish
ORDER BY id;

UPDATE wish w
    JOIN treatment_wish t ON t.id = w.legacy_id
    JOIN wish newer ON newer.kind = 'TREATMENT' AND newer.legacy_id = t.superseded_by_id
SET w.superseded_by_id = newer.id
WHERE w.kind = 'TREATMENT';

UPDATE wish w
SET w.notify_family_member_ids = COALESCE((SELECT JSON_ARRAYAGG(n.family_member_id)
                                           FROM treatment_wish_notify n
                                           WHERE n.treatment_wish_id = w.legacy_id), JSON_ARRAY())
WHERE w.kind = 'TREATMENT';

INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system, occurred_at, confirmed_at)
SELECT 'M01.END_OF_LIFE_WISH',
       o.wish_id,
       'M01',
       'M01.WISH_OPINION',
       JSON_OBJECT('sourceType', o.source_type, 'sourceFamilyMemberId', o.source_family_member_id,
                   'content', o.content, 'legacy', JSON_OBJECT('table', 'end_of_life_wish_opinion', 'id', o.id)),
       o.recorded_by,
       FALSE,
       o.recorded_at,
       o.recorded_at
FROM end_of_life_wish_opinion o
ORDER BY o.id;

INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system, occurred_at, confirmed_at)
SELECT 'M01.TREATMENT_WISH',
       w.id,
       'M01',
       'M01.WISH_OPINION',
       JSON_OBJECT('sourceType', o.source_type, 'sourceFamilyMemberId', o.source_family_member_id,
                   'content', o.content, 'legacy', JSON_OBJECT('table', 'treatment_wish_opinion', 'id', o.id)),
       o.recorded_by,
       FALSE,
       o.recorded_at,
       o.recorded_at
FROM treatment_wish_opinion o
         JOIN wish w ON w.kind = 'TREATMENT' AND w.legacy_id = o.treatment_wish_id
ORDER BY o.id;

-- Tệp đã gắn không sửa được (trg_file_attached_immutable): gỡ đúng lúc đổi owner_id rồi dựng lại nguyên văn
DROP TRIGGER trg_file_attached_immutable;

UPDATE file f
    JOIN wish w ON w.kind = 'TREATMENT' AND w.legacy_id = f.owner_id
SET f.owner_id = w.id
WHERE f.owner_type = 'M01.TREATMENT_WISH';

DELIMITER $$
CREATE TRIGGER trg_file_attached_immutable
    BEFORE UPDATE
    ON file
    FOR EACH ROW
BEGIN
    IF OLD.attached_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'file rows are immutable once attached (research H-13)';
    END IF;
END$$

CREATE PROCEDURE m01_verify_wishes()
BEGIN
    IF (SELECT COUNT(*) FROM wish WHERE kind = 'END_OF_LIFE') <> @m01_eol_count
        OR (SELECT COUNT(*) FROM wish WHERE kind = 'TREATMENT') <> @m01_treatment_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: wish row count differs';
    END IF;
    -- Phiếu cuối đời giữ id và nguyên mọi cột
    IF EXISTS (SELECT 1
               FROM end_of_life_wish o
               WHERE NOT EXISTS (SELECT 1
                                 FROM wish w
                                 WHERE w.id = o.id AND w.kind = 'END_OF_LIFE'
                                   AND w.resident_id = o.resident_id AND w.choice <=> o.choice
                                   AND w.first_contact <=> o.first_contact AND w.note <=> o.note
                                   AND w.respondent_type <=> o.respondent_type
                                   AND w.respondent_family_member_id <=> o.respondent_family_member_id
                                   AND w.relationship <=> o.relationship
                                   AND w.assessing_doctor_id <=> o.assessing_doctor_id
                                   AND w.can_self_determine <=> o.can_self_determine
                                   AND w.assessed_at <=> o.assessed_at AND w.support_method <=> o.support_method
                                   AND w.review_date <=> o.review_date AND w.signer_type <=> o.signer_type
                                   AND w.signer_family_member_id <=> o.signer_family_member_id
                                   AND w.representative_selection <=> o.representative_selection
                                   AND w.court_decision_number <=> o.court_decision_number
                                   AND w.court_decision_date <=> o.court_decision_date
                                   AND w.court_decision_court <=> o.court_decision_court
                                   AND w.court_decision_file_id <=> o.court_decision_file_id
                                   AND w.signed_at <=> o.signed_at
                                   AND w.signed_copy_file_id <=> o.signed_copy_file_id AND w.source <=> o.source
                                   AND w.status <=> o.status AND w.superseded_by_id <=> o.superseded_by_id
                                   AND w.recorded_by <=> o.recorded_by AND w.recorded_at <=> o.recorded_at)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: an end of life wish was not copied';
    END IF;
    -- Phiếu điều trị nhận id mới: nguyên mọi cột, phiếu thay thế trỏ đúng id mới
    IF EXISTS (SELECT 1
               FROM treatment_wish t
               WHERE NOT EXISTS (SELECT 1
                                 FROM wish w
                                 WHERE w.kind = 'TREATMENT' AND w.legacy_id = t.id
                                   AND w.resident_id = t.resident_id AND w.hospital1_name <=> t.hospital1_name
                                   AND w.hospital1_address <=> t.hospital1_address
                                   AND w.hospital2_name <=> t.hospital2_name
                                   AND w.hospital2_address <=> t.hospital2_address
                                   AND w.insurance_registration_place <=> t.insurance_registration_place
                                   AND w.insurance_card_number <=> t.insurance_card_number
                                   AND w.insurance_card_expiry <=> t.insurance_card_expiry
                                   AND w.ask_family_first <=> t.ask_family_first AND w.escort <=> t.escort
                                   AND w.note <=> t.note AND w.respondent_type <=> t.respondent_type
                                   AND w.respondent_family_member_id <=> t.respondent_family_member_id
                                   AND w.signed_copy_file_id <=> t.signed_copy_file_id
                                   AND w.status <=> t.status AND w.recorded_by <=> t.recorded_by
                                   AND w.recorded_at <=> t.recorded_at
                                   AND w.superseded_by_id <=> (SELECT n.id
                                                               FROM wish n
                                                               WHERE n.kind = 'TREATMENT'
                                                                 AND n.legacy_id = t.superseded_by_id))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a treatment wish was not copied';
    END IF;
    -- Người cần báo
    IF (SELECT COALESCE(SUM(JSON_LENGTH(notify_family_member_ids)), 0) FROM wish WHERE kind = 'TREATMENT')
        <> @m01_notify_count
        OR EXISTS (SELECT 1
                   FROM treatment_wish_notify n
                            JOIN wish w ON w.kind = 'TREATMENT' AND w.legacy_id = n.treatment_wish_id
                   WHERE NOT (n.family_member_id MEMBER OF (w.notify_family_member_ids))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a treatment wish notify member was not copied';
    END IF;
    -- Ý kiến khác: đủ số dòng, đúng phiếu, đúng nội dung và nguồn
    IF (SELECT COUNT(*)
        FROM record_event
        WHERE kind = 'M01.WISH_OPINION' AND entity_type = 'M01.END_OF_LIFE_WISH'
          AND content ->> '$.legacy.table' = 'end_of_life_wish_opinion') <> @m01_eol_opinion_count
        OR (SELECT COUNT(*)
            FROM record_event
            WHERE kind = 'M01.WISH_OPINION' AND entity_type = 'M01.TREATMENT_WISH'
              AND content ->> '$.legacy.table' = 'treatment_wish_opinion') <> @m01_treatment_opinion_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: wish opinion count differs';
    END IF;
    IF EXISTS (SELECT 1
               FROM end_of_life_wish_opinion o
               WHERE NOT EXISTS (SELECT 1
                                 FROM record_event e
                                 WHERE e.kind = 'M01.WISH_OPINION' AND e.entity_type = 'M01.END_OF_LIFE_WISH'
                                   AND e.entity_id = o.wish_id
                                   AND CAST(e.content ->> '$.legacy.id' AS UNSIGNED) = o.id
                                   AND e.content ->> '$.legacy.table' = 'end_of_life_wish_opinion'
                                   AND e.content ->> '$.sourceType' = o.source_type
                                   AND e.content ->> '$.content' = o.content
                                   AND e.actor_id = o.recorded_by AND e.occurred_at = o.recorded_at
                                   AND e.confirmed_at IS NOT NULL)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: an end of life wish opinion was not copied';
    END IF;
    IF EXISTS (SELECT 1
               FROM treatment_wish_opinion o
                        JOIN wish w ON w.kind = 'TREATMENT' AND w.legacy_id = o.treatment_wish_id
               WHERE NOT EXISTS (SELECT 1
                                 FROM record_event e
                                 WHERE e.kind = 'M01.WISH_OPINION' AND e.entity_type = 'M01.TREATMENT_WISH'
                                   AND e.entity_id = w.id
                                   AND CAST(e.content ->> '$.legacy.id' AS UNSIGNED) = o.id
                                   AND e.content ->> '$.legacy.table' = 'treatment_wish_opinion'
                                   AND e.content ->> '$.sourceType' = o.source_type
                                   AND e.content ->> '$.content' = o.content
                                   AND e.actor_id = o.recorded_by AND e.occurred_at = o.recorded_at
                                   AND e.confirmed_at IS NOT NULL)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a treatment wish opinion was not copied';
    END IF;
    -- Tệp của phiếu điều trị trỏ đúng id mới
    IF (SELECT COUNT(*) FROM file WHERE owner_type = 'M01.TREATMENT_WISH') <> @m01_treatment_file_count
        OR EXISTS (SELECT 1
                   FROM file f
                   WHERE f.owner_type = 'M01.TREATMENT_WISH'
                     AND NOT EXISTS (SELECT 1 FROM wish w WHERE w.id = f.owner_id AND w.kind = 'TREATMENT'))
        OR EXISTS (SELECT 1
                   FROM treatment_wish t
                            JOIN wish w ON w.kind = 'TREATMENT' AND w.legacy_id = t.id
                            JOIN file f ON f.id = t.signed_copy_file_id
                   WHERE f.owner_type = 'M01.TREATMENT_WISH' AND f.owner_id <> w.id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a treatment wish file owner was not remapped';
    END IF;
END$$
DELIMITER ;

CALL m01_verify_wishes();
DROP PROCEDURE m01_verify_wishes;

DROP TABLE treatment_wish_notify;
DROP TABLE treatment_wish_opinion;
DROP TABLE treatment_wish;
DROP TABLE end_of_life_wish_opinion;
DROP TABLE end_of_life_wish;

-- Chỉ đổi theo chiều Hiệu lực → Được thay thế (cuối đời) hoặc Hiệu lực → Hết hiệu lực (điều trị), không xóa (FR-023b,
-- FR-023k, DBR-34, DBR-39). Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger.
DELIMITER $$
CREATE TRIGGER trg_wish_only_supersede
    BEFORE UPDATE
    ON wish
    FOR EACH ROW
BEGIN
    IF NOT (NEW.kind <=> OLD.kind
        AND NEW.legacy_id <=> OLD.legacy_id
        AND NEW.resident_id <=> OLD.resident_id
        AND NEW.note <=> OLD.note
        AND NEW.respondent_type <=> OLD.respondent_type
        AND NEW.respondent_family_member_id <=> OLD.respondent_family_member_id
        AND NEW.signed_copy_file_id <=> OLD.signed_copy_file_id
        AND NEW.recorded_by <=> OLD.recorded_by
        AND NEW.recorded_at <=> OLD.recorded_at
        AND NEW.choice <=> OLD.choice
        AND NEW.first_contact <=> OLD.first_contact
        AND NEW.relationship <=> OLD.relationship
        AND NEW.assessing_doctor_id <=> OLD.assessing_doctor_id
        AND NEW.can_self_determine <=> OLD.can_self_determine
        AND NEW.assessed_at <=> OLD.assessed_at
        AND NEW.support_method <=> OLD.support_method
        AND NEW.review_date <=> OLD.review_date
        AND NEW.signer_type <=> OLD.signer_type
        AND NEW.signer_family_member_id <=> OLD.signer_family_member_id
        AND NEW.representative_selection <=> OLD.representative_selection
        AND NEW.court_decision_number <=> OLD.court_decision_number
        AND NEW.court_decision_date <=> OLD.court_decision_date
        AND NEW.court_decision_court <=> OLD.court_decision_court
        AND NEW.court_decision_file_id <=> OLD.court_decision_file_id
        AND NEW.signed_at <=> OLD.signed_at
        AND NEW.source <=> OLD.source
        AND NEW.hospital1_name <=> OLD.hospital1_name
        AND NEW.hospital1_address <=> OLD.hospital1_address
        AND NEW.hospital2_name <=> OLD.hospital2_name
        AND NEW.hospital2_address <=> OLD.hospital2_address
        AND NEW.insurance_registration_place <=> OLD.insurance_registration_place
        AND NEW.insurance_card_number <=> OLD.insurance_card_number
        AND NEW.insurance_card_expiry <=> OLD.insurance_card_expiry
        AND NEW.ask_family_first <=> OLD.ask_family_first
        AND NEW.escort <=> OLD.escort
        AND NEW.notify_family_member_ids <=> OLD.notify_family_member_ids) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wish content is immutable (FR-023b, FR-023k)';
    END IF;
    IF OLD.status = 'ACTIVE' AND NEW.status <> IF(OLD.kind = 'END_OF_LIFE', 'SUPERSEDED', 'EXPIRED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'an active wish can only be superseded or expire (DBR-34, DBR-39)';
    END IF;
    IF OLD.status <> 'ACTIVE' THEN
        -- Bản đã hết hiệu lực nhận tham chiếu tới bản mới sau khi bản mới được thêm (đúng một lần, từ NULL)
        IF NOT (NEW.status <=> OLD.status
            AND (NEW.superseded_by_id <=> OLD.superseded_by_id OR OLD.superseded_by_id IS NULL)) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a superseded or expired wish cannot change (DBR-34, DBR-39)';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_wish_no_delete
    BEFORE DELETE
    ON wish
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wish rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT ON wish TO 'eldercare_app'@'%';
GRANT UPDATE (status, superseded_by_id) ON wish TO 'eldercare_app'@'%';
GRANT DELETE ON wish TO 'eldercare_retention'@'%';

-- Khai nhóm dữ liệu (audit F-04): bản đồng ý là nhóm 2 (chỉ đổi qua lệnh rút lại, trigger trg_consent_only_withdrawal)
ALTER TABLE consent COMMENT = 'DataGroup=BUSINESS';

-- Nhóm 3 (một phần): lịch sử trạng thái (resident_status_history) và mức chăm sóc (care_level_history) vào
-- status_history (data-model 001 mục 0.1). Đối tượng M01.RESIDENT; thuộc tính STATUS: giá trị trước, giá trị là trạng
-- thái "từ", "đến", miễn điều kiện vào details.conditionWaiver; thuộc tính CARE_LEVEL: giá trị là mã mức, starts_at là
-- thời điểm hiệu lực, lúc ghi vào details.recordedAt, khóa M01.CLH:<residentId>:<từ> thay UNIQUE (resident_id,
-- effective_from). Lý do bắt buộc (FR-046) kiểm ở StatusHistoryApi (HistoryTrack.reasonRequired, A1 2026-11-04).
-- Dòng lịch sử trạng thái nhận id mới (legacy {table, id}); id mới luôn lớn hơn mọi id cũ nên tham chiếu cũ còn sót
-- (thông báo của 009, residency_ending.history_id của 004, phiếu đón của 012, nhật ký) không trỏ nhầm dòng mới. Nơi trỏ
-- id dòng lịch sử đổi theo bảng ánh xạ: tệp M01.HOSPITAL_DOCUMENT, M01.DEATH_EVIDENCE; đính chính, chuỗi đính chính
-- và yêu cầu đính chính chưa áp dụng loại M01.RESIDENT_STATUS_HISTORY; lần báo đánh giá lại nguồn STATUS_HISTORY.
SET @m01_status_count = (SELECT COUNT(*) FROM resident_status_history);
SET @m01_care_level_count = (SELECT COUNT(*) FROM care_level_history);
SET @m01_status_max_id = (SELECT COALESCE(MAX(id), 0) FROM resident_status_history);
SET @m01_history_file_count =
        (SELECT COUNT(*) FROM file WHERE owner_type IN ('M01.HOSPITAL_DOCUMENT', 'M01.DEATH_EVIDENCE'));
SET @m01_history_correction_count =
        (SELECT COUNT(*) FROM correction WHERE record_type = 'M01.RESIDENT_STATUS_HISTORY');
SET @m01_history_chain_count =
        (SELECT COUNT(*) FROM correction_chain WHERE record_type = 'M01.RESIDENT_STATUS_HISTORY');
SET @m01_history_notice_count = (SELECT COUNT(*) FROM reassessment_notice WHERE source_type = 'STATUS_HISTORY');

-- AUTO_INCREMENT chỉ đặt được bằng câu động; giá trị nhỏ hơn id lớn nhất đang có thì InnoDB giữ id lớn nhất + 1.
DELIMITER $$
CREATE PROCEDURE m01_raise_auto_increment(IN table_name VARCHAR(64), IN above BIGINT)
BEGIN
    SET @m01_sql = CONCAT('ALTER TABLE ', table_name, ' AUTO_INCREMENT = ', above + 1);
    PREPARE m01_statement FROM @m01_sql;
    EXECUTE m01_statement;
    DEALLOCATE PREPARE m01_statement;
END$$
DELIMITER ;

CALL m01_raise_auto_increment('status_history', @m01_status_max_id);

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_before, value, starts_at, command,
                            actor_id, by_system, reason, basis_type, basis_id, details)
SELECT 'M01.RESIDENT',
       resident_id,
       'STATUS',
       'M01',
       'POINT',
       from_status,
       to_status,
       occurred_at,
       command,
       performed_by,
       by_system,
       reason,
       basis_type,
       basis_id,
       JSON_OBJECT('conditionWaiver', condition_waiver,
                   'legacy', JSON_OBJECT('table', 'resident_status_history', 'id', id))
FROM resident_status_history
ORDER BY id;

INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            basis_type, basis_id, details, unique_key)
SELECT 'M01.RESIDENT',
       resident_id,
       'CARE_LEVEL',
       'M01',
       'POINT',
       care_level,
       effective_from,
       recorded_by,
       FALSE,
       basis_type,
       basis_id,
       JSON_OBJECT('recordedAt', DATE_FORMAT(recorded_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'legacy', JSON_OBJECT('table', 'care_level_history', 'id', id)),
       CONCAT('M01.CLH:', resident_id, ':', DATE_FORMAT(effective_from, '%Y-%m-%dT%H:%i:%s.%f'))
FROM care_level_history
ORDER BY id;

CREATE TABLE m01_history_id_map
(
    old_id BIGINT NOT NULL PRIMARY KEY,
    new_id BIGINT NOT NULL,
    UNIQUE (new_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO m01_history_id_map (old_id, new_id)
SELECT CAST(details ->> '$.legacy.id' AS UNSIGNED), id
FROM status_history
WHERE entity_type = 'M01.RESIDENT'
  AND attribute = 'STATUS'
  AND details ->> '$.legacy.table' = 'resident_status_history';

-- Tệp đã gắn không sửa được (trg_file_attached_immutable): gỡ đúng lúc đổi owner_id rồi dựng lại nguyên văn
DROP TRIGGER trg_file_attached_immutable;

UPDATE file f
    JOIN m01_history_id_map m ON m.old_id = f.owner_id
SET f.owner_id = m.new_id
WHERE f.owner_type IN ('M01.HOSPITAL_DOCUMENT', 'M01.DEATH_EVIDENCE');

DELIMITER $$
CREATE TRIGGER trg_file_attached_immutable
    BEFORE UPDATE
    ON file
    FOR EACH ROW
BEGIN
    IF OLD.attached_at IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'file rows are immutable once attached (research H-13)';
    END IF;
END$$
DELIMITER ;

-- Đính chính chỉ thêm (trg_correction_no_update, V000_06): gỡ đúng lúc đổi bản gốc rồi dựng lại nguyên văn. Id mới lớn hơn
-- mọi id cũ nên uq_correction_sequence không trùng giữa chừng.
DROP TRIGGER trg_correction_no_update;

UPDATE correction c
    JOIN m01_history_id_map m ON m.old_id = c.original_record_id
SET c.original_record_id = m.new_id
WHERE c.record_type = 'M01.RESIDENT_STATUS_HISTORY';

CREATE TRIGGER trg_correction_no_update
    BEFORE UPDATE
    ON correction
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'correction is append-only (DBR-23)';

UPDATE correction_chain c
    JOIN m01_history_id_map m ON m.old_id = c.original_record_id
SET c.original_record_id = m.new_id
WHERE c.record_type = 'M01.RESIDENT_STATUS_HISTORY';

-- Yêu cầu đính chính cách C chưa áp dụng (dòng đã áp dụng bị khóa, bản đính chính của nó đã đổi ở trên)
UPDATE approval_request r
    JOIN m01_history_id_map m ON m.old_id = CAST(r.content ->> '$.originalRecordId' AS UNSIGNED)
SET r.content = JSON_SET(r.content, '$.originalRecordId', m.new_id)
WHERE r.type = 'M15.CORRECTION'
  AND r.locked_at IS NULL
  AND r.content ->> '$.recordType' = 'M01.RESIDENT_STATUS_HISTORY';

UPDATE reassessment_notice n
    JOIN m01_history_id_map m ON m.old_id = n.source_id
SET n.source_id = m.new_id
WHERE n.source_type = 'STATUS_HISTORY';

DELIMITER $$
CREATE PROCEDURE m01_verify_histories()
BEGIN
    IF (SELECT COUNT(*) FROM status_history WHERE entity_type = 'M01.RESIDENT' AND attribute = 'STATUS'
          AND details ->> '$.legacy.table' = 'resident_status_history') <> @m01_status_count
        OR (SELECT COUNT(*) FROM m01_history_id_map) <> @m01_status_count
        OR (SELECT COUNT(*) FROM status_history WHERE entity_type = 'M01.RESIDENT' AND attribute = 'CARE_LEVEL'
              AND details ->> '$.legacy.table' = 'care_level_history') <> @m01_care_level_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: status or care level history row count differs';
    END IF;
    IF (SELECT MIN(new_id) FROM m01_history_id_map) <= @m01_status_max_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a new status history id is not above the old ids';
    END IF;
    -- Từng dòng nguyên nội dung theo ánh xạ id cũ → mới
    IF EXISTS (SELECT 1
               FROM resident_status_history o
               WHERE NOT EXISTS (SELECT 1
                                 FROM m01_history_id_map m
                                          JOIN status_history n ON n.id = m.new_id
                                 WHERE m.old_id = o.id
                                   AND n.entity_type = 'M01.RESIDENT' AND n.attribute = 'STATUS'
                                   AND n.entity_id = o.resident_id AND n.kind = 'POINT'
                                   AND n.value_before <=> o.from_status AND n.value = o.to_status
                                   AND n.starts_at = o.occurred_at AND n.command = o.command
                                   AND n.actor_id = o.performed_by AND n.by_system = o.by_system
                                   AND n.reason <=> o.reason AND n.basis_type <=> o.basis_type
                                   AND n.basis_id <=> o.basis_id
                                   AND (n.details -> '$.conditionWaiver') <=> COALESCE(o.condition_waiver,
                                                                                    CAST('null' AS JSON)))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a status history row was not copied';
    END IF;
    IF EXISTS (SELECT 1
               FROM care_level_history o
               WHERE NOT EXISTS (SELECT 1
                                 FROM status_history n
                                 WHERE n.entity_type = 'M01.RESIDENT' AND n.attribute = 'CARE_LEVEL'
                                   AND CAST(n.details ->> '$.legacy.id' AS UNSIGNED) = o.id
                                   AND n.entity_id = o.resident_id AND n.kind = 'POINT' AND n.value = o.care_level
                                   AND n.starts_at = o.effective_from AND n.basis_type = o.basis_type
                                   AND n.basis_id = o.basis_id AND n.actor_id = o.recorded_by
                                   AND n.details ->> '$.recordedAt' = DATE_FORMAT(o.recorded_at, '%Y-%m-%dT%H:%i:%s.%fZ')
                                   AND n.unique_key = CONCAT('M01.CLH:', o.resident_id, ':',
                                                             DATE_FORMAT(o.effective_from, '%Y-%m-%dT%H:%i:%s.%f')))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a care level history row was not copied';
    END IF;
    -- Nơi trỏ dòng lịch sử: đủ số dòng, mỗi dòng trỏ dòng lịch sử trạng thái mới
    IF (SELECT COUNT(*) FROM file WHERE owner_type IN ('M01.HOSPITAL_DOCUMENT', 'M01.DEATH_EVIDENCE'))
        <> @m01_history_file_count
        OR EXISTS (SELECT 1
                   FROM file f
                   WHERE f.owner_type IN ('M01.HOSPITAL_DOCUMENT', 'M01.DEATH_EVIDENCE')
                     AND NOT EXISTS (SELECT 1 FROM m01_history_id_map m WHERE m.new_id = f.owner_id)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a hospital document or death evidence file was not remapped';
    END IF;
    IF (SELECT COUNT(*) FROM correction WHERE record_type = 'M01.RESIDENT_STATUS_HISTORY')
        <> @m01_history_correction_count
        OR (SELECT COUNT(*) FROM correction_chain WHERE record_type = 'M01.RESIDENT_STATUS_HISTORY')
        <> @m01_history_chain_count
        OR EXISTS (SELECT 1
                   FROM correction c
                   WHERE c.record_type = 'M01.RESIDENT_STATUS_HISTORY'
                     AND NOT EXISTS (SELECT 1 FROM m01_history_id_map m WHERE m.new_id = c.original_record_id))
        OR EXISTS (SELECT 1
                   FROM correction_chain c
                   WHERE c.record_type = 'M01.RESIDENT_STATUS_HISTORY'
                     AND NOT EXISTS (SELECT 1 FROM m01_history_id_map m WHERE m.new_id = c.original_record_id))
        OR EXISTS (SELECT 1
                   FROM approval_request r
                   WHERE r.type = 'M15.CORRECTION' AND r.locked_at IS NULL
                     AND r.content ->> '$.recordType' = 'M01.RESIDENT_STATUS_HISTORY'
                     AND NOT EXISTS (SELECT 1
                                     FROM m01_history_id_map m
                                     WHERE m.new_id = CAST(r.content ->> '$.originalRecordId' AS UNSIGNED))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a status history correction was not remapped';
    END IF;
    IF (SELECT COUNT(*) FROM reassessment_notice WHERE source_type = 'STATUS_HISTORY') <> @m01_history_notice_count
        OR EXISTS (SELECT 1
                   FROM reassessment_notice n
                   WHERE n.source_type = 'STATUS_HISTORY'
                     AND NOT EXISTS (SELECT 1 FROM m01_history_id_map m WHERE m.new_id = n.source_id)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a reassessment notice source was not remapped';
    END IF;
    -- Không còn khóa ngoại nào trỏ vào hai bảng sắp xóa
    IF EXISTS (SELECT 1
               FROM information_schema.KEY_COLUMN_USAGE
               WHERE TABLE_SCHEMA = DATABASE()
                 AND REFERENCED_TABLE_NAME IN ('resident_status_history', 'care_level_history')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a foreign key still references a history table to be dropped';
    END IF;
END$$
DELIMITER ;

CALL m01_verify_histories();
DROP PROCEDURE m01_verify_histories;

DROP TABLE resident_status_history;
DROP TABLE care_level_history;
DROP TABLE m01_history_id_map;

-- Nhóm 2 (một phần): lần báo cần đánh giá lại (reassessment_notice, bảng kỹ thuật của M01) vào simple_record loại
-- M01.REASSESSMENT_NOTICE (data-model 001 mục 0.1): người cao tuổi ở resident_id; nguồn là đối tượng cha (ASSESSMENT →
-- M01.ASSESSMENT, INCIDENT → M05.INCIDENT, STATUS_HISTORY → M01.RESIDENT_STATUS_HISTORY trỏ id lịch sử mới đã đổi ở
-- trên, CORRECTION → M15.CORRECTION); {basis, notifiedAt, deadlineFrom, overdueAlertedAt} ở content; created_at là lúc
-- báo; khóa M01.RAN:<residentId>:<basis>:<sourceType>:<sourceId> thay uq_reassessment_notice_source. Dòng không khóa
-- (hạn, dấu đã cảnh báo còn đổi). Id mới lớn hơn mọi id cũ (sự kiện ReassessmentDue của 009 trỏ id lần báo).
SET @m01_notice_count = (SELECT COUNT(*) FROM reassessment_notice);
SET @m01_notice_max_id = (SELECT COALESCE(MAX(id), 0) FROM reassessment_notice);

CALL m01_raise_auto_increment('simple_record', @m01_notice_max_id);

INSERT INTO simple_record (kind, module, resident_id, entity_type, entity_id, status, content, unique_key, created_at,
                           created_by, version)
SELECT 'M01.REASSESSMENT_NOTICE',
       'M01',
       resident_id,
       CASE source_type
           WHEN 'ASSESSMENT' THEN 'M01.ASSESSMENT'
           WHEN 'INCIDENT' THEN 'M05.INCIDENT'
           WHEN 'STATUS_HISTORY' THEN 'M01.RESIDENT_STATUS_HISTORY'
           WHEN 'CORRECTION' THEN 'M15.CORRECTION'
           END,
       source_id,
       'NOTIFIED',
       JSON_OBJECT('basis', basis,
                   'notifiedAt', DATE_FORMAT(notified_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'deadlineFrom', DATE_FORMAT(deadline_from, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'overdueAlertedAt', DATE_FORMAT(overdue_alerted_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                   'legacy', JSON_OBJECT('table', 'reassessment_notice', 'id', id)),
       CONCAT('M01.RAN:', resident_id, ':', basis, ':', source_type, ':', source_id),
       notified_at,
       0,
       0
FROM reassessment_notice
ORDER BY id;

DELIMITER $$
CREATE PROCEDURE m01_verify_reassessment_notices()
BEGIN
    IF (SELECT COUNT(*) FROM simple_record WHERE kind = 'M01.REASSESSMENT_NOTICE'
          AND content ->> '$.legacy.table' = 'reassessment_notice') <> @m01_notice_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: reassessment notice row count differs';
    END IF;
    IF EXISTS (SELECT 1 FROM simple_record WHERE kind = 'M01.REASSESSMENT_NOTICE' AND entity_type IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a reassessment notice has an unknown source type';
    END IF;
    IF (SELECT MIN(id) FROM simple_record WHERE kind = 'M01.REASSESSMENT_NOTICE'
          AND content ->> '$.legacy.table' = 'reassessment_notice') <= @m01_notice_max_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a new reassessment notice id is not above the old ids';
    END IF;
    IF EXISTS (SELECT 1
               FROM reassessment_notice o
               WHERE NOT EXISTS (SELECT 1
                                 FROM simple_record s
                                 WHERE s.kind = 'M01.REASSESSMENT_NOTICE'
                                   AND CAST(s.content ->> '$.legacy.id' AS UNSIGNED) = o.id
                                   AND s.resident_id = o.resident_id AND s.entity_id = o.source_id
                                   AND s.content ->> '$.basis' = o.basis AND s.created_at = o.notified_at
                                   AND s.locked_at IS NULL
                                   AND JSON_REMOVE(s.content, '$.legacy') = JSON_OBJECT(
                                           'basis', o.basis,
                                           'notifiedAt', DATE_FORMAT(o.notified_at, '%Y-%m-%dT%H:%i:%s.%fZ'),
                                           'deadlineFrom', DATE_FORMAT(o.deadline_from, '%Y-%m-%dT%H:%i:%s.%fZ'),
                                           'overdueAlertedAt',
                                           DATE_FORMAT(o.overdue_alerted_at, '%Y-%m-%dT%H:%i:%s.%fZ'))
                                   AND s.unique_key = CONCAT('M01.RAN:', o.resident_id, ':', o.basis, ':', o.source_type,
                                                             ':', o.source_id))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V001_95: a reassessment notice was not copied';
    END IF;
END$$
DELIMITER ;

CALL m01_verify_reassessment_notices();
DROP PROCEDURE m01_verify_reassessment_notices;

DROP TABLE reassessment_notice;

DROP PROCEDURE m01_raise_auto_increment;
