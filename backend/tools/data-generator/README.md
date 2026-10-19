# Công cụ sinh dữ liệu (000 T101)

Sinh nhật ký ở quy mô NFR-01 với 1 năm dữ liệu (khoảng 22 triệu dòng, research R-06) để đo SC-006: tra cứu nhật ký
toàn viện và lịch sử một đối tượng vẫn nhanh khi bảng lớn (quickstart mục 5).

- Chỉ chạy trên schema của profile `demo` hoặc schema đo riêng. `audit_log` chỉ ghi thêm: dòng đã sinh không xóa được
  bằng tài khoản ứng dụng, muốn bỏ thì tạo lại schema.
- Người thực hiện là tài khoản 1000 → 1099 (100 nhân viên), người cao tuổi 1 → 300; mọi dòng có
  `details.generated = true`. 20% dòng là `CORRECTION_ADDED` có lý do.

```sh
mysql -u <tài khoản> -p elder_care < backend/tools/data-generator/generate-audit-log.sql
mysql -u <tài khoản> -p elder_care -e "CALL generate_audit_log(365, 60000)"
```

Tham số: số ngày lùi từ hôm nay và số dòng mỗi ngày. Chạy thử nhỏ trước, ví dụ `CALL generate_audit_log(2, 1000)`.
Thử trên cloud (MySQL 8.0.46): 75 000 dòng trong khoảng 4 giây, nên 22 triệu dòng mất cỡ 20 phút; máy chủ dự án có thể khác.

Đo SC-006 (quickstart mục 5, 10 lần lấy trung vị): `GET /api/v1/audit-logs` không lọc và lọc theo `actorId`, `action`,
khoảng thời gian; `GET /api/v1/audit-logs/entities/M07.MEDICATION_DOSE/<id>`; trang kế tiếp theo `nextCursor`.
