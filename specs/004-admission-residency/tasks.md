---
description: "Danh sách task triển khai feature 004 – Tiếp nhận và lưu trú"
---

# Tasks: Tiếp nhận và lưu trú (004)

**Input**: Tài liệu thiết kế ở `specs/004-admission-residency/` (plan.md, spec.md, research.md T-01 → T-16, data-model.md, contracts/api-004.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 001 có `m01.api` (SPI `ThamGiaChuyenTrangThai`, `YeuCauDoiMucApi`, `HopDongApi`, `LuotVangApi`); 003 có `PhanBoGiuongLenhApi`, `SucChuaBanTruApi`, SPI `NguonLuuTruChoPhanBo`, `DemLichDenBanTru`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 15.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

**Cập nhật 2026-10-22** (plan mục "Cập nhật 2026-10-22"; Q-379, Q-385): sửa câu chữ T039 (điều chỉnh có duyệt thay giao dịch đảo); thêm T106 → T108; Q-387 (chốt sau plan) thêm T109. Không đánh số lại.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M02: `BE/m02/{api,dangky,danhsachcho,hopdong,dichvu,thaydoi,vang,ketthuc,quadoi,tinhphi,trangthai,lich}/`
- Web `frontend/src/tinh-nang/{dang-ky-tiep-nhan,danh-sach-cho,hop-dong,dich-vu-don-gia,yeu-cau-thay-doi,vang-mat,ket-thuc-luu-tru,qua-doi}/`; app nhân viên `mobile/lib/nhan-vien/vang-mat/`; app người thân `mobile/lib/nguoi-than/{hop-dong,yeu-cau-thay-doi,luu-tru}/`

---

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; Q-356, 009 FR-001a): sửa T011, T013, T015 → T019; thêm T016a. Mã task cũ giữ nguyên. Phụ thuộc mới: `NguonYKienApi` (001 T012a).

## Phase 1: Setup

- [x] T001 Tạo khung gói `BE/m02/{api,dangky,danhsachcho,hopdong,dichvu,thaydoi,vang,ketthuc,quadoi,tinhphi,trangthai,lich}/` với `package-info.java` (research T-01)
- [x] T002 [P] Tạo bản giả SPI 004 khai báo (contracts mục 2.1) trong `BT/support/gia004/`: `NguoiDaiDienPort`, `BanGhiDonPort` (012), `TrangThaiDatCocPort` (017), `KyCuoiPort` (010), `TrangThaiCoMatBanTruPort` (005), `MucViecSauQuaDoiPort` (010, 013, 006, 007, 017)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `m02.api`, SPI, sự kiện, bảng khóa theo người, dịch vụ và đơn giá (hợp đồng cần), job hằng ngày khung, bean trạng thái khung.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T003 [P] Tạo giao diện công khai `BE/m02/api/`: `HopDongDocApi`, `DonGiaApi`, `NgayLuuTruApi`, `DieuKienTiepNhanApi`, `LuuTruThongKeApi` (016), `DanhSachViecSauQuaDoiApi` (contracts mục 1.2 → 1.6) và SPI `NguoiDaiDienPort`, `BanGhiDonPort` (có `daDung` MANDATORY), `TrangThaiDatCocPort`, `KyCuoiPort` (mọi hàm MANDATORY), `TrangThaiCoMatBanTruPort`, `MucViecSauQuaDoiPort` (contracts mục 2.1)
- [x] T004 [P] Tạo sự kiện `BE/m02/api/sukien/` theo contracts mục 1.8 (gồm `TiepNhanHoanTat`, `HopDongDaDoiTrangThai`, `GiuongTrongCanChonHoSo`, `NgayLuuTruDaDoi`, `LoaiLuuTruDaDoi`, sự kiện nhắc, báo); sự kiện gửi 009 cài `CoYeuCauThongBao` (research T-12)
- [x] T005 Tạo `MIG/V004_01__luu_tru_nct.sql`: `luu_tru_nct` (`nct_id` khóa chính, `da_dong_luc DATETIME(6) NULL` đặt một lần, `phien_ban`) và bảng kỹ thuật `lan_chay_theo_ngay (viec, ngay)` khóa chính hai cột, `nhac_m02 (loai, doi_tuong_id)` khóa chính (data-model mục 2, 11)
- [x] T006 Cài `BE/m02/trangthai/internal/KhoaLuuTru.java`: khóa `luu_tru_nct` `FOR UPDATE` cho mọi lệnh 004 trên cùng người; lệnh dẫn tới Hủy tiếp nhận đi qua 001 để chỉ có một thứ tự khóa (research T-03)
- [x] T007 Tạo `MIG/V004_02__dich_vu.sql`: `dich_vu` (`loai ENUM('CHAM_SOC','PHI_LUU_TRU','BUOI_BAN_TRU','PHUC_HOI','HOAT_DONG','DUA_DI_KHAM','TIEM','NGUOI_THAN_O_LAI','SUAT_AN','THUOC_NGUON_VIEN','KHAC')`; `ky_tinh ENUM('THANG','NGAY','BUOI','LAN','DON_VI')`; `CHECK ((loai='CHAM_SOC') = (ma_muc IS NOT NULL))`; cột sinh `muc_hieu_luc` `UNIQUE`; `ten_hieu_luc UNIQUE`) và `phien_ban_don_gia` (`don_gia DECIMAL(15,0) >= 0`; `CHECK (hieu_luc_den IS NULL OR hieu_luc_den >= hieu_luc_tu)`; `da_tham_chieu`; trigger `trg_phien_ban_don_gia_khong_chong`, `trg_phien_ban_don_gia_khong_sua`) (data-model mục 6)
- [x] T008 Cài `BE/m02/dichvu/internal/DonGiaService.java` (`DonGiaApi`: đơn giá tại ngày, đánh dấu `da_tham_chieu`) (research T-05)
- [x] T009 Cài khung bean `BE/m02/trangthai/internal/ThamGiaM02.java` (`@Order(1)`, `lenhQuanTam` gồm 7 lệnh; `kiemTraChiTiet` cho các kiểu `chiTiet` ở contracts mục 1.1) và khung job `BE/m02/lich/ViecHangNgayM02.java` (00:15 và khi khởi động, ShedLock; chạy bù từng ngày D theo `lan_chay_theo_ngay`; năm việc `TINH_DIEM`, `NGAY_VANG`, `HOP_DONG`, `KET_THUC`, `NHAC` đăng ký dần ở các story) (research T-11; FR-077, BR-M02-19)
- [ ] T010 [P] Cài SPI M02 cài cho module thấp hơn trong `BE/m02/trangthai/internal/`: `HopDongApi`, `LuotVangApi`, `YeuCauDoiMucApi` (001); `KetThucLuuTruApi` (002); `NguonLuuTruChoPhanBo`, `DemLichDenBanTru` (003) – khung trả dữ liệu từ bảng của các story, hoàn thiện ở story tương ứng (contracts mục 1.1)
- [ ] T011 [P] Cài provider cho 000/002 trong `BE/m02/trangthai/internal/`: `PhanLoaiTruongProvider` (`TAI_CHINH` cho giá, cọc, dòng ghi đè; `SUC_KHOE` cho `nguyenNhan` qua đời; `y_kien_vao_o` là `CONG_KHAI_NOI_BO`), `LuuTruProvider` theo data-model mục 14, `GanPhamViProvider`, `QuyenTepProvider` (`M02.BAN_SCAN_HOP_DONG`, `M02.BANG_CHUNG_QUA_DOI`)
- [x] T012 [P] Test `BT/m02/BR_M02_19_ChayBuIT.java` (đồng hồ giả lập): tắt job 3 ngày rồi chạy → mỗi việc mỗi ngày chạy đúng một lần; chạy lại không trùng

