# Plan thực hiện: sửa luồng BF-08 – Thay đổi lưu trú (tài liệu + mã ba kho)

> **Dành cho AI thực hiện.** File này tự chứa: hành vi đích, quyết định đã chốt, từng file cần sửa, test và lệnh kiểm. Mọi quyết định nghiệp vụ trong file **đã được chủ dự án chốt**; không dừng để hỏi, không mở lượt clarify. Chỗ nào mã thực tế khác mô tả ở đây (tên hàm, chữ ký), đọc mã rồi làm theo **ý** của bước, ghi lệch vào mục "Ghi chú triển khai" của `specs/004-admission-residency/plan.md`.

---

## Cập nhật 2026-11-21 (đọc trước)

- **Tên hiển thị đã đổi** thành **"Điều chỉnh hợp đồng"** (Q-425, đã có trong `nghiep-vu.md` mục 6.6, BF-08 và ở web, app). Mọi chữ hiển thị mới (tiêu đề, nút, thông báo, mô tả lỗi tiếng Việt, bản in) dùng "điều chỉnh hợp đồng" / "yêu cầu điều chỉnh hợp đồng"; chỗ nào trong plan này còn ghi "thay đổi lưu trú" ở **văn bản hiển thị** thì hiểu là tên mới. **Tên trong mã giữ nguyên** (`M02.RESIDENCY_CHANGE`, `ResidencyChange*`, `/residency-change-requests`, `RESIDENCY_CHANGE_REQUEST`).
- **Q-425 đã dùng cho việc đổi tên.** Quy tắc 4 loại dịch vụ (R21) dùng **Q-426**. Nếu Bước 1 đã chạy xong: thêm dòng Q-426 (Phụ lục B) vào cuối bảng 24.2, thêm **FR-043k** vào spec 004, `SERVICE_TYPE_NOT_REGISTRABLE` vào `api-004.yaml`, rồi làm 5.4a và W15.
- Trước khi làm tiếp, đối chiếu `tasks.md` Phase 14 với mã hiện có (một số phần như trang in phụ lục, hộp thoại quyết định giữ mức đã có): chỉ làm phần chưa có, không làm lại.

---

## 0. Cách làm

1. Đọc trước: `Elder_Care/CLAUDE.md`, `Elder_Care/AGENTS.md`, `Elder_Care/backend/README.md`, `specs/000-shared-foundation/research.md` (R-01 → R-15), `Elder_Care_Frontend/DESIGN.md` mục 0 (chỉ cho phần web).
2. Làm theo thứ tự **Bước 1 → Bước 10**. Mỗi bước có mục "Xong khi"; chưa đạt thì không sang bước sau.
3. Test viết trước, chạy thấy đỏ, rồi mới cài (constitution, CLAUDE.md "Khi vào triển khai" điều 5).
4. Quy ước bắt buộc:
   - Tên trong mã bằng tiếng Anh; tên tiếng Việt trong tài liệu dịch theo `specs/000-shared-foundation/glossary.md` (thêm mục mới vào phần 004, 000, 001, 005).
   - File trong kho dùng **CRLF**. Sửa bằng công cụ Edit hoặc script node tách dòng `/\r?\n/`, ghi lại `\r\n`; không dùng `sed -i` trong Git Bash. Kiểm bằng `git diff --stat` (cả file bị đổi là hỏng xuống dòng).
   - Java: chạy `./mvnw spotless:apply` trước khi verify.
   - Migration chỉ dùng cú pháp chạy được trên cả MySQL 8.0 và 8.4; bảng mới khai `COMMENT = 'DataGroup=…'` (R-15); `CHECK` cho phép `NULL` viết rõ `IS NULL OR …`.
   - Chỉ sửa đúng chỗ cần (karpathy-guidelines); không sửa định dạng mã kế bên.
   - Mã mới (Q-xx, CFG-xx, FR-xx, mã lỗi) không được trùng hoặc dùng lại mã đã có hay đã bỏ: trước khi đặt, `grep` cả `nghiep-vu.md` (kể cả bảng 24 và ghi chú "bỏ theo Q-…") và spec liên quan; trùng thì lấy số kế tiếp chưa dùng và ghi lệch vào plan 004.
   - Không commit, push trừ khi người dùng yêu cầu.
5. Nền: ba kho đang ở nhánh `fix/approval-superseded-and-bed-fee` (Elder_Care), `fix/residency-change-approval-content` (Frontend), `impl/004-residency-change-confirmation` (Mobile), trong đó có phần xác nhận người đại diện (FR-043a, FR-043c) chưa commit. **Làm tiếp trên chính các nhánh này**, không tạo nhánh từ `develop`, để không mất phần đó.

---

## 0a. Phạm vi được sửa (bắt buộc tuân theo)

**Chỉ được sửa các chỗ trong bảng dưới.** Cần sửa file ngoài danh sách (kể cả để test chạy được) thì **dừng, không sửa**, ghi vào báo cáo: file nào, vì sao, đề xuất sửa gì. Không "tiện tay" sửa lỗi, định dạng, đổi tên ở chỗ khác; thấy lỗi không thuộc plan thì chỉ ghi vào báo cáo.

| Khu vực | Được sửa | Giới hạn |
| ------- | -------- | -------- |
| Tài liệu | `docs/business/luong-nghiep-vu.md` (chỉ mục BF-08); `docs/business/nghiep-vu.md` (chỉ 6.6, BR-M01-03, BR-M04-20, 7.4, 8.1, Phụ lục 25 dòng CFG-M02-17/18, bảng 24.2 dòng Q-415 → Q-426); spec, plan, tasks, data-model, contracts, glossary của 000, 001, 004, 005, 009 (và 010 chỉ ở Đợt 2) theo mục 2 | Không sửa mục khác của hai file nghiệp vụ |
| `administration/approval` (000) | `api/spi/RequestTypeHandler.java`, `api/ApprovalRequestApi.java`, `api/events/RequestReturnedToDraft.java` (mới), `internal/domain/RequestTransitionTable.java`, `internal/domain/ApprovalActions.java`, `internal/service/ApplyTransaction.java`, `internal/service/ApprovalRequestService.java`, `internal/persistence/ApprovalRequestStore.java`, `web/ApprovalRequestController.java`, `web/request/` (body mới) | **Mọi hàm SPI mới phải có mặc định giữ nguyên hành vi cũ**; không đổi hành vi của loại yêu cầu khác; không đổi chữ ký hàm cũ (chỉ thêm overload) |
| `admission` (004) | Toàn bộ `internal/change/`, `web/ResidencyChangeRequestController.java`, `web/ResidencyChangeConfirmationController.java`, `web/ContractController.java` (chỉ phần phụ lục), `web/request/`, `web/response/` liên quan thay đổi lưu trú và phụ lục, `api/events/` (sự kiện mới), `scheduling/` (job nhắc ký) | Không sửa phần tiếp nhận, hợp đồng gốc, tạm vắng, kết thúc lưu trú, qua đời, tạm trú |
| Migration | Tạo mới `V004_07`, `V004_08`, `V004_09`, `V001_<kế tiếp>` | **Không sửa migration đã có.** Trigger `trg_contract_version_row_lock`: DROP/CREATE lại trong migration mới, chép **nguyên văn** phần DRAFT và `locked_at`, chỉ đổi phần ADDENDUM |
| `resident` (001) | `internal/assessment/service/CareLevelChangeRequestReminderService.java` (thêm `stopReminding`), `scheduling/RemindCareLevelChangeRequestJob.java`, `api/` (hàm đọc mức đánh giá gần nhất, sự kiện mới), phần mới cho quyết định giữ mức (service, store, controller) | Không đổi cách xác nhận đánh giá, không đổi `AssessmentService` ngoài chỗ cần cho 6.6 |
| `care` (005) | `internal/careplan/service/CarePlanReviewListener.java` (chỉ `onAddendum`), `api/CareRiskAgreementApi.java` (mới) và lớp cài trong `internal/careplan/`; endpoint đọc thỏa thuận theo người cao tuổi nếu chưa có (W14) | Không đổi kế hoạch chăm sóc, checklist, công việc |
| `notification` (009) | Chỉ chỗ khai danh mục loại thông báo và ánh xạ người nhận cho 7 loại ở mục 8 | Không đổi cơ chế gửi |
| `roombedallocation` (003) | **Không sửa.** Chỉ gọi API có sẵn | — |
| `billing` (010) | **Chỉ ở Đợt 2** (Bước 9) | Đợt 1 không đụng |
| Test backend | IT, test mới ghi trong plan; `ChangeTestBase` (helper); sửa IT có sẵn **chỉ** khi đỏ vì hành vi mới (phải ký trước khi áp dụng, ô "thuộc gói", mức theo đánh giá), và chỉ sửa phần dựng dữ liệu, không nới điều kiện kiểm | Không xóa, không `@Disabled` test |
| Web | `src/features/admissions/` (trừ màn không liên quan thay đổi lưu trú, phụ lục), `src/features/approval-requests/` (chỉ nút Trả về nháp), `src/features/residents/` (chỉ hộp thoại quyết định giữ mức), `src/common/notifications/NotificationDetail.jsx` (chỉ mở nguồn mới), `src/App.jsx` (chỉ thêm route trang in); `src/api/` chỉ sinh lại bằng `npm run gen:api` | Không sửa `src/components/ui/`, DESIGN.md, skill |
| App | `lib/family/admissions/` (3 màn ở M1, M1a, M2), chỗ điều hướng thông báo của `lib/family/` (M3), test tương ứng | Không sửa `lib/common/`, `lib/staff/`, `lib/resident/` |

**Hai đợt:**
- **Đợt 1** (làm ngay): Bước 1 → 8 và Bước 10.
- **Đợt 2** (chỉ làm khi người dùng yêu cầu riêng): Bước 9 – chênh lệch giá khi chuyển giường gấp (R14). Ở Đợt 1, phần R14 trong tài liệu vẫn ghi (Q-424 (c)) nhưng **không** sửa mã `billing`; ghi vào `tasks.md` là task chưa làm.

---

## 1. Hành vi đích (tiêu chí nghiệm thu)

Mỗi dòng là một quy tắc phải có test chứng minh (cột Test ở mục 11).

