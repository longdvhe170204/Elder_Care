---
description: "Danh sách task triển khai feature 014 – Hoạt động và chất lượng chăm sóc"
---

# Tasks: Quản lý hoạt động và chất lượng chăm sóc (014)

**Input**: Tài liệu thiết kế ở `specs/014-activity-care-quality/` (plan.md, spec.md, research.md L-01 → L-15, data-model.md, contracts/api-014.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 005 đã xong (014 thêm gói con vào `care`, dùng dịch vụ nội bộ công việc, có mặt bán trú, công việc làm lại); 001 (`TrangThaiNguoiCaoTuoiApi` cho điểm danh rời viện/về), 003 (lịch xe ở `roombedallocation.api` do 019 cung cấp), 004, 006 (`MangThuocChuyenDiApi`, SPI `ChuyenDiThuocPort`), 007 (`SuCoLenhApi`, `KhoanhVungApi`, `DiemDanhTruyVetPort`), 008 (`CaTrucApi`, `ChuyenDiNhiemVuPort`), 010 (`SuKienTinhPhiNguon`), 011 (`ChuyenDiBuaAnPort`), 012 (`QuyenNguoiThanApi`) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/eldercare/backend/`, `BT/` = `backend/src/test/java/com/eldercare/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M04 (phần 014): `BE/care/{hoatdong,buoi,chuyendi,tinhthan,kiemtra}/`; bổ sung `BE/care/api/`
- Web `frontend/src/tinh-nang/hoat-dong/{danh-muc,buoi,chuyen-di,tinh-than,kiem-tra-chat-luong}/`, `frontend/src/tinh-nang/cong-nguoi-than/hoat-dong/`; app nhân viên `mobile/lib/nhan-vien/hoat-dong/`; app người thân `mobile/lib/nguoi-than/hoat-dong/`

---

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; 009 FR-001a, Q-363): sửa T002, T003, T021, T022. Mã task cũ giữ nguyên.

## Phase 1: Setup

- [x] T001 Tạo năm gói con `BE/care/{hoatdong,buoi,chuyendi,tinhthan,kiemtra}/`; ArchUnit `BT/care/M04HoatDongArchTest.java`: gói `..lich..` không tạo buổi, danh sách, mục, công việc làm lại, sự cố (Q-306, Q-307) (research L-01)
  - *(2026-11-05, C)* Gói `care/internal/{activity, session, trip, wellbeing, qualitycheck}` (package-info), job ở `care/scheduling` có sẵn; luật `M04ArchitectureRules.NO_JOB_CREATES_ACTIVITY_RECORDS`, test `M04ActivityArchTest` (3 ca, có bytecode cố ý vi phạm).

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Giao diện mới ở `care.api`, danh mục, hoạt động, mẫu lặp, khung job, listener.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T002 [P] Bổ sung `BE/care/api/`: `ChuyenDiNhiemVuLenhApi`, `ChuyenDiThayDoi`, `HoatDongThongKeApi`, sự kiện `BuoiDaHuy`, `BuoiDaDoi`, `BuoiDaKetThuc`; khai báo sự kiện phát cho module khác (`incident.api.YeuCauTaoCanhBao`, `YeuCauGhiDienBienCanhBao`, `billing.api.SuKienTinhPhiNguon`, `nutrition.api.ChuyenDiDaDoi`) (contracts mục 1.2, 1.3; research L-11); bổ sung 2026-10-18 (009 FR-001a): sự kiện có người nhận là người thân khai `doiTuongThongTin` theo research L-13, test hợp đồng kiểm mọi yêu cầu tới người thân có đối tượng
  - *(2026-11-05, C)* Chủ dự án duyệt đặt sự kiện 014 ở `care.api.events` thay `incident.api`, `nutrition.api` (care không được phụ thuộc incident, luật C-02, cấm vòng): `AlertProgressRequested`, `TripChanged` (gồm ChuyenDiDaDoi cho 011), `ActivitySessionCancelled`, `ActivitySessionChanged`, `ActivitySessionEnded`; cảnh báo dùng lại `care.api.events.AlertRequested`; tính phí phát thẳng `billing.api.events.CostSourceBilled`, `CostSourceChanged` của 010 (billing không phụ thuộc care, không vòng). `TripDutyCommandApi`, `ActivityStatisticsApi` + DTO; bản giả ở `FakeM04ApiConfig`. Đối tượng thông tin người thân do `NotificationRequest` chung bắt buộc; thông báo người thân của 014 khai ở US2. Test `T002_ActivityApiContractTest` 4/4.
