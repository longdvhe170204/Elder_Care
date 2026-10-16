---
description: "Danh sách task triển khai feature 007 – Sức khỏe, cảnh báo và sự cố"
---

# Tasks: Sức khỏe, cảnh báo và sự cố (007)

**Input**: Tài liệu thiết kế ở `specs/007-health-alert-incident/` (plan.md, spec.md, research.md S-01 → S-22, data-model.md, contracts/api-007.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 001 (`TrangThaiNguoiCaoTuoiApi`, `NguyenVongApi`, `MucSucKhoeApi`, `ThamGiaChuyenTrangThai`), 003 (`TinhTrangLayNhiemChoPhanBo`, `ViTriApi`, `LichSuViTriApi`), 004 (`MucViecSauQuaDoiPort`), 005 (`NguonCongViecPort`, `ChiSoPort`, `CanhBaoDocPort`, sự kiện yêu cầu cảnh báo), 006 (`DonThuocApi`, sự kiện yêu cầu cảnh báo) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo data-model.md mục 15 và quickstart.md mục 4.

**Giai đoạn**: Triển khai (chủ dự án mở ngày 2026-10-10); cách làm theo mục "Giai đoạn hiện tại" của CLAUDE.md.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M06: `BE/m06/{api,danhmuc,lichdo,chiso,nguong,giayphep,kham,trangthai,lich}/`; module M05: `BE/m05/{api,canhbao,xuhuong,suco,khancap,nguykich,nga,laynhiem,trangthai,lich}/`; phụ thuộc một chiều `m05 → m06` (research S-01)
- Web `frontend/src/tinh-nang/{danh-muc-suc-khoe,nguong-lich-do,chi-so,canh-bao,su-co,nguy-kich,lay-nhiem,giay-phep-kham}/`; app nhân viên `mobile/lib/nhan-vien/{ghi-chi-so,canh-bao,su-co,nguy-kich,dong-bo}/`; app người thân `mobile/lib/nguoi-than/suc-khoe/`

---

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/m06/...` và `BE/m05/...` theo plan với `package-info.java`; ArchUnit `BT/m05/M05M06ArchTest.java` kiểm chỉ có chiều `m05 → m06` (research S-01)
- [ ] T002 [P] Tạo bản giả SPI 007 khai báo trong `BT/support/gia007/`: `NhanSuCanhBaoPort` (008), `NguoiThanKhanCapPort` (012), `DiemDanhTruyVetPort` (014), `NhatKyThongBaoPort` (009) (contracts mục 2.1)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `m06.api`, `m05.api`, SPI, sự kiện; danh mục; bảng khóa theo người; khung job; listener yêu cầu cảnh báo.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T003 [P] Tạo giao diện `BE/m06/api/`: `ChiSoApi`, `LichDoApi`, `SoLienHeVienApi`, `ChiSoThongKeApi` và sự kiện `ChiSoDaXacNhan`, `ChiSoDaDinhChinh`, `LichDoDaDoi`, `NhacThieuNguongCaNhan` (contracts mục 1.2 → 1.4, 1.10, 1.11)
- [ ] T004 [P] Tạo giao diện `BE/m05/api/`: `CanhBaoBanGiaoApi`, `SuCoLenhApi` (MANDATORY), `SuCoApi`, `CanhBaoApi`, `KhoanhVungApi`, `SucKhoeCongNguoiThanApi`, `BanTinSucKhoeApi`, `CanhBaoThongKeApi`; SPI `NhanSuCanhBaoPort`, `NguoiThanKhanCapPort`, `DiemDanhTruyVetPort`, `NhatKyThongBaoPort`; sự kiện mục 1.12 và sự kiện cho module làm plan sau phát (`YeuCauTaoCanhBao`, `YeuCauDongCanhBaoNguon`, mục 1.13); sự kiện gửi 009 cài `CoYeuCauThongBao` (research S-19)
- [ ] T005 Tạo `MIG/V007_01__nen.sql`: `chi_so_nct` (`nct_id` khóa chính, `da_nhac_thieu_nguong_luc`, `phien_ban`), `theo_doi_nct` (`nct_id` khóa chính, `phien_ban`), `lan_chay_m05`, `lan_chay_m06`, `su_kien_canh_bao_da_nhan` (`khoa VARCHAR(191)` khóa chính); danh mục `chi_so` (`UNIQUE (ma)`; nạp `HUYET_AP`, `NHIP_TIM`, `NHIET_DO`, `SPO2`, `DUONG_HUYET`, `CAN_NANG`; `sinh_hieu BOOLEAN`), `chi_so_thanh_phan` (khóa chính `(chi_so_id, ma)`; `TAM_THU`, `TAM_TRUONG` với huyết áp), `quy_tac_xu_huong` (`ma` khóa chính `SUT_CAN|BO_LO_LAN_DO|MAT_NGU_KEO_DAI|TU_CHOI_THUOC`; `muc ENUM('NHE','TRUNG_BINH','KHAN_CAP')`), `loai_su_co` (nạp 9.3 gồm `NGA`, `LAY_NHIEM_NGHI_NGO`, `CO_SO_VAT_CHAT`, `DO_GUI_*`…; `muc_mac_dinh`; `khong_thuoc_suc_khoe`; `cho_phep_khong_nct`), `loai_dien_bien` (`nhom` `DANH_GIA|HANH_DONG|THONG_BAO|THEO_DOI|HE_THONG`; `bien_phap_hoi_suc`; nạp năm biện pháp hồi sức) (data-model mục 2, 3)
- [ ] T006 Cài danh mục trong `BE/m06/danhmuc/internal/ChiSoDanhMucService.java` và `BE/m05/canhbao/internal/DanhMucM05Service.java` (mã có quy tắc riêng không Ngừng hiệu lực được; đổi `khong_thuoc_suc_khoe` ghi nhật ký); controller `/chi-so-danh-muc`, `/{id}`, `/{id}/ngung-hieu-luc`, `/quy-tac-xu-huong`, `/{ma}`, `/loai-su-co`, `/{id}`, `/loai-dien-bien` (research S-04)
- [ ] T007 Cài khóa `BE/m06/trangthai/internal/KhoaChiSo.java`, `BE/m05/trangthai/internal/KhoaTheoDoi.java` (research S-03) và khung job `BE/m06/lich/ViecTheoPhutM06.java`, `ViecHangNgayM06.java`, `BE/m05/lich/ViecTheoPhutM05.java` (ShedLock, chạy bù một cấp mỗi lần; research S-21)
- [ ] T008 [P] Cài provider cho 000/002 ở hai module: `PhanLoaiTruongProvider` (`SUC_KHOE` cho giá trị, mức, mô tả, diễn biến, thẻ khẩn cấp, nguyện vọng, nhận định; `LIEN_HE` cho số điện thoại người tiếp xúc), `LuuTruProvider` theo data-model mục 14, `GanPhamViProvider`, `QuyenTepProvider` (`M06.SCAN_GIAY_PHEP_CO_SO`, `M06.TEP_KHAM`, `M05.TEP_SU_CO`) (research S-20, S-22)
- [ ] T009 [P] Tạo màn web `frontend/src/tinh-nang/danh-muc-suc-khoe/`

**Checkpoint**: Nền 007 sẵn sàng.

---

## Phase 3: User Story 1 – Ghi chỉ số, so ngưỡng hai mức, tạo cảnh báo (Priority: P1) 🎯 MVP

**Goal**: Ghi chỉ số (công việc đo, ngoài lịch, ngoại tuyến), chặn ngoài khoảng vật lý CFG-M06-03, so ngưỡng theo thành phần nặng nhất, đo lại giá trị nguy hiểm trong CFG-M06-04, tạo cảnh báo (FR-010 → FR-018, BR-M06-03, BR-M06-04, BR-M04-10 phần 007).

**Independent Test**: A có ngưỡng mặc định SpO2 (cảnh báo < 94, nguy hiểm < 90); ghi 101, 96, 92, 88 rồi đo lại 87 → 101 bị chặn, 96 không cảnh báo, 92 cảnh báo, 88 chờ đo lại, 87 cảnh báo Khẩn cấp.

### Tests cho User Story 1

- [ ] T010 [P] [US1] Test đơn vị `BT/m06/SoNguongTest.java`, `SC_001_XepMucTest.java` (thành phần nặng nhất; chưa có ngưỡng)
- [ ] T011 [P] [US1] Test `BT/m06/BR_M06_04_DoLaiIT.java`, `BR_M06_03_TaoCanhBaoIT.java`, `BR_M04_10_DoChiSoCongViecIT.java`, `ChiSoNgoaiTuyenIT.java`: Nguy hiểm → `CHO_DO_LAI`; đo lại một lần (`UNIQUE do_lai_cho`); quá hạn → `DA_XAC_NHAN` + `CHUA_DO_LAI` và cảnh báo Khẩn cấp; "xử lý ngay" bỏ qua đo lại; `ChiSoPort.ghi` trả `DA_LUU`/`BI_CHAN`/`CHO_DO_LAI`; ngoại tuyến lưu `thoi_diem_thiet_bi`

### Implementation cho User Story 1

- [ ] T012 [US1] Tạo `MIG/V007_02__ban_ghi_chi_so.sql`: `ban_ghi_chi_so` (nhóm 3 có bản nháp; `muc ENUM('BINH_THUONG','CANH_BAO','NGUY_HIEM','CHUA_CO_NGUONG')`; `trang_thai_xac_nhan ENUM('CHO_DO_LAI','DA_XAC_NHAN','DA_DO_LAI')`; `do_lai_cho` `UNIQUE`; `nhan SET('XU_LY_NGAY','CHUA_DO_LAI','GHI_NHAN_MUON','GHI_NGOAI_TUYEN')`; `thoi_diem_do <= tao_luc`; trigger cho đặt `thoi_diem_xac_nhan` đúng một lần; chỉ mục `(nct_id, chi_so_id, thoi_diem_do)`, `(trang_thai_xac_nhan, han_do_lai)`, `(cong_viec_id)`) và `ban_ghi_chi_so_thanh_phan` (`gia_tri DECIMAL(7,2)`; không `UPDATE`) (data-model mục 5)
- [ ] T013 [US1] Cài `BE/m06/chiso/internal/ChiSoService.java`: kiểm khoảng vật lý CFG-M06-03; chọn phiên bản ngưỡng áp dụng tại `thoi_diem_do`; `SoNguong`; đo lại; phát `ChiSoDaXacNhan` sau commit; cài `ChiSoPort` (005), `ChiSoApi` (gồm `ganNhat` cho 001) (research S-06)
- [ ] T014 [US1] Cài `NgoaiTuyenHandler` `M06.BAN_GHI_CHI_SO` và việc theo phút `CHO_DO_LAI` quá `han_do_lai` trong `BE/m06/chiso/internal/`
- [ ] T015 [US1] Tạo `MIG/V007_03__canh_bao.sql`: `canh_bao` (`muc ENUM('NHE','TRUNG_BINH','KHAN_CAP')`; `trang_thai ENUM('MOI','LEO_THANG','DA_TIEP_NHAN','DANG_XU_LY','DA_DONG','CHUYEN_SU_CO')`; `cap_leo_thang TINYINT` 0..2; cột sinh `khoa_mo` với `UNIQUE (nct_id, khoa_mo)`; bốn chỉ mục; trigger chặn đổi `nct_id`, `khoa_loai`), `nguon_canh_bao` (nhóm 3; cột xử lý đặt một lần), `lich_su_canh_bao`, `lan_nhac_canh_bao` (`UNIQUE (loai, canh_bao_id, su_co_id, so_thu_tu)`) (data-model mục 7)
- [ ] T016 [US1] Cài `BE/m05/canhbao/internal/CanhBaoService.java` phần tạo: `KhoaLoai.tu`, khóa `theo_doi_nct`, gộp vào cảnh báo mở cùng khóa (tăng `so_lan`, nâng mức nếu cao hơn), người phụ trách ban đầu (`NhanSuCanhBaoPort.dieuDuongPhuTrach`), hạn tiếp nhận theo mức; bỏ qua khi người cao tuổi ở trạng thái cuối (research S-08)
- [ ] T017 [US1] Cài listener `BE/m05/canhbao/internal/YeuCauCanhBaoListener.java` cho `ChiSoDaXacNhan`, `ChiSoDaDinhChinh` (m06), `m04.api.YeuCauCanhBao`/`YeuCauDongCanhBao` (005), `m07.api.YeuCauCanhBao`/`YeuCauDongCanhBao` (006), `m05.api.YeuCauTaoCanhBao`/`YeuCauDongCanhBaoNguon`; chống trùng bằng `su_kien_canh_bao_da_nhan`; yêu cầu đóng đóng cảnh báo khi mọi nguồn đã xử lý, hoặc thêm diễn biến `NGUON_DA_XU_LY` khi đã chuyển sự cố (research S-09)
- [ ] T018 [US1] Tạo `BE/m06/chiso/web/ChiSoController.java` (`POST/GET /nguoi-cao-tuoi/{nctId}/chi-so`, `POST /ban-ghi-chi-so/{id}/do-lai`); app `mobile/lib/nhan-vien/ghi-chi-so/` (ngoại tuyến, đo lại, kết quả so) và `dong-bo/` với test `mobile/test/nhan-vien/ket_qua_dong_bo_chi_so_test.dart` (BR-M15-20); màn web `frontend/src/tinh-nang/chi-so/`

**Checkpoint**: Ghi chỉ số tạo cảnh báo đúng.

---

## Phase 4: User Story 2 – Bác sĩ thiết lập ngưỡng và lịch đo (Priority: P1)

**Goal**: Ngưỡng mặc định và cá nhân theo phiên bản; nhắc thiếu ngưỡng cá nhân; lịch đo là căn cứ `LAN_DO` cho 005, không sinh công việc (FR-003 → FR-006, FR-020 → FR-025, BR-M06-01, BR-M06-02).

**Independent Test**: Ngưỡng mặc định nhiệt độ; ngưỡng cá nhân cho A từ ngày mai; ghi một giá trị hôm nay và ngày mai → hôm nay so theo mặc định, ngày mai theo cá nhân.

### Tests cho User Story 2

- [ ] T019 [P] [US2] Test `BT/m06/BR_M06_01_NguongApDungIT.java`, `BR_M06_02_NhacThieuNguongIT.java`: một Hiệu lực và một Chờ hiệu lực mỗi (phạm vi, chỉ số) (`UNIQUE khoa_hieu_luc`, `khoa_cho`); thứ tự cận và khoảng vật lý; trigger chặn sửa; nhắc thiếu ngưỡng cá nhân một lần (CFG); trạng thái cuối kết thúc ngưỡng cá nhân
- [ ] T020 [P] [US2] Test `BT/m06/LichDoIT.java` và `ChuoiMocDoTest.java`: ba kiểu tần suất; thay đổi lịch phát `nguonDaDoi(LAN_DO)`; ngừng phát `nguonBiHuy(LAN_DO)`; `NguonCongViecPort.kiemTra(LAN_DO)` trả "theo chỉ định" và mức quan trọng; không có dòng công việc nào do 007 tạo

### Implementation cho User Story 2

- [ ] T021 [US2] Tạo `MIG/V007_04__nguong_lich_do.sql`: `nguong_khoa` (`chi_so_id` khóa chính), `nguong_phien_ban` (`pham_vi ENUM('CO_SO','CA_NHAN')`; `nct_id` bắt buộc khi `CA_NHAN` (`CHECK`); `trang_thai ENUM('CHO_HIEU_LUC','HIEU_LUC','HET_HIEU_LUC','DA_HUY')`; cột sinh `khoa_pham_vi`, `khoa_hieu_luc` `UNIQUE`, `khoa_cho` `UNIQUE`; trigger), `nguong_thanh_phan` (`duoi_nguy_hiem`, `duoi_canh_bao`, `tren_canh_bao`, `tren_nguy_hiem DECIMAL(7,2) NULL`; trigger chặn `UPDATE`), `lich_do` (`kieu_tan_suat ENUM('MOC_GIO','MOI_N_GIO','THEO_THU')`; `muc_quan_trong` mặc định `BAT_BUOC`; `nguon ENUM('CHI_DINH_BAC_SI')`; `trang_thai ENUM('CHO_HIEU_LUC','HIEU_LUC','DA_NGUNG','HET_HAN')`; trigger), `lich_do_chi_so` (data-model mục 4, 6)
- [ ] T022 [US2] Cài `BE/m06/nguong/internal/NguongService.java` (`KiemTraNguong`; thiết lập, hủy, ngừng; khóa `nguong_khoa` hoặc `chi_so_nct`) và việc hằng ngày nhắc thiếu ngưỡng; việc theo phút `CHO_HIEU_LUC → HIEU_LUC` (research S-07)
- [ ] T023 [US2] Cài `BE/m06/lichdo/internal/LichDoService.java` (`ChuoiMocDo`; tạo, thay đổi, ngừng; `LichDoApi`; `NguonCongViecPort` `LAN_DO`) (research S-05)
- [ ] T024 [US2] Tạo controller `/nguoi-cao-tuoi/{nctId}/lich-do`, `/lich-do/{id}/moc-do`, `/thay-doi`, `/ngung`, `/nguong`, `/nguong/{id}/huy`, `/ngung`, `/chi-so/chua-co-nguong-ca-nhan`; màn web `frontend/src/tinh-nang/nguong-lich-do/`

**Checkpoint**: Ngưỡng và lịch đo chạy độc lập.

---

## Phase 5: User Story 3 – Tiếp nhận, leo thang và gộp cảnh báo (Priority: P1)

**Goal**: Vòng đời cảnh báo; hạn tiếp nhận theo mức; leo thang ba cấp (một cấp mỗi lần chạy); nhắc khi đã tối đa; báo lặp lại 24 giờ trượt; chuyển người phụ trách theo bàn giao (FR-026 → FR-038a, DBR-18, BR-M05-01 → 03, 05, 19, 25).

**Independent Test**: Cảnh báo Trung bình cho A không ai tiếp nhận → leo thang ở phút 15, 30, 45 đúng cấp; thêm hai lần cùng loại trong 24 giờ → số lần gộp đúng.

### Tests cho User Story 3

- [ ] T025 [P] [US3] Test đơn vị `BT/m05/ChuoiLeoThangTest.java`, `DemLap24hTest.java`
- [ ] T026 [P] [US3] Test `BT/m05/DBR_18_GopDongThoiIT.java` (SC-003), `BR_M05_01_LeoThangIT.java`, `BR_M05_02_GopCanhBaoIT.java`, `BR_M05_03_BaoLapLaiIT.java`, `BR_M05_25_ChayBuIT.java`, `BR_M05_19_NguoiVangMatIT.java`: hai luồng cùng khóa chỉ một cảnh báo mở; leo thang đúng mốc, chạy bù một cấp mỗi lần; nhắc tối đa không trùng; báo lặp lại; người vắng mặt dùng tầng của người, hạn không dừng
- [ ] T027 [P] [US3] Test `BT/m05/BR_M05_05_BanGiaoIT.java`, `FR_037_TamNhanIT.java`: bàn giao được xác nhận đổi người phụ trách thuộc ca trước; tạm nhận khi bàn giao chưa xác nhận (Q-94)

### Implementation cho User Story 3

- [ ] T028 [US3] Hoàn thiện `CanhBaoService.java` với `BangChuyenCanhBao` (bảng FR-029): tiếp nhận, giao người phụ trách, bắt đầu xử lý, đổi mức (hạ từ Khẩn cấp chỉ Bác sĩ), đóng (lý do với quá hạn Bắt buộc), chuyển sự cố, tạo thủ công (mô tả bắt buộc); `DemLap24h`; trạng thái cuối đặt `dung_leo_thang`
- [ ] T029 [US3] Đăng ký việc theo phút M05: quá hạn → leo thang một cấp (`NhanSuCanhBaoPort.truongTangDuocGiao`, Quản lý viện cấp 2), nhắc khi tối đa, ca đã bắt đầu chưa xác nhận bàn giao → tạm nhận (research S-08, S-18, S-21)
- [ ] T030 [US3] Cài `CanhBaoBanGiaoApi` (008) và `CanhBaoDocPort` (005) trong `BE/m05/canhbao/internal/`
- [ ] T031 [US3] Tạo `BE/m05/canhbao/web/CanhBaoController.java`: `GET/POST /canh-bao`, `GET /canh-bao/{id}` (kèm `lenhDuocPhep`), `POST …/tiep-nhan`, `/giao-nguoi-phu-trach`, `/bat-dau-xu-ly`, `/doi-muc`, `/dong`; màn web `frontend/src/tinh-nang/canh-bao/`, app `mobile/lib/nhan-vien/canh-bao/`

**Checkpoint**: Vòng đời cảnh báo đầy đủ.

---

## Phase 6: User Story 4 – Ghi nhận và xử lý sự cố ba mức (Priority: P1)

**Goal**: Sự cố tách nội dung (nhóm 3) và xử lý (nhóm 2); ghi nhận kể cả ngoài phạm vi; diễn biến, đổi mức, theo dõi, đóng; hủy và đổi loại bằng đính chính; sự cố cơ sở vật chất (FR-040 → FR-046b, BR-M05-08, 09, 17, 18, 21, 22, 26).

**Independent Test**: Bếp ghi sự cố Nhẹ "bỏ bữa" cho người ngoài phạm vi; điều dưỡng ghi Trung bình "từ chối thuốc"; kiểm phạm vi hiển thị, thông báo theo mức, điều kiện đóng.

### Tests cho User Story 4

- [ ] T032 [P] [US4] Test `BT/m05/SuCoTriggerIT.java`, `BR_M05_09_DongSuCoIT.java`, `BR_M05_17_SuCoCoSoVatChatIT.java`, `BR_M05_18_LayNhiemGanNctIT.java`: `su_co` không `UPDATE`; chuyển theo dõi cần ≥ 1 diễn biến `DANH_GIA` và ≥ 1 `HANH_DONG`; đóng theo mức; cơ sở vật chất không người cao tuổi có tầng/khu bắt buộc; lây nhiễm bắt buộc người cao tuổi; ghi ngoài phạm vi có `ngoai_pham_vi` (SC-012)
- [ ] T033 [P] [US4] Test `BT/m05/BR_M05_21_HuySuCoKhanCapIT.java`, `BR_M05_22_HaMucDungGoiIT.java`, `BR_M05_26_SuCoHuyBanTinIT.java`: hủy sự cố Khẩn cấp qua đính chính có điều kiện; hạ mức phát `YeuCauDungNhacGoiDien`; sự cố hủy phát `SuCoDaHuyTrongBanTin`; đồ gửi bị hủy nguồn → `DA_HUY` Hệ thống
- [ ] T066 [P] [US4] Test `BT/m05/FR_045a_GiaoNguoiXuLyIT.java`: Trưởng tầng giao sự cố Mới cho Điều dưỡng có quyền với người cao tuổi kèm ghi chú → `DANG_XU_LY`, `nguoi_phu_trach_id` đổi, dừng leo thang, `phan_cong_su_co` một dòng, `SuCoDaGiaoNguoiXuLy` đúng mức; thiếu ghi chú → 422; giao cho Nhân viên chăm sóc hoặc Điều dưỡng ngoài phạm vi → 409; Trưởng tầng tầng khác giao → 403; Nhân viên bếp (có `SU_CO, TAO` nhờ ngoại lệ ¹⁰) giao → 403; Quản lý viện giao lại sự cố Đang xử lý cho Bác sĩ → 200, trạng thái giữ nguyên, rồi Quản lý viện đóng → 403; người xử lý hiện tại giao lại → 200; sự cố cơ sở vật chất kiểm phạm vi theo tầng của địa điểm (spec 007 US4 #11; FR-045a, Q-339, Q-71)

### Implementation cho User Story 4

- [ ] T034 [US4] Tạo `MIG/V007_05__su_co.sql`: `su_co` (nhóm 3; `nguon_phat_sinh ENUM` 10 giá trị; `ngoai_pham_vi`; `thoi_diem_xac_nhan` ngay khi lưu), `su_co_xu_ly` (`su_co_id` khóa chính; `trang_thai ENUM('MOI','DANG_XU_LY','DANG_THEO_DOI','DA_DONG','DA_HUY')`; `ket_qua_lay_nhiem` `XAC_NHAN_NHIEM|LOAI_TRU|KHONG_XAC_DINH`; cột sinh `khoa_khan_cap_mo` không `UNIQUE`; trigger chặn đổi khi `DA_DONG`/`DA_HUY` trừ `DA_DONG → DA_HUY` và đổi loại kèm đính chính), `dien_bien_su_co`, `doi_muc_su_co`, `nguoi_xu_ly_su_co` (`UNIQUE (su_co_id, nhan_vien_id, vai_tro_tham_gia)`), `phan_cong_su_co` (nhóm 3, tạo sau `su_co`; FR-045a, bổ sung 2026-10-11) (data-model mục 8)
- [ ] T035 [US4] Cài `BE/m05/suco/internal/SuCoService.java` với `BangChuyenSuCo` (bảng FR-045, gồm hai dòng "Giao người xử lý" của FR-045a: khóa `SU_CO, GIAO`, không dùng ngoại lệ phạm vi; người giao kiểm `TaiKhoanApi.coVaiTro(TT, BS, QL)` hoặc là người xử lý hiện tại, cộng `PhamViApi.trongPhamVi` với Trưởng tầng và người xử lý hiện tại; người được giao thuộc `NguoiDuyetApi.danhSach((SU_CO, XU_LY), doiTuong)` và `coVaiTro(ĐD, TT, BS)`; giao từ Mới đặt `tiep_nhan_luc`, `nguoi_tiep_nhan_id` = người giao; phát `SuCoDaGiaoNguoiXuLy`; research mục sự cố "Cách kiểm cụ thể"): ghi nhận (ngoại lệ phạm vi 002 FR-044a; mức thấp hơn mặc định cần lý do; phát `SuCoDaGhiNhan` có `canDanhGiaLai`, `SuCoDaTaoThongBao`), tiếp nhận, tham gia, diễn biến, đổi mức, chuyển theo dõi, mở lại, đóng; leo thang sự cố trong việc theo phút; `SuCoLenhApi` cho 011, 013, 014 (research S-11)
- [ ] T036 [US4] Cài `DinhChinhDuocProvider` `M05.SU_CO` (hủy ghi nhận, đổi loại; `choPhep` BR-M05-21) và `M05.DIEN_BIEN_SU_CO` trong `BE/m05/suco/internal/`
- [ ] T037 [US4] Tạo `BE/m05/suco/web/SuCoController.java`: `/su-co`, `/{id}`, `/{id}/tiep-nhan`, `/giao-nguoi-xu-ly` (FR-045a), `/tham-gia-xu-ly`, `/dien-bien`, `/doi-muc`, `/chuyen-theo-doi`, `/mo-lai-xu-ly`, `/dong`; `POST /canh-bao/{id}/chuyen-su-co`; màn web `frontend/src/tinh-nang/su-co/`, app `mobile/lib/nhan-vien/su-co/`

**Checkpoint**: Sự cố ba mức chạy độc lập.

---

## Phase 7: User Story 5 – Quy trình khẩn cấp: thẻ thông tin, đối chiếu nguyện vọng, chuyển viện (Priority: P1)

**Goal**: Kích hoạt khẩn cấp chỉ trực tuyến; thẻ thông tin khẩn cấp dựng từ 001, 003, 006, 012 và lưu bản chụp; thông báo song song; đối chiếu nguyện vọng trước biện pháp hồi sức; chuyển viện gọi 001 trong cùng transaction kèm bản tóm tắt (FR-047 → FR-050, DBR-19, BR-M05-06, 13, 14, 20).

**Independent Test**: A có nguyện vọng "không đặt nội khí quản", dị ứng Penicillin, hai thuốc; điều dưỡng kích hoạt khẩn cấp → nội dung thẻ, bốn người nhận, chặn ghi biện pháp hồi sức khi chưa đối chiếu.

### Tests cho User Story 5

- [ ] T038 [P] [US5] Test `BT/m05/BR_M05_13_TheKhanCapIT.java`, `DBR_19_DoiChieuNguyenVongIT.java`, `BR_M05_20_DanhMucHoiSucIT.java`: thẻ có mục "không lấy được" khi nguồn lỗi; có nguyện vọng thì ghi diễn biến `bien_phap_hoi_suc` bị chặn tới khi có `xac_nhan_doi_chieu`
- [ ] T039 [P] [US5] Test `BT/m05/BR_M05_06_ThongBaoKhanCapIT.java`, `FR_047b_KhongCoBacSiTrucIT.java`, `BR_M05_14_ChuyenVienIT.java`: `SuCoKhanCapDaKichHoat` song song bốn nhóm; không có Bác sĩ trực → cờ và người nhận thay; chuyển viện gọi `TrangThaiNguoiCaoTuoiApi` `CHUYEN_VIEN` cùng transaction và tạo bản tóm tắt; lỗi 001 thì rollback

### Implementation cho User Story 5

- [ ] T040 [US5] Tạo `MIG/V007_06__khan_cap.sql`: `ban_chup_the_khan_cap` (nhóm 3; `noi_dung JSON`), `xac_nhan_doi_chieu` (`su_co_id UNIQUE`), `ban_tom_tat_chuyen_vien` (nhóm 3; `noi_dung JSON`)
- [ ] T041 [US5] Cài `BE/m05/khancap/internal/TheKhanCapService.java` (dựng thẻ từ `NguoiCaoTuoiApi`, `MucSucKhoeApi`, `NguyenVongApi`, `ViTriApi`, `DonThuocApi`, `NguoiThanKhanCapPort`; bản chụp mỗi lần kích hoạt), lệnh kích hoạt khẩn cấp (ghi sự cố Khẩn cấp hoặc nâng lên; từ cảnh báo thì `CHUYEN_SU_CO`), xác nhận đối chiếu, chuyển viện (bản tóm tắt; `chiTiet.canCu = {suCoId}`) (research S-12)
- [ ] T042 [US5] Tạo endpoint `POST /canh-bao/{id}/kich-hoat-khan-cap`, `GET /su-co/{id}/the-khan-cap`, `POST /su-co/{id}/xac-nhan-doi-chieu-nguyen-vong`, `/chuyen-vien`, `GET /ban-tom-tat-chuyen-vien/{id}` (bản in); app chặn kích hoạt khi mất kết nối, hiển thị thẻ

**Checkpoint**: Khẩn cấp chạy độc lập.

---

## Phase 8: User Story 11 – Nguy kịch: báo gia đình, xác nhận lại và thực hiện nguyện vọng (Priority: P1)

**Goal**: Dấu nguy kịch (chính thức, tạm), cảnh báo không gộp, xác nhận lại nguyện vọng, lựa chọn thực hiện; tự gỡ khi chuyển viện, trạng thái cuối (FR-050a → FR-050g, DBR-34, BR-M05-15, 16).

**Independent Test**: A có nguyện vọng "đưa về nhà", B chưa có; ghi dấu nguy kịch cho cả hai; thử đóng cảnh báo khi chưa xác nhận lại; ghi kết quả khác nhau và kiểm lệnh được phép.

### Tests cho User Story 11

- [ ] T043 [P] [US11] Test `BT/m05/DBR_34_DauNguyKichIT.java`, `BR_M05_15_NguyKichIT.java`, `BR_M05_16_XacNhanLaiIT.java`: một dấu mở mỗi người, một cảnh báo mỗi dấu; dấu tạm cần Bác sĩ xác nhận; đóng cảnh báo cần kết quả xác nhận lại và lựa chọn; không liên lạc được và không có nguyện vọng cần lý do; chọn về nhà phát `CanChoTamVangVeNha`, giảm nhẹ phát `LuaChonChamSocGiamNheDaGhi`; nguyện vọng mới ghi qua `NguyenVongApi.ghiPhienBan`

### Implementation cho User Story 11

- [ ] T044 [US11] Tạo `MIG/V007_07__nguy_kich.sql`: `dau_nguy_kich` (`loai` `CHINH_THUC|TAM`; `trang_thai ENUM('TAM','DANG_MO','DA_GO')`; `canh_bao_id NOT NULL UNIQUE`; cột sinh `khoa_mo` `UNIQUE`; trigger), `ket_qua_xac_nhan_lai` (nhóm 3; `kenh` `GOI_DIEN|TRUC_TIEP`; `ket_qua` `GIU|MOI|KHONG_LIEN_LAC_DUOC`), `lua_chon_thuc_hien` (nhóm 3; `lua_chon` `CHUYEN_BENH_VIEN|VE_NHA|GIAM_NHE`) (data-model mục 9)
- [ ] T045 [US11] Cài `BE/m05/nguykich/internal/NguyKichService.java` (ghi dấu, dấu tạm, xác nhận, gỡ; cảnh báo nguy kịch không gộp; kết quả xác nhận lại; lựa chọn; chuyển bệnh viện dùng luồng chuyển viện của US5; `DinhChinhDuocProvider` `M05.KET_QUA_XAC_NHAN_LAI`, `M05.LUA_CHON_THUC_HIEN`) (research S-13)
- [ ] T046 [US11] Tạo endpoint `/nguoi-cao-tuoi/{nctId}/dau-nguy-kich`, `/dau-nguy-kich/{id}/xac-nhan`, `/go`, `/canh-bao/{id}/ket-qua-xac-nhan-lai`, `/lua-chon-thuc-hien`; màn web `frontend/src/tinh-nang/nguy-kich/`, app `mobile/lib/nhan-vien/nguy-kich/`

**Checkpoint**: P1 hoàn tất.

---

## Phase 9: User Story 6 – Sự cố ngã và theo dõi sau ngã (Priority: P2)

**Goal**: Ngã mặc định Khẩn cấp; khoảng theo dõi 72 giờ với khoảng đo CFG-M05-06; ba thông báo; căn cứ `THEO_DOI_SAU_NGA` cho 005; không tạo lịch đo hay công việc (FR-051, FR-052, BR-M05-07).

**Independent Test**: Ghi ngã cho A lúc 10:00 → ba thông báo, A "đang theo dõi sau ngã" tới 10:00 sau 72 giờ, không có yêu cầu, lịch đo hay công việc tự tạo.

### Tests cho User Story 6

- [ ] T047 [P] [US6] Test đơn vị `BT/m05/MocTheoDoiSauNgaTest.java` và `BT/m05/BR_M05_07_NgaIT.java`: một khoảng đang diễn ra mỗi người (`UNIQUE khoa_dang`); ngã mới thay khoảng cũ (`DUOC_THAY`); kết thúc sớm có lý do; phát `SuCoNgaDaGhiNhan`, `CanLapCongViecTheoDoiSauNga`

### Implementation cho User Story 6

- [ ] T048 [US6] Tạo bảng `khoang_theo_doi_sau_nga` (`trang_thai` `DANG_THEO_DOI|DUOC_THAY|KET_THUC_SOM|DA_HET`; `khoang_do_phut` chốt từ CFG-M05-06; cột sinh `khoa_dang` `UNIQUE`) trong `MIG/V007_08__nga.sql`; cài `BE/m05/nga/internal/TheoDoiSauNgaService.java` (tác động kèm lệnh ghi sự cố ngã; `NguonCongViecPort` `THEO_DOI_SAU_NGA`; hết hạn trong việc theo phút) và endpoint `POST /su-co/{id}/ket-thuc-theo-doi-sau-nga` (research S-14)

**Checkpoint**: Ngã chạy độc lập.

---

## Phase 10: User Story 7 – Cảnh báo theo xu hướng (Priority: P2)

**Goal**: Sụt cân, bỏ lỡ lần đo theo lịch, mất ngủ kéo dài, từ chối thuốc; bật/tắt và mức theo `quy_tac_xu_huong` (FR-039 → FR-039c, BR-M05-04).

**Independent Test**: Cân nặng 50,0 → 48,5 → 47,4 kg trong 25 ngày; "mất ngủ" 3 đêm liên tiếp; một công việc đo theo lịch Không thực hiện → ba cảnh báo đúng loại và mức.

### Tests cho User Story 7

- [ ] T049 [P] [US7] Test đơn vị `BT/m05/SutCanTest.java`, `DemDemMatNguTest.java` và `BT/m05/BR_M05_04_BoLoLanDoIT.java`: ngưỡng CFG-M05-03…; `CongViecDoBiBo` (005) tạo cảnh báo khóa `BO_LO_LAN_DO:<chiSo>`; `KetQuaNguDaGhi` (005) đếm đêm; phát `SutCanPhatHien` cho 011

### Implementation cho User Story 7

- [ ] T050 [US7] Cài `BE/m05/xuhuong/internal/QuyTacXuHuongService.java` (`SutCan`, `DemDemMatNgu`, bỏ lỡ lần đo, từ chối thuốc; listener `ChiSoDaXacNhan`, `CongViecDoBiBo`, `KetQuaNguDaGhi`, sự kiện từ chối thuốc của 006) (research S-10)

**Checkpoint**: Xu hướng chạy độc lập.

---

## Phase 11: User Story 8 – Sự cố lây nhiễm: truy vết tiếp xúc và khoanh vùng (Priority: P2)

**Goal**: Tra cứu truy vết (cùng phòng 003, cùng buổi 014, phân công 008, lượt thăm 012) trong CFG-M05-07; danh sách tiếp xúc do người lập xác nhận; dấu tiếp xúc; khoanh vùng tầng hoặc khu vực; `TinhTrangLayNhiemChoPhanBo` cho 003; người thân cần liên hệ cho Hành chính (FR-060 → FR-066, BR-M05-10 → 12, 23, 24).

**Independent Test**: Dữ liệu 5 ngày (A ở P201 cùng B; 2 buổi cùng C, E; S chăm sóc A; N thăm A); ghi sự cố lây nhiễm cho A → tra cứu đúng 6 người; không tự đưa ai vào danh sách.

### Tests cho User Story 8

- [ ] T051 [P] [US8] Test `BT/m05/BR_M05_10_TraCuuTiepXucIT.java` (SC-008), `BR_M05_11_KhoanhVungIT.java`, `BR_M05_12_GoKhoanhVungIT.java`, `BR_M05_23_VungKhoanhKhongChanIT.java`, `BR_M05_24_HanhChinhTiepXucIT.java`: tra cứu đủ bốn nguồn, không tự thêm; một danh sách mỗi sự cố; xác nhận cần ≥ 1 người hoặc "không có" kèm lý do; khoanh vùng là các tầng hoặc đúng một khu vực; gỡ khi đủ điều kiện; vùng khoanh chỉ cảnh báo ở lệnh trạng thái; Hành chính chỉ thấy người thân cần liên hệ

### Implementation cho User Story 8

- [ ] T052 [US8] Tạo `MIG/V007_09__lay_nhiem.sql`: `danh_sach_tiep_xuc` (`su_co_id UNIQUE`; `trang_thai` `NHAP|DA_XAC_NHAN|DA_KET_THUC`), `nguoi_tiep_xuc` (`loai` `NGUOI_CAO_TUOI|NHAN_VIEN|NGUOI_THAN|NGUOI_DI_CUNG`), `khoanh_vung` (`trang_thai` `DANG_KHOANH_VUNG|DA_GO`; trigger), `khoanh_vung_pham_vi` (`loai` `TANG|KHU_VUC`), `khoanh_vung_su_co` (data-model mục 10)
- [ ] T053 [US8] Cài `BE/m05/laynhiem/internal/TruyVetService.java` (`LichSuViTriApi`, `DiemDanhTruyVetPort`, `NhanSuCanhBaoPort.phanCongTrongKhoang`, `NguoiThanKhanCapPort.luotThamTrongKhoang`), `DanhSachTiepXucService.java` (lập, thêm, bỏ, xác nhận, loại; phát `DaThemVaoDanhSachTiepXuc`, `CanLapCongViecDoNhietDo`, `ThongBaoTiepXuc`; `NguonCongViecPort` `THEO_DOI_TIEP_XUC`), `KhoanhVungService.java` (`KhoanhVungApi`; phát `KhoanhVungDaTao`/`DaGo`), `TinhTrangLayNhiemChoPhanBo` cho 003, `DauNghiNhiemDaDoi` cho 014 (research S-15)
- [ ] T054 [US8] Tạo endpoint `/su-co/{id}/tra-cuu-tiep-xuc`, `/su-co/{id}/danh-sach-tiep-xuc`, `/danh-sach-tiep-xuc/{id}/nguoi`, `/nguoi/{nguoiId}/bo`, `/nguoi/{nguoiId}/loai`, `/danh-sach-tiep-xuc/{id}/xac-nhan`, `/su-co/{id}/nguoi-than-can-lien-he`, `/khoanh-vung`, `/{id}/gan-su-co`, `/{id}/go`; màn web `frontend/src/tinh-nang/lay-nhiem/`

**Checkpoint**: Lây nhiễm chạy độc lập.

---

## Phase 12: User Story 9 – Phạm vi y tế theo giấy phép và ghi khám, điều trị (Priority: P3)

**Goal**: Giấy phép cơ sở có lịch sử không sửa; `GiayPhepCoSoApi` cho 002; số liên hệ viện; bản ghi khám tại viện hoặc ngoài viện (FR-070 → FR-075, BR-M06-05).

**Independent Test**: Giấy phép cơ sở không có phạm vi khám chữa bệnh; bác sĩ có giấy phép ghi chẩn đoán tại viện (bị chặn) và kết quả khám tại Bệnh viện X (được); thêm giấy phép có phạm vi rồi thử lại.

### Tests cho User Story 9

- [ ] T055 [P] [US9] Test `BT/m06/BR_M06_05_PhamViKhamIT.java` (SC-010) và `GiayPhepCoSoIT.java`: chẩn đoán/kê tại viện cần phạm vi cơ sở; ghi kết quả ngoài viện được phép; giấy phép không sửa, không xóa, thay thế có lý do; `sapHetHan` cho cảnh báo của 002

### Implementation cho User Story 9

- [ ] T056 [US9] Tạo `MIG/V007_10__giay_phep_kham.sql`: `giay_phep_co_so` (không `UPDATE`, không `DELETE`; `thay_the_cho`, `thay_the_do_sai_sot`), `giay_phep_co_so_pham_vi`, `so_lien_he_vien` (một dòng), `ban_ghi_kham` (nhóm 3; `loai` `KHAM|DANH_GIA|CHAN_DOAN|CHI_DINH|DIEU_TRI|KET_QUA`)
- [ ] T057 [US9] Cài `BE/m06/giayphep/internal/GiayPhepCoSoService.java` (`GiayPhepCoSoApi` cho 002; `SoLienHeVienApi`) và `BE/m06/kham/internal/KhamService.java` (khóa `CHUYEN_MON` tại viện; `DinhChinhDuocProvider` `M06.BAN_GHI_KHAM`); endpoint `/giay-phep-co-so`, `/so-lien-he-vien`, `/nguoi-cao-tuoi/{nctId}/kham-dieu-tri`; màn web `frontend/src/tinh-nang/giay-phep-kham/` (research S-16)

**Checkpoint**: Giấy phép và khám chạy độc lập.

---

## Phase 13: User Story 10 – Đính chính chỉ số và tra lịch sử sức khỏe (Priority: P3)

**Goal**: Đính chính bản ghi chỉ số; cảnh báo đã tạo không tự đóng, người phụ trách được báo; lịch sử kèm ngưỡng áp dụng (FR-019, BR-M06-06).

**Independent Test**: Ghi nhầm 180/100 (đúng 130/80), đính chính → giá trị hiện hành đổi, gốc còn, cảnh báo không tự đóng, người phụ trách được báo.

### Tests cho User Story 10

- [ ] T058 [P] [US10] Test `BT/m06/BR_M06_06_DinhChinhChiSoIT.java`: `UPDATE` sau xác nhận bị trigger chặn; `ChiSoDaDinhChinh` → `NguonCanhBaoDaDinhChinh` (không đóng cảnh báo); tra lịch sử dùng giá trị hiện hành

### Implementation cho User Story 10

- [ ] T059 [US10] Cài `DinhChinhDuocProvider` `M06.BAN_GHI_CHI_SO` (tầng của người lúc đo; phát `ChiSoDaDinhChinh`) và lịch sử chỉ số kèm ngưỡng áp dụng trong `ChiSoController.java`

**Checkpoint**: Mọi user story của 007 chạy độc lập.

---

## Phase 14: Polish & Cross-Cutting

- [ ] T060 [P] Cài bean `ThamGiaChuyenTrangThai` hai module (`@Order` sau 006: `m06` rồi `m05`): điều kiện `KT_D_CANH_BAO_SU_CO` (`coTheMien = true`); trạng thái cuối dừng leo thang, kết thúc lịch đo, ngưỡng cá nhân, gỡ dấu nguy kịch; `canhBao` vùng khoanh (BR-M05-23); `MucViecSauQuaDoiPort` `CANH_BAO_SU_CO` (research S-17)
- [ ] T061 [P] Cài `SucKhoeCongNguoiThanApi`, `BanTinSucKhoeApi` (012), `CanhBaoThongKeApi`, `ChiSoThongKeApi` (016); app `mobile/lib/nguoi-than/suc-khoe/`
- [ ] T062 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/007-health-alert-incident/contracts/api-007.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 007
- [ ] T063 Khi 008, 009, 012, 014 có bản thật, chạy lại test US3, US5, US8 với bean thật thay T002
- [ ] T064 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo NFR-03 (thông báo khẩn cấp) và SC theo quickstart mục 5
- [ ] T065 Rà truy vết data-model mục 15: DBR-18, 19, 23, 25, 34 và BR-M05-01 → 26, BR-M06-01 → 06, BR-M04-10 (phần 007), FR-045a có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-001, FR-002, FR-080, FR-081, FR-082, FR-083, FR-084, FR-085, FR-086 (bổ sung 2026-10-10) — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story; Foundational của 007 chặn 008, 009, 011, 012, 013, 014, 016 (cần `m05.api`, `m06.api`)
- **US1 (P1)**: sau Foundational; cần ngưỡng (US2) để thử mức – dùng dữ liệu test ngưỡng mặc định
- **US2 (P1)**: sau Foundational
- **US3 (P1)**: sau US1
- **US4 (P1)**: sau Foundational
- **US5 (P1)**: sau US3, US4
- **US11 (P1)**: sau US3; dùng chuyển viện của US5
- **US6, US7, US8 (P2)**: sau US4 (US7 sau US1)
- **US9, US10 (P3)**: sau US1

### Parallel Opportunities

- T003, T004, T008, T009 song song
- US2 và US4 song song với US1; US6, US7, US8 song song sau US4; US9, US10 song song
- Web và app song song khi controller đã có

## Parallel Example: User Story 3

```text
Task: "T025 ChuoiLeoThangTest + DemLap24hTest"
Task: "T026 DBR_18/BR_M05_01/02/03/25/19 IT"
Task: "T027 BR_M05_05_BanGiaoIT + FR_037_TamNhanIT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US2 → US1 → US3
2. **Dừng và kiểm chứng**: ghi chỉ số → cảnh báo → leo thang → gộp (SC-003)
3. Thêm US4, US5, US11 cho sự cố, khẩn cấp, nguy kịch

### Incremental Delivery

US2 → US1 → US3 → US4 → US5 → US11 → US6 → US7 → US8 → US9 → US10; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- 007 không sinh công việc đo, lịch đo theo dõi, sự cố, danh sách tiếp xúc; không đề xuất nâng mức hay người tiếp xúc (Q-306, Q-307)
- Kích hoạt khẩn cấp bắt buộc trực tuyến; không có `NgoaiTuyenHandler` cho sự cố
- Mọi mốc đọc `ThamSoApi` (CFG-M05-*, CFG-M06-*)
