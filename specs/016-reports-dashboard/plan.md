# Implementation Plan: Báo cáo và dashboard

**Branch**: `016-reports-dashboard` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/016-reports-dashboard/spec.md`

## Summary

Feature 016 là module M14 – Báo cáo (mục 18; UC-67). Nó chỉ đọc và cung cấp: dashboard theo vai trò cho số liệu trong ngày và ca hiện tại; báo cáo người cao tuổi, chăm sóc, chất lượng, sức khỏe và thuốc, chi phí phát sinh, nhân sự và ca trực; danh sách chi tiết của mọi con số; xuất file có ghi nhật ký. Mọi số liệu áp phạm vi dữ liệu và giới hạn trường của người xem.

Hướng kỹ thuật:
- Gói `m14` đọc qua `*ThongKeApi` và API đọc mà các module đã khai; không đọc bảng, không lưu bản sao (research O-01, O-02).
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
backend/src/main/java/com/cambae/backend/m14/
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
- Gói: `m14.internal.metric` (`Metric`, `MetricGroup`, `MetricGroupAccess`, `MetricCatalog`), `m14.internal.scope` (`ReportScope`, `ReportScopeService`, `DetailRowPolicy`, `ReportPeriods`).
- Bảng FR-010 mã hóa trong `MetricGroupAccess`; áp sau đó khóa `(DASHBOARD_REPORT, VIEW)` của 002 (thu hẹp mức khóa, `PermissionApi.permissions`). Ô toàn viện bị 002 hạ phạm vi thì tính như phạm vi phân công.
- Phạm vi theo ca của Điều dưỡng, Người phụ trách ca lấy từ `AssignmentApi.history` (SPI của 002, 008 cài) thay cho `CaTrucApi.caCuaNhanVien` ở research O-03: phân công gắn ca đã mang giờ ca, CFG-M15-07 cộng hai đầu; ca hủy, vắng, nghỉ có duyệt thuộc luật của 008 khi trả lịch sử phân công. Trưởng tầng: phân công `FLOOR_MANAGER` đang hiệu lực lúc xem, không giới hạn thời gian.
- Thu hẹp chi tiết theo `metric_group`, `channel` (FR-010a): "Đổi api" 002 `PermissionApi.metricGroupRestrictions` (chủ dự án duyệt 2026-10-24) trả dòng thu hẹp theo từng vai trò và theo tài khoản; `MetricGroupAccess.scopes(roles, group, channel, restrictions)` áp theo vai trò trước khi hợp, theo tài khoản sau (A-05); bỏ thao tác thì nhóm không hiện, hạ phạm vi thì ô toàn viện thành phạm vi phân công (dashboard dùng phân công đang góp lúc xem). Dashboard dùng kênh `DASHBOARD`, báo cáo và xuất dùng `REPORT`.
- Dòng rút gọn (Q-196) xét nhóm thông tin theo vai trò, không theo phạm vi hiện hành: "Đổi api" 002 `FieldVisibilityApi.canViewByRole`. Hai hàm có mặc định trong interface nên bản giả cũ không phải sửa; `FakePermissions` có thêm `metricGroupRestrictions(...)`.
- FR-016: M14 từ chối khi `InvalidPermissionDataException`; việc báo Quản lý viện (002 FR-041b) do 002 ghi khi kiểm quyền, `ScopeApi.filter` hiện chưa ghi.

### Phase 11 (US9, xuất) (2026-10-24, nhánh `impl/016-report-export`)

- `LICENSE.FACILITY` chỉ là chỉ tiêu dashboard (FR-020), không thuộc nhóm báo cáo nào của `ReportGroup` nên không xuất được (FR-061 "Dashboard không có lệnh xuất"); chủ dự án chọn cài khung xuất chung, test bằng chỉ tiêu giả `TEST.MISSED_DOSES` (nhóm Liều thuốc, báo cáo `HEALTH_MEDICATION`) có định danh người cao tuổi, chia nhóm theo ca. `LICENSE.FACILITY` cài ở `m14.internal.license` (nguồn `FacilityLicenseApi.expiringWithin` với CFG-M06-02, chưa có bản cài của M06 thì rỗng) cho dashboard sau này.
- Mỗi chỉ tiêu là một bean `MetricSource` (chỉ tiêu và nguồn bản ghi); `MetricCatalog` gom theo mã. `ReportGroup` gán nhóm chỉ tiêu FR-010 vào báo cáo theo dải FR của spec (18.1 → 18.6); nhóm chỉ có ở dashboard không vào báo cáo nào. Spec không có bảng gán này; cần đổi thì sửa `ReportGroup`.
- `ReportService.period` tính báo cáo cho cả màn hình và xuất (cùng số, FR-061): nhóm người xem không thấy thì bỏ (`ReportScopeService.visible`, không ném lỗi, vì ngoại lệ ra khỏi bean `@Transactional` đánh dấu rollback transaction của lệnh xuất); không thấy nhóm nào thì từ chối như `period`. `GET /reports/{group}` chưa cài (cần màn đã duyệt), còn ở `pending` của `ApiContractIT`.
- `ExportContent.decide` (`NoiDungDuocXuat`): có định danh chỉ Quản lý viện (vai trò khác 403 "chỉ Quản lý viện xuất được…"), bắt buộc mục đích (422 `PURPOSE_REQUIRED`); không định danh thì bỏ mọi dòng tổng hợp (tổng hoặc ô chia nhóm) mà tập người cao tuổi của nó có đúng một người, không có trang chi tiết; `SHIFT.WORKING_HOURS` chỉ Quản lý viện. Quản lý viện không chọn định danh cũng chỉ có tổng hợp. "Số dòng" của nhật ký là dòng tổng hợp cộng dòng chi tiết.
- Thứ tự (research O-08): tính báo cáo → dựng XLSX (`ExportWorkbook` trên `common.document.ExcelDocument`: đầu trang CFG-M11-11, người xuất, "tính đến", khoảng, bộ lọc, chế độ chi phí với báo cáo chi phí, mục đích) → `FileApi.upload` và `attach(fileId, "M14.REPORT_EXPORT", fileId)` → `AuditLogApi.write` `REPORT_EXPORTED` (đối tượng `M14.REPORT_EXPORT`/mã tệp; nhóm báo cáo, khoảng, bộ lọc, phạm vi, chế độ, số dòng, dấu định danh, định dạng ở `details`; mục đích là lý do, bắt buộc khi có định danh) trong cùng transaction → trả `{fileId, auditLogId}`. Lỗi ghi nhật ký thì rollback dòng tệp (đối tượng S3 mồ côi vô hại như `FileService.upload`). Tasks ghi POI; dùng fastexcel theo research O-08 và `common.document`.
- Quyền tải (`ReportExportFilePermission`): Quản lý viện (khóa `AuditLogPermissions.VIEW` toàn viện, như 000 FR-049) hoặc người thực hiện của bản ghi nhật ký lần xuất. `GET /reports/exports` đọc nhật ký: Quản lý viện mọi lần, người khác lần của mình. Cần "Đổi api" 000 `AuditLogApi.recorded` (`AuditLogRecord`; 000 module-interfaces mục 1.2) vì M14 không có bảng và không đọc bảng của M15.
- api-016 thêm schema `ExportResult` (`fileId`, `auditLogId`) cho phản hồi 201 của xuất và `ExportRecord` (`auditLogId`, `fileId`, `exportedAt`, `exportedBy`, `role`, `group`, `from`, `to`, `includeIdentity`, `purpose`, `rowCount`) cho 200 của `GET /reports/exports` (chủ dự án duyệt 2026-10-24); trước đó hợp đồng không có nội dung phản hồi.
- CSV (`format: CSV`) bị từ chối 422 `INVALID_VALUE` vì kho tệp chưa nhận `text/csv` (`FileTypes.SYSTEM_GENERATED`); research O-08 để CSV là tùy chọn. Khóa lạ trong `filters` cũng 422 `INVALID_VALUE`.

### Dashboard, khung báo cáo và màn web (2026-10-24, nhánh `impl/016-report-screens`)

- Bố cục duyệt (DESIGN.md 5.21.0): Tổng quan thành dashboard 016 thay bản mẫu cũ; mục menu "Báo cáo" ngay dưới Tổng quan, tab theo nhóm báo cáo vai trò thấy (suy từ bảng FR-010, `REPORT_GROUPS` của web) và tab "Lần xuất".
- `MetricEvaluator` (`m14.internal.metric.service`) là đường tính duy nhất cho dashboard, báo cáo, danh sách chi tiết, xuất: nguồn lọc thô, rồi `ReportScope.covers` và bộ lọc chung. Dashboard truyền khoảng hôm nay; nguồn "lúc xem" (giấy phép) bỏ qua khoảng.
- `DashboardService`: khối theo các nhóm của bảng FR-020 (`GROUPS`, thứ tự dòng), chỉ nhóm thấy ở kênh `DASHBOARD`; phạm vi rỗng thì bỏ khối, không còn khối thì "Hiện không có phạm vi phân công"; nhóm thấy mà chưa có chỉ tiêu trả khối `values` rỗng (web ghi "Chưa có dữ liệu"). Bộ đệm 20 giây (thuộc tính kỹ thuật `eldercare.dashboard.cache`, mặc định `PT20S`, vì `TimeArchTest` cấm hằng `Duration` trong logic module) theo (nhóm, `ReportScope`), dọn mục hết hạn mỗi lần gọi; "tính đến" là lúc lấy số của khối cũ nhất. ETag băm khối và thông điệp (không gồm "tính đến"). Research O-06 ghi chạy song song bằng luồng ảo, thời hạn 2,5 giây, khối quá hạn "đang cập nhật": chưa làm vì luồng ảo mất người dùng hiện tại (phạm vi request) và transaction; hiện chạy tuần tự, khối luôn `COMPLETE`; làm khi có nguồn thật và đo SC-002.
- `GET /metrics`: chỉ tiêu của nhóm thấy ở dashboard hoặc báo cáo; không có khóa "Dashboard, báo cáo" thì danh sách rỗng (hợp đồng chỉ có 200).
- `GET /reports/{group}`: `date` là báo cáo tại một ngày (từ ngày đó tới ngày đó); khoảng chứa hôm nay thì mọi giá trị thêm nhãn `DAY_NOT_ENDED` (cả file xuất); báo cáo chi phí có ghi chú FR-050. Bộ lọc tầng trên web chờ danh mục tầng của 003.
- `GET /metric-details/{metricCode}`: không có khoảng thì như dashboard (hôm nay, phạm vi hiện hành, kênh `DASHBOARD`); 50 dòng mỗi trang; `asOf` nhận nhưng chưa dùng (nguồn chưa trả số liệu tại một thời điểm); mã không có thì 404.
- Dải "Cần xử lý ngay" (`dashboard-urgent`) chưa có trên web: cần chỉ tiêu cảnh báo, sự cố leo thang tối đa của 007.

### US8 phần 008 (2026-10-27, nhánh `impl/016-staffing-metrics`)

- "Đổi api" trên M09 (chủ dự án duyệt, E review): `StaffingRatioView` thêm `staffCount`, `totalWeight`, `shiftTemplateId`, `shiftStartsAt`, `shiftEndsAt` (đề xuất ban đầu chỉ có `shiftStartsAt`; thêm `shiftEndsAt` để biết ca đang diễn ra, ca đã kết thúc); `HandoverReportView` thêm `shiftEndsAt`; hàm mới `StaffStatisticsApi.openHandovers(scope)` (Đã lập, Có ý kiến, Bản nháp của ca Chờ bàn giao, không giới hạn ngày ca); `ExpiringCredentialView` thêm `credentialGroup`. Bản giả `FakeStaffStatistics.openHandovers` lọc theo trạng thái. Không đổi hàm cũ, không thêm migration.
- Lệch kỹ thuật trong khung: `MetricSource.current(rough, from, to)` (mặc định gọi `records`) là đường "lúc xem" của dashboard và `GET /metric-details` không khoảng (`MetricEvaluator.current`, `currentDetails`). Lý do: dashboard truyền khoảng hôm nay cho mọi nguồn nên `SHIFT.STAFFING_RATIO` không phân biệt được ca hiện tại (FR-020) với ca đã diễn ra (FR-070) dùng cùng mã. Nguồn khác giữ nguyên hành vi.
- `SHIFT.STAFFING_RATIO` (`m14.internal.staffing.service.StaffingRatioMetric`): đếm ca, chia khóa `MET`, `NOT_MET`, `NOT_CONFIGURED`. Dashboard: ca đang diễn ra (ca qua đêm từ hôm qua), không đạt theo lần tính mới nhất. Báo cáo: ca đã kết thúc có giờ bắt đầu trong khoảng (xếp tháng theo giờ bắt đầu ca), không đạt theo dấu "từng không đạt" (Q-200). Ngưỡng rỗng là `NOT_CONFIGURED`. Dòng chi tiết có tỷ lệ, ngưỡng, số nhân viên, tổng trọng số, mẫu ca; "tổng hợp theo tầng, mẫu ca" của FR-070 hiện qua bộ lọc tầng và dòng chi tiết (một khóa chia nhóm mỗi chỉ tiêu).
- `SHIFT.UNCONFIRMED_HANDOVERS` (`UnconfirmedHandoversMetric`): nguồn `openHandovers`, chia theo trạng thái, `waitingMinutes` từ giờ kết thúc ca.
- `LICENSE.STAFF` (`m14.internal.license.service.StaffLicenseMetric`): một lần gọi `expiringCredentials` với mốc lớn hơn của CFG-M06-02, CFG-M09-03 rồi lọc theo `credentialGroup`; chia theo nhóm; họ tên qua `ShiftApi.staff`. Trưởng tầng (Q-389): nhân viên có dòng `assignmentHistory(phạm vi, lúc xem, hết cửa sổ)`; `floorAreaId` của bản ghi là một tầng trong phạm vi, chỉ để lọc bước 2.
- Web: không đổi (tab "Nhân sự, ca" và khối dashboard dùng chung); khóa chia nhóm hiện nguyên mã như các chỉ tiêu khác.
- Còn chờ 015 (`XoayCaThongKeApi`): FR-071 → FR-073. T023, T024 chưa đánh `[x]`.

### Chỉ tiêu giường (2026-10-27, nhánh `impl/016-bed-metrics`)

- "Đổi api" trên M03 (chủ dự án duyệt, C review): `BedStateOnDate` thêm `roomId`, `holdReason` (suy từ sự kiện `HOLD_WAITING_RECORD`, `ABSENT_KEEP_BED` của lịch sử, không thêm cột); `AwaitingReadyInterval` thêm `startedAt`. Bản giả `FakeHandoverLocations` truyền `0L`, `null`, `awaitingSince` cho trường mới. Không đổi hàm, không migration.
- Mã con (chủ dự án duyệt; khung mỗi mã một số và một khóa chia nhóm): `BED.STATUS` (tổng giường, chia theo trạng thái, giữ chỗ tách `HELD_WAITING_RECORD`, `HELD_ABSENT_RESIDENT`), `BED.USABLE`, `BED.ISOLATED_ROOMS`, `BED.OCCUPANTS` (chia theo tầng; phòng ở dòng chi tiết) trong nhóm `BEDS`; `BED.AWAITING_READY` (trung bình phút), `BED.AWAITING_READY_MAX`, `BED.AWAITING_READY_OVERDUE` (chia theo tầng) trong nhóm `BED_AWAITING_READY`.
- Thời điểm: lúc xem là hiện tại; báo cáo là ngay trước cuối khoảng, không quá lúc xem (FR-024 tại một ngày đã qua).
- Q-390: lần Chờ sẵn sàng có lúc bắt đầu trong khoảng; đọc `awaitingReady` tới lúc xem để lần kết thúc sau cuối khoảng vẫn đủ thời gian; vượt hạn là `to` sau `deadline` của 003.
- `BED.EMPTY_AWAITING_TRANSFER` (Trống – chờ xử lý hư hỏng) khai trong `chi-tieu.md` nhưng chưa cài: dấu của 019 ở port nội bộ M03 chưa có bản cài; cài chỉ tiêu trả 0 sẽ sai nghĩa.
- Test chỉ tiêu dùng stub cục bộ của `BedStatisticsApi` trong lớp test (bản giả chung không có lý do giữ chỗ, phòng cách ly); `FakeBedAllocations` cho số người.

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Feature không có bảng. Số liệu đọc qua API của module như trước, không đọc bảng chung trực tiếp; API module giữ nguyên nên báo cáo và bảng truy vết số liệu không đổi.
