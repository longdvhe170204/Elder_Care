# Implementation Plan: Dịch vụ thông báo

**Branch**: `009-notification-service` | **Date**: 2026-10-10, cập nhật 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/009-notification-service/spec.md`

## Summary

Feature 009 là module M13 (mục 17, 17.1; BR-M13-01 → 05; UC-66; mục 23 dòng "Nhà cung cấp SMS", "Gọi điện"). Nó cung cấp:
1. Một cách gửi yêu cầu thông báo duy nhất cho mọi module, chống trùng theo (bản ghi nguồn, khóa sự kiện) (mục A).
2. Xác định người nhận theo phân công hiện tại, cách thay của từng nhóm, lọc người thân theo quyền và bản đồng ý (mục B).
3. Chọn kênh theo mức và ca, gửi song song, gửi lại, gộp tin nhắn Trung bình, chặn thông tin sức khỏe ở kênh ngoài ứng dụng, danh sách "cần liên hệ trực tiếp" (mục C).
4. Giờ yên tĩnh cho người thân (mục D).
5. Thông báo Khẩn cấp: xác nhận, "Xác nhận và tiếp nhận", lượt nhắc gọi, kết quả gọi, chuyển nhắc, chuỗi gọi (mục E).
6. Hộp thông báo hiển thị theo quyền hiện hành, lịch sử gửi và xem, dữ liệu cho module nguồn, danh sách lỗi gửi (mục F, G).

Hướng kỹ thuật:
- Khuôn `YeuCauThongBao` và giao diện `CoYeuCauThongBao` ở gói kỹ thuật `common.thongbao`; sự kiện miền của module nguồn cài giao diện này; 009 có một listener duy nhất (research B-02). Nhờ vậy module làm plan trước (000 → 008) không phụ thuộc M13.
- Đường xử lý: outbox của lệnh nguồn → listener tiếp nhận (lưu yêu cầu, xác định người nhận, tạo thông báo) → bộ phát gửi song song từng kênh, mỗi kết quả là dòng ghi thêm (B-03).
- Thông báo, sự kiện, lần gửi, lượt nhắc, kết quả gọi là nhóm 3 ghi thêm; trạng thái theo bảng FR-044, FR-030 là hàm thuần trên sự kiện, có bảng trạng thái kỹ thuật dẫn xuất để truy vấn (B-09).
- Kênh trong ứng dụng là hộp thông báo, đẩy tới app di động qua Firebase Cloud Messaging và tới web qua Server-Sent Events (Q-305); tin nhắn qua SPI `NhaCungCapTinNhan`, mặc định "chưa cấu hình" (Q-06) (B-05).
- Bất biến FR-005, FR-012, FR-032 đưa xuống `UNIQUE` (data-model mục 8).
- Job 20 giây cho lượt nhắc gọi, chuyển nhắc, khung gộp, giải phóng giờ yên tĩnh, gửi lại (B-10).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 009 không tạo yêu cầu gọi điện hay việc giao; lượt nhắc gọi là một thông báo Khẩn cấp, trạng thái tính được từ kết quả gọi và xác nhận;
- 009 không tự nâng, hạ mức hay quyết định sự kiện nào cần thông báo (FR-002); chỉ gửi, nhắc, giữ và giải phóng, chặn nội dung không hợp lệ.

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": Q-352 → Q-354)

- **Khuôn yêu cầu** (FR-001a): `YeuCauThongBao` thêm `doiTuongThongTin` (`VE_NGUOI_CAO_TUOI` | `VE_VIEC_NGUOI_THAN`) và `maTruongHop` (mã Q-354); thiếu đối tượng thì coi là về người cao tuổi và gắn dấu (research B-02, contracts mục 1.1).
- **Phạm vi "nhận thông báo"** (FR-014, Q-353): hàm thuần `XetPhamViDongY` chạy cho từng người thân trước `LocPhanNoiDung`, cả lúc tiếp nhận và lúc giải phóng giờ yên tĩnh (research B-04).
- **Mã trường hợp** (FR-014a, Q-354): lưu ở yêu cầu (mã khai) và ở dòng `thong_bao` của người nhận ngoài phạm vi (mã áp dụng); hiện trong lịch sử gửi (`DongLichSu`).
- **Phần chung, phần sức khỏe của sự cố** (FR-013, Q-352): 009 không đổi cơ chế; 007, 012 khai hai `PhanNoiDung` (`CHUNG`, `SUC_KHOE`) theo 007 research S-19a.
- Khuôn đổi nên mọi module nguồn có nhóm người thân phải khai đối tượng: 001, 004, 006, 007, 010, 011, 012, 013, 014 (điểm báo lại 7). Điểm báo lại 5 → 7 bên dưới.

### Cập nhật 2026-10-18, lượt 3 (spec mục "Cập nhật 2026-10-18, lượt 3": Q-366)

- **Nhóm (f) chính người cao tuổi** (FR-007): biến thể `NhomNhan.NguoiCaoTuoi(nctId)`, phân giải qua `TaiKhoanApi.theoNguoiCaoTuoi` (002): chỉ tài khoản không ở Đã khóa, Không hoạt động. Không có cách thay; rỗng thì không ghi lỗi gửi (research B-04).
- **Kiểm yêu cầu** (FR-022a): yêu cầu có nhóm (f) phải mức Nhẹ và chỉ có phần `CHUNG`; sai thì `BI_TU_CHOI` với mã `NHOM_NGUOI_CAO_TUOI_KHONG_HOP_LE` vào danh sách lỗi gửi (research B-02).
- **Kênh và giờ yên tĩnh**: Nhẹ chỉ trong ứng dụng, kênh `TRONG_UNG_DUNG` (đẩy tới app chung, `thiet_bi_day.ung_dung = APP_NGUOI_CAO_TUOI`); `GioYenTinh` áp cho người nhận `NGUOI_CAO_TUOI` như người thân (research B-06). Hộp thông báo của tài khoản người cao tuổi dùng endpoint hộp thư sẵn có.
- Người gửi yêu cầu nhóm (f): 012 (nhắc lượt thăm, phản hồi đã trả lời), 014 (nhắc hoạt động). Cảnh báo SOS (007) gửi nhân viên, không đổi gì ở 009.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 008: Spring Boot 4.1.x, Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock, Spring Security 7. Thêm cho kênh đẩy (Q-305): Firebase Admin SDK cho Java (backend), `firebase_messaging` (app Flutter chung, Q-346). Web dùng `EventSource` chuẩn cho SSE. Tin nhắn: SPI `NhaCungCapTinNhan` cùng bộ chuyển gọi API của nhà cung cấp SMS bên ngoài *(bổ sung 2026-10-19)*; nhà cung cấp và thư viện (HTTP client của Spring hay SDK của nhà cung cấp) chờ chủ dự án chốt (Q-06).

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test bảng cho bảy hàm thuần: `ChuoiThay`, `LocPhanNoiDung`, `ChonKenh`, `KhungGop`, `GioYenTinh`, `NguoiNhacGoi`, `TrangThaiThongBao`.
- SC-004: 200 yêu cầu trên dữ liệu phân công dựng sẵn.
- Kênh giả lập (FCM giả, nhà cung cấp tin nhắn giả có lỗi theo kịch bản); đo SC-001 bằng thời điểm FCM giả nhận.
- Test đồng thời: yêu cầu trùng khóa; hai thông báo Khẩn cấp cùng người cao tuổi quá hạn cùng lúc; ba "Xác nhận và tiếp nhận".
- Đồng hồ giả lập cho CFG-M13-01 → 06 (FR-042).

**Target Platform**: Như 000.
- Web: hộp thông báo (nhân viên, cổng người thân), luồng SSE, lượt nhắc gọi và ghi kết quả, danh sách liên hệ trực tiếp, lịch sử, lỗi gửi, cấu hình kênh.
- App nhân viên: nhận đẩy, hộp thông báo (xem ngoại tuyến), xác nhận trực tuyến, "Xác nhận và tiếp nhận", lượt nhắc gọi và ghi kết quả.
- App người thân: nhận đẩy, hộp thông báo, xác nhận thông báo Khẩn cấp.

**Project Type**: Web service (REST `/api/v1`, SSE), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Khẩn cấp đến thiết bị ≤ 10 giây (NFR-03, SC-001): outbox giao ngay sau commit; hàng Khẩn cấp riêng; xác định người nhận ≤ 1 giây; gửi song song trên luồng ảo.
- Lượt nhắc gọi ≤ 1 phút sau hạn (SC-002); giải phóng giờ yên tĩnh ≤ 5 phút (SC-003): job 20 giây.
- Hộp thông báo ≤ 1 giây: bảng trạng thái dẫn xuất có chỉ mục theo người nhận.
- Khối lượng: khoảng 3.000 thông báo mỗi ngày ở quy mô 300 người, 100 nhân viên, 600 người thân (NFR-01).

**Constraints**:
- Thông báo, lần gửi, xác nhận, kết quả gọi không bị `UPDATE`, `DELETE`; không thu hồi (constitution XII, FR-036).
- Lỗi gửi không làm hỏng lệnh nguồn (FR-006).
- Xác nhận Khẩn cấp, ghi kết quả gọi bắt buộc trực tuyến (Q-01).
- Nhật ký trong transaction cho lệnh người dùng (000 FR-047). CFG đọc lúc chạy (constitution XIII).

**Scale/Scope**:
- 2 thực thể có vòng đời tính được (thông báo, lượt nhắc gọi) và 1 có lệnh (liên hệ trực tiếp); 18 bảng (gồm bảng kỹ thuật).
- 2 job (20 giây, hằng ngày).
- 16 đường dẫn, 16 thao tác REST ([contracts/api-009.yaml](contracts/api-009.yaml)) và một luồng SSE.
- 1 khuôn dùng chung ở `common`; 1 SPI 009 khai báo; 4 SPI 009 cài; 1 giao diện công khai.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR. Data-model mục 11 là bảng truy vết BR-M13-01 → 05 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; FCM, SSE chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Kênh theo mẫu nhóm 1. Thông báo, lượt nhắc chỉ có sự kiện ghi thêm; liên hệ trực tiếp chỉ có lệnh ghi kết quả |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 4, 6, 7 khớp FR-044, FR-030, FR-021. Mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M13-01 → 06 đọc từ `ThamSoApi`. Chu kỳ job 20 giây là thông số kỹ thuật (Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Khóa quyền theo bảng FR-039 (B-12). Không thêm vai trò |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ; điểm chưa khớp với Q-212, Q-307 báo lại (điểm 1, 2) |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-009 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 000 (`common`) và 007 được bổ sung (mục "Đồng bộ") |
| X. Kiến trúc | Đạt | Một khối; gói `m13`; khuôn yêu cầu ở `common` để không có vòng (B-02). REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Người nhận, lọc quyền, chọn kênh, giờ yên tĩnh, chặn nội dung, quyền gọi đều ở backend. Mỗi lệnh có endpoint riêng |
| XII. Dữ liệu | Đạt | Nhóm 3 ghi thêm; bảng trạng thái là dẫn xuất kỹ thuật (như `chuoi_dinh_chinh` của 000); `Clock`; `UNIQUE` cho FR-005, FR-012, FR-032 |
| XIII. Tham số | Đạt | Như V. Đổi CFG-M13-02 tính lại thời điểm gửi dự kiến (FR-024) |
| XIV. Ngoại tuyến, thông báo | Đạt | 009 chính là dịch vụ chung; app di động nhận đẩy qua FCM (Q-305); xác nhận Khẩn cấp trực tuyến |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 009 có test; phần ở client (xác nhận hai thao tác, chặn ngoại tuyến) có test Flutter |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có năm bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Khuôn yêu cầu ở `common.thongbao`.
2. Bảng trạng thái dẫn xuất `thong_bao_trang_thai`, `luot_nhac_goi_trang_thai`.
3. FCM và SSE cho kênh trong ứng dụng.
4. Job 20 giây.
5. Bảng `thiet_bi_day`.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 009 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 100 nhân viên, 600 người thân; chỉ mục theo người nhận, bản ghi nguồn |
| NFR-02 Hiệu năng | Có | Hộp thông báo đọc bảng dẫn xuất; xác nhận một dòng sự kiện |
| NFR-03 Thông báo khẩn cấp | Có | Outbox giao ngay sau commit, hàng Khẩn cấp riêng, gửi song song mọi kênh, FCM |
| NFR-04 Tác vụ định kỳ | Có | `UNIQUE` lượt nhắc, lần gửi lại, khung gộp; chạy bù khi khởi động |
| NFR-05 Khả dụng | Có | Outbox giao lại khi listener lỗi; hộp thông báo xem ngoại tuyến ở app |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL |
| NFR-07 Lưu trữ | Có | Không ngắn hơn CFG-M15-04 và bản ghi nguồn (data-model mục 10) |
| NFR-08 Bảo mật | Có | Lọc phần nội dung theo quyền hiện hành; kênh ngoài không có sức khỏe; số điện thoại che một phần |
| NFR-09 Thời gian | Có | `Clock`; giờ yên tĩnh, giờ hành chính theo Asia/Ho_Chi_Minh; thời điểm thiết bị không dùng |
| NFR-10 Thiết bị | Có | Đẩy tới app nhân viên và app người thân; web SSE; xác nhận hai thao tác |
| NFR-11 Ngôn ngữ | Có | Tiêu đề, tin tổng hợp tiếng Việt; thời điểm dd/MM/yyyy HH:mm |
| NFR-12 Cấu hình | Có | CFG-M13 qua tham số; kênh qua màn cấu hình |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; kênh giả lập |

## Project Structure

### Documentation (this feature)

```text
specs/009-notification-service/
├── plan.md                      # File này
├── research.md                  # Phase 0: B-01 → B-13
├── data-model.md                # Phase 1: bảng, bảng chuyển, bảo đảm ở cơ sở dữ liệu, truy vết
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-009.yaml             # OpenAPI 3.1: hộp thông báo, khẩn cấp, lượt nhắc gọi, liên hệ, tra cứu, kênh
│   └── module-interfaces.md     # Khuôn yêu cầu ở common, API tra cứu, SPI, sự kiện
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/common/thongbao/   # YeuCauThongBao, CoYeuCauThongBao, NhomNhan, CachThay, TRUONG_SUC_KHOE

