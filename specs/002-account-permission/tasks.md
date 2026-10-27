---
description: "Danh sách task triển khai feature 002 – Tài khoản và phân quyền"
---

# Tasks: Tài khoản và phân quyền (002)

**Input**: Tài liệu thiết kế ở `specs/002-account-permission/` (plan.md, spec.md, research.md A-01 → A-16, data-model.md, contracts/api-002.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: Feature 000 đã xong Phase 1–2 (`common`, `NhatKyApi`, `ThamSoApi`, sự kiện, ShedLock); bản giả `m15/taikhoan/api` của 000 (000 T025) được thay bằng bản thật ở đây.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 7.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Feature 002 ở `BE/m15/taikhoan/` với `api/`, `xacthuc/`, `quyen/`, `phamvi/`, `phaply/`, `ngoaituyen/`, `lich/` (plan "Project Structure")
- Web `frontend/src/tinh-nang/{dang-nhap,tai-khoan,phan-quyen,cho-xem-lai}/`; app nhân viên `mobile/lib/nhan-vien/{dang-nhap,mo-khoa-som,ket-qua-dong-bo,cho-xem-lai}/`; app người thân `mobile/lib/nguoi-than/dang-nhap/` *(Bổ sung 2026-10-18)* App người cao tuổi `mobile/lib/nguoi-cao-tuoi/dang-nhap-qr/`; màn tạo mã QR của Điều dưỡng `mobile/lib/nhan-vien/ma-qr-nguoi-cao-tuoi/`.

---

## Phase 1: Setup

- [x] T001 *(2026-10-24: phần mobile xong cùng T097)* Thêm Spring Security 7 và Bouncy Castle (cho `Argon2PasswordEncoder`) vào `backend/pom.xml`; thêm `flutter_secure_storage` vào `mobile/pubspec.yaml` (plan Technical Context)
- [x] T002 [P] Tạo khung gói `BE/m15/taikhoan/{api,xacthuc,quyen,phamvi,phaply,ngoaituyen,lich}/` với `package-info.java`; gỡ bản giả 000 T025 khỏi `BE/m15/taikhoan/api/` và chuyển bản giả test sang `BT/support/QuyenGia.java` (research A-01)
- [x] T003 [P] *(2026-10-27: kiểm lại trên develop: bản giả ở `support/fake/m15/` (`FakeStaffProfiles`, `FakeLicenses`, `FakePracticeScopes`, `FakeAssignments`, `FakeDuties`, `FakeBedAllocations`, `FakeFamilyRelationships`, `FakeResidencyEnds`, `FakeFacilityLicenses`); `NguoiCaoTuoiApi` do B viết ở `support/fake/m01/FakeResidents`, plan ghi chú T003)* Tạo bản giả các giao diện 002 cần (contracts/module-interfaces.md mục 2) trong `BT/support/gia002/`: `HoSoNhanVienApi`, `GiayPhepApi`, `DanhMucPhamViHanhNgheApi`, `PhanCongApi`, `NhiemVuApi` (008); `PhanBoGiuongApi` (003); `NguoiCaoTuoiApi` (001); `QuanHeNguoiThanApi` (012); `KetThucLuuTruApi` (004); `GiayPhepCoSoApi` (007)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Bảng lõi, khai báo SPI cho module khác cài, phiên và bộ lọc xác thực, ma trận quyền nạp sẵn. Không có các phần này thì không endpoint nào của 000–019 chạy được.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T004 Tạo `MIG/V002_01__tai_khoan.sql`: bảng `tai_khoan` theo data-model mục 2 (`ten_dang_nhap VARCHAR(64) UNIQUE` không đổi sau khi tạo; `loai ENUM('NHAN_VIEN','NGUOI_THAN')`; `nhan_vien_id`, `nguoi_than_id` `UNIQUE`; `trang_thai ENUM('HOAT_DONG','KHOA_TAM','KHONG_HOAT_DONG','DA_KHOA')`; `nguyen_nhan_khoa ENUM('THU_CONG','NGHI_VIEC','HET_HAN_SAU_KET_THUC','HET_QUAN_HE') NULL`; `khoa_tam_den`; `so_lan_sai_lien_tiep INT >= 0`; `mat_khau_bam VARCHAR(255)`; `buoc_doi_mat_khau`; `lan_dang_nhap_cuoi`; `moc_tinh_khong_hoat_dong`; `phien_ban`; `phien_ban_quyen`; `CHECK ((loai='NHAN_VIEN' AND nhan_vien_id IS NOT NULL AND nguoi_than_id IS NULL) OR (loai='NGUOI_THAN' AND nguoi_than_id IS NOT NULL AND nhan_vien_id IS NULL))`; trigger chặn xóa); bảng `lich_su_trang_thai_tai_khoan` nhóm 3 chỉ thêm, `UNIQUE (tai_khoan_id, hieu_luc_tu)`
- [x] T005 Tạo `MIG/V002_02__phien_thiet_bi.sql`: `phien_dang_nhap` (`ma_bam_token BINARY(32)` khóa chính, `ma_thiet_bi CHAR(36)`, `kenh ENUM('WEB','APP_NHAN_VIEN','APP_NGUOI_THAN')`, `bat_dau_luc`, `hoat_dong_cuoi_luc`, `ket_thuc_luc NULL`, `ly_do_ket_thuc` `DANG_XUAT|TAI_KHOAN_ROI_HOAT_DONG|THIET_BI_DANG_NHAP_KHAC|HET_THOI_GIAN`; cột sinh `thiet_bi_dang_mo = IF(ket_thuc_luc IS NULL, ma_thiet_bi, NULL)` có `UNIQUE`; chỉ mục `(ma_thiet_bi, ket_thuc_luc)`) và `thiet_bi` (`ma_thiet_bi`, `lan_ket_noi_cuoi`, `tai_khoan_cuoi_id`)
- [x] T006 Tạo `MIG/V002_03__vai_tro_quyen.sql`: `vai_tro` (9 mã AC-01 → 05, 07, 09, 10, 14; `loai_tai_khoan`; `pham_vi_thuc_hien` `PHAN_CONG|TOAN_VIEN`; `phien_ban_quyen`), `ma_tran_quyen` (khóa chính `(vai_tro, chuc_nang, loai_thao_tac)`, `pham_vi` `TOAN_VIEN|PHAN_CONG|QUAN_HE`, `ma_chu_thich JSON`, `nguon` `PL27|19.3`), `gan_vai_tro` (cột sinh `con_hieu_luc = IF(hieu_luc_den IS NULL, vai_tro, NULL)` `UNIQUE (tai_khoan_id, con_hieu_luc)`, `ly_do NOT NULL`), `thu_hep_quyen` (`CHECK` đúng một trong `vai_tro`/`tai_khoan_id` theo `doi_tuong`; `kieu` `BO_THAO_TAC|HA_PHAM_VI`; `nhom_chi_tieu`, `kenh` chỉ với `DASHBOARD_BAO_CAO`), `quyen_gan_them` (`CHECK (chuc_nang='KE_HOACH_CHAM_SOC' AND loai_thao_tac='DUYET')`), `quy_tac_xem_truong` (`muc_xem` `DAY_DU|CHI_CO_HAY_KHONG|KHONG`)
- [x] T007 Tạo `MIG/V002_04__nap_ma_tran_quyen.sql`: nạp `ma_tran_quyen` từ Phụ lục 27 `docs/business/nghiep-vu.md` theo quy đổi ký hiệu research A-04, cộng các dòng `nguon='19.3'` ở data-model mục 3; nạp `quy_tac_xem_truong` theo 19.3 và chú thích ⁵, ¹⁴, ¹⁶, ³⁰ (FR-016 → FR-022)
- [x] T008 [P] Tạo kiểu và giao diện công khai trong `BE/m15/taikhoan/api/`: `NguoiDungHienTai`, `VaiTro`, `LoaiTaiKhoan`, `KenhTruyCap`, `KhoaQuyen`, `ChucNang`, `LoaiThaoTac`, `DoiTuong` (có `tuPhamVi`, `KHONG_CU_THE`), `KetQuaQuyen` (`lopKhongThoa` `VAI_TRO|PHAM_VI|PHAP_LY|DU_LIEU_NGUON`), `QuyenApi`, `PhamViApi` (`PhamViLoc`), `QuyenXemTruongApi` (`MucXem`), `NguoiDuyetApi`, `TaiKhoanApi`, chú thích `@CanQuyen`, `@NhomTruong` đúng chữ ký contracts/module-interfaces.md mục 1.1 → 1.6
- [x] T009 [P] *(2026-10-27: kiểm lại trên develop: đủ SPI ở `m15.account.api.spi` (`BedAllocationApi`, `FamilyRelationshipApi`, `ResidencyEndApi`, `OfflineHandler` có `requiresOnline`, `FootnoteCondition`, `ScopeAssignmentProvider`, `InvalidPermissionDataException`); `QuanHeNguoiThanApi.quyenDangBat` E chốt khi dịch hợp đồng 012, plan ghi chú T009, team-plan mục 8)* Khai báo SPI cho module khác cài trong `BE/m15/taikhoan/api/`: `PhanBoGiuongApi` (M03 cài), `QuanHeNguoiThanApi` (M10 cài), `KetThucLuuTruApi` (M02 cài), `NgoaiTuyenHandler<T>` (có `batBuocTrucTuyen()`), `DieuKienChuThich`, `GanPhamViProvider`, ngoại lệ `DuLieuQuyenKhongHopLe` (contracts mục 1.7 → 1.9, mục 2)
- [x] T010 [P] *(Xong 2026-10-22: bốn sự kiện có thông báo cài `HasNotificationRequests`; ghi chú triển khai ở plan)* Tạo sự kiện `BE/m15/taikhoan/api/sukien/`: `TaiKhoanDaTao`, `TaiKhoanDoiTrangThai`, `BanGhiDongBoSauMatQuyen`, `BanGhiChoXemLai`, `LoiDuLieuQuyen`, `GiayPhepSapHetHan` (cài `CoYeuCauThongBao` với nhóm nhận ở contracts mục 1.10)
- [x] T011 Cài `BE/m15/taikhoan/xacthuc/internal/PhienService.java` và bộ lọc `BE/m15/taikhoan/xacthuc/web/XacThucTokenFilter.java`: token opaque 256 bit, chỉ lưu SHA-256; web dùng cookie `HttpOnly`, `Secure`, `SameSite=Strict` cùng header `X-Yeu-Cau-Tu: web` cho mọi lệnh ghi; app dùng `Authorization: Bearer`; mỗi yêu cầu tra phiên theo khóa chính, không đệm; so `hoat_dong_cuoi_luc` với CFG-M15-10 của kênh, quá hạn thì `HET_THOI_GIAN` và 401 `PHIEN_HET_HAN`; cập nhật `thiet_bi.lan_ket_noi_cuoi` (research A-02; FR-013, FR-013a, BR-M15-21) *(Bổ sung 2026-10-18)* Ba đường không cần phiên: `/xac-thuc/dang-nhap`, `/xac-thuc/dang-nhap-qr`, `/sos/**`; `/sos/**` không tra phiên và không đòi `X-Yeu-Cau-Tu`, xác thực do 007 gọi `ThietBiGanApi.xacDinh` (research A-15).
- [x] T012 Cài `NguoiDungHienTai` từ phiên trong `BE/m15/taikhoan/xacthuc/internal/NguoiDungHienTaiImpl.java` và bản Bộ lập lịch (`laBoLapLich() = true`) đặt khi job chạy (FR-044; 000 FR-045)
- [x] T013 Cài bộ tính quyền vai trò `BE/m15/taikhoan/quyen/internal/TinhQuyenVaiTro.java` theo thứ tự A-05: ma trận trừ thu hẹp vai trò → hợp vai trò (toàn viện thắng) → trừ thu hẹp tài khoản → cộng `quyen_gan_them`; bộ đệm khóa `(tai_khoan_id, phien_ban_quyen, tổng phien_ban_quyen vai trò)`; có biến thể tại thời điểm `t` (FR-025, FR-026a, BR-M15-18)
- [x] T014 Cài interceptor `BE/m15/taikhoan/quyen/internal/CanQuyenInterceptor.java` gọi `QuyenApi.coQuyen` trước mọi phương thức `@CanQuyen`; ánh xạ lỗi FR-043: không xem được đối tượng → 404 chung; thiếu quyền thao tác → 403 `KHONG_CO_QUYEN` kèm `lopKhongThoa`; thiếu pháp lý → 403 `DIEU_KIEN_PHAP_LY` (research A-10)
- [x] T015 [P] Test kiến trúc `BT/m15/taikhoan/CanQuyenArchTest.java`: mọi `@LenhNghiepVu` và mọi endpoint ghi có `@CanQuyen`; cấm gọi `PhamViApi.boLocLichSu` từ `@LenhNghiepVu` (BR-M15-01, FR-035a)
- [x] T016 Cài lệnh khởi tạo Quản lý viện đầu tiên (`nguoi_thuc_hien_loai = KHOI_TAO`) trong `BE/m15/taikhoan/xacthuc/internal/KhoiTaoQuanLyVien.java`, chỉ chạy khi chưa có AC-01, và test `BT/m15/taikhoan/KhoiTaoQuanLyVienIT.java` (BR-M15-22)

**Checkpoint**: Có đăng nhập kỹ thuật, `NguoiDungHienTai`, `@CanQuyen` cho mọi module.

---

## Phase 3: User Story 1 – Mọi lần xem và thao tác bị giới hạn bởi quyền hiệu lực (Priority: P1) 🎯 MVP

**Goal**: Một điểm kiểm tra ba lớp BR-M15-01 (vai trò ∩ phạm vi ∩ pháp lý), giới hạn trường, ngoại lệ sự cố, `lenhDuocPhep`, `/toi/quyen` (FR-016 → FR-022, FR-041 → FR-044a).

**Independent Test**: Viện hai tầng, mỗi vai trò một tài khoản phân công tầng 1; với mỗi dòng ma trận, thử thao tác trên người cao tuổi tầng 1 và tầng 2, đối chiếu ma trận và 19.3.

### Tests cho User Story 1

- [x] T017 [P] [US1] Test `BT/m15/taikhoan/MaTranQuyenKhopNguonTest.java`: đọc Phụ lục 27 từ `docs/business/nghiep-vu.md`, đối chiếu từng ô với `ma_tran_quyen` đã nạp (trừ dòng `nguon='19.3'` có liệt kê) (research A-04)
- [x] T018 [P] [US1] Test `BT/m15/taikhoan/MaTranQuyenIT.java` (SC-001) và `QuyenBaLopIT.java`: mỗi vai trò × chức năng × tầng trong/ngoài phạm vi; lớp không thỏa đúng (US1 #1–13; BR-M15-01)
- [x] T019 [P] [US1] Test `BT/m15/taikhoan/GioiHanTruongIT.java`: Hành chính, Kế toán không thấy trường `SUC_KHOE`, `THUOC`; nguyện vọng chỉ "có/không" (FR-022)
- [x] T020 [P] [US1] Test `BT/m15/taikhoan/SuCoNgoaiPhamViIT.java`: khóa `(SU_CO_KHAN_CAP, TAO)` bỏ lớp phạm vi, nhật ký có `ngoaiPhamVi`; dữ liệu nguồn lỗi vẫn cho ghi sự cố (FR-044a, FR-041b)
- [x] T021 [P] [US1] Test `BT/m15/taikhoan/PhamViBaoCaoLichSuIT.java`: `boLocLichSu` chỉ dùng cho xem (US1 #13, FR-035a)

### Implementation cho User Story 1

- [x] T022 [US1] Cài `BE/m15/taikhoan/quyen/internal/QuyenService.java` (`QuyenApi`): thứ tự vai trò + chú thích → nhiệm vụ → phạm vi (trừ FR-044a) → pháp lý khi `CHUYEN_MON`; Bộ lập lịch bỏ lớp phạm vi và pháp lý; `coQuyenTaiThoiDiem`, `tapQuyen`, `lenhDuocPhep` (research A-10; FR-041 → FR-044a)
- [x] T023 [US1] Cài giới hạn trường `BE/m15/taikhoan/quyen/internal/GioiHanTruongSerializerModifier.java` (`BeanSerializerModifier` đọc `@NhomTruong` và `quy_tac_xem_truong`) và `QuyenXemTruongService.java` (`QuyenXemTruongApi`) (research A-08; FR-022, FR-034)
- [x] T024 [US1] Cài `BE/m15/taikhoan/phamvi/internal/LoiDuLieuQuyenService.java`: bảng kỹ thuật `loi_du_lieu_quyen` (`MIG/V002_05__loi_du_lieu_quyen.sql`: khóa duy nhất `(nguon, ban_ghi_id, loai_loi)`, `so_lan`, `da_khac_phuc_luc`), lần đầu phát `LoiDuLieuQuyen`, ghi `QUYEN_TU_CHOI_DU_LIEU_NGUON` (FR-041b)
- [x] T025 [US1] Tạo `BE/m15/taikhoan/quyen/web/ToiController.java` cho `GET /toi` và `GET /toi/quyen` theo contracts/api-002.yaml (FR-042)
- [x] T026 [P] [US1] Tạo hook `frontend/src/chung/quyen/useQuyen.js` ẩn nút/menu theo `/toi/quyen` và `lenhDuocPhep`, test `frontend/src/chung/quyen/AnNutTheoQuyen.test.jsx`

**Checkpoint**: Mọi module có thể bảo vệ endpoint bằng `@CanQuyen`.

---

## Phase 4: User Story 2 – Quản lý viện cấp và thu hồi tài khoản nhân viên (Priority: P1)

**Goal**: Tạo tài khoản nhân viên gắn hồ sơ 008, gán vai trò, khóa, mở khóa, khóa kèm Nghỉ việc, luôn còn Quản lý viện, không xóa (FR-001 → FR-006, FR-009, FR-011, FR-012).

**Independent Test**: Tạo tài khoản cho hồ sơ nhân viên, gán Điều dưỡng, đăng nhập; cho nhân viên Nghỉ việc ở 008 (bản giả) → tài khoản `DA_KHOA`, phiên kết thúc, lịch sử giữ tên.

### Tests cho User Story 2

- [x] T027 [P] [US2] Test `BT/m15/taikhoan/TaoTaiKhoanIT.java`: hồ sơ Nghỉ việc hoặc chức danh không có vai trò hệ thống bị từ chối; tài khoản nhân viên phải có ít nhất một vai trò AC-01 → 05, 07, 09, 14; trùng `nhan_vien_id` bị từ chối (US2; FR-002 → FR-004a)
- [x] T028 [P] [US2] Test `BT/m15/taikhoan/NghiViecKhoaTaiKhoanIT.java`: `khoaDoNghiViec` trong transaction 008 → `DA_KHOA` `NGHI_VIEC`, mọi phiên kết thúc; rollback 008 thì tài khoản không đổi (BR-M09-04, FR-009, FR-013)
- [x] T029 [P] [US2] Test `BT/m15/taikhoan/KhoaMoKhoaIT.java`, `LuonConQuanLyVienIT.java`, `KhongXoaTaiKhoanIT.java`: mở khóa khi nguyên nhân tự động còn → 409; khóa/gỡ vai trò AC-01 cuối cùng → 409 `KHONG_CON_QUAN_LY_VIEN`; `DELETE` bị trigger chặn (FR-005, FR-011, FR-012)

### Implementation cho User Story 2

- [x] T030 [P] [US2] Tạo entity `BE/m15/taikhoan/xacthuc/internal/TaiKhoan.java` (`@NhomDuLieu(NGHIEP_VU)`), `LichSuTrangThaiTaiKhoan.java` (`GHI_NHAN`, `ChiThemRepository`), `GanVaiTro.java`
- [x] T031 [US2] Cài máy trạng thái tài khoản `BE/m15/taikhoan/xacthuc/internal/TaiKhoanService.java` đúng bảng chuyển data-model mục 2: tạo nhân viên, khóa (kèm `nguoiThayPhuTrach` gọi 008 cùng transaction khi đang là Phụ trách bộ phận, Q-292), mở khóa, cấp lại mật khẩu (`buoc_doi_mat_khau`); mỗi chuyển thêm `lich_su_trang_thai_tai_khoan`, kết thúc mọi phiên khi rời `HOAT_DONG`, ghi `TK_*`; mật khẩu và token không vào nhật ký
- [x] T032 [US2] Cài `TaiKhoanApi` trong `BE/m15/taikhoan/xacthuc/internal/TaiKhoanApiImpl.java`: `khoaDoNghiViec` (`MANDATORY`), `theoNhanVien`, `theoNguoiThan`, `coVaiTro` (contracts mục 1.6)
- [x] T032a [US2] *(Bổ sung 2026-10-20)* Họ tên người dùng: thêm `HoSoNhanVienApi.hoTen(nhanVienId)` (008 cài), `QuanHeNguoiThanApi.hoTenNguoiThan(nguoiThanId)` (012 cài) kèm bản giả trong `BT/support/fake/m15/`, và `TaiKhoanApi.hoTen(taiKhoanId)` trong `TaiKhoanApiImpl.java`; dùng cho `UserInfo.fullName` (`/me`, đăng nhập) và `Actor.fullName` của 000 (nhật ký, yêu cầu phê duyệt, đính chính, lịch sử tham số; trước đây trả `null`); tài khoản người cao tuổi lấy tên qua `NguoiCaoTuoiApi` ở T091. Test `BT/m15/taikhoan/HoTenNguoiThucHienIT.java` (contracts/module-interfaces.md mục 1.6, mục 2; PR ghi "Đổi api", báo E, B)
- [x] T033 [US2] Cài gán/gỡ vai trò trong `BE/m15/taikhoan/quyen/internal/GanVaiTroService.java`: đóng dòng cũ và thêm dòng mới, vai trò cùng loại tài khoản, tăng `phien_ban_quyen`, `If-Match`, đếm AC-01 dưới `SELECT … FOR UPDATE` (FR-003, FR-004, FR-012, FR-026)
- [x] T034 [US2] Tạo `BE/m15/taikhoan/xacthuc/web/TaiKhoanController.java`: `POST/GET /tai-khoan`, `GET /tai-khoan/{id}`, `POST …/{id}/khoa`, `…/mo-khoa`, `…/cap-lai-mat-khau`, `…/gan-vai-tro`, `…/go-vai-tro` theo contracts/api-002.yaml; không có `PUT/PATCH/DELETE`
- [x] T035 [P] [US2] Tạo màn quản trị `frontend/src/tinh-nang/tai-khoan/TaiKhoanDanhSach.jsx`, `TaoTaiKhoanNhanVien.jsx`, `ChiTietTaiKhoan.jsx` (lệnh theo `lenhDuocPhep`, lý do bắt buộc)

**Checkpoint**: Quản lý viện cấp và thu hồi tài khoản nhân viên.

---

## Phase 5: User Story 3 – Đăng nhập an toàn cho nhân viên và người thân (Priority: P1)

**Goal**: Đăng nhập ba kênh, khóa tạm khi sai liên tiếp, mở khóa sớm, Không hoạt động với người thân, bắt buộc đổi mật khẩu, một thiết bị một phiên (FR-007 → FR-015, BR-M15-05, BR-M15-15, BR-M15-21).

**Independent Test**: Đăng nhập đúng, sai tới ngưỡng CFG-M15-01, chờ hết khóa; tua đồng hồ quá CFG-M15-02 với tài khoản người thân.

### Tests cho User Story 3

- [x] T036 [P] [US3] Test `BT/m15/taikhoan/DangNhapKhoaTamIT.java` và `KhongHoatDongIT.java` (đồng hồ giả lập): sai đủ CFG-M15-01 → `KHOA_TAM`; hết `khoa_tam_den` tính ở yêu cầu kế tiếp; người thân quá CFG-M15-02 → `KHONG_HOAT_DONG`; job chạy lại không chuyển hai lần (BR-M15-05)
- [x] T037 [P] [US3] Test `BT/m15/taikhoan/MoKhoaSomIT.java`: AC-01; Trưởng tầng hoặc Người phụ trách ca cùng tầng và cùng ca mở khóa sớm nhân viên; người khác bị từ chối; `lyDo` bắt buộc (FR-019c)
- [x] T038 [P] [US3] Test đơn vị `BT/m15/taikhoan/DangNhapKhongLoTonTaiTest.java`: thông báo lỗi giống nhau cho tên sai, mật khẩu sai, tài khoản khóa (NFR-08)
- [x] T039 [P] [US3] Test `BT/m15/taikhoan/BuocDoiMatKhauIT.java`, `DoManhMatKhauIT.java`, `TuDangXuatIT.java`: cấp lại mật khẩu bắt đổi ở lần đăng nhập kế tiếp; mật khẩu không đạt CFG-M15-09 → 422; phiên quá CFG-M15-10 theo kênh → 401 (BR-M15-21)
- [x] T040 [P] [US3] Test `BT/m15/taikhoan/MotThietBiMotPhienIT.java` và `KetThucPhienNgayIT.java`: đăng nhập trên thiết bị đã có phiên của tài khoản khác kết thúc phiên cũ; khóa tài khoản làm yêu cầu kế tiếp bị 401 (BR-M15-15, FR-013, FR-014a)

### Implementation cho User Story 3

- [x] T041 [US3] Cài `BE/m15/taikhoan/xacthuc/internal/DangNhapService.java`: Argon2id; đếm `so_lan_sai_lien_tiep`, khóa tạm theo CFG-M15-01 (người thực hiện Bộ lập lịch, căn cứ CFG); kết thúc phiên cùng thiết bị trong cùng transaction rồi thêm phiên; đặt `moc_tinh_khong_hoat_dong`; ghi `DANG_NHAP`/`DANG_NHAP_THAT_BAI` (research A-03)
- [x] T042 [US3] Cài đổi mật khẩu (kiểm CFG-M15-09), đăng xuất, mở khóa sớm (FR-019c qua `NhiemVuApi`), kích hoạt lại trong `BE/m15/taikhoan/xacthuc/internal/MatKhauService.java` và `TaiKhoanService.java`
- [x] T043 [US3] Cài job `BE/m15/taikhoan/lich/ChuyenKhongHoatDong.java` (01:00 và 06:00, ShedLock): chỉ người thân Hoạt động có `moc_tinh_khong_hoat_dong` quá CFG-M15-02; điều kiện trên trạng thái hiện hành để chạy lại an toàn (NFR-04, NFR-05)
- [x] T044 [US3] Tạo `BE/m15/taikhoan/xacthuc/web/XacThucController.java` cho `POST /xac-thuc/dang-nhap`, `/xac-thuc/dang-xuat`, `/xac-thuc/doi-mat-khau` và bổ sung `POST /tai-khoan/{id}/mo-khoa-som`, `/kich-hoat-lai` vào `TaiKhoanController.java`
- [x] T045 [P] [US3] Tạo màn web `frontend/src/tinh-nang/dang-nhap/DangNhap.jsx`, `DoiMatKhau.jsx` (mã thiết bị trong `X-Thiet-Bi`) và test `DangNhap.test.jsx` (thông báo chung), `DoiMatKhau.test.jsx`
- [x] T046 [P] [US3] Tạo `mobile/lib/chung/xac-thuc/` (token trong `flutter_secure_storage`, mã thiết bị UUID), `mobile/lib/nhan-vien/dang-nhap/`, `mobile/lib/nhan-vien/mo-khoa-som/` và test `mobile/test/nhan-vien/dang_nhap_thiet_bi_chung_test.dart`, `mo_khoa_som_test.dart` *(2026-10-27: phiên `common/auth/session_controller.dart` (`staffLogin` kênh `STAFF_APP`, `signOut`, máy đã gắn người cao tuổi chỉ quét mã); màn chọn "Tôi là" `common/auth/login_choice_screen.dart`; `staff/login/staff_login_screen.dart`; `staff/early_unlock/early_unlock_screen.dart` dùng endpoint mới `GET /early-unlock-candidates` (đổi api-002, chủ dự án duyệt); test `test/staff/login/staff_login_test.dart`, `test/staff/early_unlock/early_unlock_test.dart`)*
- [x] T047 [P] [US3] Tạo `mobile/lib/chung/xac-thuc/`, `mobile/lib/nguoi-than/dang-nhap/` (đăng nhập, đổi mật khẩu) và test `mobile/test/nguoi-than/dang_nhap_nguoi_than_test.dart` *(2026-10-27: `family/login/family_login_screen.dart` kênh `FAMILY_APP`; đổi mật khẩu chung `common/auth/change_password_screen.dart` (chuyển từ `resident/qr_login/`), người thân giữ phiên sau khi đổi; `family/family_root.dart` có "Đăng xuất"; test `test/family/login/family_login_test.dart`)*

**Checkpoint**: Ba kênh đăng nhập được.

---

## Phase 6: User Story 4 – Phạm vi dữ liệu tự cập nhật theo phân công (Priority: P2)

**Goal**: Phạm vi suy ra từ phân công (008) và phân bổ giường (003) tại từng yêu cầu; quyền theo nhiệm vụ; ngoại tuyến kiểm quyền theo thời điểm thiết bị và bản ghi chờ xem lại (FR-028 → FR-035a, FR-019a → f, FR-041a → c, BR-M15-02, BR-M15-20).

**Independent Test**: Phân công nhân viên chăm sóc tầng 1 → thấy A không thấy B; đổi sang tầng 2 → thao tác kế tiếp đổi ngay; chuyển A sang phòng tầng 2 → phạm vi theo A.

### Tests cho User Story 4

- [x] T048 [P] [US4] Test `BT/m15/taikhoan/PhamViTheoCaIT.java`, `PhamViDoiPhanCongIT.java`, `PhamViHopPhanCongIT.java`: khoảng CFG-M15-07 quanh ca; đổi phân công có hiệu lực ở yêu cầu kế tiếp; nhiều phân công thì hợp (BR-M15-02)
- [ ] T049 [P] [US4] Test `BT/m15/taikhoan/QuyenTheoNhiemVuIT.java`: bảy ngoại lệ FR-019a → g, gồm FR-019g Người phụ trách ca Xác nhận giường sẵn sàng (Q-313) (người phụ trách ca, trưởng tầng tạm, Phụ trách bộ phận, trưởng đoàn, người đi cùng, người nhận tại tầng), nhật ký có `canCu` nhiệm vụ *(2026-10-20: xong trừ phạm vi trưởng đoàn, người đi cùng, chờ SPI người tham gia chuyến của 014; plan ghi chú Phase 6)*
- [x] T050 [P] [US4] Test `BT/m15/taikhoan/DongBoKetQuaIT.java` và `NgoaiTuyenChoXemLaiIT.java`: kiểm quyền theo `thoiDiemThietBi`; mất quyền sau khi ghi → `CHAP_NHAN_KEM_NHAN` và sự kiện `BanGhiDongBoSauMatQuyen`; thời điểm thiết bị muộn hơn đồng bộ, sớm hơn lần kết nối cuối, hoặc quá CFG-M15-08 → `CHO_XEM_LAI`; `batBuocTrucTuyen()` → `TU_CHOI`; trùng `Idempotency-Key` không ghi hai lần (BR-M15-20, FR-041a, FR-041c)
- [x] T051 [P] [US4] Test `BT/m15/taikhoan/DuLieuNguonLoiIT.java`: lỗi dữ liệu nguồn gộp theo `(nguon, ban_ghi_id, loai_loi)`, báo Quản lý viện lần đầu, đánh dấu khắc phục (FR-041b)

### Implementation cho User Story 4

- [x] T052 [US4] Cài `BE/m15/taikhoan/phamvi/internal/PhamViService.java` (`PhamViApi`): `trongPhamVi`, `boLoc`, `boLocLichSu`, `nguoiPhuTrachCa`, `truongTangDuocGiao`, `nguoiQuyetDinhXemLai` từ `PhanCongApi`, `NhiemVuApi`, `PhanBoGiuongApi` tính trong từng yêu cầu, không đệm (research A-07)
- [x] T053 [US4] Cài quyền theo nhiệm vụ `BE/m15/taikhoan/quyen/internal/QuyenTheoNhiemVu.java` (FR-019a → g; research A-06) và nối vào bước 2 của `QuyenService`
- [x] T054 [US4] Cài `NguoiDuyetApi` trong `BE/m15/taikhoan/quyen/internal/NguoiDuyetService.java`: tài khoản Hoạt động có quyền vai trò, nhiệm vụ, phạm vi bao đối tượng tại lúc hỏi (BR-M15-09 của 000)
- [x] T055 [US4] Tạo `MIG/V002_06__ban_ghi_cho_xem_lai.sql`: bảng `ban_ghi_cho_xem_lai` theo data-model mục 5 (`idempotency_key` `UNIQUE` cùng `nguoi_ghi_id`; `ly_do_cho` `MUON_HON_DONG_BO|SOM_HON_KET_NOI_CUOI|QUA_CFG_M15_08`; `trang_thai` `CHO_XEM_LAI → DA_CHAP_NHAN/DA_TU_CHOI`; `ly_do_tu_choi` bắt buộc khi từ chối; `phien_ban`)
- [x] T056 [US4] Cài điều phối đồng bộ `BE/m15/taikhoan/ngoaituyen/internal/DongBoService.java`: nhận lô phong bì, đối chiếu `lan_ket_noi_cuoi` trước khi cập nhật, tra `NgoaiTuyenHandler` theo `loaiBanGhi`, kiểm quyền `coQuyenTaiThoiDiem(thoiDiemThietBi)`, trả `KetQuaDongBo` từng bản ghi (research A-11)
- [x] T057 [US4] Cài lệnh chấp nhận/từ chối bản ghi chờ xem lại trong `BE/m15/taikhoan/ngoaituyen/internal/ChoXemLaiService.java` (người quyết định: Trưởng tầng được giao → Người phụ trách ca → AC-01 khi không gắn tầng; quyết định đầu tiên thắng; chấp nhận gọi `handler.apDung` hiệu lực từ lúc chấp nhận; nhật ký `NT_*`)
- [x] T058 [US4] Tạo `BE/m15/taikhoan/ngoaituyen/web/DongBoController.java` (`POST /dong-bo`) và `ChoXemLaiController.java` (`GET /ban-ghi-cho-xem-lai`, `POST …/{id}/chap-nhan`, `…/tu-choi`) theo contracts/api-002.yaml
- [x] T059 [P] [US4] Tạo màn `frontend/src/tinh-nang/cho-xem-lai/ChoXemLai.jsx` *(2026-10-27: `frontend/src/features/pending-review/PendingReviewPage.jsx`, mục menu "Chờ xem lại"; api-002 thêm `recorderName`, `floorAreaName`, `version` cho `PendingReviewRecord`, SPI `FloorAreaNameApi` M03 cài; plan ghi chú)*
- [x] T060 [P] [US4] Tạo `mobile/lib/nhan-vien/ket-qua-dong-bo/` (hiển thị kết quả từng bản ghi sau đồng bộ, BR-M15-20), `mobile/lib/nhan-vien/cho-xem-lai/` và test `mobile/test/nhan-vien/ket_qua_dong_bo_test.dart`, `cho_xem_lai_test.dart` (constitution XV: BR-M15-20 có test ở client) *(2026-10-27: `staff/sync_results/sync_results_screen.dart` trên hàng đợi 000 T100; `staff/pending_review/pending_review_screen.dart`; thanh điều hướng dưới của `staff/staff_root.dart`; test `test/staff/sync_results/sync_results_test.dart`, `test/staff/pending_review/pending_review_test.dart`)*

**Checkpoint**: Phạm vi và ngoại tuyến chạy theo phân công.

---

## Phase 7: User Story 5 – Nghiệp vụ chuyên môn chỉ bật khi giấy phép còn hiệu lực (Priority: P2)

**Goal**: Danh mục nghiệp vụ chuyên môn, kiểm giấy phép người hành nghề và giấy phép cơ sở theo ngày ở mỗi thao tác, cảnh báo sắp hết hạn (FR-036 → FR-040, BR-M06-05, BR-M15-19).

**Independent Test**: Bác sĩ có giấy phép hết hạn ngày D; tua tới D và D+1, thử kê đơn nội bộ; lặp lại với giấy phép cơ sở hết hạn; gia hạn và thử lại.

### Tests cho User Story 5

- [x] T061 [P] [US5] Test `BT/m15/taikhoan/DieuKienPhapLyIT.java`: giấy phép hết hạn hết ngày theo Asia/Ho_Chi_Minh; thiếu phạm vi cơ sở; 403 `DIEU_KIEN_PHAP_LY` kèm tên giấy phép, ngày hết hạn (US5 #1–8, BR-M06-05)
- [x] T062 [P] [US5] Test `BT/m15/taikhoan/PhamViHanhNgheIT.java` và `CanhBaoGiayPhepIT.java`: phạm vi hành nghề theo danh mục của 008; cảnh báo `GiayPhepSapHetHan` một lần cho mỗi `(loai, giay_phep_id, ngay_het_han)` (BR-M15-19, FR-039)

### Implementation cho User Story 5

- [x] T063 [US5] Tạo `MIG/V002_07__nghiep_vu_chuyen_mon.sql`: `nghiep_vu_chuyen_mon` (mẫu nhóm 1 của 000, `chuc_nang`, `loai_giay_phep_yeu_cau`, `can_pham_vi_co_so`, `mac_dinh` – `true` thì không ngừng hiệu lực được), `nghiep_vu_chuyen_mon_pham_vi` (khóa chính `(nghiep_vu_id, pham_vi_hanh_nghe_id)`), `da_canh_bao_giay_phep` (khóa chính `(loai, giay_phep_id, ngay_het_han)`); nạp hai dòng mặc định (chẩn đoán và kê đơn nội bộ; phát thuốc và xác nhận liều)
- [x] T064 [US5] Cài `BE/m15/taikhoan/phaply/internal/DieuKienPhapLyService.java` (gọi `GiayPhepApi.hieuLuc`, `GiayPhepCoSoApi.coPhamVi` theo ngày ở mỗi thao tác, không có job bật/tắt) và nối bước 4 của `QuyenService` (research A-09)
- [x] T065 [US5] Cài lệnh thêm, sửa, ngừng hiệu lực nghiệp vụ chuyên môn (chỉ AC-01, `lyDo`, nhật ký `NVCM_*`) trong `BE/m15/taikhoan/phaply/internal/NghiepVuChuyenMonService.java` và `BE/m15/taikhoan/phaply/web/NghiepVuChuyenMonController.java` (`/nghiep-vu-chuyen-mon`, `/{id}`, `/{id}/ngung-hieu-luc`) (FR-036, FR-037)
- [x] T066 [US5] Cài job `BE/m15/taikhoan/phaply/lich/CanhBaoGiayPhepSapHetHan.java` (06:00, ShedLock; mốc từ CFG của 008/007 qua `GiayPhepCoSoApi.sapHetHan`) phát `GiayPhepSapHetHan`, ghi `da_canh_bao_giay_phep` (FR-039)

**Checkpoint**: Nghiệp vụ chuyên môn bị chặn đúng theo giấy phép.

---

## Phase 8: User Story 6 – Người thân chỉ xem được đúng những gì được cấp (Priority: P2)

**Goal**: Tài khoản người thân gắn quan hệ ở 012, phạm vi theo quan hệ, phần sức khỏe theo bản đồng ý (chú thích ¹), khóa khi hết quan hệ hoặc sau kết thúc lưu trú (FR-045 → FR-052, BR-M15-14, BR-M01-08, DBR-03).

**Independent Test**: Hai người thân của cùng người cao tuổi, chỉ một trong bản đồng ý → phần sức khỏe hiện/ẩn đúng; rút đồng ý; kết thúc lưu trú và tua quá CFG-M01-04.

### Tests cho User Story 6

- [x] T067 [P] [US6] Test `BT/m15/taikhoan/NguoiThanSucKhoeIT.java` và `NguoiThanPhamViIT.java`: điều kiện chú thích ¹ kiểm mỗi lần xem; rút đồng ý có hiệu lực ngay; người thân không thấy người cao tuổi khác (BR-M01-08, DBR-03, FR-045, FR-046)
- [x] T068 [P] [US6] Test `BT/m15/taikhoan/HetQuanHeKhoaTaiKhoanIT.java` và `XacMinhNguoiThanIT.java`: `xetLaiNguoiThan` trong transaction 012 → `DA_KHOA` `HET_QUAN_HE`; job `KhoaSauKetThucLuuTru` sau CFG-M01-04 → `HET_HAN_SAU_KET_THUC`; tạo, kích hoạt lại, cấp lại mật khẩu phải có `cachXacMinh` (BR-M15-14, FR-049, FR-052)

### Implementation cho User Story 6

- [x] T069 [US6] Cài tạo tài khoản người thân (AC-01 hoặc AC-09; hồ sơ người thân có ít nhất một quan hệ chưa ở trạng thái cuối qua `QuanHeNguoiThanApi`; vai trò duy nhất AC-10; `cachXacMinh` vào `chi_tiet` nhật ký) trong `TaiKhoanService.java` và endpoint `POST /tai-khoan-nguoi-than` trong `TaiKhoanController.java` (FR-050, FR-052)
- [x] T070 [US6] Cài `xetLaiNguoiThan` (`MANDATORY`) trong `TaiKhoanApiImpl.java` và job `BE/m15/taikhoan/lich/KhoaSauKetThucLuuTru.java` (dùng `KetThucLuuTruApi.thoiDiemTrangThaiCuoi` và CFG-M01-04; ShedLock) (BR-M15-14, FR-049)
- [x] T071 [US6] Cài phạm vi `QUAN_HE` cho AC-10 trong `PhamViService.java` và đăng ký `DieuKienChuThich` ¹, ² (bản đồng ý, người đại diện) do 001/012 cài, qua `QuanHeNguoiThanApi` (FR-045 → FR-048) *(2026-10-21: phạm vi `QUAN_HE`, chế độ chỉ đọc, nhóm trường sức khỏe theo ¹; quyền riêng của từng người thân chờ `quyenDangBat` của 012, plan ghi chú Phase 8)*
- [ ] T072 [P] [US6] Test app `mobile/test/nguoi-than/an_suc_khoe_test.dart`: phần sức khỏe ẩn khi API không trả trường (giới hạn trường ở backend)

**Checkpoint**: Người thân chỉ thấy đúng phần được cấp.

---

## Phase 9: User Story 7 – Quản lý viện điều chỉnh quyền của vai trò trong giới hạn cho phép (Priority: P3)

**Goal**: Thu hẹp theo vai trò hoặc tài khoản, gán thêm quyền duyệt kế hoạch chăm sóc cho Điều dưỡng, tra quyền tại thời điểm quá khứ, chống ghi đè (FR-024 → FR-027, BR-M15-04, BR-M15-16 → 18).

**Independent Test**: Gán quyền duyệt kế hoạch chăm sóc cho một điều dưỡng; điều dưỡng đó duyệt được, điều dưỡng khác không; thu hồi và kiểm lại; tra nhật ký.

### Tests cho User Story 7

- [x] T073 [P] [US7] Test đơn vị `BT/m15/taikhoan/ThuTuThuHepTest.java`: thứ tự tính A-05 (BR-M15-18)
- [x] T074 [P] [US7] Test `BT/m15/taikhoan/ThuHepKhongVuotIT.java`, `DoiQuyenDongThoiIT.java`, `QuyenTaiThoiDiemIT.java`, `PhanQuyenChiQuanLyIT.java`: vượt ma trận → 409 `VUOT_QUYEN_TOI_DA`; `If-Match` cũ → 412 kèm tập quyền hiện hành; quyền tại `t` quá khứ đúng; chỉ AC-01 đổi quyền; nhật ký có tập quyền trước/sau (BR-M15-04, BR-M15-16, BR-M15-17, SC-007)

### Implementation cho User Story 7

- [x] T075 [US7] Cài `BE/m15/taikhoan/quyen/internal/ThuHepQuyenService.java`: thu hẹp/khôi phục theo vai trò và theo tài khoản (đóng dòng cũ, thêm dòng mới; `nhom_chi_tieu`, `kenh` cho `DASHBOARD_BAO_CAO`), gán/gỡ `quyen_gan_them` (tài khoản phải có AC-04), kiểm không vượt ma trận, tăng `phien_ban_quyen`, nhật ký `QUYEN_*` (FR-024 → FR-027)
- [x] T076 [US7] Tạo `BE/m15/taikhoan/quyen/web/PhanQuyenController.java`: `GET /vai-tro`, `GET /vai-tro/{ma}/quyen`, `POST /vai-tro/{ma}/thu-hep`, `/khoi-phuc-quyen`, `GET /tai-khoan/{id}/quyen` (có `taiThoiDiem`) *(phần hiện hành làm sớm ở T035, 2026-10-20; `taiThoiDiem` xong 2026-10-21)*, `POST /tai-khoan/{id}/thu-hep`, `/khoi-phuc-quyen`, `/gan-quyen-duyet-ke-hoach`, `/go-quyen-duyet-ke-hoach` theo contracts/api-002.yaml
- [x] T077 [P] [US7] *(bố cục duyệt 2026-10-21)* Tạo màn `frontend/src/tinh-nang/phan-quyen/PhanQuyenVaiTro.jsx`, `PhanQuyenTaiKhoan.jsx`, `QuyenTaiThoiDiem.jsx` và test `PhanQuyen.test.jsx` (412, chặn vượt ma trận)

**Checkpoint**: Mọi user story của 002 chạy độc lập.

---

## Phase 9a: User Story 8 – Cấp tài khoản và đăng nhập cho người cao tuổi (Priority: P2) *(bổ sung 2026-10-18, Q-365, Q-368, Q-370, Q-371)*

**Goal**: Tài khoản người cao tuổi tạo theo lời chính người cao tuổi, đăng nhập bằng mã QR, gắn một thiết bị, phiên không hết theo thời gian, token thiết bị cho nút SOS, khóa khi kết thúc lưu trú hoặc theo yêu cầu phê duyệt (FR-067 → FR-075, BR-M15-23, 24).

**Independent Test**: Quickstart K28 → K35 bằng đồng hồ giả lập và bản giả 001.

### Tests cho User Story 8

- [x] T083 [P] [US8] *(2026-10-24: `CreateResidentAccountIT` (5))* Test `BT/m15/taikhoan/TaoTaiKhoanNguoiCaoTuoiIT.java`: tạo khi người cao tuổi Đang lưu trú và có `xacNhan`; thiếu `xacNhan` 422; đã có tài khoản hoặc không Đang lưu trú 409; vai trò duy nhất AC-16, phạm vi `BAN_THAN` (FR-067, FR-068, US8 #1)
- [x] T084 [P] [US8] Test `BT/m15/taikhoan/MaQrDangNhapIT.java`: mã dùng một lần, hết hạn CFG-M15-11 (đồng hồ giả lập), mã mới vô hiệu mã cũ, Điều dưỡng ngoài phạm vi 403, dùng mã gắn thiết bị và trả `tokenThietBi` (FR-069, US8 #2)
- [x] T085 [P] [US8] Test `BT/m15/taikhoan/ThietBiGanIT.java`: một tài khoản một thiết bị; gắn máy mới gỡ máy cũ và kết thúc phiên trên đó; máy đã gắn không đăng nhập được tài khoản khác (409 `THIET_BI_DA_GAN`); gỡ thiết bị kèm lý do (FR-070, FR-014a, BR-M15-24, US8 #4, #5)
- [x] T086 [P] [US8] Test `BT/m15/taikhoan/PhienNguoiCaoTuoiKhongHetIT.java`: 200 ngày không thao tác thì phiên kênh `APP_NGUOI_CAO_TUOI` còn, tài khoản không chuyển Không hoạt động; đổi mật khẩu kết thúc phiên (FR-010, FR-013a, US8 #3, SC-013)
- [x] T087 [P] [US8] *(2026-10-21: #7, #8 xong ở `ResidentAccountLockIT`; 2026-10-24: #6 ở `ResidentAccountFinalStatusIT` (3))* Test `BT/m15/taikhoan/KhoaTaiKhoanNguoiCaoTuoiIT.java`: sự kiện sang trạng thái cuối → `DA_KHOA` `NCT_KET_THUC_LUU_TRU`, thiết bị gỡ, bỏ trùng theo `lichSuId`, không mở lại; tự yêu cầu rồi mở lại; yêu cầu `M15.KHOA_TAI_KHOAN_NCT` chỉ khóa khi Quản lý viện duyệt (BR-M15-23, FR-072, FR-073, US8 #6 → #8)
- [x] T088 [P] [US8] Test `BT/m15/taikhoan/XacDinhThietBiSosIT.java`: `ThietBiGanApi.xacDinh` trả người cao tuổi khi không có phiên; rỗng khi token sai, thiết bị gỡ, tài khoản Đã khóa; Khóa tạm vẫn xác định được; gọi `/sos` không có phiên thì bộ lọc không trả 401, ngoài ba đường của T011 mọi đường khác vẫn đòi phiên (`DuongKhongCanPhienIT`) (FR-071, SC-013)
- [x] T089 [P] [US8] Test `BT/m15/taikhoan/NguoiThanKhongXemTaiKhoanNctIT.java`: tài khoản AC-10 gọi endpoint tài khoản, thiết bị, nhật ký của tài khoản người cao tuổi → 403/404 (FR-075, US8 #9)

### Implementation cho User Story 8

- [x] T090 [US8] Tạo `MIG/V002_08__tai_khoan_nguoi_cao_tuoi.sql`: thêm `NGUOI_CAO_TUOI` vào `tai_khoan.loai`, cột `nguoi_cao_tuoi_id` `UNIQUE`, `CHECK` mới, giá trị `nguyen_nhan_khoa` mới, kênh `APP_NGUOI_CAO_TUOI` và lý do kết thúc phiên mới; bảng `thiet_bi_gan`, `ma_qr_dang_nhap` với cột sinh `UNIQUE`; nạp vai trò AC-16 và cột NCT của ma trận (data-model mục 2, 3; cú pháp dùng được ở cả MySQL 8.0 và 8.4)
- [x] T091 [US8] *(2026-10-24: hồ sơ người cao tuổi qua SPI `ResidentProfileApi` do M01 cài thay cho gọi thẳng `NguoiCaoTuoiApi`, vì vòng M15 ↔ M01; ghi chú ở plan)* Cài tạo tài khoản người cao tuổi trong `TaiKhoanService.java` (`NguoiCaoTuoiApi.trangThai`, `xacNhan` vào `chi_tiet` nhật ký) và `POST /tai-khoan-nguoi-cao-tuoi` trong `TaiKhoanController.java`; phạm vi `BAN_THAN` trong `PhamViService.java`; `TaiKhoanApi.theoNguoiCaoTuoi` (FR-067, FR-068)
- [x] T092 [US8] Cài `BE/m15/taikhoan/xacthuc/internal/MaQrService.java`, `ThietBiGanService.java` và endpoint `POST /tai-khoan/{id}/ma-qr`, `POST /xac-thuc/dang-nhap-qr`, `POST /tai-khoan/{id}/go-thiet-bi`; đăng nhập mật khẩu kênh `APP_NGUOI_CAO_TUOI` cũng gắn thiết bị; bỏ kiểm CFG-M15-10 cho kênh này ở lần tra phiên; job `ChuyenKhongHoatDong` chỉ lọc người thân; khai báo CFG-M15-11 như các CFG-M15 khác (FR-069, FR-070, FR-010, FR-013a, FR-014a; research A-15)
- [x] T093 [US8] Cài `ThietBiGanApi` trong `BE/m15/taikhoan/api/` và `xacthuc/internal/ThietBiGanApiImpl.java`; ArchUnit chỉ cho endpoint SOS của 007 gọi; `CanQuyenArchTest` có danh sách ngoại lệ có tên, chỉ gồm các lệnh của `SosController` (007), vì lệnh SOS kiểm quyền bằng `ThietBiGanApi.xacDinh` thay cho `@CanQuyen` (FR-071; module-interfaces 1.11)
- [x] T094 [US8] *(2026-10-21: endpoint khóa, mở khóa, loại yêu cầu xong; 2026-10-24: listener đặt ở M01 (`ResidentAccountLockOnFinalStatus`) gọi `AccountApi.lockResidentOnFinalStatus`, vì vòng M15 ↔ M01)* Cài khóa, mở khóa tài khoản người cao tuổi: `POST /tai-khoan/{id}/khoa-nguoi-cao-tuoi`, `/mo-khoa-nguoi-cao-tuoi`; listener `BE/m15/taikhoan/lich/KhoaTaiKhoanNctKhiKetThuc.java` nghe `NguoiCaoTuoiDaChuyenTrangThai`; `LoaiYeuCauHandler` `M15.KHOA_TAI_KHOAN_NCT` (`choTuDuyet() = false`) (FR-072, FR-073, BR-M15-23; research A-16)
- [x] T095 [P] [US8] *(2026-10-24: bố cục duyệt, DESIGN.md 5.20.0; `frontend/src/features/accounts/` `CreateResidentAccount`, `ResidentAccountSection`, test `ResidentAccount.test.jsx` (8))* Tạo màn web `frontend/src/tinh-nang/tai-khoan/TaiKhoanNguoiCaoTuoi.jsx` (tạo kèm xác nhận của người cao tuổi, hiện mã QR, gỡ thiết bị, khóa, mở khóa, lập đề nghị khóa theo lời người thân) và test `TaiKhoanNguoiCaoTuoi.test.jsx`
- [ ] T096 [P] [US8] Tạo `mobile/lib/nhan-vien/ma-qr-nguoi-cao-tuoi/` (Điều dưỡng tạo, hiện mã QR, đếm ngược hết hạn) và test `mobile/test/nhan-vien/ma_qr_nguoi_cao_tuoi_test.dart`
- [x] T097 [P] [US8] *(2026-10-24: `mobile/lib/resident/qr_login/`, `biometric_unlock/`, `today/`; test `test/resident/qr_login/qr_login_test.dart` (8), `biometric_unlock/biometric_unlock_test.dart` (4); chưa chạy trên máy thật)* Thêm `mobile_scanner`, `local_auth` vào `mobile/pubspec.yaml`; tạo `mobile/lib/nguoi-cao-tuoi/dang-nhap-qr/` (quét mã, lưu token phiên và token thiết bị trong `flutter_secure_storage`, mở khóa sinh trắc học tùy chọn, không có PIN) và test `mobile/test/nguoi-cao-tuoi/dang_nhap_qr_test.dart`, `mo_khoa_sinh_trac_test.dart` (FR-074)

**Checkpoint**: Quickstart K28 → K35 chạy được; 007 và 012 dùng được `ThietBiGanApi`, `TaiKhoanApi.theoNguoiCaoTuoi`.

---

## Phase 10: Polish & Cross-Cutting

- [x] T078 [P] Mở rộng `BT/common/HopDongApiTest.java` (000 T038) so springdoc với `specs/002-account-permission/contracts/api-002.yaml`
- [x] T079 [P] Ghi vào `backend/docs/van-hanh.md`: phiên khôi phục từ bản sao lưu bị kết thúc khi khởi động; HTTPS bắt buộc (NFR-06, NFR-08)
- [x] T079a *(Bổ sung 2026-10-20)* Dữ liệu mẫu profile `demo` của 002 `backend/src/main/resources/db/migration/demo/V002_90__sample_accounts.sql`: một tài khoản cho mỗi vai trò AC-01 → 05, 07, 09, 14 và một Quản lý viện thứ hai (quickstart.md mục 1), gán vai trò, mật khẩu demo băm Argon2id (mật khẩu ghi ở quickstart, chỉ dùng cho `demo`); làm sau T032, T044. Tầng, ca, phân công, giấy phép, người thân R, M là dữ liệu demo của module chủ (003, 008, 012), thêm khi các module đó có; bản giả trong test không dùng cho `demo` (000 plan, T106). Mở khóa 000 T106, T103 cùng T022, T023, T032, T044, T052, T054
- [x] T080 Cập nhật `BT/support/QuyenGia.java` thành tiện ích dựng người dùng thật cho test của 001, 003–019
- [ ] T081 *(2026-10-21: `./mvnw clean verify`, `npm test` qua; còn `flutter test` chờ mobile, kịch bản quickstart mục 3 chờ dữ liệu demo của 003, 008, 012 và 001, SC-005, SC-008 đo với người dùng thật)* Chạy `./mvnw verify`, `npm test`, `flutter test` hai app và kịch bản quickstart.md mục 3; đo SC-005, SC-008 theo quickstart mục 5
- [x] T082 Rà truy vết data-model mục 7: mỗi BR-M15-01, 02, 04, 05, 14 → 24, BR-M06-05, BR-M09-04 có test tương ứng; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-006a, FR-023 — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story; Foundational của 002 chặn mọi feature 001, 003–019 (cần `@CanQuyen`, `NguoiDungHienTai`)
- **US1 (P1)**: sau Foundational; là điểm kiểm tra dùng chung, làm trước
- **US2, US3 (P1)**: sau Foundational; độc lập với nhau; US3 dùng tài khoản tạo ở US2 (hoặc dữ liệu test)
- **US4 (P2)**: sau US1 (nối bước 2, 3 của `QuyenService`)
- **US5 (P2)**: sau US1 (bước 4)
- **US6 (P2)**: sau US2 (tạo tài khoản), US4 (phạm vi)
- **US7 (P3)**: sau US1, US2
- **US8 (P2)** *(bổ sung 2026-10-18)*: sau US2, US3 (tài khoản, phiên), US4 (phạm vi); trước nút SOS của 007 và kênh người cao tuổi của 012
- Bản giả T003 được thay dần khi làm plan 001, 003, 004, 007, 008, 012

### Parallel Opportunities

- T008–T010 song song; T015 song song với T011–T014
- Test [P] trong mỗi story song song; US2 và US3 song song; US5 và US4 song song sau US1
- Client web, app nhân viên, app người thân song song khi controller đã có

## Parallel Example: User Story 3

```text
Task: "T036 DangNhapKhoaTamIT + KhongHoatDongIT"
Task: "T038 DangNhapKhongLoTonTaiTest"
Task: "T040 MotThietBiMotPhienIT"
# sau T041–T044:
Task: "T045 web DangNhap.jsx"
Task: "T046 app nhân viên đăng nhập"
Task: "T047 app người thân đăng nhập"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → Phase 3 (US1) → Phase 5 (US3 phần đăng nhập nhân viên)
2. **Dừng và kiểm chứng**: `MaTranQuyenKhopNguonTest`, `MaTranQuyenIT` xanh; đăng nhập web chạy
3. Các feature khác bắt đầu dùng `@CanQuyen`

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6 → US7; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- Không có API sửa ma trận quyền; ma trận chỉ đổi bằng migration khi tài liệu nguồn đổi
- Không đệm phạm vi và trạng thái phiên giữa các yêu cầu
- Mọi mốc thời gian đọc `ThamSoApi` (CFG-M15-01, 02, 07 → 10, CFG-M01-04)
