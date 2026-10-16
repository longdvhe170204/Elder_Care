# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Hệ thống quản lý viện dưỡng lão

Đồ án quản lý viện dưỡng lão: nghiệp vụ và spec viết theo Spec Kit (thư mục specs/, .specify/); mã nguồn triển khai theo plan và tasks của từng feature.

## Giai đoạn hiện tại: TRIỂN KHAI

- Lịch sử:
  - phân tích xong ngày 2026-10-06;
  - thiết kế từ 2026-10-06 tới 2026-10-10: cả 20 feature (000 → 019) có plan, tasks và đã qua /speckit-analyze; điểm báo lại của plan chốt bằng Q-311 → Q-336;
  - chủ dự án mở giai đoạn triển khai ngày 2026-10-10.
- Công nghệ (Q-305, Q-346, constitution 3.0.0, nguyên tắc X–XV):
  - backend Spring Boot, RESTful API `/api/v1`, MySQL; một khối chia gói theo module M01–M15;
  - web React (TypeScript) có mọi chức năng, gồm quản trị và cổng người thân trên trình duyệt;
  - một app Flutter chung cho nhân viên và người thân, chức năng theo loại tài khoản (Q-346); phần nhân viên ghi ngoại tuyến theo Q-01. "App nhân viên", "app người thân" trong spec, plan là app này dùng bằng tài khoản tương ứng;
  - phiên bản theo môi trường đã cài trên máy chủ dự án (sửa 2026-10-16): Java 25, Spring Boot 4.1.x, MySQL 8.0, TypeScript 5.x, Flutter stable 3.x (research R-01 của 000); thư viện cụ thể ghi ở plan và tasks. Không đổi cài đặt trên máy (JDK, `JAVA_HOME`, MySQL) để khớp tài liệu; tài liệu phải khớp máy.
- Lệnh được dùng:
  - /speckit-implement, /speckit-analyze, /speckit-converge;
  - /speckit-plan, /speckit-tasks chỉ để sửa plan, tasks khi spec hoặc tài liệu nguồn thay đổi;
  - /speckit-taskstoissues chỉ khi chủ dự án yêu cầu (đẩy dữ liệu lên GitHub).
- Tiến độ triển khai (cập nhật mỗi khi xong một phase hoặc feature):
  - 2026-10-16: chưa có task nào `[x]`; feature kế tiếp là 000, Phase 1.
  - Ba thư mục `backend/` (Spring Boot 4.1.1, Java 25, gói `com.cambae.backend`), `frontend/` (React 19 + Vite, đang là JavaScript), `mobile/` (Flutter) đã khởi tạo, chưa commit. Tasks 000 Phase 1 (T001 → T010) hoàn thiện chúng theo plan: thêm Flyway, Modulith, springdoc, ShedLock, ArchUnit; chuyển `frontend/` sang TypeScript; chia `mobile/lib/` thành phần chung, nhân viên, người thân; thêm `.gitignore`.
- Thứ tự triển khai theo phụ thuộc: 000 → 002 → 001 → 003 → 004 → 005 → … → 019; điều kiện của từng feature ghi ở dòng "Prerequisites" đầu tasks.md. Phần lớn feature chỉ cần feature trước "đã xong Phase 2" hoặc "đã có `api`", nên ví dụ 002 bắt đầu được khi 000 xong Phase 1–2.
- Cách làm mỗi feature:
  1. Từ `develop` mới nhất tạo nhánh `impl/NNN-ten` (ví dụ `impl/000-shared-foundation`).
  2. Tạo hoặc đổi `.specify/feature.json` thành `{"feature_directory": "specs/NNN-ten"}`. File này bị `.specify/.gitignore` bỏ qua, nên bản clone mới không có.
  3. Đọc dòng Prerequisites; phần phụ thuộc feature chưa làm thì dùng bản giả như tasks ghi (thường ở `BT/support/`), không bỏ qua task.
  4. Trước khi viết mã của feature: dịch `specs/NNN-ten/contracts/api-NNN.yaml` sang tên tiếng Anh và bổ sung [glossary.md](specs/000-shared-foundation/glossary.md) cho các bảng, lớp, endpoint, mã lỗi của feature (research R-03). Không đổi nghĩa, không thêm bớt endpoint hay trường. Đề nghị chủ dự án duyệt bản dịch trước khi cài.
  5. Chạy `/speckit-implement`, làm lần lượt từng phase của tasks.md; trong mỗi user story viết test trước, test phải lỗi trước khi cài.
  6. Hết mỗi phase: chạy test của các phần đã có, đánh dấu `[x]` task đã xong, cập nhật "Tiến độ triển khai" ở trên, đề nghị chủ dự án commit. Không đánh dấu `[x]` task có test chưa chạy được; ghi rõ lý do.
  7. Xong feature: chạy đủ lệnh kiểm ở mục "Môi trường"; đề nghị mở PR `impl/NNN-ten` → `develop`.
