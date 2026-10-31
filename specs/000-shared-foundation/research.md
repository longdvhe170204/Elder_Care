# Research: Nền tảng quy tắc nghiệp vụ dùng chung (000)

**Ngày**: 2026-10-06 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Đây là các quyết định kỹ thuật. Chúng không đổi nghiệp vụ (constitution IX). Các quyết định nền (R-01 → R-05) áp dụng cho mọi feature sau. Plan của các feature đó dẫn lại, không quyết lại.

---

## R-01. Phiên bản công nghệ

**Quyết định**:

| Thành phần | Phiên bản | Ghi chú |
| --- | --- | --- |
| Java | 25 | Theo constitution X (3.1.0); JDK cài trên máy chủ dự án |
| Spring Boot | 4.1.x (dự án khởi tạo ở 4.1.1) | Spring Framework 7, Hibernate 7; phiên bản thư viện con do Spring Boot quản lý |
| Spring Modulith | 2.x (bản tương thích Spring Boot 4.1) | Kiểm tra ranh giới module, sự kiện có lưu trữ (R-09) |
| MySQL | 8.0 (bản cài trên máy) | InnoDB, `utf8mb4_0900_ai_ci` |
| Flyway | Bản do Spring Boot 4.1 quản lý (`spring-boot-starter-flyway`, `flyway-mysql`) | Migration cơ sở dữ liệu |
| springdoc-openapi | 3.x (dòng cho Spring Boot 4) | Sinh OpenAPI 3.1 từ mã để đối chiếu với `contracts/` |
| JUnit 5, AssertJ, ArchUnit | Bản hiện hành | Kiểm thử backend; Testcontainers hoãn tới khi có Docker (R-14) |
| Lombok | Bản do Spring Boot quản lý | Có sẵn trong dự án khởi tạo; chỉ dùng `@Getter`, constructor, `@Builder`; không dùng `@Data`, `@Setter` trên entity nhóm 2, 3 (R-05) |
| React | 19.x, JavaScript (JSX, ES module), Vite 8 | Web (`frontend/`); không dùng TypeScript (constitution X 3.1.0) |
| Tailwind CSS | 4.x (`@tailwindcss/vite`) | Giao diện web; biến màu, bo góc khai báo trong `frontend/src/index.css` |
| shadcn/ui | CLI 4.x, kiểu `base-nova` trên Base UI (`@base-ui/react`), `class-variance-authority`, `cn`, `tw-animate-css` | Component giao diện ở `frontend/src/components/ui/`, thêm bằng `npx shadcn add` |
| lucide-react, Be Vietnam Pro (`@fontsource/be-vietnam-pro`, nạp 400, 500, 600) | Bản hiện hành | Icon và font của web; font thiết kế cho tiếng Việt, thay Geist theo `frontend/DESIGN.md` 2.0.0 (2026-10-16) |
| ESLint | 10.x (`eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`) | Lint web, có sẵn từ lúc khởi tạo |
| TanStack Query, React Router | Bản hiện hành | Gọi API, điều hướng |
| Flutter | Kênh stable 3.x (máy: 3.41.9), Dart 3.x (`sdk: ^3.11.5`) | Một app chung (`mobile/`, Q-346), Material; gọi API bằng `http` |
| drift (SQLite) | Bản hiện hành | Hàng đợi ngoại tuyến của phần nhân viên (R-13) |

**Lý do**: Constitution X (3.1.0) đặt mặc định theo môi trường đã cài trên máy của chủ dự án (sửa 2026-10-16): JDK 25, MySQL 8.0, dự án khởi tạo bằng Spring Boot 4.1.1; web giữ JavaScript, Tailwind CSS v4, shadcn/ui như dự án `frontend/` đã khởi tạo (chủ dự án chốt 2026-10-16, lượt 2).

**Rủi ro đã biết**: MySQL 8.0 đã hết hỗ trợ của Oracle từ 04/2026 (không còn bản vá bảo mật). Trước khi vận hành MUST nâng lên 8.4 LTS rồi chạy lại toàn bộ test tích hợp; để việc nâng không phải sửa lược đồ, migration chỉ dùng cú pháp có ở cả 8.0 và 8.4 (không dùng tính năng đã bỏ ở 8.4 như `mysql_native_password` cho tài khoản mới, `utf8mb3`). Testcontainers chưa dùng nên test tích hợp phụ thuộc MySQL cài trên máy (R-14).

**Phương án đã cân nhắc**: Spring Boot 3.5 với Java 21 và MySQL 8.4 (bản thiết kế 2026-10-06): bỏ ngày 2026-10-16 để dùng đúng môi trường đã cài. Web bằng TypeScript 5 (bản 2026-10-16 trước đó): bỏ cùng ngày để giữ dự án JavaScript đã khởi tạo; kiểu dữ liệu của API dựa vào hợp đồng `contracts/` và test so springdoc (R-14) thay cho kiểm kiểu lúc biên dịch.

---

## R-02. Cấu trúc kho mã và gói backend

**Quyết định**: Một kho mã có ba thư mục gốc: `backend/`, `frontend/` (web), `mobile/` (một app Flutter chung, Q-346; phần nhân viên, phần người thân và phần người cao tuổi là ba nhánh `lib/` theo loại tài khoản; phần người cao tuổi bổ sung 2026-10-19 theo Q-365). Tên gói, thư mục con, file mã bằng tiếng Anh theo R-03 và bảng thuật ngữ [glossary.md](glossary.md); tên tiếng Việt trong plan, tasks là tên logic. Backend là một ứng dụng Spring Boot. Mỗi module M01–M15 là một gói con trực tiếp của `com.eldercare.backend` (`resident` … `administration`) và được Spring Modulith coi là một application module. Mỗi module chỉ công khai gói `api` (giao diện, DTO, sự kiện), phần còn lại là nội bộ.

Feature 000 nằm trong **M15 – Quản trị hệ thống**, vì tham số (BR-M15-04), nhật ký (19.4, BR-M15-11 → 13) và quy tắc nền (BR-M15-06 → 10) đều thuộc M15. Năm gói con: `administration.thamso`, `administration.nhatky`, `administration.pheduyet`, `administration.dinhchinh`, `administration.luutru`. Thêm một gói kỹ thuật `com.eldercare.backend.common`: đồng hồ, mô hình lỗi, kiểu cơ sở, chú thích phân nhóm dữ liệu. Gói này không chứa dữ liệu nghiệp vụ và không phải module nghiệp vụ.

**Cấu trúc trong module** *(bổ sung 2026-10-21, chủ dự án duyệt; tổ chức lại backend theo Modular Monolith, package-by-feature kết hợp phân tầng)*. Áp cho mỗi module `resident` … `reporting` và mỗi phần của M15 (`administration.account`, `administration.approval`, `administration.auditlog`, `administration.correction`, `administration.parameter`, `administration.retention`); chỉ tạo gói khi có lớp, không tạo gói rỗng theo sơ đồ.

```
<module hoặc phần>/
├── api/              interface module khác gọi, chú thích dùng khi viết mã (@NamedInterface)
│   ├── dto/          record, enum của hợp đồng trao đổi giữa module
│   ├── spi/          interface module khác cài (kèm kiểu chỉ SPI dùng)
│   └── events/       sự kiện công khai
├── web/              controller (chỉ nhận yêu cầu, gọi service, trả phản hồi; không truy cập dữ liệu)
│   ├── request/      thân yêu cầu HTTP
│   ├── response/     phản hồi HTTP (hàm of(...) dựng từ dữ liệu nghiệp vụ)
│   └── security/     thành phần HTTP đặc thù (chỉ account: chuỗi lọc, cấu hình bảo mật)
├── internal/         triển khai nội bộ, luôn chia tầng
│   ├── service/      use case, điều phối, transaction, kiểm quyền, ghi nhật ký, phát sự kiện
│   ├── domain/       entity, value object, enum, luật thuần (chuyển trạng thái, bất biến), hằng số hành động nhật ký
│   ├── persistence/  truy cập dữ liệu (lớp JDBC, Spring Data repository, entity ánh xạ nếu tách)
│   └── config/       cấu hình nội bộ của module
│   (module lớn chia theo chức năng trước, mỗi chức năng có các tầng trên: account/internal/<auth, permission, scope,
│    legal, offline>/{service, domain, persistence, config}; truy cập dữ liệu dùng chung giữa các chức năng ở
│    account/internal/domain/, account/internal/persistence/; lớp persistence trả record của domain)
└── scheduling/       job theo lịch (SchedulerScopeArchTest)
```

