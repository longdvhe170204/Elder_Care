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
- Gói `m11.sodu` trong M11 cùng 010; dùng port nội bộ `ChiPhiChoSoDuPort` của 010 (thêm `bangDaChot`, 010 research P-12), cài `SoDuMuaHoPort`; cài `TrangThaiDatCocPort`, `MucViecSauQuaDoiPort` (`QUYET_TOAN`) và bean `ThamGiaChuyenTrangThai` với hai mã điều kiện `KT_H_QUYET_TOAN`, `KT_H_XAC_NHAN_QUYET_TOAN` (004 research T-09, cập nhật 2026-10-22) (research E-01, E-02).
- Số dư, số dư cọc lưu dẫn xuất trên `so_du`, cập nhật cùng transaction với giao dịch Đã xác nhận dưới khóa dòng `so_du`; bất biến đối chiếu bằng test và truy vấn hằng đêm (E-03).
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
| X. Kiến trúc | Đạt | Một khối; gói `m11.sodu`; port nội bộ có ArchUnit; không có vòng (E-02) |
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
backend/src/main/java/com/cambae/backend/m11/
├── api/           # (bổ sung) SoDuApi, SoDuThongKeApi, sự kiện 017
└── sodu/
    ├── so/          # sổ, mã nộp tiền, VietQR, cổng người thân
    ├── giaodich/    # nộp tiền mặt, chuyển khoản, thanh toán bảng, hoàn, điều chỉnh, cấn trừ, duyệt, đặt cọc
    ├── tinhtrang/   # PhiMotNgay, TinhTrang, QuyetDinhBao, SoDuMuaHoPort
    ├── quyettoan/   # DieuKienQuyetToan, bean trạng thái, MucViecSauQuaDoiPort, xác nhận, bảng quyết toán
    ├── chungtu/     # phiếu báo phí
    ├── xuat/        # sao kê, báo cáo thu chi
    └── lich/        # job hằng ngày

backend/src/main/java/com/cambae/backend/common/bieumau/   # dùng chung, tạo ở 007 T104; 017 thêm VietQR ảnh nếu cần