| Mã | Quy tắc |
| -- | ------- |
| R1 | Bác sĩ xác nhận đánh giá lại ra mức khác: Điều dưỡng phụ trách và người duyệt kế hoạch được báo xem xét kế hoạch **ngay** (đã có). Mức chăm sóc hiện hành, dịch vụ chăm sóc, đơn giá của hợp đồng chỉ đổi khi phụ lục được áp dụng (đã có). |
| R2 | Khi phụ lục có `CARE_LEVEL` được áp dụng: **không** báo xem xét kế hoạch lần nữa nếu người cao tuổi đã có phiên bản kế hoạch chăm sóc tạo sau lần báo gần nhất từ đánh giá lại; chưa có thì báo như cũ. |
| R3 | Duyệt yêu cầu **không phải thay đổi nhỏ** thì phụ lục ở **Chờ ký** (`PENDING_SIGNATURE`). Thay đổi nhỏ (Q-338) thì phụ lục ở Chờ hiệu lực như cũ. |
| R4 | Phụ lục Chờ ký: bộ lập lịch **không** áp dụng và **không** chuyển yêu cầu sang Áp dụng không thành; yêu cầu giữ Đã duyệt. |
| R5 | Lệnh **Ghi nhận phụ lục đã ký** (Hành chính, Quản lý viện): ngày ký ≤ hôm nay và ≥ ngày duyệt; người ký là người đại diện hiệu lực của người cao tuổi; tệp PDF/JPG/PNG bắt buộc. Phụ lục Chờ ký → Chờ hiệu lực. |
| R6 | Ngày hiệu lực thực tế = muộn nhất trong (ngày mong muốn, ngày duyệt, ngày ký). Ký muộn hơn ngày hiệu lực đang có thì dời ngày hiệu lực của yêu cầu và phụ lục, dời giữ giường (nếu có); không tính phí lùi. |
| R7 | Phụ lục Chờ ký: nhắc Hành chính mỗi [1 ngày] (CFG-M02-17); quá [7 ngày] (CFG-M02-18) kể từ ngày duyệt thì báo Quản lý viện đúng một lần. |
| R8 | Người duyệt có lệnh **Trả về nháp** (Chờ duyệt → Nháp, bắt buộc lý do). Trả về thì xác nhận người đại diện cũ bị bỏ (cả xác nhận kèm bằng chứng lẫn lần gửi xác nhận trên app), người lập và Hành chính được báo. Người duyệt không sửa nội dung trực tiếp (giữ như hiện tại). |
| R9 | Hủy (bắt buộc lý do): Nháp, Chờ duyệt – người lập, Hành chính, Quản lý viện; tài khoản người thân chỉ hủy yêu cầu do chính mình lập. Đã duyệt – người duyệt được (đã có). Hủy khi phụ lục Chờ ký hoặc Chờ hiệu lực thì phụ lục → **Đã hủy** (`CANCELLED`), nhả thuộc tính khóa, hủy giữ giường. Áp dụng không thành và yêu cầu bị thay thế thì phụ lục → Không áp dụng (giữ như cũ, kể cả khi đang Chờ ký). |
| R10 | Báo người đại diện (mức Nhẹ, không kèm giá) khi yêu cầu liên quan tới gia đình bị **Từ chối**, **Áp dụng không thành**, **Hủy**, và khi phụ lục **Đã áp dụng** (đã có). Có nhiều người đại diện: khi gửi duyệt và khi áp dụng, mọi người đại diện hiệu lực được báo. |
| R11 | **Ý kiến người cao tuổi** về yêu cầu: Hành chính, Điều dưỡng, Bác sĩ ghi (Đồng ý / Không đồng ý / Chưa nói được, nguồn, ghi chú), ghi thêm, bản mới là hiện hành. Gửi duyệt không bị chặn khi thiếu ý kiến, phản hồi có cảnh báo; ý kiến hiện hành "Không đồng ý" thì gửi duyệt bắt buộc lý do. Thay đổi nhỏ không cảnh báo. Quản lý viện thấy ý kiến khi duyệt. |
| R12 | Yêu cầu người đại diện gửi qua cổng: nếu Hành chính đổi phần thực chất (loại thay đổi, giá trị mới, ngày hiệu lực mong muốn, giường, lịch đến, dịch vụ) thì phải có xác nhận người đại diện (1a hoặc 1b) mới gửi duyệt được; chỉ đổi ghi chú, tệp thì không cần. |
| R13 | Gia đình không đồng ý đổi mức: Hành chính ghi **quyết định** cho lần đánh giá lại: (a) `KEEP_CURRENT_LEVEL` kèm thỏa thuận lựa chọn có rủi ro đang hiệu lực của người cao tuổi và ngày xem lại → dừng nhắc lập yêu cầu đổi mức (CFG-M03-02), tới ngày xem lại nhắc Bác sĩ và Hành chính; (b) `LEAVE_FACILITY` → dừng nhắc, báo Hành chính lập hồ sơ kết thúc lưu trú (không tự lập). Đánh giá lại mới thì quyết định cũ hết tác dụng. |
| R14 | Chuyển giường gấp (003 FR-030) sang giường đơn giá cao hơn: từ ngày chuyển tới ngày phụ lục đổi phòng được áp dụng, phí lưu trú tính theo đơn giá cũ; đơn giá thấp hơn thì tính giá mới từ ngày chuyển. |
| R15 | Người cao tuổi sang trạng thái cuối: yêu cầu Nháp, Chờ duyệt → Hủy; Đã duyệt → Áp dụng không thành; phụ lục Chờ ký hoặc Chờ hiệu lực → Không áp dụng (đã có cho Chờ hiệu lực; mở rộng cho Chờ ký). |
| R16 | Xác nhận người đại diện kèm bằng chứng (1a) ghi **người đại diện nào** đã xác nhận (bắt buộc, phải là người đại diện hiệu lực). Thông báo "gửi duyệt" ở R10 bỏ qua người này (và người đã Đồng ý trên app ở 1b). |
| R17 | Phụ lục có **số phụ lục** tăng dần theo từng hợp đồng (01, 02, …), cấp khi tạo phụ lục, không đổi, không tái sử dụng (kể cả phụ lục Đã hủy, Không áp dụng). Ghi nhận đã ký có thể ghi **người ký phía viện** (nhân viên, không bắt buộc). Số phụ lục và người ký phía viện hiện trên API và bản in. |
| R18 | Ý kiến người cao tuổi lưu dấu nội dung yêu cầu lúc ghi. Gửi duyệt mà nội dung thực chất đã khác thì cảnh báo `RESIDENT_OPINION_OUTDATED` (không chặn); lý do bắt buộc của R11 chỉ xét ý kiến hiện hành. |
| R19 | **Thay đổi nhỏ do hệ thống xác định**, không theo ô người lập tick: dòng thêm dịch vụ chỉ thuộc gói khi hợp đồng hiện hành đã có dòng dịch vụ đó mang dấu thuộc gói; dịch vụ chưa có trong hợp đồng thì không thuộc gói. Trường `inPackage` người lập gửi lên bị bỏ qua. |
| R20 | **Mức chăm sóc mới lấy từ đánh giá**: yêu cầu có `CARE_LEVEL` phải có mức bằng mức của lần đánh giá gần nhất đã xác nhận, và mức đó khác mức của hợp đồng hiện hành; sai thì 409 `CARE_LEVEL_NOT_FROM_ASSESSMENT` khi lập, sửa, gửi duyệt và duyệt. |
| R21 | **Chỉ 4 loại dịch vụ được thêm qua loại thay đổi "Dịch vụ"**: `REHABILITATION` (phục hồi chức năng), `ACTIVITY` (hoạt động đăng ký dài hạn), `MEDICAL_ESCORT` (đưa đi khám), `INJECTION` (tiêm theo y lệnh). Các loại khác bị chặn khi **thêm**: `CARE` (đổi qua Mức chăm sóc), `RESIDENCY_FEE` (qua Giường), `DAY_CARE_SESSION` (qua Loại lưu trú, Lịch đến), `MEAL`, `FAMILY_STAY` (luồng dinh dưỡng, người thân ở lại), `FACILITY_SUPPLIED_MEDICATION` (theo liều từ đơn thuốc), `CONSUMABLE`, `MEDICAL_SUPPLY` (theo lần dùng hoặc đề nghị mua hộ), `OTHER`. **Bớt** thì cho bớt mọi dòng đang có trong hợp đồng (để dọn dòng đăng ký sai trước đây). Không đổi cách tính phí: dịch vụ thuộc gói thì công việc, lượt điểm danh của dịch vụ đó không sinh phí riêng; ngoài gói hoặc không đăng ký thì tính theo lần (15.2) – chỉ **kiểm** đúng như vậy, không sửa `billing` (ngoài phạm vi). |
| R22 | **Loại thay đổi "Giường" → "Loại phòng"** (Q-445, FR-043l): chỉ dùng khi giường mới thuộc loại phòng khác loại phòng của hợp đồng; cùng loại phòng thì 422 `BED_CHANGE_SAME_ROOM_TYPE`, dùng Chuyển giường trực tiếp (7.4), không lập yêu cầu, không ký phụ lục. Không xét khi kèm bán trú sang nội trú, sau chuyển giường gấp, hoặc hợp đồng chưa ghi loại phòng. **Đã cài 2026-11-23** (xem tasks T130). |

---

## 2. Bước 1 – Tài liệu nghiệp vụ và spec

### 1.1 `docs/business/luong-nghiep-vu.md`
Thay **nguyên mục** `## BF-08 – Thay đổi lưu trú` (từ tiêu đề tới trước `---` kế tiếp) bằng nội dung **Phụ lục A**.

### 1.2 `docs/business/nghiep-vu.md`
- Mục **6.6**: sửa dòng trạng thái thành
  `Trạng thái yêu cầu thay đổi: Nháp → Chờ duyệt → Đã duyệt (chờ hiệu lực) → Đã áp dụng / Từ chối / Hủy; Chờ duyệt → Nháp (Trả về nháp, Q-419).`
  Thêm các gạch đầu dòng sau, đặt sau gạch "Người duyệt (Q-338)":
  - Xác nhận người đại diện trước khi gửi duyệt (Q-415), gồm xác nhận kèm bằng chứng hoặc đồng ý trên ứng dụng (Q-416); sửa phần thực chất sau khi xác nhận, kể cả yêu cầu người đại diện gửi qua cổng, thì xác nhận lại (Q-418).
  - Ý kiến người cao tuổi về thay đổi được ghi riêng, có nguồn; không chặn gửi duyệt; "Không đồng ý" thì gửi duyệt bắt buộc lý do (Q-417).
  - Phụ lục của yêu cầu không phải thay đổi nhỏ ở **Chờ ký** cho tới khi Hành chính ghi nhận bản đã ký (ngày ký, người đại diện ký, bản scan); chưa ký thì không áp dụng. Ngày hiệu lực thực tế là ngày muộn nhất trong ngày mong muốn, ngày duyệt, ngày ký (Q-420). Trạng thái phụ lục: Chờ ký → Chờ hiệu lực → Đã áp dụng / Không áp dụng / Đã hủy.
  - Hủy: Nháp, Chờ duyệt do người lập, Hành chính, Quản lý viện; người đại diện chỉ hủy yêu cầu mình gửi; Đã duyệt do người duyệt. Phụ lục của yêu cầu bị hủy chuyển Đã hủy (Q-421).
  - Nhiều người đại diện: một người xác nhận và ký là đủ; người đại diện khác được báo khi gửi duyệt và khi áp dụng (Q-422).
- **BR-M01-03**: thêm câu cuối: "Điều dưỡng phụ trách được báo xem xét kế hoạch ngay khi lần đánh giá lại được xác nhận; kế hoạch có thể đổi trước khi phụ lục đổi mức được áp dụng (Q-423)."
- **BR-M04-20**: thêm câu: "Phụ lục đổi mức được áp dụng không báo lại khi đã có phiên bản kế hoạch lập sau lần báo từ đánh giá lại (Q-423)."
- Mục **8.1** (thỏa thuận lựa chọn có rủi ro): thêm câu: "Gia đình không đồng ý đổi mức chăm sóc theo đánh giá lại thì có thể giữ mức cũ kèm thỏa thuận lựa chọn có rủi ro (Q-424)."
- Mục **7.4** (chuyển giường gấp): thêm câu về chênh lệch đơn giá như R14 (Q-424 phần (c)).
- **Phụ lục 25**: thêm 2 dòng sau dòng `CFG-M02-13` (mã CFG-M02-14 → 16 đã bỏ theo Q-385, Q-380, **không dùng lại**):
  `| CFG-M02-17 | Chu kỳ nhắc ghi nhận phụ lục đã ký | 1 ngày | 6.6 |`
  `| CFG-M02-18 | Báo Quản lý viện khi phụ lục chưa ký sau ngày duyệt | 7 ngày | 6.6 |`
- **Mục 24.2**: thêm các dòng Q-415 → Q-426 ở **Phụ lục B** vào cuối bảng (sau Q-413).

### 1.3 Spec
| File | Sửa |
| ---- | --- |
| `specs/004-admission-residency/spec.md` | FR-043a, FR-043c: bỏ chữ "chờ chủ dự án duyệt", ghi nguồn Q-415, Q-416. Thêm **FR-043d** (R3–R7, Q-420), **FR-043e** (R8, Q-419), **FR-043f** (R9, Q-421), **FR-043g** (R10, Q-422), **FR-043h** (R11, Q-417), **FR-043i** (R12, Q-418), **FR-043j** (R20, BR-M02-22), **FR-043k** (R21, Q-426). Bổ sung vào FR đã có: FR-043a – xác nhận ghi người đại diện đã xác nhận (R16, Q-415); FR-043d – số phụ lục, người ký phía viện (R17, Q-420); FR-043h – dấu nội dung của ý kiến (R18, Q-417); FR-043 – thay đổi nhỏ do hệ thống xác định theo hợp đồng hiện hành, không theo dấu người lập (R19, Q-338). Bảng trạng thái phụ lục (dòng ~612): thêm Chờ ký, Đã hủy. Mục tham số: CFG-M02-17, 16. |
| `specs/001-elder-profile-assessment/spec.md` | Thêm FR cho R13 (quyết định khi gia đình không đồng ý đổi mức), nguồn Q-424. |
| `specs/005-daily-care-tasks/spec.md` | FR-011: thêm R2, nguồn Q-423. |
| `specs/000-shared-foundation/spec.md` | Vòng đời yêu cầu phê duyệt: thêm Trả về nháp (tùy loại khai báo), hoãn áp dụng (loại chưa sẵn sàng), dời ngày hiệu lực của yêu cầu Đã duyệt, quyền hủy do loại quy định, lý do khi gửi duyệt (tùy chọn). |
| `specs/010-incurred-costs/spec.md` | Thêm FR cho R14, nguồn Q-424. |
| `specs/009-notification-service/spec.md` | Thêm loại thông báo ở mục 9 (Bước 7). |
| `data-model.md` của 004, 001, 000 | Cột, bảng mới ở Bước 3, 5, 6. |
| `contracts/api-004.yaml`, `contracts/api-000.yaml`, `contracts/api-001.yaml` | Theo **Phụ lục C**. Sau đó ở kho web chạy `npm run gen:api`. |
| `contracts/module-interfaces.md` của 000, 004, 005 | SPI, API mới ở Bước 2, 6, 8. |
| `specs/004-admission-residency/tasks.md` | Thêm "Phase 14: BF-08 sửa đổi" liệt kê task theo các Bước 2 → 10 của file này, đánh `[x]` khi xong. |

**Xong khi:** các file trên đã sửa; `grep -n "Q-415" docs/business/nghiep-vu.md` có kết quả; BF-08 mới có ở `luong-nghiep-vu.md`.

