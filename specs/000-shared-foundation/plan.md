# Implementation Plan: Nền tảng quy tắc nghiệp vụ dùng chung

**Branch**: `000-shared-foundation` | **Date**: 2026-10-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/000-shared-foundation/spec.md`

## Summary

Feature 000 cung cấp năm khung mà mọi module M01–M15 kế thừa:

1. Phân nhóm dữ liệu và thao tác được phép (mục A).
2. Tham số cấu hình có lịch sử, đọc lúc chạy (mục B, UC-69).
3. Vòng đời yêu cầu phê duyệt chung, áp dụng đúng một lần (mục D, UC-14).
4. Đính chính bản ghi đã xác nhận (mục C).
5. Nhật ký hệ thống, lưu giữ và loại bỏ theo chuỗi (mục E).

Hướng kỹ thuật:
- Các khung này nằm trong module M15 của backend Spring Boot một khối (research R-02).
- Bảo đảm nhóm dữ liệu bằng ba lớp: mã, API, cơ sở dữ liệu (trigger, quyền tài khoản MySQL; R-05).
- Nhật ký ghi trong cùng transaction với thay đổi (R-06).
- Tham số đọc lúc chạy; bộ đệm bị xóa sau commit (R-07).
- Yêu cầu phê duyệt có SPI `LoaiYeuCauHandler`. Mỗi lần áp dụng là một transaction riêng, có ràng buộc duy nhất chống áp dụng hai lần (R-10). *(Bổ sung theo plan 004, 2026-10-11, Q-338)* SPI có thêm `gioiHanVaiTroDuyet` (mặc định không giới hạn); 000 áp giới hạn này nhất quán ở lệnh Duyệt, Từ chối, `lenhDuocPhep`, nhắc và BR-M15-09, danh sách người nhận khi gửi duyệt (R-10, task T105).
- Đính chính dùng một bảng chung với SPI `DinhChinhDuocProvider` (R-08).
- Thông báo đi qua sự kiện có outbox tới feature 009 (R-09).

Plan này cũng chốt các quyết định nền cho mọi feature sau (R-01 → R-05): phiên bản, cấu trúc kho mã, quy ước API, thời gian, bảo đảm nhóm dữ liệu.

## Technical Context

**Language/Version**: Java 25 (backend); JavaScript (JSX, ES module) cho web, không dùng TypeScript (constitution X 3.1.0); Dart 3.x với Flutter stable 3.x (một app chung, Q-346). Chi tiết ở research R-01.

**Primary Dependencies**:
- Backend: Spring Boot 4.1.x (Web MVC, Data JPA/Hibernate 7, Security, Validation), Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock.
- Web: React 19, Vite 8, Tailwind CSS v4, shadcn/ui (Base UI), lucide-react, font Be Vietnam Pro (thay Geist theo DESIGN.md gốc kho); TanStack Query, React Router (thêm ở T005).
- App: Flutter, `http` (có sẵn), drift (hàng đợi ngoại tuyến của app nhân viên, thêm ở T006).

**Storage**: MySQL 8.0 (bản cài trên máy), InnoDB, utf8mb4. MySQL 8.0 hết hỗ trợ từ 04/2026: trước khi vận hành phải nâng lên 8.4 LTS rồi chạy lại toàn bộ test tích hợp (research R-01). Migration chỉ dùng tính năng có ở cả 8.0 và 8.4.
- Hai tài khoản: `eldercare_app` (ứng dụng) và `eldercare_luu_tru` (job loại bỏ dữ liệu hết hạn).
- Thời điểm lưu `DATETIME(6)` UTC; ngày nghiệp vụ lưu `DATE` theo Asia/Ho_Chi_Minh (R-04).

**Testing**:
- JUnit 5, AssertJ, MySQL 8.0 cài trên máy cho test tích hợp (chạy cả trigger và quyền tài khoản; R-14);
- ArchUnit, Spring Modulith `verify()`;
- openapi-diff so mã với `contracts/`;
- Vitest và React Testing Library (web), `flutter_test` (app).
- Đồng hồ giả lập cho mọi quy tắc theo thời gian (NFR-13).

**Target Platform**: Backend Linux server (container). Web trên trình duyệt máy tính và điện thoại. App Android, iOS trên điện thoại và máy tính bảng.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung cho nhân viên, người thân và người cao tuổi (Q-346, Q-365).

**Performance Goals**:
- Lệnh của 000 (đổi tham số, quyết định yêu cầu, đính chính) nằm trong ngân sách 2 giây của NFR-02. Mỗi lệnh chỉ thêm một `INSERT` nhật ký vào transaction.
- Tra lịch sử một đối tượng (SC-006) dùng chỉ mục `(doi_tuong_loai, doi_tuong_id, thoi_diem)` và phân trang theo khóa, trên khoảng 22 triệu dòng mỗi năm.
- Job áp dụng yêu cầu đến hạn nằm trong NFR-04 (5 phút).

**Constraints**:
- Mọi thay đổi nhóm 2, 3 có nhật ký trong cùng transaction (FR-047).
- Không có `UPDATE` hay `DELETE` trên bản ghi nhóm 3 đã xác nhận (constitution XII).
- Tham số không viết cứng (XIII).
- Hệ thống không tự sinh dữ liệu nghiệp vụ (FR-010a, Q-306, Q-307). Bộ lập lịch của 000 chỉ có ba job: áp dụng yêu cầu đã duyệt đến hạn, nhắc yêu cầu chờ duyệt, loại bỏ dữ liệu hết hạn. Job của các module khác chỉ được chuyển trạng thái theo thời gian, cảnh báo, leo thang, nhắc; không được `INSERT` bản ghi nghiệp vụ ngoài nhật ký, lần chạy, thông báo.
- Thời gian lấy từ một `Clock` (NFR-09).
- Lệnh của 000 chỉ chạy trực tuyến; 000 chỉ cung cấp khung ngoại tuyến cho module ghi nhận (R-13).

**Scale/Scope**:
- NFR-01: 300 người cao tuổi, 100 nhân viên, 60 người dùng đồng thời, 600 tài khoản người thân.
- 119 mã CFG (Phụ lục 25 hiện hành, gồm CFG-M11-10 theo Q-335 và CFG-M15-11 theo Q-370); 14 loại yêu cầu phê duyệt đã biết (bảng ở spec); khoảng 10 loại bản ghi nhóm 3 có đính chính.
- Một instance backend.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Hai điểm về tài liệu nguồn được ghi ở mục "Điểm cần báo lại" bên dưới; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mọi quyết định thiết kế dẫn FR, BR, DBR. Data-model mục 7 là bảng truy vết DBR, BR |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi. Công nghệ chỉ ở plan và research |
| III. Không mặc định CRUD | Đạt | R-03, R-05: nhóm 2 chỉ có endpoint lệnh; nhóm 3 chỉ thêm và đính chính. "Sửa nháp" yêu cầu là một lệnh có lý do (data-model mục 3) |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3 khớp bảng trạng thái của spec. Mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | Mọi mốc nghiệp vụ đọc `ThamSoApi`. Chu kỳ chạy job (15 phút, 00:05, hằng tháng), hạn giữ `Idempotency-Key` là thông số kỹ thuật, không phải quy tắc nghiệp vụ (xem Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Quyền đi qua `QuyenApi` (002) theo Phụ lục 27 và 19.3. Không thêm vai trò. Đổi tham số chỉ AC-01 |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ. Hai điểm lệch nguồn được báo lại, không tự giải quyết |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-011 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tạo tài liệu thiết kế. Không có mã, `src/`, cấu hình build hay SQL. Không đổi nghiệp vụ |
| X. Kiến trúc | Đạt | Một khối Spring Boot. Gói `m01`…`m15`; 000 ở `m15`. Gói `common` chỉ chứa phần kỹ thuật. REST `/api/v1`; MySQL; hai client (Q-346). Phiên bản ở R-01. NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Mọi kiểm tra BR, chuyển trạng thái, quyền ba lớp nằm ở backend. `lenhDuocPhep` trả cho client chỉ để ẩn hoặc hiện nút. Mỗi lệnh có endpoint `POST …/{id}/<lenh>` riêng |
| XII. Dữ liệu | Đạt | Nhóm 3 chỉ thêm (trigger cùng quyền tài khoản). Nhật ký trong cùng transaction. Thời gian theo R-04. Mỗi DBR liên quan có cách bảo đảm (data-model mục 7) |
| XIII. Tham số | Đạt | `tham_so` cùng `lich_su_tham_so`, đọc lúc chạy. Enum `MaThamSo` sinh từ Phụ lục 25; ArchUnit cấm hằng số thời lượng ở tầng service |
| XIV. Ngoại tuyến, thông báo | Đạt | Khung ngoại tuyến chung (R-13); lệnh của 000 bắt buộc trực tuyến. Thông báo chỉ qua sự kiện tới 009 (R-09) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR-M15-03, 04, 06 → 13 có test tích hợp ở tầng backend hoặc cơ sở dữ liệu (data-model mục 7) |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Các trạng thái mở rộng của 001 và 015 chỉ được dành chỗ trong SPI; nội dung do plan của hai feature đó quyết.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 000 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | Ước lượng khối lượng nhật ký; chỉ mục; phân vùng theo năm (R-06) |
| NFR-02 Hiệu năng | Có | Mỗi lệnh thêm một `INSERT` nhật ký; tham số đọc từ bộ đệm trong tiến trình (R-07) |
| NFR-03 Thông báo khẩn cấp | Gián tiếp | 000 không phát sự kiện khẩn cấp. Outbox giao sau commit, không chặn đường khẩn cấp của 007, 009 |
| NFR-04 Tác vụ định kỳ | Có | Job áp dụng yêu cầu: chạy lại không tạo trùng (`ket_qua_ap_dung` khóa chính theo yêu cầu, R-10) |
| NFR-05 Khả dụng | Có | Job chạy lúc 00:05 và lúc khởi động (bắt kịp sau gián đoạn). Bảo trì ngoài khung giao ca là việc vận hành |
| NFR-06 Sao lưu | Gián tiếp | Sao lưu MySQL hằng ngày giữ 30 ngày (vận hành). Lưu ý: bản khôi phục không được làm sống lại dữ liệu đã loại bỏ; quy trình ghi ở task triển khai |
| NFR-07 Lưu trữ | Có | Job loại bỏ theo chuỗi, tài khoản riêng, nhật ký tóm tắt (R-11) |
| NFR-08 Bảo mật | Có | Che trường nhật ký ở backend (R-06); người thân bị chặn khỏi nhật ký. HTTPS và băm mật khẩu ở 002 |
| NFR-09 Thời gian | Có | Một `Clock`, múi giờ Asia/Ho_Chi_Minh; ArchUnit cấm `now()` (R-04) |
| NFR-10 Thiết bị | Có | Màn tham số và tra nhật ký ở web. Màn duyệt yêu cầu ở web và app nhân viên. Người thân và người cao tuổi không dùng chức năng nào của 000 |
| NFR-11 Ngôn ngữ | Có | API trả ISO-8601; client hiển thị dd/MM/yyyy, giao diện tiếng Việt |
| NFR-12 Cấu hình | Có | Đổi tham số không cần triển khai lại (R-07, SC-004) |
| NFR-13 Kiểm thử | Có | `DieuKhienDongHo`, endpoint chỉ ở profile test và demo (R-04) |

## Project Structure

### Documentation (this feature)

```text
specs/000-shared-foundation/
├── plan.md                      # File này
├── research.md                  # Phase 0: R-01 → R-14
├── data-model.md                # Phase 1: quy ước nhóm dữ liệu, bảng của 000, truy vết DBR/BR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-000.yaml             # OpenAPI 3.1: tham số, yêu cầu phê duyệt, đính chính, nhật ký, đồng hồ kiểm thử
│   └── module-interfaces.md     # Giao diện Java giữa module: 000 cung cấp / 000 cần
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc cho giai đoạn triển khai. Tên thư mục, gói dưới đây là tên logic; khi viết mã dịch sang tiếng Anh theo [glossary.md](glossary.md) (R-03). Ba thư mục gốc `backend/`, `frontend/`, `mobile/` đã được khởi tạo (sửa 2026-10-16, constitution 3.0.0; web giữ JavaScript theo constitution 3.1.0, file `.jsx`/`.js`, alias `@/` trỏ `frontend/src/`).

