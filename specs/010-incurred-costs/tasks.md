---
description: "Danh sách task triển khai feature 010 – Chi phí phát sinh"
---

# Tasks: Chi phí phát sinh (010)

**Input**: Tài liệu thiết kế ở `specs/010-incurred-costs/` (plan.md, spec.md, research.md P-01 → P-15, data-model.md, contracts/api-010.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 001 (`ThamGiaChuyenTrangThai`), 004 (`HopDongDocApi`, `DonGiaApi`, `NgayLuuTruApi`, `KyCuoiPort`, `MucViecSauQuaDoiPort`, `NguoiDaiDienPort`), 005 (sự kiện `KetQuaCoTinhPhi`, trạng thái có mặt), 006 (`SuKienTinhPhiThuoc*`, `DeNghiMuaHoPort`) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 11.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

**Cập nhật 2026-10-22** (plan mục "Cập nhật 2026-10-22"; Q-378, Q-380, Q-385): thêm T036, T037 (port cho phiếu báo phí của 017). Không đánh số lại.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/eldercare/backend/`, `BT/` = `backend/src/test/java/com/eldercare/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M11 (phần chi phí): `BE/billing/{api,ky,khoan,nguon,tinhgia,dieuchinh,muaho,kycuoi,xuat,cungcap,lich}/`; 017 thêm `billing.sodu`, 019 thêm `billing.muasam`
- Web `frontend/src/tinh-nang/chi-phi/{tra-cuu-nguon,bang-chi-phi,dieu-chinh,mua-ho,xuat-ke-toan}/`, `frontend/src/tinh-nang/cong-nguoi-than/chi-phi/`; app người thân `mobile/lib/nguoi-than/chi-phi/`

---

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; 009 FR-001a): sửa T003. Mã task cũ giữ nguyên.

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/billing/{api,ky,khoan,nguon,tinhgia,dieuchinh,muaho,kycuoi,xuat,cungcap,lich}/` với `package-info.java`; ArchUnit `BT/billing/M11ArchTest.java`: chỉ `billing.sodu`, `billing.muasam` dùng port nội bộ; gói `..lich..` không lập khoản, bảng (trừ kỳ của viện) (research P-01; Q-306)
- [ ] T002 [P] Tạo bản giả trong `BT/support/gia010/`: phát `SuKienTinhPhiNguon` của 011, 012, 014; `SoDuMuaHoPort` (017); `NguoiDaiDienPort` (012)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `billing.api`, bảng kỳ, bảng, khoản, chỉ mục sự kiện; hàm thuần đơn giá; khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T003 [P] Tạo `BE/billing/api/`: `ChiPhiCongNguoiThanApi`, `ChiPhiKyCuoiApi`, `ChiPhiThongKeApi`, record `SuKienTinhPhiNguon`, `SuKienTinhPhiNguonDaDoi` (cho 011, 012, 014 phát) và sự kiện `BangChiPhiDaChot`, `DeNghiMuaHoDaGui/DaQuyetDinh/DaHuy/DaMua`, `KhoanChiPhiDaDoi` (thông báo cài `CoYeuCauThongBao`, loại `CHI_PHI`, không tên thuốc) (contracts mục 1.2 → 1.6); bổ sung 2026-10-18 (009 FR-001a): sự kiện có người nhận là người thân khai `doiTuongThongTin` theo research P-13, test hợp đồng kiểm mọi yêu cầu tới người thân có đối tượng
- [ ] T004 [P] Tạo port nội bộ `BE/billing/cungcap/port/ChiPhiChoSoDuPort.java` (`tongChuaChot`, `phiNgayCoSo`) và `BE/billing/muaho/port/SoDuMuaHoPort.java` (`soDuKhongDu`; không có bean thì không đặt dấu) (research P-12)
- [ ] T005 Tạo `MIG/V010_01__chi_phi.sql`: `chi_phi_nct` (`nct_id` khóa chính, `mien_hoan_thanh_tu`), `ky_chi_phi` (`thang` `UNIQUE`; `trang_thai` `DANG_MO|CHO_CHOT|DA_CHOT`; `han_chot`), `bang_chi_phi` (`UNIQUE (ky_id, nct_id, loai_bang)`; `loai_bang` `THUONG|BO_SUNG`; `trang_thai` `DANG_MO|CHO_CHOT|DA_CHOT`; trigger sau chốt chỉ cho đặt `lan_xuat_dau_id` từ `NULL`), `bang_chi_phi_tong_loai` (nhóm 3), `khoan_chi_phi` (`loai_chi_phi ENUM` 10 loại FR-014a; `nguon_sinh ENUM('THEO_SU_KIEN','NHAP_TAY','MUA_HO','DIEU_CHINH')`; `so_tien NULL` khi `THIEU_DON_GIA`; `dau SET(...)` 10 giá trị ở data-model mục 4; `trang_thai ENUM('NHAP','DA_KIEM_TRA','DA_DUYET','DA_CHOT','DA_HUY')`; cột sinh `khoa_don_vi_hieu_luc` `UNIQUE`; năm chỉ mục; trigger chặn đổi khi `DA_CHOT`/`DA_HUY` và chặn đổi số tiền khi không có `@tinh_lai`), `lich_su_tinh_lai` (nhóm 3), `su_kien_tinh_phi` (khóa chính `(loai_nguon, nguon_id, don_vi)`), `su_kien_nguon_da_xu_ly`, `lan_chay_m11` (data-model mục 2 → 5) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `chi_phi_nct` → `trang_thai_nct`; `ky_chi_phi` → `bang_chi_phi`; `bang_chi_phi_tong_loai` → cột `JSON` của `bang_chi_phi`; `lich_su_tinh_lai` → `lich_su_chung`; `su_kien_nguon_da_xu_ly` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T006 [P] Cài hàm thuần `BE/billing/tinhgia/internal/LamTronDong.java`, `PhiNgayLuuTru.java` (chia giá tháng), `PhiBuoiBanTru.java`, `ThoiGianTinhPhi.java` và test `BT/billing/LamTronDongTest.java`, `BR_M11_09_ChiaGiaThangTest.java` (12 tháng, SC-002), `PhiBuoiBanTruTest.java`, `ThoiGianTinhPhiTest.java` (research P-05)
- [ ] T007 Cài `BE/billing/ky/internal/KhoaChiPhi.java` (khóa `chi_phi_nct`, bảng; research P-03) và khung job `BE/billing/lich/ViecHangNgayM11.java` (00:20) và `ViecHangGioM11.java` (ShedLock, chạy bù) với test `BT/billing/ChayBuM11IT.java` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `chi_phi_nct` → `trang_thai_nct`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T008 [P] Cài provider: `PhanLoaiTruongProvider` (số tiền, đơn giá, tổng, lý do → `TAI_CHINH`; tên thuốc → `THUOC`), `LuuTruProvider` theo data-model mục 10, `GanPhamViProvider`, `QuyenTepProvider` `M11.CHUNG_TU_MUA_HO`; không đăng ký `DinhChinhDuocProvider`

**Checkpoint**: Nền 010 sẵn sàng.

---

## Phase 3: User Story 1 – Lập chi phí nháp theo sự kiện tính phí, mỗi khoản truy về đúng nguồn (Priority: P1) 🎯 MVP

**Goal**: Chỉ mục sự kiện tính phí dựng từ sự kiện của 005, 006, 011, 012, 014; ngày lưu trú, buổi bán trú tính từ 004 lúc tra cứu; Hành chính tra cứu và lập từng khoản hoặc hàng loạt; không có danh sách "chưa lập khoản" (FR-005 → FR-007, FR-011, DBR-15, BR-M11-01).

**Independent Test**: Người nội trú tính theo tháng có trong 3 ngày: 1 ngày vắng, thay tã 2 gói, 1 lần đưa đi khám, 1 liều nguồn viện, 1 buổi hoạt động có phí → tra cứu thấy đủ nguồn, lập hàng loạt, mỗi khoản trỏ đúng nguồn.

### Tests cho User Story 1

- [ ] T009 [P] [US1] Test `BT/billing/BR_M11_01_LapTheoSuKienIT.java`, `FR_005_HangLoatIT.java`, `DBR_15_MotKhoanMoiDonViIT.java`, `LapTrungDongThoiIT.java`, `FR_007_NgayChuaKetThucIT.java`, `FR_011_ThoiGianTinhPhiIT.java`: lập trùng đơn vị bị `UNIQUE` chặn (kể cả đồng thời); hàng loạt trả nguồn bị bỏ qua; ngày chưa kết thúc dấu `LAP_TRUOC`; ngoài thời gian tính phí bị chặn (Q1 → Q6)

### Implementation cho User Story 1

- [ ] T010 [US1] Cài listener chỉ mục `BE/billing/nguon/internal/ChiMucSuKienListener.java` cho `KetQuaCoTinhPhi` (005), `SuKienTinhPhiThuoc` (006), `SuKienTinhPhiNguon` (011, 012, 014); chống trùng `su_kien_nguon_da_xu_ly` (research P-04) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `su_kien_nguon_da_xu_ly` → `moc_da_nhac`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T011 [US1] Cài `BE/billing/nguon/internal/TraCuuNguonService.java` (gộp chỉ mục với ngày lưu trú `NgayLuuTruApi` và buổi bán trú tính lúc tra cứu; không lọc "chưa lập khoản") và `LapKhoanService.java` (từng khoản và hàng loạt; tạo bảng `DANG_MO` khi khoản đầu tiên; đơn giá qua `TinhDonGia` US2)
- [ ] T012 [US1] Tạo `BE/billing/nguon/web/NguonController.java` (`GET /chi-phi/ban-ghi-nguon`, `POST /chi-phi/khoan/theo-su-kien`) và màn web `frontend/src/tinh-nang/chi-phi/tra-cuu-nguon/` (không có bộ lọc "chưa lập khoản", hiển thị nguồn bị bỏ qua) với test Vitest

**Checkpoint**: Lập khoản theo nguồn chạy độc lập.

---

## Phase 4: User Story 2 – Đơn giá và số tiền đúng căn cứ (Priority: P1)

**Goal**: Đơn giá theo nội dung hợp đồng tại ngày phát sinh hoặc phiên bản đơn giá; thuộc gói 0 đồng; ngày vắng nội trú tính như ngày lưu trú, chặn sau Giải phóng giường (Q-395); thiếu đơn giá thì đánh dấu, tính lại khi có phiên bản (FR-008 → FR-013, DBR-16, BR-M11-02, 03, 09, BR-M02-06 phần 010).

**Independent Test**: Hợp đồng giá tháng 9.300.000 đồng tháng 10 (31 ngày) có gói gồm tã; phiên bản "đưa đi khám" đổi giữa tháng; phụ lục đổi giá tháng từ 20/11 → số tiền từng khoản đúng.

### Tests cho User Story 2

- [ ] T013 [P] [US2] Test `BT/billing/DBR_16_DonGiaNgayPhatSinhIT.java`, `BR_M11_02_PhienBanDonGiaIT.java`, `BR_M11_03_ThuocGoiIT.java`, `FR_012_ThieuDonGiaIT.java`, `PhuLucDoiGiaIT.java`, `NgayVangHeSoIT.java`: căn cứ lưu ở `can_cu_don_gia`; khoản theo sự kiện không có ô nhập số tiền; `PhienBanDonGiaDaTao` tính lại khoản thiếu đơn giá chưa chốt (Q10, Q11, Q14)

### Implementation cho User Story 2

- [ ] T014 [US2] Cài `BE/billing/tinhgia/internal/TinhDonGia.java` (`HopDongDocApi.noiDungTaiNgay`, `DonGiaApi`, gói, `NgayLuuTruApi` (vắng, đã giải phóng giường; Q-395), `PhiNgayLuuTru`, `LamTronDong`; ghi `can_cu_don_gia`, `gia_tri_nguon_da_dung`) và listener `PhienBanDonGiaDaTao`, `PhuLucDaApDung` (research P-05)

**Checkpoint**: Số tiền đúng căn cứ.

---

## Phase 5: User Story 3 – Kiểm tra, duyệt và chốt bảng chi phí (Priority: P1)

**Goal**: Hành chính kiểm tra (hàng loạt), Quản lý viện duyệt và chốt; tách quyền; gửi chốt khi không còn Nháp; cảnh báo biến động; chốt nguyên tử, lưu tổng, phát `BangChiPhiDaChot` một lần; kỳ chuyển trạng thái theo lịch (FR-001 → FR-004, FR-014 → FR-017, FR-030 → FR-033, DBR-17, BR-M11-06, 07).

**Independent Test**: Bảng tháng 10 của A gồm 40 khoản: kiểm tra 38, để 1 Nháp, 1 thiếu đơn giá; thử gửi chốt, chốt; trả lại một khoản; hoàn tất rồi chốt; thử sửa sau chốt.

### Tests cho User Story 3

- [ ] T015 [P] [US3] Test đơn vị `BT/billing/BR_M11_07_BienDongTest.java`, `KiemTraVuotTest.java`
- [ ] T016 [P] [US3] Test `BT/billing/FR_015_HangLoatIT.java`, `BR_M11_06_ChotBangIT.java`, `FR_033_ChotNguyenTuIT.java`, `FR_004_VeDangMoIT.java`, `DBR_17_BangDaChotIT.java`, `FR_017_TachQuyenIT.java`: không gửi chốt khi còn Nháp; khoản Nháp mới làm bảng về Đang mở; chốt trong lúc lập khoản mới (đồng thời); trigger chặn sửa sau chốt; người kiểm tra không tự duyệt (Q16 → Q24, Q41)

### Implementation cho User Story 3

- [ ] T017 [US3] Cài `BE/billing/khoan/internal/KhoanService.java` (bảng chuyển khoản: kiểm tra, bỏ kiểm tra, duyệt có lý do chấp nhận vượt, trả lại, hủy) và `BE/billing/ky/internal/BangChiPhiService.java` (gửi chốt với `BienDongKy`, trả lại, chốt nguyên tử dưới khóa bảng: khoản → `DA_CHOT`, `bang_chi_phi_tong_loai`, `tong`, kỳ → `DA_CHOT` khi bảng cuối, phát `BangChiPhiDaChot`) (research P-06 → P-08) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `bang_chi_phi_tong_loai` → cột `JSON` của `bang_chi_phi`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T018 [US3] Đăng ký việc hằng ngày: tạo kỳ ngày 1, `DANG_MO → CHO_CHOT` hết tháng (`han_chot` CFG-M11-01), gỡ dấu lập trước, nhắc hạn chốt và người chưa có bảng thường
- [ ] T019 [US3] Tạo `BE/billing/khoan/web/KhoanController.java` (`GET /chi-phi/khoan/{id}`, `POST /chi-phi/khoan/kiem-tra`, `/{id}/bo-kiem-tra`, `/duyet`, `/{id}/tra-lai`, `/{id}/huy`) và `BE/billing/ky/web/BangController.java` (`GET /chi-phi/bang`, `/{id}`, `POST /{id}/gui-chot`, `/tra-lai`, `/chot` với `If-Match`, `GET /chi-phi/ky`); màn web `frontend/src/tinh-nang/chi-phi/bang-chi-phi/`

**Checkpoint**: P1 hoàn tất.

---

## Phase 6: User Story 4 – Tính lại trước chốt, khoản điều chỉnh sau chốt (Priority: P2)

**Goal**: Nguồn đổi trước chốt → tính lại, về Nháp hoặc hủy, có lịch sử; sau chốt → biến động và báo Hành chính, Hành chính lập khoản điều chỉnh (có thể bảng bổ sung); tổng gốc + điều chỉnh ≥ 0 (FR-018 → FR-020, FR-025 → FR-027, BR-M11-05).

**Independent Test**: Đính chính số lượng tã khi bảng mở; hủy ghi nhận một liều sau chốt; đính chính ngày giải phóng giường sau chốt; Hành chính lập miễn giảm → kết quả từng tình huống.

### Tests cho User Story 4

- [ ] T020 [P] [US4] Test `BT/billing/BR_M11_05_TinhLaiIT.java`, `FR_027_BaoSauChotIT.java`, `FR_026_TongKhongAmIT.java`: tính lại ghi `lich_su_tinh_lai`; khoản Đã kiểm tra/Đã duyệt về Nháp; sau chốt chỉ ghi biến động, không tính sẵn số chênh lệch; điều chỉnh vượt làm tổng âm bị chặn (Q7, Q25, Q26) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lich_su_tinh_lai` → `lich_su_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**

### Implementation cho User Story 4

- [ ] T021 [US4] Cài listener nguồn đổi `BE/billing/nguon/internal/NguonDoiListener.java` (`KetQuaTinhPhiDaDinhChinh`, `SuKienTinhPhiThuocDaDieuChinh/DaDinhChinh`, `NgayLuuTruDaDoi`, `TrangThaiCoMatDaDoi`, `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi`, `SuKienTinhPhiNguonDaDoi`): tính lại với `@tinh_lai`, về Nháp, hủy; sau chốt thêm `bien_dong_nguon_sau_chot` và báo Hành chính (research P-06) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `bien_dong_nguon_sau_chot` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T022 [US4] Tạo bảng `bien_dong_nguon_sau_chot` (nhóm 3) trong `MIG/V010_02__bien_dong.sql`; cài `BE/billing/dieuchinh/internal/DieuChinhService.java` (lập khoản điều chỉnh, bảng bổ sung khi kỳ đã chốt, kiểm tổng không âm dưới khóa bảng gốc) và endpoint `POST /chi-phi/khoan/{id}/dieu-chinh`, `GET /chi-phi/bien-dong-sau-chot`; màn web `frontend/src/tinh-nang/chi-phi/dieu-chinh/` (không có số chênh lệch tính sẵn) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `bien_dong_nguon_sau_chot` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**

**Checkpoint**: Sai sót xử lý đúng.

---

## Phase 7: User Story 5 – Kỳ cuối khi kết thúc lưu trú hoặc qua đời (Priority: P2)

**Goal**: `KyCuoiPort` tạo, tính lại, hủy mốc dừng trong lệnh của 004; điều kiện `KT_A_CHI_PHI`; qua đời; việc sau qua đời `CHI_PHI_KY_CUOI` (FR-029, FR-034 → FR-036, BR-M11-08).

**Independent Test**: Hồ sơ kết thúc ngày 20/11, chốt kỳ cuối; đổi ngày 22/11 trước và sau chốt; quá ngày dự kiến; qua đời lúc 10:00 ngày 15/11 cho người khác.

### Tests cho User Story 5

- [ ] T023 [P] [US5] Test đơn vị `BT/billing/DieuKienChiPhiKyCuoiTest.java` và `BT/billing/BR_M11_08_KyCuoiIT.java`, `FR_034_DoiNgayKetThucIT.java`, `FR_035_QuaNgayKetThucIT.java`, `FR_029_BangBoSungIT.java`, `FR_036_QuaDoiIT.java`, `KT_A_ChiPhiIT.java` (Q27 → Q32)

### Implementation cho User Story 5

- [ ] T024 [US5] Cài `BE/billing/kycuoi/internal/KyCuoiPortImpl.java` (`taoKyCuoiNhap`, `tinhLai`, `huyMocDung`; MANDATORY), `DieuKienChiPhiKyCuoi.java`, bean `ThamGiaChuyenTrangThai` (`@Order` sau 007; `KT_A_CHI_PHI` `coTheMien = true`; tác động `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN`), `MucViecSauQuaDoiPort` `CHI_PHI_KY_CUOI`, `ChiPhiKyCuoiApi`; listener `HoSoKetThuc*`; việc hằng ngày quá ngày kết thúc (research P-10)

**Checkpoint**: Kỳ cuối chạy độc lập.

---

## Phase 8: User Story 6 – Chi phí nhập tay và đề nghị mua hộ (Priority: P2)

**Goal**: Khoản nhập tay chỉ ngoài danh mục; đề nghị mua hộ với ngưỡng CFG-M11-02, đồng ý người đại diện hoặc Quản lý viện, xin bổ sung, ghi đã mua, vượt số đồng ý; thuốc mua hộ cho 006 (FR-021 → FR-024, BR-M11-04, BR-M11-07).

**Independent Test**: Khoản nhập tay cho mục đã có trong danh mục (bị chặn) và ngoài danh mục; hai đề nghị 300.000 và 800.000 đồng; người đại diện đồng ý; ghi đã mua vượt số.

### Tests cho User Story 6

- [ ] T025 [P] [US6] Test `BT/billing/BR_M11_04_NhapTayIT.java`, `BR_M11_07_MuaHoIT.java`, `FR_023a_NguoiDaiDienIT.java` (gồm BR-M10-10: người đại diện đồng ý hoặc từ chối qua cổng, 012 FR-046a), `FR_024_VuotSoDongYIT.java`, `Q139_ThuocMuaHoIT.java`: hai người đại diện quyết định đồng thời chỉ một thắng; dấu số dư không đủ qua `SoDuMuaHoPort`; nhắc chờ đồng ý CFG-M15-05, 06 (Q34 → Q37)

### Implementation cho User Story 6

- [ ] T026 [US6] Tạo `MIG/V010_03__mua_ho.sql`: `de_nghi_mua_ho` (`trang_thai ENUM('NHAP','CHO_DONG_Y','DUOC_PHEP_MUA','TU_CHOI','HUY','DA_MUA')`; `dang_cho_bo_sung`; trigger chặn đổi khi trạng thái cuối), `de_nghi_gui_nguoi_dai_dien`, `quyet_dinh_mua_ho` (nhóm 3; `loai` `DONG_Y|TU_CHOI|RUT_BO_SUNG|XIN_BO_SUNG`) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `de_nghi_mua_ho` → `ho_so_chung`; `de_nghi_gui_nguoi_dai_dien`, `quyet_dinh_mua_ho` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T027 [US6] Cài `BE/billing/muaho/internal/DeNghiMuaHoService.java` (bảng chuyển data-model mục 6; người đại diện qua `NguoiDaiDienPort`; ghi đã mua tạo khoản `MUA_HO`, chứng từ qua `TepApi`; `DeNghiMuaHoPort` cho 006), khoản nhập tay trong `KhoanService.java`, nhắc trong `ViecHangGioM11` (research P-09)
- [ ] T028 [US6] Tạo endpoint `POST /chi-phi/khoan/nhap-tay`, `/de-nghi-mua-ho`, `/{id}/gui`, `/dong-y`, `/tu-choi`, `/xin-bo-sung`, `/rut-bo-sung`, `/huy`, `/ghi-da-mua`; màn web `frontend/src/tinh-nang/chi-phi/mua-ho/`; app `mobile/lib/nguoi-than/chi-phi/` (đồng ý/từ chối) với test flutter

**Checkpoint**: Mua hộ chạy độc lập.

---

## Phase 9: User Story 7 – Người thân xem bảng chi phí, kế toán nhận dữ liệu kỳ đã chốt (Priority: P3)

**Goal**: Cổng người thân (bảng, tạm tính, tổng chưa chốt) theo quyền xem chi phí; xuất kế toán XLSX/CSV toàn bộ hoặc chưa từng xuất; dữ liệu cho 016, 017 (FR-037 → FR-042).

**Independent Test**: Chốt bảng tháng 10 của 3 người, 1 người chưa chốt; xuất kỳ, xuất lại; đối chiếu file; mở cổng bằng người thân có và không có quyền xem chi phí.

### Tests cho User Story 7

- [ ] T029 [P] [US7] Test đơn vị `BT/billing/TongTamTinhTest.java` và `BT/billing/FR_039_XuatKeToanIT.java` (SC-009), `FR_019_TenThuocIT.java`, `FR_038_TamTinhIT.java`, `FR_038a_TongChuaChotIT.java` (Q38 → Q40)

### Implementation cho User Story 7

- [ ] T030 [US7] Tạo bảng `lan_xuat_ke_toan`, `lan_xuat_bang` (nhóm 3) trong `MIG/V010_04__xuat.sql`; cài `BE/billing/xuat/internal/XuatKeToanService.java` (nhật ký hành động; đặt `lan_xuat_dau_id`) và `BE/billing/cungcap/internal/` (`ChiPhiCongNguoiThanApi`, `ChiPhiThongKeApi`, `ChiPhiChoSoDuPort`; tên thuốc đọc qua 006 khi được phép) (research P-11) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lan_xuat_ke_toan`, `lan_xuat_bang` → hành động `nhat_ky`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T031 [US7] Tạo endpoint `POST /chi-phi/ky/{id}/xuat`, `GET /nguoi-cao-tuoi/{nctId}/chi-phi/bang`, `/chi-phi/tam-tinh`; màn web `frontend/src/tinh-nang/chi-phi/xuat-ke-toan/`, `frontend/src/tinh-nang/cong-nguoi-than/chi-phi/`; app người thân phần bảng và tạm tính

