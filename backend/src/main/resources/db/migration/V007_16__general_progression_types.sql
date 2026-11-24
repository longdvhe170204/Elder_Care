-- Loại diễn biến chung để người xử lý ghi được việc không nằm trong danh sách có sẵn và vẫn đủ điều kiện Chuyển theo dõi
-- (cần một diễn biến đánh giá và một diễn biến xử lý, BR-M05-20). Không phải biện pháp hồi sức nên không cần kiểm nguyện
-- vọng (DBR-19). Nội dung cụ thể ghi ở ô nội dung của diễn biến.
INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
VALUES
    ('M05.PROGRESSION_TYPE', 'M05', 'FIRST_AID', 'Sơ cứu, chăm sóc tại chỗ', 14, 'ACTIVE',
     JSON_OBJECT('group', 'ACTION', 'resuscitation', FALSE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'OTHER_ACTION', 'Xử lý khác (ghi rõ)', 15, 'ACTIVE',
     JSON_OBJECT('group', 'ACTION', 'resuscitation', FALSE)),
    ('M05.PROGRESSION_TYPE', 'M05', 'OTHER_ASSESSMENT', 'Đánh giá khác (ghi rõ)', 16, 'ACTIVE',
     JSON_OBJECT('group', 'EVALUATION', 'resuscitation', FALSE));
