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

## Cắt bớt (chủ dự án 2026-11-26) và kết quả

Chỉ làm phần lõi; các phần sau gác lại, làm khi cần:
- Nhắc ngày 25 (CFG-M04-14) và báo Quản lý viện ngày đầu tháng (Task 5 cũ).
- Tự hủy việc theo bản kế hoạch cũ khi kế hoạch đổi giữa tháng (Task 4 cũ): vẫn chỉ báo như FR-006 hiện hành; Trưởng tầng bấm "Chép từ kế hoạch" lại, hệ thống không tạo trùng.
- Chép riêng theo một người từ một ngày: chép lại cả tháng cũng bổ sung người mới.
- Sửa từng trường của việc Nháp: người làm lấy theo phân công ca (người phụ trách chính, vai trò khớp), không có thì việc chung; sai thì bỏ dòng.
- Màn web vẫn giữ phần lập theo ca (chọn ngày, ca) bên dưới khối "Công việc tháng" để bổ sung và lập việc phát sinh cho một ca cụ thể.

Đã làm:
- [x] Backend (kho Elder_Care 805543a9): `TaskStatus.DRAFT`, migration `V005_105` (sửa enum, không bảng mới); việc Nháp bị loại khỏi checklist, lịch theo người (FR-048), lịch hằng ngày người thân, thống kê; `CareMonthPlanning` + `CareMonthPlanningController`: `POST /care-tasks/month-drafts` (chép tháng, dùng mốc `planOccurrences` của Q-452 cho mọi ca đã công bố của tầng), `GET` (danh sách Nháp), `POST /publish`, `POST /{id}/discard` (bảng không cho xóa nên chuyển Hủy với `closure_rule` DRAFT_DISCARDED). IT `BR_18_ShiftTaskPlanningIT` 7/7 (thêm 3 ca Q453), `ApiContractIT` và 9 IT checklist, quá hạn, bàn giao, lịch, thống kê xanh.
- [x] Web (kho Elder_Care_Frontend 7f297c7): `MonthTaskPlanning.jsx` trên đầu màn Lập công việc ca: chọn tháng (mặc định tháng sau), "Chép từ kế hoạch", bảng Nháp 10 dòng mỗi trang, "Bỏ", "Công bố công việc tháng (n)" có hộp xác nhận; tầng lấy từ lịch ca của Trưởng tầng, nhiều tầng thì báo và khóa. Vitest care-tasks 123/123, lint xanh.

Còn tồn:
- Dữ liệu demo: `demo.truongtang` (staff 9003) có hai phân công Trưởng tầng cùng hiệu lực (tầng 9001 từ 2026-10-21, 9002 từ 2026-11-22), nên màn báo "đang phụ trách nhiều tầng" và không chép được. Cần kết thúc phân công cũ (dữ liệu demo hoặc lệnh của 008).
- PR #242: khoảng ngày Q-451 đọc từ `CFG-M04-15` (mặc định 7 ngày); migration `V099_11` đồng thời khai báo `CFG-M04-14` theo Phụ lục 25. Job nhắc công bố tháng vẫn gác lại.
- Chưa chạy `clean verify` toàn bộ.
