# Q-447: Tinh gọn lập và duyệt kế hoạch chăm sóc — kế hoạch triển khai

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Điều dưỡng lập hoặc sửa kế hoạch rồi gửi Bác sĩ duyệt, không nhập ngày hiệu lực. Người chưa có kế hoạch thì kế hoạch có hiệu lực ngay khi duyệt; đã có thì bản sửa có hiệu lực từ ngày mai. Giao diện không hiện phiên bản.

**Architecture:** Bảng `care_plan` giữ nguyên theo phiên bản. Thay đổi gồm:
- `CarePlanTransitions`: bỏ tham số ngày, thêm RETRACT, bỏ WITHDRAW.
- `CarePlanService.approve`: chọn EFFECTIVE (hiệu lực hôm nay) hoặc PENDING_EFFECT (hiệu lực ngày mai), đồng thời hủy bản PENDING_EFFECT cũ.
- Một migration nới CHECK cho phép hiệu lực cùng ngày duyệt.

Web dựng lại màn theo dạng "kế hoạch hiện hành + bản sửa". Mobile chỉ đổi nhãn.

**Tech Stack:** Spring Boot 4.1 / Java 25 / MySQL 8.0 (Flyway); React 19 + Vitest; Flutter.

**Spec:** `specs/005-daily-care-tasks/spec.md` (mục Cập nhật 2026-11-25, US1, FR-003, FR-004, FR-007, FR-008); thiết kế `specs/005-daily-care-tasks/design-care-plan-streamline.md`; quyết định Q-447 ở `docs/business/nghiep-vu.md`.

## Global Constraints

- Migration chỉ dùng cú pháp có ở cả MySQL 8.0 và 8.4 (research R-01). `out-of-order` đã bật.
- Đổi hợp đồng thì sửa `contracts/api-005.yaml` ở kho Elder_Care trước. Kho web chạy `npm run gen:api` sau, không sửa tay `src/api/`.
- Không đổi enum `status` của `care_plan`. Không đổi phần lập công việc ca, `effectiveOn`, `CarePlanActivation.run`.
- Chữ trên giao diện:
  - Nhãn trạng thái: `Đang soạn`, `Bản sửa đang soạn`, `Chờ Bác sĩ duyệt`, `Đã duyệt, áp dụng từ dd/mm`.
  - Nút: `Lập kế hoạch`, `Sửa kế hoạch`, `Lưu nháp`, `Gửi Bác sĩ duyệt`, `Rút lại`, `Bỏ bản sửa`, `Duyệt`, `Trả lại`.
  - Ô nhập: `Lý do sửa`.
- Commit theo đợt, mỗi kho một commit riêng. Message không ghi attribution. Không đổi nhánh: Elder_Care, Elder_Care_Frontend ở `fix/daily-care-and-meal-menu`, Elder_Care_Mobile ở `fix/daily-care-v2`.
- Kho mobile đang có 2 file staged sẵn không thuộc việc này (`care_result_form.dart`, `care_checklist_screen_test.dart`). Không đưa vào commit Q-447.

## Review Focus

1. **Dữ liệu cũ có bản PENDING_EFFECT nhưng không có EFFECTIVE**, ví dụ bản duyệt theo luật cũ có ngày xa. Khi một bản mới được duyệt, bản mới phải EFFECTIVE ngay và bản PENDING_EFFECT cũ chuyển CANCELLED. Nếu không, lịch chạy sau đó sẽ đè lên. Test `Q447_approveCancelsStalePendingEffectEvenWithoutEffective` ở Task 2.
2. **Duyệt hai lần trong cùng một ngày.** Lần đầu: bản đầu tiên EFFECTIVE từ hôm nay. Lần hai: bản sửa PENDING_EFFECT từ ngày mai. Khi lịch chạy, bản cũ chỉ kết thúc ở hôm nay, và `chk_care_plan_period` (`effective_to >= effective_from`) vẫn phải đúng. Test `Q447_secondApprovalSameDayStartsTomorrowAndActivationKeepsPeriodValid` ở Task 2.
3. **Người khác người lập bấm Rút lại** phải nhận 403, không được kéo bản của người khác về Nháp. Test `Q447_retractOnlyByCreator` ở Task 2.
4. **Bác sĩ tự lập rồi tự duyệt kế hoạch đầu tiên.** Vẫn bắt buộc lý do và `self_approved = true` khi hiệu lực ngay. Thêm assert vào `Q447_firstApprovalTakesEffectToday` ở Task 2.
5. **Bản đang soạn khi đã có bản "áp dụng từ ngày mai".** Màn hình phải hiện cả hai trạng thái và không mở được bản soạn thứ hai. Test `Q447_dangCoBanApDungNgayMai_hienNhanVaNutSuaChepBanDo` ở Task 3.

