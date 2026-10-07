# Quickstart: Kiểm chứng feature 003

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-003.yaml](contracts/api-003.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000, 001, 002: JDK 21, Docker (Testcontainers MySQL 8.4), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - các tài khoản của quickstart 002: Quản lý viện QL, Hành chính HC, Trưởng tầng H được giao Tầng 1, Trưởng tầng H2 được giao Tầng 2, bác sĩ BS, Điều dưỡng D, Nhân viên chăm sóc C; ca ngày Tầng 2 có Người phụ trách ca N (vai trò Điều dưỡng);
  - cơ cấu: Khu A; Tầng 1, Tầng 2; P101 (chỉ "Chăm sóc cơ bản", Nữ), P105, P107 (Không giới hạn), P201 ("Thường xuyên", "Đặc biệt", Nữ, loại phòng 2 giường), P305 (loại phòng khác đơn giá); mỗi phòng 2 giường;
  - người cao tuổi của 001: A (nữ, Đang tiếp nhận, mức Đặc biệt), B (nam, Đang lưu trú ở P105-G1), E (Đang tiếp nhận, có hồ sơ chờ);
  - CFG theo mặc định Phụ lục 25: CFG-M02-02 48 giờ, CFG-M03-02 1 ngày, CFG-M03-04 4 giờ, CFG-M03-07 24 giờ, CFG-M03-08 30 phút; CFG-M03-01 đặt 3 cho dữ liệu kiểm thử; buổi sáng 07:00–12:00, chiều 12:00–17:00;
  - bản giả có công tắc qua `/api/v1/kiem-thu/gia-lap/{module}` (chỉ profile `test`, `demo`): `NguonLuuTruChoPhanBo`, `DemLichDenBanTru` (004), `TinhTrangLayNhiemChoPhanBo` (007), `ThamChieuViTri` (008), `TaiSanGiuongPort` (019).

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
web/      npm ci && npm test && npm run dev
app-nhan-vien/   flutter test
```

Kết quả mong đợi: `verify` xanh. Bộ test gồm test kiến trúc (Modulith `verify()` không có vòng phụ thuộc; ArchUnit chỉ `GiuongTrangThaiService` ghi trạng thái giường), test cơ sở dữ liệu (data-model mục 8), test đồng thời và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | QL tạo Khu B, Tầng 3, phòng P301 (loại 2 giường, mức {Thường xuyên, Đặc biệt}, Nữ), giường G1, G2, G3 | 201; G1–G3 `TRONG`; nhãn "Khu B – Tầng 3 – P301 – G1"; G3 kèm cảnh báo `VUOT_SO_GIUONG_TIEU_CHUAN`; bản giả 019 ghi nhận ba lần ghi tăng | US1 #1, FR-001 → FR-004, BR-M03-22, Q-238 |
| Q2 | Tạo phòng mã P201 ở Tầng 3; tạo G1 thứ hai trong P301; H tạo phòng | 409 `MA_DA_TON_TAI` hai lần; 403 | US1 #2, #6 |
| Q3 | QL đổi P105 sang chính sách "Nữ" khi B (nam) đang ở | 409 liệt kê B | US1 #5, FR-006 |
| Q4 | HC mở danh sách giường phù hợp cho A | Chỉ giường thỏa FR-018 (không có P101, P105, P107 nếu sai điều kiện), sắp theo khu, tầng, phòng; không giường nào được chọn sẵn | US2 #12, FR-022, Q-307 |
| Q5 | HC phân bổ P201-G1 cho A từ T | 201 `TUONG_LAI`, `nguonMuc = HIEN_HANH`; G1 vẫn `TRONG`, hiển thị "đã có phân bổ từ T"; không có `GiuongDaTrong` | US2 #1, FR-023 |
| Q6 | Tua tới T + 1 phút, chạy job | HC nhận `PhanBoTuongLaiQuaBatDau` một lần; chạy job lần hai không báo lại | FR-023 |
| Q7 | Hoàn tất tiếp nhận A (bản giả bean khác của 001 Đạt) | Phân bổ `DANG_HIEU_LUC`, `batDau` = thời điểm lệnh; G1 `DANG_SU_DUNG` trong cùng lần | US2 #1, FR-023 |
| Q8 | Phân bổ giường cho B ở phòng "Nữ" và phòng chỉ "Cơ bản", trong P105 cách ly, giường Tạm ngừng sử dụng, cùng lúc | 409 một lần, liệt kê `PB_GIOI_TINH`, `PB_MUC_CHAM_SOC`, `PB_CACH_LY`, `PB_TRANG_THAI_GIUONG` | US2 #2 → #5, #7, SC-002 |
| Q9 | 10 luồng cùng phân bổ P301-G2 cho 10 người Đang tiếp nhận trong một giây | Đúng 1 thành công; 9 nhận `PB_CHONG_THOI_GIAN` "Giường đã được phân bổ…"; không có 500 | US2 #8, BR-M03-02, SC-001 |
| Q10 | Phân bổ thêm cho A (đã có phân bổ đang hiệu lực) | 409 `DA_CO_PHAN_BO` | US2 #9 |
| Q11 | H (Tầng 1) phân bổ giường Tầng 2 cho người Đang tiếp nhận; Điều dưỡng phân bổ | 403 ngoài phạm vi; 403 | US2 #14, #15, BR-M03-20 |
| Q12 | H2 chuyển A từ P201-G1 sang P201-G2 lúc 14:00 | Phân bổ cũ `DA_DONG` kết thúc 14:00; mới bắt đầu 14:00, `phanBoTruocId`; G1 `CHO_SAN_SANG`, G2 `DANG_SU_DUNG`; `DaChuyenGiuong` tới bản giả 005 | US3 #1, #5, FR-028, BR-M03-04, SC-004 |
| Q13 | Bản giả 019 ném lỗi giữa lệnh chuyển; hai lệnh chuyển A đồng thời | Không có thay đổi nào; một lệnh thành công, lệnh kia được kiểm lại trên vị trí mới | US3 #3, #9, FR-033 |
| Q14 | H2 chuyển A sang P305 (khác đơn giá) với lý do thường; rồi lý do "Ngã nhiều lần" có cờ y tế/an toàn | 409 `CAN_YEU_CAU_THAY_DOI_LUU_TRU`; 200, dấu `CHO_LAP_YEU_CAU`, HC và QL được báo, không có yêu cầu nào tự tạo; tua 1 ngày: HC được nhắc | US3 #8, FR-030, Q-306 |
| Q15 | Bản giả 004 gọi `ketQuaYeuCauSauChuyenGap(tuChoi)`; tua 1 ngày; chuyển A về giường cùng giá | Dấu `CHO_VE_CUNG_GIA`, H2 và HC được nhắc; sau khi chuyển: dấu gỡ, không nhắc nữa | US3 #8, SC-013 |
| Q16 | N (Người phụ trách ca Tầng 2) Xác nhận G1 sẵn sàng; C thử xác nhận | 200, G1 `TRONG`, `GiuongDaTrong`; C 403 | US8 #2, #7, FR-044 |
| Q17 | Giường G2 Chờ sẵn sàng, tua 4 giờ 1 phút, chạy job hai lần | Trưởng tầng được nhắc đúng một lần; không có mục bàn giao nào được tạo | US8 #3, SC-011, Q-307 |
| Q18 | Người rời G2 được bản giả 007 đưa vào danh sách tiếp xúc; H2 xác nhận G2 không ghi khử khuẩn; rồi ghi đủ | H2 được báo; 409 `CHUA_GHI_KHU_KHUAN`; 200 | US8 #4, #5, BR-M03-10, SC-010 |
| Q19 | Phân bổ người mới vào G2 (Chờ sẵn sàng từ 10:00) bắt đầu 12:00, rồi 15:00 | 409 `PB_CHO_SAN_SANG_TRUOC_HAN`; 201 `TUONG_LAI`, không `GiuongDaTrong` | Edge Case Q-40, FR-011a |
| Q20 | Bản giả 004 duyệt yêu cầu đổi giường của B sang P107-G2 hiệu lực ngày D (`khiDuyet`); P107-G2 Chờ sẵn sàng tới ngày D; job 000 chạy 00:05; H xác nhận G2 lúc 15:40 | Phân bổ tương lai tạo lúc duyệt; 00:05: giữ `TUONG_LAI`, `choGiuongSanSang`, H và HC được báo; 15:40: chuyển thực hiện, phân bổ mới bắt đầu 15:40, G2 `DANG_SU_DUNG` | US3 #7, FR-011a, FR-029 |
| Q21 | Như Q20 nhưng dừng job tới 02:30 | Hai bản ghi ghi 02:30, `thoiDiemDuKien` 00:00, `thucHienTre`; QL nhận `ViecLichTre` | US3 #11, FR-016a, SC-014 |
| Q22 | Cho tạm vắng A với bản giả 004 trả "giữ giường, hạn 30 ngày"; Ghi nhận trở về | G2 `DANG_GIU_CHO` (người vắng), phân bổ không đổi; trở về: `DANG_SU_DUNG`, vẫn một phân bổ | US4 #6, #7, FR-024 |
| Q23 | B vắng giữ P105-G1; BS đặt cách ly P105; Ghi nhận trở về B; BS xác nhận ở lại | Xem trước lệnh có cảnh báo `TRO_VE_PHONG_CACH_LY`; lệnh thành công; `TroVePhongCachLy` Khẩn cấp; dấu `CHO_BS_XAC_NHAN` có trong `CanhBaoBanGiaoApi`; xác nhận: dấu gỡ | US7 #7, FR-024a |
| Q24 | H chuyển B ra P107 khi P105 cách ly; rồi dùng endpoint kiểm soát lây nhiễm với BS; với Điều dưỡng làm "bác sĩ chỉ định" | 409 `CHI_CHUYEN_THEO_CHI_DINH`; 200, phân bổ ghi bác sĩ, BS được báo; 422 `BAC_SI_KHONG_HOP_LE` | US7 #3, FR-010, BR-M03-21, SC-012 |
| Q25 | Bản giả 007 khoanh vùng Tầng 1; phân bổ giường Tầng 1; gỡ khoanh vùng | 409 `PB_CACH_LY`; sau khi gỡ: được, trừ phòng còn cách ly riêng | US7 #6, FR-009 |
| Q26 | Bản giả 004 gọi `giuChoHoSoCho(E, P301-G1)`; tua 48 giờ 1 phút; chạy job | G1 `DANG_GIU_CHO` hạn +48 giờ; sau hạn: `TRONG` không qua Chờ sẵn sàng, bản giả 004 nhận `hoSoChoHetHanGiuCho`, `GiuongDaTrong` | US4 #4, #5, FR-015, SC-007 |
| Q27 | Kết thúc lưu trú A (đang vắng có giữ giường) | Phân bổ đóng tại thời điểm hiệu lực; giường `CHO_SAN_SANG`; không `GiuongDaTrong` tới khi xác nhận | US4 #9, FR-025, CHK004 |
| Q28 | Trở về khi giường đã giải phóng; tua 1 ngày; HC phân bổ giường mới | Dấu "chưa có giường"; HC được nhắc; phân bổ `DANG_HIEU_LUC` bắt đầu ngay, dấu gỡ | FR-025a, BR-M03-19 |
| Q29 | Bản giả 001 phát `MucChamSocDaDoi` cho người ở P101 sang Đặc biệt; tua 1 ngày | Dấu `PHONG_KHONG_PHU_HOP`, báo một lần, nhắc mỗi ngày; không tự chuyển | US3 #10, FR-034, BR-M03-23 |
| Q30 | H2 chuyển A với `thoiDiemChuyen` lùi 8 giờ; lùi 3 ngày; lùi chồng khoảng Tạm vắng | 200 `nhapBu`; 202 yêu cầu `M03.NHAP_BU_PHAN_BO`, tra cứu có `coNhapBuChoDuyet`; 409 `KHOANG_VANG` | FR-027, Edge Case |
| Q31 | Tra cứu A lúc 10:00 và 14:00 ngày chuyển; người cùng phòng P201 trong 30 ngày | P201-G1; P201-G2; danh sách khớp dữ liệu, có khoảng chồng và khoảng vắng, không có trường sức khỏe | US5 #1 → #4, SC-003 |
| Q32 | Đính chính phân bổ đã đóng của A làm đổi giờ kết thúc; đính chính gây chồng | 201, tra cứu dùng giá trị mới với `daDinhChinh`; `PhanBoDaDinhChinh` tới bản giả 007; 409 `PB_CHONG_THOI_GIAN` | FR-026, BR-M03-24 |
| Q33 | Bản giả 019 Đưa vào bảo trì giường Trống có phân bổ tương lai (thường); rồi gắn hư hỏng khi dọn giường | 409 chỉ ra phân bổ tương lai; 200, giường `DANG_BAO_TRI`, phân bổ `DA_HUY` "giường hỏng", HC và Trưởng tầng được báo | US4 #3, US8 #6, FR-013a |
| Q34 | Bản giả 019 gắn dấu "chờ chuyển người" cho G đang có người; chuyển người đi; xác nhận sẵn sàng; gỡ dấu "đã sửa tại chỗ" | Giường giữ nguyên; về `TRONG` không `GiuongDaTrong`, phân bổ mới bị chặn `PB_CHO_CHUYEN_NGUOI`; gỡ dấu: `GiuongDaTrong` | US4 #12, FR-013b |
| Q35 | QL Tạm ngừng sử dụng G7 không lý do; có lý do; Sử dụng lại; H xác nhận; QL Ngừng hiệu lực phòng có hai giường Tạm ngừng | 422; 200 `TAM_NGUNG`; `CHO_SAN_SANG`; `TRONG` và `GiuongDaTrong`; 409 liệt kê hai giường | US4 #13 → #16 |
| Q36 | QL tạo khu nghỉ bán trú thứ hai | 409 `DA_CO_KHU_NGHI` | US6 #1, FR-038 |
| Q37 | Bản giả 004 đăng ký người thứ 4 sáng 06/10; lịch 10:00–15:00 khi buổi chiều đủ; QL giảm CFG-M03-01 xuống 2 | Vi phạm `VUOT_SUC_CHUA` kèm số chỗ còn lại; vi phạm buổi chiều; QL nhận `KhuNghiVuotSucChua`, không lịch nào bị hủy | US6 #2, #6, #7, FR-039, FR-040, SC-006 |

## 4. Test tự động: truy vết kịch bản → test

| Spec | Lớp test (backend, `vn.eldercare.m03…`) | Loại |
| --- | --- | --- |
| US1 #1–8, FR-001 → FR-007 | `CoCauIT`, `MaDuyNhatIT`, `NgungHieuLucCoCauIT`, `FR_006_DoiChinhSachPhongIT`, `FR_007_KhongDoiChaIT`, `BR_M03_22_SoGiuongTieuChuanIT`, `Q_238_TaoGiuongGhiTangIT` | Tích hợp |
| US2 #1–15, FR-017 → FR-023, FR-025a | `BR_M03_01_DieuKienPhanBoIT`, `BR_M03_02_HaiNguoiCungGiuongIT`, `DBR_09_KhongChongIT`, `FR_020_PhanBoDongThoiIT`, `FR_023_PhanBoBanDauIT`, `BR_M03_20_PhamViPhanBoBanDauIT`, `FR_022_DanhSachGiuongIT`, `BR_M03_19_TroVeChuaCoGiuongIT` | Tích hợp, đồng thời |
| US3 #1–11, FR-027 → FR-034 | `BR_M03_07_ChuyenGiuongIT`, `FR_033_ChuyenDongThoiIT`, `BR_M03_04_SuKienChuyenIT`, `FR_029_ChuyenTheoLichIT`, `FR_030_ChuyenKhacGiaIT`, `FR_027_NhapBuIT`, `BR_M03_23_TuongLaiKhongPhuHopIT`, `FR_034_PhongKhongPhuHopIT` | Tích hợp, đồng hồ giả lập |
| US4 #1–16, FR-011 → FR-016a | `BangChuyenTrangThaiGiuongTest` (tham số hóa mọi dòng và cặp ngoài bảng), `BR_M03_05_GiuongCoNguoiIT`, `BR_M03_06_VeTrongIT`, `BR_M03_13_GiuongHongDatTruocIT`, `BR_M03_15_DauChoChuyenNguoiIT`, `TamNgungSuDungIT`, `FR_016a_ThucHienTreIT`, `ChiMotNoiGhiTrangThaiArchTest` | Đơn vị, tích hợp, kiến trúc |
| US5 #1–5, FR-035 → FR-037 | `LichSuViTriIT`, `CungPhongIT`, `TraCuuDinhChinhIT`, `BR_M03_24_DinhChinhBao007IT`, `QuyenTraCuuIT` | Tích hợp |
| US6 #1–7, FR-038 → FR-042 | `BR_M03_03_SucChuaIT`, `FR_039_BuoiGiaoIT`, `BR_M03_25_DoiKhungGioIT`, `KhuNghiDuyNhatIT` | Tích hợp, cơ sở dữ liệu |
| US7 #1–7, FR-008 → FR-010, FR-024a | `CachLyPhongIT`, `BR_M03_01_CachLyIT`, `BR_M03_21_BacSiChiDinhIT`, `FR_024a_TroVePhongCachLyIT` | Tích hợp |
| US8 #1–7, FR-044 → FR-048 | `BR_M03_09_ChoSanSangIT`, `BR_M03_10_KhuKhuanIT`, `XacNhanSanSangQuyenIT` | Tích hợp, đồng hồ giả lập |
| SPI 001 (G-06) | `PhanBoThamGiaChuyenTrangThaiIT` (mỗi lệnh của bảng G-06, rollback khi lỗi) | Tích hợp |
| Cơ sở dữ liệu | `PhanBoRangBuocDbIT` (cột sinh, trigger, `CHECK`), `NhomBaTriggerIT` mở rộng | Testcontainers |
| Edge Cases | Mỗi gạch đầu dòng một phương thức trong các lớp trên | |
| Client web | `BangGiuong.test.tsx` (trạng thái, dấu, "đã có phân bổ từ"), `ChonGiuong.test.tsx` (không chọn sẵn), `DieuKienChuaThoa.test.tsx` | Vitest |
| App nhân viên | `xac_nhan_san_sang_test.dart` (ô khử khuẩn bắt buộc khi `canKhuKhuan`), `chuyen_giuong_test.dart` | flutter_test |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

- SC-001, SC-002, SC-004, SC-006, SC-007, SC-009 → SC-014: test tự động ở mục 4, chạy trên MySQL thật và đồng hồ giả lập.
- SC-003 (danh sách cùng phòng trong 1 phút): đo từ lúc mở chức năng khoanh vùng của 007 tới lúc danh sách hiện ra, với bộ dữ liệu 30 ngày; phần 003 phải trả `cungPhong` dưới 1 giây.
- SC-005 (phân bổ trong 2 phút): đo với dữ liệu quy mô NFR-01 (300 giường), 60 người dùng đồng thời, ít nhất 10 lần của Hành chính đã được hướng dẫn, lấy trung vị.
- SC-008: test so `TinhTrangGiuongApi` với trạng thái từng giường tại cùng thời điểm.
