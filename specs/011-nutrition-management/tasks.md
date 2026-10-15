---
description: "Danh sách task triển khai feature 011 – Quản lý dinh dưỡng và suất ăn"
---

# Tasks: Quản lý dinh dưỡng và suất ăn (011)

**Input**: Tài liệu thiết kế ở `specs/011-nutrition-management/` (plan.md, spec.md, research.md M-01 → M-15, data-model.md, contracts/api-011.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 001 (`DiNguyenApi`, `MucSucKhoeApi`, sự kiện dị ứng, `ThamGiaChuyenTrangThai`), 003 (`ViTriApi`), 004 (sự kiện vắng, bán trú), 005 (`NguonCongViecPort`, `CoMatBanTruApi`, `AnKemKeoDai`), 007 (`SuCoLenhApi`, `SutCanPhatHien`, `YeuCauTaoCanhBao`), 010 (`SuKienTinhPhiNguon`) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Dùng ở giai đoạn triển khai; CLAUDE.md hiện chặn `/speckit-implement`.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/vn/eldercare/`, `BT/` = `backend/src/test/java/vn/eldercare/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M08 (phần dinh dưỡng): `BE/m08/{api,danhmuc,chedoan,doichieu,thucdon,chotsuat,phieu,phucvu,doangiadinh,xemlai,luumau,lich}/`; 019 thêm `m08.kho`
- Web `web/src/tinh-nang/dinh-duong/{danh-muc,ban-gan,thuc-don,chot-suat,phieu,do-an-gia-dinh,luu-mau}/`; app nhân viên `app-nhan-vien/lib/tinh-nang/dinh-duong/`

---

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/m08/...` với `package-info.java`; ArchUnit `BT/m08/M08ArchTest.java`: gói `..lich..` không gọi lệnh chốt, không lập phiếu (Q-306); DTO bếp không tham chiếu kiểu sức khỏe (research M-01, M-09)
- [ ] T002 [P] Tạo bản giả trong `BT/support/gia011/`: `LuotOLaiAnPort` (012), `ChuyenDiBuaAnPort` (014), `TonKhoNguyenLieuPort` (019); phát `LuotOLaiDaDoi`, `ChuyenDiDaDoi` (contracts mục 1.6, 1.7)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `m08.api`, sự kiện, hàm đối chiếu xung đột, danh mục, khung job, listener idempotent.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T003 [P] Tạo `BE/m08/api/`: `CheDoAnApi`, `ThucDonChungApi`, `LichBuaApi`, `DinhDuongThongKeApi`, SPI `LuotOLaiAnPort`, `ChuyenDiBuaAnPort`, record `LuotOLaiDaDoi`, `ChuyenDiDaDoi`, port `TonKhoNguyenLieuPort` và sự kiện mục 1.8 (cài `CoYeuCauThongBao`) (contracts mục 1.2 → 1.8)
- [ ] T004 Tạo `MIG/V011_01__nen_m08.sql`: `dinh_duong_nct` (`nct_id` khóa chính), `bua_an` (`ma` khóa của CFG-M08-04; `phuc_vu_noi_tru`, `phuc_vu_ban_tru`), `loai_do_an_cam`, `su_kien_m08_da_xu_ly` (`khoa_su_kien UNIQUE`), `lan_chay_m08` (data-model mục 2, 3, 11)
- [ ] T005 [P] Cài hàm thuần `BE/m08/doichieu/internal/XungDot.java`, `ChonMonThayThe.java` và test `BT/m08/XungDotTest.java`, `ChonMonThayTheTest.java` (dị ứng và hạn chế, BR-M08-02, BR-M01-07; research M-06)
- [ ] T006 Cài khung job `BE/m08/lich/ViecTheoPhutM08.java`, `ViecHangNgayM08.java` (ShedLock, `lan_chay_m08`, chạy bù) và test `BT/m08/ChayBuM08IT.java`; khung listener idempotent `BE/m08/doichieu/internal/SuKienM08Listener.java` (chỉ xét bữa đã chốt chưa tới giờ) (research M-08, M-15)
- [ ] T007 [P] Cài provider: `PhanLoaiTruongProvider` (dị ứng, hạn chế → `SUC_KHOE`), `LuuTruProvider` (`THEO_HO_SO` cho bản gán, xác nhận phục vụ, đồ ăn gia đình, suất ăn người thân; còn lại `KHONG_GAN_HO_SO`), `GanPhamViProvider` (`M08.BAN_GAN`, `M08.XAC_NHAN_PHUC_VU`, `M08.DO_AN_GIA_DINH`, `M08.PHIEU_BUA_AN`)

**Checkpoint**: Nền 011 sẵn sàng.

---

## Phase 3: User Story 2 – Món ăn gắn thành phần gây dị ứng, Bác sĩ xác nhận; xung đột phát hiện ở mọi điểm (Priority: P1) 🎯 MVP

**Goal**: Chế độ ăn (hạn chế, món an toàn, một mặc định), món ăn có phiên bản nội dung do Bác sĩ xác nhận, đối chiếu xung đột cho một người (FR-001 → FR-007, FR-020 → FR-022, Q-308).

**Independent Test**: Bếp tạo "Cá kho" có thành phần "cá", "Đậu phụ sốt cà" làm món thay thế; Bác sĩ xác nhận một món, trả lại một món; bếp sửa thành phần món đã xác nhận → bản sửa chờ, bản Hiệu lực cũ vẫn dùng.

### Tests cho User Story 2

- [ ] T008 [P] [US2] Test `BT/m08/FR_004a_XacNhanMonIT.java`, `FR_014_CheDoMacDinhIT.java`: bảng chuyển món; bản sửa là phiên bản mới; ngừng món an toàn của chế độ Hiệu lực bị chặn; một chế độ mặc định (`UNIQUE khoa_mac_dinh`); đổi hạn chế chạy lại FR-005 với thực đơn chưa hết tuần

### Implementation cho User Story 2

- [ ] T009 [US2] Tạo `MIG/V011_02__danh_muc_mon.sql`: `che_do_an` (`lien_quan_dieu_tri`; cột sinh `khoa_mac_dinh` `UNIQUE`; `mon_an_toan_id`), `che_do_an_han_che`, `mon_an` (`trang_thai` `NHAP|CHO_XAC_NHAN|HIEU_LUC|NGUNG_HIEU_LUC|DA_HUY`; `phien_ban_hieu_luc_id`; `tung_hieu_luc`), `mon_an_phien_ban` (nhóm 3 có bản chờ; `ket_cau` `THUONG|MEM|XAY_NHUYEN`), `mon_an_phien_ban_thanh_phan`, `mon_an_phien_ban_che_do` (data-model mục 3)
- [ ] T010 [US2] Cài `BE/m08/danhmuc/internal/CheDoAnService.java`, `MonAnService.java` (tạo, gửi xác nhận, xác nhận/trả lại/rút, bản sửa, ngừng, hủy; Bác sĩ xác nhận, Nhân viên bếp lập; Q-283, Q-308), `BuaAnService.java`, `LoaiDoAnCamService.java` (research M-04)
- [ ] T011 [US2] Tạo `BE/m08/danhmuc/web/DanhMucController.java`: `/dinh-duong/che-do-an`, `/{id}`, `/{id}/ngung-hieu-luc`, `/thanh-phan-thuc-pham`, `/mon-an`, `/{id}`, `/{id}/{lenh}`, `/bua-an`, `/{id}`, `/loai-do-an-cam`, `GET /nguoi-cao-tuoi/{nctId}/doi-chieu-mon`; màn web `web/src/tinh-nang/dinh-duong/danh-muc/`

**Checkpoint**: Món và chế độ ăn chạy độc lập.

---

## Phase 4: User Story 1 – Bác sĩ gán chế độ ăn; thay đổi liên quan điều trị bắt buộc lý do (Priority: P1)

**Goal**: Bản gán Đề xuất → Áp dụng (Hiệu lực/Chờ hiệu lực) → Ngừng; lý do chuyên môn khi `CanLyDoChuyenMon`; cảnh báo xung đột xác nhận; chế độ mặc định; trạng thái cuối (FR-010 → FR-017, BR-M08-03).

**Independent Test**: A có "Thường" Hiệu lực; áp dụng "Tiểu đường" kết cấu "mềm" thiếu lý do (bị chặn) rồi có lý do; lập bản cho ngày mai; trạng thái cuối hủy bản chờ.

### Tests cho User Story 1

- [ ] T012 [P] [US1] Test đơn vị `BT/m08/CanLyDoChuyenMonTest.java` và `BT/m08/BR_M08_03_BanGanIT.java`, `FR_015_TrangThaiCuoiIT.java`: một bản chờ, một bản Hiệu lực mỗi người (`UNIQUE`); hai Bác sĩ áp dụng đồng thời; trigger chặn sửa nội dung; listener dị ứng mới đối chiếu và `daKiemTra` (Q1 → Q5)

### Implementation cho User Story 1

- [ ] T013 [US1] Tạo bảng `ban_gan_che_do_an` (`trang_thai` `DE_XUAT|CHO_HIEU_LUC|HIEU_LUC|NGUNG|DA_HUY`; `can_cu` `DANH_GIA|XEM_LAI|KHAC`; cột sinh `khoa_cho`, `khoa_hieu_luc` `UNIQUE`; trigger), `ban_gan_han_che` trong `MIG/V011_03__ban_gan.sql`
- [ ] T014 [US1] Cài `BE/m08/chedoan/internal/BanGanService.java` (lập, áp dụng có xác nhận cảnh báo, hủy; khóa `dinh_duong_nct`; chế độ mặc định khi không có bản gán), việc theo phút Chờ hiệu lực → Hiệu lực, bean `ThamGiaChuyenTrangThai` (`@Order` sau 006, trước 010; tác động trạng thái cuối), listener `DiUngDaGhiNhan`, `DiUngKhacDaGhiNhan`, `DiUngDaLoaiTru`; `CheDoAnApi` (research M-05)
- [ ] T015 [US1] Tạo endpoint `/nguoi-cao-tuoi/{nctId}/ban-gan-che-do-an`, `/ban-gan-che-do-an/{id}/ap-dung`, `/huy`; màn web `web/src/tinh-nang/dinh-duong/ban-gan/`

**Checkpoint**: Bản gán chạy độc lập.

---

## Phase 5: User Story 3 – Nhân viên bếp lập thực đơn tuần, Bác sĩ công bố (Priority: P1)

**Goal**: Thực đơn tuần Nháp → Chờ công bố → Công bố → Đã áp dụng; kiểm tra độ phủ, lặp món, dị ứng trong một lần; xác nhận chế biến; đổi món có lý do; nhắc (FR-023 → FR-032, BR-M08-06 → 08, 16).

**Independent Test**: 4 chế độ ăn có người dùng; thực đơn thiếu bữa tối thứ 6 của "Tiểu đường", "Cháo gà" ba sáng liên tiếp, một món chứa thành phần dị ứng → mọi lỗi hiện một lần.

### Tests cho User Story 3

- [ ] T016 [P] [US3] Test đơn vị `BT/m08/KiemTraDoPhuTest.java`, `LapMonTest.java` (qua tuần)
- [ ] T017 [P] [US3] Test `BT/m08/BR_M08_06_DoPhuIT.java`, `BR_M08_07_LapMonIT.java`, `BR_M08_08_DoiMonIT.java`, `BR_M08_16_NhacThucDonIT.java`: một thực đơn chưa hủy mỗi tuần (`UNIQUE khoa_tuan`); cảnh báo dị ứng cho bếp chỉ có số người, không có tên; đổi món sau chốt tạo phát sinh (Q6 → Q19)

### Implementation cho User Story 3

- [ ] T018 [US3] Tạo `MIG/V011_04__thuc_don.sql`: `thuc_don` (`trang_thai` `NHAP|CHO_CONG_BO|CONG_BO|DA_AP_DUNG|DA_HUY`; cột sinh `khoa_tuan` `UNIQUE`; `xac_nhan_che_bien` bắt buộc khi gửi), `thuc_don_mon`, `thuc_don_mon_thay_the` (sửa chỉ khi `NHAP`, trigger), `thuc_don_kiem_tra` (nhóm 3), `doi_mon` (nhóm 3) (data-model mục 5)
- [ ] T019 [US3] Cài `BE/m08/thucdon/internal/ThucDonService.java` (lập, sao chép, kiểm tra một lần, gửi, công bố, trả lại, hủy, đổi món; hiển thị tồn kho qua `TonKhoNguyenLieuPort`; `ThucDonChungApi`; việc hằng ngày nhắc lập thực đơn và Đã áp dụng) (research M-07)
- [ ] T020 [US3] Tạo endpoint `/thuc-don`, `/{id}`, `/{id}/truoc-khi-gui`, `/{id}/kiem-tra`, `/{id}/{lenh}`, `/{id}/doi-mon`; màn web `web/src/tinh-nang/dinh-duong/thuc-don/` (mọi lỗi hiện một lần) với test Vitest

**Checkpoint**: Thực đơn chạy độc lập.

---

## Phase 6: User Story 4 – Nhân viên bếp chốt số suất trước mỗi bữa (Priority: P1)

**Goal**: Tính sẵn khi bếp mở lệnh (chỉ xem); lệnh Chốt suất lập lần chốt, người được tính, suất theo chế độ, phiếu mỗi tầng/khu, suất ăn người thân trong một transaction; phát sinh sau chốt từ listener; suất giữ; món an toàn tại mốc CFG-M08-05; lịch bữa cho 005; DBR-27 (FR-033 → FR-043, BR-M08-01, 09, 13).

**Independent Test**: 3 tầng và khu bán trú: 40 nội trú (2 Tạm vắng, 1 bệnh viện, 3 đi hoạt động ngoài viện qua bữa trưa), 8 bán trú có lịch, người thân ở lại có đăng ký ăn → số suất đúng tính tay.

### Tests cho User Story 4

- [ ] T021 [P] [US4] Test đơn vị `BT/m08/NguoiDuocTinhTest.java`, `TinhPhatSinhTest.java`
- [ ] T022 [P] [US4] Test `BT/m08/BR_M08_01_ChotSuatIT.java`, `BR_M08_13_PhatSinhIT.java`, `FR_040a_SuatGiuIT.java`, `FR_037a_MonAnToanIT.java`, `FR_043_SuatNguoiThanIT.java`, `DBR_27_TongPhieuIT.java`: một lần chốt mỗi bữa ngày (hai lệnh đồng thời); không tự chốt; phát sinh ≤ 5 phút, idempotent (SC-002); suất người thân phát `SuKienTinhPhiNguon` `SUAT_AN_NGUOI_THAN`; SC-001 bộ 7 ngày × 60 người (Q20 → Q31)

### Implementation cho User Story 4

- [ ] T023 [US4] Tạo `MIG/V011_05__chot_suat.sql`: `bua_ngay` (`UNIQUE (bua_an_id, ngay)`; `gio_du_kien` chốt từ CFG-M08-04), `lan_chot` (`bua_ngay_id UNIQUE`), `nguoi_duoc_tinh` (`loai` `NOI_TRU|BAN_TRU|NGUOI_THAN`), `suat_theo_che_do`, `phat_sinh` (`khoa_su_kien UNIQUE`; `can_giao`; xác nhận đặt một lần), `yeu_cau_suat_bo_sung`, `suat_an_nguoi_than` (`trang_thai` `DA_GHI|DA_HUY`; `ma_su_kien_tinh_phi`) (data-model mục 6)
- [ ] T024 [US4] Cài `BE/m08/chotsuat/internal/ChotSuatService.java`: tính sẵn (`NguoiDuocTinh` từ trạng thái 001, vị trí 003, vắng 004, có mặt 005, `LuotOLaiAnPort`, `ChuyenDiBuaAnPort`), lệnh chốt dưới khóa `bua_ngay` (phiếu và suất đặc biệt US5 trong cùng transaction), suất giữ, món an toàn, suất ăn người thân (research M-08)
- [ ] T025 [US4] Cài listener phát sinh trong `SuKienM08Listener.java` (`NguoiCaoTuoiDaChuyenTrangThai`, `DaChuyenGiuong`, `LuotVang*`, `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi`, `PhuLucDaApDung`, `TrangThaiCoMat*`, `LuotOLaiDaDoi`, `ChuyenDiDaDoi`, đổi bản gán, đổi món) và lệnh bếp xác nhận phát sinh; việc theo phút: nhắc chốt, món an toàn, suất giữ, nhắc phát sinh
- [ ] T026 [US4] Cài `NguonCongViecPort` loại `BUA` (`kiemTra`, `xacNhanPhucVu`; bữa bỏ vì vắng → `nguonBiHuy`) và `LichBuaApi` trong `BE/m08/phucvu/internal/`
- [ ] T027 [US4] Tạo endpoint `/bua-ngay`, `/{id}/tinh-san`, `/{id}/chot-suat`, `/{id}/phat-sinh`, `/phat-sinh/{id}/xac-nhan`, `/bua-ngay/{id}/suat-bo-sung`, `/suat-dac-biet/{id}/phuc-vu-suat-giu`, `/huy-suat-giu`, `/chi-dinh-mon`; màn web `web/src/tinh-nang/dinh-duong/chot-suat/` (tính sẵn chỉ xem, nút chốt tách riêng)

**Checkpoint**: P1 hoàn tất.

---

## Phase 7: User Story 5 – Chuẩn bị, giao phiếu bữa ăn; tầng kiểm đếm và nhận (Priority: P2)

**Goal**: Phiếu mỗi tầng/khu; suất đặc biệt có nhãn; giao khi mọi suất đặc biệt đã chuẩn bị và có lưu mẫu; nhận; sai lệch và xử lý; phần bổ sung; báo giao trễ; DTO bếp không có trường sức khỏe (FR-044 → FR-048b, BR-M08-09 → 12, SC-008).

**Independent Test**: Bữa trưa Tầng 2 có 14 suất (12 Thường, 2 Tiểu đường) và 3 suất đặc biệt; giao khi còn 1 suất chưa chuẩn bị (bị chặn); giao; tầng báo thiếu 1; bếp bổ sung.

### Tests cho User Story 5

- [ ] T028 [P] [US5] Test `BT/m08/BR_M08_10_GiaoPhieuIT.java`, `BR_M08_11_NhanPhieuIT.java`, `BR_M08_12_GiaoTreIT.java`, `FR_048b_PhanBoSungIT.java`, `SC_008_DtoBepTest.java` (quét mọi DTO, thông báo tới bếp) (Q32 → Q37)

### Implementation cho User Story 5

- [ ] T029 [US5] Tạo `MIG/V011_06__phieu.sql`: `phieu_bua_an` (`UNIQUE (bua_ngay_id, tang_khu_vuc_id)`; `trang_thai` `DA_CHOT|DA_CHUAN_BI|DA_GIAO|CO_SAI_LECH|DA_NHAN`), `suat_dac_biet`, `sai_lech_phieu` (nhóm 3; xử lý đặt một lần), `phan_bo_sung` (nhóm 3; đúng một nguồn khác `NULL`)
- [ ] T030 [US5] Cài `BE/m08/phieu/internal/PhieuService.java` (chuẩn bị, giao, nhận, sai lệch, xử lý, phần bổ sung; DTO bếp và DTO tầng tách riêng; việc theo phút báo giao trễ một lần) (research M-09)
- [ ] T031 [US5] Tạo endpoint `/phieu-bua-an`, `/suat-dac-biet/{id}/da-chuan-bi`, `/phieu-bua-an/{id}/{lenh}`, `/phan-bo-sung/{id}/{lenh}`; màn web `web/src/tinh-nang/dinh-duong/phieu/`, app `app-nhan-vien/lib/tinh-nang/dinh-duong/` (nhận phiếu, sai lệch, suất bổ sung)

**Checkpoint**: Phiếu chạy độc lập.

---

## Phase 8: User Story 6 – Xác nhận phục vụ suất đặc biệt (Priority: P2)

**Goal**: Xác nhận đúng người, đúng suất; chặn khi xung đột dị ứng; đã ăn thì ghi sự cố qua `SuCoLenhApi`; ngoại tuyến (FR-049 → FR-051, BR-M08-14).

**Independent Test**: Phục vụ suất của E cho E; suất của E cho G; cho F một suất chứa "tôm" khi F vừa có dị ứng tôm sau chốt; khai "đã ăn" → sự cố; "chưa ăn" → không sự cố.

### Tests cho User Story 6

- [ ] T032 [P] [US6] Test `BT/m08/BR_M08_14_XacNhanPhucVuIT.java`, `FR_050_ChanKetQuaAnIT.java`; app `app-nhan-vien/test/xac_nhan_phuc_vu_test.dart` (chọn suất theo nhãn, khai đã ăn/chưa ăn, ngoại tuyến) (Q38 → Q41)

### Implementation cho User Story 6

- [ ] T033 [US6] Tạo bảng `xac_nhan_phuc_vu` (nhóm 3; `ket_qua` `DUNG_SUAT|CHAN_XUNG_DOT_DA_AN|CHAN_XUNG_DOT_CHUA_AN`) trong `MIG/V011_07__phuc_vu.sql`; cài `BE/m08/phucvu/internal/XacNhanPhucVuService.java` (`XungDot` tại lúc phục vụ; đã ăn → `SuCoLenhApi`; chưa ăn → `YeuCauTaoCanhBao`), `NgoaiTuyenHandler` `M08.XAC_NHAN_PHUC_VU`, `DinhChinhDuocProvider` `M08.XAC_NHAN_PHUC_VU` và endpoint `POST /suat-dac-biet/{id}/xac-nhan-phuc-vu` (research M-10)

**Checkpoint**: Phục vụ an toàn.

---

## Phase 9: User Story 7 – Đồ ăn gia đình mang vào (Priority: P2)

**Goal**: `DoiChieuDoAn` với dị ứng, hạn chế, loại cấm; Không sử dụng / Cần xác nhận / Được sử dụng / Đã kết thúc; xung đột mới, hết hạn chuyển Không sử dụng (FR-052 → FR-056, BR-M08-04).

**Independent Test**: 4 lần gửi: bánh có "đậu phộng" cho người dị ứng; "rượu thuốc" (bị cấm); hộp cơm "không rõ"; trái cây cho người không có hạn chế.

### Tests cho User Story 7

- [ ] T034 [P] [US7] Test đơn vị `BT/m08/DoiChieuDoAnTest.java` và `BT/m08/BR_M08_04_DoAnGiaDinhIT.java` (không có chuyển `KHONG_SU_DUNG → DUOC_SU_DUNG`) (Q42 → Q44)

### Implementation cho User Story 7

- [ ] T035 [US7] Tạo bảng `do_an_gia_dinh` (`trang_thai` `KHONG_SU_DUNG|CAN_XAC_NHAN|DUOC_SU_DUNG|DA_KET_THUC`) và bảng con thành phần trong `MIG/V011_08__do_an_gia_dinh.sql`; cài `BE/m08/doangiadinh/internal/DoAnGiaDinhService.java` (khóa `dinh_duong_nct`; việc theo phút hạn dùng; listener dị ứng mới) và endpoint `/nguoi-cao-tuoi/{nctId}/do-an-gia-dinh`, `/do-an-gia-dinh/{id}/{lenh}`; màn web `web/src/tinh-nang/dinh-duong/do-an-gia-dinh/`, app phần đồ ăn gia đình (research M-11)

**Checkpoint**: Đồ ăn gia đình chạy độc lập.

---

## Phase 10: User Story 8 – Bác sĩ được báo xem lại chế độ ăn (Priority: P3)

**Goal**: `AnKemKeoDai` (005), `SutCanPhatHien` (007) → báo Bác sĩ; không tạo yêu cầu (FR-058, FR-059, BR-M08-05, Q-307).

**Independent Test**: B có 3 bữa kém liên tiếp, rồi sụt cân → Bác sĩ được báo; bác sĩ lập đề xuất có tham chiếu thông báo.

### Tests cho User Story 8

- [ ] T036 [P] [US8] Test `BT/m08/BR_M08_05_XemLaiIT.java` (Q45)

### Implementation cho User Story 8

- [ ] T037 [US8] Tạo bảng kỹ thuật `thong_bao_xem_lai` (không trạng thái) trong `MIG/V011_09__xem_lai.sql`; cài listener `BE/m08/xemlai/internal/XemLaiListener.java` và endpoint `GET /dinh-duong/thong-bao-xem-lai`; bản gán tham chiếu `thong_bao_xem_lai_id`

**Checkpoint**: Xem lại chạy.

---

## Phase 11: User Story 9 – Lưu mẫu thức ăn mỗi bữa (Priority: P3)

**Goal**: Giao phiếu cần bản ghi lưu mẫu; hết hạn lưu CFG-M08-06; ghi hủy mẫu (FR-060 → FR-062, BR-M08-15).

**Independent Test**: Giao phiếu khi chưa có lưu mẫu (bị chặn); ghi lưu mẫu; giao; quá CFG-M08-06 → nhắc; ghi hủy mẫu.

### Tests cho User Story 9

- [ ] T038 [P] [US9] Test đơn vị `BT/m08/MonCanLuuMauTest.java` và `BT/m08/BR_M08_15_LuuMauIT.java` (Q46, Q47)

### Implementation cho User Story 9

- [ ] T039 [US9] Tạo bảng `luu_mau` (nhóm 3; `bua_ngay_id UNIQUE`; `het_han_luu`; hủy đặt một lần), `luu_mau_mon` trong `MIG/V011_10__luu_mau.sql`; cài `BE/m08/luumau/internal/LuuMauService.java` (`DinhChinhDuocProvider` `M08.LUU_MAU`; nhắc hủy mẫu) và endpoint `POST /bua-ngay/{id}/luu-mau`, `/luu-mau/{id}/huy-mau`; màn web `web/src/tinh-nang/dinh-duong/luu-mau/`

**Checkpoint**: Mọi user story của 011 chạy độc lập.

---

## Phase 12: Polish & Cross-Cutting

- [ ] T040 [P] Cài `DinhDuongThongKeApi` (016)
- [ ] T041 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/011-nutrition-management/contracts/api-011.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 011
- [ ] T042 Khi 012, 014, 019 có bản thật, chạy lại test US4 với bean thật thay T002; thay bản giả `NguonCongViecPort` `BUA` ở test 005
- [ ] T043 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC-001, SC-002 theo quickstart mục 5
- [ ] T044 Rà truy vết: BR-M08-01 → 16, DBR-15 (phần 011), DBR-25, DBR-27 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-057, FR-063, FR-064, FR-065 — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US2 (P1)**: sau Foundational (danh mục món, chế độ ăn)
- **US1 (P1)**: sau US2
- **US3 (P1)**: sau US2
- **US4 (P1)**: sau US1, US3
- **US5, US6 (P2)**: sau US4; **US7 (P2)**: sau US1
- **US8 (P3)**: sau US1; **US9 (P3)**: sau US5

### Parallel Opportunities

- T003, T005, T007 song song; US1 và US3 song song sau US2; US7 song song với US4

## Parallel Example: User Story 4

```text
Task: "T021 NguoiDuocTinhTest + TinhPhatSinhTest"
Task: "T022 BR_M08_01/BR_M08_13/FR_040a/FR_037a/FR_043/DBR_27 IT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US2 → US1 → US3 → US4
2. **Dừng và kiểm chứng**: món → bản gán → thực đơn → chốt suất; DBR-27 giữ sau phát sinh
3. Thêm US5, US6 cho giao phiếu và phục vụ

### Incremental Delivery

US2 → US1 → US3 → US4 → US5 → US6 → US7 → US8 → US9; mỗi bước chạy `./mvnw verify`.

## Notes

- 011 không tự chốt suất, không lập phiếu ngoài lệnh của bếp, không tạo yêu cầu xem lại hay sự cố tự động (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M08-*)
