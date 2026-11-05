# Implementation Plan: Quản lý đồ gửi của người cao tuổi

**Branch**: `013-personal-belongings` | **Date**: 2026-10-10, cập nhật 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/013-personal-belongings/spec.md`

## Summary

Feature 013 là module M12 – Đồ gửi (mục 16; UC-65; DBR-22). Nó cung cấp:
1. Danh mục loại đồ, mức tình trạng, vị trí lưu giữ (mục A).
2. Tiếp nhận đồ gửi có ảnh với đồ có giá trị; tiếp nhận và giao ngay với đồng ý cho tự giữ (mục B, C; BR-M12-05, BR-M12-06).
3. Bàn giao: mọi đổi người giữ, vị trí, trạng thái là một bản ghi nhóm 3; tách đồ khi bàn giao một phần; đính chính có giới hạn (mục D; BR-M12-01, Q-160).
4. Trả cho người có quyền nhận, xác nhận người nhận khác, duyệt thay của Quản lý viện (mục E; BR-M12-04, Q-149, Q-150, Q-253).
5. Thất lạc, hư hỏng: thông báo, người ghi chọn ghi sự cố mới, liên kết hoặc không (mục F; BR-M12-02, Q-307).
6. Điều kiện kết thúc lưu trú, mục "xử lý đồ gửi" sau qua đời, xử lý đồ không người nhận (mục G; BR-M12-03, BR-M12-08).
7. Kiểm kê đồ có giá trị do Hành chính lập theo chu kỳ (mục H2; BR-M12-07, Q-154, Q-306).
8. Cổng người thân chỉ cho người đại diện và người có quyền đón (FR-030; BR-M12-09).

Hướng kỹ thuật:
- Gói `belongings` với bảy gói con (research K-01). 013 cài các SPI 001, 004, 008, 000 đã khai cho mình; không khai SPI mới (K-02).
- Trạng thái, người giữ, vị trí của đồ là cột suy ra từ bản ghi bàn giao hiện hành gần nhất, cập nhật cùng transaction với lệnh và đính chính (K-04, K-05).
- Mỗi lệnh gửi phiên bản từng đồ; khóa theo người cao tuổi rồi theo đồ (K-03).
- Người nhận hợp lệ, điều kiện kết thúc, bằng chứng liên hệ là hàm thuần (K-06, K-08, K-09).
- Sự cố đồ gửi ghi qua `SuCoLenhApi.ghiNhan` trong cùng transaction khi người ghi chọn (K-07).
- Đề nghị xử lý đồ không người nhận là loại yêu cầu phê duyệt của 000, không tự duyệt (K-09).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 013 không tạo sự cố, không tạo phiếu kiểm kê, không gợi ý vị trí kiểm kê, không đưa mục vào bàn giao ca, không tự hủy sự cố khi bản ghi bị hủy ghi nhận;
- hệ thống chỉ: tách đồ và ghi Báo thất lạc phần thiếu kèm lệnh; tính lại trạng thái đồ sau đính chính; chuyển Hết hạn, Hết hiệu lực; kiểm tra và chặn; nhắc, thông báo.

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": feature 009 FR-001a)

- **Đối tượng thông tin** (FR-032, 009 FR-001a): mọi thông báo tới người thân, kể cả đồ thất lạc, hư hỏng, được tìm thấy và mọi thông báo sau qua đời, khai `doiTuongThongTin = VE_VIEC_NGUOI_THAN` (Q-362); không yêu cầu nào mang mã Q-354 (research K-13).

### Cập nhật 2026-10-18, lượt 3 (spec mục "Cập nhật 2026-10-18, lượt 3": Q-366)

- **Đồ gửi trên kênh người cao tuổi** (FR-030a): 013 cài SPI `family.api.DoGuiKenhNctPort.dangGiu(nctId)` (012 module-interfaces 1.4): đồ ở Đang giữ, Đang được sử dụng, tên, trạng thái, ảnh; không có lịch sử bàn giao, yêu cầu xác nhận, đồng ý cho tự giữ (research K-16). Ảnh trả qua `QuyenTepProvider` `M12.ANH_DO_GUI` thêm người cao tuổi chủ đồ. Không thêm bảng.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 012. Không thêm thư viện. Ảnh, chữ ký, bản scan dùng kho tệp `common.tep` của 000.

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test bảng cho sáu hàm thuần (research K-15); bất biến tách và trạng thái sau mọi kịch bản (SC-001).
- Test tích hợp MySQL cho trigger nhóm 3, `UNIQUE` phiếu theo (vị trí, kỳ), một đồng ý Hiệu lực mỗi đồ.
- Test đồng thời: hai bàn giao cùng đồ; Trả trong lúc Kết thúc lưu trú.
- Đồng hồ giả lập cho CFG-M10-08, CFG-M12-02 → 07.
- Bản giả `SuCoLenhApi` (có chế độ lỗi), `QuyenNguoiThanApi`.

**Target Platform**: Như 000.
- Web: danh mục, tiếp nhận, bàn giao, trả, xác nhận người nhận khác, đề nghị xử lý, kiểm kê; cổng người thân trên trình duyệt (đồ gửi, xác nhận, đồng ý).
- App nhân viên: tiếp nhận, bàn giao, trả, báo thất lạc, ghi hư hỏng (chụp ảnh, chữ ký trên thiết bị); chỉ trực tuyến.
- App người thân: đồ gửi, xác nhận người nhận khác, ghi/rút đồng ý.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Điều kiện (b) Đạt ≤ 1 phút sau đồ cuối được trả (SC-006): tính lúc hỏi.
- Thông báo thất lạc, hư hỏng ≤ 5 phút (SC-005): outbox ngay sau commit.
- Tiếp nhận một đồ có giá trị ≤ 2 phút, trả 5 đồ ≤ 5 phút thao tác (SC-007): một lệnh nhiều đồ.
- Quy mô: 300 người cao tuổi, vài nghìn đồ gửi, vài chục vị trí.

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI). Mọi lệnh chỉ trực tuyến (FR-031).
- Bản ghi bàn giao, lần trả, lần chặn, phiếu đã hoàn thành không bị `UPDATE`, `DELETE` ngoài cột đặt một lần (constitution XII, BR-M15-03).
- Nhật ký trong transaction (000 FR-047). CFG-M12-01 đọc lúc lệnh và lưu dấu trên bản ghi.

**Scale/Scope**:
- 6 thực thể có vòng đời (đồ gửi, xác nhận người nhận khác, đồng ý cho tự giữ, phiếu kiểm kê, đề nghị xử lý qua 000, bản ghi bàn giao có đính chính); 18 bảng (gồm bảng con, kỹ thuật).
- 2 job (theo phút, hằng ngày).
- 23 đường dẫn, 29 thao tác REST ([contracts/api-013.yaml](contracts/api-013.yaml)).
- 8 SPI hoặc provider 013 cài; 2 giao diện công khai.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR; x-fr, x-br trên mọi thao tác REST; data-model mục 8 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Đồ gửi chỉ đổi qua lệnh bàn giao; xác nhận, đồng ý, phiếu theo bảng trạng thái; chỉ danh mục nhóm 1 có sửa |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3, 5, 7 khớp bốn bảng trạng thái của spec; quickstart mục 4 |
| V. Không cố định giá trị | Đạt | CFG-M10-08, CFG-M12-01 → 07, CFG-M15-03 đọc từ `ThamSoApi` |
| VI. Vai trò, quyền | Đạt | Giới hạn Điều dưỡng với lệnh Trả; Quản lý viện duyệt thay, duyệt đề nghị; người thân theo BR-M12-09 (K-14) |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ; điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-012 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 007 được ghi chú (mục "Đồng bộ") |
| X. Kiến trúc | Đạt | Một khối; gói `belongings`. Không có vòng (K-02) |
| XI. Nghiệp vụ ở backend | Đạt | Người nhận hợp lệ, điều kiện kết thúc, tách đồ, kiểm kê ở backend |
| XII. Dữ liệu | Đạt | DBR-22 bằng `tiep_nhan_goc_id` và bất biến tổng số lượng; nhóm 3 bằng trigger; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V; dấu đồ có giá trị lưu trên bản ghi lúc lệnh |
| XIV. Ngoại tuyến, thông báo | Đạt | Chỉ trực tuyến (FR-031). Thông báo theo mức FR-032, nội dung tới người thân chỉ loại đồ và sự kiện |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR; mỗi BR-M12-01 → 09 có test (quickstart mục 4) |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Bổ sung kỹ thuật giải thích ở Complexity Tracking.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 013 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | Chỉ mục theo người cao tuổi, người giữ, vị trí |
| NFR-02 Hiệu năng | Có | Cột suy ra trên đồ; lệnh nhiều đồ |
| NFR-03 Thông báo khẩn cấp | Không | Không có mức Khẩn cấp |
| NFR-04 Tác vụ định kỳ | Có | Hai job; khóa nhắc theo ngày; chạy bù |
| NFR-05 Khả dụng | Gián tiếp | Listener idempotent |
| NFR-06 Sao lưu | Có | Ảnh, chữ ký ở kho tệp được sao lưu cùng cơ sở dữ liệu (000) |
| NFR-07 Lưu trữ | Có | Theo hồ sơ (CFG-M15-03) |
| NFR-08 Bảo mật | Có | Ảnh, bản ký theo `QuyenTepProvider`; người thân theo BR-M12-09 |
| NFR-09 Thời gian | Có | `Clock`; kỳ kiểm kê theo Asia/Ho_Chi_Minh |
| NFR-10 Thiết bị | Có | App nhân viên chụp ảnh, ký trên thiết bị |
| NFR-11 Ngôn ngữ | Có | Tiền VND số nguyên |
| NFR-12 Cấu hình | Có | CFG-M12 đổi qua tham số |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả 007, 012 |

## Project Structure

### Documentation (this feature)

```text
specs/013-personal-belongings/
├── plan.md                      # File này
├── research.md                  # Phase 0: K-01 → K-15
├── data-model.md                # Phase 1: bảng, bảng chuyển, DBR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng Q1 → Q46, truy vết → test
├── contracts/
│   ├── api-013.yaml             # OpenAPI 3.1: danh mục, tiếp nhận, bàn giao, trả, xác nhận, kiểm kê, cổng
│   └── module-interfaces.md     # SPI 013 cài, API cung cấp, sự kiện
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/eldercare/backend/belongings/
├── api/           # DoGuiApi, DoGuiThongKeApi, sự kiện; bản cài SPI
├── danhmuc/       {internal, web}   # loại đồ, mức tình trạng, vị trí
├── dogui/         {internal, web}   # đồ, bản ghi bàn giao, KiemTraLenhBanGiao, TachDo, đính chính
├── tra/           {internal, web}   # lần trả, NguoiNhanHopLe, xác nhận người nhận khác
├── dongy/         {internal, web}   # đồng ý cho tự giữ
├── suco/          {internal}        # thông báo, gọi 007
├── ketthuc/       {internal}        # DieuKienDoGui, MucViecSauQuaDoiPort, handler đề nghị xử lý
├── kiemke/        {internal, web}   # phiếu kiểm kê
└── lich/                            # ViecTheoPhutM12, ViecHangNgayM12