backend/src/main/java/com/cambae/backend/m13/
├── api/           # ThongBaoTraCuuApi, SPI NguoiThanNhanThongBaoPort, sự kiện
├── tiepnhan/      {internal}        # listener CoYeuCauThongBao, kiểm tra, chống trùng, lỗi gửi
├── nguoinhan/     {internal}        # XacDinhNguoiNhan, ChuoiThay, LocPhanNoiDung
├── gui/           {internal}        # BoPhatKenh, ChonKenh, KhungGop, FCM, NhaCungCapTinNhan, kiểm nội dung kênh ngoài
├── yentinh/       {internal}        # GioYenTinh, giữ, giải phóng
├── khancap/       {internal, web}   # xác nhận, xác nhận và tiếp nhận, lượt nhắc, kết quả gọi, NguoiNhacGoi
├── hopthu/        {internal, web}   # hộp thông báo, SSE, thiết bị đẩy
├── lienhe/        {internal, web}
├── tracuu/        {internal, web}   # lịch sử, lỗi gửi, theoNguon
├── cauhinh/       {internal, web}
└── lich/                            # ViecNhanhM13, ViecHangNgayM13

frontend/src/tinh-nang/thong-bao/           # hộp thông báo, lượt nhắc gọi, liên hệ trực tiếp, lịch sử, lỗi gửi, cấu hình kênh
mobile/lib/nhan-vien/thong-bao/ # đẩy, hộp thư, xác nhận, xác nhận và tiếp nhận, ghi kết quả gọi
mobile/lib/nguoi-than/thong-bao/
```

**Structure Decision**: Theo R-02 của 000. Feature 009 nằm ở `backend/…/m13` với chín gói con và gói `api`; khuôn yêu cầu ở `common.thongbao`. Web, app nhân viên, app người thân đều có hộp thông báo và nhận đẩy.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Khuôn yêu cầu ở `common.thongbao` | FR-001 đòi một cách gửi; M13 cần đọc dữ liệu M01, M05, M09, M15 nên mọi module không thể phụ thuộc `m13.api` | `m13.api`: vòng phụ thuộc; 009 cài bộ chuyển cho từng kiểu sự kiện: 009 phải biết nghiệp vụ của mọi module |
| Bảng trạng thái dẫn xuất | FR-044 đòi trạng thái suy ra từ sự kiện ghi thêm; hộp thông báo, job cần truy vấn theo trạng thái | Tính trạng thái từ sự kiện mỗi lần đọc: chậm với hàng triệu dòng; sửa dòng nhóm 3: trái FR-036 |
| FCM, SSE | Q-305 chốt thông báo đẩy cho app di động; NFR-03 ≤ 10 giây | Chỉ hộp thư, client hỏi định kỳ: không đến thiết bị đang khóa màn hình, không đạt NFR-03 |
| Job 20 giây | SC-002 ≤ 1 phút sau hạn, SC-003 ≤ 5 phút, khung gộp CFG-M13-03 cần đóng đúng lúc; chu kỳ là thông số vận hành | Job mỗi phút: sát giới hạn SC-002 khi có trễ outbox |
| `thiet_bi_day` | FCM cần token theo thiết bị; 002 chỉ giữ mã thiết bị | Lưu token ở 002: 002 phải biết kênh đẩy |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung hoặc dựa vào các điểm dưới đây; plan của feature liên quan giữ đúng hoặc ghi rõ thay đổi.

1. **000** (đã ghi bổ sung vào [../000-shared-foundation/contracts/module-interfaces.md](../000-shared-foundation/contracts/module-interfaces.md) mục 1.1): khuôn `common.thongbao` (`YeuCauThongBao`, `CoYeuCauThongBao`, `NhomNhan`, `CachThay`, `LoaiThongTin`, danh mục `TRUONG_SUC_KHOE`). Các sự kiện 000 phát cho 009 (yêu cầu phê duyệt, mục 1.5) cài `CoYeuCauThongBao`.
2. **001 → 008**: mọi sự kiện đã ghi "009 nghe" trong module-interfaces của các plan này cài `CoYeuCauThongBao` và dựng yêu cầu với mức, nhóm, loại thông tin, khóa đã nêu ở plan đó (001 H-14, 003 G-12, 004 T-12, 005 C-13, 006 D-17, 007 S-19, 008 N-14). Không đổi hành vi. Ánh xạ các tên nhóm đã dùng sang `NhomNhan` ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.1.
3. **007** (đã ghi bổ sung vào [../007-health-alert-incident/contracts/module-interfaces.md](../007-health-alert-incident/contracts/module-interfaces.md) mục 1.6): `CanhBaoLenhApi.tiepNhan`, `SuCoLenhApi.tiepNhan` cho "Xác nhận và tiếp nhận" (FR-026a). 009 cài `NhatKyThongBaoPort` qua `ThongBaoTraCuuApi.theoNguon`; nghe `YeuCauDungNhacGoiDien`, `DaCoKetQuaXacNhanLai`.
4. **002**: mã thiết bị (A-02) dùng làm khóa `thiet_bi_day`; 009 không đổi phiên đăng nhập. **008**: `CaTrucApi.dangTrongCa`, `NhiemVuApi`, `PhanCongApi` như đã khai báo. **012** cài `NguoiThanNhanThongBaoPort`; nghe `ThongBaoDaXem` (BR-M10-06). **016** (giai đoạn sau, Q-194) đọc thống kê từ `thong_bao_trang_thai`, `ket_qua_goi`.
5. *(Bổ sung 2026-10-18, Q-366)* **002** cung cấp `TaiKhoanApi.theoNguoiCaoTuoi` (002 module-interfaces 1.6). **012**, **014** khai nhóm `NguoiCaoTuoi` cho các thông báo ở 012 FR-069, 014 FR-071.

## Điểm cần báo lại về tài liệu nguồn

**(2026-10-19) Nhà cung cấp SMS — chờ chủ dự án chốt.** Chủ dự án chọn dùng dịch vụ SMS bên ngoài; Q-06 ở 24.2 của tài liệu nghiệp vụ vẫn ghi "chưa chọn; giai đoạn đầu chỉ thông báo trong ứng dụng; kênh tin nhắn là cấu hình bật sau". Khi chốt nhà cung cấp cần sửa Q-06 (chủ dự án yêu cầu sửa tài liệu nghiệp vụ), đăng ký brandname nếu nhà cung cấp trong nước yêu cầu, rồi làm T026a. Nội dung tin nhắn đã không chứa thông tin sức khỏe (FR-020), nên rủi ro dữ liệu ra nước ngoài thấp hơn tệp ở S3.

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Vai trò không xếp ca ở FR-016a** chỉ nêu Quản lý viện, Hành chính; Q-212 (spec 008 Assumptions) xếp Kế toán là vai trò không xếp ca, "trong ca" theo CFG-M13-06. Plan áp cho cả Kế toán. Đề nghị bổ sung Kế toán vào FR-016a. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
2. **Câu chữ còn sót trước Q-307 trong spec 009.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Bảng FR-043b dòng 004 "Đề xuất hồ sơ chờ khi có giường trống (FR-013)": sau Q-307 không còn đề xuất; nên đổi thành "Báo có giường trống phù hợp" theo spec 004 hiện hành.
   - Key Entities "Lượt nhắc gọi *(sửa 2026-10-06, Q-307: thay "Lượt nhắc gọi")*" tự thay chính nó; nên ghi "thay Yêu cầu gọi điện".
   - Điểm báo lại 1, 2, 7 của spec còn dùng "yêu cầu gọi điện".
3. **Danh mục trường thông tin sức khỏe dùng để chặn nội dung rút gọn** (FR-020): spec đòi "kiểm tra nội dung rút gọn theo các trường thuộc danh mục". Plan kiểm theo khóa tham số hiển thị mà module nguồn dùng để dựng nội dung rút gọn (research B-05), không phân tích văn bản tự do. Không đổi nghiệp vụ; ghi lại để chủ dự án biết giới hạn của cách kiểm. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
4. **(Đồng bộ câu chữ spec 009, không đổi nghiệp vụ)** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Assumptions còn câu dẫn `docs/phan-tich-yeu-cau.md` mục 4.2 (đã bỏ); có thể giữ làm lịch sử.
   - Phạm vi còn "CFG-M13-03 → CFG-M13-06 (đề xuất)" và FR-016a, FR-016b, FR-018 còn "đề xuất" dù các mã đã ở Phụ lục 25.

5. **(2026-10-18) "Phạm vi nhận thông báo" là cờ hay danh sách người.** 009 FR-014, 012 FR-054 nói "người thân nằm trong phạm vi 'nhận thông báo' của bản đồng ý", nhưng 001 FR-010 và 5.1 chỉ có một mục "đồng ý nhận thông báo" (data-model 001: `dong_y_thong_bao BOOLEAN`), không có danh sách người như phạm vi xem sức khỏe. Plan hiểu: cờ bật thì mọi người thân có quan hệ Hiệu lực (và đúng nhóm, đúng quyền) đều trong phạm vi; cờ tắt hoặc chưa có bản đồng ý Hiệu lực thì không ai. Nếu ý định là danh sách người thân, cần Q mới và sửa 001 FR-010 (thêm danh sách), 001 data-model và hàm `XetPhamViDongY`. *(Đã xử lý 2026-10-18: Q-357: một mục chung; thiết kế giữ nguyên, spec FR-014 đã làm rõ)*

6. **(2026-10-18) Yêu cầu Khẩn cấp thiếu mã trường hợp.** FR-001a bắt từ chối yêu cầu gửi người thân nội dung vượt phạm vi mà không có mã; FR-014 bắt người liên hệ chính luôn nhận Khẩn cấp. Hai quy tắc chỉ va nhau khi module nguồn quên mã. Plan: không gửi người thân ngoài phạm vi đó, ghi lỗi gửi `THIEU_MA_TRUONG_HOP`, vẫn gửi các nhóm nhân viên; test hợp đồng của 001, 007 kiểm mọi yêu cầu tới người thân có mã. Đề nghị xác nhận. *(Đã xử lý 2026-10-18: giữ theo plan, chủ dự án ủy quyền chốt)*

7. **(2026-10-18) Module nguồn cần khai đối tượng thông tin.** Spec 009 "Điểm cần báo lại" 1 của lượt 2026-10-18 đã nêu. Plan 001, 007 đã khai (cập nhật 2026-10-18); 012 khai khi cập nhật plan 012; 004, 006, 010, 011, 013, 014 cần khai ở spec rồi plan. Tới khi khai, yêu cầu của các module đó bị coi là về người cao tuổi và lọc theo bản đồng ý. *(Đã xử lý 2026-10-18: spec 004, 006, 010, 011, 013, 014 và plan của chúng đã khai; Q-362 chốt thuốc gửi, đồ gửi)*
