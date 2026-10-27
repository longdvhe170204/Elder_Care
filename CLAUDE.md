# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Hệ thống quản lý viện dưỡng lão

Đồ án nhóm quản lý viện dưỡng lão: nghiệp vụ và spec viết theo Spec Kit (`specs/`, `.specify/`); mã nguồn triển khai theo plan và tasks của từng feature.

## Giai đoạn hiện tại: TRIỂN KHAI BACKEND VÀ WEB

- Từ 2026-10-19 (constitution IX), chủ dự án mở triển khai cho `backend/` và `frontend/`, làm theo mục "Khi vào triển khai", bắt đầu từ 000. Trước đó (2026-10-16 → 2026-10-19) là giai đoạn thiết kế; cả 20 feature (000 → 019) đã có spec, plan, tasks và đã qua /speckit-analyze.
- `mobile/`: chủ dự án mở phần **người cao tuổi** ngày 2026-10-24 và mở thêm phần **nhân viên**, **người thân** ngày 2026-10-27: task mobile của mọi feature làm được theo tasks, màn đặt ở `lib/staff/`, `lib/family/`, `lib/resident/` theo mobile/README.md. Phần nền dùng chung ở `lib/common/` do A làm: điều hướng theo loại tài khoản (000 T006, T007), lint (T008b), script sinh client (T010b), hàng đợi ngoại tuyến drift (T100), đăng nhập nhân viên, người thân (002 T046, T047), kết quả đồng bộ và chờ xem lại (002 T060); màn của feature cần các phần này (ghi khi mất mạng, đăng nhập) thì làm sau khi A xong phần đó, trước đó làm được phần trực tuyến và test widget với bản giả. Phần backend, web của một feature không chờ task mobile. Thư viện đã duyệt: `http`, `mobile_scanner`, `local_auth`, `flutter_secure_storage`, `firebase_messaging`, `geolocator`, `url_launcher`, `drift` (hàng đợi ngoại tuyến của phần nhân viên, research R-01, R-13 của 000); thêm gói khác nhờ A. Quyết định kỹ thuật (ChangeNotifier, client viết tay, theme) ở mobile/README.md mục 5.
- Thiết kế vẫn làm song song: giao diện web, mobile; sửa plan, tasks, data-model, contracts khi cần.
- Lệnh: /speckit-implement từng phase của feature đang làm; /speckit-plan, /speckit-tasks để sửa; /speckit-analyze, /speckit-converge để rà; /speckit-taskstoissues chỉ khi chủ dự án yêu cầu.
- Plan, tasks không được đổi nghiệp vụ (constitution IX). Gặp thiếu sót nghiệp vụ hoặc lệch giữa spec, plan, tasks thì dừng và báo lại.
- /speckit-analyze chạy theo thứ tự triển khai, đổi `.specify/feature.json` trước mỗi feature; chỉ đọc, không ghi file trừ khi chủ dự án yêu cầu.
- Nhật ký tiến độ (feature, nhánh, PR, kết quả test, việc còn lại) ở [docs/progress.md](docs/progress.md): hết phase thêm một dòng cuối file; nhật ký chỉ thêm, không sửa, xóa dòng cũ. Không ghi tiến độ vào CLAUDE.md, AGENTS.md (các dòng tiến độ 008 của E trước đây ở file này đã chuyển nguyên văn sang progress.md ngày 2026-10-27).
- Trước khi vận hành: chốt 3 điểm pháp lý ở 24.4 (Q-03, Thông tư 27/2024/TT-BYT, Luật Cư trú 118/2025/QH15); nâng MySQL 8.0 lên 8.4 LTS, nên migration chỉ dùng cú pháp có ở cả hai bản (research R-01).

### Thiết kế giao diện

Mọi quy tắc giao diện cho cả web (`frontend/`) và app di động (`mobile/`) (màu, chữ, bố cục, component, trợ năng, quy trình, skill) ở [DESIGN.md](DESIGN.md) gốc kho; mục 15 riêng cho Flutter. Đọc hết file đó trước khi thiết kế hay sửa màn hình.

