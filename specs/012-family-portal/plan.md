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
- Gói `family` với tám gói con (research F-01). 012 cài chín SPI do module thấp hơn khai báo; `QuanHeNguoiThanApi` được hợp nhất thành SPI ở `administration.taikhoan.api` để 001, 002, 006, 011 không phụ thuộc M10 (F-02, F-14).
- Quyền có tác dụng là hàm thuần trên quan hệ và bản đồng ý tại thời điểm xem; lịch sử quyền nhóm 3 trả lời "ai được phép đón tại T" (F-04).
- DBR-02 bằng cột sinh `UNIQUE` (≤ 1 liên hệ chính) và kiểm bất biến dưới khóa theo người cao tuổi; điều kiện `DBR_02` cho Hoàn tất tiếp nhận (F-05).
- Bản ghi đón nhóm 3 với sự kiện `DA_DUNG` / `KHONG_DUNG`; 004, 005 đánh dấu dùng qua `daDung` trong transaction lệnh nguồn (F-07).
- Sức chứa khung thăm bằng dòng khóa `khung_tham_ngay` (F-08); đêm ở lại phát sự kiện tính phí cho 010 lúc 00:00 (F-09). *(R-15: `khung_tham_ngay` → `khoa_ky_thuat`)*
- Phần sức khỏe trên cổng đi qua 012 để kiểm quyền và ghi nhật ký tại một chỗ; chi phí, số dư, đồ gửi, hoạt động do REST của 010, 017, 013, 014 phục vụ và kiểm quyền qua `family.api.QuyenNguoiThanApi` (F-10, F-11).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 012 không tạo bản tin, không soạn sẵn nội dung, không tạo khoản chi phí, không tự Từ chối yêu cầu, không tự kết thúc lượt ở lại, không tự gửi bản tin đính chính;
- hệ thống chỉ: tính sẵn số liệu khi Điều dưỡng soạn; tự duyệt hoặc từ chối đăng ký thăm theo điều kiện (FR-031); chuyển trạng thái theo giờ (Không đến, Hết hạn, Không dùng, Không gửi, tự đóng); tác động kèm lệnh (bỏ khỏi danh sách đón, hủy lượt thăm khi kết thúc quan hệ, khi vắng, khi khoanh vùng); nhắc, leo thang, thông báo.

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": Q-347, Q-348, Q-351 → Q-353)

- **Mong muốn về khách thăm, người đón** (FR-020a): bảng `mong_muon_nct` (nhóm 2, nội dung bất biến, thay thế bằng bản mới) có nguồn theo mẫu 001 (`NguonYKienApi`); ghi bởi Hành chính, Trưởng tầng, Điều dưỡng có phạm vi (research F-06a, data-model mục 3a). *(R-15: `mong_muon_nct` → `ho_so_chung`)*
- **Yêu cầu trái mong muốn** (FR-020b, FR-020c): `KiemTraYeuCau` thêm kiểm trái mong muốn; yêu cầu hoặc dòng phiếu trái mong muốn chỉ Hiệu lực qua Quản lý viện duyệt kèm lý do và nội dung đã trao đổi với người cao tuổi; bỏ người đón vẫn hiệu lực ngay; người đại diện theo pháp luật (đọc qua `BanDongYApi.daiDienPhapLuat` của 001) không bị chặn. Cảnh báo "quyền trái mong muốn đang hiệu lực" tính lúc đọc, không job.
- **Sự cố trên cổng và bản tin** (FR-046, FR-052, Q-352): dùng `SuCoNguoiThanView` của 007 (phần chung kèm mức cho mọi người thân có quan hệ; phần sức khỏe khi có quyền xem sức khỏe).
- **Bản tin và thông báo** (FR-054, FR-069, Q-353): mọi yêu cầu tới người thân khai đối tượng thông tin theo 009 FR-001a (bảng ở research F-15); bản tin là `VE_NGUOI_CAO_TUOI`, 009 lọc theo mục "nhận thông báo".
- **Người đại diện** (Q-348): giữ tên; `laNguoiDaiDienHieuLuc` không còn dùng cho 001 FR-011, FR-023c.
- Điểm báo lại 6 → 9 bên dưới.

### Cập nhật 2026-10-18, lượt 3 (spec mục "Cập nhật 2026-10-18, lượt 3": Q-365 → Q-371)

- **Kênh người cao tuổi** (FR-083, FR-084): gói mới `family.kenhnct`, REST `/kenh-nguoi-cao-tuoi/**` chỉ cho tài khoản AC-16, phạm vi `BAN_THAN` của 002; DTO riêng không có trường sức khỏe, chi phí, số dư, hợp đồng (research F-18). 012 dựng cả năm phần ở một chỗ: lượt thăm (012), lịch đến/về bán trú (`CoMatBanTruApi` của 005), thực đơn và suất ăn dự kiến (`ThucDonChungApi`, `SuatAnDuKienApi` mới của 011), hoạt động sắp tới (SPI mới `HoatDongKenhNctPort`, 014 cài), đồ gửi (SPI mới `DoGuiKenhNctPort`, 013 cài).
- **Phản hồi của người cao tuổi** (FR-058, FR-058a, FR-058b, FR-066, FR-086): `phan_hoi` thêm `nguoi_gui_loai`, `ghi_thay_boi_id`, loại `MONG_MUON`, nguồn `KENH_NGUOI_CAO_TUOI`, `GHI_TRUC_TIEP`; bảng `phan_hoi_y_kien` gắn mục ý kiến; lệnh `doi-loai`, `gan-y-kien`; Trả lời loại Mong muốn bị chặn khi chưa gắn mục; mọi truy vấn của người thân lọc `nguoi_gui_loai = NGUOI_THAN` (research F-13, F-18). *(R-15: `phan_hoi_y_kien` → `su_kien_chung`)*
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
| X. Kiến trúc | Đạt | Một khối; gói `family`; module khác dùng `family.api` hoặc SPI ở module thấp hơn. Không có vòng (F-02) |
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
backend/src/main/java/com/eldercare/backend/family/
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

**Structure Decision**: Theo R-02 của 000. Feature 012 nằm ở `backend/…/family` với tám gói con và gói `api`. Web có đủ chức năng; app di động chung (Q-346) chia phần nhân viên và phần người thân theo loại tài khoản.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Hợp nhất `QuanHeNguoiThanApi` thành SPI ở `administration.taikhoan.api` | 001, 002 (plan trước) gọi dữ liệu quan hệ của 012 | Để 001, 002 phụ thuộc M10: vòng M10 ↔ M01, M15 |
| `lich_su_quyen` nhóm 3 | FR-068 đòi biết ai được phép đón tại thời điểm bất kỳ; FR-017 khôi phục không hồi tố | Dựng lại từ nhật ký 000: nhật ký là dữ liệu kiểm toán, không dùng làm nguồn nghiệp vụ *(R-15: `lich_su_quyen` → `lich_su_chung`)* |
| Sự kiện bản ghi đón tách bảng | Bản ghi đón là nhóm 3 nhưng có "Đã dùng", "Không dùng" (phân loại dữ liệu của spec) | Cột trạng thái trên bản ghi: phá ràng buộc nhóm 3 |
| `khung_tham_ngay` kỹ thuật | Sức chứa chung cho cả viện cần một điểm khóa (SC-004) | Đếm không khóa: hai đăng ký cùng lấy chỗ cuối *(R-15: `khung_tham_ngay` → `khoa_ky_thuat`)* |
| Cổng sức khỏe đi qua 012 | Một chỗ kiểm quyền có tác dụng và ghi nhật ký xem (SC-002) | Mỗi module tự ghi: nhật ký nằm rải rác, khó bảo đảm 100% |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung các điểm dưới đây vào hợp đồng của feature đã làm plan; chữ ký cũ giữ nguyên.

