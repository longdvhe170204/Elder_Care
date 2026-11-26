-- BF-06 (đề xuất 2026-11-24): "Ghi nhận sau" cho sự cố đã xảy ra và đã xử lý xong (không báo khẩn, không leo thang) và
-- ghi nhận việc báo người nhà (ai, lúc nào, cách nào). Chỉ thêm cột có giá trị mặc định: sự cố cũ và mọi luồng ghi sự cố
-- hiện có giữ nguyên hành vi. Bộ kích hoạt trg_incident_row_lock không so sánh các cột này nên cập nhật sau khi ghi được.
ALTER TABLE incident
    ADD COLUMN recorded_later BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN family_notice_state VARCHAR(16) NOT NULL DEFAULT 'NONE',
    ADD COLUMN family_notified_by BIGINT NULL,
    ADD COLUMN family_notified_at DATETIME(6) NULL,
    ADD COLUMN family_notice_method VARCHAR(16) NULL,
    ADD COLUMN family_notice_note VARCHAR(500) NULL,
    ADD CONSTRAINT chk_incident_family_notice_state CHECK (family_notice_state IN ('NONE', 'INFORMED', 'NOT_YET'));
