---
description: "Danh sách task triển khai feature 015 – Lịch ca xoay vòng, phủ ca, đổi ca và nghỉ đột xuất"
---

> **Sửa 2026-10-29 (Q-397):** bỏ mẫu xoay ca, nhóm xoay ca, lập lịch từ mẫu, phủ tối thiểu, thiếu ca, cảnh báo thiếu phủ, lời mời nhận ca; 015 chỉ còn đổi ca và nghỉ đột xuất. Nghỉ "theo ngày" mở cho mọi nhân viên, người lập chọn phạm vi. Phần dưới còn nhắc các phần đã bỏ thì bỏ qua; spec là căn cứ.

# Tasks: Đổi ca và nghỉ đột xuất (015)

**Input**: Tài liệu thiết kế ở `specs/015-shift-rotation-swap/` (plan.md, spec.md, research.md X-01 → X-13, data-model.md, contracts/api-015.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 008 đã xong (port nội bộ `CaLenhPort`, `PhanCongLenhPort`, sự kiện nội bộ M09, chỗ gọi `KiemTraCongBoMoRong`); 005 (`CongViecCaApi`), 014 (`ChuyenDiNhiemVuLenhApi`) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/eldercare/backend/`, `BT/` = `backend/src/test/java/com/eldercare/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M09 (phần 015): `BE/staffing/{xoayca,phuca,doica}/`; port bổ sung ở `BE/staffing/lichca/port/`
- Web `frontend/src/tinh-nang/lich-ca/{xoay-ca,phu-ca,doi-ca,nghi,loi-moi}/`; app nhân viên `mobile/lib/nhan-vien/lich-ca/`

---

## Phase 1: Setup

- [ ] T001 Tạo gói con `BE/staffing/doica/` (Q-397: bỏ `xoayca`, `phuca`); mở rộng ArchUnit `BT/staffing/M09ArchTest.java` (008 T001): chỉ `staffing.doica` dùng port nội bộ; gói `..lich..` không lập lịch, không chỉ định người phụ trách (research X-01)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Port bổ sung của 008, bảng mẫu, nhóm, hàm thuần xếp ca và tính phủ, khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T002 Bổ sung port trong `BE/staffing/lichca/port/`: `LichNhapLenhPort` (chỉ `goNhanVienKhoiNhap`, `lichThang`; Q-397), `NghiCoDuyetPort`; cài ở gói `staffing.lichca.internal` của 008 (contracts mục 1.2; research X-03)
- [ ] T003 [P] Bổ sung `BE/staffing/api/DoiCaThongKeApi.java` (yêu cầu, giờ làm theo tháng; Q-397: bỏ phủ, lời mời) và thông báo (cài `CoYeuCauThongBao`) (contracts mục 2.1, 2.3)
- [ ] T004 *(Bỏ theo Q-397, 2026-10-29)* Không có bảng mẫu xoay ca, nhóm, `moc_nhac_lich`; `lan_chay_m09xc` tạo ở T007 **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lan_chay_m09xc` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T005 [P] *(Bỏ theo Q-397, 2026-10-29)*
- [ ] T006 [P] *(Bỏ theo Q-397, 2026-10-29)*
- [ ] T007 Cài khung job `BE/staffing/doica/lich/ViecM09DoiCa.java` (ShedLock, `lan_chay_m09xc`) cho Hết hiệu lực yêu cầu, tách ca theo giờ (Q-397: bỏ nhắc lập lịch, nhắc công bố, cảnh báo) (research X-13) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lan_chay_m09xc` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T008 [P] Cài provider: `GanPhamViProvider` (`M09.DOI_CA`, `M09.NGHI_DOT_XUAT`), `PhanLoaiTruongProvider` (lý do `LY_DO_NHAN_SU`), `LuuTruProvider` (≥ CFG-M15-04)

**Checkpoint**: Nền 015 sẵn sàng.

---

## Phase 3: User Story 1 – Lập lịch từ mẫu xoay ca *(Bỏ theo Q-397, 2026-10-29)*

- [ ] T009, T010, T011, T012 *(Bỏ theo Q-397, 2026-10-29)*

---

## Phase 4: User Story 2 – Phủ tối thiểu và công bố lịch *(Bỏ theo Q-397, 2026-10-29)*

- [ ] T013, T014, T015, T016 *(Bỏ theo Q-397, 2026-10-29)*

---

## Phase 5: User Story 3 – Đổi ca: người nhận đồng ý, trưởng tầng duyệt (Priority: P2)

**Goal**: Loại yêu cầu `M09.DOI_CA` (hai chiều, nhận thay) dùng vòng đời mở rộng của 000; `KiemTraDoiCa` lúc lập, đồng ý, duyệt; tác động khi duyệt trong một transaction (ca, phân công, người phụ trách, `CongViecCaApi`, nhiệm vụ chuyến đi 014); Hết hiệu lực; nhắc CFG-M09-11; riêng tư lý do (FR-019 → FR-025, BR-M09-10).

**Independent Test**: Trên lịch đã công bố: một yêu cầu hợp lệ, một gây chồng giờ, một người nhận thiếu giấy phép cho phân công Điều dưỡng → đúng kết quả từng bước.

### Tests cho User Story 3

- [ ] T017 [P] [US3] Test đơn vị `BT/staffing/KiemTraDoiCaTest.java` và `BT/staffing/BR_M09_10_DoiCaIT.java`: duyệt đổi ca và vắng ca đồng thời; một yêu cầu mở mỗi (ca, nhân viên); chỉ A thấy lý do từ chối của B (Q13 → Q20, Q34)

### Implementation cho User Story 3

- [ ] T018 [US3] Tạo bảng `yeu_cau_doi_ca` (`yeu_cau_id` khóa chính trỏ `yeu_cau_phe_duyet`; `dang` `HAI_CHIEU|NHAN_THAY`; `trang_thai` 7 giá trị đồng bộ qua `chuyenMoRong`), `yeu_cau_ca_mo` (`UNIQUE (ca_id, nhan_vien_id)`) trong `MIG/V015_04__doi_ca.sql` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `yeu_cau_doi_ca` → `ho_so_chung`; `yeu_cau_ca_mo` → `khoa_duy_nhat` của `ho_so_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T019 [US3] Cài handler `BE/staffing/doica/internal/DoiCaHandler.java` cho `M09.DOI_CA` (`chuyenMoRong` cho `CHO_NGUOI_NHAN_DONG_Y`, `NGUOI_NHAN_TU_CHOI`, `HET_HIEU_LUC`; không có `NHAP`; `kiemTraKhiDuyet`; `khiDuyet` hoặc `apDung` gọi `CaLenhPort`, `PhanCongLenhPort`, `CongViecCaApi.khiKetThucPhanCong`, `ChuyenDiNhiemVuLenhApi.goNhiemVu`; tự duyệt Q-10) và `DoiCaService.java` (lập, đồng ý, từ chối, rút, hủy) (research X-08)
- [ ] T020 [US3] Tạo endpoint `/doi-ca`, `/doi-ca/ca-cua-nguoi-nhan`, `/{id}/kiem-tra`, `/{id}/{lenh}`; màn web `frontend/src/tinh-nang/lich-ca/doi-ca/`; app `mobile/lib/nhan-vien/lich-ca/` (SC-006 ≤ 2 phút) với test flutter

**Checkpoint**: Đổi ca chạy độc lập.

---

## Phase 6: User Story 4 – Nghỉ đột xuất có duyệt (Priority: P2)

**Goal**: `M09.NGHI_DOT_XUAT` theo ca hoặc theo ngày; lập thay; màn tác động trước duyệt; duyệt một phần; tác động (rút khỏi ca, phân công, `CongViecCaApi.khiNghiCoDuyet`, người phụ trách mới); theo ngày chuyển theo ca khi lịch công bố (FR-026 → FR-031).

**Independent Test**: Nghỉ ba ca, một ca làm tầng thiếu Điều dưỡng; xem tác động trước duyệt; duyệt; kiểm lịch, phân công, công việc.

### Tests cho User Story 4

- [ ] T021 [P] [US4] Test `BT/staffing/FR_029_NghiTheoCaIT.java`, `FR_026a_NghiTheoNgayIT.java` (Q21 → Q27)

### Implementation cho User Story 4

- [ ] T022 [US4] Tạo bảng `yeu_cau_nghi` (`dang` `THEO_CA|THEO_NGAY`; `lap_thay`), `yeu_cau_nghi_muc` (`ket_qua` `CHO|DUYET|TU_CHOI|DA_TACH|DA_ROI`), `nghi_theo_ngay` (nhóm 3; `UNIQUE (nhan_vien_id, ngay)`) trong `MIG/V015_05__nghi.sql` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `yeu_cau_nghi` → `ho_so_chung`; `nghi_theo_ngay` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T023 [US4] Cài handler `BE/staffing/doica/internal/NghiDotXuatHandler.java` (`DA_AP_DUNG_MOT_PHAN`, `HET_HIEU_LUC`; tác động qua `NghiCoDuyetPort`, `CaLenhPort`) và `NghiService.java` (tác động trước duyệt; tách ca theo giờ trong job; listener `LichCaDaCongBo`) (research X-09)
- [ ] T024 [US4] Tạo endpoint `/nghi-dot-xuat`, `/{id}/truoc-khi-duyet`, `/{id}/{lenh}`; màn web `frontend/src/tinh-nang/lich-ca/nghi/`; app phần nghỉ

**Checkpoint**: Nghỉ đột xuất chạy độc lập.

---

## Phase 7: User Story 5 – Thiếu phủ, lời mời *(Bỏ theo Q-397, 2026-10-29)*

- [ ] T025, T026, T027 *(Bỏ theo Q-397, 2026-10-29)*

---

## Phase 8: Polish & Cross-Cutting

- [ ] T028 [P] Cài `DoiCaThongKeApi` (016); mở rộng `BT/common/HopDongApiTest.java` với `specs/015-shift-rotation-swap/contracts/api-015.yaml`
- [ ] T029 *(Bỏ theo Q-397, 2026-10-29)* Không còn `KiemTraCongBoMoRong`; 008 hiển thị nghỉ theo ngày còn chờ qua `NghiCoDuyetPort`
- [ ] T030 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC-004, SC-006 theo quickstart mục 5
- [ ] T031 Rà truy vết: DBR-21 (qua 008), DBR-25 và BR-M09-10 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-040, FR-041, FR-042, FR-043, FR-044, FR-045, FR-046 — mỗi FR có task hoặc test tương ứng (constitution XV)
- [ ] T032 *(bổ sung 2026-10-29, research R-15 bản 2 của 000)* Khi viết migration và entity của feature, áp data-model mục 0: không tạo các bảng đã gộp, dùng bảng gộp, cột `JSON` và bảng chung (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`); test bảo vệ nhóm dữ liệu viết cho bảng gộp

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US1 → US2 (P1)**
- **US3, US4 (P2)**: sau US2 (tính phủ), song song với nhau
- **US5 (P3)**: sau US2; dùng tình huống thiếu phủ từ US4

### Parallel Opportunities

- T003, T005, T006, T008 song song; US3 và US4 song song

## Parallel Example: User Story 3

```text
Task: "T017 KiemTraDoiCaTest + BR_M09_10_DoiCaIT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2
2. **Dừng và kiểm chứng**: lập lịch từ mẫu, tính phủ, công bố có kiểm phủ
3. Thêm US3, US4, US5

### Incremental Delivery

US1 → US2 → US3 → US4 → US5; mỗi bước chạy `./mvnw verify`.

## Notes

- 015 không tự lập lịch, không chỉ định người phụ trách, không gợi ý hay xếp hạng người thay, không tự mời (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M09-02, 06, 07, 10, 11, CFG-M15-04 → 06)
