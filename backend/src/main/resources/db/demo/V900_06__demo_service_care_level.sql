-- Dữ liệu demo (chỉ profile demo; 2026-11-19): sửa mức chăm sóc của dịch vụ demo 9302 "Chăm sóc thường xuyên" do
-- V900_05 nạp: mã mức chăm sóc của người cao tuổi là REGULAR (001 CareLevel), V900_05 ghi nhầm FREQUENT (mã của phần
-- phòng, giường) nên hợp đồng mức Thường xuyên không chọn được dịch vụ chăm sóc tương ứng. Sửa bằng migration mới, không
-- sửa V900_05: V900_05 đã chạy trên máy phát triển và bản demo Docker (image develop), sửa thẳng thì Flyway báo lệch
-- checksum mà dữ liệu cũ vẫn sai. Chạy lại không đổi gì. Cú pháp dùng được ở MySQL 8.0 và 8.4.
--
-- Tên tiếng Việt của từng loại danh mục V900_05 nạp (cột catalog_type giữ mã trong mã nguồn, ứng dụng tìm theo mã này):
--   M01.ALLERGEN             Dị nguyên                        (Hồ sơ người cao tuổi)
--   M07.MEDICATION           Danh mục thuốc                   (Quản lý thuốc)
--   M02.SERVICE              Dịch vụ, kèm bảng đơn giá        (Tiếp nhận và lưu trú; Chi phí phát sinh)
--   M04.TASK_TYPE            Loại công việc chăm sóc          (Chăm sóc hằng ngày)
--   M04.RESULT_VALUE         Giá trị kết quả chăm sóc         (Chăm sóc hằng ngày)
--   M04.ACTIVITY_TYPE        Loại hoạt động                   (Hoạt động và chất lượng chăm sóc)
--   M08.DIET                 Chế độ ăn                        (Dinh dưỡng)
--   M08.FORBIDDEN_FOOD_TYPE  Loại đồ ăn bị cấm                (Dinh dưỡng)
--   M12.STORAGE_LOCATION     Vị trí lưu giữ đồ dùng cá nhân   (Đồ dùng cá nhân)
--   M06.FACILITY_CONTACT     Số liên hệ của cơ sở             (Cảnh báo sức khỏe và sự cố)
--   M03.DAY_CARE_REST_AREA   Khu nghỉ bán trú                 (Phòng, giường và phân bổ)
UPDATE catalog_item
SET attributes = JSON_SET(attributes, '$.careLevelCode', 'REGULAR')
WHERE id = 9302
  AND catalog_type = 'M02.SERVICE'
  AND JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.careLevelCode')) = 'FREQUENT';