frontend/src/tinh-nang/do-gui/
├── danh-muc/  ├── tiep-nhan/  ├── ban-giao/  ├── tra/  ├── kiem-ke/  ├── xu-ly-do/  └── cong-nguoi-than/do-gui/

mobile/lib/nhan-vien/do-gui/      # tiếp nhận, bàn giao, trả (ảnh, chữ ký)
mobile/lib/nguoi-than/do-gui/     # xem, xác nhận, đồng ý
```

**Structure Decision**: Theo R-02 của 000. Feature 013 nằm ở `backend/…/belongings` với bảy gói con và gói `api`.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Cột suy ra trên `do_gui` | SC-008 tra cứu một đồ không mở bản ghi; điều kiện kết thúc, kiểm kê đọc nhanh | Tính từ bản ghi gần nhất mỗi lần đọc: truy vấn nặng cho danh sách |
| Phiên bản từng đồ trong thân lệnh | Một lệnh có nhiều đồ; FR-011b đòi từ chối lệnh dựa trên trạng thái cũ | `If-Match` chỉ cho một tài nguyên |
| `lan_chan_tra` ghi ở transaction riêng | Lần chặn phải còn sau khi lệnh Trả bị từ chối (FR-016) | Ghi chung transaction: bị rollback cùng lệnh *(R-15: `lan_chan_tra` → `ban_giao_do_gui` (`loai`))* |

## Đồng bộ với hợp đồng của feature khác

1. **007** ([../007-health-alert-incident/contracts/module-interfaces.md](../007-health-alert-incident/contracts/module-interfaces.md) mục 1.6): ghi chú `huyDoNguonDoGui` không được 013 gọi (013 FR-011a, Q-307); chữ ký giữ chờ quyết định (điểm báo lại 1). 013 dùng `ghiNhan`, `themDienBien`, `SuCoApi.xem` như đã khai báo.
2. **001, 004, 008, 000**: cài `ThamGiaChuyenTrangThai` (`KT_B_DO_GUI`), `MucViecSauQuaDoiPort` (`DO_GUI`), `DoGuiBanGiaoPort`, `LoaiYeuCauHandler`, `DinhChinhDuocProvider` đúng chữ ký. Không đổi.
3. **012**: dùng `QuyenNguoiThanApi` (`laNguoiDaiDien`, `duocPhepDonTai`, `giayTo`, `xemDoGui`) theo hợp đồng 012. Không đổi.
4. **009**: thông báo theo FR-032 cài `CoYeuCauThongBao`; **016** đọc `DoGuiThongKeApi` (giai đoạn sau, Q-194).

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Hủy sự cố đồ gửi khi bản ghi bàn giao bị Hủy ghi nhận — 007 và 013 nói ngược nhau**: 007 FR-046b (và bảng trạng thái sự cố dòng "Nguồn đồ gửi bị Hủy ghi nhận → Đã hủy, Hệ thống") cho 007 tự chuyển sự cố Đã hủy theo yêu cầu của 013; 013 FR-011a (sửa theo Q-307) cấm tự hủy, chỉ báo người xử lý sự cố. Plan làm theo 013 (quyết định mới hơn, khớp Q-307): không gọi `huyDoNguonDoGui`. Đề nghị sửa 007 FR-046b, bảng trạng thái sự cố và mục Clarifications của 007 theo Q-307, rồi bỏ hàm khỏi hợp đồng 007. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

2. **Mức thông báo nhắc đề nghị xử lý Chờ duyệt** (bảng FR-032 ghi "Nhẹ; theo feature 000 FR-031a"): plan dùng cơ chế nhắc của 000 (`NhacQuaHan`) với mức Nhẹ. Đề nghị bỏ vế "theo feature 000 FR-031a" ở cột mức để không lẫn với mức của 000. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Câu chữ còn lẫn trong spec 013** (không đổi nghiệp vụ): FR-007, FR-029, bảng trạng thái đồ gửi dẫn "4.4" (Permission Matrix của `docs/phan-tich-yeu-cau.md` đã bỏ; tra Phụ lục 27); Assumptions dòng đầu dẫn mục 4.2 của tài liệu đã bỏ; điểm báo lại 16 còn yêu cầu 001 cung cấp sự kiện cờ đi lạc (không còn đúng sau Q-281). *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Ánh xạ bảng ở data-model mục 0; task T034. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.

## Kế hoạch triển khai R-15 *(bổ sung 2026-11-05, Phase 1; nhánh `impl/013-personal-belongings` từ `develop` 0c155c46)*

Tên trong mục này là tên thật trên `develop` (đã tra bằng mã) hoặc tên dự kiến ở glossary mục 19; tasks, research giữ tên logic. Mỗi phiên làm một phase; phần web, app chờ chủ dự án duyệt hợp đồng api-013 và bố cục (DESIGN.md).

### Tên thật của giao diện 013 dùng hoặc cài

| Tên logic (tasks, contracts) | Tên thật | Trên develop |
| --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `thoiDiemTrangThaiCuoi`; sự kiện `NguoiCaoTuoiDaChuyenTrangThai` | `resident.api.ResidentApi`, `ResidentStatusApi`; `resident.api.events.ResidentStatusChanged` | thật |
| `ThamGiaChuyenTrangThai` (013 cài, `KT_B_DO_GUI`) | `resident.api.spi.StatusTransitionParticipant`; mã `END_B_BELONGINGS` | SPI có, 013 cài |
| `QuyenApi`, `PhamViApi`, `TaiKhoanApi`; `GanPhamViProvider` | `administration.account.api` (`PermissionApi`, `ScopeApi`, `AccountApi`, `@RequiresPermission`); `api.spi.ScopeAssignmentProvider` (013 cài); `DutyApi` (bản thật `staffing.duty.internal.service.DutyDirectory`) | thật |
| `ViTriApi`; sự kiện `DaChuyenGiuong` | `roombedallocation.api.ResidentLocationApi` (bản thật `AllocationQueries`); `roombedallocation.api.events.BedTransferred` | thật |
| `DanhSachViecSauQuaDoiApi.capNhat`; `MucViecSauQuaDoiPort` (013 cài); `NguoiDaiDienPort`; sự kiện `HoSoKetThucDaLap` | `admission.api.PostDeathTaskApi.refresh` (bản thật `PostDeathTaskService`); `admission.api.spi.PostDeathItemPort` (`PostDeathItemCode.BELONGINGS`); `admission.api.spi.RepresentativePort` (012 cài); `admission.api.events.ResidencyEndingCreated` | thật; `FakePostDeathItem` của 004 giữ cho test 004 |
| `SuCoLenhApi.ghiNhan`, `themDienBien`; `SuCoApi.xem` | `incident.api.IncidentCommandApi.record`, `addProgression`; `incident.api.IncidentApi` | **chưa có bean thật** → bản giả có sẵn `support/fake/incident/FakeIncidentCommands`, `FakeIncidents` (không tạo bản giả thứ hai) |
| `CaTrucApi.dangTrongCa`, `nguoiPhuTrachCa`; sự kiện `CaDaKetThuc`; `DoGuiBanGiaoPort` (013 cài) | `staffing.api.ShiftApi` (bản thật `ShiftQueries`); `staffing.api.events.ShiftEnded`; `staffing.api.spi.HeldBelongingsHandoverPort` (`heldBy`, `stillHeld`; `HeldBelongingView`) | thật; 008 tiêm port qua `ObjectProvider`, bản giả `FakeHeldBelongings` của 008 là `@Primary` trong `FakeM09Config` nên bean thật của 013 không làm trùng bean ở test 008 |
| `ThongBaoTraCuuApi`; `CoYeuCauThongBao` | `notification.api.NotificationLookupApi`; `common.notification.HasNotificationRequests` | thật |
| `QuyenNguoiThanApi` (`laNguoiDaiDien`, `duocPhepDonTai`, `giayTo`, `xemDoGui`, `nguoiDaiDien`, `nguoiLienHeChinh`); `DoGuiKenhNctPort` (013 cài) | `family.api.FamilyRightsApi` (`isRepresentative`, `allowedPickersAt`, `idDocument`, `canViewBelongings`, `representatives`, `primaryContact`; bản thật `FamilyRightsLookup`, bản giả `support/fake/family/FakeFamilyRights`); `family.api.spi.ResidentChannelBelongingPort.held` | thật |
| `ThamSoApi`, `NhatKyApi`, `DinhChinhApi`, `YeuCauPheDuyetApi`, `TepApi`; `DinhChinhDuocProvider`, `LoaiYeuCauHandler`, `PhanLoaiTruongProvider`, `LuuTruProvider`, `QuyenTepProvider` (013 cài) | `ParameterApi`, `AuditLogApi`, `CorrectionApi`, `ApprovalRequestApi`, `common.file.FileApi`; `CorrectableRecordProvider`, `RequestTypeHandler`, `FieldClassificationProvider`, `RetentionProvider`, `common.file.FilePermissionProvider` | thật |
| bảng chung R-15 | `administration.shared.api` (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`); `common.data.ReminderMarkers`, `TechnicalLocks`; bản giả `support/fake/shared/` | thật |

