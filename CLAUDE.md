# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Hệ thống quản lý viện dưỡng lão

Đồ án nhóm quản lý viện dưỡng lão: nghiệp vụ và spec viết theo Spec Kit (`specs/`, `.specify/`); mã nguồn triển khai theo plan và tasks của từng feature.

## Giai đoạn hiện tại: THIẾT KẾ

- Từ 2026-10-16 (constitution IX). Cả 20 feature (000 → 019) đã có spec, plan, tasks và đã qua /speckit-analyze.
- Gồm hai việc: thiết kế giao diện web `frontend/`; thiết kế kỹ thuật (sửa plan, tasks, data-model, contracts khi cần).
- Chưa viết mã nghiệp vụ (backend, gọi API, màn hình chức năng) cho tới khi chủ dự án mở giai đoạn triển khai ở mục này.
- Lệnh: /speckit-plan, /speckit-tasks để sửa; /speckit-analyze, /speckit-converge để rà; /speckit-implement chưa dùng; /speckit-taskstoissues chỉ khi chủ dự án yêu cầu.
- Plan, tasks không được đổi nghiệp vụ (constitution IX). Gặp thiếu sót nghiệp vụ hoặc lệch giữa spec, plan, tasks thì dừng và báo lại.
- /speckit-analyze chạy theo thứ tự triển khai, đổi `.specify/feature.json` trước mỗi feature; chỉ đọc, không ghi file trừ khi chủ dự án yêu cầu.
- Tiến độ (2026-10-16): xong 000 T001 (`.gitignore`, README gốc) và T005a (design system, khung ứng dụng, hai màn mẫu). Đã làm trước một phần, task chưa `[x]`: T002 (driver MySQL, `ddl-auto=validate`), T005 (bỏ file mẫu Vite), T008 (ESLint bỏ qua hai luật cho mã shadcn sinh ra).
- Trước khi vận hành: chốt 3 điểm pháp lý ở 24.4 (Q-03, Thông tư 27/2024/TT-BYT, Luật Cư trú 118/2025/QH15); nâng MySQL 8.0 lên 8.4 LTS, nên migration chỉ dùng cú pháp có ở cả hai bản (research R-01).

### Thiết kế giao diện

- Hướng: chuyên nghiệp, điềm tĩnh, ấm, phục vụ vận hành; không theo khuôn dashboard SaaS chung chung.
  - Màu khởi điểm: xanh teal đậm, trắng ấm, xanh sage nhạt, xám trung tính; khai báo bằng biến CSS của shadcn trong `frontend/src/index.css`, có cả chế độ tối.
  - Font Geist; viền nhẹ, bo góc vừa, khoảng cách nhất quán. Tránh gradient dày, glassmorphism, hiệu ứng động trang trí, thẻ số liệu quá lớn, biểu đồ không cần thiết.
- Ưu tiên danh sách người cao tuổi, lịch chăm sóc, công việc thuốc, sự cố, bàn giao, tình trạng phòng/giường; dùng bảng, dòng thời gian, danh sách việc, khung chi tiết bên cạnh khi hợp luồng làm việc hơn thẻ.
- Màu đỏ chỉ cho quá hạn, Khẩn cấp, "đã leo thang tối đa"; cảnh báo nổi rõ nhưng không làm cả màn hình căng thẳng.
- Menu và nút theo vai trò, căn cứ ma trận quyền (Phụ lục 27, mục 19.3); nút lệnh lấy theo danh sách lệnh được phép do backend trả.
- Nhãn tiếng Việt theo mục 2.4 nghiep-vu.md; ngày dd/MM/yyyy (NFR-11).
- Không bịa quy tắc nghiệp vụ, dữ liệu người cao tuổi hay quyền; dữ liệu minh họa ghi rõ là minh họa, để trong một file riêng.
- Quy trình: đề xuất design system và bố cục → chủ dự án duyệt → mới sửa file; cài từng bước, chạy `npm run build` sau mỗi bước.

## Cách viết mã (bắt buộc cho mọi thành viên, skill karpathy-guidelines)

Skill ở `.claude/skills/karpathy-guidelines/` (nguồn ghi ở `skills-lock.json`); dùng khi viết, xem lại hoặc sửa mã. Khi xung đột thì constitution và tasks thắng.