---

## 3. Bước 2 – Bộ máy phê duyệt 000 (`administration.approval`)

Gốc: `backend/src/main/java/com/eldercare/backend/administration/approval/`.

| # | Thay đổi | File |
| - | -------- | ---- |
| 2.1 | SPI `RequestTypeHandler`, thêm (mọi hàm có mặc định giữ hành vi cũ): `default boolean readyToApply(ApprovalRequestView r) { return true; }`; `default boolean allowReturnToDraft() { return false; }`; `default void onReturnToDraft(ApprovalRequestView r) {}`; `default boolean mayCancel(CurrentUser u, ApprovalRequestView r) { return r.requesterId() == u.accountId(); }`; `default List<UnmetCondition> checkOnSubmit(ApprovalRequestView r, Optional<Reason> reason) { return checkOnSubmit(r); }` | `api/spi/RequestTypeHandler.java` |
| 2.2 | `ApplyTransaction.apply`: sau kiểm `APPROVED` và ngày, gọi `handler.readyToApply(request)`; `false` thì `return Outcome.NOT_APPLICABLE` (không ghi gì) | `internal/service/ApplyTransaction.java` |
| 2.3 | `RequestTransitionTable`: thêm `Command.RETURN_TO_DRAFT` với `Map.of(PENDING_APPROVAL, DRAFT)` | `internal/domain/RequestTransitionTable.java` |
| 2.4 | `ApprovalRequestService.returnToDraft(id, ifMatch, Reason reason)`: `@Transactional`; khóa yêu cầu; `handler.allowReturnToDraft()` false → 409 `RETURN_TO_DRAFT_NOT_ALLOWED`; `requireDecider` (cùng kiểm như approve, gồm `approverRoleRestriction`); `requireVersion`; cập nhật trạng thái; ghi nhật ký `ApprovalActions.RETURNED_TO_DRAFT` (thêm hằng `new AuditAction("REQUEST_RETURNED_TO_DRAFT", false)`) kèm lý do; gọi `handler.onReturnToDraft(updated)`; phát `RequestReturnedToDraft(request, reason, at)` (`api/events/`, theo mẫu `RequestDecided`) | `internal/service/ApprovalRequestService.java`, `internal/domain/ApprovalActions.java`, `api/events/RequestReturnedToDraft.java` |
| 2.5 | `ApprovalRequestService.cancel`: nhánh không phải `APPROVED` thay `requireRequester(user, request)` bằng `if (!handler.mayCancel(user, request)) throw new PermissionDeniedException(...)` | như trên |
| 2.6 | `submit(id, ifMatch, Optional<Reason> reason)`: truyền lý do vào `handler.checkOnSubmit(request, reason)`; lý do có thì ghi vào nhật ký SUBMITTED. Giữ overload cũ `submit(id, ifMatch)` gọi với `Optional.empty()` | như trên, `submitLocked` |
| 2.7 | `ApprovalRequestApi.postponeEffectiveDate(long id, LocalDate newDate, Reason reason)`: `Propagation.MANDATORY`; yêu cầu phải `APPROVED`; `newDate` sau `actualEffectiveDate` (không thì bỏ qua); cập nhật `actual_effective_date`; ghi nhật ký `REQUEST_EFFECTIVE_DATE_POSTPONED` | `api/ApprovalRequestApi.java`, `ApprovalRequestService`, `persistence/ApprovalRequestStore.java` |
| 2.8 | Web: `POST /api/v1/approval-requests/{id}/return-to-draft` body `{reason}` (yêu cầu `If-Match`); `POST /{id}/submit` nhận body tùy chọn `{reason}` | `web/ApprovalRequestController.java`, `web/request/` |
| 2.9 | Bản giả của `ApprovalRequestApi` ở `support/fake/` (nếu có) thêm `postponeEffectiveDate` | `backend/src/test/java/.../support/fake/` |

**Test (viết trước):** `RequestTransitionTableTest` (thêm ca RETURN_TO_DRAFT); `ApprovalReturnToDraftIT` (loại cho phép / không cho phép, người không có quyền duyệt bị 403, `onReturnToDraft` được gọi); `ApplyTransactionNotReadyIT` (handler giả `readyToApply=false` → yêu cầu giữ `APPROVED`, không có nhật ký `REQUEST_APPLY_FAILED`, lần sau `true` thì áp dụng); `PostponeEffectiveDateIT`; `ApprovalCancelPermissionIT` (`mayCancel` override).

**Xong khi:** các test trên xanh; `ApiContractIT`, `ModuleBoundaryTest`, `*ArchTest` xanh.

---

## 4. Bước 3 – Phụ lục Chờ ký (`admission`)

Gốc: `backend/src/main/java/com/eldercare/backend/admission/`.

### 3.1 Migration `backend/src/main/resources/db/migration/V004_07__addendum_signature.sql`
- `ALTER TABLE contract_version MODIFY addendum_status ENUM ('PENDING_SIGNATURE','PENDING_EFFECTIVE','APPLIED','NOT_APPLIED','CANCELLED') NULL;`
- Thêm cột: `signed_date DATE NULL`, `signed_file_id BIGINT NULL`, `signed_by_family_member_id BIGINT NULL`, `facility_signatory_staff_id BIGINT NULL`, `signature_recorded_by BIGINT NULL`, `signature_recorded_at DATETIME(6) NULL`, `signature_note VARCHAR(1000) NULL`.
- **Số phụ lục (R17):** cột `addendum_no SMALLINT NULL`, `UNIQUE KEY uq_contract_version_addendum_no (contract_id, addendum_no)`, `CHECK (kind <> 'ADDENDUM' OR addendum_no IS NOT NULL)` thêm **sau** khi điền dữ liệu cũ. Điền phụ lục đã có: đánh số theo `contract_id`, thứ tự `created_at, id` (dùng biến phiên hoặc `ROW_NUMBER()` trong `UPDATE … JOIN`, MySQL 8.0 có). Trigger: `addendum_no` không đổi sau khi đặt.
- `CHECK chk_contract_version_signed`: `addendum_status IS NULL OR addendum_status = 'PENDING_SIGNATURE' OR signed_date IS NOT NULL OR <phụ lục của thay đổi nhỏ>`. Vì không biết được thay đổi nhỏ ở mức cột, **thêm cột** `signature_required BOOLEAN NOT NULL DEFAULT FALSE` và viết `CHECK (signature_required = FALSE OR addendum_status IN ('PENDING_SIGNATURE','NOT_APPLIED','CANCELLED') OR signed_date IS NOT NULL)`.
- Thay trigger `trg_contract_version_row_lock` (DROP rồi CREATE, chép nguyên phần DRAFT và phần khóa `locked_at`). Phần ADDENDUM: giữ cấm đổi `contract_id`, `approval_request_id`, `change_types`, `created_at`, `content_before`, `content_after`, `signature_required`; **cho đổi `effective_date` chỉ khi** `OLD.addendum_status IN ('PENDING_SIGNATURE','PENDING_EFFECTIVE')` và `NEW.effective_date >= OLD.effective_date`; cho đặt các cột ký **một lần** khi `OLD.addendum_status = 'PENDING_SIGNATURE'` (`OLD.signed_date IS NULL`); cấm đổi cột ký khi đã có giá trị.
- Migration tham số (cùng file hoặc `V004_08__parameter_addendum_signature.sql`, theo mẫu `V004_06__parameter_q394_q395.sql`): `CFG-M02-17` = 1 ngày, `CFG-M02-18` = 7 ngày.

### 3.2 Domain và lưu trữ
- `internal/change/domain/Addendum.java`: `Status` thêm `PENDING_SIGNATURE`, `CANCELLED`; record thêm `int addendumNo`, `boolean signatureRequired`, `LocalDate signedDate`, `Long signedFileId`, `Long signedByFamilyMemberId`, `Long facilitySignatoryStaffId`.
- `internal/change/persistence/AddendumStore.java`: `insert(..., boolean signatureRequired, Status initial)` cấp `addendum_no = COALESCE(MAX(addendum_no), 0) + 1` của hợp đồng trong cùng transaction (hợp đồng đã bị `contracts.lock` ở `onApprove`, nên không tranh chấp); `markSigned(id, signedDate, fileId, familyMemberId, facilitySignatoryStaffId, recordedBy, at, note)` (chỉ khi `PENDING_SIGNATURE`, chuyển `PENDING_EFFECTIVE`); `postponeEffective(id, newDate)`; `markCancelled(id)`; truy vấn `pendingSignature()` cho job nhắc.
- `web/response/AddendumBody.java`, `web/response/ResidencyChangeRequestBody.java`: thêm `addendumNo`, `status` mới, `signedDate`, `signedByFamilyMemberId`, `facilitySignatoryStaffId`, `signedFileId`.
- **Mô tả lỗi:** mọi `UnmetCondition` mới (ở mọi bước) có `description` tiếng Việt nói rõ việc cần làm, vì web hiện thẳng `description` (`common/ProblemAlert.jsx`).
- **Ánh xạ trạng thái giữ như cũ:** CSDL và domain dùng `NOT_APPLIED`, API (`api-004.yaml`) dùng `NOT_APPLICABLE`. Chỉ thêm giá trị mới (`PENDING_SIGNATURE`, `CANCELLED` giống nhau ở hai phía); không đổi tên giá trị cũ.

### 3.3 `ResidencyChangeHandler`
- `onApprove`: `boolean minor = isMinor(request)`; insert phụ lục với `signatureRequired = !minor`, trạng thái đầu `minor ? PENDING_EFFECTIVE : PENDING_SIGNATURE`. Giữ giường như cũ (giữ lúc duyệt).
- Thêm `@Override readyToApply(request)`: `addenda.findByRequest(id).map(a -> a.status() != PENDING_SIGNATURE).orElse(true)`.
- `applyConditions`: chấp nhận phụ lục `PENDING_EFFECTIVE`; `PENDING_SIGNATURE` không tới được đây (đã chặn bằng `readyToApply`).
- `AddendumClosing.close`: lọc `PENDING_EFFECTIVE` **hoặc** `PENDING_SIGNATURE` → `NOT_APPLIED` (dùng cho Áp dụng không thành, thay thế, trạng thái cuối). Thêm `cancel(requestId, residentId, basis)` → `CANCELLED` (cùng các tác động nhả thuộc tính, hủy giữ giường, nhật ký `M02_ADDENDUM_CANCELLED`).
- `ResidencyChangeListeners.onCancelled`: gọi `closing.cancel(...)` thay `closing.close(...)`.

### 3.4 Lệnh Ghi nhận phụ lục đã ký
- Endpoint: `POST /api/v1/residency-change-requests/{id}/addendum-signature`, header `If-Match` theo yêu cầu, body `AddendumSignatureInput { LocalDate signedDate; long signedByFamilyMemberId; Long facilitySignatoryStaffId; long fileId; String note }` (`facilitySignatoryStaffId` tùy chọn; có thì phải là hồ sơ nhân viên đang làm việc) (`web/request/AddendumSignatureBody.java`), trả `ResidencyChangeRequestBody`. Đặt trong `web/ResidencyChangeRequestController.java`.
- `ResidencyChangeService.recordSignature(...)` `@Transactional`:
  1. Quyền: khóa `PermissionKey(Function.RESIDENCY_CHANGE_REQUEST, Operation.UPDATE)` hoặc khóa riêng nếu ma trận có; vai trò phải là `OFFICE_STAFF` hoặc `NURSING_HOME_MANAGER` (cùng cách kiểm ghi `representativeConfirmation` hiện có).
  2. `residencyLock.lock(residentId)`; yêu cầu `APPROVED`; phụ lục `PENDING_SIGNATURE` (khác → 409 `ADDENDUM_NOT_PENDING_SIGNATURE`).
  3. `signedDate` ≤ `time.today()` và ≥ ngày duyệt (khác → 422 `SIGNED_DATE_INVALID`).
  4. `RepresentativePort.isActiveRepresentative(familyMemberId, residentId, signedDate)` (sai → 409 `SIGNER_NOT_REPRESENTATIVE`).
  5. Gắn tệp vào hợp đồng loại `M02.CONTRACT_SCAN`: dùng đúng cách gắn tệp của `representativeConfirmation` hoặc bản scan hợp đồng (tìm trong `ResidencyChangeService`, `ContractService`).
  6. `addenda.markSigned(...)`.
  7. `newEffective = max(request.actualEffectiveDate(), signedDate)`; nếu `newEffective` sau ngày cũ: `approvals.postponeEffectiveDate(id, newEffective, reason "Ký phụ lục ngày …")`; `addenda.postponeEffective(addendumId, newEffective)`; nếu phụ lục có `BED` hoặc là giường đầu tiên (bán trú sang nội trú): `beds.cancelScheduledAllocation(requestId, reason)` rồi giữ lại bằng `reserveScheduledTransfer` / `reserveFirstAllocation` ở `newEffective` (chép logic ở `onApprove`; trước đó gọi `checkScheduledReservation`, vi phạm → 409 liệt kê điều kiện, rollback cả lệnh). Bỏ qua bước giường khi `afterUrgentTransfer`.
  8. Nhật ký `M02_ADDENDUM_SIGNED`.