Không module nào trên develop import `belongings` (đã tra 2026-11-05), nên 013 gọi các module trên không tạo vòng; mỗi phase vẫn chạy `ModuleBoundaryTest` trước khi thêm import gói mới.

### Phase → task → phụ thuộc

| Phase | Task | Việc chính | Phụ thuộc thật/giả, việc chờ |
| --- | --- | --- | --- |
| 1 Setup | T001 | `M12ArchTest`; dịch api-013, glossary mục 19; mục này | — |
| 2 Foundational | T002 | `belongings.api` (đọc, thống kê, sự kiện `BelongingStatusChanged`, thông báo) | thật |
| | T003 | `V013_01__belongings.sql` chỉ hai bảng giữ (data-model mục 0): `belonging`, `belonging_handover` (nhóm `RECORD`, trigger `no_update` trừ đặt `incident_id` một lần, `no_delete`; ảnh, lần trả, lần chặn trả vào cột `kind` và `JSON`); phần còn lại vào bảng chung | thật |
| | T004 | hàm thuần kiểm lệnh bàn giao, tách đồ | — |
| | T005 | khóa theo người rồi theo đồ; danh mục loại đồ, mức tình trạng, vị trí trên `catalog_item` | thật; quyền cấu hình danh mục của Quản lý viện cần rà (điểm 3 dưới) |
| | T006 | job theo phút, hằng ngày của M12 | **chờ A**: `belongings.scheduling` là job mới, cần dòng ở `SchedulerScopeArchTest.ALLOWED`, cấu hình `eldercare.jobs.m12-*` ở `application.yml` và profile test (file của A; team-plan mục 8) |
| | T007 | provider phân loại trường, lưu giữ, phạm vi, quyền tệp | thật |
| 3 US1 | T008–T011 | tiếp nhận, giao ngay, đồng ý cho tự giữ | thật (`ResidentStatusApi`, `RepresentativePort`, `FamilyRightsApi`, `FileApi`, CFG-M12-01); web, app chờ duyệt |
| 4 US2 | T012–T015 | bàn giao, tách, đính chính, `HeldBelongingsHandoverPort`, nghe `ShiftEnded`, `BedTransferred` | thật; web chờ duyệt |
| 5 US3 | T016–T019 | trả, xác nhận người nhận khác, duyệt thay | thật (`FamilyRightsApi`, CFG-M10-08, CFG-M12-05, 06); web, app chờ duyệt |
| 6 US4 | T020–T021 | thất lạc, hư hỏng, sự cố | **giả**: `FakeIncidentCommands`, `FakeIncidents` (007 chưa có bean thật); cách xử lý khi production chưa có bean chốt cùng D lúc làm phase này |
| 7 US5 | T022–T026 | điều kiện kết thúc, mục sau qua đời, đề nghị xử lý, kiểm kê | thật (`StatusTransitionParticipant`, `PostDeathItemPort`, `PostDeathTaskApi`, `ResidencyEndingCreated`, `ApprovalRequestApi`); web chờ duyệt |
| 8 US6 | T027–T028 | cổng người thân | thật (`FamilyRightsApi.canViewBelongings`); web, app chờ duyệt |
| 9 Polish | T029–T034 | entry api-013 ở `ApiContractIT` (A xem), `RecordGroupTriggerIT`, kênh người cao tuổi, rà truy vết, `clean verify` toàn kho | entry hợp đồng và `TRANSLATED` sau khi chủ dự án duyệt |

