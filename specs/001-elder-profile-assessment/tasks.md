---
description: "Danh sách task triển khai feature 001 – Hồ sơ và đánh giá người cao tuổi"
---

# Tasks: Hồ sơ và đánh giá người cao tuổi (001)

**Input**: Tài liệu thiết kế ở `specs/001-elder-profile-assessment/` (plan.md, spec.md, research.md H-01 → H-18, data-model.md, contracts/api-001.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000 (common, nhật ký, tham số, phê duyệt, đính chính, lưu trữ) và 002 (`@CanQuyen`, `PhamViApi`, `QuyenXemTruongApi`, SPI `QuanHeNguoiThanApi`) đã xong Phase 2.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 12.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; Q-347, Q-348, Q-353): sửa T007, T053, T055 → T057, T059, T067 → T070, T075; thêm T012a, T013a. Mã task cũ giữ nguyên.

**Cập nhật 2026-10-22** (plan mục "Cập nhật 2026-10-22"; Q-383, Q-384): thêm T077 → T083 (phiếu nguyện vọng khi điều trị FR-023j → FR-023m, DBR-39; giấy tờ bệnh viện FR-047c; `NguyenVongApi.dieuTriHieuLuc` cho 007). Không đánh số lại, không đổi task đã có.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M01: `BE/m01/{api,hoso,trangthai,dongy,suckhoe,nguyenvong,danhgia,lich}/`; tệp dùng chung `BE/common/tep/`
- Web `frontend/src/tinh-nang/{ho-so-nguoi-cao-tuoi,ban-dong-y,suc-khoe,nguyen-vong,danh-gia,danh-muc-m01}/`; app nhân viên `mobile/lib/nhan-vien/{tra-cuu-nguoi-cao-tuoi,suc-khoe,danh-gia,trang-thai}/`; app người thân `mobile/lib/nguoi-than/ho-so/`

---

## Phase 1: Setup