- Thứ tự đọc khi làm một task: tasks.md (task và mã FR, BR, R-xx dẫn kèm) → plan.md → data-model.md → contracts/ (api-NNN.yaml, module-interfaces.md) → research.md → spec.md → docs/business/nghiep-vu.md. Quy ước dùng chung cho mọi feature nằm ở research R-01 → R-14 của 000.
- Mã nguồn:
  - theo constitution X–XV: nghiệp vụ, quyền và điều kiện kiểm ở backend; mỗi lệnh nhóm 2 là một endpoint riêng; bản ghi nhóm 3 chỉ thêm, sai sót dùng đính chính; mọi CFG đọc qua `ThamSoApi`; mọi thông báo qua 009; module gọi nhau qua giao diện ở contracts/module-interfaces.md; mỗi BR có test tự động;
  - tên gói, lớp, bảng, endpoint lấy từ tên logic tiếng Việt của plan, data-model, contracts rồi dịch sang tiếng Anh theo glossary (mục "Quy ước kỹ thuật"); tên test gắn mã (ví dụ `BR_M02_11_DeclarationIT`, `FR_076_…IT`);
  - không đổi nghiệp vụ trong mã. Gặp thiếu sót nghiệp vụ hoặc lệch giữa spec, plan, tasks thì dừng task đó và báo lại; sửa theo thứ tự docs/business/nghiep-vu.md → spec → plan → tasks rồi mới viết mã (constitution I, IX);
  - lệch kỹ thuật (tên lớp, thư viện, cách cài) thì sửa plan, tasks cùng lúc với mã và ghi lý do ở plan.
- Còn mở 3 điểm chờ tư vấn pháp lý (24.4): Q-03, Thông tư 27/2024/TT-BYT, Luật Cư trú 118/2025/QH15. Các ngưỡng liên quan là tham số nên không chặn việc viết mã; cần chốt trước khi vận hành.
- Việc bắt buộc trước khi vận hành về kỹ thuật: nâng MySQL 8.0 (hết hỗ trợ từ 04/2026) lên 8.4 LTS rồi chạy lại toàn bộ test tích hợp; migration chỉ dùng cú pháp có ở cả 8.0 và 8.4 (research R-01).
- Chạy /speckit-analyze theo cùng thứ tự, đổi feature.json trước mỗi feature; analyze chỉ đọc, kết quả trả trong hội thoại, không ghi file trừ khi chủ dự án yêu cầu. Sau analyze từng feature, rà thêm giao diện giữa các feature (mục "bổ sung theo plan này" phải có ở feature cung cấp).
- Xử lý kết quả analyze (sau khi chủ dự án duyệt):
  - lệch ở plan, tasks: sửa plan, tasks;
  - spec lệch tài liệu nguồn: đồng bộ spec, ghi mục "Cập nhật <ngày>" ở Clarifications và một dòng "(Đã xử lý …)" ở "Điểm cần báo lại";
  - thiếu sót nghiệp vụ: báo lại để sửa docs/business/nghiep-vu.md trước;
  - xung đột constitution (CRITICAL): sửa spec, plan hoặc tasks, không sửa constitution trong analyze.
- Chỉ sửa spec khi được yêu cầu, hoặc để đồng bộ khi tài liệu nguồn thay đổi. Không tự mở thêm lượt clarify hay checklist mới.

