# Implementation Plan: Số dư và thu chi của người cao tuổi

**Branch**: `017-resident-balance-ledger` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/017-resident-balance-ledger/spec.md`

## Summary

Feature 017 là phần 15.9 của module M11 (UC-12, UC-85 → UC-88, UC-93; BF-16; BR-M11-10 → 16). Nó cung cấp:
1. Sổ số dư, sổ tiền cọc, mã nộp tiền, thông tin nộp tiền (VietQR, không thanh toán trực tuyến) cho cổng người thân (mục A; DBR-28).
2. Phiếu thu tiền mặt có số phiếu liên tục, chốt quỹ ngày, trạng thái đặt cọc dẫn xuất cho 004 (mục B; BR-M11-16, DBR-35).
3. Nhập sao kê, khớp mã nộp tiền, đối soát, cặp điều chỉnh chuyển tiền (mục B; BR-M11-12, DBR-29).
4. Giao dịch thanh toán bảng chi phí kèm lệnh chốt bảng; hoàn tiền, hoàn cọc, điều chỉnh, đảo có duyệt; cấn trừ cọc (mục C; BR-M11-10, 11, 14, DBR-30).
5. Số ngày còn đủ tiền, tình trạng "Sắp hết tiền", "Còn nợ", báo theo bảng chuyển; dấu "số dư không đủ" cho mua hộ (mục D; BR-M11-13, 15).
6. Điều kiện quyết toán khi kết thúc lưu trú, mục việc sau qua đời (mục E; Q-222).
7. Xuất sao kê, báo cáo thu chi, sổ cọc (mục F).

Hướng kỹ thuật:
- Gói `m11.sodu` trong M11; dùng và cài port nội bộ P-12 với 010; cài `TrangThaiDatCocPort`, `MucViecSauQuaDoiPort` (`QUYET_TOAN`), điều kiện `KT_H_QUYET_TOAN` (research E-01, E-02).
- Số dư lưu dẫn xuất trên `so_du`, cập nhật cùng transaction với giao dịch Đã xác nhận dưới khóa theo hồ sơ; đối chiếu bất biến hằng đêm (E-03).
- Số phiếu cấp từ dãy có khóa trong transaction (không khoảng trống); `UNIQUE` cho dòng sao kê, thanh toán bảng, một đảo mỗi gốc, một chốt quỹ mỗi ngày (E-03).
- Khớp mã, tình trạng số dư, quyết định báo, giới hạn hoàn, điều kiện quyết toán là hàm thuần (E-06 → E-09).
- Giao dịch cần duyệt là loại yêu cầu phê duyệt của 000; duyệt kiểm lại giới hạn và người nhận tại lúc duyệt (E-07).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 017 không tự tạo giao dịch ngoài thanh toán bảng (tác động kèm lệnh chốt bảng của Quản lý viện) và không tự hoàn, tự cấn trừ, tự đối soát;
- hệ thống chỉ: mở sổ kèm lệnh gửi ký hợp đồng của 004; khớp dòng sao kê (không tạo giao dịch); tính số dư, tình trạng, điều kiện để hiển thị; báo, nhắc.

## Technical Context

**Language/Version**: Java 21 LTS; TypeScript 5.x; Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 015. fastexcel (đã chọn ở 010) cả đọc sao kê lẫn ghi xuất; CSV không cần thư viện. Payload VietQR dựng trong mã (chuẩn EMVCo, có CRC16); client vẽ ảnh QR bằng thư viện vẽ QR của web/Flutter.

**Storage**: MySQL 8.4 LTS. Bảng ở [data-model.md](data-model.md), cùng schema M11 với 010.

**Testing**:
- Như 000 R-14.
- Test bảng cho chín hàm thuần (research E-12); SC-001 với 3 tháng × 50 hồ sơ; SC-004 với 200 dòng sao kê.
- Testcontainers cho `UNIQUE`, trigger nhóm 3, dãy số phiếu không khoảng trống khi rollback.
- Test đồng thời: hai phiếu thu; duyệt hoàn tiền cùng lúc với thanh toán bảng.
- Đồng hồ giả lập cho CFG-M11-04, 05, CFG-M02-09, CFG-M13-06, CFG-M15-05, 06.
- Bản giả `ChiPhiChoSoDuPort`, sự kiện 004, `QuyenNguoiThanApi`.

**Target Platform**: Như 000.
- Web: số dư, phiếu thu, chốt quỹ, nhập sao kê và đối soát, giao dịch và duyệt, tình trạng số dư, quyết toán, xuất; cổng người thân trên trình duyệt (số dư, nộp tiền, sao kê).
- App nhân viên: không có màn của 017.
- App người thân: số dư, thông tin nộp tiền (mã QR), tải sao kê.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client liên quan.

**Performance Goals**:
- Job tình trạng 300 hồ sơ ≤ 5 phút (SC-012): đọc lô số dư, tổng chưa chốt, phí ngày.
- Nhập sao kê 200 dòng ≤ 10 giây; đối soát xong ≤ 30 phút thao tác (SC-004).
- Thanh toán bảng ≤ vài giây sau chốt (listener ngay sau commit).

**Constraints**:
- Không có thao tác sửa giao dịch; mỗi lệnh một endpoint (constitution XI).
- Giao dịch Đã xác nhận, dòng sao kê (nội dung), lần nhập, chốt quỹ Đã xác nhận, lịch sử tình trạng, lần xuất không bị `UPDATE`, `DELETE` (constitution XII, DBR-17).
- Không chặn lệnh của module khác vì số dư (FR-020). Nhật ký trong transaction. CFG đọc lúc chạy.

**Scale/Scope**:
- 4 thực thể có vòng đời (giao dịch, dòng sao kê, chốt quỹ ngày, tình trạng số dư); 11 bảng (gồm bảng con, kỹ thuật).
- 2 job (hằng ngày 02:00, theo giờ hành chính).
- 18 đường dẫn, 19 thao tác REST ([contracts/api-017.yaml](contracts/api-017.yaml)).
- 7 SPI hoặc provider 017 cài; 1 port nội bộ cài, 1 dùng; 2 giao diện công khai; 3 sự kiện.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR; data-model mục 7 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Giao dịch, dòng sao kê, chốt quỹ chỉ có lệnh theo bảng trạng thái; không có sửa, xóa giao dịch |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3 → 5 khớp bốn bảng của spec; quickstart mục 4 |
| V. Không cố định giá trị | Đạt | CFG-M11-04, 05, CFG-M02-09, CFG-M13-06, CFG-M14-01 đọc từ `ThamSoApi` |
| VI. Vai trò, quyền | Đạt | Kế toán lập, Quản lý viện duyệt, Hành chính chỉ xem; người thân theo quyền xem chi phí (E-11) |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ; điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-013 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ |
| X. Kiến trúc | Đạt | Một khối; gói `m11.sodu`; port nội bộ có ArchUnit. Không có vòng (E-02) |
| XI. Nghiệp vụ ở backend | Đạt | Khớp mã, tình trạng, giới hạn, điều kiện ở backend |
| XII. Dữ liệu | Đạt | DBR-17, 28, 29, 30, 35 ở data-model mục 7; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V |
| XIV. Ngoại tuyến, thông báo | Đạt | Không có ghi ngoại tuyến. Thông báo theo FR-030, chia phần `CHUNG` / `CHI_PHI` |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR; BR-M11-10 → 16 có test (quickstart mục 4) |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Bổ sung kỹ thuật giải thích ở Complexity Tracking.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 017 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 hồ sơ; vài nghìn giao dịch mỗi tháng |
| NFR-02 Hiệu năng | Có | Số dư dẫn xuất; job đọc lô |
| NFR-03 Thông báo khẩn cấp | Không | Không có mức Khẩn cấp |
| NFR-04 Tác vụ định kỳ | Có | Khóa `(nct_id, ngày, nguồn)` chống báo trùng; chạy bù |
| NFR-05 Khả dụng | Gián tiếp | Listener idempotent |
| NFR-06 Sao lưu | Có | Giao dịch, sao kê ở MySQL; tệp ở kho tệp |
| NFR-07 Lưu trữ | Có | Theo CFG-M15-03 |
| NFR-08 Bảo mật | Có | Trường tài chính `TAI_CHINH`; file sao kê chỉ Kế toán, Quản lý viện |
| NFR-09 Thời gian | Có | `Clock`; ngày chốt quỹ theo Asia/Ho_Chi_Minh |
| NFR-10 Thiết bị | Có | Cổng hai kênh |
| NFR-11 Ngôn ngữ | Có | VND số nguyên |
| NFR-12 Cấu hình | Có | CFG-M11 đổi qua tham số |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả 010 |

## Project Structure

### Documentation (this feature)

```text
specs/017-resident-balance-ledger/
├── plan.md                      # File này
├── research.md                  # Phase 0: E-01 → E-12
├── data-model.md                # Phase 1: bảng, bảng chuyển, DBR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng Q1 → Q35, truy vết → test
├── contracts/
│   ├── api-017.yaml             # OpenAPI 3.1: số dư, thu, chốt quỹ, sao kê, giao dịch, tình trạng, quyết toán, xuất, cổng
│   └── module-interfaces.md     # Port P-12, SPI 017 cài, API, sự kiện
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/vn/eldercare/m11/
├── api/           # (bổ sung) SoDuApi, SoDuThongKeApi, sự kiện 017
└── sodu/
    ├── so/          {internal, web}   # sổ, mã nộp tiền, VietQR, cổng
    ├── thu/         {internal, web}   # phiếu thu, dãy số phiếu, chốt quỹ, TrangThaiDatCoc
    ├── saoke/       {internal, web}   # nhập, KhopMa, dòng sao kê
    ├── giaodich/    {internal, web}   # giao dịch, duyệt, thanh toán bảng, cấn trừ, GioiHanHoan
    ├── tinhtrang/   {internal, web}   # PhiMotNgay, TinhTrang, QuyetDinhBao, SoDuMuaHoPort
    ├── quyettoan/   {internal, web}   # DieuKienQuyetToan, bean trạng thái, MucViecSauQuaDoiPort
    ├── xuat/        {internal, web}
    └── lich/