- [ ] T003 Tạo `MIG/V014_01__hoat_dong.sql`: `hoat_dong_nct` (`nct_id` khóa chính), `loai_hoat_dong` (`hoat_dong_nhom`, `ngoai_vien`), `nhom_so_thich`, `loai_hoat_dong_nhom_so_thich`, `hoat_dong` (`pham_vi` `TANG|TOAN_VIEN`; giới hạn số lượng; thu phí – dịch vụ 004), `mau_lap_phien_ban` (có lịch sử), `nguoi_thuong_xuyen` (cột sinh `UNIQUE` bản đang hiệu lực), `su_kien_m04hd_da_xu_ly`, `lan_chay_m04hd` (data-model mục 2) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `hoat_dong_nct` → `trang_thai_nct`; `loai_hoat_dong`, `nhom_so_thich`, `loai_hoat_dong_nhom_so_thich`, `hoat_dong` → `danh_muc_chung`; `mau_lap_phien_ban`, `nguoi_thuong_xuyen` → `ho_so_chung`; `su_kien_m04hd_da_xu_ly`, `lan_chay_m04hd` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *(2026-11-05, C, một phần)* Không cần file migration: mọi bảng của task gộp vào bảng chung, loại khai bằng mã ở `ActivityKinds` (`M04.ACTIVITY_TYPE`, `M04.INTEREST_GROUP` ở `catalog_item`; `M04.ACTIVITY`, `M04.TEMPLATE_VERSION`, `M04.REGULAR_PARTICIPANT` ở `simple_record`). Lệch kỹ thuật: hoạt động ở `simple_record` thay `catalog_item` vì `catalog_item` buộc tên Hiệu lực duy nhất theo loại (plan Phase 2). Còn: `hoat_dong_nct` → `trang_thai_nct` (khóa sở thích, US6), dấu job ở `reminder_marker` (T005).
- [ ] T004 Cài danh mục và hoạt động (Trưởng tầng lập; mẫu đổi theo phiên bản) trong `BE/care/hoatdong/internal/HoatDongService.java` và controller `/hoat-dong/loai`, `/hoat-dong`, `/{id}`, `/{id}/{lenh}` (research L-04)
  - *(2026-11-05, C, một phần)* Đã có `ActivityService`, `ActivityController`: `GET`, `POST /activities`, `PUT /activities/{id}`, `POST /activities/{id}/{command}` (`deactivate`, `change-template`, `add-regular-participant`, `remove-regular-participant`; `create-sessions-from-template` trả 503 tới US1); test `FR_002_ActivityDeclarationIT` 9/9. Còn: `/activity-types` chờ quyền danh mục (team-plan mục 8), hoạt động toàn viện (Target cần tầng từ địa điểm, 003), kiểm địa điểm theo cấu trúc 003 và vai trò người phụ trách (FR-002a), tác động lên buổi (US1, US2).
- [x] T005 Cài khung job `BE/care/hoatdong/lich/ViecTheoPhutM04HoatDong.java`, `ViecHangNgayM04HoatDong.java` (00:00; ShedLock, chạy bù) và test `BT/care/ChayBuM04HoatDongIT.java` (research L-15)
  - *(2026-11-05, C)* Không thêm job riêng: việc của 014 là bước `CareMinuteWork`/`CareDailyWork` trong khung `CARE_MINUTE`/`CARE_DAILY` của 005 (ShedLock, chạy bù lúc khởi động, mốc (việc, ngày) `M04.DAILY_WORK` ở `reminder_marker` thay `lan_chay_m04hd`); `ActivityScheduling` (tiền tố `M04HD_`, ngày cần xử lý hết ngày). Test `ChayBuM04HoatDongIT` đỏ 2/2 rồi xanh. Bước cụ thể (hết ngày, nhắc) cài ở story (plan Phase 2).
- [ ] T006 [P] Cài provider: `GanPhamViProvider` (`M04.BUOI`, `M04.DANG_KY`, `M04.CHUYEN_DI`, `M04.SO_THICH`, `M04.KIEM_TRA_CHAT_LUONG`), `PhanLoaiTruongProvider`, `LuuTruProvider`; `ThoiKhoaBieuNguonPort` (005) cho buổi, chuyến trong ngày
  - *(2026-11-05, C, một phần)* Đã có: phạm vi `M04.ACTIVITY_SESSION`, `M04.ACTIVITY_REGISTRATION`, `M04.TRIP`, `M04.PREFERENCE`, `M04.QUALITY_CHECK` (`CareGovernanceStore`), phân loại `M04.ACTIVITY_REGISTRATION.acknowledgedWarnings` sức khỏe (FR-018), lưu giữ `M04.ACTIVITY_SESSION` (không gắn hồ sơ), `M04.ACTIVITY_REGISTRATION` (theo hồ sơ), `ActivityTimetableSource`; test `T006_ActivityGovernanceIT` đỏ 5/5 rồi xanh. Còn: nhánh đọc bảng `activity_session`, `activity_registration` mới chạy với bảng vắng; test lại khi US1 tạo bảng (T008); phân loại trường của điểm danh, sở thích, kết quả kiểm tra khi story khai nội dung.

**Checkpoint**: Nền 014 sẵn sàng.

---

## Phase 3: User Story 1 – Khai báo hoạt động định kỳ và tạo buổi từ mẫu (Priority: P1) 🎯 MVP

**Goal**: Trưởng tầng tạo buổi từ mẫu bằng lệnh (kèm đăng ký sẵn người thường xuyên); buổi lẻ; dời; hủy; đổi mẫu hủy buổi tương lai; `UNIQUE (hoat_dong_id, bat_dau)` (FR-001 → FR-014a, BR-M04-21).

**Independent Test**: "Thể dục buổi sáng" 08:00 thứ 2 → thứ 6, tối đa 20 người, 3 người thường xuyên; Trưởng tầng tạo buổi 7 ngày tới → buổi đúng ngày, người thường xuyên đã đăng ký; không tạo trùng.

### Tests cho User Story 1

