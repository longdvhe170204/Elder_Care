# Quickstart: Kiểm chứng feature 016

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-016.yaml](contracts/api-016.yaml), [contracts/module-interfaces.md](contracts/module-interfaces.md), [contracts/chi-tieu.md](contracts/chi-tieu.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 019; profile `demo` có đồng hồ giả lập.
- Bộ dữ liệu báo cáo dựng sẵn (`demo-bao-cao`): 3 tầng, 60 người cao tuổi, 30 ngày dữ liệu của mọi module, với bảng kết quả tính tay cho mọi chỉ tiêu ở [chi-tieu.md](contracts/chi-tieu.md), gồm trường hợp mẫu số 0, bản ghi Hủy ghi nhận, bản ghi chờ xem lại, khoảng có dữ liệu bị loại bỏ theo lưu trữ.
- Bộ 11 tài khoản kiểm thử quyền của User Story 2: Quản lý viện; Trưởng tầng T2; Trưởng tầng giao tạm T3; Trưởng tầng đã thôi giao T1; Bác sĩ; Bác sĩ kiêm Trưởng tầng; Điều dưỡng có 2 ca; Người phụ trách ca; Hành chính; Hành chính kiêm Điều dưỡng; Kế toán.
- Bản giả mọi `*ThongKeApi` cho test M14 độc lập; bản thật cho test đầu–cuối.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo,demo-bao-cao
frontend/ npm ci && npm test && npm run dev
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (M14 chỉ phụ thuộc `*.api`), ArchUnit "M14 chỉ đọc" (research O-09).

## 3. Kịch bản kiểm chứng (profile demo-bao-cao)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | Quản lý viện mở dashboard | Mọi khối của cột QL; "tính đến"; mở ≤ 3 giây | US1, FR-020, SC-002 |
| Q2 | Ghi một công việc Quá hạn mới trong lúc dashboard mở | Số mới hiện ≤ 1 phút (làm mới 30 giây, ETag) | FR-021, SC-002 |
| Q3 | Trưởng tầng T2 mở dashboard | Chỉ tầng 2; không có danh sách chờ, chi phí, giấy phép cơ sở | US2, FR-010 |
| Q4 | Điều dưỡng ngoài giờ ca ± CFG-M15-07 mở dashboard | "hiện không có phạm vi phân công" | FR-020 |
| Q5 | Người phụ trách ca trong ca mở dashboard | Nhóm của cột ĐD cho cả tầng/khu của ca; số mục kiểm tra còn chờ của ca; không có nhóm chất lượng | FR-012, Q-195, Q-263 |
| Q6 | Hành chính mở báo cáo sức khỏe; xem sự cố | 403 với chỉ số, thuốc; chỉ sự cố loại "không thuộc sức khỏe", không có mô tả | FR-014, FR-042a, SC-012 |
| Q7 | Đổi loại một sự cố sang loại không mang dấu | Rời khỏi số liệu của Hành chính ở lần lấy kế tiếp | FR-042a |
| Q8 | Kế toán mở báo cáo chăm sóc; chi phí | 403; được với chi phí và số dư | FR-010 |
| Q9 | Bộ lọc tầng của T2 | Chỉ tầng 2; không có tổng toàn viện | FR-013, SC-004 |
| Q10 | T3 (giao tạm) xem báo cáo tháng trước của tầng 3 | Gồm bản ghi trước khi được giao; T1 (đã thôi) không còn xem được | FR-012, Q-192, SC-011 |
| Q11 | Điều dưỡng có 2 ca xem báo cáo tháng | Chỉ bản ghi của phân công trong 2 ca ± CFG-M15-07; ca bị ghi vắng không tính | FR-012, SC-011 |
| Q12 | Mở chi tiết "Công việc quá hạn" | Số dòng = con số; mở bản ghi chỉ xem ở 005 | US7, FR-009, SC-006 |
| Q13 | Mở chi tiết có người đã chuyển tầng | Dòng rút gọn (họ tên, phòng lúc phát sinh, nội dung); không mở được hồ sơ hiện tại | FR-012, Q-196 |
| Q14 | Báo cáo tỷ lệ đúng hạn công việc Bắt buộc | Khớp tính tay; Không thực hiện do Hệ thống đếm riêng | US4, FR-031, SC-001 |
| Q15 | Báo cáo chất lượng của T2 | Tỷ lệ Đạt theo nhân viên; công việc của T2 ghi "ngoài phạm vi kiểm tra"; số mục theo vai trò kiểm tra | FR-037 |
| Q16 | Báo cáo cảnh báo | Thời gian tạo → tiếp nhận theo mức lúc tiếp nhận; cảnh báo gộp đếm một | US5, FR-041 |
| Q17 | Báo cáo liều | Tỷ lệ Bỏ lỡ, Từ chối khớp 006 FR-051 | FR-043 |
| Q18 | Báo cáo chi phí chế độ "đã chốt" tháng 9 | Khớp tổng bảng Đã chốt và dòng tổng kiểm soát file kế toán; ghi chú "không phải báo cáo tài chính" | US6, FR-050, FR-055, SC-007 |
| Q19 | Chế độ "tạm tính"; khoản điều chỉnh sau chốt | Nhãn tạm tính; khoản điều chỉnh xếp theo kỳ chứa kèm kỳ gốc | FR-051, FR-052 |
| Q20 | Hành chính xem báo cáo chi phí theo thuốc | Chỉ mã vật phẩm | FR-054, SC-005 |
| Q21 | Báo cáo nhân sự của T2 | Tỷ lệ phục vụ lần tính cuối kèm "từng không đạt"; yêu cầu đổi ca, nghỉ đột xuất theo trạng thái cuối (ca thiếu phủ bỏ theo Q-397); không có lý do nghỉ | US8, FR-070 → FR-072 |
| Q22 | Người phụ trách ca xem giờ làm theo nhân viên | 403 | FR-073 |
| Q23 | Chọn khoảng 13 tháng | 422 `VUOT_KHOANG_TOI_DA` | FR-060, SC-003 |
| Q24 | Báo cáo 12 tháng toàn viện | ≤ 30 giây | SC-003 |
| Q25 | QL xuất danh sách có định danh không nhập mục đích; có mục đích | 422 `THIEU_MUC_DICH`; tệp + bản ghi nhật ký | US9, FR-061, FR-062, SC-013 |
| Q26 | T2 xuất báo cáo tỷ lệ liều theo người | Chỉ tổng hợp, không có định danh, không có nhóm một người | FR-061, SC-010 |
| Q27 | Giả lập lỗi ghi nhật ký khi xuất | Không giao tệp | FR-062, SC-008 |
| Q28 | So tệp xuất với màn hình | Cùng số, cùng tổng | SC-010 |
| Q29 | Chạy toàn bộ bộ kiểm thử báo cáo | Số bản ghi nghiệp vụ, thông báo không đổi; nhật ký tăng đúng số lần xuất | SC-008 |
| Q30 | Báo cáo khoảng có dữ liệu đã loại bỏ theo lưu trữ | Nhãn "số liệu không đầy đủ…" kèm loại dữ liệu | FR-007 |
| Q31 | So số giường theo trạng thái với 003 cùng thời điểm | Khớp 100% | SC-014 |
| Q32 | 002 không xác định được phạm vi của người xem | 403 `KHONG_XAC_DINH_PHAM_VI`; QL được báo | FR-016 |

## 4. Nhóm test tự động

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Chỉ tiêu | `ChiTieu_*Test` cho mọi mã ở chi-tieu.md (SC-001) | Q14 → Q21 |
| Phạm vi | `PhamViBaoCaoTest`, `FR_012_PhamViTheoThoiGianIT` (SC-011), bộ 11 tài khoản (SC-004) | Q3 → Q11 |
| Giới hạn trường | `FR_014_GioiHanTruongIT`, `FR_042a_SuCoHanhChinhIT`, `SC_005_TenThuocIT` | Q6, Q7, Q20 |
| Chi tiết | `FR_009_ChiTietKhopSoIT` (SC-006) | Q12, Q13 |
| Chi phí | `SC_007_KhopBangChotIT` | Q18, Q19 |
| Xuất | `FR_061_NoiDungXuatTest`, `FR_062_NhatKyXuatIT` | Q25 → Q28 |
| Chỉ đọc | `SC_008_ChiDocIT` | Q29 |
| Hiệu năng | Dashboard 60 người dùng (SC-002), báo cáo 12 tháng (SC-003) | Q1, Q24 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit cấm gọi API lệnh | |
| Web (Vitest) | Định nghĩa chỉ tiêu tại chỗ; "—" khi mẫu số 0; làm mới 30 giây với ETag | Q1, Q2 |

## 5. Tiêu chí hoàn tất

| SC | Cách đo |
| --- | --- |
| SC-001 | Bộ dữ liệu tính tay |
| SC-002 | Q1, Q2 |
| SC-003 | Q23, Q24 |
| SC-004 | Bộ 11 tài khoản |
| SC-005 | Q20 |
| SC-006 | 20 chỉ tiêu ngẫu nhiên, Q12 |
| SC-007 | Q18 |
| SC-008 | Q27, Q29 |
| SC-009 | Đợt dùng thử với Quản lý viện, trưởng tầng |
| SC-010 | Q26, Q28 |
| SC-011 | Q10, Q11 |
| SC-012 | Q6, Q7 |
| SC-013 | Q25 |
| SC-014 | Q31 |

## 6. Kịch bản bổ sung 2026-10-18, lượt 3 (Q-369)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| B1 | Dữ liệu tháng 10: 12 cảnh báo SOS Tầng 2 (1 chưa tiếp nhận), thời gian đã biết; xem báo cáo cảnh báo | Dòng SOS: 12 cảnh báo; trung bình, lớn nhất trên 11 cảnh báo đã tiếp nhận; 12 cảnh báo vẫn trong tổng Khẩn cấp | US5 #6, FR-041 |

Test: `ChiTieu_SK_CanhBaoSosTest`.

## 7. Kịch bản bổ sung 2026-10-22 (Q-374, Q-375)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| B2 | Tháng 10: 12 cảnh báo SOS Tầng 2 (10 có mốc tới nơi), 3 cảnh báo nút hỗ trợ Tầng 2, 1 cảnh báo nút hỗ trợ chưa có tầng; xem báo cáo cảnh báo | Dòng SOS và dòng nút hỗ trợ riêng; thời gian tạo → tới nơi trung bình, lớn nhất trên cảnh báo có mốc; "không ghi mốc tới nơi" của SOS là 2; dòng "chưa rõ tầng" có 1 cảnh báo; mọi cảnh báo vẫn trong tổng Khẩn cấp | US5 #7, FR-041 |

Test: `ChiTieu_SK_CanhBaoSosHoTroTest` (thay `ChiTieu_SK_CanhBaoSosTest`, giữ ca B1).
