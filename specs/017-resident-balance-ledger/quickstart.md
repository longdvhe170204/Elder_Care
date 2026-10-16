# Quickstart: Kiểm chứng feature 017

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-017.yaml](contracts/api-017.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 015: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản: Kế toán KT, KT2; Quản lý viện QL; Hành chính HC; Điều dưỡng DD; người đại diện R (có quyền xem chi phí), S (không có quyền xem chi phí) của A;
  - A nội trú giá tháng 9.300.000 đồng, khoản cọc 10.000.000 đồng; E bán trú giá buổi 150.000 đồng, lịch thứ 2, 4, 6;
  - cấu hình triển khai: số tài khoản thu và mã BIN ngân hàng của viện;
  - CFG theo mặc định Phụ lục 25: CFG-M11-04 15 ngày, CFG-M11-05 7 ngày, CFG-M02-09 1 ngày, CFG-M13-06 07:30–17:00 thứ 2 → thứ 7, CFG-M14-01 12 tháng, CFG-M15-05 48 giờ, CFG-M15-06 96 giờ;
  - file sao kê mẫu 200 dòng (180 dòng ghi đúng mã);
  - bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}`: `ChiPhiChoSoDuPort` (010), sự kiện 004, `QuyenNguoiThanApi` (012).

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
web/      npm ci && npm test && npm run dev
mobile/  flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()`, ArchUnit (chỉ `m11.sodu` dùng port P-12), test cơ sở dữ liệu (data-model mục 7) và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | 004 chuyển hợp đồng A sang Chờ ký | Sổ số dư, sổ cọc mở với 0; mã nộp tiền cấp; sự kiện lặp không mở sổ thứ hai | US1, FR-001 |
| Q2 | R mở cổng phần số dư; S mở | R thấy số dư, cọc, số tài khoản, payload VietQR không số tiền; S 403 | US1, FR-003, SC-010 |
| Q3 | DD, Bác sĩ gọi API số dư | 403 | FR-004 |
| Q4 | KT ghi phiếu thu cọc 10.000.000 tiền mặt | Giao dịch Đã xác nhận, số phiếu T-000001; đặt cọc Đã đáp ứng; 004 nhận `TrangThaiDatCocDaDoi` | US2, FR-005, FR-006 |
| Q5 | KT và KT2 ghi phiếu thu cùng lúc; một lệnh lỗi rollback | Số phiếu liên tục, không bỏ số | FR-005, SC-011 |
| Q6 | Hết giờ hành chính chưa lập chốt quỹ; KT lập với chênh lệch −50.000 không lý do; có lý do | Nhắc KT; 409 `THIEU_LY_DO_CHENH_LECH`; Đã lập, QL được báo | US2, FR-005a |
| Q7 | QL trả lại; KT lập lại; QL xác nhận; sau đó hoàn tiền tiền mặt được duyệt cho ngày đó | Bị trả lại → chốt mới Đã xác nhận; giao dịch mới mang dấu "phát sinh sau chốt quỹ", KT được nhắc | FR-005a |
| Q8 | KT nhập file sao kê 200 dòng; nhập lại 2 lần | 180 dòng Đã khớp, 20 Chưa khớp kèm lý do; lần nhập lại: 0 dòng mới, báo "đã nhập trước đó" | US3, FR-007, FR-008, SC-003, SC-004 |
| Q9 | File thiếu cột "mã giao dịch" | 422; không ghi dòng nào | FR-007 |
| Q10 | KT xác nhận dòng khớp A vào số dư; xác nhận lần hai | Giao dịch Nộp chuyển khoản; lần hai 409 `DONG_DA_SINH_GIAO_DICH` | US3, FR-009, SC-013 |
| Q11 | Dòng 15.000.000 gồm 10.000.000 cọc và 5.000.000 số dư | Xác nhận vào cọc; lập cặp chuyển tiền 5.000.000 cọc → số dư; QL duyệt: hai giao dịch Đã xác nhận cùng lúc | FR-009a, SC-013 |
| Q12 | 010 chốt bảng A tổng 9.300.000; sự kiện gửi lặp | Một giao dịch Thanh toán bảng −9.300.000 | US4, FR-012, SC-002 |
| Q13 | KT lập đảo giao dịch thanh toán bảng | 409 `KHONG_DAO_THANH_TOAN_BANG` | FR-011 |
| Q14 | KT lập hoàn tiền 2.000.000 cho R khi số dư − tổng chưa chốt = 1.500.000 | 409 `VUOT_GIOI_HAN_HOAN` | US6, FR-014 |
| Q15 | Hoàn tiền 1.000.000 cho R; trước khi duyệt R thôi làm người đại diện; QL duyệt | Duyệt bị chặn `NGUOI_NHAN_KHONG_CON_DAI_DIEN`; KT được báo | US6, FR-014, SC-007 |
| Q16 | KT lập đảo phiếu thu; lập đảo thứ hai cho cùng gốc | Chờ duyệt; lần hai 409 `DA_CO_GIAO_DICH_DAO` | FR-011 |
| Q17 | Đêm: A số ngày còn đủ tiền 10 | Tình trạng Sắp hết tiền; R nhận Trung bình (phần chung + chi phí), S nhận phần chung; KT nhận Nhẹ kèm danh sách | US5, FR-016 → FR-018, SC-005 |
| Q18 | 7 ngày sau vẫn Sắp hết tiền; sau 3 ngày khác chuyển Còn nợ | Nhắc lại ngày thứ 7; Còn nợ báo ngay, đặt lại chu kỳ | FR-018 |
| Q19 | KT ghi phiếu thu đưa A về Bình thường | Tính lại ngay; R nhận "đã ghi nhận khoản nộp, số dư đã đủ" | Q-252 |
| Q20 | Phiếu thu chưa đủ để thoát Còn nợ | R nhận "đã ghi nhận khoản nộp" kèm tình trạng; chu kỳ giữ nguyên | Q-257 |
| Q21 | Đảo phiếu thu ở Q19 | Tình trạng tính lại, xấu đi → báo ngay; không có thông báo thu hồi | Q-268 |
| Q22 | Còn nợ liên tục 7 ngày | QL nhận Nhẹ ở mỗi lần nhắc | FR-019 |
| Q23 | 010 gọi `soDuKhongDu` cho đề nghị 2.000.000 khi số dư khả dụng 1.000.000 | Dấu "số dư không đủ"; sau phiếu thu tính lại không còn dấu | FR-021 |
| Q24 | Lệnh chăm sóc, thuốc, suất ăn của A khi Còn nợ | Không bị chặn | FR-020, SC-006 |
| Q25 | E bán trú giá buổi | Phí một ngày = 150.000 × số buổi 7 ngày tới ÷ 7 | FR-016, Q-223 |
| Q26 | A có hồ sơ kết thúc; bảng kỳ cuối đã chốt; KT cấn trừ cọc vượt phần nợ | 409; đúng số → hai giao dịch liên kết | US7, FR-015 |
| Q27 | Kết thúc lưu trú A khi số dư còn 200.000 | Điều kiện `KT_H_QUYET_TOAN` Chưa đạt kèm lý do; hoàn tiền xong → Đạt; 004 nhận `DieuKienQuyetToanDaDoi` | US7, FR-022, SC-008 |
| Q28 | Bảng bổ sung chưa chốt khi xét điều kiện | Không làm Chưa đạt | FR-022, Q-134 |
| Q29 | Chuyển khoản tới sau khi A đã Kết thúc lưu trú | Ghi bình thường; KT được nhắc mỗi ngày tới khi số dư về 0 | FR-024 |
| Q30 | A qua đời, sổ có giao dịch | Mục `QUYET_TOAN` theo điều kiện; hồ sơ chưa từng có giao dịch khác 0 → Không áp dụng | FR-023 |
| Q31 | Hợp đồng B bị hủy trước Hiệu lực khi có cọc | KT được nhắc hoàn cọc mỗi ngày | FR-015a |
| Q32 | QL ghi "dừng nhắc" cho hồ sơ còn nợ chưa ở trạng thái cuối | 409 `KHONG_PHAI_TRANG_THAI_CUOI` | FR-020 |
| Q33 | KT xuất sao kê 3 tháng của A; xuất 13 tháng | Đầu kỳ + tổng = cuối kỳ; 422 `VUOT_KHOANG_XUAT` | US8, FR-025, SC-009 |
| Q34 | R tải sao kê qua cổng | Tệp của A; lần xuất được ghi | FR-026, FR-027 |
| Q35 | Chạy lại job tình trạng cùng ngày | Không gửi thông báo trùng | SC-012 |