```text
backend/
├── src/main/java/com/cambae/backend/
│   ├── common/                  # Clock, ThoiGianNghiepVu, LyDo, @NhomDuLieu, @LenhNghiepVu,
│   │                            # ChiThemRepository, LoiNghiepVu, Problem Details, Idempotency, PhongBiNgoaiTuyen
│   ├── m01/ … m14/              # module nghiệp vụ (feature 001, 003–019 theo Phụ lục 26.2)
│   └── m15/
│       ├── thamso/   {api, internal, web}
│       ├── nhatky/   {api, internal, web}
│       ├── pheduyet/ {api, internal, web, lich}
│       ├── dinhchinh/{api, internal, web}
│       ├── luutru/   {api, internal, lich}
│       └── taikhoan/ …          # feature 002 (plan sau)
├── src/main/resources/db/migration/   # Flyway (giai đoạn triển khai)
└── src/test/java/com/cambae/backend/        # cùng cấu trúc; *Test (đơn vị), *IT (tích hợp MySQL), *ArchTest

frontend/
└── src/
    ├── api/                     # client sinh từ OpenAPI
    ├── components/ui/           # component shadcn/ui (thêm bằng `npx shadcn add`)
    ├── lib/                     # tiện ích dùng chung (`cn`)
    ├── chung/                   # xử lý Problem Details, 412, hiển thị "đã thay đổi";
    │                            # dinh-chinh/: component đính chính dùng chung (T106, FR-028, SC-007)
    └── tinh-nang/
        ├── tham-so/
        ├── nhat-ky/
        └── yeu-cau-phe-duyet/

mobile/                          # một app chung cho nhân viên, người thân, người cao tuổi (Q-346, Q-365)
├── lib/
│   ├── chung/                   # đăng nhập, API client, hàng đợi ngoại tuyến (drift), phong bì đồng bộ
│   ├── nhan-vien/yeu-cau-phe-duyet/
│   ├── nguoi-than/              # 000 không có màn hình cho người thân
│   └── nguoi-cao-tuoi/          # 000 không có màn hình cho người cao tuổi (bổ sung 2026-10-19)
└── test/{nhan-vien, nguoi-than, nguoi-cao-tuoi}/
```

**Structure Decision**: Một kho mã, ba thư mục gốc (R-02). Feature 000 nằm ở `backend/…/m15/{thamso, nhatky, pheduyet, dinhchinh, luutru}` và `backend/…/common`. Ở web có ba màn: tham số, nhật ký, hộp duyệt yêu cầu. App nhân viên có hộp duyệt yêu cầu, cần cho Trưởng tầng duyệt yêu cầu của 015. Module khác chỉ dùng 000 qua `contracts/module-interfaces.md`.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Gói `com.cambae.backend.common` ngoài M01–M15 | Cần chỗ cho thành phần kỹ thuật mọi module dùng (Clock, lỗi, chú thích nhóm dữ liệu). Gói không chứa dữ liệu nghiệp vụ, Modulith coi là gói dùng chung | Đặt trong `m15` thì mọi module phụ thuộc phần nội bộ của M15 |
| Bảng kỹ thuật `chuoi_dinh_chinh`, `khoa_lap_lai`, `lan_chay_lich`, `event_publication`, `shedlock` được `UPDATE` và không ghi nhật ký nghiệp vụ | Đây là dữ liệu dẫn xuất hoặc hạ tầng, không thuộc ba nhóm của mục 1.5. Mọi thay đổi nghiệp vụ tương ứng (thêm đính chính, áp dụng yêu cầu) đã có nhật ký riêng | Ghi nhật ký cho cả bảng kỹ thuật làm nhật ký phình to mà không thêm thông tin truy vết |
| Cột `da_nhac_luc`, `da_bao_qlv_luc` trên `yeu_cau_phe_duyet` do Bộ lập lịch đặt | Chống nhắc lặp. Mỗi lần nhắc hoặc báo vẫn ghi nhật ký `YC_NHAC` / `YC_BAO_QLV`, người thực hiện là Bộ lập lịch, có căn cứ CFG (FR-045) | Suy ra từ `event_publication` thì phụ thuộc bảng của thư viện |
| Chu kỳ chạy job và hạn giữ `Idempotency-Key` đặt trong cấu hình ứng dụng, không phải mã CFG | Đây là thông số vận hành, không có trong Phụ lục 25 và không đổi quy tắc nghiệp vụ. Mốc nghiệp vụ (CFG-M15-05, 06) vẫn đọc từ `tham_so`. Chu kỳ 15 phút làm nhắc trễ tối đa 15 phút so với mốc | Thêm mã CFG mới thì phải sửa tài liệu nguồn chỉ vì một thông số kỹ thuật |

