ALTER TABLE medication_reconciliation
    ADD COLUMN facility_name VARCHAR(255) NULL AFTER reason_detail,
    ADD COLUMN prescriber_name VARCHAR(255) NULL AFTER facility_name,
    ADD COLUMN prescribed_at DATE NULL AFTER prescriber_name,
    ADD COLUMN comparison_source VARCHAR(255) NULL AFTER prescribed_at,
    ADD COLUMN attachment_file_id BIGINT NULL AFTER comparison_source,
    ADD COLUMN attachment_name VARCHAR(255) NULL AFTER attachment_file_id,
    ADD COLUMN nurse_note VARCHAR(2000) NULL AFTER attachment_name,
    ADD COLUMN doctor_conclusion VARCHAR(2000) NULL AFTER nurse_note;
