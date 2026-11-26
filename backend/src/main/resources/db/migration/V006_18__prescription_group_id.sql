-- M07-01: gom nhóm các dòng thuốc thuộc cùng một đơn thuốc (giữ nguyên dữ liệu cũ, không xóa bảng/cột)
ALTER TABLE prescription
    ADD COLUMN prescription_group_id VARCHAR(64) NULL AFTER resident_id;

CREATE INDEX idx_prescription_group ON prescription (prescription_group_id);