## Quyết định kỹ thuật ngày 2026-10-16

Chủ dự án chốt: giữ nguyên cài đặt trên máy và dự án đã khởi tạo; tài liệu sửa theo.

| Mục | Quyết định | Ghi ở |
| --- | --- | --- |
| Phiên bản | Java 25, Spring Boot 4.1.x, MySQL 8.0 cài trên máy | constitution X 3.0.0, research R-01 |
| Gói gốc backend | `com.cambae.backend` (giữ như lúc khởi tạo) | research R-02, mọi plan, tasks |
| Web | `frontend/`, chuyển sang TypeScript | tasks 000 T005 |
| App di động | một app `mobile/` chung cho nhân viên và người thân | Q-346, constitution X, research R-02 |
| Tên trong mã | tiếng Anh cho gói, lớp, hàm, bảng, cột, endpoint, trường JSON, mã lỗi; tên tiếng Việt trong tài liệu là tên logic | constitution X, research R-03, glossary.md |
| Docker | chưa dùng; test tích hợp chạy trên MySQL 8.0 của máy | research R-14, tasks 000 T009 |

Rủi ro đã biết: MySQL 8.0 hết hỗ trợ từ 04/2026; phải nâng lên 8.4 LTS trước khi vận hành (mục "Giai đoạn hiện tại", R-01).


## Cách viết mã (theo skill karpathy-guidelines)

Skill nằm ở `.claude/skills/karpathy-guidelines/` (nguồn ghi ở `skills-lock.json`); dùng khi viết, xem lại hoặc sửa mã. Bốn nguyên tắc áp dụng cùng constitution X–XV; khi xung đột thì constitution và tasks thắng.

1. **Nghĩ trước khi viết.** Nói rõ giả định trước khi cài. Task có nhiều cách hiểu thì nêu ra, không tự chọn im lặng. Chưa rõ thì dừng và hỏi; chưa rõ về nghiệp vụ thì tra mã Q-xx và báo lại như mục "Mã nguồn", không đoán. Có cách đơn giản hơn tasks ghi thì nói trước khi làm.
2. **Đơn giản trước.** Chỉ viết đủ mã cho task đang làm. Không thêm tính năng, tùy chọn cấu hình, lớp trừu tượng hay xử lý lỗi cho tình huống không thể xảy ra mà tasks, plan không yêu cầu. SPI, port, giao diện chỉ tạo khi contracts/module-interfaces.md có; bản giả chỉ đủ cho test cần.
3. **Sửa đúng chỗ.** Chỉ đụng file và dòng mà task cần; không sửa định dạng, chú thích, mã kế bên; viết theo phong cách mã xung quanh. Thấy mã chết hoặc lỗi không thuộc task thì báo lại, không tự xóa. Dọn import, biến, hàm do chính thay đổi của mình làm thừa. Mỗi dòng thay đổi phải truy được về task đang làm.
4. **Làm theo mục tiêu kiểm chứng được.** Trước khi cài, đổi task thành tiêu chí kiểm được: test nào phải lỗi rồi xanh, theo BR, FR và kịch bản quickstart nào. Task nhiều bước thì nêu kế hoạch ngắn, mỗi bước kèm cách kiểm. Chỉ báo xong khi đã chạy test và thấy kết quả; test chưa chạy được (ví dụ MySQL trên máy chưa chạy) thì nói rõ là chưa kiểm.

## Môi trường, cấu trúc mã và lệnh

- Máy chủ dự án (kiểm 2026-10-16; không sửa cài đặt máy để khớp tài liệu):
  - JDK 25 là `JAVA_HOME` (máy còn JDK 17, 21 nhưng không dùng); Maven qua `./mvnw` của dự án (PowerShell: `.\mvnw.cmd`).
  - MySQL Server 8.0, dịch vụ Windows `mysql`; schema chạy `elder_care` (biến `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` trong `application.properties`), schema test `eldercare_test` (biến `TEST_DB_URL`, `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD`). Không ghi mật khẩu vào kho mã.
  - Node 24, npm 11, Flutter 3.41.9 stable.
  - Chưa có Docker, chưa dùng Testcontainers. Không thay MySQL bằng H2 hay cơ sở dữ liệu nhúng (R-14 cần trigger và quyền tài khoản MySQL).