## Cách viết mã (bắt buộc cho mọi thành viên, skill karpathy-guidelines)

Skill ở `.claude/skills/karpathy-guidelines/` (nguồn ghi ở `skills-lock.json`); dùng khi viết, xem lại hoặc sửa mã. Khi xung đột thì constitution và tasks thắng.

1. **Nghĩ trước khi viết.** Nói rõ giả định. Có nhiều cách hiểu thì nêu ra, không tự chọn im lặng; chưa rõ thì hỏi. Chưa rõ nghiệp vụ thì tra mã Q-xx và báo lại, không đoán. Có cách đơn giản hơn tasks ghi thì nói trước.
2. **Đơn giản trước.** Chỉ viết đủ mã cho task đang làm; không thêm tính năng, cấu hình, lớp trừu tượng, xử lý lỗi mà tasks, plan không yêu cầu. Giao diện, port chỉ tạo khi contracts/module-interfaces.md có.
3. **Sửa đúng chỗ.** Chỉ đụng file và dòng task cần; không sửa định dạng, mã kế bên; viết theo phong cách xung quanh. Mã chết hoặc lỗi không thuộc task thì báo lại, không tự xóa; chỉ dọn phần do chính thay đổi của mình làm thừa.
4. **Làm theo mục tiêu kiểm chứng được.** Đổi task thành tiêu chí kiểm được (test nào phải lỗi rồi xanh, theo BR, FR, kịch bản quickstart). Chỉ báo xong khi đã chạy test và thấy kết quả; chưa chạy được thì nói rõ.

## Khi vào triển khai

- Đầu mỗi phiên: báo người dùng các dòng có tên họ ở [docs/team-plan.md](docs/team-plan.md) mục 8 (việc người khác đang chờ); cần người khác làm gì thì thêm dòng ở đó trong PR; việc xong thì xóa dòng trong chính PR làm xong, kết quả ghi ở progress.md (quy định ngay trên bảng mục 8).
- Thứ tự (sửa 2026-10-19, nhóm 5 người): 000 xong; 002 công bố gói `api` trước, sau đó các feature làm song song theo [docs/team-plan.md](docs/team-plan.md) (chủ module, đợt, luật bản giả, file dùng chung); điều kiện từng feature ở dòng "Prerequisites" đầu tasks.md, phụ thuộc chưa xong thì dùng bản giả do người khai báo interface viết (`support/fake/<module>/`).
- Mỗi feature:
  1. Từ `develop` mới nhất tạo nhánh `impl/NNN-ten`; chỉ sửa module, thư mục, migration, mục glossary của mình (docs/team-plan.md mục 3, `.github/CODEOWNERS`).
  2. Đặt `.specify/feature.json` thành `{"feature_directory": "specs/NNN-ten"}` (file bị `.gitignore` bỏ qua, mỗi máy tự tạo).
  3. Phụ thuộc feature chưa làm thì dùng bản giả như tasks ghi (thường ở `BT/support/`).
  4. Trước khi viết mã: dịch `contracts/api-NNN.yaml` sang tên tiếng Anh, bổ sung [glossary.md](specs/000-shared-foundation/glossary.md) (research R-03); không thêm bớt endpoint hay trường; xin chủ dự án duyệt.
  5. `/speckit-implement` từng phase; trong mỗi user story viết test trước, test phải lỗi trước khi cài.
  6. Hết phase: chạy test, đánh dấu `[x]` (không đánh dấu task có test chưa chạy được), ghi tiến độ ở [docs/progress.md](docs/progress.md), xóa hoặc sửa dòng đã xong ở team-plan mục 8, đề nghị commit.
  7. Xong feature: chạy đủ lệnh kiểm ở mục "Lệnh"; mở PR `impl/NNN-ten` → `develop`.
