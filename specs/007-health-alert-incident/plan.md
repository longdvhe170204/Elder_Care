# Implementation Plan: Sức khỏe, cảnh báo và sự cố

**Branch**: `007-health-alert-incident` | **Cập nhật**: 2026-10-18 | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-health-alert-incident/spec.md`

## Summary

Feature 007 gồm module M06 – Sức khỏe (mục 10) và M05 – Sự cố và cảnh báo (mục 9); UC-16 phần khởi phát từ sự cố, UC-32 → UC-38, UC-83, UC-84. Nó cung cấp:
1. Danh mục chỉ số với khoảng hợp lệ vật lý ở CFG-M06-03; lịch đo của Bác sĩ làm căn cứ lập công việc đo, không sinh công việc (mục A).
2. Ghi chỉ số (từ công việc đo, ngoài lịch, ngoại tuyến), so ngưỡng hai mức theo thành phần nặng nhất, đo lại giá trị nguy hiểm, đính chính (mục B).
3. Ngưỡng mặc định và cá nhân theo phiên bản; nhắc thiếu ngưỡng cá nhân (mục C).
4. Cảnh báo từ mọi nguồn: khóa loại, gộp (DBR-18), hạn tiếp nhận theo mức, leo thang ba cấp, nhắc khi đã leo thang tối đa, báo lặp lại, tự đóng khi nguồn đã xử lý, chuyển người phụ trách theo bàn giao (mục D).
5. Quy tắc xu hướng: sụt cân, bỏ lỡ lần đo theo lịch, mất ngủ kéo dài, từ chối thuốc (mục E).
6. Sự cố ba mức: ghi nhận (kể cả ngoài phạm vi), diễn biến, đổi mức, theo dõi, đóng, hủy và đổi loại bằng đính chính (mục F).
   *(Bổ sung 2026-10-11, FR-045a, Q-339)* Lệnh "Giao người xử lý" với khóa riêng `SU_CO, GIAO`, kiểm phạm vi của người giao và người được giao; lịch sử ở `phan_cong_su_co`.
7. Quy trình khẩn cấp: thẻ thông tin khẩn cấp, đối chiếu nguyện vọng trước biện pháp hồi sức, thông báo song song, chuyển viện kèm bản tóm tắt (mục G).
8. Nguy kịch: dấu nguy kịch, cảnh báo không gộp, xác nhận lại nguyện vọng, lựa chọn thực hiện (mục G1).
9. Sự cố ngã và khoảng theo dõi sau ngã (mục H); sự cố lây nhiễm, tra cứu truy vết, danh sách tiếp xúc, khoanh vùng (mục I).
10. Giấy phép hoạt động của cơ sở làm căn cứ cho BR-M06-05; ghi khám và điều trị (mục J); dữ liệu cho 002, 003, 004, 005, 008, 012, 016 (mục K).
11. *(Bổ sung 2026-10-18, Q-369)* Nút SOS của người cao tuổi: tạo cảnh báo Khẩn cấp từ thiết bị đã gắn, điều kiện gửi, người nhận thêm khi ở Hoạt động bên ngoài, trạng thái cho người cao tuổi (FR-047d → FR-047g).

Hướng kỹ thuật:
- Hai gói Modulith `m06` và `m05`, một chiều `m05 → m06`. Kết quả so ngưỡng sang `m05` bằng sự kiện `ChiSoDaXacNhan` sau commit (research S-01).
- Bản ghi chỉ số là nhóm 3 có bản nháp: `CHO_DO_LAI` chưa có `thoi_diem_xac_nhan`; trigger cho đặt đúng một lần (S-06).
- Gộp cảnh báo tuần tự theo người bằng khóa `theo_doi_nct`, lưới an toàn là cột sinh `khoa_mo` với `UNIQUE (nct_id, khoa_mo)` (S-03, DBR-18). Mỗi phát sinh là một dòng `nguon_canh_bao`; dòng này dùng cho số lần, báo lặp lại 24 giờ trượt và tự đóng theo nguồn (S-08, S-09).
- Sự cố tách thành `su_co` (nội dung nhóm 3, không `UPDATE`) và `su_co_xu_ly` (trạng thái nhóm 2) (S-11).
- Thẻ thông tin khẩn cấp dựng từ 001, 003, 006, 012 trong lệnh kích hoạt; bản chụp lưu cùng sự cố; chuyển viện gọi `TrangThaiNguoiCaoTuoiApi.thucHien(CHUYEN_VIEN)` của 001 trong cùng transaction (S-12).
- Một job theo phút mỗi module cho hạn tiếp nhận, leo thang (một cấp mỗi lần, FR-084), chờ đo lại, chuyển trạng thái lịch đo và ngưỡng; một job hằng ngày nhắc thiếu ngưỡng (S-21).
- 008, 009, 012, 014 làm plan sau; dữ liệu ca, Bác sĩ trực, người liên hệ chính, lượt thăm, điểm danh, nhật ký gửi lấy qua SPI khai báo ở `m05.api` (S-02).
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

- **Nút SOS** (FR-026 (d), FR-047d → FR-047g, US12): `POST /sos` ở `m05.canhbao`, không qua phiên; xác định người cao tuổi bằng token thiết bị qua `ThietBiGanApi.xacDinh` của 002. Mỗi lần bấm là một dòng `lan_bam_sos` (nhóm 3) và một dòng `nguon_canh_bao` loại `SOS`; cảnh báo Khẩn cấp loại `NCT_YEU_CAU_TRO_GIUP`, khóa loại `SOS` theo người, gộp theo đường tạo cảnh báo sẵn có (research S-23).
- **Điều kiện gửi**: trạng thái người cao tuổi qua `NguoiCaoTuoiApi.trangThai` (001); người bán trú qua `CoMatBanTruApi.theoNgay` (005, đã có); chuyến đang diễn ra, trưởng đoàn, người đi cùng qua SPI mới `ChuyenDiSosPort` (014 cài, 014 FR-068a).
- **Trạng thái cho người cao tuổi**: `GET /sos/hien-tai` (token thiết bị), app hỏi lại mỗi 5 giây khi màn trạng thái đang mở; lỗi từ chối trả kèm số liên hệ của viện và 115 từ `SoLienHeVienApi`.
- FR-081: `CanhBaoThongKeApi.canhBao` thêm `nguon` để 016 tách SOS. Thêm một bảng, một SPI; không đổi vòng đời cảnh báo.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 006: Spring Boot 4.1.x, Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock, Spring Security 7. Web: React 19, TanStack Query. App nhân viên: Flutter, drift (hàng đợi ngoại tuyến, 000 R-13). App người thân: Flutter. Không thêm thư viện mới.

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
| X. Kiến trúc | Đạt | Một khối; gói `m05`, `m06`; module khác chỉ dùng `m05.api`, `m06.api`. Không có vòng phụ thuộc (S-02). REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | So ngưỡng, gộp, leo thang, chặn hồi sức, quyền ngoài phạm vi, kiểm tra giấy phép ở backend. Mỗi lệnh có `POST …/{id}/<lenh>` riêng; chuyển viện dùng lệnh của 001 |
| XII. Dữ liệu | Đạt | DBR-18, 19, 34 bằng `UNIQUE` cột sinh và trigger; nhóm 3 theo mẫu 000; nhật ký trong transaction; `Clock`; `thoi_diem_thiet_bi` cho ngoại tuyến (DBR-25) |
| XIII. Tham số | Đạt | Như V. Đổi khoảng vật lý, ngưỡng không đổi kết quả so đã lưu (FR-002, FR-023) |
| XIV. Ngoại tuyến, thông báo | Đạt | Ghi chỉ số ngoại tuyến theo Q-01 qua khung 002; sự cố, kích hoạt khẩn cấp bắt buộc trực tuyến. Mọi thông báo là sự kiện tới 009, có mức (S-19) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 007 có test (data-model mục 15). BR-M15-20 cho ghi chỉ số ngoại tuyến có thêm test Flutter |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có sáu bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Hai gói `m05`, `m06` cho một feature.
2. Bảng `chi_so_nct`, `theo_doi_nct` làm đích khóa theo người.
3. `nguon_canh_bao` một dòng mỗi phát sinh.
4. Tách `su_co` và `su_co_xu_ly`.
5. Bốn SPI khai báo ở `m05.api` cho 008, 009, 012, 014.
6. Ba job và bảng `lan_chay_m05`, `lan_chay_m06`.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 007 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 người, 60 người dùng đồng thời giờ giao ca; chỉ mục ở data-model mục 5, 7 |
| NFR-02 Hiệu năng | Có | Ghi chỉ số một truy vấn ngưỡng; thẻ khẩn cấp đọc song song |
| NFR-03 Thông báo khẩn cấp | Có | Sự kiện ghi cùng transaction của lệnh tạo sự cố, cảnh báo Khẩn cấp, dấu nguy kịch; 009 gửi song song mọi kênh |
| NFR-04 Tác vụ định kỳ | Có | Job theo phút; khóa dòng, `UNIQUE` lần nhắc, `su_kien_canh_bao_da_nhan` chống trùng; chạy bù một cấp mỗi lần |
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
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m06/
├── api/           # ChiSoApi, LichDoApi, SoLienHeVienApi, ChiSoThongKeApi, sự kiện
├── danhmuc/       {internal, web}   # chỉ số, thành phần
├── lichdo/        {internal, web}   # lịch đo, ChuoiMocDo, NguonCongViecPort LAN_DO
├── chiso/         {internal, web}   # ghi, SoNguong, đo lại, ChiSoPort, NgoaiTuyenHandler, đính chính
├── nguong/        {internal, web}   # phiên bản, KiemTraNguong, nhắc thiếu ngưỡng
├── giayphep/      {internal, web}   # giấy phép cơ sở, GiayPhepCoSoApi, số liên hệ
├── kham/          {internal, web}
├── trangthai/     {internal}        # bean ThamGiaChuyenTrangThai
└── lich/                            # ViecTheoPhutM06, ViecHangNgayM06

backend/src/main/java/com/cambae/backend/m05/
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

mobile/lib/nguoi-than/suc-khoe/   # sự cố, chỉ số khi có bản đồng ý (dữ liệu qua 012)
```

