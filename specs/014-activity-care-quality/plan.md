# Implementation Plan: Quản lý hoạt động và chất lượng chăm sóc

**Branch**: `014-activity-care-quality` | **Date**: 2026-10-10, cập nhật 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/014-activity-care-quality/spec.md`

## Summary

Feature 014 là phần 8.8 → 8.10 và BR-M04-23 của module M04 (UC-29, UC-30, UC-31). Nó cung cấp:
1. Danh mục loại hoạt động, hoạt động, mẫu lặp; Trưởng tầng tạo buổi từ mẫu bằng lệnh (mục A, B; BR-M04-21, Q-306).
2. Buổi trong viện: tạo lẻ, dời, hủy; đăng ký có giới hạn số lượng, khoanh vùng, nghi nhiễm; đồng ý về muộn cho bán trú (mục C, D; BR-M04-15, Q-167).
3. Điểm danh buổi; lượt Có mặt ở hoạt động có thu phí là sự kiện tính phí (mục F; BR-M04-18).
4. Chuyến đi: phân công, đánh giá khả năng, thuốc mang theo, lịch xe, rời viện, gia hạn, về, thiếu người (mục G, H; BR-M04-16, BR-M04-17, Q-307).
5. Sở thích, hồ sơ tinh thần, cảnh báo nguy cơ cô lập (mục I; BR-M04-22).
6. Kiểm tra chất lượng: người kiểm tra tự chọn công việc đủ tỷ lệ, ghi kết quả, lập công việc làm lại (mục J; BR-M04-23, Q-307).

Hướng kỹ thuật:
- Thêm gói con vào `care` cùng 005; dùng dịch vụ nội bộ của 005 (công việc, có mặt bán trú, công việc làm lại) và cài các SPI 005 đã khai cho 014 (research L-01, L-02).
- Module phụ thuộc M04 (006, 007, 008, 011, 012) đã khai SPI cho 014; 014 cài. Với 015, 016 (làm sau), 014 khai `ChuyenDiNhiemVuLenhApi`, `ChuyenDiThayDoi`, `HoatDongThongKeApi` ở `care.api`; lịch xe gọi API ở `roombedallocation.api` do 019 cung cấp (sửa theo plan 019).
- Điểm danh rời viện, về gọi lệnh hệ thống của 001 trong cùng transaction; 001 điều phối các bean (005, 006, 011…) như mọi lệnh trạng thái (L-08).
- Điều kiện đăng ký, ngày tính, chuỗi cô lập, công việc đủ điều kiện, số mục tối thiểu, người kiểm tra là hàm thuần (L-05, L-09, L-10).
- Hợp đồng 007 thêm `YeuCauGhiDienBienCanhBao` và `DauNghiNhiemDaDoi` (L-11).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 014 không tạo buổi, danh sách kiểm tra, mục kiểm tra, công việc làm lại, sự cố, khoản chi phí; không gợi ý người tham gia, hoạt động, công việc cần kiểm tra;
- hệ thống chỉ: đăng ký sẵn người thường xuyên kèm lệnh tạo buổi; hủy tự động kèm sự kiện (đổi mẫu, khoanh vùng, nghi nhiễm, trạng thái cuối); gán "Không ghi nhận" hết ngày; cảnh báo cô lập, thiếu người qua 007; kiểm tra tỷ lệ; nhắc, thông báo.

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": feature 009 FR-001a; Q-363)

- **Đối tượng thông tin** (FR-071, 009 FR-001a): dòng đồng ý về muộn tới người đại diện và người đã đăng ký khai `doiTuongThongTin = VE_VIEC_NGUOI_THAN`; các thông báo khác chỉ tới nhân viên; không yêu cầu nào mang mã Q-354 (research L-13).
- **Sở thích theo nguồn** (FR-051, FR-052, Q-363): mỗi người cao tuổi tối đa một mục hiện hành cho mỗi (nhóm sở thích, nguồn); nguồn theo mẫu 001 (`NguonYKienApi`), nguồn người thân ghi rõ người nào; cảnh báo đăng ký nêu nguồn từng mục "không thích" (research L-09).

### Cập nhật 2026-10-18, lượt 3 (spec mục "Cập nhật 2026-10-18, lượt 3": Q-366, Q-369)

- **Kênh người cao tuổi** (FR-068a): 014 cài SPI `family.api.HoatDongKenhNctPort.sapToi(nctId, tu, den)`: buổi của tầng người cao tuổi và toàn viện, cờ đã có tên; không trả đánh giá khả năng tham gia, lý do không được đăng ký (research L-16).
- **Nút SOS** (FR-068a): 014 cài SPI `incident.api.ChuyenDiSosPort.dangDienRa(nctId)`: chuyến mà người cao tuổi đã điểm danh rời viện, chưa điểm danh về; trưởng đoàn, người đi cùng hiện tại (007 research S-23).
- **Nhắc buổi trong ngày** (FR-071): job 00:05 phát yêu cầu Nhẹ nhóm `NguoiCaoTuoi` (009 FR-007 (f)) cho buổi trong ngày mà người cao tuổi có tên; đăng ký trong ngày thì phát khi đăng ký. 009 giữ tới hết giờ yên tĩnh.
- **Sở thích ghi từ phản hồi Mong muốn** (001 FR-023g): `so_thich` thêm `phan_hoi_id NULL`; lệnh ghi có `phanHoiId` thì phát `YKienDaGhiTuPhanHoi` (001 research H-17). *(R-15: `so_thich` → `ho_so_chung`)*

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 013. Không thêm thư viện.

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md), cùng schema M04 với 005.

**Testing**:
- Như 000 R-14.
- Test bảng cho tám hàm thuần (research L-15); 30 kịch bản cô lập (SC-007); bộ 4 tuần tạo buổi (SC-001).
- Test tích hợp MySQL cho `UNIQUE` buổi, danh sách, mục, sở thích; trigger nhóm 3.
- Test đồng thời: hai đăng ký chỗ cuối; rời viện từ hai thiết bị; hai lệnh Báo thiếu người.
- Đồng hồ giả lập cho CFG-M04-06, 07, 10, 11, 13, CFG-M09-04, nửa đêm.
- Bản giả `LichXeApi`, `LichXeLenhApi` (019), `MangThuocChuyenDiApi`, `SuCoLenhApi`.

**Target Platform**: Như 000.
- Web: hoạt động, mẫu, buổi, đăng ký, điểm danh, chuyến đi, sở thích, hồ sơ tinh thần, kiểm tra chất lượng; cổng người thân (hoạt động, đồng ý về muộn).
- App nhân viên: điểm danh buổi (ngoại tuyến), rời viện/về (ngoại tuyến), Báo thiếu người (trực tuyến), danh sách chuyến, kiểm tra chất lượng tại tầng.
- App người thân: buổi, chuyến, số buổi tham gia; ghi/rút đồng ý về muộn.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Cảnh báo thiếu người ≤ 1 phút sau lệnh (SC-006): outbox ngay sau commit.
- Điểm danh buổi 20 người trên app trong thời gian của SC-009: một lệnh nhiều người.
- Tạo buổi từ mẫu cho 4 tuần ≤ 2 giây mỗi hoạt động.
- Job cô lập 00:00 cho 300 người ≤ 1 phút: đọc lô điểm danh, khoảng vắng, dấu nghi nhiễm.

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI).
- Điểm danh, đánh giá khả năng, điểm danh chuyến, kết quả kiểm tra không bị `UPDATE`, `DELETE` (constitution XII).
- Lệnh liên quan an toàn người (Báo thiếu người, Kết thúc điểm danh về có người thiếu) chỉ trực tuyến.
- Nhật ký trong transaction (000 FR-047). CFG đọc lúc chạy.

**Scale/Scope**:
- 9 thực thể có vòng đời (buổi, đăng ký, đồng ý về muộn, chuyến đi, người tham gia, nhiệm vụ chuyến, sở thích, danh sách kiểm tra, mục kiểm tra); 23 bảng (gồm bảng con, kỹ thuật).
- 2 job (theo phút, nửa đêm).
- 24 đường dẫn, 29 thao tác REST ([contracts/api-014.yaml](contracts/api-014.yaml)).
- 13 SPI hoặc provider 014 cài; 1 API và 1 sự kiện khai cho 015, 016; 1 giao diện thống kê cho 016; dùng 2 API lịch xe của 019 (M03).

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR; x-fr, x-br trên mọi thao tác REST; data-model mục 9 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Buổi, đăng ký, chuyến, người tham gia, mục kiểm tra chỉ có lệnh theo bảng trạng thái; chỉ danh mục nhóm 1 có sửa |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3, 4, 6, 7, 8 khớp tám bảng trạng thái của spec; quickstart mục 4 |
| V. Không cố định giá trị | Đạt | CFG-M04-06, 07, 10, 11, 13, CFG-M09-04 đọc từ `ThamSoApi` |
| VI. Vai trò, quyền | Đạt | Bảng L-14; nhiệm vụ trưởng đoàn chỉ mở lệnh của chuyến; người kiểm tra theo FR-066 |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ; điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-015 (trừ SC-011, 012 đã bỏ) có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 007 được bổ sung (mục "Đồng bộ") |
| X. Kiến trúc | Đạt | Một khối; gói con của `care`; module khác dùng `care.api` hoặc SPI. Không có vòng (L-02) |
| XI. Nghiệp vụ ở backend | Đạt | Điều kiện đăng ký, rời viện, chuỗi cô lập, tỷ lệ kiểm tra ở backend |
| XII. Dữ liệu | Đạt | `UNIQUE` và trigger ở data-model; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V |
| XIV. Ngoại tuyến, thông báo | Đạt | `NgoaiTuyenHandler` cho điểm danh; lệnh an toàn bắt buộc trực tuyến; thông báo theo FR-071; cảnh báo qua 007 |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR; mỗi BR-M04-15 → 18, 21 → 23 có test (quickstart mục 4) |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Bổ sung kỹ thuật giải thích ở Complexity Tracking.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 014 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | Chỉ mục theo buổi, người cao tuổi, ca |
| NFR-02 Hiệu năng | Có | Lệnh nhiều người; job cô lập đọc lô |
| NFR-03 Thông báo khẩn cấp | Có | Thiếu người: cảnh báo, sự cố Khẩn cấp qua 007 ngay sau commit |
| NFR-04 Tác vụ định kỳ | Có | Hai job; cột "đã làm lúc"; chạy bù |
| NFR-05 Khả dụng | Có | Ghi ngoại tuyến cho điểm danh; lệnh an toàn có hướng dẫn gọi trưởng tầng khi mất mạng |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL |
| NFR-07 Lưu trữ | Có | Theo hồ sơ với đăng ký, điểm danh, sở thích, kết quả kiểm tra |
| NFR-08 Bảo mật | Có | Thông tin sức khỏe khi đăng ký, đánh giá theo giới hạn trường của 002; cổng chỉ loại "chung" |
| NFR-09 Thời gian | Có | `Clock`; thời điểm thiết bị cho bản ghi ngoại tuyến |
| NFR-10 Thiết bị | Có | App nhân viên cho điểm danh, chuyến đi |
| NFR-11 Ngôn ngữ | Có | Tiếng Việt |
| NFR-12 Cấu hình | Có | CFG-M04 đổi qua tham số |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả port |

## Project Structure

### Documentation (this feature)

```text
specs/014-activity-care-quality/
├── plan.md                      # File này
├── research.md                  # Phase 0: L-01 → L-15
├── data-model.md                # Phase 1: bảng, bảng chuyển, DBR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng Q1 → Q45, truy vết → test
├── contracts/
│   ├── api-014.yaml             # OpenAPI 3.1: hoạt động, buổi, đăng ký, điểm danh, chuyến đi, tinh thần, kiểm tra, cổng
│   └── module-interfaces.md     # SPI 014 cài, giao diện mới ở care.api, sự kiện
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/eldercare/backend/care/
├── api/           # (bổ sung) ChuyenDiNhiemVuLenhApi, ChuyenDiThayDoi, HoatDongThongKeApi
├── hoatdong/      {internal, web}   # loại, hoạt động, mẫu, người thường xuyên, BuoiTheoMau, lich/
├── buoi/          {internal, web}   # buổi, đăng ký, DieuKienDangKy, đồng ý về muộn, điểm danh
├── chuyendi/      {internal, web}   # chuyến, nhiệm vụ, đánh giá, rời viện, về, thiếu người
├── tinhthan/      {internal, web}   # sở thích, hồ sơ tinh thần, NgayTinh, chuỗi cô lập
└── kiemtra/       {internal, web}   # danh sách, mục, CongViecDuDieuKien, NguoiKiemTra

