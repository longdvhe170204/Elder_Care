# Q-448: Hàng chờ lập và sửa kế hoạch chăm sóc — kế hoạch triển khai

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Điều dưỡng mở màn Kế hoạch chăm sóc là thấy ngay ai cần lập hoặc sửa kế hoạch và vì sao. Người mới vào ở khỏe mạnh, không có nhu cầu chăm sóc, ở trạng thái "Theo lịch chung" và không bị coi là thiếu sót.

**Architecture:**
- Bảng mới `care_plan_review` lưu yêu cầu xem xét (module care). `CarePlanReviewListener` đang phát thông báo ở mọi nguồn; nay ghi thêm một dòng ở cùng chỗ, trong cùng giao dịch.
- Nguồn mới: nghe `InitialAssessmentConfirmed`, tạo yêu cầu khi đánh giá đầu vào có ít nhất một nhu cầu và người cao tuổi chưa có kế hoạch EFFECTIVE hay PENDING_EFFECT.
- Đóng yêu cầu: trong `CarePlanService.approve` (Đã xử lý) và lệnh mới `POST /residents/{residentId}/care-plan-reviews/dismiss` (Không cần thay đổi). Nghe `ResidentStatusChanged` sang trạng thái kết thúc lưu trú để đóng tự động.
- Đọc: `GET /care-plan-reviews/queue` trả hàng chờ theo phạm vi xem. `GET /residents/{residentId}/care-plans` trả thêm `planState` và `openReviews` (FR-009).
- Web: `CarePlanPage` thay chữ "Chọn người cao tuổi để xem kế hoạch" bằng hàng chờ; `CarePlanDetail` có dải nguồn và nút "Không cần thay đổi". Menu có số đếm.
- Mobile: màn kế hoạch đọc thêm `planState` để hiện nhãn. Hàng chờ trên mobile để đợt sau.

**Tech Stack:** Spring Boot 4.1 / Java 25 / MySQL 8.0 (Flyway); React 19 + Vitest; Flutter.

**Spec:** `specs/005-daily-care-tasks/spec.md` (mục Cập nhật 2026-11-26, US8, FR-009, FR-011, FR-058, FR-059); quyết định Q-448 ở `docs/business/nghiep-vu.md` 24.2.

## Global Constraints

- Migration `V005_104__q448_care_plan_review.sql`, chỉ cú pháp có ở cả MySQL 8.0 và 8.4 (research R-01).
- Đổi hợp đồng thì sửa `contracts/api-005.yaml` trước. Kho web chạy `npm run gen:api`, không sửa tay `src/api/`.
- Không đổi luồng Q-447 (lập, gửi, rút lại, duyệt, trả lại, bỏ). Không đổi phần lập công việc ca.
- Giữ nguyên thông báo hiện có của BR-M04-20 và dấu chống báo lặp (`ReminderMarkers`, `M04.CARE_PLAN_REVIEW`). Yêu cầu chỉ được ghi khi dấu được đặt lần đầu, để báo một lần thì cũng chỉ một dòng.
- Chữ trên giao diện:
  - Trạng thái kế hoạch: `Cần xem xét kế hoạch`, `Cần lập kế hoạch`, `Có kế hoạch`, `Chờ đánh giá`, `Theo lịch chung`.
  - Nguồn: `Đánh giá đầu vào`, `Bác sĩ đánh giá lại`, `Sự cố ngã`, `Phụ lục đổi mức`, `Chăm sóc giảm nhẹ`, `Bác sĩ trả lại`, `Bản sửa đang soạn`.
  - Tiêu đề: `Cần lập hoặc sửa kế hoạch`, `Đang chờ Bác sĩ duyệt`. Nút: `Lập kế hoạch`, `Sửa kế hoạch`, `Sửa tiếp`, `Mở`, `Không cần thay đổi`. Ô nhập: `Lý do không cần thay đổi`.
- Commit theo đợt, mỗi kho một commit riêng, message không ghi attribution. Không đổi nhánh: Elder_Care, Elder_Care_Frontend ở `fix/daily-care-and-meal-menu`, Elder_Care_Mobile ở `fix/daily-care-v2`.
- Kho Elder_Care đang có `T022_WellbeingReadIT.java` sửa dở không thuộc việc này; kho mobile có 2 file staged sẵn. Không đưa vào commit Q-448.

## Review Focus

