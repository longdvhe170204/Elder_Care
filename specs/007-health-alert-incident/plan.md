# Implementation Plan: Sức khỏe, cảnh báo và sự cố

**Branch**: `007-health-alert-incident` | **Cập nhật**: 2026-10-22 | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-health-alert-incident/spec.md`

## Summary

Feature 007 gồm module M06 – Sức khỏe (mục 10) và M05 – Sự cố và cảnh báo (mục 9); UC-16 phần khởi phát từ sự cố, UC-32 → UC-38, UC-83, UC-84. Nó cung cấp:
1. Danh mục chỉ số với khoảng hợp lệ vật lý ở CFG-M06-03; lịch đo của Bác sĩ làm căn cứ lập công việc đo, không sinh công việc (mục A).
2. Ghi chỉ số (từ công việc đo, ngoài lịch, ngoại tuyến), so ngưỡng hai mức theo thành phần nặng nhất, đo lại giá trị nguy hiểm, đính chính (mục B).
3. Ngưỡng mặc định và cá nhân theo phiên bản; nhắc thiếu ngưỡng cá nhân (mục C).
4. Cảnh báo từ mọi nguồn: khóa loại, gộp (DBR-18), hạn tiếp nhận theo mức, leo thang ba cấp, nhắc khi đã leo thang tối đa, báo lặp lại, tự đóng khi nguồn đã xử lý, chuyển người phụ trách theo bàn giao (mục D).
5. Quy tắc xu hướng: sụt cân, bỏ lỡ lần đo theo lịch, mất ngủ kéo dài, từ chối thuốc (mục E).
6. Sự cố ba mức: ghi nhận (kể cả ngoài phạm vi), diễn biến, đổi mức, theo dõi, đóng, hủy và đổi loại bằng đính chính (mục F).
   *(Bổ sung 2026-10-11, FR-045a, Q-339)* Lệnh "Giao người xử lý" với khóa riêng `SU_CO, GIAO`, kiểm phạm vi của người giao và người được giao; lịch sử ở `phan_cong_su_co`. *(R-15: `phan_cong_su_co` → `su_kien_chung`)*
7. Quy trình khẩn cấp: thẻ thông tin khẩn cấp, đối chiếu nguyện vọng trước biện pháp hồi sức, thông báo song song, chuyển viện kèm bản tóm tắt (mục G).
8. Nguy kịch: dấu nguy kịch, cảnh báo không gộp, xác nhận lại nguyện vọng, lựa chọn thực hiện (mục G1).
9. Sự cố ngã và khoảng theo dõi sau ngã (mục H); sự cố lây nhiễm, tra cứu truy vết, danh sách tiếp xúc, khoanh vùng (mục I).
10. Giấy phép hoạt động của cơ sở làm căn cứ cho BR-M06-05; ghi khám và điều trị (mục J); dữ liệu cho 002, 003, 004, 005, 008, 012, 016 (mục K).
11. *(Bổ sung 2026-10-18, Q-369)* Nút SOS của người cao tuổi: tạo cảnh báo Khẩn cấp từ thiết bị đã gắn, điều kiện gửi, người nhận thêm khi ở Hoạt động bên ngoài, trạng thái cho người cao tuổi (FR-047d → FR-047g).
12. *(Bổ sung 2026-10-22, Q-373 → Q-376, Q-383, Q-384)* Vị trí GPS kèm SOS và nút hỗ trợ khẩn cấp; nút hỗ trợ khẩn cấp của nhân viên (UC-99); mốc "Đã tới nơi"; nút "Gọi 115"; lệnh Chuyển viện hiện nguyện vọng khi điều trị, ghi người đi kèm, đính kèm giấy tờ bệnh viện và dựng phiếu tóm tắt chuyển viện PDF (FR-047h → FR-047k, FR-049, FR-050, FR-050h).

Hướng kỹ thuật:
- Hai gói Modulith `health` và `incident`, một chiều `incident → health`. Kết quả so ngưỡng sang `incident` bằng sự kiện `ChiSoDaXacNhan` sau commit (research S-01).
- Bản ghi chỉ số là nhóm 3 có bản nháp: `CHO_DO_LAI` chưa có `thoi_diem_xac_nhan`; trigger cho đặt đúng một lần (S-06).
- Gộp cảnh báo tuần tự theo người bằng khóa `theo_doi_nct`, lưới an toàn là cột sinh `khoa_mo` với `UNIQUE (nct_id, khoa_mo)` (S-03, DBR-18). Mỗi phát sinh là một dòng `nguon_canh_bao`; dòng này dùng cho số lần, báo lặp lại 24 giờ trượt và tự đóng theo nguồn (S-08, S-09). *(R-15: `theo_doi_nct` → `trang_thai_nct`; `nguon_canh_bao` → `su_kien_chung`)*
- Sự cố tách thành `su_co` (nội dung nhóm 3, không `UPDATE`) và `su_co_xu_ly` (trạng thái nhóm 2) (S-11). *(R-15: `su_co_xu_ly` → `su_co`)*
- Thẻ thông tin khẩn cấp dựng từ 001, 003, 006, 012 trong lệnh kích hoạt; bản chụp lưu cùng sự cố; chuyển viện gọi `TrangThaiNguoiCaoTuoiApi.thucHien(CHUYEN_VIEN)` của 001 trong cùng transaction (S-12).
- Một job theo phút mỗi module cho hạn tiếp nhận, leo thang (một cấp mỗi lần, FR-084), chờ đo lại, chuyển trạng thái lịch đo và ngưỡng; một job hằng ngày nhắc thiếu ngưỡng (S-21).
- 008, 009, 012, 014 làm plan sau; dữ liệu ca, Bác sĩ trực, người liên hệ chính, lượt thăm, điểm danh, nhật ký gửi lấy qua SPI khai báo ở `incident.api` (S-02).
- Mọi thông báo là sự kiện ghi cùng transaction qua outbox, mang mức và nhóm người nhận (S-19).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 007 không sinh công việc đo, lịch đo theo dõi, sự cố, danh sách tiếp xúc, yêu cầu gọi điện hay lượt vắng; không đề xuất nâng mức hay người tiếp xúc;
- hệ thống chỉ: tính chuỗi mốc đo để kiểm căn cứ và hiển thị; so ngưỡng; tạo, gộp, leo thang, nhắc, đóng cảnh báo theo quy tắc; chuyển trạng thái theo giờ; kiểm tra và chặn; tác động kèm lệnh của actor (bản tóm tắt, khoảng theo dõi sau ngã, tác động trạng thái cuối, hủy công việc khi nguồn bị hủy).

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": Q-352, Q-354)

- **Mã trường hợp Q-354** (FR-047, FR-047c): mọi yêu cầu thông báo tới người thân của 007 khai `doiTuongThongTin = VE_NGUOI_CAO_TUOI` (009 FR-001a) và mang mã trường hợp: `KHAN_CAP_NGUOI_LIEN_HE_CHINH` (FR-047, FR-050b c), `DANH_SACH_TIEP_XUC` (FR-062), `XAC_NHAN_LAI_NGUYEN_VONG` (FR-050b d, nhắc gọi của 009), `CHUYEN_VIEN` (thông báo do 001 phát khi lệnh Chuyển viện của FR-049 chạy). 009 chỉ dùng mã với người nhận nằm ngoài phạm vi bản đồng ý và lưu vào bản ghi gửi (research S-19).
- **Phần chung và phần sức khỏe của sự cố** (FR-082, Q-352): `SucKhoeCongNguoiThanApi.suCo` và `BanTinSucKhoeApi.choBanTin` trả mỗi sự cố thành `phanChung` và `phanSucKhoe`; phần sức khỏe chỉ có khi người thân thuộc phạm vi xem sức khỏe của bản đồng ý (với `choBanTin`, 012 lọc theo người nhận). Danh sách trường ở research S-19a.
- **Nguyện vọng mới khi xác nhận lại** (FR-050c): thân lệnh theo api-001 sau cập nhật 2026-10-18 của 001 (nhận định gắn với phiếu, người ký theo FR-023c, ý kiến khác có nguồn).
- Không đổi bảng; điểm báo lại 8 → 10 bên dưới.

### Cập nhật 2026-10-18, lượt 3 (spec mục "Cập nhật 2026-10-18, lượt 3": Q-369)

- **Nút SOS** (FR-026 (d), FR-047d → FR-047g, US12): `POST /sos` ở `incident.canhbao`, không qua phiên; xác định người cao tuổi bằng token thiết bị qua `ThietBiGanApi.xacDinh` của 002. Mỗi lần bấm là một dòng `lan_bam_sos` (nhóm 3) và một dòng `nguon_canh_bao` loại `SOS`; cảnh báo Khẩn cấp loại `NCT_YEU_CAU_TRO_GIUP`, khóa loại `SOS` theo người, gộp theo đường tạo cảnh báo sẵn có (research S-23). *(R-15: `lan_bam_sos`, `nguon_canh_bao` → `su_kien_chung`)*
- **Điều kiện gửi**: trạng thái người cao tuổi qua `NguoiCaoTuoiApi.trangThai` (001); người bán trú qua `CoMatBanTruApi.theoNgay` (005, đã có); chuyến đang diễn ra, trưởng đoàn, người đi cùng qua SPI mới `ChuyenDiSosPort` (014 cài, 014 FR-068a).
- **Trạng thái cho người cao tuổi**: `GET /sos/current` (token thiết bị), app hỏi lại mỗi 5 giây khi màn trạng thái đang mở; lỗi từ chối trả kèm số liên hệ của viện và 115 từ `SoLienHeVienApi`.
- FR-081: `CanhBaoThongKeApi.canhBao` thêm `nguon` để 016 tách SOS. Thêm một bảng, một SPI; không đổi vòng đời cảnh báo.

### Cập nhật 2026-10-22 (spec mục "Cập nhật 2026-10-22": Q-373 → Q-376, Q-383, Q-384)

- **Vị trí GPS** (FR-047h, BR-M05-28, CFG-M05-09): bảng `vi_tri_canh_bao` (nhóm 3), mỗi lần gửi một dòng; `POST /sos/{id}/location` (token thiết bị) và `POST /emergency-assist/{id}/location` (phiên); nhận khi cảnh báo đang mở và chưa có mốc tới nơi, ngược lại 409 để app dừng gửi. Trạng thái trả cho app có `guiViTri` và `chuKyGuiViTriGiay` (CFG-M05-09). Dấu "có thể đã cũ" tính lúc đọc (không lưu). Vị trí không vào nội dung thông báo, không có ở API cho 012 phần người thân (research S-24). *(R-15: `vi_tri_canh_bao` → `su_kien_chung`)*
- **Nút hỗ trợ khẩn cấp** (FR-047i, BR-M05-29, UC-99, quyền `NUT_HO_TRO_KHAN_CAP` = `EMERGENCY_ASSIST_BUTTON` của 002 V002_09): `POST /emergency-assist` tạo hoặc gộp cảnh báo Khẩn cấp loại `NV_YEU_CAU_HO_TRO`, khóa theo người bấm; `canh_bao.nct_id` được trống chỉ với loại này và đặt được một lần khi người bấm chọn người cao tuổi; bảng `lan_bam_ho_tro` (nhóm 3). "Đang trong ca" và tầng của người bấm lấy qua `NhanSuCanhBaoPort` (008 cài, thêm ba hàm); người nhận là nhóm `OnDutyDoctor`, `ShiftLead(tầng)`, `OnShiftStaff(tầng)` của `common.notification`, chưa có tầng thì `ShiftLead` của mọi tầng đang có ca (research S-24). *(R-15: `lan_bam_ho_tro` → `su_kien_chung`)*
- **Mốc "Đã tới nơi"** (FR-047j, BR-M05-30): lệnh `POST /alerts/{id}/mark-arrived` của người phụ trách; cột `da_toi_noi_luc`, `da_toi_noi_boi_id`; cờ `khong_ghi_moc_toi_noi` đặt khi đóng; `GET /sos/current` và `GET /emergency-assist/current` trả `DA_GUI`, `DA_TIEP_NHAN` ("đang tới"), `DA_TOI`.
- **Gọi 115** (FR-047k, FR-048b, BR-M05-31): bảng `luot_goi_115` (nhóm 3, kết quả đặt một lần); `GET /alerts/{id}/call-115-info hoặc GET /incidents/{id}/call-115-info` trả thông tin đọc cho tổng đài (CFG-M11-11 qua `ThamSoApi`, vị trí, nhận dạng; dị ứng, bệnh nền theo quyền xem sức khỏe). Lượt "Đã gọi được" thêm diễn biến `GOI_CAP_CUU` vào sự cố liên kết; lượt trên cảnh báo trước khi chuyển sự cố được chép vào sự cố lúc chuyển (plan điểm báo lại 12). *(R-15: `luot_goi_115` → `su_kien_chung`)*
- **Chuyển viện và phiếu tóm tắt** (FR-049, FR-050, FR-050h, BR-M05-32): `GET /incidents/{id}/transfer-out/info` hiện nguyện vọng khi điều trị (`NguyenVongApi.dieuTriHieuLuc` của 001) và nguyện vọng cuối đời; thân lệnh thêm `nguoiDiKem`, `daLienLacNguoiCanBao`, `lyDoKhongHoiTruoc`, `tepDinhKemIds` (chuyển nguyên cho `TrangThaiNguoiCaoTuoiApi.thucHien(CHUYEN_VIEN)`, 001 research H-18). Phiếu PDF dựng trong lệnh bằng thành phần dùng chung `common.bieumau` (OpenPDF, font Be Vietnam Pro nhúng), lưu S3 qua `TepApi`; in lại đóng dấu "Bản in lại" lúc tải, mỗi lần in một dòng `lan_in_phieu_chuyen_vien` (research S-25). *(R-15: `lan_in_phieu_chuyen_vien` → hành động `nhat_ky`)*
- **Báo cáo** (FR-081): `CanhBaoBaoCaoView` thêm `nguon` = `NUT_HO_TRO`, `daToiNoiLuc`, `khongGhiMocToiNoi`.
- **Thư viện mới**: OpenPDF (dựng, đóng dấu PDF) cho backend; web hiện vị trí ngoài viện bằng Leaflet với lớp bản đồ OpenStreetMap và liên kết chỉ đường; app dùng gói định vị khi chạy nền trước (`geolocator`) và mở ứng dụng bản đồ của máy (`url_launcher`) khi `mobile/` được mở. Lý do, phương án ở research S-24, S-25; thêm vào `pom.xml`, `package.json` cần chủ dự án duyệt (docs/team-plan.md mục 3).

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 006: Spring Boot 4.1.x, Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock, Spring Security 7. Web: React 19, TanStack Query. App nhân viên: Flutter, drift (hàng đợi ngoại tuyến, 000 R-13). App người thân: Flutter. *(Sửa 2026-10-22)* Thêm OpenPDF (backend, phiếu tóm tắt chuyển viện; dùng chung qua `common.bieumau`), Leaflet (web, bản đồ vị trí ngoài viện), `geolocator`, `url_launcher` (app, khi mở `mobile/`); research S-24, S-25.

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test bảng cho mười hàm thuần: `SoNguong`, `KiemTraNguong`, `KhoaLoai`, `DemLap24h`, `SutCan`, `DemDemMatNgu`, `MocTheoDoiSauNga`, `ChuoiMocDo`, `ChuoiLeoThang`, chọn phiên bản ngưỡng áp dụng.
- Test tích hợp MySQL cho cột sinh `UNIQUE` và trigger nhóm 3 có bản nháp.
- Test đồng thời: 10 phát sinh cùng khóa (SC-003); đo lại và job hết hạn; hai lần kích hoạt khẩn cấp; hai lần tiếp nhận.
- Đồng hồ giả lập cho mọi CFG-M05, CFG-M06 theo thời gian, gồm chạy bù (FR-084).
- Bản giả cho mọi SPI 007 khai báo (research S-22).

**Target Platform**: Như 000.
- Web: mọi màn của 007, gồm danh mục, ngưỡng, lịch đo, chỉ số và lịch sử, cảnh báo, sự cố, thẻ khẩn cấp và bản tóm tắt in được, nguy kịch, tra cứu và danh sách tiếp xúc, khoanh vùng, giấy phép cơ sở, khám điều trị; dashboard dùng dữ liệu của 016.
- App nhân viên: ghi chỉ số (có ngoại tuyến) và đo lại; nhận, tiếp nhận, xử lý cảnh báo; ghi sự cố, kích hoạt khẩn cấp (bắt buộc trực tuyến), thẻ khẩn cấp, diễn biến; dấu nguy kịch, ghi kết quả xác nhận lại.
- App người thân: xem sự cố, chỉ số khi có bản đồng ý (qua 012); nhận thông báo khẩn cấp, tiếp xúc, nguy kịch (qua 009).

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Ghi chỉ số ≤ 2 giây p95 (SC-002, NFR-02): đọc CFG-M06-03 một lần, một truy vấn phiên bản ngưỡng theo chỉ mục `(nct_id, chi_so_id, hieu_luc_tu)`, thêm bản ghi.
- Cảnh báo tới người phụ trách ≤ 10 giây (SC-002): listener outbox ngay sau commit; gộp khóa một dòng người.
- Thẻ khẩn cấp ≤ 3 giây (SC-005): sáu lời đọc song song có giới hạn thời gian; thông báo khẩn cấp ghi cùng transaction, 009 gửi song song (NFR-03).
- Job theo phút ≤ 1 phút (SC-004): khoảng vài chục cảnh báo đang mở ở quy mô 300 người; chỉ mục `(trang_thai, han_tiep_nhan)`.

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI).
- Bản ghi chỉ số sau xác nhận, nội dung và diễn biến sự cố, bản chụp thẻ, xác nhận đối chiếu, bản tóm tắt, kết quả xác nhận lại, lựa chọn, khám điều trị, lịch sử cảnh báo không bị `UPDATE`, `DELETE` (constitution XII, DBR-19).
- Kích hoạt khẩn cấp và ghi sự cố bắt buộc trực tuyến (8.6, Q-01).
- Nhật ký trong transaction (000 FR-047). CFG đọc lúc chạy (constitution XIII).
- Không tự sinh bản ghi nghiệp vụ (000 FR-010a).

**Scale/Scope**:
- 300 người cao tuổi; khoảng 2.000 bản ghi chỉ số mỗi ngày (0,7 triệu mỗi năm); vài chục cảnh báo và vài sự cố mỗi ngày.
- 8 thực thể có vòng đời (lịch đo, phiên bản ngưỡng, cảnh báo, sự cố, dấu nguy kịch, khoảng theo dõi sau ngã, danh sách tiếp xúc, khoanh vùng) và trạng thái xác nhận của bản ghi chỉ số; 42 bảng.
- 3 job (theo phút M06, theo phút M05, hằng ngày M06).
- 59 đường dẫn, 72 thao tác REST ([contracts/api-007.yaml](contracts/api-007.yaml)).
- 4 SPI 007 khai báo; 14 SPI 007 cài cho module thấp hơn; 10 giao diện 007 cung cấp.
- *(Bổ sung 2026-10-22)* Thêm 4 bảng (`lan_bam_ho_tro`, `vi_tri_canh_bao`, `luot_goi_115`, `lan_in_phieu_chuyen_vien`), 12 thao tác REST, 3 hàm của `NhanSuCanhBaoPort`. *(R-15: `lan_bam_ho_tro`, `vi_tri_canh_bao`, `luot_goi_115` → `su_kien_chung`; `lan_in_phieu_chuyen_vien` → hành động `nhat_ky`)*

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR. Data-model mục 15 là bảng truy vết DBR-18, 19, 23, 25, 34, BR-M05-01 → 26, BR-M06-01 → 06 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Danh mục chỉ số, loại sự cố, quy tắc xu hướng, loại diễn biến theo mẫu nhóm 1. Lịch đo, ngưỡng, cảnh báo, sự cố, dấu nguy kịch, danh sách tiếp xúc, khoanh vùng chỉ có lệnh. Bản ghi chỉ số, diễn biến, khám chỉ thêm và đính chính |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 4 → 10 khớp bảng FR-006, FR-022, FR-029, FR-045, FR-050a, FR-061, FR-064. Mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M05-01 → 08, CFG-M06-01 → 04, CFG-M09-04, CFG-M04-06, CFG-M13-01 đọc từ `ThamSoApi`. Khoảng hợp lệ vật lý ở CFG-M06-03 (S-04). Chu kỳ job một phút, 00:15 là thông số kỹ thuật (Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Khóa quyền theo bảng FR-080 (S-20); 19.3 cho Trưởng tầng xem ngưỡng (Q-279). Quản lý viện không xử lý (Q-71). Không thêm vai trò |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ. Mức các thông báo spec chưa nêu: plan áp nguyên tắc 3 bậc và báo lại (điểm 5). Ba chỗ spec mâu thuẫn được chọn theo FR mới hơn và báo lại (điểm 1 → 3) |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-016 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 001, 005, 006 được bổ sung (mục "Đồng bộ"), không đổi hành vi đã thiết kế |
| X. Kiến trúc | Đạt | Một khối; gói `incident`, `health`; module khác chỉ dùng `incident.api`, `health.api`. Không có vòng phụ thuộc (S-02). REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | So ngưỡng, gộp, leo thang, chặn hồi sức, quyền ngoài phạm vi, kiểm tra giấy phép ở backend. Mỗi lệnh có `POST …/{id}/<lenh>` riêng; chuyển viện dùng lệnh của 001 |
| XII. Dữ liệu | Đạt | DBR-18, 19, 34 bằng `UNIQUE` cột sinh và trigger; nhóm 3 theo mẫu 000; nhật ký trong transaction; `Clock`; `thoi_diem_thiet_bi` cho ngoại tuyến (DBR-25) |
| XIII. Tham số | Đạt | Như V. Đổi khoảng vật lý, ngưỡng không đổi kết quả so đã lưu (FR-002, FR-023) |
| XIV. Ngoại tuyến, thông báo | Đạt | Ghi chỉ số ngoại tuyến theo Q-01 qua khung 002; sự cố, kích hoạt khẩn cấp bắt buộc trực tuyến. Mọi thông báo là sự kiện tới 009, có mức (S-19) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 007 có test (data-model mục 15). BR-M15-20 cho ghi chỉ số ngoại tuyến có thêm test Flutter |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có sáu bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Hai gói `incident`, `health` cho một feature.
2. Bảng `chi_so_nct`, `theo_doi_nct` làm đích khóa theo người. *(R-15: `chi_so_nct`, `theo_doi_nct` → `trang_thai_nct`)*
3. `nguon_canh_bao` một dòng mỗi phát sinh. *(R-15: `nguon_canh_bao` → `su_kien_chung`)*
4. Tách `su_co` và `su_co_xu_ly`. *(R-15: `su_co_xu_ly` → `su_co`)*
5. Bốn SPI khai báo ở `incident.api` cho 008, 009, 012, 014.
6. Ba job và bảng `lan_chay_m05`, `lan_chay_m06`. *(R-15: `lan_chay_m05`, `lan_chay_m06` → `moc_da_nhac`)*

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 007 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 người, 60 người dùng đồng thời giờ giao ca; chỉ mục ở data-model mục 5, 7 |
| NFR-02 Hiệu năng | Có | Ghi chỉ số một truy vấn ngưỡng; thẻ khẩn cấp đọc song song |
| NFR-03 Thông báo khẩn cấp | Có | Sự kiện ghi cùng transaction của lệnh tạo sự cố, cảnh báo Khẩn cấp, dấu nguy kịch; 009 gửi song song mọi kênh |
| NFR-04 Tác vụ định kỳ | Có | Job theo phút; khóa dòng, `UNIQUE` lần nhắc, `su_kien_canh_bao_da_nhan` chống trùng; chạy bù một cấp mỗi lần *(R-15: `su_kien_canh_bao_da_nhan` → `moc_da_nhac`)* |
| NFR-05 Khả dụng | Có | Chạy bù khi khởi động (FR-084); ghi chỉ số ngoại tuyến |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL |
| NFR-07 Lưu trữ | Có | `LuuTruProvider` theo hồ sơ hoặc không gắn hồ sơ (data-model mục 14) |
| NFR-08 Bảo mật | Có | Chỉ số, sự cố, khám, thẻ mang `SUC_KHOE`; người ngoài phạm vi chỉ thấy nhận dạng; lần người thân xem ghi nhật ký |
| NFR-09 Thời gian | Có | `Clock`; 24 giờ trượt, khoảng truy vết, hạn đo lại tính trên thời điểm tuyệt đối; đêm theo ngày dương lịch Asia/Ho_Chi_Minh |
| NFR-10 Thiết bị | Có | App nhân viên một tay, nút kích hoạt khẩn cấp lớn; web đủ chức năng, bản tóm tắt in được |
| NFR-11 Ngôn ngữ | Có | Ngày giờ dd/MM/yyyy HH:mm ở client; lỗi chặn nêu khoảng hợp lệ, nguyện vọng, điều kiện chưa thỏa |
| NFR-12 Cấu hình | Có | Mọi CFG-M05, CFG-M06 đổi qua tham số; danh mục qua màn cấu hình |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả bật tắt được |

## Project Structure

### Documentation (this feature)

```text
specs/007-health-alert-incident/
├── plan.md                      # File này
├── research.md                  # Phase 0: S-01 → S-22
├── data-model.md                # Phase 1: bảng, bảng chuyển, bảo đảm ở cơ sở dữ liệu, truy vết
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-007.yaml             # OpenAPI 3.1: danh mục, lịch đo, chỉ số, ngưỡng, cảnh báo, sự cố, khẩn cấp, nguy kịch, lây nhiễm, giấy phép, khám
│   └── module-interfaces.md     # Giao diện Java: 007 cung cấp / 007 cần; sự kiện; bean trạng thái
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/eldercare/backend/health/
├── api/           # ChiSoApi, LichDoApi, SoLienHeVienApi, ChiSoThongKeApi, sự kiện
├── danhmuc/       {internal, web}   # chỉ số, thành phần
├── lichdo/        {internal, web}   # lịch đo, ChuoiMocDo, NguonCongViecPort LAN_DO
├── chiso/         {internal, web}   # ghi, SoNguong, đo lại, ChiSoPort, NgoaiTuyenHandler, đính chính
├── nguong/        {internal, web}   # phiên bản, KiemTraNguong, nhắc thiếu ngưỡng
├── giayphep/      {internal, web}   # giấy phép cơ sở, GiayPhepCoSoApi, số liên hệ
├── kham/          {internal, web}
├── trangthai/     {internal}        # bean ThamGiaChuyenTrangThai
└── lich/                            # ViecTheoPhutM06, ViecHangNgayM06