## Ghi chú triển khai *(2026-10-19, Phase 1)*

- Phiên bản đã chốt (R-01): Spring Boot 4.1.1, Spring Modulith 2.1.1, springdoc-openapi 3.1.1 (`webmvc-api`, không kèm giao diện), ShedLock 7.10.1, ArchUnit 1.5.1, openapi-diff 2.1.7; Flyway, JUnit, AssertJ, Lombok theo Spring Boot. Web: TanStack Query 5, React Router 8, Vitest 5, Testing Library, Prettier.
- Gói `common` khai báo `@ApplicationModule(type = OPEN)` để mọi module dùng trực tiếp (khớp Complexity Tracking).
- Tài khoản `eldercare_retention` (tên logic `eldercare_luu_tru`) đọc từ hai biến môi trường mới `DB_RETENTION_USERNAME`, `DB_RETENTION_PASSWORD` (khóa `eldercare.datasource.retention.*`); bean DataSource tạo ở T071.
- `TEST_DB_URL` ghi cả tên schema (ví dụ `jdbc:mysql://localhost:3306/eldercare_test`, thiếu thì dùng `eldercare_test`). Lớp cơ sở T009 tạo lại hai tài khoản với mật khẩu ngẫu nhiên mỗi lần chạy và cấp `SELECT` mức schema (đủ để kết nối, đọc); Flyway chạy bằng tài khoản quản trị, nên quyền `INSERT`, `UPDATE`, `DELETE` theo bảng của hai tài khoản nằm trong migration của chính bảng đó. Môi trường dựng sẵn (T024) cũng cấp `SELECT` mức schema như vậy. `BackendApplicationTests` kế thừa lớp này để không cần CSDL thật.
- Định dạng: backend dùng Spotless (palantir-java-format), `./mvnw verify` kiểm tra. Web có Prettier (`npm run format`) nhưng chưa định dạng lại mã cũ (37/65 file lệch), chờ chủ dự án quyết để tránh xung đột với nhánh khác.
- Client web sinh bằng `npm run gen:api` (`frontend/scripts/gen-api.mjs`): chỉ đọc hợp đồng của feature có trong danh sách `TRANSLATED`; tên hàm suy từ phương thức và đường dẫn vì hợp đồng không có `operationId` (ví dụ `postApprovalRequestsByIdApprove`).

**Nhóm Thời gian (T011 → T014, 2026-10-19)**:
- `TestClockControl` là bean `Clock` ở profile test, demo: chạy theo đồng hồ máy cộng độ lệch; `setTo`, `advanceBy` đổi độ lệch, `simulated` là có độ lệch khác 0. `BusinessTime.isWithin` nhận `to = null` là khoảng còn mở (khớp `hieu_luc_den NULL`).
- `POST /testing/clock` với `runJobs` trả 422 cho tới khi T071, T093, T094 đăng ký job của mình vào controller.
- `TimeArchTest` cấm thêm `LocalTime.now()`, `ZonedDateTime.now()`, `OffsetDateTime.now()` (R-04 "không mã nào gọi now()"). Thêm `TestClockControllerIT` để kiểm T012 (tasks không ghi test riêng).
- Surefire loại `**/*IT.java` (tên `TestClockControllerIT` khớp mẫu `Test*` của surefire).

**Nhóm Lỗi, API, chống lặp (T015 → T021, 2026-10-19)**:
- Lỗi nghiệp vụ là `common.error.BusinessException` (có `status`, `errorCode`) và sáu lớp con theo T015, thêm `NotFoundException` (404 "Không tìm thấy", không lộ tồn tại, cho T016). Module cần mã riêng (`RECORD_VOIDED`, `CATALOG_ITEM_IN_USE`…) dùng thẳng `BusinessException`.
- `ProblemDetailsHandler` kế thừa `ResponseEntityExceptionHandler`: lỗi của framework (400, 404, 405…) cũng là problem+json có `errorCode` bằng tên trạng thái HTTP.
- `VersionEtag`: ETag dạng `"3"`; If-Match nhận `"3"`, `W/"3"`, `3`; If-Match không đọc được coi như sai phiên bản (412). Thiếu If-Match thì controller khai `@RequestHeader` bắt buộc nên Spring trả 400.
- `IdempotencyFilter` (bộ lọc servlet chạy sau Spring Security) áp cho POST, PUT, PATCH, DELETE có header: khóa theo (khóa, `Authentication.getName()`) thay cho `nguoi_dung_id`; từ 002 T011 tên này là id tài khoản (`SessionAuthentication`) vì `common` không được phụ thuộc `m15` (Modulith cấm vòng phụ thuộc) và 002 chưa có; chỉ lưu kết quả 2xx (lệnh lỗi thì bỏ khóa để gửi lại được); cùng khóa khác nội dung thì 422 `IDEMPOTENCY_KEY_REUSED`, lần đầu đang chạy thì 409 `IDEMPOTENCY_IN_PROGRESS`; trả lại cả header `ETag`, `Location`.
- Migration `V000_01__idempotency_key.sql` cấp `INSERT, UPDATE, DELETE` trên bảng này cho `eldercare_app` (bảng kỹ thuật).
- Test thêm ngoài tasks để kiểm nhóm này: `ReasonTest`, `VersionEtagTest`, `ProblemDetailsHandlerIT`, `IdempotencyFilterIT`; controller mẫu cho test ở gói `com.testsupport` (ngoài vùng quét bean, nạp bằng `@Import`).

**Nhóm Khung nhóm dữ liệu, bản giả quyền (T022 → T025, 2026-10-19)**:
- `AppendOnlyRepository` (chỉ `append`, `findById`, `existsById`) chạy nhờ lớp cơ sở `BaseJpaRepository` khai cho mọi repository ở `common.data.JpaConfig` (`@EnableJpaRepositories(repositoryBaseClass = …)`). `append` dùng `persist` nên bản ghi đã có bị từ chối, không bị `merge` đè.
- T024 thành migration Flyway `V000_00__database_accounts.sql` chạy mọi môi trường: `CREATE USER IF NOT EXISTS` hai tài khoản (mật khẩu từ placeholder `appPassword` = `DB_PASSWORD`, `retentionPassword` = `DB_RETENTION_PASSWORD`) và cấp `SELECT` mức schema; ở test tài khoản đã do lớp cơ sở tạo nên không đổi. Quyền `INSERT`, `UPDATE`, `DELETE` (danh sách của T024) cấp trong migration của từng bảng (T026, T029, T053…), vì bảng chưa có lúc chạy `V000_00`.
- Flyway chạy bằng tài khoản riêng `DB_MIGRATION_USERNAME`, `DB_MIGRATION_PASSWORD` (thiếu thì dùng `DB_*`), vì `eldercare_app` không có quyền DDL và tạo tài khoản.
- Test có thêm thư mục migration `db/test-migration` (bảng `sample_record`, chỉ cấp `INSERT`) để kiểm `AppendOnlyRepositoryIT`, gồm trường hợp `eldercare_app` bị MySQL chặn `DELETE`.
- T025: giao diện đặt ở `m15.account.api` theo chữ ký 002 đã chốt (tham số đối tượng là `Target`, tương ứng `DoiTuong`), chỉ phần 000 dùng; `PermissionKey` tạm dùng hai chuỗi, 002 đổi sang enum. Thêm `Role.RESIDENT` (AC-16) theo Q-365. Bản giả `support/FakePermissions` và `FakePermissionsTest`.

