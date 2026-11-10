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

- [x] T001 Tạo gói con `BE/staffing/doica/` (Q-397: bỏ `xoayca`, `phuca`); mở rộng ArchUnit `BT/staffing/M09ArchTest.java` (008 T001): chỉ `staffing.doica` dùng port nội bộ; gói `..lich..` không lập lịch, không chỉ định người phụ trách (research X-01) *(2026-11-09, D tự chọn: gói là `staffing.internal.swap` (glossary mục 2 "doica → swap"; `LayerArchTest` không cho gói cấp trên `staffing.swap`), không phải `doica`; luật "chỉ `internal.swap` gọi port lệnh nội bộ" đổi theo gói mới, Q-306 cho `scheduling..` đã có từ 008; thêm `Q_397_swapDoesNotCreateSchedulesOrShifts` cấm `internal.swap..` gọi createSchedule/createShift/createHandover/addHandoverItem; `M09ArchTest` 3, `LayerArchTest` 10, `ModuleBoundaryTest` 1 xanh)*

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Port bổ sung của 008, bảng mẫu, nhóm, hàm thuần xếp ca và tính phủ, khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T002 Bổ sung port trong `BE/staffing/lichca/port/`: `LichNhapLenhPort` (chỉ `goNhanVienKhoiNhap`, `lichThang`; Q-397), `NghiCoDuyetPort`; cài ở gói `staffing.lichca.internal` của 008 (contracts mục 1.2; research X-03) *(2026-11-09, D tự chọn, làm một phần: không có gói `lichca` ở 008 và gói đó thuộc E, nên 015 không tạo `LichNhapLenhPort`; việc gỡ nhân viên khỏi ca Nháp (FR-026a) 015 làm trực tiếp qua `ShiftStore.updateShift` và `AssignmentService.removeDraftMembers` trong `LeaveService.applyDay`, đã có test `FR_026a_dayLeaveRemovesStaffFromDraftShiftAndClearsLead`; `NghiCoDuyetPort` đặt tên `ApprovedLeaveDayPort` ở `staffing.internal.swap.service`, `LeaveService` cài (`approvedLeaveDay`, `pendingLeaveDayStaff`), có test. **Chờ E (008)** gọi hai hàm này khi xếp tay lịch Nháp (FR-012) và khi công bố (FR-016): dòng team-plan mục 8)*
- [x] T003 [P] Bổ sung `BE/staffing/api/DoiCaThongKeApi.java` (yêu cầu, giờ làm theo tháng; Q-397: bỏ phủ, lời mời) và thông báo (cài `CoYeuCauThongBao`) (contracts mục 2.1, 2.3) *(2026-11-09: `SwapStatisticsApi`, `StaffRequestView`, `WorkingHoursView` ở `staffing.api`, bản cài `SwapStatistics` (= T028); thông báo `SwapNotification` implements `HasNotificationRequests`; test `FR_042_SwapStatisticsIT` 3 ca, `FR_044_SwapNoticeAuditIT` mức, người nhận, không nêu lý do)*
- [ ] T004 *(Bỏ theo Q-397, 2026-10-29)* Không có bảng mẫu xoay ca, nhóm, `moc_nhac_lich`; `lan_chay_m09xc` tạo ở T007 **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lan_chay_m09xc` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T005 [P] *(Bỏ theo Q-397, 2026-10-29)*
- [ ] T006 [P] *(Bỏ theo Q-397, 2026-10-29)*
- [x] T007 Cài khung job `BE/staffing/doica/lich/ViecM09DoiCa.java` (ShedLock, `lan_chay_m09xc`) cho Hết hiệu lực yêu cầu, tách ca theo giờ (Q-397: bỏ nhắc lập lịch, nhắc công bố, cảnh báo) (research X-13) *(2026-11-09, D tự chọn: `SwapExpiryTask implements M09TimedTask` nhịp phút trong khung `M09MinuteJob` (ShedLock `M09_MINUTE`, đánh dấu tick của 008) gọi `SwapService.expireDue/remindDue`, `LeaveService.expireDue/remindDue`; không tạo `lan_chay_m09xc` vì việc dựa trên trạng thái nên chạy bù an toàn, mốc nhắc lưu ở `remindedAt` của chi tiết; test `FR_025_openSwapExpiresWhenTheEarliestShiftStarts`, `FR_030_shiftThatStartsBeforeDecisionIsSplitOffAndRequestExpires`, `FR_025_reminderIsSentOnceWhenTheShiftIsWithinTheConfiguredWindow`)* **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lan_chay_m09xc` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T008 [P] Cài provider: `GanPhamViProvider` (`M09.DOI_CA`, `M09.NGHI_DOT_XUAT`), `PhanLoaiTruongProvider` (lý do `LY_DO_NHAN_SU`), `LuuTruProvider` (≥ CFG-M15-04) *(2026-11-09: xong `ScopeAssignmentProvider` và `FieldClassificationProvider` cho `M09.SHIFT_SWAP`, `M09.LEAVE_REQUEST` ở `SwapGovernanceConfig` (lý do `STAFF_REASON`). **Chờ A (000)** phần lưu giữ: `ApprovalRequestRetention.FINAL` thiếu trạng thái cuối mở rộng và chưa xóa dòng `simple_record` của 015 cùng yêu cầu; dòng team-plan mục 8)*

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

