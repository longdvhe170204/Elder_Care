# Implementation Plan: Nhân sự, ca trực và bàn giao

**Branch**: `008-staff-shift-handover` | **Date**: 2026-10-10, cập nhật 2026-10-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-staff-shift-handover/spec.md`

## Summary

Feature 008 là module M09 (mục 13.1 → 13.5; UC-49, UC-50 phần lập và công bố lịch, UC-51 → UC-53). Nó cung cấp:
1. Hồ sơ nhân viên, trạng thái làm việc, lệnh Cho nghỉ việc và tác động; bộ phận và Phụ trách bộ phận (mục A).
2. Loại giấy phép/đào tạo, phạm vi hành nghề, giấy phép và đào tạo của nhân viên với hiệu lực suy ra, cảnh báo hết hạn, thu hồi (mục A).
3. Mẫu ca, lịch ca tháng Nháp → Đã công bố, ca và vòng đời ca, người phụ trách ca, Bác sĩ trực; lệnh sau công bố: bổ sung, chuyển người phụ trách, ghi nhận vắng, hủy ca (mục B).
4. Giao Trưởng tầng, phân công trong ca, xác định nhân viên chính, kiểm tra chứng chỉ, sao chép phân công (mục C).
5. Tỷ lệ phục vụ theo trọng số chăm sóc và cảnh báo dưới ngưỡng (mục D).
6. Bàn giao ca do người phụ trách ca lập, danh sách bản ghi nguồn để tham khảo, hoàn tất, ý kiến, xác nhận nguyên tử với chuyển công việc và cảnh báo (mục E).
7. Nhiệm vụ và dữ liệu ca theo thời điểm cho 002, 003, 005, 006, 007, 009, 011, 013, 016 (mục F).
8. *(Bổ sung 2026-10-22, Q-382)* Import danh sách nhân viên bằng Excel: xem trước, hoặc toàn bộ hoặc không (FR-001b, FR-001c; UC-102).

Hướng kỹ thuật:
- Gói `m09` với bảy gói con; 015 (cùng M09) dùng port nội bộ, không qua API công khai (research N-01, N-07).
- 002 đã gọi tên `HoSoNhanVienApi`, `GiayPhepApi`, `DanhMucPhamViHanhNgheApi`, `PhanCongApi`, `NhiemVuApi`; chúng được khai báo dạng SPI ở `m15.taikhoan.api` và M09 cài, giữ chiều `m09 → m15` (N-02).
- Hiệu lực chứng chỉ, chuỗi ca liên tục, nhân viên chính, ca sau, tỷ lệ phục vụ, mục nghiêm trọng là hàm thuần kiểm thử bằng bảng (N-05, N-06, N-08 → N-10).
- Chồng ca, nhân viên chính trùng, Trưởng tầng chồng không đưa được vào `UNIQUE`; kiểm ở backend dưới khóa `nhan_vien` và `tang_ca_khoa` (N-03). DBR-20 bằng `UNIQUE (ca_id)`.
- Bàn giao: `ban_giao` giữ trạng thái; mục, nhận định là nhóm 3 có bản nháp, trigger khóa sau `chot_luc`; danh sách bản ghi nguồn là phép đọc song song sang 003 → 007, 013, 014 (N-10, Q-307).
- Xác nhận bàn giao gọi `CongViecCaApi.khiXacNhanBanGiao` (005) và `CanhBaoBanGiaoApi.khiXacNhanBanGiao` (007) trong cùng transaction (FR-044). Tạm nhận khi chưa xác nhận do 005, 006, 007 tự làm từ dữ liệu ca của 008.
- Một job theo phút, một job hằng giờ, một job hằng ngày (N-13).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 008 không lập ca, lịch, phân công, bàn giao hay mục bàn giao thay actor; không gợi ý người phụ trách ca;
- hệ thống chỉ: chuyển trạng thái ca, phân công theo giờ; tác động kèm lệnh của actor (nghỉ việc, vắng ca, thu hồi, xác nhận bàn giao); kiểm tra và chặn; nhắc, cảnh báo; hiển thị danh sách bản ghi nguồn và số liệu tỷ lệ phục vụ.

### Cập nhật 2026-10-22 (spec mục "Cập nhật 2026-10-22": Q-382; 007 FR-047i)

- **Import hồ sơ nhân viên** (FR-001b, FR-001c, BR-M09-18, CFG-M09-13): ba endpoint `GET /nhan-vien/import/mau` (file mẫu .xlsx), `POST /nhan-vien/import/xem-truoc`, `POST /nhan-vien/import/xac-nhan` (cả hai nhận file multipart). Hệ thống không lưu file gốc: xác nhận gửi lại cùng file, máy chủ đọc và kiểm tra lại mọi dòng rồi tạo mọi hồ sơ trong một transaction. Bảng `lan_import_nhan_vien` (nhóm 3); `nhan_vien` thêm `nguon`, `lan_import_id`, `dien_thoai_chuan` (số chỉ gồm chữ số, dùng so trùng) (research N-16, data-model mục 2).
- **Thư viện**: fastexcel (`fastexcel-reader` đọc, `fastexcel` ghi file mẫu; thư viện XLSX 010 đã chọn) qua `common.bieumau` (`ExcelBieuMau`, 007 research S-25); giới hạn kích thước file 5 MB và số dòng đọc.
- **Cho 007 FR-047i**: `NhanSuCanhBaoPort` thêm `dangTrongCa`, `tangTrongCa`, `tangDangCoCa` (007 module-interfaces mục 2.1); `TraCuuCaService` cài, cùng quy tắc "trong ca" với `CaTrucApi.dangTrongCa` (vai trò không xếp ca: giờ hành chính CFG-M13-06). Không thêm quy tắc.
- Không thêm vai trò, trạng thái; một bảng mới, ba cột mới.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 007: Spring Boot 4.1.x, Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock, Spring Security 7. Web: React 19, TanStack Query. App nhân viên: Flutter, drift (lưu bản xem bàn giao). *(Sửa 2026-10-22)* Dùng thêm `fastexcel-reader` (cùng họ fastexcel của 010) cho import Excel (FR-001b), qua `common.bieumau` (research N-16).

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test bảng cho sáu hàm thuần: `HieuLucChungChi`, `ChuoiCaLienTuc`, `XacDinhNhanVienChinh`, `XacDinhCaSau`, `TinhTyLePhucVu`, `MucNghiemTrong`.
- Test tích hợp MySQL cho `UNIQUE (ca_id)`, trigger bàn giao, trigger trạng thái nhân viên, cột sinh `UNIQUE` của bộ phận và người phụ trách ca.
- Test đồng thời: xếp một nhân viên vào hai ca; hai phân công chính; hai giao Trưởng tầng; Xác nhận và Gửi ý kiến cùng lúc.
- Đồng hồ giả lập cho CFG-M09-02 → 05, 08, 09, CFG-M15-07, ca qua đêm, ca qua tháng, hiệu lực tới hết ngày.
- Bản giả cho `DoGuiBanGiaoPort`, `ChuyenDiNhiemVuPort`, `KiemTraCongBoMoRong`.

**Target Platform**: Như 000.
- Web: mọi màn của 008, gồm hồ sơ nhân viên, bộ phận, chứng chỉ, mẫu ca, lịch ca, phân công, giao Trưởng tầng, tỷ lệ phục vụ, bàn giao.
- App nhân viên: ca của tôi; bàn giao (lập, ghi chú, hoàn tất, xác nhận, ý kiến; xem ngoại tuyến trong phạm vi ca); ghi nhận vắng ca của Người phụ trách ca; phân công cho Trưởng tầng.
- App người thân: không có màn của 008.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Tra cứu nhiệm vụ, ca, phân công theo thời điểm (gọi từ 002 mỗi yêu cầu, 005, 006, 007 trên đường nóng) ≤ vài mili giây: chỉ mục `ca (pham_vi, bat_dau, ket_thuc)`, `ca_nhan_vien (nhan_vien_id)`, `phan_cong (ca_id, trang_thai)`, `nguoi_phu_trach_ca (ca_id)`; bộ nhớ theo yêu cầu (002 A-07).
- Danh sách bản ghi nguồn bàn giao ≤ 3 giây (NFR-02): bảy lời đọc song song có giới hạn thời gian; nguồn lỗi trả "chưa có dữ liệu".
- Xác nhận bàn giao ≤ 2 giây với khoảng 30 người, 50 công việc tồn (SC-004 ≤ 1 phút).
- Tính tỷ lệ phục vụ một ca ≤ 1 giây (SC-007: cảnh báo ≤ 1 phút).

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI).
- Bàn giao đã chốt, đã xác nhận, ý kiến, lần tính tỷ lệ, bản công bố lịch không bị `UPDATE`, `DELETE`; hồ sơ nhân viên, chứng chỉ không xóa (constitution XII).
- Hoàn tất, ý kiến, bổ sung, xác nhận bàn giao bắt buộc trực tuyến (FR-060, Q-01).
- Nhật ký trong transaction (000 FR-047). CFG đọc lúc chạy (constitution XIII).
- Không tự sinh bản ghi nghiệp vụ (000 FR-010a).

**Scale/Scope**:
- 100 nhân viên, khoảng 15 tầng/khu vực × 2 ca mỗi ngày ≈ 900 ca mỗi tháng, khoảng 3.000 phân công mỗi tháng, 30 bàn giao mỗi ngày.
- 7 thực thể có vòng đời (nhân viên, lịch ca, ca, nhân viên trong ca, giao Trưởng tầng, phân công, bàn giao); 24 bảng.
- 3 job (theo phút, hằng giờ, hằng ngày).
- 45 đường dẫn, 57 thao tác REST ([contracts/api-008.yaml](contracts/api-008.yaml)).
- 2 SPI 008 khai báo; 13 SPI 008 cài cho module thấp hơn; 3 port nội bộ cho 015; 3 giao diện công khai.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR. Data-model mục 11 là bảng truy vết DBR-20, 21, 23, 25, 36 và BR-M09-01 → 08 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Mẫu ca, loại chứng chỉ, bộ phận, hồ sơ nhân viên (phần mô tả) theo mẫu nhóm 1. Trạng thái nhân viên, ca, phân công, giao Trưởng tầng, bàn giao chỉ có lệnh. Lịch Nháp sửa tự do đúng bảng phân nhóm của spec |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 2, 4, 5, 7 khớp FR-003, FR-022, FR-027, FR-042. Mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M09-01 → 05, 08, 09, 12, CFG-M15-07 đọc từ `ThamSoApi`. Chu kỳ job là thông số kỹ thuật (Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Khóa quyền theo bảng FR-048 (N-12); 19.3 cho Nhân viên chăm sóc xem bàn giao trong phạm vi. Không thêm vai trò |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ. Mâu thuẫn về nhật ký xem bàn giao và câu chữ còn sót trước Q-307 được báo lại (điểm 1, 2) |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-012 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 002, 003, 005 được bổ sung khai báo (mục "Đồng bộ") |
| X. Kiến trúc | Đạt | Một khối; gói `m09`; module khác chỉ dùng `m09.api` và các SPI họ khai báo. Không có vòng phụ thuộc (N-02). REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Chồng ca, chứng chỉ, nhân viên chính, công bố, điều kiện bàn giao kiểm ở backend. Mỗi lệnh có `POST …/{id}/<lenh>` riêng |
| XII. Dữ liệu | Đạt | DBR-20 bằng `UNIQUE`; DBR-21, DBR-36 bằng kiểm tra dưới khóa và `UNIQUE` cột sinh; nhóm 3 theo mẫu 000; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V. CFG-M09-01 theo mẫu ca, CFG-M09-12 trọng số theo mức (kiểu bảng) |
| XIV. Ngoại tuyến, thông báo | Đạt | 008 không có ghi ngoại tuyến (FR-060); bàn giao xem ngoại tuyến trong phạm vi ca. Mọi thông báo là sự kiện tới 009 với mức của FR-058 |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 008 có test (data-model mục 11). Không có BR thực hiện ở client; test Flutter kiểm hiển thị và chặn ngoại tuyến |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có năm bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Năm giao diện của 002 khai báo dạng SPI ở `m15.taikhoan.api`.
2. Port nội bộ M09 cho 015.
3. Bảng `tang_ca_khoa`.
4. `ban_cong_bo_lich` lưu bản gốc lúc công bố.
5. Ba job và bảng `lan_chay_m09`.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 008 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 100 nhân viên, 60 người dùng đồng thời giờ giao ca; chỉ mục ở data-model mục 4, 5 |
| NFR-02 Hiệu năng | Có | Tra cứu nhiệm vụ theo chỉ mục; danh sách nguồn đọc song song |
| NFR-03 Thông báo khẩn cấp | Không | FR-058: 008 không có thông báo Khẩn cấp |
| NFR-04 Tác vụ định kỳ | Có | Job theo phút; cột "đã làm lúc" chống gửi trùng; chạy bù khi khởi động |
| NFR-05 Khả dụng | Có | Bàn giao xem ngoại tuyến; job chạy bù |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL |
| NFR-07 Lưu trữ | Có | Bàn giao giữ theo người cao tuổi có mục; dữ liệu nhân sự theo CFG-M15-04 (data-model mục 10) |
| NFR-08 Bảo mật | Có | Trường hạn chế của hồ sơ nhân viên, lý do vắng ca, nội dung sức khỏe của bàn giao che theo 002; bản lưu trên thiết bị xóa khi hết phạm vi ca |
| NFR-09 Thời gian | Có | `Clock`; hiệu lực chứng chỉ tới hết ngày theo Asia/Ho_Chi_Minh; ca qua đêm, qua tháng tính trên thời điểm tuyệt đối |
| NFR-10 Thiết bị | Có | App nhân viên cho bàn giao, ghi vắng; web đủ chức năng |
| NFR-11 Ngôn ngữ | Có | dd/MM/yyyy HH:mm; lỗi chặn nêu ca bị chồng, chứng chỉ thiếu, mục thiếu ghi chú |
| NFR-12 Cấu hình | Có | Mọi CFG-M09 đổi qua tham số; mẫu ca, loại chứng chỉ qua màn cấu hình |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả bật tắt được |

## Project Structure

### Documentation (this feature)

```text
specs/008-staff-shift-handover/
├── plan.md                      # File này
├── research.md                  # Phase 0: N-01 → N-15
├── data-model.md                # Phase 1: bảng, bảng chuyển, bảo đảm ở cơ sở dữ liệu, truy vết
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-008.yaml             # OpenAPI 3.1: nhân viên, bộ phận, chứng chỉ, mẫu ca, lịch ca, ca, phân công, tỷ lệ, bàn giao
│   └── module-interfaces.md     # Giao diện Java: SPI 008 cài, API cung cấp, SPI khai báo, port cho 015, sự kiện
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m09/
├── api/           # CaTrucApi, NhanSuThongKeApi, SPI (DoGuiBanGiaoPort, ChuyenDiNhiemVuPort), sự kiện
├── nhanvien/      {internal, web}   # hồ sơ, trạng thái, nghỉ việc, bộ phận, Phụ trách bộ phận
├── giayphep/      {internal, web}   # loại, phạm vi hành nghề, chứng chỉ, HieuLucChungChi, cảnh báo
├── lichca/        {internal, web, port}   # mẫu ca, lịch, ca, ChuoiCaLienTuc, công bố, lệnh sau công bố, CaLenhPort
├── phancong/      {internal, web, port}   # giao Trưởng tầng, phân công, XacDinhNhanVienChinh, sao chép, PhanCongLenhPort
├── tyle/          {internal, web}   # TinhTyLePhucVu, lần tính, listener
├── bangiao/       {internal, web}   # bàn giao, mục, XacDinhCaSau, BanGhiNguonBanGiaoService, xác nhận
├── nhiemvu/       {internal}        # TraCuuCaService; bản cài NhiemVuApi, PhanCongApi, HoSoNhanVienApi, GiayPhepApi,
│                                    # ThamChieuViTri, NhanSuCaPort, DieuDuongCaPort, NhanSuCanhBaoPort
└── lich/                            # ViecTheoPhutM09, ViecHangGioM09, ViecHangNgayM09

