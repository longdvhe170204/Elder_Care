-- Gộp bảng về 79 (research R-15 của 000, chủ dự án yêu cầu 2026-11-13), phần 003: bỏ 5 bảng chỉ 003 dùng.
--   room_type_care_level   -> cột JSON room_type.allowed_care_levels (như room.allowed_care_levels của V003_95);
--   room_isolation_history -> status_history, điểm M03.ROOM / ISOLATION (value ISOLATE, LIFT; căn cứ M07.INCIDENT);
--   bed_backfill_request   -> record_event loại M03.BED_BACKFILL_REQUEST của phân bổ hiện hành, occurred_at là lúc chuyển;
--   bed_transfer_reason    -> catalog_item loại M03.BED_TRANSFER_REASON (cờ y tế/an toàn, cờ cố định ở attributes);
--   day_care_rest_area     -> catalog_item loại M03.DAY_CARE_REST_AREA (mã, tầng hoặc khu vực ở attributes).
-- Giữ area, floor, room_type, day_care_session: module khác lưu id (quyết định 2026-11-09). Id lý do chuyển giường đổi:
-- bed_allocation.transfer_reason_id được chép theo mã lý do; yêu cầu nhập bù còn chờ duyệt lập trước migration mang id
-- lý do cũ trong nội dung yêu cầu (chưa có dữ liệu vận hành). Số V099 để chạy sau migration của mọi feature trên cơ sở
-- dữ liệu mới. Cú pháp dùng được ở MySQL 8.0 và 8.4. Thời điểm DATETIME(6) của 003 là UTC.

-- 1. Mức chăm sóc của loại phòng vào cột JSON (mảng mã M01.CARE_LEVEL), giữ id loại phòng.
ALTER TABLE room_type ADD COLUMN allowed_care_levels JSON NULL;
UPDATE room_type t
SET t.allowed_care_levels = COALESCE(
        (SELECT JSON_ARRAYAGG(l.care_level) FROM room_type_care_level l WHERE l.room_type_id = t.id), JSON_ARRAY());
ALTER TABLE room_type MODIFY allowed_care_levels JSON NOT NULL DEFAULT (JSON_ARRAY());

-- 2. Lịch sử cách ly phòng.
INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system,
                            reason, basis_type, basis_id, details)
SELECT 'M03.ROOM',
       room_id,
       'ISOLATION',
       'M03',
       'POINT',
       action,
       occurred_at,
       actor_id,
       FALSE,
       reason,
       IF(incident_id IS NULL, NULL, 'M07.INCIDENT'),
       incident_id,
       JSON_OBJECT('legacy', JSON_OBJECT('table', 'room_isolation_history', 'id', id))
FROM room_isolation_history
ORDER BY occurred_at, id;

-- 3. Chỉ mục yêu cầu nhập bù chờ duyệt.
INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at, confirmed_at,
                          unique_key)
SELECT 'M03.BED_ALLOCATION',
       current_allocation_id,
       'M03',
       'M03.BED_BACKFILL_REQUEST',
       JSON_OBJECT('approvalRequestId', approval_request_id, 'residentId', resident_id, 'newBedId', new_bed_id),
       TRUE,
       transfer_at,
       transfer_at,
       CONCAT('M03.BED_BACKFILL_REQUEST:', approval_request_id)
FROM bed_backfill_request
ORDER BY approval_request_id;

-- 4. Lý do chuyển giường; phân bổ trỏ sang id mới theo mã lý do (mã duy nhất ở cả hai bảng).
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes, version)
SELECT 'M03.BED_TRANSFER_REASON',
       'M03',
       code,
       name,
       0,
       status,
       JSON_OBJECT('medicalSafety', IF(medical_safety, CAST('true' AS JSON), CAST('false' AS JSON)),
                   'fixed', IF(fixed, CAST('true' AS JSON), CAST('false' AS JSON))),
       version
FROM bed_transfer_reason
ORDER BY id;

ALTER TABLE bed_allocation DROP FOREIGN KEY fk_allocation_transfer_reason;
-- Phân bổ Đã đóng, Đã hủy không sửa được (FR-026): gỡ trigger trong lúc đổi id tham chiếu rồi tạo lại nguyên văn.
DROP TRIGGER trg_bed_allocation_guard_update;

UPDATE bed_allocation a
    JOIN bed_transfer_reason o ON o.id = a.transfer_reason_id
    JOIN catalog_item c ON c.catalog_type = 'M03.BED_TRANSFER_REASON' AND c.code = o.code
