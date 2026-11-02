# Elder_Care backend (`backend/`)

Backend của hệ thống quản lý viện dưỡng lão: một ứng dụng Spring Boot 4.1 (Java 25, Maven wrapper), MySQL 8.0, Flyway, Spring Modulith; REST `/api/v1`. Kiến trúc Modular Monolith: chia module theo nghiệp vụ, trong module chia theo chức năng và tầng.

Đọc trước khi viết mã: [AGENTS.md](../AGENTS.md) (luật chung cho mọi người và mọi công cụ AI), [docs/team-plan.md](../docs/team-plan.md) (ai sở hữu module nào), research R-01 → R-14 của [000](../specs/000-shared-foundation/research.md) (quy ước kỹ thuật; R-02 là nguồn của file này). File này là luật cấu trúc của `backend/`; đường dẫn file trong tasks, plan (`BE/resident/hoso/internal/…`) là tên logic, đặt lớp theo file này.

## Lệnh

```bash
./mvnw verify                 # test đơn vị, tích hợp, kiến trúc, so hợp đồng (PowerShell: .\mvnw.cmd)
./mvnw test -Dtest=TenLopTest # một test đơn vị (hoặc -Dtest=TenLopTest#tenPhuongThuc)
./mvnw spotless:apply         # định dạng Java (verify kiểm định dạng)
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo   # chạy với dữ liệu mẫu, đồng hồ giả lập
```

Test `*IT` cần MySQL đang chạy và biến `TEST_DB_URL`, `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD` (CLAUDE.md mục "Công nghệ và cấu trúc mã").

## 1. Gói gốc `com.eldercare.backend`

| Gói | Nội dung |
| --- | --- |
| `common/` | Thành phần kỹ thuật dùng chung (đồng hồ, lỗi, nhóm dữ liệu, lập lịch, web, tệp `file`, chứng từ PDF và Excel `document`, khuôn thông báo). Không chứa dữ liệu nghiệp vụ; không dùng để né ranh giới module. A giữ |
| `resident/` … `administration/` | 15 application module của Spring Modulith (M01 … M15; bảng tên ở research R-02 của 000: `resident`, `admission`, `roombedallocation` (gồm tài sản, xe của 019), `care`, `incident`, `health`, `medication`, `nutrition`, `staffing`, `family`, `billing`, `belongings`, `notification`, `reporting`, `administration`), mỗi module một chủ. Mỗi module có `package-info.java` gắn `@ApplicationModule`. Viết mã theo SOLID (constitution XVI, `SolidArchTest`) |

M15 có sáu phần, mỗi phần theo cấu trúc như một module: `account`, `approval`, `auditlog`, `correction`, `parameter`, `retention`.

## 2. Cấu trúc trong một module

Chỉ tạo gói khi có lớp; không tạo gói rỗng theo sơ đồ.

```
<module>/  (ví dụ resident/; hoặc một phần của M15, ví dụ administration/approval/)
├── api/              interface module khác gọi; chú thích dùng khi viết mã
│   ├── dto/          record, enum của hợp đồng trao đổi giữa module
│   ├── spi/          interface module khác cài (kèm kiểu chỉ SPI dùng)
│   └── events/       sự kiện công khai
├── web/              controller
│   ├── request/      thân yêu cầu HTTP
│   └── response/     phản hồi HTTP (hàm of(...) dựng từ dữ liệu nghiệp vụ)
├── internal/
│   ├── service/      use case: điều phối, transaction, kiểm quyền, ghi nhật ký, phát sự kiện
│   ├── domain/       entity, value object, enum, luật thuần (chuyển trạng thái, bất biến), hằng số hành động nhật ký
│   ├── persistence/  truy cập dữ liệu: lớp JDBC (*Store), Spring Data repository
│   └── config/       cấu hình nội bộ của module
└── scheduling/       job theo lịch
```

**Module lớn** chia theo chức năng trước, mỗi chức năng có các tầng trên. Phần dùng chung giữa các chức năng ở `internal/domain/`, `internal/persistence/` của module. Lớp `persistence` trả record của `domain` (không khai báo record lồng trong lớp truy cập dữ liệu), để `web` không phụ thuộc tầng lưu trữ.

```
administration/account/
├── api/              AccountApi, PermissionApi, ScopeApi, CurrentUser, RequiresPermission…
│   ├── dto/          Target, PermissionKey, PermissionResult, Function, Operation, AccountView…
│   ├── spi/          DutyApi, FamilyRelationshipApi, LicenseApi, FootnoteCondition, OfflineHandler…
│   └── events/
├── web/              AccountController, AuthController, MeController…
│   ├── request/  response/  security/
├── internal/
│   ├── auth/         service/ domain/ config/
│   ├── permission/   service/ domain/ config/
│   ├── scope/        service/
│   ├── legal/        service/ domain/ persistence/
│   ├── offline/      service/ domain/ persistence/
│   ├── domain/       Account (record tài khoản, dùng chung)
│   └── persistence/  AccountStore (dùng chung cho auth, permission)
└── scheduling/
```

**Module nhỏ** (một chức năng) chia tầng ngay dưới `internal/`:

