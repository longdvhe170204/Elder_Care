---
description: "Danh sách task triển khai feature 013 – Quản lý đồ gửi của người cao tuổi"
---

# Tasks: Quản lý đồ gửi của người cao tuổi (013)

**Input**: Tài liệu thiết kế ở `specs/013-personal-belongings/` (plan.md, spec.md, research.md K-01 → K-15, data-model.md, contracts/api-013.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 001, 003, 004 (`MucViecSauQuaDoiPort`, `DanhSachViecSauQuaDoiApi`), 007 (`SuCoLenhApi`), 008 (`CaTrucApi`, SPI `DoGuiBanGiaoPort`), 009 (`ThongBaoTraCuuApi`), 012 (`QuyenNguoiThanApi`, `NguoiDaiDienPort`) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/eldercare/backend/`, `BT/` = `backend/src/test/java/com/eldercare/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M12: `BE/belongings/{api,danhmuc,dogui,tra,dongy,suco,ketthuc,kiemke,lich}/`
- Web `frontend/src/tinh-nang/do-gui/{danh-muc,tiep-nhan,ban-giao,tra,kiem-ke,xu-ly-do}/`, `frontend/src/tinh-nang/cong-nguoi-than/do-gui/`; app nhân viên `mobile/lib/nhan-vien/do-gui/`; app người thân `mobile/lib/nguoi-than/do-gui/`

---

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; 009 FR-001a): sửa T002. Mã task cũ giữ nguyên.

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/belongings/...` với `package-info.java`; ArchUnit `BT/belongings/M12ArchTest.java`: gói `..lich..` không tạo phiếu kiểm kê, không gọi `SuCoLenhApi` (Q-306, Q-307) (research K-01)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `belongings.api`, danh mục, bảng đồ gửi và bàn giao nhóm 3, hàm kiểm lệnh, khóa theo người và đồ, khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T002 [P] Tạo `BE/belongings/api/`: `DoGuiApi`, `DoGuiThongKeApi`, sự kiện `DoGuiDaDoiTrangThai` và thông báo (cài `CoYeuCauThongBao`) (contracts mục 1.2 → 1.4); bổ sung 2026-10-18 (009 FR-001a): sự kiện có người nhận là người thân khai `doiTuongThongTin` theo research K-13, test hợp đồng kiểm mọi yêu cầu tới người thân có đối tượng
- [ ] T003 Tạo `MIG/V013_01__do_gui.sql`: `do_gui_nct` (`nct_id` khóa chính), `loai_do` (`co_so_tien`, `can_dong_y_giao_su_dung`), `loai_do_lich_su_dau` (nhóm 3), `muc_tinh_trang` (`khong_dung_duoc` – đúng một mức cuối, cột sinh `UNIQUE`), `vi_tri_luu_giu`, `do_gui` (đúng một trong `so_luong`/`so_tien` theo `co_so_tien`; `trang_thai` `DANG_GIU|DANG_SU_DUNG|DA_TRA|THAT_LAC|HU_HONG|DA_XU_LY` suy ra; `ban_giao_hien_hanh_id`; `bi_loai`; `phien_ban`; ba chỉ mục), `ban_giao_do_gui` (nhóm 3; trigger chặn `UPDATE` ngoài `su_co_id` từ `NULL`, chặn `DELETE`; thuộc danh sách bảo vệ BR-M15-03), `ban_giao_anh`, `su_kien_m12_da_xu_ly`, `lan_chay_m12` (data-model mục 2 → 4) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `do_gui_nct` → `trang_thai_nct`; `loai_do`, `muc_tinh_trang`, `vi_tri_luu_giu` → `danh_muc_chung`; `loai_do_lich_su_dau` → `lich_su_chung`; `ban_giao_anh` → cột `JSON` của `ban_giao_do_gui`; `su_kien_m12_da_xu_ly`, `lan_chay_m12` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T004 [P] Cài hàm thuần `BE/belongings/dogui/internal/KiemTraLenhBanGiao.java`, `TachDo.java` và test `BT/belongings/KiemTraLenhBanGiaoTest.java`, `TachDoTest.java` (research K-04)
- [ ] T005 Cài `BE/belongings/dogui/internal/KhoaDoGui.java` (khóa `do_gui_nct` rồi từng đồ; so `phien_ban` mỗi đồ trong lệnh; research K-03) và danh mục (Quản lý viện; loại đồ, mức tình trạng, vị trí – ngừng bị chặn khi còn đồ Đang giữ/Hư hỏng) trong `BE/belongings/danhmuc/internal/DanhMucM12Service.java`, controller `/do-gui/loai-do`, `/{id}`, `/do-gui/muc-tinh-trang`, `/do-gui/vi-tri` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `do_gui_nct` → `trang_thai_nct`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T006 Cài khung job `BE/belongings/lich/ViecTheoPhutM12.java`, `ViecHangNgayM12.java` (ShedLock, chạy bù) và test `BT/belongings/ChayBuM12IT.java` (research K-15)
- [ ] T007 [P] Cài provider: `PhanLoaiTruongProvider`, `LuuTruProvider`, `GanPhamViProvider` (`M12.DO_GUI`, `M12.XAC_NHAN_NGUOI_NHAN`, `M12.DONG_Y_TU_GIU`, `M12.PHIEU_KIEM_KE`), `QuyenTepProvider` (`M12.ANH_DO_GUI`, `M12.BAN_KY`) (contracts mục 1.1)

**Checkpoint**: Nền 013 sẵn sàng.

---

## Phase 3: User Story 1 – Tiếp nhận đồ gửi có vị trí và hình ảnh với đồ có giá trị (Priority: P1) 🎯 MVP

**Goal**: Lệnh Tiếp nhận tạo đồ và bản ghi `TIEP_NHAN` cùng transaction; ảnh bắt buộc với đồ có giá trị theo CFG-M12-01; Tiếp nhận và giao ngay khi có đồng ý cho tự giữ (FR-003 → FR-010, DBR-22, BR-M12-05, BR-M12-06).

**Independent Test**: A Đang lưu trú: tiếp nhận điện thoại không ảnh (bị chặn), rồi có ảnh; 2.000.000 đồng tiền mặt; 5 bộ quần áo giao ngay có đồng ý.

### Tests cho User Story 1

- [ ] T008 [P] [US1] Test `BT/belongings/DBR_22_TiepNhanGocIT.java`, `BR_M12_06_DongYTuGiuIT.java`: mỗi đồ truy về đúng một `TIEP_NHAN`; thiếu ảnh với đồ có giá trị → 422; giao sử dụng cần đồng ý Hiệu lực (`khoa_hieu_luc UNIQUE`); rút đồng ý (Q1 → Q4, Q11 → Q13)

### Implementation cho User Story 1

- [ ] T009 [US1] Tạo bảng `dong_y_tu_giu` (`cach` `CONG|BAN_KY|TRONG_LENH_TIEP_NHAN`; `trang_thai` `HIEU_LUC|DA_RUT|HET_HIEU_LUC`; cột sinh `khoa_hieu_luc` `UNIQUE`) trong `MIG/V013_02__dong_y.sql` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `dong_y_tu_giu` → `do_gui` (cột); ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T010 [US1] Cài `BE/belongings/dogui/internal/TiepNhanService.java` (Tiếp nhận, Tiếp nhận và giao ngay; người thực hiện Hành chính hoặc Điều dưỡng trong phạm vi; ảnh qua `TepApi`; đồ có giá trị theo CFG-M12-01 lúc lệnh) và `BE/belongings/dongy/internal/DongYTuGiuService.java` (người đại diện qua cổng hoặc bản ký) (research K-04, K-06)
- [ ] T011 [US1] Tạo endpoint `/nguoi-cao-tuoi/{nctId}/do-gui`, `/do-gui/{id}`, `/dong-y-tu-giu`, `/dong-y-tu-giu/{id}/rut`, `/cong/dong-y-tu-giu`, `/cong/dong-y-tu-giu/{id}/rut`; màn web `frontend/src/tinh-nang/do-gui/tiep-nhan/`; app `mobile/lib/nhan-vien/do-gui/` (ảnh, chặn thao tác khi mất mạng) với test flutter

**Checkpoint**: Tiếp nhận chạy độc lập.

---

## Phase 4: User Story 2 – Mọi lần bàn giao ghi ai giao, ai nhận, khi nào, vật gì, số lượng, tình trạng (Priority: P1)

**Goal**: Giao sử dụng, thu lại, chuyển giữ, tách đồ khi bàn giao một phần; trạng thái đồ suy ra từ bản ghi hiện hành; đính chính có giới hạn và tính lại đồ; nhắc khi hết ca, chuyển tầng; `DoGuiBanGiaoPort` cho 008 (FR-011 → FR-014a, FR-028, BR-M12-01, Q-160).

**Independent Test**: Điện thoại của A Đang giữ: giao A sử dụng, thu lại, chuyển điều dưỡng khác giữ tại kho tầng; ghi sai số lượng rồi đính chính.

### Tests cho User Story 2

- [ ] T012 [P] [US2] Test `BT/belongings/BR_M12_01_BanGiaoIT.java`, `FR_011b_PhienBanIT.java`, `FR_011a_DinhChinhIT.java`: `UPDATE` bản ghi bị trigger chặn; hai bàn giao đồng thời cùng đồ chỉ một thành công; tách đồ khi bàn giao một phần; đính chính tính lại trạng thái đồ; hủy ghi nhận Tiếp nhận không có bản ghi sau → `bi_loai` (Q5 → Q10, Q27, Q28)

### Implementation cho User Story 2

- [ ] T013 [US2] Cài `BE/belongings/dogui/internal/BanGiaoService.java` (lệnh bàn giao theo bảng chuyển data-model mục 3; cập nhật cột suy ra cùng transaction; `TachDo`) và `DinhChinhDuocProvider` `M12.BAN_GIAO_DO_GUI` (không gắn tầng; giới hạn FR-011a; `sauKhiDinhChinh` tính lại đồ) (research K-04, K-05)
- [ ] T014 [US2] Cài `DoGuiBanGiaoPort` (`dangGiu`, `conDangGiu`) cho 008 và listener `CaDaKetThuc` (008), `DaChuyenGiuong` (003) nhắc trong `BE/belongings/dogui/internal/` (research K-11)
- [ ] T015 [US2] Tạo endpoint `POST /do-gui/ban-giao`, `GET /nhan-vien/{nhanVienId}/do-gui-dang-giu`; màn web `frontend/src/tinh-nang/do-gui/ban-giao/` (hiển thị chênh lệch tình trạng) với test Vitest

**Checkpoint**: Bàn giao chạy độc lập.

---

## Phase 5: User Story 3 – Đồ gửi chỉ được trả cho người có quyền (Priority: P1)

**Goal**: `NguoiNhanHopLe` (người đại diện, người có quyền đón tại thời điểm, người cao tuổi); xác nhận người nhận khác (một lần dùng, hết hạn CFG-M10-08); duyệt thay của Quản lý viện khi không liên hệ được hoặc không có đại diện; Điều dưỡng chỉ trả đồ không có giá trị; lần chặn trả (FR-015 → FR-019, BR-M12-04).

**Independent Test**: Trả điện thoại của A cho R (đại diện); cho N không có quyền đón (bị chặn); R xác nhận cho N qua cổng rồi trả.

### Tests cho User Story 3

- [ ] T016 [P] [US3] Test đơn vị `BT/belongings/NguoiNhanHopLeTest.java`, `DuBangChungLienHeTest.java` và `BT/belongings/BR_M12_04_TraDoIT.java`, `FR_017a_DuyetThayIT.java`: duyệt thay sau từ chối bị chặn; xác nhận dùng một lần; Trả trong lúc Kết thúc lưu trú (đồng thời) (Q14 → Q22)

### Implementation cho User Story 3

- [ ] T017 [US3] Tạo `MIG/V013_03__tra.sql`: `lan_tra` (nhóm 3; `nguoi_nhan_loai` `NGUOI_THAN|NGUOI_CAO_TUOI|NGUOI_KHAC`), `lan_chan_tra` (nhóm 3; `ly_do` 4 giá trị), `xac_nhan_nguoi_nhan` (`trang_thai` `CHO_XAC_NHAN|HIEU_LUC|TU_CHOI|HUY|DA_DUNG|HET_HAN`; `cach` `CONG|BAN_KY|QL_DUYET_THAY`; khóa sau khi lập), `xac_nhan_nguoi_nhan_do`, `bang_chung_lien_he` (nhóm 3) (data-model mục 5) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lan_tra`, `lan_chan_tra` → `ban_giao_do_gui` (`loai`); `xac_nhan_nguoi_nhan` → `ho_so_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T018 [US3] Cài `BE/belongings/tra/internal/TraService.java` (lệnh Trả theo `NguoiNhanHopLe` qua `QuyenNguoiThanApi`; lần chặn) và `XacNhanNguoiNhanService.java` (lập, xác nhận, từ chối, duyệt thay, hủy; việc theo phút Hết hạn; báo Quản lý viện khi đủ điều kiện duyệt thay) (research K-06)
- [ ] T019 [US3] Tạo endpoint `POST /do-gui/tra`, `/xac-nhan-nguoi-nhan`, `/{id}/bang-chung-lien-he`, `/{id}/{lenh}`, `/cong/xac-nhan-nguoi-nhan/{id}/{lenh}`; màn web `frontend/src/tinh-nang/do-gui/tra/`; app người thân xác nhận người nhận khác với test flutter

**Checkpoint**: Trả đồ an toàn.

---

## Phase 6: User Story 4 – Thất lạc, hư hỏng báo người liên hệ chính; người ghi quyết định ghi sự cố (Priority: P1)

**Goal**: Báo thất lạc, ghi hư hỏng, tìm thấy; một thông báo tới người liên hệ chính và Hành chính; người ghi chọn ghi sự cố mới qua `SuCoLenhApi.ghiNhan`, liên kết sự cố có sẵn, hoặc không (FR-020 → FR-025, BR-M12-02, Q-307).

**Independent Test**: Thất lạc điện thoại của A; hư hỏng kính của B có ảnh → mỗi lần đúng một thông báo; không tự tạo sự cố.

### Tests cho User Story 4

- [ ] T020 [P] [US4] Test `BT/belongings/BR_M12_02_ThatLacHuHongIT.java`, `SC_012_ThongBaoTest.java`: ghi sự cố trong cùng transaction khi chọn; liên kết sau một lần (`su_co_id` từ `NULL`); tìm thấy thêm diễn biến vào sự cố; không tự hủy sự cố khi hủy ghi nhận (Q23 → Q26)

### Implementation cho User Story 4

- [ ] T021 [US4] Cài `BE/belongings/suco/internal/SuCoDoGuiService.java` (lựa chọn của người ghi; `SuCoLenhApi.ghiNhan`, `themDienBien` MANDATORY; thông báo) và endpoint `POST /ban-giao-do-gui/{id}/lien-ket-su-co` (research K-07)

**Checkpoint**: P1 hoàn tất.

---

## Phase 7: User Story 5 – Kết thúc lưu trú bị chặn khi còn đồ chưa trả (Priority: P2)

**Goal**: Điều kiện `KT_B_DO_GUI`; mục "đồ gửi" sau qua đời; đề nghị xử lý đồ không người nhận qua yêu cầu phê duyệt `M12.XU_LY_DO_KHONG_NGUOI_NHAN` (không tự duyệt); nhắc CFG-M12-02; kiểm kê đồ có giá trị theo chu kỳ (FR-025a → FR-028b, BR-M12-03, 07, 08).

**Independent Test**: Hồ sơ kết thúc cho A còn 2 đồ Đang giữ, 1 Đang sử dụng → điều kiện Chưa đạt kèm 3 đồ; trả dần → Đạt.

### Tests cho User Story 5

- [ ] T022 [P] [US5] Test đơn vị `BT/belongings/DieuKienDoGuiTest.java`, `MucDoGuiSauQuaDoiTest.java` và `BT/belongings/BR_M12_03_DieuKienKetThucIT.java`, `FR_026_ViecSauQuaDoiIT.java`, `BR_M12_08_XuLyDoIT.java` (Q29 → Q36)
- [ ] T023 [P] [US5] Test `BT/belongings/BR_M12_07_KiemKeIT.java`: `UNIQUE (vi_tri_id, ma_ky)`; nhắc; hoàn thành phát sinh bản ghi bàn giao cho lệch; không có danh sách vị trí "gợi ý" (Q38 → Q41)

### Implementation cho User Story 5

- [ ] T024 [US5] Cài `BE/belongings/ketthuc/internal/DieuKienDoGui.java`, bean `ThamGiaChuyenTrangThai` (`@Order` sau 012; `KT_B_DO_GUI` `coTheMien = true`), `MucViecSauQuaDoiPort` `DO_GUI`, handler `M12.XU_LY_DO_KHONG_NGUOI_NHAN` (`choTuDuyet = false`; `apDung` ghi bản ghi `XU_LY`); listener `HoSoKetThucDaLap`, `NguoiCaoTuoiDaChuyenTrangThai`; nhắc CFG-M12-02 (research K-08, K-09)
- [ ] T025 [US5] Tạo bảng `phieu_kiem_ke` (`trang_thai` `CHO_KIEM_KE|HOAN_THANH|DA_HUY`; `UNIQUE (vi_tri_id, ma_ky)`), `phieu_kiem_ke_dong`, `do_khong_ro_chu` trong `MIG/V013_04__kiem_ke.sql`; cài `BE/belongings/kiemke/internal/KiemKeService.java` (Hành chính lập; hoàn thành phát sinh bản ghi; `DinhChinhDuocProvider` `M12.PHIEU_KIEM_KE`; nhắc chu kỳ) (research K-10) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `phieu_kiem_ke` → `ho_so_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T026 [US5] Tạo endpoint `GET /nguoi-cao-tuoi/{nctId}/do-gui/dieu-kien-ket-thuc`, `/kiem-ke`, `/{id}`, `/{id}/{lenh}`; màn web `frontend/src/tinh-nang/do-gui/kiem-ke/`, `frontend/src/tinh-nang/do-gui/xu-ly-do/`