SET a.transfer_reason_id = c.id;

DELIMITER $$
CREATE TRIGGER trg_bed_allocation_guard_update
    BEFORE UPDATE
    ON bed_allocation
    FOR EACH ROW
BEGIN
 IF OLD.status IN ('CLOSED','CANCELLED') THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Closed or cancelled allocation is immutable (FR-026)';
 END IF;
 IF NEW.status<>'CANCELLED' AND EXISTS (
   SELECT 1 FROM bed_allocation a WHERE a.id<>OLD.id AND a.bed_id=NEW.bed_id AND a.status<>'CANCELLED'
    AND a.start_at<COALESCE(NEW.end_at,NEW.expected_end,'9999-12-31')
    AND NEW.start_at<a.effective_end
 ) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ALLOCATION_TIME_OVERLAP'; END IF;
END$$
DELIMITER ;

ALTER TABLE bed_allocation
    ADD CONSTRAINT fk_allocation_transfer_reason FOREIGN KEY (transfer_reason_id) REFERENCES catalog_item (id)
        ON DELETE RESTRICT;

-- 5. Khu nghỉ bán trú. Đúng một trong tầng, khu vực và chỉ một khu Hiệu lực do DayCareConfigurationService kiểm dưới
-- khóa M03.SUC_CHUA_BAN_TRU (thay CHECK, UNIQUE của bảng cũ).
INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes, version)
SELECT 'M03.DAY_CARE_REST_AREA',
       'M03',
       name,
       0,
       status,
       JSON_OBJECT('code', code, 'floorId', floor_id, 'areaId', area_id),
       version
FROM day_care_rest_area
ORDER BY id;

-- Kiểm số dòng trước khi bỏ bảng cũ.
DELIMITER $$
CREATE PROCEDURE m03_check_consolidation_79()
BEGIN
    IF (SELECT COUNT(*) FROM room_type_care_level l
            JOIN room_type t ON t.id = l.room_type_id
        WHERE NOT JSON_CONTAINS(t.allowed_care_levels, JSON_QUOTE(CONCAT(l.care_level, '')))) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V099_03: mức chăm sóc của loại phòng chép thiếu';
    END IF;
    IF (SELECT COUNT(*) FROM room_isolation_history)
        <> (SELECT COUNT(*) FROM status_history WHERE entity_type = 'M03.ROOM' AND attribute = 'ISOLATION') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V099_03: số dòng lịch sử cách ly lệch';
    END IF;
    IF (SELECT COUNT(*) FROM bed_backfill_request)
        <> (SELECT COUNT(*) FROM record_event WHERE kind = 'M03.BED_BACKFILL_REQUEST') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V099_03: số dòng yêu cầu nhập bù lệch';
    END IF;
    IF (SELECT COUNT(*) FROM bed_transfer_reason)
        <> (SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M03.BED_TRANSFER_REASON')
        OR (SELECT COUNT(*) FROM bed_allocation a
            WHERE a.transfer_reason_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM catalog_item c
                              WHERE c.id = a.transfer_reason_id AND c.catalog_type = 'M03.BED_TRANSFER_REASON')) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V099_03: lý do chuyển giường chép thiếu';
    END IF;
    IF (SELECT COUNT(*) FROM day_care_rest_area)
        <> (SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M03.DAY_CARE_REST_AREA') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V099_03: số dòng khu nghỉ lệch';
    END IF;
END$$
DELIMITER ;

CALL m03_check_consolidation_79();
DROP PROCEDURE m03_check_consolidation_79;

-- Gỡ quyền trước khi bỏ bảng cũ.
REVOKE INSERT, UPDATE, DELETE ON room_type_care_level FROM 'eldercare_app'@'%';
REVOKE INSERT ON room_isolation_history FROM 'eldercare_app'@'%';
REVOKE DELETE ON room_isolation_history FROM 'eldercare_retention'@'%';
REVOKE INSERT ON bed_backfill_request FROM 'eldercare_app'@'%';
REVOKE DELETE ON bed_backfill_request FROM 'eldercare_retention'@'%';
REVOKE INSERT, UPDATE, DELETE ON bed_transfer_reason FROM 'eldercare_app'@'%';
REVOKE INSERT, UPDATE ON day_care_rest_area FROM 'eldercare_app'@'%';

DROP TABLE room_type_care_level;
DROP TABLE room_isolation_history;
DROP TABLE bed_backfill_request;
DROP TABLE bed_transfer_reason;
DROP TABLE day_care_rest_area;
