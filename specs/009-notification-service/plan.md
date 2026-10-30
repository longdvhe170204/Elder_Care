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

### Cập nhật 2026-10-22 (spec mục "Cập nhật 2026-10-22": Q-373 → Q-379, Q-385)

- **Nút hỗ trợ khẩn cấp** (007 FR-047i): cảnh báo Khẩn cấp dùng nhóm người nhận có sẵn của `common.notification` (`OnDutyDoctor`, `ShiftLead(tầng)`, `OnShiftStaff(tầng)`); 007 phát một `ShiftLead` cho mỗi tầng đang có ca khi chưa biết tầng. 009 không thêm nhóm, quy tắc hay kênh; phân giải các nhóm này vẫn cần `CaTrucApi` của 008 (Phase 3).
- **Vị trí, mốc tới nơi** (Q-373, Q-374): không vào nội dung thông báo; không thay đổi ở 009.
- **017** (Q-378, Q-379, Q-385): thêm thông báo phiếu báo phí, bảng quyết toán mới phát hành (Nhẹ, `FamilyWithRight(VIEW_COSTS)`, phần chi phí) do 017 phát theo khuôn; bỏ thông báo sao kê chưa khớp, chốt quỹ ngày (chưa có mã nào của 009 dựa vào hai thông báo này).
- Không thêm task; Phase 1, 2 đã xong không bị ảnh hưởng.

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
| X. Kiến trúc | Đạt | Một khối; gói `notification`; khuôn yêu cầu ở `common` để không có vòng (B-02). REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Người nhận, lọc quyền, chọn kênh, giờ yên tĩnh, chặn nội dung, quyền gọi đều ở backend. Mỗi lệnh có endpoint riêng |
| XII. Dữ liệu | Đạt | Nhóm 3 ghi thêm; bảng trạng thái là dẫn xuất kỹ thuật (như `chuoi_dinh_chinh` của 000); `Clock`; `UNIQUE` cho FR-005, FR-012, FR-032 *(R-15: `chuoi_dinh_chinh` → `dinh_chinh` (cột gốc, nếu không phá view))* |
| XIII. Tham số | Đạt | Như V. Đổi CFG-M13-02 tính lại thời điểm gửi dự kiến (FR-024) |
| XIV. Ngoại tuyến, thông báo | Đạt | 009 chính là dịch vụ chung; app di động nhận đẩy qua FCM (Q-305); xác nhận Khẩn cấp trực tuyến |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 009 có test; phần ở client (xác nhận hai thao tác, chặn ngoại tuyến) có test Flutter |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có năm bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Khuôn yêu cầu ở `common.thongbao`.
2. Bảng trạng thái dẫn xuất `thong_bao_trang_thai`, `luot_nhac_goi_trang_thai`. *(R-15: `thong_bao_trang_thai` → `thong_bao` (cột dẫn xuất); `luot_nhac_goi_trang_thai` → `luot_nhac_goi` (cột dẫn xuất))*
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
backend/src/main/java/com/eldercare/backend/common/thongbao/   # YeuCauThongBao, CoYeuCauThongBao, NhomNhan, CachThay, TRUONG_SUC_KHOE

backend/src/main/java/com/eldercare/backend/notification/
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

**Structure Decision**: Theo R-02 của 000. Feature 009 nằm ở `backend/…/notification` với chín gói con và gói `api`; khuôn yêu cầu ở `common.thongbao`. Web, app nhân viên, app người thân đều có hộp thông báo và nhận đẩy.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Khuôn yêu cầu ở `common.thongbao` | FR-001 đòi một cách gửi; M13 cần đọc dữ liệu M01, M05, M09, M15 nên mọi module không thể phụ thuộc `notification.api` | `notification.api`: vòng phụ thuộc; 009 cài bộ chuyển cho từng kiểu sự kiện: 009 phải biết nghiệp vụ của mọi module |
| Bảng trạng thái dẫn xuất | FR-044 đòi trạng thái suy ra từ sự kiện ghi thêm; hộp thông báo, job cần truy vấn theo trạng thái | Tính trạng thái từ sự kiện mỗi lần đọc: chậm với hàng triệu dòng; sửa dòng nhóm 3: trái FR-036 |
| FCM, SSE | Q-305 chốt thông báo đẩy cho app di động; NFR-03 ≤ 10 giây | Chỉ hộp thư, client hỏi định kỳ: không đến thiết bị đang khóa màn hình, không đạt NFR-03 |
| Job 20 giây | SC-002 ≤ 1 phút sau hạn, SC-003 ≤ 5 phút, khung gộp CFG-M13-03 cần đóng đúng lúc; chu kỳ là thông số vận hành | Job mỗi phút: sát giới hạn SC-002 khi có trễ outbox |
| `thiet_bi_day` | FCM cần token theo thiết bị; 002 chỉ giữ mã thiết bị | Lưu token ở 002: 002 phải biết kênh đẩy |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung hoặc dựa vào các điểm dưới đây; plan của feature liên quan giữ đúng hoặc ghi rõ thay đổi.

1. **000** (đã ghi bổ sung vào [../000-shared-foundation/contracts/module-interfaces.md](../000-shared-foundation/contracts/module-interfaces.md) mục 1.1): khuôn `common.thongbao` (`YeuCauThongBao`, `CoYeuCauThongBao`, `NhomNhan`, `CachThay`, `LoaiThongTin`, danh mục `TRUONG_SUC_KHOE`). Các sự kiện 000 phát cho 009 (yêu cầu phê duyệt, mục 1.5) cài `CoYeuCauThongBao`.
2. **001 → 008**: mọi sự kiện đã ghi "009 nghe" trong module-interfaces của các plan này cài `CoYeuCauThongBao` và dựng yêu cầu với mức, nhóm, loại thông tin, khóa đã nêu ở plan đó (001 H-14, 003 G-12, 004 T-12, 005 C-13, 006 D-17, 007 S-19, 008 N-14). Không đổi hành vi. Ánh xạ các tên nhóm đã dùng sang `NhomNhan` ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.1.
3. **007** (đã ghi bổ sung vào [../007-health-alert-incident/contracts/module-interfaces.md](../007-health-alert-incident/contracts/module-interfaces.md) mục 1.6): `CanhBaoLenhApi.tiepNhan`, `SuCoLenhApi.tiepNhan` cho "Xác nhận và tiếp nhận" (FR-026a). 009 cài `NhatKyThongBaoPort` qua `ThongBaoTraCuuApi.theoNguon`; nghe `YeuCauDungNhacGoiDien`, `DaCoKetQuaXacNhanLai`.
4. **002**: mã thiết bị (A-02) dùng làm khóa `thiet_bi_day`; 009 không đổi phiên đăng nhập. **008**: `CaTrucApi.dangTrongCa`, `NhiemVuApi`, `PhanCongApi` như đã khai báo. **012** cài `NguoiThanNhanThongBaoPort`; nghe `ThongBaoDaXem` (BR-M10-06). **016** (giai đoạn sau, Q-194) đọc thống kê từ `thong_bao_trang_thai`, `ket_qua_goi`. *(R-15: `thong_bao_trang_thai` → `thong_bao` (cột dẫn xuất); `ket_qua_goi` → `su_kien_chung`)*
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

## Ghi chú triển khai

Ghi khi triển khai (constitution XIV: lệch kỹ thuật ghi ở plan cùng lúc với mã). Tên tiếng Anh theo glossary mục 15.

### Phase 1, Phase 2 (2026-10-21)