1. **002** ([../002-account-permission/contracts/module-interfaces.md](../002-account-permission/contracts/module-interfaces.md) mục 2): `QuanHeNguoiThanApi` khai báo trong `administration.taikhoan.api` dưới dạng SPI, M10 cài; chữ ký hợp nhất ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.1. **001** ghi chú cùng khai báo.
2. **004**: `BanGhiDonPort.daDung(banGhiDonId, lenhNguon, nguonId)` (MANDATORY) gọi trong lệnh Cho tạm vắng và bàn giao khi Kết thúc lưu trú.
3. **005**: `BanGhiDonBanTruPort.daDung(banGhiDonId, diemDanhId)` (MANDATORY) gọi trong lệnh Điểm danh về; thêm `KetQuaChamSocCongApi` (`ketQua`, `tongHop`) cho cổng và bản tin.
4. **006, 007**: chú thích "ghi nhật ký xem" của `ThuocCongNguoiThanApi`, `SucKhoeCongNguoiThanApi` sửa thành "012 ghi nhật ký lượt xem".
5. **009, 010, 011**: cài `NguoiThanNhanThongBaoPort`, `LuotOLaiAnPort`, phát `SuKienTinhPhiNguon(DEM_O_LAI)` và `LuotOLaiDaDoi` đúng chữ ký đã khai báo. Không đổi.
6. **013, 014, 016, 017, 018** (làm plan sau): dùng `family.api.QuyenNguoiThanApi` để phục vụ phần cổng của mình; **014** cài `HoatDongBanTinPort` và giữ bản ghi "đồng ý về muộn" (FR-046d) ở REST của 014 với người đại diện kiểm qua `QuyenNguoiThanApi.laNguoiDaiDien`.
7. *(Bổ sung 2026-10-18)* **002**: `TaiKhoanApi.theoNguoiCaoTuoi`, phạm vi `BAN_THAN`. **007**: REST `/sos` cho app. **011**: hàm đọc mới `SuatAnDuKienApi.cuaNguoiCaoTuoi(nctId, tu, den)` ở `nutrition.api` (011 FR-063). **013** cài `DoGuiKenhNctPort` (013 FR-030a), **014** cài `HoatDongKenhNctPort` (014 FR-068a), cả hai khai báo ở `family.api`. **009**: nhóm `NguoiCaoTuoi`. **001**, **014**: sự kiện `YKienDaGhiTuPhanHoi` (001 module-interfaces 1.8b) để gắn mục sở thích, thói quen vào phản hồi Mong muốn (FR-058b).

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

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Ánh xạ bảng ở data-model mục 0; task T075. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.

## Ghi chú triển khai

### Phase 1, 2 phần SPI (2026-11-03, A)

- Gói theo glossary mục 18.5: `family.internal.relationship` (quan hệ, bản cài SPI), `family.internal.right` (quyền có tác dụng). T001 không cần `package-info` mới (gói gốc đã có `@ApplicationModule`); `M10ArchTest` kiểm M10 không dùng `billing` ngoài sự kiện (Q-306) và phần trả cho người thân không dùng `auditlog`.
- T004: chỉ hai bảng riêng `family_member`, `family_relationship` (V012_01). `nguoi_than_nct` dùng `resident_module_state` (loại `M10.FAMILY_RESIDENT_STATE`, ghi khi cài lệnh US1); `lich_su_quyen` dùng `status_history` loại `M10.FAMILY_RELATIONSHIP`, thuộc tính là sáu quyền, `REPRESENTATIVE`, `PRIMARY_CONTACT`, mỗi lần đổi một dòng `POINT`; `su_kien_m10_da_xu_ly`, `lan_chay_m10` dùng `reminder_marker` khi có listener, job (T007). Lệch kỹ thuật: thêm cột `started_at` vào quan hệ để xét "Hiệu lực tại thời điểm T" (FR-068, `NguoiDaiDienPort` theo ngày); data-model mục 2 chưa có cột này.
- Xét tại thời điểm: quan hệ Hiệu lực lúc đó (`started_at ≤ T < ended_at`) và giá trị ở lịch sử; không có dòng lịch sử trước T thì tắt (FR-008). Lệnh của US1, US3 phải ghi lịch sử cùng lúc với cột.
- `common.notification.FamilyRight` mở rộng thành sáu quyền (thêm `PICKUP_ALLOWED`, `REQUEST_SERVICES`, FR-007); 009 vẫn chỉ dùng bốn quyền đầu.
- T006 đã cài: `FamilyRelationshipApi` (gồm `familyMemberName`, `relationship`, `rightEnabled`), chú thích ², ⁷ (`FootnoteCondition`), `RepresentativePort`, `FamilyNotificationPort`, `EmergencyFamilyPort` (`visitsInRange` trả rỗng tới US4, T024). Chưa có: `QuyenNguoiThanApi` (T003), `ThamGiaChuyenTrangThai` (T012), `PickupRecordPort`, `DayCarePickupPort` (US2, T020), `TimetableSourcePort` (US4).
- Chú thích ², ⁷ đọc qua `FamilyRelationshipApi` (test dùng bản giả chung). Lớp vai trò của 002 chạy trước lớp phạm vi, nên người thân không có quan hệ với người cao tuổi của đối tượng được cho qua ở điều kiện này để lớp phạm vi trả 404 (002 FR-042).
- `RepresentativeRequestIT` (004, B) bỏ bản thử chú thích "2" tự đăng ký (trùng mã với bản cài thật), đánh dấu người đại diện trong `FakeFamilyRelationships`; không đổi assert. B review.
- Điểm báo lại: FR-003 áp DBR-02 cho Đang lưu trú, Tạm vắng, Điều trị tại bệnh viện; trạng thái Hoạt động bên ngoài (`OFF_SITE_ACTIVITY`) không có trong danh sách, `Dbr02Check` làm theo đúng FR-003.

### Phase 2 phần còn lại (2026-11-04, A)

