# Implementation Plan: Lịch ca xoay vòng, phủ ca tối thiểu, đổi ca và nghỉ đột xuất

> **Sửa 2026-10-29 (Q-397):** bỏ mẫu xoay ca, nhóm xoay ca, lập lịch từ mẫu, phủ tối thiểu, thiếu ca, cảnh báo thiếu phủ, lời mời nhận ca; 015 chỉ còn đổi ca và nghỉ đột xuất. Nghỉ "theo ngày" mở cho mọi nhân viên, người lập chọn phạm vi. Phần dưới còn nhắc các phần đã bỏ thì bỏ qua; spec là căn cứ.

**Branch**: `015-shift-rotation-swap` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/015-shift-rotation-swap/spec.md`

## Summary

Feature 015 là phần lịch ca xoay vòng của module M09 (mục 13.2; UC-50 phần lập từ mẫu, UC-54; BR-M09-09 → 11). Nó cung cấp:
1. Mẫu xoay ca, nhóm xoay ca, thành viên với vị trí chu kỳ nối tiếp qua các tháng (mục A; Q-09, Q-190).
2. Yêu cầu phủ tối thiểu theo vai trò (CFG-M09-07), tính phủ từng ca có xét giấy phép, đào tạo (mục A; Q-181, Q-188, Q-221).
3. Lệnh "Lập lịch từ mẫu xoay ca", "Lập lại", báo cáo lập lịch; nhắc lập, nhắc công bố (mục B; Q-306).
4. Kiểm tra phủ khi công bố (bổ sung lệnh công bố của 008; Q-178).
5. Yêu cầu đổi ca hai chiều và nhận thay: người nhận đồng ý, trưởng tầng duyệt (mục D; Q-179, Q-191).
6. Yêu cầu nghỉ đột xuất theo ca và theo ngày, duyệt một phần (mục E; Q-81, Q-182, Q-184).
7. Cảnh báo thiếu phủ; trưởng tầng tự tra cứu và mời người thay (mục F; Q-307).

Hướng kỹ thuật:
- Thêm ba gói con vào `m09` cùng 008; mọi thay đổi ca, phân công, người phụ trách đi qua port nội bộ của 008 (N-07) và hai port bổ sung `LichNhapLenhPort`, `NghiCoDuyetPort` (research X-01, X-03).
- Xếp ca theo chu kỳ và lập lịch tháng là hàm thuần; tính phủ là bài toán phân người vào dòng yêu cầu bằng luồng cực đại với tie-break theo thứ tự khai báo (X-05, X-06).
- Đổi ca, nghỉ đột xuất là loại yêu cầu phê duyệt của 000 với vòng đời ngoại lệ đã khai ở 000 FR-031; tác động khi duyệt trong một transaction (X-08, X-09).
- Cảnh báo thiếu phủ một mở mỗi ca; tra cứu người thay không xếp hạng; lời mời nhận là lệnh bổ sung vào ca (X-10).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 015 không tự lập lịch, không chỉ định người phụ trách, không gợi ý hay xếp hạng người thay, không tự mời;
- hệ thống chỉ: xếp nhân viên theo mẫu kèm lệnh lập lịch; tính phủ, số giờ để hiển thị; mở, cập nhật, đóng cảnh báo thiếu phủ; chuyển Hết hiệu lực, tách ca theo giờ; nhắc, thông báo.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 014. Không thêm thư viện (luồng cực đại viết trong mã, đồ thị nhỏ).

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md), cùng schema M09 với 008.

**Testing**:
- Như 000 R-14.
- Test bảng cho bảy hàm thuần (research X-13); SC-001 với 3 mẫu × 4 tháng; bộ 200 yêu cầu (SC-004).
- Test tích hợp MySQL cho `UNIQUE` (cảnh báo mở, lời mời chờ, yêu cầu mở mỗi ca) và trigger nhóm 3.
- Test đồng thời: hai lệnh lập lịch cùng phạm vi; duyệt đổi ca và ghi vắng ca; hai người nhận lời mời.
- Đồng hồ giả lập cho CFG-M09-02, 06, 10, 11, CFG-M15-05, 06.
- Bản giả `CongViecCaApi` (005), `ChuyenDiNhiemVuLenhApi` (014).

**Target Platform**: Như 000.
- Web: mẫu, nhóm xoay ca, lập lịch, báo cáo, phủ, công bố (008), duyệt đổi ca, nghỉ, cảnh báo thiếu phủ, tra cứu, lời mời.
- App nhân viên: lập đổi ca, đồng ý/từ chối, lập nghỉ, trả lời lời mời; xem ngoại tuyến yêu cầu và lời mời của mình.
- App người thân: không có màn của 015.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client liên quan.

**Performance Goals**:
- Lập lịch một phạm vi ≤ 30 giây với 40 nhân viên (SC-002): một transaction, ghi lô qua port.
- Cảnh báo thiếu phủ ≤ 1 phút sau thay đổi (SC-005): tính phủ ngay sau commit.
- Nhắc mốc ≤ 1 phút sau 00:00 (SC-002).
- Tính phủ một ca < 50 ms (≤ 30 người, ≤ 10 dòng).

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI). Mọi lệnh chỉ trực tuyến (FR-043).
- 015 không ghi trực tiếp bảng của 008; chỉ qua port.
- Lần lập lịch, lần tính phủ, nghỉ theo ngày không bị `UPDATE`, `DELETE` (constitution XII).
- Lý do nghỉ, đổi ca chỉ hiển thị cho người được xem (FR-045). Nhật ký trong transaction. CFG đọc lúc chạy.

**Scale/Scope**:
- 7 thực thể có vòng đời (mẫu, nhóm, yêu cầu đổi ca, yêu cầu nghỉ, mục nghỉ, cảnh báo thiếu phủ, lời mời); 17 bảng (gồm bảng con, kỹ thuật).
- 2 job (theo phút, nửa đêm).
- 21 đường dẫn, 26 thao tác REST ([contracts/api-015.yaml](contracts/api-015.yaml)).
- 2 port nội bộ bổ sung (1 do 008 cài, 1 do 015 cài); 1 giao diện thống kê cho 016.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR; x-fr, x-br trên mọi thao tác REST |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Yêu cầu, cảnh báo, lời mời chỉ có lệnh theo bảng trạng thái; chỉ danh mục nhóm 1 có sửa |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 4 → 6 khớp bốn bảng trạng thái của spec; quickstart mục 4 |
| V. Không cố định giá trị | Đạt | CFG-M09-02, 06, 07, 10, 11, CFG-M15-04 → 06 đọc từ `ThamSoApi`; mẫu xoay ca cấu hình được |
| VI. Vai trò, quyền | Đạt | Bảng X-11; ngoại lệ Q-177 cho ca toàn viện |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ; điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-008 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 008 được bổ sung port, sự kiện nội bộ (mục "Đồng bộ") |
| X. Kiến trúc | Đạt | Một khối; gói con của `m09`; port nội bộ có ArchUnit. Không có vòng (X-02) |
| XI. Nghiệp vụ ở backend | Đạt | Xếp ca, tính phủ, kiểm tra đổi ca, điều kiện người thay ở backend |
| XII. Dữ liệu | Đạt | `UNIQUE` và trigger ở data-model mục 7; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V; đổi CFG không phát bù mốc đã qua |
| XIV. Ngoại tuyến, thông báo | Đạt | Chỉ trực tuyến; thông báo Nhẹ/Trung bình theo FR-044; người ngoài ca chỉ trong ứng dụng (Q-186) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR; BR-M09-09 → 11 có test (quickstart mục 4) |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Bổ sung kỹ thuật giải thích ở Complexity Tracking.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 015 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | ~40 nhân viên, ~60 ca mỗi tầng mỗi tháng |
| NFR-02 Hiệu năng | Có | Lập lịch một transaction; tính phủ theo ca |
| NFR-03 Thông báo khẩn cấp | Không | Không có mức Khẩn cấp |
| NFR-04 Tác vụ định kỳ | Có | Hai job; `moc_nhac_lich`; chạy bù trong ngày |
| NFR-05 Khả dụng | Gián tiếp | Listener tính phủ idempotent theo lần tính |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL |
| NFR-07 Lưu trữ | Có | ≥ CFG-M15-04 và ≥ ca liên quan |
| NFR-08 Bảo mật | Có | Lý do nhân sự giới hạn trường; tra cứu chỉ người nhận cảnh báo |
| NFR-09 Thời gian | Có | `Clock`; ca kéo qua tháng theo giờ bắt đầu |
| NFR-10 Thiết bị | Có | App nhân viên cho yêu cầu, lời mời |
| NFR-11 Ngôn ngữ | Có | Tiếng Việt; giờ HH:mm |
| NFR-12 Cấu hình | Có | CFG-M09 đổi qua tham số |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả 005, 014 |

## Project Structure

### Documentation (this feature)

```text
specs/015-shift-rotation-swap/
├── plan.md                      # File này
├── research.md                  # Phase 0: X-01 → X-13
├── data-model.md                # Phase 1: bảng, bảng chuyển, DBR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng Q1 → Q38, truy vết → test
├── contracts/
│   ├── api-015.yaml             # OpenAPI 3.1: xoay ca, lập lịch, phủ, đổi ca, nghỉ, lời mời
│   └── module-interfaces.md     # Port nội bộ M09, SPI 015 cài, giao diện cho 016
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m09/
├── api/           # (bổ sung) XoayCaThongKeApi
├── xoayca/        {internal, web, lich}   # mẫu, nhóm, MauCaTaiNgay, LapLichThang, lập lịch, lập lại
├── phuca/         {internal, web}         # DongApDung, TinhPhu, lần tính, cảnh báo thiếu phủ, KiemTraCongBoMoRong
└── doica/         {internal, web, lich}   # đổi ca, nghỉ đột xuất, NghiCoDuyetPort, tra cứu người thay, lời mời