1. **Dấu chống lặp và yêu cầu phải đi cùng nhau.** Gọi lặp cùng sự cố ngã không tạo dòng thứ hai (khóa duy nhất `resident_id, source, reference`). Test `Q448_repeatedFallCreatesOneOpenReview`.
2. **Bản lập trước thời điểm phát sinh không được đóng yêu cầu.** Bản Nháp đã mở từ hôm qua, hôm nay ngã, Bác sĩ duyệt bản đó: yêu cầu ngã vẫn mở (dùng `care_plan.created_at` > `raised_at`). Test `Q448_approvingDraftCreatedBeforeIncidentKeepsReviewOpen`.
3. **Bản bị trả lại hoặc bỏ không đóng yêu cầu.** Test `Q448_returnedOrCancelledPlanKeepsReviewOpen`.
4. **Đánh giá đầu vào không nhu cầu không tạo yêu cầu**, và người đã có kế hoạch thì đánh giá đầu vào (sửa đính chính) không tạo. Test `Q448_initialAssessmentWithoutNeedsStaysOnGeneralSchedule`.
5. **Q-423 vẫn đúng:** phụ lục đổi mức không tạo yêu cầu thứ hai khi đã có bản lập sau lần đánh giá lại; khi yêu cầu từ đánh giá lại còn mở thì cũng không tạo thêm (đã có dòng cho người đó).
6. **Phạm vi:** Điều dưỡng tầng 2 không thấy người tầng 3 trong hàng chờ; lệnh "Không cần thay đổi" ngoài phạm vi trả 403.

---

### Task 1: Bảng và lưu yêu cầu khi có nguồn phát sinh (backend)

**Files:**
- Create: `backend/src/main/resources/db/migration/V005_104__q448_care_plan_review.sql`
- Create: `backend/src/main/java/com/eldercare/backend/care/internal/careplan/persistence/CarePlanReviewStore.java`
- Modify: `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanReviewListener.java`
- Test: `backend/src/test/java/com/eldercare/backend/care/BR_17_CarePlanReviewIT.java`

- [ ] Viết test đỏ trong `BR_17_CarePlanReviewIT`:
  - `Q448_reassessmentOpensReview`: đánh giá lại đổi mức → 1 dòng `OPEN`, nguồn `M01.ASSESSMENT`, tham chiếu là mã đánh giá. Sửa ca cũ đang assert "không có bản ghi nghiệp vụ" cho đúng spec mới.
  - `Q448_repeatedFallCreatesOneOpenReview`, `Q448_palliativeChoiceOpensReview`.
  - `Q448_addendumAfterRevisedPlanCreatesNoReview` (Q-423).
- [ ] Migration:
  ```sql
  CREATE TABLE care_plan_review (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    source VARCHAR(40) NOT NULL,          -- M01.ASSESSMENT, M01.INITIAL_ASSESSMENT, M02.ADDENDUM, M05.INCIDENT, M05.PALLIATIVE_CARE_CHOICE
    source_id BIGINT NOT NULL,
    reference VARCHAR(80) NOT NULL,
    raised_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,          -- OPEN, RESOLVED, NO_CHANGE
    closed_at DATETIME(6) NULL,
    closed_by BIGINT NULL,                -- NULL khi hệ thống đóng
    close_reason VARCHAR(500) NULL,
    resolved_plan_id BIGINT NULL,
    CONSTRAINT uq_care_plan_review_source UNIQUE (resident_id, source, reference),
    CONSTRAINT fk_care_plan_review_plan FOREIGN KEY (resolved_plan_id) REFERENCES care_plan (id),
    CONSTRAINT chk_care_plan_review_status CHECK (status IN ('OPEN','RESOLVED','NO_CHANGE')),
    INDEX ix_care_plan_review_open (status, resident_id)
  );
  ```
  `reference` của chăm sóc giảm nhẹ dùng băm của `eventKey` như dấu hiện có (cột 80 ký tự).
- [ ] `CarePlanReviewStore`: `open(residentId, source, sourceId, reference, raisedAt)` dùng `INSERT IGNORE`; `openFor(residentId)`, `openForResidents(ids)`, `resolve(residentId, planId, planCreatedAt, now)` (đóng các dòng `OPEN` có `raised_at < planCreatedAt`), `dismissAll(residentId, accountId, reason, now)`, `closeBySystem(residentId, reason, now)`.
- [ ] Trong `CarePlanReviewListener.review(...)` gọi `store.open(...)` trước khi phát thông báo. Các đường `onAssessment`, `onAddendum` chạy trong giao dịch của nguồn; kiểm `@EventListener` hiện có đang ở trong giao dịch (nếu không thì đổi sang `@TransactionalEventListener(phase = BEFORE_COMMIT)` để dòng và thông báo cùng thành công).
- [ ] Chạy test xanh. Nhật ký vào `backend/logs/005/20261126-q448-t1-{red,green}.log`.

