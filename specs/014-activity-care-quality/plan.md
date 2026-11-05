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

### Phase 3 (US1) — 05/11/2026 (C; backend xong, màn web chờ duyệt bố cục)

**Bảng (T008).** `V014_02__activity_session.sql` giữ hai bảng riêng của data-model mục 0. Giờ lưu theo giờ Việt Nam như bảng 005 (DBR-25). `auto_cancel` thêm giá trị kỹ thuật `SESSION_CANCELLED` (đăng ký hủy theo buổi bị hủy, FR-013) ngoài sáu giá trị của data-model. `ck_activity_session_cancel` buộc `cancelled_by` có khi và chỉ khi Đã hủy.

**Tên lớp.** `SessionStore`, `SessionService` trùng tên bean của `administration.account` (Spring dùng tên lớp làm tên bean), nên đặt `ActivitySessionStore`, `ActivitySessionService`; `SessionTemplate` (hàm thuần `BuoiTheoMau`), `SessionDayEnd`, `ListedSession` giữ tên ngắn. Hàm tĩnh `ActivityService.target(Long floorAreaId)` trùng tên với `target(long id)`: chỗ gọi bằng `Long` phải đổi sang `long` (lỗi đã gặp ở controller buổi lẻ).

**Điều kiện đăng ký sẵn (FR-008, FR-010, FR-012).** US1 kiểm FR-015 (a) số lượng, (d) trạng thái người cao tuổi (Đang tiếp nhận hoặc trạng thái cuối, `ResidentApi`), FR-016 trùng giờ với đăng ký còn hiệu lực khác. (c) khoanh vùng, (g) nghi nhiễm cần API của 007 (care không được phụ thuộc incident, C-02) và lịch bán trú (FR-016) làm ở US2 cùng `DieuKienDangKy`. Dời buổi hủy đăng ký không còn đạt (trừ số lượng: dời không làm vượt số lượng).

**Thông báo.** Đã có: người thường xuyên không được đăng ký sẵn (FR-008, gộp một lần mỗi lệnh), buổi bị hủy do đổi mẫu (FR-009), hết ngày (FR-014a), cùng khuôn `CareNotice` của 005. Người nhận là Trưởng tầng của tầng hoạt động; hoạt động toàn viện chưa có tầng nên gửi người tạo. Thông báo cho người đã đăng ký khi buổi bị hủy, dời và khi đăng ký bị hủy tự động làm ở US2 cùng các thông báo đăng ký (bảng thông báo dòng "Buổi bị hủy hoặc dời").

**Hết ngày (FR-014a).** Bước `M04HD_SESSION_DAY_END` trong `CARE_DAILY` (00:00, chạy bù từ ngày cũ nhất còn buổi Đã lên lịch). "Có lượt điểm danh" là có đăng ký Đã điểm danh. Đăng ký chưa có kết quả sang Đã điểm danh và ghi `record_event` `M04.SESSION_ATTENDANCE` kết quả `NOT_RECORDED` (cha là hồ sơ người cao tuổi để xóa cùng hồ sơ, `unique_key` theo đăng ký); điểm danh đầy đủ thuộc US3 dùng cùng loại. Chuyến đi (US4) loại khỏi bước này khi thêm cột chuyến.

**Khác.** `create-from-template` không tạo buổi cho khoảng sau ngày kết thúc mẫu; cả khoảng nằm sau ngày kết thúc thì 409 `AFTER_TEMPLATE_END_DATE`. Buổi lẻ, dời chỉ cho thời điểm tương lai. `locationId` của thân tạo buổi lẻ, dời buổi ghi vào `location_room_id`; kiểm địa điểm theo cấu trúc 003 (FR-002a) còn mở như T004. Tầng của buổi là tầng của hoạt động; buổi toàn viện chưa có tầng nên ngoài phạm vi Trưởng tầng (mở từ T004). Hợp đồng: thêm schema `ActivitySessionDetail` cho `GET /activity-sessions/{id}` (bổ sung schema phản hồi theo từng endpoint, chủ dự án duyệt 05/11).

Kiểm: `BuoiTheoMauTest` 5/5, `BR_M04_21_TaoBuoiTuMauIT` 9/9, `FR_014a_HetNgayIT` 2/2, `FR_002_ActivityDeclarationIT` 9/9 (ca chờ 503 đổi thành 422 khi chưa có mẫu), `T006_ActivityGovernanceIT`, `ChayBuM04HoatDongIT`, `T008_CareGovernanceIT`, `NFR_04_CareSchedulingIT`, `ExpiredDataPurgeIT`, `ApiContractIT` xanh (48 IT); 78 test đơn vị, kiến trúc xanh. Màn web chưa làm: chờ duyệt bố cục.

**Web (T009, bố cục duyệt 05/11/2026).** `features/activities/`: bảng buổi trong tuần (Thứ Hai → Chủ nhật) của tầng, thanh lọc trạng thái có số đếm, khung chi tiết `DetailLayout` với Dời buổi, Hủy buổi (lý do bắt buộc, bước xác nhận); nút chính Tạo buổi từ mẫu, nút phụ Tạo buổi lẻ mở bằng `Sheet`. Tên hoạt động lấy từ `GET /activities` (cùng module). Đăng ký hiện mã người cao tuổi vì api-014 không có họ tên (DESIGN.md mục 13 điều 6). Nút hiện theo quyền `ACTIVITY_OUTING` (CREATE, UPDATE); Điều dưỡng, Nhân viên chăm sóc có quyền này cho đăng ký, điểm danh nên vẫn thấy nút, backend trả 403 khi không phải Trưởng tầng (contract không có danh sách lệnh được phép cho buổi). Chưa xem trên trình duyệt với dữ liệu: chạy `npm run dev` không có backend thì màn ở trạng thái đang tải như các màn gọi API khác.

### Phase 4 (US2) — 05/11/2026 (C, một phần: đăng ký, hủy, hủy tự động, web đăng ký; đồng ý về muộn chờ quyền)

