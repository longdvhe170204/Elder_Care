# Implementation Plan: Số dư và thu chi của người cao tuổi

**Branch**: `017-resident-balance-ledger` | **Date**: 2026-10-22 (lập lại theo spec viết lại, Q-377 → Q-379, Q-381, Q-385, Q-386; bản 2026-10-10 bỏ) | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/017-resident-balance-ledger/spec.md`

## Summary

Feature 017 là phần 15.9, 15.9a của module M11 (UC-12, UC-85 → UC-88, UC-100, UC-101; BF-16; BR-M11-10 → 16, BR-M11-21 → 23). Ranh giới theo Q-385: hệ thống tính tiền theo dịch vụ và theo dõi người cao tuổi còn nợ hay còn dư; quỹ tiền mặt, đối soát ngân hàng, phiếu thu chi, hóa đơn, hạch toán thuộc phần mềm kế toán bên ngoài. Feature cung cấp:
1. Sổ số dư, sổ tiền cọc, mã nộp tiền, thông tin nộp tiền (tài khoản thu CFG-M11-10, mã QR VietQR không kèm số tiền) cho hồ sơ và cổng người thân (mục A; DBR-28, DBR-29).
2. Kế toán ghi nhận tiền nộp, tiền cọc bằng tay: tiền mặt, chuyển khoản có mã giao dịch ngân hàng (cảnh báo trùng, không chặn); trạng thái đặt cọc dẫn xuất cho 004 (mục B; BR-M11-12, 16).
3. Giao dịch thanh toán bảng chi phí kèm lệnh chốt bảng của 010; hoàn tiền, hoàn cọc, điều chỉnh có Quản lý viện duyệt (sửa sai bằng điều chỉnh trỏ giao dịch gốc, không có giao dịch đảo); cấn trừ cọc (mục C; BR-M11-10, 11, 14; DBR-17, DBR-30).
4. Số ngày còn đủ tiền, tình trạng "Sắp hết tiền", "Còn nợ", báo theo bảng chuyển; dấu "số dư không đủ" cho mua hộ (mục D; BR-M11-13, 15).
5. Điều kiện quyết toán cho 004, mục việc sau qua đời, lệnh Xác nhận quyết toán dựng bảng quyết toán PDF có phiên bản (mục E, J; BR-M11-22, DBR-38).
6. Phiếu báo phí PDF và Excel cho mỗi bảng chi phí Đã chốt (mục J; BR-M11-21, 23; DBR-37).
7. Sao kê số dư và báo cáo thu chi thu gọn, xuất Excel và PDF cùng số liệu; người thân tải sao kê (mục F; UC-88, Q-381, Q-386).

Hướng kỹ thuật:
- Gói `billing.sodu` trong M11 cùng 010; dùng port nội bộ `ChiPhiChoSoDuPort` của 010 (thêm `bangDaChot`, 010 research P-12), cài `SoDuMuaHoPort`; cài `TrangThaiDatCocPort`, `MucViecSauQuaDoiPort` (`QUYET_TOAN`) và bean `ThamGiaChuyenTrangThai` với hai mã điều kiện `KT_H_QUYET_TOAN`, `KT_H_XAC_NHAN_QUYET_TOAN` (004 research T-09, cập nhật 2026-10-22) (research E-01, E-02).
- Số dư, số dư cọc lưu dẫn xuất trên `so_du`, cập nhật cùng transaction với giao dịch Đã xác nhận dưới khóa dòng `so_du`; bất biến đối chiếu bằng test và truy vấn hằng đêm (E-03). *(R-15: `so_du` → `trang_thai_nct`)*
- Giao dịch cần duyệt là loại yêu cầu phê duyệt `M11.GIAO_DICH_SO_DU` của 000; duyệt kiểm lại giới hạn và người nhận tại lúc duyệt (E-06).
- Chứng từ (phiếu báo phí, bảng quyết toán) và file xuất dựng bằng `common.bieumau` (PDF: OpenPDF, 007 research S-25; Excel: fastexcel, đã chọn ở 010), lưu S3 qua `FileApi` như tệp đính kèm (001 research H-13); tải lại trả đúng file đã lưu (E-09 → E-11).
- Mã QR VietQR: chuỗi EMVCo dựng trong mã (CRC16); ảnh QR in trên PDF: ZXing core tính ma trận, `PdfBieuMau` vẽ thành ô vuông; client web, app vẽ ảnh từ chuỗi (E-04).
- Hàm thuần kiểm thử bằng bảng: `TrangThaiDatCoc`, `GioiHanHoan`, `PhiMotNgay`, `TinhTrang`, `QuyetDinhBao`, `DieuKienQuyetToan`, `SaoKe`, `BaoCaoThuChi`, `VietQrPayload` (E-13).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 017 không tự tạo giao dịch nào ngoài thanh toán bảng (tác động kèm lệnh chốt bảng của Quản lý viện ở 010); không tự hoàn, tự cấn trừ, tự xác nhận quyết toán, tự phát hành phiếu báo phí;
- hệ thống chỉ: mở sổ kèm lệnh gửi ký hợp đồng của 004; tính số dư, tình trạng, điều kiện để hiển thị; báo, nhắc; dựng file chứng từ trong lệnh của Kế toán.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 016. Dùng lại: fastexcel (010) cho Excel; OpenPDF qua `common.bieumau` (007 research S-25) cho PDF. Thêm ZXing core (`com.google.zxing:core`, Apache 2.0) để vẽ ảnh mã QR trên PDF (research E-04); đã duyệt bản 3.5.4 (2026-10-22, 007 research S-26); chỉ dùng `core`, không cần `javase`. Web, app vẽ mã QR từ chuỗi payload bằng thư viện vẽ QR của client (chọn khi làm màn, không ảnh hưởng backend).

**Storage**: MySQL 8.0, cùng schema M11 với 010; bảng ở [data-model.md](data-model.md). File chứng từ, file xuất ở bucket S3 qua `common.tep` (`FileApi.upload` + `attach`); bổ sung kiểu `.xlsx` cho tệp do hệ thống tạo (research E-11).

**Testing**:
- Như 000 R-14.
- Test bảng cho chín hàm thuần (research E-13); SC-001 với 3 tháng × 50 hồ sơ.
- Test tích hợp MySQL cho `UNIQUE` (thanh toán bảng, phiếu báo phí, phiên bản hiện hành bảng quyết toán), trigger nhóm 3, cột sinh.
- Test đồng thời: hai Kế toán ghi nộp cùng hồ sơ; duyệt hoàn tiền cùng lúc với thanh toán bảng; hai lần phát hành phiếu cho một bảng; hai lần xác nhận quyết toán.
- Đồng hồ giả lập cho CFG-M11-04, 05, CFG-M02-09, CFG-M15-05, 06.
- Bản giả `ChiPhiChoSoDuPort` (010), sự kiện và `HoSoKetThucDocApi` (004), `QuyenNguoiThanApi` (012), `S3Client` (001).
- So nội dung PDF bằng trích chữ (OpenPDF `PdfTextExtractor`) và Excel bằng đọc lại ô; SC-016 so byte khi tải lại.

**Target Platform**: Như 000.
- Web: số dư và giao dịch trên hồ sơ; ghi nhận tiền nộp (tiền mặt, chuyển khoản); hoàn, điều chỉnh, cấn trừ; hộp duyệt của Quản lý viện; tình trạng số dư; quyết toán và bảng quyết toán; phiếu báo phí; sao kê, báo cáo thu chi; cổng người thân trên trình duyệt (số dư, nộp tiền, chứng từ, sao kê).
- App nhân viên: không có màn của 017.
- App người thân: số dư, thông tin nộp tiền (mã QR), tải phiếu báo phí, bảng quyết toán, sao kê (khi mở `mobile/`).

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Job tình trạng 300 hồ sơ ≤ 5 phút (SC-012): đọc lô số dư, tổng chưa chốt, phí ngày.
- Ghi nhận một khoản chuyển khoản ≤ 1 phút thao tác (SC-003): tìm theo mã nộp tiền bằng chỉ mục `UNIQUE`.
- Phát hành phiếu báo phí 50 khoản (PDF + Excel) hoặc dựng bảng quyết toán ≤ 5 giây (SC-017).
- Thanh toán bảng ≤ vài giây sau chốt (listener ngay sau commit).

**Constraints**:
- Không có thao tác sửa, xóa, hủy giao dịch Đã xác nhận; mỗi lệnh một endpoint (constitution XI).
- Giao dịch Đã xác nhận, lịch sử tình trạng, lần xuất, phiếu báo phí, phiên bản bảng quyết toán không bị `UPDATE`, `DELETE` (constitution XII, DBR-17, DBR-37, DBR-38).
- Không chặn lệnh của module khác vì số dư (FR-020). Nhật ký trong transaction. CFG đọc lúc chạy.
- Không cấp số phiếu thu chi, không chốt quỹ, không nhập sao kê ngân hàng (Q-385).

**Scale/Scope**:
- 300 hồ sơ; vài nghìn giao dịch mỗi tháng; khoảng 300 phiếu báo phí mỗi tháng.
- 3 thực thể có vòng đời (giao dịch, tình trạng số dư, phiên bản bảng quyết toán); 9 bảng (gồm kỹ thuật).
- 1 job hằng ngày.
- 26 đường dẫn, 26 thao tác REST (duyệt, từ chối, hủy giao dịch đi qua API yêu cầu phê duyệt của 000) ([contracts/api-017.yaml](contracts/api-017.yaml)).
- 7 SPI hoặc provider 017 cài; 1 port nội bộ cài, 1 dùng; 2 giao diện công khai; 4 sự kiện.

Không còn mục NEEDS CLARIFICATION. Lựa chọn kỹ thuật ở [research.md](research.md). Điểm về tài liệu nguồn ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR; data-model mục 9 truy vết DBR-17, 28, 29, 30, 37, 38 và BR-M11-10 → 16, 21 → 23 |
| II. Spec không nêu công nghệ | Đạt | Công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Giao dịch chỉ có lệnh theo bảng trạng thái; chứng từ chỉ phát hành, xác nhận, đính kèm bản ký; không có sửa, xóa |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3, 5, 6 khớp bảng của spec; quickstart mục 4 |
| V. Không cố định giá trị | Đạt | CFG-M11-04, 05, 10, 11, CFG-M02-09, CFG-M14-01, CFG-M15-05, 06 đọc từ `ThamSoApi` |
| VI. Vai trò, quyền | Đạt | Khóa `BALANCE_LEDGER`, `STATEMENT_REPORT`, `PAYMENT_NOTICE`, `SETTLEMENT_RECORD` của 002 theo Phụ lục 27; người thân theo quyền xem chi phí (chú thích ¹⁶) (E-12) |
| VII. Điểm chưa rõ | Đạt | Chỗ spec lệch nghiệp vụ đã hỏi chủ dự án (ý Chờ duyệt theo BR-M11-22); điểm còn lại ở "Điểm cần báo lại" |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-017 (trừ SC bỏ) có cách đo ở quickstart mục 5 |
| IX. Phạm vi | Đạt | Không đổi nghiệp vụ; lập lại plan theo spec 2026-10-22 |
| X. Kiến trúc | Đạt | Một khối; gói `billing.sodu`; port nội bộ có ArchUnit; không có vòng (E-02) |
| XI. Nghiệp vụ ở backend | Đạt | Giới hạn hoàn, tình trạng, điều kiện quyết toán, nội dung chứng từ ở backend; client chỉ vẽ mã QR |
| XII. Dữ liệu | Đạt | Trigger nhóm 3; `UNIQUE` cho DBR-30, DBR-37, DBR-38; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V |
| XIV. Ngoại tuyến, thông báo | Đạt | Không có ghi ngoại tuyến. Thông báo theo FR-030, chia phần `CHUNG` / `CHI_PHI` |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR (quickstart mục 4) |

**Kiểm lại sau Phase 1 (2026-10-22)**: Đạt. Không thêm vai trò, trạng thái hay quy tắc ngoài spec. Bổ sung kỹ thuật giải thích ở Complexity Tracking.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 017 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 hồ sơ; vài nghìn giao dịch mỗi tháng |
| NFR-02 Hiệu năng | Có | Số dư dẫn xuất; job đọc lô; chứng từ ≤ 5 giây |
| NFR-03 Thông báo khẩn cấp | Không | Không có mức Khẩn cấp |
| NFR-04 Tác vụ định kỳ | Có | Khóa `(nct_id, ngày, nguồn)` chống báo trùng; chạy bù |
| NFR-05 Khả dụng | Gián tiếp | Listener idempotent |
| NFR-06 Sao lưu | Có | Giao dịch ở MySQL; file chứng từ ở S3 có versioning |
| NFR-07 Lưu trữ | Có | Theo CFG-M15-03 (FR-029) |
| NFR-08 Bảo mật | Có | Nhóm trường `TAI_CHINH`; file chứng từ, sao kê chỉ tải qua backend theo quyền (`QuyenTepProvider`) |
| NFR-09 Thời gian | Có | `Clock`; ngày theo Asia/Ho_Chi_Minh |
| NFR-10 Thiết bị | Có | Cổng web và app người thân |
| NFR-11 Ngôn ngữ | Có | VND số nguyên; chứng từ tiếng Việt, font nhúng |
| NFR-12 Cấu hình | Có | CFG-M11 đổi qua tham số |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả 004, 010, 012 |

## Project Structure

### Documentation (this feature)

```text
specs/017-resident-balance-ledger/
├── plan.md                      # File này
├── research.md                  # Phase 0: E-01 → E-14
├── data-model.md                # Phase 1: bảng, bảng chuyển, DBR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết → test
├── contracts/
│   ├── api-017.yaml             # OpenAPI 3.1: số dư, nộp tiền, giao dịch, tình trạng, quyết toán, phiếu báo phí, xuất, cổng
│   └── module-interfaces.md     # Port nội bộ M11, SPI 017 cài, API, sự kiện
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Đường dẫn là tên logic; đặt file theo [backend/README.md](../../backend/README.md) và research R-02 của 000 (`api/{dto, spi, events}`, `web/{request, response}`, `internal/<chức năng>/{service, domain, persistence, config}`).

