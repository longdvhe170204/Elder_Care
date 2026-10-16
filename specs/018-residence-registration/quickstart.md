# Quickstart: Kiểm chứng feature 018

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-018.yaml](contracts/api-018.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 017; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu: Hành chính HC; Quản lý viện QL; Điều dưỡng DD; Kế toán KT. A nội trú dài hạn 01/10/2026 → 30/09/2027; B nội trú ngắn ngày 14 ngày; C bán trú; E nội trú 14 ngày; F nội trú dài hạn thường trú cùng phường với viện.
- CFG theo mặc định Phụ lục 25: CFG-M02-09 1 ngày, CFG-M02-11 30 ngày, CFG-M02-12 1 ngày, CFG-M02-13 30 ngày.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
```

## 3. Kịch bản kiểm chứng (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | Hoàn tất tiếp nhận A ngày 01/10 | HC nhận nhắc; danh sách "cần lập" có A, hạn 02/10; chưa có khai báo | US1 #1, SC-001 |
| Q2 | HC lập khai báo cho A ngày 03/10 | Đăng ký tạm trú, Cần khai báo, hạn vẫn 02/10 | US1 #1, FR-002 |
| Q3 | Hoàn tất tiếp nhận B, lập khai báo; C lập khai báo | B: Thông báo lưu trú; C: 409 `KHONG_PHAI_NOI_TRU`, không có nhắc | US1 #2, #3 |
| Q4 | Hết 02/10 chưa ghi nộp | HC nhắc mỗi ngày; QL báo một lần | US1 #4 |
| Q5 | Hoàn tất tiếp nhận khi chưa có khai báo; Kết thúc lưu trú khi khai báo chưa xong | Không bị chặn | US1 #5, SC-004 |
| Q6 | HC xác nhận F thường trú cùng phường | Loại → Thông báo lưu trú, hạn giữ; gia hạn sau vẫn Thông báo lưu trú | US1 #6, Q-215 |
| Q7 | Ghi đã nộp; ghi chấp nhận thiếu ngày hết hạn; đủ | Đã nộp, dừng nhắc; 409 `THIEU_NGAY_HET_HAN`; Đã xác nhận | US2 |
| Q8 | Ghi chấp nhận khi chưa ghi nộp | 409 `CHUA_GHI_NOP` | US2 #4 |
| Q9 | Ghi từ chối | Về Cần khai báo; hạn mới = lúc ghi + 1 ngày | US2 #3 |
| Q10 | DD, KT gọi lệnh | 403 | US2 #6 |
| Q11 | 31/08/2027 (CFG-M02-13) | HC nhắc lập khai báo gia hạn mỗi ngày | US3 #1 |
| Q12 | Gia hạn Đã xác nhận | Khai báo cũ Được thay thế; đúng một Đã xác nhận | US3 #2, SC-003 |
| Q13 | Qua hết hạn không có gia hạn Đã xác nhận | Hết hạn; HC nhắc lập; QL báo; không có khai báo tự tạo | US3 #3 |
| Q14 | Phụ lục gia hạn B lên 60 ngày | HC nhắc; lập → Đăng ký tạm trú | US3 #4, BR-M02-14 |
| Q15 | Lập khai báo thứ hai khi có khai báo Đã nộp | 409 `DA_CO_KHAI_BAO_DANG_XU_LY` | US3 #5, DBR-33 |
| Q16 | Phụ lục gia hạn E lên 21 ngày | HC nhắc; lập → Thông báo lưu trú; khi Đã xác nhận, cũ Được thay thế | US3 #6 |
| Q17 | Chuyển người bán trú sang nội trú ngắn ngày (phụ lục) | HC nhắc lập; tổng thời gian tính từ ngày hiệu lực phần nội trú | FR-001a, Q-289 |
| Q18 | A Kết thúc lưu trú khi có khai báo Đã xác nhận | A "cần khai báo xóa", HC nhắc mỗi ngày; ghi đã khai báo xóa (hồ sơ trạng thái cuối) → Đã xóa | US4 #1, #2, #4 |
| Q19 | B qua đời khi khai báo Đã nộp | Khai báo Đã hủy; không nhắc khai báo xóa | US4 #3 |
| Q20 | Hai hợp đồng có khoảng trống 1 ngày | Tổng thời gian tính lại từ hợp đồng sau | Q-241 |

## 4. Nhóm test tự động

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `TongThoiGianLuuTruTest`, `LoaiKhaiBaoTest` | Q3, Q14, Q16, Q17, Q20 |
| Khai báo | `BR_M02_11_KhaiBaoIT`, `BR_M02_12_GiaHanIT`, `BR_M02_13_KhaiBaoXoaIT`, `BR_M02_14_ChuyenLoaiIT`, `DBR_33_MotKhaiBaoIT` | Q1 → Q19 |
| Bộ lập lịch | `ChayBuM02KhaiBaoIT` (không nhắc trùng khi chạy lại) | Q4, Q11, Q13 |
| Kiến trúc | ArchUnit: gói `..lich..` không tạo khai báo (Q-306) | |

## 5. Tiêu chí hoàn tất

| SC | Cách đo |
| --- | --- |
| SC-001 | Q1 → Q3 |
| SC-002 | Q4, Q11, Q13 với đồng hồ giả lập |
| SC-003 | Q12, Q15 |
| SC-004 | Q5 |
| SC-005 | Đo thao tác ghi nộp, ghi kết quả |