- Một kho mã, ba thư mục gốc (plan 000 mục Project Structure, research R-02):
  - `backend/`: `src/main/java/com/cambae/backend/{common, m01 … m15}`, Flyway ở `src/main/resources/db/migration/`, test ở `src/test/java/com/cambae/backend/` (`*Test` đơn vị, `*IT` tích hợp, `*ArchTest` kiến trúc), bản giả và lớp cơ sở test ở `…/support/`; tasks viết tắt `BE/`, `BT/`, `MIG/`;
  - `frontend/`: `src/{api, common, features}`; `src/api/` sinh từ `specs/*/contracts/api-*.yaml` (000 T010), không sửa tay;
  - `mobile/`: `lib/{common, staff, family}`, test ở `test/{staff, family}`; tasks ghi tên logic `chung`, `nhan-vien`, `nguoi-than`.
- File build đã có từ lúc khởi tạo; Phase 1 của tasks 000 hoàn thiện chúng (Flyway, Modulith, TypeScript, `.gitignore`).
- Lệnh:
  - `backend/`: `./mvnw verify` chạy test đơn vị, tích hợp, kiến trúc và so hợp đồng; `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` chạy với dữ liệu mẫu;
  - `frontend/`: `npm ci && npm test && npm run dev`;
  - `mobile/`: `flutter test`.
- Profile `test` và `demo` có đồng hồ giả lập để kiểm quy tắc theo thời gian (NFR-13; task T012 của 000).
- Khi lệnh thực tế khác (ví dụ cách chạy một lớp test), cập nhật mục này.

## Quy ước kỹ thuật khi viết mã (tóm tắt research 000; chi tiết ở R-xx)

- Tên (R-03, constitution X): mã dùng tiếng Anh; tên tiếng Việt trong plan, data-model, contracts, tasks là tên logic, dịch theo [glossary.md](specs/000-shared-foundation/glossary.md). Một thuật ngữ một bản dịch; thuật ngữ mới thêm vào glossary trong cùng thay đổi với mã. Mã nghiệp vụ (BR, FR, CFG, Q…) giữ nguyên.
- Module (R-02): mỗi `m01` … `m15` là một application module của Spring Modulith; chỉ gói `api` (giao diện, DTO, sự kiện) công khai, còn lại nội bộ. Module khác chỉ gọi qua `api` theo contracts/module-interfaces.md; `ApplicationModules.verify()` và ArchUnit kiểm. `common` chỉ chứa phần kỹ thuật.
- API (R-03):
  - đường dẫn tiếng Anh kebab-case dưới `/api/v1` (`/approval-requests/{id}/approve`); trường JSON camelCase tiếng Anh (`reason`, `effectiveDate`);
  - nhóm 1: `POST`, `PUT`, `…/deactivate`, `…/reactivate`, `DELETE` khi chưa được tham chiếu; nhóm 2: chỉ `POST …/{id}/<command>`; nhóm 3: `POST` tạo và `POST …/{id}/corrections`;
  - `ETag` là số phiên bản, lệnh sửa bắt buộc `If-Match`, sai thì 412 kèm `currentValue`; lệnh ghi nhận `Idempotency-Key`;
  - lỗi theo RFC 9457 có `errorCode`, `unmetConditions[]`; mã HTTP: 400 định dạng, 401, 403 quyền, 404 không tồn tại hoặc ngoài phạm vi, 409 điều kiện nghiệp vụ, 412 phiên bản, 422 thiếu lý do hoặc sai giá trị.