---

### Task 1: Bảng chuyển trạng thái và migration

**Files:**
- Modify: `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CarePlanTransitions.java`
- Create: `backend/src/main/resources/db/migration/V005_103__q447_care_plan_same_day.sql`
- Test: `backend/src/test/java/com/eldercare/backend/care/FR_003_CarePlanTransitionsTest.java`

**Interfaces:**
- Produces:
  - `CarePlanTransitions.next(CarePlanStatus status, String command): CarePlanStatus`. Bảng lệnh:
    - SUBMIT: DRAFT → PENDING_APPROVAL
    - RETRACT: PENDING_APPROVAL → DRAFT
    - APPROVE: PENDING_APPROVAL → PENDING_EFFECT
    - RETURN: PENDING_APPROVAL → DRAFT
    - CANCEL: DRAFT, PENDING_APPROVAL → CANCELLED
    - Không còn WITHDRAW. Cặp sai trả `409 REVISION_LOCKED`.
  - `CarePlanTransitions.approvedStatus(boolean hasEffective): CarePlanStatus`: chưa có EFFECTIVE thì trả `EFFECTIVE`, có thì trả `PENDING_EFFECT`.
  - `CarePlanTransitions.effectiveFromOnApproval(boolean hasEffective, LocalDate today): LocalDate`: chưa có thì `today`, có thì `today.plusDays(1)`.
  - Cột `end_reason` có thêm giá trị `'SUPERSEDED'`.

- [ ] **Step 1: Sửa test cho khớp bảng mới**
  - `FR_003_allCommandStatePairsFollowTheApprovedTable`: map như trên, có RETRACT, không có WITHDRAW. Gọi `next(status, command)` với 2 tham số.
  - Thay `FR_004_approvalCannotChangeOrAcceptAnExpiredProposedDate` bằng `Q447_approvalDateFollowsWhetherAnEffectivePlanExists`. Test này assert:
    - `approvedStatus(false) == EFFECTIVE`, `approvedStatus(true) == PENDING_EFFECT`;
    - `effectiveFromOnApproval(false, 2026-11-25) == 2026-11-25`, `effectiveFromOnApproval(true, 2026-11-25) == 2026-11-26`.
  - `FR_006_activationRechecksTheEffectiveDateAfterLocking` giữ nguyên.
- [ ] **Step 2: Chạy test, phải đỏ**
  `.\mvnw.cmd test "-Dtest=FR_003_CarePlanTransitionsTest"`. Kỳ vọng biên dịch lỗi hoặc FAIL vì chưa có `approvedStatus`.
- [ ] **Step 3: Cài đặt** `next`, `approvedStatus`, `effectiveFromOnApproval` theo đúng chữ ký ở Interfaces. Bỏ hẳn kiểm `INVALID_EFFECTIVE_DATE`.
- [ ] **Step 4: Viết migration `V005_103`**
  - Mở đầu bằng chú thích Q-447, ngày 2026-11-25.
  - `ALTER TABLE care_plan DROP CHECK chk_care_plan_approval;`
  - `ALTER TABLE care_plan ADD CONSTRAINT chk_care_plan_approval CHECK(approved_at IS NULL OR (effective_from IS NOT NULL AND effective_from>=DATE(approved_at)));`
  - `ALTER TABLE care_plan MODIFY end_reason ENUM('NEW_REVISION','FINAL_STATUS','RETURNED','CANCELLED','WITHDRAWN','SUPERSEDED') NULL;`
  - Giữ `'WITHDRAWN'` cho dữ liệu cũ.
