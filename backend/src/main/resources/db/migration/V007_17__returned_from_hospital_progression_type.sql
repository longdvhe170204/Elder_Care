-- Diễn biến hệ thống "Đã về viện": lệnh Ghi nhận trở về (001) của người cao tuổi đang Điều trị tại bệnh viện ghi một dòng vào
-- sự cố còn mở đã chuyển viện, để người xử lý biết cụ đã về (đề xuất 2026-11-23 lượt 3). Không đổi trạng thái sự cố.
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES
    ('M05.PROGRESSION_TYPE', 'M05', 'RETURNED_FROM_HOSPITAL', 'Đã về viện', 17, 'ACTIVE',
     JSON_OBJECT('group', 'SYSTEM', 'resuscitation', FALSE));
