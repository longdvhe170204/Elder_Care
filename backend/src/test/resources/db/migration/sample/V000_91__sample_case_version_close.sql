-- 000 FR-004 (chủ dự án chốt 2026-10-19): đổi thuộc tính theo phiên bản thì đóng phiên bản đang mở ở ngày trước ngày
-- hiệu lực mới (EffectiveVersioning.addVersionFrom). Tài khoản ứng dụng chỉ sửa được cột effective_to; các cột khác
-- của phiên bản đã có vẫn không sửa được.
GRANT UPDATE (effective_to) ON sample_case_version TO 'eldercare_app'@'%';