- [ ] **Step 5: Chạy lại test, phải xanh**
  Cùng lệnh ở Step 2. Kỳ vọng PASS. Task 2 sẽ còn lỗi biên dịch ở `CarePlanService`; sửa ở đó.
- [ ] **Step 6: Gộp commit chung với Task 2**, vì service chưa biên dịch được.

### Task 2: Lệnh của kế hoạch (service, controller, hợp đồng)

**Files:**
- Modify: `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanService.java` (`create`, `submit`, `approve`, `changeLocked`; thêm `retract`; bỏ `withdraw`)
- Modify: `backend/src/main/java/com/eldercare/backend/care/internal/careplan/persistence/CarePlanStore.java`: thêm `supersede(long id)`
- Modify: `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanActivation.java`: tách phần báo thành `announce(...)`
- Modify: `backend/src/main/java/com/eldercare/backend/care/web/CarePlanController.java`
- Modify: `backend/src/main/java/com/eldercare/backend/care/web/request/CarePlanSubmit.java`
- Delete: `backend/src/main/java/com/eldercare/backend/care/api/events/CarePlanApprovalWithdrawn.java` (chỉ `CarePlanService` dùng)
- Modify: `specs/005-daily-care-tasks/contracts/api-005.yaml` (đoạn `/care-plans/*`)
- Test: `backend/src/test/java/com/eldercare/backend/care/FR_003_CarePlanLifecycleIT.java`, `FR_003_CarePlanContractTest.java`

**Interfaces:**
- Consumes: các hàm ở Task 1.
- Produces:
  - `CarePlanSubmit(Reason changeReason)`: bỏ `effectiveFrom`.
  - `POST /api/v1/care-plans/{id}/retract`: header `Idempotency-Key`, không body. Trả 200 khi thành công. Sai trạng thái trả 409 `REVISION_LOCKED`. Không phải người lập trả 403.
  - `allowedCommands` của bản PENDING_APPROVAL thêm `retract`, chỉ cho người lập. Bỏ `withdrawApproval`.
  - Bỏ `/care-plans/{id}/withdraw-approval`.
  - `CarePlanStore.supersede(long id)`: chỉ khi `status='PENDING_EFFECT'`, đặt `status='CANCELLED'`, `end_reason='SUPERSEDED'`, `version=version+1`.
  - `CarePlanActivation.announce(CarePlan plan, Optional<CarePlan> previous, LocalDate date, Instant now)`: phát `CarePlanChanged` với cùng nhóm người nhận như `run`. `run` gọi lại hàm này.

Hành vi:
- **`create`**:
  - Chỉ chặn `OPEN_REVISION_EXISTS` khi có bản DRAFT hoặc PENDING_APPROVAL.
  - Nguồn chép: bản PENDING_EFFECT nếu có, không thì EFFECTIVE. Luôn chép, bỏ qua `copyCurrent`. Trường này giữ trong hợp đồng để không phá client cũ.
  - `previous_plan_id` = id của nguồn chép.
- **`submit(target, id, reason)`**: bỏ tham số ngày. Đặt `effective_from = NULL`. Mọi kiểm khác giữ nguyên.
- **`approve`**:
  - `hasEffective` = người cao tuổi có bản EFFECTIVE hay không.
  - Bản PENDING_EFFECT khác của người đó, nếu có, gọi `supersede` rồi ghi audit `CARE_PLAN_SUPERSEDED`.
  - Chuyển bản đang duyệt sang `approvedStatus(hasEffective)` với ngày `effectiveFromOnApproval(hasEffective, today)`.
  - Nếu thành EFFECTIVE thì gọi `activation.announce(plan, Optional.empty(), today, now)`.
  - Tự duyệt giữ như cũ.
- **`retract`**: `@RequiresPermission(CARE_PLAN, UPDATE)`. Người gọi phải là `createdBy`, nếu không thì `PermissionDeniedException("Chỉ người lập được rút lại")`. Dùng `next(…, "RETRACT")`. Đặt `submitted_at = NULL`, `end_reason = NULL`. Audit `CARE_PLAN_RETRACT`, không bắt buộc lý do.

