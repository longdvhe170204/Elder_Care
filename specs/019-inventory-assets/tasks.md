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

- [x] T001 Tạo ba gói `BE/billing/muasam/`, `BE/roombedallocation/taisan/`, `BE/roombedallocation/lichxe/`; ArchUnit `BT/roombedallocation/TaiSanArchTest.java`: chỉ `roombedallocation.taisan` dùng port giường; job không tạo phiếu, yêu cầu, lịch (Q-306); M03 không phụ thuộc M11 (research W-01, W-02) *(2026-11-06: gói theo backend/README `billing.internal.procurement`, `roombedallocation.internal.asset`, `roombedallocation.internal.vehicle`; `AssetArchTest` 5/5, gồm fixture cố ý vi phạm)*

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo giao diện mới ở `roombedallocation.api`, `billing.api`; khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T002 [P] Tạo `BE/roombedallocation/api/`: SPI `YeuCauMuaSamPort` (M11 cài), `LichXeApi`, `LichXeLenhApi`, `TaiSanThongKeApi`; `BE/billing/api/`: `MuaSamThongKeApi`; thông báo (cài `CoYeuCauThongBao`) (contracts mục 2.1 → 2.3) *(2026-11-06: `ProcurementRequestPort` (`api.spi`), `AssetStatisticsApi`, `ProcurementStatisticsApi` theo yêu cầu chung của 016 (chủ dự án duyệt, contracts mục 2.3 sửa), sự kiện `AssetStatusChanged`, `ProcurementRequestChanged` mang thông báo (`HasNotificationRequests`), enum glossary 25.4, bản giả thống kê; `LichXeApi`, `LichXeLenhApi` đã khai ở 014; `YeuCauMuaSamApi` cho M08 bỏ theo Q-392. `T002_AssetProcurementApiContractTest` 5/5)*
- [x] T003 Tạo bảng kỹ thuật `nhac_019` trong `MIG/V019_01__nhac.sql` và khung job (`BE/billing/muasam/lich/`, `BE/roombedallocation/taisan/lich/`, `BE/roombedallocation/lichxe/lich/`; ShedLock, chạy bù, không nhắc trùng) (research W-09) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `nhac_019` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.** *(2026-11-06, chủ dự án duyệt: không có migration `V019_01` vì `nhac_019` gộp vào `reminder_marker`; job tạo cùng processor của từng story: mua sắm ở T007, tài sản ở T018, lịch xe ở T022; đánh dấu khi đủ ba job; 2026-11-06: job tài sản `REMIND_ASSET_DUE_DATES` xong; job lịch xe `REMIND_VEHICLE_SCHEDULES` xong (Phase 9), đủ ba job)*
- [x] T004 [P] Cài provider: `PhanLoaiTruongProvider` (nguyên giá, chi phí → `TAI_CHINH`), `LuuTruProvider`, `GanPhamViProvider` (`M11.MUA_SAM` bộ phận; `M03.TAI_SAN` bộ phận và tầng của vị trí; `M03.LICH_XE` toàn viện), `QuyenTepProvider` (`M11.CHUNG_TU_MUA_SAM`) (research W-08) *(2026-11-06, chủ dự án duyệt: mỗi provider cài cùng migration của story tương ứng: `M11.PROCUREMENT_REQUEST`, `M11.PROCUREMENT_RECEIPT` ở Phase 3; `M03.ASSET` ở Phase 7; `M03.VEHICLE_SCHEDULE` ở Phase 9; đánh dấu khi đủ; 2026-11-06: M11 xong (`ProcurementGovernance`), `M03.ASSET` xong (`AssetGovernance`: phạm vi, lưu giữ, phân loại trường); `M03.VEHICLE_SCHEDULE` xong (`VehicleScheduleGovernance`: phạm vi toàn viện, lưu giữ), đủ)*

**Checkpoint**: Nền 019 sẵn sàng.

---

## Phase 3: User Story 7 – Bộ phận lập yêu cầu mua sắm; Quản lý viện duyệt chi phí; Kế toán đối chiếu (Priority: P1) 🎯 MVP

**Goal**: Yêu cầu mua sắm của bộ phận (Phụ trách bộ phận); Quản lý viện duyệt có thể giảm; ghi đã mua; Kế toán (khác người ghi đã mua) đối chiếu, khớp, lệch; vượt theo CFG-M11-07; gắn nguồn (phiếu nhập, tài sản, bảo trì thuê ngoài) một nguồn một yêu cầu; nhắc CFG-M11-08, 09 (FR-008 → FR-008c, BR-M11-17 → 20, DBR-36, Q-284).

**Independent Test**: Bếp lập 2.000.000 đồng; Quản lý viện duyệt giảm còn 1.800.000; ghi đã mua 1.950.000 → lệch; Kế toán ghi lệch; Quản lý viện chấp nhận.

### Tests cho User Story 7

- [x] T005 [P] [US7] Test đơn vị `BT/billing/KiemTraVuotTest.java`, `KhopChungTuTest.java` và `BT/billing/BR_M11_17_MuaSamIT.java`, `BR_M11_18_VuotIT.java`, `BR_M11_19_DoiChieuIT.java`, `BR_M11_20_NhacIT.java`, `DBR_36_GanNguonIT.java` (Q1 → Q9) *(2026-11-07: đủ `ApprovedAmountCheckTest`, `ReceiptMatchTest` và năm IT (`BR_M11_17_ProcurementRequestIT`, `BR_M11_18_OverrunIT`, `BR_M11_19_ReconciliationIT`, `BR_M11_20_ReminderIT`, `DBR_36_SourceLinkIT`) cho Q1, Q2, Q4 → Q9 (Q3 bỏ theo Q-392); nhóm billing 2026-11-07 xanh)*

