# Implementation Plan: Tài sản, lịch xe và mua sắm của viện

> **Sửa 2026-10-29 (Q-392):** hệ thống không quản lý kho nguyên liệu. Bỏ gói `m08.kho`, `m08.api.KhoThongKeApi`, port `TonKhoNguyenLieuPort`, `LichBuaApi` cho 019, bảng kho, DBR-31, BR-M08-17 → 20, CFG-M08-08, 09 và job kho. Các dòng còn nhắc kho trong plan này (Constitution Check, NFR, Complexity) không còn hiệu lực; mua nguyên liệu chỉ qua yêu cầu mua sắm, Kế toán so chứng từ với số tiền thực tế.

**Branch**: `019-inventory-assets` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/019-inventory-assets/spec.md`

## Summary

Feature 019 gồm ba phần thuộc hai module (UC-89, UC-90, UC-94, UC-95):
1. *(Bỏ theo Q-392)* Kho nguyên liệu.
2. Yêu cầu mua sắm của bộ phận: lập, Quản lý viện duyệt chi phí, ghi đã mua, Kế toán đối chiếu, xử lý lệch; gắn phiếu nhập, tài sản, bảo trì thuê ngoài (M11; BR-M11-17 → 20, DBR-36, Q-284).
   *(Bổ sung 2026-10-11, FR-008d, Q-337)* Loại `THUOC` cho thuốc do viện cung cấp: Điều dưỡng lập được, dòng gắn đơn nguồn viện của người cao tuổi (đọc `m07.api`); 019 cài `YeuCauThuocCungUngPort` cho 006 ghi lần tiếp nhận; không có kho thuốc (research W-02, W-05).
3. Tài sản theo bộ phận: trạng thái, vị trí, bảo trì, kiểm định, đăng kiểm, bảo hiểm, báo hỏng; giường là tài sản đồng bộ với trạng thái giường (M03; BR-M03-15, 17, 18, DBR-32).
4. Lịch xe, điều kiện "có lịch xe" cho chuyến đi (M03; BR-M03-16).

Hướng kỹ thuật:
- Gói `m11.muasam`, `m03.taisan`, `m03.lichxe`, dùng các port nội bộ 003, 010 đã khai (research W-01, W-02).
- M03 không phụ thuộc M11 (vì M11 → M02 → M03): `YeuCauMuaSamPort` khai ở `m03.api`, M11 cài.
- Lịch xe cung cấp `m03.api.LichXeApi`, `LichXeLenhApi` cho 014 gọi; sửa hợp đồng 014 (bỏ `LichXeChuyenPort` ở `m04.api`) để tránh vòng M03 ↔ M04.
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
- 2 port nội bộ cài, 3 dùng; 1 SPI mới ở `m03.api` (M11 cài); 2 API mới cho 014; 3 giao diện thống kê cho 016.

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
backend/src/main/java/com/cambae/backend/
├── m11/muasam/     {internal, web, lich}   # yêu cầu mua sắm, gắn nguồn, KiemTraVuot, KhopChungTu, YeuCauMuaSamPort (cài)
├── m11/api/        # (bổ sung) YeuCauMuaSamApi, MuaSamThongKeApi
├── m03/taisan/     {internal, web, lich}   # tài sản, lịch sử, AnhXaTrangThaiGiuong, TaiSanGiuongPort, thanh lý
├── m03/lichxe/     {internal, web, lich}   # lịch xe, KiemTraDatLich, LichXeApi, LichXeLenhApi
└── m03/api/        # (bổ sung) YeuCauMuaSamPort (SPI), LichXeApi, LichXeLenhApi, TaiSanThongKeApi

frontend/src/tinh-nang/mua-sam/  frontend/src/tinh-nang/tai-san/  frontend/src/tinh-nang/lich-xe/
```