- Thời gian (R-04): lấy từ bean `Clock` (`Asia/Ho_Chi_Minh`) qua lớp thời gian nghiệp vụ (`BusinessTime`); cấm `Instant.now()`, `LocalDate.now()`, `NOW()` của MySQL và `Thread.sleep` trong test. Thời điểm lưu `DATETIME(6)` UTC; ngày nghiệp vụ lưu `DATE`.
- Cơ sở dữ liệu (R-05): lược đồ chỉ đổi bằng migration Flyway (`ddl-auto=validate`, không `update`); tên `V{NNN}_{nn}__snake_case.sql` với NNN là số feature; bảng nhóm 3 có trigger chặn `UPDATE`, `DELETE`; hai tài khoản MySQL ứng dụng và loại bỏ dữ liệu theo R-05.
- Nhật ký, tham số, thông báo: nhật ký ghi trong cùng transaction với thay đổi (R-06); giá trị CFG đọc qua API tham số (`ThamSoApi` → `ParameterApi`), ArchUnit cấm hằng `Duration` ở tầng service (R-07); thông báo phát sự kiện tới 009 qua outbox, không gửi trực tiếp (R-09).
- Quyền (R-12, plan 002): endpoint ghi và lệnh nghiệp vụ có chú thích kiểm quyền (`@CanQuyen` → tên tiếng Anh theo glossary); trước khi có 002 dùng bản giả (000 T025).
- Test (R-14): đơn vị cho máy trạng thái và quy tắc thuần; tích hợp chạy trên MySQL 8.0 của máy, schema test riêng; tên phương thức `BR_M15_09_<situation>_<result>` hoặc `FR_037_…` khi không có BR; test so springdoc với `contracts/*.yaml` (bản đã dịch), lệch là lỗi.
- Lombok (R-01, R-05): chỉ dùng `@Getter`, constructor, `@Builder`. Entity nhóm 2, 3 không dùng `@Data`, `@Setter` vì sinh setter công khai, trái R-05 (trạng thái chỉ đổi qua lệnh nghiệp vụ); `NhomDuLieuArchTest` (000 T040) kiểm. Entity nhóm 1 được dùng `@Setter`.


## Phạm vi nghiệp vụ (tóm tắt, căn cứ là docs/business/nghiep-vu.md)

- Vai trò (2.3): Quản lý viện, Trưởng tầng, Bác sĩ (kiêm dinh dưỡng), Điều dưỡng, Nhân viên chăm sóc, Nhân viên bếp, Hành chính, Kế toán, Người thân.
- Đã bỏ từ 2026-10-05:
  - Bỏ hai vai trò Dinh dưỡng viên và Nhân viên vệ sinh, cùng nghiệp vụ vệ sinh. Nhân viên không dùng hệ thống chỉ có hồ sơ nhân viên, không có tài khoản.
  - Bỏ thang điểm và quy đổi tự động: Bác sĩ đánh giá bằng nhận xét rồi chọn mức chăm sóc.
  - Mức chăm sóc chỉ còn 3 mức (cơ bản, thường xuyên, đặc biệt); các nhu cầu cụ thể (hỗ trợ vận động, phục hồi…) là danh mục nhu cầu chăm sóc riêng (Q-291, 2026-10-06).
  - Bỏ mọi cờ gắn cho người cao tuổi: cờ nguy cơ, chỉ định hạn chế hoạt động (Q-287).
  - Không dùng cổng thanh toán.
- Hệ thống không tự sinh, tự tạo hay tự gợi ý (Q-306, Q-307, 2026-10-06): mọi bản ghi do actor lập bằng lệnh; không gợi ý, đề xuất, không tự tạo yêu cầu hay sự cố, không tự thực hiện lệnh thay actor. Vẫn giữ: tự chuyển trạng thái theo giờ, cảnh báo do hệ thống tạo và leo thang, nhắc, thông báo, kiểm tra và chặn, bản ghi và tác động kèm theo lệnh của actor, số liệu tính để hiển thị.
- Kho và tài sản: mỗi bộ phận tự quản lý (13.1a). Quản lý viện không vận hành kho, tài sản; chỉ thực hiện quyền phê duyệt cấp viện (yêu cầu mua sắm 15.10, thanh lý và chuyển tài sản 7.7, xử lý lệch). Kế toán đối chiếu chứng từ.
- Bán trú không qua đêm. Ở lại qua đêm thì thuê giường theo nội trú ngắn ngày.
- Quy mô mã hiện có:
  - 245 BR (BR-Mxx-yy; đã xóa BR-M03-08, 11, 12, 14; thêm BR-M07-24, 25 theo Q-337, Q-340).
  - Quyết định Q-01 → Q-346 (Q-337 → Q-345 chốt 2026-10-11 sau đối chiếu tài liệu gốc DOCX; Q-346 chốt 2026-10-16: một app di động chung).
  - Luồng BF-01 → BF-18.
  - Use case UC-01 → UC-95, trừ UC-71 → 73 (92 UC); actor AC-00 → AC-15, trừ AC-06, AC-08.
  - 118 tham số CFG (đã xóa CFG-M04-08, 09, CFG-M07-03 theo Q-306; CFG-M02-01, CFG-M08-02 theo Q-307; thêm CFG-M11-10 theo Q-335).
  - Quy tắc dữ liệu DBR-01 → DBR-36.