frontend/src/tinh-nang/hoat-dong/
├── danh-muc/  ├── buoi/  ├── chuyen-di/  ├── tinh-than/  ├── kiem-tra-chat-luong/  └── cong-nguoi-than/hoat-dong/

mobile/lib/nhan-vien/hoat-dong/   # điểm danh, chuyến đi, kiểm tra tại tầng
mobile/lib/nguoi-than/hoat-dong/  # buổi, chuyến, đồng ý về muộn
```

**Structure Decision**: Theo R-02 của 000 và quyết định của plan 005: 014 thêm năm gói con vào `backend/…/care`.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| `chuyen_di` là bảng phụ của `buoi_hoat_dong` với trạng thái riêng | Spec có bảng trạng thái chuyến khác buổi trong viện; đăng ký, điểm danh, `UNIQUE` theo mẫu vẫn chung | Bảng chuyến độc lập: lặp mẫu lặp, đăng ký, tạo từ mẫu *(R-15: `chuyen_di` → `buoi_hoat_dong`)* |
| Sự kiện `YeuCauGhiDienBienCanhBao` bổ sung cho 007 | FR-047, FR-056 cần diễn biến vào cảnh báo; 007 chưa có lệnh này | Đóng rồi mở cảnh báo: trái FR-056 "cảnh báo không tự đóng" |
| `chuoi_co_lap` kỹ thuật | Tính chuỗi ngày tính mỗi đêm cho 300 người; xét lại khi đính chính | Tính lại toàn bộ lịch sử mỗi đêm: tốn, khó chạy bù *(R-15: `chuoi_co_lap` → `trang_thai_nct`)* |

## Đồng bộ với hợp đồng của feature khác

1. **007** ([../007-health-alert-incident/contracts/module-interfaces.md](../007-health-alert-incident/contracts/module-interfaces.md)): thêm sự kiện `YeuCauGhiDienBienCanhBao` (mục 1.13) và `DauNghiNhiemDaDoi` (bảng sự kiện 007 phát). 014 dùng `SuCoLenhApi.ghiNhan`, `themDienBien`, `YeuCauTaoCanhBao`, `KhoanhVungApi`, `SuCoApi.dau` như đã khai.
2. **001, 005, 006, 008, 010, 011, 012**: cài và dùng các SPI, API, sự kiện đã khai đúng chữ ký (`DIEM_DANH_ROI_VIEN.chuyenId` giữ như 006 đề xuất). Không đổi.
3. **015, 016, 019** (làm plan sau): 015 gọi `ChuyenDiNhiemVuLenhApi.goNhiemVu` trong lệnh đổi ca, nghỉ đột xuất; 019 cung cấp `roombedallocation.api.LichXeApi`, `LichXeLenhApi` mà 014 gọi (M04 → M03); 016 đọc `HoatDongThongKeApi` và nghe `ChuyenDiThayDoi`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Câu chữ trước Q-306 còn sót trong spec 014**: *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - bảng trạng thái buổi dòng "Hoàn tất điểm danh" và bảng đăng ký dòng "Ghi kết quả điểm danh" ghi tác động "Chi phí nháp cho lượt có mặt" (FR-032 đã sửa: chỉ là bản ghi nguồn, Hành chính lập khoản);
   - SC-003 ghi "có đúng một chi phí nháp trỏ về lượt đó";
   - bảng trạng thái chuyến đi dòng đầu ghi "Sinh từ mẫu / Hệ thống" (FR-006 đã sửa: Trưởng tầng tạo buổi từ mẫu bằng lệnh).
   Plan làm theo FR-006, FR-032. Đề nghị sửa các dòng trên và SC-003 thành "đúng một bản ghi nguồn ở feature 010".

2. **Ghi diễn biến vào cảnh báo**: FR-047 (người Thiếu khi về chưa có sự cố mà trở về) và FR-056 (cô lập) yêu cầu 007 "nhận diễn biến" vào cảnh báo; spec 007 chưa có yêu cầu tương ứng cho diễn biến cảnh báo từ module khác. Plan thêm sự kiện kỹ thuật `YeuCauGhiDienBienCanhBao` vào hợp đồng 007; đề nghị ghi một FR ở spec 007 để khớp. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Dấu nghi nhiễm**: FR-021a cần sự kiện khi dấu "nghi nhiễm" được gắn hoặc gỡ; bảng giao tiếp của 007 chưa liệt kê việc cung cấp sự kiện này cho 014. Plan thêm `DauNghiNhiemDaDoi` vào hợp đồng 007; đề nghị bổ sung dòng ở bảng giao tiếp của spec 007. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

4. **Câu chữ còn lẫn** (không đổi nghiệp vụ): FR-002, FR-017, FR-027, FR-066, FR-067 và tiêu đề bảng trạng thái dẫn "4.4" (Permission Matrix của `docs/phan-tich-yeu-cau.md` đã bỏ; tra Phụ lục 27). *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Ánh xạ bảng ở data-model mục 0; task T034. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.

## Ghi chú triển khai

### Phase 1 — 05/11/2026 (C)

Nhánh `impl/014-activity-care-quality` từ develop 0c155c46. Tên gói chủ dự án duyệt 05/11 theo R-02 (module lớn: `internal/<chức năng>/{service, domain, persistence, config}`): `care.hoatdong` → `care.internal.activity`, `care.buoi` → `care.internal.session`, `care.chuyendi` → `care.internal.trip`, `care.tinhthan` → `care.internal.wellbeing`, `care.kiemtra` → `care.internal.qualitycheck`; `care.hoatdong.lich` dùng gói job chung `care.scheduling` có sẵn (không tạo gói lịch riêng). Mỗi gói có `package-info.java`; lớp tầng `service`, `domain`, `persistence`, `config` tạo khi có task cần.

Luật T001 đặt trong `M04ArchitectureRules.NO_JOB_CREATES_ACTIVITY_RECORDS` (cùng chỗ luật job của 005): lớp ở `care.scheduling` hoặc bước `CareMinuteWork`/`CareDailyWork` không gọi hàm `create*`, `add*` của năm gói 014, không gọi `createRework`. Sự cố do 007 tạo, care không được phụ thuộc `incident` (luật `NO_DIRECT_HIGHER_MODULES` có sẵn), nên không thêm điều kiện riêng. Luật đặt theo tiền tố tên hàm vì lớp lệnh 014 chưa có; khi cài lệnh tạo buổi, danh sách, mục phải đặt tên bắt đầu bằng `create`/`add` hoặc bổ sung tên vào luật.

Kiểm: `M04ActivityArchTest` đỏ (thiếu luật, rồi thiếu gói `activity`) rồi xanh 3/3; bytecode mẫu job gọi `createSession`, `createChecklist`, `addItem`, `createRework` bị bắt đủ 4 vi phạm. 55 test kiến trúc xanh (`*ArchTest`, `ModuleBoundaryTest`, `T009_ArchitectureRulesTest`). Không chạy `clean verify` (phase chỉ thêm package-info và luật test; theo quy ước 2026-11-04, `clean verify` chạy trước PR).

### Dịch hợp đồng — 05/11/2026 (C)

Chủ dự án duyệt bản dịch `contracts/api-014.yaml` (glossary mục 20.2 → 20.5): 24 đường dẫn, 29 operation giữ nguyên số lượng, không thêm bớt trường; quy ước như api-012 (`/{command}`, `Problem`/`Blocked`, tiền tố cổng `/family-portal`); "buổi" là `activity-session` để không trùng buổi bán trú (`DayCareSession`) của 004. `npm run gen:api` sinh `frontend/src/api/api014.js` (thêm `014` vào `TRANSLATED`); `ApiContractIT` đăng ký api-014 với 29 operation ở danh sách chờ, xóa dần khi cài controller. Kiểm: `ApiContractIT` xanh, web lint, build qua.

### Phase 2 — 05/11/2026 (C, một phần: T002, T005 xong; T003, T004, T006 một phần)

**Sự kiện phát cho module khác (T002, chủ dự án duyệt 05/11).** Plan, research L-02, L-11 ghi 014 phát `incident.api.YeuCauTaoCanhBao`, `YeuCauGhiDienBienCanhBao`, `nutrition.api.ChuyenDiDaDoi`. Code: 014 nằm trong module `care`, mà `M04ArchTest.C_02` cấm care phụ thuộc incident và health đã phụ thuộc care (vòng incident → health → care như Phase 10 của 005); `nutrition` chưa có gói `api`. Theo mẫu 005, sự kiện đặt ở `care.api.events`: cảnh báo dùng lại `AlertRequested` (loại `MISSING_ON_RETURN` Khẩn cấp, `ISOLATION_RISK` Nhẹ khi cài US5, US6), `AlertProgressRequested` (diễn biến, L-11), `TripChanged` (ChuyenDiThayDoi, đồng thời là ChuyenDiDaDoi cho 011), `ActivitySessionCancelled`, `ActivitySessionChanged`, `ActivitySessionEnded`. Tính phí: `billing.api.events.CostSourceBilled`, `CostSourceChanged` (bản dịch SuKienTinhPhiNguon của 010) đã có trên develop, billing chỉ phụ thuộc administration, common nên care phát thẳng, giữ đúng hợp đồng 010. 007 (D), 011 (D) cài listener khi làm feature của họ (team-plan mục 8). `ActivityStatisticsApi` (HoatDongThongKeApi) khai view theo chú thích hợp đồng (`SessionStatisticsView`, `TripStatisticsView`, `QualityCheckStatisticsView`, `IsolationRiskView`); A (016) review trường.

**Bảng (T003).** Không có file `V014_01`: các bảng của T003 gộp vào bảng chung theo R-15, loại khai bằng mã (`ActivityKinds`). Lệch so với data-model 0.1: `hoat_dong` đặt ở `simple_record` (`M04.ACTIVITY`) thay `catalog_item` vì `catalog_item` có `uq_catalog_item_active_name` (tên Hiệu lực duy nhất theo loại) sẽ chặn hai tầng cùng tên hoạt động, ràng buộc không có trong nghiệp vụ; liên kết loại ↔ nhóm sở thích giữ trong thuộc tính loại (`interestGroupIds`). Người thường xuyên: `unique_key` `<hoạt động>:<người cao tuổi>` chỉ khi Hiệu lực (bỏ khi Kết thúc), test FR-010 chứng minh. Phiên bản mẫu: `unique_key` `<hoạt động>:<ngày áp dụng>`.

**Hoạt động (T004, một phần).** Quyền `ACTIVITY_OUTING` (CREATE, UPDATE, VIEW) kèm kiểm vai trò Trưởng tầng (Nhân viên chăm sóc, Điều dưỡng có CREATE cho đăng ký, điểm danh nhưng không khai báo hoạt động, FR-002). Target là tầng của hoạt động; ngoài phạm vi lớp phạm vi 002 trả 404. Thiếu tầng ở hoạt động theo tầng kiểm ở controller trước lớp quyền (422). Hoạt động toàn viện: Target không tầng luôn ngoài phạm vi với Trưởng tầng, cần lấy tầng từ địa điểm mặc định (FR-002a) qua cấu trúc vị trí 003, chưa làm. `create-sessions-from-template` trả 503 `CARE_SOURCE_UNAVAILABLE` tới US1 (T009); ngừng, đổi mẫu, người thường xuyên chưa tác động buổi vì chưa có bảng buổi (US1, US2). Chưa kiểm dịch vụ có trong danh mục 004 và vai trò người phụ trách (cần SPI vai trò nhân viên, care không được phụ thuộc staffing).

**Quyền danh mục loại hoạt động.** FR-001, L-14 cho Quản lý viện cấu hình loại hoạt động, nhưng Phụ lục 27 dòng "Hoạt động, ngoài viện" chỉ cho Quản lý viện Xem, không có dòng danh mục; chờ chủ dự án (team-plan mục 8). `/activity-types` còn ở danh sách chờ của `ApiContractIT`.

**Hợp đồng.** Chủ dự án cho bổ sung schema phản hồi của endpoint đọc theo từng endpoint (chỉ trường đã có ở schema ghi, thêm id, status, version): `GET /activities` trả mảng `ActivityView`. Record lồng của thân lệnh đặt tên `ActivityTemplate` để không trùng schema `Template` của module khác.

Kiểm: `T002_ActivityApiContractTest` đỏ 4/4 (thiếu kiểu) rồi xanh; `FR_002_ActivityDeclarationIT` viết trước, đỏ (thiếu lớp) rồi xanh 9/9; `ApiContractIT` xanh; 61 test kiến trúc/hợp đồng xanh.

**Job (T005).** Không thêm `ViecTheoPhutM04HoatDong`, `ViecHangNgayM04HoatDong`: khung `CareMinuteJob`, `CareDailyJob` của 005 đã có ShedLock, chạy bù lúc khởi động, đồng hồ giả lập, mốc (việc, ngày) `M04.DAILY_WORK` trong cùng transaction với tác động (`CareDailyWorkRunner`), nên 014 đăng ký bước `CareMinuteWork`/`CareDailyWork` tên tiền tố `M04HD_`; hai job cùng tên khóa sẽ chạy song song không cần thiết. Mốc giờ chạy hằng ngày 00:00 theo tasks (research L-15 ghi 00:05; hết ngày xét ngày hôm qua nên không ảnh hưởng). `ActivityScheduling.dueDates` trả các ngày từ ngày cũ nhất còn buổi chưa đóng tới hôm qua. Chống trùng theo sự kiện (`su_kien_m04hd_da_xu_ly`) dùng `ReminderMarkers` ở story. Luật `NO_JOB_CREATES_ACTIVITY_RECORDS` (T001) áp cho các bước này.

**Provider (T006).** Thêm vào `care.internal.governance` (cùng chỗ provider 005): loại phạm vi `M04.ACTIVITY_SESSION`, `M04.TRIP` (cùng bảng `activity_session`, theo `floor_area_id`), `M04.ACTIVITY_REGISTRATION`, `M04.PREFERENCE` (theo người cao tuổi, tầng từ vị trí hiện tại), `M04.QUALITY_CHECK` (theo `floorAreaId` của danh sách `M04.QUALITY_CHECKLIST`); "chỉ người kiểm tra và Quản lý viện" là luật quyền ở lệnh (US7), không phải phạm vi. Tên bảng, cột dịch từ data-model 3, 4 (`activity_session`: `activity_id`, `starts_at`, `floor_area_id`, `status`; `activity_registration`: `session_id`, `resident_id`, `status`, `acknowledged_warnings`), US1 tạo bảng phải theo các tên này. Lưu giữ: buổi, chuyến `KHONG_GAN_HO_SO` mốc giờ bắt đầu; đăng ký `THEO_HO_SO`; dòng 014 ở bảng chung gắn `resident_id` xóa cùng hồ sơ qua `SharedRowRetention`; hoạt động, danh sách kiểm tra (không gắn hồ sơ, bảng chung) làm khi cài US7. `ActivityTimetableSource` đọc đăng ký còn hiệu lực của buổi chưa hủy trong ngày (nguồn `ACTIVITY`).

Kiểm T005, T006: `ChayBuM04HoatDongIT` đỏ 2/2 rồi xanh; `T006_ActivityGovernanceIT` đỏ 5/5 (thiếu bean) rồi xanh; `T008_CareGovernanceIT`, `ApiContractIT` xanh; 53 test kiến trúc xanh.