**Nguồn 007 (chủ dự án duyệt 05/11).** Plan, research L-05 ghi 014 gọi `KhoanhVungApi`, `SuCoApi.dau` và nghe `KhoanhVungDaTao`, `DauNghiNhiemDaDoi`. Care không được phụ thuộc incident (`M04ArchTest.C_02`, vòng incident → health → care), nên theo mẫu C-02 của 005: SPI `care.api.spi.ActivityHealthGatePort` (007 cài: `residentIsolated`, `locationIsolated`, `suspectedInfection`, `hasOpenMediumOrHigher`) và API lệnh `care.api.ActivityRegistrationAutoCancelApi` (MANDATORY; 007 gọi `cancelForIsolation` trong lệnh tạo khoanh vùng, `cancelForSuspectedInfection` khi gắn dấu). Chưa có bean SPI thì đăng ký bị chặn 503 `CARE_SOURCE_UNAVAILABLE` (mặc định an toàn, chủ dự án duyệt); đăng ký sẵn người thường xuyên khi đó coi như không đạt và vào thông báo FR-008. Test dùng `FakeActivityHealthGate` trong `FakeM04Config`. Team-plan mục 8 ghi việc cho D.

**Điều kiện (T011).** `RegistrationConditions` (hàm thuần `DieuKienDangKy`): chặn FR-015 (a) số lượng, (c) khoanh vùng người hoặc địa điểm, (d) trạng thái, (e) buổi không ở Đã lên lịch hoặc đã bắt đầu, (g) nghi nhiễm ở hoạt động nhóm, FR-016 trùng giờ và lịch bán trú (người bán trú lấy từ `ContractReadApi.contentAtDate` + `contract().residenceType`, lịch từ `attendanceOn`); FR-016a cần đồng ý về muộn. Cảnh báo FR-018: lưu ý chăm sóc (`AssessmentApi.currentCareNote`), nghi nhiễm ở hoạt động cá nhân, mục Trung bình trở lên đang mở. Chưa xét đối tượng của hoạt động (mức chăm sóc, loại hình, tầng) vì `targetGroup` chưa có cấu trúc trong hợp đồng, và sở thích "không thích" (FR-052, US6). Lỗi chặn trả 409 mã của điều kiện đầu kèm `unmetConditions`; cảnh báo chưa xác nhận trả 409 `WARNING_ACKNOWLEDGEMENT_REQUIRED` kèm danh sách; đăng ký lưu `acknowledged_warnings`. Đăng ký sẵn từ mẫu (FR-008), thêm người thường xuyên (FR-010) và dời buổi (FR-012) dùng cùng điều kiện; vì vậy buổi đã bắt đầu không nhận đăng ký sẵn (sửa test `BR_M04_21` sang khoảng từ ngày mai).

**Hủy tự động.** `RegistrationAutoCancels`: khoanh vùng hủy buổi chưa điểm danh có địa điểm trong vùng và đăng ký chưa diễn ra của người có giường trong vùng (FR-021); nghi nhiễm hủy đăng ký ở hoạt động nhóm (FR-021a; chuyến đi thêm ở US4); trạng thái cuối qua `ActivityStatusParticipant` `@Order(31)` (sau 005) hủy đăng ký chưa diễn ra, kết thúc dòng người thường xuyên và nhả `unique_key` (FR-022). Thông báo `ActivityRegistrationsCancelled` mỗi buổi tới người phụ trách buổi, người đã đăng ký (không gửi người đăng ký sẵn nguồn thường xuyên), Trưởng tầng; dùng luôn cho hủy, dời buổi (phần US1 hẹn ở US2).

**Đồng ý về muộn (chưa làm).** Phụ lục 27 không có dòng "Hoạt động, ngoài viện" cho Hành chính; Người thân (AC-10) chỉ có VIEW. Hai kênh ghi của FR-016a vì vậy không có quyền ghi để gắn `@RequiresPermission`; chủ dự án chọn chờ (team-plan mục 8). Trong lúc chờ, người bán trú đăng ký buổi kết thúc sau giờ về luôn bị chặn `LATE_RETURN_CONSENT_REQUIRED`. Người đại diện khi làm sẽ kiểm qua `admission.api.spi.RepresentativePort` (012 đã cài; care không gọi được `family.api`, chủ dự án duyệt).

**Web (T012, bố cục duyệt 05/11).** Khung chi tiết buổi thêm khối "Đăng ký người cao tuổi" (ô chọn lấy `GET /residents` của 001 như màn Điểm danh bán trú của 005), cảnh báo cần xác nhận hiện bằng `Alert` tông attention với nút "Xác nhận cảnh báo và đăng ký", điều kiện chưa thỏa qua `ProblemAlert`; bảng đăng ký hiện họ tên (thiếu thì mã) và nút "Hủy đăng ký" mỗi dòng (lý do bắt buộc).

Kiểm: `DieuKienDangKyTest` 9/9, `BR_M04_15_DangKyIT` 6/6, `FR_021_KhoanhVungIT` 3/3, `BR_M04_21_TaoBuoiTuMauIT` 9/9, IT chuyển trạng thái của 001, 004, 012 (`EndingClosesRequestsIT`, `FR_002_DeclineCancelAdmissionIT`, `M02CommandRollbackIT`, `M02StatusParticipantIT`, `FR_075_FinalStatusIT`, `BR_M01_06_ModuleCommandsIT`, `M01CommandAuditTrailIT`, `ResidentAccountFinalStatusIT`), `ApiContractIT` xanh (77 IT); 71 test đơn vị, kiến trúc xanh. Web `npm test` 500/500, lint, build qua.

### Phase 5 (US3) — 05/11/2026 (C, một phần: backend, web; còn app, nguồn truy vết cho 007)

**Lưu trữ.** Điểm danh là `record_event` `M04.SESSION_ATTENDANCE` (data-model mục 0: `diem_danh_buoi` → `su_kien_chung`), cha hồ sơ người cao tuổi, `unique_key` theo đăng ký (một kết quả mỗi đăng ký), xác nhận ngay (nhóm 3). Đăng ký chuyển Đã điểm danh khi có kết quả. Không có `V014_04`. Thêm cột `activity_registration.billed_source_id` (`V014_03`): nguồn tính phí đang hiệu lực của đăng ký; bản ghi điểm danh không sửa được nên trạng thái tính phí giữ ở đăng ký để đính chính nhiều lần (Có mặt → Vắng → Có mặt) vẫn đúng; đọc giá trị hiện hành qua `CorrectionApi` từ provider sẽ tạo vòng bean.

**Tính phí (FR-032).** Lượt Có mặt ở hoạt động có thu phí phát `billing.api.events.CostSourceBilled` (`sourceType` `ACTIVITY_ATTENDANCE` có trong enum `cost_item.source_type` của 010; đơn vị `OCCURRENCE` cho "lượt", 010 chưa chốt tên đơn vị nên ghi team-plan mục 8 cho E xác nhận; `sourceId` là mã bản ghi điểm danh; ngày là ngày của buổi). Đính chính: bỏ lượt Có mặt (Vắng hoặc Hủy ghi nhận) phát `CostSourceChanged` hủy nguồn; thêm lượt Có mặt phát `CostSourceBilled` mới với `sourceId` `C<mã đính chính>` (FR-031).

