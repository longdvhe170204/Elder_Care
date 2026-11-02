-- SimpleRecordApi.listByStatus (đổi api 2026-10-30, chủ dự án duyệt; 002 T098): danh sách theo loại và trạng thái không
-- gắn người cao tuổi hay đối tượng cha (bản ghi chờ xem lại, lỗi dữ liệu quyền đang mở). Cú pháp có ở MySQL 8.0 và 8.4.
CREATE INDEX ix_simple_record_status ON simple_record (kind, status);
