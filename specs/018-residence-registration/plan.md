# Implementation Plan: Khai báo tạm trú, lưu trú

**Branch**: `018-residence-registration` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/018-residence-registration/spec.md`

## Summary

Feature 018 là phần 6.10 của module M02 (UC-80, UC-81; BR-M02-11 → 14; DBR-33). Hành chính lập khai báo khi được nhắc, ghi nộp và kết quả; hệ thống xác định loại khai báo theo thời gian lưu trú cộng dồn, nhắc quá hạn, nhắc gia hạn, chuyển Hết hạn, nhắc khai báo xóa; không nộp hồ sơ thay, không kết nối Cổng dịch vụ công.

Hướng kỹ thuật:
- Gói `admission.khaibao` cạnh 004 trong M02, đọc hợp đồng qua dịch vụ nội bộ M02 (research Z-01).
- Nghĩa vụ khai báo là số liệu tính, giữ đệm kỹ thuật `nghia_vu_khai_bao` để nhắc đúng ngày, chạy bù (Z-03). *(R-15: `nghia_vu_khai_bao` → `trang_thai_nct`)*
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
| X. Kiến trúc | Đạt | Gói con của `admission`; không có vòng |
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
| NFR-02 Hiệu năng | Có | Mỗi lệnh ghi một khai báo dưới khóa `khai_bao_nct`; ghi nộp, kết quả ≤ 2 phút thao tác (SC-005) *(R-15: `khai_bao_nct` → `trang_thai_nct`)* |
| NFR-03 Thông báo khẩn cấp | Không | Thông báo mức Nhẹ (FR-010) |
| NFR-04 Tác vụ định kỳ | Có | Job hằng ngày cho 300 hồ sơ ≤ 1 phút; `nhac_khai_bao` `UNIQUE (loai, nct_id, ngay)` chống nhắc trùng; chạy bù (research Z-06) *(R-15: `nhac_khai_bao` → `moc_da_nhac`)* |
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
backend/src/main/java/com/eldercare/backend/admission/khaibao/   {internal, web, lich}   # nghĩa vụ, khai báo, TongThoiGianLuuTru, LoaiKhaiBao, bean trạng thái
backend/src/main/java/com/eldercare/backend/admission/api/       # (bổ sung) KhaiBaoThongKeApi
frontend/src/tinh-nang/khai-bao-tam-tru/
```

**Structure Decision**: Theo R-02 của 000 và 004: 018 thêm gói `admission.khaibao`.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Bảng đệm `nghia_vu_khai_bao` | Nhắc người chưa được lập khai báo đúng ngày, chạy bù, báo Quản lý viện một lần (FR-002) | Tính lại từ lịch sử 001, 004 mỗi lần chạy: phải tái dựng mốc từ nhiều module, khó chống báo trùng *(R-15: `nghia_vu_khai_bao` → `trang_thai_nct`)* |

## Đồng bộ với hợp đồng của feature khác

1. **004** (cùng M02): dùng sự kiện `TiepNhanHoanTat`, `PhuLucDaApDung`, `HopDongDaDoiTrangThai` đã khai cho 018. Không đổi.
2. **001**: cài `ThamGiaChuyenTrangThai` không có điều kiện chặn. **009**: thông báo Nhẹ; **016** đọc `KhaiBaoThongKeApi`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX).

1. **Căn cứ pháp lý chưa xác nhận** (spec, mục cập nhật 2026-10-06; 24.4): viện có thuộc "cơ sở lưu trú khác" theo Luật số 118/2025/QH15 không, và hạn thông báo (CFG-M02-12). Thiết kế để mọi ngưỡng là tham số nên không chặn; cần tư vấn pháp lý xác nhận trước vận hành. *(Còn mở 2026-10-10: chờ tư vấn pháp lý, mục 24.4.)*
2. **Mốc phần nội trú có hiệu lực** (FR-001a): spec nêu "hợp đồng mới hoặc phụ lục đổi loại lưu trú được áp dụng"; plan lấy mốc là ngày hiệu lực của phụ lục hoặc ngày bắt đầu hợp đồng nội trú mới (sự kiện của 004). Nếu ngày hiệu lực ở tương lai, hạn khai báo tính từ ngày đó chứ không từ lúc áp dụng. Đề nghị xác nhận ở FR-001a, FR-002. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Ánh xạ bảng ở data-model mục 0; task T018. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.

## Tích hợp R-15 *(2026-11-04, port lên develop bb2c98ee, nhánh `impl/018-r15`)*

Port Phase 2 → 7 của nhánh cũ `impl/018-residence-registration` (gói `com.cambae.backend.m02`, bảng trước R-15, dựng trên 004 cũ; chỉ đọc, không merge) sang `develop` theo R-15 (data-model mục 0, 0.1) và 004 đã công bố. Không đổi nghiệp vụ, API, quyền.

