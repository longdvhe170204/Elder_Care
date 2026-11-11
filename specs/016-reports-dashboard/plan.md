# Implementation Plan: Báo cáo và dashboard

**Branch**: `016-reports-dashboard` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/016-reports-dashboard/spec.md`

## Summary

Feature 016 là module M14 – Báo cáo (mục 18; UC-67). Nó chỉ đọc và cung cấp: dashboard theo vai trò cho số liệu trong ngày và ca hiện tại; báo cáo người cao tuổi, chăm sóc, chất lượng, sức khỏe và thuốc, chi phí phát sinh, nhân sự và ca trực; danh sách chi tiết của mọi con số; xuất file có ghi nhật ký. Mọi số liệu áp phạm vi dữ liệu và giới hạn trường của người xem.

Hướng kỹ thuật:
- Gói `reporting` đọc qua `*ThongKeApi` và API đọc mà các module đã khai; không đọc bảng, không lưu bản sao (research O-01, O-02).
- Chỉ tiêu là định nghĩa cố định trong mã với hàm tính thuần và hàm chi tiết dùng cùng tập bản ghi (O-05).
- Phạm vi theo FR-010, FR-012 dựng từ 002 và lịch sử ca của 008; lọc hai bước (O-03).
- Dashboard tính song song theo khối, làm mới 30 giây bằng ETag (O-06).
- Xuất XLSX; tệp chỉ giao sau khi nhật ký "Xuất báo cáo" đã lưu (O-08).
- Bổ sung `NguoiCaoTuoiThongKeApi` (001), `LuuTruThongKeApi` (004), `GiuongThongKeApi` (003) vào hợp đồng (O-10).

Plan tuân theo FR-010a của 000 (Q-306, Q-307) và FR-001 của spec: 016 không tạo, sửa dữ liệu nghiệp vụ, không gửi thông báo, không tính lại trạng thái của module nguồn.

### Cập nhật 2026-10-18, lượt 3 (spec mục "Cập nhật 2026-10-18, lượt 3": Q-369)

- **Cảnh báo SOS** (FR-041): báo cáo cảnh báo thêm dòng riêng cho cảnh báo có `nguon = SOS` của `CanhBaoThongKeApi.canhBao` (007 FR-081): số lần, thời gian trung bình và lớn nhất từ tạo đến tiếp nhận, theo tầng; vẫn đếm trong tổng theo mức (research O-12). Không thêm bảng.

### Cập nhật 2026-10-22 (spec mục "Cập nhật 2026-10-22": Q-374, Q-375; Q-385, Q-386)

- **Nút hỗ trợ khẩn cấp và thời gian tới nơi** (FR-041): báo cáo cảnh báo thêm dòng riêng cho `nguon = NUT_HO_TRO` như dòng SOS; với cả hai nguồn thêm thời gian trung bình, lớn nhất từ `lan_dau_luc` tới `daToiNoiLuc` (chỉ cảnh báo có mốc) và số cảnh báo `khongGhiMocToiNoi`; cảnh báo nút hỗ trợ không có người cao tuổi xếp theo tầng của cảnh báo, chưa có tầng vào dòng "chưa rõ tầng" (research O-13; 007 `CanhBaoThongKeApi`, cập nhật 2026-10-22). Không thêm bảng.
- **017 thu gọn** (Q-385, Q-386): `SoDuThongKeApi` còn `tinhTrang` (chỉ tiêu `SD.THAP_CON_NO`, không đổi) và `thuChi` thu gọn; bỏ `chotQuy` của bản cũ. 016 không dùng hai hàm sau nên không đổi.

## Technical Context

**Language/Version**: Java 25 (virtual threads cho truy vấn song song); JavaScript (JSX) (000 R-01). Không có màn trên app di động (spec không yêu cầu).

**Primary Dependencies**: Như 000 → 019; fastexcel (đã chọn ở 010) cho XLSX. Biểu đồ trên web dùng thư viện biểu đồ của web client (chọn ở giai đoạn triển khai, không ảnh hưởng hợp đồng).

**Storage**: Không có bảng riêng; nhật ký lần xuất ở bảng nhật ký của 000; tệp ở kho tệp ([data-model.md](data-model.md)).

**Testing**:
- Như 000 R-14.
- Bộ dữ liệu tính tay cho mọi chỉ tiêu (SC-001); bộ 11 tài khoản quyền (SC-004); bộ phạm vi theo thời gian (SC-011).
- Bản giả mọi `*ThongKeApi` để test M14 độc lập; test đầu–cuối với bản thật.
- Đo hiệu năng dashboard 60 người dùng (SC-002) và báo cáo 12 tháng (SC-003).

**Target Platform**: Web (Quản lý viện, Trưởng tầng, Bác sĩ, Điều dưỡng, Hành chính, Kế toán).

**Project Type**: Web service (REST `/api/v1`) và web client.

**Performance Goals**: Dashboard ≤ 3 giây ở 95% lần mở với 60 người dùng; số mới ≤ 1 phút (SC-002). Báo cáo 1 tháng ≤ 10 giây, 12 tháng ≤ 30 giây (SC-003).

**Constraints**: Chỉ đọc (FR-001); không lộ ngoài phạm vi (FR-013); giới hạn trường ở màn hình và file (FR-014); khoảng ≤ CFG-M14-01 (FR-060); không ghi nhật ký khi chỉ xem (FR-062).

**Scale/Scope**: 0 bảng nghiệp vụ; khoảng 60 chỉ tiêu ([contracts/chi-tieu.md](contracts/chi-tieu.md)); 6 đường dẫn, 6 thao tác REST ([contracts/api-016.yaml](contracts/api-016.yaml)); dùng 22 giao diện đọc của 17 feature; bổ sung 3 giao diện thống kê vào hợp đồng 001, 003, 004.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi chỉ tiêu gán FR ở chi-tieu.md; x-fr trên mọi thao tác |
| II. Spec không nêu công nghệ | Đạt | Spec để định dạng file cho thiết kế; plan chọn XLSX |
| III. Không mặc định CRUD | Đạt | Không có ghi ngoài lệnh xuất |
| IV. Trạng thái kiểm tra được | Đạt | Không có vòng đời; chỉ tiêu có định nghĩa và test tính tay |
| V. Không cố định giá trị | Đạt | CFG-M14-01, CFG-M15-07 và CFG hiển thị đọc từ `ThamSoApi` |
| VI. Vai trò, quyền | Đạt | Bảng FR-010 mã hóa; thu hẹp qua 002 FR-025 |
| VII. Điểm chưa rõ | Đạt | Điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-014 ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; hợp đồng 001, 003, 004 được bổ sung (mục "Đồng bộ") |
| X. Kiến trúc | Đạt | M14 chỉ phụ thuộc `*.api`; không đọc bảng của module khác |
| XI. Nghiệp vụ ở backend | Đạt | Tính chỉ tiêu, phạm vi ở backend |
| XII. Dữ liệu | Đạt | Không lưu bản sao; nhật ký lần xuất qua 000 |
| XIII. Tham số | Đạt | Như V |
| XIV. Ngoại tuyến, thông báo | Đạt | Không ghi ngoại tuyến; không gửi thông báo |
| XV. Truy vết, kiểm thử | Đạt | Test theo FR, mỗi chỉ tiêu có test tính tay |

**Kiểm lại sau Phase 1**: Đạt.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 016 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 60 người dùng đồng thời mở dashboard; số liệu 300 người cao tuổi, 12 tháng |
| NFR-02 Hiệu năng | Có | Dashboard ≤ 3 giây ở 95% lần mở, báo cáo 12 tháng ≤ 30 giây (SC-002, SC-003); truy vấn song song qua `*ThongKeApi`; đo bằng test hiệu năng |
| NFR-03 Thông báo khẩn cấp | Không | 016 không gửi thông báo |
| NFR-04 Tác vụ định kỳ | Không | Không có job; số liệu tính khi xem |
| NFR-05 Khả dụng | Gián tiếp | Chỉ đọc; không lưu bản sao nên không có dữ liệu phải đồng bộ lại |
| NFR-06 Sao lưu | Gián tiếp | Không có bảng riêng; nhật ký lần xuất và tệp xuất theo sao lưu của 000 |
| NFR-07 Lưu trữ | Gián tiếp | Không lưu bản sao số liệu; tệp xuất ở kho tệp của 000 |
| NFR-08 Bảo mật | Có | Phạm vi và giới hạn trường kiểm ở backend (FR-013, FR-014) |
| NFR-09 Thời gian | Có | `Clock`; khoảng ngày, "hôm nay" theo Asia/Ho_Chi_Minh (FR-005, FR-006, DBR-25) |
| NFR-10 Thiết bị | Có | Màn trên web; không có màn trên app di động (spec không yêu cầu) |
| NFR-11 Ngôn ngữ | Có | Nhãn tiếng Việt; ngày dd/MM/yyyy, tiền VND trên màn và file XLSX |
| NFR-12 Cấu hình | Có | CFG-M14-01, CFG-M15-07 đọc qua `ThamSoApi` |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập cho "hôm nay" và ngày chưa kết thúc; bản giả mọi `*ThongKeApi` |

## Project Structure

### Documentation (this feature)

```text
specs/016-reports-dashboard/
├── plan.md  ├── research.md (O-01 → O-11)  ├── data-model.md  ├── quickstart.md (Q1 → Q32)
├── contracts/ (api-016.yaml, module-interfaces.md, chi-tieu.md)
└── tasks.md   # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai; giai đoạn thiết kế không tạo (CLAUDE.md).