**Điểm danh (FR-027 → FR-030).** Quyền `ACTIVITY_OUTING` CREATE (Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc), Hoàn tất cần UPDATE; FR-027 "người phụ trách buổi hoặc người có người cao tuổi trong phạm vi" do lớp phạm vi theo tầng của buổi. Ghi từ giờ bắt đầu (409 `NOT_YET_STARTED`); Có mặt cần mức tham gia, Vắng cần lý do (422); đăng ký đã có kết quả trả 409 `ATTENDANCE_ALREADY_RECORDED`. Người thêm lúc điểm danh xét FR-015 (a) → (d), (g) qua `RegistrationEligibility` (bỏ qua "đã bắt đầu", trùng giờ, lịch bán trú). "Ghi nhận muộn" khi thời điểm ghi (thiết bị nếu ngoại tuyến) sau kết thúc + CFG-M04-06. Nhắc một lần qua bước `CARE_MINUTE` (`attendance_reminded_at`). Chưa hiện sẵn "Vắng – đang vắng mặt" (FR-028) vì api-014 không trả trạng thái người cao tuổi, bán trú cho buổi.

**Nguồn cho module khác.** `ActivityTaskSource` cài `care.api.spi.TaskSourcePort` loại `ACTIVITY_SESSION` (căn cứ đăng ký cho công việc "hoạt động" của 005). `DiemDanhTruyVetPort` (007 FR-060 (b)): D khai `incident.api.spi.AttendanceTracePort` cho 014 cài, nhưng care không được phụ thuộc incident (C-02); đề nghị D chuyển khai báo sang `care.api` (mẫu `ActivityHealthGatePort`), ghi team-plan mục 8.

**Web (bố cục duyệt 05/11).** Từ giờ bắt đầu, khung chi tiết buổi ẩn khối đăng ký và hiện khối "Điểm danh": mỗi đăng ký chưa có kết quả một dòng (Có mặt/Vắng; mức tham gia bắt buộc, mức giao tiếp, tình trạng sau, ghi chú; lý do vắng bắt buộc), "Thêm người lúc điểm danh", nút "Lưu điểm danh"; nút "Hoàn tất điểm danh" khi mọi đăng ký đã có kết quả. Hai test đăng ký của US2 đổi sang buổi ở tương lai.

Kiểm: `BR_M04_18_SuKienTinhPhiIT` 7/7, `FR_031_DinhChinhIT` 3/3, `T014_SessionAttendanceSourcesIT` 3/3 cùng IT 014, khung job, đồng bộ ngoại tuyến (`OfflinePendingReviewIT`, `SyncResultIT`), đính chính (`CorrectionIT`, `CorrectionViewIT`, `FR_026_CorrectionResidentIT`), điểm danh bán trú 005, `ApiContractIT`: 88 IT xanh; 83 test đơn vị, kiến trúc xanh. Web `npm test` 503/503, lint, build qua.

### Phase 6 (US4) — 05/11/2026 (C; kế hoạch triển khai, chưa nghiệm thu)

**Phạm vi.** T015 → T018: chuẩn bị chuyến, phân công, đánh giá và rời viện. Chưa cài lệnh trở về/gia hạn/thiếu người của Phase 7. Giữ hợp đồng HTTP đã dịch; giao diện mới phải được duyệt bố cục trước khi viết mã. Không commit/push/PR nếu chưa được yêu cầu.

**Thứ tự và kiểm chứng.**
1. T015/T016: test MySQL `T016_TripSchemaIT` đỏ trước migration. Thêm `V014_05__activity_trip.sql`: cột chuyến trên `activity_session`, cột người tham gia trên `activity_registration`; không tạo bảng chuyến, nhiệm vụ, đánh giá hoặc điểm danh riêng. Test lưu được chuyến và người tham gia, chặn khoảng giờ không hợp lệ/cột chuyến thiếu, giữ bản ghi buổi cũ, giữ UNIQUE đăng ký.
2. T015/T017 sau khi duyệt API bên dưới: test `FR_035_NhiemVuIT`, `BR_M04_16_RoiVienIT` cho vai trò/tài khoản/ca chồng giờ, đánh giá đúng tầng, thuốc và xe, hai thiết bị, trạng thái 001 thật và rollback. Nhiệm vụ dùng `StatusHistoryApi`, đánh giá/điểm danh dùng `RecordEventApi`; mọi lệnh audit cùng transaction. Loại chuyến khỏi hết ngày và nhắc điểm danh buổi; nghi nhiễm chặn chuyến cá nhân lẫn nhóm, Không đi trước rời và chỉ thông báo khi đã rời.
3. T018: endpoint tạo buổi có `trip`, đọc chuyến, phân công, đánh giá, `depart`; chỉ bỏ pending `ApiContractIT` cho operation đã cài đủ. Không công bố đã hoàn tất các command Phase 7 chưa cài. ApiContractIT kiểm theo operation, không theo từng giá trị command: giữ đúng route /trips/{id}/{command} và enum hợp đồng, chỉ depart có phần cài Phase 6; sáu command Phase 7 trả 503 CARE_SOURCE_UNAVAILABLE. Việc bỏ pending cho route không thay cho nghiệm thu từng command hoặc đánh T018 hoàn tất. Schema phản hồi chuyến phải duyệt cùng đề xuất API; không tự thêm thông tin sức khỏe/liên hệ ngoài hợp đồng đã chốt.
4. Cuối phase: spotless; test task và kiến trúc/IT liên quan; `./mvnw -o clean verify` chạy nền, log `backend/logs/`; đối chiếu lỗi sẵn có theo team-plan. Chỉ đánh [x] phần đã kiểm chứng; cập nhật progress và Working-history.