**Checkpoint**: Nền 004 sẵn sàng.

---

## Phase 3: User Story 1 – Đăng ký tiếp nhận và danh sách điều kiện tiếp nhận (Priority: P1) 🎯 MVP

**Goal**: Lượt đăng ký, tiến trình, danh sách điều kiện tiếp nhận khớp lệnh Hoàn tất tiếp nhận của 001 (FR-001 → FR-006, FR-076, BR-M02-15, BR-M02-22).

**Independent Test**: 3 lượt đăng ký (đủ điều kiện; thiếu cọc; nội trú chưa có giường); kiểm danh sách điều kiện và kết quả Hoàn tất tiếp nhận.

### Tests cho User Story 1

- [x] T013 [P] [US1] Test `BT/m02/LuotDangKyIT.java`, `CapNhatLuotDangKyIT.java`, `TienTrinhIT.java`: một lượt mở mỗi người (`UNIQUE nct_dang_mo`); cập nhật có `If-Match` và lý do; `ngay_dang_ky` không đổi khi đã có hồ sơ chờ (BR-M02-15); bổ sung 2026-10-18 `FR_004a_YKienVaoOIT.java`: ghi ý kiến (thiếu nguồn, nguồn người thân không có quan hệ Hiệu lực → 422), ý kiến mới thay thế, cũ vẫn tra được, `UPDATE` trực tiếp bị trigger chặn; Hoàn tất tiếp nhận khi chưa có ý kiến → 200 kèm cảnh báo `CHUA_GHI_Y_KIEN_VAO_O`; khi "Không đồng ý" → cảnh báo `NCT_KHONG_DONG_Y_VAO_O`, lý do lưu ở lịch sử trạng thái của 001, `y_kien_luc_tiep_nhan_id` được đặt (Q1a → Q1c)
- [x] T014 [P] [US1] Test `BT/m02/FR_004_DanhSachKhopLenhIT.java`, `BR_M02_22_MucHopDongIT.java`, `FR_076_CanhBaoChuaVaoOIT.java` (cảnh báo hằng ngày "phí đang tính cho ngày chưa vào ở" khi nội trú chưa Hoàn tất tiếp nhận sau ngày bắt đầu hợp đồng; dừng khi Hoàn tất tiếp nhận, Hủy tiếp nhận hoặc phụ lục đổi ngày bắt đầu; không báo Quản lý viện): danh sách điều kiện bằng `xemDieuKien` của 001 khớp kết quả lệnh, gồm (f) DBR-02 do bean 012 giả trả Chưa đạt khi thiếu người đại diện (Q-343); mức hợp đồng khác mức hiện hành chặn Hoàn tất tiếp nhận (FR-004, FR-025b)

### Implementation cho User Story 1

- [x] T015 [US1] Tạo `MIG/V004_03__luot_dang_ky.sql`: `luot_dang_ky` (`loai_luu_tru_mong_muon ENUM('NOI_TRU_DAI_HAN','NOI_TRU_NGAN_NGAY','BAN_TRU')`, `muc_du_kien`, `trang_thai ENUM('DANG_XU_LY','DANG_CHO','DA_TIEP_NHAN','DA_HUY')`, cột sinh `nct_dang_mo` `UNIQUE`; `y_kien_luc_tiep_nhan_id BIGINT NULL`) và `y_kien_vao_o` (bổ sung 2026-10-18: `y_kien ENUM('DONG_Y','KHONG_DONG_Y','KHONG_BAY_TO_DUOC') NOT NULL`; mẫu cột nguồn 001 data-model mục 6a; `ghi_chu VARCHAR(2000) NULL`; `trang_thai ENUM('HIEU_LUC','DUOC_THAY_THE')`; cột sinh `luot_hieu_luc` `UNIQUE`; trigger chỉ cho `HIEU_LUC → DUOC_THAY_THE`, không xóa) (data-model mục 3)
- [x] T016 [US1] Cài `BE/m02/dangky/internal/LuotDangKyService.java` (lập khi người Đang tiếp nhận, cập nhật, tiến trình) và `DieuKienTiepNhanService.java` (`DieuKienTiepNhanApi` qua `TrangThaiNguoiCaoTuoiApi.xemDieuKien`, đủ (a) → (f), (f) lấy điều kiện `DBR_02` của 012; cảnh báo FR-076 trên danh sách và nhắc hằng ngày qua việc `NHAC` của `ViecHangNgayM02.java`) (research T-09)
- [x] T016a [US1] Cài `BE/m02/dangky/internal/YKienVaoOService.java`: ghi ý kiến dưới khóa `luu_tru_nct`, kiểm `NguonYKienApi.kiemTra`, thay thế dòng Hiệu lực cũ; đưa ý kiến hiện hành vào `DanhSachDieuKien.yKienVaoO` (FR-001, FR-004a; research T-09a)
- [x] T017 [US1] Cài trong `ThamGiaM02.java` phần `HOAN_TAT_TIEP_NHAN` (điều kiện 004: hợp đồng Hiệu lực, không sớm hơn ngày bắt đầu, mức trùng, đặt cọc qua `TrangThaiDatCocPort`; `canhBao`: `CHUA_GHI_Y_KIEN_VAO_O`, `NCT_KHONG_DONG_Y_VAO_O`, không chặn; tác động: lượt `DA_TIEP_NHAN`, đặt `y_kien_luc_tiep_nhan_id`, phát `TiepNhanHoanTat`) và `HUY_TIEP_NHAN` (lượt `DA_HUY`, hợp đồng Nháp/Chờ ký → `DA_HUY`, đóng hồ sơ chờ) (FR-002, FR-004)
- [x] T018 [US1] Tạo `BE/m02/dangky/web/LuotDangKyController.java`: `POST/GET /luot-dang-ky`, `GET /luot-dang-ky/{id}`, `POST …/{id}/cap-nhat`, `POST …/{id}/y-kien-vao-o` (bổ sung 2026-10-18), `GET …/{id}/dieu-kien-tiep-nhan`, `…/tien-trinh`
- [ ] T019 [P] [US1] Tạo màn web `frontend/src/tinh-nang/dang-ky-tiep-nhan/` (gồm khối ý kiến của người cao tuổi về việc vào ở: ghi, lịch sử, nguồn; danh sách điều kiện hiện ý kiến hiện hành tách khỏi các điều kiện) và test `DieuKienTiepNhan.test.jsx`; app người thân `mobile/lib/nguoi-than/luu-tru/` (tiến trình tiếp nhận)