backend/src/main/java/com/eldercare/backend/incident/
├── api/           # CanhBaoBanGiaoApi, SuCoLenhApi, SuCoApi, KhoanhVungApi, SucKhoeCongNguoiThanApi,
│                  # CanhBaoThongKeApi, SPI (NhanSuCanhBaoPort, NguoiThanKhanCapPort, DiemDanhTruyVetPort, NhatKyThongBaoPort), sự kiện
├── canhbao/       {internal, web}   # KhoaLoai, gộp, vòng đời, ChuoiLeoThang, DemLap24h, listener yêu cầu, CanhBaoDocPort
├── xuhuong/       {internal, web}   # quy tắc, SutCan, DemDemMatNgu, bỏ lỡ lần đo
├── suco/          {internal, web}   # sự cố, diễn biến, đổi mức, provider đính chính
├── khancap/       {internal, web}   # TheKhanCapService, đối chiếu nguyện vọng, chuyển viện, bản tóm tắt
├── nguykich/      {internal, web}
├── nga/           {internal}        # khoảng theo dõi, NguonCongViecPort THEO_DOI_SAU_NGA
├── laynhiem/      {internal, web}   # tra cứu truy vết, danh sách tiếp xúc, khoanh vùng, TinhTrangLayNhiemChoPhanBo
├── trangthai/     {internal}        # bean ThamGiaChuyenTrangThai, MucViecSauQuaDoiPort
└── lich/                            # ViecTheoPhutM05