- Thứ tự đọc khi làm task: tasks.md → plan.md → data-model.md → contracts/ → research.md → spec.md → nghiep-vu.md. Quy ước kỹ thuật chung (tên, module, API, thời gian, cơ sở dữ liệu, test, Lombok) ở research R-01 → R-14 của 000.
- Đường dẫn file trong tasks, plan là tên logic, chỉ cho biết module và vai trò của lớp; không chép, đặt file theo README của thư mục:
  - backend (`BE/m01/hoso/internal/HoSoService.java`, `BE/m15/taikhoan/quyen/web/…`): [backend/README.md](backend/README.md), research R-02 của 000 mục "Cấu trúc trong module" (`api/{dto, spi, events}`, `web/{request, response}`, `internal/…/{service, domain, persistence, config}`); `LayerArchTest`, `ModuleBoundaryTest` kiểm;
  - web (`frontend/src/tinh-nang/…`): [frontend/README.md](frontend/README.md) (`src/features/…`, tên tiếng Anh);
  - mobile (`mobile/lib/nhan-vien/…`, `nguoi-than`, `nguoi-cao-tuoi`, `chung`): [mobile/README.md](mobile/README.md) (`lib/staff`, `family`, `resident`, `common`).
- Lệch kỹ thuật (tên lớp, thư viện, cách cài) thì sửa plan, tasks cùng lúc với mã và ghi lý do ở plan.

## Công nghệ và cấu trúc mã

- Luật chung cho mọi công cụ AI ở [AGENTS.md](AGENTS.md); cấu trúc và luật thư mục ở [backend/README.md](backend/README.md), [frontend/README.md](frontend/README.md), [mobile/README.md](mobile/README.md).

- Constitution 3.1.1 (nguyên tắc X–XV), research R-01, R-02 của 000. Thư viện cụ thể ghi ở plan và tasks.
- Yêu cầu môi trường: JDK 25, MySQL 8.0, Node 24 + npm, Flutter stable 3.x. Chưa dùng Docker, Testcontainers; không thay MySQL bằng H2 hay cơ sở dữ liệu nhúng (R-14 cần trigger và quyền tài khoản MySQL).
- Biến môi trường (không ghi mật khẩu vào kho mã): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` cho schema `elder_care`; `DB_RETENTION_USERNAME`, `DB_RETENTION_PASSWORD` cho tài khoản `eldercare_retention` của job loại bỏ dữ liệu (000 T004); `DB_MIGRATION_USERNAME`, `DB_MIGRATION_PASSWORD` cho tài khoản chạy Flyway (quyền DDL, tạo tài khoản; thiếu thì dùng `DB_*`; 000 T024); `AWS_REGION`, `ELDERCARE_S3_BUCKET` cho tệp đính kèm trên AWS S3, thông tin đăng nhập AWS theo chuỗi mặc định của SDK (001 research H-13; region chờ chủ dự án chốt); khóa API của nhà cung cấp SMS (009 T026a, chờ chốt nhà cung cấp); `TEST_DB_URL` (ghi cả schema, ví dụ `jdbc:mysql://localhost:3306/eldercare_test`), `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD` cho schema test. Hai tài khoản `eldercare_app`, `eldercare_retention` dùng chung cả máy: test không xóa tài khoản, chỉ gỡ quyền trên schema test, và đặt mật khẩu theo `DB_PASSWORD`, `DB_RETENTION_PASSWORD` nếu có (không có thì ngẫu nhiên, profile demo phải đặt lại mật khẩu); máy chạy cả demo nên đặt hai biến này cả khi chạy test (sửa 2026-10-26, trước đó test `DROP USER` làm mất quyền trên `elder_care`).
- Một kho mã, ba thư mục gốc:
  - `backend/`: Spring Boot 4.1.x, Java 25, Maven wrapper; một khối, RESTful `/api/v1`; gói gốc `com.cambae.backend` chia `common` và `m01` … `m15` (application module của Spring Modulith, chỉ gói `api` công khai); trong module: `api/{dto, spi, events}`, `web/{request, response}`, `internal/{service, domain, persistence, config}` (module lớn: `internal/<chức năng>/{service, domain, persistence, config}`), `scheduling/` theo research R-02 của 000 (sửa 2026-10-21). Flyway ở `src/main/resources/db/migration/`; test `*Test` đơn vị, `*IT` tích hợp, `*ArchTest` kiến trúc, bản giả ở `…/support/`. Tasks viết tắt `BE/`, `BT/`, `MIG/`.
  - `frontend/`: React 19, Vite 8, JavaScript (không TypeScript), Tailwind CSS v4, shadcn/ui (Base UI), lucide-react, font Be Vietnam Pro. `src/{api, common, features}`, `src/components/ui/` (thêm bằng `npx shadcn add`), `src/lib/`; alias `@/` trỏ `src/`; `src/api/` sinh từ `specs/*/contracts/api-*.yaml` (000 T010), không sửa tay. `cn` lấy từ gói npm `cn`.
  - `mobile/`: một app Flutter chung cho nhân viên, người thân và người cao tuổi, chức năng theo loại tài khoản (Q-346, Q-365); "app nhân viên", "app người thân", "app người cao tuổi" trong spec là app này. `lib/{common, staff, family, resident}` (tasks ghi `chung`, `nhan-vien`, `nguoi-than`, `nguoi-cao-tuoi`); phần nhân viên ghi ngoại tuyến (Q-01).
