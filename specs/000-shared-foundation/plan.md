# Implementation Plan: Nền tảng quy tắc nghiệp vụ dùng chung

**Branch**: `000-shared-foundation` | **Date**: 2026-10-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/000-shared-foundation/spec.md`

## Summary

Feature 000 cung cấp năm khung mà mọi module M01–M15 kế thừa:

1. Phân nhóm dữ liệu và thao tác được phép (mục A).
2. Tham số cấu hình có lịch sử, đọc lúc chạy (mục B, UC-69).
3. Vòng đời yêu cầu phê duyệt chung, áp dụng đúng một lần (mục D, UC-14).
4. Đính chính bản ghi đã xác nhận (mục C).
5. Nhật ký hệ thống, lưu giữ và loại bỏ theo chuỗi (mục E).

Hướng kỹ thuật:
- Các khung này nằm trong module M15 của backend Spring Boot một khối (research R-02).
- Bảo đảm nhóm dữ liệu bằng ba lớp: mã, API, cơ sở dữ liệu (trigger, quyền tài khoản MySQL; R-05).
- Nhật ký ghi trong cùng transaction với thay đổi (R-06).
- Tham số đọc lúc chạy; bộ đệm bị xóa sau commit (R-07).
- Yêu cầu phê duyệt có SPI `LoaiYeuCauHandler`. Mỗi lần áp dụng là một transaction riêng, có ràng buộc duy nhất chống áp dụng hai lần (R-10). *(Bổ sung theo plan 004, 2026-10-11, Q-338)* SPI có thêm `gioiHanVaiTroDuyet` (mặc định không giới hạn); 000 áp giới hạn này nhất quán ở lệnh Duyệt, Từ chối, `lenhDuocPhep`, nhắc và BR-M15-09, danh sách người nhận khi gửi duyệt (R-10, task T105).
- Đính chính dùng một bảng chung với SPI `DinhChinhDuocProvider` (R-08).
- Thông báo đi qua sự kiện có outbox tới feature 009 (R-09).

Plan này cũng chốt các quyết định nền cho mọi feature sau (R-01 → R-05): phiên bản, cấu trúc kho mã, quy ước API, thời gian, bảo đảm nhóm dữ liệu.

## Technical Context

**Language/Version**: Java 25 (backend); JavaScript (JSX, ES module) cho web, không dùng TypeScript (constitution X 3.1.0); Dart 3.x với Flutter stable 3.x (một app chung, Q-346). Chi tiết ở research R-01.

**Primary Dependencies**:
- Backend: Spring Boot 4.1.x (Web MVC, Data JPA/Hibernate 7, Security, Validation), Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock.
- Web: React 19, Vite 8, Tailwind CSS v4, shadcn/ui (Base UI), lucide-react, font Be Vietnam Pro (thay Geist theo frontend/DESIGN.md 2.0.0); TanStack Query, React Router (thêm ở T005).
- App: Flutter, `http` (có sẵn), drift (hàng đợi ngoại tuyến của app nhân viên, thêm ở T006).

**Storage**: MySQL 8.0 (bản cài trên máy), InnoDB, utf8mb4. MySQL 8.0 hết hỗ trợ từ 04/2026: trước khi vận hành phải nâng lên 8.4 LTS rồi chạy lại toàn bộ test tích hợp (research R-01). Migration chỉ dùng tính năng có ở cả 8.0 và 8.4.
- Hai tài khoản: `eldercare_app` (ứng dụng) và `eldercare_luu_tru` (job loại bỏ dữ liệu hết hạn).
- Thời điểm lưu `DATETIME(6)` UTC; ngày nghiệp vụ lưu `DATE` theo Asia/Ho_Chi_Minh (R-04).

**Testing**:
- JUnit 5, AssertJ, MySQL 8.0 cài trên máy cho test tích hợp (chạy cả trigger và quyền tài khoản; R-14);
- ArchUnit, Spring Modulith `verify()`;
- openapi-diff so mã với `contracts/`;
- Vitest và React Testing Library (web), `flutter_test` (app).
- Đồng hồ giả lập cho mọi quy tắc theo thời gian (NFR-13).

**Target Platform**: Backend Linux server (container). Web trên trình duyệt máy tính và điện thoại. App Android, iOS trên điện thoại và máy tính bảng.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung cho nhân viên và người thân (Q-346).

**Performance Goals**:
- Lệnh của 000 (đổi tham số, quyết định yêu cầu, đính chính) nằm trong ngân sách 2 giây của NFR-02. Mỗi lệnh chỉ thêm một `INSERT` nhật ký vào transaction.
- Tra lịch sử một đối tượng (SC-006) dùng chỉ mục `(loai_doi_tuong, id_doi_tuong, thoi_diem)` và phân trang theo khóa, trên khoảng 22 triệu dòng mỗi năm.
- Job áp dụng yêu cầu đến hạn nằm trong NFR-04 (5 phút).

**Constraints**:
- Mọi thay đổi nhóm 2, 3 có nhật ký trong cùng transaction (FR-047).
- Không có `UPDATE` hay `DELETE` trên bản ghi nhóm 3 đã xác nhận (constitution XII).
- Tham số không viết cứng (XIII).
- Hệ thống không tự sinh dữ liệu nghiệp vụ (FR-010a, Q-306, Q-307). Bộ lập lịch của 000 chỉ có ba job: áp dụng yêu cầu đã duyệt đến hạn, nhắc yêu cầu chờ duyệt, loại bỏ dữ liệu hết hạn. Job của các module khác chỉ được chuyển trạng thái theo thời gian, cảnh báo, leo thang, nhắc; không được `INSERT` bản ghi nghiệp vụ ngoài nhật ký, lần chạy, thông báo.
- Thời gian lấy từ một `Clock` (NFR-09).
- Lệnh của 000 chỉ chạy trực tuyến; 000 chỉ cung cấp khung ngoại tuyến cho module ghi nhận (R-13).

**Scale/Scope**:
- NFR-01: 300 người cao tuổi, 100 nhân viên, 60 người dùng đồng thời, 600 tài khoản người thân.
- 119 mã CFG (Phụ lục 25 hiện hành, gồm CFG-M11-10 theo Q-335 và CFG-M15-11 theo Q-370); 14 loại yêu cầu phê duyệt đã biết (bảng ở spec); khoảng 10 loại bản ghi nhóm 3 có đính chính.
- Một instance backend.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Hai điểm về tài liệu nguồn được ghi ở mục "Điểm cần báo lại" bên dưới; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mọi quyết định thiết kế dẫn FR, BR, DBR. Data-model mục 7 là bảng truy vết DBR, BR |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi. Công nghệ chỉ ở plan và research |
| III. Không mặc định CRUD | Đạt | R-03, R-05: nhóm 2 chỉ có endpoint lệnh; nhóm 3 chỉ thêm và đính chính. "Sửa nháp" yêu cầu là một lệnh có lý do (data-model mục 3) |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3 khớp bảng trạng thái của spec. Mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | Mọi mốc nghiệp vụ đọc `ThamSoApi`. Chu kỳ chạy job (15 phút, 00:05, hằng tháng), hạn giữ `Idempotency-Key` là thông số kỹ thuật, không phải quy tắc nghiệp vụ (xem Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Quyền đi qua `QuyenApi` (002) theo Phụ lục 27 và 19.3. Không thêm vai trò. Đổi tham số chỉ AC-01 |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ. Hai điểm lệch nguồn được báo lại, không tự giải quyết |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-011 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tạo tài liệu thiết kế. Không có mã, `src/`, cấu hình build hay SQL. Không đổi nghiệp vụ |
| X. Kiến trúc | Đạt | Một khối Spring Boot. Gói `m01`…`m15`; 000 ở `m15`. Gói `common` chỉ chứa phần kỹ thuật. REST `/api/v1`; MySQL; hai client (Q-346). Phiên bản ở R-01. NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Mọi kiểm tra BR, chuyển trạng thái, quyền ba lớp nằm ở backend. `lenhDuocPhep` trả cho client chỉ để ẩn hoặc hiện nút. Mỗi lệnh có endpoint `POST …/{id}/<lenh>` riêng |
| XII. Dữ liệu | Đạt | Nhóm 3 chỉ thêm (trigger cùng quyền tài khoản). Nhật ký trong cùng transaction. Thời gian theo R-04. Mỗi DBR liên quan có cách bảo đảm (data-model mục 7) |
| XIII. Tham số | Đạt | `tham_so` cùng `lich_su_tham_so`, đọc lúc chạy. Enum `MaThamSo` sinh từ Phụ lục 25; ArchUnit cấm hằng số thời lượng ở tầng service |
| XIV. Ngoại tuyến, thông báo | Đạt | Khung ngoại tuyến chung (R-13); lệnh của 000 bắt buộc trực tuyến. Thông báo chỉ qua sự kiện tới 009 (R-09) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR-M15-03, 04, 06 → 13 có test tích hợp ở tầng backend hoặc cơ sở dữ liệu (data-model mục 7) |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Các trạng thái mở rộng của 001 và 015 chỉ được dành chỗ trong SPI; nội dung do plan của hai feature đó quyết.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 000 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | Ước lượng khối lượng nhật ký; chỉ mục; phân vùng theo năm (R-06) |
| NFR-02 Hiệu năng | Có | Mỗi lệnh thêm một `INSERT` nhật ký; tham số đọc từ bộ đệm trong tiến trình (R-07) |
| NFR-03 Thông báo khẩn cấp | Gián tiếp | 000 không phát sự kiện khẩn cấp. Outbox giao sau commit, không chặn đường khẩn cấp của 007, 009 |
| NFR-04 Tác vụ định kỳ | Có | Job áp dụng yêu cầu: chạy lại không tạo trùng (`ket_qua_ap_dung` khóa chính theo yêu cầu, R-10) |
| NFR-05 Khả dụng | Có | Job chạy lúc 00:05 và lúc khởi động (bắt kịp sau gián đoạn). Bảo trì ngoài khung giao ca là việc vận hành |
| NFR-06 Sao lưu | Gián tiếp | Sao lưu MySQL hằng ngày giữ 30 ngày (vận hành). Lưu ý: bản khôi phục không được làm sống lại dữ liệu đã loại bỏ; quy trình ghi ở task triển khai |
| NFR-07 Lưu trữ | Có | Job loại bỏ theo chuỗi, tài khoản riêng, nhật ký tóm tắt (R-11) |
| NFR-08 Bảo mật | Có | Che trường nhật ký ở backend (R-06); người thân bị chặn khỏi nhật ký. HTTPS và băm mật khẩu ở 002 |
| NFR-09 Thời gian | Có | Một `Clock`, múi giờ Asia/Ho_Chi_Minh; ArchUnit cấm `now()` (R-04) |
| NFR-10 Thiết bị | Có | Màn tham số và tra nhật ký ở web. Màn duyệt yêu cầu ở web và app nhân viên. Người thân không dùng chức năng nào của 000 |
| NFR-11 Ngôn ngữ | Có | API trả ISO-8601; client hiển thị dd/MM/yyyy, giao diện tiếng Việt |
| NFR-12 Cấu hình | Có | Đổi tham số không cần triển khai lại (R-07, SC-004) |
| NFR-13 Kiểm thử | Có | `DieuKhienDongHo`, endpoint chỉ ở profile test và demo (R-04) |

## Project Structure

### Documentation (this feature)

```text
specs/000-shared-foundation/
├── plan.md                      # File này
├── research.md                  # Phase 0: R-01 → R-14
├── data-model.md                # Phase 1: quy ước nhóm dữ liệu, bảng của 000, truy vết DBR/BR
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-000.yaml             # OpenAPI 3.1: tham số, yêu cầu phê duyệt, đính chính, nhật ký, đồng hồ kiểm thử
│   └── module-interfaces.md     # Giao diện Java giữa module: 000 cung cấp / 000 cần
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
```

### Source Code (repository root)

Cấu trúc cho giai đoạn triển khai. Tên thư mục, gói dưới đây là tên logic; khi viết mã dịch sang tiếng Anh theo [glossary.md](glossary.md) (R-03). Ba thư mục gốc `backend/`, `frontend/`, `mobile/` đã được khởi tạo (sửa 2026-10-16, constitution 3.0.0; web giữ JavaScript theo constitution 3.1.0, file `.jsx`/`.js`, alias `@/` trỏ `frontend/src/`).

```text
backend/
├── src/main/java/com/cambae/backend/
│   ├── common/                  # Clock, ThoiGianNghiepVu, LyDo, @NhomDuLieu, @LenhNghiepVu,
│   │                            # ChiThemRepository, LoiNghiepVu, Problem Details, Idempotency, PhongBiNgoaiTuyen
│   ├── m01/ … m14/              # module nghiệp vụ (feature 001, 003–019 theo Phụ lục 26.2)
│   └── m15/
│       ├── thamso/   {api, internal, web}
│       ├── nhatky/   {api, internal, web}
│       ├── pheduyet/ {api, internal, web, lich}
│       ├── dinhchinh/{api, internal, web}
│       ├── luutru/   {api, internal, lich}
│       └── taikhoan/ …          # feature 002 (plan sau)
├── src/main/resources/db/migration/   # Flyway (giai đoạn triển khai)
└── src/test/java/com/cambae/backend/        # cùng cấu trúc; *Test (đơn vị), *IT (tích hợp MySQL), *ArchTest