```text
backend/src/main/java/com/eldercare/backend/reporting/
├── chitieu/     # ChiTieu, hàm tính, hàm chi tiết
├── phamvi/      # PhamViBaoCao, QuyenNhomChiTieu
├── dashboard/   {internal, web}
├── baocao/      {internal, web}
├── chitiet/     {internal, web}
└── xuat/        {internal, web}

frontend/src/tinh-nang/bao-cao/
├── dashboard/  ├── nguoi-cao-tuoi/  ├── cham-soc/  ├── chat-luong/  ├── suc-khoe-thuoc/  ├── chi-phi/  ├── nhan-su-ca/  └── chi-tiet/
```

**Structure Decision**: Theo R-02 của 000: 016 là module M14.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Lọc hai bước theo khoảng thời gian | FR-012 phạm vi của Điều dưỡng theo từng ca, `PhamViLoc` của 002 không mang khoảng | Mở rộng mọi `*ThongKeApi` nhận danh sách khoảng: đổi chữ ký của 17 feature |
| Bộ đệm dashboard 20 giây theo phạm vi | 60 người dùng làm mới mỗi 30 giây | Không đệm: tải lặp lên các module nguồn; đệm chung: lộ dữ liệu giữa phạm vi |

## Đồng bộ với hợp đồng của feature khác

1. **001, 003, 004**: thêm `NguoiCaoTuoiThongKeApi` (001 mục 1.8a), `GiuongThongKeApi` (003 mục 1.8a), `LuuTruThongKeApi` (004 mục 1.5a), chỉ đọc.
2. **005 → 019**: dùng các `*ThongKeApi` đã khai; yêu cầu chung ba điểm ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 2 (bản ghi có trạng thái do nguồn xác định, mốc, tầng theo FR-011, cờ chờ xem lại, loại Hủy ghi nhận; hàm đếm theo nhóm cho dashboard).
3. **000**: ghi nhật ký "Xuất báo cáo" qua `NhatKyApi`; đọc tóm tắt loại bỏ dữ liệu (000 FR-051).

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX).

1. **Câu chữ còn lẫn trong spec 016** (không đổi nghiệp vụ): *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - điểm báo lại "Q-306" ghi "thêm số sự kiện 'không tính'" — mục Clarifications lượt 2 đã nói chỉ tiêu này bỏ theo Q-307; FR-053 đúng;
   - FR-010 và bảng nhóm chỉ tiêu dẫn "4.4" (Permission Matrix của `docs/phan-tich-yeu-cau.md` đã bỏ; tra Phụ lục 27, 19.3 là căn cứ); Assumptions dòng đầu dẫn mục 4.2 của tài liệu đã bỏ.

2. **Q-03** (ghi nhật ký lần nhân viên xem dữ liệu sức khỏe) vẫn chờ tư vấn pháp lý theo Luật số 91/2025/QH15 (24.4); thiết kế không ghi; nếu phải ghi, cần thêm FR và một điểm ghi ở M14 (mọi API đọc trả dữ liệu sức khỏe). *(Còn mở 2026-10-10: chờ tư vấn pháp lý, mục 24.4.)*

## Ghi chú triển khai

### Phase 1, 2 (2026-10-24, nhánh `impl/016-reports-dashboard`)