**Checkpoint**: Mọi user story của 010 chạy độc lập.

---

### Bổ sung 2026-10-22 (FR-042, bảng giao tiếp "Gửi 017"; Q-378, Q-380)

- [ ] T036 [P] [US7] Test `BT/billing/BangDaChotPortIT.java` (quickstart P1): đủ khoản Đã chốt, tổng theo loại khớp `bang_chi_phi_tong_loai`; khoản thuốc không có tên thuốc; khoản điều chỉnh có kỳ gốc; bảng chưa chốt trả rỗng; thêm ca phí chấm dứt sớm ngoài danh mục (P2) vào `BR_M11_04_NhapTayIT` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `bang_chi_phi_tong_loai` → cột `JSON` của `bang_chi_phi`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [ ] T037 [US7] Thêm `bangDaChot` vào `ChiPhiChoSoDuPort` (`BE/billing/cungcap/port/`) và cài trong `BE/billing/cungcap/internal/` (research P-12); cập nhật bản giả cho test của 017 (luật "Đổi api")

## Phase 10: Polish & Cross-Cutting

- [ ] T032 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/010-incurred-costs/contracts/api-010.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 010
- [ ] T033 Thay bản giả `KyCuoiPort`, `DeNghiMuaHoPort`, `MucViecSauQuaDoiPort` ở test của 004, 006 bằng bản cài của 010 và chạy lại; khi 011, 012, 014, 017 có bản thật chạy lại US1, US6
- [ ] T034 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T035 Rà truy vết data-model mục 11: DBR-15, 16, 17, 23 và BR-M11-01 → 09, BR-M02-06 (phần 010), BR-M10-10 (phần 010) có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-028, FR-043, FR-044 — mỗi FR có task hoặc test tương ứng (constitution XV)
- [ ] T038 *(bổ sung 2026-10-29, research R-15 bản 2 của 000)* Khi viết migration và entity của feature, áp data-model mục 0: không tạo các bảng đã gộp, dùng bảng gộp, cột `JSON` và bảng chung (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`); test bảo vệ nhóm dữ liệu viết cho bảng gộp *(2026-10-30, audit cơ sở dữ liệu F-04, PR #63)* Mọi bảng tạo hoặc gộp khai nhóm dữ liệu: entity bằng `@DataGroup`, bảng JDBC bằng `COMMENT = 'DataGroup=CATALOG|BUSINESS|RECORD|MULTIPLE|TECHNICAL'` (backend/README.md mục 4); bảng `RECORD` có `trg_<bảng>_no_update`, `trg_<bảng>_no_delete`; bảng chứa nhóm 2 và nhóm 3 dùng `@DataGroup(MULTIPLE)`, cột `locked_at`, trigger `trg_<bảng>_row_lock`; dòng bảng chung đặt `entity_type` trùng `RetentionProvider.dataType()` của bản ghi cha; job dùng `SchedulerRuns` (module-interfaces 000 mục 1.8). `RecordGroupTriggerIT` kiểm.

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story; Foundational của 010 chặn 011, 012, 014 (phát `SuKienTinhPhiNguon`) và 017 (port, `BangChiPhiDaChot`)
- **US1, US2 (P1)**: sau Foundational; US1 dùng `TinhDonGia` của US2 (làm US2 song song hoặc trước)
- **US3 (P1)**: sau US1
- **US4, US5, US6 (P2)**: sau US3
- **US7 (P3)**: sau US3

### Parallel Opportunities

- T003, T004, T006, T008 song song
- US4, US5, US6 song song sau US3

## Parallel Example: User Story 3

```text
Task: "T015 BR_M11_07_BienDongTest + KiemTraVuotTest"
Task: "T016 FR_015/BR_M11_06/FR_033/FR_004/DBR_17/FR_017 IT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US2 → US1 → US3
2. **Dừng và kiểm chứng**: tra cứu nguồn → lập → kiểm tra → duyệt → chốt; `BangChiPhiDaChot` phát một lần
3. Thêm US4, US5 trước khi 017 bắt đầu

### Incremental Delivery

US2 → US1 → US3 → US4 → US5 → US6 → US7; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- 010 không lập khoản, bảng, điều chỉnh thay actor; không gợi ý đơn giá, không tính sẵn chênh lệch (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M11-*, CFG-M15-05, 06, CFG-M02-09)