- Tên trong mã bằng tiếng Anh; tên tiếng Việt trong tài liệu là tên logic, dịch theo glossary (constitution X, R-03).

## Lệnh

- `backend/` (PowerShell dùng `.\mvnw.cmd`):
  - `./mvnw verify`: test đơn vị, tích hợp, kiến trúc, so hợp đồng; một test: `./mvnw test -Dtest=TenLopTest` hoặc `-Dtest=TenLopTest#tenPhuongThuc`;
  - `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`: chạy với dữ liệu mẫu; profile `test`, `demo` có đồng hồ giả lập (NFR-13, 000 T012). PowerShell: `.mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"` (tham số có dấu chấm phải đặt trong ngoặc kép, PowerShell 5.1 không có `&&`). Spring Boot không tự đọc `backend/.env` (file mẫu biến, bị `.gitignore` bỏ qua): đặt biến `DB_*` trước khi chạy, hoặc điền vào Environment variables của cấu hình chạy IntelliJ (không tick "Store as project file"); không chạy IntelliJ, PowerShell bằng quyền quản trị (thư mục tạm của Tomcat sẽ thuộc Administrators, lần sau báo "is not owned by");
  - `./mvnw spotless:apply`: định dạng Java (verify kiểm tra định dạng); test `*IT` chạy bằng failsafe ở verify;
  - test nạp context (`BackendApplicationTests`, mọi `*IT`) kế thừa `support/MySqlIntegrationTestBase`, cần MySQL đang chạy và các biến `TEST_DB_*`.
  - `./mvnw clean verify` chạy khoảng 10–15 phút: chạy nền, nạp `backend/.env` (có `DB_PASSWORD`, `DB_RETENTION_PASSWORD`, `TEST_DB_*`) trước khi chạy; lớp test đang đỏ có sẵn trên `develop` ghi ở team-plan mục 8 kèm người sửa, so kết quả với danh sách đó trước khi kết luận;
  - schema `eldercare_test` giữ dữ liệu qua các lần chạy: id lấy từ `TestUsers.nextId()`, fixture không giả định bảng rỗng; job hoặc truy vấn quét toàn bảng (việc định kỳ, tra cứu không giới hạn ngày) phải tự cô lập khỏi dữ liệu sót của test khác (mẫu `CallReminderTestBase` của m13).
- `frontend/`: `npm ci`, `npm run lint`, `npm run build`, `npm run dev`. `npm test` (Vitest); một file: `npx vitest run <đường dẫn>`. `npm run gen:api` sinh `src/api/` từ hợp đồng đã dịch; `npm run format` (Prettier, chưa áp cho mã cũ). Không có backend thì chạy bản mẫu: `npm run dev`, đổi vai trò ở khối người dùng cuối menu để xem menu từng vai trò; có backend thì menu theo quyền thật của tài khoản (`GET /me/permissions`) và chỉ hiện mục đã có màn.
- `mobile/`: `flutter test`; một file: `flutter test test/<file>_test.dart`; lọc theo tên: `--plain-name "<tên test>"`.

