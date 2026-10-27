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

## PHASE 3 HANDOFF (US1, T006 → T008)

- **Điều kiện trước**: A thêm `ResidenceRegistrationJob` vào `SchedulerScopeArchTest.ALLOWED`, `M02_REGISTRATION` vào `runJobs`/`TestJob`, dòng `m02-registration` profile test (blocker T004). Chưa có thì có thể viết test và mã US1 nhưng `clean verify` vẫn đỏ một test của A.
- **T006** `BR_M02_11_*IT`, `DBR_33_*IT` (Q1 → Q6): dùng `M02TestBase` (`admissionResident`, `readyToAdmit`, `statusService`), đồng hồ giả lập.
- **T007** listener `AdmissionCompleted`, `ContractStatusChanged` ghi `registration_obligation` (hạn = mốc + CFG-M02-12 đọc lúc ghi, `UNIQUE (resident_id, trigger_type, source_id)` cho idempotent); nhắc lập, quá hạn là bean `RegistrationDailyTask` + `RegistrationReminderStore.claim` (`CREATE`, `OVERDUE`), báo Quản lý viện một lần qua `manager_notified_at`; thông báo Nhẹ cài `HasNotificationRequests`. Tên phương thức không bắt đầu `create`.
- **T008** lệnh lập khai báo `create…` trong `m02.internal.registration` (chỉ actor gọi), ghi bằng `ResidenceRegistrationStore.insert…`, khóa `registration_resident` (`SELECT … FOR UPDATE`, tạo dòng nếu chưa có); loại qua `ExpectedStayDuration` (chuỗi hợp đồng từ dịch vụ đọc hợp đồng 004) + `RegistrationTypeRule` (CFG-M02-11 qua `ParameterApi`). Bắt `DuplicateKeyException` của khóa DBR-33 thành 409 `REGISTRATION_IN_PROGRESS_EXISTS`. Controller `m02.web`, màn web.
- **Phụ thuộc thật trên nhánh**: `AdmissionCompleted`, `ContractStatusChanged`, `AddendumApplied` (004), `StatusTransitionParticipant` (001), `CorrectableRecordProvider` (000). Rủi ro: nhánh dựa trên 004 chưa merge; PR code 018 để base là 004 tới khi 004 vào develop.

## Đồng bộ với hợp đồng của feature khác

1. **004** (cùng M02): dùng sự kiện `TiepNhanHoanTat`, `PhuLucDaApDung`, `HopDongDaDoiTrangThai` đã khai cho 018. Không đổi.
2. **001**: cài `ThamGiaChuyenTrangThai` không có điều kiện chặn. **009**: thông báo Nhẹ; **016** đọc `KhaiBaoThongKeApi`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX).

1. **Căn cứ pháp lý chưa xác nhận** (spec, mục cập nhật 2026-10-06; 24.4): viện có thuộc "cơ sở lưu trú khác" theo Luật số 118/2025/QH15 không, và hạn thông báo (CFG-M02-12). Thiết kế để mọi ngưỡng là tham số nên không chặn; cần tư vấn pháp lý xác nhận trước vận hành. *(Còn mở 2026-10-10: chờ tư vấn pháp lý, mục 24.4.)*
2. **Mốc phần nội trú có hiệu lực** (FR-001a): spec nêu "hợp đồng mới hoặc phụ lục đổi loại lưu trú được áp dụng"; plan lấy mốc là ngày hiệu lực của phụ lục hoặc ngày bắt đầu hợp đồng nội trú mới (sự kiện của 004). Nếu ngày hiệu lực ở tương lai, hạn khai báo tính từ ngày đó chứ không từ lúc áp dụng. Đề nghị xác nhận ở FR-001a, FR-002. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