- Không tạo interface `repository/` bọc lớp truy cập dữ liệu; giữ công nghệ đang dùng (`JdbcTemplate` hay Spring Data JPA). Tầng không có lớp thì không tạo gói (ví dụ `auditlog/internal/` chỉ có `service/`).
- Lớp truy cập dữ liệu chỉ một chức năng dùng thì để trong `persistence/` của chức năng đó; chỉ phần dùng chung lên `account/internal/persistence/`. Lớp, thành viên chỉ dùng trong gói giữ package-private; chỉ mở `public` phần tầng khác gọi.
- DTO chỉ phục vụ HTTP thuộc `web/`. Ngoại lệ: `administration.approval.api.dto` (`ApprovalRequestBody`, `ActorBody`, `ActorType`) vì phản hồi 202 của lệnh ở phần khác (đính chính cách C) trả yêu cầu phê duyệt.
- Mỗi gói `api` và gói con có `package-info.java` gắn `@NamedInterface` (`ApiNamedInterfaceArchTest`). Giữa các phần của M15, `M15PartBoundaryArchTest` chỉ cho dùng gói `api` của phần khác (Modulith coi cả M15 là một module).
- `LayerArchTest` kiểm tầng trong mọi module: lớp `internal` nằm ở `service`, `domain`, `persistence` hoặc `config`; `web` không dùng `persistence`, `JdbcTemplate`, Spring Data; `domain` không dùng tầng khác; `persistence` không dùng `service`, `web`; `service` không dùng `web`; `api` không dùng `internal`, `web`. Bảng "đặt lớp ở đâu" và ví dụ ở [backend/README.md](../../backend/README.md); đường dẫn file trong tasks, plan là tên logic, đặt lớp theo mục này.

**Tên gói module** *(sửa 2026-10-30, constitution X 3.3.0; trước đó là `m01` … `m15`)*: gói gốc `com.eldercare.backend` (trước là `com.cambae.backend`; Maven `groupId` `com.eldercare`), gói module mang tên tiếng Anh của module; mã nghiệp vụ M01 … M15 (BR, loại bản ghi `M01.CONSENT`, nhật ký, đính chính, tệp, loại ở bảng dùng chung) giữ nguyên.

| Mã | Module (nghiệp vụ) | Gói |
| --- | --- | --- |
| M01 | Hồ sơ và đánh giá người cao tuổi | `resident` |
| M02 | Tiếp nhận và lưu trú | `admission` |
| M03 | Phòng và giường (gồm tài sản, xe của 019) | `roombedallocation` |
| M04 | Chăm sóc và sinh hoạt hằng ngày (gồm hoạt động của 014) | `care` |
| M05 | Sự cố và cảnh báo | `incident` |
| M06 | Sức khỏe | `health` |
| M07 | Thuốc | `medication` |
| M08 | Dinh dưỡng | `nutrition` |
| M09 | Nhân sự và ca trực | `staffing` |
| M10 | Người thân và gia đình | `family` |
| M11 | Chi phí phát sinh (gồm số dư 017, mua sắm 019) | `billing` |
| M12 | Đồ dùng và tài sản gửi | `belongings` |
| M13 | Thông báo | `notification` |
| M14 | Báo cáo và thống kê | `reporting` |
| M15 | Quản trị hệ thống (`account`, `approval`, `auditlog`, `correction`, `parameter`, `retention`, `shared`) | `administration` |

Tên lớp mang mã module (`M09ArchTest`, `M07DailyJob`, `M13ArchTest`) giữ nguyên vì mã module vẫn đúng. Khóa cấu hình job (`eldercare.jobs.m09-daily.cron`) và tên job ShedLock giữ nguyên. Migration `V000_09__rename_module_packages.sql` đổi tên lớp trong bảng `EVENT_PUBLICATION` để sự kiện chưa hoàn tất vẫn được xử lý lại; `LayerArchTest` liệt kê đủ 15 gói (mẫu `m*` cũ không còn khớp).

**Nguyên tắc SOLID** *(bổ sung 2026-10-30, constitution XVI)*: luật ở constitution XVI. Kiểm tự động: `SolidArchTest` (không tiêm vào trường, không tiêm qua phương thức, bean không tự `new` bean khác trừ lớp `@Configuration`), `LayerArchTest` (tầng trong module), `ModuleBoundaryTest` (chỉ dùng gói `api` của module khác), `ApiNamedInterfaceArchTest`. Review kiểm: service theo nhóm use case, interface `api`/`spi` chia theo module gọi, mở rộng bằng SPI hoặc strategy, bản giả giữ hợp đồng. Mã cũ sửa dần khi task chạm tới (ưu tiên trong task gộp R-15).

**Lý do**: Constitution X yêu cầu một khối chia gói theo M01–M15 và chặn truy cập chéo dữ liệu. `ApplicationModules.verify()` của Spring Modulith và ArchUnit kiểm tra điều này tự động trong bộ test. Cấu trúc trong module cho mọi thành viên cùng một chỗ đặt lớp và nhìn tên gói là biết tầng (chủ dự án chọn chia tầng cho mọi `internal`, 2026-10-21); chia theo chức năng trước ở module lớn để mã của một chức năng nằm gần nhau. Đổi lại, lớp dùng giữa các tầng phải `public` (khi chia M15: 10 lớp ở `domain`, `persistence` của `auth`, `legal`, `offline`, `permission`, `approval`, `correction`); ranh giới giữa module vẫn do Modulith và ArchUnit giữ.

**Phương án đã cân nhắc**:
- Tách gói `foundation` thành module thứ 16: bị loại vì sinh thêm một module ngoài M01–M15.
- Mỗi module một Maven module: bị loại vì quá nặng cho quy mô đồ án. Modulith đủ để kiểm tra ranh giới.
- *(2026-10-21)* Chia tầng ngay dưới `internal/` cho cả module lớn (`account/internal/{service, domain, persistence}` rồi mới tới chức năng): bị loại vì mã một chức năng rải ra nhiều gói. Để `internal/` phẳng theo chức năng: bị loại, chủ dự án muốn nhìn tên gói là biết tầng. Thêm interface `repository/` bọc lớp truy cập dữ liệu: bị loại, `JpaRepository` hay lớp JDBC đã đủ.
- *(2026-10-21)* Tách mỗi phần của M15 thành application module riêng: bị loại vì thành 20 module, trái quyết định 15 module ở trên; dùng luật ArchUnit thay.

---

## R-03. Quy ước API

**Quyết định**:
- **Tên tiếng Anh (sửa 2026-10-16, constitution X 3.0.0)**: mọi tên định danh trong mã bằng tiếng Anh: gói, lớp, hàm, biến, file và thư mục mã, bảng, cột, giá trị enum lưu xuống cơ sở dữ liệu, đường dẫn API, trường JSON, `maLoi` (đặt tên trường là `errorCode`). Mã nghiệp vụ (BR-, FR-, CFG-, Q-, UC-, DBR-) giữ nguyên.
  - Tên trong plan, data-model, contracts, tasks (`tai_khoan`, `YeuCauPheDuyetController`, `/yeu-cau-phe-duyet/{id}/duyet`, `lyDo`) là **tên logic**; khi viết mã dịch theo bảng thuật ngữ [glossary.md](glossary.md). Một thuật ngữ chỉ có một bản dịch; thuật ngữ chưa có thì thêm vào bảng trong cùng thay đổi với mã.
  - Mỗi feature, trước khi viết mã: dịch `contracts/api-NNN.yaml` sang tên tiếng Anh theo bảng thuật ngữ (test hợp đồng so springdoc với file này) và bổ sung bảng thuật ngữ cho các bảng, lớp của feature. Không đổi nghĩa, không thêm bớt endpoint hay trường.
  - Tên lớp test vẫn gắn mã quy tắc (R-14), phần mô tả bằng tiếng Anh, ví dụ `BR_M15_09_noApproverLeft_notifiesDirectorAtReminderTime`.
