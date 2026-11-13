---
description: "Danh sách task triển khai feature 011 – Quản lý dinh dưỡng và suất ăn"
---

# Tasks: Quản lý dinh dưỡng và suất ăn (011)

**Input**: Tài liệu thiết kế ở `specs/011-nutrition-management/` (plan.md, spec.md, research.md M-01 → M-15, data-model.md, contracts/api-011.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 001 (`DiNguyenApi`, `MucSucKhoeApi`, sự kiện dị ứng, `ThamGiaChuyenTrangThai`), 003 (`ViTriApi`), 004 (sự kiện vắng, bán trú), 005 (`NguonCongViecPort`, `CoMatBanTruApi`, `AnKemKeoDai`), 007 (`SuCoLenhApi`, `SutCanPhatHien`, `YeuCauTaoCanhBao`), 010 (`SuKienTinhPhiNguon`) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/eldercare/backend/`, `BT/` = `backend/src/test/java/com/eldercare/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M08 (phần dinh dưỡng): `BE/nutrition/{api,danhmuc,chedoan,doichieu,thucdon,chotsuat,phieu,phucvu,doangiadinh,xemlai,luumau,lich}/`
- Web `frontend/src/tinh-nang/dinh-duong/{danh-muc,ban-gan,thuc-don,chot-suat,phieu,do-an-gia-dinh,luu-mau}/`; app nhân viên `mobile/lib/nhan-vien/dinh-duong/`

---

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; 009 FR-001a): sửa T003. Mã task cũ giữ nguyên.

## Phase 1: Setup

- [x] T001 Tạo khung gói `BE/nutrition/...` với `package-info.java`; ArchUnit `BT/nutrition/M08ArchTest.java`: gói `..lich..` không gọi lệnh chốt, không lập phiếu (Q-306); DTO bếp không tham chiếu kiểu sức khỏe (research M-01, M-09)
  - *Ghi chú 2026-11-06:* `M08ArchTest` (ở `BT/nutrition/`): luật Q-306 (`scheduling` không phụ thuộc `internal.closing`, `internal.slip`) và SC-008 (lớp `Kitchen*` không tham chiếu kiểu sức khỏe); cả hai `allowEmptyShould` vì chưa có lớp, sẽ thực chất từ Phase 5–6. Gói `api` tạo cùng lúc ở T002 (cần kiểu cho bản giả).
- [x] T002 [P] Tạo bản giả trong `BT/support/gia011/`: `LuotOLaiAnPort` (012), `ChuyenDiBuaAnPort` (014); phát `LuotOLaiDaDoi`, `ChuyenDiDaDoi` (contracts mục 1.6, 1.7)
  - *Ghi chú 2026-11-06:* bản giả ở `support/fake/nutrition/` (không phải `gia011`, theo team-plan mục 3): `FakeStayMealPort`, `FakeTripMealPort`, `FakeNutritionEvents`, `FakeNutritionConfig`; test `FakeNutritionTest` 3 xanh. Tên Anh theo glossary mục 17.5; kiểu `StayMealView`, `TripOverlapView`, `StayMealPort`, `TripMealPort`, `StayMealChanged`, `TripMealChanged` làm sớm từ T003.

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `nutrition.api`, sự kiện, hàm đối chiếu xung đột, danh mục, khung job, listener idempotent.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T003 [P] Tạo `BE/nutrition/api/`: `CheDoAnApi`, `ThucDonChungApi`, `LichBuaApi`, `DinhDuongThongKeApi`, SPI `LuotOLaiAnPort`, `ChuyenDiBuaAnPort`, record `LuotOLaiDaDoi`, `ChuyenDiDaDoi` và sự kiện mục 1.8 (cài `CoYeuCauThongBao`) (contracts mục 1.2 → 1.8); bổ sung 2026-10-18 (009 FR-001a): sự kiện có người nhận là người thân khai `doiTuongThongTin` theo research M-13, test hợp đồng kiểm mọi yêu cầu tới người thân có đối tượng
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* đã có `DietApi`, `GeneralMenuApi`, `ExpectedMealApi` (thêm theo module-interfaces 1.3a, team-plan mục 8), `MealScheduleApi`, `NutritionStatisticsApi`, SPI `StayMealPort`, `TripMealPort`, sự kiện `StayMealChanged`, `TripMealChanged`, `DietAssignmentTookEffect`, `MealSlipDelivered`, `MealSlipDiscrepancyReported`, `MealSlipDeliveredLate`, bản giả dùng chung ở `support/fake/nutrition/` (`FakeNutritionApiTest` 5 xanh). Còn thiếu: sự kiện thông báo cài `HasNotificationRequests` theo bảng FR-065 và test hợp đồng kiểm đối tượng thông tin (009 FR-001a, research M-13); làm ở phase của từng nghiệp vụ phát thông báo vì nội dung và người nhận gắn với lệnh kích hoạt.
  - *Ghi chú 2026-11-06, Phase 4 (vẫn chưa đánh [x]):* thêm sự kiện thông báo `ResidentWithoutDietAssignment` (FR-065 dòng "Người Đang lưu trú chưa gán chế độ ăn", Bác sĩ, mức Nhẹ, cài `HasNotificationRequests`); test qua `FR_015_FinalStatusIT`. Các dòng FR-065 còn lại làm ở phase của lệnh phát thông báo.
  - *Ghi chú 2026-11-06, Phase 5–6 (vẫn chưa đánh [x]):* thêm sự kiện thông báo thực đơn (`MenuSubmitted`, `MenuReturned`, `MenuPublished`, `MenuPublicationReminder`, `DishSwapped`, `MenuDishViolation`) và chốt suất (`PortionClosingReminder`, `PortionsClosed`, `SafeDishApplied`, `AdjustmentsPending`), cùng yêu cầu cảnh báo `incident.api.events.AlertRequested` (`DIET_ALLERGY_CONFLICT`, `MENU_DISH`). Còn các dòng FR-065 của phiếu, phục vụ, đồ ăn gia đình, xem lại (Phase 7–10); không sự kiện nào tới người thân nên chưa cần đối tượng thông tin.
