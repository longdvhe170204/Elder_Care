# Implementation Plan: Tài sản, lịch xe và mua sắm của viện

> **Sửa 2026-10-29 (Q-392):** hệ thống không quản lý kho nguyên liệu. Bỏ gói `nutrition.kho`, `nutrition.api.KhoThongKeApi`, port `TonKhoNguyenLieuPort`, `LichBuaApi` cho 019, bảng kho, DBR-31, BR-M08-17 → 20, CFG-M08-08, 09 và job kho. Các dòng còn nhắc kho trong plan này (Constitution Check, NFR, Complexity) không còn hiệu lực; mua nguyên liệu chỉ qua yêu cầu mua sắm, Kế toán so chứng từ với số tiền thực tế.

**Branch**: `019-inventory-assets` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/019-inventory-assets/spec.md`

## Summary

Feature 019 gồm ba phần thuộc hai module (UC-89, UC-90, UC-94, UC-95):
1. *(Bỏ theo Q-392)* Kho nguyên liệu.
2. Yêu cầu mua sắm của bộ phận: lập, Quản lý viện duyệt chi phí, ghi đã mua, Kế toán đối chiếu, xử lý lệch; gắn phiếu nhập, tài sản, bảo trì thuê ngoài (M11; BR-M11-17 → 20, DBR-36, Q-284).
   *(Bổ sung 2026-10-11, FR-008d, Q-337)* Loại `THUOC` cho thuốc do viện cung cấp: Điều dưỡng lập được, dòng gắn đơn nguồn viện của người cao tuổi (đọc `medication.api`); 019 cài `YeuCauThuocCungUngPort` cho 006 ghi lần tiếp nhận; không có kho thuốc (research W-02, W-05).
3. Tài sản theo bộ phận: trạng thái, vị trí, bảo trì, kiểm định, đăng kiểm, bảo hiểm, báo hỏng; giường là tài sản đồng bộ với trạng thái giường (M03; BR-M03-15, 17, 18, DBR-32).
4. Lịch xe, điều kiện "có lịch xe" cho chuyến đi (M03; BR-M03-16).

Hướng kỹ thuật:
- Gói `billing.muasam`, `roombedallocation.taisan`, `roombedallocation.lichxe`, dùng các port nội bộ 003, 010 đã khai (research W-01, W-02).
- M03 không phụ thuộc M11 (vì M11 → M02 → M03): `YeuCauMuaSamPort` khai ở `roombedallocation.api`, M11 cài.
- Lịch xe cung cấp `roombedallocation.api.LichXeApi`, `LichXeLenhApi` cho 014 gọi; sửa hợp đồng 014 (bỏ `LichXeChuyenPort` ở `care.api`) để tránh vòng M03 ↔ M04.
- Trạng thái giường ↔ tài sản đổi cùng transaction qua port 003 (W-03, W-06).
- Thanh lý tài sản là loại yêu cầu phê duyệt của 000.

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 019 không tự trừ kho, không tạo yêu cầu mua sắm, phiếu, lịch xe; tài sản giường chỉ tạo kèm lệnh Tạo giường của 003;
- hệ thống chỉ: chuyển lịch xe kèm lệnh của 014; gắn dấu quá hạn theo ngày; kiểm tra và chặn; nhắc, thông báo (gồm Khẩn cấp cho báo hỏng giường có người mức mất an toàn).

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX) (000 R-01). Không có màn trên app di động ở giai đoạn này (kho, tài sản trên web; Trưởng tầng báo hỏng qua web).

**Primary Dependencies**: Như 000 → 018. Không thêm thư viện.

**Storage**: MySQL 8.0, schema M03, M08, M11. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test bảng cho năm hàm thuần (research W-09); bộ 200 phiếu (SC-001).
- Test tích hợp MySQL cho `CHECK ton >= 0`, `UNIQUE` (lô, nguồn gắn, giường ↔ tài sản, vị trí mở, kiểm kê chờ), trigger nhóm 3.
- Test đồng thời: hai phiếu xuất chỗ cuối; hai lịch xe cùng giờ; báo hỏng cùng lúc với phân bổ.
- Đồng hồ giả lập cho CFG-M03-02, 09, CFG-M08-08, 09, CFG-M11-07 → 09.

**Target Platform**: Web.

**Project Type**: Web service (REST `/api/v1`) và web client.

**Performance Goals**: Phiếu xuất 10 dòng lưu ≤ 1 giây (SC-006 ≤ 3 phút thao tác); job hằng ngày mỗi phần ≤ 1 phút.

**Constraints**:
- Mỗi lệnh một endpoint (constitution XI); phiếu kho, lịch sử tài sản nhóm 3, không sửa (constitution XII).
- Quản lý viện không làm lệnh kho, không làm thay Phụ trách bộ phận; Kế toán không lập yêu cầu mua sắm (FR-020).
- Không ghi ngoại tuyến.

**Scale/Scope**:
- 4 thực thể có vòng đời (yêu cầu mua sắm, tài sản, lịch xe, thanh lý qua 000); 10 bảng (bỏ 7 bảng kho theo Q-392).
- 3 job (mua sắm, tài sản hằng ngày; lịch xe theo phút).
- 14 đường dẫn, 21 thao tác REST ([contracts/api-019.yaml](contracts/api-019.yaml)).
- 2 port nội bộ cài, 3 dùng; 1 SPI mới ở `roombedallocation.api` (M11 cài); 2 API mới cho 014; 3 giao diện thống kê cho 016.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | x-fr, x-br; data-model mục 6 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi |
| III. Không mặc định CRUD | Đạt | Phiếu không sửa; yêu cầu, tài sản, lịch xe theo bảng trạng thái |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển data-model mục 2 → 5 khớp năm bảng của spec |
| V. Không cố định giá trị | Đạt | CFG-M03-02, 09, CFG-M08-08, 09, CFG-M11-07 → 09 |
| VI. Vai trò, quyền | Đạt | Phụ trách bộ phận theo nhiệm vụ (002 FR-019f); bảng W-08 |
| VII. Điểm chưa rõ | Đạt | Điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-007 ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; hợp đồng 014 được sửa (mục "Đồng bộ") |
| X. Kiến trúc | Đạt | Gói trong ba module theo mục nguồn; không có vòng (W-02) |
| XI. Nghiệp vụ ở backend | Đạt | Tồn, kiểm lý do chọn lô (FR-003), vượt, khớp chứng từ, lịch xe ở backend |
| XII. Dữ liệu | Đạt | DBR-31, 32, 36 ở data-model mục 6 |
| XIII. Tham số | Đạt | Như V |
| XIV. Ngoại tuyến, thông báo | Đạt | Không ngoại tuyến; thông báo FR-021 |
| XV. Truy vết, kiểm thử | Đạt | Test theo BR-M03-15 → 18, BR-M08-17 → 20, BR-M11-17 → 20 |

**Kiểm lại sau Phase 1**: Đạt.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 019 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | Kho bếp và tài sản của một viện; vài trăm phiếu mỗi tháng |
| NFR-02 Hiệu năng | Có | Phiếu xuất 10 dòng lưu ≤ 1 giây; khóa theo lô, theo xe |
| NFR-03 Thông báo khẩn cấp | Có | Báo hỏng giường có người ở mức "mất an toàn" gửi mức Khẩn cấp qua 009 (FR-021) |
| NFR-04 Tác vụ định kỳ | Có | 4 job (kho, mua sắm, tài sản hằng ngày; lịch xe theo phút), mỗi phần ≤ 1 phút; khóa nhắc chống trùng; chạy bù |
| NFR-05 Khả dụng | Gián tiếp | Listener idempotent; lệnh kèm theo lệnh của 014 trong cùng transaction |
| NFR-06 Sao lưu | Có | Phiếu, lịch sử tài sản ở MySQL; chứng từ ở kho tệp |
| NFR-07 Lưu trữ | Có | Phiếu kho, chứng từ mua sắm theo CFG-M15-03, CFG-M15-04 |
| NFR-08 Bảo mật | Có | Quyền theo bộ phận và nhiệm vụ Phụ trách bộ phận (FR-020), kiểm ở backend |
| NFR-09 Thời gian | Có | `Clock`; hạn dùng, lịch xe theo Asia/Ho_Chi_Minh |
| NFR-10 Thiết bị | Có | Màn trên web; chưa có màn trên app di động ở giai đoạn này |
| NFR-11 Ngôn ngữ | Có | Nhãn tiếng Việt; ngày dd/MM/yyyy, tiền VND |
| NFR-12 Cấu hình | Có | CFG-M03-02, 09, CFG-M08-08, 09, CFG-M11-07 → 09 đọc qua `ThamSoApi` |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập cho hạn dùng, nhắc, lịch xe |

## Project Structure

### Documentation (this feature)

```text
specs/019-inventory-assets/
├── plan.md  ├── research.md (W-01 → W-09)  ├── data-model.md  ├── quickstart.md (Q1 → Q33)
├── contracts/ (api-019.yaml, module-interfaces.md)
└── tasks.md   # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai; giai đoạn thiết kế không tạo (CLAUDE.md).