- Ký trước ngày hiệu lực: không dời ngày.

### 3.5 Nhắc ký
- `internal/change/service/AddendumSignatureReminderTask.java` theo mẫu `ChangeConfirmationReminderTask.java` (dùng `SchedulerRuns`, `ReminderMarkers`); job lịch ở `admission/scheduling/` theo mẫu job có sẵn của module (mặc định `on-startup` là `${eldercare.scheduling.enabled:true}`).
- Mỗi lần chạy: với phụ lục `PENDING_SIGNATURE`, nhắc Hành chính khi đã qua CFG-M02-17 từ lần nhắc trước (sự kiện `AddendumSignatureReminder`, mức Nhẹ); khi `hôm nay − ngày duyệt ≥ CFG-M02-18` và chưa báo thì báo Quản lý viện một lần (`AddendumSignatureOverdue`, mức Trung bình; dấu `ReminderMarkers`).

**Test:** `FR_043d_AddendumSignatureIT` kế thừa `ChangeTestBase`:
(a) duyệt yêu cầu đổi mức → phụ lục `PENDING_SIGNATURE`;
(b) chạy job áp dụng ở ngày hiệu lực → yêu cầu vẫn `APPROVED`, phụ lục vẫn Chờ ký, không có `APPLY_FAILED`;
(c) ký trước ngày → áp dụng đúng ngày;
(d) ký sau ngày → ngày hiệu lực = ngày ký, chi phí trước ngày ký theo giá cũ (FR-044), đổi giường thì phân bổ tương lai dời theo;
(e) thay đổi nhỏ → `PENDING_EFFECTIVE` ngay, không cần ký;
(f) người ký không phải người đại diện → 409; ngày ký tương lai → 422; ký lần hai → 409;
(g) trigger: `UPDATE contract_version SET content_after=…` vẫn bị chặn; đổi `addendum_no` bị chặn;
(h) số phụ lục: hai yêu cầu cùng hợp đồng được duyệt → số 1, 2; phụ lục số 1 bị hủy → yêu cầu sau nhận số 3; migration điền đúng số cho phụ lục có sẵn (dùng `MySqlIntegrationTestBase.scratchFlyway` theo mẫu `TableConsolidationMigrationIT`);
(i) người ký phía viện ghi được và hiện ở phản hồi; mã nhân viên không tồn tại → 422.
`AddendumSignatureReminderIT` (nhắc theo chu kỳ, báo Quản lý viện đúng một lần). Sửa `ChangeTestBase` thêm helper `sign(requestId, date)`; sửa các IT hiện có đang duyệt rồi chờ áp dụng (ví dụ `BR_09_ApplyAddendumIT`, `FR_045_BedChangeApplyIT`, `ChangeArrivalScheduleIT`, `FR_041_DayCareToResidentialIT`, `FR_034_ResidenceTypeReassessIT`, IT luồng phụ lục của 005 T029) để **ký trước khi áp dụng**.

**Xong khi:** test trên và mọi IT của `admission` xanh.

---

## 5. Bước 4 – Trả về nháp, hủy, thông báo, nhiều người đại diện (`admission`)

| # | Thay đổi | File |
| - | -------- | ---- |
| 4.1 | `ResidencyChangeHandler.allowReturnToDraft()` → `true`; `onReturnToDraft(request)`: xóa `representativeConfirmation` khỏi nội dung (qua đường sửa nội dung nội bộ đang dùng khi sửa nháp, hoặc ghi dấu "đã bỏ" nếu nội dung không sửa được ở đây), `confirmations.supersedeAll(request.id())` (lần gửi xác nhận đang chờ hoặc đã đồng ý → Bị thay thế); phát `ResidencyChangeReturnedToDraft(requestId, residentId, requesterId)` để báo người lập và Hành chính | `ResidencyChangeHandler.java`, `ChangeConfirmations.java`, `api/events/` |
| 4.2 | `mayCancel(user, request)`: tài khoản người thân → chỉ khi `requesterId == accountId`; nhân viên → người lập, hoặc vai trò `OFFICE_STAFF`, `NURSING_HOME_MANAGER` | `ResidencyChangeHandler.java` |
| 4.3 | Thông báo người đại diện: sự kiện `ResidencyChangeOutcomeNotice(requestId, residentId, outcome: REJECTED / APPLY_FAILED / CANCELLED, at)`. Phát ở `ResidencyChangeListeners.onDecided` (REJECTED), `onCancelled`, và nhánh lỗi của `checkApplyConditions` (APPLY_FAILED; module không nghe được `RequestApplyFailed`). Chỉ phát khi yêu cầu có loại thay đổi khác thay đổi nhỏ hoặc do người đại diện lập | `ResidencyChangeListeners.java`, `ResidencyChangeHandler.java`, `api/events/ResidencyChangeOutcomeNotice.java` |
| 4.3a | **Người đại diện đã xác nhận (R16):** `internal/change/domain/RepresentativeConfirmation.java` thêm `long representativeFamilyMemberId` (đọc, ghi trong `of(...)`, `toJson`/nơi `node.put`); nơi nhận `representativeConfirmation` từ web (`web/request/ResidencyChangeRequestInputBody.java` hoặc body xác nhận riêng, tra `ConfirmationSheet` của web để biết lệnh nào) bắt buộc trường này; kiểm `RepresentativePort.isActiveRepresentative(id, residentId, confirmedOn)`, sai → 409 `CONFIRMER_NOT_REPRESENTATIVE`. Xác nhận cũ trong dữ liệu không có trường này: đọc được (`null`), nhưng `checkOnSubmit` coi là thiếu xác nhận → phải xác nhận lại. Phản hồi (`ResidencyChangeRequestBody`) hiện trường này | `RepresentativeConfirmation.java`, `ResidencyChangeService.java`, `web/request/`, `web/response/` |
| 4.4 | Nhiều người đại diện: `ResidencyChangeSubmittedNotice` khi gửi duyệt (trong `checkOnSubmit` không phát được vì chưa chắc gửi thành công; phát từ listener `RequestSubmitted` của 000 nếu có, không có thì thêm hook `onSubmit` vào SPI ở Bước 2); `AddendumAppliedNotice` gửi tới **mọi** người đại diện hiệu lực (sửa người nhận ở nơi ánh xạ thông báo) | `admission/api/events/`, nơi khai người nhận của `AddendumAppliedNotice` |

Người nhận `ResidencyChangeSubmittedNotice`: mọi người đại diện hiệu lực **trừ** `representativeFamilyMemberId` của xác nhận 1a và người đã Đồng ý ở 1b (R16).

**Test:** `FR_043b_RepresentativeConfirmationIT` thêm ca: thiếu người đại diện → 422; người không phải người đại diện → 409 `CONFIRMER_NOT_REPRESENTATIVE`; xác nhận cũ không có trường → gửi duyệt bị 409 `REPRESENTATIVE_CONFIRMATION_REQUIRED`. `FR_043e_ReturnToDraftIT` (Quản lý viện trả về → Nháp, xác nhận bị bỏ, gửi duyệt lại → 409 `REPRESENTATIVE_CONFIRMATION_REQUIRED`; Hành chính trả về yêu cầu không nhỏ → 403); `FR_043f_CancelIT` (từng trạng thái, từng người; phụ lục Chờ ký, Chờ hiệu lực → `CANCELLED`; giữ giường bị hủy; thuộc tính được nhả, lập yêu cầu cùng loại mới được); `FR_043g_RepresentativeNoticesIT` (từ chối, hủy, áp dụng không thành, hai người đại diện cùng nhận).

---

## 6. Bước 5 – Ý kiến người cao tuổi và xác nhận lại (`admission`)

### 5.1 Migration (thêm vào `V004_07` hoặc `V004_09__residency_change_opinion.sql`)
```sql
CREATE TABLE residency_change_resident_opinion (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    approval_request_id BIGINT     NOT NULL,
    resident_id       BIGINT       NOT NULL,
    opinion           ENUM ('AGREE','DISAGREE','UNABLE_TO_EXPRESS') NOT NULL,
    source            ENUM ('RESIDENT','STAFF_OBSERVATION') NOT NULL,
    note              VARCHAR(1000) NULL,
    content_digest    CHAR(64)     NOT NULL,
    recorded_by       BIGINT       NOT NULL,
    recorded_at       DATETIME(6)  NOT NULL,
    KEY ix_rcro_request (approval_request_id, recorded_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'DataGroup=CONFIRMED_RECORD';
CREATE TABLE residency_change_portal_digest (
    approval_request_id BIGINT     NOT NULL PRIMARY KEY,
    digest            CHAR(64)     NOT NULL,
    created_at        DATETIME(6)  NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'DataGroup=CONFIRMED_RECORD';
```
Dùng đúng giá trị `DataGroup` mà các bảng nhóm 3 khác của module đang dùng (tra `@DataGroup` hoặc `COMMENT` trong `V004_*`). Bảng nhóm 3: thêm trigger cấm UPDATE, DELETE theo mẫu R-15 (`trg_<bảng>_row_lock`, `trg_<bảng>_no_delete`); đăng ký `RetentionProvider` nếu module dùng cho bảng con của yêu cầu (theo mẫu bảng có sẵn).

### 5.2 Ý kiến người cao tuổi (R11)
- Endpoint `POST /api/v1/residency-change-requests/{id}/resident-opinions` body `{opinion, source, note}`; `GET` cùng đường dẫn trả danh sách. Yêu cầu ở Nháp hoặc Chờ duyệt. Quyền: Hành chính, Điều dưỡng, Bác sĩ có phạm vi với người cao tuổi (dùng `Target` của `scope(...)` của handler).
- `ResidencyChangeRequestBody` thêm `currentResidentOpinion` (bản mới nhất) và `warnings` (mảng mã).
- `checkOnSubmit(request, reason)` (overload mới): nếu không phải thay đổi nhỏ và ý kiến hiện hành `DISAGREE` và `reason` rỗng → `UnmetCondition("RESIDENT_DISAGREE_REASON_REQUIRED", …)`. Thiếu ý kiến **không** là điều kiện; controller gửi duyệt của 004 (hoặc phản hồi chi tiết) trả `warnings: ["RESIDENT_OPINION_MISSING"]`.
- **Dấu nội dung (R18):** khi ghi ý kiến, lưu `content_digest` = digest phần thực chất của nội dung yêu cầu lúc đó (cùng hàm digest ở 5.3; tách hàm thành `ResidencyChangeDigest.of(ResidencyChangeContent)` dùng chung). Khi đọc chi tiết và khi gửi duyệt: ý kiến hiện hành có digest khác digest hiện tại → thêm `RESIDENT_OPINION_OUTDATED` vào `warnings`. `ResidentOpinion` trong phản hồi có thêm `outdated: boolean`.

### 5.3 Xác nhận lại yêu cầu qua cổng (R12)
- Khi tài khoản người thân tạo yêu cầu: tính `digest = sha256(JSON chuẩn hóa các trường thực chất)` (loại thay đổi, giá trị mới, `requestedEffectiveDate`, `newBedId`, lịch đến, danh sách dịch vụ có `action`) và lưu `residency_change_portal_digest`.
- `confirmationRequired(request)`: người yêu cầu là người thân → cần xác nhận khi `digest(hiện tại) != digest đã lưu`; không có dòng digest (yêu cầu cũ) → không cần (giữ hành vi cũ).
- Kiểm hồi quy: sửa nháp sau khi đã có xác nhận thì xác nhận bị bỏ (D đã cài; thêm test nếu chưa có).

**Test:** `FR_043h_ResidentOpinionIT` (Điều dưỡng ghi được, Kế toán 403; thiếu ý kiến → gửi được, có cảnh báo; `DISAGREE` không lý do → 409, có lý do → gửi được; thay đổi nhỏ không cảnh báo; ghi ý kiến rồi đổi giường → cảnh báo `RESIDENT_OPINION_OUTDATED`, `outdated = true`; chỉ đổi ghi chú → không cảnh báo); `FR_043i_PortalReconfirmIT` (Hành chính đổi ngày hoặc giường → 409 `REPRESENTATIVE_CONFIRMATION_REQUIRED`; chỉ đổi ghi chú → gửi được; có xác nhận rồi → gửi được).