- [x] T004 Tạo `MIG/V011_01__nen_m08.sql`: `dinh_duong_nct` (`nct_id` khóa chính), `bua_an` (`ma` khóa của CFG-M08-04; `phuc_vu_noi_tru`, `phuc_vu_ban_tru`), `loai_do_an_cam`, `su_kien_m08_da_xu_ly` (`khoa_su_kien UNIQUE`), `lan_chay_m08` (data-model mục 2, 3, 11) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `dinh_duong_nct` → `trang_thai_nct`; `bua_an`, `loai_do_an_cam` → `danh_muc_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *Ghi chú 2026-11-06:* sau R-15, cả 5 bảng của task đã gộp vào bảng chung (`dinh_duong_nct` → `resident_module_state`, `bua_an`, `loai_do_an_cam` → `catalog_item`, `su_kien_m08_da_xu_ly`, `lan_chay_m08` → `reminder_marker`), nên **không tạo `V011_01`**; không có DDL cho phase này. Ba bảng riêng (`diet_assignment`, `menu`, `portion_closing`) tạo ở phase của user story dùng chúng; hằng `SharedKind` `M08.*` khai cùng lúc với chỗ dùng đầu tiên. Dấu listener: `M08.PROCESSED_EVENT`, dấu việc: `M08.JOB.<khóa>` trên `reminder_marker` (T006).
- [x] T005 [P] Cài hàm thuần `BE/nutrition/doichieu/internal/XungDot.java`, `ChonMonThayThe.java` và test `BT/nutrition/XungDotTest.java`, `ChonMonThayTheTest.java` (dị ứng và hạn chế, BR-M08-02, BR-M01-07; research M-06)
  - *Ghi chú 2026-11-06:* `ConflictCheck` (kèm `ConflictType`, `AllergyItem`) và `SubstituteDishSelector` ở `nutrition.internal.conflict.domain`; tên test theo glossary: `ConflictCheckTest` (8), `SubstituteDishSelectorTest` (4) xanh.
- [x] T006 Cài khung job `BE/nutrition/lich/ViecTheoPhutM08.java`, `ViecHangNgayM08.java` (ShedLock, `lan_chay_m08`, chạy bù) và test `BT/nutrition/ChayBuM08IT.java`; khung listener idempotent `BE/nutrition/doichieu/internal/SuKienM08Listener.java` (chỉ xét bữa đã chốt chưa tới giờ) (research M-08, M-15) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lan_chay_m08` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *Ghi chú 2026-11-06:* khung ở `nutrition.scheduling` (`M08MinuteJob`, `M08DailyJob`, `M08Jobs`) và `internal.schedule.service` (`M08JobService`, `M08MinuteTask`, `M08DailyTask`, `M08JobMarkers`); `internal.conflict.service.M08EventListener.once(eventKey, work)`. Chạy bù: mỗi việc xét mọi dòng đến hạn tại `now` và chống lặp bằng dấu trên `reminder_marker`, job cũng chạy lúc khởi động. Hai job thêm vào `SchedulerScopeArchTest`; chưa thêm vào enum `runJobs` của api-000 (A duyệt). `M08CatchUpRunIT` (6) và `M08JobSchedulingTest` (3) xanh trên `eldercare_test_d`.
- [x] T007 [P] Cài provider: `PhanLoaiTruongProvider` (dị ứng, hạn chế → `SUC_KHOE`), `LuuTruProvider` (`THEO_HO_SO` cho bản gán, xác nhận phục vụ, đồ ăn gia đình, suất ăn người thân; còn lại `KHONG_GAN_HO_SO`), `GanPhamViProvider` (`M08.BAN_GAN`, `M08.XAC_NHAN_PHUC_VU`, `M08.DO_AN_GIA_DINH`, `M08.PHIEU_BUA_AN`)
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* đã cài `FieldClassificationProvider` cho `M08.DIET`, `M08.DIET_ASSIGNMENT`, `M08.FAMILY_FOOD`, `M08.SERVING_CONFIRMATION` (`M08FieldClassificationConfigTest` 4 xanh). Còn: `RetentionProvider` (cả hai hàm mới `referencing`, `expiredCandidates(cutoff, after, batch)`, team-plan mục 8) và `ScopeAssignmentProvider` cho `M08.PHIEU_BUA_AN` cần dữ liệu thật của bản gán, phiếu, xác nhận phục vụ, đồ ăn gia đình, nên làm trong phase tạo các thực thể đó.
  - *Ghi chú 2026-11-06, Phase 4 (vẫn chưa đánh [x]):* đã cài phần bản gán: `DietAssignmentRetention` (`RetentionProvider` `M08.DIET_ASSIGNMENT`, `THEO_HO_SO`, cả hai hàm mới `referencing` và `expiredCandidates(cutoff, after, batch)`) và `DietAssignmentScopeAssignment` (`ScopeAssignmentProvider`), test `M08AssignmentProvidersIT` (3) xanh. Còn: xác nhận phục vụ, đồ ăn gia đình, suất ăn người thân (`THEO_HO_SO`), phiếu bữa ăn (`M08.MEAL_SLIP`, phạm vi tầng/khu); làm ở phase tạo các thực thể đó.
  - *Ghi chú 2026-11-06, Phase 6 (vẫn chưa đánh [x]):* thêm `ClosingProviders.FamilyMealRetention` (`M08.FAMILY_MEAL`, `THEO_HO_SO`, `referencing`, `expiredCandidates(cutoff, after, batch)`, mẫu `M10SimpleRecordRetention`) và `MealSlipScopeAssignment` (`M08.MEAL_SLIP`, tầng/khu của phiếu). Còn xác nhận phục vụ (Phase 8), đồ ăn gia đình (Phase 9).

**Checkpoint**: Nền 011 sẵn sàng.

---

## Phase 3: User Story 2 – Món ăn gắn thành phần gây dị ứng, Bác sĩ xác nhận; xung đột phát hiện ở mọi điểm (Priority: P1) 🎯 MVP

**Goal**: Chế độ ăn (hạn chế, món an toàn, một mặc định), món ăn có phiên bản nội dung do Bác sĩ xác nhận, đối chiếu xung đột cho một người (FR-001 → FR-007, FR-020 → FR-022, Q-308).

**Independent Test**: Bếp tạo "Cá kho" có thành phần "cá", "Đậu phụ sốt cà" làm món thay thế; Bác sĩ xác nhận một món, trả lại một món; bếp sửa thành phần món đã xác nhận → bản sửa chờ, bản Hiệu lực cũ vẫn dùng.

### Tests cho User Story 2

- [x] T008 [P] [US2] Test `BT/nutrition/FR_004a_XacNhanMonIT.java`, `FR_014_CheDoMacDinhIT.java`: bảng chuyển món; bản sửa là phiên bản mới; ngừng món an toàn của chế độ Hiệu lực bị chặn; một chế độ mặc định (`UNIQUE khoa_mac_dinh`); đổi hạn chế chạy lại FR-005 với thực đơn chưa hết tuần
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `FR_004a_DishConfirmationIT` (7) và `FR_014_DefaultDietIT` (6, có cả FR-003 thêm thành phần thực phẩm) xanh trên `eldercare_test_d`, cùng hai test đơn vị `DishTransitionsTest` (33) và `SafeDishRuleTest` (5). Phần còn thiếu là "đổi hạn chế chạy lại FR-005 với thực đơn chưa hết tuần": phần này cần bảng `menu`, nên viết ở Phase 5.
  - *Ghi chú 2026-11-06, Phase 6:* phần "đổi hạn chế chạy lại FR-005 với thực đơn chưa hết tuần" đã cài ở `DietService.update` (báo Bác sĩ, bếp `MenuDishViolation`, thực đơn không tự đổi), kèm báo khi món Ngừng hiệu lực còn trong thực đơn đã công bố (`DishService`); test `BR_M08_06_CoverageIT` (`FR_002_dietRestrictionChange…`, `FR_065_dishDeactivated…`) xanh.

### Implementation cho User Story 2

