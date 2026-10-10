# Implementation Plan: Tài khoản và phân quyền

**Branch**: `002-account-permission` | **Date**: 2026-10-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-account-permission/spec.md`

## Summary

Feature 002 cung cấp cho mọi module:
1. Tài khoản nhân viên và người thân, trạng thái, đăng nhập, phiên (mục A, B; UC-68, UC-70).
2. Vai trò, ma trận quyền tối đa theo Phụ lục 27 (19.3 là căn cứ), thu hẹp, quyền gán thêm, quyền theo nhiệm vụ (mục C).
3. Phạm vi dữ liệu suy ra từ phân công và phân bổ giường (mục D).
4. Điều kiện pháp lý cho nghiệp vụ chuyên môn (mục E).
5. Một điểm kiểm tra quyền hiệu lực ba lớp BR-M15-01, gồm ngoại lệ sự cố, ngoại tuyến, lỗi dữ liệu nguồn (mục F).
6. Quyền hiệu lực của người thân (mục G).

Hướng kỹ thuật:
- Nằm ở `m15.taikhoan`, giữ đúng các giao diện mà 000 đã khai báo trước (research A-01).
- Token phiên opaque lưu phía máy chủ, để kết thúc phiên và áp quyền mới ngay ở yêu cầu kế tiếp (A-02).
- Ma trận quyền nạp từ Phụ lục 27 và 19.3, có test đối chiếu tự động với tài liệu nguồn (A-04).
- Thu hẹp và gán vai trò lưu theo thời điểm hiệu lực, nên tra được quyền quá khứ (A-05).
- Phạm vi tính trong từng yêu cầu, không đệm giữa các yêu cầu (A-07).
- Giới hạn trường áp khi tuần tự hóa DTO ở backend (A-08).
- Giấy phép kiểm theo ngày ở mỗi lần thao tác, không có job bật, tắt quyền (A-09).
- Khung ngoại tuyến dùng chung: kiểm quyền theo thời điểm thiết bị, bản ghi chờ xem lại (A-11).

Plan tuân theo FR-010a của 000 (Q-306, Q-307): 002 không tự sinh bản ghi nghiệp vụ. Ba job của 002 chỉ chuyển trạng thái theo thời gian (Không hoạt động, khóa sau kết thúc lưu trú) và gửi cảnh báo (giấy phép sắp hết hạn). Khóa tài khoản khi Nghỉ việc hay hết quan hệ là tác động kèm lệnh của 008, 012, chạy trong cùng transaction.

## Technical Context

**Language/Version**: Java 21 LTS; TypeScript 5.x; Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**:
- Backend: như 000 (Spring Boot 3.5.x, Spring Modulith 1.4.x, Flyway 11.x, springdoc-openapi 2.8.x, ShedLock), cùng Spring Security 6.5 (filter xác thực token phiên riêng, `Argon2PasswordEncoder`, cần thư viện Bouncy Castle).
- Web: như 000.
- App: như 000, cùng `flutter_secure_storage` để lưu token.

**Storage**: MySQL 8.4 LTS (000 R-01). Bảng ở [data-model.md](data-model.md). Tham chiếu tới hồ sơ nhân viên, người thân, phân công là định danh logic, không khóa ngoại chéo module (constitution X).

**Testing**: như 000 R-14. Thêm `MaTranQuyenKhopNguonTest` đọc Phụ lục 27 từ `docs/nghiep-vu.md` để đối chiếu ma trận đã nạp. Bản giả cho 008, 003, 012, 001, 004, 007 theo chữ ký ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 2.

**Target Platform**: như 000. Đăng nhập ở cả ba kênh (Q-305); quản trị tài khoản và phân quyền ở web; mở khóa sớm và duyệt bản ghi chờ xem lại có ở web và app nhân viên.

**Project Type**: Web service (REST `/api/v1`), một web client và hai mobile client.

**Performance Goals**:
- Kiểm tra quyền nằm trong ngân sách 2 giây của NFR-02 (spec CHK037). Mỗi yêu cầu: một lần tra phiên theo khóa chính; quyền vai trò lấy từ bộ đệm theo phiên bản (A-05); phạm vi tính bằng vài truy vấn trên chỉ mục, tập id tối đa vài trăm (NFR-01).
- Tự đăng xuất (FR-013a) không cần job: phiên quá CFG-M15-10 kể từ `hoat_dong_cuoi_luc` bị coi là kết thúc ở lần tra phiên kế tiếp (A-02).
- Ba job của 002 xử lý toàn bộ 600 tài khoản người thân và giấy phép trong vài giây, trong NFR-04.

**Constraints**:
- Mọi lệnh ghi nhật ký trong cùng transaction (000 FR-047); mật khẩu và token không vào nhật ký.
- Không có API sửa ma trận quyền; chỉ thu hẹp và một quyền gán thêm (FR-025, FR-024).
- Không đệm phạm vi và trạng thái phiên giữa các yêu cầu (FR-013, FR-027, FR-033).
- Không tự sinh bản ghi nghiệp vụ (FR-010a của 000).
- Độ mạnh mật khẩu (CFG-M15-09) và thời gian tự đăng xuất theo kênh (CFG-M15-10) đọc từ `ThamSoApi` (Q-309, A-13).

**Scale/Scope**: 9 vai trò; khoảng 70 dòng chức năng của Phụ lục 27; 100 tài khoản nhân viên, 600 tài khoản người thân, 60 người dùng đồng thời (NFR-01).

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mọi quyết định dẫn FR, BR; data-model mục 7 là bảng truy vết BR-M15-01 → 20, BR-M06-05, BR-M09-04 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Trạng thái tài khoản chỉ đổi qua lệnh có endpoint riêng (data-model mục 2); tài khoản không có lệnh sửa trực tiếp (CHK024), không xóa |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển trạng thái tài khoản và bản ghi chờ xem lại ở data-model khớp spec; mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M15-01, 02, 07 → 10, CFG-M06-02, CFG-M01-04 đọc từ `ThamSoApi` (CFG-M15-09, 10 chốt ở Q-309) |
| VI. Vai trò, quyền | Đạt | Ma trận nạp từ Phụ lục 27, 19.3 là căn cứ; test đối chiếu tự động; không thêm vai trò; quyền theo nhiệm vụ chỉ sáu ngoại lệ FR-019a → f |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ; điểm thiếu ở nguồn được báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-012 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ |
| X. Kiến trúc | Đạt | Một khối; gói `m15.taikhoan`; module khác chỉ dùng giao diện ở contracts; REST `/api/v1`; MySQL; ba client. NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Ba lớp quyền, phạm vi, giới hạn trường, điều kiện pháp lý đều ở backend; `lenhDuocPhep` và `/toi/quyen` chỉ để hiển thị; mỗi lệnh trạng thái tài khoản có endpoint `POST …/{id}/<lenh>` |
| XII. Dữ liệu | Đạt | `lich_su_trang_thai_tai_khoan` chỉ thêm; tài khoản không xóa (trigger theo mẫu 000); nhật ký trong transaction; thời gian theo `Clock` của 000 |
| XIII. Tham số | Đạt | Như V |
| XIV. Ngoại tuyến, thông báo | Đạt | Khung ngoại tuyến A-11 theo Q-01, CFG-M15-08, BR-M15-20; lệnh của 002 chỉ trực tuyến; thông báo qua sự kiện tới 009 |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR; BR-M15-20 có test ở app nhân viên |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Hai chỗ đổi chữ ký so với khai báo trước của 000 (`QuyenApi.coQuyen`, `QuyenXemTruongApi.duocXem` nhận thêm đối tượng) là mở rộng, có ánh xạ ngược, ghi ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.2, 1.4.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 002 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 700 tài khoản, 60 phiên đồng thời; phạm vi là tập id nhỏ (A-07) |
| NFR-02 Hiệu năng | Có | Một lần tra phiên khóa chính; quyền vai trò đệm theo phiên bản; phạm vi tính bằng truy vấn chỉ mục (A-02, A-05, A-07) |
| NFR-03 Thông báo khẩn cấp | Gián tiếp | Kích hoạt khẩn cấp miễn lớp phạm vi (FR-044a); kiểm quyền không chặn đường khẩn cấp; dữ liệu nguồn lỗi vẫn cho ghi sự cố (FR-041b) |
| NFR-04 Tác vụ định kỳ | Có | Ba job, ShedLock, chạy lại không chuyển trạng thái hai lần (điều kiện trên trạng thái hiện hành), không gửi cảnh báo lặp (`da_canh_bao_giay_phep`) |
| NFR-05 Khả dụng | Có | Job chạy lúc 01:00 và 06:00, ngoài khung giao ca |
| NFR-06 Sao lưu | Gián tiếp | Theo vận hành của 000; khôi phục không làm sống lại phiên (phiên khôi phục bị kết thúc khi khởi động, ghi ở task) |
| NFR-07 Lưu trữ | Có | Tài khoản không xóa (FR-005); nhật ký đăng nhập theo CFG-M15-04 qua 000 |
| NFR-08 Bảo mật | Có | HTTPS; Argon2id; token chỉ lưu băm; cookie `HttpOnly`, `SameSite=Strict`; thông báo đăng nhập không lộ tồn tại; giới hạn trường ở backend; nhật ký lượt xem sức khỏe của người thân thuộc 012 |
| NFR-09 Thời gian | Có | Giấy phép hết hạn hết ngày theo Asia/Ho_Chi_Minh; khoảng CFG-M15-07; thời điểm thiết bị chỉ dùng cho ngoại tuyến (A-11) |
| NFR-10 Thiết bị | Có | Một tài khoản cho mọi kênh (Q-305); một thiết bị một phiên cho thiết bị dùng chung tại tầng (FR-014a) |
| NFR-11 Ngôn ngữ | Có | Thông báo từ chối tiếng Việt nêu lớp không thỏa (FR-043, SC-008) |
| NFR-12 Cấu hình | Có | CFG đọc lúc chạy; thu hẹp quyền có hiệu lực ngay, không triển khai lại |
| NFR-13 Kiểm thử | Có | Khóa tạm, Không hoạt động, khóa sau kết thúc lưu trú, giấy phép hết hạn, khoảng ca, ngoại tuyến đều kiểm bằng đồng hồ giả lập |

## Project Structure

### Documentation (this feature)

```text
specs/002-account-permission/
├── plan.md                      # File này
├── research.md                  # Phase 0: A-01 → A-14 (dùng lại R-01 → R-05 của 000)
├── data-model.md                # Phase 1: bảng, bảng chuyển trạng thái, truy vết BR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-002.yaml             # OpenAPI 3.1: xác thực, tài khoản, phân quyền, nghiệp vụ chuyên môn, ngoại tuyến
│   └── module-interfaces.md     # Giao diện Java: 002 cung cấp / 002 cần
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/vn/eldercare/m15/taikhoan/
├── api/          # NguoiDungHienTai, QuyenApi, PhamViApi, QuyenXemTruongApi, NguoiDuyetApi, TaiKhoanApi,
│                 # NgoaiTuyenHandler, DieuKienChuThich, GanPhamViProvider, @CanQuyen, @NhomTruong, sự kiện
├── xacthuc/      {internal, web}          # đăng nhập, phiên, mật khẩu, thiết bị
├── quyen/        {internal, web}          # vai trò, ma trận, thu hẹp, gán thêm, nhiệm vụ, giới hạn trường
├── phamvi/       {internal}               # phạm vi, ngoại lệ sự cố, phạm vi báo cáo lịch sử
├── phaply/       {internal, web, lich}    # nghiệp vụ chuyên môn, giấy phép, job cảnh báo
├── ngoaituyen/   {internal, web}          # đồng bộ, chờ xem lại
└── lich/                                  # job Không hoạt động, khóa sau kết thúc lưu trú

