---
description: "Danh sách task triển khai feature 000 – Nền tảng quy tắc nghiệp vụ dùng chung"
---

# Tasks: Nền tảng quy tắc nghiệp vụ dùng chung (000)

**Input**: Tài liệu thiết kế ở `specs/000-shared-foundation/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: plan.md, spec.md, research.md (R-01 → R-14), data-model.md, contracts/api-000.yaml, contracts/module-interfaces.md

**Tests**: Bắt buộc. Constitution XV yêu cầu mỗi BR có ít nhất một kiểm thử tự động dựng từ kịch bản Given/When/Then; tên lớp test theo quickstart.md mục 4 và data-model.md mục 7. Tên phương thức test theo R-14 (`BR_M15_09_...`, `FR_037_...`).

**Giai đoạn**: Triển khai (chủ dự án mở ngày 2026-10-10); cách làm theo mục "Giai đoạn hiện tại" của CLAUDE.md.

**Organization**: Task nhóm theo user story của spec 000 (US1–US5).

## Format: `[ID] [P?] [Story] Mô tả`

- **[P]**: chạy song song được (khác file, không phụ thuộc task chưa xong)
- **[USn]**: user story mà task phục vụ

## Quy ước đường dẫn (research R-02)

- Backend: `backend/src/main/java/com/cambae/backend/` (viết tắt `BE/`), test `backend/src/test/java/com/cambae/backend/` (viết tắt `BT/`), migration `backend/src/main/resources/db/migration/` (viết tắt `MIG/`)
- Web: `frontend/src/`; app nhân viên: `mobile/lib/`, test `mobile/test/nhan-vien/`
- Feature 000 nằm ở `BE/common/` và `BE/m15/{thamso,nhatky,pheduyet,dinhchinh,luutru}/`, mỗi gói con chia `api/`, `internal/`, `web/` (và `lich/` cho job)

---

## Phase 1: Setup (hạ tầng kho mã chung)

**Purpose**: Dựng bốn thư mục gốc và bộ công cụ mà mọi feature 001–019 dùng lại (R-01, R-02)

- [x] T001 Dùng ba thư mục gốc đã khởi tạo `backend/`, `frontend/`, `mobile/` (R-02); viết `README.md` gốc mô tả cấu trúc, và `.gitignore` gốc bỏ qua kết quả build và file IDE (`backend/target/`, `frontend/node_modules/`, `frontend/dist/`, `mobile/.dart_tool/`, `mobile/build/`, `.idea/` ở mọi cấp); gỡ `backend/.idea/`, `mobile/.idea/`, `mobile/mobile.iml` khỏi phần commit
- [ ] T002 Hoàn thiện dự án đã khởi tạo trong `backend/pom.xml` (Spring Boot 4.1.x, Java 25, gói gốc `com.cambae.backend`): giữ Web MVC, Data JPA (Hibernate 7), Security, Validation, Lombok; thêm Spring Modulith 2.x, Flyway (`spring-boot-starter-flyway`, `flyway-mysql`), springdoc-openapi 3.x, ShedLock, driver MySQL; test: JUnit 5, AssertJ, ArchUnit, openapi-diff. Đổi `application.properties` sang Flyway (`ddl-auto=validate`), bỏ `spring-boot-devtools` khỏi profile `prod` (R-01)
- [ ] T003 [P] Giữ lớp khởi động `BE/BackendApplication.java` và tạo các gói rỗng `BE/common/`, `BE/m01/` … `BE/m15/` mỗi gói có `package-info.java` khai báo `@ApplicationModule` (R-02)
- [ ] T004 [P] Cấu hình `backend/src/main/resources/application.yml` với ba profile `prod`, `test`, `demo`; `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`, JDBC `connectionTimeZone=UTC`, MySQL `utf8mb4_0900_ai_ci`; hai datasource `eldercare_app` (chính) và `eldercare_luu_tru` (job loại bỏ) (R-04, R-05)
- [ ] T005 [P] Hoàn thiện `frontend/` đã khởi tạo (React 19 + Vite, JavaScript, Tailwind CSS v4, shadcn/ui; giữ JavaScript, không chuyển TypeScript theo constitution X 3.1.0): bỏ file mẫu của Vite (`App.css`, `assets/hero.png`, `react.svg`, `vite.svg`); cài TanStack Query, React Router, Vitest, React Testing Library, thêm script `test` vào `frontend/package.json` (R-01)
- [x] T005a [P] Design system web và khung ứng dụng (giai đoạn thiết kế giao diện, 2026-10-16): biến màu teal, trắng ấm, sage có chế độ tối trong `frontend/src/index.css`; khung ứng dụng và menu theo vai trò căn cứ Phụ lục 27 ở `frontend/src/common/layout/`; màn mẫu dashboard (016) và danh sách người cao tuổi (001) dùng dữ liệu minh họa `frontend/src/common/demoData.js`, xóa khi màn hình lấy dữ liệu từ API (T010)
- [ ] T006 [P] Hoàn thiện app Flutter chung `mobile/pubspec.yaml` (đã có http, flutter_test): thêm drift; `mobile/lib/main.dart` mở màn đăng nhập rồi chuyển sang phần nhân viên hoặc phần người thân theo loại tài khoản (Q-346; R-01, R-13)
- [ ] T007 [P] Tạo khung thư mục hai phần của app chung: `mobile/lib/nhan-vien/`, `mobile/lib/nguoi-than/` (tên logic, dịch theo glossary), dùng chung `mobile/lib/chung/` (phần người thân không có màn hình của 000)
- [ ] T008 [P] Cấu hình lint/format: Checkstyle hoặc Spotless trong `backend/pom.xml`, ESLint (`frontend/eslint.config.js` đã có, bổ sung cho file test Vitest) + Prettier, `mobile/analysis_options.yaml`
- [ ] T009 [P] Dựng lớp cơ sở test tích hợp `BT/support/MySqlIntegrationTestBase.java` chạy trên MySQL 8.0 cài trên máy (schema `eldercare_test`, kết nối từ biến môi trường `TEST_DB_URL`, `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD`), xóa và tạo lại schema, tạo hai tài khoản `eldercare_app` và `eldercare_luu_tru` với quyền đúng R-05 trước khi Flyway chạy (R-14)
- [ ] T010 [P] Thiết lập sinh client API: script `frontend/scripts/gen-api.mjs` sinh `frontend/src/api/` từ `specs/*/contracts/api-*.yaml` (bản đã dịch tiếng Anh, R-03); script tương tự cho `mobile/lib/chung/api/`

---

## Phase 2: Foundational (điều kiện tiên quyết, chặn mọi user story)

**Purpose**: Gói `common`, đồng hồ, lỗi, nhật ký ghi, tham số đọc, bản giả của 002, sự kiện. Mọi user story của 000 và mọi feature sau dùng các phần này.

**⚠️ CRITICAL**: Không bắt đầu user story nào trước khi phase này xong.

### Thời gian (R-04, NFR-09, DBR-25, BR-M15-07)

- [ ] T011 [P] Tạo bean `Clock` múi giờ `Asia/Ho_Chi_Minh` trong `BE/common/thoigian/DongHoConfig.java` và tiện ích `BE/common/thoigian/ThoiGianNghiepVu.java` với `homNay()`, `bayGio()`, `batDauNgay(LocalDate)` (00:00 +07:00), `trongKhoang(LocalDate, tu, den)` gồm cả hai đầu (FR-008, FR-046, BR-M15-07)
- [ ] T012 [P] Tạo `BE/common/thoigian/DieuKhienDongHo.java` (đặt, tua) chỉ nạp ở profile `test`, `demo`, và controller `BE/common/thoigian/web/DongHoKiemThuController.java` cho `GET/POST /api/v1/kiem-thu/dong-ho` theo contracts/api-000.yaml (NFR-13)
- [ ] T013 [P] Test kiến trúc `BT/common/ThoiGianArchTest.java`: cấm `Instant.now()`, `LocalDate.now()`, `LocalDateTime.now()` ngoài `DongHoConfig`; cấm hằng số `Duration` trong gói `service`/`internal` của module (DBR-25, constitution XIII)
- [ ] T014 [P] Test đơn vị `BT/common/NgayHieuLucTest.java` cho `batDauNgay` và `trongKhoang` (BR-M15-07)

### Lỗi, API, chống lặp (R-03)

- [ ] T015 [P] Tạo phân cấp `BE/common/loi/LoiNghiepVu.java` gồm `DieuKienChuaThoa(List<DieuKien>)`→409, `KhongCoQuyen`→403, `DaDuocXuLy`→409, `SaiPhienBan(giaTriHienHanh)`→412, `ThieuLyDo`→422, `GiaTriKhongHopLe`→422 (FR-006, FR-021, FR-040)
- [ ] T016 Tạo `BE/common/loi/web/ProblemDetailsHandler.java` (`@RestControllerAdvice`) trả `application/problem+json` có `maLoi`, `dieuKienChuaThoa[]` (mã BR, mô tả), `giaTriHienHanh`; 404 cho đối tượng ngoài phạm vi không lộ tồn tại (R-03)
- [ ] T017 [P] Tạo value object `BE/common/LyDo.java`: "Không rỗng, không chỉ có khoảng trắng, tối đa 2000 ký tự; sai thì `422 THIEU_LY_DO`" (FR-044, SC-008)
- [ ] T018 [P] Tạo hỗ trợ ETag/`If-Match` trong `BE/common/web/PhienBanEtag.java`: trả `ETag` = `phien_ban`, bắt buộc `If-Match` cho lệnh sửa, sai thì `SaiPhienBan` (FR-021, FR-040)
- [ ] T019 Tạo bảng kỹ thuật `khoa_lap_lai` trong `MIG/V000_01__khoa_lap_lai.sql`: khóa chính `(khoa, nguoi_dung_id)`, cột `ma_bam_yeu_cau`, `ket_qua_json`, `het_han_luc` (giữ 7 ngày, thông số kỹ thuật) (data-model mục 6)
- [ ] T020 Tạo bộ lọc `BE/common/web/IdempotencyFilter.java`: cùng `Idempotency-Key` cùng người dùng trả lại kết quả lần đầu, không tạo bản ghi thứ hai (R-03)
- [ ] T021 [P] Tạo `BE/common/ngoaituyen/PhongBiNgoaiTuyen.java` (`idempotencyKey`, `thoiDiemThietBi`, `ngoaiTuyen`) và enum `KetQuaDongBo` `CHAP_NHAN`, `CHAP_NHAN_KEM_NHAN`, `CHO_XEM_LAI`, `TU_CHOI` kèm lý do (FR-046, DBR-25, BR-M15-20; R-13)

### Nhóm dữ liệu – phần khung (R-05)

- [ ] T022 [P] Tạo chú thích `BE/common/dulieu/NhomDuLieu.java` (`DANH_MUC | NGHIEP_VU | GHI_NHAN`) và `BE/common/dulieu/LenhNghiepVu.java` (`batBuocLyDo = true`) (FR-001, FR-005, FR-007)
- [ ] T023 [P] Tạo `BE/common/dulieu/ChiThemRepository.java` chỉ có `them(T)`, `timTheoId`, truy vấn đọc; không có `save` cho bản ghi đã có, không có `delete` (FR-009)
- [ ] T024 Tạo migration quyền tài khoản `MIG/V000_00__tai_khoan_csdl.sql` (chỉ chạy ở môi trường dựng sẵn; test dùng T009): `eldercare_app` không có `DELETE` trên bảng nhóm 3, chỉ `SELECT, INSERT` trên `nhat_ky`, `dinh_chinh`, `lich_su_tham_so`; `eldercare_luu_tru` có `DELETE` (R-05)

### Bản giả giao diện của 002 (R-12; thay bằng bản thật khi làm 002)

- [ ] T025 [P] Khai báo giao diện tạm `NguoiDungHienTai`, `QuyenApi.coQuyen(nd, KhoaQuyen, DoiTuong)`, `PhamViApi`, `QuyenXemTruongApi`, `NguoiDuyetApi`, `TaiKhoanApi.coVaiTro` (bổ sung theo plan 004, Q-338) trong `BE/m15/taikhoan/api/` đúng chữ ký ở contracts/module-interfaces.md mục 2, và bản giả `BT/support/QuyenGia.java` (bảng vai trò → quyền tối thiểu)

### Nhật ký – phần ghi (R-06, FR-042 → FR-047)

- [ ] T026 Tạo bảng `nhat_ky` trong `MIG/V000_02__nhat_ky.sql` phân vùng theo năm trên `thoi_diem`, khóa chính `(id, thoi_diem)`, cột theo data-model mục 5: `nguoi_thuc_hien_loai ENUM('NGUOI_DUNG','BO_LAP_LICH','KHOI_TAO')`, `nguoi_thuc_hien_id` "`NOT NULL` khi `NGUOI_DUNG`", `vai_tro VARCHAR(10)`, `hanh_dong VARCHAR(40)`, `doi_tuong_loai VARCHAR(40)`, `doi_tuong_id VARCHAR(64)`, `nguoi_cao_tuoi_id BIGINT NULL`, `du_lieu_truoc/du_lieu_sau JSON NULL`, `ly_do VARCHAR(2000) NULL`, `can_cu JSON NULL`, `chi_tiet JSON NULL`, `khong_loai_bo BOOLEAN`, `thoi_diem_thiet_bi DATETIME(6) NULL`; chỉ mục `(doi_tuong_loai, doi_tuong_id, thoi_diem)`, `(nguoi_thuc_hien_id, thoi_diem)`, `(hanh_dong, thoi_diem)`, `(nguoi_cao_tuoi_id)`; trigger `trg_nhat_ky_khong_sua`, `trg_nhat_ky_khong_xoa` theo data-model mục 1.4
- [ ] T027 Tạo `BE/m15/nhatky/api/NhatKyApi.java` và `BanGhiNhatKy` (builder: `hanhDong`, `doiTuong`, `nguoiCaoTuoi`, `truoc`, `sau`, `lyDo`, `canCu`, `chiTiet`, `thoiDiemThietBi`); `build()` lỗi khi hành động thuộc tập bắt buộc lý do mà thiếu `lyDo`, hoặc người thực hiện là Bộ lập lịch mà thiếu `canCu` (FR-044, FR-045)
- [ ] T028 Cài `BE/m15/nhatky/internal/NhatKyService.java`: `ghi()` với `Propagation.MANDATORY` (FR-047); `ghiNgoaiTransaction()` với `REQUIRES_NEW` chỉ cho FR-037b và FR-051; thời điểm từ `Clock` (FR-046); vai trò lấy từ `NguoiDungHienTai`

### Tham số – phần đọc (R-07; lệnh đổi ở US4)

- [ ] T029 Tạo bảng `tham_so` và `lich_su_tham_so` trong `MIG/V000_03__tham_so.sql` theo data-model mục 2: `tham_so.ma VARCHAR(16)` khóa chính, `kieu ENUM('THOI_LUONG','SO_NGUYEN','TY_LE','SO_TIEN','GIO_TRONG_NGAY','BANG')`, `rang_buoc JSON`, `gia_tri_mac_dinh JSON`, `gia_tri_hien_hanh JSON NOT NULL`, `phien_ban INT`; `lich_su_tham_so` có `UNIQUE (ma, hieu_luc_tu)`, `ly_do VARCHAR(1000)` `NOT NULL` trừ lần nạp ban đầu, `la_khoi_phuc_mac_dinh BOOLEAN`; trigger chỉ thêm cho `lich_su_tham_so` (DBR-24)
- [ ] T030 Tạo enum `BE/m15/thamso/api/MaThamSo.java` đủ 119 mã CFG hiện hành của Phụ lục 25 `docs/business/nghiep-vu.md` (đối chiếu lại số lượng với Phụ lục 25 lúc triển khai)
- [ ] T031 Tạo migration nạp `MIG/V000_04__tham_so_nap_ban_dau.sql`: một dòng `tham_so` và một dòng `lich_su_tham_so` (người thực hiện Bộ lập lịch, `hieu_luc_tu` là thời điểm khởi tạo) cho mỗi mã; chuyển giá trị mặc định sang JSON có cấu trúc (ví dụ "48 giờ" → `{"giaTri":48,"donVi":"GIO"}`), rà tay từng mã; `rang_buoc` chỉ gồm ràng buộc kỹ thuật (thời lượng > 0, số nguyên ≥ 0, tỷ lệ 0–100) và ràng buộc có nguồn (CFG-M15-06 > CFG-M15-05) (plan "Điểm cần báo lại" #1)
- [ ] T032 Cài `BE/m15/thamso/api/ThamSoApi.java`, `GiaTriThamSo` bất biến (`asDuration()`, `asInt()`, `asPercent()`, `asMoney()`, `asLocalTime()`, `asTable(Class<T>)`) và `BE/m15/thamso/internal/ThamSoService.java` với `doc(MaThamSo)` có bộ đệm trong tiến trình và `docTaiThoiDiem(ma, Instant)` (FR-018, FR-019)

### Sự kiện và bộ lập lịch (R-09, R-10)

- [ ] T033 [P] Cấu hình Event Publication Registry của Spring Modulith lưu MySQL (bảng `event_publication`) trong `BE/common/sukien/SuKienConfig.java`; sự kiện chỉ giao sau commit, giao lại khi tiến trình chết (R-09)
- [ ] T034 [P] Tạo `BE/common/thongbao/` theo plan 009 (research B-02): `YeuCauThongBao`, `CoYeuCauThongBao`, `NhomNhan`, `CachThay`, `MucThongBao`, `LoaiThongTin`, `TRUONG_SUC_KHOE` (contracts/module-interfaces.md mục 1.1)
- [ ] T035 [P] Cấu hình ShedLock (bảng `shedlock`) và bảng `lan_chay_lich` (`id`, `job`, `bat_dau`, `ket_thuc`, `ket_qua`, `so_xu_ly`) trong `MIG/V000_05__lich.sql` và `BE/common/lich/LichConfig.java`; tiện ích `BE/common/lich/LanChayLich.java` cấp `lanChayLichId` cho `canCu` (FR-045)
- [ ] T036 [P] Tạo `BE/common/tep/TepApi.java` (`taiLen(...)` trả `tepId`; `gan(tepId, loaiChuSoHuu, idChuSoHuu)` `MANDATORY`, tệp đã gắn không gắn lại) và SPI `QuyenTepProvider` (bổ sung theo plan 001, H-13; NFR-08)

### Kiến trúc

- [ ] T037 [P] Test `BT/ModuleBoundaryTest.java` gọi `ApplicationModules.of(EldercareApplication.class).verify()` (constitution X)
- [ ] T038 [P] Test `BT/common/HopDongApiTest.java` so OpenAPI sinh bởi springdoc với `specs/000-shared-foundation/contracts/api-000.yaml` bằng openapi-diff; mở rộng dần cho `api-001.yaml` … `api-019.yaml` (R-14)
- [ ] T039 Test `BT/m15/nhatky/NhatKyNguyenTuIT.java`: lệnh lỗi sau khi ghi nhật ký thì cả thay đổi và nhật ký cùng rollback; gọi `ghi()` ngoài transaction thì lỗi (US3 #6, FR-047)

**Checkpoint**: Nền sẵn sàng – các user story của 000 và plan của 002 bắt đầu được.

---

## Phase 3: User Story 1 – Mỗi loại dữ liệu chỉ cho phép đúng thao tác của nhóm mình (Priority: P1) 🎯 MVP

**Goal**: Ba lớp bảo đảm nhóm dữ liệu (mã, API, cơ sở dữ liệu) có mẫu và test để mọi module dùng lại (FR-001 → FR-010, BR-M15-03, BR-M15-06, BR-M15-07).

**Independent Test**: Dùng danh mục mẫu (nhóm 1), nghiệp vụ mẫu có phiên bản (nhóm 2), bản ghi mẫu `M15.GHI_NHAN_MAU` (nhóm 3); thử tạo, sửa, xóa, ngừng hiệu lực, đổi trạng thái trực tiếp; chỉ thao tác đúng nhóm được chấp nhận (quickstart K15, K20).

### Tests cho User Story 1

- [ ] T040 [P] [US1] Test `BT/m15/NhomDuLieuArchTest.java`: mọi `@Entity` có đúng một `@NhomDuLieu`; entity nhóm 2, 3 không có setter trạng thái công khai và không mang Lombok `@Data`, `@Setter` (R-05); phương thức `@LenhNghiepVu(batBuocLyDo=true)` có tham số `LyDo` (FR-001, FR-005, FR-007)
- [ ] T041 [P] [US1] Test `BT/m15/DanhMucMauIT.java`: xóa mục chưa dùng → 204; xóa mục đã được bản ghi nhóm 2/3 (kể cả Nháp, Hủy) tham chiếu → 409 `DANH_MUC_DA_DUOC_THAM_CHIEU`; nhật ký của chính mục không tính; mục Ngừng hiệu lực không chọn được cho nghiệp vụ mới nhưng vẫn hiển thị trong bản ghi cũ; kích hoạt lại khi có mục Hiệu lực trùng tên → 409 (US1 #1, #2, #8; FR-002, FR-003, BR-M15-06)
- [ ] T042 [P] [US1] Test `BT/m15/PhienBanKhongChongKhoangIT.java`: hai luồng đồng thời thêm phiên bản giao khoảng hiệu lực, chỉ một thành công (FR-008, BR-M15-07)
- [ ] T043 [P] [US1] Test `BT/m15/NhomGhiNhanCoTriggerIT.java`: đối chiếu danh sách entity `@NhomDuLieu(GHI_NHAN)` với `information_schema.TRIGGERS`; `UPDATE`/`DELETE` trực tiếp bằng `eldercare_app` trên bảng mẫu nhóm 3 bị MySQL chặn (thiếu quyền hoặc `SQLSTATE 45000`) (US1 #7, SC-002, FR-009, FR-010, BR-M15-03)
- [ ] T044 [P] [US1] Test `BT/m15/LenhNghiepVuLyDoTest.java`: lệnh nhóm 2 thiếu lý do → 422 `THIEU_LY_DO` trước khi chạm dữ liệu; không có endpoint `PUT/PATCH/DELETE` cho tài nguyên nhóm 2, 3 (FR-005, FR-007, SC-008)

### Implementation cho User Story 1

- [ ] T045 [P] [US1] Viết mẫu migration và tài liệu quy ước `backend/docs/quy-uoc-nhom-du-lieu.md`: cột chuẩn nhóm 1 (`trang_thai ENUM('HIEU_LUC','NGUNG_HIEU_LUC')`, cột sinh `ten_hieu_luc = IF(trang_thai='HIEU_LUC', ten, NULL)` có `UNIQUE`, `phien_ban`), nhóm 2 (`trang_thai`, `phien_ban`, `tao_luc`, `tao_boi`; bảng phiên bản `hieu_luc_tu DATE`, `hieu_luc_den DATE NULL`, `CHECK (hieu_luc_den IS NULL OR hieu_luc_den >= hieu_luc_tu)`), nhóm 3 (`thoi_diem`, `thoi_diem_thiet_bi`, `nguoi_thuc_hien_id`, `thoi_diem_xac_nhan`, `tang_khu_vuc_id`) và hai trigger `trg_<bang>_khong_sua`, `trg_<bang>_khong_xoa` (data-model mục 1)
- [ ] T046 [P] [US1] Tạo lớp cơ sở `BE/common/dulieu/DanhMucCoSo.java` (trạng thái, ngừng hiệu lực, kích hoạt lại có kiểm trùng tên) và ánh xạ vi phạm khóa ngoại `ON DELETE RESTRICT` thành 409 `DANH_MUC_DA_DUOC_THAM_CHIEU` trong `BE/common/loi/web/ProblemDetailsHandler.java` (FR-002, FR-003)
- [ ] T047 [P] [US1] Tạo tiện ích `BE/common/dulieu/PhienBanHieuLuc.java`: khóa dòng cha `SELECT … FOR UPDATE`, kiểm `NOT EXISTS` phiên bản giao khoảng, rồi thêm (FR-004, FR-008)
- [ ] T048 [US1] Tạo dữ liệu mẫu kiểm thử (chỉ profile `test`, `demo`) `BT/m15/mau/` và `MIG/demo/V000_90__du_lieu_mau.sql`: danh mục mẫu, bản ghi nhóm 3 `M15.GHI_NHAN_MAU` gắn tầng và một loại không gắn tầng, có trigger theo T045 (CHK029; quickstart mục 1)

**Checkpoint**: Khung nhóm dữ liệu chạy và được kiểm thử độc lập.

---

## Phase 4: User Story 2 – Sai sót trên bản ghi đã xác nhận được sửa bằng bản đính chính (Priority: P1)

**Goal**: Đính chính chỉ thêm, trỏ đúng bản gốc, chọn cách tạo A/B/C, giá trị hiện hành đọc qua view (FR-023 → FR-030, DBR-23).

**Independent Test**: Ghi bản ghi mẫu nhóm 3, xác nhận, đính chính; bản gốc không đổi, đính chính trỏ về gốc, giá trị hiện hành là bản đính chính, chuỗi xem lại được (quickstart K14, K16, K17).

### Tests cho User Story 2

- [ ] T049 [P] [US2] Test đơn vị `BT/m15/dinhchinh/DinhChinhChonCachTest.java`: bảng quyết định A/B/B'/C/403 của data-model mục 4 "Chọn cách tạo", gồm bản ghi không gắn tầng với Trưởng tầng (403) (FR-026, CHK033, CHK051)
- [ ] T050 [P] [US2] Test `BT/m15/dinhchinh/DinhChinhIT.java`: US2 #1–8 – bản gốc không đổi; `id_ban_ghi_goc` luôn là bản gốc ban đầu khi đính chính nhiều lần; bản gốc chưa xác nhận hoặc không tồn tại bị từ chối; loại không có provider → 409 `KHONG_HO_TRO_DINH_CHINH`; `HUY_GHI_NHAN` rồi `SUA_NOI_DUNG` → 409 `BAN_GHI_DA_HUY_GHI_NHAN` (DBR-23, FR-024, FR-027, FR-029)
- [ ] T051 [P] [US2] Test `BT/m15/dinhchinh/DinhChinhDongThoiIT.java`: hai đính chính đồng thời trên cùng gốc nhận `so_thu_tu` liên tiếp, không trùng (FR-027, FR-028)
- [ ] T052 [P] [US2] Test `BT/m15/dinhchinh/DinhChinhHienHanhViewIT.java`: `v_dinh_chinh_moi_nhat` trả bản `SUA_NOI_DUNG` có `so_thu_tu` lớn nhất; `v_ban_ghi_da_huy` có thời điểm hủy (FR-029, FR-030)

### Implementation cho User Story 2

- [ ] T053 [US2] Tạo `MIG/V000_06__dinh_chinh.sql`: bảng `dinh_chinh` (`loai_ban_ghi VARCHAR(40)`, `id_ban_ghi_goc BIGINT NOT NULL`, `kieu ENUM('SUA_NOI_DUNG','HUY_GHI_NHAN')`, `noi_dung_dung JSON NULL` – "`NOT NULL` khi `SUA_NOI_DUNG` (`CHECK`)", `ly_do VARCHAR(2000) NOT NULL CHECK (CHAR_LENGTH(TRIM(ly_do)) > 0)`, `cach_tao ENUM('A','B','C')`, `yeu_cau_phe_duyet_id BIGINT NULL` – "`NOT NULL` khi `cach_tao='C'` (`CHECK`)", `so_thu_tu INT`, `UNIQUE (loai_ban_ghi, id_ban_ghi_goc, so_thu_tu)`), trigger chỉ thêm; bảng kỹ thuật `chuoi_dinh_chinh` (`so_ban`, `da_huy`, `phien_ban`); view `v_dinh_chinh_moi_nhat`, `v_ban_ghi_da_huy` (data-model mục 4)
- [ ] T054 [P] [US2] Tạo entity `BE/m15/dinhchinh/internal/DinhChinh.java` (`@NhomDuLieu(GHI_NHAN)`, repository `ChiThemRepository`) và `ChuoiDinhChinh.java`
- [ ] T055 [P] [US2] Tạo `BE/m15/dinhchinh/api/DinhChinhApi.java` (`taoDinhChinh`, `hienHanh`, `chuoi`), SPI `DinhChinhDuocProvider` (`loaiBanGhi`, `daXacNhan`, `nguoiGhiGoc`, `tangKhuVuc`, `coQuyenGhiNhan`, `kiemTraNoiDung`, `choPhep`, `sauKhiDinhChinh`) và sự kiện `DaDinhChinh` theo contracts/module-interfaces.md mục 1.6
- [ ] T056 [US2] Cài `BE/m15/dinhchinh/internal/ChonCachDinhChinh.java` theo bảng quyết định data-model mục 4, dùng `PhamViApi.nguoiPhuTrachCa`, `truongTangDuocGiao` (FR-026)
- [ ] T057 [US2] Cài `BE/m15/dinhchinh/internal/DinhChinhService.java`: kiểm `provider.daXacNhan`, khóa `chuoi_dinh_chinh` theo `phien_ban`, cấp `so_thu_tu = so_ban + 1`, ghi nhật ký `DINH_CHINH_THEM` cùng transaction, gọi `sauKhiDinhChinh`, phát `DaDinhChinh`; cách C lập và gửi duyệt yêu cầu `M15.DINH_CHINH` qua `YeuCauPheDuyetApi` (phụ thuộc T090–T092) (FR-023 → FR-030)
- [ ] T058 [US2] Cài handler `BE/m15/dinhchinh/internal/DinhChinhYeuCauHandler.java` cho loại `M15.DINH_CHINH`: người duyệt Quản lý viện; áp dụng thì thêm `dinh_chinh` với `cach_tao='C'` và `yeu_cau_phe_duyet_id` (FR-026 c; phụ thuộc T090–T092)
- [ ] T059 [US2] Tạo controller `BE/m15/dinhchinh/web/DinhChinhController.java`: `GET /dinh-chinh/{loaiBanGhi}/{idBanGhiGoc}` (chuỗi, `daDinhChinh`, giá trị hiện hành) và `POST` (201 cho A/B, 202 kèm yêu cầu cho C) theo contracts/api-000.yaml
- [ ] T060 [US2] Cài provider mẫu `BT/m15/mau/GhiNhanMauProvider.java` cho `M15.GHI_NHAN_MAU` để chạy T049–T052 và quickstart K14, K16, K17

**Checkpoint**: Đính chính chạy được trên bản ghi mẫu; module khác chỉ cần cài provider.

---

## Phase 5: User Story 3 – Mọi thay đổi quan trọng đều truy vết được qua nhật ký (Priority: P1)

**Goal**: Tra cứu nhật ký theo đối tượng, người, thời gian; che trường theo quyền; lưu giữ và loại bỏ theo chuỗi (FR-042 → FR-051a, BR-M15-11 → 13).

**Independent Test**: Sau một thay đổi nhóm 1/2/3, một lần đổi tham số, một lần đính chính, tra nhật ký theo đối tượng, người, khoảng thời gian và kiểm đủ trường mục 19.4 (quickstart K18, K19).

### Tests cho User Story 3

- [ ] T061 [P] [US3] Test `BT/m15/nhatky/NhatKyDayDuIT.java`: proxy đếm câu lệnh xác nhận mỗi lệnh thay đổi nhóm 2, 3 có đúng một `INSERT nhat_ky` cùng transaction (SC-001, DBR-23)
- [ ] T062 [P] [US3] Test `BT/m15/nhatky/NhatKyCheTruongIT.java`: Hành chính xem nhật ký hồ sơ mẫu có đổi `diaChi` và `diUng` → `diaChi` hiện giá trị, `diUng` hiện `{"daThayDoi": true}`, `lyDo` `null` và `lyDoBiAn = true`; thiếu `PhanLoaiTruongProvider` thì chỉ Quản lý viện xem (US3 #8, BR-M15-11, FR-049)
- [ ] T063 [P] [US3] Test `BT/m15/nhatky/NhatKyPhamViIT.java`: người ngoài phạm vi không thấy nhật ký đối tượng; người thân bị chặn khỏi nhật ký (FR-049, NFR-08)
- [ ] T064 [P] [US3] Test `BT/m15/nhatky/NhatKyNgoaiTuyenIT.java`: bản ghi đồng bộ ngoại tuyến lưu cả `thoi_diem` máy chủ và `thoi_diem_thiet_bi` (US3 #12, DBR-25)
- [ ] T065 [P] [US3] Test `BT/m15/nhatky/NhatKyXuatBaoCaoIT.java`: lần xuất báo cáo ghi `XUAT_BAO_CAO` với `truoc`/`sau` rỗng và bộ lọc trong `chi_tiet` (US3 #11, FR-043a)
- [ ] T066 [P] [US3] Test `BT/m15/luutru/LoaiBoDuLieuIT.java` (đồng hồ giả lập): chỉ xóa chuỗi đã hết hạn toàn bộ; chuỗi còn phần tử chưa hết hạn được giữ; mỗi chuỗi một transaction; dừng giữa chừng vẫn ghi bản tóm tắt với số đếm thực tế, `khong_loai_bo = true`; `eldercare_app` xóa thì bị trigger chặn (US3 #4, #5; BR-M15-12, BR-M15-13; FR-050, FR-051, FR-051a)

### Implementation cho User Story 3

- [ ] T067 [P] [US3] Tạo SPI `BE/m15/nhatky/api/PhanLoaiTruongProvider.java` (`loaiDoiTuong()`, `phanLoaiTruong()`; trường không khai báo → `CONG_KHAI_NOI_BO`) (contracts mục 1.3)
- [ ] T068 [US3] Cài `BE/m15/nhatky/internal/NhatKyQueryService.java`: lọc theo đối tượng, người, hành động, khoảng thời gian, `tuDuyet`; phân trang theo khóa trên chỉ mục `(doi_tuong_loai, doi_tuong_id, thoi_diem)`; che trường và che `lyDo` bằng `PhanLoaiTruongProvider` + `QuyenXemTruongApi` (FR-049, BR-M15-11, SC-006)
- [ ] T069 [US3] Tạo controller `BE/m15/nhatky/web/NhatKyController.java` cho `GET /nhat-ky` và `GET /nhat-ky/doi-tuong/{doiTuongLoai}/{doiTuongId}` theo contracts/api-000.yaml
- [ ] T070 [P] [US3] Tạo SPI `BE/m15/luutru/api/LuuTruProvider.java` (`loaiDuLieu`, `loaiHan` `THEO_HO_SO`/`KHONG_GAN_HO_SO`/`DAI_HON(MaThamSo)`, `ungVienHetHan`, `thamChieuToi`, `xoa`) (contracts mục 1.7)
- [ ] T071 [US3] Cài job `BE/m15/luutru/lich/LoaiBoDuLieuHetHan.java` chạy hằng tháng bằng datasource `eldercare_luu_tru`: tính ứng viên theo CFG-M15-03, CFG-M15-04 đọc lúc chạy, dựng chuỗi (gốc, đính chính, nhật ký, bản ghi tham chiếu), loại chuỗi còn phần tử chưa hết hạn, xóa từng chuỗi một transaction, ghi tóm tắt `LOAI_BO_DU_LIEU_HET_HAN` bằng `ghiNgoaiTransaction` (R-11)
- [ ] T072 [P] [US3] Cài `LuuTruProvider` cho `nhat_ky`, `dinh_chinh`, `yeu_cau_phe_duyet` trong `BE/m15/luutru/internal/` (nhật ký gắn hồ sơ theo `nguoi_cao_tuoi_id`, FR-050)
- [ ] T073 [P] [US3] Tạo màn tra nhật ký `frontend/src/tinh-nang/nhat-ky/NhatKyBang.jsx` (lọc đối tượng, người, thời gian; hiển thị "đã thay đổi" cho trường bị che; ngày dd/MM/yyyy) và test `frontend/src/tinh-nang/nhat-ky/NhatKyBang.test.jsx` (NFR-10, NFR-11)

**Checkpoint**: Nhật ký tra cứu, che trường và loại bỏ dữ liệu hoạt động.

---

## Phase 6: User Story 4 – Quản lý viện thay đổi tham số cấu hình (Priority: P2)

**Goal**: Đổi giá trị tham số có lý do, có hiệu lực ngay sau commit, lưu lịch sử, tra giá trị quá khứ, khôi phục mặc định (FR-011 → FR-022, BR-M15-04, DBR-24).

**Independent Test**: Đổi CFG-M05-01 hoặc CFG-M15-05, kích hoạt quy tắc trước và sau khi đổi, tra lịch sử và giá trị tại thời điểm quá khứ (quickstart K1–K5).

### Tests cho User Story 4

- [ ] T074 [P] [US4] Test đơn vị `BT/m15/thamso/KiemTraThamSoTest.java`: kiểm `kieu`, `rang_buoc`, tham chiếu chéo CFG-M15-06 > CFG-M15-05 (đổi mã nào cũng kiểm mã kia) → 422 `GIA_TRI_KHONG_HOP_LE` (FR-015)
- [ ] T075 [P] [US4] Test `BT/m15/thamso/ThamSoIT.java`: đổi giá trị → `tham_so.gia_tri_hien_hanh` bằng dòng lịch sử mới nhất; nhật ký `THAM_SO_DOI` có trước/sau; `If-Match` cũ → 412 kèm `giaTriHienHanh`; khôi phục mặc định có `la_khoi_phuc_mac_dinh` (US4 #1–3, #9; DBR-24, FR-020, FR-021)
- [ ] T076 [P] [US4] Test `BT/m15/thamso/ThamSoQuyenIT.java`: chỉ AC-01 đổi được; Điều dưỡng → 403 (US4 #6, BR-M15-04)
- [ ] T077 [P] [US4] Test `BT/m15/thamso/ThamSoBoDemSauCommitIT.java`: quy tắc chạy ngay sau commit dùng giá trị mới, không khởi động lại (SC-004, FR-016)
- [ ] T078 [P] [US4] Test `BT/m15/thamso/ThamSoTaiThoiDiemIT.java` và `ThamSoBangIT.java`: `docTaiThoiDiem` trả giá trị đúng thời điểm quá khứ; tham số dạng `BANG` đổi một dòng ghi đủ nội dung trước/sau (US4 #8, SC-005, FR-019, FR-022)

### Implementation cho User Story 4

- [ ] T079 [US4] Cài `BE/m15/thamso/internal/KiemTraThamSo.java` và lệnh `doiGiaTri` trong `ThamSoService.java`: `UPDATE tham_so … WHERE ma=? AND phien_ban=?` → 0 dòng thì 412; `INSERT lich_su_tham_so`; `INSERT nhat_ky` cùng transaction; khóa quyền `THAM_SO_DOI` qua `QuyenApi`; phát `ThamSoDaDoi` và xóa bộ đệm ở pha `AFTER_COMMIT` (FR-014 → FR-018, FR-020, FR-021)
- [ ] T080 [US4] Tạo controller `BE/m15/thamso/web/ThamSoController.java` cho `GET /tham-so`, `GET /tham-so/{ma}` (ETag), `POST /tham-so/{ma}/doi-gia-tri`, `GET /tham-so/{ma}/lich-su?taiThoiDiem=` theo contracts/api-000.yaml; không có API tạo hay xóa mã (FR-011)
- [ ] T081 [P] [US4] Tạo màn `frontend/src/tinh-nang/tham-so/ThamSoDanhSach.jsx`, `ThamSoForm.jsx` (lý do bắt buộc; 412 hiện giá trị mới), `ThamSoLichSu.jsx`, và test `frontend/src/tinh-nang/tham-so/ThamSoForm.test.jsx`

**Checkpoint**: Tham số đổi được qua màn hình, có hiệu lực ngay.

---

## Phase 7: User Story 5 – Yêu cầu phê duyệt dùng chung (Priority: P2)

**Goal**: Vòng đời Nháp → Chờ duyệt → Đã duyệt → Đã áp dụng / Áp dụng không thành / Từ chối / Hủy; áp dụng đúng một lần; nhắc và báo Quản lý viện; SPI cho module (FR-031 → FR-041, BR-M15-08 → 10).

**Independent Test**: Dùng loại mẫu `M15.MAU` đi hết các nhánh; chạy job áp dụng lặp lại không tạo tác động lần hai (quickstart K6–K13).

### Tests cho User Story 5

- [ ] T082 [P] [US5] Test đơn vị `BT/m15/pheduyet/BangChuyenTrangThaiTest.java`: mọi cặp (trạng thái, lệnh) của bảng chuyển data-model mục 3, gồm chuyển bị cấm; `huy` thiếu `lyDo` bị từ chối (BR-M15-08; tên test `YeuCauHuyTest` gộp ở đây)
- [ ] T083 [P] [US5] Test `BT/m15/pheduyet/YeuCauVongDoiIT.java`: US5 #1–6 – lập, sửa nháp có lý do, gửi duyệt khóa nội dung, duyệt, từ chối bắt buộc ý kiến, hủy (FR-031 → FR-036)
- [ ] T084 [P] [US5] Test `BT/m15/pheduyet/ApDungMotLanIT.java`: chạy job áp dụng 3 lần → đúng 1 dòng `ket_qua_ap_dung` (US5 #7, #11, SC-003, FR-037)
- [ ] T085 [P] [US5] Test `BT/m15/pheduyet/ApDungBuIT.java`: Bộ lập lịch gián đoạn qua ngày hiệu lực → áp dụng theo thứ tự `(ngay_hieu_luc_thuc_te, thoi_diem_quyet_dinh)`, `apDungBu = true` (US5 #9, BR-M15-10, FR-037a)
- [ ] T086 [P] [US5] Test `BT/m15/pheduyet/ApDungLoiIT.java`: điều kiện không thỏa → `AP_DUNG_KHONG_THANH` và sự kiện báo người duyệt, người yêu cầu; lỗi khi áp dụng → vẫn `DA_DUYET`, nhật ký `YC_AP_DUNG_LOI` ghi transaction riêng, lần sau áp dụng được (US5 #8, #10; FR-037b, FR-038)
- [ ] T087 [P] [US5] Test `BT/m15/pheduyet/NhacYeuCauIT.java` (đồng hồ giả lập): quá CFG-M15-05 → `YeuCauCanNhacNguoiDuyet` một lần; quá CFG-M15-06 → `YeuCauCanBaoQuanLyVien`; không còn người duyệt → báo Quản lý viện ngay; không tự hủy; danh sách người duyệt trong sự kiện đã lọc theo `gioiHanVaiTroDuyet` (bổ sung theo plan 004) (US5 #4, BR-M15-09, FR-031a)
- [ ] T088 [P] [US5] Test `BT/m15/pheduyet/QuyetDinhDongThoiIT.java` và `TuDuyetIT.java`: hai quyết định cùng `If-Match` → một 200, một 409 `DA_DUOC_XU_LY`; tự duyệt thiếu ý kiến bị từ chối, có ý kiến thì 200 và nhật ký có `tuDuyet`; handler `choTuDuyet()=false` chặn tự duyệt (US5 #12, #13; FR-035, FR-040)
- [ ] T105 [P] [US5] Test `BT/m15/pheduyet/GioiHanVaiTroDuyetIT.java` với handler mẫu bật `gioiHanVaiTroDuyet = {QUAN_LY_VIEN}`: tài khoản có `quyenDuyet()` nhưng không có vai trò trong tập bị 403 khi Duyệt, Từ chối và tự duyệt, `lenhDuocPhep` không có `duyet`, `tu-choi`; danh sách người duyệt của `YeuCauDaGuiDuyet`, `YeuCauCanNhacNguoiDuyet` không có tài khoản đó; khi chỉ còn tài khoản đó thì `conNguoiDuyet = false` và báo Quản lý viện (BR-M15-09); người duyệt đều là Quản lý viện thì quá CFG-M15-06 không phát `YeuCauCanBaoQuanLyVien` (FR-031a); tắt giới hạn thì hành vi như cũ; giới hạn tính lại ở mỗi lần xét (bổ sung theo plan 004, Q-338; FR-034, FR-034a, FR-035, FR-031a)

### Implementation cho User Story 5

- [ ] T089 [US5] Tạo `MIG/V000_07__yeu_cau_phe_duyet.sql`: bảng `yeu_cau_phe_duyet` theo data-model mục 3 (`ma_hien_thi VARCHAR(20) UNIQUE`, `loai VARCHAR(40)`, `trang_thai VARCHAR(30)`, `noi_dung JSON`, `ly_do VARCHAR(2000) NOT NULL`, `ngay_hieu_luc_mong_muon DATE`, `ngay_hieu_luc_thuc_te DATE NULL`, `thoi_diem_gui_duyet`, `nguoi_quyet_dinh_id`, `thoi_diem_quyet_dinh`, `y_kien_quyet_dinh`, `tu_duyet BOOLEAN`, `ly_do_ket_thuc VARCHAR(2000) NULL`, `thoi_diem_ap_dung`, `ap_dung_bu BOOLEAN`, `da_nhac_luc`, `da_bao_qlv_luc`, `phien_ban INT`; bốn chỉ mục ở data-model) và `ket_qua_ap_dung` (khóa chính = `yeu_cau_phe_duyet_id`, `ket_qua_loai`, `ket_qua_id`, `thoi_diem`; trigger chỉ thêm)
- [ ] T090 [P] [US5] Tạo `BE/m15/pheduyet/api/YeuCauPheDuyetApi.java` (`lap`, `xem`, `ketQuaCua`, `chuyenMoRong`, `dongDoTrangThaiCuoi`), SPI `LoaiYeuCauHandler` (đủ phương thức mặc định `choTuDuyet`, `thamSoNhacRieng`, `chuyenMoRong`, `kiemTraKhiDuyet`, `khiDuyet`, `gioiHanVaiTroDuyet` (bổ sung theo plan 004)), `NhacQuaHan.kiemTra(...)` và các sự kiện `YeuCauDaLap`, `YeuCauDaGuiDuyet`, `YeuCauCanNhacNguoiDuyet`, `YeuCauCanBaoQuanLyVien`, `YeuCauDaQuyetDinh`, `YeuCauDaApDung`, `YeuCauApDungKhongThanh`, `YeuCauDaHuy` (cài `CoYeuCauThongBao`) theo contracts/module-interfaces.md mục 1.5
- [ ] T091 [US5] Cài máy trạng thái `BE/m15/pheduyet/internal/BangChuyenYeuCau.java` gồm bảng chuyển chung và cơ chế `BangChuyenMoRong` (chỗ cho `DUOC_THAY_THE` của 004 và các trạng thái của 015) không bỏ kiểm tra chung (lý do, nhật ký, khóa lạc quan, quyền tại lúc quyết định)
- [ ] T092 [US5] Cài `BE/m15/pheduyet/internal/YeuCauPheDuyetService.java`: lập, sửa nháp, gửi duyệt, duyệt (quyền, phạm vi và `gioiHanVaiTroDuyet` xét tại lúc quyết định, áp cho cả Từ chối và tự duyệt; `kiemTraKhiDuyet`, `khiDuyet` MANDATORY, `ngay_hieu_luc_thuc_te = max(mong muốn, ngày duyệt)`, ≤ hôm nay thì áp dụng ngay ở transaction riêng sau commit), từ chối, hủy, `dongDoTrangThaiCuoi`, `chuyenMoRong`; mỗi lệnh ghi nhật ký `YC_*` (FR-031 → FR-041)
- [ ] T093 [US5] Cài job `BE/m15/pheduyet/lich/ApDungYeuCauDenHan.java` (15 phút một lần, 00:05 và lúc khởi động; ShedLock): lấy `DA_DUYET` đến hạn theo thứ tự `(ngay_hieu_luc_thuc_te, thoi_diem_quyet_dinh)`, mỗi yêu cầu một transaction, `INSERT ket_qua_ap_dung` chống áp dụng hai lần, `canCu` có `lanChayLichId` (BR-M15-10, FR-037, FR-037a, FR-037b)
- [ ] T094 [US5] Cài job `BE/m15/pheduyet/lich/NhacYeuCauChoDuyet.java` và `BE/m15/pheduyet/internal/NhacQuaHanImpl.java`: mốc CFG-M15-05, CFG-M15-06 (hoặc `thamSoNhacRieng()`); danh sách người duyệt, `conNguoiDuyet` và điều kiện "người duyệt không phải Quản lý viện" của mốc CFG-M15-06 tính theo contracts mục 1.5 "Danh sách người duyệt" (lọc `gioiHanVaiTroDuyet`); đặt `da_nhac_luc`, `da_bao_qlv_luc`, ghi `YC_NHAC`, `YC_BAO_QLV` có căn cứ CFG (BR-M15-09, FR-031a)
- [ ] T095 [US5] Tạo controller `BE/m15/pheduyet/web/YeuCauPheDuyetController.java`: `POST/GET /yeu-cau-phe-duyet`, `GET /yeu-cau-phe-duyet/{id}` (kèm `lenhDuocPhep`, tính cùng điều kiện quyền, phạm vi, `gioiHanVaiTroDuyet` như lệnh), `POST …/{id}/sua-noi-dung`, `…/gui-duyet`, `…/duyet`, `…/tu-choi`, `…/huy` với `If-Match` theo contracts/api-000.yaml
- [ ] T096 [P] [US5] Cài handler mẫu `BT/m15/mau/YeuCauMauHandler.java` cho `M15.MAU` (bật/tắt "điều kiện không thỏa", "lỗi khi áp dụng", "giới hạn vai trò duyệt") dùng cho T083–T088, T105 và quickstart K6–K13, K21
- [ ] T097 [P] [US5] Tạo hộp duyệt web `frontend/src/tinh-nang/yeu-cau-phe-duyet/HopDuyet.jsx`, `ChiTietYeuCau.jsx` (nút theo `lenhDuocPhep`, ý kiến bắt buộc khi từ chối hoặc tự duyệt, xử lý 409/412) và test `HopDuyet.test.jsx`
- [ ] T098 [P] [US5] Tạo hộp duyệt app nhân viên `mobile/lib/nhan-vien/yeu-cau-phe-duyet/hop_duyet.dart` (chỉ trực tuyến, R-13) và test `mobile/test/nhan-vien/hop_duyet_test.dart`

**Checkpoint**: Mọi user story của 000 chạy độc lập; module khác đăng ký handler và provider.

---

## Phase 8: Polish & Cross-Cutting

- [ ] T099 [P] Xử lý chung trên web `frontend/src/chung/loi.js`: hiển thị Problem Details, `dieuKienChuaThoa[]`, 412 hiện giá trị mới; định dạng ngày dd/MM/yyyy, giao diện tiếng Việt (NFR-11)
- [ ] T100 [P] Hàng đợi ngoại tuyến dùng chung `mobile/lib/chung/ngoai_tuyen/hang_doi.dart` (drift): phong bì `Idempotency-Key`, `thoiDiemThietBi`, nhận `KetQuaDongBo`; chi tiết quy tắc ở plan 002, 005 (R-13)
- [ ] T101 [P] Công cụ sinh dữ liệu quy mô NFR-01 với 1 năm nhật ký (~22 triệu dòng) `backend/tools/sinh-du-lieu/` để đo SC-006 (quickstart mục 5)
- [ ] T102 Viết quy trình vận hành `backend/docs/van-hanh.md`: sao lưu hằng ngày giữ 30 ngày, bản khôi phục không làm sống lại dữ liệu đã loại bỏ, bảo trì ngoài khung giao ca (NFR-05, NFR-06)
- [ ] T103 Chạy `./mvnw verify` và toàn bộ quickstart.md mục 3 (K1–K20) ở profile `demo`; đo SC-006, SC-007 theo quickstart mục 5
- [ ] T104 Rà truy vết: mỗi BR-M15-03, 04, 06 → 13 và DBR-23, 24, 25 có test theo bảng data-model mục 7; cập nhật bảng nếu tên lớp test đổi; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-010a — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: không phụ thuộc
- **Foundational (Phase 2)**: sau Setup; chặn mọi user story của 000 và mọi feature khác
- **US1 (Phase 3)**: sau Phase 2
- **US2 (Phase 4)**: sau Phase 2 và US1 (dùng mẫu nhóm 3, T045, T048); cách C (T057, T058) cần T090–T092 của US5
- **US3 (Phase 5)**: sau Phase 2; test đầy đủ cần dữ liệu do US1, US2, US4 sinh
- **US4 (Phase 6)**: sau Phase 2 (bảng và đọc tham số đã có ở T029–T032)
- **US5 (Phase 7)**: sau Phase 2 và US4 cho mốc CFG-M15-05/06 đổi được (đọc mặc định thì không cần US4)
- **Polish (Phase 8)**: sau các user story cần thiết

### Phụ thuộc với feature khác

- 002 thay bản giả T025 bằng bản thật; mọi feature 001–019 dùng `common`, `NhatKyApi`, `ThamSoApi`, `YeuCauPheDuyetApi`, `DinhChinhApi`, `LuuTruProvider`.
- 009 lắng nghe sự kiện của US5; khi 009 chưa có, sự kiện vẫn nằm ở `event_publication`.

### Within Each User Story

- Test viết trước và phải lỗi trước khi cài
- Migration → entity → service → controller → client

### Parallel Opportunities

- Phase 1: T003–T010 song song sau T002
- Phase 2: T011–T014, T015, T017, T018, T021–T023, T025, T033–T038 song song
- Mỗi story: mọi test [P] song song; US3 và US4 làm song song được sau Phase 2

---

## Parallel Example: User Story 5

```text
Task: "T082 BangChuyenTrangThaiTest"
Task: "T083 YeuCauVongDoiIT"
Task: "T084 ApDungMotLanIT"
Task: "T085 ApDungBuIT"
Task: "T087 NhacYeuCauIT"
# sau khi có T089–T092:
Task: "T097 HopDuyet.jsx"
Task: "T098 hop_duyet.dart"
```

---

## Implementation Strategy

### MVP First

1. Phase 1 + Phase 2 (nền cho toàn hệ thống)
2. Phase 3 (US1): khung nhóm dữ liệu
3. **Dừng và kiểm chứng**: K15, K20; `NhomGhiNhanCoTriggerIT` xanh
4. Chuyển sang plan 002 để thay bản giả quyền

### Incremental Delivery

1. Nền + US1 → US3 (nhật ký tra cứu) → US2 (đính chính A/B) → US4 (tham số) → US5 (phê duyệt) → cách C của US2
2. Mỗi bước chạy lại `./mvnw verify` và kịch bản quickstart tương ứng

### Parallel Team Strategy

- Một người làm `common` + nhật ký, một người làm tham số, một người làm phê duyệt sau Phase 2; web và app làm song song khi controller đã có.

---

## Notes

- [P] = khác file, không phụ thuộc task chưa xong
- Mọi task tham chiếu FR/BR để giữ truy vết (constitution XV)
- Không viết cứng mốc nghiệp vụ; đọc `ThamSoApi` (constitution XIII)
- Hai điểm báo lại ở plan (khoảng hợp lệ tham số; BR-M15-07 và FR-016) không chặn task; T031 và T079 theo cách plan đã chọn