**Nhóm Nhật ký – phần ghi (T026 → T028, T039, 2026-10-19)**:
- `V000_02__audit_log.sql`: phân vùng `RANGE COLUMNS(occurred_at)` theo năm (2026 → 2030 và `p_max`; T102 ghi việc thêm phân vùng hằng năm), `CHECK` `actor_id` khi `USER`, hai trigger mẫu nhóm 3. Trigger xóa dùng `USER()` thay `CURRENT_USER()` (data-model mục 1.4 đã sửa) và không cho xóa dòng `exempt_from_purge`. Cấp `INSERT` cho `eldercare_app`, `DELETE` cho `eldercare_retention`.
- Hành động là `AuditAction(code, requiresReason)` do module khai, để `build()` kiểm được lý do (FR-044). Kiểm "Bộ lập lịch phải có `basis`" (FR-045) đặt ở `write()` vì builder chưa biết người thực hiện.
- `AuditLogService` ghi bằng JDBC trong transaction của lệnh; người thực hiện lấy từ bean `CurrentUser` qua `ObjectProvider` (002 cung cấp; chưa có thì `write` lỗi). Cột `role` lưu một mã AC: tài khoản nhiều vai trò lấy vai trò đầu theo thứ tự enum, 002 có thể đổi. Thời điểm truyền dạng `LocalDateTime` UTC (không dùng `Timestamp`, vì `Timestamp` bị driver đổi múi giờ theo JVM).
- T039 làm cùng nhóm này (`AuditLogAtomicityIT`); thêm `AuditLogTableIT` (phân vùng, quyền, trigger), `AuditLogSchedulerIT` (FR-045), `AuditLogEntryTest`.

**Nhóm Tham số – phần đọc (T029 → T032, 2026-10-19)**:
- `V000_03__parameter.sql`: `parameter` (nhóm 1) và `parameter_history` (nhóm 3, `UNIQUE (code, effective_from)`, khóa ngoại tới `parameter`, `CHECK` lý do khi có người thực hiện, hai trigger mẫu). `eldercare_app` chỉ có `UPDATE` trên `parameter` (không thêm, xóa mã; FR-011) và `INSERT` trên lịch sử.
- `ParameterCode` (119 hằng) và `V000_04__parameter_initial_load.sql` sinh bằng `backend/tools/parameters/gen-parameters.cjs` từ chính Phụ lục 25 (mô tả, cột "Dùng tại" giữ nguyên chữ); `ParameterCodeTest` đối chiếu enum với Phụ lục 25 mỗi lần chạy test. Phân loại: 78 thời lượng, 28 bảng, 8 số nguyên, 4 tỷ lệ, 1 số tiền.
- Giá trị dạng bảng: theo cấu trúc đã chốt ở plan sở hữu khi có (002: CFG-M15-01, 09, 10; 007: CFG-M06-03 nạp `{}` vì 007 nạp khoảng của từng chỉ số); còn lại 000 nạp cấu trúc tạm từ cột "Mặc định" (ví dụ CFG-M02-05 là bảng 6.7 theo Q-08), feature sở hữu chốt lại bằng migration của mình khi dịch hợp đồng.
- "Theo cơ sở", "Viện nhập khi cài đặt" (CFG-M03-01, CFG-M09-01, CFG-M09-12, CFG-M11-10; giờ bữa trong CFG-M08-04) nạp `null`: không tự điền số ví dụ; đọc khi chưa đặt thì `IllegalStateException` nêu mã. Quản lý viện đặt sau khi cài.
- `ParameterValue` thêm `asPeriod()` (ngoài danh sách T032) vì tháng, năm (CFG-M08-09, CFG-M14-01, CFG-M15-03, CFG-M15-04) không đổi được sang `Duration`.
- Bộ đệm `read()` chưa có đường xóa; T079 (đổi giá trị) xóa ở pha AFTER_COMMIT.
- Nạp ban đầu ghi `effective_from = UTC_TIMESTAMP(6)` của MySQL (lúc cài), không đi qua `Clock` của ứng dụng.

**Nhóm Sự kiện, lịch, tệp, kiến trúc (T033 → T038, 2026-10-19)**:
- `Role` chuyển từ `m15.account.api` sang `common`: khuôn thông báo ở `common` (009 B-02) dùng vai trò; để ở `m15` thì `common` ↔ `m15` thành vòng, `ModuleBoundaryTest` chặn.
- T033: thêm `spring-modulith-starter-jdbc`; bảng `EVENT_PUBLICATION` lấy nguyên schema v2 MySQL của thư viện (giữ tên viết hoa cho MySQL trên Linux); cấu hình ở `application.yml` (không cần lớp `SuKienConfig`), giao lại sự kiện chưa xong khi khởi động. `EventPublicationIT` kiểm lưu và giao sau commit, không giao khi rollback.
- T035: `EVENT_PUBLICATION`, `shedlock`, `scheduler_run` cùng ở `V000_05__scheduling.sql`, để không chiếm số `V000_06`, `V000_07` đã dành cho đính chính và yêu cầu phê duyệt (Flyway không áp được bản số nhỏ hơn sau bản số lớn). ShedLock dùng giờ MySQL (thông số kỹ thuật). `SchedulerRuns.start/finish` cấp `schedulerRunId` cho `Basis`.
- T034: `common.notification` dựng phần khung của 009 contracts mục 1.1 (yêu cầu, mức, loại thông tin, nhóm nguồn, Q-354) và hai nhóm người nhận 000 dùng (`FacilityRole`, `Approvers`); các biến thể khác của `RecipientGroup` và `TRUONG_SUC_KHOE` do 009 T004 làm.
- T036: chỉ giao diện `FileApi`, `FilePermissionProvider` ở `common.file`; bảng `tep`, dịch vụ, controller thuộc 001 T003, T004. `canDownload(ownerId)` không nhận người dùng vì `common` không phụ thuộc `m15`; module cài tự lấy người dùng hiện tại.
- T038 thành `ApiContractIT` (cần context và MySQL): so OpenAPI của springdoc với hợp đồng sau khi thêm tiền tố `/api/v1`; endpoint chưa cài ghi ở `PENDING` (phải rỗng khi xong 000). openapi-diff chỉ báo thay đổi "không tương thích" nên test tự kiểm cấu trúc (trường, kiểu, enum, trường bắt buộc, mã phản hồi, kiểu nội dung); đã thử đổi tên một trường để chắc test bắt được. springdoc đặt `default-produces-media-type: application/json`; `runJobs` của đồng hồ giả lập đổi sang enum cho khớp hợp đồng.