## Phạm vi nghiệp vụ (tóm tắt, căn cứ là docs/business/nghiep-vu.md)

- Vai trò (2.3): Quản lý viện, Trưởng tầng, Bác sĩ (kiêm dinh dưỡng), Điều dưỡng, Nhân viên chăm sóc, Nhân viên bếp, Hành chính, Kế toán, Người thân, Người cao tuổi (không bắt buộc; xem và gửi phản hồi về chính mình, nút SOS; Q-365). Nhân viên không dùng hệ thống chỉ có hồ sơ, không có tài khoản.
- Bác sĩ đánh giá bằng nhận xét rồi chọn mức chăm sóc; không thang điểm, không quy đổi tự động.
- Mức chăm sóc có 3 mức (cơ bản, thường xuyên, đặc biệt); nhu cầu cụ thể là danh mục riêng. Không có cờ gắn cho người cao tuổi. Không dùng cổng thanh toán.
- Hệ thống không tự sinh, tự tạo hay tự gợi ý (Q-306, Q-307): mọi bản ghi do actor lập bằng lệnh. Vẫn giữ: tự chuyển trạng thái theo giờ, cảnh báo và leo thang, nhắc, thông báo, kiểm tra và chặn, tác động kèm lệnh của actor, số liệu tính để hiển thị.
- Kho và tài sản: mỗi bộ phận tự quản lý (13.1a); Quản lý viện chỉ phê duyệt cấp viện (15.10, 7.7, xử lý lệch); Kế toán đối chiếu chứng từ.
- Bán trú không qua đêm; ở lại qua đêm thì thuê giường theo nội trú ngắn ngày.

## Nguồn tài liệu

- `docs/business/nghiep-vu.md`: nguồn duy nhất về quy tắc nghiệp vụ — BR, CFG (Phụ lục 25), quyết định Q-xx (mục 24: 24.1 còn mở, 24.2 đã chốt, 24.4 cần đối chiếu), actor và use case (Phụ lục 26), ma trận quyền (Phụ lục 27; khác thì mục 19.3 là căn cứ), DBR (Phụ lục 28), căn cứ pháp lý (Phụ lục 29).
- `docs/business/luong-nghiep-vu.md`: luồng BF-01 → BF-18 theo từng bước.
- Không sửa hai file trên trừ khi được yêu cầu rõ ràng; thấy mâu thuẫn thì báo lại.
- `.specify/memory/constitution.md` (3.1.1): I–IX cho spec, X–XV cho plan, tasks, mã; sửa phải ghi Sync Impact Report và tăng phiên bản.
- `specs/NNN-ten/`: spec, plan, research, data-model, contracts, quickstart, tasks; NNN theo cột Feature của Phụ lục 26.2. 000 là nền tảng dùng chung (phê duyệt, đính chính, nhật ký, tham số). Chỉ sửa spec khi được yêu cầu; không tự mở lượt clarify hay checklist mới.
- Điểm chưa rõ: không đoán; tra Q-xx ở mục 24 và báo lại. Đọc dải mã: "BR-M01-09, 10" là BR-M01-09 và BR-M01-10; "UC-71 → 73" là dải.
- Script sửa file hàng loạt dùng node (có sẵn vì frontend cần); kiểm tra số lần khớp trước khi ghi. File trong thư mục làm việc dùng CRLF: script tách dòng bằng `/\r?\n/` rồi ghi lại `\r\n`; không chạy `node -e` có regex chứa `\\` trong Git Bash; `sed -i` trong Git Bash có thể làm mất CR. Kiểm bằng `git diff --stat` (cả file bị đổi là đã hỏng xuống dòng) hoặc `git ls-files --eol`.