### 5.4 Thay đổi nhỏ do hệ thống xác định (R19)
- `internal/change/service/IsMinorChange.java`: nhánh `case ADD` không dùng `line.inPackage()` nữa, mà dùng `currentLine(currentContent, line.serviceId()).map(current -> current.path("inPackage").asBoolean(false)).orElse(false)` (giống nhánh `REMOVE`). Sửa Javadoc cho đúng.
- `internal/change/domain/ResidencyChangeContent.java`: vẫn đọc được `inPackage` trong nội dung cũ (không làm 422), nhưng không dùng để quyết thay đổi nhỏ. Tìm mọi nơi khác đọc `ServiceChange.inPackage()` (ví dụ `AddendumContent` khi dựng nội dung hợp đồng mới): dòng thêm lấy dấu thuộc gói từ dòng cùng dịch vụ của hợp đồng hiện hành; không có thì `false`.
- `api-004.yaml`: đánh dấu `inPackage` của dòng dịch vụ trong `ResidencyChangeRequestInput` là `deprecated: true`, mô tả "bỏ qua, hệ thống tự xác định" (không xóa để client cũ không lỗi).
- **Test:** `FR_043_MinorChangeIT` thêm ca: thêm dịch vụ chưa có trong hợp đồng kèm `inPackage: true` → **không** phải thay đổi nhỏ (Hành chính duyệt bị chặn, cần xác nhận người đại diện, phụ lục Chờ ký); thêm lại dịch vụ mà hợp đồng có dòng thuộc gói → thay đổi nhỏ. Sửa ca cũ đang dựa vào `inPackage: true` của dòng thêm.

### 5.4a Loại dịch vụ được thêm (R21)
- `internal/change/service/ResidencyChangeRules.java` (chỗ đang kiểm dòng dịch vụ, gần dòng chặn `"CARE"` hiện có, ~229–245) và `validateContent` của `ResidencyChangeHandler`: dòng `ADD` có `ServiceKind.type()` không thuộc `{REHABILITATION, ACTIVITY, MEDICAL_ESCORT, INJECTION}` → 422 `SERVICE_TYPE_NOT_REGISTRABLE`, `description` tiếng Việt chỉ đúng luồng cần dùng theo loại (ví dụ `CONSUMABLE`: "Tã, bỉm, sữa tính theo lần dùng hoặc lập đề nghị mua hộ, không đăng ký trong hợp đồng"; `CARE`: "Đổi dịch vụ chăm sóc bằng loại thay đổi Mức chăm sóc"; `RESIDENCY_FEE`: "Đổi phí lưu trú bằng loại thay đổi Giường"). Giữ chặn `CARE` hiện có, gộp vào quy tắc này. Dòng `REMOVE` không kiểm loại.
- Kiểm cũng chạy lại khi gửi duyệt và khi duyệt (yêu cầu cũ đã lập trước khi có quy tắc).
- Danh sách 4 loại đặt một chỗ (hằng `REGISTRABLE_SERVICE_TYPES` trong `admission`), không rải nhiều nơi.
- **Chỉ kiểm, không sửa** `billing`: đọc nơi sinh phí cho công việc dịch vụ và điểm danh hoạt động (15.2) để xác nhận dịch vụ thuộc gói không sinh phí riêng; nếu không đúng thì **ghi vào báo cáo**, không sửa (ngoài mục 0a).
- **Dữ liệu cũ:** viết truy vấn chỉ đọc liệt kê hợp đồng có dòng dịch vụ thuộc loại không được phép (đặc biệt `CONSUMABLE`, `FACILITY_SUPPLIED_MEDICATION`, `MEAL`); đưa kết quả vào báo cáo, **không tự sửa dữ liệu**.
- **Form lập hợp đồng (BF-01)** dùng chung danh mục có thể có cùng lỗi: chỉ kiểm và ghi vào báo cáo, không sửa (mục 0a: không sửa hợp đồng gốc).
- **Test:** `FR_043k_RegistrableServiceTypeIT`: thêm Tã người lớn, Paracetamol nguồn viện, Suất ăn, Phí lưu trú, Chăm sóc cơ bản → 422 `SERVICE_TYPE_NOT_REGISTRABLE` với mô tả đúng luồng; thêm Phục hồi chức năng, Hoạt động ngoài trời, Đưa đi khám, Tiêm theo y lệnh → lập được; bớt một dòng `CONSUMABLE` đang có trong hợp đồng → lập được.

### 5.5 Mức chăm sóc mới lấy từ đánh giá (R20)
- `resident.api.CareLevelApi` (hoặc API đánh giá có sẵn của 001; tra `resident/api/`): cần hàm đọc mức của lần đánh giá gần nhất đã xác nhận, ví dụ `Optional<AssessedLevel> latestConfirmedAssessment(long residentId)` trả `{assessmentId, careLevel, confirmedAt, doctorStaffId}`. Có sẵn thì dùng; chưa có thì thêm (module `resident` cùng chủ với `admission`), cập nhật bản giả ở `support/fake/resident`.
- `ResidencyChangeRules` (nơi kiểm FR-041 khi duyệt) và `validateContent` / `checkOnSubmit` của `ResidencyChangeHandler`: yêu cầu có `CARE_LEVEL` thì `change.careLevel()` phải bằng mức đánh giá gần nhất, và mức đó khác mức của nội dung hợp đồng hiện hành; sai → `UnmetCondition("CARE_LEVEL_NOT_FROM_ASSESSMENT", …)` (lập, sửa: 409 qua `validateContent`; gửi duyệt, duyệt: liệt kê cùng điều kiện khác).
- Phản hồi `ResidencyChangeRequestBody` thêm `assessedCareLevel { assessmentId, careLevel, confirmedAt }` để web điền sẵn và hiện nguồn.
- **Test:** `FR_043j_CareLevelFromAssessmentIT`: mức khác mức đánh giá → 409; chưa có đánh giá lại khác mức hợp đồng → 409; đúng mức → lập được; đánh giá lại mới (mức khác) sau khi lập → gửi duyệt bị 409 (yêu cầu cũ thường đã Được thay thế, ca này kiểm khi thay thế không chạy được). Sửa các IT đổi mức đang lập yêu cầu không có đánh giá lại (`ChangeTestBase`: thêm helper tạo đánh giá lại đã xác nhận trước khi lập yêu cầu `CARE_LEVEL`).

---

## 7. Bước 6 – Quyết định khi gia đình không đồng ý đổi mức (`resident`, đọc `care`)

Gốc: `backend/src/main/java/com/eldercare/backend/resident/`.

| # | Thay đổi |
| - | -------- |
| 6.1 | Migration `V001_<số kế tiếp>__care_level_change_decision.sql` (tra số lớn nhất của `V001_*`): bảng `care_level_change_decision(id, resident_id, assessment_id, decision ENUM('KEEP_CURRENT_LEVEL','LEAVE_FACILITY'), risk_agreement_id BIGINT NULL, review_date DATE NULL, note VARCHAR(1000), recorded_by, recorded_at, review_notified_at DATETIME(6) NULL)`, `UNIQUE(assessment_id)`, `CHECK (decision <> 'KEEP_CURRENT_LEVEL' OR (risk_agreement_id IS NOT NULL AND review_date IS NOT NULL))`; nhóm 3, trigger cấm sửa trừ cột `review_notified_at` |
| 6.2 | `care.api`: thêm `CareRiskAgreementApi.isActive(long residentId, long agreementId, LocalDate at)` cài bằng `CareRiskService` / `CareRiskStore` (đọc `care/internal/careplan/`); bản giả ở `support/fake/care` |
| 6.3 | Endpoint `POST /api/v1/residents/{residentId}/care-level-change-decisions` body `{assessmentId, decision, riskAgreementId, reviewDate, note}`; quyền Hành chính (khóa của "Yêu cầu thay đổi lưu trú" T). Kiểm: lần đánh giá là lần gần nhất đã xác nhận, đang có nhắc lập yêu cầu đổi mức (`CareLevelChangeRequestReminderService`); chưa có yêu cầu đổi mức đang sống (Nháp, Chờ duyệt, Đã duyệt) cho lần đánh giá đó, có thì 409 `CHANGE_REQUEST_EXISTS` (phải hủy trước); `KEEP_CURRENT_LEVEL` cần `riskAgreementId` hiệu lực (6.2) và `reviewDate` sau hôm nay |
| 6.4 | Tác động: `CareLevelChangeRequestReminderService.stopReminding(residentId, assessmentId)`; `LEAVE_FACILITY` phát `CareLevelChangeDeclinedLeave(residentId)` để báo Hành chính lập hồ sơ kết thúc lưu trú |
| 6.5 | Nhắc ngày xem lại: mở rộng `RemindCareLevelChangeRequestJob` (hoặc job mới cùng gói `resident/scheduling/`): `review_date ≤ hôm nay` và `review_notified_at IS NULL` → phát `CareLevelKeepReviewDue` (Bác sĩ, Hành chính, mức Nhẹ), đặt `review_notified_at` |
| 6.6 | `AssessmentService` khi xác nhận đánh giá lại mới: quyết định của lần trước không còn tác dụng (tự nhiên, vì quyết định gắn `assessment_id`); nhắc mới chạy theo lần đánh giá mới (đã có) |

**Test:** `CareLevelKeepDecisionIT` (dừng nhắc; thiếu thỏa thuận → 409; tới ngày xem lại nhắc một lần; đánh giá lại mới → nhắc lại); `CareLevelLeaveDecisionIT`.

---

## 8. Bước 7 – Thông báo (`notification`, danh mục loại)

Khai các loại sau theo cách module nguồn đang khai loại thông báo (tra cách `RESIDENCY_CHANGE_CONFIRMATION_REQUESTED` của FR-043c đã được khai và ánh xạ sự kiện → người nhận; làm y hệt):

| Mã | Sự kiện | Người nhận | Mức | Nguồn mở được |
| -- | ------- | ---------- | --- | ------------- |
| `ADDENDUM_SIGNATURE_REMINDER` | `AddendumSignatureReminder` | Hành chính | Nhẹ | `M02.RESIDENCY_CHANGE` |
| `ADDENDUM_SIGNATURE_OVERDUE` | `AddendumSignatureOverdue` | Quản lý viện | Trung bình | `M02.RESIDENCY_CHANGE` |
| `RESIDENCY_CHANGE_RETURNED_TO_DRAFT` | `ResidencyChangeReturnedToDraft` | Người lập, Hành chính | Nhẹ | `M02.RESIDENCY_CHANGE` |
| `RESIDENCY_CHANGE_OUTCOME` | `ResidencyChangeOutcomeNotice` | Mọi người đại diện hiệu lực | Nhẹ, không kèm giá | `M02.RESIDENCY_CHANGE` (app người thân) |
| `RESIDENCY_CHANGE_SUBMITTED` | `ResidencyChangeSubmittedNotice` | Người đại diện hiệu lực (trừ người đã xác nhận) | Nhẹ | `M02.RESIDENCY_CHANGE` |
| `CARE_LEVEL_KEEP_REVIEW_DUE` | `CareLevelKeepReviewDue` | Bác sĩ, Hành chính | Nhẹ | `M01.RESIDENT` |
| `CARE_LEVEL_DECLINED_LEAVE` | `CareLevelChangeDeclinedLeave` | Hành chính | Nhẹ | `M01.RESIDENT` |

Đối tượng thông tin tới người thân: "về việc của chính người thân" (spec 004 FR-078). Không gửi trong giờ yên tĩnh (không phải mức Khẩn cấp). Test: thêm ca vào IT danh mục thông báo của 009 nếu có (tìm IT kiểm mọi loại có ánh xạ).

---

## 9. Bước 8 – Không báo lặp xem xét kế hoạch (`care`)

- `care/internal/careplan/service/CarePlanReviewListener.java`, `onAddendum(AddendumApplied event)`: trước `review(...)`, kiểm: đã có mốc báo từ nguồn `M01.ASSESSMENT` cho người cao tuổi này (bảng của `ReminderMarkers` / outbox mà `review` ghi) **và** có phiên bản kế hoạch chăm sóc tạo sau thời điểm mốc đó (tra `CarePlanQueries` hoặc store phiên bản kế hoạch trong `care/internal/careplan/`) → `return`. Không đổi api của module khác.
- Kiểm: lập phiên bản kế hoạch mới khi phụ lục chưa áp dụng không bị chặn bởi mức hiện hành (đọc service lập phiên bản; có chặn theo mức của hợp đồng thì bỏ chặn đó và ghi lý do vào plan 005).
- **Test:** `CarePlanReviewDedupIT`: (1) đánh giá lại đổi mức → báo; lập phiên bản kế hoạch → áp dụng phụ lục → **không** báo thêm; (2) đánh giá lại → không lập phiên bản → áp dụng phụ lục → có báo.

---

## 10. Bước 9 – Chênh lệch giá khi chuyển giường gấp (`billing`) – **Đợt 2, không làm ở Đợt 1**

- Tìm nơi `billing` tính phí lưu trú theo loại phòng của phân bổ (nguồn sự kiện tính phí `RESIDENCY_FEE`, `billing/internal/source/`; tra `CostSourceChangeEvents`).
- Phân bổ mang dấu chuyển gấp (003 FR-030; tra trường dấu trong `BedAllocationView` hoặc API 003 hiện có) và chưa có phụ lục đổi phòng Đã áp dụng: đơn giá dùng = `min(đơn giá loại phòng cũ, đơn giá loại phòng mới)` tại ngày tính. Từ ngày hiệu lực của phụ lục đổi phòng thì theo phụ lục.
- **Test:** `UrgentTransferFeeIT` (lên phòng đắt → tính giá cũ tới ngày phụ lục áp dụng; xuống phòng rẻ → giá mới từ ngày chuyển).