frontend/src/tinh-nang/
├── danh-muc-suc-khoe/       # chỉ số, loại sự cố, quy tắc xu hướng, loại diễn biến, số liên hệ
├── nguong-lich-do/
├── chi-so/                  # ghi, đo lại, lịch sử kèm ngưỡng áp dụng
├── canh-bao/                # danh sách theo phạm vi, chi tiết, lệnh theo lenhDuocPhep
├── su-co/                   # ghi nhận, hồ sơ, diễn biến, thẻ khẩn cấp, bản tóm tắt in
├── nguy-kich/
├── lay-nhiem/               # tra cứu, danh sách tiếp xúc, khoanh vùng, người thân cần liên hệ (Hành chính)
└── giay-phep-kham/

mobile/lib/nhan-vien/
├── ghi-chi-so/              # ngoại tuyến, đo lại, kết quả so
├── canh-bao/                # nhận, tiếp nhận, xử lý
├── su-co/                   # ghi nhận, kích hoạt khẩn cấp (chặn khi mất kết nối), thẻ khẩn cấp, diễn biến
├── nguy-kich/
└── dong-bo/                 # kết quả đồng bộ chỉ số (BR-M15-20)

mobile/lib/family/health/   # sự cố, chỉ số khi có bản đồng ý (dữ liệu qua 012)
```

**Structure Decision**: Theo R-02 của 000. Feature 007 nằm ở hai module `backend/…/health` và `backend/…/incident`, mỗi module có gói `api`, gói `trangthai` (SPI cài cho module thấp hơn) và gói `lich`. Web có đủ chức năng. App nhân viên có ghi chỉ số ngoại tuyến, xử lý cảnh báo, ghi sự cố và kích hoạt khẩn cấp trực tuyến. App người thân chỉ xem và nhận thông báo.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Hai gói `incident`, `health` cho một feature | Feature 007 gồm hai module của tài liệu nguồn; 002 cần giấy phép cơ sở (M06) mà không phụ thuộc M05 | Một gói: dữ liệu M06 nằm sai module (constitution X) |
| `chi_so_nct`, `theo_doi_nct` (một dòng mỗi người) | Đích khóa tuần tự cho gộp cảnh báo (DBR-18, SC-003), dấu nguy kịch (DBR-34), ngã, ngưỡng cá nhân, kích hoạt khẩn cấp đồng thời | Khóa `nguoi_cao_tuoi` của M01: truy cập dữ liệu module khác; chỉ dùng `UNIQUE`: số lần gộp sai khi thử lại *(R-15: `chi_so_nct`, `theo_doi_nct` → `trang_thai_nct`)* |
| `nguon_canh_bao` một dòng mỗi phát sinh | Cần cho số lần, báo lặp lại 24 giờ trượt (FR-038a), tự đóng khi mọi nguồn đã xử lý (FR-032), nguồn cho checklist (005) | Đếm trên cảnh báo: mất mức và thời điểm từng lần; không biết nguồn nào đã xử lý *(R-15: `nguon_canh_bao` → `su_kien_chung`)* |
| Tách `su_co` và `su_co_xu_ly` | Spec xếp nội dung nhóm 3 và trạng thái nhóm 2; DBR-19 cấm sửa nội dung sự cố Khẩn cấp | Một bảng trigger theo cột: dễ sót cột thêm sau, trái mẫu nhóm 3 của 000 *(R-15: `su_co_xu_ly` → `su_co`)* |
| Bốn SPI khai báo ở `incident.api` (008, 009, 012, 014) | Các module này làm plan sau và gọi 007 (bàn giao, ghi sự cố, khoanh vùng); 007 cần lại dữ liệu ca, Bác sĩ trực, người liên hệ chính, lượt thăm, điểm danh, nhật ký gửi | 007 gọi thẳng API của họ: vòng phụ thuộc, Modulith từ chối |
| Ba job, `lan_chay_m05`, `lan_chay_m06` | SC-004 đòi leo thang ≤ 1 phút; FR-084 đòi chạy bù đúng một cấp; tạm nhận bàn giao chạy đúng một lần mỗi ca. Chu kỳ và giờ chạy là thông số vận hành, không có trong Phụ lục 25 | Bộ hẹn giờ trong bộ nhớ: mất khi khởi động lại, vẫn phải quét để chạy bù *(R-15: `lan_chay_m05`, `lan_chay_m06` → `moc_da_nhac`)* |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung hoặc dựa vào các điểm dưới đây; plan của feature liên quan giữ đúng hoặc ghi rõ thay đổi.

1. **001** (đã ghi bổ sung vào [../001-elder-profile-assessment/contracts/module-interfaces.md](../001-elder-profile-assessment/contracts/module-interfaces.md) mục 2): sự kiện `SuCoDaGhiNhan` thêm trường `canDanhGiaLai` (mức Trung bình trở lên hoặc loại Ngã, trừ đồ gửi; FR-043 b, 013 FR-022) và được phát lại khi sự cố nâng lên Trung bình hoặc đổi loại sang Ngã; 001 chỉ báo đánh giá lại khi `canDanhGiaLai = true`. `ChiSoApi.ganNhat` do `health` cài. `TrangThaiNguoiCaoTuoiApi.thucHien(CHUYEN_VIEN)` dùng đúng bảng người gọi đã có, căn cứ là sự cố hoặc cảnh báo nguy kịch.
2. **005** (đã ghi bổ sung vào [../005-daily-care-tasks/contracts/module-interfaces.md](../005-daily-care-tasks/contracts/module-interfaces.md)):
   - `YeuCauCanhBao` thêm `loaiCanCu`, `canCuId` của công việc nguồn (FR-039b a);
   - sự kiện mới `CongViecDoBiBo(congViecId, nctId, lichDoId, chiSoIds, lyDo, mucQuanTrong)` khi công việc căn cứ `LAN_DO` đóng Không thực hiện (không do trạng thái cuối) hoặc còn Quá hạn khi hết ca (FR-039b b);
   - sự kiện mới `KetQuaNguDaGhi(ketQuaId, nctId, ngay, nhomGiaTri)` khi ghi hoặc đính chính kết quả ngủ (FR-039c);
   - hàm đọc mới `CongViecThongKeApi.nguoiDaGhiKetQua(nctId, tu, den)` (FR-060 c, CHK018).
   - 007 cài `NguonCongViecPort`, `ChiSoPort`, `CanhBaoDocPort` đúng chữ ký; gọi `nguonBiHuy` khi Ngừng lịch đo (theo 005 FR-022, điểm báo lại 2), kết thúc theo dõi sau ngã, loại người tiếp xúc; `nguonDaDoi` khi Thay đổi lịch đo; phát `SuCoNgaDaGhiNhan`, `LuaChonChamSocGiamNheDaGhi`, `LichDoDaDoi` như tên 005 đã đề xuất.
3. **006** (đã ghi bổ sung vào [../006-medication-management/contracts/module-interfaces.md](../006-medication-management/contracts/module-interfaces.md) mục 1.3): hàm đọc `LichThuocApi.nguoiDaGhiLieu(nctId, tu, den)` (FR-060 c). 007 nghe `YeuCauCanhBao`, `YeuCauDongCanhBao` của 006 với các loại đã khai báo.
4. **003**: 007 cài `TinhTrangLayNhiemChoPhanBo`; phát `DaThemVaoDanhSachTiepXuc(nctId, thoiDiem)` và `KhoanhVungDaGo` (thêm `vungId` vào đầu payload, 003 bỏ qua được); nghe `PhanBoDaDinhChinh`.
5. **004**: dùng mã điều kiện `KT_D_CANH_BAO_SU_CO`, `MucViecSauQuaDoiPort` mã `CANH_BAO_SU_CO`, `chiTiet` của `CHUYEN_VIEN` như đã khai báo; không đổi.
6. **008** cài `NhanSuCanhBaoPort`; gọi `CanhBaoBanGiaoApi.dangMo`, `khiXacNhanBanGiao` (MANDATORY) trong lệnh xác nhận bàn giao; đọc `ChiSoApi.vuotNguongTrongKhoang`; cung cấp danh mục phạm vi hành nghề cho giấy phép cơ sở. **009** cài `NhatKyThongBaoPort`; phân giải các nhóm người nhận ở research S-19 (gồm `BAC_SI_TRUC` nhiều người, `BAC_SI_TOAN_VIEN`, `NGUOI_TRONG_BAN_GHI_NGUON`, cờ `khongThay`); nghe `YeuCauDungNhacGoiDien`, `DaCoKetQuaXacNhanLai`; đọc `SoLienHeVienApi`. **012** cài `NguoiThanKhanCapPort`; nghe `KhoanhVungDaTao`, `SuCoDaHuyTrongBanTin`; đọc `KhoanhVungApi`, `SucKhoeCongNguoiThanApi`, `BanTinSucKhoeApi`. **013** gọi `SuCoLenhApi`; đọc `SuCoApi.xem`. **014** cài `DiemDanhTruyVetPort`; phát `incident.api.YeuCauTaoCanhBao` (thiếu người: Khẩn cấp); gọi `SuCoLenhApi.ghiNhan` cho "Báo thiếu người"; đọc `KhoanhVungApi`; nghe `KhoanhVungDaTao`. **011** nghe `SutCanPhatHien`; phát `YeuCauTaoCanhBao`, `YeuCauDongCanhBaoNguon`. **016** đọc `CanhBaoThongKeApi`, `ChiSoThongKeApi`, `KhoanhVungApi`.
7. *(Bổ sung 2026-10-18, Q-369)* **002** cung cấp `ThietBiGanApi.xacDinh` (002 module-interfaces 1.11). **005**: 007 thêm vào danh sách người dùng `CoMatBanTruApi.theoNgay` (đã ghi vào [../005-daily-care-tasks/contracts/module-interfaces.md](../005-daily-care-tasks/contracts/module-interfaces.md) mục 1.3). **014** cài `ChuyenDiSosPort` (014 FR-068a). **012** dựng nút SOS và màn trạng thái trên kênh người cao tuổi, gọi `/sos` (012 FR-085). **016** đọc `nguon` của `CanhBaoThongKeApi.canhBao`.

8. *(Bổ sung 2026-10-22, Q-373 → Q-376, Q-383, Q-384)* **001** thêm `NguyenVongApi.dieuTriHieuLuc` và `LenhTrangThai.tepDinhKemIds` cho `CHUYEN_VIEN` (001 research H-18; đã ghi ở 001 module-interfaces mục 1.2, 1.8). **008** cài thêm ba hàm của `NhanSuCanhBaoPort` (`dangTrongCa`, `tangTrongCa`, `tangDangCoCa`; module-interfaces mục 2.1) cho FR-047i. **012** gọi `/sos/{id}/location` và hiện `DA_TIEP_NHAN` ("đang tới"), `DA_TOI` trên kênh người cao tuổi (012 FR-085). **016** đọc `nguon` `NUT_HO_TRO`, `daToiNoiLuc`, `khongGhiMocToiNoi` để báo cáo thời gian tới nơi (18.3). **002**: `/emergency-assist` dùng `@RequiresPermission(EMERGENCY_ASSIST_BUTTON)`; `/sos/{id}/location` thuộc nhóm đường token thiết bị đã có (`SELF_CHECKED`). **017** dùng lại `common.bieumau` cho chứng từ PDF, Excel.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Câu chữ còn sót trước Q-307 trong spec 007.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Bảng FR-045 còn dòng "— | Hệ thống tạo (thiếu người khi trở về từ hoạt động ngoài viện, phục vụ suất có thành phần gây dị ứng) | Mới | Hệ thống", trái FR-040 ("Hệ thống MUST NOT tự tạo sự cố") và Q-307.
   - Key Entities "Lịch đo" còn nguồn "theo dõi sau ngã / theo dõi tiếp xúc" và "sự cố nguồn", trái FR-003.
   - Plan theo FR-040, FR-003: không có đường hệ thống tạo sự cố, lịch đo chỉ có nguồn chỉ định bác sĩ. Đề nghị xóa dòng bảng FR-045 và sửa Key Entities.

2. **Ngừng lịch đo: báo hay hủy công việc.** Bảng FR-006 ghi tác động của "Ngừng lịch đo" là "như trên" (005 chỉ báo Người phụ trách ca, Trưởng tầng); 005 FR-022 ghi "lịch đo ngừng" là căn cứ bị hủy, công việc Chưa đến hạn chuyển Hủy kèm lệnh. Plan theo 005 FR-022 cho Ngừng lịch đo và trạng thái cuối (`nguonBiHuy`), và chỉ báo khi Thay đổi lịch đo (`nguonDaDoi`). Đề nghị sửa cột Tác động dòng "Ngừng lịch đo" của bảng FR-006 cho khớp. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Lựa chọn "đưa về nhà"** (FR-050d, US11 #5, bảng FR-083 dòng "Cung cấp 004, 005"): câu "feature 004 nhận yêu cầu Cho tạm vắng" có thể hiểu là hệ thống tạo một yêu cầu ở 004, trái Q-307. Plan chỉ ghi lựa chọn và báo Hành chính, Trưởng tầng (`CanChoTamVangVeNha`, mức Trung bình); người đó thực hiện Cho tạm vắng ở 001/004 qua quy trình đón. Đề nghị sửa câu thành "Hành chính hoặc Trưởng tầng được báo để thực hiện Cho tạm vắng…". *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

4. **Nhắc thiếu ngưỡng cá nhân sau khi ngưỡng bị ngừng** (FR-025): spec nói nhắc "một lần khi vừa đạt mốc" và giữ trong danh sách "cho tới khi có ngưỡng". Khi ngưỡng cá nhân bị Ngừng sau đó, người đó lại thiếu; plan nhắc lại một lần (xóa cột đã nhắc khi có ngưỡng). Đề nghị xác nhận. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

5. **Mức của các thông báo spec 007 chưa nêu (009 FR-043a, Q-108).** Plan áp nguyên tắc 3 bậc (research S-19, dòng có *): báo lặp lại, lập công việc theo dõi sau ngã, cần lập danh sách tiếp xúc, lập công việc đo nhiệt độ, thông báo tiếp xúc, khoanh vùng, cần xác nhận dấu tạm, lựa chọn "về nhà", tạm nhận bàn giao ở mức Trung bình; nguồn đã được đính chính, nguồn đã xử lý trên sự cố liên kết, tác động còn hiệu lực, danh sách tiếp xúc cần xem lại, gỡ khoanh vùng, người phụ trách đổi ở mức Nhẹ. Đề nghị ghi vào spec 007 và bảng 009 FR-043b khi đồng bộ. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

6. **Khoảng hợp lệ vật lý khi thêm chỉ số mới.** CFG-M06-03 "theo danh mục chỉ số" nhưng FR-001 coi khoảng là thuộc tính của chỉ số. Plan lưu khoảng ở CFG-M06-03 (kiểu bảng) và chặn ghi chỉ số chưa có khoảng (409 `CHUA_CO_KHOANG_HOP_LE`); thêm chỉ số và khai khoảng là hai thao tác của Quản lý viện. Không đổi nghiệp vụ; ghi lại để chủ dự án biết cách tổ chức màn hình. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

7. **(Đồng bộ câu chữ spec 007, không đổi nghiệp vụ)** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Assumptions còn câu dẫn `docs/phan-tich-yeu-cau.md` mục 4.2 (đã bỏ); có thể giữ làm lịch sử.
   - FR-017 còn chữ "(đề xuất, mặc định \[10 phút\])" và "tham số mới, xem Điểm cần báo lại 3" dù CFG-M06-04 đã có ở Phụ lục 25.

8. **(2026-10-18) FR-047 dẫn nhầm FR-047c.** Câu "Nếu tại thời điểm kích hoạt không có Bác sĩ trực, hệ thống MUST áp FR-047c" của FR-047 trước đây là FR-047b; lần sửa 2026-10-18 đổi thành FR-047c (nay là quy tắc Q-354). Plan theo FR-047b (research S-12). Đề nghị sửa lại chữ trong spec 007. *(Đã xử lý 2026-10-18: spec 007 FR-047 đã sửa về FR-047b)*

9. **(2026-10-18) Mức sự cố và mô tả tự do thuộc phần nào (Q-352).** Q-352 (9.5, 14.5) liệt kê phần chung: có sự cố, loại, thời điểm, viện đã xử lý; không nêu mức. Spec 012 FR-052 lại ghi "phần chung của sự cố trong kỳ kèm mức". Plan theo spec 012: mức thuộc phần chung. `mo_ta`, `xu_ly_ban_dau`, diễn biến xếp vào phần sức khỏe (hạn chế hơn) vì có thể chứa tổn thương hoặc điều trị; "viện đã xử lý" là trạng thái xử lý (Đang xử lý, Đang theo dõi, Đã đóng). Đề nghị chủ dự án xác nhận mức thuộc phần chung và ghi vào Q-352. Loại sự cố tự nêu bệnh: cùng câu hỏi với spec "Điểm cần báo lại" 1; plan hiện xếp tên loại vào phần chung, trừ loại lây nhiễm theo Q-72. *(Đã xử lý 2026-10-18: Q-358: mức thuộc phần chung; thiết kế giữ nguyên)*

10. **(2026-10-18) Mã trường hợp của thông báo nguy kịch tới người thân.** FR-050b (c) gửi người liên hệ chính và mọi người đại diện nội dung tối thiểu. Q-354 có "khẩn cấp tới người liên hệ chính" và "xác nhận lại nguyện vọng khi nguy kịch". Plan gắn `XAC_NHAN_LAI_NGUYEN_VONG` cho thông báo tới người đại diện (vì họ được báo để chuẩn bị xác nhận lại) và `KHAN_CAP_NGUOI_LIEN_HE_CHINH` cho người liên hệ chính. Đề nghị xác nhận. *(Đã xử lý 2026-10-18: giữ theo plan, chủ dự án ủy quyền chốt)*

11. **(2026-10-22) "Cấp cứu không cần lý do" của FR-049.** FR-049 bắt lý do khi phiếu ghi "cần hỏi trước" mà chưa liên lạc được người cần báo, "cấp cứu không cần lý do này". Spec không định nghĩa "cấp cứu" trong lệnh Chuyển viện. Plan coi Chuyển viện từ sự cố mức Khẩn cấp là cấp cứu (không bắt lý do), từ sự cố Nhẹ, Trung bình thì bắt lý do khi `daLienLacNguoiCanBao = false` (khớp US5 kịch bản 8). Đề nghị xác nhận. *(Đã xử lý 2026-10-22: chủ dự án chọn theo plan: sự cố mức Khẩn cấp là cấp cứu.)*
12. **(2026-10-22) Lượt "Gọi 115" trên cảnh báo trước khi chuyển sự cố.** FR-047k: lượt Đã gọi được "được thêm làm diễn biến của sự cố liên kết nếu có". Plan: nếu cảnh báo chưa có sự cố liên kết lúc ghi, lượt được chép thành diễn biến `GOI_CAP_CUU` của sự cố khi cảnh báo được chuyển sự cố hoặc kích hoạt khẩn cấp; cảnh báo đóng không chuyển thì lượt chỉ nằm ở cảnh báo. Đề nghị xác nhận. *(Đã xử lý 2026-10-22: chủ dự án chọn theo plan: chép lượt Đã gọi được sang sự cố khi chuyển.)*
13. **(2026-10-22) Thư viện bản đồ và lớp bản đồ.** FR-047h đòi hiện tọa độ trên bản đồ có nút chỉ đường. Plan chọn Leaflet và lớp bản đồ OpenStreetMap (không cần khóa API, phải ghi nguồn bản đồ; tải lớp bản đồ từ máy chủ OSM nên có lưu lượng ra ngoài) cho web; nút chỉ đường mở ứng dụng bản đồ của máy qua đường dẫn chuẩn. Phương án Google Maps cần khóa API và thanh toán. Chờ chủ dự án duyệt thư viện (`package.json`) và chính sách dùng lớp bản đồ OSM; phần app chờ mở `mobile/`. *(Đã xử lý 2026-10-22: chủ dự án duyệt Leaflet với lớp bản đồ OpenStreetMap; bản ở research S-26.)*
14. **(2026-10-22) Pháp lý vị trí và phiếu chuyển viện.** Căn cứ xử lý vị trí GPS, thời hạn lưu (Q-373) và việc lập giấy chuyển cơ sở khi viện có giấy phép khám chữa bệnh (Q-383) chờ 24.4; plan lưu vị trí theo hạn của cảnh báo (`THEO_HO_SO`, cảnh báo không gắn người: `KHONG_GAN_HO_SO`) và không lập giấy chuyển cơ sở.

## Ghi chú triển khai

- *(2026-10-31, D, T003 phần nguồn cho B)* Thêm `VitalSignApi.latestPerType` và `LatestVitalSignView`, mở rộng đúng `FakeVitalSigns` đã có để B viết test nguồn tham khảo FR-024; giữ nguyên phần bàn giao `aboveThresholdInRange` của 008. DTO giữ giá trị thành phần bằng `BigDecimal`, metadata của danh mục và mốc đo; không cài đọc DB hoặc thuật toán chọn bản gần nhất trong fake. T003 vẫn mở; API/sự kiện còn lại, bản cài thật và consumer 001 chưa làm. Chữ ký ở module-interfaces mục 2.5, dòng review B và phần D còn thiếu ở team-plan.

- *(2026-10-31, kiểm tra cuối Phase 1)* `clean verify` đã chạy hết, nạp `.env`: 685 test đơn vị/kiến trúc xanh, 1487 IT có 8 thất bại và 20 lỗi; Spotless toàn backend sạch. Tám lớp resident và `FR_035_LocationHistoryHttpIT` trùng nền; `FR_012_NewAllergyIT` deadlock đã được A ghi trước đó; `T064_BedListPerformanceIT` đo 2997 ms/1000 ms, báo C. Thêm bốn lớp staffing cạn pool/kéo theo lỗi context, báo A/E; chạy riêng bốn lớp đó cùng ca deadlock và T002 sau khi kết thúc lượt chung 48/48 xanh. Lượt riêng dùng JVM mới, ghi log native; chưa kết luận nguyên nhân hay coi full verify đã xanh. Không sửa test của module khác, không thêm ngoại lệ hoặc bỏ test; Phase 1 chưa đạt điều kiện kiểm tra chung.

- *(2026-11-03, D, T052 một phần)* Lưu vòng đời khoanh vùng bằng `SimpleRecordApi` trên `simple_record`. Dùng mã `M05.CONTAINMENT` đã khai ở scope và retention. Lưu phạm vi, sự cố liên quan và thông tin gỡ trong JSON. Lệnh lưu khóa dòng theo phiên bản khi gỡ và khóa bản ghi sau gỡ. Mốc lịch sử vẫn đọc được trước thời điểm gỡ. Còn lưu danh sách tiếp xúc, lệnh nghiệp vụ và nối cổng phân bổ.
- *(2026-11-03, D, T002/Q-388)* `IncidentNotificationView` thêm chuỗi nhắc gọi theo bản ghi nguồn, dùng kiểu lồng của incident và giữ constructor cũ cho thông báo không có lượt nhắc. Fake chung chuyển tiếp đủ chuỗi. A (009) nối nguồn thật theo hợp đồng mục 2.4.
- *(2026-10-31, D, T002)* Dùng lại `FakeAlertStaff` đã có; ba SPI đọc còn lại của mục 2.1 cài bằng `EmergencyFamilyPort`, `AttendanceTracePort`, `NotificationLogPort`, kiểu trao đổi ở `api.dto`, chữ ký và trường cụ thể ghi ở module-interfaces mục 2.4 và glossary mục 13. Bản giả dùng chung ở `support/fake/incident/` cùng `FakeM05Config`, không tạo bộ `gia007` thứ hai. Chỉ test chủ động đặt dữ liệu, mặc định rỗng; giữ thứ tự người đại diện, lọc nguồn theo người cao tuổi hoặc đúng cặp loại/id, lọc khoảng chồng thời gian. Không thêm endpoint giả lập HTTP: công tắc nạp bản giả cho test là import cấu hình, theo README backend. Bản cài thật vẫn do 012, 014, 009 thực hiện.

- *(2026-10-31, D, T001)* Khung package theo backend/README.md: `health.internal/{catalog,schedule,measurement,threshold,license,examination,governance}`, `incident.internal/{alert,trend,incident,emergency,critical,fall,infection,governance}`; controller ở `web`, job ở `scheduling`. Các tên gói tiếng Việt trong sơ đồ mục Source Code là tên logic; khi có lớp triển khai thì đặt trong tầng `service`, `domain`, `persistence`, `config` của chức năng, không đặt lớp trực tiếp ở gói chức năng. Khung chỉ gồm `package-info.java`, chưa tạo service hoặc migration. `M05M06ArchTest` kiểm `health` không phụ thuộc `incident` và `incident` chỉ dùng `health.api`.

Ghi khi triển khai (constitution XIV: lệch kỹ thuật ghi ở plan cùng lúc với mã). Tên tiếng Anh theo glossary mục 6.5 (phần `common`) và mục 13.

- *(2026-10-22, T104, nhánh `impl/common-bieumau`, A)* `common.bieumau` cài ở gói `common.document`: `PdfDocument` (`a4(facility, logo, title)`, `paragraph`, `table` có dòng tiêu đề lặp mỗi trang, `signatures` ô ký có dòng "(Ký, ghi rõ họ tên)", `toBytes` đánh "Trang x/y", `stamp(bytes, "Bản in lại")`); `ExcelDocument` (`sheet(name).header(facility, title).line(text).table(columns, rows)`, định dạng `MONEY` `#,##0`, `DATE` `dd/mm/yyyy`, `DATE_TIME` `dd/mm/yyyy hh:mm`, `NUMBER`, `TEXT`; `read(xlsx, headers, maxRows)` tìm dòng tiêu đề theo tên cột của mẫu trong 10 dòng đầu, bỏ dòng trống, trả `ExcelRow` (`rowNumber()` là số dòng Excel, `text`, `number`, `date` theo tên cột); không phải `.xlsx`, thiếu cột của mẫu, quá `maxRows` dòng, ô sai kiểu → 422 `INVALID_VALUE`); `FacilityInfo`.
- **Lệch S-25**: `PdfDocument` không tự đọc CFG-M11-11 qua `ParameterApi` vì `common` là module mở mọi module dùng, gọi ngược `administration` tạo vòng phụ thuộc. Module dựng chứng từ đọc `ParameterApi.read(CFG_M11_11)`: chưa đặt (`isSet() = false`) thì truyền `null` (bỏ đầu trang), có thì `asTable(FacilityInfo.class)`. Giá trị CFG-M11-11 dùng khóa JSON `name`, `address`, `taxCode`, `phone`, `logoFileId` (017 plan điểm 6). Logo truyền dạng byte; `FileApi` chưa có hàm đọc nội dung tệp nên tới khi có thì truyền `null` (không in logo).
- `stamp` ghi nối (chế độ append của `PdfStamper`): các byte của file đã lưu là phần đầu của bản có dấu, nội dung trang gốc không đổi (FR-050h). Dấu đặt ở lề trên bên phải mỗi trang, không che nội dung.
- Font Be Vietnam Pro bản TTF tĩnh (Regular, Bold) lấy từ kho google/fonts (`ofl/bevietnampro`), kèm `BeVietnamPro-OFL.txt`, ở `backend/src/main/resources/fonts/`; nhúng tập con (Identity-H) nên trích chữ có dấu đúng. Gói `@fontsource` của web chỉ có woff tách theo dải ký tự, OpenPDF không dùng được.
- `backend/pom.xml`: OpenPDF 3.0.5 loại trừ `brotli4j` (kéo bản native theo hệ điều hành lúc build, chứng từ không cần nén Brotli; test chạy không có nó); fastexcel, fastexcel-reader 0.20.2 (010 chưa thêm).
- Bảng: dòng có số ô khác số cột thì `IllegalArgumentException` (lỗi lập trình, không âm thầm bỏ dòng).
- Màn Tham số (000 T081) dựng sẵn các ô `name`, `address`, `taxCode`, `phone` cho CFG-M11-11 khi chưa có giá trị (`TABLE_TEMPLATES`), để Quản lý viện nhập được thông tin viện; `ParameterValueTest.facilityInfo_readsCfgM11_11_withoutLogo` kiểm `asTable(FacilityInfo.class)` khi thiếu logo.
- Test: `PdfDocumentTest` (dấu tiếng Việt trích lại đúng, A4, font nhúng, không có thông tin viện thì bỏ đầu trang, bảng dài ngắt trang và đánh số mọi trang, đóng dấu giữ nguyên file gốc), `ExcelDocumentTest` (ghi rồi đọc lại theo mẫu, định dạng tiền và ngày, sai mẫu, quá số dòng, không phải `.xlsx`).

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

