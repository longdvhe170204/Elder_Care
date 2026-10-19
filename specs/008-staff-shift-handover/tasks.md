---
description: "Danh sách task triển khai feature 008 – Nhân sự, ca trực và bàn giao"
---

# Tasks: Nhân sự, ca trực và bàn giao (008)

**Input**: Tài liệu thiết kế ở `specs/008-staff-shift-handover/` (plan.md, spec.md, research.md N-01 → N-15, data-model.md, contracts/api-008.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2 (002 khai báo `HoSoNhanVienApi`, `GiayPhepApi`, `DanhMucPhamViHanhNgheApi`, `PhanCongApi`, `NhiemVuApi` dạng SPI để M09 cài); 001, 003 → 007 đã có `api` (`CongViecCaApi`, `CongViecBanGiaoApi`, `LieuBanGiaoApi`, `CanhBaoBanGiaoApi`, SPI `NhanSuCaPort`, `DieuDuongCaPort`, `NhanSuCanhBaoPort`, `ThamChieuViTri`).

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 11.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX). Từ 2026-10-16 là giai đoạn thiết kế; task chỉ được làm khi chủ dự án mở giai đoạn triển khai.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M09: `BE/m09/{api,nhanvien,giayphep,lichca,phancong,tyle,bangiao,nhiemvu,lich}/`; port nội bộ cho 015 ở `BE/m09/lichca/port/`, `BE/m09/phancong/port/`
- Web `frontend/src/tinh-nang/{nhan-vien,lich-ca,phan-cong,ty-le-phuc-vu,ban-giao}/`; app nhân viên `mobile/lib/nhan-vien/{ca-cua-toi,ban-giao,vang-ca,phan-cong}/`

