# Quickstart: Kiểm chứng feature 007

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-007.yaml](contracts/api-007.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 006: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản và cơ cấu của quickstart 002, 003, 005, 006: Quản lý viện QL, Trưởng tầng T (tầng 2), Bác sĩ B, BS1 (giấy phép còn hiệu lực), Điều dưỡng D, D2 (tầng 2), D5 (tầng 3), Nhân viên chăm sóc S, Hành chính H, Nhân viên bếp K; P giữ nhiệm vụ Người phụ trách ca tầng 2;
  - ca ngày 07:00–18:00, ca đêm 18:00–07:00 tầng 2 (bản giả `NhanSuCanhBaoPort`); D phụ trách A, C; Bác sĩ trực ca ngày là B, ca đêm không có ai;
  - người cao tuổi: A, C, E nội trú Đang lưu trú tầng 2 (A ở P201 cùng E); A có nguyện vọng Hiệu lực "đưa về nhà", dị ứng Penicillin, hai đơn đang dùng (bản giả `DonThuocApi` hoặc dữ liệu 006), lưu ý chăm sóc "nguy cơ ngã cao"; người liên hệ chính của A là M (bản giả `NguoiThanKhanCapPort`), M không thuộc bản đồng ý;
  - CFG theo mặc định Phụ lục 25: CFG-M05-01 15 phút, CFG-M05-02 3 lần/24 giờ, CFG-M05-03 5% trong 30 ngày, CFG-M05-05 3, CFG-M05-06 4 giờ trong 72 giờ, CFG-M05-07 5 ngày, CFG-M05-08 2 lần/ngày trong 7 ngày, CFG-M06-01 7 ngày, CFG-M06-04 10 phút, CFG-M06-03 với SpO2 50–100;
  - ngưỡng mặc định SpO2: cảnh báo dưới 94, nguy hiểm dưới 90;
  - bản giả có công tắc qua `/api/v1/kiem-thu/gia-lap/{module}` (chỉ `test`, `demo`): `NhanSuCanhBaoPort` (008), `NguoiThanKhanCapPort` (012), `DiemDanhTruyVetPort` (014), `NhatKyThongBaoPort` (009); bộ ghi sự kiện để xem sự kiện gửi 001, 003, 005, 009, 011, 012.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (chiều `m05 → m06`; không có vòng 007 ↔ 008, 007 ↔ 009, 007 ↔ 012, 007 ↔ 014), test cơ sở dữ liệu (data-model mục 12), test đồng thời và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | S ghi SpO2 cho A: 101; 96; 92 | 422 `NGOAI_KHOANG_VAT_LY` (50–100), không lưu; lưu, Bình thường, không cảnh báo; lưu, Cảnh báo, cảnh báo Trung bình `VUOT_NGUONG:SPO2` cho D, hạn 15 phút | US1 #1, #2, SC-001 |
| Q2 | S ghi 88; ghi lần đo lại 87 | Bản ghi `CHO_DO_LAI`, chưa có cảnh báo; lần đo lại xác nhận, bản ghi đầu `DA_DO_LAI`, cảnh báo gộp nâng lên Khẩn cấp; D, P, B, T nhận cùng lúc; M và QL không nhận; không có sự cố | US1 #3, #10, FR-047a, SC-016 |
| Q3 | S ghi 88 chọn "xử lý ngay"; ghi 88 khác rồi tua 10 phút; ghi 88 rồi đo lại 95 | Xác nhận ngay + Khẩn cấp; xác nhận nhãn `CHUA_DO_LAI` + Khẩn cấp; lần đo lại không cảnh báo, bản ghi đầu còn xem được | US1 #4 → #6 |
| Q4 | 005 ghi SpO2 qua `ChiSoPort` từ công việc đo, giá trị 88 | `CHO_DO_LAI`; công việc chưa đóng; sau đo lại, 005 nhận `daXacNhan` | FR-018, BR-M04-10 |
| Q5 | Huyết áp 150/95, tâm thu vượt cảnh báo, tâm trương vượt nguy hiểm | Xếp Nguy hiểm, yêu cầu đo lại | US1 #8 |
| Q6 | H mở chỉ số của A | 403 | US1 #9 |
| Q7 | B lưu ngưỡng cá nhân nhiệt độ A hiệu lực 08:00 mai; cận dưới cảnh báo < cận dưới nguy hiểm; D thử tạo ngưỡng | `CHO_HIEU_LUC`, 08:00 `HIEU_LUC`; 422 `NGUONG_KHONG_HOP_LE`; 403 | US2 #1, #2, #7 |
| Q8 | B tạo phiên bản 2 hiệu lực ngay; bản ghi đã so theo phiên bản 1 | Phiên bản 1 `HET_HIEU_LUC` cùng thời điểm; kết quả so cũ giữ nguyên | US2 #3, FR-023 |
| Q9 | E Đang lưu trú 8 ngày, chưa có ngưỡng cá nhân; tua 00:15 hai ngày liền | `NhacThieuNguongCaNhan` một lần cho mọi Bác sĩ; E có trong danh sách cả hai ngày | US2 #4, BR-M06-02 |
| Q10 | B đặt lịch đo huyết áp 07:00, 17:00 từ mai; P lập công việc 07:00 (căn cứ `LAN_DO`) và 09:00 | Lịch Hiệu lực; 005 chấp nhận 07:00 mức Bắt buộc; 09:00 bị chặn (không thuộc chuỗi); không có công việc tự sinh | US2 #5, FR-004 |
| Q11 | B đổi lịch sang 3 lần/ngày từ mai; B ngừng lịch khác | Lịch cũ `DA_NGUNG`, lịch mới trỏ lịch cũ, 005 nhận `nguonDaDoi`; 005 nhận `nguonBiHuy` | US2 #6, FR-005 |
| Q12 | Cảnh báo Trung bình 09:00 không ai nhận; tua 09:15, 09:30, 09:45, 10:00 | Cấp 1 (T, B), hạn 09:30; cấp 2 (QL); dấu "đã leo thang tối đa", `NhacLeoThangToiDa` #1; #2 lúc 10:00, không thêm dòng leo thang | US3 #1, #2, SC-004, Q-248 |
| Q13 | QL tiếp nhận khi cấp 1; khi cấp 2; QL giao cho D2 kèm ghi chú; QL thử đóng | 403; 200; D2 là người phụ trách; 403 | US3 #2, Q-71 |
| Q14 | T tiếp nhận lúc 09:20; tua 09:30 | `DA_TIEP_NHAN`, không leo thang; thời gian tới tiếp nhận 20 phút | US3 #3 |
| Q15 | Gửi 10 `YeuCauCanhBao` cùng khóa cho A cùng lúc | Một cảnh báo đang mở, `so_lan` = 10 | US3 #9, SC-003, DBR-18 |
| Q16 | Ba phát sinh Trung bình cùng khóa trong 24 giờ; D nâng mức không lý do, có lý do | `CanhBaoLapLai(3)` cho D và B; không đề xuất; 422; Khẩn cấp | US3 #6, FR-038a |
| Q17 | D đóng không kết quả; có kết quả; phát sinh mới cùng khóa | 422; `DA_DONG`; cảnh báo mới | US3 #7, #8 |
| Q18 | 008 gọi `khiXacNhanBanGiao` ca ngày → ca đêm | Cảnh báo của D chuyển sang điều dưỡng ca đêm; cảnh báo do B phụ trách giữ nguyên | US3 #10, CHK016 |
| Q19 | Tới 18:00 bàn giao chưa xác nhận | Cảnh báo của D chuyển tạm sang Người phụ trách ca đêm, dấu tạm nhận; khi xác nhận thì gỡ dấu | FR-037, Q-94 |
| Q20 | 005 gửi yêu cầu đóng `DA_THUC_HIEN_DUNG_KHUNG` cho công việc xoay trở có cảnh báo | `DA_DONG`, người đóng Hệ thống | US3 #11, FR-032 |
| Q21 | Cảnh báo bỏ lỡ liều đã Chuyển sự cố; 006 gửi yêu cầu đóng | Cảnh báo giữ nguyên; sự cố có diễn biến "nguồn đã được xử lý", người xử lý được báo; sự cố không tự đóng | US3 #13, Q-76 |
| Q22 | S thử tiếp nhận cảnh báo | 403 | US3 #12 |
| Q23 | K (bếp, ngoài phạm vi) ghi sự cố Nhẹ "bỏ bữa" cho A | 201; phản hồi chỉ nhận dạng; `ngoai_pham_vi`, nhật ký có dấu | US4 #1, SC-012 |
| Q24 | S ghi "khó ngủ" Nhẹ; D ghi "từ chối thuốc" Trung bình | Nhẹ: D nhận trong ứng dụng, hạn hết ca; Trung bình: D, P nhận (có SMS), 001 nhận `SuCoDaGhiNhan(canDanhGiaLai = true)` | US4 #2, #3 |
| Q25 | T đóng sự cố Trung bình; D đóng có kết quả; T đóng sự cố Nhẹ | 403; `DA_DONG`; 200 | US4 #4, #5, BR-M05-09 |
| Q26 | D chuyển theo dõi khi chưa có diễn biến xử lý; đủ diễn biến; mở lại không lý do, có lý do | 409; `DANG_THEO_DOI`; 422; `DANG_XU_LY` | US4 #10 |
| Q27 | D tạo sự cố Khẩn cấp "khó thở" cho A | Thẻ hiện đủ mục FR-048 ≤ 3 giây; bản chụp lưu; `SuCoKhanCapDaKichHoat` cho B, T, QL, M, D, P; nội dung cho M chỉ tối thiểu | US5 #1, #2, SC-005, SC-014 |
| Q28 | D ghi "ép tim" trước xác nhận đối chiếu; xác nhận; ghi lại | 409 `CHUA_DOI_CHIEU_NGUYEN_VONG` kèm nguyện vọng; 201; 201 | US5 #3, DBR-19, SC-006 |
| Q29 | Sự cố Khẩn cấp cho C (không nguyện vọng): ghi biện pháp hồi sức ngay | 201 | US5 #4 |
| Q30 | B chuyển viện A từ sự cố tới Bệnh viện X; thử lại với sự cố khác | A Điều trị tại bệnh viện, bản tóm tắt đủ mục; 409, không có bản tóm tắt, sự cố có diễn biến `CHUYEN_VIEN_BI_TU_CHOI` | US5 #6, #7, BR-M05-14 |
| Q31 | Gửi phong bì ngoại tuyến tạo sự cố | `TU_CHOI` `BAT_BUOC_TRUC_TUYEN`; app hiện "gọi trực tiếp" | US5 #9, Q-01 |
| Q32 | Ca đêm (không Bác sĩ trực): D tạo sự cố Khẩn cấp | Mọi Bác sĩ Hoạt động nhận; dấu "không có Bác sĩ trực"; QL nhận | US5 #11, FR-047b |
| Q33 | Kích hoạt khẩn cấp lần hai cho A khi sự cố Khẩn cấp đang mở; tham gia xử lý | 409 `DA_CO_SU_CO_KHAN_CAP_MO`; người sau được thêm vào người xử lý | Edge Case, FR-041 |
| Q34 | B hạ sự cố từ Khẩn cấp xuống Trung bình; thử sửa nội dung | `YeuCauDungNhacGoiDien` cho 009; không có thao tác sửa | US5 #10, BR-M05-22 |
| Q35 | S ghi sự cố ngã 10:00 cho A; chọn mức Nhẹ không lý do | 400/422 thiếu lý do; với lý do: ba thông báo (B, D, P), khoảng theo dõi 10:00 → +72 giờ, không có lịch đo, công việc nào được tạo | US6 #1, #2, SC-007 |
| Q36 | Ngã lúc 02:00 ghi 09:00 | Mốc 05:00, lần đo đầu 09:00 | FR-051 CHK013 |
| Q37 | A ngã lần nữa sau 42 giờ; P lập công việc theo dõi ngoài khoảng | Khoảng cũ `DUOC_THAY`, khoảng mới từ lần ngã này; 005 chặn công việc ngoài khoảng | US6 #3, FR-052 |
| Q38 | B kết thúc theo dõi sớm | Khoảng `KET_THUC_SOM`; 005 nhận `nguonBiHuy(THEO_DOI_SAU_NGA)` | US6 #6 |
| Q39 | D hủy ghi nhận sự cố ngã ghi nhầm người | `DA_HUY`, khoảng theo dõi giữ; D và các Bác sĩ nhận danh sách tác động còn hiệu lực | US6 #7, FR-046a |
| Q40 | Cân nặng A 50,0 → 48,5 → 47,4 kg trong 25 ngày; lần cân đầu sau tiếp nhận | Cảnh báo `SUT_CAN` Trung bình nêu hai lần đo, 011 nhận `SutCanPhatHien`; lần cân đầu không chạy quy tắc | US7 #1, CHK042 |
| Q41 | 005 phát `KetQuaNguDaGhi` "mất ngủ" ba đêm liên tiếp (một đêm vắng ở giữa) | Cảnh báo `MAT_NGU_KEO_DAI` Nhẹ; đêm vắng không làm đứt chuỗi | US7 #2, FR-039c |
| Q42 | Công việc đo 17:00 (căn cứ lịch đo, Bắt buộc) quá hạn rồi Không thực hiện | Đúng một cảnh báo `BO_LO_LAN_DO:HUYET_AP`, mức Trung bình | US7 #3, Q-75, SC-016 |
| Q43 | QL tắt quy tắc mất ngủ; ba đêm mất ngủ | Không cảnh báo; nhật ký có lý do | US7 #5 |
| Q44 | D ghi sự cố lây nhiễm cho A; tra cứu tiếp xúc | A mang dấu nghi nhiễm; D và Bác sĩ nhận `CanLapDanhSachTiepXuc`; tra cứu trả E (cùng phòng), người cùng buổi hoạt động, S, người thân thăm, kèm khoảng; khoảng vắng của A được đánh dấu; không có danh sách tự lập | US8 #1, #2, SC-008 |
| Q45 | D lập, thêm E, C, F (lý do), S, N; xác nhận | E, C, F dấu tiếp xúc; P nhận `CanLapCongViecDoNhietDo`; S, QL nhận; N nhận nội dung không danh tính; H chỉ thấy người thân không có kênh | US8 #3, FR-062, SC-014 |
| Q46 | B khoanh vùng tầng 2 gắn sự cố A; bản giả 012, 014, 003 thử đăng ký thăm, đăng ký hoạt động, phân bổ giường P203 | `KhoanhVungDaTao`; `KhoanhVungApi`/`TinhTrangLayNhiemChoPhanBo` báo đang chịu; ba lệnh bị chặn ở module sở hữu | US8 #4, #5, SC-009 |
| Q47 | T gỡ vùng; B gỡ có lý do; D đóng sự cố A khi vùng còn | 403; `KhoanhVungDaGo`; 409 `CON_VUNG_KHOANH_CHI_GAN_SU_CO` | US8 #6, #7, FR-066 |
| Q48 | Loại E khỏi danh sách | Dấu tiếp xúc E gỡ; 005 nhận `nguonBiHuy(THEO_DOI_TIEP_XUC)` | US8 #8 |
| Q49 | Cho tạm vắng A khi thuộc vùng khoanh | 001 trả cảnh báo "thuộc vùng đang khoanh vùng", lệnh vẫn thực hiện | FR-065, BR-M05-23 |
| Q50 | QL lưu giấy phép không có phạm vi khám chữa bệnh; B ghi chẩn đoán tại viện; tại Bệnh viện X | 403 `DIEU_KIEN_PHAP_LY`; 201 | US9 #2, SC-010 |
| Q51 | QL lưu giấy phép có phạm vi nội; B ghi lại | 201; `GiayPhepCoSoApi.coPhamVi` đúng | US9 #1, #4 |
| Q52 | S đính chính huyết áp 180/100 → 130/80 | Hiện hành 130/80; gốc còn; cảnh báo không tự đóng; người phụ trách nhận "nguồn đã được đính chính" | US10 #1, BR-M06-06 |
| Q53 | BS1 ghi dấu nguy kịch cho A | Một cảnh báo Khẩn cấp `NGUY_KICH`; B, D, T, QL nhận; M và người đại diện nhận không có nguyện vọng; `CanXacNhanLaiNguyenVong`; không có yêu cầu gọi điện | US11 #1, SC-015 |
| Q54 | Ca đêm D ghi dấu tạm; ca ngày D ghi dấu tạm | 201 + mọi Bác sĩ nhận; 409 `DANG_CO_BAC_SI_TRUC` | US11 #2, Q-218 |
| Q55 | D đóng cảnh báo nguy kịch chưa có kết quả; ghi "nguyện vọng mới" chuyển bệnh viện; ghi lựa chọn chuyển bệnh viện | 409 `CHUA_XAC_NHAN_LAI`; 001 nhận `ghiPhienBan`; chuyển viện + bản tóm tắt, dấu tự gỡ, đóng được | US11 #3, #4 |
| Q56 | Lựa chọn "về nhà"; lựa chọn giảm nhẹ khi không liên lạc được, không lý do | `CanChoTamVangVeNha` cho H, T, không có lượt vắng tự tạo; 422 | US11 #5, #6 |
| Q57 | Kết thúc lưu trú A khi còn cảnh báo mở | 409 `KT_D_CANH_BAO_SU_CO` (miễn được); ghi nhận qua đời: cảnh báo, sự cố dừng leo thang, lịch đo Đã ngừng, ngưỡng cá nhân Hết hiệu lực | Edge Case, FR-038 |
| Q58 | Dừng backend 1 giờ (đồng hồ giả lập) khi có ba cảnh báo Trung bình chưa nhận | Mỗi cảnh báo lên đúng một cấp, hạn mới tính từ lúc chạy lại; nhắc bị lỡ gửi một lần | FR-084, BR-M05-25 |
| Q59 | *(Bổ sung 2026-10-11, Q-339)* S (chăm sóc) ghi sự cố Trung bình cho A; T (Trưởng tầng của tầng A) giao cho D kèm ghi chú; QL giao lại cho Bác sĩ B; QL thử đóng; K (bếp) thử giao | `DANG_XU_LY`, D nhận thông báo Trung bình, leo thang dừng; giao lại 200, trạng thái giữ; QL đóng 403; K 403 | US4 #11, FR-045a |

## 4. Test tự động: truy vết kịch bản → test

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `SoNguongTest`, `SC_001_XepMucTest` (300 giá trị), `KiemTraNguongTest`, `KhoaLoaiTest`, `DemLap24hTest`, `SutCanTest`, `DemDemMatNguTest`, `MocTheoDoiSauNgaTest`, `ChuoiMocDoTest`, `ChuoiLeoThangTest` | Q1, Q5, Q7, Q16, Q36, Q40, Q41 |
| Chỉ số | `BR_M06_04_DoLaiIT`, `FR_017_HetHanDoLaiIT`, `BR_M04_10_DoChiSoCongViecIT`, `ChiSoNgoaiTuyenIT`, `BR_M06_06_DinhChinhChiSoIT`, `DoLaiDongThoiIT` | Q1 → Q6, Q52 |
| Ngưỡng, lịch đo | `BR_M06_01_NguongApDungIT`, `FR_022_PhienBanNguongIT`, `BR_M06_02_NhacThieuNguongIT`, `FR_004_LichDoCanCuIT`, `FR_005_ThayDoiLichDoIT`, `NguongTriggerIT` | Q7 → Q11 |
| Cảnh báo | `BR_M05_01_LeoThangIT`, `FR_034_NhacToiDaIT`, `BR_M05_02_GopCanhBaoIT`, `DBR_18_GopDongThoiIT`, `BR_M05_03_BaoLapLaiIT`, `FR_036_QuyenQuanLyVienIT`, `FR_032_NguonDaXuLyIT`, `BR_M05_19_NguoiVangMatIT` | Q12 → Q17, Q20 → Q22 |
| Bàn giao | `BR_M05_05_BanGiaoIT`, `FR_037_TamNhanIT` | Q18, Q19 |
| Sự cố | `FR_041_NgoaiPhamViIT`, `BR_M05_09_DongSuCoIT`, `FR_045_VongDoiSuCoIT`, `FR_045a_GiaoNguoiXuLyIT`, `BR_M05_17_SuCoCoSoVatChatIT`, `BR_M05_21_HuySuCoKhanCapIT`, `SuCoTriggerIT` | Q23 → Q26, Q39, Q59 |
| Khẩn cấp | `BR_M05_13_TheKhanCapIT`, `DBR_19_DoiChieuNguyenVongIT`, `BR_M05_14_ChuyenVienIT`, `FR_047b_KhongCoBacSiTrucIT`, `BR_M05_06_ThongBaoKhanCapIT`, `KichHoatDongThoiIT`, `BR_M05_22_HaMucDungGoiIT` | Q27 → Q34 |
| Ngã | `BR_M05_07_NgaIT`, `FR_052_NgaLaiIT` | Q35 → Q38 |
| Xu hướng | `BR_M05_04_BoLoLanDoIT`, `FR_039a_SutCanIT`, `FR_039c_MatNguIT`, `QuyTacTatIT` | Q40 → Q43 |
| Lây nhiễm | `BR_M05_10_TraCuuTiepXucIT`, `BR_M05_11_KhoanhVungIT`, `BR_M05_12_GoKhoanhVungIT`, `FR_062_ThongBaoTiepXucIT`, `BR_M05_24_HanhChinhTiepXucIT`, `BR_M05_23_VungKhoanhKhongChanIT` | Q44 → Q49 |
| Giấy phép, khám | `BR_M06_05_PhamViKhamIT`, `FR_072_DieuDuongGhiKhamIT`, `GiayPhepTriggerIT` | Q50, Q51 |
| Nguy kịch | `BR_M05_15_NguyKichIT`, `DBR_34_DauNguyKichIT`, `BR_M05_16_XacNhanLaiIT`, `FR_050d_LuaChonIT` | Q53 → Q56 |
| Trạng thái cuối, chạy bù | `FR_038_TrangThaiCuoiIT`, `KT_D_CanhBaoSuCoIT`, `BR_M05_25_ChayBuIT` | Q57, Q58 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit: `m06` không phụ thuộc `m05`; `m05`, `m06` không gọi API của 008, 009, 012, 014 (chỉ qua SPI mục 2.1); gói `..lich..` không tạo bản ghi nghiệp vụ | |
| Web (Vitest) | Màn cảnh báo (lệnh theo `lenhDuocPhep`, 409 đã có người tiếp nhận); màn sự cố (chặn hồi sức, thẻ chỉ nhận dạng); tra cứu tiếp xúc không chọn sẵn ai | Q13, Q28, Q44 |
| App (`flutter_test`) | `KichHoatKhanCapKhongNgoaiTuyenTest`, `GhiChiSoNgoaiTuyenTest`, `KetQuaDongBoChiSoTest` (BR-M15-20), `DoLaiTest`, `TheKhanCapTest` | Q2, Q27, Q31 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | `SC_001_XepMucTest`: 300 giá trị, 6 chỉ số, so với bảng tính tay 100%; 0 giá trị phi vật lý được lưu; 100% Nguy hiểm có `CHO_DO_LAI` hoặc `XU_LY_NGAY` |
| SC-002 | Đo p95 `POST …/chi-so` ≤ 2 giây với 60 người dùng đồng thời và 200 bản ghi đồng bộ cùng lúc; từ lưu tới cảnh báo hiện ở danh sách của người phụ trách ≤ 10 giây |
| SC-003 | Q15 và `DBR_18_GopDongThoiIT` |
| SC-004 | Q12 với đồng hồ giả lập: chênh giữa hạn và leo thang ≤ 1 phút; mỗi lần leo thang có dòng lịch sử; 0 cảnh báo đã tiếp nhận bị leo thang |
| SC-005 | Q27: `GET /su-co/{id}/the-khan-cap` ≤ 3 giây; sự kiện thông báo ghi cùng transaction (thời gian tới thiết bị đo ở 009) |
| SC-006 | Q28, trigger nhóm 3 |
| SC-007 | Q35 → Q37 |
| SC-008 | Q44 với bộ dữ liệu 10 ngày dựng sẵn; so với danh sách tính tay |
| SC-009 | Q46, Q47 (chặn ở 003, 012, 014 kiểm bằng bản cài thật khi có, bản giả khi chưa) |
| SC-010 | Q50 |
| SC-011 | Đo trên app: ghi sự cố Nhẹ ≤ 90 giây, kích hoạt khẩn cấp ≤ 15 giây từ lúc mở chức năng |
| SC-012 | Q23 |
| SC-013 | Bộ 50 cảnh báo dựng sẵn: `CanhBaoThongKeApi` khớp tính tay (thời gian tới tiếp nhận, số lần leo thang) |
| SC-014 | Q27, Q45: kiểm nội dung sự kiện gửi người thân |
| SC-015 | Q53 → Q55 |
| SC-016 | Q2, Q13, Q32, Q42 và test quyền Điều dưỡng ngoài phạm vi xác nhận danh sách tiếp xúc (403) |

## Kịch bản bổ sung 2026-10-18 (Q-352, Q-354)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| K1 | Sự cố Khẩn cấp cho A; người liên hệ chính M không có trong bản đồng ý | Yêu cầu tới M mang `doiTuongThongTin = VE_NGUOI_CAO_TUOI`, mã `KHAN_CAP_NGUOI_LIEN_HE_CHINH`; bản ghi gửi của 009 (bản giả) lưu mã | FR-047, FR-047c |
| K2 | Xác nhận danh sách tiếp xúc có người thân P; ghi dấu nguy kịch cho B có người đại diện R | Yêu cầu tới P mang `DANH_SACH_TIEP_XUC`; tới R mang `XAC_NHAN_LAI_NGUYEN_VONG` | FR-047c, FR-050b, FR-062 |
| K3 | A bị ngã Trung bình, đã khám; người thân M có quyền xem sức khỏe, N không; 012 gọi `suCo` và `choBanTin` | M: phần chung và phần sức khỏe; N: chỉ phần chung (loại, thời điểm, mức, "viện đã xử lý"), không có mô tả, khám, điều trị | FR-082, Q-352 |
| K4 | Ghi kết quả xác nhận lại `MOI` thiếu `nhanDinh` trong `nguyenVongMoi` | 422 từ 001; không có kết quả nào được ghi | FR-050c, 001 FR-023a |

Test: `FR_047c_MaTruongHopIT` (K1, K2), `FR_082_PhanChungSucKhoeIT` (K3), mở rộng `BR_M05_16_XacNhanLaiIT` (K4).

## Kịch bản bổ sung 2026-10-18, lượt 3 (Q-369, nút SOS)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| S1 | A (tài khoản người cao tuổi, thiết bị đã gắn) gọi `POST /sos` không có phiên lúc 14:00 | 201 `DA_GUI`; cảnh báo Khẩn cấp `NCT_YEU_CAU_TRO_GIUP` có phòng, giường; D, P, B, T nhận; người liên hệ chính và QLV không nhận | US12 #1, FR-047d, FR-047e |
| S2 | Gọi `POST /sos` thêm hai lần (Idempotency-Key khác); gửi lại một yêu cầu với cùng Idempotency-Key | Một cảnh báo, số lần 3; yêu cầu lặp không tăng số lần | US12 #3 |
| S3 | D tiếp nhận; A gọi `GET /sos/hien-tai`; A chọn nơi `NHA_VE_SINH` hai lần | `DA_TIEP_NHAN` kèm họ tên D; lần chọn thứ hai 409 | US12 #4, FR-047f |
| S4 | Bản giả 001 đặt A Tạm vắng; gọi `POST /sos`; B bán trú chưa đến gọi `POST /sos` | 409 `KHONG_O_VIEN` kèm `lienHe`; 409 `KHONG_CO_MAT` | US12 #5, FR-047d |
| S5 | A ở Hoạt động bên ngoài, bản giả 014 trả trưởng đoàn L, người đi cùng M; gọi `POST /sos` | L, M nhận thêm | US12 #6, FR-047e |
| S6 | D đóng cảnh báo kết quả "hỗ trợ uống nước"; lần khác D kích hoạt khẩn cấp từ cảnh báo SOS | Không sự cố, người thân không được báo; sự cố Khẩn cấp và người liên hệ chính được báo theo FR-047 | US12 #7, #8 |
| S7 | Không ai tiếp nhận quá hạn mức Khẩn cấp | Leo thang theo BR-M05-01 | US12 #9 |
| S8 | HC gỡ thiết bị của A; gọi `POST /sos` bằng token cũ | 401 `THIET_BI_KHONG_HOP_LE` kèm `lienHe` | FR-047d |

Test: `BR_M05_27_NutSosIT` (S1, S2), `FR_047f_TrangThaiSosIT` (S3), `FR_047d_DieuKienGuiSosIT` (S4, S8), `FR_047e_SosNgoaiVienIT` (S5), `SosXuLyIT` (S6, S7, US12 #10). SC-017: `BR_M05_27_NutSosIT` đếm cảnh báo mở và yêu cầu tới người liên hệ chính.