### Lệch kỹ thuật và điểm cần rà (không đổi nghiệp vụ)

1. **`dong_y_tu_giu`**: data-model mục 0 (sửa 2026-10-30) đặt vào `ho_so_chung` (`simple_record`, một bản Hiệu lực mỗi đồ qua `unique_key`); tasks T009 và research K-03 còn ghi "→ `do_gui` (cột)". Đề xuất theo data-model mục 0 (bản mới hơn, giữ được lịch sử Hiệu lực → Đã rút → Hết hiệu lực); chốt khi làm T009 (Phase 3), sửa tasks T009 cùng lúc.
2. **`do_gui_nct` → `trang_thai_nct`** (`resident_module_state`, một dòng mỗi người mỗi module): M12 chỉ cần dòng này làm đích khóa theo người (K-03), không cần nhiều dòng; dữ liệu nhiều dòng theo người (đồng ý, xác nhận) dùng `simple_record` (bài học 018). `nhac_kiem_ke` (data-model mục 7) → `reminder_marker`.
3. **Quyền** (K-14, FR-029): ma trận chỉ có một dòng `Function.BELONGINGS` (`VIEW`, `CREATE`, `UPDATE`, `CONFIRM`), Quản lý viện (AC-01) chỉ `VIEW`. Lệnh của Quản lý viện (cấu hình loại đồ, mức tình trạng; duyệt thay; duyệt đề nghị xử lý qua 000) và giới hạn "Điều dưỡng chỉ trả đồ không có giá trị" phải ánh xạ vào khóa sẵn có hoặc kiểm vai trò trong service. Rà khi làm T005 (Phase 2); nếu cần khóa hoặc ô ma trận mới thì dừng, ghi team-plan nhờ A, không tự thêm `Function`.
4. **Gói** (glossary 19.5): `internal.catalog`, `belonging`, `returns`, `consent`, `incident`, `ending`, `inventorycheck`, `transition`; `scheduling`. Theo backend/README mục 2 không tạo gói rỗng: T001 chỉ thêm `M12ArchTest`, gói con tạo cùng lớp đầu tiên ở Phase 2. Luật của `M12ArchTest` áp cho `belongings..scheduling..` (gồm phần nội bộ `internal.scheduling` nếu có, như 012).
5. **Hợp đồng**: api-013 đã dịch 2026-11-05 (glossary mục 19), chờ chủ dự án duyệt; `Problem` giữ cấu trúc gốc (`errorCode`, `unmetConditions`) như api-004, chưa đổi sang `Problem` chung của api-012. Chưa thêm `'013'` vào `TRANSLATED` của `frontend/scripts/gen-api.mjs`, chưa thêm entry ở `ApiContractIT`.
6. **FR-015 chọn "Ghi hư hỏng"** (Phase 2, T004): khi mức tình trạng xấu hơn và thân lệnh có `conditionHandling = RECORD_DAMAGE`, `HandoverCommandCheck` coi lệnh là Ghi hư hỏng (áp điều kiện của Ghi hư hỏng: từ Đang giữ hoặc Đang sử dụng, lý do, ảnh, vị trí). Đây là cách hiểu enum `xuLyTinhTrang` của hợp đồng (research K-04 "chuyển sang lệnh Ghi hư hỏng"); xem lại khi làm US2.
7. **Thêm, ngừng mức tình trạng** (Phase 2, T005): `POST /belongings/condition-levels` không có mã mức nên chỉ thêm được; ngừng mức chưa có đường trong hợp đồng (thiếu `id`). Mã đang trả 422 khi `status` khác `ACTIVE`. Đề nghị chủ dự án bổ sung khi duyệt api-013.
8. **Quyền danh mục** (Phase 2, cập nhật điểm 3): loại đồ, mức tình trạng dùng khóa cấu hình danh mục `ACCOUNT_PERMISSION_PARAMETER/UPDATE` kèm kiểm vai trò Quản lý viện (như danh mục M05 của 007); vị trí lưu giữ: Hành chính qua `BELONGINGS/UPDATE`, Quản lý viện qua `ACCOUNT_PERMISSION_PARAMETER/UPDATE`; đọc ba danh mục kiểm vai trò có dòng "Đồ gửi" (Quản lý viện, Trưởng tầng, Điều dưỡng, Hành chính) vì lệnh không gắn người cao tuổi nên phạm vi "được phân công" của 002 luôn trượt lớp phạm vi. Duyệt thay, giới hạn trả của Điều dưỡng rà ở US3.
9. **`ApiContractIT`** (Phase 2): theo tiền lệ 018 (BL-91), B thêm entry api-013 khi có controller đầu tiên (đường dẫn kiểm ngay, so operation tắt tới T029); A xem (dòng team-plan "duyệt hợp đồng 013").
10. **Job M12** (T006): chưa tạo lớp job vì `SchedulerScopeArchTest` (của A) bắt mọi `ScheduledJob` phải có trong `ALLOWED`; chờ A (team-plan mục 8).
11. **US1** (Phase 3): đồng ý cho tự giữ ở `simple_record` (chốt điểm 1 theo data-model mục 0). Người đại diện kiểm bằng `FamilyRightsApi.isRepresentative` của 012 lúc ký, rút. Lệnh cổng (`/family-portal/self-custody-consents`) không dùng `@RequiresPermission` vì phạm vi "quan hệ" của 002 đọc bảng thật của 012; service kiểm tài khoản người thân và người đại diện (research K-12). Điều dưỡng ngoài phạm vi nhận 404 (002 FR-043, không lộ người cao tuổi), quickstart Q4 ghi 403. Thiếu ảnh đồ có giá trị trả 409 `PHOTO_REQUIRED` theo hợp đồng và quickstart Q1 (tasks T008 ghi 422). Người ký bản đồng ý không phải người đại diện: 409 `GIVER_NOT_REPRESENTATIVE`. Phản hồi `Belonging.holder` là loại người giữ (`STAFF`, `RESIDENT`). Chưa làm: thông báo khi bật dấu "cần đồng ý" mà đồ đang sử dụng chưa có đồng ý (FR-015a), đồng ý Hết hiệu lực khi đồ đổi trạng thái (US2, US3).
12. **US2** (Phase 4, 2026-11-05): `internal.belonging.service.BelongingHandoverService` (không đặt tên `HandoverService`: trùng tên bean với 008) nhận đủ sáu lệnh của `POST /belongings/handovers` (Giao sử dụng, Thu lại, Chuyển giữ, Báo thất lạc, Ghi hư hỏng, Tìm thấy) theo `HandoverCommandCheck`; lệnh chỉ gồm đồ của một người cao tuổi (khác người: 422). **Sự cố** (FR-021, FR-024): Phase 4 chỉ nhận `incident.method` trống hoặc `NONE`; `NEW`, `LINK` trả 422 `INVALID_VALUE` tới khi làm US4 (T021, `FakeIncidentCommands`); thông báo thất lạc, hư hỏng (Trung bình, người liên hệ chính và Hành chính), tìm thấy (Nhẹ) đã gửi từ Phase 4 vì phần đếm thiếu (FR-013) cần. **Người giao trống**: người cao tuổi khi người cao tuổi giữ, ngược lại là người thực hiện (nên FR-014 thực chất kiểm "người thực hiện là người giữ"); **người nhận** của Thu lại, Ghi hư hỏng, Tìm thấy: `recipientStaffId` hoặc người thực hiện; Chuyển giữ: `recipientStaffId` hoặc người giữ hiện tại, phải đổi vị trí hoặc người giữ. **Người giữ vắng mặt**: `ShiftApi.onShift` (Hành chính, Kế toán, Quản lý viện tính theo giờ hành chính CFG-M13-06 của 008) hoặc `AccountApi.byStaff` khác `ACTIVE`; bản ghi ghi người giao là người giữ cũ; người giữ cũ nhận Nhẹ. **Tách**: `original_belonging_id` là đồ bị tách ra (cha trực tiếp), `intake_handover_id` của đồ đó; đồ gốc giữ phần còn lại, không thêm bản ghi. **Số lượng** (`BelongingAmounts`): đồ mang số lượng của bản ghi đầu tiên của nó (Tiếp nhận hoặc bản ghi tách) trừ bản ghi đầu tiên của các đồ tách trực tiếp; `BelongingRecalculation.verify` kiểm SC-001 cuối mỗi lệnh. Mục `quantity` của từng đồ là số tiền với tiền mặt (hợp đồng không có `cashAmount` trong `items`). **Đồng ý cho tự giữ**: kiểm trên đồ trong lệnh; đồ tách mới không mang đồng ý của đồ gốc (điểm cần chủ dự án xác nhận: giao 500.000 trong 2.000.000 đã có đồng ý thì phần 500.000 sau khi Thu lại muốn giao lại phải có đồng ý mới); Hết hiệu lực trong cùng lệnh khi đồ sang Đã trả, Thất lạc, Hư hỏng, Đã xử lý (`SelfCustodyConsentService.expire`). **Nguồn thông báo** là bản ghi bàn giao (`M12.BELONGING_HANDOVER`, khóa `(loại sự kiện, bản ghi)` thay cho `(loại sự kiện, đồ gửi)` của K-13, vì một đồ có thể thất lạc nhiều lần) để Hủy ghi nhận tra `NotificationLookupApi.bySource` đúng người đã nhận. Sau qua đời thông báo thất lạc, hư hỏng, tìm thấy gửi người đại diện, dấu `afterDeath`.
    **Đính chính** `BelongingHandoverCorrection` (`M12.BELONGING_HANDOVER`, không gắn tầng, người ghi Hành chính hoặc Điều dưỡng): trường sửa được `occurredAt`, `quantity`, `conditionLevelId`, `conditionDescription`, `storageLocationId`, `giverName`, `giverIdDocument`, `recipientName`, `recipientStaffId`, `addedPhotoFileIds`; trường khác (gồm `kind`, `targetStatus`) 422. Giới hạn theo bản gốc kiểm trong `afterCorrection` (cùng transaction, lỗi thì không lưu đính chính) vì `allows` của 000 không biết kiểu đính chính; với cách C kiểm lúc áp dụng yêu cầu. Hủy ghi nhận: chỉ bản ghi hiện hành mới nhất của đồ, và bản ghi đầu tiên của đồ chỉ khi đồ không còn đồ tách → 409 `NOT_LATEST_HANDOVER` (mã mới, glossary 19.4); đồ không còn bản ghi hiện hành bị loại (`excluded`), số lượng trả về đồ cha. Sửa số lượng chỉ ở bản ghi đầu tiên của đồ → 409 `QUANTITY_CORRECTION_NOT_ALLOWED` (sửa bản ghi sau làm lệch SC-001; muốn sửa thì Hủy ghi nhận rồi ghi lại); mức Không dùng được chỉ ở Tiếp nhận, Ghi hư hỏng (409 `UNUSABLE_LEVEL_ONLY_VIA_RECORD_DAMAGE`); số lượng âm sau tính lại 409 `INVALID_QUANTITY`. Chưa làm: báo người xử lý sự cố khi bản ghi bị hủy có `incident_id` (US4), `PostDeathTaskApi.refresh` sau đính chính (US5).
    **T014**: `HeldBelongingsHandoverPortImpl` (đồ có giá trị theo CFG-M12-01 lúc gọi); `HeldBelongingReminders` (gọi từ `HeldBelongingListeners` `@ApplicationModuleListener`; idempotent bằng `reminder_marker` loại `M12.EVENT`). Hết ca: `ShiftApi` không có danh sách thành viên theo ca nên xét từng người giữ đồ có giá trị bằng `staffShifts(người giữ, kết thúc − 1 giây, kết thúc)`; một thông báo mỗi người giữ (khóa `(loại, ca, người giữ)`). Chuyển tầng: đồ Đang giữ tại vị trí có `floorAreaId` bằng tầng cũ → Hành chính và `AssignedNurse` (phân giải lúc gửi). **Bean trùng với test 008**: `staffing.HandoverIT` khai `FakeHeldBelongings` không `@Primary` (chỉ `FakeM09Config` có `@Primary`), nên bean thật của 013 làm `ObjectProvider.getIfAvailable` của 008 có hai ứng viên (`HandoverIT.FR_039_sourcesAreReadInParallelAndBelongingStateIsReadEvenAfterRelease` lỗi khi chạy thử); 013 không sửa test của 008 mà đánh dấu bean thật `@Fallback` (Spring 6.2+): có bản giả nào thì bản giả được chọn, production chỉ có bean của 013.
    **T015**: `GET /staff/{staffId}/held-belongings` trả schema `Belonging`; người xem là chính nhân viên đó hoặc vai trò có dòng "Đồ gửi" (như điểm 8, lệnh không gắn người cao tuổi). Nợ Phase 3 xong: bật dấu "cần đồng ý" báo từng đồ loại đó Đang được sử dụng chưa có đồng ý (`SELF_CUSTODY_CONSENT_REQUIRED_IN_USE`, Trung bình, Hành chính và điều dưỡng phụ trách).
