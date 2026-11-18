-- Dữ liệu demo (chỉ profile demo; 2026-11-19): từ nay kê đơn nội bộ, đổi liều sang đơn nội bộ, ghi và xác nhận phiếu đối
-- chiếu thuốc kiểm "Bác sĩ có quyền kê đơn hợp lệ" qua lớp pháp lý của 002 (006 FR-005, BR-M06-05) thay cho vai trò.
-- Để Bác sĩ demo (9004, giấy phép DOCTOR_LICENSE ở V900_01) kê đơn được, làm thay Quản lý viện hai việc cấu hình:
-- (1) loại giấy phép của nghiệp vụ "chẩn đoán và kê đơn nội bộ" là DOCTOR_LICENSE; (2) giấy phép hoạt động của cơ sở
-- có phạm vi khám bệnh, chữa bệnh gồm nghiệp vụ đó. Chạy lại không đổi gì. Cú pháp dùng được ở MySQL 8.0 và 8.4.
-- Loại giấy phép chưa cấu hình là JSON null (V002_95), không phải SQL NULL: kiểm bằng JSON_TYPE.

UPDATE catalog_item
SET attributes = JSON_SET(attributes, '$.requiredLicenseType', 'DOCTOR_LICENSE')
WHERE catalog_type = 'M15.PROFESSIONAL_ACTIVITY'
  AND code = 'INTERNAL_DIAGNOSIS_PRESCRIBING'
  AND COALESCE(JSON_TYPE(JSON_EXTRACT(attributes, '$.requiredLicenseType')), 'NULL') = 'NULL';

INSERT IGNORE INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES ('M06.FACILITY_LICENSE', 'M06', 'GP-DEMO-0001@2024-01-01', 'Giấy phép GP-DEMO-0001', 0, 'ACTIVE',
        JSON_OBJECT('number', 'GP-DEMO-0001', 'issuer', 'Sở Y tế', 'issueDate', '2024-01-01',
                    'expiryDate', '2030-12-31', 'medicalCare', CAST('true' AS JSON),
                    'operationScopes', JSON_ARRAY('INTERNAL_DIAGNOSIS_PRESCRIBING'), 'scanFileId', NULL,
                    'replacesId', NULL, 'replacementDueToError', CAST('false' AS JSON), 'reason', NULL));
