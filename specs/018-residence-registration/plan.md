# Implementation Plan: Khai báo tạm trú, lưu trú

**Branch**: `018-residence-registration` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/018-residence-registration/spec.md`

## Summary

Feature 018 là phần 6.10 của module M02 (UC-80, UC-81; BR-M02-11 → 14; DBR-33). Hành chính lập khai báo khi được nhắc, ghi nộp và kết quả; hệ thống xác định loại khai báo theo thời gian lưu trú cộng dồn, nhắc quá hạn, nhắc gia hạn, chuyển Hết hạn, nhắc khai báo xóa; không nộp hồ sơ thay, không kết nối Cổng dịch vụ công.

Hướng kỹ thuật:
- Gói `m02.khaibao` cạnh 004 trong M02, đọc hợp đồng qua dịch vụ nội bộ M02 (research Z-01).
- Nghĩa vụ khai báo là số liệu tính, giữ đệm kỹ thuật `nghia_vu_khai_bao` để nhắc đúng ngày, chạy bù (Z-03).
- `TongThoiGianLuuTru`, `LoaiKhaiBao` là hàm thuần (Z-03); DBR-33 bằng hai cột sinh `UNIQUE` (Z-04).
- Bean trạng thái không chặn lệnh nào; hủy khai báo đang xử lý khi trạng thái cuối (Z-05).

Plan tuân theo FR-010a của 000 (Q-306): hệ thống không tạo khai báo, khai báo gia hạn, việc khai báo xóa; chỉ nhắc, chuyển Hết hạn theo giờ, chuyển Được thay thế và Đã hủy kèm lệnh.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX) (000 R-01). Không có màn trên app di động.

**Primary Dependencies**: Như 000 → 017. Không thêm thư viện.

**Storage**: MySQL 8.0, schema M02. Bảng ở [data-model.md](data-model.md).

**Testing**: Như 000 R-14; test bảng hai hàm thuần; Test tích hợp MySQL cho DBR-33; đồng hồ giả lập cho CFG-M02-09, 11 → 13.

**Target Platform**: Web (Hành chính, Quản lý viện).

**Project Type**: Web service (REST `/api/v1`) và web client.

**Performance Goals**: Job hằng ngày cho 300 hồ sơ ≤ 1 phút; ghi nộp, kết quả ≤ 2 phút thao tác (SC-005).

**Constraints**: Mỗi lệnh một endpoint; nội dung ghi (nộp, kết quả, xóa) nhóm 3 qua đính chính; không chặn Hoàn tất tiếp nhận, Kết thúc lưu trú (SC-004).

**Scale/Scope**: 1 thực thể có vòng đời; 5 bảng (gồm kỹ thuật); 1 job; 3 đường dẫn, 4 thao tác REST ([contracts/api-018.yaml](contracts/api-018.yaml)); 5 SPI hoặc provider cài; 1 giao diện thống kê.

Không còn mục NEEDS CLARIFICATION.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | x-fr, x-br; data-model mục 5 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi |
| III. Không mặc định CRUD | Đạt | Chỉ lệnh theo bảng FR-005 |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển data-model mục 3 khớp FR-005 |
| V. Không cố định giá trị | Đạt | CFG-M02-09, 11 → 13 |
| VI. Vai trò, quyền | Đạt | Hành chính thực hiện, Quản lý viện xem (FR-009) |
| VII. Điểm chưa rõ | Đạt | Điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-005 ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế |
| X. Kiến trúc | Đạt | Gói con của `m02`; không có vòng |
| XI. Nghiệp vụ ở backend | Đạt | Loại, hạn, nghĩa vụ ở backend |
| XII. Dữ liệu | Đạt | DBR-33 bằng `UNIQUE`; nhật ký trong transaction |
| XIII. Tham số | Đạt | Hạn chốt lúc ghi mốc; đổi CFG không đổi hạn đã có |
| XIV. Ngoại tuyến, thông báo | Đạt | Không ngoại tuyến; thông báo Nhẹ (FR-010) |
| XV. Truy vết, kiểm thử | Đạt | Test theo BR-M02-11 → 14, DBR-33 |

**Kiểm lại sau Phase 1**: Đạt.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 018 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 hồ sơ; vài chục khai báo mỗi tháng |
| NFR-02 Hiệu năng | Có | Mỗi lệnh ghi một khai báo dưới khóa `khai_bao_nct`; ghi nộp, kết quả ≤ 2 phút thao tác (SC-005) |
| NFR-03 Thông báo khẩn cấp | Không | Thông báo mức Nhẹ (FR-010) |
| NFR-04 Tác vụ định kỳ | Có | Job hằng ngày cho 300 hồ sơ ≤ 1 phút; `nhac_khai_bao` `UNIQUE (loai, nct_id, ngay)` chống nhắc trùng; chạy bù (research Z-06) |
| NFR-05 Khả dụng | Gián tiếp | Listener sự kiện của 004 idempotent theo mốc nghĩa vụ |
| NFR-06 Sao lưu | Có | Bảng khai báo ở MySQL; bằng chứng ở kho tệp của 000 |
| NFR-07 Lưu trữ | Có | Theo hồ sơ (CFG-M15-03, `LuuTruProvider`) |
| NFR-08 Bảo mật | Có | Chỉ Hành chính, Quản lý viện; tệp bằng chứng `M02.BANG_CHUNG_KHAI_BAO` |
| NFR-09 Thời gian | Có | `Clock`; hạn khai báo theo ngày Asia/Ho_Chi_Minh |
| NFR-10 Thiết bị | Có | Màn trên web; không có màn trên app di động |
| NFR-11 Ngôn ngữ | Có | Nhãn tiếng Việt; ngày dd/MM/yyyy |
| NFR-12 Cấu hình | Có | CFG-M02-09, 11 → 13 đọc qua `ThamSoApi` |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập cho hạn, nhắc, hết hạn |

## Project Structure

### Documentation (this feature)

```text
specs/018-residence-registration/
├── plan.md  ├── research.md (Z-01 → Z-07)  ├── data-model.md  ├── quickstart.md (Q1 → Q20)
├── contracts/ (api-018.yaml, module-interfaces.md)
└── tasks.md   # Phase 2 (chưa tạo)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai; giai đoạn thiết kế không tạo (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m02/khaibao/   {internal, web, lich}   # nghĩa vụ, khai báo, TongThoiGianLuuTru, LoaiKhaiBao, bean trạng thái
backend/src/main/java/com/cambae/backend/m02/api/       # (bổ sung) KhaiBaoThongKeApi
frontend/src/tinh-nang/khai-bao-tam-tru/
```

**Structure Decision**: Theo R-02 của 000 và 004: 018 thêm gói `m02.khaibao`.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Bảng đệm `nghia_vu_khai_bao` | Nhắc người chưa được lập khai báo đúng ngày, chạy bù, báo Quản lý viện một lần (FR-002) | Tính lại từ lịch sử 001, 004 mỗi lần chạy: phải tái dựng mốc từ nhiều module, khó chống báo trùng |

## Đồng bộ với hợp đồng của feature khác

1. **004** (cùng M02): dùng sự kiện `TiepNhanHoanTat`, `PhuLucDaApDung`, `HopDongDaDoiTrangThai` đã khai cho 018. Không đổi.
2. **001**: cài `ThamGiaChuyenTrangThai` không có điều kiện chặn. **009**: thông báo Nhẹ; **016** đọc `KhaiBaoThongKeApi`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX).

1. **Căn cứ pháp lý chưa xác nhận** (spec, mục cập nhật 2026-10-06; 24.4): viện có thuộc "cơ sở lưu trú khác" theo Luật số 118/2025/QH15 không, và hạn thông báo (CFG-M02-12). Thiết kế để mọi ngưỡng là tham số nên không chặn; cần tư vấn pháp lý xác nhận trước vận hành. *(Còn mở 2026-10-10: chờ tư vấn pháp lý, mục 24.4.)*
2. **Mốc phần nội trú có hiệu lực** (FR-001a): spec nêu "hợp đồng mới hoặc phụ lục đổi loại lưu trú được áp dụng"; plan lấy mốc là ngày hiệu lực của phụ lục hoặc ngày bắt đầu hợp đồng nội trú mới (sự kiện của 004). Nếu ngày hiệu lực ở tương lai, hạn khai báo tính từ ngày đó chứ không từ lúc áp dụng. Đề nghị xác nhận ở FR-001a, FR-002. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
