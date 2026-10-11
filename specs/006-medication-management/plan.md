# Implementation Plan: Quản lý thuốc

**Branch**: `006-medication-management` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-medication-management/spec.md`

## Summary

Feature 006 là module M07 (mục 11, UC-39 → UC-44). Nó cung cấp:
1. Danh mục thuốc có hoạt chất liên kết danh mục dị nguyên, cửa sổ riêng, dấu kiểm soát đặc biệt (mục A).
2. Đơn thuốc định kỳ và PRN: nhập đơn nội bộ hoặc bên ngoài, kiểm trùng hoạt chất và dị ứng, vòng đời, đổi liều bằng đơn thay thế, gia hạn, hết hạn (mục B; UC-39, UC-40).
3. Lịch thuốc tính từ đơn, không lập sẵn bản ghi liều; vòng đời liều theo giờ (mục C).
4. Thuốc khi cần với khoảng cách tối thiểu và số lần trong 24 giờ trượt (mục D; UC-42).
5. Xác nhận liều, liều chung của tầng, thuốc kiểm soát đặc biệt, ngoại tuyến, cảnh báo, đính chính, sự kiện tính phí (mục E; UC-41).
6. Phiếu đối chiếu do Bác sĩ, Điều dưỡng lập; quy tắc hai người; tạm dừng đơn khi tiếp nhận, trở về từ bệnh viện (mục F; UC-43).
7. Thuốc gia đình gửi: lô, biến động số lượng, mốc nhắc sắp hết, hoàn trả, hủy (mục G; UC-44).
8. Vắng mặt, thuốc mang theo, giao và nhận lại; tác động của trạng thái cuối; quyền xem; dữ liệu cung cấp cho 004, 005, 007, 008, 010, 012, 014, 016 (mục H).

Hướng kỹ thuật:
- Lịch thuốc là hàm thuần tính từ đơn và khoảng vắng. Dòng `lieu_thuoc` chỉ được thêm khi liều tới đầu cửa sổ (job theo phút), hoặc kèm lệnh của Điều dưỡng (ghi nhận, gắn lần giao). Không có bản ghi liều cho ngày tới (Q-306, SC-001; research D-06). Liều được định danh bằng `lieuKey = {donThuocId}-{thời điểm dự kiến}`.
- Mỗi lần đóng liều (kể cả Bỏ lỡ do Hệ thống) có đúng một dòng `ket_qua_lieu` nhóm 3; đính chính trỏ dòng này (DBR-13, BR-M07-12; D-08, D-12).
- Người phụ trách liều tính lúc đọc và lúc ghi từ phân công của 008, không lưu trên liều; liều chung của tầng là kết quả của phép tính đó (D-09).
- Ràng buộc đưa xuống MySQL: `UNIQUE (don_thuoc_id, thoi_diem_du_kien)` (DBR-13), trigger khóa trường đơn (DBR-14), trigger trạng thái cuối của đơn, liều, phiếu, lô, cột sinh `UNIQUE` cho một phiếu mở và một phiếu tiếp nhận, trigger giữ số lượng thuốc gửi chỉ đổi qua biến động (data-model mục 11).
- Tác động lên đơn, liều, phiếu khi tiếp nhận, vắng, trở về, trạng thái cuối là bean `ThamGiaChuyenTrangThai` trong transaction của 001 (D-16).
- 006 làm plan trước 007, 008, 010, 012, 014, 016; dữ liệu ca và phân công (008), chuyến đi (014), đề nghị mua hộ (010) lấy qua SPI khai báo ở `m07.api` (D-02).
- Yêu cầu tạo/đóng cảnh báo, thông báo, sự kiện tính phí đi qua outbox ghi cùng transaction (D-17).
- Ngoại tuyến chỉ cho xác nhận liều và lần dùng PRN của thuốc không kiểm soát đặc biệt, qua hai `NgoaiTuyenHandler` của khung 002 (D-11).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 006 không lập sẵn liều, không tự tạo phiếu đối chiếu hay dòng phiếu, không tự tạo chi phí, không chọn sẵn hay đề xuất lô;
- hệ thống chỉ: tính lịch để hiển thị và xét giờ; chuyển trạng thái theo giờ (đơn, liều, lô hết hạn dùng); tạo yêu cầu cảnh báo, nhắc, thông báo; kiểm tra và chặn; tác động kèm lệnh của actor (tạm dừng đơn chờ đối chiếu, đóng đơn và liều khi trạng thái cuối, trừ và cộng số lượng).

## Technical Context

**Language/Version**: Java 21 LTS; TypeScript 5.x; Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 005: Spring Boot 3.5.x, Spring Modulith 1.4.x, Flyway 11.x, springdoc-openapi 2.8.x, ShedLock, Spring Security 6.5. Web: React 19, TanStack Query. App nhân viên: Flutter, drift (hàng đợi ngoại tuyến, 000 R-13). App người thân: Flutter. Không thêm thư viện mới.

**Storage**: MySQL 8.4 LTS. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test đơn vị dạng bảng cho tám hàm thuần: chuỗi thời điểm của đơn, tính lịch, bảng chuyển liều, kiểm tra đơn mới, giới hạn PRN, đếm từ chối liên tiếp, số ngày còn lại, người phụ trách liều.
- Testcontainers cho trigger, cột sinh.
- Test đồng thời: hai lần xác nhận một liều; xác nhận và job Bỏ lỡ; hai lần dùng PRN; hai lần trừ một lô.
- Đồng hồ giả lập cho mọi CFG-M07, gồm chạy bù.
- Bản giả cho mọi SPI 006 khai báo (research D-20).

**Target Platform**: Như 000.
- Web: mọi màn của 006, gồm danh mục thuốc, đơn thuốc, lịch thuốc, phiếu đối chiếu, thuốc gia đình gửi, giao và nhận lại thuốc, xem lại thuốc kiểm soát đặc biệt; cổng người thân trên trình duyệt (thuốc gia đình gửi; đơn và phiếu khi có bản đồng ý).
- App nhân viên: vòng phát thuốc, xác nhận liều và PRN (có ngoại tuyến, trừ thuốc kiểm soát đặc biệt), chọn lô, ghi theo báo lại, giao và nhận lại thuốc mang theo, tiếp nhận thuốc gia đình gửi, xem lại.
- App người thân: xem thuốc gia đình gửi, đơn và phiếu Đã xác nhận khi có bản đồng ý; nhận thông báo sắp hết, hết hạn dùng (qua 009, 012).

**Project Type**: Web service (REST `/api/v1`), một web client và hai mobile client.

**Performance Goals**:
- Vòng phát thuốc 15 người ≤ 3 giây (SC-009, NFR-02): một truy vấn đơn theo `(nct_id, trang_thai)`, tính lịch trong bộ nhớ cho khoảng của ca, ghép dòng đã vật hóa theo `(nct_id, thoi_diem_du_kien)`; phân công lấy một lần theo lô.
- Xác nhận liều ≤ 2 giây (NFR-02), ≤ 10 giây thao tác (SC-009): khóa một dòng, thêm một kết quả, vài sự kiện outbox.
- Job theo phút ≤ 1 phút (SC-004): khoảng 1.500 đơn ở quy mô 300 người, khoảng 1.800 liều được vật hóa mỗi ngày, cao điểm vài trăm dòng lúc 07:30.

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI).
- Kết quả liều, lần dùng PRN, biến động, lần giao, lần nhận lại, căn cứ, vấn đề không bị `UPDATE`, `DELETE`; phiếu Đã xác nhận không sửa (constitution XII).
- Nhật ký trong transaction (000 FR-047). CFG đọc lúc chạy (constitution XIII).
- Không tự sinh bản ghi nghiệp vụ (000 FR-010a).

**Scale/Scope**:
- 300 người cao tuổi, khoảng 1.500 đơn hiệu lực, 1.800 liều và vài chục lần dùng PRN mỗi ngày (khoảng 0,7 triệu liều mỗi năm).
- 5 thực thể có vòng đời (đơn, liều, phiếu, lô thuốc gửi, lần giao) và 2 vòng đời phụ (xem lại, đề nghị đính chính phiếu); 26 bảng.
- 2 job (theo phút, hằng ngày).
- 42 đường dẫn, 48 thao tác REST ([contracts/api-006.yaml](contracts/api-006.yaml)).
- 3 SPI 006 khai báo; 10 SPI 006 cài cho module thấp hơn; 7 giao diện 006 cung cấp.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR. Data-model mục 14 là bảng truy vết DBR-13, 14, 15, 17, 23, 25, 03 và BR-M07-01 → 23 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Danh mục thuốc theo mẫu nhóm 1. Đơn, liều, phiếu, lô chỉ có lệnh; đơn chỉ sửa ghi chú hướng dẫn. Kết quả, PRN, biến động, lần giao, nhận lại chỉ thêm và đính chính |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 4, 5, 7, 8, 9 khớp bảng FR-011, FR-021, FR-035, FR-041, FR-048a. Mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M07-01, 02, 04 → 07, CFG-M05-04, CFG-M15-03, CFG-M15-08 đọc từ `ThamSoApi`. Chu kỳ job một phút và 00:00 là thông số kỹ thuật (Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Khóa quyền theo bốn dòng Phụ lục 27 (research D-18); 19.3 cho vai trò không xem thuốc. Spec thu hẹp: chỉ Điều dưỡng đính chính liều (FR-030). Không thêm vai trò |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ. Mức các thông báo spec chưa nêu: plan áp nguyên tắc 3 bậc và báo lại (điểm 5). Cách tính số ngày còn lại khi lô dùng chung: plan theo BR-M07-18 và báo lại (điểm 1) |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-014 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 001 được bổ sung một sự kiện (mục "Đồng bộ"), không đổi hành vi đã thiết kế |
| X. Kiến trúc | Đạt | Một khối; gói `m07`; module khác chỉ dùng `m07.api`. Không có vòng phụ thuộc (D-02). REST `/api/v1`; MySQL; ba client. NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Kiểm tra đơn mới, giới hạn PRN, quyền xác nhận, quy tắc hai người, số lượng ở backend. Mỗi lệnh có `POST …/{id}/<lenh>` riêng; lệnh trạng thái người cao tuổi dùng endpoint của 001 |
| XII. Dữ liệu | Đạt | DBR-13, DBR-14 bằng `UNIQUE` và trigger; nhóm 3 theo mẫu 000; nhật ký trong transaction; `Clock`; `thoi_diem_thiet_bi` cho ngoại tuyến (DBR-25) |
| XIII. Tham số | Đạt | Như V. Cửa sổ chốt lúc liều tới đầu cửa sổ; đổi CFG-M07-01 chỉ áp liều sau đó |
| XIV. Ngoại tuyến, thông báo | Đạt | Xác nhận liều, PRN ngoại tuyến theo Q-01 qua khung 002; thuốc kiểm soát đặc biệt bắt buộc trực tuyến (8.6). Mọi thông báo là sự kiện tới 009, có mức (D-17) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 006 có test (data-model mục 14); BR-M07-23 là phạm vi loại trừ nên không có chức năng để kiểm. BR-M15-20 và thao tác ngoại tuyến có thêm test Flutter |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có sáu bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Bảng `thuoc_nct` làm đích khóa theo người và giữ hạn đối chiếu.
2. Vật hóa dòng liều tại đầu cửa sổ thay vì lập sẵn hoặc không lưu.
3. `ket_qua_lieu` cho mọi lần đóng, kể cả do Hệ thống.
4. Ba SPI khai báo ở `m07.api` cho 008, 014, 010.
5. Bảng `khoang_vang_thuoc` giữ bản sao khoảng vắng, khoảng đi.
6. Hai job (theo phút, hằng ngày) và bảng `lan_chay_m07`.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 006 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 người, 60 người dùng đồng thời giờ giao ca; chỉ mục ở data-model mục 4, 5 |
| NFR-02 Hiệu năng | Có | Vòng phát thuốc tính trong bộ nhớ từ một truy vấn đơn; xác nhận khóa một dòng |
| NFR-03 Thông báo khẩn cấp | Không | 006 không có thông báo Khẩn cấp |
| NFR-04 Tác vụ định kỳ | Có | Job theo phút; khóa dòng, cột `da_*_luc`, `lan_chay_m07` chống gửi trùng; khóa sự kiện cho bên nghe |
| NFR-05 Khả dụng | Có | Chạy bù khi khởi động (D-19); ngoại tuyến cho xác nhận tại giường |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL |
| NFR-07 Lưu trữ | Có | `LuuTruProvider` theo hồ sơ, CFG-M15-03 (data-model mục 13) |
| NFR-08 Bảo mật | Có | Thuốc, liều, phản ứng mang `SUC_KHOE`; Chăm sóc, Bếp, Hành chính không thấy; lần người thân xem ghi nhật ký |
| NFR-09 Thời gian | Có | `Clock`; `lieuKey` và cửa sổ theo Asia/Ho_Chi_Minh; 24 giờ trượt tính trên thời điểm tuyệt đối |
| NFR-10 Thiết bị | Có | App nhân viên một tay, nút lớn cho vòng phát thuốc; web đủ chức năng; cổng người thân hai kênh |
| NFR-11 Ngôn ngữ | Có | Giờ HH:mm, ngày dd/MM/yyyy ở client; lỗi chặn nêu mốc sớm nhất, đơn trùng, mục dị ứng |
| NFR-12 Cấu hình | Có | Mọi CFG-M07 đổi qua tham số; danh mục thuốc qua màn cấu hình |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả bật tắt được |

## Project Structure

### Documentation (this feature)

```text
specs/006-medication-management/
├── plan.md                      # File này
├── research.md                  # Phase 0: D-01 → D-20
├── data-model.md                # Phase 1: bảng, bảng chuyển, bảo đảm ở cơ sở dữ liệu, truy vết
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-006.yaml             # OpenAPI 3.1: danh mục, đơn, lịch, liều, PRN, đối chiếu, thuốc gửi, mang theo
│   └── module-interfaces.md     # Giao diện Java: 006 cung cấp / 006 cần; sự kiện; tác động của bean trạng thái
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/vn/eldercare/m07/
├── api/           # DonThuocApi, LichThuocApi, LieuBanGiaoApi, MangThuocChuyenDiApi, ThuocGiaDinhGuiApi,
│                  # ThuocCongNguoiThanApi, ThuocThongKeApi, SPI (DieuDuongCaPort, ChuyenDiThuocPort, DeNghiMuaHoPort), sự kiện
├── danhmuc/       {internal, web}   # thuốc, hoạt chất
├── donthuoc/      {internal, web}   # đơn, KiemTraDonMoi, đổi liều, gia hạn, căn cứ cơ sở kê, listener dị ứng
├── lichthuoc/     {internal, web}   # ChuoiThoiDiemDon, LichThuoc, vật hóa, BangChuyenLieu
├── lieu/          {internal, web}   # xác nhận, XacDinhNguoiPhuTrachLieu, DemTuChoiLienTiep, xem lại, NgoaiTuyenHandler
├── prn/           {internal, web}   # KiemTraGioiHanPrn, NgoaiTuyenHandler
├── doichieu/      {internal, web}   # phiếu, dòng, xác nhận hai người, đề nghị đính chính phiếu
├── thuocgui/      {internal, web}   # lô, biến động, TinhSoNgayConLai, mốc nhắc, MucViecSauQuaDoiPort
├── mangtheo/      {internal, web}   # khoảng vắng, lần giao, nhận lại, không mang thuốc
├── trangthai/     {internal}        # bean ThamGiaChuyenTrangThai; cài LieuThuocChecklistPort, ThoiKhoaBieuNguonPort
└── lich/                            # ViecTheoPhutM07, ViecHangNgayM07