```
administration/approval/
├── api/              ApprovalRequestApi, ApprovalRequestView… ; dto/ ; spi/ (RequestTypeHandler)
├── web/              ApprovalRequestController ; request/ ; response/
├── internal/
│   ├── service/      ApprovalRequestService, RequestApplier, DueRequestApplication…
│   ├── domain/       ApprovalActions, RequestTransitionTable
│   └── persistence/  ApprovalRequestStore
└── scheduling/       ApplyDueRequestsJob, RemindPendingRequestsJob
```

## 3. Đặt lớp ở đâu

| Lớp | Gói |
| --- | --- |
| Interface cho module khác gọi (`XxxApi`), chú thích dùng chung (`@RequiresPermission`) | `api/` |
| Record, enum trong chữ ký của interface `api` | `api/dto/` |
| Interface module khác phải cài cho module mình (bản giả ở `src/test/.../support/fake/<module>/`) | `api/spi/` |
| Sự kiện module khác nghe | `api/events/` |
| `@RestController` | `web/` |
| Record thân yêu cầu, tham số lệnh | `web/request/` |
| Record phản hồi, trang kết quả | `web/response/` |
| `@Service` lệnh, truy vấn; bản cài interface `api`; aspect | `internal/…/service/` |
| Entity, record trạng thái, enum nội bộ, bảng chuyển trạng thái, luật thuần, hằng số `AuditAction` | `internal/…/domain/` |
| Lớp đọc ghi bảng bằng `JdbcTemplate` (`*Store`), Spring Data repository | `internal/…/persistence/` |
| `@Configuration`, runner chạy một lần | `internal/…/config/` |
| `@Scheduled` (cài `ScheduledJob`) | `scheduling/` |

## 4. Luật bắt buộc (test kiểm)

| Luật | Test |
| --- | --- |
| Module khác chỉ dùng gói `api` (và gói con) của module mình; không vòng phụ thuộc | `ModuleBoundaryTest` (Spring Modulith) |
| Mỗi gói `api` và gói con có `package-info.java` gắn `@NamedInterface` | `ApiNamedInterfaceArchTest` |
| Trong M15, phần này chỉ dùng gói `api` của phần kia | `M15PartBoundaryArchTest` |
| Gói cấp một của module chỉ là `api`, `web`, `internal`, `scheduling` (M15 thêm một cấp phần) | `LayerArchTest` |
| Lớp trong `internal` nằm ở `service`, `domain`, `persistence` hoặc `config`; tầng không có gói con | `LayerArchTest` |
| `web` không dùng `persistence`, `JdbcTemplate`, Spring Data; `api` không dùng `internal`, `web` | `LayerArchTest` |
| `domain` không dùng `service`, `persistence`, `web`; `persistence` không dùng `service`, `web`; `service` không dùng `web` | `LayerArchTest` |
| Chỉ `persistence` dùng JDBC (`org.springframework.jdbc`); ngoại lệ: phần `administration.retention` và lớp cài `RetentionProvider` | `LayerArchTest` |
| Gốc `api` chỉ chứa interface, chú thích; record, enum ở `api/dto`, sự kiện ở `api/events` | `LayerArchTest` |
| Lớp `persistence` không khai báo record lồng; record trả về đặt ở `domain` | `LayerArchTest` |
| Mọi bảng khai nhóm dữ liệu: entity bằng `@DataGroup`, bảng JDBC bằng `COMMENT = 'DataGroup=CATALOG\|BUSINESS\|RECORD\|MULTIPLE\|TECHNICAL'` khi tạo bảng; bảng `RECORD` có `trg_<bảng>_no_update`, `trg_<bảng>_no_delete` | `RecordGroupTriggerIT` (audit F-04) |

Chỗ lệch có sẵn trước luật (rà 2026-10-30) ghi ở các hằng `PENDING_*` của `LayerArchTest`, kèm chủ module; người sửa xóa dòng tương ứng, không thêm dòng mới.
| Mọi `@BusinessCommand` có `@RequiresPermission` (hoặc trong danh sách tự kiểm) | `RequiresPermissionArchTest` |
| Job chỉ làm việc FR-010a cho phép; `@Scheduled` nằm trong `ScheduledJob` | `SchedulerScopeArchTest` |
| Không gọi `Instant.now()`; thời gian qua `BusinessTime`/`Clock` | `TimeArchTest` |

Luật không có test:

- DTO chỉ phục vụ HTTP đặt ở `web/`. Ngoại lệ duy nhất: `administration.approval.api.dto` (phản hồi yêu cầu phê duyệt dùng ở phần khác).
- Không tạo interface `repository/` bọc lớp truy cập dữ liệu; giữ công nghệ đang dùng (`JdbcTemplate` hay Spring Data JPA).
- Lớp, thành viên chỉ dùng trong gói giữ package-private; chỉ mở `public` phần tầng khác gọi.
- Đổi gói `api` là "Đổi api": báo người dùng và ghi ở docs/team-plan.md mục 8 (AGENTS.md).

## 5. Test

- `*Test`: đơn vị (surefire); `*IT`: tích hợp, kế thừa `support/MySqlIntegrationTestBase` (failsafe ở `verify`); `*ArchTest`: kiến trúc.
- Test của module đặt ở gói gốc của module (`com.eldercare.backend.administration.account`); test lớp package-private đặt cùng gói với lớp.
- Bản giả SPI ở `src/test/java/com/eldercare/backend/support/fake/<module>/`, do người khai báo interface viết.