### Implementation cho User Story 7

- [x] T006 [US7] Tạo `MIG/V019_02__mua_sam.sql`: `yeu_cau_mua_sam` (`loai` `NGUYEN_LIEU|TAI_SAN|DICH_VU|THUOC|KHAC`; `muc_uu_tien`, `thoi_diem_can` (`CHECK` bắt buộc khi `THUOC`), `tep_lien_quan_ids` (không bắt buộc); dòng thêm `nct_id`, `don_thuoc_id`, `hoat_chat`, `ham_luong`, `dang_thuoc`, `don_vi` (FR-008d, Q-337); `trang_thai` `NHAP|CHO_DUYET|DA_DUYET|TU_CHOI|CHO_DOI_CHIEU|LECH|DA_DOI_CHIEU|DA_HUY`), `yeu_cau_mua_sam_dong`, `mua_sam_gan_nguon` (nhóm 3; `loai_nguon` `PHIEU_NHAP|TAI_SAN|BAO_TRI`; `UNIQUE (loai_nguon, nguon_id)`) (data-model mục 3) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `mua_sam_gan_nguon` → cột `JSON` của yêu cầu mua sắm; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.** *(2026-11-07: `V019_02__procurement_request.sql` đủ cột, `CHECK` MEDICATION bắt buộc mức ưu tiên và thời điểm cần, dòng và nguồn gắn ở cột JSON theo R-15, trigger khóa nội dung ngoài Nháp, khóa dòng, chặn xóa; kiểm bằng `BR_M11_17`, `DBR_36`, `RecordGroupTriggerIT`)*
- [x] T007 [US7] Cài `BE/billing/muasam/internal/MuaSamService.java` (bảng chuyển data-model mục 3; quyền Phụ trách bộ phận qua `NhiemVuApi`; Điều dưỡng chỉ lập, gửi, rút lại, hủy loại `THUOC`, dòng gắn đơn qua `DonThuocApi.donNguonVienChuaKetThuc` và người cao tuổi trong phạm vi phân công; mọi người lập loại `THUOC` phải xem được `SUC_KHOE` của người cao tuổi của dòng (`QuyenXemTruongApi`) mới gắn được đơn (research W-05); Kế toán không lập, người đối chiếu ≠ người ghi đã mua; loại `THUOC` đối chiếu chỉ chứng từ với số thực tế), `YeuCauMuaSamApi` cho M08, `YeuCauMuaSamPortImpl` cho M03 (kiểm yêu cầu Đã duyệt cùng bộ phận khi gắn nguồn); nhắc trong job (research W-05) *(2026-11-07: `ProcurementRequestService`, `ProcurementRequestPortImpl`, job nhắc xong; `YeuCauMuaSamApi` cho M08 bỏ theo Q-392; dòng thuốc kiểm đơn qua `billing.api.spi.ProcurementPrescriptionPort` (đã duyệt thay W-02), test dùng `FakeProcurementPrescriptions` đúng quy định bản giả của phụ thuộc chưa xong; bản cài thật là việc của D, tới đó production lập yêu cầu thuốc có dòng gắn đơn trả 503 (đã ghi ở team-plan mục 8); thêm ca `T007_medicationRequestIsReconciledAgainstTheReceiptOnlyAndNotByTheRecorder` cho đối chiếu loại THUOC. Phần "Đã nhận" thuộc T027, không thuộc T007)*
- [x] T027 [US7] Cài `BE/billing/muasam/internal/YeuCauThuocCungUngPortImpl.java` (SPI `medication.api.YeuCauThuocCungUngPort`, research W-05) và hiển thị "Đã nhận" từ `TiepNhanThuocCungUngApi.theoYeuCau` trên chi tiết yêu cầu (bổ sung 2026-10-11, Q-337)
  - *(2026-11-10, C, theo quyết định 2026-11-08 ở team-plan mục 8; tên cũ là hướng trước đảo chiều)*: (1) `ProcurementRequestService` đọc `medication.api.PrescriptionApi.unendedFacilityPrescriptions`, bỏ `ProcurementPrescriptionPort`, `FakeProcurementPrescriptions`; `ModuleBoundaryTest` 1/1, `LayerArchTest` 10/10 ngay sau khi đổi, không báo vòng. (2) `billing.api.ProcurementMedicationLineApi.lineRecordedPurchased` (DTO `billing.api.dto.ProcurementMedicationLineView`) cài ở `ProcurementMedicationLines`. (3) khai `billing.api.spi.ProcurementIntakePort.passedLineIds`, bản giả `support/fake/billing/FakeProcurementIntakes`; `received` của `GET /procurement-requests` tính từ port, thiếu bean thì `null`. (4) id dòng `lines[].id` theo `ProcurementLineId` (`requestId × 1.000.000 + thứ tự`), xóa dòng `lines[] thiếu trường id` ở `contract/known-mismatches.txt`; `ApiContractIT` xanh. Test: `FR_008d_MedicationRequestIT.T028_purchasedLineIsReadByLineIdAndReceivedFollowsIntake` đỏ (`lines[0].id` không có) → xanh. Phần 006 (đổi `SuppliedMedicationIntakeService` sang API mới, cài `ProcurementIntakePort`, màn gửi `requestLineId`) là của D, dòng team-plan mục 8.