### Rà T004 và triển khai phần xác định được của T005 (2026-11-01, D)

- T004 còn mở sau rà hợp đồng: các record/sự kiện M05 hiện tại chưa đủ mục 1.12 và chưa có `HasNotificationRequests`; DTO sự cố cho người thân còn phải tách các trường theo S-19a. Bổ sung module-interfaces 2.7–2.8 để dòng review dẫn đúng nơi; bộ test API/fake xanh không thay nghiệm thu phần khai báo còn thiếu hoặc publisher thật.
- T004 hoàn tất sau lượt test đỏ/xanh tiếp theo: bổ sung đủ kiểu sự kiện mục 1.12, mọi sự kiện gửi 009 cài `HasNotificationRequests`; `FamilyIncidentView` tách phần chung/phần sức khỏe theo S-19a; `FakeIncidentEvents.publish` dùng chung cho consumer. Publisher production vẫn thuộc task nghiệp vụ tạo từng sự kiện. Bộ T004 và kiến trúc 30/30 xanh.
- T010 được kéo lên trong khi T005 chờ dữ liệu lâm sàng: `ThresholdComparator` so bốn cận bằng `BigDecimal`, cận `null` không so, bằng cận không vượt; thứ tự mức của nhiều thành phần là `NORMAL < NO_THRESHOLD < WARNING < DANGEROUS`. `SC_001_VitalSignLevelTest` kiểm 300 giá trị từ sáu bảng cận; T010 và kiến trúc 17/17 xanh sau lượt đỏ thiếu miền.
- T007 đã cài phần khóa tuần tự theo người bằng `VitalSignLock` và `MonitoringLock`, dùng `ResidentModuleStateApi` với loại `M06.RESIDENT_VITAL_SIGNS` và `M05.RESIDENT_MONITORING`. Lần tạo đầu được tuần tự bằng MySQL advisory mutex trên chính connection của transaction gọi; sau khi có dòng, khóa `FOR UPDATE` của bảng chung giữ tới commit/rollback. Test đỏ loại hai cách không an toàn: `TechnicalLocks` gây deadlock khi 10 lệnh cùng tạo lần đầu; transaction `REQUIRES_NEW` để khởi tạo làm cạn Hikari vì transaction ngoài đang giữ connection. Bản hiện tại giữ rollback nguyên tử và 8/8 IT xanh. Ba job T007 chưa cài vì mọi `ScheduledJob` mới phải được A thêm vào `SchedulerScopeArchTest.ALLOWED`; đã ghi dòng chờ ở team-plan mục 8.
- Fake chung tách đúng hai lệnh `acknowledge` (cảnh báo/sự cố) bằng bean ủy quyền; thống kê chỉ trả nguồn của đúng `ScopeFilter`; nguồn người thân đặt theo cặp người cao tuổi/người thân. Lịch sử sự cố lọc bản hủy vẫn giữ bản đã đóng theo mục 1.7; truy vấn dùng khoảng nửa mở. Không thêm fake production hoặc bộ fake thứ hai.
- T005 dùng bảng chung đã có, không tạo `chi_so_nct`, `theo_doi_nct`, bảng chạy job hoặc danh mục riêng. Phần migration đã cài chỉ nạp giá trị có nguồn: bốn quy tắc FR-039, 12 loại diễn biến (năm dấu hồi sức theo BR-M05-20). Dùng `catalog_item.status` làm bật/tắt quy tắc, tránh giữ hai trạng thái trùng nhau; các thuộc tính mức/mã CFG/nhóm/dấu hồi sức ở JSON. Bảng chung đã khai `DataGroup=CATALOG` nên không đổi cấu trúc bảng do A sở hữu.
- Chưa nạp danh mục chỉ số/thành phần và loại sự cố khi chưa có đầy đủ giá trị khởi tạo (nhóm sinh hiệu, đơn vị/độ chính xác, mức mặc định chưa nêu); không tự điền khoảng lâm sàng CFG-M06-03. Ghi phần cần chủ dự án chốt vào team-plan mục 8; T005 giữ mở. Danh sách hồi sức khởi tạo vẫn cần Bác sĩ đối chiếu theo nghiep-vu 24.4/Q-278. Không sửa spec hoặc tài liệu nghiệp vụ.
- Phần 006 giữ nguyên theo yêu cầu người dùng; không chuyển nhánh khi 007 còn thay đổi chưa commit. Chưa tuyên bố hoàn tất Phase 1/2.

