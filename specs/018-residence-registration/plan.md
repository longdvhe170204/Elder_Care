# Implementation Plan: Khai báo tạm trú, lưu trú

**Branch**: `018-residence-registration` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/018-residence-registration/spec.md`

## Summary

Feature 018 là phần 6.10 của module M02 (UC-80, UC-81; BR-M02-11 → 14; DBR-33). Hành chính lập khai báo khi được nhắc, ghi nộp và kết quả; hệ thống xác định loại khai báo theo thời gian lưu trú cộng dồn, nhắc quá hạn, nhắc gia hạn, chuyển Hết hạn, nhắc khai báo xóa; không nộp hồ sơ thay, không kết nối Cổng dịch vụ công.

Hướng kỹ thuật:
- Gói `m02.khaibao` cạnh 004 trong M02, đọc hợp đồng qua dịch vụ nội bộ M02 (research Z-01).
- Nghĩa vụ khai báo là số liệu tính, giữ đệm kỹ thuật `nghia_vu_khai_bao` để nhắc đúng ngày, chạy bù (Z-03).
- `TongThoiGianLuuTru`, `LoaiKhaiBao` là hàm thuần (Z-03); DBR-33 bằng hai cột sinh `UNIQUE` (Z-04).
- Bean trạng thái không chặn lệnh nào; hủy khai báo đang xử lý khi trạng thái cuối (Z-05).

Plan tuân theo FR-010a của 000 (Q-306): hệ thống không tạo khai báo, khai báo gia hạn, việc khai báo xóa; chỉ nhắc, chuyển Hết hạn theo giờ, chuyển Được thay thế và Đã hủy kèm lệnh.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX) (000 R-01). Không có màn trên app di động.

**Primary Dependencies**: Như 000 → 017. Không thêm thư viện.

**Storage**: MySQL 8.0, schema M02. Bảng ở [data-model.md](data-model.md).

**Testing**: Như 000 R-14; test bảng hai hàm thuần; Test tích hợp MySQL cho DBR-33; đồng hồ giả lập cho CFG-M02-09, 11 → 13.

**Target Platform**: Web (Hành chính, Quản lý viện).

**Project Type**: Web service (REST `/api/v1`) và web client.

**Performance Goals**: Job hằng ngày cho 300 hồ sơ ≤ 1 phút; ghi nộp, kết quả ≤ 2 phút thao tác (SC-005).

**Constraints**: Mỗi lệnh một endpoint; nội dung ghi (nộp, kết quả, xóa) nhóm 3 qua đính chính; không chặn Hoàn tất tiếp nhận, Kết thúc lưu trú (SC-004).

**Scale/Scope**: 1 thực thể có vòng đời; 5 bảng (gồm kỹ thuật); 1 job; 3 đường dẫn, 4 thao tác REST ([contracts/api-018.yaml](contracts/api-018.yaml)); 5 SPI hoặc provider cài; 1 giao diện thống kê.

Không còn mục NEEDS CLARIFICATION.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | x-fr, x-br; data-model mục 5 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi |
| III. Không mặc định CRUD | Đạt | Chỉ lệnh theo bảng FR-005 |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển data-model mục 3 khớp FR-005 |
| V. Không cố định giá trị | Đạt | CFG-M02-09, 11 → 13 |
| VI. Vai trò, quyền | Đạt | Hành chính thực hiện, Quản lý viện xem (FR-009) |
| VII. Điểm chưa rõ | Đạt | Điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-005 ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế |
| X. Kiến trúc | Đạt | Gói con của `m02`; không có vòng |
| XI. Nghiệp vụ ở backend | Đạt | Loại, hạn, nghĩa vụ ở backend |
| XII. Dữ liệu | Đạt | DBR-33 bằng `UNIQUE`; nhật ký trong transaction |
| XIII. Tham số | Đạt | Hạn chốt lúc ghi mốc; đổi CFG không đổi hạn đã có |
| XIV. Ngoại tuyến, thông báo | Đạt | Không ngoại tuyến; thông báo Nhẹ (FR-010) |
| XV. Truy vết, kiểm thử | Đạt | Test theo BR-M02-11 → 14, DBR-33 |

**Kiểm lại sau Phase 1**: Đạt.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 018 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 hồ sơ; vài chục khai báo mỗi tháng |
| NFR-02 Hiệu năng | Có | Mỗi lệnh ghi một khai báo dưới khóa `khai_bao_nct`; ghi nộp, kết quả ≤ 2 phút thao tác (SC-005) |
| NFR-03 Thông báo khẩn cấp | Không | Thông báo mức Nhẹ (FR-010) |
| NFR-04 Tác vụ định kỳ | Có | Job hằng ngày cho 300 hồ sơ ≤ 1 phút; `nhac_khai_bao` `UNIQUE (loai, nct_id, ngay)` chống nhắc trùng; chạy bù (research Z-06) |
| NFR-05 Khả dụng | Gián tiếp | Listener sự kiện của 004 idempotent theo mốc nghĩa vụ |
| NFR-06 Sao lưu | Có | Bảng khai báo ở MySQL; bằng chứng ở kho tệp của 000 |
| NFR-07 Lưu trữ | Có | Theo hồ sơ (CFG-M15-03, `LuuTruProvider`) |
| NFR-08 Bảo mật | Có | Chỉ Hành chính, Quản lý viện; tệp bằng chứng `M02.BANG_CHUNG_KHAI_BAO` |
| NFR-09 Thời gian | Có | `Clock`; hạn khai báo theo ngày Asia/Ho_Chi_Minh |
| NFR-10 Thiết bị | Có | Màn trên web; không có màn trên app di động |
| NFR-11 Ngôn ngữ | Có | Nhãn tiếng Việt; ngày dd/MM/yyyy |
| NFR-12 Cấu hình | Có | CFG-M02-09, 11 → 13 đọc qua `ThamSoApi` |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập cho hạn, nhắc, hết hạn |

## Project Structure

### Documentation (this feature)

```text
specs/018-residence-registration/
├── plan.md  ├── research.md (Z-01 → Z-07)  ├── data-model.md  ├── quickstart.md (Q1 → Q20)
├── contracts/ (api-018.yaml, module-interfaces.md)
└── tasks.md   # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai; giai đoạn thiết kế không tạo (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m02/khaibao/   {internal, web, lich}   # nghĩa vụ, khai báo, TongThoiGianLuuTru, LoaiKhaiBao, bean trạng thái
backend/src/main/java/com/cambae/backend/m02/api/       # (bổ sung) KhaiBaoThongKeApi
frontend/src/tinh-nang/khai-bao-tam-tru/
```

**Structure Decision**: Theo R-02 của 000 và 004: 018 thêm gói `m02.khaibao`.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Bảng đệm `nghia_vu_khai_bao` | Nhắc người chưa được lập khai báo đúng ngày, chạy bù, báo Quản lý viện một lần (FR-002) | Tính lại từ lịch sử 001, 004 mỗi lần chạy: phải tái dựng mốc từ nhiều module, khó chống báo trùng |

## Phase 1 hoàn thành (T001, 2026-10-27)

- **Nhánh**: `impl/018-residence-registration` tạo từ `impl/004-admission-residency` (`0c7171c`) vì 018 cần sự kiện, dịch vụ hợp đồng của 004 chưa vào develop. Bản dịch hợp đồng, glossary mục 24 commit ở `docs/018-contract` (`f826030`, từ develop) và cherry-pick sang nhánh này (`8c1bfba`); PR hợp đồng vẫn chờ chủ dự án duyệt (team-plan mục 3, luật 7).
- **Gói** (theo R-02 của 000 và cách 004 chia M02, glossary 24.5): `khaibao/internal` → `m02.internal.registration` (`package-info`; tầng `service`, `domain`, `persistence`, `config` tạo khi có lớp đầu tiên); `khaibao/web` → `m02.web`, `khaibao/lich` → `m02.scheduling`, dùng chung với 004, không tạo gói con riêng.
- **Lệch so với tasks**: luật "gói lịch không tạo khai báo" không viết bằng `noClasses().that().resideInAPackage("..scheduling..")` vì job M02 chỉ ủy cho lớp chạy trong `internal` (`M02DailyJob` → `M02DailyRunner`), luật như vậy luôn qua. `ResidenceRegistrationArchTest` đi theo chuỗi gọi, tham chiếu phương thức, lambda và bản cài interface từ mọi lớp của `m02.scheduling`; không được tới phương thức **lập khai báo** (T001, Z-06, Q-306: khai báo, khai báo gia hạn do Hành chính lập). Phạm vi chỉ là lập khai báo; chủ dự án không duyệt bản đầu cấm mọi `@BusinessCommand` (2026-10-27).
- **Quy ước tên đi kèm luật** (Phase 2 → 6 phải theo, glossary 24.5): trong `m02.internal.registration`, chỉ phương thức lập khai báo, lập khai báo gia hạn bắt đầu bằng `create` (luật bắt cả khi không có `@BusinessCommand`, nên bỏ annotation không làm luật qua); bảng khai báo chỉ ghi qua `ResidenceRegistrationStore.insert…` (luật bắt cả job ghi thẳng bảng, bỏ qua service). Việc của job (nhắc, chuyển Hết hạn, ghi nghĩa vụ, khóa nhắc) dùng tên khác. Giới hạn: luật dựa vào tên; đặt lớp ghi bảng khai báo tên khác thì luật không thấy, người xem lại kiểm ở T002, T008.
- **Test**: Bản đầu RED khi hàm duyệt còn trả rỗng rồi GREEN (14 test). Sửa phạm vi: RED với điều kiện cũ (`OtherCommandJob` gọi lệnh ghi kết quả bị chặn sai; `StoreBypassJob` ghi thẳng `ResidenceRegistrationStore` lọt), GREEN sau khi đổi điều kiện. Lớp mẫu `archfixture.m02registration`: `ViolatingJob` (lớp chạy + lambda → `create`), `StoreBypassJob` (lớp chạy + tham chiếu phương thức → `insert`) bị bắt; `CompliantJob` (Hết hạn, khóa nhắc), `OtherCommandJob` không bị bắt. Đã chạy `ResidenceRegistrationArchTest` (3), `ModuleBoundaryTest`, `LayerArchTest`, `ApiNamedInterfaceArchTest`, `SchedulerScopeArchTest`, `TimeArchTest`: 15 test, 0 lỗi; Spotless qua. Chưa chạy `clean verify` toàn kho (Phase 1 không đổi mã chạy, chỉ thêm `package-info`).

## Phase 2 hoàn thành (T002, T003, T005; T004 chờ A, 2026-10-27)

- **T002** `V018_01__residence_registration.sql` (sau `V004_11`; không sửa migration đã áp): `registration_resident`, `registration_obligation`, `residence_registration`, `registration_submission`, `registration_reminder` (glossary 24.5). DBR-33 bằng `in_progress_key`, `confirmed_key` sinh `STORED` `UNIQUE` (cùng mẫu `residency_ending` của 004, chạy trên MySQL 8.4). Thêm so với data-model, để giữ bảng chuyển FR-005 ở tầng dữ liệu: trigger `trg_residence_registration_transition` (chỉ cho các bước của data-model mục 3; trạng thái Đã xóa, Được thay thế, Đã hủy không đổi được; loại, cờ thường trú cùng xã/phường chỉ đổi khi Cần khai báo; ngày hết hạn, khai báo xóa đặt một lần), `CHECK` Đăng ký tạm trú sau chấp nhận phải có ngày hết hạn, `CHECK` Đã xóa phải có thời điểm, người ghi, bằng chứng (FR-008). Lần nộp nhóm 3: trigger chỉ cho đặt kết quả một lần từ `PENDING`, từ chối phải có lý do; chỉ `eldercare_retention` xóa được khai báo, lần nộp. Khóa ngoại: lần nộp → khai báo, khai báo → nghĩa vụ, khai báo mới → khai báo cũ nó thay; `current_submission_id`, `registration_obligation.registration_id` là tham chiếu logic để tránh khóa ngoại vòng (xóa theo hạn lưu giữ). Không backfill.
- **T003** `ExpectedStayDuration` (`TongThoiGianLuuTru`), `RegistrationTypeRule` (`LoaiKhaiBao`) ở `m02.internal.registration.service`, hàm thuần, không đọc đồng hồ. **Đếm cả ngày đầu và ngày cuối** (01/10 → 30/10 là 30 ngày, đủ CFG-M02-11): spec, BR-M02-11 không nói, chủ dự án chốt 2026-10-27. Bên gọi truyền hợp đồng của hồ sơ hiện hành (Q-12) với ngày kết thúc dự kiến đã gồm phụ lục gia hạn, và ngày hiệu lực phần nội trú (FR-001a, Q-336) nếu có. Hợp đồng chồng nhau bị từ chối (DBR-06 của 004 không cho phép; không tự hợp nhất).
- **T004** `m02.scheduling.ResidenceRegistrationJob` (06:00 Asia/Ho_Chi_Minh, `@SchedulerLock` `M02_REGISTRATION`, chạy bù lúc khởi động, `eldercare.jobs.m02-registration.*`) → `RegistrationDailyRunner` → các bean `RegistrationDailyTask` (Phase 3 → 6 thêm). Không sửa `M02DailyJob`/`M02DailyRunner`; dùng chung bảng `daily_run` của M02 (chỉ ghi dòng, tên việc của 018). Khác 004: chỉ xử lý ngày mới nhất chưa chạy, không chạy lại từng ngày lỡ, vì mọi việc 018 xét trạng thái tại ngày chạy (SC-002 "lần chạy đầu tiên sau đó"); tránh dồn nhắc của nhiều ngày cũ. Khóa nhắc `RegistrationReminderStore.claim` (`registration_reminder`, transaction bắt buộc). **Blocker A**: `SchedulerScopeArchTest.FR_010a_everyJobIsAnAllowedKind` đỏ, chỉ vì `ResidenceRegistrationJob` chưa có trong `ALLOWED` (đã kiểm: một vi phạm duy nhất); B không sửa file của A. Cùng lúc cần A: `M02_REGISTRATION` vào `runJobs` api-000 và `TestClockController.TestJob` (`ScheduledJob.name()` phải trùng), và `m02-registration: cron: "-", on-startup: false` ở profile `test` của `application.yml` (B chưa sửa; `RegistrationCatchUpIT` tự tắt bằng `@TestPropertySource`). Phase 2 chưa có việc nào nên job lúc khởi động là không làm gì; **trước khi Phase 3 thêm `RegistrationDailyTask` phải có dòng profile test**, nếu không mọi context IT chạy việc thật lúc khởi động.
- **T005** `RegistrationFieldClassificationConfig` (map rỗng: spec, data-model không xếp trường nào vào nhóm hạn chế; đề nghị chủ dự án xác nhận `basisNote` có thể chứa địa chỉ thường trú có cần nhóm `IDENTITY` không), `RegistrationRetention` (theo hồ sơ, CFG-M15-03; lần nộp xóa cùng khai báo; `referencesTo` trả khai báo mới thay khai báo cũ; người hết khai báo thì xóa luôn nghĩa vụ, khóa nhắc, cờ thường trú), `RegistrationEvidenceFilePermission` (`Function.RESIDENCE_REGISTRATION` `VIEW` theo ma trận V002_04: AC-01, AC-09; khai báo không còn thì không tải), `RegistrationScopeAssignment`; `ResidenceRegistrationStore` mới có `residentOf` (lệnh `insert…` thêm ở T008). Giới hạn đã biết: nghĩa vụ của người chưa từng được lập khai báo không là ứng viên lưu giữ (ứng viên tính theo khai báo); xử lý khi có quyết định về loại dữ liệu kỹ thuật.
- **Luật T001 với mã thật**: `ResidenceRegistrationArchTest` thêm lớp mẫu `TaskInterfaceJob` (lớp chạy gọi việc qua interface như `RegistrationDailyRunner` → `RegistrationDailyTask`): bản cài lập khai báo bị bắt, bản cài chuyển Hết hạn không. Mã thật chưa có `create…`, `insert…` nên luật qua; tên lớp, phương thức Phase 2 (`claim`, `residentOf`, `run`, `catchUp`) không trùng quy ước của luật. Không mở rộng thành cấm mọi `@BusinessCommand`.
- **Test** (MySQL 8.4, `localhost:3307`): RED → GREEN `RegistrationSchemaIT` 13 (RED: thiếu bảng), `ExpectedStayDurationTest` 14 + `RegistrationTypeRuleTest` 8 (RED: chưa có lớp), `RegistrationCatchUpIT` 7 (RED: chưa có lớp), `RegistrationProvidersIT` 5 (RED: chưa có bean); `ResidenceRegistrationArchTest` 4, `ModuleBoundaryTest`, `LayerArchTest`, `TimeArchTest`, `ApiNamedInterfaceArchTest` qua. **`clean verify` toàn kho chưa xanh** (2026-10-27, chạy với `-Dmaven.test.failure.ignore=true` để IT chạy hết sau lỗi đã biết, nên "BUILD SUCCESS" của log không có nghĩa là xanh): đơn vị 327 test, 1 failure `SchedulerScopeArchTest.FR_010a_everyJobIsAnAllowedKind` (chỉ vì `ResidenceRegistrationJob` chưa có trong `ALLOWED` của A, một vi phạm duy nhất); tích hợp 1337 test, 1 error `ScoreAdjustmentIT.FR_011_adjustmentSurvivesDailyRecalculation` (004, `CannotCreateTransactionException`: mất kết nối khi máy ngủ giữa lần chạy). Chạy lại riêng `ScoreAdjustmentIT` (failsafe, không bỏ qua lỗi): 9/9 xanh. Mọi test của 018 xanh. Cần chạy lại `clean verify` sau khi A thêm `ALLOWED`.
- **BL-87 `KhaiBaoThongKeApi`** (T015) giữ mở: module-interfaces chỉ ghi `tongHop()` và bốn số, không có kiểu trả về; spec 016 FR-020 còn đếm theo khai báo (trước Q-306), 018 đếm theo người, gồm người chưa được lập. Không khai báo interface, bản giả. Câu hỏi cho A ở team-plan mục 8. Không chặn Phase 2 → 6.

## Phase 3 hoàn thành (T006 `[x]`; T007, T008 còn `[ ]`, 2026-10-27)

- **T006** `BR_M02_11_ResidenceRegistrationIT` (14) và `DBR_33_OneRegistrationIT` (3), nền `RegistrationTestBase` (kế thừa `M02TestBase`; hợp đồng dựng bằng SQL với ngày kết thúc tùy ý; Hoàn tất tiếp nhận thật qua 001, 004; tắt job khai báo bằng `@TestPropertySource` như `RegistrationCatchUpIT`). Phủ: Q1 (nghĩa vụ, hạn = mốc + CFG-M02-12, nhắc lập một lần, không tự tạo khai báo, danh sách cần làm `TO_CREATE`; lập muộn vẫn giữ hạn), Q2 (14 ngày → Thông báo lưu trú; 30 ngày tính cả hai đầu → Đăng ký tạm trú), Q3 (bán trú: không nghĩa vụ, không nhắc, lập bị chặn `NOT_RESIDENTIAL_CARE`), Q4 (quá hạn: nhắc Hành chính mỗi ngày, chạy lại cùng ngày không nhắc trùng, Quản lý viện một lần; đã lập mà chưa ghi nộp vẫn nhắc), Q5 (Hoàn tất tiếp nhận không bị chặn), Q6 (xác nhận thường trú cùng xã/phường: loại đổi, hạn giữ, cờ hồ sơ; thiếu ghi chú căn cứ bị từ chối), FR-001a (hợp đồng nội trú mới của người bán trú: mốc từ ngày bắt đầu, kể cả ở tương lai; người đã nội trú hoặc chưa Hoàn tất tiếp nhận thì không), sự kiện giao lại không ghi trùng, FR-009 (Quản lý viện xem, không lập; Điều dưỡng không xem), thiếu nghĩa vụ `NO_REGISTRATION_OBLIGATION`; DBR-33: lập lần hai `REGISTRATION_IN_PROGRESS_EXISTS`, hai lệnh đồng thời chỉ một thành công, `UNIQUE` của cơ sở dữ liệu giữ khi bỏ qua service. Phần Q5 "Kết thúc lưu trú khi khai báo chưa xong" thuộc T013, phần Q6 "gia hạn sau vẫn Thông báo lưu trú" thuộc T011.
- **T007** (còn `[ ]`): `RegistrationObligationListeners` nghe `AdmissionCompleted` (nội trú) và `ContractStatusChanged` (sang Hiệu lực, hợp đồng nội trú, hợp đồng đã ký liền trước là bán trú, người đang lưu trú) bằng `@ApplicationModuleListener` (sau commit, transaction riêng, giao lại khi khởi động nhờ bảng sự kiện của Spring Modulith); `RegistrationObligations.record` ghi mốc qua `RegistrationObligationStore.record` (khóa `UNIQUE (resident_id, trigger_type, source_id)`, trùng thì bỏ qua), hạn đọc CFG-M02-12 lúc ghi, mốc mới thì giành khóa nhắc `CREATE` rồi phát `ResidenceRegistrationReminder(TO_CREATE)`. Nhắc quá hạn `RegistrationOverdueReminders.remind(day)`: nghĩa vụ chưa ghi nộp, hạn trước 00:00 ngày `day`; Hành chính qua khóa `OVERDUE`, Quản lý viện qua `manager_notified_at` (một lần mỗi nghĩa vụ). **Chưa gắn vào job**: lớp chưa cài `RegistrationDailyTask` vì job chạy bù lúc khởi động trong mọi context test tới khi A thêm dòng profile test (BL-90); khi A xong chỉ cần `implements RegistrationDailyTask` (tên việc `REG_OVERDUE`, `run(day)` gọi `remind(day)`) và test qua `RegistrationDailyRunner`. Không sửa `SchedulerScopeArchTest`, api-000, `TestClockController`, `application.yml`.
- **Thông báo** `m02.api.events.ResidenceRegistrationReminder` (mới, cài `HasNotificationRequests`, mức Nhẹ, nội dung chung; `TO_CREATE`, `OVERDUE` gửi Hành chính, `OVERDUE_MANAGER` gửi Quản lý viện; khóa sự kiện theo ngày). Nguồn `M02.REGISTRATION_REMINDER` (không dùng `M02.RESIDENCE_REGISTRATION` vì mã là người cao tuổi, không phải khai báo).
- **Mốc Hoàn tất tiếp nhận**: `AdmissionCompleted` của 004 không mang thời điểm nên mốc lấy lúc listener chạy (ngay sau commit); chỉ lệch khi sự kiện được giao lại sau khởi động lại (mốc muộn hơn nên hạn và nhắc cũng muộn theo). Giao lại không ghi trùng nhờ khóa `(resident_id, trigger_type, source_id)` với nguồn là mã hợp đồng; lần ghi đầu thành công được giữ. Mốc FR-001a lấy ngày bắt đầu hợp đồng nên không lệch, nhưng điều kiện (người đang lưu trú, hợp đồng liền trước là bán trú) đọc lúc listener chạy, không phải lúc phát sự kiện. Không đổi sự kiện của 004 ở nhánh này; nếu cần chính xác tuyệt đối, thêm `at` vào `AdmissionCompleted` (B, cùng M02) ở PR sau.
- **T008** (còn `[ ]` vì màn web): `ResidenceRegistrationService.createRegistration` (`@BusinessCommand(requiresReason = false)`: hợp đồng không có lý do; quyền `RESIDENCE_REGISTRATION` `CREATE` = Hành chính): khóa `registration_resident` (`INSERT … ON DUPLICATE KEY UPDATE` rồi `SELECT … FOR UPDATE`), đọc khóa khai báo đang xử lý và nghĩa vụ mở (`FOR UPDATE`, không theo ảnh chụp của transaction), loại qua `ExpectedStayDuration` (hợp đồng đã ký của hồ sơ, ngày kết thúc hiện hành; từ ngày hiệu lực phần nội trú với mốc FR-001a) + `RegistrationTypeRule` (CFG-M02-11 đọc lúc lập, cờ thường trú cùng xã/phường), ghi qua `ResidenceRegistrationStore.insertToSubmit`, hạn = hạn nghĩa vụ sớm nhất, mọi nghĩa vụ mở được gắn khai báo; `DuplicateKeyException` của DBR-33 thành 409. Không có nghĩa vụ: `NOT_RESIDENTIAL_CARE` nếu hợp đồng đã ký bao hôm nay (hoặc mới nhất) là bán trú, ngược lại `NO_REGISTRATION_OBLIGATION`. `confirmLocalPermanentResident` (quyền `UPDATE`): ghi chú căn cứ bắt buộc, chỉ khi Cần khai báo (`STATE_CHANGED`), ghi cờ ở `registration_resident`. Nhật ký `M02_REGISTRATION_CREATED`, `M02_REGISTRATION_LOCAL_CONFIRMED`. `toDo`: `OVERDUE` (hạn trước hôm nay, chưa ghi nộp) hoặc `TO_CREATE` (chưa lập), kèm họ tên từ `ResidentApi.identity`.
- **Controller** `ResidenceRegistrationController` theo api-018 đã dịch: `GET /residence-registrations/to-do`, `GET`/`POST /residents/{residentId}/residence-registrations`, `POST /residence-registrations/{id}/{command}` (enum đủ năm lệnh; Phase 3 cài `confirm-local-permanent-resident`, lệnh khác trả 422 `INVALID_COMMAND` tới T010, T014; `renewal: true` trả 422 `INVALID_VALUE` tới T012). Hai mã 422 này là **tạm, ngoài hợp đồng** (api-018 không khai 422 cho hai operation, controller cũng không khai trong OpenAPI nên `ApiContractIT` không thấy); không test nào khẳng định chúng; phải bỏ khi cài T010, T012, T014. Thân `RegistrationCommand` đủ tám trường; enum `taskType` đủ năm giá trị (Phase 3 trả hai). **File chung**: api-018 đăng ký vào `ApiContractIT` (`pending` rỗng, so operation bật), cùng mẫu api-004; chờ A xem (team-plan mục 8). Màn web chưa làm: DESIGN.md chưa có bố cục màn khai báo được duyệt.
- **Luật T001 với mã thật**: `createRegistration`, `insertToSubmit` nay có trong mã; `ResidenceRegistrationArchTest` (4) qua, Bộ lập lịch không tới được hai phương thức này. Lớp ghi nghĩa vụ dùng tên `record` (không phải `insert…`), việc nhắc dùng `remind`, `claim`.
- **Test** (MySQL 8.4 `localhost:3307`, `DB_URL`, `TEST_DB_URL` ghi đè trong cùng lệnh): RED: `test-compile` lỗi vì chưa có lớp; lần chạy đầu sau khi cài: 7 error (`@BusinessCommand` mặc định bắt lý do) + 3 failure → sửa `requiresReason = false` → GREEN 17/17. `ApiContractIT` RED (đường dẫn 018 không có trong hợp đồng nào) → GREEN sau khi controller theo đúng đường dẫn `{command}` và đăng ký api-018. Các test liên quan qua: `RegistrationSchemaIT` 13, `RegistrationProvidersIT` 5, `RegistrationCatchUpIT` 7, `AdmissionCompletedEventIT` 6, `ContractLifecycleIT` 19, `Events017018IT` 3, `FR_004_AdmissionWithRealContractIT` 3, `M02StatusParticipantIT` 1, `BackendApplicationTests` 1. **Toàn kho** (`clean verify '-Dtest=!SchedulerScopeArchTest'`, 3307, không bỏ qua lỗi; `-Dtest` làm surefire chạy cả `*IT`): 1679 test, 1 failure `ResidentContractIT` (001, của B: hai đường dẫn `/residents/{residentId}/residence-registrations` của 018 bị coi là của 001) → loại đúng hai operation đó (so bằng chuỗi đầy đủ, không lọc theo đoạn đường dẫn) như đã loại endpoint qua đời của 004, chạy lại `ResidentContractIT` + `ApiContractIT` xanh. Không chạy lại toàn kho lần hai (BL-90 chưa xong nên `SchedulerScopeArchTest` vẫn đỏ đúng một vi phạm). **Chưa thể tuyên bố `clean verify` xanh** tới khi A xong BL-90.
- **Sự cố môi trường (2026-10-27)**: một lần `./mvnw -o test` chạy thiếu ghi đè biến môi trường; `BackendApplicationTests` (surefire, nạp context) nối 3306 và `MySqlIntegrationTestBase` đã xóa, tạo lại schema `eldercare_test` và hai tài khoản `eldercare_app`, `eldercare_retention` (mật khẩu ngẫu nhiên) trên 3306. Schema `elder_care` không bị xóa nhưng hai tài khoản mất quyền và mật khẩu cũ. Đã khôi phục cùng ngày (B duyệt, BL-92): `ALTER USER` hai tài khoản `@'%'` theo `DB_PASSWORD`, `DB_RETENTION_PASSWORD` và 27 `GRANT` chép đúng từ 16 migration đã áp dụng (000.00 → 002.05.1), chạy bằng `eldercare_dev_admin`, không chạy lại Flyway; `SHOW GRANTS` của từng tài khoản khớp. `eldercare_test` trên 3306 giữ nguyên làm bằng chứng (3 bảng, 000.02 lỗi), còn quyền thừa của hai tài khoản trên schema đó. `TEST_DB_URL` của tài khoản Windows đổi sang 3307; fail-fast cho `MySqlIntegrationTestBase` đề xuất A (BL-93). Từ nay mọi lệnh mvnw `test`/`verify` vẫn ghi đè `DB_URL`, `TEST_DB_URL` trong cùng lệnh.

## PHASE 4 HANDOFF (US2, T009 → T010)

- **Điều kiện**: BL-90 không chặn US2 (ghi nộp, kết quả không chạy trong job). Nhánh vẫn dựa trên 004 chưa merge (BL-89); PR code 018 để base là 004 tới khi 004 vào develop.
- **T009** `RegistrationSubmissionResultIT` (`KhaiBaoNopKetQuaIT`, Q7 → Q10) dùng `RegistrationTestBase`; lập khai báo qua `createRegistration` như Phase 3.
- **T010**: thêm nhánh `record-submitted`, `record-accepted`, `record-rejected` vào `ResidenceRegistrationController.command` (đã có thân `RegistrationCommandBody` đủ trường) và lệnh tương ứng trong `ResidenceRegistrationService` (tên không bắt đầu `create`). Mỗi lệnh khóa `registration_resident` trước. Ghi nộp: dòng `registration_submission` (`PENDING`), `current_submission_id`, trạng thái `SUBMITTED`, đặt `registration_obligation.submitted = TRUE` (dừng nhắc quá hạn). Chấp nhận: bằng chứng bắt buộc (`EVIDENCE_REQUIRED`), Đăng ký tạm trú cần `expiryDate` (`EXPIRY_DATE_REQUIRED`); khai báo Đã xác nhận cũ → `SUPERSEDED` trước rồi mới `CONFIRMED` trong cùng transaction (khóa `confirmed_key`). Từ chối: lý do bắt buộc; về `TO_SUBMIT`, hạn mới = thời điểm ghi + CFG-M02-12. **Cần quyết ở T010**: để nhắc quá hạn chạy lại sau từ chối, đặt lại `submitted = FALSE` và `due_at` của nghĩa vụ theo hạn mới (bảng là đệm dựng lại được), hoặc cho `RegistrationOverdueReminders` xét thêm hạn của khai báo; đề xuất cách thứ nhất. Đính chính `M02.RESIDENCE_REGISTRATION` (`CorrectableRecordProvider`, không gắn tầng), gồm gỡ xác nhận thường trú cùng xã/phường khi còn Cần khai báo (FR-001). Ghi kết quả khi chưa ghi nộp: `NOT_SUBMITTED`.
- **Còn treo từ Phase 3**: A xem đăng ký api-018 trong `ApiContractIT` (BL-91); A quyết fail-fast của `MySqlIntegrationTestBase` (BL-93); dọn quyền thừa của `eldercare_app`, `eldercare_retention` trên `eldercare_test` ở 3306 (BL-94, chưa làm, cần duyệt).
  - **Backend còn thiếu**: T007 gắn `RegistrationOverdueReminders` vào `ResidenceRegistrationJob` (`implements RegistrationDailyTask`) sau BL-90; T004 cùng BL-90; `KhaiBaoThongKeApi` chờ A chốt (BL-87); lệnh `record-*` (T010), gia hạn (T012), khai báo xóa (T014) đang trả 422 tạm.
  - **Frontend còn thiếu**: toàn bộ màn T008 (danh sách cần làm, khai báo của người cao tuổi, lập khai báo, xác nhận thường trú cùng xã/phường); chưa có bố cục duyệt trong DESIGN.md, chưa sinh `src/api/api018.js`.

## Đồng bộ với hợp đồng của feature khác

1. **004** (cùng M02): dùng sự kiện `TiepNhanHoanTat`, `PhuLucDaApDung`, `HopDongDaDoiTrangThai` đã khai cho 018. Không đổi.
2. **001**: cài `ThamGiaChuyenTrangThai` không có điều kiện chặn. **009**: thông báo Nhẹ; **016** đọc `KhaiBaoThongKeApi`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX).

1. **Căn cứ pháp lý chưa xác nhận** (spec, mục cập nhật 2026-10-06; 24.4): viện có thuộc "cơ sở lưu trú khác" theo Luật số 118/2025/QH15 không, và hạn thông báo (CFG-M02-12). Thiết kế để mọi ngưỡng là tham số nên không chặn; cần tư vấn pháp lý xác nhận trước vận hành. *(Còn mở 2026-10-10: chờ tư vấn pháp lý, mục 24.4.)*
2. **Mốc phần nội trú có hiệu lực** (FR-001a): spec nêu "hợp đồng mới hoặc phụ lục đổi loại lưu trú được áp dụng"; plan lấy mốc là ngày hiệu lực của phụ lục hoặc ngày bắt đầu hợp đồng nội trú mới (sự kiện của 004). Nếu ngày hiệu lực ở tương lai, hạn khai báo tính từ ngày đó chứ không từ lúc áp dụng. Đề nghị xác nhận ở FR-001a, FR-002. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