**Checkpoint**: Đăng ký và điều kiện tiếp nhận chạy được.

---

## Phase 4: User Story 2 – Danh sách chờ với điểm ưu tiên tự tính (Priority: P1)

**Goal**: Hồ sơ chờ, điểm theo bảng CFG-M02-08, tính hằng ngày, điều chỉnh qua phê duyệt, danh sách phù hợp khi giường Trống, Hành chính tự chọn, giữ chỗ tạm (FR-007 → FR-018, BR-M02-01, 02, 03, 10, 20).

**Independent Test**: 6 hồ sơ chờ khác mức, ngày, tình huống, giới tính; tua đồng hồ; giải phóng giường → thông báo, danh sách phù hợp, thứ tự.

### Tests cho User Story 2

- [x] T020 [P] [US2] Test đơn vị `BT/m02/BR_M02_10_DiemUuTienTest.java` và `DiemHangNgayIT.java`, `DieuChinhDiemIT.java`: điểm theo thành phần; `lich_su_diem` chỉ thêm khi điểm đổi; điều chỉnh qua yêu cầu `M02.DIEU_CHINH_DIEM` (FR-009 → FR-011)
- [ ] T021 [P] [US2] Test `BT/m02/BR_M02_01_DanhSachPhuHopIT.java`, `FR_012_LuotTrongIT.java`, `BR_M02_02_GiuChoIT.java`, `GhiNhanLienHeDongThoiIT.java`: danh sách phù hợp không chọn sẵn; hồ sơ đã liên hệ/từ chối giường bị loại khỏi lượt trống hiện tại; bỏ qua thứ tự phải có lý do; giữ chỗ CFG-M02-02, hết hạn về `DANG_CHO`; hai Hành chính liên hệ cùng giường chỉ một thành công
- [x] T022 [P] [US2] Test `BT/m02/FR_002_TuChoiHuyTiepNhanIT.java`, `BR_M02_03_NhacHoSoChoIT.java`: Từ chối, Hủy chờ đi qua `HUY_TIEP_NHAN` của 001; nhắc cập nhật hồ sơ chờ theo CFG-M02-03

### Implementation cho User Story 2

- [x] T023 [US2] Tạo `MIG/V004_04__danh_sach_cho.sql`: `ho_so_cho` (`trang_thai ENUM('DANG_CHO','DA_LIEN_HE','DA_TIEP_NHAN','TU_CHOI','HUY_CHO')`, `diem_chi_tiet JSON`, cột sinh `nct_dang_mo` `UNIQUE`, `CHECK` `giuong_dang_giu_id NOT NULL` khi `DA_LIEN_HE`), `ho_so_cho_tinh_huong` (`trang_thai ENUM('XAC_NHAN','CHO_XAC_NHAN')`), `lich_su_diem` (nhóm 3; `nguyen_nhan ENUM('TINH_HANG_NGAY','DOI_MUC','DOI_TINH_HUONG','DIEU_CHINH','DOI_BANG_DIEM','TAO')`), `lien_he_giuong` (nhóm 3; `ket_qua ENUM('LIEN_HE','TU_CHOI_GIUONG','HET_HAN')`, `ly_do_khong_theo_thu_tu NULL`) (data-model mục 4)
- [x] T024 [US2] Cài `BE/m02/danhsachcho/internal/TinhDiemUuTien.java` (bảng CFG-M02-08 đọc lúc chạy) và việc `TINH_DIEM` của job hằng ngày; listener `MucChamSocDaDoi`, `ThamSoDaDoi(CFG-M02-08)` (research T-07)
- [ ] T025 [US2] Cài `BE/m02/danhsachcho/internal/HoSoChoService.java`: đưa vào danh sách, cập nhật tình huống, đề nghị điều chỉnh điểm (handler `M02.DIEU_CHINH_DIEM`), ghi nhận đã liên hệ (`giuChoHoSoCho` của 003), từ chối giường (`huyGiuCho`), từ chối/hủy chờ qua 001, xác nhận còn nhu cầu; listener `GiuongDaTrong` phát `GiuongTrongCanChonHoSo`; cài `hoSoChoHetHanGiuCho`, `daPhanBo` của `NguonLuuTruChoPhanBo`
- [ ] T026 [US2] Tạo `BE/m02/danhsachcho/web/HoSoChoController.java`: `/ho-so-cho`, `/{id}`, `/{id}/lich-su-diem`, `/{id}/cap-nhat-tinh-huong`, `/{id}/de-nghi-dieu-chinh-diem`, `GET /giuong/{giuongId}/ho-so-phu-hop`, `/{id}/ghi-nhan-da-lien-he`, `/tu-choi-giuong`, `/tu-choi`, `/huy-cho`, `/xac-nhan-con-nhu-cau`
- [ ] T027 [US2] Đăng ký phần nhắc hồ sơ chờ (CFG-M02-03, `HO_SO_CHO_CAP_NHAT`, `PHAN_BO_LAI`) vào việc `NHAC` của `ViecHangNgayM02.java`
- [ ] T028 [P] [US2] Tạo màn web `frontend/src/tinh-nang/danh-sach-cho/` và test `HoSoPhuHop.test.jsx` (không chọn sẵn, lý do khi bỏ qua thứ tự)

**Checkpoint**: Danh sách chờ chạy độc lập.

---

## Phase 5: User Story 3 – Hợp đồng bị khóa khi có hiệu lực, không chồng thời gian (Priority: P1)

**Goal**: Vòng đời hợp đồng, khóa nội dung ngoài Nháp, duyệt điều khoản khác chuẩn, phiên bản nội dung, nội dung tại ngày D, quá hạn hợp đồng (FR-019 → FR-029, DBR-02, DBR-06, BR-M02-05, 08, 09).