- Hợp đồng dịch theo glossary mục 22; "chỉ tiêu" là `metric` (cùng cột `metric_group` của 002). Không xác định được phạm vi (FR-016) trả `PERMISSION_DENIED` kèm `failedLayer: SOURCE_DATA` như 002, không có mã riêng (chủ dự án chọn).
- Gói: `reporting.internal.metric` (`Metric`, `MetricGroup`, `MetricGroupAccess`, `MetricCatalog`), `reporting.internal.scope` (`ReportScope`, `ReportScopeService`, `DetailRowPolicy`, `ReportPeriods`).
- Bảng FR-010 mã hóa trong `MetricGroupAccess`; áp sau đó khóa `(DASHBOARD_REPORT, VIEW)` của 002 (thu hẹp mức khóa, `PermissionApi.permissions`). Ô toàn viện bị 002 hạ phạm vi thì tính như phạm vi phân công.
- Phạm vi theo ca của Điều dưỡng, Người phụ trách ca lấy từ `AssignmentApi.history` (SPI của 002, 008 cài) thay cho `CaTrucApi.caCuaNhanVien` ở research O-03: phân công gắn ca đã mang giờ ca, CFG-M15-07 cộng hai đầu; ca hủy, vắng, nghỉ có duyệt thuộc luật của 008 khi trả lịch sử phân công. Trưởng tầng: phân công `FLOOR_MANAGER` đang hiệu lực lúc xem, không giới hạn thời gian.
- Thu hẹp chi tiết theo `metric_group`, `channel` (FR-010a): "Đổi api" 002 `PermissionApi.metricGroupRestrictions` (chủ dự án duyệt 2026-10-24) trả dòng thu hẹp theo từng vai trò và theo tài khoản; `MetricGroupAccess.scopes(roles, group, channel, restrictions)` áp theo vai trò trước khi hợp, theo tài khoản sau (A-05); bỏ thao tác thì nhóm không hiện, hạ phạm vi thì ô toàn viện thành phạm vi phân công (dashboard dùng phân công đang góp lúc xem). Dashboard dùng kênh `DASHBOARD`, báo cáo và xuất dùng `REPORT`.
- Dòng rút gọn (Q-196) xét nhóm thông tin theo vai trò, không theo phạm vi hiện hành: "Đổi api" 002 `FieldVisibilityApi.canViewByRole`. Hai hàm có mặc định trong interface nên bản giả cũ không phải sửa; `FakePermissions` có thêm `metricGroupRestrictions(...)`.
- FR-016: M14 từ chối khi `InvalidPermissionDataException`; việc báo Quản lý viện (002 FR-041b) do 002 ghi khi kiểm quyền, `ScopeApi.filter` hiện chưa ghi. *(Sửa 2026-11-01: M14 gọi `PermissionApi.recordDataError`, xem mục US2.)*

### Phase 11 (US9, xuất) (2026-10-24, nhánh `impl/016-report-export`)

- `LICENSE.FACILITY` chỉ là chỉ tiêu dashboard (FR-020), không thuộc nhóm báo cáo nào của `ReportGroup` nên không xuất được (FR-061 "Dashboard không có lệnh xuất"); chủ dự án chọn cài khung xuất chung, test bằng chỉ tiêu giả `TEST.MISSED_DOSES` (nhóm Liều thuốc, báo cáo `HEALTH_MEDICATION`) có định danh người cao tuổi, chia nhóm theo ca. `LICENSE.FACILITY` cài ở `reporting.internal.license` (nguồn `FacilityLicenseApi.expiringWithin` với CFG-M06-02, chưa có bản cài của M06 thì rỗng) cho dashboard sau này.
- Mỗi chỉ tiêu là một bean `MetricSource` (chỉ tiêu và nguồn bản ghi); `MetricCatalog` gom theo mã. `ReportGroup` gán nhóm chỉ tiêu FR-010 vào báo cáo theo dải FR của spec (18.1 → 18.6); nhóm chỉ có ở dashboard không vào báo cáo nào. Spec không có bảng gán này; cần đổi thì sửa `ReportGroup`.
- `ReportService.period` tính báo cáo cho cả màn hình và xuất (cùng số, FR-061): nhóm người xem không thấy thì bỏ (`ReportScopeService.visible`, không ném lỗi, vì ngoại lệ ra khỏi bean `@Transactional` đánh dấu rollback transaction của lệnh xuất); không thấy nhóm nào thì từ chối như `period`. `GET /reports/{group}` chưa cài (cần màn đã duyệt), còn ở `pending` của `ApiContractIT`.
- `ExportContent.decide` (`NoiDungDuocXuat`): có định danh chỉ Quản lý viện (vai trò khác 403 "chỉ Quản lý viện xuất được…"), bắt buộc mục đích (422 `PURPOSE_REQUIRED`); không định danh thì bỏ mọi dòng tổng hợp (tổng hoặc ô chia nhóm) mà tập người cao tuổi của nó có đúng một người, không có trang chi tiết; `SHIFT.WORKING_HOURS` chỉ Quản lý viện. Quản lý viện không chọn định danh cũng chỉ có tổng hợp. "Số dòng" của nhật ký là dòng tổng hợp cộng dòng chi tiết.
- Thứ tự (research O-08): tính báo cáo → dựng XLSX (`ExportWorkbook` trên `common.document.ExcelDocument`: đầu trang CFG-M11-11, người xuất, "tính đến", khoảng, bộ lọc, chế độ chi phí với báo cáo chi phí, mục đích) → `FileApi.upload` và `attach(fileId, "M14.REPORT_EXPORT", fileId)` → `AuditLogApi.write` `REPORT_EXPORTED` (đối tượng `M14.REPORT_EXPORT`/mã tệp; nhóm báo cáo, khoảng, bộ lọc, phạm vi, chế độ, số dòng, dấu định danh, định dạng ở `details`; mục đích là lý do, bắt buộc khi có định danh) trong cùng transaction → trả `{fileId, auditLogId}`. Lỗi ghi nhật ký thì rollback dòng tệp (đối tượng S3 mồ côi vô hại như `FileService.upload`). Tasks ghi POI; dùng fastexcel theo research O-08 và `common.document`.
- Quyền tải (`ReportExportFilePermission`): Quản lý viện (khóa `AuditLogPermissions.VIEW` toàn viện, như 000 FR-049) hoặc người thực hiện của bản ghi nhật ký lần xuất. `GET /reports/exports` đọc nhật ký: Quản lý viện mọi lần, người khác lần của mình. Cần "Đổi api" 000 `AuditLogApi.recorded` (`AuditLogRecord`; 000 module-interfaces mục 1.2) vì M14 không có bảng và không đọc bảng của M15.
- api-016 thêm schema `ExportResult` (`fileId`, `auditLogId`) cho phản hồi 201 của xuất và `ExportRecord` (`auditLogId`, `fileId`, `exportedAt`, `exportedBy`, `role`, `group`, `from`, `to`, `includeIdentity`, `purpose`, `rowCount`) cho 200 của `GET /reports/exports` (chủ dự án duyệt 2026-10-24); trước đó hợp đồng không có nội dung phản hồi.
- CSV (`format: CSV`) bị từ chối 422 `INVALID_VALUE` vì kho tệp chưa nhận `text/csv` (`FileTypes.SYSTEM_GENERATED`); research O-08 để CSV là tùy chọn. Khóa lạ trong `filters` cũng 422 `INVALID_VALUE`.