- **Không có migration `V018_*`**: không bảng riêng; mọi dữ liệu 018 ở bảng chung (glossary 24.6). Ràng buộc DBR-33 bằng `unique_key` của `simple_record`; khai báo, nghĩa vụ, cờ thường trú ở `simple_record`, lần nộp ở `record_event`, dấu nhắc ở `reminder_marker`, khóa theo người bằng `TechnicalLocks`. Bảng chuyển trạng thái FR-005 giữ ở `ResidenceRegistrationStore.transition` (trigger riêng không còn); dòng trạng thái cuối khóa (`locked_at`), chỉ job loại bỏ xóa.
- **Lệch data-model 0.1**: `nghia_vu_khai_bao` ghi ở `simple_record` (`M02.REGISTRATION_OBLIGATION`) thay vì `trang_thai_nct`, vì dòng `resident_module_state` của module M02 (khóa `(resident_id, module)`) đã là `ResidencyState` của 004, mỗi người một dòng. Lần nộp có cha `M01.RESIDENT` (không phải khai báo) để job loại bỏ theo hồ sơ không để dòng mồ côi; `RegistrationRetention` xóa lần nộp theo `content.registrationId`.
- **Job (thay BL-90)**: bốn việc `REG_EXPIRY`, `REG_RENEWAL`, `REG_OVERDUE`, `REG_DEREGISTER` là `M02DailyTask` (`latestDayOnly`, `@Order` 10, 20, 30, 40) của job `M02_DAILY` 00:15 do 004 đã có, nên không còn `ResidenceRegistrationJob`, `RegistrationDailyRunner`, `daily_run` riêng; không cần sửa `SchedulerScopeArchTest`, `runJobs`, profile test. Giờ chạy đổi từ 06:00 sang 00:15 (cùng khung chạy bù BR-M02-19; việc xét trạng thái tại ngày chạy nên nhắc vẫn đúng ngày).
- **Nối 004 thật (thay bản giả BL-98)**: listener `@ApplicationModuleListener` nghe `AdmissionCompleted`, `ContractStatusChanged` (sang Hiệu lực, hợp đồng nội trú liền sau bán trú) và `AddendumApplied` (`RESIDENCE_TYPE`: bán trú → nội trú, mốc là ngày hiệu lực phụ lục, Q-289, Q-336; `DURATION`: gia hạn khi chỉ có Thông báo lưu trú); đọc hợp đồng qua `ContractReadApi` (`contract`, `ofResident`, `contentAtDate`). Không dùng `ResidenceTypeChanged` (chỉ mang mã người, 003 nghe) và `ResidencyStatisticsApi` (số liệu cho 016 của 004).
- **Giữ nguyên các quyết định đã chốt**: tổng thời gian lưu trú đếm cả ngày đầu và cuối; hạn khai báo gia hạn = hết ngày hết hạn; gia hạn sau trạng thái cuối → 409 `STATE_CHANGED` (BL-97 A); từ chối đặt lại Cần khai báo, hạn mới ở `due_at`, hạn gốc nghĩa vụ giữ.
- **Số liệu cho 016 (T015, BL-87)**: `ResidenceRegistrationStatisticsApi.summary()`, đếm theo người (xem team-plan).
- **Lớp, test đã port** (65 file ở nhánh cũ): toàn bộ `registration/{domain,persistence,service,config}`, `ResidenceRegistrationController`, `ResidenceRegistrationReminder`, request/response; test `BR_M02_11_ResidenceRegistrationIT`, `BR_M02_12_RenewalIT`, `BR_M02_13_DeregistrationIT`, `BR_M02_14_TypeChangeIT`, `DBR_33_OneRegistrationIT`, `RegistrationProvidersIT`, `RegistrationSubmissionResultIT`, `RegistrationCatchUpIT` (viết lại), `RegistrationStoreIT` (thay `RegistrationSchemaIT`), `ResidenceRegistrationArchTest`, `ExpectedStayDurationTest`, `RegistrationTypeRuleTest`, `RegistrationTestBase`, `RegistrationSql`. Mới: `RegistrationStatisticsIT`, `RegistrationStatistics`, `FakeResidenceRegistrationStatistics`.
- **A trả lời 2026-11-05, đã áp (2026-11-06)**: BL-96 duyệt api-018 đã dịch và bố cục K1–K7, làm màn web `frontend/src/features/residence-registration/` (menu "Khai báo cư trú", Hết hạn tông `urgent`); hợp đồng thêm `ResidenceRegistration.basisNote`, `allowedCommands`, `submissions[]`, `RegistrationTask.allowedCommands` (lệnh theo trạng thái, chưa xét quyền; màn ẩn thêm theo quyền sửa). BL-95: thêm 422 `INVALID_VALUE`, `INVALID_COMMAND` vào api-018 và `@ApiResponse` ở controller. BL-97 B: MỘT thủ tục khai báo xóa theo người: `toDo` trả một việc `TO_DEREGISTER` mỗi người (mã khai báo sớm nhất), `record-deregistered` đổi mọi khai báo Đã xác nhận, Hết hạn của người sang Đã xóa với một bằng chứng, mỗi khai báo một dòng nhật ký; nhắc theo người như cũ. BL-87: bỏ sự kiện `KhaiBaoDaDoiTrangThai` (mã không có), 016 dùng `ResidenceRegistrationStatisticsApi.summary()` đếm theo người.
- **Kiểm cuối (T016, 2026-11-06)**: `clean verify -Dit.forkCount=3` toàn kho 2692 test, chỉ `StaffCommandsHttpIT` (008, đỏ sẵn); `npm test` 512 xanh. Kịch bản quickstart mục 3 (Q1–Q20) đối chiếu với test ở quickstart mục 6; riêng giao diện (K1–K7) kiểm bằng Vitest, chưa duyệt bằng mắt trên profile demo.
- **Còn chờ**: A xem BL-91, BL-87 (team-plan mục 8); hỏi A thêm `evidenceFileId` vào `ResidenceRegistration` để màn tải bằng chứng.
- **Rà soát theo luồng nghiệp vụ (2026-11-06)**: báo cáo `ra-soat-018.md` (ngoài kho), task T019. Đã sửa: nút "Lập khai báo gia hạn" chỉ hiện khi còn Đăng ký tạm trú Đã xác nhận; khai báo gia hạn chưa nộp vào danh sách cần làm (`EXPIRING_SOON`, quá hạn thì `OVERDUE`), nhắc quá hạn (Hành chính mỗi ngày, Quản lý viện một lần qua `ReminderType.OVERDUE_MANAGER`) và `summary()`; hợp đồng nội trú mới nối tiếp hợp đồng nội trú là mốc `RENEWAL_ADDENDUM` cho người chỉ có Thông báo lưu trú; hợp đồng Chấm dứt sớm cắt theo hợp đồng sau trong `ResidenceRegistrationService.periods`; không báo Quản lý viện Hết hạn của người đã ở trạng thái cuối. Các quyết định này chờ chủ dự án xác nhận (spec mục "Cập nhật 2026-11-06"). Mục Technical Context, Project Structure ở trên là thiết kế ban đầu; tên gói, bảng, job thật theo mục "Tích hợp R-15" này. Còn chờ: `evidenceFileId` (A), tư vấn pháp lý khai báo xóa cho Thông báo lưu trú.
- **Sau PR #119 (2026-11-08, nhánh `impl/018-followup`)**: (1) "Đổi api" A duyệt 2026-11-07: `ResidenceRegistration.evidenceFileId` (tệp bằng chứng của kết quả được chấp nhận; trước đó mã chỉ giữ trong nội dung khai báo, hợp đồng không trả) vào api-018, `ResidenceRegistrationBody`, test `Q7_accepted_exposesEvidenceFileId_onTheRegistration` (đỏ rồi xanh), `ApiContractIT` xanh; màn web `RegistrationDetail` hiện nút "Xem bằng chứng" (`openFile` của `src/common/files.js`, bố cục K3) khi có `evidenceFileId`, 2 test Vitest mới. Client `api018.js` không đổi (chỉ thêm trường schema). (2) Sửa `RegistrationSubmissionResultIT.Q7_recordingSubmitted_…`: đỏ khi schema test không có sẵn dấu nhắc `CREATE` của (người, ngày) vì lệnh hoàn tất tiếp nhận ghi nghĩa vụ và phát `TO_CREATE` lần đầu trong ngày (`RegistrationObligations.record` → `claim`), nên `reminders(residentId)` không rỗng; dấu nằm trong schema test giữ qua các lần chạy nên có khi xanh (máy A, id và ngày đã có dấu), có khi đỏ (schema mới, `residentId=1`). Không phải lỗi giờ chạy hay mã: assertion "đã ghi nộp thì dừng nhắc quá hạn" nay chỉ xét `OVERDUE`, `OVERDUE_MANAGER` (`overdueReminders`). (3) BL-87: `ResidenceRegistrationStatisticsApi.summary()` và bản giả `FakeResidenceRegistrationStatistics` đã có; sự kiện `KhaiBaoDaDoiTrangThai` không có trong mã.