- [x] T028 [P] [US7] Test `BT/billing/FR_008d_YeuCauThuocIT.java`: Điều dưỡng không là Phụ trách bộ phận lập yêu cầu `THUOC` gắn đơn nguồn viện Hiệu lực của người cao tuổi trong phạm vi → 201, gửi duyệt → Chờ duyệt và báo Quản lý viện; dòng gắn đơn gia đình gửi hoặc đơn Đã ngừng → 409; người cao tuổi ngoài phạm vi phân công → 403; thiếu mức ưu tiên hoặc thời điểm cần → 422; Điều dưỡng lập loại `NGUYEN_LIEU` → 403; Điều dưỡng ghi đã mua → 403; Phụ trách bộ phận Bếp (không xem được `SUC_KHOE`) lập yêu cầu `THUOC` gắn đơn → 403 và không được cấp thêm quyền xem; Phụ trách bộ phận Y tế xem được thì lập được; dòng thiếu `nctId` hoặc `donThuocId` → 422; đơn không thuộc người cao tuổi của dòng → 409; `YeuCauThuocCungUngPort` trả `boPhanId` của yêu cầu, không đổi khi người lập chuyển bộ phận; sau khi Phụ trách bộ phận ghi đã mua, `YeuCauThuocCungUngPort` trả dòng; trước đó trả rỗng; không có khoản chi phí người cao tuổi (spec 019 US7 #8; FR-008d, BR-M11-17, Q-337)
  - *(2026-11-10, C)*: giữ tên lớp `FR_008d_MedicationRequestIT` (đã có, glossary tên tiếng Anh), 3 ca: Q34 luồng chính (201, Chờ duyệt, báo Quản lý viện); `T028_prescriptionMustBeFacilityUnendedOfTheLineResidentAndAuthorMustSeeHealth` (đơn gia đình, Đã ngừng, đơn của người khác → 409 `PRESCRIPTION_NOT_FACILITY_UNENDED`; ngoài phạm vi → 403; Phụ trách Bếp → 403, sau đó vẫn không xem được HEALTH; Phụ trách Y tế lập được); `T028_purchasedLineIsReadByLineIdAndReceivedFollowsIntake` (rỗng trước ghi đã mua, có dòng sau đó với `departmentId` của yêu cầu khi người lập đã chuyển bộ phận, `received` false → true, loại khác `null`, không có `cost_item` của người cao tuổi). Đơn thuốc qua bean thật `PrescriptionApi` của 006 bọc `@MockitoSpyBean` (006 chỉ lập đơn qua repository nội bộ, không có đường công khai gọn; chưa có bản giả chung của `PrescriptionApi`). Nhánh 422 (thiếu mức ưu tiên, thời điểm cần, `residentId`, `prescriptionId`), Điều dưỡng lập `INGREDIENT`, ghi đã mua → 403 ở unit `FR_008d_MedicationRequestRulesTest` (2 ca). Ca thứ hai của IT đỏ đúng chỗ trước khi cài, hai ca còn lại và unit xanh ngay vì là hành vi có sẵn sau bước (1).
- [x] T008 [US7] Tạo endpoint `/mua-sam`, `/mua-sam/{id}`, `/mua-sam/{id}/{lenh}`; màn web `frontend/src/tinh-nang/mua-sam/` *(2026-11-06: phần backend xong (4 endpoint, ApiContractIT); màn web chủ dự án hoãn, làm sau khi thống nhất thiết kế M35 với A)*
  - *(2026-11-10, C, bố cục theo duyệt 09/11 mục (8); người dùng chọn đọc tên từ API module khác)*: màn `frontend/src/features/inventory/ProcurementRequestsPage` (+ `ProcurementParts`, `procurementLabels`), mục menu `procurement` nhóm "Chi phí, dịch vụ"; bảng lọc trạng thái, khung chi tiết với 10 lệnh theo trạng thái và `/me/permissions`, lập yêu cầu (gồm loại thuốc gắn đơn nguồn viện, FR-008d), ghi đã mua tải chứng từ qua `uploadFile`. Test `ProcurementRequestsPage.test.jsx` 7 ca: đỏ 7/7 (chưa có màn) → xanh 7/7; `npm test` 939/939, lint, build xanh; xem trên trình duyệt (dữ liệu giả lập theo hợp đồng, 1280px, 375px, tối). Giữ [ ]: chưa sửa được Nháp (PUT cần `If-Match` nhưng `ProcurementRequest` không trả phiên bản) và nút theo quyền chưa phân biệt người lập, Phụ trách bộ phận (hợp đồng không trả người lập, `allowedCommands`); chưa chạy với backend thật.
  - *(2026-11-10, A; chủ dự án duyệt gói api-019)* `ProcurementRequest` trả `version`, `createdByStaffId`, `allowedCommands`; màn web nút lệnh theo `allowedCommands`, thêm "Sửa nháp…" (PUT kèm `If-Match` theo `version`, `CreateProcurementSheet` chế độ sửa). `BR_M11_17_ProcurementRequestIT.FR_008_list…` và `ProcurementRequestsPage.test.jsx` ca sửa Nháp đỏ rồi xanh; `npm test` 965/965, lint, build xanh.

**Checkpoint**: Mua sắm chạy độc lập.

---

## Phase 4 → 6: User Story 1 → 3 – Kho nguyên liệu *(Bỏ theo Q-392)*

T009 → T015 bỏ: hệ thống không quản lý kho nguyên liệu (nhập, xuất, lô, kiểm kê, cảnh báo hạn dùng, tồn tối thiểu). Giữ mã task để không đánh số lại.

---

## Phase 7: User Story 4 – Phụ trách bộ phận quản lý tài sản, bảo trì và báo hỏng (Priority: P2)

**Goal**: Ghi tăng (nguyên giá bắt buộc khi gắn yêu cầu mua sắm, Q-294), đưa vào sử dụng, vị trí, chuyển bộ phận (Quản lý viện), bảo trì/kiểm định/đăng kiểm/bảo hiểm, báo hỏng, thanh lý qua yêu cầu `M03.THANH_LY_TAI_SAN` (không tự duyệt); dấu quá hạn theo ngày (FR-011 → FR-016, BR-M03-17, BR-M03-18).

**Independent Test**: Ghi tăng một xe và một máy tạo oxy; báo hỏng máy tạo oxy; bảo trì Đạt; qua mốc hạn đăng kiểm của xe; thanh lý một tài sản.

### Tests cho User Story 4

- [x] T016 [P] [US4] Test `BT/roombedallocation/BR_M03_17_HanBaoTriIT.java`, `BR_M03_18_HuHongIT.java` (Q18 → Q21, Q24 → Q26) *(2026-11-06 lượt 1: `FR_013_AssetLifecycleIT` 6/6 (gồm báo hỏng theo tầng của BR-M03-18); `BR_M03_17_MaintenanceDueIT`, `BR_M03_18_DamageIT` đủ phần ở lượt 2; lượt 2: `BR_M03_17_MaintenanceDueIT` 3/3, `FR_013_AssetDisposalIT` 1/1, `T004_AssetGovernanceIT` 3/3; BR-M03-18 phần giường (mức ảnh hưởng, chờ chuyển người) ở Phase 8)* *(2026-11-07 rà: giữ [ ] vì Q19 (tài sản giường tạo kèm lệnh Tạo giường, `recordIncreaseForBed` trả 503 chờ chốt bộ phận Chăm sóc) và Q24 (chuyển vị trí giường đã từng phân bổ, 409 `GIUONG_DA_PHAN_BO`, chờ cổng đồng bộ vị trí Q-243) chưa có hành vi thật để kiểm; Q18, Q20, Q21, Q25, Q26 và BR-M03-17, BR-M03-18 đã có test xanh)*
  - *(2026-11-11, A)* Q19 (`BR_M03_15_BedAssetIT.Q19_…`: Tạo giường ghi tăng tài sản giường, Sẵn sàng, bộ phận Chăm sóc mã `CARE`, vị trí phòng) và Q24 (`Q24_…`: lệnh `relocate` trên tài sản giường 409 `BED_HAS_ALLOCATION_HISTORY` khi giường đã từng phân bổ, `STATE_CHANGED` hướng sang sửa giường của 003 khi chưa) đỏ rồi xanh.

### Implementation cho User Story 4

- [x] T017 [US4] Tạo `MIG/V019_05__tai_san.sql`: `loai_tai_san`, `tai_san` (`ma UNIQUE`; `nguon` `MUA|TANG|CHUYEN|GIUONG_003`; `giuong_id NULL UNIQUE`; `trang_thai` `SAN_SANG|DANG_SU_DUNG|HONG|DANG_BAO_TRI|DA_THANH_LY`; dấu quá hạn), `tai_san_vi_tri` (nhóm 3; cột sinh `khoa_mo` `UNIQUE`), `tai_san_bo_phan`, `tai_san_bao_tri` (`loai` `BAO_TRI|KIEM_DINH|DANG_KIEM|BAO_HIEM`; `ket_qua` `DAT|KHONG_DAT` đặt một lần), `tai_san_hu_hong` (`muc_anh_huong` `MAT_AN_TOAN|KHONG_MAT_AN_TOAN`) (data-model mục 4) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `tai_san_vi_tri`, `tai_san_bo_phan` → `lich_su_chung`; `tai_san_bao_tri`, `tai_san_hu_hong` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.** *(2026-11-06: `V019_05__asset.sql` (bảng `asset`, hai loại hệ thống BED, VEHICLE; lịch sử ở bảng chung); chờ lượt 2 xác nhận đủ cột dấu và hạn; lượt 2: `V019_07__asset_disposal.sql` (`disposal_request_id`, `disposed_at`))* *(2026-11-07: rà `V019_05`, `V019_07` đủ cột dấu (`maintenance_overdue`, `vehicle_inspection_overdue`, `insurance_overdue`, `awaiting_resident_transfer`) và hạn theo data-model mục 4, `uq_asset_code`, `uq_asset_bed`, `CHECK` nguồn/giường/nguyên giá, trigger nguồn đặt một lần và trạng thái cuối; ràng buộc nhóm 3 của vị trí và bảo trì nằm ở `status_history`, `record_event` của 000, thêm `T017_AssetHistoryConstraintsIT` 3/3 (một khoảng vị trí mở, khoảng đã đóng và dòng lịch sử không sửa hoặc xóa, lần bảo trì đã ghi không sửa hoặc xóa, ghi kết quả lần hai 409); `RecordGroupTriggerIT` xanh. Không cần migration mới)*
- [x] T018 [US4] Cài `BE/roombedallocation/taisan/internal/TaiSanService.java` (bảng chuyển; gắn nguồn mua sắm qua `YeuCauMuaSamPort`; bảo trì thuê ngoài gắn yêu cầu), handler `M03.THANH_LY_TAI_SAN` (`choTuDuyet = false`), `DinhChinhDuocProvider` `M03.TAI_SAN_LICH_SU`, job dấu quá hạn; endpoint `/tai-san`, `/tai-san/loai`, `/tai-san/{id}`, `/tai-san/{id}/{lenh}`; màn web `frontend/src/tinh-nang/tai-san/` (research W-06) *(2026-11-06 lượt 1: `AssetService` (7 lệnh), `AssetTypeService`, `AssetController` (5 thao tác); còn thanh lý, job, đính chính, `GET /assets/{id}`, màn web; lượt 2: `maintenanceKind`, `nextDueOn` (bốn loại lần, gỡ dấu, hạn kế tiếp), thanh lý qua `AssetDisposalHandler` (`M03.ASSET_DISPOSAL`), `AssetReminders` và job `REMIND_ASSET_DUE_DATES`; còn đính chính (Phase 10), `GET /assets/{id}` và màn web (hoãn tới M35), lệnh giường (Phase 8))* *(2026-11-07: cài `AssetHistoryCorrectionProvider` `M03.ASSET_HISTORY` cho lần bảo trì và hư hỏng đã xác nhận ở `record_event` theo quy tắc chủ dự án chốt (chỉ ghi chú: nội dung, đơn vị, chi phí của lần bảo trì, mô tả hư hỏng, hoặc Hủy ghi nhận; không đổi hiện hành; Phụ trách bộ phận quản lý tài sản đính chính); `T018_AssetHistoryCorrectionIT` 5/5, đỏ 5/5 trước khi cài. Đính chính lịch sử vị trí, bộ phận (`status_history`, id riêng) hoãn theo chủ dự án chọn. Còn `GET /assets/{id}` và màn web (hoãn tới M35); lệch nghiep-vu 7.7 với FR-013 chưa sửa tài liệu)* *(2026-11-12: `AssetDetail` thêm trường mô tả của `AssetWrite` (chủ dự án duyệt 2026-11-12), màn "Sửa mô tả…" trong khung chi tiết (nút theo `allowedCommands` `update`) điền sẵn từ `GET /assets/{id}`, PUT kèm `If-Match`; bộ phận, nguồn, yêu cầu mua sắm khóa, nguyên giá gắn yêu cầu mua sắm khóa. `FR_013_AssetLifecycleIT` (ca FR_012 thêm khẳng định trường mô tả) và Vitest `FR_011_suaMoTa…` đỏ rồi xanh; `npm test` 980/980, lint, build xanh. Chưa kiểm trực quan trên trình duyệt (bản mẫu không có dữ liệu tài sản))*
  - *(2026-11-10, C; người dùng chọn thêm `GET /asset-types`, khung chi tiết dùng trường của danh sách)*: api-019 thêm `GET /asset-types` (schema `AssetType`, chờ chủ dự án duyệt), `AssetTypeService.list` (người có quyền bất kỳ của "Tài sản, lịch xe"), `AssetTypeResponse`; `FR_013_AssetLifecycleIT.FR_011_assetTypesAreListedWithSystemCodes` đỏ (405) → xanh, `ApiContractIT` xanh. Màn web `frontend/src/features/inventory/AssetsPage` (+ `AssetParts`, `assetLabels`, `FormParts`), menu `assets`; `AssetsPage.test.jsx` 6 ca đỏ (chưa có màn) → xanh; xem trên trình duyệt với dữ liệu giả lập. Giữ [ ]: `GET /assets/{id}` chưa có schema nên chưa có lịch sử vị trí, bảo trì, hư hỏng và đính chính trên màn; chưa sửa được mô tả (PUT cần `If-Match`, `Asset` không trả phiên bản); `link-bed-preparation-damage` không có nút (lệnh đi từ luồng dọn giường của 003); chưa chạy với backend thật.
  - *(2026-11-10, A)* `GET /assets/{id}` trả `AssetDetail` (lịch sử vị trí, chuyển bộ phận, bảo trì, hư hỏng kèm đã đính chính/Hủy ghi nhận; `AssetQueries`), `Asset` có `version`, `allowedCommands`; màn web nút theo `allowedCommands`, khung chi tiết có lịch sử. `FR_013_AssetLifecycleIT.FR_012_detail…`, `AssetsPage.test.jsx` ca lịch sử đỏ rồi xanh. Giữ [ ]: sửa mô tả cần `Asset` trả `source`, `acquisitionCost`, chu kỳ, hạn (PUT nhận đủ `AssetWrite`) để điền lại form; ghi tăng tài sản giường (`recordIncreaseForBed` 503, chưa có cách xác định bộ phận Chăm sóc).

---

## Phase 8: User Story 5 – Giường là tài sản: trạng thái đi cùng trạng thái giường (Priority: P2)

**Goal**: Tài sản giường tạo kèm lệnh Tạo giường (`TaiSanGiuongPort.ghiTangChoGiuong`); báo hỏng/bảo trì/thanh lý giường đi qua `GiuongTheoTaiSanPort` cùng transaction; giường có người → dấu "chờ chuyển người"; mất an toàn gửi Khẩn cấp (FR-011, FR-015, BR-M03-15, DBR-32).

**Independent Test**: G5 trống và G6 có người; báo hỏng cả hai → G5 Đang bảo trì, G6 giữ nguyên có dấu chờ chuyển người; kết quả bảo trì Đạt cho G5 → Trống.

### Tests cho User Story 5

- [x] T019 [P] [US5] Test đơn vị `BT/roombedallocation/AnhXaTrangThaiGiuongTest.java` và `BT/roombedallocation/BR_M03_15_GiuongTaiSanIT.java` (SC-004), `DBR_32_TaiSanGiuongIT.java` (báo hỏng cùng lúc với phân bổ) (Q20 → Q23) *(2026-11-06 lượt 1: `BedAssetRulesTest` 3/3 (ánh xạ trạng thái, người nhận Q-245, Q-256), `BR_M03_15_BedAssetIT` 11/11 (US5 kịch bản 1, 2, 3, 4, 6, 7; FR-013b, FR-016, Q-242), đỏ trước khi cài; DBR-32 báo hỏng đồng thời với phân bổ và nhắc CFG-M03-02 ở lượt 2; lượt 2: `BR_M03_15_BedAssetIT` thêm 3 ca (nhắc chuyển người, nhắc báo hỏng lại, đếm giường Trống mang dấu theo thời điểm cho 016), 14/14; DBR-32 kiểm và hoàn tất ở Phase 10 bên dưới)*
  - Phase 10 (2026-11-06): bổ sung `DBR_32_BedAssetConcurrencyIT` (NOT_SUPPORTED, hai transaction thật, điều phối allocation commit giữa lần đọc đầu và khóa giường); đỏ `CannotAcquireLockException` trước khi thêm retry quanh transaction `AssetService.reportDamage`, sau sửa nhóm DBR-32 + ba IT chuyến 014 xanh 50/50. Các test đơn vị, BR_M03_15_BedAssetIT và DBR-32 đều qua trong lượt kiểm cuối; đánh dấu task. Toàn bộ build còn lỗi fixture StaffCommandsHttpIT của E đã có ở mục 8.

### Implementation cho User Story 5

- [x] T020 [US5] Cài `BE/roombedallocation/taisan/internal/TaiSanGiuongPortImpl.java` (`ghiTangChoGiuong`, `mangDauChoChuyenNguoi`, `giuongMangDau`) và `AnhXaTrangThaiGiuong` nối lệnh tài sản của giường với `GiuongTheoTaiSanPort` trong `TaiSanService.java`; màn báo hỏng giường hiện tiêu chí "mất an toàn" với test Vitest *(2026-11-06 lượt 1: `BedAssets` (cài `BedAssetPort`; `recordIncreaseForBed` trả 503 chờ chủ dự án chốt bộ phận Chăm sóc), `BedAssetCommands` (003 cài `BedAssetCommandPort`, `residentTransferFlagAdded(bedId)` theo chủ dự án chọn), `BedAssetRules`; `AssetService` nối báo hỏng, đưa vào bảo trì, kết quả, thanh lý giường, `remove-transfer-flag`, `link-bed-preparation-damage`; lượt 2: nhắc CFG-M03-02 trong `AssetReminders`, `flaggedBedsAt`, `bedState`, `BedStatusApi.countEmptyAwaitingTransfer` cho 016; còn chuyển vị trí giường (Q-243, tạm hoãn), ghi tăng kèm Tạo giường, màn web (hoãn tới M35))*
  - *(2026-11-11, A; chủ dự án duyệt mã bộ phận `CARE` 2026-11-11)* `BedAssets.recordIncreaseForBed` ghi tăng tài sản giường kèm lệnh Tạo giường (bộ phận qua `DutyApi.departmentByCode("CARE")`, 008 cài; `V008_96` gắn mã cho bộ phận "Chăm sóc"); `BedAssetCommandPort.everAllocated` cho Q-243; báo hỏng giường có mức ảnh hưởng ở màn web (`AssetsPage.test.jsx` FR_015). `BR_M03_15_BedAssetIT` 4, `FR_013_AssetContractsTest`, `FR_011_BedCommandsIT`, `DBR_32_BedAssetConcurrencyIT`, `DutyDirectoryIT`, `DepartmentCommandsIT`, mọi `*ArchTest` xanh.

---

## Phase 9: User Story 6 – Đặt lịch dùng xe đưa đón (Priority: P2)

**Goal**: Lịch xe không chồng giờ dưới khóa xe, xe không ở bảo trì/hỏng/thanh lý; `LichXeApi.coLichXe` cho điều kiện rời viện của 014; `LichXeLenhApi` chuyển trạng thái kèm lệnh 014 (FR-017, FR-018, BR-M03-16).

**Independent Test**: Hai lịch chồng giờ trên XE-01 (bị chặn); lịch cho xe đang bảo trì (bị chặn); điểm danh rời viện chuyến chưa có lịch xe (bị chặn ở 014).

### Tests cho User Story 6

- [x] T021 [P] [US6] Test đơn vị `BT/roombedallocation/KiemTraDatLichTest.java` và `BT/roombedallocation/BR_M03_16_LichXeIT.java` (SC-005; hai lịch cùng giờ đồng thời), `FR_018_TheoChuyenIT.java` (Q27 → Q32) *(2026-11-06: `VehicleBookingCheckTest` 4, `BR_M03_16_VehicleScheduleIT` 7 (gồm SC-005 đồng thời, provider T004), `FR_018_FollowTripIT` 6; đỏ trước khi cài)*

### Implementation cho User Story 6

- [x] T022 [US6] Tạo bảng `lich_xe` (`muc_dich` `CHUYEN_DI|DI_KHAM|CHUYEN_VIEN_KHONG_CAP_CUU|KHAC`; `trang_thai` `DA_DAT|DANG_DUNG|DA_HOAN_THANH|DA_HUY`) trong `MIG/V019_06__lich_xe.sql`; cài `BE/roombedallocation/lichxe/internal/LichXeService.java` (`KiemTraDatLich`; `LichXeApi`, `LichXeLenhApi`; báo người lái có tài khoản; `DinhChinhDuocProvider` `M03.LICH_XE`) và endpoint `/lich-xe`, `/lich-xe/{id}/{lenh}`; màn web `frontend/src/tinh-nang/lich-xe/` (research W-07) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lich_xe` → `ho_so_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.** *(2026-11-06: `simple_record` loại `M03.VEHICLE_SCHEDULE` (không có migration), `VehicleScheduleService` (cài `VehicleScheduleApi`, `VehicleScheduleCommandApi`), `VehicleBookingCheck`, ba thao tác `/vehicle-schedules`, job `REMIND_VEHICLE_SCHEDULES`; còn đính chính `M03.VEHICLE_SCHEDULE` (Phase 10) và màn web (hoãn tới M35))* *(2026-11-07: cài `VehicleScheduleCorrectionProvider` `M03.VEHICLE_SCHEDULE` cho lịch Đã hoàn thành theo quy tắc chủ dự án chốt: sửa giờ đi/về dự kiến, xuất phát/trả thực tế, người lái, ghi chú hoặc Hủy ghi nhận; đổi giờ dự kiến kiểm lại chồng giờ (409 `VEHICLE_SCHEDULE_OVERLAP`) và lịch gắn chuyến phát `VehicleScheduleCorrected` cho 014; `T022_VehicleScheduleCorrectionIT` 6/6, đỏ 6/6 trước khi cài. Chỉ còn màn web (hoãn tới M35); 014 chưa nghe sự kiện, ghi team-plan mục 8)* *(2026-11-10, A rà: màn web `features/inventory/VehicleSchedulesPage` đã vào `develop` qua PR #154; `VehicleSchedulesPage.test.jsx` cùng `AssetsPage.test.jsx` 11/11 xanh)*
  - *(2026-11-10, C, bố cục theo duyệt 09/11 mục (8))*: màn web `frontend/src/features/inventory/VehicleSchedulesPage` (+ `VehicleScheduleParts`, `vehicleLabels`), menu `vehicles` "Lịch xe": lịch theo tuần (from, to giờ Việt Nam), lọc trạng thái, đặt lịch (Trưởng tầng, Hành chính, Quản lý viện), Ghi xuất phát, Dời giờ, Hủy lịch (lý do), Ghi trả xe; tên xe từ `GET /assets` lọc loại Xe (`GET /asset-types`), người lái từ `GET /staff`. `VehicleSchedulesPage.test.jsx` 5 ca đỏ (chưa có màn) → xanh; `npm test` 952/952, lint, build xanh; xem trên trình duyệt với dữ liệu giả lập. Giữ [ ]: nút lệnh theo vai trò chưa phân biệt người đặt (`/me` không có mã nhân viên, api-019 chưa có `allowedCommands`); đính chính lịch Đã hoàn thành (`M03.VEHICLE_SCHEDULE`) chưa gắn vào màn; chưa chạy với backend thật.

**Checkpoint**: Mọi user story của 019 chạy độc lập.

---

## Phase 10: Polish & Cross-Cutting

- [x] T023 [P] Cài `TaiSanThongKeApi`, `MuaSamThongKeApi` (016); mở rộng `BT/common/HopDongApiTest.java` với `specs/019-inventory-assets/contracts/api-019.yaml`; mở rộng `NhomGhiNhanCoTriggerIT`
  - Phase 10 (2026-11-06): bản thật `AssetStatisticsService`/`AssetStatisticsStore` cho phần tài sản; ngày on dùng dấu/hạn hiện tại và dueOn < on (chủ dự án xác nhận), count GROUP BY tại DB. T023 đỏ 3/3 trước cài, sau cài xanh 3/3; ca bổ sung view IN_USE/COMPLETED cũng xanh, tổng T023 4/4 trong clean verify cuối. Kiến trúc 58, ApiContractIT 1, RecordGroupTriggerIT 6 xanh. Phần ProcurementStatisticsApi còn chờ A xác nhận khoảng CP.MUA_SAM nên task giữ mở.
  - *(2026-11-08, C)* `ProcurementStatisticsService` (billing/internal/procurement/service) và hai truy vấn ở `ProcurementRequestStore`: khoảng theo ngày lập (A xác nhận 2026-11-07, progress.md), `created_at` nửa mở `[from, to+1)` theo `BusinessTime.ZONE` để dùng chỉ mục (department_id, created_at); `departmentIds` rỗng là toàn viện; `totals` COUNT/SUM GROUP BY (bộ phận, trạng thái) tại DB, mọi trạng thái, tiền chưa có tính 0; `byDepartment` trả luồng từ danh sách đã đọc xong (kết nối trả lại trước khi hàm trả về). Javadoc API bỏ "chờ A xác nhận". `T023_ProcurementStatisticsIT` (1 ca, dùng context `ProcurementTestBase`) đỏ 2/2 NoSuchBeanDefinitionException trước cài, xanh sau cài, gộp còn 1/1 xanh vì toàn kho IT đã vượt trần 400; kiến trúc 74 (gồm ModuleBoundaryTest, LayerArchTest, T002 hợp đồng), ApiContractIT, RecordGroupTriggerIT, BR_M11_17, BR_M11_19 xanh. `T023_AssetStatisticsIT` cũ đã bỏ ở đợt rút gọn test d56a6ac3: phần tài sản hiện chỉ còn T002 kiểm chữ ký. 016 chưa dùng `ProcurementStatisticsApi` (không có mã, test nào).
- [x] T024 Thay bản giả `TaiSanGiuongPort` (003 T002), `YeuCauThuocCungUngPort` (006 T062, bổ sung 2026-10-11) bằng bản cài của 019 và chạy lại test 003, 011, 014 (lịch xe), 006 US9
  - Phase 10 (2026-11-06): ba lớp IT chuyến 014 dùng lịch xe thật; ca FR-019 kiểm rời viện chuyển lịch sang IN_USE. Ca kiểm rollback dùng spy chủ động gây lỗi API lệnh, dữ liệu lịch và API đọc vẫn thật. Cổng giường thật được kiểm ở BR_M03_15_BedAssetIT và DBR_32_BedAssetConcurrencyIT; giữ fake ở các test cô lập đặt riêng điều kiện. Phần thuốc/Đã nhận còn chờ T027/T028, không thay bằng API chưa được duyệt.
  - *(2026-11-10, C)*: phía 019 đã có bản cài (`ProcurementMedicationLines` thay hướng `YeuCauThuocCungUngPort`, `ProcurementIntakePort` khai kèm bản giả chung). Test 006 US9 (`BR_M07_24_SuppliedMedicationIntakeIT`) chạy lại xanh 2/2 nhưng vẫn dùng `FakeProcurementMedicationPort` của 006, vì `SuppliedMedicationIntakeService` chưa đổi sang `ProcurementMedicationLineApi` (mã của D). Giữ [ ]: chờ D đổi rồi chạy lại 006 US9 với bản cài thật của 019.
  - *(2026-11-11, A thay D)*: bean thật cả hai chiều do M11 cài (module-interfaces mục "Đảo chiều", sửa 11/11): `MedicationProcurementPort` (006 đọc dòng đã ghi đã mua), `MedicationIntakes` ("Đã nhận" từ kết quả tiếp nhận của 006). `T024_MedicationProcurementPortIT` (bean thật, không bản giả) và `T024_MedicationIntakesTest` đỏ rồi xanh; 006 US9 `BR_M07_24_SuppliedMedicationIntakeIT` 2/2, `FR_008d_MedicationRequestIT`, `ModuleBoundaryTest`, `*ArchTest` 77 xanh. Test nghiệp vụ US9 vẫn đặt dữ liệu dòng bằng `FakeProcurementMedicationPort`; bean thật kiểm ở IT T024.
- [x] T025 *(2026-11-13: chủ dự án (A) nhận tự nghiệm thu kịch bản quickstart và đo SC bằng demo tay, đánh dấu theo yêu cầu của A; phần test tự động theo `clean verify`, `npm test`, `flutter test` ngày 2026-11-12 trên `impl/acceptance-2026-11-12`, ghi ở docs/progress.md)* Chạy `./mvnw verify`, `npm test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
  - Phase 10 (2026-11-06): `./mvnw -o clean verify -Dit.forkCount=3`: BUILD FAILURE do StaffCommandsHttpIT của E (409 INVALID_DEPARTMENT, khớp dòng chờ mục 8) (1131 đơn vị/kiến trúc xanh; 2799/2802 IT xanh, 1 thất bại, 2 bỏ qua); `npm test` xanh 512/512 (86 file). Giữ task mở: gate toàn bộ chưa xanh; kịch bản/đo SC có màn web và các tích hợp còn chờ chưa xác nhận đầy đủ.
- [x] T026 Rà truy vết: DBR-32, 36 và BR-M03-15 → 18, BR-M11-17 → 20, FR-008d (bổ sung 2026-10-11) có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-019, FR-020, FR-021 — mỗi FR có task hoặc test tương ứng (constitution XV)
  - Phase 10 (2026-11-06): bảng truy vết DBR/BR/FR ở plan mục Phase 10; bổ sung DBR-32 và `FR_013_AssetDisposalTransactionIT` (hai ca duyệt → áp dụng thật không transaction test, chạy riêng 2/2). FR-008d phần adapter dòng thuốc/Đã nhận còn chờ T027/T028; chưa tuyên bố bao phủ phần hợp đồng chưa chốt.
  - *(2026-11-12, A, rà tự động: tìm mã quy tắc trong tên và nội dung test backend, web, app)* Đủ: DBR-32, DBR-36, BR-M03-15 → 18, BR-M11-17 → 20, FR-008d, FR-019, FR-020, FR-021 đều có test mang mã. Đóng.
- [x] T029 *(bổ sung 2026-10-29, research R-15 bản 2 của 000)* Khi viết migration và entity của feature, áp data-model mục 0: không tạo các bảng đã gộp, dùng bảng gộp, cột `JSON` và bảng chung (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`); test bảo vệ nhóm dữ liệu viết cho bảng gộp *(2026-10-30, audit cơ sở dữ liệu F-04, PR #63)* Mọi bảng tạo hoặc gộp khai nhóm dữ liệu: entity bằng `@DataGroup`, bảng JDBC bằng `COMMENT = 'DataGroup=CATALOG|BUSINESS|RECORD|MULTIPLE|TECHNICAL'` (backend/README.md mục 4); bảng `RECORD` có `trg_<bảng>_no_update`, `trg_<bảng>_no_delete`; bảng chứa nhóm 2 và nhóm 3 dùng `@DataGroup(MULTIPLE)`, cột `locked_at`, trigger `trg_<bảng>_row_lock`; dòng bảng chung đặt `entity_type` trùng `RetentionProvider.dataType()` của bản ghi cha; job dùng `SchedulerRuns` (module-interfaces 000 mục 1.8). `RecordGroupTriggerIT` kiểm.
  - Phase 10 (2026-11-06): đã rà hai bảng nghiệp vụ, bảng chung, entity_type, tiền, trigger và SchedulerRuns, ghi chi tiết ở plan. `RecordGroupTriggerIT` hiện có kiểm đủ nhóm và trigger tổng quát, chạy lại 6/6 xanh; không sửa test của 000. Nhóm dữ liệu và các module bị ảnh hưởng xanh; clean verify cuối BUILD FAILURE do StaffCommandsHttpIT của E (409 INVALID_DEPARTMENT, khớp dòng chờ mục 8) (1131 đơn vị/kiến trúc xanh; 2799/2802 IT xanh, 1 thất bại, 2 bỏ qua).

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