```text
backend/src/main/java/com/eldercare/backend/
├── billing/muasam/     {internal, web, lich}   # yêu cầu mua sắm, gắn nguồn, KiemTraVuot, KhopChungTu, YeuCauMuaSamPort (cài)
├── billing/api/        # (bổ sung) YeuCauMuaSamApi, MuaSamThongKeApi
├── roombedallocation/taisan/     {internal, web, lich}   # tài sản, lịch sử, AnhXaTrangThaiGiuong, TaiSanGiuongPort, thanh lý
├── roombedallocation/lichxe/     {internal, web, lich}   # lịch xe, KiemTraDatLich, LichXeApi, LichXeLenhApi
└── roombedallocation/api/        # (bổ sung) YeuCauMuaSamPort (SPI), LichXeApi, LichXeLenhApi, TaiSanThongKeApi

frontend/src/tinh-nang/mua-sam/  frontend/src/tinh-nang/tai-san/  frontend/src/tinh-nang/lich-xe/
```

**Structure Decision**: Theo R-02 của 000, 003 G-01, 010 P-01, 011 M-01.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| SPI `YeuCauMuaSamPort` ở `roombedallocation.api` | Tài sản (M03) gắn yêu cầu mua sắm (M11) trong cùng lệnh; M11 phụ thuộc gián tiếp M03 | M03 gọi M11: vòng; sự kiện sau commit: mất "chặn khi thiếu yêu cầu" trong lệnh (FR-008c) |
| Lịch xe ở `roombedallocation.api` thay cho SPI ở `care.api` | M03 cài SPI của M04 tạo vòng | Giữ SPI ở `care.api`: Modulith `verify()` từ chối |
| `mua_sam_gan_nguon` lưu số tiền nguồn | Kế toán đối chiếu tổng phiếu nhập, nguyên giá mà M11 không đọc được M08 (vòng) | M11 đọc M08: vòng *(R-15: `mua_sam_gan_nguon` → cột `JSON` của yêu cầu mua sắm)* |

