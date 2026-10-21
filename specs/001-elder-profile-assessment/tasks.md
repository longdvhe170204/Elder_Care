---
description: "Danh sách task triển khai feature 001 – Hồ sơ và đánh giá người cao tuổi"
---

# Tasks: Hồ sơ và đánh giá người cao tuổi (001)

**Input**: Tài liệu thiết kế ở `specs/001-elder-profile-assessment/` (plan.md, spec.md, research.md H-01 → H-16, data-model.md, contracts/api-001.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000 (common, nhật ký, tham số, phê duyệt, đính chính, lưu trữ) và 002 (`@CanQuyen`, `PhamViApi`, `QuyenXemTruongApi`, SPI `QuanHeNguoiThanApi`) đã xong Phase 2.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 12.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; Q-347, Q-348, Q-353): sửa T007, T053, T055 → T057, T059, T067 → T070, T075; thêm T012a, T013a. Mã task cũ giữ nguyên.

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

- [ ] T003 Tạo `MIG/V001_01__tep.sql` và cài `BE/common/tep/internal/TepService.java` (cài `TepApi` của 000 T036): bảng `tep` (`uuid` là khóa đối tượng S3, `ten_goc`, `kieu_mime`, `kich_thuoc`, `sha256`, `tai_len_boi`, `tai_len_luc`, `chu_so_huu_loai`, `chu_so_huu_id`, `gan_luc` – `NULL` tới khi gắn, trigger chặn đổi khi đã gắn); nội dung lưu trong bucket AWS S3 qua `S3Client` (AWS SDK v2; cấu hình `AWS_REGION`, `ELDERCARE_S3_BUCKET`); kiểm kiểu và kích thước khi tải lên; lỗi S3 khi tải lên thì không tạo dòng `tep`; test dùng bản giả `S3Client` trong `BT/support/` (research H-13; NFR-08) *(sửa 2026-10-19: S3 thay volume máy chủ)*
- [ ] T004 [P] Tạo `BE/common/tep/web/TepController.java` cho `POST /tep` và `GET /tep/{id}` (quyền tải qua `QuyenTepProvider` của module sở hữu, nội dung đọc từ S3 qua backend, không phát URL ký sẵn) và job `BE/common/tep/lich/DonTepChuaGan.java` (xóa đối tượng S3 rồi dòng `tep`) theo contracts/api-001.yaml (H-13)
- [ ] T005 Tạo `MIG/V001_02__nguoi_cao_tuoi.sql`: bảng `nguoi_cao_tuoi` theo data-model mục 2 (`ma_ho_so VARCHAR(20) UNIQUE` dạng `NCT-<năm>-<id>`; `ho_so_truoc_id` khóa ngoại tự trỏ; `ho_ten VARCHAR(200) NOT NULL`; cột sinh `ho_ten_so_khop VARCHAR(200) COLLATE utf8mb4_0900_as_ci` và chỉ mục `(ho_ten_so_khop, ngay_sinh)`; `ngay_sinh DATE NOT NULL`; `gioi_tinh ENUM('NAM','NU') NOT NULL`; `loai_giay_to ENUM('CCCD','KHAC') NULL`; `so_giay_to VARCHAR(30) NULL` với `CHECK ((loai_giay_to IS NULL) = (so_giay_to IS NULL))`; cột sinh `cccd_dang_mo` `UNIQUE`; `trang_thai` 8 mã `NOT NULL`; `trang_thai_tu_luc`; `muc_cham_soc_hien_hanh VARCHAR(20) NULL`; `phien_ban`), `lich_su_trang_thai_nct` (nhóm 3 chỉ thêm; `CHECK (ly_do IS NOT NULL OR do_he_thong OR lenh = 'TAO_HO_SO')`; `mien_dieu_kien JSON NULL`; hai trigger), `muc_cham_soc` (nạp `CO_BAN`, `THUONG_XUYEN`, `DAC_BIET`), `lich_su_muc_cham_soc` (`UNIQUE (nguoi_cao_tuoi_id, hieu_luc_tu)`, `can_cu_loai ENUM('DANH_GIA','YEU_CAU_THAY_DOI_LUU_TRU')`)
- [ ] T006 Tạo `MIG/V001_03__danh_muc_m01.sql`: `di_nguyen` (`nhom ENUM('THUOC_HOAT_CHAT','THUC_PHAM','KHAC')`), `muc_do_di_ung` (`thu_tu`; nạp Nhẹ, Trung bình, Nặng), `nguon_thong_tin` (nạp Người cao tuổi, Người thân, Hồ sơ y tế kèm theo, Bác sĩ khám), `nhu_cau_cham_soc` (nạp Hỗ trợ vận động, Theo dõi sức khỏe thường xuyên, Phục hồi sau tai biến, Phục hồi chức năng khác; Q-291) theo mẫu nhóm 1 của 000 (BR-M01-11)
- [x] T007 [P] Tạo kiểu và giao diện công khai trong `BE/m01/api/`: `NguoiCaoTuoiApi`, `TrangThaiNguoiCaoTuoiApi`, `MucChamSocApi`, `DanhGiaApi`, `DanhGiaLaiApi`, `MucSucKhoeApi`, `HoSoSucKhoeApi`, `DiNguyenApi`, `BanDongYApi`, `NguyenVongApi`, `NguonYKienApi` cùng `NguonYKien`, `LoaiNguonYKien` (mục 1.8b, bổ sung 2026-10-18), `NguoiCaoTuoiThongKeApi` (016), enum `TrangThaiNct`, `LenhTrangThai` đúng contracts/module-interfaces.md mục 1.1 → 1.8b *(2026-10-21: tên và chữ ký theo glossary mục 8.5; `isChecked` đổi thành `markChecked`; trường của các view do 001 đặt vì hợp đồng chỉ nêu tên)*
- [x] T008 [P] Khai báo SPI `BE/m01/api/ThamGiaChuyenTrangThai.java` (`lenhQuanTam`, `kiemTraDieuKien`, `mienDieuKien`, `kiemTraChiTiet`, `thucHienTacDong` MANDATORY, `canhBao`) và record `NguCanhLenh`, `DieuKien`, `MienDieuKien`, `KetQuaChuyen`; khai báo SPI M02 cài `YeuCauDoiMucApi`, `HopDongApi`, `LuotVangApi` trong `BE/m01/api/` (contracts mục 1.3, mục 2; tránh vòng M01 ↔ M02)
- [ ] T009 [P] Tạo sự kiện `BE/m01/api/sukien/` theo contracts mục 1.10 (`NguoiCaoTuoiDaTao`, `NguoiCaoTuoiDaChuyenTrangThai`, `BanDongYDaDoi`, `DiUngDaGhiNhan`, `DiUngKhacDaGhiNhan`, `DiUngDaLoaiTru`, `HoSoSucKhoeBanDauDaDoi`, `DanhGiaDauVaoDaXacNhan`, `DanhGiaDaDoiMucHoacNhuCau`, `CanDanhGiaLai`, `QuaHanDanhGiaLai`, `CanLapYeuCauDoiMuc`, `NhacLapYeuCauDoiMuc`, `NhacThieuBanDongY`, `NhacThieuNguyenVong`, `NhacXemLaiNhanDinh` (bổ sung 2026-10-18, FR-023i), `TamVangQuaHan`, `MucChamSocDaDoi`, `ThongTinCaNhanDaCapNhat`); sự kiện gửi 009 cài `CoYeuCauThongBao` với mức, nhóm nhận, loại thông tin theo research H-14; Chuyển viện, Ghi nhận qua đời tới người liên hệ chính khai `doiTuongThongTin = VE_NGUOI_CAO_TUOI` và mã Q-354 `CHUYEN_VIEN` / `QUA_DOI` (009 FR-001a; bổ sung 2026-10-18) *(2026-10-21: đã khai báo 19 sự kiện ở `m01.api.events`. CHƯA xong phần `HasNotificationRequests` (mức, nhóm nhận, `doiTuongThongTin`, Q-354): `RecipientGroup` của 000 mới có `FacilityRole`, `Approvers`; chờ 009 T004 (A) bổ sung nhóm Bác sĩ trực, Điều dưỡng phụ trách, Trưởng tầng, người liên hệ chính)*
- [ ] T010 Cài `BE/m01/trangthai/internal/HoSoMoGuard.java` (chặn ghi mới trên hồ sơ ở trạng thái cuối) và test kiến trúc `BT/m01/HoSoMoGuardArchTest.java` buộc mọi `@LenhNghiepVu` ghi vào hồ sơ gọi guard (BR-M01-05)
- [ ] T011 [P] Cài provider cho 000/002 trong `BE/m01/hoso/internal/`: `PhanLoaiTruongProvider` theo bảng data-model mục 10 (gồm `qdToa*` của bản đồng ý → `NHAN_DANG`, bổ sung 2026-10-18), `GanPhamViProvider` cho `M01.NGUOI_CAO_TUOI` và bản ghi con, `QuyenTepProvider` cho `M01.ANH_NGUOI_CAO_TUOI`, `M01.BAN_DONG_Y`, `M01.NGUYEN_VONG`, `M01.QUA_DOI`, `LuuTruProvider` theo data-model mục 11 (`THEO_HO_SO`, hồ sơ có hồ sơ sau trỏ tới không bị loại trước)
- [ ] T012 Cài lệnh danh mục chung M01 (tạo, sửa, ngừng hiệu lực, kích hoạt lại; người cấu hình theo data-model mục 5 "Danh mục") trong `BE/m01/suckhoe/internal/DanhMucM01Service.java` và `BE/m01/suckhoe/web/DanhMucController.java` cho `/danh-muc/{loai}`, `/{id}`, `/{id}/ngung-hieu-luc`, `/{id}/kich-hoat-lai`; test `BT/m01/BR_M01_11_DanhMucIT.java`
- [ ] T012a Cài `BE/m01/hoso/internal/NguonYKienService.java` (cài `NguonYKienApi.kiemTra`: `NGUOI_THAN` thì người thân phải có quan hệ Hiệu lực lúc gọi qua `QuanHeNguoiThanApi.thuocNguoiCaoTuoi`, loại khác thì `nguoiThanId` phải rỗng; sai → 422 `NGUON_Y_KIEN_KHONG_HOP_LE`) và test `BT/m01/BR_M01_14_NguonYKienIT.java` viết trước; 004, 005, 012, 014 dùng giao diện này (FR-023g, BR-M01-14, research H-16; bổ sung 2026-10-18)
- [ ] T013 [P] Test `BT/m01/NhatKyDayDuM01IT.java`: mọi lệnh M01 có một `INSERT nhat_ky` với `nguoi_cao_tuoi_id` (DBR-23)
- [ ] T013a [P] Test kiến trúc `BT/m01/FR_023h_KhongDauNangLucArchTest.java`: quét `information_schema.COLUMNS` của schema test và OpenAPI sinh từ springdoc; báo lỗi khi bảng có `nguoi_cao_tuoi_id` (hoặc chính `nguoi_cao_tuoi`) hay schema người cao tuổi có cột, trường chứa `nang_luc` / `nangLuc` (danh sách cho phép rỗng; `tu_quyet_duoc` chỉ ở phiếu nguyện vọng của 001 và mục thỏa thuận của 005) (FR-023h, data-model mục 6a; bổ sung 2026-10-18)

