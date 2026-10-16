# Implementation Plan: Quản lý dinh dưỡng và suất ăn

**Branch**: `011-nutrition-management` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/011-nutrition-management/spec.md`

## Summary

Feature 011 là phần dinh dưỡng của module M08 (mục 12.1 → 12.6; UC-45 → UC-48, UC-75 → UC-78). Kho nguyên liệu (12.7) thuộc feature 019. Feature cung cấp:
1. Danh mục chế độ ăn (hạn chế, món an toàn, mặc định), món ăn do Nhân viên bếp lập và Bác sĩ xác nhận, bữa ăn, loại đồ ăn bị cấm (mục A, Q-308).
2. Bản gán chế độ ăn do Bác sĩ lập và áp dụng, có lý do chuyên môn khi thuộc FR-013 (a)–(d) (mục B, Q-283).
3. Đối chiếu xung đột món với dị ứng và hạn chế ở mọi điểm (mục C).
4. Thực đơn tuần do Nhân viên bếp lập, kiểm tra độ phủ, lặp món, dị ứng trong một lần; Bác sĩ công bố; đổi món có lý do (mục D).
5. Chốt suất bằng lệnh của Nhân viên bếp, phát sinh sau chốt, suất giữ, món an toàn, suất ăn người thân (mục E, Q-306).
6. Phiếu bữa ăn theo tầng/khu, suất đặc biệt, giao, nhận, sai lệch, phần bổ sung (mục F).
7. Xác nhận phục vụ suất đặc biệt, chặn khi xung đột dị ứng và ghi sự cố qua 007 khi đã ăn (mục G, Q-307).
8. Đồ ăn gia đình mang vào, báo Bác sĩ xem lại chế độ ăn, lưu mẫu thức ăn (mục H, I, J).

Hướng kỹ thuật:
- Gói `m08` với mười gói con; 019 thêm `m08.kho` và cài port nội bộ `TonKhoNguyenLieuPort` (research M-01, M-12).
- Món ăn có phiên bản nội dung đã xác nhận; kiểm tra, chốt, phục vụ đọc phiên bản Hiệu lực tại lúc đó (M-04).
- `XungDot`, `CanLyDoChuyenMon`, `KiemTraDoPhu`, `LapMon`, `NguoiDuocTinh`, `TinhPhatSinh`, `ChonMonThayThe`, `DoiChieuDoAn`, `MonCanLuuMau` là hàm thuần kiểm thử bằng bảng.
- Lệnh Chốt suất tính lại tại lúc lệnh, lập lần chốt, phiếu mỗi tầng/khu, suất ăn người thân trong một transaction có khóa `bua_ngay`; DBR-27 giữ bằng `UNIQUE (bua_ngay_id, tang_khu_vuc_id)` và việc chỉ hai lệnh (chốt, xác nhận phát sinh) đổi số suất (M-08, M-09).
- Thay đổi ở module khác thành phát sinh qua listener sau commit, idempotent theo khóa sự kiện, chỉ xét bữa đã chốt chưa tới giờ (M-08).
- DTO cho bếp là lớp riêng không có trường sức khỏe (SC-008, SC-014; M-09).
- Lịch bữa cho 005 qua bean `NguonCongViecPort` loại `BUA` (kèm `xacNhanPhucVu`); bữa bị bỏ vì vắng gọi `nguonBiHuy` (M-08).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 011 không tự chốt suất, không lập phiếu ngoài lệnh của bếp, không tạo yêu cầu xem lại chế độ ăn, không tự tạo sự cố;
- hệ thống chỉ: tính sẵn khi bếp mở lệnh; tạo phát sinh kèm thay đổi do actor gây ra; các việc spec cho phép ở mục Actor (tự dùng món an toàn FR-037a, phát sinh ghi nhận suất giữ FR-040a, chuyển đồ ăn tới hạn dùng FR-055); chuyển trạng thái theo giờ; kiểm tra và chặn; nhắc, thông báo, cảnh báo qua 007.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 010. Không thêm thư viện.

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test bảng cho chín hàm thuần (research M-15); SC-001 với bộ 7 ngày × 60 người.
- Test tích hợp MySQL cho `UNIQUE` (bản gán chờ và hiệu lực, lần chốt, phiếu, thực đơn mỗi tuần, chế độ mặc định) và trigger nhóm 3.
- Test đồng thời: hai lệnh chốt; hai Bác sĩ áp dụng bản gán; xác nhận phát sinh và giao cùng lúc.
- Đồng hồ giả lập cho CFG-M08-01, 03 → 07, giờ bữa, CFG-M13-06.
- Bản giả `LuotOLaiAnPort`, `ChuyenDiBuaAnPort`, `TonKhoNguyenLieuPort` và bộ phát sự kiện của 001, 003, 004, 005, 007.

**Target Platform**: Như 000.
- Web: danh mục, bản gán, thực đơn (lập, kiểm tra, công bố, đổi món), tính sẵn và chốt suất, phiếu và phát sinh cho bếp, phiếu cho tầng, đồ ăn gia đình, lưu mẫu.
- App nhân viên: nhận phiếu, báo sai lệch, yêu cầu suất bổ sung, xác nhận phục vụ suất đặc biệt (ghi ngoại tuyến, Q-01), ghi đồ ăn gia đình.
- App người thân, cổng web: thực đơn chung và chế độ ăn riêng qua 012 (FR-028).

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Chốt suất một bữa cho 300 người ≤ 1 phút (SC-006): đọc lô vị trí, lượt vắng, lịch bán trú, dị ứng một lần; ghi lô.
- Phát sinh ≤ 5 phút sau thay đổi (SC-002): listener ngay sau commit; chỉ xét bữa đã chốt chưa tới giờ trong ngày.
- Kiểm tra thực đơn 6 chế độ ăn × 7 ngày × 3 bữa trong một lần ≤ 5 giây (SC-009).
- Quy mô: 300 người, 3 → 5 bữa mỗi ngày, khoảng 10 tầng/khu.

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI).
- Lần chốt, người được tính, phát sinh, xác nhận phục vụ, sai lệch, lưu mẫu không bị `UPDATE`, `DELETE` ngoài cột đặt một lần (constitution XII).
- Bếp không nhận dữ liệu sức khỏe (FR-046). Nhật ký trong transaction (000 FR-047). CFG đọc lúc chạy; giờ bữa chốt vào `bua_ngay` lúc tạo.

**Scale/Scope**:
- 7 thực thể có vòng đời (món, bản gán, thực đơn, phiếu, đồ ăn gia đình, suất ăn người thân, suất đặc biệt); khoảng 35 bảng (gồm bảng con, kỹ thuật).
- 2 job (theo phút, hằng ngày).
- 39 đường dẫn, 48 thao tác REST ([contracts/api-011.yaml](contracts/api-011.yaml)).
- 5 SPI 011 cài cho module thấp hơn; 2 SPI và 2 sự kiện khai báo cho 012, 014; 1 port nội bộ cho 019; 4 giao diện công khai.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR; x-fr, x-br trên mọi thao tác REST |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Món, bản gán, thực đơn, phiếu, đồ ăn gia đình chỉ có lệnh theo bảng trạng thái; chỉ danh mục nhóm 1 có sửa |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3, 4, 5, 7, 9 khớp năm bảng trạng thái của spec; quickstart mục 4 |
| V. Không cố định giá trị | Đạt | CFG-M08-01, 03 → 07, CFG-M13-06, CFG-M15-03, 04, 07 đọc từ `ThamSoApi` |
| VI. Vai trò, quyền | Đạt | Tách Nhân viên bếp (lập) / Bác sĩ (xác nhận, công bố) theo Q-308; người nhận tại tầng qua 002 (M-14) |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ; điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-014 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 007 được ghi thêm 011 là bên gọi `SuCoLenhApi` (mục "Đồng bộ") |
| X. Kiến trúc | Đạt | Một khối; gói `m08`; module khác dùng `m08.api`. Không có vòng (M-02). REST; MySQL; hai client (Q-346) |
| XI. Nghiệp vụ ở backend | Đạt | Đối chiếu, độ phủ, tính suất, phát sinh, chặn phục vụ ở backend; client chỉ hiển thị |
| XII. Dữ liệu | Đạt | DBR-27 bằng `UNIQUE` và lệnh đổi số suất duy nhất; nhóm 3 bằng trigger; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V; giờ bữa lưu trên `bua_ngay` để đổi CFG-M08-04 không làm lệch bữa đã chốt |
| XIV. Ngoại tuyến, thông báo | Đạt | Xác nhận phục vụ ghi ngoại tuyến qua `NgoaiTuyenHandler`, kiểm tra lại khi đồng bộ. Thông báo theo mức FR-065; cảnh báo qua 007 |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR; mỗi BR-M08-01 → 16 có test (quickstart mục 4) |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Bổ sung kỹ thuật giải thích ở Complexity Tracking.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 011 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 người; đọc lô khi chốt |
| NFR-02 Hiệu năng | Có | Chốt ≤ 1 phút; kiểm tra thực đơn một lần |
| NFR-03 Thông báo khẩn cấp | Không | 011 không có mức Khẩn cấp |
| NFR-04 Tác vụ định kỳ | Có | Job theo phút, hằng ngày; cột "đã làm lúc"; chạy bù |
| NFR-05 Khả dụng | Gián tiếp | Listener chạy lại khi outbox giao lại; idempotent |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL |
| NFR-07 Lưu trữ | Có | Bản gán, xác nhận phục vụ, đồ ăn gia đình theo hồ sơ (CFG-M15-03); phần còn lại theo CFG-M15-04 |
| NFR-08 Bảo mật | Có | DTO bếp không có trường sức khỏe; dị ứng chỉ với người có quyền |
| NFR-09 Thời gian | Có | `Clock`; giờ bữa, tuần theo Asia/Ho_Chi_Minh |
| NFR-10 Thiết bị | Có | Web đủ chức năng; app nhân viên cho tầng |
| NFR-11 Ngôn ngữ | Có | Tiếng Việt; giờ HH:mm |
| NFR-12 Cấu hình | Có | CFG-M08 đổi qua tham số |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả port |

## Project Structure

### Documentation (this feature)

```text
specs/011-nutrition-management/
├── plan.md                      # File này
├── research.md                  # Phase 0: M-01 → M-15
├── data-model.md                # Phase 1: bảng, bảng chuyển, DBR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng Q1 → Q49, truy vết → test
├── contracts/
│   ├── api-011.yaml             # OpenAPI 3.1: danh mục, bản gán, thực đơn, chốt suất, phiếu, phục vụ, đồ ăn gia đình, lưu mẫu
│   └── module-interfaces.md     # SPI 011 cài, API cung cấp, SPI cho 012, 014, port nội bộ cho 019, sự kiện
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m08/
├── api/           # CheDoAnApi, ThucDonChungApi, LichBuaApi, DinhDuongThongKeApi, LuotOLaiAnPort, ChuyenDiBuaAnPort, sự kiện
├── danhmuc/       {internal, web}   # chế độ ăn, món và phiên bản, bữa, loại cấm
├── chedoan/       {internal, web}   # bản gán, CanLyDoChuyenMon, bean trạng thái
├── doichieu/      {internal}        # XungDot, ChonMonThayThe, listener dị ứng
├── thucdon/       {internal, web, port}   # thực đơn, KiemTraDoPhu, LapMon, đổi món, TonKhoNguyenLieuPort
├── chotsuat/      {internal, web}   # bữa ngày, NguoiDuocTinh, lần chốt, TinhPhatSinh, suất giữ, món an toàn, suất người thân
├── phieu/         {internal, web}   # phiếu, suất đặc biệt, sai lệch, phần bổ sung, DTO bếp/tầng
├── phucvu/        {internal, web}   # xác nhận phục vụ, NguonCongViecPort (BUA), NgoaiTuyenHandler
├── doangiadinh/   {internal, web}   # DoiChieuDoAn
├── xemlai/        {internal}        # listener AnKemKeoDai, SutCanPhatHien
├── luumau/        {internal, web}   # MonCanLuuMau
└── lich/                            # ViecTheoPhutM08, ViecHangNgayM08

