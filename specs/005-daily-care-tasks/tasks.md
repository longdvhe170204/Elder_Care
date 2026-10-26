---
description: "Danh sách task triển khai feature 005 – Chăm sóc hằng ngày"
---

# Tasks: Chăm sóc hằng ngày (005)

**Input**: Tài liệu thiết kế ở `specs/005-daily-care-tasks/` (plan.md, spec.md, research.md C-01 → C-17, data-model.md, contracts/api-005.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2 (gồm khung ngoại tuyến `NgoaiTuyenHandler`); 001 (`ThamGiaChuyenTrangThai`, `DanhGiaApi`), 003 (`ViTriApi`, `CoCauViTriApi`), 004 (`HopDongDocApi`, `TrangThaiCoMatBanTruPort`, sự kiện bán trú) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 12.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M04 (phần 8.1 → 8.7): `BE/m04/{api,kehoach,comat,congviec,ghinhan,checklist,danhmuc,quytac,quahan,thoikhoabieu,trangthai,lich}/`; phần 8.8 → 8.10 thuộc feature 014
- Web `frontend/src/tinh-nang/{ke-hoach-cham-soc,lap-cong-viec-ca,checklist,viec-qua-han,diem-danh-ban-tru,loai-cong-viec,thoi-khoa-bieu}/`; app nhân viên `mobile/lib/nhan-vien/{checklist,ghi-nhan,dong-bo,lap-cong-viec-ca,viec-qua-han,diem-danh-ban-tru}/`

---

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; Q-347, Q-349, Q-350): sửa T007, T008, T012, T014, T016, T017, T033, T036 → T041; thêm T007a, T016a. Mã task cũ giữ nguyên. Phụ thuộc mới: `NguonYKienApi` (001 T012a), `QuanHeNguoiThanApi.thuocNguoiCaoTuoi` (bản giả T002).

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/m04/{api,careplan,attendance,task,recording,checklist,catalog,rules,overdue,timetable,status,scheduling}/` với `package-info.java` (research C-01; tên mã theo glossary mục 11, lớp sẽ đặt theo backend/README.md)
- [ ] T002 [P] Khai báo trước tám SPI cần cho fixture trong `BE/m04/api/spi/` (phần tương ứng của T003), tạo bản giả do 005 sở hữu trong `BT/support/fake/m04/`: `ShiftStaffPort` (008), `TaskSourcePort` (007, 011, 014), `VitalSignPort`, `AlertReadPort` (007), `MedicationChecklistPort` (006), `DayCarePickupPort` (012), `QualityCheckPort` (014), `TimetableSourcePort`. Công tắc `PUT /api/v1/testing/fakes/{module}`, body `{enabled: boolean}`, trả 204 (chủ dự án duyệt 2026-10-26); chỉ khi import `FakeM04Config` ở profile `test`/`demo`, không đưa fake vào production. Lý do đổi thứ tự kỹ thuật và bằng chứng kiểm ở plan mục Ghi chú triển khai.

**Kiểm 2026-10-26**: T001–T002 đã cài; 23 ca mới và các kiểm kiến trúc xanh. `clean verify` nền kết thúc exit1: 461 test đơn vị/kiến trúc xanh; 1098 IT còn 6 failure + 9 error ở 8 lớp M01 đã bàn giao B. Spotless qua. Giữ hai checkbox trống theo gate cuối phase, không ghi Phase1 đã nghiệm thu. Chi tiết ở plan/Working-history.md; task tiếp theo T003, không tạo lại tám SPI đã có.

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `m04.api` và SPI, sự kiện, danh mục loại công việc, bảng khóa theo người, khung hai job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T003 [P] Tạo giao diện công khai `BE/m04/api/`: `KeHoachChamSocApi`, `CoMatBanTruApi`, `CongViecCaApi` (mọi hàm MANDATORY), `CongViecBanGiaoApi`, `NguonCongViecApi`, `CongViecVangMatApi`, `CongViecDoChiSoApi`, `CongViecThongKeApi`, `ThoiKhoaBieuApi`, `KetQuaChamSocCongApi` (contracts mục 1.2 → 1.10a) và SPI mục 2.1 (`NhanSuCaPort`, `NguonCongViecPort`, `ChiSoPort`, `LieuThuocChecklistPort`, `CanhBaoDocPort`, `BanGhiDonBanTruPort`, `KetQuaKiemTraPort`, `ThoiKhoaBieuNguonPort`) (research C-02)
- [ ] T004 [P] Tạo sự kiện `BE/m04/api/sukien/` theo contracts mục 1.11 (`YeuCauCanhBao`, `YeuCauDongCanhBao`, `NhacQuaHan`, `NhacQuaHanGop`, `CongViecQuaHanCanXuLy`, `CaChuaCoCongViec`, `CanLapCongViec`, `KeHoachDaDoi`, `NguonCongViecDaDoi`, sự kiện phiên bản kế hoạch, `NhacXemLaiThoaThuan` (bổ sung 2026-10-18, Q-360), `CanXemXetKeHoach`, `CongViecThanhChung`, `CongViecChungKhongAiDuDieuKien`, `DaDiemDanhDen`, `VangKhongBao`, `BanTruConCoMatHetNgay`, `TrangThaiCoMatDaXacDinh`, `TrangThaiCoMatDaDoi`, `KetQuaCoTinhPhi`, `KetQuaTinhPhiDaDinhChinh`, `AnKemKeoDai`, `CongViecDoBiBo`, `KetQuaNguDaGhi`); mỗi sự kiện mang khóa `loại + id + mốc` (NFR-04); sự kiện gửi 009 cài `CoYeuCauThongBao` (research C-13)
- [ ] T005 Tạo `MIG/V005_01__nen_m04.sql`: `cham_soc_nct` (`nct_id` khóa chính, `phien_ban`), `lan_chay_m04 (viec, moc)` khóa chính hai cột; `loai_cong_viec` (mẫu nhóm 1; `nhom ENUM(...)` 18 giá trị ở data-model mục 4, `UONG_THUOC` không chọn được cho mục kế hoạch; `chuyen_mon BOOLEAN`; `loai_ket_qua ENUM('AN_UONG','ML','GIA_TRI_DO','VAN_DONG','NGU','TAM_TRANG','HANH_VI','SO_LUONG_VAT_PHAM','DA_THUC_HIEN','KHONG_GHI')`; `muc_mac_dinh ENUM('THUONG','QUAN_TRONG','BAT_BUOC')`; `khung_truoc`, `khung_sau SMALLINT NULL` – `NULL` dùng CFG-M04-02; `co_tinh_phi`, `dich_vu_id`, `gioi_han_tren`, `loai_chi_so`), `loai_cong_viec_chung_chi (loai_cong_viec_id, ma_chung_chi)`, `danh_muc_gia_tri_ket_qua` (`loai` `NGU_CHAT_LUONG|TAM_TRANG|HANH_VI`, `nhom_quy_tac` `MAT_NGU|TIEU_CUC|NULL`)
- [ ] T006 Cài `BE/m04/trangthai/internal/KhoaChamSoc.java` (khóa `cham_soc_nct` theo thứ tự research C-03) và khung job `BE/m04/lich/ViecTheoPhutM04.java` (mỗi phút, ShedLock, chạy bù, nhắc gộp) và `ViecHangNgayM04.java` (00:00 và khi khởi động; `lan_chay_m04`) – các việc đăng ký dần ở story (research C-12; NFR-04, SC-005)
- [ ] T007 Cài danh mục loại công việc, giá trị kết quả (AC-01; tạo, sửa, ngừng hiệu lực) trong `BE/m04/danhmuc/internal/LoaiCongViecService.java` và `BE/m04/danhmuc/web/DanhMucM04Controller.java` (`/loai-cong-viec`, `/{id}`, `/{id}/ngung-hieu-luc`, `/danh-muc-gia-tri-ket-qua`) (FR-038; research C-15)
- [ ] T007a Tạo `MIG/V005_01a__loai_ly_do.sql` và cài danh mục loại lý do Không thực hiện trong `BE/m04/danhmuc/internal/LoaiLyDoService.java`, endpoint `/loai-ly-do-khong-thuc-hien`, `/{id}`, `/{id}/ngung-hieu-luc` trong `DanhMucM04Controller`: mẫu nhóm 1 cộng `co_dinh BOOLEAN`; nạp dòng cố định "Người cao tuổi từ chối"; `CHECK (NOT co_dinh OR trang_thai = 'HIEU_LUC')`; trigger chặn đổi `ten`, `co_dinh` của dòng cố định; sửa hoặc ngừng dòng cố định → 409 `LOAI_LY_DO_CO_DINH`; test `BT/m04/FR_028a_LoaiLyDoKhongThucHienIT.java` (phần danh mục) viết trước (FR-028a, research C-18; bổ sung 2026-10-18)
- [ ] T008 [P] Cài provider cho 000/002 trong `BE/m04/trangthai/internal/`: `PhanLoaiTruongProvider` (`SUC_KHOE` cho giá trị kết quả, `noi_dung` mục và công việc, mọi trường thỏa thuận và ý kiến, loại lý do và ghi chú Không thực hiện; `CONG_KHAI_NOI_BO` cho trạng thái, thời điểm, có mặt), `LuuTruProvider` theo data-model mục 11, `GanPhamViProvider` (`M04.KE_HOACH`, `M04.CONG_VIEC`, `M04.KET_QUA_GHI_NHAN`, `M04.CO_MAT_BAN_TRU`)
- [ ] T009 [P] Test kiến trúc `BT/m04/M04ArchTest.java`: cấm gói `m04` gọi API của 008, 007 trực tiếp (chỉ qua SPI); cấm gói `m04.lich` gọi lệnh tạo công việc (Q-306)

**Checkpoint**: Nền 005 sẵn sàng.

---

## Phase 3: User Story 1 – Kế hoạch chăm sóc theo phiên bản (Priority: P1) 🎯 MVP

**Goal**: Phiên bản Nháp → Chờ duyệt → Chờ hiệu lực → Hiệu lực → Hết hiệu lực; mục chỉ sửa ở Nháp; hiệu lực từ ngày sau ngày duyệt; so sánh phiên bản (FR-001 → FR-008, DBR-11, BR-M04-19, BR-M04-25).

**Independent Test**: Lập nháp 3 mục, gửi duyệt, Bác sĩ duyệt với ngày mai; hôm nay phiên bản cũ còn hiệu lực, ngày mai phiên bản mới hiệu lực và phiên bản cũ Hết hiệu lực; thử sửa phiên bản đang hiệu lực.

### Tests cho User Story 1

- [ ] T010 [P] [US1] Test đơn vị `BT/m04/BR_M04_25_ChuoiMoiNGioTest.java` (chuỗi thời điểm dự kiến của mục theo bốn kiểu tần suất) và `SoSanhPhienBanTest.java`
- [ ] T011 [P] [US1] Test `BT/m04/FR_003_VongDoiKeHoachIT.java`, `BR_M04_19_PhienBanKhoaIT.java`, `DBR_11_MotPhienBanHieuLucIT.java`, `FR_005_TuDuyetIT.java`, `MucKeHoachTriggerIT.java`, `FR_002a_ThoaThuanRuiRoIT.java` (bổ sung 2026-10-18: thỏa thuận khóa ngoài Nháp, sao chép sang phiên bản mới, người quyết định khớp nhận định, nguồn ý kiến qua `NguonYKienApi`, không công việc nào có căn cứ là thỏa thuận; Q39 → Q41): mọi chuyển của bảng FR-003; trigger khóa mục ngoài Nháp; hai phiên bản Hiệu lực bị `UNIQUE` chặn; `ngay_hieu_luc > DATE(duyet_luc)` (Q-34); tự duyệt theo Q-10; nhắc Chờ duyệt qua `NhacQuaHan` (Q1 → Q7)

### Implementation cho User Story 1

- [ ] T012 [US1] Tạo `MIG/V005_02__ke_hoach.sql`: `ke_hoach_cham_soc` (`UNIQUE (nct_id, so_phien_ban)`; `trang_thai ENUM('NHAP','CHO_DUYET','CHO_HIEU_LUC','HIEU_LUC','HET_HIEU_LUC','DA_HUY')`; `ly_do_ket_thuc ENUM('PHIEN_BAN_MOI','TRANG_THAI_CUOI','TRA_LAI','HUY','THU_HOI') NULL`; cột sinh `mo_nct`, `hieu_luc_nct` `UNIQUE`; `CHECK (duyet_luc IS NULL OR ngay_hieu_luc > DATE(duyet_luc))`; chỉ mục `(trang_thai, ngay_hieu_luc)`, `(nct_id, ngay_hieu_luc)`) và `muc_ke_hoach` (`kieu_tan_suat ENUM('SO_LAN_NGAY','MOI_N_GIO','NGAY_TRONG_TUAN','MOT_LAN_MOI_CA')`; `CHECK` `MOI_N_GIO` thì `n_gio BETWEEN 1 AND 23` và có `moc_bat_dau`; `muc_quan_trong ENUM('THUONG','QUAN_TRONG','BAT_BUOC')`; `muc_tieu_ml INT NULL` chỉ nhóm uống nước; trigger `trg_muc_ke_hoach_khoa`) và ba bảng thỏa thuận (bổ sung 2026-10-18): `thoa_thuan_rui_ro` (`lua_chon`, `rui_ro_da_trao_doi VARCHAR(2000) NOT NULL`; `nct_tham_gia BOOLEAN` với `CHECK (nct_tham_gia OR NOT tu_quyet_duoc)`; `bac_si_nhan_dinh_id`, `tu_quyet_duoc`, `thoi_diem_nhan_dinh`, `cach_ho_tro_hieu VARCHAR(2000)` "`NOT NULL`, không rỗng", `ngay_xem_lai_nhan_dinh DATE NOT NULL`; `nguoi_quyet_dinh_loai ENUM('NGUOI_CAO_TUOI','DAI_DIEN_NGUOI_BENH','BAC_SI')` với `CHECK (tu_quyet_duoc = (nguoi_quyet_dinh_loai = 'NGUOI_CAO_TUOI'))`; `nguoi_quyet_dinh_nguoi_than_id`, `nguoi_quyet_dinh_bac_si_id` bắt buộc theo loại; `ly_do_bac_si_quyet_dinh VARCHAR(1000)` bắt buộc khi `BAC_SI`; `ngay_xem_lai DATE NOT NULL`; `thoa_thuan_goc_id`), `thoa_thuan_nguoi_tham_gia` (khóa chính `(thoa_thuan_id, loai, doi_tuong_id)`, `loai ENUM('NGUOI_THAN','NHAN_VIEN')`), `thoa_thuan_y_kien` (mẫu cột nguồn 001 data-model mục 6a; `noi_dung VARCHAR(2000) NOT NULL`); trigger `trg_thoa_thuan_*_khoa` như mục kế hoạch (data-model mục 3)
- [ ] T013 [P] [US1] Tạo entity `BE/m04/kehoach/internal/KeHoachChamSoc.java`, `MucKeHoach.java` và `ChuoiThoiDiemMuc.java` (research C-05)
- [ ] T014 [US1] Cài `BE/m04/kehoach/internal/KeHoachService.java` với `BangChuyenKeHoach`: lập (sao chép mục và thỏa thuận từ phiên bản Hiệu lực), cập nhật mục (`If-Match`; chỉ loại công việc `HIEU_LUC`; khung lấy từ loại hoặc CFG-M04-02 và cố định khi gửi duyệt), gửi duyệt (lý do khi có phiên bản trước), duyệt (Bác sĩ; ngày hiệu lực sau ngày duyệt), trả lại, hủy, thu hồi; phát sự kiện phiên bản; nhắc qua `NhacQuaHan` (CFG-M15-05, 06) (research C-04)
- [ ] T015 [US1] Đăng ký việc `HIEU_LUC_KE_HOACH` của `ViecHangNgayM04`: `CHO_HIEU_LUC` → `HIEU_LUC`, phiên bản cũ → `HET_HIEU_LUC` cùng transaction, phát `KeHoachDaDoi` (không hủy công việc đã lập, BR-M04-03)
- [ ] T016 [US1] Tạo `BE/m04/kehoach/web/KeHoachController.java`: `POST/GET /nguoi-cao-tuoi/{nctId}/ke-hoach-cham-soc`, `GET /ke-hoach-cham-soc/{id}`, `PUT …/{id}/muc`, `POST …/gui-duyet`, `/duyet`, `/tra-lai`, `/huy`, `/thu-hoi`, `GET /ke-hoach-cham-soc/so-sanh`; cài `KeHoachChamSocApi`
- [ ] T016a [US1] Cài lệnh thỏa thuận trong `BE/m04/kehoach/internal/ThoaThuanService.java` và `PUT /ke-hoach-cham-soc/{id}/thoa-thuan` trong `KeHoachController` (`If-Match`; chỉ Nháp; bác sĩ nhận định có AC-03; người quyết định theo nhận định, người đại diện của người bệnh kiểm `QuanHeNguoiThanApi.thuocNguoiCaoTuoi`; mỗi ý kiến qua `NguonYKienApi.kiemTra`; ngày xem lại lưu để nhắc) và việc `XEM_LAI_THOA_THUAN` của `ViecHangNgayM04` (phát `NhacXemLaiThoaThuan` một lần mỗi ngày xem lại cho Điều dưỡng phụ trách và Bác sĩ đã nhận định; test `FR_002a_NhacXemLaiIT.java` viết trước, Q43) (FR-002a, Q-360, research C-18; bổ sung 2026-10-18)
- [ ] T017 [P] [US1] Tạo màn web `frontend/src/tinh-nang/ke-hoach-cham-soc/` (phiên bản, mục, thỏa thuận lựa chọn có rủi ro với nhận định của Bác sĩ, người quyết định, ý kiến có nguồn tách riêng từng người, duyệt, so sánh) và `frontend/src/tinh-nang/loai-cong-viec/`

**Checkpoint**: Kế hoạch chăm sóc chạy độc lập.

---

## Phase 4: User Story 2 – Người phụ trách ca lập công việc của ca (Priority: P1)

**Goal**: Lệnh lập theo lô; `KiemTraLapCongViec` trả lỗi theo dòng; một dòng lỗi thì cả lô không lưu; không gợi ý, không chép sẵn; công việc phát sinh, làm lại; kiểm tra đầu ca một lần mỗi (ca, điều dưỡng); trạng thái theo đồng hồ khi lập muộn (FR-017 → FR-025, FR-026a, FR-029, DBR-12, BR-M04-01, 24, 26, BR-M09-01).

**Independent Test**: 3 người (nội trú có kế hoạch, nội trú Tạm vắng, bán trú); lập công việc cho một ca; thử lập cho người Tạm vắng, bán trú chưa đến, lập trùng, giao người không đủ điều kiện.

### Tests cho User Story 2

- [ ] T018 [P] [US2] Test đơn vị `BT/m04/KiemTraLapCongViecTest.java` và `BangChuyenCongViecTest.java` (mọi chuyển của bảng FR-026)
- [ ] T019 [P] [US2] Test `BT/m04/BR_M04_01_LapCongViecCaIT.java`, `DBR_12_KhongTrungCanCuIT.java`, `FR_021_LapLaiSauHuyVangIT.java`, `BR_M04_24_KiemTraDauCaIT.java`, `BR_M04_26_LapMuonVaChayBuIT.java`, `FR_006_KhongTrungKhungPhienBanCuIT.java`, `BR_M09_01_ChungChiCongViecIT.java`: lỗi theo dòng, lô không lưu khi có lỗi; người vắng/chưa đến bị chặn; trùng căn cứ bị `khoa_trung` chặn (bỏ qua Hủy); kiểm tra đầu ca thứ hai bị chặn; lập muộn vào thẳng `DEN_HAN`/`QUA_HAN`; người thiếu chứng chỉ bị chặn; nhắc ca chưa có công việc; không có endpoint gợi ý (Q8 → Q10, Q38)

### Implementation cho User Story 2

- [ ] T020 [US2] Tạo `MIG/V005_03__cong_viec.sql`: `cong_viec` theo data-model mục 6 (`loai_can_cu ENUM('MUC_KE_HOACH','LAN_DO','BUA','BUOI_HOAT_DONG','THEO_DOI_SAU_NGA','THEO_DOI_TIEP_XUC','CANH_BAO_UONG_NUOC','KIEM_TRA_DAU_CA','GIAM_SAT','PHAT_SINH','GHI_NHAN_PHAT_SINH','LAM_LAI')`; `vi_tri_loai ENUM('PHONG','GIUONG','TANG','KHU_VUC') NULL`; `trang_thai ENUM('CHUA_DEN_HAN','DEN_HAN','QUA_HAN','HOAN_THANH','HOAN_THANH_TRE','KHONG_THUC_HIEN','HUY')`; `dau_ton ENUM('TON_TU_CA_TRUOC','TON_CHO_XAC_NHAN') NULL`; cột sinh `khoa_trung`, `khoa_chuyen_mon_ca` có `UNIQUE`; `CHECK (nct_id IS NOT NULL OR vi_tri_id IS NOT NULL)`, `dau_khung <= thoi_diem_du_kien <= cuoi_khung`; năm chỉ mục ở data-model; trigger `trg_cong_viec_trang_thai_cuoi`) và `lich_su_xu_ly_cong_viec` (nhóm 3 chỉ thêm)
- [ ] T021 [P] [US2] Tạo entity `BE/m04/congviec/internal/CongViec.java`, `LichSuXuLyCongViec.java`
- [ ] T022 [US2] Cài `BE/m04/congviec/internal/KiemTraLapCongViec.java`: một hàm kiểm từng dòng (người có mặt/đang ở, căn cứ hợp lệ qua `NguonCongViecPort` hoặc mục kế hoạch Hiệu lực tại thời điểm, ca chứa thời điểm, vai trò, người thực hiện có tên trong ca và đủ chứng chỉ qua `NhanSuCaPort`, trùng khung phiên bản cũ `TRUNG_KHUNG_PHIEN_BAN_CU`, `LAN_DO` theo chỉ định buộc Bắt buộc) (research C-07)
- [ ] T023 [US2] Cài `BE/m04/congviec/internal/LapCongViecService.java`: lập theo lô (Người phụ trách ca hoặc Trưởng tầng; Trưởng tầng lập trước cho ca đã công bố), công việc phát sinh, làm lại (`KetQuaKiemTraPort.khongDat`, BR-M04-23), kiểm tra đầu ca/giám sát; trạng thái lúc lập theo đồng hồ (BR-M04-26); nhật ký; không có endpoint gợi ý (Q-307)
- [ ] T024 [US2] Đăng ký việc theo phút: chuyển `CHUA_DEN_HAN → DEN_HAN` ở `dau_khung`; nhắc `CaChuaCoCongViec` tại giờ bắt đầu ca (`lan_chay_m04` `CA_CHUA_CO_CONG_VIEC`) (FR-017)
- [ ] T025 [US2] Tạo `BE/m04/congviec/web/CongViecController.java`: `GET /ca/{caId}/du-lieu-nguon` (dữ liệu nguồn để người lập tự nhập, không chọn sẵn), `GET /ca/{caId}/cong-viec`, `POST /ca/{caId}/cong-viec/lap`, `POST /cong-viec/phat-sinh`, `POST /cong-viec/{id}/lam-lai`, `GET /cong-viec/{id}`, `POST /cong-viec/{id}/huy`; cài `NguonCongViecApi` (MANDATORY)
- [ ] T026 [P] [US2] Tạo màn web `frontend/src/tinh-nang/lap-cong-viec-ca/` (lỗi theo dòng, không có nút chép sẵn) với test Vitest, và app `mobile/lib/nhan-vien/lap-cong-viec-ca/`

**Checkpoint**: Lập công việc chạy độc lập.

---

## Phase 5: User Story 3 – Người bán trú chỉ có công việc trong thời gian có mặt (Priority: P1)

**Goal**: Trạng thái có mặt theo ngày, điểm danh đến/về qua bản ghi đón, Vắng không báo theo CFG-M02-07, giờ về dự kiến, báo còn có mặt hết ngày (FR-012 → FR-016, FR-018, FR-018a, FR-019, BR-M04-02).

**Independent Test**: E có lịch 07:30–16:30; lập trước điểm danh bị chặn; điểm danh đến 08:10 → Người phụ trách ca được báo, lập được từ 08:10; điểm danh về 15:00 → công việc sau đó bị hủy.

### Tests cho User Story 3

- [ ] T027 [P] [US3] Test `BT/m04/BR_M04_02_BanTruIT.java`, `FR_013_BangCoMatIT.java`, `FR_018a_GioVeIT.java`, `FR_019_HetNgayConCoMatIT.java`: mọi chuyển của bảng FR-013; điểm danh về hủy công việc Chưa đến hạn sau giờ về; về qua bản ghi đón hợp lệ (`BanGhiDonBanTruPort.daDung`); giờ về dời theo đăng ký của 014; báo một lần còn Có mặt hết ngày (Q15 → Q18)

### Implementation cho User Story 3

- [ ] T028 [US3] Tạo `MIG/V005_04__co_mat_ban_tru.sql`: `co_mat_ban_tru` (`UNIQUE (nct_id, ngay)`; `trang_thai ENUM('CHUA_DEN','CO_MAT','DA_VE','VANG_CO_BAO','VANG_KHONG_BAO','DA_HUY_NGAY_NGHI')`; `buoi_phat_sinh`; `gio_ve_du_kien_hien_hanh`; chỉ mục `(trang_thai, ngay, gio_den_du_kien)`, `(ngay, trang_thai)`) và `diem_danh_ban_tru` (nhóm 3; `loai` `DEN|VE`; `loai_ve` `QUA_DON|TU_VE|VIEN_DUA_VE`) (data-model mục 5)
- [ ] T029 [US3] Cài `BE/m04/comat/internal/CoMatService.java`: điểm danh đến (phát `DaDiemDanhDen`, `CanLapCongViec`), điểm danh về (hủy công việc sau giờ về, `daDung` bản ghi đón), listener `BaoVangBanTruDaGhi`, `NgayNghiBanTruDaDoi`, `PhuLucDaApDung` (`LICH_DEN`, `LOAI_LUU_TRU`); `CoMatBanTruApi` (gồm `diemDanhVe` cho 012), `TrangThaiCoMatBanTruPort` (004); `DinhChinhDuocProvider` `M04.DIEM_DANH_BAN_TRU`; phát `TrangThaiCoMatDaXacDinh`/`DaDoi` (research C-06)
- [ ] T030 [US3] Đăng ký việc hằng ngày `CO_MAT_DAU_NGAY` (lịch đến của 004 → `CHUA_DEN`/`VANG_CO_BAO`), `BAN_TRU_HET_NGAY` và việc theo phút Vắng không báo (CFG-M02-07, phát `VangKhongBao`)
- [ ] T031 [US3] Tạo `BE/m04/comat/web/CoMatController.java`: `GET /co-mat-ban-tru`, `POST /co-mat-ban-tru/diem-danh-den`, `POST …/{id}/diem-danh-ve`, `POST /diem-danh-ban-tru/{id}/dinh-chinh`; màn web `frontend/src/tinh-nang/diem-danh-ban-tru/` và app `mobile/lib/nhan-vien/diem-danh-ban-tru/`

**Checkpoint**: Bán trú chạy độc lập.

---

## Phase 6: User Story 4 – Checklist theo ca và ghi nhận kết quả theo loại (Priority: P1)

**Goal**: Checklist theo ca và phân công, công việc chung, nhận việc; ghi nhận theo loại kết quả, đo chỉ số qua `ChiSoPort`, xác nhận phục vụ bữa, Ghi nhận phát sinh, ghi muộn, ngoại tuyến, sự kiện tính phí (FR-029 → FR-037, FR-040a, FR-041, FR-051, BR-M04-10, 12, 27, BR-M15-20).

**Independent Test**: Nhân viên chăm sóc phân công 5 người ca ngày → checklist đúng 5 người và công việc chung của tầng; ghi uống nước 200 ml và một công việc khác.

### Tests cho User Story 4

- [ ] T032 [P] [US4] Test đơn vị `BT/m04/XacDinhTrangThaiDongTest.java` (Hoàn thành / Hoàn thành trễ theo thời điểm thực hiện so với khung)
- [ ] T033 [P] [US4] Test `BT/m04/BR_M04_12_QuyenGhiNhanIT.java`, `BR_M04_10_DoChiSoIT.java`, `FR_034_KetQuaTheoLoaiIT.java`, `FR_040a_XacNhanPhucVuIT.java`, `FR_025_GhiNhanPhatSinhIT.java`, `FR_036_GhiDongThoiIT.java`, `FR_051_SuKienTinhPhiIT.java`: quyền ghi (được giao, Trưởng tầng, Người phụ trách ca); `ChiSoPort` `BI_CHAN`/`CHO_DO_LAI`; vượt giới hạn trên cần xác nhận; hai người ghi đồng thời chỉ một thành công (`UNIQUE (cong_viec_id)`); Không thực hiện thiếu `loaiLyDoId` hoặc loại đã ngừng → 422, có ghi chú thì lưu (`FR_028a_LoaiLyDoKhongThucHienIT`, phần lệnh; Q24, Q42); kết quả có tính phí phát `KetQuaCoTinhPhi` (Q19 → Q24, Q29)
- [ ] T034 [P] [US4] Test `BT/m04/BR_M04_12_GhiMuonNgoaiTuyenIT.java`, `BR_M04_27_DongBoCongViecDaDongIT.java`, `FR_035_TrangThaiCuoiNgoaiTuyenIT.java`: đồng bộ ngoại tuyến ghi nhãn ghi muộn, `thoi_diem_thiet_bi`; công việc đã đóng khi đồng bộ → bản ghi bị từ chối kèm hướng xử lý (Q28)

### Implementation cho User Story 4

- [ ] T035 [US4] Tạo `MIG/V005_05__ket_qua_ghi_nhan.sql`: `ket_qua_ghi_nhan` (nhóm 3; `UNIQUE (cong_viec_id)`; `gia_tri_so DECIMAL(10,2) NULL`, `gia_tri_ma VARCHAR(40) NULL`, `gia_tri_json JSON NULL`; `nhan_ghi_muon`, `nhan_dong_bo_sau_mat_quyen`; `can_cu_quyen ENUM('DUOC_GIAO','VAI_TRO_TRUONG_TANG','NHIEM_VU_PHU_TRACH_CA')`; `CHECK (thoi_diem_thuc_hien <= COALESCE(thoi_diem_thiet_bi, thoi_diem))`; chỉ mục `(nct_id, loai_ket_qua, thoi_diem_thuc_hien)`; trigger nhóm 3)
- [ ] T036 [US4] Cài `BE/m04/ghinhan/internal/GhiNhanService.java`: ghi nhận hoàn thành/không thực hiện theo loại kết quả (FR-034; Không thực hiện lưu `loai_ly_do_id` bắt buộc và `ly_do` là ghi chú, `CHECK` ở data-model mục 6, FR-028a), đo chỉ số gọi đồng bộ `ChiSoPort.ghi`, xác nhận phục vụ (`NguonCongViecPort.xacNhanPhucVu`), Ghi nhận phát sinh (`HOAN_THANH` ngay), nhãn ghi muộn (BR-M04-12), yêu cầu đóng cảnh báo khi đóng công việc, sự kiện `KetQuaCoTinhPhi`, `KetQuaNguDaGhi` qua outbox cùng transaction (research C-09)
- [ ] T037 [US4] Cài hai `NgoaiTuyenHandler` `M04.KET_QUA_CONG_VIEC`, `M04.GHI_NHAN_PHAT_SINH` trong `BE/m04/ghinhan/internal/` (BR-M04-27; loại lý do Không thực hiện chấp nhận nếu còn hiệu lực tại `thoi_diem_thiet_bi`; research C-14)
- [ ] T038 [US4] Cài `BE/m04/checklist/internal/ChecklistService.java`: checklist theo ca (công việc của người, công việc chung tầng/khu, liều thuốc qua `LieuThuocChecklistPort`, cảnh báo mở qua `CanhBaoDocPort`, việc bàn giao qua `NhanSuCaPort.viecBanGiao`, lưu ý chăm sóc qua `DanhGiaApi`, thỏa thuận lựa chọn có rủi ro của phiên bản Hiệu lực đọc theo lô, danh mục loại lý do cho app; FR-030), nhận việc chung (người có tên trong ca) (research C-10; NFR-02 mở checklist 3 giây)
- [ ] T039 [US4] Tạo `BE/m04/ghinhan/web/GhiNhanController.java` (`POST /cong-viec/{id}/ghi-nhan`, `/khong-thuc-hien`, `POST /ghi-nhan-phat-sinh`, `GET /nguoi-cao-tuoi/{nctId}/ket-qua-ghi-nhan`) và `BE/m04/checklist/web/ChecklistController.java` (`GET /checklist`, `POST /cong-viec/{id}/nhan-viec`); cài `CongViecDoChiSoApi`, `KetQuaChamSocCongApi`
- [ ] T040 [P] [US4] Tạo app `mobile/lib/nhan-vien/checklist/` (tải, lưu ngoại tuyến, dấu "chưa cập nhật"), `ghi-nhan/` (theo loại kết quả, Không thực hiện chọn loại lý do từ danh mục đã lưu và ghi chú tùy chọn, Ghi nhận phát sinh, bản ghi bị từ chối; thẻ người cao tuổi hiện thỏa thuận), `dong-bo/` và test `mobile/test/nhan-vien/checklist_ngoai_tuyen_test.dart`, `ghi_nhan_theo_loai_test.dart`, `ket_qua_dong_bo_test.dart` (BR-M15-20), `ban_ghi_bi_tu_choi_test.dart`
- [ ] T041 [P] [US4] Tạo màn web `frontend/src/tinh-nang/checklist/` (công việc sắp Quá hạn lên đầu; thỏa thuận trên thẻ người cao tuổi; Không thực hiện chọn loại lý do) với test Vitest

**Checkpoint**: Checklist và ghi nhận chạy độc lập, kể cả ngoại tuyến.

---

## Phase 7: User Story 5 – Quá hạn được nhắc, leo thang theo mức và đưa vào bàn giao (Priority: P1)

**Goal**: Hết khung → Quá hạn; nhắc người thực hiện một lần; Quan trọng báo Trưởng tầng và Người phụ trách ca; Bắt buộc yêu cầu cảnh báo Trung bình; Thường quá CFG-M04-12 báo Người phụ trách ca; danh sách chờ bàn giao; tự đóng sau bàn giao; tồn chờ xác nhận (FR-043 → FR-047a, FR-052, BR-M04-05 → 07, 29, BR-M09-08).

**Independent Test**: 3 công việc (Thường, Quan trọng, Bắt buộc) đến hạn 10:00 khung ±15 phút; không ghi nhận → lúc 10:15 cả ba Quá hạn, nhắc, báo, cảnh báo đúng mức.

### Tests cho User Story 5

- [ ] T042 [P] [US5] Test `BT/m04/BR_M04_05_QuaHanNhacIT.java`, `BR_M04_06_LeoThangTheoMucIT.java`, `BR_M04_29_NhacMotLanIT.java` (đồng hồ giả lập): chuyển trong ≤ 1 phút (SC-005); nhắc một lần; leo thang đúng mức; chạy bù gộp nhắc (`NhacQuaHanGop`); không tự tạo công việc (Q25, Q26)
- [ ] T043 [P] [US5] Test `BT/m04/BR_M04_07_DanhSachChoBanGiaoIT.java`, `FR_047_TuDongDongSauBanGiaoIT.java`, `FR_047a_TonChoXacNhanIT.java`: danh sách cho 008; xác nhận bàn giao đóng công việc Thường Quá hạn có trong bàn giao ("hệ thống đóng sau bàn giao"), chuyển công việc khác sang ca sau với dấu `TON_TU_CA_TRUOC`; bàn giao chưa xác nhận lúc bắt đầu ca → `TON_CHO_XAC_NHAN` (Q27)

### Implementation cho User Story 5

- [ ] T044 [US5] Đăng ký việc theo phút ở `BE/m04/quahan/internal/QuaHanService.java`: `DEN_HAN → QUA_HAN` ở `cuoi_khung`; nhắc (`da_nhac_luc`); Quan trọng báo; Bắt buộc phát `YeuCauCanhBao` Trung bình; Thường quá CFG-M04-12 báo Người phụ trách ca; ghi `lich_su_xu_ly_cong_viec`; tồn chờ xác nhận tại giờ bắt đầu ca (research C-12)
- [ ] T045 [US5] Cài `CongViecBanGiaoApi` (danh sách công việc chưa đóng của ca cho bàn giao, BR-M09-06) và `CongViecCaApi.khiXacNhanBanGiao` trong `BE/m04/quahan/internal/BanGiaoCongViec.java`
- [ ] T046 [US5] Tạo `BE/m04/quahan/web/QuaHanController.java` (`GET /cong-viec/qua-han`) và màn web `frontend/src/tinh-nang/viec-qua-han/`, app `mobile/lib/nhan-vien/viec-qua-han/`

**Checkpoint**: P1 hoàn tất.

---

## Phase 8: User Story 6 – Quy tắc từ kết quả: uống nước thiếu, ăn kém kéo dài (Priority: P2)

**Goal**: Mốc kiểm tra nước, chuỗi bữa kém, ngày tâm trạng tiêu cực; chỉ tạo yêu cầu cảnh báo và báo, không tự sinh công việc (FR-039, FR-040, FR-042, BR-M04-08, 09, 11).

**Independent Test**: A mục tiêu 1.500 ml, tổng 800 ml trước 16:00 → cảnh báo nhẹ, không có công việc tự sinh; B 3 bữa liên tiếp kém → điều dưỡng được báo, 011 nhận `AnKemKeoDai`.

### Tests cho User Story 6

- [ ] T047 [P] [US6] Test đơn vị `BT/m04/BR_M04_08_NuocUongTest.java`, `BR_M04_09_ChuoiBuaKemTest.java`, `BR_M04_11_TamTrangTest.java` (Q30 → Q33)
- [ ] T048 [P] [US6] Test `BT/m04/NuocUongIT.java`, `ChuoiBuaKemIT.java`, `US7_6_HuyGhiNhanChuoiIT.java`: dùng giá trị sau đính chính; hủy ghi nhận làm đứt chuỗi; mốc nước mỗi ngày một lần (`lan_chay_m04` `NUOC_UONG`)

### Implementation cho User Story 6

- [ ] T049 [US6] Cài `BE/m04/quytac/internal/DanhGiaNuocUong.java`, `DemChuoiBuaKem.java`, `DemNgayTamTrang.java` (đọc qua `DinhChinhApi.hienHanh`; CFG-M04 đọc lúc chạy) và nối: mốc nước vào việc theo phút; chuỗi bữa và tâm trạng gọi sau khi ghi nhận/đính chính; phát `YeuCauCanhBao`, `AnKemKeoDai` (research C-11)

**Checkpoint**: Quy tắc mục F chạy độc lập.

---

## Phase 9: User Story 7 – Đính chính kết quả đã hoàn thành (Priority: P2)

**Goal**: Kết quả nhóm 3 chỉ đính chính; tổng nước, chuỗi bữa, phí dùng giá trị đính chính (FR-037, BR-M04-14, DBR-17 phần 005).

**Independent Test**: Ghi 2.000 ml (nhầm), thử sửa trực tiếp bị chặn; đính chính 200 ml có lý do; tổng nước dùng giá trị đính chính, bản gốc vẫn xem được.

### Tests cho User Story 7

- [ ] T050 [P] [US7] Test `BT/m04/BR_M04_14_DinhChinhIT.java`, `US7_4_DinhChinhCoTinhPhiIT.java`, `CongViecTrangThaiCuoiTriggerIT.java`: `UPDATE` bị trigger chặn; đính chính cách A/B (tầng của công việc); đo chỉ số không đính chính ở 005 (`choPhep` sai); đính chính có tính phí phát `KetQuaTinhPhiDaDinhChinh` (Q34)

### Implementation cho User Story 7

- [ ] T051 [US7] Cài `DinhChinhDuocProvider` `M04.KET_QUA_GHI_NHAN` trong `BE/m04/ghinhan/internal/KetQuaDinhChinhProvider.java` (`tangKhuVuc` = tầng/khu công việc; `sauKhiDinhChinh` phát sự kiện cho 010 và chạy lại quy tắc F) và endpoint `POST /ket-qua-ghi-nhan/{id}/dinh-chinh` trong `GhiNhanController.java`

**Checkpoint**: Đính chính chạy độc lập.

---

## Phase 10: User Story 8 – Điều dưỡng được báo xem xét kế hoạch (Priority: P2)

**Goal**: Bốn nguồn (đánh giá lại đổi mức/nhu cầu, sự cố ngã, phụ lục đổi mức, chọn chăm sóc giảm nhẹ) → `CanXemXetKeHoach`; không tạo bản ghi yêu cầu (FR-011, BR-M04-20, Q-306).

**Independent Test**: Xác nhận đánh giá lại khác mức cho A → điều dưỡng phụ trách A nhận thông báo, không có bản ghi yêu cầu nào.

### Tests cho User Story 8

- [ ] T052 [P] [US8] Test `BT/m04/BR_M04_20_BaoXemXetKeHoachIT.java`: bốn nguồn sự kiện, người nhận theo `NhanSuCaPort.dieuDuongPhuTrach`; không có `INSERT` bản ghi nghiệp vụ ngoài nhật ký (Q35)

### Implementation cho User Story 8

- [ ] T053 [US8] Cài listener `BE/m04/kehoach/internal/BaoXemXetKeHoach.java` cho `DanhGiaDaDoiMucHoacNhuCau` (001), `PhuLucDaApDung` (004), `SuCoNgaDaGhiNhan`, `LuaChonChamSocGiamNheDaGhi`, `LichDoDaDoi` (007) phát `CanXemXetKeHoach`, `NguonCongViecDaDoi`

**Checkpoint**: Báo xem xét kế hoạch chạy.

---

## Phase 11: User Story 9 – Phân lại công việc chung và thời khóa biểu cá nhân (Priority: P3)

**Goal**: Vắng ca, nghỉ có duyệt, nghỉ việc, đổi/kết thúc phân công làm công việc thành chung; Trưởng tầng phân lại; thời khóa biểu chỉ xem; hủy tự động kèm lệnh nguồn (FR-024, FR-027, FR-028, FR-032, FR-048, BR-M04-04, 13, 28).

**Independent Test**: Đánh dấu S vắng ca → công việc của S thành chung; Trưởng tầng phân lại cho S2 → S2 thấy trong checklist.

### Tests cho User Story 9

- [ ] T054 [P] [US9] Test `BT/m04/BR_M04_13_CongViecChungIT.java`, `BR_M04_28_GanLaiIT.java`: `khiVangCa`, `khiNghiCoDuyet`, `khiNghiViec`, `khiDoiPhanCong`, `khiKetThucPhanCong` trong transaction của 008/015; ca không ai đủ điều kiện phát `CongViecChungKhongAiDuDieuKien`; `DaChuyenGiuong` gán lại theo khu mới (Q36, Q37)
- [ ] T055 [P] [US9] Test `BT/m04/BR_M04_04_HuyKhiVangIT.java`, `FR_010_TrangThaiCuoiIT.java`, `FR_022_NguonBiHuyIT.java`, `FR_020a_DinhChinhChuyenDiIT.java`: bean 001 hủy công việc khi vắng, đóng khi trạng thái cuối; nguồn bị hủy (007, 011, 014) hủy công việc; đính chính chuyến đi tính lại (Q11 → Q14)

### Implementation cho User Story 9

- [ ] T056 [US9] Cài `CongViecCaApi` (`khiVangCa`, `khiNghiCoDuyet`, `khiNghiViec`, `khiDoiPhanCong`, `khiKetThucPhanCong`) và phân lại (Trưởng tầng; kiểm `NhanSuCaPort.duDieuKien`) trong `BE/m04/checklist/internal/CongViecChungService.java`; endpoint `POST /cong-viec/{id}/phan-lai` trong `ChecklistController.java`; listener `DaChuyenGiuong` (research C-10)
- [ ] T057 [US9] Cài bean `ThamGiaChuyenTrangThai` của 005 trong `BE/m04/trangthai/internal/ThamGiaM04.java` (`@Order` sau 004, 003; tác động theo bảng contracts mục 1.1), `NguonCongViecApi.nguonBiHuy`, `CongViecVangMatApi.tinhLai` (research C-08)
- [ ] T058 [US9] Cài `BE/m04/thoikhoabieu/internal/ThoiKhoaBieuService.java` (gộp công việc và `ThoiKhoaBieuNguonPort` của 006, 012, 014; chỉ xem) và endpoint `GET /nguoi-cao-tuoi/{nctId}/thoi-khoa-bieu`; màn web `frontend/src/tinh-nang/thoi-khoa-bieu/` (FR-048)

**Checkpoint**: Mọi user story của 005 chạy độc lập.

---

## Phase 12: Polish & Cross-Cutting

- [ ] T059 [P] Cài `CongViecThongKeApi` (016) trong `BE/m04/congviec/internal/`
- [ ] T060 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/005-daily-care-tasks/contracts/api-005.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 005
- [ ] T061 Khi 006, 007, 008, 011, 012, 014 có bản thật, chạy lại test US2, US4, US5, US9 với bean thật thay T002
- [ ] T062 Đo NFR-02 (mở checklist 3 giây) và khối lượng 7.500 công việc/ngày với dữ liệu sinh; chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3
- [ ] T063 Rà truy vết data-model mục 12: DBR-11, 12, 15, 17, 23, 25 và BR-M04-01 → 14, 19, 20, 24 → 29, BR-M09-01, 08 (phần 005), BR-M15-20 (phần 005), FR-002a, FR-028a (bổ sung 2026-10-18) có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-009, FR-010, FR-049, FR-050, FR-053 (bổ sung 2026-10-10) — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story; Foundational của 005 chặn 006, 007, 008, 011, 012, 014 (cần `m04.api` và SPI để cài)
- **US1 (P1)**: sau Foundational
- **US2 (P1)**: sau US1 (căn cứ mục kế hoạch)
- **US3 (P1)**: sau Foundational; tích hợp với US2 (chặn lập khi chưa có mặt)
- **US4 (P1)**: sau US2
- **US5 (P1)**: sau US4
- **US6 (P2)**: sau US4
- **US7 (P2)**: sau US4
- **US8 (P2)**: sau US1
- **US9 (P3)**: sau US4

### Parallel Opportunities

- T003, T004, T008, T009 song song
- US3 song song với US1, US2; US6, US7, US8 song song sau US4
- Web và app song song khi controller đã có

## Parallel Example: User Story 4

```text
Task: "T032 XacDinhTrangThaiDongTest"
Task: "T033 BR_M04_12/BR_M04_10/FR_034/FR_040a/FR_025/FR_036/FR_051 IT"
Task: "T034 ngoại tuyến IT"
# sau T035–T039:
Task: "T040 app checklist, ghi nhận, đồng bộ"
Task: "T041 web checklist"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2 → US4
2. **Dừng và kiểm chứng**: kế hoạch có hiệu lực → lập công việc ca → checklist → ghi nhận, kể cả ngoại tuyến
3. Thêm US5 để có nhắc và leo thang

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6 → US7 → US8 → US9; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- 005 không sinh, không gợi ý công việc (Q-306, Q-307); job chỉ chuyển trạng thái theo giờ, nhắc, báo, yêu cầu cảnh báo
- Mọi mốc đọc `ThamSoApi` (CFG-M04-*, CFG-M02-07, CFG-M15-05, 06)
- Ngoại tuyến chỉ cho ghi nhận thực hiện (Q-01)