- T003: `family.api.FamilyRightsApi` (contracts mục 1.2), hai SPI kênh người cao tuổi `ResidentChannelActivityPort` (014 cài), `ResidentChannelBelongingPort` (013 cài) (mục 1.4), sự kiện `RelationshipChanged`, `FamilyRightChanged` (mục 1.5; lệnh US1, US3 phát). Tên ở glossary mục 18.6. Bản giả `support/fake/family/FakeFamilyRights` cho 013, 014, 017, 018.
- Lệch kỹ thuật của T003 (đơn giản trước): chưa khai `NguoiThanThongKeApi` và sự kiện `LuotThamDaVao`, `LuotThamDaRa`, `PhanHoiQuaHan` vì người dùng duy nhất là 016 và 016 không dùng ở giai đoạn này (016 contracts/module-interfaces.md, Q-194); khai cùng lệnh lượt thăm (US4), phản hồi (US6) khi 016 cần. Sự kiện thông báo (`HasNotificationRequests`, đối tượng thông tin theo F-15) khai cùng lệnh phát chúng, vì nội dung, người nhận theo từng lệnh. `nutrition.api.LuotOLaiDaDoi`, `billing.api.SuKienTinhPhiNguon` do 011, 010 khai báo, 012 phát ở US8.
- T006 xong: `FamilyRightsLookup` cài `FamilyRightsApi`. Quyền xem sức khỏe có tác dụng khi bản đồng ý Hiệu lực lúc gọi bao gồm người thân (`ConsentApi.includes`; 001 chưa có tra theo thời điểm, mọi lần gọi là lúc xem); danh sách đón tại thời điểm đọc lịch sử quyền `PICKUP_ALLOWED`. `ThamGiaChuyenTrangThai`, `PickupRecordPort`, `DayCarePickupPort`, `TimetableSourcePort` thuộc T012, T020, US4.
- T007: `M10Jobs` chạy tác vụ `M10TimedTask` theo phút, theo giờ, nửa đêm 00:00; dấu `M10.JOB_RUN` ở `reminder_marker` thay `lan_chay_m10` (R-15), lần chạy ghi `jobs.log`. Theo phút, theo giờ chỉ chạy mốc hiện tại vì tác vụ xử lý mọi việc đã đến hạn; nửa đêm chạy bù từng đêm bị lỡ theo thứ tự. Chưa có tác vụ nghiệp vụ; ba job đã thêm vào `SchedulerScopeArchTest`.
- T008 cài phần của hai bảng đã có: `FamilyMemberFieldClassification` (loại giấy tờ, số giấy tờ là `IDENTITY`), `FamilyRelationshipRetention` (theo hồ sơ; xóa hồ sơ người thân khi không còn quan hệ). Provider của đối tượng chưa có bảng cài cùng task tạo bảng: `GanPhamViProvider` `M10.YEU_CAU_QUYEN` (T015), `M10.MONG_MUON` và phân loại số giấy tờ của mong muốn (T016a), `M10.NGOAI_LE_DON` (US2), `M10.LUOT_THAM` (US4), `M10.PHAN_HOI` và tệp `M10.PHAN_HOI` (US6), `M10.LUOT_O_LAI` (US8); tệp `M10.BANG_CHUNG`, `M10.PHIEU_DANG_KY` (T010, T011).

### Phase 3, US1 (2026-11-04, A)

- T010: không có `V012_02`. Theo R-15 (data-model mục 0, 0.1 đã có từ 2026-10-30) phiếu đăng ký là một dòng `record_event` loại `M10.FAMILY_REGISTRATION_FORM` gắn `M01.RESIDENT`, dòng phiếu ở `lines` (JSON); ghi nháp, tạo yêu cầu của từng dòng rồi xác nhận trong cùng lệnh (nhóm 3). Dòng gắn hồ sơ người cao tuổi nên job loại bỏ xóa cùng hồ sơ, không cần `LuuTruProvider` riêng. Tệp: bằng chứng đặt người đại diện chủ sở hữu `M10.REPRESENTATIVE_EVIDENCE` (quan hệ), bản scan chủ sở hữu `M10.FAMILY_REGISTRATION_FORM` (phiếu), chỉ Hành chính, Quản lý viện tải (`StaffOnlyFilePermission`).
- Lệch kỹ thuật của T010, T011: FR-008 cần mỗi quyền trên phiếu là một yêu cầu Hiệu lực, nên US1 đã tạo `simple_record` loại `M10.RIGHT_CHANGE_REQUEST` trạng thái `EFFECTIVE` (cách `SIGNED_FORM`, nguồn `REGISTRATION_FORM`, khóa dòng ngay). Lập, xác nhận, duyệt, từ chối, hủy, `khoa_cho` và thông báo "có kết quả" (FR-069) thêm ở T015, T016; kiểm trái mong muốn trên phiếu (FR-020b) ở T016b.
- T011 `FamilyMemberService`: khóa `FamilyResidentLock` (upsert rồi `FOR UPDATE`, mẫu `CareLockStore` của 005), ghi cột và `status_history` cùng lệnh, phát `RelationshipChanged`, `FamilyRightChanged`, nhật ký (FR-068). DBR-02: lệnh bị từ chối khi làm phát sinh vi phạm mới (so trước và sau lệnh); vi phạm có sẵn (dữ liệu cũ) không chặn lệnh khác. ≤ 1 liên hệ chính kiểm trước để trả `DBR_02_VIOLATED` thay vì lỗi khóa duy nhất. Kết thúc quan hệ gọi `AccountApi.recheckFamilyMember` (căn cứ `FR-006`, bản ghi nguồn là quan hệ) và bỏ quyền được phép đón; hủy lượt thăm tương lai, yêu cầu chờ của người đó thêm cùng US4, US3. Q-249 ở US1 chỉ có ở phiếu đăng ký: người nhận là người đại diện Hiệu lực khác người ký (người lập là Hành chính).
- Mã lỗi không thêm mới: lập quan hệ trùng cặp Hiệu lực, đổi liên hệ chính cho người đang là liên hệ chính, lệnh trên quan hệ đã kết thúc trả 409 `STATE_CHANGED`; người ký phiếu không phải người đại diện trả 409 `REQUEST_NOT_ALLOWED`; thiếu bằng chứng, thứ tự, bản scan trả 422.
- Lệnh hồ sơ, quan hệ chỉ cho tài khoản nhân viên: ma trận cho AC-10 Tạo, Sửa ở `FAMILY_MEMBER_PERMISSION` (phạm vi quan hệ, chú thích ²) để lập yêu cầu qua cổng (US3), nên service chặn thêm tài khoản người thân (403). `GET /residents/{residentId}/relationships` với người thân chỉ trả quan hệ của chính họ (FR-067 "X (của mình)").
- REST (T013): hợp đồng không khai nội dung phản hồi 201 và lệnh; controller vẫn trả `{id}` (không có trong OpenAPI, `content = @Content`) để màn web không phải đoán. `GET /residents/{residentId}/pickup-list` thuộc T017, còn ở pending của `ApiContractIT`.
- T012 `M10StatusParticipant` (`@Order(70)`, sau 003 ở 20): hiện chỉ điều kiện `DBR_02` cho `COMPLETE_ADMISSION`; tác động vắng, trạng thái cuối thêm khi có lượt thăm, lượt ở lại, yêu cầu, ngoại lệ đón. Test nền của 001, 004 Hoàn tất tiếp nhận không có người thân nên thay bean này như bean của 003: `M02TestBase`, `AdmissionRealBedIT` dùng `@MockitoBean(name = "m10StatusParticipant")`, `StatusTestBase` gỡ bean cùng `m02StatusParticipant`; không đổi assert. B review.
- Màn web: `features/relatives/FamilyMembersPage.jsx` (mục menu "Người thân và quyền", dòng Phụ lục 27 "Người thân và quyền", Hành chính, Quản lý viện): chọn người cao tuổi, bảng quan hệ, khung chi tiết có lệnh kèm lý do, Sheet thêm người thân và ghi phiếu đăng ký. Bố cục chưa qua bước duyệt của DESIGN.md mục 13 điều 2.