frontend/src/tinh-nang/dinh-duong/
├── danh-muc/  ├── ban-gan/  ├── thuc-don/  ├── chot-suat/  ├── phieu/  ├── do-an-gia-dinh/  └── luu-mau/

mobile/lib/nhan-vien/dinh-duong/   # nhận phiếu, sai lệch, suất bổ sung, xác nhận phục vụ, đồ ăn gia đình
```

**Structure Decision**: Theo R-02 của 000. Feature 011 nằm ở `backend/…/m08` với mười một gói con và gói `api`; 019 thêm `m08.kho`. Web có đủ chức năng; app nhân viên có phần việc tại tầng.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Phiên bản nội dung món (`mon_an_phien_ban`) | FR-004a: món Hiệu lực có bản sửa chờ, nội dung đã xác nhận vẫn dùng; lần chốt, xác nhận phục vụ phải biết đúng nội dung đã dùng | Sửa thẳng món: mất nội dung đã xác nhận khi có bản sửa chờ, không truy được thành phần lúc phục vụ |
| Bảng `bua_ngay` kỹ thuật | Một điểm khóa cho chốt, phát sinh, phiếu của một bữa; giữ giờ bữa tại lúc tạo | Khóa theo (bữa, ngày) không có dòng: không khóa được bằng `SELECT … FOR UPDATE` |
| DTO bếp tách riêng | SC-008, SC-014 đòi 0 lần lộ dữ liệu sức khỏe | Lọc trường động theo vai trò: dễ lọt trường mới thêm |
| SPI `LuotOLaiAnPort`, `ChuyenDiBuaAnPort` ở `m08.api` | 011 làm plan trước 012, 014 nhưng cần dữ liệu của họ (FR-034) | 011 phụ thuộc 012, 014: tạo vòng vì 012, 014 đọc thực đơn, lịch bữa |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung hoặc dựa vào các điểm dưới đây; plan của feature liên quan giữ đúng hoặc ghi rõ thay đổi.

1. **007** (đã ghi vào [../007-health-alert-incident/contracts/module-interfaces.md](../007-health-alert-incident/contracts/module-interfaces.md) mục 1.6): 011 là bên gọi `SuCoLenhApi.ghiNhan` khi khai "đã ăn" (loại `PHUC_VU_SAI_SUAT_AN`, nguồn `AN_UONG`). 011 phát `YeuCauTaoCanhBao` với `loaiNguon = THUC_DON_MON` hoặc `SUAT_DAC_BIET` và `nguonId` kiểu số, đúng chữ ký mục 1.13; nghe `SutCanPhatHien`.
2. **005**: sửa chú thích `xacNhanPhucVu` trong [../005-daily-care-tasks/contracts/module-interfaces.md](../005-daily-care-tasks/contracts/module-interfaces.md) từ "011 FR-040a" thành "011 FR-050" (không đổi chữ ký).
3. **001, 003, 004, 005**: dùng API và sự kiện đã khai báo (001 `MucSucKhoeApi`, `DiNguyenApi`, `DiUng*`; 003 `ViTriApi`, `DaChuyenGiuong`; 004 `HopDongDocApi.lichDenTaiNgay`, `LuotVang*`, `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi`; 005 `NguonCongViecPort` loại `BUA` với `xacNhanPhucVu`, `nguonBiHuy`, `CoMatBanTruApi`, `AnKemKeoDai`). Không đổi.
4. **010**: phát `m11.api.SuKienTinhPhiNguon` / `SuKienTinhPhiNguonDaDoi` loại `SUAT_AN_NGUOI_THAN`, đúng chữ ký 010 mục 1.5.
5. **012** cài `LuotOLaiAnPort`, phát `LuotOLaiDaDoi`, đọc `ThucDonChungApi`, `CheDoAnApi`. **014** cài `ChuyenDiBuaAnPort`, phát `ChuyenDiDaDoi`. **019** (cùng M08) cài `TonKhoNguyenLieuPort`. **016** đọc `DinhDuongThongKeApi`. **009**: thông báo theo FR-065 cài `CoYeuCauThongBao`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Việc hệ thống tự làm theo giờ ở mục E** (FR-037a món an toàn tại mốc CFG-M08-05, FR-040a phát sinh "−1" ghi nhận suất giữ tại giờ bữa): mục Actor của spec cho phép rõ, plan làm theo. Tuy vậy danh sách "vẫn giữ" của Q-306, Q-307 (tự chuyển trạng thái theo giờ, cảnh báo, nhắc, kiểm tra, tác động kèm lệnh, số liệu hiển thị) không nêu "đổi nội dung suất theo giờ". Đề nghị ghi hai ngoại lệ này vào dòng Q-307 ở 24.2 hoặc BR-M08-01, để các feature khác không hiểu Q-307 chặt hơn. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

2. **Phát sinh "kèm lệnh của actor gây thay đổi"** khi actor thuộc module khác (chuyển giường, trở về, vắng, điểm danh chuyến, ở lại): plan tạo phát sinh ngay sau commit của lệnh đó qua sự kiện (vài giây, trong SC-002 5 phút), không cùng transaction. Kết quả nghiệp vụ không đổi; ghi lại cách hiểu "kèm lệnh" giống plan 010 điểm 1. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Câu chữ còn lẫn trong spec 011** (không đổi nghiệp vụ): *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - FR-048, mục F, FR-063 còn dẫn "4.4" (Permission Matrix của `docs/phan-tich-yeu-cau.md` đã bỏ); plan tra theo Phụ lục 27 qua bảng ánh xạ của Phụ lục 26.
