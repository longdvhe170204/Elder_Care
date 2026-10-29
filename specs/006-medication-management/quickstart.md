# Quickstart: Kiểm chứng feature 006

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-006.yaml](contracts/api-006.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 005: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản và cơ cấu của quickstart 002, 003, 005: Quản lý viện QL, Trưởng tầng H (tầng 2), Bác sĩ B (giấy phép kê đơn còn hiệu lực) và B2 (giấy phép hết hạn hôm qua), Điều dưỡng D, D3, D4 (tầng 2, giấy phép phát thuốc còn hiệu lực), D2 (giấy phép hết hạn), D5 (tầng 3), Nhân viên chăm sóc S, Hành chính HC; P giữ nhiệm vụ Người phụ trách ca tầng 2;
  - cơ sở có phạm vi khám chữa bệnh còn hiệu lực (bản giả `GiayPhepCoSoApi` của 007, bật tắt được);
  - ca ngày 07:00–18:00, ca đêm 18:00–07:00 tầng 2 (bản giả `DieuDuongCaPort`); phân công: D phụ trách A, C;
  - người cao tuổi: A, C nội trú Đang lưu trú tầng 2; A có dị ứng "Penicillin" (danh mục) và một mục dị ứng "khác"; hồ sơ E Đang tiếp nhận có 2 thuốc đang dùng khi tiếp nhận (001);
  - danh mục thuốc mẫu: Amlodipin 5 mg, Amlodipin + valsartan, Amoxicillin, Metformin 500 mg, Donepezil 5 mg, Paracetamol 500 mg, Insulin nhanh (cửa sổ ±15 phút), Morphin (kiểm soát đặc biệt), Clopidogrel 75 mg; Amlodipin có dịch vụ `THUOC_NGUON_VIEN` ở 004;
  - CFG theo mặc định Phụ lục 25: CFG-M07-01 ±30 phút, CFG-M07-02 30 phút, CFG-M07-04 3 ngày, CFG-M07-05 4 giờ, CFG-M05-04 2, CFG-M15-08 24 giờ;
  - bản giả có công tắc qua `/api/v1/kiem-thu/gia-lap/{module}` (chỉ `test`, `demo`): `DieuDuongCaPort` (008), `ChuyenDiThuocPort` (014), `DeNghiMuaHoPort` (010), `QuanHeNguoiThanApi`, `NguoiDaiDienPort` (012); bộ ghi sự kiện để xem sự kiện gửi 007, 009, 010.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (không có vòng 006 ↔ 007, 006 ↔ 008, 006 ↔ 014), test cơ sở dữ liệu (data-model mục 11), test đồng thời và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | B kê đơn nội bộ Metformin 500 mg 07:00, 19:00 từ 01/10 cho A | 201, người kê B, cơ sở kê là viện, `HIEU_LUC` từ 01/10 | US1 #1, BR-M07-05 |
| Q2 | B2 kê đơn nội bộ; B2 nhập đơn Bệnh viện X đủ cơ sở kê; tắt phạm vi cơ sở rồi D nhập đơn bên ngoài thiếu cơ sở kê | 403 `DIEU_KIEN_PHAP_LY`; 201; 422 | US1 #2, #3 |
| Q3 | Nhập Amoxicillin cho A không lý do, rồi có lý do; nhập Amlodipin + valsartan khi đã có Amlodipin; nhập Metformin mới | 409 `CAN_XAC_NHAN_VAN_DE` (mục Penicillin, mức độ); 201 kèm vấn đề đã xác nhận; 409 trùng "amlodipin"; mục dị ứng "khác" có trong `canXemXet` không chặn | US1 #4 → #6, SC-003 |
| Q4 | A có đơn Amoxicillin; bản giả 001 phát `DiUngDaGhiNhan` Penicillin | 007 nhận `YeuCauCanhBao(XUNG_DOT_DI_UNG)` cho đơn đó; đơn vẫn `HIEU_LUC`; 001 nhận `daKiemTra` | US1 #7, FR-012 |
| Q5 | S mở đơn thuốc của A; H mở đơn; H mở lịch thuốc | 403; 403; 200 chỉ liều và trạng thái | US1 #8, FR-049 |
| Q6 | Đơn Amlodipin 08:00 hằng ngày; xem lịch 7 ngày; đếm `lieu_thuoc` có `cua_so_tu` ngày mai | Mỗi ngày đúng một liều 08:00, cửa sổ 07:30–08:30; Insulin 11:30 cửa sổ 11:15–11:45; 0 dòng | US2 #1, #2, SC-001 |
| Q7 | Tua 07:30 | Dòng liều được vật hóa `DEN_GIO`, có trong `LieuThuocChecklistPort.theoCa` cho D | US2 #3 |
| Q8 | D xác nhận Đã dùng 08:10; D3 xác nhận lại cùng liều | 200, `SuKienTinhPhiThuoc(LIEU)` cho 010; 409 `LIEU_DA_XAC_NHAN` kèm người, thời điểm | US2 #4, #5, DBR-13 |
| Q9 | Liều 20:00 không xác nhận; tua 20:30, 21:00; liều khác Trễ, D xác nhận 20:40 | `TRE`, D nhận `NhacLieuTre` một lần; `BO_LO`, 007 nhận `YeuCauCanhBao(BO_LO_LIEU)`; Đã dùng nhãn "dùng trễ" | US2 #6, #7, SC-004 |
| Q10 | A Từ chối Amlodipin hôm qua và hôm nay (có lý do); Từ chối không lý do; Đã dùng có phản ứng | 007 nhận `TU_CHOI_LIEN_TIEP`; 422; 007 nhận `PHAN_UNG_THUOC` | US2 #8, #9, BR-M07-13 |
| Q11 | S, H, D2 xác nhận liều | 403 mỗi người | US2 #10, SC-008 |
| Q12 | Bản giả 008 bỏ phân công A ca ngày; tua 07:30; D4 (tầng 2) xác nhận; D5 (tầng 3) xác nhận | H, P nhận `NctChuaCoDieuDuongPhuTrach` một lần; D4 200 căn cứ `LIEU_CHUNG_TANG`; D5 403 | US2 #11, FR-026a, SC-011 |
| Q13 | Bản giả 008 không còn Điều dưỡng nào có ca tầng 2 | H, QL nhận `TangKhongCoDieuDuong` kèm liều sắp tới giờ; liều vẫn Trễ, Bỏ lỡ | BR-M07-21 |
| Q14 | Morphin 22:00: gửi bản ghi ngoại tuyến; ghi trực tuyến lúc 22:50 với `ghiSauMatKetNoi`, thời điểm dùng 22:05; P xem lại | `TU_CHOI` `BAT_BUOC_TRUC_TUYEN`; `DA_DUNG` không nhãn trễ, `xem_lai_lieu` `CHO_XEM_LAI` cho P, 007 nhận yêu cầu đóng cảnh báo nếu có; P gắn `CHAP_NHAN` | US2 #12, FR-029, SC-012 |
| Q15 | Hai thiết bị ngoại tuyến cùng ghi Đã dùng một liều, đồng bộ lần lượt | Bản đầu đóng liều; bản sau `TU_CHOI` kèm người đóng; 007 nhận `NGHI_DUNG_TRUNG_LIEU` | BR-M07-20 |
| Q16 | Sửa liều đơn Amlodipin qua API và `UPDATE` trực tiếp; sửa ghi chú | 404/405 (không có endpoint); trigger chặn; 200, nhật ký trước/sau | US3 #1, #2, SC-002 |
| Q17 | B đổi liều 5 → 10 mg từ 14:00 hôm nay | Cùng lệnh: đơn cũ `DA_NGUNG` "thay thế", đơn mới `HIEU_LUC` trỏ đơn cũ; liều 08:00 hôm nay giữ Đã dùng; lịch ngày mai: 5 mg Đã hủy, 10 mg Chưa đến giờ | US3 #3, DBR-14 |
| Q18 | B đổi liều từ 07:00 ngày mai rồi ngừng đơn 10 mg hôm nay | Đơn cũ thôi bị ngừng, lịch có liều 5 mg ngày mai | US3 #10, BR-M07-15 |
| Q19 | Ngừng đơn có liều Đến giờ và liều Trễ; ngừng không lý do | Đến giờ, Chưa đến giờ → Đã hủy; Trễ đi tiếp tới Bỏ lỡ; 422 | US3 #5, #6 |
| Q20 | Đơn kết thúc 10/10; tua 07/10, 11/10; đơn Tạm dừng có ngày kết thúc | Nhắc người kê (Nhẹ); `HET_HAN`, không liều sau 10/10; Tạm dừng → Hết hạn, Tiếp tục bị 409 | US3 #7 → #9, BR-M07-08 |
| Q21 | A có 3 đơn; Chuyển viện rồi Ghi nhận trở về 10:00 (001) | Ba đơn `TAM_DUNG` "chờ đối chiếu", D và Bác sĩ nhận `CanLapPhieuDoiChieu`; không có phiếu nào tự tạo | US4 #1, SC-005 |
| Q22 | D lập phiếu, gửi khi thiếu dòng một đơn; thêm đủ dòng (Thay đổi liều, Tiếp tục, Ngừng, Thêm mới); gửi; D tự xác nhận; D3 xác nhận khi có dòng tạo đơn nội bộ; B xác nhận | 409 `THIEU_DONG`; `CHO_XAC_NHAN`; 409 `QUY_TAC_HAI_NGUOI`; 403 `CAN_BAC_SI_KE_DON`; `DA_XAC_NHAN`, áp từng dòng trong một lệnh, liều từ thời điểm xác nhận | US4 #2 → #4, #9, SC-014 |
| Q23 | Không xác nhận phiếu; tua 14:00 | 007 nhận `CHUA_DOI_CHIEU` một lần; phiếu vẫn xử lý được | US4 #5, BR-M07-09 |
| Q24 | E Đang tiếp nhận: D lập phiếu "tiếp nhận" lúc 09:00, B xác nhận 09:30; HC Hoàn tất tiếp nhận 14:00 | Đơn Insulin `CHO_HIEU_LUC` "chờ tiếp nhận", không liều, không cảnh báo; 14:00 `HIEU_LUC`, liều từ 14:00, không đơn nào tạm dừng | US4 #6, #10, FR-034a |
| Q25 | Bản giả 001 phát `HoSoSucKhoeBanDauDaDoi` khi phiếu tiếp nhận Mở | Người thực hiện được báo; phiếu không đổi; dòng thuốc đã bỏ có dấu khi đọc | BR-M07-22 |
| Q26 | A Cho tạm vắng 09:00 "không mang thuốc"; trở về 15:00 | Liều 12:00 Tạm dừng, không nhắc, không cảnh báo; sau 15:00 Chưa đến giờ; 12:00 kết thúc ở Tạm dừng | US5 #1, #2, BR-M07-03 |
| Q27 | Bản giả 014 điểm danh rời viện A 07:00 → 13:00; S ghi liều Mang theo; về 13:30, D ghi theo báo lại của S | Liều 08:00, 12:00 Mang theo; 403; Đã dùng căn cứ `GHI_THEO_BAO_LAI`, người báo lại S | US5 #3 → #6, FR-027 |
| Q28 | Tạm vắng 3 ngày có mang thuốc: D giao 3 viên Donepezil (lô 20) và 3 viên Amlodipin; về sau 2 ngày, ghi 2 liều theo báo lại, Nhận lại 1 viên mỗi thuốc | Lô 17; `SuKienTinhPhiThuoc(LAN_GIAO, 3)`; liều không trừ, không tính phí; lô 18; `SuKienTinhPhiThuocDaDieuChinh(2)` | US5 #8, FR-048a |
| Q29 | Trở về sớm 15:00 khi liều 20:00 thuộc lần giao; D xác nhận 20:00 trước khi Nhận lại | Liều 20:00 thôi thuộc lần giao, Chưa đến giờ; xác nhận được, phản hồi kèm nhắc Nhận lại; tính phí 1 viên | US5 #9, Q-60 |
| Q30 | Đơn PRN Paracetamol 4 giờ, tối đa 4: 08:00; thử 11:00; 12:00, 16:00, 20:00; thử 00:30; 08:05 hôm sau; lần không lý do | 409 `somNhatDuocDung` 12:00; 409 08:00; 201; 422 | US6 #1 → #4, SC-006 |
| Q31 | PRN ghi theo báo lại vượt giới hạn; PRN khi đơn Tạm dừng | 201 `vuotGioiHan`, 007 nhận cảnh báo; 409 | Edge Case, FR-024 |
| Q32 | Tiếp nhận 30 viên Donepezil có toa; gắn đơn; xác nhận liều; ghi Báo gia đình sắp hết; tiếp nhận hộp không toa *(Sửa 2026-10-29, Q-393)* | `CHO_DOI_CHIEU`; `DUOC_SU_DUNG`; liều không chọn lô, số lượng lúc tiếp nhận không đổi; 009 nhận một `ThuocGuiSapHet` cho mỗi lần báo; hộp không toa chỉ `CHI_GIU_HO` | US7 |
| Q33 | Lô hạn 30/09, tua 01/10; xác nhận liều từ lô đó; lô về 0 rồi đến liều | `CHI_GIU_HO` "hết hạn dùng", thông báo; 409 `LO_KHONG_HOP_LE`; `DA_DUNG_HET`, 409 `CHUA_CO_THUOC`, ghi Không thực hiện | US7 #5, #8 |
| Q34 | Hoàn trả 9 khi còn 10; Kiểm kê điều chỉnh về 9 rồi hoàn trả 9; Kết thúc lưu trú khi còn lô Chỉ giữ hộ | 409 `SO_LUONG_KHAC_SO_CON`; `DA_HOAN_TRA`; 409 `KT_C_THUOC_GUI` (miễn được) | US7 #7, BR-M07-19, FR-047 |
| Q35 | Người thân không có bản đồng ý xem thuốc gửi, đơn, phiếu; có bản đồng ý | Thấy thuốc gửi, không thấy đơn, phiếu; thấy đơn và phiếu Đã xác nhận, không thấy liều | US7 #9, SC-013 |
| Q36 | D đính chính Đã dùng (gia đình gửi) → Không thực hiện; H đính chính; D đính chính Bỏ lỡ → Đã dùng 08:20 | Bản gốc còn, hiện hành Không thực hiện, lô +1; 403; hiện hành Đã dùng, 007 nhận yêu cầu đóng cảnh báo | US8 #1 → #5 |
| Q37 | Liều nguồn viện đã có khoản (bản giả 010) bị đính chính khỏi Đã dùng | 010 nhận `SuKienTinhPhiThuocDaDinhChinh` | US8 #3, DBR-17 |
| Q38 | A qua đời 09:10 có liều Đến giờ 09:00, Tạm dừng, phiếu Mở, lô Chỉ giữ hộ, lần giao Chờ nhận lại | Cùng lệnh: đơn Đã ngừng; Đến giờ → Không thực hiện (Hệ thống); Tạm dừng → Đã hủy; phiếu Đã hủy; lô, lần giao giữ nguyên; `THUOC_GUI` Chưa hoàn thành; không có trong `LieuBanGiaoApi` | Edge Case, FR-047, SC-010 |
| Q39 | Dừng backend 3 giờ (đồng hồ giả lập) rồi khởi động | Liều trong khoảng được vật hóa với mốc dự kiến; Trễ, Bỏ lỡ bù; nhắc Trễ gộp một lần mỗi người | D-19, NFR-04 |
| Q40 | *(Bổ sung 2026-10-11, Q-340)* A Đang lưu trú có hai đơn Hiệu lực; D lập phiếu "theo yêu cầu chuyên môn" không lý do, rồi có lý do; ghi Ngừng một đơn, Tiếp tục đơn kia, gửi; B xác nhận | 422; rồi `MO`, không đơn nào `TAM_DUNG`, `hanLuc` rỗng; xác nhận: một đơn `DA_NGUNG`, đơn kia giữ `HIEU_LUC` | US4 #11, FR-034b, BR-M07-25 |
| Q41 | *(Bổ sung 2026-10-11, Q-337)* Bản giả 019 trả dòng Amlodipin đã ghi đã mua; D ghi tiếp nhận Đạt | 201; đơn, liều không đổi; không có `SuKienTinhPhiThuoc`; `daNhan` khi mọi dòng Đạt | US9 #1, FR-054 |
| Q42 | Dòng thứ hai: D ghi Không đạt thiếu sai lệch; đủ sai lệch, lý do; ghi lần giao bù Đạt | 422; 201 và `TiepNhanThuocKhongDat` cho người lập và Phụ trách bộ phận hiện tại của bộ phận của yêu cầu (đổi Phụ trách bộ phận sau khi ghi đã mua thì người mới nhận); 201 lần mới | US9 #2, FR-055 |
| Q43 | Bản giả trả rỗng (yêu cầu chưa ghi đã mua); D ghi tiếp nhận; D xác nhận liều của đơn chưa có lần tiếp nhận | 409; xác nhận liều 200 | US9 #3, BR-M11-17 |

## 4. Test tự động: truy vết kịch bản → test

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `ChuoiThoiDiemDonTest`, `BR_M07_01_LichThuocTest`, `BangChuyenLieuTest`, `BR_M07_06_TrungHoatChatDiUngTest`, `BR_M07_04_GioiHanPrnTest`, `BR_M07_13_TuChoiLienTiepTest`, `XacDinhNguoiPhuTrachLieuTest` | Q3, Q6, Q10, Q12, Q30, Q32 |
| Đơn thuốc | `BR_M07_05_QuyenKeDonIT`, `FR_012_DiUngMoiIT`, `DBR_14_DonThayTheIT`, `BR_M07_07_DoiLieuIT`, `BR_M07_15_HuyDoiLieuIT`, `BR_M07_16_CanCuCoSoKeIT`, `BR_M07_08_HetHanDonIT`, `DonThuocTriggerIT` | Q1 → Q5, Q16 → Q20 |
| Lịch, liều | `SC_001_BayNgayIT`, `DBR_13_LieuDuyNhatIT`, `BR_M07_02_TreBoLoIT`, `BR_M07_12_XacNhanMotLanIT`, `FR_026_QuyenXacNhanIT`, `FR_026a_LieuChungTangIT`, `BR_M07_21_TangKhongDieuDuongIT`, `PhanUngIT`, `LieuDongThoiIT` | Q6 → Q13 |
| Ngoại tuyến, kiểm soát đặc biệt | `FR_029_KsdbGhiSauMatKetNoiIT`, `FR_029_DongBoMuonIT`, `FR_029_HaiBanNgoaiTuyenIT` | Q14, Q15 |
| Đối chiếu | `BR_M07_09_DoiChieuIT`, `FR_034a_PhieuTiepNhanTruocIT`, `FR_038_HaiNguoiIT`, `FR_037_ChanNgoaiPhieuIT`, `ThuocDangDungDoiIT`, `BR_M07_17_DinhChinhPhieuIT`, `BR_M07_25_DoiChieuChuyenMonIT` | Q21 → Q25, Q40 |
| Tiếp nhận thuốc cung ứng *(bổ sung 2026-10-11)* | `BR_M07_24_TiepNhanThuocCungUngIT` | Q41 → Q43 |
| Vắng, mang theo | `BR_M07_03_VangMatIT`, `FR_027_MangTheoBaoLaiIT`, `FR_048a_GiaoNhanLaiIT`, `FR_048a_TroVeSomIT`, `FR_048b_KhongMangThuocIT`, `BR_M07_22_NhieuLanGiaoIT` | Q26 → Q29 |
| PRN | `PrnDongThoiIT`, `FR_027_PrnVuotGioiHanIT` | Q30, Q31 |
| Thuốc gửi | `BR_M07_10_ThuocGuiChuaDoiChieuIT`, `ThuocGuiSapHetIT` (Báo gia đình sắp hết), `BR_M07_19_HoanTraIT`, `ThuocMuaHoIT` | Q32 → Q34 |
| Quyền xem | `FR_049_NguoiThanXemThuocIT`, `FR_049_TruongTangChiXemLieuIT` | Q5, Q35 |
| Đính chính, chi phí | `FR_030_DinhChinhLieuIT`, `FR_030_DinhChinhChiPhiIT`, `BR_M07_14_SuKienTinhPhiIT` | Q8, Q28, Q36, Q37 |
| Trạng thái cuối | `FR_047_TrangThaiCuoiIT`, `KT_C_ThuocGuiIT` | Q34, Q38 |
| Bộ lập lịch | `ChayBuM07IT` | Q39 |
| Cơ sở dữ liệu | `DonThuocTriggerIT`, `LieuTrangThaiCuoiTriggerIT`, `PhieuDoiChieuTriggerIT`, `ThuocGuiSoLuongTriggerIT`, `NhomBaTriggerIT` (000) mở rộng | Q16 |
| Kiến trúc | `ApplicationModules.verify()`, ArchUnit cấm gọi API của 007, 008, 010, 014, 019 (M11) trực tiếp từ `m07` (chỉ qua SPI mục 2.1) | |
| Web (Vitest) | Màn nhập đơn hiện vấn đề cần lý do; phiếu đối chiếu không có nút chép sẵn, lỗi thiếu dòng; thuốc gửi không chọn sẵn lô | Q3, Q22, Q32 |
| App (`flutter_test`) | `XacNhanLieuNgoaiTuyenTest`, `KsdbKhongVaoHangDoiTest`, `ChonLoThuocGuiTest`, `KetQuaDongBoThuocTest` (BR-M15-20), `BanGhiLieuBiTuChoiTest` | Q14, Q15, Q32 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | `SC_001_BayNgayIT`: 20 người, 60 đơn, 7 ngày giả lập; so lịch với danh sách tính tay (thời điểm, cửa sổ, trạng thái) 100%; 0 trùng; 0 dòng `lieu_thuoc` có `cua_so_tu` sau đồng hồ mà không gắn lần giao |
| SC-002 | Q16, Q17 và trigger: 0 đơn hiệu lực đổi trường bị khóa; mỗi lần đổi liều có đơn thay thế trỏ đơn cũ; 0 kết quả Đã dùng đổi |
| SC-003 | 30 đơn có vấn đề và 30 đơn không vấn đề trong `BR_M07_06_TrungHoatChatDiUngTest` và IT |
| SC-004 | Q9, Q20, Q23, Q33 với đồng hồ giả lập: chênh giữa mốc và chuyển trạng thái ≤ 1 phút; 0 liều Tạm dừng có cảnh báo |
| SC-005 | Q21, Q24: mỗi lần tiếp nhận, trở về có `CanLapPhieuDoiChieu`; 0 phiếu `nguoi_lap_id` là Hệ thống; 0 liều Đến giờ khi chưa có phiếu Đã xác nhận |
| SC-006 | Q30, `PrnDongThoiIT` |
| SC-007 | Q32 → Q34, Q28 *(Sửa 2026-10-29, Q-393)*: 0 liều Đã dùng khi đơn không còn lô Được sử dụng chưa hết hạn; số lượng lúc tiếp nhận không đổi; đúng một `ThuocGuiSapHet` mỗi lần Báo gia đình sắp hết |
| SC-008 | Q11 và `FR_026_QuyenXacNhanIT` |
| SC-009 | Đo trên app: chạm liều → lưu ≤ 10 giây (nguồn viện, không kiểm soát đặc biệt, trực tuyến); `GET /lieu-thuoc/vong-phat-thuoc` 15 người ≤ 3 giây p95 (dữ liệu 300 người) |
| SC-010 | Q38 |
| SC-011 | Q12 |
| SC-012 | Q14: mọi `xem_lai_lieu` có kết quả trước lần xác nhận bàn giao kế tiếp trong kịch bản (xem plan điểm báo lại 4) |
| SC-013 | Q35 |
| SC-014 | Q22, Q24 |
