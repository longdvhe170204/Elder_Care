---
description: "Danh sách task triển khai feature 014 – Hoạt động và chất lượng chăm sóc"
---

# Tasks: Quản lý hoạt động và chất lượng chăm sóc (014)

**Input**: Tài liệu thiết kế ở `specs/014-activity-care-quality/` (plan.md, spec.md, research.md L-01 → L-15, data-model.md, contracts/api-014.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 005 đã xong (014 thêm gói con vào `m04`, dùng dịch vụ nội bộ công việc, có mặt bán trú, công việc làm lại); 001 (`TrangThaiNguoiCaoTuoiApi` cho điểm danh rời viện/về), 003 (lịch xe ở `m03.api` do 019 cung cấp), 004, 006 (`MangThuocChuyenDiApi`, SPI `ChuyenDiThuocPort`), 007 (`SuCoLenhApi`, `KhoanhVungApi`, `DiemDanhTruyVetPort`), 008 (`CaTrucApi`, `ChuyenDiNhiemVuPort`), 010 (`SuKienTinhPhiNguon`), 011 (`ChuyenDiBuaAnPort`), 012 (`QuyenNguoiThanApi`, `HoatDongBanTinPort`) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Dùng ở giai đoạn triển khai; CLAUDE.md hiện chặn `/speckit-implement`.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/vn/eldercare/`, `BT/` = `backend/src/test/java/vn/eldercare/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M04 (phần 014): `BE/m04/{hoatdong,buoi,chuyendi,tinhthan,kiemtra}/`; bổ sung `BE/m04/api/`
- Web `web/src/tinh-nang/hoat-dong/{danh-muc,buoi,chuyen-di,tinh-than,kiem-tra-chat-luong}/`, `web/src/tinh-nang/cong-nguoi-than/hoat-dong/`; app nhân viên `app-nhan-vien/lib/tinh-nang/hoat-dong/`; app người thân `app-nguoi-than/lib/tinh-nang/hoat-dong/`

---

## Phase 1: Setup

- [ ] T001 Tạo năm gói con `BE/m04/{hoatdong,buoi,chuyendi,tinhthan,kiemtra}/`; ArchUnit `BT/m04/M04HoatDongArchTest.java`: gói `..lich..` không tạo buổi, danh sách, mục, công việc làm lại, sự cố (Q-306, Q-307) (research L-01)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Giao diện mới ở `m04.api`, danh mục, hoạt động, mẫu lặp, khung job, listener.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T002 [P] Bổ sung `BE/m04/api/`: `ChuyenDiNhiemVuLenhApi`, `ChuyenDiThayDoi`, `HoatDongThongKeApi`, sự kiện `BuoiDaHuy`, `BuoiDaDoi`, `BuoiDaKetThuc`; khai báo sự kiện phát cho module khác (`m05.api.YeuCauTaoCanhBao`, `YeuCauGhiDienBienCanhBao`, `m11.api.SuKienTinhPhiNguon`, `m08.api.ChuyenDiDaDoi`) (contracts mục 1.2, 1.3; research L-11)
- [ ] T003 Tạo `MIG/V014_01__hoat_dong.sql`: `hoat_dong_nct` (`nct_id` khóa chính), `loai_hoat_dong` (`hoat_dong_nhom`, `ngoai_vien`), `nhom_so_thich`, `loai_hoat_dong_nhom_so_thich`, `hoat_dong` (`pham_vi` `TANG|TOAN_VIEN`; giới hạn số lượng; thu phí – dịch vụ 004), `mau_lap_phien_ban` (có lịch sử), `nguoi_thuong_xuyen` (cột sinh `UNIQUE` bản đang hiệu lực), `su_kien_m04hd_da_xu_ly`, `lan_chay_m04hd` (data-model mục 2)
- [ ] T004 Cài danh mục và hoạt động (Trưởng tầng lập; mẫu đổi theo phiên bản) trong `BE/m04/hoatdong/internal/HoatDongService.java` và controller `/hoat-dong/loai`, `/hoat-dong`, `/{id}`, `/{id}/{lenh}` (research L-04)
- [ ] T005 Cài khung job `BE/m04/hoatdong/lich/ViecTheoPhutM04HoatDong.java`, `ViecHangNgayM04HoatDong.java` (00:00; ShedLock, chạy bù) và test `BT/m04/ChayBuM04HoatDongIT.java` (research L-15)
- [ ] T006 [P] Cài provider: `GanPhamViProvider` (`M04.BUOI`, `M04.DANG_KY`, `M04.CHUYEN_DI`, `M04.SO_THICH`, `M04.KIEM_TRA_CHAT_LUONG`), `PhanLoaiTruongProvider`, `LuuTruProvider`; `ThoiKhoaBieuNguonPort` (005) cho buổi, chuyến trong ngày

**Checkpoint**: Nền 014 sẵn sàng.

---

## Phase 3: User Story 1 – Khai báo hoạt động định kỳ và tạo buổi từ mẫu (Priority: P1) 🎯 MVP

**Goal**: Trưởng tầng tạo buổi từ mẫu bằng lệnh (kèm đăng ký sẵn người thường xuyên); buổi lẻ; dời; hủy; đổi mẫu hủy buổi tương lai; `UNIQUE (hoat_dong_id, bat_dau)` (FR-001 → FR-014a, BR-M04-21).

**Independent Test**: "Thể dục buổi sáng" 08:00 thứ 2 → thứ 6, tối đa 20 người, 3 người thường xuyên; Trưởng tầng tạo buổi 7 ngày tới → buổi đúng ngày, người thường xuyên đã đăng ký; không tạo trùng.

### Tests cho User Story 1

- [ ] T007 [P] [US1] Test đơn vị `BT/m04/BuoiTheoMauTest.java` và `BT/m04/BR_M04_21_TaoBuoiTuMauIT.java`, `FR_014a_HetNgayIT.java`: không tự tạo buổi; tạo lại không trùng; hết ngày → `DA_DIEM_DANH` (dấu) hoặc `KHONG_DIEM_DANH`, đăng ký chưa ghi → "Không ghi nhận" (Q1 → Q5)

### Implementation cho User Story 1

- [ ] T008 [US1] Tạo bảng `buoi_hoat_dong` (`UNIQUE (hoat_dong_id, bat_dau)` kể cả Đã hủy; `nguon` `MAU|LE`; `trang_thai` `DA_LEN_LICH|DA_DIEM_DANH|KHONG_DIEM_DANH|DA_HUY`), `dang_ky_buoi` (`nguon` `THUONG_XUYEN|THEM_TAY|THEM_LUC_DIEM_DANH`; `trang_thai` `DA_DANG_KY|DA_HUY|DA_DIEM_DANH`) trong `MIG/V014_02__buoi.sql`
- [ ] T009 [US1] Cài `BE/m04/buoi/internal/BuoiService.java` (tạo từ mẫu kèm đăng ký sẵn, buổi lẻ, dời, hủy; phát `BuoiDaHuy`, `BuoiDaDoi` cho phần công việc "hoạt động" của 005; việc hằng ngày hết ngày) và endpoint `/buoi`, `/buoi/{id}`, `/buoi/{id}/{lenh}`; màn web `web/src/tinh-nang/hoat-dong/buoi/` (research L-04)

**Checkpoint**: Buổi từ mẫu chạy độc lập.

---

## Phase 4: User Story 2 – Đăng ký có giới hạn số lượng và điều kiện sức khỏe (Priority: P1)

**Goal**: `DieuKienDangKy` (sức chứa dưới khóa buổi, khoanh vùng, nghi nhiễm, vắng mặt); đồng ý về muộn cho bán trú dời giờ về (005 FR-018a); hủy tự động khi khoanh vùng, nghi nhiễm, trạng thái cuối (FR-015 → FR-021, BR-M04-15).

**Independent Test**: Buổi tối đa 2: A, B rồi C (bị chặn); D nghi nhiễm vào hoạt động nhóm (bị chặn); khoanh vùng tầng → hủy đăng ký.

### Tests cho User Story 2

- [ ] T010 [P] [US2] Test đơn vị `BT/m04/DieuKienDangKyTest.java` và `BT/m04/BR_M04_15_DangKyIT.java` (hai đăng ký chỗ cuối đồng thời), `FR_016a_DongYVeMuonIT.java`, `FR_021_KhoanhVungIT.java` (Q6 → Q12)

### Implementation cho User Story 2

- [ ] T011 [US2] Tạo bảng `dong_y_ve_muon` (`cach` `CONG|BAN_KY`; `trang_thai` `HIEU_LUC|DA_RUT|...`) trong `MIG/V014_03__dong_y_ve_muon.sql`; cài `BE/m04/buoi/internal/DangKyService.java` (đăng ký, hủy, hủy tự động; listener `KhoanhVungDaTao`, `DauNghiNhiemDaDoi`) và `DongYVeMuonService.java` (người đại diện qua `QuyenNguoiThanApi`; dời `gio_ve_du_kien_hien_hanh` của 005); bean `ThamGiaChuyenTrangThai` (`@Order` sau 005; trạng thái cuối hủy đăng ký, gỡ thường xuyên) (research L-05)
- [ ] T012 [US2] Tạo endpoint `/buoi/{id}/dang-ky`, `/dang-ky/{id}/huy`, `/dong-y-ve-muon`, `/{id}/rut`, `/cong/dong-y-ve-muon`, `/{id}/rut`; màn web đăng ký (cảnh báo cần xác nhận) với test Vitest; app người thân đồng ý về muộn

**Checkpoint**: Đăng ký chạy độc lập.

---

## Phase 5: User Story 3 – Điểm danh buổi; lượt có mặt có thu phí là sự kiện tính phí (Priority: P1)

**Goal**: Điểm danh Có mặt/Vắng/Không ghi nhận; thêm người tại chỗ; ngoại tuyến; đính chính; sự kiện tính phí `DIEM_DANH_HOAT_DONG` cho 010; nhắc điểm danh CFG-M04-06 (FR-022 → FR-032, BR-M04-18).

**Independent Test**: "Tập đàn" thu phí 4 đăng ký; điểm danh 3 có mặt, 1 vắng; thêm 1 người tại chỗ → 4 sự kiện tính phí; đính chính một người từ có mặt sang vắng → sự kiện đổi.

### Tests cho User Story 3

- [ ] T013 [P] [US3] Test `BT/m04/BR_M04_18_SuKienTinhPhiIT.java`, `FR_031_DinhChinhIT.java`; app `app-nhan-vien/test/diem_danh_buoi_test.dart` (20 người trong thời gian SC-009) (Q13 → Q17)

### Implementation cho User Story 3

- [ ] T014 [US3] Tạo bảng `diem_danh_buoi` (nhóm 3; `ket_qua` `CO_MAT|VANG|KHONG_GHI_NHAN`; `ly_do_vang` 5 giá trị) trong `MIG/V014_04__diem_danh_buoi.sql`; cài `BE/m04/buoi/internal/DiemDanhBuoiService.java` (hoàn tất điểm danh; phát `SuKienTinhPhiNguon`/`DaDoi`; `NgoaiTuyenHandler` `M04.DIEM_DANH_BUOI`; `DinhChinhDuocProvider` `M04.DIEM_DANH_BUOI`; `NguonCongViecPort` `BUOI_HOAT_DONG`; `DiemDanhTruyVetPort` cho 007; `HoatDongBanTinPort` cho 012; nhắc CFG-M04-06) và endpoint `POST /buoi/{id}/diem-danh`; app `app-nhan-vien/lib/tinh-nang/hoat-dong/` (research L-06)

**Checkpoint**: Điểm danh chạy độc lập.

---

## Phase 6: User Story 4 – Chuẩn bị và điểm danh rời viện cho hoạt động ngoài viện (Priority: P1)

**Goal**: Chuyến đi: phân công trưởng đoàn và người đi cùng (`ChuyenDiNhiemVuPort` cho 008/002); đánh giá khả năng (Trưởng tầng của tầng người đó); thuốc mang theo (006); lịch xe (019 qua `m03.api`); điều kiện rời viện; điểm danh rời viện gọi `TrangThaiNguoiCaoTuoiApi.thucHien(DIEM_DANH_ROI_VIEN)` trong cùng transaction (FR-033 → FR-040, BR-M04-16).

**Independent Test**: "Tham quan chùa" 07:00–13:00 với 4 đăng ký; trưởng đoàn và người đi cùng; 3 Đạt, 1 Không đạt; A có hai liều trong chuyến → thuốc cần mang; rời viện chặn khi chưa chuẩn bị thuốc.

### Tests cho User Story 4

- [ ] T015 [P] [US4] Test `BT/m04/BR_M04_16_RoiVienIT.java`, `FR_035_NhiemVuIT.java`: rời viện từ hai thiết bị đồng thời; điều kiện (Đạt, thuốc `MangThuocChuyenDiApi.daChuanBi`, lịch xe); 001 chuyển `HOAT_DONG_BEN_NGOAI` cùng transaction, lỗi thì rollback; nghi nhiễm → `KHONG_DI` và báo trưởng đoàn (Q18 → Q25)

### Implementation cho User Story 4

- [ ] T016 [US4] Tạo `MIG/V014_05__chuyen_di.sql`: `chuyen_di` (`buoi_id` khóa chính; `trang_thai` `DA_LEN_LICH|DANG_DI|DA_VE|DA_HUY`), `chuyen_di_gia_han` (nhóm 3), `nhiem_vu_chuyen` (`vai` `TRUONG_DOAN|DI_CUNG`), `nguoi_tham_gia` (`trang_thai` 7 giá trị), `danh_gia_kha_nang` (nhóm 3; `ket_qua` `DAT|KHONG_DAT`), `diem_danh_chuyen` (nhóm 3; `loai` `ROI_VIEN|VE`; `thoi_diem_thiet_bi`) (data-model mục 6)
- [ ] T017 [US4] Cài `BE/m04/chuyendi/internal/ChuyenDiService.java` (phân công, đánh giá, rời viện; phát `ChuyenDiDaDoi`, `ChuyenDiThayDoi`; `ChuyenDiNhiemVuLenhApi` cho 015; `ChuyenDiThuocPort` (006), `ChuyenDiNhiemVuPort` (008), `ChuyenDiBuaAnPort` (011)) (research L-07, L-08)
- [ ] T018 [US4] Tạo endpoint `/chuyen-di/{id}`, `/{id}/phan-cong`, `/{id}/danh-gia`, `/{id}/{lenh}`; màn web `web/src/tinh-nang/hoat-dong/chuyen-di/`; app phần chuyến đi

**Checkpoint**: Chuẩn bị chuyến đi chạy độc lập.

---

## Phase 7: User Story 5 – Theo dõi chuyến đi, điểm danh về, quá giờ và thiếu người (Priority: P1)

**Goal**: Gia hạn giờ về; về từng phần; báo thiếu người (sự cố qua `SuCoLenhApi`, một lần); kết thúc điểm danh về khi còn người chưa về → cảnh báo Khẩn cấp; quá giờ về báo một lần mỗi mốc; chuyển trưởng đoàn; hủy ghi nhận rời viện/về; về muộn (FR-041 → FR-050, BR-M04-17).

**Independent Test**: A, B, C rời 07:05, về dự kiến 13:00; quá 13:30 → cảnh báo; gia hạn; về A, B, kết thúc điểm danh khi C chưa về → sự cố, cảnh báo Khẩn cấp.

### Tests cho User Story 5

- [ ] T019 [P] [US5] Test `BT/m04/BR_M04_17_ThieuNguoiIT.java`, `FR_031a_DinhChinhChuyenIT.java`: hai lệnh Báo thiếu người đồng thời → một sự cố; Báo thiếu người chặn khi mất mạng (app); đính chính rời viện/về gọi `MangThuocChuyenDiApi.tinhLaiKhoang` và 001 `HUY_GHI_NHAN_ROI_VIEN` (Q26 → Q33)

### Implementation cho User Story 5

- [ ] T020 [US5] Hoàn thiện `ChuyenDiService.java`: điểm danh về (001 `DIEM_DANH_VE`), gia hạn, chuyển trưởng đoàn, báo thiếu người (`SuCoLenhApi.ghiNhan` MANDATORY), kết thúc về (`YeuCauTaoCanhBao` Khẩn cấp), về muộn, hủy ghi nhận; việc theo phút quá giờ về (một lần mỗi mốc, báo Quản lý viện); `NgoaiTuyenHandler` `M04.DIEM_DANH_ROI_VIEN`, `M04.DIEM_DANH_VE` (lệnh thiếu người bắt buộc trực tuyến); `DinhChinhDuocProvider` `M04.DIEM_DANH_CHUYEN` (research L-08)

**Checkpoint**: P1 hoàn tất.

---

## Phase 8: User Story 6 – Tinh thần, sở thích và cảnh báo nguy cơ cô lập (Priority: P2)

**Goal**: Sở thích, hồ sơ tinh thần; ngày tính và chuỗi cô lập; cảnh báo Nhẹ qua 007, không gợi ý (FR-051 → FR-056, BR-M04-22).

**Independent Test**: A sở thích "âm nhạc", "cờ tướng"; 7 ngày không có mặt ở hoạt động nhóm → một cảnh báo nhẹ, không gợi ý.

### Tests cho User Story 6

- [ ] T021 [P] [US6] Test đơn vị `BT/m04/NgayTinhTest.java`, `CapNhatChuoiTest.java`, `TyLeThamGiaTest.java` và `BT/m04/BR_M04_22_CoLapIT.java` (30 kịch bản, SC-007), `FR_051_SoThichIT.java` (Q34 → Q36)

### Implementation cho User Story 6

- [ ] T022 [US6] Tạo bảng `so_thich` (`muc` `THICH|KHONG_THICH`; `nguon` 3 giá trị; `trang_thai`), `chuoi_co_lap` (kỹ thuật) trong `MIG/V014_06__tinh_than.sql`; cài `BE/m04/tinhthan/internal/TinhThanService.java` (sở thích, hồ sơ tinh thần; việc 00:00 cập nhật chuỗi, `YeuCauTaoCanhBao` `NGUY_CO_CO_LAP` Nhẹ một lần mỗi chuỗi) và endpoint `/nguoi-cao-tuoi/{nctId}/so-thich`, `/so-thich/{id}/bo`, `/nguoi-cao-tuoi/{nctId}/ho-so-tinh-than`; màn web `web/src/tinh-nang/hoat-dong/tinh-than/` (research L-09)

**Checkpoint**: Tinh thần chạy độc lập.

---

## Phase 9: User Story 7 – Người kiểm tra lập danh sách kiểm tra chất lượng (Priority: P2)

**Goal**: Người kiểm tra (Trưởng tầng hoặc Người phụ trách ca) tự chọn công việc đã hoàn thành đủ tỷ lệ; ghi Đạt/Không đạt/Không kiểm tra được; Không đạt kèm lập công việc làm lại (005); hết ca → quá hạn; ca không có danh sách ghi lại (FR-057 → FR-066, BR-M04-23).

**Independent Test**: Ca ngày tầng 2 có 200 công việc hoàn thành; T lập danh sách thiếu tỷ lệ (bị chặn), rồi đủ 10; Không đạt một mục → công việc làm lại.

### Tests cho User Story 7

- [ ] T023 [P] [US7] Test đơn vị `BT/m04/CongViecDuDieuKienTest.java`, `SoMucToiThieuTest.java`, `NguoiKiemTraTest.java` và `BT/m04/BR_M04_23_KiemTraChatLuongIT.java`, `FR_066_NguoiKiemTraIT.java` (Q37 → Q43)

### Implementation cho User Story 7

- [ ] T024 [US7] Tạo bảng `danh_sach_kiem_tra` (`UNIQUE (ca_id, tang_khu_vuc_id)`), `muc_kiem_tra` (`cong_viec_id UNIQUE`; `trang_thai` `CHO_KIEM_TRA|DAT|KHONG_DAT|KHONG_KIEM_TRA_DUOC|QUA_HAN_KIEM_TRA`), `ca_khong_danh_sach` (nhóm 3) trong `MIG/V014_07__kiem_tra.sql`; cài `BE/m04/kiemtra/internal/KiemTraChatLuongService.java` (kiểm tỷ lệ khi lập; Không đạt gọi lệnh lập công việc làm lại của 005 cùng transaction; `KetQuaKiemTraPort`; `NguonCongViecPort` `LAM_LAI`; `DinhChinhDuocProvider` `M04.KET_QUA_KIEM_TRA`; việc theo phút hết ca) (research L-10)
- [ ] T025 [US7] Tạo endpoint `/kiem-tra-chat-luong/ca/{caId}`, `/muc-kiem-tra/{id}/ket-qua`; màn web `web/src/tinh-nang/hoat-dong/kiem-tra-chat-luong/` (không xếp hạng công việc) với test Vitest

**Checkpoint**: Mọi user story của 014 chạy độc lập.

---

## Phase 10: Polish & Cross-Cutting

- [ ] T026 [P] Cài cổng người thân `GET /cong/nguoi-cao-tuoi/{nctId}/hoat-dong` (`QuyenNguoiThanApi`) và test `BT/m04/FR_068_CongHoatDongIT.java`; app `app-nguoi-than/lib/tinh-nang/hoat-dong/` (research L-12)
- [ ] T027 [P] Cài `HoatDongThongKeApi` (016); mở rộng `BT/common/HopDongApiTest.java` với `specs/014-activity-care-quality/contracts/api-014.yaml`
- [ ] T028 Thay bản giả SPI 014 cài ở test 005, 006, 007, 008, 011, 012 (`NguonCongViecPort` `BUOI_HOAT_DONG`/`LAM_LAI`, `KetQuaKiemTraPort`, `ChuyenDiThuocPort`, `DiemDanhTruyVetPort`, `ChuyenDiNhiemVuPort`, `ChuyenDiBuaAnPort`, `HoatDongBanTinPort`) và chạy lại
- [ ] T029 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T030 Rà truy vết: DBR-12 (tương tự), 15, 18, 23, 25 và BR-M04-15 → 18, 21 → 23 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-021a, FR-067, FR-068, FR-069, FR-070, FR-071 — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US1 → US2 → US3 (P1)**; **US4 → US5 (P1)** sau US2
- **US6 (P2)**: sau US3 (điểm danh có mặt)
- **US7 (P2)**: sau Foundational (dùng công việc của 005)

### Parallel Opportunities

- T002, T006 song song; US4 song song với US3; US7 song song với US1 → US5

## Parallel Example: User Story 4

```text
Task: "T015 BR_M04_16_RoiVienIT + FR_035_NhiemVuIT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2 → US3
2. **Dừng và kiểm chứng**: buổi từ mẫu → đăng ký → điểm danh → sự kiện tính phí
3. Thêm US4, US5 (chuyến đi)

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6 → US7; mỗi bước chạy `./mvnw verify`.

## Notes

- 014 không tạo buổi, danh sách kiểm tra, mục, công việc làm lại, sự cố, khoản chi phí; không gợi ý (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M04-*)
- Báo thiếu người, kết thúc điểm danh về bắt buộc trực tuyến