**Hướng API đã được chủ dự án duyệt trong phiên 05/11/2026.** Các điểm dưới đây là nội dung đề xuất đã duyệt; tên Java chính thức ở contracts/module-interfaces mục 3.
- 006 đã phụ thuộc care; care gọi `medication.api.TripMedicationApi` hoặc cài `medication.api.spi.TripMedicationPort` sẽ tạo vòng. Đề xuất care khai `care.api.spi.TripMedicationPreparationPort` cho D cài, gồm `boolean isPrepared(long residentId, long tripId)`, `void onTripChanged(long tripId, long residentId, TripMedicationChangeType changeType)`, `void recalculateInterval(long tripId, long residentId, TripInterval newInterval)` (hai lệnh MANDATORY). `TripInterval(Instant from, Instant to)` và enum đổi chuyến đặt ở `care.api.dto`, ánh xạ đủ giá trị hợp đồng thuốc hiện có sau khi duyệt. Nguồn ngược lại: `care.api.TripMedicationReadApi` có `Optional<TripInterval> approvedTripInterval(long residentId, long tripId)`, `Optional<TripInterval> tripInterval(long tripId)`. D bọc adapter ở module của D, giữ API/SPI cũ để không gãy caller; C không sửa medication.
- care không được phụ thuộc staffing (C-02). Đề xuất `care.api.TripDutyReadApi`: `Optional<Long> groupLeader(long tripId)`, `List<Long> companions(long tripId)`, `List<TripInShiftView> inShift(long floorAreaId, Instant from, Instant to)`; view ở `care.api.dto` giữ đúng năm trường của `staffing.api.spi.TripInShiftView`. E cài adapter từ SPI staffing hiện có sang API care; C không sửa staffing. Kiểm tài khoản/vai trò qua `AccountApi` hiện có; thêm `care.api.spi.TripStaffSchedulePort` với `boolean hasOverlappingPublishedShift(long staffId, Instant from, Instant to)` cho E cài để không suy ca theo một thời điểm hoặc chỉ theo tầng tổ chức. Thiếu nguồn thì chặn 503, không dùng fake trong production.
- 019 đã có hợp đồng logic nhưng chưa có Java API. Đề xuất dịch `LichXeApi` → `roombedallocation.api.VehicleScheduleApi` (`hasBookedSchedule`, `checkChange`), `LichXeLenhApi` → `VehicleScheduleCommandApi.followTrip` (MANDATORY), giữ nguyên tham số/ngữ nghĩa contracts 019 mục 2.2. C khai API và fake duy nhất của 019; chưa có provider thật thì xe viện bị chặn, chuyến phương tiện khác không gọi nguồn xe. Kiểu kết quả/enum dịch theo hợp đồng 019, không đổi nghiệp vụ.

**Quyết định kỹ thuật thuộc C.** Tách thông tin chuyến khỏi record `Session` hiện có bằng phần dữ liệu trên cùng dòng và repository `care.internal.trip.persistence`; không buộc caller 005/014 truyền trường chuyến khi tạo buổi trong viện. Cột tham gia nullable cho đăng ký buổi trong viện; service đồng bộ với trạng thái đăng ký, không đặt FK tới bảng của module chưa vào develop. Không sửa migration V014_02/V014_03 đã commit.
### T018 — Schema đọc và bố cục đã được chủ dự án duyệt 05/11/2026

**GET /trips/{id}.** Đề xuất `TripDetail`: `id`, `status`, `version`, `destination`, `plannedDepartureAt`, `plannedReturnAt`, `transport`, `facilityVehicle`, `leaderId`, `companionIds`, `participants` (mỗi dòng `id` mã đăng ký, `residentId`, `status`, `reason`). Các trường chuyến/phân công lấy đúng thân ghi đã có; mảng participants bổ sung cấu trúc phản hồi để thực hiện FR-039 (trạng thái từng người). Không tự thêm họ tên, liên hệ chính, thuốc hoặc thẻ khẩn cấp khi chưa có hợp đồng nguồn. Chủ dự án đã duyệt schema này cùng bố cục web/app; GET được cài với đúng các trường trên. Trạng thái người tham gia giữ bản dịch glossary 20.7, gồm MISSING_ON_RETURN và LEFT_GROUP.

**Web.** Giữ màn Buổi hoạt động và menu hiện có; không tạo menu thứ hai. Sheet Tạo buổi lẻ có lựa chọn Chuyến đi, mở khối Điểm đến, Giờ rời/về dự kiến, Phương tiện, Xe của viện. Dòng chuyến bấm mở khung chi tiết cùng DetailLayout: (1) thông tin chuyến, trạng thái; (2) phân công trưởng đoàn/người đi cùng, nhập mã nhân viên tới khi có nguồn chọn phù hợp; (3) danh sách người tham gia và trạng thái. Bấm một người mở Sheet đánh giá Đạt/Không đạt, Không đạt bắt buộc lý do. Ô chọn người Được đi, nút chính Điểm danh rời viện ở chân; Trưởng tầng làm thay có ô lý do. Lỗi 409/503 giữ trong khung qua ProblemAlert, giữ lựa chọn và nội dung đã nhập. Thông tin sức khỏe, liều thuốc chưa có nguồn/field được duyệt phải hiện rõ trạng thái chưa có dữ liệu, không suy ra an toàn; phần đánh giá đầy đủ FR-036 còn mở tới khi nối đủ nguồn. Trạng thái chữ màu theo DESIGN, không thêm màu.

**App nhân viên.** `mobile/lib/staff/activities/`: danh sách chuyến theo ngày, dòng trắng trạng thái; bấm mở màn chi tiết dài, cùng thứ tự nội dung và câu chữ của web; nút Điểm danh rời viện cố định ở chân, xác nhận danh sách trước gửi. Gọi ApiClient.requestJson; không tự tạo client/hàng đợi. Đường ngoại tuyến hiện bị chặn do 001 chưa hỗ trợ deviceAt cho lịch sử trạng thái: không báo đã đồng bộ hoặc âm thầm dùng giờ máy chủ. Tích hợp màn vào StaffRoot do A review; không tự sửa common/mobile nền.

**Giới hạn nghiệm thu.** Đây là bố cục cho phần dữ liệu/endpoint đã có thể cài. T018 không đánh [x] chỉ vì có form: FR-036/039 còn nguồn thuốc, chi tiết cảnh báo, liên hệ chính và thẻ khẩn cấp; app còn kiểm trên máy thật/giả lập. Phase 6 chưa hoàn tất tới khi các phần còn lại được xử lý hoặc chủ dự án chốt phạm vi.
### Phase 6 — Kết quả triển khai và phần còn mở (05/11/2026)