- [x] T009 [US2] Tạo `MIG/V011_02__danh_muc_mon.sql`: `che_do_an` (`lien_quan_dieu_tri`; cột sinh `khoa_mac_dinh` `UNIQUE`; `mon_an_toan_id`), `che_do_an_han_che`, `mon_an` (`trang_thai` `NHAP|CHO_XAC_NHAN|HIEU_LUC|NGUNG_HIEU_LUC|DA_HUY`; `phien_ban_hieu_luc_id`; `tung_hieu_luc`), `mon_an_phien_ban` (nhóm 3 có bản chờ; `ket_cau` `THUONG|MEM|XAY_NHUYEN`), `mon_an_phien_ban_thanh_phan`, `mon_an_phien_ban_che_do` (data-model mục 3) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `che_do_an`, `mon_an` → `danh_muc_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *Ghi chú 2026-11-06:* không tạo `V011_02`. Chế độ ăn (`M08.DIET`) và món (`M08.DISH`) nằm trong `catalog_item`. Hạn chế, món an toàn, cờ mặc định và trạng thái món nằm trong `attributes`. Phiên bản món (`M08.DISH_VERSION`, gồm thành phần, chế độ phù hợp, kết cấu) nằm trong `simple_record`, cha là món. Khóa `uniqueKey` `M08.DISH_VERSION.OPEN:<dishId>` bảo đảm mỗi món chỉ có một bản Nháp hoặc Chờ; bản đã đóng thì bị khóa. Ràng buộc "một chế độ mặc định" là cột sinh `m08_default_diet` có `UNIQUE`, đặt trong `V011_94__shared_m08_default_diet.sql`.
- [x] T010 [US2] Cài `BE/nutrition/danhmuc/internal/CheDoAnService.java`, `MonAnService.java` (tạo, gửi xác nhận, xác nhận/trả lại/rút, bản sửa, ngừng, hủy; Bác sĩ xác nhận, Nhân viên bếp lập; Q-283, Q-308), `BuaAnService.java`, `LoaiDoAnCamService.java` (research M-04)
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* đã có `nutrition.internal.catalog.service.DietService` và `DishService`, cùng các phần sau:
    - miền `DishTransitions`, `SafeDishRule`;
    - lưu trữ qua `NutritionCatalogStore`, dùng CatalogApi và SimpleRecordApi;
    - sự kiện `DishConfirmationRequested` (gửi Bác sĩ) và `DishConfirmationResult` (gửi người gửi món).
    
    Chủ dự án quyết ngày 2026-11-06:
    - lúc lưu chế độ ăn không xét điều kiện món an toàn "phù hợp";
    - khi chưa có món Hiệu lực nào thì chế độ ăn đầu tiên được để trống món an toàn;
    - đặt một chế độ làm mặc định thì chuyển mặc định ngay trong cùng lệnh.
    
    Còn lại:
    - `MealService`, `ForbiddenFoodTypeService`: chờ A thêm Function quyền danh mục dinh dưỡng (team-plan mục 8);
    - kiểm ngừng chế độ ăn còn bản gán: xong ở Phase 4 (`DietService.deactivate` chặn `STILL_ASSIGNED` khi còn bản Hiệu lực hoặc Chờ hiệu lực, test `FR_002_dietStillAssigned_cannotBeDeactivated`); còn kiểm thực đơn dùng (`STILL_IN_MENU`) và kiểm lại thực đơn khi đổi hạn chế: Phase 5
- [x] T011 [US2] Tạo `BE/nutrition/danhmuc/web/DanhMucController.java`: `/dinh-duong/che-do-an`, `/{id}`, `/{id}/ngung-hieu-luc`, `/thanh-phan-thuc-pham`, `/mon-an`, `/{id}`, `/{id}/{lenh}`, `/bua-an`, `/{id}`, `/loai-do-an-cam`, `GET /nguoi-cao-tuoi/{nctId}/doi-chieu-mon`; màn web `frontend/src/tinh-nang/dinh-duong/danh-muc/`
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `nutrition.web.NutritionCatalogController` đã có 9 thao tác:
    - `GET`, `POST /nutrition/diets`;
    - `PUT /diets/{id}`, `POST /diets/{id}/deactivate`;
    - `POST /food-ingredients`;
    - `GET`, `POST /dishes`;
    - `PUT /dishes/{id}`, `POST /dishes/{id}/{command}`.
    
    `ApiContractIT` so khớp với `api-011.yaml` đã dịch. Các thao tác chưa cài nằm trong tập `pending`. Còn lại:
    - meals, forbidden-food-types: chờ A;
    - `GET /residents/{residentId}/dish-conflict-check`: Phase 5;
    - màn web: chờ chủ dự án duyệt bố cục (DESIGN.md mục 13, team-plan mục 8).

**Checkpoint**: Món và chế độ ăn chạy độc lập.

---

## Phase 4: User Story 1 – Bác sĩ gán chế độ ăn; thay đổi liên quan điều trị bắt buộc lý do (Priority: P1)

**Goal**: Bản gán Đề xuất → Áp dụng (Hiệu lực/Chờ hiệu lực) → Ngừng; lý do chuyên môn khi `CanLyDoChuyenMon`; cảnh báo xung đột xác nhận; chế độ mặc định; trạng thái cuối (FR-010 → FR-017, BR-M08-03).

**Independent Test**: A có "Thường" Hiệu lực; áp dụng "Tiểu đường" kết cấu "mềm" thiếu lý do (bị chặn) rồi có lý do; lập bản cho ngày mai; trạng thái cuối hủy bản chờ.

### Tests cho User Story 1

- [x] T012 [P] [US1] Test đơn vị `BT/nutrition/CanLyDoChuyenMonTest.java` và `BT/nutrition/BR_M08_03_BanGanIT.java`, `FR_015_TrangThaiCuoiIT.java`: một bản chờ, một bản Hiệu lực mỗi người (`UNIQUE`); hai Bác sĩ áp dụng đồng thời; trigger chặn sửa nội dung; listener dị ứng mới đối chiếu và `daKiemTra` (Q1 → Q5)
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* đã có `ProfessionalReasonRuleTest` (8, FR-013 a–d, tên `CanLyDoChuyenMonTest` trong tasks), `BR_M08_03_DietAssignmentIT` (16: lý do chuyên môn, hủy, Chờ hiệu lực → Hiệu lực qua việc theo phút, `UNIQUE` một bản chờ và một bản Hiệu lực, trigger khóa nội dung và trạng thái cuối, bếp không xem bản gán, chế độ mặc định, `STILL_ASSIGNED`), `BR_M08_03_ConcurrentApplyIT` (hai Bác sĩ áp dụng đồng thời), `FR_015_FinalStatusIT` (5, tên `FR_015_TrangThaiCuoiIT` trong tasks), `FR_021_AllergyCheckIT` (gỡ cờ `daKiemTra` dinh dưỡng, chạy lại không lỗi) đều xanh. Còn thiếu "listener dị ứng mới đối chiếu": đối chiếu với thực đơn, suất đã chốt, đồ ăn gia đình cần dữ liệu của Phase 5, 6, 9; hiện listener chỉ gỡ cờ.
  - *Ghi chú 2026-11-06, Phase 5–6 (vẫn chưa đánh [x]):* dị ứng mới đối chiếu thực đơn (`AllergyMenuCheck`, `FR_016_AssignmentMenuConflictIT` `FR_021_…`) và suất đã chốt (`ClosingEventHandlers` cho `AllergyRecorded`, `OtherAllergyRecorded`, `AllergyExcluded`). Còn đối chiếu đồ ăn gia đình (Phase 9).

### Implementation cho User Story 1

- [x] T013 [US1] Tạo bảng `ban_gan_che_do_an` (`trang_thai` `DE_XUAT|CHO_HIEU_LUC|HIEU_LUC|NGUNG|DA_HUY`; `can_cu` `DANH_GIA|XEM_LAI|KHAC`; cột sinh `khoa_cho`, `khoa_hieu_luc` `UNIQUE`; trigger), `ban_gan_han_che` trong `MIG/V011_03__ban_gan.sql` **Theo R-15 (data-model mục 0; bổ sung 2026-10-30): giữ bảng `ban_gan_che_do_an` (bảng đích); bảng con `ban_gan_han_che` không tạo, danh sách `di_nguyen_id` ghi vào cột `JSON` `han_che_rieng` của bản gán (ghi một lần cùng bản gán).**
  - *Ghi chú 2026-11-06:* `V011_03__diet_assignment.sql` tạo bảng `diet_assignment` (tên cột theo glossary mục 17.6), `personal_restrictions` kiểu `JSON` thay `ban_gan_han_che`, cột sinh `pending_key`, `active_key` có `UNIQUE`, `CHECK` hiệu lực và lý do hủy, trigger `trg_diet_assignment_lock_content` (khóa nội dung, chặn đổi trạng thái cuối). Khai nhóm dữ liệu `BUSINESS`; không khóa ngoại sang module khác.
- [x] T014 [US1] Cài `BE/nutrition/chedoan/internal/BanGanService.java` (lập, áp dụng có xác nhận cảnh báo, hủy; khóa `dinh_duong_nct`; chế độ mặc định khi không có bản gán), việc theo phút Chờ hiệu lực → Hiệu lực, bean `ThamGiaChuyenTrangThai` (`@Order` sau 006, trước 010; tác động trạng thái cuối), listener `DiUngDaGhiNhan`, `DiUngKhacDaGhiNhan`, `DiUngDaLoaiTru`; `CheDoAnApi` (research M-05) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `dinh_duong_nct` → `trang_thai_nct`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* đã có `DietAssignmentService` (lập, áp dụng, hủy; khóa `M08.RESIDENT_NUTRITION`; `READ_COMMITTED`), `PendingAssignmentTask` (việc theo phút), `M08StatusParticipant` (`@Order(50)`: trạng thái cuối, báo Bác sĩ khi Hoàn tất tiếp nhận chưa có bản gán), `NutritionAllergyListener` (`AllergyRecorded` → `markChecked(NUTRITION)`), `DietQueries` (`DietApi` thật). Còn thiếu: cảnh báo FR-016 và `WARNINGS_NOT_CONFIRMED` có nội dung (cần bảng `menu`, Phase 5; hiện danh sách cảnh báo rỗng); phát sinh cho bữa đã chốt chưa tới giờ ở FR-017 và FR-015 (Phase 6); listener `OtherAllergyRecorded`, `AllergyExcluded` (đối chiếu thực đơn, suất đã chốt, đồ ăn gia đình: Phase 5, 6, 9); thông báo "chưa gán chế độ ăn" ở mỗi lần chốt (Phase 6).
  - *Ghi chú 2026-11-06, Phase 5:* cảnh báo FR-016 đã có nội dung: lập đề xuất trả xung đột (`allergies`, mỗi món mỗi loại, có món thay thế phù hợp chưa) và bữa không có món trong thực đơn đã công bố (`coverageGaps`); Áp dụng chặn `WARNINGS_NOT_CONFIRMED` khi chưa xác nhận (`FR_016_AssignmentMenuConflictIT`). Còn thiếu như ghi chú trên: FR-017, FR-015 cho bữa đã chốt; listener `OtherAllergyRecorded`, `AllergyExcluded`; thông báo "chưa gán chế độ ăn" ở lần chốt.
  - *Ghi chú 2026-11-06, Phase 6:* phần còn lại đã có: phát sinh cho bữa đã chốt chưa tới giờ khi bản gán Hiệu lực (FR-017, `DietAssignmentTookEffect`) và khi người cao tuổi đổi trạng thái, kể cả trạng thái cuối (FR-015, `ResidentStatusChanged`); listener `OtherAllergyRecorded`, `AllergyExcluded`; thông báo "chưa gán chế độ ăn" ở mỗi lần chốt (`PortionsClosed`). Test `BR_M08_13_AdjustmentIT` (đổi trạng thái), `FR_015_FinalStatusIT`.
- [x] T015 [US1] Tạo endpoint `/nguoi-cao-tuoi/{nctId}/ban-gan-che-do-an`, `/ban-gan-che-do-an/{id}/ap-dung`, `/huy`; màn web `frontend/src/tinh-nang/dinh-duong/ban-gan/`
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `nutrition.web.DietAssignmentController` đã có 4 thao tác (`GET`, `POST /residents/{residentId}/diet-assignments`, `POST /diet-assignments/{id}/apply`, `POST /diet-assignments/{id}/cancel`); `ApiContractIT` so khớp, đã gỡ khỏi tập `pending`. Màn web `frontend/src/features/nutrition/assignment/` chờ chủ dự án duyệt bố cục (DESIGN.md mục 13, team-plan mục 8).

**Checkpoint**: Bản gán chạy độc lập.

---

## Phase 5: User Story 3 – Nhân viên bếp lập thực đơn tuần, Bác sĩ công bố (Priority: P1)

**Goal**: Thực đơn tuần Nháp → Chờ công bố → Công bố → Đã áp dụng; kiểm tra độ phủ, lặp món, dị ứng trong một lần; xác nhận chế biến; đổi món có lý do; nhắc (FR-023 → FR-032, BR-M08-06 → 08, 16).

**Independent Test**: 4 chế độ ăn có người dùng; thực đơn thiếu bữa tối thứ 6 của "Tiểu đường", "Cháo gà" ba sáng liên tiếp, một món chứa thành phần dị ứng → mọi lỗi hiện một lần.

### Tests cho User Story 3

- [x] T016 [P] [US3] Test đơn vị `BT/nutrition/KiemTraDoPhuTest.java`, `LapMonTest.java` (qua tuần)
  - *Ghi chú 2026-11-06:* `CoverageCheckTest` (6, tên `KiemTraDoPhuTest` trong tasks), `DishRepetitionCheckTest` (7, tên `LapMonTest`; qua ranh giới tuần, món thay thế không tính, nhiều bữa một ngày tính một ngày), thêm `AllergyWarningRuleTest` (5, cảnh báo dị ứng FR-025 c) đều xanh; đỏ trước khi cài (8/18).
- [x] T017 [P] [US3] Test `BT/nutrition/BR_M08_06_DoPhuIT.java`, `BR_M08_07_LapMonIT.java`, `BR_M08_08_DoiMonIT.java`, `BR_M08_16_NhacThucDonIT.java`: một thực đơn chưa hủy mỗi tuần (`UNIQUE khoa_tuan`); cảnh báo dị ứng cho bếp chỉ có số người, không có tên; đổi món sau chốt tạo phát sinh (Q6 → Q19)
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `BR_M08_06_CoverageIT` (8: thiếu độ phủ liệt kê một lần, xác nhận kiểm tra chế biến, số người không tên, một thực đơn chưa hủy mỗi tuần, tuần đã kết thúc, sửa chỉ khi Nháp và `If-Match`, sao chép đánh dấu món cần thay, bếp không công bố 403, trả lại cần lý do, rút lại, công bố chạy lại độ phủ, tuần đã bắt đầu thì Đã áp dụng, FR-002 `STILL_IN_MENU`), `BR_M08_07_DishRepetitionIT` (2: lặp món qua tuần trước đã công bố phải xác nhận; bếp chỉ thấy số người, Bác sĩ thấy tên), `BR_M08_08_DishSwapIT` (7: áp ngay, lịch sử trước/sau, báo Bác sĩ Nhẹ/Trung bình, lý do, món Hiệu lực phù hợp, bữa đã qua, cảnh báo mới phải xác nhận, trigger; thêm món cho bữa trống, bớt món bị chặn khi bữa mất món cuối, chỉ đổi món thay thế), `BR_M08_16_MenuReminderIT` (3), `FR_016_AssignmentMenuConflictIT` (2) đều xanh (Q11 → Q18). Còn Q19 "phát sinh cho các tầng" khi đổi món sau chốt: cần lần chốt (Phase 6). Bữa trong test ghi thẳng `catalog_item` vì lệnh danh mục bữa chờ Function của A.
  - *Ghi chú 2026-11-06, Phase 6:* Q19 phần phát sinh: đổi món sau chốt tạo phát sinh `DISH_SWAP` mỗi tầng/khu có người dùng chế độ ăn (`BR_M08_13_AdjustmentIT` `FR_039_dishSwapAfterClosing…`).

### Implementation cho User Story 3

- [x] T018 [US3] Tạo `MIG/V011_04__thuc_don.sql`: `thuc_don` (`trang_thai` `NHAP|CHO_CONG_BO|CONG_BO|DA_AP_DUNG|DA_HUY`; cột sinh `khoa_tuan` `UNIQUE`; `xac_nhan_che_bien` bắt buộc khi gửi), `thuc_don_mon`, `thuc_don_mon_thay_the` (sửa chỉ khi `NHAP`, trigger), `thuc_don_kiem_tra` (nhóm 3), `doi_mon` (nhóm 3) (data-model mục 5) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `thuc_don_kiem_tra`, `doi_mon` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *Ghi chú 2026-11-06:* `V011_04__menu.sql` tạo `menu` (tên cột glossary mục 17.6): nội dung `content` kiểu `JSON` thay `thuc_don_mon`, `thuc_don_mon_thay_the`; `active_week` sinh `UNIQUE`; `CHECK` thứ Hai, xác nhận kiểm tra chế biến khi Chờ công bố trở đi, người công bố, lý do hủy; trigger `trg_menu_lock_content` (nội dung chỉ sửa khi Nháp, hoặc kèm `last_swap_id` mới của lệnh Đổi món; Đã hủy là cuối; Công bố, Đã áp dụng không quay lại). `thuc_don_kiem_tra`, `doi_mon` là `record_event` loại `M08.MENU_CHECK`, `M08.DISH_SWAP`.
- [x] T019 [US3] Cài `BE/nutrition/thucdon/internal/ThucDonService.java` (lập, sao chép, kiểm tra một lần, gửi, công bố, trả lại, hủy, đổi món; `ThucDonChungApi`; việc hằng ngày nhắc lập thực đơn và Đã áp dụng) (research M-07)
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `MenuService` (lập, sao chép, sửa Nháp, thông tin trước khi gửi, kiểm tra một lần, gửi, rút, hủy, trả lại, công bố, đổi món), `MenuChecker` (dữ liệu cho ba hàm thuần, FR-027), `MenuQueries` (`GeneralMenuApi`, `ExpectedMealApi` thật), `MenuAppliedTask`, `MenuReminderTask` (việc hằng ngày), `ResidentMenuConflicts` (FR-016, FR-020, FR-022), `MealTimes` (CFG-M08-04), sự kiện thông báo `MenuSubmitted`, `MenuReturned`, `MenuPublished`, `MenuPublicationReminder`, `DishSwapped`. Còn thiếu: đổi món sau thời điểm chốt tạo phát sinh (FR-039, Phase 6); kiểm lại FR-005 với thực đơn chưa hết tuần khi sửa hạn chế chế độ ăn (FR-002, T008) chưa làm.
  - *Ghi chú 2026-11-06, Phase 6:* phần còn lại đã có: đổi món sau thời điểm chốt tạo phát sinh (FR-039, `PortionClosingService.dishSwapped`); kiểm lại FR-005 khi sửa hạn chế chế độ ăn (FR-002).
- [x] T020 [US3] Tạo endpoint `/thuc-don`, `/{id}`, `/{id}/truoc-khi-gui`, `/{id}/kiem-tra`, `/{id}/{lenh}`, `/{id}/doi-mon`; màn web `frontend/src/tinh-nang/dinh-duong/thuc-don/` (mọi lỗi hiện một lần) với test Vitest
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `nutrition.web.MenuController` có 8 thao tác `/menus…`; `DishSwap` sửa theo lựa chọn của chủ dự án (thay, thêm, bớt món, đổi món thay thế; plan ghi chú Phase 5); `GET /residents/{residentId}/dish-conflict-check` ở `DietAssignmentController` (quyền `ALLERGY_CONDITION VIEW` theo người cao tuổi, bếp 403); cả 9 đã gỡ khỏi `pending` của `ApiContractIT`. Màn web `frontend/src/features/nutrition/menu/` chờ duyệt bố cục (team-plan mục 8).

**Checkpoint**: Thực đơn chạy độc lập.

---

## Phase 6: User Story 4 – Nhân viên bếp chốt số suất trước mỗi bữa (Priority: P1)

**Goal**: Tính sẵn khi bếp mở lệnh (chỉ xem); lệnh Chốt suất lập lần chốt, người được tính, suất theo chế độ, phiếu mỗi tầng/khu, suất ăn người thân trong một transaction; phát sinh sau chốt từ listener; suất giữ; món an toàn tại mốc CFG-M08-05; lịch bữa cho 005; DBR-27 (FR-033 → FR-043, BR-M08-01, 09, 13).

**Independent Test**: 3 tầng và khu bán trú: 40 nội trú (2 Tạm vắng, 1 bệnh viện, 3 đi hoạt động ngoài viện qua bữa trưa), 8 bán trú có lịch, người thân ở lại có đăng ký ăn → số suất đúng tính tay.

### Tests cho User Story 4

- [x] T021 [P] [US4] Test đơn vị `BT/nutrition/NguoiDuocTinhTest.java`, `TinhPhatSinhTest.java`
  - *Ghi chú 2026-11-06:* `CountedResidentsTest` (7, tên `NguoiDuocTinhTest`), `AdjustmentCalculatorTest` (5, tên `TinhPhatSinhTest`), thêm `PortionContentTest` (8, suất đặc biệt, món thay thế, món an toàn) đều xanh; đỏ trước khi cài.
- [x] T022 [P] [US4] Test `BT/nutrition/BR_M08_01_ChotSuatIT.java`, `BR_M08_13_PhatSinhIT.java`, `FR_040a_SuatGiuIT.java`, `FR_037a_MonAnToanIT.java`, `FR_043_SuatNguoiThanIT.java`, `DBR_27_TongPhieuIT.java`: một lần chốt mỗi bữa ngày (hai lệnh đồng thời); không tự chốt; phát sinh ≤ 5 phút, idempotent (SC-002); suất người thân phát `SuKienTinhPhiNguon` `SUAT_AN_NGUOI_THAN`; SC-001 bộ 7 ngày × 60 người (Q20 → Q31)
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `BR_M08_01_PortionClosingIT` (5: Q20 → Q22, Q24, Q31, FR-043), `BR_M08_13_AdjustmentIT` (6: Q23, Q25 → Q27, đổi món, chỉ định món), `FR_040a_HeldPortionIT` (2: Q28), `FR_037a_SafeDishIT` (3: Q29, Q30) xanh, trên nền `ClosingTestBase` (bản giả dùng chung của 003, 004, 012, 014; giờ bữa qua spy). Còn: `DBR_27_TongPhieuIT` (tổng suất trên phiếu, cần phiếu của Phase 7), hai lệnh chốt đồng thời (test hiện là tuần tự; khóa `M08.DAILY_MEAL` và `UNIQUE` đã có), bộ SC-001 7 ngày × 60 người.
  - *Ghi chú 2026-11-06, Phase 7 (vẫn chưa đánh [x]):* thêm `DBR_27_SlipTotalIT` (tổng suất trên phiếu = số đã chốt + phát sinh cần giao đã xác nhận; phát sinh chưa xác nhận và suất giữ không làm đổi tổng; mỗi bữa, tầng/khu một phiếu), xanh. Còn: hai lệnh chốt đồng thời, bộ SC-001 7 ngày × 60 người.

### Implementation cho User Story 4

- [x] T023 [US4] Tạo `MIG/V011_05__chot_suat.sql`: `bua_ngay` (`UNIQUE (bua_an_id, ngay)`; `gio_du_kien` chốt từ CFG-M08-04), `lan_chot` (`bua_ngay_id UNIQUE`), `nguoi_duoc_tinh` (`loai` `NOI_TRU|BAN_TRU|NGUOI_THAN`), `suat_theo_che_do`, `phat_sinh` (`khoa_su_kien UNIQUE`; `can_giao`; xác nhận đặt một lần), `yeu_cau_suat_bo_sung`, `suat_an_nguoi_than` (`trang_thai` `DA_GHI|DA_HUY`; `ma_su_kien_tinh_phi`) (data-model mục 6) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `bua_ngay` → `khoa_ky_thuat`; `nguoi_duoc_tinh`, `suat_theo_che_do` → cột `JSON` của `lan_chot`; `phat_sinh`, `yeu_cau_suat_bo_sung` → `su_kien_chung`; `suat_an_nguoi_than` → `ho_so_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *Ghi chú 2026-11-06:* `V011_05__portion_closing.sql` tạo `portion_closing` (nhóm 3, `DataGroup=RECORD`, trigger `no_update`, `no_delete` trừ tài khoản loại bỏ dữ liệu; `UNIQUE (daily_meal_id)`, `UNIQUE (meal_id, meal_date)`; người được tính, suất theo chế độ ở cột `JSON`). `bua_ngay` không có bảng: id bữa ngày suy ra từ bữa và ngày, khóa `M08.DAILY_MEAL:<id>` (plan ghi chú Phase 6); phát sinh, yêu cầu suất bổ sung ở `record_event` (`M08.ADJUSTMENT`, `M08.EXTRA_PORTION_REQUEST`); suất ăn người thân, phiếu, suất đặc biệt ở `simple_record` (`M08.FAMILY_MEAL`, `M08.MEAL_SLIP`, `M08.SPECIAL_PORTION`).
- [x] T024 [US4] Cài `BE/nutrition/chotsuat/internal/ChotSuatService.java`: tính sẵn (`NguoiDuocTinh` từ trạng thái 001, vị trí 003, vắng 004, có mặt 005, `LuotOLaiAnPort`, `ChuyenDiBuaAnPort`), lệnh chốt dưới khóa `bua_ngay` (phiếu và suất đặc biệt US5 trong cùng transaction), suất giữ, món an toàn, suất ăn người thân (research M-08) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `bua_ngay` → `khoa_ky_thuat`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *Ghi chú 2026-11-06:* `PortionClosingService` (tính sẵn, chốt dưới khóa bữa ngày, lập phiếu mỗi tầng/khu và suất đặc biệt cùng lệnh, suất ăn người thân kèm `CostSourceBilled`, món an toàn ngoài giờ hành chính CFG-M13-06), `PortionCounter` (gom dữ liệu 001, 003, 004, 005, 012, 014), `DailyMeals`. Quyền: chốt `MEAL_PREPARATION CREATE`, xem `MEAL_COUNT_TICKET VIEW` (chủ dự án chọn 2026-11-06). Bữa chưa đặt giờ thì chặn `MEAL_TIME_NOT_SET` (chủ dự án chốt).
- [x] T025 [US4] Cài listener phát sinh trong `SuKienM08Listener.java` (`NguoiCaoTuoiDaChuyenTrangThai`, `DaChuyenGiuong`, `LuotVang*`, `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi`, `PhuLucDaApDung`, `TrangThaiCoMat*`, `LuotOLaiDaDoi`, `ChuyenDiDaDoi`, đổi bản gán, đổi món) và lệnh bếp xác nhận phát sinh; việc theo phút: nhắc chốt, món an toàn, suất giữ, nhắc phát sinh
  - *Ghi chú 2026-11-06:* `ClosingEventHandlers` (+ `ClosingEventListener` sau commit) tính lại theo người cho `ResidentStatusChanged`, `BedTransferred`, `AbsenceOpened/Extended/Closed`, `AttendanceChanged/Determined`, `DayCareAbsenceNoticeRecorded`, `DayCareClosureDayChanged`, `DietAssignmentTookEffect`, `AllergyRecorded`, `OtherAllergyRecorded`, `AllergyExcluded`, `StayMealChanged`, `TripMealChanged`; đổi món (`MenuService.swap`) và xác nhận bản sửa món (`DishService`) gọi tính lại thẳng trong lệnh, vì chỉ M13 được nghe sự kiện có yêu cầu thông báo (`M13ArchTest`); `AdjustmentService` (bếp xác nhận `MEAL_PREPARATION CONFIRM`, lập phiếu tầng chưa có; suất bổ sung; phục vụ, hủy suất giữ; Bác sĩ chỉ định món `DIET UPDATE`); `ClosingMinuteTasks` (nhắc chốt, món an toàn tại CFG-M08-05, suất giữ, nhắc phát sinh). Hai sự kiện của danh sách FR-039 chưa có nguồn phát: Hoàn tất tiếp nhận (đi qua `ResidentStatusChanged`), đổi giờ dự kiến trở lại của chuyến (014 chưa có).