## Đồng bộ với hợp đồng của feature khác

1. **014** ([../014-activity-care-quality/contracts/module-interfaces.md](../014-activity-care-quality/contracts/module-interfaces.md) mục 1.2; research L-02, L-07, L-08; plan; quickstart): bỏ `LichXeChuyenPort` ở `care.api`; 014 gọi `roombedallocation.api.LichXeApi` và `LichXeLenhApi.theoChuyen` (MANDATORY) trong lệnh của chuyến; `ChuyenDiThayDoi` chỉ còn cho 016.
2. **003, 011, 010**: cài `TaiSanGiuongPort` (`TonKhoNguyenLieuPort` bỏ theo Q-392), dùng `GiuongTheoTaiSanPort`, `LichBuaApi` đúng chữ ký. 010 P-12 ghi "yêu cầu mua sắm không sinh khoản; 010 không cần port cho 019" — giữ nguyên.
3. **008 / 002**: dùng `NhiemVuApi.boPhanCua`, `laPhuTrachBoPhan`, `phuTrachBoPhan` (đã khai). **009**: thông báo; **016** đọc ba giao diện thống kê.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX).

1. **"Hệ thống MUST đề xuất lô có hạn dùng sớm nhất"** (FR-003, BR-M08-17) so với Q-307 (bỏ mọi gợi ý, đề xuất của hệ thống): spec 019 chưa được rà theo Q-307 ở điểm này. Plan làm theo FR-003 (chọn sẵn lô hạn sớm nhất, đổi lô cần lý do) vì đây là quy tắc xuất kho (FEFO) chứ không phải gợi ý nội dung nghiệp vụ. Đề nghị chủ dự án xác nhận và ghi ngoại lệ vào dòng Q-307 ở 24.2, hoặc đổi FR-003 thành "chặn xuất lô có hạn muộn hơn khi còn lô hạn sớm hơn trừ khi có lý do". *(Đã xử lý 2026-10-10: FR-003 sửa theo cách thứ hai; hệ thống hiển thị lô theo hạn dùng, không chọn sẵn; `GoiYLo` đổi thành `ThuTuLoTheoHan`.)*

2. **Thanh lý tài sản không có bảng trạng thái riêng** (FR-013 ghi "Phụ trách bộ phận lập yêu cầu, Quản lý viện duyệt"): plan dùng yêu cầu phê duyệt chung của 000 (`M03.THANH_LY_TAI_SAN`). Đề nghị ghi rõ ở 7.7 rằng thanh lý đi qua vòng đời yêu cầu phê duyệt chung (6.6). *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Trưởng tầng báo hỏng tài sản "trong tầng mình"** (FR-013, FR-020): tài sản có vị trí tầng, phòng; tài sản ở "kho tài sản", "bãi xe" không thuộc tầng nào nên chỉ Phụ trách bộ phận báo hỏng được. Plan hiểu như vậy; đề nghị xác nhận ở Phụ lục 27 ³³. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

## Ghi chú triển khai *(2026-11-06)*

- Dịch `contracts/api-019.yaml` theo glossary mục 25 (chủ dự án duyệt 2026-11-06); không thêm bớt endpoint, trường. Thân lệnh lịch xe và thân loại tài sản trước viết thẳng trong operation, nay đặt tên `VehicleScheduleCommand`, `AssetTypeWrite` (cùng trường).
- Gói (T001): `billing.muasam` → `billing.internal.procurement` (tách khỏi `billing.internal.purchase` của đề nghị mua hộ 010); `roombedallocation.taisan`, `lichxe` → `roombedallocation.internal.asset`, `roombedallocation.internal.vehicle`; job ở `scheduling` của từng module. Port giường của 003: `BedAssetCommandPort` (dùng), `BedAssetPort` (cài).
- SPI của 006 cho dòng thuốc đã ghi đã mua là `medication.api.spi.ProcurementMedicationPort.lineRecordedPurchased(lineId)` (tra theo id dòng), khác mô tả "trả dòng của yêu cầu" ở contracts mục 3; dòng nằm trong cột `JSON` (R-15) nên phải có id ổn định. Cách 006 lấy id dòng ghi ở team-plan mục 8.
- `BedAssetCommandPort` (003) chưa có bản cài; US5 cần bản cài của 003 trước khi nối thật.
- T003, T004 (chủ dự án duyệt 2026-11-06): không làm khung rỗng ở Phase 2. Job và provider cần bảng nghiệp vụ, nên tạo cùng story có bảng: mua sắm (Phase 3), tài sản (Phase 7), lịch xe (Phase 9). Bảng `nhac_019` gộp vào `reminder_marker` nên không có migration `V019_01`.
- Phase 3 lượt 1 (2026-11-06), cách hiểu đã cài, cần chủ dự án xem khi review:
  - Quyền: chú thích ³⁷, ³⁸, ⁴⁴ chỉ mô tả ở lớp vai trò của 002 (`FootnoteRules`), nên 019 kiểm trong lệnh: Phụ trách bộ phận của đúng bộ phận (`DutyApi.departmentLead`) cho lập, sửa, gửi, hủy Đã duyệt, ghi đã mua; người lập cho sửa, gửi, rút lại, hủy Nháp; vai trò Kế toán và khác người ghi đã mua cho ba lệnh đối chiếu; Quản lý viện (`APPROVE`) cho duyệt, từ chối, xử lý lệch. Phụ trách bộ phận khác người dùng: 403 `NOT_DEPARTMENT_LEAD`; bộ phận chưa có Phụ trách: 409 `DEPARTMENT_LEAD_MISSING`, nhắc Quản lý viện trong transaction riêng (Q-292).
  - Danh sách: Quản lý viện, Kế toán thấy mọi yêu cầu; Phụ trách bộ phận thấy yêu cầu của bộ phận mình; người khác thấy yêu cầu do mình lập ("bộ phận của tôi" của api-019).
  - Thành tiền dòng = số lượng × đơn giá dự kiến, làm tròn HALF_UP tới đồng; tổng dự kiến là tổng thành tiền. Duyệt chỉ giảm ("có thể giảm"), số lớn hơn tổng dự kiến trả 422.
  - Trả lại đối chiếu xóa số thực tế, ngày mua, nhà cung cấp, chứng từ của lần ghi trước để ghi lại; tệp chứng từ đã gắn vẫn thuộc yêu cầu.
  - Xác nhận khớp: hệ thống chỉ so được tổng nguyên giá với số thực tế ở loại tài sản (FR-008b); số trên chứng từ do Kế toán so bằng mắt, hợp đồng không có trường số tiền chứng từ.
  - Xử lý lệch báo người lập và Kế toán đã ghi lệch (`reconciled_by_staff_id`).