frontend/src/tinh-nang/
├── nhan-vien/               # hồ sơ, bộ phận, chứng chỉ
├── lich-ca/                 # mẫu ca, lịch Nháp, xem trước công bố, lệnh sau công bố
├── phan-cong/               # phân công, sao chép, giao Trưởng tầng
├── ty-le-phuc-vu/
└── ban-giao/                # lập, danh sách nguồn, mục, hoàn tất, xác nhận, ý kiến

mobile/lib/nhan-vien/
├── ca-cua-toi/
├── ban-giao/                # xem ngoại tuyến trong phạm vi ca; lệnh chỉ trực tuyến
├── vang-ca/
└── phan-cong/               # cho Trưởng tầng
```

**Structure Decision**: Theo R-02 của 000. Feature 008 nằm ở `backend/…/m09` với sáu gói con nghiệp vụ, gói `nhiemvu` gom bản cài SPI cho module khác, gói `lich` và gói `api`. Feature 015 thêm gói con vào cùng `m09` và dùng port nội bộ. Web có đủ chức năng. App nhân viên có ca của tôi, bàn giao và ghi vắng ca. App người thân không có màn của 008.

## Ghi chú triển khai

### 2026-10-23 — Chuẩn bị trước Phase 1 (E)

- **Trạng thái**: A đã duyệt bản dịch hợp đồng và glossary mục 14 theo xác nhận của E trong phiên làm việc ngày 2026-10-23. Phase 1 T001–T002 đã hoàn thành và qua kiểm thử cuối phase; các task từ T003 chưa được đánh hoàn thành. Chưa tạo PR vào develop.
- **Phạm vi bản nháp**: dịch nốt đường dẫn, schema/response, tham số, trường JSON, enum, mã lỗi và tên khai báo phiên sang tiếng Anh. Giữ các tên đã dịch của hồ sơ nhân viên, `Reason` và `ReasonBody`; bảng đối chiếu đầy đủ ở glossary mục 14. Không thêm hoặc bớt thao tác, trường hay quy tắc.
- **Đối chiếu đã chạy**: YAML hợp lệ, không khóa trùng; đủ 48 đường dẫn, 60 thao tác HTTP, 24 schema, 199 tham chiếu nội bộ phân biệt hoa thường. So sánh cấu trúc trước/sau theo bảng đổi tên xác nhận giữ nguyên kiểu, `required`, `format`, giới hạn, enum, HTTP status, `x-fr`, `x-br` và các mã nghiệp vụ. Glossary ngoài mục 14 được giữ nguyên.
- **Thứ tự**: thực hiện Phase 1 T001 → T002 bằng test trước, chạy test của task cùng các kiểm tra kiến trúc; cuối phase mới chạy `clean verify` và đánh `[x]`.
- **Áp dụng R-02 khi triển khai**: chỉ tạo gói có lớp, dùng `api/{dto,spi,events}`, `web/{request,response}`, `internal/.../{service,domain,persistence,config}`, `scheduling`; không tạo các gói tiếng Việt từ đường dẫn logic trong tasks. Bản giả do 008 khai báo đặt ở `support/fake/m09/`.
- **Điều chỉnh kỹ thuật T001**: giữ module gốc đã có, tạo gói có lớp theo R-02; không tạo các gói rỗng chỉ để đủ cây thư mục logic cũ. Luật kiến trúc có test với trường hợp được phép/bị cấm để vẫn kiểm chứng được khi Phase 1 chưa có dịch vụ nghiệp vụ.
- **Điều chỉnh kỹ thuật T002/T003**: đưa khai báo tối thiểu của hai SPI đồ gửi/chuyến đi đã có trong hợp đồng từ T003 lên T002 để bản giả biên dịch được; T003 còn API tra cứu ca, thống kê và sự kiện. `KiemTraCongBoMoRong` là SPI nội bộ theo N-07, nằm ở `m09.shift.internal.service.port` vì phần 015 cài và lệnh công bố của 008 gọi. Luật "chỉ 015 gọi port" áp cho port lệnh ca/phân công/lịch nháp, không áp cho SPI kiểm tra mở rộng có chiều gọi ngược này.

### 2026-10-23 — Phase 1 T001, T002 (E)

- **Phạm vi**: hai luật `M09ArchTest`, test chứng minh luật bắt được vi phạm, hai SPI công khai + kiểu dùng riêng, SPI kiểm tra công bố nội bộ và ba bản giả dùng chung. Chưa có controller, migration, job hay dịch vụ nghiệp vụ; T003 vẫn chưa xong.
- **Khai báo SPI**: `HeldBelongingsHandoverPort`, `TripDutyPort` cùng record ở `m09.api.spi`, có `@NamedInterface` cho `api`, `api.spi`. Chữ ký giữ đúng hợp đồng mục 2.1, dịch tên theo glossary 14.6. Các view trước đây chỉ có tên, chưa có cấu trúc: chọn các id tối thiểu và vị trí lưu giữ (013 mục 1.1), nhân viên đang đi và giờ về dự kiến (FR-039 e); không thêm luật lọc hoặc quyền mới.
- **Kiểm tra công bố**: `ExtendedPublicationCheck`, `PublicationWarning` ở `m09.shift.internal.service.port`; cảnh báo giữ `code`, `description`, `details` như schema `Warning`. Đây là SPI nội bộ cho 015, không mở ra module khác. Không dùng port lệnh cho chiều gọi ngược này.
- **Bản giả**: `FakeM09Config` là `@TestConfiguration`, ba bean `@Primary`, chỉ test import mới có. Dữ liệu mặc định rỗng; test chủ động đặt dữ liệu, chuyển nguồn có/không dữ liệu qua `available`. Không thêm controller thử nghiệm vào hợp đồng HTTP. Bản giả chuyến lọc khoảng đi thực tế `[departedAt, returnedAt)` giao khoảng ca và đúng tầng/khu; `returnedAt = null` giữ chuyến đang đi kể cả quá giờ về dự kiến. Danh sách đầu vào được sao chép để test không vô tình đổi dữ liệu đã đặt.
- **TDD T001**: viết `M09ArchRuleTest` và các lớp thử trước; lệnh `./mvnw -o -B '-Dtest=M09ArchRuleTest,*ArchTest,ModuleBoundaryTest' test` đỏ ở bước biên dịch do chưa có `M09ArchTest`. Sau khi cài luật: 27 test qua, 0 lỗi/thất bại/bỏ qua. Bốn test riêng kiểm lời gọi được phép, bị cấm, năm lệnh tạo bản ghi trong job và chuyển trạng thái được phép.
- **TDD T002**: viết `FakeM09Test` trước; lệnh `./mvnw -o -B '-Dtest=FakeM09Test,*ArchTest,ModuleBoundaryTest' test` đỏ ở bước biên dịch do chưa có SPI và bản giả. Sau khi cài: 34 test qua, 0 lỗi/thất bại/bỏ qua (11 test bản giả). Có kiểm phạm vi người giữ, thay người giữ/trả đồ, chuyến tiếp giáp ca, chuyến chưa về đã quá giờ, nguồn không có dữ liệu và `@Primary` khi cùng tồn tại bản cài khác.
- **Kiểm tra cuối phase**: `./mvnw -o -B spotless:apply` thành công, chỉ định dạng 20 file Java mới của M09. Sau đó chạy nền `./mvnw -B clean verify` trên JDK 25.0.4.1/MySQL 8.0 bằng Maven wrapper Windows (`.\mvnw.cmd`): **BUILD SUCCESS**, 174 test đơn vị/kiến trúc + 426 test tích hợp = 600 test, 0 lỗi/thất bại/bỏ qua; Spotless check qua. Chỉ sau kết quả này mới đánh T001–T002. Biến kiểm thử đọc từ `.env` cục bộ được Git bỏ qua, schema xác nhận là `eldercare_test` trên MySQL cục bộ; không ghi giá trị bí mật vào tài liệu.
- **Bàn giao và bước sau**: đã ghi "Đổi api" và việc B (013), C (014), E (015) cần cài vào team-plan mục 8; các bản cài SPI của 002, `CaTrucApi` và dữ liệu demo vẫn chờ đúng phase. Phase 2 bắt đầu từ T003 (API tra cứu ca, thống kê và sự kiện), dùng lại hai SPI có sẵn. Không thay giao diện nên không chụp màn hình, không chạy kiểm tra frontend. Chưa tạo PR vào develop.

### 2026-10-23 — Phase 2 hoàn thành (E)

- T003 test trước: `M09PublicApiTest` đỏ ở biên dịch khi chưa có API/sự kiện; sau khai báo `ShiftApi`, `StaffStatisticsApi`, các view/sự kiện và bản giả thì 27 test (4 test task + ArchTests) qua. Khai báo sự kiện nhận danh sách yêu cầu thông báo do dịch vụ nguồn dựng ở story tương ứng; sự kiện không mang lý do vắng/nghỉ việc. Khóa sự kiện gồm loại, id, thời điểm. `ShiftApi` chưa có bản truy vấn thật (T015).
- T004 test trước: `StaffSchemaIT` đỏ 5 test vì thiếu bảng; sau migration `V008_01__staff_credentials.sql`: 5 test MySQL + 23 ArchTests qua. Mã loại mặc định `DOCTOR_LICENSE`, `NURSING_LICENSE`, `FIRST_AID_TRAINING`; `practice_scope` thêm id kỹ thuật để khớp SPI 002 đang dùng id, vẫn giữ code. Tham chiếu liên module là id logic, chưa thêm khóa ngoại. Các cột import trong hồ sơ chuẩn bị theo data-model hiện hành; bảng lần import và lệnh import thuộc T046–T048.
- T005 test trước: `CredentialValidityTest` đỏ vì chưa có hàm; sau cài 4 test task + 23 ArchTests qua. `CredentialValidity` ở `credential.internal.domain`, nhận thời điểm và `BusinessTime`, không tự đọc đồng hồ. Hết hạn tại 00:00 ngày kế tiếp theo giờ Việt Nam, kiểm khoảng đóng gồm cả hai đầu.
- **Đổi api/phần A đã được người dùng duyệt trong phiên này**: bổ sung `InfoGroup.STAFF_CONTACT`, `STAFF_RESTRICTED`, `STAFF_REASON` và bộ xét trường của 002 để thực hiện FR-052/FR-053; không dùng CONTACT chung vì quyền rộng hơn dữ liệu nhân viên. Test `StaffFieldVisibilityIT` viết trước, đã thấy đỏ vì chưa có nhóm. Hai nhóm đầu dùng ngay cho hồ sơ/chứng chỉ; `STAFF_REASON` mặc định đóng với người không phải Quản lý viện tới khi T039–T040 có dữ liệu người ghi/phạm vi ca để cài FR-053. Không tuyên bố đã hoàn thành quyền đọc lý do vắng ca ở Phase 2.
- T006 `StaffProfileHttpIT` đã đỏ với 404 khi chưa có controller; sau cài các test HTTP/quyền qua. Mô tả hồ sơ và Thêm chứng chỉ không thêm trường reason vì hợp đồng không yêu cầu; dùng lệnh ghi transaction có nhật ký với hành động không bắt buộc lý do. Đổi bộ phận dùng endpoint lệnh riêng ở phase sau, không thực hiện ngầm qua PUT mô tả.
- Người dùng đã duyệt giá trị nạp ban đầu: hai loại giấy phép có thời hạn/phạm vi, bắt buộc DOCTOR/NURSING_STAFF tương ứng; đào tạo sơ cứu có thời hạn, không có phạm vi, chưa gán vai trò bắt buộc. Đã duyệt bổ sung duy nhất `Credential.hiddenFields` (mảng chuỗi, tùy chọn, chỉ đọc) vào api-008. Test đã đỏ đúng lỗi thiếu `$[0].hiddenFields` sau biên dịch sạch, rồi thêm DTO/schema cùng thay đổi.
- T007 test trước `StaffSpiIT` đỏ vì chưa có bean SPI; sau cài `StaffDirectory`, `StaffLicenseLookup`, `PracticeScopeLookup`: 5 test MySQL + 23 ArchTests qua. `createMinimal` MANDATORY và nhật ký BOOTSTRAP; chức danh tự do không được tự gán vai trò. `LicenseApi.latest` chọn ngày cấp mới nhất, id làm thứ tự khi cùng ngày; loại nhận code hoặc id kiểu chuỗi. `expiringWithin` chỉ cấp giấy phép hành nghề cho job 002, đào tạo do story 008 riêng xử lý.
- T008 test trước `M09JobCatchUpIT` đỏ vì chưa có khung; sau cài: 3 test MySQL + 23 ArchTests qua. `M09TimedTask` là điểm đăng ký nội bộ cho story, `M09Jobs` điều phối transaction và `M09JobRuns` ở tầng persistence. Ba wrapper job có ShedLock; mốc hằng ngày là 00:10 giờ Việt Nam. Khóa dòng theo job/mốc, chỉ SUCCEEDED được bỏ trùng; lỗi rollback tác vụ, lưu FAILED để thử lại. Chạy bù mốc gần nhất, tác vụ xét tất cả dòng nguồn đã tới hạn, không tạo lại từng nhịp bị lỡ. Đăng ký ba tên job theo hướng dẫn ngay trong `SchedulerScopeArchTest`; chuyển Map.of sang Map.ofEntries vì vượt 10 mục; chỉ thêm khóa tắt job M09 trong profile test của application.yml.
- Đã gặp class do bộ biên dịch khác ghi vào target có lỗi "Unresolved compilation problems" dù nguồn hợp lệ; từ lượt kiểm tra bổ sung dùng clean + test của task + ArchTests để javac biên dịch lại toàn bộ, không sửa test cho xanh. Script nạp biến kiểm thử ở cache cục bộ được Git bỏ qua; không thêm thư viện hoặc sửa pom.
- T009: người dùng đã duyệt hai hàm đọc `ProfessionalActivityApi.usesCredentialType`, `usesPracticeScope` và bản cài/test trong phần A để giữ danh mục còn tham chiếu. Provider gắn bảng ca/phân công/bàn giao sẽ đăng ký cùng migration của story tương ứng; Phase 2 không tạo sớm các bảng hoặc truy vấn bảng chưa tồn tại. Phân loại trường cho nội dung bàn giao/lý do vắng ca có thể công bố trước vì không cần đọc bảng.
- T009 test trước `M09GovernanceIT` đỏ vì chưa có hai hàm đọc tham chiếu danh mục; sau cài 6 test MySQL + 23 ArchTests qua. Kiểm phân loại trường, gắn phạm vi, quyền tải scan (Quản lý viện/chính nhân viên), giữ hồ sơ đang làm và các tham chiếu, giữ danh mục đang được nghiệp vụ chuyên môn dùng, xoá chứng chỉ hết hạn cùng liên kết phạm vi bằng tài khoản lưu giữ. Provider `M09.STAFF` giữ hồ sơ còn tài khoản liên kết; phần bảng chưa tồn tại bổ sung ở T012/T019/T026. Tệp scan cần bản cài `FileApi.attach` của B (001); chưa có thì trả lỗi, không ghi chứng chỉ dở dang và không tạo thêm bản giả của B.
- T001–T002 giữ hoàn thành; T003–T009 đã đánh xong sau clean verify cuối phase. Chưa commit/push Phase 2.
- Kiểm tra toàn dự án lần đầu: 182 test đơn vị/kiến trúc qua; 452 test tích hợp có 4 thất bại. Ba test pháp lý của 002 bị ảnh hưởng bởi fixture nghiệp vụ chuyên môn của `M09GovernanceIT` chưa hoàn tác; sửa riêng test mới của M09 bằng transaction rollback, không sửa test/quy tắc của 002. `ApiContractIT` phát hiện đường dẫn mới chưa đăng ký: lấy sớm phần đăng ký hợp đồng của T042 vào T006, ghi rõ 9 operation đã cài và 51 operation còn chờ story; giữ nguyên mọi luật so cấu trúc schema/tham số/phản hồi.
- Sau sửa: 19 test HTTP/provider/pháp lý và 23 ArchTests qua. Thêm trước hai test hồi quy cho thời điểm tạo loại chứng chỉ và số liên hệ không chuẩn: đã thấy đỏ (ngày SQL khác ngày giả lập; khóa tra cứu chứa chữ), rồi sửa để thời điểm lấy từ `BusinessTime`, khóa tra cứu chỉ chứa tối đa 15 chữ số hoặc NULL, không sửa liên hệ gốc hay thêm luật chặn tạo tay. Metadata OpenAPI dùng đúng schema đọc/ghi chung, enum, mã phản hồi và header của hợp đồng; `Idempotency-Key` bắt buộc kiểu UUID ở bốn lệnh đã cài.
- Chủ dự án đã duyệt đồng bộ `Problem` của 008 với bản chung 000, giữ `warnings` của 008 (2026-10-23). Bổ sung required/type URI, `currentValue`, `candidates`, `failedLayer`, `license`, `unlocksAt`, `module`/`waivable` trong điều kiện. `M09ProblemBody` ở `web.response` mô tả đúng object phẳng của hợp đồng, dùng lại `UnmetCondition` và `ProblemBody.License`, thêm cảnh báo; không thay handler lỗi, không sửa schema 000/002. Mô tả ghép `allOf` không đối chiếu được với object phẳng trong bộ kiểm tra hiện tại nên giữ bản mô tả trực tiếp; không nới luật kiểm tra. Test đối chiếu đã đỏ trước khi sửa; sau sửa 8 test MySQL (`ApiContractIT`, 7 test HTTP) và 23 ArchTests qua.
- **Chốt Phase 2**: `./mvnw -o -B spotless:apply` qua; sau đó chạy nền `./mvnw -B clean verify` bằng Maven wrapper Windows, JDK 25/MySQL 8.0: **BUILD SUCCESS**, 182 test đơn vị/kiến trúc + 454 tích hợp = **636**, 0 lỗi/thất bại/bỏ qua. Đối chiếu cả api-000, api-002, api-008 qua; 51 operation 008 chờ story được giữ chính xác ở PENDING, không bỏ kiểm operation đã cài. Chỉ sau kết quả này mới đánh T003–T009. Không đổi giao diện nên không chụp màn hình, không chạy kiểm tra frontend. Đã cập nhật glossary mục 14, CLAUDE.md, team-plan mục 8; chưa commit/push hoặc mở PR Phase 2.
- **Bước tiếp**: Phase 3 T010–T016 (lập/công bố lịch, người phụ trách ca). Cài `ShiftApi` thật ở T015, provider lưu giữ/phạm vi lịch và ca cùng T012; phân công/nhiệm vụ ở T019–T021, bàn giao ở T026. Quyền lý do vắng ca hoàn thiện tại T039–T040. Các phần này chưa được tuyên bố hoàn thành trong Phase 2.

### 2026-10-24 — Phase 3 hoàn thành phần backend/web (E)

- Đầu phiên Git sạch, Phase 2 đã có commit `2a2ec09` trên nhánh `impl/008-staff-shift-handover`. Người dùng tự commit/push; AI chỉ sửa và kiểm thử phần được giao.
- T010 test trước `ContinuousShiftChainTest` đỏ ở biên dịch vì chưa có hàm; sau cài 6 test task + 23 ArchTests qua. `ContinuousShiftChain` ở `shift.internal.domain`, tính khoảng liên tục chứa ca đang xếp, nối tiếp giáp, không làm tròn thời gian và không sửa đầu vào. Ngưỡng do service đọc CFG-M09-02, hàm thuần không chứa ngưỡng cố định.
- T011/T012 viết trước `ShiftSchemaIT`, đỏ 4 ca vì chưa có bảng. Migration mới `V008_02__shift_schedules.sql` tạo `shift_template`, `shift_schedule`, `work_shift`, `shift_staff`, `shift_lead_history`, `shift_publication`. Khóa duy nhất lịch dùng `scope_key = COALESCE(scope_id,0)` để lịch toàn viện cũng không trùng tháng; thời lượng ca kiểm ở cả backend và MySQL; tham chiếu ngoài M09 vẫn là id logic. Migration đã qua kiểm tra MySQL.
- **Đổi api được người dùng duyệt trong phiên này**: khai báo tối thiểu đúng hợp đồng các SPI `ThamChieuViTri` (003), `NhanSuCaPort` (005), `DieuDuongCaPort` (006), `NhanSuCanhBaoPort` (007), cùng bản giả dùng chung do nhánh hiện chưa có API/bản giả các phần này. Ghi team-plan mục 8 để C/D review; không cài nghiệp vụ của C/D.
- **Đổi api đã được người dùng duyệt**: thêm `version` kiểu integer, int64, chỉ đọc vào ba schema `ShiftTemplate`, `ShiftSchedule`, `Shift` để client có nguồn lấy If-Match. Không thêm endpoint; giữ cơ chế ETag số phiên bản của common.
- T012 sau migration: 4 test MySQL + 23 ArchTests qua. T013 test trước `ShiftScheduleIT` đỏ vì chưa có service; sau cài 11 test MySQL + 23 ArchTests qua, gồm khóa chống xếp chồng đồng thời ở hai phạm vi, tiếp giáp, cảnh báo vượt CFG, ca 24 giờ (vẫn phải xác nhận cảnh báo vượt CFG), vai trò từ tài khoản, phủ Trưởng tầng liên tục cả tháng, rollback công bố, phiên bản cũ và thông báo chỉ chứa ca riêng của người nhận.
- Theo ngoại lệ rõ trong FR-055, sửa lịch Nháp không ghi nhật ký từng lần. Lịch sử Người phụ trách ca bắt đầu khi công bố, nên gỡ người/xoá ca Nháp không phải xoá bản ghi nhóm 3. Công bố lưu bản gốc và nhật ký, đánh selfApproved khi người lập tự công bố. Khóa nhân viên theo id tăng rồi mới khóa lịch/ca; phép đọc kiểm chồng dùng FOR UPDATE để thấy lệnh vừa commit dù transaction đã mở trước khi chờ khóa.
- Phần kiểm tra phân công, snapshot/phát hiệu lực phân công nối ở T019–T021 khi có bảng phân công; tỷ lệ phục vụ nối ở T037 khi có nguồn trọng số, không bịa tỷ lệ bằng 0. Điều kiện phủ Trưởng tầng đọc lịch sử từ SPI `AssignmentApi` của 002; chưa có bản thật T021 thì khi chạy thật chặn công bố lịch tầng thiếu dữ liệu, test dùng đúng `FakeAssignments` do A khai báo. Lịch toàn viện không có điều kiện này theo FR-016. T014 dùng port đọc trạng thái bàn giao của M09, chưa có T026 thì ca yêu cầu bàn giao phải chờ, không tự đóng.
- T014 đỏ trước vì chưa có port/trạng thái, sau cài 2 IT + 23 ArchTests qua: job chạy bù đúng một lần, không sinh ca/bàn giao, giữ Chờ bàn giao cho tới khi nguồn xác nhận đã lập. T015 đỏ trước khi có SPI/bản cài, sau cài 5 IT + 3 test bản giả chung và ArchTests qua; các bean giả chỉ thuộc test. Truy vấn nóng lọc SQL theo nhân viên/phạm vi/thời gian, dùng chỉ mục có sẵn; không tải toàn bộ lịch sử toàn viện rồi truy vấn thành viên từng ca. Chưa đo tải sản xuất SC-005 trong phase này.
- Hai regression bổ sung đã đỏ rồi xanh: FR-055 bản chụp công bố phải chứa trạng thái PUBLISHED vừa ghi; FR-013 nhân viên vắng từ một mốc vẫn giữ khoảng thực tế trước mốc để kiểm chồng/chuỗi ca. Lần kiểm liên quan gần nhất: 18 IT + 23 ArchTests qua (lịch, truy vấn, vòng đời).
- T009/T012 bổ sung cùng bảng mới: 6 provider lưu giữ, 2 provider phạm vi; tham chiếu từ hồ sơ nhân viên giữ thành viên ca, lịch sử phụ trách và ca đang trỏ người đó. Hai IT đỏ vì thiếu provider rồi xanh: không chọn ca chưa kết thúc, xoá đồ thị đúng thứ tự bằng tài khoản lưu giữ. Ca/lịch/bản công bố giữ lại tới khi ca con kết thúc; loại dữ liệu danh mục chỉ chọn khi ngừng dùng.
- T016: 13 thao tác HTTP đúng hợp đồng; `ApiContractIT` gỡ đúng 13 dòng PENDING, không giảm mức kiểm tra. HTTP đỏ 404 trước controller rồi 3 IT và đối chiếu API qua. `onDutyDoctor` đầu vào dùng Boolean vì trường không bắt buộc, tránh Jackson 3 từ chối khi thiếu; mặc định false tại bước chuyển vào domain. Lệnh sửa dùng If-Match, đọc ca có ETag; lịch/mẫu lấy version từ danh sách theo bổ sung đã duyệt. `/me/shifts` dùng khoảng ngày `[from, to)` theo giờ Việt Nam; giới hạn nhân viên chỉ ca có tên mình, người quản lý xem theo quyền phạm vi.
- Web ở `features/shifts`, đăng ký menu có sẵn trong App; client 008 sinh bằng `npm run gen:api`, chỉ thêm 008 vào danh sách hợp đồng đã duyệt. Dùng bố cục bảng lịch, Sheet form/chi tiết và bước xem trước công bố theo công thức đã chốt của DESIGN (quyền tự quyết kỹ thuật của người dùng); không thêm token màu/thư viện hay dữ liệu minh họa vào sản phẩm. Trạng thái ca dùng nhãn tiếng Việt với tông neutral mặc định, Đã đóng dùng settled có sẵn. 5 test Vitest (đỏ khi thiếu màn hoặc chưa đúng điều kiện lý do rồi xanh) kiểm xác nhận cảnh báo, điều kiện chặn công bố, lý do tự duyệt do backend quyết định, 412/tải lại và chế độ chỉ xem ca riêng.
- Kiểm trình duyệt Edge headless với API giả chỉ trong harness bỏ qua bởi Git: 1440/900/390 px không tràn chiều ngang trang, có cuộn riêng bảng; chế độ tối, Tab trong Sheet, Escape đóng và các vai trò AC-01/02/03/04/05/07/09 đúng chế độ theo quyền trả về, không lỗi JavaScript. Đây chưa phải kiểm UI kết nối backend thật/dữ liệu demo. Ảnh nằm trong `frontend/node_modules/.cache/008-shifts-*.png`, không đưa vào kho.
- Frontend sau thay đổi cuối: 105 test/17 file qua với `npm test -- --maxWorkers=1`; lint và build qua. Một lượt chạy đồng thời với Maven có test cũ `FR_026a_xemTaiThoiDiem_guiAsOf_chiXem` quá thời gian, chạy lại toàn suite bằng một worker thì xanh; không sửa test, không tăng thời hạn hay bỏ qua test. Build có cảnh báo kích thước chunk >500 kB, không chặn build.
- **Kết quả cuối phase**: Spotless qua; `./mvnw clean verify` chạy nền trên JDK 25/MySQL 8.0 qua 191 test đơn vị/kiến trúc + 481 IT = 672, 0 lỗi/thất bại/bỏ qua, gồm đối chiếu API và mọi ArchTest. Sau kết quả xanh mới đánh T010–T016 `[x]`. Mobile tách T016b, vẫn ngoài phạm vi triển khai theo CLAUDE.md. Phần nối phân công/giao Trưởng tầng, bàn giao và tỷ lệ đã ghi rõ tại T021/T026/T037; không báo chúng hoàn tất trong Phase 3. Chưa commit/push/PR Phase 3; người dùng tự thực hiện Git.

### Phase 4 hoàn thành backend/web — 2026-10-24 (E)

- Đầu phiên nhánh `impl/008-staff-shift-handover` sạch; Phase 3 đã được người dùng commit tại `ec85fd5`. AI không commit/push/merge/đổi nhánh.
- Đã đọc tasks T017–T022 và ghi chú Phase 3 trước, sau đó data-model/contracts/research N-03/N-08; tra FR-024–FR-033 và nghiệp vụ mục 13.3/13.4 khi cần. Người dùng đã duyệt ba thay đổi: khai báo tối thiểu API B/C, mở rộng SPI phân công và bản giả chung để B/C/D review; thêm `AssignmentView.scopeEndsAt` cùng test/phần tính phạm vi của A; thêm POST `/shifts/{id}/assignments/preview` nhận `NewAssignment`, trả `ResultWithWarnings` không có id, không ghi dữ liệu/nhật ký. Giữ constructor AssignmentView bảy tham số để các module hiện có không phải sửa.
- T017 test trước `PrimaryAssigneeResolverTest` đỏ do chưa có domain, sau cài 7 test + 23 ArchTests qua; thêm test đỏ rồi xanh cho việc xác định nhân viên chính trong khoảng lịch sử của phân công đã kết thúc (FR-037a). Hàm thuần ở `assignment.internal.domain`, nhận vị trí đã xác định từ nguồn, không gọi module khác hay đọc đồng hồ; đối tượng hẹp ưu tiên trước giới hạn loại công việc. Hai nguồn cùng ưu tiên mâu thuẫn báo lỗi dữ liệu nguồn, không chọn theo thứ tự. 200 hoán vị chỉ kiểm tính ổn định FR-028. SC-005 được kiểm riêng bằng 200 trường hợp trong `AssignmentIT`: chứng chỉ hợp lệ, thiếu và hết hiệu lực giữa ca qua đêm, qua service/quyền/nguồn thật của M09 trên MySQL; không tính 200 vòng lặp là 200 test JUnit.
- T018/T019 `AssignmentSchemaIT` đỏ 4 test vì thiếu bảng. Migration mới `V008_03__staff_assignments.sql` tạo `floor_scope_lock`, `floor_manager_assignment`, `staff_assignment`, chỉ FK nội bộ M09; trạng thái cuối không sửa/xoá qua tài khoản ứng dụng. Lệnh trạng thái dùng khóa và kiểm trạng thái theo hợp đồng hiện tại, không tự thêm If-Match/version vào HTTP. Ngày kết thúc giao Trưởng tầng được lưu thành biên mở đầu ngày kế tiếp theo R-04 (kịch bản thay người từ ngày sau trong quickstart Q24); khoảng giờ phân công dùng `[from,to)`.
- T018/T020 test trước `AssignmentIT` đỏ vì thiếu service, sau cài kiểm nhân viên trong ca và khoảng hiện diện, vai trò hệ thống, Điều dưỡng làm việc chăm sóc, nguồn vị trí theo thời điểm, chứng chỉ toàn khoảng, nhiệm vụ Điều dưỡng phụ trách đọc loại qua `ProfessionalActivityApi`, cảnh báo công việc không đủ điều kiện, sao chép từng dòng. Khóa đăng nhập tạm thời không tự gỡ điều kiện phân công của nhân viên (BR-M15-05); không có ca không tự khóa tài khoản (BR-M09-17). Đã có ca kiểm hai lệnh phân công chính và hai lệnh giao Trưởng tầng đồng thời, kết thúc sớm hoàn tác nếu tác động 005 thất bại. API thiếu bản thật thì dùng đúng fake chung trong test, không có bean giả trong ứng dụng.
- Khóa theo N-03: toàn bộ nhân viên tăng dần → gom toàn bộ tầng/khu rồi khóa tăng dần → lịch → ca → phân công. Sao chép đọc lại nguồn sau khóa, nguồn đổi thì báo STATE_CHANGED; chỉ bỏ từng dòng khi bước kiểm nghiệp vụ thất bại, không bắt lỗi ghi SQL hoặc lỗi tác động 005 để tiếp tục một transaction đã hỏng. Sao chép lập dòng mới dùng giờ mặc định của ca đích theo FR-025. Đọc hiện hành FOR UPDATE sau khóa tránh dùng snapshot thành viên cũ khi lệnh khác vừa hoàn thành.
- T021 test trước các chỗ nối công bố/xóa Nháp: đã thấy đỏ do chưa chuyển trạng thái, snapshot thiếu phân công, chưa xóa bản ghi con và chưa có tham chiếu vị trí. Sau cài, công bố kiểm lại, gỡ đúng các id Nháp người dùng chọn, chuyển hiệu lực và lưu toàn bộ phân công cùng bản gốc; gỡ thành viên/xóa ca Nháp gỡ phân công Nháp tương ứng. Không coi người chỉ có tên trong ca là đã được phân công cả tầng; người phụ trách ca có phạm vi riêng theo SPI 002. `AssignmentApi` và `DutyApi` là bản thật, gồm nhận theo người/phòng, thay Trưởng tầng và bộ phận; phần bàn giao nối T026–T028, tỷ lệ nối T037 như ghi chú Phase 3.
- `DepartmentDutyService.replaceLead` và `DutyDirectory.replaceDepartmentLead` tham gia transaction của lệnh gọi bằng MANDATORY, kiểm người thay đang làm việc/cùng bộ phận/tài khoản Hoạt động, loại Quản lý viện và Kế toán, Hành chính – Kế toán yêu cầu Hành chính; đóng/mở lịch sử và nhật ký cùng lần. Đây là tác động kèm lệnh 002 đã kiểm quyền, không phải một lệnh gốc thứ hai: không gắn BusinessCommand rồi nới RequiresPermissionArchTest. Vẫn kiểm Reason trực tiếp; đã kiểm hoàn tác cùng transaction ngoài bằng 8 IT.
- `ResidentFinalAssignmentEffects` nghe `ResidentStatusChanged` của 001 theo N-08, cùng transaction lệnh nguồn: kết thúc/hủy phân công trực tiếp, giữ khoảng lịch sử, gọi tác động công việc trong cùng lần. Sau khi người dùng báo 001 đã lên, fetch và đối chiếu `origin/develop` tại `b049b1f` (001 vào develop qua PR #23, merge `dc0c452`): đồng bộ đúng `ResidentApi.isFinalStatus`, các DTO trong chữ ký, sự kiện dùng `ResidentStatus`/`StatusCommand` và `FakeResidents` do B viết; thay khai báo tối thiểu tạm trước đó, không thêm một fake khác. Test đổi theo chữ ký thật đã đỏ trước khi đồng bộ rồi xanh. Chỉ lấy phần API cần cho 008; chưa merge bản cài 001 hay các thay đổi khác của develop vào nhánh đang làm. Tính lại tỷ lệ thuộc T037. SPI đọc của C/D đã nối nguồn phân công thật; truy vết dùng các đoạn vị trí lịch sử, không lấy phòng hiện tại thay cho cả quá khứ.
- T009/T019 bổ sung hai provider lưu giữ và hai provider phạm vi cùng migration; hồ sơ nhân viên/ca giữ lại khi còn phân công tham chiếu. Chỉ loại bỏ trạng thái cuối, giữ tới khi ca cha đóng; mốc lưu giữ dùng kết thúc thực tế khi có. Khóa tầng là dữ liệu kỹ thuật, không đưa vào danh sách tham chiếu nghiệp vụ của 003. Test provider đã đỏ rồi xanh.
- Nhắc FR-029 có hai cờ riêng: cờ công bố `publication_assignments_notified_at` thêm ở T019, cờ đầu ca có sẵn từ T012. Khi chưa có nguồn B/C, giữ cờ chưa xử lý để tác vụ theo phút thử lại; không báo danh sách rỗng như dữ liệu thật, không làm hỏng chuyển trạng thái ca vì riêng nguồn nhắc chưa triển khai. Thứ tự tác vụ: phân công (10), vòng đời ca (20), nhắc (30), giữ thứ tự khóa trước khi giữ khóa ca. Giao Trưởng tầng đến hạn chuyển Đã kết thúc và nhắc nếu có khoảng trống; đầu mỗi ca nhắc lại khi chưa có người được giao.
- M13ArchTest chỉ cho M13 nghe sự kiện cài HasNotificationRequests, nên bước nhắc lúc công bố được gọi trực tiếp trong transaction trước khi phát SchedulePublished; không sửa test kiến trúc của A. Đầu ca nghe ShiftStarted (sự kiện nghiệp vụ không cài HasNotificationRequests). Các thay đổi giờ tự nhiên chỉ chuyển trạng thái/nhắc, không tự lập ca, phân công hay bàn giao.
- T022 HTTP có 9 thao tác gốc và 1 xem trước đã duyệt. Giữ nguyên hợp đồng NewAssignment/Assignment và header của lệnh; kết quả xem trước bỏ id bằng NON_NULL, không có confirmWarnings mới trong yêu cầu tạo. Tạo vẫn kiểm lại. Schema sao chép dùng hai tên mô tả khác nhau để springdoc không gộp request/response cùng tên Copy; không đổi dữ liệu HTTP hoặc nới bộ so hợp đồng. Client sinh lại bằng npm run gen:api; gỡ đúng 9 dòng PENDING, endpoint xem trước được kiểm ngay.
- Các test bổ sung đã đỏ rồi xanh: điểm bắt đầu thật với DATETIME(6), xem trước trùng ở ca khác, không ghi đè khoảng lịch sử của phân công đã kết thúc, mốc cắt phạm vi sớm và đầu biên CFG-M15-07. Truy vấn nóng lọc đối tượng và khoảng giờ ngay tại SQL bằng chỉ mục T019; không tải phân công toàn viện rồi lọc theo từng người. Phép đọc điểm lấy dư một microgiây ở bước đọc khoảng, rồi lọc chính xác thời điểm, để không mất biên vì độ chính xác SQL.
- `DutyDirectory.shiftHasNoFloorManager` kiểm mốc hiện tại qua BusinessTime, không giữ kết quả ở đầu ca: giao Trưởng tầng kết thúc giữa ca thì người phụ trách ca được nhận diện đúng là người thay trong khoảng trống hiện tại. Test T021 cho trường hợp này đã đỏ rồi xanh.
- Frontend tiếp tục ở `features/shifts` theo README, gồm AssignmentPanel và FloorManagerPanel mở từ ShiftsPage. Xem trước trước lưu, xác nhận danh sách công việc thiếu điều kiện, đổi dữ liệu phải xem trước lại; lý do bắt buộc khi hủy/kết thúc/rút ngắn giao; nút phân công theo allowedCommands của ca. Không thêm thư viện, không sửa common/components. Phát hiện và sửa khung bị cắt trên màn nhỏ bằng biến thể Sheet ở nơi sử dụng và min-width của fieldset; ô/nút mới cao tối thiểu 44px. Ảnh kiểm trong `frontend/node_modules/.cache/008-phase4-*.png`, không đưa vào Git: 1440/900/390 px, sáng/tối, Tab/Escape và các vai trò AC-01/02/03/04/05/07/09, API giả trong harness riêng; chưa phải UI chạy nguồn B/C thật.
- Lấy id fixture bằng GeneratedKeyHolder trên chính connection INSERT; lượt mở rộng test lộ LAST_INSERT_ID đọc connection khác khi có tác vụ thông báo chạy song song, gây FK lỗi. Sửa fixture mới và `ShiftLifecycleIT` của E; không sửa test của người khác để khớp mã. Các fixture lưu giữ `AssignmentGovernanceIT`, `M09GovernanceIT`, `ShiftGovernanceIT` lấy id hồ sơ từ TestUsers để không đụng dải hồ sơ được dựng tường minh ở test HTTP; `AssignmentIT` tạo tài khoản Quản lý viện trước khi dịch đồng hồ tới tương lai, tránh để vai trò hiệu lực tương lai gây lỗi test khởi tạo 002 sau khi reset; `DepartmentDutyIT` dọn đúng bộ phận mình tạo và hai bảng con bằng connection lưu giữ, không làm tăng số danh mục mặc định ở test 008 cũ.
- **Kết quả cuối phase**: Spotless qua; `./mvnw clean verify` chạy nền trên JDK 25/MySQL 8.0 qua 204 test đơn vị/kiến trúc + 535 IT = 739, 0 lỗi/thất bại/bỏ qua, gồm đối chiếu hợp đồng và tất cả ArchTests. Web `npm test -- --maxWorkers=1` qua 111 test/19 file; lint/build qua. Sau kết quả xanh mới đánh T017–T022 `[x]`; T022b mobile giữ chưa làm theo CLAUDE.md. Cập nhật glossary 14.9, tiến độ CLAUDE và mục 8 team-plan; phần API C/D và thay đổi đã duyệt của A chờ review. Bộ test dùng fake nguồn chung; chưa kiểm liên thông với bản cài 001 mới trên develop, không báo ứng dụng thật đã đủ nguồn B/C. Bước tiếp là Phase 5 T023–T029, mobile T030 chưa mở; cần đồng bộ develop có kiểm soát trước khi nối nguồn 001 thật. Người dùng tự commit/push; AI chưa commit/push/merge/đổi nhánh hoặc mở PR.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Năm giao diện của 002 khai báo dạng SPI ở `m15.taikhoan.api`, M09 cài | 002 làm plan trước, cần dữ liệu nhân sự mỗi yêu cầu (phạm vi, nhiệm vụ, giấy phép); 008 lại gọi `QuyenApi`, `NhatKyApi` của M15 | Gọi thẳng `m09` từ `m15`: vòng phụ thuộc, Modulith từ chối |
| Port nội bộ `m09.*.port` cho 015 | 015 cùng M09, cần lệnh trên ca và phân công với cùng kiểm tra của 008 | Đặt ở `m09.api`: mở lệnh nội bộ cho module khác; 015 sửa thẳng bảng: lặp kiểm tra BR-M09-01, 03 |
| `tang_ca_khoa` | Kiểm FR-024, FR-028 trên tập phân công, giao của một tầng; cần tuần tự theo tầng (SC-011) | Khóa dòng `ca`: không phủ giao Trưởng tầng (không gắn ca) và phân công chồng giữa hai ca |
| `ban_cong_bo_lich` | FR-055 đòi lưu toàn bộ lịch, phân công lúc công bố làm bản gốc có nhật ký vì thay đổi ở Nháp không ghi từng lần | Ghi một nhật ký mỗi ca, mỗi phân công lúc công bố: hàng trăm bản ghi mỗi lần |
| Ba job, `lan_chay_m09` | Chuyển trạng thái ca, phân công theo giờ (FR-022, FR-027) và nhắc theo mốc (FR-039, FR-043, SC-001 ≤ 1 phút); chu kỳ là thông số vận hành, không có ở Phụ lục 25 | Tính mọi trạng thái lúc đọc: không có thời điểm chuyển để nhắc, đóng ca và báo cáo đúng hạn |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung hoặc dựa vào các điểm dưới đây; plan của feature liên quan giữ đúng hoặc ghi rõ thay đổi.

1. **002** (đã ghi bổ sung vào [../002-account-permission/contracts/module-interfaces.md](../002-account-permission/contracts/module-interfaces.md) mục 2):
   - `HoSoNhanVienApi`, `GiayPhepApi`, `DanhMucPhamViHanhNgheApi`, `PhanCongApi`, `NhiemVuApi` khai báo dạng SPI ở `m15.taikhoan.api`, M09 cài; chữ ký giữ đúng. `NhiemVuApi` thêm `nguoiThayTruongTang(tang, t)` (Q-90), `boPhanCua(nv, t)` (019, DBR-36), `laPhuTrachBoPhan(nv)` (002 dùng trước khi khóa tài khoản người đang là Phụ trách bộ phận).
   - Hàm đọc mới `NghiepVuChuyenMonApi.loaiYeuCau(maNghiepVu)` để 008 biết loại giấy phép của "phát thuốc và xác nhận liều" (FR-030).
2. **003** (đã ghi bổ sung vào [../003-room-bed-allocation/contracts/module-interfaces.md](../003-room-bed-allocation/contracts/module-interfaces.md) mục 1.8): hàm đọc `TinhTrangGiuongApi.choSanSangQuaHan(tangId, t)` cho nhóm (f) của danh sách bản ghi nguồn bàn giao (FR-039 f). 008 cài `ThamChieuViTri` đúng chữ ký.
3. **005** (đã ghi bổ sung vào [../005-daily-care-tasks/contracts/module-interfaces.md](../005-daily-care-tasks/contracts/module-interfaces.md) mục 1.2): hàm đọc `LoaiCongViecApi.yeuCau(loaiCongViecId)` (vai trò, chứng chỉ yêu cầu; FR-030) và `KeHoachChamSocApi.loaiCongViecHienHanh(nctId)` (FR-031). 008 cài `NhanSuCaPort`, gọi `CongViecCaApi`, `CongViecBanGiaoApi` đúng chữ ký.
4. **006, 007**: 008 cài `DieuDuongCaPort`, `NhanSuCanhBaoPort` đúng chữ ký (*2026-10-22:* kèm ba hàm mới cho nút hỗ trợ khẩn cấp, 007 FR-047i); gọi `CanhBaoBanGiaoApi.khiXacNhanBanGiao` trong lệnh xác nhận; đọc `LieuBanGiaoApi`, `ChiSoApi.vuotNguongTrongKhoang`. Không gọi 006 khi xác nhận (006 đọc `banGiaoDaXacNhan`).
5. **009** nghe các sự kiện ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.6 với mức FR-058; đọc `CaTrucApi.dangTrongCa` để chọn kênh (BR-M13-01). **011** đọc `NhiemVuApi.nguoiNhanTaiTang` (UC-76). **013** cài `DoGuiBanGiaoPort`; nghe `CaDaKetThuc`. **014** cài `ChuyenDiNhiemVuPort`. **015** dùng port nội bộ M09 (research N-07), đặt các cột của `ca_nhan_vien` dành cho 015, cài `KiemTraCongBoMoRong`. **016** đọc `NhanSuThongKeApi`. **019** đọc `NhiemVuApi.phuTrachBoPhan`, `boPhanCua`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Nhật ký mỗi lần xem bàn giao.** Spec 008 FR-047 ("Mọi lần xem, hoàn tất, xác nhận, gửi ý kiến MUST ghi nhật ký") và FR-056 ("Nhật ký xem bàn giao MUST chỉ Quản lý viện xem được") trái 000 FR-043a ("Việc xem dữ liệu… MUST NOT được ghi nhật ký, trừ lần người thân xem hồ sơ sức khỏe") và Q-03 ("Không bổ sung nhật ký lần nhân viên xem dữ liệu sức khỏe ở giai đoạn này"). Plan theo Q-03 và 000: không ghi lần xem; hoàn tất, xác nhận, gửi ý kiến, bổ sung có nhật ký như mọi lệnh. Đề nghị bỏ chữ "xem" ở FR-047 và câu về nhật ký xem ở FR-056, hoặc mở Q mới nếu viện muốn ghi lần xem bàn giao. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

2. **Câu chữ còn sót trước Q-307 trong spec 008.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - FR-055 còn "Thay đổi do hệ thống (tự lập, tự cập nhật bản nháp bàn giao, …)", trái FR-039, FR-040 sau Q-307.
   - Bảng FR-027 ghi người thực hiện "Hệ thống khi sao chép"; sao chép là lệnh của Trưởng tầng (FR-032). Plan ghi người thực hiện là Trưởng tầng, nguồn phân công `SAO_CHEP`.
   - Đề nghị sửa hai chỗ trên.

3. **Danh sách bản ghi nguồn nhóm (f) "giường Chờ sẵn sàng quá hạn"**: 003 module-interfaces mục 1.9 ghi "Giường Chờ sẵn sàng quá hạn không có ở đây: người bàn giao tự ghi (FR-044, Q-307)", trong khi spec 008 FR-039 (f) yêu cầu người lập xem được danh sách này. Plan thêm hàm đọc `TinhTrangGiuongApi.choSanSangQuaHan` ở 003 (mục Đồng bộ 2): chỉ hiển thị, không lập mục. Không đổi nghiệp vụ; ghi lại để thống nhất cách hiểu "người bàn giao tự ghi". *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

4. **Mức thông báo "bộ phận chưa có Phụ trách bộ phận"** (FR-001a) không có trong FR-058. Plan xếp Trung bình theo nguyên tắc 3 bậc (research N-14) vì lệnh của bộ phận bị chặn. Đề nghị ghi vào FR-058 và bảng 009 FR-043b. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

5. **Vai trò được tính phủ và tỷ lệ của Điều dưỡng làm việc chăm sóc** (FR-028a, FR-034): spec rõ; không cần đổi. Ghi lại: plan tính Điều dưỡng một lần trong tỷ lệ phục vụ và để 015 loại khỏi dòng phủ Nhân viên chăm sóc (Q-221). *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

6. **(Đồng bộ câu chữ spec 008, không đổi nghiệp vụ)** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Assumptions còn câu dẫn `docs/phan-tich-yeu-cau.md` mục 4.2 (đã bỏ); có thể giữ làm lịch sử.
   - FR-036, FR-038 còn chữ "đề xuất" với CFG-M09-08, CFG-M09-09 dù đã có ở Phụ lục 25.

7. **(2026-10-22) Trùng số điện thoại khi tạo tay.** FR-001b coi trùng số điện thoại với hồ sơ đã có là lỗi của dòng import, nhưng lệnh tạo hồ sơ bằng tay (FR-001) không kiểm trùng. Plan chỉ kiểm khi import (theo đúng spec) và dùng cột `dien_thoai_chuan` có chỉ mục; tạo tay vẫn cho trùng. Nếu viện muốn số điện thoại duy nhất cho mọi hồ sơ thì cần Q mới. *(Đã xử lý 2026-10-22: chủ dự án chọn giữ đúng spec: chỉ kiểm trùng khi import; muốn kiểm khi tạo tay thì cần Q mới.)*
8. **(2026-10-22) Bố cục file mẫu.** Spec để plan chốt (Điểm cần báo lại 2026-10-22). Plan chốt: một trang tính "NhanVien", dòng 1 là tiêu đề cố định theo thứ tự Họ tên, Chức danh, Số điện thoại, Ngày bắt đầu làm việc, Chuyên môn, Email; dữ liệu từ dòng 2; ngày nhận ô kiểu ngày của Excel hoặc chữ `dd/MM/yyyy`; số điện thoại nhận ô chữ (giữ số 0 đầu). Thiếu hoặc sai thứ tự cột tiêu đề là "không đúng mẫu". Trang tính thứ hai "HuongDan" chỉ để đọc. Đề nghị chủ dự án duyệt bố cục. *(Đã xử lý 2026-10-22: chủ dự án chọn duyệt bố cục trên.)*
