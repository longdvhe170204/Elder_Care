-- Dữ liệu demo (chỉ profile demo; chủ dự án duyệt 2026-11-21): Trưởng tầng đặt Điều dưỡng phụ trách thì 008 kiểm giấy phép
-- của nghiệp vụ "Phát thuốc" (AssignmentService: Điều dưỡng phụ trách cần loại giấy phép của MEDICATION_ADMINISTRATION).
-- Demo chưa cấu hình nên lệnh bị chặn "Chưa cấu hình loại giấy phép cho phát thuốc" (BF-01 bước 16, F17). Làm thay Quản
-- lý viện việc cấu hình như V900_04 cho kê đơn: loại giấy phép của nghiệp vụ Phát thuốc là NURSING_LICENSE, loại mà Điều
-- dưỡng demo 9005 (V900_01), 9011 (V900_09) đã có. Chạy lại không đổi gì. Loại giấy phép chưa cấu hình là JSON null
-- (V002_95), không phải SQL NULL: kiểm bằng JSON_TYPE. Cú pháp dùng được ở MySQL 8.0 và 8.4.

UPDATE catalog_item
SET attributes = JSON_SET(attributes, '$.requiredLicenseType', 'NURSING_LICENSE')
WHERE catalog_type = 'M15.PROFESSIONAL_ACTIVITY'
  AND code = 'MEDICATION_ADMINISTRATION'
  AND COALESCE(JSON_TYPE(JSON_EXTRACT(attributes, '$.requiredLicenseType')), 'NULL') = 'NULL';