13. **US3** (Phase 5, 2026-11-05): lần trả, lần chặn trả là dòng của `belonging_handover` (`V013_02`, tasks ghi V013_03): `RETURN` mỗi đồ một bản ghi dùng chung người nhận, thời điểm, chữ ký (tệp bản ký `M12.RETURN_SIGNATURE` gắn vào bản ghi đầu của lệnh); `RETURN_BLOCKED` mỗi đồ trong lệnh bị chặn một bản ghi (đồ giữ nguyên, `current_handover_id` không đổi; `BelongingHandoverStore.ofIntake`, `ofBelongings` loại bản ghi này khỏi lịch sử và tính lại). **Lần chặn còn lại sau khi lệnh bị từ chối** (FR-016, SC-004): `ReturnService` không có transaction, `ReturnTransaction.run` trả kết quả "bị chặn" (không ném lỗi) để commit cả bản ghi chặn rồi `ReturnService` ném lỗi; không dùng `REQUIRES_NEW` vì transaction ngoài đang giữ khóa dòng đồ gửi và dòng người cao tuổi nên giao dịch trong sẽ chờ khóa của chính lệnh gọi. Chặn khi sai người nhận (bốn lý do `ID_DOCUMENT_MISMATCH`, `NOT_ENTITLED_TO_RECEIVE`, `INVALID_RECIPIENT_CONFIRMATION`, `NURSING_STAFF_VALUABLE_ITEM`; mã lỗi 409 `RETURN_BLOCKED` cho hai lý do đầu, hai lý do sau trả đúng mã của lý do); thiếu ảnh đồ có giá trị, trạng thái đồ không cho trả, `STATE_CHANGED` không ghi lần chặn (không liên quan người nhận). Người nhận được kiểm trước điều kiện từng đồ. **Điều dưỡng** (`ReturnRecipient`): đồ có giá trị chặn cả lệnh; chỉ trả cho người có quyền nhận hoặc chính người cao tuổi thuộc ngoại lệ Q-253, không dùng xác nhận người nhận khác (`NOT_ENTITLED_TO_RECEIVE`); Hành chính trả mọi đồ. Phân biệt bằng vai trò trong service, không thêm khóa hay ô ma trận. **Duyệt thay** của Quản lý viện: `@RequiresPermission(BELONGINGS, VIEW)` (AC-01 chỉ có `VIEW`) kèm kiểm vai trò Quản lý viện trong service, như danh mục M05 của 007; `SELF_APPROVAL_NOT_ALLOWED` so tài khoản người lập với tài khoản duyệt. **Đồ trong lệnh Trả**: `ReturnCommand` của hợp đồng không có số lượng, số tiền từng đồ nên trả nguyên đồ (không trả một phần tiền mặt dù FR-012 coi là tách đồ): xin chủ dự án thêm trường nếu cần (team-plan mục 8). Tình trạng lúc trả ghi nguyên vẹn vào bản ghi, xử lý `ACKNOWLEDGE` ngầm (không bắt chọn khi xấu hơn vì Đã trả là trạng thái cuối); mức "Không dùng được" vẫn chỉ qua Ghi hư hỏng. **Người giao** của Trả là người thực hiện (nên phải là người giữ, FR-014; hợp đồng không có `holderAbsent` cho Trả), riêng Trả từ Đang được người cao tuổi sử dụng ghi nhân viên thực hiện và ghi chú "Thu từ người cao tuổi" (FR-018). **Đã trả** không còn người giữ (`holder` trống như Thất lạc). **Chữ ký** (FR-019): `signatureFileId` bắt buộc; riêng người nhận là người cao tuổi có thể trống nếu có `noSignatureReason` (nhân viên ghi lý do người cao tuổi không còn khả năng ký), lệch nhẹ so với "required" của schema để khớp FR-019. **Hết hạn** xác nhận kiểm cả khi job chưa chạy (`isUsable` so `expiresAt`). **Việc theo phút** (T018): `RecipientConfirmationTimers.expireDue` (Hiệu lực quá CFG-M10-08 → Hết hạn, báo người lập Nhẹ) và `notifyEligible` (Chờ xác nhận đủ điều kiện duyệt thay theo CFG-M12-05, CFG-M12-06 hoặc không có người đại diện → báo Quản lý viện một lần, Trung bình); chưa có lớp `ScheduledJob` (chờ A, điểm 10), job chỉ gọi hai phương thức này; hai việc tự động chưa ghi nhật ký vì job chưa có người dùng hệ thống. Đồ rời Đang giữ, Đang được sử dụng, Hư hỏng bị loại khỏi danh sách xác nhận còn mở; hết đồ thì tự Hủy (`dropBelonging`, gọi từ listener `BelongingStatusChanged`; test gọi thẳng vì listener bất đồng bộ). Thông báo mới (mọi thông báo tới người thân khai `ABOUT_FAMILY_MATTER` qua `BelongingNotices`): `BELONGING_RETURNED` (người đại diện trừ người nhận, Nhẹ), `RECIPIENT_CONFIRMATION_REQUESTED` (Trung bình), `_CONFIRMED`, `_REJECTED`, `_EXPIRED`, `_CANCELLED_NO_ITEMS` (người lập, Nhẹ), `_ELIGIBLE` (Quản lý viện, Trung bình), `_APPROVED_ON_BEHALF` (người đại diện Hiệu lực, Trung bình), `FAMILY_ID_DOCUMENT_MISSING` (Hành chính, Nhẹ). **Chưa làm**: ca Trả đồng thời Kết thúc lưu trú (T022), `PostDeathTaskApi.refresh` sau Trả (US5), bản ký và lần trả xem ở cổng (US6).
14. **US4** (Phase 6, 2026-11-05): sự cố đồ gửi do người ghi chọn, gói `internal.incident.service` (glossary 19.5). `HandoverCommand.Item.incident` là `IncidentChoice(method, incidentId, level, description, place)` (thay `incidentMethod`; `HandoverRequest.IncidentOptionRequest` có đủ trường của `IncidentOption`). `BelongingIncidents.choose` là cổng sang 007, chạy trong transaction của lệnh (lỗi thì rollback cả lệnh): `NEW` gọi `IncidentCommandApi.record(RecordIncidentCommand(source = BELONGINGS, incidentTypeId tra từ IncidentTypeLookup theo BELONGINGS_LOST hoặc BELONGINGS_DAMAGED, level bắt buộc LOW, MEDIUM, CRITICAL, description trống thì lấy lý do của lệnh, location = place, occurredAt = detectedAt = lúc phát hiện hoặc lúc ghi))`; `LINK` kiểm `IncidentApi.view` (tồn tại, cùng người cao tuổi, không `VOIDED`, nếu không thì 409 `INVALID_INCIDENT`); `NONE` hoặc trống không làm gì. Lựa chọn `NEW`, `LINK` ở lệnh không phải Báo thất lạc, Ghi hư hỏng (kể cả Ghi hư hỏng do `conditionHandling`) trả 422 `INVALID_VALUE`; riêng lệnh có phần đếm thiếu mà không phải Báo thất lạc thì phần thiếu (bản ghi `REPORT_LOST` tách riêng) nhận lựa chọn của đồ (FR-024). Mã sự cố lưu bằng `BelongingHandoverStore.linkIncident` (một UPDATE `incident_id` từ NULL mà trigger `no_update` cho phép). **Tìm thấy**: `lossIncidentOf(đồ nguồn)` lấy sự cố của bản ghi `REPORT_LOST` gần nhất có sự cố rồi `addProgression(FOUND, "đã tìm thấy <số tìm được>/<số lượng trước khi tìm>")`, không đóng sự cố; không có sự cố liên kết thì không làm gì. **Liên kết sau** `POST /belonging-handovers/{id}/link-incident` (`BelongingIncidentLinks`, quyền Cập nhật "Đồ gửi" theo phạm vi): bản ghi phải là `REPORT_LOST` hoặc `RECORD_DAMAGE` (409 `HANDOVER_NOT_LINKABLE`), chưa có sự cố (409 `INCIDENT_ALREADY_LINKED`), sự cố hợp lệ; nhật ký `M12_BELONGING_INCIDENT_LINKED`. **Hủy ghi nhận** bản ghi có `incident_id` → thông báo `BELONGING_INCIDENT_LINK_VOIDED` (Nhẹ), không gọi hủy sự cố, `incident_id` giữ nguyên (điểm báo lại 1). Thông báo FR-032 (một thông báo mỗi bản ghi, Trung bình cho thất lạc, hư hỏng) đã có từ điểm 12. **Phụ thuộc vào D (007), không chặn Phase 6** (ghi team-plan mục 8): (a) bản thật `IncidentCommandApi`, `IncidentApi` chưa có bean, nên production trả 503 `SOURCE_UNAVAILABLE` cho `NEW`, `LINK`, Tìm thấy có sự cố; (b) 013 không tra được `incidentTypeId`: `M05.INCIDENT_TYPE` là kind nội bộ M05, không `sharedRead`, `V007_02` chưa nạp loại `BELONGINGS_LOST`, `BELONGINGS_DAMAGED`; 013 khai `IncidentTypeLookup` (mặc định `UnavailableIncidentTypes` trả 503; test khai bản giả `@Primary`), D cung cấp bản cài hoặc cho `RecordIncidentCommand` nhận mã loại; (c) `IncidentView` chưa có người phụ trách nên báo "người xử lý sự cố" tạm gửi Quản lý viện, D thêm người phụ trách thì đổi sang `RecipientGroup.Staff`; (d) `RecordIncidentCommand` không có trường tham chiếu (đồ gửi, bản ghi) như research K-07, liên kết lưu một phía ở 013. Web và app chưa làm.
15. **US5** (Phase 7, 2026-11-05): điều kiện kết thúc, mục sau qua đời, đề nghị xử lý, kiểm kê. **Điều kiện (b)**: `internal.ending.domain.EndingConditionCalculator` (hàm thuần), `BelongingEndingService` cài `BelongingApi` và `GET /residents/{residentId}/belongings/ending-condition` (phản hồi `EndingConditionResponse`: `met`, `blocking`, `lostWithOpenIncident`, `postDeathItem`; sự cố mở = `NEW`, `IN_PROGRESS`, `MONITORING` qua `BelongingIncidents.isOpen`, chưa có bean 007 thì coi là không mở). `internal.transition.service.BelongingTransitionParticipant` (`StatusTransitionParticipant`, `@Order(80)`, lệnh `END_RESIDENCY`, mã `END_B_BELONGINGS`, `waivable = true`, không có tác động). **Mục sau qua đời**: `BelongingPostDeathItem` (`PostDeathItemPort`, `BELONGINGS`) tính từ bản ghi bàn giao hiện hành có `occurred_at` không muộn hơn lúc qua đời (`PostDeathBelongings`); `BelongingHandoverService` gọi `PostDeathTaskApi.refresh` cuối mỗi lệnh (và `dispose`) khi người cao tuổi đã qua đời. FR-026a: Tìm thấy khi mục đang Hoàn thành đặt `disposal_clock_start_at` (`BelongingEndingStore`), đồ đó không tính vào mục và có nhắc, CFG-M12-03 tính từ lúc đó. **Đề nghị xử lý**: loại yêu cầu `M12.UNCLAIMED_BELONGING_DISPOSAL` (tên của glossary 19; contracts ghi `M12.XU_LY_DO_KHONG_NGUOI_NHAN`), `UnclaimedDisposalHandler`: `choTuDuyet = false` là `allowSelfApproval() = false`; Hành chính lập (`BELONGINGS/CREATE` kèm kiểm vai trò), Quản lý viện duyệt (`BELONGINGS/VIEW` vì AC-01 chỉ có Xem, kèm `approverRoleRestriction`). Nội dung: `residentId, belongingIds, method (SELL|DESTROY|TRANSFER_TO_AUTHORITY), receivingUnit, minutesFileId, photoFileIds, contactAttempts[{familyMemberId, channel, at, result}]`; bằng chứng liên hệ nằm trong nội dung yêu cầu (không dùng `record_event` `M12.CONTACT_EVIDENCE` như xác nhận người nhận vì đề nghị chưa có mã khi lập); quy tắc ở hàm thuần `UnclaimedDisposalRules` (CFG-M12-03, CFG-M12-07 với khoảng cách CFG-M12-02, tiền mặt chỉ chuyển cơ quan, ảnh, biên bản; vi phạm 409 mang mã `DISPOSAL_TOO_EARLY`, `BELONGING_NOT_DISPOSABLE`, `CASH_ONLY_TRANSFER_TO_AUTHORITY`, `PHOTO_REQUIRED`, `MINUTES_REQUIRED`, `CONTACT_EVIDENCE_INSUFFICIENT`, `NO_BELONGINGS`). Áp dụng: `BelongingHandoverService.dispose` mỗi đồ còn Đang giữ, Hư hỏng một bản ghi `DISPOSAL` (`approval_request_id`, `receiving_unit`), đồ đã đổi trạng thái bị loại, thông báo người đại diện Nhẹ. **Nhắc**: `FinalStateReminders.remindUnreturned()` (CFG-M12-02 mỗi chu kỳ báo Hành chính; CFG-M12-03 một lần mỗi đồ) và `endingCreated` (nghe `ResidencyEndingCreated`, báo người đại diện), `InventoryCheckReminders.remind()` (đầu kỳ, sau hạn mỗi ngày, Quản lý viện một lần); job M12 (T006) chờ A chỉ gọi các phương thức này. **Kiểm kê**: không cần migration (phiếu ở `simple_record` loại `M12.INVENTORY_CHECK`, `unique_key = vị trí:kỳ`; dòng, đồ không rõ chủ trong JSON; cột `inventory_check_id` đã có ở `V013_01`), nên không có `V013_04` như tasks ghi. Kỳ tính từ CFG-M12-04 (`InventoryPeriod`, hạn = hết ngày thứ N của kỳ). Lập phiếu chặn trùng (vị trí, kỳ) `INVENTORY_CHECK_ALREADY_EXISTS` và vị trí không có đồ có giá trị `NO_VALUABLE_BELONGINGS` (mã glossary cũ `INVENTORY_CHECK_EXISTS_FOR_PERIOD` đổi theo mã thực). Hoàn thành: kết quả cho mọi đồ còn Đang giữ tại vị trí (thiếu → 422), đồ đã đổi trạng thái hoặc vị trí → `DROPPED`; `NOT_FOUND` → Báo thất lạc, `CONDITION_MISMATCH` + `recordDamage` → Ghi hư hỏng (người giao là người giữ), `found` → Tìm thấy (đồ có giá trị cần ảnh mà hợp đồng `found` chỉ có mã nên trả 409 `PHOTO_REQUIRED`: xin chủ dự án thêm ảnh cho `found`), `MATCHED` và Lệch tình trạng không ghi hư hỏng không sinh bản ghi (mức mới chỉ lưu ở dòng phiếu, đồ giữ nguyên); bản ghi sinh ra mang `inventory_check_id` (`BelongingHandoverService.inventoryHandover`). Đính chính số lượng khi thừa (FR-028b) đi theo đính chính của 000, không nằm trong lệnh hoàn thành. Ảnh đồ không rõ chủ gắn `M12.INVENTORY_PHOTO` (`InventoryPhotoPermission`: Hành chính, Quản lý viện), cùng lớp là `ScopeAssignmentProvider` `M12.INVENTORY_CHECK` (toàn viện). **Chưa làm**: `CorrectableRecordProvider` `M12.INVENTORY_CHECK` (chưa rõ trường sửa được, xin chủ dự án), ca Trả đồng thời Kết thúc lưu trú (T022), `BelongingStatisticsApi` (016), web `do-gui/kiem-ke`, `do-gui/xu-ly-do`, nhật ký ba hành động mới ghi loại thường (`UNCLAIMED_DISPOSAL_APPLIED`, `INVENTORY_CHECK_COMPLETED`, `INVENTORY_CHECK_CANCELLED`) vì loại quan trọng đòi lý do.
16. **US6** (Phase 8, 2026-11-05): `GET /family-portal/residents/{residentId}/belongings` (`BelongingPortalController`, `BelongingPortalQueries`, `PortalBelongingResponse`): tài khoản người thân và `FamilyRightsApi.canViewBelongings` (người đại diện hoặc người có quyền được phép đón) kiểm ở mỗi lần gọi, không đúng thì 403; phản hồi không có vị trí lưu giữ, người giữ chỉ là loại, lịch sử gồm loại bản ghi, trạng thái đích, thời điểm, số lượng, tình trạng, ảnh (không tên nhân viên, người giao, người nhận). Ảnh: `BelongingPhotoPermission` cho tài khoản người thân tải khi `canViewBelongings` (kiểm lại mỗi lần tải). Phần AC-16 (người cao tuổi chủ đồ) và `ResidentChannelBelongingPort` làm ở T033 (Phase 9). Lệnh của người đại diện qua cổng (xác nhận người nhận khác, đồng ý cho tự giữ) đã có từ Phase 3, 5. Web `cong-nguoi-than/do-gui` và app `nguoi-than/do-gui` chờ duyệt hợp đồng. Đã bỏ dòng pending cuối của api-013 ở `ApiContractIT`.