- Backend đã có tạo chuyến qua thân trip của buổi lẻ, phân công, đánh giá PASS/FAIL, GET chi tiết tối thiểu và depart. T015 gộp ca nhiệm vụ vào BR_M04_16_RoiVienIT thay vì tạo FR_035_NhiemVuIT riêng vì cùng fixture HTTP, phạm vi và nguồn ca; không nhân đôi fixture. T016 dùng T016_TripSchemaIT (7 ca). Nhiệm vụ giữ snapshot leaderId/companionIds theo kỳ; đóng kỳ khi hủy. Phân công qua endpoint assignment chỉ trước khi rời viện; chuyển trưởng đoàn/bổ sung người sau rời thuộc lệnh Phase 7 để không vượt yêu cầu lý do.
- Rời viện gọi trạng thái 001 thật trong cùng transaction; khóa người cao tuổi theo id tăng dần trước khóa chuyến, sau đó kiểm lại version/điều kiện để tránh hai thiết bị cùng ghi. Không đọc trạng thái 001 trước lệnh gây giữ entity với phiên bản cũ. Thuốc chưa chuẩn bị, nghi nhiễm/khoanh vùng, xe chưa Đã đặt, bán trú chưa Có mặt hoặc ngoài khung thời gian đều bị chặn. Thiếu nguồn thật trả 503; callback xe lỗi rollback cả trạng thái, điểm danh và tính phí.
- Chuyến được loại khỏi SessionDayEnd và nhắc điểm danh buổi thường; endpoint điểm danh/Hoàn tất của buổi thường từ chối chuyến. Khoanh vùng không hủy chuyến đã rời viện sớm. Nghi nhiễm trước rời chuyển Không đi; sau rời chỉ phát thông báo. Nguồn bàn giao giữ chuyến đang đi dù quá giờ về dự kiến. Chuyến chưa rời được dời sau giờ bắt đầu dự kiến; đổi ngày trả người Được đi về Đã đăng ký và báo nguồn thuốc. Tạo từ mẫu ngoài viện giữ 503 vì thân lệnh hiện không có đủ metadata chuyến từng buổi; không tạo buổi ngoài viện thiếu dữ liệu.
- Web đã có tạo buổi lẻ kèm chuyến, khung thông tin/phân công/người tham gia, đăng ký có xác nhận cảnh báo, hủy đăng ký có lý do, dời/hủy chuyến và xác nhận rời viện. Lỗi giữ lựa chọn, nội dung và Idempotency-Key khi thử lại cùng thân lệnh; khi xác nhận cảnh báo đổi thân thì đổi khóa. App có danh sách theo ngày, chi tiết dài và xác nhận rời viện trực tuyến; nút chính cố định ở chân, lỗi hiện cạnh nút. Chưa làm Sheet đánh giá đầy đủ vì chưa có nguồn lâm sàng để hiển thị đúng FR-036; không biến form PASS/FAIL thành bằng chứng đã đủ thông tin.
- Kiểm trực quan chưa thực hiện được: trình duyệt tích hợp timeout tới localhost; computer-use của Chrome tự dừng vì không xác định chắc URL để áp chính sách. Không có ảnh hoặc kết luận ba bề rộng/tối/200%/máy thật. Widget test không thay cho nghiệm thu trực quan.
- T015–T018 tiếp tục để mở: adapter D/E và provider xe 019; dữ liệu thuốc/sức khỏe/liên hệ/thẻ khẩn cấp theo quyền; ngoại tuyến cần 001 nhận thời điểm nguồn; A review tích hợp màn app vào StaffRoot; tạo chuyến từ mẫu còn thiếu trường hợp đồng. Chưa commit/push/PR.

**Đối chiếu cuối Phase 6.** Ba actor_id của nhiệm vụ/đánh giá/rời viện đã đổi sang user.accountId(); test DBR_04 dùng fixture accountId khác staffId, đỏ đúng cả ba trường trước sửa. Mã lỗi nhánh chuyến giữ đúng bản dịch api-014/glossary 20.5 (ROLE_NOT_ELIGIBLE, NOT_ON_SHIFT, ELIGIBILITY_NOT_PASSED, MEDICATION_NOT_HANDLED, DEPARTURE_TOO_EARLY, VEHICLE_ALREADY_SCHEDULED; tình huống trạng thái dùng STATE_CHANGED/SESSION_ALREADY_STARTED). Không công bố các alias từng dùng trong triển khai. BR-M04-15/FR-021a: sau xuất phát một phần, registeredAfter vẫn chọn người còn REGISTERED/ELIGIBLE của chuyến IN_PROGRESS để tự hủy, nhưng không chọn DEPARTED; test riêng đã đỏ ELIGIBLE thay NOT_GOING trước khi sửa truy vấn. Phần giữ nguyên cho người đã đi được kiểm cùng ca này.

**Race phân công khi rời viện (05/11/2026).** Mốc full trước sửa đã xanh (1092 đơn vị, 2692 IT, 2 perf skipped). Test mới ép REPEATABLE_READ bằng hai transaction/latch tái hiện lệnh dùng trưởng đoàn cũ vẫn được chấp nhận sau khi phân công mới commit: status_history là SELECT snapshot, chỉ khóa lại chuyến không làm mới snapshot lịch sử. Quyết định kỹ thuật: mọi replaceDuties tăng phiên bản dòng activity_session trong cùng transaction, bao gồm gỡ nhiệm vụ; kiểm phiên bản đang có ở depart buộc rollback 001 khi snapshot hết hiệu lực. Giữ khóa hồ sơ → chuyến, không đổi isolation hay hợp đồng. Test đã đỏ trước sửa, sau sửa nhóm 53 kiến trúc + 73 IT xanh (33 ca chuyến); không dùng mốc full trước fix để đánh nghiệm thu bản mã sau.

**Race hủy đăng ký khi rời viện (05/11/2026).** Test hai luồng tái hiện đăng ký bị hủy sau lần kiểm tra ban đầu nhưng trước khi ghi `DEPARTED`; bản đầu cho phép lệnh rời ghi đè đăng ký đã hủy. Quyết định kỹ thuật: dùng cập nhật có điều kiện `status='REGISTERED' AND participant_status='ELIGIBLE'`, tăng phiên bản khi thành công; nếu không có dòng bị cập nhật thì rollback toàn transaction và trả `STATE_CHANGED`. Mã lỗi hủy sau khi chuyến đã rời dùng `SESSION_ALREADY_STARTED` theo hợp đồng. Scoped cuối sau hai race: 53 test kiến trúc và 74 IT xanh.

