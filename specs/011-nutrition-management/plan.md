# Implementation Plan: Quản lý dinh dưỡng và suất ăn

**Branch**: `011-nutrition-management` | **Date**: 2026-10-10, cập nhật 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/011-nutrition-management/spec.md`

## Summary

Feature 011 là phần dinh dưỡng của module M08 (mục 12.1 → 12.6; UC-45 → UC-48, UC-75 → UC-78). Kho nguyên liệu (12.7) bỏ theo Q-392. Feature cung cấp:
1. Danh mục chế độ ăn (hạn chế, món an toàn, mặc định), món ăn do Nhân viên bếp lập và Bác sĩ xác nhận, bữa ăn, loại đồ ăn bị cấm (mục A, Q-308).
2. Bản gán chế độ ăn do Bác sĩ lập và áp dụng, có lý do chuyên môn khi thuộc FR-013 (a)–(d) (mục B, Q-283).
3. Đối chiếu xung đột món với dị ứng và hạn chế ở mọi điểm (mục C).
4. Thực đơn tuần do Nhân viên bếp lập, kiểm tra độ phủ, lặp món, dị ứng trong một lần; Bác sĩ công bố; đổi món có lý do (mục D).
5. Chốt suất bằng lệnh của Nhân viên bếp, phát sinh sau chốt, suất giữ, món an toàn, suất ăn người thân (mục E, Q-306).
6. Phiếu bữa ăn theo tầng/khu, suất đặc biệt, giao, nhận, sai lệch, phần bổ sung (mục F).
7. Xác nhận phục vụ suất đặc biệt, chặn khi xung đột dị ứng và ghi sự cố qua 007 khi đã ăn (mục G, Q-307).
8. Đồ ăn gia đình mang vào, báo Bác sĩ xem lại chế độ ăn, lưu mẫu thức ăn (mục H, I, J).

Hướng kỹ thuật:
- Gói `nutrition` với mười gói con (research M-01); `nutrition.kho` và `TonKhoNguyenLieuPort` bỏ theo Q-392.
- Món ăn có phiên bản nội dung đã xác nhận; kiểm tra, chốt, phục vụ đọc phiên bản Hiệu lực tại lúc đó (M-04).
- `XungDot`, `CanLyDoChuyenMon`, `KiemTraDoPhu`, `LapMon`, `NguoiDuocTinh`, `TinhPhatSinh`, `ChonMonThayThe`, `DoiChieuDoAn`, `MonCanLuuMau` là hàm thuần kiểm thử bằng bảng.
- Lệnh Chốt suất tính lại tại lúc lệnh, lập lần chốt, phiếu mỗi tầng/khu, suất ăn người thân trong một transaction có khóa `bua_ngay`; DBR-27 giữ bằng `UNIQUE (bua_ngay_id, tang_khu_vuc_id)` và việc chỉ hai lệnh (chốt, xác nhận phát sinh) đổi số suất (M-08, M-09). *(R-15: `bua_ngay` → `khoa_ky_thuat`)*
- Thay đổi ở module khác thành phát sinh qua listener sau commit, idempotent theo khóa sự kiện, chỉ xét bữa đã chốt chưa tới giờ (M-08).
- DTO cho bếp là lớp riêng không có trường sức khỏe (SC-008, SC-014; M-09).
- Lịch bữa cho 005 qua bean `NguonCongViecPort` loại `BUA` (kèm `xacNhanPhucVu`); bữa bị bỏ vì vắng gọi `nguonBiHuy` (M-08).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 011 không tự chốt suất, không lập phiếu ngoài lệnh của bếp, không tạo yêu cầu xem lại chế độ ăn, không tự tạo sự cố;
- hệ thống chỉ: tính sẵn khi bếp mở lệnh; tạo phát sinh kèm thay đổi do actor gây ra; các việc spec cho phép ở mục Actor (tự dùng món an toàn FR-037a, phát sinh ghi nhận suất giữ FR-040a, chuyển đồ ăn tới hạn dùng FR-055); chuyển trạng thái theo giờ; kiểm tra và chặn; nhắc, thông báo, cảnh báo qua 007.

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": feature 009 FR-001a)

- **Đối tượng thông tin** (FR-065, 009 FR-001a): thông báo "Đồ ăn gia đình Không sử dụng" tới người gửi khai `doiTuongThongTin = VE_VIEC_NGUOI_THAN`; lý do có nội dung sức khỏe đặt ở phần `SUC_KHOE`; các thông báo khác chỉ tới nhân viên; không yêu cầu nào mang mã Q-354 (research M-13).

### Cập nhật 2026-10-18, lượt 3 (spec mục "Cập nhật 2026-10-18, lượt 3": Q-366)

- **Suất ăn dự kiến cho kênh người cao tuổi** (FR-063 dòng "Người cao tuổi"): hàm đọc mới `nutrition.api.SuatAnDuKienApi.cuaNguoiCaoTuoi(nctId, tu, den)` cho 012: món dự kiến theo ngày, bữa từ thực đơn đã Công bố hoặc Đã áp dụng và chế độ ăn hiện hành của người cao tuổi, kèm món thay thế; chỉ trả tên món (research M-16). Không thêm bảng.

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
- Bản giả `LuotOLaiAnPort`, `ChuyenDiBuaAnPort` và bộ phát sự kiện của 001, 003, 004, 005, 007.

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
- Bếp không nhận dữ liệu sức khỏe (FR-046). Nhật ký trong transaction (000 FR-047). CFG đọc lúc chạy; giờ bữa chốt vào `bua_ngay` lúc tạo. *(R-15: `bua_ngay` → `khoa_ky_thuat`)*

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
| X. Kiến trúc | Đạt | Một khối; gói `nutrition`; module khác dùng `nutrition.api`. Không có vòng (M-02). REST; MySQL; hai client (Q-346) |
| XI. Nghiệp vụ ở backend | Đạt | Đối chiếu, độ phủ, tính suất, phát sinh, chặn phục vụ ở backend; client chỉ hiển thị |
| XII. Dữ liệu | Đạt | DBR-27 bằng `UNIQUE` và lệnh đổi số suất duy nhất; nhóm 3 bằng trigger; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V; giờ bữa lưu trên `bua_ngay` để đổi CFG-M08-04 không làm lệch bữa đã chốt *(R-15: `bua_ngay` → `khoa_ky_thuat`)* |
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
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/eldercare/backend/nutrition/
├── api/           # CheDoAnApi, ThucDonChungApi, LichBuaApi, DinhDuongThongKeApi, LuotOLaiAnPort, ChuyenDiBuaAnPort, sự kiện
├── danhmuc/       {internal, web}   # chế độ ăn, món và phiên bản, bữa, loại cấm
├── chedoan/       {internal, web}   # bản gán, CanLyDoChuyenMon, bean trạng thái
├── doichieu/      {internal}        # XungDot, ChonMonThayThe, listener dị ứng
├── thucdon/       {internal, web, port}   # thực đơn, KiemTraDoPhu, LapMon, đổi món
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

**Structure Decision**: Theo R-02 của 000. Feature 011 nằm ở `backend/…/nutrition` với mười một gói con và gói `api`. Web có đủ chức năng; app nhân viên có phần việc tại tầng.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Phiên bản nội dung món (`mon_an_phien_ban`) | FR-004a: món Hiệu lực có bản sửa chờ, nội dung đã xác nhận vẫn dùng; lần chốt, xác nhận phục vụ phải biết đúng nội dung đã dùng | Sửa thẳng món: mất nội dung đã xác nhận khi có bản sửa chờ, không truy được thành phần lúc phục vụ *(R-15: `mon_an_phien_ban` → `ho_so_chung` (phiên bản món, khóa khi Bác sĩ xác nhận; thành phần, chế độ thành `JSON`))* |
| Bảng `bua_ngay` kỹ thuật | Một điểm khóa cho chốt, phát sinh, phiếu của một bữa; giữ giờ bữa tại lúc tạo | Khóa theo (bữa, ngày) không có dòng: không khóa được bằng `SELECT … FOR UPDATE` *(R-15: `bua_ngay` → `khoa_ky_thuat`)* |
| DTO bếp tách riêng | SC-008, SC-014 đòi 0 lần lộ dữ liệu sức khỏe | Lọc trường động theo vai trò: dễ lọt trường mới thêm |
| SPI `LuotOLaiAnPort`, `ChuyenDiBuaAnPort` ở `nutrition.api` | 011 làm plan trước 012, 014 nhưng cần dữ liệu của họ (FR-034) | 011 phụ thuộc 012, 014: tạo vòng vì 012, 014 đọc thực đơn, lịch bữa |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung hoặc dựa vào các điểm dưới đây; plan của feature liên quan giữ đúng hoặc ghi rõ thay đổi.

1. **007** (đã ghi vào [../007-health-alert-incident/contracts/module-interfaces.md](../007-health-alert-incident/contracts/module-interfaces.md) mục 1.6): 011 là bên gọi `SuCoLenhApi.ghiNhan` khi khai "đã ăn" (loại `PHUC_VU_SAI_SUAT_AN`, nguồn `AN_UONG`). 011 phát `YeuCauTaoCanhBao` với `loaiNguon = THUC_DON_MON` hoặc `SUAT_DAC_BIET` và `nguonId` kiểu số, đúng chữ ký mục 1.13; nghe `SutCanPhatHien`.
2. **005**: sửa chú thích `xacNhanPhucVu` trong [../005-daily-care-tasks/contracts/module-interfaces.md](../005-daily-care-tasks/contracts/module-interfaces.md) từ "011 FR-040a" thành "011 FR-050" (không đổi chữ ký).
3. **001, 003, 004, 005**: dùng API và sự kiện đã khai báo (001 `MucSucKhoeApi`, `DiNguyenApi`, `DiUng*`; 003 `ViTriApi`, `DaChuyenGiuong`; 004 `HopDongDocApi.lichDenTaiNgay`, `LuotVang*`, `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi`; 005 `NguonCongViecPort` loại `BUA` với `xacNhanPhucVu`, `nguonBiHuy`, `CoMatBanTruApi`, `AnKemKeoDai`). Không đổi.
4. **010**: phát `billing.api.SuKienTinhPhiNguon` / `SuKienTinhPhiNguonDaDoi` loại `SUAT_AN_NGUOI_THAN`, đúng chữ ký 010 mục 1.5.
5. **012** cài `LuotOLaiAnPort`, phát `LuotOLaiDaDoi`, đọc `ThucDonChungApi`, `CheDoAnApi`. **014** cài `ChuyenDiBuaAnPort`, phát `ChuyenDiDaDoi`. **016** đọc `DinhDuongThongKeApi`. **009**: thông báo theo FR-065 cài `CoYeuCauThongBao`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Việc hệ thống tự làm theo giờ ở mục E** (FR-037a món an toàn tại mốc CFG-M08-05, FR-040a phát sinh "−1" ghi nhận suất giữ tại giờ bữa): mục Actor của spec cho phép rõ, plan làm theo. Tuy vậy danh sách "vẫn giữ" của Q-306, Q-307 (tự chuyển trạng thái theo giờ, cảnh báo, nhắc, kiểm tra, tác động kèm lệnh, số liệu hiển thị) không nêu "đổi nội dung suất theo giờ". Đề nghị ghi hai ngoại lệ này vào dòng Q-307 ở 24.2 hoặc BR-M08-01, để các feature khác không hiểu Q-307 chặt hơn. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

2. **Phát sinh "kèm lệnh của actor gây thay đổi"** khi actor thuộc module khác (chuyển giường, trở về, vắng, điểm danh chuyến, ở lại): plan tạo phát sinh ngay sau commit của lệnh đó qua sự kiện (vài giây, trong SC-002 5 phút), không cùng transaction. Kết quả nghiệp vụ không đổi; ghi lại cách hiểu "kèm lệnh" giống plan 010 điểm 1. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Câu chữ còn lẫn trong spec 011** (không đổi nghiệp vụ): *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - FR-048, mục F, FR-063 còn dẫn "4.4" (Permission Matrix của `docs/phan-tich-yeu-cau.md` đã bỏ); plan tra theo Phụ lục 27 qua bảng ánh xạ của Phụ lục 26.

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Ánh xạ bảng ở data-model mục 0; task T046. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.

## Ghi chú triển khai *(2026-11-06, Phase 1–2)*

- Tên Anh theo glossary mục 17 (chủ dự án duyệt miệng 2026-11-06): cổng 012, 014 là `StayMealPort`, `TripMealPort`; sự kiện `StayMealChanged`, `TripMealChanged`; API đọc `DietApi`, `GeneralMenuApi`, `ExpectedMealApi`, `MealScheduleApi`, `NutritionStatisticsApi`.
- Bản giả đặt ở `support/fake/nutrition/` thay cho `BT/support/gia011/` của tasks (team-plan mục 3: bản giả của chủ interface ở `fake/<module>/`).
- Sau R-15, Phase 2 không có DDL: `V011_01__nen_m08.sql` không tạo vì cả 5 bảng của T004 đã gộp vào bảng chung. Ba bảng riêng tạo ở phase của user story; hằng `SharedKind` `M08.*` khai cùng chỗ dùng đầu tiên.
- Khung job: việc đăng ký bằng bean `M08MinuteTask`, `M08DailyTask` (thay cho mã bảng `lan_chay_m08`); chống lặp và chạy bù bằng dấu `M08.JOB.<khóa>` trên `reminder_marker`; listener dùng `M08EventListener.once(eventKey, work)` (dấu `M08.PROCESSED_EVENT`, khóa dài hơn 80 ký tự dùng SHA-256 hex).
- `RetentionProvider`, `ScopeAssignmentProvider` (T007) chuyển sang phase tạo thực thể tương ứng vì cần dữ liệu thật; chỉ `FieldClassificationProvider` làm ở Phase 2. Sự kiện thông báo FR-065 (T003) cũng làm cùng lệnh kích hoạt.

## Ghi chú triển khai *(2026-11-06, Phase 3)*

- `contracts/api-011.yaml` đã dịch tại chỗ sang tên Anh. Kết quả có 39 path, 48 thao tác, đúng số của bản tiếng Việt. Glossary mục 17 thêm các tên `weekStart`, `copyFromId`, `processingCheckConfirmed`, `processingNote`, `confirmedWarnings`, `content`, `KitchenMenuWarning`.
- Không tạo `V011_02`. Chế độ ăn và món nằm trong `catalog_item`; phiên bản món là `simple_record` kind `M08.DISH_VERSION`, cha là món. Mỗi món chỉ có một bản mở nhờ `uniqueKey` `M08.DISH_VERSION.OPEN:<dishId>`. Ràng buộc một chế độ mặc định là cột sinh `m08_default_diet` có `UNIQUE`, đặt trong `V011_94__shared_m08_default_diet.sql` vì đây là bảng chung.
- Lớp: `nutrition.internal.catalog.{domain, persistence, service}` và `nutrition.web.NutritionCatalogController`. Thao tác cài thay cho `CheDoAnService`, `MonAnService`, `DanhMucController`.
- `POST /dishes/{id}/{command}`: một lệnh do bếp hoặc Bác sĩ thực hiện tùy loại lệnh (Q-308), nên `DishService.command` tự kiểm quyền: bếp cần `DISH_MENU UPDATE`, Bác sĩ cần `APPROVE`. Phương thức này được thêm vào `RequiresPermissionArchTest.SELF_CHECKED`.
- Trả lại hoặc rút một bản thì bản đó bị đóng và khóa, rồi một bản Nháp mới được mở, chép nội dung cũ. Như vậy lịch sử phiên bản không bị ghi đè (FR-004a).
- Quyết định nghiệp vụ của chủ dự án ngày 2026-11-06, ghi theo lúc hỏi:
  - lúc lưu chế độ ăn không xét điều kiện món an toàn "phù hợp";
  - chưa có món Hiệu lực nào thì chế độ ăn đầu tiên được để trống món an toàn;
  - đặt mặc định mới thì bỏ mặc định cũ trong cùng lệnh (khóa `M08.DIET.DEFAULT`).
- Chưa làm ở phase này:
  - bữa ăn và loại đồ ăn bị cấm: chờ A thêm Function quyền danh mục dinh dưỡng;
  - `dish-conflict-check`: Phase 5;
  - kiểm ngừng chế độ ăn khi còn bản gán hoặc thực đơn, và kiểm lại thực đơn khi đổi hạn chế: Phase 4 và Phase 5;
  - màn web: chờ duyệt bố cục.

## Ghi chú triển khai *(2026-11-06, Phase 4)*

- Lớp: `nutrition.internal.assignment.{domain, persistence, service}` và `nutrition.web.DietAssignmentController`. Thay cho `BanGanService`: `DietAssignmentService`; hàm thuần `CanLyDoChuyenMon` là `ProfessionalReasonRule`. Migration `V011_03__diet_assignment.sql` (xem glossary mục 17.6).
- **Đồng thời**: lệnh lập, áp dụng, hủy và việc chuyển Chờ hiệu lực → Hiệu lực chạy `READ_COMMITTED` như 005 (`CarePlanService`). Lý do: MySQL mặc định `REPEATABLE READ`, lệnh thứ hai khóa `M08.RESIDENT_NUTRITION` xong vẫn đọc lại bản chụp cũ nên cả hai cùng áp dụng được (`BR_M08_03_ConcurrentApplyIT` lỗi lúc đầu, trả 200 và 200). Hai tác động chạy trong lệnh của 001 (`onFinalStatus`, `onAdmissionCompleted`) dùng transaction của 001 và đọc dưới khóa người cao tuổi.
- **Hợp đồng**: `POST /residents/{residentId}/diet-assignments` trả `MenuCheckResult` đúng hợp đồng, nên id bản gán mới nằm ở header `Location`. `GET` trả thêm một dòng `isDefault = true` (chế độ ăn mặc định của viện) khi người đó chưa có bản Hiệu lực (FR-014); dòng này không có id, trạng thái.
- **Quyền xem**: Phụ lục 27 dòng `DIET` cho Quản lý viện, Bác sĩ, Điều dưỡng và cả bếp (AC-07) quyền `VIEW` để xem danh mục, nhưng FR-063 cấm bếp xem bản gán. Chủ dự án chọn (2026-11-06): `DietAssignmentService.list` tự loại vai trò ngoài Bác sĩ, Điều dưỡng, Quản lý viện (403 `PERMISSION_DENIED`, `failedLayer` `ROLE`); không tách Function. Không cần thêm vào `SELF_CHECKED` vì `list` không phải `@BusinessCommand`.
- **Quyết định nghiệp vụ của chủ dự án (2026-11-06)**: (1) lập đề xuất với chế độ ăn thiếu món an toàn bị chặn `CONDITION_NOT_MET` mã `SAFE_DISH_MISSING` (FR-001); (2) `DietAssignmentCommand` không có trường lý do lúc lập, giữ đúng hợp đồng (FR-010 nhắc "lý do" nhưng hợp đồng không có; `personalInstruction` là ghi chú nhu cầu dinh dưỡng).
- **Hoãn sang phase sau** (chưa có dữ liệu): đối chiếu FR-016 với thực đơn (Phase 5: danh sách cảnh báo hiện rỗng, `confirmedWarnings` rỗng là đủ); phát sinh FR-017 và FR-015 cho bữa đã chốt chưa tới giờ (Phase 6); kiểm ngừng chế độ ăn khi còn thực đơn (`STILL_IN_MENU`, Phase 5); listener `OtherAllergyRecorded`, `AllergyExcluded` (Phase 5, 6, 9); thông báo "chưa gán chế độ ăn" ở mỗi lần chốt (Phase 6).
- Bản gán Hiệu lực ngay: bản cũ Ngừng và sự kiện `DietAssignmentTookEffect` phát trong cùng lệnh; với Chờ hiệu lực, hiệu lực thực tế là thời điểm hiệu lực mong muốn (FR-017), không phải lúc job chạy.

## Ghi chú triển khai *(2026-11-06, Phase 5)*

- Lớp: `nutrition.internal.menu.{domain, persistence, service}` và `nutrition.web.MenuController`. Thay cho `ThucDonService`: `MenuService`; hàm thuần `KiemTraDoPhu`, `LapMon` là `CoverageCheck`, `DishRepetitionCheck`; thêm hàm thuần `AllergyWarningRule` cho cảnh báo dị ứng (FR-025 c). Migration `V011_04__menu.sql` (glossary mục 17.6).
- **Đổi món dưới trigger**: thực đơn Công bố, Đã áp dụng chỉ đổi nội dung khi lệnh ghi kèm `last_swap_id` mới (id `record_event` `M08.DISH_SWAP` vừa ghi), thay cho trigger "đối chiếu trạng thái" của data-model mục 5 (nội dung đã gộp vào `JSON`, không còn dòng `thuc_don_mon`).
- **Người dùng chế độ ăn (FR-027)**: bản Hiệu lực và bản Chờ hiệu lực có hiệu lực trước hết tuần, của người chưa ở trạng thái cuối; người chưa gán (gồm cả Đang tiếp nhận) dùng chế độ mặc định (chủ dự án chốt 2026-11-06 theo chữ spec). Loại lưu trú đọc qua `ContractApi.currentResidenceType` từng ngày của tuần; không có hợp đồng thì coi là nội trú như 005 (`AttendanceSources`).
- **Mã cảnh báo**: `warningCodes`, `confirmedWarnings` là mã ổn định theo ô và món (glossary 17.4), không theo số người. Công bố chấp nhận các mã bếp đã xác nhận cộng mã Bác sĩ gửi kèm; Đổi món chấp nhận các mã đã có trước lệnh.
- **Tên người trong cảnh báo dị ứng**: chỉ khi người gọi có `ALLERGY_CONDITION VIEW` với người cao tuổi đó (kiểm từng người qua `PermissionApi`); bếp nhận `residentCount`, trường `residents` vắng mặt. `dish-conflict-check` cũng kiểm `ALLERGY_CONDITION VIEW` thay cho `DIET VIEW`, vì kết quả có thành phần gây dị ứng (FR-022) mà bếp có `DIET VIEW`.
- **Giờ bữa**: CFG-M08-04 chưa có định dạng; đọc `mealTimes` là đối tượng mã bữa → `"HH:mm"` (`MealTimes`). Quyết định của chủ dự án (2026-11-06): Đổi món cho ngày đã qua luôn chặn `PAST_MEAL_TIME`; hôm nay chỉ chặn khi bữa có giờ và đã qua; chưa đặt giờ thì cho đổi.
- **FR-016** (chuyển từ Phase 4): đối chiếu thực đơn tuần hiện tại (mọi trạng thái chưa hủy) và các tuần đã công bố, chỉ các ngày từ hôm nay; xung đột chưa có món thay thế (`CONFLICT:…`) và bữa không có món của chế độ ăn mới trong thực đơn đã công bố (`NO_DISH:…`, FR-038) phải được xác nhận khi Áp dụng.
- **Danh mục bữa**: khai `MealAttributes` (`servesResidential`, `servesDayCare`, theo `Meal` của api-011) để đọc; lệnh quản lý bữa vẫn chờ Function của A. Bỏ `MenuItem` và câu SQL đọc thẳng `catalog_item` trong `MenuStore` của bản nháp trước (trái R-15), tên bữa, món đọc qua `NutritionCatalogStore`.
- **Thông báo**: "Thực đơn Công bố" gửi `FacilityRole(KITCHEN_STAFF)`: `RecipientGroup` chưa có nhóm "Bếp đang trong ca" (`OnShiftStaff` cần tầng/khu vực); gửi kiểm tra và rút lại dùng chung `MenuSubmitted` (cờ `withdrawn`).
- **Hợp đồng**: `GET /menus/{id}`, `/pre-submit-info`, `POST /menus` (201) không khai schema phản hồi; controller khai `@Content` rỗng để khớp `ApiContractIT`, thân vẫn trả JSON theo mô tả thao tác (chi tiết: nội dung, ô cần thay, các lần kiểm tra, lịch sử đổi món; `diets[]` gồm `dietId`, `dietName`, `residentCount`). Id thực đơn mới ở header `Location`.
- **Sửa hợp đồng `DishSwap` (chủ dự án chọn 2026-11-06)**: FR-031, FR-032 cho Đổi món thêm, bớt món và món thay thế, bổ sung món cho bữa trống, nhưng bản dịch cũ bắt buộc `oldDishId`, `newDishId`. Nay chỉ `date`, `mealId`, `dietId`, `reason` bắt buộc: có cả hai là thay món; chỉ `newDishId` là thêm; chỉ `oldDishId` là bớt (chặn nếu bữa mất món của chế độ ăn đang dùng); `oldDishId` = `newDishId` kèm trường mới `substituteDishIds` là đổi món thay thế (vắng thì giữ danh sách cũ). `DishSwapRecord` lưu món thay thế trước/sau.
- **Hoãn**: phát sinh khi đổi món sau thời điểm chốt (FR-039, Phase 6); kiểm lại FR-005 với thực đơn chưa hết tuần khi sửa hạn chế chế độ ăn và thông báo (FR-002); listener `OtherAllergyRecorded`, `AllergyExcluded` và đối chiếu thực đơn khi có dị ứng mới (FR-021, gửi cảnh báo 007); `RetentionProvider`, phân loại trường của `menu` (T007).

## Ghi chú triển khai *(2026-11-06, Phase 6)*

- Lớp: `nutrition.internal.closing.{domain, persistence, service}` và `nutrition.web.PortionClosingController`. Thay cho `ChotSuatService`: `PortionClosingService` (chốt, tính lại) và `AdjustmentService` (lệnh trên phát sinh, suất đặc biệt); hàm thuần `NguoiDuocTinh`, `TinhPhatSinh` là `CountedResidents`, `AdjustmentCalculator`, thêm `PortionContent` (nội dung suất FR-036 → FR-038). Migration `V011_05__portion_closing.sql`.
- **Bữa ngày không có bảng**: `khoa_ky_thuat` (`technical_lock`) chỉ có khóa, không có id, nên id bữa ngày = id bữa × 100000 + số ngày từ 1970-01-01 (`DailyMeal.id`), khóa lệnh `M08.DAILY_MEAL:<id>`; giờ bữa, ngưỡng giao trễ đọc CFG-M08-04 tới khi chốt rồi ghi vào lần chốt (FR-006). "Đã làm lúc" của job là dấu `M08.JOB.<khóa>`.
- **Quyết định của chủ dự án (2026-11-06)**: (1) bữa chưa đặt giờ thì chặn chốt `MEAL_TIME_NOT_SET`, việc theo giờ bỏ qua bữa đó; (2) Phụ lục 27 dòng "Suất ăn đã chốt, phiếu bữa ăn" chỉ cho bếp X nên chốt suất dùng `MEAL_PREPARATION CREATE`, bếp xác nhận phát sinh `MEAL_PREPARATION CONFIRM`, xem `MEAL_COUNT_TICKET VIEW`; (3) Bác sĩ chỉ định món riêng dùng `DIET UPDATE`.
- **Phát sinh tính lại theo người**: trạng thái suất hiện hành = lần chốt + trạng thái sau mỗi phát sinh (`Adjustment.after`). Mỗi sự kiện nguồn tính lại suất của người liên quan ở mọi bữa đã chốt chưa tới giờ, `AdjustmentCalculator.diff` ra "+1", "−1" (đổi tầng/chế độ là "−1" chỗ cũ, "+1" chỗ mới), đổi suất đặc biệt (0 suất). Khóa phát sinh = khóa sự kiện + bữa ngày + người + loại (NFR-04). Gỡ dấu "dự kiến trở về" khi người về trước giờ bữa không tạo phát sinh (FR-040a). Suất thường (không đặc biệt, không dấu) đổi món theo thực đơn không là phát sinh từng người; đổi món ghi một phát sinh `DISH_SWAP` mỗi tầng/khu.
- **Người được tính** (FR-034): nội trú không có giường thì không được tính; bán trú chia theo khu bán trú như 005 (`AttendanceSources.floor`), không cấu hình thì không được tính; không có hợp đồng thì coi là nội trú. Trạng thái có mặt bán trú chưa có bản ghi được coi là Chưa đến. Món an toàn (FR-037a) áp khi ngoài giờ hành chính CFG-M13-06 hoặc từ mốc CFG-M08-05; món an toàn xung đột chỉ báo điều dưỡng ở lần chốt và ở mốc, không báo lại mỗi lần tính lại.
- **Suất ăn người thân**: nguồn chi phí `CostSourceBilled` (`FAMILY_MEAL`, `sourceId` id bản ghi, `unit` `PORTION`), hủy trước giờ bữa `CostSourceChanged` (`cancelled`); bản ghi Đã hủy khóa dòng, giữ để truy vết.
- **Hợp đồng**: `GET /daily-meals` không khai schema; controller khai `@Content` rỗng, thân trả `dailyMealId`, `mealId`, `mealName`, `date`, `expectedTime`, `closingOpensAt`, `closed`. Hợp đồng ghi mã chặn `CLOSED`, mã dùng là `ALREADY_CLOSED` (`DA_CHOT`, glossary 17.4). Danh sách phát sinh gồm cả yêu cầu suất bổ sung (`changeType` `EXTRA_PORTION_REQUEST`); `/adjustments/{id}/confirm` xác nhận cả hai loại.
- **Thông báo**: "Nhân viên bếp đang trong ca" gửi mọi nhân viên bếp như Phase 5.
- **Sự kiện của chính 011**: `DishSwapped`, `DishConfirmationResult` cài `HasNotificationRequests` nên không được nghe ngoài M13 (`M13ArchTest`); lệnh đổi món, xác nhận bản sửa gọi `PortionClosingService` trong cùng transaction (phát sinh đổi món ghi cùng lệnh, FR-031).
- **Test**: listener sau commit không chạy trong transaction của test, nên IT gọi `ClosingEventHandlers` thẳng; trạng thái người cao tuổi đổi bằng SQL thì xóa bộ đệm `EntityManager` (001 đọc qua JPA). Lệnh người nhận tại tầng (phạm vi `ASSIGNED` theo ca của 008) được mở bằng spy `PermissionApi` trong test.
- **Chờ người khác**: `TaskSourcePort` loại `MEAL` và báo bữa bị bỏ vì vắng chờ C (005) (team-plan mục 8); `AlertRequested` (`DIET_ALLERGY_CONFLICT`) của FR-021 đã phát nhưng 007 chưa có nơi nhận.
- **Hoãn**: `DBR_27_TongPhieuIT` và vòng đời phiếu (Phase 7); bộ SC-001 7 ngày × 60 người; test hai lệnh chốt đồng thời.

## Ghi chú triển khai *(2026-11-06, Phase 7)*

- Lớp: `nutrition.internal.slip.service.MealSlipService` (thay `PhieuService`), `LateSlipTask` (việc theo phút giao trễ, khóa `MEAL_SLIP_LATE`), `nutrition.web.MealSlipController`. Không tạo `V011_06`: phiếu, suất đặc biệt ở `simple_record` (`M08.MEAL_SLIP`, `M08.SPECIAL_PORTION`); sai lệch, phần bổ sung ở `record_event` (`M08.SLIP_DISCREPANCY`, `M08.SUPPLEMENTARY_PORTION`; xử lý, nhận đặt một lần rồi khóa dòng). `UNIQUE (bua_ngay_id, tang_khu_vuc_id)` là `uniqueKey` `M08.MEAL_SLIP:<bữa ngày>:<tầng>`.
- **Lỗi tìm thấy khi test**: `SimpleRecordApi.change` ghi lại cột `unique_key` mỗi lần đổi, truyền `null` là mất khóa phiếu. Khi đó lệnh sau lập phiếu thứ hai cho cùng tầng, sai DBR-27. `ClosingRecordStore.changeSlip` luôn truyền lại khóa.
- **Quyết định của chủ dự án (2026-11-07)**: (1) thêm vào `KitchenMealSlip` hai trường `labels[].id` và `supplementaryPortions[]` (`id`, `dietId`, `portionCount`, `deliveredAt`, `receivedAt`); (2) thêm lệnh `report-discrepancy` (thân `content`) cho `/supplementary-portions/{id}/{command}`; sai lệch này lưu như sai lệch phiếu, bếp xử lý bằng `resolve-discrepancy` của phiếu (xử lý mọi sai lệch chưa xử lý), phiếu giữ trạng thái; (3) yêu cầu suất bổ sung (FR-040) bếp xác nhận thì luôn thành phần bổ sung, không cộng vào số suất theo chế độ ăn của phiếu, không chặn giao phiếu (DBR-27 giữ đúng công thức data-model).
- **Số suất trên phiếu** = lần chốt của tầng/khu + phát sinh cần giao đã được bếp xác nhận (DBR-27). Phát sinh cần giao mới đưa phiếu Đã chuẩn bị về Đã chốt. Bếp xác nhận phát sinh `+` hoặc đổi suất khi phiếu đã Đã giao, Có sai lệch, Đã nhận thì tạo phần bổ sung (FR-048b).
- **Giao** (FR-047) nêu đủ mọi điều kiện chưa thỏa (`NOT_PREPARED`, `MISSING_SUBSTITUTE_DISH`, `ADJUSTMENTS_NOT_CONFIRMED`, `SAMPLE_NOT_RETAINED`) rồi mới xét trạng thái Đã chuẩn bị. "Suất đặc biệt" phải chuẩn bị là bản ghi có `special`; suất chỉ có dấu (ví dụ "chưa gán chế độ ăn") có nhãn nhưng không chặn. Bản ghi lưu mẫu: giao chỉ kiểm có `simple_record` `M08.RETAINED_SAMPLE` cha là bữa ngày (`RetainedSampleMarker`); Phase 11 (T039) cài nội dung và lệnh, test ghi thẳng.
- **Quyền**: bếp `MEAL_PREPARATION UPDATE` (đánh dấu chuẩn bị, giao, xử lý sai lệch, giao phần bổ sung), xem `MEAL_COUNT_TICKET VIEW`. Người nhận tại tầng `MEAL_RECEIPT` (`CONFIRM` nhận, `CREATE` báo sai lệch, `VIEW` xem): `floorCommand`, `supplementFloorCommand` tự kiểm để trả 403 `OUT_OF_RECEIVING_SCOPE` khi lớp phạm vi không thỏa (quickstart Q33), đã thêm vào `RequiresPermissionArchTest.SELF_CHECKED`. `GET /meal-slips`: tài khoản có vai trò bếp luôn nhận `KitchenMealSlip`; người khác nhận `FloorMealSlip` của tầng/khu trong phạm vi, dị ứng từng người chỉ khi có `ALLERGY_CONDITION VIEW`.
- **Thông báo** (FR-065): `SlipDiscrepancyNotice` (báo: bếp mức Trung bình; xử lý: người báo mức Nhẹ, theo hồ sơ nhân viên), `MealSlipLateNotice` (Trưởng tầng, cách thay mặc định của 009 là Người phụ trách ca, và bếp; mức Nhẹ; một lần mỗi phiếu theo dấu `M08.JOB.MEAL_SLIP_LATE`; bỏ qua lần chốt sau giờ bữa). Phát thêm `MealSlipDelivered`, `MealSlipDeliveredLate`, `MealSlipDiscrepancyReported` cho 016.
- **Còn lại**: khu bán trú không có ai được phân công thì báo Quản lý viện (FR-048), hiện dùng cách thay mặc định; dấu mức phiếu (`NO_MENU`, `CLOSED_AFTER_MEAL_TIME`, FR-046) chưa có trường trong `KitchenMealSlip`; màn web, app chờ duyệt bố cục (team-plan mục 8).