- Phase 3 lượt 2 (2026-11-06): nhắc CFG-M11-08, 09 (`ProcurementReminders`, job `REMIND_PROCUREMENT_REQUESTS` 07:00, một lần mỗi lần vào trạng thái, dấu ở `reminder_marker`; tên job chưa thêm vào enum `runJobs` của api-000, như hai job của 003); loại MEDICATION (Điều dưỡng của bộ phận lập, gửi, rút lại, hủy; lệnh lập tự kiểm quyền theo từng người cao tuổi của dòng để người ngoài phạm vi nhận 403, ghi ở `RequiresPermissionArchTest.SELF_CHECKED`; danh sách tự kiểm quyền xem theo vai trò); provider M11 (phạm vi, lưu giữ không gắn hồ sơ tính từ `locked_at`, quyền tải chứng từ).
- Lệch research W-02 (chủ dự án duyệt 2026-11-06): M11 không phụ thuộc M07 vì vòng `billing → medication → care → billing`. Kiểm đơn qua `billing.api.spi.ProcurementPrescriptionPort` (M07 cài); T027 đổi thành M11 cung cấp dòng thuốc đã ghi đã mua ở `billing.api` và khai `ProcurementIntakePort` cho "Đã nhận" (contracts mục cuối module-interfaces). Lỗi đơn không hợp lệ dùng 409 `CONDITION_NOT_MET` sẵn có (điều kiện `PRESCRIPTION_NOT_FACILITY_UNENDED`).
- T027 (2026-11-10, theo quyết định chủ dự án 2026-11-08, team-plan mục 8): (1) bỏ `ProcurementPrescriptionPort`, M11 đọc `medication.api.PrescriptionApi.unendedFacilityPrescriptions`; `ModuleBoundaryTest`, `LayerArchTest` xanh sau khi đổi (vòng `billing → medication → care → billing` của 06/11 không còn); (2) `billing.api.ProcurementMedicationLineApi` cài ở `ProcurementMedicationLines`; (3) khai `billing.api.spi.ProcurementIntakePort`, `received` tính từ port, thiếu bean thì `null` (không 503 cho cả danh sách); (4) id dòng tính `requestId × 1.000.000 + thứ tự` (`ProcurementLineId`), không lưu vào cột `lines` nên không có migration; thêm giới hạn 999.999 dòng mỗi yêu cầu (quá thì 422) để id không trùng. Lệch nhỏ: team-plan ghi trả id ở `GET /procurement-requests`, `/{id}`, nhưng api-019 chỉ có `GET /procurement-requests` nên id và `received` trả ở danh sách. Chi tiết ở module-interfaces mục "Đảo chiều".
- Còn lại của Phase 3: T027 (chờ id dòng và D đồng ý đảo chiều, team-plan mục 8), màn web T008 (chủ dự án hoãn 2026-11-06, làm sau khi thống nhất thiết kế M35 với A; khi làm: thư mục theo frontend/README là `features/inventory`, khác glossary 25.1 ghi ba thư mục, sửa glossary cùng lúc; api-019 không có `allowedCommands` và tên bộ phận, cần chốt cách hiện nút lệnh và cột bộ phận).
- Phase 7 lượt 1 (2026-11-06, chủ dự án duyệt): (1) api-019 không có nhóm loại tài sản, nên V019_05 nạp hai loại hệ thống mã `BED` (Giường), `VEHICLE` (Xe) trong `M03.ASSET_TYPE`, đổi tên được, không ngừng được; trường xe (biển số, số chỗ, hạn đăng kiểm, bảo hiểm) chỉ cho loại Xe. (2) Ghi kết quả bảo trì theo spec FR-013 (chỉ từ Đang bảo trì; Không đạt → Hỏng), khác nghiep-vu 7.7 ("Đang bảo trì, Hỏng" → Sẵn sàng), ghi ở team-plan mục 8. (3) Lượt 1 gồm ghi tăng, sửa mô tả, đưa vào sử dụng, chuyển vị trí, báo hỏng, bảo trì và kết quả, chuyển bộ phận, danh sách, danh mục loại; lệnh trên tài sản giường trả 503 tới Phase 8. Tầng của tài sản (`floor_area_id`) lấy từ phòng, tầng của vị trí; khu vực, bãi xe, kho tài sản không thuộc tầng nên chỉ Phụ trách bộ phận báo hỏng (Q-313).
- Phase 7 lượt 2 (chủ dự án chốt 2026-11-06): (a) thêm `maintenanceKind` (MAINTENANCE, INSPECTION, VEHICLE_INSPECTION, INSURANCE; mặc định MAINTENANCE) và `nextDueOn` vào `AssetCommand` của api-019; vòng Đưa vào bảo trì → Ghi kết quả dùng cho cả bốn loại lần, giữ lần nhóm 3 như data-model; Đạt thì đặt hạn tương ứng (bảo trì, kiểm định: ngày + chu kỳ; đăng kiểm, bảo hiểm: `nextDueOn` bắt buộc) và gỡ dấu; (b) `GET /assets/{id}` hoãn tới màn web M35, giữ chờ trong `ApiContractIT`; (c) phạm vi: thanh lý qua yêu cầu phê duyệt `M03.ASSET_DISPOSAL` (không tự duyệt), job 06:00 nhắc trước hạn CFG-M03-09 và gắn dấu, nhắc mỗi ngày khi quá hạn (`BR_M03_17_MaintenanceDueIT`), provider `M03.ASSET` (phạm vi, lưu giữ, phân loại trường nguyên giá, chi phí → tài chính); đính chính `M03.ASSET_HISTORY` để Phase 10.
- Phase 8 lượt 1 (US5, chủ dự án chọn 2026-11-06): (1) `BedAssetCommandPort.residentTransferFlagAdded(bedId)` bỏ tham số người bị ảnh hưởng, 003 tự tra phân bổ tương lai và báo Hành chính, Trưởng tầng (module-interfaces 003 mục 3 sửa); `applyAssetCommand` chỉ gọi sau `checkAssetCommand` không lỗi trong cùng transaction (cài: `BedAssetCommands` ở `roombedallocation.internal.bed.service`, `BedAssetStateService` tách `check`). (2) `link-bed-preparation-damage`: 003 không còn bản ghi hư hỏng khi dọn giường (Q-282), nên lệnh ghi hư hỏng nguồn `BED_PREPARATION` rồi làm như Báo hỏng với ngoại lệ FR-013a; `damageId` của api-019 không dùng. (3) Mã tài sản giường `BED-{bedId}`, tên theo giường; ghi tăng kèm lệnh Tạo giường (`BedAssetPort.recordIncreaseForBed`) trả 503 tới khi chủ dự án chốt cách lấy id bộ phận Chăm sóc (008 không có mã, không có API tra), kèm tạo bù tài sản cho giường đã có (team-plan mục 8). (4) Cách hiểu đã cài: báo hỏng giường có người là khi 003 trả điều kiện BR-M03-05 (Đang sử dụng, Đang giữ chỗ); hư hỏng mang dấu là bản nháp ở `record_event` tới khi gỡ dấu (lý do, nội dung đã sửa, người sửa đặt một lần); Quản lý viện báo hỏng lại, đưa vào bảo trì được khi tài sản mang dấu, gỡ dấu được (FR-015); mức mất an toàn đọc `suitableEmptyBedsForOccupant` (giường Trống toàn viện đạt FR-018 qua `AllocationChecker`), tầng không còn giường phù hợp thì thêm Quản lý viện, Hành chính và nội dung kèm giường ở tầng khác (Q-256); vì kiểm FR-018 cần nguồn cách ly của 007, mức mất an toàn trả 503 khi 007 chưa cài `AllocationInfectionStatus`. Chuyển vị trí giường (Q-243, team-plan "cổng đồng bộ vị trí") vẫn 503. Còn lượt 2: nhắc mỗi CFG-M03-02 (chuyển người khi giường còn người, báo hỏng lại khi giường mang dấu về Trống) và đọc số giường Trống mang dấu cho 016.
- Phase 8 lượt 2 (chủ dự án duyệt 2026-11-06): thêm `BedAssetCommandPort.bedState(bedId)` (003 cài) cho job nhắc, `BedAssetPort.flaggedBedsAt(bedIds, at)` (019 cài, dấu theo lúc gắn và lúc gỡ của hư hỏng mang dấu) và `BedStatusApi.countEmptyAwaitingTransfer(scope, at)` cho 016 (team-plan L134); module-interfaces 003 mục 1.8, 3 và glossary 9 sửa. Nhắc CFG-M03-02 trong job `REMIND_ASSET_DUE_DATES` (06:00, nên chu kỳ ngắn hơn một ngày vẫn nhắc tối đa mỗi ngày một lần): giường còn người (Đang sử dụng, Đang giữ chỗ) nhắc Trưởng tầng chuyển người từ chu kỳ thứ nhất sau lúc gắn dấu (lúc gắn đã báo ngay); giường mang dấu về Trống nhắc báo hỏng lại ngay lần chạy đầu sau khi về Trống (mốc `emptyPeriodStarted`), rồi mỗi chu kỳ; trạng thái khác không nhắc; một lần mỗi chu kỳ (dấu `reminder_marker`).
- Phase 9 (US6, chủ dự án chốt 2026-11-06): (1) api-019 thêm schema `VehicleSchedule` cho `GET /vehicle-schedules` (trước chỉ ghi "OK"); (2) `hasBookedSchedule` đúng khi lịch của chuyến Đã đặt hoặc Đang dùng và [giờ đi, giờ về) chứa giờ rời dự kiến (014 gọi ở mọi lượt điểm danh rời viện); (3) tối đa một lịch chưa hủy mỗi chuyến (khóa duy nhất `TRIP:<id>`, nhả khi hủy; đặt thêm trả 409 `STATE_CHANGED`); (4) quá hạn đăng kiểm, bảo hiểm xét theo ngày đi (đang mang dấu hoặc hạn trước ngày đi); Hỏng, Đang bảo trì, Đã thanh lý xét theo hiện tại. Cách hiểu đã cài: lịch mục đích `TRIP` bắt buộc `tripId`, mục đích khác không gắn chuyến; M03 không kiểm chuyến tồn tại (care phụ thuộc M03, không đọc ngược); người lái chỉ kiểm có id, báo qua `RecipientGroup.Staff` (009 chỉ gửi khi có tài khoản, Q-298); đặt lịch, dời giờ khóa dòng tài sản xe rồi đọc lịch của xe bằng câu đọc có khóa (đọc thường ở REPEATABLE READ không thấy lịch vừa ghi của transaction trước, `SC_005` đỏ trước khi sửa); lệnh tự kiểm quyền vì lịch xe toàn viện không gắn tầng (`RequiresPermissionArchTest.SELF_CHECKED`); chuyến không có lịch thì `followTrip` bỏ qua, lịch không đúng trạng thái thì 409. Job `REMIND_VEHICLE_SCHEDULES` mỗi phút: lịch Đang dùng quá giờ về nhắc người đặt một lần cho mỗi giờ về; lịch Đã đặt kế tiếp của cùng xe đã tới giờ đi thì báo Quản lý viện. Đính chính `M03.VEHICLE_SCHEDULE` và màn web lịch xe để Phase 10 và M35.
- Phase 7 lượt 2 đã cài (2026-11-06): nhắc trước hạn một lần mỗi hạn, quá hạn gắn dấu và nhắc mỗi ngày (dấu ở `reminder_marker` loại `M03.ASSET_REMINDER`), gỡ dấu chỉ khi ghi lần mới Đạt; đăng kiểm, bảo hiểm chỉ cho loại Xe; yêu cầu thanh lý dùng ngày hiệu lực hôm nay (áp dụng khi duyệt), chặn khi đang có yêu cầu Nháp hoặc Chờ duyệt. Bước áp dụng của 000 chạy ở transaction riêng nên IT gọi `AssetDisposalHandler.apply` trong transaction của test; luồng duyệt → áp dụng đầy đủ chưa kiểm bằng IT không transaction.

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Ánh xạ bảng ở data-model mục 0; task T029. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.