### Phase 4, US3 (2026-11-05, A)

- T015, T016a: không có `V012_03`, `V012_03a`. Theo R-15 (data-model mục 0, 0.1) yêu cầu là `simple_record` loại `M10.RIGHT_CHANGE_REQUEST`, dấu "được tự về" loại `M10.SELF_RETURN_FLAG` (một dòng mỗi người cao tuổi), mong muốn loại `M10.VISIT_PICKUP_WISH`. `khoa_cho` (FR-018) và `khoa_hieu_luc` (FR-020a) là `unique_key` của dòng, chỉ đặt khi Chờ xác nhận, Hiệu lực; mong muốn Được thay thế khóa dòng (`locked`) thay trigger bất biến. Hai cột `trai_mong_muon_id`, `trao_doi_voi_nct` nằm trong nội dung JSON; `CHECK` của data-model mục 3 kiểm ở service (Quản lý viện duyệt yêu cầu trái mong muốn thiếu nội dung đã trao đổi → 422). Không có cột tiền, không có bảng con 2 → 3.
- T016: `RightChangeRequestService` khóa `FamilyResidentLock` rồi khóa dòng yêu cầu (`lockForUpdate`), nên hai người đại diện xác nhận cùng lúc chỉ một người thành công, người sau nhận 409 `STATE_CHANGED` (FR-016). Tắt "được phép đón", tắt "được tự về" áp ngay khi lập; Từ chối, Hủy khôi phục từ lúc có kết quả. "Áp ngay khi lập" chỉ ghi khi giá trị thật sự đổi, nên yêu cầu bỏ M khi yêu cầu bật M còn chờ (Q12) thay yêu cầu bật mà Từ chối sau đó không bật M. Kết thúc quan hệ hủy yêu cầu chờ của quan hệ đó; `M10StatusParticipant` hủy mọi yêu cầu chờ khi chuyển trạng thái cuối (FR-075). Nhắc CFG-M10-11: `RightChangeReminders` trong việc hằng giờ, không tự Từ chối (Q-307). Bỏ quyền nhận thông báo khẩn phát `FamilyRightChanged` (`RECEIVE_URGENT`, `false`) cho 009.
- Quyền: lệnh lập, xác nhận, từ chối, hủy tự kiểm quyền (người thân theo FR-014, FR-015; nhân viên qua `PermissionApi`), đã khai trong `RequiresPermissionArchTest.SELF_CHECKED`; duyệt dùng `@RequiresPermission(FAMILY_MEMBER_PERMISSION, APPROVE)`.
- T016b: người đại diện theo pháp luật đọc qua `ConsentApi.legalRepresentative` (bản giả chung `FakeConsentApi.legalRepresentative`, thêm hàm cho test; B review). Dòng phiếu đăng ký trái mong muốn thành yêu cầu Chờ xác nhận chờ Quản lý viện, dòng khác vẫn Hiệu lực.
- Điểm báo lại: research F-06a ghi `GET /nguoi-cao-tuoi/{nctId}/quan-he` trả `quyenTraiMongMuon`, nhưng api-012 `Relationship` không có trường này; theo hợp đồng, cảnh báo chỉ có ở `GET /residents/{residentId}/visit-pickup-wishes` (`conflictingRights`). Bản ghi đón chưa dùng chuyển Không dùng khi tắt đón (FR-017 câu cuối) thêm cùng US2.
- T017 web: khối "Yêu cầu thay đổi quyền" và "Mong muốn của người cao tuổi" trong `FamilyMembersPage` (Hành chính, Quản lý viện). Hợp đồng `RightChangeRequest` không có tên người thân, người lập, thời điểm; màn lấy tên từ danh sách quan hệ. Mục menu "Người thân" (`relatives`, Trưởng tầng, Điều dưỡng ghi mong muốn) vẫn là bản mẫu `RelativesPage`; Trưởng tầng, Điều dưỡng không có quyền xem quan hệ nên cần màn mong muốn riêng. Bố cục chưa qua bước duyệt của DESIGN.md mục 13 điều 2.
- T017 mobile: tab "Yêu cầu" của `FamilyRoot` (thanh điều hướng dưới có từ màn thứ hai): người đại diện xác nhận, từ chối (lý do) yêu cầu Chờ xác nhận; yêu cầu trái mong muốn không có nút Xác nhận. Chỉ trực tuyến. Thẻ ghi "người thân" vì hợp đồng không trả tên người bị tác động.

### Phase 5, US2 (2026-11-05, A)