- **Cấu trúc gói** (000 research R-02, `backend/README.md`; module lớn chia theo chức năng): `notification.tiepnhan` → `notification.internal.intake`, `notification.gui` → `notification.internal.delivery`, `notification.lich` → `notification.scheduling` cùng `notification.internal.job`; phần dùng chung giữa chức năng ở `notification.internal.domain` (`NotificationStatusRules`, `NotificationEventType`, `DeliveryOutcome`) và `notification.internal.persistence` (`NotificationStore`); provider của 000, 002 ở `notification.internal.governance`. Gói chỉ tạo khi có lớp (README mục 2), nên T002 không tạo trước `nguoinhan`, `yentinh`, `khancap`, `hopthu`, `lienhe`, `tracuu`, `cauhinh`; mỗi phase sau tạo gói của mình (`internal.recipient`, `internal.quiethours`, `internal.urgent`, `internal.inbox`, `internal.directcontact`, `internal.lookup`, `internal.channel`).
- **T001**: Firebase Admin SDK `com.google.firebase:firebase-admin` 9.11.0 (chủ dự án duyệt 2026-10-21). Tệp khóa dịch vụ đọc từ biến môi trường `FIREBASE_CREDENTIALS` (`eldercare.notification.fcm.credentials-file`); trống thì `UnconfiguredPushGateway` (không đẩy, thông báo vẫn vào hộp). Phần mobile (`firebase_messaging`) chờ mở mobile, nên T001 chưa `[x]`.
- **T002**: `M13ArchTest` kiểm (1) đúng một listener nhận `HasNotificationRequests` trong toàn mã chính và nó ở `notification`; (2) ngoài `notification` không lớp nào dùng `com.google.firebase..` hay `SmsProvider`. `FamilyNotificationPort` không vào luật (2) vì 012 phải cài nó.
- **T003**: bản giả `FakeFamilyNotifications`, `FakeSmsProvider` ở `BT/support/fake/notification/` (team-plan mục 3) thay `BT/support/gia009/`, đăng ký qua `FakeM13Config` (`@Primary`). FCM giả (`FakePushGateway`) nằm ở `BT/notification/` vì `PushGateway` là kiểu nội bộ của M13, module khác không dùng.
- **T004**: `RecipientGroup` thêm `DutyGroup` (interface đóng con) cho tám nhóm loại (a), mỗi nhóm có `substitution()`; biến thể (d), (e) theo bảng B-04; `FamilyRight` (`QuyenNguoiThan`) ở `common.notification` vì `FamilyWithRight` dùng; `HealthFields.KEYS` là danh mục khóa `displayParams` cấm ở nội dung rút gọn (khóa tiếng Anh: `diagnosis`, `allergies`, `underlyingConditions`, `vitalSigns`, `medication`, `assessmentResult`, `wishes`, `incidentContent`). `NguoiCaoTuoi` (nhóm (f)) để T041 thêm như tasks ghi. Thêm biến thể là thay đổi cộng thêm, không đổi chữ ký cũ.
- **T005**: `NotificationLookupApi` (`bySource`, `viewedAt`), `NotifiedPersonView` ở `api/dto` cùng `RecipientType`, `NotificationStatus`, `AcknowledgementMethod`; SPI `FamilyNotificationPort` (`activeRelationships`, `callOrder`, `contact`), `SmsProvider` (`send` → `SmsResult` với `SENT`, `TEMPORARY_FAILURE`, `PERMANENT_FAILURE`, `NOT_CONFIGURED`); sự kiện `NotificationViewed`, `UrgentNotificationAcknowledged`, `NotificationUnreachable`. "Tài khoản cổng (trạng thái)" của `QuanHeNhanThongBaoView` thành `portalAccountId` + `portalAccountActive`. Bản cài `NotificationLookupApi` làm ở T034.
- **T006**: `V009_01__notification.sql`. Yêu cầu bị từ chối vẫn lưu (FR-003) nên `source_record_type`, `source_record_id`, `event_key` cho phép `NULL` (`UNIQUE` không chặn dòng `NULL`). `notification_state.status` `NULL` khi đã gửi đi mà chưa kênh nào có kết quả. Cột sinh `once_key` của `notification_event` cho `SEND`, `NOT_SENT`, `VIEW`, `ACKNOWLEDGE`. `push_device.app` chưa có `RESIDENT_APP` (T041, `V009_04`); hết hạn token ghi `invalidated_at` (data-model mục 10 cần mốc để xóa sau 90 ngày). Kênh mặc định: `IN_APP`, `FAMILY_PORTAL` bật, `SMS` tắt (Q-06). Lần chạy job hằng ngày: `m13_job_run (job, business_date)`. *(R-15: `notification_event` → `su_kien_chung`)*
- **T007**: `NotificationIntakeListener` (`@ApplicationModuleListener`, sau commit qua outbox) gọi `NotificationIntake.receive`. Kiểm tra là hàm thuần `RequestValidation` (lý do từ chối `MISSING_SOURCE_RECORD`, `MISSING_EVENT_KEY`, `MISSING_RECIPIENT_GROUPS`, `MISSING_CONTENT`; phần nội dung rỗng tính như thiếu). Sự kiện nguồn mà `notificationRequests()` ném lỗi thì lưu một yêu cầu bị từ chối lý do `REQUEST_BUILD_FAILED` kèm tên lớp sự kiện, không thử lại (FR-006). Hai lần nhận đồng thời cùng khóa: lần chèn sau gặp `DuplicateKeyException`, đọc có khóa (`FOR SHARE`, vì ảnh chụp của transaction có thể có trước lần chèn kia) rồi ghi lần nhận trùng. Phase 2 chưa xác định người nhận nên chưa gắn dấu `NO_RECIPIENT` (T015).
- **T008**: `DeliveryDispatcher` gửi sau commit (`TransactionSynchronization.afterCommit`; ngoài transaction thì gửi ngay) trên luồng ảo; Khẩn cấp một executor riêng không giới hạn, thông báo thường giới hạn 4 lần gửi đồng thời (thông số kỹ thuật) để không chiếm hết pool kết nối; kênh gửi ngoài transaction, kết quả ghi trong transaction ngắn kèm tính lại trạng thái (khóa dòng `notification_state`). Một lần gửi đang chạy không được gửi lần nữa trong cùng tiến trình; `UNIQUE (attempt_id)` chặn kết quả thứ hai. `InAppSender` (`IN_APP`, `FAMILY_PORTAL`): luôn `SENT` (FR-018) và đẩy FCM tới mọi token còn hiệu lực của tài khoản, nội dung đẩy chỉ tiêu đề và câu chung (không có sức khỏe trên màn hình khóa); FCM trả `UNREGISTERED`/`INVALID_ARGUMENT` thì vô hiệu token. Khẩn cấp đẩy ưu tiên cao (Android `HIGH`, APNs 10). *(R-15: `notification_state` → `thong_bao` (cột dẫn xuất))*
- **T009**: `NotificationStatusRules.of(events, attempts)`; ưu tiên `ACKNOWLEDGED` > `UNREACHABLE` > `NOT_SENT` > (chưa `SEND`: `HELD_FOR_QUIET_HOURS`) > `VIEWED` > `SENT` > `DELIVERY_FAILED`; chỉ lần gửi mới nhất của từng kênh được tính; `BATCHED` coi như đã tới. `HIDE` không đổi trạng thái, chỉ bật `hidden`.
- **T010**: `M13FastJob` (cron `*/20 * * * * *`, `eldercare.jobs.m13-fast.cron`) chạy mọi bean `M13FastTask`; `M13DailyJob` (07:30 giờ Việt Nam, thông số vận hành) chạy mọi `M13DailyTask` mỗi ngày nghiệp vụ một lần, lúc khởi động sau 07:30 mà ngày đó chưa chạy thì chạy bù. Việc lỗi không chặn việc khác, lần chạy ghi `FAILED`. Hai job đã thêm vào danh sách FR-010a của `SchedulerScopeArchTest`. **Chưa thêm** `M13_FAST`, `M13_DAILY` vào `runJobs` của api-000 (đổi hợp đồng, chờ chủ dự án duyệt); tới đó test gọi job trực tiếp.
- **T011**: `NotificationFieldClassification` (`M13.NOTIFICATION`: `content.HEALTH` → `HEALTH`, `content.COSTS` → `COSTS`, `address` → `CONTACT`; loại khác không có nhóm tương ứng); `NotificationRequestRetention` (`M13.NOTIFICATION_REQUEST`, `NotResidentLinked` từ `received_at`; yêu cầu gắn người cao tuổi chưa hết hạn tới khi 001 có ngày kết thúc lưu trú, như yêu cầu phê duyệt của 000; xóa cả chuỗi thông báo, sự kiện, lần gửi, kết quả); `NotificationScopeAssignment` (`M13.NOTIFICATION`: người cao tuổi, tầng của yêu cầu). Phân loại `M13.CALL_RESULT`, `M13.DIRECT_CONTACT_RESULT` làm cùng T019, T027.
- **T012**: `FR_003_RequestValidationIT`, `FR_005_DeduplicationIT` (gồm hai lần nhận đồng thời), `FR_006_SourceUnaffectedIT`; thêm `NFR_03_DeliveryDispatcherIT` (T008), `NFR_04_M13CatchUpIT` (T010, tên logic `ChayBuM13IT`), `M13ProvidersIT` (T011), test đơn vị `RequestValidationTest`, `NotificationStatusRulesTest`, `RecipientGroupTest`, `FakeM13Test`.
- **Không phụ thuộc 008 ở Phase 2**: `DutyApi`, `AssignmentApi` (tên logic `NhiemVuApi`, `PhanCongApi`) đã có ở `administration.account.api.spi` kèm bản giả; `CaTrucApi` (`dangTrongCa`, `caDangDienRa`, M09) cần từ Phase 3 (nhóm `OnShiftStaff`, `OnDutyDoctor`) và Phase 5 (FR-016a), đã ghi ở docs/team-plan.md mục 8 cho E.
- *(2026-10-22, nhánh `impl/009-notification-service` từ `develop` sau PR #19)* **`runJobs`**: chủ dự án duyệt thêm `M13_FAST`, `M13_DAILY` vào `runJobs` của api-000 (`TestClockController.TestJob`); test `TestClockControllerIT`.
- **Phase 3 (US2), phần làm được** (chủ dự án chọn làm phần của A, chờ B, E): `notification/internal/recipient/` (tên logic `nguoinhan`): hàm thuần `SubstitutionChain` (`ChuoiThay`, bậc `SubstitutionStep`), `ContentFilter` (`LocPhanNoiDung`), `ConsentScope` (`XetPhamViDongY`); `RecipientResolver` (`XacDinhNguoiNhan`) và `NotificationCreation` nối vào `NotificationIntake`. **Thứ tự trong transaction tiếp nhận**: kiểm tra → chống trùng → xác định người nhận → thêm yêu cầu kèm kết quả nhóm (`outcome`) và dấu `NO_RECIPIENT`, `MISSING_Q354_CASE` → thêm `notification`, sự kiện `SEND`, lần gửi dự kiến (`IN_APP` cho nhân viên, `FAMILY_PORTAL` cho người thân có tài khoản cổng) → bộ phát gửi sau commit. Lý do đổi thứ tự so với research B-03: `notification_request`, `notification_request_recipient_group` chỉ cho `INSERT` (dấu đặt một lần lúc tiếp nhận, data-model). *(R-15: `notification_request_recipient_group` → cột `JSON` của `yeu_cau_thong_bao`)*
  - Nhân viên là người nhận theo hồ sơ nhân viên (`recipient_id` = id nhân viên, `account_id` = tài khoản); nhân viên không có tài khoản không nhận. FR-008: loại Nghỉ việc (`StaffProfileApi.status`), tài khoản Đã khóa, Không hoạt động; Khóa tạm vẫn nhận.
  - Căn cứ (`basis`): tên nhóm (`AssignedNurse`, `ShiftLead`…), `SUBSTITUTE:<nhóm>` khi thay, `FacilityRole:<vai trò>`, `Approvers:<loại>`, `Staff:<căn cứ nguồn>`, `FamilyMember:<căn cứ>`, `FamilyWithRight:<quyền>`, `NO_RECIPIENT_FALLBACK` (FR-011).
  - FR-011: Khẩn cấp không có ai thì mọi Quản lý viện, Trưởng tầng Hoạt động nhận thay; Trưởng tầng không được giao tầng của yêu cầu có người cao tuổi thì `identity_only`.
  - FR-014 urgent: người liên hệ chính luôn qua lọc khẩn; người thân khác cần `RECEIVE_URGENT`.
  - **Giao diện mới** ("Đổi api", chủ dự án duyệt): `AccountApi.activeWithRole(Role)` (002; tài khoản Hoạt động hoặc Khóa tạm có vai trò) cho nhóm (b); SPI `AssignmentApi.staffAssignedToResident`, `staffAssignedToRoom` (khai báo ở 002, E cài ở 008; bản giả `FakeAssignments` chỉ xét phân công gắn thẳng đối tượng) cho `AssignedNurse`, `AssignedStaff`.
  - **Chưa làm, chờ module khác** (hợp đồng mục 2.3 ghi "Có sẵn" nhưng chưa có trên `develop`): `OnShiftStaff`, `OnDutyDoctor`, loại người vắng ca cần `CaTrucApi` (E): hiện coi như không có người rồi theo cách thay; `BanDongYApi` (`baoGom`, `hienHanh`; B): hiện người thân luôn ngoài phạm vi "nhận thông báo" (không khẩn về người cao tuổi chỉ gửi khi có mã Q-354) và không thấy phần sức khỏe, đúng mặc định an toàn của FR-013, FR-014; FR-015 (`NguoiCaoTuoiApi.thoiDiemTrangThaiCuoi`; B), nội dung "chỉ nhận dạng" (`nhanDang`; B) chưa áp. T013 → T015a để `[ ]` tới khi đủ.
  - Lần gửi đầu của thông báo vừa tạo dùng `NotificationStore.planFirstAttempt` (chèn thẳng lần thử 1): `planAttempt` (`INSERT … SELECT MAX(attempt_no)`) khóa khoảng chỉ mục `delivery_attempt`, làm các lượt tiếp nhận song song deadlock (thấy ở `FR_005_DeduplicationIT`); sự kiện lỗi để dở làm context test sau phát lại và hỏng. *(R-15: `delivery_attempt` → `su_kien_chung`)*
  - Test: `SubstitutionChainTest`, `ContentFilterTest`, `ConsentScopeTest`, `RecipientResolutionIT` (11), `AccountByIdIT.activeWithRole…`; `M15AccountEventsIntakeIT` kiểm dấu bằng "có chứa" vì có người nhận hay không tùy dữ liệu tài khoản. `./mvnw clean verify` qua trên JDK 25 (157 đơn vị, 424 tích hợp).
- *(2026-10-22, Phase 7 phần backend, nhánh `impl/009-notification-service` từ `develop` sau PR #20)* **T032 → T034**:
  - Hộp thông báo (`notification/internal/inbox`, tên logic `hopthu`): `InboxService` theo người nhận của tài khoản đang đăng nhập (`notification_state.recipient_key` = `<loại>:<id hồ sơ>`); Khẩn cấp chưa xác nhận lên đầu rồi mới gửi trước, phân trang theo khóa `(khẩn chưa xác nhận, sent_at, id)` (khóa `nextCursor` dạng `<0|1>_<micro giây>_<id>`, 50 dòng một trang). Mở lần đầu ghi sự kiện `VIEW` (khóa `once_key` giữ một lần) và phát `NotificationViewed`. Ẩn chỉ khi đã xem, chưa xem thì 409 `NOTIFICATION_NOT_VIEWED`.
  - Hiển thị theo quyền hiện hành (FR-034): nhân viên còn phạm vi với người cao tuổi (`ScopeApi.inScope`) và `FieldVisibilityApi` đầy đủ cho `HEALTH`, `COSTS` (như `NotificationFieldClassification`); người thân theo quan hệ còn hiệu lực và `ContentFilter` với quyền hiện hành (chưa có bản đồng ý nên không hiện phần sức khỏe); `identity_only` thì không hiện phần nào. `sourceRecord.canOpen`, `canAcknowledgeAndTakeCharge` để `false` (khóa quyền xem của module nguồn, US1).
  - SSE: `NotificationStreams` giữ luồng theo tài khoản (30 phút), `InAppSender` đẩy sự kiện `notification` (id, mức) khi thông báo vào hộp. Thiết bị đẩy: `DeliveryStore.registerToken` (thêm hoặc cập nhật theo `(account_id, device_id)`, bỏ dấu vô hiệu); `V009_02` thêm `RESIDENT_APP` cho cột `push_device.app` theo hợp đồng.
  - Tra cứu (`notification/internal/lookup`, tên logic `tracuu`): `NotificationLookupService` cài `NotificationLookupApi` (`bySource`, `viewedAt`); lịch sử gửi (Quản lý viện toàn viện; Trưởng tầng các tầng có phân công `FLOOR_MANAGER` lúc tra; vai trò khác 403; tối đa 500 dòng); lỗi gửi chỉ Quản lý viện (`REJECTED`, dấu `MISSING_LEVEL`, `MISSING_INFORMATION_TYPE`, `NO_RECIPIENT`, `DELIVERY_FAILED`, `INVALID_CONTENT`). Quyền theo bảng FR-039 (Phụ lục 27 không có dòng thông báo) nên tự kiểm theo vai trò, không dùng `@RequiresPermission`. Lượt nhắc gọi trong kết quả rỗng tới US1.
  - Hợp đồng: `ApiContractIT` nay so cả api-009 (kéo phần T036 lên), 9 endpoint của US1, US3 ở danh sách chờ. Schema `Problem` của api-009 sửa cho giống `Problem` chung của api-000, api-002 (mã trả `ProblemBody` chung); không đổi endpoint hay trường nghiệp vụ.
  - Test: `InboxIT` (8: FR-033, FR-034 xem lần đầu, 404, phạm vi nhân viên, quyền người thân, FR-036, thiết bị đẩy, SSE), `NotificationLookupIT` (3: FR-037, FR-038, FR-035). T035 (màn web) chờ duyệt bố cục.
- *(2026-10-23, Phase 6 phần backend, nhánh `impl/009-notification-service`)* **T029, T030, T040, T041** (gói `notification/internal/quiethours`, tên logic `yentinh`):
  - `QuietHours` (hàm thuần, giờ Việt Nam; hai mốc bằng nhau là không có giờ yên tĩnh); `QuietHoursPolicy` đọc CFG-M13-02 dạng bảng `{"from","to"}` một lần mỗi yêu cầu; viện chưa đặt giá trị thì dùng mặc định 21:00–07:00 của FR-022 (giả định, để không gửi người thân ban đêm). Áp cho người nhận `FAMILY_MEMBER`, `RESIDENT` mức Nhẹ, Trung bình; nhân viên, Khẩn cấp không giữ.
  - Giữ lúc tiếp nhận (`NotificationCreation`): sự kiện `HOLD` kèm thời điểm gửi dự kiến, không có `SEND`, không có lần gửi (hộp thông báo chỉ hiện thông báo có `sent_at`, nên cổng cũng chưa thấy).
  - Giải phóng: việc 20 giây `RELEASE_QUIET_HOURS` (`QuietHoursRelease`), theo `received_at` của yêu cầu, mỗi thông báo một transaction có khóa dòng trạng thái. Xét lại (FR-025) theo căn cứ đã ghi ở `notification.basis`: `PrimaryContact`, `Representative`, `FamilyWithRight:<quyền>` phải còn đúng với quan hệ hiện hành, `FamilyMember:*` (nhóm (d)) không cần quan hệ; phạm vi đồng ý (`ConsentScope`) và còn ít nhất một phần nội dung được xem (`ContentFilter`); người cao tuổi còn tài khoản Hoạt động hoặc Khóa tạm. Đủ thì `SEND` kèm lần gửi `FAMILY_PORTAL`/`IN_APP` theo tài khoản hiện hành, không đủ thì `NOT_SENT` lý do `RECIPIENT_NO_LONGER_ELIGIBLE`. Không xác định lại cả yêu cầu vì nhóm người nhận lưu dạng JSON không dựng lại được `Substitution`; người thân/người cao tuổi không có cách thay nên xét theo căn cứ là đủ.
  - Đổi CFG-M13-02 (FR-024): `QuietHoursRescheduler` nghe `ParameterChanged`, lấy giá trị mới từ `after` của sự kiện (bộ đệm tham số xóa ở một callback sau commit khác, có thể chưa xóa khi listener chạy), ghi `RESCHEDULE` cho mọi thông báo đang giữ có thời điểm dự kiến khác; bây giờ không còn trong giờ yên tĩnh thì dự kiến là bây giờ (cắt về micro giây như cột) rồi giải phóng ngay.
  - Nhóm (f) (T041): `RecipientGroup.Resident` ở `common.notification`; `RequestValidation` từ chối `INVALID_RESIDENT_GROUP` khi mức khác Nhẹ hoặc có phần `HEALTH`, `COSTS` (phần thiếu loại đã thành `HEALTH`); `RecipientResolver` lấy tài khoản qua `AccountApi.byResident` (Hoạt động, Khóa tạm), không có tài khoản thì không có người nhận và không gắn `NO_RECIPIENT` (FR-007). `push_device.app` đã có `RESIDENT_APP` từ `V009_02` nên không tạo `V009_04`.
  - **Chưa làm, chờ 001**: lý do `RESIDENT_DECEASED` (FR-015) và listener `ResidentStatusChanged` (T030), bản đồng ý hiện hành lúc giải phóng; tới đó thông báo "về người cao tuổi" chưa tới người thân nên test giờ yên tĩnh dùng đối tượng thông tin `ABOUT_FAMILY_MATTER`. Gộp tin nhắn của các thông báo giải phóng cùng lúc làm cùng US3.
  - Test: `QuietHoursTest` (4), `BR_M13_03_QuietHoursIT` (4: US4 #1, #2, #3, #6 phần quyền và quan hệ), `FR_024_ParameterChangeIT` (US4 #9; phát `ParameterChanged` như lệnh đổi của 000 sau commit, vì gọi API đổi tham số cần tài khoản Quản lý viện tạo theo đồng hồ năm 2030 làm hỏng `FirstManagerBootstrapIT`, `ManagerAlwaysExistsIT`), `FR_022a_ResidentRecipientIT` (R1 → R3); `RecipientGroupTest` thêm nhóm (f); `TestUsers.resident` cho tài khoản người cao tuổi.
- *(2026-10-23, Phase 5 phần backend, nhánh `impl/009-notification-service`)* **T023 → T028** (gói `notification/internal/delivery` cho kênh, `directcontact` tên logic `lienhe`, `channel` tên logic `cauhinh`):
  - **Số điện thoại nhân viên**: chưa có nguồn nào, chủ dự án duyệt thêm `StaffProfileApi.phoneNumber(staffId)` (luật "Đổi api", E cài ở 008; bản giả `FakeStaffProfiles.phone`; team-plan mục 8). Người thân lấy theo quan hệ, không có thì theo liên hệ của 012 (`RecipientPhones`).
  - **Chọn kênh** (`ChannelSelection`, hàm thuần): trong ứng dụng/cổng khi có tài khoản; tin nhắn cho Trung bình, Khẩn cấp khi kênh `SMS` bật; kênh khác (`OTHER_*`) ở mức đã gắn; người cao tuổi chỉ trong ứng dụng. Kênh tắt thì không có lần gửi. Lần gửi biết trước kết quả ghi ngay: `SKIPPED_OFF_SHIFT`, `INVALID_CONTENT`, `NO_ADDRESS`. **FR-016a chờ `CaTrucApi` (008)**: hiện mọi nhân viên coi như đang trong ca.
  - **Nội dung kênh ngoài** (FR-019, FR-020): `OutOfAppContent` xét lúc tiếp nhận (tham số hiển thị không lưu) và ghi dấu `INVALID_OUT_OF_APP_CONTENT` vào yêu cầu, để lúc giải phóng giờ yên tĩnh dùng lại; thiếu nội dung rút gọn cũng tính là không hợp lệ (không có gì để gửi). Tin nhắn gửi nội dung rút gọn, tin tổng hợp gửi nội dung riêng của lần gửi (cột `delivery_attempt.content`).
  - **Gộp tin nhắn Trung bình** (FR-016b): `OutOfAppPlanner` (dùng chung lúc tiếp nhận và lúc giải phóng): không có khung mở thì gửi ngay và mở khung CFG-M13-03 (`sms_batch_window.first_attempt_id`); có khung thì lần gửi ghi `batch_window_id`, chưa gửi; khung đã hết hạn mà việc 20 giây chưa đóng thì đóng ngay trong lệnh rồi xét lại. `SmsWindowCloser` (`CLOSE_SMS_WINDOWS`): có tin dồn thì thêm một lần gửi tin tổng hợp trên thông báo dồn sau cùng (`delivery_result` chỉ thêm nên không gắn được tin tổng hợp vào lần gửi đã có), các tin dồn ghi `BATCHED` kèm `digest_attempt_id`, mở khung mới bắt đầu bằng tin tổng hợp; khung người thân hết hạn trong giờ yên tĩnh thì chờ. **Lệch spec, để sau**: tin tổng hợp gửi nhân viên chưa có danh sách tầng/khu và ca (cần tên tầng của 003, ca của 008); các thông báo giải phóng cùng lúc lúc hết giờ yên tĩnh đi theo khung gộp (tin đầu gửi ngay, các tin sau trong tin tổng hợp), chưa gộp thành đúng một tin như Q-101. *(R-15: `delivery_result` → `su_kien_chung`)*
  - **Gửi lại** (FR-018): `SmsSender` ghi lý do lỗi có tiền tố `TEMPORARY`/`PERMANENT`; `OutOfAppResender` (`RESEND_OUT_OF_APP`) chỉ gửi lại lỗi tạm, lần gửi mới nhất của kênh, tới khi số lần thử vượt 1 + CFG-M13-05. Nhà cung cấp mặc định `UnconfiguredSmsProvider` (`CHANNEL_NOT_CONFIGURED`, Q-06); T026a chờ chốt nhà cung cấp.
  - **Liên hệ trực tiếp** (FR-021): `DirectContactDetector` (`DIRECT_CONTACTS`) thêm mục cho thông báo người thân không khẩn (Trung bình, hoặc nhóm (d) Q-112) đã gửi đi trong 2 ngày gần nhất (`LOOKBACK_DAYS`, giới hạn quét kỹ thuật, không phải hằng `Duration` theo XIII) mà không lần gửi nào tới được, không còn lần gửi chờ kết quả hay chờ gửi lại. Họ tên theo liên hệ của 012; `relationship` để trống vì `FamilyNotificationPort` không có quan hệ (hợp đồng có trường này). `DirectContactService.record`: Hành chính ghi, Không liên hệ được bắt buộc lý do (422 `REASON_REQUIRED`), mục đã có kết quả 409 `FINAL_STATE` (trigger chặn đổi sau trạng thái cuối), nhật ký `DIRECT_CONTACT_RECORDED`; đính chính qua 000 loại `M13.DIRECT_CONTACT_RESULT`. `DirectContactReminder` (`REMIND_DIRECT_CONTACTS`, việc hằng ngày): còn mục chờ thì gửi qua chính M13 một yêu cầu cho Hành chính và Quản lý viện, **mức Nhẹ (giả định theo mặc định Q-108, spec chưa nêu mức)**.
  - **Cấu hình kênh** (FR-016, FR-040): `ChannelService` chỉ Quản lý viện, bắt buộc lý do, nhật ký `NOTIFICATION_CHANNEL_CHANGED`; `levels` chỉ đổi với kênh khác. Schema `NotificationChannel` không có phiên bản nên `If-Match` so với ETag của cả danh sách kênh (tổng `version`), trả ở `GET` và `PUT`. Thân `PUT` khai `allOf` (`ChannelChangeSchema`) cho khớp hợp đồng. Hai lệnh tự kiểm theo vai trò, đã thêm vào `SELF_CHECKED` của `RequiresPermissionArchTest` (đổi sang `Map.ofEntries` vì quá 10 mục).
  - Job loại bỏ dữ liệu (`NotificationRequestRetention`) xóa thêm `direct_contact_result`, `direct_contact` trước thông báo. *(R-15: `direct_contact_result` → `su_kien_chung`; `direct_contact` → `ho_so_chung`)*
  - Test: `ChannelSelectionTest` (5), `SmsBatchingTest` (3), `BR_M13_01_ChannelSelectionIT` (4), `FR_016b_SmsBatchingIT`, `FR_021_DirectContactIT` (3), `FR_016_ChannelConfigIT` (2); `RequestValidationTest` cập nhật dấu mới; `ApiContractIT` bỏ bốn endpoint US3 khỏi danh sách chờ. Màn web T028 làm cùng T035 (mục dưới).
- *(2026-10-23, Phase 7 và Phase 5 phần web; bố cục chủ dự án duyệt, DESIGN.md 5.19.0)* **T035, T028 phần web** (`frontend/src/features/notifications/`, tên logic `tinh-nang/thong-bao`):
  - Mục menu "Thông báo" ngay dưới "Yêu cầu phê duyệt" cho mọi vai trò nhân viên (`NotificationsPage`); tab theo bảng FR-039: Hộp thông báo (mọi vai trò), Lịch sử gửi (Quản lý viện, Trưởng tầng), Lỗi gửi (Quản lý viện), Liên hệ trực tiếp (Hành chính ghi, Quản lý viện xem), Kênh (Quản lý viện). Một tab thì không hiện thanh tab.
  - `npm run gen:api` thêm `009` vào hợp đồng đã dịch (`src/api/api009.js`).
  - Hộp thông báo (`NotificationInbox`, `NotificationDetail`): khối "Khẩn cấp chưa xác nhận" trên cùng, lọc mức, chưa xem, loại sự kiện (lấy từ thông báo đã tải; hợp đồng không có danh mục loại sự kiện); thông báo mới qua `EventSource` (`/notifications/stream`, sự kiện `notification`) thì tải lại trang đầu, lỗi luồng thì hỏi lại mỗi 60 giây. Khung chi tiết chia khối theo loại thông tin, phần hết quyền là dòng khóa; "Ẩn khỏi danh sách" khi đã xem, có `Idempotency-Key`. Danh sách không có họ tên người cao tuổi vì schema `Notification` không có; lọc theo người cao tuổi chờ danh sách người cao tuổi của 001; nút "Xác nhận đã nhận" chờ US1.
  - Lịch sử gửi (`NotificationHistory`): lọc khoảng thời gian (`datetime-local` đổi sang ISO có múi giờ), bản ghi nguồn; người nhận hiện dạng "Nhân viên 301" (chưa có API tên người nhận); khung chi tiết có kênh, mã Q-354, dấu. Lỗi gửi (`DeliveryErrors`): lọc theo loại, lý do từ chối dịch sang chữ.
  - Liên hệ trực tiếp (`DirectContacts`): nút "Ghi kết quả" chỉ cho Hành chính (vai trò khung, backend vẫn kiểm); form Đã liên hệ / Không liên hệ được, thời điểm điền sẵn, lý do bắt buộc khi không liên hệ được (kiểm ở web và 422 của backend), `Idempotency-Key` giữ suốt form; 409 thì tải lại bảng. Kênh (`NotificationChannels`): không có nút Đổi cho Trong ứng dụng, Cổng người thân; mức chỉ chọn được cho kênh khác; If-Match theo ETag danh sách; 412 thì báo và tải lại bảng.
  - *(2026-10-23, sau merge develop có BL-23, PR #21)* Schema `Problem` của api-009 thêm `candidates` và `basis`, `module`, `waivable` trong `unmetConditions` cho giống bản chung. **US1 phần xác nhận** (T017, T019, T021 một phần): `UrgentAcknowledgement` (`notification/internal/urgent`, tên logic `khancap`) chỉ người nhận (`recipient_key` của tài khoản, khác thì 404), chỉ Khẩn cấp (409 `NOT_URGENT`), một lần (khóa dòng trạng thái; 409 `ALREADY_ACKNOWLEDGED`, cả khi `once_key` trùng); đang Không liên lạc được thì cách `LATE`; sự kiện `ACKNOWLEDGE` có người thực hiện, nhật ký `NOTIFICATION_ACKNOWLEDGED`, phát `UrgentNotificationAcknowledged`; thêm vào `SELF_CHECKED`. Dừng lượt nhắc gọi làm cùng phần lượt nhắc gọi. Web: nút "Xác nhận đã nhận" (nút chính) trên từng dòng của khối Khẩn cấp, 409 hiện ngay dòng đó. **T036**: `M13RecordGroupTriggerIT`.
  - Tông trạng thái mới vào `StatusBadge` theo DESIGN.md 3.5. Test: `NotificationInbox.test.jsx` (5), `NotificationAdmin.test.jsx` (8); 113 Vitest qua; `npm run lint`, `npm run build` qua. Đã xem trên trình duyệt (dev server, phản hồi API giả lập trong trang vì máy chưa đặt `DB_*` cho profile `demo`): sáng, tối, vai trò Trưởng tầng và Quản lý viện. Chưa thử với backend thật.
- *(2026-10-24, phần chờ 001 sau PR #23)* **T014, T015 phần bản đồng ý và qua đời, T015a, T029, T030, T035 lọc người cao tuổi**:
  - `notification/internal/recipient/service/ResidentFacts` đọc 001 tại thời điểm gửi: `notificationScope` = bản đồng ý Hiệu lực có `notificationConsent` (FR-014, Q-357: mục chung, mọi người thân có quan hệ Hiệu lực thuộc phạm vi; người thân nhóm (d) không có quan hệ thì ngoài phạm vi); `healthConsent` = `ConsentApi.includes` (FR-013, DBR-03); `deathAt` = `finalStatusAt` khi trạng thái là `DECEASED` (trạng thái cuối khác không chặn, FR-015).
  - `RecipientResolver`: yêu cầu có `occurredAt` sau thời điểm qua đời và không có dấu `afterDeath` thì bỏ mọi nhóm người thân (nhóm nhân viên vẫn gửi); tin báo qua đời cùng thời điểm vẫn gửi. Như bỏ vì ngoài phạm vi đồng ý, nhóm không còn ai thì tính "không có người nhận" (FR-011).
  - `QuietHoursRelease`: người thân của người cao tuổi đã Qua đời thì Không gửi lý do `RESIDENT_DECEASED` (trừ `AFTER_DEATH`), phạm vi và phần sức khỏe theo bản đồng ý hiện hành; `ResidentDeathListener` nghe `ResidentStatusChanged` sang `DECEASED` và chuyển ngay thông báo người thân đang giữ (`HeldNotificationStore.heldForFamilyOf`).
  - `InboxService`: phần sức khỏe trong hộp của người thân theo bản đồng ý hiện hành (FR-034).
  - Bản giả `support/fake/resident/FakeConsentApi` (`@Import(FakeConsentApi.Config.class)`, không trong `FakeM01Config`): 001 chưa có bản giả `ConsentApi`; A viết vào thư mục của B, báo B ở team-plan mục 8.
  - Web: ô "Người cao tuổi" trong thanh lọc hộp thông báo, danh sách là trang đầu `GET /residents` (trong phạm vi); 403 thì không hiện ô. Chưa xem trên trình duyệt với backend thật.
  - Test: `FR_014_015_ConsentAndDeathIT` (6), `InboxIT` thêm FR-034 rút đồng ý (10); `NotificationInbox.test.jsx` (7).
- *(2026-10-26, nối ca trực sau PR #38, nhánh `impl/009-shift-recipients` từ `develop` f1b57e0)* **T013 → T016, T024, T026** (phần chờ `CaTrucApi` của 008):
  - **Đổi api trên M09** (chủ dự án duyệt, phương án thêm hàm vào `ShiftApi`): `ShiftApi` có sẵn `currentShift`, `staffShifts`, `onShift`, `staff` nhưng không liệt kê được người trong ca của tầng hay Bác sĩ trực. A thêm `staffOnShift(floorAreaId, at)` (người có mặt trong ca đang diễn ra của tầng, trừ vắng ca, rời ca, nghỉ có duyệt) và `onDutyDoctors(at)` (dùng lại `ShiftQueries.doctors`: Bác sĩ trực có mặt ở ca cấp viện `FACILITY`); bản giả `FakeShifts.onDutyDoctor`, `reset`; test `ShiftQueriesIT` thêm 2. Ghi ở 008 module-interfaces mục 1.3, glossary 15.5; E review (team-plan mục 8).
  - `RecipientResolver`: `OnShiftStaff` lọc vai trò theo tài khoản (tập rỗng là mọi vai trò; `eligible` nhận `Set<Role>`) và chứng chỉ theo `LicenseApi.valid` ngày gửi theo giờ Việt Nam (mọi loại phải còn hiệu lực, như `ShiftQueries.qualified` của 008; không có nguồn giấy phép thì không ai đủ); `OnDutyDoctor` theo `onDutyDoctors`. Người vắng ca (FR-008) không có trong ca đang diễn ra nên tự bị loại; Trưởng tầng được giao không gắn ca nên không xét vắng ca (008 FR-024).
  - FR-016a: `OutOfAppPlanner` hỏi `ShiftApi.onShift` cho người nhận là nhân viên (008 đã tính giờ hành chính CFG-M13-06 cho Quản lý viện, Hành chính, Kế toán); chưa có bản cài thì coi như trong ca.
  - FR-016b, 008 FR-059: tin tổng hợp gửi nhân viên nêu tầng/khu vực (tên `FloorView.name` của 003 `LocationStructureApi`) và ca đang diễn ra lúc xảy ra sự kiện (`currentShift` tại `occurred_at`, không có thì `received_at`), dạng "Bạn có 2 thông báo mới (Tầng 2, ca 07:00–15:00; Tầng 3). Vui lòng mở ứng dụng để xem." (cách viết do A chọn, không nêu nội dung từng thông báo); không có tầng thì như tin cũ. `SmsWindowStore.Batched` thêm tầng và thời điểm. Lệch Q-101 (gộp đúng một tin khi giải phóng giờ yên tĩnh) giữ như ghi chú Phase 5.
  - T016: hàm thuần `CallReminderTarget` (`NguoiNhacGoi`, FR-029) trả bậc đầu tiên có người đủ điều kiện, bỏ chính người cần gọi; bậc Quản lý viện trả mọi người đủ điều kiện, việc chọn một người để giao lượt (cột `nguoi_duoc_nhac_id` một người, FR-030a "Quản lý viện đó") làm cùng T019; cần chủ dự án chốt cách chọn khi viện có nhiều Quản lý viện.
  - Lệch tên test: `SC_004_NguoiNhanTest` (tasks ghi test đơn vị) thành `SC_004_RecipientTableIT` vì `RecipientResolver` đọc tài khoản thật; gọi thẳng `RecipientResolver.resolve` 200 lượt, hạt giống cố định, bảng tính tay từ mô hình tầng trong test. Đã thấy đỏ khi tạm trả `RecipientResolver` về bản `develop`.
  - `BR_M13_01_ChannelSelectionIT`: đồng hồ đặt 10:00 thứ Ba gần nhất đã qua thay cho "hôm qua" (chạy ngày thứ Hai thì hôm qua là Chủ nhật, Kế toán ngoài giờ hành chính nên tin nhắn Trung bình bị bỏ qua đúng FR-016a); giữ nguyên assertion.
  - Test: `ShiftQueriesIT` (7), `BR_M13_04_ShiftGroupsIT` (5), `SC_004_RecipientTableIT` (1), `CallReminderTargetTest` (5), `FR_016a_OffShiftIT` (2), `SmsBatchingTest` (4). `./mvnw clean verify`: 625 đơn vị qua; 1390 tích hợp còn 7 thất bại, 10 lỗi ở đúng 10 lớp có sẵn (8 lớp resident, `roombedallocation.FR_035_LocationHistoryHttpIT`, `staffing.StaffImportTransportIT`), mọi IT notification xanh.
- *(2026-10-27, US1 lượt nhắc gọi, nhánh `impl/009-call-reminders` từ `develop` cb2d070)* **T017 → T021 phần không chờ 007**, gói `notification/internal/urgent` (tên logic `khancap`):
  - **#113 (`CredentialExpiryWarningClosed`)**: chủ dự án chọn 009 không làm gì. 008 FR-008 "cảnh báo được đóng khi có bản gia hạn" đã làm ở phía 008 (bỏ cờ nguồn, không nhắc lại); 009 giữ thông báo đã gửi theo Edge Cases "Nguồn đã được xử lý trước khi người nhận xem: thông báo không bị thu hồi". Không viết mã.
  - **Bảng** `V009_04__call_reminders.sql` (tasks ghi `V009_02`, số này đã dùng cho thiết bị đẩy): `call_reminder`, `call_reminder_notification` (`UNIQUE notification_id`, FR-032), `call_reminder_handoff`, `call_result` nhóm 3 có trigger; `call_reminder_state` kỹ thuật, cột sinh `pending_key` `UNIQUE` (một lượt Chờ mỗi (người cần gọi, người cao tuổi), không gắn người cao tuổi thì theo bản ghi nguồn). Lệch data-model: bảng chuyển nhắc thêm cột `tier` (bậc FR-029 của người được nhắc) để biết khi nào áp "hết ca" (FR-030a (b)); lượt đầu chuỗi để `call_chain_id` rỗng (bảng chỉ thêm, chưa biết id lúc chèn). *(R-15: `call_reminder_notification`, `call_reminder_handoff`, `call_result` → `su_kien_chung`; `call_reminder_state` → `luot_nhac_goi` (cột dẫn xuất))*
  - **Tạo lượt** (`CallReminderCreation`, việc 20 giây): thông báo Khẩn cấp của nhân viên, người thân đã gửi, chưa xác nhận, quá CFG-M13-01, hoặc mọi lần gửi đều có kết quả không tới hay không có lần gửi nào (FR-028; trễ tối đa một nhịp việc thay vì xét trong bộ phát). Mỗi thông báo một transaction, khóa dòng trạng thái rồi xét lại. Có lượt Chờ của cùng người cần gọi thì gộp (FR-027a), báo người được nhắc "nội dung cập nhật".
  - **Người được nhắc** (`CallReminderRouting` + `CallReminderTarget`): Người phụ trách ca, Trưởng tầng được giao qua `DutyApi` của 008 (đã bỏ người vắng ca), rồi Quản lý viện; đủ điều kiện là còn làm việc, tài khoản Hoạt động hoặc Khóa tạm. **Bậc Quản lý viện** (chủ dự án chọn 2026-10-26, viện chỉ có một Quản lý viện): giao cho Quản lý viện có tài khoản tạo sớm nhất (id tài khoản nhỏ nhất, `account.id` tự tăng), báo các Quản lý viện khác; nhắc lại cùng người đó khi quá hạn (FR-030a "Quản lý viện đó").
  - **Chuyển nhắc** (`CallReminderHandoffs`, việc 20 giây): ở bậc Người phụ trách ca mà người đó không còn là Người phụ trách ca đang diễn ra của tầng (hết ca, vắng ca) thì nhắc người đang phụ trách ca, không có thì bậc kế tiếp, lý do `SHIFT_ENDED`; quá hạn CFG-M13-04 thì bậc kế tiếp (`OVERDUE`), ở Quản lý viện thì `MANAGER_REPEAT`; hạn tính lại từ lần nhắc; báo người được nhắc trước.
  - **Kết quả gọi** (`CallResultService`, `POST /call-reminders/{id}/results`): người có quyền gọi theo nhiệm vụ lúc ghi (403 `NOT_ALLOWED_TO_CALL`); Đã liên lạc được bắt buộc ghi chú, Không liên lạc được bắt buộc lý do (422); lượt đã kết thúc 409 `CALL_REMINDER_ENDED`; số đã gọi mặc định là số hiện hành. Sau "Không liên lạc được" ghi được lần gọi bổ sung (`supplementary`). Người được nhắc khác người ghi thì được báo.
  - **Chuỗi gọi** (FR-029a, FR-044): "thông báo của chuỗi" là mọi thông báo gộp vào các lượt của chuỗi; lượt người thân kế tiếp (`PREVIOUS_NOT_REACHED`) không gộp thêm thông báo, nội dung cần báo lấy từ chuỗi. **Đã liên lạc được** chỉ xác nhận qua cuộc gọi các thông báo gộp vào chính lượt vừa gọi (FR-030 "mọi thông báo gộp vào lượt"): gọi được người đại diện R khi không gọi được M thì thông báo của M giữ nguyên chưa xác nhận (chính M chưa biết tin, FR-026) và cũng không chuyển Không liên lạc được (Q-104: chưa phải mọi cuộc gọi đều không liên lạc được); chuỗi dừng (chủ dự án chọn 2026-10-27, sau khi bỏ cách xác nhận cả chuỗi). Tra cứu FR-031 trả mọi lượt của chuỗi có thông báo, mỗi lượt kèm người cần gọi (`CallReminderSummary.personToCallType`, `personToCallId`; luật "Đổi api" ở `notification.api`, chủ dự án duyệt), để 007 thấy cả lượt gọi R. Quyết định ghi thành Q-388 ở nghiep-vu.md mục 24.2 và Clarifications của spec 009 (chủ dự án yêu cầu). Hết người (người thân kế tiếp theo `FamilyNotificationPort.callOrder` chưa gọi trong chuỗi; người cần gọi là nhân viên thì hết ngay) và không còn lượt Chờ của chuỗi: thông báo của chuỗi chuyển Không liên lạc được (`UNREACHABLE`, phát `NotificationUnreachable`), Quản lý viện (trừ chính người cần gọi) nhận Khẩn cấp kèm các cuộc gọi. Người thân kế tiếp đã có lượt Chờ về cùng người cao tuổi thì gộp vào lượt đó (FR-032) và chuỗi hiện tại không chuyển Không liên lạc được.
  - **Dừng khi tự xác nhận** (FR-030): `UrgentAcknowledgement` gọi `CallReminderActions.afterAcknowledged`; mọi thông báo của chuỗi đã xác nhận thì lượt Chờ dừng `SELF_ACKNOWLEDGED`, báo người được nhắc không cần gọi.
  - **Thông báo của lượt** (`CallReminderNotices`): qua chính M13 (nguồn `M13.CALL_REMINDER`); nhắc gọi mức Khẩn cấp, không có số điện thoại; tin báo phụ (đã chuyển, dừng, cập nhật, người khác đã ghi, tới Quản lý viện) **mức Nhẹ (giả định theo mặc định Q-108, spec không nêu mức)**. Thông báo do M13 gửi không sinh lượt nhắc gọi (tránh nhắc gọi chính lượt nhắc).
  - **Đọc**: `GET /me/call-reminders` lượt Chờ tôi đang được nhắc hoặc có quyền gọi; `GET /call-reminders/{id}` cho người có quyền gọi hoặc người từng được nhắc (khác thì 403), số điện thoại hiện hành lúc mở; `personToCall.relationship` để trống (012 chưa có nhãn quan hệ, team-plan mục 8). FR-031: `NotificationLookupApi.bySource` trả lượt nhắc gọi và kết quả gọi. Đính chính `M13.CALL_RESULT` (`CallResultCorrection`; người được đính chính là người có quyền gọi lúc đính chính). Lưu giữ xóa lượt và cả chuỗi của thông báo hết hạn. `ApiContractIT` bỏ ba endpoint khỏi danh sách chờ; `CallResultService.record` vào `SELF_CHECKED`.
  - **Còn chờ**: "Xác nhận và tiếp nhận" (`CanhBaoLenhApi.tiepNhan`, `SuCoLenhApi.tiepNhan` của 007, D); dừng lượt theo `YeuCauDungNhacGoiDien`, `DaCoKetQuaXacNhanLai` (sự kiện 007 chưa khai báo ở `incident.api`); dừng khi người cần gọi không còn đủ điều kiện (quan hệ hết hiệu lực, mất quyền nhận khẩn: cần sự kiện của 012); mobile T022.
  - **Outbox, dữ liệu test**: sự kiện mang yêu cầu thông báo lưu ở `EVENT_PUBLICATION.SERIALIZED_EVENT` (VARCHAR 4000). Clean verify thứ nhất đỏ vì tin báo "các Quản lý viện khác" liệt kê mọi Quản lý viện trong một yêu cầu (DB test có hàng trăm tài khoản); tách mỗi người một sự kiện thì lần thứ hai ngập hàng đợi tiếp nhận (DB test giữ qua các lần chạy: 380 lượt Chờ, 1557 thông báo Khẩn cấp chưa xác nhận). Nay báo các Quản lý viện khác bằng một yêu cầu nhóm vai trò Quản lý viện, người đang giữ lượt loại qua `causedBy` (FR-012, mức Nhẹ); hết chuỗi báo nhóm vai trò, trừ khi người cần gọi là Quản lý viện; nội dung cần báo trong thông báo nhắc tối đa 500 ký tự, danh sách cuộc gọi tối đa 20 (thông số kỹ thuật); một lượt lỗi không dừng việc 20 giây. `CallReminderTestBase` dừng lượt Chờ và đánh dấu đã xác nhận thông báo Khẩn cấp còn sót trước mỗi test (hai việc xét toàn cơ sở dữ liệu).
  - Test: `CallReminderTargetTest` thêm 1 (6), `M13RecordGroupTriggerIT` thêm bốn bảng, `BR_M13_02_CallReminderIT` (7), `FR_029a_CallResultIT` (5, gồm FR-031 và gọi được người thân kế tiếp), `M13ProvidersIT` thêm lưu giữ (4). Test viết trước mã nhưng hai lớp IT chỉ chạy sau khi cài (trước đó không biên dịch được); trigger, hàm thuần, FR-031 đã thấy đỏ trước khi cài. `./mvnw clean verify`: 626 đơn vị qua; 1403 tích hợp còn 7 thất bại + 10 lỗi ở đúng 10 lớp có sẵn (8 lớp resident, `roombedallocation.FR_035_LocationHistoryHttpIT`, `staffing.StaffImportTransportIT`), mọi IT notification xanh.

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Lược đồ sau gộp và cách chuyển dữ liệu: data-model mục 0, 0.1; task T043.
- Bảo đảm DBR, BR sau gộp (XII): ràng buộc của bảng giữ riêng giữ nguyên; ràng buộc của bảng đã gộp chuyển theo cột "ràng buộc chuyển về backend" và đoạn "Bảo đảm" của data-model 0.1 (`khoa_duy_nhat`, cột sinh `UNIQUE`, `CHECK` theo `kind`, quyền theo cột, trigger khóa theo dòng, kiểm ở domain). Mỗi ràng buộc chuyển về domain có test tích hợp; khi làm task gộp, sửa cột "bảo đảm bằng" của bảng DBR áp dụng trong data-model.
- Hiệu năng (NFR): truy vấn trên bảng chung dùng chỉ mục `(loai_doi_tuong, doi_tuong_id, loai)` và cột sinh có chỉ mục khai báo ở 0.1; chạy lại phần đo của quickstart sau khi gộp.
- Dòng nhắc bảng cũ trong plan, research, tasks có ghi chú "R-15:" chỉ bảng đích.