- [x] T026 *(2026-11-07, Phase 8: đã cài `MealTaskSource` — `validate`, `sources`, `confirmServing`; còn lại bữa bỏ vì vắng → `sourceCancelled`, chưa đánh [x])* [US4] Cài `NguonCongViecPort` loại `BUA` (`kiemTra`, `xacNhanPhucVu`; bữa bỏ vì vắng → `nguonBiHuy`) và `LichBuaApi` trong `BE/nutrition/phucvu/internal/`
  - *(2026-11-12, A, luồng chăm sóc/sự cố/thông báo)* "Đổi api" `care.api.TaskSourceApi.sourceCancelled(type, sourceId, residentId, reason)` (giữ hàm cũ; 005 hủy công việc Chưa đến hạn của căn cứ chỉ của người đó). 011: tính lại suất sau chốt có phát sinh "−" (`REMOVED`) do vắng (`ABSENCE`, `DAY_CARE_ATTENDANCE`) thì gọi hàm mới cho bữa ngày đó (`PortionClosingService.recompute`). Bữa chưa chốt: người vắng không vào lịch bữa, công việc đã lập do 005 FR-020 hủy. `BR_M08_13_AdjustmentIT.FR_042_…` đỏ rồi xanh với `CareTaskSources` thật; `T003_PublicCareApiTest`, `M04ArchTest`, `M08ArchTest`, `BR_M08_01_PortionClosingIT` xanh.
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `MealScheduleApi` thật (`MealScheduleQueries`: bữa trong ngày, người có trong lịch bữa). Bản cài `TaskSourcePort` loại `MEAL` và báo bữa bị bỏ vì vắng chờ C (005): `FakeM04Config` đã đăng ký nguồn `MEAL` giả nên thêm bean thật làm `CareTaskValidator` thấy hai nguồn; `TaskSourceApi.sourceCancelled(type, sourceId, reason)` không có người cao tuổi nên không hủy được bữa của một người (team-plan mục 8).
  - *(2026-11-09, chờ C (005), đã ghi team-plan mục 8)* Còn bữa bỏ vì vắng → `sourceCancelled`: `TaskSourceApi.sourceCancelled(type, sourceId, reason)` không có người cao tuổi, gọi cho một bữa sẽ hủy việc "hỗ trợ ăn" của mọi người trong bữa. Cần 005 thêm `residentId` (hoặc hàm hủy theo người); có rồi thì gọi trong listener lượt vắng, chuyến, trạng thái (research M-08), viết test trước, rồi đánh `[x]`.