```text
backend/src/main/java/com/eldercare/backend/billing/
├── api/           # (bổ sung) SoDuApi, SoDuThongKeApi, sự kiện 017
└── sodu/
    ├── so/          # sổ, mã nộp tiền, VietQR, cổng người thân
    ├── giaodich/    # nộp tiền mặt, chuyển khoản, thanh toán bảng, hoàn, điều chỉnh, cấn trừ, duyệt, đặt cọc
    ├── tinhtrang/   # PhiMotNgay, TinhTrang, QuyetDinhBao, SoDuMuaHoPort
    ├── quyettoan/   # DieuKienQuyetToan, bean trạng thái, MucViecSauQuaDoiPort, xác nhận, bảng quyết toán
    ├── chungtu/     # phiếu báo phí
    ├── xuat/        # sao kê, báo cáo thu chi
    └── lich/        # job hằng ngày

backend/src/main/java/com/eldercare/backend/common/bieumau/   # dùng chung, tạo ở 007 T104; 017 thêm VietQR ảnh nếu cần

frontend/src/tinh-nang/so-du/          # so-du, nop-tien, giao-dich, tinh-trang, quyet-toan, phieu-bao-phi, xuat
frontend/src/tinh-nang/cong-nguoi-than/so-du/
mobile/lib/nguoi-than/so-du/           # số dư, mã QR, chứng từ, sao kê
```