- [ ] **Step 1: Viết lại IT `FR_003_CarePlanLifecycleIT`**
  - Hàm phụ `submit(id)`: gửi body `{}` thay cho ngày.
  - Thay `FR_003_submitApproveAndWithdrawKeepItemsLockedAndAuditAtomic` bằng các test dưới:
    - `Q447_firstApprovalTakesEffectToday`: bác sĩ duyệt. Assert `status = EFFECTIVE`, `effective_from = time.today()`. `GET /residents/{id}/care-plans` có `noEffectivePlan = false`. Có đúng 1 `CarePlanChanged`, dùng `@RecordApplicationEvents` như các IT khác của care. Thêm nhánh tự duyệt: Điều dưỡng được gán quyền duyệt; duyệt không lý do thì 422 `REASON_REQUIRED`; duyệt có lý do thì `self_approved = 1`.
    - `Q447_revisionApprovedStartsTomorrowAndKeepsCurrentPlan`: sau khi có bản EFFECTIVE, lập bản sửa. Assert nội dung bản sửa đã chép mục. Gửi kèm `changeReason`, bác sĩ duyệt. Assert bản sửa `PENDING_EFFECT` với `effective_from = today+1`, bản cũ vẫn `EFFECTIVE`.
    - `Q447_secondApprovalSameDayStartsTomorrowAndActivationKeepsPeriodValid`: tiếp test trên. Gọi `CarePlanActivation.run(today+1, now)`. Assert bản cũ `EXPIRED` với `effective_to = today`, bản mới `EFFECTIVE`.
    - `Q447_editWhilePendingEffectSupersedesIt`: khi đang có bản PENDING_EFFECT, lập bản sửa mới. Assert được phép, nội dung chép từ bản PENDING_EFFECT. Duyệt xong thì bản cũ `CANCELLED` với `end_reason = 'SUPERSEDED'`.
    - `Q447_approveCancelsStalePendingEffectEvenWithoutEffective`: dùng `jdbc` chèn một bản `PENDING_EFFECT` có `effective_from = today+30`, không có bản EFFECTIVE. Lập, gửi, duyệt một bản mới. Assert bản mới `EFFECTIVE` hôm nay, bản chèn `CANCELLED`.
    - `Q447_retractOnlyByCreator`: bác sĩ gọi `/retract` thì 403. Điều dưỡng lập gọi thì 200 và trạng thái `DRAFT`. Sau đó `PUT items` với ETag mới thì 200.
    - `Q447_withdrawEndpointIsGone`: `POST /withdraw-approval` trả 404.
  - Giữ `FR_003_createPreventsSecondOpenRevisionAndEmptySubmission`, chỉ đổi body submit.
- [ ] **Step 2: Sửa `FR_003_CarePlanContractTest`**
  - Có operation `retractCarePlan`, không còn `withdrawCarePlanApproval`.
  - Body của `submitCarePlan` không có `required: [effectiveFrom]`.
- [ ] **Step 3: Chạy các test, phải đỏ**
  `.\mvnw.cmd verify "-Dit.test=FR_003_CarePlanLifecycleIT" "-Dtest=FR_003_CarePlanContractTest" "-Dsurefire.failIfNoSpecifiedTests=false"`. Kỳ vọng FAIL.
- [ ] **Step 4: Sửa `api-005.yaml`**
  - Đoạn `/care-plans/{id}/submit`: summary "Gửi Bác sĩ duyệt"; requestBody chỉ còn `changeReason`, không `required`.
  - Đoạn `approve`: description theo 4.3 của thiết kế.
  - Thêm `/care-plans/{id}/retract`, operationId `retractCarePlan`, `x-fr: [FR-003]`.
  - Xóa `/care-plans/{id}/withdraw-approval`.
  - `createCarePlan`: description "luôn chép bản Chờ hiệu lực hoặc Hiệu lực; chỉ chặn khi có Nháp/Chờ duyệt".
- [ ] **Step 5: Cài đặt service, store, activation, controller, request** theo Interfaces. Xóa `CarePlanApprovalWithdrawn`.
- [ ] **Step 6: Chạy lại, phải xanh**
  Cùng lệnh Step 3, cộng thêm `FR_003_CarePlanTransitionsTest`, `BR_17_CarePlanReviewIT`, `BR_18_ShiftTaskPlanningIT`, `ApiContractIT`, `ModuleBoundaryTest`. Kỳ vọng tất cả PASS.