web/src/tinh-nang/
├── danh-muc-thuoc/
├── don-thuoc/               # nhập đơn, vấn đề cần lý do, đổi liều, chuỗi thay thế
├── lich-thuoc/              # lịch người cao tuổi, vòng phát thuốc web
├── doi-chieu-thuoc/         # phiếu, dòng, gửi, xác nhận, đính chính
├── thuoc-gia-dinh-gui/      # tiếp nhận, gắn đơn, biến động, hoàn trả, hủy
├── thuoc-mang-theo/         # giao, nhận lại, không mang thuốc
└── cong-nguoi-than/thuoc/   # thuốc gửi; đơn, phiếu khi có bản đồng ý

app-nhan-vien/lib/tinh-nang/
├── vong-phat-thuoc/         # tải, lưu ngoại tuyến, dấu "chưa cập nhật", chặn kiểm soát đặc biệt
├── xac-nhan-lieu/           # Đã dùng, Từ chối, Không thực hiện, chọn lô, ghi theo báo lại, PRN
├── dong-bo/                 # kết quả đồng bộ từng bản ghi (BR-M15-20)
├── thuoc-mang-theo/
├── thuoc-gia-dinh-gui/
└── xem-lai-lieu/

app-nguoi-than/lib/tinh-nang/thuoc/   # thuốc gửi; đơn, phiếu khi có bản đồng ý
```

**Structure Decision**: Theo R-02 của 000. Feature 006 nằm ở `backend/…/m07` với tám gói con nghiệp vụ, gói `trangthai` gom SPI cài cho module thấp hơn, gói `lich` và gói `api`. Web có đủ chức năng. App nhân viên có vòng phát thuốc, xác nhận ngoại tuyến và các thao tác tại tầng. App người thân chỉ xem.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Bảng `thuoc_nct` (một dòng mỗi người) | Đích khóa tuần tự cho FR-007, FR-025, FR-037 trên tập đơn của một người; giữ hạn đối chiếu tính từ lệnh tiếp nhận/trở về, kể cả khi chưa có phiếu | Khóa dòng `nguoi_cao_tuoi` của M01 là truy cập dữ liệu module khác; đặt hạn trên phiếu thì không có chỗ khi chưa ai lập phiếu |
| Vật hóa dòng liều tại đầu cửa sổ | Q-306 cấm lập sẵn; FR-018, FR-020, FR-022 đòi lưu trạng thái do bộ lập lịch chuyển, cửa sổ đã chốt, người đóng "Hệ thống" | Lập sẵn N ngày: trái Q-306. Không lưu dòng nào trước khi xác nhận: mất Trễ, Bỏ lỡ, Tạm dừng kết thúc và cửa sổ đã chốt |
| `ket_qua_lieu` cho mọi lần đóng, kể cả Bỏ lỡ | Đính chính của 000 cần một bản gốc nhóm 3 (DBR-23); Bỏ lỡ → Đã dùng (US8 #4, FR-029) là đính chính trên lần đóng của Hệ thống | Đính chính trực tiếp dòng `lieu_thuoc`: dòng nhóm 2, vi phạm trigger trạng thái cuối |
| Ba SPI khai báo ở `m07.api` (008, 014, 010) | 008, 014, 010 làm plan sau và cần gọi hoặc đọc 006; 006 cần lại dữ liệu ca, chuyến, mua hộ | 006 gọi thẳng API của họ: vòng phụ thuộc, Modulith từ chối |
| Bảng `khoang_vang_thuoc` | Lịch thuốc cần khoảng vắng và khoảng đi (có/không mang thuốc) để tính Tạm dừng, Mang theo cho liều tương lai; ghi kèm lệnh trạng thái | Đọc lượt vắng của 004 và chuyến của 014 mỗi lần tính lịch: nhiều lời gọi trên đường đọc nóng; `LuotVangApi` chỉ trả lượt đang mở |
| Job theo phút, job hằng ngày, bảng `lan_chay_m07` | SC-004 đòi chuyển và nhắc ≤ 1 phút; NFR-04 đòi chạy bù không trùng; mốc theo ca chạy đúng một lần. Chu kỳ và giờ chạy là thông số vận hành, không có trong Phụ lục 25 | Mỗi việc một job: khó giữ thứ tự (đơn hiệu lực trước, rồi vật hóa liều) |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung hoặc dựa vào các điểm dưới đây; plan của feature liên quan giữ đúng hoặc ghi rõ thay đổi.

1. **001** (đã bổ sung vào [../001-elder-profile-assessment/contracts/module-interfaces.md](../001-elder-profile-assessment/contracts/module-interfaces.md) mục 1.10): sự kiện `HoSoSucKhoeBanDauDaDoi(nctId)` khi "thuốc đang sử dụng khi tiếp nhận" của hồ sơ ban đầu đổi (sửa khi Đang tiếp nhận hoặc đính chính `M01.HO_SO_SUC_KHOE_BAN_DAU`). Cần cho FR-034a, BR-M07-22.
2. **005**: 006 cài `LieuThuocChecklistPort`, `ThoiKhoaBieuNguonPort` đúng chữ ký; `LieuHienThiView` được định nghĩa ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.3.
3. **004**: dùng `MucViecSauQuaDoiPort` mã `THUOC_GUI`, mã điều kiện `KT_C_THUOC_GUI`, `chiTiet.mangThuoc` của `CHO_TAM_VANG` như đã khai báo; không đổi.
4. **008** cài `DieuDuongCaPort`; đọc `LieuBanGiaoApi`. **014** cài `ChuyenDiThuocPort`, gọi `MangThuocChuyenDiApi`, đặt `chuyenId` trong `chiTiet` của `DIEM_DANH_ROI_VIEN` (đề xuất). **010** cài `DeNghiMuaHoPort`, nghe `SuKienTinhPhiThuoc*`. **012** cài `QuanHeNguoiThanApi`, `NguoiDaiDienPort`, đọc `ThuocCongNguoiThanApi`. **007** đọc `DonThuocApi.thuocDangDung`, nghe `YeuCauCanhBao`, `YeuCauDongCanhBao`. **016** đọc `ThuocThongKeApi`.
5. **009**: nhóm người nhận `BAC_SI_TOAN_VIEN` (mọi tài khoản Hoạt động vai trò Bác sĩ; spec FR-017 CHK014) cần có trong cách phân giải người nhận của 009.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Số ngày dùng còn lại khi một lô gắn nhiều đơn.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - FR-046 ghi "số ngày còn lại của mỗi đơn = tổng số lượng còn của các lô gắn đơn ÷ lượng dùng mỗi ngày của đơn"; FR-043 và BR-M07-18 ghi "tính trên tổng lượng dùng mỗi ngày của các đơn dùng chung lô".
   - Với một lô gắn cả đơn định kỳ và đơn PRN, công thức "của mỗi đơn" đếm cùng số thuốc hai lần và có thể gửi hai thông báo cho một thuốc.
   - Plan tính trên nhóm dùng chung (lô và đơn liên thông qua liên kết), một thông báo mỗi nhóm (research D-14). Khi không có lô dùng chung, kết quả trùng FR-046. Đề nghị sửa câu FR-046 theo BR-M07-18.

2. **"Đánh dấu dòng của thuốc đã bỏ" (BR-M07-22, FR-034a) chưa nói ai đánh dấu.** Q-307 cấm hệ thống tự sửa phiếu. Plan hiển thị dấu "thuốc đã bỏ khỏi hồ sơ" tính lúc đọc, không ghi vào dòng; người thực hiện tự sửa dòng. Đề nghị ghi rõ ở BR-M07-22. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Bỏ lỡ → Đã dùng khi đồng bộ muộn.** FR-029 cho liều "đã Trễ/Bỏ lỡ do mất kết nối nhưng thời điểm dùng trong cửa sổ thì chuyển Đã dùng"; bảng FR-021 và 11.2 coi Bỏ lỡ là trạng thái cuối, không có chuyển Bỏ lỡ → Đã dùng. Plan làm phần Bỏ lỡ bằng bản đính chính tạo kèm bản ghi đồng bộ (người tạo là người ghi; trạng thái hiện hành Đã dùng), giữ trạng thái cuối (research D-11). Đề nghị ghi cách này vào FR-029 hoặc thêm dòng vào bảng FR-021. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

4. **SC-012 "100% bản ghi ghi sau mất kết nối có kết quả xem lại trước khi bàn giao ca kế tiếp được xác nhận"** không có quy tắc chặn tương ứng ở spec 006 hay 008. Plan đưa bản ghi chờ xem lại vào `LieuBanGiaoApi` và thông báo người xem lại; không chặn xác nhận bàn giao. Đề nghị chốt (Q mới) bàn giao có bị chặn khi còn bản ghi chờ xem lại không, hoặc sửa SC-012 thành chỉ tiêu theo dõi. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

5. **Mức của các thông báo spec 006 chưa nêu (009 FR-043a, Q-108).** Plan áp nguyên tắc 3 bậc (research D-17): thuốc gia đình gửi hết hạn dùng, hết thuốc gia đình gửi khi đến liều, cần nhận lại thuốc mang theo ở mức Trung bình; phiếu gửi xác nhận / trả lại, thuốc gửi đã dùng hết, chuyển Chỉ giữ hộ do đơn ngừng, thuốc đang dùng khi tiếp nhận đã đổi ở mức Nhẹ. Đề nghị ghi vào spec 006 và bảng 009 FR-043b khi đồng bộ. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

6. **Edge Case "Người cao tuổi chuyển lại bệnh viện khi phiếu đối chiếu chưa xác nhận … khi trở về, phiếu mới được tạo"** còn câu chữ trước Q-306 (hệ thống tạo phiếu). Plan theo FR-034: hệ thống báo, Bác sĩ hoặc Điều dưỡng lập phiếu mới. Đề nghị sửa câu Edge Case thành "người thực hiện lập phiếu mới". *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

7. **Liều đã xác nhận sau thời điểm qua đời ghi trong quá khứ** (Edge Case, "đánh dấu cần rà soát", feature 004): spec không nói ai rà soát và kết thúc rà soát thế nào. Plan hiển thị dấu tính lúc đọc, không đổi bản ghi, không thông báo. Đề nghị xác nhận, hoặc giao người rà soát bằng Q mới. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

8. **(Đồng bộ câu chữ spec 006, không đổi nghiệp vụ)** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - FR-026 có hai cụm "(Nguồn: …)" nối nhau; nên gộp.
   - Assumptions còn câu dẫn `docs/phan-tich-yeu-cau.md` mục 4.2 (đã bỏ); có thể giữ làm lịch sử.
   - Điểm báo lại 12 của spec còn nêu "đề xuất lô hạn dùng sớm nhất (FR-044)", đã bỏ theo Q-307.

9. **Q-63 và Thông tư 27/2024/TT-BYT** (spec điểm báo lại 2026-10-06, Q-304): thiết kế giữ phạm vi hiện tại (bắt buộc trực tuyến, xem lại khi mất kết nối), không có sổ nhập–xuất–tồn hay người chứng kiến cho thuốc gây nghiện, hướng thần. Nếu tư vấn pháp lý xác nhận viện thuộc diện áp dụng thì cần Q mới và thêm bảng sổ theo dõi ở `m07`; cấu trúc gói hiện tại không chặn việc bổ sung này. *(Còn mở 2026-10-10: chờ tư vấn pháp lý, mục 24.4.)*
