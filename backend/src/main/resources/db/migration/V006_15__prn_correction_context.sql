-- Tầng của người cao tuổi tại thời điểm ghi lần dùng PRN, phục vụ phân quyền đính chính FR-030.
ALTER TABLE prn_administration
    ADD COLUMN floor_area_id BIGINT NULL AFTER resident_id,
    ADD INDEX idx_prn_floor_time (floor_area_id, administered_at);