---

## 11. Bước 10 – Web và app

### Web (`Elder_Care_Frontend`)
Trước khi sửa: `npm run gen:api` (kho Elder_Care đặt cạnh). Sửa bố cục theo DESIGN.md mục 0; không đổi mẫu màn.

| # | Việc | File |
| - | ---- | ---- |
| W1 | Nhãn trạng thái phụ lục: Chờ ký, Chờ hiệu lực, Đã áp dụng, Không áp dụng, Đã hủy | `src/features/admissions/changeRequestRules.js`, màn Hợp đồng (danh sách phụ lục) |
| W2 | Hộp thoại **Ghi nhận phụ lục đã ký**: ngày ký (mặc định hôm nay, không cho ngày tương lai), chọn người đại diện (từ người đại diện hiệu lực của người cao tuổi), tải tệp PDF/JPG/PNG, ghi chú; hiện cho Hành chính, Quản lý viện khi phụ lục Chờ ký; sau khi lưu nếu ngày hiệu lực bị dời thì báo ngày mới | `src/features/admissions/ChangeParts.jsx` |
| W3 | Trang **in phụ lục** (dùng `window.print()`, CSS in): người cao tuổi, hợp đồng, bảng giá trị trước / sau, đơn giá, ngày hiệu lực dự kiến, chỗ ký hai bên | `src/features/admissions/AddendumPrint.jsx` (mới) |
| W4 | Mục **Ý kiến người cao tuổi** ở chi tiết yêu cầu: lịch sử, nút ghi ý kiến (Hành chính, Điều dưỡng, Bác sĩ); khi Gửi duyệt: thiếu ý kiến thì hỏi xác nhận "Chưa có ý kiến người cao tuổi, vẫn gửi?"; ý kiến Không đồng ý thì bắt nhập lý do | `ChangeParts.jsx`, `ChangeRequestsPage.jsx` |
| W5 | Nút **Trả về nháp** (lý do) cho người duyệt, ở màn Thay đổi lưu trú và màn Yêu cầu phê duyệt | `ChangeRequestsPage.jsx`, `src/features/approval-requests/…` |
| W6 | Nút **Hủy** theo R9 | `ChangeRequestsPage.jsx` |
| W7 | Hộp thoại **Quyết định khi gia đình không đồng ý đổi mức** ở nơi hiện nhắc "cần lập yêu cầu đổi mức" của hồ sơ người cao tuổi: chọn Giữ mức cũ (chọn thỏa thuận lựa chọn có rủi ro, ngày xem lại) hoặc Rời viện | `src/features/residents/…` (tìm nơi hiển thị nhắc `CareLevelChangeRequestNeeded`) |
| W2a | Hộp thoại W2 thêm ô **Người ký phía viện** (chọn nhân viên, không bắt buộc); bản in W3 và chi tiết phụ lục hiện **Số phụ lục** ("Phụ lục số 02") và người ký phía viện | `ChangeParts.jsx`, `AddendumPrint.jsx`, màn Hợp đồng |
| W2b | `ConfirmationSheet` (xác nhận 1a): thêm ô bắt buộc **Người đại diện đã xác nhận** (chọn trong người đại diện hiệu lực của người cao tuổi); chi tiết yêu cầu hiện tên người này | `ChangeParts.jsx` (`ConfirmationSheet`) |
| W4a | Ý kiến người cao tuổi có `outdated` thì hiện nhãn "Ghi trước khi nội dung thay đổi" và gợi ý ghi lại; xử lý cảnh báo `RESIDENT_OPINION_OUTDATED` khi gửi duyệt như `RESIDENT_OPINION_MISSING` | `ChangeParts.jsx` |
| W9 | **Form lập yêu cầu (`ChangeRequestSheet`)**: (a) **bỏ ô "thuộc gói"** ở dòng thêm dịch vụ và bỏ `inPackage` khỏi dữ liệu gửi; hiện nhãn chỉ đọc "Thuộc gói" / "Ngoài gói" theo hợp đồng hiện hành; (b) **Mức chăm sóc mới**: khi tick "Mức chăm sóc", điền sẵn mức của lần đánh giá gần nhất đã xác nhận, không cho sửa, kèm dòng "Theo đánh giá của BS … ngày …"; chưa có đánh giá lại khác mức hợp đồng thì ô loại "Mức chăm sóc" bị tắt kèm lý do; (c) **Loại thay đổi**: chọn loại lưu trú mới thì tự tick và hiện ô bắt buộc tương ứng (sang bán trú → Lịch đến; bán trú sang nội trú → Giường; sang nội trú ngắn ngày → Thời hạn), bỏ tick loại kèm khi đổi lựa chọn | `ChangeParts.jsx` (`ChangeRequestSheet`), `changeRequestRules.js` |
| W10 | **Danh sách yêu cầu** (`ChangeRequestsPage`): trạng thái hiển thị gộp yêu cầu + phụ lục (Đã duyệt mà phụ lục Chờ ký → "Đã duyệt · Chờ ký", kèm số ngày từ lúc duyệt; quá CFG-M02-18 thì tô cảnh báo); thêm bộ lọc nhanh "Chờ ký" để Hành chính làm việc theo danh sách; cột "Số phụ lục" khi đã có | `ChangeRequestsPage.jsx`, `changeRequestRules.js` |
| W11 | **Cảnh báo dạng mã** từ phản hồi (`warnings`): thêm bảng nhãn tiếng Việt trong `changeRequestRules.js` (`RESIDENT_OPINION_MISSING` → "Chưa ghi ý kiến người cao tuổi", `RESIDENT_OPINION_OUTDATED` → "Ý kiến người cao tuổi ghi trước khi nội dung thay đổi"); hiện ở chi tiết yêu cầu và ở bước xác nhận Gửi duyệt. Lỗi 409 (`unmetConditions`) không cần bảng nhãn: `common/ProblemAlert.jsx` đã hiện `description` của backend, nên mọi `UnmetCondition` mới ở backend phải có `description` tiếng Việt rõ việc cần làm | `changeRequestRules.js`, `ChangeParts.jsx` |
| W12 | **Mở từ thông báo**: thông báo nguồn `M02.RESIDENCY_CHANGE` (nhắc ký, quá hạn ký, trả về nháp, kết quả) mở đúng yêu cầu ở màn Thay đổi lưu trú (tra cách `common/notifications/NotificationDetail.jsx` mở nguồn của loại `RESIDENCY_CHANGE_CONFIRMATION_*` và làm giống); nguồn `M01.RESIDENT` (`CARE_LEVEL_KEEP_REVIEW_DUE`, `CARE_LEVEL_DECLINED_LEAVE`) mở hồ sơ người cao tuổi. Màn Thay đổi lưu trú nhận tham số chọn sẵn yêu cầu (ví dụ `?id=`) nếu chưa có | `common/notifications/NotificationDetail.jsx`, `ChangeRequestsPage.jsx` |
| W13 | **Hiện nút theo quyền và trạng thái** qua `usePermissions` (như form hiện dùng `can(...)`): Ghi nhận đã ký, Trả về nháp, Hủy, Ghi ý kiến người cao tuổi, Quyết định giữ mức; nút không có quyền thì ẩn, không hiện rồi báo 403. Đăng ký đường dẫn trang in phụ lục (W3) trong `App.jsx` cạnh các route của `features/admissions` | `App.jsx`, `ChangeParts.jsx`, `ChangeRequestsPage.jsx` |
| W14 | Hộp thoại W7 cần **danh sách thỏa thuận lựa chọn có rủi ro đang hiệu lực** của người cao tuổi: dùng endpoint có sẵn của 005 (tra `features/` phần kế hoạch chăm sóc đang đọc thỏa thuận ở đâu); chưa có endpoint đọc theo người cao tuổi thì thêm `GET /residents/{residentId}/care-risk-agreements?status=ACTIVE` ở `care` (Bước 6.2) và `api-005.yaml`. Không có thỏa thuận nào thì hiện hướng dẫn "Điều dưỡng lập thỏa thuận lựa chọn có rủi ro trong kế hoạch chăm sóc, Bác sĩ xác nhận" và tắt lựa chọn Giữ mức cũ | `features/residents/…` |
| W15 | **Danh sách dịch vụ ở form (R21):** phần "Dịch vụ thêm hoặc bớt" chỉ hiện để **thêm** các dịch vụ thuộc 4 loại `REHABILITATION`, `ACTIVITY`, `MEDICAL_ESCORT`, `INJECTION` (lọc theo `type` của danh mục), nhóm theo loại với tiêu đề tiếng Việt (Phục hồi chức năng; Hoạt động; Đưa đi khám; Tiêm). Dòng đang có trong hợp đồng thuộc **mọi** loại vẫn hiện để **bớt**. Sửa câu hướng dẫn: "Chỉ đăng ký phục hồi chức năng, hoạt động dài hạn, đưa đi khám, tiêm. Mức chăm sóc đổi ở loại Mức chăm sóc; phí lưu trú đổi ở loại Giường; tã, bỉm, thuốc, suất ăn không đăng ký ở đây." | `ChangeParts.jsx` (`ChangeRequestSheet`), `changeRequestRules.js` |
| W8 | Test Vitest cho W2, W2a, W2b, W4, W4a, W5, W6, W7, W9 → W15; mở rộng `ChangeRequestsPage.test.jsx` | |

Thứ tự ở web: (1) gộp hợp đồng API ở kho Elder_Care, (2) `npm run gen:api`, (3) W9 (form, vì sửa lỗi đang có), (4) W1, W10, W11, (5) W2, W2a, W2b, W3, W13, (6) W4, W4a, W5, W6, (7) W7, W14, W12, (8) W8. Không sửa tay `src/api/` (sinh từ hợp đồng).

Kiểm: `npm run lint`, `npm test`, `npm run build`.

### App (`Elder_Care_Mobile`)
| # | Việc | File |
| - | ---- | ---- |
| M1 | Sau khi người đại diện Đồng ý: dòng "Viện sẽ hẹn bạn ký phụ lục" | `lib/family/admissions/change_confirmation_screen.dart` |
| M1a | Màn đề nghị của người đại diện (`change_request_screen.dart`): bỏ ô "thuộc gói" nếu có; không cho chọn loại "Mức chăm sóc" (người đại diện không chọn mức, mức theo đánh giá của Bác sĩ) | `lib/family/admissions/change_request_screen.dart` |
| M2 | Màn hợp đồng: danh sách phụ lục có số phụ lục và trạng thái mới; lấy dữ liệu qua API cổng người thân (`GET /family-portal/residents/{residentId}/contract` hoặc endpoint phụ lục của cổng nếu có), không dùng `GET /contracts` (người thân bị 403) | `lib/family/admissions/contract_screen.dart` |
| M3 | Mở được thông báo `RESIDENCY_CHANGE_OUTCOME`, `RESIDENCY_CHANGE_SUBMITTED`, `ADDENDUM_APPLIED` về đúng màn | điều hướng thông báo ở `lib/family/` |
| M4 | Test widget với bản giả | `test/` |

Kiểm: `flutter analyze`, `flutter test`.

---

## 12. Bảng test và lệnh kiểm

| Quy tắc | Test |
| ------- | ---- |
| R1, R2 | `CarePlanReviewDedupIT` |
| R3–R6 | `FR_043d_AddendumSignatureIT` |
| R7 | `AddendumSignatureReminderIT` |
| R8 | `ApprovalReturnToDraftIT`, `FR_043e_ReturnToDraftIT` |
| R9 | `ApprovalCancelPermissionIT`, `FR_043f_CancelIT` |
| R10 | `FR_043g_RepresentativeNoticesIT` |
| R11 | `FR_043h_ResidentOpinionIT` |
| R12 | `FR_043i_PortalReconfirmIT` |
| R13 | `CareLevelKeepDecisionIT`, `CareLevelLeaveDecisionIT` |
| R14 | `UrgentTransferFeeIT` |
| R15 | `FR_070_FinalStateClosingIT` (thêm ca phụ lục Chờ ký) |
| R16 | `FR_043b_RepresentativeConfirmationIT` (ca mới), `FR_043g_RepresentativeNoticesIT` |
| R17 | `FR_043d_AddendumSignatureIT` (h), (i) |
| R18 | `FR_043h_ResidentOpinionIT` |
| R19 | `FR_043_MinorChangeIT` (ca mới) |
| R20 | `FR_043j_CareLevelFromAssessmentIT` |
| R21 | `FR_043k_RegistrableServiceTypeIT` |
| 000 | `RequestTransitionTableTest`, `ApplyTransactionNotReadyIT`, `PostponeEffectiveDateIT` |