- [x] T027 [US4] Tạo endpoint `/bua-ngay`, `/{id}/tinh-san`, `/{id}/chot-suat`, `/{id}/phat-sinh`, `/phat-sinh/{id}/xac-nhan`, `/bua-ngay/{id}/suat-bo-sung`, `/suat-dac-biet/{id}/phuc-vu-suat-giu`, `/huy-suat-giu`, `/chi-dinh-mon`; màn web `frontend/src/tinh-nang/dinh-duong/chot-suat/` (tính sẵn chỉ xem, nút chốt tách riêng)
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `nutrition.web.PortionClosingController` có 9 thao tác (`/daily-meals`, `…/precomputed`, `…/close-portions`, `…/adjustments`, `/adjustments/{id}/confirm`, `…/extra-portions`, `/special-portions/{id}/serve-held-portion`, `/discard-held-portion`, `/assign-dish`), đã gỡ khỏi `pending` của `ApiContractIT`. Màn web `frontend/src/features/nutrition/closing/` chờ duyệt bố cục (team-plan mục 8).

**Checkpoint**: P1 hoàn tất.

---

## Phase 7: User Story 5 – Chuẩn bị, giao phiếu bữa ăn; tầng kiểm đếm và nhận (Priority: P2)

**Goal**: Phiếu mỗi tầng/khu; suất đặc biệt có nhãn; giao khi mọi suất đặc biệt đã chuẩn bị và có lưu mẫu; nhận; sai lệch và xử lý; phần bổ sung; báo giao trễ; DTO bếp không có trường sức khỏe (FR-044 → FR-048b, BR-M08-09 → 12, SC-008).