### Điều chỉnh thứ tự cho nguồn chỉ số B đang chờ (2026-10-31, D)

- T003 đã đủ hợp đồng và bản giả; 27 kiểm thử hợp đồng/kiến trúc xanh. Các bản cài lịch đo, thống kê, liên hệ viện vẫn theo task nghiệp vụ tương ứng.
- Kéo riêng phần lược đồ T012 và đọc T013 lên trước phần còn lại của Phase 2 để tháo chờ `VitalSignApi.latestPerType` cho B. Không kéo lệnh ghi, HTTP, so ngưỡng hoặc job lên cùng; không đánh dấu hoàn tất US1. Hoán đổi số hai migration chưa cài: T012 dùng `V007_01__vital_sign_record.sql`, T005 dùng `V007_02__health_incident_catalogs.sql`, các số còn lại giữ nguyên; không thêm migration thấp hơn bản 007 đã triển khai.
- Bảng `vital_sign_record` gộp thành phần vào JSON (giữ cả giá trị và mức tại lúc đo), `DataGroup=MULTIPLE`, khóa theo dòng bằng `locked_at`; không có bảng thành phần riêng. Danh mục chỉ số dùng `catalog_item`, loại `M06.VITAL_SIGN`. Không tự đặt cờ sinh hiệu mặc định cho các chỉ số; seed chính thức thuộc T005.
- Đọc đính chính qua `CorrectionApi.current`, vị trí tại lúc đo qua `ResidentLocationApi.at`; không truy vấn SQL của module khác. `LocationApi` chỉ có khai báo, chưa có bean thật; dùng phương thức `at` cùng chữ ký đã được C cài trên `ResidentLocationApi`, không thêm fake vào ứng dụng. Bản ghi mới của 007 chưa có dữ liệu cũ nên không cần ánh xạ đổi ID như 006 T069.
- Kiểm chứng cuối phần đọc: `LatestVitalSignReadIT` 4 ca + `VitalSignSchemaIT` 3 ca xanh; bộ chung 50/50 gồm T002/T003, `RecordGroupTriggerIT`, `DataGroupArchTest`, `SharedKindArchTest`, ranh giới module và cấu trúc. Lượt đầu phát hiện thiếu bean `LocationApi`, đã sửa sang API thật nêu trên rồi chạy lại toàn bộ bộ chọn; không tắt test. Chưa hoàn tất Phase 2 hoặc US1, chưa thay lượt clean verify toàn backend đang đỏ bằng kết quả bộ chọn.