Lệnh (thư mục `backend/`; PowerShell dùng `.\mvnw.cmd` và đặt mỗi `-D…` trong ngoặc kép):
1. `./mvnw spotless:apply`
2. Theo bước: `./mvnw verify -Dit.test="<các IT của bước>,FR_043_MinorChangeIT,FR_043b_RepresentativeConfirmationIT,FR_043c_PortalConfirmationIT,FR_045_BedChangeApplyIT,BR_09_ApplyAddendumIT,FR_070_FinalStateClosingIT,ApiContractIT" -Dtest="RequestTransitionTableTest,*ArchTest,ModuleBoundaryTest" -Dsurefire.failIfNoSpecifiedTests=false`
3. Cuối cùng (Bước 2 đụng 000, bắt buộc): nạp `backend/.env` rồi `./mvnw clean verify -Dit.forkCount=3` (khoảng 20 phút). So lớp đỏ với danh sách "đang đỏ có sẵn trên develop" ở `docs/team-plan.md` mục 8; chỉ lớp đỏ mới do thay đổi này mới phải sửa.

Cần MySQL đang chạy và biến `TEST_DB_URL`, `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD`, `DB_PASSWORD`, `DB_RETENTION_PASSWORD`. Không chạy được thì nói rõ test nào chưa chạy; không đánh `[x]` task đó.

### Chạy tay trên profile `demo` (đồng hồ giả lập)
Bác sĩ đánh giá lại lên Đặc biệt → Điều dưỡng nhận báo xem xét kế hoạch → Điều dưỡng lập phiên bản kế hoạch → Hành chính lập yêu cầu đổi mức, ghi ý kiến người cao tuổi, gửi xác nhận trên app → người đại diện Đồng ý → Hành chính gửi duyệt → Quản lý viện Trả về nháp → Hành chính gửi xác nhận lại, gửi duyệt → Quản lý viện duyệt → phụ lục Chờ ký → tua đồng hồ qua ngày hiệu lực: chưa áp dụng, Hành chính nhận nhắc → ghi nhận ký muộn → ngày hiệu lực dời → tua tới ngày mới: áp dụng, người đại diện nhận báo, Điều dưỡng **không** nhận báo lần hai.

---

## 13. Kết thúc

- `specs/004-admission-residency/tasks.md` Phase 14 (và task tương ứng ở 000, 001, 005, 010): đánh `[x]` task có test đã chạy xanh.
- Thêm **một dòng cuối** `docs/progress.md` (chỉ thêm, không sửa dòng cũ): ngày, "BF-08 sửa đổi (Q-415 → Q-426)", nhánh của ba kho, kết quả test (số test, lớp đỏ còn lại nếu có), việc còn lại.
- `docs/team-plan.md` mục 8: sửa hai dòng "Chủ dự án, B (004)" ngày 2026-11-18 và 2026-11-20 thành đã xong (ghi kết quả ở progress.md theo quy định đầu mục 8), bỏ chữ "chờ chủ dự án duyệt".
- Báo lại cho người dùng: danh sách file đã sửa ở từng kho, test đã chạy và kết quả, chỗ lệch so với plan này, và **mọi file ngoài mục 0a đã phải dừng không sửa** (kèm đề xuất).
- Kiểm phạm vi trước khi báo xong: `git status --short` ở ba kho; mọi file thay đổi phải thuộc bảng mục 0a.

---

## Phụ lục A – Nội dung BF-08 mới (dán vào `luong-nghiep-vu.md`)

```markdown
## BF-08 – Thay đổi lưu trú (yêu cầu thay đổi và phụ lục hợp đồng)

Khởi phát: người đại diện đề nghị thay đổi, Bác sĩ đánh giá lại ra mức chăm sóc khác, hoặc viện cần đổi phòng/giường. Kết thúc: yêu cầu Đã áp dụng, Từ chối, Hủy, Được thay thế hoặc Áp dụng không thành; kế hoạch chăm sóc đã được xem xét. Chuyển giường trực tiếp (7.4), tạm vắng (6.7) và đánh giá lại chỉ đổi nhu cầu chăm sóc không đi qua luồng này.

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 0 | Bác sĩ; Hệ thống | Bác sĩ xác nhận lần đánh giá lại ra mức chăm sóc khác. Hệ thống báo ngay Điều dưỡng phụ trách và người có quyền Duyệt kế hoạch chăm sóc để xem xét kế hoạch theo tình trạng mới, không chờ phụ lục; báo Hành chính lập yêu cầu đổi mức (bước 1). Chăm sóc theo đánh giá; mức của hợp đồng, dịch vụ chăm sóc và đơn giá đổi theo phụ lục. Trong lúc chờ phụ lục, chi phí tính theo mức cũ của hợp đồng | Mức của hợp đồng giữ nguyên tới bước 4 | BR-M01-03, BR-M04-20, Q-306, Q-423 |
| 1 | Hành chính, Bác sĩ; Người đại diện | Lập yêu cầu thay đổi: loại lưu trú, mức chăm sóc, dịch vụ, thời gian, phòng/giường; ngày hiệu lực mong muốn; lý do. Đánh giá lại ra mức khác thì Hành chính lập yêu cầu đổi mức, chưa lập thì được nhắc mỗi [1 ngày] (CFG-M03-02) tới khi lập hoặc có quyết định ở nhánh "Gia đình không đồng ý đổi mức"; yêu cầu đổi mức chăm sóc chưa áp dụng trước đó chuyển Được thay thế. Người đại diện gửi qua cổng thì yêu cầu ở Nháp để Hành chính hoàn thiện. Bác sĩ lập thì yêu cầu ở Nháp, Hành chính được báo; Bác sĩ không tự gửi duyệt | Yêu cầu: Nháp | UC-13, 6.6, BR-M01-03, Q-306 |
| 1a | Hành chính (hoặc Quản lý viện) | Yêu cầu do Hành chính hoặc Bác sĩ lập: xác nhận ý định thay đổi với người đại diện (gặp trực tiếp, gọi điện hoặc tin nhắn), ghi cách xác nhận, ngày xác nhận và đính kèm bằng chứng; thiếu thì không gửi duyệt được. Thay đổi nhỏ (Q-338) không cần bước này. Xác nhận này dùng để gửi duyệt, không thay phụ lục đã ký ở bước 3a | Yêu cầu: Nháp (đã có xác nhận) | Q-415 |
| 1b | Hành chính; Người đại diện | Thay cho 1a khi gia đình dùng ứng dụng: Hành chính gửi người đại diện xác nhận; người đại diện xem loại thay đổi, giá trị trước, giá trị mới, ảnh hưởng chi phí, ngày hiệu lực rồi Đồng ý hoặc Không đồng ý kèm lý do. Đồng ý thì Hành chính gửi duyệt; Không đồng ý thì Hành chính sửa, hủy hoặc xử lý theo nhánh "Gia đình không đồng ý đổi mức". Quá [48 giờ] (CFG-M15-05) chưa trả lời thì nhắc người đại diện và báo Hành chính liên hệ trực tiếp, một lần, không tự hủy | Lần gửi xác nhận: Chờ người đại diện → Đồng ý / Không đồng ý / Bị thay thế; yêu cầu vẫn ở Nháp | Q-416 |
| 1c | Hành chính, Điều dưỡng, Bác sĩ | Ghi ý kiến của chính người cao tuổi về thay đổi, có nguồn (người cao tuổi tự nói hoặc nhân viên ghi nhận khi người cao tuổi không diễn đạt được); ý kiến người cao tuổi và người đại diện là hai mục riêng. Không chặn gửi duyệt: chưa có ý kiến thì cảnh báo; ý kiến "Không đồng ý" thì gửi duyệt bắt buộc lý do. Thay đổi nhỏ không cần | Ý kiến người cao tuổi: đã ghi / chưa ghi | 1.3, BR-M01-14, BR-M01-15, Q-347, Q-348, Q-417 |
| 1d | Hành chính; Hệ thống | Gửi duyệt. Hệ thống xác định dấu "thay đổi nhỏ" và kiểm đủ xác nhận (1a hoặc 1b). Sửa phần thực chất (loại thay đổi, giá trị mới, ngày hiệu lực, giường, lịch đến, dịch vụ) sau khi đã xác nhận thì phải xác nhận lại; áp cả cho yêu cầu người đại diện tự gửi qua cổng khi Hành chính đổi phần thực chất | Yêu cầu: Nháp → Chờ duyệt | 6.6, Q-418 |
| 2 | Bộ lập lịch | Quá [48 giờ] chưa duyệt: nhắc người duyệt. Quá [96 giờ]: báo Quản lý viện. Không tự hủy | — | 6.6, CFG-M15-05, 06 |
| 3 | Quản lý viện; Hành chính (chỉ thay đổi nhỏ, Q-338) | Duyệt, Từ chối kèm lý do, hoặc Trả về nháp kèm lý do khi muốn sửa nội dung (không sửa trực tiếp; trả về thì xác nhận cũ bị bỏ). Hệ thống kiểm điều kiện của từng loại thay đổi và chặn khi đã có yêu cầu cùng loại Đã duyệt (chờ hiệu lực). Người lập có quyền duyệt loại yêu cầu đó thì được tự duyệt, bắt buộc lý do. Từ chối thì người lập và người đại diện được báo | Đã duyệt (chờ hiệu lực) / Từ chối / Nháp. Duyệt: phụ lục Chờ ký (thay đổi nhỏ: Chờ hiệu lực); đổi giường thì giữ giường từ ngày hiệu lực | UC-14, 6.6, Q-10, Q-338, Q-419 |
| 3a | Người đại diện, Hành chính | Ký phụ lục: Hành chính in phụ lục (giá trị trước, giá trị sau, đơn giá, ngày hiệu lực), người đại diện ký; Hành chính ghi nhận đã ký (ngày ký, người đại diện ký, bản scan). Ngày hiệu lực thực tế là ngày muộn nhất trong ngày mong muốn, ngày duyệt, ngày ký; không tính lùi. Chưa ký thì Hành chính được nhắc mỗi [1 ngày] (CFG-M02-17) | Phụ lục: Chờ ký → Chờ hiệu lực | Bộ luật Dân sự 2015 Điều 403; 6.3; Q-420 |
| 4 | Bộ lập lịch | Tới ngày hiệu lực của phụ lục Chờ hiệu lực: áp dụng trong cùng một lần: đổi mức chăm sóc của hợp đồng, đổi dịch vụ chăm sóc sang dịch vụ của mức mới, đổi đơn giá từ ngày hiệu lực, chuyển giường, đổi ngày kết thúc, đổi dịch vụ đăng ký. Bỏ lỡ ngày thì áp dụng bù ở lần chạy kế tiếp. Điều kiện không còn thỏa thì Áp dụng không thành. Phụ lục còn Chờ ký thì không áp dụng, chờ bước 3a | Yêu cầu: Đã áp dụng / Áp dụng không thành. Phụ lục: Đã áp dụng / Không áp dụng | BR-M02-04, Q-11 |
| 4a | Hệ thống | Báo mọi người đại diện (mức Nhẹ; loại thay đổi, ngày hiệu lực, không kèm giá) khi phụ lục Đã áp dụng. Áp dụng không thành thì báo người duyệt, người lập và người đại diện kèm lý do | — | Q-11, Q-422 |
| 5 | Hệ thống | Báo Điều dưỡng phụ trách và người có quyền Duyệt kế hoạch chăm sóc xem xét kế hoạch, trừ khi đã có phiên bản kế hoạch lập sau lần báo ở bước 0. Không tạo yêu cầu, không có hạn | — | BR-M04-20, Q-306, Q-423 |
| 6 | Điều dưỡng; người có quyền Duyệt kế hoạch chăm sóc | Điều dưỡng quyết định lập phiên bản mới hay giữ kế hoạch hiện hành; phiên bản mới do người có quyền duyệt. Việc gấp trước khi phiên bản mới có hiệu lực thì dùng công việc phát sinh | Kế hoạch: Nháp → Chờ duyệt → Hiệu lực, từ ngày hôm sau ngày duyệt | BR-M04-19, Q-34 |
| 7 | Người lập, Hành chính, Quản lý viện; Người đại diện | Hủy yêu cầu, bắt buộc lý do. Nháp, Chờ duyệt: người lập, Hành chính, Quản lý viện; người đại diện chỉ hủy yêu cầu do mình gửi. Đã duyệt (chờ hiệu lực): người duyệt. Phụ lục Chờ ký, Chờ hiệu lực chuyển Đã hủy, giữ giường (nếu có) bị hủy. Người đại diện được báo | Yêu cầu: Hủy. Phụ lục: Đã hủy | 6.6, Q-421 |

**Nhánh ngoại lệ**

| Tình huống | Vai trò | Xử lý | Căn cứ |
| ---------- | ------- | ----- | ------ |
| Gia đình không đồng ý đổi mức chăm sóc mà Bác sĩ đánh giá là cần | Hành chính; Bác sĩ | Hành chính ghi quyết định cho lần đánh giá lại: (a) giữ mức cũ của hợp đồng, kèm thỏa thuận lựa chọn có rủi ro đang hiệu lực (Bác sĩ đã xác nhận) và ngày xem lại; viện vẫn chăm sóc theo tình trạng thực; dừng nhắc CFG-M03-02; tới ngày xem lại thì nhắc Bác sĩ và Hành chính; (b) rời viện: dừng nhắc, Hành chính lập hồ sơ kết thúc lưu trú (BF-10). Đánh giá lại mới thì quyết định cũ hết tác dụng | 8.1, Q-349, Q-424 |
| Người đại diện Không đồng ý ở 1b với thay đổi khác | Hành chính | Sửa rồi gửi lại, hoặc hủy (bước 7) | Q-416 |
| Người đại diện không ký phụ lục sau khi đã xác nhận ý định | Hành chính; Quản lý viện | Phụ lục giữ Chờ ký, Hành chính được nhắc mỗi [1 ngày] (CFG-M02-17); quá [7 ngày] (CFG-M02-18) từ ngày duyệt thì báo Quản lý viện. Gia đình đổi ý thì hủy (bước 7); với đổi mức chăm sóc thì xử lý như dòng đầu | Q-420 |
| Có nhiều người đại diện | Người đại diện; Hệ thống | Một người đại diện hiện tại xác nhận và ký là đủ; người đại diện khác được báo mức Nhẹ khi yêu cầu được gửi duyệt và khi phụ lục được áp dụng | Q-422 |
| Đánh giá lại mới ra mức khác khi yêu cầu đổi mức chưa áp dụng | Hệ thống; Hành chính | Yêu cầu cũ chuyển Được thay thế, phụ lục cũ (nếu có) Không áp dụng; Hành chính được báo lập yêu cầu mới | 6.6, BR-M01-03 |
| Viện phải chuyển giường gấp vì lý do vận hành hoặc an toàn | Hành chính, Trưởng tầng | Chuyển giường trực tiếp theo 7.4, không qua BF-08. Giường mới đơn giá cao hơn: tới khi phụ lục đổi phòng được áp dụng, phí tính theo đơn giá cũ; thấp hơn thì tính giá mới từ ngày chuyển. Hành chính lập yêu cầu đổi phòng nếu người cao tuổi ở lâu dài tại giường mới | 7.4, Q-306, Q-424 |
| Người cao tuổi chuyển trạng thái cuối khi yêu cầu chưa áp dụng | Hệ thống | Yêu cầu Nháp, Chờ duyệt chuyển Hủy; Đã duyệt chuyển Áp dụng không thành; phụ lục Chờ ký, Chờ hiệu lực chuyển Không áp dụng | 6.6 |
| Đổi loại lưu trú từ bán trú sang nội trú | Hành chính; Hệ thống | Yêu cầu kèm giường mới. Khi phần nội trú có hiệu lực, Hành chính được nhắc lập khai báo tạm trú, lưu trú "Cần khai báo" như BF-01 bước 15a | BR-M02-11, Q-286, Q-336 |

**Điểm đã làm rõ**

- Chăm sóc và hợp đồng tách nhau (bước 0): chăm sóc theo tình trạng thực từ lúc Bác sĩ đánh giá lại; mức của hợp đồng, dịch vụ chăm sóc và đơn giá chỉ đổi khi phụ lục được áp dụng. BR-M02-04 không tạo phiên bản kế hoạch tự động.
- Đánh giá lại chỉ đổi nhu cầu chăm sóc (mức giữ nguyên) không đi qua luồng này; Điều dưỡng phụ trách vẫn được báo như bước 0.
- Xác nhận ở 1a, 1b là xác nhận ý định để gửi duyệt; phụ lục đã ký ở 3a là căn cứ để áp dụng. Thay đổi nhỏ viện tự quyết, không cần xác nhận, không cần ký.
- Người duyệt không sửa nội dung yêu cầu; muốn sửa thì Trả về nháp, để nội dung gia đình đã xác nhận và ký trùng nội dung được áp dụng.
- Ý kiến người cao tuổi không chặn luồng, như BF-01; quyết định cuối thuộc người có thẩm quyền theo BR-M01-15.
```