### Dashboard, khung báo cáo và màn web (2026-10-24, nhánh `impl/016-report-screens`)

- Bố cục duyệt (DESIGN.md 5.21.0): Tổng quan thành dashboard 016 thay bản mẫu cũ; mục menu "Báo cáo" ngay dưới Tổng quan, tab theo nhóm báo cáo vai trò thấy (suy từ bảng FR-010, `REPORT_GROUPS` của web) và tab "Lần xuất".
- `MetricEvaluator` (`reporting.internal.metric.service`) là đường tính duy nhất cho dashboard, báo cáo, danh sách chi tiết, xuất: nguồn lọc thô, rồi `ReportScope.covers` và bộ lọc chung. Dashboard truyền khoảng hôm nay; nguồn "lúc xem" (giấy phép) bỏ qua khoảng.
- `DashboardService`: khối theo các nhóm của bảng FR-020 (`GROUPS`, thứ tự dòng), chỉ nhóm thấy ở kênh `DASHBOARD`; phạm vi rỗng thì bỏ khối, không còn khối thì "Hiện không có phạm vi phân công"; nhóm thấy mà chưa có chỉ tiêu trả khối `values` rỗng (web ghi "Chưa có dữ liệu"). Bộ đệm 20 giây (thuộc tính kỹ thuật `eldercare.dashboard.cache`, mặc định `PT20S`, vì `TimeArchTest` cấm hằng `Duration` trong logic module) theo (nhóm, `ReportScope`), dọn mục hết hạn mỗi lần gọi; "tính đến" là lúc lấy số của khối cũ nhất. ETag băm khối và thông điệp (không gồm "tính đến"). Research O-06 ghi chạy song song bằng luồng ảo, thời hạn 2,5 giây, khối quá hạn "đang cập nhật": chưa làm vì luồng ảo mất người dùng hiện tại (phạm vi request) và transaction; hiện chạy tuần tự, khối luôn `COMPLETE`; làm khi có nguồn thật và đo SC-002.
- `GET /metrics`: chỉ tiêu của nhóm thấy ở dashboard hoặc báo cáo; không có khóa "Dashboard, báo cáo" thì danh sách rỗng (hợp đồng chỉ có 200).
- `GET /reports/{group}`: `date` là báo cáo tại một ngày (từ ngày đó tới ngày đó); khoảng chứa hôm nay thì mọi giá trị thêm nhãn `DAY_NOT_ENDED` (cả file xuất); báo cáo chi phí có ghi chú FR-050. Bộ lọc tầng trên web chờ danh mục tầng của 003.
- `GET /metric-details/{metricCode}`: không có khoảng thì như dashboard (hôm nay, phạm vi hiện hành, kênh `DASHBOARD`); 50 dòng mỗi trang; `asOf` nhận nhưng chưa dùng (nguồn chưa trả số liệu tại một thời điểm); mã không có thì 404.
- Dải "Cần xử lý ngay" (`dashboard-urgent`) chưa có trên web: cần chỉ tiêu cảnh báo, sự cố leo thang tối đa của 007.

### US8 phần 008 (2026-10-27, nhánh `impl/016-staffing-metrics`)

- "Đổi api" trên M09 (chủ dự án duyệt, E review): `StaffingRatioView` thêm `staffCount`, `totalWeight`, `shiftTemplateId`, `shiftStartsAt`, `shiftEndsAt` (đề xuất ban đầu chỉ có `shiftStartsAt`; thêm `shiftEndsAt` để biết ca đang diễn ra, ca đã kết thúc); `HandoverReportView` thêm `shiftEndsAt`; hàm mới `StaffStatisticsApi.openHandovers(scope)` (Đã lập, Có ý kiến, Bản nháp của ca Chờ bàn giao, không giới hạn ngày ca); `ExpiringCredentialView` thêm `credentialGroup`. Bản giả `FakeStaffStatistics.openHandovers` lọc theo trạng thái. Không đổi hàm cũ, không thêm migration.
- Lệch kỹ thuật trong khung: `MetricSource.current(rough, from, to)` (mặc định gọi `records`) là đường "lúc xem" của dashboard và `GET /metric-details` không khoảng (`MetricEvaluator.current`, `currentDetails`). Lý do: dashboard truyền khoảng hôm nay cho mọi nguồn nên `SHIFT.STAFFING_RATIO` không phân biệt được ca hiện tại (FR-020) với ca đã diễn ra (FR-070) dùng cùng mã. Nguồn khác giữ nguyên hành vi.
- `SHIFT.STAFFING_RATIO` (`reporting.internal.staffing.service.StaffingRatioMetric`): đếm ca, chia khóa `MET`, `NOT_MET`, `NOT_CONFIGURED`. Dashboard: ca đang diễn ra (ca qua đêm từ hôm qua), không đạt theo lần tính mới nhất. Báo cáo: ca đã kết thúc có giờ bắt đầu trong khoảng (xếp tháng theo giờ bắt đầu ca), không đạt theo dấu "từng không đạt" (Q-200). Ngưỡng rỗng là `NOT_CONFIGURED`. Dòng chi tiết có tỷ lệ, ngưỡng, số nhân viên, tổng trọng số, mẫu ca; "tổng hợp theo tầng, mẫu ca" của FR-070 hiện qua bộ lọc tầng và dòng chi tiết (một khóa chia nhóm mỗi chỉ tiêu).
- `SHIFT.UNCONFIRMED_HANDOVERS` (`UnconfirmedHandoversMetric`): nguồn `openHandovers`, chia theo trạng thái, `waitingMinutes` từ giờ kết thúc ca.
- `LICENSE.STAFF` (`reporting.internal.license.service.StaffLicenseMetric`): một lần gọi `expiringCredentials` với mốc lớn hơn của CFG-M06-02, CFG-M09-03 rồi lọc theo `credentialGroup`; chia theo nhóm; họ tên qua `ShiftApi.staff`. Trưởng tầng (Q-389): nhân viên có dòng `assignmentHistory(phạm vi, lúc xem, hết cửa sổ)`; `floorAreaId` của bản ghi là một tầng trong phạm vi, chỉ để lọc bước 2.
- Web: không đổi (tab "Nhân sự, ca" và khối dashboard dùng chung); khóa chia nhóm hiện nguyên mã như các chỉ tiêu khác.
- Còn chờ 015 (`XoayCaThongKeApi`): FR-071 → FR-073. T023, T024 chưa đánh `[x]`.