- Ánh xạ bảng ở data-model mục 0; task T106. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.

### Kiểm chứng T004 và bổ sung T005 theo tài liệu gốc (2026-11-02, D)

- Rà phần T004 đã có trong workspace; test mới đỏ vì `hasOpenItem` bỏ sót cảnh báo mở. `FakeIncidents` nhận `AlertApi` qua constructor, cấu hình dùng đúng `FakeAlerts` chung. Test catalog sự kiện tiếp tục đỏ vì `ContactAdded` chưa cài `HasNotificationRequests`; đã bổ sung `eventKey`/yêu cầu thông báo, giữ constructor hai tham số cho consumer 003. Constructor tương thích không gửi tin; publisher FR-062 phải cấp đầy đủ yêu cầu. Không thay HTTP hoặc cài fake vào production.
- Tra docs/business mục 8.9, 9.2–9.3, 10.1, BR-M04-17, BR-M06-04: đủ căn cứ nạp Ngã và Đi lạc/không trở về Khẩn cấp. Test IT đỏ vì chưa có hai dòng; migration bổ sung `V007_02_1__emergency_incident_type_catalog.sql` đưa vào `M05.INCIDENT_TYPE`, không sửa V007_02 đã có, không tạo bảng mới. Đơn vị/độ chính xác/cờ sinh hiệu và mức mặc định còn thiếu không được suy ra từ ví dụ lâm sàng. T005 tiếp tục mở; không đổi spec/nghiệp vụ.
- Kiểm cuối: `spotless:apply verify` có chọn test, 39 test hợp đồng/kiến trúc + 4 IT MySQL đều xanh, không bỏ qua test trong bộ chọn. Spotless apply/check giới hạn incident (100 file); `git diff --check` sạch. Log tại `backend/logs/007-t004-t005-green-20261102.log`; các bước đỏ tại `007-t004-red-20261102.log`, `007-t004-contact-red-20261102.log`, `007-t005-red-20261102.log`. Đây không phải verify toàn backend; Phase 2 chưa hoàn tất.

