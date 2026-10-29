# Quickstart: Kiểm chứng feature 017

**Ngày**: 2026-10-22 (lập lại theo spec viết lại) | **Plan**: [plan.md](plan.md)

Hướng dẫn kiểm chứng ở giai đoạn triển khai. Hợp đồng ở [contracts/api-017.yaml](contracts/api-017.yaml), bảng ở [data-model.md](data-model.md); tài liệu này không chép lại.

## 1. Điều kiện cần

- Như quickstart của 000: JDK 25, MySQL 8.0 (000 R-14), Node 24, Flutter stable; biến `TEST_DB_*`; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản: Kế toán KT, KT2; Quản lý viện QL; Hành chính HC; Điều dưỡng DD; người đại diện R (có quyền xem chi phí), người thân N (không có quyền xem chi phí) của A;
  - A nội trú giá tháng 9.000.000 đồng, khoản cọc 20.000.000 đồng; E bán trú giá buổi 150.000 đồng, lịch thứ 2, 4, 6;
  - CFG-M11-10 (tài khoản thu, mã BIN), CFG-M11-11 (tên, địa chỉ, mã số thuế, điện thoại viện); CFG theo mặc định Phụ lục 25: CFG-M11-04 15 ngày, CFG-M11-05 7 ngày, CFG-M02-09 1 ngày, CFG-M14-01 12 tháng, CFG-M15-05 48 giờ, CFG-M15-06 96 giờ;
  - bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}`: `ChiPhiChoSoDuPort` (010, gồm `bangDaChot`), sự kiện và `HoSoKetThucDocApi` (004), `QuyenNguoiThanApi` (012); `S3Client` bản giả trong bộ nhớ (001).

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()`, ArchUnit (chỉ `m11.sodu` dùng port P-12), test cơ sở dữ liệu (data-model mục 9) và nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | Bản giả 004 phát `HopDongDaDoiTrangThai` → Chờ ký cho A; phát lại lần hai; trả về nháp rồi Chờ ký | Một sổ số dư 0, một sổ cọc 0, một mã nộp tiền; không mở thêm | US1 #1, #2, FR-001 |
| Q1a | *(Bổ sung 2026-10-22)* QL mở tham số CFG-M11-10, chọn "Vietcombank" từ danh sách, nhập số tài khoản giả, tên chủ tài khoản; lưu với `maBin` không có trong danh mục; lưu với số tài khoản có chữ cái; xem số dư A | `GET /ngan-hang` trả danh sách còn hiệu lực; 422 hai lần sai; lưu đúng thì thông tin nộp tiền của A có ngân hàng, số tài khoản, chuỗi VietQR chứa `970436`. Trước khi nhập CFG-M11-10, cổng không có mục nộp tiền | FR-003 |
| Q2 | KT, HC, DD xem số dư A; R, N mở cổng | KT, HC thấy (HC chỉ số dư, cọc, đặt cọc); DD 403; R thấy số dư, mã nộp tiền, chuỗi VietQR; N 403 | US1 #3 → #5, FR-003, FR-004 |
| Q3 | KT ghi tiền mặt 10.000.000 đồng vào sổ cọc A; HC thử ghi | Cọc 10.000.000, đặt cọc Chưa đáp ứng; HC 403 | US2 #1, #5 |
| Q4 | KT tìm A theo mã nộp tiền, ghi chuyển khoản 10.000.000 đồng sổ cọc mã NH001 | Cọc 20.000.000, đặt cọc Đã đáp ứng; 004 nhận `TrangThaiDatCocDaDoi` | US2 #2, US3 #1, FR-006 |
| Q5 | Ghi chuyển khoản NH002 hai dòng (cọc 20.000.000, số dư 5.000.000) cho B; ghi khoản khác cho A với NH001 | Hai giao dịch cùng mã, Đã xác nhận; NH001: 409 `MA_GIAO_DICH_DA_DUNG` kèm giao dịch trước; gửi lại `xacNhanTrungMa` thì 201 | US3 #2, #3, FR-007 |
| Q6 | KT ghi tiền mặt 5.000.000 đồng với ghi chú "PT 0123"; ghi số tiền 0 | Đã xác nhận, không có số phiếu; 422 | US2 #4, #6, FR-005 |
| Q7 | Bản giả 010 phát `BangChiPhiDaChot` tháng 10 của A 9.300.000 đồng hai lần; bảng bổ sung −200.000; bảng 0 đồng | Một giao dịch −9.300.000; +200.000; một giao dịch 0 đồng | US4, FR-012, DBR-30 |
| Q8 | Đồng hồ giả lập: A số dư 6.000.000, chưa chốt 2.400.000, phí ngày 300.000; chạy job | 12 ngày, Sắp hết tiền; R nhận Trung bình (phần chi phí), N nhận phần chung; KT nhận Nhẹ | US5 #1, #6, FR-018 |
| Q9 | Chạy job 02/11 → 07/11 và 08/11; số dư âm từ 01/11, chạy 08/11 | Không báo tới 08/11 thì nhắc lại; QL được báo thêm | US5 #2, #4, FR-019 |
| Q10 | A Sắp hết tiền; KT ghi nộp 10.000.000; lần khác A Còn nợ, nộp một phần còn 4 ngày | Bình thường, R nhận "đã ghi nhận khoản nộp, số dư đã đủ"; Sắp hết tiền, R nhận "đã ghi nhận khoản nộp", chu kỳ giữ | US5 #3, #7, Q-252, Q-257 |
| Q11 | KT lập hoàn 3.000.000 khi số dư 2.000.000; lập hoàn 2.000.000 cho R; QL duyệt qua API 000 | 409 `VUOT_GIOI_HAN_HOAN`; Chờ duyệt, số dư không đổi, QL được báo; duyệt → số dư 0 | US6 #1, #2, FR-013, FR-014 |
| Q12 | Hoàn Chờ duyệt 2.000.000; chốt bảng 500.000 trước khi duyệt; duyệt. Hoàn khác cho R, R thôi là người đại diện, duyệt | Hai lần duyệt bị chặn; KT được báo | US6 #3, #8 |
| Q13 | Khoản 5.000.000 ghi nhầm cho A: KT lập điều chỉnh −5.000.000 trỏ giao dịch gốc, lý do; QL duyệt; lập điều chỉnh trỏ thanh toán bảng | Số dư A giảm, gốc mang `daDieuChinh`; trỏ thanh toán bảng 409 | US3 #4, US6 #4, #5, FR-011 |
| Q14 | QL từ chối một điều chỉnh có lý do | Từ chối, số dư không đổi, KT được báo | US6 #6 |
| Q15 | A lưu trú số dư 5.000.000, chưa chốt 4.000.000, lập hoàn 2.000.000; lập 1.000.000 | 409; 201 | US6 #7, FR-014 |
| Q16 | Hồ sơ kết thúc A lập (bản giả 004), kỳ cuối chưa chốt; xem quyết toán | Hai mã Chưa đạt; lý do "bảng kỳ cuối chưa chốt" | US7 #1, FR-022 |
| Q17 | Kỳ cuối chốt, số dư −1.500.000, cọc 20.000.000; KT cấn trừ 1.500.000; lập hoàn cọc 18.500.000, QL duyệt; xem điều kiện; KT xác nhận quyết toán | Cặp cấn trừ; số dư 0, cọc 0; `KT_H_QUYET_TOAN` Đạt, `KT_H_XAC_NHAN_QUYET_TOAN` Chưa đạt "chưa xác nhận quyết toán"; xác nhận → bảng phiên bản 1, hai mã Đạt, 004 nhận `DieuKienQuyetToanDaDoi`, R được báo | US7 #2, #3, US10 #1, FR-015, FR-034 |
| Q18 | A còn nợ 3.000.000, cọc 0; KT xác nhận khi chưa có ngoại lệ; bản giả 004 ngoại lệ đã duyệt; còn một hoàn Chờ duyệt; hủy hoàn đó, xác nhận | 409; 409 `CON_GIAO_DICH_CHO_DUYET` (ý Chờ duyệt không miễn được, BR-M11-22); 201 bảng ghi còn nợ 3.000.000, `coNgoaiLe`; vẫn báo còn nợ | US7 #4, FR-020, FR-034 |
| Q19 | Đã xác nhận, hồ sơ kết thúc chưa Hoàn tất; KT ghi chuyển khoản tới muộn 500.000 | `KT_H_XAC_NHAN_QUYET_TOAN` Chưa đạt; hoàn, xác nhận lại → phiên bản 2 hiện hành, phiên bản 1 vẫn tải được | US10 #4, DBR-38 |
| Q20 | Đính kèm bản ký vào phiên bản hiện hành; đính lần hai; vào phiên bản cũ | 200; 409; 409 | US10 #3 |
| Q21 | Kỳ cuối chưa chốt; KT xác nhận | 409 "bảng kỳ cuối chưa chốt" | US10 #2 |
| Q22 | Bản giả 004 ghi qua đời C; xem mục việc sau qua đời | `QUYET_TOAN` Chưa hoàn thành tới khi ba ý tiền Đạt và đã xác nhận; sổ chưa từng có giao dịch khác 0 → Không áp dụng | US7 #5, US10 #5, FR-023 |
| Q22a | *(Q-387)* C qua đời còn nợ 2.000.000, cọc 0; bản giả 004 trả ngoại lệ đã duyệt; KT xác nhận quyết toán | 201, bảng ghi còn nợ 2.000.000, `coNgoaiLe`; mục `QUYET_TOAN` Hoàn thành; vẫn báo còn nợ tới khi số dư 0 hoặc QL dừng nhắc | US10 #5, FR-023 |
| Q23 | Lệnh Kết thúc lưu trú đã chạy; bảng bổ sung chốt muộn | Giao dịch ghi; KT được nhắc mỗi ngày tới khi số dư 0 và xác nhận lại; hồ sơ không mở lại | US7 #6, FR-024 |
| Q24 | Hợp đồng A hủy trước Hiệu lực khi cọc 20.000.000 | KT được nhắc lập hoàn cọc mỗi CFG-M02-09 tới khi cọc 0 | US2 #7, FR-015a |
| Q25 | Danh sách chờ phát hành; KT phát hành phiếu bảng tháng 10 của A (15.200.000, số dư trước 10.000.000); phát hành lần hai | Có bảng tháng 10; phiếu mã `PBP-202610-<bangId>`, PDF và Excel; số dư sau −5.200.000, số còn nợ, mã nộp tiền, tài khoản, ảnh QR; dòng "Chứng từ này không thay thế hóa đơn"; lần hai 409 `DA_PHAT_HANH` | US9 #1, #2, FR-031, FR-032 |
| Q26 | Bảng có khoản thuốc Amlodipin; xem phiếu PDF, Excel | Khoản ghi "Thuốc" và mã vật phẩm, không có "Amlodipin" | US9 #3, FR-037 |
| Q27 | Sau phát hành: R mở cổng, N mở cổng; tải file hai lần | R nhận Nhẹ, thấy phiếu, tải đúng file (cùng byte); N không nhận, 403 | US9 #4, FR-038, SC-016 |
| Q28 | Bảng bổ sung tháng 11 chứa điều chỉnh tháng 10 chốt; phát hành | Phiếu riêng; phiếu tháng 10 không đổi | US9 #5 |
| Q29 | Xuất sao kê A 01/10 → 31/10 Excel rồi PDF | Đầu kỳ 1.000.000, hai giao dịch, cuối kỳ 1.700.000; PDF cùng số liệu, có thông tin viện | US8 #1, #7, FR-025 |
| Q30 | Xuất báo cáo thu chi tháng 10 (nộp 55.000.000, trừ 48.000.000, hoàn 2.000.000, B nợ 1.500.000) | Ba tổng và danh sách còn nợ gồm B; không có giao dịch Chờ duyệt, Từ chối | US8 #2, #3, Q-386 |
| Q31 | HC xuất báo cáo; xuất khoảng 13 tháng | 403; 422 `VUOT_KHOANG_XUAT` | US8 #4, #6 |
| Q32 | Mỗi lần xuất; dữ liệu đổi rồi tải lại | Có `lan_xuat_so_du` đủ trường; file không đổi | US8 #5, FR-027 *(R-15: `lan_xuat_so_du` → hành động `nhat_ky`)* |
| Q33 | R xuất sao kê trên cổng PDF; N thử | 200 file; 403 | FR-026 |
| Q34 | A Còn nợ: DD phát thuốc, nhân viên ghi công việc, bếp chốt suất | Không lệnh nào bị chặn | US5 #5, FR-020 |
| Q35 | Hồ sơ trạng thái cuối còn nợ; QL dừng nhắc có lý do; chạy job | Không nhắc nữa | FR-020 |
| Q36 | Đề nghị mua hộ 800.000 khi số dư − chưa chốt = 500.000 | 010 thấy dấu "số dư không đủ"; không chặn | FR-021 |