- Tiền tố `/api/v1`. Đường dẫn tiếng Anh, chữ thường, nối bằng gạch ngang, ví dụ tên logic `/api/v1/yeu-cau-phe-duyet/{id}/duyet` thành `/api/v1/approval-requests/{id}/approve`. Tên trường JSON là camelCase tiếng Anh (`lyDo` thành `reason`).
- Nhóm 2: chỉ có `POST .../{id}/<lenh>` cho từng lệnh nghiệp vụ, không có `PUT`/`PATCH` lên trạng thái (constitution XI). Nhóm 3: chỉ có `POST` tạo và `POST .../{id}/dinh-chinh`. Nhóm 1: `POST`, `PUT`, `POST .../ngung-hieu-luc`, `POST .../kich-hoat-lai`, và `DELETE` khi chưa được tham chiếu.
- Chống ghi đè: mọi tài nguyên sửa được đều trả `ETag` (số phiên bản). Lệnh sửa bắt buộc `If-Match`. Sai phiên bản thì trả `412 Precondition Failed` kèm giá trị hiện hành (FR-021, FR-040).
- Lỗi theo RFC 9457 (`application/problem+json`) và thêm các trường:
  - `maLoi` (ổn định, ví dụ `DIEU_KIEN_CHUA_THOA`, `THIEU_LY_DO`, `KHONG_CO_QUYEN`, `DA_DUOC_XU_LY`);
  - `dieuKienChuaThoa[]` (mã BR, mô tả; FR-006);
  - `giaTriHienHanh` khi bị xung đột.
- Ánh xạ mã HTTP:

  | Mã | Khi nào |
  | --- | --- |
  | 400 | Sai định dạng |
  | 401 | Chưa xác thực |
  | 403 | Không có quyền (BR-M15-01) |
  | 404 | Không tồn tại hoặc ngoài phạm vi; không lộ là có tồn tại |
  | 409 | Vi phạm điều kiện nghiệp vụ hoặc trạng thái |
  | 412 | Sai phiên bản |
  | 422 | Thiếu lý do hoặc giá trị sai kiểu, ngoài khoảng |

- Thời điểm theo ISO-8601 có offset `+07:00`. Ngày theo `yyyy-MM-dd`; client hiển thị dạng dd/MM/yyyy (NFR-11).
- Mỗi lệnh ghi nhận `Idempotency-Key` (UUID do client sinh). Cùng khóa, cùng người gửi thì trả lại kết quả lần đầu, không tạo bản ghi thứ hai. Đây là nền cho đồng bộ ngoại tuyến (R-13) và cho FR-037 ("kích hoạt lặp lại không tạo tác động").

**Lý do**: Constitution X, XI. Cùng một mô hình lỗi cho ba client giúp hiển thị đúng "điều kiện chưa thỏa".

**Phương án đã cân nhắc**: Đường dẫn tiếng Anh bị loại để giữ thuật ngữ mục 2.4 xuyên suốt và khớp ví dụ ở constitution XI. GraphQL bị loại vì constitution chốt REST.

---

## R-04. Thời gian và đồng hồ giả lập

**Quyết định**:
- Backend dùng một bean `java.time.Clock` duy nhất, múi giờ `Asia/Ho_Chi_Minh`. Không mã nào gọi `Instant.now()`, `LocalDate.now()` hay `NOW()` của MySQL; ArchUnit chặn các lời gọi này. Thời điểm luôn lấy từ máy chủ (NFR-09, DBR-25, FR-046).
- Lưu trữ:
  - thời điểm (`Instant`) lưu bằng `DATETIME(6)` theo UTC; JDBC đặt `connectionTimeZone=UTC`, Hibernate đặt `hibernate.jdbc.time_zone=UTC`;
  - ngày lịch nghiệp vụ (ngày hiệu lực, khoảng hiệu lực) lưu bằng `DATE`, tính theo `Asia/Ho_Chi_Minh` (BR-M15-07);
  - quy đổi giữa thời điểm và ngày đi qua một tiện ích duy nhất `ThoiGianNghiepVu` (ví dụ `batDauNgay(LocalDate)` trả 00:00 +07:00).
- Bản ghi ngoại tuyến lưu thêm `thoiDiemThietBi` do client gửi. `thoiDiem` (thời điểm đồng bộ) vẫn do máy chủ đặt.
- Đồng hồ giả lập (NFR-13): profile `test` và `demo` thay bean bằng `DieuKhienDongHo` (đặt, tua). Chỉ hai profile này có endpoint `/api/v1/kiem-thu/dong-ho`; profile `prod` không nạp bean và không có endpoint.

**Lý do**: Một nguồn thời gian cho mọi quy tắc theo giờ, kiểm thử được mọi mốc CFG.

**Phương án đã cân nhắc**:
- Lưu giờ địa phương: Việt Nam không có giờ mùa hè nên cũng được, nhưng UTC tránh lệch khi máy chủ hoặc driver đổi múi giờ.
- `TIMESTAMP` của MySQL: bị loại vì giới hạn năm 2038 và tự đổi múi giờ theo session.

---

## R-05. Bảo đảm nhóm dữ liệu (FR-001 → FR-010, DBR-23)

**Quyết định**: Ba lớp, lớp nào cũng đủ để chặn:

1. **Mã nguồn**:
   - mỗi entity mang chú thích `@NhomDuLieu(DANH_MUC | NGHIEP_VU | GHI_NHAN)`;
   - repository của nhóm 3 kế thừa `ChiThemRepository`, chỉ có `them(...)` và các hàm đọc, không có `save` cho bản ghi đã có hay `delete`;
   - entity nhóm 3 đánh dấu `@Immutable` (Hibernate) sau khi xác nhận;
   - ArchUnit kiểm tra mọi entity có đúng một chú thích nhóm (FR-001) và nhóm 2, 3 không có setter trạng thái công khai;
   - vì vậy entity nhóm 2, 3 không dùng Lombok `@Data` hay `@Setter` (sinh setter công khai); trạng thái chỉ đổi qua phương thức lệnh nghiệp vụ. Entity nhóm 1 được dùng `@Setter`.