---

## Phụ lục B – Dòng Q-415 → Q-426 (thêm vào cuối bảng 24.2 của `nghiep-vu.md`)

Định dạng cột theo bảng hiện có: `| Mã | Câu hỏi | Quyết định | Mục liên quan | Feature |`. Ghi "Chủ dự án 2026-11-20".

| Mã | Câu hỏi | Quyết định | Mục | Feature |
| -- | ------- | ---------- | --- | ------- |
| Q-415 | Yêu cầu thay đổi lưu trú do viện lập có cần ý kiến người đại diện trước khi duyệt | Có, trừ thay đổi nhỏ: Hành chính xác nhận với người đại diện (gặp, gọi điện, tin nhắn) kèm bằng chứng trước khi gửi Quản lý viện duyệt; yêu cầu người đại diện tự gửi qua cổng không cần | 6.6 | 004 |
| Q-416 | Người đại diện xác nhận trên ứng dụng được không | Được: Hành chính gửi xác nhận, người đại diện xem chi phí rồi Đồng ý hoặc Không đồng ý kèm lý do; Đồng ý thay cho xác nhận kèm bằng chứng; quá CFG-M15-05 nhắc một lần, không tự hủy | 6.6 | 004, 012 |
| Q-417 | Có ghi ý kiến người cao tuổi về thay đổi lưu trú không | Có, mục riêng, có nguồn; không chặn gửi duyệt; "Không đồng ý" thì gửi duyệt bắt buộc lý do; thay đổi nhỏ không cần | 6.6, 1.3, BR-M01-14, 15 | 004 |
| Q-418 | Sửa yêu cầu sau khi người đại diện đã xác nhận, hoặc Hành chính sửa yêu cầu người đại diện gửi qua cổng | Đổi phần thực chất (loại, giá trị mới, ngày hiệu lực, giường, lịch đến, dịch vụ) thì phải xác nhận lại; đổi ghi chú, tệp thì không | 6.6 | 004 |
| Q-419 | Người duyệt muốn sửa nội dung yêu cầu | Không sửa trực tiếp; có lệnh Trả về nháp (Chờ duyệt → Nháp, bắt buộc lý do), xác nhận cũ bị bỏ | 6.6 | 000, 004 |
| Q-420 | Phụ lục có cần ký trước khi áp dụng | Có, trừ thay đổi nhỏ: duyệt tạo phụ lục Chờ ký; Hành chính ghi nhận ngày ký, người đại diện ký, bản scan thì phụ lục Chờ hiệu lực; chưa ký không áp dụng; ngày hiệu lực thực tế là ngày muộn nhất trong ngày mong muốn, ngày duyệt, ngày ký; nhắc theo CFG-M02-17, báo Quản lý viện theo CFG-M02-18 | 6.6, Phụ lục 25 | 000, 004 |
| Q-421 | Ai hủy yêu cầu thay đổi lưu trú, phụ lục của yêu cầu bị hủy thế nào | Nháp, Chờ duyệt: người lập, Hành chính, Quản lý viện; người đại diện chỉ hủy yêu cầu mình gửi; Đã duyệt: người duyệt. Phụ lục Chờ ký, Chờ hiệu lực chuyển Đã hủy, giữ giường bị hủy | 6.6 | 000, 004 |
| Q-422 | Người cao tuổi có nhiều người đại diện | Một người xác nhận và ký là đủ; người đại diện khác được báo mức Nhẹ khi gửi duyệt và khi áp dụng; báo người đại diện khi yêu cầu bị từ chối, hủy, áp dụng không thành | 6.6 | 004, 009 |
| Q-423 | Khi nào báo Điều dưỡng xem xét kế hoạch chăm sóc khi đổi mức | Ngay khi xác nhận đánh giá lại; phụ lục đổi mức áp dụng không báo lại nếu đã có phiên bản kế hoạch lập sau lần báo đó; chi phí vẫn theo mức của hợp đồng tới khi phụ lục áp dụng | BR-M01-03, BR-M04-20 | 001, 005 |
| Q-424 | (a) Gia đình không đồng ý đổi mức theo đánh giá lại; (b) dừng nhắc lập yêu cầu đổi mức; (c) chênh lệch giá khi chuyển giường gấp | (a) Hành chính ghi quyết định: giữ mức cũ kèm thỏa thuận lựa chọn có rủi ro và ngày xem lại, hoặc rời viện (BF-10); (b) có quyết định thì dừng nhắc CFG-M03-02, đánh giá lại mới thì nhắc lại; (c) giường mới đắt hơn: tính giá cũ tới khi phụ lục đổi phòng áp dụng; rẻ hơn: giá mới từ ngày chuyển | 7.4, 8.1, CFG-M03-02 | 001, 004, 005, 010 |
| Q-426 | Loại dịch vụ nào được thêm vào hợp đồng qua yêu cầu thay đổi lưu trú | Chỉ phục hồi chức năng, hoạt động đăng ký dài hạn, đưa đi khám, tiêm theo y lệnh (dịch vụ đi kèm đăng ký riêng, 6.4). Dịch vụ chăm sóc đổi qua mức chăm sóc; phí lưu trú qua giường; buổi bán trú qua loại lưu trú, lịch đến; suất ăn, người thân ở lại, thuốc nguồn viện, vật phẩm tiêu hao, vật tư y tế đi theo luồng riêng (15.2, BF-12, BR-M07-14, UC-79), không đăng ký trong hợp đồng. Bớt được mọi dòng đang có. Dịch vụ thuộc gói thì công việc, điểm danh không sinh phí riêng; ngoài gói tính theo lần | 6.4, 6.6, 15.2 | 004 |

---

## Phụ lục C – Hợp đồng API cần thêm

`api-000.yaml`
- `POST /approval-requests/{id}/return-to-draft`: header `If-Match`; body `ReturnToDraftInput { reason: string (bắt buộc, 1..1000) }`; 200 `ApprovalRequest`; 403; 409 (`RETURN_TO_DRAFT_NOT_ALLOWED`, sai trạng thái); 412.
- `POST /approval-requests/{id}/submit`: thêm body tùy chọn `SubmitInput { reason?: string }`.

`api-004.yaml`
- Schema phụ lục (dòng ~973): `status` enum thêm `PENDING_SIGNATURE`, `CANCELLED` (giữ `NOT_APPLICABLE`); thêm `addendumNo` (int32), `signedDate` (date, nullable), `signedByFamilyMemberId` (int64, nullable), `facilitySignatoryStaffId` (int64, nullable), `signedFileId` (int64, nullable), `signatureRequired` (boolean).
- `POST /residency-change-requests/{id}/addendum-signature`: header `If-Match`; body `AddendumSignatureInput { signedDate: date, signedByFamilyMemberId: int64, facilitySignatoryStaffId?: int64, fileId: int64, note?: string }`;
- Schema `RepresentativeConfirmation` (xác nhận 1a): thêm `representativeFamilyMemberId: int64` (bắt buộc khi ghi).
- Dòng dịch vụ của `ResidencyChangeRequestInput`: `inPackage` → `deprecated: true`, mô tả "bỏ qua; hệ thống xác định theo hợp đồng hiện hành".
- `ResidencyChangeRequest`: thêm `assessedCareLevel { assessmentId: int64, careLevel, confirmedAt: date-time }` (nullable).
- Mã điều kiện mới: `CONFIRMER_NOT_REPRESENTATIVE`, `CARE_LEVEL_NOT_FROM_ASSESSMENT`, `SERVICE_TYPE_NOT_REGISTRABLE` (422); cảnh báo `RESIDENT_OPINION_OUTDATED`. 200 `ResidencyChangeRequest`; 403; 409 (`ADDENDUM_NOT_PENDING_SIGNATURE`, `SIGNER_NOT_REPRESENTATIVE`, điều kiện giữ giường); 422 (`SIGNED_DATE_INVALID`).
- `GET|POST /residency-change-requests/{id}/resident-opinions`: body `ResidentOpinionInput { opinion: enum [AGREE, DISAGREE, UNABLE_TO_EXPRESS], source: enum [RESIDENT, STAFF_OBSERVATION], note?: string }`; GET trả mảng `ResidentOpinion { id, opinion, source, note, recordedBy, recordedAt, outdated: boolean }`.
- `ResidencyChangeRequest`: thêm `currentResidentOpinion` (nullable `ResidentOpinion`), `warnings` (mảng string), `addendum` có các trường mới ở trên.

`api-001.yaml`
- `POST /residents/{residentId}/care-level-change-decisions`: body `CareLevelChangeDecisionInput { assessmentId: int64, decision: enum [KEEP_CURRENT_LEVEL, LEAVE_FACILITY], riskAgreementId?: int64, reviewDate?: date, note?: string }`; 201 `CareLevelChangeDecision`; 409 (`CHANGE_REQUEST_EXISTS`, `RISK_AGREEMENT_REQUIRED`, `NOT_LATEST_ASSESSMENT`); 422.
- `GET /residents/{residentId}/care-level-change-decisions`: mảng `CareLevelChangeDecision`.

Mọi tên mới thêm vào glossary (tên Việt ↔ tên Anh) theo R-03.