**Independent Test**: Bữa trưa Tầng 2 có 14 suất (12 Thường, 2 Tiểu đường) và 3 suất đặc biệt; giao khi còn 1 suất chưa chuẩn bị (bị chặn); giao; tầng báo thiếu 1; bếp bổ sung.

### Tests cho User Story 5

- [x] T028 [P] [US5] Test `BT/nutrition/BR_M08_10_GiaoPhieuIT.java`, `BR_M08_11_NhanPhieuIT.java`, `BR_M08_12_GiaoTreIT.java`, `FR_048b_PhanBoSungIT.java`, `SC_008_DtoBepTest.java` (quét mọi DTO, thông báo tới bếp) (Q32 → Q37)
  - *Ghi chú 2026-11-06:* `BR_M08_10_SlipDeliveryIT` (3: Q32, Q37, phát sinh đưa phiếu về Đã chốt), `BR_M08_11_SlipReceiptIT` (2: Q33, Q34), `BR_M08_12_LateDeliveryIT` (2: Q36, chốt sau giờ bữa không báo), `FR_048b_SupplementIT` (2: Q35, yêu cầu suất bổ sung là phần bổ sung), `SC_008_KitchenDtoTest` (2: quét DTO bếp, thông báo tới bếp), trên nền `SlipTestBase`.

### Implementation cho User Story 5

- [x] T029 [US5] Tạo `MIG/V011_06__phieu.sql`: `phieu_bua_an` (`UNIQUE (bua_ngay_id, tang_khu_vuc_id)`; `trang_thai` `DA_CHOT|DA_CHUAN_BI|DA_GIAO|CO_SAI_LECH|DA_NHAN`), `suat_dac_biet`, `sai_lech_phieu` (nhóm 3; xử lý đặt một lần), `phan_bo_sung` (nhóm 3; đúng một nguồn khác `NULL`) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `phieu_bua_an`, `suat_dac_biet` → `ho_so_chung`; `sai_lech_phieu`, `phan_bo_sung` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *Ghi chú 2026-11-06:* không tạo `V011_06` (R-15, không có DDL): phiếu, suất đặc biệt ở `simple_record` (`M08.MEAL_SLIP` có `uniqueKey` theo bữa ngày, tầng/khu; `M08.SPECIAL_PORTION`); sai lệch, phần bổ sung ở `record_event` (`M08.SLIP_DISCREPANCY`, `M08.SUPPLEMENTARY_PORTION`; xử lý, nhận đặt một lần rồi khóa dòng; đúng một nguồn kiểm ở `SupplementaryPortion`).
- [x] T030 [US5] Cài `BE/nutrition/phieu/internal/PhieuService.java` (chuẩn bị, giao, nhận, sai lệch, xử lý, phần bổ sung; DTO bếp và DTO tầng tách riêng; việc theo phút báo giao trễ một lần) (research M-09)
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `MealSlipService` (chuẩn bị suất đặc biệt và phiếu, giao theo FR-047, nhận, sai lệch và xử lý, phần bổ sung; DTO bếp `KitchenSlip`, DTO tầng `FloorSlip`), `LateSlipTask` (báo giao trễ một lần), tạo phần bổ sung khi bếp xác nhận phát sinh sau giao (`AdjustmentService`), phiếu Đã chuẩn bị về Đã chốt khi có phát sinh (`PortionClosingService`). Còn: khu bán trú không có ai được phân công thì báo Quản lý viện thay Trưởng tầng (FR-048), hiện dùng cách thay mặc định của 009.
- [x] T031 [US5] Tạo endpoint `/phieu-bua-an`, `/suat-dac-biet/{id}/da-chuan-bi`, `/phieu-bua-an/{id}/{lenh}`, `/phan-bo-sung/{id}/{lenh}`; màn web `frontend/src/tinh-nang/dinh-duong/phieu/`, app `mobile/lib/nhan-vien/dinh-duong/` (nhận phiếu, sai lệch, suất bổ sung)
  - *Ghi chú 2026-11-06 (chưa đánh [x]):* `MealSlipController` có 4 thao tác (`GET /meal-slips`, `/special-portions/{id}/mark-prepared`, `/meal-slips/{id}/{command}`, `/supplementary-portions/{id}/{command}`), đã gỡ khỏi `pending` của `ApiContractIT`. Hợp đồng thêm `labels[].id`, `supplementaryPortions`, lệnh `report-discrepancy` của phần bổ sung (chủ dự án chọn 2026-11-07). Dấu mức phiếu (FR-046) đã có `flags` (2026-11-07, Phase 13). Màn web `frontend/src/features/nutrition/slip/` và app `mobile/lib/staff/nutrition/` chờ duyệt bố cục (team-plan mục 8).