- [x] T007 [P] [US1] Test đơn vị `BT/care/BuoiTheoMauTest.java` và `BT/care/BR_M04_21_TaoBuoiTuMauIT.java`, `FR_014a_HetNgayIT.java`: không tự tạo buổi; tạo lại không trùng; hết ngày → `DA_DIEM_DANH` (dấu) hoặc `KHONG_DIEM_DANH`, đăng ký chưa ghi → "Không ghi nhận" (Q1 → Q5)
  - *(2026-11-05, C)* `BuoiTheoMauTest` (5 ca, hàm `SessionTemplate.slots`), `BR_M04_21_TaoBuoiTuMauIT` (9 ca: tạo từ mẫu kèm đăng ký sẵn, tạo lại không trùng, buổi đã hủy không tạo lại, ngày đầu trước hôm nay 422, người thường xuyên không đạt điều kiện bị bỏ và báo một lần, đổi mẫu, ngừng, thêm/bỏ người thường xuyên, buổi lẻ, dời, hủy cần lý do, không job nào tạo buổi), `FR_014a_HetNgayIT` (2 ca). Đỏ 4/5 và 11/11 rồi xanh.

### Implementation cho User Story 1

- [x] T008 [US1] Tạo bảng `buoi_hoat_dong` (`UNIQUE (hoat_dong_id, bat_dau)` kể cả Đã hủy; `nguon` `MAU|LE`; `trang_thai` `DA_LEN_LICH|DA_DIEM_DANH|KHONG_DIEM_DANH|DA_HUY`), `dang_ky_buoi` (`nguon` `THUONG_XUYEN|THEM_TAY|THEM_LUC_DIEM_DANH`; `trang_thai` `DA_DANG_KY|DA_HUY|DA_DIEM_DANH`) trong `MIG/V014_02__buoi.sql` **Theo R-15 (data-model mục 0; bổ sung 2026-10-30): giữ `buoi_hoat_dong`, `dang_ky_buoi` (bảng đích); cột của `chuyen_di` (1–1) thêm vào `buoi_hoat_dong` ở T016, người tham gia chuyến vào `dang_ky_buoi`.**
  - *(2026-11-05, C)* `V014_02__activity_session.sql`: `activity_session` (`UNIQUE (activity_id, starts_at)` kể cả Đã hủy), `activity_registration` (cột sinh `active_key` `UNIQUE` cho đăng ký chưa hủy), GRANT cho `eldercare_app`, `eldercare_retention`. Tên dịch ở glossary 20.6.
- [x] T009 [US1] Cài `BE/care/buoi/internal/BuoiService.java` (tạo từ mẫu kèm đăng ký sẵn, buổi lẻ, dời, hủy; phát `BuoiDaHuy`, `BuoiDaDoi` cho phần công việc "hoạt động" của 005; việc hằng ngày hết ngày) và endpoint `/buoi`, `/buoi/{id}`, `/buoi/{id}/{lenh}`; màn web `frontend/src/tinh-nang/hoat-dong/buoi/` (research L-04)
  - *(2026-11-05, C, một phần)* Backend xong: `ActivitySessionService` (tạo từ mẫu kèm đăng ký sẵn, buổi lẻ, dời, hủy; tác động của ngừng, đổi mẫu, thêm/bỏ người thường xuyên cùng lệnh của hoạt động), `SessionDayEnd` (bước `M04HD_SESSION_DAY_END` của `CARE_DAILY`), `ActivitySessionController` (`GET`, `POST /activity-sessions`, `GET /activity-sessions/{id}`, `POST /activity-sessions/{id}/{command}`); sự kiện `ActivitySessionCancelled`, `ActivitySessionChanged`, `ActivitySessionEnded`, thông báo `RegularParticipantsNotRegistered`, `ActivitySessionsCancelledByTemplateChange`, `ActivitySessionDayEnded`. Còn: màn web `features/activities/` chờ duyệt bố cục (DESIGN.md mục 13 điều 2); `complete-attendance` 503 tới US3, thân có `trip` 503 tới US4; điều kiện đăng ký sẵn mới có (a), (d), trùng giờ (FR-016), phần còn lại ở US2 (plan Phase 3).
  - *(2026-11-05, C)* Web xong (bố cục duyệt 05/11): `frontend/src/features/activities/` (`ActivitySessionsPage`, `ActivitySessionDetail`, `ActivitySessionForms`, `ActivityField`, `sessionView`), mục menu `activity-sessions`, tông trạng thái ở `StatusBadge` và DESIGN.md 6.8.2. Test `ActivitySessionsPage.test.jsx` (8 ca), `sessionView.test.js` (4 ca) đỏ rồi xanh; `npm test` 497/497, lint, build qua. Phần chờ US2, US3, US4 ghi ở dòng trên.

**Checkpoint**: Buổi từ mẫu chạy độc lập.

---

## Phase 4: User Story 2 – Đăng ký có giới hạn số lượng và điều kiện sức khỏe (Priority: P1)

**Goal**: `DieuKienDangKy` (sức chứa dưới khóa buổi, khoanh vùng, nghi nhiễm, vắng mặt); đồng ý về muộn cho bán trú dời giờ về (005 FR-018a); hủy tự động khi khoanh vùng, nghi nhiễm, trạng thái cuối (FR-015 → FR-021, BR-M04-15).

**Independent Test**: Buổi tối đa 2: A, B rồi C (bị chặn); D nghi nhiễm vào hoạt động nhóm (bị chặn); khoanh vùng tầng → hủy đăng ký.

### Tests cho User Story 2