**Independent Test**: Lập Nháp, sửa, gửi ký, thử sửa, ghi nhận đã ký, thử sửa lại; hợp đồng thứ hai chồng thời gian; áp dụng hai phụ lục, hỏi nội dung tại ba ngày.

### Tests cho User Story 3

- [x] T029 [P] [US3] Test `BT/m02/HopDongVongDoiIT.java`, `HopDongKhoaNoiDungDbIT.java`, `DBR_06_HopDongKhongChongIT.java`: mọi chuyển của bảng hợp đồng; trigger chặn sửa nội dung ngoài Nháp (kể cả bảng con); trigger chặn hai Hiệu lực chồng khoảng
- [x] T030 [P] [US3] Test `BT/m02/FR_021_GhiNhanDaKyIT.java`, `XacDinhKhacChuanTest.java`, `DuyetDieuKhoanIT.java`: Ghi nhận đã ký cần người đại diện hiệu lực (DBR-02), ngày ký, bản scan; điều khoản khác chuẩn phải được duyệt cho đúng băm nội dung; sửa sau duyệt làm mất hiệu lực bản duyệt (FR-025a)
- [x] T031 [P] [US3] Test `BT/m02/BR_M02_09_NoiDungTaiNgayIT.java`, `BR_M02_05_NhacHopDongIT.java`, `FR_029_QuaHanHopDongIT.java`: nội dung tại D đúng phiên bản; nhắc hợp đồng sắp hết; dấu quá hạn đặt đúng ngày, báo Quản lý viện khi quá CFG-M02-10

### Implementation cho User Story 3

- [x] T032 [US3] Tạo `MIG/V004_05__hop_dong.sql`: `hop_dong` theo data-model mục 5 (`so_hop_dong VARCHAR(20) UNIQUE`; `ngay_ket_thuc` bắt buộc với `NOI_TRU_NGAN_NGAY` (`CHECK`); `ky_tinh_phi_luu_tru ENUM('THANG','NGAY','BUOI')`; `khoan_dat_coc DECIMAL(15,0) NULL`; `bam_noi_dung CHAR(64)`; `trang_thai ENUM('NHAP','CHO_KY','HIEU_LUC','KET_THUC','CHAM_DUT','DA_HUY')`; `CHECK (trang_thai <> 'CHAM_DUT' OR ly_do_cham_dut IS NOT NULL)`; trigger `trg_hop_dong_khoa_noi_dung`, `trg_hop_dong_khong_chong`), bảng con `hop_dong_dich_vu`, `hop_dong_chinh_sach_vang`, `hop_dong_lich_den` (`CHECK (gio_ve > gio_den)`) có trigger khóa khi cha ngoài Nháp, `phien_ban_noi_dung_hop_dong` (nhóm 3; `UNIQUE (hop_dong_id, so_thu_tu)`, `UNIQUE (hop_dong_id, hieu_luc_tu)`)
- [ ] T033 [US3] Cài `BE/m02/hopdong/internal/HopDongService.java`: lập (có người đại diện qua `NguoiDaiDienPort`), sửa Nháp (`If-Match`), gửi ký (đúng một dòng `CHAM_SOC` và mức trùng, FR-025), trả về nháp, ghi nhận đã ký (phiên bản 0, ghi `don_gia_ghi`, `da_tham_chieu`; bán trú kiểm sức chứa `SucChuaBanTruApi`; research T-10), hủy; khóa `luu_tru_nct`; nhật ký; phát `HopDongDaDoiTrangThai` (research T-04)
- [x] T034 [US3] Cài `BE/m02/hopdong/internal/XacDinhKhacChuan.java` và handler `M02.DUYET_DIEU_KHOAN_HOP_DONG` (`kiemTraDieuKienApDung`: hợp đồng còn Nháp, băm khớp) (FR-025a)
- [x] T035 [US3] Cài `HopDongDocApi` và `HopDongApi` (001) trên `phien_ban_noi_dung_hop_dong` (nội dung tại D: `hieu_luc_tu` lớn nhất `<= D`) trong `BE/m02/hopdong/internal/HopDongDocService.java`
- [ ] T036 [US3] Đăng ký việc `HOP_DONG` (dấu quá hạn, báo CFG-M02-10) và nhắc `HOP_DONG_SAP_HET`, `HOP_DONG_NGAN_NGAY`, `QUA_HAN_HOP_DONG` vào `ViecHangNgayM02.java` (FR-026, FR-029)
- [x] T037 [US3] Tạo `BE/m02/hopdong/web/HopDongController.java`: `/hop-dong`, `/{id}`, `/{id}/dieu-khoan-khac-chuan`, `/gui-duyet-dieu-khoan`, `/gui-ky`, `/tra-ve-nhap`, `/ghi-nhan-da-ky`, `/huy`, `GET /{id}/noi-dung?ngay=`, `GET /{id}/phu-luc`
- [ ] T038 [P] [US3] Tạo màn web `frontend/src/tinh-nang/hop-dong/` và test `HopDong.test.jsx` (`truongBiAn` tài chính); app người thân `mobile/lib/nguoi-than/hop-dong/` (người đại diện xem hợp đồng, phụ lục, đặt cọc)

**Checkpoint**: Hợp đồng chạy độc lập.

---

## Phase 6: User Story 4 – Đặt cọc là điều kiện tiếp nhận, trạng thái lấy từ 017 (Priority: P1)

**Goal**: Điều kiện đặt cọc đọc qua `TrangThaiDatCocPort`; đặt cọc sau khi đã lưu trú; sự kiện cho 017, 018 (FR-030 → FR-033b).

**Independent Test**: Hợp đồng có và không yêu cầu cọc; kiểm điều kiện trước và sau khi Kế toán thu cọc ở 017 (bản giả); đảo một lần thu ghi nhầm.

### Tests cho User Story 4

- [x] T039 [P] [US4] Test `BT/m02/DatCocDieuKienIT.java`, `FR_032_DatCocSauLuuTruIT.java`, `SuKien017018IT.java`: điều kiện cọc `CHUA_DAP_UNG`/`DA_DAP_UNG`/`KHONG_YEU_CAU` theo port; điều chỉnh giảm thu cọc có duyệt ở 017 (sửa 2026-10-22, Q-385: không còn giao dịch đảo) làm điều kiện quay lại chưa đạt; phát sự kiện cho 017 và 018 khi hợp đồng, tiếp nhận đổi

### Implementation cho User Story 4

