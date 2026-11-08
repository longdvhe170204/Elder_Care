-- 007 T005 bổ sung: bốn loại có tác động riêng (IncidentCatalogs.PROTECTED_TYPES) ngoài bảng lâm sàng Q-398.
-- Đồ gửi thất lạc, hư hỏng: mức Trung bình theo spec 007 (FR-042, dòng "đồ gửi"; 013 FR-021), không đánh giá lại (FR-022).
-- Cơ sở vật chất, lây nhiễm nghi ngờ: spec chưa ghi mức, D chốt Trung bình 2026-11-08 (team-plan mục 8, chờ chủ dự án duyệt trong PR).
-- Dấu "không thuộc sức khỏe" mặc định bật cho đồ gửi và cơ sở vật chất (nghiep-vu 9.3); chỉ cơ sở vật chất được không gắn
-- người cao tuổi (BR-M05-17). R-15: dòng catalog_item, không tạo bảng riêng.

INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES
    ('M05.INCIDENT_TYPE', 'M05', 'BELONGINGS_LOST', 'Đồ gửi thất lạc', 10, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'MEDIUM', 'nonHealth', TRUE, 'allowsNoResident', FALSE)),
    ('M05.INCIDENT_TYPE', 'M05', 'BELONGINGS_DAMAGED', 'Đồ gửi hư hỏng', 11, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'MEDIUM', 'nonHealth', TRUE, 'allowsNoResident', FALSE)),
    ('M05.INCIDENT_TYPE', 'M05', 'FACILITY', 'Cơ sở vật chất', 12, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'MEDIUM', 'nonHealth', TRUE, 'allowsNoResident', TRUE)),
    ('M05.INCIDENT_TYPE', 'M05', 'SUSPECTED_INFECTION', 'Lây nhiễm nghi ngờ', 13, 'ACTIVE',
     JSON_OBJECT('defaultLevel', 'MEDIUM', 'nonHealth', FALSE, 'allowsNoResident', FALSE));