- T019: `V012_04__pickup_record.sql` chỉ có bảng giữ `pickup_record` (nhóm 3, `eldercare_app` chỉ có `INSERT`; `CHECK` theo loại, căn cứ; `UNIQUE (exception_id)` để một ngoại lệ chỉ dùng cho một bản ghi). Theo R-15: sự kiện kết quả (`ban_ghi_don_su_kien`) là `record_event` loại `M10.PICKUP_RECORD_EVENT` gắn `M10.PICKUP_RECORD`; lần chặn (`lan_chan_don`) là `record_event` loại `M10.PICKUP_BLOCK` gắn hồ sơ; ngoại lệ (`yeu_cau_ngoai_le_don`) là `simple_record` loại `M10.PICKUP_EXCEPTION`. Người đón, người cao tuổi là tham chiếu logic: người đón theo ngoại lệ không có hồ sơ người thân (họ tên, số giấy tờ lưu trên bản ghi).
- Lệch kỹ thuật (đơn giản trước): `khoa_da_dung` thành một khóa kết quả `M10.PICKUP_DONE:<id>` cho cả Đã dùng và Không dùng, nên mỗi bản ghi có tối đa một kết quả và lệnh nguồn không dùng được bản ghi đã Không dùng mà không cần khóa thêm. `markUsed` không khóa người cao tuổi của M10 (lệnh nguồn của 004, 005 giữ khóa của module đó; Viện đưa về khóa M10 rồi gọi 005, khóa ngược sẽ tắc).
- T020: `PickupService` kiểm quyền Tạo của lệnh nguồn theo loại đón (`ABSENCE_RETURN`, `DAY_CARE_ATTENDANCE`, `RESIDENCY_END_DEATH`; thiếu ở lớp vai trò → 403 `SOURCE_COMMAND_PERMISSION_DENIED`) rồi (`PICKUP`, `CREATE`); giao của hai ô đúng FR-022 (Trưởng tầng không có Điểm danh về; Điều dưỡng không có Cho tạm vắng; Bác sĩ có Kết thúc lưu trú nhưng không có Quy trình đón). Ghi đón chạy trong transaction riêng (`TransactionTemplate`): không đạt thì lần chặn, ngoại lệ Hết hạn vẫn ghi rồi mới trả 409 `PICKUP_BLOCKED` (`PICKUP_EXCEPTION_EXPIRED` khi ngoại lệ hết hạn) kèm `blockReason`. Lệnh khóa `FamilyResidentLock` như lệnh đổi quyền. Viện đưa về gọi `DayCareAttendanceApi.checkOut(…, INSTITUTE_RETURN, …)` trong cùng transaction; 005 gọi lại `DayCarePickupPort.valid`, `markUsed`. Loại đón của 005: `DAY_CARE_DEPARTURE` → `PICKUP` (glossary QUA_DON).
- FR-017 câu cuối: `PickupPorts` nghe `FamilyRightChanged` (`PICKUP_ALLOWED`, `false`; lệnh yêu cầu, kết thúc quan hệ) và `SelfReturnTurnedOff` (sự kiện trong M10, phát khi tắt dấu tự về) trong transaction của lệnh, ghi Không dùng cho bản ghi chưa có kết quả. Việc theo phút `PICKUP_RECORD_EXPIRY` (FR-027), `PICKUP_EXCEPTION_EXPIRY` (FR-024).
- Thông báo: `PickupBlockNotice` (Nhẹ, Trưởng tầng của tầng qua `ResidentLocationApi.floorOn`; không biết tầng thì không có người nhận), `PickupExceptionNotice` (Trung bình, người đại diện và Quản lý viện). Đính chính `M10.PICKUP_RECORD` chỉ thời điểm bàn giao, nhân viên bàn giao; người đón, căn cứ, giấy tờ sai thì ghi lượt mới.
- Điểm báo lại: (1) 004 `AbsenceTransition` tìm bản ghi theo `pickedUpById` (người thân); bản ghi theo ngoại lệ không có hồ sơ người thân nên Cho tạm vắng với người đón ngoại lệ chưa dùng được bản ghi: cần B quyết `pickedUpById` cho trường hợp này. (2) 004 Kết thúc lưu trú (`ResidencyEndingService`) chưa gọi `PickupRecordPort` cho bàn giao (FR-064 (e)). (3) `GET /pickup-records` (hợp đồng ghi "bản ghi đón, lần chặn đón") trả mảng `PickupRecord`, schema không có trường lần chặn; hiện chỉ trả bản ghi đón.
- T021 web, app: mục menu `pickups` "Quy trình đón" (chức năng `PICKUP`; Quản lý viện, Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc, Hành chính theo Phụ lục 27), màn `frontend/src/features/relatives/pickups/` (chọn người cao tuổi dùng lại `ResidentPicker` của màn Người thân và quyền; người được phép đón, mong muốn về người đón kèm nhãn người nói khi backend cho đọc; bảng bản ghi đón; ghi đón, lập ngoại lệ bằng `Sheet`; lệnh Duyệt, Từ chối, Hủy ngoại lệ theo mã). Nhân viên bàn giao chọn từ `GET /staff?status=WORKING` (api-008). App: `mobile/lib/staff/pickups/pickup_screen.dart`, chỉ trực tuyến, không vào hàng đợi ngoại tuyến; gửi lại cùng nội dung dùng cùng khóa idempotency. Khung cổng người thân thêm link "Bỏ qua menu" và bọc `SessionGuard` khi có phiên thật (team-plan mục 8, DESIGN.md 6.8.2).
- Điểm báo lại của T021: (4) api-012 không có lệnh đọc yêu cầu ngoại lệ đón, nên Quản lý viện, người lập nhập mã ngoại lệ (mã trả về khi lập, có trong thông báo `PickupExceptionNotice`); người đại diện xác nhận qua cổng chưa có màn vì cổng chưa có danh sách ngoại lệ chờ mình. Thêm `GET /residents/{residentId}/pickup-exceptions` là đổi hợp đồng, cần chủ dự án duyệt. (5) Thanh dưới phần nhân viên của app đã đủ 5 mục, chưa có mục "Thêm" (DESIGN.md 15.4), nên `PickupScreen` chưa gắn vào `StaffRoot` (như các màn 005 của C).

### Phase 6, US4 (2026-11-05, A)

- T023: không có migration. Theo R-15: `luot_tham` là `simple_record` loại `M10.VISIT` (ngày thăm ở `effective_from`, `effective_to`; người đi cùng `luot_tham_di_cung` là mảng JSON trong nội dung); `khung_tham_ngay` là `technical_lock` khóa `M10.VISIT_SLOT:<ngày>:<HH:mm-HH:mm>`, số người đã giữ đếm khi đã khóa (lượt Đã duyệt, Đã vào). Trạng thái kết thúc khóa dòng.
- T024: `VisitRegistrationCheck` (hàm thuần, trả mọi lý do theo mã glossary) → 422 với `errorCode` là lý do đầu, `unmetConditions` đủ các lý do. Lệch kỹ thuật: đăng ký chạy trong transaction riêng mở đầu bằng khóa khung (như `PickupService.record`), vì MySQL REPEATABLE READ lập ảnh chụp ở lần đọc đầu; đọc quyền trước khi có khóa thì không thấy lượt vừa giữ chỗ (SC-004 lần chạy đầu cho 5 lượt cùng thành công). `GET /visit-slots` trả `{startTime, endTime, capacity, seatsLeft}` (hợp đồng không khai nội dung); người thân xét quyền Xem ở lớp vai trò vì khung không gắn người cao tuổi. Hủy của hệ thống không ghi nhật ký thao tác (không có phiên), dòng ghi người hủy, mã lý do. Listener `IsolationCreated` nhận tham số `Object` vì chỉ 009 được nghe kiểu mang yêu cầu thông báo (`M13ArchTest`).
- (d) theo BR-M10-02: chặn Tạm vắng, Điều trị tại bệnh viện, chưa vào ở, trạng thái cuối; Hoạt động bên ngoài vẫn nhận (spec FR-030 (d) ghi "đang Đang lưu trú", nghiep-vu chỉ nêu hai trạng thái chặn; theo nghiep-vu, A xác nhận 2026-11-05). Bán trú: lịch đến của 004 (`ContractReadApi.attendanceOn`), khung nằm trọn giờ có mặt dự kiến, không báo vắng, ngày nghỉ (005).
- Điểm báo lại: (1) 007 chưa có bản cài `IsolationApi` (chỉ bản giả `FakeIsolation` trong test); 012 dùng `ObjectProvider`, chưa có bản cài thì (c) coi như không khoanh vùng. (2) Khu tiếp khách chưa có mã khu cấu hình ở 003 nên chưa chặn "khu tiếp khách nằm trong vùng". (3) 004 chưa có API lượt vắng dự kiến, nên (d) chưa xét lượt vắng dự kiến trùng ngày. (4) App: `VisitRegistrationScreen` cần mã người cao tuổi; cổng chưa có `GET /family-portal/residents` (US5, T028) nên chưa gắn vào `FamilyRoot`. Màn Thăm của cổng web còn bản mẫu tới task web US5.

### Phase 7, US5 (2026-11-05, A)