- [x] T001 Tạo khung gói `BE/m01/{api,hoso,trangthai,dongy,suckhoe,nguyenvong,danhgia,lich}/` với `package-info.java` (`@ApplicationModule`, chỉ công khai `api`) (research H-01) *(2026-10-21: chỉ tạo gói `api`, `api/dto`, `api/spi`, `api/events` có `package-info` `@NamedInterface`; các gói `internal/…` chưa tạo vì backend/README.md cấm gói rỗng, tạo cùng lớp đầu tiên của từng chức năng)*
- [ ] T002 [P] Tạo bản giả các giao diện 001 cần (contracts/module-interfaces.md mục 2) trong `BT/support/gia001/`: bean `ThamGiaChuyenTrangThai` cấu hình Đạt/Chưa đạt cho 003, 004, 005, 006, 007, 010, 011, 012, 013 (CHK035); `YeuCauDoiMucApi`, `HopDongApi`, `LuotVangApi` (004); `ChiSoApi` (007); bản giả sự kiện `SuCoDaGhiNhan` *(2026-10-21: đã xong `FakeStatusTransitionParticipant`, `FakeCareLevelChangeRequests`, `FakeContracts`, `FakeAbsences`, `FakeResidents` (bản giả `ResidentApi` cho 002, 003, 007), `FakeResidentEvents` (phát `ResidentStatusChanged` trong test), `FakeM01Config` ở `support/fake/m01/` (theo team-plan mục 3, thay `gia001/`). CHƯA xong `ChiSoApi` và `SuCoDaGhiNhan`: interface do D (007) khai báo, chưa có trong `develop`; không viết hộ, làm nốt khi D công bố)*

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Bảng hồ sơ, tệp đính kèm, khai báo `m01.api` và SPI mà module khác cài, danh mục, provider cho 000/002.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T003 Tạo `MIG/V001_01__tep.sql` và cài `BE/common/tep/internal/TepService.java` (cài `TepApi` của 000 T036): bảng `tep` (`uuid` là khóa đối tượng S3, `ten_goc`, `kieu_mime`, `kich_thuoc`, `sha256`, `tai_len_boi`, `tai_len_luc`, `chu_so_huu_loai`, `chu_so_huu_id`, `gan_luc` – `NULL` tới khi gắn, trigger chặn đổi khi đã gắn); nội dung lưu trong bucket AWS S3 qua `S3Client` (AWS SDK v2; cấu hình `AWS_REGION`, `ELDERCARE_S3_BUCKET`); kiểm kiểu và kích thước khi tải lên; lỗi S3 khi tải lên thì không tạo dòng `tep`; test dùng bản giả `S3Client` trong `BT/support/` (research H-13; NFR-08) *(sửa 2026-10-19: S3 thay volume máy chủ)* *(2026-10-23: XONG phần backend; tên thực `common/file` (`FileService`, `FileStore`, `FileStorageConfig`), migration `V001_06_1__file.sql`; xem plan CP-001-P7)*
- [x] T004 [P] Tạo `BE/common/tep/web/TepController.java` cho `POST /tep` và `GET /tep/{id}` (quyền tải qua `QuyenTepProvider` của module sở hữu, nội dung đọc từ S3 qua backend, không phát URL ký sẵn) và job `BE/common/tep/lich/DonTepChuaGan.java` (xóa đối tượng S3 rồi dòng `tep`) theo contracts/api-001.yaml (H-13) *(2026-10-23: XONG; `FileController` ở `m01/web` vì cần người dùng hiện tại của M15; job `UnattachedFileCleanupJob`)*
- [x] T005 Tạo `MIG/V001_02__nguoi_cao_tuoi.sql`: bảng `nguoi_cao_tuoi` theo data-model mục 2 (`ma_ho_so VARCHAR(20) UNIQUE` dạng `NCT-<năm>-<id>`; `ho_so_truoc_id` khóa ngoại tự trỏ; `ho_ten VARCHAR(200) NOT NULL`; cột sinh `ho_ten_so_khop VARCHAR(200) COLLATE utf8mb4_0900_as_ci` và chỉ mục `(ho_ten_so_khop, ngay_sinh)`; `ngay_sinh DATE NOT NULL`; `gioi_tinh ENUM('NAM','NU') NOT NULL`; `loai_giay_to ENUM('CCCD','KHAC') NULL`; `so_giay_to VARCHAR(30) NULL` với `CHECK ((loai_giay_to IS NULL) = (so_giay_to IS NULL))`; cột sinh `cccd_dang_mo` `UNIQUE`; `trang_thai` 8 mã `NOT NULL`; `trang_thai_tu_luc`; `muc_cham_soc_hien_hanh VARCHAR(20) NULL`; `phien_ban`), `lich_su_trang_thai_nct` (nhóm 3 chỉ thêm; `CHECK (ly_do IS NOT NULL OR do_he_thong OR lenh = 'TAO_HO_SO')`; `mien_dieu_kien JSON NULL`; hai trigger), `muc_cham_soc` (nạp `CO_BAN`, `THUONG_XUYEN`, `DAC_BIET`), `lich_su_muc_cham_soc` (`UNIQUE (nguoi_cao_tuoi_id, hieu_luc_tu)`, `can_cu_loai ENUM('DANH_GIA','YEU_CAU_THAY_DOI_LUU_TRU')`) *(2026-10-21: xong `V001_02__resident.sql`, test `ResidentSchemaIT` 11 test xanh trên MySQL 8.4 riêng cổng 3307; `profile_code` để NULL tới lúc đặt từ `id` vì cột sinh không dùng được với AUTO_INCREMENT (H-04); khóa ngoại tới `file` thêm sau T003)*
- [x] T006 Tạo `MIG/V001_03__danh_muc_m01.sql`: `di_nguyen` (`nhom ENUM('THUOC_HOAT_CHAT','THUC_PHAM','KHAC')`), `muc_do_di_ung` (`thu_tu`; nạp Nhẹ, Trung bình, Nặng), `nguon_thong_tin` (nạp Người cao tuổi, Người thân, Hồ sơ y tế kèm theo, Bác sĩ khám), `nhu_cau_cham_soc` (nạp Hỗ trợ vận động, Theo dõi sức khỏe thường xuyên, Phục hồi sau tai biến, Phục hồi chức năng khác; Q-291) theo mẫu nhóm 1 của 000 (BR-M01-11) *(2026-10-21: xong `V001_03__m01_catalogs.sql`, `CatalogSchemaIT` xanh; cột `nhom` thành `allergen_group` vì `group` là từ khóa)*
- [x] T007 [P] Tạo kiểu và giao diện công khai trong `BE/m01/api/`: `NguoiCaoTuoiApi`, `TrangThaiNguoiCaoTuoiApi`, `MucChamSocApi`, `DanhGiaApi`, `DanhGiaLaiApi`, `MucSucKhoeApi`, `HoSoSucKhoeApi`, `DiNguyenApi`, `BanDongYApi`, `NguyenVongApi`, `NguonYKienApi` cùng `NguonYKien`, `LoaiNguonYKien` (mục 1.8b, bổ sung 2026-10-18), `NguoiCaoTuoiThongKeApi` (016), enum `TrangThaiNct`, `LenhTrangThai` đúng contracts/module-interfaces.md mục 1.1 → 1.8b *(2026-10-21: tên và chữ ký theo glossary mục 8.5; `isChecked` đổi thành `markChecked`; trường của các view do 001 đặt vì hợp đồng chỉ nêu tên)*
- [x] T008 [P] Khai báo SPI `BE/m01/api/ThamGiaChuyenTrangThai.java` (`lenhQuanTam`, `kiemTraDieuKien`, `mienDieuKien`, `kiemTraChiTiet`, `thucHienTacDong` MANDATORY, `canhBao`) và record `NguCanhLenh`, `DieuKien`, `MienDieuKien`, `KetQuaChuyen`; khai báo SPI M02 cài `YeuCauDoiMucApi`, `HopDongApi`, `LuotVangApi` trong `BE/m01/api/` (contracts mục 1.3, mục 2; tránh vòng M01 ↔ M02)
- [x] T009 [P] Tạo sự kiện `BE/m01/api/sukien/` theo contracts mục 1.10 (`NguoiCaoTuoiDaTao`, `NguoiCaoTuoiDaChuyenTrangThai`, `BanDongYDaDoi`, `DiUngDaGhiNhan`, `DiUngKhacDaGhiNhan`, `DiUngDaLoaiTru`, `HoSoSucKhoeBanDauDaDoi`, `DanhGiaDauVaoDaXacNhan`, `DanhGiaDaDoiMucHoacNhuCau`, `CanDanhGiaLai`, `QuaHanDanhGiaLai`, `CanLapYeuCauDoiMuc`, `NhacLapYeuCauDoiMuc`, `NhacThieuBanDongY`, `NhacThieuNguyenVong`, `NhacXemLaiNhanDinh` (bổ sung 2026-10-18, FR-023i), `TamVangQuaHan`, `MucChamSocDaDoi`, `ThongTinCaNhanDaCapNhat`); sự kiện gửi 009 cài `CoYeuCauThongBao` với mức, nhóm nhận, loại thông tin theo research H-14; Chuyển viện, Ghi nhận qua đời tới người liên hệ chính khai `doiTuongThongTin = VE_NGUOI_CAO_TUOI` và mã Q-354 `CHUYEN_VIEN` / `QUA_DOI` (009 FR-001a; bổ sung 2026-10-18) *(2026-10-21: đã khai báo 19 sự kiện ở `m01.api.events`. 2026-10-24: XONG `HasNotificationRequests` cho 9 sự kiện nhắc, cảnh báo và sự kiện mới `ResidentStatusNotice` (Chuyển viện, Ghi nhận qua đời: Khẩn cấp, người liên hệ chính, `ABOUT_RESIDENT`, Q-354); mức, nhóm nhận theo research H-14; `ResidentNotificationRequestsTest` 12 test, `ResidentNotificationsIntakeIT` 5 test qua listener thật của 009. Người nhận thật phụ thuộc `CaTrucApi` (E), phân công (E), quan hệ người thân (E))*
- [x] T010 Cài `BE/m01/trangthai/internal/HoSoMoGuard.java` (chặn ghi mới trên hồ sơ ở trạng thái cuối) và test kiến trúc `BT/m01/HoSoMoGuardArchTest.java` buộc mọi `@LenhNghiepVu` ghi vào hồ sơ gọi guard (BR-M01-05) *(2026-10-21: xong `ProfileOpenGuard` và `ProfileOpenGuardArchTest`; luật kiểm lệnh gọi guard trực tiếp, lệnh không ghi hồ sơ khai trong danh sách ngoại lệ có lý do, có test chứng minh luật bắt được lệnh thiếu guard)*
- [ ] T011 [P] Cài provider cho 000/002 trong `BE/m01/hoso/internal/`: `PhanLoaiTruongProvider` theo bảng data-model mục 10 (gồm `qdToa*` của bản đồng ý → `NHAN_DANG`, bổ sung 2026-10-18), `GanPhamViProvider` cho `M01.NGUOI_CAO_TUOI` và bản ghi con, `QuyenTepProvider` cho `M01.ANH_NGUOI_CAO_TUOI`, `M01.BAN_DONG_Y`, `M01.NGUYEN_VONG`, `M01.QUA_DOI`, `LuuTruProvider` theo data-model mục 11 (`THEO_HO_SO`, hồ sơ có hồ sơ sau trỏ tới không bị loại trước) *(2026-10-21: MỚI XONG một phần. Đã có `FieldClassificationProvider` và `ScopeAssignmentProvider` cho `M01.RESIDENT`. 2026-10-24: thêm `FieldClassificationProvider` cho `M01.HEALTH_ITEM`, `M01.INITIAL_HEALTH_RECORD`, `M01.ASSESSMENT` (`AuditLogHealthMaskingIT`) và `FilePermissionProvider` cho `M01.RESIDENT_PHOTO`, `M01.DEATH_EVIDENCE`. 2026-10-24 đợt 4: XONG `ScopeAssignmentProvider` cho bảy loại bản ghi con (`ChildScopeAssignmentIT`) và `RetentionProvider` (`ResidentRetention` gốc `M01.RESIDENT` cùng bảy loại bản ghi con, `ResidentRetentionIT`; xem plan, ghi chú đợt 4). CÒN `[ ]`: lưu giữ tệp đính kèm (data-model mục 11 "`tep` gắn với các bản ghi trên"): xóa dòng `file` không xóa nội dung ở S3 và `FileApi` chưa có hàm xóa; cần chủ dự án quyết cách xóa nội dung S3 trước khi làm; hiện tệp được giữ lâu hơn hạn (hướng an toàn))*
- [x] T012 Cài lệnh danh mục chung M01 (tạo, sửa, ngừng hiệu lực, kích hoạt lại; người cấu hình theo data-model mục 5 "Danh mục") trong `BE/m01/suckhoe/internal/DanhMucM01Service.java` và `BE/m01/suckhoe/web/DanhMucController.java` cho `/danh-muc/{loai}`, `/{id}`, `/{id}/ngung-hieu-luc`, `/{id}/kich-hoat-lai`; test `BT/m01/BR_M01_11_DanhMucIT.java` *(2026-10-21: MỚI XONG MỘT PHẦN. Xong `CatalogService`, thực thể, repository và migration (`CatalogSchemaIT` xanh); controller `/catalogs/{type}` và `BR_M01_11_CatalogIT` (7 test xanh) viết xong nhưng chưa đưa vào nhánh vì `ApiContractIT` (file chung của A) chưa có `api-001` và schema `Problem` của `api-001` (có `candidates`) lệch `ProblemBody` dùng chung, cần chủ dự án duyệt cách xử lý; `care-levels` chỉ đọc, ngừng hiệu lực và kích hoạt lại bắt buộc lý do, tạo và sửa không có lý do vì hợp đồng không có trường lý do)* *(2026-10-22: controller `CatalogController` và `BR_M01_11_CatalogIT` (7 test xanh trên cổng 3307) đã lấy lại từ stash vào nhánh để màn Sức khỏe (T033) lấy danh mục; phần sửa `ApiContractIT` và `api-001.yaml` vẫn trong stash chờ A duyệt (BL-23))*
- [x] T012a Cài `BE/m01/hoso/internal/NguonYKienService.java` (cài `NguonYKienApi.kiemTra`: `NGUOI_THAN` thì người thân phải có quan hệ Hiệu lực lúc gọi qua `QuanHeNguoiThanApi.thuocNguoiCaoTuoi`, loại khác thì `nguoiThanId` phải rỗng; sai → 422 `NGUON_Y_KIEN_KHONG_HOP_LE`) và test `BT/m01/BR_M01_14_NguonYKienIT.java` viết trước; 004, 005, 012, 014 dùng giao diện này (FR-023g, BR-M01-14, research H-16; bổ sung 2026-10-18) *(2026-10-21: xong `OpinionSourceService`, `BR_M01_14_OpinionSourceIT` 4 test xanh)*
- [x] T013 [P] Test `BT/m01/NhatKyDayDuM01IT.java`: mọi lệnh M01 có một `INSERT nhat_ky` với `nguoi_cao_tuoi_id` (DBR-23) *(2026-10-24, đợt 3: `M01AuditCompletenessIT` (proxy `DataSource`, đúng một `INSERT audit_log` cho từng lệnh danh mục) và `M01CommandAuditTrailIT` 12 test cho mọi lệnh gắn người cao tuổi: hồ sơ (tạo, sửa), mục sức khỏe (ghi, loại trừ, đã kiểm tra), hồ sơ sức khỏe ban đầu (tạo, sửa, xác nhận), đánh giá (tạo, sửa, xác nhận, hủy nháp), bảy lệnh trạng thái và lệnh do module khác gọi, đồng ý (ghi, thay, rút), nguyện vọng cuối đời và khi điều trị (ghi, thay), đổi mức theo yêu cầu thay đổi lưu trú, đính chính; kiểm `resident_id`, loại và mã bản ghi, người thực hiện, vai trò, lý do, căn cứ; kèm kiểm mọi trường được ghi đã phân loại hoặc cố ý công khai; `specialNotes` và `familyMemberIdsAllowedHealthView` thuộc nhóm `HEALTH`. Phần bean thật của 012 (DBR-02) chưa có, chỉ kiểm qua bản giả. Sửa trong M01: `CareLevelApiImpl.applyChange` chưa ghi nhật ký; đính chính không có `resident_id`)*
- [x] T013a [P] Test kiến trúc `BT/m01/FR_023h_KhongDauNangLucArchTest.java`: quét `information_schema.COLUMNS` của schema test và OpenAPI sinh từ springdoc; báo lỗi khi bảng có `nguoi_cao_tuoi_id` (hoặc chính `nguoi_cao_tuoi`) hay schema người cao tuổi có cột, trường chứa `nang_luc` / `nangLuc` (danh sách cho phép rỗng; `tu_quyet_duoc` chỉ ở phiếu nguyện vọng của 001 và mục thỏa thuận của 005) (FR-023h, data-model mục 6a; bổ sung 2026-10-18) *(2026-10-24: test thật là `FR_023h_NoCapacityFlagIT` (đổi tên từ `FR_023h_KhongDauNangLucIT`); đặt tên `*IT` vì cần MySQL để đọc `information_schema`, quét thêm OpenAPI do springdoc sinh; 2 test xanh)*