**Checkpoint**: Hồ sơ và SPI sẵn sàng; module khác có thể cài `ThamGiaChuyenTrangThai`.

---

## Phase 3: User Story 1 – Hành chính tạo hồ sơ và nhân viên tra cứu theo quyền (Priority: P1) 🎯 MVP

**Goal**: Tạo hồ sơ, chống trùng CCCD, cảnh báo trùng họ tên + ngày sinh, liên kết hồ sơ của người quay lại, cập nhật thông tin cá nhân, tra cứu theo phạm vi và giới hạn trường (FR-001 → FR-009, DBR-01).

**Independent Test**: Tạo hồ sơ đủ thông tin, thử hồ sơ thứ hai trùng CCCD, đăng nhập từng vai trò để tra cứu và kiểm phần hiển thị.

### Tests cho User Story 1

- [ ] T014 [P] [US1] Test `BT/m01/TaoHoSoIT.java`, `HoSoCccdIT.java`, `HoSoTrungTenIT.java`: CCCD trùng hồ sơ chưa ở trạng thái cuối → 409; trùng hồ sơ trạng thái cuối được phép; trùng họ tên + ngày sinh yêu cầu xác nhận không trùng (`NCT_XAC_NHAN_KHONG_TRUNG`) (US1 #1–5; DBR-01, FR-003, FR-005)
- [ ] T015 [P] [US1] Test `BT/m01/HoSoLienKetIT.java` và `CapNhatThongTinIT.java`: liên kết hồ sơ trước; cập nhật với `If-Match` cũ → 412; đổi giới tính phát `ThongTinCaNhanDaCapNhat` (FR-004, FR-006)
- [ ] T016 [P] [US1] Test `BT/m01/TraCuuPhamViIT.java` và `GioiHanTruongHoSoIT.java`: nhân viên ngoài phạm vi → 404; Hành chính không thấy trường sức khỏe (US1 #6–10; FR-007 → FR-009)

### Implementation cho User Story 1

- [ ] T017 [P] [US1] Tạo entity `BE/m01/hoso/internal/NguoiCaoTuoi.java` (`@NhomDuLieu(NGHIEP_VU)`, không có setter trạng thái) và `LichSuTrangThaiNct.java` (`GHI_NHAN`, `ChiThemRepository`)
- [ ] T018 [US1] Cài `BE/m01/hoso/internal/HoSoService.java`: tạo hồ sơ (`TAO_HO_SO` → `DANG_TIEP_NHAN`, dòng lịch sử đầu, `NCT_TAO`), xác nhận không trùng, liên kết hồ sơ trước, cập nhật thông tin cá nhân (`NCT_CAP_NHAT`, `If-Match`); ảnh qua `TepApi.gan` (research H-04)
- [ ] T019 [US1] Cài tra cứu `BE/m01/hoso/internal/TraCuuNguoiCaoTuoi.java` dùng `PhamViApi.boLoc` và chỉ mục `(ho_ten_so_khop, ngay_sinh)`, `ma_ho_so`, `cccd_dang_mo`; DTO có `@NhomTruong` (research H-05; NFR-02)
- [ ] T020 [US1] Tạo `BE/m01/hoso/web/NguoiCaoTuoiController.java`: `POST/GET /nguoi-cao-tuoi`, `GET /nguoi-cao-tuoi/{id}` (kèm `lenhDuocPhep`), `POST …/{id}/cap-nhat-thong-tin`, `GET …/{id}/ho-so-lien-ket` theo contracts/api-001.yaml
- [ ] T021 [US1] Cài `NguoiCaoTuoiApi` (`trangThai`, `nhanDang`, …) trong `BE/m01/hoso/internal/NguoiCaoTuoiApiImpl.java` cho 002 và các module khác
- [ ] T022 [P] [US1] Tạo màn web `frontend/src/tinh-nang/ho-so-nguoi-cao-tuoi/TraCuu.jsx`, `TaoHoSo.jsx`, `ChiTietHoSo.jsx` (hiển thị `truongBiAn`, xác nhận trùng) và test `HoSo.test.jsx`, `XacNhanTrung.test.jsx`
- [ ] T023 [P] [US1] Tạo `mobile/lib/nhan-vien/tra-cuu-nguoi-cao-tuoi/` (tra cứu, thẻ người cao tuổi) và `mobile/lib/nguoi-than/ho-so/` (xem hồ sơ, phần sức khỏe theo chú thích ¹)

**Checkpoint**: Tạo và tra cứu hồ sơ chạy độc lập.

---

## Phase 4: User Story 2 – Hồ sơ sức khỏe ban đầu; dị ứng và bệnh nền là từng mục chỉ loại trừ (Priority: P1)

**Goal**: Hồ sơ sức khỏe ban đầu nhóm 3 có nháp; mục sức khỏe nội dung bất biến, chỉ loại trừ có lý do; kiểm tra dị ứng sau commit có cờ chờ kiểm tra (FR-017 → FR-023, DBR-04, BR-M01-07, BR-M01-11, BR-M01-12).

**Independent Test**: Ghi 2 dị ứng và 1 bệnh nền, loại trừ 1 dị ứng, thử xóa và sửa nội dung; kiểm danh sách hiện hành, đã loại trừ và nhật ký.

### Tests cho User Story 2

- [ ] T024 [P] [US2] Test `BT/m01/MucSucKhoeBatBienIT.java`: `UPDATE` nội dung và `DELETE` bị trigger chặn; `DA_LOAI_TRU → HIEU_LUC` bị chặn; không có endpoint sửa, xóa (DBR-04, SC-003)
- [ ] T025 [P] [US2] Test `BT/m01/BR_M01_07_DiUngIT.java`, `BR_M01_12_ChoKiemTraIT.java`, `MucSucKhoeTrungIT.java`: dị ứng thiếu mức độ → 422; dị ứng không có dị nguyên đánh dấu "không kiểm tra tự động"; 006/011 lỗi thì mục vẫn lưu, cờ chờ kiểm tra còn và job phát lại; trùng → 409 `MUC_SUC_KHOE_TRUNG` trừ khi `xacNhanTrung` (US2 #1–8; FR-018a, FR-021)
- [ ] T026 [P] [US2] Test `BT/m01/HoSoSucKhoeBanDauIT.java`: nháp sửa được, xác nhận chỉ AC-03, sau xác nhận trigger chặn sửa; đính chính `M01.HO_SO_SUC_KHOE_BAN_DAU` phát `HoSoSucKhoeBanDauDaDoi` (FR-017)

### Implementation cho User Story 2

- [ ] T027 [US2] Tạo `MIG/V001_04__suc_khoe.sql`: `ho_so_suc_khoe_ban_dau` (`UNIQUE (nguoi_cao_tuoi_id)`, trigger chặn `UPDATE` khi `OLD.thoi_diem_xac_nhan IS NOT NULL`), `muc_suc_khoe` (`loai ENUM('DI_UNG','BENH_NEN','TIEN_SU')`; `noi_dung_van_ban VARCHAR(1000) NULL` với `CHECK (di_nguyen_id IS NOT NULL OR noi_dung_van_ban IS NOT NULL)`; cột sinh `khong_kiem_tra_tu_dong`; `CHECK (loai <> 'DI_UNG' OR muc_do_id IS NOT NULL)`; `trang_thai ENUM('HIEU_LUC','DA_LOAI_TRU')`; ba cột loại trừ bắt buộc khi `DA_LOAI_TRU`; `cho_kiem_tra_thuoc`, `cho_kiem_tra_an`; trigger `trg_muc_suc_khoe_noi_dung_bat_bien`, `trg_muc_suc_khoe_khong_xoa`) (data-model mục 5)
- [ ] T028 [P] [US2] Tạo entity `BE/m01/suckhoe/internal/HoSoSucKhoeBanDau.java`, `MucSucKhoe.java`
- [ ] T029 [US2] Cài `BE/m01/suckhoe/internal/HoSoSucKhoeService.java`: lập, sửa nháp (`If-Match`), xác nhận (AC-03); `DinhChinhDuocProvider` cho `M01.HO_SO_SUC_KHOE_BAN_DAU` (`tangKhuVuc` rỗng, trên hồ sơ trạng thái cuối chỉ cách C; FR-049a) (research H-08)
- [ ] T030 [US2] Cài `BE/m01/suckhoe/internal/MucSucKhoeService.java`: ghi nhận (danh mục `HIEU_LUC`, trùng, `HoSoMoGuard`), loại trừ có lý do; đặt cờ chờ kiểm tra và phát `DiUngDaGhiNhan`/`DiUngKhacDaGhiNhan`/`DiUngDaLoaiTru` sau commit; nhận kết quả kiểm tra thì ghi `MSK_DA_KIEM_TRA` (research H-07)
- [ ] T031 [US2] Cài job `BE/m01/lich/ThuLaiKiemTraDiUng.java` (15 phút, ShedLock): phát lại `DiUngDaGhiNhan` cho mục còn cờ quá 15 phút (BR-M01-12; research H-12)
- [ ] T032 [US2] Tạo `BE/m01/suckhoe/web/SucKhoeController.java`: `PUT /nguoi-cao-tuoi/{id}/ho-so-suc-khoe-ban-dau`, `POST …/xac-nhan`, `POST /ho-so-suc-khoe-ban-dau/{id}/dinh-chinh`, `POST/GET /nguoi-cao-tuoi/{id}/muc-suc-khoe`, `POST /muc-suc-khoe/{id}/loai-tru`; cài `MucSucKhoeApi`, `HoSoSucKhoeApi`, `DiNguyenApi`
- [ ] T033 [P] [US2] Tạo màn web `frontend/src/tinh-nang/suc-khoe/HoSoBanDau.jsx`, `MucSucKhoe.jsx` và app `mobile/lib/nhan-vien/suc-khoe/` (ghi mục sức khỏe trực tuyến)

**Checkpoint**: Sức khỏe ban đầu và dị ứng chạy độc lập.

---

## Phase 5: User Story 3 – Bác sĩ đánh giá đầu vào bằng nhận xét và chọn mức chăm sóc (Priority: P1)

**Goal**: Lần đánh giá nhóm 3 có nháp, một nháp mỗi người, xác nhận chỉ Bác sĩ với nhận xét và mức; không có điểm hay gợi ý; lưu ý chăm sóc hiện hành cho thẻ người cao tuổi (FR-024 → FR-038, DBR-05, BR-M01-09, BR-M01-10, BR-M01-13).

**Independent Test**: Lập đánh giá đầu vào; xác nhận thiếu nhận xét, thiếu mức, bởi điều dưỡng (đều bị từ chối); xác nhận hợp lệ rồi kiểm mức hiện hành, lưu ý chăm sóc, và không có nháp kế hoạch chăm sóc tự sinh.

### Tests cho User Story 3

- [ ] T034 [P] [US3] Test `BT/m01/BR_M01_09_DanhGiaIT.java` và `XacNhanDanhGiaIT.java`: thiếu nhận xét hoặc mức → 422 và `CHECK` ở MySQL chặn; Điều dưỡng xác nhận → 403; xác nhận đặt `muc_cham_soc_hien_hanh` và dòng `lich_su_muc_cham_soc` cùng transaction; không tạo bản ghi kế hoạch chăm sóc (DBR-05, Q-306)
- [ ] T035 [P] [US3] Test `BT/m01/BR_M01_13_MotBanNhapIT.java`: nháp thứ hai → 409 `DA_CO_BAN_NHAP`; hủy nháp có lý do rồi tạo được; đầu vào Đã xác nhận thứ hai bị `UNIQUE` chặn
- [ ] T036 [P] [US3] Test `BT/m01/BR_M01_10_LuuYChamSocIT.java` và `DinhChinhDanhGiaIT.java`: `DanhGiaApi.luuYHienHanh` theo lô; đính chính đánh giá thêm lần báo `DINH_CHINH_DANH_GIA` khi đổi mức (research H-10)

### Implementation cho User Story 3

- [ ] T037 [US3] Tạo `MIG/V001_05__danh_gia.sql`: `danh_gia` theo data-model mục 7 (`loai ENUM('DAU_VAO','DANH_GIA_LAI')`; `loai_luu_tru_de_xuat ENUM('NOI_TRU_DAI_HAN','NOI_TRU_NGAN_NGAY','BAN_TRU') NULL` chỉ với `DAU_VAO`; `trang_thai ENUM('NHAP','DA_XAC_NHAN','DA_HUY')`; cột sinh `nhap_dang_mo`, `dau_vao_da_xac_nhan` có `UNIQUE`; `CHECK (trang_thai <> 'DA_XAC_NHAN' OR (nhan_xet IS NOT NULL AND CHAR_LENGTH(TRIM(nhan_xet)) > 0 AND muc_cham_soc IS NOT NULL))`; trigger `trg_danh_gia_khong_sua`) và `danh_gia_nhu_cau` (trigger chặn `INSERT`/`DELETE` khi cha không còn `NHAP`)
- [ ] T038 [P] [US3] Tạo entity `BE/m01/danhgia/internal/DanhGia.java`, `DanhGiaNhuCau.java`, `LichSuMucChamSoc.java`
- [ ] T039 [US3] Cài `BE/m01/danhgia/internal/DanhGiaService.java`: tạo (`DAU_VAO` chỉ khi `DANG_TIEP_NHAN` và chưa có đầu vào xác nhận; `DANH_GIA_LAI` theo FR-035), sửa nháp, hủy nháp, xác nhận với tác động cùng transaction theo research H-10 (đặt mức hiện hành, `lich_su_muc_cham_soc`, `MUC_CS_DOI`, gọi `YeuCauDoiMucApi.thayTheBoiDanhGia` MANDATORY, đáp ứng lần báo đánh giá lại, phát `DanhGiaDauVaoDaXacNhan`, `DanhGiaDaDoiMucHoacNhuCau`, `MucChamSocDaDoi`); gợi ý chỉ số gần nhất qua `ChiSoApi.ganNhat` để hiển thị
- [ ] T040 [US3] Cài `MucChamSocApi`, `DanhGiaApi` (`luuYHienHanh` theo lô) và `DinhChinhDuocProvider` cho `M01.DANH_GIA` trong `BE/m01/danhgia/internal/`
- [ ] T041 [US3] Tạo `BE/m01/danhgia/web/DanhGiaController.java`: `POST /nguoi-cao-tuoi/{id}/danh-gia`, `GET/PUT /danh-gia/{id}`, `POST /danh-gia/{id}/xac-nhan`, `/huy-nhap`, `/dinh-chinh`, `GET /nguoi-cao-tuoi/{id}/muc-cham-soc`, `/lich-su-muc-cham-soc`
- [ ] T042 [P] [US3] Tạo màn web `frontend/src/tinh-nang/danh-gia/LapDanhGia.jsx` (không có ô điểm, không có mức đề xuất) và test `DanhGia.test.jsx`; app `mobile/lib/nhan-vien/danh-gia/` (lập nháp); thẻ người cao tuổi hiển thị lưu ý chăm sóc và test `mobile/test/nhan-vien/the_nguoi_cao_tuoi_test.dart`

**Checkpoint**: Đánh giá đầu vào và mức chăm sóc chạy độc lập.

---

## Phase 6: User Story 4 – Trạng thái người cao tuổi chỉ thay đổi qua lệnh nghiệp vụ có điều kiện (Priority: P1)

**Goal**: Một điểm chuyển trạng thái, khóa dòng, gộp điều kiện từ mọi bean, tác động trong cùng transaction, endpoint riêng mỗi lệnh, API cho 007, 014 (FR-044 → FR-050, BR-M01-01, BR-M01-04 → 06).

**Independent Test**: Đi hết chuyển hợp lệ, thử mọi cặp không có trong bảng, thử từng lệnh khi thiếu từng điều kiện (bean giả của module khác).

### Tests cho User Story 4

- [ ] T043 [P] [US4] Test đơn vị `BT/m01/BR_M01_04_BangChuyenTest.java`: mọi cặp (trạng thái, lệnh) của bảng chuyển data-model mục 2; cặp ngoài bảng → `CHUYEN_KHONG_HOP_LE`
- [ ] T044 [P] [US4] Test `BT/m01/FR_045_LietKeDieuKienIT.java`, `FR_047_RollbackTacDongIT.java`, `FR_048_LenhDongThoiIT.java`: liệt kê đủ điều kiện chưa thỏa trong một lần; một bean ném lỗi thì cả lệnh rollback; hai lệnh đồng thời chỉ một thành công; `nguoi_cao_tuoi.trang_thai` luôn bằng dòng lịch sử mới nhất
- [ ] T045 [P] [US4] Test `BT/m01/BR_M01_05_HoSoDongIT.java`, `BR_M01_06_LenhRiengIT.java`, `GhiNhanQuaDoiIT.java`, `LichSuTrangThaiIT.java`: hồ sơ trạng thái cuối chặn ghi mới; lệnh từ 014 có `do_he_thong`; AC-09 ghi nhận qua đời chỉ khi khác `DANG_LUU_TRU` và có `bangChungTepId`; yêu cầu ngoại lệ đã duyệt của 004 miễn điều kiện và ghi `mien_dieu_kien` (US4 #1–13; FR-047b)
- [ ] T046 [P] [US4] Test `BT/m01/BR_M01_01_TamVangQuaHanIT.java` (đồng hồ giả lập): quá mốc dự kiến + CFG-M01-01 cảnh báo đúng một lần mỗi mốc; gia hạn tạo mốc mới

### Implementation cho User Story 4

- [ ] T047 [US4] Cài `BE/m01/trangthai/internal/ChuyenTrangThaiService.java`: khóa `SELECT … FOR UPDATE`, `HoSoMoGuard`, kiểm bảng chuyển, quyền theo khóa ở data-model mục 2, gọi `kiemTraChiTiet`, gộp `kiemTraDieuKien` của mọi bean `lenhQuanTam` chứa lệnh, áp `mienDieuKien` của 004, ghi `lich_su_trang_thai_nct` + `NCT_CHUYEN`, gọi `thucHienTacDong` của mọi bean trong cùng transaction, gom `canhBao`, phát `NguoiCaoTuoiDaChuyenTrangThai` (research H-02)
- [ ] T048 [US4] Cài tác động riêng M01: Hoàn tất tiếp nhận kiểm đánh giá đầu vào Đã xác nhận chưa quá CFG-M01-02 (FR-034a); Ghi nhận trở về từ bệnh viện đặt `han_tinh_tu` và thêm lần báo `TRO_VE_BENH_VIEN` (research H-11) trong `BE/m01/trangthai/internal/ThamGiaM01.java`
- [ ] T049 [US4] Cài `TrangThaiNguoiCaoTuoiApi` trong `BE/m01/trangthai/internal/TrangThaiApiImpl.java`: `DIEM_DANH_ROI_VIEN`, `DIEM_DANH_VE`, `HUY_GHI_NHAN_ROI_VIEN` cho 014 (`do_he_thong`), `CHUYEN_VIEN` cho 007, `xemDieuKien` cho 004 (research H-03)
- [ ] T050 [US4] Tạo `BE/m01/trangthai/web/TrangThaiController.java`: `GET /nguoi-cao-tuoi/{id}/lich-su-trang-thai`, `GET …/dieu-kien/{lenh}`, `POST …/hoan-tat-tiep-nhan`, `/huy-tiep-nhan`, `/cho-tam-vang`, `/ghi-nhan-tro-ve`, `/chuyen-vien`, `/ket-thuc-luu-tru`, `/ghi-nhan-qua-doi` (thân `lyDo`, `chiTiet`) theo contracts/api-001.yaml; `DinhChinhDuocProvider` cho `M01.LICH_SU_TRANG_THAI`
- [ ] T051 [US4] Tạo `MIG/V001_06__canh_bao_tro_lai.sql` (`(luot_vang_id, moc_du_kien)` khóa chính, `canh_bao_luc`) và cài job `BE/m01/lich/CanhBaoQuaHanTroLai.java` dùng `LuotVangApi.dangMo()` và CFG-M01-01, phát `TamVangQuaHan` (BR-M01-01, FR-050)
- [ ] T052 [P] [US4] Tạo màn web lệnh trạng thái trong `frontend/src/tinh-nang/ho-so-nguoi-cao-tuoi/LenhTrangThai.jsx` (xem trước điều kiện, hiển thị `dieuKienChuaThoa[]`, cảnh báo) và test `DieuKienChuaThoa.test.jsx`; app `mobile/lib/nhan-vien/trang-thai/` (Cho tạm vắng, Ghi nhận trở về, Chuyển viện, Ghi nhận qua đời; chỉ trực tuyến)

**Checkpoint**: Máy trạng thái chạy với bean giả; P1 hoàn tất.

---

## Phase 7: User Story 5 – Hành chính ghi nhận và thu hồi bản đồng ý chia sẻ dữ liệu (Priority: P2)

**Goal**: Bản đồng ý có bằng chứng, tối đa một bản Hiệu lực, thay thế và rút lại; là căn cứ chú thích ¹; nhắc khi thiếu (FR-010 → FR-016, BR-M01-08, DBR-03).

**Independent Test**: Bản đồng ý gồm người thân X không gồm Y → X thấy sức khỏe, Y không; rút lại → X mất quyền ngay.

### Tests cho User Story 5

- [ ] T053 [P] [US5] Test `BT/m01/BanDongYIT.java` và `RutLaiDongYIT.java`: thiếu tệp → 422; bản mới thay bản cũ (`BDY_THAY_THE`); `DAI_DIEN_PHAP_LUAT` thiếu một trong bốn trường quyết định của Tòa án → 422 `THIEU_QUYET_DINH_TOA_AN`; người ký thay không có quan hệ Hiệu lực → 409 `NGUOI_KY_KHONG_HOP_LE`; người đại diện ở 012 mà không có quyết định của Tòa án bị từ chối (US5 #3, #10, lớp `FR_011_DaiDienPhapLuatIT.java`, sửa 2026-10-18); rút lại được cả trên hồ sơ trạng thái cuối; trigger chỉ cho đổi cột rút lại theo chiều `HIEU_LUC → DA_RUT_LAI` (FR-010 → FR-016)
- [ ] T054 [P] [US5] Test `BT/m01/BR_M01_08_DongYIT.java` và `NhacThieuDongYIT.java` (đồng hồ giả lập): điều kiện chú thích ¹ kiểm mỗi yêu cầu; nhắc Hành chính mỗi CFG-M01-06 khi thiếu bản Hiệu lực (BR-M01-08, DBR-03, FR-012)

### Implementation cho User Story 5

- [ ] T055 [US5] Tạo `MIG/V001_07__ban_dong_y.sql`: `ban_dong_y` theo data-model mục 4 (`nguoi_dong_y_loai ENUM('NGUOI_CAO_TUOI','DAI_DIEN_PHAP_LUAT')`; `nguoi_dong_y_nguoi_than_id` "Bắt buộc khi `DAI_DIEN_PHAP_LUAT` (`CHECK`)"; `qd_toa_so VARCHAR(100)`, `qd_toa_ngay DATE`, `qd_toa_toa_an VARCHAR(200)`, `qd_toa_tep_id BIGINT` "Bắt buộc cả bốn khi `DAI_DIEN_PHAP_LUAT`, rỗng cả bốn khi `NGUOI_CAO_TUOI` (`CHECK`)"; không có `ly_do_dai_dien` (sửa 2026-10-18, Q-348); `bang_chung_tep_id NOT NULL`; `trang_thai ENUM('HIEU_LUC','DA_RUT_LAI')`; cột sinh `dang_hieu_luc` `UNIQUE`; trigger chỉ cho cột rút lại và thay thế), `ban_dong_y_nguoi_than` (khóa chính `(ban_dong_y_id, nguoi_than_id)`, chỉ thêm) và `nhac_chu_ky` (khóa chính `(loai, nguoi_cao_tuoi_id, moc_id)`, `loai ENUM('THIEU_BAN_DONG_Y','THIEU_NGUYEN_VONG','LAP_YEU_CAU_DOI_MUC','XEM_LAI_NHAN_DINH')`)
- [ ] T056 [US5] Cài `BE/m01/dongy/internal/BanDongYService.java` (ghi nhận, thay thế, rút lại; người ký thay kiểm `QuanHeNguoiThanApi.thuocNguoiCaoTuoi`, không gọi `laNguoiDaiDienHieuLuc`; gắn tệp quyết định của Tòa án; `BanDongYApi` gồm `daiDienPhapLuat` cho 012 FR-020c; phát `BanDongYDaDoi`) và `DieuKienChuThich("1")` trong `BE/m01/dongy/internal/DieuKienDongY.java` (research H-06)
- [ ] T057 [US5] Tạo `BE/m01/dongy/web/BanDongYController.java`: `POST/GET /nguoi-cao-tuoi/{id}/ban-dong-y` (thân lệnh có `quyetDinhToaAn`), `POST /ban-dong-y/{id}/rut-lai`
- [ ] T058 [US5] Cài job `BE/m01/lich/NhacThieuDongYNguyenVong.java` (phần bản đồng ý: trạng thái Đang lưu trú, Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện; mỗi CFG-M01-06; dòng `nhac_chu_ky` thêm khi Hoàn tất tiếp nhận thiếu bản đồng ý) (FR-012, SC-013)
- [ ] T059 [P] [US5] Tạo màn web `frontend/src/tinh-nang/ban-dong-y/BanDongY.jsx` (người ký: người cao tuổi hoặc người đại diện theo pháp luật; chọn người ký thay thì hiện bốn ô quyết định của Tòa án và tải bản scan; nhãn "nhận thông báo không khẩn về người cao tuổi")

**Checkpoint**: Bản đồng ý điều khiển quyền xem sức khỏe của người thân.

---

## Phase 8: User Story 6 – Đánh giá lại theo định kỳ, sau sự cố, sau nằm viện; đổi mức thì Hành chính lập yêu cầu (Priority: P2)

**Goal**: Báo đánh giá lại (định kỳ CFG-M01-02, sau sự cố, sau trở về từ bệnh viện), cảnh báo quá hạn CFG-M01-03, nhắc Hành chính lập yêu cầu đổi mức (CFG-M03-02); không tự tạo yêu cầu (FR-039 → FR-043, FR-036, FR-053, BR-M01-02, BR-M01-03).

**Independent Test**: Tua tới hạn CFG-M01-02 → bác sĩ được báo; đánh giá lại khác mức → Hành chính được báo; quá CFG-M01-03 → cảnh báo.

### Tests cho User Story 6

- [ ] T060 [P] [US6] Test `BT/m01/BR_M01_02_BaoDanhGiaLaiIT.java` (đồng hồ giả lập): báo định kỳ; sự cố `canDanhGiaLai` báo một lần; đang Điều trị tại bệnh viện thì `han_tinh_tu` rỗng; `UNIQUE` chống báo lặp; cảnh báo quá hạn một lần (US6 #1–8)
- [ ] T061 [P] [US6] Test `BT/m01/BR_M01_03_DoiMucIT.java`, `NhacLapYeuCauDoiMucIT.java`, `FR_053_MucTaiNgayIT.java`, `DocLapLuuTruMucChamSocIT.java`: đánh giá lại khác mức phát `CanLapYeuCauDoiMuc`, không tự tạo yêu cầu; nhắc mỗi CFG-M03-02 tới khi 004 có yêu cầu chưa kết thúc; mức tại ngày D đúng lịch sử; áp dụng yêu cầu thay đổi lưu trú ghi `lich_su_muc_cham_soc` `YEU_CAU_THAY_DOI_LUU_TRU` từ 00:00 ngày hiệu lực (US6 #9–12; FR-051 → FR-053)

### Implementation cho User Story 6

- [ ] T062 [US6] Tạo `MIG/V001_08__lan_bao_danh_gia_lai.sql`: `lan_bao_danh_gia_lai` (`can_cu ENUM('DINH_KY','SAU_SU_CO','TRO_VE_BENH_VIEN','DINH_CHINH_DANH_GIA')`, `han_tinh_tu DATETIME(6) NULL`, `da_canh_bao_qua_han_luc`, `UNIQUE (nguoi_cao_tuoi_id, can_cu, nguon_loai, nguon_id)`)
- [ ] T063 [US6] Cài `BE/m01/danhgia/internal/BaoDanhGiaLaiService.java` ("cần đánh giá lại" tính từ lần báo chưa được đáp ứng; Q-307, research H-11), listener `SuCoDaGhiNhan` (khi `canDanhGiaLai`), `DanhGiaLaiApi`
- [ ] T064 [US6] Cài job `BE/m01/lich/BaoDanhGiaLaiDinhKy.java`, `CanhBaoQuaHanDanhGiaLai.java`, `NhacLapYeuCauDoiMuc.java` (ShedLock, 15 phút; CFG-M01-02, CFG-M01-03, CFG-M03-02 đọc mỗi lần chạy; `lan_chay_lich`) (research H-12)
- [ ] T065 [US6] Cài lệnh ghi mức từ yêu cầu thay đổi lưu trú (004 gọi khi áp dụng, `MANDATORY`) trong `MucChamSocApi` và cập nhật `lich_su_muc_cham_soc` (FR-052, FR-053)
- [ ] T066 [US6] Tạo endpoint `GET /nguoi-cao-tuoi/{id}/danh-gia-lai`, `GET /danh-gia-lai/can-thuc-hien` trong `BE/m01/danhgia/web/DanhGiaLaiController.java` và màn web `frontend/src/tinh-nang/danh-gia/CanDanhGiaLai.jsx`

**Checkpoint**: Vòng đánh giá lại chạy với đồng hồ giả lập.

---

## Phase 9: User Story 7 – Phiếu nguyện vọng cuối đời khi tiếp nhận (Priority: P2)

**Goal**: Phiếu có phiên bản, tối đa một bản Hiệu lực, người ký theo năng lực, giới hạn xem, nhắc khi thiếu (FR-023a → FR-023f, DBR-34).

**Independent Test**: Hoàn tất tiếp nhận người chưa có phiếu → cảnh báo, nhắc; ghi phiếu, ghi phiên bản mới; Hành chính xem phiếu bị chặn.

### Tests cho User Story 7

- [ ] T067 [P] [US7] Test `BT/m01/NguyenVongPhienBanIT.java`, `NguyenVongNguoiKyIT.java`, `NguyenVongGioiHanXemIT.java`, `NhacThieuNguyenVongIT.java`: phiên bản mới thay bản cũ, `UNIQUE dang_hieu_luc`; `CHECK (tu_quyet_duoc = (nguoi_ky_loai = 'NGUOI_CAO_TUOI'))`; thiếu `cachHoTroHieu` hoặc `ngayXemLai` → 422; `DAI_DIEN_NGUOI_BENH` thiếu `cachChonDaiDien` → 422; `DAI_DIEN_PHAP_LUAT` thiếu quyết định của Tòa án → 422; người ký thay không có quan hệ Hiệu lực ở 012 → 409; ý kiến khác lưu từng dòng có nguồn, nguồn sai → 422 `NGUON_Y_KIEN_KHONG_HOP_LE` (US7 #2, #3, #7; sửa 2026-10-18); Hành chính không xem được nhóm `NGUYEN_VONG`; nhắc Bác sĩ/Điều dưỡng phụ trách mỗi CFG-M01-06; `NhacXemLaiNhanDinhIT` (đồng hồ giả lập): tới ngày xem lại nhắc một lần Bác sĩ đã nhận định và Điều dưỡng phụ trách, chạy lại không nhắc trùng, phiếu bị thay trước ngày đó không nhắc (Q37; FR-023i) (DBR-34; FR-023a → FR-023f, FR-023i)

### Implementation cho User Story 7

- [ ] T068 [US7] Tạo `MIG/V001_09__nguyen_vong.sql`: `nguyen_vong_cuoi_doi` theo data-model mục 6 (`lua_chon ENUM('CHUYEN_BENH_VIEN','DUA_VE_NHA','O_LAI_GIAM_NHE')`; `nguoi_tra_loi_loai`, `nguoi_ky_loai ENUM('NGUOI_CAO_TUOI','DAI_DIEN_NGUOI_BENH','DAI_DIEN_PHAP_LUAT')`; `tu_quyet_duoc BOOLEAN`, `thoi_diem_nhan_dinh DATETIME(6)`, `cach_ho_tro_hieu TEXT` "`NOT NULL`, không rỗng", `ngay_xem_lai_nhan_dinh DATE` "`NOT NULL`; `>= DATE(thoi_diem_nhan_dinh)` (`CHECK`)"; `cach_chon_dai_dien ENUM('NCT_DA_CHON','GIA_DINH_CHON') NULL` "Bắt buộc khi `DAI_DIEN_NGUOI_BENH`, rỗng khi khác (`CHECK`)"; `qd_toa_*` bắt buộc cả bốn khi `DAI_DIEN_PHAP_LUAT`; không có `du_nang_luc`; `ban_ky_tep_id NOT NULL`; `nguon ENUM('TIEP_NHAN','YEU_CAU_GIA_DINH','XAC_NHAN_LAI_NGUY_KICH')`; `trang_thai ENUM('HIEU_LUC','DUOC_THAY_THE')`; cột sinh `dang_hieu_luc` `UNIQUE`; trigger chỉ cho `HIEU_LUC → DUOC_THAY_THE`) và `nguyen_vong_y_kien` (chỉ thêm; mẫu cột nguồn data-model mục 6a: `nguon_loai ENUM('NGUOI_CAO_TUOI','NGUOI_THAN','NHAN_VIEN_QUAN_SAT')`, `CHECK ((nguon_loai = 'NGUOI_THAN') = (nguon_nguoi_than_id IS NOT NULL))`; `noi_dung TEXT NOT NULL`; không cấp `UPDATE`, `DELETE`)
- [ ] T069 [US7] Cài `BE/m01/nguyenvong/internal/NguyenVongService.java` (ghi phiên bản mới; `NguyenVongApi.ghiPhienBan` cho 007; bác sĩ nhận định có AC-03 lúc ghi; nhận định gắn với phiếu, người ký theo FR-023c qua `thuocNguoiCaoTuoi`; ý kiến khác kiểm bằng `NguonYKienApi` rồi ghi `nguyen_vong_y_kien` cùng transaction; ngày xem lại chỉ lưu, không nhắc; `NV_GHI`) và phần nguyện vọng của job `NhacThieuDongYNguyenVong`, job `BE/m01/lich/NhacXemLaiNhanDinh.java` (ShedLock, 15 phút; `nhac_chu_ky` `XEM_LAI_NHAN_DINH`; FR-023i) (research H-09, H-12)
- [ ] T070 [US7] Tạo `BE/m01/nguyenvong/web/NguyenVongController.java` cho `POST/GET /nguoi-cao-tuoi/{id}/nguyen-vong` và màn web `frontend/src/tinh-nang/nguyen-vong/NguyenVong.jsx` (khối nhận định của Bác sĩ cho phiếu này: tự quyết được, cách hỗ trợ hiểu, ngày xem lại; người ký theo nhận định; danh sách ý kiến khác, mỗi ý kiến chọn nguồn; không có ô năng lực trên hồ sơ)

**Checkpoint**: Mọi user story của 001 chạy độc lập.

---

## Phase 10: Polish & Cross-Cutting

- [ ] T076 *(Bổ sung 2026-10-18, Q-366)* Thêm `phan_hoi_id` vào mẫu cột nguồn ý kiến (data-model mục 6a) và khai báo `YKienDaGhiTuPhanHoi` ở `BE/m01/api/`; test `BT/m01/FR_023g_YKienTuPhanHoiIT.java` (quickstart Y1; research H-17)

- [ ] T071 [P] Mở rộng `BT/common/HopDongApiTest.java` so springdoc với `specs/001-elder-profile-assessment/contracts/api-001.yaml`
- [ ] T072 [P] Màn danh mục `frontend/src/tinh-nang/danh-muc-m01/` (dị nguyên, mức độ dị ứng, nguồn thông tin, nhu cầu chăm sóc)
- [ ] T073 Khi plan của 003, 004, 005, 006, 007, 010, 011, 012, 013 cài bean `ThamGiaChuyenTrangThai` thật, chạy lại `FR_045_LietKeDieuKienIT`, `FR_047_RollbackTacDongIT` với bean thật (research H-02 mục 4)
- [ ] T074 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T075 Rà truy vết data-model mục 12: DBR-01 → 05, 23, 25, 34 và BR-M01-01 → 15 (BR-M01-14, 15 phần của 001) có test; FR bổ sung 2026-10-10: FR-054 — có task hoặc test tương ứng (constitution XV)

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