**Checkpoint**: Kết thúc và kiểm kê chạy độc lập.

---

## Phase 8: User Story 6 – Người thân xem đồ gửi và lịch sử bàn giao trên cổng (Priority: P3)

**Goal**: Chỉ người đại diện và người có quyền đón thấy; kiểm `QuyenNguoiThanApi.xemDoGui` mỗi lần gọi (FR-030, BR-M12-09).

**Independent Test**: A có 3 đồ; R (đại diện), M (quyền đón) thấy danh sách và lịch sử; N không thấy.

### Tests cho User Story 6

- [ ] T027 [P] [US6] Test `BT/belongings/BR_M12_09_CongDoGuiIT.java` (Q42, Q43)

### Implementation cho User Story 6

- [ ] T028 [US6] Tạo endpoint `GET /cong/nguoi-cao-tuoi/{nctId}/do-gui` trong `BE/belongings/dogui/web/CongDoGuiController.java`; màn web `frontend/src/tinh-nang/cong-nguoi-than/do-gui/`, app `mobile/lib/nguoi-than/do-gui/` (research K-12)

**Checkpoint**: Mọi user story của 013 chạy độc lập.

---

## Phase 9: Polish & Cross-Cutting

- [ ] T033 [US6] *(Bổ sung 2026-10-18, Q-366)* Cài `BE/belongings/dogui/internal/DoGuiKenhNctPortImpl.java` và thêm AC-16 chủ đồ vào `QuyenTepProvider` `M12.ANH_DO_GUI`; test `BT/belongings/FR_030a_DoGuiKenhNguoiCaoTuoiIT.java` (quickstart G1; research K-16)

