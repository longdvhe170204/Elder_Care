---
description: "Danh sách task triển khai feature 019 – Mua sắm, tài sản và lịch xe (kho nguyên liệu bỏ theo Q-392)"
---

# Tasks: Tài sản, lịch xe và mua sắm của viện (019; kho nguyên liệu bỏ theo Q-392)

**Input**: Tài liệu thiết kế ở `specs/019-inventory-assets/` (plan.md, spec.md, research.md W-01 → W-09, data-model.md, contracts/api-019.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 003 (port `GiuongTheoTaiSanPort`, khai báo `TaiSanGiuongPort`), 008 (`NhiemVuApi.boPhanCua`, `laPhuTrachBoPhan`, bộ phận), 010 (gói `billing`), 011 (port `TonKhoNguyenLieuPort`, `LichBuaApi` không còn cần, Q-392), 014 (lệnh chuyến đi gọi `LichXeLenhApi`), 006 (`DonThuocApi.donNguonVienChuaKetThuc`, `TiepNhanThuocCungUngApi`, SPI `YeuCauThuocCungUngPort` ở `medication.api`; bổ sung 2026-10-11, Q-337) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/eldercare/backend/`, `BT/` = `backend/src/test/java/com/eldercare/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Ba gói: `BE/billing/muasam/`, `BE/roombedallocation/taisan/`, `BE/roombedallocation/lichxe/` (mỗi gói `{internal, web, lich}`); bổ sung `BE/roombedallocation/api/`, `BE/billing/api/`
- Web `frontend/src/tinh-nang/{mua-sam,tai-san,lich-xe}/`

---

## Phase 1: Setup

- [ ] T001 Tạo ba gói `BE/billing/muasam/`, `BE/roombedallocation/taisan/`, `BE/roombedallocation/lichxe/`; ArchUnit `BT/roombedallocation/TaiSanArchTest.java`: chỉ `roombedallocation.taisan` dùng port giường; job không tạo phiếu, yêu cầu, lịch (Q-306); M03 không phụ thuộc M11 (research W-01, W-02)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo giao diện mới ở `roombedallocation.api`, `billing.api`; khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T002 [P] Tạo `BE/roombedallocation/api/`: SPI `YeuCauMuaSamPort` (M11 cài), `LichXeApi`, `LichXeLenhApi`, `TaiSanThongKeApi`; `BE/billing/api/`: `MuaSamThongKeApi`; thông báo (cài `CoYeuCauThongBao`) (contracts mục 2.1 → 2.3)
- [ ] T003 Tạo bảng kỹ thuật `nhac_019` trong `MIG/V019_01__nhac.sql` và khung job (`BE/billing/muasam/lich/`, `BE/roombedallocation/taisan/lich/`, `BE/roombedallocation/lichxe/lich/`; ShedLock, chạy bù, không nhắc trùng) (research W-09) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `nhac_019` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T004 [P] Cài provider: `PhanLoaiTruongProvider` (nguyên giá, chi phí → `TAI_CHINH`), `LuuTruProvider`, `GanPhamViProvider` (`M11.MUA_SAM` bộ phận; `M03.TAI_SAN` bộ phận và tầng của vị trí; `M03.LICH_XE` toàn viện), `QuyenTepProvider` (`M11.CHUNG_TU_MUA_SAM`) (research W-08)

**Checkpoint**: Nền 019 sẵn sàng.

---

## Phase 3: User Story 7 – Bộ phận lập yêu cầu mua sắm; Quản lý viện duyệt chi phí; Kế toán đối chiếu (Priority: P1) 🎯 MVP

**Goal**: Yêu cầu mua sắm của bộ phận (Phụ trách bộ phận); Quản lý viện duyệt có thể giảm; ghi đã mua; Kế toán (khác người ghi đã mua) đối chiếu, khớp, lệch; vượt theo CFG-M11-07; gắn nguồn (phiếu nhập, tài sản, bảo trì thuê ngoài) một nguồn một yêu cầu; nhắc CFG-M11-08, 09 (FR-008 → FR-008c, BR-M11-17 → 20, DBR-36, Q-284).

**Independent Test**: Bếp lập 2.000.000 đồng; Quản lý viện duyệt giảm còn 1.800.000; ghi đã mua 1.950.000 → lệch; Kế toán ghi lệch; Quản lý viện chấp nhận.

### Tests cho User Story 7

- [ ] T005 [P] [US7] Test đơn vị `BT/billing/KiemTraVuotTest.java`, `KhopChungTuTest.java` và `BT/billing/BR_M11_17_MuaSamIT.java`, `BR_M11_18_VuotIT.java`, `BR_M11_19_DoiChieuIT.java`, `BR_M11_20_NhacIT.java`, `DBR_36_GanNguonIT.java` (Q1 → Q9)

### Implementation cho User Story 7

- [ ] T006 [US7] Tạo `MIG/V019_02__mua_sam.sql`: `yeu_cau_mua_sam` (`loai` `NGUYEN_LIEU|TAI_SAN|DICH_VU|THUOC|KHAC`; `muc_uu_tien`, `thoi_diem_can` (`CHECK` bắt buộc khi `THUOC`), `tep_lien_quan_ids` (không bắt buộc); dòng thêm `nct_id`, `don_thuoc_id`, `hoat_chat`, `ham_luong`, `dang_thuoc`, `don_vi` (FR-008d, Q-337); `trang_thai` `NHAP|CHO_DUYET|DA_DUYET|TU_CHOI|CHO_DOI_CHIEU|LECH|DA_DOI_CHIEU|DA_HUY`), `yeu_cau_mua_sam_dong`, `mua_sam_gan_nguon` (nhóm 3; `loai_nguon` `PHIEU_NHAP|TAI_SAN|BAO_TRI`; `UNIQUE (loai_nguon, nguon_id)`) (data-model mục 3) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `mua_sam_gan_nguon` → cột `JSON` của yêu cầu mua sắm; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T007 [US7] Cài `BE/billing/muasam/internal/MuaSamService.java` (bảng chuyển data-model mục 3; quyền Phụ trách bộ phận qua `NhiemVuApi`; Điều dưỡng chỉ lập, gửi, rút lại, hủy loại `THUOC`, dòng gắn đơn qua `DonThuocApi.donNguonVienChuaKetThuc` và người cao tuổi trong phạm vi phân công; mọi người lập loại `THUOC` phải xem được `SUC_KHOE` của người cao tuổi của dòng (`QuyenXemTruongApi`) mới gắn được đơn (research W-05); Kế toán không lập, người đối chiếu ≠ người ghi đã mua; loại `THUOC` đối chiếu chỉ chứng từ với số thực tế), `YeuCauMuaSamApi` cho M08, `YeuCauMuaSamPortImpl` cho M03 (kiểm yêu cầu Đã duyệt cùng bộ phận khi gắn nguồn); nhắc trong job (research W-05)
- [ ] T027 [US7] Cài `BE/billing/muasam/internal/YeuCauThuocCungUngPortImpl.java` (SPI `medication.api.YeuCauThuocCungUngPort`, research W-05) và hiển thị "Đã nhận" từ `TiepNhanThuocCungUngApi.theoYeuCau` trên chi tiết yêu cầu (bổ sung 2026-10-11, Q-337)
- [ ] T028 [P] [US7] Test `BT/billing/FR_008d_YeuCauThuocIT.java`: Điều dưỡng không là Phụ trách bộ phận lập yêu cầu `THUOC` gắn đơn nguồn viện Hiệu lực của người cao tuổi trong phạm vi → 201, gửi duyệt → Chờ duyệt và báo Quản lý viện; dòng gắn đơn gia đình gửi hoặc đơn Đã ngừng → 409; người cao tuổi ngoài phạm vi phân công → 403; thiếu mức ưu tiên hoặc thời điểm cần → 422; Điều dưỡng lập loại `NGUYEN_LIEU` → 403; Điều dưỡng ghi đã mua → 403; Phụ trách bộ phận Bếp (không xem được `SUC_KHOE`) lập yêu cầu `THUOC` gắn đơn → 403 và không được cấp thêm quyền xem; Phụ trách bộ phận Y tế xem được thì lập được; dòng thiếu `nctId` hoặc `donThuocId` → 422; đơn không thuộc người cao tuổi của dòng → 409; `YeuCauThuocCungUngPort` trả `boPhanId` của yêu cầu, không đổi khi người lập chuyển bộ phận; sau khi Phụ trách bộ phận ghi đã mua, `YeuCauThuocCungUngPort` trả dòng; trước đó trả rỗng; không có khoản chi phí người cao tuổi (spec 019 US7 #8; FR-008d, BR-M11-17, Q-337)
- [ ] T008 [US7] Tạo endpoint `/mua-sam`, `/mua-sam/{id}`, `/mua-sam/{id}/{lenh}`; màn web `frontend/src/tinh-nang/mua-sam/`

**Checkpoint**: Mua sắm chạy độc lập.

---

## Phase 4 → 6: User Story 1 → 3 – Kho nguyên liệu *(Bỏ theo Q-392)*

T009 → T015 bỏ: hệ thống không quản lý kho nguyên liệu (nhập, xuất, lô, kiểm kê, cảnh báo hạn dùng, tồn tối thiểu). Giữ mã task để không đánh số lại.

---

## Phase 7: User Story 4 – Phụ trách bộ phận quản lý tài sản, bảo trì và báo hỏng (Priority: P2)

**Goal**: Ghi tăng (nguyên giá bắt buộc khi gắn yêu cầu mua sắm, Q-294), đưa vào sử dụng, vị trí, chuyển bộ phận (Quản lý viện), bảo trì/kiểm định/đăng kiểm/bảo hiểm, báo hỏng, thanh lý qua yêu cầu `M03.THANH_LY_TAI_SAN` (không tự duyệt); dấu quá hạn theo ngày (FR-011 → FR-016, BR-M03-17, BR-M03-18).

**Independent Test**: Ghi tăng một xe và một máy tạo oxy; báo hỏng máy tạo oxy; bảo trì Đạt; qua mốc hạn đăng kiểm của xe; thanh lý một tài sản.

### Tests cho User Story 4

- [ ] T016 [P] [US4] Test `BT/roombedallocation/BR_M03_17_HanBaoTriIT.java`, `BR_M03_18_HuHongIT.java` (Q18 → Q21, Q24 → Q26)

### Implementation cho User Story 4

- [ ] T017 [US4] Tạo `MIG/V019_05__tai_san.sql`: `loai_tai_san`, `tai_san` (`ma UNIQUE`; `nguon` `MUA|TANG|CHUYEN|GIUONG_003`; `giuong_id NULL UNIQUE`; `trang_thai` `SAN_SANG|DANG_SU_DUNG|HONG|DANG_BAO_TRI|DA_THANH_LY`; dấu quá hạn), `tai_san_vi_tri` (nhóm 3; cột sinh `khoa_mo` `UNIQUE`), `tai_san_bo_phan`, `tai_san_bao_tri` (`loai` `BAO_TRI|KIEM_DINH|DANG_KIEM|BAO_HIEM`; `ket_qua` `DAT|KHONG_DAT` đặt một lần), `tai_san_hu_hong` (`muc_anh_huong` `MAT_AN_TOAN|KHONG_MAT_AN_TOAN`) (data-model mục 4) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `tai_san_vi_tri`, `tai_san_bo_phan` → `lich_su_chung`; `tai_san_bao_tri`, `tai_san_hu_hong` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T018 [US4] Cài `BE/roombedallocation/taisan/internal/TaiSanService.java` (bảng chuyển; gắn nguồn mua sắm qua `YeuCauMuaSamPort`; bảo trì thuê ngoài gắn yêu cầu), handler `M03.THANH_LY_TAI_SAN` (`choTuDuyet = false`), `DinhChinhDuocProvider` `M03.TAI_SAN_LICH_SU`, job dấu quá hạn; endpoint `/tai-san`, `/tai-san/loai`, `/tai-san/{id}`, `/tai-san/{id}/{lenh}`; màn web `frontend/src/tinh-nang/tai-san/` (research W-06)

---

## Phase 8: User Story 5 – Giường là tài sản: trạng thái đi cùng trạng thái giường (Priority: P2)

**Goal**: Tài sản giường tạo kèm lệnh Tạo giường (`TaiSanGiuongPort.ghiTangChoGiuong`); báo hỏng/bảo trì/thanh lý giường đi qua `GiuongTheoTaiSanPort` cùng transaction; giường có người → dấu "chờ chuyển người"; mất an toàn gửi Khẩn cấp (FR-011, FR-015, BR-M03-15, DBR-32).

**Independent Test**: G5 trống và G6 có người; báo hỏng cả hai → G5 Đang bảo trì, G6 giữ nguyên có dấu chờ chuyển người; kết quả bảo trì Đạt cho G5 → Trống.

### Tests cho User Story 5

- [ ] T019 [P] [US5] Test đơn vị `BT/roombedallocation/AnhXaTrangThaiGiuongTest.java` và `BT/roombedallocation/BR_M03_15_GiuongTaiSanIT.java` (SC-004), `DBR_32_TaiSanGiuongIT.java` (báo hỏng cùng lúc với phân bổ) (Q20 → Q23)

### Implementation cho User Story 5

- [ ] T020 [US5] Cài `BE/roombedallocation/taisan/internal/TaiSanGiuongPortImpl.java` (`ghiTangChoGiuong`, `mangDauChoChuyenNguoi`, `giuongMangDau`) và `AnhXaTrangThaiGiuong` nối lệnh tài sản của giường với `GiuongTheoTaiSanPort` trong `TaiSanService.java`; màn báo hỏng giường hiện tiêu chí "mất an toàn" với test Vitest

---

## Phase 9: User Story 6 – Đặt lịch dùng xe đưa đón (Priority: P2)

**Goal**: Lịch xe không chồng giờ dưới khóa xe, xe không ở bảo trì/hỏng/thanh lý; `LichXeApi.coLichXe` cho điều kiện rời viện của 014; `LichXeLenhApi` chuyển trạng thái kèm lệnh 014 (FR-017, FR-018, BR-M03-16).

**Independent Test**: Hai lịch chồng giờ trên XE-01 (bị chặn); lịch cho xe đang bảo trì (bị chặn); điểm danh rời viện chuyến chưa có lịch xe (bị chặn ở 014).

### Tests cho User Story 6

- [ ] T021 [P] [US6] Test đơn vị `BT/roombedallocation/KiemTraDatLichTest.java` và `BT/roombedallocation/BR_M03_16_LichXeIT.java` (SC-005; hai lịch cùng giờ đồng thời), `FR_018_TheoChuyenIT.java` (Q27 → Q32)

### Implementation cho User Story 6

- [ ] T022 [US6] Tạo bảng `lich_xe` (`muc_dich` `CHUYEN_DI|DI_KHAM|CHUYEN_VIEN_KHONG_CAP_CUU|KHAC`; `trang_thai` `DA_DAT|DANG_DUNG|DA_HOAN_THANH|DA_HUY`) trong `MIG/V019_06__lich_xe.sql`; cài `BE/roombedallocation/lichxe/internal/LichXeService.java` (`KiemTraDatLich`; `LichXeApi`, `LichXeLenhApi`; báo người lái có tài khoản; `DinhChinhDuocProvider` `M03.LICH_XE`) và endpoint `/lich-xe`, `/lich-xe/{id}/{lenh}`; màn web `frontend/src/tinh-nang/lich-xe/` (research W-07) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lich_xe` → `ho_so_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**

**Checkpoint**: Mọi user story của 019 chạy độc lập.

---

## Phase 10: Polish & Cross-Cutting

- [ ] T023 [P] Cài `TaiSanThongKeApi`, `MuaSamThongKeApi` (016); mở rộng `BT/common/HopDongApiTest.java` với `specs/019-inventory-assets/contracts/api-019.yaml`; mở rộng `NhomGhiNhanCoTriggerIT`
- [ ] T024 Thay bản giả `TaiSanGiuongPort` (003 T002), `YeuCauThuocCungUngPort` (006 T062, bổ sung 2026-10-11) bằng bản cài của 019 và chạy lại test 003, 011, 014 (lịch xe), 006 US9
- [ ] T025 Chạy `./mvnw verify`, `npm test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T026 Rà truy vết: DBR-32, 36 và BR-M03-15 → 18, BR-M11-17 → 20, FR-008d (bổ sung 2026-10-11) có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-019, FR-020, FR-021 — mỗi FR có task hoặc test tương ứng (constitution XV)
- [ ] T029 *(bổ sung 2026-10-29, research R-15 bản 2 của 000)* Khi viết migration và entity của feature, áp data-model mục 0: không tạo các bảng đã gộp, dùng bảng gộp, cột `JSON` và bảng chung (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`); test bảo vệ nhóm dữ liệu viết cho bảng gộp

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US7 (P1)**; US1 → US3 bỏ theo Q-392
- **US4 → US5 (P2)**; **US6 (P2)**: sau US4 (xe là tài sản)

### Parallel Opportunities

- T002, T004 song song

## Parallel Example: P2

```text
Task: "T016 BR_M03_17 + BR_M03_18"
```

## Implementation Strategy

- MVP: Phase 1–2 → US7; kiểm chứng mua sắm
- Sau đó US4 → US5 (giường), US6 (lịch xe cho 014)

## Notes

- 019 không tạo yêu cầu mua sắm, phiếu, lịch xe; tài sản giường chỉ tạo kèm lệnh Tạo giường của 003 (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M03-02, 09, CFG-M11-07 → 09)