frontend/src/tinh-nang/lich-ca/
├── xoay-ca/  ├── phu-ca/  ├── doi-ca/  ├── nghi/  └── loi-moi/

mobile/lib/nhan-vien/lich-ca/   # đổi ca, nghỉ, lời mời
```

**Structure Decision**: Theo R-02 của 000 và 008 N-01: 015 thêm ba gói con vào `backend/…/m09`.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Luồng cực đại khi tính phủ | FR-006 đòi số dòng đạt lớn nhất khi người có nhiều vai trò, kết quả duy nhất | Tham lam theo thứ tự: sai khi người nhiều vai trò bị xếp vào dòng đầu |
| `LichNhapLenhPort`, `NghiCoDuyetPort` bổ sung vào M09 | Lịch Nháp thuộc 008; nghỉ theo ngày thuộc 015 nhưng 008 cần cảnh báo khi sửa tay | 015 ghi thẳng bảng lịch: phá ranh giới gói đã chốt ở 008 N-07 |
| `yeu_cau_ca_mo` kỹ thuật | Một ca của một nhân viên không nằm trong hai yêu cầu mở (FR-019, FR-026) | Kiểm bằng truy vấn không khóa: hai yêu cầu đồng thời cùng lọt |

## Đồng bộ với hợp đồng của feature khác

1. **008** ([../008-staff-shift-handover/contracts/module-interfaces.md](../008-staff-shift-handover/contracts/module-interfaces.md) mục 1.5): thêm `LichNhapLenhPort` (008 cài), `NghiCoDuyetPort` (015 cài), sự kiện nội bộ M09 `LichNhapDaLuu`, `GiayPhepDaDoiHieuLuc`, `DaoTaoDaDoiHieuLuc`. Dùng `CaLenhPort`, `PhanCongLenhPort`, `KiemTraCongBoMoRong` đúng chữ ký N-07.
2. **005, 014**: gọi `CongViecCaApi.khiNghiCoDuyet`, `khiKetThucPhanCong`, `ChuyenDiNhiemVuLenhApi.goNhiemVu` đúng chữ ký đã khai. Không đổi.
3. **000**: dùng ngoại lệ vòng đời đã khai ở 000 FR-031 cho `M09.DOI_CA`, `M09.NGHI_DOT_XUAT`. **009**: thông báo cài `CoYeuCauThongBao`; **016** đọc `XoayCaThongKeApi`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Câu chữ còn lẫn trong spec 015** (không đổi nghiệp vụ): *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - FR-023, FR-040 và tiêu đề bảng FR-040 dẫn "Permission Matrix 4.4" (của `docs/phan-tich-yeu-cau.md` đã bỏ; tra Phụ lục 27, 19.3 là căn cứ); Assumptions dòng đầu dẫn mục 4.2 của tài liệu đã bỏ;
   - điểm báo lại 1 còn liệt kê ca toàn viện của "vệ sinh" (vai trò đã bỏ theo Q-282).

