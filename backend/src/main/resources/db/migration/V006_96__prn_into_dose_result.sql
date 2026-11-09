-- 006 T069 (R-15 của 000, data-model 006 mục 0.1): gộp prn_administration vào dose_result, cột kind phân biệt.
-- A chốt 2026-11-05 (như V001_95): dòng PRN cấp id mới, ghi legacy = {table, id}; nhật ký, đính chính cũ trỏ
-- M07.PRN_ADMINISTRATION + id cũ không nối sang id mới, không khai LegacyRecordTypeSource vì id cũ trùng id kết quả liều.
-- Id mới bắt đầu sau cả id lớn nhất của dose_result và của prn_administration, để không id PRN mới nào trùng id PRN cũ
-- (đính chính cũ của PRN không gắn nhầm vào lần dùng PRN khác).

ALTER TABLE dose_result
    DROP CHECK chk_dose_result_time;

ALTER TABLE dose_result
    ADD COLUMN kind ENUM ('SCHEDULED', 'PRN') NOT NULL DEFAULT 'SCHEDULED' COMMENT 'Kết quả liều theo lịch hoặc lần dùng PRN' AFTER id,
    MODIFY COLUMN dose_id BIGINT NULL COMMENT 'Single closure record per dose (DBR-13, BR-M07-12); NULL khi PRN',
    ADD COLUMN prescription_id BIGINT NULL COMMENT 'Đơn PRN (chỉ kind PRN)' AFTER dose_id,
    MODIFY COLUMN authority_basis ENUM ('ASSIGNED', 'SHIFT_LEAD', 'FLOOR_SHARED', 'LEAVE_MEDICATION', 'REPORTED_BY_OTHERS', 'SYSTEM') NULL COMMENT 'Authority justification; NULL khi PRN',
    ADD COLUMN dosage DECIMAL(12, 3) NULL COMMENT 'Liều PRN đã dùng',
    ADD COLUMN dosage_unit VARCHAR(30) NULL COMMENT 'Đơn vị liều PRN',
    ADD COLUMN exceeded_limit BOOLEAN NULL COMMENT 'PRN vượt giới hạn (BR-M07-04)',
    ADD COLUMN legacy JSON NULL COMMENT 'Bảng và id cũ của dòng chép sang (R-15)',
    ADD CONSTRAINT chk_dose_result_kind_dose CHECK ((kind = 'SCHEDULED') = (dose_id IS NOT NULL)),
    ADD CONSTRAINT chk_dose_result_prn CHECK (kind = 'SCHEDULED' OR (prescription_id IS NOT NULL AND dosage > 0
        AND dosage_unit IS NOT NULL AND exceeded_limit IS NOT NULL AND administered_at IS NOT NULL AND reason IS NOT NULL)),
    ADD CONSTRAINT chk_dose_result_basis CHECK (kind = 'PRN' OR authority_basis IS NOT NULL),
    -- Giữ đúng phạm vi cũ: ràng buộc thời điểm chỉ có ở kết quả liều theo lịch.
    ADD CONSTRAINT chk_dose_result_time CHECK (kind = 'PRN' OR administered_at IS NULL
        OR administered_at <= COALESCE(device_recorded_at, recorded_at)),
    ADD INDEX idx_dose_result_kind_prescription_time (kind, prescription_id, administered_at);

SET @m07_prn_id_base = GREATEST(
        COALESCE((SELECT MAX(id) FROM dose_result), 0),
        COALESCE((SELECT MAX(id) FROM prn_administration), 0));

INSERT INTO dose_result (id, kind, dose_id, prescription_id, resident_id, floor_area_id, result_type, administered_at,
                         recorded_at, device_recorded_at, recorder_id, authority_basis, reported_by_name,
                         adverse_reaction, reason, note, late_flag, offline_flag, synced_after_permission_loss,
                         dosage, dosage_unit, exceeded_limit, legacy)
SELECT @m07_prn_id_base + ROW_NUMBER() OVER (ORDER BY p.id),
       'PRN',
       NULL,
       p.prescription_id,
       p.resident_id,
       p.floor_area_id,
       'ADMINISTERED',
       p.administered_at,
       p.created_at,
       p.offline_device_time,
       p.administered_by_id,
       NULL,
       p.reported_by,
       p.adverse_reaction,
       p.reason,
       p.note,
       FALSE,
       p.offline_device_time IS NOT NULL,
       p.recorded_after_disconnect,
       p.dosage,
       p.dosage_unit,
       p.exceeded_limit,
       JSON_OBJECT('table', 'prn_administration', 'id', p.id)
FROM prn_administration p;

-- Bản ghi chờ xem lại của PRN trỏ id mới (data-model 006 mục 0.1).
UPDATE dose_review r
    JOIN dose_result d ON d.kind = 'PRN'
        AND JSON_UNQUOTE(d.legacy->'$.table') = 'prn_administration'
        AND CAST(d.legacy->'$.id' AS UNSIGNED) = r.prn_usage_id
SET r.prn_usage_id = d.id
WHERE r.prn_usage_id IS NOT NULL;

DROP TABLE prn_administration;