### Phase 10 — rà và kiểm phần backend (2026-11-06)

- T019/DBR-32: thêm `DBR_32_BedAssetConcurrencyIT`, hai transaction không bọc transaction test, dùng `BedAssetPort` thật. Spy chỉ điều phối sau lần đọc phân bổ tương lai đầu tiên, để transaction phân bổ commit trước khi transaction báo hỏng lấy khóa giường. Đã thấy đỏ đúng `CannotAcquireLockException` (danh sách phân bổ đổi giữa hai lần đọc); `reportDamage` thêm retry toàn transaction theo mẫu `AllocationCommands.create`. Không đổi chữ ký hay nghiệp vụ. Đã đỏ có điều phối trước khi sửa, sau sửa nhóm DBR-32 + ba lớp IT chuyến 014 xanh 50/50; kiểm cuối toàn bộ ghi bổ sung sau khi chạy đủ.
- IT thanh lý thật: `FR_013_AssetDisposalTransactionIT` (NOT_SUPPORTED) gọi endpoint lập yêu cầu, duyệt; xác nhận APPLIED, result trên `approval_request` theo R-15, disposed_at và trạng thái giường RETIRED. Không gọi trực tiếp handler. Chạy riêng xanh 2/2.
- T024 phần lịch xe: ba lớp `BR_28_RoiVienIT`, `BR_39_ThieuNguoiIT`, `FR_031a_DinhChinhChuyenIT` bỏ `FakeVehicleSchedules.Config`; ca FR-019 đặt lịch qua endpoint thật và kiểm Đang dùng khi người đầu tiên rời viện. Ca kiểm rollback dùng spy API lệnh để chủ động gây lỗi nguồn, nguồn đọc và lịch đã đặt vẫn là thật. Giữ fake của các test cô lập 003 dùng để đặt điều kiện riêng; `BR_54_BedAssetIT` và DBR-32 kiểm cổng giường thật. Phần thuốc của T024 vẫn chờ T027/T028.
- T029: rà `V019_02`, `V019_05`, `V019_07`: chỉ hai bảng nghiệp vụ `procurement_request` (MULTIPLE, khóa nội dung ngoài Nháp, row_lock/no_delete khi chốt) và `asset` (BUSINESS, nguồn/giường đặt một lần, thanh lý là cuối); tiền DECIMAL(15,0). Loại tài sản dùng catalog_item; vị trí/bộ phận dùng status_history; bảo trì/hư hỏng dùng record_event; lịch xe dùng simple_record; mốc nhắc dùng reminder_marker. Các dòng con mang entity_type M03.ASSET theo RetentionProvider của cha. Ba job 019 dùng SchedulerRuns. `RecordGroupTriggerIT` đã có kiểm tổng quát khai nhóm, trigger RECORD và MULTIPLE; không sửa test của 000, chạy lại để kiểm chứng.
- Chờ chốt trước khi cài: trường, kiểu và tác động đính chính M03.ASSET_HISTORY/M03.VEHICLE_SCHEDULE (000 mục 1.6 chỉ quy định cơ chế); người dùng xác nhận giữ chờ chủ dự án/A. Đã thêm dòng team-plan mục 8. Ba tên job đề xuất A bổ sung vào enum runJobs, không tự sửa api-000. *(Đã chốt 2026-11-07, xem mục "Đính chính tài sản và lịch xe" ngay sau bảng truy vết.)*

