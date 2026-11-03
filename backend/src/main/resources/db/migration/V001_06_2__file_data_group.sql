-- Audit F-04, R-15: khai nhóm dữ liệu bảng file (common.file, tạo ở V001_06_1). Bảng kỹ thuật giữ siêu dữ liệu tệp:
-- tệp vừa tải lên chưa có chủ sở hữu, lệnh nghiệp vụ gắn một lần (trg_file_attached_immutable), tệp chưa gắn bị dọn sau
-- 24 giờ. Chủ dự án chốt 2026-11-03 (team-plan, dòng V001_95 điểm 2): A khai, không đợi V001_95.
-- Số V001_06_2 để chạy sau V001_06_1 (tạo bảng) trên schema mới; thấp hơn migration đã chạy trên schema dev: profile
-- demo đã bật out-of-order, profile khác xem team-plan mục 8 dòng "Ghi nhớ migration số nhỏ đến sau".
ALTER TABLE file COMMENT = 'DataGroup=TECHNICAL';