**Structure Decision**: Theo R-02 của 000 và 010 P-01: 017 thêm gói `billing.sodu` với bảy gói con.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Số dư lưu dẫn xuất trên `so_du` | Hiển thị, tình trạng, dấu mua hộ đọc thường xuyên; FR-002 giữ bằng cập nhật cùng transaction và đối chiếu hằng đêm | Cộng tổng giao dịch mỗi lần đọc: chậm cho job 300 hồ sơ, cổng *(R-15: `so_du` → `trang_thai_nct`)* |
| Mã nộp tiền có ký tự kiểm tra | Giảm nhận nhầm người khi Kế toán đọc nội dung chuyển khoản gõ sai | Mã tuần tự: sai một chữ số vẫn ra hồ sơ khác |
| Lưu file chứng từ ở S3 thay vì dựng lại | FR-037: tải lại phải trả đúng file đã phát hành dù dữ liệu nguồn đổi | Dựng lại từ số liệu chụp: phụ thuộc phiên bản thư viện, font; không bảo đảm giống byte |
| Hai mã điều kiện quyết toán cho 004 | FR-064 (004), BR-M11-22: ngoại lệ chỉ miễn ba ý tiền, không miễn ý xác nhận và ý Chờ duyệt | Một mã: ngoại lệ sẽ miễn cả ý xác nhận |

## Đồng bộ với hợp đồng của feature khác