| Truy vết T026 | Test hiện có / bổ sung |
| --- | --- |
| DBR-32 | DBR_32_BedAssetConcurrencyIT; BR_54_BedAssetIT; FR_013_AssetDisposalTransactionIT |
| DBR-36 | DBR_36_SourceLinkIT; FR_013_AssetLifecycleIT (mua mới, bảo trì thuê ngoài) |
| BR-M03-15, BR-M03-18 | BedAssetRulesTest; BR_54_BedAssetIT; FR_013_AssetLifecycleIT |
| BR-M03-16 | VehicleBookingCheckTest; BR_54_VehicleScheduleIT (SC-005) |
| BR-M03-17 | BR_M03_17_MaintenanceDueIT |
| BR-M11-17 → 20 | BR_64_ProcurementRequestIT; BR_64_OverrunIT; BR_64_ReconciliationIT; BR_64_ReminderIT |
| FR-008d | FR_008d_MedicationRequestIT (quyền, đơn nguồn viện, thiếu trường, phạm vi); adapter dòng/Đã nhận vẫn chờ T027/T028 |
| FR-019 | FR_018_FollowTripIT; BR_28_RoiVienIT.FR_019_firstDepartureStartsTheRealVehicleSchedule |
| FR-020 | FR_013_AssetLifecycleIT; BR_54_BedAssetIT; BR_54_VehicleScheduleIT; ba lớp BR_M11_17/18/19; FR_008d_MedicationRequestIT |
| FR-021 | BR_54_BedAssetIT; BR_54_VehicleScheduleIT; BR_M03_17_MaintenanceDueIT; BR_64_ProcurementRequestIT; BR_64_ReconciliationIT; BR_64_ReminderIT |