**Checkpoint**: Phiếu chạy độc lập.

---

## Phase 8: User Story 6 – Xác nhận phục vụ suất đặc biệt (Priority: P2)

**Goal**: Xác nhận đúng người, đúng suất; chặn khi xung đột dị ứng; đã ăn thì ghi sự cố qua `SuCoLenhApi`; ngoại tuyến (FR-049 → FR-051, BR-M08-14).

**Independent Test**: Phục vụ suất của E cho E; suất của E cho G; cho F một suất chứa "tôm" khi F vừa có dị ứng tôm sau chốt; khai "đã ăn" → sự cố; "chưa ăn" → không sự cố.

### Tests cho User Story 6

- [x] T032 [P] [US6] Test `BT/nutrition/BR_M08_14_XacNhanPhucVuIT.java`, `FR_050_ChanKetQuaAnIT.java`; app `mobile/test/nhan-vien/xac_nhan_phuc_vu_test.dart` (chọn suất theo nhãn, khai đã ăn/chưa ăn, ngoại tuyến) (Q38 → Q41) *(2026-11-07, Phase 8: xong và xanh — `BR_M08_14_ServingConfirmationIT` 5 test, `FR_050_ServingConfirmationBlocksEatingIT` 2 test; app `mobile/test/staff/nutrition/serving_confirmation_screen_test.dart` 5 test, màn theo mẫu `PickupScreen`)*

### Implementation cho User Story 6

- [x] T033 [US6] Tạo bảng `xac_nhan_phuc_vu` (nhóm 3; `ket_qua` `DUNG_SUAT|CHAN_XUNG_DOT_DA_AN|CHAN_XUNG_DOT_CHUA_AN`) trong `MIG/V011_07__phuc_vu.sql`; cài `BE/nutrition/phucvu/internal/XacNhanPhucVuService.java` (`XungDot` tại lúc phục vụ; đã ăn → `SuCoLenhApi`; chưa ăn → `YeuCauTaoCanhBao`), `NgoaiTuyenHandler` `M08.XAC_NHAN_PHUC_VU`, `DinhChinhDuocProvider` `M08.XAC_NHAN_PHUC_VU` và endpoint `POST /suat-dac-biet/{id}/xac-nhan-phuc-vu` (research M-10) *(2026-11-07, Phase 8: đã cài — không có bảng riêng theo R-15, `M08.SERVING_CONFIRMATION` ở `record_event`; màn app `mobile/lib/staff/nutrition/serving_confirmation_screen.dart`, mục "Xác nhận phục vụ suất ăn" trong "Thêm" của `staff_root.dart`)* **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `xac_nhan_phuc_vu` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**

**Checkpoint**: Phục vụ an toàn.

---

## Phase 9: User Story 7 – Đồ ăn gia đình mang vào (Priority: P2)

**Goal**: `DoiChieuDoAn` với dị ứng, hạn chế, loại cấm; Không sử dụng / Cần xác nhận / Được sử dụng / Đã kết thúc; xung đột mới, hết hạn chuyển Không sử dụng (FR-052 → FR-056, BR-M08-04).

**Independent Test**: 4 lần gửi: bánh có "đậu phộng" cho người dị ứng; "rượu thuốc" (bị cấm); hộp cơm "không rõ"; trái cây cho người không có hạn chế.

### Tests cho User Story 7

- [x] T034 [P] [US7] Test đơn vị `BT/nutrition/DoiChieuDoAnTest.java` và `BT/nutrition/BR_M08_04_DoAnGiaDinhIT.java` (không có chuyển `KHONG_SU_DUNG → DUOC_SU_DUNG`) (Q42 → Q44) *(2026-11-07, Phase 9: xong và xanh — hàm thuần là `FamilyFoodCheck` nên test `FamilyFoodCheckTest` 10 test, `BR_M08_04_FamilyFoodIT` 12 test; app `mobile/test/staff/nutrition/family_food_screen_test.dart` 6 test)*

### Implementation cho User Story 7

- [x] T035 *(2026-11-07, Phase 9: đã cài backend và app — không có bảng riêng theo R-15, `M08.FAMILY_FOOD` ở `simple_record`, loại đồ ăn cấm `M08.FORBIDDEN_FOOD_TYPE` ở `catalog_item`; `FamilyFoodService`, `FamilyFoodCheck`, `ForbiddenFoodTypeService`, `FamilyFoodExpiryTask`, `FamilyFoodListener`, `FamilyFoodRetention`; màn app `mobile/lib/staff/nutrition/family_food_screen.dart`, mục "Đồ ăn gia đình" trong "Thêm"; còn màn web `frontend/src/features/nutrition/family-food/`, chờ duyệt bố cục — team-plan mục 8; chưa đánh [x])* [US7] Tạo bảng `do_an_gia_dinh` (`trang_thai` `KHONG_SU_DUNG|CAN_XAC_NHAN|DUOC_SU_DUNG|DA_KET_THUC`) và bảng con thành phần trong `MIG/V011_08__do_an_gia_dinh.sql`; cài `BE/nutrition/doangiadinh/internal/DoAnGiaDinhService.java` (khóa `dinh_duong_nct`; việc theo phút hạn dùng; listener dị ứng mới) và endpoint `/nguoi-cao-tuoi/{nctId}/do-an-gia-dinh`, `/do-an-gia-dinh/{id}/{lenh}`; màn web `frontend/src/tinh-nang/dinh-duong/do-an-gia-dinh/`, app phần đồ ăn gia đình (research M-11) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `do_an_gia_dinh` → `ho_so_chung`; `dinh_duong_nct` → `trang_thai_nct`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**

**Checkpoint**: Đồ ăn gia đình chạy độc lập.

---

## Phase 10: User Story 8 – Bác sĩ được báo xem lại chế độ ăn (Priority: P3)

**Goal**: `AnKemKeoDai` (005), `SutCanPhatHien` (007) → báo Bác sĩ; không tạo yêu cầu (FR-058, FR-059, BR-M08-05, Q-307).

**Independent Test**: B có 3 bữa kém liên tiếp, rồi sụt cân → Bác sĩ được báo; bác sĩ lập đề xuất có tham chiếu thông báo.

### Tests cho User Story 8

- [x] T036 [P] [US8] Test `BT/nutrition/BR_M08_05_XemLaiIT.java` (Q45) *(2026-11-07, Phase 10: xong và xanh — `BR_M08_05_ReviewIT` 4 test: ăn kém kéo dài rồi sụt cân mỗi sự kiện một thông báo Nhẹ, không yêu cầu, không nhắc lại sau 30 ngày; chế độ mặc định; bản gán tham chiếu thông báo (FR-059, Q46); bếp 403)*

### Implementation cho User Story 8

- [x] T037 [US8] Tạo bảng kỹ thuật `thong_bao_xem_lai` (không trạng thái) trong `MIG/V011_09__xem_lai.sql`; cài listener `BE/nutrition/xemlai/internal/XemLaiListener.java` và endpoint `GET /dinh-duong/thong-bao-xem-lai`; bản gán tham chiếu `thong_bao_xem_lai_id` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `thong_bao_xem_lai` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.** *(2026-11-07, Phase 10: xong — không có bảng riêng theo R-15, mốc `M08.REVIEW_NOTICE` ở `reminder_marker` (không `V011_09`); `ReviewNoticeService`, `ReviewNoticeStore`, `ReviewEventListener` (`ProlongedPoorEating` của 005, `WeightLossDetected` của 007), `GET /nutrition/review-notices`; bản gán chỉ nhận `reviewNoticeId` của chính người đó; 007 chưa có nơi phát `WeightLossDetected` — team-plan mục 8)*

**Checkpoint**: Xem lại chạy.

---

## Phase 11: User Story 9 – Lưu mẫu thức ăn mỗi bữa (Priority: P3)

**Goal**: Giao phiếu cần bản ghi lưu mẫu; hết hạn lưu CFG-M08-06; ghi hủy mẫu (FR-060 → FR-062, BR-M08-15).

**Independent Test**: Giao phiếu khi chưa có lưu mẫu (bị chặn); ghi lưu mẫu; giao; quá CFG-M08-06 → nhắc; ghi hủy mẫu.

### Tests cho User Story 9

- [x] T038 [P] [US9] Test đơn vị `BT/nutrition/MonCanLuuMauTest.java` và `BT/nutrition/BR_M08_15_LuuMauIT.java` (Q46, Q47) *(2026-11-07, Phase 11: xong và xanh — IT tên `BR_M08_15_RetainedSampleIT` (3 test), hàm thuần `DishesToRetain` (4 test))*

