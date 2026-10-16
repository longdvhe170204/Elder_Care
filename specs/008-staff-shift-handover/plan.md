# Implementation Plan: Nhân sự, ca trực và bàn giao

**Branch**: `008-staff-shift-handover` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

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

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 007: Spring Boot 4.1.x, Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock, Spring Security 7. Web: React 19, TanStack Query. App nhân viên: Flutter, drift (lưu bản xem bàn giao). Không thêm thư viện mới.

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
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
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
4. **006, 007**: 008 cài `DieuDuongCaPort`, `NhanSuCanhBaoPort` đúng chữ ký; gọi `CanhBaoBanGiaoApi.khiXacNhanBanGiao` trong lệnh xác nhận; đọc `LieuBanGiaoApi`, `ChiSoApi.vuotNguongTrongKhoang`. Không gọi 006 khi xác nhận (006 đọc `banGiaoDaXacNhan`).
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