web/src/tinh-nang/so-du/
├── so-du/  ├── thu-chot-quy/  ├── sao-ke/  ├── giao-dich/  ├── tinh-trang/  ├── xuat/  └── cong-nguoi-than/so-du/

app-nguoi-than/lib/tinh-nang/so-du/   # số dư, mã QR, sao kê
```

**Structure Decision**: Theo R-02 của 000 và 010 P-01: 017 thêm gói `m11.sodu` với tám gói con.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Số dư lưu dẫn xuất trên `so_du` | Hiển thị, tình trạng, dấu mua hộ đọc thường xuyên; FR-002 vẫn giữ bằng cập nhật cùng transaction và đối chiếu | Cộng tổng giao dịch mỗi lần đọc: chậm cho job 300 hồ sơ, cổng |
| Dãy số phiếu có khóa | SC-011 đòi không khoảng trống, không trùng | AUTO_INCREMENT: bỏ số khi rollback |
| Mã nộp tiền có chữ số kiểm tra | Giảm khớp nhầm khi gia đình gõ sai một ký tự | Mã tuần tự: sai một chữ số vẫn khớp hồ sơ khác |

## Đồng bộ với hợp đồng của feature khác

1. **004**: cài `TrangThaiDatCocPort`, `MucViecSauQuaDoiPort` (`QUYET_TOAN`), điều kiện `KT_H_QUYET_TOAN` đúng chữ ký và mã đã khai (004 T-09). Bổ sung hai sự kiện 017 phát cho 004 làm mới màn điều kiện: `TrangThaiDatCocDaDoi`, `DieuKienQuyetToanDaDoi` (004 vẫn đọc qua port; sự kiện chỉ để báo đổi, FR-006, FR-022).
2. **010**: dùng `ChiPhiChoSoDuPort`, cài `SoDuMuaHoPort` đúng P-12; nghe `BangChiPhiDaChot`. Không đổi.
3. **012**: dùng `QuyenNguoiThanApi`; phần số dư trên cổng do REST của 017 phục vụ (012 F-10). **009**: thông báo cài `CoYeuCauThongBao`; **016** đọc `SoDuThongKeApi`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/nghiep-vu.md` và spec liên quan.