**Đối chiếu full verify cuối (05/11/2026).** `phase6-clean-verify-final-final.log` đạt `BUILD SUCCESS` sau 46:58: 1092 test đơn vị, 2694 IT, 0 failure/error, 2 skipped (`SC_001_UrgentConcurrencyIT` khi chưa bật `eldercare.perf`). Đây là mốc sau cả hai fix race; các lớp đỏ sẵn của team-plan không phát sinh thêm lỗi trong lượt này.

**Sửa sau review Phase 6 (05/11/2026).**
- Đăng ký lại sau Không đi/hủy: `ActivityTripStore.participants` trước đây trả mọi dòng có `participant_status`, nên lệnh đánh giá/rời viện lấy nhầm dòng cũ `NOT_GOING` và GET hiện hai dòng cho một người. Quyết định kỹ thuật: mỗi người chỉ lấy đăng ký mới nhất (MAX id) của chuyến; dòng Không đi vẫn hiện khi người đó chưa đăng ký lại (FR-039). Test `FR_036_reRegistrationAfterNotGoingUsesTheNewRegistrationForAssessmentAndDeparture` đỏ (2 dòng) rồi xanh.
- Thứ tự khóa: `RegistrationService.cancel` khóa đăng ký rồi mới khóa buổi, ngược với `register`, `depart` (buổi rồi đăng ký), có thể deadlock và trả 500. Nay đọc `session_id` không khóa (không đổi theo thời gian), khóa buổi rồi khóa đăng ký. Test `FR_017_cancelWaitingForTheTripLockDoesNotHoldTheRegistrationLock` (giữ khóa buổi, kiểm dòng đăng ký vẫn `FOR UPDATE NOWAIT` được trong lúc lệnh hủy chờ) đỏ rồi xanh.
- Chưa sửa, chờ chủ dự án (team-plan mục 8): tầng đánh giá của người bán trú không có giường (`assessmentTarget` qua `BedAllocationApi.openAt` trả 404); phạm vi trưởng đoàn ngoài tầng chuyến (FR-031). Web chưa có form đánh giá Đạt/Không đạt, nên trên giao diện chưa đưa được người sang Được đi; luồng rời viện qua web/app chưa chạy trọn.
- Kiểm: scoped 53 kiến trúc + 76 IT xanh (`phase6-review-fix-scoped.log`); `./mvnw -o clean verify` sau hai sửa này: 1092 unit + 2696 IT, 0 lỗi, 2 skipped (`eldercare.perf`), `BUILD SUCCESS` 57:46 (`phase6-review-fix-clean-verify.log`).

**Phase 7 (US5, một phần, 05/11/2026).**
- Phạm vi đã cài (T020 một phần): `TripReturnService` (tách khỏi `ChuyenDiService` của tasks như Phase 6 đã tách `TripPreparationService`, `TripDepartureService`): `return` (FR-044, về muộn cho người Thiếu khi về sau khi chuyến Đã về FR-050), `extend` (FR-043, lý do bắt buộc, giờ mới sau giờ cũ và sau hiện tại), `transfer-leader` (FR-035a: người nhận phải là người đi cùng có vai trò Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc; trưởng đoàn cũ thành người đi cùng; lịch sử `TRIP_DUTIES` thêm khoảng mới), `finish-return` (FR-047: người Đã rời viện chưa về chuyển `MISSING_ON_RETURN`, mỗi người một `AlertRequested` `MISSING_ON_RETURN` mức Khẩn cấp gửi trưởng đoàn và Trưởng tầng; không tạo sự cố; chuyến `RETURNED`, đóng khoảng nhiệm vụ). `TripReturnOverdueStep` (bước `CARE_MINUTE`, FR-049, FR-049a): báo một lần mỗi mốc giờ về qua `trip_overdue_notified_for`, `trip_manager_notified_for`; gia hạn đổi mốc nên mốc cũ không báo. `TripLeftGroupListener` (FR-045): `ResidentStatusChanged` từ Hoạt động bên ngoài sang Điều trị tại bệnh viện hoặc Qua đời thì đăng ký đang ở ngoài chuyển `LEFT_GROUP`.
- Không thêm migration: `V014_05` đã có các cột cần dùng. Gia hạn ghi `record_event` `M04.TRIP_EXTENSION`; điểm danh về ghi `M04.TRIP_ATTENDANCE` type `RETURN`. 006 nhận khoảng mới qua `TripMedicationPreparationPort.recalculateInterval` (gia hạn: từ lúc rời thực tế tới giờ về mới; về: tới lúc về thực tế); 011, 019 qua `TripChanged` (`CHANGED`, `EXTENDED`, `RETURN_FINISHED`) và `VehicleScheduleCommandApi` (`EXTENDED`, `RETURN_FINISHED`) khi dùng xe viện.
- Quyết định kỹ thuật: điểm danh về, kết thúc điểm danh về do trưởng đoàn hoặc Trưởng tầng, không bắt lý do (FR-044, FR-047 không ghi lý do, khác FR-040 rời viện thay). Sau khi Đã về, quyền về muộn xét theo trưởng đoàn của khoảng nhiệm vụ cuối. Lỗi giá trị trả 422 theo `InvalidValueException` chung. Cảnh báo thiếu người dùng nguồn `M04.TRIP_PARTICIPANT` / id đăng ký trong `CareEventContext`; `AlertProgressRequested` `FOUND_RETURNED` dùng cùng nguồn để 007 ghi diễn biến (D cài listener).
- Chưa làm: `report-missing`, `record-missing-incident` trả 503 `CARE_SOURCE_UNAVAILABLE` vì `care` không được phụ thuộc `incident` (`M04ArchitectureRules.NO_DIRECT_HIGHER_MODULES`); cần SPI do care khai, 007 cài (đề xuất ở team-plan mục 8, chờ chủ dự án duyệt). Ghi khi mất mạng (`deviceAt`) của `return`, `finish-return` trả 503 như `depart` (001 chưa nhận thời điểm thiết bị). Đính chính, hủy ghi nhận FR-031a (`FR_031a_DinhChinhChuyenIT`, provider `M04.DIEM_DANH_CHUYEN`) để lượt sau. Báo riêng cho 005 lập lại công việc khi về chưa nối, giống `depart`. Web, app chưa có màn lệnh trở về.
- Kiểm: `BR_M04_17_ThieuNguoiIT` 8 ca đỏ trước khi cài (6 lỗi đúng lý do, 2 ca 503 xanh sẵn) rồi 8/8 xanh; scoped 55 kiến trúc + 91 IT xanh (`-Dit.forkCount=3`, 2 phút 35 giây), gồm `ApiContractIT`, `BR_M04_15`, `BR_M04_16`, `BR_M04_21`, `NFR_04_CareSchedulingIT`, `ResidentAccountFinalStatusIT`. Chưa chạy `clean verify`.