- [ ] **Step 7: Định dạng mã rồi commit (kho Elder_Care)**
  `.\mvnw.cmd spotless:apply`, rồi commit với message `feat(005): Q-447 gửi Bác sĩ duyệt không nhập ngày, hiệu lực ngay hoặc từ ngày mai, Rút lại`.

### Task 3: Màn Kế hoạch chăm sóc trên web

**Files:**
- Regenerate: `Elder_Care_Frontend/src/api/` (`npm run gen:api`)
- Modify: `src/features/care-tasks/CarePlanPage.jsx`
- Modify: `src/features/care-tasks/CarePlanDetail.jsx`
- Modify: `src/features/care-tasks/carePlanLabels.js`
- Test: `CarePlanPage.test.jsx`, `CarePlanDetail.test.jsx`, `carePlanClient.test.js`

**Interfaces:**
- Consumes:
  - các operation sinh ra: `postCarePlansByIdRetract`, `postCarePlansByIdSubmit` (body `{ changeReason? }`);
  - `getResidentsByResidentIdCarePlans` (danh sách bản);
  - `getCarePlansCompare`.
- Produces:
  - `carePlanLabels.js` thêm hàm `planState(plans)`, trả `{ current, scheduled, open }`:
    - `current`: bản EFFECTIVE;
    - `scheduled`: bản PENDING_EFFECT;
    - `open`: bản DRAFT hoặc PENDING_APPROVAL;
    - thiếu bản nào thì giá trị đó là `null`.
  - `openLabel(open, hasCurrent)`, trả một trong các chữ ở Global Constraints.

Bố cục theo mục 4.5 của thiết kế, giữ mẫu màn hiện tại (DESIGN.md mục 0) và `DetailLayout`:
- Trang hiển thị kế hoạch hiện hành ở dạng chỉ xem.
- Có dải nhãn trạng thái cho `open` và `scheduled`.
- Nút `Lập kế hoạch` hiện khi không có `current` lẫn `scheduled`. Nút `Sửa kế hoạch` hiện khi đã có một trong hai và chưa có `open`.
- Chi tiết bản `open` đặt trong sheet:
  - DRAFT: form mục, form thỏa thuận, ô `Lý do sửa` (chỉ khi có `current` hoặc `scheduled`), các nút `Lưu nháp`, `Gửi Bác sĩ duyệt`, `Bỏ bản sửa`.
  - PENDING_APPROVAL: nút `Rút lại`, `Duyệt`, `Trả lại` theo `allowedCommands`, kèm khu "Thay đổi so với hiện hành" (gọi compare `from = current.id`, `to = open.id`, chỉ khi có `current`).
- Bỏ: danh sách lịch sử, thẻ so sánh, ô tick sao chép, ô ngày, nút Thu hồi duyệt, chữ "Phiên bản N".

Nút `Gửi Bác sĩ duyệt` khi bản còn DRAFT làm 3 việc nối tiếp:
1. `PUT items` với `If-Match` của bản.
2. `GET` lại bản để lấy ETag mới.
3. `POST submit`.
Bước nào lỗi thì dừng lại và hiện `ProblemAlert`.

- [ ] **Step 1: Chạy `npm run gen:api`**
  Kỳ vọng `src/api/api005.js` có `postCarePlansByIdRetract` và không còn `postCarePlansByIdWithdrawApproval`.