---

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/m09/{api,nhanvien,giayphep,lichca,phancong,tyle,bangiao,nhiemvu,lich}/` với `package-info.java`; ArchUnit `BT/m09/M09ArchTest.java`: chỉ `m09.doica`, `m09.xoayca` (015) dùng port nội bộ; gói `..lich..` không lập ca, phân công, bàn giao (research N-01; Q-306)
- [ ] T002 [P] Tạo bản giả SPI 008 khai báo trong `BT/support/gia008/`: `DoGuiBanGiaoPort` (013), `ChuyenDiNhiemVuPort` (014), `KiemTraCongBoMoRong` (015) (contracts mục 2.1)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `m09.api`, sự kiện; hồ sơ nhân viên, chứng chỉ và hàm hiệu lực (mọi story cần); khung job; bản cài SPI tra cứu ca cho module khác.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T003 [P] Tạo giao diện `BE/m09/api/`: `CaTrucApi`, `NhanSuThongKeApi`, SPI `DoGuiBanGiaoPort`, `ChuyenDiNhiemVuPort` và sự kiện mục 1.6 (cài `CoYeuCauThongBao` khi gửi 009) (contracts mục 1.3, 1.4, 1.6, 2.1)
- [ ] T004 Tạo `MIG/V008_01__nhan_vien.sql`: `nhan_vien` (`tai_khoan_id BIGINT NULL UNIQUE`; `trang_thai ENUM('DANG_LAM_VIEC','NGHI_VIEC')` chỉ đổi khi `@lenh_trang_thai_nv`; trigger chặn `DELETE`), `bo_phan` (nạp Y tế, Chăm sóc, Bếp, Hành chính – Kế toán; Q-284), `nhan_vien_bo_phan` (cột sinh `khoa_hien_hanh = IF(den IS NULL, nhan_vien_id, NULL)` `UNIQUE`), `lich_su_phu_trach_bo_phan` (nhóm 3; cột sinh `khoa_mo` `UNIQUE`), `loai_chung_chi` (`nhom` `GIAY_PHEP_HANH_NGHE|DAO_TAO`; `bat_buoc_voi_vai_tro JSON`), `pham_vi_hanh_nghe`, `chung_chi` (trigger chặn `DELETE`; `UPDATE` chỉ cột thu hồi, cột job, sửa sai khi `@sua_sai`), `chung_chi_pham_vi`, `lan_chay_m09` (data-model mục 2, 3)
- [ ] T005 [P] Cài hàm thuần `BE/m09/giayphep/internal/HieuLucChungChi.java` (hiệu lực tới hết ngày hết hạn theo `ThoiGianNghiepVu`; thu hồi; thay thế) và test `BT/m09/HieuLucChungChiTest.java` (research N-05; DBR-25)
- [ ] T006 Cài tạo, sửa hồ sơ nhân viên (Quản lý viện; trường liên hệ `@NhomTruong(HAN_CHE_NHAN_VIEN)`) và thêm chứng chỉ trong `BE/m09/nhanvien/internal/NhanVienService.java`, `BE/m09/giayphep/internal/ChungChiService.java`; controller `/nhan-vien`, `/{id}`, `/loai-chung-chi`, `/pham-vi-hanh-nghe`, `/nhan-vien/{id}/chung-chi` (FR-001, FR-005, FR-006)
- [ ] T007 Cài SPI cho 002 trong `BE/m09/nhiemvu/internal/`: `HoSoNhanVienApi` (`trangThai`, `chucDanhCoVaiTro`), `GiayPhepApi` (`hieuLuc`, `sapHetHan`), `DanhMucPhamViHanhNgheApi` (contracts mục 1.1)
- [ ] T008 Cài khung job `BE/m09/lich/ViecTheoPhutM09.java`, `ViecHangGioM09.java`, `ViecHangNgayM09.java` (00:10) (ShedLock, `lan_chay_m09`, chạy bù) – việc đăng ký ở các story; test `BT/m09/ChayBuM09IT.java` (research N-13)
- [ ] T009 [P] Cài provider cho 000/002: `PhanLoaiTruongProvider` (`HAN_CHE_NHAN_VIEN`, `LY_DO_NHAN_SU`, nội dung bàn giao), `LuuTruProvider` theo data-model mục 10, `GanPhamViProvider`, `QuyenTepProvider` `M09.SCAN_CHUNG_CHI` (chỉ Quản lý viện và chính nhân viên) (research N-12)

**Checkpoint**: Hồ sơ nhân viên và chứng chỉ sẵn sàng.

---

## Phase 3: User Story 2 – Lập, công bố lịch ca và chỉ định người phụ trách ca (Priority: P1) 🎯 MVP

**Goal**: Mẫu ca, lịch tháng Nháp → Đã công bố, kiểm chồng ca và chuỗi ca liên tục, người phụ trách ca, Bác sĩ trực, bản công bố; vòng đời ca theo giờ (FR-011 → FR-016, FR-022, FR-023, FR-055, BR-M09-03, 12).

**Independent Test**: Hai mẫu ca; lịch tháng 10 cho tầng 2 có một trường hợp chồng giờ, một trường hợp liên tục 22 giờ, một ca trống người phụ trách → công bố bị chặn với đủ lý do; sửa xong công bố được.

### Tests cho User Story 2

- [ ] T010 [P] [US2] Test đơn vị `BT/m09/ChuoiCaLienTucTest.java`
- [ ] T011 [P] [US2] Test `BT/m09/BR_M09_03_ChongCaIT.java`, `ChongCaDongThoiIT.java`, `FR_016_CongBoIT.java`, `FR_022_TrangThaiCaIT.java`, `BR_M09_12_Ca24GioIT.java` (giờ bắt đầu bằng giờ kết thúc là ca 24 giờ, kết thúc hôm sau; ca dài 0 bị chặn): chồng giờ bị chặn dưới khóa `nhan_vien`; liên tục vượt CFG-M09-* cảnh báo/chặn theo FR-016; công bố cần mọi ca có người phụ trách; trạng thái ca theo giờ (Đang diễn ra, Đã đóng hoặc Chờ bàn giao); trigger chặn đổi giờ khi khác `NHAP` (Q1 → Q8)

### Implementation cho User Story 2

- [ ] T012 [US2] Tạo `MIG/V008_02__lich_ca.sql`: `mau_ca` (`loai_pham_vi` `TANG|KHU_VUC|TOAN_VIEN`; `yeu_cau_ban_giao`), `lich_ca` (`trang_thai` `NHAP|DA_CONG_BO`; `UNIQUE (thang, loai_pham_vi, pham_vi_id)`), `ca` (`trang_thai ENUM('NHAP','DA_CONG_BO','DANG_DIEN_RA','CHO_BAN_GIAO','DA_DONG','DA_HUY')`; `ket_thuc > bat_dau AND ket_thuc <= bat_dau + 24h` kiểm ở backend; ba chỉ mục; trigger), `ca_nhan_vien` (`bac_si_truc`, `vang_luc`, `rut_do_nghi_viec_luc`, `bo_sung_luc`, `nghi_co_duyet_yeu_cau_id`, `roi_ca_luc`), `nguoi_phu_trach_ca` (nhóm 3; cột sinh `khoa_mo` `UNIQUE`), `ban_cong_bo_lich` (nhóm 3; `noi_dung JSON`) (data-model mục 4)
- [ ] T013 [US2] Cài `BE/m09/lichca/internal/LichCaService.java`: mẫu ca (AC-01), lịch Nháp, thêm ca và nhân viên (khóa `nhan_vien`, chồng giờ, `ChuoiCaLienTuc`, chứng chỉ), người phụ trách ca, Bác sĩ trực, xem trước công bố, công bố (tự duyệt, lý do; `KiemTraCongBoMoRong` của 015; lưu `ban_cong_bo_lich`; phân công Nháp → Tương lai/Hiệu lực) (research N-06)
- [ ] T014 [US2] Đăng ký việc theo phút: ca `DA_CONG_BO → DANG_DIEN_RA → DA_DONG/CHO_BAN_GIAO`
- [ ] T015 [US2] Cài `CaTrucApi`, `NhanSuCaPort` (005), `DieuDuongCaPort` (006), `NhanSuCanhBaoPort` (007), `ThamChieuViTri` (003) trong `BE/m09/nhiemvu/internal/TraCuuCaService.java` (người phụ trách ca theo thời điểm, Bác sĩ trực nhiều người, có tên trong ca) (research N-11)
- [ ] T016 [US2] Tạo `BE/m09/lichca/web/LichCaController.java`: `/mau-ca`, `/{id}`, `/lich-ca`, `/lich-ca/{id}/ca`, `/ca/{id}`, `/lich-ca/{id}/xem-truoc-cong-bo`, `/cong-bo`, `GET /toi/ca`, `GET /bac-si-truc`; màn web `frontend/src/tinh-nang/lich-ca/` (cảnh báo xác nhận, danh sách chặn công bố) với test Vitest; app `mobile/lib/nhan-vien/ca-cua-toi/`

**Checkpoint**: Lịch ca chạy độc lập.

---

## Phase 4: User Story 3 – Phân công theo tầng, phòng, người cao tuổi; chặn thiếu chứng chỉ (Priority: P1)

**Goal**: Giao Trưởng tầng không chồng; phân công theo tầng/khu/phòng/người cao tuổi, giới hạn loại công việc; nhân viên chính duy nhất; chặn thiếu chứng chỉ; sao chép phân công; `NhiemVuApi`, `PhanCongApi` cho 002 (FR-024 → FR-033, DBR-21, BR-M09-01).

**Independent Test**: Tầng 2 có 25 người ở 8 phòng; phân S chính phòng 201–204, D Điều dưỡng phụ trách tầng; giấy phép của E hết hạn hôm qua → E không được phân công; xác định nhân viên chính đúng.

### Tests cho User Story 3

- [ ] T017 [P] [US3] Test đơn vị `BT/m09/XacDinhNhanVienChinhTest.java`, `SC_005_XepCaPhanCongTest.java` (200 lần)
- [ ] T018 [P] [US3] Test `BT/m09/DBR_21_ChungChiPhanCongIT.java`, `BR_M09_01_ChanThieuChungChiIT.java`, `FR_028_NhanVienChinhIT.java`, `PhanCongDongThoiIT.java`, `FR_032_SaoChepIT.java`, `FR_024_GiaoTruongTangIT.java`, `BR_M09_15_PhamViPhanCongIT.java` (phân công Nháp, đã kết thúc, đã hủy không tạo phạm vi dữ liệu), `BR_M09_16_ConPhanCongIT.java` (danh sách "còn phân công" cho 003 gồm phân công Nháp, giao Trưởng tầng còn hiệu lực, ca chưa đóng), `BR_M09_17_NghiDaiNgayIT.java` (không xếp ca không khóa tài khoản; cảnh báo giấy phép vẫn chạy; rút ngắn giao Trưởng tầng và giao có thời hạn cho người thay), `FR_028a_DieuDuongViecChamSocIT.java`, `NhiemVuApiIT.java`, `ThamChieuViTriIT.java`: hai phân công chính cùng đối tượng chồng thời gian bị chặn dưới khóa `tang_ca_khoa`; sao chép kiểm lại từng dòng; Trưởng tầng chồng bị chặn (SC-011) (Q10 → Q13, Q24, Q26)

### Implementation cho User Story 3

- [ ] T019 [US3] Tạo `MIG/V008_03__phan_cong.sql`: `tang_ca_khoa` (`tang_khu_vuc_id` khóa chính), `giao_truong_tang` (`trang_thai` `HIEU_LUC|DA_KET_THUC`), `phan_cong` (`loai_doi_tuong` `TANG|KHU_VUC|PHONG|NGUOI_CAO_TUOI`; `chinh_ho_tro`; `dieu_duong_phu_trach`; `trang_thai ENUM('NHAP','TUONG_LAI','HIEU_LUC','DA_KET_THUC','DA_HUY')`; `nguon` `LAP_TAY|SAO_CHEP|DOI_CA_015`; ba chỉ mục) (data-model mục 5)
- [ ] T020 [US3] Cài `BE/m09/phancong/internal/PhanCongService.java`: giao/kết thúc Trưởng tầng, lập (yêu cầu loại công việc qua `LoaiCongViecApi.yeuCau`; nghiệp vụ "phát thuốc" qua `NghiepVuChuyenMonApi.loaiYeuCau`), hủy, kết thúc (gọi `CongViecCaApi.khiKetThucPhanCong`), sao chép; nhắc người cao tuổi chưa có người phụ trách; việc theo phút chuyển trạng thái phân công (research N-08)
- [ ] T021 [US3] Cài `PhanCongApi`, `NhiemVuApi` (002; gồm `nguoiThayTruongTang`, `boPhanCua`, `laPhuTrachBoPhan`) trong `BE/m09/nhiemvu/internal/`
- [ ] T022 [US3] Tạo `BE/m09/phancong/web/PhanCongController.java`: `/giao-truong-tang`, `/{id}/ket-thuc`, `/ca/{id}/phan-cong`, `/phan-cong/{id}`, `/huy`, `/ket-thuc`, `/ca/{id}/sao-chep-phan-cong`; màn web `frontend/src/tinh-nang/phan-cong/`, app `mobile/lib/nhan-vien/phan-cong/`

**Checkpoint**: Phân công chạy độc lập.

---

## Phase 5: User Story 1 – Người phụ trách ca lập bàn giao và ca sau xác nhận (Priority: P1)

**Goal**: Một bàn giao mỗi ca; danh sách bản ghi nguồn để tham khảo (không chép tự động); mục và nhận định nhóm 3 có bản nháp khóa sau `chot_luc`; hoàn tất; ý kiến; xác nhận nguyên tử với chuyển công việc (005) và cảnh báo (007); nhắc (FR-038 → FR-047, DBR-20, BR-M09-06 → 08).

**Independent Test**: Ca ngày tầng 2 có công việc Quá hạn, liều Bỏ lỡ, chỉ số vượt ngưỡng, cảnh báo mở, người trở về; 17:30 người phụ trách ca lập bàn giao, danh sách nguồn đủ, hoàn tất; ca sau xác nhận → công việc, cảnh báo chuyển.

### Tests cho User Story 1

- [ ] T023 [P] [US1] Test đơn vị `BT/m09/XacDinhCaSauTest.java`, `MucNghiemTrongTest.java`
- [ ] T024 [P] [US1] Test `BT/m09/BR_M09_06_DanhSachNguonIT.java`, `FR_040_MucBanNhapIT.java`, `FR_041_HoanTatIT.java`, `BR_M09_07_ChoBanGiaoIT.java`, `FR_043_NhacBanGiaoIT.java`: danh sách nguồn đọc song song 003 → 007, 013, 014, không có "chép tất cả"; mục nghiêm trọng cần ghi chú; hoàn tất cần nhận định chung; ca không đóng khi chưa lập xong; nhắc lập, nhắc hết ca, nhắc chưa xác nhận (Q27 → Q33)
- [ ] T025 [P] [US1] Test `BT/m09/BR_M09_08_XacNhanBanGiaoIT.java`, `FR_044_XacNhanNguyenTuIT.java`, `FR_042_LenhDongThoiIT.java`, `DBR_20_MotBanGiaoMoiCaIT.java`, `BanGiaoTriggerIT.java`, `FR_047_ChamSocXemBanGiaoIT.java`: xác nhận gọi `CongViecCaApi.khiXacNhanBanGiao` và `CanhBaoBanGiaoApi.khiXacNhanBanGiao` trong cùng transaction, lỗi thì rollback; ý kiến và gửi lại; hai lệnh đồng thời; trigger khóa sau `chot_luc` và `DA_XAC_NHAN` (Q34 → Q39)

### Implementation cho User Story 1

- [ ] T026 [US1] Tạo `MIG/V008_04__ban_giao.sql`: `ban_giao` (`ca_id UNIQUE`; `trang_thai ENUM('BAN_NHAP','DA_LAP','CO_Y_KIEN','DA_XAC_NHAN')`; trigger chặn đổi khi `DA_XAC_NHAN`, chặn đổi `chot_luc` đã có), `muc_ban_giao` (nhóm 3 có bản nháp; `nhom` 9 giá trị ở data-model mục 7; trigger khóa sau `chot_luc`), `nhan_dinh_ban_giao` (`loai` `CHUNG|THEO_NGUOI_CAO_TUOI`), `y_kien_ban_giao`, `phan_hoi_y_kien` (chỉ thêm)
- [ ] T027 [US1] Cài `BE/m09/bangiao/internal/BanGhiNguonBanGiaoService.java`: đọc song song `CongViecBanGiaoApi`, `LieuBanGiaoApi`, `ChiSoApi.vuotNguongTrongKhoang`, `CanhBaoBanGiaoApi.dangMo`, `LuotVangApi`, `TrangThaiNguoiCaoTuoiApi.khoangTrangThai`, `TinhTrangGiuongApi.choSanSangQuaHan`, `DoGuiBanGiaoPort`, `ChuyenDiNhiemVuPort` (research N-10; Q-307)
- [ ] T028 [US1] Cài `BE/m09/bangiao/internal/BanGiaoService.java`: lập (người phụ trách ca hoặc Trưởng tầng từ mốc FR-039), thêm/xóa mục ở Bản nháp (`If-Match`), nhận định, hoàn tất (`chot_luc`; ca `CHO_BAN_GIAO → DA_DONG`), xác nhận (`XacDinhCaSau`; nguyên tử với 005, 007), ý kiến, bổ sung gửi lại; `DinhChinhDuocProvider` `M09.BAN_GIAO`; nhắc trong việc theo phút
- [ ] T029 [US1] Tạo `BE/m09/bangiao/web/BanGiaoController.java`: `POST/GET /ca/{id}/ban-giao`, `GET /ban-giao/{id}`, `/ban-ghi-nguon`, `/muc`, `/muc/{mucId}`, `/nhan-dinh`, `POST /hoan-tat`, `/xac-nhan`, `/y-kien`, `/bo-sung-gui-lai`; màn web `frontend/src/tinh-nang/ban-giao/` với test Vitest (không có nút "chép tất cả", mục không bỏ được, lỗi thiếu ghi chú)
- [ ] T030 [P] [US1] Tạo app `mobile/lib/nhan-vien/ban-giao/` (xem ngoại tuyến trong phạm vi ca; lệnh chỉ trực tuyến) và test `mobile/test/nhan-vien/ban_giao_ngoai_tuyen_chi_xem_test.dart`, `xoa_ban_giao_khi_het_pham_vi_ca_test.dart`

**Checkpoint**: P1 hoàn tất: lịch ca → phân công → bàn giao.

---

## Phase 6: User Story 4 – Hồ sơ nhân viên, giấy phép, đào tạo và cảnh báo hết hạn (Priority: P2)

**Goal**: Cho nghỉ việc với tác động (khóa tài khoản 002, rút khỏi ca, công việc thành chung), nhận lại; bộ phận và Phụ trách bộ phận; gia hạn, thu hồi, sửa sai chứng chỉ; cảnh báo đào tạo sắp hết hạn (FR-001a, FR-002 → FR-010, DBR-36, BR-M09-04, BR-M09-05).

**Independent Test**: Điều dưỡng D có giấy phép hết hạn sau 59 ngày và đào tạo sơ cứu hết hạn sau 61 ngày → cảnh báo giấy phép (002) có, cảnh báo đào tạo xuất hiện khi tới mốc CFG-M09-03.

### Tests cho User Story 4

- [ ] T031 [P] [US4] Test `BT/m09/BR_M09_04_NghiViecIT.java`, `FR_003_NhanLaiIT.java`, `FR_001a_PhuTrachBoPhanIT.java`, `DBR_36_MotBoPhanIT.java`, `NhanVienTriggerIT.java`: nghỉ việc gọi `TaiKhoanApi.khoaDoNghiViec` và `CongViecCaApi.khiNghiViec` cùng transaction, rút khỏi ca tương lai, hủy phân công; Phụ trách bộ phận cần người thay (Q-292); một bộ phận mỗi nhân viên (Q16 → Q19)
- [ ] T032 [P] [US4] Test `BT/m09/BR_M09_05_CanhBaoDaoTaoIT.java`, `FR_009_ThuHoiIT.java`: cảnh báo một lần; thu hồi làm mất hiệu lực, kết thúc phân công cần chứng chỉ (Q14, Q15)

### Implementation cho User Story 4

- [ ] T033 [US4] Cài lệnh Cho nghỉ việc (hiệu lực ngay hoặc tương lai qua việc theo phút), Nhận lại làm việc, đổi bộ phận, chỉ định Phụ trách bộ phận trong `NhanVienService.java` (research N-04)
- [ ] T034 [US4] Cài gia hạn, thu hồi, sửa sai chứng chỉ trong `ChungChiService.java`; việc theo phút chứng chỉ hết hiệu lực theo mốc; việc hằng ngày cảnh báo đào tạo sắp hết hạn (CFG-M09-03), bộ phận chưa có Phụ trách (research N-05)
- [ ] T035 [US4] Bổ sung endpoint `POST /nhan-vien/{id}/cho-nghi-viec`, `/nhan-lai-lam-viec`, `/doi-bo-phan`, `GET /bo-phan`, `POST /bo-phan/{id}/chi-dinh-phu-trach`, `POST /chung-chi/{id}/gia-han`, `/thu-hoi`, `/sua-sai`; màn web `frontend/src/tinh-nang/nhan-vien/`

**Checkpoint**: Hồ sơ nhân viên đầy đủ.

---

## Phase 7: User Story 5 – Cảnh báo tỷ lệ phục vụ theo trọng số chăm sóc (Priority: P2)

**Goal**: Tỷ lệ = tổng trọng số mức chăm sóc / số nhân viên được tính; cảnh báo dưới ngưỡng một lần; tính lại theo sự kiện (FR-034 → FR-037a, BR-M09-02).

**Independent Test**: Tầng 2 có 20 người cơ bản (trọng số 1), 6 người đặc biệt (2,5), tổng 35; ngưỡng 8 trọng số/nhân viên; 4 nhân viên được tính → cảnh báo.

### Tests cho User Story 5

- [ ] T036 [P] [US5] Test đơn vị `BT/m09/TinhTyLePhucVuTest.java` và `BT/m09/BR_M09_02_TyLePhucVuIT.java`, `FR_036_CanhBaoMotLanIT.java`, `BR_M09_14_TinhTheoCaIT.java` (người có ca chưa có phân công vẫn được tính; yêu cầu đổi mức chăm sóc đang chờ duyệt không tính trước) (Q20, Q21)

### Implementation cho User Story 5

- [ ] T037 [US5] Tạo bảng `lan_tinh_ty_le` (nhóm 3; `su_kien` 10 giá trị; `tong_trong_so DECIMAL(8,2)`) trong `MIG/V008_05__ty_le.sql`; cài `BE/m09/tyle/internal/TyLePhucVuService.java` (`TinhTyLePhucVu`; listener `MucChamSocDaDoi`, `NguoiCaoTuoiDaChuyenTrangThai`, `PhanBoDaBatDau`, `PhanBoDaDong`, `DaChuyenGiuong`; cảnh báo một lần qua `dang_duoi_nguong`), việc hằng giờ ca sắp bắt đầu dưới ngưỡng (CFG-M09-08); `NhanSuThongKeApi` (016) (research N-09)
- [ ] T038 [US5] Tạo endpoint `GET /ca/{id}/ty-le-phuc-vu` và màn web `frontend/src/tinh-nang/ty-le-phuc-vu/`

**Checkpoint**: Tỷ lệ phục vụ chạy độc lập.

---

## Phase 8: User Story 6 – Điều chỉnh ca đã công bố: vắng ca, bổ sung, giao trưởng tầng (Priority: P3)

**Goal**: Lệnh sau công bố: bổ sung nhân viên, chuyển người phụ trách, ghi nhận vắng (công việc thành chung, liều thành chung, phân công kết thúc, tỷ lệ tính lại), hủy ca; port nội bộ cho 015 (FR-017 → FR-021, BR-M04-13 phần 008).

**Independent Test**: Ghi S vắng ca ngày lúc 07:20 → công việc của S thành chung, liều của người do S phụ trách thành liều chung, phân công của S kết thúc, tỷ lệ tính lại.

### Tests cho User Story 6

- [ ] T039 [P] [US6] Test `BT/m09/FR_020_VangCaIT.java`, `FR_020_NgoaiKhoangIT.java`, `FR_019_ChuyenNguoiPhuTrachIT.java` (gồm BR-M09-13: người bàn giao của bàn giao đang dở đổi theo), `FR_021_HuyCaIT.java`, `FR_053_LyDoVangIT.java`, `FR_052_GioiHanTruongNhanVienIT.java`, `SC_012_ThongBaoNgoaiUngDungIT.java`: vắng ca gọi `CongViecCaApi.khiVangCa` cùng transaction; vắng của người phụ trách ca chuyển người phụ trách; bàn giao Bản nháp đổi người bàn giao; lý do vắng chỉ người có quyền thấy (Q22, Q23, Q25, Q40, Q41)

### Implementation cho User Story 6

- [ ] T040 [US6] Cài lệnh bổ sung nhân viên, chuyển người phụ trách ca, ghi nhận vắng, hủy ca trong `BE/m09/lichca/internal/LenhSauCongBoService.java`; port `BE/m09/lichca/port/CaLenhPort.java` và `BE/m09/phancong/port/PhanCongLenhPort.java` cho 015 (research N-07)
- [ ] T041 [US6] Bổ sung endpoint `POST /ca/{id}/bo-sung-nhan-vien`, `/chuyen-nguoi-phu-trach`, `/ghi-nhan-vang`, `/huy`, `GET /nhat-ky-can-cu-nhiem-vu`; app `mobile/lib/nhan-vien/vang-ca/` và test `mobile/test/nhan-vien/ghi_nhan_vang_ca_test.dart`

**Checkpoint**: Mọi user story của 008 chạy độc lập.

---

## Phase 9: Polish & Cross-Cutting

- [ ] T042 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/008-staff-shift-handover/contracts/api-008.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 008
- [ ] T043 Thay bản giả của 002 (002 T003), 005 → 007 (`NhanSuCaPort`, `DieuDuongCaPort`, `NhanSuCanhBaoPort`) bằng bản cài của 008 và chạy lại test của các feature đó
- [ ] T044 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T045 Rà truy vết data-model mục 11: DBR-20, 21, 23, 25, 36 và BR-M09-01 → 08, 12 → 17, BR-M04-13, BR-M05-05 (phần 008) có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-048, FR-049, FR-050, FR-051, FR-052, FR-053, FR-054, FR-056, FR-057, FR-058, FR-059, FR-060 — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story; Foundational của 008 cung cấp SPI cho 002 (thay bản giả)
- **US2 (P1)**: sau Foundational
- **US3 (P1)**: sau US2
- **US1 (P1)**: sau US2 (ca); dùng phân công của US3 cho danh sách nguồn
- **US4 (P2)**: sau Foundational; tác động lên ca cần US2
- **US5 (P2)**: sau US3
- **US6 (P3)**: sau US2, US3; 015 dùng port của US6

### Parallel Opportunities

- T003, T005, T009 song song
- US4 song song với US2–US3; US5 song song với US1 sau US3
- Web và app song song khi controller đã có

## Parallel Example: User Story 1

```text
Task: "T023 XacDinhCaSauTest + MucNghiemTrongTest"
Task: "T024 BR_M09_06/FR_040/FR_041/BR_M09_07/FR_043 IT"
Task: "T025 BR_M09_08/FR_044/FR_042/DBR_20/BanGiaoTrigger/FR_047 IT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US2 → US3 → US1
2. **Dừng và kiểm chứng**: lịch ca công bố → phân công → bàn giao → xác nhận nguyên tử
3. Thay bản giả của 002, 005 → 007

### Incremental Delivery

US2 → US3 → US1 → US4 → US5 → US6; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- 008 không lập ca, lịch, phân công, bàn giao thay actor; không gợi ý người phụ trách ca (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M09-*)
- Lệnh bàn giao chỉ trực tuyến; app chỉ xem ngoại tuyến