| T017 (data-model 0.1) | T017_AssetHistoryConstraintsIT (một khoảng vị trí mở, khoảng đóng và bảo trì đã ghi không sửa/xóa); RecordGroupTriggerIT |
| T007 (đối chiếu THUOC) | FR_008d_MedicationRequestIT.T007_medicationRequestIsReconciledAgainstTheReceiptOnlyAndNotByTheRecorder |

Đây là truy vết test, chưa phải tuyên bố hoàn tất các task có phần phụ thuộc còn mở hoặc SC/UI chưa đo.

- T023 phần tài sản (chủ dự án xác nhận trong phiên 2026-11-06): ngày on lọc dấu hiện tại có dueOn < on, không tái dựng dấu quá khứ; nếu cần lịch sử phải chốt nguồn trước. `AssetStatisticsService` cài `AssetStatisticsApi`, `AssetStatisticsStore` lọc theo tầng, countOverdue GROUP BY tại DB; vehicleSchedules đọc qua SimpleRecordApi (bốn trạng thái), lọc ngày đi dự kiến theo BusinessTime.ZONE, hai đầu bao gồm. `T023_AssetStatisticsIT` đỏ 3/3 do thiếu provider trước khi cài. T023 phần mua sắm vẫn chờ A xác nhận khoảng của CP.MUA_SAM.