frontend/src/tinh-nang/so-du/          # so-du, nop-tien, giao-dich, tinh-trang, quyet-toan, phieu-bao-phi, xuat
frontend/src/tinh-nang/cong-nguoi-than/so-du/
mobile/lib/nguoi-than/so-du/           # số dư, mã QR, chứng từ, sao kê
```

**Structure Decision**: Theo R-02 của 000 và 010 P-01: 017 thêm gói `m11.sodu` với bảy gói con.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Số dư lưu dẫn xuất trên `so_du` | Hiển thị, tình trạng, dấu mua hộ đọc thường xuyên; FR-002 giữ bằng cập nhật cùng transaction và đối chiếu hằng đêm | Cộng tổng giao dịch mỗi lần đọc: chậm cho job 300 hồ sơ, cổng |
| Mã nộp tiền có ký tự kiểm tra | Giảm nhận nhầm người khi Kế toán đọc nội dung chuyển khoản gõ sai | Mã tuần tự: sai một chữ số vẫn ra hồ sơ khác |
| Lưu file chứng từ ở S3 thay vì dựng lại | FR-037: tải lại phải trả đúng file đã phát hành dù dữ liệu nguồn đổi | Dựng lại từ số liệu chụp: phụ thuộc phiên bản thư viện, font; không bảo đảm giống byte |
| Hai mã điều kiện quyết toán cho 004 | FR-064 (004), BR-M11-22: ngoại lệ chỉ miễn ba ý tiền, không miễn ý xác nhận và ý Chờ duyệt | Một mã: ngoại lệ sẽ miễn cả ý xác nhận |

## Đồng bộ với hợp đồng của feature khác

1. **004**: cài `TrangThaiDatCocPort`, `MucViecSauQuaDoiPort` (`QUYET_TOAN`) đúng chữ ký; bean trạng thái trả `KT_H_QUYET_TOAN` (ba ý tiền, `coTheMien = true`) và `KT_H_XAC_NHAN_QUYET_TOAN` (không còn giao dịch Chờ duyệt, đã xác nhận quyết toán và số liệu không đổi; `coTheMien = false`) — đã ghi ở 004 research T-09, module-interfaces mục 2.1 (2026-10-22). Dùng `HoSoKetThucDocApi` (`dangChuanBi`, `thongTinQuyetToan`, `coNgoaiLeQuyetToanDaDuyet`; 004 mục 1.5b) và nghe `NgoaiLeKetThucDaDuyet`.
2. **010**: dùng `ChiPhiChoSoDuPort` (`tongChuaChot`, `phiNgayCoSo`, `bangDaChot` thêm 2026-10-22, 010 research P-12); cài `SoDuMuaHoPort`; nghe `BangChiPhiDaChot`.
3. **012**: dùng `QuyenNguoiThanApi` (`quyenCoTacDung(XEM_CHI_PHI)`, `laNguoiDaiDien`, `nguoiDaiDien`); REST cổng của 017 phục vụ số dư, chứng từ, sao kê trên cổng (012 FR-046). **009**: thông báo cài khuôn `common.notification` (nhóm `Representative`, `FamilyWithRight(CHI_PHI)`, `FacilityRole(KE_TOAN)`, `Approvers`). **016** đọc `SoDuThongKeApi` (bỏ hàm chốt quỹ cũ).
4. **007, `common`**: dùng `common.bieumau` (`PdfBieuMau`, `ExcelBieuMau`) do 007 T104 tạo; 017 cần thêm `FileApi` nhận kiểu `.xlsx` cho tệp hệ thống tạo (sửa nhỏ ở `common.tep`, chủ `common`).
5. **000** *(bổ sung 2026-10-22)*: SPI mới `KiemTraGiaTriThamSo` ở `m15.thamso.api` để module sở hữu kiểm giá trị tham số kiểu bảng trước khi lưu; 017 cài cho CFG-M11-10 (`maBin`, `soTaiKhoan`, `chuTaiKhoan`). Màn Tham số của 000 hiện CFG-M11-10 với ô chọn ngân hàng lấy từ `GET /ngan-hang` của 017 (research E-04).
6. **000**: CFG-M11-11 kiểu bảng có trường logo; plan coi logo là id tệp `common.tep` (chưa có thì không in logo). Cần 000 thống nhất dạng giá trị khi làm màn tham số.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Ý "không còn giao dịch Chờ duyệt" với ngoại lệ (đã hỏi, 2026-10-22).** Spec FR-022, FR-034 gộp "bốn ý về tiền" (gồm ý không còn giao dịch Chờ duyệt) và cho ngoại lệ thay cả bốn; BR-M11-22 tách "ba ý tiền … hoặc ngoại lệ, và không còn giao dịch chờ duyệt". Chủ dự án chọn theo BR-M11-22: ngoại lệ chỉ miễn ba ý tiền; ý Chờ duyệt và ý xác nhận không miễn được. Đề nghị sửa câu chữ FR-022, FR-034 (và User Story 7 kịch bản 4) cho khớp. *(Đã xử lý 2026-10-22: spec đã sửa.)*
2. **Hồ sơ đã kết thúc vẫn có giao dịch mới**: FR-034 cho xác nhận lại khi hồ sơ ở trạng thái cuối có giao dịch mới; điều kiện của 004 lúc đó không còn ý nghĩa (lệnh Kết thúc lưu trú đã chạy). Plan vẫn cho xác nhận lại và tạo phiên bản bảng mới (FR-024, Q-224); không đổi hồ sơ. Ghi lại để thống nhất cách hiểu.
3. **Danh sách việc sau qua đời và ngoại lệ**: 004 không có ngoại lệ cho mục của danh sách việc sau qua đời; FR-034 "đã có ngoại lệ được duyệt" chỉ áp cho hồ sơ kết thúc lưu trú. Với người qua đời, Kế toán chỉ xác nhận được khi ba ý tiền đã đạt. Đề nghị xác nhận, hoặc nêu rõ nếu người qua đời còn nợ cần một đường ngoại lệ (cần Q mới). *(Đã xử lý 2026-10-22: chủ dự án chọn giữ plan: không có ngoại lệ cho người qua đời. Còn mở: danh sách việc sau qua đời có thể không đóng được khi gia đình không trả; đề nghị mở Q mới (ví dụ Quản lý viện duyệt đóng mục quyết toán dù còn nợ, khoản nợ vẫn theo dõi).)* *(Đã xử lý tiếp 2026-10-22: chủ dự án mở Q-387 — có ngoại lệ do Quản lý viện duyệt cho mục quyết toán khi người qua đời còn nợ; 004 FR-071a, 017 FR-023, FR-034.)*
4. **Tên, mẫu chứng từ** đối chiếu Nghị định 123/2020/NĐ-CP (24.4, Q-377) trước khi vận hành; plan in dòng "Chứng từ này không thay thế hóa đơn" ở mọi chứng từ, không dùng chữ "hóa đơn", "phiếu thu" trong tên.
5. **Báo cáo thu chi "danh sách người còn nợ tại cuối khoảng"** (FR-025 (b)): plan tính theo số dư (tổng giao dịch Đã xác nhận tới hết ngày cuối khoảng) < 0, không trừ tổng chi phí chưa chốt (khác tình trạng "Còn nợ" của FR-017, vốn tính cả chi phí chưa chốt và chỉ có cho hiện tại). Đề nghị xác nhận. *(Đã xử lý 2026-10-22: chủ dự án chọn theo plan: số dư cuối khoảng < 0.)*