**Structure Decision**: Theo R-02 của 000. Feature 007 nằm ở hai module `backend/…/m06` và `backend/…/m05`, mỗi module có gói `api`, gói `trangthai` (SPI cài cho module thấp hơn) và gói `lich`. Web có đủ chức năng. App nhân viên có ghi chỉ số ngoại tuyến, xử lý cảnh báo, ghi sự cố và kích hoạt khẩn cấp trực tuyến. App người thân chỉ xem và nhận thông báo.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Hai gói `m05`, `m06` cho một feature | Feature 007 gồm hai module của tài liệu nguồn; 002 cần giấy phép cơ sở (M06) mà không phụ thuộc M05 | Một gói: dữ liệu M06 nằm sai module (constitution X) |
| `chi_so_nct`, `theo_doi_nct` (một dòng mỗi người) | Đích khóa tuần tự cho gộp cảnh báo (DBR-18, SC-003), dấu nguy kịch (DBR-34), ngã, ngưỡng cá nhân, kích hoạt khẩn cấp đồng thời | Khóa `nguoi_cao_tuoi` của M01: truy cập dữ liệu module khác; chỉ dùng `UNIQUE`: số lần gộp sai khi thử lại |
| `nguon_canh_bao` một dòng mỗi phát sinh | Cần cho số lần, báo lặp lại 24 giờ trượt (FR-038a), tự đóng khi mọi nguồn đã xử lý (FR-032), nguồn cho checklist (005) | Đếm trên cảnh báo: mất mức và thời điểm từng lần; không biết nguồn nào đã xử lý |
| Tách `su_co` và `su_co_xu_ly` | Spec xếp nội dung nhóm 3 và trạng thái nhóm 2; DBR-19 cấm sửa nội dung sự cố Khẩn cấp | Một bảng trigger theo cột: dễ sót cột thêm sau, trái mẫu nhóm 3 của 000 |
| Bốn SPI khai báo ở `m05.api` (008, 009, 012, 014) | Các module này làm plan sau và gọi 007 (bàn giao, ghi sự cố, khoanh vùng); 007 cần lại dữ liệu ca, Bác sĩ trực, người liên hệ chính, lượt thăm, điểm danh, nhật ký gửi | 007 gọi thẳng API của họ: vòng phụ thuộc, Modulith từ chối |
| Ba job, `lan_chay_m05`, `lan_chay_m06` | SC-004 đòi leo thang ≤ 1 phút; FR-084 đòi chạy bù đúng một cấp; tạm nhận bàn giao chạy đúng một lần mỗi ca. Chu kỳ và giờ chạy là thông số vận hành, không có trong Phụ lục 25 | Bộ hẹn giờ trong bộ nhớ: mất khi khởi động lại, vẫn phải quét để chạy bù |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung hoặc dựa vào các điểm dưới đây; plan của feature liên quan giữ đúng hoặc ghi rõ thay đổi.

