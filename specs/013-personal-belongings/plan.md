# Implementation Plan: Quản lý đồ gửi của người cao tuổi

**Branch**: `013-personal-belongings` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

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
- Gói `m12` với bảy gói con (research K-01). 013 cài các SPI 001, 004, 008, 000 đã khai cho mình; không khai SPI mới (K-02).
- Trạng thái, người giữ, vị trí của đồ là cột suy ra từ bản ghi bàn giao hiện hành gần nhất, cập nhật cùng transaction với lệnh và đính chính (K-04, K-05).
- Mỗi lệnh gửi phiên bản từng đồ; khóa theo người cao tuổi rồi theo đồ (K-03).
- Người nhận hợp lệ, điều kiện kết thúc, bằng chứng liên hệ là hàm thuần (K-06, K-08, K-09).
- Sự cố đồ gửi ghi qua `SuCoLenhApi.ghiNhan` trong cùng transaction khi người ghi chọn (K-07).
- Đề nghị xử lý đồ không người nhận là loại yêu cầu phê duyệt của 000, không tự duyệt (K-09).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 013 không tạo sự cố, không tạo phiếu kiểm kê, không gợi ý vị trí kiểm kê, không đưa mục vào bàn giao ca, không tự hủy sự cố khi bản ghi bị hủy ghi nhận;
- hệ thống chỉ: tách đồ và ghi Báo thất lạc phần thiếu kèm lệnh; tính lại trạng thái đồ sau đính chính; chuyển Hết hạn, Hết hiệu lực; kiểm tra và chặn; nhắc, thông báo.

## Technical Context

**Language/Version**: Java 21 LTS; TypeScript 5.x; Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 012. Không thêm thư viện. Ảnh, chữ ký, bản scan dùng kho tệp `common.tep` của 000.

**Storage**: MySQL 8.4 LTS. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test bảng cho sáu hàm thuần (research K-15); bất biến tách và trạng thái sau mọi kịch bản (SC-001).
- Testcontainers cho trigger nhóm 3, `UNIQUE` phiếu theo (vị trí, kỳ), một đồng ý Hiệu lực mỗi đồ.
- Test đồng thời: hai bàn giao cùng đồ; Trả trong lúc Kết thúc lưu trú.
- Đồng hồ giả lập cho CFG-M10-08, CFG-M12-02 → 07.
- Bản giả `SuCoLenhApi` (có chế độ lỗi), `QuyenNguoiThanApi`.

**Target Platform**: Như 000.
- Web: danh mục, tiếp nhận, bàn giao, trả, xác nhận người nhận khác, đề nghị xử lý, kiểm kê; cổng người thân trên trình duyệt (đồ gửi, xác nhận, đồng ý).
- App nhân viên: tiếp nhận, bàn giao, trả, báo thất lạc, ghi hư hỏng (chụp ảnh, chữ ký trên thiết bị); chỉ trực tuyến.
- App người thân: đồ gửi, xác nhận người nhận khác, ghi/rút đồng ý.

**Project Type**: Web service (REST `/api/v1`), một web client và hai mobile client.

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
| X. Kiến trúc | Đạt | Một khối; gói `m12`. Không có vòng (K-02) |
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
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/vn/eldercare/m12/
├── api/           # DoGuiApi, DoGuiThongKeApi, sự kiện; bản cài SPI
├── danhmuc/       {internal, web}   # loại đồ, mức tình trạng, vị trí
├── dogui/         {internal, web}   # đồ, bản ghi bàn giao, KiemTraLenhBanGiao, TachDo, đính chính
├── tra/           {internal, web}   # lần trả, NguoiNhanHopLe, xác nhận người nhận khác
├── dongy/         {internal, web}   # đồng ý cho tự giữ
├── suco/          {internal}        # thông báo, gọi 007
├── ketthuc/       {internal}        # DieuKienDoGui, MucViecSauQuaDoiPort, handler đề nghị xử lý
├── kiemke/        {internal, web}   # phiếu kiểm kê
└── lich/                            # ViecTheoPhutM12, ViecHangNgayM12

web/src/tinh-nang/do-gui/
├── danh-muc/  ├── tiep-nhan/  ├── ban-giao/  ├── tra/  ├── kiem-ke/  ├── xu-ly-do/  └── cong-nguoi-than/do-gui/

app-nhan-vien/lib/tinh-nang/do-gui/      # tiếp nhận, bàn giao, trả (ảnh, chữ ký)
app-nguoi-than/lib/tinh-nang/do-gui/     # xem, xác nhận, đồng ý
```

**Structure Decision**: Theo R-02 của 000. Feature 013 nằm ở `backend/…/m12` với bảy gói con và gói `api`.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Cột suy ra trên `do_gui` | SC-008 tra cứu một đồ không mở bản ghi; điều kiện kết thúc, kiểm kê đọc nhanh | Tính từ bản ghi gần nhất mỗi lần đọc: truy vấn nặng cho danh sách |
| Phiên bản từng đồ trong thân lệnh | Một lệnh có nhiều đồ; FR-011b đòi từ chối lệnh dựa trên trạng thái cũ | `If-Match` chỉ cho một tài nguyên |
| `lan_chan_tra` ghi ở transaction riêng | Lần chặn phải còn sau khi lệnh Trả bị từ chối (FR-016) | Ghi chung transaction: bị rollback cùng lệnh |

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