## Nguồn tài liệu

- docs/business/nghiep-vu.md là nguồn duy nhất về quy tắc nghiệp vụ:
  - BR, CFG (Phụ lục 25);
  - quyết định Q-xx (mục 24: 24.1 còn mở, 24.2 đã chốt, 24.4 cần đối chiếu);
  - actor và use case (Phụ lục 26);
  - ma trận quyền (Phụ lục 27; khi khác thì mục 19.3 là căn cứ);
  - DBR (Phụ lục 28);
  - căn cứ pháp lý (Phụ lục 29): văn bản thay đổi thì rà các mục ở cột "Quy tắc dựa vào", đổi quy tắc thì mở Q mới.
- docs/business/luong-nghiep-vu.md mô tả các luồng BF-01 → BF-18 theo từng bước: ai làm, làm gì, trạng thái đổi thế nào, căn cứ.
- Không sửa hai file trên trừ khi được yêu cầu rõ ràng. Phát hiện mâu thuẫn thì báo lại, không tự sửa.
- Không có file dẫn xuất. Các file tom-tat-nghiep-vu.md, quy-tac-nghiep-vu.md, business-rule.md, cau-hoi-ra-soat-van-hanh.md đã xoá ngày 2026-10-05; không tạo lại. Tham chiếu tới chúng trong checklist cũ chỉ là lịch sử.
- docs/phan-tich-yeu-cau.md đã bỏ ngày 2026-09-28. Các tham chiếu cũ "4.1", "4.2", "4.4", "DBR" trong spec tra theo bảng ánh xạ ở đầu Phụ lục 26.
- .specify/memory/constitution.md (3.0.0): nguyên tắc I–IX cho spec, X–XV là nguyên tắc kỹ thuật cho plan, tasks, mã; spec trích dẫn dạng "constitution I". Sửa constitution phải ghi Sync Impact Report và tăng phiên bản.

## Spec Kit và cấu trúc spec

- Lệnh dùng qua skill `/speckit-*` (specify, clarify, checklist, analyze, plan, tasks…); script hỗ trợ ở .specify/scripts/powershell. Phạm vi lệnh được dùng theo constitution IX và mục "Giai đoạn hiện tại".
- .specify/feature.json chỉ feature mà các lệnh `/speckit-*` đang làm việc; kiểm tra hoặc đổi trước khi chạy lệnh cho một spec khác.
- specs/NNN-ten/spec.md, số NNN là cột Feature của Phụ lục 26.2 (bảng UC → feature). 000 là nền tảng dùng chung (yêu cầu phê duyệt, đính chính, nhật ký, tham số) mà các spec khác gọi là "feature 000 FR-xx".
- Mỗi spec gồm:
  - Clarifications: các Session và mục "Cập nhật <ngày>[, lượt n]" ghi thay đổi theo từng đợt Q; đây là lịch sử.
  - Phạm vi, User Scenarios (Given/When/Then), Requirements (FR-xx, mỗi FR kết thúc bằng *(Nguồn: …)*), bảng trạng thái, Key Entities, Success Criteria, Assumptions; đây là nội dung hiện hành. Dòng "Truy vết use case" nối nhóm FR với UC.
  - Bảng giao tiếp giữa các feature (Nhận / Cung cấp / Gửi, kèm số feature).
  - "Điểm cần báo lại về tài liệu nguồn" ở cuối: nhật ký nối thêm theo thời gian, mục đã xử lý gắn "(Đã xử lý <ngày>: …)".