**Phase 7 lượt 2: đính chính điểm danh chuyến FR-031a (06/11/2026).**
- `TripAttendanceCorrectionProvider` (recordType `M04.TRIP_ATTENDANCE`, tên logic `M04.DIEM_DANH_CHUYEN`): chỉ Trưởng tầng (`canRecord`), tầng của chuyến do khung 000 xét qua `floorArea`. Chỉ hỗ trợ `VOID` bản ghi rời viện (b): điều kiện chuyến Đang đi và người tham gia còn Đã rời viện hoặc Thiếu khi về (chưa có bản ghi về), kiểm cả trong `allows` (trước khi lập, kể cả cách C) và lúc áp dụng. Tác động: 001 `CANCEL_DEPARTURE_RECORD` (căn cứ chuyến, chi tiết `correctionId`), người tham gia về `ELIGIBLE` nếu còn trước giờ về dự kiến (006 nhận lại khoảng dự kiến) hoặc Không đi nếu đã quá (006 `NOT_ATTENDING`), 010 `CostSourceChanged` hủy nguồn có id là bản ghi rời viện khi hoạt động có thu phí, 011 qua `TripChanged`; không còn ai rời viện thì chuyến về `SCHEDULED`, `TripChanged` `LAST_DEPARTURE_UNDONE`, lịch xe `LAST_DEPARTURE_UNDONE` (Q-240).
- Lệch kỹ thuật: khóa chống trùng của bản ghi rời viện đổi từ `TRIP_DEPARTURE:<đăng ký>` thành `TRIP_DEPARTURE:<đăng ký>:<số lần đã rời>` để rời viện lại sau Hủy ghi nhận không bị 409 `ALREADY_PROCESSED`; hai thiết bị cùng lúc vẫn bị chặn dưới khóa chuyến và UPDATE có điều kiện (`BR_M04_16_RoiVienIT` 36/36 xanh).
- Chưa làm, chờ 001 (B): đổi thời điểm thực tế (a) trả 409 `CORRECTION_NOT_SUPPORTED` vì `ResidentStatusApi` chưa có cách sửa thời điểm lịch sử trạng thái; Hủy ghi nhận bản ghi về (c) trả 503 `CARE_SOURCE_UNAVAILABLE` vì 001 chưa có lệnh đưa người đã về trở lại Hoạt động bên ngoài. Báo riêng 005 chưa nối như các lệnh chuyến khác.
- Kiểm: `FR_031a_DinhChinhChuyenIT` 4 ca đỏ (chưa có provider) rồi 4/4 xanh; scoped 63 đơn vị/kiến trúc + 145 IT xanh (gồm các IT đính chính của 000, `FR_031_DinhChinhIT`, `BR_M04_16`, `BR_M04_17`, `BR_M04_18`, `ApiContractIT`). Chưa chạy `clean verify`.

**Phase 8 (US6, một phần, 06/11/2026).**
- Đã cài: `POST /residents/{residentId}/preferences`, `POST /preferences/{id}/remove` (`PreferenceService`, `PreferenceController`; bỏ hai operation khỏi danh sách chờ của `ApiContractIT`). Lưu `simple_record` `M04.PREFERENCE` (R-15); một mục hiện hành mỗi (người cao tuổi, nhóm, nguồn) qua `unique_key`; nhóm và nguồn đã có mục hiện hành thì 409 `PREFERENCE_REPLACEMENT_CONFIRMATION_REQUIRED` nêu mục cũ, `confirmReplacement` thì mục cũ `REPLACED`; bỏ cần lý do, mục `REMOVED` giữ người, lúc, lý do. Nguồn kiểm qua `OpinionSourceApi` của 001 (người thân không có quan hệ Hiệu lực → 422 `INVALID_OPINION_SOURCE`). Người ghi: Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc; quyền dòng "Hoạt động, ngoài viện" (`ACTIVITY_OUTING`, Phụ lục 27 dòng UC-29, 30), phạm vi `M04.PREFERENCE` theo người cao tuổi.
- FR-052: `RegistrationEligibility` thêm cảnh báo `DISLIKED_INTEREST` cho mỗi mục "không thích" hiện hành khớp nhóm sở thích của loại hoạt động, nêu nguồn; mục "thích" của chính người cao tuổi không che mục của nguồn khác. Mã cảnh báo mới ghi ở glossary 20.10 (hợp đồng không liệt kê mã cảnh báo trong `unmetConditions`). `SessionAttendanceFixture` đổi `typeId` giả thành loại hoạt động có thật vì đăng ký đọc nhóm của loại.
- Chưa làm, chờ duyệt (team-plan mục 8): `GET /residents/{residentId}/preferences` và `GET /residents/{residentId}/wellbeing-profile` trong api-014 chỉ có `200 OK`, chưa có schema phản hồi; hồ sơ tinh thần còn cần nguồn đọc cảnh báo, sự cố của 007. Chuỗi cô lập FR-054 → FR-056 (job 00:00, cảnh báo `ISOLATION_RISK`): ngày tính loại ngày "mang dấu nghi nhiễm" bất kỳ lúc nào trong ngày, nhưng `ActivityHealthGatePort.suspectedInfection` chỉ trả theo một thời điểm; cần thêm hàm theo khoảng ở port (007 cài). Web sở thích chưa làm (bố cục cần duyệt).
- Kiểm: `FR_051_SoThichIT` 4 ca đỏ (404, chưa có endpoint) rồi 4/4 xanh; scoped 70 đơn vị/kiến trúc + 101 IT: 2 ca `BR_M04_18_SuKienTinhPhiIT` đỏ do fixture `typeId` giả, sửa fixture rồi `BR_M04_18`, `FR_031_DinhChinhIT`, `T014_SessionAttendanceSourcesIT` 13/13 xanh. Chưa chạy `clean verify`.