## 4. Test tự động: truy vết kịch bản → test

| Spec | Lớp test (backend, `com.cambae.backend.m11.sodu…`) | Loại |
| --- | --- | --- |
| US1, FR-001 → FR-004 | `MoSoIT`, `VietQrPayloadTest`, `SoDuHienThiIT`, `CongSoDuIT` | Đơn vị, tích hợp |
| US2, US3, FR-005 → FR-007 | `BR_M11_16_TienMatIT`, `BR_M11_12_ChuyenKhoanIT`, `TrangThaiDatCocTest`, `FR_006_DatCocIT` | Đơn vị, tích hợp |
| US4, FR-012 | `DBR_30_ThanhToanBangIT`, `BR_M11_11_ThanhToanBangIT` | Tích hợp |
| US5, FR-016 → FR-021 | `PhiMotNgayTest`, `TinhTrangTest`, `QuyetDinhBaoTest`, `BR_M11_13_TinhTrangIT`, `BR_M11_15_DauMuaHoIT`, `FR_020_KhongChanIT` | Đơn vị, tích hợp, đồng hồ giả lập |
| US6, FR-010 → FR-015a | `GioiHanHoanTest`, `BR_M11_14_HoanTienIT`, `BR_M11_10_DieuChinhIT`, `FR_015_CanTruCocIT`, `FR_015a_NhacHoanCocIT`, `DBR_17_GiaoDichKhongSuaIT` | Đơn vị, tích hợp |
| US7, US10, FR-022 → FR-024, FR-034 | `DieuKienQuyetToanTest`, `FR_022_DieuKienQuyetToanIT`, `BR_M11_22_XacNhanQuyetToanIT`, `FR_023_ViecSauQuaDoiIT`, `FR_024_SauTrangThaiCuoiIT` | Đơn vị, tích hợp |
| US9, FR-031, FR-032, FR-037, FR-038 | `BR_M11_21_PhieuBaoPhiIT`, `BR_M11_23_ChungTuIT`, `CongChungTuIT` | Tích hợp |
| US8, FR-025 → FR-027 | `SaoKeTest`, `BaoCaoThuChiTest`, `FR_025_XuatIT`, `FR_026_SaoKeCongIT` | Đơn vị, tích hợp |
| FR-028, FR-029 | `QuyenSoDuIT` (bảng vai trò research E-12), `LuuTruSoDuIT` | Tích hợp |
| DBR-28 | `DBR_28_SoDuBatBienIT` (SC-001) | Tích hợp |
| Đồng thời | `DongThoiSoDuIT` (hai lần nộp; duyệt cùng thanh toán; hai lần phát hành; hai lần xác nhận) | Tích hợp |
| Client web | `NopChuyenKhoan.test.jsx` (cảnh báo trùng mã), `QuyetToan.test.jsx` (hai mã điều kiện), `PhieuBaoPhi.test.jsx` | Vitest |
| App người thân | `so_du_test.dart` (mã QR, chứng từ) | flutter_test |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | Bộ 3 tháng × 50 hồ sơ dựng từ bản giả 010; `DBR_28_SoDuBatBienIT` so số dư với tính tay; trigger chặn sửa |
| SC-002 | Q7; `DBR_30_ThanhToanBangIT` gửi sự kiện lặp |
| SC-003 | Q5 (cảnh báo trùng 100%); đo thao tác ghi nhận một khoản ≤ 1 phút trên web với người dùng đã hướng dẫn |
| SC-005, SC-012 | Q8, Q9 với đồng hồ giả lập; job 300 hồ sơ ≤ 5 phút; chạy lại không báo trùng |
| SC-006 | Q34 |
| SC-007 | Q11 → Q15 |
| SC-008, SC-015 | Q17 → Q19 cùng 004 bản giả; bảng quyết toán khớp sổ |
| SC-009 | Q29, Q30; `SaoKeTest` đầu kỳ + tổng = cuối kỳ |
| SC-010 | Q2, Q27, Q33; `QuyenSoDuIT` |
| SC-013 | Q17 |
| SC-014 | Q25, Q28 |
| SC-016 | Q25 → Q27; so byte khi tải lại |
| SC-017 | Đo phát hành phiếu 50 khoản và dựng bảng quyết toán ≤ 5 giây |

SC-004, SC-011 bị bỏ theo Q-385.
