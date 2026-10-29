# Quickstart: Kiểm chứng feature 010

> **Sửa 2026-10-29 (Q-395):** bỏ bảng hệ số phí khi vắng; nội trú vắng tính như ngày lưu trú, từ ngày Giải phóng giường không tính phí lưu trú; bỏ dấu "chờ quyết định", "không có dòng chính sách". Spec FR-005a, FR-007, FR-009 là căn cứ; chỗ nào dưới đây còn nhắc hệ số vắng theo 004 thì theo spec.

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-010.yaml](contracts/api-010.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 009: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản: Hành chính HC, HC2; Quản lý viện QL; Kế toán KT; người đại diện R, S của A; người thân N (chỉ quyền xem chi phí);
  - A nội trú dài hạn, hợp đồng giá tháng 9.300.000 đồng có gói "tã", phụ lục đổi giá từ 20/11 (12.000.000 → 13.500.000); C nội trú vắng bệnh viện với hệ số 70% từ ngày thứ 8; E bán trú giá tháng 3.000.000 đồng, lịch thứ 2, 4, 6;
  - danh mục dịch vụ, phiên bản đơn giá "đưa đi khám" 300.000 tới 14/10, 350.000 từ 15/10; thuốc X nguồn viện chưa có giá;
  - dữ liệu nguồn từ 004 (ngày lưu trú, lượt vắng), 005 (kết quả thay tã 2 gói, đưa đi khám), 006 (liều nguồn viện) dựng qua dữ liệu thật của các module hoặc bộ phát sự kiện thử;
  - CFG theo mặc định Phụ lục 25: CFG-M11-01 ngày cuối tháng liền sau, CFG-M11-02 500.000 đồng, CFG-M11-03 30%, CFG-M11-06 10%, CFG-M15-05 48 giờ, CFG-M15-06 96 giờ, CFG-M02-09 1 ngày;
  - bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}`: `SuKienTinhPhiNguon` của 011, 012, 014; `NguoiDaiDienPort` (012); `SoDuMuaHoPort` (017).

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/  flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (không có vòng 010 ↔ 004, 010 ↔ 006, 010 ↔ 012), test cơ sở dữ liệu (data-model mục 8) và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | Sau 05/10, HC tra cứu ngày lưu trú A 01/10 → 05/10 và lập 05/10; lập 07/10 khi chưa kết thúc | Một khoản Phí lưu trú Nháp 05/10, hệ số 100%, nguồn ngày lưu trú; bảng tháng 10 lập cùng lệnh; 409 `NGAY_CHUA_KET_THUC` | US1 #1 |
| Q2 | Lập ngày 06/10 (vắng về nhà, hệ số 100%) | Một khoản trỏ lượt vắng | US1 #2 |
| Q3 | Tra cứu kết quả tính phí của A; lập khoản thay tã | Khoản Vật phẩm 2 gói, dấu `THUOC_GOI`, 0 đồng, nguồn kết quả | US1 #3, US2 #5 |
| Q4 | Chọn 30 liều nguồn viện, lập hàng loạt hai lần | Lần 1: 30 khoản; lần 2: 30 nguồn bị bỏ qua `DA_CO_KHOAN` | US1 #4, FR-006 |
| Q5 | Bản giả 012 phát lượt ở lại 18:00 01/11 → 07:00 03/11; lập | Hai khoản Người thân ở lại (đêm 01/11, 02/11) | US1 #5 |
| Q6 | HC và HC2 cùng lập ngày 05/10; lập liều trước ngày bắt đầu hợp đồng | Một thành công, một 409 `DA_CO_KHOAN`; 409 `NGOAI_THOI_GIAN_TINH_PHI` | US1 #6, DBR-15 |
| Q7 | 004 đính chính lượt vắng 06/10 thành không vắng (`NgayLuuTruDaDoi`) khi khoản Nháp; khi đã chốt | Tính lại, lịch sử tính lại; đã chốt: biến động sau chốt, HC nhận thông báo, không có khoản điều chỉnh tự tạo | US1 #7, FR-020 |
| Q8 | Lập phí cả tháng 10 (31 ngày) A; cả tháng 11 (30 ngày) giá 10.000.000 | Mỗi ngày 300.000, tổng 9.300.000; 29 × 333.333 + 333.343 | US2 #1, #2, SC-002 |
| Q9 | Phí C ngày vắng thứ 8 → 30 | Phí ngày × 70%, làm tròn nửa lên | US2 #3 |
| Q10 | Đưa đi khám 14/10, lập ngày 01/11 | Đơn giá 300.000, căn cứ phiên bản hiệu lực 14/10 | US2 #4, BR-M11-02 |
| Q11 | Phí tháng 11 có phụ lục từ 20/11 | 01 → 19/11 theo 12.000.000/30; 20 → 30/11 theo 13.500.000/30 | US2 #6 |
| Q12 | E tháng 11: 13 buổi; vắng có báo 09/11; vắng không báo 11/11; khu nghỉ 18/11; đến thêm 24/11 | 230.769 (230.772 buổi 30/11); 0; 115.385; khu nghỉ: 12 buổi, 250.000, 18/11 không lập được; buổi phát sinh 100% theo đơn giá buổi | US2 #7, Q-140 |
| Q13 | Hợp đồng A bắt đầu 10/10, Hoàn tất tiếp nhận 12/10; lập 10/10, 11/10 | Dấu `CHUA_VAO_O`, 100% | US2 #8, Q-22 |
| Q14 | Lập liều thuốc X chưa có giá; 004 tạo phiên bản lùi từ 01/10 | Khoản `THIEU_DON_GIA`, không số tiền, HC nhận Nhẹ; sau `PhienBanDonGiaDaTao`: tính lại, gỡ dấu | US2 #9, FR-012 |
| Q15 | Trạng thái có mặt đổi hệ số 50% → 70% → 30% khi khoản Nháp | 161.538; 69.231 | US2 #10, FR-013 |
| Q16 | Gửi chốt khi còn khoản Nháp; đủ điều kiện | 409 `CON_KHOAN_NHAP` liệt kê; `CHO_CHOT`, QL nhận | US3 #1, #2 |
| Q17 | QL trả lại một khoản; chốt khi còn Đã kiểm tra chưa duyệt; chốt khi đủ | Khoản Nháp, bảng Đang mở; 409; `DA_CHOT`, khoản Đã chốt, `BangChiPhiDaChot`, người thân có quyền chi phí nhận Trung bình | US3 #3 → #5 |
| Q18 | Sửa, xóa khoản Đã chốt bằng API; `UPDATE` trực tiếp; gọi đính chính 000 | Không có endpoint; trigger chặn; 409 `KHONG_HO_TRO_DINH_CHINH` | US3 #6, SC-004 |
| Q19 | Gửi chốt tháng 10 tổng 13.200.000 so tháng 9 9.800.000 | Cảnh báo tăng 34,7% kèm ba loại; không chặn | US3 #7, BR-M11-07 |
| Q20 | Chốt bảng A khi bảng B chưa xong; chốt bảng B cuối cùng | Kỳ vẫn Chờ chốt; kỳ `DA_CHOT` | US3 #8, Q-131 |
| Q21 | CFG-M11-01 = ngày 5 tháng sau; tua 06/11 | HC nhận nhắc hằng ngày; kỳ không đổi ranh giới | US3 #9, Q-132 |
| Q22 | Kiểm tra hàng loạt 60 khoản (1 thiếu giá, 1 nhập tay); QL duyệt hàng loạt | 59 kiểm tra, 1 bỏ qua; nhập tay bị loại khỏi duyệt hàng loạt | US3 #10, FR-015 |
| Q23 | Lập khoản mới vào bảng Chờ chốt; nguồn đổi đưa khoản Đã kiểm tra về Nháp | Bảng về Đang mở; HC được báo | US3 #11, FR-004 |
| Q24 | Bỏ kiểm tra khi bảng Đang mở; khi Chờ chốt | 200; 409 | US3 #12 |
| Q25 | Liều có khoản Nháp bị hủy ghi nhận; liều có khoản đã chốt bị hủy ghi nhận ngày 05/11; HC lập điều chỉnh −30.000 | Khoản `DA_HUY`; biến động sau chốt, HC nhận; điều chỉnh ở bảng tháng 11, trỏ khoản gốc và biến động | US4 #3, #4 |
| Q26 | Điều chỉnh làm tổng gốc âm | 409 `TONG_AM` | US4 #7, FR-026 |
| Q27 | 004 lập hồ sơ kết thúc A ngày 10/11, E = 20/11 (`KyCuoiPort`); lập trước 11 → 20/11; lập 21/11 | Bảng tháng 11 là kỳ cuối đến 20/11; khoản `LAP_TRUOC`; 409 ngoài thời gian tính phí | US5 #1, BR-M11-08 |
| Q28 | Chốt kỳ cuối; 001 xem điều kiện Kết thúc lưu trú | `KT_A_CHI_PHI` Đạt | US5 #2, FR-034a |
| Q29 | Đổi E sang 22/11 khi kỳ cuối chưa chốt; khi đã chốt | Lập được 21, 22/11; Chưa đạt tới khi chốt; đã chốt: khoản bổ sung `ANH_HUONG_KET_THUC` ở bảng bổ sung | US5 #3, #4 |
| Q30 | Hết ngày 20/11 chưa Kết thúc lưu trú | HC được báo; điều kiện Chưa đạt; lập phí 21/11 | US5 #5, FR-035 |
| Q31 | Liều 20/11 sau khi kỳ cuối chốt | Khoản bổ sung ở bảng bổ sung, không dấu ảnh hưởng; điều kiện vẫn Đạt; nhắc mỗi CFG-M02-09 | US5 #6, FR-029 |
| Q32 | B qua đời 10:00 15/11 (001 lệnh, bean 010) | Không lập được sự kiện sau 10:00 15/11, ngày 16/11; ngày 15/11 lập được; liều 08:00 ghi muộn lập được; mục việc sau qua đời Hoàn thành khi kỳ cuối chốt | US5 #7, FR-036 |
| Q33 | Lập nhập tay "bỉm"; "phí làm lại thẻ BHYT" không lý do, có lý do | 409 `CO_TRONG_DANH_MUC`; 422; Nháp, không duyệt hàng loạt | US6 #1, #2 |
| Q34 | Đề nghị 300.000 gửi; 800.000 gửi; R từ chối; R đồng ý và S từ chối cùng lúc | Được phép mua; Chờ đồng ý, R, S nhận Trung bình, QL thấy; Từ chối; một quyết định thắng, sau 409 | US6 #3 → #5, #7 |
| Q35 | Ghi đã mua 820.000 (đồng ý 800.000); QL duyệt không lý do, có lý do | Khoản `VUOT_SO_DONG_Y`; 409; Đã duyệt, R nhận Nhẹ | US6 #6, Q-137 |
| Q36 | Ghi đã mua 1.000.000; xin bổ sung, R đồng ý; hoặc chọn mua gấp thuốc | 409 `VUOT_SO_DUOC_PHEP`; ghi được không dấu; ghi được dấu `MUA_GAP_VUOT` | US6 #9, Q-251 |
| Q37 | Thuốc mua hộ đã Đã mua tiếp nhận ở 006 nguồn gia đình gửi, 30 liều | Lập khoản Thuốc cho các liều: không có trong chỉ mục, 409 `KHONG_PHAI_SU_KIEN_TINH_PHI`; 006 đọc `DeNghiMuaHoPort.daMua` | US6 #8, Q-139 |
| Q38 | KT xuất tháng 10 (A, B, C chốt; D chưa); xuất lại toàn bộ; D chốt, xuất chưa từng xuất | File có cột FR-039, dòng tổng, ghi D chưa chốt; cùng nội dung, "đã xuất trước đó"; chỉ D "xuất lần đầu" | US7 #1 → #3, SC-009 |
| Q39 | R (có quyền sức khỏe), N (chỉ chi phí), HC, KT mở bảng có khoản thuốc; file | R thấy tên thuốc; N, HC, KT, file chỉ mã | US7 #7, SC-012 |
| Q40 | R mở tạm tính tháng 11 | Phí lưu trú, thuốc, mua hộ đã duyệt; không nhập tay chưa duyệt; ghi chú thiếu đơn giá | US7 #8, FR-038 |
| Q41 | QL thử lập khoản, xuất kế toán; HC thử chốt | 403 | FR-017, FR-043 |
| Q42 | Dừng backend qua đầu tháng và qua hạn chốt | Kỳ mới mở, kỳ cũ Chờ chốt, nhắc gửi một lần mỗi ngày | NFR-04 |

## 4. Test tự động: truy vết kịch bản → test

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `LamTronDongTest`, `BR_M11_09_ChiaGiaThangTest` (12 tháng), `PhiBuoiBanTruTest`, `ThoiGianTinhPhiTest`, `BR_M11_07_BienDongTest`, `DieuKienChiPhiKyCuoiTest`, `KiemTraVuotTest`, `TongTamTinhTest` | Q8, Q9, Q12, Q13, Q15, Q19, Q36, Q40 |
| Lập khoản | `BR_M11_01_LapTheoSuKienIT`, `FR_005_HangLoatIT`, `DBR_15_MotKhoanMoiDonViIT`, `LapTrungDongThoiIT`, `FR_007_NgayChuaKetThucIT`, `FR_011_ThoiGianTinhPhiIT` | Q1 → Q6 |
| Đơn giá | `DBR_16_DonGiaNgayPhatSinhIT`, `BR_M11_03_ThuocGoiIT`, `FR_012_ThieuDonGiaIT`, `PhuLucDoiGiaIT` | Q10, Q11, Q14 |
| Vòng đời | `FR_015_HangLoatIT`, `BR_M11_06_ChotBangIT`, `FR_033_ChotNguyenTuIT`, `FR_004_VeDangMoIT`, `DBR_17_BangDaChotIT` | Q16 → Q24 |
| Nguồn đổi | `BR_M11_05_TinhLaiIT`, `FR_027_BaoSauChotIT`, `FR_026_TongKhongAmIT` | Q7, Q25, Q26 |
| Kỳ cuối | `BR_M11_08_KyCuoiIT`, `FR_034_DoiNgayKetThucIT`, `FR_035_QuaNgayKetThucIT`, `FR_029_BangBoSungIT`, `FR_036_QuaDoiIT`, `KT_A_ChiPhiIT` | Q27 → Q32 |
| Mua hộ | `BR_M11_07_MuaHoIT`, `FR_023a_NguoiDaiDienIT`, `FR_024_VuotSoDongYIT`, `Q139_ThuocMuaHoIT` | Q34 → Q37 |
| Cung cấp | `FR_039_XuatKeToanIT` (SC-009), `FR_019_TenThuocIT`, `FR_038_TamTinhIT`, `FR_038a_TongChuaChotIT` | Q38 → Q40 |
| Quyền | `FR_017_TachQuyenIT` | Q41 |
| Bộ lập lịch | `ChayBuM11IT` | Q42 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit: chỉ `m11.sodu`, `m11.muasam` dùng port nội bộ; gói `..lich..` không lập khoản, bảng (trừ kỳ của viện) | |
| Web (Vitest) | Tra cứu nguồn không có bộ lọc "chưa lập khoản"; lập hàng loạt hiển thị nguồn bị bỏ qua; khoản theo sự kiện không có ô nhập số tiền; màn biến động sau chốt không có số chênh lệch tính sẵn | Q4, Q25 |
| App (`flutter_test`) | Người đại diện đồng ý/từ chối mua hộ; tạm tính chi phí | Q34, Q40 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | Q1 → Q6: 100% khoản theo sự kiện trỏ nguồn tồn tại; lập trùng và ngoài thời gian bị chặn; 0 khoản có `nguoi_tao` là Hệ thống |
| SC-002 | `BR_M11_09_ChiaGiaThangTest` |
| SC-003 | Q3, Q10, Q11 và bộ khoản dựng sẵn |
| SC-004 | Q17, Q18 |
| SC-005 | Q7, Q25: thông báo hoặc tính lại ≤ 5 phút sau khi bản đính chính nguồn lưu; tổng kỳ gốc và file đã xuất không đổi |
| SC-006 | Đo `POST /chi-phi/khoan/theo-su-kien` với 30 ngày và 300 nguồn ≤ 30 giây |
| SC-007 | Đợt thử 300 người một tháng |
| SC-008 | Truy vấn mọi khoản đã chốt (trừ nhập tay, miễn giảm) có nguồn tồn tại |
| SC-009 | Q38 |
| SC-010 | Q27 → Q32 |
| SC-011 | Q34 → Q36 |
| SC-012 | Q39 và quét lịch sử tính lại, nhật ký, thông báo, tạm tính |

## Kịch bản bổ sung 2026-10-22 (Q-378, Q-380)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| P1 | Chốt bảng tháng 10 của A gồm phí lưu trú, một khoản thuốc Amlodipin, một khoản điều chỉnh cho tháng 9; gọi `ChiPhiChoSoDuPort.bangDaChot` | Đủ khoản và tổng theo loại; khoản thuốc mô tả "Thuốc" và mã vật phẩm, không có tên thuốc; khoản điều chỉnh có kỳ gốc tháng 9; bảng chưa chốt trả rỗng | FR-042, FR-019 |
| P2 | Hợp đồng của B có quy định phí chấm dứt sớm; Hành chính lập khoản nhập tay "phí chấm dứt sớm" có lý do vào bảng kỳ cuối; Quản lý viện duyệt | Khoản theo luồng FR-021 như mọi khoản nhập tay; không có tính tự động số ngày báo trước | Q-380 |

Test: `BangDaChotPortIT` (P1); P2 nằm trong `BR_M11_04_NhapTayIT` hiện có (thêm một ca).