**Structure Decision**: Theo R-02 của 000, 003 G-01, 010 P-01, 011 M-01.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| SPI `YeuCauMuaSamPort` ở `m03.api` | Tài sản (M03) gắn yêu cầu mua sắm (M11) trong cùng lệnh; M11 phụ thuộc gián tiếp M03 | M03 gọi M11: vòng; sự kiện sau commit: mất "chặn khi thiếu yêu cầu" trong lệnh (FR-008c) |
| Lịch xe ở `m03.api` thay cho SPI ở `m04.api` | M03 cài SPI của M04 tạo vòng | Giữ SPI ở `m04.api`: Modulith `verify()` từ chối |
| `mua_sam_gan_nguon` lưu số tiền nguồn | Kế toán đối chiếu tổng phiếu nhập, nguyên giá mà M11 không đọc được M08 (vòng) | M11 đọc M08: vòng *(R-15: `mua_sam_gan_nguon` → cột `JSON` của yêu cầu mua sắm)* |

## Đồng bộ với hợp đồng của feature khác

1. **014** ([../014-activity-care-quality/contracts/module-interfaces.md](../014-activity-care-quality/contracts/module-interfaces.md) mục 1.2; research L-02, L-07, L-08; plan; quickstart): bỏ `LichXeChuyenPort` ở `m04.api`; 014 gọi `m03.api.LichXeApi` và `LichXeLenhApi.theoChuyen` (MANDATORY) trong lệnh của chuyến; `ChuyenDiThayDoi` chỉ còn cho 016.
2. **003, 011, 010**: cài `TaiSanGiuongPort` (`TonKhoNguyenLieuPort` bỏ theo Q-392), dùng `GiuongTheoTaiSanPort`, `LichBuaApi` đúng chữ ký. 010 P-12 ghi "yêu cầu mua sắm không sinh khoản; 010 không cần port cho 019" — giữ nguyên.
3. **008 / 002**: dùng `NhiemVuApi.boPhanCua`, `laPhuTrachBoPhan`, `phuTrachBoPhan` (đã khai). **009**: thông báo; **016** đọc ba giao diện thống kê.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX).

1. **"Hệ thống MUST đề xuất lô có hạn dùng sớm nhất"** (FR-003, BR-M08-17) so với Q-307 (bỏ mọi gợi ý, đề xuất của hệ thống): spec 019 chưa được rà theo Q-307 ở điểm này. Plan làm theo FR-003 (chọn sẵn lô hạn sớm nhất, đổi lô cần lý do) vì đây là quy tắc xuất kho (FEFO) chứ không phải gợi ý nội dung nghiệp vụ. Đề nghị chủ dự án xác nhận và ghi ngoại lệ vào dòng Q-307 ở 24.2, hoặc đổi FR-003 thành "chặn xuất lô có hạn muộn hơn khi còn lô hạn sớm hơn trừ khi có lý do". *(Đã xử lý 2026-10-10: FR-003 sửa theo cách thứ hai; hệ thống hiển thị lô theo hạn dùng, không chọn sẵn; `GoiYLo` đổi thành `ThuTuLoTheoHan`.)*

2. **Thanh lý tài sản không có bảng trạng thái riêng** (FR-013 ghi "Phụ trách bộ phận lập yêu cầu, Quản lý viện duyệt"): plan dùng yêu cầu phê duyệt chung của 000 (`M03.THANH_LY_TAI_SAN`). Đề nghị ghi rõ ở 7.7 rằng thanh lý đi qua vòng đời yêu cầu phê duyệt chung (6.6). *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Trưởng tầng báo hỏng tài sản "trong tầng mình"** (FR-013, FR-020): tài sản có vị trí tầng, phòng; tài sản ở "kho tài sản", "bãi xe" không thuộc tầng nào nên chỉ Phụ trách bộ phận báo hỏng được. Plan hiểu như vậy; đề nghị xác nhận ở Phụ lục 27 ³³. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Ánh xạ bảng ở data-model mục 0; task T029. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.