1. **FR-016 dựa vào "Bộ lập lịch của feature 010 đã sinh phí của ngày vừa kết thúc"**: sau Q-306, 010 không tự sinh khoản; khoản do Hành chính lập từ bản ghi nguồn (010 FR-005, plan 010). "Tổng chi phí chưa chốt" vì vậy chỉ gồm khoản Hành chính đã lập, có thể chậm hơn thực tế vài ngày, làm số ngày còn đủ tiền cao hơn. Plan chạy job lúc 02:00 với dữ liệu hiện có, không chờ 010. Đề nghị sửa câu đầu FR-016 và xác nhận cách tính (có cần cộng phần phí lưu trú ước tính của các ngày chưa lập khoản hay không; nếu cần thì phải có Q mới vì đổi nghiệp vụ).

2. **Số tài khoản thu, ngân hàng của viện** (FR-003) không có mã CFG ở Phụ lục 25 và không có chức năng quản lý trong spec. Plan đặt ở cấu hình triển khai (Quản lý viện xem được), không tự tạo CFG. Đề nghị thêm một tham số (ví dụ CFG-M11-xx "Tài khoản thu của viện") nếu muốn Quản lý viện đổi được trong hệ thống.

3. **Câu chữ còn lẫn** (không đổi nghiệp vụ): mục Phạm vi dòng 4 ghi "Tự tạo giao dịch thanh toán bảng chi phí" — đúng là tác động kèm lệnh chốt bảng; đề nghị viết "giao dịch thanh toán được tạo kèm lệnh chốt bảng" cho khớp Q-306.