2. **API**: không có endpoint sửa hay xóa cho nhóm 2, 3 (R-03).
3. **Cơ sở dữ liệu**:
   - backend chạy bằng tài khoản `eldercare_app`. Tài khoản này chỉ có `SELECT, INSERT` trên các bảng nhóm 3 không có bản nháp (`nhat_ky`, `dinh_chinh`, `lich_su_tham_so`) và không có `DELETE` trên mọi bảng nhóm 3;
   - bảng nhóm 3 có bản nháp (do module quy định) cần `UPDATE` cho tới lúc xác nhận. Trigger `BEFORE UPDATE` chặn bằng `SIGNAL SQLSTATE '45000'` khi `OLD.thoi_diem_xac_nhan IS NOT NULL`;
   - trigger `BEFORE DELETE` trên mọi bảng nhóm 3 chỉ cho xóa khi `CURRENT_USER()` là `eldercare_luu_tru`, tài khoản riêng của job loại bỏ dữ liệu (FR-009, FR-048, FR-051).

   Quy ước đặt tên và mẫu trigger nằm ở [data-model.md](data-model.md#quy-uoc-bao-dam-o-co-so-du-lieu). Migration sinh ở giai đoạn triển khai.

*(Sửa 2026-10-29, constitution XII 3.2.0, R-15)* Bảng chứa cả dòng nhóm 2 và nhóm 3 (bảng gộp, `ho_so_chung`) không chặn được bằng quyền theo bảng ở lớp 3: dùng cột `khoa_luc` và trigger theo dòng (data-model mục 8). Quyền chỉ `SELECT, INSERT` vẫn áp cho bảng chỉ chứa nhóm 3. Ở lớp mã nguồn, entity của bảng nhiều nhóm gắn `@NhomDuLieu(NHIEU_NHOM)` và bắt buộc có trường `khoaLuc`; ArchUnit (`NhomDuLieuArchTest`) kiểm mỗi entity có đúng một chú thích, entity `NHIEU_NHOM` có `khoaLuc` và không có setter công khai; `NhomGhiNhanCoTriggerIT` kiểm entity `GHI_NHAN` có trigger không sửa, không xóa và entity `NHIEU_NHOM` có trigger khóa theo dòng. FR-001 vẫn đúng: mỗi **loại dữ liệu** (mỗi `loai`, `kind`) thuộc đúng một nhóm, khai báo ở data-model của module.

"Đã được đính chính" và "Hủy ghi nhận" không ghi ngược vào bản gốc. Chúng được suy ra từ bảng `dinh_chinh` (R-08), nên bản gốc không bao giờ bị `UPDATE`.

**Lý do**: Constitution XII yêu cầu không có UPDATE hay DELETE trên bản ghi đã xác nhận, kể cả với tài khoản Quản lý viện. Chặn ở cơ sở dữ liệu giữ được điều này ngay cả khi mã có lỗi (SC-002).

**Phương án đã cân nhắc**:
- Chỉ kiểm tra ở backend: bị loại vì một lỗi mã là phá được SC-002.
- Event sourcing toàn hệ thống: bị loại vì quá nặng cho đồ án. Bảng ghi thêm cùng nhật ký đã đủ cho nhu cầu truy vết.

---

## R-06. Nhật ký (FR-042 → FR-051a)

**Quyết định**:
- **Ghi**: lệnh nghiệp vụ gọi `NhatKyApi.ghi(BanGhiNhatKy)` ngay trong transaction của lệnh (`@Transactional`, `Propagation.MANDATORY`).
  - Nhật ký và thay đổi cùng commit hoặc cùng rollback, vì cùng một transaction MySQL (FR-047).
  - Không dùng Hibernate Envers hay listener tự động. Nhật ký cần lý do, căn cứ, dấu "tự duyệt", "áp dụng bù", "ngoài phạm vi"; listener không biết các thông tin này và có thể bỏ sót thay đổi ghi bằng câu lệnh hàng loạt.
- **Kiểm tra đủ nhật ký (SC-001)**: test tích hợp dùng `DataSource` proxy đếm câu `INSERT`/`UPDATE` trên bảng nhóm 2, 3. Mỗi transaction có thay đổi phải có ít nhất một `INSERT` vào `nhat_ky`. Ngoài ra, ArchUnit bắt mọi phương thức `@LenhNghiepVu` phải nhận `LyDo` khi lệnh đó bắt buộc lý do.
- **Dữ liệu trước/sau**: cột `JSON`, dạng `{ "<tenTruong>": giaTri }`, chỉ ghi các trường đã đổi. Phần chi tiết riêng (FR-043a) lưu ở cột `chi_tiet JSON`.
- **Che trường (FR-049, BR-M15-11)**:
  - mỗi loại đối tượng khai báo bảng `tenTruong → nhomThongTin` (ví dụ `diUng → SUC_KHOE`, `diaChi → LIEN_HE`) qua `PhanLoaiTruongProvider` của module sở hữu;
  - khi đọc nhật ký, `NhatKyQueryService` gọi `QuyenXemTruongApi` của feature 002 để biết người xem được xem nhóm thông tin nào. Trường không được xem trả `{"daThayDoi": true}`. Có ít nhất một trường bị che thì `lyDo` trả `null` kèm `lyDoBiAn: true`;
  - việc che làm ở backend, không gửi giá trị thật về client.
- **Phạm vi xem**:
  - Quản lý viện xem toàn viện.
  - Nhân viên chỉ xem lịch sử của một đối tượng cụ thể, sau khi `PhamViApi` (feature 002) xác nhận họ đang có quyền xem đối tượng đó. Nhân viên không có tra cứu toàn viện.
  - Người thân và người cao tuổi bị chặn ở tầng xác thực, theo vai trò AC-10, AC-16 (FR-049, Phụ lục 27).
- **Hiệu năng (SC-006, NFR-01)**:
  - Ước lượng khoảng 300 người cao tuổi, khoảng 200 bản ghi nhật ký mỗi người mỗi ngày (công việc, liều, chỉ số, cộng thêm thao tác nhân viên). Như vậy khoảng 60 nghìn bản ghi một ngày, khoảng 22 triệu một năm.
  - Chỉ mục `(doi_tuong_loai, doi_tuong_id, thoi_diem)`, `(nguoi_thuc_hien_id, thoi_diem)`, `(hanh_dong, thoi_diem)`.
  - Phân vùng `RANGE` theo năm của `thoi_diem`, nên khóa chính là `(id, thoi_diem)` và bảng không có khóa ngoại. Tham chiếu tới đối tượng là định danh logic.
  - Tra cứu dùng phân trang theo khóa (`thoi_diem`, `id`), không dùng `OFFSET`.
- **Bản tóm tắt đợt loại bỏ**: ghi vào chính bảng `nhat_ky`, `hanh_dong = LOAI_BO_DU_LIEU_HET_HAN`, `khong_loai_bo = true`. Job loại bỏ bỏ qua các dòng này (FR-050, FR-051).

**Lý do**: Ghi nhật ký tường minh trong cùng transaction là cách duy nhất đồng thời bảo đảm nguyên tử và đủ ngữ nghĩa.

**Phương án đã cân nhắc**:
- Hibernate Envers: thiếu lý do, căn cứ và ngữ cảnh lệnh.
- Trigger MySQL ghi nhật ký: không biết người thực hiện và lý do.
- Ghi nhật ký bất đồng bộ qua hàng đợi: vi phạm FR-047.

---

## R-07. Tham số cấu hình (FR-011 → FR-022)

**Quyết định**:
- Bảng `tham_so` (nhóm 1, mỗi mã một dòng) giữ giá trị hiện hành và `phien_ban`. Bảng `lich_su_tham_so` (nhóm 3, chỉ thêm) có một dòng cho mỗi giá trị từng hiện hành, gồm `hieu_luc_tu` (thời điểm lưu), giá trị, người đổi, lý do. Không có cột "hiệu lực đến": giá trị của một dòng hết hiệu lực khi dòng kế tiếp của cùng mã bắt đầu. Vì vậy bảng không bao giờ bị `UPDATE`.
  - Lần nạp ban đầu ghi một dòng lịch sử cho mỗi mã, giá trị mặc định, người thực hiện là Bộ lập lịch hệ thống, `hieu_luc_tu` là thời điểm khởi tạo.
  - DBR-24 được bảo đảm hai cách: `tham_so.ma` là khóa chính, nên mỗi mã có đúng một giá trị hiện hành; `UNIQUE (ma, hieu_luc_tu)` trên lịch sử, nên tại mỗi thời điểm có đúng một dòng lịch sử áp dụng. Lệnh đổi giá trị cập nhật `tham_so` và thêm dòng lịch sử trong cùng transaction. Test tích hợp kiểm `tham_so.gia_tri` luôn bằng giá trị của dòng lịch sử mới nhất.
- **Mã và giá trị**: mã CFG cố định trong một enum Java `MaThamSo`, sinh từ Phụ lục 25. Không có API tạo hay xóa mã (FR-011). Migration nạp dữ liệu ban đầu chứa mô tả, mặc định, cột "Dùng tại".
  - Kiểu giá trị: `THOI_LUONG`, `SO_NGUYEN`, `TY_LE`, `SO_TIEN`, `GIO_TRONG_NGAY`, `BANG`.
  - `BANG` lưu JSON theo JSON Schema riêng của từng mã. Đổi một dòng là một lần thay đổi cả tham số, với nội dung trước và sau đầy đủ (FR-022).
  - Mỗi mã có `rangBuoc`. Phần kỹ thuật hiển nhiên (thời lượng > 0, số nguyên ≥ 0, tỷ lệ 0–100) do plan đặt. Ràng buộc nghiệp vụ lấy từ nguồn, ví dụ CFG-M15-06 > CFG-M15-05.
- **Đọc lúc chạy**: `ThamSoApi.doc(MaThamSo)` trả `GiaTriThamSo` bất biến.
  - Có bộ nhớ đệm trong tiến trình. Bộ đệm bị xóa bằng sự kiện `ThamSoDaDoi` ở pha `AFTER_COMMIT`, nên lần kiểm tra quy tắc đầu tiên sau commit đã thấy giá trị mới (FR-016, SC-004).
  - Backend chạy một instance (đồ án). Nếu sau này chạy nhiều instance, cần thêm kênh xóa bộ đệm chung; ghi ở rủi ro.
  - Mỗi lần kiểm tra quy tắc đọc tham số một lần vào biến cục bộ, nên dùng trọn một giá trị (FR-018).
- **Tra cứu quá khứ (FR-019)**: `ThamSoApi.docTaiThoiDiem(ma, Instant)` lấy dòng `lich_su_tham_so` có `hieu_luc_tu` lớn nhất mà `<= t`.
- **Đổi giá trị**: `POST /tham-so/{ma}/doi-gia-tri` với `If-Match`. Phiên bản lệch thì trả 412 kèm giá trị mới (FR-021). Khôi phục mặc định là cùng lệnh, giá trị bằng mặc định (FR-020).
- **Nhắc trước giá trị theo module**: FR-031a cho phép module đặt thời hạn ngắn hơn. Module làm việc này bằng mã CFG riêng của nó (ví dụ CFG-M10-11), không ghi đè CFG-M15-05.

**Lý do**: Constitution XIII, BR-M15-04, NFR-12.

**Phương án đã cân nhắc**:
- Spring Cloud Config hoặc file `application.yml`: bị loại vì cần triển khai lại hoặc khởi động lại, và không có lịch sử nghiệp vụ.
- Một bảng duy nhất có cột `hien_hanh`: bị loại vì phải `UPDATE` cờ trên dòng cũ, khó bảo đảm duy nhất.

---

## R-08. Đính chính (FR-023 → FR-030)

**Quyết định**:
- Một bảng chung `dinh_chinh` (nhóm 3, chỉ thêm). Mỗi dòng gồm:
  - `loai_ban_ghi`: mã loại của module, ví dụ `M07.LIEU_THUOC`;
  - `id_ban_ghi_goc`;
  - `kieu`: `SUA_NOI_DUNG` hoặc `HUY_GHI_NHAN`;
  - `noi_dung_dung` (JSON);
  - `ly_do`, `nguoi_thuc_hien_id`, `thoi_diem`;
  - `cach_tao`: `A_NGUOI_GHI_GOC`, `B_PHU_TRACH_TANG` hoặc `C_YEU_CAU_PHE_DUYET`;
  - `yeu_cau_phe_duyet_id`, chỉ có với cách (c).
- Mọi bản đính chính trỏ về bản gốc ban đầu (FR-027). Giá trị hiện hành là bản `SUA_NOI_DUNG` mới nhất, xếp theo `(thoi_diem, id)`.
- **Tuần tự hóa** đính chính trên cùng một bản gốc: bảng kỹ thuật `chuoi_dinh_chinh (loai_ban_ghi, id_ban_ghi_goc, so_ban, da_huy, phien_ban)` có khóa lạc quan.
  - Thêm đính chính đồng thời tăng `so_ban`. Bên đến sau gặp xung đột và thử lại, nên thứ tự xác định được.
  - Đã `da_huy = true` thì từ chối `SUA_NOI_DUNG` (FR-029).
  - Bảng này là dữ liệu dẫn xuất kỹ thuật, không phải bản ghi nhóm 3, nên được `UPDATE`.
- **SPI cho module sở hữu** (`DinhChinhDuocProvider<T>`):
  - `loaiBanGhi()`;
  - `ganTangKhuVuc(id)` trả tầng/khu vực hoặc rỗng (FR-026 b/c, CHK051);
  - `nguoiGhiGoc(id)`, `daXacNhan(id)`;
  - `kiemTraNoiDung(json)`;
  - `coQuyenGhiNhan(nguoiDung)`;
  - `thuHep(...)` (tùy chọn, FR-026 "module MAY thu hẹp");
  - `apDung(id, banDinhChinh)` (tùy chọn, để module phát sự kiện tác động dây chuyền, ví dụ 010 lập khoản điều chỉnh).
- **Chọn cách tạo (FR-026)**: `DinhChinhService` thử lần lượt (a), (b), (c). Với (b), dùng `PhamViApi` (002) để xác định "người phụ trách ca / trưởng tầng của tầng đó, trong phạm vi hiện tại", gồm cả nhánh dự phòng CHK033. Cách (c) tạo một yêu cầu phê duyệt loại `DINH_CHINH` (R-10). Bản đính chính chỉ được thêm khi yêu cầu đó Đã áp dụng.
- **Đọc giá trị hiện hành (FR-030)**:
  - API `DinhChinhApi.hienHanh(loai, ids)` trả theo lô;
  - view MySQL `v_dinh_chinh_moi_nhat` (bản sửa mới nhất mỗi bản gốc) và `v_ban_ghi_da_huy` để truy vấn báo cáo của module tự `LEFT JOIN`. Báo cáo không tự cài lại logic "mới nhất".
- Chi phí đã chốt và giao dịch số dư không đăng ký provider, vì không có đính chính (1.5, DBR-17). Gọi đính chính cho các loại này thì trả 409 `KHONG_HO_TRO_DINH_CHINH`.

**Lý do**: Một cơ chế cho mọi loại bản ghi nhóm 3 để giữ đúng một quy tắc (FR-026) cho mọi module; module chỉ khai báo phần riêng.

**Phương án đã cân nhắc**:
- Mỗi module có bảng đính chính riêng: lặp quy tắc ở 10 module, dễ lệch.
- Ghi cờ "đã đính chính" lên bản gốc: vi phạm R-05.

---

## R-09. Sự kiện giữa module và thông báo

**Quyết định**: Dùng sự kiện ứng dụng của Spring Modulith, có Event Publication Registry lưu ở MySQL (transactional outbox). Sự kiện chỉ được giao sau commit và được giao lại nếu tiến trình chết giữa chừng.

000 phát các sự kiện:
- `YeuCauDaGuiDuyet`;
- `YeuCauCanNhacNguoiDuyet`;
- `YeuCauCanBaoQuanLyVien`;
- `YeuCauDaQuyetDinh`;
- `YeuCauApDungKhongThanh`;
- `ThamSoDaDoi`;
- `DaDinhChinh`.

Feature 009 (M13) lắng nghe các sự kiện này và gửi thông báo (constitution XIV). 000 không gọi trực tiếp kênh gửi nào. Mức thông báo do module nguồn nêu trong payload sự kiện.

**Lý do**: Constitution XIV yêu cầu thông báo qua dịch vụ chung. Gửi sau commit không thông báo một thay đổi đã rollback. Outbox không mất thông báo nhắc.

**Phương án đã cân nhắc**:
- Gọi đồng bộ `ThongBaoApi`: lỗi gửi có thể làm hỏng lệnh nghiệp vụ.
- Message broker ngoài (Kafka, RabbitMQ): thừa với một khối.

---

## R-10. Yêu cầu phê duyệt (FR-031 → FR-041)

**Quyết định**:
- **Bảng**: `yeu_cau_phe_duyet` (nhóm 2). Lịch sử trạng thái lưu ở `nhat_ky`. Tác động lưu ở dữ liệu của module, có cột `yeu_cau_phe_duyet_id` (FR-039).
- **Máy trạng thái**: viết bằng mã. Trạng thái là chuỗi mã. Bảng chuyển chung ở [data-model.md](data-model.md#yeu_cau_phe_duyet) được cài thành `BangChuyenTrangThai`. Loại yêu cầu được khai báo thêm trạng thái và chuyển (015: `CHO_NGUOI_NHAN_DONG_Y`, `NGUOI_NHAN_TU_CHOI`, `DA_AP_DUNG_MOT_PHAN`, `HET_HIEU_LUC`; 001: `DUOC_THAY_THE`). Chuyển không khai báo thì bị chặn.
- **SPI** (`LoaiYeuCauHandler`), mỗi loại yêu cầu do module sở hữu cài một handler:
  - `maLoai()`;
  - `kiemTraNoiDung(json)`;
  - `quyenTao()`, `quyenDuyet()`: khóa quyền đối chiếu cột T, D của Phụ lục 27 qua 002;
  - `phamVi(yeuCau)`: đối tượng hoặc tầng, để kiểm tra phạm vi duyệt;
  - `kiemTraDieuKienApDung(yeuCau)`: trả danh sách điều kiện chưa thỏa;
  - `apDung(yeuCau, ngayHieuLucThucTe)`: ghi tác động, trả tham chiếu kết quả;
  - `choTuDuyet()`: mặc định `true`; ví dụ 013 FR-027a trả `false`;
  - `maThamSoNhac()`: tùy chọn, mã CFG riêng của module.
- **Áp dụng đúng một lần (FR-037, SC-003)**:
  - mỗi lần áp dụng chạy trong một transaction riêng: khóa dòng yêu cầu bằng `SELECT … FOR UPDATE`, kiểm tra còn `DA_DUYET`, gọi `kiemTraDieuKienApDung`, rồi gọi `apDung`, rồi chuyển `DA_AP_DUNG`, rồi ghi nhật ký;
  - `UNIQUE (yeu_cau_phe_duyet_id)` trên bảng `ket_qua_ap_dung` chặn trùng ngay cả khi có lỗi logic; *(R-15: `ket_qua_ap_dung` → `yeu_cau_phe_duyet` (cột))*
  - lần kích hoạt sau thấy trạng thái đã kết thúc thì không làm gì.
- **Lỗi khi áp dụng (FR-037b)**: transaction rollback, yêu cầu vẫn `DA_DUYET`. Một transaction riêng (`REQUIRES_NEW`) ghi nhật ký `AP_DUNG_LOI` kèm nguyên nhân.
- **Điều kiện không thỏa (FR-038)**: chuyển `AP_DUNG_KHONG_THANH`, ghi `dieuKienChuaThoa` vào lý do, phát `YeuCauApDungKhongThanh`.
- **Bộ lập lịch**:
  - Spring `@Scheduled`; mỗi lần chạy có một dòng `lan_chay_lich` (id, job, bắt đầu, kết thúc, kết quả), dùng làm "định danh lần chạy" trong căn cứ nhật ký (FR-045); *(R-15: `lan_chay_lich` → file `logs/jobs.log`)*
  - job `ApDungYeuCauDenHan` chạy lúc 00:05 hằng ngày và khi ứng dụng khởi động. Nó lấy mọi yêu cầu `DA_DUYET` có `ngay_hieu_luc_thuc_te <= hôm nay`, sắp theo `(ngay_hieu_luc_thuc_te, thoi_diem_quyet_dinh)`, áp dụng lần lượt (FR-037a, BR-M15-10);
  - "Áp dụng bù" là yêu cầu có ngày hiệu lực thực tế trước ngày chạy. Nhật ký mang dấu `apDungBu = true` cùng cả hai mốc;
  - job `NhacYeuCauChoDuyet` chạy mỗi 15 phút (chu kỳ kỹ thuật, không phải tham số nghiệp vụ). Hai cột `da_nhac_luc` và `da_bao_qlv_luc` chống nhắc lặp. Cách tính theo giờ liên tục từ `thoi_diem_gui_duyet`. Khi không còn người duyệt thì báo Quản lý viện ngay ở mốc CFG-M15-05 (BR-M15-09).
  - Hai job dùng ShedLock (bảng `shedlock`) để không chạy chồng khi kích hoạt tay hoặc khi khởi động lại.
  - Giới hạn chung cho mọi job (FR-010a, Q-306, Q-307): job chỉ chuyển trạng thái theo thời gian, cảnh báo, leo thang, nhắc, áp dụng yêu cầu đã duyệt và loại bỏ dữ liệu hết hạn; job không tạo bản ghi nghiệp vụ, không gợi ý. Kiểm tra bằng một quy tắc ArchUnit: lớp trong gói `..lich..` chỉ được gọi các cổng chuyển trạng thái, nhắc, cảnh báo đã khai báo, không gọi trực tiếp repository tạo bản ghi nghiệp vụ.
- **Ngày hiệu lực thực tế (FR-037)**: `max(ngayHieuLucMongMuon, ngàyDuyệt)`, tính khi duyệt. Nếu kết quả ≤ hôm nay thì áp dụng ngay trong transaction riêng sau khi quyết định đã commit. Quyết định duyệt không bị rollback vì áp dụng lỗi; yêu cầu nằm lại `DA_DUYET` và job xử lý ở lần sau, đúng FR-037b.
- **Đồng thời (FR-040)**: cột `phien_ban` (khóa lạc quan) cùng `If-Match`. Quyết định đến sau trả 409 `DA_DUOC_XU_LY`.
- **Quyền xét lúc quyết định (FR-034a)**: không lưu sẵn danh sách người duyệt. Mỗi lần quyết định đều gọi `QuyenApi` (002).
- **Giới hạn vai trò người duyệt** *(bổ sung theo plan 004, Q-338)*: SPI có `default Optional<Set<VaiTro>> gioiHanVaiTroDuyet(yc)` trả rỗng. Loại `M02.THAY_DOI_LUU_TRU` trả `{QUAN_LY_VIEN}` khi yêu cầu không phải thay đổi nhỏ (004 FR-043), vì Phụ lục 27 cho Hành chính quyền D có chú thích ⁴² trên cả dòng. 000 áp giới hạn ở lệnh Duyệt, Từ chối (kể cả tự duyệt), `lenhDuocPhep`, danh sách người duyệt của nhắc và BR-M15-09, danh sách người nhận khi gửi duyệt; mọi chỗ dùng cùng một hàm tính danh sách (contracts mục 1.5). Vai trò kiểm bằng `TaiKhoanApi.coVaiTro` (cùng module M15). Phương án bỏ: chú thích có điều kiện `DieuKienChuThich` của 002 cho ⁴² (002 research A-04: chú thích chỉ mô tả, kiểm ở module, U2), và tách thành hai loại yêu cầu (đổi FR-037 của 004).

**Lý do**: Áp dụng ngay sau commit tách được quyết định khỏi tác động, đúng bảng trạng thái của spec: lỗi áp dụng giữ "Đã duyệt (chờ hiệu lực)".

**Phương án đã cân nhắc**:
- Spring State Machine: nặng và khó kiểm thử hơn bảng chuyển viết tay.
- Quartz: thừa với một instance; `@Scheduled` cùng ShedLock đủ dùng.

---

## R-11. Lưu giữ và loại bỏ dữ liệu (FR-050, FR-051, FR-051a)

**Quyết định**:
- Module đăng ký `LuuTruProvider` cho từng loại dữ liệu của mình:
  - gắn với hồ sơ người cao tuổi nào, nếu có;
  - mốc bắt đầu tính hạn (kết thúc lưu trú, hoặc thời điểm tạo);
  - danh sách tham chiếu ra và vào.
- Job `LoaiBoDuLieuHetHan` chạy hằng tháng, bằng một `DataSource` riêng với tài khoản `eldercare_luu_tru` (R-05):
  1. Tính tập ứng viên theo CFG-M15-03 và CFG-M15-04 hiện hành.
  2. Dựng chuỗi tham chiếu: bản gốc, các bản đính chính, nhật ký của các bản ghi trong chuỗi, và các bản ghi tham chiếu.
  3. Loại khỏi tập mọi chuỗi còn phần tử chưa hết hạn (FR-051a).
  4. Xóa từng chuỗi trong một transaction (BR-M15-13).
  5. Ghi một bản nhật ký tóm tắt bằng tài khoản ứng dụng, sau khi đợt chạy kết thúc hoặc dừng giữa chừng, với số đếm thực tế (FR-051).
- CFG-M15-03 và 04 mặc định 10 năm, nên trong thời gian đồ án job không xóa gì trên dữ liệu thật. Job vẫn được kiểm thử đầy đủ bằng đồng hồ giả lập (NFR-13).
- Phạm vi của 000 là khung job, SPI và nhật ký tóm tắt. Mỗi module khai báo provider trong plan của nó.

**Lý do**: Chỉ một tác nhân (AC-11) được xóa, có tài khoản cơ sở dữ liệu riêng để trigger kiểm được.

**Phương án đã cân nhắc**:
- Xóa mềm (cờ `da_loai_bo`): không đáp ứng mục đích "loại bỏ" sau thời hạn.
- `DROP PARTITION` theo năm: không đúng ngữ nghĩa theo chuỗi và theo hồ sơ.

---

## R-12. Xác thực và quyền: phần 000 dùng từ 002

**Quyết định**: 000 không tự cài xác thực. 000 gọi các giao diện của 002, được khai báo trước ở [contracts/module-interfaces.md](contracts/module-interfaces.md):
- `NguoiDungHienTai`: id, vai trò, có phải Bộ lập lịch không;
- `QuyenApi.coQuyen(nguoiDung, khoaQuyen, phamVi)`: ba lớp BR-M15-01;
- `PhamViApi`: đối tượng hoặc tầng trong phạm vi; người phụ trách ca, trưởng tầng của tầng;
- `QuyenXemTruongApi`.

Trong lúc 002 chưa có, test của 000 dùng bản cài giả. Plan của 002 sẽ chốt cơ chế token (dự kiến JWT access token ngắn hạn kèm refresh token, Spring Security Resource Server). Feature 000 không phụ thuộc vào lựa chọn này.

**Lý do**: Thứ tự plan 000 → 002 (CLAUDE.md). Giao diện cần có trước để 000 thiết kế được.

---

## R-13. Ngoại tuyến: phần khung dùng chung (FR-046, DBR-25)

**Quyết định**: 000 chỉ định nghĩa khung chung. Các lệnh của chính 000 (đổi tham số, phê duyệt, đính chính, tra cứu nhật ký) **chỉ chạy trực tuyến**, vì 8.6 chỉ cho ghi tạm với "ghi nhận thực hiện" và các lệnh này cần trạng thái máy chủ (khóa phiên bản, quyết định đầu tiên thắng). Khung chung gồm:
- phong bì đồng bộ: `Idempotency-Key`, `thoiDiemThietBi`, `ngoaiTuyen = true`;
- cột `thoi_diem_thiet_bi` trong `nhat_ky`;
- kết quả đồng bộ từng bản ghi (`CHAP_NHAN`, `CHAP_NHAN_KEM_NHAN`, `CHO_XEM_LAI`, `TU_CHOI`; BR-M15-20) trả cho app.

Hàng đợi trên thiết bị dùng drift (SQLite) trong app nhân viên. Quy tắc nhận, đưa vào "chờ xem lại" theo CFG-M15-08 do plan 002 và 005 chi tiết.

**Lý do**: Không mở rộng ngoại tuyến ngoài 8.6 (constitution IX). Khung thống nhất cho các module ghi nhận dùng chung.

---

## R-14. Kiểm thử (constitution XV, NFR-13)

**Quyết định**:
- **Backend**:
  - test đơn vị cho máy trạng thái, chọn cách đính chính, kiểm tra tham số;
  - test tích hợp chạy trên MySQL thật, có trigger, tài khoản `eldercare_app` và `eldercare_luu_tru`, để chứng minh SC-002 ở tầng cơ sở dữ liệu. Từ 2026-10-16 (chưa dùng Docker): dùng MySQL 8.0 cài trên máy, schema riêng cho test (mặc định `eldercare_test`, không dùng chung với schema chạy demo); địa chỉ, tài khoản quản trị đọc từ biến môi trường `TEST_DB_URL`, `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD`, không ghi mật khẩu vào kho mã. Lớp cơ sở test xóa và tạo lại schema, tạo hai tài khoản trên rồi chạy Flyway. Khi có Docker thì đổi lớp cơ sở sang Testcontainers, không đổi test;
  - test kiến trúc bằng `ApplicationModules.verify()` và ArchUnit.
  - Không thay MySQL bằng H2 hay cơ sở dữ liệu nhúng.
- **Đặt tên test**: `BR_M15_09_noApproverLeft_notifiesDirectorAtReminderTime`, hoặc theo FR khi FR không có mã BR (`FR_037_repeatedApply_noSecondEffect`). Mỗi kịch bản Given/When/Then của spec 000 có một test. Bảng truy vết nằm ở [quickstart.md](quickstart.md).
- **Đồng hồ**: `DieuKhienDongHo`. Không dùng `Thread.sleep`.
- **Hợp đồng API**: test so `springdoc` sinh ra với `contracts/*.yaml` (bằng openapi-diff); lệch thì test lỗi.
- **Client**:
  - Web dùng Vitest và React Testing Library cho màn hình tham số, nhật ký (hiện "đã thay đổi"), duyệt yêu cầu, xử lý 412;
  - Flutter dùng `flutter_test` cho màn duyệt yêu cầu trên app nhân viên.
  - Không có BR của 000 thực hiện ở client nên không bắt buộc test BR ở client (XV); các test trên kiểm tra hiển thị.
---

## R-15. Gộp bảng cơ sở dữ liệu (bổ sung 2026-10-29, sửa cùng ngày lần 2, chủ dự án duyệt)

**Quyết định**: Giảm số bảng từ khoảng 300 (data-model của 20 feature) xuống 79 (chủ dự án chốt 2026-10-29). Không đổi nghiệp vụ, API, quyền. Ánh xạ từng bảng ở mục "0. Gộp bảng theo R-15" đầu data-model của mỗi feature; phần mô tả bảng cũ bên dưới vẫn là căn cứ cột và ràng buộc.

1. **Luật gộp**:
   - **Nhóm theo dòng** (constitution XII, 3.2.0): một bảng được chứa dòng nhóm 2 và nhóm 3. Dòng nhóm 3, hoặc dòng nhóm 2 đã khóa (xác nhận, chốt, đóng), có `khoa_luc NOT NULL`; trigger `BEFORE UPDATE` chặn đổi mọi cột của dòng đã khóa, trừ các cột trạng thái dẫn xuất được liệt kê ở data-model của bảng; trigger `BEFORE DELETE` theo R-05. Lớp quyền tài khoản theo bảng của R-05 chỉ còn áp cho bảng chỉ chứa nhóm 3 (`nhat_ky`, `dinh_chinh`, `lich_su_tham_so`, `lich_su_chung`, `su_kien_chung`);
   - bản ghi con gộp vào cha khi quan hệ 1–1 hoặc ghi một lần cùng cha (cột hoặc `JSON`); bản ghi con ghi thêm dần thành dòng của `su_kien_chung`;
   - giữ bảng riêng khi có `CHECK`, `UNIQUE`, khóa ngoại bảo đảm BR, DBR mà bảng chung không biểu diễn được, hoặc là đích tham chiếu của nhiều bảng: `lich_su_trang_thai_giuong` (BR-M03-10), `lich_su_tham_so` (DBR-24), `nhan_vien_bo_phan` (DBR-36), `phien_ban_don_gia` (DBR-08), `ban_giao_do_gui` (BR-M15-03), `dose_result` (006: đích của đính chính `M07.DOSE_RESULT`, tách nhóm 3 khỏi lịch liều), `device_credential` (002: tra mã băm mỗi lần đăng nhập), `push_device` (009: token đẩy do 009 quản lý, không mở API sang 002);
   - ràng buộc duy nhất của bảng cũ chuyển thành cột `khoa_duy_nhat VARCHAR(200) NULL UNIQUE` do backend đặt theo luật của từng `loai` (đặt `NULL` khi dòng hết hiệu lực); test tích hợp của module chứng minh từng ràng buộc;
   - cột trong `JSON` mà job quét hoặc báo cáo lọc thì thêm cột sinh có chỉ mục (migration của module sở hữu, tên cột có tiền tố module).
2. **Bảy bảng dùng chung** (data-model 000 mục 8):
   - `danh_muc_chung` (nhóm 1): mọi danh mục, kể cả danh mục có thuộc tính chuyên môn (`thuoc` và hoạt chất, `mon_an` và thành phần dị ứng, xác nhận của Bác sĩ, `dich_vu`, `giay_phep_co_so`, `vai_tro`, `tang`, `khu_vuc`); thuộc tính riêng trong `thuoc_tinh JSON`, kiểm bằng JSON Schema của `loai`. Thuộc tính theo phiên bản vẫn ở bảng phiên bản (`phien_ban_don_gia`);
   - `lich_su_chung` (nhóm 3): lịch sử trạng thái, lịch sử phụ trách dạng "từ … đến …";
   - `su_kien_chung` (nhóm 3): mọi bản ghi con ghi thêm dần (ý kiến, phản hồi, diễn biến, kết quả gọi, lần gửi, sai lệch, điểm danh, kết quả kiểm tra, bàn giao trong quy trình) và bản ghi nhóm 3 nhỏ không có bảng cha riêng;
   - `ho_so_chung` (nhóm 2, khóa theo dòng): đối tượng nhỏ có máy trạng thái đơn giản, không là đích khóa ngoại của bảng khác ngoài bảng chung; nội dung trong `noi_dung JSON`, máy trạng thái ở lớp domain của module;
   - `moc_da_nhac` (kỹ thuật, chỉ thêm): dấu "đã nhắc", "đã cảnh báo", "đã xử lý sự kiện", lần chạy cần biết đã chạy;
   - `khoa_ky_thuat` (kỹ thuật): dòng chỉ dùng để khóa (`SELECT … FOR UPDATE`);
   - `trang_thai_nct` (nhóm 2 kỹ thuật): thay mọi bảng `*_nct` của module, khóa chính `(nct_id, module)`, dữ liệu riêng trong `du_lieu JSON`.
   `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct` thuộc phần mới `administration.shared`; module ghi, đọc qua `CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi` ở `administration.shared.api` (constitution X: không truy cập trực tiếp dữ liệu module khác; mỗi dòng ghi `module` sở hữu, API chỉ cho module đọc, ghi dòng của mình trừ khi `loai` khai báo cho đọc chung). `moc_da_nhac`, `khoa_ky_thuat` ở `common` (`ReminderMarkers`, `TechnicalLocks`). Job loại bỏ dữ liệu (R-11) xóa dòng bảng chung theo `(loai_doi_tuong, doi_tuong_id)` hoặc `nct_id` của chuỗi bản ghi module trả về.
   *(Bổ sung 2026-10-30, chủ dự án duyệt khi làm 009 T043)* **Đọc bằng SQL dòng loại của mình**: module được `SELECT`, `JOIN` dòng bảng chung có `loai` mang tiền tố module mình (`M13.…`), trong lớp `persistence`, cùng bảng riêng của module (phép nối, `NOT EXISTS`, đếm mà API theo đối tượng cha không làm được, ví dụ lần gửi chưa có kết quả); cột `JSON` cần lọc thường xuyên thì thêm cột sinh có tiền tố module ở file `_94`. Mọi lệnh ghi (thêm, đổi trạng thái, khóa) vẫn qua API của `administration.shared` để giữ kiểm nội dung, `khoa_duy_nhat`, khóa dòng. Không đọc dòng loại của module khác. Job loại bỏ: lớp cài `RetentionProvider` gọi `SharedRowRetention.deleteFor` cho dòng bảng chung gắn với bản ghi con của chuỗi (đối tượng khác `dataType()`), trong transaction của chuỗi.
3. **Nhật ký nghiệp vụ giữ trong cơ sở dữ liệu** (`nhat_ky`, R-06): nghiệp vụ 19.4, BR-M15-11, NFR-07. Các bảng "lần xuất", "lần in", "lần xem" (010, 007, 012) thành hành động nhật ký.
4. **Log kỹ thuật ra file**: lần chạy job (`scheduler_run`, `m07_job_run`, `m09_job_run`, `m13_job_run`) ghi bằng structured logging của Spring Boot (`logging.structured.format.file`), file `logs/jobs.log`, xoay theo ngày, giữ 180 ngày, sao lưu cùng thư mục log (backend/docs/operations.md). Mã lần chạy (UUID) đưa vào MDC `jobRunId` và `can_cu.lanChayLichId` của nhật ký (FR-045). Nhật ký giữ 10 năm (NFR-07) còn file log giữ 180 ngày, nên bản ghi nhật ký của Bộ lập lịch phải tự đủ căn cứ: ghi cả mã job, thời điểm chạy và mã quy tắc hoặc yêu cầu phê duyệt (FR-045); mã lần chạy chỉ dùng để tra chi tiết trong 180 ngày. Giữ trong cơ sở dữ liệu: `khoa_lap_lai`, `event_publication`, `shedlock`.
5. **Bảng đã cài**: không sửa migration cũ. Mỗi chủ module viết migration mới: tạo hoặc mở rộng bảng gộp, chép dữ liệu, đổi khóa ngoại, `DROP TABLE` bảng cũ. Dòng chép ghi `legacy = {bang, id}` trong `JSON` của dòng; tệp đính kèm (`tep.chu_so_huu_id`) cập nhật theo id mới trong cùng migration. Bảng là đích của `dinh_chinh` hoặc được `nhat_ky` trỏ theo id: bảng gộp giữ nguyên id của bảng đó (bảng gộp vào cấp id mới), `loaiBanGhi` của đính chính và `doi_tuong_loai` của nhật ký cũ được ánh xạ sang loại mới ở lớp đọc (không sửa dòng nhật ký). Migration chung của A (`V000_10`) tạo bảy bảng, trigger, quyền trước migration module. *(Bổ sung cùng ngày)* Migration gộp của module đánh số `V<NNN>_95` để chạy sau dữ liệu mẫu `demo/V<NNN>_90…` trên cơ sở dữ liệu mới (Flyway xếp theo số; ví dụ `demo/V002_90` ghi `account_status_history`, `role_assignment` nên phải chạy trước `V002_95`); không sửa script demo đã chạy (đổi checksum), dữ liệu mẫu cũ được migration gộp chép như dữ liệu thật; script demo viết sau khi gộp đánh số từ `_97` và ghi vào bảng mới. Phần module thêm vào bảng dùng chung (cột sinh, chỉ mục) đặt ở file riêng `V<NNN>_94__shared_<mô tả>.sql`, A review (CODEOWNERS); không sửa định nghĩa khác của bảng chung.

**Lý do**: Viện khoảng 40 nhân viên; nhiều bảng chỉ khác tên mà cùng cấu trúc (danh mục, lịch sử, sự kiện con, đối tượng nhỏ có trạng thái, dấu đã nhắc, khóa). Chủ dự án đặt mục tiêu 70–80 bảng để migration, entity, test và sao lưu gọn hơn.

**Đánh đổi chấp nhận**: bản ghi trong bảng chung mất cột có kiểu, khóa ngoại và `CHECK` riêng; bảo đảm chuyển về lớp domain, service của module và test tích hợp (constitution XII: DBR được bảo đảm bằng ràng buộc cơ sở dữ liệu hoặc kiểm tra ở backend, plan chỉ rõ cách). Báo cáo (016) đọc qua API của module như trước, không đọc JSON trực tiếp. Bảo vệ nhóm 3 ở cơ sở dữ liệu với bảng chứa nhiều nhóm dựa vào trigger theo dòng thay cho quyền theo bảng.

**Phương án đã cân nhắc**:
- Bản 1 cùng ngày (khoảng 190 bảng, giữ luật nhóm theo bảng): thay bằng bản này theo yêu cầu 70–80 bảng của chủ dự án.
- Rút gọn nghiệp vụ để giảm bảng (Q-398 → Q-401 đề xuất: danh sách tiếp xúc, phiếu bữa ăn, kiểm tra chất lượng, người thân ở lại): không cần cho mục tiêu số bảng sau khi có `ho_so_chung`, `su_kien_chung`; chủ dự án xét riêng.
- Ghi nhật ký nghiệp vụ ra file: bị loại vì mất màn xem lịch sử theo quyền và không bảo đảm giữ 10 năm cùng hồ sơ.
