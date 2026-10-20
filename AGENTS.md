# AGENTS.md: luật chung cho mọi người và mọi công cụ AI

Dự án Elder_Care (quản lý viện dưỡng lão), nhóm 5 người làm song song, mỗi người có thể dùng một công cụ AI khác nhau (Claude Code, ChatGPT/Codex, Cursor, Copilot, Gemini…). File này là điểm vào chung: công cụ nào cũng phải đọc file này trước khi viết mã. Bản đầy đủ ở [CLAUDE.md](CLAUDE.md); khi khác nhau thì thứ tự thắng là: `.specify/memory/constitution.md` → `tasks.md` của feature → CLAUDE.md → file này.

Nếu công cụ của bạn không tự đọc file này, hãy dán câu sau vào đầu cuộc trò chuyện:

> Đọc AGENTS.md, CLAUDE.md, docs/team-plan.md và README của thư mục bạn sẽ sửa (backend/, frontend/ hoặc mobile/). Tôi là <A/B/C/D/E>, đang làm feature <NNN> trên nhánh impl/<NNN-ten>. Chỉ sửa trong phần của tôi theo docs/team-plan.md. Trước khi làm, liệt kê cho tôi các dòng có tên tôi ở mục 8 của docs/team-plan.md (việc người khác đang chờ tôi).

## 1. Đọc gì, theo thứ tự nào

1. `docs/team-plan.md`: bạn sở hữu module, thư mục, migration nào; đang ở đợt nào; **mục 8: việc người khác đang chờ bạn** (đầu mỗi phiên báo cho người dùng các dòng có tên họ; cần người khác làm gì thì thêm dòng trong PR của mình).
2. README của phần sẽ sửa: [backend/README.md](backend/README.md), [frontend/README.md](frontend/README.md), [mobile/README.md](mobile/README.md).
3. Khi làm một task: `specs/NNN-*/tasks.md` → `plan.md` → `data-model.md` → `contracts/` → `research.md` → `spec.md` → `docs/business/nghiep-vu.md`. Đường dẫn file trong tasks, plan (`BE/m01/hoso/internal/…`) là tên logic: đặt lớp theo cấu trúc ở [backend/README.md](backend/README.md) (research R-02 của 000), không chép đường dẫn trong tasks.
4. Tên tiếng Anh: [specs/000-shared-foundation/glossary.md](specs/000-shared-foundation/glossary.md). Giao diện: [DESIGN.md](DESIGN.md).

## 2. Mười luật không được vi phạm

1. **Nghiệp vụ chỉ lấy từ `docs/business/nghiep-vu.md`** (BR, CFG, Q-xx, Phụ lục 27 ma trận quyền). Không đoán, không tự thêm quy tắc. Chưa rõ thì tra mã Q-xx ở mục 24, rồi dừng và hỏi chủ dự án. Không sửa `docs/business/*.md` và `spec.md` khi chưa được yêu cầu.
2. **Chỉ sửa phần của mình** (docs/team-plan.md mục 1, `.github/CODEOWNERS`). Cần thay đổi ở phần người khác: nhắn chủ phần đó hoặc mở PR để họ review.
3. **Module khác chỉ gọi qua gói `api`** của module đó. Chưa có bản thật thì dùng bản giả do người khai báo interface viết, ở `backend/src/test/java/com/cambae/backend/support/fake/<module>/`. Không tự viết bản giả thứ hai.
4. **Test trước**: viết test, chạy thấy lỗi, rồi mới cài. Tên test bắt đầu bằng mã quy tắc (`BR_M01_09_…`, `FR_017_…`, `US5_4_…`). Chỉ báo "xong" và đánh `[x]` task khi đã chạy test và thấy xanh.
5. **Hợp đồng API là luật**: chỉ cài đúng endpoint, trường trong `contracts/api-NNN.yaml` đã dịch tiếng Anh. Không thêm, bớt, đổi tên endpoint hay trường. Cần đổi thì hỏi chủ dự án.
6. **Tên và chỗ đặt file**: mã nguồn tiếng Anh theo glossary; tài liệu, chữ trên màn, comment tiếng Việt. Thuật ngữ mới thêm vào mục glossary của feature mình cùng thay đổi với mã. Đặt file theo README của thư mục đang sửa ([backend/README.md](backend/README.md), [frontend/README.md](frontend/README.md), [mobile/README.md](mobile/README.md)); đường dẫn file trong tasks, plan (`BE/m01/hoso/internal/…`, `frontend/src/tinh-nang/…`, `mobile/lib/nhan-vien/…`) là tên logic, chỉ cho biết module và vai trò, không chép. Backend có `LayerArchTest`, `ModuleBoundaryTest` kiểm.
7. **Hệ thống không tự sinh bản ghi** (Q-306, Q-307): mọi bản ghi do người dùng lập bằng lệnh. Job chỉ được chuyển trạng thái theo giờ, nhắc, cảnh báo, áp dụng yêu cầu đã duyệt, loại bỏ dữ liệu hết hạn (`SchedulerScopeArchTest` kiểm).
8. **Không cố định giá trị nghiệp vụ**: mốc thời gian, ngưỡng đọc từ tham số CFG qua `ParameterApi`; thời gian lấy từ `BusinessTime`/`Clock`, không gọi `Instant.now()` (`TimeArchTest` kiểm).
9. **Không viết mã thừa**: chỉ đủ cho task đang làm; không thêm tính năng, lớp trừu tượng, cấu hình mà tasks/plan không yêu cầu; không sửa định dạng, mã kế bên. Lệch kỹ thuật thì sửa plan, tasks cùng lúc và ghi lý do ở plan.
10. **Git**: AI không tự commit, push, merge, rebase, đổi nhánh khi người dùng chưa đồng ý. Mỗi feature một nhánh `impl/NNN-ten` từ `develop`; mỗi phase một PR vào `develop`; không push thẳng `develop`.