- T027: không có migration. Theo R-15 `nhat_ky_xem_suc_khoe` là hành động nhật ký `HEALTH_SECTION_VIEWED` (đối tượng `M10.HEALTH_VIEW`, mã đối tượng là người cao tuổi; `details`: người thân, phần, nguồn `PORTAL`); `GET /health-view-logs` đọc qua `AuditLogApi.recordedBetween`, chỉ Quản lý viện. Chỉ ghi khi phần có dữ liệu. Phần sức khỏe kiểm quyền bật (`NO_HEALTH_VIEW_RIGHT`) rồi bản đồng ý (`RIGHT_NOT_EFFECTIVE`). `health()` không bọc transaction: 005 ném lỗi 503 trong transaction của nó (công thức ăn uống chưa chốt) làm transaction ngoài bị đánh dấu rollback; nhật ký ghi bằng transaction riêng.
- Hình dạng phản hồi 200 (hợp đồng không khai): `/family-portal/residents` `{residentId, fullName, relationship, representative, primaryContact, rights{6 quyền có tác dụng}, canViewBelongings}`; `/general` `{residentId, fullName, status, location, visits, absences, dayCareSchedule, incidents, unavailable}`; `/health/{section}` `{section, available, note, items}`; `/contract` `{contracts}`; `/health-view-logs` `[{familyMemberId, residentId, section, source, at}]`.
- Nguồn: dị ứng, bệnh nền (001 `HealthItemApi`), thuốc (006 `FamilyPortalMedicationApi`), kết quả chăm sóc (005) có bản cài. Chưa có nguồn, trả `available: false` kèm câu: chỉ số, sự cố (007 `FamilyPortalHealthApi` chưa có bản cài), cân nặng (chưa có mã chỉ số cân nặng), ăn uống (005 chờ công thức, team-plan mục 8), chế độ ăn riêng, thực đơn chung (011), lịch khám bên ngoài (004 chưa có API lượt vắng loại khám). Hợp đồng: danh sách hợp đồng của 004; phụ lục, đặt cọc, tiến trình kết thúc lưu trú chưa có API đọc cho cổng.
- Web: có phiên thật thì Hôm nay, Sức khỏe, Hợp đồng dùng API; các màn cổng khác còn bản mẫu tới task web của phần đó. Người thân có nhiều người cao tuổi: cổng dùng người đầu tiên (chọn người chưa thiết kế, DESIGN.md 14). App: tab "Người thân" đứng đầu `FamilyRoot`, mở đăng ký thăm (gắn màn Phase 6).
- Ghi chú quy trình: `LivePortal.test.jsx` viết trước mã nhưng không chạy riêng trước khi cài (lần chạy đầu sau khi cài đã xanh); test backend và app chạy đỏ trước.

### Phase 8, US6 (2026-11-05, A)

- T030: `V012_07__feedback.sql` chỉ có bảng giữ `feedback` (nhóm 2, `DataGroup=BUSINESS`; cột tiếng Anh theo glossary 18.9) với cột của US6; cột người gửi người cao tuổi, ghi thay, chủ đề Mong muốn và giá trị `WISH`, `RESIDENT_CHANNEL`, `RECORDED_DIRECTLY` thêm ở US9 (T065) như tasks ghi. Theo R-15 `phan_hoi_muc` là `record_event` loại `M10.FEEDBACK_ITEM` gắn `M10.FEEDBACK` (`FeedbackItem`: `CONTENT`, `REPLY`, `HANDLING_DIRECTION`, `NOTE`, `REASSIGNMENT`, `PRIORITY_CHANGE`, `REOPEN`; cờ `healthInformation`), chỉ thêm, không sửa (FR-065). Provider: phạm vi `M10.FEEDBACK` theo người cao tuổi, tệp đính kèm (chủ sở hữu `M10.FEEDBACK`, tải được khi xem được phản hồi), lưu giữ theo hồ sơ (`FeedbackRetention`).
- Người phụ trách (FR-059): Trưởng tầng được giao của tầng người cao tuổi lúc gửi (`ResidentLocationApi.at` rồi `ScopeApi.assignedFloorManager`), tầng không có thì nhóm Quản lý viện; chi phí, hợp đồng, khác là nhóm Hành chính. Nhóm lưu `assignee_role`, `assignee_account_id` trống; người trong nhóm Nhận xử lý thì thành người phụ trách. Giao lại (FR-060) cho tài khoản nhân viên đang hoạt động có vai trò Trưởng tầng, Điều dưỡng hoặc Hành chính và có (`FEEDBACK`, `UPDATE`) trong phạm vi của người cao tuổi, lấy từ `ApproverApi.approvers` (sửa 2026-11-05 cùng đổi api; trước đó chỉ kiểm vai trò, phạm vi kiểm khi người được giao ra lệnh).
- Quyền: lệnh của người phụ trách cần (`FEEDBACK`, `UPDATE`) trong phạm vi, Quản lý viện qua (`FEEDBACK`, `APPROVE`) (Phụ lục 27 ô D); Đồng ý đóng, Mở lại chỉ người thân gửi; Hành chính ghi thay người thân tại quầy (nguồn `COUNTER`), vai trò nhân viên khác bị 403 ở US6 (Điều dưỡng ghi thay người cao tuổi thuộc US9). Các lệnh tự kiểm quyền, đã thêm vào `RequiresPermissionArchTest.SELF_CHECKED`.
- Hạn, leo thang (FR-061 → FR-064): `FeedbackDeadline` (hàm thuần); hạn đọc CFG-M10-01 lúc tính; `escalated_due_at` nhớ hạn đã leo thang nên mỗi hạn một lần; đổi mức mà hạn mới đã qua thì leo thang ngay trong lệnh. Việc theo phút `FEEDBACK_ESCALATION`, `FEEDBACK_AUTO_CLOSE` (mỗi phản hồi một transaction, khóa dòng rồi kiểm lại). Thông báo `family.api.events.FeedbackNotice` (`FEEDBACK_ASSIGNED` Trung bình cho người phụ trách hoặc nhóm vai trò; `FEEDBACK_REPLIED` Nhẹ cho người gửi, nhóm sức khỏe có phần `HEALTH` và phần chung; `FEEDBACK_ESCALATED` Trung bình cho Quản lý viện).
- Đọc FR-058 với kịch bản 8 (lệch câu chữ giữa FR-058 "trả lời ... MUST được xếp loại sức khỏe" và kịch bản 8 "nội dung trả lời gửi N chỉ ở loại chung"): trả lời, hướng xử lý của nhóm "sức khỏe, thuốc" luôn xếp sức khỏe; người gửi không có quyền xem sức khỏe có tác dụng lúc xem thì không nhận các mục đó, `healthReplyHidden` báo màn hiện dòng khóa (DESIGN.md 2.2) kèm câu mời liên hệ viện; người phụ trách thấy `senderHealthRightEffective = false` để được cảnh báo lúc soạn. Người gửi chỉ thấy mục nội dung, trả lời, hướng xử lý, mở lại; ghi chú, giao lại, đổi mức là nội bộ.
- REST: hợp đồng không khai nội dung của `POST /feedback` (trả `{id}`). `GET /feedback/{id}` và 200 của lệnh trả `FeedbackDetail` (khai trong hợp đồng từ đổi api 2026-11-05, xem dưới). DTO khai đủ trường của hợp đồng gồm phần US9; `senderType = RESIDENT`, loại `WISH`, lệnh `change-type`, `link-opinion` trả 422 tới US9. Bỏ bốn dòng pending `/feedback` ở `ApiContractIT`.
- Web: mục menu `feedback` "Phản hồi" (chức năng `FEEDBACK`; Quản lý viện, Trưởng tầng, Điều dưỡng, Hành chính), màn `frontend/src/features/relatives/feedback/` (bảng, lọc trạng thái và Quá hạn, `DetailLayout`; Nhận xử lý, Trả lời có cảnh báo quyền sức khỏe, Đổi mức, Giao lại, Ghi chú; lý do bắt buộc). Cổng: `features/family-portal/live/LiveFeedbackPage.jsx` thay bản mẫu khi có phiên thật. App người thân: tab "Phản hồi" của `FamilyRoot` (`mobile/lib/family/feedback/feedback_screen.dart`), chỉ trực tuyến. Bố cục duyệt 2026-11-05 (DESIGN.md 6.8.3, mục 14): quá hạn lên đầu rồi theo hạn, nhãn giữ trạng thái kèm "quá hạn", cổng và app cùng tiêu đề "Gửi phản hồi về <họ tên>"; hoãn dòng liền trên app và ẩn cột Loại. Đã xem trên trình duyệt với backend demo (2026-11-05): tài khoản chưa có hồ sơ nhân viên thì `AssignableStaff.fullName` là `null` (web hiện "Tài khoản <mã>"), `assignee` hiện tên vai trò của nhóm thay cho chuỗi rỗng.
- Đổi api (2026-11-05, chủ dự án duyệt; giải quyết ba điểm báo lại trước đây: danh sách người nhận giao lại, tên người gửi và thời điểm gửi, lệnh được phép): `Feedback` thêm `familyMemberName`, `submittedAt` (chỉ đọc); schema mới `FeedbackItem`, `FeedbackDetail` (`allOf` `Feedback` và phần chi tiết: `source`, `assigneeId`, `repliedAt`, `closedAt`, `closeMethod`, `items`, `senderHealthRightEffective`, `healthReplyHidden`, `allowedCommands`); endpoint mới `GET /feedback/{id}/assignable-staff` (`AssignableStaff`: `accountId`, `fullName`, `role`). `allowedCommands` tính theo bảng FR-066 cho người xem và trạng thái (người phụ trách: `start-handling`, `reply`, `change-priority`, `add-note`; Trưởng tầng, Quản lý viện: `reassign` khi chưa đóng; người gửi: `agree-to-close`, `reopen` khi Đã phản hồi); backend vẫn kiểm lại ở lệnh. Giữ cách đọc FR-058 ở trên: trả lời nhóm "Sức khỏe, thuốc" luôn xếp loại sức khỏe, người gửi không có quyền xem sức khỏe thấy dòng khóa. Web: nút lệnh theo `allowedCommands`, Giao lại chọn người từ `assignable-staff`, bảng có cột Người gửi kèm thời điểm gửi; cổng và app đọc chi tiết của phản hồi Đã phản hồi để hiện Đồng ý đóng, Mở lại theo `allowedCommands`.
- Sửa lỗi cùng đổi api: `GET /feedback` của nhân viên kiểm (`FEEDBACK`, `VIEW`) với `Target.UNSPECIFIED`, chỉ phạm vi toàn viện đạt nên Trưởng tầng, Điều dưỡng, Hành chính bị 403; nay kiểm lớp vai trò rồi lọc từng phản hồi theo phạm vi (ca `FR_066_detailHasSenderAndAllowedCommands` phát hiện).
- Ghi chú quy trình: `BR_M10_05_FeedbackIT` (12) và `FeedbackDeadlineTest` (5) chạy đỏ trước rồi xanh; `FeedbackPage.test.jsx` (5) chạy đỏ trước; `LiveFeedback.test.jsx` (3) và `feedback_screen_test.dart` (3) viết trước mã nhưng không chạy riêng trước khi cài.

