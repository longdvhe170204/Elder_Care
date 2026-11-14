# AGENTS.md: luật chung cho mọi người và mọi công cụ AI

Dự án Elder_Care (quản lý viện dưỡng lão), nhóm 5 người làm song song, mỗi người có thể dùng một công cụ AI khác nhau (Claude Code, ChatGPT/Codex, Cursor, Copilot, Gemini…). File này là điểm vào chung: công cụ nào cũng phải đọc file này trước khi viết mã. Bản đầy đủ ở [CLAUDE.md](CLAUDE.md); khi khác nhau thì thứ tự thắng là: `.specify/memory/constitution.md` → `tasks.md` của feature → CLAUDE.md → file này.

Nếu công cụ của bạn không tự đọc file này, hãy dán câu sau vào đầu cuộc trò chuyện:

> Đọc AGENTS.md, CLAUDE.md, docs/team-plan.md và README của phần bạn sẽ sửa (backend/ của kho này, hoặc kho Elder_Care_Frontend, Elder_Care_Mobile). Tôi là <A/B/C/D/E>, đang làm feature <NNN> trên nhánh impl/<NNN-ten>. Chỉ sửa trong phần của tôi theo docs/team-plan.md. Trước khi làm, liệt kê cho tôi các dòng có tên tôi ở mục 8 của docs/team-plan.md (việc người khác đang chờ tôi).

## 1. Đọc gì, theo thứ tự nào

