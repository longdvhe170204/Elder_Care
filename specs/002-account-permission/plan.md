# Implementation Plan: Tài khoản và phân quyền

**Branch**: `002-account-permission` | **Date**: 2026-10-07, cập nhật 2026-10-18 (Q-365 → Q-371) | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-account-permission/spec.md`

## Summary

Feature 002 cung cấp cho mọi module:
1. Tài khoản nhân viên và người thân, trạng thái, đăng nhập, phiên (mục A, B; UC-68, UC-70).
2. Vai trò, ma trận quyền tối đa theo Phụ lục 27 (19.3 là căn cứ), thu hẹp, quyền gán thêm, quyền theo nhiệm vụ (mục C).
3. Phạm vi dữ liệu suy ra từ phân công và phân bổ giường (mục D).
4. Điều kiện pháp lý cho nghiệp vụ chuyên môn (mục E).
5. Một điểm kiểm tra quyền hiệu lực ba lớp BR-M15-01, gồm ngoại lệ sự cố, ngoại tuyến, lỗi dữ liệu nguồn (mục F).
6. Quyền hiệu lực của người thân (mục G).
7. *(Bổ sung 2026-10-18)* Tài khoản người cao tuổi: tạo theo lời chính người cao tuổi, mã QR đăng nhập, thiết bị gắn, token thiết bị cho nút SOS, khóa khi kết thúc lưu trú hoặc theo yêu cầu phê duyệt (mục H; Q-365, Q-368, Q-370, Q-371).

Hướng kỹ thuật:
- Nằm ở `m15.taikhoan`, giữ đúng các giao diện mà 000 đã khai báo trước (research A-01).
- Token phiên opaque lưu phía máy chủ, để kết thúc phiên và áp quyền mới ngay ở yêu cầu kế tiếp (A-02).
- Ma trận quyền nạp từ Phụ lục 27 và 19.3, có test đối chiếu tự động với tài liệu nguồn (A-04).
- Thu hẹp và gán vai trò lưu theo thời điểm hiệu lực, nên tra được quyền quá khứ (A-05).
- Phạm vi tính trong từng yêu cầu, không đệm giữa các yêu cầu (A-07).
- Giới hạn trường áp khi tuần tự hóa DTO ở backend (A-08).
- Giấy phép kiểm theo ngày ở mỗi lần thao tác, không có job bật, tắt quyền (A-09).
- Khung ngoại tuyến dùng chung: kiểm quyền theo thời điểm thiết bị, bản ghi chờ xem lại (A-11).
- *(Bổ sung 2026-10-18)* Người cao tuổi đăng nhập bằng mã QR dùng một lần; phiên kênh `APP_NGUOI_CAO_TUOI` không hết theo thời gian; một token thiết bị riêng, chỉ lưu băm, chỉ dùng cho lệnh SOS của 007 (A-15).
- *(Bổ sung 2026-10-18)* Khóa tài khoản người cao tuổi khi nhận sự kiện `NguoiCaoTuoiDaChuyenTrangThai` sang trạng thái cuối; đề nghị khóa của người thân là loại yêu cầu `M15.KHOA_TAI_KHOAN_NCT` của vòng đời phê duyệt chung (A-16).

Plan tuân theo FR-010a của 000 (Q-306, Q-307): 002 không tự sinh bản ghi nghiệp vụ. Ba job của 002 chỉ chuyển trạng thái theo thời gian (Không hoạt động, khóa sau kết thúc lưu trú) và gửi cảnh báo (giấy phép sắp hết hạn). Khóa tài khoản khi Nghỉ việc hay hết quan hệ là tác động kèm lệnh của 008, 012, chạy trong cùng transaction. Khóa tài khoản người cao tuổi khi kết thúc lưu trú là tác động khi nhận sự kiện chuyển trạng thái của 001, không phải job (A-16).

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**:
- Backend: như 000 (Spring Boot 4.1.x, Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock), cùng Spring Security 7 (filter xác thực token phiên riêng, `Argon2PasswordEncoder`, cần thư viện Bouncy Castle).
- Web: như 000.
- App: như 000, cùng `flutter_secure_storage` để lưu token; *(bổ sung 2026-10-18)* `mobile_scanner` để quét mã QR đăng nhập và `local_auth` cho mở khóa bằng vân tay, khuôn mặt của người cao tuổi (A-15).

**Storage**: MySQL 8.0 (000 R-01). Bảng ở [data-model.md](data-model.md). Tham chiếu tới hồ sơ nhân viên, người thân, phân công là định danh logic, không khóa ngoại chéo module (constitution X).

**Testing**: như 000 R-14. Thêm `MaTranQuyenKhopNguonTest` đọc Phụ lục 27 từ `docs/business/nghiep-vu.md` để đối chiếu ma trận đã nạp. Bản giả cho 008, 003, 012, 001, 004, 007 theo chữ ký ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 2.

**Target Platform**: như 000. Đăng nhập ở cả ba kênh (Q-305); quản trị tài khoản và phân quyền ở web; mở khóa sớm và duyệt bản ghi chờ xem lại có ở web và app nhân viên.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Kiểm tra quyền nằm trong ngân sách 2 giây của NFR-02 (spec CHK037). Mỗi yêu cầu: một lần tra phiên theo khóa chính; quyền vai trò lấy từ bộ đệm theo phiên bản (A-05); phạm vi tính bằng vài truy vấn trên chỉ mục, tập id tối đa vài trăm (NFR-01).
- Tự đăng xuất (FR-013a) không cần job: phiên quá CFG-M15-10 kể từ `hoat_dong_cuoi_luc` bị coi là kết thúc ở lần tra phiên kế tiếp (A-02).
- *(Bổ sung 2026-10-18)* Nút SOS: một lần tra `thiet_bi_gan` theo khóa băm token thiết bị, không qua phiên (A-15).
- Ba job của 002 xử lý toàn bộ 600 tài khoản người thân và giấy phép trong vài giây, trong NFR-04.

**Constraints**:
- Mọi lệnh ghi nhật ký trong cùng transaction (000 FR-047); mật khẩu và token không vào nhật ký.
- Không có API sửa ma trận quyền; chỉ thu hẹp và một quyền gán thêm (FR-025, FR-024).
- Không đệm phạm vi và trạng thái phiên giữa các yêu cầu (FR-013, FR-027, FR-033).
- Không tự sinh bản ghi nghiệp vụ (FR-010a của 000).
- Độ mạnh mật khẩu (CFG-M15-09) và thời gian tự đăng xuất theo kênh (CFG-M15-10) đọc từ `ThamSoApi` (Q-309, A-13).

**Scale/Scope**: 10 vai trò (thêm AC-16, Q-365); khoảng 72 dòng chức năng của Phụ lục 27; 100 tài khoản nhân viên, 600 tài khoản người thân, 300 tài khoản người cao tuổi, 60 người dùng đồng thời (NFR-01).

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
| VI. Vai trò, quyền | Đạt | Ma trận nạp từ Phụ lục 27, 19.3 là căn cứ; test đối chiếu tự động; không tự thêm vai trò (AC-16 có ở 26.1, Q-365); quyền theo nhiệm vụ chỉ sáu ngoại lệ FR-019a → f |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ; điểm thiếu ở nguồn được báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-013 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ |
| X. Kiến trúc | Đạt | Một khối; gói `m15.taikhoan`; module khác chỉ dùng giao diện ở contracts; REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Ba lớp quyền, phạm vi, giới hạn trường, điều kiện pháp lý đều ở backend; `lenhDuocPhep` và `/toi/quyen` chỉ để hiển thị; mỗi lệnh trạng thái tài khoản có endpoint `POST …/{id}/<lenh>` |
| XII. Dữ liệu | Đạt | `lich_su_trang_thai_tai_khoan` chỉ thêm; tài khoản không xóa (trigger theo mẫu 000); nhật ký trong transaction; thời gian theo `Clock` của 000 |
| XIII. Tham số | Đạt | Như V |
| XIV. Ngoại tuyến, thông báo | Đạt | Khung ngoại tuyến A-11 theo Q-01, CFG-M15-08, BR-M15-20; lệnh của 002 chỉ trực tuyến; thông báo qua sự kiện tới 009 |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR; BR-M15-20 có test ở app nhân viên |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Hai chỗ đổi chữ ký so với khai báo trước của 000 (`QuyenApi.coQuyen`, `QuyenXemTruongApi.duocXem` nhận thêm đối tượng) là mở rộng, có ánh xạ ngược, ghi ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.2, 1.4.

**Kiểm lại 2026-10-18 (Q-365 → Q-371)**: Đạt. Thêm loại tài khoản, vai trò AC-16 theo 26.1, hai bảng `thiet_bi_gan`, `ma_qr_dang_nhap`, một loại yêu cầu phê duyệt; không thêm trạng thái tài khoản. Token thiết bị là ngoại lệ có chủ đích của "mọi yêu cầu qua phiên", chỉ mở một lệnh (SOS), ghi ở Complexity Tracking.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 002 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 1.000 tài khoản (thêm 300 tài khoản người cao tuổi), 60 phiên đồng thời; phạm vi là tập id nhỏ (A-07) |
| NFR-02 Hiệu năng | Có | Một lần tra phiên khóa chính; quyền vai trò đệm theo phiên bản; phạm vi tính bằng truy vấn chỉ mục (A-02, A-05, A-07) |
| NFR-03 Thông báo khẩn cấp | Gián tiếp | Kích hoạt khẩn cấp miễn lớp phạm vi (FR-044a); kiểm quyền không chặn đường khẩn cấp; dữ liệu nguồn lỗi vẫn cho ghi sự cố (FR-041b) |
| NFR-04 Tác vụ định kỳ | Có | Ba job, ShedLock, chạy lại không chuyển trạng thái hai lần (điều kiện trên trạng thái hiện hành), không gửi cảnh báo lặp (`da_canh_bao_giay_phep`) |
| NFR-05 Khả dụng | Có | Job chạy lúc 01:00 và 06:00, ngoài khung giao ca |
| NFR-06 Sao lưu | Gián tiếp | Theo vận hành của 000; khôi phục không làm sống lại phiên (phiên khôi phục bị kết thúc khi khởi động, ghi ở task) |
| NFR-07 Lưu trữ | Có | Tài khoản không xóa (FR-005); nhật ký đăng nhập theo CFG-M15-04 qua 000 |
| NFR-08 Bảo mật | Có | HTTPS; Argon2id; token chỉ lưu băm; cookie `HttpOnly`, `SameSite=Strict`; thông báo đăng nhập không lộ tồn tại; giới hạn trường ở backend; nhật ký lượt xem sức khỏe của người thân thuộc 012; token thiết bị và mã QR của người cao tuổi chỉ lưu băm, token thiết bị chỉ dùng cho nút SOS (A-15) |
| NFR-09 Thời gian | Có | Giấy phép hết hạn hết ngày theo Asia/Ho_Chi_Minh; khoảng CFG-M15-07; thời điểm thiết bị chỉ dùng cho ngoại tuyến (A-11) |
| NFR-10 Thiết bị | Có | Một tài khoản cho mọi kênh (Q-305); một thiết bị một phiên cho thiết bị dùng chung tại tầng (FR-014a); điện thoại riêng của người cao tuổi gắn tối đa một tài khoản, không đăng nhập được tài khoản khác (FR-070, FR-014a) |
| NFR-11 Ngôn ngữ | Có | Thông báo từ chối tiếng Việt nêu lớp không thỏa (FR-043, SC-008) |
| NFR-12 Cấu hình | Có | CFG đọc lúc chạy; thu hẹp quyền có hiệu lực ngay, không triển khai lại |
| NFR-13 Kiểm thử | Có | Khóa tạm, Không hoạt động, khóa sau kết thúc lưu trú, giấy phép hết hạn, khoảng ca, ngoại tuyến đều kiểm bằng đồng hồ giả lập |

## Project Structure

### Documentation (this feature)

```text
specs/002-account-permission/
├── plan.md                      # File này
├── research.md                  # Phase 0: A-01 → A-16 (dùng lại R-01 → R-05 của 000)
├── data-model.md                # Phase 1: bảng, bảng chuyển trạng thái, truy vết BR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-002.yaml             # OpenAPI 3.1: xác thực, tài khoản, phân quyền, nghiệp vụ chuyên môn, ngoại tuyến
│   └── module-interfaces.md     # Giao diện Java: 002 cung cấp / 002 cần
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m15/taikhoan/
├── api/          # NguoiDungHienTai, QuyenApi, PhamViApi, QuyenXemTruongApi, NguoiDuyetApi, TaiKhoanApi,
│                 # NgoaiTuyenHandler, DieuKienChuThich, GanPhamViProvider, @CanQuyen, @NhomTruong, sự kiện
├── xacthuc/      {internal, web}          # đăng nhập, phiên, mật khẩu, thiết bị
├── quyen/        {internal, web}          # vai trò, ma trận, thu hẹp, gán thêm, nhiệm vụ, giới hạn trường
├── phamvi/       {internal}               # phạm vi, ngoại lệ sự cố, phạm vi báo cáo lịch sử
├── phaply/       {internal, web, lich}    # nghiệp vụ chuyên môn, giấy phép, job cảnh báo
├── ngoaituyen/   {internal, web}          # đồng bộ, chờ xem lại
└── lich/                                  # job Không hoạt động, khóa sau kết thúc lưu trú