1. **001** (đã ghi bổ sung vào [../001-elder-profile-assessment/contracts/module-interfaces.md](../001-elder-profile-assessment/contracts/module-interfaces.md) mục 2): sự kiện `SuCoDaGhiNhan` thêm trường `canDanhGiaLai` (mức Trung bình trở lên hoặc loại Ngã, trừ đồ gửi; FR-043 b, 013 FR-022) và được phát lại khi sự cố nâng lên Trung bình hoặc đổi loại sang Ngã; 001 chỉ báo đánh giá lại khi `canDanhGiaLai = true`. `ChiSoApi.ganNhat` do `m06` cài. `TrangThaiNguoiCaoTuoiApi.thucHien(CHUYEN_VIEN)` dùng đúng bảng người gọi đã có, căn cứ là sự cố hoặc cảnh báo nguy kịch.
2. **005** (đã ghi bổ sung vào [../005-daily-care-tasks/contracts/module-interfaces.md](../005-daily-care-tasks/contracts/module-interfaces.md)):
   - `YeuCauCanhBao` thêm `loaiCanCu`, `canCuId` của công việc nguồn (FR-039b a);
   - sự kiện mới `CongViecDoBiBo(congViecId, nctId, lichDoId, chiSoIds, lyDo, mucQuanTrong)` khi công việc căn cứ `LAN_DO` đóng Không thực hiện (không do trạng thái cuối) hoặc còn Quá hạn khi hết ca (FR-039b b);
   - sự kiện mới `KetQuaNguDaGhi(ketQuaId, nctId, ngay, nhomGiaTri)` khi ghi hoặc đính chính kết quả ngủ (FR-039c);
   - hàm đọc mới `CongViecThongKeApi.nguoiDaGhiKetQua(nctId, tu, den)` (FR-060 c, CHK018).
   - 007 cài `NguonCongViecPort`, `ChiSoPort`, `CanhBaoDocPort` đúng chữ ký; gọi `nguonBiHuy` khi Ngừng lịch đo (theo 005 FR-022, điểm báo lại 2), kết thúc theo dõi sau ngã, loại người tiếp xúc; `nguonDaDoi` khi Thay đổi lịch đo; phát `SuCoNgaDaGhiNhan`, `LuaChonChamSocGiamNheDaGhi`, `LichDoDaDoi` như tên 005 đã đề xuất.