1. **Nghĩ trước khi viết.** Nói rõ giả định. Có nhiều cách hiểu thì nêu ra, không tự chọn im lặng; chưa rõ thì hỏi. Chưa rõ nghiệp vụ thì tra mã Q-xx và báo lại, không đoán. Có cách đơn giản hơn tasks ghi thì nói trước.
2. **Đơn giản trước.** Chỉ viết đủ mã cho task đang làm; không thêm tính năng, cấu hình, lớp trừu tượng, xử lý lỗi mà tasks, plan không yêu cầu. Giao diện, port chỉ tạo khi contracts/module-interfaces.md có.
3. **Sửa đúng chỗ.** Chỉ đụng file và dòng task cần; không sửa định dạng, mã kế bên; viết theo phong cách xung quanh. Mã chết hoặc lỗi không thuộc task thì báo lại, không tự xóa; chỉ dọn phần do chính thay đổi của mình làm thừa.
4. **Làm theo mục tiêu kiểm chứng được.** Đổi task thành tiêu chí kiểm được (test nào phải lỗi rồi xanh, theo BR, FR, kịch bản quickstart). Chỉ báo xong khi đã chạy test và thấy kết quả; chưa chạy được thì nói rõ.

## Khi vào triển khai

- Thứ tự: 000 → 002 → 001 → 003 → 004 → 005 → … → 019; điều kiện từng feature ở dòng "Prerequisites" đầu tasks.md.
- Mỗi feature:
  1. Từ `develop` mới nhất tạo nhánh `impl/NNN-ten`.
  2. Đặt `.specify/feature.json` thành `{"feature_directory": "specs/NNN-ten"}` (file bị `.gitignore` bỏ qua, mỗi máy tự tạo).
  3. Phụ thuộc feature chưa làm thì dùng bản giả như tasks ghi (thường ở `BT/support/`).
  4. Trước khi viết mã: dịch `contracts/api-NNN.yaml` sang tên tiếng Anh, bổ sung [glossary.md](specs/000-shared-foundation/glossary.md) (research R-03); không thêm bớt endpoint hay trường; xin chủ dự án duyệt.
  5. `/speckit-implement` từng phase; trong mỗi user story viết test trước, test phải lỗi trước khi cài.
  6. Hết phase: chạy test, đánh dấu `[x]` (không đánh dấu task có test chưa chạy được), ghi tiến độ ở mục "Giai đoạn hiện tại", đề nghị commit.
  7. Xong feature: chạy đủ lệnh kiểm ở mục "Lệnh"; mở PR `impl/NNN-ten` → `develop`.
- Thứ tự đọc khi làm task: tasks.md → plan.md → data-model.md → contracts/ → research.md → spec.md → nghiep-vu.md. Quy ước kỹ thuật chung (tên, module, API, thời gian, cơ sở dữ liệu, test, Lombok) ở research R-01 → R-14 của 000.
- Lệch kỹ thuật (tên lớp, thư viện, cách cài) thì sửa plan, tasks cùng lúc với mã và ghi lý do ở plan.

## Công nghệ và cấu trúc mã