- [ ] **Step 2: Viết test, phải đỏ**
  - `CarePlanPage.test.jsx`:
    - `Q447_chuaCoKeHoach_hienNutLapKeHoachKhongCoPhienBan`: không có chữ "Phiên bản", có nút `Lập kế hoạch`.
    - `Q447_coKeHoachHienHanh_hienMucVaNutSuaKeHoach`.
    - `Q447_dangCoBanApDungNgayMai_hienNhanVaNutSuaChepBanDo`: hiện nhãn `Đã duyệt, áp dụng từ 26/11`; bấm `Sửa kế hoạch` gọi POST lập bản.
    - Giữ `FR_007_khongCoQuyenXemThiKhongHienDuLieuHayLenhLap` và `F5_…`. Xóa `FR_001_…SaoChep` và `FR_008_soSanh…`.
  - `CarePlanDetail.test.jsx`:
    - `Q447_guiBacSiDuyet_luuMucRoiGuiKhongCoNgay`: thứ tự gọi là PUT items → GET → POST submit, body submit không có `effectiveFrom`.
    - `Q447_choDuyet_nguoiLapThayRutLai`.
    - `Q447_choDuyet_bacSiThayThayDoiSoVoiHienHanh`.
    - Giữ `FR_007_nutLenh…`, `FR_004_412…`, `FR_003_huy…`, đổi chữ thành `Bỏ bản sửa`.
  - `carePlanClient.test.js`: danh sách operation có `retractCarePlan`, không có `withdrawCarePlanApproval`.
  - Chạy `npx vitest run src/features/care-tasks/`. Kỳ vọng FAIL.
- [ ] **Step 3: Cài đặt** `planState`, `openLabel` và hai màn theo bố cục trên.
- [ ] **Step 4: Chạy kiểm tra**
  `npx vitest run src/features/care-tasks/`, `npm run lint`, `npm run build`. Kỳ vọng tất cả xanh.
- [ ] **Step 5: Mở màn trên trình duyệt để xem**
  `npm run dev`, mở với backend đang chạy, hoặc bản mẫu nếu không có backend. Chụp màn: chưa có kế hoạch, đang soạn, chờ duyệt (vai Bác sĩ).
- [ ] **Step 6: Commit (kho Elder_Care_Frontend)** với message `feat(care-plan): Q-447 một kế hoạch, gửi Bác sĩ duyệt, bỏ phiên bản trên màn`.

### Task 4: Nhãn trên mobile

**Files:**
- Modify: `Elder_Care_Mobile/lib/staff/care_tasks/care_plans_screen.dart:120-121`
- Test: `Elder_Care_Mobile/test/staff/care_tasks/care_plans_screen_test.dart`

- [ ] **Step 1:** Trong `chamNguoiCaoTuoi_hienKeHoachDangHieuLuc`, assert có chữ `Hiệu lực từ` và không có chữ `Kế hoạch lần`. Chạy `flutter test test/staff/care_tasks/care_plans_screen_test.dart`. Kỳ vọng FAIL.
- [ ] **Step 2:** Đổi dòng 120–121 thành `'Hiệu lực từ ${formatDate(...)}'`. Khi `effectiveFrom` null thì ghi `'Đang hiệu lực'`.
- [ ] **Step 3:** Chạy `flutter test test/staff/care_tasks/care_plans_screen_test.dart` và `flutter analyze`. Kỳ vọng PASS.
- [ ] **Step 4: Commit (kho Elder_Care_Mobile)**
  Chỉ `git add` hai file của task này, không đưa 2 file đang staged sẵn. Message `feat(care-plan): Q-447 bỏ số phiên bản trên màn kế hoạch`.

### Task 5: Ghi tiến độ và kiểm tra cuối

**Files:**
- Modify: `Elder_Care/specs/005-daily-care-tasks/tasks.md` (thêm phase "Q-447" với T-mã mới, đánh `[x]` theo kết quả)
- Modify: `Elder_Care/docs/progress.md` (thêm một dòng cuối)

- [ ] **Step 1: Chạy nhóm IT của module care**
  `.\mvnw.cmd verify "-Dit.test=FR_003_CarePlanLifecycleIT,BR_17_CarePlanReviewIT,BR_18_ShiftTaskPlanningIT,FR_039_CareResultRulesIT,ApiContractIT" "-Dtest=FR_003_*,ModuleBoundaryTest,*ArchTest" "-Dsurefire.failIfNoSpecifiedTests=false"`. Kỳ vọng BUILD SUCCESS.
- [ ] **Step 2: Ghi `tasks.md` và `progress.md`**
  Ghi feature 005, Q-447, nhánh, các test đã chạy và kết quả. Ghi rõ "Chưa chạy clean verify".
- [ ] **Step 3: Commit (kho Elder_Care)** với message `docs(005): Q-447 tasks và tiến độ`.
