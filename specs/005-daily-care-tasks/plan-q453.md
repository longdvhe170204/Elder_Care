# Q-453: Mở lại Q-439, Trưởng tầng lập công việc theo tháng — kế hoạch triển khai

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trưởng tầng lập công việc mỗi tháng một lần. Hệ thống đọc kế hoạch chăm sóc Hiệu lực của từng ngày (tần suất, mốc giờ, ngày trong tuần) và tạo bản Nháp công việc cho cả tháng. Trưởng tầng xem, sửa, rồi Công bố.

**Căn cứ:** Q-453 và Q-439 (`docs/business/nghiep-vu.md` 24.2); BR-M04-01, 03, 25; spec 005 FR-054, FR-006, FR-017, FR-018, FR-024 và mục Cập nhật 2026-11-26 (Q-453).

**Architecture:**
- Không tạo bảng mới. `care_task.status` thêm giá trị `DRAFT` bằng migration `ALTER TABLE ... MODIFY`. Tham số CFG-M04-14 thêm một dòng vào bảng `parameter` có sẵn.
- Lệnh mới ở backend, module care: "Chép từ kế hoạch" theo tháng (hoặc theo một người từ một ngày) và "Công bố công việc tháng".
- Tầng lấy theo phân công Trưởng tầng hiện hành của người đăng nhập. Không có, hoặc có từ hai tầng trở lên, thì trả 409 với mã rõ ràng, không đoán.
- Mỗi mốc của mục kế hoạch được gắn vào ca đã công bố của tầng chứa thời điểm đó.
  - Người thực hiện: người được phân công người cao tuổi trong ca đó, vai trò khớp mục. Không có ai thì là việc chung của tầng.
  - Khi phân công công bố hoặc đổi, `SharedCareTasks.reassignShift` hoặc `reassignResident` (FR-024) gán lại, kể cả việc Nháp.
- Web: màn Lập công việc ca đổi thành chọn tháng. Bỏ ô ngày, ô ca, ô tầng. Có bản xem trước, sửa và Công bố. Lập lẻ một việc (Q-452) giữ cho việc phát sinh và bổ sung.

**Tech Stack:** Spring Boot 4.1 / Java 25 / MySQL 8.0 (Flyway); React 19 + Vitest.

## Global Constraints

- Không tạo bảng mới (chủ dự án 2026-11-26). Migration chỉ sửa enum và thêm dòng tham số, cú pháp dùng được ở cả MySQL 8.0 và 8.4.
- Đổi hợp đồng thì sửa `contracts/api-005.yaml` trước, rồi kho web chạy `npm run gen:api`.
- Viết test trước, thấy đỏ rồi mới cài. IT chạy bằng JDK 25 (`E:\JAVA\JDK_GE\INS_25`), schema `eldercare_test`, tài khoản `DB_MIGRATION_*` trong `backend/.env`.
- Lệnh chỉ chạy khi Trưởng tầng bấm. Hệ thống không tự lập, không tự công bố (ngoại lệ có giới hạn của Q-306, Q-307 theo Q-439).
- File dùng CRLF: không dùng `sed -i`.
- Commit theo đợt, mỗi kho một commit, message không ghi tên Claude hay Co-Authored-By. Không đổi nhánh. Không đưa `T022_WellbeingReadIT.java` vào commit.

## Review Focus

1. **Việc Nháp không được lọt ra vận hành.** Không hiện trên checklist, lịch hằng ngày của người thân (FR-055), thống kê, báo cáo. Không bị nhắc, không quá hạn, không vào bàn giao.
2. **Không tạo trùng.** Chép lại cùng tháng, hoặc chép theo người sau khi đã chép theo tầng, không sinh dòng thứ hai (`duplicate_key`).
3. **Ca qua nửa đêm.** Mốc 05:00 ngày D thuộc ca 18:00 ngày D-1 đến 07:00 ngày D. Mốc ngày cuối tháng có thể rơi vào ca bắt đầu từ ngày cuối tháng trước.
4. **Kế hoạch đổi giữa tháng (BR-M04-03, FR-006).** Bản mới có hiệu lực từ ngày mai thì hủy các việc Nháp và Chưa đến hạn lập theo mục của bản cũ, tính từ ngày hiệu lực, rồi báo Trưởng tầng. Việc đã Đến hạn, Quá hạn hay đã làm thì giữ nguyên.
5. **Người bán trú (FR-018).** Chỉ lập vào ngày có lịch đến. Người tạm vắng, đi viện thì không lập cho những ngày đó.

---

### Task 1: Trạng thái Nháp của công việc