**Phase 3 – US1 (T040 → T048, 2026-10-19)**:
- Dữ liệu mẫu (T048) ở `db/migration/demo/V000_90__sample_data.sql`, chạy ở `demo` và `test`: `sample_catalog` (nhóm 1), `sample_case` và `sample_case_version` (nhóm 2), `sample_floor_record` (gắn tầng), `sample_office_record` (không gắn tầng) có trigger. Bảng tạm `sample_record` của nhóm 3 (T023) bỏ, test chuyển sang bảng mẫu. Entity, service mẫu chỉ ở mã test (`m15.sample`); controller mẫu ở `com.testsupport`, nạp bằng `@Import` để `ApiContractIT` không thấy. Profile `demo` đặt `flyway.out-of-order: true` vì `V000_90` có trước `V000_06`….
- T046: `CatalogBase` có `deactivate`, `reactivate(Optional<mục cùng tên đang Hiệu lực>)`; trùng tên trả 409 `CONDITION_NOT_MET` có điều kiện `BR-M15-06` nêu mục trùng. Ghi nhật ký FR-043 do service của module (mẫu ở `SampleService`). `ProblemDetailsHandler` đổi lỗi MySQL 1451 (khóa ngoại RESTRICT) thành 409 `CATALOG_ITEM_IN_USE`; lỗi nghiệp vụ ném khi đọc thân request (ví dụ lý do chỉ có khoảng trắng) trả đúng mã của nó thay vì 400.
- Thêm `BusinessCommandAspect` (ngoài tasks): lệnh `@BusinessCommand(requiresReason = true)` có tham số `Reason` null thì 422 `REASON_REQUIRED` trước khi mở transaction (T044 "trước khi chạm dữ liệu"); `@Order(0)`, chạy trước `@Transactional`.
- T047: `EffectiveVersioning.addVersion` khóa dòng cha `SELECT … FOR UPDATE` (tài khoản ứng dụng cần `UPDATE` trên bảng cha), kiểm giao khoảng (ngày lịch, gồm hai đầu, `to` null là mở), trùng thì 409 `CONDITION_NOT_MET` (`BR-M15-07`). `EffectiveVersionNoOverlapIT` chạy hai luồng đồng thời.
- T040 dùng ArchUnit quét cả mã test (để có entity mẫu). T043 đọc `information_schema.TRIGGERS` bằng tài khoản quản trị vì `eldercare_app` không có quyền `TRIGGER` nên không thấy trigger. Quy ước và mẫu migration ở `backend/docs/data-group-conventions.md` (T045).

**Phase 4 – US2 đính chính (T049 → T060, 2026-10-19)**:
- Xong cách A, B; cách C (bản ghi không gắn tầng, hoặc gắn tầng mà không có người phụ trách ca, trưởng tầng) làm sau US5: xem mục "Phase 7" bên dưới.
- Bộ chọn: A khi là người ghi gốc và còn quyền ghi nhận; B khi là người phụ trách ca hoặc trưởng tầng được giao của tầng và trong phạm vi (gồm CHK033: không có người phụ trách ca thì trưởng tầng được giao); C khi cùng vai trò với người ghi gốc, có quyền ghi nhận, và bản ghi không gắn tầng hoặc tầng không có ai cho B; còn lại 403 `CORRECTION_NOT_ALLOWED`. "Cùng vai trò" kiểm bằng `AccountApi.hasRole`.
- Chuỗi đính chính tuần tự bằng khóa dòng `correction_chain` (`INSERT IGNORE` rồi `SELECT … FOR UPDATE`), không dùng khóa lạc quan có thử lại; `CorrectionConcurrencyIT` chứng minh số thứ tự liên tiếp.
- SPI thêm `originalContent(id)` (hợp đồng trả `originalContent`); module-interfaces mục 1.6 đã ghi bổ sung. `Actor.fullName` trả `null` cho tới khi 002 có hồ sơ tài khoản.
- `CorrectionService` lấy `CurrentUser`, `ScopeApi`, `AccountApi` qua `ObjectProvider` (002 chưa có); test nạp `CorrectionFixtures` (bản giả quyền, `SwitchableUser`). Provider mẫu `SampleFloorRecordProvider` (`M15.SAMPLE_RECORD`, gắn tầng) và `SampleOfficeRecordProvider` (không gắn tầng) ở mã test.
- `ApiContractIT`: thêm `ProblemBody` mô tả schema `Problem` cho annotation phản hồi lỗi; kiểm "đổi kiểu" so kiểu gốc, bỏ qua `format` (int32, int64) và `null` của OpenAPI 3.1. Hai lệch thật đã sửa: `actor.type` là enum, header `Idempotency-Key` kiểu UUID.
- T106 (component web đính chính): xem mục "T106" bên dưới.

**T106 – component web đính chính (2026-10-19, chủ dự án duyệt bố cục)**:
- Đặt ở `frontend/src/common/correction/` (tasks ghi `src/chung/dinh-chinh/`; DESIGN.md mục 7 và glossary đặt phần dùng chung ở `src/common/`): `CorrectionBadge` ("Đã đính chính", "Đã hủy ghi nhận"), `CorrectionHistorySheet` (giá trị hiện hành, các lần đính chính xếp theo `sequenceNo` tăng dần, nội dung gốc nguyên văn), `CorrectionForm`. Dùng client sinh `getCorrectionsBy…`, `postCorrectionsBy…`; không đổi hợp đồng.
- `CorrectionChain` không có lệnh được phép, nên màn của module truyền `canCorrect` (mặc định `false`, form ẩn) theo lệnh được phép của chính bản ghi; đây không là cơ chế bảo mật, backend vẫn kiểm (403 `CORRECTION_NOT_ALLOWED`).
- Schema của `correctedContent` do provider từng loại quyết, nên màn của module truyền `fields: [{ name, label }]`; form gửi `correctedContent` gồm đúng các trường đó, "Hủy ghi nhận" không gửi `correctedContent`. `M15.SAMPLE_RECORD` dùng `note`, nhãn "Ghi chú".
- Phản hồi: 201 tải lại chuỗi và gọi `onChanged(chain)` để màn cập nhật nhãn; 202 (cách C) báo "Đã gửi duyệt, mã yêu cầu …", không gọi `onChanged`; 403, 409 và lỗi mạng hiện `Alert` trong form kèm `unmetConditions`; 422 báo tại ô (`REASON_REQUIRED` ở ô Lý do, còn lại ở ô nội dung). Lý do trống hoặc chỉ khoảng trắng báo tại ô, không gọi API.
- Test `CorrectionSheet.test.jsx` (Vitest, giả `fetch`), 17 trường hợp. Thêm `afterEach(cleanup)` vào `frontend/src/test/setup.js` vì Vitest không bật `globals` nên Testing Library không tự dọn DOM. Dùng `fireEvent`, không thêm `@testing-library/user-event`.
- Chưa xong (T106 giữ `[ ]`): profile `demo` chưa có `CurrentUser`, Security (002) và provider mẫu chỉ ở mã test, nên chưa gắn component vào màn nào và chưa chạy đầu-cuối; SC-007 chưa đo (T103). Không làm bản giả quyền cho `demo` khi chưa được duyệt.

**Phase 5 – US3, thứ tự và phần dời (2026-10-19, chủ dự án duyệt)**:
- T072: provider lưu trữ cho `yeu_cau_phe_duyet` cần bảng của T089 (US5, Phase 7), nên dời sau T089; Phase 5 làm provider cho `audit_log` và `correction`. T072 giữ `[ ]` cho tới khi đủ ba provider và `LoaiBoDuLieuIT` có trường hợp yêu cầu phê duyệt.
- Cloud session không có JDK 25 nên không chạy được test backend; test backend của Phase 5 chạy trên máy chủ dự án (Windows, JDK 25, MySQL 8.0.45) trước khi đánh dấu `[x]`.