### Phase 10a, US9 – phần backend (2026-11-05, A)

- T065: `V012_10__resident_channel.sql` thêm vào `feedback` cột `sender_type`, `recorded_on_behalf_by`, `wish_topic`, giá trị `WISH`, `RESIDENT_CHANNEL`, `RECORDED_DIRECTLY`, `RECORDED_ON_BEHALF` và bốn `CHECK` theo data-model mục 9 (người thân ⇔ có `family_member_id`; Mong muốn ⇔ có chủ đề và ⇒ người cao tuổi; ghi trực tiếp ⇔ có người ghi thay; người cao tuổi ⇔ nguồn kênh hoặc ghi trực tiếp). Theo R-15 `phan_hoi_y_kien` là `record_event` loại `M10.FEEDBACK_OPINION` (khóa `unique_key`), `DOI_LOAI` là mục `TYPE_CHANGE`; không có bảng mới.
- T066: `FeedbackService` nhận người gửi người cao tuổi (kênh của mình) và ghi thay của Hành chính, Điều dưỡng (`POST /feedback` với `senderType = RESIDENT`, có hay không có tài khoản; Trưởng tầng và vai trò khác 403). Loại Mong muốn: chủ đề bắt buộc, nhóm đặt theo chủ đề (khách thăm, người đón → `OTHER`, Hành chính; khác → `CARE`, Trưởng tầng), mức Thường; Trả lời bị chặn 409 `OPINION_NOT_LINKED` tới khi có mục gắn; `link-opinion` chỉ nhận mong muốn Hiệu lực của đúng người cao tuổi có nguồn người cao tuổi, gắn lại không thêm dòng; nghe `OpinionRecordedFromFeedback` của 014 với cùng điều kiện. `change-type` (Mong muốn → Góp ý, Hỏi đáp; lý do) giữ mức, hạn, bỏ chủ đề. Truy vấn của người thân lọc `sender_type = FAMILY_MEMBER` (FR-086); người cao tuổi chỉ thấy phản hồi của mình. Trả lời nhóm sức khỏe trên kênh chỉ phần chung (`healthReplyHidden`), nhân viên thấy `senderHealthRightEffective = false` để được nhắc trả lời trực tiếp; báo đã trả lời gửi `RecipientGroup.Resident`, chỉ phần chung.
- Cách đọc FR-058a (c): nhân viên ghi thay Đồng ý đóng, Mở lại chỉ khi người cao tuổi chưa có tài khoản Hoạt động (`AccountApi.byResident`); người có tài khoản tự ra lệnh. Người cao tuổi xem chi tiết và ra lệnh qua `GET /feedback/{id}`, `POST /feedback/{id}/{command}` (quyền `FEEDBACK` phạm vi SELF có sẵn ở V002_08); `/resident-channel/feedback` chỉ có danh sách và gửi như hợp đồng.
- T067, T068: hai port `ResidentChannelActivityPort`, `ResidentChannelBelongingPort` đã khai ở Phase 2; bản giả `FakeResidentChannelActivities`, `FakeResidentChannelBelongings`. `ResidentChannelService` dựng năm phần với DTO riêng (hình dạng 200 do hợp đồng không khai: phần là `{available, note, items}`, hôm nay là `{date, visits, activities, dayCareSchedule, unavailable}`); port chưa có bản cài thì phần báo "Chưa có dữ liệu". `ResidentChannelArchTest` chặn kênh gọi API sức khỏe, thuốc, kết quả chăm sóc, sự cố, chi phí.
- Điểm báo lại: (1) 011 chưa khai báo `SuatAnDuKienApi`, `ThucDonChungApi` (gói `nutrition` trống) nên phần thực đơn luôn "Chưa có dữ liệu" và chưa có bản giả của 011 (T067 phần này chờ 011; bản giả do người khai báo interface viết). (2) Nhắc lượt thăm chạy ở việc nửa đêm của M10 (cron 00:00) thay cho 00:05 của research F-18; 009 vẫn giữ tới hết giờ yên tĩnh nên không khác với người nhận.
- T069: `ResidentVisitReminders` (việc nửa đêm `VISIT_RESIDENT_REMINDER`, lượt lập cho chính hôm nay nhắc ngay) phát `ResidentVisitReminder` mức Nhẹ, phần chung cho người cao tuổi có tài khoản Hoạt động.
- Ghi chú quy trình: `FR_083_ResidentChannelIT` (2), `FR_084_ResidentChannelNoLeakIT` (3), `ResidentChannelArchTest` (2), `FR_058a_ResidentFeedbackIT` (7), `BR_M10_12_WishIT` (4), `FR_086_FamilyCannotSeeResidentFeedbackIT` (2) chạy đỏ trước (17/18 đỏ; `FR_084_cannotOpenOtherResident` xanh sẵn vì cổng vốn chỉ cho người thân) rồi xanh. `FR_069_ResidentVisitReminderIT` (3) viết sau khi cài. Web (T072) và app (T070, T071, T074) chờ duyệt bố cục theo DESIGN.md 13 điều 2.