1. **004**: cài `TrangThaiDatCocPort`, `MucViecSauQuaDoiPort` (`QUYET_TOAN`) đúng chữ ký; bean trạng thái trả `KT_H_QUYET_TOAN` (ba ý tiền, `coTheMien = true`) và `KT_H_XAC_NHAN_QUYET_TOAN` (không còn giao dịch Chờ duyệt, đã xác nhận quyết toán và số liệu không đổi; `coTheMien = false`) — đã ghi ở 004 research T-09, module-interfaces mục 2.1 (2026-10-22). Dùng `HoSoKetThucDocApi` (`dangChuanBi`, `thongTinQuyetToan`, `coNgoaiLeQuyetToanDaDuyet`; 004 mục 1.5b) và nghe `NgoaiLeKetThucDaDuyet`.
2. **010**: dùng `ChiPhiChoSoDuPort` (`tongChuaChot`, `phiNgayCoSo`, `bangDaChot` thêm 2026-10-22, 010 research P-12); cài `SoDuMuaHoPort`; nghe `BangChiPhiDaChot`.
3. **012**: dùng `QuyenNguoiThanApi` (`quyenCoTacDung(XEM_CHI_PHI)`, `laNguoiDaiDien`, `nguoiDaiDien`); REST cổng của 017 phục vụ số dư, chứng từ, sao kê trên cổng (012 FR-046). **009**: thông báo cài khuôn `common.notification` (nhóm `Representative`, `FamilyWithRight(CHI_PHI)`, `FacilityRole(KE_TOAN)`, `Approvers`). **016** đọc `SoDuThongKeApi` (bỏ hàm chốt quỹ cũ).
4. **007, `common`**: dùng `common.bieumau` (`PdfBieuMau`, `ExcelBieuMau`) do 007 T104 tạo; 017 cần thêm `FileApi` nhận kiểu `.xlsx` cho tệp hệ thống tạo (sửa nhỏ ở `common.tep`, chủ `common`). *(Đã xử lý 2026-10-22: gói `common.document`, `common.file.FileTypes`; xem Ghi chú triển khai.)*
5. **000** *(bổ sung 2026-10-22)*: SPI mới `KiemTraGiaTriThamSo` ở `administration.thamso.api` để module sở hữu kiểm giá trị tham số kiểu bảng trước khi lưu; 017 cài cho CFG-M11-10 (`maBin`, `soTaiKhoan`, `chuTaiKhoan`). Màn Tham số của 000 hiện CFG-M11-10 với ô chọn ngân hàng lấy từ `GET /ngan-hang` của 017 (research E-04). *(Phần 000 đã xử lý 2026-10-22: `ParameterValueCheck` ở `administration.parameter.api.spi`.)*
6. **000**: CFG-M11-11 kiểu bảng có trường logo; plan coi logo là id tệp `common.tep` (chưa có thì không in logo). Cần 000 thống nhất dạng giá trị khi làm màn tham số. *(Đã xử lý 2026-10-22: khóa JSON `name`, `address`, `taxCode`, `phone`, `logoFileId` theo `common.document.FacilityInfo`.)*

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Ý "không còn giao dịch Chờ duyệt" với ngoại lệ (đã hỏi, 2026-10-22).** Spec FR-022, FR-034 gộp "bốn ý về tiền" (gồm ý không còn giao dịch Chờ duyệt) và cho ngoại lệ thay cả bốn; BR-M11-22 tách "ba ý tiền … hoặc ngoại lệ, và không còn giao dịch chờ duyệt". Chủ dự án chọn theo BR-M11-22: ngoại lệ chỉ miễn ba ý tiền; ý Chờ duyệt và ý xác nhận không miễn được. Đề nghị sửa câu chữ FR-022, FR-034 (và User Story 7 kịch bản 4) cho khớp. *(Đã xử lý 2026-10-22: spec đã sửa.)*
2. **Hồ sơ đã kết thúc vẫn có giao dịch mới**: FR-034 cho xác nhận lại khi hồ sơ ở trạng thái cuối có giao dịch mới; điều kiện của 004 lúc đó không còn ý nghĩa (lệnh Kết thúc lưu trú đã chạy). Plan vẫn cho xác nhận lại và tạo phiên bản bảng mới (FR-024, Q-224); không đổi hồ sơ. Ghi lại để thống nhất cách hiểu.
3. **Danh sách việc sau qua đời và ngoại lệ**: 004 không có ngoại lệ cho mục của danh sách việc sau qua đời; FR-034 "đã có ngoại lệ được duyệt" chỉ áp cho hồ sơ kết thúc lưu trú. Với người qua đời, Kế toán chỉ xác nhận được khi ba ý tiền đã đạt. Đề nghị xác nhận, hoặc nêu rõ nếu người qua đời còn nợ cần một đường ngoại lệ (cần Q mới). *(Đã xử lý 2026-10-22: chủ dự án chọn giữ plan: không có ngoại lệ cho người qua đời. Còn mở: danh sách việc sau qua đời có thể không đóng được khi gia đình không trả; đề nghị mở Q mới (ví dụ Quản lý viện duyệt đóng mục quyết toán dù còn nợ, khoản nợ vẫn theo dõi).)* *(Đã xử lý tiếp 2026-10-22: chủ dự án mở Q-387 — có ngoại lệ do Quản lý viện duyệt cho mục quyết toán khi người qua đời còn nợ; 004 FR-071a, 017 FR-023, FR-034.)*
4. **Tên, mẫu chứng từ** đối chiếu Nghị định 123/2020/NĐ-CP (24.4, Q-377) trước khi vận hành; plan in dòng "Chứng từ này không thay thế hóa đơn" ở mọi chứng từ, không dùng chữ "hóa đơn", "phiếu thu" trong tên.
5. **Báo cáo thu chi "danh sách người còn nợ tại cuối khoảng"** (FR-025 (b)): plan tính theo số dư (tổng giao dịch Đã xác nhận tới hết ngày cuối khoảng) < 0, không trừ tổng chi phí chưa chốt (khác tình trạng "Còn nợ" của FR-017, vốn tính cả chi phí chưa chốt và chỉ có cho hiện tại). Đề nghị xác nhận. *(Đã xử lý 2026-10-22: chủ dự án chọn theo plan: số dư cuối khoảng < 0.)*

## Ghi chú triển khai

Ghi khi triển khai (constitution XIV: lệch kỹ thuật ghi ở plan cùng lúc với mã). Tên tiếng Anh theo glossary mục 6.5 (phần `common`, tham số của 000) và mục 23.