**Phase 5 – US3 nhật ký và lưu giữ (T061 → T073, 2026-10-19; chủ dự án duyệt D1 → D6)**:
- D1 quyền xem: khóa `AuditLogPermissions.VIEW` với `Target.UNSPECIFIED` (cột X, chỉ Quản lý viện; trước 2026-10-19 là khóa riêng `VIEW_FACILITY`, bỏ khi 002 đổi khóa sang enum) cho `GET /audit-logs`, thiếu thì 403 `PERMISSION_DENIED`; `VIEW` (cột P) cộng `ScopeApi.inScope` cho `GET /audit-logs/entities/…`, thiếu thì 404 (hợp đồng chỉ khai 404, không lộ tồn tại). Loại đối tượng chưa có `FieldClassificationProvider` thì chỉ Quản lý viện xem. Đối tượng chưa có nhật ký trả trang rỗng: 000 không biết đối tượng có tồn tại; 002 có thể trả "ngoài phạm vi" qua `ScopeApi` để thành 404.
- `AuditLogQueryService`: trang 50 dòng (thông số kỹ thuật), sắp `(occurred_at, id)` giảm dần, con trỏ `<micro giây UTC>_<id>`; con trỏ sai trả 422 `INVALID_VALUE` (hợp đồng chưa khai mã lỗi cho GET này). Che trường bằng `FieldVisibilityApi.canView` theo nhóm của từng trường (không khai báo là `INTERNAL_PUBLIC`); khác `FULL` thì `{"changed": true}`, có trường bị che thì `reason = null`, `reasonHidden = true`. Lọc `selfApproved` theo `details.selfApproved`. Người thực hiện `BOOTSTRAP` (002 ghi) chưa có trong enum của hợp đồng: đọc gặp thì báo lỗi cho tới khi 002 bổ sung hợp đồng. `fullName` để `null` cho tới khi 002 có hồ sơ tài khoản.
- D2: job ghi nhật ký với người thực hiện lấy từ `CurrentUser`; yêu cầu cho 002: ngoài request HTTP thì `CurrentUser` là Bộ lập lịch. Test đặt `SwitchableUser` thành Bộ lập lịch. `POST /testing/clock` với `runJobs` chạy job trong request; 002 T012 (2026-10-20) chốt: `SchedulerUserAspect` bọc `@Scheduled` và `ScheduledJob.runNow()`, nên job chạy trong request có phiên vẫn ghi người thực hiện là Bộ lập lịch.
- D3: `AuditLogService` đặt `exempt_from_purge = TRUE` khi hành động là `AuditAction.EXPIRED_DATA_PURGED`; không đổi `AuditLogEntry`.
- D4: thêm `net.ttddyy:datasource-proxy` 1.11.0 (phạm vi test; 1.11.1 phát hành 2026-10-13, chưa đủ hai tuần). `AuditLogCompletenessIT` bọc DataSource bằng proxy trong context riêng.
- D5: nhật ký có `resident_id` tính hạn từ ngày kết thúc lưu trú (001), chưa có giao diện trong contracts/module-interfaces.md nên chưa bao giờ hết hạn; chuỗi có nhật ký như vậy được giữ (FR-050 là thời hạn tối thiểu). Khi 001 khai giao diện thì `AuditLogRetention` dùng. *(Sửa 2026-10-27, nhánh `fix/m15-audit-correction-retention`)* `AttachedRetention.attachedTo` nhận thêm `RetentionKind` của bản ghi trong chuỗi: dòng nhật ký gắn hồ sơ hết hạn khi bản ghi đó tính hạn theo hồ sơ (`ResidentRecord`, đã hết hạn theo CFG-M15-03) và chính dòng đã qua CFG-M15-04; đi kèm bản ghi không gắn hồ sơ thì vẫn giữ cả chuỗi. Test `FR_050_ResidentLinkedLogRetentionIT`.
- D6: dữ liệu và bản giả chỉ ở mã test: `AuditLogFixtures` (hồ sơ mẫu `M15.SAMPLE_PROFILE`: `address` → `CONTACT`, `allergies` → `HEALTH`), `FakePermissions` thêm `visibility(...)`, `outOfScope(...)` (mặc định như cũ), `SampleReportService` (xuất báo cáo mẫu), `RetentionFixtures` (provider cho hai bảng mẫu nhóm 3, tham chiếu và lỗi giả lập).
- Lưu giữ: `RetentionDatabase` (gói `api`) giữ pool riêng bằng `eldercare_retention`, không là bean `DataSource` để không làm Spring Boot bỏ DataSource chính; ứng dụng nay cần `DB_RETENTION_USERNAME` lúc khởi động (đã có trong danh sách biến môi trường). `ExpiredDataPurge` (gói `internal`): mốc hạn đọc CFG lúc chạy; mỗi bản ghi hết hạn của provider là gốc chuỗi, thêm bản ghi trỏ tới nó (lặp), rồi đính chính và nhật ký đi kèm; có phần tử chưa hết hạn hoặc thuộc loại không có provider thì giữ cả chuỗi; mỗi chuỗi một transaction trên `eldercare_retention`; lỗi thì dừng đợt; luôn ghi bản tóm tắt (`deleted` theo loại kèm số lượng và khoảng thời gian, `parameters` là giá trị CFG đã áp, `stopped`) bằng `writeOutsideTransaction`, `scheduler_run` ghi `FAILED` khi dừng. Tối đa 1000 ứng viên mỗi loại mỗi đợt (kỹ thuật). Nhật ký và đính chính (`AttachedRetention`) không tự là gốc chuỗi: phần không gắn bản ghi nào có provider được giữ lâu hơn thời hạn tối thiểu. Job `ExpiredDataPurgeJob` chạy 02:30 ngày 1 hằng tháng (ShedLock) và cài `common.scheduling.ScheduledJob` để `runJobs = PURGE_EXPIRED_DATA` của đồng hồ giả lập chạy được; job chưa cài (T093, T094) vẫn 422.
- Test: T061 và hai trường hợp ghi của T065 qua ngay trước khi cài (phần ghi nhật ký đã có từ Phase 2, 3); đây là test bao phủ SC-001, FR-043a. Các test còn lại lỗi trước khi cài (404 vì chưa có endpoint, `UnsupportedOperationException` của job), xanh sau khi cài. Trên cloud (JDK 21, MySQL 8.0.46): 42 test đơn vị, 93 test tích hợp (thêm 23), `./mvnw verify` qua; chờ chạy lại trên JDK 25.
- T073: `features/audit-log/AuditLogTable.jsx` gắn vào mục menu "Nhật ký thay đổi"; vai trò khác Quản lý viện nhận 403 từ backend và thấy `PermissionDeniedState` (thêm prop `description`, mặc định giữ câu cũ). Hàm định dạng ngày giờ chung tách ra `src/common/dateTime.js` (component đính chính T106 dùng lại). Bảng vừa khít từ 1280px, hẹp hơn thì cuộn ngang trong thẻ. Ô ngày dùng `type="date"` của trình duyệt nên định dạng hiển thị theo ngôn ngữ máy (máy tiếng Việt là dd/mm/yyyy); chờ chủ dự án quyết có cần ô nhập dd/MM/yyyy riêng không.

**Phase 6 – US4 đổi tham số (T074 → T080, 2026-10-19)**:
- `ParameterValidator` kiểm theo `kind` (DURATION là `{value, unit}` cùng đơn vị của mã; INTEGER, MONEY số nguyên; PERCENTAGE trong 0–100; TIME_OF_DAY `HH:mm`; TABLE là object hoặc mảng), `min`, `max` của `constraints`, và `greaterThan` theo cả hai chiều (đổi CFG-M15-05 cũng kiểm với CFG-M15-06). Sai trả 422 `INVALID_VALUE`.
- `changeValue`: quyền `ParameterPermissions.CHANGE` (403), `If-Match` sai thì 412 kèm giá trị hiện hành, `UPDATE … WHERE version = ?`, thêm `parameter_history`, nhật ký `PARAMETER_CHANGED` (trước, sau là `{value}`), phát `ParameterChanged`; bộ đệm xóa khi transaction kết thúc (`afterCompletion`), nên quy tắc chạy ngay sau commit đọc giá trị mới (SC-004).
- Quyền xem: danh sách 403 khi thiếu `VIEW`; chi tiết và lịch sử 404 (hợp đồng chỉ khai 404 cho hai GET này). Lịch sử suy `effectiveTo` từ dòng kế tiếp; `asOf` lọc dòng có hiệu lực tới thời điểm đó.
- Test chỉ đổi mã mà 000 không dùng (ví dụ CFG-M06-04 cho `ParameterAsOfIT`) và trả lại giá trị trước khi đặt lại đồng hồ, để không làm hỏng lịch sử dùng chung của schema test. `ParameterIT` đếm theo mã, không phụ thuộc thứ tự chạy.
- JsonNode trên OpenAPI khai `@Schema(implementation = Object.class)` để springdoc không bung thuộc tính nội bộ.