web/src/tinh-nang/
├── dang-nhap/            # đăng nhập, đổi mật khẩu
├── tai-khoan/            # quản trị tài khoản nhân viên (QLV), người thân (HC, QLV)
├── phan-quyen/           # vai trò, thu hẹp, gán thêm, tra quyền tại thời điểm
└── cho-xem-lai/

app-nhan-vien/lib/
├── chung/xac-thuc/       # token trong flutter_secure_storage, mã thiết bị
└── tinh-nang/{dang-nhap, mo-khoa-som, ket-qua-dong-bo, cho-xem-lai}/

app-nguoi-than/lib/
├── chung/xac-thuc/
└── tinh-nang/dang-nhap/
```

**Structure Decision**: Theo R-02 của 000. Feature 002 nằm ở `backend/…/m15/taikhoan` với sáu gói con. Bộ lọc xác thực và interceptor `@CanQuyen` là bean của 002, áp cho mọi endpoint của mọi module. Ở web có màn đăng nhập, quản trị tài khoản, phân quyền và chờ xem lại. App nhân viên có đăng nhập, mở khóa sớm, kết quả đồng bộ, chờ xem lại. App người thân có đăng nhập và đổi mật khẩu.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Token phiên opaque thay vì JWT như dự kiến ở 000 R-12 | FR-013, FR-027 đòi thu hồi phiên và đổi quyền có hiệu lực ở yêu cầu kế tiếp | JWT cần danh sách thu hồi, vẫn phải tra máy chủ mỗi yêu cầu nhưng phức tạp hơn |
| Bảng kỹ thuật `phien_dang_nhap`, `thiet_bi`, `loi_du_lieu_quyen`, `da_canh_bao_giay_phep` được `UPDATE`, không ghi nhật ký nghiệp vụ riêng | Là dữ liệu hạ tầng; thay đổi nghiệp vụ tương ứng (đăng nhập, khóa, báo lỗi dữ liệu) đã có nhật ký | Ghi nhật ký cho mọi lần cập nhật `hoat_dong_cuoi_luc` làm nhật ký phình to |
| Bảng `ban_ghi_cho_xem_lai` chung ở 002 thay vì mỗi module một bảng | FR-041c là quy tắc chung của mọi bản ghi ngoại tuyến; 000 R-13 giao cho plan 002 chi tiết | Mỗi module một bảng: lặp quy tắc chọn người quyết định và vòng đời ở nhiều module |

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới, rồi sửa `docs/nghiep-vu.md` và spec 002.

1. *(Đã xử lý 2026-10-07: Q-309, CFG-M15-09, CFG-M15-10, BR-M15-21; spec FR-006a, FR-013a.)* **Độ mạnh mật khẩu và thời gian tự đăng xuất chưa có mã CFG.** Spec ghi "cần mã CFG khi thiết kế" (Assumptions; điểm báo lại 13, đã xử lý 2026-10-01 nhưng để lại hai mã này). Plan tạm đặt ở cấu hình ứng dụng với giá trị đề nghị: tối thiểu 8 ký tự có chữ và số; tự đăng xuất sau 30 phút ở web, 12 giờ ở app nhân viên, 30 ngày ở app người thân (research A-13). Đề nghị: mở Q và thêm hai mã CFG-M15-09, CFG-M15-10 vào Phụ lục 25.
2. *(Đã xử lý 2026-10-07: Q-310, BR-M15-22; spec FR-004b.)* **Tài khoản Quản lý viện đầu tiên.** FR-004 chỉ cho Quản lý viện tạo tài khoản nhân viên, FR-012 đòi luôn có một Quản lý viện; nguồn không nói ai tạo tài khoản Quản lý viện đầu tiên khi hệ thống mới cài. Plan dùng một công cụ khởi tạo chạy một lần lúc cài đặt (tạo hồ sơ nhân viên qua API của 008 và tài khoản AC-01, buộc đổi mật khẩu, ghi nhật ký người thực hiện là "khởi tạo hệ thống"). Đề nghị ghi việc này vào 19.1 bằng một Q, để không coi là ngoại lệ của FR-004.
3. **(Đồng bộ 000, đã sửa trong plan này)** Hợp đồng `YeuCauPheDuyetApi.lap` của 000 ghi "dùng khi module tự lập yêu cầu (người lập là 'Hệ thống')", trái Q-307. Đã sửa câu chú thích ở `specs/000-shared-foundation/contracts/module-interfaces.md`: yêu cầu luôn do actor lập; `lap` dùng khi một lệnh của actor ở module khác lập kèm yêu cầu. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
