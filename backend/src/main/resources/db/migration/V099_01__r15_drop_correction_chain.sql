-- Gộp bảng về 79 (research R-15 của 000, chủ dự án yêu cầu 2026-11-13): bỏ bảng kỹ thuật correction_chain.
-- Số thứ tự và Hủy ghi nhận của chuỗi đọc từ correction (sequence_no lớn nhất, có dòng type = 'VOID'); các lần đính
-- chính trên cùng bản gốc tuần tự bằng dòng technical_lock 'M15.CORRECTION:<loại>:<id bản gốc>'. Không có dữ liệu
-- cần chép: mọi dòng correction_chain đều suy ra được từ correction. Cú pháp dùng được ở MySQL 8.0 và 8.4.
REVOKE INSERT, UPDATE ON correction_chain FROM 'eldercare_app'@'%';
REVOKE DELETE ON correction_chain FROM 'eldercare_retention'@'%';
DROP TABLE correction_chain;