- [x] T083 [P] *(Bổ sung 2026-10-22)* Thêm vào `BE/m01/api/`: `NguyenVongApi.dieuTriHieuLuc(nctId)` và record `NguyenVongDieuTriView`, `TrangThaiNguoiCaoTuoiApi.giayToDinhKem(lichSuId)` (contracts/module-interfaces.md mục 1.2, 1.8); cập nhật bản giả `BT/support/fake/m01/` cùng PR (luật "Đổi api", docs/team-plan.md mục 3) để 007, 006 dùng *(2026-10-24: `dieuTriHieuLuc` đã có từ Phase 9 dưới tên `EndOfLifeWishApi.currentTreatmentWish`; thêm bản giả `FakeEndOfLifeWishes` (`current`, `recordVersion`, `currentTreatmentWish`; `@Import(FakeEndOfLifeWishes.Config.class)`). Xong 2026-10-24 (chủ dự án duyệt): `ResidentStatusApi.hospitalDocuments(historyId): List<FileView>` (tên `giayToDinhKem` của hợp đồng) gọi `FileApi.attachedTo(ownerType, ownerId)` mới ở `common.file` (A; chỉ siêu dữ liệu `FileView(id, originalName, mimeType, sizeBytes)`, cộng thêm, không đổi `upload`, `attach`, quyền tải); test `FileServiceIT.attachedTo_…`, `HospitalDocumentsIT.FR_047c_attachmentsOfARow_…`)*

**Checkpoint**: Hồ sơ và SPI sẵn sàng; module khác có thể cài `ThamGiaChuyenTrangThai`.

---

## Phase 3: User Story 1 – Hành chính tạo hồ sơ và nhân viên tra cứu theo quyền (Priority: P1) 🎯 MVP

**Goal**: Tạo hồ sơ, chống trùng CCCD, cảnh báo trùng họ tên + ngày sinh, liên kết hồ sơ của người quay lại, cập nhật thông tin cá nhân, tra cứu theo phạm vi và giới hạn trường (FR-001 → FR-009, DBR-01).

**Independent Test**: Tạo hồ sơ đủ thông tin, thử hồ sơ thứ hai trùng CCCD, đăng nhập từng vai trò để tra cứu và kiểm phần hiển thị.

### Tests cho User Story 1