- [ ] T029 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/013-personal-belongings/contracts/api-013.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho `ban_giao_do_gui`, `lan_tra` (BR-M15-03) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lan_tra` → `ban_giao_do_gui` (`loai`); ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T030 Thay bản giả `DoGuiBanGiaoPort` (008), `MucViecSauQuaDoiPort` `DO_GUI` (004) bằng bản cài của 013 và chạy lại test 004, 008
- [ ] T031 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T032 Rà truy vết: DBR-22, 23, 25 và BR-M12-01 → 09 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-001, FR-001a, FR-002, FR-026, FR-026a, FR-027, FR-027a, FR-028a, FR-028c, FR-029, FR-031, FR-031a, FR-032 — mỗi FR có task hoặc test tương ứng (constitution XV)
- [ ] T034 *(bổ sung 2026-10-29, research R-15 bản 2 của 000)* Khi viết migration và entity của feature, áp data-model mục 0: không tạo các bảng đã gộp, dùng bảng gộp, cột `JSON` và bảng chung (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`); test bảo vệ nhóm dữ liệu viết cho bảng gộp

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US1 (P1)** → **US2 (P1)** → **US3, US4 (P1)** song song
- **US5 (P2)**: sau US3
- **US6 (P3)**: sau US2

### Parallel Opportunities

- T002, T004, T007 song song; US3 và US4 song song; US6 song song với US5

## Parallel Example: User Story 3

```text
Task: "T016 NguoiNhanHopLeTest + DuBangChungLienHeTest + BR_M12_04 + FR_017a"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2 → US3
2. **Dừng và kiểm chứng**: tiếp nhận → bàn giao → trả đúng người
3. Thêm US4, US5

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6; mỗi bước chạy `./mvnw verify`.

## Notes

- 013 không tạo sự cố, phiếu kiểm kê, không gợi ý vị trí kiểm kê (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M12-*, CFG-M10-08)
- Thao tác đồ gửi trên app chỉ trực tuyến
