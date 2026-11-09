-- 019 T018 lượt 2 (FR-013, Q-314; W-09): yêu cầu thanh lý đang xét và thời điểm thanh lý (mốc tính hạn lưu giữ).
ALTER TABLE asset
    ADD COLUMN disposal_request_id BIGINT      NULL COMMENT 'Yêu cầu phê duyệt M03.ASSET_DISPOSAL gần nhất',
    ADD COLUMN disposed_at         DATETIME(6) NULL COMMENT 'Lúc áp dụng yêu cầu thanh lý đã duyệt',
    ADD CONSTRAINT chk_asset_disposed CHECK (status <> 'DISPOSED' OR disposed_at IS NOT NULL);
