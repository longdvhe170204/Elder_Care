# Implementation Plan: Người thân và cổng thông tin gia đình

> **Sửa 2026-10-29 (Q-396):** bỏ bản tin tuần (mục H, User Story 7, bảng `ban_tin*`, `/ban-tin`, `HoatDongBanTinPort`, `ViecBanTinM10`). Phần dưới còn nhắc bản tin thì bỏ qua; spec là căn cứ.

**Branch**: `012-family-portal` | **Date**: 2026-10-10, cập nhật 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/012-family-portal/spec.md`

## Summary

Feature 012 là module M10 – Người thân (mục 14.1 → 14.8; UC-55 → UC-60). Nó cung cấp:
1. Hồ sơ người thân, quan hệ, người đại diện, người liên hệ chính, thứ tự liên hệ, phiếu đăng ký người thân (mục A; DBR-02).
2. Sáu quyền theo từng quan hệ; quyền xem sức khỏe gắn bản đồng ý (mục B; DBR-03).
3. Yêu cầu thay đổi quyền, danh sách được phép đón, dấu "được tự về", đặt/thôi người đại diện, theo vòng đời riêng của BR-M10-07 (mục C).
4. Quy trình đón, lần chặn đón, ngoại lệ đón; bản ghi đón làm căn cứ cho lệnh của 004, 005 (mục D; BR-M10-03, Q-295).
5. Đăng ký thăm tự duyệt theo khung và sức chứa, vào/ra, tự hủy khi vắng hoặc khoanh vùng (mục E; BR-M10-02).
6. Người thân ở lại, đêm tính phí, suất ăn cho 011 (mục F; BR-M10-04).
7. Cổng người thân theo bốn loại thông tin, nhật ký lượt xem sức khỏe (mục G; BR-M10-01, NFR-08).
8. Bản tin tuần do Điều dưỡng lập và duyệt, số liệu tổng hợp tính sẵn, đính chính (mục H; Q-306, Q-307).
9. Phản hồi, khiếu nại có hạn, leo thang, tự đóng (mục I; BR-M10-05).
10. *(Bổ sung 2026-10-18)* Kênh người cao tuổi: năm phần thông tin của chính người cao tuổi, phản hồi của người cao tuổi (kể cả ghi thay, loại Mong muốn), nút SOS và trạng thái (mục G2; Q-365, Q-366, Q-369, Q-371).

Hướng kỹ thuật:
- Gói `m10` với tám gói con (research F-01). 012 cài chín SPI do module thấp hơn khai báo; `QuanHeNguoiThanApi` được hợp nhất thành SPI ở `m15.taikhoan.api` để 001, 002, 006, 011 không phụ thuộc M10 (F-02, F-14).
- Quyền có tác dụng là hàm thuần trên quan hệ và bản đồng ý tại thời điểm xem; lịch sử quyền nhóm 3 trả lời "ai được phép đón tại T" (F-04).
- DBR-02 bằng cột sinh `UNIQUE` (≤ 1 liên hệ chính) và kiểm bất biến dưới khóa theo người cao tuổi; điều kiện `DBR_02` cho Hoàn tất tiếp nhận (F-05).
- Bản ghi đón nhóm 3 với sự kiện `DA_DUNG` / `KHONG_DUNG`; 004, 005 đánh dấu dùng qua `daDung` trong transaction lệnh nguồn (F-07).
- Sức chứa khung thăm bằng dòng khóa `khung_tham_ngay` (F-08); đêm ở lại phát sự kiện tính phí cho 010 lúc 00:00 (F-09).
- Phần sức khỏe trên cổng đi qua 012 để kiểm quyền và ghi nhật ký tại một chỗ; chi phí, số dư, đồ gửi, hoạt động do REST của 010, 017, 013, 014 phục vụ và kiểm quyền qua `m10.api.QuyenNguoiThanApi` (F-10, F-11).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 012 không tạo bản tin, không soạn sẵn nội dung, không tạo khoản chi phí, không tự Từ chối yêu cầu, không tự kết thúc lượt ở lại, không tự gửi bản tin đính chính;
- hệ thống chỉ: tính sẵn số liệu khi Điều dưỡng soạn; tự duyệt hoặc từ chối đăng ký thăm theo điều kiện (FR-031); chuyển trạng thái theo giờ (Không đến, Hết hạn, Không dùng, Không gửi, tự đóng); tác động kèm lệnh (bỏ khỏi danh sách đón, hủy lượt thăm khi kết thúc quan hệ, khi vắng, khi khoanh vùng); nhắc, leo thang, thông báo.

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": Q-347, Q-348, Q-351 → Q-353)

- **Mong muốn về khách thăm, người đón** (FR-020a): bảng `mong_muon_nct` (nhóm 2, nội dung bất biến, thay thế bằng bản mới) có nguồn theo mẫu 001 (`NguonYKienApi`); ghi bởi Hành chính, Trưởng tầng, Điều dưỡng có phạm vi (research F-06a, data-model mục 3a).
- **Yêu cầu trái mong muốn** (FR-020b, FR-020c): `KiemTraYeuCau` thêm kiểm trái mong muốn; yêu cầu hoặc dòng phiếu trái mong muốn chỉ Hiệu lực qua Quản lý viện duyệt kèm lý do và nội dung đã trao đổi với người cao tuổi; bỏ người đón vẫn hiệu lực ngay; người đại diện theo pháp luật (đọc qua `BanDongYApi.daiDienPhapLuat` của 001) không bị chặn. Cảnh báo "quyền trái mong muốn đang hiệu lực" tính lúc đọc, không job.
- **Sự cố trên cổng và bản tin** (FR-046, FR-052, Q-352): dùng `SuCoNguoiThanView` của 007 (phần chung kèm mức cho mọi người thân có quan hệ; phần sức khỏe khi có quyền xem sức khỏe).
- **Bản tin và thông báo** (FR-054, FR-069, Q-353): mọi yêu cầu tới người thân khai đối tượng thông tin theo 009 FR-001a (bảng ở research F-15); bản tin là `VE_NGUOI_CAO_TUOI`, 009 lọc theo mục "nhận thông báo".
- **Người đại diện** (Q-348): giữ tên; `laNguoiDaiDienHieuLuc` không còn dùng cho 001 FR-011, FR-023c.
- Điểm báo lại 6 → 9 bên dưới.

### Cập nhật 2026-10-18, lượt 3 (spec mục "Cập nhật 2026-10-18, lượt 3": Q-365 → Q-371)

- **Kênh người cao tuổi** (FR-083, FR-084): gói mới `m10.kenhnct`, REST `/kenh-nguoi-cao-tuoi/**` chỉ cho tài khoản AC-16, phạm vi `BAN_THAN` của 002; DTO riêng không có trường sức khỏe, chi phí, số dư, hợp đồng (research F-18). 012 dựng cả năm phần ở một chỗ: lượt thăm (012), lịch đến/về bán trú (`CoMatBanTruApi` của 005), thực đơn và suất ăn dự kiến (`ThucDonChungApi`, `SuatAnDuKienApi` mới của 011), hoạt động sắp tới (SPI mới `HoatDongKenhNctPort`, 014 cài), đồ gửi (SPI mới `DoGuiKenhNctPort`, 013 cài).
- **Phản hồi của người cao tuổi** (FR-058, FR-058a, FR-058b, FR-066, FR-086): `phan_hoi` thêm `nguoi_gui_loai`, `ghi_thay_boi_id`, loại `MONG_MUON`, nguồn `KENH_NGUOI_CAO_TUOI`, `GHI_TRUC_TIEP`; bảng `phan_hoi_y_kien` gắn mục ý kiến; lệnh `doi-loai`, `gan-y-kien`; Trả lời loại Mong muốn bị chặn khi chưa gắn mục; mọi truy vấn của người thân lọc `nguoi_gui_loai = NGUOI_THAN` (research F-13, F-18).
- **Thông báo** (FR-069): nhắc lượt thăm trong ngày và phản hồi đã trả lời gửi nhóm `NguoiCaoTuoi` của 009, mức Nhẹ, phần `CHUNG` (research F-15).
- **Nút SOS** (FR-085): giao diện ở app người cao tuổi; lệnh `/sos` thuộc 007 (007 research S-23); app giữ số liên hệ nhận được gần nhất để hiện khi mất kết nối. *(2026-10-22: thêm "đang tới", "đã tới", gửi vị trí GPS, xem mục "Cập nhật 2026-10-22".)*
- Gói, bảng, SPI mới ở trên; không đổi bảng khác. Điểm báo lại 10 bên dưới.

### Cập nhật 2026-10-22 (spec mục "Cập nhật 2026-10-22": Q-373, Q-374, Q-377 → Q-379)

- **Chứng từ trên cổng** (FR-046 loại (3)): phiếu báo phí (PDF, Excel), bảng quyết toán (phiên bản hiện hành, PDF) và sao kê Excel, PDF do REST của 017 phục vụ (`/cong/nguoi-cao-tuoi/{nctId}/chung-tu`, `/cong/chung-tu/{loai}/{id}/file`, `/cong/nguoi-cao-tuoi/{nctId}/sao-ke`; 017 research E-10, E-11), cùng cách số dư đã làm (research F-10); 017 kiểm `QuyenNguoiThanApi.quyenCoTacDung(XEM_CHI_PHI)`. 012 không thêm bảng, SPI hay REST; màn trên web và app người thân do task của 017 (T011, T036) và 012 T073 dựng.
- **Nút SOS** (FR-085): app người cao tuổi hiện "[họ tên] đang tới" (`DA_TIEP_NHAN`) và "[họ tên] đã tới" (`DA_TOI`) từ `GET /sos/hien-tai`; khi `guiViTri = true` và người cao tuổi đã cho quyền vị trí "khi đang dùng ứng dụng", app gửi `POST /sos/{lanBamId}/vi-tri` mỗi `chuKyGuiViTriGiay` (CFG-M05-09), dừng khi `guiViTri = false` hoặc nhận 409; không theo dõi nền (007 research S-24). Danh sách nơi chọn nhanh thêm hành lang, phòng sinh hoạt chung. Gói `geolocator` cho app (007 plan).
- **Thông báo** (FR-043): dòng 017 thêm phiếu báo phí, bảng quyết toán mới phát hành (Nhẹ, người thân có quyền xem chi phí, phần "chi phí") do 017 phát theo khuôn 009; bỏ thông báo sao kê chưa khớp, chốt quỹ ngày. Cảnh báo từ nút hỗ trợ khẩn cấp dùng nhóm (a) có sẵn; vị trí không vào nội dung thông báo.
- Không thêm bảng, gói, SPI.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 011. Không thêm thư viện.

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test bảng cho bảy hàm thuần (research F-17); SC-001 bộ 100 lượt đón; SC-008 bộ 30 ngày ở lại.
- Test tích hợp MySQL cho `UNIQUE` (liên hệ chính, yêu cầu chờ, bản tin mỗi kỳ, đêm ở lại, một lần dùng bản ghi đón) và trigger nhóm 3.
- Test đồng thời: 20 đăng ký cho chỗ cuối; hai người đại diện xác nhận cùng lúc; bỏ người đón cùng lúc với ghi đón.
- Đồng hồ giả lập cho CFG-M10-01 → 12, CFG-M01-04, mốc 00:00.
- Bản giả `HoatDongBanTinPort` và API đọc của 005, 006, 007, 010, 011.

**Target Platform**: Như 000.
- Web: hồ sơ người thân, quan hệ, yêu cầu, quy trình đón, thăm, ở lại, bản tin, phản hồi, nhật ký xem; cổng người thân trên trình duyệt (Q-305).
- App nhân viên: ghi đón (gồm Tự về, Viện đưa về), ghi vào/ra lượt thăm, soạn và duyệt bản tin, xử lý phản hồi.
- App người thân: cổng, đăng ký thăm, xác nhận yêu cầu và ngoại lệ, phản hồi, bản tin.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Yêu cầu bỏ người đón có hiệu lực ≤ 1 phút (SC-003): áp trong lệnh lập.
- Đăng ký thăm ≤ 1 giây phía máy chủ; 20 đăng ký đồng thời xếp hàng trên một dòng khóa (SC-004).
- Leo thang phản hồi quá hạn ≤ 5 phút (SC-005): job theo phút.
- Lượt thăm cho truy vết ≤ 1 phút (SC-009): chỉ mục `(nct_id, ngay)`.
- Quy mô: 300 người cao tuổi, khoảng 1.000 người thân, vài chục lượt thăm mỗi ngày, 300 bản tin mỗi tuần.

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI). Quy trình đón chỉ trực tuyến (FR-028).
- Bản ghi đón, lần chặn, lịch sử quyền, nhật ký xem, bản tin đã gửi, nội dung phản hồi không bị `UPDATE`, `DELETE` (constitution XII).
- Người thân chỉ thấy giá trị hiện hành (FR-047). Nhật ký trong transaction (000 FR-047). CFG đọc lúc chạy; khung thăm chép vào lượt lúc đăng ký (FR-032).

**Scale/Scope**:
- 8 thực thể có vòng đời (quan hệ, yêu cầu quyền, ngoại lệ đón, bản ghi đón, lượt thăm, lượt ở lại, bản tin, phản hồi); 26 bảng (gồm bảng con, kỹ thuật).
- 4 job (theo phút, theo giờ, nửa đêm, theo lịch bản tin).
- 30 đường dẫn, 39 thao tác REST ([contracts/api-012.yaml](contracts/api-012.yaml)).
- 13 SPI hoặc provider 012 cài; 1 SPI khai báo cho 014; 2 giao diện công khai.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR; x-fr, x-br trên mọi thao tác REST; data-model mục 11 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Quan hệ, yêu cầu, ngoại lệ, lượt thăm, lượt ở lại, bản tin, phản hồi chỉ có lệnh theo bảng trạng thái; chỉ hồ sơ người thân (nhóm 1) có sửa |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3 → 9 khớp sáu bảng trạng thái của spec; quickstart mục 4 |
| V. Không cố định giá trị | Đạt | CFG-M10-01 → 12, CFG-M01-04, CFG-M15-03 đọc từ `ThamSoApi` |
| VI. Vai trò, quyền | Đạt | Bảng FR-067 (F-16); quy trình đón theo quyền của lệnh nguồn; người thân kiểm quan hệ và quyền có tác dụng ở backend |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ; điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-010 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 001, 002, 004, 005, 006, 007 được bổ sung (mục "Đồng bộ") |
| X. Kiến trúc | Đạt | Một khối; gói `m10`; module khác dùng `m10.api` hoặc SPI ở module thấp hơn. Không có vòng (F-02) |
| XI. Nghiệp vụ ở backend | Đạt | Điều kiện thăm, đón, DBR-02, quyền có tác dụng, hạn phản hồi ở backend; client chỉ hiển thị |
| XII. Dữ liệu | Đạt | DBR-02, DBR-03 ở data-model mục 11; nhóm 3 bằng trigger; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V; khung thăm chép vào lượt, CFG-M10-09 chép vào bản ghi đón lúc ghi |
| XIV. Ngoại tuyến, thông báo | Đạt | Quy trình đón bắt buộc trực tuyến (Q-01); không đăng ký `NgoaiTuyenHandler`. Thông báo theo mức FR-069 |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR; mỗi BR-M10-01 → 09 có test (quickstart mục 4) |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Bổ sung kỹ thuật giải thích ở Complexity Tracking.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 012 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | Chỉ mục theo người cao tuổi, người thân, ngày |
| NFR-02 Hiệu năng | Có | Đăng ký thăm một dòng khóa; cổng gọi API nguồn theo phần |
| NFR-03 Thông báo khẩn cấp | Gián tiếp | 012 cung cấp người liên hệ chính, người đại diện, thứ tự gọi cho 007, 009 |
| NFR-04 Tác vụ định kỳ | Có | Bốn job; cột "đã làm lúc"; chạy bù |
| NFR-05 Khả dụng | Gián tiếp | Listener idempotent |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL |
| NFR-07 Lưu trữ | Có | Theo hồ sơ (CFG-M15-03) |
| NFR-08 Bảo mật | Có | Nhật ký lượt xem sức khỏe; giấy tờ tùy thân là trường cá nhân; DTO người thân riêng |
| NFR-09 Thời gian | Có | `Clock`; mốc 00:00, kỳ bản tin, khung thăm theo Asia/Ho_Chi_Minh |
| NFR-10 Thiết bị | Có | Cổng hai kênh cùng chức năng (Q-305) |
| NFR-11 Ngôn ngữ | Có | Tiếng Việt; ngày dd/MM/yyyy |
| NFR-12 Cấu hình | Có | CFG-M10 đổi qua tham số; khung thăm mới chỉ áp cho đăng ký mới |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả port |

## Project Structure

### Documentation (this feature)

```text
specs/012-family-portal/
├── plan.md                      # File này
├── research.md                  # Phase 0: F-01 → F-17
├── data-model.md                # Phase 1: bảng, bảng chuyển, DBR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng Q1 → Q49, truy vết → test
├── contracts/
│   ├── api-012.yaml             # OpenAPI 3.1: người thân, yêu cầu quyền, đón, thăm, ở lại, cổng, bản tin, phản hồi
│   └── module-interfaces.md     # SPI 012 cài, API cung cấp, SPI cho 014, sự kiện
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m10/
├── api/           # QuyenNguoiThanApi, NguoiThanThongKeApi, HoatDongBanTinPort, sự kiện; bản cài SPI
├── nguoithan/     {internal, web}   # hồ sơ, quan hệ, KiemTraDbr02, phiếu đăng ký
├── quyen/         {internal, web}   # yêu cầu quyền, QuyenCoTacDung, lịch sử quyền, dấu tự về
├── don/           {internal, web}   # bản ghi đón, lần chặn, ngoại lệ, BanGhiDonPort, BanGhiDonBanTruPort
├── tham/          {internal, web}   # lượt thăm, khung, KiemTraDangKyTham
├── olai/          {internal, web}   # lượt ở lại, SoDem, LuotOLaiAnPort
├── cong/          {internal, web}   # cổng, phần sức khỏe, nhật ký xem
├── bantin/        {internal, web}   # bản tin, số liệu, đính chính
├── phanhoi/       {internal, web}   # phản hồi, hạn, leo thang
└── lich/                            # ViecTheoPhutM10, ViecHangGioM10, ViecNuaDemM10, ViecBanTinM10

frontend/src/tinh-nang/nguoi-than/
├── ho-so/  ├── yeu-cau-quyen/  ├── don/  ├── tham/  ├── o-lai/  ├── ban-tin/  ├── phan-hoi/  └── cong-nguoi-than/

mobile/lib/nhan-vien/nguoi-than/   # ghi đón, vào/ra, bản tin, phản hồi
mobile/lib/nguoi-than/             # cổng, thăm, yêu cầu, phản hồi, bản tin
```

**Structure Decision**: Theo R-02 của 000. Feature 012 nằm ở `backend/…/m10` với tám gói con và gói `api`. Web có đủ chức năng; app di động chung (Q-346) chia phần nhân viên và phần người thân theo loại tài khoản.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Hợp nhất `QuanHeNguoiThanApi` thành SPI ở `m15.taikhoan.api` | 001, 002 (plan trước) gọi dữ liệu quan hệ của 012 | Để 001, 002 phụ thuộc M10: vòng M10 ↔ M01, M15 |
| `lich_su_quyen` nhóm 3 | FR-068 đòi biết ai được phép đón tại thời điểm bất kỳ; FR-017 khôi phục không hồi tố | Dựng lại từ nhật ký 000: nhật ký là dữ liệu kiểm toán, không dùng làm nguồn nghiệp vụ |
| Sự kiện bản ghi đón tách bảng | Bản ghi đón là nhóm 3 nhưng có "Đã dùng", "Không dùng" (phân loại dữ liệu của spec) | Cột trạng thái trên bản ghi: phá ràng buộc nhóm 3 |
| `khung_tham_ngay` kỹ thuật | Sức chứa chung cho cả viện cần một điểm khóa (SC-004) | Đếm không khóa: hai đăng ký cùng lấy chỗ cuối |
| Cổng sức khỏe đi qua 012 | Một chỗ kiểm quyền có tác dụng và ghi nhật ký xem (SC-002) | Mỗi module tự ghi: nhật ký nằm rải rác, khó bảo đảm 100% |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung các điểm dưới đây vào hợp đồng của feature đã làm plan; chữ ký cũ giữ nguyên.

1. **002** ([../002-account-permission/contracts/module-interfaces.md](../002-account-permission/contracts/module-interfaces.md) mục 2): `QuanHeNguoiThanApi` khai báo trong `m15.taikhoan.api` dưới dạng SPI, M10 cài; chữ ký hợp nhất ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.1. **001** ghi chú cùng khai báo.
2. **004**: `BanGhiDonPort.daDung(banGhiDonId, lenhNguon, nguonId)` (MANDATORY) gọi trong lệnh Cho tạm vắng và bàn giao khi Kết thúc lưu trú.
3. **005**: `BanGhiDonBanTruPort.daDung(banGhiDonId, diemDanhId)` (MANDATORY) gọi trong lệnh Điểm danh về; thêm `KetQuaChamSocCongApi` (`ketQua`, `tongHop`) cho cổng và bản tin.
4. **006, 007**: chú thích "ghi nhật ký xem" của `ThuocCongNguoiThanApi`, `SucKhoeCongNguoiThanApi` sửa thành "012 ghi nhật ký lượt xem".
5. **009, 010, 011**: cài `NguoiThanNhanThongBaoPort`, `LuotOLaiAnPort`, phát `SuKienTinhPhiNguon(DEM_O_LAI)` và `LuotOLaiDaDoi` đúng chữ ký đã khai báo. Không đổi.
6. **013, 014, 016, 017, 018** (làm plan sau): dùng `m10.api.QuyenNguoiThanApi` để phục vụ phần cổng của mình; **014** cài `HoatDongBanTinPort` và giữ bản ghi "đồng ý về muộn" (FR-046d) ở REST của 014 với người đại diện kiểm qua `QuyenNguoiThanApi.laNguoiDaiDien`.
7. *(Bổ sung 2026-10-18)* **002**: `TaiKhoanApi.theoNguoiCaoTuoi`, phạm vi `BAN_THAN`. **007**: REST `/sos` cho app. **011**: hàm đọc mới `SuatAnDuKienApi.cuaNguoiCaoTuoi(nctId, tu, den)` ở `m08.api` (011 FR-063). **013** cài `DoGuiKenhNctPort` (013 FR-030a), **014** cài `HoatDongKenhNctPort` (014 FR-068a), cả hai khai báo ở `m10.api`. **009**: nhóm `NguoiCaoTuoi`. **001**, **014**: sự kiện `YKienDaGhiTuPhanHoi` (001 module-interfaces 1.8b) để gắn mục sở thích, thói quen vào phản hồi Mong muốn (FR-058b).

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Bảng quyền FR-067 thiếu Điều dưỡng ở dòng "Quy trình đón"**: FR-022 (Q-214) cho Điều dưỡng thực hiện quy trình đón cho Điểm danh về của bán trú, nhưng ô ĐD của dòng này ghi "—". Plan làm theo FR-022 (quyền theo lệnh nguồn). Đề nghị sửa ô ĐD của FR-067 và đối chiếu Phụ lục 27 chú thích ²⁷. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

2. **Hai FR cùng mã FR-046c**: một FR-046c về thông báo số dư, nguy kịch (Q-211, Q-213), một FR-046c về đồng ý về muộn (Q-167, Q-176). Plan gọi FR thứ hai là "FR-046c (đồng ý về muộn)". Đề nghị đánh lại mã một FR (ví dụ FR-046d) khi sửa spec. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Mốc Trưởng tầng nhận xét bản tin**: bảng FR-057 ghi Trưởng tầng nhận xét "sau mốc báo của CFG-M10-11", trong khi FR-053 cho Trưởng tầng nhận xét và duyệt thay sau CFG-M10-12 kể từ mốc của kỳ (CFG-M10-11 là tham số của yêu cầu thay đổi quyền). Plan dùng CFG-M10-12 theo FR-053. Đề nghị sửa dòng của FR-057. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

4. **Đồng ý về muộn trên cổng** (FR-046c thứ hai): spec 012 cho người đại diện ghi trên cổng, spec 014 FR-016a cho Hành chính ghi nhận đồng ý qua bản ký. Plan để bản ghi thuộc 014, người đại diện ghi qua REST của 014 (kiểm người đại diện qua 012). Đề nghị ghi rõ ở 014 FR-016a rằng có hai nguồn ghi (cổng, bản ký). *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

5. **Câu chữ còn lẫn trong spec 012** (không đổi nghiệp vụ): FR-014, FR-015, FR-059, FR-060, FR-067 dẫn "4.4" (Permission Matrix của `docs/phan-tich-yeu-cau.md` đã bỏ; tra Phụ lục 27); Assumptions dòng đầu dẫn mục 4.2 của tài liệu đã bỏ. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

6. **(2026-10-18) FR-071 của spec 012 còn dòng "Cung cấp 001: Người đại diện hợp lệ để làm người đồng ý (FR-011 của 001)".** Sau Q-348, 001 FR-011 chỉ nhận người đại diện theo pháp luật kèm quyết định của Tòa án; 012 chỉ cung cấp "người thân có quan hệ Hiệu lực". Đề nghị sửa dòng này khi đồng bộ spec 012. *(Đã xử lý 2026-10-18: spec FR-071 đã sửa)*

7. **(2026-10-18) Bảng FR-069 chưa có cột đối tượng thông tin (009 FR-001a).** Plan khai theo cách hiểu ở research F-15 (bản tin, bản tin đính chính: về người cao tuổi; còn lại: về việc của chính người thân). Đề nghị thêm cột vào FR-069 khi cập nhật spec 012 (cùng việc với các spec 004, 006, 010, 011, 013, 014). *(Đã xử lý 2026-10-18: spec FR-069 đã khai đối tượng, khớp research F-15)*

8. **(2026-10-18) Nhận ra "cùng một người" giữa mong muốn và quyền.** FR-020a cho ghi mong muốn về người ngoài (họ tên, số giấy tờ). Plan coi yêu cầu trái mong muốn khi người được cấp quyền có quan hệ trùng `quan_he_id` hoặc hồ sơ người thân có số giấy tờ trùng số giấy tờ của mong muốn; không so họ tên. Đề nghị xác nhận. *(Đã xử lý 2026-10-18: giữ theo plan, chủ dự án ủy quyền chốt)*

9. **(2026-10-18) FR-020c dựa vào quyết định của Tòa án đã ghi ở 001.** 001 chỉ lưu quyết định của Tòa án trên bản đồng ý (FR-011). Plan nhận người đại diện theo pháp luật qua bản đồng ý Hiệu lực có người ký `DAI_DIEN_PHAP_LUAT`; có quyết định mà chưa ghi bản đồng ý thì hệ thống không biết. Liên quan plan 001 điểm báo lại 10. *(Đã xử lý 2026-10-18: giữ theo plan; người đại diện ngoài gia đình theo Q-359)*

10. **(2026-10-18) Màn kênh người cao tuổi là màn mới của app chung.** DESIGN.md mục 15 chưa có màn cho người cao tuổi (chữ lớn, một việc mỗi màn, nút SOS ở màn đầu). Không phải điểm nghiệp vụ; đề nghị bổ sung mục 15 trước khi dựng màn mẫu.