- Constitution 3.1.0 (nguyên tắc X–XV), research R-01, R-02 của 000. Thư viện cụ thể ghi ở plan và tasks.
- Yêu cầu môi trường: JDK 25, MySQL 8.0, Node 24 + npm, Flutter stable 3.x. Chưa dùng Docker, Testcontainers; không thay MySQL bằng H2 hay cơ sở dữ liệu nhúng (R-14 cần trigger và quyền tài khoản MySQL).
- Biến môi trường (không ghi mật khẩu vào kho mã): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` cho schema `elder_care`; `TEST_DB_URL`, `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD` cho schema test `eldercare_test`.
- Một kho mã, ba thư mục gốc:
  - `backend/`: Spring Boot 4.1.x, Java 25, Maven wrapper; một khối, RESTful `/api/v1`; gói gốc `com.cambae.backend` chia `common` và `m01` … `m15` (application module của Spring Modulith, chỉ gói `api` công khai). Flyway ở `src/main/resources/db/migration/`; test `*Test` đơn vị, `*IT` tích hợp, `*ArchTest` kiến trúc, bản giả ở `…/support/`. Tasks viết tắt `BE/`, `BT/`, `MIG/`.
  - `frontend/`: React 19, Vite 8, JavaScript (không TypeScript), Tailwind CSS v4, shadcn/ui (Base UI), lucide-react, Geist. `src/{api, common, features}`, `src/components/ui/` (thêm bằng `npx shadcn add`), `src/lib/`; alias `@/` trỏ `src/`; `src/api/` sinh từ `specs/*/contracts/api-*.yaml` (000 T010), không sửa tay. `cn` lấy từ gói npm `cn`.
  - `mobile/`: một app Flutter chung cho nhân viên và người thân, chức năng theo loại tài khoản (Q-346); "app nhân viên", "app người thân" trong spec là app này. `lib/{common, staff, family}` (tasks ghi `chung`, `nhan-vien`, `nguoi-than`); phần nhân viên ghi ngoại tuyến (Q-01).
- Tên trong mã bằng tiếng Anh; tên tiếng Việt trong tài liệu là tên logic, dịch theo glossary (constitution X, R-03).

## Lệnh

- `backend/` (PowerShell dùng `.\mvnw.cmd`):
  - `./mvnw verify`: test đơn vị, tích hợp, kiến trúc, so hợp đồng; một test: `./mvnw test -Dtest=TenLopTest` hoặc `-Dtest=TenLopTest#tenPhuongThuc`;
  - `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`: chạy với dữ liệu mẫu; profile `test`, `demo` có đồng hồ giả lập (NFR-13, 000 T012);
  - `pom.xml` chưa có Flyway, failsafe (000 T002 thêm); `BackendApplicationTests` nạp cả context nên cần MySQL đang chạy và các biến `DB_*`.
- `frontend/`: `npm ci`, `npm run lint`, `npm run build`, `npm run dev`. `npm test` (Vitest) có sau 000 T005; một file: `npx vitest run <đường dẫn>`. Bản mẫu giao diện: `npm run dev`, đổi vai trò ở thanh trên để xem menu từng vai trò.
- `mobile/`: `flutter test`; một file: `flutter test test/<file>_test.dart`; lọc theo tên: `--plain-name "<tên test>"`.

## Phạm vi nghiệp vụ (tóm tắt, căn cứ là docs/business/nghiep-vu.md)

- Vai trò (2.3): Quản lý viện, Trưởng tầng, Bác sĩ (kiêm dinh dưỡng), Điều dưỡng, Nhân viên chăm sóc, Nhân viên bếp, Hành chính, Kế toán, Người thân. Nhân viên không dùng hệ thống chỉ có hồ sơ, không có tài khoản.
- Bác sĩ đánh giá bằng nhận xét rồi chọn mức chăm sóc; không thang điểm, không quy đổi tự động.
- Mức chăm sóc có 3 mức (cơ bản, thường xuyên, đặc biệt); nhu cầu cụ thể là danh mục riêng. Không có cờ gắn cho người cao tuổi. Không dùng cổng thanh toán.
- Hệ thống không tự sinh, tự tạo hay tự gợi ý (Q-306, Q-307): mọi bản ghi do actor lập bằng lệnh. Vẫn giữ: tự chuyển trạng thái theo giờ, cảnh báo và leo thang, nhắc, thông báo, kiểm tra và chặn, tác động kèm lệnh của actor, số liệu tính để hiển thị.
- Kho và tài sản: mỗi bộ phận tự quản lý (13.1a); Quản lý viện chỉ phê duyệt cấp viện (15.10, 7.7, xử lý lệch); Kế toán đối chiếu chứng từ.
- Bán trú không qua đêm; ở lại qua đêm thì thuê giường theo nội trú ngắn ngày.

## Nguồn tài liệu

- `docs/business/nghiep-vu.md`: nguồn duy nhất về quy tắc nghiệp vụ — BR, CFG (Phụ lục 25), quyết định Q-xx (mục 24: 24.1 còn mở, 24.2 đã chốt, 24.4 cần đối chiếu), actor và use case (Phụ lục 26), ma trận quyền (Phụ lục 27; khác thì mục 19.3 là căn cứ), DBR (Phụ lục 28), căn cứ pháp lý (Phụ lục 29).
- `docs/business/luong-nghiep-vu.md`: luồng BF-01 → BF-18 theo từng bước.
- Không sửa hai file trên trừ khi được yêu cầu rõ ràng; thấy mâu thuẫn thì báo lại.
- `.specify/memory/constitution.md` (3.1.0): I–IX cho spec, X–XV cho plan, tasks, mã; sửa phải ghi Sync Impact Report và tăng phiên bản.
- `specs/NNN-ten/`: spec, plan, research, data-model, contracts, quickstart, tasks; NNN theo cột Feature của Phụ lục 26.2. 000 là nền tảng dùng chung (phê duyệt, đính chính, nhật ký, tham số). Chỉ sửa spec khi được yêu cầu; không tự mở lượt clarify hay checklist mới.
- Điểm chưa rõ: không đoán; tra Q-xx ở mục 24 và báo lại. Đọc dải mã: "BR-M01-09, 10" là BR-M01-09 và BR-M01-10; "UC-71 → 73" là dải.
- Script sửa file hàng loạt dùng node (có sẵn vì frontend cần); kiểm tra số lần khớp trước khi ghi.