### Task 2: Nguồn mới từ đánh giá đầu vào (backend)

**Files:**
- Modify: `CarePlanReviewListener.java`
- Test: `BR_17_CarePlanReviewIT.java`

- [ ] Test đỏ `Q448_initialAssessmentWithNeedsOpensReview`, `Q448_initialAssessmentWithoutNeedsStaysOnGeneralSchedule`.
- [ ] Thêm `@EventListener onInitialAssessment(InitialAssessmentConfirmed e)`: lấy `assessments.latestConfirmed(residentId)`; `careNeeds` rỗng thì dừng; đã có bản EFFECTIVE hoặc PENDING_EFFECT thì dừng; còn lại gọi `review(residentId, "M01.INITIAL_ASSESSMENT", assessmentId)` (vừa báo vừa lưu). Dùng dấu `M04.CARE_PLAN_REVIEW` như các nguồn khác.
- [ ] Nếu `latestConfirmed` chưa thấy lần vừa xác nhận trong cùng giao dịch thì thêm `needIds` vào sự kiện `InitialAssessmentConfirmed` của 001 (cập nhật `AllocationSuitabilityListener` của 003 nếu chữ ký đổi).
- [ ] Chạy test xanh.

### Task 3: Đóng yêu cầu (backend)

**Files:**
- Modify: `care/internal/careplan/service/CarePlanService.java` (`approve`)
- Create: `care/internal/careplan/service/CarePlanReviewCommands.java`
- Modify: `care/web/CarePlanController.java`, `care/web/request/` (thêm `CarePlanReviewDismiss`)
- Modify: `specs/005-daily-care-tasks/contracts/api-005.yaml`
- Test: `BR_17_CarePlanReviewIT.java`, `FR_003_CarePlanLifecycleIT.java`

- [ ] Test đỏ: `Q448_approveResolvesReviewsRaisedBeforePlanCreated`, `Q448_approvingDraftCreatedBeforeIncidentKeepsReviewOpen`, `Q448_returnedOrCancelledPlanKeepsReviewOpen`, `Q448_dismissRequiresReason`, `Q448_dismissOutsideScopeForbidden`, `Q448_residentLeavesClosesOpenReviews`.
- [ ] `approve`: sau khi đổi trạng thái, gọi `reviews.resolve(residentId, planId, plan.createdAt(), now)`.
- [ ] Lệnh `POST /residents/{residentId}/care-plan-reviews/dismiss` body `{ reason }` (bắt buộc, ≤ 500), cần `CARE_PLAN.CREATE` hoặc `CARE_PLAN.APPROVE` trong phạm vi; có `Idempotency-Key` như các lệnh khác; ghi nhật ký `CARE_PLAN_REVIEW_DISMISSED`. Không có yêu cầu mở thì 409 `NO_OPEN_REVIEW`.
- [ ] Nghe `ResidentStatusChanged` sang trạng thái kết thúc lưu trú: `closeBySystem(residentId, "Kết thúc lưu trú")`.
- [ ] Cập nhật `api-005.yaml`, chạy test hợp đồng `FR_003_CarePlanContractTest`.

### Task 4: Đọc hàng chờ và trạng thái kế hoạch (backend)

**Files:**
- Create: `care/internal/careplan/service/CarePlanQueue.java`
- Modify: `care/web/CarePlanController.java`, `care/web/response/` (thêm `CarePlanQueueResponse`, `PlanState`)
- Modify: `api-005.yaml`
- Test: `BR_17_CarePlanReviewIT.java`