- [ ] T040 [US4] Cài điều kiện đặt cọc trong `ThamGiaM02.java` và endpoint `GET /hop-dong/{id}/dat-coc` trong `HopDongController.java`; listener `TrangThaiDatCocDaDoi` của 017 làm mới màn điều kiện (FR-030 → FR-032) *(2026-10-26: điều kiện (c), endpoint và `DepositRecheckService` (FR-032) xong; còn `[ ]` vì listener chờ `m11.api.TrangThaiDatCocDaDoi` của 017/E, team-plan mục 8)*
- [x] T041 [US4] Phát sự kiện cho 017 (hợp đồng, khoản cọc) và 018 (tiếp nhận hoàn tất, hợp đồng; BR-M02-11 → 14 thuộc 018) trong `HopDongService.java` (FR-033a, FR-033b)

**Checkpoint**: P1 hoàn tất: đăng ký → chờ → hợp đồng → cọc → Hoàn tất tiếp nhận.

---

## Phase 7: User Story 5 – Thay đổi lưu trú qua yêu cầu có duyệt thành phụ lục (Priority: P2)

**Goal**: Yêu cầu `M02.THAY_DOI_LUU_TRU` dùng vòng đời 000; phụ lục và giữ giường trong transaction duyệt; áp dụng vào ngày hiệu lực; thay thế bởi đánh giá lại (FR-037 → FR-047, DBR-07, BR-M02-04, BR-M02-16).

**Independent Test**: Ba yêu cầu (đổi mức, gia hạn ngắn ngày, đổi giường khác giá) ngày hiệu lực tương lai; duyệt; tua tới ngày; kiểm phụ lục, nội dung hợp đồng, giường.

### Tests cho User Story 5