- [ ] Migration `V005_105__q453_care_task_draft.sql`: thêm `DRAFT` vào enum `care_task.status`. Kiểm tra `duplicate_key` vẫn chặn trùng giữa bản Nháp và bản đã công bố.
- [ ] `CareTaskClockProcessor`, nhắc quá hạn, checklist, lịch hằng ngày người thân, thống kê (`CareTaskStatistics`) bỏ qua `DRAFT`.
- [ ] IT: tạo việc Nháp rồi qua giờ: không chuyển Đến hạn, không nhắc, không hiện ở `GET /checklist`.

### Task 2: Lệnh "Chép từ kế hoạch" theo tháng

- [ ] `POST /care-tasks/month-drafts` với body `{ month: "2026-12" }` hoặc `{ residentId, fromDate }`. Quyền là quyền lập công việc ca của Trưởng tầng (Phụ lục 27, dòng 53).
- [ ] Với mỗi người cao tuổi của tầng và mỗi ngày trong tháng: lấy mục của kế hoạch Hiệu lực ngày đó, tính các mốc (dùng lại phần tính mốc của Q-452 ở `CareTaskQueries` hoặc `CareShiftSources`), tìm ca đã công bố chứa mốc, xác định người thực hiện như mục Architecture, rồi tạo việc `DRAFT` với căn cứ `CARE_PLAN_ITEM`. Mốc đã có thì bỏ qua.
- [ ] Người cao tuổi vào giữa tháng: chép từ ngày muộn nhất trong ba ngày: hợp đồng hiệu lực, Hoàn tất tiếp nhận, kế hoạch hiệu lực.
- [ ] Trả về tóm tắt: số việc tạo, số mốc bỏ qua, các ngày chưa có ca công bố, người chưa có kế hoạch.
- [ ] IT: tháng có lịch ca qua đêm; mục "theo ngày trong tuần"; chép lại không trùng; không có ca thì báo ngày.

### Task 3: Sửa bản Nháp và Công bố tháng

- [ ] Trưởng tầng đổi người thực hiện, bỏ, thêm việc Nháp (dùng lại kiểm tra của FR-017). Bỏ việc Nháp thì xóa hoặc chuyển Hủy, theo quy định hiện có.
- [ ] `POST /care-tasks/month-drafts/publish` `{ month }`: chuyển toàn bộ Nháp của tầng và tháng sang Chưa đến hạn, rồi cho bộ chạy theo giờ chuyển tiếp (BR-M04-26). Công bố lại sau khi bổ sung dùng cùng lệnh.
- [ ] IT: công bố xong thì việc hiện trên checklist đúng ca; người làm nhận được việc.

### Task 4: Kế hoạch đổi giữa tháng và phân công đổi

- [ ] Nghe `CarePlanChanged` (hoặc sửa chỗ phát trong `CarePlanActivation`): hủy việc `DRAFT`, `NOT_YET_DUE` theo mục của bản cũ từ ngày hiệu lực, rồi báo Trưởng tầng "kế hoạch đã đổi, hãy chép bổ sung".
- [ ] `SharedCareTasks.reassignShift` gán cả việc Nháp khi phân công ca công bố.
- [ ] IT cho cả hai.

### Task 5: Nhắc ngày 25 (CFG-M04-14)

- [ ] Thêm `CFG_M04_14` vào `ParameterCode` và một dòng tham số (mặc định 25).
- [ ] Việc định kỳ hằng ngày: đúng ngày CFG-M04-14 mà tháng sau của tầng chưa công bố thì nhắc Trưởng tầng. 00:00 ngày đầu tháng vẫn chưa công bố thì báo Quản lý viện (mức Trung bình). Đặt dấu `reminder_marker` để không nhắc lặp.

### Task 6: Web màn Lập công việc theo tháng

- [ ] Bỏ ô ngày, ô ca, ô tầng. Màn có ô chọn tháng (mặc định tháng sau), nút "Chép từ kế hoạch", và bản xem trước nhóm theo ngày rồi theo người, mỗi dòng gồm giờ, việc, người làm, trạng thái Nháp.
- [ ] Đổi người làm, bỏ dòng, thêm việc phát sinh (dùng lại hộp thoại Q-452, ngày giờ suy ra ca). Nút chính "Công bố công việc tháng" có hộp xác nhận nêu số việc.
- [ ] Hiện tóm tắt của lệnh chép: ngày chưa có ca, người chưa có kế hoạch.
- [ ] Giữ phần lịch sử công việc đã lập như hiện nay.
- [ ] Test Vitest; xem trên trình duyệt bằng tài khoản Trưởng tầng mẫu.

### Task 7: Dữ liệu demo, tài liệu

- [ ] Sửa dữ liệu demo: mỗi Trưởng tầng một tầng. Hiện `demo.truongtang` (staff 9003) có hai phân công cùng hiệu lực (tầng 9001 và 9002). Đảm bảo tháng sau có lịch ca công bố để thử.
- [ ] Cập nhật tasks.md (Phase 16), progress.md, DESIGN.md (bố cục màn mới). Chạy `./mvnw clean verify -Dit.forkCount=3` trước khi mở PR.