- [ ] `GET /care-plan-reviews/queue?floorAreaId=&source=` cần `CARE_PLAN.VIEW`; phạm vi theo tầng như `GET /residents`. Mỗi người một dòng: `resident` (tên, mã hồ sơ, phòng, giường từ `BedAllocationApi`), `planState`, `reasons[]` (nguồn, mã tham chiếu, `raisedAt`), `waitingSince`, `draft` (id, trạng thái, lý do trả lại nếu có), `allowedCommands`. Nhóm `pendingApproval[]` riêng. Sắp `waitingSince` tăng dần.
- [ ] `planState` theo thứ tự FR-009. "Chờ đánh giá" dùng `assessments.latestConfirmed(...)` của loại đầu vào; gọi gộp cho danh sách để tránh N+1 (thêm hàm lô vào `AssessmentApi` nếu chưa có).
- [ ] `GET /residents/{residentId}/care-plans` trả thêm `planState`, `openReviews[]`. Giữ `noEffectivePlan` cho mobile cũ.
- [ ] `GET /care-plan-reviews/count` trả số dòng của hàng chờ cho menu.
- [ ] Test `Q448_queueGroupsReasonsPerResidentAndRespectsFloorScope`, `Q448_planStateOrder`.
- [ ] Chạy `./mvnw verify` cho module care. Nhật ký vào `backend/logs/005/`.

### Task 5: Web — hàng chờ trên màn Kế hoạch chăm sóc

**Files (kho Elder_Care_Frontend):**
- Regenerate: `src/api/api005.js` (`npm run gen:api`)
- Create: `src/features/care-tasks/CarePlanQueue.jsx`, `CarePlanQueue.test.jsx`
- Modify: `src/features/care-tasks/CarePlanPage.jsx`, `CarePlanPage.test.jsx`, `carePlanLabels.js`
- Modify: `src/features/care-tasks/CarePlanDetail.jsx`, `CarePlanDetail.test.jsx`
- Modify: `src/common/layout/navigation.js` (số đếm)

- [ ] Test đỏ `CarePlanQueue.test.jsx`: hiện tiêu đề và số; mỗi dòng có tên, phòng, nhãn nguồn, "x ngày"; nút theo trạng thái (`Lập kế hoạch` / `Sửa kế hoạch` / `Sửa tiếp` / `Mở`); lọc tầng, lọc nguồn; nhóm `Đang chờ Bác sĩ duyệt` thu gọn; hàng chờ rỗng thì hiện "Không có ai cần lập hoặc sửa kế hoạch".
- [ ] `CarePlanPage`: khi chưa chọn người thì hiện `CarePlanQueue` dưới ô tìm; bấm dòng thì chọn người đó (giữ `?residentId=` trên URL để thông báo dẫn thẳng tới). Có nút quay về hàng chờ khi đang xem một người.
- [ ] `CarePlanDetail`: nhãn `planState` cạnh tên; dải "Cần xem xét vì: Sự cố ngã 25/11 14:20 · Xem sự cố" cho mỗi `openReviews`; nút `Không cần thay đổi` mở hộp thoại bắt buộc lý do; ô `Lý do sửa` điền sẵn từ nguồn (người dùng sửa được).
- [ ] Liên kết nguồn: đánh giá → trang hồ sơ tab đánh giá; sự cố → trang chi tiết sự cố 007; phụ lục → trang hợp đồng 004. Kiểm đường dẫn có sẵn trong `App.jsx`, chưa có thì chỉ hiện chữ.
- [ ] Menu "Kế hoạch chăm sóc" hiện số từ `count`, làm mới khi đóng hoặc duyệt.
- [ ] Theo DESIGN.md của kho web (badge, bảng, khoảng cách). Kiểm trên trình duyệt: vai Điều dưỡng (hàng chờ có 3 nguồn khác nhau), vai Bác sĩ, màn hẹp 375px, chế độ tối. Chụp màn đưa chủ dự án.
- [ ] `npm test`, `npm run lint`.

### Task 6: Mobile — nhãn trạng thái kế hoạch

**Files (kho Elder_Care_Mobile):**
- Modify: `lib/staff/care_tasks/care_plans_screen.dart`, `test/staff/care_tasks/care_plans_screen_test.dart`

- [ ] Đọc `planState` thay cho `noEffectivePlan`, hiện nhãn tiếng Việt như web; người "Theo lịch chung" không hiện cảnh báo.
- [ ] `flutter test` cho file này.

### Task 7: Dữ liệu mẫu, tài liệu, tiến độ

- [ ] Cập nhật `sql/demo-full-data.sql` (ngoài kho) để có: 1 người mới vào ở chưa có kế hoạch có nhu cầu, 1 người theo lịch chung, 1 người vừa ngã, 1 bản bị trả lại.
- [ ] Đánh dấu tasks Q-448 trong `specs/005-daily-care-tasks/tasks.md`, ghi `docs/progress.md`.
- [ ] Commit từng kho: Elder_Care (docs + backend), Elder_Care_Frontend, Elder_Care_Mobile.