- [ ] T010 [P] [US2] Test đơn vị `BT/care/DieuKienDangKyTest.java` và `BT/care/BR_M04_15_DangKyIT.java` (hai đăng ký chỗ cuối đồng thời), `FR_016a_DongYVeMuonIT.java`, `FR_021_KhoanhVungIT.java` (Q6 → Q12)
  - *(2026-11-05, C, một phần)* `DieuKienDangKyTest` (9 ca, hàm thuần `RegistrationConditions`; viết trước nhưng chạy lần đầu khi hàm đã có, nên không thấy đỏ), `BR_M04_15_DangKyIT` (6 ca: chỗ cuối, hai đăng ký đồng thời, nghi nhiễm, cảnh báo cần xác nhận, hủy cần lý do, trạng thái cuối), `FR_021_KhoanhVungIT` (3 ca: khoanh vùng, nghi nhiễm, trạng thái cuối) đỏ 9/9 rồi xanh. Còn `FR_016a_DongYVeMuonIT`: đồng ý về muộn chờ quyền (team-plan mục 8).

### Implementation cho User Story 2

- [ ] T011 [US2] Tạo bảng `dong_y_ve_muon` (`cach` `CONG|BAN_KY`; `trang_thai` `HIEU_LUC|DA_RUT|...`) trong `MIG/V014_03__dong_y_ve_muon.sql`; cài `BE/care/buoi/internal/DangKyService.java` (đăng ký, hủy, hủy tự động; listener `KhoanhVungDaTao`, `DauNghiNhiemDaDoi`) và `DongYVeMuonService.java` (người đại diện qua `QuyenNguoiThanApi`; dời `gio_ve_du_kien_hien_hanh` của 005); bean `ThamGiaChuyenTrangThai` (`@Order` sau 005; trạng thái cuối hủy đăng ký, gỡ thường xuyên) (research L-05) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `dong_y_ve_muon` → `ho_so_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *(2026-11-05, C, một phần)* Đã có: `RegistrationConditions`, `RegistrationEligibility`, `RegistrationService` (đăng ký dưới khóa dòng buổi, hủy), `RegistrationAutoCancels` (cài `care.api.ActivityRegistrationAutoCancelApi` cho 007 gọi), `ActivityStatusParticipant` (`@Order(31)`; Kết thúc lưu trú, Ghi nhận qua đời, Hủy tiếp nhận). Nguồn 007 qua SPI `care.api.spi.ActivityHealthGatePort` thay listener `KhoanhVungDaTao`, `DauNghiNhiemDaDoi` (chủ dự án duyệt 05/11; C-02). Còn: đồng ý về muộn (`M04.LATE_RETURN_CONSENT` ở `simple_record`, `DongYVeMuonService`, dời giờ về qua `DayCareAttendanceApi.changeReturnTime`, chuyển Đã dùng lúc bắt đầu) chờ quyền của Hành chính và Người thân (Phụ lục 27 không có dòng; team-plan mục 8).
- [ ] T012 [US2] Tạo endpoint `/buoi/{id}/dang-ky`, `/dang-ky/{id}/huy`, `/dong-y-ve-muon`, `/{id}/rut`, `/cong/dong-y-ve-muon`, `/{id}/rut`; màn web đăng ký (cảnh báo cần xác nhận) với test Vitest; app người thân đồng ý về muộn
  - *(2026-11-05, C, một phần)* Đã có `POST /activity-sessions/{id}/registrations`, `POST /activity-registrations/{id}/cancel`; web: khối "Đăng ký người cao tuổi" trong khung chi tiết buổi (cảnh báo cần xác nhận, điều kiện chưa thỏa), nút "Hủy đăng ký" mỗi dòng (bố cục duyệt 05/11); test Vitest 3 ca mới đỏ rồi xanh. Còn: `/late-return-consents`, `/{id}/withdraw`, `/family-portal/late-return-consents` (chờ quyền), app người thân (task mobile).

**Checkpoint**: Đăng ký chạy độc lập.

---

## Phase 5: User Story 3 – Điểm danh buổi; lượt có mặt có thu phí là sự kiện tính phí (Priority: P1)

**Goal**: Điểm danh Có mặt/Vắng/Không ghi nhận; thêm người tại chỗ; ngoại tuyến; đính chính; sự kiện tính phí `DIEM_DANH_HOAT_DONG` cho 010; nhắc điểm danh CFG-M04-06 (FR-022 → FR-032, BR-M04-18).

**Independent Test**: "Tập đàn" thu phí 4 đăng ký; điểm danh 3 có mặt, 1 vắng; thêm 1 người tại chỗ → 4 sự kiện tính phí; đính chính một người từ có mặt sang vắng → sự kiện đổi.

### Tests cho User Story 3

- [ ] T013 [P] [US3] Test `BT/care/BR_M04_18_SuKienTinhPhiIT.java`, `FR_031_DinhChinhIT.java`; app `mobile/test/nhan-vien/diem_danh_buoi_test.dart` (20 người trong thời gian SC-009) (Q13 → Q17)
  - *(2026-11-05, C, một phần)* `BR_M04_18_SuKienTinhPhiIT` (7 ca), `FR_031_DinhChinhIT` (3 ca), `T014_SessionAttendanceSourcesIT` (3 ca: nhắc CFG-M04-06, nguồn công việc cho 005, handler ngoại tuyến) đỏ rồi xanh; web Vitest 2 ca mới. Còn test app `mobile/test/staff/...` (task mobile).

### Implementation cho User Story 3

- [ ] T014 [US3] Tạo bảng `diem_danh_buoi` (nhóm 3; `ket_qua` `CO_MAT|VANG|KHONG_GHI_NHAN`; `ly_do_vang` 5 giá trị) trong `MIG/V014_04__diem_danh_buoi.sql`; cài `BE/care/buoi/internal/DiemDanhBuoiService.java` (hoàn tất điểm danh; phát `SuKienTinhPhiNguon`/`DaDoi`; `NgoaiTuyenHandler` `M04.DIEM_DANH_BUOI`; `DinhChinhDuocProvider` `M04.DIEM_DANH_BUOI`; `NguonCongViecPort` `BUOI_HOAT_DONG`; `DiemDanhTruyVetPort` cho 007; nhắc CFG-M04-06) và endpoint `POST /buoi/{id}/diem-danh`; app `mobile/lib/nhan-vien/hoat-dong/` (research L-06) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `diem_danh_buoi` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - *(2026-11-05, C, một phần)* Không có `V014_04`: điểm danh là `record_event` `M04.SESSION_ATTENDANCE` (R-15); thêm `V014_03__activity_registration_billing.sql` (`billed_source_id`). Đã có `SessionAttendanceService` (điểm danh, thêm người lúc điểm danh, Hoàn tất; phát `billing.api.events.CostSourceBilled`), `SessionAttendanceCorrectionProvider` (`CostSourceChanged` khi bỏ lượt Có mặt), `SessionAttendanceOfflineHandler`, `ActivityTaskSource` (`ACTIVITY_SESSION` cho 005), `SessionAttendanceReminder` (CFG-M04-06), `POST /activity-sessions/{id}/attendance`, `complete-attendance`; web khối "Điểm danh" và nút "Hoàn tất điểm danh" (bố cục duyệt 05/11). Còn: `DiemDanhTruyVetPort` cho 007 (`incident.api.spi.AttendanceTracePort` do D khai, care không cài được vì C-02; team-plan mục 8), hiện sẵn "Vắng – đang vắng mặt" (FR-028) chờ trường trạng thái trong hợp đồng, app nhân viên.

**Checkpoint**: Điểm danh chạy độc lập.

---

## Phase 6: User Story 4 – Chuẩn bị và điểm danh rời viện cho hoạt động ngoài viện (Priority: P1)

**Goal**: Chuyến đi: phân công trưởng đoàn và người đi cùng (`ChuyenDiNhiemVuPort` cho 008/002); đánh giá khả năng (Trưởng tầng của tầng người đó); thuốc mang theo (006); lịch xe (019 qua `roombedallocation.api`); điều kiện rời viện; điểm danh rời viện gọi `TrangThaiNguoiCaoTuoiApi.thucHien(DIEM_DANH_ROI_VIEN)` trong cùng transaction (FR-033 → FR-040, BR-M04-16).

**Independent Test**: "Tham quan chùa" 07:00–13:00 với 4 đăng ký; trưởng đoàn và người đi cùng; 3 Đạt, 1 Không đạt; A có hai liều trong chuyến → thuốc cần mang; rời viện chặn khi chưa chuẩn bị thuốc.

### Tests cho User Story 4

- [ ] T015 [P] [US4] Test `BT/care/BR_M04_16_RoiVienIT.java`, `FR_035_NhiemVuIT.java`: rời viện từ hai thiết bị đồng thời; điều kiện (Đạt, thuốc `MangThuocChuyenDiApi.daChuanBi`, lịch xe); 001 chuyển `HOAT_DONG_BEN_NGOAI` cùng transaction, lỗi thì rollback; nghi nhiễm → `KHONG_DI` và báo trưởng đoàn (Q18 → Q25)
  - Kiểm 05/11/2026: `BR_M04_16_RoiVienIT` 34/34 và nhóm scoped 74 IT xanh; race phân công/rời viện và hủy đăng ký/rời viện đều đỏ trước khi cài rồi xanh. `FR_035_NhiemVuIT` vẫn chưa tách thành lớp riêng vì dùng chung fixture/phạm vi với BR_M04_16.
  - Sửa sau review 05/11/2026: thêm ca đăng ký lại sau Không đi và thứ tự khóa hủy đăng ký (đỏ rồi xanh); `BR_M04_16_RoiVienIT` 36/36, scoped 76 IT xanh, `clean verify` 1092 unit + 2696 IT 0 lỗi. Còn mở: tầng đánh giá người bán trú, phạm vi trưởng đoàn ngoài tầng (plan Phase 6).

### Implementation cho User Story 4

- [ ] T016 [US4] Tạo `MIG/V014_05__chuyen_di.sql`: `chuyen_di` (`buoi_id` khóa chính; `trang_thai` `DA_LEN_LICH|DANG_DI|DA_VE|DA_HUY`), `chuyen_di_gia_han` (nhóm 3), `nhiem_vu_chuyen` (`vai` `TRUONG_DOAN|DI_CUNG`), `nguoi_tham_gia` (`trang_thai` 7 giá trị), `danh_gia_kha_nang` (nhóm 3; `ket_qua` `DAT|KHONG_DAT`), `diem_danh_chuyen` (nhóm 3; `loai` `ROI_VIEN|VE`; `thoi_diem_thiet_bi`) (data-model mục 6) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `chuyen_di` → `buoi_hoat_dong`; `nguoi_tham_gia` → `dang_ky_buoi`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - Triển khai 05/11/2026: migration vật lý `V014_05__activity_trip.sql`, `T016_TripSchemaIT`; mới có cột chuyến/người tham gia và ràng buộc trên hai bảng 014. Nhiệm vụ/đánh giá/điểm danh đã khai qua TripKinds và cài cùng T017; gia hạn, trở về thuộc Phase 7 chưa cài, giữ T016 mở tới khi kiểm đủ.
- [ ] T017 [US4] Cài `BE/care/chuyendi/internal/ChuyenDiService.java` (phân công, đánh giá, rời viện; phát `ChuyenDiDaDoi`, `ChuyenDiThayDoi`; `ChuyenDiNhiemVuLenhApi` cho 015; `ChuyenDiThuocPort` (006), `ChuyenDiNhiemVuPort` (008), `ChuyenDiBuaAnPort` (011)) (research L-07, L-08)
  - Tiến độ 05/11/2026: đã có phân công/đánh giá/rời viện, nguồn đọc thuốc/nhiệm vụ, gỡ nhiệm vụ và TripChanged; còn nguồn chuyên môn/adapter thật, ngoại tuyến và metadata chuyến theo mẫu (plan Phase 6). Không đánh hoàn tất chỉ từ fake/test.
- [ ] T018 [US4] Tạo endpoint `/chuyen-di/{id}`, `/{id}/phan-cong`, `/{id}/danh-gia`, `/{id}/{lenh}`; màn web `frontend/src/tinh-nang/hoat-dong/chuyen-di/`; app phần chuyến đi

  - Tiến độ 05/11/2026: GET TripDetail tối thiểu và bố cục web/app đã duyệt; web/app phần dữ liệu hiện có đã cài. Còn Sheet đánh giá đầy đủ, thông tin thuốc/sức khỏe/liên hệ/thẻ khẩn cấp, tích hợp StaffRoot, kiểm trực quan và nguồn thời điểm ngoại tuyến.

**Checkpoint**: Chuẩn bị chuyến đi chạy độc lập.

---

## Phase 7: User Story 5 – Theo dõi chuyến đi, điểm danh về, quá giờ và thiếu người (Priority: P1)

**Goal**: Gia hạn giờ về; về từng phần; báo thiếu người (sự cố qua `SuCoLenhApi`, một lần); kết thúc điểm danh về khi còn người chưa về → cảnh báo Khẩn cấp; quá giờ về báo một lần mỗi mốc; chuyển trưởng đoàn; hủy ghi nhận rời viện/về; về muộn (FR-041 → FR-050, BR-M04-17).

**Independent Test**: A, B, C rời 07:05, về dự kiến 13:00; quá 13:30 → cảnh báo; gia hạn; về A, B, kết thúc điểm danh khi C chưa về → sự cố, cảnh báo Khẩn cấp.

### Tests cho User Story 5

- [ ] T019 [P] [US5] Test `BT/care/BR_M04_17_ThieuNguoiIT.java`, `FR_031a_DinhChinhChuyenIT.java`: hai lệnh Báo thiếu người đồng thời → một sự cố; Báo thiếu người chặn khi mất mạng (app); đính chính rời viện/về gọi `MangThuocChuyenDiApi.tinhLaiKhoang` và 001 `HUY_GHI_NHAN_ROI_VIEN` (Q26 → Q33)

### Implementation cho User Story 5

- [ ] T020 [US5] Hoàn thiện `ChuyenDiService.java`: điểm danh về (001 `DIEM_DANH_VE`), gia hạn, chuyển trưởng đoàn, báo thiếu người (`SuCoLenhApi.ghiNhan` MANDATORY), kết thúc về (`YeuCauTaoCanhBao` Khẩn cấp), về muộn, hủy ghi nhận; việc theo phút quá giờ về (một lần mỗi mốc, báo Quản lý viện); `NgoaiTuyenHandler` `M04.DIEM_DANH_ROI_VIEN`, `M04.DIEM_DANH_VE` (lệnh thiếu người bắt buộc trực tuyến); `DinhChinhDuocProvider` `M04.DIEM_DANH_CHUYEN` (research L-08)
  - Một phần 05/11/2026: `return`, `extend`, `transfer-leader`, `finish-return`, bước quá giờ về, Rời đoàn đã cài với test `BR_M04_17_ThieuNguoiIT` 8/8 (plan Phase 7). Còn: hai lệnh ghi sự cố (chờ port 007), ghi khi mất mạng, đính chính FR-031a; T019, T020 chưa đánh [x].
  - Một phần 06/11/2026: đính chính FR-031a Hủy ghi nhận rời viện (`TripAttendanceCorrectionProvider`, `FR_031a_DinhChinhChuyenIT` 4/4). Còn: đổi thời điểm, Hủy ghi nhận về (chờ 001).

**Checkpoint**: P1 hoàn tất.

---

## Phase 8: User Story 6 – Tinh thần, sở thích và cảnh báo nguy cơ cô lập (Priority: P2)

**Goal**: Sở thích, hồ sơ tinh thần; ngày tính và chuỗi cô lập; cảnh báo Nhẹ qua 007, không gợi ý (FR-051 → FR-056, BR-M04-22).

**Independent Test**: A sở thích "âm nhạc", "cờ tướng"; 7 ngày không có mặt ở hoạt động nhóm → một cảnh báo nhẹ, không gợi ý.

### Tests cho User Story 6

- [ ] T021 [P] [US6] Test đơn vị `BT/care/NgayTinhTest.java`, `CapNhatChuoiTest.java`, `TyLeThamGiaTest.java` và `BT/care/BR_M04_22_CoLapIT.java` (30 kịch bản, SC-007), `FR_051_SoThichIT.java` (Q34 → Q36; bổ sung 2026-10-18, Q-363: mục của người cao tuổi và của người thân cùng nhóm cùng hiện hành, mục mới cùng nhóm cùng nguồn cần xác nhận thay, nguồn người thân không có quan hệ Hiệu lực → 422, cảnh báo đăng ký nêu nguồn)

### Implementation cho User Story 6

- [ ] T022 [US6] Tạo bảng `so_thich` (`muc` `THICH|KHONG_THICH`; `nguon_loai ENUM('NGUOI_CAO_TUOI','NGUOI_THAN','NHAN_VIEN_QUAN_SAT')`, `nguon_nguoi_than_id BIGINT NULL` với `CHECK ((nguon_loai = 'NGUOI_THAN') = (nguon_nguoi_than_id IS NOT NULL))` theo mẫu 001; cột sinh `khoa_hien_hanh` theo (nhóm, nguồn) `UNIQUE`; `trang_thai`; sửa 2026-10-18, Q-363), `chuoi_co_lap` (kỹ thuật) trong `MIG/V014_06__tinh_than.sql`; cài `BE/care/tinhthan/internal/TinhThanService.java` (sở thích, hồ sơ tinh thần; việc 00:00 cập nhật chuỗi, `YeuCauTaoCanhBao` `NGUY_CO_CO_LAP` Nhẹ một lần mỗi chuỗi) và endpoint `/nguoi-cao-tuoi/{nctId}/so-thich`, `/so-thich/{id}/bo`, `/nguoi-cao-tuoi/{nctId}/ho-so-tinh-than`; màn web `frontend/src/tinh-nang/hoat-dong/tinh-than/` (research L-09) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `so_thich` → `ho_so_chung`; `chuoi_co_lap` → `trang_thai_nct`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
  - Một phần 06/11/2026: ghi, bỏ sở thích và cảnh báo "không thích" khi đăng ký (`FR_051_SoThichIT` 4/4, plan Phase 8). Còn: hai GET chờ schema, chuỗi cô lập chờ hàm nghi nhiễm theo khoảng của 007, `BR_M04_22_CoLapIT` và các test đơn vị ngày tính, web; T021, T022 chưa đánh [x].

**Checkpoint**: Tinh thần chạy độc lập.

---

## Phase 9: User Story 7 – Người kiểm tra lập danh sách kiểm tra chất lượng (Priority: P2)

**Goal**: Người kiểm tra (Trưởng tầng hoặc Người phụ trách ca) tự chọn công việc đã hoàn thành đủ tỷ lệ; ghi Đạt/Không đạt/Không kiểm tra được; Không đạt kèm lập công việc làm lại (005); hết ca → quá hạn; ca không có danh sách ghi lại (FR-057 → FR-066, BR-M04-23).

**Independent Test**: Ca ngày tầng 2 có 200 công việc hoàn thành; T lập danh sách thiếu tỷ lệ (bị chặn), rồi đủ 10; Không đạt một mục → công việc làm lại.

### Tests cho User Story 7

- [ ] T023 [P] [US7] Test đơn vị `BT/care/CongViecDuDieuKienTest.java`, `SoMucToiThieuTest.java`, `NguoiKiemTraTest.java` và `BT/care/BR_M04_23_KiemTraChatLuongIT.java`, `FR_066_NguoiKiemTraIT.java` (Q37 → Q43)

### Implementation cho User Story 7

- [ ] T024 [US7] Tạo bảng `danh_sach_kiem_tra` (`UNIQUE (ca_id, tang_khu_vuc_id)`), `muc_kiem_tra` (`cong_viec_id UNIQUE`; `trang_thai` `CHO_KIEM_TRA|DAT|KHONG_DAT|KHONG_KIEM_TRA_DUOC|QUA_HAN_KIEM_TRA`), `ca_khong_danh_sach` (nhóm 3) trong `MIG/V014_07__kiem_tra.sql`; cài `BE/care/kiemtra/internal/KiemTraChatLuongService.java` (kiểm tỷ lệ khi lập; Không đạt gọi lệnh lập công việc làm lại của 005 cùng transaction; `KetQuaKiemTraPort`; `NguonCongViecPort` `LAM_LAI`; `DinhChinhDuocProvider` `M04.KET_QUA_KIEM_TRA`; việc theo phút hết ca) (research L-10) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `danh_sach_kiem_tra` → `ho_so_chung`; `muc_kiem_tra` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T025 [US7] Tạo endpoint `/kiem-tra-chat-luong/ca/{caId}`, `/muc-kiem-tra/{id}/ket-qua`; màn web `frontend/src/tinh-nang/hoat-dong/kiem-tra-chat-luong/` (không xếp hạng công việc) với test Vitest
  - Một phần 06/11/2026: lập danh sách, thêm mục, ghi kết quả kèm công việc làm lại, `KetQuaKiemTraPort` (`BR_M04_23_KiemTraChatLuongIT` 6/6, plan Phase 9). Còn: GET chờ schema, việc theo phút (nhắc, quá hạn, ca không danh sách), tự Không kiểm tra được khi Hủy ghi nhận, đính chính kết quả, web; T023 → T025 chưa đánh [x].
  - Một phần 06/11/2026 lượt 2: việc theo phút nhắc, quá hạn, ca không có danh sách; mục tự Không kiểm tra được khi Hủy ghi nhận (`BR_M04_23_KiemTraChatLuongIT` 10/10). Còn: GET chờ schema, đính chính kết quả kiểm tra, báo cáo FR-065, web.

**Checkpoint**: Mọi user story của 014 chạy độc lập.

---

## Phase 10: Polish & Cross-Cutting

- [ ] T031 [P] *(Bổ sung 2026-10-18, Q-366, Q-369)* Test `BT/care/FR_068a_KenhNguoiCaoTuoiIT.java`, `FR_071_NhacBuoiNguoiCaoTuoiIT.java`, `SoThichTuPhanHoiIT.java` (quickstart H1 → H4)
- [ ] T032 *(Bổ sung 2026-10-18)* Cài `HoatDongKenhNctPortImpl`, `ChuyenDiSosPortImpl` trong `BE/care/hoatdong/internal/` (research L-16)
- [ ] T033 *(Bổ sung 2026-10-18)* Job nhắc buổi trong ngày cho nhóm `NguoiCaoTuoi` (00:05 và khi đăng ký trong ngày); thêm `so_thich.phan_hoi_id` (`MIG/V014_08__nguoi_cao_tuoi.sql`) và phát `YKienDaGhiTuPhanHoi` **Theo R-15 (data-model mục 0; bổ sung 2026-10-30): `so_thich` là loại `M04.SO_THICH` của `ho_so_chung`, nên `phan_hoi_id` là trường trong `noi_dung` (record của loại), không thêm cột; `V014_08` không còn sửa bảng `so_thich`.**

- [ ] T026 [P] Cài cổng người thân `GET /cong/nguoi-cao-tuoi/{nctId}/hoat-dong` (`QuyenNguoiThanApi`) và test `BT/care/FR_068_CongHoatDongIT.java`; app `mobile/lib/nguoi-than/hoat-dong/` (research L-12)
- [ ] T027 [P] Cài `HoatDongThongKeApi` (016); mở rộng `BT/common/HopDongApiTest.java` với `specs/014-activity-care-quality/contracts/api-014.yaml`
  - Một phần 06/11/2026: `ActivityStatistics` (buổi, chuyến, kết quả kiểm tra) với `T027_ActivityStatisticsIT` 4/4 (plan T027). Còn: `isolationRisks` chờ chuỗi cô lập; `HopDongApiTest` không có (so hợp đồng HTTP đã qua `ApiContractIT`); A review trường view.
- [ ] T028 Thay bản giả SPI 014 cài ở test 005, 006, 007, 008, 011, 012 (`NguonCongViecPort` `BUOI_HOAT_DONG`/`LAM_LAI`, `KetQuaKiemTraPort`, `ChuyenDiThuocPort`, `DiemDanhTruyVetPort`, `ChuyenDiNhiemVuPort`, `ChuyenDiBuaAnPort`) và chạy lại
- [ ] T029 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T030 Rà truy vết: DBR-12 (tương tự), 15, 18, 23, 25 và BR-M04-15 → 18, 21 → 23 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-021a, FR-067, FR-068, FR-069, FR-070, FR-071 — mỗi FR có task hoặc test tương ứng (constitution XV)
- [ ] T034 *(bổ sung 2026-10-29, research R-15 bản 2 của 000)* Khi viết migration và entity của feature, áp data-model mục 0: không tạo các bảng đã gộp, dùng bảng gộp, cột `JSON` và bảng chung (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`); test bảo vệ nhóm dữ liệu viết cho bảng gộp *(2026-10-30, audit cơ sở dữ liệu F-04, PR #63)* Mọi bảng tạo hoặc gộp khai nhóm dữ liệu: entity bằng `@DataGroup`, bảng JDBC bằng `COMMENT = 'DataGroup=CATALOG|BUSINESS|RECORD|MULTIPLE|TECHNICAL'` (backend/README.md mục 4); bảng `RECORD` có `trg_<bảng>_no_update`, `trg_<bảng>_no_delete`; bảng chứa nhóm 2 và nhóm 3 dùng `@DataGroup(MULTIPLE)`, cột `locked_at`, trigger `trg_<bảng>_row_lock`; dòng bảng chung đặt `entity_type` trùng `RetentionProvider.dataType()` của bản ghi cha; job dùng `SchedulerRuns` (module-interfaces 000 mục 1.8). `RecordGroupTriggerIT` kiểm.

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US1 → US2 → US3 (P1)**; **US4 → US5 (P1)** sau US2
- **US6 (P2)**: sau US3 (điểm danh có mặt)
- **US7 (P2)**: sau Foundational (dùng công việc của 005)

### Parallel Opportunities

- T002, T006 song song; US4 song song với US3; US7 song song với US1 → US5

## Parallel Example: User Story 4

```text
Task: "T015 BR_M04_16_RoiVienIT + FR_035_NhiemVuIT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2 → US3
2. **Dừng và kiểm chứng**: buổi từ mẫu → đăng ký → điểm danh → sự kiện tính phí
3. Thêm US4, US5 (chuyến đi)

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6 → US7; mỗi bước chạy `./mvnw verify`.

## Notes

- 014 không tạo buổi, danh sách kiểm tra, mục, công việc làm lại, sự cố, khoản chi phí; không gợi ý (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M04-*)
- Báo thiếu người, kết thúc điểm danh về bắt buộc trực tuyến