frontend/
└── src/
    ├── api/                     # client sinh từ OpenAPI
    ├── components/ui/           # component shadcn/ui (thêm bằng `npx shadcn add`)
    ├── lib/                     # tiện ích dùng chung (`cn`)
    ├── chung/                   # xử lý Problem Details, 412, hiển thị "đã thay đổi"
    └── tinh-nang/
        ├── tham-so/
        ├── nhat-ky/
        └── yeu-cau-phe-duyet/

mobile/                          # một app chung cho nhân viên và người thân (Q-346)
├── lib/
│   ├── chung/                   # đăng nhập, API client, hàng đợi ngoại tuyến (drift), phong bì đồng bộ
│   ├── nhan-vien/yeu-cau-phe-duyet/
│   └── nguoi-than/              # 000 không có màn hình cho người thân
└── test/{nhan-vien, nguoi-than}/
```

**Structure Decision**: Một kho mã, ba thư mục gốc (R-02). Feature 000 nằm ở `backend/…/m15/{thamso, nhatky, pheduyet, dinhchinh, luutru}` và `backend/…/common`. Ở web có ba màn: tham số, nhật ký, hộp duyệt yêu cầu. App nhân viên có hộp duyệt yêu cầu, cần cho Trưởng tầng duyệt yêu cầu của 015. Module khác chỉ dùng 000 qua `contracts/module-interfaces.md`.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Gói `com.cambae.backend.common` ngoài M01–M15 | Cần chỗ cho thành phần kỹ thuật mọi module dùng (Clock, lỗi, chú thích nhóm dữ liệu). Gói không chứa dữ liệu nghiệp vụ, Modulith coi là gói dùng chung | Đặt trong `m15` thì mọi module phụ thuộc phần nội bộ của M15 |
| Bảng kỹ thuật `chuoi_dinh_chinh`, `khoa_lap_lai`, `lan_chay_lich`, `event_publication`, `shedlock` được `UPDATE` và không ghi nhật ký nghiệp vụ | Đây là dữ liệu dẫn xuất hoặc hạ tầng, không thuộc ba nhóm của mục 1.5. Mọi thay đổi nghiệp vụ tương ứng (thêm đính chính, áp dụng yêu cầu) đã có nhật ký riêng | Ghi nhật ký cho cả bảng kỹ thuật làm nhật ký phình to mà không thêm thông tin truy vết |
| Cột `da_nhac_luc`, `da_bao_qlv_luc` trên `yeu_cau_phe_duyet` do Bộ lập lịch đặt | Chống nhắc lặp. Mỗi lần nhắc hoặc báo vẫn ghi nhật ký `YC_NHAC` / `YC_BAO_QLV`, người thực hiện là Bộ lập lịch, có căn cứ CFG (FR-045) | Suy ra từ `event_publication` thì phụ thuộc bảng của thư viện |
| Chu kỳ chạy job và hạn giữ `Idempotency-Key` đặt trong cấu hình ứng dụng, không phải mã CFG | Đây là thông số vận hành, không có trong Phụ lục 25 và không đổi quy tắc nghiệp vụ. Mốc nghiệp vụ (CFG-M15-05, 06) vẫn đọc từ `tham_so`. Chu kỳ 15 phút làm nhắc trễ tối đa 15 phút so với mốc | Thêm mã CFG mới thì phải sửa tài liệu nguồn chỉ vì một thông số kỹ thuật |

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới, rồi sửa `docs/business/nghiep-vu.md` và spec 000.

1. **Phụ lục 25 không có kiểu và khoảng hợp lệ của tham số.** FR-011 và FR-015 đòi mỗi tham số có "kiểu giá trị, khoảng hợp lệ", và giá trị ngoài khoảng bị từ chối. Phụ lục 25 chỉ có mã, mô tả, mặc định, nơi dùng. Plan tự đặt kiểu (suy từ đơn vị của giá trị mặc định) và ràng buộc kỹ thuật hiển nhiên (thời lượng > 0, tỷ lệ 0–100, số lượng ≥ 0). Plan chỉ dùng ràng buộc nghiệp vụ khi nguồn có ghi, ví dụ CFG-M15-06 > CFG-M15-05. Cận trên và cận dưới nghiệp vụ (ví dụ CFG-M05-01 không được dài quá bao nhiêu) chưa có nguồn. Đề nghị: thêm cột "Khoảng hợp lệ" vào Phụ lục 25, hoặc chốt bằng Q rằng chỉ áp ràng buộc kỹ thuật. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
2. **BR-M15-07 và FR-016 khác nhau về thời điểm hiệu lực của tham số.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - BR-M15-07 ghi "Ngày hiệu lực của phiên bản, **tham số** và yêu cầu là ngày lịch, có hiệu lực từ 00:00 của ngày đó".
   - FR-016, NFR-12 và US4 #3 ghi giá trị mới "có hiệu lực ngay từ thời điểm lưu".
   - Plan theo FR-016 và US4 #3: hiệu lực ngay, lịch sử theo thời điểm (R-07), vì đó là hành vi spec đã viết kịch bản kiểm thử.
   - Đề nghị chốt bằng Q: bỏ chữ "tham số" khỏi BR-M15-07, hoặc nếu muốn tham số có ngày hiệu lực tương lai thì bổ sung lệnh "đặt giá trị từ ngày".
3. **(Nhỏ, đồng bộ câu chữ spec)** Mục "Phạm vi" của spec 000 ghi "mọi module (001–016)". Hiện đã có feature 017–019 cũng dùng 000 (bảng loại yêu cầu ở FR-031 có 017, 019). Đề nghị sửa thành "mọi feature 001–019" khi đồng bộ spec lần sau. Không ảnh hưởng thiết kế. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