- Kiểm cuối Phase 10 (2026-11-06): `./mvnw -o -q spotless:apply`; `./mvnw -o clean verify -Dit.forkCount=3`: BUILD FAILURE do StaffCommandsHttpIT của E (409 INVALID_DEPARTMENT, khớp dòng chờ mục 8) (1131 đơn vị/kiến trúc xanh; 2799/2802 IT xanh, 1 thất bại, 2 bỏ qua, 15 phút 25 giây); IT các module liên quan đều xanh: roombedallocation 305, care 383, admission 443, reporting 53, medication 39, billing 41. `T023_AssetStatisticsIT` 4/4 (đủ bốn trạng thái lịch và giờ thực tế), DBR-32 1/1, IT thanh lý thật 2/2; `npm test` 512/512 (86 file). Hai test bỏ qua sẵn có là SC_001_UrgentConcurrencyIT của 009, yêu cầu property eldercare.perf; không thay cấu hình bỏ qua test. Log ở scratchpad phiên, không ở backend/logs.
- Đối chiếu mục 8: chỉ StaffCommandsHttpIT.FR_001a_departmentCatalogueAndChangeEndpointWorkWithoutEditingProfile đỏ (mong 200, nhận 409 INVALID_DEPARTMENT, “Quản lý viện không thuộc bộ phận”, staff_id 5005013); fixture vẫn tạo tài khoản quản lý trước INSERT staff_profile tự tăng, khớp dòng E đang chờ. Không sửa test/fixture của E. ResidentContractIT, BR_18_ShiftTaskPlanningIT, BR_19_OverdueReminderIT xanh trong lượt này; không tự xóa các dòng baseline trên develop. Reviewer độc lập không có finding actionable; giới hạn kiểm: DBR-32 ép chiều phân bổ commit trước khóa giường, IT thanh lý mới kiểm đường thành công (chưa chèn lỗi sau bước đổi trạng thái giường).
- T019, T029 đã đủ bằng chứng để đánh dấu; T023/T024 còn phần mua sắm thuốc/thống kê mua sắm, T025 còn kịch bản/đo SC đầy đủ, T026 còn kiểm phần adapter FR-008d chưa chốt nên giữ mở. Chưa commit, push hay mở PR. Bản nháp PR lưu scratchpad; nhánh dựa trên 014 nên cần so lại phạm vi khi người dùng cho phép mở PR vào develop.

- Rà 2026-11-07 (lượt tiếp 019): (1) `FR_018_FollowTripIT.FR_018_inUseScheduleOverdue…` đỏ nhận 2 lần nhắc: `BR_28_RoiVienIT.FR_019_…` commit lịch xe Đang dùng có giờ về trước đồng hồ 2099 của ca, job `VehicleScheduleReminders` quét mọi lịch Đang dùng nên nhắc thêm; tái hiện đỏ bằng chạy hai lớp cùng JVM, sửa cô lập trong transaction của test (đưa lịch Đang dùng có sẵn sang Đã hoàn thành, rollback theo test, như `CallReminderTestBase.isolateFromOtherTests`), không đổi mã production, không nới assertion; nhóm roombedallocation + care 776 IT xanh với `-Dit.forkCount=3`. (2) T005, T006, T007, T017 đủ bằng chứng. (3) T016 còn Q19, Q24; quy tắc đính chính đã chốt cùng ngày (mục dưới), T018, T022 chỉ còn phần hoãn tới M35; T026 giữ [ ].

### Đính chính tài sản và lịch xe *(chủ dự án chốt 2026-11-07)*

- `M03.ASSET_HISTORY` (`AssetHistoryCorrectionProvider`): chỉ lần bảo trì và hư hỏng ở `record_event` đã xác nhận (hư hỏng mang dấu còn bản nháp thì chưa đính chính được). Trường: `content`, `performedBy`, `cost` của bảo trì; `description` của hư hỏng; kiểu AMEND_CONTENT hoặc VOID. Không đổi trạng thái, vị trí, bộ phận, hạn hiện hành; không báo module khác. Người ghi gốc là mã nhân viên ở `record_event`, provider đổi sang tài khoản (`AccountApi.byStaff`) vì 000 so với mã tài khoản. Chỉ Phụ trách bộ phận quản lý tài sản (`allows`, `canRecord`). Lệch spec/glossary: lịch sử vị trí, bộ phận ở `status_history` có id trùng `record_event` nên không chung loại; chủ dự án chọn hoãn.
- `M03.VEHICLE_SCHEDULE` (`VehicleScheduleCorrectionProvider`): chỉ lịch Đã hoàn thành (đã khóa). Trường: giờ đi/về dự kiến, xuất phát/trả thực tế, người lái, ghi chú; không đổi mục đích. Đổi giờ dự kiến kiểm lại chồng giờ với lịch chưa hủy khác của cùng xe, chồng thì 409 `VEHICLE_SCHEDULE_OVERLAP` và lệnh rollback; lịch gắn chuyến đổi giờ thì phát `roombedallocation.api.events.VehicleScheduleCorrected` (chủ dự án duyệt thêm sự kiện) để 014 nghe. Ngưỡng người đính chính: vai trò đặt lịch (`canRecord`), cách A (người ghi gốc) hoặc C (lịch toàn viện không gắn tầng).
- Giới hạn đã biết: dòng `simple_record` đã khóa không đổi được, nên `GET /vehicle-schedules` và `VehicleScheduleApi` vẫn trả giá trị gốc; giá trị đúng ở chuỗi `/corrections/M03.VEHICLE_SCHEDULE/{id}`. Kiểm chồng lịch của lệnh đặt/dời mới chỉ xét lịch Đã đặt, Đang dùng nên không đọc giờ đã đính chính của lịch Đã hoàn thành.

## T018 Sửa mô tả — 2026-11-12

- `AssetDetail` (api-019) thêm trường mô tả đúng tập trường của `AssetWrite` (chủ dự án duyệt 2026-11-12) để màn Sửa mô tả điền sẵn; `AssetDetailResponse` đọc từ `AssetRow`, không thêm truy vấn. Web dùng lại `CreateAssetSheet` với `editing` (không tạo form thứ hai); bộ phận, nguồn, yêu cầu mua sắm khóa vì `AssetService.update` từ chối đổi, nguyên giá gắn yêu cầu mua sắm khóa (BR-M11-19).