### Phase 10a, US9 – web T072 (2026-11-05, A)

- Màn Phản hồi (`frontend/src/features/relatives/feedback/`): `ResidentFeedbackParts.jsx` có `OnBehalfSheet` (ghi lời người cao tuổi; người cao tuổi chọn trong danh sách Đang lưu trú của `GET /residents`; Mong muốn gửi nhóm theo chủ đề như backend) và `WishOpinions` (gắn mong muốn nguồn người cao tuổi qua `link-opinion`; "Ghi mong muốn mới" dùng lại `RecordWishSheet` của màn Người thân, nay export kèm `defaultSource`); `FeedbackPage` thêm Đổi loại, Ghi đồng ý đóng, Ghi mở lại theo `allowedCommands`, câu người gửi theo nguồn. Bố cục duyệt 2026-11-05 (DESIGN.md 6.8.4).
- Lệch so với bố cục duyệt: dòng người ghi thay là "Nhân viên ghi thay" vì hợp đồng `Feedback` không có họ tên người ghi thay; số mục ý kiến đã gắn không hiện vì `FeedbackDetail` không trả danh sách mục gắn (chưa gắn thì nhân viên biết qua lỗi 409 khi Trả lời). Nút "Ghi lời người cao tuổi" theo quyền Tạo nên Trưởng tầng cũng thấy và nhận 403 khi lưu.
- Ghi chú quy trình: `ResidentFeedback.test.jsx` (7) viết trước, 7/7 đỏ rồi xanh. Chưa xem trên trình duyệt (schema demo chưa có người cao tuổi Đang lưu trú).
- Xem trên trình duyệt 2026-11-05 (backend `demo`; hồ sơ demo id 1 đổi sang Đang lưu trú bằng SQL trên máy vì Hoàn tất tiếp nhận cần hợp đồng, đặt cọc, giường của 003, 004 chưa có dữ liệu demo): Hành chính ghi lời người cao tuổi (Mong muốn về người đến thăm), ghi mong muốn mới nguồn đặt sẵn "Người cao tuổi nói", gắn, nhận xử lý, trả lời, ghi mở lại, trả lời lần hai, ghi đồng ý đóng; Quản lý viện thấy câu nhắc sở thích của Mong muốn việc khác, Đổi loại không lý do bị chặn, có lý do thì đổi, mức và hạn giữ nguyên. Không có lỗi. Còn lại: sau khi gắn, câu nhắc "gắn vào đây trước khi trả lời" vẫn hiện vì `FeedbackDetail` không trả mục gắn (đã ghi ở trên). Ghi đồng ý đóng không có bước xác nhận.

### Phase 10a, US9 – app (2026-11-05, A)

- T070: màn "Hôm nay" (`mobile/lib/resident/today/`) có thẻ lớn một cột mở màn tương ứng, Thông báo đầu tiên, nút SOS ở đáy; `lib/resident/channel/channel_screens.dart` dựng Lịch hôm nay, Bữa ăn, Hoạt động, Người đến thăm, Đồ của tôi từ `GET /resident-channel/*` (phần chưa có dữ liệu hiện câu báo của backend). `lib/resident/feedback/resident_feedback_screen.dart` "Góp ý với viện": danh sách kèm trạng thái và trả lời (nhóm sức khỏe chỉ phần chung), Đồng ý đóng, Hỏi lại theo `allowedCommands`, gửi mới theo bước; loại khác Mong muốn hỏi thêm "Về việc gì?" (năm nhóm, FR-058 bắt buộc nhóm, FR-059), chủ dự án duyệt 2026-11-05 (DESIGN.md 6.8.5); Mong muốn gửi nhóm theo chủ đề như backend.
- T071, T074: `lib/resident/sos/sos_button.dart` `SosButton` (đếm ngược 3 giây, Hủy, Gọi ngay; trạng thái đang đến, đã tới, hỏi lại 5 giây; sáu nút nơi đang ở; gửi vị trí theo chu kỳ khi được cấp quyền lúc đang dùng, dừng theo `guiViTri` hoặc 409; không gửi được thì hiện số của viện đã lưu và 115, không xếp hàng gửi sau), `SosIntroGate` giới thiệu một lần. Nút hiện ở Hôm nay, ở màn khóa sinh trắc và ở màn quét mã khi máy đã gắn người cao tuổi nhưng chưa có phiên (FR-085).
- Điểm báo lại: (1) ảnh đồ gửi chưa hiện vì chưa có đường tải tệp cho tài khoản người cao tuổi; (2) `/sos` của 007 chưa cài, app chỉ kiểm bằng backend giả trong test widget; (3) nhắc lượt thăm, thực đơn chờ 011 (xem phần backend).
- Ghi chú quy trình: test mới `today_screen_test.dart` (2), `resident_feedback_screen_test.dart` (3), `sos_button_test.dart` (9); hai ca FR-085 của màn quét mã trong `qr_login_test.dart` viết trước, đỏ rồi xanh. Tám test cũ (`app_test.dart`, `qr_login_test.dart`, `biometric_unlock_test.dart`) ghi sẵn khóa đã xem giới thiệu SOS, không đổi hành vi. `flutter analyze` sạch, `flutter test` 187 xanh. Chưa chạy trên máy thật.

### Bỏ Bản tin khỏi cổng web theo Q-396 (2026-11-05, A)

- Chủ dự án duyệt (DESIGN.md 6.8.6): bỏ mục Bản tin ở đầu trang và thanh dưới (còn 5 và 4 mục), màn `BulletinPage.jsx`, khối "Bản tin mới nhất" ở Hôm nay, lối vào Bản tin và câu "bản tin tuần" ở trang chủ, dữ liệu mẫu `DEMO_PORTAL_BULLETINS` và thông báo mẫu bản tin. App người thân không có Bản tin.
- Ghi chú quy trình: `FamilyPortal.test.jsx` thêm `Q396_khongConBanTin` viết trước, đỏ rồi xanh; npm run lint, npm run build qua, npm test 531/91 file; xem `/family` bản mẫu trên trình duyệt.
