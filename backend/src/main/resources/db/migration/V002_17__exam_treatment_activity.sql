-- Q-401 (chủ dự án duyệt 2026-11-07), 007 FR-072, FR-073: nghiệp vụ chuyên môn mặc định "khám, chẩn đoán và điều trị tại
-- viện" cho thao tác (EXAM_TREATMENT, PROFESSIONAL), cùng khuôn với "chẩn đoán và kê đơn nội bộ" của V002_07 (bảng đã gộp vào
-- catalog_item ở V002_95): cần phạm vi của giấy phép cơ sở; loại giấy phép NULL tới khi Quản lý viện cấu hình (BR-M06-05).
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES ('M15.PROFESSIONAL_ACTIVITY', 'M15', 'INTERNAL_EXAM_TREATMENT', 'Khám, chẩn đoán và điều trị tại viện', 0, 'ACTIVE',
        JSON_OBJECT('functionCode', 'EXAM_TREATMENT',
                    'requiredLicenseType', NULL,
                    'requiresFacilityScope', CAST('true' AS JSON),
                    'isDefault', CAST('true' AS JSON),
                    'practiceScopeIds', JSON_ARRAY()));
