# Quickstart: Kiểm chứng feature 004

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-004.yaml](contracts/api-004.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 003: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản và cơ cấu của quickstart 002, 003 (Quản lý viện QL, Hành chính HC, Kế toán KT, Trưởng tầng H, Bác sĩ BS, Điều dưỡng D; phòng P201 nữ "Thường xuyên, Đặc biệt", P305 loại phòng khác đơn giá);
  - danh mục dịch vụ: ba dịch vụ chăm sóc theo mức, phí lưu trú theo hai loại phòng, phục hồi, đưa đi khám; mỗi dịch vụ một phiên bản đơn giá từ 01/01;
  - CFG theo mặc định Phụ lục 25: CFG-M02-05 là bảng 6.7, CFG-M02-08 theo bảng mặc định, CFG-M02-02 48 giờ, CFG-M02-03 30 ngày, CFG-M02-04 30 ngày, CFG-M02-09 1 ngày, CFG-M02-10 7 ngày;
  - người cao tuổi của 001: A (nữ, Đang tiếp nhận), C, E, F, H, K cho danh sách chờ (US2 #4), B (bán trú);
  - bản giả có công tắc qua `/api/v1/kiem-thu/gia-lap/{module}` (chỉ `test`, `demo`): `NguoiDaiDienPort`, `BanGhiDonPort` (012), `TrangThaiDatCocPort` (017), `KyCuoiPort` (010), `TrangThaiCoMatBanTruPort` (005), năm bean `MucViecSauQuaDoiPort`, bean `ThamGiaChuyenTrangThai` của 006, 007, 010, 013, 017 cho lệnh `KET_THUC_LUU_TRU`.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (không có vòng 001 ↔ 004, 002 ↔ 004), test cơ sở dữ liệu (data-model mục 12), test đồng thời và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | HC lập lượt đăng ký cho A (nội trú dài hạn, Thường xuyên, vào 15/10); lập lượt thứ hai | 201 `DANG_XU_LY`, sáu điều kiện "Chưa đạt" (thêm (f) DBR-02, Q-343); 409 `DA_CO_LUOT_DANG_KY` | US1 #1, #2, FR-001, FR-004 |
| Q1a | HC ghi ý kiến của A "Không bày tỏ được" nguồn `NHAN_VIEN_QUAN_SAT`; rồi "Đồng ý" nguồn `NGUOI_CAO_TUOI`; ghi ý kiến nguồn người thân không có quan hệ Hiệu lực | 201, 201 (ý kiến đầu `DUOC_THAY_THE`, vẫn xem được); 422 `NGUON_Y_KIEN_KHONG_HOP_LE` | FR-001, Q-356 (bổ sung 2026-10-18) |
| Q1b | Hoàn tất tiếp nhận B khi lượt chưa có ý kiến (mọi điều kiện Đạt) | 200 kèm cảnh báo `CHUA_GHI_Y_KIEN_VAO_O`; danh sách điều kiện có `yKienVaoO = null`, không có dòng điều kiện mới | FR-004a |
| Q1c | C có ý kiến "Không đồng ý"; Hoàn tất tiếp nhận không lý do; có lý do | 422 (thiếu `lyDo`, 001 FR-046); 200 kèm cảnh báo `NCT_KHONG_DONG_Y_VAO_O`, lịch sử trạng thái lưu lý do, `yKienLucTiepNhanId` trỏ ý kiến đó | FR-004a |
| Q2 | Trưởng tầng, Bác sĩ lập lượt đăng ký | 403 | US1 #8 |
| Q3 | HC lập hợp đồng Nháp cho A khi chưa có đánh giá; sửa giá tháng thấp hơn phiên bản; Gửi ký | 201 kèm "chưa đánh giá"; Gửi ký 409 vì điều khoản khác chuẩn chưa duyệt | US3 #1, #9, FR-025, FR-025a |
| Q4 | Gửi duyệt điều khoản; QL duyệt (api-000); Gửi ký; Trả về nháp, sửa giá lần nữa; Gửi ký | Duyệt xong Gửi ký được; sau khi sửa: 409, phải duyệt lại | US3 #2, #9, SC-011 |
| Q5 | Gửi ký hợp đồng thiếu dịch vụ chăm sóc, hoặc dịch vụ chăm sóc lệch mức | 409 (FR-025, Q-297) | FR-025 |
| Q6 | Ghi nhận đã ký thiếu bản scan; người ký không phải người đại diện; A chưa có phân bổ giường từ 15/10; đủ điều kiện | 422; 409; 409 "chưa có giường từ ngày bắt đầu"; 200 `HIEU_LUC`, phiên bản nội dung 0 có đơn giá đã ghi | US3 #3, FR-021, FR-036 |
| Q7 | `PUT` hợp đồng Hiệu lực; `UPDATE` trực tiếp ở cơ sở dữ liệu | 409 `HOP_DONG_DA_KHOA`; trigger chặn | US3 #4, SC-001 |
| Q8 | Ghi nhận đã ký hợp đồng thứ hai của A từ 01/06/2027; hợp đồng kế tiếp từ 15/10/2027; hai lệnh đồng thời chồng nhau | 409 chồng thời gian; 200; đúng một lệnh thành công | US3 #5, #11, DBR-06 |
| Q9 | Bản giả 017 trả Chưa đáp ứng, rồi Đã đáp ứng; xem điều kiện tiếp nhận | (c) "Chưa đạt", rồi "Đạt" kèm giao dịch nguồn và Kế toán lập (sửa 2026-10-22, Q-385: không còn số phiếu thu); HC không có lệnh xác nhận cọc | US4 #1 → #5 |
| Q10 | Bác sĩ xác nhận đánh giá đầu vào A mức Đặc biệt khi hợp đồng Hiệu lực ghi Thường xuyên; Hoàn tất tiếp nhận | HC nhận `LechMucHopDong` (Trung bình); 409 liệt kê "mức của hợp đồng khác mức hiện hành" cùng các điều kiện khác trong một lần | US1 #4, #9, FR-025b |
| Q11 | HC lập yêu cầu đổi mức có tham chiếu đánh giá; QL duyệt với ngày hiệu lực ngày mai; tua 1 ngày | Duyệt: phụ lục `CHO_HIEU_LUC`; 00:05 hôm sau: phụ lục `DA_AP_DUNG`, mức hiện hành đổi, dịch vụ chăm sóc đổi theo mức, 005 nhận `PhuLucDaApDung`; (e) Đạt | US5 #1, #2, FR-042, BR-M02-04 |
| Q12 | Hoàn tất tiếp nhận A trước ngày bắt đầu hợp đồng; đúng ngày | 409 (FR-075); 200, lượt đăng ký `DA_TIEP_NHAN`, 018 nhận `TiepNhanHoanTat` | US1 #7, FR-075, FR-033b |
| Q13 | Nội dung hợp đồng A tại 30/11, 01/12, 01/01 với PL1 (01/12) và PL2 (01/01) | Đúng giá trị từng ngày | US3 #6, SC-002 |
| Q14 | Duyệt yêu cầu đổi giường sang P305-G2 hiệu lực 05/12; bản giả 007 cách ly phòng; tua tới 05/12 | Duyệt: phân bổ tương lai (003); 05/12: yêu cầu "Áp dụng không thành", phụ lục `KHONG_AP_DUNG`, phân bổ tương lai hủy | US5 #3, FR-045 |
| Q15 | Duyệt yêu cầu đổi mức thứ hai khi đã có một yêu cầu đổi mức chờ hiệu lực | 409 (FR-046) | US5 #7 |
| Q16 | Người đại diện lập yêu cầu thêm dịch vụ qua cổng; người thân khác thử | Nháp, HC được báo; 403 | US5 #5 |
| Q17 | Đưa C, E, F, H, K vào danh sách chờ; tua 15 ngày; xem điểm của C | C = 9 với chi tiết từng thành phần; tua 7 ngày: 10 | US2 #1, #2, SC-004 |
| Q18 | Đề nghị +3 điểm cho C; trước khi duyệt; QL duyệt; QL từ chối một đề nghị khác | Điểm chưa đổi; tăng 3, lịch sử có người duyệt; không đổi khi từ chối | US2 #3, #11 |
| Q19 | 003 phát `GiuongDaTrong` cho P201-G1 | HC nhận `GiuongTrongCanChonHoSo`; danh sách phù hợp E, C, K theo thứ tự; không hồ sơ nào được chọn sẵn | US2 #4, BR-M02-01, SC-003 |
| Q20 | HC chọn K (không đứng đầu) không lý do; có lý do; chọn E | 422; được; E `DA_LIEN_HE`, G1 giữ chỗ 48 giờ (003) | US2 #5, FR-014 |
| Q21 | Tua 48 giờ 1 phút (job 003) | E về `DANG_CHO`, giữ điểm; HC được báo chọn người tiếp theo; E không còn trong danh sách của G1 trong lượt này | US2 #6, SC-005 |
| Q22 | Ghi nhận từ chối hồ sơ của F | F `TU_CHOI`, lượt đăng ký `DA_HUY`, F Hủy tiếp nhận (001) cùng lý do, hợp đồng Nháp `DA_HUY` trong cùng lệnh | FR-002 |
| Q23 | Cho tạm vắng A loại Về nhà 01/11 → 05/11 với người đón hợp lệ; người đón không hợp lệ | 200, lượt vắng mở, G1 giữ chỗ (003); 409 thiếu bản ghi đón | US6 #1, FR-048 |
| Q24 | Tua từng ngày tới 05/11; Gia hạn dự kiến trở lại; Ghi nhận trở về lúc 16:00 ngày 05/11 | Mỗi ngày 100%, giữ giường; lịch sử mốc; ngày 05/11 vẫn là ngày vắng | US6 #2, #3, #8, FR-058 |
| Q25 | C chuyển viện 01/11; tua 31 ngày; QL chưa quyết; QL "Giữ tiếp" 50%; quyết định khác "Giải phóng" | Ngày 1–7 100%, 8–30 70%, ngày 31 báo QL, các ngày "chờ quyết định" 70%; quyết định: tính lại 50%; giải phóng: giường Chờ sẵn sàng (003) | US6 #4 → #6, BR-M02-07, SC-006 |
| Q26 | D có hợp đồng ghi đè "50%, không giữ giường"; cho tạm vắng | 50%; phân bổ đóng, giường Chờ sẵn sàng | US6 #7, FR-051, FR-054 |
| Q27 | Loại vắng "Đi khám trong ngày" không có dòng | 100%, giữ giường, dấu "không có dòng chính sách", QL được báo | FR-053 |
| Q28 | Dòng ngày 1 là "Quản lý quyết định" | Giữ tạm, 100%, "chờ quyết định", QL được báo ngay | FR-056, BR-M02-17 |
| Q29 | Đính chính lượt vắng đã đóng của C từ ngày 30 thành ngày 32 | Thêm ngày 31, 32 "chờ quyết định"; QL được báo ghi quyết định chỉ đặt hệ số; 010 nhận `NgayLuuTruDaDoi` | US6 #11, FR-061, BR-M02-18 |
| Q30 | B (bán trú) báo vắng trước 25 giờ; khai báo ngày nghỉ cho hôm qua không lý do, rồi có lý do | Vắng có báo, 0%; 422; 201, 010, 011 nhận sự kiện | US6 #9, FR-060, FR-060a |
| Q31 | HC lập hồ sơ kết thúc cho A "Xuất viện" 30/11; xem điều kiện khi bản giả 013 còn 1 đồ gửi, 007 còn 1 cảnh báo | Bản giả 010 nhận `taoKyCuoiNhap`; danh sách (a) → (h) với hai mục Chưa đạt, (f), (g) "Tự động" | US7 #1, FR-063, FR-066 |
| Q32 | Kết thúc lưu trú (api-001); lập ngoại lệ (d), QL duyệt; trả đồ, ghi bàn giao; Kết thúc lưu trú | 409 liệt kê hai điều kiện; (d) "Đạt (ngoại lệ)"; 200, hợp đồng Kết thúc hoặc Chấm dứt, giường Chờ sẵn sàng, hồ sơ lưu trú đã đóng | US7 #2 → #4, #7, SC-007 |
| Q33 | Lập ngoại lệ cho (e) | 422 | FR-064 |
| Q34 | Ngày kết thúc dự kiến 30/11, tua tới 01/12 khi còn đồ gửi; đặt ngày mới 03/12 | Dấu "quá ngày dự kiến", ngày 01/12 là sự kiện tính phí, HC được nhắc; ngày mới: dấu gỡ, `KyCuoiPort.tinhLai` | US7 #6, FR-066a |
| Q35 | Bác sĩ Ghi nhận qua đời A (api-001) khi còn đồ gửi, thuốc gửi, chi phí chưa chốt, hồ sơ kết thúc đang chuẩn bị | Lệnh không bị chặn; hợp đồng Chấm dứt "Qua đời"; hồ sơ kết thúc `DA_HUY`; danh sách việc có mục Chưa hoàn thành; tua 1 ngày: HC được nhắc | US8 #1 → #3, FR-070 → FR-072 |
| Q36 | Năm bản giả `MucViecSauQuaDoiPort` chuyển Hoàn thành; module gọi `capNhat` | Hồ sơ lưu trú đã đóng, dừng nhắc | US8 #3, FR-072 |
| Q37 | HC ghi qua đời có nguyên nhân rồi xem lại bản ghi; Bác sĩ xem | HC không thấy `nguyenNhan` (`truongBiAn`); Bác sĩ thấy | FR-069, BR-M02-21 |
| Q38 | QL tạo phiên bản đơn giá mới từ 01/12; tạo phiên bản chồng khoảng; sửa phiên bản đã tham chiếu; tạo phiên bản hiệu lực lùi cho khoảng trống | Phiên bản cũ kết thúc 30/11; 409; trigger chặn; được, cắt trước phiên bản kế tiếp | US9 #1 → #3, #6, DBR-08 |
| Q39 | Hợp đồng ngắn ngày kết thúc 20/10, tua tới 21/10 và 29/10 | 20/10 nhắc; 21/10 dấu "quá hạn hợp đồng", nhắc hằng ngày; quá 7 ngày: báo QL đúng một lần | US3 #8, FR-029, SC-011 |
| Q40 | Dừng Bộ lập lịch 3 ngày khi C đang vắng; khởi động lại | Ba ngày vắng được tra và ghi theo thứ tự; dấu đặt đúng ngày; nhắc gộp một lần | FR-077, BR-M02-19 |
| Q41 | *(Bổ sung 2026-10-11, Q-343)* A có hợp đồng Hiệu lực, cọc đạt, giường, đánh giá, mức trùng nhưng chưa có người đại diện (bản giả 012); xem điều kiện tiếp nhận; Hoàn tất tiếp nhận; thêm người đại diện rồi làm lại | (f) "Chưa đạt", lệnh 409 liệt kê (f); sau khi thêm thì sáu điều kiện "Đạt", 200 | US1 #3, FR-004 (f), DBR-02 |
| Q42 | *(Bổ sung 2026-10-11, Q-338)* HC lập yêu cầu chỉ bớt và thêm dịch vụ thuộc gói, rồi tự duyệt có lý do; HC lập yêu cầu thêm dịch vụ ngoài gói và thử duyệt; QL duyệt yêu cầu đó | Yêu cầu 1: `thayDoiNho = true`, 200, nhật ký `tuDuyet`; yêu cầu 2: `thayDoiNho = false`, HC 403, `lenhDuocPhep` của HC không có `duyet`; QL 200 | US5 #6, #9, FR-043 |

## 4. Test tự động: truy vết kịch bản → test

| Spec | Lớp test (backend, `com.cambae.backend.m02…`) | Loại |
| --- | --- | --- |
| US1, FR-001 → FR-006 | `LuotDangKyIT`, `CapNhatLuotDangKyIT`, `FR_004_DanhSachKhopLenhIT`, `BR_M02_22_MucHopDongIT`, `TienTrinhIT` | Tích hợp |
| US2, FR-007 → FR-018 | `BR_M02_10_DiemUuTienTest`, `DiemHangNgayIT`, `DieuChinhDiemIT`, `BR_M02_01_DanhSachPhuHopIT`, `FR_012_LuotTrongIT`, `BR_M02_02_GiuChoIT`, `FR_002_TuChoiHuyTiepNhanIT`, `BR_M02_03_NhacHoSoChoIT`, `GhiNhanLienHeDongThoiIT` | Đơn vị, tích hợp, đồng thời |
| US3, FR-019 → FR-029 | `HopDongVongDoiIT`, `HopDongKhoaNoiDungDbIT`, `DBR_06_HopDongKhongChongIT`, `FR_021_GhiNhanDaKyIT`, `XacDinhKhacChuanTest`, `DuyetDieuKhoanIT`, `BR_M02_09_NoiDungTaiNgayIT`, `BR_M02_05_NhacHopDongIT`, `FR_029_QuaHanHopDongIT` | Đơn vị, tích hợp, cơ sở dữ liệu |
| US4, FR-030 → FR-033b | `DatCocDieuKienIT`, `FR_032_DatCocSauLuuTruIT`, `SuKien017018IT` | Tích hợp |
| US5, FR-037 → FR-047 | `BR_M02_04_ApDungPhuLucIT`, `DBR_07_PhuLucIT`, `FR_041_KiemTraKhiDuyetIT`, `FR_046_TrungThuocTinhIT`, `ApDungKhongThanhIT`, `YeuCauNguoiDaiDienIT`, `LaThayDoiNhoTest`, `FR_043_ThayDoiNhoIT` | Tích hợp, đồng hồ giả lập |
| US6, FR-048 → FR-061 | `BR_M02_06_TraChinhSachTest` (phủ bảng 6.7), `NgayVangIT`, `BR_M02_07_QuyetDinhGiuGiuongIT`, `BR_M02_17_QuanLyQuyetDinhNgay1Test`, `BR_M02_18_DinhChinhVuotNguongIT`, `FR_057_ChuyenVienKhiTamVangIT`, `BanTruHeSoIT`, `NgayNghiBanTruIT` | Đơn vị, tích hợp |
| US7, FR-062 → FR-068 | `HoSoKetThucIT`, `DieuKienKetThucIT`, `NgoaiLeKetThucIT`, `FR_066_KyCuoiIT`, `FR_066a_QuaNgayDuKienIT` | Tích hợp |
| US8, FR-069 → FR-073 | `GhiNhanQuaDoiIT`, `DanhSachViecSauQuaDoiIT`, `BR_M02_21_NguyenNhanTuVongIT` | Tích hợp |
| US9, FR-034 → FR-036 | `DichVuIT`, `DBR_08_PhienBanDonGiaIT`, `HopDongLechGiaIT` | Tích hợp, cơ sở dữ liệu |
| Mục J, K | `NgayLuuTruApiIT` (chưa vào ở, mốc dừng, ngày qua đời), `BR_M02_19_ChayBuIT` | Tích hợp, đồng hồ giả lập |
| SPI | `HopDongApiSpiIT` (bản cài cho 001), `NguonLuuTruChoPhanBoIT` (cho 003), `BeanThamGiaChuyenTrangThaiM02IT` (mỗi lệnh, rollback) | Tích hợp |
| Edge Cases | Mỗi gạch đầu dòng một phương thức trong các lớp trên | |
| Client web | `DieuKienTiepNhan.test.jsx`, `DieuKienKetThuc.test.jsx`, `HoSoPhuHop.test.jsx` (không chọn sẵn, lý do khi bỏ qua thứ tự), `HopDong.test.jsx` (`truongBiAn` tài chính) | Vitest |
| App nhân viên | `cho_tam_vang_test.dart`, `gia_han_du_kien_test.dart` | flutter_test |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

- SC-001, SC-002, SC-004 → SC-008, SC-011, SC-012: test tự động ở mục 4 trên MySQL thật và đồng hồ giả lập. Với SC-008, 004 kiểm sự kiện qua đời kèm thời điểm được phát cho 005, 006, 009, 010, 011, 012 trong cùng lệnh (qua 001 và sự kiện của 004).
- SC-003: đo từ commit của lệnh làm giường về Trống (003) tới lúc thông báo tới Hành chính, dữ liệu quy mô NFR-01; chỉ tiêu 5 phút.
- SC-009, SC-010: đo trên ít nhất 10 lần của Hành chính đã được hướng dẫn, dữ liệu quy mô NFR-01, lấy trung vị.

## 6. Kịch bản bổ sung 2026-10-22 (Q-379, Q-385)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q38 | Hồ sơ kết thúc của A; bản giả 017 trả `KT_H_QUYET_TOAN` chưa đạt và `KT_H_XAC_NHAN_QUYET_TOAN` chưa đạt; xem điều kiện | Dòng (h) "Chưa đạt" kèm hai lý do | FR-063 (h) |
| Q39 | Kế toán lập ngoại lệ (h) với `KT_H_QUYET_TOAN`, Quản lý viện duyệt; bản giả 017 vẫn trả `KT_H_XAC_NHAN_QUYET_TOAN` chưa đạt; Kết thúc lưu trú | (h) vẫn "Chưa đạt" (lý do "chưa xác nhận quyết toán"); lệnh 409; bản giả 017 nhận `NgoaiLeKetThucDaDuyet`, `coNgoaiLeQuyetToanDaDuyet` = true | FR-064 |
| Q40 | Bản giả 017 chuyển `KT_H_XAC_NHAN_QUYET_TOAN` đạt; xem điều kiện; Kết thúc lưu trú (các điều kiện khác đạt) | (h) "Đạt (ngoại lệ)"; 200 | FR-063, FR-064, FR-065 |
| Q41 | Lập ngoại lệ với `maDieuKien = KT_H_XAC_NHAN_QUYET_TOAN` | 422 | FR-064 |
| Q42 | Bản giả 017 gọi `HoSoKetThucDocApi.thongTinQuyetToan(A)` khi hồ sơ kết thúc Đang chuẩn bị; rồi với người qua đời B | Trả loại lưu trú, hợp đồng, ngày bắt đầu, ngày kết thúc dự kiến, trường hợp; B trả `QUA_DOI` kèm ngày qua đời | 017 FR-034 |

Test: `NgoaiLeKetThucIT` mở rộng (Q39, Q41), `DieuKienKetThucIT` mở rộng (Q38, Q40), `HoSoKetThucDocApiIT` (Q42).