- [x] T017 [P] [US3] Test đơn vị `BT/staffing/KiemTraDoiCaTest.java` và `BT/staffing/BR_M09_10_DoiCaIT.java`: duyệt đổi ca và vắng ca đồng thời; một yêu cầu mở mỗi (ca, nhân viên); chỉ A thấy lý do từ chối của B (Q13 → Q20, Q34) *(2026-11-09: `SwapRulesTest` 3 ca (chồng giờ, giờ trong tháng) và `BR_M09_10_ShiftSwapIT` 11 ca xanh: đổi hai chiều, nhận thay, chồng giờ chặn, một yêu cầu mở mỗi (ca, nhân viên), người nhận đã có tên, thiếu vai trò chung, lý do từ chối chỉ người nhường thấy, rút yêu cầu nhả khóa, người phụ trách chuyển, duyệt chung bị chặn, hết hiệu lực. "Duyệt đổi ca và vắng ca đồng thời" không còn xảy ra được: khóa (ca, nhân viên) chặn lập yêu cầu thứ hai và ghi vắng chỉ làm được khi ca đã bắt đầu, lúc đó yêu cầu đã hết hiệu lực)*

### Implementation cho User Story 3

- [x] T018 [US3] *(2026-11-09, D tự chọn: không có migration `V015_04`, theo data-model mục 0 "còn 0 bảng riêng": chi tiết ở `simple_record` loại `M09.SWAP_DETAIL`, khóa ở `M09.OPEN_SHIFT_LOCK`)* Tạo bảng `yeu_cau_doi_ca` (`yeu_cau_id` khóa chính trỏ `yeu_cau_phe_duyet`; `dang` `HAI_CHIEU|NHAN_THAY`; `trang_thai` 7 giá trị đồng bộ qua `chuyenMoRong`), `yeu_cau_ca_mo` (`UNIQUE (ca_id, nhan_vien_id)`) trong `MIG/V015_04__doi_ca.sql` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `yeu_cau_doi_ca` → `ho_so_chung`; `yeu_cau_ca_mo` → `khoa_duy_nhat` của `ho_so_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [x] T019 [US3] *(2026-11-09: `SwapHandler` (`M09.SHIFT_SWAP`, trạng thái mở rộng `AWAITING_RECIPIENT`, `RECIPIENT_DECLINED`, `EXPIRED`; duyệt chung của 000 bị chặn `USE_SHIFT_SWAP_ENDPOINT`), `SwapService` (lập, đồng ý, từ chối, rút đồng ý, hủy, duyệt, từ chối duyệt; `CaLenhPort`, `PhanCongLenhPort` qua `ShiftCommandPort`, `AssignmentCommandPort`; `ShiftTaskApi.onAssignmentEnded` cho phân công không chuyển được; `TripDutyCommandApi.removeDuty`); chưa tính tỷ lệ phục vụ trước/sau (FR-021 f): chờ E, dòng team-plan)* Cài handler `BE/staffing/doica/internal/DoiCaHandler.java` cho `M09.DOI_CA` (`chuyenMoRong` cho `CHO_NGUOI_NHAN_DONG_Y`, `NGUOI_NHAN_TU_CHOI`, `HET_HIEU_LUC`; không có `NHAP`; `kiemTraKhiDuyet`; `khiDuyet` hoặc `apDung` gọi `CaLenhPort`, `PhanCongLenhPort`, `CongViecCaApi.khiKetThucPhanCong`, `ChuyenDiNhiemVuLenhApi.goNhiemVu`; tự duyệt Q-10) và `DoiCaService.java` (lập, đồng ý, từ chối, rút, hủy) (research X-08)
- [x] T020 [US3] *(2026-11-09: `SwapController` `/shift-swaps…` (contracts đã dịch, `ApiContractIT` xanh); web `frontend/src/features/shift-swaps/` (`SwapPanel`), vitest 6 ca; app `mobile/lib/staff/shift_swaps/`, flutter test 7 ca; chưa đo SC-006 ≤ 2 phút trên thiết bị)* Tạo endpoint `/doi-ca`, `/doi-ca/ca-cua-nguoi-nhan`, `/{id}/kiem-tra`, `/{id}/{lenh}`; màn web `frontend/src/tinh-nang/lich-ca/doi-ca/`; app `mobile/lib/nhan-vien/lich-ca/` (SC-006 ≤ 2 phút) với test flutter

**Checkpoint**: Đổi ca chạy độc lập.

---

## Phase 6: User Story 4 – Nghỉ đột xuất có duyệt (Priority: P2)

**Goal**: `M09.NGHI_DOT_XUAT` theo ca hoặc theo ngày; lập thay; màn tác động trước duyệt; duyệt một phần; tác động (rút khỏi ca, phân công, `CongViecCaApi.khiNghiCoDuyet`, người phụ trách mới); theo ngày chuyển theo ca khi lịch công bố (FR-026 → FR-031).

**Independent Test**: Nghỉ ba ca, một ca làm tầng thiếu Điều dưỡng; xem tác động trước duyệt; duyệt; kiểm lịch, phân công, công việc.

### Tests cho User Story 4

- [x] T021 [P] [US4] Test `BT/staffing/FR_029_NghiTheoCaIT.java`, `FR_026a_NghiTheoNgayIT.java` (Q21 → Q27) *(2026-11-09: `FR_029_ShiftLeaveIT` 9 ca xanh: nghỉ theo ca đánh dấu nghỉ có duyệt, duyệt một phần cần lý do, chọn người phụ trách, ca thiếu người phụ trách, lập thay, ca đang trong yêu cầu khác, tách ca khi bắt đầu, nghỉ theo ngày gỡ khỏi ca Nháp và để trống người phụ trách, tháng đã công bố bị từ chối, một nghỉ theo ngày mỗi (nhân viên, ngày))*

### Implementation cho User Story 4

- [x] T022 [US4] *(2026-11-09, D tự chọn: không có migration `V015_05`; `M09.LEAVE_DETAIL` (mục nghỉ nằm trong nội dung), `M09.LEAVE_DAY` ở `simple_record` nhóm 3 khóa dòng ngay)* Tạo bảng `yeu_cau_nghi` (`dang` `THEO_CA|THEO_NGAY`; `lap_thay`), `yeu_cau_nghi_muc` (`ket_qua` `CHO|DUYET|TU_CHOI|DA_TACH|DA_ROI`), `nghi_theo_ngay` (nhóm 3; `UNIQUE (nhan_vien_id, ngay)`) trong `MIG/V015_05__nghi.sql` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `yeu_cau_nghi` → `ho_so_chung`; `nghi_theo_ngay` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [x] T023 [US4] *(2026-11-09: `LeaveHandler`, `LeaveService` gồm `approve` (từng ca, ngày), `reject`, `cancel`, `preApproval`, `expireDue`, `remindDue`, listener `SchedulePublished` chuyển nghỉ theo ngày thành theo ca; tác động `ShiftCommandPort.leaveShift` (APPROVED_LEAVE), `ShiftTaskApi.onApprovedLeave`, `TripDutyCommandApi.removeDuty`; `NghiCoDuyetPort` = `ApprovedLeaveDayPort` ở T002; test listener `SchedulePublished` bổ sung 2026-11-09 lượt 2: `FR_026a_publishingTheMonthTurnsPendingDayLeaveIntoLeaveForThatDaysShifts`)* Cài handler `BE/staffing/doica/internal/NghiDotXuatHandler.java` (`DA_AP_DUNG_MOT_PHAN`, `HET_HIEU_LUC`; tác động qua `NghiCoDuyetPort`, `CaLenhPort`) và `NghiService.java` (tác động trước duyệt; tách ca theo giờ trong job; listener `LichCaDaCongBo`) (research X-09)
- [x] T024 [US4] *(2026-11-09: `LeaveController` `/leave-requests…`; web `LeavePanel`; app `LeaveCreateSheet` và duyệt trong `shift_swaps_screen.dart`; **chờ chủ dự án chốt**: thêm `scopeType`, `scopeId` vào `CreateLeave` cho nghỉ theo ngày (hợp đồng cũ chưa có phạm vi), 2026-11-09 lượt 2: web chọn người nghỉ (lập thay) từ `/staff`, tầng/khu từ `/floors`, `/areas` thay cho nhập mã, vitest `FR_026a_nghiTheoNgayChonTangTuDanhSach_vaLapThayChonNguoi`; app thêm nghỉ theo ngày (chọn ngày, phạm vi, tầng/khu), flutter `FR_026a_createLeaveByDayPicksDatesAndFloorFromCatalog`)* Tạo endpoint `/nghi-dot-xuat`, `/{id}/truoc-khi-duyet`, `/{id}/{lenh}`; màn web `frontend/src/tinh-nang/lich-ca/nghi/`; app phần nghỉ

**Checkpoint**: Nghỉ đột xuất chạy độc lập.

---

## Phase 7: User Story 5 – Thiếu phủ, lời mời *(Bỏ theo Q-397, 2026-10-29)*

- [ ] T025, T026, T027 *(Bỏ theo Q-397, 2026-10-29)*

---

## Phase 8: Polish & Cross-Cutting

- [x] T028 [P] Cài `DoiCaThongKeApi` (016); mở rộng `BT/common/HopDongApiTest.java` với `specs/015-shift-rotation-swap/contracts/api-015.yaml` *(2026-11-09: `SwapStatistics` (T003); `ApiContractIT` thêm hợp đồng 015, `checkOperations` bật, không dòng `known-mismatches`; `api-015.yaml` đã dịch (glossary mục 21); `gen-api.mjs` thêm `015`)*
- [ ] T029 *(Bỏ theo Q-397, 2026-10-29)* Không còn `KiemTraCongBoMoRong`; 008 hiển thị nghỉ theo ngày còn chờ qua `NghiCoDuyetPort`
- [ ] T030 *(2026-11-09, chưa xong: `clean verify -Dit.forkCount=3` dừng ở pha test đơn vị vì OutOfMemoryError của JVM surefire sau 835 test xanh (cùng lỗi khi gom nhiều ArchTest), chưa chạy được pha IT; đã chạy riêng và xanh: `SwapRulesTest` 3, IT 015 26 ca (`BR_M09_10_ShiftSwapIT` 11, `FR_029_ShiftLeaveIT` 9, `FR_042_SwapStatisticsIT` 3, `FR_044_SwapNoticeAuditIT` 3), `ApiContractIT`, `LayerArchTest`, `SolidArchTest`, `ApiNamedInterfaceArchTest`, `RequiresPermissionArchTest`, `M09ArchTest`, `ModuleBoundaryTest`, `SharedKindArchTest`; vitest `shift-swaps` 6, flutter test 7 xanh; chưa chạy quickstart mục 3, chưa đo SC-004, SC-006)* Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC-004, SC-006 theo quickstart mục 5
- [ ] T031 *(2026-11-09, làm một phần: FR-040 (`BR_M09_10_ShiftSwapIT` duyệt chung bị chặn, người ngoài 403; `FR_029_ShiftLeaveIT` lập thay), FR-042 (`FR_042_SwapStatisticsIT`), FR-043 (flutter `FR_043_offlineShowsMessageAndKeepsInput`), FR-044, FR-046 (`FR_044_SwapNoticeAuditIT`), FR-045 (lý do từ chối, lý do nghỉ); FR-041: `FR_041_approvedMutualSwapRemovesTripDutiesOfBothLeavingStaff`, `FR_041_approvedLeaveRemovesTripDutyOnlyForApprovedShifts` (015 gọi `TripDutyCommandApi` đúng người, đúng ca; bản cài thật của 014 do C kiểm), listener `SchedulePublished` có test (T023); còn tỷ lệ phục vụ FR-021 (f) chờ E)* Rà truy vết: DBR-21 (qua 008), DBR-25 và BR-M09-10 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-040, FR-041, FR-042, FR-043, FR-044, FR-045, FR-046 — mỗi FR có task hoặc test tương ứng (constitution XV)
- [x] T032 *(2026-11-09: không có migration và bảng riêng; mọi bản ghi ở `simple_record` (nhóm 2 theo dòng; `M09.LEAVE_DAY` khóa dòng ngay); `DataGroupArchTest`, `SharedKindArchTest` xanh)* *(bổ sung 2026-10-29, research R-15 bản 2 của 000)* Khi viết migration và entity của feature, áp data-model mục 0: không tạo các bảng đã gộp, dùng bảng gộp, cột `JSON` và bảng chung (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`); test bảo vệ nhóm dữ liệu viết cho bảng gộp *(2026-10-30, audit cơ sở dữ liệu F-04, PR #63)* Mọi bảng tạo hoặc gộp khai nhóm dữ liệu: entity bằng `@DataGroup`, bảng JDBC bằng `COMMENT = 'DataGroup=CATALOG|BUSINESS|RECORD|MULTIPLE|TECHNICAL'` (backend/README.md mục 4); bảng `RECORD` có `trg_<bảng>_no_update`, `trg_<bảng>_no_delete`; bảng chứa nhóm 2 và nhóm 3 dùng `@DataGroup(MULTIPLE)`, cột `locked_at`, trigger `trg_<bảng>_row_lock`; dòng bảng chung đặt `entity_type` trùng `RetentionProvider.dataType()` của bản ghi cha; job dùng `SchedulerRuns` (module-interfaces 000 mục 1.8). `RecordGroupTriggerIT` kiểm.

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

## Phase 9: Convergence

- [x] T033 Cho nhân viên chỉ có ca Đã công bố chưa bắt đầu (chưa có phân công quanh giờ hiện tại) lập được yêu cầu đổi ca, nghỉ đột xuất của chính mình: hiện `ApprovalRequestApi.create` kiểm `createPermission()` theo phạm vi ASSIGNED chỉ tính phân công hiện tại nên trả 403; A chọn cách (a) `RequestTypeHandler.selfCheckedCreatePermission()` hoặc (b) ASSIGNED tính ca tương lai của chính nhân viên cho `SHIFT_SWAP_REQUEST/CREATE` (team-plan mục 8, dòng A (000, 002)), rồi bỏ cấp phân công tạm trong test, test viết trước per FR-019, FR-026 (partial)
  - *(2026-11-10, A chọn cách (a))* `RequestTypeHandler.creatorInOwnScope(user, content)` (mặc định rỗng): có giá trị thì 000 chỉ kiểm người lập giữ khóa quyền tạo, không xét phạm vi theo phân công hiện tại; dùng ở lệnh lập và lúc gửi duyệt (`ApprovalRequestService.mayCreate`). `SwapHandler`: người lập là người nhường và có tên trong ca X; `LeaveHandler`: lập cho chính mình (lập thay vẫn kiểm theo phạm vi). Bỏ ca đang diễn ra và phân công tầng tạm trong `SwapTestBase`: 29 IT của 015 đỏ 403 rồi xanh (`BR_M09_10_ShiftSwapIT` 12, `FR_029_ShiftLeaveIT` 11, `FR_042_SwapStatisticsIT` 3, `FR_044_SwapNoticeAuditIT` 3); `ApplyOnceIT`, `ApprovalLifecycleIT`, `ConcurrentDecisionIT`, test kiến trúc xanh.