## 3. Định nghĩa "xong" một phase

- [ ] Test của phase đã viết trước, đã thấy lỗi, giờ xanh
- [ ] `backend`: `./mvnw verify` qua (Windows: `.\mvnw.cmd verify`); có sửa Java thì `./mvnw spotless:apply`
- [ ] `frontend`: `npm test`, `npm run lint`, `npm run build` qua
- [ ] Đánh `[x]` task trong `tasks.md`; ghi chú triển khai, lệch kỹ thuật ở `plan.md`
- [ ] Glossary của feature cập nhật
- [ ] PR mô tả: phase, task, có đổi `api` không ("Đổi api" + đã báo ai), kết quả lệnh kiểm
- [ ] `docs/team-plan.md` mục 8: đánh "Xong" dòng mình vừa làm; thêm dòng cho việc cần người khác làm

## 4. Backend (`backend/`)

- Spring Boot 4.1, Java 25, Maven wrapper, MySQL 8.0 (không dùng H2), Flyway, Spring Modulith.
- Gói gốc `com.cambae.backend`: `common` (kỹ thuật dùng chung, A giữ) và `m01` … `m15` (mỗi module một chủ). Trong module: `api/` (công khai: interface; `dto/`, `spi/`, `events/`), `web/` (controller; `request/`, `response/`), `internal/` (luôn chia `service/`, `domain/`, `persistence/`, `config/`; module lớn chia theo chức năng trước, mỗi chức năng có các tầng đó), `scheduling/` (job); chi tiết và luật ở research R-02 của 000, mục "Cấu trúc trong module". Module khác chỉ dùng `api/` (`ModuleBoundaryTest`); trong M15, phần này chỉ dùng `api/` của phần kia (`M15PartBoundaryArchTest`).
- Migration: `src/main/resources/db/migration/V<NNN>_<nn>__<ten>.sql` (NNN là số feature). Không sửa migration đã merge; sửa bằng migration mới. Khóa ngoại tới bảng module khác chỉ thêm khi module đó đã vào `develop`.
- Nhóm dữ liệu (`backend/docs/data-group-conventions.md`): danh mục, nghiệp vụ có trạng thái (chỉ đổi qua lệnh, `If-Match`), ghi nhận (chỉ thêm, sửa bằng đính chính). Mọi lệnh ghi nhật ký qua `AuditLogApi`, lệnh cần lý do dùng `@BusinessCommand` + `Reason`.
- Lỗi theo RFC 9457 (`errorCode`, `unmetConditions`, `currentValue`); mã lỗi theo glossary.
- Quyền: `PermissionKey(Function, Operation)`, `Function` là một dòng Phụ lục 27 (`m15.account.api.dto.Function`). Không tự đặt khóa chuỗi.
- Test: `*Test` đơn vị, `*IT` tích hợp (kế thừa `support/MySqlIntegrationTestBase`, cần biến `TEST_DB_URL`, `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD`), `*ArchTest` kiến trúc.

## 5. Web và mobile

Theo [frontend/README.md](frontend/README.md) và [mobile/README.md](mobile/README.md): thư mục màn hình theo feature, không import chéo feature, gọi API chỉ qua client sinh từ hợp đồng, lỗi qua `problemOf`/`ProblemAlert`, ngày giờ qua `dateTime.js`, màu chữ qua token của DESIGN.md. Mobile hiện chỉ làm màn mẫu thiết kế.

## 6. AI không được làm

- Không bịa dữ liệu người cao tuổi, quy tắc, quyền; dữ liệu minh họa chỉ ở `frontend/src/common/demoData.js`, tên dạng `[Họ tên NCT 1]`.
- Không ghi mật khẩu, khóa API vào kho mã (dùng biến môi trường).
- Không tắt, bỏ qua, xóa test cho xanh; không sửa test của người khác cho khớp mã của mình.
- Không đổi `pom.xml`, `package.json`, `pubspec.yaml`, `CLAUDE.md`, `common/` khi không phải A.
- Không sửa file sinh tự động (`frontend/src/api/`); chạy lại `npm run gen:api`.