### Implementation cho User Story 9

- [x] T039 *(2026-11-07, Phase 11: đã cài backend — không có `V011_10` theo R-15, `M08.RETAINED_SAMPLE` ở `simple_record`; `RetainedSampleService`, `RetainedSampleExpiryTask`, `RetainedSampleController`; chưa đánh [x] vì còn màn web, chờ duyệt bố cục, team-plan mục 8)* [US9] Tạo bảng `luu_mau` (nhóm 3; `bua_ngay_id UNIQUE`; `het_han_luu`; hủy đặt một lần), `luu_mau_mon` trong `MIG/V011_10__luu_mau.sql`; cài `BE/nutrition/luumau/internal/LuuMauService.java` (`DinhChinhDuocProvider` `M08.LUU_MAU`; nhắc hủy mẫu) và endpoint `POST /bua-ngay/{id}/luu-mau`, `/luu-mau/{id}/huy-mau`; màn web `frontend/src/tinh-nang/dinh-duong/luu-mau/` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `luu_mau`, `luu_mau_mon` → `ho_so_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**

**Checkpoint**: Mọi user story của 011 chạy độc lập.

---

## Phase 12: Polish & Cross-Cutting

- [x] T045 *(Bổ sung 2026-10-18, Q-366)* Cài `BE/nutrition/api/SuatAnDuKienApi.java` và bản cài trong `BE/nutrition/phieu/internal/` theo research M-16; test `BT/nutrition/FR_063_SuatAnDuKienNguoiCaoTuoiIT.java` (quickstart A1, A2)

- [x] T040 [P] Cài `DinhDuongThongKeApi` (016)
- [x] T041 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/011-nutrition-management/contracts/api-011.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 011
- [x] T042 *(2026-11-07, chưa làm: `main` chưa có bản cài `StayMealPort` của 012 và `TripMealPort` của 014, 019 chưa có; team-plan mục 8)* Khi 012, 014, 019 có bản thật, chạy lại test US4 với bean thật thay T002; thay bản giả `NguonCongViecPort` `BUA` ở test 005
  - *(2026-11-12, A, xong)* Bỏ bản giả `MEAL` (`fakeMealTaskSource`, `removeRealMealTaskSource`) khỏi `FakeM04Config`: context 005 dùng `MealTaskSource` thật. Các IT quá hạn, bàn giao, lập việc (`CareOverdueTestBase` 15 lớp con, `BR_M04_01_ShiftTaskPlanningIT`) không kiểm điều riêng của bữa nên đổi căn cứ chung sang mục kế hoạch chăm sóc Hiệu lực (`CARE_PLAN_ITEM`, nguồn thật của 005, dữ liệu theo người của test nên không lẫn dữ liệu sót); `CareResultTestBase` (`FR_039`, `BR_M04_14`) giữ căn cứ `MEAL` với bản thật (bữa không có phiếu thì `confirmServing` đúng như bản giả); `T002_FakeTaskSourcesIT` kiểm `MEAL` không còn là bản giả; `BR_M08_14_ServingConfirmationIT` thêm một test gọi thẳng `MealTaskSource` (`validate`, `sources`, `confirmServing`). IT `care` (88) và `BR_M08_14` xanh, `*ArchTest`, `ModuleBoundaryTest` xanh.
  - *(2026-11-12, A, luồng E, chưa làm)* Rà để thay: `MealTaskSource` thật đọc mọi bữa Hiệu lực của danh mục (schema test dùng chung giữ bữa của test khác) và `isInMealSchedule` đi qua `PortionCounter` (9 port: hồ sơ, loại lưu trú, lượt vắng, lịch đến, có mặt bán trú, vị trí, cấu trúc khu, lượt ở lại, chuyến đi), còn `CareOverdueTestBase` (15 lớp con), `BR_M04_01_ShiftTaskPlanningIT`, `CareResultTestBase` (`FR_039`, `BR_M04_14` với căn cứ `MEAL`), `T002_FakeTaskSourcesIT` chạy trên hồ sơ, giường, ca giả (`FakeM01Config`, `FakeM04Config`). Phải dựng lại nền test theo `RealCareFixture` + danh mục bữa, CFG-M08-04 (mẫu `ClosingTestBase` của 011) hoặc đổi căn cứ chung của các IT quá hạn/bàn giao sang loại có nguồn thật; việc lớn, để phiên riêng. Giữ `[ ]`.
  - *(2026-11-12, A, luồng chăm sóc/sự cố/thông báo)* Test US4 (chốt suất, phát sinh) đã chạy với bean thật `StayMeals`, `TripMeals` và `CareTaskSources` (T026); `FakeTripMealPort`, `FakeNutritionConfig` không còn lớp nào dùng. Còn: thay bản giả `MEAL` của `FakeM04Config` ở 6 IT 005 cần dựng danh mục bữa và CFG-M08-04 trong test 005 (bản giả đang điều khiển `confirmServing`); chưa làm.
  - *(2026-11-09)* Giữ nguyên chờ 012 (`StayMealPort`), 014 (`TripMealPort`), 019; khi có bản thật chạy lại test US4 và thay bản giả `BUA` ở test 005.
  - *(2026-11-11, A)* `TripMealPort` có bean thật `TripMeals` (đọc `care.api.TripMealReadApi`, chủ dự án duyệt); `StayMealPort` (012) đã có từ 2026-11-08. `FR_034_TripMealReadTest`, `T042_TripMealsTest` đỏ rồi xanh; `BR_M08_01_PortionClosingIT`, `CountedResidentsTest`, kiến trúc xanh. Còn bỏ `FakeTripMealPort`, `FakeNutritionConfig` ở test US4 và bản giả `BUA` ở test 005 (chờ 011 T026 `sourceCancelled` theo người).
  - *(2026-11-11, A)* `ClosingTestBase` bỏ `FakeTripMealPort`, `FakeStayMealPort` (không lớp con nào dùng); `BR_M08_01`, `BR_M08_10`, `BR_M08_11`, `BR_M08_13`, `BR_M08_15`, `DBR_27`, `BR_M10_04` xanh với `TripMeals`, `StayMeals` thật. Còn bản giả `MEAL` ở `FakeM04Config` (test 005): chờ `TaskSourceApi.sourceCancelled` nhận người cao tuổi (011 T026, hỏi chủ dự án).
- [x] T043 *(2026-11-13: chủ dự án (A) nhận tự nghiệm thu kịch bản quickstart và đo SC bằng demo tay, đánh dấu theo yêu cầu của A; phần test tự động theo `clean verify`, `npm test`, `flutter test` ngày 2026-11-12 trên `impl/acceptance-2026-11-12`, ghi ở docs/progress.md)* *(2026-11-07, một phần: test cuối phase và `flutter test test/staff` xanh; chưa đo SC-001, SC-002, SC-006 đúng quy mô, xem plan "Phase 12")* Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC-001, SC-002 theo quickstart mục 5
  - *(2026-11-09)* Còn đo SC-001 (7 ngày × 60 người), SC-002 (≤ 5 phút), SC-006 (300 người ≤ 1 phút) đúng quy mô và quickstart mục 3; chạy khi chốt PR cùng `clean verify`.
- [x] T044 Rà truy vết: BR-M08-01 → 16, DBR-15 (phần 011), DBR-25, DBR-27 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-057, FR-063, FR-064, FR-065 — mỗi FR có task hoặc test tương ứng (constitution XV)
- [x] T046 *(bổ sung 2026-10-29, research R-15 bản 2 của 000)* Khi viết migration và entity của feature, áp data-model mục 0: không tạo các bảng đã gộp, dùng bảng gộp, cột `JSON` và bảng chung (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`); test bảo vệ nhóm dữ liệu viết cho bảng gộp *(2026-10-30, audit cơ sở dữ liệu F-04, PR #63)* Mọi bảng tạo hoặc gộp khai nhóm dữ liệu: entity bằng `@DataGroup`, bảng JDBC bằng `COMMENT = 'DataGroup=CATALOG|BUSINESS|RECORD|MULTIPLE|TECHNICAL'` (backend/README.md mục 4); bảng `RECORD` có `trg_<bảng>_no_update`, `trg_<bảng>_no_delete`; bảng chứa nhóm 2 và nhóm 3 dùng `@DataGroup(MULTIPLE)`, cột `locked_at`, trigger `trg_<bảng>_row_lock`; dòng bảng chung đặt `entity_type` trùng `RetentionProvider.dataType()` của bản ghi cha; job dùng `SchedulerRuns` (module-interfaces 000 mục 1.8). `RecordGroupTriggerIT` kiểm.

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