**Phase 9 (US7, một phần, 06/11/2026).**
- Đã cài: `POST /quality-checks/shifts/{shiftId}` (lập danh sách hoặc thêm mục) và `POST /quality-check-items/{id}/result` (`QualityCheckService`, `QualityCheckController`; bỏ hai operation khỏi danh sách chờ của `ApiContractIT`). Người kiểm tra (FR-066): Trưởng tầng được giao theo `DutyApi.assignedFloorManager`; Người phụ trách ca theo `DutyApi.shiftLead` khi `DutyApi.shiftHasNoFloorManager(shiftId)` (nguồn 008 đã khai cho 014 FR-066, Q-262); quyền theo nhiệm vụ của 002 (`DutyPermissions` dòng `QUALITY_CHECK`). Lệnh chỉ trong thời gian ca (`OUTSIDE_SHIFT_TIME`).
- Công việc đủ điều kiện (FR-060): `care_task` của ca và tầng ở Hoàn thành/Hoàn thành trễ, `result_performed_at` trong ca; loại công việc làm lại (`REWORK`), công việc người ghi kết quả là Trưởng tầng được giao, công việc của Người phụ trách ca tại lúc hoàn thành (Q-270) khi ca không có Trưởng tầng, kết quả đã Hủy ghi nhận (`CorrectionApi.current` `M04.CARE_RESULT`). Lập danh sách chặn khi số mục nhỏ hơn trần(CFG-M04-11 × n), tối thiểu 1 (`SAMPLE_RATIO_NOT_MET`); thêm mục sau không xét lại tỷ lệ; mỗi công việc một lần (`TASK_NOT_ELIGIBLE`). Người thực hiện không được báo khi được chọn.
- Kết quả (FR-061, FR-062, FR-064): `PASS`; `FAIL` cần ghi chú (422) và công việc làm lại (409 `REWORK_TASK_REQUIRED`), giờ dự kiến trong ca; lệnh gọi `CareTaskService.rework` của 005 trong cùng transaction rồi xác nhận mục, báo người thực hiện (`QualityCheckFailedNotice`); `NOT_CHECKABLE` cần lý do. Người phụ trách ca không ghi cho công việc của chính mình (`SELF_CHECK_NOT_ALLOWED`). `QualityCheckResultSource` cài `QualityCheckPort` cho 005; trong test có `FakeM04Config`, bản giả `FakeQualityChecks` là `@Primary` nên 005 đọc bản giả, test kiểm nguồn thật riêng (thay bản giả thuộc T028).
- Chưa làm: `GET /quality-checks/shifts/{shiftId}` chỉ có `200 OK`, chờ schema phản hồi (team-plan mục 8); việc theo phút nhắc ở mốc CFG-M09-04, đóng `OVERDUE` khi hết ca và báo Quản lý viện, ghi ca không có danh sách (FR-059a, FR-063); mục tự `NOT_CHECKABLE` khi công việc bị Hủy ghi nhận sau khi chọn (FR-062); đính chính kết quả `M04.KET_QUA_KIEM_TRA`; báo cáo FR-065; web.
- Kiểm: `BR_M04_23_KiemTraChatLuongIT` 5 ca đỏ (404, chưa có endpoint) rồi xanh; thêm ca ngoại lệ Người phụ trách ca sau khi cài (lần đầu 403 do fixture thiếu phân công `SHIFT_LEAD`, sửa fixture rồi xanh), tổng 6/6. `LayerArchTest.noNestedRecordsInPersistence` bắt record lồng trong store, chuyển `CompletedTask` sang domain. Scoped 65 đơn vị/kiến trúc + 167 IT xanh (gồm `BR_M04_01_ShiftTaskPlanningIT`, `BR_M04_14`, `FR_025`, `FR_034`, `DutyPermissionIT`, `ApiContractIT`). Chưa chạy `clean verify`.

**Phase 9 lượt 2 (06/11/2026): việc theo phút và Hủy ghi nhận.**
- `QualityCheckClockStep` (bước `CARE_MINUTE`, chỉ nhắc, chuyển trạng thái theo giờ, báo; không lập danh sách, mục, công việc làm lại): xét các cặp (ca, tầng) có công việc hoàn thành từ đầu ngày hôm qua theo giờ nghiệp vụ (giới hạn kỹ thuật cho ca qua nửa đêm và chạy bù; dừng lâu hơn thì ca cũ không được nhắc hay ghi không có danh sách). Trong khoảng CFG-M09-04 trước kết ca: nhắc người kiểm tra (Trưởng tầng được giao, thêm Người phụ trách ca khi ca không có Trưởng tầng) một lần khi chưa có danh sách mà có công việc đủ điều kiện hoặc số mục nhỏ hơn tỷ lệ (FR-059a). Hết ca: mục `PENDING` thành `OVERDUE` (xác nhận, không tính tỷ lệ Đạt), danh sách `CLOSED`, báo Quản lý viện một lần mỗi danh sách; ca có công việc đủ điều kiện mà không có danh sách ghi `reminder_marker` `M04.QUALITY_NO_CHECKLIST` kèm lý do `NO_INSPECTOR` (không Trưởng tầng trong ca, không Người phụ trách ca) hoặc `NOT_CREATED`, báo Quản lý viện một lần (FR-063). `QualityCheckService.status` tính số công việc đủ điều kiện và người kiểm tra không theo người dùng hiện tại.
- FR-062: `CareResultCorrectionProvider` (005) khi Hủy ghi nhận kết quả gọi `QualityCheckTaskVoids.resultVoided` trong cùng transaction: mục đang chờ thành `NOT_CHECKABLE` với lý do "Công việc đã hủy ghi nhận". Test dùng `@Transactional(isolation = READ_COMMITTED)` như `BR_M04_14_CareResultCorrectionIT` vì đính chính kết quả cần isolation này.
- Lệch kỹ thuật: `TimeArchTest.XIII_noDurationConstantsInModuleLogic` cấm hằng `Duration` trong logic module nên giới hạn tìm lùi tính theo ngày lịch, không phải tham số nghiệp vụ.
- Kiểm: 4 ca mới trong `BR_M04_23_KiemTraChatLuongIT` đỏ (bước rỗng; Hủy ghi nhận lần đầu 503 do test chạy trong transaction mặc định, sửa test sang READ_COMMITTED rồi đỏ đúng lý do) rồi xanh, tổng 10/10; `BR_M04_14_CareResultCorrectionProviderTest` 2/2 sau khi thêm tham số; scoped 70 đơn vị/kiến trúc + 178 IT xanh (gồm `NFR_04_CareSchedulingIT`, `BR_M04_14_CareResultCorrectionIT`, `CorrectionIT`). Chưa chạy `clean verify`. Còn: GET chờ schema, đính chính kết quả kiểm tra `M04.KET_QUA_KIEM_TRA`, báo cáo FR-065, web.