- [ ] T042 [P] [US5] Test `BT/m02/BR_M02_04_ApDungPhuLucIT.java`, `DBR_07_PhuLucIT.java`, `FR_041_KiemTraKhiDuyetIT.java`: duyệt tạo phụ lục `CHO_HIEU_LUC` và giữ giường (`datTruocChuyenTheoLich`); áp dụng tạo phiên bản nội dung mới, đổi mức qua `MucChamSocApi`, chuyển giường theo lịch; phụ lục không sớm hơn ngày bắt đầu hợp đồng
- [ ] T043 [P] [US5] Test `BT/m02/FR_046_TrungThuocTinhIT.java`, `ApDungKhongThanhIT.java`, `YeuCauNguoiDaiDienIT.java`, `DoiLichDenIT.java`: hai yêu cầu chờ hiệu lực cùng thuộc tính bị `UNIQUE` chặn khi duyệt; áp dụng không thành → phụ lục `KHONG_AP_DUNG`, hủy phân bổ theo lịch; người đại diện gửi yêu cầu ở Nháp qua app; đổi lịch đến bán trú (BR-M02-16); xác nhận đánh giá lại chuyển yêu cầu đổi mức sang `DUOC_THAY_THE`
- [ ] T075 [P] [US5] Test `BT/m02/LaThayDoiNhoTest.java` (bảng: chỉ `DICH_VU` thuộc gói, ảnh hưởng chi phí 0 → đúng; có dịch vụ ngoài gói, có khoản đổi giá, có loại khác → sai) và `BT/m02/FR_043_ThayDoiNhoIT.java`: Hành chính lập rồi tự duyệt thay đổi nhỏ có lý do, nhật ký `tuDuyet`; Hành chính duyệt yêu cầu đổi loại lưu trú, đổi giường cùng giá hoặc thêm dịch vụ ngoài gói → 403; Quản lý viện duyệt mọi loại; hợp đồng đổi giữa lúc gửi và lúc duyệt làm yêu cầu hết là thay đổi nhỏ → Hành chính bị chặn; Hành chính không có trong danh sách nhắc của yêu cầu không phải thay đổi nhỏ (spec 004 US5 #6, #9; FR-043; 000 T105)

### Implementation cho User Story 5

- [ ] T044 [US5] Tạo `MIG/V004_06__phu_luc.sql`: `phu_luc_hop_dong` (`yeu_cau_phe_duyet_id NOT NULL UNIQUE`; `loai_thay_doi SET('LOAI_LUU_TRU','MUC_CHAM_SOC','DICH_VU','THOI_GIAN','GIUONG','LICH_DEN')`; `trang_thai ENUM('CHO_HIEU_LUC','DA_AP_DUNG','KHONG_AP_DUNG')`; `CHECK` có `phien_ban_noi_dung_id` khi `DA_AP_DUNG`; trigger chặn sửa nội dung) và `yeu_cau_thay_doi_thuoc_tinh` (cột sinh `khoa = IF(cho_hieu_luc, CONCAT(nct_id,':',thuoc_tinh), NULL)` `UNIQUE`) (data-model mục 7)
- [ ] T045 [US5] Cài handler `BE/m02/thaydoi/internal/ThayDoiLuuTruHandler.java` cho `M02.THAY_DOI_LUU_TRU`: `kiemTraNoiDung`, `kiemTraKhiDuyet`, `khiDuyet` (tạo phụ lục, đặt `cho_hieu_luc`, giữ giường), `kiemTraDieuKienApDung`, `apDung` (phiên bản nội dung mới, `ngay_ket_thuc_hien_hanh`, mức qua 001, giường qua 003, phát `LoaiLuuTruDaDoi`), `chuyenMoRong` `DUOC_THAY_THE`, `gioiHanVaiTroDuyet` (thay đổi nhỏ, FR-043, Q-338; hàm thuần `LaThayDoiNho`), `thayDoiNho` tính lúc đọc, không lưu trong nội dung (research T-06)
- [ ] T046 [US5] Cài `YeuCauDoiMucApi.thayTheBoiDanhGia` (MANDATORY, qua `YeuCauPheDuyetApi.chuyenMoRong`) và `coYeuCauDoiMucChuaKetThuc`; listener `YeuCauApDungKhongThanh`, `YeuCauDaHuy` đặt phụ lục `KHONG_AP_DUNG` trong `BE/m02/thaydoi/internal/`
- [ ] T047 [US5] Tạo `BE/m02/thaydoi/web/YeuCauThayDoiController.java` (`/yeu-cau-thay-doi-luu-tru`, `/{id}`), màn web `frontend/src/tinh-nang/yeu-cau-thay-doi/` và app người thân `mobile/lib/nguoi-than/yeu-cau-thay-doi/`

**Checkpoint**: Phụ lục có ngày hiệu lực chạy độc lập.

---

## Phase 8: User Story 6 – Tạm vắng áp bảng chính sách phí và giữ giường (Priority: P2)

**Goal**: Lượt vắng, tra chính sách (hợp đồng → cơ sở → mặc định), ngày vắng hằng ngày, ngưỡng và quyết định giữ giường, báo vắng và ngày nghỉ bán trú (FR-048 → FR-061, BR-M02-06, 07, 17, 18).

**Independent Test**: Bảng mặc định 6.7; về nhà 5 ngày, nằm viện 35 ngày, ngắn ngày có ghi đè; kiểm hệ số từng ngày, giường, thông báo.

### Tests cho User Story 6

- [ ] T048 [P] [US6] Test đơn vị `BT/m02/BR_M02_06_TraChinhSachTest.java` (phủ bảng 6.7) và `BR_M02_17_QuanLyQuyetDinhNgay1Test.java`
- [ ] T049 [P] [US6] Test `BT/m02/NgayVangIT.java`, `BR_M02_07_QuyetDinhGiuGiuongIT.java`, `BR_M02_18_DinhChinhVuotNguongIT.java`, `FR_057_ChuyenVienKhiTamVangIT.java`: hệ số từng ngày; ngưỡng giữ giường báo Quản lý viện, không tự tạo yêu cầu quyết định; lệnh Quyết định giữ giường chỉ Quản lý viện, Trưởng tầng và Hành chính bị 403 (khóa `TAM_VANG_TRO_VE, TAO`, Q-345); giải phóng gọi `giaiPhongGiuongKhiVang`; đính chính lượt vắng tính lại ngày vắng và phát `NgayLuuTruDaDoi`; chuyển viện khi đang tạm vắng mở lượt mới trỏ `luot_truoc_id`
- [ ] T050 [P] [US6] Test `BT/m02/BanTruHeSoIT.java`, `NgayNghiBanTruIT.java`: báo vắng đủ hạn CFG-M02-06; ngày nghỉ bán trú khai báo muộn cần lý do; một ngày nghỉ Hiệu lực mỗi ngày

### Implementation cho User Story 6

- [ ] T051 [US6] Tạo `MIG/V004_07__vang_mat.sql`: `luot_vang` (`loai_vang ENUM('VE_NHA','DI_CHOI_GIA_DINH','DI_KHAM_TRONG_NGAY','LY_DO_KHAC','BENH_VIEN')`; `du_kien_tro_lai` bắt buộc trừ `BENH_VIEN` (`CHECK`); `trang_thai ENUM('DANG_MO','DA_DONG')`; cột sinh `nct_dang_mo` `UNIQUE`; trigger `trg_luot_vang_khong_sua`), `lich_su_du_kien_tro_lai`, `quyet_dinh_giu_giuong` (`lua_chon ENUM('GIU_TIEP','GIAI_PHONG','CHI_DAT_HE_SO')`; `ngay_xem_xet_lai` bắt buộc với `GIU_TIEP`), `bao_vang_ban_tru` (nhóm 3), `ngay_vang` (khóa chính `(luot_vang_id, ngay)`, `he_so DECIMAL(5,2)`, `nguon_dong ENUM('HOP_DONG','CO_SO','MAC_DINH')`), `ngay_nghi_ban_tru` (`trang_thai ENUM('HIEU_LUC','DA_HUY')`, cột sinh `ngay_hieu_luc` `UNIQUE`) (data-model mục 8)
- [ ] T052 [US6] Cài `BE/m02/vang/internal/TraChinhSachVang.java` (CFG-M02-05 đọc lúc chạy, ghi đè hợp đồng) và việc `NGAY_VANG` của job hằng ngày (ngưỡng, ngày xem xét lại, `XEM_XET_GIU_GIUONG`) (research T-08)
- [ ] T053 [US6] Cài phần vắng của `ThamGiaM02.java`: `CHO_TAM_VANG` (bản ghi đón qua `BanGhiDonPort` và `daDung`; mở lượt vắng), `CHUYEN_VIEN` (mở/đổi lượt loại `BENH_VIEN`), `GHI_NHAN_TRO_VE` (đóng lượt); cài `giuGiuongKhiVang`, `LuotVangApi.dangMo`
- [ ] T054 [US6] Cài `BE/m02/vang/internal/LuotVangService.java`: gia hạn dự kiến trở lại, quyết định giữ giường (khóa `TAM_VANG_TRO_VE, TAO` cộng kiểm `TaiKhoanApi.coVaiTro(QUAN_LY_VIEN)` trong service vì Trưởng tầng, Hành chính cũng có T ở dòng này, chú thích ⁸, Q-345; `giaiPhongGiuongKhiVang` của 003), báo vắng bán trú, ngày nghỉ bán trú; `DinhChinhDuocProvider` cho `M02.LUOT_VANG` (tính lại ngày vắng), `M02.QUYET_DINH_GIU_GIUONG`; `TrangThaiCoMatBanTruPort` dùng cho ngày bán trú
- [ ] T055 [US6] Tạo `BE/m02/vang/web/VangController.java`: `/luot-vang`, `/{id}`, `/{id}/gia-han-du-kien`, `/{id}/quyet-dinh-giu-giuong`, `/{id}/dinh-chinh`, `/bao-vang-ban-tru`, `/ngay-nghi-ban-tru`, `/{id}/huy`
- [ ] T056 [P] [US6] Tạo màn web `frontend/src/tinh-nang/vang-mat/` và app `mobile/lib/nhan-vien/vang-mat/` (Cho tạm vắng, Gia hạn, Ghi nhận trở về; chỉ trực tuyến) với test `mobile/test/nhan-vien/cho_tam_vang_test.dart`, `gia_han_du_kien_test.dart`

**Checkpoint**: Vắng mặt và chính sách phí chạy độc lập.

---

## Phase 9: User Story 7 – Kết thúc lưu trú chỉ hoàn tất khi đủ điều kiện (Priority: P2)

**Goal**: Hồ sơ kết thúc, bảng kỳ cuối nháp ở 010, danh sách điều kiện (a)–(h), bàn giao, ngoại lệ có duyệt, quá ngày dự kiến (FR-062 → FR-068).

**Independent Test**: Hồ sơ kết thúc cho người còn 1 đồ gửi, 1 thuốc gửi, 1 cảnh báo mở → lệnh bị chặn đủ lý do; xử lý từng điều kiện; thử một ngoại lệ có duyệt.

### Tests cho User Story 7

- [ ] T057 [P] [US7] Test `BT/m02/HoSoKetThucIT.java`, `DieuKienKetThucIT.java`, `NgoaiLeKetThucIT.java`, `FR_066_KyCuoiIT.java`, `FR_066a_QuaNgayDuKienIT.java`: một hồ sơ mở mỗi người; Bác sĩ lập phải `ly_do_y_te`; lập gọi `KyCuoiPort.taoKyCuoiNhap`, đổi ngày gọi `tinhLai`, hủy gọi `huyMocDung`; điều kiện đọc qua `xemDieuKien`; ngoại lệ có duyệt miễn đúng mã (không có `KT_E_BAN_GIAO`); dấu quá ngày dự kiến đúng ngày

### Implementation cho User Story 7

- [ ] T058 [US7] Tạo `MIG/V004_08__ket_thuc.sql`: `ho_so_ket_thuc` (`truong_hop ENUM('HET_HOP_DONG','XUAT_VIEN','CHUYEN_CO_SO','CHAM_DUT_HOP_DONG')`; `CHECK (truong_hop <> 'CHUYEN_CO_SO' OR noi_chuyen_den IS NOT NULL)`; `trang_thai ENUM('DANG_CHUAN_BI','HOAN_TAT','DA_HUY')`; cột sinh `nct_dang_mo` `UNIQUE`), `ban_giao_nct` (nhóm 3; `loai_nguoi_nhan ENUM('NGUOI_THAN','CO_SO_TIEP_NHAN')`), `ngoai_le_dieu_kien` (nhóm 3; `ma_dieu_kien ENUM('KT_A_CHI_PHI','KT_B_DO_GUI','KT_C_THUOC_GUI','KT_D_CANH_BAO_SU_CO','KT_H_QUYET_TOAN')`; `yeu_cau_phe_duyet_id UNIQUE`) (data-model mục 9)
- [ ] T059 [US7] Cài `BE/m02/ketthuc/internal/HoSoKetThucService.java` (lập, đổi ngày, hủy, bàn giao qua `BanGhiDonPort.daDung`; `DinhChinhDuocProvider` cho `M02.BAN_GIAO_NCT`) và handler `M02.NGOAI_LE_KET_THUC` (`quyenTao` gồm Kế toán khi `KT_H_QUYET_TOAN`; `apDung` thêm `ngoai_le_dieu_kien`) (research T-09)
- [ ] T060 [US7] Cài phần `KET_THUC_LUU_TRU` của `ThamGiaM02.java`: điều kiện (e) bàn giao; `mienDieuKien` từ `ngoai_le_dieu_kien`; tác động: hợp đồng `KET_THUC`/`CHAM_DUT`, hồ sơ `HOAN_TAT`, `luu_tru_nct.da_dong_luc`, phụ lục `KHONG_AP_DUNG`, `dongDoTrangThaiCuoi` cho yêu cầu mở (000); cài `KetThucLuuTruApi`; việc `KET_THUC` và nhắc `QUA_NGAY_DU_KIEN` của job
- [ ] T061 [US7] Tạo `BE/m02/ketthuc/web/KetThucController.java`: `/ho-so-ket-thuc`, `/{id}`, `/{id}/dieu-kien`, `/doi-ngay`, `/huy`, `/ban-giao`, `/yeu-cau-ngoai-le`; màn web `frontend/src/tinh-nang/ket-thuc-luu-tru/` và test `DieuKienKetThuc.test.jsx`
- [ ] T106 [P] [US7] *(Bổ sung 2026-10-22)* Mở rộng `DieuKienKetThucIT`, `NgoaiLeKetThucIT` và thêm `BT/m02/HoSoKetThucDocApiIT.java` (quickstart Q38 → Q42): dòng (h) gộp `KT_H_QUYET_TOAN`, `KT_H_XAC_NHAN_QUYET_TOAN`; ngoại lệ (h) không miễn ý xác nhận; lập ngoại lệ với `KT_H_XAC_NHAN_QUYET_TOAN` → 422; duyệt ngoại lệ phát `NgoaiLeKetThucDaDuyet`; `thongTinQuyetToan` trả đủ trường cả khi qua đời (FR-063 (h), FR-064; 017 FR-022, FR-034)
- [ ] T107 [US7] *(Bổ sung 2026-10-22)* Cài gộp dòng (h) trong màn điều kiện kết thúc, chặn ngoại lệ cho `KT_H_XAC_NHAN_QUYET_TOAN` trong handler `M02.NGOAI_LE_KET_THUC` và phát `NgoaiLeKetThucDaDuyet`; cài `BE/m02/ketthuc/internal/HoSoKetThucDocApiImpl.java` (`dangChuanBi`, `thongTinQuyetToan`, `coNgoaiLeQuyetToanDaDuyet`); màn web `ket-thuc-luu-tru/` hiện hai ý của (h) (research T-09)
- [ ] T108 [P] *(Bổ sung 2026-10-22)* Khai báo `HoSoKetThucDocApi` và sự kiện `NgoaiLeKetThucDaDuyet` ở `BE/m02/api/`, thêm mã `KT_H_XAC_NHAN_QUYET_TOAN` vào danh sách mã điều kiện kết thúc; cập nhật bản giả `BT/support/fake/m02/` và bản giả bean 017 (T002) trả hai mã (luật "Đổi api") (module-interfaces mục 1.5b, 2.1)

**Checkpoint**: Kết thúc lưu trú chạy với bean giả của module khác.

---

## Phase 10: User Story 8 – Ghi nhận qua đời và danh sách việc sau qua đời (Priority: P2)

**Goal**: Bản ghi qua đời, tác động tức thời, danh sách việc tính từ `MucViecSauQuaDoiPort`, nhắc, đóng hồ sơ (FR-069 → FR-073, BR-M02-21).

**Independent Test**: Ghi nhận qua đời cho người còn đồ gửi, thuốc gửi, chi phí chưa chốt; kiểm tác động, danh sách việc, nhắc, đóng hồ sơ.

### Tests cho User Story 8

- [ ] T062 [P] [US8] Test `BT/m02/GhiNhanQuaDoiIT.java`, `DanhSachViecSauQuaDoiIT.java`, `BR_M02_21_NguyenNhanTuVongIT.java`: Hành chính ghi phải có tệp bằng chứng; danh sách việc cập nhật theo port; nhắc `VIEC_SAU_QUA_DOI` tới khi hoàn thành; `nguyenNhan` chỉ vai trò được xem `SUC_KHOE` thấy

### Implementation cho User Story 8

- [ ] T063 [US8] Tạo `MIG/V004_09__qua_doi.sql`: `ban_ghi_qua_doi` (nhóm 3; `nct_id UNIQUE`; `nguyen_nhan VARCHAR(2000) NULL`) và `muc_viec_sau_qua_doi` (khóa chính `(ban_ghi_qua_doi_id, ma_muc)`; `ma_muc ENUM('DO_GUI','THUOC_GUI','CHI_PHI_KY_CUOI','CANH_BAO_SU_CO','QUYET_TOAN')`; `trang_thai ENUM('CHUA_HOAN_THANH','HOAN_THANH','KHONG_AP_DUNG')`) (data-model mục 10)
- [ ] T064 [US8] Cài phần `GHI_NHAN_QUA_DOI` của `ThamGiaM02.java` (bản ghi qua đời, hợp đồng `CHAM_DUT`, `dongDoTrangThaiCuoi`) và `BE/m02/quadoi/internal/ViecSauQuaDoiService.java` (`DanhSachViecSauQuaDoiApi`; tính lại theo `MucViecSauQuaDoiPort`; đóng hồ sơ khi đủ; nhắc CFG-M02-09) và `DinhChinhDuocProvider` `M02.BAN_GHI_QUA_DOI` (research T-13)
- [ ] T065 [US8] Tạo `BE/m02/quadoi/web/QuaDoiController.java`: `GET /nguoi-cao-tuoi/{nctId}/ban-ghi-qua-doi`, `POST /ban-ghi-qua-doi/{id}/dinh-chinh`, `GET /nguoi-cao-tuoi/{nctId}/viec-sau-qua-doi`; màn web `frontend/src/tinh-nang/qua-doi/`
- [ ] T109 [US8] *(Bổ sung 2026-10-22, FR-071a, Q-387)* Ngoại lệ mục quyết toán sau qua đời: cột `nct_id`, `ho_so_ket_thuc_id` rỗng được và `CHECK` ở `ngoai_le_dieu_kien` (migration của 004 lúc làm); endpoint `POST /nguoi-cao-tuoi/{nctId}/viec-sau-qua-doi/yeu-cau-ngoai-le-quyet-toan`; handler `M02.NGOAI_LE_KET_THUC` nhận căn cứ danh sách việc sau qua đời; `coNgoaiLeQuyetToanDaDuyet` trả cả hai căn cứ; nút trên màn danh sách việc sau qua đời; test mở rộng `NgoaiLeKetThucIT` (quickstart Q43)

**Checkpoint**: Qua đời chạy độc lập.

---

## Phase 11: User Story 9 – Quản lý viện quản lý danh mục dịch vụ và phiên bản đơn giá (Priority: P3)

**Goal**: Lệnh dịch vụ và phiên bản đơn giá; không chồng khoảng; không sửa khi đã tham chiếu; hợp đồng lệch giá (FR-034 → FR-036, DBR-08).

**Independent Test**: Tạo dịch vụ, hai phiên bản đơn giá liên tiếp, thử phiên bản chồng khoảng, thử sửa phiên bản đã áp dụng.

### Tests cho User Story 9

- [ ] T066 [P] [US9] Test `BT/m02/DichVuIT.java`, `DBR_08_PhienBanDonGiaIT.java`, `HopDongLechGiaIT.java`: chỉ AC-01; chồng khoảng bị khóa `dich_vu` và trigger chặn; phiên bản đã tham chiếu chỉ được đóng `hieu_luc_den`; một dịch vụ chăm sóc Hiệu lực mỗi mức (Q-297); liệt kê hợp đồng lệch giá

### Implementation cho User Story 9

- [ ] T067 [US9] Cài lệnh dịch vụ (tạo, sửa, ngừng hiệu lực) và phiên bản đơn giá (thêm dưới khóa `dich_vu`, đóng khoảng) trong `BE/m02/dichvu/internal/DichVuService.java` (research T-05)
- [ ] T068 [US9] Tạo `BE/m02/dichvu/web/DichVuController.java`: `/dich-vu`, `/{id}`, `/{id}/ngung-hieu-luc`, `/{id}/phien-ban-don-gia`, `/{id}/hop-dong-lech-gia`; màn web `frontend/src/tinh-nang/dich-vu-don-gia/`

**Checkpoint**: Mọi user story của 004 chạy độc lập.

---

## Phase 12: Polish & Cross-Cutting

- [ ] T069 [P] Cài `NgayLuuTruApi` (010; chưa vào ở, mốc dừng, ngày qua đời, ngày vắng, bán trú) trong `BE/m02/tinhphi/internal/NgayLuuTruService.java` và test `BT/m02/NgayLuuTruApiIT.java`; cài `LuuTruThongKeApi` (016)
- [ ] T070 [P] Test SPI `BT/m02/HopDongApiSpiIT.java`, `NguonLuuTruChoPhanBoIT.java`, `BeanThamGiaChuyenTrangThaiM02IT.java` (mỗi lệnh, rollback)
- [ ] T071 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/004-admission-residency/contracts/api-004.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 004
- [ ] T072 Khi 005, 006, 007, 010, 012, 013, 017 có bản thật, chạy lại test US1, US4, US6, US7, US8 với bean thật thay T002
- [ ] T073 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T074 Rà truy vết data-model mục 15: DBR-02, 06, 07, 08, 23, 25 và BR-M02-01 → 10, 15 → 22 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-074, FR-075, FR-075a, FR-078 (bổ sung 2026-10-10) — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story; Foundational của 004 chặn 005, 010, 012, 017, 018 (cần `HopDongDocApi`, `NgayLuuTruApi`, SPI)
- **US1 (P1)**: sau Foundational
- **US2 (P1)**: sau US1
- **US3 (P1)**: sau Foundational (dịch vụ dữ liệu test); US1 để chạy Hoàn tất tiếp nhận
- **US4 (P1)**: sau US3
- **US5 (P2)**: sau US3
- **US6 (P2)**: sau US3 (dòng ghi đè chính sách)
- **US7 (P2)**: sau US3, US6
- **US8 (P2)**: sau US3
- **US9 (P3)**: sau Foundational; độc lập
- *(2026-10-22)* T108 sớm (017 cần `HoSoKetThucDocApi`); T106, T107 sau T059 → T061

### Parallel Opportunities

- T003, T004, T010–T012 song song
- US2 và US3 song song sau US1; US5, US6, US8, US9 song song sau US3
- Test [P] trong mỗi story song song

## Parallel Example: User Story 3

```text
Task: "T029 HopDongVongDoiIT + HopDongKhoaNoiDungDbIT + DBR_06"
Task: "T030 FR_021 + XacDinhKhacChuanTest + DuyetDieuKhoanIT"
Task: "T031 BR_M02_09 + BR_M02_05 + FR_029"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US3 → US4
2. **Dừng và kiểm chứng**: một người đi từ đăng ký → hợp đồng ký → cọc → Hoàn tất tiếp nhận (001) → phân bổ giường bắt đầu (003)
3. Thêm US2 cho danh sách chờ

### Incremental Delivery

US1 → US3 → US4 → US2 → US5 → US6 → US7 → US8 → US9; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- 004 không tự lập yêu cầu, chi phí, hồ sơ; không đề xuất hồ sơ chờ (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M02-02 → 10)
- Bảng kỳ cuối nháp ở 010 là tác động kèm lệnh lập hồ sơ kết thúc (FR-066)