## 4. Nhóm test tự động

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `TrangThaiDatCocTest`, `KhopMaTest`, `GioiHanHoanTest`, `PhiMotNgayTest`, `TinhTrangTest`, `QuyetDinhBaoTest`, `DieuKienQuyetToanTest`, `TongQuyNgayTest`, `SaoKeTest` | Q4, Q8, Q14, Q17 → Q22, Q25, Q27, Q33 |
| Sổ, thu | `DBR_28_SoDuIT`, `BR_M11_16_ChotQuyIT`, `DBR_35_SoPhieuIT` | Q1, Q4 → Q7 |
| Sao kê | `BR_M11_12_DoiSoatIT`, `DBR_29_DongSaoKeIT` | Q8 → Q11 |
| Giao dịch | `DBR_30_ThanhToanBangIT`, `BR_M11_10_GiaoDichDuyetIT`, `BR_M11_14_HoanTienIT` | Q12 → Q16 |
| Tình trạng | `BR_M11_13_TinhTrangIT`, `BR_M11_15_MuaHoIT` | Q17 → Q25 |
| Quyết toán | `FR_022_QuyetToanIT`, `FR_024_SauTrangThaiCuoiIT` | Q26 → Q31 |
| Xuất | `FR_025_XuatIT` | Q33, Q34 |
| SC-001 | Bộ 3 tháng × 50 hồ sơ | |
| Đồng thời | Hai phiếu thu cùng lúc; duyệt hoàn tiền cùng lúc với thanh toán bảng | Q5 |
| Bộ lập lịch | `ChayBuM11SoDuIT` | Q35 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit port P-12; gói `..lich..` không tạo giao dịch ngoài thanh toán bảng (kèm sự kiện chốt) | |
| Web (Vitest) | Màn đối soát; màn duyệt hiển thị giới hạn tính lại | Q8, Q15 |
| App (`flutter_test`) | Cổng số dư ẩn khi không có quyền; hiện mã QR | Q2 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | Bộ 3 tháng × 50 hồ sơ so tính tay |
| SC-002 | Q12 |
| SC-003 | Q8 |
| SC-004 | Q8 và đo đợt thử đối soát ≤ 30 phút |
| SC-005 | Q17, Q18 |
| SC-006 | Q24 |
| SC-007 | Q14 → Q16 |
| SC-008 | Q27 |
| SC-009 | Q33 |
| SC-010 | Q2, Q3 |
| SC-011 | Q5 → Q7 |
| SC-012 | Đo job 300 hồ sơ ≤ 5 phút; Q35 |
| SC-013 | Q10, Q11, Q26 |