### T006 — phần lệnh danh mục M05 (2026-11-02, D)

- `IncidentCatalogService` ở `incident.internal.alert.service`; kiểu lệnh/kết quả, enum, `SharedKind` và `AuditAction` ở `domain`, đúng README. Ghi danh mục qua `CatalogApi`, không tạo bảng/migration hoặc gọi nội bộ module khác. Quy tắc dùng `catalog_item.status` cho bật/tắt; đổi mức và trạng thái trong cùng transaction, ghi một nhật ký có lý do. ETag so trước khi ghi, API bảng chung tiếp tục kiểm phiên bản khi cập nhật.
- Thêm loại sự cố tùy chỉnh luôn giữ `allowsNoResident=false`; sửa loại giữ nguyên mã và cờ chỉ đọc này. Nhật ký đổi `nonHealth` giữ giá trị trước/sau. Loại diễn biến lưu nhóm và dấu hồi sức đúng nội dung lệnh. Lệnh cần lý do khai `@BusinessCommand`, kiểm thiếu lý do trước khi ghi. Chỉ Quản lý viện được cấu hình theo FR-039/042/048a; dùng khóa cấu hình hiện có `ACCOUNT_PERMISSION_PARAMETER/UPDATE` cùng kiểm vai trò riêng, không thêm khóa hoặc sửa ma trận quyền; ghi dòng A review ánh xạ này.
- Test đỏ thiếu lớp ghi ở `backend/logs/007-t006-red.log`. Lượt `spotless:apply verify` chọn 25 test đơn vị/kiến trúc và 2 IT MySQL: tất cả xanh, Spotless apply/check sạch, kết thúc 13:04:07; log `007-t006-verify.log`. IT dùng tài khoản và quyền thật, kiểm bật lại quy tắc, từ chối ETag cũ không thêm nhật ký và rollback dòng danh mục cùng nhật ký. Wrapper hiện lỗi truy cập `.m2` trong sandbox; chạy đúng bản Maven 3.10.0 đã được wrapper tải, không sửa wrapper/pom.
- T006 chưa hoàn tất: còn phần M06, truy vấn/controller và vòng đời hiệu lực/bảo vệ mã đặc biệt. Không thêm endpoint hoặc đổi hợp đồng HTTP trong phần này; chưa chạy verify toàn backend.

### T006 — hoàn thiện dịch vụ M05/M06, chờ duyệt HTTP (2026-11-02, D)

- `VitalSignCatalogService` dùng `CatalogApi` cho cha/con M06, nhật ký cùng transaction; `HealthCatalogs.VITAL_SIGN_COMPONENT` có parent và độ chính xác, không sửa record `VitalSignAttributes` cũ hoặc constructor consumer. Chỉ sửa metadata cho phép của FR-002; giữ cấu trúc thành phần. Khoảng vật lý lấy CFG-M06-03 một lần mỗi truy vấn, không tự nạp giá trị mặc định; thiếu thành phần/khoảng thì có dấu `missingValidRange`.
- Hoàn tất truy vấn M05 và cập nhật `active` cho loại sự cố; chặn ngừng `FALL`, `SUSPECTED_INFECTION`, `FACILITY`, `BELONGINGS_LOST`, `BELONGINGS_DAMAGED`. Nhật ký thêm hiệu lực trước/sau. Không tự thêm endpoint ngừng loại sự cố vì hợp đồng hiện chỉ có trường `hieuLuc` trong PUT.
- Test mới viết trước, đỏ thiếu service M06 rồi đỏ thiếu truy vấn/vòng đời M05. Bộ cuối 31 test đơn vị/kiến trúc xanh (14 nghiệp vụ, 17 kiến trúc), 4 IT MySQL xanh: M05 bật lại/ETag cũ và rollback; M06 giữ thành phần sau ngừng và rollback cha/con. Spotless apply/check hai module sạch. Log `007-t006-health-red.log`, `007-t006-health-green.log` (lượt đỏ mở rộng M05), `007-t006-catalog-green.log`, `007-t006-catalog-it.log`, `007-t006-health-format-check.log`; lượt IT kết thúc 13:15:07.
- Người dùng ưu tiên T006, hoãn T005. Còn controller HTTP: bản gốc api-007.yaml vẫn tiếng Việt. CLAUDE.md mục 4 yêu cầu dịch và chủ dự án duyệt trước cài; đã chuẩn bị bản dịch cụ thể `contracts/catalog-translation.md` để duyệt, giữ nguyên số endpoint/trường/kiểu. Chưa thay hợp đồng hoặc cài controller khi chưa có câu trả lời cho việc duyệt. T006 giữ mở; chưa chạy verify toàn backend.

### T006 — hoàn tất HTTP danh mục M05/M06 (2026-11-02, D)

- Theo yêu cầu hoàn tất rồi mới đánh dấu, đã áp bản dịch tiếng Anh phần danh mục vào `api-007.yaml` và nối đủ tám đường dẫn bằng `VitalSignCatalogController`, `IncidentCatalogController` cùng request/response riêng trong `web/`.
- HTTP không lộ trường `version`; lệnh tạo/sửa trả ETag, lệnh sửa nhận `If-Match`. `allowsNoResident` và khoảng vật lý chỉ đọc; `sortOrder` không được thêm vào hợp đồng.
- TDD: lượt đỏ thiếu các lớp HTTP ở `backend/logs/007-t006-http-red.log`. Sau cài, Spotless và 32 test liên quan/kiến trúc xanh; 4 IT MySQL xanh, đồng thời nạp được Spring context có controller mới. Log: `007-t006-http-green.log`, `007-t006-http-it.log`. T006 hoàn tất; T005 vẫn mở vì thiếu dữ liệu seed có căn cứ.

### T014 — ngoại tuyến và xác nhận quá hạn đo lại (2026-11-02, D)

- `VitalSignOfflineHandler` dùng loại phong bì `M06.BAN_GHI_CHI_SO` của hợp đồng 002, chuyển `effectiveAt` thành mốc đo/thiết bị và truyền cờ `ACCEPTED_WITH_FLAG` vào cả `recordOffline` và `remeasureOffline`. Không đổi API công khai; kiểm quyền theo thời điểm thiết bị vẫn do khung 002 điều phối.
- Migration mới `V007_04__vital_sign_offline_permission_flag.sql` lưu `synced_after_permission_loss BOOLEAN NOT NULL DEFAULT FALSE`; trigger giữ cờ bất biến cùng dữ liệu gốc, kể cả khi xác nhận bản nháp. Hai fixture `VitalSignSchemaIT` bổ sung cột trong phép chép dòng để tiếp tục kiểm đúng CHECK cũ; dữ liệu thời gian test dùng micro giây đúng DATETIME(6).
- Tác vụ `OverdueRemeasurementTask` ở `health.internal.measurement.service` cài `M06TimedTask` nhịp phút, được `M06Jobs`/`M06MinuteJob` gọi cả khi chạy bù; chỉ xác nhận bản nháp quá hạn, thêm `NOT_REMEASURED`, khóa đúng một lần và phát `VitalSignConfirmed` cho M05. Không sinh bản ghi đo mới. T007 và các phần job khác giữ trạng thái hiện tại.
- TDD: tiếp tục từ `007-t014-flag-red.log`, xác nhận đỏ chữ ký handler tại `007-t014-handler-red-current.log`; lượt hồi quy đầu bắt fixture SQL thiếu cột và độ chính xác thời gian. Lượt cuối `backend/logs/007-t014-final.log`: 43/43 xanh, không bỏ qua ca nào (21 MySQL, 8 đơn vị, 14 kiến trúc), bao gồm M05M06ArchTest, ModuleBoundaryTest, LayerArchTest, ApiNamedInterfaceArchTest; quá hạn 1/1, handler 4/4, ngoại tuyến MySQL 4/4. Spotless apply/check giới hạn bốn file Java T014. Đây là bộ chọn liên quan, không thay thế verify toàn backend; chỉ đánh dấu T014.

### T009 - Màn web Danh mục sức khỏe (2026-11-02, D)

- Đã tạo `frontend/src/features/health-catalog/HealthCatalogPage.jsx` và test `HealthCatalogPage.test.jsx`.
- 4 tab ToggleGroup: Chỉ số sức khỏe, Quy tắc xu hướng, Loại sự cố, Loại diễn biến.
- Chỉ số sức khỏe: hiển thị khoảng hợp lệ vật lý theo CFG-M06-03 hoặc cảnh báo chưa có khoảng hợp lệ, cờ sinh hiệu, lọc theo trạng thái hiệu lực, sửa thông tin chỉ số gửi If-Match kèm ETag, ngừng hiệu lực chỉ số bắt buộc lý do.
- Quy tắc xu hướng: cấu hình mức cảnh báo (Nhẹ / Trung bình / Khẩn cấp), bật/tắt quy tắc, bắt buộc lý do gửi kèm If-Match.
- Loại sự cố: sửa loại sự cố, mức mặc định, cờ không thuộc sức khỏe và cờ cho phép không gắn người cao tuổi.
- Loại diễn biến: hiển thị danh mục diễn biến và cờ biện pháp hồi sức.
- Sinh client API từ api-007.yaml (`npm run gen:api`), kết nối `PAGES` trong `App.jsx` và menu `navigation.js`.
- TDD: test đỏ trước khi cài (`HealthCatalogPage.test.jsx`), sau khi cài 6/6 test xanh. `npm run lint` và `npm run build` đều qua 100%.

### Sửa kiểm kiến trúc sau đồng bộ develop (2026-11-03, D)