**Phase 7 – US5 yêu cầu phê duyệt (T082 → T096, T105, 2026-10-19)**:
- Bảng chuyển (`RequestTransitionTable`) gồm 8 lệnh; chuyển không khai báo trả 409 `INVALID_STATE`. Quyết định đến sau trên yêu cầu đã rời Chờ duyệt trả 409 `ALREADY_PROCESSED` (xét trước `If-Match`, FR-040); thứ tự kiểm ở lệnh Duyệt và Từ chối: quyền, phạm vi, giới hạn vai trò (403) → trạng thái (409) → phiên bản (412) → tự duyệt (409 `SELF_APPROVAL_NOT_ALLOWED`, ý kiến bắt buộc 422).
- `display_code` dạng `YC-YYYY-000123` đặt ngay sau khi thêm (MySQL không cho cột sinh tham chiếu AUTO_INCREMENT).
- Áp dụng: mỗi yêu cầu một transaction `REQUIRES_NEW` (`ApplyTransaction`) khóa dòng, kiểm còn Đã duyệt và đã đến ngày; khóa chính của `application_result` chặn áp dụng hai lần. Lỗi khi áp dụng thì rollback, nhật ký `REQUEST_APPLY_ERROR` ghi bằng `writeOutsideTransaction`, yêu cầu giữ Đã duyệt. Duyệt khi ngày hiệu lực thực tế ≤ hôm nay thì áp dụng ngay trong `afterCommit`.
- Job: `ApplyDueRequestsJob` theo tasks T093 (15 phút một lần, cron `0 5/15 * * * *` gồm 00:05, và lúc khởi động; research ghi 00:05 hằng ngày, tasks thắng); `RemindPendingRequestsJob` 15 phút một lần. Cron và lần chạy lúc khởi động đọc từ `eldercare.jobs.*` (mặc định như trên); profile `test` tắt cả hai để job không chạy chồng lên test dùng chung schema, test gọi `run()`. Cả hai cài `ScheduledJob` nên `runJobs` của đồng hồ giả lập chạy được.
- Nhắc: mốc tính theo giờ liên tục từ `submitted_at`; không còn người duyệt thì báo Quản lý viện ngay ở mốc nhắc (`NO_APPROVER_LEFT`, đặt cả `reminded_at`, `escalated_at`); người duyệt đều là Quản lý viện thì không báo ở mốc CFG-M15-06. Nhật ký `REQUEST_REMINDED`, `REQUEST_ESCALATED` có `basis.ruleCode` và `schedulerRunId`.
- Danh sách (`GET /approval-requests`) quét theo lô id giảm dần và lọc quyền từng dòng tới khi đủ 50; con trỏ là id. Chi tiết của người không xem được trả 404; lệnh của người không có quyền trả 403. Người thực hiện của chuyển mở rộng và đóng do trạng thái cuối là người dùng hiện tại của lệnh gọi (module gọi).
- Điểm chờ 009: chưa có nhóm người nhận "người yêu cầu" trong `RecipientGroup`, nên `RequestApplyFailed` chỉ báo người duyệt đã quyết định; báo người yêu cầu làm khi 009 thêm nhóm. *(Đã xử lý 2026-10-22: `RequestApplyFailed` thêm `requesterStaffId` (tra bằng `AccountApi.byId` ở `ApproverDirectory.requesterStaffId`), báo thêm nhóm `Staff(staffId, "REQUESTER")`; người yêu cầu không gắn hồ sơ nhân viên thì không báo; test `ApplyFailureIT`.)* `kieuNoiDung` của module-interfaces gộp vào `validateContent`.
- Cách C của đính chính (T057 phần C, T058): `CorrectionService` lập và gửi duyệt yêu cầu `M15.CORRECTION` (nội dung: bản gốc, kiểu, nội dung đúng; ngày hiệu lực mong muốn là hôm nay) và trả 202 kèm `ApprovalRequest`. `CorrectionRequestHandler`: người duyệt giới hạn vai trò Quản lý viện; áp dụng thêm `correction` với `creation_method = 'C'`, `approval_request_id`, người thực hiện là người lập yêu cầu, nhật ký `CORRECTION_ADDED` có `basis.approvalRequestId`; bản gốc đã Hủy ghi nhận hoặc không còn xác nhận lúc áp dụng thì áp dụng không thành. Khóa quyền `CorrectionPermissions.REQUEST`, `APPROVE` do 000 đặt vì Phụ lục 27 không có dòng riêng: ai được lập do bảng chọn cách tạo quyết định, 002 cần cấp `REQUEST` cho mọi nhân viên có tài khoản và `APPROVE` cho Quản lý viện (báo lại ở mục cuối). `CorrectionService` lấy `ApprovalRequestApi` qua `ObjectProvider` vì handler phụ thuộc ngược lại.
- T072 phần yêu cầu phê duyệt: `ApprovalRequestRetention` chỉ xét yêu cầu ở trạng thái cuối, không gắn hồ sơ, theo CFG-M15-04 từ mốc muộn nhất (lập, quyết định, áp dụng); tham chiếu tới nó là kết quả áp dụng và bản đính chính cách C còn tồn tại; `application_result` xóa cùng yêu cầu. Kết quả thuộc loại không có provider thì yêu cầu được giữ (FR-051a).
- Trên cloud (JDK 21, MySQL 8.0.46): các test Phase 6, 7 lỗi trước khi cài, xanh sau khi cài; chờ chạy lại trên JDK 25.
- T081, T097 (web, bố cục chủ dự án duyệt 2026-10-19, DESIGN.md 5.16.0). Tham số: mục menu "Tài khoản, tham số" (`AdminPage`, tab Tham số; tab Tài khoản chờ 002); bảng có ô tìm theo mã, mô tả (lọc trên máy) và chọn module (gửi `module`); bấm mã mở khung chi tiết tải `GET /parameters/{code}` để lấy ETag; hai tab Đổi giá trị, Lịch sử (vai trò khác Quản lý viện chỉ có Lịch sử; chỉ để hiển thị, backend kiểm quyền). Lịch sử chọn ngày gửi `asOf` cuối ngày đó giờ Việt Nam. Tham số `TABLE` sửa bằng `TableValueEditor` dựng theo cấu trúc giá trị hiện hành (mảng đối tượng thành bảng thêm, xóa dòng; `{value, unit}` thành ô số; boolean thành ô chọn); tham số chưa có cấu trúc (null) thì báo module sở hữu chốt. *(Bổ sung 2026-10-22, 007 T104)* Tham số đã chốt dạng giá trị mà chưa có giá trị thì dựng từ `TABLE_TEMPLATES` (`parameterValue.js`): CFG-M11-11 có `name`, `address`, `taxCode`, `phone` theo `common.document.FacilityInfo` (logo chưa nhập trên màn: cần hàm đọc tệp của `FileApi`). Yêu cầu phê duyệt: mục menu ngay dưới Tổng quan cho mọi vai trò nhân viên; ba hộp gửi `inbox`; số ở hộp Chờ tôi duyệt là số dòng trang đầu (thêm "+" khi còn trang). Khung chi tiết hiện nội dung dạng "trường: giá trị"; nút theo `allowedCommands`; ý kiến bắt buộc khi từ chối (kiểm trên màn), tự duyệt thiếu ý kiến thì backend trả 422 và màn báo tại ô; 409, 412 báo rồi tải lại chi tiết; Hủy mở vùng nhập lý do ngay trong khung (không mở hộp thoại chồng lên khung). Diễn biến lấy từ `GET /audit-logs/entities/M15.APPROVAL_REQUEST/{id}`, lỗi (403, 404) thì ẩn. Chưa có nút sửa nội dung nháp (`edit-content`) vì nội dung theo schema của module sở hữu loại yêu cầu; module đầu tiên có loại yêu cầu lập từ màn web thêm form của mình. Test dùng `src/test/fetchRoutes.js` (giả `fetch` theo phương thức và đường dẫn). Màn chưa xem trên trình duyệt với backend thật: cần profile `demo` có người dùng (002).
- T104 (chủ dự án quyết 2026-10-19): thêm test cho ba FR chỉ có task. FR-004: `EffectiveVersioning.addVersionFrom` đóng phiên bản đang mở ở ngày trước ngày hiệu lực mới (phải sau hôm nay) rồi thêm phiên bản mới; tài khoản ứng dụng có thêm `UPDATE (effective_to)` trên bảng phiên bản (quy ước nhóm dữ liệu, migration mẫu `V000_91` thêm vào `db/migration/demo` chứ không sửa `V000_90` đã chạy ở máy chủ dự án). Trước đó phiên bản đang mở không đóng được nên kịch bản đổi đơn giá của FR-004 bị chặn vì chồng khoảng. FR-010a: `SchedulerScopeArchTest`, module thêm job thì thêm dòng vào `ALLOWED` kèm loại việc được phép. FR-017: `ParameterChangeNoRecalculationIT` (yêu cầu lập ở thời điểm 49 giờ trước để mọi lần đổi tham số ở giờ thật, giữ lịch sử tham số của schema test đúng thứ tự); `ApprovalFixtures` cấp thêm quyền tham số cho Quản lý viện. Hành vi FR-010a, FR-017 đã có từ trước nên hai test này không lỗi trước khi cài; đã kiểm bằng cách cài thử lỗi (bỏ một job khỏi danh sách; xóa `reminded_at` khi chưa tới mốc mới) và thấy test lỗi.
- Công tắc lập lịch chung (2026-10-26, nhánh `fix/test-scheduling-switch`, R-10): `@EnableScheduling` chuyển vào `SchedulingConfig.Enabled`, chỉ bật khi `eldercare.scheduling.enabled` (mặc định `true`); mặc định `eldercare.jobs.<job>.on-startup` của 7 job chạy lúc khởi động (m03 ×4, m07, m13, m15) lấy theo công tắc. Profile `test` đặt `false` và tắt `spring.modulith.moments` (Moments tự bật `@EnableScheduling`, mã không dùng). Trước đó mỗi job tự ghi `cron: "-"`; job của m03 (4) và m07 (`m07-minute` mỗi phút, `m07-daily`) quên nên chạy giữa lúc test. Test `SchedulingSwitchIT` (không lịch chạy nào, không job nào chạy lúc khởi động trong test, gồm job thêm sau). Job mới: đặt mặc định `on-startup` là `${eldercare.scheduling.enabled:true}`.