- *(2026-10-22, nhánh `impl/common-bieumau`, A)* **T003**: `common.file.FileTypes`: `SYSTEM_GENERATED` (PDF, JPEG, PNG, `.xlsx`) cho `FileApi.upload`, `USER_UPLOAD` (PDF, JPEG, PNG) cho `POST /files`; `check(mimeType, content, allowed)` kiểm kiểu khai báo khớp chữ ký đầu nội dung và giới hạn 10 MB, lỗi 422 `FILE_TYPE_NOT_ALLOWED`, `FILE_TOO_LARGE` (mã lỗi của hợp đồng 001). `FileApi` chưa có bản cài (001 T003, B): bản cài gọi `FileTypes.check(…, SYSTEM_GENERATED)`, controller `POST /files` gọi với `USER_UPLOAD` trước. Test `FileTypesTest`.
- **T045, phần 000**: SPI `ParameterValueCheck` (tên logic `KiemTraGiaTriThamSo`; `code()`, `check(JsonNode value)` ném `InvalidValueException`, 422 `INVALID_VALUE` kèm lý do) ở `administration.parameter.api.spi`; `ParameterService.changeValue` gọi mọi bản cài có mã trùng, sau phần kiểm chung theo kiểu và ràng buộc, trước khi lưu; khôi phục về "Theo cơ sở" (`null`) thì không gọi. Bản giả `support/fake/administration/FakeParameterValueCheck` (nhận mọi giá trị tới khi gọi `reject`; test khai bean cho mã mình cần). Test `ParameterValueCheckIT`. Còn của 017: bảng `ngan_hang`, `GET /ngan-hang`, bản cài cho CFG-M11-10, ô chọn ngân hàng, `NganHangThamSoIT`. *(R-15: `ngan_hang` → `danh_muc_chung`)*
- **Chứng từ**: dùng `common.document` (`PdfDocument`, `ExcelDocument`, 007 plan ghi chú triển khai). `PdfDocument` nhận thông tin viện từ module gọi: đọc `ParameterApi.read(CFG_M11_11)`, chưa đặt thì truyền `null`, có thì `asTable(FacilityInfo.class)`. Ảnh mã QR VietQR (T002, ZXing) chưa có hàm vẽ trên `PdfDocument`; 017 thêm khi làm phiếu báo phí.
- *(2026-11-08, nhánh `impl/017-balance-foundation`, A)* **T001, gói**: mã 017 ở `billing.balance` (`web`, `internal/<chức năng>/{service, domain, persistence, config}`, `scheduling`), hợp đồng vẫn ở `billing.api`. `LayerArchTest` thêm `FEATURE_PARTS` (phần của feature giao chủ khác trong cùng module, như phần của M15 nhưng không có `api`); `M11ArchTest` (010, E review) đổi gói được gọi `CostForBalancePort` từ `billing.internal.balance` sang `billing.balance`.
- **T004, giữ bảng `resident_balance`** (data-model mục 0 cho giữ khi không gộp được): `resident_module_state` khóa `(resident_id, module)`, dòng `M11` là `BillingResidentState` của 010; sổ cần cột tiền có kiểu cập nhật dưới `SELECT … FOR UPDATE` (DBR-28, E-03) và `payment_code` `UNIQUE`. Tiền `DECIMAL(15,0)` (000 data-model mục 1, audit F-07) thay `BIGINT` ghi ở data-model; Java dùng `long` đồng như `CostForBalancePort`. `balance_transaction` là `MULTIPLE`: Chờ duyệt sửa được, Đã xác nhận, Từ chối, Đã hủy có `locked_at` (`CHECK`), trigger chỉ cho đặt `adjusted` từ false sang true khi Đã xác nhận; xóa chỉ bằng tài khoản lưu giữ. Lịch sử tình trạng, ngân hàng, phiếu báo phí, bảng quyết toán, mốc sự kiện ghi vào bảng chung khi cài story tương ứng; cột sinh, chỉ mục trên bảng chung (ví dụ phiên bản hiện hành của bảng quyết toán, DBR-38) viết ở `V017_94__shared_*`.
- **T005, chỗ đặt sự kiện cho 004**: `billing` đã gọi `admission` (giá, nguồn lưu trú), nên `DepositStatusChanged`, `SettlementConditionChanged` (004 nghe) khai ở `admission.api.events`, 017 phát; khai ở `billing.api.events` thì 004 nghe sẽ thành vòng module (`ModuleBoundaryTest`). Cũng vì vậy 004 không gọi được `BalanceApi`: màn 004 lấy số dư qua api-017, điều kiện quyết toán qua bean điều kiện chuyển trạng thái. Chủ dự án duyệt "Đổi api" trên `admission.api` ngày 2026-11-08 (PR #135); B review trên `develop`.
- **T007**: `BalanceGovernanceConfig` đăng ký phân loại trường (`FINANCE`), lưu giữ (`BalanceRetention`: theo hồ sơ, `referencing` hồ sơ `M01.RESIDENT`, trang theo `(at, id)`, sổ bị giữ tới khi xóa hết giao dịch, xóa giao dịch từ mã lớn xuống), gắn phạm vi theo người cao tuổi cho `M11.BALANCE` (mã sổ là mã người cao tuổi) và `M11.BALANCE_TRANSACTION`. `FilePermissionProvider` cho `M11.PAYMENT_NOTICE`, `M11.SETTLEMENT_STATEMENT`, `M11.SETTLEMENT_SIGNED_COPY`, `M11.BALANCE_EXPORT` cần bản ghi chủ của phần chứng từ, xuất: B làm cùng Phase 8–10.
- *(2026-11-08, nhánh `impl/017-balance-us1-us5`, A)* **Phase 3–6**: mở sổ và trừ bảng nghe sự kiện **đồng bộ** (`@EventListener`, `MANDATORY`) trong transaction của lệnh gửi ký (004), lệnh chốt (010): "tác động kèm lệnh" (E-04, FR-012), nhật ký ghi đúng người thực hiện kèm căn cứ. Listener chốt bảng bỏ qua hồ sơ chưa có sổ (ghi log), để không chặn lệnh chốt của 010. `M13ArchTest` (009) đổi luật thành "đúng một listener nhận chính `HasNotificationRequests`, ở M13; listener khác không đọc `notificationRequests()`" vì `CostStatementFinalized` vừa là sự kiện nghiệp vụ 017 phải nghe vừa mang yêu cầu thông báo.
- **Phí một ngày (FR-016)**: lấy từ `CostForBalancePort.baseDailyFee(residentId, ngày)` (module-interfaces mục 1 ghi port này cho FR-016); 010 trả theo bốn trường hợp của FR-016 (tháng, ngày, bán trú tháng, bán trú buổi). 017 không đọc giá trong nội dung hợp đồng của 004 nên không có hàm `PhiMotNgay` (E-07). Chưa có bản cài của 010 thì tổng chưa chốt 0, phí chưa biết, chỉ xét Còn nợ. Khi chốt bảng không tính lại tình trạng: số dư và tổng chưa chốt cùng giảm đúng tổng bảng; bảng chuyển tình trạng của spec chỉ cho job và giao dịch nộp tiền đổi tình trạng (E-06 ghi "tính lại tình trạng", spec thắng).
- **Thông báo (FR-018, FR-030)**: người đại diện mức Trung bình hai phần `GENERAL`, `COSTS` (`RecipientGroup.Representative`, 009 lọc phần chi phí theo quyền); Kế toán mức Nhẹ **mỗi hồ sơ một thông báo** (không gộp danh sách vào một sự kiện, AGENTS mục 4 về outbox; danh sách ở `GET /balance-statuses`); Quản lý viện khi còn nợ từ CFG-M11-05. Khóa sự kiện `<loại>:<người cao tuổi>:<ngày | TRANSACTION:id>`. "Dừng nhắc" chỉ chặn lần báo tình trạng, không chặn thông báo nộp tiền.
- **Mã nộp tiền**: theo E-04 (mod 37) nhưng bỏ mã có ký tự kiểm tra `* ~ $ =` (sinh lại) vì nội dung chuyển khoản của ngân hàng thường bỏ ký tự đặc biệt; ký tự kiểm tra `U` vẫn dùng. Khóa duy nhất của lịch sử tình trạng là toàn bảng nên ghi `M11.BALANCE:<người cao tuổi>:<ngày>:<nguồn>`. Danh mục ngân hàng ở `V017_02` (A); A giữ `V017_02` → `V017_09`, B dùng từ `V017_10`.
- **Web**: `PaymentRecording` (T015), `BalanceStatuses` (T022) là phần để `BalancePage` của B gắn (nhánh `impl/017-b-phase7-10` đã có trang khung, chưa merge); A không tạo trang, mục menu thứ hai. Cổng người thân: `live/LiveCostsPage` thay bản mẫu ở mục Chi phí, số dư khi có phiên thật; bảng chi phí từng tháng (010) chưa nối.
- *(2026-11-08, nhánh `impl/017-a-phase11`, A)* **T021 bổ sung**: job hằng ngày đối chiếu bất biến sổ (`LedgerStore.mismatches`: số dư, số dư cọc khác tổng giao dịch Đã xác nhận), ghi log lỗi, không tự sửa (E-03). `US4_StatementPaymentIT` gộp vào `DBR_28_BalanceInvariantIT` để giữ ngân sách IT (AGENTS mục 4).
- **T044, truy vết Phase 1–6 (A)**: DBR-17 (tầng cơ sở dữ liệu: trigger `trg_balance_transaction_row_lock`, `_no_delete`) → `DBR_28_BalanceInvariantIT`; tầng lệnh (không có lệnh sửa, xóa; không điều chỉnh thanh toán bảng) là T024 của B. DBR-28 → `DBR_28_BalanceInvariantIT`, `US2_PaymentIT`. DBR-29 → `US2_PaymentIT`, `US1_BalanceIT` (mã nộp tiền duy nhất), `PaymentCodeTest`. DBR-30, BR-M11-11, FR-012 → `DBR_28_BalanceInvariantIT`. BR-M11-12, 16, FR-005 → FR-007 → `US2_PaymentIT`. BR-M11-13, FR-016 → FR-020 → `US5_BalanceStatusIT`, `BalanceStatusRuleTest`, `NoticeDecisionTest`. BR-M11-15, FR-021 → `US5_BalanceStatusIT`. FR-001 → FR-004 → `US1_BalanceIT`, `PaymentCodeTest`. FR-006 → `US2_PaymentIT`, `DepositStatusTest`. FR-029 → `T007_BalanceProvidersIT`. FR-030 (thông báo số dư, nộp tiền) → `US5_BalanceStatusIT`. T045 (CFG-M11-10) → `US1_BalanceIT`. Web, app: `PaymentRecording.test.jsx`, `BalanceStatuses.test.jsx`, `LiveCosts.test.jsx`, `ParameterForm.test.jsx`, `family_balance_screen_test.dart`.
- *(2026-11-08, B, nhánh `impl/017-b-phase7-10`)* **Hàm thuần và bộ dựng file của Phase 7–10** (làm trước, không cần `V017_01`): ban đầu đặt ở `billing.internal.balance`; khi merge `develop` (PR #135 chốt gói `billing.balance`, `LayerArchTest.FEATURE_PARTS`) đã chuyển sang `billing.balance.internal.<chức năng>.{domain, service}` và bỏ bản `Ledger`, `TransactionType`, `TransactionStatus`, `PaymentMethod`, `VietQrPayload` của B (A đặt cùng nội dung, đã so từng dòng); `BalanceEntry` vẫn của B. Tiền là `long` đồng. `SettlementConditions.evaluate` trả hai điều kiện kèm danh sách `Reason` (câu chữ do tầng service/web dựng); ngoại lệ đã duyệt chỉ vào `Result.satisfied(exceptionApproved)` và chỉ thay `END_H_SETTLEMENT`. `VietQrPayload` đặt điểm khởi tạo `11` (mã tĩnh, không số tiền); CRC16-CCITT kiểm bằng giá trị chuẩn `29B1` và hai chuỗi dựng độc lập bằng script. Bộ dựng PDF, Excel là lớp thuần (không bean) trong `service`. `PdfDocument` chưa có hàm chèn ảnh nên ảnh QR (`QrImage`, ZXing) chưa vào thân PDF phiếu báo phí: chủ dự án duyệt `PdfDocument.image` 2026-11-08, chờ A (team-plan mục 8).

- *(2026-11-08, B, nhánh `impl/017-b-phase7-10`)* **Phase 7 (US6)**: giao dịch cần duyệt là dòng `balance_transaction` Chờ duyệt (nhóm 2) kèm yêu cầu `M11.BALANCE_TRANSACTION` (nội dung `{transactionId, residentId, type, amount}`, gửi duyệt ngay, ngày hiệu lực hôm nay). Kiểm khi lập và kiểm lại khi duyệt dùng chung hàm thuần `TransactionChecks` (dựa trên `RefundLimit`): vượt số dư → `BALANCE_EXCEEDED`; trong số dư nhưng vượt (số dư − tổng chưa chốt) khi chưa ở trạng thái cuối → `REFUND_LIMIT_EXCEEDED`; hoàn cọc vượt số dư cọc → `DEPOSIT_BALANCE_EXCEEDED`. Duyệt bị chặn (`checkOnApprove`, 409, giữ Chờ duyệt) thì lệnh duyệt rollback nên thông báo cho Kế toán ghi ở transaction riêng (`BlockedApprovalNotices`, `REQUIRES_NEW`; người nhận `FacilityRole(ACCOUNTANT)`). Áp dụng chạy sau commit của lệnh duyệt (000 `ApplyTransaction`) và kiểm lại dưới khóa sổ; lúc đó không đạt thì yêu cầu Áp dụng không thành và giao dịch Đã hủy (Kế toán lập lại). Người nhận: hồ sơ còn người đại diện thì phải là người đại diện Hiệu lực (`admission.api.spi.RepresentativePort`; `billing` không gọi được `family.api` vì `family` đã gọi `billing`), không còn thì ghi họ tên người nhận khác (JSON ở cột `recipient`). Điều chỉnh trỏ giao dịch gốc phải cùng hồ sơ, cùng sổ, Đã xác nhận; điều chỉnh được duyệt ở sổ số dư tính lại tình trạng ngay bằng `BalanceStatusService.recalculate(…, Trigger.DAILY, "TRANSACTION:<id>")` (xấu đi báo ngay, tốt lên không báo; Q-268). Cấn trừ cọc: cặp hai dòng `DEPOSIT_OFFSET` trỏ nhau (dòng cọc ghi tạm Chờ duyệt rồi xác nhận kèm `linked_transaction_id`, vì trigger khóa dòng Đã xác nhận). Mô tả `GET /balance-transactions/pending-approval` ghi "kèm kết quả kiểm tra hiện tại" nhưng schema không có trường: không thêm (team-plan dòng của chủ dự án 2026-11-08). `V017_10`: `CHECK` mã giao dịch ngân hàng của `V017_01` chặn cả hoàn tiền bằng chuyển khoản (hợp đồng không có mã cho lệnh này) → chỉ bắt buộc với `BANK_TRANSFER_PAYMENT`, `DEPOSIT_RECEIPT` chuyển khoản (FR-007). Nhắc hoàn cọc đặt ở `status` vì `M11ArchTest` cấm job phụ thuộc gói `transaction`; mốc ở cột `deposit_refund_due_since`, `deposit_refund_reminded_at` của `V017_01`. Sửa nhỏ ở file của A: `BalanceTestBase` import `FakeResidencyEndingReadsConfig`, `BalanceStatusJob` gọi `DepositRefundReminders.remind`, `ApiContractIT` bỏ ba operation đã cài khỏi danh sách chờ (team-plan dòng review).

- *(2026-11-08, B, nhánh `impl/017-b-phase7-10`)* **Phase 8 (US7, US10)**: bảng quyết toán, bản ký ghi ở `record_event` (R-15, data-model mục 0.1 "khoa_duy_nhat"): loại `M11.SETTLEMENT_STATEMENT` mỗi phiên bản một dòng đã xác nhận, gắn sổ `M11.BALANCE` (lưu giữ cùng sổ, FR-029), khóa duy nhất `M11.SETTLEMENT_STATEMENT:<người cao tuổi>:<phiên bản>` giữ `UNIQUE (nct_id, phien_ban)` của DBR-38. `record_event` chỉ thêm (trigger) nên không có cột `hien_hanh` để đổi: phiên bản hiện hành là phiên bản lớn nhất (tối đa một hiện hành theo cách tính), không cần cột sinh `khoa_hien_hanh` hay `V017_94__shared_*`. Một dòng phiên bản thay cho cặp "bảng" và "xác nhận" (`M11.SETTLEMENT_CONFIRMATION` của data-model mục 0): người, thời điểm xác nhận là `actor_id`, `confirmed_at` của dòng. Bản ký là dòng `M11.SETTLEMENT_SIGNED_COPY` khóa `…:<bảng>` (một lần mỗi phiên bản), tệp gắn chủ là dòng đó. "Giao dịch sau xác nhận" so mã giao dịch với `lastTransactionId` của phiên bản (lúc xác nhận không còn Chờ duyệt nên mã tăng theo thứ tự xác nhận; không so thời điểm vì đồng hồ test có thể đứng yên). Kỳ cuối Đã chốt lấy từ `FinalPeriodCostApi` của 010 (thiếu bean thì chưa đạt, như cấn trừ Phase 7); số liệu bảng (số dư trước quyết toán, giao dịch sau kỳ cuối) lấy giao dịch "Thanh toán bảng chi phí" mới nhất làm bảng kỳ cuối vì 017 chưa biết loại bảng; tổng theo loại để trống tới khi 010 có `finalizedStatement` (E, 010 T037); họ tên người đại diện để trống vì chưa có cổng đọc người đại diện hiện tại (`RepresentativePort` chỉ có hàm kiểm; `billing` không gọi được `family.api`), bộ dựng bỏ dòng khi rỗng; hai điểm ghi team-plan mục 8, T032 giữ `[ ]`. "Số còn nợ hoặc còn phải hoàn" là số dư hiện tại (E-09). Căn cứ của lệnh (`SettlementRules.Basis`): hồ sơ kết thúc Đang chuẩn bị, danh sách việc sau qua đời đang mở (`ResidencyEndingReadApi.settlementInfo`), hoặc trạng thái cuối mà có giao dịch Đã xác nhận sau phiên bản hiện hành (FR-034 (c)); không có thì 409 `NOT_FINAL_STATUS`. Mã lỗi: `SETTLEMENT_CONDITIONS_NOT_MET`, `PENDING_TRANSACTIONS_REMAIN`, `SIGNED_COPY_EXISTS`, `NOT_CURRENT_VERSION` (glossary 23.4). `SettlementConditionChanged` phát khi hồ sơ đang có căn cứ (hồ sơ kết thúc, danh sách việc sau qua đời) sau mỗi lệnh đổi sổ hoặc giao dịch Chờ duyệt (nộp tiền qua `PaymentRecordedHook`, lập, duyệt, đóng, cấn trừ, xác nhận) và khi nghe sự kiện 004, 010; 004 đọc lại hai mã nên phát khi kết quả không đổi cũng không sai (không lưu kết quả cũ để so). Lệnh của 017 trên người qua đời gọi `PostDeathTaskApi.refresh` để đóng danh sách ngay; listener sự kiện 004, 010 chỉ báo, không tính lại danh sách trong lệnh của module khác. Nhắc sau trạng thái cuối (FR-024, Q-224) đặt ở `status` (job không phụ thuộc `transaction`), cột `final_status_reminded_at` của `V017_01`: chỉ khi không còn hồ sơ kết thúc, danh sách việc đang mở và có giao dịch sau thời điểm trạng thái cuối; đã có bảng thì nhắc tới khi xác nhận lại, chưa từng có bảng (mục quyết toán Không áp dụng) thì nhắc tới khi hai sổ về 0. Không gỡ, không đổi `BalanceApi`: chưa có bản thật (`settlementConditions`, `documents` không task nào giao; 004 không gọi được `BalanceApi`). Bean điều kiện thật chặn Kết thúc lưu trú tới khi xác nhận quyết toán (BR-M11-22) nên lớp nền test của 001 (`StatusTestBase`) và 004 (`M02TestBase`) gỡ `settlementTransitionParticipant` như bean thật của 003, 012 (B duyệt 2026-11-08); ca thật ở `US7_SettlementIT`. Sửa nhỏ ở file của A: `BalanceTestBase` import `FakeFileConfig`; `BalanceStatusJob` gọi `AfterFinalStatusReminders.remind`; `ApiContractIT` bỏ bốn operation quyết toán (team-plan dòng review).

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Ánh xạ bảng ở data-model mục 0; task T046. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.
- *(2026-11-08, nhánh `impl/017-b-phase7-10`, B)* **Phase 9, phiếu báo phí**: lưu trên `record_event` loại `M11.PAYMENT_NOTICE` gắn sổ, khóa duy nhất `M11.PAYMENT_NOTICE:<bảng chi phí>` giữ DBR-37 nên không cần `V017_94`; mã phiếu `PBP-<yyyyMM>-<bảng>`. `PaymentNoticeService.publish` nhận số liệu đã chụp (`PaymentNoticeData`) vì nguồn bảng Đã chốt (`CostForBalancePort.finalizedStatement`) chưa có ở 010; lệnh phát hành và danh sách chờ chưa nối. Quyền con "xem chi phí" của người thân kiểm ở `FamilyCostsRight` (ma trận quyền chỉ kiểm quan hệ), áp cho phiếu, cổng và quyền tệp. `BalanceApi.documents` (FR-038 cho 012) chưa cài, REST cổng đã đủ cho web và app.
- *(2026-11-09, nhánh `impl/017-b-phase7-10`, B)* **Phase 9, lệnh phát hành phiếu và tổng kỳ cuối theo loại**: 010 đã có `CostForBalancePort.finalizedStatement` thật (E). `PaymentNoticeIssuance` (`billing.balance.internal.document.service`) đọc bảng Đã chốt, tìm giao dịch `STATEMENT_PAYMENT` Đã xác nhận cùng mã bảng (không có thì 409 `STATEMENT_NOT_PAID`), tính số dư trước theo thứ tự (thời điểm xác nhận, mã) trên sổ `BALANCE`, số dư sau = trước + số tiền giao dịch, ghép thông tin nộp tiền CFG-M11-10 qua `CollectionAccounts`, rồi gọi `PaymentNoticeService.publish`. Danh sách chờ: `PaymentNoticeStore.unissued` (giao dịch thanh toán chưa có khóa duy nhất `M11.PAYMENT_NOTICE:<bảng>`), lọc bảng 010 trả được; chỉ Kế toán. Bảng quyết toán: `SettlementLedger.finalPeriodStatementId` (mã bảng của thanh toán kỳ cuối) để `SettlementService` lấy tổng theo loại từ cùng hàm. Tên loại khoản dùng chung ở `CostTypeLabels`. Họ tên người đại diện vẫn để trống (chờ `RepresentativePort`).
- *(2026-11-08, nhánh `impl/017-b-phase7-10`, B)* **Phase 10, xuất sao kê và báo cáo thu chi**: lần xuất lưu trên `record_event` loại `M11.BALANCE_EXPORT` (cha `M11.BALANCE_EXPORT_BY` theo người xuất) kèm dòng nhật ký `BALANCE_EXPORTED`, vì tải lại cần chủ sở hữu tệp; data-model mục 0.1 chỉ ghi hành động nhật ký. Thời điểm giao dịch trong sao kê là lúc xác nhận (khớp sổ). Khoảng kiểm CFG-M14-01 tại 017 (`ReportPeriods` ở module khác), mã `EXPORT_RANGE_EXCEEDED`. Cổng người thân trả file ngay, chỉ ghi nhật ký, không lưu file. `BalanceStatisticsApi` thật (`export.service.BalanceStatistics`): `statuses` đọc cột `status` đã lưu ở `resident_balance` (cập nhật khi tính lại), phạm vi là danh sách hồ sơ đã được 002 đổi sẵn trong `ScopeFilter`; bản giả `@Primary` của A vẫn ưu tiên trong test của 016. App người thân: `family/balance/family_documents_screen.dart`, thêm `ApiClient.postBytes` (sửa nhỏ `common/`, ghi ở team-plan).
- *(2026-11-08, nhánh `impl/017-b-phase7-10`, B)* **T044, truy vết Phase 7–10 (B)**: DBR-17 tầng lệnh (không có lệnh sửa, xóa giao dịch; điều chỉnh có duyệt; không điều chỉnh thanh toán bảng), FR-010, FR-011, FR-013 → FR-015a, BR-M11-14 → `US6_BalanceTransactionIT`, `RefundLimitTest`. FR-022 → FR-024, BR-M11-22 → `US7_SettlementIT`, `SettlementConditionsTest`, `SettlementRulesTest`, `SettlementStatementDocumentTest`; DBR-38 (khóa duy nhất `M11.SETTLEMENT_STATEMENT:<người cao tuổi>:<phiên bản>`, phiên bản hiện hành là phiên bản lớn nhất, không dùng cột sinh `hien_hanh` vì `record_event` không đổi cờ) → `US7_SettlementIT`, `T040_ConcurrencyAndPermissionsIT` (hai lần xác nhận đồng thời). FR-031, FR-032, FR-037, FR-038, BR-M11-21, BR-M11-23, DBR-37 (khóa duy nhất `M11.PAYMENT_NOTICE:<bảng chi phí>`) → `US9_PaymentNoticeIT`, `PaymentNoticeDocumentTest`, `QrImageTest`, `VietQrPayloadTest`, `T040_ConcurrencyAndPermissionsIT` (hai lần phát hành đồng thời, dòng không sửa, xóa). FR-025 → FR-027 → `FR_025_ExportIT`, `BalanceStatementTest`, `IncomeExpenseReportTest`, `BalanceExportDocumentTest`, `US8_BalanceStatisticsIT`. FR-028 (quyền, SC-010) → `T040_ConcurrencyAndPermissionsIT` (theo vai trò), `PermissionMatrixIT`. FR-029 (lưu giữ) → `T007_BalanceProvidersIT`, `FR_051a_AttachedFileRetentionIT`. Hợp đồng → `ApiContractIT`. Chưa có test: lệnh phát hành phiếu (FR-031 phần danh sách chờ, `STATEMENT_NOT_PAID`) chờ `finalizedStatement` của E; "duyệt cùng thanh toán" đồng thời chưa kiểm vì bản giả người dùng dùng chung một người cho mọi luồng (`SwitchableUser`), duyệt cần Quản lý viện còn thanh toán cần Kế toán. Web, app: `PaymentNotices.test.jsx`, `ResidentNotices`, `BalanceExports.test.jsx`, `QuyetToan.test.jsx`, `family_documents_screen_test.dart`.