1. `docs/team-plan.md`: bạn sở hữu module, thư mục, migration nào; đang ở đợt nào; **mục 8: việc người khác đang chờ bạn** (đầu mỗi phiên báo cho người dùng các dòng có tên họ; cần người khác làm gì thì thêm dòng trong PR của mình; việc xong thì xóa dòng, quy định ngay trên bảng mục 8).
2. README của phần sẽ sửa: [backend/README.md](backend/README.md), [README kho web](https://github.com/longdvhe170204/Elder_Care_Frontend/blob/develop/README.md), [README kho app](https://github.com/longdvhe170204/Elder_Care_Mobile/blob/develop/README.md). Web và app ở hai kho riêng từ 2026-11-14 (README gốc kho này mục "Ba kho của hệ thống").
3. Khi làm một task: `specs/NNN-*/tasks.md` → `plan.md` → `data-model.md` → `contracts/` → `research.md` → `spec.md` → `docs/business/nghiep-vu.md`. Đường dẫn file trong tasks, plan (`BE/resident/hoso/internal/…`) là tên logic: đặt lớp theo cấu trúc ở [backend/README.md](backend/README.md) (research R-02 của 000), không chép đường dẫn trong tasks.
4. Tên tiếng Anh: [specs/000-shared-foundation/glossary.md](specs/000-shared-foundation/glossary.md). Giao diện: [DESIGN.md](DESIGN.md).

## 2. Mười luật không được vi phạm

1. **Nghiệp vụ chỉ lấy từ `docs/business/nghiep-vu.md`** (BR, CFG, Q-xx, Phụ lục 27 ma trận quyền). Không đoán, không tự thêm quy tắc. Chưa rõ thì tra mã Q-xx ở mục 24, rồi dừng và hỏi chủ dự án. Không sửa `docs/business/*.md` và `spec.md` khi chưa được yêu cầu.
2. **Chỉ sửa phần của mình** (docs/team-plan.md mục 1, `.github/CODEOWNERS`). Cần thay đổi ở phần người khác: nhắn chủ phần đó hoặc mở PR để họ review.
3. **Module khác chỉ gọi qua gói `api`** của module đó. Chưa có bản thật thì dùng bản giả do người khai báo interface viết, ở `backend/src/test/java/com/eldercare/backend/support/fake/<module>/`. Không tự viết bản giả thứ hai.
4. **Test trước**: viết test, chạy thấy lỗi, rồi mới cài. Tên test bắt đầu bằng mã quy tắc (`BR_M01_09_…`, `FR_017_…`, `US5_4_…`). Chỉ báo "xong" và đánh `[x]` task khi đã chạy test và thấy xanh.
5. **Hợp đồng API là luật**: chỉ cài đúng endpoint, trường trong `contracts/api-NNN.yaml` đã dịch tiếng Anh. Không thêm, bớt, đổi tên endpoint hay trường. Cần đổi thì hỏi chủ dự án.
6. **Tên và chỗ đặt file**: mã nguồn tiếng Anh theo glossary; tài liệu, chữ trên màn, comment tiếng Việt. Thuật ngữ mới thêm vào mục glossary của feature mình cùng thay đổi với mã. Đặt file theo README của phần đang sửa ([backend/README.md](backend/README.md), README kho Elder_Care_Frontend, README kho Elder_Care_Mobile); đường dẫn file trong tasks, plan (`BE/resident/hoso/internal/…`, `frontend/src/tinh-nang/…`, `mobile/lib/nhan-vien/…`) là tên logic, chỉ cho biết module và vai trò, không chép (`frontend/…`, `mobile/…` nay ở gốc kho web, kho app). Backend có `LayerArchTest`, `ModuleBoundaryTest` kiểm.
7. **Hệ thống không tự sinh bản ghi** (Q-306, Q-307): mọi bản ghi do người dùng lập bằng lệnh. Job chỉ được chuyển trạng thái theo giờ, nhắc, cảnh báo, áp dụng yêu cầu đã duyệt, loại bỏ dữ liệu hết hạn (`SchedulerScopeArchTest` kiểm).
8. **Không cố định giá trị nghiệp vụ**: mốc thời gian, ngưỡng đọc từ tham số CFG qua `ParameterApi`; thời gian lấy từ `BusinessTime`/`Clock`, không gọi `Instant.now()` (`TimeArchTest` kiểm).
9. **Không viết mã thừa**: chỉ đủ cho task đang làm; không thêm tính năng, lớp trừu tượng, cấu hình mà tasks/plan không yêu cầu; không sửa định dạng, mã kế bên. Lệch kỹ thuật thì sửa plan, tasks cùng lúc và ghi lý do ở plan.
10. **Git**: AI không tự commit, push, merge, rebase, đổi nhánh khi người dùng chưa đồng ý. Mỗi feature một nhánh `impl/NNN-ten` từ `develop`; hết phase thì commit trên nhánh đó, xong feature mới mở PR vào `develop` (CLAUDE.md "Khi vào triển khai" điều 6, 7; sửa 2026-11-04); PR riêng sớm hơn chỉ cho hợp đồng đã dịch và `api` cần công bố (docs/team-plan.md mục 4); không push thẳng `develop`.

## 3. Định nghĩa "xong" một phase

- [ ] Test của phase đã viết trước, đã thấy lỗi, giờ xanh
- [ ] `backend`: test của feature, test kiến trúc (`*ArchTest`, `ModuleBoundaryTest`, `ApiContractIT`) và IT của module mà phase sửa hoặc có thể bị ảnh hưởng đều xanh; `./mvnw clean verify -Dit.forkCount=3` toàn bộ (Windows: `.\mvnw.cmd clean verify "-Dit.forkCount=3"`; IT chạy trên 3 JVM, cần `DB_PASSWORD`, `DB_RETENTION_PASSWORD`) chỉ bắt buộc trước khi mở PR vào `develop`, hoặc khi phase đụng `common`, 000, thêm bean ảnh hưởng nhiều module (sửa 2026-11-04, chủ dự án duyệt; lệnh chạy nhóm IT ở CLAUDE.md mục "Lệnh"); có sửa Java thì `./mvnw spotless:apply`
- [ ] Kho web: `npm test`, `npm run lint`, `npm run build` qua; kho app: `flutter analyze`, `flutter test` qua
- [ ] Đánh `[x]` task trong `tasks.md`; ghi chú triển khai, lệch kỹ thuật ở `plan.md`
- [ ] Glossary của feature cập nhật
- [ ] Commit trên nhánh feature (khi người dùng đồng ý) ghi phase, task, có đổi `api` không ("Đổi api" + đã báo ai); PR cuối feature mô tả các phase, kết quả `clean verify`
- [ ] `docs/team-plan.md` mục 8: xóa dòng mình vừa làm xong (kết quả ghi ở `docs/progress.md`); sửa dòng xong một phần cho chỉ còn phần dở; thêm dòng cho việc cần người khác làm
- [ ] `docs/progress.md`: thêm một dòng tiến độ (nhánh, PR, việc xong, kết quả test, việc còn lại); nhật ký chỉ thêm, không sửa, xóa dòng cũ; không ghi tiến độ vào AGENTS.md, CLAUDE.md

## 4. Backend (`backend/`)

- Spring Boot 4.1, Java 25, Maven wrapper, MySQL 8.0 (không dùng H2), Flyway, Spring Modulith.
- Gói gốc `com.eldercare.backend`: `common` (kỹ thuật dùng chung, A giữ) và `resident` … `administration` (mỗi module một chủ). Trong module: `api/` (công khai: interface; `dto/`, `spi/`, `events/`), `web/` (controller; `request/`, `response/`), `internal/` (luôn chia `service/`, `domain/`, `persistence/`, `config/`; module lớn chia theo chức năng trước, mỗi chức năng có các tầng đó), `scheduling/` (job); chi tiết và luật ở research R-02 của 000, mục "Cấu trúc trong module". Module khác chỉ dùng `api/` (`ModuleBoundaryTest`); trong M15, phần này chỉ dùng `api/` của phần kia (`M15PartBoundaryArchTest`).
- Migration: `src/main/resources/db/migration/V<NNN>_<nn>__<ten>.sql` (NNN là số feature). Không sửa migration đã merge; sửa bằng migration mới. Khóa ngoại tới bảng module khác chỉ thêm khi module đó đã vào `develop`.
- Nhóm dữ liệu (`backend/docs/data-group-conventions.md`): danh mục, nghiệp vụ có trạng thái (chỉ đổi qua lệnh, `If-Match`), ghi nhận (chỉ thêm, sửa bằng đính chính). Mọi lệnh ghi nhật ký qua `AuditLogApi`, lệnh cần lý do dùng `@BusinessCommand` + `Reason`.
- Lỗi theo RFC 9457 (`errorCode`, `unmetConditions`, `currentValue`); mã lỗi theo glossary.
- Quyền: `PermissionKey(Function, Operation)`, `Function` là một dòng Phụ lục 27 (`administration.account.api.dto.Function`). Không tự đặt khóa chuỗi.
- Test: `*Test` đơn vị, `*IT` tích hợp (kế thừa `support/MySqlIntegrationTestBase`, cần biến `TEST_DB_URL`, `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD`), `*ArchTest` kiến trúc. Hai tài khoản MySQL `eldercare_app`, `eldercare_retention` dùng chung cho test và profile demo: máy chạy cả hai thì đặt thêm `DB_PASSWORD`, `DB_RETENTION_PASSWORD` khi chạy test, nếu không test đổi mật khẩu sang ngẫu nhiên và demo không đăng nhập được.
- Job (`scheduling/`): profile test tắt mọi lịch chạy bằng công tắc chung `eldercare.scheduling.enabled` (`SchedulingConfig`), không cần ghi `cron: "-"`. Job chạy lúc khởi động đặt mặc định là `@Value("${eldercare.jobs.<job>.on-startup:${eldercare.scheduling.enabled:true}}")`; test gọi job trực tiếp hoặc qua `runJobs` (`SchedulingSwitchIT` kiểm). Job mới thêm vào `SchedulerScopeArchTest` và, nếu test cần, vào `runJobs` của api-000 (A duyệt).
- "Đổi api" (thêm hàm, trường vào interface, DTO, sự kiện công khai ở `api/`) trên module của người khác: chủ dự án duyệt trước; ghi ở `contracts/module-interfaces.md` của feature sở hữu và glossary; thêm dòng team-plan mục 8 để chủ module review trong PR. Không đổi chữ ký hàm cũ; thêm thành phần record thì sửa mọi nơi tạo record và bản giả chung có sẵn, không viết bản giả thứ hai.
- Sự kiện qua outbox lưu ở `EVENT_PUBLICATION.SERIALIZED_EVENT` (`TEXT` từ `V000_16`, trước đó VARCHAR 4000): không đưa danh sách dài (nhiều người nhận, nhiều id) vào một sự kiện; người nhận theo nhóm vai trò (`RecipientGroup` của `common.notification`).
- Schema test `eldercare_test` giữ dữ liệu qua các lần chạy: id lấy từ `TestUsers.nextId()`, không giả định bảng rỗng; việc định kỳ hoặc truy vấn quét toàn bảng phải tự cô lập khỏi dữ liệu sót của test khác (mẫu `CallReminderTestBase` của notification). `./mvnw clean verify` mất khoảng 10–15 phút, chạy nền; lớp đang đỏ có sẵn trên `develop` ghi ở team-plan mục 8.
- Ngân sách test backend (chủ dự án chốt 2026-11-07, sau khi giảm unit 1.220 → khoảng 810, IT 3.660 → khoảng 360; `clean verify` 56 → 14 phút): toàn kho giữ unit khoảng 650–950, IT khoảng 250–400, đếm theo số `Tests run` Maven báo (mỗi ca của `@ParameterizedTest`, mỗi luật ArchUnit là một ca; sửa 2026-11-07: lúc đó unit 1.058 ca dù chỉ 814 phương thức, đã đưa về 905). Bảng dữ liệu (bảng chuyển trạng thái, danh sách đầu vào sai, các tháng) kiểm trong một `@Test` bằng `support/TableRows.each` hoặc `TableRows.csv` (gom mọi dòng sai rồi báo một lần), không dùng `@ParameterizedTest` cho bảng trên khoảng 5 dòng. IT chỉ cho luồng chính của mỗi user story, quy tắc DBR (trigger, khóa duy nhất, ràng buộc), đồng thời và khóa, sự kiện sau commit; nhánh từ chối phụ (thiếu trường, 422, từng vai trò bị 403, If-Match 412, Idempotency-Key) viết unit test hoặc dựa vào test chung (`PermissionMatrixIT`, `RequiresPermissionArchTest`, `RecordGroupTriggerIT`, `AuditLogCompletenessIT`, `ApiContractIT`, `ReasonTest`, `VersionEtagTest`, `IdempotencyFilterIT`). Mỗi mã yêu cầu tối đa 3 IT, 2 unit. Không viết IT cho migration chép dữ liệu hoặc hình dạng schema.
- Thời gian `verify` do số lần dựng Spring context quyết định: IT mới không khai cấu hình lồng `@TestConfiguration` riêng hay `@MockitoBean` nếu bản giả hoặc cấu hình dùng chung có sẵn đáp ứng (người dùng đổi được: `support/SwitchableUserConfig`); cùng tổ hợp `@Import` với lớp khác của module để dùng lại context. Bản giả thay bean thật cùng loại thì tự gỡ bean thật trong context của mình (mẫu `FakeM02PortsConfig.withoutCostStatusParticipant`) thay vì để hai bean cùng chạy.

## 5. Web và mobile

Web ở kho [Elder_Care_Frontend](https://github.com/longdvhe170204/Elder_Care_Frontend), app ở kho [Elder_Care_Mobile](https://github.com/longdvhe170204/Elder_Care_Mobile); theo README của từng kho: thư mục màn hình theo feature, không import chéo feature, gọi API chỉ qua client sinh từ hợp đồng, lỗi qua `problemOf`/`ProblemAlert`, ngày giờ qua `dateTime.js`, màu chữ qua token của DESIGN.md. Mobile đã mở cả ba phần (người cao tuổi 2026-10-24; nhân viên, người thân 2026-10-27): màn của feature ở `lib/{staff, family, resident}/<feature>/` của kho app, phần dùng chung ở `lib/common/` do A giữ; màn cần đăng nhập nhân viên, người thân hoặc ghi khi mất mạng thì chờ phần nền của A (CLAUDE.md mục "Giai đoạn hiện tại").

## 6. AI không được làm

- Không bịa dữ liệu người cao tuổi, quy tắc, quyền; dữ liệu minh họa chỉ ở `src/common/demoData.js` của kho web, tên người hư cấu, đúng hình dạng hợp đồng (DESIGN.md mục 8, 13 điều 6); màn không hiện trường ngoài spec và hợp đồng.
- Không ghi mật khẩu, khóa API vào kho mã (dùng biến môi trường).
- Không tắt, bỏ qua, xóa test cho xanh; không sửa test của người khác cho khớp mã của mình.
- Không đổi `pom.xml`, `package.json`, `pubspec.yaml`, `CLAUDE.md`, `common/` khi không phải A.
- Không sửa file sinh tự động (`src/api/` của kho web); chạy lại `npm run gen:api`.