- Lệnh `IncidentCatalogService.updateTrendRule` nhận `Reason` trực tiếp thay vì gói trong `TrendRuleUpdate`, theo FR-007 và `DataGroupArchTest`. HTTP vẫn gửi cùng trường `reason`; controller tách lý do, nhật ký giữ nguyên và test thiếu lý do/ETag/rollback không bỏ. Chỉ đổi kiểu nội bộ, không đổi `api/` hoặc hợp đồng HTTP.
- Bỏ hai thời lượng mặc định ghi cứng của `AlertService`: mức Trung bình đọc `CFG_M05_01` mỗi lần; mức Nhẹ lấy giờ kết thúc ca qua `AlertStaffPort`, sau mốc `CFG_M09_04` thì lấy ca kế tiếp theo FR-031/CHK020. Khi thiếu cấu hình hoặc nguồn ca, dừng trước khi ghi, không suy ra hạn 60 phút. Test `FR_031_AlertDeadlineTest` được viết trước, chạy đỏ 5/7, gồm hạn tùy cấu hình và biên bàn giao.
- Người dùng yêu cầu sửa ba lỗi kiến trúc và hoàn tất merge: thêm đúng ba lớp job vào danh sách FR-010a của `SchedulerScopeArchTest` theo S-21; không sửa predicate/assertion của luật. Job gọi trực tiếp trong test, chưa thay enum `runJobs` của api-000. Giữ dòng A review ở team-plan vì file kiểm dùng chung thuộc A. Định dạng `NFR_04_M06JobFrameworkTest` bằng Spotless, không đổi hành vi. Kết quả kiểm cuối ghi ở progress.md.

- Kiểm toàn backend phát hiện test `FacilitySourcesIT` đã có nhưng thiếu bean: cài nguồn đọc `health.internal.license.service.FacilitySources` theo S-16, dùng `CatalogApi` cho giấy phép, thay thế do sai sót và số liên hệ hiện hành. Không thêm lệnh tạo/sửa giấy phép hoặc endpoint; T057 vẫn mở.
- `VitalSignController` bỏ hai alias tiếng Anh chưa có trong api-007, giữ đúng đường dẫn đang khai báo. Đăng ký api-007 trong `ApiContractIT` với 14 operation đã cài và 76 operation chưa cài; phần kiểm cấu trúc operation chưa bật vì hợp đồng đo chỉ số chưa dịch đầy đủ. Không đổi cấu hình kiểm của hợp đồng khác; T062 giữ mở cho phần so operation, không coi kiểm đường dẫn là nghiệm thu đầy đủ.
- Test seed T005 giới hạn truy vấn theo bốn mã khởi tạo thay vì giả định bảng không có dòng do lớp IT khác tạo; giữ nguyên kiểm số lượng, thứ tự, mức và mã CFG. Lượt đỏ toàn backend cho thấy dòng TREND_* của test danh mục làm sai kích thước.

- Sau khi người dùng merge 006 vào develop, nhánh 007 đã nhận `57671a35` ở merge commit `40de7edd`; conflict chỉ progress, kiểm giữ đủ dòng hai phía. Lượt verify toàn kho trước ghép 006 dừng tại HandoverIT sau khi lưu thread dump: lỗi ghi EVENT_PUBLICATION.COMPLETION_DATE với ngày 2045 và chờ MySQL; không coi full verify đạt. Lỗi ngoài phạm vi 007 còn FR_035_LocationHistoryHttpIT (503/404 đã ghi trước) và AssignmentIT (quyền, đồng thời). Bản cuối chạy verify chọn toàn bộ test health/incident, kiến trúc, ModuleBoundaryTest và ApiContractIT; kết quả cuối ghi ở progress.

- Kiểm sạch sau ghép 006: 120 test đơn vị/kiến trúc xanh; 51 IT health/incident xanh. ApiContractIT phát hiện schema ReasonBody của health trùng tên resident làm mất required của bốn lệnh 001; sửa riêng DTO health bằng @Schema(name="M06ReasonBody") và khai reason bắt buộc. Không sửa DTO hoặc hợp đồng 001. T062 vẫn chưa so đầy đủ operation 007; không dùng đăng ký đường dẫn thay nghiệm thu schema.

### Đường dẫn tiếng Anh cho chỉ số đang triển khai (2026-11-03, D)

- Người dùng duyệt đổi nhanh hai đường dẫn đang dùng: GET/POST `/residents/{residentId}/vital-signs`, POST `/vital-sign-records/{id}/remeasure`. Đổi đồng thời hai path của api-007 và binding `residentId` trong controller; thêm parameter ResidentId cho đúng operation, giữ NctId cho các endpoint khác chưa dịch. Không đổi số operation hoặc dữ liệu nghiệp vụ.
- Sinh lại client bằng `npm run gen:api`: `getResidentsByResidentIdVitalSigns`, `postResidentsByResidentIdVitalSigns`, `postVitalSignRecordsByIdRemeasure`. Tìm toàn frontend/mobile không có consumer của ba hàm tên cũ ngoài file client sinh, nên không cần đổi màn hình/mobile.
- Test HTTP FR_010_VitalSignRoutesTest chạy đỏ 404 trước cài; test VitalSignApi.test.js đỏ vì chưa có hàm tiếng Anh. Sau sửa kiểm GET/POST/đo lại đúng id, các đường cũ trả 404, client gọi đúng ba URL. T062 giữ trạng thái hiện tại: chỉ dịch đường dẫn hai endpoint này, chưa dịch toàn bộ schema/operation 007.

### T062 — dịch đủ api-007 và so operation (2026-11-03, thay D)

- Bản dịch ở `contracts/api-translation.md`, chủ dự án duyệt 2026-11-03. Chỉ đổi tên; số endpoint, trường, kiểu, mã phản hồi giữ nguyên. `donViDuaDi` dịch thành `transferUnit` theo lệnh Chuyển viện của 001, khác với `transportedBy` trong bản đề xuất.
- Năm điểm ở mục 7 của bản dịch, áp theo đề xuất:
  1. `V007_07__alert_staff_assist_type.sql` đổi `NV_YEU_CAU_HO_TRO` thành `STAFF_ASSIST_REQUEST`: sửa cột sinh, `CHECK`, các dòng cũ và tạo lại trigger chặn sửa.
  2. Mã lỗi `OUT_OF_PHYSICAL_RANGE` thay `NGOAI_KHOANG_VAT_LY`; phạm vi ngưỡng trả `FACILITY` thay `CO_SO`.
  3. Thân lệnh ghi, đo lại chỉ số bỏ tên trường tiếng Việt và bỏ `taskId`; 005 ghi qua `VitalSignPort`.
  4. `measuredBy` trả họ tên người đo qua `AccountApi.fullName` (thiếu thì "#id"), bỏ `residentId` khỏi phản hồi.
  5. `/sos` chỉ đổi tên header thành `X-Device-Token`.
- Sửa thêm để khớp hợp đồng khi bật so operation:
  - nhận `Idempotency-Key` (không bắt buộc, như 006);
  - khai báo mã phản hồi 201, 403, 409, 412, 422;
  - các trường bắt buộc; enum của mã quy tắc xu hướng;
  - `id`, `active` ở thân ghi danh mục (bỏ qua khi ghi);
  - schema lỗi riêng `M06Problem`, `M05Problem` chỉ để mô tả, như `M09Problem` của 008.
- Còn lệch, chưa sửa: `VitalSignRecordResponse` luôn trả phạm vi `FACILITY` dù bản ghi so theo ngưỡng cá nhân. Sửa khi cài phần ngưỡng (T020 → T023).

- *(2026-11-03, D, T034 một phần / T052–T054 khoanh vùng)* Migration sự cố dùng `V007_08__incident.sql` vì `V007_05` đã được dùng cho ngưỡng theo dõi trên develop. Nội dung ghi nhận được bảo vệ bằng quyền theo cột và trigger; trạng thái xử lý gộp cùng bảng theo R-15. `IncidentAllocationStatus` là `@Fallback` để adapter thật được test/module ưu tiên bản giả khai rõ; adapter đọc bảng sự cố và hồ sơ chung, tính tại thời điểm gọi, hợp phạm vi nhiều vùng chồng nhau. Lệnh HTTP khoanh vùng khóa các sự cố theo thứ tự id trước khi tạo/gắn vùng, ghi nhật ký và phát `IsolationCreated`/`IsolationLifted` trong cùng transaction. Theo phần mở đầu api-007, lệnh trạng thái không dùng If-Match. Các record nội dung hồ sơ chung đặt ở domain theo LayerArchTest. Spotless và 13 test 007 MySQL/đơn vị xanh; `AdmissionRealBedIT` hồi quy xanh 7/7; `ApiContractIT`, `DataGroupArchTest`, `ModuleBoundaryTest`, `LayerArchTest` xanh 16/16. Lượt `verify` toàn backend bị dừng giữa IT ở `PermissionMatrixIT`, không có kết quả BUILD cuối. T034, T052, T053, T054 còn mở cho phần nghiệp vụ sự cố, đính chính, truy vết và danh sách tiếp xúc.
- *(2026-11-03, D, T053–T054 một phần / danh sách tiếp xúc)* `POST /incidents/{id}/contact-list` lập Nháp bằng lệnh của người có `CONTACT_LIST/CREATE`, kiểm phạm vi bằng người cao tuổi của sự cố nghi nhiễm; khóa sự cố và kiểm trạng thái đang mở, giữ khóa duy nhất một danh sách. Khoảng truy vết được tính từ `discovered_at` và một lần đọc `CFG-M05-07`, không tự thêm người theo Q-307; ghi audit cùng transaction. Store giữ danh sách trên `simple_record` theo R-15. T053/T054 vẫn mở cho tra cứu, thêm/bỏ/xác nhận/loại người, thông báo và nguồn công việc. Test HTTP đỏ 404 trước cài; sau cài 3/3 xanh. `ApiContractIT` xanh; `LayerArchTest` tìm record lồng trong persistence, đã chuyển sang domain rồi chạy lại `LayerArchTest` và `ModuleBoundaryTest` xanh 11/11.

- *(2026-11-03, D, BR-M05-22 một phần)* Thêm `IncidentService.changeLevel` và ghi có khóa/phiên bản trong `IncidentStore`. Hạ từ `CRITICAL` chỉ cho Bác sĩ, giữ `wasEmergency`, ghi audit có lý do và phát `CallReminderStopRequested("M05.INCIDENT", incidentId)` trong cùng transaction. Nối `POST /incidents/{id}/change-level` và bỏ operation này khỏi danh sách chờ của `ApiContractIT`. Unit/controller 4/4, `IncidentRecordIT` 3/3 và `ApiContractIT` 1/1 xanh; kiểm tầng/ranh giới xanh. `RequiresPermissionArchTest` không còn nêu `IncidentService`; gate vẫn đỏ riêng ở `ContactListService.createDraft` có sẵn. T033/T035/T037 giữ mở vì phần vòng đời sự cố còn lại chưa cài.