- [x] T014 [P] [US1] Test `BT/m01/TaoHoSoIT.java`, `HoSoCccdIT.java`, `HoSoTrungTenIT.java`: CCCD trùng hồ sơ chưa ở trạng thái cuối → 409; trùng hồ sơ trạng thái cuối được phép; trùng họ tên + ngày sinh yêu cầu xác nhận không trùng (`NCT_XAC_NHAN_KHONG_TRUNG`) (US1 #1–5; DBR-01, FR-003, FR-005)
- [x] T015 [P] [US1] Test `BT/m01/HoSoLienKetIT.java` và `CapNhatThongTinIT.java`: liên kết hồ sơ trước; cập nhật với `If-Match` cũ → 412; đổi giới tính phát `ThongTinCaNhanDaCapNhat` (FR-004, FR-006)
- [x] T016 [P] [US1] Test `BT/m01/TraCuuPhamViIT.java` và `GioiHanTruongHoSoIT.java`: nhân viên ngoài phạm vi → 404; Hành chính không thấy trường sức khỏe (US1 #6–10; FR-007 → FR-009) *(2026-10-22: `TraCuuPhamViIT` ở mức service, `GioiHanTruongHoSoIT` qua HTTP.)*

### Implementation cho User Story 1

- [x] T017 [P] [US1] Tạo entity `BE/m01/hoso/internal/NguoiCaoTuoi.java` (`@NhomDuLieu(NGHIEP_VU)`, không có setter trạng thái) và `LichSuTrangThaiNct.java` (`GHI_NHAN`, `ChiThemRepository`)
- [x] T018 [US1] Cài `BE/m01/hoso/internal/HoSoService.java`: tạo hồ sơ (`TAO_HO_SO` → `DANG_TIEP_NHAN`, dòng lịch sử đầu, `NCT_TAO`), xác nhận không trùng, liên kết hồ sơ trước, cập nhật thông tin cá nhân (`NCT_CAP_NHAT`, `If-Match`); ảnh qua `TepApi.gan` (research H-04) *(2026-10-22: service ở `ResidentProfileService`, test mức service vì controller chờ BL-23. 2026-10-24: ảnh gắn bằng `FileApi.attach` khi tạo và thay ảnh, chủ sở hữu `M01.RESIDENT_PHOTO` (id hồ sơ), `ResidentPhotoFilePermission`; `ResidentPhotoIT` 8 test)*
- [ ] T019 [US1] Cài tra cứu `BE/m01/hoso/internal/TraCuuNguoiCaoTuoi.java` dùng `PhamViApi.boLoc` và chỉ mục `(ho_ten_so_khop, ngay_sinh)`, `ma_ho_so`, `cccd_dang_mo`; DTO có `@NhomTruong` (research H-05; NFR-02) *(2026-10-22: lọc theo tập hồ sơ `residentIds` của 002; lọc theo tầng, phòng chờ phân bổ giường của 003. `@NhomTruong` ở DTO làm cùng T020.)*
- [x] T020 [US1] Tạo `BE/m01/hoso/web/NguoiCaoTuoiController.java`: `POST/GET /nguoi-cao-tuoi`, `GET /nguoi-cao-tuoi/{id}` (kèm `lenhDuocPhep`), `POST …/{id}/cap-nhat-thong-tin`, `GET …/{id}/ho-so-lien-ket` theo contracts/api-001.yaml *(2026-10-22: `ResidentController`; `ApiContractIT` của 000 chưa đăng ký api-001 nên đang đỏ vì đường dẫn `/residents` chưa có trong hợp đồng nào, chờ BL-23; `ResidentContractIT` kiểm tạm khớp hợp đồng. `allowedCommands` mới có `UPDATE_PERSONAL_INFO`.)*
- [x] T021 [US1] Cài `NguoiCaoTuoiApi` (`trangThai`, `nhanDang`, …) trong `BE/m01/hoso/internal/NguoiCaoTuoiApiImpl.java` cho 002 và các module khác
- [x] T022 [P] [US1] Tạo màn web `frontend/src/tinh-nang/ho-so-nguoi-cao-tuoi/TraCuu.jsx`, `TaoHoSo.jsx`, `ChiTietHoSo.jsx` (hiển thị `truongBiAn`, xác nhận trùng) và test `HoSo.test.jsx`, `XacNhanTrung.test.jsx` *(2026-10-22: đã có `CreateResident.jsx`, `ResidentSearch.jsx` gắn vào `ResidentListPage` và test `HoSo.test.jsx`, `XacNhanTrung.test.jsx`; 2026-10-22: thêm `ResidentDetail.jsx` (xem hồ sơ, cập nhật thông tin cá nhân với `If-Match`, chuỗi hồ sơ trước, báo trường bị ẩn) và test `ResidentDetail.test.jsx`, mở từ bảng `ResidentSearch`; cờ phiếu nguyện vọng khi điều trị và cảnh báo thiếu phiếu thuộc T079, T080. Thư mục là `features/residents` theo frontend/README.)*
- [ ] T023 [P] [US1] Tạo `mobile/lib/nhan-vien/tra-cuu-nguoi-cao-tuoi/` (tra cứu, thẻ người cao tuổi) và `mobile/lib/nguoi-than/ho-so/` (xem hồ sơ, phần sức khỏe theo chú thích ¹)

**Checkpoint**: Tạo và tra cứu hồ sơ chạy độc lập.

---

## Phase 4: User Story 2 – Hồ sơ sức khỏe ban đầu; dị ứng và bệnh nền là từng mục chỉ loại trừ (Priority: P1)

**Goal**: Hồ sơ sức khỏe ban đầu nhóm 3 có nháp; mục sức khỏe nội dung bất biến, chỉ loại trừ có lý do; kiểm tra dị ứng sau commit có cờ chờ kiểm tra (FR-017 → FR-023, DBR-04, BR-M01-07, BR-M01-11, BR-M01-12).

**Independent Test**: Ghi 2 dị ứng và 1 bệnh nền, loại trừ 1 dị ứng, thử xóa và sửa nội dung; kiểm danh sách hiện hành, đã loại trừ và nhật ký.

### Tests cho User Story 2

- [x] T024 [P] [US2] Test `BT/m01/MucSucKhoeBatBienIT.java`: `UPDATE` nội dung và `DELETE` bị trigger chặn; `DA_LOAI_TRU → HIEU_LUC` bị chặn; không có endpoint sửa, xóa (DBR-04, SC-003) *(2026-10-22: `HealthItemImmutableIT`, 9 test xanh trên MySQL 8.4 cổng 3307; kiểm thêm `CHECK` dị ứng thiếu mức độ, loại trừ đủ ba cột, cột sinh `not_auto_checked`)*
- [x] T025 [P] [US2] Test `BT/m01/BR_M01_07_DiUngIT.java`, `BR_M01_12_ChoKiemTraIT.java`, `MucSucKhoeTrungIT.java`: dị ứng thiếu mức độ → 422; dị ứng không có dị nguyên đánh dấu "không kiểm tra tự động"; 006/011 lỗi thì mục vẫn lưu, cờ chờ kiểm tra còn và job phát lại; trùng → 409 `MUC_SUC_KHOE_TRUNG` trừ khi `xacNhanTrung` (US2 #1–8; FR-018a, FR-021) *(2026-10-22: `BR_M01_07_AllergyIT` 10 test, `BR_M01_12_PendingCheckIT` 4 test (có listener giả lỗi), `HealthItemDuplicateIT` 5 test, xanh trên cổng 3307)*
- [x] T026 [P] [US2] Test `BT/m01/HoSoSucKhoeBanDauIT.java`: nháp sửa được, xác nhận chỉ AC-03, sau xác nhận trigger chặn sửa; đính chính `M01.HO_SO_SUC_KHOE_BAN_DAU` phát `HoSoSucKhoeBanDauDaDoi` (FR-017) *(2026-10-22: `InitialHealthRecordIT` 11 test và `HealthEndpointsIT` 7 test qua HTTP, xanh trên cổng 3307)*

### Implementation cho User Story 2

- [x] T027 [US2] Tạo `MIG/V001_04__suc_khoe.sql`: `ho_so_suc_khoe_ban_dau` (`UNIQUE (nguoi_cao_tuoi_id)`, trigger chặn `UPDATE` khi `OLD.thoi_diem_xac_nhan IS NOT NULL`), `muc_suc_khoe` (`loai ENUM('DI_UNG','BENH_NEN','TIEN_SU')`; `noi_dung_van_ban VARCHAR(1000) NULL` với `CHECK (di_nguyen_id IS NOT NULL OR noi_dung_van_ban IS NOT NULL)`; cột sinh `khong_kiem_tra_tu_dong`; `CHECK (loai <> 'DI_UNG' OR muc_do_id IS NOT NULL)`; `trang_thai ENUM('HIEU_LUC','DA_LOAI_TRU')`; ba cột loại trừ bắt buộc khi `DA_LOAI_TRU`; `cho_kiem_tra_thuoc`, `cho_kiem_tra_an`; trigger `trg_muc_suc_khoe_noi_dung_bat_bien`, `trg_muc_suc_khoe_khong_xoa`) (data-model mục 5) *(2026-10-22: xong `V001_04__health.sql` (`initial_health_record`, `health_item`); trigger đặt tên `trg_initial_health_record_no_update` để `RecordGroupTriggerIT` nhận (chặn `UPDATE` khi đã xác nhận); cột nhóm máu 10 ký tự)*
- [x] T028 [P] [US2] Tạo entity `BE/m01/suckhoe/internal/HoSoSucKhoeBanDau.java`, `MucSucKhoe.java` *(2026-10-22: `InitialHealthRecord`, `HealthItem` ở `m01/internal/health/domain`; `not_auto_checked` tính lại trong mã, không ánh xạ)*
- [x] T029 [US2] Cài `BE/m01/suckhoe/internal/HoSoSucKhoeService.java`: lập, sửa nháp (`If-Match`), xác nhận (AC-03); `DinhChinhDuocProvider` cho `M01.HO_SO_SUC_KHOE_BAN_DAU` (`tangKhuVuc` rỗng, trên hồ sơ trạng thái cuối chỉ cách C; FR-049a) (research H-08) *(2026-10-22: `InitialHealthRecordService` và `InitialHealthRecordCorrectionProvider`; chỉ Bác sĩ xác nhận kiểm theo vai trò (ma trận không có dòng riêng); `HoSoSucKhoeBanDauDaDoi` (`InitialHealthRecordChanged`) phát khi đính chính xong, không phát khi xác nhận lần đầu vì 006 đọc qua `HealthRecordApi.initial`)*
- [x] T030 [US2] Cài `BE/m01/suckhoe/internal/MucSucKhoeService.java`: ghi nhận (danh mục `HIEU_LUC`, trùng, `HoSoMoGuard`), loại trừ có lý do; đặt cờ chờ kiểm tra và phát `DiUngDaGhiNhan`/`DiUngKhacDaGhiNhan`/`DiUngDaLoaiTru` sau commit; nhận kết quả kiểm tra thì ghi `MSK_DA_KIEM_TRA` (research H-07) *(2026-10-22: `HealthItemService` (cài `HealthItemApi`); cảnh báo trùng so khớp không phân biệt hoa thường, bỏ khoảng trắng thừa, có phân biệt dấu; `markChecked` ghi nhật ký `HEALTH_ITEM_CHECKED` kèm căn cứ BR-M01-12 để gọi được từ Bộ lập lịch)*
- [x] T031 [US2] Cài job `BE/m01/lich/ThuLaiKiemTraDiUng.java` (15 phút, ShedLock): phát lại `DiUngDaGhiNhan` cho mục còn cờ quá 15 phút (BR-M01-12; research H-12) *(2026-10-22: `AllergyCheckRetry` và `m01/scheduling/RetryAllergyChecksJob`; chu kỳ, ngưỡng 15 phút ở `eldercare.jobs.retry-allergy-checks.*` (không là hằng số, theo `TimeArchTest`); thêm dòng vào `SchedulerScopeArchTest`; tên job `RETRY_ALLERGY_CHECKS` CHƯA có trong `runJobs` của api-000, chờ A, test gọi `run()` trực tiếp)*
- [x] T032 [US2] Tạo `BE/m01/suckhoe/web/SucKhoeController.java`: `PUT /nguoi-cao-tuoi/{id}/ho-so-suc-khoe-ban-dau`, `POST …/xac-nhan`, `POST /ho-so-suc-khoe-ban-dau/{id}/dinh-chinh`, `POST/GET /nguoi-cao-tuoi/{id}/muc-suc-khoe`, `POST /muc-suc-khoe/{id}/loai-tru`; cài `MucSucKhoeApi`, `HoSoSucKhoeApi`, `DiNguyenApi` *(2026-10-22: `HealthController`, `AllergenApiImpl` (`DiNguyenApi`; `addAllergen` chỉ Bác sĩ, chỉ nhóm thực phẩm); `ResidentController` thêm bốn lệnh vào `allowedCommands` (`RECORD_HEALTH_ITEM`, `EXCLUDE_HEALTH_ITEM`, `WRITE_INITIAL_HEALTH_RECORD`, `CONFIRM_INITIAL_HEALTH_RECORD`, lệnh cuối lọc thêm theo vai trò Bác sĩ); `HealthItemResult` giữ tên trường `items` đúng hợp đồng (glossary ghi `item`, chờ chủ dự án chốt); `ResidentContractIT` mở rộng; `ApiContractIT` vẫn đỏ vì BL-23)*
- [ ] T033 [P] [US2] Tạo màn web `frontend/src/tinh-nang/suc-khoe/HoSoBanDau.jsx`, `MucSucKhoe.jsx` và app `mobile/lib/nhan-vien/suc-khoe/` (ghi mục sức khỏe trực tuyến) *(2026-10-22: PHẦN WEB XONG (bố cục tab Sức khỏe duyệt 2026-10-22): `HealthTab`, `InitialHealthRecord`, `HealthItems` ở `features/residents/` (đặt trong `residents`, theo frontend/README mục 2), 19 test Vitest; phần app `mobile/` chưa làm nên task chưa `[x]`)*

**Checkpoint**: Sức khỏe ban đầu và dị ứng chạy độc lập.

---

## Phase 5: User Story 3 – Bác sĩ đánh giá đầu vào bằng nhận xét và chọn mức chăm sóc (Priority: P1)

**Goal**: Lần đánh giá nhóm 3 có nháp, một nháp mỗi người, xác nhận chỉ Bác sĩ với nhận xét và mức; không có điểm hay gợi ý; lưu ý chăm sóc hiện hành cho thẻ người cao tuổi (FR-024 → FR-038, DBR-05, BR-M01-09, BR-M01-10, BR-M01-13).

**Independent Test**: Lập đánh giá đầu vào; xác nhận thiếu nhận xét, thiếu mức, bởi điều dưỡng (đều bị từ chối); xác nhận hợp lệ rồi kiểm mức hiện hành, lưu ý chăm sóc, và không có nháp kế hoạch chăm sóc tự sinh.

### Tests cho User Story 3

- [x] T034 [P] [US3] Test `BT/m01/BR_M01_09_DanhGiaIT.java` và `XacNhanDanhGiaIT.java`: thiếu nhận xét hoặc mức → 422 và `CHECK` ở MySQL chặn; Điều dưỡng xác nhận → 403; xác nhận đặt `muc_cham_soc_hien_hanh` và dòng `lich_su_muc_cham_soc` cùng transaction; không tạo bản ghi kế hoạch chăm sóc (DBR-05, Q-306)
- [x] T035 [P] [US3] Test `BT/m01/BR_M01_13_MotBanNhapIT.java`: nháp thứ hai → 409 `DA_CO_BAN_NHAP`; hủy nháp có lý do rồi tạo được; đầu vào Đã xác nhận thứ hai bị `UNIQUE` chặn
- [x] T036 [P] [US3] Test `BT/m01/BR_M01_10_LuuYChamSocIT.java` và `DinhChinhDanhGiaIT.java`: `DanhGiaApi.luuYHienHanh` theo lô; đính chính đánh giá thêm lần báo `DINH_CHINH_DANH_GIA` khi đổi mức (research H-10) *(2026-10-23: phần `luuYHienHanh`, đính chính, sự kiện `AssessmentCorrectedLevel` xong và xanh; lần báo `DINH_CHINH_DANH_GIA` chờ bảng của T062 nên chưa `[x]`)* *(2026-10-23, Phase 8: lần báo `DINH_CHINH_DANH_GIA` ghi qua `ReassessmentNoticeListener`, `AssessmentCorrectionIT` xanh)*

### Implementation cho User Story 3

- [x] T037 [US3] Tạo `MIG/V001_05__danh_gia.sql`: `danh_gia` theo data-model mục 7 (`loai ENUM('DAU_VAO','DANH_GIA_LAI')`; `loai_luu_tru_de_xuat ENUM('NOI_TRU_DAI_HAN','NOI_TRU_NGAN_NGAY','BAN_TRU') NULL` chỉ với `DAU_VAO`; `trang_thai ENUM('NHAP','DA_XAC_NHAN','DA_HUY')`; cột sinh `nhap_dang_mo`, `dau_vao_da_xac_nhan` có `UNIQUE`; `CHECK (trang_thai <> 'DA_XAC_NHAN' OR (nhan_xet IS NOT NULL AND CHAR_LENGTH(TRIM(nhan_xet)) > 0 AND muc_cham_soc IS NOT NULL))`; trigger `trg_danh_gia_khong_sua`) và `danh_gia_nhu_cau` (trigger chặn `INSERT`/`DELETE` khi cha không còn `NHAP`)
- [x] T038 [P] [US3] Tạo entity `BE/m01/danhgia/internal/DanhGia.java`, `DanhGiaNhuCau.java`, `LichSuMucChamSoc.java`
- [x] T039 [US3] Cài `BE/m01/danhgia/internal/DanhGiaService.java`: tạo (`DAU_VAO` chỉ khi `DANG_TIEP_NHAN` và chưa có đầu vào xác nhận; `DANH_GIA_LAI` theo FR-035), sửa nháp, hủy nháp, xác nhận với tác động cùng transaction theo research H-10 (đặt mức hiện hành, `lich_su_muc_cham_soc`, `MUC_CS_DOI`, gọi `YeuCauDoiMucApi.thayTheBoiDanhGia` MANDATORY, đáp ứng lần báo đánh giá lại, phát `DanhGiaDauVaoDaXacNhan`, `DanhGiaDaDoiMucHoacNhuCau`, `MucChamSocDaDoi`); gợi ý chỉ số gần nhất qua `ChiSoApi.ganNhat` để hiển thị
- [x] T040 [US3] Cài `MucChamSocApi`, `DanhGiaApi` (`luuYHienHanh` theo lô) và `DinhChinhDuocProvider` cho `M01.DANH_GIA` trong `BE/m01/danhgia/internal/`
- [x] T041 [US3] Tạo `BE/m01/danhgia/web/DanhGiaController.java`: `POST /nguoi-cao-tuoi/{id}/danh-gia`, `GET/PUT /danh-gia/{id}`, `POST /danh-gia/{id}/xac-nhan`, `/huy-nhap`, `/dinh-chinh`, `GET /nguoi-cao-tuoi/{id}/muc-cham-soc`, `/lich-su-muc-cham-soc`
- [ ] T042 [P] [US3] Tạo màn web `frontend/src/tinh-nang/danh-gia/LapDanhGia.jsx` (không có ô điểm, không có mức đề xuất) và test `DanhGia.test.jsx`; app `mobile/lib/nhan-vien/danh-gia/` (lập nháp); thẻ người cao tuổi hiển thị lưu ý chăm sóc và test `mobile/test/nhan-vien/the_nguoi_cao_tuoi_test.dart` *(2026-10-23: phần web xong, 13 Vitest, lint, build xanh, chờ chủ dự án kiểm thử trên trình duyệt; phần mobile chưa mở nên chưa `[x]`)*

**Checkpoint**: Đánh giá đầu vào và mức chăm sóc chạy độc lập.

---

## Phase 6: User Story 4 – Trạng thái người cao tuổi chỉ thay đổi qua lệnh nghiệp vụ có điều kiện (Priority: P1)

**Goal**: Một điểm chuyển trạng thái, khóa dòng, gộp điều kiện từ mọi bean, tác động trong cùng transaction, endpoint riêng mỗi lệnh, API cho 007, 014 (FR-044 → FR-050, BR-M01-01, BR-M01-04 → 06).

**Independent Test**: Đi hết chuyển hợp lệ, thử mọi cặp không có trong bảng, thử từng lệnh khi thiếu từng điều kiện (bean giả của module khác).

### Tests cho User Story 4

- [x] T043 [P] [US4] Test đơn vị `BT/m01/BR_M01_04_BangChuyenTest.java`: mọi cặp (trạng thái, lệnh) của bảng chuyển data-model mục 2; cặp ngoài bảng → `CHUYEN_KHONG_HOP_LE`
- [x] T044 [P] [US4] Test `BT/m01/FR_045_LietKeDieuKienIT.java`, `FR_047_RollbackTacDongIT.java`, `FR_048_LenhDongThoiIT.java`: liệt kê đủ điều kiện chưa thỏa trong một lần; một bean ném lỗi thì cả lệnh rollback; hai lệnh đồng thời chỉ một thành công; `nguoi_cao_tuoi.trang_thai` luôn bằng dòng lịch sử mới nhất
- [x] T045 [P] [US4] Test `BT/m01/BR_M01_05_HoSoDongIT.java`, `BR_M01_06_LenhRiengIT.java`, `GhiNhanQuaDoiIT.java`, `LichSuTrangThaiIT.java`: hồ sơ trạng thái cuối chặn ghi mới; lệnh từ 014 có `do_he_thong`; AC-09 ghi nhận qua đời chỉ khi khác `DANG_LUU_TRU` và có `bangChungTepId`; yêu cầu ngoại lệ đã duyệt của 004 miễn điều kiện và ghi `mien_dieu_kien` (US4 #1–13; FR-047b)
- [x] T046 [P] [US4] Test `BT/m01/BR_M01_01_TamVangQuaHanIT.java` (đồng hồ giả lập): quá mốc dự kiến + CFG-M01-01 cảnh báo đúng một lần mỗi mốc; gia hạn tạo mốc mới

### Implementation cho User Story 4

- [x] T047 [US4] Cài `BE/m01/trangthai/internal/ChuyenTrangThaiService.java`: khóa `SELECT … FOR UPDATE`, `HoSoMoGuard`, kiểm bảng chuyển, quyền theo khóa ở data-model mục 2, gọi `kiemTraChiTiet`, gộp `kiemTraDieuKien` của mọi bean `lenhQuanTam` chứa lệnh, áp `mienDieuKien` của 004, ghi `lich_su_trang_thai_nct` + `NCT_CHUYEN`, gọi `thucHienTacDong` của mọi bean trong cùng transaction, gom `canhBao`, phát `NguoiCaoTuoiDaChuyenTrangThai` (research H-02) *(2026-10-23: xong `ResidentStatusService`; tệp đính kèm thuộc T082. 2026-10-24: bằng chứng qua đời được kiểm tồn tại, gắn bằng `FileApi.attach` vào dòng lịch sử trong transaction (chủ sở hữu `M01.DEATH_EVIDENCE`), `DeathEvidenceFilePermission` (người thân không tải được: quyết định tạm thời chờ A xác nhận); `RecordDeathIT` 12 test. Phần backend trong phạm vi task đã đủ; ô tải tệp trên web `StatusTab` còn thiếu, thuộc T052)*
- [x] T048 [US4] Cài tác động riêng M01: Hoàn tất tiếp nhận kiểm đánh giá đầu vào Đã xác nhận chưa quá CFG-M01-02 (FR-034a); Ghi nhận trở về từ bệnh viện đặt `han_tinh_tu` và thêm lần báo `TRO_VE_BENH_VIEN` (research H-11) trong `BE/m01/trangthai/internal/ThamGiaM01.java` *(2026-10-23: xong phần FR-034a (`M01StatusParticipant`); phần Ghi nhận trở về từ bệnh viện cần bảng `lan_bao_danh_gia_lai` của T062, làm cùng T062)* *(2026-10-23, Phase 8: Ghi nhận trở về đặt hạn và thêm lần báo `TRO_VE_BENH_VIEN`, test trong `BR_M01_02_BaoDanhGiaLaiIT`)*
- [x] T049 [US4] Cài `TrangThaiNguoiCaoTuoiApi` trong `BE/m01/trangthai/internal/TrangThaiApiImpl.java`: `DIEM_DANH_ROI_VIEN`, `DIEM_DANH_VE`, `HUY_GHI_NHAN_ROI_VIEN` cho 014 (`do_he_thong`), `CHUYEN_VIEN` cho 007, `xemDieuKien` cho 004 (research H-03) *(2026-10-23: xong `execute`, `previewConditions`; `statusPeriods` xong 2026-10-24: khoảng Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài dựng từ lịch sử trạng thái, lấy khoảng giao với [từ, đến], trả nguyên khoảng; `StatusPeriodsIT` 5 test)*
- [x] T050 [US4] Tạo `BE/m01/trangthai/web/TrangThaiController.java`: `GET /nguoi-cao-tuoi/{id}/lich-su-trang-thai`, `GET …/dieu-kien/{lenh}`, `POST …/hoan-tat-tiep-nhan`, `/huy-tiep-nhan`, `/cho-tam-vang`, `/ghi-nhan-tro-ve`, `/chuyen-vien`, `/ket-thuc-luu-tru`, `/ghi-nhan-qua-doi` (thân `lyDo`, `chiTiet`) theo contracts/api-001.yaml; `DinhChinhDuocProvider` cho `M01.LICH_SU_TRANG_THAI` *(2026-10-23: xong 9 endpoint, `allowedCommands`; 2026-10-24: xong `StatusHistoryCorrectionProvider` (loại `M01.RESIDENT_STATUS_HISTORY`; chỉ đính chính được lý do; người ghi gốc cách A, hồ sơ trạng thái cuối chỉ cách C; lịch sử hiển thị lý do đã đính chính; `StatusHistoryCorrectionIT` 4 test). Hạn chế: 000 cho phép đính chính loại Hủy ghi nhận (VOID) với mọi loại, provider không chặn được; Hủy ghi nhận dòng lịch sử không đổi trạng thái hiện tại, báo A ở team-plan mục 8)*
- [x] T051 [US4] Tạo `MIG/V001_06__canh_bao_tro_lai.sql` (`(luot_vang_id, moc_du_kien)` khóa chính, `canh_bao_luc`) và cài job `BE/m01/lich/CanhBaoQuaHanTroLai.java` dùng `LuotVangApi.dangMo()` và CFG-M01-01, phát `TamVangQuaHan` (BR-M01-01, FR-050) *(2026-10-23: `TemporaryAbsenceOverdueAlert`, `V001_06__return_overdue_alert.sql`)*
- [ ] T052 [P] [US4] Tạo màn web lệnh trạng thái trong `frontend/src/tinh-nang/ho-so-nguoi-cao-tuoi/LenhTrangThai.jsx` (xem trước điều kiện, hiển thị `dieuKienChuaThoa[]`, cảnh báo) và test `DieuKienChuaThoa.test.jsx`; app `mobile/lib/nhan-vien/trang-thai/` (Cho tạm vắng, Ghi nhận trở về, Chuyển viện, Ghi nhận qua đời; chỉ trực tuyến) *(2026-10-23: xong phần web `frontend/src/features/status/` (`StatusTab.jsx`, `StatusTab.test.jsx`, 10 test Vitest); chủ dự án chưa nghiệm thu giao diện; phần app `mobile/` chưa mở)*
- [x] T081 [P] [US4] *(Bổ sung 2026-10-22)* Test `BT/m01/GiayToBenhVienIT.java` (quickstart T5): Ghi nhận trở về từ `DIEU_TRI_BENH_VIEN` kèm hai tệp → tệp gắn `M01.GIAY_TO_BENH_VIEN` với `lich_su_trang_thai_nct.id`, `giayToDinhKem` trả hai tệp; không kèm tệp vẫn thành công; Cho tạm vắng kèm `tepDinhKemIds` → 422 `KHONG_NHAN_TEP_DINH_KEM`; Hành chính tải tệp bị chặn, Điều dưỡng trong phạm vi tải được (FR-047c, BR-M05-32) *(2026-10-24: `HospitalDocumentsIT` 10 test, tên lớp thật thay `GiayToBenhVienIT`; đã kiểm: gắn hai tệp, không kèm tệp, lệnh không nhận tệp → 422, tệp không có, đã gắn, quá 10, quyền tải. Thêm 2026-10-24 test `hospitalDocuments` trả hai tệp kèm tên, kiểu, kích thước (11 test))*
- [ ] T082 [US4] *(Bổ sung 2026-10-22)* Nhận `tepDinhKemIds` trong `ChuyenTrangThaiService` (chỉ `GHI_NHAN_TRO_VE` từ `DIEU_TRI_BENH_VIEN` và `CHUYEN_VIEN`; gắn bằng `TepApi.gan` trong transaction của lệnh), cài `giayToDinhKem`, `QuyenTepProvider` cho `M01.GIAY_TO_BENH_VIEN`; màn web `LenhTrangThai.jsx` thêm chọn tệp cho Ghi nhận trở về; app `mobile/lib/nhan-vien/trang-thai/` thêm chụp giấy tờ (theo mục "Giai đoạn hiện tại" của CLAUDE.md) (research H-18) *(2026-10-24: XONG backend: `attachmentFileIds` ở `details` (REST gộp từ thân), chỉ `RECORD_RETURN` từ `HOSPITALIZED` và `TRANSFER_OUT` kể cả qua `ResidentStatusApi.execute` của 007; tối đa 10, trùng chỉ gắn một lần; `FileApi.attach` vào dòng lịch sử (`M01.HOSPITAL_DOCUMENT`) trong transaction, lỗi thì rollback; `HospitalDocumentFilePermission` theo quyền xem dị ứng, bệnh nền trong phạm vi. Web xong 2026-10-24 (bố cục được duyệt): `StatusTab` chọn nhiều tệp (tối đa 10, bỏ từng tệp, tải khi gửi, không tải lại khi gửi lại, lỗi `FILE_TOO_LARGE`, `FILE_TYPE_NOT_ALLOWED` tại ô) cho Ghi nhận trở về từ Điều trị tại bệnh viện và Chuyển viện; ô nhập tay mã tệp của Ghi nhận qua đời thay bằng chọn tệp (`FileField` dùng chung thêm `multiple`); `hospitalDocuments` xong. CHƯA `[x]`: app `mobile/` chưa mở (theo CLAUDE.md, task mobile để `[ ]` tới khi chủ dự án mở)* )

**Checkpoint**: Máy trạng thái chạy với bean giả; P1 hoàn tất.

---

## Phase 7: User Story 5 – Hành chính ghi nhận và thu hồi bản đồng ý chia sẻ dữ liệu (Priority: P2)

**Goal**: Bản đồng ý có bằng chứng, tối đa một bản Hiệu lực, thay thế và rút lại; là căn cứ chú thích ¹; nhắc khi thiếu (FR-010 → FR-016, BR-M01-08, DBR-03).

**Independent Test**: Bản đồng ý gồm người thân X không gồm Y → X thấy sức khỏe, Y không; rút lại → X mất quyền ngay.

### Tests cho User Story 5

- [x] T053 [P] [US5] Test `BT/m01/BanDongYIT.java` và `RutLaiDongYIT.java`: thiếu tệp → 422; bản mới thay bản cũ (`BDY_THAY_THE`); `DAI_DIEN_PHAP_LUAT` thiếu một trong bốn trường quyết định của Tòa án → 422 `THIEU_QUYET_DINH_TOA_AN`; người ký thay không có quan hệ Hiệu lực → 409 `NGUOI_KY_KHONG_HOP_LE`; người đại diện ở 012 mà không có quyết định của Tòa án bị từ chối (US5 #3, #10, lớp `FR_011_DaiDienPhapLuatIT.java`, sửa 2026-10-18); rút lại được cả trên hồ sơ trạng thái cuối; trigger chỉ cho đổi cột rút lại theo chiều `HIEU_LUC → DA_RUT_LAI` (FR-010 → FR-016)
- [x] T054 [P] [US5] Test `BT/m01/BR_M01_08_DongYIT.java` và `NhacThieuDongYIT.java` (đồng hồ giả lập): điều kiện chú thích ¹ kiểm mỗi yêu cầu; nhắc Hành chính mỗi CFG-M01-06 khi thiếu bản Hiệu lực (BR-M01-08, DBR-03, FR-012)

### Implementation cho User Story 5

- [x] T055 [US5] Tạo `MIG/V001_07__ban_dong_y.sql`: `ban_dong_y` theo data-model mục 4 (`nguoi_dong_y_loai ENUM('NGUOI_CAO_TUOI','DAI_DIEN_PHAP_LUAT')`; `nguoi_dong_y_nguoi_than_id` "Bắt buộc khi `DAI_DIEN_PHAP_LUAT` (`CHECK`)"; `qd_toa_so VARCHAR(100)`, `qd_toa_ngay DATE`, `qd_toa_toa_an VARCHAR(200)`, `qd_toa_tep_id BIGINT` "Bắt buộc cả bốn khi `DAI_DIEN_PHAP_LUAT`, rỗng cả bốn khi `NGUOI_CAO_TUOI` (`CHECK`)"; không có `ly_do_dai_dien` (sửa 2026-10-18, Q-348); `bang_chung_tep_id NOT NULL`; `trang_thai ENUM('HIEU_LUC','DA_RUT_LAI')`; cột sinh `dang_hieu_luc` `UNIQUE`; trigger chỉ cho cột rút lại và thay thế), `ban_dong_y_nguoi_than` (khóa chính `(ban_dong_y_id, nguoi_than_id)`, chỉ thêm) và `nhac_chu_ky` (khóa chính `(loai, nguoi_cao_tuoi_id, moc_id)`, `loai ENUM('THIEU_BAN_DONG_Y','THIEU_NGUYEN_VONG','LAP_YEU_CAU_DOI_MUC','XEM_LAI_NHAN_DINH')`)
- [x] T056 [US5] Cài `BE/m01/dongy/internal/BanDongYService.java` (ghi nhận, thay thế, rút lại; người ký thay kiểm `QuanHeNguoiThanApi.thuocNguoiCaoTuoi`, không gọi `laNguoiDaiDienHieuLuc`; gắn tệp quyết định của Tòa án; `BanDongYApi` gồm `daiDienPhapLuat` cho 012 FR-020c; phát `BanDongYDaDoi`) và `DieuKienChuThich("1")` trong `BE/m01/dongy/internal/DieuKienDongY.java` (research H-06)
- [x] T057 [US5] Tạo `BE/m01/dongy/web/BanDongYController.java`: `POST/GET /nguoi-cao-tuoi/{id}/ban-dong-y` (thân lệnh có `quyetDinhToaAn`), `POST /ban-dong-y/{id}/rut-lai`
- [x] T058 [US5] *(2026-10-23: XONG; lần nhắc đầu sau một chu kỳ kể từ Hoàn tất tiếp nhận)* Cài job `BE/m01/lich/NhacThieuDongYNguyenVong.java` (phần bản đồng ý: trạng thái Đang lưu trú, Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện; mỗi CFG-M01-06; dòng `nhac_chu_ky` thêm khi Hoàn tất tiếp nhận thiếu bản đồng ý) (FR-012, SC-013)
- [x] T059 [P] [US5] *(2026-10-23: XONG phần web, chờ chủ dự án nghiệm thu trên trình duyệt; mã thực `frontend/src/features/consents/` (`ConsentTab`, `consentLabels.js`, 12 test Vitest), `frontend/src/common/files.js`; mã người thân nhập tay tạm tới khi 012 có danh sách)* Tạo màn web `frontend/src/tinh-nang/ban-dong-y/BanDongY.jsx` (người ký: người cao tuổi hoặc người đại diện theo pháp luật; chọn người ký thay thì hiện bốn ô quyết định của Tòa án và tải bản scan; nhãn "nhận thông báo không khẩn về người cao tuổi")

**Checkpoint**: Bản đồng ý điều khiển quyền xem sức khỏe của người thân.

---

## Phase 8: User Story 6 – Đánh giá lại theo định kỳ, sau sự cố, sau nằm viện; đổi mức thì Hành chính lập yêu cầu (Priority: P2)

**Goal**: Báo đánh giá lại (định kỳ CFG-M01-02, sau sự cố, sau trở về từ bệnh viện), cảnh báo quá hạn CFG-M01-03, nhắc Hành chính lập yêu cầu đổi mức (CFG-M03-02); không tự tạo yêu cầu (FR-039 → FR-043, FR-036, FR-053, BR-M01-02, BR-M01-03).

**Independent Test**: Tua tới hạn CFG-M01-02 → bác sĩ được báo; đánh giá lại khác mức → Hành chính được báo; quá CFG-M01-03 → cảnh báo.

### Tests cho User Story 6

- [x] T060 [P] [US6] Test `BT/m01/BR_M01_02_BaoDanhGiaLaiIT.java` (đồng hồ giả lập): báo định kỳ; sự cố `canDanhGiaLai` báo một lần; đang Điều trị tại bệnh viện thì `han_tinh_tu` rỗng; `UNIQUE` chống báo lặp; cảnh báo quá hạn một lần (US6 #1–8) *(2026-10-23: `BR_M01_02_BaoDanhGiaLaiIT` 10 test xanh)*
- [x] T061 [P] [US6] Test `BT/m01/BR_M01_03_DoiMucIT.java`, `NhacLapYeuCauDoiMucIT.java`, `FR_053_MucTaiNgayIT.java`, `DocLapLuuTruMucChamSocIT.java`: đánh giá lại khác mức phát `CanLapYeuCauDoiMuc`, không tự tạo yêu cầu; nhắc mỗi CFG-M03-02 tới khi 004 có yêu cầu chưa kết thúc; mức tại ngày D đúng lịch sử; áp dụng yêu cầu thay đổi lưu trú ghi `lich_su_muc_cham_soc` `YEU_CAU_THAY_DOI_LUU_TRU` từ 00:00 ngày hiệu lực (US6 #9–12; FR-051 → FR-053) *(2026-10-23: `BR_M01_03_DoiMucIT`, `NhacLapYeuCauDoiMucIT`, `FR_053_MucTaiNgayIT`, `DocLapLuuTruMucChamSocIT` xanh)*

### Implementation cho User Story 6

- [x] T062 [US6] Tạo `MIG/V001_08__lan_bao_danh_gia_lai.sql`: `lan_bao_danh_gia_lai` (`can_cu ENUM('DINH_KY','SAU_SU_CO','TRO_VE_BENH_VIEN','DINH_CHINH_DANH_GIA')`, `han_tinh_tu DATETIME(6) NULL`, `da_canh_bao_qua_han_luc`, `UNIQUE (nguoi_cao_tuoi_id, can_cu, nguon_loai, nguon_id)`) *(2026-10-23: `V001_08__reassessment_notice.sql`)*
- [ ] T063 [US6] Cài `BE/m01/danhgia/internal/BaoDanhGiaLaiService.java` ("cần đánh giá lại" tính từ lần báo chưa được đáp ứng; Q-307, research H-11), listener `SuCoDaGhiNhan` (khi `canDanhGiaLai`), `DanhGiaLaiApi` *(2026-10-23: `ReassessmentNoticeService`, `ReassessmentApi` impl và listener `AssessmentCorrectedLevel` xong; còn listener sự cố vì D chưa khai báo `IncidentRecorded` (team-plan mục 8), nên chưa `[x]`)*
- [x] T064 [US6] Cài job `BE/m01/lich/BaoDanhGiaLaiDinhKy.java`, `CanhBaoQuaHanDanhGiaLai.java`, `NhacLapYeuCauDoiMuc.java` (ShedLock, 15 phút; CFG-M01-02, CFG-M01-03, CFG-M03-02 đọc mỗi lần chạy; `lan_chay_lich`) (research H-12) *(2026-10-23: ba job 15 phút, `SchedulerScopeArchTest` xanh)*
- [x] T065 [US6] Cài lệnh ghi mức từ yêu cầu thay đổi lưu trú (004 gọi khi áp dụng, `MANDATORY`) trong `MucChamSocApi` và cập nhật `lich_su_muc_cham_soc` (FR-052, FR-053) *(2026-10-23: `CareLevelApi.applyChange` có từ Phase 5, nay có test `FR_053_MucTaiNgayIT`)*
- [x] T066 [US6] Tạo endpoint `GET /nguoi-cao-tuoi/{id}/danh-gia-lai`, `GET /danh-gia-lai/can-thuc-hien` trong `BE/m01/danhgia/web/DanhGiaLaiController.java` và màn web `frontend/src/tinh-nang/danh-gia/CanDanhGiaLai.jsx` *(2026-10-23: `ReassessmentController` và web `ReassessmentNotice`, `ResidentSearch`; chờ B nghiệm thu trình duyệt)*

**Checkpoint**: Vòng đánh giá lại chạy với đồng hồ giả lập.

---

## Phase 9: User Story 7 – Phiếu nguyện vọng cuối đời khi tiếp nhận và phiếu nguyện vọng khi điều trị (Priority: P2)

**Goal**: Phiếu có phiên bản, tối đa một bản Hiệu lực, người ký theo năng lực, giới hạn xem, nhắc khi thiếu (FR-023a → FR-023f, DBR-34).

**Independent Test**: Hoàn tất tiếp nhận người chưa có phiếu → cảnh báo, nhắc; ghi phiếu, ghi phiên bản mới; Hành chính xem phiếu bị chặn.

### Tests cho User Story 7

- [x] T067 [P] [US7] Test `BT/m01/NguyenVongPhienBanIT.java`, `NguyenVongNguoiKyIT.java`, `NguyenVongGioiHanXemIT.java`, `NhacThieuNguyenVongIT.java`: phiên bản mới thay bản cũ, `UNIQUE dang_hieu_luc`; `CHECK (tu_quyet_duoc = (nguoi_ky_loai = 'NGUOI_CAO_TUOI'))`; thiếu `cachHoTroHieu` hoặc `ngayXemLai` → 422; `DAI_DIEN_NGUOI_BENH` thiếu `cachChonDaiDien` → 422; `DAI_DIEN_PHAP_LUAT` thiếu quyết định của Tòa án → 422; người ký thay không có quan hệ Hiệu lực ở 012 → 409; ý kiến khác lưu từng dòng có nguồn, nguồn sai → 422 `NGUON_Y_KIEN_KHONG_HOP_LE` (US7 #2, #3, #7; sửa 2026-10-18); Hành chính không xem được nhóm `NGUYEN_VONG`; nhắc Bác sĩ/Điều dưỡng phụ trách mỗi CFG-M01-06; `NhacXemLaiNhanDinhIT` (đồng hồ giả lập): tới ngày xem lại nhắc một lần Bác sĩ đã nhận định và Điều dưỡng phụ trách, chạy lại không nhắc trùng, phiếu bị thay trước ngày đó không nhắc (Q37; FR-023i) (DBR-34; FR-023a → FR-023f, FR-023i)

### Implementation cho User Story 7

- [x] T068 [US7] Tạo `MIG/V001_09__nguyen_vong.sql`: `nguyen_vong_cuoi_doi` theo data-model mục 6 (`lua_chon ENUM('CHUYEN_BENH_VIEN','DUA_VE_NHA','O_LAI_GIAM_NHE')`; `nguoi_tra_loi_loai`, `nguoi_ky_loai ENUM('NGUOI_CAO_TUOI','DAI_DIEN_NGUOI_BENH','DAI_DIEN_PHAP_LUAT')`; `tu_quyet_duoc BOOLEAN`, `thoi_diem_nhan_dinh DATETIME(6)`, `cach_ho_tro_hieu TEXT` "`NOT NULL`, không rỗng", `ngay_xem_lai_nhan_dinh DATE` "`NOT NULL`; `>= DATE(thoi_diem_nhan_dinh)` (`CHECK`)"; `cach_chon_dai_dien ENUM('NCT_DA_CHON','GIA_DINH_CHON') NULL` "Bắt buộc khi `DAI_DIEN_NGUOI_BENH`, rỗng khi khác (`CHECK`)"; `qd_toa_*` bắt buộc cả bốn khi `DAI_DIEN_PHAP_LUAT`; không có `du_nang_luc`; `ban_ky_tep_id NOT NULL`; `nguon ENUM('TIEP_NHAN','YEU_CAU_GIA_DINH','XAC_NHAN_LAI_NGUY_KICH')`; `trang_thai ENUM('HIEU_LUC','DUOC_THAY_THE')`; cột sinh `dang_hieu_luc` `UNIQUE`; trigger chỉ cho `HIEU_LUC → DUOC_THAY_THE`) và `nguyen_vong_y_kien` (chỉ thêm; mẫu cột nguồn data-model mục 6a: `nguon_loai ENUM('NGUOI_CAO_TUOI','NGUOI_THAN','NHAN_VIEN_QUAN_SAT')`, `CHECK ((nguon_loai = 'NGUOI_THAN') = (nguon_nguoi_than_id IS NOT NULL))`; `noi_dung TEXT NOT NULL`; không cấp `UPDATE`, `DELETE`)
- [x] T069 [US7] Cài `BE/m01/nguyenvong/internal/NguyenVongService.java` (ghi phiên bản mới; `NguyenVongApi.ghiPhienBan` cho 007; bác sĩ nhận định có AC-03 lúc ghi; nhận định gắn với phiếu, người ký theo FR-023c qua `thuocNguoiCaoTuoi`; ý kiến khác kiểm bằng `NguonYKienApi` rồi ghi `nguyen_vong_y_kien` cùng transaction; ngày xem lại chỉ lưu, không nhắc; `NV_GHI`) và phần nguyện vọng của job `NhacThieuDongYNguyenVong`, job `BE/m01/lich/NhacXemLaiNhanDinh.java` (ShedLock, 15 phút; `nhac_chu_ky` `XEM_LAI_NHAN_DINH`; FR-023i) (research H-09, H-12) *(2026-10-23: xong; việc nhắc xem lại nhận định gộp vào `MissingConsentReminderJob` (một job nhắc theo chu kỳ của H-12) với `WishReminderService.runReviews`, không thêm lớp job riêng để khỏi đổi `SchedulerScopeArchTest`, `application.yml`, `runJobs` của A; chu kỳ kiểm tra mỗi giờ thay vì 15 phút vì hạn nhắc tính theo ngày (FR-023i) và theo CFG-M01-06 (1 ngày); tách job khi A cho phép. Đã rà 2026-10-23: đáp ứng FR-023d, FR-023i về hành vi (kiểm thử `MissingEndOfLifeWishReminderIT`, `SelfDeterminationReviewReminderIT`), lệch hình thức đã ghi.)*
- [x] T070 [US7] Tạo `BE/m01/nguyenvong/web/NguyenVongController.java` cho `POST/GET /nguoi-cao-tuoi/{id}/nguyen-vong` và màn web `frontend/src/tinh-nang/nguyen-vong/NguyenVong.jsx` (khối nhận định của Bác sĩ cho phiếu này: tự quyết được, cách hỗ trợ hiểu, ngày xem lại; người ký theo nhận định; danh sách ý kiến khác, mỗi ý kiến chọn nguồn; không có ô năng lực trên hồ sơ) *(2026-10-23: xong; backend `EndOfLifeWishController`, web `frontend/src/features/wishes/EndOfLifeWish.jsx` trong tab "Nguyện vọng" cùng 11 Vitest. Chưa nghiệm thu trên trình duyệt.)*

### Bổ sung 2026-10-22: phiếu nguyện vọng khi điều trị (US7 #9 → #11; FR-023, FR-023j → FR-023m; DBR-39)

- [x] T077 [P] [US7] Test `BT/m01/NguyenVongDieuTriPhienBanIT.java`, `NguyenVongDieuTriGioiHanXemIT.java`, `HoanTatTiepNhanCanhBaoDieuTriIT.java` (quickstart T1 → T4): phiên bản mới chuyển bản cũ `HET_HIEU_LUC`, `UNIQUE dang_hieu_luc` chặn hai bản Hiệu lực; thiếu bệnh viện ưu tiên hoặc quá hai bệnh viện → 422; người cần báo không có quan hệ Hiệu lực → 409 `NGUOI_THAN_KHONG_HOP_LE`; `hoiGiaDinhTruoc` không người cần báo → 201 kèm `CHUA_CO_NGUOI_CAN_BAO`; ý kiến gia đình khác lưu từng dòng có nguồn; Hành chính chỉ có `coNguyenVongDieuTri`, `GET …/nguyen-vong-dieu-tri` 403; Kế toán không có cờ (`truongBiAn`); người thân xem khi thỏa chú thích ¹; Hoàn tất tiếp nhận không phiếu → 200 kèm `THIEU_NGUYEN_VONG_DIEU_TRI`, tua đồng hồ không có nhắc; `dieuTriHieuLuc` trả đủ trường cho 007 (SC-014)
- [x] T078 [US7] Tạo `MIG/V001_10__nguyen_vong_dieu_tri.sql` theo data-model mục 6b: `nguyen_vong_dieu_tri` (`bv1_*` `NOT NULL`, `CHECK` cặp `bv2_*`; `bhyt_*` `NULL`; `hoi_gia_dinh_truoc BOOLEAN NOT NULL`; `nguoi_di_kem ENUM('GIA_DINH','VIEN_CU_NHAN_VIEN','TUY_TINH_HUONG')`; mẫu cột nguồn ý kiến cho người trả lời; `ban_ky_tep_id NULL`; `trang_thai ENUM('HIEU_LUC','HET_HIEU_LUC')`; cột sinh `dang_hieu_luc` `UNIQUE`; trigger chỉ cho `HIEU_LUC → HET_HIEU_LUC`), `nguyen_vong_dieu_tri_nguoi_bao` (khóa chính ghép), `nguyen_vong_dieu_tri_y_kien` (mẫu mục 6a); không cấp `UPDATE`, `DELETE` cho bảng con
- [x] T079 [US7] Cài `BE/m01/nguyenvong/internal/NguyenVongDieuTriService.java` (khóa quyền `DI_UNG_BENH_NEN`; `HoSoMoGuard`; người cần báo, người trả lời qua `QuanHeNguoiThanApi.thuocNguoiCaoTuoi`; ý kiến qua `NguonYKienApi`; `NVDT_GHI`), `NguyenVongApi.dieuTriHieuLuc` (họ tên, số điện thoại người cần báo qua SPI của 012), cảnh báo `THIEU_NGUYEN_VONG_DIEU_TRI` trong `ThamGiaM01.canhBao` cho `HOAN_TAT_TIEP_NHAN`, cờ `coNguyenVongDieuTri` trên DTO hồ sơ (chỉ cho người có `(DI_UNG_BENH_NEN, XEM)` hoặc Hành chính; Kế toán không có, vào `truongBiAn`); thêm `M01.NGUYEN_VONG_DIEU_TRI` vào `PhanLoaiTruongProvider` (mọi trường `SUC_KHOE`), `LuuTruProvider`, `QuyenTepProvider` (research H-18) *(2026-10-24: xong; `LuuTruProvider` cho `M01.TREATMENT_WISH` và hai bảng con (`treatment_wish_notify`, `treatment_wish_opinion`) nằm trong `ResidentRetention`, kiểm bởi `ResidentRetentionIT`; tệp bản ký chưa lưu giữ, xem T011.)*
- [x] T080 [US7] Tạo `BE/m01/nguyenvong/web/NguyenVongDieuTriController.java` cho `GET/POST /nguoi-cao-tuoi/{id}/nguyen-vong-dieu-tri` và màn web `frontend/src/tinh-nang/nguyen-vong/NguyenVongDieuTri.jsx` (một hoặc hai bệnh viện, bảo hiểm y tế không bắt buộc, hỏi trước, chọn người cần báo từ người thân có quan hệ Hiệu lực, người đi kèm, ý kiến khác có nguồn; lịch sử phiên bản) cùng test Vitest `NguyenVongDieuTri.test.jsx` (Hành chính chỉ thấy "đã có phiếu") *(2026-10-23: xong; backend `TreatmentWishController`, web `frontend/src/features/wishes/TreatmentWish.jsx` cùng 11 Vitest (tách khỏi `NguyenVongDieuTri.test.jsx` thành `TreatmentWish.test.jsx`). Chưa nghiệm thu trên trình duyệt.)*

**Checkpoint**: Mọi user story của 001 chạy độc lập.

---

## Phase 10: Polish & Cross-Cutting

- [x] T076 *(Bổ sung 2026-10-18, Q-366)* Thêm `phan_hoi_id` vào mẫu cột nguồn ý kiến (data-model mục 6a) và khai báo `YKienDaGhiTuPhanHoi` ở `BE/m01/api/`; test `BT/m01/FR_023g_YKienTuPhanHoiIT.java` (quickstart Y1; research H-17) *(2026-10-24 đợt 4: chốt bộ giá trị `sourceModule = "M04"` (014 thuộc m04, do C làm), `itemType = "INTEREST"`; bản dựng sự kiện từ chối giá trị khác; `FR_023g_OpinionFromFeedbackIT` 5 test kiểm cả đường giao qua kho sự kiện Modulith (người nghe nhận sau khi giao dịch hoàn tất, lưu và đọc lại được JSON, bản ghi giao hoàn tất))*

- [ ] T071 [P] Mở rộng `BT/common/HopDongApiTest.java` so springdoc với `specs/001-elder-profile-assessment/contracts/api-001.yaml`
- [x] T072 [P] Màn danh mục `frontend/src/tinh-nang/danh-muc-m01/` (dị nguyên, mức độ dị ứng, nguồn thông tin, nhu cầu chăm sóc) *(2026-10-24 đợt 4: tab Danh mục theo quyền thật `HEALTH_CATALOGS` từ `GET /me/permissions` thay cho vai trò: ẩn tab khi không có quyền xem; nút Thêm mục theo CREATE, Sửa, Ngừng, Kích hoạt lại theo UPDATE; `AdminPage.test.jsx` 5 test. Vì `PermissionsProvider` của 002 chưa gắn ở `App`, `AdminPage` tự bọc provider; A nên gắn một lần ở `App` (team-plan mục 8). Vẫn chưa nghiệm thu trên trình duyệt)*
- [ ] T073 Khi plan của 003, 004, 005, 006, 007, 010, 011, 012, 013 cài bean `ThamGiaChuyenTrangThai` thật, chạy lại `FR_045_LietKeDieuKienIT`, `FR_047_RollbackTacDongIT` với bean thật (research H-02 mục 4)
- [ ] T074 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [x] T075 Rà truy vết data-model mục 12: DBR-01 → 05, 23, 25, 34 và BR-M01-01 → 15 (BR-M01-14, 15 phần của 001) có test; FR bổ sung 2026-10-10: FR-054 — có task hoặc test tương ứng (constitution XV); *(bổ sung 2026-10-22)* DBR-39, BR-M01-16, FR-047c, SC-014 *(2026-10-24, đợt 3: kiểm lại thấy DBR-23 ghi "có test" quá sớm vì chỉ có test lệnh danh mục; nay có `M01CommandAuditTrailIT`, FR-047c có `HospitalDocumentsIT`, FR-054 có `ResidentNotificationRequestsTest`, `ResidentNotificationsIntakeIT`; xem data-model mục 12)*

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story; Foundational của 001 chặn 003, 004 và mọi feature cài `ThamGiaChuyenTrangThai`
- **US1 (P1)**: sau Foundational
- **US2 (P1)**: sau US1 (cần hồ sơ)
- **US3 (P1)**: sau US1; xác nhận đánh giá gọi 004 qua SPI giả
- **US4 (P1)**: sau US1 và US3 (Hoàn tất tiếp nhận cần đánh giá đầu vào)
- **US5 (P2)**: sau US1; độc lập với US2–US4
- **US6 (P2)**: sau US3, US4
- **US7 (P2)**: sau US1
- Bean giả T002 được thay khi các feature khác triển khai (T073)
- *(2026-10-22)* T083 trước khi 007 làm lệnh Chuyển viện, phiếu tóm tắt; T077 → T080 sau US1 và T079 cần `ThamGiaM01` (T048); T081, T082 sau T047

### Parallel Opportunities

- T007–T009, T011, T013 song song
- Sau US1: US2, US3, US5, US7 song song được; US4 sau US3; US6 sau US4
- Client web và app song song khi controller đã có

## Parallel Example: User Story 4

```text
Task: "T043 BR_M01_04_BangChuyenTest"
Task: "T044 FR_045/FR_047/FR_048 IT"
Task: "T045 BR_M01_05/06, GhiNhanQuaDoiIT"
Task: "T046 BR_M01_01_TamVangQuaHanIT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US3 → US4 (bean giả)
2. **Dừng và kiểm chứng**: tạo hồ sơ, đánh giá đầu vào, Hoàn tất tiếp nhận chạy; `FR_047_RollbackTacDongIT` xanh
3. Chuyển sang plan 003, 004 để thay bean giả

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6 → US7; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- M01 không tự sinh bản ghi nghiệp vụ, không gợi ý mức (Q-306, Q-307); job chỉ báo, nhắc, cảnh báo
- Mọi mốc đọc `ThamSoApi` (CFG-M01-01, 02, 03, 06, CFG-M03-02)
- Lệnh của 001 chỉ chạy trực tuyến
