---
description: "Danh sách task triển khai feature 016 – Báo cáo và dashboard"
---

# Tasks: Báo cáo và dashboard (016)

**Input**: Tài liệu thiết kế ở `specs/016-reports-dashboard/` (plan.md, spec.md, research.md O-01 → O-13, data-model.md, contracts/api-016.yaml, contracts/chi-tieu.md, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 (phạm vi, `boLocLichSu`, thu hẹp `DASHBOARD_BAO_CAO`), 008 (lịch sử ca) đã xong; các module nguồn đã cài `*ThongKeApi`: 001 `NguoiCaoTuoiThongKeApi`, 003 `TinhTrangGiuongApi`, `GiuongThongKeApi`, 004 `LuuTruThongKeApi`, 005 `CongViecThongKeApi`, 006 `ThuocThongKeApi`, 007 `CanhBaoThongKeApi`, `ChiSoThongKeApi`, 008 `NhanSuThongKeApi`, 010 `ChiPhiThongKeApi`, 011 `DinhDuongThongKeApi`, 014 `HoatDongThongKeApi`, 015 `XoayCaThongKeApi`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/eldercare/backend/`, `BT/` = `backend/src/test/java/com/eldercare/backend/`
- Module M14: `BE/reporting/{chitieu,phamvi,dashboard,baocao,chitiet,xuat}/`; không có bảng riêng (data-model mục 1)
- Web `frontend/src/tinh-nang/bao-cao/{dashboard,nguoi-cao-tuoi,cham-soc,chat-luong,suc-khoe-thuoc,chi-phi,nhan-su-ca,chi-tiet}/`

---

## Phase 1: Setup

- [x] T001 Tạo khung gói `BE/reporting/{chitieu,phamvi,dashboard,baocao,chitiet,xuat}/`; ArchUnit `BT/reporting/M14ArchTest.java`: cấm gọi API lệnh của module khác, cấm repository ghi; mọi service `@Transactional(readOnly = true)` trừ ghi nhật ký xuất (research O-01, O-09) *(2026-10-24: gói theo R-02 `reporting.internal.{metric, scope}`, `dashboard`, `report`, `detail`, `export` tạo cùng lớp của US; luật ArchUnit: không `jdbc`, `data`, `jakarta.persistence`, `ApplicationEventPublisher`, không lớp `*Command*`, `*Port` của module khác, service ngoài `export` gắn `@Transactional(readOnly = true)`)*
- [x] T002 [P] Thêm thư viện tạo XLSX (fastexcel, đã chọn ở 010; sửa 2026-10-22 cho khớp research O-08) vào `backend/pom.xml` nếu 010 chưa thêm *(2026-10-22: fastexcel, fastexcel-reader 0.20.2 đã thêm cùng `common.document`, 007 T104)*

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khung chỉ tiêu (hàm tính và hàm chi tiết dùng cùng tập bản ghi), phạm vi báo cáo lọc hai bước, quyền nhóm chỉ tiêu, giới hạn trường.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T003 Tạo mô hình đọc `BE/reporting/chitieu/ChiTieu.java`, `GiaTriChiTieu.java` (nhãn `KHONG_CO_DU_LIEU`, `CHUA_CAU_HINH`, `NGAY_CHUA_KET_THUC`, `CHO_XEM_LAI:n`, `LOAI_BO_LUU_TRU:loai`), `DongChiTiet.java` và khung đăng ký chỉ tiêu cố định trong mã: mỗi mã ở `contracts/chi-tieu.md` có hàm tính thuần và hàm chi tiết trên cùng tập bản ghi đã lọc (data-model mục 2; research O-05; FR-003, FR-009) *(2026-10-24: `Metric` (phép `Count`, `Ratio`, `Aggregate`), `MetricValue`, `DetailRow`, `MetricRecord`, `MetricCatalog`; tỷ lệ là phần trăm một chữ số thập phân; nhãn `DAY_NOT_ENDED`, `PURGED_BY_RETENTION`, `NOT_CONFIGURED`, `PROVISIONAL` gắn ở tầng báo cáo; test `MetricTest`)*
- [x] T004 Cài `BE/reporting/phamvi/PhamViBaoCao.java` (dựng từ `PhamViApi.boLoc`/`boLocLichSu` của 002 và lịch sử ca 008; lọc hai bước: lấy tập bản ghi theo phạm vi thô rồi lọc theo khoảng thời gian của từng phạm vi) và `QuyenNhomChiTieu.java` (nhóm quyền FR-010; thu hẹp `DASHBOARD_BAO_CAO` theo `nhom_chi_tieu`, `kenh`) với test `BT/reporting/PhamViBaoCaoTest.java` (research O-03) *(2026-10-24: xong bảng FR-010 `MetricGroupAccess`, `ReportScope`, `ReportScopeService` (phạm vi theo ca từ `AssignmentApi.history` của 008, ± CFG-M15-07), test `ReportScopeTest`, `MetricGroupAccessTest`; thu hẹp chi tiết theo `metric_group`, `channel` qua `PermissionApi.metricGroupRestrictions` ("Đổi api" 002, chủ dự án duyệt 2026-10-24), áp theo vai trò trước khi hợp rồi theo tài khoản; dòng rút gọn xét `FieldVisibilityApi.canViewByRole`; test 002 `MetricGroupRestrictionsIT`)*
- [x] T005 [P] Cài giới hạn trường cho DTO và cột file (`@NhomTruong`; tên người cao tuổi rút gọn Q-196; tên thuốc ẩn với vai trò không xem) (research O-04) *(2026-10-24: `DetailRowPolicy` dùng chung cho màn hình và file: dòng rút gọn Q-196, kết quả theo nhóm thông tin, mã vật phẩm thay tên thuốc; `@FieldGroup` trên DTO phản hồi thêm cùng controller ở US; test `DetailRowPolicyTest`)*
- [x] T006 [P] Bổ sung kiểm CFG-M14-01 (khoảng tối đa) dùng chung cho báo cáo và xuất (FR-060) *(2026-10-24: `ReportPeriods`, lỗi 422 `RANGE_EXCEEDS_MAXIMUM`; test `FR_060_ReportPeriodTest`)*

**Checkpoint**: Khung báo cáo sẵn sàng.

---

## Phase 3: User Story 1 – Dashboard trong ngày cho Quản lý viện và trưởng tầng (Priority: P1) 🎯 MVP

**Goal**: Dashboard theo vai trò cho ngày và ca hiện tại; khối tính song song; bộ đệm 20 giây; làm mới 30 giây bằng ETag (FR-020, FR-021, SC-002, SC-009).

**Independent Test**: Dữ liệu một ngày cho 2 tầng (40 công việc, 3 Quá hạn, cảnh báo mở có dấu leo thang tối đa, sự cố…) → số trên dashboard đúng theo vai trò.

### Tests cho User Story 1

- [ ] T007 [P] [US1] *(2026-11-08, A: thêm `MetricAssetOverdueTest` cho `ASSET.OVERDUE`)* *(2026-10-27: xong phần giường `MetricBedStatusTest`; các mã khác chờ nguồn)* Test `ChiTieu_*Test` cho các mã dashboard (`NCT.DANG_LUU_TRU`, `GIUONG.TRANG_THAI`, `CV.HOM_NAY`, `CV.QUA_HAN`, `HD.*`, `SK.CANH_BAO`, `SK.SU_CO`…) trong `BT/reporting/chitieu/` (SC-001; Q14 → Q16); test hiệu năng dashboard 60 người dùng (SC-002) trong `BT/reporting/DashboardHieuNangIT.java`
  - *(2026-11-11, A, rà lại)* Test mã dashboard đã đủ; còn test hiệu năng dashboard 60 người dùng (SC-002), chưa có lớp test, chuyển cùng nghiệm thu đo SC.
  - *(2026-11-11, A, luồng C, rà lại)* Thêm `ASSET.DUE_SOON` có test (`MetricAssetOverdueTest`); còn đo SC-002 (60 người dùng) cùng nghiệm thu. Giữ [ ].

### Implementation cho User Story 1

- [x] T008 [US1] *(2026-11-08, A: xong `ASSET.OVERDUE` (dấu quá hạn bảo trì, đăng kiểm, bảo hiểm theo `AssetStatisticsApi.overdue` của 003), `HEALTH.CRITICAL_FLAGS`; phần "tới mốc nhắc CFG-M03-09" chưa có nguồn)* *(2026-10-27: xong `BED.STATUS`, `BED.USABLE`, `BED.ISOLATED_ROOMS`; `BED.EMPTY_AWAITING_TRANSFER` chờ 019; mã khác chờ nguồn)* Cài hàm tính và chi tiết cho các chỉ tiêu dashboard trong `BE/reporting/chitieu/` gọi `*ThongKeApi` tương ứng (contracts/chi-tieu.md)
  - *(2026-11-11, A)* Thêm `BED.EMPTY_AWAITING_TRANSFER` (`EmptyAwaitingTransferMetric`, nguồn `BedStatisticsApi.emptyAwaitingTransferBedIds`, tầng theo phòng), `BALANCE.LOW_OR_OWING` (017), `REGISTRATION.TO_DO` (018); `MetricRemainingSourcesTest` xanh. Còn phần "tới mốc nhắc CFG-M03-09" của `ASSET.OVERDUE` (003 chưa trả).
  - *(2026-11-11, A, rà lại)* Còn phần "tới mốc nhắc CFG-M03-09" của `ASSET.OVERDUE`: `AssetStatisticsApi` của 019 chỉ trả dấu quá hạn; cần thêm hàm đọc tài sản sắp tới mốc nhắc (đổi api 019), chờ chủ dự án.
  - *(2026-11-11, A, luồng C)* Xong phần "tới mốc nhắc CFG-M03-09": "Đổi api" 019 `AssetStatisticsApi.dueSoon` (bản thật `AssetStatisticsStore.dueSoon`, bản giả `FakeAssetStatistics.dueSoon`), chỉ tiêu riêng `ASSET.DUE_SOON` (`TS.TOI_MOC_NHAC`, chia theo loại hạn), nhãn ô web. `T008_AssetDueSoonStatisticsIT`, `MetricAssetOverdueTest`, `T002_AssetProcurementApiContractTest`, `reportLabels.test.js` xanh (bước đỏ là lỗi biên dịch, test viết trước nhưng không chạy riêng trước khi cài). Đóng.
- [x] T009 [US1] *(2026-10-24: `DashboardService` theo nhóm của bảng FR-020, bộ đệm 20 giây theo (nhóm, phạm vi), ETag; chạy tuần tự, ghi chú ở plan; `GET /dashboard`, `GET /metrics`; test `FR_020_DashboardIT`)* Cài `BE/reporting/dashboard/internal/DashboardService.java` (khối theo vai trò, song song, bộ đệm trong tiến trình 20 giây, ETag) và `BE/reporting/dashboard/web/DashboardController.java` (`GET /dashboard`, `GET /chi-tieu`) (research O-06)
- [x] T010 [P] [US1] *(2026-10-24: bố cục duyệt, DESIGN.md 5.21.0; `src/features/dashboard/DashboardPage.jsx` thay bản mẫu Tổng quan; test `DashboardPage.test.jsx`)* Tạo màn web `frontend/src/tinh-nang/bao-cao/dashboard/` (định nghĩa chỉ tiêu tại chỗ; "—" khi mẫu số 0; làm mới 30 giây với ETag) với test Vitest

**Checkpoint**: Dashboard chạy độc lập.

---

## Phase 4: User Story 2 – Mọi số liệu tuân theo phạm vi và giới hạn trường (Priority: P1)

**Goal**: Phạm vi theo thời gian (trưởng tầng giao tạm, đã thôi), không lộ ngoài phạm vi, bộ lọc chỉ giá trị trong phạm vi, giới hạn trường (FR-010 → FR-017, FR-014, FR-042a, SC-004, SC-011).

**Independent Test**: Bộ 11 tài khoản kiểm thử quyền (QLV; trưởng tầng tầng 1; trưởng tầng giao tạm tầng 3 có thời hạn; trưởng tầng đã thôi…) → mỗi người chỉ thấy đúng phạm vi.

### Tests cho User Story 2

- [x] T011 [P] [US2] *(2026-11-08, A: `FR_042a_AdminIncidentIT` chạy trên chỉ tiêu thật `HEALTH.NON_HEALTH_INCIDENTS`, `HEALTH.INCIDENTS` (nguồn 007 thay bằng danh sách trong test), thêm Q7 đổi loại sự cố; còn `FR_014_FieldRestrictionIT` (SC-005) chờ `CostStatisticsApi` của 010)* *(2026-11-01: xong `FR_012_TimeScopeIT` trên ca, phân công thật của 008, `SC_004_ScopeIT` bộ 11 tài khoản, `FR_014_FieldRestrictionIT` gồm cả `SC_005`; `FR_042a_AdminIncidentIT`, `SC_005` chỉ kiểm khung bằng chỉ tiêu giả `TestScopedMetrics`; còn chạy lại với chỉ tiêu thật khi 007 có phần đọc sự cố, 010 có `ChiPhiThongKeApi`, và Q7 đổi loại sự cố chờ 007)* Test `BT/reporting/FR_012_PhamViTheoThoiGianIT.java` (SC-011), bộ 11 tài khoản `BT/reporting/SC_004_PhamViIT.java`, `FR_014_GioiHanTruongIT.java`, `FR_042a_SuCoHanhChinhIT.java`, `SC_005_TenThuocIT.java` (Q3 → Q11)
  - *(2026-11-11, A, rà lại)* 010 đã có nguồn chi phí thật (`COST.*`, 2026-11-11); còn chạy lại `FR_014_FieldRestrictionIT` (SC-005) trên chỉ tiêu chi phí thật thay chỉ tiêu giả, thuộc đợt chạy lại test với bean thật.
  - *(2026-11-11, A)* `FR_014_FieldRestrictionIT.SC_005_realCostMetric_…`: khoản loại Thuốc trên `COST.SUMMARY` thật, Hành chính và Kế toán chỉ thấy mã vật phẩm ở danh sách chi tiết, file xuất không có tên thuốc, hàm lượng (file xuất chỉ có giá trị tổng hợp, hai người để dòng không bị bỏ theo FR-061). Xanh.

### Implementation cho User Story 2

- [x] T012 [US2] *(2026-11-01: phạm vi theo từng ô của nhóm, ca thật sự thuộc về, việc chung của ca, báo Quản lý viện khi lỗi nguồn; `filterOptions` cho tầng/khu; giá trị lọc nhân viên, người cao tuổi làm cùng chỉ tiêu có các cột này, hiện lọc chỉ thu hẹp trong phạm vi)* Nối `PhamViBaoCao`, `QuyenNhomChiTieu`, giới hạn trường vào mọi đường đọc của `DashboardService` và khung báo cáo; bộ lọc trả chỉ giá trị trong phạm vi

**Checkpoint**: P1 hoàn tất.

---

## Phase 5: User Story 3 – Báo cáo người cao tuổi (Priority: P2)

**Goal**: `NCT.TAI_NGAY`, `NCT.BIEN_DONG`, `NCT.DANH_SACH_CHO`, `GIUONG.*` theo khoảng (FR-022 → FR-025, FR-033).

**Independent Test**: 120 hồ sơ với trạng thái, loại lưu trú, mức khác nhau; 5 người đổi mức, 3 người chuyển tầng trong tháng 10 → báo cáo đúng.

### Tests cho User Story 3

- [x] T013 [P] [US3] *(2026-11-08, A: `MetricResidentsTest` 3 ca cho `RES.IN_RESIDENCE`, `RES.AT_DATE` (loại lưu trú theo hợp đồng tại ngày, chưa có hợp đồng là "chưa xác định", lọc theo loại), `RES.MOVEMENTS`)* *(2026-10-27: xong `ChiTieu_GIUONG_*` = `MetricBedStatusTest`; `NCT` chờ 001)* Test `ChiTieu_NCT_*Test`, `ChiTieu_GIUONG_*Test` trong `BT/reporting/chitieu/`
  - *(2026-11-11, A, rà lại)* `MetricResidentsTest` (RES.*), `MetricBedStatusTest` (BED.*) có và xanh. Đóng.

### Implementation cho User Story 3

- [ ] T014 [US3] *(2026-11-08, A: xong `RES.IN_RESIDENCE`, `RES.AT_DATE`, `RES.MOVEMENTS` trên `ResidentStatisticsApi` (001), `ResidencyStatisticsApi.residenceTypeAtDate` (004) thật; còn phần có mặt bán trú (005) của FR-020, đổi loại lưu trú của FR-023, chia theo mức chăm sóc (bộ lọc `careLevelId` là mã số, 001 trả enum), xem plan "Chỉ tiêu người cao tuổi, công việc, bữa ăn, hoạt động, kiểm tra")* *(2026-10-27: xong `BED.STATUS`, `BED.OCCUPANTS` của FR-024 tại ngày đã qua; 2026-10-24: xong phần khung `GET /reports/{group}` (`ReportController`, `ReportService`, test `ReportApiIT`) và màn `src/features/reports/` dùng chung cho mọi nhóm; còn chỉ tiêu nhóm người cao tuổi, giường chờ `*ThongKeApi` của 001, 003, 004)* Cài chỉ tiêu nhóm người cao tuổi, giường và `BE/reporting/baocao/internal/BaoCaoService.java` + `BE/reporting/baocao/web/BaoCaoController.java` (`GET /bao-cao/{nhom}`) (research O-07); màn web `frontend/src/tinh-nang/bao-cao/nguoi-cao-tuoi/`
  - *(2026-11-11, A, rà lại)* Còn ba phần: (1) có mặt bán trú của 005 trong `RES.IN_RESIDENCE` (FR-020); (2) đổi loại lưu trú trong `RES.MOVEMENTS` (FR-023); (3) bộ lọc mức chăm sóc nhận mã số trong khi 001 trả enum. Chưa làm.
  - *(2026-11-11, A)* Xong phần có mặt bán trú của FR-020: `RES.DAY_CARE_ATTENDANCE` (`NCT.BAN_TRU_CO_MAT`, 005 `attendanceInRange` tại ngày cuối khoảng, chia theo trạng thái có mặt); web nhãn ô. `MetricResidentsTest.FR_020_dayCare…` xanh (bước đỏ là lỗi biên dịch, không chạy riêng); Vitest đỏ rồi xanh. Còn chờ chủ dự án: đổi loại lưu trú của FR-023 cần hàm mới ở 004 `ResidencyStatisticsApi`; bộ lọc mức chăm sóc cần đổi `careLevelId` (số) của api-016 sang mã `CareLevel` hoặc có danh mục mức có id.

---

## Phase 6: User Story 4 – Báo cáo chăm sóc và chất lượng (Priority: P2)

**Goal**: `CV.*`, `BUA.*`, `HD.*`, `KT.*` (FR-030 → FR-037).

**Independent Test**: Tháng 10 tầng 1: 1.200 công việc (60 Bắt buộc: 54 Hoàn thành, 4 trễ, 2 Không thực hiện), 15 ghi nhận muộn, 40 mục kiểm tra → số đúng.

### Tests cho User Story 4

- [x] T015 [P] [US4] *(2026-11-08, A: `MetricCareTasksTest` 4 ca (`TASK.*`), `MetricMealActivityQualityTest` 4 ca (`MEAL.*`, `ACT.*` trừ `ACT.ISOLATION_RISK`, `QC.*`))* *(2026-10-27: xong thời gian giường Chờ sẵn sàng `MetricBedAwaitingReadyTest`; `CV`, `BUA`, `HD`, `KT` chờ nguồn)* Test `ChiTieu_CV_*Test`, `ChiTieu_BUA_*Test`, `ChiTieu_HD_*Test`, `ChiTieu_KT_*Test` (Q17)
  - *(2026-11-11, A, rà lại)* `MetricCareTasksTest`, `MetricMealActivityQualityTest`, `MetricBedAwaitingReadyTest` có và xanh; `ACT.ISOLATION_RISK` bỏ theo Q-407. Đóng.

### Implementation cho User Story 4

- [ ] T016 [US4] *(2026-11-08, A: xong `TASK.TODAY`, `TASK.OVERDUE`, `TASK.BY_STATUS`, `TASK.MANDATORY_ON_TIME_RATE`, `TASK.LATE_RECORDINGS`, `TASK.BY_STAFF` (005), `MEAL.LATE_TICKETS`, `MEAL.DISCREPANCY_TICKETS` (011), `ACT.SESSIONS`, `ACT.PARTICIPATION_RATE`, `ACT.OVERDUE_TRIPS`, `ACT.MISSING_PARTICIPANTS`, `QC.PASS_RATE`, `QC.BY_CHECKER_ROLE` (014) trên nguồn thật; màn web dùng bảng chung, thêm nhãn ô; còn `ACT.ISOLATION_RISK` (bản cài `isolationRisks` của 014 còn trả rỗng), nhãn ghi nhận muộn của điểm danh hoạt động, xem plan)* *(2026-10-27: xong `BED.AWAITING_READY`, `_MAX`, `_OVERDUE`; nhóm khác chờ nguồn)* Cài chỉ tiêu nhóm chăm sóc, bữa ăn, hoạt động, kiểm tra chất lượng (005, 011, 014); màn web `frontend/src/tinh-nang/bao-cao/cham-soc/`, `frontend/src/tinh-nang/bao-cao/chat-luong/`
  - *(2026-11-10, A, Q-407)* `ACT.ISOLATION_RISK` bỏ (FR-036 bỏ): không còn cài chỉ tiêu này.
  - *(2026-11-11, A, rà lại)* Còn nhãn ghi nhận muộn của điểm danh hoạt động (014) trong `TASK.LATE_RECORDINGS` hoặc chỉ tiêu hoạt động; `ACT.ISOLATION_RISK` bỏ theo Q-407. Chưa làm.
  - *(2026-11-11, A)* Xong nhãn ghi nhận muộn của điểm danh hoạt động (FR-032): 014 `SessionStatisticsView.lateRecorded` (đếm `lateRecorded` của điểm danh, bỏ bản Hủy ghi nhận; ghi ở 014 module-interfaces theo dòng "Gửi Module 14" của spec 014), chỉ tiêu `ACT.LATE_ATTENDANCE` chia theo hoạt động. `T016_LateAttendanceStatisticsTest`, ca `FR_032_…` của `MetricMealActivityQualityTest` xanh (bước đỏ là lỗi biên dịch). Còn "ngoài phạm vi kiểm tra" cho Trưởng tầng tự làm công việc (FR-037, Q-168): nguồn 014 chưa trả việc của Trưởng tầng.
  - *(2026-11-11, A, luồng C, rà lại)* Còn "ngoài phạm vi kiểm tra" (FR-037, Q-168, Q-254): cần 014 trả các công việc bị loại khỏi mẫu vì do Trưởng tầng được giao hoặc Người phụ trách ca thực hiện; logic chọn mẫu ở `care.internal.qualitycheck.QualityCheckService.eligible`, luồng song song đang sửa `care` nên chưa đổi. Giữ [ ].

---

## Phase 7: User Story 5 – Báo cáo sức khỏe và thuốc (Priority: P2)

**Goal**: `SK.*`, `THUOC.*` (FR-040 → FR-044).

**Independent Test**: Tháng 10 với 50 cảnh báo, 8 lần leo thang, 900 liều đến hạn (20 Bỏ lỡ, 15 Từ chối), 3 phiếu đối chiếu quá hạn → số đúng.

### Tests cho User Story 5

- [x] T017 [P] [US5] *(2026-11-08, A: `MetricHealthEventsTest` 5 ca: cảnh báo theo mức, thời gian tiếp nhận chỉ cảnh báo đã tiếp nhận, leo thang; nút SOS, nút hỗ trợ; sự cố, thời gian tiếp nhận, leo thang; sự cố không thuộc sức khỏe không có kết quả; theo dõi, nguy kịch, đánh giá lại quá hạn; phần thuốc ở `MetricMedicationTest` từ trước; còn `HEALTH.CONTAINMENT_ZONES` chờ 007 có nguồn khoanh vùng)* Test `ChiTieu_SK_*Test`, `ChiTieu_THUOC_*Test` (Q20, Q21)
  - *(2026-11-11, A, rà lại)* `HEALTH.CONTAINMENT_ZONES` đã có test trong `MetricRemainingSourcesTest` (2026-11-11); phần còn lại ở `MetricHealthEventsTest`, `MetricMedicationTest`. Đóng.

### Implementation cho User Story 5

- [ ] T018 [US5] *(2026-11-08, A: cài `HEALTH.ALERTS`, `HEALTH.ALERT_RESPONSE_TIME`, `HEALTH.ALERT_ESCALATIONS`, `HEALTH.INCIDENTS`, `HEALTH.INCIDENT_RESPONSE_TIME`, `HEALTH.INCIDENT_ESCALATIONS`, `HEALTH.FOLLOW_UPS`, `HEALTH.NON_HEALTH_INCIDENTS`, `HEALTH.OVERDUE_REASSESSMENTS` trên `IncidentStatisticsApi`, `ResidentStatisticsApi` thật; màn web dùng bảng chung của `ReportsPage`, thêm nhãn ô ở `reportLabels.js`; còn `HEALTH.CONTAINMENT_ZONES`, mức lúc tiếp nhận của sự cố, dòng tham khảo "số sự cố đã hủy" (nguồn 007 chưa có, team-plan mục 8), xem plan "Chỉ tiêu sức khỏe, sự cố, tài sản")* Cài chỉ tiêu nhóm sức khỏe, thuốc (001, 006, 007); màn web `frontend/src/tinh-nang/bao-cao/suc-khoe-thuoc/`
  - *(2026-11-11, A)* Thêm `HEALTH.CONTAINMENT_ZONES` (`ContainmentZonesMetric`, `IncidentStatisticsApi.containmentZones`; một khu nhiều tầng đếm một lần). `MetricRemainingSourcesTest` xanh.
  - *(2026-11-11, A, rà lại)* 007 đã có `levelAtAcknowledgement` (đã dùng) và `IncidentStatisticsApi.cancelledCount`; còn dòng tham khảo "số sự cố đã hủy" (FR-042, cũng FR-007): `MetricValue` và api-016 chưa có trường cho dòng tham khảo, cần đổi api 016, chờ chủ dự án.
  - *(2026-11-11, A, luồng C)* Xong chuỗi giá trị theo thời gian cho một người (FR-040): "Đổi api" `VitalSignReportView.values` (giá trị hiện hành theo thành phần, áp đính chính; `DefaultVitalSignStatistics`), dòng chi tiết `HEALTH.VITAL_SIGNS` có `values` (nhóm `HEALTH`), lọc `residentId` cho chuỗi theo lúc đo. `MetricHealthVitalSignsTest` (bước đỏ là lỗi biên dịch, log `stream-c/t038.log`), `VitalSignStatisticsIT`, `T003_HealthApiContractTest` xanh. Giữ [ ]: còn dòng tham khảo "số sự cố đã hủy" cần đổi api-016, chờ chủ dự án.

---

## Phase 8: User Story 6 – Báo cáo chi phí phát sinh (Priority: P3)

**Goal**: Chi phí theo kỳ đã chốt và chưa chốt, nhập tay, điều chỉnh (FR-050 → FR-054, SC-007).

**Independent Test**: Chốt bảng tháng 10 của 3 người, 1 chưa chốt; 2 khoản nhập tay, 1 điều chỉnh sau chốt của tháng 9 nằm trong bảng tháng 10 → khớp bảng đã chốt.

### Tests cho User Story 6

- [x] T019 [P] [US6] Test `BT/reporting/SC_007_KhopBangChotIT.java` (Q18, Q19)
  - *(2026-11-11, A, rà lại)* `SC_007_KhopBangChotIT` chưa có; 010 đã có `COST.STATEMENTS`, `COST.SUMMARY` thật nên viết được. Chưa làm.
  - *(2026-11-11, A)* `SC_007_KhopBangChotIT` 2 ca trên lệnh thật của 010 (kiểm tra, duyệt, gửi chốt, chốt; tháng riêng năm 1000+): `COST.SUMMARY` đã chốt của tháng 10 = tổng `cost_statement.total` các bảng Đã chốt = dòng tổng kiểm soát file kế toán (`3 bảng; 4 khoản; 730 đồng`), tháng 9 không đổi sau khoản điều chỉnh nằm trong bảng tháng 10 (Q18); tạm tính thêm khoản của bảng chưa chốt, nhãn `PROVISIONAL`, dòng điều chỉnh có `originalPeriod` (Q19). Xanh ngay vì T020 đã cài trước, không có bước đỏ.

### Implementation cho User Story 6

- [x] T020 [US6] Cài chỉ tiêu nhóm chi phí qua `ChiPhiThongKeApi` (010); màn web `frontend/src/tinh-nang/bao-cao/chi-phi/`
  - *(2026-11-11, A)* `COST.SUMMARY`, `COST.EVENT_GENERATED_RATE`, `COST.POST_FINALIZATION_ADJUSTMENTS` (qua `CostItems`: bỏ khoản Đã hủy, đã chốt chỉ khoản Đã chốt, tạm tính thêm khoản chưa hủy, khoản thiếu đơn giá không cộng), `COST.STATEMENTS`, `COST.PURCHASING` (019, tách khỏi chi phí người cao tuổi); chế độ tạm tính gắn nhãn `PROVISIONAL` (`ReportService`); web: lựa chọn "Đã chốt / Tạm tính" ở nhóm Chi phí, gửi `costMode` cho bảng và file xuất, nhãn ô (`reportLabels.js`). `MetricRemainingSourcesTest` 5, `ReportApiIT.FR_051_…` đỏ rồi xanh, `ReportsPage.test.jsx` FR_051 đỏ rồi xanh, IT báo cáo, xuất, phạm vi xanh.

---

## Phase 9: User Story 7 – Số liệu nhất quán và truy về bản ghi nguồn (Priority: P3)

**Goal**: Mọi con số mở được danh sách chi tiết có số dòng bằng giá trị; dashboard và báo cáo cùng ngày khớp (FR-009, SC-006).

**Independent Test**: 20 chỉ tiêu ngẫu nhiên: số dashboard = số báo cáo cùng ngày = số dòng chi tiết.

### Tests cho User Story 7

- [ ] T021 [P] [US7] *(2026-11-01: xong khung `FR_009_DetailMatchIT` bằng chỉ tiêu giả: dashboard, báo cáo, chi tiết cùng số, ô chia nhóm, phân trang, dòng rút gọn; còn 20 chỉ tiêu thật của SC-006 và mở bản ghi chỉ xem ở feature sở hữu, chờ nguồn)* Test `BT/reporting/FR_009_ChiTietKhopSoIT.java` (SC-006; Q12, Q13)
  - *(2026-11-11, A, rà lại)* Còn chạy `FR_009_DetailMatchIT` trên 20 chỉ tiêu thật của SC-006 (mọi nguồn đã có từ 2026-11-11). Chưa làm.
  - *(2026-11-11, A)* `FR_009_DetailMatchIT.SC_006_realMetrics_…`: mọi chỉ tiêu đếm, tỷ lệ thật (không chỉ 20) ở cùng mốc: số báo cáo hôm nay = số dòng chi tiết cùng khoảng (tỷ lệ: mẫu số), số dashboard = số dòng chi tiết không khoảng; Quản lý viện (≥ 20 mã) và Trưởng tầng. Xanh. Còn Q12 "mở bản ghi chỉ xem ở feature sở hữu" (FR-009 câu cuối): `DetailRow.canOpenRecord` luôn `false`, web chưa có liên kết; cần bảng loại bản ghi → màn chỉ xem và khóa quyền của từng nguồn (plan "Dòng rút gọn luôn canOpenRecord = false").
  - *(2026-11-11, A, luồng C, rà lại)* Q12 chưa làm: cần bảng (loại bản ghi nguồn → màn chỉ xem của feature sở hữu, khóa quyền xem) cho mọi chỉ tiêu và liên kết trên web; không phải điểm nghiệp vụ, để lượt riêng. Giữ [ ].

### Implementation cho User Story 7

- [x] T022 [US7] *(2026-10-24: xong `GET /metric-details/{metricCode}` (`MetricDetailService`, phân trang 50 dòng) và khung `MetricDetailSheet`; còn test FR-012, FR-014 của US7 với nguồn thật)* Cài `BE/reporting/chitiet/internal/ChiTietService.java` và `BE/reporting/chitiet/web/ChiTietController.java` (`GET /chi-tiet/{maChiTieu}`; phân trang; giới hạn trường); màn web `frontend/src/tinh-nang/bao-cao/chi-tiet/`
  - *(2026-11-11, A, rà lại)* Còn test FR-012, FR-014 của US7 trên nguồn thật (cùng đợt T011, T021).
  - *(2026-11-11, A)* Test FR-012 (phạm vi Trưởng tầng) và FR-014 (giới hạn trường) của US7 trên nguồn thật: ca mới của `FR_009_DetailMatchIT`, `FR_014_FieldRestrictionIT`. Phần mở bản ghi của FR-009 theo dõi ở T021.

---

## Phase 10: User Story 8 – Báo cáo nhân sự và ca trực (Priority: P3)

**Goal**: Ca, đổi ca, nghỉ, tỷ lệ phục vụ, chứng chỉ (FR-070 → FR-073; thiếu phủ, thiếu ca, lời mời bỏ theo Q-397).

**Independent Test**: Tháng 11 tầng 2 với 60 ca, 6 yêu cầu đổi ca → số đúng (ca thiếu phủ, mục thiếu ca bỏ theo Q-397).

### Tests cho User Story 8

- [x] T023 [P] [US8] *(2026-10-27: xong phần 008: `MetricShiftStaffingRatioTest`, `MetricShiftHandoversTest`, `MetricLicenseStaffTest`, `FR_020_StaffingDashboardIT`; còn phần 015 chờ `DoiCaThongKeApi`; 2026-10-29 Q-397 bỏ chỉ tiêu thiếu phủ, thiếu ca, lời mời)* Test `ChiTieu_NS_*Test` trong `BT/reporting/chitieu/` (Q22, Q23)
  - *(2026-11-11, A)* Phần 015: `MetricRemainingSourcesTest` (`SHIFT.REQUESTS`); `SHIFT.WORKING_HOURS` bỏ theo Q-408. Đóng task.

### Implementation cho User Story 8

- [x] T024 [US8] *(2026-10-27: xong `SHIFT.STAFFING_RATIO`, `SHIFT.UNCONFIRMED_HANDOVERS`, `LICENSE.STAFF` qua `StaffStatisticsApi` thật; màn web dùng tab "Nhân sự, ca" có sẵn; còn `REQUESTS`, `WORKING_HOURS` chờ 015; `SHIFT.UNDER_COVERAGE`, `MISSING_SHIFTS`, `INVITATIONS` bỏ theo Q-397)* Cài chỉ tiêu nhân sự, ca qua `NhanSuThongKeApi` (008), `DoiCaThongKeApi` (015); màn web `frontend/src/tinh-nang/bao-cao/nhan-su-ca/`
  - *(2026-11-11, A)* `SHIFT.REQUESTS` (theo loại và trạng thái, không lý do) và `SHIFT.WORKING_HOURS` (giờ đã làm theo nhân viên, giờ đã xếp ở chi tiết; chỉ Quản lý viện xuất) qua `SwapStatisticsApi`; nhãn ô web. Giữ [ ]: `WorkingHoursView` của 015 chưa có tầng nên trưởng tầng được giao chưa thấy nhân viên tầng mình (FR-073); cần "Đổi api" thêm tầng vào view.
  - *(2026-11-11, A; chủ dự án chốt Q-408)* Bỏ chỉ tiêu giờ làm theo nhân viên: gỡ `WorkingHoursMetric`, `SwapStatisticsApi.workingHoursByMonth`, `WorkingHoursView`, luật chỉ Quản lý viện xuất của chỉ tiêu này (`ExportContent.MANAGER_ONLY`) và nhãn web; spec 016 FR-073, 015 FR-042, `chi-tieu.md` ghi bỏ. `Q408_NoWorkingHoursMetricTest` đỏ rồi xanh; `FR_061_ExportContentTest`, `MetricRemainingSourcesTest`, `FR_042_SwapStatisticsIT`, `ReportApiIT`, kiến trúc, Vitest reports xanh. Đóng task.

---

## Phase 11: User Story 9 – Xuất báo cáo ra file (Priority: P3)

**Goal**: Xuất XLSX theo báo cáo, khoảng, bộ lọc, chế độ, có/không định danh, mục đích; ghi nhật ký "Xuất báo cáo" trong transaction trước khi trả liên kết; tệp lưu ở `common.tep` loại `M14.BAO_CAO_XUAT` (FR-060 → FR-062).

**Independent Test**: Xuất báo cáo chi phí tháng 10 bằng Quản lý viện và Hành chính; xuất báo cáo sức khỏe bằng trưởng tầng → file khớp màn hình, có bản ghi nhật ký.

### Tests cho User Story 9

- [x] T025 [P] [US9] *(2026-10-24: `FR_061_ExportContentTest`, `FR_062_ExportAuditIT`; chỉ tiêu giả `TEST.MISSED_DOSES`, ghi chú Phase 11 ở plan)* Test đơn vị `BT/reporting/FR_061_NoiDungXuatTest.java` và `BT/reporting/FR_062_NhatKyXuatIT.java` (Q25 → Q28)

### Implementation cho User Story 9

- [x] T026 [US9] *(2026-10-24: `reporting.internal.export`, fastexcel qua `common.document` thay POI; lần xuất đọc bằng `AuditLogApi.recorded`)* Cài `BE/reporting/xuat/internal/XuatBaoCaoService.java` (`YeuCauXuat`; POI; `NhatKyApi.ghi` `XUAT_BAO_CAO` với bộ lọc ở `chi_tiet`; `TepApi`; `QuyenTepProvider` `M14.BAO_CAO_XUAT`: người xuất, Quản lý viện) và endpoint `POST /bao-cao/{nhom}/xuat`, `GET /bao-cao/lan-xuat` (research O-08)

**Checkpoint**: Mọi user story của 016 chạy độc lập.

---

## Phase 12: Polish & Cross-Cutting

- [x] T031 [US5] *(2026-11-08, A: số cảnh báo SOS là ô `SOS` của `HEALTH.SOS_ASSISTANCE_ALERTS` (T032); thời gian tiếp nhận trung bình, lớn nhất theo tầng chưa có ô riêng, xem plan)* *(Bổ sung 2026-10-18, Q-369)* Thêm chỉ tiêu cảnh báo SOS vào nhóm sức khỏe (research O-12) và màn `frontend/src/tinh-nang/bao-cao/suc-khoe-thuoc/`; test `ChiTieu_SK_CanhBaoSosTest` (quickstart B1)
  - *(2026-11-11, A, rà lại)* Còn ô thời gian tiếp nhận trung bình, lớn nhất theo tầng của cảnh báo SOS. Chưa làm.
  - *(2026-11-11, A)* Thời gian tạo → tiếp nhận trung bình, lớn nhất theo nguồn và tầng: `HEALTH.SOS_ASSISTANCE_RESPONSE_TIME`, `_MAX` (chi-tieu.md bổ sung 2026-11-11). `MetricHealthEventsTest.T032_…` đỏ (chưa có lớp) rồi xanh.
- [x] T032 [US5] *(2026-11-08, A: `HEALTH.SOS_ASSISTANCE_ALERTS` đếm theo nguồn `SOS`, `EMERGENCY_ASSIST`, vẫn đếm trong `HEALTH.ALERTS`; dòng chi tiết có phút tạo → tiếp nhận, tạo → tới nơi, dấu không ghi mốc tới nơi; còn trung bình, lớn nhất, số "không ghi mốc tới nơi" theo tầng và dòng "chưa rõ tầng", xem plan)* *(Bổ sung 2026-10-22, Q-374, Q-375)* Mở rộng chỉ tiêu của T031 thành `SK.CANH_BAO_SOS_HO_TRO` (research O-13, contracts/chi-tieu.md): dòng nút hỗ trợ khẩn cấp, thời gian tạo → tới nơi, số "không ghi mốc tới nơi", dòng "chưa rõ tầng"; test `ChiTieu_SK_CanhBaoSosHoTroTest` (quickstart B1, B2); cần 007 T096 (`daToiNoiLuc`, `NUT_HO_TRO` trong `CanhBaoThongKeApi`), bản giả trước đó
  - *(2026-11-11, A, rà lại)* Còn trung bình, lớn nhất, số "không ghi mốc tới nơi" theo tầng và dòng "chưa rõ tầng". Chưa làm.
  - *(2026-11-11, A)* Mọi mã `HEALTH.SOS_ASSISTANCE_*` chia ô `<nguồn>:<tầng>`, tầng rỗng `UNKNOWN_FLOOR` ("chưa rõ tầng"); thêm `HEALTH.SOS_ASSISTANCE_ARRIVAL_TIME`, `_ARRIVAL_TIME_MAX` (chỉ cảnh báo có mốc tới nơi), `HEALTH.SOS_ASSISTANCE_NO_ARRIVAL` (số "không ghi mốc tới nơi"); web `breakdownLabel` hiện "Nút SOS, Tầng 1", "…, chưa rõ tầng" (Vitest đỏ rồi xanh). `FR_062_ExportAuditIT` số dòng xuất 21 → 26 (năm mã mới).

- [ ] T027 [P] *(2026-11-01: xong `SC_008_ReadOnlyIT`, so `CHECKSUM TABLE` toàn schema; còn đo SC-003 báo cáo 12 tháng, chờ chỉ tiêu thật)* Test `BT/reporting/SC_008_ChiDocIT.java` (đếm bản ghi trước/sau mọi báo cáo; Q29) và hiệu năng báo cáo 12 tháng (SC-003; Q24)
- [x] T028 [P] *(2026-10-24: `ApiContractIT`; `pending` còn `/dashboard`, `/metrics`, `/reports/{group}`, `/metric-details/{metricCode}`)* Mở rộng `BT/common/HopDongApiTest.java` với `specs/016-reports-dashboard/contracts/api-016.yaml`
- [x] T105 *(bổ sung 2026-10-29, Q-392)* Bỏ nhóm chỉ tiêu kho nguyên liệu: `MetricGroup.INVENTORY`, chỉ tiêu `KHO.DUOI_TOI_THIEU`, `KHO.LO_SAP_HET_HAN` (contracts/chi-tieu.md) và các ô quyền của nhóm này; sửa test liên quan
- [ ] T029 Chạy `./mvnw verify`, `npm test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T030 *(2026-11-06: rà xong lượt đầu, plan "Rà T030"; mọi mã đã cài có test, 43 mã chưa cài chờ nguồn; DBR-25, FR-002, 004 (thêm `M14ArchTest.FR_004_noSourcesOutsideThisPhase`), 005, 006, 008 có mã và test; thiếu FR-007 nhãn loại bỏ theo lưu trữ (T109); FR-055, 055a chờ 010, 019 (T020); để `[ ]` tới khi các mã còn lại có test)* Rà: mọi mã ở `contracts/chi-tieu.md` có `ChiTieu_*Test` (SC-001); DBR-25 (mốc theo Asia/Ho_Chi_Minh); FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-002, FR-004, FR-005, FR-006, FR-007, FR-008, FR-055, FR-055a — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US1 → US2 (P1)**: US2 nối phạm vi vào mọi đường đọc
- **US3, US4, US5 (P2)**: sau US2, song song
- **US6, US7, US8, US9 (P3)**: sau US2; US7 cần chỉ tiêu của US1, US3 → US6, US8; US9 sau US3

### Parallel Opportunities

- T005, T006 song song; US3, US4, US5 song song; US6, US8 song song

## Parallel Example: P2

```text
Task: "T013 ChiTieu_NCT_* + ChiTieu_GIUONG_*"
Task: "T015 ChiTieu_CV_* + BUA + HD + KT"
Task: "T017 ChiTieu_SK_* + THUOC"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2
2. **Dừng và kiểm chứng**: dashboard đúng phạm vi với bộ 11 tài khoản
3. Thêm báo cáo theo nhóm

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6 → US7 → US8 → US9; mỗi bước chạy `./mvnw verify`.

## Notes

- 016 chỉ đọc; không tạo, sửa dữ liệu nghiệp vụ, không gửi thông báo (FR-001, Q-306)
- Mọi mốc đọc `ThamSoApi` (CFG-M14-*)

## Phase 13: Convergence

*(/speckit-converge 2026-11-06. Các nguồn dưới đây đã có bản cài thật nhưng `reporting` chưa dùng; phần khác của T008, T014, T016, T018, T020, T022, T024, T031, T032 vẫn chờ nguồn như ghi ở từng task.)*

- [x] T106 [US3] *(2026-11-06: `WaitingListMetric` `RES.WAITING_LIST`, thêm `RES.WAITING_TIME`, `RES.WAITING_TIME_MAX` (chi-tieu.md, glossary); test `MetricResWaitingListTest` 3; còn số hồ sơ rời danh sách theo lý do chờ 004 trả `leftOn`, ghi ở plan)* Cài `NCT.DANH_SACH_CHO` qua `ResidencyStatisticsApi.waitingList` (004, bản thật `ResidencyStatisticsService`); test `ChiTieu_NCT_*Test` viết trước; một phần của T013, T014 per FR-025 (partial)
- [x] T107 [US5] *(2026-11-06: `MissedDoseRateMetric`, `RefusedDoseRateMetric`, `OverdueReconciliationsMetric`; test `MetricMedicationTest` 2)* Cài `THUOC.TY_LE_BO_LO`, `THUOC.TY_LE_TU_CHOI` (`MedicationStatisticsApi.dosesInPeriod`) và `THUOC.DOI_CHIEU_QUA_HAN` (`reconciliationsInPeriod`) của 006 (bản thật `DefaultMedicationStatisticsService`); test `ChiTieu_THUOC_*Test` viết trước; một phần của T017, T018 per FR-043, FR-044 (partial)
- [x] T108 [US5] *(2026-11-06: `VitalSignsMetric`; test `MetricHealthVitalSignsTest` 1; còn chuỗi giá trị theo thời gian của một người, chờ 007 thêm giá trị đo vào `VitalSignReportView`, ghi ở plan)* Cài `SK.CHI_SO` qua `VitalSignStatisticsApi.readings` (007, bản thật `DefaultVitalSignStatistics`); test `ChiTieu_SK_*Test` viết trước; một phần của T017, T018 per FR-040 (partial)
- [x] T109 [US7] Gắn nhãn `PURGED_BY_RETENTION` khi khoảng báo cáo giao với khoảng dữ liệu đã loại bỏ theo bản tóm tắt `EXPIRED_DATA_PURGED` của 000 (FR-051) cho loại dữ liệu mà chỉ tiêu đọc; test viết trước per FR-007 (missing) *(bổ sung 2026-11-06 khi rà T030: nhãn có trong enum, file xuất, web nhưng không chỗ nào gắn; "kèm loại dữ liệu bị ảnh hưởng" cần thêm trường vào `MetricValue` của api-016, chờ chủ dự án duyệt)* *(2026-11-10, A: chủ dự án duyệt `MetricValue.purgedDataTypes`; `PurgedDataTypes` đọc bản tóm tắt `EXPIRED_DATA_PURGED` (`AuditLogApi.recorded`, `M15.PURGE_RUN`), chỉ tiêu ứng với module qua API nguồn ở `Metric.source`, khoảng báo cáo bắt đầu trước mốc `to` muộn nhất của loại dữ liệu có bản ghi bị xóa thì gắn nhãn kèm loại; báo cáo (`ReportService`), file xuất (cột ghi chú), web (`labelNotes` hiện tên module). Test: `FR_007_PurgedByRetentionTest` 2, `ReportApiIT.FR_007_…` đỏ rồi xanh, `FR_061_ExportContentTest.FR_007_…`, `reportLabels.test.js`)*
- [x] T110 *(Bổ sung 2026-11-06)* Web: ô chia nhóm hiện chữ thay khóa thô (`reportLabels.breakdownLabel`: trạng thái giường kèm lý do giữ chỗ, hồ sơ chờ, loại lưu trú, mức chỉ số, bàn giao, tỷ lệ phục vụ, giấy phép, phiếu đối chiếu; tầng theo `getFloors`; ca hiện "Ca {id}" vì hợp đồng chỉ trả khóa) ở bảng báo cáo, tiêu đề danh sách chi tiết, dashboard; test `reportLabels.test.js` 4 (đỏ rồi xanh), `ReportsPage.test.jsx` sửa khóa mẫu

## Phase 14: Convergence

- [x] T111 Cô lập `FR_020_DashboardIT`, `SC_008_ReadOnlyIT`, `FR_009_DetailMatchIT` khỏi dữ liệu sót của test khác (đỏ ở IT 3 JVM vì `CareTaskData.publicTaskTypeId` null; chạy riêng xanh) theo mẫu `CallReminderTestBase` per FR-020, SC-008, FR-009 (partial)
  - *(2026-11-10, A)* Nguyên nhân: `T064_LocationTaskRetentionIT` (005) chèn `care_task` năm 2010 với `data='{}'`, chỉ tiêu công việc quá hạn đọc từ `Instant.EPOCH` gặp dòng này (`CareTaskData.publicTaskTypeId` null). Sửa fixture cho đủ nội dung; xóa một dòng sót ở `eldercare_test_1` bằng tài khoản `eldercare_retention`. `./mvnw clean verify -Dit.forkCount=3` 2026-11-10: unit 1099, IT 662 xanh (21 phút 37 giây).