### Chỉ tiêu giường (2026-10-27, nhánh `impl/016-bed-metrics`)

- "Đổi api" trên M03 (chủ dự án duyệt, C review): `BedStateOnDate` thêm `roomId`, `holdReason` (suy từ sự kiện `HOLD_WAITING_RECORD`, `ABSENT_KEEP_BED` của lịch sử, không thêm cột); `AwaitingReadyInterval` thêm `startedAt`. Bản giả `FakeHandoverLocations` truyền `0L`, `null`, `awaitingSince` cho trường mới. Không đổi hàm, không migration.
- Mã con (chủ dự án duyệt; khung mỗi mã một số và một khóa chia nhóm): `BED.STATUS` (tổng giường, chia theo trạng thái, giữ chỗ tách `HELD_WAITING_RECORD`, `HELD_ABSENT_RESIDENT`), `BED.USABLE`, `BED.ISOLATED_ROOMS`, `BED.OCCUPANTS` (chia theo tầng; phòng ở dòng chi tiết) trong nhóm `BEDS`; `BED.AWAITING_READY` (trung bình phút), `BED.AWAITING_READY_MAX`, `BED.AWAITING_READY_OVERDUE` (chia theo tầng) trong nhóm `BED_AWAITING_READY`.
- Thời điểm: lúc xem là hiện tại; báo cáo là ngay trước cuối khoảng, không quá lúc xem (FR-024 tại một ngày đã qua).
- Q-390: lần Chờ sẵn sàng có lúc bắt đầu trong khoảng; đọc `awaitingReady` tới lúc xem để lần kết thúc sau cuối khoảng vẫn đủ thời gian; vượt hạn là `to` sau `deadline` của 003.
- `BED.EMPTY_AWAITING_TRANSFER` (Trống – chờ xử lý hư hỏng) khai trong `chi-tieu.md` nhưng chưa cài: dấu của 019 ở port nội bộ M03 chưa có bản cài; cài chỉ tiêu trả 0 sẽ sai nghĩa.
- Test chỉ tiêu dùng stub cục bộ của `BedStatisticsApi` trong lớp test (bản giả chung không có lý do giữ chỗ, phòng cách ly); `FakeBedAllocations` cho số người.

### US2 phạm vi và giới hạn trường (2026-11-01, nhánh `impl/016-report-scope`)