frontend/src/tinh-nang/
├── dang-nhap/            # đăng nhập, đổi mật khẩu
├── tai-khoan/            # quản trị tài khoản nhân viên (QLV), người thân (HC, QLV), người cao tuổi (HC, QLV; mã QR, gỡ thiết bị, đề nghị khóa)
├── phan-quyen/           # vai trò, thu hẹp, gán thêm, tra quyền tại thời điểm
└── cho-xem-lai/

mobile/lib/
├── chung/xac-thuc/       # token trong flutter_secure_storage, mã thiết bị
└── tinh-nang/{dang-nhap, mo-khoa-som, ket-qua-dong-bo, cho-xem-lai}/

mobile/lib/
├── chung/xac-thuc/
└── tinh-nang/dang-nhap/

mobile/lib/                # bổ sung 2026-10-18 (Q-365, Q-370)
├── nhan-vien/ma-qr-nguoi-cao-tuoi/   # Điều dưỡng tạo mã QR
└── nguoi-cao-tuoi/dang-nhap-qr/      # quét mã, token phiên và token thiết bị, mở khóa sinh trắc học
```

**Structure Decision**: Theo R-02 của 000. Feature 002 nằm ở `backend/…/m15/taikhoan` với sáu gói con. Bộ lọc xác thực và interceptor `@CanQuyen` là bean của 002, áp cho mọi endpoint của mọi module. Ở web có màn đăng nhập, quản trị tài khoản, phân quyền và chờ xem lại. App nhân viên có đăng nhập, mở khóa sớm, kết quả đồng bộ, chờ xem lại. App người thân có đăng nhập và đổi mật khẩu. *(Bổ sung 2026-10-18)* Phần người cao tuổi của app chung có đăng nhập bằng mã QR; màn kênh người cao tuổi và nút SOS do 012 dựng, lệnh SOS thuộc 007.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Token phiên opaque thay vì JWT như dự kiến ở 000 R-12 | FR-013, FR-027 đòi thu hồi phiên và đổi quyền có hiệu lực ở yêu cầu kế tiếp | JWT cần danh sách thu hồi, vẫn phải tra máy chủ mỗi yêu cầu nhưng phức tạp hơn |
| Bảng kỹ thuật `phien_dang_nhap`, `thiet_bi`, `loi_du_lieu_quyen`, `da_canh_bao_giay_phep` được `UPDATE`, không ghi nhật ký nghiệp vụ riêng | Là dữ liệu hạ tầng; thay đổi nghiệp vụ tương ứng (đăng nhập, khóa, báo lỗi dữ liệu) đã có nhật ký | Ghi nhật ký cho mọi lần cập nhật `hoat_dong_cuoi_luc` làm nhật ký phình to |
| Bảng `ban_ghi_cho_xem_lai` chung ở 002 thay vì mỗi module một bảng | FR-041c là quy tắc chung của mọi bản ghi ngoại tuyến; 000 R-13 giao cho plan 002 chi tiết | Mỗi module một bảng: lặp quy tắc chọn người quyết định và vòng đời ở nhiều module |
| *(Bổ sung 2026-10-18)* Token thiết bị cho nút SOS, ngoài phiên đăng nhập | FR-071, Q-370: nút SOS phải gửi được khi phiên đã hết hoặc chưa đăng nhập | Bắt phiên: trái Q-370. Ký yêu cầu bằng cặp khóa trên máy: phải quản lý khóa trên thiết bị, phức tạp hơn mà không thêm bảo vệ cho một lệnh chỉ tạo cảnh báo. Chỉ dùng mã `X-Thiet-Bi`: dễ giả |

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới, rồi sửa `docs/business/nghiep-vu.md` và spec 002.

1. *(Đã xử lý 2026-10-07: Q-309, CFG-M15-09, CFG-M15-10, BR-M15-21; spec FR-006a, FR-013a.)* **Độ mạnh mật khẩu và thời gian tự đăng xuất chưa có mã CFG.** Spec ghi "cần mã CFG khi thiết kế" (Assumptions; điểm báo lại 13, đã xử lý 2026-10-01 nhưng để lại hai mã này). Plan tạm đặt ở cấu hình ứng dụng với giá trị đề nghị: tối thiểu 8 ký tự có chữ và số; tự đăng xuất sau 30 phút ở web, 12 giờ ở app nhân viên, 30 ngày ở app người thân (research A-13). Đề nghị: mở Q và thêm hai mã CFG-M15-09, CFG-M15-10 vào Phụ lục 25.
2. *(Đã xử lý 2026-10-07: Q-310, BR-M15-22; spec FR-004b.)* **Tài khoản Quản lý viện đầu tiên.** FR-004 chỉ cho Quản lý viện tạo tài khoản nhân viên, FR-012 đòi luôn có một Quản lý viện; nguồn không nói ai tạo tài khoản Quản lý viện đầu tiên khi hệ thống mới cài. Plan dùng một công cụ khởi tạo chạy một lần lúc cài đặt (tạo hồ sơ nhân viên qua API của 008 và tài khoản AC-01, buộc đổi mật khẩu, ghi nhật ký người thực hiện là "khởi tạo hệ thống"). Đề nghị ghi việc này vào 19.1 bằng một Q, để không coi là ngoại lệ của FR-004.
3. **(Đồng bộ 000, đã sửa trong plan này)** Hợp đồng `YeuCauPheDuyetApi.lap` của 000 ghi "dùng khi module tự lập yêu cầu (người lập là 'Hệ thống')", trái Q-307. Đã sửa câu chú thích ở `specs/000-shared-foundation/contracts/module-interfaces.md`: yêu cầu luôn do actor lập; `lap` dùng khi một lệnh của actor ở module khác lập kèm yêu cầu. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
4. *(2026-10-18)* **Thư mục app cho người cao tuổi.** CLAUDE.md ghi `mobile/lib/{common, staff, family}`; plan thêm `mobile/lib/nguoi-cao-tuoi/` (tên logic, mã là `resident` theo glossary). Không phải điểm nghiệp vụ; đề nghị chủ dự án cập nhật mục "Công nghệ và cấu trúc mã" của CLAUDE.md.

## Ghi chú triển khai

Ghi khi triển khai (constitution XIV: lệch kỹ thuật ghi ở plan cùng lúc với mã). Tên tiếng Anh theo glossary mục 7.5.

### Gói `api` công bố trước (đợt 0, 2026-10-19)

- **Spring Modulith coi gói con của module là nội bộ.** Trước thay đổi này, module khác dùng `m15.account.api` sẽ trượt `ModuleBoundaryTest` ("depends on non-exposed type"; đã thử bằng một lớp trong `m01`). Mỗi gói `…api` của M15 (`account`, `approval`, `auditlog`, `correction`, `parameter`, `retention`) và `account.api.events` có `package-info.java` gắn `@NamedInterface`. `ApiNamedInterfaceArchTest` kiểm luật này cho mọi module `m01` → `m15`, nên chủ module khác thêm `package-info.java` như vậy khi công bố `api` của mình.
- **T001**: `spring-boot-starter-security` (Spring Security 7) đã có trong `pom.xml` từ 000; Bouncy Castle (cho `Argon2PasswordEncoder`) thêm cùng T016, chỗ dùng đầu tiên (T011 chỉ cần SHA-256 của JDK). Phần mobile của T001 chờ mở mobile.
- **T003**: bản giả ở `BT/support/fake/m15/` (docs/team-plan.md mục 3) thay cho `BT/support/gia002/`; `FakeM15Config` đăng ký bean `@Primary`. Chỉ viết bản giả cho interface 002 khai báo (8 SPI). `NguoiCaoTuoiApi` (001) do B khai báo ở `m01.api` nên bản giả do B viết theo luật bản giả. *(Sửa 2026-10-20)* `GiayPhepCoSoApi` nằm ở `m15.account.api` theo hợp đồng 007 (bảng SPI, M06 cài, tránh vòng M15 ↔ M06 như các SPI khác) nên A khai báo và viết bản giả; trước đây ghi nhầm là D. 002 dùng khi tới US cần. Việc chờ giữa các thành viên ghi ở docs/team-plan.md mục 8.
- **T008**: `CurrentUser` thêm `residentId()` cho tài khoản người cao tuổi (A-15; mục 1.1 của contracts chưa ghi vì viết trước 2026-10-18). `PermissionResult.basis` là `Optional` (rỗng khi không được phép). `DoiTuong.tuPhamVi` không cài: 000 không có kiểu `PhamVi`, mọi chỗ đã dùng `Target`.
- **T009**: thêm năm SPI do 008 cài (`StaffProfileApi`, `LicenseApi`, `PracticeScopeCatalogApi`, `AssignmentApi`, `DutyApi`; contracts mục 2) vì T003 cần bản giả của chúng. `DutyApi` đúng chữ ký 008 mục 1.2; kiểu trả về của bốn SPI còn lại contracts chưa ghi, đặt ở mức tối thiểu (glossary mục 7.5), E (008) xem lại khi dịch hợp đồng 008, đổi theo luật "Đổi api". `HoSoNhanVienApi.taoHoSoToiThieu` khai báo cùng T016 (`StaffProfileApi.createMinimal`). Chưa khai báo: `QuanHeNguoiThanApi.quyenDangBat` và kiểu `QuyenNguoiThan` (chưa có định nghĩa ở 012; E chốt khi dịch hợp đồng 012), `ThietBiGanApi` (mục 1.11, cùng US8), `NghiepVuChuyenMonApi.loaiYeuCau` (cùng US5).
- **T010**: sự kiện là record ở `m15.account.api.events`. Phần `HasNotificationRequests` (nội dung, nhóm người nhận) làm cùng US phát sự kiện; nhóm người nhận theo nhiệm vụ (Trưởng tầng, Người phụ trách ca) chờ 009 T004 thêm biến thể vào `RecipientGroup`.
- **T002**: khung gói `m15.account.{auth, permission, scope, legal, offline, scheduling}` có `package-info.java`; gói `api` cũ của 000 không có bản giả trong mã chính nên không phải gỡ; bản giả test là `FakePermissions` (mở rộng theo các hàm mới).
- **T004 → T006** (2026-10-20): migration `V002_01__account.sql`, `V002_02__session_device.sql`, `V002_03__role_permission.sql`; tên bảng, cột, giá trị enum tiếng Anh như bảng của 000 (glossary mục 7.6). Cột `chuc_nang` đặt `function_code` vì `function` là từ khóa dành riêng của MySQL 8 (lỗi 1064). Thêm `CHECK` nhất quán: `lock_cause` có đúng khi `LOCKED`, `locked_until` có đúng khi `TEMPORARILY_LOCKED`, `end_reason` có đúng khi phiên đã kết thúc. `role` nạp sẵn 9 vai trò; AC-16, loại `RESIDENT`, kênh `RESIDENT_APP` thêm ở V002_08 (T090) như tasks. `username` và liên kết hồ sơ không đổi sau khi tạo: kiểm ở backend (T031), quyền `UPDATE` cấp mức bảng như 000. `./mvnw verify` qua trên JDK 25 (61 đơn vị, 142 tích hợp); test ràng buộc của các bảng nằm ở test US2, US3 (`KhongXoaTaiKhoanIT`, `MotThietBiMotPhienIT`…).
- **T007, T017: luật quy đổi Phụ lục 27** (chủ dự án giao chốt theo nghiệp vụ, 2026-10-20). Cài ở `BT/m15/account/Appendix27Matrix.java`; `V002_04` sinh theo cùng luật và được đối chiếu từng ô (683 dòng cho 9 vai trò; cột NCT nạp ở V002_08):
  - X → `VIEW` toàn viện; P → `VIEW` theo phân công, trừ P¹¹, P²⁹ (dòng "Dashboard, báo cáo" của Bác sĩ, Hành chính, Kế toán) là toàn viện vì chính chú thích ghi "toàn viện" (19.3 nói rõ với Bác sĩ, Hành chính). Người thân luôn theo quan hệ (FR-045).
  - T, D, C kéo theo `VIEW` cùng phạm vi: lệnh luôn cần đọc đối tượng (spec FR-020 "Xem: đọc dữ liệu trong phạm vi"); ví dụ Hành chính T⁵ ở "Hồ sơ người cao tuổi". Phạm vi T, D, C theo vai trò: Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc theo phân công, còn lại toàn viện (Phụ lục 27, 19.3).
  - T → `CREATE`, `UPDATE`; thêm `CONFIRM` ở 16 dòng có ghi nhận nhóm 3 (`RECORDING_FUNCTIONS`: checklist, điểm danh bán trú, hoạt động/chuyến đi, kiểm tra chất lượng, chỉ số, sự cố, phát thuốc, đối chiếu thuốc, tiếp nhận thuốc cung ứng, chuẩn bị/lưu mẫu, nhận phiếu, phục vụ suất đặc biệt, đối chiếu đồ ăn gia đình, bàn giao ca, đồ gửi, số dư thu chi); thêm `PROFESSIONAL` ở "Đơn thuốc" và "Phát thuốc" (hai nghiệp vụ chuyên môn mặc định, data-model mục 4). Ô T⁴ của Điều dưỡng (chỉ nhập đơn ngoài) vẫn có `PROFESSIONAL` vì bảng loại thao tác của spec ghi rõ "T³, T⁴"; giới hạn "chỉ nhập đơn ngoài" do 006 kiểm và điều kiện pháp lý theo danh mục nghiệp vụ chuyên môn (US5).
  - D → `APPROVE`; dòng "Chốt kỳ, xuất kế toán" thêm `FINALIZE`. "—" và "—³⁰" không sinh dòng (³⁰ thuộc quy tắc xem trường).
  - Mã chú thích theo ký hiệu đứng trước; hai ký hiệu của một ô cho cùng thao tác thì phạm vi rộng hơn thắng, chú thích hợp lại. Chú thích ở tên dòng (ví dụ ³⁶) không gắn vào ô.
  - Dòng `SECTION_19_3`: đúng ba dòng "Yêu cầu đổi ca" (Quản lý viện `APPROVE`, `CREATE` toàn viện; Trưởng tầng `CREATE` theo phân công; Q-15, Q-177). "Điều dưỡng có mọi T của Nhân viên chăm sóc": Phụ lục 27 đã ghi đủ, không ô nào thiếu, nên không thêm dòng (data-model mục 3 đã ghi chú).
  - `field_visibility_rule` chỉ có dòng hạn chế, thiếu dòng là `FULL`: Hành chính, Kế toán `HEALTH`, `MEDICATION` `NONE`, `WISHES` `PRESENCE_ONLY` (⁵, ¹⁶, ³⁰, 19.3); Bếp `HEALTH`, `MEDICATION`, `WISHES` `NONE` (¹⁴, 19.3). Người thân xem sức khỏe theo chú thích ¹ ở lớp quyền.
  - Test T017 đặt tên `PermissionMatrixMatchesSourceIT` (tasks ghi `MaTranQuyenKhopNguonTest`) vì cần MySQL nên chạy bằng failsafe; thêm kiểm `Function` khớp đủ 73 dòng Phụ lục 27, cùng thứ tự. Chạy trước khi có `V002_04`: FR_016 xanh, SC_001, FR_018, FR_022 đỏ vì bảng rỗng; sau khi thêm: `./mvnw verify` qua trên JDK 25 (61 đơn vị, 146 tích hợp).
- **T011** (2026-10-20): `SessionService.resolve` tra `login_session` theo `token_hash` mỗi yêu cầu (JdbcTemplate, không đệm), so `last_active_at` với khóa kênh của CFG-M15-10 (`{"value","unit"}` hoặc `{"noLimit": true}`), quá hạn (lớn hơn hẳn) thì `TIMED_OUT`; còn hạn thì cập nhật `last_active_at` và `device` (`INSERT … ON DUPLICATE KEY UPDATE`, cú pháp có ở MySQL 8.0 và 8.4). `SessionTokenFilter` nằm trong `SecurityFilterChain` của `SecurityConfig` (tạo trong cấu hình, không đăng ký bean để Spring Boot không gắn hai lần); Bearer ưu tiên hơn cookie; `SessionAuthentication.getName()` là id tài khoản (khóa người gửi của `IdempotencyFilter`). Quyết định kỹ thuật chưa ghi ở research: không có token thì 401 `UNAUTHORIZED` (lỗi framework, R-03); có token mà phiên không còn (không có, đã kết thúc, vừa hết giờ) thì cùng 401 `SESSION_EXPIRED`; lệnh ghi qua cookie thiếu `X-Requested-From: web` thì 403 `FORBIDDEN`, kiểm trước khi tra phiên để yêu cầu giả mạo không đếm lại giờ. Tắt CSRF, form, basic, phiên servlet. Đặt cookie (`SessionTokens.cookie`: `HttpOnly`, `Secure`, `SameSite=Strict`, path `/api`, không Max-Age) do lệnh đăng nhập (US2) gọi. Test cũ dùng `@WithMockUser` vẫn chạy vì bộ lọc không đụng tới yêu cầu không có token. Bouncy Castle chưa thêm: T011 chỉ cần SHA-256 của JDK; thêm cùng chỗ dùng `Argon2PasswordEncoder` đầu tiên (T016). Test `SessionTokensTest` (3), `SessionTokenFilterIT` (10; trước khi cài: 10 đỏ với security mặc định). `./mvnw verify` qua trên JDK 25 (64 đơn vị, 156 tích hợp).
- **T012** (2026-10-20): `SessionService.resolve` đọc kèm `account` (loại, `staff_id`, `family_member_id`) và vai trò đang hiệu lực của `role_assignment` (`effective_from <= now < effective_to`) trong cùng lần tra phiên; `ActiveSession` cài `CurrentUser`. Bean `CurrentUserImpl` (singleton, `@Fallback`) mỗi lần gọi đọc ngữ cảnh hiện tại: job đang chạy thì Bộ lập lịch, có `SessionAuthentication` thì người dùng của phiên, không có thì `IllegalStateException`. `@Fallback` để bean `CurrentUser` giả mà test của 000 tự khai vẫn thắng, không sửa test cũ. "Đặt khi job chạy": `SchedulerUserAspect` bọc mọi `@Scheduled` và `ScheduledJob.runNow()` (cờ theo luồng, lồng được, trả lại cờ cũ khi xong), nên job chạy theo yêu cầu kiểm thử trong một yêu cầu có phiên vẫn là Bộ lập lịch. `SchedulerUser`: `accountId() = 0` (không có tài khoản; nhật ký kiểm `isScheduler()` trước), loại `STAFF`, không liên kết hồ sơ, vai trò `{SCHEDULER}`; `deviceId()`, `channel()` ném lỗi vì không có phiên. Trước T012 chạy thật không có bean `CurrentUser` nào nên job của 000 lỗi khi gọi người dùng hiện tại; T012 sửa luôn. Test `CurrentUserIT` (4; trước khi cài: 4 lỗi vì không có bean). `./mvnw verify` qua trên JDK 25 (64 đơn vị, 160 tích hợp).
- **T013** (2026-10-20, chủ dự án đồng ý cách gộp): `RolePermissionCalculator.at(accountId, t)` theo thứ tự A-05, mọi bảng lọc `effective_from <= t < effective_to`. Kết quả mỗi khóa là tập phương án `RoleGrant(scope, footnotes, source)`, không gộp thành một dòng: A-05 chỉ ghi "toàn viện thắng", không ghi cách gộp chú thích; ma trận có ô thật cần điều này (Trưởng tầng + Điều dưỡng: `BED_READINESS` `CREATE` theo phân công không chú thích và có ⁴⁰; `DAY_CARE_ATTENDANCE` `VIEW` toàn viện không chú thích và theo phân công ²⁷). `RoleGrants.add` bỏ phương án bị phủ (phạm vi phủ và chú thích là tập con), nên toàn viện thắng mà chú thích của vai trò này không gán cho vai trò kia; `PermissionApi` (T022) cho phép khi một phương án thỏa. `GET /me/permissions` có thể trả nhiều dòng cùng khóa khi phạm vi rộng hơn kèm điều kiện. `LOWER_SCOPE` hạ `FACILITY` xuống `ASSIGNED`, phạm vi khác giữ nguyên. Thu hẹp có `metric_group` hoặc `channel` (`DASHBOARD_REPORT`, 016 FR-010a) không trừ ở mức khóa; T075 và 016 xử lý. Quyền gán thêm có phạm vi `ASSIGNED` (phạm vi dữ liệu của Điều dưỡng, spec US7 #2). `current(accountId)` đệm trong `ConcurrentHashMap` theo `(account.permission_version, tổng role.permission_version của vai trò đang gán)`; số phiên bản đọc bằng một truy vấn riêng ngay trước khi tra đệm (A-05 ghi đọc cùng lần tra phiên; tách ra để `CurrentUser` không mang số phiên bản), lệnh đổi quyền phải tăng số phiên bản (T075). Test `RoleGrantsTest` (4), `RolePermissionCalculatorIT` (9); trước khi cài cả 13 lỗi với bản khung.
- **T014** (2026-10-20): `RequiresPermissionAspect` (`@Order(-1)`, trước `BusinessCommandAspect` để người không có quyền không nhận lỗi thiếu lý do) lấy đối tượng là tham số `Target` đầu tiên, không có thì `Target.UNSPECIFIED`. Ánh xạ FR-043: khóa bị từ chối và đối tượng cụ thể mà khóa `VIEW` cùng chức năng trượt lớp `SCOPE` thì 404 `NOT_FOUND`; `LEGAL` có giấy phép thì 403 `LEGAL_CONDITION_NOT_MET` kèm `failedLayer`, `license` (`name`, `expiryDate`); còn lại 403 `PERMISSION_DENIED` kèm `failedLayer`. Không có đối tượng cụ thể thì không trả 404. `PermissionDeniedException` (common) thêm constructor có `errorCode`, `properties`; `ProblemDetailsHandler` chép `properties` vào thân lỗi. `PermissionApi` tiêm bằng `ObjectProvider` vì bản thật cài ở T022. Test `RequiresPermissionAspectTest` (6, trước khi cài 6 đỏ), thêm `ProblemDetailsHandlerIT.FR_043_permissionDenied_carriesFailedLayerAndLicense` (đỏ trước khi sửa handler). `./mvnw verify` qua trên JDK 25 sau T013, T014 (74 đơn vị, 170 tích hợp).
- **T015** (2026-10-20, chủ dự án chọn cách làm): `RequiresPermissionArchTest`. Luật: mọi `@BusinessCommand` có `@RequiresPermission` hoặc có trong `SELF_CHECKED` kèm cách tự kiểm. Năm lệnh của 000 vào danh sách vì khóa quyền chỉ biết lúc chạy hoặc có quy tắc riêng: `ApprovalRequestService.editContent`, `reject`, `cancel` (khóa theo loại yêu cầu, người yêu cầu, người duyệt; 000 FR-034), `CorrectionService.create` (theo loại bản ghi gốc; 000 FR-026), `ParameterService.changeValue` (chỉ Quản lý viện; BR-M15-04); trước khi có danh sách test đỏ đúng năm lệnh này. Tasks ghi "mọi endpoint ghi": hiểu là lệnh mà endpoint gọi, kiểm ở tầng lệnh vì ArchUnit khó lần từ controller tới lệnh; `TestClockController` (profile test, demo) không là lệnh nghiệp vụ. Luật FR-035a: `@BusinessCommand` không gọi trực tiếp `ScopeApi.historyFilter` (chỉ lời gọi trong thân lệnh, không lần theo phương thức được gọi). Lệnh SOS của 007 (A-04) thêm vào `SELF_CHECKED` khi 007 viết. `./mvnw verify` qua trên JDK 25 (76 đơn vị, 170 tích hợp).
- **T016** (2026-10-20, chủ dự án đồng ý bốn điểm): `FirstManagerBootstrap.run(username, fullName)` trong một transaction: `SELECT … FOR UPDATE` các dòng `role_assignment` AC-01 còn hiệu lực (khóa cả khoảng chỉ mục khi chưa có dòng), có thì 409 `CONDITION_NOT_MET` (BR-M15-22); gọi `StaffProfileApi.createMinimal(fullName, NURSING_HOME_MANAGER)` (SPI mới cho 008, E xem lại khi dịch hợp đồng 008; bản giả trong `FakeStaffProfiles`); thêm `account` (`must_change_password = TRUE`, `created_by` NULL), `account_status_history` (ACTIVE, `actor_id` NULL), `role_assignment` AC-01 (`assigned_by` NULL); nhật ký `ACCOUNT_CREATED`, `ROLE_ASSIGNED` trên `M15.ACCOUNT`, căn cứ `BR-M15-22`. Nhật ký người thực hiện "khởi tạo hệ thống" qua hàm mới `AuditLogApi.writeAsBootstrap` (`actor_type = BOOTSTRAP` đã có ở V000_02; không đi qua `CurrentUser`). Mật khẩu tạm 12 ký tự ngẫu nhiên có chữ và số (CFG-M15-09), băm Argon2id (`PasswordConfig`, `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`); Bouncy Castle `bcprov-jdk18on` 1.86 thêm vào `pom.xml` (Spring Boot không quản lý phiên bản). Chạy bằng `FirstManagerBootstrapRunner` khi có `--eldercare.bootstrap-manager.run-once=true` (operations.md mục 2a), mật khẩu tạm in ra màn hình. `StaffProfileApi`, `PasswordEncoder` tiêm bằng `ObjectProvider`/bean riêng để context không có 008 vẫn chạy. Test `FirstManagerBootstrapIT` (4, chạy trong transaction rồi rollback, đóng tạm các dòng AC-01 của test khác); mã viết trước khi chạy đỏ, nên kiểm ngược bằng đột biến: bỏ điều kiện AC-01 thì `managerAlreadyExists` lỗi, ghi nhật ký qua `CurrentUser` thì cả 4 lỗi. `./mvnw clean verify` qua trên JDK 25 (76 đơn vị, 174 tích hợp). **Hợp đồng 000** (chủ dự án đồng ý, 2026-10-20): `Actor.type` của `contracts/api-000.yaml` thêm `BOOTSTRAP`; bốn enum `ActorType` (phê duyệt, nhật ký, đính chính, tham số) thêm giá trị này để schema `Actor` springdoc sinh ra khớp hợp đồng; `AuditLogQueryService` đọc được dòng `BOOTSTRAP` (`AuditLogBootstrapActorIT`, trước khi sửa lỗi 500); màn nhật ký web hiện "Khởi tạo hệ thống" (test Vitest trong `AuditLogTable.test.jsx`). `src/api/` sinh lại không đổi nội dung (client không mang enum). `./mvnw clean verify` qua trên JDK 25 (76 đơn vị, 175 tích hợp); Vitest 64 test, `npm run lint` sạch.
- **Rà việc 000 chờ 002** (2026-10-20, chủ dự án giao quyết): 000 T106 (phần `demo`), T103 chỉ mở khi có `PermissionApi` (T022), giới hạn trường (T023), `AccountApi` (T032), đăng nhập (T044), `ScopeApi` (T052), `ApproverApi` (T054) và người dùng mẫu. Hai chỗ thiếu trong tasks, đã thêm: (1) T079a dữ liệu mẫu `demo` của 002 (quickstart mục 1 có mô tả mà không task nào làm); chỉ dữ liệu của 002, phần tầng, ca, giấy phép, người thân do module chủ thêm, vì bản giả chỉ ở mã test và 000 đã chốt không làm bản giả quyền cho `demo`; (2) T032a họ tên người dùng: hợp đồng có `UserInfo.fullName` (002) và `Actor.fullName` (000) nhưng `account` không lưu tên và các SPI chưa có hàm lấy tên; thêm `hoTen` vào hai SPI do 002 khai báo (`HoSoNhanVienApi` cho 008, `QuanHeNguoiThanApi` cho 012) và `TaiKhoanApi.hoTen`, tên người cao tuổi qua `NguoiCaoTuoiApi` của B ở T091. Mã của T032a viết khi làm task (không khai báo trước), hợp đồng ghi ngay để E, B biết.
- **Phase 3, US1** (2026-10-20). Chủ dự án duyệt: phân loại chú thích, kéo `inScope` của T052 lên US1, lớp pháp lý tạm từ chối, cách hiểu "ngoài phạm vi" của FR-044a.
  - **T022** `PermissionService` (`@Fallback`, bản giả quyền của test 000 vẫn thắng): vai trò cùng chú thích (`FootnoteRules`: ¹, ², ⁷ là điều kiện, chỉ thỏa khi module chủ cài `FootnoteCondition`; ⁹, ¹⁵, ⁴⁰ là nhiệm vụ, lớp vai trò từ chối; mã khác chỉ mô tả, cho qua) → nhiệm vụ (T053) → phạm vi (phương án toàn viện qua; theo phân công cần đối tượng cụ thể và `ScopeApi.inScope`, đối tượng không cụ thể thì `SCOPE`) → pháp lý (`PROFESSIONAL` từ chối lớp `LEGAL` cho tới T064). Bộ lập lịch được phép, căn cứ `SCHEDULER`. `permissions()` là khóa có phương án không vướng chú thích chặn; `allowedCommands` gọi `hasPermission` từng lệnh. `hasPermissionAt` dùng quyền vai trò tại `t`, phạm vi hiện hành (theo thời điểm ở T056).
  - **FR-044a** (chủ dự án chọn cách B): `(INCIDENT_EMERGENCY, CREATE)` trên một người cao tuổi miễn lớp phạm vi; căn cứ `OUT_OF_SCOPE` khi người ghi không qua `(RESIDENT_PROFILE, VIEW)` trên người đó (vai trò ∩ phạm vi), ngược lại `ROLE`. Lý do: US1 #9 đòi Bếp được đánh dấu ngoài phạm vi, trong khi luật quy đổi cho T của Bếp phạm vi toàn viện. Chưa kiểm "người cao tuổi chưa ở trạng thái cuối" vì `NguoiCaoTuoiApi` do B khai báo, chưa có.
  - **Phần T052 kéo lên**: `ScopeService.inScope` của nhân viên từ `AssignmentApi.activeAt` và `BedAllocationApi.openAt`: trùng người cao tuổi, phòng hoặc tầng/khu. Chưa có khoảng CFG-M15-07 quanh ca; `filter`, `historyFilter`, người phụ trách ca, trưởng tầng được giao, người quyết định xem lại ném `UnsupportedOperationException` tới T052; người thân, người cao tuổi chưa có phạm vi (T071, T091).
  - **T023** `FieldVisibilityService`: mỗi vai trò, mức trong `field_visibility_rule` (không có dòng là `FULL`) chỉ tính khi đối tượng thuộc phạm vi của vai trò đó (`role.execution_scope` toàn viện, hoặc `inScope`), rồi lấy mức cao nhất (FR-034). `FieldGroupSerializerModifier` (Jackson 3 `ValueSerializerModifier.changeProperties`, đăng ký bằng bean `JacksonModule`): thuộc tính `@FieldGroup` chỉ ghi khi `FULL`; thuộc tính `hiddenFields` của DTO được ghi lại bằng danh sách trường đã bỏ; đối tượng qua `api.VisibilityTarget` (bổ sung contracts mục 1.4). Không xác định được người dùng thì bỏ mọi trường có nhóm.
  - **T024** `PermissionDataErrors` và `V002_05__permission_data_error.sql`: lỗi đang mở duy nhất theo cột sinh `open_key`; ghi trong `REQUIRES_NEW` (lệnh bị từ chối rollback vẫn giữ lỗi, sự kiện, nhật ký); lần đầu phát `PermissionDataError`, sau đó tăng `occurrences`; mỗi lần từ chối ghi nhật ký `PERMISSION_DENIED_SOURCE_DATA` (người thực hiện là người dùng hiện tại). Gọi `ScopeApi.inScope` thành công cho một người cao tuổi thì `markFixed(residentId)`: lỗi đang mở có `record_id` là người đó coi là đã khắc phục (xấp xỉ: nguồn lỗi hiện là dữ liệu theo người cao tuổi).
  - **T025** `MeController` (`GET /me`, `/me/permissions`); `UserInfo.fullName` là `null` tới T032a. Kéo một phần T078 lên: `ApiContractIT` so từng hợp đồng (api-000, api-002) với phần OpenAPI có đường dẫn của nó (hai hợp đồng có component trùng tên khác nội dung), endpoint không thuộc hợp đồng nào thì lỗi; 37 endpoint 002 chưa cài ở `pending`. Khi endpoint 002 đầu tiên có phản hồi `Problem` cần thống nhất schema `Problem` của 000 và 002 (`failedLayer`, `license`).
  - **T026** `frontend/src/common/permissions/permissions.jsx`; chưa gắn vào menu, nút (cần đăng nhập thật, T044). Sửa lỗi do T011: client web (mẫu trong `scripts/gen-api.mjs`) gửi `X-Requested-From: web` cho mọi lệnh ghi; `gen:api` thêm hợp đồng 002 (`TRANSLATED`).
  - **T021 chưa xong**: cần `historyFilter` (T052); luật kiến trúc FR-035a đã có ở T015. US1 #7, #12, #13 (danh sách, dashboard, báo cáo khoảng đã qua) kiểm cùng T052.
  - Test: `PermissionMatrixIT` (hơn 7.000 ô: 8 vai trò × 73 chức năng × 6 thao tác × 2 tầng), `ThreeLayerPermissionIT` (8), `FieldVisibilityIT` (6), `IncidentOutOfScopeIT` (3, viết sau mã, kiểm ngược bằng đột biến), `PermissionDataErrorIT` (3), `MeControllerIT` (3), `HideByPermission.test.jsx` (3), `apiClient.test.js` (2). `./mvnw clean verify` qua trên JDK 25 (76 đơn vị, 199 tích hợp); Vitest 69, lint, build qua.

- **Phase 4, US2** (2026-10-20; điểm kỹ thuật tự quyết theo cách làm chủ dự án duyệt cùng ngày, chỉ dừng hỏi điểm nghiệp vụ).
  - **T030**: không có entity JPA; `AccountStore` đọc, ghi `account`, `account_status_history`, `role_assignment`, `login_session` bằng JDBC như phần còn lại của M15. Lịch sử chỉ thêm, không sửa (quyền tài khoản MySQL của 000).
  - **T031, T032**: `AccountService` vừa là máy trạng thái vừa cài `AccountApi` (tasks ghi hai lớp `TaiKhoanService`, `TaiKhoanApiImpl`; gộp vì `AccountApi` chỉ đọc cùng bảng và `lockOnResignation` dùng chung đường khóa). `@Fallback` để bản giả của test 000 vẫn thắng. `lockOnResignation` `MANDATORY`, ghi nhật ký người thực hiện Bộ lập lịch kèm căn cứ BR-M09-04 (`SchedulerUserAspect.runAsScheduler`). Mật khẩu tạm tách ra `TemporaryPasswords` (dùng chung với T016); mã hành động nhật ký ở `AccountAuditActions`.
  - **Q-292**: thêm SPI `DutyApi.replaceDepartmentLead` (hợp đồng mục 2 `NhiemVuApi.thayPhuTrachBoPhan`; 008 cài `MANDATORY`, kiểm người thay); docs/team-plan.md mục 8 có dòng cho E.
  - **T032a**: `AccountApi.fullName` theo loại tài khoản qua `StaffProfileApi.fullName`, `FamilyRelationshipApi.familyMemberName`; module chủ chưa có bản cài thì rỗng (họ tên chỉ để hiển thị, không làm `/me` lỗi). Người thực hiện trong phản hồi của 000 (nhật ký, phê duyệt, đính chính, lịch sử tham số) điền `fullName`; tài khoản người cao tuổi rỗng tới T091.
  - **T033**: `RoleAssignmentService`; `If-Match` là `permission_version`; `AccountPermissionsQuery` dựng tập quyền dùng chung cho `/me/permissions` và phản hồi lệnh phân quyền (`MeController` gọi lại, không tự tính).
  - **Schema `Problem`**: api-002 thống nhất với api-000 (thêm `type` dạng uri, `detail`, `required`); api-000 thêm `failedLayer`, `license` (002 FR-043); `ProblemBody` có hai trường này. Gỡ dòng tương ứng ở bảng việc dang dở.
  - **`Account.fullName`** (chủ dự án duyệt 2026-10-20): api-002 thêm trường vào schema `Account` cho bảng tài khoản T035; `AccountController` điền từ `AccountApi.fullName`.
  - **Kéo một phần T076 lên**: `GET /accounts/{id}/permissions` phần hiện hành (`RoleAssignmentService.current`, chỉ Quản lý viện, ETag là `permission_version`) vì màn T035 cần ETag này cho gán, gỡ vai trò; `asOf` ném `UnsupportedOperationException` tới T076.
  - **T035**: `frontend/src/features/accounts/` (tasks ghi `tinh-nang/tai-khoan/`; theo cấu trúc thư mục hiện có): `AccountList` (bảng, lọc, phân trang con trỏ), `AccountDetail` (khung chi tiết, nút lệnh theo `allowedCommands`), `AccountCommandForm` (lý do bắt buộc; 409 `REPLACEMENT_LEAD_REQUIRED` thì hiện ô người thay, Q-292), `CreateStaffAccount` (gõ mã hồ sơ tới khi 008 có danh sách, chủ dự án duyệt; `Idempotency-Key` giữ suốt form), `TemporaryPasswordNotice` (mật khẩu tạm hiện trong khung, không dùng hộp thoại chồng lên khung). Màu trạng thái theo DESIGN.md 5.17.0. Nút tạo theo vai trò bản mẫu như màn Tham số; gắn hook quyền T026 sau T044. Test `AccountList.test.jsx` (9, viết sau mã, kiểm ngược bằng đột biến: bỏ tông Đã khóa, bỏ `If-Match`, dùng nhầm ETag thì 3 đỏ).
  - Test: `StaffAccountCreationIT` (6), `ResignationLockIT` (4), `LockUnlockIT` (7), `ManagerAlwaysExistsIT` (2), `AccountNotDeletableIT` (1), `RoleAssignmentIT` (5; ca xem quyền đỏ 404 trước khi cài), `ActorFullNameIT` (4; kiểm ngược: `fullName` trả rỗng thì 3 đỏ; ca danh sách, chi tiết đỏ trước khi thêm trường). `ApiContractIT` bỏ 9 endpoint (8 của US2 và `GET /accounts/{id}/permissions`) khỏi `pending`.

- **Phase 5, US3** (2026-10-20; điểm kỹ thuật tự quyết, trừ một điểm chủ dự án duyệt).
  - **Nhật ký đăng nhập thất bại** (chủ dự án duyệt): thêm người thực hiện `ANONYMOUS` (migration `V002_05_1__anonymous_actor.sql`, số này vì V002_06 → 08 đã dành cho T055, T063, T090; `AuditLogApi.writeAnonymous`; `Actor.type` api-000 thêm `ANONYMOUS`; bốn enum `ActorType` của 000 thêm giá trị). Mọi lần thất bại ghi `ANONYMOUS`; thành công ghi người thực hiện là chính tài khoản (đặt tạm ngữ cảnh phiên vừa mở). Tên không tồn tại: `entity_id` là chuỗi rỗng (cột NOT NULL ở 000), chi tiết có tên đã nhập.
  - **T041** `LoginService.login` không ném lỗi trong transaction: bộ đếm sai, khóa tạm, nhật ký lưu rồi `AuthController` mới trả 401 `LOGIN_FAILED` hoặc 423 (`ACCOUNT_TEMPORARILY_LOCKED` kèm `unlocksAt`, `ACCOUNT_CONTACT_FACILITY` cho Không hoạt động và Đã khóa). Quyết định tách thành hàm thuần `LoginRules.decide` để test đơn vị T038. Khóa tạm hết hạn chuyển Hoạt động ở lần đăng nhập kế tiếp (Bộ lập lịch, căn cứ CFG-M15-01); màn quản trị hiển thị Hoạt động khi đã quá `locked_until` (`AccountService.displayed`). Băm giả cho tên không tồn tại. Kênh `RESIDENT_APP` trả 422 tới T092. Không kiểm kênh khớp loại tài khoản (spec không nêu).
  - **`BusinessException.properties`**: chuyển khả năng mang trường thêm từ `PermissionDeniedException` lên lớp cha để lỗi đăng nhập có `unlocksAt` và lỗi `PASSWORD_POLICY_NOT_MET` (422) có `unmetConditions`; `ProblemBody` và schema `Problem` của api-000, api-002 thêm `unlocksAt`.
  - **Buộc đổi mật khẩu** (FR-006, research A-03): `SessionService.resolveRequest` đọc `must_change_password` cùng lần tra phiên; bộ lọc trả 403 `PASSWORD_CHANGE_REQUIRED` (mã mới, glossary 7.4) cho mọi đường trừ đổi mật khẩu, đăng xuất và `GET /me`. Cho `GET /me` qua (lệch chữ A-03 "chỉ đổi mật khẩu và đăng xuất") vì `UserInfo.mustChangePassword` của hợp đồng chỉ có nghĩa khi `/me` trả được lúc cờ bật, và web cần `/me` sau khi tải lại trang. `MeControllerIT` sửa fixture theo cờ.
  - **T042** đổi mật khẩu kiểm mật khẩu hiện tại (sai thì 422 `INVALID_VALUE`), CFG-M15-09 và khác mật khẩu hiện tại (A-03 "khác mật khẩu tạm"; hiểu là khác mật khẩu hiện tại). Mở khóa sớm `AccountService.earlyUnlock`: Quản lý viện; Trưởng tầng có phân công `FLOOR_MANAGER` cùng tầng, hoặc người có `SHIFT_LEAD` cùng tầng và cùng ca, với nhân viên có phân công cùng tầng trong ca đang diễn ra (`AssignmentApi.activeAt`); `verifiedInPerson` phải là `true` (422); nhật ký `details.authority`; vào `SELF_CHECKED`. Kích hoạt lại `AccountService.reactivate`: Hành chính, Quản lý viện; xác minh FR-052 (gọi lại đúng số đã đăng ký, hoặc có số giấy tờ); điều kiện FR-011 qua `FamilyRelationshipApi.active` và `ResidencyEndApi` với CFG-M01-04; buộc đổi mật khẩu; nhật ký ghi cách xác minh, không ghi số. `allowedCommands` thêm `early-unlock`, `reactivate`.
  - **T043** `MarkInactiveAccountsJob` (01:00, 06:00, ShedLock; `runJobs` của api-000 thêm `MARK_INACTIVE_ACCOUNTS`): mỗi tài khoản một transaction, kiểm lại dưới khóa dòng; căn cứ CFG-M15-02 kèm lần chạy. Đăng nhập thành công đặt lại mốc tính (FR-010).
  - **T044** `AuthController`; `UserInfo` tách thành record riêng dùng chung với `/me`. Đăng nhập, đăng xuất, đổi mật khẩu không gắn `@BusinessCommand` (chưa có người dùng lúc đăng nhập; đổi mật khẩu là việc của chính người dùng).
  - Test (viết trước, đỏ trước khi cài: 29/30 tích hợp, 3/3 đơn vị): `LoginTemporaryLockIT` (6), `InactivityIT` (4), `EarlyUnlockIT` (5), `LoginDoesNotRevealExistenceTest` (3), `MustChangePasswordIT` (2), `PasswordStrengthIT` (2), `AutoLogoutIT` (2), `OneDeviceOneSessionIT` (3), `EndSessionNowIT` (2), `ReactivateIT` (4). Sửa vai trò người thân trong fixture web T035: AC-10, không phải AC-06.

  - **T045** (chủ dự án chọn hướng A, 2026-10-20): một màn `/login` chung cho nhân viên và người thân (`frontend/src/features/auth/LoginPage.jsx`; tasks ghi `tinh-nang/dang-nhap/DangNhap.jsx`), thay màn mẫu `FamilyLoginPage`; `ChangePasswordPage` ở `/change-password`; `common/session.js` (mã thiết bị trong `localStorage`, gửi `X-Device-Id`; vai trò chính; nơi đến theo `mustChangePassword` và loại tài khoản). `App` gọi `GET /me`: có phiên thì vai trò thật, khối người dùng có họ tên và nút Đăng xuất; lỗi (chưa đăng nhập, không có backend) thì giữ bản mẫu đổi vai trò. `vite.config.js` thêm proxy `/api` → `localhost:8080`. Hook quyền T026 chưa gắn vào menu (menu vẫn theo `navigation.js`). Test `LoginPage.test.jsx` (5), `ChangePasswordPage.test.jsx` (3), `App.test.jsx` (2), viết trước, đỏ trước khi cài.
  - Nút Đăng xuất của cổng người thân (`FamilyPortal`, `MorePage`) vẫn là link tới `/login`, chưa gọi `POST /auth/logout` nên phiên người thân chưa kết thúc khi bấm; sửa cùng màn người thân (012) hoặc US6.

### Việc dang dở sau Phase 3 và tác động khi chạy thật (2026-10-20)

Test không bị ảnh hưởng: test của 000 và của module khác dùng bản giả (`support/fake`, `FakePermissions`). Tác động chỉ có khi chạy ứng dụng thật (`demo`, `prod`), hiện chưa chạy được vì chưa có đăng nhập (T044). Mọi chỗ dang dở đều nghiêng về từ chối, trừ FR-044a.

| Việc dang dở | Khi chạy thật | Hướng | Gỡ ở |
| --- | --- | --- | --- |
| `ScopeService.shiftLead`, `assignedFloorManager`, `filter`, `historyFilter`, `reviewDecider` ném `UnsupportedOperationException` | Đính chính của 000 (`CorrectionMethodSelector`) gọi `shiftLead`, `assignedFloorManager`: đính chính bản ghi gắn tầng lỗi 500 | Hỏng chức năng, không lộ dữ liệu | T052 |
| `GET /accounts/{id}/permissions` có `asOf` | Lỗi 500 | Hỏng chức năng | T076 |
| Chưa có bản thật `ApproverApi` (`AccountApi` có từ T032) | Phê duyệt, đính chính của 000 lỗi khi gọi | Hỏng chức năng | T054 |
| Lớp pháp lý tạm | Mọi thao tác `PROFESSIONAL` bị từ chối | Từ chối | T064 |
| Chưa có lớp nhiệm vụ | Quyền theo nhiệm vụ (Người phụ trách ca…) không dùng được | Từ chối | T053 |
| Chú thích ¹, ², ⁷ chưa có bản cài; người thân chưa có phạm vi | Người thân không xem được gì | Từ chối | T071 |
| **FR-044a chưa kiểm "người cao tuổi chưa ở trạng thái cuối"** | Ghi được sự cố cho người đã ở trạng thái cuối | **Nới hơn spec** (chỉ thêm được sự cố, không xem được dữ liệu) | Khi B khai báo `NguoiCaoTuoiApi.trangThai` (001); 002 cũng cần ở US8 (T090, T091) |
| Hook quyền web chưa gắn vào menu, nút | Giao diện còn theo chế độ đổi vai trò demo; backend vẫn chặn | Chỉ giao diện | Sau T044 |
| T021 chưa có test (US1 #7, #12, #13) | Luật kiến trúc FR-035a đã có (T015) | Thiếu kiểm chứng | T052 |

- **Thứ tự**: làm T032, T052, T054 trước dữ liệu demo T079a để chạy `demo` không gặp lỗi 500 ở đính chính, phê duyệt; khớp điều kiện mở 000 T106, T103.
- **Báo nhóm** (họp tuần, docs/team-plan.md mục 6): `ScopeApi` thật mới có `inScope`, các hàm còn lại từ US4; test vẫn dùng bản giả. Nhờ B khai báo sớm `NguoiCaoTuoiApi.trangThai` và bản giả (cho FR-044a và US8 của 002).