- Trong spec được giữ nhãn lịch sử "(Sửa/Bổ sung/Bỏ, <ngày>, Q-xx)" ngay trong FR; tài liệu nguồn thì không (xem Quy ước).
- specs/*/checklists/ là kết quả checklist cũ, chỉ có giá trị lịch sử.
- Phần lịch sử của spec có thể nhắc mã đã xóa khỏi tài liệu nguồn (AC-06, AC-08, UC-71 → 73, BR-M03-08, 11, 12, 14, CFG-M01-05, CFG-M03-03, 05, 06, CFG-M04-14); đó không phải lỗi. Bản tài liệu nguồn còn ghi chú thay đổi nằm ở commit 3543384.

## Quy ước

- Viết spec bằng tiếng Việt; thuật ngữ theo mục 2.4 docs/business/nghiep-vu.md.
- Giữ nguyên mã BR, DBR, UC, CFG, Q, BF khi trích dẫn. Mã bị bỏ thì xóa khỏi tài liệu nguồn; không đánh lại số và không dùng lại mã đó. Riêng mã Q bị bỏ vẫn giữ một dòng ở 24.2: "Không còn áp dụng, thay bằng Q-xx".
- Tài liệu nguồn chỉ ghi nội dung hiện hành (từ 2026-10-06): sửa thẳng nội dung, không đánh dấu "(Bổ sung/Sửa/Bỏ…)", không giữ nội dung gạch. Lịch sử thay đổi tra theo git và dòng Q ở 24.2. Số mục và số bước của luồng giữ cố định để spec tham chiếu.
- Cách viết dải mã: "BR-M01-09, 10" nghĩa là BR-M01-09 và BR-M01-10; "UC-71 → 73" là dải. Vì vậy không đặt số mục ngay sau mã bằng dấu phẩy ("BR-M01-09, 17" sẽ bị đọc là BR-M01-17); viết "BR-M01-09; mục 17".
- Điểm chưa rõ: không tự đoán; luôn tham chiếu mã Q-xx ở mục 24.
  - Quyết định còn mở đã có Mặc định ở 24.1: ghi "theo mặc định Q-xx" và viết yêu cầu theo giá trị đó.
  - Chưa có Mặc định hoặc chưa có mã Q: đánh dấu [NEEDS CLARIFICATION] kèm mã Q-xx (Q-204).
- Khi chốt một quyết định (từ 24.1, hoặc chủ dự án quyết trực tiếp), làm theo thứ tự:
  1. Ghi dòng vào 24.2 (theo thứ tự số Q), kèm các Q cũ bị thay hoặc sửa.
  2. Viết lại các dòng Q cũ đó theo nội dung hiện hành, hoặc đổi thành "Không còn áp dụng, thay bằng Q-xx".
  3. Cập nhật thân tài liệu và Phụ lục 25 → 29 nếu liên quan.
  4. Cập nhật docs/business/luong-nghiep-vu.md.
  5. Cập nhật bảng 24.4 nếu cần đối chiếu.
  6. Cập nhật mục "Điểm cần báo lại" của các spec liên quan.
- Không đưa diagram vào spec; trạng thái thể hiện bằng bảng.
- Sau mỗi bước spec: nếu yêu cầu còn mơ hồ thì gợi ý bước tiếp theo; nếu không thì ghi hoàn thành, tránh đào quá sâu làm lệch với các spec khác.

## Ghi chú công cụ

- Môi trường không có Python; dùng node cho script sửa file hàng loạt.
- Trong lệnh Bash (kể cả heredoc), chuỗi `\\` có thể bị rút thành `\`. Không dựng regex từ chuỗi có backslash; dùng regex literal hoặc `split`/`startsWith`. Script sửa file phải kiểm tra số lần khớp trước khi ghi.