3. **006** (đã ghi bổ sung vào [../006-medication-management/contracts/module-interfaces.md](../006-medication-management/contracts/module-interfaces.md) mục 1.3): hàm đọc `LichThuocApi.nguoiDaGhiLieu(nctId, tu, den)` (FR-060 c). 007 nghe `YeuCauCanhBao`, `YeuCauDongCanhBao` của 006 với các loại đã khai báo.
4. **003**: 007 cài `TinhTrangLayNhiemChoPhanBo`; phát `DaThemVaoDanhSachTiepXuc(nctId, thoiDiem)` và `KhoanhVungDaGo` (thêm `vungId` vào đầu payload, 003 bỏ qua được); nghe `PhanBoDaDinhChinh`.
5. **004**: dùng mã điều kiện `KT_D_CANH_BAO_SU_CO`, `MucViecSauQuaDoiPort` mã `CANH_BAO_SU_CO`, `chiTiet` của `CHUYEN_VIEN` như đã khai báo; không đổi.
6. **008** cài `NhanSuCanhBaoPort`; gọi `CanhBaoBanGiaoApi.dangMo`, `khiXacNhanBanGiao` (MANDATORY) trong lệnh xác nhận bàn giao; đọc `ChiSoApi.vuotNguongTrongKhoang`; cung cấp danh mục phạm vi hành nghề cho giấy phép cơ sở. **009** cài `NhatKyThongBaoPort`; phân giải các nhóm người nhận ở research S-19 (gồm `BAC_SI_TRUC` nhiều người, `BAC_SI_TOAN_VIEN`, `NGUOI_TRONG_BAN_GHI_NGUON`, cờ `khongThay`); nghe `YeuCauDungNhacGoiDien`, `DaCoKetQuaXacNhanLai`; đọc `SoLienHeVienApi`. **012** cài `NguoiThanKhanCapPort`; nghe `KhoanhVungDaTao`, `SuCoDaHuyTrongBanTin`; đọc `KhoanhVungApi`, `SucKhoeCongNguoiThanApi`, `BanTinSucKhoeApi`. **013** gọi `SuCoLenhApi`; đọc `SuCoApi.xem`. **014** cài `DiemDanhTruyVetPort`; phát `m05.api.YeuCauTaoCanhBao` (thiếu người: Khẩn cấp); gọi `SuCoLenhApi.ghiNhan` cho "Báo thiếu người"; đọc `KhoanhVungApi`; nghe `KhoanhVungDaTao`. **011** nghe `SutCanPhatHien`; phát `YeuCauTaoCanhBao`, `YeuCauDongCanhBaoNguon`. **016** đọc `CanhBaoThongKeApi`, `ChiSoThongKeApi`, `KhoanhVungApi`.
7. *(Bổ sung 2026-10-18, Q-369)* **002** cung cấp `ThietBiGanApi.xacDinh` (002 module-interfaces 1.11). **005**: 007 thêm vào danh sách người dùng `CoMatBanTruApi.theoNgay` (đã ghi vào [../005-daily-care-tasks/contracts/module-interfaces.md](../005-daily-care-tasks/contracts/module-interfaces.md) mục 1.3). **014** cài `ChuyenDiSosPort` (014 FR-068a). **012** dựng nút SOS và màn trạng thái trên kênh người cao tuổi, gọi `/sos` (012 FR-085). **016** đọc `nguon` của `CanhBaoThongKeApi.canhBao`.

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
