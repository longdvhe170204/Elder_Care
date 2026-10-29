---
description: "Danh sách task triển khai feature 008 – Nhân sự, ca trực và bàn giao"
---

# Tasks: Nhân sự, ca trực và bàn giao (008)

**Input**: Tài liệu thiết kế ở `specs/008-staff-shift-handover/` (plan.md, spec.md, research.md N-01 → N-16, data-model.md, contracts/api-008.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2 (002 khai báo `HoSoNhanVienApi`, `GiayPhepApi`, `DanhMucPhamViHanhNgheApi`, `PhanCongApi`, `NhiemVuApi` dạng SPI để M09 cài); 001, 003 → 007 đã có `api` (`CongViecCaApi`, `CongViecBanGiaoApi`, `LieuBanGiaoApi`, `CanhBaoBanGiaoApi`, SPI `NhanSuCaPort`, `DieuDuongCaPort`, `NhanSuCanhBaoPort`, `ThamChieuViTri`).

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 11.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

**Cập nhật 2026-10-22** (plan mục "Cập nhật 2026-10-22"; Q-382; 007 FR-047i): thêm T046 → T049. Không đánh số lại.

**Tiến độ 2026-10-23 (E)**: bản dịch đầy đủ `contracts/api-008.yaml` và glossary mục 14 đã được A duyệt theo xác nhận của E trong phiên làm việc. Phase 1 (T001–T002) đã hoàn thành sau TDD và `./mvnw clean verify` trên JDK 25/MySQL 8.0: 174 test đơn vị/kiến trúc, 426 test tích hợp, không lỗi/thất bại/bỏ qua. Kết quả đối chiếu và cách xử lý phụ thuộc T002/T003 ở plan mục "Ghi chú triển khai"; chưa tạo PR vào develop. Phase 2 T003–T009 hoàn thành: clean verify chạy nền trên JDK 25/MySQL 8.0 qua 182 test đơn vị/kiến trúc và 454 tích hợp (636 tổng), không lỗi/thất bại/bỏ qua; Spotless và đối chiếu API qua. Các đổi hợp đồng/phần A đã được người dùng duyệt và ghi ở plan; người dùng đã commit/push Phase 2 ở `2a2ec09`.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M09: `BE/m09/{api,nhanvien,giayphep,lichca,phancong,tyle,bangiao,nhiemvu,lich}/`; port nội bộ cho 015 ở `BE/m09/lichca/port/`, `BE/m09/phancong/port/`
- Web `frontend/src/tinh-nang/{nhan-vien,lich-ca,phan-cong,ty-le-phuc-vu,ban-giao}/`; app nhân viên `mobile/lib/nhan-vien/{ca-cua-toi,ban-giao,vang-ca,phan-cong}/`

---

## Phase 1: Setup

- [x] T001 Tạo khung M09 theo `backend/README.md`, R-02: dùng tên tiếng Anh, chỉ tạo gói khi có lớp; giữ `@ApplicationModule` ở gốc, `@NamedInterface` cho các gói API được tạo. ArchUnit `BT/m09/M09ArchTest.java`: chỉ phần đổi/xoay ca 015 gọi các port lệnh nội bộ; `scheduling/` không lập lịch, ca, phân công, bàn giao hoặc mục bàn giao (research N-01, N-07; Q-306). Tên và chỗ đặt cụ thể ở glossary mục 14, plan "Ghi chú triển khai".
- [x] T002 [P] Khai báo tối thiểu các SPI đã có trong hợp đồng để tạo bản giả: `DoGuiBanGiaoPort` (013), `ChuyenDiNhiemVuPort` (014), `KiemTraCongBoMoRong` (015); bản giả và `@TestConfiguration` ở `BT/support/fake/m09/`, SPI/kiểu công khai ở `m09.api.spi`, port mở rộng công bố nội bộ ở tầng service của phần ca (contracts 008 mục 2.1; research N-07). Phần khai báo hai SPI lấy sớm từ T003; T003 vẫn giữ API tra cứu ca, thống kê và sự kiện.

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `m09.api`, sự kiện; hồ sơ nhân viên, chứng chỉ và hàm hiệu lực (mọi story cần); khung job; bản cài SPI tra cứu ca cho module khác.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T003 [P] Tạo giao diện `BE/m09/api/`: `CaTrucApi`, `NhanSuThongKeApi` và sự kiện mục 1.6 (cài `CoYeuCauThongBao` khi gửi 009). Hai SPI `DoGuiBanGiaoPort`, `ChuyenDiNhiemVuPort` đã được đưa lên T002 để bản giả biên dịch được; dùng lại, không khai báo lần hai (contracts mục 1.3, 1.4, 1.6, 2.1; plan "Ghi chú triển khai").
- [x] T004 Tạo `MIG/V008_01__nhan_vien.sql`: `nhan_vien` (`tai_khoan_id BIGINT NULL UNIQUE`; `trang_thai ENUM('DANG_LAM_VIEC','NGHI_VIEC')` chỉ đổi khi `@lenh_trang_thai_nv`; trigger chặn `DELETE`), `bo_phan` (nạp Y tế, Chăm sóc, Bếp, Hành chính – Kế toán; Q-284), `nhan_vien_bo_phan` (cột sinh `khoa_hien_hanh = IF(den IS NULL, nhan_vien_id, NULL)` `UNIQUE`), `lich_su_phu_trach_bo_phan` (nhóm 3; cột sinh `khoa_mo` `UNIQUE`), `loai_chung_chi` (`nhom` `GIAY_PHEP_HANH_NGHE|DAO_TAO`; `bat_buoc_voi_vai_tro JSON`), `pham_vi_hanh_nghe`, `chung_chi` (trigger chặn `DELETE`; `UPDATE` chỉ cột thu hồi, cột job, sửa sai khi `@sua_sai`), `chung_chi_pham_vi`, `lan_chay_m09` (data-model mục 2, 3)
- [x] T005 [P] Cài hàm thuần `BE/m09/giayphep/internal/HieuLucChungChi.java` (hiệu lực tới hết ngày hết hạn theo `ThoiGianNghiepVu`; thu hồi; thay thế) và test `BT/m09/HieuLucChungChiTest.java` (research N-05; DBR-25)
- [x] T006 Cài tạo, sửa hồ sơ nhân viên (Quản lý viện; trường liên hệ `@NhomTruong(HAN_CHE_NHAN_VIEN)`) và thêm chứng chỉ trong `BE/m09/nhanvien/internal/NhanVienService.java`, `BE/m09/giayphep/internal/ChungChiService.java`; controller `/nhan-vien`, `/{id}`, `/loai-chung-chi`, `/pham-vi-hanh-nghe`, `/nhan-vien/{id}/chung-chi` (FR-001, FR-005, FR-006)
- [x] T007 Cài SPI cho 002 trong `BE/m09/nhiemvu/internal/`: `HoSoNhanVienApi` (`trangThai`, `chucDanhCoVaiTro`), `GiayPhepApi` (`hieuLuc`, `sapHetHan`), `DanhMucPhamViHanhNgheApi` (contracts mục 1.1)
- [x] T008 Cài khung job `BE/m09/lich/ViecTheoPhutM09.java`, `ViecHangGioM09.java`, `ViecHangNgayM09.java` (00:10) (ShedLock, `lan_chay_m09`, chạy bù) – việc đăng ký ở các story; test `BT/m09/ChayBuM09IT.java` (research N-13)
- [x] T009 [P] Cài provider cho 000/002: `PhanLoaiTruongProvider` (`HAN_CHE_NHAN_VIEN`, `LY_DO_NHAN_SU`, nội dung bàn giao), `LuuTruProvider` theo data-model mục 10 cho bảng đã có trong T004, `GanPhamViProvider` hồ sơ/chứng chỉ, `QuyenTepProvider` `M09.SCAN_CHUNG_CHI` (chỉ Quản lý viện và chính nhân viên). Đăng ký provider lưu giữ/phạm vi ca, phân công, bàn giao cùng migration T012/T019/T026; không truy vấn bảng chưa tạo. Bổ sung kiểm tra danh mục còn được 002 tham chiếu qua API đã duyệt; chi tiết ở plan "Ghi chú triển khai" (research N-12).

**Checkpoint**: Hồ sơ nhân viên và chứng chỉ sẵn sàng.

---

## Phase 3: User Story 2 – Lập, công bố lịch ca và chỉ định người phụ trách ca (Priority: P1) 🎯 MVP

**Goal**: Mẫu ca, lịch tháng Nháp → Đã công bố, kiểm chồng ca và chuỗi ca liên tục, người phụ trách ca, Bác sĩ trực, bản công bố; vòng đời ca theo giờ (FR-011 → FR-016, FR-022, FR-023, FR-055, BR-M09-03, 12).

**Independent Test**: Hai mẫu ca; lịch tháng 10 cho tầng 2 có một trường hợp chồng giờ, một trường hợp liên tục 22 giờ, một ca trống người phụ trách → công bố bị chặn với đủ lý do; sửa xong công bố được.

### Tests cho User Story 2

- [x] T010 [P] [US2] Test đơn vị `BT/m09/ChuoiCaLienTucTest.java`
- [x] T011 [P] [US2] Test `BT/m09/BR_M09_03_ChongCaIT.java`, `ChongCaDongThoiIT.java`, `FR_016_CongBoIT.java`, `FR_022_TrangThaiCaIT.java`, `BR_M09_12_Ca24GioIT.java` (giờ bắt đầu bằng giờ kết thúc là ca 24 giờ, kết thúc hôm sau; ca dài 0 bị chặn): chồng giờ bị chặn dưới khóa `nhan_vien`; liên tục vượt CFG-M09-* cảnh báo/chặn theo FR-016; công bố cần mọi ca có người phụ trách; trạng thái ca theo giờ (Đang diễn ra, Đã đóng hoặc Chờ bàn giao); trigger chặn đổi giờ khi khác `NHAP` (Q1 → Q8)

### Implementation cho User Story 2

- [x] T012 [US2] Tạo `MIG/V008_02__lich_ca.sql`: `mau_ca` (`loai_pham_vi` `TANG|KHU_VUC|TOAN_VIEN`; `yeu_cau_ban_giao`), `lich_ca` (`trang_thai` `NHAP|DA_CONG_BO`; `UNIQUE (thang, loai_pham_vi, pham_vi_id)`), `ca` (`trang_thai ENUM('NHAP','DA_CONG_BO','DANG_DIEN_RA','CHO_BAN_GIAO','DA_DONG','DA_HUY')`; `ket_thuc > bat_dau AND ket_thuc <= bat_dau + 24h` kiểm ở backend; ba chỉ mục; trigger), `ca_nhan_vien` (`bac_si_truc`, `vang_luc`, `rut_do_nghi_viec_luc`, `bo_sung_luc`, `nghi_co_duyet_yeu_cau_id`, `roi_ca_luc`), `nguoi_phu_trach_ca` (nhóm 3; cột sinh `khoa_mo` `UNIQUE`), `ban_cong_bo_lich` (nhóm 3; `noi_dung JSON`) (data-model mục 4) **Cài thực tế**: `V008_02__shift_schedules.sql`; cùng thay đổi đăng ký hai provider phạm vi lịch/ca, sáu provider lưu giữ và tham chiếu ngược từ nhân viên (phần T009 hoãn tới migration này).
- [x] T013 [US2] Cài `BE/m09/lichca/internal/LichCaService.java`: mẫu ca (AC-01), lịch Nháp, thêm ca và nhân viên (khóa `nhan_vien`, chồng giờ, `ChuoiCaLienTuc`, chứng chỉ), người phụ trách ca, Bác sĩ trực, xem trước công bố, công bố (tự duyệt, lý do; `KiemTraCongBoMoRong` của 015; lưu `ban_cong_bo_lich`; phân công Nháp → Tương lai/Hiệu lực) (research N-06) **Tách theo phụ thuộc kỹ thuật**: T013 hoàn thành phần lịch/ca và lưu bản công bố; kiểm, gỡ, chuyển hiệu lực và snapshot phân công thực hiện cùng T019–T021 vì bảng phân công chưa tồn tại. Tỷ lệ phục vụ nối T037; không trả số liệu giả.
- [x] T014 [US2] Đăng ký việc theo phút: ca `DA_CONG_BO → DANG_DIEN_RA → DA_DONG/CHO_BAN_GIAO`
- [x] T015 [US2] Cài `CaTrucApi`, `NhanSuCaPort` (005), `DieuDuongCaPort` (006), `NhanSuCanhBaoPort` (007), `ThamChieuViTri` (003) trong `BE/m09/nhiemvu/internal/TraCuuCaService.java` (người phụ trách ca theo thời điểm, Bác sĩ trực nhiều người, có tên trong ca) (research N-11) **Phần hiện có**: `ShiftQueries` cài `ShiftApi` và các hàm đọc ca của bốn SPI tối thiểu được người dùng duyệt; test dùng bản giả chung m03/m04/m05/m07. Các hàm phân công/giao Trưởng tầng ở T021; bàn giao ở T026–T028, không khai báo/cài trả rỗng để thay dữ liệu nguồn chưa có.
- [x] T016 [US2] Tạo `BE/m09/lichca/web/LichCaController.java`: `/mau-ca`, `/{id}`, `/lich-ca`, `/lich-ca/{id}/ca`, `/ca/{id}`, `/lich-ca/{id}/xem-truoc-cong-bo`, `/cong-bo`, `GET /toi/ca`, `GET /bac-si-truc`; màn web `frontend/src/tinh-nang/lich-ca/` (cảnh báo xác nhận, danh sách chặn công bố) với test Vitest; phần mobile tách T016b theo CLAUDE.md
- [ ] T016b [US2] App `mobile/lib/nhan-vien/ca-cua-toi/`: giữ chưa làm chức năng cho tới khi chủ dự án mở triển khai mobile; backend/web T016 không chờ task này.

**Checkpoint**: Phần backend/web lịch ca đã kiểm tra; clean verify chạy nền qua 672 test (191 đơn vị/kiến trúc, 481 IT), 105 test web với một worker, lint/build và kiểm trình duyệt qua. Phân công/giao Trưởng tầng, bàn giao và tỷ lệ nối đúng task nguồn đã ghi ở T021/T026/T037; T016b mobile chưa mở triển khai. Người dùng đã commit/push Phase 3 tại `ec85fd5` (đối chiếu origin ngày 2026-10-24).

---

## Phase 4: User Story 3 – Phân công theo tầng, phòng, người cao tuổi; chặn thiếu chứng chỉ (Priority: P1)

**Goal**: Giao Trưởng tầng không chồng; phân công theo tầng/khu/phòng/người cao tuổi, giới hạn loại công việc; nhân viên chính duy nhất; chặn thiếu chứng chỉ; sao chép phân công; `NhiemVuApi`, `PhanCongApi` cho 002 (FR-024 → FR-033, DBR-21, BR-M09-01).

**Independent Test**: Tầng 2 có 25 người ở 8 phòng; phân S chính phòng 201–204, D Điều dưỡng phụ trách tầng; giấy phép của E hết hạn hôm qua → E không được phân công; xác định nhân viên chính đúng.

### Tests cho User Story 3

- [x] T017 [P] [US3] Test đơn vị `BT/m09/XacDinhNhanVienChinhTest.java`, `SC_005_XepCaPhanCongTest.java` (200 lần). **Tên/chỗ cài**: `PrimaryAssigneeResolverTest` kiểm hàm thuần; bộ 200 trường hợp SC-005 nằm trong `AssignmentIT` của T018 để kiểm chứng chỉ, ca và quyền qua service/MySQL thật, không thay bằng 200 hoán vị đầu vào.
- [x] T018 [P] [US3] Test `BT/m09/DBR_21_ChungChiPhanCongIT.java`, `BR_M09_01_ChanThieuChungChiIT.java`, `FR_028_NhanVienChinhIT.java`, `PhanCongDongThoiIT.java`, `FR_032_SaoChepIT.java`, `FR_024_GiaoTruongTangIT.java`, `BR_M09_15_PhamViPhanCongIT.java` (phân công Nháp, đã kết thúc, đã hủy không tạo phạm vi dữ liệu), `BR_M09_16_ConPhanCongIT.java` (danh sách "còn phân công" cho 003 gồm phân công Nháp, giao Trưởng tầng còn hiệu lực, ca chưa đóng), `BR_M09_17_NghiDaiNgayIT.java` (không xếp ca không khóa tài khoản; cảnh báo giấy phép vẫn chạy; rút ngắn giao Trưởng tầng và giao có thời hạn cho người thay), `FR_028a_DieuDuongViecChamSocIT.java`, `NhiemVuApiIT.java`, `ThamChieuViTriIT.java`: hai phân công chính cùng đối tượng chồng thời gian bị chặn dưới khóa `tang_ca_khoa`; sao chép kiểm lại từng dòng; Trưởng tầng chồng bị chặn (SC-011) (Q10 → Q13, Q24, Q26)

### Implementation cho User Story 3

- [x] T019 [US3] Tạo `MIG/V008_03__phan_cong.sql`: `tang_ca_khoa` (`tang_khu_vuc_id` khóa chính), `giao_truong_tang` (`trang_thai` `HIEU_LUC|DA_KET_THUC`), `phan_cong` (`loai_doi_tuong` `TANG|KHU_VUC|PHONG|NGUOI_CAO_TUOI`; `chinh_ho_tro`; `dieu_duong_phu_trach`; `trang_thai ENUM('NHAP','TUONG_LAI','HIEU_LUC','DA_KET_THUC','DA_HUY')`; `nguon` `LAP_TAY|SAO_CHEP|DOI_CA_015`; ba chỉ mục) (data-model mục 5). **Migration thật** `V008_03__staff_assignments.sql`, kèm cờ kỹ thuật `work_shift.publication_assignments_notified_at` để nhắc công bố/đầu ca độc lập và thử lại khi nguồn B/C chưa có; provider phạm vi/lưu giữ nối cùng bảng mới.
- [x] T020 [US3] Cài `BE/m09/phancong/internal/PhanCongService.java`: giao/kết thúc Trưởng tầng, lập (yêu cầu loại công việc qua `LoaiCongViecApi.yeuCau`; nghiệp vụ "phát thuốc" qua `NghiepVuChuyenMonApi.loaiYeuCau`), hủy, kết thúc (gọi `CongViecCaApi.khiKetThucPhanCong`), sao chép; nhắc người cao tuổi chưa có người phụ trách; việc theo phút chuyển trạng thái phân công (research N-08)
- [x] T021 [US3] Cài `PhanCongApi`, `NhiemVuApi` (002; gồm `nguoiThayTruongTang`, `boPhanCua`, `laPhuTrachBoPhan`) trong `BE/m09/nhiemvu/internal/` Nối kiểm/gỡ/chuyển hiệu lực/snapshot phân công lúc công bố, `AssignmentApi` thật cho phủ Trưởng tầng và các hàm phân công còn thiếu của SPI 003/005/006/007 (tách khỏi T013/T015 vì chờ bảng T019).
- [x] T022 [US3] Tạo `BE/m09/phancong/web/PhanCongController.java`: `/giao-truong-tang`, `/{id}/ket-thuc`, `/ca/{id}/phan-cong`, `/phan-cong/{id}`, `/huy`, `/ket-thuc`, `/ca/{id}/sao-chep-phan-cong`; màn web `frontend/src/tinh-nang/phan-cong/`. **Tên/chỗ cài**: `m09.assignment.web.AssignmentController`, `features/shifts/AssignmentPanel`, `FloorManagerPanel` theo README; 9 thao tác gốc và POST `/shifts/{id}/assignments/preview` đã được người dùng duyệt cho cảnh báo trước lưu FR-031. Chỉ sửa bản hợp đồng đã duyệt và sinh lại client.
- [ ] T022b [P] [US3] Màn app `mobile/lib/nhan-vien/phan-cong/` (tên logic); chưa mở triển khai chức năng mobile theo CLAUDE.md, không chặn phần backend/web.

**Checkpoint**: T017–T022 backend/web hoàn thành (2026-10-24): test trước, Spotless qua; clean verify chạy nền qua 739 test (204 đơn vị/kiến trúc + 535 IT), 0 lỗi/thất bại/bỏ qua. Web 111 test/19 file, lint/build qua; kiểm giao diện ở 1440/900/390 px, sáng/tối, bàn phím và vai trò. API/enum/fake 001 đã khớp B trên develop b049b1f; bản cài 001 chưa gộp vào nhánh E, nguồn 003/005 trong test dùng fake chung, cần C/D review/cài thật. T022b mobile chưa mở; T026–T028 bàn giao và T037 tỷ lệ thuộc phase sau. Chưa commit/push Phase 4.

---

## Phase 5: User Story 1 – Người phụ trách ca lập bàn giao và ca sau xác nhận (Priority: P1)

**Goal**: Một bàn giao mỗi ca; danh sách bản ghi nguồn để tham khảo (không chép tự động); mục và nhận định nhóm 3 có bản nháp khóa sau `chot_luc`; hoàn tất; ý kiến; xác nhận nguyên tử với chuyển công việc (005) và cảnh báo (007); nhắc (FR-038 → FR-047, DBR-20, BR-M09-06 → 08).

**Independent Test**: Ca ngày tầng 2 có công việc Quá hạn, liều Bỏ lỡ, chỉ số vượt ngưỡng, cảnh báo mở, người trở về; 17:30 người phụ trách ca lập bàn giao, danh sách nguồn đủ, hoàn tất; ca sau xác nhận → công việc, cảnh báo chuyển.

### Tests cho User Story 1

**Ghi chú triển khai Phase 5 (2026-10-25)**: đường dẫn logic chuyển sang `m09/handover/internal/{domain,persistence,service}` và `web/{request,response}` theo README/R-02. Các test IT theo quy tắc được gom trong `HandoverIT`/`HandoverSchemaIT`, hai test thuần `NextHandoverShiftTest`/`CriticalHandoverItemTest`; web có `HandoverPage.test.jsx`/`HandoverCorrections.test.jsx`. Migration thực là `V008_04__shift_handovers.sql`. API bổ sung đã được chủ dự án duyệt, ghi ở plan và contracts mục 2.5; A/B/C/D review ở team-plan mục 8. HandoverEventSerializationTest thêm 18 trường hợp đọc lại sự kiện; bộ lọc ResidentContractIT của B đã được chủ dự án duyệt sửa riêng theo gốc đường dẫn, giữ nguyên assertion/schema. T023–T029 chỉ được đánh xong sau clean verify cuối xanh; T030 mobile chưa mở.

- [x] T023 [P] [US1] Test đơn vị `BT/m09/XacDinhCaSauTest.java`, `MucNghiemTrongTest.java`
- [x] T024 [P] [US1] Test `BT/m09/BR_M09_06_DanhSachNguonIT.java`, `FR_040_MucBanNhapIT.java`, `FR_041_HoanTatIT.java`, `BR_M09_07_ChoBanGiaoIT.java`, `FR_043_NhacBanGiaoIT.java`: danh sách nguồn đọc song song 003 → 007, 013, 014, không có "chép tất cả"; mục nghiêm trọng cần ghi chú; hoàn tất cần nhận định chung; ca không đóng khi chưa lập xong; nhắc lập, nhắc hết ca, nhắc chưa xác nhận (Q27 → Q33)
- [x] T025 [P] [US1] Test `BT/m09/BR_M09_08_XacNhanBanGiaoIT.java`, `FR_044_XacNhanNguyenTuIT.java`, `FR_042_LenhDongThoiIT.java`, `DBR_20_MotBanGiaoMoiCaIT.java`, `BanGiaoTriggerIT.java`, `FR_047_ChamSocXemBanGiaoIT.java`: xác nhận gọi `CongViecCaApi.khiXacNhanBanGiao` và `CanhBaoBanGiaoApi.khiXacNhanBanGiao` trong cùng transaction, lỗi thì rollback; ý kiến và gửi lại; hai lệnh đồng thời; trigger khóa sau `chot_luc` và `DA_XAC_NHAN` (Q34 → Q39)

### Implementation cho User Story 1

- [x] T026 [US1] Tạo `MIG/V008_04__ban_giao.sql`: `ban_giao` (`ca_id UNIQUE`; `trang_thai ENUM('BAN_NHAP','DA_LAP','CO_Y_KIEN','DA_XAC_NHAN')`; trigger chặn đổi khi `DA_XAC_NHAN`, chặn đổi `chot_luc` đã có), `muc_ban_giao` (nhóm 3 có bản nháp; `nhom` 9 giá trị ở data-model mục 7; trigger khóa sau `chot_luc`), `nhan_dinh_ban_giao` (`loai` `CHUNG|THEO_NGUOI_CAO_TUOI`), `y_kien_ban_giao`, `phan_hoi_y_kien` (chỉ thêm) Nối `ShiftHandoverState` thật cho job T014 và trạng thái bàn giao của SPI 005/006/007 khi có nguồn; T028 hoàn tất nghiệp vụ.
- [x] T027 [US1] Cài `BE/m09/bangiao/internal/BanGhiNguonBanGiaoService.java`: đọc song song `CongViecBanGiaoApi`, `LieuBanGiaoApi`, `ChiSoApi.vuotNguongTrongKhoang`, `CanhBaoBanGiaoApi.dangMo`, `LuotVangApi`, `TrangThaiNguoiCaoTuoiApi.khoangTrangThai`, `TinhTrangGiuongApi.choSanSangQuaHan`, `DoGuiBanGiaoPort`, `ChuyenDiNhiemVuPort` (research N-10; Q-307)
- [x] T028 [US1] Cài `BE/m09/bangiao/internal/BanGiaoService.java`: lập (người phụ trách ca hoặc Trưởng tầng từ mốc FR-039), thêm/xóa mục ở Bản nháp (`If-Match`), nhận định, hoàn tất (`chot_luc`; ca `CHO_BAN_GIAO → DA_DONG`), xác nhận (`XacDinhCaSau`; nguyên tử với 005, 007), ý kiến, bổ sung gửi lại; `DinhChinhDuocProvider` `M09.BAN_GIAO`; nhắc trong việc theo phút
- [x] T029 [US1] Tạo `BE/m09/bangiao/web/BanGiaoController.java`: `POST/GET /ca/{id}/ban-giao`, `GET /ban-giao/{id}`, `/ban-ghi-nguon`, `/muc`, `/muc/{mucId}`, `/nhan-dinh`, `POST /hoan-tat`, `/xac-nhan`, `/y-kien`, `/bo-sung-gui-lai`; màn web `frontend/src/tinh-nang/ban-giao/` với test Vitest (không có nút "chép tất cả", mục không bỏ được, lỗi thiếu ghi chú)
- [ ] T030 [P] [US1] Tạo app `mobile/lib/nhan-vien/ban-giao/` (xem ngoại tuyến trong phạm vi ca; lệnh chỉ trực tuyến) và test `mobile/test/nhan-vien/ban_giao_ngoai_tuyen_chi_xem_test.dart`, `xoa_ban_giao_khi_het_pham_vi_ca_test.dart`

**Checkpoint backend/web US1 — 2026-10-25**: T023–T029 hoàn thành sau TDD và `./mvnw clean verify` chạy nền trên JDK 25/MySQL 8.0: 311 test đơn vị/kiến trúc + 1000 IT = 1311, không lỗi/thất bại/bỏ qua; gồm test đọc lại sự kiện, hợp đồng 001/008, toàn bộ ArchTests và test hồi quy gắn nhầm nguồn liều. Spotless qua. Web `npm ci`, 242 test/35 file, lint/build qua; Edge 1440/900/390 px, sáng/tối, bàn phím/vai trò đã kiểm. Plan/glossary/CLAUDE/team-plan mục 8 cập nhật; A/B/C/D review các phần đã được chủ dự án duyệt, nguồn thật còn thiếu cần chủ module nối. T030 mobile chưa mở. Chưa commit/push hoặc mở PR; người dùng tự thực hiện Git.

**Checkpoint**: P1 hoàn tất: lịch ca → phân công → bàn giao.

---

## Phase 6: User Story 4 – Hồ sơ nhân viên, giấy phép, đào tạo và cảnh báo hết hạn (Priority: P2)

**Goal**: Cho nghỉ việc với tác động (khóa tài khoản 002, rút khỏi ca, công việc thành chung), nhận lại; bộ phận và Phụ trách bộ phận; gia hạn, thu hồi, sửa sai chứng chỉ; cảnh báo đào tạo sắp hết hạn (FR-001a, FR-002 → FR-010, DBR-36, BR-M09-04, BR-M09-05).

**Independent Test**: Điều dưỡng D có giấy phép hết hạn sau 59 ngày và đào tạo sơ cứu hết hạn sau 61 ngày → cảnh báo giấy phép (002) có, cảnh báo đào tạo xuất hiện khi tới mốc CFG-M09-03.

### Tests cho User Story 4

- [x] T031 [P] [US4] Test `BT/m09/BR_M09_04_NghiViecIT.java`, `FR_003_NhanLaiIT.java`, `FR_001a_PhuTrachBoPhanIT.java`, `DBR_36_MotBoPhanIT.java`, `NhanVienTriggerIT.java`: nghỉ việc gọi `TaiKhoanApi.khoaDoNghiViec` và `CongViecCaApi.khiNghiViec` cùng transaction, rút khỏi ca tương lai, hủy phân công; Phụ trách bộ phận cần người thay (Q-292); một bộ phận mỗi nhân viên (Q16 → Q19)
- [x] T032 [P] [US4] Test `BT/m09/BR_M09_05_CanhBaoDaoTaoIT.java`, `FR_009_ThuHoiIT.java`: cảnh báo một lần; thu hồi làm mất hiệu lực, kết thúc phân công cần chứng chỉ (Q14, Q15)

### Implementation cho User Story 4

- [x] T033 [US4] Cài lệnh Cho nghỉ việc (hiệu lực ngay hoặc tương lai qua việc theo phút), Nhận lại làm việc, đổi bộ phận, chỉ định Phụ trách bộ phận trong `NhanVienService.java` (research N-04)
- [x] T034 [US4] Cài gia hạn, thu hồi, sửa sai chứng chỉ trong `ChungChiService.java`; việc theo phút chứng chỉ hết hiệu lực theo mốc; việc hằng ngày cảnh báo đào tạo sắp hết hạn (CFG-M09-03), bộ phận chưa có Phụ trách (research N-05)
- [x] T035 [US4] Bổ sung endpoint `POST /nhan-vien/{id}/cho-nghi-viec`, `/nhan-lai-lam-viec`, `/doi-bo-phan`, `GET /bo-phan`, `POST /bo-phan/{id}/chi-dinh-phu-trach`, `POST /chung-chi/{id}/gia-han`, `/thu-hoi`, `/sua-sai`; màn web `frontend/src/tinh-nang/nhan-vien/`

**Ánh xạ triển khai Phase 6 (2026-10-25)**: T031 gom StaffEmploymentIT/DepartmentCommandsIT và dùng lại DepartmentDutyIT/StaffSchemaIT; T032 gom CredentialLifecycleIT, StaffNotificationsTest. T046 gồm StaffImportIT, StaffImportRowValidatorTest, StaffImportTransportIT. Lớp đặt trong m09/staff, duty, credential theo backend/README; màn trong features/shifts. V008_05 cho khoảng rỗng khi hủy giao Trưởng tầng tương lai, V008_06 import; T037 chuyển số migration sang V008_07. Đánh [x] sau clean verify cuối phase đã xanh.

**Checkpoint**: Hồ sơ nhân viên đầy đủ.

---

### Bổ sung 2026-10-22: import hồ sơ nhân viên (US4 #10 → #14; FR-001b, FR-001c; Q-382)

- [x] T046 [P] [US4] Test `BT/m09/FR_001b_ImportNhanVienIT.java`, `KiemTraDongImportTest.java`, `ImportHieuNangIT.java` (quickstart I1 → I7): xem trước không ghi; mọi loại lỗi đúng dòng, cột, lý do; trùng hồ sơ mọi trạng thái và trùng trong file; xác nhận gửi lại file kiểm lại, còn lỗi thì 0 hồ sơ; hồ sơ tạo ra không tài khoản, không bộ phận, `nguon = IMPORT`; nhật ký đủ trường, không lưu file; quá CFG-M09-13, sai mẫu, sai loại → 422; chỉ Quản lý viện; 500 dòng ≤ 10 giây (SC-013)
- [x] T047 [US4] Tạo `MIG/V008_xx__import_nhan_vien.sql` (số theo thứ tự migration 008 lúc làm): `lan_import_nhan_vien`; cột `nhan_vien.dien_thoai_chuan` (chỉ mục, điền cho dữ liệu cũ), `nguon`, `lan_import_id`; cài `BE/m09/nhanvien/internal/ImportNhanVienService.java` (`KiemTraDongImport` hàm thuần, khóa `FOR UPDATE` theo tập số, chèn theo lô, `NV_IMPORT`) dùng `ExcelBieuMau` của `common.bieumau` (007 T104; thêm `fastexcel-reader` 0.20.2 vào `pom.xml` (đã duyệt 2026-10-22, 007 research S-26)); service tạo, sửa hồ sơ ghi `dien_thoai_chuan` (research N-16)
- [x] T048 [US4] Tạo endpoint `GET /nhan-vien/import/mau`, `POST /nhan-vien/import/xem-truoc`, `POST /nhan-vien/import/xac-nhan` trong `NhanVienController.java` và màn web import trong `frontend/src/tinh-nang/nhan-vien/` (bảng xem trước tô lỗi theo dòng, cột; nút xác nhận chỉ bật khi không lỗi) cùng test Vitest `ImportNhanVien.test.jsx`

**Checkpoint Phase 6 phần E — 2026-10-25**: T031–T035, T046–T048 hoàn thành backend/web sau TDD. Clean verify chạy nền JDK 25/MySQL 8.0: 322 lượt test đơn vị/kiến trúc + 1040 IT, không lỗi/thất bại/bỏ qua; Spotless qua. Web 256 test/38 file, lint/build qua; Edge 1440/900/390, sáng/tối/bàn phím/chỉ xem qua. Import 500 dòng từng bước dưới 10 giây, đồng thời không tạo trùng, HTTP multipart thật chống lặp theo nội dung file. Các phần đã duyệt của A/C ghi review ở team-plan mục 8; bản thật 005 và listener đóng cảnh báo 009 do chủ module nối. T037 tính tỷ lệ thuộc Phase 7. Đã cập nhật plan/glossary/CLAUDE; không commit/push/merge.

## Phase 7: User Story 5 – Cảnh báo tỷ lệ phục vụ theo trọng số chăm sóc (Priority: P2)

**Goal**: Tỷ lệ = tổng trọng số mức chăm sóc / số nhân viên được tính; cảnh báo dưới ngưỡng một lần; tính lại theo sự kiện (FR-034 → FR-037a, BR-M09-02).

**Independent Test**: Tầng 2 có 20 người cơ bản (trọng số 1), 6 người đặc biệt (2,5), tổng 35; ngưỡng 8 trọng số/nhân viên; 4 nhân viên được tính → cảnh báo.

### Tests cho User Story 5

- [x] T036 [P] [US5] Test đơn vị `BT/m09/TinhTyLePhucVuTest.java` và `BT/m09/BR_M09_02_TyLePhucVuIT.java`, `FR_036_CanhBaoMotLanIT.java`, `BR_M09_14_TinhTheoCaIT.java` (người có ca chưa có phân công vẫn được tính; yêu cầu đổi mức chăm sóc đang chờ duyệt không tính trước) (Q20, Q21)

### Implementation cho User Story 5

- [x] T037 [US5] Tạo bảng `lan_tinh_ty_le` (nhóm 3; `su_kien` 10 giá trị; `tong_trong_so DECIMAL(8,2)`) trong `MIG/V008_07__ty_le.sql`; cài `BE/m09/tyle/internal/TyLePhucVuService.java` (`TinhTyLePhucVu`; listener `MucChamSocDaDoi`, `NguoiCaoTuoiDaChuyenTrangThai`, `PhanBoDaBatDau`, `PhanBoDaDong`, `DaChuyenGiuong`; cảnh báo một lần qua `dang_duoi_nguong`), việc hằng giờ ca sắp bắt đầu dưới ngưỡng (CFG-M09-08); `NhanSuThongKeApi` (016) (research N-09) Nối staffingRatio trong chi tiết ca và cảnh báo xem trước công bố; T016 để null khi chưa có lần tính.
- [x] T038 [US5] Tạo endpoint `GET /ca/{id}/ty-le-phuc-vu` và màn web `frontend/src/tinh-nang/ty-le-phuc-vu/`

**Checkpoint**: Tỷ lệ phục vụ chạy độc lập.

**Checkpoint Phase 7 phần E — 2026-10-25**: T036–T038 hoàn thành sau TDD. Có bộ tính, migration `V008_07__staffing_ratios.sql`, lịch sử/cảnh báo/nhắc, endpoint/panel trong `features/shifts`, nối lưu/công bố/nghỉ việc và listener sau commit. StaffStatisticsApi đã có các truy vấn tỷ lệ, bàn giao, chứng chỉ, lịch sử phân công/Trưởng tầng/thành viên/phụ trách và căn cứ nhiệm vụ; hai thay đổi API phân trang/shiftId nullable đã được chủ dự án duyệt, có test và ghi A/016 review. Clean verify chạy nền JDK 25/MySQL 8.0 qua 333 lượt test đơn vị/kiến trúc + 1074 IT = 1407, không lỗi/thất bại/bỏ qua (07:14); Spotless qua. Web 265 test/40 file, lint/build qua; Edge ba bề rộng/sáng/tối/bàn phím đã kiểm. B/C còn review/cài nguồn 003/004 thật theo team-plan mục 8; không dùng fake trong production, thiếu nguồn báo rõ chưa đủ dữ liệu. Chi tiết tại plan "Phase 7 hoàn thành phần E". Chưa commit/push/merge.

---

## Phase 8: User Story 6 – Điều chỉnh ca đã công bố: vắng ca, bổ sung, giao trưởng tầng (Priority: P3)

**Goal**: Lệnh sau công bố: bổ sung nhân viên, chuyển người phụ trách, ghi nhận vắng (công việc thành chung, liều thành chung, phân công kết thúc, tỷ lệ tính lại), hủy ca; port nội bộ cho 015 (FR-017 → FR-021, BR-M04-13 phần 008).

**Independent Test**: Ghi S vắng ca ngày lúc 07:20 → công việc của S thành chung, liều của người do S phụ trách thành liều chung, phân công của S kết thúc, tỷ lệ tính lại.

### Tests cho User Story 6

- [x] T039 [P] [US6] Test các quy tắc FR-017 → FR-021, FR-052/053, FR-058/059 và BR-M09-13 trong `BT/m09/PostPublicationShiftIT.java`, `PostPublicationPortsIT.java`, `PostPublicationNotificationsTest.java`, `AbsenceSourceContractsTest.java` (tên phương thức bắt đầu mã quy tắc; gộp fixture MySQL/quyền theo plan Phase 8): vắng ca gọi `ShiftTaskApi.onAbsence` cùng transaction; vắng của người phụ trách ca chuyển người phụ trách; bàn giao đang dở đổi người bàn giao và giữ nội dung/tác giả; lý do vắng chỉ người có quyền thấy (Q22, Q23, Q25, Q40, Q41). Kiểm rollback, đồng thời, biên giờ, phạm vi, thông báo ngoài ứng dụng và ca khác không đổi; nguồn 005 dùng bản giả chung trong test, C còn nối nguồn thật.

### Implementation cho User Story 6

- [x] T040 [US6] Cài lệnh bổ sung nhân viên, chuyển người phụ trách ca, ghi nhận vắng, hủy ca trong `BE/m09/shift/internal/service/PostPublicationService.java`; port `shift/internal/service/port/ShiftCommandPort.java` và `assignment/internal/service/port/AssignmentCommandPort.java` cùng bộ cài MANDATORY cho 015 (research N-07; plan Phase 8, module-interfaces mục 2.8).
- [x] T041 [US6] Bổ sung đúng endpoint đã dịch: `POST /shifts/{id}/add-staff`, `/change-lead`, `/record-absence`, `/cancel`, `GET /duty-basis-audit-logs`; nối các lệnh và tra cứu vào web `features/shifts` theo giai đoạn backend/web. Đối chiếu toàn bộ HTTP 008 xanh; không đổi YAML/client sinh. Phần mobile tách T041b theo CLAUDE.md/mobile README.
- [ ] T041b [US6] Khi chủ dự án mở mobile nhân viên: app `mobile/lib/nhan-vien/vang-ca/` và test `mobile/test/nhan-vien/ghi_nhan_vang_ca_test.dart`; đặt đường dẫn thật theo mobile/README.md. Chưa làm trong giai đoạn chỉ mở mobile người cao tuổi.

**Checkpoint**: Mọi user story của 008 chạy độc lập.

---

## Phase 9: Polish & Cross-Cutting

- [x] T042 [P] Chốt đối chiếu đầy đủ `ApiContractIT` với `contracts/api-008.yaml` (pending và pendingResponses rỗng, checkOperations=true); bổ sung `BT/m09/M09RecordProtectionIT.java` kiểm đủ BEFORE UPDATE/DELETE trên mười một bảng ghi nhận/lịch sử JDBC và chặn quản trị sửa/xóa bản gốc. Giữ test chung 000 của A; lý do tách và bảng truy vết ở plan Phase 9, `validation.md`.
- [x] T043 Kiểm các SPI của 002, 005–007 bằng bean thật của 008 trong `BT/m09/StaffModuleIntegrationIT.java`, không nhập FakeM15Config: nối ScopeApi thật của A, kiểm lịch sử phụ trách/chuyển người/vắng/biên CFG và phạm vi. Giữ test cô lập của chủ module khác; phần chạy lại toàn luồng nguồn thật tách T043b.
- [ ] T043b Khi C/D có nghiệp vụ 005–007 và bộ test tương ứng: chủ module thay cấu hình fake nguồn 008 trong lượt kiểm tích hợp, chạy lại test với CareShiftPort/NursingShiftPort/AlertStaffPort thật. A review hồi quy 002 với nguồn 008. Không coi test fake callback là nghiệm thu nguồn công việc/liều/cảnh báo thật.
- [x] T044 Chạy nền `./mvnw clean verify`, `npm test`, lint/build và `flutter test --no-pub`; đối chiếu tự động các Q/SC của quickstart theo `validation.md`. Flutter hiện chỉ kiểm phần người cao tuổi đã mở; phần demo đầu–cuối/đo thực tế và mobile nhân viên tách T044b.
- [ ] T044b Sau khi đủ nguồn/dữ liệu demo 001/003–007/013–015 và mở mobile nhân viên: chạy quickstart mục 3 đầu–cuối, Q38 trên app; đo SC-002/SC-010 với ít nhất mười lần thao tác thật, SC-003 bằng vận hành thử một tháng. Lưu chứng cứ theo hướng dẫn ở `validation.md`, không dùng thời gian test tự động thay số đo người dùng.
- [x] T045 Rà truy vết data-model mục 11 và `validation.md`: DBR-20, 21, 23, 25, 36; BR-M09-01 → 08, 12 → 17, BR-M04-13, BR-M05-05 (phần 008); FR-048 → FR-060 đều có task/test hoặc task nghiệm thu còn mở. Ghi đúng giới hạn phụ thuộc/mobile và bằng chứng từng Q/SC (constitution XV); không đánh đạt FR-060 mobile khi chưa làm T030.

---
- [x] T049 [P] *(Bổ sung 2026-10-22, 007 FR-047i)* Chốt ba hàm `AlertStaffPort.onShift`, `floorDuringShift`, `floorsWithShift` đã cài trong AlertStaffLookup/ShiftQueries; `BT/m09/AlertStaffPortIT.java` kiểm K1 với bean thật, biên ca/vắng/giờ hành chính CFG/danh sách tầng, không dùng fake ca. Sửa ví dụ K1 theo Q-332: vai trò không xếp ca là Quản lý viện/Hành chính/Kế toán. Phần kiểm nghiệp vụ US13 của D tách T049b.
- [ ] T049b Khi D có US13 của 007: chạy lại H1–H9 với AlertStaffPort thật (007 T063/T099); D review ba hàm và bản giả chung hiện có. Nhánh hiện chưa có bộ test nghiệp vụ này, chưa nghiệm thu nút hỗ trợ khẩn cấp đầu–cuối.
- [ ] T050 *(bổ sung 2026-10-29, research R-15 bản 2 của 000; chờ A xong 000 T108)* Gộp bảng đã cài theo data-model mục 0: migration `V008_95` tạo bảng gộp hoặc cột `JSON`, chép dữ liệu (dòng chép sang bảng chung ghi `chi_tiet.legacy`), đổi khóa ngoại và `tep.chu_so_huu_id`, `DROP TABLE` bảng cũ; giữ id của bảng nhóm 3 là đích của `dinh_chinh`, `nhat_ky` (R-15 mục 5); sửa entity, lớp truy cập dữ liệu, service sang `CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`; job ghi lần chạy ra file log. Test: toàn bộ test của module xanh như trước khi gộp, test bảo vệ nhóm dữ liệu (trigger, quyền `eldercare_app`) cho bảng gộp, test migration chép đủ số dòng trên dữ liệu demo; không đổi API (`ApiContractIT` xanh)

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