- Dashboard không còn dùng nguyên `ScopeApi.filter` cho mọi nhóm: "Đổi api" 002 `ScopeApi.assignedFilter(user, kinds)` trả phạm vi hiện hành chỉ từ các loại phân công ô của nhóm cho phép; lý do: tài khoản Điều dưỡng kiêm Trưởng tầng thấy người phân công ở nhóm chỉ dành cho Trưởng tầng (FR-015).
- Báo cáo khoảng lấy giao của `AssignmentApi.history` với `ShiftApi.staffShifts` (thành viên hiệu lực của 008): ca hủy, nghỉ có duyệt, vắng từ đầu ca không tính. Lệch so với ghi chú Phase 1, 2: lịch sử phân công của 008 không tự loại các ca này.
- Chủ dự án chốt 2026-11-01: vắng giữa ca thì phạm vi dừng tại `absent_at`, không bỏ cả ca (khớp "thật sự có tên trong ca" và cách 008 tính Người phụ trách ca); vắng từ đầu ca thì không tính. Mốc kết thúc sớm (`scopeEndsAt`) không cộng CFG-M15-07; bổ sung giữa ca không cộng biên trước mốc bổ sung. `AssignmentQueries.history` trả `from` là lúc phân công bắt đầu thay cho giờ bắt đầu ca (A sửa trên M09, chờ E xem).
- Việc chung, liều chung (Q-61, Q-85): `Grant.commonFloorAreaIds` là tầng/khu của ca (`ShiftView.floorAreaId`), chỉ khớp bản ghi không gắn người cao tuổi; dùng cho cả ô `ASSIGNMENT_AREA` (khoanh vùng, bàn giao).
- `GROUPS` thêm `WAITING_LIST`, `NON_HEALTH_INCIDENTS` (US2 #1); người thấy nhóm sức khỏe không có khối sự cố riêng.
- FR-016: "Đổi api" 002 `PermissionApi.recordDataError` ghi lỗi và báo Quản lý viện khi M14 bắt `InvalidPermissionDataException`; thay ghi chú ở Phase 1, 2.
- Hợp đồng (chủ dự án duyệt 2026-11-01): `Report.filterOptions` (`facilityWide`, `floorAreas`), `/metric-details` nhận bộ lọc của báo cáo (trước đó danh sách bỏ qua bộ lọc nên lệch số, FR-009). Web: bộ lọc tầng trên màn Báo cáo; phạm vi toàn viện lấy danh mục `GET /floors`, không gọi được thì không hiện; chi tiết và file xuất cùng bộ lọc.

### T021, T027 phần chạy được (2026-11-01, nhánh `impl/016-detail-readonly`)

- `SC_008_ReadOnlyIT` so `CHECKSUM TABLE` mọi bảng trước và sau khi xem; bỏ `login_session`, `device` (mỗi yêu cầu ghi lần kết nối cuối, không phải dữ liệu nghiệp vụ). Lần xuất chỉ đổi `audit_log`, `file`, `idempotency_key`.
- Dòng rút gọn luôn `canOpenRecord = false` (`DetailRow.asReduced`), nghiêm hơn FR-012 (mở được khi quyền hiện hành ở feature sở hữu cho phép, ví dụ quyền "X" của Trưởng tầng ở dòng phát thuốc). Xét lại khi có chỉ tiêu thật: M14 cần biết khóa quyền của từng loại bản ghi nguồn.

### Chỉ tiêu sức khỏe, sự cố, tài sản (2026-11-08, A, nhánh `impl/a-now-2026-11-08`)

- Nguồn thật: `IncidentStatisticsApi` (007: `alerts`, `incidents`, `underMonitoring`, `criticalResidentIds`), `ResidentStatisticsApi.overdueReassessments` (001), `AssetStatisticsApi.overdue` (003). Chỉ tiêu "lúc xem" mà nguồn chỉ trả mã người cao tuổi (`HEALTH.CRITICAL_FLAGS`, `HEALTH.OVERDUE_REASSESSMENTS`) lấy tầng, phòng hiện tại qua `ResidentLocationApi.current` để phạm vi tầng lọc được (FR-011).
- `Metric` có một phép tính và một khóa chia, nên mỗi mã chia theo một khóa: cảnh báo, sự cố theo mức hiện hành (thời gian tiếp nhận cảnh báo theo mức lúc tiếp nhận), `HEALTH.SOS_ASSISTANCE_ALERTS` theo nguồn, `HEALTH.FOLLOW_UPS` theo loại theo dõi, `HEALTH.NON_HEALTH_INCIDENTS` theo loại sự cố, `ASSET.OVERDUE` theo loại dấu; tầng, loại cảnh báo qua bộ lọc chung và dòng chi tiết.
- Thời gian tiếp nhận, số lần leo thang chỉ đọc bản ghi đã tiếp nhận, có leo thang (bản ghi khác không vào phép tính; số chưa tiếp nhận đọc ở `HEALTH.ALERTS`, `HEALTH.INCIDENTS`).
- Còn thiếu so với FR (ghi ở team-plan mục 8 cho 007): (1) `IncidentReportView` chưa có mức lúc tiếp nhận nên `HEALTH.INCIDENT_RESPONSE_TIME` chia theo mức hiện hành; (2) nguồn bỏ sự cố Đã hủy nên chưa có dòng tham khảo "số sự cố đã hủy" (FR-042); (3) chưa có trạng thái sự cố cho danh sách của Hành chính (FR-042a); (4) chưa có nguồn khoanh vùng cho `HEALTH.CONTAINMENT_ZONES`. Trung bình, lớn nhất thời gian tới nơi theo tầng của O-13 cần một mã hoặc kiểu phép tính mới, chưa làm; số liệu từng cảnh báo có ở dòng chi tiết.
- `ASSET.OVERDUE` đếm cặp (tài sản, dấu): tài sản mang hai dấu đếm ở hai ô; phần "tới mốc nhắc CFG-M03-09" chưa có nguồn ở `AssetStatisticsApi`.
- Test: `MetricHealthEventsTest` (5 ca), `MetricAssetOverdueTest`, `FR_042a_AdminIncidentIT` (chỉ tiêu thật, Q7); `FR_062_ExportAuditIT` đổi số dòng xuất từ 11 thành 21 (mười chỉ tiêu mới nguồn rỗng, mỗi mã một dòng tổng).

### Chỉ tiêu người cao tuổi, công việc, bữa ăn, hoạt động, kiểm tra (2026-11-08, A, nhánh `impl/a-now-2026-11-08`)

- Nguồn thật: `ResidentStatisticsApi` (001: `statusAtDate`, `changes`), `ResidencyStatisticsApi.residenceTypeAtDate` (004), `TaskStatisticsApi.inRange` (005), `NutritionStatisticsApi.slips` (011), `ActivityStatisticsApi` (014: `sessions`, `trips`, `qualityChecks`); tầng tại ngày của người cao tuổi qua `ResidentLocationApi.floorOn`. Nguồn nhận ngày (`LocalDate`) đổi từ khoảng `[from, to)` theo giờ Việt Nam như `WaitingListRecords`.
- `RES.AT_DATE`, `RES.IN_RESIDENCE` báo cáo theo ngày cuối của khoảng (dashboard: hôm nay). 004 chỉ lọc theo mã người cao tuổi nên đọc toàn viện rồi ghép với người 001 trả trong phạm vi. `RES.AT_DATE` chia theo loại lưu trú; trạng thái, mức ở dòng chi tiết. `RES.MOVEMENTS` đếm bảy lệnh của FR-023 và đổi mức chăm sóc; lệnh ghi nhận quay về tách theo trạng thái trước (kết thúc Tạm vắng, về sau điều trị); điểm danh đi, về hoạt động bên ngoài không tính.
- `TASK.OVERDUE`: báo cáo đếm công việc có thời điểm dự kiến trong khoảng đã có mốc vào Quá hạn trong khoảng; dashboard đếm công việc đang Quá hạn lúc xem (đọc `inRange` từ `Instant.EPOCH`). Việc do "Hệ thống" đóng là Không thực hiện, Hủy không có người đóng; `TASK.BY_STATUS` tách thành ô `<trạng thái>:SYSTEM:<quy tắc>`. Việc chung của tầng (không gắn người cao tuổi) là ô `COMMON` của `TASK.BY_STAFF`.
- `ACT.PARTICIPATION_RATE`: 014 trả số lượt theo buổi, 016 dựng mỗi lượt Có mặt, Vắng (từ chối, sức khỏe, khác) thành một bản ghi để tỷ lệ và dòng chi tiết cùng tập (FR-009); dòng chi tiết là lượt, không có người cao tuổi. `ACT.OVERDUE_TRIPS` tính theo giờ về dự kiến hiện hành (lần gia hạn cuối) và CFG-M04-07 đọc lúc tính; ô `OVERDUE_DOUBLE` cho chuyến tới mốc gấp đôi.
- Còn thiếu: *(`ACT.ISOLATION_RISK` bỏ theo Q-407, 2026-11-10, FR-036 bỏ)*; nhãn ghi nhận muộn của điểm danh hoạt động (FR-032, 014 chưa trả); mục "ngoài phạm vi kiểm tra" (Q-168, Q-254) không có ở nguồn; có mặt bán trú hôm nay của FR-020 (005 `attendanceInRange`) chưa ghép vào `RES.IN_RESIDENCE`; đổi loại lưu trú của FR-023 chưa có nguồn ở 004; bộ lọc mức chăm sóc chưa áp (hợp đồng nhận `careLevelId` số, 001 trả `CareLevel`).
- Dashboard `HEALTH.ALERTS`, `HEALTH.INCIDENTS` hiện đếm bản ghi tạo hôm nay; FR-020 cần cảnh báo đang mở và sự cố chưa đóng mọi ngày, cần trạng thái ở view của 007 (cùng dòng "Đổi api" ở team-plan mục 8).
- Test: `MetricResidentsTest` 3 ca, `MetricCareTasksTest` 4 ca, `MetricMealActivityQualityTest` 4 ca.

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Feature không có bảng. Số liệu đọc qua API của module như trước, không đọc bảng chung trực tiếp; API module giữ nguyên nên báo cáo và bảng truy vết số liệu không đổi.

## Phase 13, Convergence (T106 → T108, 2026-11-06, A)

- Nguồn thật đã có: 004 `ResidencyStatisticsApi.waitingList`, 006 `MedicationStatisticsApi`, 007 `VitalSignStatisticsApi`. Lớp mới ở `reporting.internal.waitinglist`, `internal.medication`, `internal.vitalsign` (`service`), mẫu như `internal.bed`.
- `RES.WAITING_LIST` (FR-025): hồ sơ có ngày đăng ký trong khoảng (đổi `[from, to)` sang ngày theo giờ Việt Nam), chia theo trạng thái hiện tại của 004, lọc `residenceType`; loại lưu trú, mức chăm sóc dự kiến ở dòng chi tiết. Thời gian chờ trung bình, dài nhất tách thành `RES.WAITING_TIME`, `RES.WAITING_TIME_MAX` như tiền lệ `BED.AWAITING_READY_MAX` (chi-tieu.md, glossary 22.4): số liệu "lúc xem" của hồ sơ Chờ, Đã liên hệ, ngày từ ngày đăng ký tới hôm nay, chia theo loại lưu trú. Lệch còn lại: "số hồ sơ rời danh sách trong khoảng theo lý do" chưa tính được vì 004 để `leftOn` trống (M02 chưa lưu ngày rời danh sách); báo B.
- `MED.MISSED_RATE`, `MED.REFUSED_RATE` (FR-043, 006 FR-051): tỷ lệ trên liều có trạng thái cuối Đã dùng, Từ chối, Không thực hiện, Bỏ lỡ; Không thực hiện do hệ thống đóng (FR-047) nhận biết bằng `closedByStaffId` rỗng vì view không có loại người đóng; chia theo ca (`NO_SHIFT` khi 006 không xác định được ca), lọc người cao tuổi bằng bộ lọc chung; bản ghi ngoại tuyến Chờ xem lại gắn nhãn. Các loại loại khỏi mẫu số (Đã hủy, Tạm dừng, do hệ thống) và liều dùng trễ "đếm riêng" chưa có mã chỉ tiêu riêng ở chi-tieu.md, dòng chi tiết có `administeredLate`.
- `MED.OVERDUE_RECONCILIATIONS` (FR-044): phiếu Mở, Chờ xác nhận mang dấu quá hạn; "lúc xem" nên đọc `reconciliationsInPeriod(EPOCH, MAX)` vì 006 lọc theo lúc lập phiếu.
- `HEALTH.VITAL_SIGNS` (FR-040): số lần đo đã xác nhận trong khoảng, chia theo mức lúc đo (vượt ngưỡng là ô `WARNING`, `DANGEROUS`); loại chỉ số ở dòng chi tiết, tầng qua bộ lọc chung (một khóa chia nhóm mỗi chỉ tiêu). Lệch còn lại: chuỗi giá trị theo thời gian cho một người cao tuổi chưa có vì `VitalSignReportView` không có giá trị đo; báo D.
- Ô chia nhóm trên web hiện khóa thô (`WAITING`, mã ca) như các chỉ tiêu có sẵn; nhãn ô để task web của nhóm.
- Test: ba lớp test mới viết trước (lỗi biên dịch vì chưa có lớp) rồi xanh. `FR_062_ExportAuditIT` đếm dòng xuất nhóm Sức khỏe, thuốc: thêm `@MockitoBean` hai nguồn để số dòng không phụ thuộc dữ liệu sót, số dòng 7 → 11 (thêm một dòng tổng mỗi chỉ tiêu thật).

## Rà T030 (2026-11-06, A)

- SC-001: 15 mã của chi-tieu.md và `SHIFT.STAFFING_RATIO`, `SHIFT.UNCONFIRMED_HANDOVERS` đã cài đều có test; 43 mã chưa cài vì chờ nguồn (team-plan mục 8), test viết khi cài.
- DBR-25, FR-005: mốc ngày qua `BusinessTime` (giờ Việt Nam), không có lời gọi giờ hệ thống trong `reporting`; web hiện ngày qua `common/dateTime` (dd/MM/yyyy); `FR_060_ReportPeriodTest`. Tiền VND chưa có chỉ tiêu chi phí.
- FR-002: `M14ArchTest.FR_001_noDirectDataAccessOrEvents`, `FR_001_noCommandApisOfOtherModules` (chỉ đọc API của module nguồn); trạng thái lấy từ view của nguồn (ví dụ `DoseReportView.finalStatus` đã tính đính chính).
- FR-004: `ReportGroup` sáu nhóm; thêm `M14ArchTest.FR_004_noSourcesOutsideThisPhase` chặn `reporting` dùng `family`, `belongings`, `notification`.
- FR-006: `MetricRecord.referenceTime` (liều theo thời điểm dự kiến, chỉ số theo lúc đo), chia theo ca theo ca của nguồn; `FR_012_TimeScopeIT`.
- FR-007: Hủy ghi nhận, chờ xem lại (`MetricTest`), "ngày chưa kết thúc" (`ReportApiIT`), "tính đến" (`asOf`) có; nhãn loại bỏ theo lưu trữ thiếu (T109).
- FR-008: `NO_DATA`, tử số và mẫu số (`MetricTest`, `ReportsPage.test.jsx`), `NOT_CONFIGURED` (`MetricShiftStaffingRatioTest`).
- FR-055, FR-055a: chờ `CostStatisticsApi` thật (010) và nguồn mua sắm (019), thuộc T020.
- T109 hoãn (chủ dự án chọn 2026-11-06): nhãn chỉ hiện khi khoảng báo cáo phủ dữ liệu đã quá thời hạn lưu giữ (mặc định 10 năm), chưa có tác dụng trong giai đoạn này; làm khi đủ chỉ tiêu thật để khai loại dữ liệu cho mọi chỉ tiêu một lần. Cách đã đề xuất: trường `purgedDataTypes` (mảng mã loại dữ liệu) trong `MetricValue` của api-016.

## Chỉ tiêu còn thiếu nguồn (2026-11-11, luồng C của A)

- *(Đổi api 019, T008)* `AssetStatisticsApi.dueSoon(scope, on)`: hạn bảo trì, đăng kiểm, bảo hiểm trong `[on, on + CFG-M03-09]`, chưa mang dấu quá hạn của hạn đó, tài sản chưa thanh lý (cùng cửa sổ với `AssetReminders`, BR-M03-17). Lý do: FR-020 dòng "Tài sản quá hạn" có "số tài sản tới mốc nhắc" mà nguồn chỉ trả dấu quá hạn. Trả lại `OverdueAssetView` (trường `flag` là dấu sẽ gắn khi qua hạn) thay cho DTO mới vì cùng các trường. Chỉ tiêu riêng `ASSET.DUE_SOON` (`TS.TOI_MOC_NHAC`, chi-tieu.md) chia theo loại hạn (`MAINTENANCE`, `VEHICLE_INSPECTION`, `INSURANCE`), vì mỗi mã một khóa chia (tiền lệ `RES.DAY_CARE_ATTENDANCE`). Test: `T008_AssetDueSoonStatisticsIT` (bản thật), `MetricAssetOverdueTest.FR_020_dueSoon_…`; nhãn ô ở `reportLabels.js`.
- *(Đổi api 007/health, T018)* `VitalSignReportView` thêm `values` (giá trị hiện hành theo thành phần, áp đính chính). Lý do: FR-040 "cho một người cao tuổi, chuỗi giá trị theo thời gian". Chuỗi là danh sách chi tiết của `HEALTH.VITAL_SIGNS` lọc `residentId` (bộ lọc chung đã có), thứ tự theo lúc đo; dòng chi tiết thêm `values` trong kết quả nhóm `HEALTH` (FR-014 ẩn với vai trò không xem được). Không thêm mã chỉ tiêu hay endpoint. Test: `VitalSignStatisticsIT`, `MetricHealthVitalSignsTest.FR_040_oneResidentSeries_…`.
- *(017 T038)* `US8_BalanceStatisticsIT` kiểm `BALANCE.LOW_OR_OWING` đọc `BalanceStatisticsApi` thật (`BalanceStatistics`); không lớp test nào của 016 import `FakeBalanceConfig`, nên production và test 016 đều dùng bản thật, không phải bỏ `@Primary`.
- Còn lại, chưa làm: (1) dòng tham khảo "số sự cố đã hủy" (FR-042, FR-007) cần trường mới ở `MetricValue` của api-016, chờ chủ dự án; (2) "ngoài phạm vi kiểm tra" của FR-037 (Q-168, Q-254) cần 014 trả công việc bị loại khỏi mẫu vì do Trưởng tầng được giao hoặc Người phụ trách ca làm: logic chọn mẫu nằm trong `care.internal.qualitycheck` (luồng khác đang sửa `care`), chưa thêm; (3) Q12 mở bản ghi chỉ xem từ dòng chi tiết (T021) cần bảng loại bản ghi → màn chỉ xem của feature sở hữu và khóa quyền của từng nguồn; (4) đo SC-002 (T007) thuộc nghiệm thu.

## T014 bộ lọc mức chăm sóc, đổi loại lưu trú (2026-11-11, luồng C của A; chủ dự án duyệt 2026-11-11)

- Hợp đồng api-016 (chủ dự án duyệt 2026-11-11): tham số `careLevelId` (int64) đổi thành `careLevel` (enum `BASIC`, `REGULAR`, `SPECIAL` theo `resident.api.dto.CareLevel` của 001) ở `GET /reports/{group}` và `GET /metric-details/{metricCode}`; khóa `filters.careLevel` của lệnh xuất. Lý do: 001 chỉ có ba mức enum, không có danh mục mức có mã số. `ReportFilters.careLevel` là chuỗi tên enum như `residenceType`; áp ở `RES.AT_DATE`, `RES.IN_RESIDENCE` (mức hiệu lực tại ngày) và `RES.WAITING_LIST`, `RES.WAITING_TIME*` (mức dự kiến), là các chỉ tiêu có mức của người cao tuổi ở nguồn.
- *(Đổi api 004, chủ dự án duyệt 2026-11-11)* `ResidencyStatisticsApi.residenceTypeChanges(scope, from, to)`: phiên bản nội dung (`contract_version` loại `CONTENT`) của hợp đồng Hiệu lực, Kết thúc, Chấm dứt có loại lưu trú khác phiên bản liền trước (`LAG` theo `sequence_no`), ngày hiệu lực trong khoảng. `RES.MOVEMENTS` thêm ô `RESIDENCE_TYPE_CHANGE` (mốc là đầu ngày hiệu lực, tầng tại ngày); `MovementRecord` đổi từ bọc `ResidentChangeView` sang trường chung (nguồn, người, lúc, loại, kết quả) để gộp hai nguồn 001, 004.
- Web: bộ lọc "Mức chăm sóc" chỉ ở tab Người cao tuổi, gửi cùng báo cáo, danh sách chi tiết (FR-009 cùng số) và file xuất.

## T016 "ngoài phạm vi kiểm tra" (2026-11-12, luồng C của A; người dùng duyệt sửa `care`)

- *(Đổi api 014)* `ActivityStatisticsApi.outOfCheckScope(scope, from, to)` → `OutOfCheckScopeView(careTaskId, shiftId, floorAreaId, performerStaffId, performerRole, performedAt)`. Bản cài ở `QualityCheckService.outOfCheckScope` (chỉ đọc, dùng cùng luật loại khỏi mẫu của `eligible`: Trưởng tầng được giao tại lúc thực hiện; Người phụ trách ca khi ca không có Trưởng tầng được giao; bỏ công việc làm lại và kết quả đã Hủy ghi nhận), ca lấy từ cặp (ca, tầng) có công việc hoàn thành trong khoảng (`QualityCheckStore.shiftsCompletedIn`).
- Chỉ tiêu `QC.OUT_OF_CHECK_SCOPE` (`KT.NGOAI_PHAM_VI_KIEM_TRA`, chi-tieu.md) chia theo nhân viên thực hiện, dòng chi tiết có `performerRole`; `QC.PASS_RATE` không đổi (chỉ đọc mục đã kiểm tra) nên không có tỷ lệ Đạt cho các công việc này. Web: ô theo nhân viên như `QC.PASS_RATE`.