**Dịch vụ ngoài (2026-10-19)**: chủ dự án chọn AWS S3 cho tệp, FCM cho thông báo đẩy, dịch vụ SMS bên ngoài. Giao diện của 000 không đổi: `FileApi`, `FilePermissionProvider` (T036) giữ nguyên, 001 cài bằng S3 (001 research H-13); khuôn thông báo `common.notification` (T034) không phụ thuộc kênh, 009 gửi qua FCM và bộ chuyển SMS (009 research B-05). Region S3, nghĩa vụ chuyển dữ liệu ra nước ngoài và nhà cung cấp SMS chờ chủ dự án chốt (plan 001, 009).

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới, rồi sửa `docs/business/nghiep-vu.md` và spec 000.

1. **Phụ lục 25 không có kiểu và khoảng hợp lệ của tham số.** FR-011 và FR-015 đòi mỗi tham số có "kiểu giá trị, khoảng hợp lệ", và giá trị ngoài khoảng bị từ chối. Phụ lục 25 chỉ có mã, mô tả, mặc định, nơi dùng. Plan tự đặt kiểu (suy từ đơn vị của giá trị mặc định) và ràng buộc kỹ thuật hiển nhiên (thời lượng > 0, tỷ lệ 0–100, số lượng ≥ 0). Plan chỉ dùng ràng buộc nghiệp vụ khi nguồn có ghi, ví dụ CFG-M15-06 > CFG-M15-05. Cận trên và cận dưới nghiệp vụ (ví dụ CFG-M05-01 không được dài quá bao nhiêu) chưa có nguồn. Đề nghị: thêm cột "Khoảng hợp lệ" vào Phụ lục 25, hoặc chốt bằng Q rằng chỉ áp ràng buộc kỹ thuật. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
2. **BR-M15-07 và FR-016 khác nhau về thời điểm hiệu lực của tham số.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - BR-M15-07 ghi "Ngày hiệu lực của phiên bản, **tham số** và yêu cầu là ngày lịch, có hiệu lực từ 00:00 của ngày đó".
   - FR-016, NFR-12 và US4 #3 ghi giá trị mới "có hiệu lực ngay từ thời điểm lưu".
   - Plan theo FR-016 và US4 #3: hiệu lực ngay, lịch sử theo thời điểm (R-07), vì đó là hành vi spec đã viết kịch bản kiểm thử.
   - Đề nghị chốt bằng Q: bỏ chữ "tham số" khỏi BR-M15-07, hoặc nếu muốn tham số có ngày hiệu lực tương lai thì bổ sung lệnh "đặt giá trị từ ngày".
3. **(Nhỏ, đồng bộ câu chữ spec)** Mục "Phạm vi" của spec 000 ghi "mọi module (001–016)". Hiện đã có feature 017–019 cũng dùng 000 (bảng loại yêu cầu ở FR-031 có 017, 019). Đề nghị sửa thành "mọi feature 001–019" khi đồng bộ spec lần sau. Không ảnh hưởng thiết kế. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
4. **(2026-10-19, khi cài cách C; chủ dự án xác nhận giữ cách này 2026-10-19) Phụ lục 27 không có dòng quyền cho yêu cầu "Đính chính".** Nghiệp vụ 1.5 ghi người lập là nhân viên cùng vai trò với người ghi gốc, người duyệt là Quản lý viện; không có cột T, D riêng. 000 đặt khóa `CORRECTION.REQUEST` (002 cấp cho mọi nhân viên có tài khoản; ai được lập vẫn do bảng chọn cách tạo quyết định) và `CORRECTION.APPROVE` (Quản lý viện), kèm giới hạn vai trò duyệt là Quản lý viện. Đề nghị: thêm dòng vào Phụ lục 27 khi 002 lập ma trận quyền, hoặc xác nhận cách này. *(Sửa 2026-10-19, khi 002 đổi khóa quyền sang enum: Phụ lục 27 **có** dòng "Đính chính bản ghi không gắn tầng" (1.5; QL D, các vai trò khác T¹³), chính là cách C. Khóa đổi thành `(Function.UNASSIGNED_RECORD_CORRECTION, CREATE)` và `(…, APPROVE)`, cấp theo ma trận; không cần thêm dòng vào Phụ lục 27. Ghi nhận "không có dòng" ở trên là sai.)*
5. **(2026-10-19, T102) Chưa có lệnh chạy tay job loại bỏ dữ liệu ở `prod`.** Quy trình khôi phục (`backend/docs/operations.md` mục 5) cần chạy `PURGE_EXPIRED_DATA` trước khi mở cho người dùng để bản sao lưu không làm sống lại dữ liệu đã loại bỏ (FR-051). Hợp đồng chỉ có `POST /testing/clock` (`test`, `demo`). Đề nghị: thêm lệnh quản trị (API hoặc tham số khởi động) khi lập 002 hoặc trước khi vận hành; trong thời gian đồ án CFG-M15-03, CFG-M15-04 mặc định 10 năm nên chưa ảnh hưởng. *(Đã xử lý 2026-10-19, chủ dự án chọn tham số khởi động: `eldercare.jobs.purge-expired-data.run-once=true` chạy một đợt khi ứng dụng sẵn sàng, `ExpiredDataPurgeJobTest`; không đổi hợp đồng API. Người thực hiện trong nhật ký lúc khởi động theo D2, chờ 002 như lần chạy lúc khởi động của `APPLY_DUE_REQUESTS`.)*
